package org.overengineer.bettacraft.registry;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.overengineer.bettacraft.BettaCraft;

/**
 * Sound events (M4). The sounds.json maps them onto vanilla sound files (water, bubbles), so no audio files are
 * shipped; every event has a translated subtitle.
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, BettaCraft.MOD_ID);

    /** Quiet bubbling of a running filter (client config {@code sound.filterSound}). */
    public static final Supplier<SoundEvent> FILTER_RUNNING = register("block.filter.running");
    /** Water poured into a tank. */
    public static final Supplier<SoundEvent> TANK_FILL = register("block.tank.fill");
    /** Water scooped out of a tank. */
    public static final Supplier<SoundEvent> TANK_DRAIN = register("block.tank.drain");

    private static Supplier<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(BettaCraft.id(name)));
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }

    private ModSounds() {}
}
