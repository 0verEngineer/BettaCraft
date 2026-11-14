package org.overengineer.bettacraft.containers;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.Salinity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tankmates.TankmateBucketItem;
import org.overengineer.bettacraft.tankmates.TankmateItems;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M22.3: buckets and fish bags with several animals of one species. How many fit depends on the size of the animal: a
 * bucket holds 32 of the smallest (shrimp, mosquito rasboras, neons), 24 of a harlequin rasbora's size, fewer of the bigger
 * fish and 1 of the biggest (a betta, a clown loach: one always fits); a fish bag half of that (rounded down, at least 1),
 * so the bags hold as many small animals as before (16 shrimp, 12 rasboras). Bags of older versions that hold more stay
 * valid; nothing is added to them.
 * <p>
 * Taking an animal out of a tank (menu, net) goes into a container of the player in this order: a bucket or bag of the
 * same species with room, then (buckets) a bucket of water whose water suits the animal (fresh water for freshwater
 * animals, sea water for reef animals; the tank keeps its water), then an empty bucket (1000 mB of tank water) or an
 * empty fish bag (100 mB).
 */
public final class AnimalContainers {
    private AnimalContainers() {}

    /** Where a taken animal goes. */
    public enum Mode {
        /** Into a container of the same species with room (no water). */
        ADD,
        /** A bucket of water that suits the animal (no tank water). */
        WATER_BUCKET,
        /** An empty bucket (1000 mB of tank water). */
        EMPTY_BUCKET,
        /** An empty fish bag (100 mB). */
        EMPTY_BAG
    }

    public record Found(int slot, Mode mode) {
        public long waterMb() {
            return mode == Mode.EMPTY_BUCKET ? TankInhabitants.BUCKET_MB : mode == Mode.EMPTY_BAG ? TankInhabitants.BAG_MB : 0;
        }
    }

    // ------------------------------------------------------------------ capacities

    /** Animals of a tankmate species per bucket (by size; 1 without {@code features.bigContainers}). */
    public static int bucket(TankmateSpecies s) {
        if (!CommonConfig.bigContainers()) return 1;
        double l = s.length();
        return l <= 0.25 ? 32 : l <= 0.4 ? 24 : l <= 0.5 ? 12 : l <= 0.65 ? 6 : l <= 0.75 ? 2 : 1;
    }

    /** Animals of a tankmate species per fish bag (half a bucket, at least 1; 12 without the feature, like before). */
    public static int bag(TankmateSpecies s) {
        if (!CommonConfig.bigContainers()) return TankmateItems.MAX_PER_BAG;
        return Math.max(1, bucket(s) / 2);
    }

    /** Small animals per bucket (0 without the feature: they only go into bags). */
    public static int bucket(CritterSpecies s) {
        if (!CommonConfig.bigContainers()) return 0;
        return s.length() <= 0.35 ? 32 : 24;
    }

    /** Small animals per fish bag (16 without the feature, like before). */
    public static int bag(CritterSpecies s) {
        if (!CommonConfig.bigContainers()) return org.overengineer.bettacraft.critters.TankCritters.MAX_PER_BAG;
        return Math.max(1, bucket(s) / 2);
    }

    // ------------------------------------------------------------------ water

    /** Whether the water of a filled water bucket (sea water or not) suits an animal with this salinity range. */
    public static boolean suits(@Nullable Salinity.Range range, boolean sea) {
        if (range == null) return true;
        return !Salinity.deadly(range, sea ? Salinity.SEA : 0);
    }

    /** A plain or sea water bucket. */
    public static boolean waterBucket(ItemStack s) {
        return s.is(Items.WATER_BUCKET) || s.is(ModItems.SEA_WATER_BUCKET.get());
    }

    public static boolean seaWater(ItemStack s) {
        return s.is(ModItems.SEA_WATER_BUCKET.get());
    }

    // ------------------------------------------------------------------ small animals

    /** The small animals in a bag or a critter bucket (null for anything else or an empty one). */
    @Nullable
    public static List<CompoundTag> critters(ItemStack stack) {
        if (!(stack.getItem() instanceof FishBagItem) && !(stack.getItem() instanceof CritterBucketItem)) return null;
        List<CompoundTag> l = stack.get(ModDataComponents.CRITTERS.get());
        return l == null || l.isEmpty() ? null : l;
    }

    @Nullable
    public static CritterSpecies critterSpecies(List<CompoundTag> animals) {
        if (animals == null || animals.isEmpty()) return null;
        return CritterSpecies.byIdOrNull(animals.getFirst().getString("species"));
    }

    /** A critter bucket with these animals (water of the tank or the world). */
    public static ItemStack critterBucket(List<CompoundTag> animals) {
        ItemStack b = new ItemStack(ModItems.CRITTER_BUCKET.get());
        b.set(ModDataComponents.CRITTERS.get(), List.copyOf(animals));
        return b;
    }

    /** A container of the player for a small animal of {@code s} (bag or bucket), or null. */
    @Nullable
    public static Found findForCritter(Player player, CritterSpecies s, boolean bucket) {
        var inv = player.getInventory();
        int cap = bucket ? bucket(s) : bag(s);
        if (cap <= 0) return null;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (bucket != st.getItem() instanceof CritterBucketItem) continue;
            List<CompoundTag> in = critters(st);
            if (in == null || in.size() >= cap || critterSpecies(in) != s) continue;
            return new Found(i, Mode.ADD);
        }
        if (bucket) {
            var range = org.overengineer.bettacraft.reef.SaltTolerance.critter(s);
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack st = inv.getItem(i);
                if (waterBucket(st) && suits(range, seaWater(st))) return new Found(i, Mode.WATER_BUCKET);
            }
            for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(Items.BUCKET)) return new Found(i, Mode.EMPTY_BUCKET);
            return null;
        }
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (st.getItem() instanceof FishBagItem && !FishBagItem.isFilled(st)) return new Found(i, Mode.EMPTY_BAG);
        }
        return null;
    }

    /** Puts the animal into the container found (the slot gets the filled container). */
    public static void putCritter(Player player, Found f, CompoundTag animal, boolean bucket) {
        var inv = player.getInventory();
        ItemStack st = inv.getItem(f.slot());
        if (f.mode() == Mode.ADD) {
            List<CompoundTag> l = new ArrayList<>(critters(st));
            l.add(animal);
            st.set(ModDataComponents.CRITTERS.get(), List.copyOf(l));
            return;
        }
        ItemStack filled = bucket ? critterBucket(List.of(animal)) : org.overengineer.bettacraft.critters.TankCritters.bag(List.of(animal));
        give(player, f.slot(), filled);
    }

    /** Replaces one item of the stack in {@code slot} by {@code filled} (creative keeps the empty one). */
    public static void give(Player player, int slot, ItemStack filled) {
        var inv = player.getInventory();
        ItemStack st = inv.getItem(slot);
        if (player.getAbilities().instabuild && !waterBucket(st)) {
            if (!inv.add(filled)) player.drop(filled, false);
            return;
        }
        if (st.getCount() <= 1) {
            inv.setItem(slot, filled);
        } else {
            st.shrink(1);
            if (!inv.add(filled)) player.drop(filled, false);
        }
    }

    // ------------------------------------------------------------------ tankmates

    /** Fish (bucket data) in a tankmate bucket or bag of one species. */
    public static List<CompoundTag> fish(ItemStack stack) {
        return TankmateItems.bagFish(stack);
    }

    /** Species of the tankmates in a bucket or a bag, or null. */
    @Nullable
    public static TankmateSpecies tankmateSpecies(ItemStack stack) {
        if (stack.getItem() instanceof TankmateBucketItem b) return b.species();
        if (stack.getItem() instanceof FishBagItem) return TankmateItems.bagSpecies(stack);
        return null;
    }

    /** A bucket of water with these fish of one species (the first one also as the vanilla bucket data). */
    public static ItemStack tankmateBucket(TankmateSpecies s, List<CompoundTag> fish) {
        ItemStack b = new ItemStack(ModItems.TANKMATE_BUCKETS.get(s).get());
        b.set(DataComponents.BUCKET_ENTITY_DATA, CustomData.of(fish.getFirst()));
        if (fish.size() > 1) b.set(ModDataComponents.TANKMATE_GROUP.get(), List.copyOf(fish));
        return b;
    }

    /** The same kind of container with these fish (bag or bucket). */
    public static ItemStack withFish(ItemStack container, TankmateSpecies s, List<CompoundTag> fish) {
        return container.getItem() instanceof TankmateBucketItem ? tankmateBucket(s, fish) : TankmateItems.groupBag(s, fish);
    }

    /**
     * A container for a tankmate of {@code s} (or, with {@code s} null, a betta or a frog: no adding): same species with
     * room, a water bucket that suits it, an empty bucket; for bags a bag of the same species with room or an empty bag.
     */
    @Nullable
    public static Found findForFish(Player player, @Nullable TankmateSpecies s, @Nullable Salinity.Range range, boolean bucket) {
        var inv = player.getInventory();
        if (s != null) {
            int cap = bucket ? bucket(s) : bag(s);
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack st = inv.getItem(i);
                if (bucket ? !(st.getItem() instanceof TankmateBucketItem) : !(st.getItem() instanceof FishBagItem)) continue;
                if (tankmateSpecies(st) != s) continue;
                if (st.has(ModDataComponents.TANKMATE_CLUTCH.get())) continue;
                List<CompoundTag> in = fish(st);
                if (in.isEmpty() || in.size() >= cap) continue;
                return new Found(i, Mode.ADD);
            }
        }
        if (!bucket) return null; // empty bags and the containers of other mods: TankInhabitants.findContainer
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (waterBucket(st) && suits(range, seaWater(st))) return new Found(i, Mode.WATER_BUCKET);
        }
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(Items.BUCKET)) return new Found(i, Mode.EMPTY_BUCKET);
        return null;
    }

    /** Adds the fish (its bucket data) to the container in the slot ({@link Mode#ADD}). */
    public static void addFish(Player player, int slot, TankmateSpecies s, CompoundTag fishData) {
        var inv = player.getInventory();
        ItemStack st = inv.getItem(slot);
        List<CompoundTag> l = new ArrayList<>(fish(st));
        l.add(fishData);
        ItemStack out = withFish(st, s, l);
        inv.setItem(slot, out);
    }

    /** The entity type id of a tankmate species (bag component). */
    public static net.minecraft.resources.ResourceLocation typeId(TankmateSpecies s) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(ModEntities.TANKMATES.get(s).get());
    }

    /** Loads the critters of a list (unknown species are skipped). */
    public static List<Critter> load(List<CompoundTag> animals) {
        List<Critter> out = new ArrayList<>();
        for (CompoundTag t : animals) {
            Critter k = Critter.loadAnimal(t, 0, 0, 0, 0);
            if (k != null) out.add(k);
        }
        return out;
    }
}
