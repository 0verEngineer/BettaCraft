package org.overengineer.bettacraft.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.overengineer.bettacraft.BettaCraft;

/**
 * Server -> client: changed sections of an interior region from version {@code fromVersion} to {@code toVersion}
 * (same section encoding as the full sync; emptied sections have palette size 0). A client that does not hold
 * {@code fromVersion} ignores it and requests a full resync.
 */
public record InteriorDeltaPayload(int regionId, int fromVersion, int toVersion, byte[] data) implements CustomPacketPayload {
    public static final Type<InteriorDeltaPayload> TYPE = new Type<>(BettaCraft.id("interior_delta"));

    public static final StreamCodec<FriendlyByteBuf, InteriorDeltaPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.regionId);
                buf.writeVarInt(p.fromVersion);
                buf.writeVarInt(p.toVersion);
                buf.writeByteArray(p.data);
            },
            buf -> new InteriorDeltaPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readByteArray(InteriorDataPayload.MAX_PART_BYTES + 1024)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
