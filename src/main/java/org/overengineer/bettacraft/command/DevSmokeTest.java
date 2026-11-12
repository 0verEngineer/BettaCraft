package org.overengineer.bettacraft.command;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorBench;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development-only smoke test, active only with {@code -Dbettacraft.smoketest=true} (gradlew runServer -Psmoketest):
 * builds a tank near spawn, fills it, creates its interior region, runs the interior benchmark and stops the server.
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class DevSmokeTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static final boolean ENABLED = Boolean.getBoolean("bettacraft.smoketest");
    private static int ticks;
    private static BlockPos base;

    private DevSmokeTest() {}

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (!ENABLED) return;
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        ticks++;
        try {
            if (ticks == 20) {
                base = level.getSharedSpawnPos().above(40);
                for (int x = 0; x < 3; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++)
                    level.setBlockAndUpdate(base.offset(x, y, z), ModBlocks.TANK.get().defaultBlockState());
            } else if (ticks == 30) {
                TankBlockEntity c = TankMultiblock.getController(level, base);
                if (c == null) throw new IllegalStateException("tank did not form");
                TankInteractions.changeWater(level, c, 9000);
                InteriorRegion r = InteriorManager.ensureRegion(level, c);
                LOGGER.info("[smoketest] tank {} formed, water ok, interior region {} built={}", c.getTank(), r.id(), r.built());
                InteriorBench.runDefault();
                InteriorBench.realRegions(server);
                // M1: bettas from buckets, a heater, a feeder with pellets
                c.getTank().heaters().setItem(0, new net.minecraft.world.item.ItemStack(org.overengineer.bettacraft.registry.ModItems.HEATER_SMALL.get()));
                for (int i = 0; i < 3; i++) {
                    var g = org.overengineer.bettacraft.genetics.BettaGenome.randomDomestic(level.getRandom());
                    var bucket = org.overengineer.bettacraft.entity.betta.BettaBucketItem.withFish(
                            new net.minecraft.world.item.ItemStack(org.overengineer.bettacraft.registry.ModItems.BETTA_BUCKET.get()),
                            org.overengineer.bettacraft.entity.betta.BettaData.create(g, i == 1, level.getGameTime()));
                    var err = org.overengineer.bettacraft.tank.TankInhabitants.insert(level, c, bucket,
                            org.overengineer.bettacraft.registry.ModEntities.BETTA.get(), null, false);
                    if (err != null) throw new IllegalStateException("insert failed: " + err.getString());
                }
                level.setBlockAndUpdate(base.above(2), ModBlocks.FEEDER.get().defaultBlockState());
                if (level.getBlockEntity(base.above(2)) instanceof org.overengineer.bettacraft.equipment.FeederBlockEntity f) {
                    f.items().insertItem(0, new net.minecraft.world.item.ItemStack(org.overengineer.bettacraft.registry.ModItems.BETTA_PELLETS.get(), 8), false);
                    f.feedNow(level, c);
                }
                level.setBlockAndUpdate(base.offset(1, 2, 0), ModBlocks.TANK_LIGHT.get().defaultBlockState());
            } else if (ticks == 200) {
                TankBlockEntity c = TankMultiblock.getController(level, base);
                var live = org.overengineer.bettacraft.tank.TankInhabitants.liveInhabitants(level, c.getTank());
                LOGGER.info("[smoketest] {} inhabitants alive, water {} C, crowded {}, log {}", live.size(),
                        c.getTank().waterTemp(), c.isCrowded(), c.getTank().log().size());
                if (live.size() < 3) throw new IllegalStateException("expected at least 3 bettas (more from earlier runs persist), found " + live.size());
                if (org.overengineer.bettacraft.tank.TankInhabitants.breakBlockedReason(level, base).isEmpty()) {
                    throw new IllegalStateException("break rule not active");
                }
                LOGGER.info("[smoketest] PASSED");
            } else if (ticks == 210) {
                server.halt(false);
            }
        } catch (RuntimeException e) {
            LOGGER.error("[smoketest] FAILED", e);
            server.halt(false);
        }
    }
}
