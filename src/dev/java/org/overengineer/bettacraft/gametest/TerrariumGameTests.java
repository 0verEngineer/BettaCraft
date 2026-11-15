package org.overengineer.bettacraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.terrarium.HeatMatBlock;
import org.overengineer.bettacraft.terrarium.LandWalker;
import org.overengineer.bettacraft.terrarium.MisterBlockEntity;
import org.overengineer.bettacraft.terrarium.MuskTurtleEntity;
import org.overengineer.bettacraft.terrarium.TankClimate;
import org.overengineer.bettacraft.terrarium.TerrariumCare;
import org.overengineer.bettacraft.terrarium.TerrariumMode;
import org.overengineer.bettacraft.terrarium.TerrariumNeeds;
import org.overengineer.bettacraft.terrarium.ThermoHygrometerBlock;

/**
 * M31 part a: terrarium and paludarium. The mode follows the water and the land of the interior; an aquarium stays as it
 * was (no climate, nothing new saved); each device moves its value (heat mat: ground and air with the target as
 * thermostat, basking lamp: basking spot and UV, mister: humidity and its water); the care hook assesses the musk turtle,
 * lets it in by the mode, harms it in cold water and feeds it; the land walker brings it out of the water onto the land
 * and to the basking spot. The climate steps are called directly within one tick (deterministic).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TerrariumGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private TerrariumGameTests() {}

    // ------------------------------------------------------------------ helpers

    private static ServerLevel interiorLevel(GameTestHelper h) {
        ServerLevel in = InteriorManager.interiorLevel(h.getLevel().getServer());
        if (in == null) throw new GameTestAssertException("no interior dimension");
        return in;
    }

    /** The interior with a sand floor. */
    private static InteriorRegion interior(GameTestHelper h, TankBlockEntity c) {
        InteriorRegion r = InteriorManager.ensureRegion(h.getLevel(), c);
        ServerLevel in = interiorLevel(h);
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) {
            Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.SAND.defaultBlockState(), false);
        }
        return r;
    }

    /** Land: interior columns x0..x1 (all z) filled with dirt up to the local height {@code top} (exclusive). */
    private static void land(GameTestHelper h, InteriorRegion r, int x0, int x1, int top) {
        ServerLevel in = interiorLevel(h);
        for (int x = x0; x <= x1; x++) for (int z = 0; z < r.sizeZ(); z++) for (int y = 0; y < top; y++) {
            in.setBlock(r.toWorld(x, y, z), Blocks.DIRT.defaultBlockState(), 3);
        }
    }

    /** A ramp: column x rises one block per column from {@code x0} (height 1) up to {@code top}, then flat. */
    private static void ramp(GameTestHelper h, InteriorRegion r, int x0, int top) {
        ServerLevel in = interiorLevel(h);
        for (int x = x0; x < r.sizeX(); x++) {
            int height = Math.min(top, x - x0 + 1);
            for (int z = 0; z < r.sizeZ(); z++) for (int y = 0; y < height; y++) in.setBlock(r.toWorld(x, y, z), Blocks.DIRT.defaultBlockState(), 3);
        }
    }

    private static void step(GameTestHelper h, TankBlockEntity c, double days, int times) {
        for (int i = 0; i < times; i++) TankClimate.step(h.getLevel(), c, days);
    }

    private static MuskTurtleEntity turtleIn(GameTestHelper h, TankBlockEntity c) {
        MuskTurtleEntity t = ModEntities.MUSK_TURTLE.get().create(h.getLevel());
        if (t == null) throw new GameTestAssertException("no turtle");
        ItemStack bucket = t.getBucketItemStack();
        t.saveToBucketTag(bucket);
        t.discard();
        var err = TankInhabitants.insert(h.getLevel(), c, bucket, ModEntities.MUSK_TURTLE.get(), null, 0L);
        if (err != null) throw new GameTestAssertException("turtle in: " + err.getString());
        for (Entity e : TankInhabitants.liveInhabitants(h.getLevel(), c.getTank())) if (e instanceof MuskTurtleEntity m) return m;
        throw new GameTestAssertException("no turtle in the tank");
    }

    // ------------------------------------------------------------------ modes

    /** No water = terrarium; full water = aquarium; land over half the floor and a third of water = paludarium. */
    @GameTest(template = EMPTY)
    public static void modeFollowsWaterAndLand(GameTestHelper h) {
        TankBlockEntity c = CritterGameTests.tank(h, 2, 2, 2, 3, 3, 3, 0);
        InteriorRegion r = interior(h, c);
        TankData t = c.getTank();
        step(h, c, 0.01, 1);
        h.assertTrue(t.terrariumMode() == TerrariumMode.TERRARIUM, "no water: terrarium, got " + t.terrariumMode());
        h.assertTrue(t.hasClimate(), "a terrarium has a climate");
        TankInteractions.changeWater(h.getLevel(), c, t.capacityMb());
        step(h, c, 0.01, 1);
        h.assertTrue(t.terrariumMode() == TerrariumMode.AQUARIUM, "full of water: aquarium, got " + t.terrariumMode());
        h.assertFalse(t.hasClimate(), "an aquarium has no climate");
        // land over the half x >= 24 up to 30 of 48 interior blocks, water up to one tank block (16 interior blocks)
        land(h, r, 24, r.sizeX() - 1, 30);
        TankInteractions.changeWater(h.getLevel(), c, 9000 - t.waterMb());
        step(h, c, 0.01, 1);
        h.assertTrue(Math.abs(t.landShare() - 0.5) < 0.05, "land share about a half, got " + t.landShare());
        h.assertTrue(t.terrariumMode() == TerrariumMode.PALUDARIUM, "paludarium, got " + t.terrariumMode());
        h.assertTrue(t.hasClimate(), "a paludarium has a climate");
        // the water is only a dish when the land covers almost all of the floor
        land(h, r, 2, 23, 30);
        TankInteractions.changeWater(h.getLevel(), c, 500 - t.waterMb());
        step(h, c, 0.01, 1);
        h.assertTrue(t.terrariumMode() == TerrariumMode.TERRARIUM, "land 96 %: terrarium with a water dish, got " + t.terrariumMode() + " land " + t.landShare());
        h.assertTrue(TankCare.waterLocalHeight(t, r.scale()) >= 0, "water height");
        h.succeed();
    }

    /** A fish tank of an old world: an aquarium without climate, nothing new in its save, its water values as before. */
    @GameTest(template = EMPTY)
    public static void oldFishTankStaysAsItWas(GameTestHelper h) {
        TankBlockEntity c = CritterGameTests.tank(h, 2, 2, 2, 3, 2, 2, 12000);
        interior(h, c);
        TankData t = c.getTank();
        float temp = t.waterTemp();
        step(h, c, 0.5, 4);
        h.assertTrue(t.terrariumMode() == TerrariumMode.AQUARIUM, "an aquarium, got " + t.terrariumMode());
        h.assertFalse(t.hasClimate(), "no climate values");
        h.assertFalse(t.save().contains("terrarium"), "nothing new is saved for an aquarium");
        h.assertFalse(t.saveClientView().contains("terrarium"), "nothing new is synced");
        h.assertTrue(t.waterTemp() == temp, "the climate step leaves the water temperature alone");
        // the climate tab stays hidden
        h.assertTrue(TankClimate.conditions(t).mode() == TerrariumMode.AQUARIUM, "conditions of an aquarium");
        h.succeed();
    }

    // ------------------------------------------------------------------ devices

    /** Heat mats under the floor warm the ground more than the air; the target temperature is their thermostat. */
    @GameTest(template = EMPTY)
    public static void heatMatsWarmTheGroundUpToTheTarget(GameTestHelper h) {
        TankBlockEntity c = CritterGameTests.tank(h, 2, 2, 2, 2, 2, 2, 0);
        interior(h, c);
        TankData t = c.getTank();
        for (int x = 2; x <= 3; x++) for (int z = 2; z <= 3; z++) h.setBlock(new BlockPos(x, 4, z), ModBlocks.TANK_LID.get().defaultBlockState());
        t.setTargetTemp(32F);
        step(h, c, 1.0, 2);
        float ambient = TankCare.ambient(h.getLevel(), t);
        h.assertTrue(Math.abs(t.airTemp() - ambient) < 0.3, "without devices the air is the room: " + t.airTemp() + " / " + ambient);
        for (int x = 2; x <= 3; x++) for (int z = 2; z <= 3; z++) {
            h.setBlock(new BlockPos(x, 1, z), ModBlocks.HEAT_MAT.get().defaultBlockState().setValue(HeatMatBlock.FACING, Direction.UP));
        }
        TankEquipment.invalidate(c);
        h.assertTrue(TankEquipment.heatMats(h.getLevel(), c).size() == 4, "four mats found, got " + TankEquipment.heatMats(h.getLevel(), c).size());
        step(h, c, 1.0, 3);
        h.assertTrue(t.airTemp() > ambient + 2, "the mats warm the air: " + t.airTemp() + " room " + ambient);
        h.assertTrue(t.groundTemp() > t.airTemp() + 2, "and the ground more: ground " + t.groundTemp() + " air " + t.airTemp());
        h.assertTrue(h.getBlockState(new BlockPos(2, 1, 2)).getValue(HeatMatBlock.HEATING), "a heating mat glows");
        float full = t.airTemp();
        // the thermostat: a target just above the room makes them rest most of the time
        t.setTargetTemp(Math.max(20F, ambient + 0.5F));
        step(h, c, 1.0, 3);
        if (t.targetTemp() < full - 0.5) h.assertTrue(t.airTemp() < full - 0.5, "a lower target, less warmth: " + t.airTemp() + " < " + full);
        h.assertTrue(t.airTemp() <= Math.max(t.targetTemp(), ambient) + 0.2, "held at the target: " + t.airTemp() + " target " + t.targetTemp());
        h.succeed();
    }

    /** A lit basking lamp makes a basking spot 12 K warmer than the air with UV 5; the thermo-hygrometer reads the humidity. */
    @GameTest(template = EMPTY)
    public static void baskingLampMakesASpotWithUv(GameTestHelper h) {
        TankBlockEntity c = CritterGameTests.tank(h, 2, 2, 2, 2, 2, 2, 0);
        interior(h, c);
        TankData t = c.getTank();
        for (int x = 2; x <= 3; x++) for (int z = 2; z <= 3; z++) if (x != 2 || z != 2) h.setBlock(new BlockPos(x, 4, z), ModBlocks.TANK_LID.get().defaultBlockState());
        step(h, c, 1.0, 2);
        h.assertTrue(t.uv() <= 3.01F, "no lamp: at most sunlight through the open top, got " + t.uv());
        h.assertTrue(Math.abs(t.baskTemp() - t.airTemp()) < 0.01, "no lamp: no basking spot");
        h.setBlock(new BlockPos(2, 4, 2), ModBlocks.BASKING_LAMP.get().defaultBlockState());
        step(h, c, 1.0, 2);
        h.assertTrue(Math.abs(t.uv() - 5F) < 0.01, "UV 5 at the spot, got " + t.uv());
        h.assertTrue(Math.abs(t.baskTemp() - t.airTemp() - 12F) < 0.05, "the spot is 12 K warmer: " + t.baskTemp() + " / " + t.airTemp());
        h.assertFalse(TankClimate.baskingSpots(h.getLevel(), t).isEmpty(), "a basking spot on the land under the lamp");
        // the display on the wall
        h.setBlock(new BlockPos(1, 2, 2), ModBlocks.THERMO_HYGROMETER.get().defaultBlockState().setValue(ThermoHygrometerBlock.FACING, Direction.WEST));
        var s = h.getBlockState(new BlockPos(1, 2, 2));
        h.assertTrue(ThermoHygrometerBlock.tank(h.getLevel(), h.absolutePos(new BlockPos(1, 2, 2)), s) == c, "the display finds its tank");
        int signal = s.getAnalogOutputSignal(h.getLevel(), h.absolutePos(new BlockPos(1, 2, 2)));
        h.assertTrue(signal == Math.round(t.humidity() * 15 / 100F), "comparator reads the humidity: " + signal + " for " + t.humidity());
        h.succeed();
    }

    /** A mister sprays while the humidity is below its setting, raises it and uses its water. */
    @GameTest(template = EMPTY)
    public static void misterRaisesTheHumidityAndUsesWater(GameTestHelper h) {
        TankBlockEntity c = CritterGameTests.tank(h, 2, 2, 2, 2, 2, 2, 0);
        interior(h, c);
        TankData t = c.getTank();
        // a lid on the other top blocks keeps the moisture in
        for (int x = 2; x <= 3; x++) for (int z = 2; z <= 3; z++) if (x != 2 || z != 2) h.setBlock(new BlockPos(x, 4, z), ModBlocks.TANK_LID.get().defaultBlockState());
        step(h, c, 1.0, 2);
        float dry = t.humidity();
        h.setBlock(new BlockPos(2, 4, 2), ModBlocks.MISTER.get().defaultBlockState());
        TankEquipment.invalidate(c);
        MisterBlockEntity m = (MisterBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(2, 4, 2)));
        if (m == null) throw new GameTestAssertException("no mister");
        m.water().fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
        m.setSetting(95);
        for (int i = 0; i < 20; i++) step(h, c, 0.005, 1);
        h.assertTrue(t.humidity() > dry + 10, "the mister made it humid: " + t.humidity() + " (dry " + dry + ")");
        h.assertTrue(m.amount() < 4000, "it used water: " + m.amount());
        // at a setting below the humidity it rests
        m.setSetting(40);
        int water = m.amount();
        long later = h.getLevel().getGameTime() + 100;
        h.assertFalse(m.wants(Math.max(41F, t.humidity()), later), "no spraying above the setting");
        step(h, c, 0.005, 1);
        h.assertTrue(t.humidity() < 42F || m.amount() == water, "it rests above its setting: " + m.amount() + " / " + water);
        h.succeed();
    }

    // ------------------------------------------------------------------ the care hook

    /** The musk turtle: into a paludarium only; cold water harms it, warm water is fine; it eats pellets. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void muskTurtleCareHook(GameTestHelper h) {
        // an aquarium refuses it
        TankBlockEntity fish = CritterGameTests.tank(h, 10, 2, 2, 2, 2, 2, 8000);
        interior(h, fish);
        MuskTurtleEntity probe = ModEntities.MUSK_TURTLE.get().create(h.getLevel());
        ItemStack bucket = probe.getBucketItemStack();
        probe.saveToBucketTag(bucket);
        probe.discard();
        var no = TankInhabitants.insert(h.getLevel(), fish, bucket, ModEntities.MUSK_TURTLE.get(), null, 0L);
        h.assertTrue(no != null, "an aquarium refuses the turtle");
        // a paludarium takes it, also without the fish's minimum water level
        TankBlockEntity c = CritterGameTests.tank(h, 2, 2, 2, 3, 3, 3, 0);
        InteriorRegion r = interior(h, c);
        land(h, r, 32, r.sizeX() - 1, 24);
        TankInteractions.changeWater(h.getLevel(), c, 9000);
        TankData t = c.getTank();
        t.setWaterTemp(24F);
        step(h, c, 0.01, 1);
        h.assertTrue(t.terrariumMode() == TerrariumMode.PALUDARIUM, "a paludarium, got " + t.terrariumMode() + " land " + t.landShare());
        MuskTurtleEntity turtle = turtleIn(h, c);
        TerrariumNeeds.Assessment fine = turtle.terrariumNeeds().assess(TankClimate.conditions(t));
        h.assertTrue(fine.fine(), "fine at 24 °C water: " + fine.problems());
        float hp = turtle.getHealth();
        t.setWaterTemp(12F);
        TerrariumNeeds.Assessment cold = turtle.terrariumNeeds().assess(TankClimate.conditions(t));
        h.assertTrue(cold.problems().contains(TerrariumNeeds.Problem.WATER_COLD) && cold.harmful(), "cold water: " + cold.problems());
        TerrariumCare.step(h.getLevel(), c, 1.5);
        h.assertTrue(turtle.getHealth() < hp, "a day and a half in cold water costs health: " + turtle.getHealth());
        h.assertTrue(turtle.stress() > 0, "and stresses it");
        // the tank's food reaches it
        turtle.setLastFedForTest(h.getLevel().getGameTime() - 48000);
        h.assertTrue(turtle.daysSinceFed() > 1.9, "hungry");
        int used = TankCare.feed(h.getLevel(), c, new ItemStack(ModItems.BETTA_PELLETS.get()), 1);
        h.assertTrue(used == 1 && turtle.daysSinceFed() < 0.01, "pellets feed the turtle: used " + used + ", days " + turtle.daysSinceFed());
        // a dry terrarium refuses it
        TankBlockEntity dry = CritterGameTests.tank(h, 20, 2, 2, 2, 2, 2, 0);
        interior(h, dry);
        h.assertTrue(TankInhabitants.insert(h.getLevel(), dry, bucket, ModEntities.MUSK_TURTLE.get(), null, 0L) != null, "a dry terrarium refuses it");
        h.succeed();
    }

    /** The land walker: the turtle starts in the water, walks up the ramp onto the land and lies on the basking spot. */
    @GameTest(template = EMPTY, timeoutTicks = 1600)
    public static void turtleClimbsOutAndBasks(GameTestHelper h) {
        TankBlockEntity c = CritterGameTests.tank(h, 2, 2, 2, 3, 2, 2, 0);
        InteriorRegion r = interior(h, c);
        // water up to 12 of 32 interior blocks, a ramp from x 16 up to 20 blocks (one block per column), land from x 27 on
        ramp(h, r, 16, 20);
        TankData t = c.getTank();
        TankInteractions.changeWater(h.getLevel(), c, Math.round(t.capacityMb() * 12 / 32.0));
        t.setWaterTemp(24F);
        h.setBlock(new BlockPos(4, 4, 2), ModBlocks.BASKING_LAMP.get().defaultBlockState());
        step(h, c, 0.01, 1);
        h.assertTrue(t.terrariumMode() == TerrariumMode.PALUDARIUM, "a paludarium, got " + t.terrariumMode() + " land " + t.landShare());
        MuskTurtleEntity turtle = turtleIn(h, c);
        // start at the bottom of the water at the far end
        double y0 = t.min().getY() + 0.05;
        turtle.moveTo(t.min().getX() + 0.4, y0, t.min().getZ() + 1.0, 0, 0);
        double surface = t.waterSurfaceY();
        h.assertTrue(turtle.getY() < surface, "starts in the water");
        turtle.walker().forceActivity(LandWalker.Activity.BASK);
        h.succeedWhen(() -> {
            h.assertTrue(turtle.isAlive(), "alive");
            h.assertTrue(turtle.getY() > surface, "out of the water: y " + turtle.getY() + " surface " + surface + " at " + turtle.position());
            h.assertTrue(turtle.walker().basking(), "lies on the basking spot (" + turtle.position() + ")");
            h.assertTrue(turtle.isBasking(), "synced to the client");
            Vec3 lamp = Vec3.atCenterOf(h.absolutePos(new BlockPos(4, 4, 2)));
            h.assertTrue(Math.abs(turtle.getX() - lamp.x) < 0.3 && Math.abs(turtle.getZ() - lamp.z) < 0.3, "under the lamp");
        });
    }
}
