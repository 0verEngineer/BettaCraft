package org.overengineer.bettacraft.fabric.client;

import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
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
        // M52c (26.1): the crack overlay of a block being mined asks every model with an empty level, BlockPos.ZERO and AIR
        // (no block context): then the model without a state, as for an item
        boolean noBlock = state.isAir();
        ModelValues data = noBlock ? ModelValues.EMPTY : values(level, pos, state);
        if (noBlock) state = null;
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
        emitter.chunkLayer(layer);
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
    public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return new net.minecraft.client.resources.model.sprite.Material.Baked(dynamic.particle(values(level, pos, state)), false);
    }

    /** M52c (26.1): without the level the model may draw translucent quads (glass, water) and animated ones (water). */
    @Override
    public int materialFlags() {
        return BakedQuad.FLAG_TRANSLUCENT | BakedQuad.FLAG_ANIMATED;
    }

    @Override
    public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        int flags = BakedQuad.FLAG_ANIMATED;
        for (ChunkSectionLayer l : dynamic.layers(state, random, values(level, pos, state))) if (l.translucent()) flags |= BakedQuad.FLAG_TRANSLUCENT;
        return flags;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
        for (ChunkSectionLayer layer : dynamic.layers(null, random, ModelValues.EMPTY)) {
            @SuppressWarnings("unchecked")
            List<BakedQuad>[] sides = new List[7];
            for (Direction d : Direction.values()) sides[d.ordinal()] = dynamic.quads(null, d, random, ModelValues.EMPTY, layer);
            sides[6] = dynamic.quads(null, null, random, ModelValues.EMPTY, layer);
            parts.add(new Part(sides, dynamic.ambientOcclusion(), dynamic.particle(ModelValues.EMPTY), layer));
        }
    }

    @Override
    public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial() {
        return new net.minecraft.client.resources.model.sprite.Material.Baked(dynamic.particle(ModelValues.EMPTY), false);
    }

    /** The quads of one chunk layer for the vanilla paths. */
    record Part(List<BakedQuad>[] sides, boolean ao, TextureAtlasSprite particle, ChunkSectionLayer layer) implements BlockStateModelPart {
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

        @Override
        public int materialFlags() {
            return layer.translucent() ? BakedQuad.FLAG_TRANSLUCENT : 0;
        }
    }
}
