package org.overengineer.bettacraft.gametest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.genetics.BettaSpecies;
import org.overengineer.bettacraft.registry.ModAttachments;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;

/**
 * M32 part b, dev group {@code -Pcreate} (Create, Create Aeronautics and Sable in run/create-mods): tanks travel on a
 * mechanical bearing (moved up, turned by a quarter) and on a Sable sub-level, by Create's and Sable's public classes
 * through reflection. Nothing may be lost or doubled: blocks, water, small animals, the interior region and its blocks,
 * the bettas (packed during the trip, back afterwards, each exactly once). Without Create the tests pass at once.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CreateGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private CreateGameTests() {}

    private static boolean create() {
        return ModList.get().isLoaded("create");
    }

    private static Block block(String id) {
        Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
        if (b == Blocks.AIR) throw new GameTestAssertException("no block " + id);
        return b;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState with(BlockState s, String property, Comparable value) {
        for (Property<?> p : s.getProperties()) if (p.getName().equals(property)) return s.setValue((Property) p, value);
        throw new GameTestAssertException("no property " + property + " on " + s);
    }

    private static Field field(Class<?> c, String name) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try {
                Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new GameTestAssertException("no field " + c.getName() + "." + name);
    }

    private static Method method(Class<?> c, String name, int params) {
        for (Method m : c.getMethods()) if (m.getName().equals(name) && m.getParameterCount() == params) return m;
        throw new GameTestAssertException("no method " + c.getName() + "." + name);
    }

    /** A tank of sx x sy x sz blocks at rel (x, y, z), full of water, with two bettas and two snails. */
    private static TankBlockEntity stockedTank(GameTestHelper h, int x, int y, int z, int sx, int sy, int sz) {
        TankBlockEntity c = CritterGameTests.tank(h, x, y, z, sx, sy, sz, (long) sx * sy * sz * 1000L);
        c.getTank().setWaterQuality(1F);
        RandomSource r = RandomSource.create(32);
        CommunityFishGameTests.betta(h, c, BettaGenotype.randomWild(BettaSpecies.SPLENDENS, r), false);
        CommunityFishGameTests.betta(h, c, BettaGenotype.randomWild(BettaSpecies.SPLENDENS, r), true);
        org.overengineer.bettacraft.critters.TankCritters.add(h.getLevel(), c, CritterSpecies.APPLE_SNAIL, CritterGenes.pure(0, 0.5F), true, 30);
        org.overengineer.bettacraft.critters.TankCritters.add(h.getLevel(), c, CritterSpecies.APPLE_SNAIL, CritterGenes.pure(0, 0.5F), false, 30);
        return c;
    }

    private static List<UUID> animals(GameTestHelper h, TankBlockEntity c) {
        List<UUID> out = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(h.getLevel(), c.getTank())) out.add(e.getUUID());
        return out;
    }

    /** How many live entities with this id the level has (0 while packed, exactly 1 afterwards). */
    private static int alive(ServerLevel level, UUID id) {
        Entity e = level.getEntity(id);
        return e != null && e.isAlive() ? 1 : 0;
    }

    /** The bearing below {@code anchor} (rel), facing up, and its block entity. */
    private static BlockEntity bearing(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel, with(block("create:mechanical_bearing").defaultBlockState(), "facing", Direction.UP));
        BlockEntity be = h.getBlockEntity(rel);
        if (be == null) throw new GameTestAssertException("no bearing block entity");
        return be;
    }

    /** Assembles the bearing's contraption; returns the contraption entity. */
    private static Entity assemble(BlockEntity bearing) throws Exception {
        method(bearing.getClass(), "assemble", 0).invoke(bearing);
        Object moved = field(bearing.getClass(), "movedContraption").get(bearing);
        if (moved == null) {
            Object ex = field(bearing.getClass(), "lastException").get(bearing);
            throw new GameTestAssertException("the bearing did not assemble: " + ex);
        }
        hold(bearing);
        return (Entity) moved;
    }

    /** A bearing without rotation would put its contraption down again on its next tick: it waits for the test. */
    private static void hold(BlockEntity bearing) {
        try {
            field(bearing.getClass(), "assembleNextTick").setBoolean(bearing, false);
        } catch (IllegalAccessException e) {
            throw new GameTestAssertException("bearing: " + e);
        }
    }

    /** Puts the contraption down after moving it by {@code offset} and turning it by {@code angle} degrees. */
    private static void disassemble(BlockEntity bearing, Entity contraption, BlockPos offset, float angle) throws Exception {
        contraption.setPos(contraption.getX() + offset.getX(), contraption.getY() + offset.getY(), contraption.getZ() + offset.getZ());
        method(contraption.getClass(), "setAngle", 1).invoke(contraption, angle);
        method(bearing.getClass(), "disassemble", 0).invoke(bearing);
    }

    private static int interiorBlocks(ServerLevel interior, InteriorRegion r, Block b) {
        int n = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = 0; x < r.sizeX(); x++)
            for (int y = 0; y < r.sizeY(); y++)
                for (int z = 0; z < r.sizeZ(); z++) if (interior.getBlockState(m.set(r.toWorld(x, y, z))).is(b)) n++;
        return n;
    }

    // ------------------------------------------------------------------ tests

    /**
     * A 2x2x2 tank on a bearing goes up three blocks: on the contraption the bettas are packed (not in the world), the
     * tank is gone from its place; put down, the same tank stands three blocks higher with its water, snails, interior
     * region (same id, its stone still there) and the same two bettas, each exactly once, adopted by it.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_create", timeoutTicks = 200)
    public static void bearingCarriesTankWithEverything(GameTestHelper h) throws Exception {
        if (!create()) {
            h.succeed();
            return;
        }
        ServerLevel level = h.getLevel();
        BlockEntity bearing = bearing(h, new BlockPos(4, 1, 4));
        TankBlockEntity c = stockedTank(h, 4, 2, 4, 2, 2, 2);
        TankData t = c.getTank();
        UUID tankId = t.id();
        long water = t.waterMb();
        int critters = t.critters().size();
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        ServerLevel interior = InteriorManager.interiorLevel(level.getServer());
        interior.setBlockAndUpdate(r.toWorld(3, 0, 3), Blocks.STONE.defaultBlockState());
        interior.setBlockAndUpdate(r.toWorld(20, 5, 9), Blocks.STONE.defaultBlockState());
        int regionId = r.id();
        List<UUID> bettas = animals(h, c);
        h.assertTrue(bettas.size() == 2, "two bettas in the tank: " + bettas.size());
        BlockPos oldAnchor = c.getBlockPos();
        Entity contraption = assemble(bearing);
        h.assertTrue(!(level.getBlockState(oldAnchor).getBlock() instanceof TankBlock), "the tank left its place");
        h.onEachTick(() -> hold(bearing));
        h.runAfterDelay(2, () -> {
            for (UUID u : bettas) h.assertTrue(alive(level, u) == 0, "a betta travels packed, not in the world");
            h.assertTrue(InteriorRegions.get(level.getServer()).get(regionId) != null, "the region stays while the tank travels");
            try {
                disassemble(bearing, contraption, new BlockPos(0, 3, 0), 0F);
            } catch (Exception e) {
                throw new GameTestAssertException("disassembly failed: " + e);
            }
            h.succeedWhen(() -> {
                TankBlockEntity n = TankMultiblock.getController(level, oldAnchor.above(3));
                h.assertTrue(n != null, "a tank stands three blocks higher");
                TankData nt = n.getTank();
                h.assertTrue(nt.id().equals(tankId), "the same tank");
                h.assertTrue(nt.memberCount() == 8 && nt.min().equals(oldAnchor.above(3)), "all eight blocks: " + nt);
                h.assertTrue(nt.waterMb() == water, "the water: " + nt.waterMb() + " of " + water);
                h.assertTrue(nt.critters().size() == critters, "the snails: " + nt.critters().size());
                for (var k : nt.critters()) h.assertTrue(nt.contains(BlockPos.containing(k.x, k.y + 0.01, k.z)), "a snail inside the moved tank");
                h.assertTrue(nt.regionId() == regionId, "the same interior region");
                InteriorRegion nr = InteriorRegions.get(level.getServer()).get(regionId);
                h.assertTrue(nr != null && interiorBlocks(interior, nr, Blocks.STONE) == 2, "the interior keeps its stones");
                for (UUID u : bettas) {
                    h.assertTrue(alive(level, u) == 1, "the betta is back exactly once");
                    Entity e = level.getEntity(u);
                    h.assertTrue(nt.contains(e.blockPosition()), "the betta swims in the moved tank: " + e.blockPosition());
                    h.assertTrue(e.getData(ModAttachments.TANK_REF.get()).tankId().equals(tankId)
                            && e.getData(ModAttachments.TANK_REF.get()).controller().equals(n.getBlockPos()), "adopted by the moved tank");
                }
                h.assertTrue(n.stowed().isEmpty(), "nothing left packed");
                h.assertTrue(TankInhabitants.liveInhabitants(level, nt).size() == 2, "two bettas in the moved tank");
            });
        });
    }

    /**
     * A 3x1x2 tank turned by a quarter on the bearing: it comes back as 2x1x3, its interior turned with it (a new region of
     * the turned shape; the marker block at the turned place, the old region gone), the front turned, the bettas inside.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_create", timeoutTicks = 200)
    public static void bearingTurnsTankAndInterior(GameTestHelper h) throws Exception {
        if (!create()) {
            h.succeed();
            return;
        }
        ServerLevel level = h.getLevel();
        BlockEntity bearing = bearing(h, new BlockPos(10, 1, 10));
        TankBlockEntity c = stockedTank(h, 10, 2, 10, 3, 1, 2);
        c.getTank().setFront(Direction.SOUTH);
        UUID tankId = c.getTank().id();
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        ServerLevel interior = InteriorManager.interiorLevel(level.getServer());
        // marker in the tank block (x+2, z+0) of the 3 x 2 footprint: the far end of the long side
        interior.setBlockAndUpdate(r.toWorld(2 * 16 + 8, 0, 4), Blocks.GOLD_BLOCK.defaultBlockState());
        int oldRegion = r.id();
        List<UUID> bettas = animals(h, c);
        BlockPos anchor = c.getBlockPos();
        Entity contraption = assemble(bearing);
        h.onEachTick(() -> hold(bearing));
        h.runAfterDelay(2, () -> {
            try {
                disassemble(bearing, contraption, BlockPos.ZERO, 90F);
            } catch (Exception e) {
                throw new GameTestAssertException("disassembly failed: " + e);
            }
            h.succeedWhen(() -> {
                TankBlockEntity n = null;
                for (int dx = -3; dx <= 3 && n == null; dx++)
                    for (int dz = -3; dz <= 3 && n == null; dz++) {
                        TankBlockEntity k = TankMultiblock.getController(level, anchor.offset(dx, 0, dz));
                        if (k != null && k.getTank().id().equals(tankId)) n = k;
                    }
                h.assertTrue(n != null, "the turned tank formed");
                TankData nt = n.getTank();
                h.assertTrue(nt.memberCount() == 6 && nt.sizeX() == 2 && nt.sizeZ() == 3, "turned to 2 x 3: " + nt);
                h.assertTrue(nt.front().getAxis() == Direction.Axis.X, "the front turned with it: " + nt.front());
                h.assertTrue(InteriorRegions.get(level.getServer()).get(oldRegion) == null, "the old region is gone");
                InteriorRegion nr = InteriorRegions.get(level.getServer()).get(nt.regionId());
                h.assertTrue(nr != null && nr.sizeX() == 32 && nr.sizeZ() == 48, "a region of the turned shape");
                h.assertTrue(interiorBlocks(interior, nr, Blocks.GOLD_BLOCK) == 1, "the marker block came along");
                for (UUID u : bettas) {
                    h.assertTrue(alive(level, u) == 1, "the betta is back exactly once");
                    h.assertTrue(nt.contains(level.getEntity(u).blockPosition()), "the betta is in the turned tank");
                }
                // the marker lies in the block that was at the far end of the long side: after a quarter turn about the
                // bearing's axis that block is in the turned tank's z extent
                BlockPos gold = null;
                for (int x = 0; x < nr.sizeX() && gold == null; x++)
                    for (int z = 0; z < nr.sizeZ() && gold == null; z++)
                        if (interior.getBlockState(nr.toWorld(x, 0, z)).is(Blocks.GOLD_BLOCK)) gold = new BlockPos(x, 0, z);
                h.assertTrue(gold != null, "found the marker");
            });
        });
    }

    /** Only one block of a long tank sits on the bearing: all its blocks move together (they are attached to each other). */
    @GameTest(template = EMPTY, batch = "bettacraft_create", timeoutTicks = 200)
    public static void tankMovesAsAWhole(GameTestHelper h) throws Exception {
        if (!create()) {
            h.succeed();
            return;
        }
        ServerLevel level = h.getLevel();
        BlockEntity bearing = bearing(h, new BlockPos(18, 1, 4));
        TankBlockEntity c = stockedTank(h, 16, 2, 4, 4, 1, 1);
        UUID tankId = c.getTank().id();
        Set<Long> old = new HashSet<>(c.getTank().members());
        Entity contraption = assemble(bearing);
        for (long l : old) h.assertTrue(level.getBlockState(BlockPos.of(l)).isAir(), "every block of the tank left");
        h.onEachTick(() -> hold(bearing));
        h.runAfterDelay(2, () -> {
            try {
                disassemble(bearing, contraption, new BlockPos(0, 2, 0), 0F);
            } catch (Exception e) {
                throw new GameTestAssertException("disassembly failed: " + e);
            }
            h.succeedWhen(() -> {
                TankBlockEntity n = TankMultiblock.getController(level, BlockPos.of(old.iterator().next()).above(2));
                h.assertTrue(n != null && n.getTank().id().equals(tankId) && n.getTank().memberCount() == 4, "the whole tank came back");
                h.assertTrue(TankInhabitants.liveInhabitants(level, n.getTank()).size() == 2, "with both bettas");
            });
        });
    }

    /** Eggs in the bubble nest: the tank refuses the trip, the contraption does not assemble and nothing changes. */
    @GameTest(template = EMPTY, batch = "bettacraft_create", timeoutTicks = 100)
    public static void tankWithEggsStaysPut(GameTestHelper h) throws Exception {
        if (!create()) {
            h.succeed();
            return;
        }
        ServerLevel level = h.getLevel();
        BlockEntity bearing = bearing(h, new BlockPos(24, 1, 20));
        TankBlockEntity c = stockedTank(h, 24, 2, 20, 2, 2, 2);
        List<UUID> bettas = animals(h, c);
        BettaEntity male = null, female = null;
        for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) {
            if (e instanceof BettaEntity b) {
                if (b.isFemale()) female = b;
                else male = b;
            }
        }
        h.assertTrue(male != null && female != null, "a pair");
        TankData t = c.getTank();
        t.setTargetTemp(27F);
        t.setWaterTemp(27F);
        RandomSource r = RandomSource.create(7);
        for (int i = 0; i < 60 && (t.nest() == null || !t.nest().hasEggs()); i++) org.overengineer.bettacraft.tank.TankBreeding.step(level, c, 0.05, r);
        h.assertTrue(t.nest() != null && t.nest().hasEggs(), "eggs in the nest");
        method(bearing.getClass(), "assemble", 0).invoke(bearing);
        h.assertTrue(field(bearing.getClass(), "movedContraption").get(bearing) == null, "the bearing did not take the tank");
        h.runAfterDelay(2, () -> {
            h.assertTrue(TankMultiblock.getController(level, c.getBlockPos()) == c, "the tank stands where it was");
            for (UUID u : bettas) h.assertTrue(alive(level, u) == 1, "the bettas are still there");
            h.succeed();
        });
    }

    /** With features.createContraptions off Create cannot move a tank (as before M32). */
    @GameTest(template = EMPTY, batch = "bettacraft_create_config", timeoutTicks = 60)
    public static void switchOffKeepsTanksUnmovable(GameTestHelper h) throws Exception {
        if (!create()) {
            h.succeed();
            return;
        }
        boolean saved = CommonConfig.FEATURE_CREATE_CONTRAPTIONS.get();
        try {
            CommonConfig.FEATURE_CREATE_CONTRAPTIONS.set(false);
            BlockEntity bearing = bearing(h, new BlockPos(4, 1, 20));
            TankBlockEntity c = CritterGameTests.tank(h, 4, 2, 20, 2, 1, 2, 4000L);
            method(bearing.getClass(), "assemble", 0).invoke(bearing);
            h.assertTrue(field(bearing.getClass(), "movedContraption").get(bearing) == null, "the bearing did not take the tank");
            h.assertTrue(TankMultiblock.getController(h.getLevel(), c.getBlockPos()) == c, "the tank stays");
        } finally {
            CommonConfig.FEATURE_CREATE_CONTRAPTIONS.set(saved);
        }
        h.succeed();
    }

    /**
     * Sable (Create Aeronautics): the tank is assembled into a sub-level like a physics assembler does; there it is the
     * same tank (region, water) with its bettas packed and its care paused. Disassembled a quarter turned onto the ground
     * again, the bettas come back into the turned tank.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_create", timeoutTicks = 300)
    public static void airshipCarriesTank(GameTestHelper h) throws Exception {
        if (!ModList.get().isLoaded("sable")) {
            h.succeed();
            return;
        }
        ServerLevel level = h.getLevel();
        TankBlockEntity c = stockedTank(h, 12, 3, 22, 2, 1, 3);
        UUID tankId = c.getTank().id();
        long water = c.getTank().waterMb();
        int regionId = InteriorManager.ensureRegion(level, c).id();
        List<UUID> bettas = animals(h, c);
        BlockPos anchor = c.getBlockPos();
        Class<?> helper = Class.forName("dev.ryanhcode.sable.api.SubLevelAssemblyHelper");
        Class<?> predicate = Class.forName("dev.ryanhcode.sable.api.SubLevelAssemblyHelper$FrontierPredicate");
        Object onlyTanks = Proxy.newProxyInstance(predicate.getClassLoader(), new Class<?>[]{predicate}, (p, m, a) -> {
            if (m.getName().equals("isValidConnection")) return ((BlockState) a[3]).getBlock() instanceof TankBlock;
            return m.getName().equals("hashCode") ? 0 : m.getName().equals("equals") ? p == a[0] : null;
        });
        Object gathered = method(helper, "gatherConnectedBlocks", 4).invoke(null, anchor, level, 64, onlyTanks);
        Object blocks = gathered.getClass().getMethod("blocks").invoke(gathered);
        Object bounds = gathered.getClass().getMethod("boundingBox").invoke(gathered);
        h.assertTrue(blocks instanceof Set<?> s && s.size() == 6, "Sable gathers the six tank blocks: " + blocks);
        Object subLevel = method(helper, "assembleBlocks", 4).invoke(null, level, anchor, blocks, bounds);
        Object plot = subLevel.getClass().getMethod("getPlot").invoke(subLevel);
        BlockPos plotAnchor = (BlockPos) plot.getClass().getMethod("getCenterBlock").invoke(plot);
        h.runAfterDelay(5, () -> {
            TankBlockEntity up = TankMultiblock.getController(level, plotAnchor);
            h.assertTrue(up != null && up.getTank().id().equals(tankId), "the tank is on the airship: " + up);
            h.assertTrue(up.getTank().regionId() == regionId && up.getTank().waterMb() == water, "with its region and water");
            h.assertTrue(up.stowed().size() == 2, "the bettas travel packed: " + up.stowed().size());
            for (UUID u : bettas) h.assertTrue(alive(level, u) == 0, "no betta left behind in the world");
            h.assertTrue(!(level.getBlockState(anchor).getBlock() instanceof TankBlock), "nothing left at the old place");
            try {
                Class<?> sim = Class.forName("dev.simulated_team.simulated.util.SimAssemblyHelper");
                Method dis = null;
                for (Method m : sim.getMethods()) if (m.getName().equals("disassembleSubLevel")) dis = m;
                if (dis == null) throw new GameTestAssertException("no disassembleSubLevel");
                dis.invoke(null, level, subLevel, plotAnchor, anchor.above(2), Rotation.CLOCKWISE_90, false);
            } catch (ReflectiveOperationException e) {
                throw new GameTestAssertException("disassembly failed: " + e);
            }
            h.succeedWhen(() -> {
                TankBlockEntity n = null;
                for (int dx = -3; dx <= 3 && n == null; dx++)
                    for (int dz = -3; dz <= 3 && n == null; dz++) {
                        TankBlockEntity k = TankMultiblock.getController(level, anchor.offset(dx, 2, dz));
                        if (k != null && k.getTank().id().equals(tankId)) n = k;
                    }
                h.assertTrue(n != null, "the tank is back on the ground");
                h.assertTrue(n.getTank().memberCount() == 6 && n.getTank().sizeX() == 3 && n.getTank().sizeZ() == 2, "turned: " + n.getTank());
                h.assertTrue(n.getTank().waterMb() == water, "the water");
                for (UUID u : bettas) {
                    h.assertTrue(alive(level, u) == 1, "the betta is back exactly once");
                    h.assertTrue(n.getTank().contains(level.getEntity(u).blockPosition()), "in the tank");
                }
            });
        });
    }
}
