package net.bananemdnsa.hstg_permissions.rank;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class RankChoiceTest {

    private static RankSource source(String id, String prefix, String suffix, int priority) {
        return new RankSource(id, prefix, suffix, priority);
    }

    @Test
    void noSourceIsNoRank() {
        assertEquals(Rank.NONE, RankChoice.pick(List.of()));
    }

    @Test
    void theHighestPriorityWins() {
        Rank rank = RankChoice.pick(List.of(source("member", "[M]", "", 1), source("admin", "[A]", "", 10)));
        assertEquals("[A]", rank.prefix());
    }

    @Test
    void prefixAndSuffixAreChosenApart() {
        Rank rank = RankChoice.pick(List.of(source("admin", "[A]", "", 10), source("iron", "", "⚒", 2)));
        assertEquals(new Rank("[A]", "⚒"), rank);
    }

    @Test
    void aTieGoesToTheSmallerId() {
        Rank rank = RankChoice.pick(List.of(source("b", "[B]", "", 5), source("a", "[A]", "", 5)));
        assertEquals("[A]", rank.prefix());
    }

    @Test
    void blankFieldsDoNotCount() {
        Rank rank = RankChoice.pick(List.of(source("top", "  ", null, 99), source("low", "[L]", "", 0)));
        assertEquals("[L]", rank.prefix());
        assertEquals("", rank.suffix());
    }
}
