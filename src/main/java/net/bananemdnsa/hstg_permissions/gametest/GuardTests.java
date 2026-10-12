package net.bananemdnsa.hstg_permissions.gametest;

import java.util.List;

import com.mojang.brigadier.tree.CommandNode;
import net.bananemdnsa.historystages.api.stage.StageStates;
import net.bananemdnsa.hstg_permissions.HSTGPermissions;
import net.bananemdnsa.hstg_permissions.gate.CommandGuard;
import net.bananemdnsa.hstg_permissions.gate.PermissionService;
import net.bananemdnsa.hstg_permissions.tree.CommandTree;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(HSTGPermissions.MODID)
@PrefixGameTestTemplate(false)
public final class GuardTests {

    private GuardTests() {}

    @GameTest(template = "empty", batch = "hstgp_guard_op")
    public static void anOpIsLockedOutUntilTheStageIsUnlocked(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.createOp(helper);
        String stage = TestStages.global("guard_op", "minecraft.gamemode");
        try {
            TestStages.reload(server);
            if (TestPlayers.canUse(player, "gamemode")) {
                helper.fail("an OP without the stage must not be able to use /gamemode");
                return;
            }
            if (!TestPlayers.canUse(player, "time")) {
                helper.fail("a command no entry names must stay as it was");
                return;
            }
            StageStates.unlockGlobal(stage, helper.getLevel());
            PermissionService.invalidate();
            if (!TestPlayers.canUse(player, "gamemode")) {
                helper.fail("once the stage is unlocked /gamemode must open again");
                return;
            }
            helper.succeed();
        } finally {
            StageStates.relockGlobal(stage, helper.getLevel());
            TestPlayers.cleanUp(player);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_guard_console")
    public static void theConsoleIsNeverLocked(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        try {
            TestStages.global("guard_console", "minecraft.time");
            TestStages.reload(server);
            CommandNode<CommandSourceStack> gamemode = server.getCommands().getDispatcher().getRoot().getChild("gamemode");
            if (!gamemode.canUse(server.createCommandSourceStack())) {
                helper.fail("the console must get past every lock");
                return;
            }
            helper.succeed();
        } finally {
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_guard_history")
    public static void historyCommandsCannotBeTaken(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.createOp(helper);
        try {
            TestStages.global("guard_history", "historystages.history");
            TestStages.reload(server);
            if (!TestPlayers.canUse(player, "history")) {
                helper.fail("/history must never be lockable, not even by a stage that names it");
                return;
            }
            helper.succeed();
        } finally {
            TestPlayers.cleanUp(player);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_guard_history_run")
    public static void historyCommandsRunWhenTaken(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.createOp(helper);
        try {
            TestStages.global("guard_history_run", "historystages.history", "hstg_permissions.anything");
            TestStages.reload(server);
            // HSTG has no list command; zone info only reads the player's own selection.
            Inbox inbox = TestPlayers.run(player, "history zone info");
            if (inbox.mentions("message.hstg_permissions.command_locked") || inbox.mentions("command.unknown.command")) {
                helper.fail("/history must run while its stage is missing, got " + inbox);
                return;
            }
            if (!inbox.mentions("command.historystages.zone.none")) {
                helper.fail("/history zone info must have run and answered, got " + inbox);
                return;
            }
            helper.succeed();
        } finally {
            TestPlayers.cleanUp(player);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_guard_teleport")
    public static void lockingTeleportLocksTp(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.createOp(helper);
        try {
            TestStages.global("guard_teleport", "minecraft.teleport");
            TestStages.reload(server);
            if (TestPlayers.canUse(player, "teleport") || TestPlayers.canUse(player, "tp")) {
                helper.fail("a lock on teleport must reach tp as well");
                return;
            }
            helper.succeed();
        } finally {
            TestPlayers.cleanUp(player);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_guard_tp")
    public static void lockingTpLocksTeleport(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.createOp(helper);
        try {
            TestStages.global("guard_tp", "minecraft.tp");
            TestStages.reload(server);
            if (TestPlayers.canUse(player, "teleport")) {
                helper.fail("a lock on tp must reach teleport as well");
                return;
            }
            helper.succeed();
        } finally {
            TestPlayers.cleanUp(player);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_guard_rebuilt")
    public static void aRebuiltDispatcherIsGuardedToo(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.createOp(helper);
        try {
            TestStages.global("guard_rebuilt", "minecraft.gamemode");
            TestStages.reload(server);
            // What /reload does: a new Commands, which fires RegisterCommandsEvent on a new tree.
            Commands rebuilt = new Commands(Commands.CommandSelection.DEDICATED,
                    CommandBuildContext.simple(server.registryAccess(), server.getWorldData().enabledFeatures()));
            CommandNode<CommandSourceStack> gamemode = rebuilt.getDispatcher().getRoot().getChild("gamemode");
            if (gamemode.canUse(player.createCommandSourceStack())) {
                helper.fail("a dispatcher built by /reload must carry the lock too");
                return;
            }
            helper.succeed();
        } finally {
            CommandGuard.install(server.getCommands().getDispatcher());
            TestPlayers.cleanUp(player);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_guard_live_tree")
    public static void theTreeDescribesTheLiveDispatcher(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        try {
            // A /reload that never goes live, or has not yet: its dispatcher must not become the tree.
            new Commands(Commands.CommandSelection.DEDICATED,
                    CommandBuildContext.simple(server.registryAccess(), server.getWorldData().enabledFeatures()));
            CommandTree<CommandSourceStack> tree = PermissionService.tree();
            if (tree == null || tree.root() != server.getCommands().getDispatcher().getRoot()) {
                helper.fail("the tree must describe the dispatcher the server runs on");
                return;
            }
            helper.succeed();
        } finally {
            CommandGuard.install(server.getCommands().getDispatcher());
        }
    }

    @GameTest(template = "empty", batch = "hstgp_guard_star_grant")
    public static void aStarGrantOpensEverythingButProtectedCommands(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.create(helper);
        String stage = TestStages.individual("guard_star_grant", "*");
        try {
            TestStages.reload(server);
            if (TestPlayers.canUse(player, "gamemode")) {
                helper.fail("a player without OP and without the stage must not reach /gamemode");
                return;
            }
            StageStates.unlockIndividual(stage, player);
            PermissionService.forget(player.getUUID());
            if (!TestPlayers.canUse(player, "gamemode")) {
                helper.fail("holding * must open /gamemode without OP");
                return;
            }
            if (TestPlayers.canUse(player, "op")) {
                helper.fail("/op is protected and must stay closed even under *");
                return;
            }
            helper.succeed();
        } finally {
            StageStates.relockIndividual(stage, player);
            TestPlayers.cleanUp(player);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_guard_protected")
    public static void aStageCannotHandOutAProtectedCommand(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.create(helper);
        String stage = TestStages.individual("guard_protected", "minecraft.op", "minecraft.time");
        try {
            TestStages.reload(server);
            StageStates.unlockIndividual(stage, player);
            PermissionService.forget(player.getUUID());
            if (!TestPlayers.canUse(player, "time")) {
                helper.fail("the entry for /time in the same stage must work, or the check below proves nothing");
                return;
            }
            if (TestPlayers.canUse(player, "op")) {
                helper.fail("a stage must not hand out /op");
                return;
            }
            helper.succeed();
        } finally {
            StageStates.relockIndividual(stage, player);
            TestPlayers.cleanUp(player);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_guard_mods")
    public static void commandsAreFiledUnderTheirMod(GameTestHelper helper) {
        CommandTree<CommandSourceStack> tree = PermissionService.tree();
        if (tree == null) {
            helper.fail("the tree must be built by the time the server runs tests");
            return;
        }
        String gamemode = tree.targetFor(List.of("gamemode")).mod();
        String neoforge = tree.targetFor(List.of("neoforge")).mod();
        String history = tree.targetFor(List.of("history")).mod();
        if (!gamemode.equals("minecraft") || !neoforge.equals("neoforge") || !history.equals("historystages")) {
            helper.fail("expected minecraft/neoforge/historystages, got " + gamemode + "/" + neoforge + "/" + history);
            return;
        }
        helper.succeed();
    }
}
