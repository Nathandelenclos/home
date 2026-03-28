package fr.nathandelenclos.home.application;

import fr.nathandelenclos.home.application.port.HomeRepository;
import fr.nathandelenclos.home.domain.TeleportName;
import fr.nathandelenclos.home.domain.TeleportPoint;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class HomeService {

    private final HomeRepository homeRepository;

    public HomeService(HomeRepository homeRepository) {
        this.homeRepository = homeRepository;
    }

    public String setHome(UUID playerId, String rawName, TeleportPoint point) {
        String normalized = TeleportName.fromRaw(rawName).value();
        homeRepository.save(playerId, normalized, point);
        return normalized;
    }

    public Optional<TeleportPoint> findHome(UUID playerId, String rawName) {
        return homeRepository.find(playerId, TeleportName.fromRaw(rawName).value());
    }

    public List<String> listHomes(UUID playerId) {
        return homeRepository.listNames(playerId);
    }

    public boolean deleteHome(UUID playerId, String rawName) {
        return homeRepository.delete(playerId, TeleportName.fromRaw(rawName).value());
    }
}
