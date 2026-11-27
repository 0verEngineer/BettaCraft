package org.overengineer.bettacraft.platform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.overengineer.bettacraft.BettaCraft;

/**
 * A world saved on one loader and opened on the other (a modpack moving from NeoForge to Fabric or back) keeps our entity
 * attachments. Both loaders save an attachment as the same compound under its id, but in different containers: NeoForge
 * under {@value #NEOFORGE}, Fabric under {@value #FABRIC}. Each loader reads only its own and drops the other, so a world
 * built on NeoForge lost every {@code bettacraft:tank_ref} on Fabric: the animals forgot their tanks, the terrarium animals on
 * the land (not adopted again like the swimmers) became free animals and the crested geckos climbed out over the open tops,
 * the menu listed no animals and the mobs in glass lost their data.
 * <p>
 * Before an entity is loaded ({@code mixin/EntityAttachmentsMixin}) our entries of each container are copied into the other
 * one where it has none of its own; the loader then reads them from its own container. Only our ids, never another mod's.
 */
public final class CrossLoaderAttachments {
    public static final String NEOFORGE = "neoforge:attachments", FABRIC = "fabric:attachments";

    private CrossLoaderAttachments() {}

    /** Copies our attachments between the two containers of an entity's saved tag (in place); returns how many were copied. */
    public static int merge(CompoundTag entity) {
        return copy(entity, NEOFORGE, FABRIC) + copy(entity, FABRIC, NEOFORGE);
    }

    private static int copy(CompoundTag entity, String from, String to) {
        if (!entity.contains(from, Tag.TAG_COMPOUND)) return 0;
        CompoundTag src = entity.getCompound(from);
        CompoundTag dst = entity.contains(to, Tag.TAG_COMPOUND) ? entity.getCompound(to) : null;
        int n = 0;
        String prefix = BettaCraft.MOD_ID + ":";
        for (String key : src.getAllKeys()) {
            if (!key.startsWith(prefix) || dst != null && dst.contains(key)) continue;
            if (dst == null) {
                dst = new CompoundTag();
                entity.put(to, dst);
            }
            Tag t = src.get(key);
            if (t != null) {
                dst.put(key, t.copy());
                n++;
            }
        }
        return n;
    }
}
