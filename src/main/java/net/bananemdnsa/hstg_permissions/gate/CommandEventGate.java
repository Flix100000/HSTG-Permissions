package net.bananemdnsa.hstg_permissions.gate;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.ParseResults;
import net.bananemdnsa.historystages.api.diagnostics.Kind;
import net.bananemdnsa.historystages.api.diagnostics.RuntimeCategory;
import net.bananemdnsa.historystages.api.diagnostics.RuntimeLog;
import net.bananemdnsa.historystages.api.lock.LockFeedbacks;
import net.bananemdnsa.historystages.api.stage.StageStates;
import net.bananemdnsa.hstg_permissions.config.PermissionsConfig;
import net.bananemdnsa.hstg_permissions.config.PermissionsConfig.MessageMode;
import net.bananemdnsa.hstg_permissions.rules.CommandTarget;
import net.bananemdnsa.hstg_permissions.rules.Decision;
import net.bananemdnsa.hstg_permissions.rules.LockedMessage;
import net.bananemdnsa.hstg_permissions.tree.CommandTree;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.CommandEvent;

/**
 * The check on a finished parse. The tree guard decides what a player sees; only here are the
 * typed values known, so a lock on {@code gamemode.creative} is enforced here.
 */
public final class CommandEventGate {

    static final RuntimeCategory LOG = RuntimeCategory.of("hstg_permissions:commands", "Commands", Kind.LOCK);

    private CommandEventGate() {}

    public static void onCommand(CommandEvent event) {
        ParseResults<CommandSourceStack> parse = event.getParseResults();
        CommandSourceStack source = parse.getContext().getSource();
        ServerPlayer player = source.getPlayer();
        CommandTree<CommandSourceStack> tree = PermissionService.tree();
        if (player == null || player instanceof FakePlayer || tree == null) return;
        String input = parse.getReader().getString();
        VerboseWatch.onCommand(player, input);

        if (parse.getReader().canRead() || !parse.getExceptions().isEmpty()) {
            // The parse stopped short, most likely at a word a lock hides. Vanilla's own message
            // covers that unless the server wants locks named as locks.
            if (PermissionsConfig.messageMode() != MessageMode.LOCKED) return;
            for (CommandTarget target : tree.targetsForInput(input)) {
                Decision decision = PermissionService.decideOnce(player, target);
                if (decision.verdict() == Decision.Verdict.LOCK) {
                    block(event, source, player, decision, input);
                    return;
                }
            }
            return;
        }

        for (CommandStep step : CommandStep.of(parse, tree)) {
            Decision decision = PermissionService.decideOnce(player, step.target());
            if (decision.verdict() == Decision.Verdict.LOCK) {
                block(event, source, player, decision, input);
                return;
            }
            if (decision.verdict() == Decision.Verdict.DEFAULT && !step.vanillaAllows(source)) {
                // Only reachable through a grant further down that opened the way to this node.
                event.setCanceled(true);
                source.sendFailure(Component.translatable("command.unknown.command"));
                return;
            }
        }
    }

    private static void block(CommandEvent event, CommandSourceStack source, ServerPlayer player,
                              Decision decision, String input) {
        event.setCanceled(true);
        RuntimeLog.write(LOG, player, "Blocked /" + input);
        if (PermissionsConfig.messageMode() == MessageMode.VANILLA) {
            source.sendFailure(Component.translatable("command.unknown.command"));
            return;
        }
        String stages = stageNames(player, decision.lockingStages());
        String command = input.startsWith("/") ? input.substring(1) : input;
        MutableComponent message = LockedMessage.format(PermissionsConfig.lockedMessageText(), stages, command)
                .<MutableComponent>map(Component::literal)
                .orElseGet(() -> {
                    MutableComponent fallback = Component.translatable("message.hstg_permissions.command_locked");
                    Component required = LockFeedbacks.requiredSuffix(player, decision.lockingStages());
                    if (required != null) fallback.append(required);
                    return fallback;
                });
        source.sendFailure(message);
    }

    // Names only where every stage allows hints, the same rule as LockFeedbacks.requiredSuffix.
    private static String stageNames(ServerPlayer player, List<String> stageIds) {
        List<String> names = new ArrayList<>();
        for (String stageId : stageIds) {
            if (!StageStates.showsLockHints(stageId)) return LockedMessage.HIDDEN;
            names.add(StageStates.displayNameFor(stageId, player));
        }
        return names.isEmpty() ? LockedMessage.HIDDEN : String.join(", ", names);
    }
}
