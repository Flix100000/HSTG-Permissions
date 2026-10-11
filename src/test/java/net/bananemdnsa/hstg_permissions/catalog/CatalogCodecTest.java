package net.bananemdnsa.hstg_permissions.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import net.bananemdnsa.hstg_permissions.tree.SampleTree;
import org.junit.jupiter.api.Test;

class CatalogCodecTest {

    @Test
    void aCatalogSurvivesTheTrip() {
        CommandCatalog sent = SampleTree.catalog(List.of("op", "whitelist.add"));
        CommandCatalog received = CatalogCodec.decode(CatalogCodec.encode(sent));
        assertEquals(sent.nodes(), received.nodes());
        assertEquals(sent.rootMods(), received.rootMods());
        assertEquals(sent.aliasTargets(), received.aliasTargets());
        assertEquals(sent.openValues(), received.openValues());
        assertEquals(sent.protectedPaths(), received.protectedPaths());
        assertEquals(sent.neverLockableMods(), received.neverLockableMods());
    }

    @Test
    void anotherVersionIsRefused() {
        byte[] data = CatalogCodec.encode(SampleTree.catalog(List.of()));
        data[3] = 99; // the version int is written first, big-endian
        assertThrows(IllegalArgumentException.class, () -> CatalogCodec.decode(data));
    }
}
