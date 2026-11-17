package org.overengineer.bettacraft.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** M52: instead of reflection by name (names differ in a Fabric production run): an entity counts as in water. */
@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("wasTouchingWater")
    void bettacraft$setWasTouchingWater(boolean value);
}
