package org.overengineer.bettacraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
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
 * layer (NeoForge's {@code BlockStateModelPart#getRenderType}); a block entity's model data is read from the level.
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
        // M52c (26.1): the model renderer writes quads (with their chunk layer) to an output; ours puts them into the
        // buffer of their layer, through the pose
        Minecraft mc = Minecraft.getInstance();
        BlockStateModel model = mc.getModelManager().getBlockStateModelSet().get(state);
        PoseStack.Pose pose = ps.last();
        renderer(checkSides).tesselateBlock((x, y, z, quad, instance) -> {
            ChunkSectionLayer l = quad.materialInfo().layer();
            if (only == null || l == only) buffers.apply(l).putBakedQuad(pose, quad, instance);
        }, 0, 0, 0, level, pos, state, model, state.getSeed(pos));
    }

    /** M52c: one model renderer per thread (the miniature meshes on workers), culling mode and ambient occlusion option. */
    private static final ThreadLocal<net.minecraft.client.renderer.block.ModelBlockRenderer[]> RENDERERS =
            ThreadLocal.withInitial(() -> new net.minecraft.client.renderer.block.ModelBlockRenderer[4]);

    private static net.minecraft.client.renderer.block.ModelBlockRenderer renderer(boolean cull) {
        Minecraft mc = Minecraft.getInstance();
        boolean ao = mc.options.ambientOcclusion().get();
        int i = (cull ? 1 : 0) | (ao ? 2 : 0);
        var arr = RENDERERS.get();
        if (arr[i] == null) arr[i] = new net.minecraft.client.renderer.block.ModelBlockRenderer(ao, cull, mc.getBlockColors());
        return arr[i];
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
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(rand, parts);
        if (parts.size() == 1 && layer == null) return parts.getFirst().getQuads(side);
        List<BakedQuad> out = new ArrayList<>();
        for (BlockStateModelPart p : parts) {
            for (BakedQuad q : p.getQuads(side)) if (layer == null || q.materialInfo().layer() == layer) out.add(q);
        }
        return out;
    }

    @Override
    public Collection<ChunkSectionLayer> layers(BlockStateModel model, BlockState state, RandomSource rand) {
        // M52c (26.1): the layers of the quads, in the order of the chunk layers
        java.util.EnumSet<ChunkSectionLayer> set = java.util.EnumSet.noneOf(ChunkSectionLayer.class);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(rand, parts);
        for (BlockStateModelPart p : parts) {
            for (Direction d : Direction.values()) for (BakedQuad q : p.getQuads(d)) set.add(q.materialInfo().layer());
            for (BakedQuad q : p.getQuads(null)) set.add(q.materialInfo().layer());
        }
        return new LinkedHashSet<>(set);
    }

    @Override
    public BlockStateModel standaloneModel(net.minecraft.resources.Identifier id) {
        StandaloneModelKey<BlockStateModel> key = STANDALONE.get(id);
        BlockStateModel m = key == null ? null : Minecraft.getInstance().getModelManager().getStandaloneModel(key);
        return m != null ? m : Minecraft.getInstance().getModelManager().getBlockStateModelSet().missingModel();
    }

    /** Ints per vertex of the 1.21.1 layout: x, y, z, colour ABGR, u, v, light, normal. */
    private static final int STRIDE = 8;

    @Override
    public BakedQuad quad(int[] v, int tint, Direction direction, TextureAtlasSprite sprite, boolean shade, boolean ambientOcclusion, ChunkSectionLayer layer) {
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
        // M52c (26.1): the material info carries the chunk layer and the item render type
        var info = new BakedQuad.MaterialInfo(sprite, layer, layer.translucent() ? net.minecraft.client.renderer.Sheets.translucentBlockItemSheet()
                : net.minecraft.client.renderer.Sheets.cutoutBlockItemSheet(), tint, shade, 0, ambientOcclusion);
        return new BakedQuad(pos[0], pos[1], pos[2], pos[3], uv[0], uv[1], uv[2], uv[3], direction, info,
                net.neoforged.neoforge.client.model.quad.BakedNormals.of(normals[0], normals[1], normals[2], normals[3]),
                net.neoforged.neoforge.client.model.quad.BakedColors.of(colors[0], colors[1], colors[2], colors[3]));
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
        return quad.materialInfo().ambientOcclusion();
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
                    new org.joml.Matrix4f(e.getModelViewMatrix()), RenderFrame.projection(), mc.levelRenderer.getTicks(), mc.getDeltaTracker(),
                    mc.gameRenderer.getMainCamera(), RenderFrame.frustum()));
        }
    }
}
