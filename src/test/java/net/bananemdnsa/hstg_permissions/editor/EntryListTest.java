package net.bananemdnsa.hstg_permissions.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.bananemdnsa.hstg_permissions.rules.PermissionEntry;
import net.bananemdnsa.hstg_permissions.tree.SampleTree;
import org.junit.jupiter.api.Test;

class EntryListTest {

    private static EntryList listOf(String... commands) {
        EntryList list = new EntryList();
        List<PermissionEntry> entries = new ArrayList<>();
        for (String command : commands) entries.add(new PermissionEntry(command));
        list.load(entries);
        return list;
    }

    @Test
    void unreadableEntriesSurviveASave() {
        List<PermissionEntry> file = List.of(new PermissionEntry("minecraft..op"), new PermissionEntry(" Minecraft.Msg "));
        EntryList list = new EntryList();
        list.load(file);
        assertEquals(file, list.toEntries());
    }

    @Test
    void pickAddsThenRemoves() {
        EntryList list = listOf();
        assertEquals(EntryList.Change.ADDED, list.pick("minecraft.gamemode.creative"));
        assertEquals(List.of(new PermissionEntry("minecraft.gamemode.creative")), list.toEntries());
        assertEquals(EntryList.Change.REMOVED, list.pick("minecraft.gamemode.creative"));
        assertEquals(0, list.size());
    }

    @Test
    void differentSpellingsAreOneEntry() {
        EntryList list = listOf("minecraft.gamemode.*");
        assertEquals(EntryList.Change.REMOVED, list.pick("Minecraft.GameMode"));
        EntryList paths = listOf("minecraft.tp");
        // A concrete path ignores its mod at runtime, so a wrong mod is still the same entry.
        assertEquals(EntryList.Change.REMOVED, paths.pick("othermod.tp"));
        assertEquals(0, paths.size());
    }

    @Test
    void marksShowDirectAndInheritedEntries() {
        EntryList list = listOf("*", "ftbquests.*", "minecraft.gamemode", "minecraft.msg");
        assertEquals(Optional.of(EntryList.Mark.DIRECT), list.markFor("minecraft.msg"));
        assertEquals(Optional.of(EntryList.Mark.INHERITED), list.markFor("ftbquests.quests.open_book"));
        assertEquals(Optional.of(EntryList.Mark.INHERITED), list.markFor("minecraft.gamemode.creative"));
        assertEquals(Optional.of(EntryList.Mark.INHERITED), list.markFor("minecraft.time"));
        assertEquals(Optional.of(EntryList.Mark.DIRECT), list.markFor("*"));
        assertEquals(Optional.empty(), listOf().markFor("minecraft.time"));
    }

    @Test
    void removeAtRemovesTheEntry() {
        EntryList list = listOf("minecraft.a", "minecraft.b", "minecraft.c");
        list.removeAt(1);
        assertEquals(List.of(new PermissionEntry("minecraft.a"), new PermissionEntry("minecraft.c")), list.toEntries());
        assertEquals(List.of("minecraft.a", "minecraft.c"), list.commands());
    }

    @Test
    void anAliasIsTheSameEntryWithACatalog() {
        EntryList list = listOf("minecraft.teleport");
        list.setSpellings(SampleTree.catalog(List.of())::canonical);
        assertEquals(Optional.of(EntryList.Mark.DIRECT), list.markFor("minecraft.tp"));
        assertEquals(EntryList.Change.REMOVED, list.pick("minecraft.tp"));
        assertEquals(0, list.size());
    }

    @Test
    void withoutACatalogSpellingsStaySeparate() {
        EntryList list = listOf("minecraft.teleport");
        assertEquals(Optional.empty(), list.markFor("minecraft.tp"));
    }
}
