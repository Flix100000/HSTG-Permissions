package net.bananemdnsa.hstg_permissions.gametest;

import net.bananemdnsa.historystages.api.stage.StageStates;
import net.bananemdnsa.hstg_permissions.HSTGPermissions;
import net.bananemdnsa.hstg_permissions.config.PermissionsConfig;
import net.bananemdnsa.hstg_permissions.config.PermissionsConfig.MessageMode;
import net.bananemdnsa.hstg_permissions.gate.PermissionService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(HSTGPermissions.MODID)
@PrefixGameTestTemplate(false)
public final class GateTests {

    private static final String LOCKED = "message.hstg_permissions.command_locked";
    private static final String UNKNOWN = "command.unknown.command";

    private GateTests() {}

    @GameTest(template = "empty", batch = "hstgp_gate_value_lock")
    public static void aTakenValueStopsOnlyThatValue(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.createOp(helper);
        try {
            TestStages.global("gate_value_lock", "minecraft.gamemode.creative");
            TestStages.reload(server);
            Inbox inbox = TestPlayers.run(player, "gamemode creative");
            if (player.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
                helper.fail("/gamemode creative must be stopped by a stage that does not have it");
                return;
            }
            if (!inbox.mentions(LOCKED)) {
                helper.fail("the player must be told the command is locked, got " + inbox);
                return;
            }
            TestPlayers.run(player, "gamemode adventure");
            if (player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) {
                helper.fail("other values must stay open");
                return;
            }
            helper.succeed();
        } finally {
            TestPlayers.cleanUp(player);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_gate_value_grant")
    public static void aGivenValueOpensOnlyThatValueForANonOp(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.create(helper);
        String stage = TestStages.individual("gate_value_grant", "minecraft.gamemode.creative");
        try {
            TestStages.reload(server);
            StageStates.unlockIndividual(stage, player);
            PermissionService.forget(player.getUUID());
            if (!TestPlayers.canUse(player, "gamemode")) {
                helper.fail("an entry on a value must open /gamemode in the tree");
                return;
            }
            TestPlayers.run(player, "gamemode creative");
            if (player.gameMode.getGameModeForPlayer() != GameType.CREATIVE) {
                helper.fail("the given value must work without OP");
                return;
            }
            Inbox inbox = TestPlayers.run(player, "gamemode adventure");
            if (player.gameMode.getGameModeForPlayer() != GameType.CREATIVE) {
                helper.fail("a value no stage names must stay closed to a non-OP");
                return;
            }
            if (!inbox.mentions(UNKNOWN)) {
                helper.fail("a value outside the entry must answer like vanilla, got " + inbox);
                return;
            }
            helper.succeed();
        } finally {
            StageStates.relockIndividual(stage, player);
            TestPlayers.cleanUp(player);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_gate_locked_word")
    public static void aTakenWordIsNamedAsLocked(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.createOp(helper);
        try {
            TestStages.global("gate_locked_word", "minecraft.time");
            TestStages.reload(server);
            Inbox inbox = TestPlayers.run(player, "time set day");
            if (!inbox.mentions(LOCKED) || inbox.mentions(UNKNOWN)) {
                helper.fail("a hidden command must be named as locked in LOCKED mode, got " + inbox);
                return;
            }
            helper.succeed();
        } finally {
            TestPlayers.cleanUp(player);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_gate_vanilla_mode")
    public static void theVanillaModeSaysUnknownCommand(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.createOp(helper);
        try {
            PermissionsConfig.setMessageMode(MessageMode.VANILLA);
            TestStages.global("gate_vanilla_mode", "minecraft.time", "minecraft.gamemode.creative");
            TestStages.reload(server);
            Inbox word = TestPlayers.run(player, "time set day");
            Inbox value = TestPlayers.run(player, "gamemode creative");
            if (!word.mentions(UNKNOWN) || word.mentions(LOCKED) || !value.mentions(UNKNOWN) || value.mentions(LOCKED)) {
                helper.fail("VANILLA mode must look like an unknown command, got " + word + " / " + value);
                return;
            }
            helper.succeed();
        } finally {
            PermissionsConfig.setMessageMode(MessageMode.LOCKED);
            TestPlayers.cleanUp(player);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_gate_execute")
    public static void executeRunCannotSlipPastATakenValue(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.createOp(helper);
        try {
            TestStages.global("gate_execute", "minecraft.gamemode.creative");
            TestStages.reload(server);
            TestPlayers.run(player, "execute run gamemode creative");
            if (player.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
                helper.fail("execute ... run must not get around a taken value");
                return;
            }
            helper.succeed();
        } finally {
            TestPlayers.cleanUp(player);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_gate_console")
    public static void theConsoleRunsATakenValue(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        try {
            TestStages.global("gate_console", "minecraft.time.set");
            TestStages.reload(server);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "time set 1000");
            if (helper.getLevel().getDayTime() % 24000 != 1000) {
                helper.fail("the console must run a taken command");
                return;
            }
            helper.succeed();
        } finally {
            TestStages.removeAll(server);
        }
    }
}
