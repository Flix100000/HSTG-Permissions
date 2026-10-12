package net.bananemdnsa.hstg_permissions.gate;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.ParseResults;
import net.bananemdnsa.historystages.api.stage.StageStates;
import net.bananemdnsa.hstg_permissions.config.PermissionsConfig;
import net.bananemdnsa.hstg_permissions.rules.Candidate;
import net.bananemdnsa.hstg_permissions.rules.Decision;
import net.bananemdnsa.hstg_permissions.rules.ProtectionRules;
import net.bananemdnsa.hstg_permissions.tree.CommandTree;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/** Answers "may this player run this line, and why" without running it, by the same rules as the gate. */
public final class PermissionCheck {

    public enum Outcome { ALLOWED_BY_STAGE, LOCKED, MINECRAFT_ALLOWS, MINECRAFT_DENIES, UNREADABLE }

    public record Result(Outcome outcome, Component line) {}

    private static final String KEY = "command.hstg_permissions.check.";

    private PermissionCheck() {}

    public static Result of(ServerPlayer player, String input) {
        String command = input.startsWith("/") ? input.substring(1) : input;
        MinecraftServer server = player.getServer();
        CommandTree<CommandSourceStack> tree = PermissionService.tree();
        // Parsed as the console: it sees every node, so a word hidden from the player still counts.
        ParseResults<CommandSourceStack> parse =
                server.getCommands().getDispatcher().parse(command, server.createCommandSourceStack());
        if (tree == null || parse.getReader().canRead() || !parse.getExceptions().isEmpty()) {
            return new Result(Outcome.UNREADABLE, describe(Outcome.UNREADABLE, null, player, command));
        }

        ProtectionRules rules = PermissionsConfig.protection();
        CommandSourceStack asPlayer = player.createCommandSourceStack();
        Outcome outcome = null;
        Decision deciding = null;
        Decision granted = null;
        boolean isProtected = false;
        boolean neverLockable = false;
        for (CommandStep step : CommandStep.of(parse, tree)) {
            Decision decision = PermissionService.decideOnce(player, step.target());
            if (decision.verdict() != Decision.Verdict.GRANT) {
                isProtected |= rules.isProtected(step.target());
                neverLockable |= rules.isNeverLockable(step.target());
            }
            if (decision.verdict() == Decision.Verdict.LOCK) {
                outcome = Outcome.LOCKED;
                deciding = decision;
                break;
            }
            if (decision.verdict() == Decision.Verdict.DEFAULT && !step.vanillaAllows(asPlayer)) {
                outcome = Outcome.MINECRAFT_DENIES;
                break;
            }
            if (decision.verdict() == Decision.Verdict.GRANT && granted == null) granted = decision;
        }
        if (outcome == null) {
            outcome = granted != null ? Outcome.ALLOWED_BY_STAGE : Outcome.MINECRAFT_ALLOWS;
            deciding = granted;
        }

        MutableComponent line = describe(outcome, deciding, player, command);
        if (isProtected) line.append(Component.translatable(KEY + "note.protected"));
        if (neverLockable) line.append(Component.translatable(KEY + "note.never_lockable"));
        return new Result(outcome, line);
    }

    private static MutableComponent describe(Outcome outcome, @Nullable Decision decision,
                                             ServerPlayer player, String command) {
        Component name = player.getDisplayName();
        return switch (outcome) {
            case ALLOWED_BY_STAGE -> {
                Candidate by = decision.grantedBy().orElseThrow();
                yield Component.translatable(KEY + "allowed", name, command,
                        StageStates.displayNameFor(by.stageId(), player), by.pattern().text());
            }
            case LOCKED -> Component.translatable(KEY + "locked", name, command,
                    stageNames(player, decision.lockingStages()));
            case MINECRAFT_ALLOWS -> Component.translatable(KEY + "minecraft_allows", name, command);
            case MINECRAFT_DENIES -> Component.translatable(KEY + "minecraft_denies", name, command);
            case UNREADABLE -> Component.translatable(KEY + "unreadable", name, command);
        };
    }

    // Unlike the locked message this is for whoever runs the server, so hidden stages are named too.
    private static String stageNames(ServerPlayer player, List<String> stageIds) {
        List<String> names = new ArrayList<>();
        for (String stageId : stageIds) names.add(StageStates.displayNameFor(stageId, player));
        return String.join(", ", names);
    }
}
