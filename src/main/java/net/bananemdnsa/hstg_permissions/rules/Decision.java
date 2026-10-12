package net.bananemdnsa.hstg_permissions.rules;

import java.util.List;
import java.util.Optional;

/**
 * @param lockingStages the missing stages at the most specific level; empty unless the verdict is LOCK
 * @param grantedBy     the entry that gave the command; empty unless the verdict is GRANT
 */
public record Decision(Verdict verdict, List<String> lockingStages, Optional<Candidate> grantedBy) {

    public enum Verdict { LOCK, GRANT, DEFAULT }

    public static final Decision DEFAULT = new Decision(Verdict.DEFAULT, List.of(), Optional.empty());

    public static Decision grant(Candidate by) {
        return new Decision(Verdict.GRANT, List.of(), Optional.of(by));
    }

    public static Decision lock(List<String> stages) {
        return new Decision(Verdict.LOCK, List.copyOf(stages), Optional.empty());
    }
}
