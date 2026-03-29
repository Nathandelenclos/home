package fr.nathandelenclos.home.infrastructure;

import fr.nathandelenclos.home.application.port.ChatStateRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class InMemoryChatStateRepository implements ChatStateRepository {

    private final Map<UUID, UUID> lastPrivateContactByPlayer = new ConcurrentHashMap<>();
    private final AtomicReference<UUID> lastPublicMessageSender = new AtomicReference<>();

    @Override
    public void saveLastPrivateContact(UUID playerId, UUID contactId) {
        lastPrivateContactByPlayer.put(playerId, contactId);
    }

    @Override
    public Optional<UUID> findLastPrivateContact(UUID playerId) {
        return Optional.ofNullable(lastPrivateContactByPlayer.get(playerId));
    }

    @Override
    public void saveLastPublicSender(UUID senderId) {
        lastPublicMessageSender.set(senderId);
    }

    @Override
    public Optional<UUID> findLastPublicSender() {
        return Optional.ofNullable(lastPublicMessageSender.get());
    }
}
