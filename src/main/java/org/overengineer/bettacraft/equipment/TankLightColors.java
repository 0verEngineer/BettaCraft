package org.overengineer.bettacraft.equipment;

import java.util.List;
import net.minecraft.world.item.DyeColor;

/**
 * M18: the 16 colours of the aquarium light. The colour is optics only: the light level (15) and the light the plants
 * and the red root floater get are the same for every colour. White is the light of the earlier versions (no tint).
 * The RGB values are light colours (brighter and more saturated than the dye colours); black is a violet "black light".
 */
public final class TankLightColors {
    private TankLightColors() {}

    /** RGB of the light (white = 0xFFFFFF, i.e. no tint). */
    public static int rgb(DyeColor c) {
        return switch (c) {
            case WHITE -> 0xFFFFFF;
            case ORANGE -> 0xFF9A3C;
            case MAGENTA -> 0xF050E8;
            case LIGHT_BLUE -> 0x6CC4FF;
            case YELLOW -> 0xFFE85A;
            case LIME -> 0x9CFF4E;
            case PINK -> 0xFF96C8;
            case GRAY -> 0x9098A0;
            case LIGHT_GRAY -> 0xC8D0D8;
            case CYAN -> 0x3CE6E6;
            case PURPLE -> 0xA45AFF;
            case BLUE -> 0x4A64FF;
            case BROWN -> 0xC88A4E;
            case GREEN -> 0x46D24A;
            case RED -> 0xFF4646;
            case BLACK -> 0x6A30D0;
        };
    }

    /**
     * Blended colour of the lit lights of a tank: the mean RGB of all of them (white ones dilute the others). 0 when
     * nothing is lit or all lit lights are white (water keeps its own colour).
     */
    public static int blend(List<DyeColor> lit) {
        if (lit.isEmpty()) return 0;
        long r = 0, g = 0, b = 0;
        boolean coloured = false;
        for (DyeColor c : lit) {
            int v = rgb(c);
            r += (v >> 16) & 0xFF;
            g += (v >> 8) & 0xFF;
            b += v & 0xFF;
            coloured |= c != DyeColor.WHITE;
        }
        if (!coloured) return 0;
        int n = lit.size();
        return (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
    }

    /** Linear mix of two RGB colours (t = share of {@code b}); alpha of {@code a} kept. */
    public static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = Math.round(ar + (br - ar) * t), g = Math.round(ag + (bg - ag) * t), bl = Math.round(ab + (bb - ab) * t);
        return (a & 0xFF000000) | r << 16 | g << 8 | bl;
    }

    /**
     * Water colour under a coloured light: the water colour mixed with the light colour, keeping the brightness of the
     * water (so a blue light does not make it darker or a yellow one milky). {@code light} 0 = unchanged.
     */
    public static int tintWater(int water, int light, float strength) {
        if (light == 0 || strength <= 0) return water;
        float lw = luma(water), ll = Math.max(1, luma(light));
        float k = lw / ll;
        int r = clamp(Math.round(((light >> 16) & 0xFF) * k)), g = clamp(Math.round(((light >> 8) & 0xFF) * k)), b = clamp(Math.round((light & 0xFF) * k));
        return mix(water, (water & 0xFF000000) | r << 16 | g << 8 | b, Math.min(1F, strength));
    }

    /**
     * Tint of the panel texture: lit = the light colour; off = a paler colour (the "off" texture is dark grey already,
     * so the colour still shows faintly). White: no tint, the look of earlier versions.
     */
    public static int panel(DyeColor c, boolean lit) {
        int v = rgb(c);
        return lit ? v : mix(v, 0xFFFFFF, 0.4F);
    }

    static float luma(int rgb) {
        return 0.299F * ((rgb >> 16) & 0xFF) + 0.587F * ((rgb >> 8) & 0xFF) + 0.114F * (rgb & 0xFF);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
