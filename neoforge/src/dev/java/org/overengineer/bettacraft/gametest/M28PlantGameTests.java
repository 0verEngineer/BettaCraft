package org.overengineer.bettacraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.chem.WaterChemistry;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.plants.AquaticPlantBlock;
import org.overengineer.bettacraft.plants.InteriorPlant;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantColors;
import org.overengineer.bettacraft.plants.PlantPart;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;

/**
 * M28 part a: the new plants. Hygrophila pinnatifida roots on stone and wood (the other stem plants only in the
 * substrate), the demanding stem plants grow slowly without CO2 and in hard water, the white and pink Bucephalandras
 * turn white only under strong light (an LED panel or a high-power LED; the light bar gives one step less).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class M28PlantGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private M28PlantGameTests() {}

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

    private static void lights(GameTestHelper h, int x, int z, boolean led) {
        for (int a = 0; a < 2; a++) {
            h.setBlock(new BlockPos(x + a, 2, z), (led ? ModBlocks.HIGH_POWER_LED.get() : ModBlocks.TANK_LIGHT.get()).defaultBlockState()
                    .setValue(TankLightBlock.LIT, true));
        }
    }

    private static ServerLevel interior(GameTestHelper h) {
        ServerLevel l = InteriorManager.interiorLevel(h.getLevel().getServer());
        if (l == null) throw new GameTestAssertException("interior dimension not loaded");
        return l;
    }

    /** Hygrophila pinnatifida on a log and on stone; Rotala "H'ra" and the other stem plants only in the substrate. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void hygrophilaRootsOnStoneAndWood(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 2);
        lights(h, 2, 2, false);
        ServerLevel in = interior(h);
        InteriorRegion r = InteriorManager.ensureRegion(h.getLevel(), c);
        BlockPos log = r.toWorld(8, 0, 8), stone = r.toWorld(20, 0, 8);
        in.setBlock(log, Blocks.OAK_LOG.defaultBlockState(), 3);
        in.setBlock(stone, Blocks.STONE.defaultBlockState(), 3);
        h.assertTrue(InteriorPlants.supportProblem(PlantSpecies.HYGROPHILA_PINNATIFIDA, in, log, in.getBlockState(log), Direction.UP) == null,
                "Hygrophila on wood");
        h.assertTrue(InteriorPlants.supportProblem(PlantSpecies.HYGROPHILA_PINNATIFIDA, in, stone, in.getBlockState(stone), Direction.UP) == null,
                "Hygrophila on stone");
        h.assertTrue("needs_substrate".equals(InteriorPlants.supportProblem(PlantSpecies.HYGROPHILA_PINNATIFIDA, in, log, in.getBlockState(log),
                Direction.NORTH)), "not on the side of the log");
        for (PlantSpecies s : new PlantSpecies[]{PlantSpecies.ROTALA_HRA, PlantSpecies.LUDWIGIA_SUPER_RED, PlantSpecies.BACOPA_CAROLINIANA}) {
            h.assertTrue("needs_substrate".equals(InteriorPlants.supportProblem(s, in, log, in.getBlockState(log), Direction.UP)), s + " not on wood");
        }
        // planted on the log it grows into a plant of many blocks
        InteriorPlant p = InteriorPlants.plantAt(in, r, PlantSpecies.HYGROPHILA_PINNATIFIDA, 8, 1, 8, PlantSpecies.HYGROPHILA_PINNATIFIDA.stages(), 5L);
        int blocks = 0;
        for (int y = 1; y < 30; y++) {
            for (int dx = -3; dx <= 3; dx++)
                for (int dz = -3; dz <= 3; dz++) if (in.getBlockState(r.toWorld(8 + dx, y, 8 + dz)).getBlock() instanceof AquaticPlantBlock) blocks++;
        }
        h.assertTrue(p != null && blocks > 10, "grown on the log: " + blocks + " blocks");
        h.succeed();
    }

    /** Without CO2 the demanding stem plants grow at 40 %, with a good CO2 level at full speed; soft water plants slow down in hard water. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void demandingPlantsNeedCo2AndSoftWater(GameTestHelper h) {
        TankBlockEntity a = tank(h, 2, 2), b = tank(h, 2, 8);
        lights(h, 2, 2, true);
        lights(h, 2, 8, true);
        TankData ta = a.getTank(), tb = b.getTank();
        for (TankData t : new TankData[]{ta, tb}) {
            t.setPh(6.6F);
            t.setTds(150F);
        }
        ta.setCo2(WaterChemistry.CO2_AIR);
        tb.setCo2(30F);
        double without = InteriorPlants.needsFactor(ta, PlantSpecies.ROTALA_HRA), with = InteriorPlants.needsFactor(tb, PlantSpecies.ROTALA_HRA);
        h.assertTrue(Math.abs(without - PlantSpecies.NO_CO2_GROWTH) < 1e-6, "no CO2: 40 % " + without);
        h.assertTrue(Math.abs(with - 1) < 1e-6, "with CO2: full " + with);
        h.assertTrue(InteriorPlants.needsFactor(ta, PlantSpecies.LIMNOPHILA_SESSILIFLORA) == 1, "the undemanding ones do not care");
        // hard, alkaline water: the soft water plants slow down, the others not
        tb.setPh(8.2F);
        tb.setTds(800F);
        h.assertTrue(InteriorPlants.needsFactor(tb, PlantSpecies.ROTALA_WALLICHII) <= 0.26, "hard water: " + InteriorPlants.needsFactor(tb, PlantSpecies.ROTALA_WALLICHII));
        h.assertTrue(InteriorPlants.needsFactor(tb, PlantSpecies.ROTALA_HRA) == 1, "H'ra takes hard water");
        tb.setPh(6.6F);
        tb.setTds(150F);
        // the same plant in both tanks: the one with CO2 grows clearly faster
        ServerLevel in = interior(h);
        InteriorRegion ra = InteriorManager.ensureRegion(h.getLevel(), a), rb = InteriorManager.ensureRegion(h.getLevel(), b);
        InteriorPlant pa = InteriorPlants.plantAt(in, ra, PlantSpecies.ROTALA_HRA, 8, 0, 8, 1, 7L);
        InteriorPlant pb = InteriorPlants.plantAt(in, rb, PlantSpecies.ROTALA_HRA, 8, 0, 8, 1, 7L);
        InteriorPlants.grow(h.getLevel(), a, ra, 0.05);
        InteriorPlants.grow(h.getLevel(), b, rb, 0.05);
        double ga = pa.target + pa.progress - 1, gb = pb.target + pb.progress - 1;
        h.assertTrue(ga > 0 && gb > ga * 2, "with CO2 faster: " + gb + " vs " + ga);
        h.succeed();
    }

    /** "Snow White" stays greenish under the light bar and turns white under a high-power LED (M23 light tiers). */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void whiteBucephalandraNeedsStrongLight(GameTestHelper h) {
        TankBlockEntity bar = tank(h, 2, 2), led = tank(h, 2, 8);
        lights(h, 2, 2, false);
        lights(h, 2, 8, true);
        ServerLevel in = interior(h);
        InteriorRegion rb = InteriorManager.ensureRegion(h.getLevel(), bar), rl = InteriorManager.ensureRegion(h.getLevel(), led);
        BlockPos sb = rb.toWorld(8, 0, 8), sl = rl.toWorld(8, 0, 8);
        in.setBlock(sb, Blocks.STONE.defaultBlockState(), 3);
        in.setBlock(sl, Blocks.STONE.defaultBlockState(), 3);
        InteriorPlant pb = InteriorPlants.plantAt(in, rb, PlantSpecies.BUCE_SNOW_WHITE, 8, 1, 8, 6, 3L);
        InteriorPlant pl = InteriorPlants.plantAt(in, rl, PlantSpecies.BUCE_SNOW_WHITE, 8, 1, 8, 6, 3L);
        for (int i = 0; i < 12; i++) {
            InteriorPlants.grow(h.getLevel(), bar, rb, 0.5);
            InteriorPlants.grow(h.getLevel(), led, rl, 0.5);
        }
        h.assertTrue(pb.tint == 2, "light bar: half way, tint " + pb.tint);
        h.assertTrue(pl.tint == 3, "high-power LED: white, tint " + pl.tint);
        int green = PlantColors.color(PlantSpecies.BUCE_SNOW_WHITE, PlantPart.LEAF, 0), white = PlantColors.color(PlantSpecies.BUCE_SNOW_WHITE, PlantPart.LEAF, 3);
        h.assertTrue(((green >> 8) & 0xFF) > ((green >> 16) & 0xFF) && (white & 0xFF) > 0xE0, "greenish, then white: "
                + Integer.toHexString(green) + " " + Integer.toHexString(white));
        h.succeed();
    }
}
