package net.bananemdnsa.hstg_permissions.editor;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.bananemdnsa.hstg_permissions.catalog.CommandCatalog;
import net.bananemdnsa.hstg_permissions.rules.EntryProblems;
import net.bananemdnsa.hstg_permissions.rules.PermissionEntry;
import net.bananemdnsa.hstg_permissions.rules.ProtectionRules;

/** The one problem a row shows: an error before a warning. */
public record EntryStatus(@Nullable EntryProblems.Problem problem) {

    // Without the server's answer the editor knows neither its commands nor its protected list,
    // so only what is wrong with the entry itself can be said.
    private static final ProtectionRules NOTHING_KNOWN = new ProtectionRules(List.of(), Set.of());

    public static EntryStatus of(PermissionEntry entry, @Nullable CommandCatalog catalog) {
        List<EntryProblems.Problem> problems = catalog == null
                ? EntryProblems.check(entry, NOTHING_KNOWN, null, null)
                : catalog.problems(entry);
        for (EntryProblems.Problem problem : problems) {
            if (problem.level() == EntryProblems.Level.ERROR) return new EntryStatus(problem);
        }
        return new EntryStatus(problems.isEmpty() ? null : problems.get(0));
    }

    @Nullable
    public EntryProblems.Level level() {
        return problem == null ? null : problem.level();
    }
}
