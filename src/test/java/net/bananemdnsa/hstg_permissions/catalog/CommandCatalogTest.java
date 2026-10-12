package net.bananemdnsa.hstg_permissions.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.bananemdnsa.hstg_permissions.rules.EntryPattern;
import net.bananemdnsa.hstg_permissions.rules.EntryProblems;
import net.bananemdnsa.hstg_permissions.rules.PermissionEntry;
import net.bananemdnsa.hstg_permissions.tree.CommandTree;
import net.bananemdnsa.hstg_permissions.tree.SampleTree;
import org.junit.jupiter.api.Test;

class CommandCatalogTest {

    private final CommandTree<Object> tree = SampleTree.build();
    private final CommandCatalog catalog = SampleTree.catalog(List.of("op"));

    @Test
    void existsAgreesWithTheTree() {
        for (String raw : List.of("*", "minecraft.*", "ftbquests.*", "nomod.*",
                "minecraft.gamemode", "minecraft.gamemode.creative", "minecraft.gamemode.creatve",
                "minecraft.tp", "minecraft.teleport", "minecraft.teleport.steve", "minecraft.tp.steve",
                "minecraft.time.set.day", "minecraft.time.set", "minecraft.time.bogus",
                "ftbquests.quests.open_book", "minecraft.op")) {
            EntryPattern pattern = EntryPattern.parse(raw).orElseThrow();
            assertEquals(tree.exists(pattern), catalog.exists(pattern), raw);
        }
    }

    @Test
    void listsWordsAndKnownValuesButNoPlaceholders() {
        assertTrue(catalog.nodes().contains(new CommandCatalog.Node(List.of("gamemode", "creative"), true)));
        assertTrue(catalog.nodes().contains(new CommandCatalog.Node(List.of("time", "set", "day"), false)));
        assertTrue(catalog.nodes().contains(new CommandCatalog.Node(List.of("tp"), false)));
        assertFalse(catalog.nodes().stream().anyMatch(node -> node.path().stream().anyMatch(word -> word.startsWith("<"))));
    }

    @Test
    void namesWithADotAreNotListed() {
        assertFalse(catalog.nodes().stream().anyMatch(node -> node.path().contains("weird.name")));
    }

    @Test
    void knowsModsAndAliasTargets() {
        assertEquals("teleport", catalog.aliasTargets().get("tp"));
        assertEquals("ftbquests", catalog.modOfRoot("quests"));
        assertEquals("unknown", catalog.modOfRoot("nope"));
        assertEquals(List.of("ftbquests", "minecraft"), List.copyOf(catalog.mods()));
    }

    @Test
    void judgesEntriesLikeTheReport() {
        assertTrue(catalog.problems(new PermissionEntry("minecraft.gamemode.creative")).isEmpty());
        assertTrue(catalog.problems(new PermissionEntry("minecraft.op")).stream()
                .anyMatch(problem -> problem.kind() == EntryProblems.Kind.PROTECTED));
        assertEquals(EntryProblems.Kind.NOT_FOUND,
                catalog.problems(new PermissionEntry("minecraft.gamemode.creatve")).get(0).kind());
    }

    @Test
    void anAliasResolvesToItsTarget() {
        assertEquals(List.of("teleport", "steve"), catalog.canonical(List.of("tp", "steve")));
        assertEquals(List.of("time", "set"), catalog.canonical(List.of("time", "set")));
        assertEquals(List.of(), catalog.canonical(List.of()));
    }
}
