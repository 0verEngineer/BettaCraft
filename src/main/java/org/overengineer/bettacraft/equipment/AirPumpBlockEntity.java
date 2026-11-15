package org.overengineer.bettacraft.equipment;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M23: the air pump. Stores its power regulator (0..10 = 0..100 %) and, set by the tank's care step, how many air
 * stones and sponge filters it drives right now ({@link #driven}; 0 = it stands still: no tank, nothing to drive,
 * switched to 0 % or no FE). The client hums while it runs and draws the air hose into the tank.
 */
public class AirPumpBlockEntity extends BlockEntity implements AirSource {
    /** Client-side pumps currently loaded (hum, hose; never touched on the server). */
    public static final java.util.Set<AirPumpBlockEntity> CLIENT_LOADED =
            java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    private int power = 10;
    private int driven;

    public AirPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AIR_PUMP.get(), pos, state);
    }

    /** Regulator 0..10. */
    public int power() {
        return power;
    }

    /** Share 0..1 of the regulator (1 while features.power.regulator is off). */
    public double powerShare() {
        return Features.enabled(Feature.POWER_REGULATOR) ? power / 10.0 : 1.0;
    }

    public void setPower(int p) {
        p = Math.max(0, Math.min(10, p));
        if (p == power) return;
        power = p;
        changed();
    }

    /** Air stones and sponge filters driven right now (0 = standing still). */
    public int driven() {
        return driven;
    }

    public boolean running() {
        return driven > 0;
    }

    /** Server: what the care step found out. */
    public void setDriven(int n) {
        if (n == driven) return;
        driven = n;
        changed();
    }

    @Override
    public double airShare() {
        return powerShare();
    }

    @Override
    public boolean airRunning() {
        return running();
    }

    @Override
    public void setAirDriven(int n) {
        setDriven(n);
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    /** The tank block next to (or under) the pump whose interior it serves, or null. */
    @Nullable
    public BlockPos tankBlockPos() {
        if (level == null) return null;
        return tankBlock(level, worldPosition);
    }

    /** First tank block among the neighbours: below first (a pump on the tank), then the sides. */
    @Nullable
    public static BlockPos tankBlock(Level level, BlockPos pos) {
        for (Direction d : new Direction[]{Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            BlockPos n = pos.relative(d);
            if (level.isLoaded(n) && level.getBlockState(n).getBlock() instanceof TankBlock) return n;
        }
        return null;
    }

    @Nullable
    public static TankBlockEntity findTank(Level level, BlockPos pump) {
        BlockPos t = tankBlock(level, pump);
        return t == null ? null : TankMultiblock.getController(level, t);
    }

    static void clientTick(Level level, BlockPos pos, BlockState state, AirPumpBlockEntity be) {
        org.overengineer.bettacraft.util.ClientHooks.pumpTick.accept(be);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) CLIENT_LOADED.add(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        CLIENT_LOADED.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        CLIENT_LOADED.remove(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("power", power);
        tag.putInt("driven", driven);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.contains("power") ? Math.max(0, Math.min(10, tag.getInt("power"))) : 10;
        driven = Math.max(0, tag.getInt("driven"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag t = new CompoundTag();
        t.putInt("power", power);
        t.putInt("driven", driven);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
