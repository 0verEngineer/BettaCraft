package org.overengineer.bettacraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.plumbing.WaterChangerBlockEntity;

/**
 * Client -> server (M24): the settings of a water change controller (interval in days, share in %, on/off) and
 * {@code changeNow}. The server checks distance and build rights like for the power regulator.
 */
public record WaterChangerPayload(BlockPos pos, int interval, int share, boolean enabled, boolean changeNow) implements CustomPacketPayload {
    public static final Type<WaterChangerPayload> TYPE = new Type<>(BettaCraft.id("water_changer"));

    public static final StreamCodec<ByteBuf, WaterChangerPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, WaterChangerPayload::pos, ByteBufCodecs.VAR_INT, WaterChangerPayload::interval, ByteBufCodecs.VAR_INT, WaterChangerPayload::share,
            ByteBufCodecs.BOOL, WaterChangerPayload::enabled, ByteBufCodecs.BOOL, WaterChangerPayload::changeNow, WaterChangerPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Server side: checked and applied. */
    public static void apply(ServerPlayer player, WaterChangerPayload p) {
        if (player.distanceToSqr(p.pos().getCenter()) > 64 || !player.mayBuild() || player.isSpectator()) return;
        // M33 part d (hardening): claims / spawn protection and the build rights of an interior, like the regulator
        if (!player.level().isLoaded(p.pos()) || !player.level().mayInteract(player, p.pos())) return;
        if (org.overengineer.bettacraft.tank.interior.InteriorManager.isInterior(player.level())
                && !org.overengineer.bettacraft.tank.interior.InteriorManager.canBuild(player, p.pos())) return;
        if (!(player.level().getBlockEntity(p.pos()) instanceof WaterChangerBlockEntity be)) return;
        be.set(p.interval(), p.share(), p.enabled());
        if (p.changeNow() && be.manualChangeAllowed(player.level().getGameTime())) {
            int mb = be.check(player.serverLevel(), true);
            player.displayClientMessage(Component.translatable(mb > 0 ? "message.bettacraft.water_changer.changed" : "message.bettacraft.water_changer.not_changed", mb), true);
        }
    }
}
