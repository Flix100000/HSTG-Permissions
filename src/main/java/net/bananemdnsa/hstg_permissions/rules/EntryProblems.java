package net.bananemdnsa.hstg_permissions.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

/** What is wrong with one stored entry, in the words the debug report uses. */
public final class EntryProblems {

    public enum Level { ERROR, WARN }

    /** What is wrong, for a caller that words it itself – the editor shows its own translated text. */
    public enum Kind { BAD_PATH, PROTECTED, NEVER_LOCKABLE, ALWAYS_VANILLA, NOT_FOUND }

    public record Problem(Level level, Kind kind, String message) {}

    private EntryProblems() {}

    /**
     * @param exists whether the server has the path; null while no command tree is known
     * @param realModOf the mod a path's command really belongs to; null while no tree is known
     */
    public static List<Problem> check(PermissionEntry entry, ProtectionRules rules,
                                      @Nullable Predicate<EntryPattern> exists,
                                      @Nullable Function<EntryPattern, String> realModOf) {
        List<Problem> problems = new ArrayList<>();
        String command = String.valueOf(entry.command());
        Optional<EntryPattern> pattern = entry.pattern();
        if (pattern.isEmpty()) {
            problems.add(new Problem(Level.ERROR, Kind.BAD_PATH, "'" + command
                    + "' is not a command path. Write it like minecraft.gamemode.creative, mod.* or *."));
        }
        if (!problems.isEmpty()) return problems;

        EntryPattern read = pattern.get();
        // At runtime a concrete path ignores its own mod prefix, so judge by the command's real mod.
        String lockMod = read.mod();
        if (read.kind() == EntryPattern.Kind.PATH && realModOf != null) {
            String real = realModOf.apply(read);
            if (real != null && !real.equals("unknown")) lockMod = real;
        }
        CommandTarget named = new CommandTarget(read.mod(),
                List.of(read.kind() == EntryPattern.Kind.PATH ? read.segments() : List.of("*")));
        boolean isProtected = read.kind() == EntryPattern.Kind.PATH && rules.isProtected(named);
        boolean neverLockable = read.kind() != EntryPattern.Kind.ALL
                && rules.isNeverLockable(new CommandTarget(lockMod, named.spellings()));
        if (isProtected && neverLockable) {
            // Either warning alone would be false: the resolver always answers DEFAULT here.
            problems.add(new Problem(Level.WARN, Kind.ALWAYS_VANILLA, "'" + command
                    + "' can neither be given nor taken by a stage. It always keeps Minecraft's own operator check."));
        } else if (isProtected) {
            problems.add(new Problem(Level.WARN, Kind.PROTECTED, "'" + command
                    + "' is on the protected list. A stage can take it away, but whoever has the stage still needs to be an operator."));
        } else if (neverLockable) {
            problems.add(new Problem(Level.WARN, Kind.NEVER_LOCKABLE, "'" + command
                    + "' belongs to History Stages or HSTG: Permissions. A stage can give it, but never take it away."));
        }
        if (exists != null && !exists.test(read)) {
            problems.add(new Problem(Level.WARN, Kind.NOT_FOUND, "No command '" + command
                    + "' on this server. A typo, or the mod is not installed. The entry stays and works once the command exists."));
        }
        return problems;
    }
}
