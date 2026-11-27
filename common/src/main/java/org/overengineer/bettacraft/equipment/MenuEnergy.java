package org.overengineer.bettacraft.equipment;

import javax.annotation.Nullable;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankPower;

/**
 * M66: the stored energy a filter menu (external filter, cabinet) shows. A filter keeps no energy of its own: what flows in
 * through it (FE, M67 also the crank) goes into the one store of the tank it is attached to, and every device of that tank
 * draws from there. The values
 * travel in four data slots (a menu data slot carries 16 bits: stored and capacity as a low and a high word each).
 */
public final class MenuEnergy {
    private final int[] client = new int[4];
    @Nullable
    private final BlockEntity host;
    private long cachedAt = Long.MIN_VALUE;
    private long cachedStored, cachedCap;

    public MenuEnergy(@Nullable BlockEntity host) {
        this.host = host;
    }

    /** The four data slots (the menu adds them). */
    public DataSlot[] slots() {
        DataSlot[] out = new DataSlot[4];
        for (int i = 0; i < 4; i++) {
            int k = i;
            out[i] = new DataSlot() {
                @Override
                public int get() {
                    if (host == null) return client[k];
                    read();
                    long v = k < 2 ? cachedStored : cachedCap;
                    return (int) ((k % 2 == 0 ? v : v >>> 16) & 0xFFFF);
                }

                @Override
                public void set(int v) {
                    client[k] = v & 0xFFFF;
                }
            };
        }
        return out;
    }

    /** Server: looks up the tank's store (at most once a tick). */
    private void read() {
        var level = host.getLevel();
        if (level == null) {
            cachedStored = cachedCap = 0;
            return;
        }
        if (cachedAt == level.getGameTime()) return;
        cachedAt = level.getGameTime();
        cachedStored = cachedCap = 0;
        if (host instanceof FilterHost f) {
            TankBlockEntity t = FilterHost.tankOf(level, f.tankBlockPos());
            if (t != null) {
                cachedStored = Math.max(0, Math.min(Integer.MAX_VALUE, t.getTank().powerBuffer()));
                cachedCap = Math.max(0, Math.min(Integer.MAX_VALUE, TankPower.capacity(t)));
            }
        }
    }

    /** Client: the energy in the tank's store in FE. */
    public long stored() {
        return (client[0] & 0xFFFFL) | ((client[1] & 0xFFFFL) << 16);
    }

    /** Client: the capacity of the store in FE; 0 while the filter is not attached to a tank. */
    public long capacity() {
        return (client[2] & 0xFFFFL) | ((client[3] & 0xFFFFL) << 16);
    }
}
