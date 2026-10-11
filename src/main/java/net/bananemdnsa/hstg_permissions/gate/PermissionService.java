package net.bananemdnsa.hstg_permissions.gate;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

import com.mojang.brigadier.tree.RootCommandNode;
import net.bananemdnsa.historystages.api.lock.CategoryLocks;
import net.bananemdnsa.historystages.api.lock.LockHit;
import net.bananemdnsa.historystages.api.stage.StageStates;
import net.bananemdnsa.historystages.data.StageEntry;
import net.bananemdnsa.hstg_permissions.category.CommandsCategory;
import net.bananemdnsa.hstg_permissions.config.PermissionsConfig;
import net.bananemdnsa.hstg_permissions.rules.Candidate;
import net.bananemdnsa.hstg_permissions.rules.CommandTarget;
import net.bananemdnsa.hstg_permissions.rules.Decision;
import net.bananemdnsa.hstg_permissions.rules.EntryPattern;
import net.bananemdnsa.hstg_permissions.rules.Mode;
import net.bananemdnsa.hstg_permissions.rules.PermissionEntry;
import net.bananemdnsa.hstg_permissions.rules.Resolver;
import net.bananemdnsa.hstg_permissions.tree.CommandTree;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Answers "may this player use this command" and remembers the answer until a stage or the
 * definitions change. Sending the command tree asks once per literal, so the cache is what keeps a
 * login from scanning every stage a few hundred times.
 */
public final class PermissionService {

    private static final Map<UUID, Map<String, Decision>> DECISIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Boolean>> GRANTS_BELOW = new ConcurrentHashMap<>();
    private static volatile List<EntryPattern> grantPatterns;
    private static volatile CommandTree<CommandSourceStack> tree;

    private PermissionService() {}

    public static boolean canUse(ServerPlayer player, CommandTarget target, BooleanSupplier vanilla) {
        Decision decision = decide(player, target);
        if (decision.verdict() == Decision.Verdict.GRANT) return true;
        // A grant further down has to open the way to it, or /gamemode creative could never be
        // granted without all of /gamemode.
        if (grantBelow(player, target)) return true;
        return decision.verdict() == Decision.Verdict.DEFAULT && vanilla.getAsBoolean();
    }

    public static Decision decide(ServerPlayer player, CommandTarget target) {
        return DECISIONS.computeIfAbsent(player.getUUID(), id -> new ConcurrentHashMap<>())
                .computeIfAbsent(target.key(), key -> decideUncached(player.getUUID(), target));
    }

    /** Typed values are unbounded, so the command gate must not fill the cache with them. */
    public static Decision decideOnce(ServerPlayer player, CommandTarget target) {
        return decideUncached(player.getUUID(), target);
    }

    private static Decision decideUncached(UUID player, CommandTarget target) {
        List<Candidate> candidates = new ArrayList<>();
        for (LockHit<PermissionEntry> hit : CategoryLocks.hits(CommandsCategory.get(), target, player)) {
            Optional<EntryPattern> pattern = hit.entry().pattern();
            Optional<Mode> mode = hit.entry().parsedMode();
            if (pattern.isEmpty() || mode.isEmpty()) continue;
            // locks() counts interchangeable stages in; !unlocked() would ignore them.
            boolean applies = mode.get() == Mode.LOCK ? hit.locks() : hit.unlocked();
            candidates.add(new Candidate(pattern.get(), mode.get(), applies, hit.stageId()));
        }
        return Resolver.decide(target, candidates, PermissionsConfig.protection());
    }

    private static boolean grantBelow(ServerPlayer player, CommandTarget target) {
        return GRANTS_BELOW.computeIfAbsent(player.getUUID(), id -> new ConcurrentHashMap<>())
                .computeIfAbsent(target.key(), key -> {
                    for (EntryPattern grant : grantPatterns()) {
                        if (!grant.isStrictlyBelow(target)) continue;
                        if (decide(player, deeper(target, grant)).verdict() == Decision.Verdict.GRANT) return true;
                    }
                    return false;
                });
    }

    private static CommandTarget deeper(CommandTarget above, EntryPattern grant) {
        CommandTree<CommandSourceStack> current = tree();
        return current != null
                ? current.targetFor(grant.segments())
                : new CommandTarget(above.mod(), List.of(grant.segments()));
    }

    private static List<EntryPattern> grantPatterns() {
        List<EntryPattern> patterns = grantPatterns;
        if (patterns == null) {
            Set<EntryPattern> found = new LinkedHashSet<>();
            for (StageEntry stage : StageStates.loadedStages()) {
                for (PermissionEntry entry : CommandsCategory.get().read(stage)) {
                    if (entry.parsedMode().orElse(null) != Mode.GRANT) continue;
                    entry.pattern().filter(p -> p.kind() == EntryPattern.Kind.PATH).ifPresent(found::add);
                }
            }
            patterns = List.copyOf(found);
            grantPatterns = patterns;
        }
        return patterns;
    }

    /** After anything that can change any player's answer. */
    public static void invalidate() {
        DECISIONS.clear();
        GRANTS_BELOW.clear();
        grantPatterns = null;
    }

    /** After something that can only change this player's answer. */
    public static void forget(UUID player) {
        DECISIONS.remove(player);
        GRANTS_BELOW.remove(player);
    }

    /**
     * The tree of the dispatcher the server is running on. /reload builds its dispatcher on a worker
     * thread and only swaps it in later, if at all, so the tree follows the live root instead of
     * whatever RegisterCommandsEvent saw last.
     */
    @Nullable
    public static synchronized CommandTree<CommandSourceStack> tree() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return tree;
        RootCommandNode<CommandSourceStack> liveRoot = server.getCommands().getDispatcher().getRoot();
        if (tree == null || tree.root() != liveRoot) {
            tree = CommandTree.build(liveRoot, ModAttribution::modOf, KnownValues::of);
            invalidate();
        }
        return tree;
    }
}
