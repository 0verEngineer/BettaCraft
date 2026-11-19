package org.overengineer.bettacraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.menu.TankMenu;
import org.overengineer.bettacraft.terrarium.TankClimate;
import org.overengineer.bettacraft.terrarium.TankModeSetting;
import org.overengineer.bettacraft.terrarium.WaterLimit;

/**
 * Client -> server (M33.1): the tank type chosen in the tank menu ({@link TankModeSetting}). The server checks it like the
 * other menu actions and the M33 hardening: the open tank menu of this container and its reach ({@link TankMenu#stillValid}),
 * the lock ({@link TankInhabitants#canModify}), claims and spawn protection ({@code mayInteract}) and build rights, the
 * switch, a valid type, and at most one change every {@link WaterLimit#SET_TICKS} ticks per tank.
 */
public record TankModePayload(int containerId, int mode) implements CustomPacketPayload {
    public static final Type<TankModePayload> TYPE = new Type<>(BettaCraft.id("tank_mode"));

    public static final StreamCodec<ByteBuf, TankModePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TankModePayload::containerId, ByteBufCodecs.VAR_INT, TankModePayload::mode, TankModePayload::new);

    /** What the server made of a request (for the GameTests). */
    public enum Result { SET, UNCHANGED, NO_MENU, OFF, INVALID, LOCKED, PROTECTED, THROTTLED }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Server side: checked and applied. */
    public static Result apply(ServerPlayer player, TankModePayload p) {
        if (!(player.containerMenu instanceof TankMenu menu) || menu.containerId != p.containerId() || player.isSpectator()) return Result.NO_MENU;
        if (!menu.stillValid(player)) return Result.NO_MENU;
        TankBlockEntity c = menu.controller();
        if (c == null) return Result.NO_MENU;
        if (!WaterLimit.enabled()) return Result.OFF;
        if (p.mode() < 0 || p.mode() >= TankModeSetting.values().length) return Result.INVALID;
        TankData t = c.getTank();
        ServerLevel level = player.level();
        if (!TankInhabitants.canModify(player, t)) {
            player.sendOverlayMessage(Component.translatable("message.bettacraft.tank.locked").withStyle(ChatFormatting.RED));
            return Result.LOCKED;
        }
        // M33 part d pattern: claims / spawn protection and the build rights of the player
        if (!player.mayBuild() || !level.isLoaded(c.getBlockPos()) || !level.mayInteract(player, c.getBlockPos())) {
            player.sendOverlayMessage(Component.translatable("message.bettacraft.tank_mode.protected").withStyle(ChatFormatting.RED));
            return Result.PROTECTED;
        }
        long now = level.getGameTime();
        if (c.modeSetTime != Long.MIN_VALUE && now >= c.modeSetTime && now - c.modeSetTime < WaterLimit.SET_TICKS) return Result.THROTTLED;
        TankModeSetting mode = TankModeSetting.byOrdinal(p.mode());
        if (mode == t.tankMode()) return Result.UNCHANGED;
        c.modeSetTime = now;
        t.setTankMode(mode);
        c.limitMb = -1;
        TankClimate.refreshMode(level, c);
        TankCare.log(level, c, "log.bettacraft.tank_mode.set", TankCare.nameArg(player), "@" + mode.translationKey());
        // nothing is removed: a fixed type that does not fit the water says so, and only keeps more water out
        if (WaterLimit.limits(t) && t.waterMb() > WaterLimit.maxWaterMb(level, c)) {
            player.sendOverlayMessage(Component.translatable(mode == TankModeSetting.TERRARIUM ? "message.bettacraft.tank_mode.water_stays_terrarium"
                    : "message.bettacraft.tank_mode.water_stays_paludarium", WaterLimit.maxWaterMb(level, c)).withStyle(ChatFormatting.GOLD));
        } else {
            player.sendOverlayMessage(Component.translatable("message.bettacraft.tank_mode.set", Component.translatable(mode.translationKey()))
                    .withStyle(ChatFormatting.GREEN));
        }
        c.setChanged();
        c.markTankChanged();
        menu.requestSync();
        return Result.SET;
    }
}
