package org.overengineer.bettacraft.client.interior;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.interior.InteriorFloorBlockEntity;

/**
 * Interior floor with substrate (PLAN 7.2): the floor block shows the model of its substrate block (from the block
 * entity's model data); tint (e.g. grass) is passed through to the substrate's block colour.
 */
public final class SubstrateFloorModel extends BakedModelWrapper<BakedModel> {
    public SubstrateFloorModel(BakedModel original) {
        super(original);
    }

    @Nullable
    private static BlockState substrate(ModelData data) {
        return data.get(InteriorFloorBlockEntity.SUBSTRATE_STATE);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        BlockState s = substrate(data);
        if (s == null) return super.getQuads(state, side, rand, data, renderType);
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(s).getQuads(s, side, rand, ModelData.EMPTY, renderType);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        BlockState s = substrate(data);
        if (s == null) return super.getRenderTypes(state, rand, data);
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(s).getRenderTypes(s, rand, ModelData.EMPTY);
    }

    @Override
    public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon(ModelData data) {
        BlockState s = substrate(data);
        return s == null ? super.getParticleIcon(data) : Minecraft.getInstance().getBlockRenderer().getBlockModel(s).getParticleIcon(ModelData.EMPTY);
    }

    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        for (BlockState s : ModBlocks.INTERIOR_FLOOR.get().getStateDefinition().getPossibleStates()) {
            ModelResourceLocation loc = BlockModelShaper.stateToModelLocation(s);
            BakedModel m = event.getModels().get(loc);
            if (m != null && !(m instanceof SubstrateFloorModel)) event.getModels().put(loc, new SubstrateFloorModel(m));
        }
    }

    public static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> {
            if (level == null || pos == null || !(level.getBlockEntity(pos) instanceof InteriorFloorBlockEntity be)) return -1;
            return Minecraft.getInstance().getBlockColors().getColor(be.substrate(), level, pos, tint);
        }, ModBlocks.INTERIOR_FLOOR.get());
    }
}
