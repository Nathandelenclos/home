package fr.nathandelenclos.home.application;

import fr.nathandelenclos.home.application.port.WarpRepository;
import fr.nathandelenclos.home.domain.TeleportPoint;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarpServiceTest {

    private static final TeleportPoint POINT = new TeleportPoint("world", 10.0, 64.0, 10.0, 0.0f, 0.0f);

    @Test
    void setWarpNormalizesNameAndSaves() {
        InMemoryWarpRepository repository = new InMemoryWarpRepository();
        WarpService service = new WarpService(repository);

        String savedName = service.setWarp("  Spawn ", POINT);

        assertEquals("spawn", savedName);
        assertTrue(repository.find("spawn").isPresent());
    }

    @Test
    void findAndDeleteWarpUseNormalizedName() {
        InMemoryWarpRepository repository = new InMemoryWarpRepository();
        WarpService service = new WarpService(repository);
        service.setWarp("Market", POINT);

        Optional<TeleportPoint> found = service.findWarp("  marKET ");
        boolean deleted = service.deleteWarp(" MARKET ");
        Optional<TeleportPoint> foundAfterDelete = service.findWarp("market");

        assertTrue(found.isPresent());
        assertTrue(deleted);
        assertTrue(foundAfterDelete.isEmpty());
    }

    @Test
    void listWarpsReturnsCurrentNames() {
        InMemoryWarpRepository repository = new InMemoryWarpRepository();
        WarpService service = new WarpService(repository);
        service.setWarp("mine", POINT);
        service.setWarp("spawn", POINT);

        List<String> warps = service.listWarps();

        assertEquals(List.of("mine", "spawn"), warps);
    }

    @Test
    void setWarpRejectsBlankName() {
        InMemoryWarpRepository repository = new InMemoryWarpRepository();
        WarpService service = new WarpService(repository);

        assertThrows(IllegalArgumentException.class, () -> service.setWarp("   ", POINT));
    }

    @Test
    void deleteWarpReturnsFalseWhenMissing() {
        InMemoryWarpRepository repository = new InMemoryWarpRepository();
        WarpService service = new WarpService(repository);

        assertFalse(service.deleteWarp("unknown"));
    }

    private static final class InMemoryWarpRepository implements WarpRepository {

        private final Map<String, TeleportPoint> warps = new HashMap<>();

        @Override
        public void save(String warpName, TeleportPoint point) {
            warps.put(warpName, point);
        }

        @Override
        public Optional<TeleportPoint> find(String warpName) {
            return Optional.ofNullable(warps.get(warpName));
        }

        @Override
        public List<String> listNames() {
            List<String> names = new ArrayList<>(warps.keySet());
            names.sort(Comparator.naturalOrder());
            return names;
        }

        @Override
        public boolean delete(String warpName) {
            return warps.remove(warpName) != null;
        }
    }
}
