package org.overengineer.bettacraft.fabric.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.client.tank.TankWaterRenderer;
import org.overengineer.bettacraft.tank.TankBlock;

/**
 * M52: Fabric's half of NeoForge's TankAwareWaterExtensions: the water of a tank block is drawn by
 * {@link TankWaterRenderer}, implicit water (seagrass, kelp) in the dry interior dimension not at all; every other water
 * goes to the handler that was registered for vanilla water.
 */
final class TankAwareWater implements FluidRenderHandler {
    private final FluidRenderHandler delegate;

    private TankAwareWater(FluidRenderHandler delegate) {
        this.delegate = delegate;
    }

    static void register() {
        FluidRenderHandler water = FluidRenderHandlerRegistry.INSTANCE.get(Fluids.WATER);
        if (water == null || water instanceof TankAwareWater) return;
        TankAwareWater ours = new TankAwareWater(water);
        FluidRenderHandlerRegistry.INSTANCE.register(Fluids.WATER, Fluids.FLOWING_WATER, ours);
    }

    @Override
    public TextureAtlasSprite[] getFluidSprites(@Nullable BlockAndTintGetter view, @Nullable BlockPos pos, FluidState state) {
        return delegate.getFluidSprites(view, pos, state);
    }

    @Override
    public int getFluidColor(@Nullable BlockAndTintGetter view, @Nullable BlockPos pos, FluidState state) {
        return delegate.getFluidColor(view, pos, state);
    }

    @Override
    public void renderFluid(BlockPos pos, BlockAndTintGetter world, VertexConsumer vertexConsumer, BlockState blockState, FluidState fluidState) {
        if (blockState.getBlock() instanceof TankBlock) {
            TankWaterRenderer.render(world, pos, vertexConsumer, blockState);
            return;
        }
        if (!(blockState.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock)) {
            ClientLevel cl = Minecraft.getInstance().level;
            if (cl != null && cl.dimension() == org.overengineer.bettacraft.registry.ModDimensions.TANK_INTERIOR) return;
        }
        delegate.renderFluid(pos, world, vertexConsumer, blockState, fluidState);
    }

    @Override
    public void reloadTextures(TextureAtlas textureAtlas) {
        delegate.reloadTextures(textureAtlas);
    }
}
