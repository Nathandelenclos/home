package fr.nathandelenclos.home.infrastructure;

import fr.nathandelenclos.home.application.port.TpaRequestRepository;
import fr.nathandelenclos.home.domain.TpaRequest;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryTpaRequestRepository implements TpaRequestRepository {

    private final Map<UUID, TpaRequest> requestsByTarget = new ConcurrentHashMap<>();

    @Override
    public void saveForTarget(UUID targetId, TpaRequest request) {
        requestsByTarget.put(targetId, request);
    }

    @Override
    public Optional<TpaRequest> removeForTarget(UUID targetId) {
        return Optional.ofNullable(requestsByTarget.remove(targetId));
    }
}
