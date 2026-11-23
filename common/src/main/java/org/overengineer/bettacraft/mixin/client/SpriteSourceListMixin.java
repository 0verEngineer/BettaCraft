package org.overengineer.bettacraft.mixin.client;

import java.util.List;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.overengineer.bettacraft.client.tank.GlassVariants;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * M64: the block atlas gets the frameless copies of the glass textures ({@link GlassVariants}) for the connected glass of the
 * textured tanks. Added to the sources read from the atlas files (after them: a resource pack's glass is the one copied).
 */
@Mixin(SpriteSourceList.class)
public abstract class SpriteSourceListMixin {
    @Shadow
    @Final
    private List<SpriteSource> sources;

    @Inject(method = "load", at = @At("RETURN"))
    private static void bettacraft$glassVariants(ResourceManager resourceManager, ResourceLocation sprite, CallbackInfoReturnable<SpriteSourceList> cir) {
        if (!GlassVariants.BLOCK_ATLAS.equals(sprite)) return;
        ((SpriteSourceListMixin) (Object) cir.getReturnValue()).sources.add(GlassVariants.INSTANCE);
    }
}
