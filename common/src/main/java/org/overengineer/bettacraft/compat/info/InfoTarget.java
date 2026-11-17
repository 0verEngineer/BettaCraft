package org.overengineer.bettacraft.compat.info;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.entity.betta.BettaEntity;

/** Looking through the glass of a tank at a fish shows the fish, not the tank block (all info plugins). */
public final class InfoTarget {
    private InfoTarget() {}

    /** The betta on the view ray of {@code player} (a bit beyond the interaction range), or null. */
    public static EntityHitResult bettaBehindGlass(Level level, Player player) {
        if (level == null || player == null) return null;
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(player.entityInteractionRange() + 3.0));
        AABB area = new AABB(eye, end).inflate(1.0);
        return ProjectileUtil.getEntityHitResult(level, player, eye, end, area, e -> e instanceof BettaEntity && e.isAlive(), 0.0F);
    }
}
