package fr.nathandelenclos.home.application.port;

import fr.nathandelenclos.home.domain.TpaRequest;

import java.util.Optional;
import java.util.UUID;

public interface TpaRequestRepository {

    void saveForTarget(UUID targetId, TpaRequest request);

    Optional<TpaRequest> removeForTarget(UUID targetId);
}
