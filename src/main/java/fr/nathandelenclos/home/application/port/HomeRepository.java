package fr.nathandelenclos.home.application.port;

import fr.nathandelenclos.home.domain.TeleportPoint;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HomeRepository {

    void save(UUID playerId, String homeName, TeleportPoint point);

    Optional<TeleportPoint> find(UUID playerId, String homeName);

    List<String> listNames(UUID playerId);

    boolean delete(UUID playerId, String homeName);
}
