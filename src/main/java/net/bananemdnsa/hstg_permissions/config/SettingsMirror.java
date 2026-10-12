package net.bananemdnsa.hstg_permissions.config;

import java.util.List;

import net.bananemdnsa.hstg_permissions.rules.ProtectionRules;

/**
 * The settings as plain values, so a client (which has no server config) and a test without a
 * loaded spec still read what was last written. Kept apart from {@link PermissionsConfig} because
 * that class cannot load without NeoForge.
 */
final class SettingsMirror {

    private volatile List<String> protectedCommands = ProtectionRules.DEFAULT_PROTECTED;
    private volatile String messageMode = "LOCKED";
    private volatile String lockedMessageText = "";

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
