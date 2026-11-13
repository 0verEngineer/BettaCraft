package org.overengineer.bettacraft.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.overengineer.bettacraft.BettaCraft;

/**
 * Server -> client (M14): the shrimp, crayfish and snails of the tank whose controller is at {@code controller}, in the
 * compact format of {@link org.overengineer.bettacraft.critters.CritterSync#encode} (18 bytes per animal). Sent
 * every care step while a tank has animals, to the players tracking its chunk.
 */
public record TankCrittersPayload(BlockPos controller, byte[] data) implements CustomPacketPayload {
    public static final Type<TankCrittersPayload> TYPE = new Type<>(BettaCraft.id("tank_critters"));

    public static final StreamCodec<FriendlyByteBuf, TankCrittersPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TankCrittersPayload::controller,
            ByteBufCodecs.byteArray(1 << 20), TankCrittersPayload::data,
            TankCrittersPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
