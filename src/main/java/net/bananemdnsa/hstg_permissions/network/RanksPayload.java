package net.bananemdnsa.hstg_permissions.network;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import net.bananemdnsa.hstg_permissions.HSTGPermissions;
import net.bananemdnsa.hstg_permissions.client.ClientRanks;
import net.bananemdnsa.hstg_permissions.rank.Rank;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Ranks for the name tags above players' heads, which the client draws itself.
 *
 * @param replace whether this is the whole list (on login) rather than a few changes
 */
public record RanksPayload(boolean replace, List<Entry> entries) implements CustomPacketPayload {

    public record Entry(UUID player, String prefix, String suffix) {

        static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Entry::player,
                ByteBufCodecs.STRING_UTF8, Entry::prefix,
                ByteBufCodecs.STRING_UTF8, Entry::suffix,
                Entry::new);
    }

    public static final Type<RanksPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HSTGPermissions.MODID, "ranks"));

    public static final StreamCodec<ByteBuf, RanksPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, RanksPayload::replace,
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), RanksPayload::entries,
            RanksPayload::new);

    public static void handle(RanksPayload payload, IPayloadContext context) {
        Map<UUID, Rank> ranks = new LinkedHashMap<>();
        for (Entry entry : payload.entries()) ranks.put(entry.player(), new Rank(entry.prefix(), entry.suffix()));
        context.enqueueWork(() -> ClientRanks.apply(payload.replace(), ranks));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
