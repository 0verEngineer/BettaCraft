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
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.api.event.AnimalBornEvent;
import org.overengineer.bettacraft.api.event.AnimalDiedEvent;
import org.overengineer.bettacraft.api.event.TankAlarmEvent;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import static org.overengineer.bettacraft.api.ApiEvents.*;

/** M52: the NeoForge event handlers of {@link ApiEvents} (moved here from the common class). */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
final class ApiEventsNeoForgeEvents {
    private ApiEventsNeoForgeEvents() {}

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
