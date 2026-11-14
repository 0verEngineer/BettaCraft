package org.overengineer.bettacraft.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.chem.TankChemistry;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.decor.DecorItem;
import org.overengineer.bettacraft.decor.DecorTemplate;
import org.overengineer.bettacraft.decor.DecorType;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.equipment.DeviceSegmentBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankAeration;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tank.interior.InteriorEquipment;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorPaths;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.tankmates.TankmateSpots;

/**
 * M28 part c GameTests: the new decorations place in a tank interior; the clay pipe of the ship's bow and the clay
 * tubes are spawning caves a pleco swims into; the treasure chest bubbles only with an air pump; leaves and alder cones
 * on the floor give off tannins and feed the shrimp, terracotta hides young shrimp; {@code features.decorEffects} off:
 * nothing (own batch, config restored).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class M28DecorGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private M28DecorGameTests() {}

    private static BlockPos at(InteriorRegion r, int x, int y, int z) {
        return new BlockPos(r.originX() + x, InteriorRegion.BASE_Y + y, r.originZ() + z);
    }

    private static List<DecorTemplate.Placed> place(GameTestHelper h, InteriorRegion r, DecorType t, int x, int z, Rotation rot) {
        ServerLevel in = AerationGameTests.interior(h);
        var err = DecorItem.place(in, t, at(r, x, 0, z), rot, null);
        if (err != null) throw new GameTestAssertException(t + ": " + err.getString());
        return DecorItem.template(in, t.structure()).orElseThrow().layout(at(r, x, 0, z), rot);
    }

    private static InteriorEquipment eq(GameTestHelper h, InteriorRegion r) {
        return InteriorManager.equipment(h.getLevel().getServer(), r);
    }

    /** Every new decoration places in a 2 x 2 x 2 tank (structure there, fits) with all its blocks. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void newDecorationsPlaceInATank(GameTestHelper h) {
        TankBlockEntity c = AerationGameTests.tank(h, 1, 1, 1, 2, 2, 2);
        InteriorRegion r = AerationGameTests.region(h, c);
        ServerLevel in = AerationGameTests.interior(h);
        for (DecorType t : DecorType.values()) {
            if (!t.hasHint()) continue;
            List<DecorTemplate.Placed> blocks = place(h, r, t, 16, 16, Rotation.CLOCKWISE_90);
            h.assertTrue(blocks.size() >= 30, t + ": only " + blocks.size() + " blocks");
            for (var b : blocks) {
                if (!in.getBlockState(b.pos()).is(b.state().getBlock())) throw new GameTestAssertException(t + ": missing " + b.state() + " at " + b.pos());
            }
            for (var b : blocks) in.setBlock(b.pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        h.succeed();
    }

    /** The clay tubes are two spawning caves; the bow of the ship has one in its hold (also turned). */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void clayTubesAndTheShipBowAreSpawningCaves(GameTestHelper h) {
        TankBlockEntity c = AerationGameTests.tank(h, 1, 1, 1, 3, 2, 3);
        InteriorRegion r = AerationGameTests.region(h, c);
        place(h, r, DecorType.CLAY_TUBES, 12, 12, Rotation.NONE);
        h.assertTrue(TankmateSpots.caves(h.getLevel().getServer(), c.getTank()).size() == 2, "two caves in the clay tubes");
        place(h, r, DecorType.SHIP_BOW, 32, 34, Rotation.CLOCKWISE_180);
        h.assertTrue(TankmateSpots.caves(h.getLevel().getServer(), c.getTank()).size() == 3, "and one in the ship's bow");
        h.succeed();
    }

    /** A pleco swims from the open water in front of the wreck through the broken hull into the clay pipe. */
    @GameTest(template = EMPTY, timeoutTicks = 1200)
    public static void plecoSwimsIntoTheCaveOfTheShipBow(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = CritterGameTests.tank(h, 2, 2, 2, 2, 2, 2, 8000);
        c.getTank().setWaterQuality(1F);
        InteriorRegion r = AerationGameTests.region(h, c);
        place(h, r, DecorType.SHIP_BOW, 13, 16, Rotation.NONE);
        var caves = TankmateSpots.caves(level.getServer(), c.getTank());
        h.assertTrue(caves.size() == 1, "one cave: " + caves.size());
        var spot = caves.getFirst();
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        Vec3 start = w.clamp(spot.pos().add(spot.dir().scale(0.6)).add(0, 0.2, 0), 0.3, 0.1, 0.2);
        TankmateEntity m = TankmateBreeding.spawn(level, c, TankmateSpecies.BRISTLENOSE_PLECO, TankmateGenes.wild(TankmateSpecies.BRISTLENOSE_PLECO),
                false, LifeStage.ADULT, 0, start);
        if (m == null) throw new GameTestAssertException("no pleco");
        m.health().feed();
        m.setCave(spot.pos(), spot.dir());
        h.assertTrue(InteriorPaths.swim(level.getServer(), c.getTank(), start, spot.pos(), w.surface()) != null, "a way into the cave");
        h.onEachTick(() -> {
            m.holdAt(spot.pos(), level.getGameTime() + 100);
            m.health().feed();
        });
        h.succeedWhen(() -> h.assertTrue(m.position().distanceTo(spot.pos()) < 0.15, "the pleco is not in the wreck's cave yet: " + m.position() + " -> " + spot.pos()));
    }

    /** The air stone among the coins of the treasure chest bubbles only while an air pump drives it. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void treasureChestBubblesOnlyWithAnAirPump(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = AerationGameTests.tank(h, 3, 1, 3, 2, 2, 2);
        InteriorRegion r = AerationGameTests.region(h, c);
        BlockPos stone = null;
        for (var b : place(h, r, DecorType.TREASURE_CHEST, 16, 16, Rotation.NONE)) if (b.state().is(ModBlocks.AIR_STONE.get())) stone = b.pos();
        if (stone == null) throw new GameTestAssertException("no air stone in the chest");
        h.assertTrue(eq(h, r).airStones().size() == 1, "the chest's air stone is an outlet of the tank");
        TankAeration.step(level, c, 0.1);
        ServerLevel in = AerationGameTests.interior(h);
        h.assertTrue(!in.getBlockState(stone).getValue(DeviceSegmentBlock.ACTIVE), "no pump: no bubbles");
        AerationGameTests.pump(h, c, new BlockPos(4, 2, 2));
        TankAeration.step(level, c, 0.1);
        h.assertTrue(in.getBlockState(stone).getValue(DeviceSegmentBlock.ACTIVE), "with the pump the chest bubbles");
        h.succeed();
    }

    private static void lay(GameTestHelper h, InteriorRegion r, ItemStack stack, int x, int z) {
        ServerLevel in = AerationGameTests.interior(h);
        BlockPos floor = at(r, x, -1, z);
        var ctx = new BlockPlaceContext(in, null, InteractionHand.MAIN_HAND, stack, new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false));
        stack.getItem().useOn(ctx);
    }

    /** Leaves and alder cones laid on the floor give off tannins and feed the shrimp; terracotta tubes hide young shrimp. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void leafLitterAndTubesWork(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = AerationGameTests.tank(h, 1, 1, 1, 2, 2, 2);
        InteriorRegion r = AerationGameTests.region(h, c);
        double food0 = TankCritters.foodPerDay(level, c.getTank()), hide0 = TankCritters.hiding(level, c.getTank());
        ItemStack leaves = new ItemStack(ModItems.ALMOND_LEAF.get(), 4), cones = new ItemStack(ModItems.ALDER_CONE.get(), 3);
        for (int i = 0; i < 4; i++) lay(h, r, leaves, 4 + 2 * i, 4);
        for (int i = 0; i < 3; i++) lay(h, r, cones, 4 + 2 * i, 8);
        h.assertTrue(leaves.isEmpty() && cones.isEmpty(), "used up: " + leaves + " " + cones);
        h.assertTrue(AerationGameTests.interior(h).getBlockState(at(r, 4, 0, 4)).is(ModBlocks.ALMOND_LEAF_LITTER.get()), "a leaf on the floor");
        InteriorEquipment eq = eq(h, r);
        h.assertTrue(eq.leafLitter() == 4 && eq.alderCones() == 3, "counted: " + eq.leafLitter() + " " + eq.alderCones());
        double perDay = TankChemistry.litterPerDay(level, c);
        h.assertTrue(perDay > 0, "the litter gives off tannins");
        c.getTank().setTannin(0F);
        for (int i = 0; i < 10; i++) TankChemistry.step(level, c, 1.0);
        h.assertTrue(c.getTank().tannin() > 0.01, "tannins in the water: " + c.getTank().tannin());
        h.assertTrue(TankCritters.foodPerDay(level, c.getTank()) > food0 + 0.2, "the shrimp graze on the litter");
        place(h, r, DecorType.SHRIMP_TUBES, 20, 20, Rotation.NONE);
        h.assertTrue(eq.shelter() >= 40, "terracotta counted: " + eq.shelter());
        h.assertTrue(TankCritters.hiding(level, c.getTank()) >= hide0 + 0.15, "young shrimp hide: " + hide0 + " -> " + TankCritters.hiding(level, c.getTank()));
        h.succeed();
    }

    /** features.decorEffects off: litter and tubes are only decoration. */
    @GameTest(template = EMPTY, batch = "bettacraft_m28_decor_off", timeoutTicks = 200)
    public static void decorEffectsOff(GameTestHelper h) {
        boolean saved = CommonConfig.FEATURE_DECOR_EFFECTS.get();
        try {
            CommonConfig.FEATURE_DECOR_EFFECTS.set(false);
            ServerLevel level = h.getLevel();
            TankBlockEntity c = AerationGameTests.tank(h, 1, 1, 1, 2, 2, 2);
            InteriorRegion r = AerationGameTests.region(h, c);
            double food0 = TankCritters.foodPerDay(level, c.getTank()), hide0 = TankCritters.hiding(level, c.getTank());
            lay(h, r, new ItemStack(ModItems.ALMOND_LEAF.get(), 1), 4, 4);
            place(h, r, DecorType.SHRIMP_TUBES, 20, 20, Rotation.NONE);
            h.assertTrue(eq(h, r).leafLitter() == 1, "the leaf lies there");
            h.assertTrue(TankChemistry.litterPerDay(level, c) == 0, "no tannins");
            h.assertTrue(TankCritters.foodPerDay(level, c.getTank()) == food0, "no food");
            h.assertTrue(TankCritters.hiding(level, c.getTank()) == hide0, "no shelter");
        } finally {
            CommonConfig.FEATURE_DECOR_EFFECTS.set(saved);
        }
        h.succeed();
    }
}
