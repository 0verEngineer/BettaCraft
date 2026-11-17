package org.overengineer.bettacraft.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.overengineer.bettacraft.BettaCraft;

/**
 * Server -> client: (part of) the block data of an interior region, encoded by
 * {@link org.overengineer.bettacraft.tank.interior.InteriorSnapshot}. Large regions are split into several parts.
 */
public record InteriorDataPayload(int regionId, int version, int part, int parts, int sizeX, int sizeY, int sizeZ,
                                  int scale, boolean truncated, byte[] data) implements CustomPacketPayload {
    public static final Type<InteriorDataPayload> TYPE = new Type<>(BettaCraft.id("interior_data"));
    public static final int MAX_PART_BYTES = 512 * 1024;

    public static final StreamCodec<FriendlyByteBuf, InteriorDataPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.regionId);
                buf.writeVarInt(p.version);
                buf.writeVarInt(p.part);
                buf.writeVarInt(p.parts);
                buf.writeVarInt(p.sizeX);
                buf.writeVarInt(p.sizeY);
                buf.writeVarInt(p.sizeZ);
                buf.writeVarInt(p.scale);
                buf.writeBoolean(p.truncated);
                buf.writeByteArray(p.data);
            },
            buf -> new InteriorDataPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(),
                    buf.readByteArray(MAX_PART_BYTES + 1024)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
