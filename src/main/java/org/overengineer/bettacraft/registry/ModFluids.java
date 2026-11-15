package org.overengineer.bettacraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.overengineer.bettacraft.BettaCraft;

/**
 * M25: reverse osmosis water and sea water as fluids of their own, so that other mods' tanks and storage (AE2, Refined
 * Storage, Mekanism ...) keep them apart from water. They never stand in the world (no fluid block): a bucket poured
 * out gives plain water, and in a tank they become water with their values ({@link org.overengineer.bettacraft.tank.WaterValues#from}).
 */
public final class ModFluids {
    public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, BettaCraft.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, BettaCraft.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> RO_WATER_TYPE = TYPES.register("ro_water", () -> new FluidType(props()));
    public static final DeferredHolder<FluidType, FluidType> SEA_WATER_TYPE = TYPES.register("sea_water", () -> new FluidType(props().density(1025)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> RO_WATER = FLUIDS.register("ro_water", () -> new BaseFlowingFluid.Source(ro()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_RO_WATER = FLUIDS.register("flowing_ro_water", () -> new BaseFlowingFluid.Flowing(ro()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SEA_WATER = FLUIDS.register("sea_water", () -> new BaseFlowingFluid.Source(sea()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_SEA_WATER = FLUIDS.register("flowing_sea_water", () -> new BaseFlowingFluid.Flowing(sea()));

    /** M26: live food (daphnia, artemia ...) in water, from the live food pump; a tank feeds it to its animals. */
    public static final DeferredHolder<FluidType, FluidType> LIVE_FOOD_TYPE = TYPES.register("live_food", () -> new FluidType(props()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LIVE_FOOD = FLUIDS.register("live_food", () -> new BaseFlowingFluid.Source(live()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_LIVE_FOOD = FLUIDS.register("flowing_live_food", () -> new BaseFlowingFluid.Flowing(live()));

    /** M37: liquid fertilizer from the fertilizer maker (in pipes and tanks; a tank takes it into its water up to 100 %). */
    public static final DeferredHolder<FluidType, FluidType> FERTILIZER_TYPE = TYPES.register("fertilizer", () -> new FluidType(props()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> FERTILIZER = FLUIDS.register("fertilizer", () -> new BaseFlowingFluid.Source(fert()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_FERTILIZER = FLUIDS.register("flowing_fertilizer", () -> new BaseFlowingFluid.Flowing(fert()));

    private static BaseFlowingFluid.Properties fert() {
        return new BaseFlowingFluid.Properties(FERTILIZER_TYPE, FERTILIZER, FLOWING_FERTILIZER);
    }

    private static BaseFlowingFluid.Properties live() {
        return new BaseFlowingFluid.Properties(LIVE_FOOD_TYPE, LIVE_FOOD, FLOWING_LIVE_FOOD);
    }

    private static FluidType.Properties props() {
        return FluidType.Properties.create().canSwim(true).canDrown(true).canExtinguish(true).canHydrate(true).supportsBoating(true)
                .canConvertToSource(false).sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL).sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY);
    }

    private static BaseFlowingFluid.Properties ro() {
        return new BaseFlowingFluid.Properties(RO_WATER_TYPE, RO_WATER, FLOWING_RO_WATER).bucket(ModItems.RO_WATER_BUCKET);
    }

    private static BaseFlowingFluid.Properties sea() {
        return new BaseFlowingFluid.Properties(SEA_WATER_TYPE, SEA_WATER, FLOWING_SEA_WATER).bucket(ModItems.SEA_WATER_BUCKET);
    }

    /** Whether this fluid is water for the tanks and pipes: plain water, reverse osmosis water or sea water. */
    public static boolean isWater(Fluid f) {
        return f.isSame(net.minecraft.world.level.material.Fluids.WATER) || f.isSame(RO_WATER.get()) || f.isSame(SEA_WATER.get());
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        FLUIDS.register(bus);
    }

    private ModFluids() {}
}
