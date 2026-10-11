package net.bananemdnsa.hstg_permissions.rules;

import java.util.ArrayList;
import java.util.List;

/** Decides what the entries matching one command add up to for one player. */
public final class Resolver {

    private Resolver() {}

    public static Decision decide(CommandTarget target, List<Candidate> candidates, ProtectionRules rules) {
        boolean isProtected = rules.isProtected(target);
        boolean neverLockable = rules.isNeverLockable(target);

        List<Candidate> live = new ArrayList<>();
        for (Candidate candidate : candidates) {
            if (!candidate.applies() || !candidate.pattern().matches(target)) continue;
            if (candidate.mode() == Mode.GRANT && isProtected) continue;
            if (candidate.mode() == Mode.LOCK && neverLockable) continue;
            live.add(candidate);
        }
        if (live.isEmpty()) return Decision.DEFAULT;

        // An admin stage: granting * beats every lock, however specific.
        for (Candidate candidate : live) {
            if (candidate.mode() == Mode.GRANT && candidate.pattern().kind() == EntryPattern.Kind.ALL) {
                return Decision.GRANT;
            }
        }

        int best = 0;
        for (Candidate candidate : live) best = Math.max(best, candidate.pattern().specificity());
        List<String> locking = new ArrayList<>();
        for (Candidate candidate : live) {
            if (candidate.pattern().specificity() == best && candidate.mode() == Mode.LOCK
                    && !locking.contains(candidate.stageId())) {
                locking.add(candidate.stageId());
            }
        }
        return locking.isEmpty() ? Decision.GRANT : Decision.lock(locking);
    }
}
