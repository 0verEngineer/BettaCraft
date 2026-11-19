package org.overengineer.bettacraft.client.interior;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.overengineer.bettacraft.tank.interior.InteriorSnapshot;

/**
 * Block view over an (immutable) snapshot of a {@link ClientRegion} in interior-local coordinates, used for meshing
 * the miniature on a worker thread. Light comes from a per-tank-block copy taken on the render thread (the miniature
 * "inherits" the tank's light); biome tint is read from the level at the tank position (thread-safe tint cache, as
 * vanilla's section compiler does).
 */
final class RegionBlockGetter implements BlockAndTintGetter {
    private final Long2ObjectOpenHashMap<InteriorSnapshot.Section> sections;
    private final int sizeX, sizeY, sizeZ, scale;
    private final ClientLevel level;
    private final BlockPos tankMin;
    private final int tx, ty, tz;
    /** Per tank block: block light << 4 | sky light. */
    private final byte[] light;

    RegionBlockGetter(Long2ObjectOpenHashMap<InteriorSnapshot.Section> sections, int sizeX, int sizeY, int sizeZ, int scale,
                      ClientLevel level, BlockPos tankMin, int tx, int ty, int tz, byte[] light) {
        this.sections = sections;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.scale = scale;
        this.level = level;
        this.tankMin = tankMin;
        this.tx = tx;
        this.ty = ty;
        this.tz = tz;
        this.light = light;
    }

    /** Copies the light of all tank blocks (render thread). */
    static byte[] captureLight(ClientLevel level, BlockPos min, int tx, int ty, int tz) {
        byte[] out = new byte[tx * ty * tz];
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = 0; y < ty; y++)
            for (int z = 0; z < tz; z++)
                for (int x = 0; x < tx; x++) {
                    p.set(min.getX() + x, min.getY() + y, min.getZ() + z);
                    out[(y * tz + z) * tx + x] = (byte) (level.getBrightness(LightLayer.BLOCK, p) << 4 | level.getBrightness(LightLayer.SKY, p));
                }
        return out;
    }

    private int lightAt(BlockPos local) {
        int x = Math.clamp(Math.floorDiv(local.getX(), scale), 0, tx - 1);
        int y = Math.clamp(Math.floorDiv(Math.max(0, local.getY()), scale), 0, ty - 1);
        int z = Math.clamp(Math.floorDiv(local.getZ(), scale), 0, tz - 1);
        return light[(y * tz + z) * tx + x] & 0xFF;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return ClientRegion.get(sections, pos.getX(), pos.getY(), pos.getZ(), sizeX, sizeY, sizeZ);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public int getBrightness(LightLayer layer, BlockPos pos) {
        int l = lightAt(pos);
        return layer == LightLayer.BLOCK ? l >> 4 : l & 15;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int amount) {
        int l = lightAt(pos);
        return Math.max(l >> 4, (l & 15) - amount);
    }

    @Override
    public boolean canSeeSky(BlockPos pos) {
        return (lightAt(pos) & 15) >= 15;
    }

    /** M52c (26.1): the face shading of the level ({@code getShade} became the level's cardinal lighting). */
    @Override
    public net.minecraft.world.level.CardinalLighting cardinalLighting() {
        return level.cardinalLighting();
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return level.getLightEngine();
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        BlockPos t = new BlockPos(tankMin.getX() + Math.clamp(Math.floorDiv(pos.getX(), scale), 0, tx - 1),
                tankMin.getY() + Math.clamp(Math.floorDiv(Math.max(0, pos.getY()), scale), 0, ty - 1),
                tankMin.getZ() + Math.clamp(Math.floorDiv(pos.getZ(), scale), 0, tz - 1));
        return level.getBlockTint(t, resolver);
    }

    @Override
    public int getHeight() {
        return sizeY + 1;
    }

    @Override
    public int getMinY() {
        return -1;
    }
}
