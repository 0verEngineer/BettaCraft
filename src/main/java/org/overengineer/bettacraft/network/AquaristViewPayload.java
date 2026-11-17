package org.overengineer.bettacraft.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.overengineer.bettacraft.BettaCraft;

/** M51, server -> client: opens (or refreshes) the menu of a hired aquarist ({@code aquarist.AquaristMenu#view}). */
public record AquaristViewPayload(CompoundTag view) implements CustomPacketPayload {
    public static final Type<AquaristViewPayload> TYPE = new Type<>(BettaCraft.id("aquarist_view"));

    public static final StreamCodec<FriendlyByteBuf, AquaristViewPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, AquaristViewPayload::view, AquaristViewPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
