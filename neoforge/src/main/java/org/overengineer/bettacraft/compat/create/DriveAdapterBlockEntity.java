package org.overengineer.bettacraft.compat.create;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.tank.TankDrive;

/**
 * M41: the drive adapter's block entity, a member of Create's kinetic network. Every care step of the tank at its back
 * gives it a load ({@link #load}); its stress impact follows that load ({@link TankDrive#impact}), so the network pays for
 * what the tank's devices need. An overstressed network gives the tank nothing. Without a tank (or with the switch off) it
 * turns idle.
 */
public class DriveAdapterBlockEntity extends KineticBlockEntity implements TankDrive.Input {
    /** Shown in the goggles: no tank, idle (the tank needs nothing), charging, overstressed. */
    public static final int NO_TANK = 0, IDLE = 1, CHARGING = 2, OVERSTRESSED = 3;
    private float impact;
    private double fe;
    private int state = NO_TANK;
    private long lastLoad = Long.MIN_VALUE;

    public DriveAdapterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        impact = idleImpact();
    }

    public DriveAdapterBlockEntity(BlockPos pos, BlockState blockState) {
        this(CreateDrive.ADAPTER_BE.get(), pos, blockState);
    }

    private static float idleImpact() {
        return TankDrive.enabled() || TankDrive.BRIDGE == null ? (float) TankDrive.idleStress() : 0F;
    }

    @Override
    public float calculateStressApplied() {
        lastStressApplied = impact;
        return impact;
    }

    /** FE per tick it gives its tank now (0 while idle or overstressed). */
    public double fePerTick() {
        return state == CHARGING ? fe : 0;
    }

    public int driveState() {
        return state;
    }

    /** Stress impact per RPM now. */
    @Override
    public float stressImpact() {
        return impact;
    }

    // ------------------------------------------------------------------ TankDrive.Input

    @Override
    public BlockPos pos() {
        return getBlockPos();
    }

    @Override
    public float speed() {
        return Math.abs(getTheoreticalSpeed());
    }

    @Override
    public boolean overstressed() {
        return isOverStressed();
    }

    @Override
    public void load(double fePerTick, boolean delivering) {
        lastLoad = level == null ? 0 : level.getGameTime();
        float rpm = speed();
        float next = TankDrive.impact(fePerTick, rpm, TankDrive.stressPerFe(), TankDrive.idleStress());
        int st = isOverStressed() ? OVERSTRESSED : delivering ? CHARGING : IDLE;
        set(next, fePerTick, st);
    }

    private void set(float nextImpact, double nextFe, int nextState) {
        boolean stressChanged = Math.abs(nextImpact - impact) > 1e-4F;
        boolean shown = stressChanged || Math.abs(nextFe - fe) > 0.5 || nextState != state;
        impact = nextImpact;
        fe = nextFe;
        state = nextState;
        if (stressChanged && hasNetwork()) getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
        if (shown) {
            setChanged();
            sendData();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide() || (level.getGameTime() + worldPosition.asLong()) % 20 != 0) return;
        // no care step served it for a while (no tank at its back, the tank gone, the switch off): idle
        boolean served = lastLoad != Long.MIN_VALUE && level.getGameTime() - lastLoad <= CreateDrive.STALE && level.getGameTime() >= lastLoad;
        if (!served || !TankDrive.enabled()) set(idleImpact(), 0, NO_TANK);
    }

    // ------------------------------------------------------------------ NBT and goggles

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("DriveImpact", impact);
        tag.putDouble("DriveFe", fe);
        tag.putInt("DriveState", state);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("DriveImpact")) impact = tag.getFloatOr("DriveImpact", 0f);
        fe = tag.getDoubleOr("DriveFe", 0d);
        state = tag.getIntOr("DriveState", 0);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.literal("    ").append(Component.translatable("goggles.bettacraft.drive_adapter").withStyle(ChatFormatting.GRAY)));
        Component line = switch (state) {
            case CHARGING -> Component.translatable("goggles.bettacraft.drive_adapter.charging", Math.round(fe)).withStyle(ChatFormatting.AQUA);
            case OVERSTRESSED -> Component.translatable("goggles.bettacraft.drive_adapter.overstressed").withStyle(ChatFormatting.RED);
            case IDLE -> Component.translatable("goggles.bettacraft.drive_adapter.idle").withStyle(ChatFormatting.DARK_GRAY);
            default -> Component.translatable("goggles.bettacraft.drive_adapter.no_tank").withStyle(ChatFormatting.GOLD);
        };
        tooltip.add(Component.literal("    ").append(line));
        return true;
    }
}
