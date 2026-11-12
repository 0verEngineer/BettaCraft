package org.overengineer.bettacraft.entity.betta;

import javax.annotation.Nullable;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.phys.Vec3;

/**
 * Random swimming. Inside a tank the target is picked inside the tank water volume (never outside the glass) and
 * hand-fed food attracts the fish; outside a tank this is the vanilla fish behaviour.
 */
final class BettaSwimGoal extends RandomSwimmingGoal {
    private final BettaEntity fish;

    BettaSwimGoal(BettaEntity fish) {
        super(fish, 1.0, 40);
        this.fish = fish;
    }

    @Override
    public boolean canUse() {
        if (fish.foodTarget != null && fish.level().getGameTime() < fish.foodUntil) {
            trigger();
        }
        return super.canUse();
    }

    @Nullable
    @Override
    protected Vec3 getPosition() {
        var box = fish.tankWater;
        if (box == null) return super.getPosition();
        var r = fish.getRandom();
        if (fish.foodTarget != null && fish.level().getGameTime() < fish.foodUntil) {
            Vec3 f = fish.foodTarget;
            return clamp(box, new Vec3(f.x + (r.nextDouble() - 0.5) * 0.6, f.y - r.nextDouble() * 0.8, f.z + (r.nextDouble() - 0.5) * 0.6));
        }
        // stay within ~5 blocks so paths stay short in large tanks; avoid interior decoration (PLAN 4.2)
        Vec3 p = fish.position();
        Vec3 best = null;
        for (int attempt = 0; attempt < 8; attempt++) {
            Vec3 c = clamp(box, new Vec3(p.x + (r.nextDouble() - 0.5) * 10, p.y + (r.nextDouble() - 0.5) * 4, p.z + (r.nextDouble() - 0.5) * 10));
            if (best == null) best = c;
            if (fish.pathFreeOfDecor(p, c)) return c;
        }
        return fish.pathFreeOfDecor(p, best) ? best : null;
    }

    /** Inside the actual water of the tank (M10: never in the notch of a free shape). */
    private static Vec3 clamp(org.overengineer.bettacraft.tank.TankWaterVolume water, Vec3 v) {
        return water.clamp(v, 0.25 + 1.0 / 16, 0.1 + 1.0 / 16, 0.35);
    }
}
