package org.overengineer.bettacraft.client.model;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.client.ClientPlatform;
import org.overengineer.bettacraft.platform.client.WrappedModel;
import org.overengineer.bettacraft.platform.model.ModelValues;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.CabinetBlockItem;

/**
 * Cabinet model (PLAN 6.5): the generated cabinet models use the placeholder texture {@code bettacraft:block/cabinet_wood}
 * for every wooden part. This wrapper swaps it at runtime for the side texture of the planks the cabinet is made of
 * (UVs are remapped into the planks sprite), so cabinets of any mod's planks look right without extra assets.
 * The planks sprite is taken from the planks' own block model (north face, else its particle).
 */
public class CabinetBakedModel extends WrappedModel {
    public static final Identifier PLACEHOLDER = BettaCraft.id("block/cabinet_wood");
    private static final Map<BlockState, TextureAtlasSprite> WOOD_SPRITES = new ConcurrentHashMap<>();

    @Nullable
    private final BlockState fixedWood;
    /** (wood, side) -> retextured quads of this wrapped model. */
    private final Map<BlockState, List<BakedQuad>[]> cache = new ConcurrentHashMap<>();

    public CabinetBakedModel(BlockStateModel original, @Nullable BlockState fixedWood) {
        super(original);
        this.fixedWood = fixedWood;
    }

    /** M52b: the quads of the cabinet item in the planks {@code wood} (1.21.1: item overrides; see {@code BlockModelItem}). */
    public static List<BakedQuad> retextureAll(List<BakedQuad> quads, BlockState wood) {
        TextureAtlasSprite target = woodSprite(wood);
        List<BakedQuad> out = new ArrayList<>(quads.size());
        for (BakedQuad q : quads) out.add(q.materialInfo().sprite().contents().name().equals(PLACEHOLDER) ? retexture(q, target) : q);
        return List.copyOf(out);
    }

    /** Forget the planks sprites (resource reload). */
    public static void clearSprites() {
        WOOD_SPRITES.clear();
    }

    private BlockState woodOf(ModelValues data) {
        if (fixedWood != null) return fixedWood;
        BlockState w = data.get(CabinetBlockEntity.WOOD_STATE);
        return w != null ? w : Blocks.OAK_PLANKS.defaultBlockState();
    }

    static TextureAtlasSprite woodSprite(BlockState wood) {
        return WOOD_SPRITES.computeIfAbsent(wood, w -> {
            BlockStateModel m = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(w);
            RandomSource r = RandomSource.create(42);
            for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.UP}) {
                List<BakedQuad> q = ClientPlatform.get().quads(m, w, d, r, null);
                if (!q.isEmpty() && q.getFirst().materialInfo().sprite().atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS)) return q.getFirst().materialInfo().sprite();
            }
            List<BakedQuad> q = ClientPlatform.get().quads(m, w, null, r, null);
            if (!q.isEmpty()) return q.getFirst().materialInfo().sprite();
            return m.particleMaterial().sprite();
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<BakedQuad> quads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelValues data, @Nullable ChunkSectionLayer renderType) {
        BlockState wood = woodOf(data);
        List<BakedQuad>[] perSide = cache.computeIfAbsent(wood, k -> new List[7]);
        int idx = side == null ? 6 : side.ordinal();
        List<BakedQuad> cached = perSide[idx];
        if (cached != null) return cached;
        List<BakedQuad> orig = ClientPlatform.get().quads(originalModel, state, side, rand, renderType);
        TextureAtlasSprite target = woodSprite(wood);
        List<BakedQuad> out = new ArrayList<>(orig.size());
        for (BakedQuad q : orig) out.add(q.materialInfo().sprite().contents().name().equals(PLACEHOLDER) ? retexture(q, target) : q);
        // the generated cabinet models are plain JSON models: their quads do not depend on the render type
        List<BakedQuad> result = List.copyOf(out);
        perSide[idx] = result;
        return result;
    }

    /** Copies the quad with UVs moved from the placeholder sprite into {@code target}. */
    static BakedQuad retexture(BakedQuad q, TextureAtlasSprite target) {
        TextureAtlasSprite src = q.materialInfo().sprite();
        int[] v = org.overengineer.bettacraft.platform.client.ClientPlatform.get().vertices(q).clone();
        int stride = v.length / 4;
        for (int i = 0; i < 4; i++) {
            int o = i * stride + 4; // position (3) + colour (1), then uv0
            float u = Float.intBitsToFloat(v[o]), w = Float.intBitsToFloat(v[o + 1]);
            // M21.5: clamped to the planks texture (UVs past the placeholder used to land on neighbouring atlas textures)
            float fu = clamp01((u - src.getU0()) / (src.getU1() - src.getU0()));
            float fv = clamp01((w - src.getV0()) / (src.getV1() - src.getV0()));
            v[o] = Float.floatToRawIntBits(target.getU0() + fu * (target.getU1() - target.getU0()));
            v[o + 1] = Float.floatToRawIntBits(target.getV0() + fv * (target.getV1() - target.getV0()));
        }
        return ClientPlatform.get().quad(v, q.materialInfo().tintIndex(), q.direction(), target, q.materialInfo().shadeDirectionOverride(), ClientPlatform.get().quadAo(q), q.materialInfo().layer());
    }

    /** M21.7: a quarter texel inside the sprite (a UV exactly on its edge can pick a texel of the neighbouring atlas texture). */
    private static final float INSET = 1F / 64F;

    static float clamp01(float f) {
        return f < INSET ? INSET : f > 1 - INSET ? 1 - INSET : f;
    }

    @Override
    public java.util.Collection<ChunkSectionLayer> layers(BlockState state, RandomSource rand, ModelValues data) {
        return List.of(ChunkSectionLayer.SOLID);
    }

    @Override
    public TextureAtlasSprite particle(ModelValues data) {
        return woodSprite(woodOf(data));
    }

}
