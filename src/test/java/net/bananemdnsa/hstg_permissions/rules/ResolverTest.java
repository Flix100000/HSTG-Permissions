package net.bananemdnsa.hstg_permissions.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import net.bananemdnsa.hstg_permissions.rules.Decision.Verdict;
import org.junit.jupiter.api.Test;

class ResolverTest {

    private static final ProtectionRules RULES =
            ProtectionRules.of(List.of("op", "execute"), ProtectionRules.NEVER_LOCKABLE_MODS);
    private static final CommandTarget CREATIVE = CommandTarget.of("minecraft", "gamemode", "creative");

    private static Candidate lock(String pattern) {
        return candidate(pattern, Mode.LOCK, true);
    }

    private static Candidate grant(String pattern) {
        return candidate(pattern, Mode.GRANT, true);
    }

    private static Candidate candidate(String pattern, Mode mode, boolean applies) {
        return new Candidate(EntryPattern.parse(pattern).orElseThrow(), mode, applies, "stage_" + pattern);
    }

    private static Verdict verdict(CommandTarget target, Candidate... candidates) {
        return Resolver.decide(target, List.of(candidates), RULES).verdict();
    }

    @Test
    void noEntryLeavesTheVanillaCheck() {
        assertEquals(Verdict.DEFAULT, verdict(CREATIVE));
    }

    @Test
    void aLockWhoseStageIsMissingLocks() {
        assertEquals(Verdict.LOCK, verdict(CREATIVE, lock("minecraft.gamemode")));
    }

    @Test
    void entriesThatDoNotApplyAreIgnored() {
        assertEquals(Verdict.DEFAULT, verdict(CREATIVE, candidate("minecraft.gamemode", Mode.LOCK, false)));
        assertEquals(Verdict.DEFAULT, verdict(CREATIVE, candidate("minecraft.gamemode", Mode.GRANT, false)));
    }

    @Test
    void entriesForOtherCommandsAreIgnored() {
        assertEquals(Verdict.DEFAULT, verdict(CREATIVE, lock("minecraft.time")));
    }

    @Test
    void aGrantWhoseStageIsUnlockedGrants() {
        assertEquals(Verdict.GRANT, verdict(CREATIVE, grant("minecraft.gamemode")));
    }

    @Test
    void theMostSpecificEntryWins() {
        assertEquals(Verdict.GRANT, verdict(CREATIVE, lock("minecraft.gamemode"), grant("minecraft.gamemode.creative")));
        assertEquals(Verdict.LOCK, verdict(CREATIVE, grant("minecraft.gamemode"), lock("minecraft.gamemode.creative")));
    }

    @Test
    void onEqualFootingLockWins() {
        assertEquals(Verdict.LOCK, verdict(CREATIVE, grant("minecraft.gamemode"), lock("minecraft.gamemode")));
    }

    @Test
    void aWholeModIsWeakerThanAnyPath() {
        assertEquals(Verdict.GRANT, verdict(CREATIVE, lock("minecraft.*"), grant("minecraft.gamemode")));
    }

    @Test
    void starIsTheWeakestLock() {
        CommandTarget msg = CommandTarget.of("minecraft", "msg");
        assertEquals(Verdict.GRANT, verdict(msg, lock("*"), grant("minecraft.msg")));
        assertEquals(Verdict.LOCK, verdict(CREATIVE, lock("*"), grant("minecraft.msg")));
    }

    @Test
    void aStarGrantBeatsEvenAMoreSpecificLock() {
        assertEquals(Verdict.GRANT, verdict(CREATIVE, grant("*"), lock("minecraft.gamemode.creative")));
    }

    @Test
    void protectedCommandsCanOnlyBeLocked() {
        CommandTarget op = CommandTarget.of("minecraft", "op", "steve");
        assertEquals(Verdict.DEFAULT, verdict(op, grant("minecraft.op")));
        assertEquals(Verdict.DEFAULT, verdict(op, grant("*")));
        assertEquals(Verdict.LOCK, verdict(op, lock("minecraft.op")));
    }

    @Test
    void aSubcommandOfAProtectedCommandIsProtectedToo() {
        CommandTarget executeAs = CommandTarget.of("minecraft", "execute", "as");
        assertEquals(Verdict.DEFAULT, verdict(executeAs, grant("minecraft.execute.as")));
    }

    @Test
    void historyStagesCommandsCanNeverBeLockedButCanBeGranted() {
        CommandTarget history = CommandTarget.of("historystages", "history", "reload");
        assertEquals(Verdict.DEFAULT, verdict(history, lock("*")));
        assertEquals(Verdict.DEFAULT, verdict(history, lock("historystages.history")));
        assertEquals(Verdict.GRANT, verdict(history, grant("historystages.history")));
        assertEquals(Verdict.DEFAULT, verdict(CommandTarget.of("hstg_permissions", "anything"), lock("*")));
    }

    @Test
    void historyStaysOpenEvenWhenItsModIsGuessedWrong() {
        assertEquals(Verdict.DEFAULT, verdict(CommandTarget.of("unknown", "history", "list"), lock("*")));
    }

    @Test
    void onlyTheWinningLocksAreNamed() {
        Decision decision = Resolver.decide(CREATIVE, List.of(
                new Candidate(EntryPattern.parse("*").orElseThrow(), Mode.LOCK, true, "guest"),
                new Candidate(EntryPattern.parse("minecraft.gamemode").orElseThrow(), Mode.LOCK, true, "builder"),
                new Candidate(EntryPattern.parse("minecraft.gamemode").orElseThrow(), Mode.LOCK, true, "builder")),
                RULES);
        assertEquals(Verdict.LOCK, decision.verdict());
        assertEquals(List.of("builder"), decision.lockingStages());
    }

    @Test
    void aliasSpellingsCountAsTheSamePath() {
        CommandTarget tp = new CommandTarget("minecraft", List.of(List.of("tp"), List.of("teleport")));
        assertEquals(Verdict.LOCK, verdict(tp, grant("minecraft.tp"), lock("minecraft.teleport")));
    }

    @Test
    void aWrongModNameDoesNotSwitchALockOff() {
        assertEquals(Verdict.LOCK, verdict(CREATIVE, lock("othermod.gamemode")));
    }

    @Test
    void unreadableEntriesNeverReachTheResolver() {
        // The service drops what does not parse; this is the shape it builds the candidates from.
        List<Candidate> candidates = new ArrayList<>();
        for (PermissionEntry entry : List.of(new PermissionEntry("minecraft..gamemode", "lock"),
                new PermissionEntry("minecraft.gamemode", "deny"), new PermissionEntry(null, "lock"))) {
            if (entry.pattern().isEmpty() || entry.parsedMode().isEmpty()) continue;
            candidates.add(new Candidate(entry.pattern().get(), entry.parsedMode().get(), true, "s"));
        }
        assertEquals(Verdict.DEFAULT, Resolver.decide(CREATIVE, candidates, RULES).verdict());
    }
}
