package org.overengineer.bettacraft.mixin.interior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** M53c: the entity sections of a level (the interior clean-up deletes the stored entities of removed regions). */
@Mixin(ServerLevel.class)
public interface ServerLevelEntitiesAccessor {
    @Accessor("entityManager")
    PersistentEntitySectionManager<Entity> bettacraft$entityManager();
}
