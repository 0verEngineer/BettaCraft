package org.overengineer.bettacraft.client.tank;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
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
    public static final Identifier STILL = BettaCraft.id("block/tank_water_still");
    /** Side faces: same animation, clearer (alpha 90 instead of 140). */
    public static final Identifier SIDE = BettaCraft.id("block/tank_water_side");
    /**
     * RGB tint. M5: the side faces (looked through at the fish) are clearer and less saturated than the surface, so the
     * fish keep their colours (BettaTints compensates the rest). How see-through the water is lives in the alpha of the
     * textures (surface 140, sides 90), not in the vertex colour: shader packs such as Complementary ignore vertex alpha
     * (Iris uses it for ambient occlusion) and drew the water opaque.
     */
    private static final int COLOR = 0xFF4A90E2;
    private static final int SIDE_COLOR = 0xFF6CA6DC;
    private static final float INSET = TankWaterGeometry.INSET;

    /**
     * Dev smoke test only ("before" pictures of the water seam): the M10 geometry, every face inset on all outer sides,
     * and no split of the glass. Never set in normal play.
     */
    public static volatile boolean m10Look;

    private TankWaterRenderer() {}

    /**
     * Receives one water face: direction, 4 corners (x,y,z each, reused between calls), ARGB colour, flipped. Faces along
     * Y use the {@link #STILL} texture, all others {@link #SIDE}.
     */
    public interface FaceSink {
        void face(Direction d, float[] corners, int argb, boolean reversed);
    }

    public static void render(BlockAndTintGetter level, BlockPos pos, VertexConsumer vc, BlockState state) {
        if (state.getValue(TankBlock.FILL) <= 0) return;
        // with a shader pack the tank model draws the water instead (see TankBakedModel#waterQuads)
        if (org.overengineer.bettacraft.client.interior.IrisCompat.shaderPackInUse()) return;
        // M50: a travelling tank (airship) draws its sloshing water itself (the model data of the block says so)
        if (Boolean.TRUE.equals(org.overengineer.bettacraft.platform.client.ClientPlatform.get().modelValue(level, pos, org.overengineer.bettacraft.tank.TankBlockEntity.WATER_DYNAMIC))) return;
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite still = atlas.apply(STILL), side = atlas.apply(SIDE);
        int light = LevelRenderer.getLightColor(level, pos);
        // Section-relative coordinates, like vanilla LiquidBlockRenderer.
        float ox = pos.getX() & 15, oy = pos.getY() & 15, oz = pos.getZ() & 15;
        faces(state, waterAbove(level, pos, state), gapMask(level, pos, state), TankLightTint.colorAt(level, pos), (d, c, col, rev) -> {
            int a = (col >>> 24) & 0xFF, r = (col >> 16) & 0xFF, g = (col >> 8) & 0xFF, b = col & 0xFF;
            emit(vc, d, c, ox, oy, oz, d.getAxis() == Direction.Axis.Y ? still : side, r, g, b, a, light, rev);
        });
    }

    /** The block above belongs to the tank and holds water (no surface in this block). */
    public static boolean waterAbove(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        if (!TankBlock.isConnected(state, Direction.UP)) return false;
        BlockState above = level.getBlockState(pos.above());
        return above.getBlock() instanceof TankBlock && above.getValue(TankBlock.FILL) > 0;
    }

    /**
     * M10 (free shapes): horizontal neighbours (bit = 2D data value) of the same tank whose top is closed by glass; their
     * water ends just below the glass, the thin gap to this column (which goes on upwards) is closed by a face.
     */
    public static int gapMask(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        if (state.getValue(TankBlock.FILL) < 16 || !waterAbove(level, pos, state)) return 0;
        int m = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (!TankBlock.isConnected(state, d)) continue;
            BlockState n = level.getBlockState(pos.relative(d));
            if (n.getBlock() instanceof TankBlock && !TankBlock.isConnected(n, Direction.UP)) m |= 1 << d.get2DDataValue();
        }
        return m;
    }

    /** All water faces of one tank block, in block coordinates (0..1). */
    public static void faces(BlockState state, boolean waterAbove, int gapMask, FaceSink sink) {
        faces(state, waterAbove, gapMask, 0, sink);
    }

    /** M18: the colour of the water mixed with the colour of the tank's lights ({@code light} RGB, 0 = none). */
    public static int color(boolean surface, int light) {
        int base = surface ? COLOR : SIDE_COLOR;
        // M20: salt water is a clearer, more turquoise blue (the salinity rides in the top byte of the light value)
        int salt = TankLightTint.salt(light);
        if (salt > 0) base = 0xFF000000 | org.overengineer.bettacraft.tank.Salinity.waterColor(base & 0xFFFFFF, salt);
        int rgb = TankLightTint.rgb(light);
        return rgb == 0 ? base : org.overengineer.bettacraft.equipment.TankLightColors.tintWater(base, rgb, TankLightTint.strength());
    }

    /** All water faces of one tank block; M18: {@code light} = RGB of the tank's coloured lights (0 = none). */
    public static void faces(BlockState state, boolean waterAbove, int gapMask, int light, FaceSink sink) {
        int fill = state.getValue(TankBlock.FILL);
        if (fill <= 0) return;
        TankWaterGeometry.Body body = TankWaterGeometry.body(TankBlock.connectionMask(state), fill, waterAbove);
        float[] c = new float[12];
        for (Direction d : Direction.values()) {
            if (!body.has(d)) continue;
            // M11: inset only along the normal, full width sideways (see TankWaterGeometry: sorting behind stained glass)
            if (m10Look) BoxFaces.corners(d, body.x0(), body.y0(), body.z0(), body.x1(), body.y1(), body.z1(), c);
            else TankWaterGeometry.face(body, d, c);
            int col = color(d.getAxis() == Direction.Axis.Y, light);
            sink.face(d, c, col, false);
            // visible from below the surface too (swimming players)
            if (d == Direction.UP) sink.face(d, c, col, true);
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if ((gapMask & (1 << d.get2DDataValue())) == 0) continue;
            // as wide as the water of this block (inset at the outer sides, like the faces)
            BoxFaces.corners(d, body.x0(), 1 - INSET, body.z0(), body.x1(), 1, body.z1(), c);
            sink.face(d, c, color(false, light), false);
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
