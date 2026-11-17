package org.overengineer.bettacraft.entity.betta;

import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

/**
 * Like the vanilla fish move control, but the fish turns in smooth curves instead of snapping (vanilla turns up to
 * 90 degrees per tick): at most {@link #TURN} degrees per tick when cruising ({@link #TURN_FAST} when fleeing, chasing or
 * panicking), and it slows down while the target is not ahead, so it swims round in an arc instead of turning on the spot.
 * <p>
 * Inside a tank the swim goal steers it straight to its target ({@link #swimTo}) instead of using the path finder: the
 * water is open (decoration is checked along the straight line) and the path nodes snap every target to the bottom of a
 * block, which made the fish bob up and down.
 */
final class BettaMoveControl extends MoveControl {
    private static final float TURN = 7F, TURN_FAST = 24F;
    private final BettaEntity fish;
    private boolean direct;

    BettaMoveControl(BettaEntity fish) {
        super(fish);
        this.fish = fish;
    }

    @Override
    public void tick() {
        if (fish.isEyeInFluid(FluidTags.WATER)) fish.setDeltaMovement(fish.getDeltaMovement().add(0, 0.005, 0));
        if (direct && (operation != Operation.MOVE_TO || fish.distanceToSqr(wantedX, wantedY, wantedZ) < 0.3 * 0.3)) {
            direct = false;
            operation = Operation.WAIT;
        }
        if (operation == Operation.MOVE_TO && (direct || !fish.getNavigation().isDone())) {
            double dx = wantedX - fish.getX(), dy = wantedY - fish.getY(), dz = wantedZ - fish.getZ();
            float speed = (float) (speedModifier * fish.getAttributeValue(Attributes.MOVEMENT_SPEED));
            if (dx != 0 || dz != 0) {
                float want = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F;
                float turn = speedModifier > 1.1 ? TURN_FAST : TURN;
                fish.setYRot(rotlerp(fish.getYRot(), want, turn));
                fish.yBodyRot = fish.getYRot();
                // slow down while the target is beside or behind (cos of the remaining angle)
                float off = Math.abs(Mth.wrapDegrees(want - fish.getYRot())) * Mth.DEG_TO_RAD;
                speed *= Math.max(0.15F, Mth.cos(off));
            }
            fish.setSpeed(Mth.lerp(0.125F, fish.getSpeed(), speed));
            if (dy != 0) {
                double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                fish.setDeltaMovement(fish.getDeltaMovement().add(0, fish.getSpeed() * (dy / d) * 0.1, 0));
            }
        } else {
            fish.setSpeed(0);
        }
    }

    /** Swim straight to this point (no path finding), until reached or {@link #stopDirect}. */
    void swimTo(double x, double y, double z, double speed) {
        setWantedPosition(x, y, z, speed);
        direct = true;
    }

    boolean isSwimmingDirect() {
        return direct;
    }

    void stopDirect() {
        if (direct) {
            direct = false;
            operation = Operation.WAIT;
        }
    }
}
