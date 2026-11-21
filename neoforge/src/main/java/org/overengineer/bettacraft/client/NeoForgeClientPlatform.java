package org.overengineer.bettacraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.neoforge.NeoModelData;
import org.overengineer.bettacraft.platform.client.ClientPlatform;
import org.overengineer.bettacraft.platform.client.DynamicModel;
import org.overengineer.bettacraft.platform.client.RenderFrame;
import org.overengineer.bettacraft.platform.client.RenderLevelStageEvent;
import org.overengineer.bettacraft.platform.model.ModelKey;

/**
 * M52: the client platform layer on NeoForge. M52b (1.21.11): block models are made of parts, each part names its chunk
 * layer (NeoForge's {@code BlockModelPart#getRenderType}); a block entity's model data is read from the level.
 */
public final class NeoForgeClientPlatform implements ClientPlatform {
    /** The extra models of {@code ModelSwaps#extraModels} (filter bundle arms), registered as standalone models. */
    static final java.util.Map<net.minecraft.resources.Identifier, StandaloneModelKey<BlockStateModel>> STANDALONE = new java.util.concurrent.ConcurrentHashMap<>();

    static void registerStandalone(net.neoforged.neoforge.client.event.ModelEvent.RegisterStandalone e) {
        for (var id : org.overengineer.bettacraft.client.model.ModelSwaps.extraModels()) {
            StandaloneModelKey<BlockStateModel> key = STANDALONE.computeIfAbsent(id, k -> new StandaloneModelKey<>(k::toString));
            e.register(key, SimpleUnbakedStandaloneModel.blockStateModel(id));
        }
    }

    @Override
    public Object modelData(BlockStateModel model, BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return null; // the models read the block entity's model data from the level themselves
    }

    @Override
    public void tesselate(BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack ps, Function<ChunkSectionLayer, VertexConsumer> buffers,
            boolean checkSides, RandomSource rand, Object modelData, @Nullable ChunkSectionLayer only) {
        var brd = Minecraft.getInstance().getBlockRenderer();
        List<BlockModelPart> parts = brd.getBlockModel(state).collectParts(level, pos, state, rand);
        if (only != null) parts.removeIf(p -> p.getRenderType(state) != only);
        if (parts.isEmpty()) return;
        brd.getModelRenderer().tesselateBlock(level, parts, state, pos, ps, buffers, checkSides, OverlayTexture.NO_OVERLAY);
    }

    @Override
    public <T> @Nullable T modelValue(BlockAndTintGetter level, BlockPos pos, ModelKey<T> key) {
        return level.getModelData(pos).get(NeoModelData.prop(key));
    }

    @Override
    public BlockStateModel wrap(DynamicModel model) {
        return new NeoDynamicModel(model);
    }

    @Override
    public @Nullable DynamicModel dynamic(BlockStateModel model) {
        return model instanceof NeoDynamicModel n ? n.dynamic() : null;
    }

    @Override
    public List<BakedQuad> quads(BlockStateModel model, @Nullable BlockState state, @Nullable Direction side, RandomSource rand,
            @Nullable ChunkSectionLayer layer) {
        List<BlockModelPart> parts = model.collectParts(rand);
        if (parts.size() == 1 && (layer == null || state == null || parts.getFirst().getRenderType(state) == layer)) return parts.getFirst().getQuads(side);
        List<BakedQuad> out = new ArrayList<>();
        for (BlockModelPart p : parts) {
            if (layer != null && state != null && p.getRenderType(state) != layer) continue;
            out.addAll(p.getQuads(side));
        }
        return out;
    }

    @Override
    public Collection<ChunkSectionLayer> layers(BlockStateModel model, BlockState state, RandomSource rand) {
        LinkedHashSet<ChunkSectionLayer> out = new LinkedHashSet<>(2);
        for (BlockModelPart p : model.collectParts(rand)) out.add(p.getRenderType(state));
        return out;
    }

    @Override
    public BlockStateModel standaloneModel(net.minecraft.resources.Identifier id) {
        StandaloneModelKey<BlockStateModel> key = STANDALONE.get(id);
        BlockStateModel m = key == null ? null : Minecraft.getInstance().getModelManager().getStandaloneModel(key);
        return m != null ? m : Minecraft.getInstance().getModelManager().getMissingBlockStateModel();
    }

    /** Ints per vertex of the 1.21.1 layout: x, y, z, colour ABGR, u, v, light, normal. */
    private static final int STRIDE = 8;

    @Override
    public BakedQuad quad(int[] v, int tint, Direction direction, TextureAtlasSprite sprite, boolean shade, boolean ambientOcclusion) {
        org.joml.Vector3f[] pos = new org.joml.Vector3f[4];
        long[] uv = new long[4];
        int[] colors = new int[4], normals = new int[4];
        for (int i = 0; i < 4; i++) {
            int o = i * STRIDE;
            pos[i] = new org.joml.Vector3f(Float.intBitsToFloat(v[o]), Float.intBitsToFloat(v[o + 1]), Float.intBitsToFloat(v[o + 2]));
            colors[i] = net.minecraft.util.ARGB.fromABGR(v[o + 3]);
            uv[i] = net.minecraft.client.model.geom.builders.UVPair.pack(Float.intBitsToFloat(v[o + 4]), Float.intBitsToFloat(v[o + 5]));
            normals[i] = v[o + 7] & 0xFFFFFF;
        }
        return new BakedQuad(pos[0], pos[1], pos[2], pos[3], uv[0], uv[1], uv[2], uv[3], tint, direction, sprite, shade, 0,
                net.neoforged.neoforge.client.model.quad.BakedNormals.of(normals[0], normals[1], normals[2], normals[3]),
                net.neoforged.neoforge.client.model.quad.BakedColors.of(colors[0], colors[1], colors[2], colors[3]), ambientOcclusion);
    }

    @Override
    public int[] vertices(BakedQuad q) {
        int[] v = new int[STRIDE * 4];
        var n = q.direction().getUnitVec3i();
        int faceNormal = net.neoforged.neoforge.client.model.quad.BakedNormals.pack(n.getX(), n.getY(), n.getZ());
        for (int i = 0; i < 4; i++) {
            int o = i * STRIDE;
            var p = q.position(i);
            v[o] = Float.floatToRawIntBits(p.x());
            v[o + 1] = Float.floatToRawIntBits(p.y());
            v[o + 2] = Float.floatToRawIntBits(p.z());
            v[o + 3] = net.minecraft.util.ARGB.toABGR(q.bakedColors().color(i));
            v[o + 4] = Float.floatToRawIntBits(net.minecraft.client.model.geom.builders.UVPair.unpackU(q.packedUV(i)));
            v[o + 5] = Float.floatToRawIntBits(net.minecraft.client.model.geom.builders.UVPair.unpackV(q.packedUV(i)));
            v[o + 6] = 0;
            int normal = q.bakedNormals().normal(i);
            v[o + 7] = net.neoforged.neoforge.client.model.quad.BakedNormals.isUnspecified(normal) ? faceNormal : normal;
        }
        return v;
    }

    @Override
    public boolean quadAo(BakedQuad quad) {
        return quad.hasAmbientOcclusion();
    }

    @Override
    public com.mojang.blaze3d.platform.NativeImage spriteImage(net.minecraft.client.renderer.texture.SpriteContents contents) {
        return contents.getOriginalImage();
    }

    /** NeoForge's render stage events as the common stages (several of ours share one of NeoForge's since 1.21.9). */
    static void fire(net.neoforged.neoforge.client.event.RenderLevelStageEvent e, RenderLevelStageEvent.Stage... stages) {
        Minecraft mc = Minecraft.getInstance();
        PoseStack ps = e.getPoseStack() != null ? e.getPoseStack() : new PoseStack();
        for (RenderLevelStageEvent.Stage s : stages) {
            org.overengineer.bettacraft.platform.client.ClientEvents.fireRenderLevel(new RenderLevelStageEvent(s, e.getLevelRenderer(), ps,
                    e.getModelViewMatrix(), RenderFrame.projection(), mc.levelRenderer.getTicks(), mc.getDeltaTracker(),
                    mc.gameRenderer.getMainCamera(), RenderFrame.frustum()));
        }
    }

    /** M53: the client section of NeoForge's config screen (the same classes on Fabric through Forge Config API Port). */
    @Override
    public net.minecraft.client.gui.screens.Screen clientConfigScreen(net.minecraft.client.gui.screens.Screen parent) {
        for (net.neoforged.fml.config.ModConfig c : net.neoforged.fml.config.ModConfigs.getConfigSet(net.neoforged.fml.config.ModConfig.Type.CLIENT)) {
            if (!c.getModId().equals(org.overengineer.bettacraft.BettaCraft.MOD_ID)) continue;
            return new net.neoforged.neoforge.client.gui.ConfigurationScreen.ConfigurationSectionScreen(parent, net.neoforged.fml.config.ModConfig.Type.CLIENT, c,
                    net.minecraft.network.chat.Component.translatable("gui.bettacraft.client_config.title"));
        }
        return null;
    }
}
