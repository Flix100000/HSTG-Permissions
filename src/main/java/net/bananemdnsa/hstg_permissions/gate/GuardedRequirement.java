package net.bananemdnsa.hstg_permissions.gate;

import java.util.List;
import java.util.function.Predicate;

import net.bananemdnsa.hstg_permissions.rules.CommandTarget;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * Wraps one literal's requirement. Locked: no. Granted: yes. Otherwise Minecraft's own check,
 * along the whole path, because a grant further down may have opened a node above this one.
 */
final class GuardedRequirement implements Predicate<CommandSourceStack> {

    private final Predicate<CommandSourceStack> original;
    private final CommandTarget target;
    private final List<Predicate<CommandSourceStack>> vanillaChain;

    GuardedRequirement(Predicate<CommandSourceStack> original, CommandTarget target,
                       List<Predicate<CommandSourceStack>> vanillaChain) {
        this.original = original;
        this.target = target;
        this.vanillaChain = List.copyOf(vanillaChain);
    }

    Predicate<CommandSourceStack> original() {
        return original;
    }

    CommandTarget target() {
        return target;
    }

    @Override
    public boolean test(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        // Console, command blocks, functions and machines acting as players are never touched.
        if (player == null || player instanceof FakePlayer) return original.test(source);
        return PermissionService.canUse(player, target, () -> {
            for (Predicate<CommandSourceStack> step : vanillaChain) {
                if (!step.test(source)) return false;
            }
            return true;
        });
    }
}
