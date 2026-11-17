package org.overengineer.bettacraft.network;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.overengineer.bettacraft.BettaCraft;

/**
 * Client -> server: an action in the tank menu. Validated on the server against the open menu, distance, lock and
 * limits ({@link org.overengineer.bettacraft.tank.menu.TankActions}).
 */
public record TankActionPayload(int containerId, int action, int value, String text, Optional<UUID> target) implements CustomPacketPayload {
    public static final Type<TankActionPayload> TYPE = new Type<>(BettaCraft.id("tank_action"));

    public static final StreamCodec<FriendlyByteBuf, TankActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TankActionPayload::containerId,
            ByteBufCodecs.VAR_INT, TankActionPayload::action,
            ByteBufCodecs.VAR_INT, TankActionPayload::value,
            ByteBufCodecs.stringUtf8(64), TankActionPayload::text,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), TankActionPayload::target,
            TankActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
