package org.overengineer.bettacraft.config;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M33 part b: {@link FeatureContent} on real items and stacks, for the places that hide what is off: the creative tab, JEI
 * and the handbook. An item of another mod is never hidden. A stack also counts the animals in it: a fish bag of shrimp
 * belongs to the small animals, a bag of cichlids to the cichlids (the bag itself is core).
 */
public final class FeatureGate {
    private FeatureGate() {}

    private static final Map<Item, List<Feature>> ITEMS = new ConcurrentHashMap<>();

    /** The features of an item of ours (empty: core, or another mod's item). */
    public static List<Feature> of(Item item) {
        return ITEMS.computeIfAbsent(item, i -> {
            Identifier id = BuiltInRegistries.ITEM.getKey(i);
            return BettaCraft.MOD_ID.equals(id.getNamespace()) ? FeatureContent.ofItem(id.getPath()) : List.of();
        });
    }

    /** "This is turned off on this server (label)" for the action bar. */
    public static net.minecraft.network.chat.Component offMessage(Feature f) {
        return net.minecraft.network.chat.Component.translatable("message.bettacraft.feature_off_named", net.minecraft.network.chat.Component.translatable(f.translationKey()))
                .withStyle(net.minecraft.ChatFormatting.GOLD);
    }

    /** The item's features are all on. */
    public static boolean item(Item item) {
        return Features.all(of(item));
    }

    /** The stack's item and the animals in it have all their features on. */
    public static boolean stack(ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (!item(stack.getItem())) return false;
        List<CompoundTag> critters = stack.get(ModDataComponents.CRITTERS.get());
        if (critters != null) {
            for (CompoundTag t : critters) {
                CritterSpecies s = CritterSpecies.byIdOrNull(t.getStringOr("species", ""));
                if (s != null && !Features.all(FeatureContent.ofCritter(s))) return false;
            }
        }
        Identifier entity = stack.get(ModDataComponents.BAG_ENTITY.get());
        if (entity != null && BettaCraft.MOD_ID.equals(entity.getNamespace())) {
            if (entity.getPath().equals("betta") && !Features.enabled(Feature.BETTAS)) return false;
            if (entity.getPath().equals("dwarf_frog") && !Features.enabled(Feature.FROGS)) return false;
            TankmateSpecies t = TankmateSpecies.byId(entity.getPath());
            if (t != null && !Features.enabled(t.feature())) return false;
        }
        return true;
    }
}
