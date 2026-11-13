package org.overengineer.bettacraft.entity.tankmate;

import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

/**
 * M19: swims straight to the wanted position in smooth curves (like the bettas' move control, no path finding: the
 * behaviour checks the decoration along the line). Slows down while the target is not ahead and near the target, so a
 * shoal does not overshoot; a fish without a target hovers (a little buoyancy, damped).
 */
final class TankmateMoveControl extends MoveControl {
    private final TankmateEntity fish;
    /** M21: follows the height of the target closely (the way into a cave). */
    boolean precise;

    TankmateMoveControl(TankmateEntity fish) {
        super(fish);
        this.fish = fish;
    }

    @Override
    public void tick() {
        if (fish.isEyeInFluid(FluidTags.WATER)) fish.setDeltaMovement(fish.getDeltaMovement().add(0, 0.005, 0));
        if (operation != Operation.MOVE_TO) {
            fish.setSpeed(0);
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
            float turn = speedModifier > 1.1 ? 26F : 10F;
            fish.setYRot(rotlerp(fish.getYRot(), want, turn));
            fish.yBodyRot = fish.getYRot();
            fish.yHeadRot = fish.getYRot();
            float off = Math.abs(Mth.wrapDegrees(want - fish.getYRot())) * Mth.DEG_TO_RAD;
            speed *= Math.max(0.2F, Mth.cos(off));
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
