package net.bananemdnsa.hstg_permissions.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.bananemdnsa.hstg_permissions.rules.EntryProblems.Level;
import net.bananemdnsa.hstg_permissions.rules.EntryProblems.Problem;
import org.junit.jupiter.api.Test;

class EntryProblemsTest {

    private static final ProtectionRules RULES =
            ProtectionRules.of(ProtectionRules.DEFAULT_PROTECTED, ProtectionRules.NEVER_LOCKABLE_MODS);

    private static List<Problem> check(String command) {
        return EntryProblems.check(new PermissionEntry(command, null), RULES, pattern -> true, null);
    }

    @Test
    void aGoodEntryHasNoProblems() {
        assertTrue(check("minecraft.gamemode.creative").isEmpty());
        assertTrue(check("*").isEmpty());
    }

    @Test
    void anUnreadablePathIsAnError() {
        List<Problem> problems = check("minecraft..op");
        assertEquals(1, problems.size());
        assertEquals(Level.ERROR, problems.get(0).level());
        assertEquals(EntryProblems.Kind.BAD_PATH, problems.get(0).kind());
        assertTrue(problems.get(0).message().contains("minecraft..op"));
    }

    @Test
    void anOldModeIsIgnored() {
        assertTrue(EntryProblems.check(new PermissionEntry("minecraft.gamemode", "deny"), RULES,
                pattern -> true, null).isEmpty());
    }

    @Test
    void aProtectedCommandIsAWarning() {
        List<Problem> problems = check("minecraft.op");
        assertEquals(1, problems.size());
        assertEquals(Level.WARN, problems.get(0).level());
        assertEquals(EntryProblems.Kind.PROTECTED, problems.get(0).kind());
    }

    @Test
    void aHistoryStagesCommandIsAWarning() {
        assertEquals(Level.WARN, check("historystages.history").get(0).level());
        assertTrue(check("historystages.history").stream()
                .anyMatch(problem -> problem.kind() == EntryProblems.Kind.NEVER_LOCKABLE));
        assertEquals(Level.WARN, check("hstg_permissions.*").get(0).level());
        assertEquals(EntryProblems.Kind.NEVER_LOCKABLE, check("hstg_permissions.*").get(0).kind());
    }

    @Test
    void aPathTheServerDoesNotHaveIsAWarning() {
        List<Problem> problems = EntryProblems.check(new PermissionEntry("minecraft.gamemode.creatve", null),
                RULES, pattern -> false, null);
        assertEquals(1, problems.size());
        assertEquals(Level.WARN, problems.get(0).level());
        assertEquals(EntryProblems.Kind.NOT_FOUND, problems.get(0).kind());
    }

    @Test
    void withoutATreeNothingIsCalledMissing() {
        assertTrue(EntryProblems.check(new PermissionEntry("minecraft.gamemode.creatve", null), RULES, null, null).isEmpty());
    }

    @Test
    void theRealModDecidesWhetherItIsNeverLockable() {
        assertTrue(EntryProblems.check(new PermissionEntry("historystages.gamemode", null), RULES,
                pattern -> true, pattern -> "minecraft").isEmpty());
        List<Problem> problems = EntryProblems.check(new PermissionEntry("minecraft.history", null), RULES,
                pattern -> true, pattern -> "historystages");
        // history is protected as well, so it is flagged twice.
        assertTrue(problems.stream().anyMatch(problem -> problem.kind() == EntryProblems.Kind.NEVER_LOCKABLE));
        assertTrue(problems.stream().allMatch(problem -> problem.level() == Level.WARN));
    }
}
