package org.overengineer.bettacraft.tank;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Care;
import static org.overengineer.bettacraft.tank.TankDamageGuard.*;

/** M52: the NeoForge event handlers of {@link TankDamageGuard} (moved here from the common class). */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
final class TankDamageGuardNeoForgeEvents {
    private TankDamageGuardNeoForgeEvents() {}

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Pre event) {
        LivingEntity e = event.getEntity();
        if (e instanceof Player || !(e.level() instanceof ServerLevel level) || Care.canDie()) return;
        if (!spared(event.getSource()) || TankInhabitants.resolveController(level, e) == null) return;
        float max = Math.max(0F, e.getHealth() - 1F);
        if (event.getNewDamage() > max) event.setNewDamage(max);
    }
}
