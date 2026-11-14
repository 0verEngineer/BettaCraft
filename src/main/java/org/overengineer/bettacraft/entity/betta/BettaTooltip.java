package org.overengineer.bettacraft.entity.betta;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.genetics.BettaLoci;
import org.overengineer.bettacraft.genetics.BettaPatterns;
import org.overengineer.bettacraft.genetics.BettaTypes;
import org.overengineer.bettacraft.genetics.GeneticsData;

/** Text description of a betta (tooltips, menu). Colour names are text, not only colours (accessibility). */
public final class BettaTooltip {
    private BettaTooltip() {}

    public static Component sex(boolean female) {
        return Component.translatable(female ? "gui.bettacraft.female" : "gui.bettacraft.male")
                .withStyle(female ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA);
    }

    public static Component color(String id) {
        return GeneticsData.get().name(BettaLoci.BODY_COLOR, id);
    }

    /** Fin form: "Halfmoon Plakat", "Veiltail Dumbo" ... */
    public static Component shape(BettaGenome g) {
        return BettaTypes.form(g);
    }

    /** Full type name, e.g. "Mustard Gas Halfmoon". */
    public static Component type(BettaGenome g) {
        return BettaTypes.name(g);
    }

    public static Component pattern(BettaGenome g) {
        if (BettaPatterns.NONE.equals(g.pattern())) return GeneticsData.get().name(BettaLoci.PATTERN, BettaPatterns.NONE);
        return Component.translatable("gui.bettacraft.pattern_with_color", GeneticsData.get().name(BettaLoci.PATTERN, g.pattern()),
                color(g.patternColor()));
    }

    public static Component iridescence(BettaGenome g) {
        return Component.translatable("gui.bettacraft.pattern_with_color", GeneticsData.get().name(BettaLoci.IRIDESCENCE, g.iridescence()),
                color(g.iridescenceColor()));
    }

    /** Lines: sex + form, body/fin colour, pattern, iridescence. */
    public static List<Component> lines(BettaData d) {
        BettaGenome g = d.genome();
        return List.of(
                Component.translatable("tooltip.bettacraft.betta.sex_shape", sex(d.female()), shape(g)),
                Component.translatable("tooltip.bettacraft.betta.colors", color(g.bodyColor()), color(g.finColor())),
                Component.translatable("tooltip.bettacraft.betta.pattern", pattern(g)),
                Component.translatable("tooltip.bettacraft.betta.iridescence", "none".equals(g.iridescence())
                        ? GeneticsData.get().name(BettaLoci.IRIDESCENCE, "none") : iridescence(g)));
    }

    public static void append(BettaData d, List<Component> tooltip) {
        tooltip.add(type(d.genome()).copy().withStyle(ChatFormatting.GOLD));
        for (Component c : lines(d)) tooltip.add(c.copy().withStyle(ChatFormatting.GRAY));
        if (!d.isAdult()) tooltip.add(Component.translatable(d.lifeStage().translationKey()).withStyle(ChatFormatting.DARK_AQUA));
        if (d.generation() > 0) {
            tooltip.add(Component.translatable("gui.bettacraft.fish.generation", d.generation()).withStyle(ChatFormatting.DARK_GRAY));
        }
        // M22.3: the genes behind the look, once analysed with the magnifier
        if (org.overengineer.bettacraft.genetics.GeneVisibility.visible(d.analysed())) {
            for (Component c : org.overengineer.bettacraft.genetics.GenotypeText.summary(d.genotype())) tooltip.add(c.copy().withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.bettacraft.genes.unknown").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
