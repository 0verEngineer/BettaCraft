package org.overengineer.bettacraft.plants.wild;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tank.plants.TankPlants;

/**
 * M28.1: a patch of a floating plant on calm water in the world (like a lily pad, one block for every
 * {@link FloatingPlantType} but the vanilla lily pad, the type as block state). Collected by hand, with shears or with the
 * aquarium net (it gives the floating plant item; floating Riccia the aquarium plant Riccia); boats break it like a lily
 * pad. Placed by {@link WildPlantFeature}; no item of the block.
 */
public class WildFloatingPlantBlock extends BushBlock {
    public static final MapCodec<WildFloatingPlantBlock> CODEC = simpleCodec(WildFloatingPlantBlock::new);
    public static final EnumProperty<FloatingPlantType> TYPE = EnumProperty.create("type", FloatingPlantType.class, t -> t != FloatingPlantType.LILY_PAD);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 1.5, 15);

    public WildFloatingPlantBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(TYPE, FloatingPlantType.DUCKWEED));
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(TYPE);
    }

    public static FloatingPlantType type(BlockState s) {
        return s.getValue(TYPE);
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    /** On still water (or ice) with nothing fluid above, like the lily pad. */
    @Override
    protected boolean mayPlaceOn(BlockState below, BlockGetter level, BlockPos pos) {
        FluidState f = level.getFluidState(pos);
        FluidState above = level.getFluidState(pos.above());
        return (f.getType() == Fluids.WATER || below.getBlock() instanceof IceBlock) && above.getType() == Fluids.EMPTY;
    }

    @Override
    protected boolean canSurvive(BlockState s, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return mayPlaceOn(level.getBlockState(below), level, below);
    }

    @Override
    protected void entityInside(BlockState s, Level level, BlockPos pos, Entity entity) {
        super.entityInside(s, level, pos, entity);
        if (level instanceof ServerLevel && entity instanceof Boat) level.destroyBlock(pos, true, entity);
    }

    /** The item of the patch (always: by hand, with shears or silk touch). The block has no loot table. */
    @Override
    protected List<ItemStack> getDrops(BlockState s, LootParams.Builder params) {
        ItemStack st = TankPlants.item(type(s));
        return st.isEmpty() ? List.of() : List.of(st);
    }

    /** The aquarium net scoops the patch up into the inventory. True when there was one. */
    public static boolean scoop(Level level, BlockPos pos, Player player) {
        BlockState s = level.getBlockState(pos);
        if (!(s.getBlock() instanceof WildFloatingPlantBlock)) return false;
        ItemStack st = TankPlants.item(type(s));
        level.removeBlock(pos, false);
        level.playSound(null, pos, SoundEvents.BUCKET_FILL_FISH, SoundSource.PLAYERS, 0.6F, 1.4F);
        if (!st.isEmpty() && !player.getInventory().add(st)) player.drop(st, false);
        return true;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return TankPlants.item(type(state));
    }
}
