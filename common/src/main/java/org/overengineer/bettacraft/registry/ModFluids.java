package org.overengineer.bettacraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.platform.Platform;
import org.overengineer.bettacraft.platform.fluid.FluidSpec;
import org.overengineer.bettacraft.platform.registry.DeferredHolder;
import org.overengineer.bettacraft.platform.registry.DeferredRegister;

/**
 * M25: reverse osmosis water and sea water as fluids of their own, so that other mods' tanks and storage (AE2, Refined
 * Storage, Mekanism ...) keep them apart from water. They never stand in the world (no fluid block): a bucket poured
 * out gives plain water, and in a tank they become water with their values ({@link org.overengineer.bettacraft.tank.WaterValues#from}).
 * M52: the fluid objects come from the loader ({@link Platform#fluid}; NeoForge adds a fluid type of the same id to each).
 */
public final class ModFluids {
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, BettaCraft.MOD_ID);

    public static final DeferredHolder<Fluid, FlowingFluid> RO_WATER = FLUIDS.register("ro_water", () -> Platform.get().fluid(ro(), true));
    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_RO_WATER = FLUIDS.register("flowing_ro_water", () -> Platform.get().fluid(ro(), false));
    public static final DeferredHolder<Fluid, FlowingFluid> SEA_WATER = FLUIDS.register("sea_water", () -> Platform.get().fluid(sea(), true));
    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_SEA_WATER = FLUIDS.register("flowing_sea_water", () -> Platform.get().fluid(sea(), false));

    /** M26: live food (daphnia, artemia ...) in water, from the live food pump; a tank feeds it to its animals. */
    public static final DeferredHolder<Fluid, FlowingFluid> LIVE_FOOD = FLUIDS.register("live_food", () -> Platform.get().fluid(live(), true));
    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_LIVE_FOOD = FLUIDS.register("flowing_live_food", () -> Platform.get().fluid(live(), false));

    /** M37: liquid fertilizer from the fertilizer maker (in pipes and tanks; a tank takes it into its water up to 100 %). */
    public static final DeferredHolder<Fluid, FlowingFluid> FERTILIZER = FLUIDS.register("fertilizer", () -> Platform.get().fluid(fert(), true));
    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_FERTILIZER = FLUIDS.register("flowing_fertilizer", () -> Platform.get().fluid(fert(), false));

    public static final FluidSpec RO_WATER_SPEC = new FluidSpec("ro_water", 1000, RO_WATER, FLOWING_RO_WATER, () -> ModItems.RO_WATER_BUCKET.get());
    public static final FluidSpec SEA_WATER_SPEC = new FluidSpec("sea_water", 1025, SEA_WATER, FLOWING_SEA_WATER, () -> ModItems.SEA_WATER_BUCKET.get());
    public static final FluidSpec LIVE_FOOD_SPEC = new FluidSpec("live_food", 1000, LIVE_FOOD, FLOWING_LIVE_FOOD, null);
    public static final FluidSpec FERTILIZER_SPEC = new FluidSpec("fertilizer", 1000, FERTILIZER, FLOWING_FERTILIZER, null);

    private static FluidSpec ro() {
        return RO_WATER_SPEC;
    }

    private static FluidSpec sea() {
        return SEA_WATER_SPEC;
    }

    private static FluidSpec live() {
        return LIVE_FOOD_SPEC;
    }

    private static FluidSpec fert() {
        return FERTILIZER_SPEC;
    }

    /** Whether this fluid is water for the tanks and pipes: plain water, reverse osmosis water or sea water. */
    public static boolean isWater(Fluid f) {
        return f.isSame(net.minecraft.world.level.material.Fluids.WATER) || f.isSame(RO_WATER.get()) || f.isSame(SEA_WATER.get());
    }

    public static void register() {
        FLUIDS.register();
    }

    private ModFluids() {}
}
