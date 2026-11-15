package org.overengineer.bettacraft.terrarium;

import java.util.Locale;

/**
 * M31: what a tank is, worked out from its water and its land (it is never set by hand): an <b>aquarium</b> (water, little
 * or no land: everything as before M31), a <b>paludarium</b> (water and a land part built in the interior above the water
 * line) or a <b>terrarium</b> (no water, or so much land that the water is only a dish). The land share is the share of the
 * interior columns whose walkable top (the M14 height map, {@code InteriorGround}) lies above the water surface. Pure, unit-tested.
 */
public enum TerrariumMode {
    AQUARIUM, PALUDARIUM, TERRARIUM;

    /** Thresholds of the land share (config {@code terrarium.paludariumLandShare}, {@code terrarium.terrariumLandShare}). */
    public record Thresholds(double paludarium, double terrarium) {
        public static final Thresholds DEFAULT = new Thresholds(0.15, 0.9);
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "gui.bettacraft.terrarium.mode." + id();
    }

    /** True for the two modes with a land part (the climate values are simulated only there). */
    public boolean hasLand() {
        return this != AQUARIUM;
    }

    public static TerrariumMode byOrdinal(int i) {
        TerrariumMode[] v = values();
        return i >= 0 && i < v.length ? v[i] : AQUARIUM;
    }

    /**
     * The mode of a tank: no water at all = terrarium; with water, a land share from {@code paludarium} on = paludarium, from
     * {@code terrarium} on = terrarium (the water is a dish); else an aquarium.
     */
    public static TerrariumMode detect(long waterMb, double landShare, Thresholds th) {
        if (waterMb <= 0) return TERRARIUM;
        if (!(landShare >= 0)) return AQUARIUM;
        if (landShare >= th.terrarium() - 1e-9) return TERRARIUM;
        if (landShare >= th.paludarium() - 1e-9) return PALUDARIUM;
        return AQUARIUM;
    }
}
