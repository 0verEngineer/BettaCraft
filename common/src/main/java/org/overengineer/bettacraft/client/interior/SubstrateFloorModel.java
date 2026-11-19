package org.overengineer.bettacraft.client.interior;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.client.ClientPlatform;
import org.overengineer.bettacraft.platform.client.WrappedModel;
import org.overengineer.bettacraft.platform.model.ModelValues;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.interior.InteriorFloorBlockEntity;

/**
 * Interior floor with substrate (PLAN 7.2): the floor block shows the model of its substrate block (from the block
 * entity's model data); tint (e.g. grass) is passed through to the substrate's block colour.
 */
public final class SubstrateFloorModel extends WrappedModel {
    public SubstrateFloorModel(BlockStateModel original) {
        super(original);
    }

    @Nullable
    private static BlockState substrate(ModelValues data) {
        return data.get(InteriorFloorBlockEntity.SUBSTRATE_STATE);
    }

    @Override
    public List<BakedQuad> quads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelValues data, @Nullable ChunkSectionLayer renderType) {
        BlockState s = substrate(data);
        if (s == null) return super.quads(state, side, rand, data, renderType);
        return ClientPlatform.get().quads(Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(s), s, side, rand, renderType);
    }

    @Override
    public java.util.Collection<ChunkSectionLayer> layers(BlockState state, RandomSource rand, ModelValues data) {
        BlockState s = substrate(data);
        if (s == null) return super.layers(state, rand, data);
        return ClientPlatform.get().layers(Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(s), s, rand);
    }

    @Override
    public net.minecraft.client.renderer.texture.TextureAtlasSprite particle(ModelValues data) {
        BlockState s = substrate(data);
        return s == null ? super.particle(data) : Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(s).particleMaterial().sprite();
    }

    /** The floor is tinted like its substrate (grass, moss). */
    public static int blockColor(BlockState state, @Nullable net.minecraft.client.renderer.block.BlockAndTintGetter level, @Nullable net.minecraft.core.BlockPos pos, int tint) {
        if (level == null || pos == null || !(level.getBlockEntity(pos) instanceof InteriorFloorBlockEntity be)) return -1;
        return org.overengineer.bettacraft.client.ClientSetup.tint(be.substrate(), level, pos, tint);
    }
}
