package org.overengineer.bettacraft.plants;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.platform.Tags;

/**
 * One block of an aquarium plant in the tank interior (M13), one block per species. The part (cutting, root, stem,
 * leaf, ...) picks the model, {@code tint} the redness of red species (grey textures tinted by
 * {@link PlantColors}). No collision (players walk through, bettas still swim around them, see
 * {@code InteriorObstacles}); never drops anything by itself: the plant index decides what a broken block gives
 * ({@link InteriorPlants}). Shears trim the plant.
 */
public class AquaticPlantBlock extends Block {
    public static final EnumProperty<PlantPart> PART = EnumProperty.create("part", PlantPart.class);
    public static final IntegerProperty TINT = IntegerProperty.create("tint", 0, PlantColors.MAX_TINT);

    private static final VoxelShape CUTTING = Block.box(4, 0, 4, 12, 10, 12);
    private static final VoxelShape STEM = Block.box(5, 0, 5, 11, 16, 11);
    private static final VoxelShape MAT = Block.box(0, 0, 0, 16, 7, 16);
    private static final VoxelShape PAD = Block.box(0, 12, 0, 16, 15, 16);
    private static final VoxelShape RHIZOME = Block.box(1, 0, 1, 15, 10, 15);

    private final PlantSpecies species;

    public AquaticPlantBlock(PlantSpecies species, Properties props) {
        super(props);
        this.species = species;
        registerDefaultState(stateDefinition.any().setValue(PART, PlantPart.CUTTING).setValue(TINT, 0));
    }

    public PlantSpecies species() {
        return species;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(PART, TINT);
    }

    /** The part as drawn: the root looks like the rhizome, stem foot, crown ... of its species. */
    public PlantPart looks(BlockState s) {
        PlantPart p = s.getValue(PART);
        return p == PlantPart.ROOT ? PlantColors.rootLooksLike(species.form()) : p;
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (looks(s)) {
            case CUTTING -> CUTTING;
            case STEM -> STEM;
            case MAT -> MAT;
            case PAD -> PAD;
            case RHIZOME -> RHIZOME;
            default -> Shapes.block();
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState s) {
        return true;
    }

    @Override
    protected int getLightDampening(BlockState s) {
        return 0;
    }

    @Override
    protected float getShadeBrightness(BlockState s, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean isPathfindable(BlockState s, PathComputationType type) {
        return true;
    }

    @Override
    protected boolean canSurvive(BlockState s, LevelReader level, BlockPos pos) {
        return true;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!stack.is(Tags.Items.TOOLS_SHEAR)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level instanceof ServerLevel server) InteriorPlants.trimBlock(server, pos, player, stack, hand);
        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return InteriorPlants.item(species, 0);
    }
}
