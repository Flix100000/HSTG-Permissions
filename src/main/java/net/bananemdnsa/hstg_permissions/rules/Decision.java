package net.bananemdnsa.hstg_permissions.rules;

import java.util.List;

/** @param lockingStages the missing stages at the most specific level; empty unless the verdict is LOCK */
public record Decision(Verdict verdict, List<String> lockingStages) {

    public enum Verdict { LOCK, GRANT, DEFAULT }

    public static final Decision DEFAULT = new Decision(Verdict.DEFAULT, List.of());
    public static final Decision GRANT = new Decision(Verdict.GRANT, List.of());

    public static Decision lock(List<String> stages) {
        return new Decision(Verdict.LOCK, List.copyOf(stages));
    }
}
