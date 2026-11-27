package org.overengineer.bettacraft.plumbing.interior;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.debug.LogArea;
import org.overengineer.bettacraft.filter.FilterPartBlock;
import org.overengineer.bettacraft.plumbing.PipeBlock;
import org.overengineer.bettacraft.plumbing.PipeBundleBlock;
import org.overengineer.bettacraft.plumbing.PipeNetwork;
import org.overengineer.bettacraft.plumbing.PipeRing;
import org.overengineer.bettacraft.plumbing.PipeRole;
import org.overengineer.bettacraft.plumbing.PipeSlots;
import org.overengineer.bettacraft.plumbing.Pipes;
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
 * connection point inside is on that face of the tank block in the interior: on that wall, at the top or on the floor
 * ({@link #localKey}). There the game puts a {@link PipeAnchorBlock} ({@link Plumb#ANCHOR}); a floor block with a bulkhead
 * uses the bulkhead's connection. Interior pipes lead on from it to the guards and outlets. The anchor goes again when nothing
 * docks there any more ({@link #sweep}). Every connection of a filter needs a guard behind it ({@link #guard}): an intake guard
 * or the intake strainer on the intake, an outlet guard or one of the outlets (lily pipe, nozzle, spray bar) on the return.
 * Guards also go on from outside: use the guard on the glass where the pipe docks.
 * <p>
 * M68 part C: <b>a dock per line.</b> A single pipe (or a bundle with one line on that side, a filter connector, a water change
 * controller against the glass) has one dock in the middle of the face (line 0). A pipe bundle that docks with two to four lines
 * gives every line its own dock at the spot of its flange (line = slot + 1, {@link #localKey}), each with its own guards, its own
 * outlets and the ring colour of its line. A guard used on the glass goes behind the line whose flange was clicked
 * ({@link #slotAt}). <b>Migration:</b> a dock of the whole face from before (line 0 where a bundle docks with several lines, or a
 * single pipe that became a bundle) is split once the lines are asked for ({@link #split}): its intake guards and strainers go to
 * the line that sucks (a filter intake, the controller's waste water in), its outlet guards and outlets to the line that returns
 * water (filter return, fresh water); pipes behind it go with the line their outlets belong to. When that cannot be told apart
 * (pipes with both kinds behind one dock), the old dock stays shared by every line of the face and works as before.
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

    /**
     * M68 part C: the dock of a line: the region, its interior-local key, its line (0 = a single pipe's) and whether it is an old dock
     * shared by every line of a bundle (then each line asks it for its own kind of guard, as before).
     */
    public record Anchor(InteriorRegion region, long key, int line, boolean shared) {}

    /** Region id -> old shared dock -> the plumbing version its split failed at (not tried again until the pipes inside change). */
    private static final Map<Integer, Long2IntOpenHashMap> SPLIT_FAILED = new ConcurrentHashMap<>();

    /** Forgets the failed splits (tests). */
    public static void forget() {
        SPLIT_FAILED.clear();
    }

    // ------------------------------------------------------------------ geometry (pure)

    /** Pure: the interior-local key of the dock on side {@code face} of tank cell (cx, cy, cz) at interior scale {@code s} (line 0: the middle). */
    public static long localKey(int cx, int cy, int cz, Direction face, int s) {
        return localKey(cx, cy, cz, face, s, 0);
    }

    /**
     * Pure, M68 part C: the interior-local key of the dock of line {@code line} (0 = the middle of the face, 1..4 = the flange of
     * bundle slot 0..3, {@link PipeBundleBlock#signs}) on side {@code face} of cell (cx, cy, cz).
     */
    public static long localKey(int cx, int cy, int cz, Direction face, int s, int line) {
        int m = s / 2;
        int x = cx * s + m, y = cy * s + m, z = cz * s + m;
        if (line > 0) {
            int[] sg = PipeBundleBlock.signs(line - 1);
            x = cx * s + spot(sg[0], s);
            y = cy * s + spot(sg[1], s);
            z = cz * s + spot(sg[2], s);
        }
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

    /**
     * Pure: the coordinate in a cell (0..s-1) of a line's flange on one axis of the face ({@code sign} -1 or +1): the pixel of the
     * flange's middle ({@code 8 -/+ OFFSET}) at the interior scale, at least two blocks apart (so the docks of two lines never touch).
     */
    static int spot(int sign, int s) {
        int low = (int) Math.floor((8 - PipeBundleBlock.OFFSET) * s / 16.0), high = (int) Math.floor((8 + PipeBundleBlock.OFFSET) * s / 16.0);
        if (high - low < 3) {
            low = s / 2 - 2;
            high = s / 2 + 1;
        }
        return Math.max(0, Math.min(s - 1, sign < 0 ? low : high));
    }

    /** Pure: the connection of the dock on side {@code face} of cell (cx, cy, cz): its anchor or (floor) the bulkhead, or MIN_VALUE. */
    public static long find(Long2IntOpenHashMap plumbing, int cx, int cy, int cz, Direction face, int s) {
        return find(plumbing, cx, cy, cz, face, s, 0);
    }

    /** Pure, M68 part C: the dock of line {@code line} on side {@code face} of cell (cx, cy, cz) (line 0 on the floor: else the bulkhead), or MIN_VALUE. */
    public static long find(Long2IntOpenHashMap plumbing, int cx, int cy, int cz, Direction face, int s, int line) {
        long k = anchorOf(plumbing, cx, cy, cz, face, s, line);
        if (k != Long.MIN_VALUE || line != 0 || face != Direction.DOWN) return k;
        for (var e : plumbing.long2IntEntrySet()) {
            long q = e.getLongKey();
            if (inCell(q, cx, cy, cz, s) && Plumb.kind(e.getIntValue()) == Plumb.PORT) return q; // the bulkhead of this floor block
        }
        return Long.MIN_VALUE;
    }

    /** Pure: the anchor block of line {@code line} on that face of the cell (no bulkhead), or MIN_VALUE. */
    static long anchorOf(Long2IntOpenHashMap plumbing, int cx, int cy, int cz, Direction face, int s, int line) {
        int code = Plumb.code(Plumb.ANCHOR, face.get3DDataValue(), line);
        long want = localKey(cx, cy, cz, face, s, line);
        if (plumbing.containsKey(want) && plumbing.get(want) == code) return want; // where it belongs (almost always)
        for (var e : plumbing.long2IntEntrySet()) {
            if (e.getIntValue() != code) continue;
            if (inCell(e.getLongKey(), cx, cy, cz, s)) return e.getLongKey();
        }
        return Long.MIN_VALUE;
    }

    private static boolean inCell(long k, int cx, int cy, int cz, int s) {
        return Math.floorDiv(BlockPos.getX(k), s) == cx && Math.floorDiv(BlockPos.getY(k), s) == cy && Math.floorDiv(BlockPos.getZ(k), s) == cz;
    }

    private static long offset(long k, Direction d) {
        return BlockPos.asLong(BlockPos.getX(k) + d.getStepX(), BlockPos.getY(k) + d.getStepY(), BlockPos.getZ(k) + d.getStepZ());
    }

    // ------------------------------------------------------------------ the lines outside

    /** The slots of the bundle on side {@code face} of tank block {@code member} that join the tank (bit i = slot i); 0 for anything else. */
    static int bundleSlots(BlockGetter level, BlockPos member, Direction face) {
        BlockPos n = member.relative(face);
        if (!(level.getBlockState(n).getBlock() instanceof PipeBundleBlock)) return 0;
        int[] slots = Pipes.slotsAt(level, n);
        Direction back = face.getOpposite();
        int mask = 0;
        for (int i = 0; i < slots.length; i++) if (PipeSlots.present(slots[i]) && PipeSlots.joined(slots[i], back)) mask |= 1 << i;
        return mask;
    }

    /**
     * The dock line of slot {@code slot} (-1: a single pipe or a device) of what docks on side {@code face} of {@code member}: 0
     * unless a bundle docks there with two or more lines (then slot + 1; for an unknown slot its first line). The same rule as the
     * flanges ({@code TankFlanges}: one line of a bundle on a side is drawn in the middle).
     */
    public static int line(BlockGetter level, BlockPos member, Direction face, int slot) {
        int mask = bundleSlots(level, member, face);
        if (Integer.bitCount(mask) < 2) return 0;
        if (slot >= 0 && slot < PipeSlots.COUNT && (mask & 1 << slot) != 0) return slot + 1;
        return Integer.numberOfTrailingZeros(mask) + 1;
    }

    /** The dock lines on side {@code face} of {@code member}: {0} for a single dock, the lines of a bundle with two or more. */
    public static int[] lines(BlockGetter level, BlockPos member, Direction face) {
        int mask = bundleSlots(level, member, face);
        if (Integer.bitCount(mask) < 2) return new int[]{0};
        int[] out = new int[Integer.bitCount(mask)];
        int j = 0;
        for (int i = 0; i < PipeSlots.COUNT; i++) if ((mask & 1 << i) != 0) out[j++] = i + 1;
        return out;
    }

    /** The ring colour of a dock line: the ring of the single pipe, of the bundle's slot (none for a device against the glass). */
    static PipeRing ring(BlockGetter level, BlockPos member, Direction face, int line) {
        BlockPos n = member.relative(face);
        BlockState s = level.getBlockState(n);
        if (s.getBlock() instanceof PipeBlock) return s.getValue(PipeBlock.RING);
        if (!(s.getBlock() instanceof PipeBundleBlock)) return PipeRing.NONE;
        int[] slots = Pipes.slotsAt(level, n);
        if (line > 0) return line - 1 < slots.length && PipeSlots.present(slots[line - 1]) ? PipeSlots.ring(slots[line - 1]) : PipeRing.NONE;
        int mask = bundleSlots(level, member, face);
        return Integer.bitCount(mask) == 1 ? PipeSlots.ring(slots[Integer.numberOfTrailingZeros(mask)]) : PipeRing.NONE; // a shared old dock: no colour
    }

    /**
     * The slot whose flange is nearest to {@code hit} (a point on side {@code face} of {@code member}, world coordinates), or -1 when no
     * bundle docks there with two or more lines.
     */
    public static int slotAt(BlockGetter level, BlockPos member, Direction face, @Nullable Vec3 hit) {
        int mask = bundleSlots(level, member, face);
        if (hit == null || Integer.bitCount(mask) < 2) return -1;
        double[] local = {(hit.x - member.getX()) * 16, (hit.y - member.getY()) * 16, (hit.z - member.getZ()) * 16};
        int best = -1;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < PipeSlots.COUNT; i++) {
            if ((mask & 1 << i) == 0) continue;
            float[] a = PipeBundleBlock.arm(i, face.getOpposite(), PipeBundleBlock.ARM, 0);
            double d = 0;
            for (int ax = 0; ax < 3; ax++) {
                if (ax == face.getAxis().ordinal()) continue;
                double c = (a[ax] + a[ax + 3]) / 2;
                d += (local[ax] - c) * (local[ax] - c);
            }
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ the docks in the interior

    /** The region of a tank, built (null without the interior). */
    @Nullable
    private static InteriorRegion region(ServerLevel level, TankBlockEntity c, boolean build) {
        if (!Features.enabled(Feature.INTERIOR_PIPES) || InteriorManager.interiorLevel(level.getServer()) == null) return null;
        InteriorRegion r = build ? InteriorManager.ensureRegion(level, c) : InteriorRegions.get(level.getServer()).get(c.getTank().regionId());
        if (r == null || !r.built()) return null;
        InteriorManager.equipment(level.getServer(), r);
        return r;
    }

    /** The connection of the dock on side {@code face} of tank block {@code member} (its first line), or MIN_VALUE (nothing placed). */
    public static long existing(ServerLevel level, TankBlockEntity c, BlockPos member, Direction face) {
        return existing(level, c, member, face, -1);
    }

    /** M68 part C: the connection of the dock of slot {@code slot}'s line on that side, or MIN_VALUE (nothing placed). */
    public static long existing(ServerLevel level, TankBlockEntity c, BlockPos member, Direction face, int slot) {
        Anchor a = anchor(level, c, member, face, slot, false);
        return a == null ? Long.MIN_VALUE : a.key();
    }

    /** The connection of the dock on side {@code face} of tank block {@code member} (its first line), put in if missing. */
    public static long ensure(ServerLevel level, TankBlockEntity c, BlockPos member, Direction face) {
        return ensure(level, c, member, face, -1);
    }

    /**
     * The connection of the dock of slot {@code slot}'s line (-1: a single pipe) on side {@code face} of tank block {@code member};
     * the anchor is put into the interior if it is missing (at its spot on that face, else the free spot nearest to it). MIN_VALUE
     * without the interior (then no guard can be asked for) or without room.
     */
    public static long ensure(ServerLevel level, TankBlockEntity c, BlockPos member, Direction face, int slot) {
        Anchor a = anchor(level, c, member, face, slot, true);
        return a == null ? Long.MIN_VALUE : a.key();
    }

    /**
     * M68 part C: the dock serving slot {@code slot}'s line (-1: a single pipe or a device) on side {@code face} of tank block
     * {@code member}, or null. An old dock of the whole face is split first ({@link #split}); a line dock of a bundle that is a
     * single pipe again becomes the face's dock. {@code create}: put it in when it is missing.
     */
    @Nullable
    public static Anchor anchor(ServerLevel level, TankBlockEntity c, BlockPos member, Direction face, int slot, boolean create) {
        TankData t = c.getTank();
        if (!t.contains(member) || t.contains(member.relative(face))) return null;
        InteriorRegion r = region(level, c, create);
        if (r == null) return null;
        int s = r.scale();
        BlockPos cell = member.subtract(t.min());
        int cx = cell.getX(), cy = cell.getY(), cz = cell.getZ();
        boolean known = level.isLoaded(member.relative(face));
        int line = known ? line(level, member, face, slot) : 0;
        Long2IntOpenHashMap m = r.equipment().plumbing();
        long k = find(m, cx, cy, cz, face, s, line);
        if (k != Long.MIN_VALUE) return new Anchor(r, k, line, false);
        if (!known) return null;
        if (line > 0) {
            long old = anchorOf(m, cx, cy, cz, face, s, 0);
            if (old != Long.MIN_VALUE) {
                if (!split(level, c, r, member, face, old)) return new Anchor(r, old, 0, true);
                k = anchorOf(m, cx, cy, cz, face, s, line);
                if (k != Long.MIN_VALUE) return new Anchor(r, k, line, false);
            }
        } else {
            // a bundle that docked with several lines is a single line again: its line's dock (else the first) serves it from now on
            int keep = keptLine(level, member, face);
            for (int i = 0; i <= PipeSlots.COUNT; i++) {
                int l = i == 0 ? keep : i;
                if (l <= 0) continue;
                long q = anchorOf(m, cx, cy, cz, face, s, l);
                if (q == Long.MIN_VALUE) continue;
                relabel(level, r, q, 0, ring(level, member, face, 0));
                return new Anchor(r, q, 0, false);
            }
        }
        if (!create) return null;
        k = place(level, r, cx, cy, cz, face, line, ring(level, member, face, line));
        return k == Long.MIN_VALUE ? null : new Anchor(r, k, line, false);
    }

    /** The line whose dock stays when a bundle docks with one line only: that slot's (0 when it is no bundle). */
    private static int keptLine(BlockGetter level, BlockPos member, Direction face) {
        int mask = bundleSlots(level, member, face);
        return Integer.bitCount(mask) == 1 ? Integer.numberOfTrailingZeros(mask) + 1 : 0;
    }

    /** Sets the line (and ring) of the anchor at {@code key}. */
    private static void relabel(ServerLevel level, InteriorRegion r, long key, int line, PipeRing ring) {
        ServerLevel interior = InteriorManager.interiorLevel(level.getServer());
        if (interior == null) return;
        BlockPos w = r.toWorld(BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key));
        BlockState st = interior.getBlockState(w);
        if (st.getBlock() instanceof PipeAnchorBlock && (st.getValue(PipeAnchorBlock.LINE) != line || st.getValue(PipeAnchorBlock.RING) != ring))
            interior.setBlock(w, st.setValue(PipeAnchorBlock.LINE, line).setValue(PipeAnchorBlock.RING, ring), Block.UPDATE_ALL);
    }

    /**
     * Puts the anchor of line {@code line} on side {@code face} of cell (cx, cy, cz): at its spot, else the free spot of that face
     * nearest to it where it joins no other line ({@link #fits}). Returns its key or MIN_VALUE (no room).
     */
    private static long place(ServerLevel level, InteriorRegion r, int cx, int cy, int cz, Direction face, int line, PipeRing ring) {
        ServerLevel interior = InteriorManager.interiorLevel(level.getServer());
        if (interior == null) return Long.MIN_VALUE;
        int s = r.scale();
        Long2IntOpenHashMap m = r.equipment().plumbing();
        long want = localKey(cx, cy, cz, face, s, line);
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        // the face plane of the cell: the two axes that are not the face's
        Direction.Axis ax = face.getAxis();
        int wx = BlockPos.getX(want), wy = BlockPos.getY(want), wz = BlockPos.getZ(want);
        int x0 = cx * s, y0 = cy * s, z0 = cz * s;
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
                if (!interior.getBlockState(w).canBeReplaced() || !fits(m, BlockPos.asLong(x, y, z), Long.MIN_VALUE)) continue;
                bestD = d;
                best = w;
            }
        if (best == null) return Long.MIN_VALUE;
        interior.setBlock(best, ModBlocks.PIPE_ANCHOR.get().defaultBlockState().setValue(PipeAnchorBlock.MOUNT, face)
                .setValue(PipeAnchorBlock.LINE, line).setValue(PipeAnchorBlock.RING, ring), Block.UPDATE_ALL);
        return BlockPos.asLong(r.localX(best.getX()), r.localY(best.getY()), r.localZ(best.getZ()));
    }

    /**
     * Pure: whether a dock or a guard at {@code key} joins no other line: none of its neighbours is a connection to the outside
     * (other than {@code own}) or a pipe that leads to one.
     */
    static boolean fits(Long2IntOpenHashMap m, long key, long own) {
        for (Direction d : Direction.values()) {
            long n = offset(key, d);
            if (n == own || !m.containsKey(n)) continue;
            int code = m.get(n);
            Plumb p = Plumb.kind(code);
            if (p.port()) return false;
            int lane = -1;
            if (p == Plumb.BUNDLE) {
                lane = InteriorLanes.laneOn(code, d.getOpposite());
                if (lane < 0) continue;
            } else if (p != Plumb.PIPE) {
                continue; // an outlet ends a line: it joins nothing through itself
            }
            for (long q : InteriorPlumbing.search(m, n, lane, key).ports()) if (q != own) return false;
        }
        return true;
    }

    /** Whether guards can be asked for at all (interior pipes on and the interior dimension there). */
    public static boolean required(ServerLevel level) {
        return Features.enabled(Feature.INTERIOR_PIPES) && InteriorManager.interiorLevel(level.getServer()) != null;
    }

    /** How the dock on side {@code face} of tank block {@code member} is guarded (its anchor is put in if missing). */
    public static Guard guardOf(ServerLevel level, TankBlockEntity c, BlockPos member, Direction face, boolean intake) {
        return guardOf(level, c, member, face, -1, intake);
    }

    /** M68 part C: how the dock of slot {@code slot}'s line on that side is guarded for water leaving the tank ({@code intake}) or coming in. */
    public static Guard guardOf(ServerLevel level, TankBlockEntity c, BlockPos member, Direction face, int slot, boolean intake) {
        Anchor a = anchor(level, c, member, face, slot, true);
        return a == null ? Guard.OPEN : guard(a.region(), a.key(), intake);
    }

    /** Water of a filter went through this dock now: the guards and outlets behind it run (stream, current). */
    public static void markFlow(ServerLevel level, TankBlockEntity c, BlockPos member, Direction face, boolean intake, long now) {
        markFlow(level, c, member, face, -1, intake, now);
    }

    /** M68 part C: the same for the dock of slot {@code slot}'s line: only that line's guards and outlets run. */
    public static void markFlow(ServerLevel level, TankBlockEntity c, BlockPos member, Direction face, int slot, boolean intake, long now) {
        Anchor a = anchor(level, c, member, face, slot, false);
        if (a == null) return;
        InteriorPlumbing.Net n = InteriorPlumbing.net(a.region(), a.key());
        InteriorPlumbing.markFlow(a.region().id(), intake ? n.intakes() : n.outflows(), now);
    }

    /** How the connection {@code key} is guarded for water leaving the tank ({@code intake}) or coming in. */
    public static Guard guard(InteriorRegion r, long key, boolean intake) {
        InteriorPlumbing.Net n = InteriorPlumbing.net(r, key);
        if (n.passThrough()) return Guard.PASS_THROUGH;
        return (intake ? !n.intakes().isEmpty() : !n.outflows().isEmpty()) ? Guard.OK : Guard.OPEN;
    }

    /** M68 part C: the ring of the dock of slot {@code slot}'s line follows the ring of its pipe outside (when it is there). */
    public static void updateRing(ServerLevel level, TankBlockEntity c, BlockPos member, Direction face, int slot) {
        Anchor a = anchor(level, c, member, face, slot, false);
        if (a == null || a.shared()) return;
        relabel(level, a.region(), a.key(), a.line(), ring(level, member, face, a.line()));
    }

    /** Whether one of our pipes (filter pipes, the pipes of a filter from before M40) or a filter connector docks on that side. */
    public static boolean docked(Level level, BlockPos member, Direction face) {
        BlockPos n = member.relative(face);
        if (!level.isLoaded(n)) return true; // unknown: keep it
        BlockState s = level.getBlockState(n);
        Direction back = face.getOpposite();
        if (org.overengineer.bettacraft.plumbing.Pipes.isPipe(s)) return org.overengineer.bettacraft.plumbing.Pipes.joinedTowards(level, n, s, back); // M68: any pipe, also of a bundle
        // M68 part B: a water change controller against the tank drains it there (its intake needs a guard like a waste water line)
        if (s.getBlock() instanceof org.overengineer.bettacraft.plumbing.WaterDeviceBlock w && w.kind() == org.overengineer.bettacraft.plumbing.WaterDeviceBlock.Kind.WATER_CHANGER) return true;
        if (s.getBlock() instanceof FilterPartBlock f) return (f.part() == FilterPartBlock.Part.INLET || f.part() == FilterPartBlock.Part.OUTLET) && s.getValue(FilterPartBlock.FACING) == back;
        return false;
    }

    /**
     * Care step: the anchors whose pipe outside is gone are taken out (and those of cells that left the tank). M68 part C: also the
     * docks of bundle lines that no longer dock there; when only one line is left, its dock becomes the face's dock (it keeps its
     * guards).
     */
    public static void sweep(ServerLevel level, TankBlockEntity c) {
        InteriorRegion r = region(level, c, false);
        if (r == null) return;
        TankData t = c.getTank();
        int s = r.scale();
        Long2IntOpenHashMap m = r.equipment().plumbing();
        List<Long> gone = new ArrayList<>();
        List<long[]> single = new ArrayList<>(); // {key, face}
        for (var e : m.long2IntEntrySet()) {
            if (Plumb.kind(e.getIntValue()) != Plumb.ANCHOR) continue;
            long k = e.getLongKey();
            Direction face = Direction.from3DDataValue(Plumb.facing(e.getIntValue()));
            int line = Plumb.extra(e.getIntValue());
            int cx = Math.floorDiv(BlockPos.getX(k), s), cy = Math.floorDiv(BlockPos.getY(k), s), cz = Math.floorDiv(BlockPos.getZ(k), s);
            BlockPos member = t.min().offset(cx, cy, cz);
            if (!t.contains(member) || t.contains(member.relative(face)) || !docked(level, member, face)) {
                gone.add(k);
                continue;
            }
            if (line == 0 || !level.isLoaded(member.relative(face))) continue;
            int[] ls = lines(level, member, face);
            boolean kept = false;
            for (int l : ls) kept |= l == line;
            if (kept) continue;
            if (ls.length == 1 && anchorOf(m, cx, cy, cz, face, s, 0) == Long.MIN_VALUE) single.add(new long[]{k, face.get3DDataValue()});
            else gone.add(k);
        }
        // a single line is left where a bundle docked: the dock of that line (else the lowest) becomes the face's dock, the others go
        single.sort(java.util.Comparator.comparingInt((long[] q) -> {
            Direction face = Direction.from3DDataValue((int) q[1]);
            BlockPos member = t.min().offset(Math.floorDiv(BlockPos.getX(q[0]), s), Math.floorDiv(BlockPos.getY(q[0]), s), Math.floorDiv(BlockPos.getZ(q[0]), s));
            int line = Plumb.extra(m.get(q[0]));
            return line == keptLine(level, member, face) ? 0 : line;
        }));
        List<long[]> relabelled = new ArrayList<>();
        for (long[] q : single) {
            long k = q[0];
            Direction face = Direction.from3DDataValue((int) q[1]);
            int cx = Math.floorDiv(BlockPos.getX(k), s), cy = Math.floorDiv(BlockPos.getY(k), s), cz = Math.floorDiv(BlockPos.getZ(k), s);
            boolean taken = false;
            for (long[] o : relabelled) taken |= o[1] == q[1] && inCell(o[0], cx, cy, cz, s);
            if (taken) {
                gone.add(k);
                continue;
            }
            relabel(level, r, k, 0, ring(level, t.min().offset(cx, cy, cz), face, 0));
            relabelled.add(q);
        }
        if (gone.isEmpty()) return;
        ServerLevel interior = InteriorManager.interiorLevel(level.getServer());
        if (interior == null) return;
        for (long k : gone) {
            BlockPos w = r.toWorld(BlockPos.getX(k), BlockPos.getY(k), BlockPos.getZ(k));
            if (interior.getBlockState(w).getBlock() instanceof PipeAnchorBlock) interior.setBlock(w, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            else m.remove(k); // the index was stale
        }
    }

    // ------------------------------------------------------------------ migration: a dock of the whole face becomes a dock per line

    /**
     * M68 part C, migration: splits the old dock {@code old} (line 0) of side {@code face} of {@code member}, where a bundle now docks
     * with several lines, into one dock per line. What hangs behind it goes with the line it belongs to: intake guards and strainers
     * to the line that sucks (a filter intake, the waste water line of a water change controller), outlet guards and the outlets
     * (lily pipe, nozzle, spray bar) to the line that brings water in (filter return, fresh water); single pieces move to the new dock
     * of their line, interior pipes stay and keep the old dock (now theirs, where it is). Returns false (nothing changed, the old dock
     * stays shared) when interior pipes behind it lead to both kinds or to two lines, another dock touches it, or nothing behind it
     * belongs to a line (lines without a role: plain water pipes).
     */
    static boolean split(ServerLevel level, TankBlockEntity c, InteriorRegion r, BlockPos member, Direction face, long old) {
        var eq = r.equipment();
        Long2IntOpenHashMap failed = SPLIT_FAILED.computeIfAbsent(r.id(), x -> new Long2IntOpenHashMap());
        synchronized (failed) {
            if (failed.containsKey(old) && failed.get(old) == eq.plumbingVersion()) return false;
        }
        ServerLevel interior = InteriorManager.interiorLevel(level.getServer());
        int[] lines = lines(level, member, face);
        if (interior == null || lines.length < 2) return false;
        // the line that sucks and the line that brings water in, by the roles of the pipes outside
        int suction = -1, inflow = -1;
        BlockPos n = member.relative(face);
        for (int line : lines) {
            PipeNetwork net = PipeNetwork.of(level, n, line - 1);
            PipeRole role = net == null || net.shortCircuit() ? null : net.role();
            if (role == null) continue;
            if (role.suction()) {
                if (suction < 0) suction = line;
            } else if (role == PipeRole.RETURN || role == PipeRole.FRESH_OUT) {
                if (inflow < 0) inflow = line;
            }
        }
        Long2IntOpenHashMap m = eq.plumbing();
        List<long[]> moves = new ArrayList<>(); // {key, line}
        int keeper = -1;
        boolean conflict = false;
        for (Direction d : Direction.values()) {
            long nk = offset(old, d);
            if (!m.containsKey(nk)) continue;
            int code = m.get(nk);
            Plumb p = Plumb.kind(code);
            if (p.port()) {
                conflict = true; // another dock right beside it: leave it as it is
                continue;
            }
            if (p.outlet()) {
                int to = p.intake() ? suction : p.outflow() ? inflow : -1;
                if (to > 0) moves.add(new long[]{nk, to});
                continue;
            }
            int lane = p == Plumb.BUNDLE ? InteriorLanes.laneOn(code, d.getOpposite()) : -1;
            if (p == Plumb.BUNDLE && lane < 0) continue;
            InteriorPlumbing.Net b = InteriorPlumbing.search(m, nk, lane, old);
            if (!b.ports().isEmpty()) {
                conflict = true;
                continue;
            }
            int need = 0, to = -1;
            if (!b.intakes().isEmpty() && suction > 0) {
                need++;
                to = suction;
            }
            if (!b.outflows().isEmpty() && inflow > 0) {
                need++;
                to = inflow;
            }
            if (need > 1 || need == 1 && keeper > 0 && keeper != to) conflict = true;
            else if (need == 1) keeper = to;
        }
        final int kept = keeper;
        moves.removeIf(mv -> mv[1] == kept); // they stay at the old dock, which is their line's now
        boolean behind = false;
        for (Direction d : Direction.values()) behind |= m.containsKey(offset(old, d));
        // nothing behind it belongs to one of the lines (lines without a role, outlets of no line): it stays as it is, shared
        if (!conflict && behind && moves.isEmpty() && keeper < 0) conflict = true;
        if (conflict) {
            synchronized (failed) {
                failed.put(old, eq.plumbingVersion());
            }
            if (LogArea.TANK.debug) LogArea.TANK.debug("pipe dock {} of tank {} stays shared by its {} lines (its pipes inside lead to both)", BlockPos.of(old), c.getBlockPos(), lines.length);
            return false;
        }
        // take the moving pieces out, the old dock becomes the keeper's or goes
        List<BlockState> moved = new ArrayList<>();
        for (long[] mv : moves) {
            BlockPos w = r.toWorld(BlockPos.getX(mv[0]), BlockPos.getY(mv[0]), BlockPos.getZ(mv[0]));
            moved.add(interior.getBlockState(w));
            interior.setBlock(w, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        int s = r.scale();
        BlockPos cell = member.subtract(c.getTank().min());
        int cx = cell.getX(), cy = cell.getY(), cz = cell.getZ();
        if (keeper > 0) {
            relabel(level, r, old, keeper, ring(level, member, face, keeper));
        } else {
            BlockPos w = r.toWorld(BlockPos.getX(old), BlockPos.getY(old), BlockPos.getZ(old));
            interior.setBlock(w, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        for (int line : lines) if (anchorOf(m, cx, cy, cz, face, s, line) == Long.MIN_VALUE) place(level, r, cx, cy, cz, face, line, ring(level, member, face, line));
        // the guards and outlets in front of (else beside) their line's dock
        for (int i = 0; i < moves.size(); i++) {
            long from = moves.get(i)[0];
            int line = (int) moves.get(i)[1];
            BlockState st = moved.get(i);
            long a = anchorOf(m, cx, cy, cz, face, s, line);
            BlockPos at = a == Long.MIN_VALUE ? null : putNextTo(interior, r, a, face, st);
            if (at == null) {
                interior.setBlock(r.toWorld(BlockPos.getX(from), BlockPos.getY(from), BlockPos.getZ(from)), st, Block.UPDATE_ALL);
                LogArea.TANK.warnThrottled("anchor_split_room", "no room for a {} beside the dock of line {} in tank {}: it stays where it was", st.getBlock(), line, c.getBlockPos());
            }
        }
        if (LogArea.TANK.debug) LogArea.TANK.debug("pipe dock {} of tank {} split into {} line docks ({} pieces moved, kept by line {})", BlockPos.of(old), c.getBlockPos(), lines.length, moves.size(), keeper);
        return true;
    }

    /**
     * Puts {@code state} (a guard or an outlet) in front of the dock {@code anchor} (into the tank), else beside it, where it joins
     * nothing but that dock. Returns where, or null (no room).
     */
    @Nullable
    private static BlockPos putNextTo(ServerLevel interior, InteriorRegion r, long anchor, Direction face, BlockState state) {
        Long2IntOpenHashMap m = r.equipment().plumbing();
        BlockPos at = r.toWorld(BlockPos.getX(anchor), BlockPos.getY(anchor), BlockPos.getZ(anchor));
        for (Direction d : order(face)) {
            BlockPos p = at.relative(d);
            long k = offset(anchor, d);
            if (!r.containsShapeInterior(p) || !interior.getBlockState(p).canBeReplaced() || !alone(m, k, anchor)) continue;
            interior.setBlock(p, state, Block.UPDATE_ALL);
            return p;
        }
        return null;
    }

    /** In front of a dock on side {@code face} (into the tank) first, then the four sides along the wall. */
    private static List<Direction> order(Direction face) {
        List<Direction> order = new ArrayList<>();
        order.add(face.getOpposite());
        for (Direction d : Direction.values()) if (d != face && d != face.getOpposite()) order.add(d);
        return order;
    }

    /** Pure: no plumbing touches {@code key} but {@code own}. */
    static boolean alone(Long2IntOpenHashMap m, long key, long own) {
        for (Direction d : Direction.values()) {
            long n = offset(key, d);
            if (n != own && m.containsKey(n)) return false;
        }
        return true;
    }

    // ------------------------------------------------------------------ guards from outside

    private static void say(@Nullable Player p, String key, ChatFormatting color, Object... args) {
        if (p != null) p.displayClientMessage(Component.translatable(key, args).withStyle(color), true);
    }

    /**
     * Puts an intake or outlet guard ({@code kind}) behind the connection where a pipe docks on side {@code face} of the tank
     * block {@code member} (used on the glass from outside). Public for the GameTests. M68 part C: where a bundle docks, behind the
     * line that needs it (an intake guard: the first sucking line without one; an outlet guard: the first returning line).
     */
    public static InteractionResult installGuard(ServerLevel level, BlockPos member, Direction face, @Nullable Player player, ItemStack stack, Plumb kind) {
        return installGuard(level, member, face, -1, player, stack, kind);
    }

    /** M68 part C: the same behind the line of slot {@code slot} (the flange that was clicked; -1: the line that needs it). */
    public static InteractionResult installGuard(ServerLevel level, BlockPos member, Direction face, int slot, @Nullable Player player, ItemStack stack, Plumb kind) {
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
        boolean intake = kind.intake();
        if (slot < 0) slot = pickSlot(level, c, member, face, intake);
        Anchor a = anchor(level, c, member, face, slot, true);
        if (a == null) {
            say(player, "message.bettacraft.bulkhead.no_room", ChatFormatting.GOLD);
            return InteractionResult.FAIL;
        }
        InteriorRegion r = a.region();
        long key = a.key();
        InteriorPlumbing.Net behind = InteriorPlumbing.net(r, key);
        if (intake ? !behind.intakes().isEmpty() : !behind.outflows().isEmpty()) {
            say(player, "message.bettacraft.guard.already", ChatFormatting.GOLD);
            return InteractionResult.FAIL;
        }
        ServerLevel interior = InteriorManager.interiorLevel(level.getServer());
        BlockPos at = r.toWorld(BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key));
        // right in front of the connection (into the tank), else beside it; never where it would touch another line
        Direction inward = face.getOpposite();
        Direction facing = inward.getAxis().isHorizontal() ? inward : Direction.NORTH;
        Long2IntOpenHashMap m = r.equipment().plumbing();
        for (Direction d : order(face)) {
            BlockPos p = at.relative(d);
            if (!r.containsShapeInterior(p) || !interior.getBlockState(p).canBeReplaced() || !fits(m, offset(key, d), key)) continue;
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

    /**
     * The slot a guard put on without a clicked flange goes behind: of the bundle's lines that dock there, the first one of the
     * guard's kind (sucking for an intake guard, bringing water in for an outlet guard) that has none yet, else the first of its
     * kind, else -1 (the face's first line).
     */
    static int pickSlot(ServerLevel level, TankBlockEntity c, BlockPos member, Direction face, boolean intake) {
        int[] lines = lines(level, member, face);
        if (lines.length < 2) return -1;
        BlockPos n = member.relative(face);
        int first = -1;
        for (int line : lines) {
            PipeNetwork net = PipeNetwork.of(level, n, line - 1);
            PipeRole role = net == null || net.shortCircuit() ? null : net.role();
            boolean fits = role != null && (intake ? role.suction() : role == PipeRole.RETURN || role == PipeRole.FRESH_OUT);
            if (!fits) continue;
            if (first < 0) first = line - 1;
            Anchor a = anchor(level, c, member, face, line - 1, false);
            if (a == null || guard(a.region(), a.key(), intake) != Guard.OK) return line - 1;
        }
        return first;
    }
}
