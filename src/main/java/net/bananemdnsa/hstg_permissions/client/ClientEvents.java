package net.bananemdnsa.hstg_permissions.client;

import net.bananemdnsa.hstg_permissions.HSTGPermissions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = HSTGPermissions.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {}

    // Rows must not be judged against the commands of the server we just left.
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientCatalog.reset();
    }
}
