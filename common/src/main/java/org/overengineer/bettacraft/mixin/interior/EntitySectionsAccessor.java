package org.overengineer.bettacraft.mixin.interior;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** M53c: which entity chunks are loaded or waiting to be saved, and their storage. */
@Mixin(PersistentEntitySectionManager.class)
public interface EntitySectionsAccessor {
    @Accessor("permanentStorage")
    EntityPersistentStorage<?> bettacraft$permanentStorage();

    @Accessor("chunkLoadStatuses")
    Long2ObjectMap<?> bettacraft$chunkLoadStatuses();

    @Accessor("chunksToUnload")
    LongSet bettacraft$chunksToUnload();
}
