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
 * <p>
 * M40 part b (F1, {@code features.pipes.filterWater}): on a multiblock filter it watches every tank of the filter and tops
 * them all off through the filter's return line, also from the filter's fresh water ports and their lines; when the water
 * is short, each tank gets its part by what it misses ({@code filter.FilterWater}).
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
    /** M40 part b (synced): the tanks it watches through a multiblock filter (0: none, or a tank next to it). */
    private int filterTanks;
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

    public int filterTanks() {
        return filterTanks;
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, LevelSensorBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % 20 != 0) return;
        be.check((ServerLevel) level);
    }

    /** One check (public for the GameTests); returns the mB topped off. */
    public int check(ServerLevel level) {
        Status st;
        int added = 0;
        TankBlockEntity c = org.overengineer.bettacraft.equipment.AirPumpBlockEntity.findTank(level, worldPosition);
        if (c == null) c = org.overengineer.bettacraft.equipment.Sump.tankBeside(level, worldPosition); // M49: next to the cabinet of a sump
        var filter = c == null ? org.overengineer.bettacraft.filter.FilterWater.filterNextTo(level, worldPosition) : null;
        if (c == null && filter == null) c = WaterSources.tank(level, worldPosition, true); // the filter switch off: its first tank, as before
        int viaFilter = 0;
        if (!Features.enabled(Feature.TOP_OFF)) st = Status.OFF;
        else if (filter != null) {
            var tanks = org.overengineer.bettacraft.filter.FilterWater.tanks(level, filter);
            viaFilter = tanks.size();
            if (tanks.isEmpty()) st = Status.NO_TANK;
            else {
                int[] r = topOffAll(level, filter, tanks);
                added = r[0];
                st = Status.values()[r[1]];
            }
        }
        else if (c == null) st = Status.NO_TANK;
        else {
            TankData t = c.getTank();
            long want = t.capacityMb() * target / 100;
            // M33.1: never above the limit of a fixed terrarium or paludarium
            if (org.overengineer.bettacraft.terrarium.WaterLimit.limits(t)) want = Math.min(want, org.overengineer.bettacraft.terrarium.WaterLimit.maxWaterMb(level, c));
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
        if (st != status || added > 0 || viaFilter != filterTanks) {
            status = st;
            filterTanks = viaFilter;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        return added;
    }

    /** M40 part b: tops off every tank of the filter; returns {mB added, status ordinal}. */
    private int[] topOffAll(ServerLevel level, org.overengineer.bettacraft.filter.FilterPumpHeadBlockEntity filter, java.util.List<TankBlockEntity> tanks) {
        long[] want = new long[tanks.size()];
        long sum = 0;
        for (int i = 0; i < want.length; i++) {
            TankBlockEntity c = tanks.get(i);
            TankData t = c.getTank();
            long goal = t.capacityMb() * target / 100;
            if (org.overengineer.bettacraft.terrarium.WaterLimit.limits(t)) goal = Math.min(goal, org.overengineer.bettacraft.terrarium.WaterLimit.maxWaterMb(level, c));
            long missing = goal - t.waterMb();
            want[i] = missing < HYSTERESIS_MB ? 0 : Math.min(missing, MAX_PER_STEP);
            sum += want[i];
        }
        if (sum <= 0) return new int[]{0, Status.IDLE.ordinal()};
        int fe = (int) Math.ceil(DeviceEnergy.pumpFe() * sum / 100.0);
        if (DeviceEnergy.required()) energy.drive(level, worldPosition, fe); // M32 part c: Create's rotation
        if (DeviceEnergy.required() && energy.getEnergyStored() < fe) return new int[]{0, Status.NO_POWER.ordinal()};
        WaterSources.Supply supply = org.overengineer.bettacraft.filter.FilterWater.supply(level, worldPosition, filter);
        long[] give = org.overengineer.bettacraft.filter.FilterWater.distribute(want, supply.endless() ? sum : supply.available(true));
        int added = 0;
        for (int i = 0; i < give.length; i++) {
            if (give[i] <= 0) continue;
            if (supply.endless()) {
                TankInteractions.topOff(level, tanks.get(i), give[i], null);
                added += (int) give[i];
            } else {
                for (WaterSources.Portion p : supply.take((int) give[i], true)) {
                    TankInteractions.topOff(level, tanks.get(i), p.mb(), p.values());
                    added += p.mb();
                }
            }
        }
        if (added > 0 && DeviceEnergy.required()) energy.use((int) Math.ceil(DeviceEnergy.pumpFe() * added / 100.0));
        Status st = added > 0 ? Status.TOPPED : supply.onlySalty() ? Status.SALTY : Status.NO_SOURCE;
        return new int[]{added, st.ordinal()};
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
        tag.putInt("filter_tanks", filterTanks);
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
        filterTanks = tag.getInt("filter_tanks");
        energy.load(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag t = new CompoundTag();
        t.putInt("target", target);
        t.putString("status", status.name());
        t.putLong("topped", toppedTotal);
        t.putInt("filter_tanks", filterTanks);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
