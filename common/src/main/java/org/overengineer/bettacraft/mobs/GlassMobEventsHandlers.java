package org.overengineer.bettacraft.mobs;

import net.minecraft.world.entity.Mob;
import org.overengineer.bettacraft.platform.event.SubscribeEvent;
import org.overengineer.bettacraft.platform.event.LivingChangeTargetEvent;
import org.overengineer.bettacraft.platform.event.LivingDropsEvent;
import org.overengineer.bettacraft.platform.event.LivingExperienceDropEvent;
import org.overengineer.bettacraft.platform.event.MobDespawnEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.tank.TankInhabitants;
import static org.overengineer.bettacraft.mobs.GlassMobEvents.*;

/** M52: the game event handlers of {@link GlassMobEvents} (M52: on the common game events, fired by each loader). */
public final class GlassMobEventsHandlers {
    private GlassMobEventsHandlers() {}

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        if (GlassMobs.isGlassMob(e.getEntity())) e.setCanceled(true);
    }

    /** M33 part d: the mark goes with a conversion (lightning, powder snow, other mods), see {@link GlassMobs#converted}. */
    @SubscribeEvent
    public static void onConverted(org.overengineer.bettacraft.platform.event.LivingConversionEvent.Post e) {
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
        if (GlassMobs.isGlassMob(e.getEntity()) && TankInhabitants.hasTankRef(e.getEntity())) e.setResult(MobDespawnEvent.DespawnResult.DENY);
    }

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent e) {
        if (e.getNewAboutToBeSetTarget() == null) return;
        if (e.getEntity() instanceof Mob m && GlassMobs.held(m) || GlassMobs.held(e.getNewAboutToBeSetTarget())) e.setCanceled(true);
    }
}
