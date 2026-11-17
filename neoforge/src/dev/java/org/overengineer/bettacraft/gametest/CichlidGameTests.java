package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.decor.DecorItem;
import org.overengineer.bettacraft.decor.DecorType;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateCare;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tankmates.Clutch;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.tankmates.TankmateSpots;

/**
 * M29 part a: the cichlids in a tank: a ram pair spawns on the flat roof of a slate cave and both guard the brood (an
 * intruding neon is driven off; without the parents the brood dies), an Apistogramma female claims the coconut shell and
 * spawns and guards there alone, adult angelfish eat neons and shrimp, too many ram males fight over their territories.
 * The breeding readiness and the prey of a two-day step are deterministic.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CichlidGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private CichlidGameTests() {}

    private static TankBlockEntity tank(GameTestHelper h, int x, int y, int z, int sx, int sy, int sz, float temp) {
        TankBlockEntity c = CritterGameTests.tank(h, x, y, z, sx, sy, sz, sx * sy * sz * 1000L);
        c.getTank().setWaterTemp(temp);
        c.getTank().setTargetTemp(temp);
        c.getTank().setWaterQuality(1F);
        return c;
    }

    private static TankmateEntity add(GameTestHelper h, TankBlockEntity c, TankmateSpecies s, boolean female) {
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        var b = w.box();
        Vec3 p = w.clamp(new Vec3((b.minX + b.maxX) / 2, b.minY + 0.5, (b.minZ + b.maxZ) / 2), 0.35, 0.1, 0.2);
        TankmateEntity e = TankmateBreeding.spawn(h.getLevel(), c, s, TankmateGenes.wild(s), female, LifeStage.ADULT, 0, p);
        if (e == null) throw new GameTestAssertException("could not spawn " + s.id());
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

    private static Clutch clutch(TankData t, Clutch.Kind kind) {
        for (Clutch k : t.clutches()) if (k.kind == kind) return k;
        return null;
    }

    private static boolean logged(TankData t, String key) {
        return t.log().stream().anyMatch(e -> e.key().equals(key));
    }

    private static InteriorRegion interior(GameTestHelper h, TankBlockEntity c) {
        if (InteriorManager.interiorLevel(h.getLevel().getServer()) == null) throw new GameTestAssertException("no interior dimension");
        InteriorRegion r = InteriorManager.ensureRegion(h.getLevel(), c);
        ServerLevel in = InteriorManager.interiorLevel(h.getLevel().getServer());
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) {
            Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.SAND.defaultBlockState(), false);
        }
        return r;
    }

    private static void decor(GameTestHelper h, InteriorRegion r, DecorType type, int lx, int lz) {
        ServerLevel in = InteriorManager.interiorLevel(h.getLevel().getServer());
        var err = DecorItem.place(in, type, new BlockPos(r.originX() + lx, InteriorRegion.BASE_Y, r.originZ() + lz), Rotation.NONE, null);
        if (err != null) throw new GameTestAssertException("decor " + type + ": " + err.getString());
    }

    private static void remove(TankBlockEntity c, TankmateEntity e) {
        c.getTank().inhabitants().remove(e.getUUID());
        e.discard();
    }

    /** A ram pair spawns on the flat roof of a slate cave, both guard, a neon at the brood is driven off; without them the brood dies. */
    @GameTest(template = EMPTY, batch = "bettacraft_cichlid_defence", timeoutTicks = 300)
    public static void ramPairSpawnsOnAFlatStoneAndGuards(GameTestHelper h) {
        double defence = org.overengineer.bettacraft.config.ServerConfig.TANKMATES_BROOD_DEFENCE.get();
        try {
            org.overengineer.bettacraft.config.ServerConfig.TANKMATES_BROOD_DEFENCE.set(1.0);
            ramPair(h);
        } finally {
            org.overengineer.bettacraft.config.ServerConfig.TANKMATES_BROOD_DEFENCE.set(defence);
        }
    }

    private static void ramPair(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 2, 2, 3, 2, 3, 28F);
        InteriorRegion r = interior(h, c);
        decor(h, r, DecorType.SLATE_CAVE, 18, 18);
        TankmateEntity male = add(h, c, TankmateSpecies.RAM_CICHLID, false);
        TankmateEntity female = add(h, c, TankmateSpecies.RAM_CICHLID, true);
        TankData t = c.getTank();
        for (int i = 0; i < 12 && clutch(t, Clutch.Kind.OPEN) == null; i++) days(h, c, 0.25, 0.25, 28F);
        Clutch k = clutch(t, Clutch.Kind.OPEN);
        h.assertTrue(k != null, "the pair spawned: " + t.clutches().size());
        h.assertTrue(male.getUUID().equals(k.guardian) && female.getUUID().equals(k.partner), "both parents guard");
        h.assertTrue(k.y > t.min().getY() + 0.2, "on the raised slate, not the floor: " + k.y);
        h.assertTrue(logged(t, "log.bettacraft.tm.spawned_open"), "logged");
        days(h, c, 0.1, 0.1, 28F);
        h.assertTrue(male.isGuarding() && female.isGuarding(), "they stay at the brood");
        // a neon swims up to the brood: chased off and bitten
        TankmateEntity neon = add(h, c, TankmateSpecies.NEON_TETRA, false);
        neon.moveTo(k.x, k.y + 0.1, k.z);
        float hp = neon.getHealth();
        int before = k.count;
        TankmateBreeding.step(h.getLevel(), c, 1.0); // tankmates.behaviour.broodDefence 1: one hit point a day
        h.assertTrue((neon.stressBits() & TankmateCare.STRESS_CHASED) != 0, "the intruder is chased");
        h.assertTrue(neon.getHealth() < hp, "and bitten");
        h.assertTrue(k.count == before, "guarded eggs are safe");
        h.assertTrue(logged(t, "log.bettacraft.tm.guards_brood"), "logged the guarding");
        remove(c, neon);
        // without both parents the brood dies
        remove(c, male);
        remove(c, female);
        days(h, c, 1.0, 0.25, 28F);
        h.assertTrue(clutch(t, Clutch.Kind.OPEN) == null, "the brood died without its parents");
        h.assertTrue(logged(t, "log.bettacraft.tm.brood_died"), "logged");
        h.succeed();
    }

    /** A cockatoo female claims the coconut shell, spawns in it with the male and guards alone; the eggs hatch into fry. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void apistogrammaFemaleSpawnsInTheCoconutShell(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity none = tank(h, 2, 2, 2, 2, 2, 2, 26F);
        TankBlockEntity c = tank(h, 8, 2, 2, 3, 2, 3, 26F);
        InteriorRegion r = interior(h, c);
        decor(h, r, DecorType.COCONUT_CAVE, 20, 20);
        var caves = TankmateSpots.cichlidCaves(level.getServer(), c.getTank());
        h.assertTrue(caves.size() == 1, "the coconut shell is a small cave: " + caves.size());
        h.assertTrue(TankmateSpots.caves(level.getServer(), c.getTank()).isEmpty(), "but no pleco cave");
        for (TankBlockEntity t : List.of(none, c)) {
            add(h, t, TankmateSpecies.COCKATOO_CICHLID, false);
            add(h, t, TankmateSpecies.COCKATOO_CICHLID, true);
        }
        days(h, none, 3, 0.25, 26F);
        h.assertTrue(none.getTank().clutches().isEmpty(), "no cave, no brood");
        TankData t = c.getTank();
        for (int i = 0; i < 12 && clutch(t, Clutch.Kind.CAVE) == null; i++) days(h, c, 0.25, 0.25, 26F);
        Clutch k = clutch(t, Clutch.Kind.CAVE);
        h.assertTrue(k != null, "a brood in the shell");
        TankmateEntity female = mates(h, c, TankmateSpecies.COCKATOO_CICHLID).stream().filter(TankmateEntity::isFemale).findFirst().orElseThrow();
        h.assertTrue(female.getUUID().equals(k.guardian), "the female guards");
        h.assertTrue(k.pos().distanceTo(caves.getFirst().pos()) < 1e-3, "in the shell");
        h.assertTrue(logged(t, "log.bettacraft.tm.spawned_cichlid_cave"), "logged");
        days(h, c, 2.0, 0.25, 26F);
        long fry = mates(h, c, TankmateSpecies.COCKATOO_CICHLID).stream().filter(m -> m.lifeStage() == LifeStage.FRY).count();
        h.assertTrue(fry > 0, "the young swim free: " + fry);
        h.succeed();
    }

    /** Four adult angelfish eat neons and shrimp (two-day steps: at least one prey per step), not the bigger fish. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void angelfishEatSmallFishAndShrimp(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 2, 2, 3, 3, 3, 26F);
        for (int i = 0; i < 4; i++) add(h, c, TankmateSpecies.ANGELFISH, i % 2 == 0);
        for (int i = 0; i < 6; i++) add(h, c, TankmateSpecies.NEON_TETRA, i % 2 == 0);
        TankmateEntity platy = add(h, c, TankmateSpecies.PLATY, true);
        for (int i = 0; i < 6; i++) TankCritters.add(level, c, CritterSpecies.NEOCARIDINA, CritterGenes.WILD, i % 2 == 0, 10);
        int before = mates(h, c, TankmateSpecies.NEON_TETRA).size() + c.getTank().critters().size();
        for (int i = 0; i < 3; i++) {
            for (TankmateEntity m : mates(h, c, null)) m.health().feed();
            TankmateBreeding.step(level, c, 2.0);
        }
        int after = mates(h, c, TankmateSpecies.NEON_TETRA).size() + c.getTank().critters().size();
        h.assertTrue(after <= before - 3, "neons and shrimp eaten: " + before + " -> " + after);
        h.assertTrue(platy.isAlive(), "a platy is too big");
        h.assertTrue(logged(c.getTank(), "log.bettacraft.tm.ate_prey"), "logged");
        h.succeed();
    }

    /** Three ram males in a tank with room for two territories: the third is chased and bitten. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void tooManyRamMalesFight(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 2, 2, 2, 2, 2, 28F);
        for (int i = 0; i < 3; i++) add(h, c, TankmateSpecies.RAM_CICHLID, false);
        TankmateBreeding.step(h.getLevel(), c, 0.1);
        long chased = mates(h, c, TankmateSpecies.RAM_CICHLID).stream().filter(m -> (m.stressBits() & TankmateCare.STRESS_CHASED) != 0).count();
        h.assertTrue(chased == 1, "one male without a territory: " + chased);
        h.assertTrue(logged(c.getTank(), "log.bettacraft.tm.territory"), "logged");
        // a bigger tank has room for all three
        TankBlockEntity big = tank(h, 8, 2, 2, 3, 2, 3, 28F);
        for (int i = 0; i < 3; i++) add(h, big, TankmateSpecies.RAM_CICHLID, false);
        TankmateBreeding.step(h.getLevel(), big, 0.1);
        h.assertTrue(mates(h, big, TankmateSpecies.RAM_CICHLID).stream().noneMatch(m -> (m.stressBits() & TankmateCare.STRESS_CHASED) != 0), "room for all");
        h.succeed();
    }
}
