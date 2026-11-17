package org.overengineer.bettacraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.neoforge.NeoModelData;
import org.overengineer.bettacraft.platform.client.DynamicModel;
import org.overengineer.bettacraft.platform.model.ModelValues;

/**
 * M52: one of our {@link DynamicModel}s as a NeoForge model: the model data becomes its render values, the render types
 * its layers; the item draws this model itself (render passes, transforms) with the wrapped JSON model's item render types.
 */
public final class NeoDynamicModel extends BakedModelWrapper<BakedModel> {
    private final DynamicModel dynamic;

    public NeoDynamicModel(DynamicModel dynamic) {
        super((BakedModel) dynamic);
        this.dynamic = dynamic;
    }

    public DynamicModel dynamic() {
        return dynamic;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        return dynamic.quads(state, side, rand, NeoModelData.fromNeo(data), renderType);
    }

    @Override
    @SuppressWarnings("deprecation")
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return dynamic.quads(state, side, rand, ModelValues.EMPTY, null);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return ChunkRenderTypeSet.of(dynamic.layers(state, rand, NeoModelData.fromNeo(data)));
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
        ModelValues in = NeoModelData.fromNeo(data);
        ModelValues out = dynamic.modelData(level, pos, state, in);
        return out == in ? data : NeoModelData.toNeo(out);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return dynamic.particle(NeoModelData.fromNeo(data));
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
        List<RenderType> own = dynamic.itemLayers(fabulous);
        if (own != null) return own;
        BakedModel original = dynamic.original();
        return original != null ? original.getRenderTypes(stack, fabulous) : super.getRenderTypes(stack, fabulous);
    }

    /** M21.7: the item is drawn with this model (the wrapper used to hand the passes to the wrapped model). */
    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
        return List.of(this);
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
        BakedModel original = dynamic.original();
        if (original != null) original.applyTransform(context, pose, leftHand);
        else getTransforms().getTransform(context).apply(leftHand, pose);
        return this;
    }
}
