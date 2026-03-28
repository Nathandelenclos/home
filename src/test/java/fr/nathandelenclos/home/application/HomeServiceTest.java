package fr.nathandelenclos.home.application;

import fr.nathandelenclos.home.application.port.HomeRepository;
import fr.nathandelenclos.home.domain.TeleportPoint;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeServiceTest {

    private static final UUID PLAYER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final TeleportPoint POINT = new TeleportPoint("world", 10.0, 64.0, 10.0, 0.0f, 0.0f);

    @Test
    void setHomeNormalizesNameAndSaves() {
        InMemoryHomeRepository repository = new InMemoryHomeRepository();
        HomeService service = new HomeService(repository);

        String savedName = service.setHome(PLAYER_ID, "  Maison ", POINT);

        assertEquals("maison", savedName);
        assertTrue(repository.find(PLAYER_ID, "maison").isPresent());
    }

    @Test
    void findAndDeleteHomeUseNormalizedName() {
        InMemoryHomeRepository repository = new InMemoryHomeRepository();
        HomeService service = new HomeService(repository);
        service.setHome(PLAYER_ID, "Base", POINT);

        Optional<TeleportPoint> found = service.findHome(PLAYER_ID, "  bASE ");
        boolean deleted = service.deleteHome(PLAYER_ID, " BASE ");
        Optional<TeleportPoint> foundAfterDelete = service.findHome(PLAYER_ID, "base");

        assertTrue(found.isPresent());
        assertTrue(deleted);
        assertTrue(foundAfterDelete.isEmpty());
    }

    @Test
    void listHomesReturnsCurrentNames() {
        InMemoryHomeRepository repository = new InMemoryHomeRepository();
        HomeService service = new HomeService(repository);
        service.setHome(PLAYER_ID, "mine", POINT);
        service.setHome(PLAYER_ID, "farm", POINT);

        List<String> homes = service.listHomes(PLAYER_ID);

        assertEquals(List.of("farm", "mine"), homes);
    }

    @Test
    void setHomeRejectsBlankName() {
        InMemoryHomeRepository repository = new InMemoryHomeRepository();
        HomeService service = new HomeService(repository);

        assertThrows(IllegalArgumentException.class, () -> service.setHome(PLAYER_ID, "   ", POINT));
    }

    @Test
    void deleteHomeReturnsFalseWhenMissing() {
        InMemoryHomeRepository repository = new InMemoryHomeRepository();
        HomeService service = new HomeService(repository);

        assertFalse(service.deleteHome(PLAYER_ID, "unknown"));
    }

    private static final class InMemoryHomeRepository implements HomeRepository {

        private final Map<UUID, Map<String, TeleportPoint>> homes = new HashMap<>();

        @Override
        public void save(UUID playerId, String homeName, TeleportPoint point) {
            homes.computeIfAbsent(playerId, ignored -> new HashMap<>()).put(homeName, point);
        }

        @Override
        public Optional<TeleportPoint> find(UUID playerId, String homeName) {
            return Optional.ofNullable(homes.getOrDefault(playerId, Map.of()).get(homeName));
        }

        @Override
        public List<String> listNames(UUID playerId) {
            List<String> names = new ArrayList<>(homes.getOrDefault(playerId, Map.of()).keySet());
            names.sort(Comparator.naturalOrder());
            return names;
        }

        @Override
        public boolean delete(UUID playerId, String homeName) {
            Map<String, TeleportPoint> byPlayer = homes.get(playerId);
            if (byPlayer == null) {
                return false;
            }
            return byPlayer.remove(homeName) != null;
        }
    }
}
