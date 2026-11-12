package org.overengineer.bettacraft.client.interior;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;

/**
 * Block view over a {@link ClientRegion} in interior-local coordinates, used for meshing the miniature.
 * Light and biome tint are taken from the real tank position (the miniature "inherits" the tank's light).
 */
final class RegionBlockGetter implements BlockAndTintGetter {
    private final ClientRegion region;
    private final ClientLevel level;
    private final BlockPos tankMin;
    private final BlockPos.MutableBlockPos tmp = new BlockPos.MutableBlockPos();

    RegionBlockGetter(ClientRegion region, ClientLevel level, BlockPos tankMin) {
        this.region = region;
        this.level = level;
        this.tankMin = tankMin;
    }

    private BlockPos tankPos(BlockPos local) {
        int s = region.scale;
        return tmp.set(tankMin.getX() + Math.floorDiv(local.getX(), s), tankMin.getY() + Math.floorDiv(local.getY(), s),
                tankMin.getZ() + Math.floorDiv(local.getZ(), s));
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return region.get(pos.getX(), pos.getY(), pos.getZ());
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
        return level.getBrightness(layer, tankPos(pos));
    }

    @Override
    public int getRawBrightness(BlockPos pos, int amount) {
        return level.getRawBrightness(tankPos(pos), amount);
    }

    @Override
    public boolean canSeeSky(BlockPos pos) {
        return level.canSeeSky(tankPos(pos));
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        return level.getShade(direction, shade);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return level.getLightEngine();
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        return level.getBlockTint(tankPos(pos).immutable(), resolver);
    }

    @Override
    public int getHeight() {
        return region.sizeY;
    }

    @Override
    public int getMinBuildHeight() {
        return 0;
    }
}
