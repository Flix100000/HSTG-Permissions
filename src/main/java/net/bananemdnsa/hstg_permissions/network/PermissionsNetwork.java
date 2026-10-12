package net.bananemdnsa.hstg_permissions.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class PermissionsNetwork {

    private PermissionsNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        // Optional, so a client with the addon can still join a server without it; the editor
        // then simply has no tree to offer.
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToServer(CatalogRequestPayload.TYPE, CatalogRequestPayload.STREAM_CODEC, CatalogRequestPayload::handle);
        registrar.playToClient(CatalogPayload.TYPE, CatalogPayload.STREAM_CODEC, CatalogPayload::handle);
    }
}
