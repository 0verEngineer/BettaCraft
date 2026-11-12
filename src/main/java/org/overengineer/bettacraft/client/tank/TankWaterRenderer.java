package org.overengineer.bettacraft.client.tank;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.tank.TankBlock;

/**
 * Our own water for tank blocks (PLAN 3.3): flat surface at the exact fill height, no flow, no slope, fixed colour
 * (no biome tint), only outer faces of the water body, 0.5 px inside the glass to avoid z-fighting (M11: inset only
 * along the normal, so the faces sort behind translucent glass, see {@link TankWaterGeometry}). Works per
 * block from the block states, so any tank shape (M10) renders: each layer has one fill height.
 * Called from the chunk mesher via {@link TankAwareWaterExtensions#renderFluid}; also used by Sodium 0.8.
 */
public final class TankWaterRenderer {
    public static final ResourceLocation STILL = BettaCraft.id("block/tank_water_still");
    /**
     * ARGB; alpha controls how see-through the water is. M5: the side faces (looked through at the fish) are clearer and
     * less saturated than the surface, so the fish keep their colours (BettaTints compensates the rest).
     */
    private static final int COLOR = 0x8C4A90E2;
    private static final int SIDE_COLOR = 0x5A6CA6DC;
    private static final float INSET = TankWaterGeometry.INSET;

    /**
     * Dev smoke test only ("before" pictures of the water seam): the M10 geometry, every face inset on all outer sides,
     * and no split of the glass. Never set in normal play.
     */
    public static volatile boolean m10Look;

    private TankWaterRenderer() {}

    public static void render(BlockAndTintGetter level, BlockPos pos, VertexConsumer vc, BlockState state) {
        int fill = state.getValue(TankBlock.FILL);
        if (fill <= 0) return;
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(STILL);
        int light = LevelRenderer.getLightColor(level, pos);

        boolean up = TankBlock.isConnected(state, Direction.UP);
        BlockState above = up ? level.getBlockState(pos.above()) : null;
        boolean waterAbove = above != null && above.getBlock() instanceof TankBlock && above.getValue(TankBlock.FILL) > 0;
        TankWaterGeometry.Body body = TankWaterGeometry.body(TankBlock.connectionMask(state), fill, waterAbove);
        float x0 = body.x0(), y0 = body.y0(), z0 = body.z0(), x1 = body.x1(), y1 = body.y1(), z1 = body.z1();

        // Section-relative coordinates, like vanilla LiquidBlockRenderer.
        float ox = pos.getX() & 15, oy = pos.getY() & 15, oz = pos.getZ() & 15;
        float[] c = new float[12];

        for (Direction d : Direction.values()) {
            if (!body.has(d)) continue;
            // M11: inset only along the normal, full width sideways (see TankWaterGeometry: sorting behind stained glass)
            if (m10Look) BoxFaces.corners(d, body.x0(), body.y0(), body.z0(), body.x1(), body.y1(), body.z1(), c);
            else TankWaterGeometry.face(body, d, c);
            int col = d.getAxis() == Direction.Axis.Y ? COLOR : SIDE_COLOR;
            int a = (col >>> 24) & 0xFF, r = (col >> 16) & 0xFF, g = (col >> 8) & 0xFF, b = col & 0xFF;
            emit(vc, d, c, ox, oy, oz, sprite, r, g, b, a, light, false);
            if (d == Direction.UP) {
                // visible from below the surface too (swimming players)
                emit(vc, d, c, ox, oy, oz, sprite, r, g, b, a, light, true);
            }
        }

        // M10 (free shapes): a connected neighbour of the same layer whose top is closed by glass ends its water just
        // below the glass; close the thin gap between it and this column, which goes on upwards
        if (waterAbove && fill >= 16) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (!TankBlock.isConnected(state, d)) continue;
                BlockState n = level.getBlockState(pos.relative(d));
                if (!(n.getBlock() instanceof TankBlock) || TankBlock.isConnected(n, Direction.UP)) continue;
                BoxFaces.corners(d, 0, 1 - INSET, 0, 1, 1, 1, c);
                int a = (SIDE_COLOR >>> 24) & 0xFF, r = (SIDE_COLOR >> 16) & 0xFF, g = (SIDE_COLOR >> 8) & 0xFF, b = SIDE_COLOR & 0xFF;
                emit(vc, d, c, ox, oy, oz, sprite, r, g, b, a, light, false);
            }
        }
    }

    private static void emit(VertexConsumer vc, Direction d, float[] c, float ox, float oy, float oz, TextureAtlasSprite sprite,
                             int r, int g, int b, int a, int light, boolean reversed) {
        float nx = d.getStepX(), ny = d.getStepY(), nz = d.getStepZ();
        if (reversed) {
            nx = -nx; ny = -ny; nz = -nz;
        }
        for (int k = 0; k < 4; k++) {
            int i = reversed ? 3 - k : k;
            float x = c[i * 3], y = c[i * 3 + 1], z = c[i * 3 + 2];
            vc.addVertex(ox + x, oy + y, oz + z)
                    .setColor(r, g, b, a)
                    .setUv(sprite.getU(BoxFaces.u(d, x, y, z)), sprite.getV(BoxFaces.v(d, x, y, z)))
                    .setLight(light)
                    .setNormal(nx, ny, nz);
        }
    }
}
