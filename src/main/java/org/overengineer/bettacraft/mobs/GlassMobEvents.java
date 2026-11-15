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

/**
 * M31 part d (question C4): a mob that lived in glass drops nothing and gives no experience, whoever or whatever kills it:
 * {@link LivingDropsEvent} holds every death drop (loot table, equipment, the player kill bonus) of every cause, and
 * {@link LivingExperienceDropEvent} the experience. Also: a mob in its tank does not despawn (not in peaceful either), it
 * takes no target and nothing outside targets it (an iron golem next to the tank).
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class GlassMobEvents {
    private GlassMobEvents() {}

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
