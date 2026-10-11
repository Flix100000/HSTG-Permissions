package net.bananemdnsa.hstg_permissions.rules;

/**
 * One entry that matched, from one stage.
 *
 * @param applies for a lock: the stage still keeps the command locked ({@code LockHit.locks()});
 *                for a grant: the player has the stage ({@code LockHit.unlocked()})
 */
public record Candidate(EntryPattern pattern, Mode mode, boolean applies, String stageId) {}
