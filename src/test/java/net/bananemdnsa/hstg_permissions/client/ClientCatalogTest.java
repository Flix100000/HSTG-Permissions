package net.bananemdnsa.hstg_permissions.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.bananemdnsa.hstg_permissions.catalog.CommandCatalog;
import net.bananemdnsa.hstg_permissions.tree.SampleTree;
import org.junit.jupiter.api.Test;

class ClientCatalogTest {

    @Test
    void askingAgainKeepsTheLastAnswerUntilTheNextArrives() {
        CommandCatalog first = SampleTree.catalog(List.of());
        ClientCatalog.received(first);
        long before = ClientCatalog.version();
        ClientCatalog.loading();
        assertEquals(ClientCatalog.State.LOADING, ClientCatalog.state());
        assertSame(first, ClientCatalog.get());
        assertTrue(ClientCatalog.version() > before);
    }

    @Test
    void aServerWithoutTheAddonLeavesNothingBehind() {
        ClientCatalog.received(SampleTree.catalog(List.of()));
        ClientCatalog.unavailable();
        assertEquals(ClientCatalog.State.UNAVAILABLE, ClientCatalog.state());
        assertNull(ClientCatalog.get());
    }
}
