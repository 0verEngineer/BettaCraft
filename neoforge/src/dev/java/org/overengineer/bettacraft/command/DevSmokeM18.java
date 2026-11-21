package org.overengineer.bettacraft.command;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.plants.TankPlants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M18 part of the dedicated-server smoke test: a 3x2x2 tank with a light bar dyed red on the server and one betta of
 * every stage from fry to adult. The real care loop runs for the rest of the test; at the end the bar is still red and
 * lit, the plants get full light, and the stocking is 1 + 0.02 + 0.1 + 0.3 (young in fractions).
 */
final class DevSmokeM18 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private DevSmokeM18() {}

    private static BlockPos tankPos(BlockPos base) {
        return base.offset(0, 0, 52);
    }

    static void setup(ServerLevel level, BlockPos base) {
        BlockPos p = tankPos(base);
        AABB box = new AABB(p).expandTowards(3, 3, 2).inflate(2);
        for (Entity e : level.getEntities((Entity) null, box, e -> !(e instanceof net.minecraft.world.entity.player.Player))) e.discard();
        for (int x = 0; x < 3; x++)
            for (int y = 0; y < 3; y++)
                for (int z = 0; z < 2; z++) {
                    BlockPos q = p.offset(x, y, z);
                    if (!level.getBlockState(q).isAir()) level.setBlockAndUpdate(q, Blocks.AIR.defaultBlockState());
                }
        for (int x = 0; x < 3; x++)
            for (int y = 0; y < 2; y++)
                for (int z = 0; z < 2; z++) {
                    level.setBlockAndUpdate(p.offset(x, y, z), ModBlocks.TANK.get().defaultBlockState());
                    TankMultiblock.onTankBlockAdded(level, p.offset(x, y, z), null);
                }
        TankBlockEntity c = TankMultiblock.getController(level, p);
        if (c == null) throw new IllegalStateException("M18: tank did not form");
        TankInteractions.changeWater(level, c, c.getTank().capacityMb() - c.getTank().waterMb());
        c.getTank().setWaterTemp(27F);
        c.getTank().setTargetTemp(27F);
        for (int x = 0; x < 3; x++) {
            level.setBlockAndUpdate(p.offset(x, 2, 1), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 2));
        }
        BlockPos l = p.offset(1, 2, 1);
        int n = TankLightBlock.dyeBar(level, l, level.getBlockState(l), DyeColor.RED, false);
        if (n != 3) throw new IllegalStateException("M18: dyed " + n + " of 3 light segments");
        long seed = 1800;
        for (LifeStage s : new LifeStage[]{LifeStage.FRY, LifeStage.JUVENILE, LifeStage.SUBADULT, LifeStage.ADULT}) {
            BettaData d = BettaData.create(BettaGenotype.randomDomestic(RandomSource.create(seed++)), true, level.getGameTime()).withStage(s);
            Component err = TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d),
                    ModEntities.BETTA.get(), null, false);
            if (err != null) throw new IllegalStateException("M18: " + s.id() + " not inserted: " + err.getString());
        }
        LOGGER.info("[smoketest] M18 tank at {}: red light bar, a fry, a juvenile, a subadult and an adult", p);
        setupTime = level.getGameTime();
    }

    /**
     * M55: the animals of this run. The bettas of an earlier run in the saved dev world can load after the set-up (their
     * entities load later than the chunk on a busy machine) and doubled the stocking; the check sends them away first (born
     * before this run's set-up).
     */
    private static long setupTime;

    static void check(ServerLevel level, BlockPos base) {
        BlockPos p = tankPos(base);
        TankBlockEntity c = TankMultiblock.getController(level, p);
        if (c == null) throw new IllegalStateException("M18: tank gone");
        for (int x = 0; x < 3; x++) {
            BlockState s = level.getBlockState(p.offset(x, 2, 1));
            if (!s.is(ModBlocks.TANK_LIGHT.get()) || s.getValue(TankLightBlock.COLOR) != DyeColor.RED || !s.getValue(TankLightBlock.LIT)) {
                throw new IllegalStateException("M18: light segment " + x + " is " + s);
            }
        }
        AABB box = new AABB(p).expandTowards(3, 3, 2).inflate(2);
        for (Entity e : level.getEntities((Entity) null, box, e -> e instanceof org.overengineer.bettacraft.entity.betta.BettaEntity b
                && b.getBettaData().bornTime() < setupTime - 1)) {
            LOGGER.info("[smoketest] M18 an animal of an earlier run loaded late: {}", e);
            e.discard();
        }
        int[] lights = TankCare.lights(level, c.getTank());
        int light = TankPlants.light(level, c);
        double stock = TankInhabitants.stocking(level, c.getTank());
        LOGGER.info("[smoketest] M18 lights {}/{} lit, surface light {}, stocking {} (crowded {})", lights[1], lights[0], light, stock, c.isCrowded());
        if (lights[0] != 3 || lights[1] != 3) throw new IllegalStateException("M18: red lights not counted as lit");
        if (light != 15) throw new IllegalStateException("M18: plants under the red light get " + light + " instead of 15");
        if (Math.abs(stock - 1.42) > 0.01) throw new IllegalStateException("M18: stocking " + stock + " instead of 1.42");
    }
}
