package net.bananemdnsa.hstg_permissions.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.bananemdnsa.hstg_permissions.rules.EntryPattern.Kind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EntryPatternTest {

    private static EntryPattern parse(String raw) {
        return EntryPattern.parse(raw).orElseThrow(() -> new AssertionError("should parse: " + raw));
    }

    @Test
    void starIsEverything() {
        EntryPattern all = parse("*");
        assertEquals(Kind.ALL, all.kind());
        assertTrue(all.matches(CommandTarget.of("ftbquests", "quests")));
    }

    @Test
    void aBareModNameMeansTheWholeMod() {
        assertEquals(new EntryPattern(Kind.MOD, "minecraft", List.of()), parse("minecraft.*"));
        assertEquals(parse("minecraft.*"), parse("minecraft"));
    }

    @Test
    void aTrailingStarChangesNothing() {
        assertEquals(parse("minecraft.gamemode"), parse("minecraft.gamemode.*"));
    }

    @Test
    void handEditedCaseAndSpacesAreForgiven() {
        assertEquals(parse("minecraft.gamemode.creative"), parse("  Minecraft.GameMode.Creative "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", ".", "**", ".op", "minecraft.op.", "minecraft..op",
            "mine*craft.op", "minecraft.*.op", "minecraft.game mode"})
    void malformedEntriesAreRejected(String raw) {
        assertTrue(EntryPattern.parse(raw).isEmpty(), raw);
    }

    @Test
    void nullIsRejected() {
        assertTrue(EntryPattern.parse(null).isEmpty());
    }

    @Test
    void aPathCoversItselfAndEverythingBelow() {
        EntryPattern gamemode = parse("minecraft.gamemode");
        assertTrue(gamemode.matches(CommandTarget.of("minecraft", "gamemode")));
        assertTrue(gamemode.matches(CommandTarget.of("minecraft", "gamemode", "creative", "steve")));
        assertFalse(gamemode.matches(CommandTarget.of("minecraft", "gamemodes")));
        assertFalse(parse("minecraft.gamemode.creative").matches(CommandTarget.of("minecraft", "gamemode")));
    }

    @Test
    void theModNameIsIgnoredForAConcretePath() {
        assertTrue(parse("somemod.gamemode").matches(CommandTarget.of("minecraft", "gamemode")));
    }

    @Test
    void aModEntryOnlyCoversItsOwnMod() {
        assertTrue(parse("ftbquests.*").matches(CommandTarget.of("ftbquests", "quests")));
        assertFalse(parse("ftbquests.*").matches(CommandTarget.of("minecraft", "gamemode")));
    }

    @Test
    void anySpellingOfTheTargetCounts() {
        CommandTarget tp = new CommandTarget("minecraft",
                List.of(List.of("tp", "steve"), List.of("teleport", "steve")));
        assertTrue(parse("minecraft.teleport").matches(tp));
        assertTrue(parse("minecraft.tp").matches(tp));
    }

    @Test
    void specificityGrowsWithEveryWord() {
        assertTrue(parse("*").specificity() < parse("minecraft.*").specificity());
        assertTrue(parse("minecraft.*").specificity() < parse("minecraft.gamemode").specificity());
        assertTrue(parse("minecraft.gamemode").specificity() < parse("minecraft.gamemode.creative").specificity());
    }

    @Test
    void onlyLongerPathsLieBelow() {
        CommandTarget gamemode = CommandTarget.of("minecraft", "gamemode");
        assertTrue(parse("minecraft.gamemode.creative").isStrictlyBelow(gamemode));
        assertFalse(parse("minecraft.gamemode").isStrictlyBelow(gamemode));
        assertFalse(parse("minecraft.time.set").isStrictlyBelow(gamemode));
        assertFalse(parse("minecraft.*").isStrictlyBelow(gamemode));
        assertFalse(parse("*").isStrictlyBelow(gamemode));
    }

    @Test
    void textWritesThePatternBack() {
        assertEquals("*", EntryPattern.parse("*").orElseThrow().text());
        assertEquals("ftb.*", EntryPattern.parse("FTB").orElseThrow().text());
        assertEquals("minecraft.gamemode.creative", EntryPattern.parse("minecraft.gamemode.creative").orElseThrow().text());
    }
}
