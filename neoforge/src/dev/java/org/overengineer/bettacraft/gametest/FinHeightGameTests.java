package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.AnimalSize;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.BettaExtents;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.entity.tankmate.TankmateExtents;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * Angelfish fix (1.33.3): tall fish keep their whole rendered body below the water surface. Up to 1.33.2 a tankmate kept
 * only 0.8 of its hitbox below the surface; the angelfish (drawn about 1.1 blocks high, hitbox 0.6) then swam with its
 * dorsal fin out of the water and through the lid (M43 media, the Amazon tank). The fish start right at the surface and
 * swim freely (fixed seeds) for a minute; in no tick may the top of the model ({@link TankmateExtents#top}, checked
 * against the baked models by {@code TankmateExtentsTest}) or of the hitbox be above the surface.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FinHeightGameTests {
    private static final String EMPTY = "empty_32x10x32";
    private static final int TICKS = 1200;
    /** The first ticks settle the fish that start above the line (the tank water is known from their first tick). */
    private static final int SETTLE = 10;

    private FinHeightGameTests() {}

    private static TankmateEntity add(GameTestHelper h, TankBlockEntity c, TankWaterVolume w, TankmateSpecies s, boolean female, double x, long seed) {
        var b = w.box();
        // right at the surface: the old margin would have left the fin above it
        Vec3 p = new Vec3(b.minX + x, w.surface() - 0.1, (b.minZ + b.maxZ) / 2);
        TankmateEntity e = TankmateBreeding.spawn(h.getLevel(), c, s, TankmateGenes.wild(s), female, LifeStage.ADULT, 0, p);
        if (e == null) throw new GameTestAssertException("could not spawn " + s.id());
        e.getRandom().setSeed(seed);
        e.health().feed();
        return e;
    }

    /** Two angelfish, a discus and a pearl gourami (it gulps air at the surface) in a 5 x 2 x 3 tank. */
    @GameTest(template = EMPTY, batch = "bettacraft_fin_height", timeoutTicks = TICKS + 100)
    public static void tallFishKeepTheirFinsInTheWater(GameTestHelper h) {
        TankBlockEntity c = CritterGameTests.tank(h, 2, 1, 2, 5, 2, 3, 5 * 2 * 3 * 1000L);
        c.getTank().setWaterTemp(28F);
        c.getTank().setTargetTemp(28F);
        c.getTank().setWaterQuality(1F);
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        List<TankmateEntity> fish = new ArrayList<>();
        fish.add(add(h, c, w, TankmateSpecies.ANGELFISH, false, 1.0, 4331L));
        fish.add(add(h, c, w, TankmateSpecies.ANGELFISH, true, 2.0, 4332L));
        fish.add(add(h, c, w, TankmateSpecies.DISCUS, false, 3.0, 4333L));
        fish.add(add(h, c, w, TankmateSpecies.PEARL_GOURAMI, false, 4.0, 4334L));
        // the cause: the angelfish model reaches far above its hitbox
        TankmateEntity angel = fish.getFirst();
        h.assertTrue(TankmateExtents.top(angel) > angel.getBbHeight() * 1.5, "angelfish top " + TankmateExtents.top(angel) + " / hitbox " + angel.getBbHeight());
        double surface = w.surface();
        double[] highest = new double[fish.size()];
        java.util.Arrays.fill(highest, -1e9);
        h.onEachTick(() -> {
            long t = h.getTick();
            if (t < SETTLE) return;
            for (int i = 0; i < fish.size(); i++) {
                TankmateEntity e = fish.get(i);
                h.assertTrue(e.isAlive() && e.isInWater(), e.species().id() + " left the water at tick " + t + ": " + e.position());
                double top = e.getY() + TankmateExtents.top(e);
                h.assertTrue(top <= surface + 1e-3, e.species().id() + ": the top of the model " + top + " above the surface " + surface + " at tick " + t);
                h.assertTrue(e.getBoundingBox().maxY <= surface + 1e-3, e.species().id() + ": hitbox above the surface at tick " + t);
                highest[i] = Math.max(highest[i], top);
            }
            if (t >= TICKS) {
                // they really swam up to the surface (else the test would prove nothing)
                for (int i = 0; i < fish.size(); i++) {
                    h.assertTrue(highest[i] > surface - 0.3, fish.get(i).species().id() + " never came near the surface: " + highest[i] + " / " + surface);
                }
                h.succeed();
            }
        });
    }

    /** The bettas at the "Large" size setting (M36) keep their dorsal fin below the surface too (BettaExtents). */
    @GameTest(template = EMPTY, batch = "bettacraft_fin_height_size", timeoutTicks = 700)
    public static void largeBettasKeepTheirDorsalInTheWater(GameTestHelper h) {
        AnimalSize saved = ServerConfig.SIZE_BETTA.get();
        TankBlockEntity c;
        List<BettaEntity> fish = new ArrayList<>();
        try {
            ServerConfig.SIZE_BETTA.set(AnimalSize.LARGE);
            c = CritterGameTests.tank(h, 2, 1, 2, 3, 1, 2, 3 * 1 * 2 * 1000L);
            var p = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            for (int i = 0; i < 3; i++) {
                var g = org.overengineer.bettacraft.genetics.BettaGenome.randomDomestic(net.minecraft.util.RandomSource.create(4340L + i));
                var s = org.overengineer.bettacraft.entity.betta.BettaBucketItem.withFish(new net.minecraft.world.item.ItemStack(org.overengineer.bettacraft.registry.ModItems.BETTA_BUCKET.get()),
                        org.overengineer.bettacraft.entity.betta.BettaData.create(g, i == 1, 0));
                var err = TankInhabitants.insert(h.getLevel(), c, s, TankInhabitants.containedType(s), p, false);
                if (err != null) throw new GameTestAssertException("insert failed: " + err.getString());
            }
            for (var e : TankInhabitants.liveInhabitants(h.getLevel(), c.getTank())) if (e instanceof BettaEntity b) fish.add(b);
            for (int i = 0; i < fish.size(); i++) {
                fish.get(i).refreshDimensions();
                fish.get(i).getRandom().setSeed(4345L + i);
            }
        } catch (RuntimeException e) {
            ServerConfig.SIZE_BETTA.set(saved);
            throw e;
        }
        h.onEachTick(() -> {
            try {
                long t = h.getTick();
                h.assertTrue(fish.size() == 3, "three bettas: " + fish.size());
                if (t < SETTLE) return;
                for (BettaEntity b : fish) {
                    var w = b.tankWater();
                    if (w == null) continue;
                    h.assertTrue(Math.abs(b.sizeScale() - 1F) < 1e-4, "Large draws at 1: " + b.sizeScale());
                    // the push that would bring the outermost points (dorsal tip included) into the water: nothing upwards to fix
                    Vec3 push = BettaExtents.push(w, b.position(), b.yBodyRot, b.genome().shape(), b.sizeScale(), false, 1.0 / 16);
                    h.assertTrue(push.y > -0.01, "dorsal fin above the surface at Large, tick " + t + ": " + push);
                }
                if (t >= 600) {
                    ServerConfig.SIZE_BETTA.set(saved);
                    h.succeed();
                }
            } catch (RuntimeException e) {
                ServerConfig.SIZE_BETTA.set(saved);
                throw e;
            }
        });
    }
}
