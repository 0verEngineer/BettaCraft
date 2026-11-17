package org.overengineer.bettacraft.terrarium;

import java.util.Locale;
import javax.annotation.Nullable;

/**
 * M44: the walk animation of mobs in our tanks (client). Vanilla turns the distance a mob moved in one tick into the swing
 * of its legs ({@code min(distance * 4, 1)}) without its size: a zombie shrunk to 0.18 in a mob jar that walks 0.011 blocks a
 * tick barely moves its legs and glides. In a tank the distance is divided by the mob's scale and multiplied by a factor
 * (client config {@code walkAnimationBoost}: "auto" = {@link #SHRUNK} for shrunk mobs, {@link #FULL} for full-size pets, a
 * number = that factor for all, 0 = vanilla). Climbers (up a wall: mostly vertical moves, spiders) count the climb as well.
 */
public final class WalkAnimation {
    /** The setting "auto". */
    public static final float AUTO = -1F;
    /** Auto factor for mobs shrunk by the tank (the mobs in glass), measured: swing 0.36-0.56 like outside. */
    public static final float SHRUNK = 1.5F;
    /** Auto factor for animals in their real size (the terrarium pets walk 3-5 times slower than outside). */
    public static final float FULL = 3.0F;
    /** The largest factor the config takes. */
    public static final float MAX = 10F;

    private WalkAnimation() {}

    /** The setting from the config text: {@link #AUTO}, a factor 0..{@link #MAX}, or null when it is neither. */
    @Nullable
    public static Float parse(@Nullable String text) {
        if (text == null) return null;
        String t = text.trim().toLowerCase(Locale.ROOT);
        if (t.equals("auto")) return AUTO;
        try {
            float f = Float.parseFloat(t.replace(',', '.'));
            return Float.isFinite(f) && f >= 0 && f <= MAX ? f : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** The factor for a mob of this scale: the setting itself, with "auto" by the scale. */
    public static float factor(float setting, float scale) {
        if (setting >= 0) return setting;
        return scale < 0.99F ? SHRUNK : FULL;
    }

    /** Whether a move counts as climbing: (nearly) straight up or down. */
    public static boolean mostlyVertical(double horizontal, double vertical) {
        return Math.abs(vertical) > 1e-4 && Math.abs(vertical) > 2 * horizontal;
    }

    /** The distance handed to {@code updateWalkAnimation}: the move (with the climb for climbers) / scale * factor. */
    public static float distance(double horizontal, double vertical, boolean climb, float scale, float factor) {
        double d = climb ? Math.sqrt(horizontal * horizontal + vertical * vertical) : horizontal;
        return (float) (d / Math.max(0.05F, scale) * factor);
    }

    /** The leg swing vanilla aims at for a distance (the limit 1 is a run). */
    public static float swing(float distance) {
        return Math.min(distance * 4F, 1F);
    }
}
