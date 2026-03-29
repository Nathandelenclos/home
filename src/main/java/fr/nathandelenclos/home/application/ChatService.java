package fr.nathandelenclos.home.application;

import fr.nathandelenclos.home.application.port.ChatStateRepository;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ChatService {

    private static final List<String> ALLOWED_REACTIONS = List.of("gg", "lol", "+1", "rip", "fire");

    private final ChatStateRepository repository;

    public ChatService(ChatStateRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public List<String> allowedReactions() {
        return ALLOWED_REACTIONS;
    }

    public String normalizeReaction(String reactionRaw) {
        return reactionRaw.toLowerCase(Locale.ROOT);
    }

    public boolean isAllowedReaction(String reactionRaw) {
        if (reactionRaw == null) {
            return false;
        }
        return ALLOWED_REACTIONS.contains(normalizeReaction(reactionRaw));
    }

    public void registerPrivateConversation(UUID firstPlayerId, UUID secondPlayerId) {
        repository.saveLastPrivateContact(firstPlayerId, secondPlayerId);
        repository.saveLastPrivateContact(secondPlayerId, firstPlayerId);
    }

    public Optional<UUID> replyTargetFor(UUID playerId) {
        return repository.findLastPrivateContact(playerId);
    }

    public void registerPublicMessageSender(UUID senderId) {
        repository.saveLastPublicSender(senderId);
    }

    public Optional<UUID> lastPublicMessageSender() {
        return repository.findLastPublicSender();
    }
}
