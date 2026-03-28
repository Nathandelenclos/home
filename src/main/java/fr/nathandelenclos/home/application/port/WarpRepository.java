package fr.nathandelenclos.home.application.port;

import fr.nathandelenclos.home.domain.TeleportPoint;

import java.util.List;
import java.util.Optional;

public interface WarpRepository {

    void save(String warpName, TeleportPoint point);

    Optional<TeleportPoint> find(String warpName);

    List<String> listNames();

    boolean delete(String warpName);
}
