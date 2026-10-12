package net.bananemdnsa.hstg_permissions.network;

import java.util.List;

import io.netty.buffer.ByteBuf;
import net.bananemdnsa.hstg_permissions.HSTGPermissions;
import net.bananemdnsa.hstg_permissions.client.ClientGameModes;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The game modes this player may switch to, sent whenever their command tree is. */
public record GameModesPayload(List<String> allowed) implements CustomPacketPayload {

    public static final Type<GameModesPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HSTGPermissions.MODID, "game_modes"));

    public static final StreamCodec<ByteBuf, GameModesPayload> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).map(GameModesPayload::new, GameModesPayload::allowed);

    public static void handle(GameModesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientGameModes.set(payload.allowed()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
