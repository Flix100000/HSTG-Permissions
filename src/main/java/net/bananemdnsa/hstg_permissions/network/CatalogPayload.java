package net.bananemdnsa.hstg_permissions.network;

import net.bananemdnsa.hstg_permissions.HSTGPermissions;
import net.bananemdnsa.hstg_permissions.catalog.CatalogCodec;
import net.bananemdnsa.hstg_permissions.catalog.CommandCatalog;
import net.bananemdnsa.hstg_permissions.client.ClientCatalog;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CatalogPayload(CommandCatalog catalog) implements CustomPacketPayload {

    public static final Type<CatalogPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HSTGPermissions.MODID, "catalog"));

    public static final StreamCodec<FriendlyByteBuf, CatalogPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeByteArray(CatalogCodec.encode(payload.catalog())),
            buf -> new CatalogPayload(CatalogCodec.decode(buf.readByteArray())));

    public static void handle(CatalogPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientCatalog.received(payload.catalog()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
