package net.bananemdnsa.hstg_permissions.gate;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.tree.CommandNode;
import net.bananemdnsa.hstg_permissions.rules.Decision;
import net.bananemdnsa.hstg_permissions.tree.CommandTree;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

/** Which game modes a player may switch to, for the F3+F4 menu. */
public final class GameModeAccess {

    private GameModeAccess() {}

    public static List<String> allowedFor(ServerPlayer player) {
        CommandTree<CommandSourceStack> tree = PermissionService.tree();
        CommandNode<CommandSourceStack> gamemode = player.server.getCommands().getDispatcher().getRoot().getChild("gamemode");
        List<String> allowed = new ArrayList<>();
        if (gamemode == null) return allowed;
        boolean vanilla = CommandGuard.originalOf(gamemode).test(player.createCommandSourceStack());
        for (GameType type : GameType.values()) {
            Decision decision = tree == null ? Decision.DEFAULT
                    : PermissionService.decide(player, tree.targetFor(List.of("gamemode", type.getName())));
            boolean may = switch (decision.verdict()) {
                case GRANT -> true;
                case LOCK -> false;
                case DEFAULT -> vanilla;
            };
            if (may) allowed.add(type.getName());
        }
        return allowed;
    }
}
