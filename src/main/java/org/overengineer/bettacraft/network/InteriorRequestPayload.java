package org.overengineer.bettacraft.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.overengineer.bettacraft.BettaCraft;

/** Client -> server: "I see the controller at {@code controller}, please send region {@code regionId}". */
public record InteriorRequestPayload(int regionId, BlockPos controller, int knownVersion) implements CustomPacketPayload {
    public static final Type<InteriorRequestPayload> TYPE = new Type<>(BettaCraft.id("interior_request"));

    public static final StreamCodec<FriendlyByteBuf, InteriorRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, InteriorRequestPayload::regionId,
            BlockPos.STREAM_CODEC, InteriorRequestPayload::controller,
            ByteBufCodecs.VAR_INT, InteriorRequestPayload::knownVersion,
            InteriorRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
