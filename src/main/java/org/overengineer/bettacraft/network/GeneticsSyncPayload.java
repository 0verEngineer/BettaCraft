package org.overengineer.bettacraft.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.overengineer.bettacraft.BettaCraft;

/** Server -> client (join and /reload): the datapack genetics (colours, genes, varieties) of {@code GeneticsData}. */
public record GeneticsSyncPayload(CompoundTag data) implements CustomPacketPayload {
    public static final Type<GeneticsSyncPayload> TYPE = new Type<>(BettaCraft.id("genetics"));

    public static final StreamCodec<FriendlyByteBuf, GeneticsSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, GeneticsSyncPayload::data, GeneticsSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
