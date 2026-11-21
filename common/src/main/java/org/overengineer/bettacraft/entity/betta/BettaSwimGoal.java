package org.overengineer.bettacraft.entity.betta;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.phys.Vec3;

/**
 * Random swimming. Inside a tank the target is picked inside the tank water volume (never outside the glass) and
 * hand-fed food attracts the fish; outside a tank this is the vanilla fish behaviour.
 */
final class BettaSwimGoal extends RandomSwimmingGoal {
    private final BettaEntity fish;

    BettaSwimGoal(BettaEntity fish) {
        super(fish, 1.0, 30);
        this.fish = fish;
    }

    @Override
    public boolean canUse() {
        if (fish.foodTarget != null && fish.level().getGameTime() < fish.foodUntil) {
            trigger();
        }
        return super.canUse();
    }

    private static final double CRUISE = 0.75;
    /** M54: a dart (speed and the share of the swims that are one). */
    static final double DART = 1.6;
    static final float DART_CHANCE = 0.1F;
    private boolean direct;
    private int directTicks;

    @Override
    public void start() {
        // in a tank: straight to the target (BettaMoveControl#swimTo), outside the vanilla path finding
        direct = fish.tankWater != null && fish.getMoveControl() instanceof BettaMoveControl;
        if (!direct) {
            super.start();
            return;
        }
        directTicks = 0;
        // calm cruising speed (the path finder used to brake at every node); hand-fed food is approached at full speed
        boolean food = fish.foodTarget != null && fish.level().getGameTime() < fish.foodUntil;
        // M54 (lively movement): now and then a short dart instead of the calm cruise
        boolean dart = !food && org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.LIVELY_MOVEMENT)
                && fish.getRandom().nextFloat() < DART_CHANCE;
        ((BettaMoveControl) fish.getMoveControl()).swimTo(wantedX, wantedY, wantedZ, food ? 1.0 : dart ? DART : CRUISE);
    }

    @Override
    public boolean canContinueToUse() {
        if (!direct) return super.canContinueToUse();
        return ++directTicks < 200 && fish.tankWater != null && ((BettaMoveControl) fish.getMoveControl()).isSwimmingDirect();
    }

    @Override
    public void stop() {
        if (direct && fish.getMoveControl() instanceof BettaMoveControl m) m.stopDirect();
        direct = false;
        super.stop();
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
        // M24: now and then a swim into the current of an outlet of the interior pipes
        if (r.nextFloat() < 0.15F && fish.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            var c = org.overengineer.bettacraft.tank.TankMultiblock.getController(sl, net.minecraft.core.BlockPos.containing(fish.position()));
            if (c != null) {
                var cur = org.overengineer.bettacraft.plumbing.interior.InteriorPlumbing.currents(sl, c);
                if (!cur.isEmpty()) {
                    var k = cur.get(r.nextInt(cur.size()));
                    if (k.strength() >= 0.5) {
                        double along = 0.6 + r.nextDouble() * 1.8, side = (r.nextDouble() - 0.5) * 0.8;
                        Vec3 t = clamp(box, new Vec3(k.x() + k.dx() * along - k.dz() * side, fish.getY() + (r.nextDouble() - 0.5) * 0.4,
                                k.z() + k.dz() * along + k.dx() * side));
                        if (fish.pathFreeOfDecor(fish.position(), t)) return t;
                    }
                }
            }
        }
        // Cruise: 1.5 to 5 blocks, mostly roughly ahead (fewer turns), mostly at the same depth (a depth change now and
        // then), never through interior decoration (PLAN 4.2). Targets cut short by the glass are only a fallback.
        Vec3 p = fish.position();
        double heading = fish.getYRot() * Mth.DEG_TO_RAD;
        Vec3 best = null;
        double bestLen = -1;
        for (int attempt = 0; attempt < 10; attempt++) {
            double dist = 1.5 + r.nextDouble() * 3.5;
            double ang = r.nextFloat() < 0.7 ? heading + (r.nextDouble() - 0.5) * (2 * Math.PI / 3) : r.nextDouble() * 2 * Math.PI;
            double dy = r.nextFloat() < 0.15 ? (r.nextDouble() - 0.5) * 1.6 : (r.nextDouble() - 0.5) * 0.2;
            Vec3 c = clamp(box, new Vec3(p.x - Math.sin(ang) * dist, p.y + dy, p.z + Math.cos(ang) * dist));
            if (!fish.pathFreeOfDecor(p, c)) continue;
            double len = Math.hypot(c.x - p.x, c.z - p.z);
            if (len >= 1.0) return c;
            if (len > bestLen) {
                best = c;
                bestLen = len;
            }
        }
        return best;
    }

    /** Inside the actual water of the tank (M10: never in the notch of a free shape). */
    private static Vec3 clamp(org.overengineer.bettacraft.tank.TankWaterVolume water, Vec3 v) {
        return water.clamp(v, 0.25 + 1.0 / 16, 0.1 + 1.0 / 16, 0.35);
    }
}
