package org.overengineer.bettacraft.command;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.overengineer.bettacraft.mobs.GlassMobs;
import org.overengineer.bettacraft.registry.ModAttachments;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The cross-loader load check ({@code gradlew fabricCrossLoadTest} / {@code smoketestCrossLoad}, active with
 * {@code -Dbettacraft.smoketest.crossload=true}): the server runs on a copy of a world saved by the OTHER loader (a showcase
 * built on NeoForge for the Fabric run and the other way round). Every tank of the overworld is loaded; every animal its tank
 * lists must still carry the tank's {@code tank_ref} (and a mob in glass its {@code glass_mob}) right after loading, before
 * any care step could adopt or forget it. Logs "CROSSLOAD PASSED" or "CROSSLOAD FAILED", then the server stops.
 */
public final class DevSmokeCrossLoad {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    public static final boolean ENABLED = Boolean.getBoolean("bettacraft.smoketest.crossload");
    private static int ticks, haltAt = -1;
    private static final List<ChunkPos> chunks = new ArrayList<>();

    private DevSmokeCrossLoad() {}

    public static void onTick(MinecraftServer server) {
        if (!ENABLED) return;
        ServerLevel level = server.overworld();
        ticks++;
        if (haltAt >= 0) {
            if (ticks == haltAt) server.halt(false); // a few ticks later, like the upgrade smoke (halting in the check's tick hung the save)
            return;
        }
        if (ticks == 5) {
            for (InteriorRegion r : InteriorRegions.get(server).all()) {
                if (r.tankDimension() != Level.OVERWORLD || r.tankMin() == null) continue;
                ChunkPos cp = new ChunkPos(r.tankMin().getX() >> 4, r.tankMin().getZ() >> 4);
                if (!chunks.contains(cp)) {
                    chunks.add(cp);
                    level.setChunkForced(cp.x(), cp.z(), true);
                }
            }
            LOGGER.info("[smoketest] crossload: {} chunks with tanks", chunks.size());
            return;
        }
        if (ticks < 6) return;
        boolean ready = true;
        for (ChunkPos cp : chunks) {
            level.getChunk(cp.x(), cp.z());
            ready &= level.areEntitiesLoaded(cp.pack());
        }
        if (!ready && ticks < 1200) return;
        List<String> lost = new ArrayList<>();
        int tanks = 0, animals = 0, herps = 0, glass = 0;
        for (InteriorRegion r : InteriorRegions.get(server).all()) {
            if (r.tankDimension() != Level.OVERWORLD || r.tankMin() == null) continue;
            TankBlockEntity c = TankMultiblock.getController(level, r.tankMin());
            if (c == null) continue;
            tanks++;
            for (UUID u : c.getTank().inhabitants()) {
                Entity e = level.getEntity(u);
                if (e == null) continue;
                animals++;
                String what = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()) + " " + u + " in " + c.getBlockPos();
                if (e instanceof org.overengineer.bettacraft.terrarium.TerrariumAnimal) herps++;
                if (!TankInhabitants.hasTankRef(e) || !c.getTank().id().equals(ModAttachments.TANK_REF.get(e).tankId())) lost.add(what + ": no tank_ref");
                if (GlassMobs.isAllowed(e.getType())) {
                    glass++;
                    if (!ModAttachments.GLASS_MOB.has(e)) lost.add(what + ": no glass_mob");
                }
            }
        }
        for (String l : lost) LOGGER.error("[smoketest] crossload: {}", l);
        boolean ok = ready && lost.isEmpty() && animals > 0 && herps > 0;
        LOGGER.info("[smoketest] CROSSLOAD {}: {} tanks, {} animals checked ({} terrarium animals, {} mobs in glass), {} lost{}", ok ? "PASSED" : "FAILED",
                tanks, animals, herps, glass, lost.size(), ready ? "" : ", entities not loaded");
        haltAt = ticks + 10;
    }
}
