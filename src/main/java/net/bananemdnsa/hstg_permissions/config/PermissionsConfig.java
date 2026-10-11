package net.bananemdnsa.hstg_permissions.config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.bananemdnsa.hstg_permissions.HSTGPermissions;
import net.bananemdnsa.hstg_permissions.gate.StageChangeListener;
import net.bananemdnsa.hstg_permissions.rules.ProtectionRules;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforgespi.language.IModInfo;

public final class PermissionsConfig {

    public enum MessageMode { LOCKED, VANILLA }

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> PROTECTED_COMMANDS;
    private static final ModConfigSpec.EnumValue<MessageMode> LOCKED_MESSAGE;

    private static volatile ProtectionRules protection =
            ProtectionRules.of(ProtectionRules.DEFAULT_PROTECTED, ProtectionRules.NEVER_LOCKABLE_MODS);
    private static volatile List<String> ignoredProtected = List.of();

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        PROTECTED_COMMANDS = builder
                .comment("Commands that can only be locked, never granted, not even through '*'.",
                        "Written without a mod name, e.g. 'op' or 'whitelist.add'.")
                .defineListAllowEmpty("protected_commands", ProtectionRules.DEFAULT_PROTECTED,
                        () -> "", value -> value instanceof String);
        LOCKED_MESSAGE = builder
                .comment("What a player sees when a command is locked.",
                        "LOCKED says it is locked (and which stage it needs, where the stage allows hints).",
                        "VANILLA answers like Minecraft does for a command that does not exist.")
                .defineEnum("locked_message", MessageMode.LOCKED);
        SPEC = builder.build();
    }

    private PermissionsConfig() {}

    public static void onConfigChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;
        Set<String> modIds = knownModIds();
        List<String> commands = protectedCommands();
        protection = ProtectionRules.of(commands, ProtectionRules.NEVER_LOCKABLE_MODS, modIds);
        ignoredProtected = ProtectionRules.unusable(commands, modIds);
        for (String ignored : ignoredProtected) {
            HSTGPermissions.LOGGER.warn("Protected command '{}' protects nothing and is ignored.", ignored);
        }
        StageChangeListener.refreshAll();
    }

    private static Set<String> knownModIds() {
        Set<String> ids = new HashSet<>();
        ids.add("minecraft");
        for (IModInfo mod : ModList.get().getMods()) ids.add(mod.getModId());
        return ids;
    }

    public static ProtectionRules protection() {
        return protection;
    }

    /** Protected entries from the config that protect nothing, for the report. */
    public static List<String> ignoredProtectedCommands() {
        return ignoredProtected;
    }

    public static List<String> protectedCommands() {
        return SPEC.isLoaded() ? List.copyOf(PROTECTED_COMMANDS.get()) : ProtectionRules.DEFAULT_PROTECTED;
    }

    public static MessageMode messageMode() {
        return SPEC.isLoaded() ? LOCKED_MESSAGE.get() : MessageMode.LOCKED;
    }

    public static void setMessageMode(MessageMode mode) {
        LOCKED_MESSAGE.set(mode);
    }
}
