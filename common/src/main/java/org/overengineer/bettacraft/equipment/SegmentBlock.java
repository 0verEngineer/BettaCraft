package org.overengineer.bettacraft.equipment;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.HitResult;

/**
 * One segment of a piece of equipment that stands in the tank interior as a column of blocks (PLAN 7.3): heaters and
 * coolers (M9) (1x1xN rod, {@link #SIZE}) and the internal filter (2x2xN box at scale 16). Breaking any segment removes the whole
 * piece and gives the item back once. Only the {@link Part#ANCHOR} block (lowest, min x/z) counts as the piece.
 */
public class SegmentBlock extends Block {
    public enum Part implements StringRepresentable {
        /** Lowest block with the smallest x/z: represents the whole piece. */
        ANCHOR,
        /** Other blocks of the lowest layer (filter footprint). */
        BASE,
        MIDDLE,
        TOP;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** What the piece is: heater rod, cooler rod (M9) or internal filter. */
    /** M20: PUMP = flow pump (a small box with an outlet like the internal filter). */
    /** M23: SPONGE_FILTER = air-driven sponge filter, CORNER_FILTER = motor corner filter (both {@link DeviceSegmentBlock}). */
    public enum Kind { HEATER, COOLER, FILTER, PUMP, SPONGE_FILTER, CORNER_FILTER;

        /** Filters with a motor that can suck in fry (M23). */
        public boolean motorFilter() {
            return this == FILTER || this == CORNER_FILTER;
        }
    }

    public enum Size implements StringRepresentable {
        SMALL(16, 6), MEDIUM(32, 9), LARGE(64, 12);

        /** Heating (or cooling) capacity in water blocks (same as {@link HeaterItem}). */
        public final int capacity;
        /** Rod length in interior blocks at scale 16. */
        public final int length16;

        Size(int capacity, int length16) {
            this.capacity = capacity;
            this.length16 = length16;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final EnumProperty<Size> SIZE = EnumProperty.create("size", Size.class);
    /** Internal filter (M4): side of the outlet with the water jet; the heater ignores it. */
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING =
            net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

    private final Kind kind;
    private final Supplier<? extends Item> filterItem;

    /** @param kind heater or cooler rod (has {@link #SIZE}), or the internal filter, dropping {@code filterItem}. */
    public SegmentBlock(Kind kind, Supplier<? extends Item> filterItem, Properties properties) {
        super(properties);
        this.kind = kind;
        this.filterItem = filterItem;
        registerDefaultState(stateDefinition.any().setValue(PART, Part.ANCHOR).setValue(FACING, Direction.NORTH).setValue(SIZE, Size.SMALL));
    }

    public Kind kind() {
        return kind;
    }

    public boolean isHeater() {
        return kind == Kind.HEATER;
    }

    /** Heater or cooler rod (has a size). */
    public boolean isRod() {
        return kind == Kind.HEATER || kind == Kind.COOLER;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
        // SIZE is on both blocks so the state definition stays static; the filter only uses SMALL.
        builder.add(SIZE);
        builder.add(FACING);
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** The item this piece gives back. */
    public ItemStack itemFor(BlockState state) {
        if (!isRod()) return new ItemStack(filterItem.get());
        boolean cool = kind == Kind.COOLER;
        return new ItemStack(switch (state.getValue(SIZE)) {
            case SMALL -> cool ? org.overengineer.bettacraft.registry.ModItems.COOLER_SMALL.get() : org.overengineer.bettacraft.registry.ModItems.HEATER_SMALL.get();
            case MEDIUM -> cool ? org.overengineer.bettacraft.registry.ModItems.COOLER_MEDIUM.get() : org.overengineer.bettacraft.registry.ModItems.HEATER_MEDIUM.get();
            case LARGE -> cool ? org.overengineer.bettacraft.registry.ModItems.COOLER_LARGE.get() : org.overengineer.bettacraft.registry.ModItems.HEATER_LARGE.get();
        });
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return itemFor(state);
    }

    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return itemFor(state);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            // M17: two pieces placed side by side touch and are removed together; every piece (its anchor block) gives
            // its own item back, not only the one that was hit
            List<ItemStack> drops = new ArrayList<>();
            for (BlockPos p : connected(level, pos)) {
                BlockState s = level.getBlockState(p);
                if (s.is(this) && s.getValue(PART) == Part.ANCHOR) {
                    drops.add(itemFor(s));
                    // M23: a fitted intake sponge comes back too
                    if (s.hasProperty(DeviceSegmentBlock.SPONGE) && s.getValue(DeviceSegmentBlock.SPONGE)) drops.add(new ItemStack(org.overengineer.bettacraft.registry.ModItems.INTAKE_SPONGE.get()));
                }
                if (!p.equals(pos)) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            if (drops.isEmpty()) drops.add(itemFor(state));
            if (!player.getAbilities().instabuild) for (ItemStack d : drops) Block.popResource(level, pos, d);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** All blocks of the same piece (connected blocks of this block type), at most 512. */
    public List<BlockPos> connected(BlockGetter level, BlockPos start) {
        List<BlockPos> out = new ArrayList<>();
        ArrayDeque<BlockPos> todo = new ArrayDeque<>();
        java.util.Set<BlockPos> seen = new java.util.HashSet<>();
        todo.add(start.immutable());
        seen.add(start.immutable());
        while (!todo.isEmpty() && out.size() < 512) {
            BlockPos p = todo.poll();
            if (!level.getBlockState(p).is(this)) continue;
            out.add(p);
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (seen.add(n)) todo.add(n);
            }
        }
        return out;
    }

    /** Number of interior blocks of a heater rod at the given interior scale (PLAN 7.3: 6/9/12 at scale 16). */
    public static int heaterLength(Size size, int scale) {
        return Math.max(2, Math.round(size.length16 * scale / 16F));
    }

    /** Filter box at the given scale: {footprint, height} (2x2x8 at scale 16). */
    public static int[] filterShape(int scale) {
        return new int[]{Math.max(1, 2 * scale / 16), Math.max(2, Math.round(8 * scale / 16F))};
    }

    /** M23: sponge filter at the given scale: {footprint, height} (2x2x5 at scale 16: sponge and lift tube). */
    public static int[] spongeShape(int scale) {
        return new int[]{Math.max(1, 2 * scale / 16), Math.max(2, Math.round(5 * scale / 16F))};
    }

    /** M23: corner filter at the given scale: {footprint, height} (2x2x7 at scale 16). */
    public static int[] cornerShape(int scale) {
        return new int[]{Math.max(1, 2 * scale / 16), Math.max(2, Math.round(7 * scale / 16F))};
    }

    /** Footprint and height of a piece of this kind (not for rods). */
    public static int[] shape(Kind kind, int scale) {
        return switch (kind) {
            case PUMP -> pumpShape(scale);
            case SPONGE_FILTER -> spongeShape(scale);
            case CORNER_FILTER -> cornerShape(scale);
            default -> filterShape(scale);
        };
    }

    /** M20: flow pump box at the given scale: {footprint, height} (2x2x2 at scale 16). */
    public static int[] pumpShape(int scale) {
        return new int[]{Math.max(1, 2 * scale / 16), Math.max(1, 2 * scale / 16)};
    }
}
