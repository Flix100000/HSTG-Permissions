package net.bananemdnsa.hstg_permissions.gametest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.bananemdnsa.historystages.api.stage.StageStates;
import net.bananemdnsa.hstg_permissions.HSTGPermissions;
import net.bananemdnsa.hstg_permissions.config.PermissionsConfig;
import net.bananemdnsa.hstg_permissions.network.RanksPayload;
import net.bananemdnsa.hstg_permissions.rank.Rank;
import net.bananemdnsa.hstg_permissions.rank.RankUpdates;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(HSTGPermissions.MODID)
@PrefixGameTestTemplate(false)
public final class RankTests {

    private static final String NAME = "hstgp_test";

    private RankTests() {}

    @GameTest(template = "empty", batch = "hstgp_rank_held")
    public static void aHeldStageDecoratesTheNames(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.create(helper);
        String stage = TestStages.individualWithRank("rank_held", "&c[Admin]", "&7★", 10);
        try {
            TestStages.reload(server);
            StageStates.unlockIndividual(stage, player);
            RankUpdates.apply(server, List.of(player));
            String chat = player.getDisplayName().getString();
            String tab = tabName(player);
            if (!chat.equals("§c[Admin] " + NAME + " §7★") || !tab.equals(chat)) {
                helper.fail("a rank stage must decorate both names, got '" + chat + "' / '" + tab + "'");
                return;
            }
            helper.succeed();
        } finally {
            StageStates.relockIndividual(stage, player);
            TestPlayers.cleanUp(player);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_rank_lost")
    public static void losingTheStageRemovesIt(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.create(helper);
        String stage = TestStages.individualWithRank("rank_lost", "[Admin]", "", 10);
        try {
            TestStages.reload(server);
            StageStates.unlockIndividual(stage, player);
            RankUpdates.apply(server, List.of(player));
            StageStates.relockIndividual(stage, player);
            RankUpdates.apply(server, List.of(player));
            String chat = player.getDisplayName().getString();
            String tab = tabName(player);
            if (!chat.equals(NAME) || !tab.equals(NAME)) {
                helper.fail("a lost rank must leave the names, got '" + chat + "' / '" + tab + "'");
                return;
            }
            helper.succeed();
        } finally {
            StageStates.relockIndividual(stage, player);
            TestPlayers.cleanUp(player);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_rank_global")
    public static void aGlobalStageGivesNoRank(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.create(helper);
        String stage = TestStages.globalWithRank("rank_global", "[World]", "", 10);
        try {
            TestStages.reload(server);
            StageStates.unlockGlobal(stage, helper.getLevel());
            RankUpdates.apply(server, List.of(player));
            String chat = player.getDisplayName().getString();
            if (!chat.equals(NAME)) {
                helper.fail("a global stage must give no rank, got '" + chat + "'");
                return;
            }
            helper.succeed();
        } finally {
            StageStates.relockGlobal(stage, helper.getLevel());
            TestPlayers.cleanUp(player);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_rank_chat_off")
    public static void theChatSwitchTurnsItOff(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.create(helper);
        String stage = TestStages.individualWithRank("rank_chat_off", "[Admin]", "", 10);
        try {
            PermissionsConfig.setRankInChat(false);
            TestStages.reload(server);
            StageStates.unlockIndividual(stage, player);
            RankUpdates.apply(server, List.of(player));
            String chat = player.getDisplayName().getString();
            String tab = tabName(player);
            if (!chat.equals(NAME) || !tab.equals("[Admin] " + NAME)) {
                helper.fail("only the chat may lose the rank, got '" + chat + "' / '" + tab + "'");
                return;
            }
            helper.succeed();
        } finally {
            PermissionsConfig.setRankInChat(true);
            StageStates.relockIndividual(stage, player);
            TestPlayers.cleanUp(player);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_rank_none")
    public static void noRankLeavesTheNameAlone(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.create(helper);
        try {
            RankUpdates.apply(server, List.of(player));
            String chat = player.getDisplayName().getString();
            if (!chat.equals(NAME) || player.getTabListDisplayName() != null) {
                helper.fail("without a rank nothing may change, got '" + chat + "' / " + player.getTabListDisplayName());
                return;
            }
            helper.succeed();
        } finally {
            TestPlayers.cleanUp(player);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_rank_payload")
    public static void thePayloadCarriesTheRank(GameTestHelper helper) {
        UUID id = new UUID(0, 7);
        try {
            RanksPayload shown = RankUpdates.payload(false, Map.of(id, new Rank("[A]", "x")));
            PermissionsConfig.setRankInNameTag(false);
            RanksPayload hidden = RankUpdates.payload(false, Map.of(id, new Rank("[A]", "x")));
            RanksPayload.Entry entry = shown.entries().get(0);
            if (!entry.player().equals(id) || !entry.prefix().equals("[A]") || !entry.suffix().equals("x")) {
                helper.fail("the payload must carry the rank, got " + shown);
                return;
            }
            RanksPayload.Entry blank = hidden.entries().get(0);
            if (!blank.prefix().isEmpty() || !blank.suffix().isEmpty()) {
                helper.fail("with name tags off the payload must clear the rank, got " + hidden);
                return;
            }
            helper.succeed();
        } finally {
            PermissionsConfig.setRankInNameTag(true);
        }
    }

    private static String tabName(ServerPlayer player) {
        Component tab = player.getTabListDisplayName();
        return tab == null ? NAME : tab.getString();
    }
}
