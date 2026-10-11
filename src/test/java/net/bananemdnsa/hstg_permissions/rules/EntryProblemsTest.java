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

    private static List<Problem> check(String command, String mode) {
        return EntryProblems.check(new PermissionEntry(command, mode), RULES, pattern -> true, null);
    }

    @Test
    void aGoodEntryHasNoProblems() {
        assertTrue(check("minecraft.gamemode.creative", "lock").isEmpty());
        assertTrue(check("*", "grant").isEmpty());
    }

    @Test
    void anUnreadablePathIsAnError() {
        List<Problem> problems = check("minecraft..op", "lock");
        assertEquals(1, problems.size());
        assertEquals(Level.ERROR, problems.get(0).level());
        assertTrue(problems.get(0).message().contains("minecraft..op"));
    }

    @Test
    void anUnknownModeIsAnError() {
        List<Problem> problems = check("minecraft.gamemode", "deny");
        assertEquals(Level.ERROR, problems.get(0).level());
        assertTrue(problems.get(0).message().contains("deny"));
        assertEquals(Level.ERROR, check("minecraft.gamemode", null).get(0).level());
    }

    @Test
    void aGrantOnTheProtectedListIsAnError() {
        List<Problem> problems = check("minecraft.op", "grant");
        assertEquals(1, problems.size());
        assertEquals(Level.ERROR, problems.get(0).level());
        assertTrue(check("minecraft.op", "lock").isEmpty());
    }

    @Test
    void aLockOnHistoryStagesIsAWarning() {
        assertEquals(Level.WARN, check("historystages.history", "lock").get(0).level());
        assertEquals(Level.WARN, check("hstg_permissions.*", "lock").get(0).level());
    }

    @Test
    void aPathTheServerDoesNotHaveIsAWarning() {
        List<Problem> problems = EntryProblems.check(new PermissionEntry("minecraft.gamemode.creatve", "lock"),
                RULES, pattern -> false, null);
        assertEquals(1, problems.size());
        assertEquals(Level.WARN, problems.get(0).level());
    }

    @Test
    void withoutATreeNothingIsCalledMissing() {
        assertTrue(EntryProblems.check(new PermissionEntry("minecraft.gamemode.creatve", "lock"), RULES, null, null).isEmpty());
    }

    @Test
    void theRealModDecidesWhetherALockIsDropped() {
        assertTrue(EntryProblems.check(new PermissionEntry("historystages.gamemode", "lock"), RULES,
                pattern -> true, pattern -> "minecraft").isEmpty());
        List<Problem> problems = EntryProblems.check(new PermissionEntry("minecraft.history", "lock"), RULES,
                pattern -> true, pattern -> "historystages");
        assertEquals(1, problems.size());
        assertEquals(Level.WARN, problems.get(0).level());
    }
}
