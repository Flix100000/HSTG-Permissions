package net.bananemdnsa.hstg_permissions.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import net.bananemdnsa.hstg_permissions.rules.Mode;
import net.bananemdnsa.hstg_permissions.rules.PermissionEntry;
import org.junit.jupiter.api.Test;

class EntryListTest {

    private static EntryList listOf(String... commandAndMode) {
        EntryList list = new EntryList();
        java.util.List<PermissionEntry> entries = new java.util.ArrayList<>();
        for (int i = 0; i < commandAndMode.length; i += 2) {
            entries.add(new PermissionEntry(commandAndMode[i], commandAndMode[i + 1]));
        }
        list.load(entries);
        return list;
    }

    @Test
    void unreadableEntriesSurviveASave() {
        List<PermissionEntry> file = List.of(new PermissionEntry("minecraft..op", "lock"),
                new PermissionEntry("minecraft.gamemode", "deny"), new PermissionEntry(" Minecraft.Msg ", "GRANT"));
        EntryList list = new EntryList();
        list.load(file);
        assertEquals(file, list.toEntries());
    }

    @Test
    void pickAddsThenRemovesThenSwitches() {
        EntryList list = listOf();
        assertEquals(EntryList.Change.ADDED, list.pick("minecraft.gamemode.creative", Mode.LOCK));
        assertEquals(List.of(new PermissionEntry("minecraft.gamemode.creative", "lock")), list.toEntries());
        assertEquals(EntryList.Change.SWITCHED, list.pick("minecraft.gamemode.creative", Mode.GRANT));
        assertEquals(Optional.of(Mode.GRANT), list.mode(0));
        assertEquals(EntryList.Change.REMOVED, list.pick("minecraft.gamemode.creative", Mode.GRANT));
        assertEquals(0, list.size());
    }

    @Test
    void differentSpellingsAreOneEntry() {
        EntryList list = listOf("minecraft.gamemode.*", "lock");
        assertEquals(EntryList.Change.REMOVED, list.pick("Minecraft.GameMode", Mode.LOCK));
        EntryList paths = listOf("minecraft.tp", "lock");
        // A concrete path ignores its mod at runtime, so a wrong mod is still the same entry.
        assertEquals(EntryList.Change.SWITCHED, paths.pick("othermod.tp", Mode.GRANT));
        assertEquals(1, paths.size());
    }

    @Test
    void marksShowDirectAndInheritedEntries() {
        EntryList list = listOf("*", "lock", "ftbquests.*", "lock", "minecraft.gamemode", "grant", "minecraft.msg", "grant");
        assertEquals(Optional.of(new EntryList.Mark(Mode.GRANT, false)), list.markFor("minecraft.msg"));
        assertEquals(Optional.of(new EntryList.Mark(Mode.LOCK, true)), list.markFor("ftbquests.quests.open_book"));
        assertEquals(Optional.of(new EntryList.Mark(Mode.GRANT, true)), list.markFor("minecraft.gamemode.creative"));
        assertEquals(Optional.of(new EntryList.Mark(Mode.LOCK, true)), list.markFor("minecraft.time"));
        assertEquals(Optional.of(new EntryList.Mark(Mode.LOCK, false)), list.markFor("*"));
        assertEquals(Optional.empty(), listOf().markFor("minecraft.time"));
    }

    @Test
    void removeAtKeepsModesInStep() {
        EntryList list = listOf("minecraft.a", "lock", "minecraft.b", "grant", "minecraft.c", "lock");
        list.removeAt(1);
        assertEquals(List.of(new PermissionEntry("minecraft.a", "lock"), new PermissionEntry("minecraft.c", "lock")),
                list.toEntries());
        assertEquals(List.of("minecraft.a", "minecraft.c"), list.commands());
    }

    @Test
    void countsByMode() {
        EntryList list = listOf("minecraft.a", "lock", "minecraft.b", "grant", "minecraft.c", "LOCK", "minecraft.d", "deny");
        assertEquals(2, list.count(Mode.LOCK));
        assertEquals(1, list.count(Mode.GRANT));
        assertTrue(list.mode(3).isEmpty());
    }
}
