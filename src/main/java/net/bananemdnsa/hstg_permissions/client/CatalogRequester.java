package net.bananemdnsa.hstg_permissions.client;

import net.bananemdnsa.hstg_permissions.network.CatalogRequestPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.neoforge.network.PacketDistributor;

/** Asks the server for its commands, or records that this server cannot answer. */
final class CatalogRequester {

    private CatalogRequester() {}

    static void request() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null || !connection.hasChannel(CatalogRequestPayload.TYPE)) {
            ClientCatalog.unavailable();
            return;
        }
        ClientCatalog.loading();
        PacketDistributor.sendToServer(new CatalogRequestPayload());
    }
}
