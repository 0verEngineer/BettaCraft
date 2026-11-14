package org.overengineer.bettacraft.registry;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.overengineer.bettacraft.BettaCraft;

/**
 * Sound events (M4). M23: the devices have sounds of their own (procedural, {@code tools/assetgen/sounds.py}); every one
 * also has a twin {@code ..._vanilla} mapped onto Minecraft's sound files, chosen with {@code sounds.useVanilla}
 * ({@link #pick}). Every event has a translated subtitle.
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, BettaCraft.MOD_ID);

    /** Quiet bubbling of a running filter (client config {@code sound.filterSound}). */
    public static final Supplier<SoundEvent> FILTER_RUNNING = register("block.filter.running");
    /** Water poured into a tank. */
    public static final Supplier<SoundEvent> TANK_FILL = register("block.tank.fill");
    /** Water scooped out of a tank. */
    public static final Supplier<SoundEvent> TANK_DRAIN = register("block.tank.drain");

    /** M23: the old filter sound (Minecraft's bubble columns and water). */
    public static final Supplier<SoundEvent> FILTER_RUNNING_VANILLA = register("block.filter.running_vanilla");
    public static final Supplier<SoundEvent> AIR_PUMP_HUM = register("block.air_pump.hum");
    public static final Supplier<SoundEvent> AIR_PUMP_HUM_VANILLA = register("block.air_pump.hum_vanilla");
    public static final Supplier<SoundEvent> AIR_STONE_BUBBLES = register("block.air_stone.bubbles");
    public static final Supplier<SoundEvent> AIR_STONE_BUBBLES_VANILLA = register("block.air_stone.bubbles_vanilla");
    public static final Supplier<SoundEvent> SPONGE_FILTER_GURGLE = register("block.sponge_filter.gurgle");
    public static final Supplier<SoundEvent> SPONGE_FILTER_GURGLE_VANILLA = register("block.sponge_filter.gurgle_vanilla");
    public static final Supplier<SoundEvent> FLOW_PUMP_WHIR = register("block.flow_pump.whir");
    public static final Supplier<SoundEvent> FLOW_PUMP_WHIR_VANILLA = register("block.flow_pump.whir_vanilla");
    public static final Supplier<SoundEvent> FEEDER_CLICK = register("block.feeder.click");
    public static final Supplier<SoundEvent> FEEDER_CLICK_VANILLA = register("block.feeder.click_vanilla");
    /** M26 part b: the ratchet of the food maker's spring (the feeder's ratchet ticks, lower). */
    public static final Supplier<SoundEvent> FOOD_MAKER_WIND = register("block.food_maker.wind");
    public static final Supplier<SoundEvent> FOOD_MAKER_WIND_VANILLA = register("block.food_maker.wind_vanilla");

    /** M23: the own sound or its Minecraft twin, by {@code sounds.useVanilla} (of the side that plays it). */
    public static SoundEvent pick(Supplier<SoundEvent> own, Supplier<SoundEvent> vanilla) {
        return org.overengineer.bettacraft.config.CommonConfig.vanillaSounds() ? vanilla.get() : own.get();
    }

    private static Supplier<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(BettaCraft.id(name)));
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }

    private ModSounds() {}
}
