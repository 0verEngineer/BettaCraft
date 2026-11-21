package org.overengineer.bettacraft.fabric.client;

import com.google.common.collect.MapMaker;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.renderer.v1.render.FabricBlockModelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.overengineer.bettacraft.platform.client.ClientPlatform;
import org.overengineer.bettacraft.platform.client.DynamicModel;
import org.overengineer.bettacraft.platform.model.ModelDataSource;
import org.overengineer.bettacraft.platform.model.ModelKey;
import org.overengineer.bettacraft.platform.model.ModelValues;

/**
 * M52: the client platform on Fabric. A foreign block has one chunk layer here ({@link ItemBlockRenderTypes}); our dynamic
 * models ({@link FabricDynamicModel}) give each quad its own layer through Fabric's renderer API.
 * M52b (1.21.11): vanilla's {@link BakedQuad} no longer carries vertex colours, normals or an ambient occlusion flag; the
 * quads we bake keep theirs in {@link #EXTRAS} (weak, by identity), and {@link FabricDynamicModel} emits them again.
 */
public final class FabricClientPlatform implements ClientPlatform {
    /** Ints per vertex of the 1.21.1 layout: x, y, z, colour ABGR, u, v, light, normal. */
    private static final int STRIDE = 8;

    /** The vertex colours (ARGB), packed normals and the ambient occlusion flag of a quad we baked. */
    record Extras(int[] argb, int[] normals, boolean ao) {}

    private static final Map<BakedQuad, Extras> EXTRAS = new MapMaker().weakKeys().concurrencyLevel(4).makeMap();

    /** The extra (standalone) models of {@code ModelSwaps#extraModels} (filter bundle arms), see {@link FabricModels}. */
    static final Map<Identifier, ExtraModelKey<BlockStateModel>> STANDALONE = new ConcurrentHashMap<>();

    static @Nullable Extras extras(BakedQuad quad) {
        return EXTRAS.get(quad);
    }

    static ModelValues blockEntityValues(BlockAndTintGetter level, BlockPos pos) {
        Object rd = level.getBlockEntityRenderData(pos);
        if (rd instanceof ModelValues v) return v;
        return level.getBlockEntity(pos) instanceof ModelDataSource s ? s.modelValues() : ModelValues.EMPTY;
    }

    @Override
    public Object modelData(BlockStateModel model, BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return null; // our models read the block entity's render data from the level themselves (FabricDynamicModel)
    }

    @Override
    public void tesselate(BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack ps, Function<ChunkSectionLayer, VertexConsumer> buffers,
            boolean checkSides, RandomSource rand, Object modelData, @Nullable ChunkSectionLayer only) {
        var brd = Minecraft.getInstance().getBlockRenderer();
        BlockStateModel model = brd.getBlockModel(state);
        ((FabricBlockModelRenderer) brd.getModelRenderer()).render(level, model, state, pos, ps,
                layer -> only == null || layer == only ? buffers.apply(layer) : Discard.INSTANCE, checkSides, state.getSeed(pos), OverlayTexture.NO_OVERLAY);
    }

    @Override
    public <T> @Nullable T modelValue(BlockAndTintGetter level, BlockPos pos, ModelKey<T> key) {
        return blockEntityValues(level, pos).get(key);
    }

    @Override
    public BlockStateModel wrap(DynamicModel model) {
        return new FabricDynamicModel(model);
    }

    @Override
    public @Nullable DynamicModel dynamic(BlockStateModel model) {
        return model instanceof FabricDynamicModel f ? f.dynamic() : null;
    }

    @Override
    public List<BakedQuad> quads(BlockStateModel model, @Nullable BlockState state, @Nullable Direction side, RandomSource rand,
            @Nullable ChunkSectionLayer layer) {
        DynamicModel d = dynamic(model);
        if (d != null) return d.quads(state, side, rand, ModelValues.EMPTY, layer);
        if (layer != null && state != null && layer != ItemBlockRenderTypes.getChunkRenderType(state)) return List.of();
        List<BlockModelPart> parts = model.collectParts(rand);
        if (parts.size() == 1) return parts.getFirst().getQuads(side);
        List<BakedQuad> out = new ArrayList<>();
        for (BlockModelPart p : parts) out.addAll(p.getQuads(side));
        return out;
    }

    @Override
    public Collection<ChunkSectionLayer> layers(BlockStateModel model, BlockState state, RandomSource rand) {
        DynamicModel d = dynamic(model);
        return d != null ? d.layers(state, rand, ModelValues.EMPTY) : List.of(ItemBlockRenderTypes.getChunkRenderType(state));
    }

    @Override
    public BlockStateModel standaloneModel(Identifier id) {
        var mm = Minecraft.getInstance().getModelManager();
        ExtraModelKey<BlockStateModel> key = STANDALONE.get(id);
        BlockStateModel m = key == null ? null : mm.getModel(key);
        return m != null ? m : mm.getMissingBlockStateModel();
    }

    @Override
    public BakedQuad quad(int[] v, int tint, Direction direction, TextureAtlasSprite sprite, boolean shade, boolean ambientOcclusion) {
        Vector3f[] pos = new Vector3f[4];
        long[] uv = new long[4];
        int[] argb = new int[4], normals = new int[4];
        for (int i = 0; i < 4; i++) {
            int o = i * STRIDE;
            pos[i] = new Vector3f(Float.intBitsToFloat(v[o]), Float.intBitsToFloat(v[o + 1]), Float.intBitsToFloat(v[o + 2]));
            argb[i] = ARGB.fromABGR(v[o + 3]);
            uv[i] = UVPair.pack(Float.intBitsToFloat(v[o + 4]), Float.intBitsToFloat(v[o + 5]));
            normals[i] = v[o + 7] & 0xFFFFFF;
        }
        BakedQuad q = new BakedQuad(pos[0], pos[1], pos[2], pos[3], uv[0], uv[1], uv[2], uv[3], tint, direction, sprite, shade, 0);
        EXTRAS.put(q, new Extras(argb, normals, ambientOcclusion));
        return q;
    }

    @Override
    public int[] vertices(BakedQuad q) {
        int[] v = new int[STRIDE * 4];
        Extras x = EXTRAS.get(q);
        var n = q.direction().getUnitVec3i();
        int faceNormal = packNormal(n.getX(), n.getY(), n.getZ());
        for (int i = 0; i < 4; i++) {
            int o = i * STRIDE;
            var p = q.position(i);
            v[o] = Float.floatToRawIntBits(p.x());
            v[o + 1] = Float.floatToRawIntBits(p.y());
            v[o + 2] = Float.floatToRawIntBits(p.z());
            v[o + 3] = x == null ? -1 : ARGB.toABGR(x.argb[i]);
            v[o + 4] = Float.floatToRawIntBits(UVPair.unpackU(q.packedUV(i)));
            v[o + 5] = Float.floatToRawIntBits(UVPair.unpackV(q.packedUV(i)));
            v[o + 6] = 0;
            v[o + 7] = x == null || x.normals[i] == 0 ? faceNormal : x.normals[i];
        }
        return v;
    }

    static int packNormal(float x, float y, float z) {
        return ((int) (x * 127F) & 0xFF) | ((int) (y * 127F) & 0xFF) << 8 | ((int) (z * 127F) & 0xFF) << 16;
    }

    @Override
    public boolean quadAo(BakedQuad quad) {
        Extras x = EXTRAS.get(quad);
        return x == null || x.ao;
    }

    @Override
    public NativeImage spriteImage(SpriteContents contents) {
        return contents.originalImage;
    }

    /** The buffer of the layers the caller does not want. */
    private enum Discard implements VertexConsumer {
        INSTANCE;

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            return this;
        }

        @Override
        public VertexConsumer setColor(int argb) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return this;
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
