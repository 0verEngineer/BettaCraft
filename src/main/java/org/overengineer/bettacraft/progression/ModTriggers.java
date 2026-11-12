package org.overengineer.bettacraft.progression;

import java.util.function.Supplier;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.overengineer.bettacraft.BettaCraft;

/** Advancement criterion triggers (M6). */
public final class ModTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, BettaCraft.MOD_ID);

    public static final Supplier<BettaEventTrigger> EVENT = TRIGGERS.register("event", BettaEventTrigger::new);

    public static void register(IEventBus bus) {
        TRIGGERS.register(bus);
    }

    private ModTriggers() {}
}
