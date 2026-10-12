package net.bananemdnsa.hstg_permissions.config;

import java.util.List;

import net.bananemdnsa.hstg_permissions.rules.LockedMessage;
import net.bananemdnsa.hstg_permissions.rules.ProtectionRules;

/**
 * The settings as plain values, for when no spec is loaded at all, so what was last written is still
 * read back. A remote client is not such a case: NeoForge syncs the server config to it. Kept apart
 * from {@link PermissionsConfig} because that class cannot load without NeoForge.
 */
final class SettingsMirror {

    private volatile List<String> protectedCommands = ProtectionRules.DEFAULT_PROTECTED;
    private volatile String messageMode = "LOCKED";
    private volatile String lockedMessageText = LockedMessage.DEFAULT;

    List<String> protectedCommands() {
        return protectedCommands;
    }

    void protectedCommands(List<String> value) {
        protectedCommands = List.copyOf(value);
    }

    String messageMode() {
        return messageMode;
    }

    void messageMode(String value) {
        messageMode = value;
    }

    String lockedMessageText() {
        return lockedMessageText;
    }

    void lockedMessageText(String value) {
        lockedMessageText = value;
    }
}
