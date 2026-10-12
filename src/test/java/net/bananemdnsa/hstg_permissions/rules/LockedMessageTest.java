package net.bananemdnsa.hstg_permissions.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class LockedMessageTest {

    @Test
    void emptyFallsBackToTheLangKey() {
        assertEquals(Optional.empty(), LockedMessage.format("", "Nether", "time"));
        assertEquals(Optional.empty(), LockedMessage.format("   ", "Nether", "time"));
        assertEquals(Optional.empty(), LockedMessage.format(null, "Nether", "time"));
    }

    @Test
    void placeholdersAreFilled() {
        assertEquals(Optional.of("§cNo /time without §eNether"),
                LockedMessage.format("&cNo /{command} without &e{stage}", "Nether", "time"));
    }

    @Test
    void theDefaultReadsWell() {
        assertEquals(Optional.of("§cThis command is still locked. §7Needs: §eNether, Mod"),
                LockedMessage.format(LockedMessage.DEFAULT, "Nether, Mod", "gamemode creative"));
    }

    @Test
    void substitutedTextKeepsItsAmpersand() {
        assertEquals(Optional.of("§cNeeds R&D for /say a&b"),
                LockedMessage.format("&cNeeds {stage} for /{command}", "R&D", "say a&b"));
    }

    @Test
    void hiddenStagesShowAsQuestionMarks() {
        assertEquals(Optional.of("Needs ???"), LockedMessage.format("Needs {stage}", LockedMessage.HIDDEN, "time"));
    }
}
