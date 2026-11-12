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
 * (no biome tint), only outer faces of the water body, sides 0.5 px inside the glass to avoid z-fighting.
 * Called from the chunk mesher via {@link TankAwareWaterExtensions#renderFluid}; also used by Sodium 0.8.
 */
public final class TankWaterRenderer {
    public static final ResourceLocation STILL = BettaCraft.id("block/tank_water_still");
    /** ARGB; alpha controls how see-through the water is. */
    private static final int COLOR = 0x7C4A90E2; // M1: clearer water so the fish colours stay visible
    private static final float INSET = 0.5F / 16F;

    private TankWaterRenderer() {}

    public static void render(BlockAndTintGetter level, BlockPos pos, VertexConsumer vc, BlockState state) {
        int fill = state.getValue(TankBlock.FILL);
        if (fill <= 0) return;
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(STILL);
        int light = LevelRenderer.getLightColor(level, pos);

        boolean up = TankBlock.isConnected(state, Direction.UP);
        BlockState above = up ? level.getBlockState(pos.above()) : null;
        boolean waterAbove = above != null && above.getBlock() instanceof TankBlock && above.getValue(TankBlock.FILL) > 0;

        float x0 = TankBlock.isConnected(state, Direction.WEST) ? 0 : INSET;
        float x1 = TankBlock.isConnected(state, Direction.EAST) ? 1 : 1 - INSET;
        float z0 = TankBlock.isConnected(state, Direction.NORTH) ? 0 : INSET;
        float z1 = TankBlock.isConnected(state, Direction.SOUTH) ? 1 : 1 - INSET;
        float y0 = TankBlock.isConnected(state, Direction.DOWN) ? 0 : INSET;
        float y1 = waterAbove ? 1 : Math.min(fill / 16F, 1 - INSET);

        // Section-relative coordinates, like vanilla LiquidBlockRenderer.
        float ox = pos.getX() & 15, oy = pos.getY() & 15, oz = pos.getZ() & 15;
        int a = (COLOR >>> 24) & 0xFF, r = (COLOR >> 16) & 0xFF, g = (COLOR >> 8) & 0xFF, b = COLOR & 0xFF;
        float[] c = new float[12];

        for (Direction d : Direction.values()) {
            boolean render;
            if (d == Direction.UP) {
                render = !waterAbove;
            } else {
                render = !TankBlock.isConnected(state, d);
            }
            if (!render) continue;
            BoxFaces.corners(d, x0, y0, z0, x1, y1, z1, c);
            emit(vc, d, c, ox, oy, oz, sprite, r, g, b, a, light, false);
            if (d == Direction.UP) {
                // visible from below the surface too (swimming players)
                emit(vc, d, c, ox, oy, oz, sprite, r, g, b, a, light, true);
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
