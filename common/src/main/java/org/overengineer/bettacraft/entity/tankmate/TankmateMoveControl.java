package org.overengineer.bettacraft.entity.tankmate;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

/**
 * M19: swims straight to the wanted position in smooth curves (like the bettas' move control, no path finding: the
 * behaviour checks the decoration along the line). Slows down while the target is not ahead and near the target, so a
 * shoal does not overshoot; a fish without a target hovers (a little buoyancy, damped).
 */
class TankmateMoveControl extends MoveControl {
    protected final TankmateEntity fish;
    /** M21: follows the height of the target closely (the way into a cave). */
    boolean precise;
    /** M21.5: turn rate (degrees per tick) eased like a real fish's, so it swims curves instead of turning on the spot. */
    protected float yawVel;

    TankmateMoveControl(TankmateEntity fish) {
        super(fish);
        this.fish = fish;
    }

    @Override
    public void tick() {
        buoyancy(fish);
        if (operation != Operation.MOVE_TO) {
            fish.setSpeed(0);
            yawVel *= 0.7F;
            if (fish.isInWater()) fish.setDeltaMovement(fish.getDeltaMovement().multiply(0.8, 0.6, 0.8));
            return;
        }
        double dx = wantedX - fish.getX(), dy = wantedY - fish.getY(), dz = wantedZ - fish.getZ();
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (d < 0.03 || speedModifier <= 0) {
            operation = Operation.WAIT;
            fish.setSpeed(0);
            return;
        }
        float speed = (float) (speedModifier * fish.getAttributeValue(Attributes.MOVEMENT_SPEED));
        // small fish take smaller steps (a 2 cm rasbora does not cross the tank in a second)
        speed *= (float) Mth.clamp(0.45 + fish.species().length() * 1.4, 0.5, 1.2) * Math.max(0.5F, fish.getAgeScale());
        if (d < 0.4) speed *= (float) Math.max(0.25, d / 0.4);
        double horiz = dx * dx + dz * dz;
        if (horiz > 1e-5) {
            float want = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F;
            boolean fast = speedModifier > 1.1;
            if (Features.enabled(Feature.SMOOTH_SWIMMING)) {
                // M21.5: the turn rate accelerates and brakes (no snapping), the fish keeps some speed while it turns: a curve
                float maxTurn = fast ? 18F : precise ? 12F : 7F;
                float diff = Mth.wrapDegrees(want - fish.getYRot());
                float wantVel = Mth.clamp(diff * 0.3F, -maxTurn, maxTurn);
                yawVel += Mth.clamp(wantVel - yawVel, -2.5F, 2.5F);
                fish.setYRot(fish.getYRot() + yawVel);
                fish.yBodyRot = fish.getYRot();
                fish.yHeadRot = fish.getYRot();
                float off = Math.abs(Mth.wrapDegrees(want - fish.getYRot())) * Mth.DEG_TO_RAD;
                speed *= (float) Mth.clamp(0.55 + 0.45 * Mth.cos(off), precise ? 0.2 : 0.35, 1.0);
                // close to the target it may turn on the spot (nothing to curve around)
                if (d < 0.25) speed *= 0.6F + 0.4F * Mth.cos(off * 0.5F);
            } else {
                float turn = fast ? 26F : 10F;
                fish.setYRot(rotlerp(fish.getYRot(), want, turn));
                fish.yBodyRot = fish.getYRot();
                fish.yHeadRot = fish.getYRot();
                float off = Math.abs(Mth.wrapDegrees(want - fish.getYRot())) * Mth.DEG_TO_RAD;
                speed *= Math.max(0.2F, Mth.cos(off));
            }
        } else {
            yawVel *= 0.7F;
        }
        fish.setSpeed(Mth.lerp(0.15F, fish.getSpeed(), speed));
        // vertical: also when the target is straight above or below (a cory dropping to the bottom)
        double vy = Mth.clamp(dy, -1, 1) * Math.max(0.02, speed) * 0.12;
        if (precise) {
            vy = Mth.clamp(dy * 0.25, -0.03, 0.03);
            fish.setDeltaMovement(fish.getDeltaMovement().multiply(1, 0.5, 1));
        }
        fish.setDeltaMovement(fish.getDeltaMovement().add(0, vy, 0));
    }

    /**
     * Lifts the fish by what vanilla fish travel lets it sink after every move ({@code AbstractFish}: -0.005 a tick without an
     * attack target), so that it hovers. M52: only while that sink is still in its motion: {@code LivingEntity#aiStep} first
     * zeroes a vertical motion below 0.003 (as do the tank's clamp and a collision), and the lift added anyway carried every
     * slow fish up by up to 0.005 a tick: it bobbed, and an otocinclus pressed to the surface never reached its spot on the
     * glass 0.2 blocks below (glassCleanersStandUpOnlyAtTheGlass).
     */
    static void buoyancy(TankmateEntity fish) {
        if (fish.getDeltaMovement().y != 0 && fish.isEyeInFluid(FluidTags.WATER)) fish.setDeltaMovement(fish.getDeltaMovement().add(0, 0.005, 0));
    }

    void swimTo(double x, double y, double z, double speed) {
        setWantedPosition(x, y, z, speed);
    }

    void stop() {
        operation = Operation.WAIT;
    }

    boolean moving() {
        return operation == Operation.MOVE_TO;
    }
}
