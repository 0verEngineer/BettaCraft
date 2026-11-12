package org.overengineer.bettacraft.client.render;

import net.minecraft.util.Mth;
import org.overengineer.bettacraft.client.model.BettaModelDefinitions;
import org.overengineer.bettacraft.genetics.BettaColors;
import org.overengineer.bettacraft.genetics.BettaGenome;

/** ARGB tint colours of the mask layers (shared by the entity renderer and the item colours). */
public final class BettaTints {
    private static final float SICK_SATURATION = 0.3F;
    private static final float SICK_BRIGHTNESS = 0.8F;

    private BettaTints() {}

    public static int body(BettaGenome g, boolean female, boolean sick) {
        return adjust(BettaColors.rgb(g.bodyColor()), female, sick);
    }

    public static int fins(BettaGenome g, boolean female, boolean sick) {
        return adjust(BettaColors.rgb(g.finColor()), female, sick);
    }

    public static int pattern(BettaGenome g, boolean female, boolean sick) {
        return adjust(BettaColors.rgb(g.patternColor()), female, sick);
    }

    /** Sheen; alpha from the gene strength, softly pulsing. */
    public static int iridescence(BettaGenome g, boolean sick, int tick) {
        float strength = "high".equals(g.iridescence()) ? 0.8F : 0.45F;
        if (sick) strength *= 0.4F;
        strength *= 0.85F + 0.15F * Mth.sin(tick * 0.08F);
        int a = Mth.clamp((int) (strength * 255), 0, 255);
        return (a << 24) | BettaColors.rgb(g.iridescenceColor());
    }

    static int adjust(int rgb, boolean female, boolean sick) {
        float sat = 1.0F, val = 1.0F;
        if (female) sat *= BettaModelDefinitions.FEMALE_SATURATION;
        if (sick) {
            sat *= SICK_SATURATION;
            val *= SICK_BRIGHTNESS;
        }
        return 0xFF000000 | scaleSaturation(rgb, sat, val);
    }

    /** Scales HSV saturation and value of an RGB colour. */
    public static int scaleSaturation(int rgb, float satFactor, float valFactor) {
        float r = ((rgb >> 16) & 0xFF) / 255F, g = ((rgb >> 8) & 0xFF) / 255F, b = (rgb & 0xFF) / 255F;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float v = max, s = max == 0 ? 0 : (max - min) / max, h;
        float d = max - min;
        if (d == 0) h = 0;
        else if (max == r) h = ((g - b) / d) % 6;
        else if (max == g) h = (b - r) / d + 2;
        else h = (r - g) / d + 4;
        h /= 6F;
        if (h < 0) h += 1;
        s = Mth.clamp(s * satFactor, 0, 1);
        v = Mth.clamp(v * valFactor, 0, 1);
        return Mth.hsvToRgb(h, s, v) & 0xFFFFFF;
    }
}
