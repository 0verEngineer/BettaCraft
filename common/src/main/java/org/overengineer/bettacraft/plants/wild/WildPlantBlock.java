package org.overengineer.bettacraft.plants.wild;

import net.minecraft.world.level.ScheduledTickAccess;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
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

/**
 * M28.1: an aquarium plant growing wild in the world, under water: a few shared blocks, one per growth form
 * ({@link WildPlantKind}), with the species as block state ({@code species}) and its look (shared grey models per leaf shape,
 * tinted in the colours of the species). Placed by {@link WildPlantFeature}; there is no item of the block.
 * <ul>
 *   <li>Shears (used on it) cut a cutting of the species off; the plant is then {@code trimmed} (a smaller model) and grows
 *   back slowly (random ticks, about one in-game day).</li>
 *   <li>Broken by hand it gives nothing, like vanilla seagrass; mined with shears one cutting; with silk touch the whole
 *   plant (the aquarium plant item at half its growth).</li>
 *   <li>It lives under water only: out of the water (or without a floor) it breaks off.</li>
 * </ul>
 */
public abstract class WildPlantBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty TRIMMED = BooleanProperty.create("trimmed");
    /** Chance per random tick that a trimmed plant grows back (random tick about every 68 s: ~1/20 = ~23 minutes). */
    public static final int REGROW_ONE_IN = 20;

    static EnumProperty<PlantSpecies> property(WildPlantKind k) {
        return EnumProperty.create("species", PlantSpecies.class, s -> WildPlantKind.of(s) == k);
    }

    /** Tint level of the red species in the wild (open daylight in murky water: a hint of red, not the deep red under an LED panel). */
    public static final int WILD_TINT = 1;

    private final WildPlantKind kind;

    protected WildPlantBlock(WildPlantKind kind, Properties props) {
        super(props);
        this.kind = kind;
        PlantSpecies first = null;
        for (PlantSpecies s : speciesProperty().getPossibleValues()) {
            first = s;
            break;
        }
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, true).setValue(TRIMMED, false).setValue(speciesProperty(), first));
    }

    public WildPlantKind kind() {
        return kind;
    }

    /** The species property of this block (a static field of the subclass: it is needed before the constructor runs). */
    public abstract EnumProperty<PlantSpecies> speciesProperty();

    public PlantSpecies species(BlockState s) {
        return s.getValue(speciesProperty());
    }

    /** The state of a species (it must grow in this block). */
    public BlockState withSpecies(PlantSpecies s) {
        return defaultBlockState().setValue(speciesProperty(), s);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(WATERLOGGED, TRIMMED, speciesProperty());
    }

    /** Height of the outline in pixels. */
    private int height(BlockState s) {
        PlantSpecies sp = species(s);
        int h = switch (kind) {
            case EPIPHYTE -> sp == PlantSpecies.JAVA_FERN ? 13 : 9;
            case MOSS -> sp == PlantSpecies.MARIMO ? 7 : 4;
            case ROSETTE -> "ribbon".equals(WildPlantKind.look(sp)) ? 16 : "grass".equals(WildPlantKind.look(sp)) ? 9 : 13;
            case STEM -> 16;
            case CARPET -> switch (WildPlantKind.look(sp)) { // M55: the sprigs of the look
                case "needle" -> 8;
                case "fine" -> 6;
                case "clover" -> 5;
                default -> 4;
            };
        };
        return s.getValue(TRIMMED) ? Math.max(2, h / 2) : h;
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        int h = height(s);
        VoxelShape shape = kind == WildPlantKind.MOSS || kind == WildPlantKind.CARPET ? Block.box(0, 0, 0, 16, h, 16) : Block.box(3, 0, 3, 13, h, 13);
        Vec3 off = s.getOffset(pos);
        return shape.move(off.x, off.y, off.z);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    protected FluidState getFluidState(BlockState s) {
        return s.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(s);
    }

    @Override
    protected boolean canSurvive(BlockState s, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState s, LevelReader level, ScheduledTickAccess tickAccess, BlockPos pos, Direction dir, BlockPos otherPos, BlockState other, RandomSource random) {
        if (s.getValue(WATERLOGGED)) tickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        if (dir == Direction.DOWN && !canSurvive(s, level, pos)) return Blocks.WATER.defaultBlockState();
        return super.updateShape(s, level, tickAccess, pos, dir, otherPos, other, random);
    }

    @Override
    protected void tick(BlockState s, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!s.getValue(WATERLOGGED)) level.destroyBlock(pos, false); // out of the water: it dries up
    }

    @Override
    public ItemStack pickupBlock(@Nullable net.minecraft.world.entity.LivingEntity player, LevelAccessor level, BlockPos pos, BlockState s) {
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
        if (s.getValue(TRIMMED) && random.nextInt(REGROW_ONE_IN) == 0) level.setBlock(pos, s.setValue(TRIMMED, false), 2);
    }

    /** What one cut with shears gives: one cutting, a stem plant two (the cut tops). */
    public static ItemStack cutting(PlantSpecies s) {
        ItemStack st = InteriorPlants.item(s, 0);
        if (s.form() == PlantSpecies.Form.STEM) st.setCount(2);
        return st;
    }

    /** Cuts a cutting off the plant at {@code pos} (shears); false when it was cut just now. */
    public static boolean cut(Level level, BlockPos pos, BlockState state, Direction face) {
        if (!(state.getBlock() instanceof WildPlantBlock b) || state.getValue(TRIMMED)) return false;
        Block.popResourceFromFace(level, pos, face, cutting(b.species(state)));
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

    /**
     * Broken: nothing by hand (like seagrass), one cutting mined with shears, the whole plant (half grown) with silk
     * touch. The block has no loot table; this is the whole rule.
     */
    @Override
    protected List<ItemStack> getDrops(BlockState s, LootParams.Builder params) {
        ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
        if (tool == null || tool.isEmpty()) return List.of();
        PlantSpecies sp = species(s);
        var silk = params.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
        if (EnchantmentHelper.getItemEnchantmentLevel(silk, tool) > 0) return List.of(InteriorPlants.item(sp, Math.max(1, sp.stages() / 2)));
        if (tool.is(Tags.Items.TOOLS_SHEAR) && !s.getValue(TRIMMED)) return List.of(cutting(sp));
        return List.of();
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return InteriorPlants.item(species(state), 0);
    }

    // ------------------------------------------------------------------ the blocks per growth form

    public static final class Epiphyte extends WildPlantBlock {
        public static final EnumProperty<PlantSpecies> SPECIES = property(WildPlantKind.EPIPHYTE);

        public Epiphyte(Properties p) {
            super(WildPlantKind.EPIPHYTE, p);
        }

        @Override
        public EnumProperty<PlantSpecies> speciesProperty() {
            return SPECIES;
        }
    }

    public static final class Moss extends WildPlantBlock {
        public static final EnumProperty<PlantSpecies> SPECIES = property(WildPlantKind.MOSS);

        public Moss(Properties p) {
            super(WildPlantKind.MOSS, p);
        }

        @Override
        public EnumProperty<PlantSpecies> speciesProperty() {
            return SPECIES;
        }
    }

    public static final class Rosette extends WildPlantBlock {
        public static final EnumProperty<PlantSpecies> SPECIES = property(WildPlantKind.ROSETTE);

        public Rosette(Properties p) {
            super(WildPlantKind.ROSETTE, p);
        }

        @Override
        public EnumProperty<PlantSpecies> speciesProperty() {
            return SPECIES;
        }
    }

    public static final class Stem extends WildPlantBlock {
        public static final EnumProperty<PlantSpecies> SPECIES = property(WildPlantKind.STEM);

        public Stem(Properties p) {
            super(WildPlantKind.STEM, p);
        }

        @Override
        public EnumProperty<PlantSpecies> speciesProperty() {
            return SPECIES;
        }
    }

    public static final class Carpet extends WildPlantBlock {
        public static final EnumProperty<PlantSpecies> SPECIES = property(WildPlantKind.CARPET);

        public Carpet(Properties p) {
            super(WildPlantKind.CARPET, p);
        }

        @Override
        public EnumProperty<PlantSpecies> speciesProperty() {
            return SPECIES;
        }
    }
}
