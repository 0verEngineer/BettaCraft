package org.overengineer.bettacraft.plumbing;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankNitrate;

/**
 * M24: the water change controller with its drain pump. Next to a tank it changes {@link #share} % of the water every
 * {@link #intervalDays} days (menu): it drains the old water (down the drain) and fills the same amount of fresh water
 * from the reservoir, pipe or other mod's tank next to it, or endlessly from a water source block next to it. It only
 * drains what it can refill. Fresh tap water has the temperature of the place (a cool water change, M19).
 */
public class WaterChangerBlockEntity extends BlockEntity {
    public enum Status { WAITING, CHANGED, NO_TANK, NO_WATER, NO_POWER, OFF, DISABLED }

    /** Days between changes; 0 = not set yet: what the difficulty asks for ({@link #defaultInterval}). */
    private int intervalDays;
    private int share = 25;
    private boolean enabled = true;
    /** Game time of the last change (or of placing it: the first change comes one interval later). */
    private long last = -1;
    private Status status = Status.WAITING;
    private int lastMb;
    private final DeviceEnergy energy = new DeviceEnergy(40000, this::setChanged);

    public WaterChangerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_CHANGER.get(), pos, state);
    }

    public DeviceEnergy energy() { return energy; }
    public int intervalDays() { return intervalDays > 0 ? intervalDays : defaultInterval(); }
    public int share() { return share; }
    public boolean enabled() { return enabled; }
    public long last() { return last; }
    public Status status() { return status; }
    public int lastMb() { return lastMb; }

    /** Menu: interval 1..60 days, share 5..50 %, on/off. */
    public void set(int interval, int sharePct, boolean on) {
        intervalDays = Math.max(1, Math.min(60, interval));
        share = Math.max(5, Math.min(50, sharePct));
        enabled = on;
        changed();
    }

    /** The default interval: what the difficulty asks for (7 days when the nitrate is off). */
    public static int defaultInterval() {
        int d = TankNitrate.intervalDays(TankNitrate.difficulty());
        return d <= 0 ? 7 : d;
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, WaterChangerBlockEntity be) {
        if (be.last < 0) {
            be.last = level.getGameTime();
            be.changed();
        }
        if ((level.getGameTime() + pos.asLong()) % 20 != 0) return;
        be.check((ServerLevel) level, false);
    }

    /** Checks the interval and changes the water when it is due (or now with {@code force}); returns the mB changed. */
    public int check(ServerLevel level, boolean force) {
        Status st;
        int done = 0;
        TankBlockEntity c = WaterSources.tank(level, worldPosition, false);
        long now = level.getGameTime();
        if (!Features.enabled(Feature.AUTO_WATER_CHANGE)) st = Status.OFF;
        else if (!enabled && !force) st = Status.DISABLED;
        else if (c == null) st = Status.NO_TANK;
        else if (!force && last >= 0 && now - last < intervalDays() * 24000L) st = status == Status.CHANGED ? Status.CHANGED : Status.WAITING;
        else {
            int r = change(level, c);
            done = Math.max(0, r);
            st = r > 0 ? Status.CHANGED : r < 0 ? Status.NO_POWER : Status.NO_WATER;
            if (done > 0) last = now;
        }
        if (st != status || done > 0) {
            status = st;
            changed();
        }
        return done;
    }

    /** Changes the water now: mB changed, 0 = no water to change or to refill, -1 = not enough FE. */
    private int change(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        long amount = Math.round(t.capacityMb() * share / 100.0);
        // like the empty bucket: never below the minimum fill while animals live in the tank
        long min = t.inhabitants().isEmpty() ? 0 : Math.round(ServerConfig.getOr(ServerConfig.TANK_MIN_FILL_TO_INSERT, 0.5) * t.capacityMb());
        amount = Math.min(amount, Math.max(0, t.waterMb() - min));
        if (amount <= 0) return 0;
        boolean endless = WaterSources.endless(level, worldPosition);
        if (!endless) amount = Math.min(amount, WaterSources.available(level, worldPosition, false));
        if (amount <= 0) return 0;
        int fe = (int) Math.ceil(DeviceEnergy.pumpFe() * amount / 100.0);
        if (DeviceEnergy.required()) energy.drive(level, worldPosition, fe); // M32 part c: Create's rotation
        if (DeviceEnergy.required() && !energy.use(fe)) return -1;
        TankInteractions.changeWater(level, c, -amount);
        long filled = 0;
        if (endless) {
            TankInteractions.changeWater(level, c, amount);
            filled = amount;
        } else {
            for (WaterSources.Portion p : WaterSources.take(level, worldPosition, (int) amount, false)) {
                TankInteractions.addWater(level, c, p.mb(), p.values());
                filled += p.mb();
            }
        }
        lastMb = (int) filled;
        TankCare.log(level, c, "log.bettacraft.auto_water_change", String.valueOf(Math.round(filled * 100.0 / Math.max(1, t.capacityMb()))),
                String.valueOf(filled));
        return (int) filled;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private void write(CompoundTag tag) {
        tag.putInt("interval", intervalDays);
        tag.putInt("share", share);
        tag.putBoolean("enabled", enabled);
        tag.putLong("last", last);
        tag.putString("status", status.name());
        tag.putInt("last_mb", lastMb);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        write(tag);
        energy.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        intervalDays = tag.contains("interval") ? Math.max(0, Math.min(60, tag.getInt("interval"))) : 0;
        share = tag.contains("share") ? Math.max(5, Math.min(50, tag.getInt("share"))) : 25;
        enabled = !tag.contains("enabled") || tag.getBoolean("enabled");
        last = tag.contains("last") ? tag.getLong("last") : -1;
        try {
            status = Status.valueOf(tag.getString("status"));
        } catch (IllegalArgumentException e) {
            status = Status.WAITING;
        }
        lastMb = tag.getInt("last_mb");
        energy.load(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag t = new CompoundTag();
        write(t);
        t.putInt("interval", intervalDays()); // the client sees the effective interval
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
