package org.overengineer.bettacraft.terrarium;

import java.util.Locale;
import javax.annotation.Nullable;

/**
 * M33.1: the tank type chosen in the menu. {@link #AUTO} (the default for new and old tanks) works the mode out from the
 * water and the land like M31 ({@link TerrariumMode#detect}); a fixed type makes the tank that mode and limits the water
 * every way it can come in ({@link WaterLimit}): a terrarium takes no water, a paludarium only up to the height where
 * its land part stays at {@code terrarium.paludariumLandShare}, an aquarium as much as fits.
 */
public enum TankModeSetting {
    AUTO, AQUARIUM, PALUDARIUM, TERRARIUM;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "gui.bettacraft.tank_mode." + id();
    }

    /** The mode a fixed type makes the tank, null for {@link #AUTO}. */
    @Nullable
    public TerrariumMode fixed() {
        return switch (this) {
            case AUTO -> null;
            case AQUARIUM -> TerrariumMode.AQUARIUM;
            case PALUDARIUM -> TerrariumMode.PALUDARIUM;
            case TERRARIUM -> TerrariumMode.TERRARIUM;
        };
    }

    public TankModeSetting next() {
        TankModeSetting[] v = values();
        return v[(ordinal() + 1) % v.length];
    }

    public static TankModeSetting byOrdinal(int i) {
        TankModeSetting[] v = values();
        return i >= 0 && i < v.length ? v[i] : AUTO;
    }

    /** By its id; unknown or empty ids (older worlds) are {@link #AUTO}. */
    public static TankModeSetting byId(@Nullable String id) {
        if (id != null) for (TankModeSetting s : values()) if (s.id().equals(id)) return s;
        return AUTO;
    }
}
