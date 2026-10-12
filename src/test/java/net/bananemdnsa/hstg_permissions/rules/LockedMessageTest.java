package net.bananemdnsa.hstg_permissions.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class LockedMessageTest {

    @Test
    void emptyFallsBackToTheLangKey() {
        assertEquals(Optional.empty(), LockedMessage.text(""));
        assertEquals(Optional.empty(), LockedMessage.text("   "));
        assertEquals(Optional.empty(), LockedMessage.text(null));
    }

    @Test
    void colourCodesAreTranslated() {
        assertEquals(Optional.of("§cNope"), LockedMessage.text("&cNope"));
    }
}
