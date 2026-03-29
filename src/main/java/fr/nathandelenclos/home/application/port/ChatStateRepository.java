package fr.nathandelenclos.home.application.port;

import java.util.Optional;
import java.util.UUID;

public interface ChatStateRepository {

    void saveLastPrivateContact(UUID playerId, UUID contactId);

    Optional<UUID> findLastPrivateContact(UUID playerId);

    void saveLastPublicSender(UUID senderId);

    Optional<UUID> findLastPublicSender();
}
