package org.overengineer.bettacraft.plants;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.tank.Salinity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.interior.InteriorGround;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;

/**
 * M49 part b: mangroves in the tank interior (switch {@code features.brackish.mangroves}). A vanilla mangrove propagule placed
 * on the floor of an interior (or used on the tank from outside: it is planted at a free spot of the floor) becomes a small
 * mangrove of vanilla blocks at the scale of the interior: in {@link #STAGES} stages (every {@code brackish.mangroveStageDays}
 * in light, a third faster in brackish water) a trunk of mangrove logs grows on arching stilt roots (mangrove roots, muddy at
 * the floor) and a crown of mangrove leaves forms above the water surface, as far as the tank leaves room. The blocks only fill
 * air and water: nothing in the way is replaced. The crown over the water is land for the tank (a paludarium) and the place
 * where the archerfish finds insects; mudskippers climb on the roots.
 * <p>
 * The trees are kept in the tank ({@link TankData#mangroves()}): root position (interior-local), seed, stage, progress.
 */
public final class Mangroves {
    public static final int STAGES = 6;
    /** The stage from which a crown stands over the water. */
    public static final int CROWN_STAGE = 4;

    private Mangroves() {}

    public static boolean enabled() {
        return Features.enabled(Feature.MANGROVES);
    }

    /** One mangrove: its root (interior-local, the block above the floor), the seed of its shape, its stage and the progress to the next. */
    public static final class Tree {
        public final int x, y, z;
        public final long seed;
        public int stage;
        public double progress;
        public boolean crownLogged;

        public Tree(int x, int y, int z, long seed) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.seed = seed;
        }

        public Tree copy() {
            Tree t = new Tree(x, y, z, seed);
            t.stage = stage;
            t.progress = progress;
            t.crownLogged = crownLogged;
            return t;
        }

        public CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putInt("x", x);
            t.putInt("y", y);
            t.putInt("z", z);
            t.putLong("seed", seed);
            t.putInt("stage", stage);
            t.putDouble("progress", progress);
            if (crownLogged) t.putBoolean("crown", true);
            return t;
        }

        @Nullable
        public static Tree load(CompoundTag t) {
            if (!t.contains("x")) return null;
            Tree m = new Tree(t.getInt("x"), t.getInt("y"), t.getInt("z"), t.getLong("seed"));
            m.stage = Math.max(0, Math.min(STAGES, t.getInt("stage")));
            double p = t.getDouble("progress");
            m.progress = Double.isFinite(p) ? Math.max(0, Math.min(1, p)) : 0;
            m.crownLogged = t.getBoolean("crown");
            return m;
        }
    }

    /** Trees whose crown stands over the water (for the archerfish's insects). */
    public static int crowns(TankData t) {
        int n = 0;
        for (Tree m : t.mangroves()) if (m.stage >= CROWN_STAGE) n++;
        return n;
    }

    /** The most mangroves one tank takes: one per four floor blocks of the tank (at least one). */
    public static int maxTrees(TankData t) {
        return Math.max(1, t.sizeX() * t.sizeZ() / 4);
    }

    // ------------------------------------------------------------------ the shape

    /**
     * Pure: the blocks of a full-grown mangrove, root at (0, 0, 0) on the floor, by the order in which they grow (0..1): local
     * offsets to block kinds (0 roots, 1 muddy roots at the floor, 2 log, 3 leaves). {@code water}: interior blocks of water above
     * the root, {@code room}: interior blocks from the root to the top of the tank (exclusive).
     */
    public static Map<BlockPos, double[]> shape(long seed, int water, int room, int scale) {
        Map<BlockPos, double[]> out = new LinkedHashMap<>();
        java.util.Random r = new java.util.Random(seed);
        double s = Math.max(4, scale) / 16.0; // a smaller interior scale makes a smaller tree
        int top = Math.max(3, room - 1);
        int trunkBase = (int) Math.round(Math.max(2, Math.min(top * 0.45, Math.max(2, water * 0.55))));
        int crownBase = Math.max(trunkBase + 2, Math.min(top - 3, Math.max(water + 2, (int) Math.round(top * 0.62))));
        int crownTop = Math.min(top, crownBase + Math.max(2, (int) Math.round(4 * s)));
        // the stilt roots: arches from the foot of the trunk out and down to the floor
        int roots = 5 + r.nextInt(2);
        double reach = Math.max(2.5, Math.min(7 * s, trunkBase * 1.2 + 1));
        double turn = r.nextDouble() * Math.PI * 2;
        for (int i = 0; i < roots; i++) {
            double a = turn + i * Math.PI * 2 / roots + (r.nextDouble() - 0.5) * 0.5;
            double len = reach * (0.75 + r.nextDouble() * 0.35);
            for (double t = 0; t <= 1.0001; t += 0.06) {
                int x = (int) Math.round(Math.cos(a) * len * t);
                int z = (int) Math.round(Math.sin(a) * len * t);
                int y = (int) Math.round(trunkBase * (1 - t * t));
                put(out, x, y, z, y == 0 ? 1 : 0, 0.05 + 0.4 * t);
            }
        }
        // the foot: the propagule takes root, a central stilt carries the trunk
        for (int y = 0; y < trunkBase; y++) put(out, 0, y, 0, y == 0 ? 1 : 0, 0.01 + 0.04 * y / Math.max(1.0, trunkBase));
        // the trunk
        for (int y = trunkBase; y <= crownBase + 1; y++) put(out, 0, y, 0, 2, 0.1 + 0.5 * (y - trunkBase) / Math.max(1.0, crownBase + 1 - trunkBase));
        // a second, thinner fork of the trunk into the crown
        int fx = r.nextBoolean() ? 1 : -1, fz = r.nextBoolean() ? 1 : -1;
        for (int y = crownBase - 1; y <= crownBase + 1; y++) put(out, y >= crownBase ? fx : 0, y, y > crownBase ? fz : 0, 2, 0.6);
        // the crown: a flat dome of leaves above the water
        double rx = Math.max(2.5, 5.5 * s), ry = Math.max(1.5, (crownTop - crownBase) / 1.6 + 0.5);
        int cy = (crownBase + crownTop) / 2 + 1;
        for (int y = crownBase; y <= crownTop; y++) {
            for (int x = (int) -Math.ceil(rx); x <= Math.ceil(rx); x++) {
                for (int z = (int) -Math.ceil(rx); z <= Math.ceil(rx); z++) {
                    double d = (x * x + z * z) / (rx * rx) + (y - cy) * (y - cy) / (ry * ry);
                    if (d > 1.0) continue;
                    if (d > 0.7 && r.nextDouble() < 0.35) continue; // a ragged edge
                    BlockPos p = new BlockPos(x, y, z);
                    if (out.containsKey(p)) continue;
                    put(out, x, y, z, 3, 0.62 + 0.38 * Math.sqrt(d));
                }
            }
        }
        return out;
    }

    private static void put(Map<BlockPos, double[]> out, int x, int y, int z, int kind, double order) {
        BlockPos p = new BlockPos(x, y, z);
        double[] old = out.get(p);
        if (old == null || kind > old[0] && old[0] < 2) out.put(p, new double[]{kind, order});
    }

    // ------------------------------------------------------------------ planting

    /**
     * Plants a mangrove at the interior column (lx, lz) of the tank: a propagule on the floor. Returns null on success, or the
     * key of the message why not.
     */
    @Nullable
    public static String plant(ServerLevel level, TankBlockEntity c, int lx, int lz) {
        if (!enabled()) return "message.bettacraft.mangrove.off";
        TankData t = c.getTank();
        if (t.mangroves().size() >= maxTrees(t)) return "message.bettacraft.mangrove.full";
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        if (r == null || in == null || !r.built()) return "message.bettacraft.mangrove.no_interior";
        lx = Math.max(1, Math.min(r.sizeX() - 2, lx));
        lz = Math.max(1, Math.min(r.sizeZ() - 2, lz));
        for (Tree m : t.mangroves()) if (Math.abs(m.x - lx) < 6 && Math.abs(m.z - lz) < 6) return "message.bettacraft.mangrove.crowded";
        int ly = (InteriorGround.top16(level.getServer(), r, lx, lz) + 15) / 16;
        if (ly >= r.sizeY() - 3) return "message.bettacraft.mangrove.no_room";
        BlockPos at = r.toWorld(lx, ly, lz);
        if (!fillable(in.getBlockState(at))) return "message.bettacraft.mangrove.no_room";
        in.setBlock(at, propagule(in.getBlockState(at)), Block.UPDATE_ALL);
        t.mangroves().add(new Tree(lx, ly, lz, level.getRandom().nextLong()));
        c.markTankChanged();
        return null;
    }

    /** A propagule placed by a player in an interior: becomes a growing mangrove of its tank. */
    public static void placedInInterior(ServerLevel interior, BlockPos pos) {
        if (!enabled()) return;
        InteriorRegion r = InteriorRegions.get(interior.getServer()).get(InteriorRegion.idAt(pos.getX(), pos.getZ()));
        if (r == null || !r.containsInterior(pos)) return;
        ServerLevel tankLevel = interior.getServer().getLevel(r.tankDimension());
        if (tankLevel == null) return;
        TankBlockEntity c = org.overengineer.bettacraft.tank.TankMultiblock.getController(tankLevel, r.tankMin());
        if (c == null || c.getTank().regionId() != r.id()) return;
        TankData t = c.getTank();
        int lx = r.localX(pos.getX()), ly = r.localY(pos.getY()), lz = r.localZ(pos.getZ());
        for (Tree m : t.mangroves()) if (m.x == lx && m.y == ly && m.z == lz) return;
        if (t.mangroves().size() >= maxTrees(t)) return;
        t.mangroves().add(new Tree(lx, ly, lz, interior.getRandom().nextLong()));
        c.markTankChanged();
    }

    private static BlockState propagule(BlockState here) {
        return Blocks.MANGROVE_PROPAGULE.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, here.getFluidState().is(Fluids.WATER));
    }

    /** Air, water or a plant that can give way (the tree never replaces anything else). */
    static boolean fillable(BlockState s) {
        return s.isAir() || s.is(Blocks.WATER) || s.canBeReplaced() && !(s.getBlock() instanceof AquaticPlantBlock);
    }

    // ------------------------------------------------------------------ growth

    /** Days per stage now: slower in the dark, a third faster in brackish water. */
    static double stageDays(ServerLevel level, TankBlockEntity c) {
        double d = ServerConfig.getOr(ServerConfig.MANGROVE_STAGE_DAYS, 1.5);
        if (Salinity.kind(c.getTank().salinity()) == Salinity.Kind.BRACKISH) d /= 1.5;
        int light = org.overengineer.bettacraft.tank.plants.TankPlants.light(level, c);
        return light < 7 ? d * 4 : d;
    }

    /** The care step: every tree grows towards its next stage; a tree whose propagule or trunk is gone is forgotten. */
    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        TankData t = c.getTank();
        if (t.mangroves().isEmpty() || days <= 0 || !enabled()) return;
        InteriorRegion r = InteriorRegions.get(level.getServer()).get(t.regionId());
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        if (r == null || in == null || !r.built()) return;
        double per = stageDays(level, c);
        for (var it = t.mangroves().iterator(); it.hasNext(); ) {
            Tree m = it.next();
            BlockPos root = r.toWorld(m.x, m.y, m.z);
            if (!in.isLoaded(root)) continue;
            BlockState rs = in.getBlockState(root);
            if (!rs.is(Blocks.MANGROVE_PROPAGULE) && !rs.is(Blocks.MANGROVE_ROOTS) && !rs.is(Blocks.MUDDY_MANGROVE_ROOTS) && !rs.is(Blocks.MANGROVE_LOG)) {
                it.remove(); // taken out
                c.markTankChanged();
                continue;
            }
            if (m.stage >= STAGES) continue;
            if (t.waterMb() <= 0) continue; // a mangrove needs its feet in water
            m.progress += days / per;
            if (m.progress < 1) continue;
            m.progress = 0;
            grow(level, c, r, in, m, m.stage + 1);
        }
    }

    /** Grows the tree to {@code stage} (the blocks of every stage up to it that have room). */
    public static void grow(ServerLevel level, TankBlockEntity c, InteriorRegion r, ServerLevel in, Tree m, int stage) {
        TankData t = c.getTank();
        m.stage = Math.min(STAGES, stage);
        double surface = t.waterMb() <= 0 ? 0 : TankCare.waterLocalHeight(t, r.scale());
        int water = Math.max(0, (int) Math.floor(surface) - m.y);
        int room = r.sizeY() - m.y;
        double upTo = m.stage / (double) STAGES;
        for (var e : shape(m.seed, water, room, r.scale()).entrySet()) {
            if (e.getValue()[1] > upTo + 1e-9) continue;
            BlockPos off = e.getKey();
            int lx = m.x + off.getX(), ly = m.y + off.getY(), lz = m.z + off.getZ();
            if (lx < 0 || lz < 0 || ly < 0 || lx >= r.sizeX() || ly >= r.sizeY() || lz >= r.sizeZ() || !r.inShape(lx, ly, lz)) continue;
            BlockPos p = r.toWorld(lx, ly, lz);
            BlockState here = in.getBlockState(p);
            boolean root = off.getX() == 0 && off.getY() == 0 && off.getZ() == 0;
            if (!root && !fillable(here)) continue;
            if (root && !here.is(Blocks.MANGROVE_PROPAGULE) && !fillable(here) && !here.is(Blocks.MANGROVE_ROOTS)) continue;
            boolean wet = here.getFluidState().is(Fluids.WATER) || ly < surface;
            BlockState s = switch ((int) e.getValue()[0]) {
                case 1 -> Blocks.MUDDY_MANGROVE_ROOTS.defaultBlockState();
                case 2 -> Blocks.MANGROVE_LOG.defaultBlockState();
                case 3 -> Blocks.MANGROVE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true).setValue(LeavesBlock.WATERLOGGED, wet);
                default -> Blocks.MANGROVE_ROOTS.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, wet);
            };
            if (root) s = Blocks.MUDDY_MANGROVE_ROOTS.defaultBlockState(); // the propagule took root
            if (here.equals(s)) continue;
            in.setBlock(p, s, Block.UPDATE_CLIENTS);
        }
        if (m.stage >= CROWN_STAGE && !m.crownLogged && surface > 0) {
            m.crownLogged = true;
            TankCare.log(level, c, "log.bettacraft.mangrove.crown");
        }
        c.markTankChanged();
    }

    /** For the menu and the tests: {trees, crowns, highest stage}. */
    public static int[] summary(TankData t) {
        int crowns = 0, max = 0;
        for (Tree m : t.mangroves()) {
            if (m.stage >= CROWN_STAGE) crowns++;
            max = Math.max(max, m.stage);
        }
        return new int[]{t.mangroves().size(), crowns, max};
    }

    /** All trees of a tank as (world) roots in the interior (GameTests). */
    public static List<BlockPos> roots(Level interior, InteriorRegion r, TankData t) {
        List<BlockPos> out = new ArrayList<>();
        for (Tree m : t.mangroves()) out.add(r.toWorld(m.x, m.y, m.z));
        return out;
    }
}
