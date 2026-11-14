package org.overengineer.bettacraft.osmosis;

import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;

/**
 * M25: the fluid handler of the sea water and reverse osmosis water buckets: the bucket pours plain water into the world
 * (its item content is water), but other mods' tanks and pipes get the fluid of its own ({@code bettacraft:sea_water},
 * {@code bettacraft:ro_water}); emptied it is an empty bucket.
 */
public final class WaterBucketWrapper extends FluidBucketWrapper {
    private final Supplier<? extends Fluid> fluid;

    public WaterBucketWrapper(ItemStack container, Supplier<? extends Fluid> fluid) {
        super(container);
        this.fluid = fluid;
    }

    @Override
    public FluidStack getFluid() {
        if (container.isEmpty() || container.is(net.minecraft.world.item.Items.BUCKET)) return FluidStack.EMPTY;
        return new FluidStack(fluid.get(), FluidType.BUCKET_VOLUME);
    }
}
