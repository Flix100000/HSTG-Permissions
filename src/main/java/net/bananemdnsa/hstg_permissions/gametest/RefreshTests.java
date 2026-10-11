package net.bananemdnsa.hstg_permissions.gametest;

import java.util.UUID;

import net.bananemdnsa.historystages.api.stage.StageStates;
import net.bananemdnsa.hstg_permissions.HSTGPermissions;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(HSTGPermissions.MODID)
@PrefixGameTestTemplate(false)
public final class RefreshTests {

    private RefreshTests() {}

    @GameTest(template = "empty", batch = "hstgp_refresh_global")
    public static void aGlobalUnlockOpensTheCommandOnItsOwn(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.createOp(helper);
        String stage = TestStages.global("refresh_global", "lock minecraft.gamemode");
        try {
            TestStages.reload(server);
            if (TestPlayers.canUse(player, "gamemode")) {
                helper.fail("the lock must hold before the unlock");
                return;
            }
            StageStates.unlockGlobal(stage, helper.getLevel());
            if (!TestPlayers.canUse(player, "gamemode")) {
                helper.fail("the unlock event must clear the remembered answer");
                return;
            }
            helper.succeed();
        } finally {
            StageStates.relockGlobal(stage, helper.getLevel());
            TestPlayers.cleanUp(player);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_refresh_individual")
    public static void anIndividualUnlockReachesOnlyThatPlayer(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer granted = TestPlayers.create(helper);
        ServerPlayer other = TestPlayers.create(helper);
        String stage = TestStages.individual("refresh_individual", "grant minecraft.time");
        try {
            TestStages.reload(server);
            if (TestPlayers.canUse(granted, "time") || TestPlayers.canUse(other, "time")) {
                helper.fail("nobody has the stage yet");
                return;
            }
            StageStates.unlockIndividual(stage, granted);
            if (!TestPlayers.canUse(granted, "time")) {
                helper.fail("the player who got the stage must see /time straight away");
                return;
            }
            if (TestPlayers.canUse(other, "time")) {
                helper.fail("another player must not profit from it");
                return;
            }
            helper.succeed();
        } finally {
            StageStates.relockIndividual(stage, granted);
            TestPlayers.cleanUp(granted);
            TestPlayers.cleanUp(other);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_refresh_offline")
    public static void anUnlockForAnOfflinePlayerIsHarmless(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        UUID offline = UUID.randomUUID();
        String stage = TestStages.individual("refresh_offline", "grant minecraft.time");
        try {
            TestStages.reload(server);
            StageStates.unlockIndividual(stage, offline, server);
            StageStates.relockIndividual(stage, offline, server);
            helper.succeed();
        } catch (RuntimeException e) {
            helper.fail("a stage change for an offline player must not throw: " + e);
        } finally {
            TestStages.removeAll(server);
        }
    }
}
