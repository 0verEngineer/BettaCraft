package org.overengineer.bettacraft.containers;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.tankmates.TankmateItems;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M55: every animal of the mod has its own icon in its container: a tankmate bucket per species (its own item), and the
 * critter bucket, the fish bag and the transport tub show the animal inside by the item property {@link #PROPERTY} (the
 * number of the animal, 0 for none or one without an own icon: a betta, a clutch, an animal of another mod). The item
 * models (one per animal and container, made by the datagen) and the sprites ({@code tools/assetgen/animal_icons.py}) follow
 * the order of {@link #index}: the tankmates, then the small animals.
 */
public final class AnimalIcons {
    /** The item property of the containers with the animal inside. */
    public static final ResourceLocation PROPERTY = BettaCraft.id("animal");

    /**
     * The small animals whose colour comes from their genes: their sprite has a grey body layer (layer 1) that the item
     * colour tints with the animal's colour (Neocaridina, Caridina, the crayfish, apple snail, ramshorn).
     */
    private static final List<CritterSpecies> TINTED = List.of(CritterSpecies.NEOCARIDINA, CritterSpecies.CARIDINA, CritterSpecies.DWARF_CRAYFISH,
            CritterSpecies.FLORIDA_CRAYFISH, CritterSpecies.APPLE_SNAIL, CritterSpecies.RAMSHORN);

    private AnimalIcons() {}

    /** Item properties are clamped to 0..1: the property is the animal's number divided by this. */
    public static final float SCALE = 1000F;

    /** The threshold of an animal's override (half a step below its value, safe against rounding). */
    public static float threshold(int index) {
        return (index - 0.5F) / SCALE;
    }

    /** The number of a tankmate (1 ..). */
    public static int index(TankmateSpecies s) {
        return s.ordinal() + 1;
    }

    /** The number of a small animal (after the tankmates). */
    public static int index(CritterSpecies s) {
        return TankmateSpecies.values().length + s.ordinal() + 1;
    }

    /** True when the small animal's body is tinted with the colour of its genes (else the sprite has its own colours). */
    public static boolean tintedBody(CritterSpecies s) {
        return TINTED.contains(s);
    }

    /** The texture of an animal's sprite ({@code item/animal/<id>}; a big animal's {@code tub_<id>}). */
    public static String sprite(TankmateSpecies s) {
        return "item/animal/" + (s.isLarge() ? "tub_" : "") + s.id();
    }

    public static String sprite(CritterSpecies s) {
        return "item/animal/" + s.id();
    }

    /** The grey (or coloured) body layer of a small animal's sprite. */
    public static String body(CritterSpecies s) {
        return "item/animal/" + s.id() + "_body";
    }

    /** The small animal in a critter bucket or a fish bag (the first one), or null. */
    @Nullable
    public static CritterSpecies critter(ItemStack stack) {
        return AnimalContainers.critterSpecies(AnimalContainers.critters(stack));
    }

    /**
     * The value of {@link #PROPERTY} for a critter bucket, a fish bag or a transport tub: the number of the animal inside if
     * that container has an own icon of it (divided by {@link #SCALE}), else 0.
     */
    public static float value(ItemStack stack) {
        return number(stack) / SCALE;
    }

    /** The number of the animal inside with an own icon in this container, 0 for none. */
    public static int number(ItemStack stack) {
        if (stack.getItem() instanceof CritterBucketItem) {
            CritterSpecies c = critter(stack);
            return c == null ? 0 : index(c);
        }
        if (stack.getItem() instanceof FishBagItem) {
            if (stack.has(ModDataComponents.TANKMATE_CLUTCH.get()) || stack.has(ModDataComponents.CRITTER_LARVAE.get())) return 0;
            CritterSpecies c = critter(stack);
            if (c != null) return index(c);
            TankmateSpecies t = TankmateItems.bagSpecies(stack);
            return t == null || t.isLarge() ? 0 : index(t);
        }
        if (stack.getItem() instanceof org.overengineer.bettacraft.big.TransportTubItem) {
            var type = org.overengineer.bettacraft.big.TransportTubItem.containedType(stack);
            TankmateSpecies t = type == null ? null : TankmateSpecies.of(type);
            return t == null || !t.isLarge() ? 0 : index(t);
        }
        return 0;
    }
}
