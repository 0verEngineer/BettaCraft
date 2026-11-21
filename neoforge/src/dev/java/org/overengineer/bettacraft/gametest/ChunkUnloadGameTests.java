package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M42 part b: a stocked tank whose chunk unloads and loads again, several times, also right after a care step and with the
 * chunk back one tick later (the entities come back after the block entity, from their own storage): the tank data goes
 * through the chunk file, the animals through the entity storage. Afterwards the same animals with the same genes live in
 * it, none twice, the small animals and the water are kept, and the tank still works. Built 3000 blocks away from the test
 * (chunks of the GameTest structures are forced and never unload), its chunk forced and released by the test.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ChunkUnloadGameTests {
    private ChunkUnloadGameTests() {}

    /** What must survive an unload: the animals by id with their genes, the small animals, the water. */
    record Snapshot(Map<UUID, String> animals, int critters, long water, Set<UUID> listed) {}

    static Snapshot snapshot(ServerLevel level, TankData t) {
        Map<UUID, String> animals = new HashMap<>();
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) {
            String genes = e instanceof BettaEntity b ? b.getBettaData().genotype().toString() : e.getType().toShortString();
            animals.put(e.getUUID(), genes);
        }
        return new Snapshot(animals, TankCritters.alive(t), t.waterMb(), new TreeSet<>(t.inhabitants()));
    }

    private static TankBlockEntity build(GameTestHelper h, BlockPos min) {
        ServerLevel level = h.getLevel();
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < 2; y++) for (int z = 0; z < 3; z++) for (int x = 0; x < 4; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlock(pos, ModBlocks.TANK.get().defaultBlockState(), 3);
        TankMultiblock.formAll(level, blocks);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        if (c == null) throw new GameTestAssertException("no tank");
        c.getTank().setFront(Direction.NORTH);
        TankInteractions.changeWater(level, c, c.getTank().capacityMb());
        c.getTank().setWaterTemp(26F);
        c.getTank().setTargetTemp(26F);
        c.getTank().setWaterQuality(1F);
        return c;
    }

    private static void stock(GameTestHelper h, TankBlockEntity c) {
        LiveFoodGameTests.betta(h, c, false, 42_11L);
        LiveFoodGameTests.betta(h, c, true, 42_12L);
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        var b = w.box();
        for (int i = 0; i < 4; i++) {
            Vec3 p = w.clamp(new Vec3(b.minX + 0.7 + i * 0.7, b.minY + 0.5, (b.minZ + b.maxZ) / 2), 0.35, 0.1, 0.2);
            if (TankmateBreeding.spawn(h.getLevel(), c, TankmateSpecies.NEON_TETRA, TankmateGenes.wild(TankmateSpecies.NEON_TETRA), i % 2 == 0,
                    LifeStage.ADULT, 0, p) == null) throw new GameTestAssertException("no tetra");
        }
        for (int i = 0; i < 5; i++) TankCritters.add(h.getLevel(), c, i < 3 ? CritterSpecies.NEOCARIDINA : CritterSpecies.ZEBRA_NERITE, CritterGenes.WILD, i % 2 == 0, 10);
    }

    private static void force(ServerLevel level, BlockPos min, boolean on) {
        ChunkPos a = new ChunkPos(min), b = new ChunkPos(min.offset(3, 0, 2));
        for (int x = a.x; x <= b.x; x++) for (int z = a.z; z <= b.z; z++) level.setChunkForced(x, z, on);
    }

    /** Waits (tick by tick, at most {@code max}) until {@code done}, then runs {@code then}; fails with {@code what} after that. */
    private static void await(GameTestHelper h, int max, java.util.function.BooleanSupplier done, String what, Runnable then) {
        if (done.getAsBoolean()) {
            then.run();
            return;
        }
        if (max <= 0) throw new GameTestAssertException("timed out: " + what);
        h.runAfterDelay(1, () -> await(h, max - 1, done, what, then));
    }

    /**
     * Three unloads: one after a while, one in the tick right after a care step (the controller's care tick), and one that comes back
     * one tick after it unloaded.
     */
    @GameTest(template = "empty_32x10x32", batch = "bettacraft_m42_chunk_unload", timeoutTicks = 6000)
    public static void aStockedTankSurvivesItsChunkUnloadingAndLoading(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos min = h.absolutePos(new BlockPos(2, 2, 2)).offset(3000, 0, 0);
        force(level, min, true);
        // M52e: 1000 ticks for the far chunks: with the biome mods of the integration suite generating them can take 20+ s;
        // M52.5: 2400, a CI runner with the power mods needed more than 50 s
        await(h, 2400, () -> level.isPositionEntityTicking(min) && level.isPositionEntityTicking(min.offset(3, 0, 2)), "the far chunks load", () -> {
            TankBlockEntity c = build(h, min);
            stock(h, c);
            Snapshot first = snapshot(level, c.getTank());
            if (first.animals().size() != 6 || first.critters() != 5) throw new GameTestAssertException("stocked: " + first);
            cycle(h, min, first, 0, () -> {
                force(level, min, false);
                h.succeed();
            });
        });
    }

    private static void cycle(GameTestHelper h, BlockPos min, Snapshot expect, int round, Runnable done) {
        ServerLevel level = h.getLevel();
        if (round >= 3) {
            done.run();
            return;
        }
        ChunkPos cp = new ChunkPos(min);
        // round 1: release the chunk in the tick right after the controller's care step
        int wait = round == 1 ? (int) Math.floorMod(-(level.getGameTime() + min.asLong() + 1), 20) + 1 : 40;
        h.runAfterDelay(Math.max(1, wait), () -> {
            TankBlockEntity c = TankMultiblock.getController(level, min);
            if (c == null) throw new GameTestAssertException("round " + round + ": no tank before the unload");
            Snapshot before = snapshot(level, c.getTank());
            check(round + " (before)", expect, before);
            force(level, min, false);
            await(h, 1200, () -> !level.hasChunk(cp.x, cp.z) && level.getEntity(expect.animals().keySet().iterator().next()) == null,
                    "round " + round + ": the chunk did not unload", () -> {
                        force(level, min, true);
                        int back = round == 2 ? 1 : 10;
                        h.runAfterDelay(back, () -> await(h, 600, () -> {
                            TankBlockEntity cc = TankMultiblock.getController(level, min);
                            return cc != null && expect.animals().keySet().stream().allMatch(u -> level.getEntity(u) != null);
                        }, "round " + round + ": the animals did not come back " + what(level, min, expect), () -> {
                            // a few care steps with everything loaded again (the inhabitant list reconciles every few seconds)
                            h.runAfterDelay(120, () -> {
                                TankBlockEntity cc = TankMultiblock.getController(level, min);
                                Snapshot after = snapshot(level, cc.getTank());
                                check(round + " (after)", expect, after);
                                long twice = level.getEntities((Entity) null, TankInhabitants.tankBox(cc.getTank()).inflate(2),
                                        e -> expect.animals().containsKey(e.getUUID())).size();
                                if (twice != expect.animals().size()) throw new GameTestAssertException("round " + round + ": " + twice + " animals for " + expect.animals().size());
                                cycle(h, min, expect, round + 1, done);
                            });
                        }));
                    });
        });
    }

    private static String what(ServerLevel level, BlockPos min, Snapshot expect) {
        TankBlockEntity c = TankMultiblock.getController(level, min);
        long found = expect.animals().keySet().stream().filter(u -> level.getEntity(u) != null).count();
        return "(" + found + " of " + expect.animals().size() + " loaded, tank " + (c == null ? "missing" : "there") + ")";
    }

    private static void check(String what, Snapshot expect, Snapshot now) {
        if (!now.animals().equals(expect.animals())) throw new GameTestAssertException("round " + what + ": animals " + now.animals().size() + " " + now.animals().keySet()
                + " expected " + expect.animals().keySet());
        if (now.critters() != expect.critters()) throw new GameTestAssertException("round " + what + ": small animals " + now.critters() + " of " + expect.critters());
        if (!now.listed().equals(expect.listed())) throw new GameTestAssertException("round " + what + ": inhabitant list " + now.listed() + " expected " + expect.listed());
        if (Math.abs(now.water() - expect.water()) > 1000) throw new GameTestAssertException("round " + what + ": water " + now.water() + " of " + expect.water());
    }
}
