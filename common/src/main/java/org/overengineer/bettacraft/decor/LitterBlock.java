package org.overengineer.bettacraft.decor;

import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.util.RandomSource;
import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * M28: leaf litter on the floor: Indian almond leaves (placed with the {@code almond_leaf} item) and alder cones. In a
 * tank interior they give off tannins (lower the pH a little, colour the water) and the shrimp graze on them
 * ({@code features.decor.effects}, counted in {@link org.overengineer.bettacraft.tank.interior.InteriorEquipment}). They
 * lie on any block, drop their item and have no collision (the fish swim over them).
 */
public class LitterBlock extends Block {
    public static final MapCodec<LitterBlock> CODEC = simpleCodec(p -> new LitterBlock(p, () -> net.minecraft.world.item.Items.AIR, Block.box(0, 0, 0, 16, 1, 16)));

    private final Supplier<? extends Item> item;
    private final VoxelShape shape;

    public LitterBlock(Properties properties, Supplier<? extends Item> item, VoxelShape shape) {
        super(properties);
        this.item = item;
        this.shape = shape;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public Item item() {
        return item.get();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shape;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return !below.isAir() && !below.is(Blocks.WATER) && Block.isFaceFull(below.getCollisionShape(level, pos.below()), Direction.UP)
                || below.is(org.overengineer.bettacraft.registry.ModBlocks.INTERIOR_FLOOR.get());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess, BlockPos pos, Direction dir, BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        return dir == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, level, tickAccess, pos, dir, neighbourPos, neighbour, random);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(item.get());
    }
}
