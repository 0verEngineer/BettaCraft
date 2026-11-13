package org.overengineer.bettacraft.critters;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Names and tooltip lines of a fish bag with shrimp, crayfish or snails (M14). */
public final class CritterTooltip {
    /** Lines shown per bag before "... and N more". */
    public static final int LINES = 8;

    private CritterTooltip() {}

    /** The variety when all animals share it, else the species, else "small animals". */
    public static Component bagName(List<CompoundTag> animals) {
        Critter first = Critter.loadAnimal(animals.getFirst(), 0, 0, 0, 0);
        if (first == null) return Component.translatable("critter.bettacraft.unknown");
        boolean sameSpecies = true, sameVariety = true;
        String v = first.genes.variety(first.species);
        for (CompoundTag t : animals) {
            Critter k = Critter.loadAnimal(t, 0, 0, 0, 0);
            if (k == null || k.species != first.species) {
                sameSpecies = false;
                break;
            }
            if (!k.genes.variety(k.species).equals(v)) sameVariety = false;
        }
        if (!sameSpecies) return Component.translatable("critter.bettacraft.mixed");
        return sameVariety ? TankCritters.name(first) : Component.translatable(first.species.translationKey());
    }

    /** One line per animal: sex, variety, grade, young or carrying eggs. */
    public static void append(List<CompoundTag> animals, List<Component> tooltip) {
        int i = 0;
        for (CompoundTag t : animals) {
            if (i++ >= LINES) {
                tooltip.add(Component.translatable("tooltip.bettacraft.critters.more", animals.size() - LINES).withStyle(ChatFormatting.DARK_GRAY));
                break;
            }
            Critter k = Critter.loadAnimal(t, 0, 0, 0, 0);
            if (k == null) continue;
            tooltip.add(line(k).withStyle(ChatFormatting.GRAY));
        }
    }

    public static MutableComponent line(Critter k) {
        String sex = !k.species.hasSexes() ? "⚥ " : k.female ? "♀ " : "♂ ";
        MutableComponent c = Component.literal(sex).append(TankCritters.name(k));
        if (k.species.hasGrade()) c.append(Component.translatable("tooltip.bettacraft.critters.grade", Math.round(k.genes.grade() * 100)));
        if (!k.adult()) c.append(Component.translatable("tooltip.bettacraft.critters.young"));
        if (k.berried()) c.append(Component.translatable("tooltip.bettacraft.critters.eggs"));
        return c;
    }
}
