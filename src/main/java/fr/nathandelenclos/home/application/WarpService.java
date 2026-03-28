package fr.nathandelenclos.home.application;

import fr.nathandelenclos.home.application.port.WarpRepository;
import fr.nathandelenclos.home.domain.TeleportName;
import fr.nathandelenclos.home.domain.TeleportPoint;

import java.util.List;
import java.util.Optional;

public final class WarpService {

    private final WarpRepository warpRepository;

    public WarpService(WarpRepository warpRepository) {
        this.warpRepository = warpRepository;
    }

    public String setWarp(String rawName, TeleportPoint point) {
        String normalized = TeleportName.fromRaw(rawName).value();
        warpRepository.save(normalized, point);
        return normalized;
    }

    public Optional<TeleportPoint> findWarp(String rawName) {
        return warpRepository.find(TeleportName.fromRaw(rawName).value());
    }

    public List<String> listWarps() {
        return warpRepository.listNames();
    }

    public boolean deleteWarp(String rawName) {
        return warpRepository.delete(TeleportName.fromRaw(rawName).value());
    }
}
