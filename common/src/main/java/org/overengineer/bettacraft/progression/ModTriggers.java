package org.overengineer.bettacraft.progression;

import java.util.function.Supplier;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import org.overengineer.bettacraft.platform.registry.DeferredRegister;
import org.overengineer.bettacraft.BettaCraft;

/** Advancement criterion triggers (M6). */
public final class ModTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, BettaCraft.MOD_ID);

    public static final Supplier<BettaEventTrigger> EVENT = TRIGGERS.register("event", BettaEventTrigger::new);

    public static void register() {
        TRIGGERS.register();
    }

    private ModTriggers() {}
}
