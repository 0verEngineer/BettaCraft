package org.overengineer.bettacraft.platform;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import org.overengineer.bettacraft.BettaCraft;

/**
 * A world saved on one loader and opened on the other (a modpack moving from NeoForge to Fabric or back) keeps our entity
 * attachments. Both loaders save an attachment as the same compound under its id, but in different containers: NeoForge
 * under {@value #NEOFORGE}, Fabric under {@value #FABRIC}. Each loader reads only its own and drops the other, so a world
 * built on NeoForge lost every {@code bettacraft:tank_ref} on Fabric: the animals forgot their tanks, the terrarium animals on
 * the land (not adopted again like the swimmers) became free animals and the crested geckos climbed out over the open tops,
 * the menu listed no animals and the mobs in glass lost their data.
 * <p>
 * After an entity is loaded ({@code mixin/EntityAttachmentsMixin}) every attachment of ours it does not have yet is read from
 * the other container (the loader has read its own by then: its own data wins). Only our ids, never another mod's.
 */
public final class CrossLoaderAttachments {
    public static final String NEOFORGE = "neoforge:attachments", FABRIC = "fabric:attachments";

    private CrossLoaderAttachments() {}

    /** Our entries of an attachment container (id -> saved compound). */
    public static Map<String, CompoundTag> ours(CompoundTag container) {
        Map<String, CompoundTag> out = new LinkedHashMap<>();
        String prefix = BettaCraft.MOD_ID + ":";
        for (String key : container.keySet()) {
            if (key.startsWith(prefix)) container.getCompound(key).ifPresent(t -> out.put(key, t));
        }
        return out;
    }

    /** Reads the attachments of ours the entity lacks from either loader's container; returns how many were set. */
    public static int readMissing(Entity e, ValueInput in) {
        int n = 0;
        for (String container : new String[]{NEOFORGE, FABRIC}) {
            var tag = in.read(container, CompoundTag.CODEC);
            if (tag.isEmpty()) continue;
            Map<String, CompoundTag> entries = ours(tag.get());
            if (entries.isEmpty()) continue;
            for (Attachment<?> a : Attachment.all()) {
                CompoundTag saved = entries.get(a.id().toString());
                if (saved != null && a.serializer() != null && !a.has(e) && read(a, e, saved)) n++;
            }
        }
        return n;
    }

    private static <T> boolean read(Attachment<T> a, Entity e, CompoundTag saved) {
        T value = a.serializer().read(saved.copy(), e.registryAccess());
        if (value == null) return false;
        a.set(e, value);
        return true;
    }
}
