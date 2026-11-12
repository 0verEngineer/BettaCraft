package org.overengineer.bettacraft.genetics;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Genotype as text (menu, Jade, tooltips): "Fins: Plakat / Veiltail" with the hidden allele greyed out. */
public final class GenotypeText {
    private GenotypeText() {}

    public static Component allele(Locus l, String allele) {
        return Component.translatable(l.key(allele));
    }

    /** "Expressed / hidden" of one locus. */
    public static MutableComponent pair(BettaGenotype g, Locus l) {
        BettaGenotype.AllelePair p = g.pair(l);
        String shown = p.expressed(l), hidden = p.hidden(l);
        MutableComponent c = Component.empty().append(allele(l, shown)).append(" / ");
        c.append(allele(l, hidden).copy().withStyle(p.homozygous() ? ChatFormatting.RESET : ChatFormatting.GRAY));
        return c;
    }

    public static Component line(BettaGenotype g, Locus l) {
        return Component.translatable("gui.bettacraft.genotype.line", Component.translatable(BettaLoci.translationKey(l)), pair(g, l));
    }

    /** Short summary for Jade: shape, body colour and pattern genotypes. */
    public static List<Component> summary(BettaGenotype g) {
        return List.of(line(g, BettaLoci.FINFORM), line(g, BettaLoci.BODY_COLOR), line(g, BettaLoci.PATTERN));
    }
}
