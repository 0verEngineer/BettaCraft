package org.overengineer.bettacraft.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.overengineer.bettacraft.BettaCraft;

/**
 * Server -> client (M12): the floating plants of the tank whose controller is at {@code controller}, in the compact
 * format of {@link org.overengineer.bettacraft.tank.plants.FloatingPlantSim#encode} (about 12 bytes per plant). Sent
 * every care step while a tank has plants, to the players tracking its chunk.
 */
public record TankPlantsPayload(BlockPos controller, byte[] data) implements CustomPacketPayload {
    public static final Type<TankPlantsPayload> TYPE = new Type<>(BettaCraft.id("tank_plants"));

    public static final StreamCodec<FriendlyByteBuf, TankPlantsPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TankPlantsPayload::controller,
            ByteBufCodecs.byteArray(1 << 20), TankPlantsPayload::data,
            TankPlantsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
