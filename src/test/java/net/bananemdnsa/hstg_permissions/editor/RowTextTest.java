package net.bananemdnsa.hstg_permissions.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.bananemdnsa.hstg_permissions.rules.EntryProblems;
import org.junit.jupiter.api.Test;

class RowTextTest {

    @Test
    void theModPartIsGrey() {
        assertEquals("§8minecraft.§rgamemode.creative", RowText.of("minecraft.gamemode.creative", null));
        assertEquals("§8ftbquests.§r*", RowText.of("ftbquests.*", null));
    }

    @Test
    void everythingWearsAGoldStar() {
        assertEquals("§6★§r *", RowText.of("*", null));
    }

    @Test
    void problemsColourTheWholeRow() {
        assertEquals("§e⚠ waystones.tp", RowText.of("waystones.tp", EntryProblems.Level.WARN));
        assertEquals("§c⛔ minecraft.op", RowText.of("minecraft.op", EntryProblems.Level.ERROR));
    }
}
