package org.overengineer.bettacraft.plumbing;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
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
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInteractions;

/**
 * M24: the level sensor of the top-off. Next to a tank (or a multiblock filter) it watches the water level; when
 * evaporation took more than a little below its target (100 % by default; right-click: 100/95/90/80/70/60/50 %), it
 * tops off from the reservoir, pipe or other mod's tank next to it, or from a water source block. Only fresh water: in a
 * reef tank the salt stays behind when water evaporates, so topping off with fresh water keeps the salinity where it was
 * (salty sources are refused).
 */
public class LevelSensorBlockEntity extends BlockEntity {
    public static final int[] TARGETS = {100, 95, 90, 80, 70, 60, 50};
    /** It waits for this much missing water before it tops off (no pumping for every drop). */
    public static final int HYSTERESIS_MB = 50;
    public static final int MAX_PER_STEP = 1000;

    public enum Status { IDLE, TOPPED, NO_TANK, NO_SOURCE, SALTY, NO_POWER, OFF }

    private int target = 100;
    private Status status = Status.IDLE;
    private long toppedTotal;
    private final DeviceEnergy energy = new DeviceEnergy(10000, this::setChanged);

    public LevelSensorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LEVEL_SENSOR.get(), pos, state);
    }

    public DeviceEnergy energy() {
        return energy;
    }

    public int target() {
        return target;
    }

    public Status status() {
        return status;
    }

    public long toppedTotal() {
        return toppedTotal;
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, LevelSensorBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % 20 != 0) return;
        be.check((ServerLevel) level);
    }

    /** One check (public for the GameTests); returns the mB topped off. */
    public int check(ServerLevel level) {
        Status st;
        int added = 0;
        TankBlockEntity c = WaterSources.tank(level, worldPosition, true);
        if (!Features.enabled(Feature.TOP_OFF)) st = Status.OFF;
        else if (c == null) st = Status.NO_TANK;
        else {
            TankData t = c.getTank();
            long want = t.capacityMb() * target / 100;
            long missing = want - t.waterMb();
            if (missing < HYSTERESIS_MB) st = Status.IDLE;
            else {
                int amount = (int) Math.min(missing, MAX_PER_STEP);
                int fe = (int) Math.ceil(DeviceEnergy.pumpFe() * amount / 100.0);
                if (DeviceEnergy.required()) energy.drive(level, worldPosition, fe); // M32 part c: Create's rotation
                if (DeviceEnergy.required() && energy.getEnergyStored() < fe) st = Status.NO_POWER;
                else if (WaterSources.endless(level, worldPosition)) {
                    TankInteractions.topOff(level, c, amount, null);
                    added = amount;
                    st = Status.TOPPED;
                } else {
                    for (WaterSources.Portion p : WaterSources.take(level, worldPosition, amount, true)) {
                        TankInteractions.topOff(level, c, p.mb(), p.values());
                        added += p.mb();
                    }
                    st = added > 0 ? Status.TOPPED : WaterSources.onlySalty(level, worldPosition) ? Status.SALTY : Status.NO_SOURCE;
                }
                if (added > 0 && DeviceEnergy.required()) energy.use((int) Math.ceil(DeviceEnergy.pumpFe() * added / 100.0));
            }
        }
        if (added > 0) toppedTotal += added;
        if (st != status || added > 0) {
            status = st;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        return added;
    }

    public void cycleTarget(Player player) {
        int i = 0;
        while (i < TARGETS.length && TARGETS[i] != target) i++;
        target = TARGETS[(i + 1) % TARGETS.length];
        setChanged();
        player.displayClientMessage(Component.translatable("message.bettacraft.level_sensor.target", target,
                Component.translatable("gui.bettacraft.level_sensor.status." + status.name().toLowerCase(java.util.Locale.ROOT))).withStyle(ChatFormatting.AQUA), true);
    }

    public void setTarget(int pct) {
        target = Math.max(10, Math.min(100, pct));
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("target", target);
        tag.putString("status", status.name());
        tag.putLong("topped", toppedTotal);
        energy.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        target = tag.contains("target") ? Math.max(10, Math.min(100, tag.getInt("target"))) : 100;
        try {
            status = Status.valueOf(tag.getString("status"));
        } catch (IllegalArgumentException e) {
            status = Status.IDLE;
        }
        toppedTotal = tag.getLong("topped");
        energy.load(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag t = new CompoundTag();
        t.putInt("target", target);
        t.putString("status", status.name());
        t.putLong("topped", toppedTotal);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
