package net.bananemdnsa.hstg_permissions.rules;

import java.util.ArrayList;
import java.util.List;

/**
 * Decides what the entries matching one command add up to for one player. A command that belongs
 * to stages may be used by whoever has one of them and by nobody else, operator or not.
 */
public final class Resolver {

    private Resolver() {}

    public static Decision decide(CommandTarget target, List<Candidate> candidates, ProtectionRules rules) {
        boolean isProtected = rules.isProtected(target);
        boolean neverLockable = rules.isNeverLockable(target);

        Candidate everything = null;
        List<Candidate> scoped = new ArrayList<>();
        for (Candidate candidate : candidates) {
            if (!candidate.pattern().matches(target)) continue;
            if (candidate.pattern().kind() == EntryPattern.Kind.ALL) {
                if (candidate.held() && everything == null) everything = candidate;
            } else {
                scoped.add(candidate);
            }
        }
        // A stage holding * may use everything and takes nothing from anyone without it.
        if (everything != null && !isProtected) return Decision.grant(everything);
        if (scoped.isEmpty()) return Decision.DEFAULT;

        int best = 0;
        for (Candidate candidate : scoped) best = Math.max(best, candidate.pattern().specificity());
        List<String> missing = new ArrayList<>();
        for (Candidate candidate : scoped) {
            if (candidate.pattern().specificity() != best) continue;
            // Protected commands can be taken but never handed out.
            if (candidate.held()) return isProtected ? Decision.DEFAULT : Decision.grant(candidate);
            if (!missing.contains(candidate.stageId())) missing.add(candidate.stageId());
        }
        // And History Stages' own commands the other way round.
        return neverLockable ? Decision.DEFAULT : Decision.lock(missing);
    }
}
