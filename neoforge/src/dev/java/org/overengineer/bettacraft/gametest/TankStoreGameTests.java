package org.overengineer.bettacraft.gametest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankPower;
import org.overengineer.bettacraft.tank.TankStore;
import org.overengineer.bettacraft.winding.Winding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M67 GameTests: one energy store per tank. The springs of an older world become FE once and nothing is lost; the store grows
 * with the water blocks and the cabinets (joined cabinets go to the nearer tank); power.costs OFF / EASY / HARD; a full store
 * runs a medium tank a day on EASY and about 20 seconds of cranking fill it; the old *RequiresEnergy switches of a world's own
 * server config become power.costs. Every test changes the power config: own batch, restored.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TankStoreGameTests {
    private static final String EMPTY = "empty_32x10x32";
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/gametest");

    private TankStoreGameTests() {}

    // ------------------------------------------------------------------ helpers

    private static CabinetBlockEntity cabinet(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel, ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH));
        if (!(h.getLevel().getBlockEntity(h.absolutePos(rel)) instanceof CabinetBlockEntity cab)) throw new GameTestAssertException("no cabinet at " + rel);
        return cab;
    }

    private static ExternalFilterBlockEntity filter(GameTestHelper h, BlockPos rel, Direction towardsTank) {
        h.setBlock(rel, ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, towardsTank));
        if (!(h.getLevel().getBlockEntity(h.absolutePos(rel)) instanceof ExternalFilterBlockEntity f)) throw new GameTestAssertException("no filter at " + rel);
        return f;
    }

    /** What an older world saved: the block entity's data with {@code ticks} wound into its spring (any size, as the old capacity allowed). */
    private static void oldSpring(GameTestHelper h, net.minecraft.world.level.block.entity.BlockEntity be, long ticks) {
        var reg = h.getLevel().registryAccess();
        CompoundTag tag = be.saveWithFullMetadata(reg);
        tag.putLong("spring", ticks);
        be.loadWithComponents(tag, reg);
    }

    /**
     * The medium tank of the design: 2 x 2 x 2 (8 water blocks) on four cabinets, an external filter with a small heater that
     * heats, a lit light on top.
     */
    private static TankBlockEntity mediumTank(GameTestHelper h, int x, int z) {
        for (int a = 0; a < 2; a++) for (int b = 0; b < 2; b++) cabinet(h, new BlockPos(x + a, 1, z + b));
        TankBlockEntity c = AerationGameTests.tank(h, x, 2, z, 2, 2, 2);
        ExternalFilterBlockEntity f = filter(h, new BlockPos(x + 2, 2, z), Direction.WEST);
        f.insertHeater(new ItemStack(ModItems.HEATER_SMALL.get()));
        h.setBlock(new BlockPos(x, 4, z), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.LIT, true));
        TankEquipment.invalidate(c);
        c.getTank().setTargetTemp(34F); // it heats
        c.getTank().setPowerBuffer(0);
        return c;
    }

    // ------------------------------------------------------------------ migration of the springs

    /**
     * An older world: two cabinets under the tank still hold wound springs (one full with the old six charges), the external
     * filter one charge. The first care step puts all of it into the tank's store (10 FE a device-tick), also beyond the
     * capacity, and empties the springs; the next steps count nothing twice. A cabinet broken before its tank ran hands its
     * spring over to the tank.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m67_migration", timeoutTicks = 100)
    public static void woundSpringsOfAnOldWorldBecomeEnergy(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        PowerSettings saved = PowerSettings.current();
        try {
            PowerSettings.costs(ServerConfig.PowerCosts.OFF); // nothing is lost even with power costs off
            TankBlockEntity c = mediumTank(h, 2, 2);
            CabinetBlockEntity a = (CabinetBlockEntity) level.getBlockEntity(h.absolutePos(new BlockPos(2, 1, 2)));
            CabinetBlockEntity b = (CabinetBlockEntity) level.getBlockEntity(h.absolutePos(new BlockPos(3, 1, 3)));
            ExternalFilterBlockEntity f = (ExternalFilterBlockEntity) level.getBlockEntity(h.absolutePos(new BlockPos(4, 2, 2)));
            oldSpring(h, a, 144_000); // six charges of a day, wound full
            oldSpring(h, b, 50_000);
            oldSpring(h, f, 24_000);
            h.assertTrue(a.windSpring().ticks() == 144_000 && f.windSpring().ticks() == 24_000, "the old springs loaded whole");
            c.getTank().setPowerBuffer(5_000);
            long expect = 5_000 + (144_000 + 50_000 + 24_000) * TankStore.FE_PER_SPRING_TICK;
            TankPower.step(level, c, 20);
            h.assertTrue(c.getTank().powerBuffer() == expect, "the springs became FE: " + c.getTank().powerBuffer() + ", expected " + expect);
            h.assertTrue(a.windSpring().empty() && b.windSpring().empty() && f.windSpring().empty(), "the springs are empty now");
            h.assertTrue(c.getTank().powerBuffer() > TankPower.capacity(c), "kept beyond the capacity: nothing lost (" + TankPower.capacity(c) + ")");
            TankPower.step(level, c, 20);
            h.assertTrue(c.getTank().powerBuffer() == expect, "counted once");
            // a cabinet with an old spring is broken before its tank ran: the spring goes into the store
            CabinetBlockEntity d = (CabinetBlockEntity) level.getBlockEntity(h.absolutePos(new BlockPos(3, 1, 2)));
            oldSpring(h, d, 1_000);
            level.destroyBlock(d.getBlockPos(), false);
            h.assertTrue(c.getTank().powerBuffer() == expect + 1_000 * TankStore.FE_PER_SPRING_TICK, "a broken cabinet hands its spring over: " + c.getTank().powerBuffer());
            // with power costs the surplus runs down first, the store takes no FE while it is over its capacity
            PowerSettings.costs(ServerConfig.PowerCosts.HARD);
            long before = c.getTank().powerBuffer();
            TankPower.step(level, c, 20);
            h.assertTrue(c.getTank().powerBuffer() == before - 20L * c.lastDemand() && c.lastDemand() > 0, "the devices take from it");
            var storage = org.overengineer.bettacraft.platform.Platform.get().energyStorage(level, c.getBlockPos(), null);
            h.assertTrue(storage != null && storage.receiveEnergy(1000, false) == 0, "over its capacity: no FE goes in");
        } finally {
            saved.apply();
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ capacity

    /**
     * The capacity: per water block plus per cabinet under the tank; with joined cabinets also the ones beside them (a joined
     * cabinet between two tanks goes to the nearer one, counted once); the crank at a joined cabinet charges its tank.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m67_capacity", timeoutTicks = 100)
    public static void theStoreGrowsWithTheTankAndItsCabinets(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        PowerSettings saved = PowerSettings.current();
        try {
            PowerSettings.costs(ServerConfig.PowerCosts.EASY);
            Features.set(Feature.CABINET_SHARED_SPRING, true);
            long w = TankStore.perWaterBlock(), k = TankStore.perCabinet();
            TankBlockEntity bare = AerationGameTests.tank(h, 2, 2, 12, 2, 2, 2);
            TankPower.step(level, bare, 20);
            h.assertTrue(TankPower.capacity(bare) == 8 * w, "8 water blocks: " + TankPower.capacity(bare));
            // a 2 x 1 x 1 tank on two cabinets, two more joined on the right, then a cabinet under a second tank
            cabinet(h, new BlockPos(2, 1, 2));
            cabinet(h, new BlockPos(3, 1, 2));
            CabinetBlockEntity j1 = cabinet(h, new BlockPos(4, 1, 2));
            CabinetBlockEntity j2 = cabinet(h, new BlockPos(5, 1, 2));
            CabinetBlockEntity j3 = cabinet(h, new BlockPos(6, 1, 2));
            cabinet(h, new BlockPos(7, 1, 2));
            cabinet(h, new BlockPos(8, 1, 2));
            TankBlockEntity a = AerationGameTests.tank(h, 2, 2, 2, 2, 1, 1);
            TankBlockEntity b = AerationGameTests.tank(h, 8, 2, 2, 1, 1, 1);
            TankEquipment.invalidate(a);
            TankEquipment.invalidate(b);
            TankPower.step(level, a, 20);
            TankPower.step(level, b, 20);
            // a: 2 and 3 under it + 4 and 5 (nearer to a); b: 8 under it + 6 and 7 (nearer to b)
            h.assertTrue(TankStore.cabinets(level, a).size() == 4 && TankPower.capacity(a) == 2 * w + 4 * k, "a: 2 water blocks + 4 cabinets: " + TankPower.capacity(a));
            h.assertTrue(TankStore.cabinets(level, b).size() == 3 && TankPower.capacity(b) == w + 3 * k, "b: 1 water block + 3 cabinets: " + TankPower.capacity(b));
            h.assertTrue(TankStore.tankOf(level, j1) == a && TankStore.tankOf(level, j2) == a && TankStore.tankOf(level, j3) == b, "the nearer tank owns a joined cabinet");
            a.getTank().setPowerBuffer(0);
            h.assertTrue(Winding.wind(level, j2.getBlockPos(), null) == Winding.Result.WOUND && a.getTank().powerBuffer() == TankStore.fePerClick(),
                    "the crank at a joined cabinet charges its tank");
            // the switch off: only the cabinets right under the tank count, a joined one takes no crank
            Features.set(Feature.CABINET_SHARED_SPRING, false);
            TankPower.step(level, a, 20);
            h.assertTrue(TankPower.capacity(a) == 2 * w + 2 * k, "switch off: only the two under the tank: " + TankPower.capacity(a));
            h.assertTrue(Winding.wind(level, j2.getBlockPos(), null) == Winding.Result.REFUSED, "a joined cabinet charges nothing with the switch off");
        } finally {
            saved.apply();
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ power.costs

    /** OFF: nothing needs FE; HARD: the FE values as they are (heater 8 x 8, filter 10, light 4); EASY: a third of each, rounded up. */
    @GameTest(template = EMPTY, batch = "bettacraft_m67_costs", timeoutTicks = 100)
    public static void powerCostsOffEasyHard(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        PowerSettings saved = PowerSettings.current();
        try {
            TankBlockEntity c = mediumTank(h, 2, 2);
            c.getTank().setPowerBuffer(1_000_000);
            PowerSettings.costs(ServerConfig.PowerCosts.OFF);
            TankPower.step(level, c, 20);
            h.assertTrue(!TankPower.anyRequired() && c.lastDemand() == 0 && c.filterPowered() && c.heatPowered() && c.lightPowered(), "OFF: everything runs, no FE");
            h.assertTrue(org.overengineer.bettacraft.platform.Platform.get().energyStorage(level, c.getBlockPos(), null) == null, "OFF: no energy capability");
            PowerSettings.costs(ServerConfig.PowerCosts.HARD);
            TankPower.step(level, c, 20);
            int hard = c.lastDemand();
            h.assertTrue(hard == 8 * 8 + 10 + 4, "HARD: 64 + 10 + 4 FE/t, got " + hard);
            PowerSettings.costs(ServerConfig.PowerCosts.EASY);
            long b0 = c.getTank().powerBuffer();
            TankPower.step(level, c, 20);
            int easy = c.lastDemand();
            h.assertTrue(easy == 22 + 4 + 2, "EASY: a third, rounded up per group (22 + 4 + 2), got " + easy);
            h.assertTrue(b0 - c.getTank().powerBuffer() == 20L * easy, "EASY takes its demand");
            // the devices without a tank follow the same setting
            h.assertTrue(org.overengineer.bettacraft.plumbing.DeviceEnergy.required() && org.overengineer.bettacraft.plumbing.DeviceEnergy.pumpFe() == 3,
                    "a pump on EASY: 8 / 3 -> 3 FE/t, got " + org.overengineer.bettacraft.plumbing.DeviceEnergy.pumpFe());
            PowerSettings.costs(ServerConfig.PowerCosts.HARD);
            h.assertTrue(org.overengineer.bettacraft.plumbing.DeviceEnergy.pumpFe() == 8, "a pump on HARD: 8 FE/t");
            // features.power.enabled off: like OFF
            Features.set(Feature.POWER, false);
            h.assertTrue(TankPower.costs() == ServerConfig.PowerCosts.OFF && !org.overengineer.bettacraft.plumbing.DeviceEnergy.required(), "power off: costs OFF");
        } finally {
            saved.apply();
        }
        h.succeed();
    }

    /**
     * The numbers of the design: a full store runs the medium tank (8 water blocks, four cabinets, filter, light and a heater
     * that heats) one in-game day on EASY, a third of a day on HARD; filling it from empty takes about 100 clicks (20 seconds
     * of holding the crank at five clicks a second).
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m67_day", timeoutTicks = 100)
    public static void aFullStoreRunsAMediumTankADayOnEasy(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        PowerSettings saved = PowerSettings.current();
        try {
            PowerSettings.costs(ServerConfig.PowerCosts.EASY);
            TankBlockEntity c = mediumTank(h, 2, 2);
            c.getTank().setPowerBuffer(1_000_000);
            TankPower.step(level, c, 20);
            long cap = TankPower.capacity(c);
            int easy = c.lastDemand();
            h.assertTrue(cap == 8 * TankStore.perWaterBlock() + 4 * TankStore.perCabinet(), "8 water blocks + 4 cabinets: " + cap);
            h.assertTrue(cap >= 24_000L * easy, "EASY: a day of " + easy + " FE/t (" + 24_000L * easy + " FE) fits into " + cap);
            PowerSettings.costs(ServerConfig.PowerCosts.HARD);
            TankPower.step(level, c, 20);
            int hard = c.lastDemand();
            double hardDays = cap / (24_000.0 * hard);
            h.assertTrue(hardDays > 0.3 && hardDays < 0.5, "HARD: about a third of a day: " + hardDays);
            // cranking it full
            PowerSettings.costs(ServerConfig.PowerCosts.EASY);
            c.getTank().setPowerBuffer(0);
            BlockPos cab = h.absolutePos(new BlockPos(2, 1, 2));
            int clicks = 0;
            while (Winding.wind(level, cab, null) == Winding.Result.WOUND) clicks++;
            h.assertTrue(c.getTank().powerBuffer() == cap, "full");
            h.assertTrue(clicks >= 90 && clicks <= 125, "about 100 clicks (20 s): " + clicks);
            LOGGER.info("M67 medium tank: store {} FE, EASY {} FE/t ({} days), HARD {} FE/t ({} days), {} clicks to fill ({} s)", cap, easy,
                    String.format(java.util.Locale.ROOT, "%.2f", cap / (24_000.0 * easy)), hard, String.format(java.util.Locale.ROOT, "%.2f", hardDays), clicks, clicks / 5.0);
        } finally {
            saved.apply();
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ the old config of a world

    /**
     * A world with its own server config from before M67 had filterRequiresEnergy on: the loader dropped the old key but kept
     * the file as {@code bettacraft-server-1.toml.bak}; the migration reads it and sets power.costs to HARD, once. All switches
     * off: power.costs stays OFF.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m67_config", timeoutTicks = 60)
    public static void oldPowerSwitchesOfAWorldBecomePowerCosts(GameTestHelper h) throws Exception {
        PowerSettings saved = PowerSettings.current();
        Path dir = Files.createTempDirectory("bettacraft-m67");
        try {
            Path file = dir.resolve("bettacraft-server.toml");
            Files.writeString(file, "[power]\ncosts = \"OFF\"\n", StandardCharsets.UTF_8);
            Path bak = dir.resolve("bettacraft-server-1.toml.bak");
            Files.writeString(bak, "[power]\nheaterRequiresEnergy = false\nfilterRequiresEnergy = false\n[pipes]\npumpsRequireEnergy = false\n", StandardCharsets.UTF_8);
            ServerConfig.POWER_COSTS.set(ServerConfig.PowerCosts.OFF);
            h.assertTrue(!org.overengineer.bettacraft.config.WorldConfigMigration.powerCostsM67(file, false)
                    && ServerConfig.POWER_COSTS.get() == ServerConfig.PowerCosts.OFF, "all off: stays OFF");
            Files.writeString(bak, "[power]\nheaterRequiresEnergy = false\nfilterRequiresEnergy = true\n", StandardCharsets.UTF_8);
            h.assertTrue(org.overengineer.bettacraft.config.WorldConfigMigration.powerCostsM67(file, false)
                    && ServerConfig.POWER_COSTS.get() == ServerConfig.PowerCosts.HARD, "a switch was on: HARD");
            // no world file (the global one, migrated before it loaded): nothing
            ServerConfig.POWER_COSTS.set(ServerConfig.PowerCosts.OFF);
            h.assertTrue(!org.overengineer.bettacraft.config.WorldConfigMigration.powerCostsM67(null, false), "no world file: nothing");
        } finally {
            saved.apply();
            try (var s = Files.list(dir)) {
                for (Path p : (Iterable<Path>) s::iterator) Files.deleteIfExists(p);
            }
            Files.deleteIfExists(dir);
        }
        h.succeed();
    }
}
