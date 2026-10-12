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
        assertNull(EntryStatus.of(new PermissionEntry("minecraft.gamemode.creatve", null), null).problem());
        assertNull(EntryStatus.of(new PermissionEntry("minecraft.op", null), null).problem());
    }

    @Test
    void brokenEntriesShowEvenWithoutACatalog() {
        assertEquals(EntryProblems.Kind.BAD_PATH,
                EntryStatus.of(new PermissionEntry("minecraft..gamemode", null), null).problem().kind());
    }

    @Test
    void aProtectedCommandIsAWarning() {
        EntryStatus status = EntryStatus.of(new PermissionEntry("minecraft.op", null), catalog);
        assertEquals(EntryProblems.Kind.PROTECTED, status.problem().kind());
        assertEquals(EntryProblems.Level.WARN, status.level());
    }

    @Test
    void aMissingPathIsAWarning() {
        EntryStatus status = EntryStatus.of(new PermissionEntry("waystones.waystone", null), catalog);
        assertEquals(EntryProblems.Kind.NOT_FOUND, status.problem().kind());
        assertEquals(EntryProblems.Level.WARN, status.level());
        assertNull(EntryStatus.of(new PermissionEntry("minecraft.gamemode.creative", null), catalog).level());
    }
}
