package org.overengineer.bettacraft.client.tank;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.lang.reflect.Field;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.ClientExtensionsManager;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.joml.Vector4f;
import org.overengineer.bettacraft.tank.TankBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delegating wrapper around the client extensions NeoForge registered for vanilla water. Only
 * {@link #renderFluid} differs: water inside a {@link TankBlock} is drawn by {@link TankWaterRenderer}; every other
 * water block is passed to the original unchanged.
 */
public final class TankAwareWaterExtensions implements IClientFluidTypeExtensions {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/water");

    private final IClientFluidTypeExtensions delegate;

    private TankAwareWaterExtensions(IClientFluidTypeExtensions delegate) {
        this.delegate = delegate;
    }

    /**
     * Replaces the entry for {@code NeoForgeMod.WATER_TYPE} in {@code ClientExtensionsManager.FLUID_TYPE_EXTENSIONS}
     * (package-private static map, filled in {@code ClientHooks.initClientHooks} during Minecraft construction, i.e.
     * before FMLClientSetupEvent). Accessed via reflection; on failure we log and vanilla rendering stays active.
     */
    @SuppressWarnings("unchecked")
    public static void install() {
        try {
            Field f = ClientExtensionsManager.class.getDeclaredField("FLUID_TYPE_EXTENSIONS");
            f.setAccessible(true);
            Map<FluidType, IClientFluidTypeExtensions> map = (Map<FluidType, IClientFluidTypeExtensions>) f.get(null);
            FluidType water = NeoForgeMod.WATER_TYPE.value();
            IClientFluidTypeExtensions current = map.get(water);
            if (current == null) {
                LOGGER.error("No client extensions registered for water; tank water renderer NOT installed");
                return;
            }
            if (current instanceof TankAwareWaterExtensions) return;
            map.put(water, new TankAwareWaterExtensions(current));
            LOGGER.info("Installed tank water renderer (wrapping {})", current.getClass().getName());
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.error("Could not install tank water renderer; tanks will show vanilla water", e);
        }
    }

    @Override
    public boolean renderFluid(FluidState fluidState, BlockAndTintGetter getter, BlockPos pos, VertexConsumer vertexConsumer, BlockState blockState) {
        if (blockState.getBlock() instanceof TankBlock) {
            TankWaterRenderer.render(getter, pos, vertexConsumer, blockState);
            return true;
        }
        // Dry interior (PLAN 7.3): seagrass/kelp report implicit water; inside the interior dimension it is not drawn.
        if (!(blockState.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock)) {
            ClientLevel cl = Minecraft.getInstance().level;
            if (cl != null && cl.dimension() == org.overengineer.bettacraft.registry.ModDimensions.TANK_INTERIOR) return true;
        }
        return delegate.renderFluid(fluidState, getter, pos, vertexConsumer, blockState);
    }

    // ---- everything else is delegated unchanged ----

    @Override public int getTintColor() { return delegate.getTintColor(); }
    @Override public Identifier getStillTexture() { return delegate.getStillTexture(); }
    @Override public Identifier getFlowingTexture() { return delegate.getFlowingTexture(); }
    @Override public Identifier getOverlayTexture() { return delegate.getOverlayTexture(); }
    @Override public Identifier getRenderOverlayTexture(Minecraft mc) { return delegate.getRenderOverlayTexture(mc); }
    @Override public void renderOverlay(Minecraft mc, PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffers) { delegate.renderOverlay(mc, poseStack, buffers); }

    @Override
    public Vector4f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector4f fluidFogColor) {
        return delegate.modifyFogColor(camera, partialTick, level, renderDistance, darkenWorldAmount, fluidFogColor);
    }

    @Override
    public void modifyFogRender(Camera camera, @org.jetbrains.annotations.Nullable FogEnvironment environment, float renderDistance, float partialTick, FogData fogData) {
        delegate.modifyFogRender(camera, environment, renderDistance, partialTick, fogData);
    }

    @Override public Identifier getStillTexture(FluidState state, BlockAndTintGetter getter, BlockPos pos) { return delegate.getStillTexture(state, getter, pos); }
    @Override public Identifier getFlowingTexture(FluidState state, BlockAndTintGetter getter, BlockPos pos) { return delegate.getFlowingTexture(state, getter, pos); }
    @Override public Identifier getOverlayTexture(FluidState state, BlockAndTintGetter getter, BlockPos pos) { return delegate.getOverlayTexture(state, getter, pos); }
    @Override public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) { return delegate.getTintColor(state, getter, pos); }
    @Override public int getTintColor(FluidStack stack) { return delegate.getTintColor(stack); }
    @Override public Identifier getStillTexture(FluidStack stack) { return delegate.getStillTexture(stack); }
    @Override public Identifier getFlowingTexture(FluidStack stack) { return delegate.getFlowingTexture(stack); }
    @Override public Identifier getOverlayTexture(FluidStack stack) { return delegate.getOverlayTexture(stack); }
}
