package net.bananemdnsa.hstg_permissions.rank;

/** One stage the player has, with the rank it would give. */
public record RankSource(String stageId, String prefix, String suffix, int priority) {}
