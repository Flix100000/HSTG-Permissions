package net.bananemdnsa.hstg_permissions.rank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RankTextTest {

    @Test
    void ampersandBecomesAColourCode() {
        assertEquals("§c[Admin]", RankText.colour("&c[Admin]"));
    }

    @Test
    void aRankTrimsItsEnds() {
        assertEquals(new Rank("[A]", "x"), new Rank(" [A] ", "x "));
        assertTrue(new Rank(null, " ").isEmpty());
    }
}
