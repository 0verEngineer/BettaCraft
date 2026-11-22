package org.overengineer.bettacraft.gametest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.debug.BugReport;
import org.overengineer.bettacraft.debug.DebugLog;
import org.overengineer.bettacraft.debug.LogArea;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;

/**
 * M57: the debug log. {@code /bettacraft debug <area> <on|off>} switches an area at once, its lines (an animal refused
 * with the reason, an animal taken, a death with its cause, the tank log) reach {@code logs/bettacraft-debug.log}; the
 * common config {@code debug.logLevel} / {@code debug.areas} sets the same switches; {@code /bettacraft report} writes the
 * versions, the changed config values and the data of the tank. Own batch: the switches are global (restored at the end).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DebugLogGameTests {
    private static final String EMPTY = "empty_32x10x32";
    private static final String BATCH = "bettacraft_m57_debug";

    private DebugLogGameTests() {}

    private static int run(GameTestHelper h, String command) {
        ServerLevel level = h.getLevel();
        CommandSourceStack src = level.getServer().createCommandSourceStack().withSuppressedOutput().withLevel(level).withPermission(4);
        try {
            return level.getServer().getCommands().getDispatcher().execute(command, src);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new GameTestAssertException("/" + command + ": " + e.getMessage());
        }
    }

    private static ItemStack bucket(long seed) {
        BettaData d = BettaData.create(BettaGenotype.randomDomestic(RandomSource.create(seed)), true, 0);
        return BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d);
    }

    private static String debugFile() {
        if (!DebugLog.file().flush(5000)) throw new GameTestAssertException("the debug log was not written in 5 s");
        try {
            return Files.readString(DebugLog.file().file(), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new GameTestAssertException("cannot read the debug log: " + e);
        }
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public static void debugCommandSwitchesAreasAndTheLinesReachTheFile(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        try {
            run(h, "bettacraft debug all off");
            for (LogArea a : LogArea.values()) h.assertFalse(a.debug, a.id + " is off");
            run(h, "bettacraft debug tank on");
            run(h, "bettacraft debug care on");
            h.assertTrue(LogArea.TANK.debug && LogArea.CARE.debug && !LogArea.CARE.trace, "tank and care on");
            h.assertFalse(LogArea.NETWORK.debug, "network stays off");
            h.assertTrue(DebugLog.overridden(LogArea.TANK), "the command overrides the config");

            TankBlockEntity c = AerationGameTests.tank(h, 2, 1, 2, 3, 3, 3);
            TankInteractions.changeWater(level, c, -c.getTank().waterMb(), 0F); // empty: the betta is refused
            h.assertTrue(TankInhabitants.insert(level, c, bucket(57), ModEntities.BETTA.get(), null, 0L) != null, "refused in an empty tank");
            TankInteractions.changeWater(level, c, c.getTank().capacityMb(), 0F);
            h.assertTrue(TankInhabitants.insert(level, c, bucket(58), ModEntities.BETTA.get(), null, false) == null, "taken in a full tank");
            List<Entity> live = TankInhabitants.liveInhabitants(level, c.getTank());
            h.assertTrue(live.size() == 1, "one betta: " + live.size());
            live.get(0).kill();

            String text = debugFile();
            String id = c.getTank().id().toString();
            for (String want : new String[]{"[bettacraft.tank]: tank " + id, "refused bettacraft:betta", "took bettacraft:betta",
                    "[bettacraft.care]: tank " + id + " log: log.bettacraft.inserted", "bettacraft:betta " + live.get(0).getUUID() + " died"}) {
                h.assertTrue(text.contains(want), "the debug log has \"" + want + "\"");
            }
            run(h, "bettacraft debug all off");
            for (LogArea a : LogArea.values()) h.assertFalse(a.debug, a.id + " is off again");
            h.assertTrue(run(h, "bettacraft debug") == 0, "the status counts no area on");
        } finally {
            DebugLog.override(null, null);
        }
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public static void configLevelAndAreasSetTheSwitches(GameTestHelper h) {
        DebugLog.Level level = CommonConfig.DEBUG_LOG_LEVEL.get();
        List<? extends String> areas = CommonConfig.DEBUG_AREAS.get();
        try {
            DebugLog.override(null, null);
            CommonConfig.DEBUG_LOG_LEVEL.set(DebugLog.Level.DEBUG);
            CommonConfig.DEBUG_AREAS.set(List.of("breeding", "world"));
            CommonConfig.applyDebug();
            h.assertTrue(LogArea.BREEDING.debug && LogArea.WORLD.debug, "breeding and world on");
            h.assertFalse(LogArea.CARE.debug || LogArea.BREEDING.trace, "care off, no trace");
            CommonConfig.DEBUG_LOG_LEVEL.set(DebugLog.Level.TRACE);
            CommonConfig.DEBUG_AREAS.set(List.of("all"));
            CommonConfig.applyDebug();
            for (LogArea a : LogArea.values()) h.assertTrue(a.trace, a.id + " traces");
            CommonConfig.DEBUG_LOG_LEVEL.set(DebugLog.Level.INFO); // the default: nothing is written, the guards are false
            CommonConfig.applyDebug();
            for (LogArea a : LogArea.values()) h.assertFalse(a.debug, a.id + " off with INFO");
            h.assertFalse(DebugLog.anyOn(), "nothing on");
        } finally {
            CommonConfig.DEBUG_LOG_LEVEL.set(level);
            CommonConfig.DEBUG_AREAS.set(areas);
            CommonConfig.applyDebug();
        }
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public static void reportHasVersionsConfigAndTheTank(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = AerationGameTests.tank(h, 2, 1, 2, 3, 3, 3);
        h.assertTrue(TankInhabitants.insert(level, c, bucket(59), ModEntities.BETTA.get(), null, false) == null, "betta in");
        Entity betta = TankInhabitants.liveInhabitants(level, c.getTank()).get(0);
        List<String> lines = BugReport.lines(level.getServer(), null, c, betta);
        String text = String.join("\n", lines);
        for (String want : new String[]{"Bettacraft: ", "Minecraft: ", "Loader: ", "Java: ", "## Integrations loaded", "Preset: ",
                "## Tank looked at", "Id: " + c.getTank().id(), "Live animals: 1", "bettacraft:betta " + betta.getUUID(), "## Loaded mods",
                "## Debug log", "care: "}) {
            h.assertTrue(text.contains(want), "the report has \"" + want + "\"");
        }
        h.assertFalse(text.contains("Loaded mods\n0 mod(s)"), "the mod list is filled");
        try {
            Path file = BugReport.write(lines);
            h.assertTrue(Files.size(file) > 1000, "the report file is written");
            Files.delete(file);
        } catch (java.io.IOException e) {
            throw new GameTestAssertException("cannot write the report: " + e);
        }
        // the command from the console (no player, no tank) writes one too
        Path logs = org.overengineer.bettacraft.platform.Platform.get().gameDir().resolve("logs");
        h.assertTrue(run(h, "bettacraft report") == 1, "/bettacraft report");
        try (var files = Files.list(logs)) {
            for (Path p : files.filter(p -> p.getFileName().toString().startsWith("bettacraft-report-")).toList()) Files.delete(p);
        } catch (java.io.IOException e) {
            throw new GameTestAssertException("cannot clean up the reports: " + e);
        }
        h.succeed();
    }
}
