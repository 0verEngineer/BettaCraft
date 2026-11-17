package org.overengineer.bettacraft.compat;

import org.overengineer.bettacraft.util.NbtCompat;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * M31 part d (question C1c): the mob capture items of other mods, read through their item and data component ids only
 * (no compile dependency; the component value is written to NBT with its own codec):
 * <ul>
 *     <li>EnderIO {@code enderio:soul_vial}: component {@code enderio:soul} = {@code {entity_type: "minecraft:zombie",
 *     entity_tag: {id: "minecraft:zombie", ...the entity}}} (older saves {@code {entityTag: {...}}}); the empty vial is the
 *     same item without the component.</li>
 *     <li>Industrial Foregoing {@code industrialforegoing:mob_imprisonment_tool}: component
 *     {@code industrialforegoing:mob_imprisonment} = the entity ({@code saveWithoutId}) with its type in {@code entity}.</li>
 *     <li>any item in {@code #bettacraft:mob_capture_items}: the first compound in its components (custom data included) that
 *     names an entity type in {@code id} (or {@code entity}, {@code EntityTag}, {@code StoredEntity} ...).</li>
 * </ul>
 * Right-click a tank with a full one: a mob of {@link org.overengineer.bettacraft.mobs.GlassMobSpecies} goes in (it
 * shrinks there) and the item is given back empty. {@link #parse} is pure (unit-tested with the formats of the jars).
 */
public final class MobCaptures {
    public static final Identifier SOUL_VIAL = Identifier.fromNamespaceAndPath("enderio", "soul_vial");
    public static final Identifier SOUL = Identifier.fromNamespaceAndPath("enderio", "soul");
    public static final Identifier IMPRISONMENT_TOOL = Identifier.fromNamespaceAndPath("industrialforegoing", "mob_imprisonment_tool");
    public static final Identifier IMPRISONMENT = Identifier.fromNamespaceAndPath("industrialforegoing", "mob_imprisonment");

    /** The formats. */
    public enum Format { SOUL_VIAL, IMPRISONMENT, GENERIC }

    private MobCaptures() {}

    @Nullable
    public static Format format(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (SOUL_VIAL.equals(id)) return Format.SOUL_VIAL;
        if (IMPRISONMENT_TOOL.equals(id)) return Format.IMPRISONMENT;
        if (stack.is(ModTags.MOB_CAPTURE_ITEMS)) return Format.GENERIC;
        return null;
    }

    /** The captured entity (with {@code id}) of a full capture item of another mod, or null (empty, or no such item). */
    @Nullable
    public static CompoundTag storedEntity(ItemStack stack) {
        Format f = format(stack);
        if (f == null) return null;
        try {
            for (TypedDataComponent<?> c : stack.getComponents()) {
                Identifier type = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(c.type());
                if (f == Format.SOUL_VIAL && !SOUL.equals(type) || f == Format.IMPRISONMENT && !IMPRISONMENT.equals(type)) continue;
                Tag t = encode(c);
                CompoundTag e = t == null ? null : parse(f, t);
                if (e != null) return e;
            }
        } catch (RuntimeException ex) {
            return null; // a format we do not know: no animal
        }
        return null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <T> Tag encode(TypedDataComponent<T> c) {
        DataComponentType<T> type = c.type();
        if (type.codec() == null) return null;
        return type.codec().encodeStart(NbtOps.INSTANCE, c.value()).result().orElse(null);
    }

    @Nullable
    public static EntityType<?> containedType(ItemStack stack) {
        CompoundTag t = storedEntity(stack);
        return t == null ? null : EntityType.byString(t.getStringOr("id", "")).orElse(null);
    }

    /** The item given back after its mob went into a tank: the same item, empty. */
    public static ItemStack emptied(ItemStack stack) {
        return new ItemStack(stack.getItem());
    }

    /**
     * The entity data (with {@code id}) in the NBT of a capture component, or null. Pure: the formats of
     * {@link Format}; a GENERIC one is searched for a compound naming an entity (depth at most 3).
     */
    @Nullable
    public static CompoundTag parse(Format f, Tag t) {
        if (!(t instanceof CompoundTag c)) return null;
        switch (f) {
            case SOUL_VIAL -> {
                for (String k : new String[]{"entity_tag", "entityTag"}) {
                    if (NbtCompat.has(c, k, Tag.TAG_COMPOUND)) {
                        CompoundTag e = c.getCompoundOrEmpty(k).copy();
                        if (!NbtCompat.has(e, "id", Tag.TAG_STRING) && NbtCompat.has(c, "entity_type", Tag.TAG_STRING)) e.putString("id", c.getStringOr("entity_type", ""));
                        return NbtCompat.has(e, "id", Tag.TAG_STRING) ? e : null;
                    }
                }
                return null;
            }
            case IMPRISONMENT -> {
                return named(c);
            }
            default -> {
                return search(c, 0);
            }
        }
    }

    /** The compound as entity data if it names its type in {@code id} or {@code entity}. */
    @Nullable
    private static CompoundTag named(CompoundTag c) {
        if (NbtCompat.has(c, "id", Tag.TAG_STRING) && Identifier.tryParse(c.getStringOr("id", "")) != null && looksLikeEntity(c)) return c.copy();
        if (NbtCompat.has(c, "entity", Tag.TAG_STRING) && Identifier.tryParse(c.getStringOr("entity", "")) != null) {
            CompoundTag e = c.copy();
            e.putString("id", e.getStringOr("entity", ""));
            e.remove("entity");
            return e;
        }
        return null;
    }

    /** An entity has health or a position or a UUID; an item stack ({@code id} + {@code count}) has none of these. */
    private static boolean looksLikeEntity(CompoundTag c) {
        return c.contains("Health") || c.contains("Pos") || c.contains("UUID") || c.contains("Attributes");
    }

    @Nullable
    private static CompoundTag search(CompoundTag c, int depth) {
        CompoundTag self = named(c);
        if (self != null) return self;
        if (depth >= 3) return null;
        for (String k : c.keySet()) {
            if (c.get(k) instanceof CompoundTag sub) {
                CompoundTag found = search(sub, depth + 1);
                if (found != null) return found;
            }
        }
        return null;
    }
}
