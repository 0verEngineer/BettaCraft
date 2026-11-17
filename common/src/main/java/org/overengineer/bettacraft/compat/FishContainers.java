package org.overengineer.bettacraft.compat;

import org.overengineer.bettacraft.util.NbtCompat;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Fish containers of other mods (M7), handled purely through item ids and their NBT format, so nothing of those mods
 * is ever loaded or linked:
 * <ul>
 *     <li>Better Fish Tanks {@code betterfishtanks:fish_bag_item}: {@code custom_data.StoredEntity} = the full entity
 *     NBT ({@code saveWithoutId} plus {@code id}). Carries 100 mB like our fish bag (PLAN 4.3).</li>
 *     <li>Fintastic {@code fintastic:fishnet}: {@code custom_data.CreatureData} = the full entity NBT with {@code id}
 *     and {@code OwnerName}. A net carries no water (0 mB).</li>
 * </ul>
 * Both work for putting animals into our tanks and for taking them out through the tank menu.
 */
public final class FishContainers {
    public static final Identifier BFT_FISH_BAG = Identifier.fromNamespaceAndPath("betterfishtanks", "fish_bag_item");
    public static final Identifier FINTASTIC_FISHNET = Identifier.fromNamespaceAndPath("fintastic", "fishnet");
    static final String BFT_KEY = "StoredEntity";
    static final String NET_KEY = "CreatureData";

    /** A container of another mod and the water it carries. */
    public enum Foreign {
        BFT_BAG(BFT_FISH_BAG, BFT_KEY, 100L),
        FISHNET(FINTASTIC_FISHNET, NET_KEY, 0L);

        public final Identifier item;
        final String key;
        public final long waterMb;

        Foreign(Identifier item, String key, long waterMb) {
            this.item = item;
            this.key = key;
            this.waterMb = waterMb;
        }
    }

    private FishContainers() {}

    @Nullable
    public static Foreign kind(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        for (Foreign f : Foreign.values()) if (f.item.equals(id)) return f;
        return null;
    }

    /** The stored entity NBT (with {@code id}) of a filled foreign container, or null. */
    @Nullable
    public static CompoundTag storedEntity(ItemStack stack) {
        Foreign f = kind(stack);
        if (f == null) return null;
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null || !cd.copyTag().contains(f.key)) return null;
        CompoundTag t = cd.copyTag().getCompoundOrEmpty(f.key);
        return NbtCompat.has(t, "id", Tag.TAG_STRING) ? t : null;
    }

    @Nullable
    public static EntityType<?> containedType(ItemStack stack) {
        CompoundTag t = storedEntity(stack);
        return t == null ? null : EntityType.byString(t.getStringOr("id", "")).orElse(null);
    }

    public static boolean isEmptyContainer(ItemStack stack) {
        return kind(stack) != null && storedEntity(stack) == null;
    }

    /** A copy (count 1) without the stored animal. */
    public static ItemStack emptied(ItemStack stack) {
        Foreign f = kind(stack);
        ItemStack out = stack.copyWithCount(1);
        if (f == null) return out;
        CustomData cd = out.get(DataComponents.CUSTOM_DATA);
        if (cd != null) {
            CompoundTag t = cd.copyTag();
            t.remove(f.key);
            if (t.isEmpty()) out.remove(DataComponents.CUSTOM_DATA);
            else out.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        }
        return out;
    }

    /** A copy (count 1) of the empty container {@code empty} holding {@code e} in the format of that mod. */
    public static ItemStack fill(ItemStack empty, Entity e, @Nullable Player player) {
        Foreign f = kind(empty);
        if (f == null) throw new IllegalArgumentException("not a foreign fish container: " + empty);
        ItemStack out = emptied(empty);
        CompoundTag entity = new CompoundTag();
        String id = e.getEncodeId();
        if (id != null) entity.putString("id", id);
        entity.merge(NbtCompat.saveEntityWithoutId(e));
        if (f == Foreign.FISHNET && player != null) entity.putString("OwnerName", player.getName().getString());
        CompoundTag data = out.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.put(f.key, entity);
        out.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return out;
    }

    /**
     * Creates the stored animal (not added to the level). Position, motion and UUID are dropped: the caller places it,
     * and a fresh UUID keeps copies of the same container (creative) from clashing.
     */
    @Nullable
    public static Entity create(ServerLevel level, CompoundTag stored) {
        CompoundTag t = stored.copy();
        t.remove("UUID");
        t.remove("Pos");
        t.remove("Motion");
        t.remove("Passengers");
        t.remove("OwnerName");
        return EntityType.create(NbtCompat.input(level.registryAccess(), t), level, net.minecraft.world.entity.EntitySpawnReason.LOAD).orElse(null);
    }
}
