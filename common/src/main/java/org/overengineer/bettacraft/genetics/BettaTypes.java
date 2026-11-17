package org.overengineer.bettacraft.genetics;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Type name of a betta (tooltip, tank menu, Jade), e.g. "Mustard Gas Halfmoon Plakat" / "Halbmond-Plakat (Mustard Gas)".
 * <ul>
 *     <li><b>Form</b> from finform, fin_length and pectoral: long fins = the tail name ("Halfmoon"), short fins = plakat
 *     ("Plakat" for the roundtail, otherwise "Halfmoon Plakat"), dumbo pectorals add "Dumbo".</li>
 *     <li><b>Colour type</b>: the first matching named variety of {@link GeneticsData} (priority order), otherwise generic:
 *     "Solid Red", "Red/Blue Bicolor", "Red Butterfly", with "Metallic" for strong iridescence.</li>
 * </ul>
 * Varieties with {@code include_form = false} (the wild type) stand alone.
 */
public final class BettaTypes {
    private BettaTypes() {}

    @Nullable
    public static GeneticsData.VarietyDef variety(BettaGenome g) {
        return GeneticsData.get().variety(g);
    }

    /** Full type name. */
    public static MutableComponent name(BettaGenome g) {
        GeneticsData.VarietyDef v = variety(g);
        if (v != null && !v.includeForm()) return varietyName(v);
        return Component.translatable("type.bettacraft.full", form(g), v != null ? varietyName(v) : genericColors(g));
    }

    public static MutableComponent varietyName(GeneticsData.VarietyDef v) {
        return Component.translatableWithFallback(GeneticsData.varietyKey(v.id()), v.name() != null ? v.name() : v.id());
    }

    /** "Halfmoon", "Halfmoon Plakat", "Plakat", "Veiltail Dumbo". */
    public static MutableComponent form(BettaGenome g) {
        GeneticsData d = GeneticsData.get();
        MutableComponent tail = d.name(BettaLoci.FINFORM, g.shape());
        MutableComponent f;
        if (g.isShortFinned()) {
            f = "round".equals(g.shape()) ? Component.translatable("form.bettacraft.plakat") : Component.translatable("form.bettacraft.short", tail);
        } else {
            f = tail;
        }
        return g.isDumbo() ? Component.translatable("gui.bettacraft.dumbo", f) : f;
    }

    /** Colour part without a named variety. */
    public static MutableComponent genericColors(BettaGenome g) {
        GeneticsData d = GeneticsData.get();
        MutableComponent c;
        if (BettaPatterns.NONE.equals(g.pattern())) {
            c = g.bodyColor().equals(g.finColor())
                    ? Component.translatable("type.bettacraft.solid", d.name(BettaLoci.BODY_COLOR, g.bodyColor()))
                    : Component.translatable("type.bettacraft.bicolor", d.name(BettaLoci.BODY_COLOR, g.bodyColor()),
                    d.name(BettaLoci.FIN_COLOR, g.finColor()));
        } else {
            c = Component.translatable("type.bettacraft.patterned", d.name(BettaLoci.BODY_COLOR, g.bodyColor()),
                    d.name(BettaLoci.PATTERN, g.pattern()));
        }
        return "high".equals(g.iridescence()) ? Component.translatable("type.bettacraft.metallic", c) : c;
    }
}
