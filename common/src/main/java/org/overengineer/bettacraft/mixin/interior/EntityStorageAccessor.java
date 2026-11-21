package org.overengineer.bettacraft.mixin.interior;

import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** M53c: the region storage of the stored entities. */
@Mixin(EntityStorage.class)
public interface EntityStorageAccessor {
    @Accessor("simpleRegionStorage")
    SimpleRegionStorage bettacraft$storage();
}
