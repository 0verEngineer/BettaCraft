package org.overengineer.bettacraft.mixin.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.overengineer.bettacraft.client.tank.TankWaterRenderer;
import org.overengineer.bettacraft.tank.TankBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * M63 (26.x): Sodium meshes fluids with its own renderer and calls neither the vanilla {@code FluidRenderer} (where
 * {@link FluidRendererMixin} draws the tank water) nor a hook of ours, so with Sodium (and Iris) the tank water was vanilla
 * water: the flowing texture on the sides, a sloped surface, and with a shader pack the model's water on top of it. Here the
 * water of a tank block is drawn by {@link TankWaterRenderer} into Sodium's translucent buffer (the vertex consumer Sodium
 * gives the custom fluid renderers of the loaders) and Sodium's own water is left out. Sodium is optional: the mixin is
 * skipped without it ({@link Pseudo}, {@code require = 0}); its types are only reached by reflection.
 */
@Pseudo
@Mixin(targets = {"net.caffeinemc.mods.sodium.neoforge.render.FluidRendererImpl", "net.caffeinemc.mods.sodium.fabric.render.FluidRendererImpl"}, remap = false)
public abstract class SodiumFluidRendererMixin {
    private static volatile MethodHandle bettacraft$material, bettacraft$builder, bettacraft$consumer;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void bettacraft$tankWater(@Coerce Object slice, BlockState blockState, FluidState fluidState, BlockPos pos, BlockPos offset,
            @Coerce Object collector, @Coerce Object buffers, CallbackInfo ci) {
        if (!(blockState.getBlock() instanceof TankBlock)) return;
        ci.cancel();
        try {
            if (bettacraft$consumer == null) bind(buffers);
            Object material = bettacraft$material.invoke(ChunkSectionLayer.TRANSLUCENT);
            Object builder = bettacraft$builder.invoke(buffers, material);
            VertexConsumer vc = (VertexConsumer) bettacraft$consumer.invoke(builder, material, collector);
            TankWaterRenderer.render((BlockAndTintGetter) slice, pos, vc, blockState);
        } catch (Throwable t) {
            org.overengineer.bettacraft.debug.LogArea.RENDER.warnThrottled("sodium_fluid", "Tank water could not be drawn through Sodium: {}", t.toString());
        }
    }

    private static synchronized void bind(Object buffers) throws ReflectiveOperationException {
        if (bettacraft$consumer != null) return;
        ClassLoader cl = buffers.getClass().getClassLoader();
        MethodHandles.Lookup l = MethodHandles.publicLookup();
        Class<?> materials = Class.forName("net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials", false, cl);
        Class<?> material = Class.forName("net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material", false, cl);
        Class<?> builder = Class.forName("net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder", false, cl);
        Class<?> collector = Class.forName("net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector", false, cl);
        Class<?> buffersClass = Class.forName("net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers", false, cl);
        bettacraft$material = l.findStatic(materials, "forChunkLayer", MethodType.methodType(material, ChunkSectionLayer.class))
                .asType(MethodType.methodType(Object.class, ChunkSectionLayer.class));
        bettacraft$builder = l.findVirtual(buffersClass, "get", MethodType.methodType(builder, material))
                .asType(MethodType.methodType(Object.class, Object.class, Object.class));
        bettacraft$consumer = l.findVirtual(builder, "asFallbackVertexConsumer", MethodType.methodType(VertexConsumer.class, material, collector))
                .asType(MethodType.methodType(VertexConsumer.class, Object.class, Object.class, Object.class));
    }
}
