package org.overengineer.bettacraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import org.overengineer.bettacraft.BettaCraft;

/**
 * Client -> server (M22.1): the player confirmed letting the animals of the container in this hand go
 * ({@link org.overengineer.bettacraft.containers.ContainerRules#confirm}).
 */
public record EmptyConfirmPayload(int hand) implements CustomPacketPayload {
    public static final Type<EmptyConfirmPayload> TYPE = new Type<>(BettaCraft.id("empty_confirm"));

    public static final StreamCodec<ByteBuf, EmptyConfirmPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EmptyConfirmPayload::hand, EmptyConfirmPayload::new);

    public EmptyConfirmPayload(InteractionHand hand) {
        this(hand.ordinal());
    }

    public InteractionHand interactionHand() {
        return hand == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
