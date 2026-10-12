package net.bananemdnsa.hstg_permissions.client;

import net.bananemdnsa.hstg_permissions.HSTGPermissions;
import net.bananemdnsa.hstg_permissions.rank.RankDisplay;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;

@EventBusSubscriber(modid = HSTGPermissions.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {}

    // Rows must not be judged against the commands of the server we just left.
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientCatalog.reset();
        ClientGameModes.reset();
        ClientRanks.reset();
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ClientRanks.get(player.getUUID())
                .ifPresent(rank -> event.setContent(RankDisplay.decorate(event.getContent(), rank)));
    }
}
