package org.overengineer.bettacraft.genetics.livebearer;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * M21.6: guppy and endler genetics as text (menu, tooltips, info overlays): the strain name (or a hybrid, or a short
 * description for unnamed fish) and one line per locus "Tail: Delta / Round" with the hidden allele greyed out.
 */
public final class LivebearerText {
    private LivebearerText() {}

    public static MutableComponent allele(LbLocus l, String allele) {
        return Component.translatable("lb.bettacraft." + l.id() + "." + allele);
    }

    public static MutableComponent pattern(String allele) {
        return Component.translatable("lb.bettacraft.pattern." + allele);
    }

    public static MutableComponent body(String body) {
        return Component.translatable("lb.bettacraft.body." + body);
    }

    /** Strain name, "Endler × Guppy hybrid (50 % endler)", or "Delta, red tail" for an unnamed fish. */
    public static MutableComponent name(LbPhenotype p) {
        if (p.hybrid()) {
            MutableComponent h = Component.translatable("gui.bettacraft.lb.hybrid", Math.round(p.endler() * 100));
            if (!p.y().equals("none") && !p.y().equals("wild")) h.append(" · ").append(pattern(p.y()));
            return h;
        }
        LivebearerStrain s = LivebearerStrain.of(p);
        if (s != null) return Component.translatable(s.translationKey());
        return Component.translatable("gui.bettacraft.lb.unnamed", allele(LbLocus.TAIL, p.tail()), allele(LbLocus.TAIL_COLOR, p.tailColor()));
    }

    private static MutableComponent pair(LivebearerGenome g, LbLocus l) {
        String shown = g.expressed(l), hidden = g.hidden(l);
        // homozygous: the allele once ("pure"), else shown / hidden (grey)
        if (g.homozygous(l)) return Component.translatable("gui.bettacraft.lb.pure", allele(l, shown));
        return Component.empty().append(allele(l, shown)).append(" / ").append(allele(l, hidden).withStyle(ChatFormatting.GRAY));
    }

    private static Component line(String key, Component value) {
        return Component.translatable("gui.bettacraft.genotype.line", Component.translatable(key), value);
    }

    /** One line per locus, the sex chromosomes and the endler share (menu tooltip). */
    public static List<Component> lines(LivebearerGenome g, boolean female) {
        List<Component> out = new ArrayList<>();
        for (LbLocus l : LbLocus.values()) out.add(line("lb.bettacraft.locus." + l.id(), pair(g, l)));
        MutableComponent x = Component.empty().append(pattern(g.xa));
        if (female && !g.xa.equals(g.xb)) x.append(" / ").append(pattern(g.xb).withStyle(ChatFormatting.GRAY));
        out.add(line("lb.bettacraft.locus.x", x));
        if (!female) out.add(line("lb.bettacraft.locus.y", pattern(g.y)));
        out.add(Component.translatable("gui.bettacraft.lb.endler", Math.round(g.endler * 100)).withStyle(ChatFormatting.GRAY));
        return out;
    }

    /** Body colour and the patterns that show (short summary for overlays and tooltips). */
    public static Component summary(LbPhenotype p) {
        MutableComponent c = body(p.body());
        if (!p.x().equals("none")) c.append(", ").append(pattern(p.x()));
        if (!p.y().equals("none") && !p.y().equals(p.x())) c.append(", ").append(pattern(p.y()));
        c.append(", ").append(allele(LbLocus.TAIL, p.tail()));
        return c;
    }
}
