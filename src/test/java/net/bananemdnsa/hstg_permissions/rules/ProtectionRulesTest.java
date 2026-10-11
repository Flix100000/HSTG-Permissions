package net.bananemdnsa.hstg_permissions.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ProtectionRulesTest {

    private static final Set<String> MODS = Set.of("minecraft");

    private static ProtectionRules rules(String... protectedCommands) {
        return ProtectionRules.of(List.of(protectedCommands), ProtectionRules.NEVER_LOCKABLE_MODS, MODS);
    }

    @Test
    void aLeadingModNameIsDropped() {
        assertTrue(rules("minecraft.op").isProtected(CommandTarget.of("minecraft", "op")));
    }

    @Test
    void aLeadingModNameIsDroppedFromADeeperPath() {
        ProtectionRules rules = rules("minecraft.whitelist.add");
        assertTrue(rules.isProtected(CommandTarget.of("minecraft", "whitelist", "add")));
        assertFalse(rules.isProtected(CommandTarget.of("minecraft", "whitelist", "list")));
    }

    @Test
    void aSingleWordIsACommandEvenWhenAModHasThatName() {
        ProtectionRules rules = ProtectionRules.of(List.of("neoforge"), ProtectionRules.NEVER_LOCKABLE_MODS,
                Set.of("minecraft", "neoforge"));
        assertTrue(rules.isProtected(CommandTarget.of("othermod", "neoforge", "tps")));
        assertEquals(List.of(), ProtectionRules.unusable(List.of("neoforge"), Set.of("minecraft", "neoforge")));
    }

    @Test
    void entriesThatProtectNothingAreListed() {
        List<String> written = List.of("*", "  ", "a..b", "op", "minecraft.op");
        assertEquals(List.of("*", "  ", "a..b"), ProtectionRules.unusable(written, MODS));
    }

    @Test
    void theDefaultsAreExactlyTheDangerousOnes() {
        assertEquals(Set.of("op", "deop", "stop", "ban", "ban-ip", "pardon", "pardon-ip", "whitelist",
                        "execute", "reload", "save-off", "save-all", "debug", "perf", "jfr", "publish", "transfer",
                        "history"),
                Set.copyOf(ProtectionRules.DEFAULT_PROTECTED));
        assertEquals(18, ProtectionRules.DEFAULT_PROTECTED.size());
        assertFalse(ProtectionRules.DEFAULT_PROTECTED.contains("kick"));
    }

    @Test
    void historyCanBeNeitherLockedNorGrantedByDefault() {
        ProtectionRules rules = ProtectionRules.of(ProtectionRules.DEFAULT_PROTECTED, ProtectionRules.NEVER_LOCKABLE_MODS);
        CommandTarget unlock = CommandTarget.of("historystages", "history", "global", "unlock");
        List<Candidate> candidates = List.of(
                new Candidate(EntryPattern.parse("*").orElseThrow(), Mode.GRANT, true, "admin"),
                new Candidate(EntryPattern.parse("historystages.history").orElseThrow(), Mode.GRANT, true, "mod"),
                new Candidate(EntryPattern.parse("historystages.history").orElseThrow(), Mode.LOCK, true, "guest"));
        assertEquals(Decision.Verdict.DEFAULT, Resolver.decide(unlock, candidates, rules).verdict());
    }
}
