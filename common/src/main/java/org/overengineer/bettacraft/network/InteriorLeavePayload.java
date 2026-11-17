package org.overengineer.bettacraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.overengineer.bettacraft.BettaCraft;

/**
 * Server -> client (M15): progress of the "sneak + look up to leave" timer in a tank interior, every tick while it counts
 * and once with 0 when it starts over; the client draws it under the leave hint.
 */
public record InteriorLeavePayload(int ticks, int needed) implements CustomPacketPayload {
    public static final Type<InteriorLeavePayload> TYPE = new Type<>(BettaCraft.id("interior_leave"));

    public static final StreamCodec<ByteBuf, InteriorLeavePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, InteriorLeavePayload::ticks, ByteBufCodecs.VAR_INT, InteriorLeavePayload::needed, InteriorLeavePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
