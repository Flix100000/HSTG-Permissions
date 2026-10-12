package net.bananemdnsa.hstg_permissions.gametest;

import net.bananemdnsa.hstg_permissions.HSTGPermissions;
import net.bananemdnsa.hstg_permissions.config.PermissionsConfig;
import net.bananemdnsa.hstg_permissions.config.PermissionsSettings;
import net.bananemdnsa.hstg_permissions.rules.CommandTarget;
import net.bananemdnsa.hstg_permissions.rules.LockedMessage;
import net.bananemdnsa.hstg_permissions.rules.ProtectedListValue;
import net.bananemdnsa.hstg_permissions.rules.ProtectionRules;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(HSTGPermissions.MODID)
@PrefixGameTestTemplate(false)
public final class SettingsTests {

    private static final String LOCKED = "message.hstg_permissions.command_locked";

    private SettingsTests() {}

    @GameTest(template = "empty", batch = "hstgp_settings_text")
    public static void aCustomMessageReplacesTheDefault(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.createOp(helper);
        try {
            PermissionsSettings.MESSAGE_TEXT.write().accept("&cNope {command}");
            TestStages.global("settings_text", "minecraft.time");
            TestStages.reload(server);
            Inbox inbox = TestPlayers.run(player, "time query daytime");
            if (!inbox.toString().contains("§cNope time query daytime") || inbox.mentions(LOCKED)) {
                helper.fail("the admin's own text must replace the default, got " + inbox);
                return;
            }
            helper.succeed();
        } finally {
            PermissionsSettings.MESSAGE_TEXT.write().accept(LockedMessage.DEFAULT);
            TestPlayers.cleanUp(player);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_settings_default_text")
    public static void theDefaultNamesTheStage(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.createOp(helper);
        try {
            PermissionsSettings.MESSAGE_TEXT.write().accept(LockedMessage.DEFAULT);
            TestStages.global("settings_default", "minecraft.time");
            TestStages.reload(server);
            Inbox inbox = TestPlayers.run(player, "time query daytime");
            if (!inbox.toString().contains("settings_default") || inbox.mentions(LOCKED)) {
                helper.fail("the default text must name the missing stage, got " + inbox);
                return;
            }
            helper.succeed();
        } finally {
            PermissionsSettings.MESSAGE_TEXT.write().accept(LockedMessage.DEFAULT);
            TestPlayers.cleanUp(player);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_settings_hidden_hints")
    public static void aStageWithoutHintsKeepsItsName(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.createOp(helper);
        try {
            PermissionsSettings.MESSAGE_TEXT.write().accept(LockedMessage.DEFAULT);
            TestStages.globalWithoutHints("settings_secret", "minecraft.time");
            TestStages.reload(server);
            Inbox inbox = TestPlayers.run(player, "time query daytime");
            if (!inbox.toString().contains(LockedMessage.HIDDEN) || inbox.toString().contains("settings_secret")) {
                helper.fail("a stage that hides its hints must show as " + LockedMessage.HIDDEN + ", got " + inbox);
                return;
            }
            helper.succeed();
        } finally {
            PermissionsSettings.MESSAGE_TEXT.write().accept(LockedMessage.DEFAULT);
            TestPlayers.cleanUp(player);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_settings_empty_text")
    public static void anEmptyMessageUsesTheLangKey(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestPlayers.createOp(helper);
        try {
            PermissionsSettings.MESSAGE_TEXT.write().accept("");
            TestStages.global("settings_empty_text", "minecraft.time");
            TestStages.reload(server);
            Inbox inbox = TestPlayers.run(player, "time query daytime");
            if (!inbox.mentions(LOCKED)) {
                helper.fail("an empty text must fall back to the lang key, got " + inbox);
                return;
            }
            helper.succeed();
        } finally {
            PermissionsSettings.MESSAGE_TEXT.write().accept(LockedMessage.DEFAULT);
            TestPlayers.cleanUp(player);
            TestStages.removeAll(server);
        }
    }

    @GameTest(template = "empty", batch = "hstgp_settings_protected")
    public static void theProtectedListIsWrittenAndApplied(GameTestHelper helper) {
        try {
            PermissionsSettings.PROTECTED.write().accept("time");
            if (!PermissionsConfig.protection().isProtected(CommandTarget.of("minecraft", "time"))) {
                helper.fail("a written entry must be protected");
                return;
            }
            if (PermissionsConfig.protection().isProtected(CommandTarget.of("minecraft", "op"))) {
                helper.fail("an entry that left the list must no longer be protected");
                return;
            }
            helper.succeed();
        } finally {
            PermissionsSettings.PROTECTED.write().accept(ProtectedListValue.join(ProtectionRules.DEFAULT_PROTECTED));
        }
    }
}
