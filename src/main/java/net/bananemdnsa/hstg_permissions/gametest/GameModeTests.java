package net.bananemdnsa.hstg_permissions.gametest;

import java.util.List;

import net.bananemdnsa.historystages.api.stage.StageStates;
import net.bananemdnsa.hstg_permissions.HSTGPermissions;
import net.bananemdnsa.hstg_permissions.gate.GameModeAccess;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(HSTGPermissions.MODID)
@PrefixGameTestTemplate(false)
public final class GameModeTests {

    private GameModeTests() {}

    @GameTest(template = "empty", batch = "hstgp_modes_op")
    public static void anOpWithoutEntriesMayUseEveryMode(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.createOp(helper);
        try {
            expect(helper, player, List.of("survival", "creative", "adventure", "spectator"));
        } finally {
            TestPlayers.cleanUp(player);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_modes_role")
    public static void aRoleMayUseOnlyItsModes(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.create(helper);
        String stage = TestStages.individual("modes_role", "minecraft.gamemode.survival");
        try {
            TestStages.reload(server);
            StageStates.unlockIndividual(stage, player);
            expect(helper, player, List.of("survival"));
        } finally {
            StageStates.relockIndividual(stage, player);
            TestPlayers.cleanUp(player);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_modes_taken")
    public static void aTakenModeIsMissingForAnOp(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.createOp(helper);
        TestStages.global("modes_taken", "minecraft.gamemode.creative");
        try {
            TestStages.reload(server);
            expect(helper, player, List.of("survival", "adventure", "spectator"));
        } finally {
            TestPlayers.cleanUp(player);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_modes_guest")
    public static void aGuestMayUseNoMode(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.create(helper);
        try {
            expect(helper, player, List.of());
        } finally {
            TestPlayers.cleanUp(player);
        }
    }

    private static void expect(GameTestHelper helper, ServerPlayer player, List<String> modes) {
        List<String> allowed = GameModeAccess.allowedFor(player);
        if (!allowed.equals(modes)) {
            helper.fail("expected the modes " + modes + ", got " + allowed);
            return;
        }
        helper.succeed();
    }
}
