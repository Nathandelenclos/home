package fr.nathandelenclos.home.domain;

import java.util.Objects;
import java.util.UUID;

public record TpaRequest(UUID requesterId, long expiresAtMillis) {

    public TpaRequest {
        Objects.requireNonNull(requesterId, "requesterId");
    }

    public boolean isExpired(long nowMillis) {
        return nowMillis > expiresAtMillis;
    }
}
