package net.bananemdnsa.hstg_permissions.network;

import java.util.List;
import java.util.Optional;

import net.bananemdnsa.hstg_permissions.catalog.CommandCatalog;
import net.bananemdnsa.hstg_permissions.config.PermissionsConfig;
import net.bananemdnsa.hstg_permissions.gate.PermissionService;
import net.bananemdnsa.hstg_permissions.rules.ProtectionRules;
import net.bananemdnsa.hstg_permissions.tree.CommandTree;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/** The server half of the editor's question: who may ask, and what they get. */
public final class CatalogAnswer {

    private CatalogAnswer() {}

    /** Empty for anyone who may not open the editor, and while the server has no tree yet. */
    public static Optional<CommandCatalog> forPlayer(ServerPlayer player) {
        if (!player.hasPermissions(2)) return Optional.empty();
        CommandTree<CommandSourceStack> tree = PermissionService.tree();
        if (tree == null) return Optional.empty();
        // Sent already read, so the client does not need the server's list of mod ids to read it.
        List<String> protectedPaths = PermissionsConfig.protection().protectedPaths().stream()
                .map(pattern -> String.join(".", pattern.segments()))
                .toList();
        return Optional.of(CommandCatalog.of(tree, protectedPaths, ProtectionRules.NEVER_LOCKABLE_MODS));
    }
}
