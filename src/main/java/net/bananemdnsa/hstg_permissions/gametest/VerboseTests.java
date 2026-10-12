package net.bananemdnsa.hstg_permissions.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.bananemdnsa.hstg_permissions.HSTGPermissions;
import net.bananemdnsa.hstg_permissions.gate.PermissionCheck;
import net.bananemdnsa.hstg_permissions.gate.VerboseWatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(HSTGPermissions.MODID)
@PrefixGameTestTemplate(false)
public final class VerboseTests {

    private VerboseTests() {}

    @GameTest(template = "empty", batch = "hstgp_verbose_line")
    public static void aWatcherSeesACommandWithTheCheckLine(GameTestHelper helper) {
        ServerPlayer watcher = TestPlayers.createOp(helper);
        ServerPlayer player = TestPlayers.createOp(helper);
        List<Component> seen = new ArrayList<>();
        try {
            VerboseWatch.watch(watcher.getUUID(), Optional.empty(), seen::add);
            TestPlayers.run(player, "time set day");
            String expected = CheckTests.flatten(PermissionCheck.of(player, "time set day").line());
            if (seen.size() != 1 || !CheckTests.flatten(seen.get(0)).contains(expected)) {
                helper.fail("the watcher must get one line saying what check says, got " + seen);
                return;
            }
            helper.succeed();
        } finally {
            VerboseWatch.stop(watcher.getUUID());
            TestPlayers.cleanUp(watcher);
            TestPlayers.cleanUp(player);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_verbose_filter")
    public static void aFilterShowsOnlyThatPlayer(GameTestHelper helper) {
        ServerPlayer watcher = TestPlayers.createOp(helper);
        ServerPlayer watched = TestPlayers.createOp(helper);
        ServerPlayer other = TestPlayers.createOp(helper);
        List<Component> seen = new ArrayList<>();
        try {
            VerboseWatch.watch(watcher.getUUID(), Optional.of(watched.getUUID()), seen::add);
            TestPlayers.run(other, "time set day");
            if (!seen.isEmpty()) {
                helper.fail("a filtered watcher must not see other players, got " + seen);
                return;
            }
            TestPlayers.run(watched, "time set day");
            if (seen.size() != 1) {
                helper.fail("a filtered watcher must see the watched player, got " + seen);
                return;
            }
            helper.succeed();
        } finally {
            VerboseWatch.stop(watcher.getUUID());
            TestPlayers.cleanUp(watcher);
            TestPlayers.cleanUp(watched);
            TestPlayers.cleanUp(other);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_verbose_off")
    public static void offStopsIt(GameTestHelper helper) {
        ServerPlayer watcher = TestPlayers.createOp(helper);
        ServerPlayer player = TestPlayers.createOp(helper);
        List<Component> seen = new ArrayList<>();
        try {
            VerboseWatch.watch(watcher.getUUID(), Optional.empty(), seen::add);
            VerboseWatch.stop(watcher.getUUID());
            TestPlayers.run(player, "time set day");
            if (!seen.isEmpty()) {
                helper.fail("after off nothing may arrive, got " + seen);
                return;
            }
            helper.succeed();
        } finally {
            VerboseWatch.stop(watcher.getUUID());
            TestPlayers.cleanUp(watcher);
            TestPlayers.cleanUp(player);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_verbose_logout")
    public static void loggingOutStopsIt(GameTestHelper helper) {
        ServerPlayer watcher = TestPlayers.createOp(helper);
        try {
            VerboseWatch.watch(watcher.getUUID(), Optional.empty(), line -> {});
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(watcher));
            if (VerboseWatch.isWatching(watcher.getUUID())) {
                helper.fail("logging out must end the watch");
                return;
            }
            helper.succeed();
        } finally {
            VerboseWatch.stop(watcher.getUUID());
            TestPlayers.cleanUp(watcher);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_verbose_console")
    public static void theConsoleWatchesOnlyTheLog(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.createOp(helper);
        List<Component> seen = new ArrayList<>();
        try {
            VerboseWatch.watch(VerboseWatch.CONSOLE, Optional.empty(), seen::add);
            TestPlayers.run(player, "time set day");
            if (!VerboseWatch.isWatching(VerboseWatch.CONSOLE) || !seen.isEmpty()) {
                helper.fail("the console watches the log only, got " + seen);
                return;
            }
            helper.succeed();
        } finally {
            VerboseWatch.stop(VerboseWatch.CONSOLE);
            TestPlayers.cleanUp(player);
        }
    }
}
