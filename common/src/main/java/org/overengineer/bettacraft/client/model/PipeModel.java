package org.overengineer.bettacraft.client.model;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.client.ClientPlatform;
import org.overengineer.bettacraft.platform.client.WrappedModel;
import org.overengineer.bettacraft.platform.model.ModelKey;
import org.overengineer.bettacraft.platform.model.ModelValues;
import org.overengineer.bettacraft.plumbing.PipeBlockEntity;
import org.overengineer.bettacraft.plumbing.PipeSlots;

/**
 * M68: the model of the glass and the steel pipe. The pipe itself is its multipart model (the M65 arms and core, in the
 * texture of its ring: {@code PipeModels} of the datagen picks them by the block state's sides and ring). On top: a steel cap on
 * every side closed with the pipe wrench where something is that the pipe would join ({@link PipeQuads#capWanted}), so a
 * closed joint is seen. With a cover (M40 part c) the pipe is drawn as that block.
 */
public final class PipeModel extends WrappedModel {
    /** The closed sides that show their cap (bit = {@code 1 << direction.get3DDataValue()}), read from the level while meshing. */
    public static final ModelKey<Integer> CAPS = new ModelKey<>("pipe_caps");

    public PipeModel(BakedModel original) {
        super(original);
    }

    @Override
    public ModelValues modelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelValues data) {
        Integer closed = data.get(PipeBlockEntity.CLOSED);
        if (closed == null || closed == 0 || data.get(PipeBlockEntity.COVER) != null) return data;
        int caps = 0;
        for (Direction d : Direction.values()) if ((closed & PipeSlots.bit(d)) != 0 && PipeQuads.capWanted(level, pos, d)) caps |= PipeSlots.bit(d);
        return caps == 0 ? data : data.derive().with(CAPS, caps).build();
    }

    @Override
    public List<BakedQuad> quads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelValues data, @Nullable RenderType renderType) {
        BlockState c = data.get(PipeBlockEntity.COVER);
        if (c != null) return ClientPlatform.get().quads(Minecraft.getInstance().getBlockRenderer().getBlockModel(c), c, side, rand, renderType);
        List<BakedQuad> base = super.quads(state, side, rand, data, renderType);
        Integer caps = data.get(CAPS);
        if (caps == null || caps == 0 || side != null) return base;
        List<BakedQuad> out = new ArrayList<>(base);
        for (Direction d : Direction.values()) if ((caps & PipeSlots.bit(d)) != 0) out.addAll(PipeQuads.pipeCap(d));
        return out;
    }

    @Override
    public java.util.Collection<RenderType> layers(BlockState state, RandomSource rand, ModelValues data) {
        BlockState c = data.get(PipeBlockEntity.COVER);
        if (c == null) return super.layers(state, rand, data);
        return ClientPlatform.get().layers(Minecraft.getInstance().getBlockRenderer().getBlockModel(c), c, rand);
    }

    @Override
    public net.minecraft.client.renderer.texture.TextureAtlasSprite particle(ModelValues data) {
        BlockState c = data.get(PipeBlockEntity.COVER);
        return c == null ? super.particle(data) : Minecraft.getInstance().getBlockRenderer().getBlockModel(c).getParticleIcon();
    }
}
