package net.bananemdnsa.hstg_permissions.diagnostics;

import java.util.Locale;

import net.bananemdnsa.historystages.api.diagnostics.RegisterReportSectionsEvent;
import net.bananemdnsa.historystages.api.diagnostics.RegisterStageChecksEvent;
import net.bananemdnsa.hstg_permissions.category.CommandsCategory;
import net.bananemdnsa.hstg_permissions.config.PermissionsConfig;
import net.bananemdnsa.hstg_permissions.gate.PermissionService;
import net.bananemdnsa.hstg_permissions.rules.EntryProblems;
import net.bananemdnsa.hstg_permissions.rules.PermissionEntry;
import net.bananemdnsa.hstg_permissions.tree.CommandTree;
import net.minecraft.commands.CommandSourceStack;

public final class PermissionDiagnostics {

    private PermissionDiagnostics() {}

    public static void registerChecks(RegisterStageChecksEvent event) {
        event.register("hstg_permissions:commands", ctx -> {
            CommandTree<CommandSourceStack> tree = PermissionService.tree();
            for (PermissionEntry entry : CommandsCategory.get().read(ctx.entry())) {
                for (EntryProblems.Problem problem : EntryProblems.check(entry, PermissionsConfig.protection(),
                        tree == null ? null : tree::exists,
                        tree == null ? null : pattern -> tree.targetFor(pattern.segments()).mod())) {
                    if (problem.level() == EntryProblems.Level.ERROR) {
                        ctx.problems().error("Commands", problem.message());
                    } else {
                        ctx.problems().warn("Commands", problem.message());
                    }
                }
            }
        });
    }

    public static void registerReport(RegisterReportSectionsEvent event) {
        event.register("hstg_permissions:commands", "HSTG: Permissions", out -> {
            out.value("Protected commands", String.join(", ", PermissionsConfig.protectedCommands()));
            if (!PermissionsConfig.ignoredProtectedCommands().isEmpty()) {
                // Quoted, or a blank entry would not show at all.
                out.value("Ignored protected entries", String.join(", ",
                        PermissionsConfig.ignoredProtectedCommands().stream().map(entry -> "'" + entry + "'").toList()));
            }
            out.value("Locked message", PermissionsConfig.messageMode().name().toLowerCase(Locale.ROOT));
            CommandTree<CommandSourceStack> tree = PermissionService.tree();
            if (tree == null) {
                out.line("Command tree not built yet");
                return;
            }
            out.value("Guarded command words", tree.literalCount());
            out.value("Mods with commands", String.join(", ", tree.mods()));
            if (!tree.unaddressable().isEmpty()) {
                out.value("Cannot be addressed (dot in the name)", String.join(", ", tree.unaddressable()));
            }
        });
    }
}
