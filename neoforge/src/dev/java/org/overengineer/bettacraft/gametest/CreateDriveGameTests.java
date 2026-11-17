package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.DriveSide;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankDrive;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankPower;
import org.overengineer.bettacraft.winding.Winding;

/**
 * M41, dev group {@code -Pcreate}: Create's rotation charges the FE buffer of a tank through the drive adapter and the
 * shaft on the cabinet, with real stress. Create is only reached through ids, our Create-free {@link TankDrive} and
 * reflection (the dev code is not compiled against Create). Without Create only {@link #withoutCreateThereIsNoDrive} checks
 * something; the others pass at once. Every test changes the config and has its own batch; the config is restored.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CreateDriveGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private CreateDriveGameTests() {}

    private static boolean create() {
        return org.overengineer.bettacraft.platform.Platform.get().isModLoaded("create");
    }

    // ------------------------------------------------------------------ config

    /** Power options and the drive settings, restored after a test. */
    record Saved(boolean heater, boolean filter, boolean light, boolean air, boolean power, boolean winding, boolean drive, boolean cabinetDrive,
                 int fullSpeed, int fe, int max, double perFe, double idle, List<? extends String> sides) {
        static Saved current() {
            return new Saved(ServerConfig.POWER_HEATER.get(), ServerConfig.POWER_FILTER.get(), ServerConfig.POWER_LIGHT.get(), ServerConfig.POWER_AIR_PUMP.get(),
                    Features.own(Feature.POWER), Features.own(Feature.WINDING), Features.own(Feature.CREATE_DRIVE), Features.own(Feature.CREATE_CABINET_DRIVE),
                    ServerConfig.DRIVE_FULL_SPEED.get(), ServerConfig.DRIVE_FE.get(), ServerConfig.DRIVE_MAX_FE.get(), ServerConfig.DRIVE_STRESS_PER_FE.get(),
                    ServerConfig.DRIVE_IDLE_STRESS.get(), List.copyOf(ServerConfig.DRIVE_CABINET_SIDES.get()));
        }

        void apply() {
            ServerConfig.POWER_HEATER.set(heater);
            ServerConfig.POWER_FILTER.set(filter);
            ServerConfig.POWER_LIGHT.set(light);
            ServerConfig.POWER_AIR_PUMP.set(air);
            Features.set(Feature.POWER, power);
            Features.set(Feature.WINDING, winding);
            Features.set(Feature.CREATE_DRIVE, drive);
            Features.set(Feature.CREATE_CABINET_DRIVE, cabinetDrive);
            ServerConfig.DRIVE_FULL_SPEED.set(fullSpeed);
            ServerConfig.DRIVE_FE.set(fe);
            ServerConfig.DRIVE_MAX_FE.set(max);
            ServerConfig.DRIVE_STRESS_PER_FE.set(perFe);
            ServerConfig.DRIVE_IDLE_STRESS.set(idle);
            ServerConfig.DRIVE_CABINET_SIDES.set(sides);
        }

        /** Every tank device needs FE, the drive at its defaults, winding as given. */
        static void powered(boolean winding) {
            ServerConfig.POWER_HEATER.set(true);
            ServerConfig.POWER_FILTER.set(true);
            ServerConfig.POWER_LIGHT.set(true);
            ServerConfig.POWER_AIR_PUMP.set(true);
            Features.set(Feature.POWER, true);
            Features.set(Feature.WINDING, winding);
            Features.set(Feature.CREATE_DRIVE, true);
            Features.set(Feature.CREATE_CABINET_DRIVE, true);
            ServerConfig.DRIVE_FULL_SPEED.set(64);
            ServerConfig.DRIVE_FE.set(256);
            ServerConfig.DRIVE_MAX_FE.set(1024);
            ServerConfig.DRIVE_STRESS_PER_FE.set(2.0);
            ServerConfig.DRIVE_IDLE_STRESS.set(0.5);
            ServerConfig.DRIVE_CABINET_SIDES.set(List.of("back", "left", "right", "front", "bottom"));
        }
    }

    // ------------------------------------------------------------------ helpers

    private static TankBlockEntity tank(GameTestHelper h, int x, int y, int z) {
        TankBlockEntity c = AerationGameTests.tank(h, x, y, z, 2, 2, 2);
        c.getTank().setPowerBuffer(0);
        return c;
    }

    /** An external filter with a heater on the tank wall, a lit light on top, the tank to heat: every group has demand. */
    private static void devices(GameTestHelper h, TankBlockEntity c, BlockPos filterRel, Direction towardsTank, BlockPos lightRel) {
        h.setBlock(filterRel, ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, towardsTank));
        if (!(h.getLevel().getBlockEntity(h.absolutePos(filterRel)) instanceof ExternalFilterBlockEntity f)) throw new GameTestFailure("no filter");
        f.insertHeater(new ItemStack(ModItems.HEATER_SMALL.get()));
        h.setBlock(lightRel, ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.LIT, true));
        c.getTank().setTargetTemp(34F);
        TankEquipment.invalidate(c);
    }

    private static Block adapterBlock() {
        Block b = BuiltInRegistries.BLOCK.getValue(BettaCraft.id("drive_adapter"));
        if (b == Blocks.AIR) throw new GameTestFailure("no drive adapter with Create");
        return b;
    }

    /** A drive adapter at {@code rel} whose shaft points {@code facing} (its back on the tank), a creative motor in front of it. */
    private static TankDrive.Input adapter(GameTestHelper h, BlockPos rel, Direction facing, int rpm) {
        BlockState s = CreateDeviceGameTests.with(adapterBlock().defaultBlockState(), "facing", facing);
        h.setBlock(rel, s);
        if (rpm > 0) CreateDeviceGameTests.motor(h, rel.relative(facing), facing.getOpposite(), rpm);
        return input(h, rel);
    }

    private static TankDrive.Input input(GameTestHelper h, BlockPos rel) {
        if (!(h.getLevel().getBlockEntity(h.absolutePos(rel)) instanceof TankDrive.Input in)) throw new GameTestFailure("no adapter at " + rel);
        return in;
    }

    private static CabinetBlockEntity cabinet(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel, ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH));
        CabinetBlockEntity cab = (CabinetBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(rel));
        if (cab == null) throw new GameTestFailure("no cabinet");
        cab.items().setStackInSlot(CabinetBlockEntity.FILTER_SLOT, new ItemStack(ModItems.EXTERNAL_FILTER.get()));
        return cab;
    }

    /** The stress (SU) of the network of a kinetic block entity, as Create counts it. */
    static float networkStress(BlockEntity kinetic) {
        try {
            // public methods only: the declared ones of Create's classes name ComputerCraft types that are not there
            Object net = kinetic.getClass().getMethod("getOrCreateNetwork").invoke(kinetic);
            return (Float) net.getClass().getMethod("calculateStress").invoke(net);
        } catch (ReflectiveOperationException e) {
            throw new GameTestFailure("network stress: " + e);
        }
    }

    static boolean overstressed(BlockEntity kinetic) {
        try {
            return (Boolean) kinetic.getClass().getMethod("isOverStressed").invoke(kinetic);
        } catch (ReflectiveOperationException e) {
            throw new GameTestFailure("overstressed: " + e);
        }
    }

    private static BlockEntity be(GameTestHelper h, BlockPos rel) {
        BlockEntity b = h.getLevel().getBlockEntity(h.absolutePos(rel));
        if (b == null) throw new GameTestFailure("no block entity at " + rel);
        return b;
    }

    /** One care step's drive (20 ticks): returns the FE it put into the buffer. */
    private static long drive(ServerLevel level, TankBlockEntity c) {
        long b0 = c.getTank().powerBuffer();
        TankDrive.step(level, c, 20);
        return c.getTank().powerBuffer() - b0;
    }

    // ------------------------------------------------------------------ tests

    /** Without Create: no adapter block, no bridge, the cabinet takes no shaft, the care step has nothing to do. */
    @GameTest(template = EMPTY, batch = "bettacraft_m41_nocreate", timeoutTicks = 60)
    public static void withoutCreateThereIsNoDrive(GameTestHelper h) {
        if (create()) {
            h.assertTrue(TankDrive.BRIDGE != null && TankDrive.ADAPTER_ITEM != null, "with Create: the bridge and the adapter item are there");
            h.succeed();
            return;
        }
        h.assertFalse(BuiltInRegistries.BLOCK.containsKey(BettaCraft.id("drive_adapter")), "no drive adapter block without Create");
        h.assertFalse(BuiltInRegistries.ITEM.containsKey(BettaCraft.id("drive_adapter")), "no drive adapter item without Create");
        h.assertTrue(TankDrive.BRIDGE == null && !TankDrive.enabled() && !TankDrive.cabinetsEnabled(), "no bridge, nothing enabled");
        Saved saved = Saved.current();
        try {
            Saved.powered(false);
            TankBlockEntity c = tank(h, 2, 2, 2);
            CabinetBlockEntity cab = cabinet(h, new BlockPos(2, 1, 2));
            cab.setDriveSide(DriveSide.BACK);
            h.assertTrue(cab.driveSetting() == DriveSide.BACK && cab.driveSide() == DriveSide.NONE, "the setting is kept, but it takes no shaft");
            TankEquipment.invalidate(c);
            h.assertTrue(drive(h.getLevel(), c) == 0 && c.driveInputs() == 0, "the care step charges nothing");
            h.assertTrue(TankEquipment.driveAdapters(h.getLevel(), c).isEmpty(), "no adapters found");
        } finally {
            saved.apply();
        }
        h.succeed();
    }

    /**
     * A drive adapter on a tank wall (no cabinet, no FE cable): the motor's rotation fills the buffer, and the filter, the
     * heater in it and the light run on it; its stress is in Create's network and follows the load.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m41_adapter", timeoutTicks = 200)
    public static void adapterPowersEveryDeviceOfItsTank(GameTestHelper h) {
        if (!create()) {
            h.succeed();
            return;
        }
        ServerLevel level = h.getLevel();
        Saved saved = Saved.current();
        Saved.powered(false);
        TankBlockEntity c = tank(h, 4, 2, 4);
        devices(h, c, new BlockPos(6, 2, 4), Direction.WEST, new BlockPos(4, 4, 4));
        BlockPos at = new BlockPos(3, 2, 4);
        TankDrive.Input in = adapter(h, at, Direction.WEST, 64);
        var watcher = QuestAdv.watcher(h, c); // M45
        h.runAfterDelay(10, () -> {
            try {
                h.assertTrue(in.speed() == 64F, "the adapter turns at 64 RPM: " + in.speed());
                h.assertTrue(TankEquipment.driveAdapters(level, c).size() == 1, "the tank finds its adapter");
                // the first step fills the empty buffer at full power
                long got = drive(level, c);
                h.assertTrue(got == 256L * 20, "full power for an empty buffer: " + got);
                QuestAdv.check(h, watcher, "create_drive");
                QuestAdv.gone(h, watcher);
                float full = in.stressImpact();
                h.assertTrue(Math.abs(full * 64F - 512F) < 1F, "512 SU at full load: " + full * 64F);
                h.assertTrue(Math.abs(networkStress(be(h, at)) - 512F) < 1F, "Create's network carries the stress: " + networkStress(be(h, at)));
                TankPower.step(level, c, 20);
                h.assertTrue(c.filterPowered() && c.heatPowered() && c.lightPowered(), "every group runs on the rotation");
                h.assertTrue(c.lastDemand() > 0, "the devices took FE: " + c.lastDemand());
                // a full buffer: only what the devices took (the care step: the devices first, then the drive)
                c.getTank().setPowerBuffer(TankPower.capacity());
                TankPower.step(level, c, 20);
                long low = drive(level, c);
                float lowImpact = in.stressImpact();
                h.assertTrue(low == (long) c.lastDemand() * 20, "a full buffer takes only the demand: " + low + " for " + c.lastDemand());
                h.assertTrue(lowImpact < full, "less load, less stress: " + lowImpact + " < " + full);
                // more devices (a second heated filter): more load, more stress
                h.setBlock(new BlockPos(5, 2, 6), ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, Direction.NORTH));
                ((ExternalFilterBlockEntity) be(h, new BlockPos(5, 2, 6))).insertHeater(new ItemStack(ModItems.HEATER_LARGE.get()));
                TankEquipment.invalidate(c);
                c.getTank().setPowerBuffer(TankPower.capacity());
                int before = c.lastDemand();
                TankPower.step(level, c, 20);
                h.assertTrue(c.lastDemand() > before, "the second filter needs more: " + before + " -> " + c.lastDemand());
                drive(level, c);
                h.assertTrue(c.getTank().powerBuffer() == TankPower.capacity(), "the buffer is full again");
                h.assertTrue(in.stressImpact() > lowImpact, "more load, more stress: " + lowImpact + " -> " + in.stressImpact());
                h.assertTrue(Math.abs(in.stressImpact() * 64F - c.lastDemand() * 2F) < 1F || in.stressImpact() * 64F <= 32.01F,
                        "2 SU per FE/t: " + in.stressImpact() * 64F + " SU for " + c.lastDemand() + " FE/t");
                // half speed: half the power
                CreateDeviceGameTests.motor(h, at.relative(Direction.WEST), Direction.EAST, 32);
            } catch (GameTestAssertException e) {
                saved.apply();
                throw e;
            }
        });
        h.runAfterDelay(25, () -> {
            try {
                h.assertTrue(in.speed() == 32F, "32 RPM now: " + in.speed());
                c.getTank().setPowerBuffer(0);
                long half = drive(level, c);
                h.assertTrue(half == 128L * 20, "half speed, half power: " + half);
            } finally {
                saved.apply();
            }
            h.succeed();
        });
    }

    /** Three adapters can give 768 FE/t; with the limit at 300 FE/t the tank gets 300, shared evenly. */
    @GameTest(template = EMPTY, batch = "bettacraft_m41_cap", timeoutTicks = 100)
    public static void severalAdaptersAddUpToTheLimit(GameTestHelper h) {
        if (!create()) {
            h.succeed();
            return;
        }
        ServerLevel level = h.getLevel();
        Saved saved = Saved.current();
        Saved.powered(false);
        TankBlockEntity c = tank(h, 4, 2, 4);
        devices(h, c, new BlockPos(6, 2, 4), Direction.WEST, new BlockPos(4, 4, 4));
        TankDrive.Input a = adapter(h, new BlockPos(3, 2, 4), Direction.WEST, 64);
        TankDrive.Input b = adapter(h, new BlockPos(3, 3, 5), Direction.WEST, 64);
        TankDrive.Input under = adapter(h, new BlockPos(5, 1, 5), Direction.DOWN, 64); // one under the tank
        h.runAfterDelay(10, () -> {
            try {
                TankEquipment.invalidate(c);
                h.assertTrue(TankEquipment.driveAdapters(level, c).size() == 3, "three adapters: " + TankEquipment.driveAdapters(level, c));
                long all = drive(level, c);
                h.assertTrue(all == 3L * 256 * 20 && c.driveFe() == 768, "they add up: " + all + " (" + c.driveFe() + " FE/t)");
                ServerConfig.DRIVE_MAX_FE.set(300);
                c.getTank().setPowerBuffer(0);
                long capped = drive(level, c);
                h.assertTrue(capped == 300L * 20 && c.driveFe() == 300, "the limit: " + capped + " (" + c.driveFe() + " FE/t)");
                float sa = a.stressImpact(), sb = b.stressImpact(), su = under.stressImpact();
                h.assertTrue(Math.abs(sa - sb) < 1e-3F && Math.abs(sa - su) < 1e-3F && Math.abs(sa * 64F - 200F) < 1F,
                        "shared evenly, each 100 FE/t = 200 SU: " + sa * 64F + ", " + sb * 64F + ", " + su * 64F);
            } finally {
                saved.apply();
            }
            h.succeed();
        });
    }

    /** Too much stress for the motor: the network is overstressed and the tank gets nothing; with less it runs again. */
    @GameTest(template = EMPTY, batch = "bettacraft_m41_overload", timeoutTicks = 100)
    public static void overstressedNetworkGivesNothing(GameTestHelper h) {
        if (!create()) {
            h.succeed();
            return;
        }
        ServerLevel level = h.getLevel();
        Saved saved = Saved.current();
        Saved.powered(false);
        TankBlockEntity c = tank(h, 4, 2, 4);
        devices(h, c, new BlockPos(6, 2, 4), Direction.WEST, new BlockPos(4, 4, 4));
        BlockPos at = new BlockPos(3, 2, 4);
        TankDrive.Input in = adapter(h, at, Direction.WEST, 64);
        h.runAfterDelay(10, () -> {
            try {
                // the creative motor holds 16384 SU per RPM: 100000 SU per FE/t at 256 FE/t is far beyond it
                ServerConfig.DRIVE_STRESS_PER_FE.set(100000.0);
                drive(level, c); // the load is set: the network is overstressed
                h.assertTrue(in.overstressed() && overstressed(be(h, at)), "overstressed: " + networkStress(be(h, at)) + " SU");
                c.getTank().setPowerBuffer(0);
                long got = drive(level, c);
                h.assertTrue(got == 0 && c.driveFe() == 0 && c.driveOverstressed(), "no power while overstressed: " + got);
                TankPower.step(level, c, 20);
                h.assertFalse(c.filterPowered(), "the filter stops without FE");
                // back to the normal stress: it runs again
                ServerConfig.DRIVE_STRESS_PER_FE.set(2.0);
                drive(level, c); // the load at the normal stress
                h.assertFalse(in.overstressed(), "no longer overstressed: " + networkStress(be(h, at)) + " SU");
                long again = drive(level, c);
                h.assertTrue(again > 0, "it charges again: " + again);
            } finally {
                saved.apply();
            }
            h.succeed();
        });
    }

    /**
     * The shaft on the cabinet, on every side it can take (back, left, right, front, bottom): it fills the buffer, the
     * cabinet's filter runs, and the stress lies on the motor's network; a side the server does not allow takes nothing.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m41_cabinet", timeoutTicks = 400)
    public static void cabinetShaftOnEverySide(GameTestHelper h) {
        if (!create()) {
            h.succeed();
            return;
        }
        ServerLevel level = h.getLevel();
        Saved saved = Saved.current();
        Saved.powered(false);
        TankBlockEntity c = tank(h, 4, 3, 4);
        BlockPos cabPos = new BlockPos(4, 2, 4);
        CabinetBlockEntity cab = cabinet(h, cabPos);
        TankEquipment.invalidate(c);
        DriveSide[] sides = {DriveSide.BACK, DriveSide.LEFT, DriveSide.RIGHT, DriveSide.FRONT, DriveSide.BOTTOM};
        var seq = h.startSequence();
        for (DriveSide side : sides) {
            seq.thenExecute(() -> {
                cab.setDriveSide(side);
                Direction face = cab.driveFace();
                if (face == null) throw new GameTestFailure("no face for " + side);
                CreateDeviceGameTests.motor(h, cabPos.relative(face), face.getOpposite(), 64);
            }).thenExecuteAfter(8, () -> {
                Direction face = cab.driveFace();
                BlockPos motor = cabPos.relative(face);
                c.getTank().setPowerBuffer(0);
                long got = drive(level, c);
                h.assertTrue(got == 256L * 20 && c.driveInputs() == 1, side + ": the shaft charges the buffer: " + got);
                h.assertTrue(Math.abs(networkStress(be(h, motor)) - 512F) < 1F, side + ": the stress is on the motor's network: " + networkStress(be(h, motor)));
                TankPower.step(level, c, 20);
                h.assertTrue(c.filterPowered(), side + ": the cabinet's filter runs");
                // the side switched off: the stress goes back
                cab.setDriveSide(DriveSide.NONE);
                h.assertTrue(networkStress(be(h, motor)) < 1F, side + ": no stress without the drive side: " + networkStress(be(h, motor)));
                h.assertTrue(drive(level, c) == 0, side + ": nothing without the drive side");
                h.setBlock(motor, Blocks.AIR.defaultBlockState());
            });
        }
        seq.thenExecute(() -> {
            // a side the server does not allow: the setting stays, the cabinet takes nothing
            ServerConfig.DRIVE_CABINET_SIDES.set(List.of("bottom"));
            cab.setDriveSide(DriveSide.BACK);
            h.assertTrue(cab.driveSetting() == DriveSide.BACK && cab.driveSide() == DriveSide.NONE, "back is not allowed now");
            CreateDeviceGameTests.motor(h, cabPos.relative(Direction.SOUTH), Direction.NORTH, 64);
        }).thenExecuteAfter(8, () -> {
            c.getTank().setPowerBuffer(0);
            h.assertTrue(drive(level, c) == 0, "a side that is not allowed takes nothing");
            h.assertTrue(networkStress(be(h, cabPos.relative(Direction.SOUTH))) < 1F, "and costs nothing");
        }).thenExecute(saved::apply).thenSucceed();
    }

    /**
     * M37 with M41: a cabinet's shaft fills the buffer first and winds the cabinet's spring with the rest; an adapter winds no
     * spring; the crank still winds the cabinet; without rotation the spring stands in for the missing FE.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m41_winding", timeoutTicks = 200)
    public static void cabinetShaftWindsTheSpring(GameTestHelper h) {
        if (!create()) {
            h.succeed();
            return;
        }
        ServerLevel level = h.getLevel();
        Saved saved = Saved.current();
        Saved.powered(true);
        TankBlockEntity c = tank(h, 4, 3, 4);
        BlockPos cabPos = new BlockPos(4, 2, 4);
        CabinetBlockEntity cab = cabinet(h, cabPos);
        cab.setDriveSide(DriveSide.BACK); // facing north: the back is south
        BlockPos motor = cabPos.relative(Direction.SOUTH);
        CreateDeviceGameTests.motor(h, motor, Direction.NORTH, 64);
        TankEquipment.invalidate(c);
        h.runAfterDelay(10, () -> {
            try {
                cab.windSpring().set(0);
                c.getTank().setPowerBuffer(TankPower.capacity());
                TankPower.step(level, c, 20); // the devices take their FE
                int demand = c.lastDemand();
                h.assertTrue(demand > 0 && demand < 256, "the tank's demand: " + demand);
                drive(level, c);
                h.assertTrue(c.getTank().powerBuffer() == TankPower.capacity(), "the buffer is full again first");
                long wound = cab.windSpring().ticks();
                // the rest of 256 FE/t after the demand winds the spring: (256 - demand) * 20 / 10 device-ticks
                long expect = (long) Math.floor((256 - demand) * 20 / 10.0);
                h.assertTrue(wound > 0 && Math.abs(wound - expect) <= 2, "the rest winds the spring: " + wound + " (expected " + expect + ", demand " + demand + ")");
                h.assertTrue(Math.abs(networkStress(be(h, motor)) - 512F) < 1F, "winding is load too: " + networkStress(be(h, motor)));
                // the crank still winds it
                long w0 = cab.windSpring().ticks();
                h.assertTrue(Winding.wind(level, cab.getBlockPos(), null) == Winding.Result.WOUND && cab.windSpring().ticks() > w0, "the crank winds it too");
                // a full spring and a full buffer: only the demand
                cab.windSpring().set(cab.windSpring().capacity());
                c.getTank().setPowerBuffer(TankPower.capacity());
                TankPower.step(level, c, 20);
                drive(level, c);
                h.assertTrue(Math.abs(networkStress(be(h, motor)) - Math.max(32F, demand * 2F)) < 1F, "full spring: only the demand: "
                        + networkStress(be(h, motor)) + " for " + demand + " FE/t");
                // power.winding off: the shaft only fills the buffer
                Features.set(Feature.WINDING, false);
                cab.windSpring().set(0);
                c.getTank().setPowerBuffer(TankPower.capacity());
                TankPower.step(level, c, 20);
                drive(level, c);
                h.assertTrue(cab.windSpring().ticks() == 0, "no winding with power.winding off");
                Features.set(Feature.WINDING, true);
                // the rotation stops: the wound spring powers the tank while its FE is short
                h.setBlock(motor, Blocks.AIR.defaultBlockState());
                cab.windSpring().set(cab.windSpring().capacity());
                c.getTank().setPowerBuffer(0);
                long s0 = cab.windSpring().ticks();
                TankDrive.step(level, c, 20);
                TankPower.step(level, c, 20);
                h.assertTrue(c.filterPowered() && cab.windSpring().ticks() < s0, "without rotation the spring stands in");
            } finally {
                saved.apply();
            }
            h.succeed();
        });
    }

    /** The switches: compat.create.drive off stops the adapters (they idle), compat.create.cabinetDrive off only the cabinet. */
    @GameTest(template = EMPTY, batch = "bettacraft_m41_switch", timeoutTicks = 200)
    public static void switchesTurnTheDriveOff(GameTestHelper h) {
        if (!create()) {
            h.succeed();
            return;
        }
        ServerLevel level = h.getLevel();
        Saved saved = Saved.current();
        Saved.powered(false);
        TankBlockEntity c = tank(h, 4, 3, 4);
        BlockPos at = new BlockPos(3, 3, 4);
        TankDrive.Input in = adapter(h, at, Direction.WEST, 64);
        BlockPos cabPos = new BlockPos(4, 2, 4);
        CabinetBlockEntity cab = cabinet(h, cabPos);
        cab.setDriveSide(DriveSide.BACK);
        BlockPos motor = cabPos.relative(Direction.SOUTH);
        CreateDeviceGameTests.motor(h, motor, Direction.NORTH, 64);
        TankEquipment.invalidate(c);
        h.runAfterDelay(10, () -> {
            try {
                h.assertTrue(drive(level, c) == 2L * 256 * 20 && c.driveInputs() == 2, "adapter and cabinet charge: " + c.driveInputs());
                // the cabinet's shaft off: only the adapter
                Features.set(Feature.CREATE_CABINET_DRIVE, false);
                c.getTank().setPowerBuffer(0);
                h.assertTrue(drive(level, c) == 256L * 20 && c.driveInputs() == 1, "only the adapter with cabinetDrive off");
                h.assertTrue(networkStress(be(h, motor)) < 1F, "the cabinet's shaft costs nothing");
                h.assertTrue(cab.driveSide() == DriveSide.NONE && cab.driveSetting() == DriveSide.BACK, "the setting is kept");
                // the whole drive off: nothing
                Features.set(Feature.CREATE_DRIVE, false);
                c.getTank().setPowerBuffer(0);
                h.assertTrue(drive(level, c) == 0 && c.driveInputs() == 0, "nothing with the drive off");
                h.assertFalse(TankDrive.enabled(), "the drive is off");
            } catch (GameTestAssertException e) {
                saved.apply();
                throw e;
            }
        });
        // the adapter notices it in its own tick (every 20 ticks): it stops costing stress
        h.runAfterDelay(50, () -> {
            try {
                h.assertTrue(in.stressImpact() == 0F && networkStress(be(h, at)) < 1F, "the adapter costs nothing with the drive off: " + in.stressImpact());
            } finally {
                saved.apply();
            }
            h.succeed();
        });
    }
}
