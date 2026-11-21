package org.overengineer.bettacraft.fabric.dev;

import net.fabricmc.api.ModInitializer;
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
        // M52: the showcase world (with the rows of the save/load test) and the save/load check, shared with NeoForge
        if (Boolean.getBoolean("bettacraft.showcase")) {
            org.overengineer.bettacraft.command.DevShowcase.extraRows = org.overengineer.bettacraft.command.FabricUpgradeReference::extras;
            org.overengineer.bettacraft.command.DevShowcase.afterSave = org.overengineer.bettacraft.command.FabricUpgradeReference::manifest;
            net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(org.overengineer.bettacraft.command.DevShowcase::onTick);
        }
        if (Integer.getInteger("bettacraft.smoketest.upgrade", 0) > 0) {
            net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(org.overengineer.bettacraft.command.DevSmokeUpgrade::onTick);
        }
        if (!Boolean.getBoolean("bettacraft.smoketest")) return;
        // M52b: built once the server ticks (like the NeoForge smoke), checked 40 ticks later
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(s -> {
            if (++ticks == 20) build(s);
            if (built >= 0 && ++built == 40) run(s);
        });
    }

    private static int built = -1, ticks;
    private static BlockPos BASE;

    private static void build(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BASE = level.getRespawnData().pos().above(60);
        // M52b: 1.21.9+ keeps no spawn chunks loaded: the smoke area is forced (as in the NeoForge smoke)
        for (int cx = (BASE.getX() - 2) >> 4; cx <= (BASE.getX() + 5) >> 4; cx++) for (int cz = (BASE.getZ() - 2) >> 4; cz <= (BASE.getZ() + 4) >> 4; cz++) {
            level.setChunkForced(cx, cz, true);
        }
        for (int x = 0; x < 3; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
            level.setBlockAndUpdate(BASE.offset(x, y, z), ModBlocks.TANK.get().defaultBlockState());
        }
        // M52.5: the tank formed at once, as a player's placing does (in a fresh world the forced chunks do not tick yet, and
        // the blocks' scheduled tick that forms a tank put there without a player came too late: CI on 26.x)
        for (int x = 0; x < 3; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
            org.overengineer.bettacraft.tank.TankMultiblock.onTankBlockAdded(level, BASE.offset(x, y, z), null);
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
