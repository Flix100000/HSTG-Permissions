package net.bananemdnsa.hstg_permissions.gametest;

import java.util.List;

import net.bananemdnsa.historystages.api.stage.StageStates;
import net.bananemdnsa.hstg_permissions.HSTGPermissions;
import net.bananemdnsa.hstg_permissions.config.PermissionsConfig;
import net.bananemdnsa.hstg_permissions.gate.PermissionCheck;
import net.bananemdnsa.hstg_permissions.gate.PermissionCheck.Outcome;
import net.bananemdnsa.hstg_permissions.gate.PermissionService;
import net.bananemdnsa.hstg_permissions.gate.VerboseWatch;
import net.bananemdnsa.hstg_permissions.rules.CommandTarget;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(HSTGPermissions.MODID)
@PrefixGameTestTemplate(false)
public final class CheckTests {

    private static final String NOTE_PROTECTED = "command.hstg_permissions.check.note.protected";
    private static final String NOTE_NEVER_LOCKABLE = "command.hstg_permissions.check.note.never_lockable";

    private CheckTests() {}

    @GameTest(template = "empty", batch = "hstgp_check_allowed")
    public static void aHeldStageAllowsAndIsNamed(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.create(helper);
        String stage = TestStages.individual("check_allowed", "minecraft.time");
        try {
            TestStages.reload(server);
            StageStates.unlockIndividual(stage, player);
            PermissionService.forget(player.getUUID());
            PermissionCheck.Result result = PermissionCheck.of(player, "time set day");
            String text = flatten(result.line());
            if (result.outcome() != Outcome.ALLOWED_BY_STAGE || !text.contains("check_allowed") || !text.contains("minecraft.time")) {
                helper.fail("a held stage must allow and be named with its entry, got " + result.outcome() + " " + text);
                return;
            }
            helper.succeed();
        } finally {
            StageStates.relockIndividual(stage, player);
            TestPlayers.cleanUp(player);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_check_locked")
    public static void aMissingStageLocksAndIsNamed(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer op = TestPlayers.createOp(helper);
        try {
            TestStages.global("check_locked", "minecraft.time");
            TestStages.reload(server);
            PermissionCheck.Result result = PermissionCheck.of(op, "time set day");
            if (result.outcome() != Outcome.LOCKED || !flatten(result.line()).contains("check_locked")) {
                helper.fail("a missing stage must lock and be named, got " + result.outcome() + " " + flatten(result.line()));
                return;
            }
            helper.succeed();
        } finally {
            TestPlayers.cleanUp(op);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_check_minecraft")
    public static void noStageLeavesItToMinecraft(GameTestHelper helper) {
        ServerPlayer guest = TestPlayers.create(helper);
        ServerPlayer op = TestPlayers.createOp(helper);
        try {
            Outcome forGuest = PermissionCheck.of(guest, "time set day").outcome();
            Outcome forOp = PermissionCheck.of(op, "time set day").outcome();
            if (forGuest != Outcome.MINECRAFT_DENIES || forOp != Outcome.MINECRAFT_ALLOWS) {
                helper.fail("without a stage Minecraft decides, got " + forGuest + " / " + forOp);
                return;
            }
            helper.succeed();
        } finally {
            TestPlayers.cleanUp(guest);
            TestPlayers.cleanUp(op);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_check_value")
    public static void aTypedValueCounts(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer op = TestPlayers.createOp(helper);
        try {
            TestStages.global("check_value", "minecraft.gamemode.creative");
            TestStages.reload(server);
            Outcome creative = PermissionCheck.of(op, "gamemode creative").outcome();
            Outcome adventure = PermissionCheck.of(op, "/gamemode adventure").outcome();
            if (creative != Outcome.LOCKED || adventure != Outcome.MINECRAFT_ALLOWS) {
                helper.fail("only the typed value may be locked, got " + creative + " / " + adventure);
                return;
            }
            helper.succeed();
        } finally {
            TestPlayers.cleanUp(op);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_check_execute")
    public static void executeRunIsCheckedToTheEnd(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer op = TestPlayers.createOp(helper);
        try {
            TestStages.global("check_execute", "minecraft.gamemode.creative");
            TestStages.reload(server);
            Outcome outcome = PermissionCheck.of(op, "execute run gamemode creative").outcome();
            if (outcome != Outcome.LOCKED) {
                helper.fail("the command behind execute ... run must be checked, got " + outcome);
                return;
            }
            helper.succeed();
        } finally {
            TestPlayers.cleanUp(op);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_check_protected")
    public static void theProtectedListIsNoted(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.create(helper);
        String stage = TestStages.individual("check_protected", "minecraft.op");
        try {
            TestStages.reload(server);
            StageStates.unlockIndividual(stage, player);
            PermissionService.forget(player.getUUID());
            PermissionCheck.Result result = PermissionCheck.of(player, "op hstgp_test");
            if (result.outcome() != Outcome.MINECRAFT_DENIES || !mentions(result.line(), NOTE_PROTECTED)) {
                helper.fail("a protected command must stay with Minecraft and say why, got "
                        + result.outcome() + " " + flatten(result.line()));
                return;
            }
            helper.succeed();
        } finally {
            StageStates.relockIndividual(stage, player);
            TestPlayers.cleanUp(player);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_check_history")
    public static void historyIsNotedAsNeverLockable(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer op = TestPlayers.createOp(helper);
        try {
            TestStages.global("check_history", "historystages.history");
            TestStages.reload(server);
            PermissionCheck.Result result = PermissionCheck.of(op, "history reload");
            if (result.outcome() == Outcome.LOCKED || !mentions(result.line(), NOTE_NEVER_LOCKABLE)) {
                helper.fail("/history must never be locked and say why, got "
                        + result.outcome() + " " + flatten(result.line()));
                return;
            }
            helper.succeed();
        } finally {
            TestPlayers.cleanUp(op);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_check_hidden")
    public static void hiddenStagesAreStillNamed(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer op = TestPlayers.createOp(helper);
        try {
            TestStages.globalWithoutHints("check_hidden", "minecraft.time");
            TestStages.reload(server);
            String text = flatten(PermissionCheck.of(op, "time set day").line());
            if (!text.contains("check_hidden") || text.contains("???")) {
                helper.fail("the check is for admins and must name hidden stages, got " + text);
                return;
            }
            helper.succeed();
        } finally {
            TestPlayers.cleanUp(op);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_check_unreadable")
    public static void nonsenseIsUnreadable(GameTestHelper helper) {
        ServerPlayer op = TestPlayers.createOp(helper);
        try {
            Outcome outcome = PermissionCheck.of(op, "gamemode banana").outcome();
            if (outcome != Outcome.UNREADABLE) {
                helper.fail("a line that is no command must say so, got " + outcome);
                return;
            }
            helper.succeed();
        } finally {
            TestPlayers.cleanUp(op);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_check_command_owner")
    public static void theCommandBelongsToThisAddon(GameTestHelper helper) {
        CommandTarget target = PermissionService.tree().targetFor(List.of("hstgperms"));
        if (!target.mod().equals(HSTGPermissions.MODID) || !PermissionsConfig.protection().isNeverLockable(target)) {
            helper.fail("/hstgperms must belong to this addon and never be lockable, got " + target.mod());
            return;
        }
        helper.succeed();
    }

    // Test players are not on the player list, so a selector finds nobody: this only proves the
    // command is wired up. What check says is covered above.
    @GameTest(template = "empty", batch = "hstgp_check_command_console")
    public static void checkAnswersTheConsole(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Inbox inbox = new Inbox();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSource(inbox),
                "hstgperms check @p time set day");
        if (!inbox.mentions("argument.entity.notfound.player")) {
            helper.fail("/hstgperms check must exist and look the player up, got " + inbox);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "hstgp_check_command_verbose")
    public static void verboseOnAndOffFromTheConsole(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        CommandSourceStack console = server.createCommandSourceStack().withSuppressedOutput();
        try {
            server.getCommands().performPrefixedCommand(console, "hstgperms verbose on");
            if (!VerboseWatch.isWatching(VerboseWatch.CONSOLE)) {
                helper.fail("verbose on from the console must watch");
                return;
            }
            server.getCommands().performPrefixedCommand(console, "hstgperms verbose off");
            if (VerboseWatch.isWatching(VerboseWatch.CONSOLE)) {
                helper.fail("verbose off must stop watching");
                return;
            }
            helper.succeed();
        } finally {
            VerboseWatch.stop(VerboseWatch.CONSOLE);
        }
    }

    // Keys and their arguments, so the test does not depend on the server having the lang file.
    static String flatten(Component component) {
        StringBuilder text = new StringBuilder();
        if (component.getContents() instanceof TranslatableContents translatable) {
            text.append(translatable.getKey()).append('(');
            for (Object arg : translatable.getArgs()) {
                text.append(arg instanceof Component nested ? flatten(nested) : String.valueOf(arg)).append(';');
            }
            text.append(')');
        } else {
            text.append(component.getContents() instanceof PlainTextContents plain
                    ? plain.text() : "");
        }
        for (Component sibling : component.getSiblings()) text.append(flatten(sibling));
        return text.toString();
    }

    static boolean mentions(Component component, String langKey) {
        return flatten(component).contains(langKey + "(");
    }
}
