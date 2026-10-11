package net.bananemdnsa.hstg_permissions.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import net.bananemdnsa.hstg_permissions.catalog.CommandCatalog;
import net.bananemdnsa.hstg_permissions.rules.EntryProblems;
import net.bananemdnsa.hstg_permissions.rules.PermissionEntry;
import net.bananemdnsa.hstg_permissions.tree.SampleTree;
import org.junit.jupiter.api.Test;

class EntryStatusTest {

    private final CommandCatalog catalog = SampleTree.catalog(List.of("op"));

    @Test
    void withoutACatalogNothingIsCalledMissing() {
        assertNull(EntryStatus.of(new PermissionEntry("minecraft.gamemode.creatve", "lock"), null).problem());
        assertNull(EntryStatus.of(new PermissionEntry("minecraft.op", "grant"), null).problem());
    }

    @Test
    void brokenEntriesShowEvenWithoutACatalog() {
        assertEquals(EntryProblems.Kind.BAD_MODE,
                EntryStatus.of(new PermissionEntry("minecraft.gamemode", "deny"), null).problem().kind());
    }

    @Test
    void anErrorWinsOverAWarning() {
        EntryStatus status = EntryStatus.of(new PermissionEntry("minecraft.op", "grant"), catalog);
        assertEquals(EntryProblems.Kind.PROTECTED_GRANT, status.problem().kind());
        assertEquals(EntryProblems.Level.ERROR, status.level());
    }

    @Test
    void aMissingPathIsAWarning() {
        EntryStatus status = EntryStatus.of(new PermissionEntry("waystones.waystone", "lock"), catalog);
        assertEquals(EntryProblems.Kind.NOT_FOUND, status.problem().kind());
        assertEquals(EntryProblems.Level.WARN, status.level());
        assertNull(EntryStatus.of(new PermissionEntry("minecraft.gamemode.creative", "lock"), catalog).level());
    }
}
