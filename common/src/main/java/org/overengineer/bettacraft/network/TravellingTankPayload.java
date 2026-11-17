package org.overengineer.bettacraft.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.overengineer.bettacraft.BettaCraft;

/**
 * Server -> client (M50): the state of a tank travelling on a Create contraption (entity {@code entity}, the controller at
 * the contraption-local {@code local}): what the client's copy of the controller loads (the client view of the tank data and
 * its packed animals as they are now). Sent every two seconds of the trip to the players tracking the contraption.
 */
public record TravellingTankPayload(int entity, BlockPos local, CompoundTag view) implements CustomPacketPayload {
    public static final Type<TravellingTankPayload> TYPE = new Type<>(BettaCraft.id("travelling_tank"));

    public static final StreamCodec<FriendlyByteBuf, TravellingTankPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TravellingTankPayload::entity,
            BlockPos.STREAM_CODEC, TravellingTankPayload::local,
            ByteBufCodecs.COMPOUND_TAG, TravellingTankPayload::view,
            TravellingTankPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
