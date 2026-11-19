package org.overengineer.bettacraft.filter;

import org.overengineer.bettacraft.util.BlockRemoval;
import org.overengineer.bettacraft.util.SinkList;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.overengineer.bettacraft.plumbing.PipeConnectable;
import org.overengineer.bettacraft.plumbing.PipeNetwork;
import org.overengineer.bettacraft.registry.ModBlockEntities;

/**
 * M24: a part of the multiblock external filter ({@link MultiblockFilter}): filter chambers (one medium each; stack them
 * and put them side by side: 1 to {@code multiblockFilter.maxChambers}), one pump head on the chambers, and inlet and
 * outlet connectors on the outside (their open side joins the pipes). Right-click a chamber with a medium to put it in,
 * with an empty hand to take it out; the pump head shows the filter.
 */
public class FilterPartBlock extends BaseEntityBlock implements org.overengineer.bettacraft.util.BlockTooltip, BlockRemoval, PipeConnectable {
    /** M40 part b: FRESH and WASTE are the fresh and waste water ports (F1, {@link FilterPortBlockEntity}). */
    public enum Part { CHAMBER, PUMP_HEAD, INLET, OUTLET, FRESH, WASTE }

    /** Inlet / outlet: the side the pipe joins (pointing away from the filter). */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final EnumProperty<FilterMedium> MEDIUM = EnumProperty.create("medium", FilterMedium.class);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private final Part part;

    public FilterPartBlock(Part part, Properties properties) {
        super(properties);
        this.part = part;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(MEDIUM, FilterMedium.NONE).setValue(ACTIVE, false));
    }

    public Part part() {
        return part;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, MEDIUM, ACTIVE);
    }

    /** Connectors face away from the block they are placed against. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getClickedFace());
    }

    @Override
    public boolean pipeConnects(BlockState state, Direction face) {
        return (part == Part.INLET || part == Part.OUTLET) && face == state.getValue(FACING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // M29.1: with a block in hand the part does not open the filter screen: the block is placed against it (a tank on
        // the connectors of a filter under it, pipes at an inlet, more chambers), like against any other block
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem && !(stack.getItem() instanceof FilterMediaItem))
            return InteractionResult.PASS;
        if (part != Part.CHAMBER || !(stack.getItem() instanceof FilterMediaItem)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level.getBlockEntity(pos) instanceof FilterChamberBlockEntity ch)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!ch.medium().isEmpty()) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.bettacraft.chamber.full").withStyle(ChatFormatting.GOLD));
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            ch.setMedium(stack.copyWithCount(1));
            if (!player.getAbilities().instabuild) stack.shrink(1);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ITEM_FRAME_ADD_ITEM, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (part == Part.CHAMBER && level.getBlockEntity(pos) instanceof FilterChamberBlockEntity ch && !ch.medium().isEmpty()) {
            if (!level.isClientSide()) {
                ItemStack out = ch.medium().copy();
                ch.setMedium(ItemStack.EMPTY);
                if (!player.addItem(out)) player.drop(out, false, net.minecraft.util.Prediction.SERVER_ONLY);
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ITEM_FRAME_REMOVE_ITEM, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        BlockPos head = MultiblockFilter.headNear(level, pos);
        if (head != null && level.isClientSide()) org.overengineer.bettacraft.util.ClientHooks.openFilter(head);
        return head != null ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!newState.is(this) && level.getBlockEntity(pos) instanceof FilterChamberBlockEntity ch && !ch.medium().isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), ch.medium().copy());
        }
        PipeNetwork.invalidate(level);
        org.overengineer.bettacraft.plumbing.FilterPipeBlock.touch(); // M40: the filter lines look again
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        PipeNetwork.invalidate(level);
        org.overengineer.bettacraft.plumbing.FilterPipeBlock.touch(); // M40: the filter lines look again
    }

    @Override // BlockTooltip (M52b)
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext ctx, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, net.minecraft.world.item.TooltipFlag flag) {
        List<Component> tooltip = new SinkList<>(tooltipAdder);
        if (part == Part.FRESH || part == Part.WASTE) tooltip.add(Component.translatable("tooltip.bettacraft.filter_port.usage").withStyle(ChatFormatting.DARK_GRAY)); // M40 part b
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (part) {
            case CHAMBER -> new FilterChamberBlockEntity(pos, state);
            case PUMP_HEAD -> new FilterPumpHeadBlockEntity(pos, state);
            case FRESH, WASTE -> new FilterPortBlockEntity(pos, state); // M40 part b
            default -> null;
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (part == Part.WASTE && !level.isClientSide()) return createTickerHelper(type, ModBlockEntities.FILTER_PORT.get(), FilterPortBlockEntity::serverTick);
        return part == Part.PUMP_HEAD && !level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.FILTER_PUMP_HEAD.get(), FilterPumpHeadBlockEntity::serverTick) : null;
    }
}
