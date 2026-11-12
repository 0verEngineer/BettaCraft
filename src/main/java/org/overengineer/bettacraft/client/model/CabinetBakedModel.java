package org.overengineer.bettacraft.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.CabinetBlockItem;

/**
 * Cabinet model (PLAN 6.5): the generated cabinet models use the placeholder texture {@code bettacraft:block/cabinet_wood}
 * for every wooden part. This wrapper swaps it at runtime for the side texture of the planks the cabinet is made of
 * (UVs are remapped into the planks sprite), so cabinets of any mod's planks look right without extra assets.
 * The planks sprite is taken from the planks' own block model (north face, else its particle).
 */
public class CabinetBakedModel extends BakedModelWrapper<BakedModel> {
    public static final ResourceLocation PLACEHOLDER = BettaCraft.id("block/cabinet_wood");
    private static final Map<BlockState, TextureAtlasSprite> WOOD_SPRITES = new ConcurrentHashMap<>();

    @Nullable
    private final BlockState fixedWood;
    /** (wood, side) -> retextured quads of this wrapped model. */
    private final Map<BlockState, List<BakedQuad>[]> cache = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, CabinetBakedModel> itemVariants;
    private final ItemOverrides overrides;

    public CabinetBakedModel(BakedModel original, @Nullable BlockState fixedWood, @Nullable Map<ResourceLocation, CabinetBakedModel> shared) {
        super(original);
        this.fixedWood = fixedWood;
        this.itemVariants = shared != null ? shared : new ConcurrentHashMap<>();
        this.overrides = new ItemOverrides() {
            @Override
            public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
                ResourceLocation id = CabinetBlockItem.getWood(stack);
                return itemVariants.computeIfAbsent(id, k -> new CabinetBakedModel(originalModel, CabinetBlockItem.woodBlock(stack).defaultBlockState(), itemVariants));
            }
        };
    }

    /** Wraps every cabinet block state model and the item model (models are rebaked on every resource reload). */
    public static void onModifyBakingResult(net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult event) {
        clearSprites();
        for (BlockState s : org.overengineer.bettacraft.registry.ModBlocks.CABINET.get().getStateDefinition().getPossibleStates()) {
            var loc = net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(s);
            BakedModel m = event.getModels().get(loc);
            if (m != null && !(m instanceof CabinetBakedModel)) event.getModels().put(loc, new CabinetBakedModel(m, null, null));
        }
        var item = net.minecraft.client.resources.model.ModelResourceLocation.inventory(BettaCraft.id("cabinet"));
        BakedModel m = event.getModels().get(item);
        if (m != null && !(m instanceof CabinetBakedModel)) event.getModels().put(item, new CabinetBakedModel(m, null, null));
    }

    /** Forget the planks sprites (resource reload). */
    public static void clearSprites() {
        WOOD_SPRITES.clear();
    }

    private BlockState woodOf(ModelData data) {
        if (fixedWood != null) return fixedWood;
        BlockState w = data.get(CabinetBlockEntity.WOOD_STATE);
        return w != null ? w : Blocks.OAK_PLANKS.defaultBlockState();
    }

    static TextureAtlasSprite woodSprite(BlockState wood) {
        return WOOD_SPRITES.computeIfAbsent(wood, w -> {
            BakedModel m = Minecraft.getInstance().getBlockRenderer().getBlockModel(w);
            RandomSource r = RandomSource.create(42);
            for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.UP}) {
                List<BakedQuad> q = m.getQuads(w, d, r, ModelData.EMPTY, null);
                if (!q.isEmpty() && q.getFirst().getSprite().atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS)) return q.getFirst().getSprite();
            }
            List<BakedQuad> q = m.getQuads(w, null, r, ModelData.EMPTY, null);
            if (!q.isEmpty()) return q.getFirst().getSprite();
            return m.getParticleIcon(ModelData.EMPTY);
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        BlockState wood = woodOf(data);
        List<BakedQuad>[] perSide = cache.computeIfAbsent(wood, k -> new List[7]);
        int idx = side == null ? 6 : side.ordinal();
        List<BakedQuad> cached = perSide[idx];
        if (cached != null) return cached;
        List<BakedQuad> orig = originalModel.getQuads(state, side, rand, ModelData.EMPTY, renderType);
        TextureAtlasSprite target = woodSprite(wood);
        List<BakedQuad> out = new ArrayList<>(orig.size());
        for (BakedQuad q : orig) out.add(q.getSprite().contents().name().equals(PLACEHOLDER) ? retexture(q, target) : q);
        // the generated cabinet models are plain JSON models: their quads do not depend on the render type
        List<BakedQuad> result = List.copyOf(out);
        perSide[idx] = result;
        return result;
    }

    /** Copies the quad with UVs moved from the placeholder sprite into {@code target}. */
    static BakedQuad retexture(BakedQuad q, TextureAtlasSprite target) {
        TextureAtlasSprite src = q.getSprite();
        int[] v = q.getVertices().clone();
        int stride = v.length / 4;
        for (int i = 0; i < 4; i++) {
            int o = i * stride + 4; // position (3) + colour (1), then uv0
            float u = Float.intBitsToFloat(v[o]), w = Float.intBitsToFloat(v[o + 1]);
            float fu = (u - src.getU0()) / (src.getU1() - src.getU0());
            float fv = (w - src.getV0()) / (src.getV1() - src.getV0());
            v[o] = Float.floatToRawIntBits(target.getU0() + fu * (target.getU1() - target.getU0()));
            v[o + 1] = Float.floatToRawIntBits(target.getV0() + fv * (target.getV1() - target.getV0()));
        }
        return new BakedQuad(v, q.getTintIndex(), q.getDirection(), target, q.isShade(), q.hasAmbientOcclusion());
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return ChunkRenderTypeSet.of(RenderType.solid());
    }

    @Override
    public ModelData getModelData(net.minecraft.world.level.BlockAndTintGetter level, net.minecraft.core.BlockPos pos, BlockState state, ModelData modelData) {
        return modelData;
    }

    @Override
    @SuppressWarnings("deprecation")
    public TextureAtlasSprite getParticleIcon() {
        return woodSprite(fixedWood != null ? fixedWood : Blocks.OAK_PLANKS.defaultBlockState());
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return woodSprite(woodOf(data));
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }

    @Override
    @SuppressWarnings("deprecation")
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }
}
