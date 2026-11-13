package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.decor.DecorItem;
import org.overengineer.bettacraft.decor.DecorType;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateCare;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tank.menu.TankView;
import org.overengineer.bettacraft.tankmates.Clutch;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateItems;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.tankmates.TankmateSpots;
import org.overengineer.bettacraft.world.TankmateTrades;

/**
 * M19: the tankmates in a tank: putting a shoal in from a fish bag, staying in the water, stocking, shoal and
 * temperature stress, every breeding mode with accelerated time (eggs on the glass after a cool water change, a pleco
 * cave with the guarding male, egg scatterers with and without a spawning mop, livebearers with and without the breeding
 * box, the gourami's foam nest), fry eaten by bettas, male bettas chasing guppy males, clown loaches eating snails,
 * algae grazers, algae wafers, the net in the jungle, the trader, a clutch moved with the net, the menu and the break rule.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TankmateGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private TankmateGameTests() {}

    // ------------------------------------------------------------------ helpers

    private static TankBlockEntity tank(GameTestHelper h, int x, int y, int z, int sx, int sy, int sz, long water, float temp) {
        TankBlockEntity c = CritterGameTests.tank(h, x, y, z, sx, sy, sz, water);
        c.getTank().setWaterTemp(temp);
        c.getTank().setTargetTemp(temp);
        c.getTank().setWaterQuality(1F);
        return c;
    }

    private static TankmateEntity add(GameTestHelper h, TankBlockEntity c, TankmateSpecies s, boolean female) {
        return add(h, c, s, TankmateGenes.wild(s), female, LifeStage.ADULT);
    }

    private static TankmateEntity add(GameTestHelper h, TankBlockEntity c, TankmateSpecies s, TankmateGenes g, boolean female, LifeStage stage) {
        var w = new org.overengineer.bettacraft.tank.TankWaterVolume(c.getTank());
        var r = h.getLevel().getRandom();
        Vec3 p = w.clamp(new Vec3(w.box().minX + r.nextDouble() * (w.box().maxX - w.box().minX), w.box().minY + 0.2,
                w.box().minZ + r.nextDouble() * (w.box().maxZ - w.box().minZ)), 0.35, 0.1, 0.2);
        TankmateEntity e = TankmateBreeding.spawn(h.getLevel(), c, s, g, female, stage, 0, p);
        if (e == null) throw new GameTestAssertException("could not spawn " + s.id());
        e.health().feed();
        return e;
    }

    private static List<TankmateEntity> mates(GameTestHelper h, TankBlockEntity c, TankmateSpecies s) {
        List<TankmateEntity> out = new ArrayList<>();
        for (TankmateEntity m : TankmateBreeding.tankmates(h.getLevel(), c.getTank())) if (s == null || m.species() == s) out.add(m);
        return out;
    }

    private static void feedAll(GameTestHelper h, TankBlockEntity c) {
        for (TankmateEntity m : mates(h, c, null)) m.health().feed();
    }

    /** {@code days} of the tank step in steps of {@code step} days, everyone fed, the temperature held. */
    private static void days(GameTestHelper h, TankBlockEntity c, double days, double step, float temp) {
        for (double d = 0; d < days - 1e-9; d += step) {
            feedAll(h, c);
            c.getTank().setWaterTemp(temp);
            c.getTank().setWaterQuality(1F);
            for (TankmateEntity m : mates(h, c, null)) TankmateCare.advance(h.getLevel(), c, m, step);
            TankmateBreeding.step(h.getLevel(), c, step);
        }
    }

    private static int clutches(TankData t, Clutch.Kind kind) {
        int n = 0;
        for (Clutch k : t.clutches()) if (kind == null || k.kind == kind) n++;
        return n;
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

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        sp.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        sp.getInventory().clearContent();
        sp.getInventory().selected = 8;
        return sp;
    }

    private static void use(ServerPlayer sp, BlockPos pos, ItemStack stack) {
        sp.setItemInHand(InteractionHand.MAIN_HAND, stack);
        Vec3 hit = Vec3.atCenterOf(pos).add(0, 0, 0.5);
        sp.gameMode.useItemOn(sp, sp.serverLevel(), stack, InteractionHand.MAIN_HAND, new BlockHitResult(hit, Direction.SOUTH, pos, false));
    }

    // ------------------------------------------------------------------ in the tank

    /** A shoal from one fish bag: all six go in, swim in the water, count for the stocking; the bag comes back empty. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void shoalFromABagSwimsInTheTank(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 2, 2, 3, 2, 2, 12000, 24F);
        ServerPlayer sp = player(h);
        ItemStack bag = TankmateItems.groupBag(TankmateSpecies.NEON_TETRA, 6, () -> TankmateGenes.wild(TankmateSpecies.NEON_TETRA),
                new java.util.Random(1)::nextLong, true);
        h.assertTrue(bag.getHoverName().getString().contains("6"), "bag name with the count: " + bag.getHoverName().getString());
        use(sp, c.getBlockPos(), bag);
        List<TankmateEntity> neons = mates(h, c, TankmateSpecies.NEON_TETRA);
        h.assertTrue(neons.size() == 6, "six neons in: " + neons.size());
        ItemStack hand = sp.getMainHandItem();
        h.assertTrue(hand.getItem() instanceof FishBagItem && !FishBagItem.isFilled(hand), "empty bag back: " + hand);
        double stock = TankInhabitants.stocking(h.getLevel(), c.getTank());
        h.assertTrue(Math.abs(stock - 6 * TankmateSpecies.NEON_TETRA.stock()) < 1e-6, "stocking " + stock);
        int females = 0;
        for (TankmateEntity m : neons) if (m.isFemale()) females++;
        h.assertTrue(females == 3, "three females, three males: " + females);
        h.runAfterDelay(200, () -> {
            for (TankmateEntity m : mates(h, c, TankmateSpecies.NEON_TETRA)) {
                h.assertTrue(m.isInTank() && m.isInWater(), "neon in the tank water at " + m.position());
                h.assertTrue(c.getTank().contains(m.blockPosition()), "inside the tank blocks " + m.blockPosition());
            }
            h.assertTrue(TankInhabitants.breakBlockedReason(h.getLevel(), c.getBlockPos()).isPresent(), "fish keep the tank");
            h.succeed();
        });
    }

    /** Two neons are a shoal too small (stressed, sick after a week); six are fine. A panda cory at 28 C is too warm. (At their spawning temperature two neons would breed their way out of it: the young count.) */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void smallShoalsAndWrongTemperaturesStress(GameTestHelper h) {
        // 26 C: neons live there but do not spawn (22-25 C), so the shoals keep their size
        TankBlockEntity small = tank(h, 2, 2, 2, 2, 2, 2, 8000, 26F);
        TankBlockEntity big = tank(h, 8, 2, 2, 2, 2, 2, 8000, 26F);
        TankBlockEntity warm = tank(h, 14, 2, 2, 2, 2, 2, 8000, 28F);
        for (int i = 0; i < 2; i++) add(h, small, TankmateSpecies.NEON_TETRA, i % 2 == 0);
        for (int i = 0; i < 6; i++) add(h, big, TankmateSpecies.NEON_TETRA, i % 2 == 0);
        for (int i = 0; i < 6; i++) add(h, warm, TankmateSpecies.PANDA_CORY, i % 2 == 0);
        days(h, small, 8, 0.25, 26F);
        days(h, big, 8, 0.25, 26F);
        days(h, warm, 8, 0.25, 28F);
        for (TankmateEntity m : mates(h, small, null)) {
            h.assertTrue((m.stressBits() & TankmateCare.STRESS_SHOAL) != 0, "two neons: shoal too small: stress " + m.stressBits() + " shoal " + m.shoalSize()
                    + " stage " + m.lifeStage() + " n=" + mates(h, small, null).size());
            h.assertTrue(m.health().isSick(), "sick after 8 days alone");
        }
        for (TankmateEntity m : mates(h, big, null)) {
            h.assertTrue(m.stressBits() == 0, "six neons are fine: " + m.stressBits());
            h.assertFalse(m.health().isSick(), "six neons stay healthy");
        }
        for (TankmateEntity m : mates(h, warm, null)) h.assertTrue((m.stressBits() & TankmateCare.STRESS_TEMP) != 0, "panda cory too warm at 28 C");
        h.assertTrue(logged(small.getTank(), "log.bettacraft.tm.shoal_small"), "the log warns about the small shoal");
        h.assertTrue(TankmateBreeding.warnings(h.getLevel(), small).contains("tm_shoal"), "menu warning");
        h.assertTrue(TankmateBreeding.warnings(h.getLevel(), warm).contains("tm_temp"), "temperature warning");
        h.succeed();
    }

    // ------------------------------------------------------------------ breeding

    /** Sterbai corys: a cool water change (half the water, 2.5 C colder) sets them off; eggs on the glass hatch into fry. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void corydorasSpawnOnTheGlassAfterACoolWaterChange(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 2, 2, 2, 2, 2, 8000, 29.5F);
        for (int i = 0; i < 6; i++) add(h, c, TankmateSpecies.STERBAI_CORY, i % 2 == 0);
        TankData t = c.getTank();
        days(h, c, 1, 0.25, 29.5F);
        h.assertTrue(clutches(t, null) == 0, "no eggs without a water change");
        float ambient = TankCare.ambient(level, t);
        t.setWaterTemp(29.5F);
        TankInteractions.changeWater(level, c, -4000);
        TankInteractions.changeWater(level, c, 4000);
        float after = t.waterTemp();
        h.assertTrue(ambient < 27.5F, "the test world is cooler than the tank: " + ambient);
        h.assertTrue(after < 29.5F - 1.5F, "the fresh water cooled the tank: " + after);
        h.assertTrue(TankmateBreeding.coolChangeRecent(level, t), "a cool water change");
        h.assertTrue(logged(t, "log.bettacraft.tm.cool_change"), "logged");
        for (int i = 0; i < 12 && clutches(t, Clutch.Kind.GLASS) == 0; i++) days(h, c, 0.1, 0.1, after);
        h.assertTrue(clutches(t, Clutch.Kind.GLASS) > 0, "eggs glued on: " + t.clutches().size());
        boolean onGlass = t.clutches().stream().anyMatch(k -> k.face >= 0);
        h.assertTrue(onGlass, "some on the glass");
        int eggs = t.clutches().stream().mapToInt(k -> k.count).sum();
        h.assertTrue(eggs >= TankmateSpecies.STERBAI_CORY.broodMin() / 2, "a brood of eggs: " + eggs);
        // raised without the adults: the eggs hatch, then the larvae swim free as fry
        for (TankmateEntity m : mates(h, c, null)) {
            t.inhabitants().remove(m.getUUID());
            m.discard();
        }
        days(h, c, 2.0, 0.25, 26F);
        long fry = mates(h, c, TankmateSpecies.STERBAI_CORY).stream().filter(m -> m.lifeStage() == LifeStage.FRY).count();
        h.assertTrue(fry > 0, "fry swim free: " + fry + " clutches left " + t.clutches().size());
        h.assertTrue(logged(t, "log.bettacraft.tm.fry_free"), "logged");
        h.succeed();
    }

    /** Bristlenoses need a spawning cave: the male takes it, the pair spawns there, he guards; without him the eggs die. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void plecosSpawnInACaveAndTheMaleGuards(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity none = tank(h, 2, 2, 2, 2, 2, 2, 8000, 25F);
        TankBlockEntity cave = tank(h, 8, 2, 2, 2, 2, 2, 8000, 25F);
        InteriorRegion r = interior(h, cave);
        decor(h, r, DecorType.SPAWNING_CAVE, 10, 12);
        var caves = TankmateSpots.caves(level.getServer(), cave.getTank());
        h.assertTrue(caves.size() == 1, "one spawning cave found: " + caves.size());
        h.assertTrue(cave.getTank().contains(BlockPos.containing(caves.getFirst().pos())), "the cave spot lies in the tank");
        for (TankBlockEntity c : List.of(none, cave)) {
            add(h, c, TankmateSpecies.BRISTLENOSE_PLECO, false);
            add(h, c, TankmateSpecies.BRISTLENOSE_PLECO, true);
        }
        days(h, none, 4, 0.25, 25F);
        h.assertTrue(clutches(none.getTank(), null) == 0, "no cave, no eggs");
        for (int i = 0; i < 30 && clutches(cave.getTank(), Clutch.Kind.CAVE) == 0; i++) days(h, cave, 0.25, 0.25, 25F);
        h.assertTrue(clutches(cave.getTank(), Clutch.Kind.CAVE) == 1, "a clutch in the cave");
        Clutch k = cave.getTank().clutches().getFirst();
        TankmateEntity male = mates(h, cave, null).stream().filter(m -> !m.isFemale()).findFirst().orElseThrow();
        h.assertTrue(male.getUUID().equals(k.guardian), "the male guards");
        days(h, cave, 0.25, 0.25, 25F);
        h.assertTrue(male.isGuarding() && male.holdSpot() != null, "he stays at the cave");
        int before = k.count;
        days(h, cave, 0.5, 0.25, 25F);
        h.assertTrue(k.count == before, "guarded eggs are safe: " + k.count + " of " + before);
        // without the male the clutch dies
        cave.getTank().inhabitants().remove(male.getUUID());
        male.discard();
        days(h, cave, 1.0, 0.25, 25F);
        h.assertTrue(clutches(cave.getTank(), Clutch.Kind.CAVE) == 0, "the clutch died without its father");
        h.assertTrue(logged(cave.getTank(), "log.bettacraft.tm.clutch_died"), "logged");
        h.succeed();
    }

    /** Neons scatter their eggs into a spawning mop (most are safe there); without plants or a mop onto the bare ground. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void eggScatterersNeedAMopOrFinePlants(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity bare = tank(h, 2, 2, 2, 2, 2, 2, 8000, 23F);
        TankBlockEntity mop = tank(h, 8, 2, 2, 2, 2, 2, 8000, 23F);
        TankBlockEntity plants = tank(h, 14, 2, 2, 2, 2, 2, 8000, 23F);
        InteriorRegion r = interior(h, mop);
        decor(h, r, DecorType.SPAWNING_MOP, 14, 14);
        h.assertTrue(TankmateSpots.mops(level.getServer(), mop.getTank()).size() == 1, "one mop");
        InteriorRegion rp = interior(h, plants);
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        long seed = 1900;
        for (int x = 4; x < rp.sizeX() - 3; x += 6) for (int z = 4; z < rp.sizeZ() - 3; z += 6) {
            InteriorPlants.plantAt(in, rp, PlantSpecies.ROTALA, x, 0, z, PlantSpecies.ROTALA.stages(), seed++);
        }
        h.assertTrue(TankmateSpots.plantShare(level.getServer(), plants.getTank(), false) > 0, "fine-leaved plants");
        for (TankBlockEntity c : List.of(bare, mop, plants)) for (int i = 0; i < 6; i++) add(h, c, TankmateSpecies.NEON_TETRA, i % 2 == 0);
        for (TankBlockEntity c : List.of(bare, mop, plants)) {
            for (int i = 0; i < 40 && clutches(c.getTank(), Clutch.Kind.SCATTER) == 0 && !logged(c.getTank(), "log.bettacraft.tm.spawned_bare"); i++) {
                feedAll(h, c);
                c.getTank().setWaterTemp(23F);
                TankmateBreeding.step(level, c, 0.1);
            }
        }
        h.assertTrue(logged(bare.getTank(), "log.bettacraft.tm.spawned_bare"), "bare tank: the eggs fall on the ground");
        h.assertTrue(logged(mop.getTank(), "log.bettacraft.tm.spawned_scatter"), "into the mop");
        h.assertTrue(logged(plants.getTank(), "log.bettacraft.tm.spawned_scatter"), "into the plants");
        double mopCover = mop.getTank().clutches().stream().filter(k -> k.kind == Clutch.Kind.SCATTER).mapToDouble(k -> k.cover).max().orElse(0);
        h.assertTrue(mopCover >= 0.75, "the mop protects the eggs: " + mopCover);
        double bareCover = bare.getTank().clutches().stream().mapToDouble(k -> k.cover).max().orElse(0);
        h.assertTrue(bareCover < 0.1, "bare ground does not: " + bareCover);
        h.succeed();
    }

    /** Guppies: the female gets pregnant and bears live young; with a breeding box they go into the box and come out as juveniles. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void livebearersBearYoungIntoTheBreedingBox(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity open = tank(h, 2, 2, 2, 3, 2, 2, 12000, 25F);
        TankBlockEntity boxed = tank(h, 8, 2, 2, 3, 2, 2, 12000, 25F);
        ServerPlayer sp = player(h);
        use(sp, boxed.getBlockPos(), new ItemStack(ModItems.BREEDING_BOX.get()));
        h.assertTrue(boxed.getTank().breedingBox(), "breeding box hung in");
        h.assertTrue(TankInhabitants.breakBlockedReason(level, boxed.getBlockPos()).isPresent(), "the box keeps the tank");
        for (TankBlockEntity c : List.of(open, boxed)) {
            add(h, c, TankmateSpecies.GUPPY, TankmateGenes.pure(TankmateSpecies.GUPPY, "red"), true, LifeStage.ADULT);
            add(h, c, TankmateSpecies.GUPPY, TankmateGenes.pure(TankmateSpecies.GUPPY, "red"), false, LifeStage.ADULT);
        }
        for (TankBlockEntity c : List.of(open, boxed)) {
            // M21.7: up to 15 days (was 5: about 2 % of the runs had no pregnancy by chance)
            for (int i = 0; i < 60 && mates(h, c, null).stream().noneMatch(TankmateEntity::isGravid); i++) days(h, c, 0.25, 0.25, 25F);
            h.assertTrue(mates(h, c, null).stream().anyMatch(TankmateEntity::isGravid), "a pregnant female");
            days(h, c, TankmateBreeding.gestationDays() + 0.25, 0.25, 25F);
            h.assertTrue(logged(c.getTank(), c == open ? "log.bettacraft.tm.born" : "log.bettacraft.tm.born_box"), "she gave birth");
        }
        long fryOpen = mates(h, open, null).stream().filter(m -> !m.isAdult()).count();
        h.assertTrue(fryOpen > 0, "free fry in the open tank: " + fryOpen);
        int inBox = TankmateBreeding.boxFry(boxed.getTank());
        h.assertTrue(inBox > 0 && inBox <= TankmateBreeding.boxCapacity(), "young in the box: " + inBox);
        // the box young grow up safely, then come out as juveniles of the parents' colour
        days(h, boxed, LifeStage.FRY.days() * TankmateSpecies.GUPPY.growth() + 0.5, 0.25, 25F);
        h.assertTrue(TankmateBreeding.boxFry(boxed.getTank()) == 0 || logged(boxed.getTank(), "log.bettacraft.tm.box_released"), "released");
        var young = mates(h, boxed, null).stream().filter(m -> !m.isAdult()).toList();
        h.assertTrue(!young.isEmpty(), "juveniles from the box");
        for (TankmateEntity y : young) h.assertTrue("red".equals(y.variety()), "red x red: red young, got " + y.variety());
        h.succeed();
    }

    /** The dwarf gourami builds a foam nest, the female spawns into it, the male guards; the larvae swim free. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void gouramiFoamNest(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 2, 2, 3, 2, 2, 12000, 27F);
        TankmateEntity male = add(h, c, TankmateSpecies.DWARF_GOURAMI, false);
        add(h, c, TankmateSpecies.DWARF_GOURAMI, true);
        TankData t = c.getTank();
        for (int i = 0; i < 30 && t.clutches().stream().noneMatch(k -> k.kind == Clutch.Kind.FOAM && k.count > 0); i++) days(h, c, 0.1, 0.1, 27F);
        Clutch nest = t.clutches().stream().filter(k -> k.kind == Clutch.Kind.FOAM).findFirst().orElse(null);
        h.assertTrue(nest != null && !nest.building && nest.count > 0, "a foam nest with eggs: " + t.clutches().size());
        h.assertTrue(male.getUUID().equals(nest.guardian), "the male's nest");
        h.assertTrue(logged(t, "log.bettacraft.nest_built"), "nest built");
        // near the surface
        h.assertTrue(nest.y > t.waterSurfaceY() - 0.3, "at the surface: " + nest.y + " / " + t.waterSurfaceY());
        days(h, c, 2.0, 0.1, 27F);
        long fry = mates(h, c, TankmateSpecies.DWARF_GOURAMI).stream().filter(m -> m.lifeStage() == LifeStage.FRY).count();
        h.assertTrue(fry > 0, "gourami fry: " + fry);
        h.succeed();
    }

    // ------------------------------------------------------------------ getting along

    /** A male betta eats guppy fry, chases the male guppy (wounds) and leaves the female alone. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void bettasHuntFryAndChaseGuppyMales(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 2, 2, 3, 2, 2, 12000, 27F);
        BettaData d = BettaData.create(BettaGenome.DEFAULT, false, level.getGameTime());
        var err = TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d), ModEntities.BETTA.get(), null, 0L);
        h.assertTrue(err == null, "betta in");
        TankmateEntity male = add(h, c, TankmateSpecies.GUPPY, false);
        TankmateEntity female = add(h, c, TankmateSpecies.GUPPY, true);
        for (int i = 0; i < 20; i++) add(h, c, TankmateSpecies.PLATY, TankmateGenes.wild(TankmateSpecies.PLATY), i % 2 == 0, LifeStage.FRY);
        float hp = male.getHealth();
        for (int i = 0; i < 8; i++) {
            feedAll(h, c);
            TankmateBreeding.step(level, c, 0.25);
        }
        long fry = mates(h, c, TankmateSpecies.PLATY).size();
        h.assertTrue(fry < 12, "the betta (and the guppies) ate fry: " + fry + " of 20 left");
        h.assertTrue(!male.isAlive() || male.getHealth() < hp, "the male guppy is wounded: " + male.getHealth());
        h.assertTrue((female.stressBits() & TankmateCare.STRESS_CHASED) == 0, "the female is left alone");
        h.assertTrue(logged(c.getTank(), "log.bettacraft.tm.betta_chases"), "logged");
        h.succeed();
    }

    /** Clown loaches eat snails, not the big apple snails; otocinclus graze the algae on the glass. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void clownLoachesEatSnailsOtosGrazeAlgae(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 2, 2, 3, 2, 3, 18000, 27F);
        for (int i = 0; i < 5; i++) add(h, c, TankmateSpecies.CLOWN_LOACH, i % 2 == 0);
        for (int i = 0; i < 12; i++) TankCritters.add(level, c, CritterSpecies.RAMSHORN, CritterGenes.WILD, true, 10);
        TankCritters.add(level, c, CritterSpecies.APPLE_SNAIL, CritterGenes.WILD, true, 20);
        days(h, c, 4, 0.25, 27F);
        long ramshorns = c.getTank().critters().stream().filter(k -> k.alive() && k.species == CritterSpecies.RAMSHORN).count();
        long apples = c.getTank().critters().stream().filter(k -> k.alive() && k.species == CritterSpecies.APPLE_SNAIL).count();
        h.assertTrue(ramshorns < 12, "snails eaten: " + ramshorns + " of 12 left");
        h.assertTrue(apples == 1, "the adult apple snail is too big");
        TankBlockEntity oto = tank(h, 12, 2, 2, 2, 2, 2, 8000, 24F);
        for (int i = 0; i < 5; i++) add(h, oto, TankmateSpecies.OTOCINCLUS, i % 2 == 0);
        TankmateBreeding.step(level, oto, 0.05);
        h.assertTrue(oto.getTank().fishGrazing() > 0, "otos graze: " + oto.getTank().fishGrazing());
        h.assertTrue(org.overengineer.bettacraft.tank.algae.TankAlgae.grazingPerDay(oto.getTank()) >= oto.getTank().fishGrazing(), "counted for the algae");
        h.succeed();
    }

    /** Algae wafers go to the bottom fish only: the corys eat, the betta does not. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void algaeWafersFeedOnlyTheBottomFish(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 2, 2, 2, 2, 2, 8000, 26F);
        BettaData d = BettaData.create(BettaGenome.DEFAULT, true, level.getGameTime());
        TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d), ModEntities.BETTA.get(), null, 0L);
        BettaEntity betta = (BettaEntity) TankInhabitants.liveInhabitants(level, c.getTank()).stream().filter(e -> e instanceof BettaEntity).findFirst().orElseThrow();
        List<TankmateEntity> corys = new ArrayList<>();
        for (int i = 0; i < 4; i++) corys.add(add(h, c, TankmateSpecies.STERBAI_CORY, i % 2 == 0));
        TankmateEntity neon = add(h, c, TankmateSpecies.CARDINAL_TETRA, true);
        for (TankmateEntity m : corys) TankmateCare.advance(level, c, m, 1.5);
        TankmateCare.advance(level, c, neon, 1.5);
        betta.health().step(1.5, org.overengineer.bettacraft.entity.betta.BettaHealth.Conditions.GOOD, org.overengineer.bettacraft.entity.betta.BettaCare.params());
        int used = TankCare.feed(level, c, new ItemStack(ModItems.ALGAE_WAFER.get()), 4);
        h.assertTrue(used >= 1, "a wafer was eaten");
        for (TankmateEntity m : corys) h.assertTrue(m.health().daysSinceFed() < 0.01, "corys fed");
        h.assertTrue(neon.health().daysSinceFed() > 1, "the cardinal did not get a wafer");
        h.assertTrue(betta.health().daysSinceFed() > 1, "the betta did not get a wafer");
        // pellets reach everybody
        TankCare.feed(level, c, new ItemStack(ModItems.BETTA_PELLETS.get()), 4);
        h.assertTrue(neon.health().daysSinceFed() < 0.01 && betta.health().daysSinceFed() < 0.01, "pellets feed the midwater fish and the betta");
        h.succeed();
    }

    // ------------------------------------------------------------------ getting them, moving eggs, menu

    /** The net in jungle water catches South American tankmates (config: always a fish); the trader sells a shoal. */
    @GameTest(template = EMPTY, batch = "bettacraft_tankmates_net", timeoutTicks = 200)
    public static void netInTheJungleAndTheTrader(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        double chance = ServerConfig.CRITTERS_NET_CHANCE.get(), share = ServerConfig.TANKMATES_NET_SHARE.get();
        try {
            ServerConfig.CRITTERS_NET_CHANCE.set(1.0);
            ServerConfig.TANKMATES_NET_SHARE.set(1.0);
            var table = NetCatching.fishTable(NetCatching.Water.JUNGLE);
            h.assertTrue(table.containsKey(TankmateSpecies.NEON_TETRA) && table.containsKey(TankmateSpecies.LEOPARD_FROG_PLECO), "amazon species");
            h.assertTrue(!table.containsKey(TankmateSpecies.HARLEQUIN_RASBORA), "no asian rasboras in the jungle");
            h.assertTrue(NetCatching.fishTable(NetCatching.Water.SWAMP).containsKey(TankmateSpecies.MOSQUITO_RASBORA), "mosquito rasboras in the swamp");
            BlockPos water = h.absolutePos(new BlockPos(16, 2, 16));
            h.setBlock(new BlockPos(16, 2, 16), Blocks.WATER.defaultBlockState());
            h.setBlock(new BlockPos(16, 1, 16), Blocks.SAND.defaultBlockState());
            var src = level.getServer().createCommandSourceStack().withSuppressedOutput().withLevel(level);
            level.getChunk(water.getX() >> 4, water.getZ() >> 4);
            level.getServer().getCommands().performPrefixedCommand(src, "fillbiome " + (water.getX() - 3) + " " + (water.getY() - 2) + " " + (water.getZ() - 3)
                    + " " + (water.getX() + 3) + " " + (water.getY() + 3) + " " + (water.getZ() + 3) + " minecraft:jungle");
            h.assertTrue(NetCatching.water(level, water) == NetCatching.Water.JUNGLE, "jungle water: " + NetCatching.water(level, water));
            ServerPlayer sp = player(h);
            sp.getInventory().add(new ItemStack(ModItems.FISH_BAG.get(), 4));
            sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.AQUARIUM_NET.get()));
            NetCatching.tryCatch(level, sp, InteractionHand.MAIN_HAND, water);
            ItemStack caught = ItemStack.EMPTY;
            for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
                ItemStack s = sp.getInventory().getItem(i);
                if (TankmateItems.bagSpecies(s) != null) caught = s;
            }
            h.assertTrue(!caught.isEmpty(), "a tankmate in the bag");
            TankmateSpecies s = TankmateItems.bagSpecies(caught);
            h.assertTrue(s.nativeWeight(TankmateSpecies.Water.JUNGLE) > 0, "a jungle species: " + s.id());
        } finally {
            ServerConfig.CRITTERS_NET_CHANCE.set(chance);
            ServerConfig.TANKMATES_NET_SHARE.set(share);
        }
        var offer = TankmateTrades.offer(TankmateSpecies.CARDINAL_TETRA, new java.util.Random(4)::nextLong);
        h.assertTrue(TankmateItems.bagFish(offer.getResult()).size() == 6, "six cardinals for sale");
        TankBlockEntity c = tank(h, 2, 2, 2, 2, 2, 2, 8000, 27F);
        ServerPlayer sp = player(h);
        use(sp, c.getBlockPos(), offer.getResult().copy());
        h.assertTrue(mates(h, c, TankmateSpecies.CARDINAL_TETRA).size() == 6, "the trader's shoal goes in");
        h.succeed();
    }

    /** Eggs on the glass are taken out with the net into a fish bag and put into another tank to be raised. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void clutchMovedWithTheNet(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity a = tank(h, 2, 2, 2, 2, 2, 2, 8000, 24F);
        TankBlockEntity b = tank(h, 8, 2, 2, 2, 2, 2, 8000, 24F);
        TankData t = a.getTank();
        BlockPos mn = t.min();
        Clutch k = new Clutch(t.nextClutchId(), TankmateSpecies.PANDA_CORY, Clutch.Kind.GLASS, mn.getX() + 1.0, mn.getY() + 0.4, mn.getZ() + 1.0 / 16 + 0.004,
                16, TankmateGenes.wild(TankmateSpecies.PANDA_CORY), TankmateGenes.wild(TankmateSpecies.PANDA_CORY));
        k.face = 0;
        t.clutches().add(k);
        h.assertTrue(TankInhabitants.breakBlockedReason(level, a.getBlockPos()).isPresent(), "eggs keep the tank");
        ServerPlayer sp = player(h);
        sp.getInventory().add(new ItemStack(ModItems.FISH_BAG.get()));
        Vec3 eye = new Vec3(k.x, k.y, mn.getZ() - 1.5);
        var aimed = TankmateItems.aimed(t, eye, new Vec3(0, 0, 1));
        h.assertTrue(aimed != null && aimed.clutch() == k, "the net is aimed at the eggs");
        var msg = TankmateItems.net(level, a, sp, k);
        h.assertTrue(msg != null && t.clutches().isEmpty(), "taken out: " + (msg == null ? "" : msg.getString()));
        ItemStack bag = ItemStack.EMPTY;
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) if (sp.getInventory().getItem(i).has(ModDataComponents.TANKMATE_CLUTCH.get())) bag = sp.getInventory().getItem(i);
        h.assertTrue(!bag.isEmpty(), "a bag with the eggs");
        h.assertTrue(bag.getHoverName().getString().contains("16"), "named with the count: " + bag.getHoverName().getString());
        use(sp, b.getBlockPos(), bag.copy());
        h.assertTrue(b.getTank().clutches().size() == 1 && b.getTank().clutches().getFirst().count == 16, "the eggs are in the other tank");
        h.succeed();
    }

    /** The menu shows a tankmate's shoal, temperature and stocking; the breeding tab its species; the data survives saving. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void menuShowsTankmatesAndDataSurvives(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 2, 2, 2, 2, 2, 8000, 24F);
        for (int i = 0; i < 3; i++) add(h, c, TankmateSpecies.HARLEQUIN_RASBORA, i % 2 == 0);
        TankmateEntity g = add(h, c, TankmateSpecies.PLATY, TankmateGenes.pure(TankmateSpecies.PLATY, "mickey_mouse"), true, LifeStage.JUVENILE);
        days(h, c, 0.1, 0.1, 24F);
        ServerPlayer sp = player(h);
        CompoundTag view = TankView.build(sp, c);
        TankView v = TankView.read(view);
        var harl = v.inhabitants.stream().filter(TankView.Inhabitant::isTankmate)
                .filter(i -> "harlequin_rasbora".equals(i.tankmate().getString("species"))).toList();
        h.assertTrue(harl.size() == 3, "three harlequins in the menu: " + harl.size());
        h.assertTrue(harl.getFirst().tankmate().getInt("shoal") == 3, "shoal size shown");
        h.assertTrue((harl.getFirst().tankmate().getInt("stress") & TankmateCare.STRESS_SHOAL) != 0, "too small a shoal shown");
        var tm = v.breeding.getList("tankmates", 10);
        h.assertTrue(tm.size() == 2, "two species on the breeding tab: " + tm.size());
        // save and load keep genes, sex, stage
        CompoundTag saved = g.saveTankmate();
        TankmateEntity copy = ModEntities.TANKMATES.get(TankmateSpecies.PLATY).get().create(level);
        copy.loadTankmate(saved);
        h.assertTrue("mickey_mouse".equals(copy.variety()) && copy.isFemale() && copy.lifeStage() == LifeStage.JUVENILE, "data survives");
        // the bucket keeps it too
        ItemStack bucket = g.getBucketItemStack();
        g.saveToBucketTag(bucket);
        h.assertTrue(bucket.getItem() instanceof org.overengineer.bettacraft.tankmates.TankmateBucketItem, "a platy bucket");
        h.assertTrue(org.overengineer.bettacraft.tankmates.TankmateBucketItem.data(bucket) != null, "with the fish data");
        h.succeed();
    }

    /** Taking a tankmate out with a fish bag from the menu works like for any fish. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void takeOutAndPutBack(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 2, 2, 2, 2, 2, 8000, 27F);
        TankmateEntity m = add(h, c, TankmateSpecies.BRISTLENOSE_PLECO, TankmateGenes.pure(TankmateSpecies.BRISTLENOSE_PLECO, "albino"), false, LifeStage.ADULT);
        ServerPlayer sp = player(h);
        sp.getInventory().add(new ItemStack(ModItems.FISH_BAG.get()));
        var err = TankInhabitants.takeOut(sp, c, m.getUUID(), true);
        h.assertTrue(err == null, "taken out: " + (err == null ? "" : err.getString()));
        ItemStack bag = ItemStack.EMPTY;
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) if (TankmateItems.bagSpecies(sp.getInventory().getItem(i)) != null) bag = sp.getInventory().getItem(i);
        h.assertTrue(!bag.isEmpty(), "the pleco in a bag");
        h.assertTrue(mates(h, c, null).isEmpty(), "gone from the tank");
        use(sp, c.getBlockPos(), bag.copy());
        List<TankmateEntity> back = mates(h, c, null);
        h.assertTrue(back.size() == 1 && "albino".equals(back.getFirst().variety()) && !back.getFirst().isFemale(), "the same albino male is back");
        h.succeed();
    }

    /** Every species can be spawned into a tank, stays inside for a while and counts for the stocking. */
    @GameTest(template = EMPTY, timeoutTicks = 260)
    public static void everySpeciesLivesInATank(GameTestHelper h) {
        List<TankBlockEntity> tanks = new ArrayList<>();
        int i = 0;
        for (TankmateSpecies s : TankmateSpecies.values()) {
            int x = 1 + (i % 6) * 5, z = 1 + (i / 6) * 7; // M21.5: 6 a row (26 species)
            TankBlockEntity c = tank(h, x, 2, z, 3, 2, 3, 18000, (float) ((s.tempMin() + s.tempMax()) / 2));
            if (s.marine()) c.getTank().setSalinity(35F); // M20: the reef fish in sea water
            for (int k = 0; k < Math.max(2, Math.min(4, s.shoal())); k++) add(h, c, s, k % 2 == 0);
            tanks.add(c);
            i++;
        }
        h.runAfterDelay(240, () -> {
            int j = 0;
            for (TankmateSpecies s : TankmateSpecies.values()) {
                TankBlockEntity c = tanks.get(j++);
                List<TankmateEntity> list = mates(h, c, s);
                h.assertTrue(list.size() >= 2, s.id() + " still in the tank: " + list.size());
                for (TankmateEntity m : list) {
                    h.assertTrue(c.getTank().contains(m.blockPosition()) || c.getTank().contains(m.blockPosition().below()), s.id() + " inside");
                    h.assertTrue(m.isInTank(), s.id() + " knows its tank");
                }
                h.assertTrue(TankInhabitants.stocking(h.getLevel(), c.getTank()) > 0, s.id() + " counts");
            }
            h.succeed();
        });
    }

    /** The clutch component of a bag shows in the tooltip; a group bag too. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void bagTexts(GameTestHelper h) {
        ItemStack group = TankmateItems.groupBag(TankmateSpecies.GUPPY, 4, () -> TankmateGenes.pure(TankmateSpecies.GUPPY, "blue_grass"),
                new java.util.Random(2)::nextLong, true);
        List<net.minecraft.network.chat.Component> tip = new ArrayList<>();
        h.assertTrue(TankmateItems.appendBag(group, tip), "group tooltip");
        h.assertTrue(tip.stream().anyMatch(c -> c.getString().contains("4")), "the variety line: " + tip);
        h.assertTrue(FishBagItem.isFilled(group), "a filled bag");
        h.succeed();
    }
}
