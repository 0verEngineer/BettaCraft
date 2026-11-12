package org.overengineer.bettacraft.tank.plants;

import java.util.Locale;

/**
 * Species of the surface plant layer of a tank (M12). Pure data (unit-tested): size, growth speed, waste uptake, how
 * much the bubble nest likes them and the length of the roots (rendering). {@link #LILY_PAD} is the vanilla lily pad on
 * a tank surface: it stays where it was put and never multiplies.
 */
public enum FloatingPlantType {
    /** Wasserlinse: tiny leaves in small patches, grows fastest (the starter plant from swamp water). */
    DUCKWEED("duckweed", 0.17, 1.6, 0.9, 0.5, 0.06),
    /** Froschbiss: round leaves, long roots hanging into the water, the favourite nest site. */
    FROGBIT("amazon_frogbit", 0.26, 0.7, 1.3, 1.0, 0.42),
    /** Schwimmfarn: small paired hairy leaves, robust, medium growth. */
    SALVINIA("salvinia", 0.22, 1.0, 1.1, 0.8, 0.14),
    /** Rote Wurzelflosse (Phyllanthus fluitans): red under strong light, green under weak light, slow. */
    RED_ROOT_FLOATER("red_root_floater", 0.2, 0.6, 1.2, 0.9, 0.3),
    /** Vanilla lily pad put onto a tank: fixed, does not move or grow. */
    LILY_PAD("lily_pad", 0.3, 0.0, 0.4, 0.9, 0.0);

    private final String id;
    private final double radius, growth, uptake, nest, roots;

    FloatingPlantType(String id, double radius, double growth, double uptake, double nest, double roots) {
        this.id = id;
        this.radius = radius;
        this.growth = growth;
        this.uptake = uptake;
        this.nest = nest;
        this.roots = roots;
    }

    /** Item id path (and save name). The lily pad uses the vanilla item. */
    public String id() { return id; }

    /** Radius (blocks) of a fully grown plant. */
    public double maxRadius() { return radius; }

    /** Growth per in-game day at full light and nutrients (size 0..1; at 1 it forms an offshoot). */
    public double growthPerDay() { return growth; }

    /** Waste taken up relative to the leaf area. */
    public double uptake() { return uptake; }

    /** How much the male likes building his nest under this plant (0..1). */
    public double nestFavour() { return nest; }

    /** Length of the roots below the surface (blocks, fully grown). */
    public double rootLength() { return roots; }

    /** Drifts with the water (all but the lily pad). */
    public boolean movable() { return this != LILY_PAD; }

    /** Grows and forms offshoots (all but the lily pad). */
    public boolean grows() { return this != LILY_PAD; }

    /** Radius at the given size (a young plant is a little smaller). */
    public double radius(double size) {
        return radius * (0.45 + 0.55 * Math.max(0, Math.min(1, size)));
    }

    public static FloatingPlantType byId(String s) {
        String k = s == null ? "" : s.toLowerCase(Locale.ROOT);
        for (FloatingPlantType t : values()) if (t.id.equals(k)) return t;
        return DUCKWEED;
    }

    /** Null for unknown names (save data of a newer version). */
    public static FloatingPlantType byIdOrNull(String s) {
        for (FloatingPlantType t : values()) if (t.id.equals(s)) return t;
        return null;
    }
}
