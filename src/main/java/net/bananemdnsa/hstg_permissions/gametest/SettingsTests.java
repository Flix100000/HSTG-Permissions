package net.bananemdnsa.hstg_permissions.gametest;

import net.bananemdnsa.hstg_permissions.HSTGPermissions;
import net.bananemdnsa.hstg_permissions.config.PermissionsConfig;
import net.bananemdnsa.hstg_permissions.config.PermissionsSettings;
import net.bananemdnsa.hstg_permissions.rules.CommandTarget;
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
            PermissionsSettings.MESSAGE_TEXT.write().accept("&cNope");
            TestStages.global("settings_text", "minecraft.time");
            TestStages.reload(server);
            Inbox inbox = TestPlayers.run(player, "time query daytime");
            if (!inbox.toString().contains("§cNope") || inbox.mentions(LOCKED)) {
                helper.fail("the admin's own text must replace the default, got " + inbox);
                return;
            }
            helper.succeed();
        } finally {
            PermissionsSettings.MESSAGE_TEXT.write().accept("");
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
