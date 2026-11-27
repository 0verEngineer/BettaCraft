package org.overengineer.bettacraft.plumbing;

import org.overengineer.bettacraft.util.NbtCompat;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;

/**
 * M24: the level sensor at a tank (beside or on it; M49 also beside the cabinet of a sump): it watches the water level against its
 * target (100 % by default; right-click: 100/95/90/80/70/60/50 %).
 * <p>
 * M68 part B: it only measures, like a float switch. When the water is {@link #HYSTERESIS_MB} or more below its target it reports
 * "below target" and gives a redstone signal of 15 (for own circuits or other mods' pumps). A water change controller that fills
 * this tank (directly or through a multiblock filter) reads it without a cable and tops the tank off through its fresh water line
 * ({@link WaterChangerBlockEntity}). No pipe joins it and it moves no water itself.
 */
public class LevelSensorBlockEntity extends BlockEntity {
    public static final int[] TARGETS = {100, 95, 90, 80, 70, 60, 50};
    /** It reports "below target" from this much missing water on (no pumping for every drop). */
    public static final int HYSTERESIS_MB = 50;
    /** The controller tops off at most this much a step. */
    public static final int MAX_PER_STEP = 1000;
    /** A controller read it within this many ticks: it counts as served. */
    public static final long SERVED_TICKS = 60;

    public enum Status { IDLE, LOW, NO_TANK, OFF }

    private int target = 100;
    private Status status = Status.IDLE;
    /** Synced: mB below the target (0 when fine), whether a controller reads it. */
    private long missing;
    private boolean served;
    @Nullable
    private BlockPos tank;
    private long readAt = Long.MIN_VALUE;

    public LevelSensorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LEVEL_SENSOR.get(), pos, state);
    }

    public int target() {
        return target;
    }

    public Status status() {
        return status;
    }

    /** mB the water is below the target (at the last check). */
    public long missingMb() {
        return missing;
    }

    /** Whether a water change controller reads it (tops its tank off). */
    public boolean served() {
        return served;
    }

    /** The controller block of the tank it watches (null: none). */
    @Nullable
    public BlockPos tankPos() {
        return tank;
    }

    /** Whether it gives its redstone signal now (the water is below the target). */
    public boolean signal() {
        return status == Status.LOW;
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, LevelSensorBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % 20 != 0) return;
        be.check((ServerLevel) level);
    }

    /** The tank it watches: a tank block below or beside it, or the tank above a cabinet with a sump beside it. */
    @Nullable
    public static TankBlockEntity tankOf(Level level, BlockPos pos) {
        TankBlockEntity c = org.overengineer.bettacraft.equipment.AirPumpBlockEntity.findTank(level, pos);
        if (c == null) c = org.overengineer.bettacraft.equipment.Sump.tankBeside(level, pos); // M49: next to the cabinet of a sump
        return c;
    }

    /** mB tank {@code c} is below this sensor's target (0 when at or above it; a fixed terrarium's limit counts). */
    public long missing(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        long want = t.capacityMb() * target / 100;
        // M33.1: never above the limit of a fixed terrarium or paludarium
        if (org.overengineer.bettacraft.terrarium.WaterLimit.limits(t)) want = Math.min(want, org.overengineer.bettacraft.terrarium.WaterLimit.maxWaterMb(level, c));
        return Math.max(0, want - t.waterMb());
    }

    /** A controller read it now. */
    public void read(long now) {
        readAt = now;
    }

    /** One check (public for the GameTests): measures, says it watches its tank ({@link TankWaterLinks}), sets the redstone signal. */
    public void check(ServerLevel level) {
        TankBlockEntity c = tankOf(level, worldPosition);
        Status st;
        long miss = 0;
        BlockPos at = c == null ? null : c.getBlockPos().immutable();
        if (!Features.enabled(Feature.TOP_OFF)) st = Status.OFF;
        else if (c == null) st = Status.NO_TANK;
        else {
            miss = missing(level, c);
            st = miss >= HYSTERESIS_MB ? Status.LOW : Status.IDLE;
        }
        if (at != null && st != Status.OFF) TankWaterLinks.sensor(level, at, worldPosition);
        else TankWaterLinks.forgetSensor(level, worldPosition);
        boolean srv = readAt != Long.MIN_VALUE && level.getGameTime() - readAt <= SERVED_TICKS && level.getGameTime() >= readAt;
        boolean sync = st != status || miss / 50 != missing / 50 || srv != served || !java.util.Objects.equals(at, tank);
        status = st;
        missing = miss;
        served = srv;
        tank = at;
        BlockState s = getBlockState();
        if (s.hasProperty(WaterDeviceBlock.POWERED) && s.getValue(WaterDeviceBlock.POWERED) != signal()) {
            level.setBlock(worldPosition, s.setValue(WaterDeviceBlock.POWERED, signal()), Block.UPDATE_ALL);
        }
        if (sync) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide()) TankWaterLinks.forgetSensor(level, worldPosition);
    }

    public void cycleTarget(Player player) {
        int i = 0;
        while (i < TARGETS.length && TARGETS[i] != target) i++;
        target = TARGETS[(i + 1) % TARGETS.length];
        setChanged();
        player.displayClientMessage(Component.translatable("message.bettacraft.level_sensor.target", target,
                Component.translatable("gui.bettacraft.level_sensor.status." + status.name().toLowerCase(java.util.Locale.ROOT))).withStyle(ChatFormatting.AQUA), true);
        if (level instanceof ServerLevel sl) check(sl);
    }

    public void setTarget(int pct) {
        target = Math.max(10, Math.min(100, pct));
        setChanged();
    }

    private void write(CompoundTag tag) {
        tag.putInt("target", target);
        tag.putString("status", status.name());
        tag.putLong("missing", missing);
        tag.putBoolean("served", served);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        write(tag);
        NbtCompat.write(output, tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        CompoundTag tag = NbtCompat.read(input);
        HolderLookup.Provider registries = input.lookup();
        super.loadAdditional(input);
        target = tag.contains("target") ? Math.max(10, Math.min(100, tag.getIntOr("target", 0))) : 100;
        try {
            status = Status.valueOf(tag.getStringOr("status", ""));
        } catch (IllegalArgumentException e) {
            status = Status.IDLE; // M68 part B: the old statuses (topped, no source ...) are gone
        }
        missing = tag.getLongOr("missing", 0L);
        served = tag.getBooleanOr("served", false);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag t = new CompoundTag();
        write(t);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
