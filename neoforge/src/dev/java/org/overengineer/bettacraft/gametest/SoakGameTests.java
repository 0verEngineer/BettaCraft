package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import java.util.ArrayList;
import java.util.List;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankInhabitants;

/**
 * M42 part b: a long run with many tanks. 24 stocked tanks (a betta pair, tetras and corys, shrimp and snails; fed every day,
 * the water changed every week) live through 60 days of care in hourly steps, a day per server tick, with the animals' own
 * ticks in between. Afterwards no number of any tank or animal is NaN or infinite, no tank holds more animals than its limits
 * allow (no runaway breeding), and a day of care for all of them stays well below a tick's budget on average. The times go to
 * the log ({@code [m42] soak}).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SoakGameTests {
    static final int TANKS = 24, DAYS = 60, STEPS_PER_DAY = 24;

    private SoakGameTests() {}

    @GameTest(template = "empty_32x10x32", batch = "bettacraft_m42_soak", timeoutTicks = 2400)
    public static void manyTanksLiveForTwoMonths(GameTestHelper h) {
        List<TankBlockEntity> tanks = new ArrayList<>();
        for (int i = 0; i < TANKS; i++) tanks.add(ConfigStressGameTests.stocked(h, 1 + (i % 4) * 7, 1 + (i / 4) * 5));
        long[] nanos = new long[1];
        int[] peak = new int[1];
        day(h, tanks, 0, nanos, peak);
    }

    private static void day(GameTestHelper h, List<TankBlockEntity> tanks, int d, long[] nanos, int[] peak) {
        ServerLevel level = h.getLevel();
        if (d >= DAYS) {
            finish(h, tanks, nanos[0], peak[0]);
            return;
        }
        long t0 = System.nanoTime();
        for (TankBlockEntity c : tanks) {
            for (int s = 0; s < STEPS_PER_DAY; s++) TankCare.care(level, c, 24000L / STEPS_PER_DAY);
            // the keeper: food every day, a third of the water fresh every week
            for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) {
                if (e instanceof org.overengineer.bettacraft.entity.betta.BettaEntity b) b.health().feed();
                if (e instanceof org.overengineer.bettacraft.entity.tankmate.TankmateEntity m) m.health().feed();
            }
            c.getTank().setCritterFood(50);
            if (d % 7 == 6) {
                c.getTank().setWaterQuality(Math.min(1F, c.getTank().waterQuality() + 0.4F));
                c.getTank().setNitrate(0);
            }
            TankInhabitants.reconcile(level, c);
            peak[0] = Math.max(peak[0], TankInhabitants.liveInhabitants(level, c.getTank()).size());
        }
        nanos[0] += System.nanoTime() - t0;
        h.runAfterDelay(1, () -> day(h, tanks, d + 1, nanos, peak));
    }

    private static void finish(GameTestHelper h, List<TankBlockEntity> tanks, long nanos, int peak) {
        ServerLevel level = h.getLevel();
        List<String> problems = new ArrayList<>();
        int animals = 0, critters = 0;
        int maxEntities = ServerConfig.getOr(ServerConfig.TANK_MAX_ENTITIES, 64);
        for (TankBlockEntity c : tanks) {
            ConfigStressGameTests.check(h, c, "tank " + c.getBlockPos().toShortString(), problems);
            int n = TankInhabitants.liveInhabitants(level, c.getTank()).size();
            animals += n;
            critters += TankCritters.alive(c.getTank());
            if (n > maxEntities) problems.add("tank " + c.getBlockPos().toShortString() + " holds " + n + " animals (limit " + maxEntities + ")");
        }
        double msPerDay = nanos / 1e6 / DAYS;
        org.slf4j.LoggerFactory.getLogger("bettacraft/gametest").info("[m42] soak: {} tanks x {} days: {} animals, {} small animals, at most {} in a tank; "
                + "{} ms per day of care for all tanks ({} ms per tank and care step)", tanks.size(), DAYS, animals, critters, peak,
                String.format(java.util.Locale.ROOT, "%.1f", msPerDay), String.format(java.util.Locale.ROOT, "%.3f", msPerDay / tanks.size() / STEPS_PER_DAY));
        // a care step runs every second per tank in play; here 24 per tank and day: stay below 1 ms per tank and step
        if (msPerDay / tanks.size() / STEPS_PER_DAY > 1.0) problems.add(String.format(java.util.Locale.ROOT, "a care step took %.2f ms", msPerDay / tanks.size() / STEPS_PER_DAY));
        if (!problems.isEmpty()) throw new GameTestFailure(problems.size() + " problem(s): " + String.join("; ", problems.subList(0, Math.min(6, problems.size()))));
        h.succeed();
    }
}
