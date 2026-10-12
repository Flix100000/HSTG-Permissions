package net.bananemdnsa.hstg_permissions.gametest;

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
public final class RuleTests {

    private RuleTests() {}

    @GameTest(template = "empty", batch = "hstgp_rule_op")
    public static void aStageTakesACommandFromAnOpWhoLacksIt(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer op = TestPlayers.createOp(helper);
        String stage = TestStages.individual("rule_op", "minecraft.time");
        try {
            TestStages.reload(server);
            if (TestPlayers.canUse(op, "time")) {
                helper.fail("an operator without the stage must lose /time once it belongs to a stage");
                return;
            }
            StageStates.unlockIndividual(stage, op);
            if (!TestPlayers.canUse(op, "time")) {
                helper.fail("with the stage the operator must have /time again");
                return;
            }
            helper.succeed();
        } finally {
            StageStates.relockIndividual(stage, op);
            TestPlayers.cleanUp(op);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_rule_everything")
    public static void theEverythingStageTakesNothing(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer op = TestPlayers.createOp(helper);
        ServerPlayer guest = TestPlayers.create(helper);
        TestStages.individual("rule_everything", "*");
        try {
            TestStages.reload(server);
            if (!TestPlayers.canUse(op, "time")) {
                helper.fail("a * stage nobody has must not take /time from an operator");
                return;
            }
            if (TestPlayers.canUse(guest, "time")) {
                helper.fail("nor may it hand /time to a player without it");
                return;
            }
            helper.succeed();
        } finally {
            TestPlayers.cleanUp(op);
            TestPlayers.cleanUp(guest);
            TestStages.removeAll(server);
        }
    }
}
