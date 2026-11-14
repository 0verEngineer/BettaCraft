package org.overengineer.bettacraft.client.render;

import net.minecraft.util.Mth;
import org.overengineer.bettacraft.client.model.BettaModelDefinitions;
import org.overengineer.bettacraft.genetics.BettaColors;
import org.overengineer.bettacraft.genetics.BettaGenome;

/** ARGB tint colours of the mask layers (shared by the entity renderer and the item colours). */
public final class BettaTints {
    private static final float SICK_SATURATION = 0.3F;
    private static final float SICK_BRIGHTNESS = 0.8F;

    /**
     * Tank water is drawn over the fish (translucent terrain after entities) and pulls every colour towards its blue.
     * The tints of fish inside a tank are pre-compensated: c' = (c - k * water) / (1 - k), so the fish shows its own
     * colours through the glass instead of turquoise ones (M5).
     */
    private static final float WATER_K = 0.16F;
    private static final float WATER_R = 0.36F, WATER_G = 0.56F, WATER_B = 0.74F;
    /** Set by the renderer for the fish being drawn (render thread only). */
    private static boolean inTank;

    private BettaTints() {}

    public static void beginFish(org.overengineer.bettacraft.entity.betta.BettaEntity f) {
        inTank = f.level() != null && f.level().getBlockState(f.blockPosition()).getBlock() instanceof org.overengineer.bettacraft.tank.TankBlock;
    }

    public static void endFish() {
        inTank = false;
    }

    static int compensate(int rgb) {
        if (!inTank) return rgb;
        float k = WATER_K, inv = 1.0F / (1.0F - k);
        int r = Mth.clamp(Math.round((((rgb >> 16) & 0xFF) / 255F - k * WATER_R) * inv * 255F), 0, 255);
        int g = Mth.clamp(Math.round((((rgb >> 8) & 0xFF) / 255F - k * WATER_G) * inv * 255F), 0, 255);
        int b = Mth.clamp(Math.round(((rgb & 0xFF) / 255F - k * WATER_B) * inv * 255F), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    /** Female breeding bars: dark on light fish, pale on dark fish (so they show on every colour). */
    public static int bars(BettaGenome g) {
        int rgb = BettaColors.rgb(g.bodyColor());
        double luma = (0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF)) / 255.0;
        return luma < 0.33 ? 0xD8E8E0D0 : 0xF0231A16;
    }

    /** Tint for a living fish: sex, sickness, life stage (fry pale and see-through) and flaring (more intense). */
    public static int forFish(int rgb, org.overengineer.bettacraft.entity.betta.BettaEntity f) {
        int stage = f.lifeStage().ordinal();
        float sat = BettaModelDefinitions.STAGE_SATURATION[stage], val = 1.0F;
        if (f.isFemale()) sat *= BettaModelDefinitions.FEMALE_SATURATION;
        if (f.isSick()) {
            sat *= SICK_SATURATION;
            val *= SICK_BRIGHTNESS;
        }
        if (f.isVaried() && !f.isSick()) { // M26 part b: a varied diet shows in the colours
            sat = Math.min(1.0F, sat * 1.12F);
            val *= 1.03F;
        }
        if (f.isFlaring()) {
            sat = Math.min(1.0F, sat * 1.25F + 0.05F);
            val *= 1.08F;
        }
        int a = Mth.clamp(Math.round(BettaModelDefinitions.STAGE_ALPHAS[stage] * 255), 0, 255);
        return (a << 24) | compensate(scaleSaturation(rgb, sat, val));
    }

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
        float strength = "high".equals(g.iridescence()) ? 0.5F : 0.24F;
        if (sick) strength *= 0.4F;
        strength *= 0.85F + 0.15F * Mth.sin(tick * 0.08F);
        int a = Mth.clamp((int) (strength * 255), 0, 255);
        return (a << 24) | compensate(BettaColors.rgb(g.iridescenceColor()));
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
