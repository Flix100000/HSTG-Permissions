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

    public record Problem(Level level, String message) {}

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
        Optional<Mode> mode = entry.parsedMode();
        if (pattern.isEmpty()) {
            problems.add(new Problem(Level.ERROR, "'" + command
                    + "' is not a command path. Write it like minecraft.gamemode.creative, mod.* or *."));
        }
        if (mode.isEmpty()) {
            problems.add(new Problem(Level.ERROR, "'" + command + "' has mode '" + entry.mode()
                    + "'. Use lock or grant."));
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
        if (mode.get() == Mode.GRANT && read.kind() == EntryPattern.Kind.PATH && rules.isProtected(named)) {
            problems.add(new Problem(Level.ERROR, "'" + command
                    + "' is on the protected list and can only be locked. The grant is ignored."));
        }
        if (mode.get() == Mode.LOCK && read.kind() != EntryPattern.Kind.ALL && rules.isNeverLockable(new CommandTarget(lockMod, named.spellings()))) {
            problems.add(new Problem(Level.WARN, "'" + command
                    + "' belongs to History Stages or HSTG: Permissions, which can never be locked. The lock is ignored."));
        }
        if (exists != null && !exists.test(read)) {
            problems.add(new Problem(Level.WARN, "No command '" + command
                    + "' on this server. A typo, or the mod is not installed. The entry stays and works once the command exists."));
        }
        return problems;
    }
}
