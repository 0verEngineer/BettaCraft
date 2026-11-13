package org.overengineer.bettacraft.plants;

/**
 * Colours of the aquarium plants (M13): the block textures are grey and tinted with these colours (block colour handler,
 * also in the miniature, so shader packs see an ordinary tinted block like leaves or grass). Red species get redder in
 * strong light like the red root floater of M12; the redness is stored in the block state as {@code tint} 0..3 and
 * follows the light of the tank on every growth check. Pure functions (unit-tested).
 */
public final class PlantColors {
    /** Highest tint level of a block state. */
    public static final int MAX_TINT = 3;

    private PlantColors() {}

    /** Tint level of red species for the light at the surface (0..15): 0 below 9, 3 from 14 (same classes as M12). */
    public static int tintFor(int light) {
        return light < 9 ? 0 : light < 12 ? 1 : light < 14 ? 2 : 3;
    }

    /** RGB colour of a part of a species at a tint level (0..3; ignored by species that do not change colour). */
    public static int color(PlantSpecies s, PlantPart part, int tint) {
        double f = Math.max(0, Math.min(MAX_TINT, tint)) / (double) MAX_TINT;
        PlantPart p = part == PlantPart.ROOT ? rootLooksLike(s.form()) : part;
        int base = switch (p) {
            case STEM -> s.stem;
            case RHIZOME -> s.rhizome;
            case TOP -> s.top;
            default -> s.leaf;
        };
        return switch (s.red()) {
            case NONE -> base;
            case RED -> switch (p) {
                case TOP -> lerp(s.top, brighter(s.redColor, 1.12), Math.min(1, 0.2 + f));
                case LEAF, PAD, RIBBON, MAT, MOSS, CUTTING -> lerp(s.leaf, s.redColor, 0.1 + 0.8 * f);
                case STEM -> lerp(s.stem, darker(s.redColor, 0.8), 0.15 + 0.5 * f);
                default -> lerp(base, darker(s.redColor, 0.7), 0.3 * f);
            };
            case TIPS -> switch (p) {
                case TOP -> lerp(s.top, s.redColor, f);
                case LEAF -> lerp(s.leaf, s.redColor, 0.12 * f);
                case STEM -> lerp(s.stem, darker(s.redColor, 0.8), 0.4 * f);
                default -> base;
            };
            case FLAMINGO -> switch (p) {
                case TOP -> lerp(s.top, brighter(s.redColor, 1.05), 0.25 + 0.75 * f);
                case LEAF, CUTTING -> brighter(lerp(s.leaf, s.redColor, 0.1 + 0.6 * f), 1.0 + 0.08 * f);
                default -> lerp(base, s.redColor, 0.3 * f);
            };
            case CORAL -> coral(s, p, base, tint);
        };
    }

    /** Bleached skeleton white of a coral that lost its algae. */
    public static final int BLEACHED = 0xE8E4D8;

    /** M20: coral colours: bleached (0), pale (1), healthy (2), glowing under actinic light (3). */
    static int coral(PlantSpecies s, PlantPart p, int base, int tint) {
        boolean skeleton = p == PlantPart.STEM && s.stony();
        return switch (Math.max(0, Math.min(MAX_TINT, tint))) {
            case 0 -> skeleton ? lerp(base, BLEACHED, 0.6) : lerp(base, BLEACHED, 0.82);
            case 1 -> lerp(base, BLEACHED, 0.4);
            case 2 -> base;
            default -> skeleton ? base : p == PlantPart.TOP ? brighter(lerp(s.top, s.redColor, 0.7), 1.12) : lerp(base, s.redColor, 0.35);
        };
    }

    /** Redness of a colour (red minus green, -1..1), used by the tests and the menu. */
    public static double redness(int rgb) {
        return (((rgb >> 16) & 0xFF) - ((rgb >> 8) & 0xFF)) / 255.0;
    }

    /** What the root block looks like (its model and colour): rhizome, stem foot, crown leaves, bulb ... */
    public static PlantPart rootLooksLike(PlantSpecies.Form form) {
        return switch (form) {
            case EPIPHYTE, BULB -> PlantPart.RHIZOME;
            case STEM -> PlantPart.STEM;
            case RIBBON -> PlantPart.RIBBON;
            case CARPET -> PlantPart.MAT;
            case MOSS -> PlantPart.MOSS;
            case ROSETTE, POLYPS, BRANCH, DOME, ANEMONE -> PlantPart.LEAF;
            case DISC, TOADSTOOL -> PlantPart.STEM;
        };
    }

    static int lerp(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = (int) Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }

    static int brighter(int c, double k) {
        int r = Math.min(255, (int) Math.round(((c >> 16) & 0xFF) * k));
        int g = Math.min(255, (int) Math.round(((c >> 8) & 0xFF) * k));
        int b = Math.min(255, (int) Math.round((c & 0xFF) * k));
        return (r << 16) | (g << 8) | b;
    }

    static int darker(int c, double k) {
        return brighter(c, k);
    }
}
