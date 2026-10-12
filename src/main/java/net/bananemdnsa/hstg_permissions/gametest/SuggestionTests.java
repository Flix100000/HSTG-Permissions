package net.bananemdnsa.hstg_permissions.gametest;

import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.Suggestion;
import net.bananemdnsa.historystages.api.stage.StageStates;
import net.bananemdnsa.hstg_permissions.HSTGPermissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(HSTGPermissions.MODID)
@PrefixGameTestTemplate(false)
public final class SuggestionTests {

    private SuggestionTests() {}

    static List<String> suggest(MinecraftServer server, CommandSourceStack source, String input) {
        CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
        return dispatcher.getCompletionSuggestions(dispatcher.parse(input, source)).join().getList().stream()
                .map(Suggestion::getText).sorted().toList();
    }

    @GameTest(template = "empty", batch = "hstgp_suggest_role")
    public static void aRoleIsOfferedOnlyItsModes(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.create(helper);
        String stage = TestStages.individual("suggest_role", "minecraft.gamemode.survival", "minecraft.gamemode.spectator");
        try {
            TestStages.reload(server);
            StageStates.unlockIndividual(stage, player);
            List<String> offered = suggest(server, player.createCommandSourceStack(), "gamemode ");
            if (!offered.equals(List.of("spectator", "survival"))) {
                helper.fail("a non-op with survival and spectator must be offered exactly those, got " + offered);
                return;
            }
            helper.succeed();
        } finally {
            StageStates.relockIndividual(stage, player);
            TestPlayers.cleanUp(player);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_suggest_op")
    public static void anOpIsOfferedAllButTheTakenMode(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer op = TestPlayers.createOp(helper);
        TestStages.global("suggest_op", "minecraft.gamemode.creative");
        try {
            TestStages.reload(server);
            List<String> offered = suggest(server, op.createCommandSourceStack(), "gamemode ");
            if (!offered.equals(List.of("adventure", "spectator", "survival"))) {
                helper.fail("creative belongs to a stage the op lacks, so it must not be offered, got " + offered);
                return;
            }
            helper.succeed();
        } finally {
            TestPlayers.cleanUp(op);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_suggest_console")
    public static void theConsoleIsOfferedEveryMode(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        TestStages.global("suggest_console", "minecraft.gamemode.creative");
        try {
            TestStages.reload(server);
            List<String> offered = suggest(server, server.createCommandSourceStack(), "gamemode ");
            if (!offered.containsAll(List.of("adventure", "creative", "spectator", "survival"))) {
                helper.fail("the console is never checked, so it must be offered every mode, got " + offered);
                return;
            }
            helper.succeed();
        } finally {
            TestStages.removeAll(server);
        }
    }
}
