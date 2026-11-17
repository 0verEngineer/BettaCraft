package org.overengineer.bettacraft.fabric.client;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.client.DynamicModel;
import org.overengineer.bettacraft.platform.model.ModelValues;

/**
 * M52: one of our {@link DynamicModel}s through Fabric's renderer API: the render values come from the block entity's
 * render data (our block entities hand theirs over, see the RenderDataMixin), every layer of the model is a material with
 * that layer's blend mode (Fabric draws a block's quads in one chunk layer otherwise).
 */
public final class FabricDynamicModel extends ForwardingBakedModel {
    private static final Direction[] SIDES = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private final DynamicModel dynamic;

    public FabricDynamicModel(DynamicModel dynamic) {
        this.wrapped = (BakedModel) dynamic;
        this.dynamic = dynamic;
    }

    public DynamicModel dynamic() {
        return dynamic;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    /** The values of the block entity at {@code pos}, read for the mesher (the chunk region's render data). */
    static ModelValues values(BlockAndTintGetter level, BlockPos pos) {
        Object data = level.getBlockEntityRenderData(pos);
        return data instanceof ModelValues v ? v : ModelValues.EMPTY;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter level, BlockState state, BlockPos pos, Supplier<RandomSource> random, RenderContext context) {
        ModelValues data = dynamic.modelData(level, pos, state, values(level, pos));
        QuadEmitter emitter = context.getEmitter();
        for (RenderType layer : dynamic.layers(state, random.get(), data)) {
            RenderMaterial material = material(layer);
            for (Direction side : SIDES) {
                if (side != null && context.isFaceCulled(side)) continue;
                for (BakedQuad q : dynamic.quads(state, side, random.get(), data, layer)) {
                    emitter.fromVanilla(q, material, side);
                    emitter.emit();
                }
            }
        }
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> random, RenderContext context) {
        List<RenderType> own = dynamic.itemLayers(false);
        if (own == null) {
            super.emitItemQuads(stack, random, context);
            return;
        }
        // the item draws translucent (the tank's glass): one translucent material for all its quads
        RenderMaterial material = material(ChunkSectionLayer.TRANSLUCENT);
        QuadEmitter emitter = context.getEmitter();
        for (Direction side : SIDES) {
            for (BakedQuad q : wrapped.getQuads(null, side, random.get())) {
                emitter.fromVanilla(q, material, side);
                emitter.emit();
            }
        }
    }

    private static final RenderMaterial[] MATERIALS = new RenderMaterial[BlendMode.values().length];

    static RenderMaterial material(RenderType layer) {
        BlendMode mode = BlendMode.fromRenderLayer(layer);
        RenderMaterial m = MATERIALS[mode.ordinal()];
        if (m == null) {
            Renderer renderer = RendererAccess.INSTANCE.getRenderer();
            m = renderer.materialFinder().blendMode(mode).find();
            MATERIALS[mode.ordinal()] = m;
        }
        return m;
    }
}
