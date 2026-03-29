package fr.nathandelenclos.home.application;

import fr.nathandelenclos.home.infrastructure.InMemoryChatStateRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatServiceTest {

    private final ChatService service = new ChatService(new InMemoryChatStateRepository());

    @Test
    void shouldValidateAllowedReaction() {
        assertTrue(service.isAllowedReaction("gg"));
        assertTrue(service.isAllowedReaction("FIRE"));
        assertFalse(service.isAllowedReaction("nope"));
        assertFalse(service.isAllowedReaction(null));
    }

    @Test
    void shouldRegisterPrivateConversationForBothPlayers() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();

        service.registerPrivateConversation(alice, bob);

        assertEquals(Optional.of(bob), service.replyTargetFor(alice));
        assertEquals(Optional.of(alice), service.replyTargetFor(bob));
    }

    @Test
    void shouldTrackLastPublicSender() {
        UUID sender = UUID.randomUUID();

        service.registerPublicMessageSender(sender);

        assertEquals(Optional.of(sender), service.lastPublicMessageSender());
    }

    @Test
    void shouldNormalizeReactionInLowerCase() {
        assertEquals("fire", service.normalizeReaction("FIRE"));
        assertEquals("+1", service.normalizeReaction("+1"));
    }
}
