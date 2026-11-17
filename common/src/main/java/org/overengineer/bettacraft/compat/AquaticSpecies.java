package org.overengineer.bettacraft.compat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M16: finds every water animal that is registered (vanilla and every loaded mod) at runtime, with its size, its
 * buckets and whether our tanks take it. Used by the inventory GameTest ({@code -Paquatic}: all water animal mods of the
 * ATM10 pack) and the dev command {@code /bettacraft dev aquatic}.
 * <p>
 * A type counts as a water animal when an instance is a {@link WaterAnimal}, an {@link AbstractFish}, an axolotl or a
 * tadpole, when its spawn category is one of the water categories, when a mob bucket holds it, or when it is a mob that
 * swims with a water or amphibious path navigation (guardians, turtles, sea monsters of other mods).
 */
public final class AquaticSpecies {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/aquatic");

    /** One water animal type as found at runtime. */
    public record Species(EntityType<?> type, ResourceLocation id, String kind, MobCategory category, float width, float height,
                          boolean bucketable, List<Item> buckets, TankInhabitants.Acceptance acceptance) {
        public String line() {
            return String.join("\t", id.toString(), kind, category.getName(), fmt(width) + " x " + fmt(height),
                    bucketable ? "yes" : "no",
                    buckets.isEmpty() ? "-" : String.join(", ", buckets.stream().map(i -> BuiltInRegistries.ITEM.getKey(i).toString()).toList()),
                    acceptance.name());
        }

        public boolean accepted() {
            return acceptance == TankInhabitants.Acceptance.OK;
        }
    }

    private AquaticSpecies() {}

    private static String fmt(float f) {
        return String.format(java.util.Locale.ROOT, "%.2f", f);
    }

    public static boolean waterCategory(MobCategory c) {
        return c == MobCategory.WATER_CREATURE || c == MobCategory.WATER_AMBIENT || c == MobCategory.UNDERGROUND_WATER_CREATURE
                || c == MobCategory.AXOLOTLS;
    }

    /** Mob buckets of every loaded mod by the entity type they hold. */
    public static Map<EntityType<?>, List<Item>> buckets() {
        Map<EntityType<?>, List<Item>> out = new LinkedHashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof MobBucketItem)) continue;
            EntityType<?> t = TankInhabitants.containedType(new ItemStack(item));
            if (t != null) out.computeIfAbsent(t, k -> new ArrayList<>()).add(item);
        }
        return out;
    }

    /** What kind of water animal {@code e} is, or null if it is none. */
    @Nullable
    public static String kind(Entity e, boolean inBucket) {
        if (e instanceof Axolotl) return "axolotl";
        if (e instanceof Tadpole) return "tadpole";
        if (e instanceof AbstractFish) return "fish";
        if (e instanceof WaterAnimal) return "water_animal";
        if (waterCategory(e.getType().getCategory())) return "water_category";
        if (inBucket) return "bucket";
        if (e instanceof Mob m && (m.getNavigation() instanceof WaterBoundPathNavigation || m.getNavigation() instanceof AmphibiousPathNavigation)) {
            return "swimming_mob";
        }
        return null;
    }

    /** Every water animal type, sorted by id. Instances are created (never added to the level) to look at them. */
    public static List<Species> discover(ServerLevel level) {
        Map<EntityType<?>, List<Item>> buckets = buckets();
        List<Species> out = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type.getCategory() == MobCategory.MISC && !buckets.containsKey(type)) continue; // projectiles, items, boats ...
            if (!type.canSummon()) continue;
            // M31: terrarium animals (turtles ...) go into terraria and paludaria by their own rules, not into aquaria
            if (org.overengineer.bettacraft.terrarium.TerrariumAnimals.isTerrariumAnimal(type)) continue;
            Entity e;
            try {
                e = type.create(level);
            } catch (Throwable t) {
                LOGGER.warn("cannot create {}: {}", BuiltInRegistries.ENTITY_TYPE.getKey(type), t.toString());
                continue;
            }
            if (e == null) continue;
            try {
                String kind = kind(e, buckets.containsKey(type));
                if (kind == null) continue;
                out.add(new Species(type, BuiltInRegistries.ENTITY_TYPE.getKey(type), kind, type.getCategory(), e.getBbWidth(),
                        e.getBbHeight(), e instanceof Bucketable, buckets.getOrDefault(type, List.of()), TankInhabitants.accepts(type, e)));
            } catch (Throwable t) {
                LOGGER.warn("cannot inspect {}: {}", BuiltInRegistries.ENTITY_TYPE.getKey(type), t.toString());
            } finally {
                e.discard();
            }
        }
        out.sort(Comparator.comparing(s -> s.id().toString()));
        return out;
    }

    /** Writes the species as a tab separated table (header first) to the log and returns the lines. */
    public static List<String> table(List<Species> species) {
        List<String> lines = new ArrayList<>();
        lines.add("id\tkind\tcategory\thitbox\tbucketable\tbuckets\ttank");
        for (Species s : species) lines.add(s.line());
        return lines;
    }
}
