package org.overengineer.bettacraft.terrarium;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.equipment.TankLightBlock;

/**
 * M31: basking lamp (heat and UV, like a mercury vapour lamp): a light-type device on a top block of the tank (or hanging
 * from a ceiling above it). It is a {@link TankLightBlock} of its own kind, one block per lamp: it shines with light 15, follows
 * the tank's dimmer and daylight timer, redstone switches it off and {@code power.lightRequiresEnergy} gives it the
 * tank's FE like the other lights. Lit on a terrarium or paludarium it makes a basking spot on the land under it (warmer
 * than the air by {@code terrarium.baskingSpotLift}) with UV ({@code terrarium.baskingLampUv}) and warms the air a little.
 */
public class BaskingLampBlock extends TankLightBlock {
    public static final MapCodec<BaskingLampBlock> CODEC = simpleCodec(BaskingLampBlock::new);
    private static final VoxelShape STANDING = box(3, 0, 3, 13, 8, 13), HANGING_SHAPE = box(3, 8, 3, 13, 16, 13);

    public BaskingLampBlock(Properties properties) {
        super(properties);
    }

    /** M64: only the bar lies on a lid. */
    @Override
    protected boolean sitsOnLids() {
        return false;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HANGING) ? HANGING_SHAPE : STANDING;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(AXIS, Direction.Axis.X).setValue(HANGING, ctx.getClickedFace() == Direction.DOWN)
                .setValue(POWERED, ctx.getLevel().hasNeighborSignal(ctx.getClickedPos()));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state;
    }

    /** One lamp is one fixture (it never joins its neighbours). */
    @Override
    protected List<BlockPos> fixture(Level level, BlockPos pos, BlockState state) {
        return List.of(pos);
    }

    /** No colours: a dye does nothing. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(this);
    }
}
