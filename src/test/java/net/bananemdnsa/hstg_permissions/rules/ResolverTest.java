package net.bananemdnsa.hstg_permissions.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import net.bananemdnsa.hstg_permissions.rules.Decision.Verdict;
import org.junit.jupiter.api.Test;

class ResolverTest {

    private static final ProtectionRules RULES =
            ProtectionRules.of(List.of("op", "execute", "history"), ProtectionRules.NEVER_LOCKABLE_MODS);
    private static final CommandTarget CREATIVE = CommandTarget.of("minecraft", "gamemode", "creative");
    private static final CommandTarget SURVIVAL = CommandTarget.of("minecraft", "gamemode", "survival");

    private static Candidate held(String pattern) {
        return new Candidate(EntryPattern.parse(pattern).orElseThrow(), true, "has_" + pattern);
    }

    private static Candidate missing(String pattern) {
        return new Candidate(EntryPattern.parse(pattern).orElseThrow(), false, "lacks_" + pattern);
    }

    private static Verdict verdict(CommandTarget target, Candidate... candidates) {
        return Resolver.decide(target, List.of(candidates), RULES).verdict();
    }

    @Test
    void noEntryLeavesItToMinecraft() {
        assertEquals(Verdict.DEFAULT, verdict(CREATIVE));
        assertEquals(Verdict.DEFAULT, verdict(CREATIVE, missing("minecraft.time")));
    }

    @Test
    void aStageThePlayerLacksTakesTheCommand() {
        assertEquals(Verdict.LOCK, verdict(CREATIVE, missing("minecraft.gamemode")));
    }

    @Test
    void aStageThePlayerHoldsGivesTheCommand() {
        assertEquals(Verdict.GRANT, verdict(CREATIVE, held("minecraft.gamemode")));
    }

    @Test
    void oneHeldStageIsEnough() {
        assertEquals(Verdict.GRANT, verdict(CREATIVE, missing("minecraft.gamemode"), held("minecraft.gamemode")));
    }

    @Test
    void theMostSpecificEntriesDecide() {
        Candidate[] roles = { held("minecraft.gamemode"), missing("minecraft.gamemode.creative") };
        assertEquals(Verdict.LOCK, verdict(CREATIVE, roles));
        assertEquals(Verdict.GRANT, verdict(SURVIVAL, roles));
    }

    @Test
    void aWholeModIsWeakerThanAnyPath() {
        assertEquals(Verdict.GRANT, verdict(CommandTarget.of("minecraft", "time"), missing("minecraft.*"), held("minecraft.time")));
        assertEquals(Verdict.LOCK, verdict(CREATIVE, missing("minecraft.*"), held("minecraft.time")));
    }

    @Test
    void theEverythingStageGivesEverything() {
        assertEquals(Verdict.GRANT, verdict(CREATIVE, held("*")));
        assertEquals(Verdict.GRANT, verdict(CREATIVE, held("*"), missing("minecraft.gamemode.creative")));
    }

    @Test
    void theEverythingStageTakesNothing() {
        assertEquals(Verdict.DEFAULT, verdict(CREATIVE, missing("*")));
        assertEquals(Verdict.GRANT, verdict(CREATIVE, missing("*"), held("minecraft.gamemode")));
    }

    @Test
    void protectedCommandsCanBeTakenButNotGiven() {
        CommandTarget op = CommandTarget.of("minecraft", "op", "steve");
        assertEquals(Verdict.LOCK, verdict(op, missing("minecraft.op")));
        assertEquals(Verdict.DEFAULT, verdict(op, held("minecraft.op")));
        assertEquals(Verdict.DEFAULT, verdict(op, held("*")));
        assertEquals(Verdict.DEFAULT, verdict(CommandTarget.of("minecraft", "execute", "as"), held("minecraft.execute.as")));
    }

    @Test
    void historyStagesCommandsCanBeGivenButNeverTaken() {
        CommandTarget hstgp = CommandTarget.of("hstg_permissions", "anything");
        assertEquals(Verdict.DEFAULT, verdict(hstgp, missing("hstg_permissions.anything")));
        assertEquals(Verdict.GRANT, verdict(hstgp, held("hstg_permissions.anything")));
        // history is protected as well, so a stage can neither take nor give it.
        CommandTarget history = CommandTarget.of("unknown", "history", "list");
        assertEquals(Verdict.DEFAULT, verdict(history, missing("historystages.history")));
        assertEquals(Verdict.DEFAULT, verdict(history, held("historystages.history")));
    }

    @Test
    void onlyTheMissingStagesAtTheTopAreNamed() {
        Decision decision = Resolver.decide(CREATIVE, List.of(
                new Candidate(EntryPattern.parse("minecraft.*").orElseThrow(), false, "guest"),
                new Candidate(EntryPattern.parse("minecraft.gamemode").orElseThrow(), false, "builder"),
                new Candidate(EntryPattern.parse("minecraft.gamemode").orElseThrow(), false, "builder"),
                new Candidate(EntryPattern.parse("minecraft.gamemode").orElseThrow(), false, "admin")),
                RULES);
        assertEquals(Verdict.LOCK, decision.verdict());
        assertEquals(List.of("builder", "admin"), decision.lockingStages());
    }

    @Test
    void aliasSpellingsCountAsTheSamePath() {
        CommandTarget tp = new CommandTarget("minecraft", List.of(List.of("tp"), List.of("teleport")));
        assertEquals(Verdict.GRANT, verdict(tp, missing("minecraft.teleport"), held("minecraft.tp")));
        assertEquals(Verdict.LOCK, verdict(tp, missing("minecraft.teleport")));
    }

    @Test
    void aWrongModNameStillNamesTheCommand() {
        assertEquals(Verdict.LOCK, verdict(CREATIVE, missing("othermod.gamemode")));
    }

    @Test
    void unreadableEntriesNeverReachTheResolver() {
        List<Candidate> candidates = new ArrayList<>();
        for (PermissionEntry entry : List.of(new PermissionEntry("minecraft..gamemode", null),
                new PermissionEntry(null, null))) {
            entry.pattern().ifPresent(pattern -> candidates.add(new Candidate(pattern, false, "s")));
        }
        assertEquals(Verdict.DEFAULT, Resolver.decide(CREATIVE, candidates, RULES).verdict());
    }
}
