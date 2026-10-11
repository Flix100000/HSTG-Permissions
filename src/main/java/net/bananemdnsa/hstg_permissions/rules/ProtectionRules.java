package net.bananemdnsa.hstg_permissions.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Commands that may only ever be locked, and the mods whose commands may never be locked.
 */
public record ProtectionRules(List<EntryPattern> protectedPaths, Set<String> neverLockableMods) {

    // execute is here because it runs anything as anyone. kick is not, on purpose: handing it to a
    // moderator stage is the point of the addon. history is here because a player who may run it can
    // unlock their own stages; being never lockable too, it always keeps its vanilla OP check.
    public static final List<String> DEFAULT_PROTECTED = List.of("op", "deop", "stop", "ban", "ban-ip",
            "pardon", "pardon-ip", "whitelist", "execute", "reload", "save-off", "save-all", "debug",
            "perf", "jfr", "publish", "transfer", "history");

    // Single player has no console. A locked /history would leave nothing but editing JSON by hand.
    public static final Set<String> NEVER_LOCKABLE_MODS = Set.of("historystages", "hstg_permissions");

    // Checked by name as well, so /history stays open even if its mod is ever guessed wrong.
    public static final Set<String> NEVER_LOCKABLE_ROOTS = Set.of("history");

    /** @param protectedCommands written without a mod name, e.g. {@code op} or {@code whitelist.add} */
    public static ProtectionRules of(Collection<String> protectedCommands, Set<String> neverLockableMods) {
        return of(protectedCommands, neverLockableMods, Set.of("minecraft"));
    }

    /**
     * @param knownModIds a leading word that names one of these is taken as a mod name and dropped,
     *                    since admins write {@code minecraft.op} out of habit from stage entries
     */
    public static ProtectionRules of(Collection<String> protectedCommands, Set<String> neverLockableMods,
                                     Set<String> knownModIds) {
        List<EntryPattern> paths = new ArrayList<>();
        for (String command : protectedCommands) {
            parse(command, knownModIds).ifPresent(paths::add);
        }
        return new ProtectionRules(List.copyOf(paths), Set.copyOf(neverLockableMods));
    }

    /**
     * The entries that protect nothing: blank, unreadable or {@code *}. A single word is always a
     * command name, since many mods name their command after themselves.
     */
    public static List<String> unusable(Collection<String> protectedCommands, Set<String> knownModIds) {
        List<String> unusable = new ArrayList<>();
        for (String command : protectedCommands) {
            if (parse(command, knownModIds).isEmpty()) unusable.add(command);
        }
        return unusable;
    }

    private static Optional<EntryPattern> parse(String command, Set<String> knownModIds) {
        if (command == null || command.isBlank()) return Optional.empty();
        String text = command.trim().toLowerCase(Locale.ROOT);
        int dot = text.indexOf('.');
        if (dot > 0 && knownModIds.contains(text.substring(0, dot))) text = text.substring(dot + 1);
        // The mod part is ignored when a path matches, so any placeholder will do.
        return EntryPattern.parse("any." + text).filter(pattern -> pattern.kind() == EntryPattern.Kind.PATH);
    }

    public boolean isProtected(CommandTarget target) {
        for (EntryPattern path : protectedPaths) {
            if (path.matches(target)) return true;
        }
        return false;
    }

    public boolean isNeverLockable(CommandTarget target) {
        if (neverLockableMods.contains(target.mod())) return true;
        for (List<String> spelling : target.spellings()) {
            if (NEVER_LOCKABLE_ROOTS.contains(spelling.get(0))) return true;
        }
        return false;
    }
}
