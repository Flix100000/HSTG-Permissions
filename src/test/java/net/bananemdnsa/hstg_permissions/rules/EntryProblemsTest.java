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
        return EntryProblems.check(new PermissionEntry(command), RULES, pattern -> true, null);
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
    void aProtectedCommandIsAWarning() {
        List<Problem> problems = check("minecraft.op");
        assertEquals(1, problems.size());
        assertEquals(Level.WARN, problems.get(0).level());
        assertEquals(EntryProblems.Kind.PROTECTED, problems.get(0).kind());
    }

    @Test
    void aHistoryStagesCommandIsAWarning() {
        assertEquals(Level.WARN, check("historystages.gamemode").get(0).level());
        assertEquals(EntryProblems.Kind.NEVER_LOCKABLE, check("historystages.gamemode").get(0).kind());
        assertEquals(Level.WARN, check("hstg_permissions.*").get(0).level());
        assertEquals(EntryProblems.Kind.NEVER_LOCKABLE, check("hstg_permissions.*").get(0).kind());
    }

    @Test
    void aPathTheServerDoesNotHaveIsAWarning() {
        List<Problem> problems = EntryProblems.check(new PermissionEntry("minecraft.gamemode.creatve"),
                RULES, pattern -> false, null);
        assertEquals(1, problems.size());
        assertEquals(Level.WARN, problems.get(0).level());
        assertEquals(EntryProblems.Kind.NOT_FOUND, problems.get(0).kind());
    }

    @Test
    void withoutATreeNothingIsCalledMissing() {
        assertTrue(EntryProblems.check(new PermissionEntry("minecraft.gamemode.creatve"), RULES, null, null).isEmpty());
    }

    @Test
    void theRealModDecidesWhetherItIsNeverLockable() {
        assertTrue(EntryProblems.check(new PermissionEntry("historystages.gamemode"), RULES,
                pattern -> true, pattern -> "minecraft").isEmpty());
        List<Problem> problems = EntryProblems.check(new PermissionEntry("minecraft.gamemode"), RULES,
                pattern -> true, pattern -> "historystages");
        assertEquals(1, problems.size());
        assertEquals(EntryProblems.Kind.NEVER_LOCKABLE, problems.get(0).kind());
        assertEquals(Level.WARN, problems.get(0).level());
    }

    @Test
    void historyIsAlwaysVanilla() {
        List<Problem> direct = check("historystages.history");
        assertEquals(1, direct.size());
        assertEquals(Level.WARN, direct.get(0).level());
        assertEquals(EntryProblems.Kind.ALWAYS_VANILLA, direct.get(0).kind());

        List<Problem> viaRealMod = EntryProblems.check(new PermissionEntry("minecraft.history"), RULES,
                pattern -> true, pattern -> "historystages");
        assertEquals(1, viaRealMod.size());
        assertEquals(EntryProblems.Kind.ALWAYS_VANILLA, viaRealMod.get(0).kind());
    }
}
