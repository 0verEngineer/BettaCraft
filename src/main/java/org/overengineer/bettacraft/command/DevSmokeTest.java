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
                LOGGER.info("[smoketest] PASSED");
            } else if (ticks == 40) {
                server.halt(false);
            }
        } catch (RuntimeException e) {
            LOGGER.error("[smoketest] FAILED", e);
            server.halt(false);
        }
    }
}
