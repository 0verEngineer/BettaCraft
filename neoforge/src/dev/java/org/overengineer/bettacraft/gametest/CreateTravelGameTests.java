package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import org.overengineer.bettacraft.util.NbtCompat;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity;
import org.overengineer.bettacraft.registry.ModAttachments;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TravelCare;
import org.overengineer.bettacraft.tank.interior.InteriorManager;

/**
 * M50, dev group {@code -Pcreate}: the care on the road ({@code features.compat.create.travelCare}).
 * <ul>
 *     <li>Sable airship: the bettas stay real entities in the tank in the sub-level's plot, tick there, eat, the water
 *     values change; put down a quarter turned, they are in the turned tank exactly once, adopted by it.</li>
 *     <li>Create bearing: two tanks with an external filter and heater glued to them turn on bearings, one with a drive
 *     adapter: the adapter turns with the trip and powers filter and heater, the other tank's devices have no power; the
 *     packed bettas get hungry; put down, the trip is in the log and the bettas come out exactly once.</li>
 *     <li>Saved and loaded in the middle of the trip (the contraption entity unloaded and made again from its data): the
 *     care goes on from where it was, nothing doubled or lost on the landing.</li>
 *     <li>Switch off: the data on the contraption stays as it was (the care pauses, M32).</li>
 * </ul>
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CreateTravelGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private CreateTravelGameTests() {}

    /** Runs {@code r} when the test ends, passed or failed (config restored after an asynchronous test). */
    static void atEnd(GameTestHelper h, Runnable r) {
        h.testInfo.addListener(new GameTestListener() {
            @Override
            public void testStructureLoaded(GameTestInfo info) {}

            @Override
            public void testPassed(GameTestInfo info, GameTestRunner runner) {
                r.run();
            }

            @Override
            public void testFailed(GameTestInfo info, GameTestRunner runner) {
                r.run();
            }

            @Override
            public void testAddedForRerun(GameTestInfo a, GameTestInfo b, GameTestRunner runner) {}
        });
    }

    private static boolean sable() {
        return org.overengineer.bettacraft.platform.Platform.get().isModLoaded("sable");
    }

    // ------------------------------------------------------------------ Sable

    @GameTest(template = EMPTY, batch = "bettacraft_create", timeoutTicks = 400)
    public static void airshipTankLivesOn(GameTestHelper h) throws Exception {
        if (!sable()) {
            h.succeed();
            return;
        }
        ServerLevel level = h.getLevel();
        TankBlockEntity c = CreateGameTests.stockedTank(h, 12, 3, 22, 2, 1, 3);
        UUID tankId = c.getTank().id();
        long water = c.getTank().waterMb();
        int regionId = InteriorManager.ensureRegion(level, c).id();
        List<UUID> bettas = CreateGameTests.animals(h, c);
        h.assertTrue(bettas.size() == 2, "two bettas");
        BlockPos anchor = c.getBlockPos();
        Class<?> helper = Class.forName("dev.ryanhcode.sable.api.SubLevelAssemblyHelper");
        Class<?> predicate = Class.forName("dev.ryanhcode.sable.api.SubLevelAssemblyHelper$FrontierPredicate");
        Object onlyTanks = Proxy.newProxyInstance(predicate.getClassLoader(), new Class<?>[]{predicate}, (p, m, a) -> {
            if (m.getName().equals("isValidConnection")) return ((BlockState) a[3]).getBlock() instanceof TankBlock;
            return m.getName().equals("hashCode") ? 0 : m.getName().equals("equals") ? p == a[0] : null;
        });
        Object gathered = CreateGameTests.method(helper, "gatherConnectedBlocks", 4).invoke(null, anchor, level, 64, onlyTanks);
        Object blocks = gathered.getClass().getMethod("blocks").invoke(gathered);
        Object bounds = gathered.getClass().getMethod("boundingBox").invoke(gathered);
        Object subLevel = CreateGameTests.method(helper, "assembleBlocks", 4).invoke(null, level, anchor, blocks, bounds);
        Object plot = subLevel.getClass().getMethod("getPlot").invoke(subLevel);
        BlockPos plotAnchor = (BlockPos) plot.getClass().getMethod("getCenterBlock").invoke(plot);
        int[] ticks = new int[2];
        h.runAfterDelay(5, () -> {
            TankBlockEntity up = TankMultiblock.getController(level, plotAnchor);
            h.assertTrue(up != null && up.getTank().id().equals(tankId), "the tank is on the airship: " + up);
            h.assertTrue(up.getTank().regionId() == regionId && up.getTank().waterMb() == water, "with its region and water");
            h.assertTrue(up.stowed().isEmpty(), "nothing packed: the bettas swim on: " + up.stowed().size());
            for (UUID u : bettas) {
                Entity e = level.getEntity(u);
                h.assertTrue(e != null && e.isAlive(), "the betta lives on the airship");
                h.assertTrue(up.getTank().contains(e.blockPosition()), "in the tank in the plot: " + e.blockPosition());
                h.assertTrue(ModAttachments.TANK_REF.get(e).controller().equals(up.getBlockPos()), "adopted by the travelling tank");
            }
            ticks[0] = level.getEntity(bettas.getFirst()).tickCount;
            // feeding, the water values
            h.assertTrue(TankCare.feed(level, up, new ItemStack(ModItems.BETTA_PELLETS.get()), 2) >= 1, "the bettas eat on the airship");
            for (UUID u : bettas) {
                if (level.getEntity(u) instanceof BettaEntity b) h.assertTrue(b.health().daysSinceFed() < 0.01, "fed");
            }
            up.getTank().setWaterTemp(18F);
            TankCare.care(level, up, 2400);
            h.assertTrue(up.getTank().waterTemp() > 18.05F, "the water warms up on the airship: " + up.getTank().waterTemp());
            float ambient = TankCare.ambient(level, up.getTank());
            h.assertTrue(ambient > 5F && ambient < 40F, "the air of the place where the ship is: " + ambient);
            h.runAfterDelay(30, () -> {
                ticks[1] = level.getEntity(bettas.getFirst()).tickCount;
                h.assertTrue(ticks[1] > ticks[0], "the betta ticks on the airship: " + ticks[0] + " -> " + ticks[1]);
                try {
                    Class<?> sim = Class.forName("dev.simulated_team.simulated.util.SimAssemblyHelper");
                    Method dis = null;
                    for (Method m : sim.getMethods()) if (m.getName().equals("disassembleSubLevel")) dis = m;
                    if (dis == null) throw new GameTestFailure("no disassembleSubLevel");
                    dis.invoke(null, level, subLevel, plotAnchor, anchor.above(2), Rotation.CLOCKWISE_90, false);
                } catch (ReflectiveOperationException e) {
                    throw new GameTestFailure("disassembly failed: " + e);
                }
                h.succeedWhen(() -> {
                    TankBlockEntity n = CreateGameTests.tankNear(level, anchor.above(2), tankId, 3);
                    h.assertTrue(n != null, "the tank is back on the ground");
                    h.assertTrue(n.getTank().memberCount() == 6 && n.getTank().sizeX() == 3, "turned: " + n.getTank());
                    h.assertTrue(n.getTank().waterMb() > water / 2 && n.getTank().waterMb() <= water, "the water (some evaporated on the trip): " + n.getTank().waterMb());
                    h.assertTrue(n.stowed().isEmpty(), "nothing packed");
                    for (UUID u : bettas) {
                        h.assertTrue(CreateGameTests.alive(level, u) == 1, "the betta is back exactly once");
                        Entity e = level.getEntity(u);
                        h.assertTrue(n.getTank().contains(e.blockPosition()), "in the tank on the ground: " + e.blockPosition());
                        h.assertTrue(ModAttachments.TANK_REF.get(e).controller().equals(n.getBlockPos()), "adopted by the landed tank");
                    }
                    h.assertTrue(TankInhabitants.liveInhabitants(level, n.getTank()).size() == 2, "two bettas in the landed tank");
                });
            });
        });
    }

    // ------------------------------------------------------------------ Create contraptions

    private static Object glue(GameTestHelper h, BlockPos from, BlockPos to) throws Exception {
        Class<?> c = Class.forName("com.simibubi.create.content.contraptions.glue.SuperGlueEntity");
        Entity e = (Entity) c.getConstructor(Level.class, AABB.class).newInstance(h.getLevel(),
                AABB.encapsulatingFullBlocks(h.absolutePos(from), h.absolutePos(to)));
        h.getLevel().addFreshEntity(e);
        return e;
    }

    /** A stocked 2x1x2 tank on a bearing (at rel x, z), an external filter with a heater glued to its west side. */
    private static TankBlockEntity tankOnBearing(GameTestHelper h, int x, int z, boolean adapter) throws Exception {
        TankBlockEntity c = CreateGameTests.stockedTank(h, x, 2, z, 2, 1, 2);
        c.getTank().setPowerBuffer(0);
        c.getTank().setTargetTemp(30F);
        h.setBlock(new BlockPos(x - 1, 2, z), ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, Direction.EAST));
        if (!(h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(x - 1, 2, z))) instanceof ExternalFilterBlockEntity f)) throw new GameTestFailure("no filter");
        f.insertHeater(new ItemStack(ModItems.HEATER_SMALL.get()));
        if (adapter) {
            var b = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(BettaCraft.id("drive_adapter"));
            h.setBlock(new BlockPos(x + 2, 2, z), CreateDeviceGameTests.with(b.defaultBlockState(), "facing", Direction.EAST));
        }
        glue(h, new BlockPos(x - 1, 2, z), new BlockPos(x + 2, 2, z + 1));
        return c;
    }

    /** The data of the controller on the contraption (its block in the contraption's blocks). */
    private static CompoundTag controllerData(Entity contraption, UUID tank) {
        try {
            Object c = CreateGameTests.method(contraption.getClass(), "getContraption", 0).invoke(contraption);
            @SuppressWarnings("unchecked")
            Map<BlockPos, Object> blocks = (Map<BlockPos, Object>) CreateGameTests.method(c.getClass(), "getBlocks", 0).invoke(c);
            for (Object info : blocks.values()) {
                CompoundTag nbt = (CompoundTag) info.getClass().getMethod("nbt").invoke(info);
                if (nbt != null && NbtCompat.has(nbt, "tank", Tag.TAG_COMPOUND) && TankData.load(nbt.getCompoundOrEmpty("tank")).id().equals(tank)) return nbt;
            }
        } catch (ReflectiveOperationException e) {
            throw new GameTestFailure("contraption data: " + e);
        }
        throw new GameTestFailure("no controller on the contraption");
    }

    private static double sinceFed(CompoundTag nbt) {
        ListTag st = nbt.getListOrEmpty("stowed");
        double min = Double.MAX_VALUE;
        for (int i = 0; i < st.size(); i++) min = Math.min(min, st.getCompoundOrEmpty(i).getCompoundOrEmpty(BettaData.KEY).getCompoundOrEmpty("health").getDoubleOr("days_since_fed", 0d));
        return st.isEmpty() ? -1 : min;
    }

    private static void turn(Entity contraption, float angle) {
        try {
            CreateGameTests.method(contraption.getClass(), "setAngle", 1).invoke(contraption, angle);
        } catch (ReflectiveOperationException e) {
            throw new GameTestFailure("setAngle: " + e);
        }
    }

    @GameTest(template = EMPTY, batch = "bettacraft_create_travel", timeoutTicks = 500)
    public static void contraptionTripRunsOnItsPower(GameTestHelper h) throws Exception {
        if (!CreateGameTests.create()) {
            h.succeed();
            return;
        }
        boolean heater = ServerConfig.POWER_HEATER.get(), filter = ServerConfig.POWER_FILTER.get(), power = Features.own(Feature.POWER),
                drive = Features.own(Feature.CREATE_DRIVE);
        double speed = ServerConfig.TRAVEL_DYNAMO_SPEED.get();
        atEnd(h, () -> {
            ServerConfig.POWER_HEATER.set(heater);
            ServerConfig.POWER_FILTER.set(filter);
            Features.set(Feature.POWER, power);
            Features.set(Feature.CREATE_DRIVE, drive);
            ServerConfig.TRAVEL_DYNAMO_SPEED.set(speed);
        });
        ServerConfig.POWER_HEATER.set(true);
        ServerConfig.POWER_FILTER.set(true);
        Features.set(Feature.POWER, true);
        Features.set(Feature.CREATE_DRIVE, true);
        ServerConfig.TRAVEL_DYNAMO_SPEED.set(0.5);
        ServerLevel level = h.getLevel();
        BlockEntity ba = CreateGameTests.bearing(h, new BlockPos(5, 1, 5)), bb = CreateGameTests.bearing(h, new BlockPos(5, 1, 18));
        TankBlockEntity a = tankOnBearing(h, 5, 5, true), b = tankOnBearing(h, 5, 18, false);
        UUID ida = a.getTank().id(), idb = b.getTank().id();
        List<UUID> bettas = CreateGameTests.animals(h, a);
        BlockPos anchorA = a.getBlockPos();
        Entity ca = CreateGameTests.assemble(ba), cb = CreateGameTests.assemble(bb);
        h.assertTrue(!(level.getBlockState(h.absolutePos(new BlockPos(4, 2, 5))).getBlock() instanceof ExternalFilterBlock), "the glued filter travels along");
        float[] angle = {0};
        h.onEachTick(() -> {
            CreateGameTests.hold(ba);
            CreateGameTests.hold(bb);
            angle[0] += 3F;
            turn(ca, angle[0]);
            turn(cb, angle[0]);
        });
        h.runAfterDelay(150, () -> {
            CompoundTag da = controllerData(ca, ida), db = controllerData(cb, idb);
            CompoundTag ta = da.getCompoundOrEmpty(TravelCare.TRIP_KEY), tb = db.getCompoundOrEmpty(TravelCare.TRIP_KEY);
            h.assertTrue(ta.getLongOr("ticks", 0L) >= 100 && tb.getLongOr("ticks", 0L) >= 100, "the trip is simulated: " + ta + " / " + tb);
            h.assertTrue(ta.getLongOr("dynamo", 0L) > 0 && ta.getLongOr("powered", 0L) > 0, "the adapter turns with the trip and powers the devices: " + ta);
            h.assertTrue(tb.getLongOr("unpowered", 0L) > 0 && tb.getLongOr("powered", 0L) == 0, "without a source the filter and heater stand: " + tb);
            h.assertTrue(sinceFed(da) > 0.003 && sinceFed(db) > 0.003, "the packed bettas get hungry: " + sinceFed(da));
            TankData tda = TankData.load(da.getCompoundOrEmpty("tank")), tdb = TankData.load(db.getCompoundOrEmpty("tank"));
            h.assertTrue(tda.waterTemp() > tdb.waterTemp(), "the powered heater warms: " + tda.waterTemp() + " / " + tdb.waterTemp());
            for (UUID u : bettas) h.assertTrue(CreateGameTests.alive(level, u) == 0, "packed, not in the world");
            try {
                turn(ca, 0F);
                CreateGameTests.disassemble(ba, ca, BlockPos.ZERO, 0F);
                turn(cb, 0F);
                CreateGameTests.disassemble(bb, cb, BlockPos.ZERO, 0F);
            } catch (Exception e) {
                throw new GameTestFailure("disassembly failed: " + e);
            }
            h.succeedWhen(() -> {
                TankBlockEntity n = CreateGameTests.tankNear(level, anchorA, ida, 2);
                h.assertTrue(n != null, "tank a stands again");
                for (UUID u : bettas) {
                    h.assertTrue(CreateGameTests.alive(level, u) == 1, "the betta is back exactly once");
                    h.assertTrue(n.getTank().contains(level.getEntity(u).blockPosition()), "in the tank");
                }
                h.assertTrue(n.getTank().log().stream().anyMatch(l -> l.key().equals("log.bettacraft.travel.arrived")), "the trip is in the log");
                h.assertTrue(n.getTank().powerBuffer() >= 0 && n.stowed().isEmpty(), "nothing left packed");
            });
        });
    }

    @GameTest(template = EMPTY, batch = "bettacraft_create", timeoutTicks = 400)
    public static void contraptionSavedMidTripLosesNothing(GameTestHelper h) throws Exception {
        if (!CreateGameTests.create()) {
            h.succeed();
            return;
        }
        ServerLevel level = h.getLevel();
        BlockEntity bearing = CreateGameTests.bearing(h, new BlockPos(20, 1, 6));
        TankBlockEntity c = CreateGameTests.stockedTank(h, 20, 2, 6, 2, 1, 2);
        UUID id = c.getTank().id();
        int critters = c.getTank().critters().size();
        List<UUID> bettas = CreateGameTests.animals(h, c);
        BlockPos anchor = c.getBlockPos();
        Entity first = CreateGameTests.assemble(bearing);
        Entity[] now = {first};
        h.onEachTick(() -> CreateGameTests.hold(bearing));
        long[] before = new long[1];
        h.runAfterDelay(50, () -> {
            CompoundTag d = controllerData(now[0], id);
            before[0] = d.getCompoundOrEmpty(TravelCare.TRIP_KEY).getLongOr("ticks", 0L);
            h.assertTrue(before[0] > 0, "the care ran before the save: " + d.getCompoundOrEmpty(TravelCare.TRIP_KEY));
            // the chunk is saved and unloaded with the contraption, later loaded again
            CompoundTag saved = new CompoundTag();
            h.assertTrue(now[0].saveAsPassenger(saved), "the contraption saves");
            now[0].remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            Entity again = EntityType.loadEntityRecursive(saved, level, net.minecraft.world.entity.EntitySpawnReason.LOAD, e -> e);
            h.assertTrue(again != null && level.addFreshEntity(again), "the contraption comes back from its data");
            now[0] = again;
            h.runAfterDelay(50, () -> {
                CompoundTag d2 = controllerData(now[0], id);
                long after = d2.getCompoundOrEmpty(TravelCare.TRIP_KEY).getLongOr("ticks", 0L);
                h.assertTrue(after > before[0], "the care goes on after loading: " + before[0] + " -> " + after);
                h.assertTrue(d2.getListOrEmpty("stowed").size() == 2, "both bettas still packed");
                for (UUID u : bettas) h.assertTrue(CreateGameTests.alive(level, u) == 0, "no betta in the world");
                try {
                    Object moved = CreateGameTests.field(bearing.getClass(), "movedContraption").get(bearing);
                    if (moved == now[0]) {
                        CreateGameTests.disassemble(bearing, now[0], new BlockPos(0, 2, 0), 0F);
                    } else {
                        now[0].setPos(now[0].getX(), now[0].getY() + 2, now[0].getZ());
                        CreateGameTests.method(now[0].getClass(), "disassemble", 0).invoke(now[0]);
                    }
                } catch (Exception e) {
                    throw new GameTestFailure("disassembly failed: " + e);
                }
                h.succeedWhen(() -> {
                    TankBlockEntity n = CreateGameTests.tankNear(level, anchor.above(2), id, 2);
                    h.assertTrue(n != null, "the tank stands again");
                    h.assertTrue(n.getTank().critters().size() == critters, "the snails");
                    for (UUID u : bettas) h.assertTrue(CreateGameTests.alive(level, u) == 1, "the betta is back exactly once");
                    h.assertTrue(TankInhabitants.liveInhabitants(level, n.getTank()).size() == 2, "both in the tank");
                    h.assertTrue(n.stowed().isEmpty(), "nothing left packed");
                });
            });
        });
    }

    @GameTest(template = EMPTY, batch = "bettacraft_create_config", timeoutTicks = 200)
    public static void travelCareOffPausesTheTrip(GameTestHelper h) throws Exception {
        if (!CreateGameTests.create()) {
            h.succeed();
            return;
        }
        boolean saved = Features.own(Feature.CREATE_TRAVEL_CARE);
        atEnd(h, () -> Features.set(Feature.CREATE_TRAVEL_CARE, saved));
        Features.set(Feature.CREATE_TRAVEL_CARE, false);
        ServerLevel level = h.getLevel();
        BlockEntity bearing = CreateGameTests.bearing(h, new BlockPos(20, 1, 20));
        TankBlockEntity c = CreateGameTests.stockedTank(h, 20, 2, 20, 2, 1, 2);
        UUID id = c.getTank().id();
        List<UUID> bettas = CreateGameTests.animals(h, c);
        BlockPos anchor = c.getBlockPos();
        Entity contraption = CreateGameTests.assemble(bearing);
        h.onEachTick(() -> CreateGameTests.hold(bearing));
        h.runAfterDelay(60, () -> {
            CompoundTag d = controllerData(contraption, id);
            h.assertTrue(!d.contains(TravelCare.TRIP_KEY) && sinceFed(d) < 0.001, "no care on the road: " + d.getCompoundOrEmpty(TravelCare.TRIP_KEY));
            try {
                CreateGameTests.disassemble(bearing, contraption, BlockPos.ZERO, 0F);
            } catch (Exception e) {
                throw new GameTestFailure("disassembly failed: " + e);
            }
            h.succeedWhen(() -> {
                TankBlockEntity n = CreateGameTests.tankNear(level, anchor, id, 1);
                h.assertTrue(n != null, "the tank stands again");
                for (UUID u : bettas) h.assertTrue(CreateGameTests.alive(level, u) == 1, "the betta is back exactly once");
            });
        });
    }
}
