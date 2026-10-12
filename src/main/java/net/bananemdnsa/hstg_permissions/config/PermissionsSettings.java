package net.bananemdnsa.hstg_permissions.config;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import net.bananemdnsa.historystages.api.config.AddonConfigField;
import net.bananemdnsa.historystages.api.config.AddonConfigSection;
import net.bananemdnsa.historystages.api.config.ConfigSide;
import net.bananemdnsa.historystages.api.config.RegisterConfigSectionsEvent;
import net.bananemdnsa.hstg_permissions.rules.LockedMessage;
import net.bananemdnsa.hstg_permissions.rules.ProtectedListValue;
import net.bananemdnsa.hstg_permissions.rules.ProtectionRules;

/** This addon's section in History Stages' config editor. Common: the values live in the server config. */
public final class PermissionsSettings {

    public static final String SECTION_ID = "hstg_permissions:settings";

    public static final AddonConfigField MESSAGE_MODE = AddonConfigField.choice("lockedMessage")
            .labelLangKey("config.hstg_permissions.settings.lockedMessage")
            .descLangKey("config.hstg_permissions.settings.lockedMessage.desc")
            .option("LOCKED", "config.hstg_permissions.settings.lockedMessage.option.locked")
            .option("VANILLA", "config.hstg_permissions.settings.lockedMessage.option.vanilla")
            .defaultValue("LOCKED")
            .read(() -> PermissionsConfig.messageMode().name())
            .write(value -> {
                try {
                    PermissionsConfig.setMessageMode(PermissionsConfig.MessageMode.valueOf(value));
                } catch (IllegalArgumentException ignored) {
                    // A broken synced value must not take the screen down.
                }
            })
            .build();

    public static final AddonConfigField MESSAGE_TEXT = AddonConfigField.richText("lockedMessageText")
            .labelLangKey("config.hstg_permissions.settings.lockedMessageText")
            .descLangKey("config.hstg_permissions.settings.lockedMessageText.desc")
            .defaultValue(LockedMessage.DEFAULT)
            .placeholder("{stage}")
            .placeholder("{command}")
            .read(PermissionsConfig::lockedMessageText)
            .write(PermissionsConfig::setLockedMessageText)
            .build();

    public static final AddonConfigField PROTECTED = AddonConfigField.customScreen("protectedCommands")
            .labelLangKey("config.hstg_permissions.settings.protectedCommands")
            .descLangKey("config.hstg_permissions.settings.protectedCommands.desc")
            .defaultValue(ProtectedListValue.join(ProtectionRules.DEFAULT_PROTECTED))
            .read(() -> ProtectedListValue.join(PermissionsConfig.protectedCommands()))
            .write(value -> PermissionsConfig.setProtectedCommands(ProtectedListValue.split(value)))
            .build();

    public static final AddonConfigField RANK_IN_CHAT = rankSwitch("rankInChat",
            PermissionsConfig::rankInChat, PermissionsConfig::setRankInChat);
    public static final AddonConfigField RANK_IN_TAB_LIST = rankSwitch("rankInTabList",
            PermissionsConfig::rankInTabList, PermissionsConfig::setRankInTabList);
    public static final AddonConfigField RANK_IN_NAME_TAG = rankSwitch("rankInNameTag",
            PermissionsConfig::rankInNameTag, PermissionsConfig::setRankInNameTag);

    private PermissionsSettings() {}

    private static AddonConfigField rankSwitch(String key, BooleanSupplier read, Consumer<Boolean> write) {
        return AddonConfigField.bool(key)
                .labelLangKey("config.hstg_permissions.settings." + key)
                .descLangKey("config.hstg_permissions.settings." + key + ".desc")
                .defaultValue("true")
                .read(() -> String.valueOf(read.getAsBoolean()))
                .write(value -> write.accept(Boolean.parseBoolean(value)))
                .build();
    }

    public static void register(RegisterConfigSectionsEvent event) {
        event.register(AddonConfigSection.builder(SECTION_ID)
                .titleLangKey("config.hstg_permissions.settings.title")
                .side(ConfigSide.COMMON)
                .field(MESSAGE_MODE)
                .field(MESSAGE_TEXT)
                .field(PROTECTED)
                .field(RANK_IN_CHAT)
                .field(RANK_IN_TAB_LIST)
                .field(RANK_IN_NAME_TAG)
                .build());
    }
}
