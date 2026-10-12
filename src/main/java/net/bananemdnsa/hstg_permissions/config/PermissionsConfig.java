package net.bananemdnsa.hstg_permissions.config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.bananemdnsa.hstg_permissions.HSTGPermissions;
import net.bananemdnsa.hstg_permissions.gate.StageChangeListener;
import net.bananemdnsa.hstg_permissions.rules.LockedMessage;
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
    private static final ModConfigSpec.ConfigValue<String> LOCKED_MESSAGE_TEXT;
    private static final ModConfigSpec.BooleanValue RANK_IN_CHAT;
    private static final ModConfigSpec.BooleanValue RANK_IN_TAB_LIST;
    private static final ModConfigSpec.BooleanValue RANK_IN_NAME_TAG;

    // Fallback for when no spec is loaded at all (no world open, or a test before loading). A remote
    // client does get the server config: NeoForge syncs it as a copy without a file.
    private static final SettingsMirror MIRROR = new SettingsMirror();

    private static volatile ProtectionRules protection =
            ProtectionRules.of(ProtectionRules.DEFAULT_PROTECTED, ProtectionRules.NEVER_LOCKABLE_MODS);
    private static volatile List<String> ignoredProtected = List.of();

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        PROTECTED_COMMANDS = builder
                .comment("Commands a stage can take away but never hand out, not even through '*'.",
                        "Written without a mod name, e.g. 'op' or 'whitelist.add'.")
                .defineListAllowEmpty("protected_commands", ProtectionRules.DEFAULT_PROTECTED,
                        () -> "", value -> value instanceof String);
        LOCKED_MESSAGE = builder
                .comment("What a player sees when a command is locked.",
                        "LOCKED says it is locked (and which stage it needs, where the stage allows hints).",
                        "VANILLA answers like Minecraft does for a command that does not exist.")
                .defineEnum("locked_message", MessageMode.LOCKED);
        LOCKED_MESSAGE_TEXT = builder
                .comment("The message shown when a command is locked. {stage} is the missing stage, "
                        + "{command} what was typed, & starts a colour code. Empty uses the translated default "
                        + "(message.hstg_permissions.command_locked).")
                .define("locked_message_text", LockedMessage.DEFAULT);
        RANK_IN_CHAT = builder
                .comment("Show the prefix and suffix of a player's rank stage around their name in chat.",
                        "Turn it off when another mod already formats the chat.")
                .define("rank_in_chat", true);
        RANK_IN_TAB_LIST = builder
                .comment("Show the rank in the player list (Tab).")
                .define("rank_in_tab_list", true);
        RANK_IN_NAME_TAG = builder
                .comment("Show the rank on the name above a player's head, for clients that have this addon.")
                .define("rank_in_name_tag", true);
        SPEC = builder.build();
    }

    private PermissionsConfig() {}

    public static void onConfigChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;
        reapply();
    }

    // Also reached through save(): it fires Reloading even for a synced copy without a file.
    private static void reapply() {
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
        return SPEC.isLoaded() ? List.copyOf(PROTECTED_COMMANDS.get()) : MIRROR.protectedCommands();
    }

    public static MessageMode messageMode() {
        return SPEC.isLoaded() ? LOCKED_MESSAGE.get() : MessageMode.valueOf(MIRROR.messageMode());
    }

    public static String lockedMessageText() {
        return SPEC.isLoaded() ? LOCKED_MESSAGE_TEXT.get() : MIRROR.lockedMessageText();
    }

    public static boolean rankInChat() {
        return SPEC.isLoaded() ? RANK_IN_CHAT.get() : MIRROR.rankInChat();
    }

    public static boolean rankInTabList() {
        return SPEC.isLoaded() ? RANK_IN_TAB_LIST.get() : MIRROR.rankInTabList();
    }

    public static boolean rankInNameTag() {
        return SPEC.isLoaded() ? RANK_IN_NAME_TAG.get() : MIRROR.rankInNameTag();
    }

    public static void setProtectedCommands(List<String> commands) {
        MIRROR.protectedCommands(commands);
        if (!SPEC.isLoaded()) {
            reapply();
            return;
        }
        PROTECTED_COMMANDS.set(MIRROR.protectedCommands());
        PROTECTED_COMMANDS.save();
    }

    public static void setMessageMode(MessageMode mode) {
        MIRROR.messageMode(mode.name());
        if (!SPEC.isLoaded()) {
            reapply();
            return;
        }
        LOCKED_MESSAGE.set(mode);
        LOCKED_MESSAGE.save();
    }

    public static void setLockedMessageText(String text) {
        MIRROR.lockedMessageText(text);
        if (!SPEC.isLoaded()) {
            reapply();
            return;
        }
        LOCKED_MESSAGE_TEXT.set(text);
        LOCKED_MESSAGE_TEXT.save();
    }

    public static void setRankInChat(boolean shown) {
        MIRROR.rankInChat(shown);
        set(RANK_IN_CHAT, shown);
    }

    public static void setRankInTabList(boolean shown) {
        MIRROR.rankInTabList(shown);
        set(RANK_IN_TAB_LIST, shown);
    }

    public static void setRankInNameTag(boolean shown) {
        MIRROR.rankInNameTag(shown);
        set(RANK_IN_NAME_TAG, shown);
    }

    private static void set(ModConfigSpec.BooleanValue value, boolean shown) {
        if (!SPEC.isLoaded()) {
            reapply();
            return;
        }
        value.set(shown);
        value.save();
    }
}
