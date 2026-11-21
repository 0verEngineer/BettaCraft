package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import org.overengineer.bettacraft.util.NbtCompat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateCare;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.entity.tankmate.TankmateExtents;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tankmates.Clutch;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateItems;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.world.TankmateTrades;
import org.overengineer.bettacraft.world.Wildlife;

/**
 * M48 part a: the bred forms of the tetras and corydoras in real fish (a gold x albino neon cross through the tank's breeding,
 * the longfin drawn with long fins, wild fish and trader fish carry the forms hidden), the bronze cory (an albino pair spawns
 * on the glass after a cool water change, the fry are albino), the panda garra (net, trader, spawning, grazing), and the switch
 * {@code features.tankmates.tetraCoryForms} off (no forms from the trader, the wild and mutations).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TetraCoryFormGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private TetraCoryFormGameTests() {}

    private static void check(boolean ok, String what) {
        if (!ok) throw new GameTestFailure(what);
    }

    private static TankBlockEntity tank(GameTestHelper h, int x, int z, int sx, int sz, float temp) {
        TankBlockEntity c = CritterGameTests.tank(h, x, 2, z, sx, 2, sz, sx * 2L * sz * 1000L);
        c.getTank().setWaterTemp(temp);
        c.getTank().setTargetTemp(temp);
        c.getTank().setWaterQuality(1F);
        return c;
    }

    private static TankmateEntity add(GameTestHelper h, TankBlockEntity c, TankmateSpecies s, TankmateGenes g, boolean female) {
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        var r = h.getLevel().getRandom();
        Vec3 p = w.clamp(new Vec3(w.box().minX + r.nextDouble() * (w.box().maxX - w.box().minX), w.box().minY + 0.2,
                w.box().minZ + r.nextDouble() * (w.box().maxZ - w.box().minZ)), 0.35, 0.1, 0.2);
        TankmateEntity e = TankmateBreeding.spawn(h.getLevel(), c, s, g, female, LifeStage.ADULT, 0, p);
        if (e == null) throw new GameTestFailure("could not spawn " + s.id());
        e.health().feed();
        return e;
    }

    private static List<TankmateEntity> mates(GameTestHelper h, TankBlockEntity c, TankmateSpecies s) {
        List<TankmateEntity> out = new ArrayList<>();
        for (TankmateEntity m : TankmateBreeding.tankmates(h.getLevel(), c.getTank())) if (s == null || m.species() == s) out.add(m);
        return out;
    }

    private static void days(GameTestHelper h, TankBlockEntity c, double days, double step, float temp) {
        for (double d = 0; d < days - 1e-9; d += step) {
            for (TankmateEntity m : mates(h, c, null)) m.health().feed();
            c.getTank().setWaterTemp(temp);
            c.getTank().setWaterQuality(1F);
            for (TankmateEntity m : mates(h, c, null)) TankmateCare.advance(h.getLevel(), c, m, step);
            TankmateBreeding.step(h.getLevel(), c, step);
        }
    }

    private static boolean logged(TankData t, String key) {
        return t.log().stream().anyMatch(e -> e.key().equals(key));
    }

    private static void removeAdults(GameTestHelper h, TankBlockEntity c) {
        for (TankmateEntity m : mates(h, c, null)) {
            if (m.lifeStage() != LifeStage.ADULT) continue;
            c.getTank().inhabitants().remove(m.getUUID());
            m.discard();
        }
    }

    /** A cool water change (like the sterbai test): a quarter or more of the water fresh, the tank at least 1.5 °C cooler. */
    private static float coolChange(GameTestHelper h, TankBlockEntity c) {
        ServerLevel level = h.getLevel();
        TankData t = c.getTank();
        float start = Math.max(27.5F, TankCare.ambient(level, t) + 4F);
        t.setWaterTemp(start);
        long half = t.waterMb() / 2;
        TankInteractions.changeWater(level, c, -half);
        TankInteractions.changeWater(level, c, half);
        check(TankmateBreeding.coolChangeRecent(level, t), "a cool water change (" + start + " -> " + t.waterTemp() + ")");
        return t.waterTemp();
    }

    // ------------------------------------------------------------------ the bronze cory

    /**
     * Six albino bronze corys spawn on the glass after a cool water change; the fry of the albinos are albino. M54: in a batch of
     * its own with the mutations off (they were on: at 0.3 % per gene a young one now and then carried a mutated gene and
     * showed it, "17 of 19" against the 90 % of the check), so every young one must be albino.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "bettacraft_m54_albino_cory")
    public static void albinoBronzeCorysSpawnAfterACoolWaterChange(GameTestHelper h) {
        GenerationsGameTests.withConfig(() -> albinoBronzeCorys(h));
    }

    private static void albinoBronzeCorys(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 2, 2, 2, 26F);
        TankmateSpecies s = TankmateSpecies.BRONZE_CORY;
        for (int i = 0; i < 6; i++) add(h, c, s, TankmateGenes.pure(s, "albino"), i % 2 == 0);
        for (TankmateEntity m : mates(h, c, s)) check(m.variety().equals("albino"), "albino shows: " + m.variety());
        check(s.texture("albino", false).equals("bronze_cory_albino"), "texture of the form");
        TankData t = c.getTank();
        days(h, c, 1, 0.25, 26F);
        check(t.clutches().isEmpty(), "no eggs without a water change");
        coolChange(h, c);
        // held at 24 C afterwards (inside the breeding range 22-26 C, whatever the ambient temperature of the test spot)
        for (int i = 0; i < 30 && t.clutches().stream().noneMatch(k -> k.kind == Clutch.Kind.GLASS); i++) days(h, c, 0.1, 0.1, 24F);
        check(t.clutches().stream().anyMatch(k -> k.kind == Clutch.Kind.GLASS), "eggs on the glass: " + t.clutches().size());
        removeAdults(h, c);
        days(h, c, 2.5, 0.25, 25F);
        List<TankmateEntity> fry = mates(h, c, s);
        check(!fry.isEmpty(), "fry swim free, clutches left " + t.clutches().size());
        for (TankmateEntity m : fry) check(m.genes().pure() && m.genes().carries("albino"), "albino parents give albino young: " + m.genes().text());
        long albino = fry.stream().filter(m -> m.variety().equals("albino")).count();
        check(albino == fry.size(), "the young are albino: " + albino + " of " + fry.size());
        h.succeed();
    }

    // ------------------------------------------------------------------ the forms in the tank

    /**
     * A gold neon male and an albino female in a tank: their eggs (scattered on the bare ground) hatch into gold young that carry
     * albino; the longfin panda cory is drawn with longer fins (and reaches higher), the albino glowlight shows its form.
     */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void neonFormsCrossInTheTank(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 2, 3, 2, 24F);
        TankmateSpecies neon = TankmateSpecies.NEON_TETRA;
        // three of each: enough females for a spawning within the test (0.6 a day each)
        for (int i = 0; i < 3; i++) add(h, c, neon, TankmateGenes.pure(neon, "gold"), false);
        for (int i = 0; i < 3; i++) add(h, c, neon, TankmateGenes.pure(neon, "albino"), true);
        TankData t = c.getTank();
        for (int i = 0; i < 150 && t.clutches().isEmpty(); i++) {
            for (TankmateEntity m : mates(h, c, null)) m.health().feed();
            t.setWaterTemp(24F);
            TankmateBreeding.step(h.getLevel(), c, 0.1);
        }
        check(!t.clutches().isEmpty(), "the neons spawned");
        Clutch k = t.clutches().getFirst();
        check(k.mother.carries("albino") && k.father.carries("gold"), "the clutch has the genes of the parents: " + k.mother.text() + " x " + k.father.text());
        k.cover = 1.0; // the parents would eat the eggs on the bare ground
        removeAdults(h, c);
        days(h, c, 2.0, 0.25, 24F);
        List<TankmateEntity> fry = mates(h, c, neon);
        check(!fry.isEmpty(), "fry hatched");
        long gold = fry.stream().filter(m -> m.variety().equals("gold") && m.genes().carries("albino")).count();
        check(gold >= fry.size() * 0.9, "gold young carrying albino: " + gold + " of " + fry.size());
        // the longfin panda cory and the albino glowlight
        TankmateEntity longfin = add(h, c, TankmateSpecies.PANDA_CORY, TankmateGenes.pure(TankmateSpecies.PANDA_CORY, "longfin"), false);
        TankmateEntity wild = add(h, c, TankmateSpecies.PANDA_CORY, TankmateGenes.wild(TankmateSpecies.PANDA_CORY), true);
        check(TankmateExtents.finScale(longfin) > 1.5F && TankmateExtents.finScale(wild) == 1F, "the longfin has long fins");
        check(TankmateExtents.top(longfin) > TankmateExtents.top(wild) * 0.99, "its fins reach higher");
        TankmateEntity glow = add(h, c, TankmateSpecies.GLOWLIGHT_TETRA, TankmateGenes.pure(TankmateSpecies.GLOWLIGHT_TETRA, "albino"), true);
        check(glow.variety().equals("albino"), "albino glowlight");
        // an old neon (saved before M48 with its single variety) loads as wild type
        TankmateEntity old = add(h, c, neon, new TankmateGenes("wild", "wild"), true);
        CompoundTag tag = new CompoundTag();
        tag.merge(NbtCompat.saveEntityWithoutId(old));
        TankmateEntity loaded = ModEntities.TANKMATES.get(neon).get().create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        org.overengineer.bettacraft.util.NbtCompat.loadEntity(loaded, tag);
        check(loaded.variety().equals("wild") && neon.texture(loaded.variety(), true).equals("neon_tetra_wild"), "the old neon loads wild");
        loaded.discard();
        h.succeed();
    }

    /** Wild neons (spawns, the net) and the wild-type neons of the trader carry every form hidden; the trader sells forms openly too. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void wildAndTraderNeonsCarryTheForms(GameTestHelper h) {
        var r = h.getLevel().getRandom();
        Set<String> carried = new HashSet<>(), shown = new HashSet<>();
        for (int i = 0; i < 6000; i++) {
            TankmateGenes g = Wildlife.tankmate(TankmateSpecies.NEON_TETRA, r::nextLong);
            if (g.pure()) shown.add(g.phenotype(TankmateSpecies.NEON_TETRA));
            else carried.add(g.a().equals("wild") ? g.b() : g.a());
        }
        check(carried.containsAll(List.of("diamond", "gold", "albino")), "wild neons carry the forms hidden: " + carried);
        Set<String> sold = new HashSet<>();
        for (int i = 0; i < 300; i++) {
            var offer = TankmateTrades.offer(TankmateSpecies.CARDINAL_TETRA, r::nextLong);
            for (CompoundTag f : TankmateItems.bagFish(offer.getResult())) {
                TankmateGenes g = TankmateGenes.load(f.getCompoundOrEmpty(TankmateEntity.KEY).getCompoundOrEmpty("genes"), TankmateSpecies.CARDINAL_TETRA);
                sold.add(g.phenotype(TankmateSpecies.CARDINAL_TETRA) + (g.carries("gold") ? "+gold" : ""));
            }
        }
        check(sold.contains("gold+gold") && sold.contains("wild+gold"), "the trader sells gold cardinals and wild carriers: " + sold);
        h.succeed();
    }

    // ------------------------------------------------------------------ the panda garra

    /** The panda garra comes from the net in rivers and from the trader, scatters its eggs and grazes algae. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void pandaGarraFromTheNetSpawnsAndGrazes(GameTestHelper h) {
        var river = NetCatching.fishTable(NetCatching.Water.RIVER);
        check(river.getOrDefault(TankmateSpecies.PANDA_GARRA, 0.0) > 0 && river.getOrDefault(TankmateSpecies.BRONZE_CORY, 0.0) > 0,
                "both in the net in rivers");
        check(NetCatching.fishTable(NetCatching.Water.JUNGLE).getOrDefault(TankmateSpecies.BRONZE_CORY, 0.0) > 0, "the bronze cory in jungle water");
        var r = h.getLevel().getRandom();
        var offer = TankmateTrades.offer(TankmateSpecies.PANDA_GARRA, r::nextLong);
        check(TankmateItems.bagSpecies(offer.getResult()) == TankmateSpecies.PANDA_GARRA, "the trader sells panda garras");
        TankBlockEntity c = tank(h, 2, 2, 2, 2, 23F);
        TankmateSpecies s = TankmateSpecies.PANDA_GARRA;
        for (int i = 0; i < 6; i++) add(h, c, s, TankmateGenes.wild(s), i % 2 == 0);
        TankData t = c.getTank();
        for (int i = 0; i < 150 && t.clutches().isEmpty() && !logged(t, "log.bettacraft.tm.spawned_bare"); i++) {
            for (TankmateEntity m : mates(h, c, null)) m.health().feed();
            t.setWaterTemp(23F);
            TankmateBreeding.step(h.getLevel(), c, 0.1);
        }
        check(logged(t, "log.bettacraft.tm.spawned_bare") || logged(t, "log.bettacraft.tm.spawned_scatter"), "the panda garras spawned");
        // grazing: they count for the algae like the otocinclus
        TankmateBreeding.step(h.getLevel(), c, 0.05);
        check(t.fishGrazing() > 0, "panda garras graze: " + t.fishGrazing());
        h.succeed();
    }

    // ------------------------------------------------------------------ the switch

    /** {@code features.tankmates.tetraCoryForms} off: wild neons, trader neons and mutations bring no forms; on again they do. */
    @GameTest(template = EMPTY, batch = "bettacraft_m48_forms_off", timeoutTicks = 100)
    public static void formsOffBringOnlyTheWildType(GameTestHelper h) {
        var r = h.getLevel().getRandom();
        Features.set(Feature.TANKMATE_FORMS, false);
        try {
            for (int i = 0; i < 3000; i++) {
                TankmateGenes g = Wildlife.tankmate(TankmateSpecies.NEON_TETRA, r::nextLong);
                check(g.a().equals("wild") && g.b().equals("wild"), "a wild neon with a form: " + g.text());
                TankmateGenes m = Wildlife.mutate(TankmateSpecies.PANDA_CORY, TankmateGenes.wild(TankmateSpecies.PANDA_CORY), r::nextLong);
                check(m.pure() && m.a().equals("wild"), "a mutation to a form: " + m.text());
            }
            for (int i = 0; i < 200; i++) {
                var offer = TankmateTrades.offer(TankmateSpecies.STERBAI_CORY, r::nextLong);
                for (CompoundTag f : TankmateItems.bagFish(offer.getResult())) {
                    TankmateGenes g = TankmateGenes.load(f.getCompoundOrEmpty(TankmateEntity.KEY).getCompoundOrEmpty("genes"), TankmateSpecies.STERBAI_CORY);
                    check(!g.carries("albino"), "the trader sells an albino sterbai with the switch off");
                }
            }
            // other species keep their forms (rams)
            check(TankmateSpecies.RAM_CICHLID.randomBred(r::nextLong) != null, "the ram forms stay");
        } finally {
            Features.set(Feature.TANKMATE_FORMS, true);
        }
        check(TankmateSpecies.NEON_TETRA.randomBred(r::nextLong) != null, "on again: forms come");
        h.succeed();
    }
}
