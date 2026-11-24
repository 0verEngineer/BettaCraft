package org.overengineer.bettacraft.client.model;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.client.ClientPlatform;
import org.overengineer.bettacraft.platform.client.WrappedModel;
import org.overengineer.bettacraft.platform.model.ModelValues;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.plumbing.FilterPipeBlockEntity;
import org.overengineer.bettacraft.plumbing.FilterPipes;
import org.overengineer.bettacraft.plumbing.PipeLine;
import org.overengineer.bettacraft.registry.ModBlocks;

/**
 * M40 part b: the model of the pipe bundle. The block state's model is the steel manifold; for every line of the bundle
 * ({@link FilterPipeBlockEntity#LINES}) its arms on the joined sides (and the brass collars of a T-piece) are added from
 * their own models (made by the datagen, {@code block/filter_pipe_bundle_<line>_<side>[_tee]}). With a cover (part c) the
 * bundle is drawn as that block, like a single filter pipe.
 * M65: on the sides where the one line joined there meets a single pipe, its arm is the one in the middle of the face
 * ({@code ..._mid[_tee]}, {@link FilterPipeBundleBlock#centredSides}, read from the neighbours while the chunk is meshed).
 */
public final class FilterBundleModel extends WrappedModel {
    private volatile BlockStateModel[][] arms, tees, midArms, midTees;
    /** M65: {@link FilterPipeBundleBlock#centredSides} of the block (absent = none). */
    public static final org.overengineer.bettacraft.platform.model.ModelKey<Integer> CENTRED = new org.overengineer.bettacraft.platform.model.ModelKey<>("bundle_centred");

    public FilterBundleModel(BlockStateModel original) {
        super(original);
    }

    /** The arm models, looked up once after the models are baked (extra models, {@link ModelSwaps#EXTRA}). */
    private void resolveArms() {
        if (arms != null) return;
        int n = PipeLine.values().length;
        BlockStateModel[][] a = new BlockStateModel[n][6], t = new BlockStateModel[n][6], ma = new BlockStateModel[n][6], mt = new BlockStateModel[n][6];
        for (PipeLine l : PipeLine.values()) for (Direction d : Direction.values()) {
            a[l.ordinal()][d.ordinal()] = ClientPlatform.get().standaloneModel(armModel(l, d, false));
            t[l.ordinal()][d.ordinal()] = ClientPlatform.get().standaloneModel(armModel(l, d, true));
            ma[l.ordinal()][d.ordinal()] = ClientPlatform.get().standaloneModel(armModel(l, d, false, true));
            mt[l.ordinal()][d.ordinal()] = ClientPlatform.get().standaloneModel(armModel(l, d, true, true));
        }
        tees = t;
        midArms = ma;
        midTees = mt;
        arms = a;
    }

    public static Identifier armModel(PipeLine l, Direction d, boolean tee) {
        return armModel(l, d, tee, false);
    }

    /** M65: {@code mid}: the arm (or collar) in the middle of the face. */
    public static Identifier armModel(PipeLine l, Direction d, boolean tee, boolean mid) {
        return BettaCraft.id("block/filter_pipe_bundle_" + l.getSerializedName() + "_" + d.getSerializedName() + (mid ? "_mid" : "") + (tee ? "_tee" : ""));
    }

    @Override
    public ModelValues modelData(net.minecraft.client.renderer.block.BlockAndTintGetter level, net.minecraft.core.BlockPos pos, BlockState state, ModelValues data) {
        Integer lines = data.get(FilterPipeBlockEntity.LINES);
        if (lines == null || data.get(FilterPipeBlockEntity.COVER) != null) return data;
        int centred = org.overengineer.bettacraft.plumbing.FilterPipeBundleBlock.centredSides(level, pos, lines);
        return centred == 0 ? data : data.derive().with(CENTRED, centred).build();
    }

    @Override
    public List<BakedQuad> quads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelValues data, @Nullable ChunkSectionLayer renderType) {
        BlockState c = data.get(FilterPipeBlockEntity.COVER);
        if (c != null) return ClientPlatform.get().quads(Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(c), c, side, rand, renderType);
        List<BakedQuad> out = new ArrayList<>(super.quads(state, side, rand, data, renderType));
        Integer lines = data.get(FilterPipeBlockEntity.LINES);
        if (lines == null) return out;
        resolveArms();
        Integer cd = data.get(CENTRED);
        int centred = cd == null ? 0 : cd;
        for (PipeLine l : PipeLine.values()) {
            int slot = FilterPipes.slot(lines, l);
            if (!FilterPipes.present(slot)) continue;
            for (Direction d : Direction.values()) {
                if (!FilterPipes.joined(slot, d)) continue;
                boolean mid = (centred & 1 << d.ordinal()) != 0;
                BlockStateModel a = (mid ? midArms : arms)[l.ordinal()][d.ordinal()];
                if (a != null) out.addAll(ClientPlatform.get().quads(a, state, side, rand, renderType));
                BlockStateModel t = FilterPipes.junction(slot) ? (mid ? midTees : tees)[l.ordinal()][d.ordinal()] : null;
                if (t != null) out.addAll(ClientPlatform.get().quads(t, state, side, rand, renderType));
            }
        }
        return out;
    }

    @Override
    public java.util.Collection<ChunkSectionLayer> layers(BlockState state, RandomSource rand, ModelValues data) {
        BlockState c = data.get(FilterPipeBlockEntity.COVER);
        if (c == null) return super.layers(state, rand, data);
        return ClientPlatform.get().layers(Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(c), c, rand);
    }

    @Override
    public net.minecraft.client.renderer.texture.TextureAtlasSprite particle(ModelValues data) {
        BlockState c = data.get(FilterPipeBlockEntity.COVER);
        return c == null ? super.particle(data) : Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(c).particleMaterial().sprite();
    }
}
