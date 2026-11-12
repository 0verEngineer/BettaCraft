package org.overengineer.bettacraft.tank;

import net.minecraft.util.StringRepresentable;

/** Where the name sign sits on the tank's front face (PLAN 4.4). */
public enum SignPosition implements StringRepresentable {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT;

    private final String serialized = name().toLowerCase(java.util.Locale.ROOT);

    @Override
    public String getSerializedName() {
        return serialized;
    }

    public boolean top() {
        return ordinal() < 3;
    }

    /** -1 left, 0 centre, 1 right (seen from the front). */
    public int horizontal() {
        return ordinal() % 3 - 1;
    }

    public String translationKey() {
        return "gui.bettacraft.sign_pos." + serialized;
    }

    public static SignPosition byName(String s) {
        for (SignPosition p : values()) if (p.serialized.equals(s)) return p;
        return TOP_CENTER;
    }

    public SignPosition next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
