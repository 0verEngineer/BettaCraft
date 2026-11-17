package org.overengineer.bettacraft.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.overengineer.bettacraft.BettaCraft;

/**
 * Client -> server (M27): a new dashboard layout ({@link org.overengineer.bettacraft.monitor.DashboardLayout#save}) for the
 * monitor screen at {@code pos} (any of its blocks), from the editor (part b). The server checks distance and build rights
 * and clips the layout to the screen ({@link org.overengineer.bettacraft.monitor.MonitorBlockEntity#applyLayout}).
 */
public record MonitorLayoutPayload(BlockPos pos, CompoundTag layout) implements CustomPacketPayload {
    public static final Type<MonitorLayoutPayload> TYPE = new Type<>(BettaCraft.id("monitor_layout"));

    public static final StreamCodec<FriendlyByteBuf, MonitorLayoutPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, MonitorLayoutPayload::pos, ByteBufCodecs.COMPOUND_TAG, MonitorLayoutPayload::layout,
            MonitorLayoutPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
