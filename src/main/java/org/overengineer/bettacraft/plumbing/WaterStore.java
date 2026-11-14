package org.overengineer.bettacraft.plumbing;

import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.overengineer.bettacraft.tank.WaterValues;

/**
 * M24: water in a reservoir: any water goes in and the values mix by volume (unlike a plain fluid tank, which only
 * takes water of exactly the same values). Water without values (a bucket, a water source) is tap water.
 */
public class WaterStore implements IFluidHandler {
    private final int capacity;
    private final Runnable changed;
    private int amount;
    @Nullable
    private WaterValues values;

    public WaterStore(int capacity, Runnable changed) {
        this.capacity = capacity;
        this.changed = changed;
    }

    public int amount() {
        return amount;
    }

    public int capacity() {
        return capacity;
    }

    @Nullable
    public WaterValues values() {
        return values;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return amount <= 0 ? FluidStack.EMPTY : WaterValues.stack(amount, values);
    }

    @Override
    public int getTankCapacity(int tank) {
        return capacity;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return stack.getFluid().isSame(Fluids.WATER);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(0, resource)) return 0;
        int n = Math.min(resource.getAmount(), capacity - amount);
        if (n <= 0) return 0;
        if (action.execute()) {
            WaterValues in = WaterValues.from(resource);
            values = mix(values, amount, in, n);
            amount += n;
            changed.run();
        }
        return n;
    }

    /** Pure: the values of {@code a} mB of {@code va} and {@code b} mB of {@code vb} (null = tap water). */
    @Nullable
    public static WaterValues mix(@Nullable WaterValues va, int a, @Nullable WaterValues vb, int b) {
        if (a <= 0) return vb;
        if (b <= 0) return va;
        if (va == null && vb == null) return null;
        WaterValues x = va != null ? va : WaterValues.tap(vb.temp()), y = vb != null ? vb : WaterValues.tap(va.temp());
        double t = a + b;
        return new WaterValues((float) ((x.temp() * a + y.temp() * b) / t), (float) ((x.salinity() * a + y.salinity() * b) / t),
                (float) ((x.calcium() * a + y.calcium() * b) / t), (float) ((x.quality() * a + y.quality() * b) / t),
                (float) ((x.nitrate() * a + y.nitrate() * b) / t), (float) ((x.tds() * a + y.tds() * b) / t),
                org.overengineer.bettacraft.chem.WaterChemistry.mixPh(x.ph(), a, y.ph(), b));
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(0, resource)) return FluidStack.EMPTY;
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        int n = Math.min(maxDrain, amount);
        if (n <= 0) return FluidStack.EMPTY;
        FluidStack out = WaterValues.stack(n, values);
        if (action.execute()) {
            amount -= n;
            if (amount <= 0) values = null;
            changed.run();
        }
        return out;
    }

    public void save(CompoundTag t, HolderLookup.Provider registries) {
        t.putInt("water", amount);
        if (values != null) WaterValues.CODEC.encodeStart(NbtOps.INSTANCE, values).result().ifPresent(v -> t.put("values", v));
    }

    public void load(CompoundTag t, HolderLookup.Provider registries) {
        amount = Math.max(0, Math.min(capacity, t.getInt("water")));
        values = t.contains("values") ? WaterValues.CODEC.parse(NbtOps.INSTANCE, t.get("values")).result().orElse(null) : null;
    }
}
