package net.bananemdnsa.hstg_permissions.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import net.bananemdnsa.hstg_permissions.rules.ProtectionRules;
import org.junit.jupiter.api.Test;

class ProtectedSelectionTest {

    @Test
    void anEntryIsOnAndItsChildrenAreCovered() {
        ProtectedSelection selection = new ProtectedSelection(List.of("whitelist"));
        assertEquals(ProtectedSelection.State.ON, selection.state(List.of("whitelist")));
        assertEquals(ProtectedSelection.State.COVERED, selection.state(List.of("whitelist", "add")));
        assertEquals(ProtectedSelection.State.OFF, selection.state(List.of("kick")));
    }

    @Test
    void togglingAddsAndRemoves() {
        ProtectedSelection selection = new ProtectedSelection(List.of());
        selection.toggle(List.of("kick"));
        assertEquals(List.of("kick"), selection.entries());
        selection.toggle(List.of("kick"));
        assertEquals(List.of(), selection.entries());
    }

    @Test
    void aCoveredRowCannotBeToggledOnItsOwn() {
        ProtectedSelection selection = new ProtectedSelection(List.of("whitelist"));
        selection.toggle(List.of("whitelist", "add"));
        assertEquals(List.of("whitelist"), selection.entries());
    }

    @Test
    void aModRowTicksAllOrNone() {
        List<List<String>> roots = List.of(List.of("op"), List.of("kick"), List.of("ban"));
        ProtectedSelection selection = new ProtectedSelection(List.of("op"));
        assertEquals(ProtectedSelection.ModState.SOME, selection.modState(roots));
        selection.toggleMod(roots);
        assertEquals(ProtectedSelection.ModState.ALL, selection.modState(roots));
        selection.toggleMod(roots);
        assertEquals(ProtectedSelection.ModState.NONE, selection.modState(roots));
    }

    @Test
    void missingEntriesSurvive() {
        ProtectedSelection selection = new ProtectedSelection(List.of("op", "ftbteams.party"));
        assertEquals(List.of("ftbteams.party"), selection.missing(path -> path.get(0).equals("op")));
        selection.toggle(List.of("kick"));
        assertEquals(List.of("op", "ftbteams.party", "kick"), selection.entries());
    }

    @Test
    void entriesWithoutACatalogStayEditable() {
        ProtectedSelection selection = new ProtectedSelection(List.of("op", "ban"));
        selection.toggle(List.of("ban"));
        assertEquals(List.of("op"), selection.entries());
    }

    @Test
    void resetRestoresTheDefaults() {
        ProtectedSelection selection = new ProtectedSelection(List.of("kick"));
        selection.reset();
        assertEquals(ProtectionRules.DEFAULT_PROTECTED, selection.entries());
    }
}
