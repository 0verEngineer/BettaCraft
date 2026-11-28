package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.overengineer.bettacraft.client.tank.AlgaeFilm;
import org.overengineer.bettacraft.client.tank.TankWaterRenderer;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;

/**
 * Algae on the inside of the glass (M14) of the tanks whose water the block entity renderer draws too (a Create contraption,
 * a travelling tank); every other tank has its algae in the chunk mesh ({@link AlgaeFilm}).
 * <p>
 * M67.5: the same film as the chunk mesh: per tank block the film quads of {@link TankWaterRenderer#faces} (the corners of
 * the water's side faces), the levels of {@link AlgaeFilm#pack} (the far wall faded by the water in front of it), the block
 * atlas film textures with the film's texture placement. They go into the travelling water's batch
 * ({@link TravelWater.Types#WATER}: sorted back to front, no depth written), so the film blends with the water in the right
 * order and never hides the glass and water behind it. The front and back film of a face are one plane; only the one that
 * faces the camera is drawn (the chunk mesh does that by back face culling, this batch draws both sides of the water).
 * Before, a separate texture in its own depth writing batch left holes in the back wall behind the film.
 */
public final class AlgaeRenderer {
    private AlgaeRenderer() {}

    /** Dev smoke only: film quads drawn by this renderer so far (the travelling tanks' film). */
    public static volatile int quadsForTest;

    public static void render(TankBlockEntity be, TankData t, PoseStack pose, MultiBufferSource buffers) {
        Level level = be.getLevel();
        if (level == null || t.algae().isEmpty() || !(be.onContraption() || TankBlockEntity.DYNAMIC_WATER.test(be))) return;
        if (AlgaeFilm.hiddenForTest) return;
        BlockPos origin = be.getBlockPos();
        int seed = AlgaeFilm.seedOf(origin);
        var atlas = org.overengineer.bettacraft.client.ClientCompat.blockSprites();
        PoseStack.Pose last = pose.last();
        VertexConsumer[] vc = new VertexConsumer[1];
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        Vector3f centre = new Vector3f(), normal = new Vector3f();
        for (var e : t.algae().long2ObjectEntrySet()) {
            long key = e.getLongKey();
            if (!t.contains(key)) continue;
            int packed = AlgaeFilm.pack(t, key, e.getValue());
            if (packed == 0) continue;
            p.set(key);
            BlockState s = level.getBlockState(p);
            if (!(s.getBlock() instanceof TankBlock) || s.getValue(TankBlock.FILL) <= 0) continue;
            BlockPos at = p.immutable();
            int light = net.minecraft.client.renderer.LevelRenderer.getLightCoords(level, at);
            float ox = at.getX() - origin.getX(), oy = at.getY() - origin.getY(), oz = at.getZ() - origin.getZ();
            TankWaterRenderer.faces(s, waterAbove(level, at, s), 0, 0, packed, new TankWaterRenderer.FaceSink() {
                @Override
                public void face(Direction d, float[] c, int argb, boolean reversed) {}

                @Override
                public void film(Direction d, float[] c, int lvl, boolean rev) {
                    // only the side towards the camera (the pose is camera-relative: a quad faces the camera when its normal
                    // points against the vector from the camera to it)
                    float nx = rev ? -d.getStepX() : d.getStepX(), nz = rev ? -d.getStepZ() : d.getStepZ();
                    centre.set(ox + (c[0] + c[6]) / 2, oy + (c[1] + c[7]) / 2, oz + (c[2] + c[8]) / 2);
                    last.pose().transformPosition(centre);
                    last.normal().transform(normal.set(nx, 0, nz));
                    if (centre.dot(normal) >= 0) return;
                    if (vc[0] == null) vc[0] = buffers.getBuffer(TravelWater.Types.WATER);
                    quadsForTest++;
                    TextureAtlasSprite sprite = atlas.apply(AlgaeFilm.SPRITES[lvl]);
                    for (int k = 0; k < 4; k++) {
                        int i = rev ? 3 - k : k;
                        float x = c[i * 3], y = c[i * 3 + 1], z = c[i * 3 + 2];
                        vc[0].addVertex(last, ox + x, oy + y, oz + z).setColor(255, 255, 255, 255)
                                .setUv(sprite.getU(AlgaeFilm.u(d, at, seed, x, y, z)), sprite.getV(AlgaeFilm.v(at, seed, d, y)))
                                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, nx, 0, nz);
                    }
                }
            });
        }
    }

    /** The block above belongs to the tank and holds water (like {@link TankWaterRenderer#waterAbove}, for the tank's own level). */
    private static boolean waterAbove(Level level, BlockPos pos, BlockState state) {
        if (!TankBlock.isConnected(state, Direction.UP)) return false;
        BlockState above = level.getBlockState(pos.above());
        return above.getBlock() instanceof TankBlock && above.getValue(TankBlock.FILL) > 0;
    }
}
