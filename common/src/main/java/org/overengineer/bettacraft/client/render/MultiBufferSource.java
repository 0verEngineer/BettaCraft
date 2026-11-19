package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * M52d: Minecraft 26.2 removed its {@code MultiBufferSource} (everything goes through the feature renderers now). The draw
 * code of the mod keeps this small interface of the same shape: {@link RecordingBuffers} records into it and hands the
 * geometry to the submit queue, {@link ImmediateBuffers} draws it right away in a level render stage.
 */
@FunctionalInterface
public interface MultiBufferSource {
    VertexConsumer getBuffer(RenderType type);
}
