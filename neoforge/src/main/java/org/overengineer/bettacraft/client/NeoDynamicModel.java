package org.overengineer.bettacraft.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.minecraft.util.TriState;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.neoforge.NeoModelData;
import org.overengineer.bettacraft.platform.client.DynamicModel;
import org.overengineer.bettacraft.platform.model.ModelValues;

/**
 * M52: one of our {@link DynamicModel}s as a NeoForge model. M52b (1.21.11): a {@link DynamicBlockStateModel} whose parts
 * are the model's quads of one chunk layer each; the block entity's model data (read from the level, as NeoForge does
 * since 21.5) becomes the render values. The items of the mod draw with {@code client.item.BlockModelItem}. M52c (26.1): the
 * quads carry their chunk layer themselves.
 */
public final class NeoDynamicModel implements DynamicBlockStateModel {
    private final DynamicModel dynamic;

    public NeoDynamicModel(DynamicModel dynamic) {
        this.dynamic = dynamic;
    }

    public DynamicModel dynamic() {
        return dynamic;
    }

    /** The render values of the block at {@code pos} (the block entity's model data plus what the model reads itself). */
    ModelValues values(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return dynamic.modelData(level, pos, state, NeoModelData.fromNeo(level.getModelData(pos)));
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        // M52c (26.1): the crack overlay of a block being mined asks every model with an empty level, BlockPos.ZERO and AIR
        // (no block context): then the model without a state, as for an item
        if (state.isAir()) collect(null, random, ModelValues.EMPTY, parts);
        else collect(state, random, values(level, pos, state), parts);
    }

    void collect(@Nullable BlockState state, RandomSource random, ModelValues data, List<BlockStateModelPart> parts) {
        for (ChunkSectionLayer layer : dynamic.layers(state, random, data)) {
            @SuppressWarnings("unchecked")
            List<BakedQuad>[] sides = new List[7];
            for (Direction d : Direction.values()) sides[d.ordinal()] = dynamic.quads(state, d, random, data, layer);
            sides[6] = dynamic.quads(state, null, random, data, layer);
            parts.add(new Part(layer, sides, dynamic.ambientOcclusion(layer, data), dynamic.particle(data)));
        }
    }

    @Override
    public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial() {
        return new net.minecraft.client.resources.model.sprite.Material.Baked(dynamic.particle(ModelValues.EMPTY), false);
    }

    @Override
    public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return new net.minecraft.client.resources.model.sprite.Material.Baked(dynamic.particle(values(level, pos, state)), false);
    }

    /** M52c (26.1): without the level the model may draw translucent quads (glass, water) and animated ones (water). */
    @Override
    public int materialFlags() {
        return BakedQuad.FLAG_TRANSLUCENT | BakedQuad.FLAG_ANIMATED;
    }

    @Override
    public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        int flags = BakedQuad.FLAG_ANIMATED;
        for (ChunkSectionLayer l : dynamic.layers(state, RandomSource.create(42L), values(level, pos, state))) if (l.translucent()) flags |= BakedQuad.FLAG_TRANSLUCENT;
        return flags;
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        return null; // the quads depend on the neighbours and the block entity: never shared
    }

    /** The quads of one chunk layer. */
    record Part(ChunkSectionLayer layer, List<BakedQuad>[] sides, boolean ao, TextureAtlasSprite particle) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction side) {
            return sides[side == null ? 6 : side.ordinal()];
        }

        @Override
        public boolean useAmbientOcclusion() {
            return ao;
        }

        @Override
        public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial() {
            return new net.minecraft.client.resources.model.sprite.Material.Baked(particle, false);
        }

        /** M52c (26.1): the quads carry their chunk layer; the flags follow the part's layer. */
        @Override
        public int materialFlags() {
            return layer.translucent() ? BakedQuad.FLAG_TRANSLUCENT : 0;
        }

        @Override
        public TriState ambientOcclusion() {
            return ao ? TriState.DEFAULT : TriState.FALSE;
        }
    }

    static List<BakedQuad> all(List<BlockStateModelPart> parts) {
        List<BakedQuad> out = new ArrayList<>();
        for (BlockStateModelPart p : parts) {
            for (Direction d : Direction.values()) out.addAll(p.getQuads(d));
            out.addAll(p.getQuads(null));
        }
        return out;
    }
}
