package net.bananemdnsa.hstg_permissions.network;

import net.bananemdnsa.hstg_permissions.HSTGPermissions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The editor asking which commands the server has. Operator-gated, like the rest of the editor's traffic. */
public record CatalogRequestPayload() implements CustomPacketPayload {

    public static final Type<CatalogRequestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HSTGPermissions.MODID, "catalog_request"));

    public static final StreamCodec<FriendlyByteBuf, CatalogRequestPayload> STREAM_CODEC =
            StreamCodec.unit(new CatalogRequestPayload());

    public static void handle(CatalogRequestPayload request, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            CatalogAnswer.forPlayer(player).ifPresent(catalog -> context.reply(new CatalogPayload(catalog)));
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
