package org.overengineer.bettacraft.mixin.client;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.client.interior.IrisLightColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * M18: when Iris takes the block ids of a shader pack, the lit aquarium lights get the ids of the vanilla light of
 * their colour ({@link IrisLightColors}), before any chunk is meshed with them. Iris is optional: without it this
 * mixin is skipped ({@link Pseudo}, {@code require = 0}).
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings", remap = false)
public abstract class IrisBlockIdsMixin {
    @ModifyVariable(method = "setBlockStateIds", at = @At("HEAD"), argsOnly = true, require = 0, remap = false)
    private Object2IntMap<BlockState> bettacraft$lightColours(Object2IntMap<BlockState> ids) {
        return IrisLightColors.withLights(ids);
    }
}
