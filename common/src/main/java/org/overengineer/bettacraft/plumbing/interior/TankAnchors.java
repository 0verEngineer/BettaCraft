package org.overengineer.bettacraft.plumbing.interior;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.filter.FilterPartBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;

/**
 * M40 (E2): where a filter pipe docks on a tank from outside (any side, the top or the bottom of a tank block), its
 * connection point inside is the middle of that face of the tank block in the interior: on that wall, at the top or on
 * the floor ({@link #localKey}). There the game puts a {@link PipeAnchorBlock} ({@link Plumb#ANCHOR}); a floor block with
 * a bulkhead uses the bulkhead's connection. Interior pipes lead on from it to the guards and outlets. The anchor goes
 * again when nothing docks there any more ({@link #sweep}). Every connection of a filter needs a guard behind it
 * ({@link #guard}): an intake guard or the intake strainer on the intake, an outlet guard or one of the outlets (lily pipe,
 * nozzle, spray bar) on the return. Guards also go on from outside: use the guard on the glass where the pipe docks.
 */
public final class TankAnchors {
    private TankAnchors() {}

    /** How a connection of a filter is guarded. */
    public enum Guard {
        OK, OPEN, PASS_THROUGH;

        public boolean ok() {
            return this == OK;
        }
    }

    /** Pure: the interior-local key of the dock on side {@code face} of tank cell (cx, cy, cz) at interior scale {@code s}. */
    public static long localKey(int cx, int cy, int cz, Direction face, int s) {
        int m = s / 2;
        int x = cx * s + m, y = cy * s + m, z = cz * s + m;
        switch (face) {
            case DOWN -> y = cy * s;
            case UP -> y = cy * s + s - 1;
            case NORTH -> z = cz * s;
            case SOUTH -> z = cz * s + s - 1;
            case WEST -> x = cx * s;
            case EAST -> x = cx * s + s - 1;
        }
        return BlockPos.asLong(x, y, z);
    }

    /** Pure: the connection of the dock on side {@code face} of cell (cx, cy, cz): its anchor or (floor) the bulkhead, or MIN_VALUE. */
    public static long find(Long2IntOpenHashMap plumbing, int cx, int cy, int cz, Direction face, int s) {
        int anchor = Plumb.code(Plumb.ANCHOR, face.get3DDataValue());
        long port = Long.MIN_VALUE;
        for (var e : plumbing.long2IntEntrySet()) {
            long k = e.getLongKey();
            if (Math.floorDiv(BlockPos.getX(k), s) != cx || Math.floorDiv(BlockPos.getY(k), s) != cy || Math.floorDiv(BlockPos.getZ(k), s) != cz) continue;
            if (e.getIntValue() == anchor) return k;
            if (face == Direction.DOWN && Plumb.kind(e.getIntValue()) == Plumb.PORT) port = k; // the bulkhead of this floor block
        }
        return port;
    }

    /** The region of a tank, built (null without the interior). */
    @Nullable
    private static InteriorRegion region(ServerLevel level, TankBlockEntity c, boolean build) {
        if (!Features.enabled(Feature.INTERIOR_PIPES) || InteriorManager.interiorLevel(level.getServer()) == null) return null;
        InteriorRegion r = build ? InteriorManager.ensureRegion(level, c) : InteriorRegions.get(level.getServer()).get(c.getTank().regionId());
        if (r == null || !r.built()) return null;
        InteriorManager.equipment(level.getServer(), r);
        return r;
    }

    /** The connection of the dock on side {@code face} of tank block {@code member}, or MIN_VALUE (nothing placed). */
    public static long existing(ServerLevel level, TankBlockEntity c, BlockPos member, Direction face) {
        InteriorRegion r = region(level, c, false);
        if (r == null) return Long.MIN_VALUE;
        BlockPos cell = member.subtract(c.getTank().min());
        return find(r.equipment().plumbing(), cell.getX(), cell.getY(), cell.getZ(), face, r.scale());
    }

    /**
     * The connection of the dock on side {@code face} of tank block {@code member}; the anchor is put into the interior if it
     * is missing (in the middle of that face, else the free spot nearest to it on that face of the block). MIN_VALUE without
     * the interior (then no guard can be asked for) or without room.
     */
    public static long ensure(ServerLevel level, TankBlockEntity c, BlockPos member, Direction face) {
        TankData t = c.getTank();
        if (!t.contains(member) || t.contains(member.relative(face))) return Long.MIN_VALUE;
        InteriorRegion r = region(level, c, true);
        if (r == null) return Long.MIN_VALUE;
        int s = r.scale();
        BlockPos cell = member.subtract(t.min());
        long k = find(r.equipment().plumbing(), cell.getX(), cell.getY(), cell.getZ(), face, s);
        if (k != Long.MIN_VALUE) return k;
        ServerLevel interior = InteriorManager.interiorLevel(level.getServer());
        long want = localKey(cell.getX(), cell.getY(), cell.getZ(), face, s);
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        // the face plane of the cell: the two axes that are not the face's
        Direction.Axis ax = face.getAxis();
        int wx = BlockPos.getX(want), wy = BlockPos.getY(want), wz = BlockPos.getZ(want);
        int x0 = cell.getX() * s, y0 = cell.getY() * s, z0 = cell.getZ() * s;
        for (int a = 0; a < s; a++)
            for (int b = 0; b < s; b++) {
                int x, y, z;
                switch (ax) {
                    case X -> { x = wx; y = y0 + a; z = z0 + b; }
                    case Y -> { x = x0 + a; y = wy; z = z0 + b; }
                    default -> { x = x0 + a; y = y0 + b; z = wz; }
                }
                double d = (double) (x - wx) * (x - wx) + (double) (y - wy) * (y - wy) + (double) (z - wz) * (z - wz);
                if (d >= bestD) continue;
                BlockPos w = r.toWorld(x, y, z);
                if (!interior.getBlockState(w).canBeReplaced()) continue;
                bestD = d;
                best = w;
            }
        if (best == null) return Long.MIN_VALUE;
        interior.setBlock(best, ModBlocks.PIPE_ANCHOR.get().defaultBlockState().setValue(PipeAnchorBlock.MOUNT, face), Block.UPDATE_ALL);
        return BlockPos.asLong(r.localX(best.getX()), r.localY(best.getY()), r.localZ(best.getZ()));
    }

    /** Whether guards can be asked for at all (interior pipes on and the interior dimension there). */
    public static boolean required(ServerLevel level) {
        return Features.enabled(Feature.INTERIOR_PIPES) && InteriorManager.interiorLevel(level.getServer()) != null;
    }

    /** How the dock on side {@code face} of tank block {@code member} is guarded (its anchor is put in if missing). */
    public static Guard guardOf(ServerLevel level, TankBlockEntity c, BlockPos member, Direction face, boolean intake) {
        long k = ensure(level, c, member, face);
        InteriorRegion r = k == Long.MIN_VALUE ? null : region(level, c, false);
        return r == null ? Guard.OPEN : guard(r, k, intake);
    }

    /** Water of a filter went through this dock now: the guards and outlets behind it run (stream, current). */
    public static void markFlow(ServerLevel level, TankBlockEntity c, BlockPos member, Direction face, boolean intake, long now) {
        long k = existing(level, c, member, face);
        InteriorRegion r = k == Long.MIN_VALUE ? null : region(level, c, false);
        if (r == null) return;
        InteriorPlumbing.Net n = InteriorPlumbing.net(r, k);
        InteriorPlumbing.markFlow(r.id(), intake ? n.intakes() : n.outflows(), now);
    }

    /** How the connection {@code key} is guarded for water leaving the tank ({@code intake}) or coming in. */
    public static Guard guard(InteriorRegion r, long key, boolean intake) {
        InteriorPlumbing.Net n = InteriorPlumbing.net(r, key);
        if (n.passThrough()) return Guard.PASS_THROUGH;
        return (intake ? !n.intakes().isEmpty() : !n.outflows().isEmpty()) ? Guard.OK : Guard.OPEN;
    }

    /** Whether one of our pipes (filter pipes, the pipes of a filter from before M40) or a filter connector docks on that side. */
    public static boolean docked(Level level, BlockPos member, Direction face) {
        BlockPos n = member.relative(face);
        if (!level.isLoaded(n)) return true; // unknown: keep it
        BlockState s = level.getBlockState(n);
        Direction back = face.getOpposite();
        if (org.overengineer.bettacraft.plumbing.Pipes.isPipe(s)) return org.overengineer.bettacraft.plumbing.Pipes.joinedTowards(level, n, s, back); // M68: any pipe, also of a bundle
        if (s.getBlock() instanceof FilterPartBlock f) return (f.part() == FilterPartBlock.Part.INLET || f.part() == FilterPartBlock.Part.OUTLET) && s.getValue(FilterPartBlock.FACING) == back;
        return false;
    }

    /** Care step: the anchors whose pipe outside is gone are taken out (and those of cells that left the tank). */
    public static void sweep(ServerLevel level, TankBlockEntity c) {
        InteriorRegion r = region(level, c, false);
        if (r == null) return;
        TankData t = c.getTank();
        int s = r.scale();
        List<Long> gone = new ArrayList<>();
        for (var e : r.equipment().plumbing().long2IntEntrySet()) {
            if (Plumb.kind(e.getIntValue()) != Plumb.ANCHOR) continue;
            long k = e.getLongKey();
            Direction face = Direction.from3DDataValue(Plumb.facing(e.getIntValue()));
            BlockPos member = t.min().offset(Math.floorDiv(BlockPos.getX(k), s), Math.floorDiv(BlockPos.getY(k), s), Math.floorDiv(BlockPos.getZ(k), s));
            if (!t.contains(member) || t.contains(member.relative(face)) || !docked(level, member, face)) gone.add(k);
        }
        if (gone.isEmpty()) return;
        ServerLevel interior = InteriorManager.interiorLevel(level.getServer());
        if (interior == null) return;
        for (long k : gone) {
            BlockPos w = r.toWorld(BlockPos.getX(k), BlockPos.getY(k), BlockPos.getZ(k));
            if (interior.getBlockState(w).getBlock() instanceof PipeAnchorBlock) interior.setBlock(w, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            else r.equipment().plumbing().remove(k); // the index was stale
        }
    }

    private static void say(@Nullable Player p, String key, ChatFormatting color, Object... args) {
        if (p != null) p.displayClientMessage(Component.translatable(key, args).withStyle(color), true);
    }

    /**
     * Puts an intake or outlet guard ({@code kind}) behind the connection where a pipe docks on side {@code face} of the tank
     * block {@code member} (used on the glass from outside). Public for the GameTests.
     */
    public static InteractionResult installGuard(ServerLevel level, BlockPos member, Direction face, @Nullable Player player, ItemStack stack, Plumb kind) {
        if (!Features.enabled(Feature.INTERIOR_PIPES) || !Features.enabled(Feature.FILTER_LINES)) {
            say(player, "message.bettacraft.feature_off", ChatFormatting.GOLD);
            return InteractionResult.FAIL;
        }
        TankBlockEntity c = TankMultiblock.getController(level, member);
        if (c == null) return InteractionResult.FAIL;
        if (player != null && !TankInhabitants.canModify(player, c.getTank())) return InteractionResult.FAIL;
        if (!docked(level, member, face)) {
            say(player, "message.bettacraft.guard.no_dock", ChatFormatting.GOLD);
            return InteractionResult.FAIL;
        }
        long key = ensure(level, c, member, face);
        InteriorRegion r = key == Long.MIN_VALUE ? null : region(level, c, false);
        if (r == null) {
            say(player, "message.bettacraft.bulkhead.no_room", ChatFormatting.GOLD);
            return InteractionResult.FAIL;
        }
        boolean intake = kind.intake();
        InteriorPlumbing.Net behind = InteriorPlumbing.net(r, key);
        if (intake ? !behind.intakes().isEmpty() : !behind.outflows().isEmpty()) {
            say(player, "message.bettacraft.guard.already", ChatFormatting.GOLD);
            return InteractionResult.FAIL;
        }
        ServerLevel interior = InteriorManager.interiorLevel(level.getServer());
        BlockPos at = r.toWorld(BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key));
        // right in front of the connection (into the tank), else beside it
        List<Direction> order = new ArrayList<>();
        order.add(face.getOpposite());
        for (Direction d : Direction.values()) if (d != face && d != face.getOpposite()) order.add(d);
        Direction inward = face.getOpposite();
        Direction facing = inward.getAxis().isHorizontal() ? inward : Direction.NORTH;
        for (Direction d : order) {
            BlockPos p = at.relative(d);
            if (!r.containsShapeInterior(p) || !interior.getBlockState(p).canBeReplaced()) continue;
            Block b = kind == Plumb.INTAKE_GUARD ? ModBlocks.INTAKE_GUARD.get() : ModBlocks.OUTLET_GUARD.get();
            interior.setBlock(p, b.defaultBlockState().setValue(PlumbingBlock.FACING, facing), Block.UPDATE_ALL);
            if (player != null) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                say(player, "message.bettacraft.guard.installed", ChatFormatting.AQUA);
            }
            level.playSound(null, member, net.minecraft.sounds.SoundEvents.COPPER_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.4F);
            return InteractionResult.CONSUME;
        }
        say(player, "message.bettacraft.bulkhead.no_room", ChatFormatting.GOLD);
        return InteractionResult.FAIL;
    }
}
