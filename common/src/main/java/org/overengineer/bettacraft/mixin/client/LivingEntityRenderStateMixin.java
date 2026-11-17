package org.overengineer.bettacraft.mixin.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.client.render.GlassMobRendering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** M52b: the wall a spider in glass lies on ({@link GlassMobRendering}), carried by its render state. */
@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateMixin implements GlassMobRendering.GlassState {
    @Unique
    private @Nullable Direction bettacraft$wall;

    @Override
    public @Nullable Direction bettacraft$glassWall() {
        return bettacraft$wall;
    }

    @Override
    public void bettacraft$setGlassWall(@Nullable Direction wall) {
        bettacraft$wall = wall;
    }
}
