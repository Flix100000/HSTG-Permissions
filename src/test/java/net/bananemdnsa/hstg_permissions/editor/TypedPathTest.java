package net.bananemdnsa.hstg_permissions.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import net.bananemdnsa.hstg_permissions.catalog.CommandCatalog;
import net.bananemdnsa.hstg_permissions.tree.SampleTree;
import org.junit.jupiter.api.Test;

class TypedPathTest {

    private final CommandCatalog catalog = SampleTree.catalog(List.of());

    private String resolve(String typed) {
        return TypedPath.resolve(typed, catalog);
    }

    @Test
    void theModNameIsAddedWhenTheCommandIsKnown() {
        assertEquals("minecraft.gamemode.creative", resolve("gamemode.creative"));
        assertEquals("minecraft.gamemode", resolve("gamemode"));
        assertEquals("minecraft.tp", resolve("tp"));
        assertEquals("ftbquests.quests.open_book", resolve("quests.open_book"));
    }

    @Test
    void typedPathsAreLowerCasedAndCompleted() {
        assertEquals("minecraft.gamemode", resolve(" Minecraft.GameMode "));
        assertEquals("minecraft.gamemode.creative", resolve("GAMEMODE.Creative"));
    }

    @Test
    void aModOnItsOwnMeansAllOfIt() {
        assertEquals("ftbquests.*", resolve("ftbquests"));
        assertEquals("ftbquests.*", resolve("ftbquests.*"));
        assertEquals("*", resolve(" * "));
    }

    @Test
    void aTrailingStarChangesNothing() {
        assertEquals("minecraft.gamemode", resolve("gamemode.*"));
        assertEquals("minecraft.gamemode", resolve("minecraft.gamemode.*"));
        assertEquals("minecraft.gamemode.creative", resolve("gamemode.creative.*"));
        assertEquals("ftbquests.*", resolve("ftbquests.*"));
        assertEquals("foo.*", resolve("foo.*"));
    }

    @Test
    void unknownPathsStayAsTyped() {
        assertEquals("waystones.waystone", resolve("waystones.waystone"));
        // tp exists, but under minecraft; whoever typed a mod name meant that mod's command.
        assertEquals("waystones.tp", resolve("waystones.tp"));
        assertEquals("minecraft..op", resolve("minecraft..op"));
    }
}
