package net.bananemdnsa.hstg_permissions.config;

import net.bananemdnsa.historystages.api.config.AddonConfigField;
import net.bananemdnsa.historystages.api.config.AddonConfigSection;
import net.bananemdnsa.historystages.api.config.ConfigSide;
import net.bananemdnsa.historystages.api.config.RegisterConfigSectionsEvent;
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
            .defaultValue("")
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

    private PermissionsSettings() {}

    public static void register(RegisterConfigSectionsEvent event) {
        event.register(AddonConfigSection.builder(SECTION_ID)
                .titleLangKey("config.hstg_permissions.settings.title")
                .side(ConfigSide.COMMON)
                .field(MESSAGE_MODE)
                .field(MESSAGE_TEXT)
                .field(PROTECTED)
                .build());
    }
}
