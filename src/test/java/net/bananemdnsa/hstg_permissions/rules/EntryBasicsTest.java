package net.bananemdnsa.hstg_permissions.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class EntryBasicsTest {

    @Test
    void modesReadForgivingly() {
        assertEquals(Optional.of(Mode.LOCK), Mode.parse("lock"));
        assertEquals(Optional.of(Mode.LOCK), Mode.parse("LOCK"));
        assertEquals(Optional.of(Mode.GRANT), Mode.parse(" Grant "));
        assertTrue(Mode.parse("deny").isEmpty());
        assertTrue(Mode.parse(null).isEmpty());
    }

    @Test
    void targetsAreLowercase() {
        CommandTarget target = CommandTarget.of("Minecraft", "GameMode", "Creative");
        assertEquals("minecraft", target.mod());
        assertEquals(List.of("gamemode", "creative"), target.spellings().get(0));
        assertEquals("minecraft:gamemode.creative", target.key());
    }

    @Test
    void aTargetNeedsAtLeastOneWord() {
        assertThrows(IllegalArgumentException.class, () -> new CommandTarget("minecraft", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new CommandTarget("minecraft", List.of(List.of())));
    }

    @Test
    void anEntryWithMissingFieldsReadsAsNothing() {
        PermissionEntry empty = new PermissionEntry(null, null);
        assertTrue(empty.pattern().isEmpty());
        assertTrue(empty.parsedMode().isEmpty());
    }

    @Test
    void anEntryReadsItsPatternAndMode() {
        PermissionEntry entry = new PermissionEntry("minecraft.gamemode", "lock");
        assertEquals(EntryPattern.parse("minecraft.gamemode"), entry.pattern());
        assertEquals(Optional.of(Mode.LOCK), entry.parsedMode());
    }
}
