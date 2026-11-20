package org.overengineer.bettacraft.entity.tankmate;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * M30: the movement of the big animals ({@link org.overengineer.bettacraft.tankmates.LargeSpecies}): a heavy body that turns
 * slowly and never on the spot. The turn rate builds up and eases off gently and is limited to the species' turn rate (a few
 * degrees per tick), the animal keeps most of its speed through a curve, accelerates and brakes slowly, rises and sinks
 * gently, and glides on for a while when it has no target. The behaviour gives it far targets mostly ahead
 * ({@code TankmateBehaviour#large}), so it swims wide curves through the tank.
 */
final class LargeMoveControl extends TankmateMoveControl {
    /** Turn acceleration in degrees per tick and tick. */
    private static final float TURN_ACCEL = 0.35F;

    LargeMoveControl(TankmateEntity fish) {
        super(fish);
    }

    private float maxTurn() {
        var l = fish.species().large();
        return (float) (l == null ? 3.0 : l.turnRate());
    }

    @Override
    public void tick() {
        buoyancy(fish);
        float base = (float) fish.getAttributeValue(Attributes.MOVEMENT_SPEED);
        if (operation != Operation.MOVE_TO || speedModifier <= 0) {
            // no target: glide on, slowly losing speed and turn (a big body does not stop at once)
            yawVel *= 0.9F;
            turn();
            fish.setSpeed(fish.getSpeed() * 0.97F);
            if (fish.isInWater()) fish.setDeltaMovement(fish.getDeltaMovement().multiply(0.95, 0.7, 0.95));
            return;
        }
        double dx = wantedX - fish.getX(), dy = wantedY - fish.getY(), dz = wantedZ - fish.getZ();
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double len = Math.max(0.5, fish.species().length() * fish.getAgeScale());
        if (d < 0.1) {
            operation = Operation.WAIT;
            return;
        }
        float speed = (float) (speedModifier * base) * Math.max(0.5F, fish.getAgeScale());
        // brakes only close to the target (within half a body length)
        if (d < len * 0.5) speed *= (float) Math.max(0.35, d / (len * 0.5));
        double horiz = dx * dx + dz * dz;
        float off = 0;
        if (horiz > 1e-4) {
            float want = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F;
            float max = maxTurn() * (speedModifier > 1.1 ? 1.6F : 1.0F);
            float diff = Mth.wrapDegrees(want - fish.getYRot());
            float wantVel = Mth.clamp(diff * 0.08F, -max, max);
            yawVel += Mth.clamp(wantVel - yawVel, -TURN_ACCEL, TURN_ACCEL);
            off = Math.abs(diff) * Mth.DEG_TO_RAD;
        } else {
            yawVel *= 0.85F;
        }
        turn();
        // keeps most of its speed in a curve, never turns on the spot
        speed *= (float) Mth.clamp(0.7 + 0.3 * Mth.cos(off), 0.55, 1.0);
        fish.setSpeed(Mth.lerp(0.04F, fish.getSpeed(), speed));
        double vy = Mth.clamp(dy, -1, 1) * Math.max(0.02, speed) * 0.05;
        fish.setDeltaMovement(fish.getDeltaMovement().add(0, vy, 0));
    }

    private void turn() {
        if (Math.abs(yawVel) < 1e-3) return;
        fish.setYRot(fish.getYRot() + yawVel);
        fish.yBodyRot = fish.getYRot();
        fish.yHeadRot = fish.getYRot();
    }

    /** GameTests: the current turn rate in degrees per tick. */
    float turnRateNow() {
        return yawVel;
    }
}
