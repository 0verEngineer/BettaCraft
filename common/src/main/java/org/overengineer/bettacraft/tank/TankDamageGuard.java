package org.overengineer.bettacraft.tank;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Care;

/**
 * M33 part c: with {@code care.mode} NO_DEATH or DECOR no animal in a tank or terrarium dies of damage that is not a
 * player's: starving and drowning frogs and reptiles, a dolphin without fish, an axolotl in warm water, fights, fin nippers,
 * pinching crayfish, burning undead in glass. The damage is capped so that one half heart stays; a player and /kill (damage
 * that bypasses invulnerability) still kill. The deaths without damage (illness, salt, being eaten, filter intake) ask
 * {@link Care} where they happen.
 */
public final class TankDamageGuard {
    private TankDamageGuard() {}


    /** Whether the care mode spares an animal from this damage (not from a player, not /kill or the void). */
    static boolean spared(DamageSource s) {
        if (s.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return !(s.getEntity() instanceof Player) && !(s.getDirectEntity() instanceof Player);
    }
}
