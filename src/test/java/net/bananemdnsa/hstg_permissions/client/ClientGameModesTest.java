package net.bananemdnsa.hstg_permissions.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClientGameModesTest {

    @BeforeEach
    void forget() {
        ClientGameModes.reset();
    }

    @Test
    void unknownMeansVanilla() {
        assertFalse(ClientGameModes.known());
    }

    @Test
    void theServerSaysWhichModes() {
        ClientGameModes.set(List.of("survival", "spectator"));
        assertTrue(ClientGameModes.known());
        assertTrue(ClientGameModes.allows("survival"));
        assertFalse(ClientGameModes.allows("creative"));
        assertTrue(ClientGameModes.anyAllowed());
    }

    @Test
    void noModeIsStillAnAnswer() {
        ClientGameModes.set(List.of());
        assertTrue(ClientGameModes.known());
        assertFalse(ClientGameModes.anyAllowed());
    }
}
