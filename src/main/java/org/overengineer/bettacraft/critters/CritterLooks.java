package org.overengineer.bettacraft.critters;

import java.util.random.RandomGenerator;

/**
 * M21.5: the individual look of a snail that is not inherited: every red or yellow racer nerite (they do not breed in
 * fresh water, so their colour cannot be bred) and every apple snail gets a random 16-bit {@code look} when it is made
 * (trader, net, creative, a hatching clutch). It picks a colour of the species' palette, a brightness and the lines or
 * bands drawn over the shell. {@code look = 0} is the classic look up to M21.1 (one colour, no lines): animals of older
 * worlds keep exactly the look they had.
 * <p>
 * Bits: 0-7 palette roll (0..255), 8-11 brightness (-8..+7 %), 12 line spacing (fine / wide), 13-15 band roll.
 */
public final class CritterLooks {
    /** What is drawn over the shell. */
    public enum Lines { NONE, FINE, WIDE, BANDS }

    /** Shell colour, colour of the lines and which lines. */
    public record Look(int body, int lines, Lines kind) {}

    private CritterLooks() {}

    /** Species with an individual look. */
    public static boolean has(CritterSpecies s) {
        return s == CritterSpecies.RED_NERITE || s == CritterSpecies.YELLOW_NERITE || s == CritterSpecies.APPLE_SNAIL;
    }

    /** A new random look (0 = classic when the feature is off or the species has none). */
    public static int random(CritterSpecies s, RandomGenerator rnd) {
        if (!has(s) || !org.overengineer.bettacraft.config.CommonConfig.snailColors()) return 0;
        return 1 + rnd.nextInt(0xFFFF);
    }

    private static int paletteRoll(int look) {
        return look & 0xFF;
    }

    private static float brightness(int look) {
        return (((look >> 8) & 0xF) - 8) / 100F;
    }

    /**
     * The look of an animal: {@code color} as from {@link CritterGenes#color}, {@code base} the colour without a look
     * (variety and grade).
     */
    public static Look of(CritterSpecies s, int color, int base, int look) {
        if (look == 0 || !has(s)) return new Look(base, 0, Lines.NONE);
        int roll = paletteRoll(look);
        boolean wide = ((look >> 12) & 1) != 0;
        Look l;
        if (s == CritterSpecies.RED_NERITE) {
            // orange (the old look), red, deep red, red with thin yellow lines
            if (roll < 77) l = new Look(0xC4521E, 0, Lines.NONE);
            else if (roll < 140) l = new Look(0xC8261A, 0, Lines.NONE);
            else if (roll < 190) l = new Look(0x8A1810, 0, Lines.NONE);
            else l = new Look(0xBE2A18, 0xF0C238, wide ? Lines.WIDE : Lines.FINE);
        } else if (s == CritterSpecies.YELLOW_NERITE) {
            // yellow, golden yellow, yellow with dark lines
            if (roll < 100) l = new Look(0xE8C428, 0, Lines.NONE);
            else if (roll < 165) l = new Look(0xEEA824, 0, Lines.NONE);
            else l = new Look(0xE4C030, 0x2E2418, wide ? Lines.WIDE : Lines.FINE);
        } else {
            // apple snails: the colour stays the genetic one; wild and gold ones may carry dark bands
            int band = (look >> 13) & 0x7;
            boolean bands = (color == CritterGenes.WILD_COLOR || color == 0) && band < 3;
            int dark = color == 0 ? 0x7A5418 : 0x2C1E12;
            l = new Look(base, dark, bands ? Lines.BANDS : Lines.NONE);
        }
        float b = brightness(look) * (s == CritterSpecies.APPLE_SNAIL ? 1.2F : 1.5F);
        return new Look(shade(l.body, b), l.lines == 0 ? 0 : shade(l.lines, b * 0.5F), l.kind);
    }

    /** Name key of the colour shown in the tooltip (null for the classic look and apple snails). */
    public static String nameKey(CritterSpecies s, int look) {
        if (look == 0 || s == CritterSpecies.APPLE_SNAIL || !has(s)) return null;
        int roll = paletteRoll(look);
        String name;
        if (s == CritterSpecies.RED_NERITE) name = roll < 77 ? "orange" : roll < 140 ? "red" : roll < 190 ? "deep_red" : "red_lines";
        else name = roll < 100 ? "yellow" : roll < 165 ? "golden" : "yellow_lines";
        return "critter.bettacraft.look." + name;
    }

    static int shade(int rgb, float f) {
        int r = clamp(Math.round(((rgb >> 16) & 0xFF) * (1 + f))), g = clamp(Math.round(((rgb >> 8) & 0xFF) * (1 + f))), b = clamp(Math.round((rgb & 0xFF) * (1 + f)));
        return (r << 16) | (g << 8) | b;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
