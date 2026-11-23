package org.overengineer.bettacraft.equipment;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Glass lid (M9): a thin pane lying on a tank block. Every top block of a tank that carries a lid (or the feeder, a
 * light or any full block) does not lose water by evaporation. Purely a cover, no block entity.
 */
public class TankLidBlock extends Block {
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 1, 16);

    public TankLidBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * M64: the aquarium light bar put onto the top of a lid takes the lid's place and lies on it (the lid stays part of the
     * block, {@link TankLightBlock#LID}); only on a tank, only from above, with {@code features.equipment.lightOnLid}.
     */
    @Override
    protected boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext ctx) {
        if (!(ctx.getItemInHand().getItem() instanceof net.minecraft.world.item.BlockItem bi) || bi.getBlock() != org.overengineer.bettacraft.registry.ModBlocks.TANK_LIGHT.get())
            return false;
        if (ctx.getClickedFace() != net.minecraft.core.Direction.UP || !TankLightBlock.lidAllowed()) return false;
        return ctx.getLevel().getBlockState(ctx.getClickedPos().below()).getBlock() instanceof org.overengineer.bettacraft.tank.TankBlock;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }
}
