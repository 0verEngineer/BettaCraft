package org.overengineer.bettacraft.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.overengineer.bettacraft.BettaCraft;

/** Server -> client: new content of the open tank menu (only sent when it changed). */
public record TankViewPayload(int containerId, CompoundTag view) implements CustomPacketPayload {
    public static final Type<TankViewPayload> TYPE = new Type<>(BettaCraft.id("tank_view"));

    public static final StreamCodec<FriendlyByteBuf, TankViewPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TankViewPayload::containerId,
            ByteBufCodecs.COMPOUND_TAG, TankViewPayload::view,
            TankViewPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
