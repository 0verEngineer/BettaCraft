package org.overengineer.bettacraft.api;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import org.overengineer.bettacraft.platform.event.EventPriority;
import org.overengineer.bettacraft.platform.event.SubscribeEvent;
import org.overengineer.bettacraft.platform.event.LivingDeathEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import static org.overengineer.bettacraft.api.ApiEvents.*;

/** M52: the game event handlers of {@link ApiEvents} (M52: on the common game events, fired by each loader). */
public final class ApiEventsHandlers {
    private ApiEventsHandlers() {}

    /** Every tank inhabitant that dies by damage (fights, predators, players, drowning, ...). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !on()) return;
        Entity e = event.getEntity();
        if (!(e.level() instanceof ServerLevel level) || !TankInhabitants.hasTankRef(e)) return;
        TankBlockEntity c = TankInhabitants.resolveController(level, e);
        if (c == null) return;
        died(level, c, e, cause(e, event.getSource()), event.getSource().getEntity());
    }
}
