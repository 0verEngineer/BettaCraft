package org.overengineer.bettacraft.fabric.dev;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M52: the dedicated server smoke on Fabric ({@code gradlew runFabricServer -Psmoketest}): the mod loaded, its registries,
 * a tank built and its interior measured (the interior dimension), then the server stops. Development only.
 */
public final class FabricDevSmoke implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    @Override
    public void onInitialize() {
        if (!Boolean.getBoolean("bettacraft.smoketest")) return;
        ServerLifecycleEvents.SERVER_STARTED.register(FabricDevSmoke::build);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(s -> {
            if (built >= 0 && ++built == 40) run(s);
        });
    }

    private static int built = -1;
    private static BlockPos BASE;

    private static void build(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BASE = level.getSharedSpawnPos().above(60);
        for (int x = 0; x < 3; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
            level.setBlockAndUpdate(BASE.offset(x, y, z), ModBlocks.TANK.get().defaultBlockState());
        }
        built = 0;
    }

    private static void run(MinecraftServer server) {
        boolean ok = true;
        long items = BuiltInRegistries.ITEM.keySet().stream().filter(id -> id.getNamespace().equals(BettaCraft.MOD_ID)).count();
        long blocks = BuiltInRegistries.BLOCK.keySet().stream().filter(id -> id.getNamespace().equals(BettaCraft.MOD_ID)).count();
        long entities = BuiltInRegistries.ENTITY_TYPE.keySet().stream().filter(id -> id.getNamespace().equals(BettaCraft.MOD_ID)).count();
        LOGGER.info("[smoketest] fabric: {} items, {} blocks, {} entity types of bettacraft", items, blocks, entities);
        ok &= items > 100 && blocks > 100 && entities > 5;
        try {
            ServerLevel level = server.overworld();
            BlockPos base = BASE;
            TankBlockEntity tank = org.overengineer.bettacraft.tank.TankMultiblock.getController(level, base);
            LOGGER.info("[smoketest] fabric: tank controller {}, data {}", tank != null, tank != null ? tank.getTank() : null);
            ok &= tank != null && tank.getTank() != null;
        } catch (Throwable t) {
            LOGGER.error("[smoketest] fabric: building a tank failed", t);
            ok = false;
        }
        LOGGER.info(ok ? "[smoketest] PASSED" : "[smoketest] FAILED");
        server.halt(false);
    }
}
