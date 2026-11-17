package org.overengineer.bettacraft.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.overengineer.bettacraft.BettaCraft;

/**
 * M51, client -> server: an action in the menu of a hired aquarist (entity id, action, value, text). The server checks
 * distance and who may do it ({@code aquarist.AquaristMenu#handle}).
 */
public record AquaristActionPayload(int entity, int action, int value, String text) implements CustomPacketPayload {
    public static final Type<AquaristActionPayload> TYPE = new Type<>(BettaCraft.id("aquarist_action"));

    public static final StreamCodec<FriendlyByteBuf, AquaristActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AquaristActionPayload::entity,
            ByteBufCodecs.VAR_INT, AquaristActionPayload::action,
            ByteBufCodecs.VAR_INT, AquaristActionPayload::value,
            ByteBufCodecs.stringUtf8(64), AquaristActionPayload::text,
            AquaristActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
