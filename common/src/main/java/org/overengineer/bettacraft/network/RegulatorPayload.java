package org.overengineer.bettacraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.overengineer.bettacraft.BettaCraft;

/**
 * Client -> server (M23): the power regulator of the device at {@code pos}: set to {@code value} (0..10) or, with
 * {@code relative}, changed by {@code value} steps (sneak + mouse wheel). The server checks distance, build rights and
 * the feature switch ({@link org.overengineer.bettacraft.equipment.Regulator#apply}).
 */
public record RegulatorPayload(BlockPos pos, int value, boolean relative) implements CustomPacketPayload {
    public static final Type<RegulatorPayload> TYPE = new Type<>(BettaCraft.id("regulator"));

    public static final StreamCodec<ByteBuf, RegulatorPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RegulatorPayload::pos, ByteBufCodecs.VAR_INT, RegulatorPayload::value, ByteBufCodecs.BOOL, RegulatorPayload::relative,
            RegulatorPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
