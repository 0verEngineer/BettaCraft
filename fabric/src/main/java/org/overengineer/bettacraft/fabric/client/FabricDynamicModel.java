package org.overengineer.bettacraft.fabric.client;

import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.platform.client.DynamicModel;
import org.overengineer.bettacraft.platform.model.ModelValues;

/**
 * M52: one of our {@link DynamicModel}s through Fabric's renderer API: the render values come from the block entity's
 * render data (our block entities hand theirs over, see the RenderDataMixin), every quad carries the chunk layer it was
 * made for. M52b (1.21.11): a {@link BlockStateModel} whose {@code emitQuads} (Fabric's renderer API, no materials any
 * more) emits the quads with their layer, vertex colours and normals ({@link FabricClientPlatform#extras}); the vanilla
 * {@code collectParts} gives one part per layer without the block entity's values.
 */
public final class FabricDynamicModel implements BlockStateModel {
    private static final Direction[] SIDES = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private final DynamicModel dynamic;

    public FabricDynamicModel(DynamicModel dynamic) {
        this.dynamic = dynamic;
    }

    public DynamicModel dynamic() {
        return dynamic;
    }

    private ModelValues values(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return dynamic.modelData(level, pos, state, FabricClientPlatform.blockEntityValues(level, pos));
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
            Predicate<@Nullable Direction> cullTest) {
        ModelValues data = values(level, pos, state);
        TriState partAo = dynamic.ambientOcclusion() ? TriState.DEFAULT : TriState.FALSE;
        for (ChunkSectionLayer layer : dynamic.layers(state, random, data)) {
            for (Direction side : SIDES) {
                if (side != null && cullTest.test(side)) continue;
                for (BakedQuad q : dynamic.quads(state, side, random, data, layer)) emit(emitter, q, side, layer, partAo);
            }
        }
    }

    static void emit(QuadEmitter emitter, BakedQuad q, @Nullable Direction side, ChunkSectionLayer layer, TriState partAo) {
        emitter.fromBakedQuad(q);
        emitter.cullFace(side);
        emitter.renderLayer(layer);
        FabricClientPlatform.Extras x = FabricClientPlatform.extras(q);
        TriState ao = partAo;
        if (x != null) {
            for (int i = 0; i < 4; i++) {
                emitter.color(i, x.argb()[i]);
                int n = x.normals()[i];
                if (n != 0) emitter.normal(i, (byte) n / 127F, (byte) (n >> 8) / 127F, (byte) (n >> 16) / 127F);
            }
            if (!x.ao()) ao = TriState.FALSE;
        }
        emitter.ambientOcclusion(ao);
        emitter.emit();
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        return null; // the quads depend on the neighbours and the block entity: never shared
    }

    @Override
    public TextureAtlasSprite particleSprite(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return dynamic.particle(values(level, pos, state));
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts) {
        for (ChunkSectionLayer layer : dynamic.layers(null, random, ModelValues.EMPTY)) {
            @SuppressWarnings("unchecked")
            List<BakedQuad>[] sides = new List[7];
            for (Direction d : Direction.values()) sides[d.ordinal()] = dynamic.quads(null, d, random, ModelValues.EMPTY, layer);
            sides[6] = dynamic.quads(null, null, random, ModelValues.EMPTY, layer);
            parts.add(new Part(sides, dynamic.ambientOcclusion(), dynamic.particle(ModelValues.EMPTY)));
        }
    }

    @Override
    public TextureAtlasSprite particleIcon() {
        return dynamic.particle(ModelValues.EMPTY);
    }

    /** The quads of one chunk layer for the vanilla paths. */
    record Part(List<BakedQuad>[] sides, boolean ao, TextureAtlasSprite particle) implements BlockModelPart {
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
    }
}
