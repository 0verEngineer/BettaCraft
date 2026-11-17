package org.overengineer.bettacraft.equipment;

import java.util.Locale;
import net.minecraft.core.Direction;

/**
 * M22.4: on which side of the tank the hose of the external filter in a cabinet goes up, seen from the front of the
 * cabinet (its door): the back wall (the default, as before), the left or right wall, or the front glass. The water jet,
 * the bubbles and the current in the tank come from that wall.
 */
public enum OutletSide {
    BACK, LEFT, RIGHT, FRONT;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "gui.bettacraft.cabinet.outlet." + id();
    }

    /**
     * The tank wall the hose goes up (pointing away from the tank). {@code front} is the cabinet's {@code FACING} (towards
     * the player standing in front of it): left and right are the player's left and right.
     */
    public Direction wall(Direction front) {
        return switch (this) {
            case BACK -> front.getOpposite();
            case FRONT -> front;
            case LEFT -> front.getClockWise();
            case RIGHT -> front.getCounterClockWise();
        };
    }

    /** The next side for the menu button (back, left, right, front, back ...). */
    public OutletSide next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /** By id, BACK for null and unknown ids (old cabinets). */
    public static OutletSide byId(String id) {
        if (id != null) for (OutletSide s : values()) if (s.id().equals(id)) return s;
        return BACK;
    }

    /** By ordinal, BACK when out of range. */
    public static OutletSide byIndex(int i) {
        return i >= 0 && i < values().length ? values()[i] : BACK;
    }
}
