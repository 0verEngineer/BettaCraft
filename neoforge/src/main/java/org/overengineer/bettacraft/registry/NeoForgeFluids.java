package org.overengineer.bettacraft.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.platform.fluid.FluidSpec;

/** M52: NeoForge's fluid types of our fluids ({@link ModFluids}), same ids as the fluids, and the fluid objects. */
public final class NeoForgeFluids {
    public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, BettaCraft.MOD_ID);
    private static final Map<String, Supplier<FluidType>> BY_NAME = new HashMap<>();

    public static final Supplier<FluidType> RO_WATER_TYPE = type(ModFluids.RO_WATER_SPEC);
    public static final Supplier<FluidType> SEA_WATER_TYPE = type(ModFluids.SEA_WATER_SPEC);
    public static final Supplier<FluidType> LIVE_FOOD_TYPE = type(ModFluids.LIVE_FOOD_SPEC);
    public static final Supplier<FluidType> FERTILIZER_TYPE = type(ModFluids.FERTILIZER_SPEC);

    private NeoForgeFluids() {}

    private static Supplier<FluidType> type(FluidSpec spec) {
        Supplier<FluidType> t = TYPES.register(spec.name(), () -> new FluidType(props(spec)));
        BY_NAME.put(spec.name(), t);
        return t;
    }

    private static FluidType.Properties props(FluidSpec spec) {
        FluidType.Properties p = FluidType.Properties.create().canSwim(true).canDrown(true).canExtinguish(true).canHydrate(true).supportsBoating(true)
                .canConvertToSource(false).sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL).sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY);
        return spec.density() != 1000 ? p.density(spec.density()) : p;
    }

    /** The fluid object of a spec (the platform's {@code fluid}). */
    public static FlowingFluid fluid(FluidSpec spec, boolean source) {
        BaseFlowingFluid.Properties p = new BaseFlowingFluid.Properties(BY_NAME.get(spec.name()), spec.source(), spec.flowing());
        if (spec.bucket() != null) p.bucket(spec.bucket());
        return source ? new BaseFlowingFluid.Source(p) : new BaseFlowingFluid.Flowing(p);
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
