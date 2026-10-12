package net.bananemdnsa.hstg_permissions.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class EntryBasicsTest {

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
        assertTrue(new PermissionEntry(null).pattern().isEmpty());
    }

    @Test
    void anEntryReadsItsPattern() {
        assertEquals(EntryPattern.parse("minecraft.gamemode"), new PermissionEntry("minecraft.gamemode").pattern());
    }

    @Test
    void anOldModeIsReadAndDropped() {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        PermissionEntry read = gson.fromJson("{\"command\":\"minecraft.time\",\"mode\":\"lock\"}", PermissionEntry.class);
        assertEquals(new PermissionEntry("minecraft.time"), read);
        assertEquals("{\"command\":\"minecraft.time\"}", gson.toJson(read));
    }
}
