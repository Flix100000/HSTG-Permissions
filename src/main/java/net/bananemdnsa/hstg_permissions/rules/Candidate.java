package net.bananemdnsa.hstg_permissions.rules;

/**
 * One entry that matched, from one stage.
 *
 * @param held whether the player has the stage – or, for a global one, whether the world has it
 */
public record Candidate(EntryPattern pattern, boolean held, String stageId) {}
