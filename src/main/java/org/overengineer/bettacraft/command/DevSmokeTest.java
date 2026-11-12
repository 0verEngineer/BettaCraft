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
import org.overengineer.bettacraft.tank.interior.InteriorWorldOps;
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
    private static boolean passed;
    private static int haltAt = -1;

    /** M3: decoration, substrate, a heater rod and a lily pad in the interior; delta sync of one change. */
    private static void interiorChecks(ServerLevel level, TankBlockEntity c, InteriorRegion r) {
        var server = level.getServer();
        ServerLevel in = InteriorManager.interiorLevel(server);
        // leftovers of an earlier run (the dev world persists)
        it.unimi.dsi.fastutil.longs.LongOpenHashSet touched = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        InteriorWorldOps.clear(in, r.originX(), InteriorRegion.BASE_Y, r.originZ(), r.originX() + r.sizeX() - 1,
                InteriorRegion.BASE_Y + r.sizeY() - 1, r.originZ() + r.sizeZ() - 1, touched);
        r.equipment().markNeedsScan();
        var err = org.overengineer.bettacraft.decor.DecorItem.place(in, org.overengineer.bettacraft.decor.DecorType.CASTLE, r.toWorld(12, 0, 12),
                net.minecraft.world.level.block.Rotation.NONE, null);
        if (err != null) throw new IllegalStateException("castle: " + err.getString());
        for (int x = 0; x < 8; x++)
            for (int z = 0; z < 8; z++)
                org.overengineer.bettacraft.tank.interior.Substrate.set(in, new BlockPos(r.originX() + 30 + x, InteriorRegion.FLOOR_Y, r.originZ() + 2 + z),
                        net.minecraft.world.level.block.Blocks.GRAVEL.defaultBlockState(), false);
        for (int y = 0; y < 6; y++) {
            in.setBlockAndUpdate(r.toWorld(40, y, 20), org.overengineer.bettacraft.registry.ModBlocks.HEATER_SEGMENT.get().defaultBlockState()
                    .setValue(org.overengineer.bettacraft.equipment.SegmentBlock.PART, y == 0 ? org.overengineer.bettacraft.equipment.SegmentBlock.Part.ANCHOR
                            : y == 5 ? org.overengineer.bettacraft.equipment.SegmentBlock.Part.TOP : org.overengineer.bettacraft.equipment.SegmentBlock.Part.MIDDLE));
        }
        in.setBlockAndUpdate(r.toWorld(20, 28, 8), net.minecraft.world.level.block.Blocks.LILY_PAD.defaultBlockState());
        var eq = InteriorManager.equipment(server, r);
        LOGGER.info("[smoketest] interior: heaters {}, floating plants {}, substrate {}, interior heat {} blocks", eq.heaters().size(),
                eq.floatingPlants().size(), eq.substrateCount(), org.overengineer.bettacraft.tank.TankCare.interiorHeaterBlocks(level, c.getTank()));
        if (eq.heaters().size() != 1 || eq.floatingPlants().size() != 1 || eq.substrateCount() != 64) throw new IllegalStateException("interior equipment");
        org.overengineer.bettacraft.tank.interior.InteriorSync.markAllDirty(r.id());
        int full = org.overengineer.bettacraft.tank.interior.InteriorSync.prepareFull(server, r.id());
        in.setBlockAndUpdate(r.toWorld(2, 0, 2), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        org.overengineer.bettacraft.tank.interior.InteriorSync.flush(server);
        LOGGER.info("[smoketest] interior sync: full {} bytes, one-block delta {} bytes", full,
                org.overengineer.bettacraft.tank.interior.InteriorSync.lastDeltaBytes);
    }

    private DevSmokeTest() {}

    /** M2: a pair in warm water runs through nest, spawning and fry (simulated in fast steps). */
    private static void breedingTank(ServerLevel level) {
        BlockPos b = base.offset(6, 0, 0);
        TankBlockEntity c = TankMultiblock.getController(level, b);
        if (c == null) throw new IllegalStateException("breeding tank did not form");
        // leftovers of an earlier run (the dev world persists)
        for (var e : org.overengineer.bettacraft.tank.TankInhabitants.liveInhabitants(level, c.getTank())) e.discard();
        c.getTank().inhabitants().clear();
        c.getTank().setNest(null);
        if (c.getTank().waterMb() < 12000) TankInteractions.changeWater(level, c, 12000 - c.getTank().waterMb());
        // M4: cabinets under the breeding tank, one of them with a filter and a large heater
        cabinetsUnder(level, c);
        c.getTank().setTargetTemp(27.0F);
        c.getTank().setWaterTemp(27.0F);
        var r = net.minecraft.util.RandomSource.create(5);
        for (int i = 0; i < 2; i++) {
            var d = org.overengineer.bettacraft.entity.betta.BettaData.create(org.overengineer.bettacraft.genetics.BettaGenotype.randomDomestic(r),
                    i == 1, level.getGameTime());
            var bucket = org.overengineer.bettacraft.entity.betta.BettaBucketItem.withFish(
                    new net.minecraft.world.item.ItemStack(org.overengineer.bettacraft.registry.ModItems.BETTA_BUCKET.get()), d);
            var err = org.overengineer.bettacraft.tank.TankInhabitants.insert(level, c, bucket,
                    org.overengineer.bettacraft.registry.ModEntities.BETTA.get(), null, false);
            if (err != null) LOGGER.warn("[smoketest] breeding insert: {}", err.getString());
        }
        int eggs = 0;
        for (int i = 0; i < 60 && (c.getTank().nest() == null || !c.getTank().nest().hasEggs()); i++) {
            org.overengineer.bettacraft.tank.TankBreeding.step(level, c, 0.05);
        }
        if (c.getTank().nest() == null || !c.getTank().nest().hasEggs()) throw new IllegalStateException("no eggs after 3 days");
        eggs = c.getTank().nest().eggCount();
        // take the female out, then let the larvae swim free
        for (var e : org.overengineer.bettacraft.tank.TankInhabitants.liveInhabitants(level, c.getTank())) {
            if (e instanceof org.overengineer.bettacraft.entity.betta.BettaEntity be && be.isFemale()) {
                c.getTank().inhabitants().remove(e.getUUID());
                e.discard();
            }
        }
        for (int i = 0; i < 40 && c.getTank().nest() != null; i++) org.overengineer.bettacraft.tank.TankBreeding.step(level, c, 0.05);
        long fry = org.overengineer.bettacraft.tank.TankInhabitants.liveInhabitants(level, c.getTank()).stream()
                .filter(e -> e instanceof org.overengineer.bettacraft.entity.betta.BettaEntity be && !be.isAdult()).count();
        LOGGER.info("[smoketest] breeding: {} eggs -> {} fry, warnings {}", eggs, fry, org.overengineer.bettacraft.tank.TankBreeding.warnings(level, c));
        if (fry <= 0) throw new IllegalStateException("no fry");
        for (var e : org.overengineer.bettacraft.tank.TankInhabitants.liveInhabitants(level, c.getTank())) e.discard();
        c.getTank().inhabitants().clear();
    }

    /** M4: a row of oak/spruce cabinets under the tank; the first one holds a filter with a large heater. */
    private static void cabinetsUnder(ServerLevel level, TankBlockEntity c) {
        var t = c.getTank();
        int y = t.min().getY() - 1;
        boolean first = true;
        for (int x = t.min().getX(); x <= t.max().getX(); x++)
            for (int z = t.min().getZ(); z <= t.max().getZ(); z++) {
                BlockPos p = new BlockPos(x, y, z);
                var state = ModBlocks.CABINET.get().defaultBlockState()
                        .setValue(org.overengineer.bettacraft.equipment.CabinetBlock.FACING, net.minecraft.core.Direction.NORTH);
                level.setBlockAndUpdate(p, state);
                if (level.getBlockEntity(p) instanceof org.overengineer.bettacraft.equipment.CabinetBlockEntity cab) {
                    cab.setWood(net.minecraft.resources.ResourceLocation.withDefaultNamespace(x % 2 == 0 ? "oak_planks" : "spruce_planks"));
                    if (first) {
                        var f = new net.minecraft.world.item.ItemStack(org.overengineer.bettacraft.registry.ModItems.EXTERNAL_FILTER.get());
                        org.overengineer.bettacraft.equipment.FilterHeaters.write(f, java.util.List.of(
                                new net.minecraft.world.item.ItemStack(org.overengineer.bettacraft.registry.ModItems.HEATER_LARGE.get()),
                                net.minecraft.world.item.ItemStack.EMPTY));
                        cab.items().setStackInSlot(org.overengineer.bettacraft.equipment.CabinetBlockEntity.FILTER_SLOT, f);
                        first = false;
                    }
                }
            }
        for (int x = t.min().getX(); x <= t.max().getX(); x++)
            for (int z = t.min().getZ(); z <= t.max().getZ(); z++) {
                BlockPos p = new BlockPos(x, y, z);
                level.setBlockAndUpdate(p, net.minecraft.world.level.block.Block.updateFromNeighbourShapes(level.getBlockState(p), level, p));
            }
        org.overengineer.bettacraft.tank.TankEquipment.invalidate(c);
    }

    /** M4: external filter west of the main tank with a small heater; reports what the migration moved. */
    private static void filterChecks(ServerLevel level, TankBlockEntity c) {
        int legacyBefore = 0;
        if (c.getTank().hasLegacyHeaters())
            for (int i = 0; i < 4; i++) if (!c.getTank().legacyHeaters().getItem(i).isEmpty()) legacyBefore++;
        BlockPos fp = base.offset(-1, 0, 0);
        level.setBlockAndUpdate(fp, ModBlocks.EXTERNAL_FILTER.get().defaultBlockState()
                .setValue(org.overengineer.bettacraft.equipment.ExternalFilterBlock.FACING, net.minecraft.core.Direction.EAST));
        var f = (org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity) level.getBlockEntity(fp);
        if (f == null) throw new IllegalStateException("no filter block entity");
        if (f.heaterCount() == 0) f.insertHeater(new net.minecraft.world.item.ItemStack(org.overengineer.bettacraft.registry.ModItems.HEATER_SMALL.get()));
        org.overengineer.bettacraft.tank.TankEquipment.invalidate(c);
        int moved = org.overengineer.bettacraft.tank.TankEquipment.moveLegacyHeaters(level, c);
        var filters = org.overengineer.bettacraft.tank.TankEquipment.filters(level, c);
        LOGGER.info("[smoketest] filter: {} attached, heating {} blocks through filters, {} old heater slot(s) before, {} moved",
                filters.size(), org.overengineer.bettacraft.tank.TankCare.filterHeaterBlocks(level, c.getTank()), legacyBefore, moved);
        if (filters.isEmpty() || org.overengineer.bettacraft.tank.TankCare.filterHeaterBlocks(level, c.getTank()) < 16) {
            throw new IllegalStateException("external filter not attached");
        }
    }

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
                InteriorBench.startRealRegions(server, line -> {});
                interiorChecks(level, c, r);
                // M1: bettas from buckets, a feeder with pellets; M4: the heater sits in an external filter
                filterChecks(level, c);
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
            } else if (ticks == 40) {
                BlockPos b = base.offset(6, 0, 0);
                for (int x = 0; x < 3; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++)
                    if (!level.getBlockState(b.offset(x, y, z)).is(ModBlocks.TANK.get()))
                        level.setBlockAndUpdate(b.offset(x, y, z), ModBlocks.TANK.get().defaultBlockState());
            } else if (ticks == 50) {
                breedingTank(level);
            } else if (!passed && ticks >= 200 && ticks < 2400 && (InteriorBench.realRunning() || ticks % 20 != 0)) {
                // wait for the asynchronous real-region bench
            } else if (ticks >= 200 && !passed) {
                if (InteriorBench.realRunning()) throw new IllegalStateException("real-region bench did not finish");
                passed = true;
                TankBlockEntity c = TankMultiblock.getController(level, base);
                var live = org.overengineer.bettacraft.tank.TankInhabitants.liveInhabitants(level, c.getTank());
                LOGGER.info("[smoketest] {} inhabitants alive, water {} C, crowded {}, log {}", live.size(),
                        c.getTank().waterTemp(), c.isCrowded(), c.getTank().log().size());
                // M5: fish from earlier runs were saved with older schemas; all of them load as v3 with a fin length
                java.util.Map<String, Integer> types = new java.util.TreeMap<>();
                int oldTails = 0;
                for (var e : live) {
                    if (!(e instanceof org.overengineer.bettacraft.entity.betta.BettaEntity b)) continue;
                    types.merge(org.overengineer.bettacraft.genetics.BettaTypes.name(b.genome()).getString(), 1, Integer::sum);
                    var fin = b.genotype().pair(org.overengineer.bettacraft.genetics.BettaLoci.FINFORM);
                    if ("plakat".equals(fin.a()) || "plakat".equals(fin.b())) oldTails++;
                }
                LOGGER.info("[smoketest] types in the tank: {}", types);
                if (oldTails > 0) throw new IllegalStateException(oldTails + " bettas still carry the old plakat finform allele");
                TankBlockEntity breed = TankMultiblock.getController(level, base.offset(6, 0, 0));
                int cabinets = org.overengineer.bettacraft.tank.TankEquipment.cabinets(level, breed).size();
                double cabHeat = org.overengineer.bettacraft.tank.TankCare.filterHeaterBlocks(level, breed.getTank());
                LOGGER.info("[smoketest] breeding tank: {} cabinets below, {} blocks of heating from the cabinet filter", cabinets, cabHeat);
                if (cabinets != 6 || cabHeat < 64) throw new IllegalStateException("cabinets under the breeding tank");
                if (live.size() < 3) throw new IllegalStateException("expected at least 3 bettas (more from earlier runs persist), found " + live.size());
                if (org.overengineer.bettacraft.tank.TankInhabitants.breakBlockedReason(level, base).isEmpty()) {
                    throw new IllegalStateException("break rule not active");
                }
                LOGGER.info("[smoketest] PASSED");
                haltAt = ticks + 10;
            } else if (passed && ticks == haltAt) {
                server.halt(false);
            }
        } catch (RuntimeException e) {
            LOGGER.error("[smoketest] FAILED", e);
            server.halt(false);
        }
    }
}
