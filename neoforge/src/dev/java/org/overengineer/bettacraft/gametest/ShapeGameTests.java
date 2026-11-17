package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankWater;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;

/**
 * M10 GameTests: free tank shapes (L, T, steps): building and merging, splitting into connected parts, water volume and
 * fill per layer, fish staying in the shape, edge flags of concave corners, equipment in a notch, the interior region of
 * an L with its walls and drops, an M9 cuboid tank (with its pre-M10 region) growing into an L, and the cuboid-only
 * config.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShapeGameTests {
    private static final String EMPTY = "empty_32x10x32";
    /** Tests that switch the global shape config run alone in their own batch. */
    private static final String CUBOID_BATCH = "bettacraft_cuboid_only";

    private ShapeGameTests() {}

    private static void add(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel, ModBlocks.TANK.get().defaultBlockState());
        TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(rel), null);
    }

    private static void add(GameTestHelper h, List<BlockPos> rel) {
        for (BlockPos p : rel) add(h, p);
    }

    private static TankBlockEntity controller(GameTestHelper h, BlockPos rel) {
        TankBlockEntity c = TankMultiblock.getController(h.getLevel(), h.absolutePos(rel));
        if (c == null) throw new GameTestAssertException("no controller for " + rel);
        return c;
    }

    private static List<BlockPos> rel(int[]... ps) {
        List<BlockPos> l = new ArrayList<>();
        for (int[] p : ps) l.add(new BlockPos(p[0], p[1], p[2]));
        return l;
    }

    /** Big L in the x/z plane at y 1: a 3x3 square without its 2x2 corner (5 blocks), notch at x 4..5, z 4..5. */
    private static List<BlockPos> bigL() {
        return rel(new int[]{3, 1, 3}, new int[]{4, 1, 3}, new int[]{5, 1, 3}, new int[]{3, 1, 4}, new int[]{3, 1, 5});
    }

    private static ServerLevel interior(GameTestHelper h) {
        ServerLevel l = InteriorManager.interiorLevel(h.getLevel().getServer());
        if (l == null) throw new GameTestAssertException("interior dimension not loaded in the GameTest server");
        return l;
    }

    // ------------------------------------------------------------------ forming / merging / splitting

    @GameTest(template = EMPTY)
    public static void lShapeBuildsBlockByBlock(GameTestHelper h) {
        List<BlockPos> l = bigL();
        add(h, l);
        TankBlockEntity c = controller(h, l.getFirst());
        for (BlockPos p : l) h.assertTrue(controller(h, p) == c, "all blocks of the L share one controller");
        TankData t = c.getTank();
        h.assertTrue(t.memberCount() == 5 && !t.isCuboid() && t.sizeX() == 3 && t.sizeZ() == 3, "5-block L in a 3x1x3 box: " + t);
        h.assertFalse(t.contains(h.absolutePos(new BlockPos(4, 1, 4))), "the notch is not part of the tank");
        BlockState corner = h.getBlockState(new BlockPos(3, 1, 3));
        h.assertTrue(TankBlock.isConnected(corner, Direction.EAST) && TankBlock.isConnected(corner, Direction.SOUTH)
                && !TankBlock.isConnected(corner, Direction.WEST), "corner connects along both arms");
        h.assertTrue(t.capacityMb() == 5000, "capacity of 5 blocks");
        h.succeed();
    }

    /** A block that touches two separate tanks joins them into one free shape (here: a T and a step). */
    @GameTest(template = EMPTY)
    public static void tAndStepMergeThroughOneBlock(GameTestHelper h) {
        // two separate bars ...
        add(h, rel(new int[]{2, 1, 2}, new int[]{3, 1, 2}, new int[]{4, 1, 2}));
        add(h, rel(new int[]{3, 1, 4}, new int[]{3, 2, 4}));
        TankBlockEntity a = controller(h, new BlockPos(2, 1, 2)), b = controller(h, new BlockPos(3, 1, 4));
        h.assertTrue(a != b, "separate before");
        TankInteractions.changeWater(h.getLevel(), a, 2000);
        TankInteractions.changeWater(h.getLevel(), b, 500);
        // ... joined by the stem of the T
        add(h, new BlockPos(3, 1, 3));
        TankBlockEntity c = controller(h, new BlockPos(3, 1, 3));
        h.assertTrue(controller(h, new BlockPos(2, 1, 2)) == c && controller(h, new BlockPos(3, 2, 4)) == c, "one tank");
        TankData t = c.getTank();
        h.assertTrue(t.memberCount() == 6 && t.waterMb() == 2500, "6 blocks, water added up: " + t);
        // a step: the column at (3, 2, 4) is the only block of layer 2
        h.assertTrue(t.layerCounts().length == 2 && t.layerCounts()[0] == 5 && t.layerCounts()[1] == 1, "layers 5 + 1");
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void sizeLimitStillApplies(GameTestHelper h) {
        int max = ServerConfig.maxSize(0);
        if (max > 28) {
            h.succeed(); // the template is 32 wide
            return;
        }
        List<BlockPos> row = new ArrayList<>();
        for (int x = 0; x < max; x++) row.add(new BlockPos(1 + x, 1, 2));
        add(h, row);
        // an L arm at the far end still fits (the box grows in z) ...
        add(h, new BlockPos(max, 1, 3));
        h.assertTrue(controller(h, new BlockPos(max, 1, 3)) == controller(h, row.getFirst()), "L within the limit joins");
        // ... one more block along x does not
        add(h, new BlockPos(max + 1, 1, 2));
        h.assertTrue(controller(h, new BlockPos(max + 1, 1, 2)) != controller(h, row.getFirst()), "beyond the maximum size: separate");
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void splitKeepsConnectedPartsAndTheirWater(GameTestHelper h) {
        // a U of 5 blocks: (2..4, z 2) plus (2, z 3) and (4, z 3); water 4000 = 800 per block
        List<BlockPos> u = rel(new int[]{2, 1, 2}, new int[]{3, 1, 2}, new int[]{4, 1, 2}, new int[]{2, 1, 3}, new int[]{4, 1, 3});
        add(h, u);
        TankBlockEntity c = controller(h, u.getFirst());
        h.assertTrue(c.getTank().memberCount() == 5, "U formed");
        TankInteractions.changeWater(h.getLevel(), c, 4000);
        // breaking a corner keeps one L-shaped tank
        h.getLevel().destroyBlock(h.absolutePos(new BlockPos(2, 1, 3)), false);
        TankBlockEntity l = controller(h, new BlockPos(2, 1, 2));
        h.assertTrue(l.getTank().memberCount() == 4 && !l.getTank().isCuboid(), "L remainder stays one tank: " + l.getTank());
        h.assertTrue(l.getTank().waterMb() == 3200, "corner's 800 mB gone, got " + l.getTank().waterMb());
        // breaking the middle of the bottom bar splits into the two arms, each with the water of its blocks
        h.getLevel().destroyBlock(h.absolutePos(new BlockPos(3, 1, 2)), false);
        TankBlockEntity left = controller(h, new BlockPos(2, 1, 2)), right = controller(h, new BlockPos(4, 1, 2));
        h.assertTrue(left != right, "two parts");
        h.assertTrue(left.getTank().memberCount() == 1 && right.getTank().memberCount() == 2, "1 + 2 blocks");
        h.assertTrue(left.getTank().waterMb() == 800 && right.getTank().waterMb() == 1600,
                "water per part: " + left.getTank().waterMb() + " + " + right.getTank().waterMb());
        h.runAfterDelay(60, () -> {
            for (BlockPos p : List.of(new BlockPos(2, 1, 3), new BlockPos(3, 1, 2))) h.assertTrue(h.getBlockState(p).isAir(), "no water left at " + p);
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ water

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void stepFillsLayerByLayerAndDoesNotLeak(GameTestHelper h) {
        // step: 2x1x1 bottom, one block on top of the east one, plus an L arm
        List<BlockPos> step = rel(new int[]{4, 1, 4}, new int[]{5, 1, 4}, new int[]{5, 2, 4}, new int[]{4, 1, 5});
        add(h, step);
        TankBlockEntity c = controller(h, step.getFirst());
        TankData t = c.getTank();
        h.assertTrue(t.memberCount() == 4 && t.capacityMb() == 4000, "4 blocks");
        TankInteractions.changeWater(h.getLevel(), c, 3500);
        h.assertTrue(h.getBlockState(new BlockPos(4, 1, 4)).getValue(TankBlock.FILL) == 16, "bottom layer full");
        h.assertTrue(h.getBlockState(new BlockPos(4, 1, 5)).getValue(TankBlock.FILL) == 16, "the arm is full too");
        h.assertTrue(h.getBlockState(new BlockPos(5, 2, 4)).getValue(TankBlock.FILL) == (17 - TankData.headroom()) / 2 /* half, rounded up */,
                "upper block half (top layer: 16 - headroom sixteenths when full)");
        double top = 1 - TankData.headroom() / 16.0;
        h.assertTrue(Math.abs(t.waterSurfaceY() - (h.absolutePos(new BlockPos(0, 2, 0)).getY() + 0.5 * top)) < 1e-6,
                "surface half way up the top layer: " + t.waterSurfaceY());
        h.assertTrue(t.fillOfLayer(h.absolutePos(new BlockPos(0, 2, 0)).getY()) == (17 - TankData.headroom()) / 2 /* half, rounded up */, "layer fill");
        // evaporation: both open tops reach the water (the full low part and the column)
        h.assertTrue(TankWater.openTop(h.getLevel(), t)[0] == 3, "three open tops: " + TankWater.openTop(h.getLevel(), t)[0]);
        h.setBlock(new BlockPos(4, 2, 4), ModBlocks.TANK_LID.get().defaultBlockState());
        h.assertTrue(TankWater.openTop(h.getLevel(), t)[0] == 2, "a lid on the low part covers it");
        // the feeder / lights / lids sit on every open top, also the lower step
        h.assertTrue(TankCare.aboveTop(t).contains(h.absolutePos(new BlockPos(4, 2, 4))), "open top of the low part");
        h.runAfterDelay(100, () -> {
            for (int x = 3; x <= 6; x++)
                for (int y = 1; y <= 3; y++)
                    for (int z = 3; z <= 6; z++) {
                        BlockPos p = new BlockPos(x, y, z);
                        if (step.contains(p) || p.equals(new BlockPos(4, 2, 4))) continue;
                        h.assertTrue(h.getBlockState(p).isAir(), "water leaked to " + p);
                    }
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ fish

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void fishStayInTheShape(GameTestHelper h) {
        List<BlockPos> l = bigL();
        add(h, l);
        TankBlockEntity c = controller(h, l.getFirst());
        TankInteractions.changeWater(h.getLevel(), c, 2600);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        for (int i = 0; i < 3; i++) {
            BettaGenome g = BettaGenome.randomDomestic(RandomSource.create(40 + i));
            ItemStack bucket = BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), BettaData.create(g, true, 0));
            EntityType<?> type = TankInhabitants.containedType(bucket);
            var err = TankInhabitants.insert(h.getLevel(), c, bucket, type, player, false);
            if (err != null) throw new GameTestAssertException("insert failed: " + err.getString());
        }
        ItemStack tropical = new ItemStack(Items.TROPICAL_FISH_BUCKET);
        var err = TankInhabitants.insert(h.getLevel(), c, tropical, EntityType.TROPICAL_FISH, player, false);
        if (err != null) throw new GameTestAssertException("insert failed: " + err.getString());
        TankData t = c.getTank();
        // the volume never hands out a point in the notch (swim targets, food, fry)
        TankWaterVolume w = new TankWaterVolume(t);
        RandomSource r = RandomSource.create(5);
        AABB box = TankCare.waterBox(t);
        for (int i = 0; i < 500; i++) {
            Vec3 v = new Vec3(box.minX - 1 + r.nextDouble() * (box.getXsize() + 2), box.minY - 1 + r.nextDouble() * 3,
                    box.minZ - 1 + r.nextDouble() * (box.getZsize() + 2));
            Vec3 cl = w.clamp(v, 0.3, 0.1, 0.3);
            if (!t.contains(BlockPos.containing(cl)) || !w.contains(cl)) throw new GameTestAssertException("clamped outside the shape: " + v + " -> " + cl);
        }
        Set<String> seen = new HashSet<>();
        long start = h.getTick();
        h.onEachTick(() -> {
            List<Entity> live = TankInhabitants.liveInhabitants(h.getLevel(), t);
            if (live.size() < 4) throw new GameTestAssertException("lost an inhabitant: " + live.size());
            for (Entity e : live) {
                if (!t.contains(e.blockPosition())) throw new GameTestAssertException(e.getType() + " left the shape at " + e.position());
                seen.add(e.blockPosition().toShortString());
                if (e instanceof BettaEntity b && !b.isInTank() && h.getTick() - start > 40) throw new GameTestAssertException("betta lost its tank");
            }
            if (h.getTick() - start >= 300) {
                h.assertTrue(seen.size() >= 2, "the fish moved around the L: " + seen);
                h.succeed();
            }
        });
    }

    // ------------------------------------------------------------------ rendering flags

    @GameTest(template = EMPTY)
    public static void concaveCornerGetsEdgeFlags(GameTestHelper h) {
        // small L: a (2,1,2), b (3,1,2), c (2,1,3); notch at (3,1,3)
        add(h, rel(new int[]{2, 1, 2}, new int[]{3, 1, 2}, new int[]{2, 1, 3}));
        ServerLevel level = h.getLevel();
        BlockPos b = h.absolutePos(new BlockPos(3, 1, 2)), cc = h.absolutePos(new BlockPos(2, 1, 3)), a = h.absolutePos(new BlockPos(2, 1, 2));
        long mb = TankBlock.concaveMask(level, b, level.getBlockState(b));
        long mc = TankBlock.concaveMask(level, cc, level.getBlockState(cc));
        long bit = 1L << (Direction.SOUTH.ordinal() * 6 + Direction.WEST.ordinal());
        h.assertTrue((mb & bit) != 0, "south face of b: crease towards west (inner corner)");
        h.assertTrue((mc & (1L << (Direction.EAST.ordinal() * 6 + Direction.NORTH.ordinal()))) != 0, "east face of c: crease towards north");
        h.assertTrue(Long.bitCount(mb) == 1 && Long.bitCount(mc) == 1, "only the vertical inner edge: " + Long.toBinaryString(mb) + " / " + Long.toBinaryString(mc));
        h.assertTrue(TankBlock.concaveMask(level, a, level.getBlockState(a)) == 0, "the outer corner has no crease");
        // a cuboid never has creases
        add(h, rel(new int[]{6, 1, 6}, new int[]{7, 1, 6}, new int[]{6, 1, 7}, new int[]{7, 1, 7}));
        for (BlockPos p : rel(new int[]{6, 1, 6}, new int[]{7, 1, 6}, new int[]{6, 1, 7}, new int[]{7, 1, 7})) {
            BlockPos ap = h.absolutePos(p);
            h.assertTrue(TankBlock.concaveMask(level, ap, level.getBlockState(ap)) == 0, "cuboid without crease at " + p);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ equipment

    @GameTest(template = EMPTY)
    public static void filterInTheNotchAttaches(GameTestHelper h) {
        List<BlockPos> l = bigL();
        add(h, l);
        TankBlockEntity c = controller(h, l.getFirst());
        // the notch at (4,1,4) faces the arm block (4,1,3) to the north
        h.setBlock(new BlockPos(4, 1, 4), ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, Direction.NORTH));
        TankEquipment.invalidate(c);
        h.assertTrue(TankEquipment.filterBlockCount(h.getLevel(), c) == 1, "filter on the notch wall is attached");
        // a light on the lower ... here: any open top
        h.setBlock(new BlockPos(5, 2, 3), ModBlocks.TANK_LIGHT.get().defaultBlockState());
        h.assertTrue(TankCare.lights(h.getLevel(), c.getTank())[0] == 1, "light on the arm counts");
        h.succeed();
    }

    // ------------------------------------------------------------------ interior

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void interiorOfAnLIsWalledAndDropsOnSplit(GameTestHelper h) {
        // L: a (2,1,2), b (3,1,2), c (2,1,3); notch (3,1,3)
        add(h, rel(new int[]{2, 1, 2}, new int[]{3, 1, 2}, new int[]{2, 1, 3}));
        TankBlockEntity ctl = controller(h, new BlockPos(2, 1, 2));
        interior(h);
        InteriorRegion r = InteriorManager.ensureRegion(h.getLevel(), ctl);
        ServerLevel in = interior(h);
        int s = r.scale();
        h.assertTrue(r.sizeX() == 2 * s && r.sizeZ() == 2 * s && !r.fullBox(), "region covers the box, shape is an L");
        h.assertTrue(r.hasCell(0, 0, 0) && r.hasCell(1, 0, 0) && r.hasCell(0, 0, 1) && !r.hasCell(1, 0, 1), "cells of the L");
        // walls close off the notch cell on its faces towards the L; no floor below it
        h.assertTrue(in.getBlockState(r.toWorld(s, 3, s)).is(ModBlocks.INTERIOR_WALL.get()), "wall on the notch face towards b");
        h.assertTrue(in.getBlockState(r.toWorld(s + 3, 3, s)).is(ModBlocks.INTERIOR_WALL.get()), "wall along the notch (north face)");
        h.assertTrue(in.getBlockState(r.toWorld(s, 3, s + 3)).is(ModBlocks.INTERIOR_WALL.get()), "wall along the notch (west face)");
        h.assertTrue(in.getBlockState(r.toWorld(s + 3, 3, s + 3)).isAir(), "inside the notch: unreachable air");
        h.assertTrue(in.getBlockState(r.toWorld(s + 3, -1, s + 3)).is(ModBlocks.INTERIOR_WALL.get()), "no floor under the notch");
        h.assertTrue(in.getBlockState(r.toWorld(3, -1, 3)).is(ModBlocks.INTERIOR_FLOOR.get()), "floor under the L");
        BlockPos entry = InteriorManager.regionAt(h.getLevel().getServer(), r.toWorld(1, 0, 1)) == r ? r.toWorld(1, 0, 1) : null;
        h.assertTrue(entry != null, "region lookup");
        // decoration in each arm
        in.setBlockAndUpdate(r.toWorld(2, 0, 2), Blocks.STONE.defaultBlockState()); // a
        in.setBlockAndUpdate(r.toWorld(s + 2, 0, 2), Blocks.COBBLESTONE.defaultBlockState()); // b
        in.setBlockAndUpdate(r.toWorld(2, 0, s + 2), Blocks.DIRT.defaultBlockState()); // c
        // breaking b: its part drops at b, the rest (a + c, now a cuboid) keeps its blocks in a new region
        BlockPos bAbs = h.absolutePos(new BlockPos(3, 1, 2));
        h.getLevel().setBlockAndUpdate(bAbs, Blocks.AIR.defaultBlockState());
        List<ItemEntity> drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(bAbs).inflate(2));
        h.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(Items.COBBLESTONE)), "b's interior part dropped");
        h.assertFalse(drops.stream().anyMatch(e -> e.getItem().is(Items.STONE) || e.getItem().is(Items.DIRT)), "the rest did not drop");
        TankBlockEntity rest = controller(h, new BlockPos(2, 1, 2));
        h.assertTrue(rest.getTank().memberCount() == 2 && rest.getTank().isCuboid(), "a + c remain");
        InteriorRegion nr = InteriorRegions.get(h.getLevel().getServer()).get(rest.getTank().regionId());
        h.assertTrue(nr != null && nr.built() && nr.fullBox(), "new full region for the rest");
        h.assertTrue(in.getBlockState(nr.toWorld(2, 0, 2)).is(Blocks.STONE) && in.getBlockState(nr.toWorld(2, 0, s + 2)).is(Blocks.DIRT),
                "decoration of a and c copied");
        h.assertTrue(InteriorRegions.get(h.getLevel().getServer()).get(r.id()) == null, "old region freed");
        h.succeed();
    }

    /**
     * An M9 cuboid tank whose interior region was saved before M10 (schema 2, no cells) grows into an L: the region is
     * migrated to a full box, the merge copies the decoration and the new region has the L's walls.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void m9CuboidTankGrowsIntoAnL(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        add(h, rel(new int[]{2, 1, 2}, new int[]{3, 1, 2}));
        TankBlockEntity c = controller(h, new BlockPos(2, 1, 2));
        TankInteractions.changeWater(level, c, 1500);
        interior(h);
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        ServerLevel in = interior(h);
        in.setBlockAndUpdate(r.toWorld(r.scale() + 1, 0, 1), Blocks.MOSSY_COBBLESTONE.defaultBlockState());
        // the region as M9 saved it: schema 2, no shape cells; the tank block entity as M9 saved it (schema 5)
        CompoundTag old = new CompoundTag();
        old.putInt("schema", 2);
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        CompoundTag reg = r.save();
        reg.remove("cells");
        list.add(reg);
        old.put("regions", list);
        CompoundTag migrated = org.overengineer.bettacraft.data.schema.Schemas.INTERIOR_REGIONS.migrate(old);
        InteriorRegion loaded = InteriorRegion.load(migrated.getListOrEmpty("regions").getCompoundOrEmpty(0));
        h.assertTrue(loaded.fullBox() && loaded.cellsX() == 2 && loaded.cellsZ() == 1, "pre-M10 region: full box of 2x1x1 cells");
        CompoundTag be = c.saveWithFullMetadata(level.registryAccess());
        h.assertTrue(be.getIntOr("schema", 0) == org.overengineer.bettacraft.data.schema.Schemas.TANK_BLOCK_ENTITY.currentVersion(), "tank block entity saved with the current schema (M10: 5, M11: 6)");
        c.loadWithComponents(be, level.registryAccess());
        h.assertTrue(c.getTank().memberCount() == 2 && c.getTank().waterMb() == 1500, "loaded M9 tank");
        // grow into an L
        add(h, new BlockPos(3, 1, 3));
        TankBlockEntity m = controller(h, new BlockPos(3, 1, 3));
        h.assertTrue(m == controller(h, new BlockPos(2, 1, 2)) && m.getTank().memberCount() == 3 && !m.getTank().isCuboid(), "L formed");
        h.assertTrue(m.getTank().waterMb() == 1500, "water kept");
        InteriorRegion nr = InteriorRegions.get(level.getServer()).get(m.getTank().regionId());
        h.assertTrue(nr != null && nr.built() && !nr.fullBox(), "merged region is an L");
        h.assertTrue(in.getBlockState(nr.toWorld(nr.scale() + 1, 0, 1)).is(Blocks.MOSSY_COBBLESTONE), "decoration copied into the L");
        h.assertTrue(in.getBlockState(nr.toWorld(0, 3, nr.scale())).is(ModBlocks.INTERIOR_WALL.get()), "the empty cell is walled off");
        h.succeed();
    }

    // ------------------------------------------------------------------ cuboid-only config

    @GameTest(template = EMPTY, batch = CUBOID_BATCH)
    public static void cuboidOnlyConfigKeepsTheOldRules(GameTestHelper h) {
        boolean before = ServerConfig.TANK_FREE_SHAPES.get();
        ServerConfig.TANK_FREE_SHAPES.set(false);
        try {
            BlockPos a = new BlockPos(2, 1, 2), b = new BlockPos(3, 1, 2), c = new BlockPos(2, 1, 3), d = new BlockPos(3, 1, 3);
            add(h, List.of(a, b, c));
            h.assertTrue(controller(h, c) != controller(h, a), "L-shape must not connect with free shapes off");
            add(h, d);
            TankBlockEntity ctl = controller(h, a);
            for (BlockPos p : List.of(a, b, c, d)) h.assertTrue(controller(h, p) == ctl, "merged 2x1x2");
            // breaking a corner splits the L remainder into cuboids
            h.getLevel().destroyBlock(h.absolutePos(a), false);
            h.assertTrue(controller(h, b) != controller(h, c), "remainder split into cuboids");
        } finally {
            ServerConfig.TANK_FREE_SHAPES.set(before);
        }
        h.succeed();
    }
}
