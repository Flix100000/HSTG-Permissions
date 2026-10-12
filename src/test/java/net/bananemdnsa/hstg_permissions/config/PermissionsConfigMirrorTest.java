package net.bananemdnsa.hstg_permissions.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.bananemdnsa.hstg_permissions.rules.ProtectionRules;

// PermissionsConfig needs NeoForge's ModConfigSpec, which the unit tests do not have; its
// reads and writes go through this mirror when no spec is loaded.
class PermissionsConfigMirrorTest {

    @Test
    void startsOnTheDefaults() {
        SettingsMirror mirror = new SettingsMirror();
        assertEquals(ProtectionRules.DEFAULT_PROTECTED, mirror.protectedCommands());
        assertEquals("LOCKED", mirror.messageMode());
        assertEquals("", mirror.lockedMessageText());
    }

    @Test
    void aClientWriteOnlyMirrors() {
        SettingsMirror mirror = new SettingsMirror();
        mirror.lockedMessageText("&cNo");
        assertEquals("&cNo", mirror.lockedMessageText());
        mirror.protectedCommands(List.of("op"));
        assertEquals(List.of("op"), mirror.protectedCommands());
        mirror.messageMode("VANILLA");
        assertEquals("VANILLA", mirror.messageMode());
    }
}
