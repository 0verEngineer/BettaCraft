package org.overengineer.bettacraft.fabric.client;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

/**
 * M52: the render layer of each of our blocks on Fabric. NeoForge reads it from the block models ({@code "render_type"});
 * Fabric does not, so the build writes the same values into {@code bettacraft_render_layers.json} (fabric/build.gradle,
 * renderLayers) and they are set here.
 */
final class FabricRenderLayers {
    private FabricRenderLayers() {}

    static void register() {
        try (InputStream in = FabricRenderLayers.class.getResourceAsStream("/bettacraft_render_layers.json")) {
            if (in == null) return;
            JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var e : o.entrySet()) {
                Block b = BuiltInRegistries.BLOCK.getValue(Identifier.parse(e.getKey()));
                RenderType t = switch (e.getValue().getAsString()) {
                    case "minecraft:cutout" -> ChunkSectionLayer.CUTOUT;
                    case "minecraft:cutout_mipped" -> ChunkSectionLayer.CUTOUT;
                    case "minecraft:translucent" -> ChunkSectionLayer.TRANSLUCENT;
                    default -> null;
                };
                if (t != null) BlockRenderLayerMap.INSTANCE.putBlock(b, t);
            }
        } catch (Exception ex) {
            com.mojang.logging.LogUtils.getLogger().error("Bettacraft: reading the render layers failed", ex);
        }
    }
}
