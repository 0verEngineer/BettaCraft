package org.overengineer.bettacraft.entity.betta;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Short-lived behaviours the tank simulation switches on (fights, nest defence, fleeing, staying at the nest). The
 * simulation refreshes them every second with an expiry time; they stop by themselves when not refreshed.
 */
final class BettaBehaviourGoals {
    private BettaBehaviourGoals() {}

    static Vec3 clampToTank(BettaEntity fish, Vec3 v) {
        AABB box = fish.tankWater;
        if (box == null) return v;
        double top = Math.max(box.minY + 0.1, box.maxY - 0.3);
        return new Vec3(Math.max(box.minX + 0.2, Math.min(box.maxX - 0.2, v.x)),
                Math.max(box.minY + 0.1, Math.min(top, v.y)),
                Math.max(box.minZ + 0.2, Math.min(box.maxZ - 0.2, v.z)));
    }

    /** Swim at a rival / the female; a quick dart when close. */
    static final class Pursue extends Goal {
        private final BettaEntity fish;

        Pursue(BettaEntity fish) {
            this.fish = fish;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Nullable
        private Entity target() {
            Entity t = fish.pursueTarget;
            if (t == null || !t.isAlive() || fish.level().getGameTime() >= fish.pursueUntil || t.level() != fish.level()) return null;
            return t;
        }

        @Override
        public boolean canUse() {
            return target() != null && fish.isInWater();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            Entity t = target();
            if (t == null) return;
            fish.getLookControl().setLookAt(t, 30F, 30F);
            if (fish.tickCount % 10 == 0 || fish.getNavigation().isDone()) {
                fish.getNavigation().moveTo(t.getX(), t.getY(), t.getZ(), 1.5);
            }
            if (fish.distanceToSqr(t) < 0.25 && fish.getRandom().nextInt(8) == 0) {
                Vec3 d = t.position().subtract(fish.position()).normalize().scale(0.08);
                t.push(d.x, d.y * 0.3, d.z);
            }
        }

        @Override
        public void stop() {
            fish.getNavigation().stop();
        }
    }

    /** Keep away from a pursuer (inside the tank). */
    static final class Flee extends Goal {
        private final BettaEntity fish;

        Flee(BettaEntity fish) {
            this.fish = fish;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Nullable
        private Entity from() {
            Entity f = fish.fleeFrom;
            if (f == null || !f.isAlive() || fish.level().getGameTime() >= fish.fleeUntil) return null;
            return f;
        }

        @Override
        public boolean canUse() {
            Entity f = from();
            return f != null && fish.isInWater() && fish.distanceToSqr(f) < 4.0;
        }

        @Override
        public boolean canContinueToUse() {
            return from() != null && fish.isInWater() && !fish.getNavigation().isDone();
        }

        @Override
        public void start() {
            Entity f = from();
            if (f == null) return;
            Vec3 away = fish.position().subtract(f.position());
            if (away.lengthSqr() < 1e-4) away = new Vec3(fish.getRandom().nextDouble() - 0.5, 0, fish.getRandom().nextDouble() - 0.5);
            Vec3 goal = clampToTank(fish, fish.position().add(away.normalize().scale(2.5)));
            fish.getNavigation().moveTo(goal.x, goal.y, goal.z, 1.6);
        }
    }

    /** Hover just below the nest. */
    static final class Nest extends Goal {
        private final BettaEntity fish;

        Nest(BettaEntity fish) {
            this.fish = fish;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return fish.nestSpot != null && fish.level().getGameTime() < fish.nestUntil && fish.isInWater()
                    && fish.getRandom().nextInt(4) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return fish.nestSpot != null && fish.level().getGameTime() < fish.nestUntil && !fish.getNavigation().isDone();
        }

        @Override
        public void start() {
            Vec3 s = fish.nestSpot;
            var r = fish.getRandom();
            Vec3 goal = clampToTank(fish, new Vec3(s.x + (r.nextDouble() - 0.5) * 0.6, s.y - 0.25 - r.nextDouble() * 0.3,
                    s.z + (r.nextDouble() - 0.5) * 0.6));
            fish.getNavigation().moveTo(goal.x, goal.y, goal.z, 0.9);
        }
    }
}
