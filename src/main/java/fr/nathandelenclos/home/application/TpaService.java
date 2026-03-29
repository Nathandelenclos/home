package fr.nathandelenclos.home.application;

import fr.nathandelenclos.home.application.port.TpaRequestRepository;
import fr.nathandelenclos.home.domain.TpaRequest;

import java.util.UUID;

public final class TpaService {

    private static final long DEFAULT_EXPIRATION_MILLIS = 60_000L;

    public enum RequestStatus {
        SENT,
        SELF_REQUEST
    }

    public enum DecisionStatus {
        PROCESSED,
        NO_PENDING,
        EXPIRED
    }

    public record DecisionResult(DecisionStatus status, UUID requesterId) {
    }

    private final TpaRequestRepository repository;
    private final long expirationMillis;

    public TpaService(TpaRequestRepository repository) {
        this(repository, DEFAULT_EXPIRATION_MILLIS);
    }

    public TpaService(TpaRequestRepository repository, long expirationMillis) {
        this.repository = repository;
        this.expirationMillis = expirationMillis;
    }

    public RequestStatus createRequest(UUID requesterId, UUID targetId, long nowMillis) {
        if (requesterId.equals(targetId)) {
            return RequestStatus.SELF_REQUEST;
        }

        TpaRequest request = new TpaRequest(requesterId, nowMillis + expirationMillis);
        repository.saveForTarget(targetId, request);
        return RequestStatus.SENT;
    }

    public DecisionResult acceptRequest(UUID targetId, long nowMillis) {
        return processDecision(targetId, nowMillis);
    }

    public DecisionResult denyRequest(UUID targetId, long nowMillis) {
        return processDecision(targetId, nowMillis);
    }

    private DecisionResult processDecision(UUID targetId, long nowMillis) {
        return repository.removeForTarget(targetId)
                .map(request -> {
                    if (request.isExpired(nowMillis)) {
                        return new DecisionResult(DecisionStatus.EXPIRED, request.requesterId());
                    }
                    return new DecisionResult(DecisionStatus.PROCESSED, request.requesterId());
                })
                .orElseGet(() -> new DecisionResult(DecisionStatus.NO_PENDING, null));
    }
}
