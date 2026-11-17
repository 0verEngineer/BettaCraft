package org.overengineer.bettacraft.client.model;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
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
 */
public final class FilterBundleModel extends WrappedModel {
    private volatile BakedModel[][] arms, tees;

    public FilterBundleModel(BakedModel original) {
        super(original);
    }

    /** The arm models, looked up once after the models are baked (extra models, {@link ModelSwaps#EXTRA}). */
    private void resolveArms() {
        if (arms != null) return;
        BakedModel[][] a = new BakedModel[PipeLine.values().length][6], t = new BakedModel[PipeLine.values().length][6];
        for (PipeLine l : PipeLine.values()) for (Direction d : Direction.values()) {
            a[l.ordinal()][d.ordinal()] = ClientPlatform.get().standaloneModel(armModel(l, d, false));
            t[l.ordinal()][d.ordinal()] = ClientPlatform.get().standaloneModel(armModel(l, d, true));
        }
        tees = t;
        arms = a;
    }

    public static ResourceLocation armModel(PipeLine l, Direction d, boolean tee) {
        return BettaCraft.id("block/filter_pipe_bundle_" + l.getSerializedName() + "_" + d.getSerializedName() + (tee ? "_tee" : ""));
    }

    @Override
    public List<BakedQuad> quads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelValues data, @Nullable RenderType renderType) {
        BlockState c = data.get(FilterPipeBlockEntity.COVER);
        if (c != null) return ClientPlatform.get().quads(Minecraft.getInstance().getBlockRenderer().getBlockModel(c), c, side, rand, renderType);
        List<BakedQuad> out = new ArrayList<>(super.quads(state, side, rand, data, renderType));
        Integer lines = data.get(FilterPipeBlockEntity.LINES);
        if (lines == null) return out;
        resolveArms();
        for (PipeLine l : PipeLine.values()) {
            int slot = FilterPipes.slot(lines, l);
            if (!FilterPipes.present(slot)) continue;
            for (Direction d : Direction.values()) {
                if (!FilterPipes.joined(slot, d)) continue;
                BakedModel a = arms[l.ordinal()][d.ordinal()];
                if (a != null) out.addAll(ClientPlatform.get().quads(a, state, side, rand, renderType));
                BakedModel t = FilterPipes.junction(slot) ? tees[l.ordinal()][d.ordinal()] : null;
                if (t != null) out.addAll(ClientPlatform.get().quads(t, state, side, rand, renderType));
            }
        }
        return out;
    }

    @Override
    public java.util.Collection<RenderType> layers(BlockState state, RandomSource rand, ModelValues data) {
        BlockState c = data.get(FilterPipeBlockEntity.COVER);
        if (c == null) return super.layers(state, rand, data);
        return ClientPlatform.get().layers(Minecraft.getInstance().getBlockRenderer().getBlockModel(c), c, rand);
    }

    @Override
    public net.minecraft.client.renderer.texture.TextureAtlasSprite particle(ModelValues data) {
        BlockState c = data.get(FilterPipeBlockEntity.COVER);
        return c == null ? super.particle(data) : Minecraft.getInstance().getBlockRenderer().getBlockModel(c).getParticleIcon();
    }
}
