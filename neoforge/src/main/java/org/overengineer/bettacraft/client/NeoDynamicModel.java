package org.overengineer.bettacraft.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
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
 * since 21.5) becomes the render values. The items of the mod draw with {@code client.item.BlockModelItem}.
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
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockModelPart> parts) {
        collect(state, random, values(level, pos, state), parts);
    }

    void collect(@Nullable BlockState state, RandomSource random, ModelValues data, List<BlockModelPart> parts) {
        for (ChunkSectionLayer layer : dynamic.layers(state, random, data)) {
            @SuppressWarnings("unchecked")
            List<BakedQuad>[] sides = new List[7];
            for (Direction d : Direction.values()) sides[d.ordinal()] = dynamic.quads(state, d, random, data, layer);
            sides[6] = dynamic.quads(state, null, random, data, layer);
            parts.add(new Part(layer, sides, dynamic.ambientOcclusion(layer, data), dynamic.particle(data)));
        }
    }

    @Override
    public TextureAtlasSprite particleIcon() {
        return dynamic.particle(ModelValues.EMPTY);
    }

    @Override
    public TextureAtlasSprite particleIcon(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return dynamic.particle(values(level, pos, state));
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        return null; // the quads depend on the neighbours and the block entity: never shared
    }

    /** The quads of one chunk layer. */
    record Part(ChunkSectionLayer layer, List<BakedQuad>[] sides, boolean ao, TextureAtlasSprite particle) implements BlockModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction side) {
            return sides[side == null ? 6 : side.ordinal()];
        }

        @Override
        public boolean useAmbientOcclusion() {
            return ao;
        }

        @Override
        public TextureAtlasSprite particleIcon() {
            return particle;
        }

        @Override
        public ChunkSectionLayer getRenderType(BlockState state) {
            return layer;
        }

        @Override
        public TriState ambientOcclusion() {
            return ao ? TriState.DEFAULT : TriState.FALSE;
        }
    }

    static List<BakedQuad> all(List<BlockModelPart> parts) {
        List<BakedQuad> out = new ArrayList<>();
        for (BlockModelPart p : parts) {
            for (Direction d : Direction.values()) out.addAll(p.getQuads(d));
            out.addAll(p.getQuads(null));
        }
        return out;
    }
}
