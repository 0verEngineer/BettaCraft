package org.overengineer.bettacraft.equipment;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.tank.TankBlockEntity;

/**
 * M49 part a: the sump in a cabinet as a filter of the tank above ({@link Sump}): its return pump moves the water through the
 * overflow (with a comb, so it takes in no fry), its two heater chambers heat or cool like the heaters of an external filter.
 * Equal to every other host of the same cabinet (the lists of the tank compare them).
 */
public final class SumpHost implements FilterHost {
    private final CabinetBlockEntity cabinet;

    public SumpHost(CabinetBlockEntity cabinet) {
        this.cabinet = cabinet;
    }

    public CabinetBlockEntity cabinet() {
        return cabinet;
    }

    public ItemStack chamber(int i) {
        return Sump.read(cabinet.sumpItem()).get(i);
    }

    /** The refugium has Chaetomorpha and a light, and the return pump runs. */
    public boolean refugiumLit(TankBlockEntity c) {
        NonNullList<ItemStack> ch = Sump.read(cabinet.sumpItem());
        return !ch.get(Sump.ALGAE).isEmpty() && !ch.get(Sump.LIGHT).isEmpty() && org.overengineer.bettacraft.tank.TankPower.filterRuns(c, this);
    }

    @Override
    public boolean hasFilter() {
        return Sump.enabled() && cabinet.hasSump();
    }

    @Override
    public ItemStack heater(int slot) {
        return slot < 0 || slot >= FilterHeaters.SLOTS ? ItemStack.EMPTY : chamber(Sump.HEATER0 + slot);
    }

    @Override
    public boolean insertHeater(ItemStack heater) {
        ItemStack sump = cabinet.sumpItem();
        if (sump.isEmpty()) return false;
        NonNullList<ItemStack> ch = Sump.read(sump);
        for (int i = 0; i < FilterHeaters.SLOTS; i++) {
            if (!ch.get(Sump.HEATER0 + i).isEmpty()) continue;
            ch.set(Sump.HEATER0 + i, heater.copyWithCount(1));
            ItemStack ns = sump.copy();
            Sump.write(ns, ch);
            cabinet.setSumpItem(ns);
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public BlockPos tankBlockPos() {
        return cabinet.tankBlockPos();
    }

    /** The overflow comb keeps the fry in the tank. */
    @Override
    public boolean intakeSponge() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SumpHost h && h.cabinet == cabinet;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(cabinet) * 31 + 7;
    }
}
