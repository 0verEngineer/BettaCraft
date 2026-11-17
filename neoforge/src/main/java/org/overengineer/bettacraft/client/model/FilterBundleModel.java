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
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
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
public final class FilterBundleModel extends BakedModelWrapper<BakedModel> {
    private final BakedModel[][] arms, tees;

    private FilterBundleModel(BakedModel original, BakedModel[][] arms, BakedModel[][] tees) {
        super(original);
        this.arms = arms;
        this.tees = tees;
    }

    public static ResourceLocation armModel(PipeLine l, Direction d, boolean tee) {
        return BettaCraft.id("block/filter_pipe_bundle_" + l.getSerializedName() + "_" + d.getSerializedName() + (tee ? "_tee" : ""));
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        BlockState c = data.get(org.overengineer.bettacraft.neoforge.NeoModelData.prop(FilterPipeBlockEntity.COVER));
        if (c != null) return Minecraft.getInstance().getBlockRenderer().getBlockModel(c).getQuads(c, side, rand, ModelData.EMPTY, renderType);
        List<BakedQuad> out = new ArrayList<>(super.getQuads(state, side, rand, data, renderType));
        Integer lines = data.get(org.overengineer.bettacraft.neoforge.NeoModelData.prop(FilterPipeBlockEntity.LINES));
        if (lines == null) return out;
        for (PipeLine l : PipeLine.values()) {
            int slot = FilterPipes.slot(lines, l);
            if (!FilterPipes.present(slot)) continue;
            for (Direction d : Direction.values()) {
                if (!FilterPipes.joined(slot, d)) continue;
                BakedModel a = arms[l.ordinal()][d.ordinal()];
                if (a != null) out.addAll(a.getQuads(state, side, rand, ModelData.EMPTY, renderType));
                BakedModel t = FilterPipes.junction(slot) ? tees[l.ordinal()][d.ordinal()] : null;
                if (t != null) out.addAll(t.getQuads(state, side, rand, ModelData.EMPTY, renderType));
            }
        }
        return out;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        BlockState c = data.get(org.overengineer.bettacraft.neoforge.NeoModelData.prop(FilterPipeBlockEntity.COVER));
        if (c == null) return super.getRenderTypes(state, rand, data);
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(c).getRenderTypes(c, rand, ModelData.EMPTY);
    }

    @Override
    public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon(ModelData data) {
        BlockState c = data.get(org.overengineer.bettacraft.neoforge.NeoModelData.prop(FilterPipeBlockEntity.COVER));
        return c == null ? super.getParticleIcon(data) : Minecraft.getInstance().getBlockRenderer().getBlockModel(c).getParticleIcon(ModelData.EMPTY);
    }

    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        for (PipeLine l : PipeLine.values()) for (Direction d : Direction.values()) for (boolean tee : new boolean[]{false, true})
            event.register(ModelResourceLocation.standalone(armModel(l, d, tee)));
    }

    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        BakedModel[][] arms = new BakedModel[PipeLine.values().length][6], tees = new BakedModel[PipeLine.values().length][6];
        for (PipeLine l : PipeLine.values()) for (Direction d : Direction.values()) {
            arms[l.ordinal()][d.ordinal()] = event.getModels().get(ModelResourceLocation.standalone(armModel(l, d, false)));
            tees[l.ordinal()][d.ordinal()] = event.getModels().get(ModelResourceLocation.standalone(armModel(l, d, true)));
        }
        for (BlockState s : ModBlocks.FILTER_PIPE_BUNDLE.get().getStateDefinition().getPossibleStates()) {
            ModelResourceLocation loc = BlockModelShaper.stateToModelLocation(s);
            BakedModel m = event.getModels().get(loc);
            if (m != null && !(m instanceof FilterBundleModel)) event.getModels().put(loc, new FilterBundleModel(m, arms, tees));
        }
    }
}
