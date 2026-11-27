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
import org.overengineer.bettacraft.plumbing.PipeBundleBlock;
import org.overengineer.bettacraft.plumbing.PipeSlots;

/**
 * M40 part b, M68: the model of the pipe bundle. The block state's model is the steel manifold; for every pipe of the bundle
 * ({@link PipeBlockEntity#SLOTS}) its arms on the joined sides are added ({@link PipeQuads#bundleArm}: glass or steel, with the
 * ring of its network), and a cap where a side of it is closed with the wrench and something is there. Glass arms go into the
 * translucent layer, the rest stays solid. M65: on the sides where one pipe alone meets a single pipe its arm is the one in the
 * middle of the face ({@link PipeBundleBlock#centredSides}, read from the neighbours while meshing). With a cover the bundle is
 * drawn as that block.
 */
public final class PipeBundleModel extends WrappedModel {
    public static final ModelKey<Integer> CENTRED = new ModelKey<>("bundle_centred");
    /** Per slot six bits: its closed sides that show a cap. */
    public static final ModelKey<Integer> CAPS = new ModelKey<>("bundle_caps");

    public PipeBundleModel(BakedModel original) {
        super(original);
    }

    @Override
    public ModelValues modelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelValues data) {
        int[] slots = data.get(PipeBlockEntity.SLOTS);
        if (slots == null || data.get(PipeBlockEntity.COVER) != null) return data;
        int centred = PipeBundleBlock.centredSides(level, pos, slots);
        int caps = 0;
        for (int i = 0; i < slots.length; i++) {
            if (!PipeSlots.present(slots[i])) continue;
            for (Direction d : Direction.values())
                if (PipeSlots.closed(slots[i], d) && PipeQuads.capWanted(level, pos, d)) caps |= PipeSlots.bit(d) << (i * 6);
        }
        if (centred == 0 && caps == 0) return data;
        return data.derive().with(CENTRED, centred).with(CAPS, caps).build();
    }

    private static boolean anyGlass(@Nullable int[] slots) {
        if (slots == null) return false;
        for (int s : slots) if (PipeSlots.present(s) && !PipeSlots.steel(s)) return true;
        return false;
    }

    @Override
    public List<BakedQuad> quads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelValues data, @Nullable RenderType renderType) {
        BlockState c = data.get(PipeBlockEntity.COVER);
        if (c != null) return ClientPlatform.get().quads(Minecraft.getInstance().getBlockRenderer().getBlockModel(c), c, side, rand, renderType);
        boolean translucent = renderType == RenderType.translucent();
        List<BakedQuad> out = new ArrayList<>(translucent ? List.of() : super.quads(state, side, rand, data, renderType));
        int[] slots = data.get(PipeBlockEntity.SLOTS);
        if (slots == null || side != null) return out;
        Integer cd = data.get(CENTRED), cp = data.get(CAPS);
        int centred = cd == null ? 0 : cd, caps = cp == null ? 0 : cp;
        for (int i = 0; i < slots.length; i++) {
            int v = slots[i];
            if (!PipeSlots.present(v)) continue;
            boolean steel = PipeSlots.steel(v);
            // glass in the translucent layer, steel in the solid one (null: everything)
            boolean mine = renderType == null || (steel ? !translucent : translucent);
            for (Direction d : Direction.values()) {
                if (mine && PipeSlots.joined(v, d)) out.addAll(PipeQuads.bundleArm(i, d, (centred & 1 << d.ordinal()) != 0, steel, PipeSlots.ring(v)));
                if (!translucent && (caps & PipeSlots.bit(d) << (i * 6)) != 0) out.addAll(PipeQuads.bundleCap(i, d));
            }
        }
        return out;
    }

    @Override
    public java.util.Collection<RenderType> layers(BlockState state, RandomSource rand, ModelValues data) {
        BlockState c = data.get(PipeBlockEntity.COVER);
        if (c != null) return ClientPlatform.get().layers(Minecraft.getInstance().getBlockRenderer().getBlockModel(c), c, rand);
        if (!anyGlass(data.get(PipeBlockEntity.SLOTS))) return super.layers(state, rand, data);
        return List.of(RenderType.solid(), RenderType.translucent());
    }

    @Override
    public net.minecraft.client.renderer.texture.TextureAtlasSprite particle(ModelValues data) {
        BlockState c = data.get(PipeBlockEntity.COVER);
        return c == null ? super.particle(data) : Minecraft.getInstance().getBlockRenderer().getBlockModel(c).getParticleIcon();
    }
}
