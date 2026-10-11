package net.bananemdnsa.hstg_permissions.editor;

import org.jetbrains.annotations.Nullable;

import net.bananemdnsa.hstg_permissions.rules.EntryProblems;

/**
 * A list row's text, written the way the stage file writes it, with Minecraft's colour codes:
 * the mod part grey, a row with a problem in its colour and with its sign.
 */
public final class RowText {

    private static final String GREY = "§8";
    private static final String GOLD = "§6";
    private static final String YELLOW = "§e";
    private static final String RED = "§c";
    private static final String RESET = "§r";

    private RowText() {}

    public static String of(String command, @Nullable EntryProblems.Level level) {
        if (level == EntryProblems.Level.ERROR) return RED + "⛔ " + command;
        if (level == EntryProblems.Level.WARN) return YELLOW + "⚠ " + command;
        String trimmed = command.trim();
        if (trimmed.equals("*")) return GOLD + "★" + RESET + " *";
        int dot = trimmed.indexOf('.');
        if (dot <= 0) return trimmed;
        return GREY + trimmed.substring(0, dot + 1) + RESET + trimmed.substring(dot + 1);
    }
}
