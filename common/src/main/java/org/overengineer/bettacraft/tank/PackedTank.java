package org.overengineer.bettacraft.tank;

import org.overengineer.bettacraft.util.NbtCompat;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.overengineer.bettacraft.registry.ModBlockEntities;

/**
 * M32 part c: a tank item that carries a whole tank as data (its controller's block entity data with the packed
 * animals), made when a mover could not put a travelling tank down and dropped it as an item (Create: the target block
 * was unbreakable). Placed again, the block entity loads that data and arrives like a moved tank ({@link TankMoves}):
 * same tank, interior region, water and animals; the blocks that are not there count as broken (their interior comes out
 * as items), so the tank stands as the one placed block. Stored in the vanilla {@code block_entity_data} component, which
 * {@link BlockItem} loads into the placed block entity by itself.
 */
public final class PackedTank {
    /**
     * Only data we packed counts (a creative pick-block copy of a standing tank has its data too, but must never become a
     * second tank with the same id and interior region).
     */
    private static final String MARK = "bettacraft_packed";

    /**
     * M42 part b: the id of one packing. A copy of the item (a creative copy, another mod's dupe) carries the same one; only the
     * first item placed with it brings the tank back, any other becomes a plain tank block (else the small animals, the water and
     * the interior region would exist twice, and the animals again once the first ones were taken out).
     */
    private static final String TOKEN = "bettacraft_packed_token";

    private PackedTank() {}

    /**
     * M42 part b: claims the packing of this item for the placement now: true the first time (the tank comes back), false for a
     * copy of an item placed before. An item of an older version (no packing id) goes by its tank id.
     */
    public static boolean claim(net.minecraft.server.level.ServerLevel level, ItemStack stack) {
        CompoundTag t = data(stack);
        if (t == null) return false;
        java.util.UUID token = NbtCompat.hasUUID(t, TOKEN) ? NbtCompat.getUUID(t, TOKEN) : NbtCompat.hasUUID(t.getCompoundOrEmpty("tank"), "id") ? NbtCompat.getUUID(t.getCompoundOrEmpty("tank"), "id") : null;
        return token == null || Claims.get(level.getServer()).claim(token);
    }

    /** The packings placed so far (saved data of the overworld). */
    static final class Claims extends net.minecraft.world.level.saveddata.SavedData {
        static final String NAME = "bettacraft_packed_tanks";
        private final java.util.Set<java.util.UUID> used = new java.util.HashSet<>();

        static Claims get(net.minecraft.server.MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(org.overengineer.bettacraft.util.NbtCompat.savedData(NAME, Claims::new, Claims::load, Claims::save));
        }

        boolean claim(java.util.UUID token) {
            if (!used.add(token)) return false;
            setDirty();
            return true;
        }

        public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
            for (java.util.UUID u : used) list.add(org.overengineer.bettacraft.util.NbtCompat.createUUID(u));
            tag.put("used", list);
            return tag;
        }

        private static Claims load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            Claims c = new Claims();
            for (Tag u : tag.getListOrEmpty("used")) c.used.add(org.overengineer.bettacraft.util.NbtCompat.loadUUID(u));
            return c;
        }
    }

    /** Puts the controller's data (as the mover held it) into a tank item, with its glass and style. */
    public static ItemStack pack(ItemStack stack, CompoundTag controllerData) {
        CompoundTag data = controllerData.copy();
        data.remove("x");
        data.remove("y");
        data.remove("z");
        data.remove("id");
        data.putBoolean(MARK, true);
        NbtCompat.putUUID(data, TOKEN, java.util.UUID.randomUUID()); // M42 part b: the item is placed once
        stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.TypedEntityData.of(ModBlockEntities.TANK.get(), data));
        return restyle(stack, data);
    }

    /** Gives a tank item the glass and style of a tank block's data (a mover's drop has neither). */
    public static ItemStack restyle(ItemStack stack, CompoundTag data) {
        Identifier glass = Identifier.tryParse(data.getStringOr("glass", ""));
        if (glass != null && !data.getStringOr("glass", "").isEmpty()) TankBlockItem.setGlass(stack, glass);
        if (NbtCompat.has(data, "style", Tag.TAG_STRING)) TankBlockItem.setStyle(stack, TankStyle.byId(data.getStringOr("style", "")));
        return stack;
    }

    /** The tank data the item carries, or null (a plain tank item). */
    @Nullable
    public static CompoundTag data(ItemStack stack) {
        var d = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (d == null) return null;
        CompoundTag t = d.copyTagWithoutId();
        return t.getBooleanOr(MARK, false) && NbtCompat.has(t, "tank", Tag.TAG_COMPOUND) ? t : null;
    }

    public static boolean isPacked(ItemStack stack) {
        return data(stack) != null;
    }

    /** How many animals travel packed in the item. */
    public static int animals(ItemStack stack) {
        CompoundTag t = data(stack);
        return t == null ? 0 : t.getListOrEmpty("stowed").size();
    }
}
