package org.overengineer.bettacraft.network;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.overengineer.bettacraft.BettaCraft;

/**
 * Server -> client (M32 part d): a monitor alarm of a tank went off ({@code add}: a waypoint at {@code pos} in
 * {@code dimension} named after the tank and the value) or the last one of the tank cleared ({@code add} false: the
 * waypoint of {@code tank} goes away). The client hands it to the map mod (JourneyMap).
 */
public record AlarmWaypointPayload(UUID tank, boolean add, Identifier dimension, BlockPos pos, String name, String value)
        implements CustomPacketPayload {
    public static final Type<AlarmWaypointPayload> TYPE = new Type<>(BettaCraft.id("alarm_waypoint"));

    public static final StreamCodec<ByteBuf, AlarmWaypointPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, AlarmWaypointPayload::tank,
            ByteBufCodecs.BOOL, AlarmWaypointPayload::add,
            Identifier.STREAM_CODEC, AlarmWaypointPayload::dimension,
            BlockPos.STREAM_CODEC, AlarmWaypointPayload::pos,
            ByteBufCodecs.stringUtf8(64), AlarmWaypointPayload::name,
            ByteBufCodecs.stringUtf8(32), AlarmWaypointPayload::value,
            AlarmWaypointPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
