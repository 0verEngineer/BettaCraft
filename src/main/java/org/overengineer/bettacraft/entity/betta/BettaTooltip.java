package org.overengineer.bettacraft.entity.betta;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.overengineer.bettacraft.genetics.BettaColors;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.genetics.BettaPatterns;

/** Text description of a betta (tooltips, menu). Colour names are text, not only colours (accessibility). */
public final class BettaTooltip {
    private BettaTooltip() {}

    public static Component sex(boolean female) {
        return Component.translatable(female ? "gui.bettacraft.female" : "gui.bettacraft.male")
                .withStyle(female ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA);
    }

    public static Component color(String id) {
        return Component.translatable(BettaColors.translationKey(id));
    }

    public static Component shape(BettaGenome g) {
        MutableComponent c = Component.translatable(BettaPatterns.shapeTranslationKey(g.shape()));
        if (g.isDumbo()) c = Component.translatable("gui.bettacraft.dumbo", c);
        return c;
    }

    public static Component pattern(BettaGenome g) {
        if (BettaPatterns.NONE.equals(g.pattern())) return Component.translatable("pattern.bettacraft.none");
        return Component.translatable("gui.bettacraft.pattern_with_color", Component.translatable(BettaPatterns.translationKey(g.pattern())),
                color(g.patternColor()));
    }

    public static Component iridescence(BettaGenome g) {
        return Component.translatable("iridescence.bettacraft." + g.iridescence());
    }

    /** Lines: sex + shape, body/fin colour, pattern, iridescence. */
    public static List<Component> lines(BettaData d) {
        BettaGenome g = d.genome();
        return List.of(
                Component.translatable("tooltip.bettacraft.betta.sex_shape", sex(d.female()), shape(g)),
                Component.translatable("tooltip.bettacraft.betta.colors", color(g.bodyColor()), color(g.finColor())),
                Component.translatable("tooltip.bettacraft.betta.pattern", pattern(g)),
                Component.translatable("tooltip.bettacraft.betta.iridescence", iridescence(g)));
    }

    public static void append(BettaData d, List<Component> tooltip) {
        for (Component c : lines(d)) tooltip.add(c.copy().withStyle(ChatFormatting.GRAY));
    }
}
