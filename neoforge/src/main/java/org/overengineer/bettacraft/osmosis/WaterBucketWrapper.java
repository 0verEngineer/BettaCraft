package org.overengineer.bettacraft.osmosis;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * M25: the fluid handler of the sea water and reverse osmosis water buckets: the bucket pours plain water into the world
 * (its item content is water), but other mods' tanks and pipes get the fluid of its own ({@code bettacraft:sea_water},
 * {@code bettacraft:ro_water}); emptied it is an empty bucket. M52b: on NeoForge 21.9's transfer API (item access).
 */
public final class WaterBucketWrapper extends ItemAccessResourceHandler<FluidResource> {
    private final Supplier<? extends Fluid> fluid;

    public WaterBucketWrapper(ItemAccess access, Supplier<? extends Fluid> fluid) {
        super(access, 1);
        this.fluid = fluid;
    }

    private boolean full(ItemResource r) {
        return !r.isEmpty() && !r.is(Items.BUCKET);
    }

    @Override
    protected FluidResource getResourceFrom(ItemResource accessResource, int index) {
        return full(accessResource) ? FluidResource.of(fluid.get()) : FluidResource.EMPTY;
    }

    @Override
    protected int getAmountFrom(ItemResource accessResource, int index) {
        return full(accessResource) ? FluidType.BUCKET_VOLUME : 0;
    }

    @Override
    protected ItemResource update(ItemResource accessResource, int index, FluidResource newResource, int newAmount) {
        if (newAmount == 0) return ItemResource.of(Items.BUCKET);
        // only emptied (a bucket of it is never filled from elsewhere)
        return newAmount == FluidType.BUCKET_VOLUME && newResource.getFluid() == fluid.get() ? accessResource : ItemResource.EMPTY;
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
        Objects.checkIndex(index, size());
        return FluidType.BUCKET_VOLUME;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return resource.getFluid() == fluid.get();
    }
}
