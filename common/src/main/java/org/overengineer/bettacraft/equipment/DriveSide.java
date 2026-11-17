package org.overengineer.bettacraft.equipment;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;

/**
 * M41: on which side a cabinet takes a Create shaft (its drive input), seen from the cabinet's door like {@link OutletSide}:
 * none (the default, also for cabinets from before M41), the back, left or right wall, the front (the door) or the bottom.
 */
public enum DriveSide {
    NONE, BACK, LEFT, RIGHT, FRONT, BOTTOM;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "gui.bettacraft.cabinet.drive." + id();
    }

    /** The face of the cabinet the shaft goes into (pointing away from the cabinet), null for {@link #NONE}. */
    @Nullable
    public Direction face(Direction front) {
        return switch (this) {
            case NONE -> null;
            case BACK -> front.getOpposite();
            case FRONT -> front;
            case LEFT -> front.getClockWise();
            case RIGHT -> front.getCounterClockWise();
            case BOTTOM -> Direction.DOWN;
        };
    }

    /** The next side for the menu button (none, back, left, right, front, bottom, none ...). */
    public DriveSide next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public DriveSide previous() {
        return values()[(ordinal() + values().length - 1) % values().length];
    }

    /** By id, null for unknown ids. */
    @Nullable
    public static DriveSide byIdOrNull(@Nullable String id) {
        if (id != null) for (DriveSide s : values()) if (s.id().equals(id)) return s;
        return null;
    }

    /** By id, NONE for null and unknown ids (cabinets from before M41). */
    public static DriveSide byId(@Nullable String id) {
        DriveSide s = byIdOrNull(id);
        return s == null ? NONE : s;
    }

    /** By ordinal, NONE when out of range. */
    public static DriveSide byIndex(int i) {
        return i >= 0 && i < values().length ? values()[i] : NONE;
    }
}
