package org.overengineer.bettacraft.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.plants.AquaticPlantBlock;
import org.overengineer.bettacraft.plants.InteriorPlant;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.plants.FloatingPlant;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tank.plants.TankPlants;

/**
 * M28 part b: Riccia is tied onto stone or wood or floats on the surface (one item for both); the Marimo ball rolls a block
 * when there is room and keeps its stage, never in still water; Hemianthus "Cuba" needs CO2.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class M28bPlantGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private M28bPlantGameTests() {}

    private static TankBlockEntity tank(GameTestHelper h, int x, int z) {
        for (int a = 0; a < 2; a++) {
            BlockPos p = new BlockPos(x + a, 1, z);
            h.setBlock(p, ModBlocks.TANK.get().defaultBlockState());
            TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(p), null);
        }
        TankBlockEntity c = TankMultiblock.getController(h.getLevel(), h.absolutePos(new BlockPos(x, 1, z)));
        if (c == null) throw new GameTestAssertException("tank did not form");
        TankInteractions.changeWater(h.getLevel(), c, 2000);
        c.getTank().setWaterTemp(26F);
        c.getTank().setTargetTemp(26F);
        return c;
    }

    private static ServerLevel interior(GameTestHelper h) {
        ServerLevel l = InteriorManager.interiorLevel(h.getLevel().getServer());
        if (l == null) throw new GameTestAssertException("interior dimension not loaded");
        return l;
    }

    /** Riccia: on stone and wood only (not on the floor, unlike the other mosses); the same item floats on the surface. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void ricciaTiedOnHardscapeOrFloating(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 2);
        ServerLevel in = interior(h);
        InteriorRegion r = InteriorManager.ensureRegion(h.getLevel(), c);
        BlockPos stone = r.toWorld(8, 0, 8), floor = r.toWorld(20, -1, 8);
        in.setBlock(stone, Blocks.STONE.defaultBlockState(), 3);
        h.assertTrue(InteriorPlants.supportProblem(PlantSpecies.RICCIA, in, stone, in.getBlockState(stone), Direction.UP) == null, "Riccia on stone");
        h.assertTrue("needs_hardscape".equals(InteriorPlants.supportProblem(PlantSpecies.RICCIA, in, floor, in.getBlockState(floor), Direction.UP)),
                "Riccia not on the floor: " + InteriorPlants.supportProblem(PlantSpecies.RICCIA, in, floor, in.getBlockState(floor), Direction.UP));
        h.assertTrue(InteriorPlants.supportProblem(PlantSpecies.CHRISTMAS_MOSS, in, floor, in.getBlockState(floor), Direction.UP) == null,
                "Christmas moss on the floor");
        // the plant item is also the floating plant, and a floating Riccia comes back as that item
        ItemStack item = InteriorPlants.item(PlantSpecies.RICCIA, 0);
        h.assertTrue(TankPlants.typeOf(item) == FloatingPlantType.RICCIA, "the Riccia item floats");
        h.assertTrue(TankPlants.typeOf(InteriorPlants.item(PlantSpecies.CHRISTMAS_MOSS, 0)) == null, "other plants do not");
        FloatingPlant f = TankPlants.add(c, FloatingPlantType.RICCIA, c.getTank().min().getX() + 1.0, c.getTank().min().getZ() + 0.5, 1F, 0F);
        h.assertTrue(f != null, "Riccia on the surface");
        List<ItemStack> back = TankPlants.items(List.of(f));
        h.assertTrue(back.size() == 1 && back.getFirst().is(item.getItem()), "taken out it is the plant item: " + back);
        h.assertTrue(TankPlants.item(FloatingPlantType.WATER_LETTUCE).getItem() != TankPlants.item(FloatingPlantType.HORNWORT).getItem()
                && !TankPlants.item(FloatingPlantType.HORNWORT).isEmpty(), "water lettuce and hornwort have items");
        h.succeed();
    }

    private static int plantBlocks(ServerLevel in, InteriorRegion r, int cx, int cz) {
        int n = 0;
        for (int y = 0; y < 14; y++)
            for (int dx = -7; dx <= 7; dx++)
                for (int dz = -7; dz <= 7; dz++) if (in.getBlockState(r.toWorld(cx + dx, y, cz + dz)).getBlock() instanceof AquaticPlantBlock) n++;
        return n;
    }

    /** A grown Marimo rolls one block to the side with its whole ball, not into a stone, and never in still water. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void marimoRollsAndStaysWhole(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 2);
        ServerLevel in = interior(h);
        InteriorRegion r = InteriorManager.ensureRegion(h.getLevel(), c);
        InteriorPlant p = InteriorPlants.plantAt(in, r, PlantSpecies.MARIMO, 12, 0, 8, PlantSpecies.MARIMO.stages(), 4L);
        h.assertTrue(p != null && p.stage == PlantSpecies.MARIMO.stages(), "a grown Marimo");
        int before = plantBlocks(in, r, 12, 8);
        h.assertTrue(before > 100, "a ball of many blocks: " + before);
        h.assertTrue(InteriorPlants.roll(in, r, p, 1, 0), "rolls to the east");
        h.assertTrue(p.x == 13 && p.z == 8 && p.stage == PlantSpecies.MARIMO.stages(), "moved, still grown: " + p);
        h.assertTrue(in.getBlockState(r.toWorld(13, 0, 8)).getBlock() instanceof AquaticPlantBlock, "the root is at the new place");
        int after = plantBlocks(in, r, 12, 8);
        h.assertTrue(Math.abs(after - before) <= 2, "the whole ball moved: " + before + " -> " + after);
        h.assertTrue(r.plants().all().contains(p) && r.plants().size() == 1, "still one plant");
        // a stone in the way: it stays
        in.setBlock(r.toWorld(12, 0, 8), Blocks.STONE.defaultBlockState(), 3);
        h.assertTrue(!InteriorPlants.roll(in, r, p, -1, 0), "not into the stone");
        h.assertTrue(p.x == 13, "stayed");
        // still water (no filter, no outlet): days of care and it does not move
        InteriorPlants.grow(h.getLevel(), c, r, 5.0);
        h.assertTrue(p.x == 13 && p.z == 8, "no current, no rolling: " + p);
        h.succeed();
    }

    /** Hemianthus "Cuba" grows at 40 % without CO2 (it is one of the demanding plants). */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void cubaNeedsCo2(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 2);
        c.getTank().setCo2(org.overengineer.bettacraft.chem.WaterChemistry.CO2_AIR);
        double f = InteriorPlants.needsFactor(c.getTank(), PlantSpecies.HC_CUBA);
        h.assertTrue(Math.abs(f - PlantSpecies.NO_CO2_GROWTH) < 1e-6, "no CO2: " + f);
        c.getTank().setCo2(30F);
        h.assertTrue(Math.abs(InteriorPlants.needsFactor(c.getTank(), PlantSpecies.HC_CUBA) - 1) < 1e-6, "with CO2 full speed");
        h.succeed();
    }
}
