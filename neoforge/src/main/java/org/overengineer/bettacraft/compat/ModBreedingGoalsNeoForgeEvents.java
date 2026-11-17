package org.overengineer.bettacraft.compat;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.tank.TankInhabitants;
import static org.overengineer.bettacraft.compat.ModBreedingGoals.*;

/** M52: the NeoForge event handlers of {@link ModBreedingGoals} (moved here from the common class). */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
final class ModBreedingGoalsNeoForgeEvents {
    private ModBreedingGoalsNeoForgeEvents() {}

    /** After the mods added their goals (they do it on joining the level, at normal priority). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob m)) return;
        if (ours(m) || Features.enabled(Feature.SUPPRESS_FOREIGN_BREEDING) && TankInhabitants.hasTankRef(m)) strip(m);
    }
}
