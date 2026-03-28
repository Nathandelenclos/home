package fr.nathandelenclos.home.domain;

import java.util.Locale;
import java.util.Objects;

public record TeleportName(String value) {

    public TeleportName {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
    }

    public static TeleportName fromRaw(String rawName) {
        if (rawName == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        String normalized = rawName.trim().toLowerCase(Locale.ROOT);
        return new TeleportName(normalized);
    }
}
