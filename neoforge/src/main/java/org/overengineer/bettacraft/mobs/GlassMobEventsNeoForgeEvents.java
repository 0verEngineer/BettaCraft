package org.overengineer.bettacraft.mobs;

import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.MobDespawnEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.tank.TankInhabitants;
import static org.overengineer.bettacraft.mobs.GlassMobEvents.*;

/** M52: the NeoForge event handlers of {@link GlassMobEvents} (moved here from the common class). */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
final class GlassMobEventsNeoForgeEvents {
    private GlassMobEventsNeoForgeEvents() {}

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        if (GlassMobs.isGlassMob(e.getEntity())) e.setCanceled(true);
    }

    /** M33 part d: the mark goes with a conversion (lightning, powder snow, other mods), see {@link GlassMobs#converted}. */
    @SubscribeEvent
    public static void onConverted(net.neoforged.neoforge.event.entity.living.LivingConversionEvent.Post e) {
        GlassMobs.converted(e.getEntity(), e.getOutcome());
    }

    @SubscribeEvent
    public static void onExperience(LivingExperienceDropEvent e) {
        if (GlassMobs.isGlassMob(e.getEntity())) {
            e.setDroppedExperience(0);
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDespawn(MobDespawnEvent e) {
        if (GlassMobs.isGlassMob(e.getEntity()) && TankInhabitants.hasTankRef(e.getEntity())) e.setResult(MobDespawnEvent.Result.DENY);
    }

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent e) {
        if (e.getNewAboutToBeSetTarget() == null) return;
        if (e.getEntity() instanceof Mob m && GlassMobs.held(m) || GlassMobs.held(e.getNewAboutToBeSetTarget())) e.setCanceled(true);
    }
}
