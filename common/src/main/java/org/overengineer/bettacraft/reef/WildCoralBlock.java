package org.overengineer.bettacraft.reef;

import net.minecraft.world.level.ScheduledTickAccess;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.platform.Tags;
import org.overengineer.bettacraft.plants.PlantSpecies;

/**
 * M22.3: a Bettacraft coral (or the bubble tip anemone) growing wild in a warm ocean reef, in its full size: one block per
 * colony on the reef among the vanilla corals ({@link WildCoralFeature}). Shears cut a frag off it (the coral item for a
 * reef tank); the colony is then trimmed and grows back after a while (random ticks). It lives under water only: out of
 * the water it breaks off. Broken it drops nothing (like vanilla corals), with silk touch itself.
 */
public class WildCoralBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty TRIMMED = BooleanProperty.create("trimmed");
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 12, 14);
    private static final VoxelShape LOW = Block.box(1, 0, 1, 15, 6, 15);

    private final PlantSpecies species;

    public WildCoralBlock(PlantSpecies species, Properties props) {
        super(props);
        this.species = species;
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, true).setValue(TRIMMED, false));
    }

    public PlantSpecies species() {
        return species;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(WATERLOGGED, TRIMMED);
    }

    /** The model of a species: a low mat, a dome, a toadstool or a bushy cross. */
    public static String shape(PlantSpecies s) {
        return switch (s.form()) {
            case POLYPS -> "mat";
            case DISC -> "disc";
            case DOME -> "dome";
            case TOADSTOOL -> "toadstool";
            default -> "cross";
        };
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (shape(species)) {
            case "mat", "disc" -> LOW;
            default -> SHAPE;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    protected FluidState getFluidState(BlockState s) {
        return s.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(s);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        FluidState f = ctx.getLevel().getFluidState(ctx.getClickedPos());
        if (!f.is(FluidTags.WATER) || !f.isSource()) return null; // only under water
        BlockState s = defaultBlockState();
        return s.canSurvive(ctx.getLevel(), ctx.getClickedPos()) ? s : null;
    }

    @Override
    protected boolean canSurvive(BlockState s, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState s, LevelReader level, ScheduledTickAccess tickAccess, BlockPos pos, Direction dir, BlockPos otherPos, BlockState other, RandomSource random) {
        if (s.getValue(WATERLOGGED)) tickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        if (dir == Direction.DOWN && !canSurvive(s, level, pos)) return net.minecraft.world.level.block.Blocks.WATER.defaultBlockState();
        return super.updateShape(s, level, tickAccess, pos, dir, otherPos, other, random);
    }

    @Override
    protected void tick(BlockState s, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!s.getValue(WATERLOGGED)) level.destroyBlock(pos, false); // out of the water: it breaks off
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState s, FluidState fluid) {
        return SimpleWaterloggedBlock.super.placeLiquid(level, pos, s, fluid);
    }

    @Override
    public net.minecraft.world.item.ItemStack pickupBlock(@Nullable net.minecraft.world.entity.LivingEntity player, LevelAccessor level, BlockPos pos, BlockState s) {
        // a bucket takes the water: the coral dries out and breaks off
        ItemStack out = SimpleWaterloggedBlock.super.pickupBlock(player, level, pos, s);
        if (!out.isEmpty()) level.scheduleTick(pos, this, 1);
        return out;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState s) {
        return s.getValue(TRIMMED);
    }

    @Override
    protected void randomTick(BlockState s, ServerLevel level, BlockPos pos, RandomSource random) {
        // a trimmed colony grows back (about two in-game days)
        if (s.getValue(TRIMMED) && random.nextInt(12) == 0) level.setBlock(pos, s.setValue(TRIMMED, false), 2);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Tags.Items.TOOLS_SHEAR)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (state.getValue(TRIMMED)) {
            player.displayClientMessage(Component.translatable("message.bettacraft.wild_coral.trimmed").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }
        ItemStack frag = org.overengineer.bettacraft.plants.InteriorPlants.item(species, 0);
        Block.popResourceFromFace(level, pos, hit.getDirection(), frag);
        level.setBlock(pos, state.setValue(TRIMMED, true), 3);
        level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.8F, 1.3F);
        if (!player.getAbilities().instabuild) stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
        return InteractionResult.CONSUME;
    }
}
