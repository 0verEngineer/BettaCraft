package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.api.event.AnimalDiedEvent;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaCare;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.betta.OldAge;
import org.overengineer.bettacraft.entity.tankmate.TankmateCare;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M33 part d: death of old age ({@code health.oldAge}): with a lifespan of 0.1 years of 96 days (9.6 days) a fed betta and
 * a neon in good water live on in {@code care.mode = NO_DEATH} past it, then in NORMAL they die of old age: tank log and the
 * KubeJS {@code AnimalDiedEvent} with the cause {@code old_age}. Own batch, the config is restored in {@code finally}.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OldAgeGameTests {
    private static final String EMPTY = "empty_32x10x32";
    private static final List<AnimalDiedEvent> DIED = new ArrayList<>();
    private static boolean listening;

    private OldAgeGameTests() {}

    @GameTest(template = EMPTY, batch = "bettacraft_m33d_old_age", timeoutTicks = 200)
    public static void fishDieOfOldAge(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        boolean on = ServerConfig.HEALTH_OLD_AGE_ENABLED.get();
        double years = ServerConfig.HEALTH_OLD_AGE_YEARS.get();
        int perYear = ServerConfig.HEALTH_OLD_AGE_DAYS_PER_YEAR.get();
        ServerConfig.CareMode mode = ServerConfig.CARE_MODE.get();
        if (!listening) {
            listening = true;
            NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, AnimalDiedEvent.class, DIED::add);
        }
        DIED.clear();
        try {
            ServerConfig.HEALTH_OLD_AGE_ENABLED.set(true);
            ServerConfig.HEALTH_OLD_AGE_YEARS.set(0.1);
            ServerConfig.HEALTH_OLD_AGE_DAYS_PER_YEAR.set(96);
            ServerConfig.CARE_MODE.set(ServerConfig.CareMode.NO_DEATH);
            h.assertTrue(Math.abs(OldAge.lifespanDays() - 9.6) < 1e-9, "lifespan 9.6 days: " + OldAge.lifespanDays());
            TankBlockEntity c = AerationGameTests.tank(h, 4, 1, 4, 4, 3, 4);
            BettaData d = BettaData.create(BettaGenotype.randomDomestic(RandomSource.create(3)), true, 0);
            var err = TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d), ModEntities.BETTA.get(), null, false);
            if (err != null) throw new GameTestAssertException("betta: " + err.getString());
            BettaEntity betta = null;
            for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) if (e instanceof BettaEntity b) betta = b;
            if (betta == null) throw new GameTestAssertException("no betta");
            TankWaterVolume w = new TankWaterVolume(c.getTank());
            TankmateEntity neon = TankmateBreeding.spawn(level, c, TankmateSpecies.NEON_TETRA, TankmateGenes.wild(TankmateSpecies.NEON_TETRA), true,
                    LifeStage.ADULT, 0, w.clamp(w.box().getCenter(), 0.35, 0.1, 0.2));
            if (neon == null) throw new GameTestAssertException("no neon");
            // NO_DEATH: they grow old but live on
            for (int i = 0; i < 40; i++) step(c, betta, neon, level);
            h.assertTrue(betta.isAlive() && neon.isAlive(), "no death of old age in NO_DEATH");
            h.assertTrue(betta.health().ageDays() >= 9.6 && neon.health().ageDays() >= 9.6,
                    "they are past the lifespan: betta " + betta.health().ageDays() + ", neon " + neon.health().ageDays());
            h.assertTrue(betta.health().ageDays() < 9.0 + 20.5, "a grown betta of 1.25 started at the age of its stage (9 days), not older");
            // NORMAL: the next step takes them
            ServerConfig.CARE_MODE.set(ServerConfig.CareMode.NORMAL);
            DIED.clear();
            step(c, betta, neon, level);
            h.assertTrue(!betta.isAlive() && !neon.isAlive(), "both died of old age");
            long events = DIED.stream().filter(e -> OldAge.CAUSE.equals(e.getCause())).count();
            h.assertTrue(events == 2, "two AnimalDiedEvents with the cause old_age: " + events + " of " + DIED.size());
            h.assertTrue(c.getTank().log().stream().anyMatch(l -> l.key().equals("log.bettacraft.died")), "tank log");
            h.assertTrue(c.getTank().inhabitants().isEmpty(), "gone from the tank");
            h.succeed();
        } finally {
            ServerConfig.HEALTH_OLD_AGE_ENABLED.set(on);
            ServerConfig.HEALTH_OLD_AGE_YEARS.set(years);
            ServerConfig.HEALTH_OLD_AGE_DAYS_PER_YEAR.set(perYear);
            ServerConfig.CARE_MODE.set(mode);
        }
    }

    /** Half a day of care, fed, in good water. */
    private static void step(TankBlockEntity c, BettaEntity betta, TankmateEntity neon, ServerLevel level) {
        c.getTank().setWaterQuality(1F);
        c.getTank().setWaterTemp(26F);
        if (betta.isAlive()) {
            betta.health().feed();
            BettaCare.advance(level, c, betta, 0.5);
        }
        if (neon.isAlive()) {
            neon.health().feed();
            TankmateCare.advance(level, c, neon, 0.5);
        }
    }
}
