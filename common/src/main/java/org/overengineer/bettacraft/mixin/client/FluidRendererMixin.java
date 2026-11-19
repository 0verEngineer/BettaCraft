package org.overengineer.bettacraft.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.overengineer.bettacraft.client.tank.TankWaterRenderer;
import org.overengineer.bettacraft.registry.ModDimensions;
import org.overengineer.bettacraft.tank.TankBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * M52c (26.1): the water of a tank block is drawn by {@link TankWaterRenderer}, every other water as before; inside the
 * interior dimension the implicit water of seagrass and kelp is not drawn (dry interior). One mixin for both loaders (1.21.x:
 * NeoForge's {@code IClientFluidTypeExtensions#renderFluid}, gone in 26.1, and Fabric's fluid render handler).
 */
@Mixin(FluidRenderer.class)
public abstract class FluidRendererMixin {
    @Inject(method = "tesselate", at = @At("HEAD"), cancellable = true)
    private void bettacraft$tankWater(BlockAndTintGetter level, BlockPos pos, FluidRenderer.Output output, BlockState blockState, FluidState fluidState,
            CallbackInfo ci) {
        if (blockState.getBlock() instanceof TankBlock) {
            TankWaterRenderer.render(level, pos, output.getBuilder(ChunkSectionLayer.TRANSLUCENT), blockState);
            ci.cancel();
            return;
        }
        if (!(blockState.getBlock() instanceof LiquidBlock)) {
            ClientLevel cl = Minecraft.getInstance().level;
            if (cl != null && cl.dimension() == ModDimensions.TANK_INTERIOR) ci.cancel();
        }
    }
}
