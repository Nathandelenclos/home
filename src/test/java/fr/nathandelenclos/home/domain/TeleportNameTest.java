package fr.nathandelenclos.home.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeleportNameTest {

    @Test
    void fromRawNormalizesValue() {
        TeleportName name = TeleportName.fromRaw("  SpAwN ");

        assertEquals("spawn", name.value());
    }

    @Test
    void fromRawRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> TeleportName.fromRaw(null));
    }

    @Test
    void fromRawRejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> TeleportName.fromRaw("   "));
    }
}
