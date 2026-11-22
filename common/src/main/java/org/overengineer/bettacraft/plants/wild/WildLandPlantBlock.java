package org.overengineer.bettacraft.plants.wild;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.platform.Tags;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * M56: a terrarium plant in the world, one block per species ({@code wild_<species>}): wild in its biomes
 * ({@link WildLandPlantFeature}) or planted by a player as decoration (the plant item used outside a tank). It does not grow
 * in the world (only in a terrarium); its look is shared per {@link LandLook} and tinted in the colours of the species.
 * <ul>
 *   <li>Shears cut a cutting off (an offset of the cacti, succulents and bromeliads, a piece of the mosses); the plant is then
 *   {@code trimmed} and grows back slowly (random ticks, like the wild aquarium plants).</li>
 *   <li>Broken: a wild one gives nothing by hand, one cutting mined with shears, the whole plant (half grown) with silk touch;
 *   a planted one gives its plant back (half grown) by hand.</li>
 *   <li>It needs ground under it: soil or sand for the rooted ones, wood or stone for the air plants and orchids, any solid
 *   block for the mosses ({@link #canGrowOn}).</li>
 * </ul>
 */
public class WildLandPlantBlock extends Block {
    public static final BooleanProperty TRIMMED = WildPlantBlock.TRIMMED;
    /** Planted by a player (decoration): it gives its plant back when broken. */
    public static final BooleanProperty PLANTED = BooleanProperty.create("planted");

    private final PlantSpecies species;

    public WildLandPlantBlock(PlantSpecies species, Properties props) {
        super(props);
        this.species = species;
        registerDefaultState(stateDefinition.any().setValue(TRIMMED, false).setValue(PLANTED, false));
    }

    public PlantSpecies species() {
        return species;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(TRIMMED, PLANTED);
    }

    /** Whether the species grows on this block (its top face). Pure on the state (the feature and the GameTests use it). */
    public static boolean canGrowOn(PlantSpecies s, BlockState below) {
        if (s.form() == PlantSpecies.Form.MOSS) return !below.isAir();
        if (s.form() == PlantSpecies.Form.EPIPHYTE) return below.is(ModTags.EPIPHYTE_SURFACES) || below.is(ModTags.LAND_PLANT_SOIL);
        return below.is(ModTags.LAND_PLANT_SOIL);
    }

    @Override
    protected boolean canSurvive(BlockState s, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState b = level.getBlockState(below);
        return b.isFaceSturdy(level, below, Direction.UP) && canGrowOn(species, b);
    }

    @Override
    protected BlockState updateShape(BlockState s, LevelReader level, ScheduledTickAccess tickAccess, BlockPos pos, Direction dir, BlockPos otherPos, BlockState other, RandomSource random) {
        if (dir == Direction.DOWN && !canSurvive(s, level, pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(s, level, tickAccess, pos, dir, otherPos, other, random);
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        LandLook look = LandLook.of(species);
        int h = s.getValue(TRIMMED) ? Math.max(2, look.height / 2) : look.height;
        VoxelShape shape = look.model == LandLook.Model.MOSS || look.model == LandLook.Model.CARPET ? Block.box(0, 0, 0, 16, h, 16) : Block.box(3, 0, 3, 13, h, 13);
        Vec3 off = s.getOffset(pos);
        return shape.move(off.x, off.y, off.z);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        // the cacti prick (a little collision like vanilla's cactus is not wanted on a decoration: walk through)
        return Shapes.empty();
    }

    @Override
    protected boolean isRandomlyTicking(BlockState s) {
        return s.getValue(TRIMMED);
    }

    @Override
    protected void randomTick(BlockState s, ServerLevel level, BlockPos pos, RandomSource random) {
        if (s.getValue(TRIMMED) && random.nextInt(WildPlantBlock.REGROW_ONE_IN) == 0) level.setBlock(pos, s.setValue(TRIMMED, false), 2);
    }

    /** What one cut with shears gives: a cutting (stem plants two), an offset (a small rooted plant) or a piece of moss. */
    public static ItemStack cutting(PlantSpecies s) {
        return switch (s.propagation()) {
            case OFFSET -> InteriorPlants.item(s, 1);
            case DIVISION -> InteriorPlants.item(s, Math.max(1, s.stages() / 3));
            default -> WildPlantBlock.cutting(s);
        };
    }

    /** Cuts a cutting off the plant at {@code pos} (shears); false when it was cut just now. */
    public static boolean cut(Level level, BlockPos pos, BlockState state, Direction face) {
        if (!(state.getBlock() instanceof WildLandPlantBlock b) || state.getValue(TRIMMED)) return false;
        Block.popResourceFromFace(level, pos, face, cutting(b.species()));
        level.setBlock(pos, state.setValue(TRIMMED, true), 3);
        level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.8F, 1.3F);
        return true;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Tags.Items.TOOLS_SHEAR)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!cut(level, pos, state, hit.getDirection())) {
            player.displayClientMessage(Component.translatable("message.bettacraft.wild_plant.trimmed").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }
        if (!player.getAbilities().instabuild) stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
        return InteractionResult.CONSUME;
    }

    /** The plant dug out of the ground: half grown. */
    public static ItemStack dugOut(PlantSpecies s) {
        return InteriorPlants.item(s, Math.max(1, s.stages() / 2));
    }

    @Override
    protected List<ItemStack> getDrops(BlockState s, LootParams.Builder params) {
        ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
        boolean silk = false;
        if (tool != null && !tool.isEmpty()) {
            var silkTouch = params.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
            silk = EnchantmentHelper.getItemEnchantmentLevel(silkTouch, tool) > 0;
        }
        if (silk || s.getValue(PLANTED)) return List.of(dugOut(species));
        if (tool != null && tool.is(Tags.Items.TOOLS_SHEAR) && !s.getValue(TRIMMED)) return List.of(cutting(species));
        return List.of();
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return InteriorPlants.item(species, 0);
    }

    /**
     * Plants the species as decoration on the block face a player clicked (the plant item outside a tank). Returns false when
     * it does not fit there.
     */
    public static boolean plantInWorld(Level level, BlockPos clicked, Direction face, PlantSpecies s, @Nullable Player player) {
        var holder = org.overengineer.bettacraft.registry.ModBlocks.WILD_LAND_PLANTS.get(s);
        if (holder == null) return false;
        BlockPos pos = level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(face);
        if (!level.getBlockState(pos).canBeReplaced() || !level.getFluidState(pos).isEmpty()) return false;
        BlockState state = holder.get().defaultBlockState().setValue(PLANTED, true);
        if (!state.canSurvive(level, pos)) {
            if (player != null) player.displayClientMessage(Component.translatable("message.bettacraft.terrarium_plant.no_ground").withStyle(ChatFormatting.GOLD), true);
            return false;
        }
        if (player != null && !player.mayBuild()) return false;
        level.setBlock(pos, state, 3);
        level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }
}
