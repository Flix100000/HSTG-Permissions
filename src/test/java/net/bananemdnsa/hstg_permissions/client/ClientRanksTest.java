package net.bananemdnsa.hstg_permissions.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.bananemdnsa.hstg_permissions.rank.Rank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClientRanksTest {

    private static final UUID ALEX = new UUID(0, 1);
    private static final UUID STEVE = new UUID(0, 2);

    @BeforeEach
    void forget() {
        ClientRanks.reset();
    }

    @Test
    void anUpdateAddsAndOverwrites() {
        ClientRanks.apply(false, Map.of(ALEX, new Rank("[A]", "")));
        ClientRanks.apply(false, Map.of(STEVE, new Rank("[S]", ""), ALEX, new Rank("[B]", "")));
        assertEquals(Optional.of(new Rank("[B]", "")), ClientRanks.get(ALEX));
        assertEquals(Optional.of(new Rank("[S]", "")), ClientRanks.get(STEVE));
    }

    @Test
    void aFullListReplacesEverything() {
        ClientRanks.apply(false, Map.of(ALEX, new Rank("[A]", "")));
        ClientRanks.apply(true, Map.of(STEVE, new Rank("[S]", "")));
        assertTrue(ClientRanks.get(ALEX).isEmpty());
    }

    @Test
    void anEmptyRankRemovesThePlayer() {
        ClientRanks.apply(false, Map.of(ALEX, new Rank("[A]", "")));
        ClientRanks.apply(false, Map.of(ALEX, Rank.NONE));
        assertTrue(ClientRanks.get(ALEX).isEmpty());
    }

    @Test
    void resetForgetsAll() {
        ClientRanks.apply(false, Map.of(ALEX, new Rank("[A]", "")));
        ClientRanks.reset();
        assertTrue(ClientRanks.get(ALEX).isEmpty());
    }
}
