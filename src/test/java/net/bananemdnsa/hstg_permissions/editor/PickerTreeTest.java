package net.bananemdnsa.hstg_permissions.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import net.bananemdnsa.hstg_permissions.rules.EntryPattern;
import net.bananemdnsa.hstg_permissions.tree.SampleTree;
import org.junit.jupiter.api.Test;

class PickerTreeTest {

    private final PickerTree tree = new PickerTree(SampleTree.catalog(List.of()));

    private static List<String> entries(List<PickerRow> rows) {
        return rows.stream().map(PickerRow::entry).toList();
    }

    @Test
    void closedItShowsEverythingAndTheMods() {
        List<PickerRow> rows = tree.rows(Set.of(), "");
        assertEquals(List.of("*", "minecraft.*", "ftbquests.*"), entries(rows));
        assertEquals(PickerRow.Kind.ALL, rows.get(0).kind());
        // execute, gamemode, teleport, time, tp – weird.name cannot be named by an entry
        assertEquals(5, rows.get(1).count());
    }

    @Test
    void openingABranchShowsItsChildren() {
        List<PickerRow> rows = tree.rows(Set.of("minecraft.*", "minecraft.gamemode"), "");
        assertTrue(entries(rows).containsAll(List.of("minecraft.gamemode", "minecraft.gamemode.creative",
                "minecraft.time", "minecraft.tp")));
        PickerRow creative = rows.get(entries(rows).indexOf("minecraft.gamemode.creative"));
        assertEquals(PickerRow.Kind.VALUE, creative.kind());
        assertEquals(2, creative.depth());
        assertFalse(entries(rows).contains("minecraft.time.set"), "time stays closed");
        assertEquals("teleport", rows.get(entries(rows).indexOf("minecraft.tp")).aliasOf());
        assertFalse(entries(rows).contains("ftbquests.quests"), "ftbquests stays closed");
    }

    @Test
    void searchFindsDeepMatchesWithTheirParents() {
        List<PickerRow> rows = tree.rows(Set.of(), "creat");
        assertEquals(List.of("minecraft.*", "minecraft.gamemode", "minecraft.gamemode.creative"), entries(rows));
        assertTrue(rows.get(0).expanded());
    }

    @Test
    void aBareKnownWordIsOffered() {
        PickerRow first = tree.rows(Set.of(), "gamemode").get(0);
        assertEquals(PickerRow.Kind.TYPED, first.kind());
        assertEquals("minecraft.gamemode", first.entry());
        assertTrue(first.found());
    }

    @Test
    void everyRowNamesAValidEntry() {
        for (PickerRow row : tree.rows(Set.of("minecraft.*", "minecraft.gamemode", "minecraft.time",
                "minecraft.time.set", "ftbquests.*"), "")) {
            assertTrue(EntryPattern.parse(row.entry()).isPresent(), row.entry());
        }
    }

    @Test
    void anAtSignFiltersByMod() {
        assertEquals(List.of("ftbquests.*"), entries(tree.rows(Set.of(), "@ftb")));
    }

    @Test
    void aTypedPathIsOfferedOnTop() {
        PickerRow typed = tree.rows(Set.of(), "gamemode.creative").get(0);
        assertEquals(PickerRow.Kind.TYPED, typed.kind());
        assertEquals("minecraft.gamemode.creative", typed.entry());
        assertTrue(typed.found());
        // waystones.tp would be found: a path ignores its mod, so it names vanilla's /tp.
        assertFalse(tree.rows(Set.of(), "waystones.waystone").get(0).found());
    }
}
