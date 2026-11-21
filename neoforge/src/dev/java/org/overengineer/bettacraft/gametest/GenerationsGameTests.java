package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import org.overengineer.bettacraft.util.NbtCompat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterMovement;
import org.overengineer.bettacraft.critters.CritterObject;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaCare;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateCare;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.foreign.ForeignBreeding;
import org.overengineer.bettacraft.foreign.ForeignCare;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.genetics.BettaLoci;
import org.overengineer.bettacraft.genetics.BettaTypes;
import org.overengineer.bettacraft.genetics.livebearer.LbLocus;
import org.overengineer.bettacraft.genetics.livebearer.LivebearerGenome;
import org.overengineer.bettacraft.genetics.livebearer.LivebearerStrain;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.AxolotlCare;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankBreeding;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tank.algae.TankAlgae;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.menu.TankView;
import org.overengineer.bettacraft.tankmates.Clutch;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.terrarium.Climate;
import org.overengineer.bettacraft.terrarium.HerpBreeding;
import org.overengineer.bettacraft.terrarium.HerpClutch;
import org.overengineer.bettacraft.terrarium.HerpEntity;
import org.overengineer.bettacraft.terrarium.HerpSpecies;
import org.overengineer.bettacraft.terrarium.TankClimate;
import org.overengineer.bettacraft.terrarium.TerrariumMode;

/**
 * M42 part c: complete breeding runs over several generations, the way a breeder works: a pair breeds (nest, brood, eggs or a
 * pregnancy), the young are reared, the ones with the wanted genes are chosen, taken out with the containers of the game
 * (bucket, fish bag, net, transport tub, box) and put into a new tank, where they breed on. Each run checks the inheritance
 * (Mendel: carriers in the first generation, the recessive trait in the second, a true breeding line in the third), the varieties,
 * the pedigree (generation, parents), the menu's values of the animals and the rare traits. Sped up by direct care steps (days
 * at a time) and a few config values (more eggs, no fry eaten); every test changes the config only in its own batch and restores
 * it in {@code finally}; the random draws come from fixed seeds (the level's random is seeded before each step).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GenerationsGameTests {
    private static final String EMPTY = "empty_32x10x32";
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("bettacraft/gametest");

    private GenerationsGameTests() {}

    // ------------------------------------------------------------------ the breeder

    /** The breeder: a survival mock player whose inventory holds only what a step hands him. */
    public static ServerPlayer breeder(GameTestHelper h) {
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        sp.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        sp.getInventory().clearContent();
        sp.getInventory().setSelectedSlot(8);
        return sp;
    }

    /**
     * Takes {@code e} out of {@code from} like the menu's buttons do (bucket button, or the bag button with a fish bag or a
     * transport tub), with {@code empty} as the only container; returns the filled container.
     */
    public static ItemStack takeOut(GameTestHelper h, ServerPlayer sp, TankBlockEntity from, Entity e, ItemStack empty, boolean bag) {
        sp.getInventory().clearContent();
        sp.getInventory().setItem(0, empty);
        Component err = TankInhabitants.takeOut(sp, from, e.getUUID(), bag);
        if (err != null) {
            // M52: the state of the animal, for a failure seen once (26.1.2, box turtle hatchling) that never came back
            Entity now = h.getLevel().getEntity(e.getUUID());
            throw new GameTestFailure("take out " + e.getType().getDescriptionId() + ": " + err.getString() + " (by its UUID: "
                    + (now == null ? "none" : now == e ? "the same" : "another") + ", alive " + e.isAlive() + ", removed " + e.getRemovalReason()
                    + ", tank ref " + (TankInhabitants.hasTankRef(e) ? org.overengineer.bettacraft.registry.ModAttachments.TANK_REF.get(e).tankId() : "none")
                    + ", this tank " + from.getTank().id() + ", listed " + from.getTank().inhabitants().contains(e.getUUID()) + ", at " + e.position() + ")");
        }
        h.assertTrue(!e.isAlive() || e.isRemoved(), "the animal left the tank");
        h.assertTrue(!from.getTank().inhabitants().contains(e.getUUID()), "no longer an inhabitant");
        for (ItemStack s : sp.getInventory().getNonEquipmentItems()) if (TankInhabitants.containedType(s) != null) return s.copy();
        throw new GameTestFailure("no filled container after taking out " + e.getType().getDescriptionId());
    }

    /** Puts the animal of a filled container into {@code to} by right-clicking the tank with it; returns the new inhabitant. */
    public static Entity putIn(GameTestHelper h, ServerPlayer sp, TankBlockEntity to, ItemStack filled) {
        ServerLevel level = h.getLevel();
        List<Entity> before = TankInhabitants.liveInhabitants(level, to.getTank());
        sp.getInventory().clearContent();
        sp.setItemInHand(InteractionHand.MAIN_HAND, filled);
        InteractionResult r = TankInhabitants.tryInsert(filled, level, to.getBlockPos(), sp, InteractionHand.MAIN_HAND);
        h.assertTrue(r == InteractionResult.CONSUME, "inserted: " + r);
        for (Entity e : TankInhabitants.liveInhabitants(level, to.getTank())) if (!before.contains(e)) return e;
        throw new GameTestFailure("nothing came into the tank from " + filled);
    }

    /** Moves {@code e} from one tank to the other in {@code empty} (the menu's take-out, then a right-click on the new tank). */
    public static Entity move(GameTestHelper h, ServerPlayer sp, TankBlockEntity from, Entity e, TankBlockEntity to, ItemStack empty, boolean bag) {
        return putIn(h, sp, to, takeOut(h, sp, from, e, empty, bag));
    }

    static TankView menu(GameTestHelper h, ServerPlayer sp, TankBlockEntity c) {
        return TankView.read(TankView.build(sp, c));
    }

    static TankView.Inhabitant row(TankView v, Entity e) {
        for (TankView.Inhabitant i : v.inhabitants) if (i.uuid().equals(e.getUUID())) return i;
        throw new GameTestFailure("the menu does not list " + e.getType().getDescriptionId());
    }

    static boolean logged(TankData t, String key) {
        return t.log().stream().anyMatch(e -> e.key().equals(key));
    }

    /** Runs {@code body} with the config values set and restores them afterwards (also when it fails). */
    @SafeVarargs
    static void withConfig(Runnable body, Object... pairs) {
        Map<net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<Object>, Object> saved = new HashMap<>();
        boolean mutations = Features.own(Feature.MUTATIONS);
        try {
            Features.set(Feature.MUTATIONS, false);
            for (int i = 0; i < pairs.length; i += 2) {
                @SuppressWarnings("unchecked")
                var cv = (net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<Object>) pairs[i];
                saved.put(cv, cv.get());
                cv.set(pairs[i + 1]);
            }
            body.run();
        } finally {
            saved.forEach(net.neoforged.neoforge.common.ModConfigSpec.ConfigValue::set);
            Features.set(Feature.MUTATIONS, mutations);
        }
    }

    /** Counts of the expressed values among the animals, for the log. */
    static <T> String tally(List<T> l, java.util.function.Function<T, String> f) {
        Map<String, Integer> m = new java.util.TreeMap<>();
        for (T x : l) m.merge(f.apply(x), 1, Integer::sum);
        return m.toString();
    }

    // ------------------------------------------------------------------ bettas

    private static TankBlockEntity bettaTank(GameTestHelper h, int x) {
        TankBlockEntity c = CritterGameTests.tank(h, x, 1, 2, 4, 2, 4, 32000);
        c.getTank().legacyHeaters().setItem(0, new ItemStack(ModItems.HEATER_LARGE.get()));
        c.getTank().setTargetTemp(27F);
        c.getTank().setWaterTemp(27F);
        c.getTank().setWaterQuality(1F);
        return c;
    }

    private static List<BettaEntity> bettas(GameTestHelper h, TankBlockEntity c) {
        List<BettaEntity> out = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(h.getLevel(), c.getTank())) if (e instanceof BettaEntity b && b.isAlive()) out.add(b);
        return out;
    }

    private static BettaEntity betta(GameTestHelper h, TankBlockEntity c, BettaGenotype g, boolean female) {
        ItemStack bucket = BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), BettaData.create(g, female, 0));
        Component err = TankInhabitants.insert(h.getLevel(), c, bucket, ModEntities.BETTA.get(), null, false);
        if (err != null) throw new GameTestFailure("insert failed: " + err.getString());
        return bettas(h, c).stream().filter(b -> b.isFemale() == female && b.genotype().equals(g)).findFirst().orElseThrow();
    }

    /**
     * One brood of a betta pair in {@code c}: fed until the male has built a nest and they spawned, the female out (bucket),
     * the eggs hatch, the free fry are left alone by taking the male out (fish bag), then the young are reared with fry food,
     * nauplii and pellets up to adults. Returns the grown young.
     */
    private static List<BettaEntity> bettaBrood(GameTestHelper h, ServerPlayer sp, TankBlockEntity c, BettaEntity male, BettaEntity female, long seed) {
        ServerLevel level = h.getLevel();
        RandomSource r = RandomSource.create(seed);
        level.getRandom().setSeed(seed);
        TankData t = c.getTank();
        UUID mother = female.getBettaData().fishId(), father = male.getBettaData().fishId();
        for (int i = 0; i < 400 && (t.nest() == null || !t.nest().hasEggs()); i++) {
            TankCare.feed(level, c, new ItemStack(ModItems.BETTA_PELLETS.get()), 4);
            TankBreeding.step(level, c, 0.05, r);
        }
        h.assertTrue(t.nest() != null && t.nest().hasEggs(), "the pair spawned");
        ItemStack her = takeOut(h, sp, c, female, new ItemStack(Items.BUCKET), false);
        h.assertTrue(TankInhabitants.containedType(her) == ModEntities.BETTA.get(), "the female in a bucket");
        for (int i = 0; i < 400 && t.nest() != null; i++) TankBreeding.step(level, c, 0.05, r);
        h.assertTrue(t.nest() == null && logged(t, "log.bettacraft.fry_free"), "the fry swim free");
        takeOut(h, sp, c, male, new ItemStack(ModItems.FISH_BAG.get()), true);
        List<BettaEntity> young = bettas(h, c);
        h.assertTrue(!young.isEmpty(), "fry");
        for (BettaEntity b : young) {
            BettaData d = b.getBettaData();
            h.assertTrue(d.mother().equals(Optional.of(mother)) && d.father().equals(Optional.of(father)), "pedigree: the parents of " + d.fishId());
        }
        for (int i = 0; i < 160 && !bettas(h, c).stream().allMatch(BettaEntity::isAdult); i++) {
            TankCare.feed(level, c, new ItemStack(ModItems.INFUSORIA.get()), 64);
            TankCare.feed(level, c, new ItemStack(ModItems.ARTEMIA_NAUPLII.get()), 64);
            TankCare.feed(level, c, new ItemStack(ModItems.BETTA_PELLETS.get()), 64);
            for (BettaEntity b : bettas(h, c)) BettaCare.advance(level, c, b, 0.25);
            t.setWaterQuality(1F);
            t.setWaterTemp(27F);
        }
        young = bettas(h, c);
        h.assertTrue(!young.isEmpty() && young.stream().allMatch(BettaEntity::isAdult), "the young grew up: " + tally(young, BettaEntity::stage));
        return young;
    }

    private static String fins(BettaEntity b) {
        return b.genome().finLength() + (b.isFemale() ? "F" : "M");
    }

    /** Whether every allele of {@code child} comes from one parent each (no mutation). */
    private static boolean inherited(BettaGenotype child, BettaGenotype mother, BettaGenotype father) {
        for (var l : BettaLoci.ALL) {
            var p = child.pair(l);
            var m = mother.pair(l);
            var f = father.pair(l);
            boolean ok = (is(p.a(), m) && is(p.b(), f)) || (is(p.a(), f) && is(p.b(), m));
            if (!ok) return false;
        }
        return true;
    }

    private static boolean is(String allele, BettaGenotype.AllelePair pair) {
        return allele.equals(pair.a()) || allele.equals(pair.b());
    }

    /**
     * Bettas over three generations: a short-finned male (homozygous short, normal pectorals) and a long-finned Dumbo female;
     * F1 all short-finned with normal pectorals (long and dumbo carried hidden); one F1 pair to a new tank (bucket and bag):
     * F2 with about a quarter long-finned and a quarter Dumbo (the Punnett square of the menu's prediction says 25 %); a
     * long-finned F2 pair to a third tank: F3 all long-finned (a true breeding line). The pedigree (generation, parents) and the
     * menu's genome, genotype and generation of every young; the rare Dumbo trait comes back from hidden carriers.
     */
    @GameTest(template = EMPTY, timeoutTicks = 400, batch = "bettacraft_m42_gen_betta")
    public static void generationsOfBettas(GameTestHelper h) {
        withConfig(() -> bettas3(h), ServerConfig.BREEDING_MUTATION_CHANCE, 0.0, ServerConfig.BREEDING_MIN_EGGS, 40, ServerConfig.BREEDING_MAX_EGGS, 40,
                ServerConfig.BREEDING_MIN_SURVIVAL, 1.0, ServerConfig.BREEDING_MAX_SURVIVAL, 1.0);
        h.succeed();
    }

    private static void bettas3(GameTestHelper h) {
        ServerPlayer sp = breeder(h);
        String species = BettaGenome.DEFAULT.species();
        BettaGenotype pm = BettaGenotype.of(species, Map.of(BettaLoci.FIN_LENGTH, new BettaGenotype.AllelePair("short", "short"),
                BettaLoci.PECTORAL, new BettaGenotype.AllelePair("normal", "normal")));
        BettaGenotype pf = BettaGenotype.of(species, Map.of(BettaLoci.FIN_LENGTH, new BettaGenotype.AllelePair("long", "long"),
                BettaLoci.PECTORAL, new BettaGenotype.AllelePair("dumbo", "dumbo")));
        // P
        TankBlockEntity a = bettaTank(h, 1);
        BettaEntity m0 = betta(h, a, pm, false), f0 = betta(h, a, pf, true);
        List<BettaEntity> f1 = bettaBrood(h, sp, a, m0, f0, 4201L);
        for (BettaEntity b : f1) {
            h.assertTrue(b.getBettaData().generation() == 1, "F1 is generation 1");
            h.assertTrue("short".equals(b.genome().finLength()) && "normal".equals(b.genome().pectoral()), "F1 short-finned, normal pectorals: " + b.genome());
            var fl = b.genotype().pair(BettaLoci.FIN_LENGTH);
            var pc = b.genotype().pair(BettaLoci.PECTORAL);
            h.assertTrue(!fl.homozygous() && !pc.homozygous(), "F1 carry long and dumbo hidden: " + fl + " " + pc);
            h.assertTrue(inherited(b.genotype(), pf, pm), "every allele from the parents");
        }
        h.assertTrue(f1.stream().anyMatch(BettaEntity::isFemale) && f1.stream().anyMatch(b -> !b.isFemale()), "F1 of both sexes: " + tally(f1, GenerationsGameTests::fins));
        menuShowsTheBettas(h, sp, a);
        // the F1 pair into a new tank: the male in a bucket, the female in a fish bag
        TankBlockEntity b = bettaTank(h, 6);
        BettaEntity m1 = (BettaEntity) move(h, sp, a, f1.stream().filter(x -> !x.isFemale()).findFirst().orElseThrow(), b, new ItemStack(Items.BUCKET), false);
        BettaEntity fem1 = (BettaEntity) move(h, sp, a, f1.stream().filter(BettaEntity::isFemale).findFirst().orElseThrow(), b, new ItemStack(ModItems.FISH_BAG.get()), true);
        h.assertTrue(m1.getBettaData().generation() == 1 && fem1.getBettaData().generation() == 1 && m1.isAdult() && fem1.isAdult(),
                "the bucket and the bag keep generation and stage");
        h.assertTrue(m1.getBettaData().mother().equals(Optional.of(f0.getBettaData().fishId())), "and the pedigree");
        var predict = BettaGenotype.predict(fem1.genotype(), m1.genotype());
        double longShare = predict.get(BettaLoci.FIN_LENGTH).stream().filter(o -> o.allele().equals("long")).mapToDouble(BettaGenotype.Outcome::probability).sum();
        h.assertTrue(Math.abs(longShare - 0.25) < 1e-6, "the prediction: a quarter long-finned, got " + longShare);
        List<BettaEntity> f2 = bettaBrood(h, sp, b, m1, fem1, 4202L);
        long longs = f2.stream().filter(x -> "long".equals(x.genome().finLength())).count();
        long dumbos = f2.stream().filter(x -> "dumbo".equals(x.genome().pectoral())).count();
        LOG.info("[gametest] betta F2: {} young, fins {}, pectorals {}", f2.size(), tally(f2, GenerationsGameTests::fins), tally(f2, x -> x.genome().pectoral()));
        for (BettaEntity x : f2) {
            h.assertTrue(x.getBettaData().generation() == 2, "F2 is generation 2");
            h.assertTrue(inherited(x.genotype(), fem1.genotype(), m1.genotype()), "F2 alleles from the F1 pair");
        }
        h.assertTrue(longs >= f2.size() / 8 && longs <= f2.size() / 2, "about a quarter long-finned: " + longs + "/" + f2.size());
        h.assertTrue(dumbos >= 1 && dumbos <= f2.size() / 2, "the rare Dumbo is back: " + dumbos + "/" + f2.size());
        menuShowsTheBettas(h, sp, b);
        // a long-finned F2 pair (the Dumbo ones if there are) into a third tank
        Predicate<BettaEntity> wanted = x -> "long".equals(x.genome().finLength());
        java.util.Comparator<BettaEntity> dumboFirst = java.util.Comparator.comparing(x -> "dumbo".equals(x.genome().pectoral()) ? 0 : 1);
        BettaEntity m2 = f2.stream().filter(x -> !x.isFemale() && wanted.test(x)).sorted(dumboFirst).findFirst().orElse(null);
        BettaEntity fe2 = f2.stream().filter(x -> x.isFemale() && wanted.test(x)).sorted(dumboFirst).findFirst().orElse(null);
        h.assertTrue(m2 != null && fe2 != null, "a long-finned male and female in F2: " + tally(f2, GenerationsGameTests::fins));
        TankBlockEntity c = bettaTank(h, 11);
        m2 = (BettaEntity) move(h, sp, b, m2, c, new ItemStack(ModItems.FISH_BAG.get()), true);
        fe2 = (BettaEntity) move(h, sp, b, fe2, c, new ItemStack(Items.BUCKET), false);
        String variety = BettaTypes.name(fe2.genome()).getString();
        List<BettaEntity> f3 = bettaBrood(h, sp, c, m2, fe2, 4203L);
        for (BettaEntity x : f3) {
            h.assertTrue(x.getBettaData().generation() == 3, "F3 is generation 3");
            h.assertTrue("long".equals(x.genome().finLength()), "long x long breeds true: " + x.genome());
        }
        boolean bothDumbo = "dumbo".equals(m2.genome().pectoral()) && "dumbo".equals(fe2.genome().pectoral());
        if (bothDumbo) {
            for (BettaEntity x : f3) h.assertTrue(BettaTypes.name(x.genome()).getString().equals(variety), "the line keeps its variety " + variety);
        }
        LOG.info("[gametest] betta F3: {} young, fins {}, pectorals {}, variety of the mother {}", f3.size(), tally(f3, GenerationsGameTests::fins),
                tally(f3, x -> x.genome().pectoral()), variety);
        menuShowsTheBettas(h, sp, c);
    }

    /** The animals tab of the menu lists every betta with its generation, genome and (once visible) genotype. */
    private static void menuShowsTheBettas(GameTestHelper h, ServerPlayer sp, TankBlockEntity c) {
        TankView v = menu(h, sp, c);
        for (BettaEntity b : bettas(h, c)) {
            TankView.Inhabitant i = row(v, b);
            h.assertTrue(i.betta() && i.generation() == b.getBettaData().generation(), "the menu's generation " + i.generation());
            h.assertTrue(i.genome().equals(b.genome()), "the menu's genome");
            h.assertTrue(i.female() == b.isFemale() && i.adult() == b.isAdult(), "the menu's sex and stage");
            if (i.genesVisible()) h.assertTrue(i.genotype().equals(b.genotype()), "the menu's genotype");
            else h.assertTrue(i.genotype().equals(BettaGenotype.homozygous(b.genome())), "hidden genes show as the phenotype");
        }
        h.assertTrue(v.inhabitants.size() == bettas(h, c).size(), "every betta listed");
    }

    // ------------------------------------------------------------------ guppies

    private static TankBlockEntity guppyTank(GameTestHelper h, int x, int z) {
        TankBlockEntity c = CritterGameTests.tank(h, x, 2, z, 4, 2, 3, 24000);
        c.getTank().setWaterTemp(25F);
        c.getTank().setTargetTemp(25F);
        c.getTank().setWaterQuality(1F);
        return c;
    }

    private static TankmateEntity guppy(GameTestHelper h, TankBlockEntity c, TankmateGenes g, boolean female) {
        var w = new TankWaterVolume(c.getTank());
        Vec3 p = w.clamp(w.box().getCenter(), 0.35, 0.1, 0.2);
        TankmateEntity e = TankmateBreeding.spawn(h.getLevel(), c, TankmateSpecies.GUPPY, g, female, LifeStage.ADULT, 0, p);
        if (e == null) throw new GameTestFailure("no guppy");
        e.health().feed();
        return e;
    }

    private static List<TankmateEntity> mates(GameTestHelper h, TankBlockEntity c) {
        return new ArrayList<>(TankmateBreeding.tankmates(h.getLevel(), c.getTank()));
    }

    private static void tankmateDays(GameTestHelper h, TankBlockEntity c, double days, double step, float temp) {
        for (double d = 0; d < days - 1e-9; d += step) {
            for (TankmateEntity m : mates(h, c)) m.health().feed();
            c.getTank().setWaterTemp(temp);
            c.getTank().setWaterQuality(1F);
            for (TankmateEntity m : mates(h, c)) TankmateCare.advance(h.getLevel(), c, m, step);
            TankmateBreeding.step(h.getLevel(), c, step);
        }
    }

    /** Births of the pair in {@code c} until at least {@code atLeast} young of {@code gen} were born (or 120 days), then rearing. */
    private static List<TankmateEntity> guppyBroods(GameTestHelper h, TankBlockEntity c, int gen, int atLeast, long seed) {
        h.getLevel().getRandom().setSeed(seed);
        for (int d = 0; d < 480 && mates(h, c).stream().filter(m -> m.generation() == gen).count() < atLeast; d++) tankmateDays(h, c, 0.25, 0.25, 25F);
        List<TankmateEntity> young = mates(h, c).stream().filter(m -> m.generation() == gen).toList();
        h.assertTrue(young.size() >= atLeast, "at least " + atLeast + " young of generation " + gen + ": " + young.size());
        // the parents out (a breeder sorts them into another tank; no more young come while these grow)
        ServerPlayer sp = breeder(h);
        for (TankmateEntity m : mates(h, c)) if (m.generation() < gen) takeOut(h, sp, c, m, new ItemStack(Items.BUCKET), false);
        for (int d = 0; d < 120 && !young.stream().allMatch(TankmateEntity::isAdult); d++) tankmateDays(h, c, 0.5, 0.5, 25F);
        young = young.stream().filter(TankmateEntity::isAlive).toList();
        h.assertTrue(!young.isEmpty() && young.stream().allMatch(TankmateEntity::isAdult), "the young grew up: " + tally(young, m -> m.lifeStage().id()));
        return young;
    }

    private static boolean gold(TankmateEntity m) {
        return "gold".equals(m.livebearerPhenotype().body());
    }

    private static String guppyLook(TankmateEntity m) {
        return m.livebearerPhenotype().body() + (m.isFemale() ? "F" : "M");
    }

    /**
     * Guppies over three generations (no fry eaten, the parents stay): a Full Gold female and a Red Cobra male; F1 wild-bodied,
     * every young carries gold, every son has the cobra Y; an F1 son and daughter by bucket and fish bag into a new tank: F2 with
     * about a quarter gold bodies, the sons still cobra; a gold pair into a third tank: F3 all gold, the sons gold cobras. The menu
     * shows the variety, generation and genes of each.
     */
    @GameTest(template = EMPTY, timeoutTicks = 400, batch = "bettacraft_m42_gen_guppy")
    public static void generationsOfGuppies(GameTestHelper h) {
        withConfig(() -> guppies3(h), ServerConfig.TANKMATES_FRY_EATEN, 0.0);
        h.succeed();
    }

    private static void guppies3(GameTestHelper h) {
        ServerPlayer sp = breeder(h);
        TankBlockEntity a = guppyTank(h, 1, 2);
        TankmateEntity mother = guppy(h, a, TankmateGenes.strain(TankmateSpecies.GUPPY, LivebearerStrain.FULL_GOLD), true);
        TankmateEntity father = guppy(h, a, TankmateGenes.strain(TankmateSpecies.GUPPY, LivebearerStrain.RED_COBRA), false);
        LivebearerGenome mg = mother.genes().lb(), fg = father.genes().lb();
        List<TankmateEntity> f1 = guppyBroods(h, a, 1, 6, 4211L);
        for (TankmateEntity y : f1) {
            h.assertTrue(!gold(y), "F1 wild-bodied (gold recessive)");
            h.assertTrue(y.genes().lb().a(LbLocus.GOLD).equals("gold") || y.genes().lb().b(LbLocus.GOLD).equals("gold"), "F1 carry gold");
            if (!y.isFemale()) h.assertTrue("cobra".equals(y.genes().lb().y), "the sons have the father's Y");
            else h.assertTrue(y.genes().lb().xb.equals(fg.xa), "the daughters have the father's X");
        }
        h.assertTrue(f1.stream().anyMatch(TankmateEntity::isFemale) && f1.stream().anyMatch(m -> !m.isFemale()), "F1 of both sexes: " + tally(f1, GenerationsGameTests::guppyLook));
        guppyMenu(h, sp, a);
        TankBlockEntity b = guppyTank(h, 6, 2);
        TankmateEntity son = (TankmateEntity) move(h, sp, a, f1.stream().filter(m -> !m.isFemale()).findFirst().orElseThrow(), b, new ItemStack(Items.BUCKET), false);
        List<TankmateEntity> daughters = f1.stream().filter(TankmateEntity::isFemale).limit(2).toList();
        TankmateEntity daughter = null;
        for (TankmateEntity d : daughters) daughter = (TankmateEntity) move(h, sp, a, d, b, new ItemStack(ModItems.FISH_BAG.get()), true);
        h.assertTrue(son.generation() == 1 && daughter.generation() == 1 && son.isAdult(), "the containers keep the generation");
        h.assertTrue("cobra".equals(son.genes().lb().y) && !gold(daughter), "and the genes");
        List<TankmateEntity> f2 = guppyBroods(h, b, 2, 30, 4212L);
        long golds = f2.stream().filter(GenerationsGameTests::gold).count();
        LOG.info("[gametest] guppy F2: {} young, {}", f2.size(), tally(f2, GenerationsGameTests::guppyLook));
        h.assertTrue(golds >= f2.size() / 10 && golds <= f2.size() / 2, "about a quarter gold: " + golds + "/" + f2.size());
        for (TankmateEntity y : f2) if (!y.isFemale()) h.assertTrue("cobra".equals(y.genes().lb().y), "the Y goes from father to son");
        guppyMenu(h, sp, b);
        TankmateEntity gm = f2.stream().filter(m -> !m.isFemale() && gold(m)).findFirst().orElse(null);
        // a virgin female: one of the grown F2 may already carry young of a wild-bodied brother (the pregnancy goes with her in the
        // bucket); the breeder waits until a gold one has given birth
        for (int d = 0; d < 120 && f2.stream().noneMatch(m -> m.isAlive() && m.isFemale() && gold(m) && !m.isGravid()); d++) tankmateDays(h, b, 0.25, 0.25, 25F);
        TankmateEntity gf = f2.stream().filter(m -> m.isAlive() && m.isFemale() && gold(m) && !m.isGravid()).findFirst().orElse(null);
        h.assertTrue(gm != null && gf != null, "a gold son and daughter: " + tally(f2, GenerationsGameTests::guppyLook));
        TankBlockEntity c = guppyTank(h, 11, 2);
        gm = (TankmateEntity) move(h, sp, b, gm, c, new ItemStack(ModItems.FISH_BAG.get()), true);
        gf = (TankmateEntity) move(h, sp, b, gf, c, new ItemStack(Items.BUCKET), false);
        LivebearerGenome g3m = gf.genes().lb(), g3f = gm.genes().lb();
        List<TankmateEntity> f3 = guppyBroods(h, c, 3, 6, 4213L);
        int mutated = 0;
        for (TankmateEntity y : f3) {
            // guppies always mutate a little (0.4 % per allele and birth, by design not behind the mutation switch): a young
            // with a new allele may look different; every other one is gold like its parents
            if (!unmutated(y.genes().lb(), g3m, g3f)) {
                mutated++;
                continue;
            }
            h.assertTrue(gold(y), "gold x gold breeds true: " + guppyLook(y));
            if (!y.isFemale()) h.assertTrue("cobra".equals(y.genes().lb().y), "gold cobra sons");
        }
        h.assertTrue(mutated <= f3.size() / 3, "mutations stay rare: " + mutated + "/" + f3.size());
        LOG.info("[gametest] guppy F3: {} young ({} with a mutation), {}, strain of a son {}", f3.size(), mutated, tally(f3, GenerationsGameTests::guppyLook),
                f3.stream().filter(m -> !m.isFemale()).map(m -> String.valueOf(LivebearerStrain.of(m.livebearerPhenotype()))).findFirst().orElse("-"));
        guppyMenu(h, sp, c);
    }

    /** Every autosomal locus of the young has one allele of each parent, unchanged (in either order). */
    private static boolean unmutated(LivebearerGenome g, LivebearerGenome mother, LivebearerGenome father) {
        if (g == null) return false;
        for (LbLocus l : LbLocus.values()) {
            boolean straight = of(g.a(l), mother, l) && of(g.b(l), father, l), crossed = of(g.a(l), father, l) && of(g.b(l), mother, l);
            if (!straight && !crossed) return false;
        }
        return true;
    }

    private static boolean of(String allele, LivebearerGenome parent, LbLocus l) {
        return allele.equals(parent.a(l)) || allele.equals(parent.b(l));
    }

    private static void guppyMenu(GameTestHelper h, ServerPlayer sp, TankBlockEntity c) {
        TankView v = menu(h, sp, c);
        for (TankmateEntity m : mates(h, c)) {
            TankView.Inhabitant i = row(v, m);
            h.assertTrue(i.isTankmate() && i.generation() == m.generation(), "the menu's generation");
            h.assertTrue(i.tankmate().getStringOr("variety", "").equals(m.variety()), "the menu's variety " + i.tankmate().getStringOr("variety", ""));
            h.assertTrue(i.female() == m.isFemale(), "the menu's sex");
            if (i.genesVisible()) h.assertTrue(i.tankmate().getStringOr("genes", "").equals(m.genes().text()), "the menu's genes");
            else h.assertTrue(i.tankmate().getStringOr("genes", "").isEmpty(), "hidden genes");
        }
    }

    // ------------------------------------------------------------------ shrimp and snails

    private static TankBlockEntity critterTank(GameTestHelper h, int x, int z) {
        TankBlockEntity c = CritterGameTests.tank(h, x, 2, z, 3, 2, 2, 11000);
        c.getTank().setWaterQuality(1F);
        return c;
    }

    /** {@code days} of care in steps of an hour, fed (like {@code CritterGameTests.days}), the level's random seeded. */
    private static void critterDays(GameTestHelper h, TankBlockEntity c, double days, long seed) {
        h.getLevel().getRandom().setSeed(seed);
        int steps = (int) Math.round(days * 24);
        for (int i = 0; i < steps; i++) {
            c.getTank().setCritterFood(50);
            c.getTank().setWaterTemp(25F);
            c.getTank().setWaterQuality(1F);
            TankAlgae.step(h.getLevel(), c, 1.0 / 24);
            TankCritters.step(h.getLevel(), c, 1.0 / 24);
            var terrain = TankCritters.terrain(h.getLevel(), c.getTank());
            var rnd = new java.util.Random(seed + i);
            for (int m = 0; m < 4; m++) for (Critter k : c.getTank().critters()) CritterMovement.step(k, terrain, rnd);
        }
    }

    private static List<Critter> alive(TankBlockEntity c, CritterSpecies s, Predicate<Critter> p) {
        return c.getTank().critters().stream().filter(k -> k.alive() && k.species == s && p.test(k)).toList();
    }

    /** Uses {@code stack} on the front of the tank like a player (bags of small animals go in this way). */
    private static void use(GameTestHelper h, ServerPlayer sp, TankBlockEntity c, ItemStack stack) {
        BlockPos pos = c.getBlockPos();
        sp.setItemInHand(InteractionHand.MAIN_HAND, stack);
        Vec3 hit = Vec3.atCenterOf(pos).add(0, 0, -0.5);
        sp.gameMode.useItemOn(sp, h.getLevel(), stack, InteractionHand.MAIN_HAND, new BlockHitResult(hit, Direction.NORTH, pos, false));
    }

    /** Takes the small animal {@code k} out by the menu's single-animal list into a fish bag and returns the filled bag. */
    private static ItemStack bagOut(GameTestHelper h, ServerPlayer sp, TankBlockEntity c, Critter k) {
        sp.getInventory().clearContent();
        sp.getInventory().setItem(0, new ItemStack(ModItems.FISH_BAG.get()));
        Component err = TankCritters.takeOutById(h.getLevel(), c, sp, k.id, false);
        if (err != null) throw new GameTestFailure("take out " + k.species.id() + ": " + err.getString());
        for (ItemStack s : sp.getInventory().getNonEquipmentItems()) if (TankCritters.bagged(s) != null) return s.copy();
        throw new GameTestFailure("no filled bag");
    }

    /** Moves the chosen small animals into {@code to} (each in a fish bag); returns them as they arrived. */
    private static List<Critter> moveCritters(GameTestHelper h, ServerPlayer sp, TankBlockEntity from, List<Critter> chosen, TankBlockEntity to) {
        List<Critter> out = new ArrayList<>();
        for (Critter k : chosen) {
            ItemStack bag = bagOut(h, sp, from, k);
            List<Integer> before = to.getTank().critters().stream().map(x -> x.id).toList();
            use(h, sp, to, bag);
            Critter got = to.getTank().critters().stream().filter(x -> !before.contains(x.id)).findFirst().orElse(null);
            h.assertTrue(got != null, "the " + k.species.id() + " came into the new tank");
            h.assertTrue(got.genes.equals(k.genes) && got.generation == k.generation && got.female == k.female, "the bag keeps genes, generation and sex");
            out.add(got);
        }
        return out;
    }

    /** Fed care until {@code atLeast} grown young of {@code gen} live in the tank (at most {@code maxDays}). */
    private static List<Critter> critterGeneration(GameTestHelper h, TankBlockEntity c, CritterSpecies s, int gen, int atLeast, int maxDays, long seed) {
        Predicate<List<Critter>> enough = l -> l.size() >= atLeast && (!s.hasSexes() || l.stream().anyMatch(k -> k.female) && l.stream().anyMatch(k -> !k.female));
        for (int d = 0; d < maxDays && !enough.test(alive(c, s, k -> k.generation == gen && k.adult())); d++) critterDays(h, c, 1, seed + d * 1000L);
        List<Critter> young = alive(c, s, k -> k.generation == gen && k.adult());
        h.assertTrue(young.size() >= atLeast, s.id() + ": " + atLeast + " grown young of generation " + gen + ", got " + young.size() + " (all: "
                + alive(c, s, k -> true).size() + ")");
        return young;
    }

    /**
     * Red Neocaridina bred up by selection over four generations: each generation the best male and female by colour grade
     * (the menu's single-animal list, best first) go in fish bags into a new tank. The grade of the young follows the mean of
     * their parents (with a little noise and a pull to the average): the best young climb from Sakura towards Fire Red; red x red
     * stays red; the generation counts up; the menu's critter list shows the same grades and varieties.
     */
    @GameTest(template = EMPTY, timeoutTicks = 400, batch = "bettacraft_m42_gen_shrimp")
    public static void generationsOfShrimp(GameTestHelper h) {
        withConfig(() -> shrimp4(h));
        h.succeed();
    }

    private static void shrimp4(GameTestHelper h) {
        // M52: the shrimp days run within this tick and look at day or night (TankCritters.terrain): at noon in every run (the
        // shared clock of the suite made the third generation one young short now and then)
        org.overengineer.bettacraft.gametest.framework.DevTime.set(h.getLevel(), 6000);
        // M52: the starting pair is placed by the level's random too (what the tests before left in it)
        h.getLevel().getRandom().setSeed(4219L);
        ServerPlayer sp = breeder(h);
        CritterSpecies s = CritterSpecies.NEOCARIDINA;
        TankBlockEntity c = critterTank(h, 1, 2);
        Critter m0 = TankCritters.add(h.getLevel(), c, s, CritterGenes.pure(0, 0.45F), false, s.juvenileDays() + 1);
        Critter f0 = TankCritters.add(h.getLevel(), c, s, CritterGenes.pure(0, 0.45F), true, s.juvenileDays() + 1);
        h.assertTrue(m0 != null && f0 != null, "a pair");
        float parents = 0.45F;
        double firstMean = 0, lastMean = 0;
        List<String> line = new ArrayList<>();
        for (int gen = 1; gen <= 4; gen++) {
            List<Critter> young = critterGeneration(h, c, s, gen, 10, 80, 4220L + gen);
            double mean = young.stream().mapToDouble(k -> k.genes.grade()).average().orElse(0);
            float best = (float) young.stream().mapToDouble(k -> k.genes.grade()).max().orElse(0);
            for (Critter k : young) h.assertTrue(k.genes.shows(0), "red x red stays red");
            h.assertTrue(Math.abs(mean - (parents + CritterGenes.GRADE_PULL * (CritterGenes.AVERAGE_GRADE - parents))) < 0.1,
                    "the young's grade follows the parents' mean " + parents + ": " + mean);
            // the menu: the single-animal list, best first
            TankView v = menu(h, sp, c);
            for (Critter k : young) {
                TankView.CritterInfo i = v.critters.animals.stream().filter(x -> x.id() == k.id).findFirst().orElse(null);
                h.assertTrue(i != null && Math.abs(i.grade() - k.genes.grade()) < 1e-3 && i.variety().equals(k.genes.variety(s)), "the menu lists " + k.id);
            }
            if (gen == 1) firstMean = mean;
            lastMean = mean;
            line.add("F" + gen + " n=" + young.size() + " mean=" + String.format("%.2f", mean) + " best=" + String.format("%.2f", best) + " "
                    + tally(young, k -> k.genes.variety(s)));
            if (gen == 4) break;
            Critter bm = young.stream().filter(k -> !k.female).max(java.util.Comparator.comparingDouble(k -> k.genes.grade())).orElse(null);
            // a female without eggs: a berried one carries the young of whichever male mated her (the breeder waits for one)
            for (int d = 0; d < 20 && young.stream().filter(k -> k.female && k.alive()).sorted(java.util.Comparator.comparingDouble(k -> -k.genes.grade())).limit(3)
                    .allMatch(Critter::berried); d++) critterDays(h, c, 0.25, 4229L + gen * 31L + d);
            Critter bf = young.stream().filter(k -> k.female && k.alive() && !k.berried()).max(java.util.Comparator.comparingDouble(k -> k.genes.grade())).orElse(null);
            h.assertTrue(bm != null && bf != null, "a male and a female in F" + gen);
            parents = (bm.genes.grade() + bf.genes.grade()) / 2F;
            TankBlockEntity next = critterTank(h, 1 + gen * 7 % 28, gen <= 3 ? 8 : 14);
            moveCritters(h, sp, c, List.of(bm, bf), next);
            c = next;
        }
        LOG.info("[gametest] Neocaridina line: {}", line);
        h.assertTrue(parents > 0.45F, "selection raised the grade of the parents above the start (0.45): " + parents);
        h.assertTrue(lastMean > firstMean, "the young of F4 are better than those of F1: " + firstMean + " -> " + lastMean);
    }

    /**
     * Ramshorn snails (hermaphrodites, one is enough): two wild red-brown carriers of pink; their young show pink at about a
     * quarter; one pink snail caught with the aquarium net through the glass and two more by the bag into a new tank: a pure pink
     * line (every young pink), generation 2.
     */
    @GameTest(template = EMPTY, timeoutTicks = 400, batch = "bettacraft_m42_gen_snail")
    public static void generationsOfRamshorns(GameTestHelper h) {
        withConfig(() -> ramshorns(h));
        h.succeed();
    }

    private static void ramshorns(GameTestHelper h) {
        ServerPlayer sp = breeder(h);
        CritterSpecies s = CritterSpecies.RAMSHORN;
        TankBlockEntity c = critterTank(h, 1, 2);
        TankCritters.add(h.getLevel(), c, s, new CritterGenes(1, 0, 0.1F), true, s.juvenileDays() + 1);
        TankCritters.add(h.getLevel(), c, s, new CritterGenes(0, 1, 0.1F), true, s.juvenileDays() + 1);
        // the young of the carriers (selfed or crossed): the breeder sorts the grown red-brown ones out (fish bags) to keep room,
        // until two pink snails have grown
        List<Critter> f1 = new ArrayList<>();
        Predicate<Critter> grownYoung = k -> k.generation >= 1 && k.adult();
        for (int d = 0; d < 200 && alive(c, s, k -> grownYoung.test(k) && "pink".equals(k.genes.variety(s))).size() < 2; d++) {
            critterDays(h, c, 1, 4230L + d);
            for (Critter k : alive(c, s, grownYoung)) {
                if (f1.stream().noneMatch(x -> x.id == k.id)) f1.add(k);
                if (!"pink".equals(k.genes.variety(s))) bagOut(h, sp, c, k);
            }
        }
        List<Critter> pink = alive(c, s, k -> grownYoung.test(k) && "pink".equals(k.genes.variety(s)));
        LOG.info("[gametest] ramshorn F1: {} grown, {}", f1.size(), tally(f1, k -> k.genes.variety(s)));
        h.assertTrue(pink.size() >= 2, "pink young from red-brown carriers: " + tally(f1, k -> k.genes.variety(s)));
        h.assertTrue(pink.size() < f1.size(), "not all pink (a recessive colour)");
        int pinkGen = pink.stream().mapToInt(k -> k.generation).max().orElse(1);
        TankBlockEntity n = critterTank(h, 8, 2);
        // the net: aimed through the glass at a pink snail held still in the middle of the tank
        Critter k = pink.getFirst();
        TankData t = c.getTank();
        BlockPos mid = t.min().offset(1, 0, 1);
        k.x = mid.getX() + 0.5;
        k.y = mid.getY() + 0.04;
        k.z = mid.getZ() + 0.5;
        k.idle = 10000;
        k.face = Critter.FLOOR;
        // the others out of the net's line (the front block before it): left and right in the back row, held still (a
        // red-brown snail in front of the pink one was caught instead now and then)
        for (Critter o : alive(c, s, x -> x != k)) {
            o.x = t.min().getX() + (o.id % 2 == 0 ? 0.3 : 2.7);
            o.y = mid.getY() + 0.04;
            o.z = mid.getZ() + 0.5;
            o.idle = 10000;
            o.face = Critter.FLOOR;
        }
        sp.getInventory().clearContent();
        sp.getInventory().setItem(5, new ItemStack(ModItems.FISH_BAG.get())); // the net puts the catch into a bag
        sp.teleportTo(h.getLevel(), mid.getX() + 0.5, mid.getY() + 0.6 - sp.getEyeHeight() + 1.0, mid.getZ() - 3.0, java.util.Set.of(), 0F, 0F, false);
        sp.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, new Vec3(k.x, k.y + 0.04, k.z));
        int before = alive(c, s, x -> true).size();
        sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.AQUARIUM_NET.get()));
        BlockPos front = t.min().offset(1, 0, 0);
        sp.gameMode.useItemOn(sp, h.getLevel(), sp.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(front).add(0, 0, -0.5), Direction.NORTH, front, false));
        h.assertTrue(alive(c, s, x -> true).size() == before - 1, "the net caught one");
        ItemStack netted = sp.getInventory().getNonEquipmentItems().stream().filter(x -> TankCritters.bagged(x) != null).findFirst().orElseThrow().copy();
        Critter caught = Critter.loadAnimal(TankCritters.bagged(netted).getFirst(), 0, 0, 0, 0);
        h.assertTrue("pink".equals(caught.genes.variety(s)) && caught.generation == k.generation, "the net caught the pink snail: " + caught.genes.variety(s));
        use(h, sp, n, netted);
        h.assertTrue(alive(n, s, x -> true).size() == 1, "into the new tank");
        moveCritters(h, sp, c, List.of(pink.get(1)), n);
        List<Critter> f2 = critterGeneration(h, n, s, pinkGen + 1, 4, 120, 4240L);
        for (Critter y : f2) h.assertTrue("pink".equals(y.genes.variety(s)), "pink x pink is pink");
        LOG.info("[gametest] ramshorn F2: {} grown, all pink", f2.size());
    }

    // ------------------------------------------------------------------ axolotls

    private static List<Axolotl> axolotls(GameTestHelper h, TankBlockEntity c) {
        List<Axolotl> out = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(h.getLevel(), c.getTank())) if (e instanceof Axolotl a && a.isAlive()) out.add(a);
        return out;
    }

    private static TankBlockEntity axolotlTank(GameTestHelper h, int x) {
        TankBlockEntity c = ForeignGameTests.tank(h, x, 2, 3, 2, 3, 18F);
        InteriorRegion r = InteriorManager.ensureRegion(h.getLevel(), c);
        InteriorPlants.plantAt(InteriorManager.interiorLevel(h.getLevel().getServer()), r, PlantSpecies.ROTALA, 24, 0, 24, 4, 5L);
        return c;
    }

    private static int generation(Axolotl a) {
        return org.overengineer.bettacraft.platform.Platform.get().persistentData(a).getIntOr(AxolotlCare.GENERATION, 0);
    }

    /** Lays and hatches one clutch of the axolotls in {@code c} (seeded), grows the larvae up; returns the young. */
    private static List<Axolotl> axolotlBrood(GameTestHelper h, TankBlockEntity c, long seed) {
        ServerLevel level = h.getLevel();
        java.util.random.RandomGenerator rnd = new java.util.Random(seed);
        List<Axolotl> before = axolotls(h, c);
        CritterObject eggs = null;
        for (int i = 0; i < 60 && eggs == null; i++) {
            AxolotlCare.step(level, c, 1.0, rnd);
            for (CritterObject o : c.getTank().critterObjects()) if (o.kind == CritterObject.Kind.AXOLOTL_EGGS) eggs = o;
        }
        h.assertTrue(eggs != null, "the axolotls laid eggs on the plant");
        eggs.age = 10;
        AxolotlCare.step(level, c, 0.01, rnd);
        List<Axolotl> young = axolotls(h, c).stream().filter(a -> !before.contains(a)).toList();
        h.assertTrue(!young.isEmpty() && young.stream().allMatch(Axolotl::isBaby), "larvae hatched");
        return young; // larvae: they grow up once chosen (vanilla growth, sped up) and do not breed before
    }

    /**
     * Axolotls over three generations (Mendel with recessive colours, blue the rarest): a leucistic axolotl and a wild one, both
     * carrying blue; F1 wild-coloured or leucistic, now and then blue, about half carrying blue; two blue carriers by bucket into a new tank: F2 with blue
     * young (a quarter) and leucistic ones; two blue F2 into a third tank: F3 all blue. The bucket keeps the colour genes and
     * (M42 fix) the generation and the analysis of the magnifier; the pedigree counts up.
     */
    @GameTest(template = EMPTY, timeoutTicks = 400, batch = "bettacraft_m42_gen_axolotl")
    public static void generationsOfAxolotls(GameTestHelper h) {
        withConfig(() -> axolotls3(h), ServerConfig.TANK_MAX_ENTITIES, 64);
        h.succeed();
    }

    private static Axolotl axolotl(GameTestHelper h, TankBlockEntity c, CritterGenes g) {
        Vec3 p = new TankWaterVolume(c.getTank()).clamp(Vec3.atCenterOf(c.getTank().min()).add(0.5, 0, 0.5), 0.45, 0.1, 0.45);
        Axolotl a = AxolotlCare.spawn(h.getLevel(), c, g, 0, p);
        if (a == null) throw new GameTestFailure("no axolotl");
        a.setAge(0);
        return a;
    }

    private static void axolotls3(GameTestHelper h) {
        ServerPlayer sp = breeder(h);
        int blue = 1 << AxolotlCare.BLUE, lucy = 1 << AxolotlCare.LUCY;
        TankBlockEntity a = axolotlTank(h, 1);
        axolotl(h, a, new CritterGenes(lucy, lucy | blue, 0.1F)); // leucistic, carries blue
        axolotl(h, a, new CritterGenes(blue, 0, 0.1F)); // wild, carries blue
        List<Axolotl> f1 = new ArrayList<>(axolotlBrood(h, a, 4250L));
        for (int i = 1; i < 4 && f1.stream().filter(x -> AxolotlCare.genes(x).carries(AxolotlCare.BLUE)).count() < 2; i++) {
            for (Axolotl x : axolotls(h, a)) org.overengineer.bettacraft.platform.Platform.get().persistentData(x).putDouble(AxolotlCare.REST, 0);
            f1.addAll(axolotlBrood(h, a, 4250L + i));
        }
        for (Axolotl x : f1) {
            h.assertTrue(generation(x) == 1, "F1 generation 1");
            h.assertTrue(x.getVariant() == Axolotl.Variant.WILD || x.getVariant() == Axolotl.Variant.LUCY || x.getVariant() == Axolotl.Variant.BLUE,
                    "F1 colours from the parents: " + x.getVariant());
            h.assertTrue(AxolotlCare.genes(x).carries(AxolotlCare.BLUE) || x.getVariant() != Axolotl.Variant.BLUE, "blue only from two blue alleles");
        }
        LOG.info("[gametest] axolotl F1: {} young, {}", f1.size(), tally(f1, x -> x.getVariant() + ":" + AxolotlCare.genotypeText(AxolotlCare.genes(x))));
        List<Axolotl> carriers = f1.stream().filter(x -> AxolotlCare.genes(x).carries(AxolotlCare.BLUE)).limit(2).toList();
        h.assertTrue(carriers.size() == 2, "two blue carriers in F1");
        TankBlockEntity b = axolotlTank(h, 6);
        org.overengineer.bettacraft.platform.Platform.get().persistentData(carriers.getFirst()).putBoolean(AxolotlCare.ANALYSED, true);
        List<Axolotl> moved = new ArrayList<>();
        for (Axolotl x : carriers) {
            CritterGenes g = AxolotlCare.genes(x);
            boolean analysed = org.overengineer.bettacraft.platform.Platform.get().persistentData(x).getBooleanOr(AxolotlCare.ANALYSED, false);
            Axolotl y = (Axolotl) move(h, sp, a, x, b, new ItemStack(Items.WATER_BUCKET), false);
            h.assertTrue(y.isBaby(), "a larva in the bucket stays a larva");
            y.setAge(0);
            h.assertTrue(AxolotlCare.genes(y).equals(g), "the bucket keeps the genes");
            h.assertTrue(generation(y) == 1, "the bucket keeps the generation (was " + generation(y) + ")");
            h.assertTrue(org.overengineer.bettacraft.platform.Platform.get().persistentData(y).getBooleanOr(AxolotlCare.ANALYSED, false) == analysed, "and the magnifier's analysis");
            moved.add(y);
        }
        List<Axolotl> f2 = new ArrayList<>(axolotlBrood(h, b, 4260L));
        for (int i = 1; i < 8 && f2.stream().filter(x -> x.getVariant() == Axolotl.Variant.BLUE).count() < 2; i++) {
            for (Axolotl x : moved) org.overengineer.bettacraft.platform.Platform.get().persistentData(x).putDouble(AxolotlCare.REST, 0);
            f2.addAll(axolotlBrood(h, b, 4260L + i));
        }
        LOG.info("[gametest] axolotl F2: {} young, {}", f2.size(), tally(f2, x -> x.getVariant().getSerializedName()));
        for (Axolotl x : f2) h.assertTrue(generation(x) == 2, "F2 generation 2: " + generation(x));
        List<Axolotl> blues = f2.stream().filter(x -> x.getVariant() == Axolotl.Variant.BLUE).limit(2).toList();
        h.assertTrue(blues.size() == 2, "the rare blue from two carriers: " + tally(f2, x -> x.getVariant().getSerializedName()));
        TankBlockEntity c = axolotlTank(h, 11);
        for (Axolotl x : blues) ((Axolotl) move(h, sp, b, x, c, new ItemStack(Items.WATER_BUCKET), false)).setAge(0);
        List<Axolotl> f3 = axolotlBrood(h, c, 4270L);
        for (Axolotl x : f3) {
            h.assertTrue(x.getVariant() == Axolotl.Variant.BLUE, "blue x blue breeds true");
            h.assertTrue(generation(x) == 3, "F3 generation 3");
        }
        LOG.info("[gametest] axolotl F3: {} young, all blue", f3.size());
    }

    /**
     * M42 regression: an axolotl taken out of a tank with a bucket and put into another keeps its generation and the magnifier's
     * analysis (before, only the colour genes went into the bucket: every moved axolotl started a new pedigree at generation 0
     * and had to be analysed again).
     */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void axolotlBucketKeepsThePedigree(GameTestHelper h) {
        ServerPlayer sp = breeder(h);
        TankBlockEntity a = axolotlTank(h, 1), b = axolotlTank(h, 6);
        CritterGenes g = new CritterGenes(1 << AxolotlCare.GOLD, (1 << AxolotlCare.GOLD) | (1 << AxolotlCare.BLUE), 0.1F);
        Axolotl x = axolotl(h, a, g);
        org.overengineer.bettacraft.platform.Platform.get().persistentData(x).putInt(AxolotlCare.GENERATION, 3);
        org.overengineer.bettacraft.platform.Platform.get().persistentData(x).putBoolean(AxolotlCare.ANALYSED, true);
        ItemStack bucket = takeOut(h, sp, a, x, new ItemStack(Items.WATER_BUCKET), false);
        h.assertTrue(bucket.is(Items.AXOLOTL_BUCKET), "an axolotl bucket");
        Axolotl y = (Axolotl) putIn(h, sp, b, bucket);
        h.assertTrue(y.getVariant() == Axolotl.Variant.GOLD && AxolotlCare.genes(y).equals(g), "the colour genes");
        h.assertTrue(generation(y) == 3, "the generation: " + generation(y));
        h.assertTrue(org.overengineer.bettacraft.platform.Platform.get().persistentData(y).getBooleanOr(AxolotlCare.ANALYSED, false), "the magnifier's analysis");
        h.succeed();
    }

    // ------------------------------------------------------------------ frogs and turtles

    private static ServerLevel interior(GameTestHelper h) {
        return InteriorManager.interiorLevel(h.getLevel().getServer());
    }

    private static InteriorRegion floor(GameTestHelper h, TankBlockEntity c, net.minecraft.world.level.block.state.BlockState s) {
        InteriorRegion r = InteriorManager.ensureRegion(h.getLevel(), c);
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) {
            org.overengineer.bettacraft.tank.interior.Substrate.set(interior(h), new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), s, false);
        }
        return r;
    }

    /** A 3x3x3 paludarium: land of {@code surface} over the half x >= 24 up to 24 interior blocks, a third of water. */
    private static TankBlockEntity paludarium(GameTestHelper h, int x, int z, net.minecraft.world.level.block.state.BlockState surface, float air, int humidity) {
        return paludarium(h, x, z, 3, 3, surface, air, humidity);
    }

    private static TankBlockEntity paludarium(GameTestHelper h, int x, int z, int sx, int sz, net.minecraft.world.level.block.state.BlockState surface, float air,
                                              int humidity) {
        TankBlockEntity c = CritterGameTests.tank(h, x, 2, z, sx, 3, sz, 0);
        InteriorRegion r = floor(h, c, Blocks.SAND.defaultBlockState());
        for (int ix = r.sizeX() / 2; ix < r.sizeX(); ix++) for (int iz = 0; iz < r.sizeZ(); iz++) for (int y = 0; y < 24; y++) {
            interior(h).setBlock(r.toWorld(ix, y, iz), y == 23 ? surface : Blocks.DIRT.defaultBlockState(), 3);
        }
        org.overengineer.bettacraft.tank.TankInteractions.changeWater(h.getLevel(), c, sx * sz * 1000L);
        TankClimate.step(h.getLevel(), c, 0.01);
        TankData t = c.getTank();
        if (t.terrariumMode() != TerrariumMode.PALUDARIUM) throw new GameTestFailure("no paludarium: " + t.terrariumMode());
        t.setClimate(new Climate.State(air, air, air + 12, humidity, 5, 0));
        t.setWaterTemp(air);
        return c;
    }

    private static HerpEntity herp(GameTestHelper h, TankBlockEntity c, HerpSpecies s, boolean female, int variety) {
        HerpEntity e = ModEntities.herp(s).create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (e == null) throw new GameTestFailure("no " + s.id());
        e.setFemale(female);
        e.setVariety(variety);
        ItemStack stack = e.getBucketItemStack();
        e.saveToBucketTag(stack);
        e.discard();
        Entity in = putIn(h, breeder(h), c, stack);
        return (HerpEntity) in;
    }

    private static List<HerpEntity> herps(GameTestHelper h, TankBlockEntity c) {
        List<HerpEntity> out = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(h.getLevel(), c.getTank())) if (e instanceof HerpEntity y && y.isAlive()) out.add(y);
        return out;
    }

    /**
     * One clutch of the fed pair in {@code c}: the female goes to the nest she found and lays (her random seeded, so the
     * clutch's seed is fixed), the eggs incubate at {@code nest} °C ground temperature until they hatch, the young grow up.
     */
    private static List<HerpEntity> herpClutch(GameTestHelper h, TankBlockEntity c, HerpEntity f, float nest, int humidity, long seed) {
        ServerLevel level = h.getLevel();
        TankData t = c.getTank();
        Climate.State base = t.climate();
        List<HerpEntity> before = herps(h, c);
        for (HerpEntity y : before) y.feed();
        f.getRandom().setSeed(seed);
        f.setNextLayForTest(0);
        HerpBreeding.step(level, c, 0.001);
        h.assertTrue(f.nestTarget() != null, f.species().id() + ": she found a nest; log " + t.log().stream().map(TankData.LogEntry::key).toList());
        f.snapTo(f.nestTarget().x, f.nestTarget().y, f.nestTarget().z);
        HerpBreeding.step(level, c, 0.001);
        h.assertTrue(t.herpClutches().size() == 1, "one clutch");
        HerpClutch k = t.herpClutches().getFirst();
        t.setClimate(new Climate.State(nest, nest, nest + 8, humidity, 5, 0));
        for (int i = 0; i < 200 && !t.herpClutches().isEmpty(); i++) HerpBreeding.step(level, c, 0.25);
        t.setClimate(base);
        h.assertTrue(t.herpClutches().isEmpty(), "hatched");
        List<HerpEntity> young = herps(h, c).stream().filter(y -> !before.contains(y)).toList();
        h.assertTrue(young.size() == k.eggs, "every egg hatched: " + young.size() + "/" + k.eggs);
        for (HerpEntity y : young) h.assertTrue(y.isBaby() && y.generation() == k.generation, "a hatchling of generation " + k.generation);
        return young; // hatchlings: the chosen ones grow up in their new tank
    }

    /**
     * Dart frogs over three generations: a D. auratus pair lays onto the moss, the tadpoles swim, grow up (froglets, adults);
     * an F1 pair by box into a new paludarium lays again: F2 and F3 are all auratus (the varieties are species of their own),
     * the generation counts up, the menu lists them.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "bettacraft_m42_gen_frog")
    public static void generationsOfDartFrogs(GameTestHelper h) {
        ServerPlayer sp = breeder(h);
        TankBlockEntity c = paludarium(h, 1, 2, Blocks.MOSS_BLOCK.defaultBlockState(), 24F, 90);
        HerpEntity f = herp(h, c, HerpSpecies.DART_FROG, true, 1), m = herp(h, c, HerpSpecies.DART_FROG, false, 1);
        int attempt = 0;
        for (int gen = 1; gen <= 3; gen++) {
            h.assertTrue(attempt < 12, "a clutch with both sexes in 12 tries");
            List<HerpEntity> young = herpClutch(h, c, f, 24F, 90, 4280L + gen * 100 + attempt++);
            for (HerpEntity y : young) h.assertTrue(y.variety() == 1, "auratus young: " + y.variety());
            TankView v = menu(h, sp, c);
            for (HerpEntity y : young) h.assertTrue(!row(v, y).adult() && row(v, y).sexed() && row(v, y).female() == y.isFemale(), "the menu shows the young frog and its sex");
            LOG.info("[gametest] dart frogs F{}: {} young, sexes {}", gen, young.size(), tally(young, y -> y.isFemale() ? "F" : "M"));
            if (gen == 3) break;
            HerpEntity nf = young.stream().filter(HerpEntity::isFemale).findFirst().orElse(null);
            HerpEntity nm = young.stream().filter(y -> !y.isFemale()).findFirst().orElse(null);
            if (nf == null || nm == null) { // one sex only: the young are sorted out, the pair lays again
                for (HerpEntity y : young) takeOut(h, sp, c, y, new ItemStack(Items.BUCKET), false);
                gen--;
                continue;
            }
            TankBlockEntity n = paludarium(h, 1 + gen * 5, 2, Blocks.MOSS_BLOCK.defaultBlockState(), 24F, 90);
            f = (HerpEntity) move(h, sp, c, nf, n, new ItemStack(Items.BUCKET), false);
            m = (HerpEntity) move(h, sp, c, nm, n, new ItemStack(Items.BUCKET), false);
            h.assertTrue(f.generation() == gen && m.generation() == gen && f.isFemale() && !m.isFemale() && f.variety() == 1, "the container keeps the frog");
            h.assertTrue(f.isBaby() && f.isTadpole() == nf.isTadpole(), "and its age");
            f.setAge(0); // grown up (sped up)
            m.setAge(0);
            c = n;
        }
        h.succeed();
    }

    /**
     * Slider turtles (temperature-dependent sex, pivot 29.2 °C) over three generations: the P pair's clutches in a warm nest
     * (32 °C) hatch as females, in a cool one (26 °C) as males; an F1 female and an F1 male by bucket into a new paludarium,
     * their F2 again by the nest temperature; the variety (elegans or scripta) comes from a parent, the generation counts up.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "bettacraft_m42_gen_turtle")
    public static void generationsOfSliderTurtles(GameTestHelper h) {
        ServerPlayer sp = breeder(h);
        TankBlockEntity c = paludarium(h, 1, 2, 4, 4, Blocks.SAND.defaultBlockState(), 25F, 60);
        HerpEntity f = herp(h, c, HerpSpecies.SLIDER, true, 0), m = herp(h, c, HerpSpecies.SLIDER, false, 1);
        for (int gen = 1; gen <= 3; gen++) {
            int fv = f.variety(), mv = m.variety();
            List<HerpEntity> warm = herpClutch(h, c, f, 32F, 60, 4290L + gen);
            for (HerpEntity y : warm) h.assertTrue(y.isFemale(), "32 °C: females");
            List<HerpEntity> cool = herpClutch(h, c, f, 26F, 60, 4295L + gen);
            for (HerpEntity y : cool) h.assertTrue(!y.isFemale(), "26 °C: males");
            List<HerpEntity> all = new ArrayList<>(warm);
            all.addAll(cool);
            for (HerpEntity y : all) h.assertTrue(y.variety() == fv || y.variety() == mv, "the variety of a parent");
            TankView v = menu(h, sp, c);
            for (HerpEntity y : all) h.assertTrue(!row(v, y).adult() && row(v, y).sexed() && row(v, y).female() == y.isFemale(), "the menu lists the hatchling with its sex (M42)");
            LOG.info("[gametest] sliders F{}: warm {}, cool {}, varieties {}", gen, warm.size(), cool.size(), tally(all, y -> String.valueOf(y.variety())));
            if (gen == 3) break;
            TankBlockEntity n = paludarium(h, 1 + gen * 6, 2, 4, 4, Blocks.SAND.defaultBlockState(), 25F, 60);
            f = (HerpEntity) move(h, sp, c, warm.getFirst(), n, new ItemStack(Items.BUCKET), false);
            m = (HerpEntity) move(h, sp, c, cool.getFirst(), n, new ItemStack(Items.BUCKET), false);
            h.assertTrue(f.isFemale() && !m.isFemale() && f.generation() == gen && m.generation() == gen, "the bucket keeps sex and generation");
            f.setAge(0); // grown up (sped up)
            m.setAge(0);
            c = n;
        }
        h.succeed();
    }

    /**
     * Leopard geckos (FMF: cool and very hot nests give females, about 32 °C males) over two generations in dry terraria by the
     * box; their captive morphs (tangerine, albino) are rare: about 4 % of the young of a big seeded clutch.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "bettacraft_m42_gen_gecko")
    public static void leopardGeckoMorphsAreRare(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = CritterGameTests.tank(h, 1, 2, 2, 3, 3, 3, 0);
        floor(h, c, Blocks.SAND.defaultBlockState());
        TankClimate.step(level, c, 0.01);
        TankData t = c.getTank();
        h.assertTrue(t.terrariumMode() == TerrariumMode.TERRARIUM, "a terrarium");
        t.setClimate(new Climate.State(25, 27, 35, 40, 0, 0));
        BlockPos egg = InteriorManager.ensureRegion(level, c).toWorld(10, 0, 10);
        int morphs = 0, n = 0;
        for (int round = 0; round < 8; round++) {
            for (HerpEntity y : herps(h, c)) {
                t.inhabitants().remove(y.getUUID());
                y.discard();
            }
            interior(h).setBlock(egg, org.overengineer.bettacraft.registry.ModBlocks.TURTLE_EGGS.get().withEggs(2), 3);
            HerpClutch k = new HerpClutch(t.nextHerpClutchId(), HerpSpecies.LEOPARD_GECKO, 50, new Vec3(t.min().getX() + 1, t.min().getY() + 0.05, t.min().getZ() + 1),
                    egg, 4300L + round);
            t.herpClutches().add(k);
            for (int i = 0; i < 60 && !t.herpClutches().isEmpty(); i++) HerpBreeding.step(level, c, 0.25);
            for (HerpEntity y : herps(h, c)) {
                n++;
                if (y.variety() >= HerpSpecies.LEOPARD_GECKO.wildVarieties()) morphs++;
            }
        }
        LOG.info("[gametest] leopard gecko morphs: {} of {} young", morphs, n);
        h.assertTrue(n >= 300, "many young: " + n);
        double share = morphs / (double) n;
        h.assertTrue(share > 0.01 && share < 0.08, "morphs are rare (about 4 %): " + share);
        h.succeed();
    }

    // ------------------------------------------------------------------ bamboo sharks and stingrays

    private static TankmateEntity big(GameTestHelper h, TankBlockEntity c, TankmateSpecies s, boolean female) {
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        Vec3 p = w.clamp(w.box().getCenter(), 0.6, 0.1, 0.2);
        TankmateEntity e = TankmateBreeding.spawn(h.getLevel(), c, s, TankmateGenes.wild(s), female, LifeStage.ADULT, 0, p);
        if (e == null) throw new GameTestFailure("could not spawn " + s.id());
        e.health().feed();
        return e;
    }

    private static List<TankmateEntity> of(GameTestHelper h, TankBlockEntity c, TankmateSpecies s) {
        return mates(h, c).stream().filter(m -> m.species() == s).toList();
    }

    /** Young of {@code s} and generation {@code gen} in {@code c}: fed steps until born, then grown up. */
    private static List<TankmateEntity> bigYoung(GameTestHelper h, TankBlockEntity c, TankmateSpecies s, int gen, float temp, long seed) {
        h.getLevel().getRandom().setSeed(seed);
        for (int d = 0; d < 400 && of(h, c, s).stream().noneMatch(m -> m.generation() == gen); d++) {
            for (Clutch k : c.getTank().clutches()) if (k.species == s) k.age = Math.max(k.age, k.totalDays() - 0.5);
            for (TankmateEntity m : of(h, c, s)) if (m.isGravid()) m.setGravid(TankmateBreeding.gestationDays(s) + 0.1, m.gravidFather());
            tankmateDays(h, c, 0.5, 0.5, temp);
        }
        List<TankmateEntity> young = of(h, c, s).stream().filter(m -> m.generation() == gen).toList();
        h.assertTrue(!young.isEmpty(), s.id() + ": young of generation " + gen);
        int days = 0;
        for (; days < 400 && !young.stream().allMatch(TankmateEntity::isAdult); days++) tankmateDays(h, c, 1.0, 1.0, temp);
        young = young.stream().filter(TankmateEntity::isAlive).toList();
        h.assertTrue(!young.isEmpty() && young.stream().allMatch(TankmateEntity::isAdult), s.id() + ": the young grew up: " + tally(young, m -> m.lifeStage().id()));
        LOG.info("[gametest] {}: grown in {} days", s.id(), days);
        return young;
    }

    /**
     * Bamboo sharks (egg cases) and motoro stingrays (live young) over three generations by the size rule: each generation an
     * adult male and female go by transport tub into a new tank of room enough, lay or bear their young, and those grow up; the
     * generation counts up, the size rule holds, the menu lists them.
     */
    @GameTest(template = EMPTY, timeoutTicks = 400, batch = "bettacraft_m42_gen_big")
    public static void generationsOfSharksAndStingrays(GameTestHelper h) {
        withConfig(() -> {
            bigLine(h, TankmateSpecies.BAMBOO_SHARK, new int[]{16, 4, 8}, org.overengineer.bettacraft.tank.Salinity.SEA, 25F, 0);
            bigLine(h, TankmateSpecies.MOTORO_STINGRAY, new int[]{10, 2, 5}, 0F, 27F, 18);
        }, ServerConfig.TANKMATES_FRY_EATEN, 0.0);
        h.succeed();
    }

    private static void bigLine(GameTestHelper h, TankmateSpecies s, int[] size, float salt, float temp, int z) {
        ServerPlayer sp = breeder(h);
        TankBlockEntity c = BigTankGameTests.tank(h, 0, 1, z, size[0], size[1], size[2], salt);
        c.getTank().setWaterTemp(temp);
        c.getTank().setTargetTemp(temp);
        big(h, c, s, false);
        big(h, c, s, true);
        TankBlockEntity first = c;
        for (int gen = 1; gen <= 3; gen++) {
            List<TankmateEntity> young = bigYoung(h, c, s, gen, temp, 4310L + gen + z);
            List<Entity> live = TankInhabitants.liveInhabitants(h.getLevel(), c.getTank());
            h.assertTrue(org.overengineer.bettacraft.big.BigAnimals.waterOfOthers(live, null) <= org.overengineer.bettacraft.big.BigAnimals.capacityBlocks(c.getTank()) + 1e-6,
                    "the size rule holds");
            TankView v = menu(h, sp, c);
            for (TankmateEntity y : young) h.assertTrue(row(v, y).generation() == gen, "the menu's generation");
            LOG.info("[gametest] {} F{}: {} young {}", s.id(), gen, young.size(), tally(young, y -> y.isFemale() ? "F" : "M"));
            if (gen == 3) break;
            // the next pair: a young one and a partner of the other sex (a parent if all young are of one sex)
            TankmateEntity y0 = young.getFirst();
            TankmateEntity partner = of(h, c, s).stream().filter(x -> x != y0 && x.isFemale() != y0.isFemale())
                    .sorted(java.util.Comparator.comparingInt(x -> -x.generation())).findFirst().orElseThrow();
            TankBlockEntity n = gen == 1 ? BigTankGameTests.tank(h, 0, 1, z + size[2] + 1, size[0], size[1], size[2], salt) : first;
            n.getTank().setWaterTemp(temp);
            n.getTank().setTargetTemp(temp);
            TankmateEntity a = (TankmateEntity) move(h, sp, c, y0, n, new ItemStack(ModItems.TRANSPORT_TUB.get()), true);
            TankmateEntity b = (TankmateEntity) move(h, sp, c, partner, n, new ItemStack(ModItems.TRANSPORT_TUB.get()), true);
            h.assertTrue(a.generation() == y0.generation() && b.generation() == partner.generation() && a.isAdult(), "the tub keeps the generation");
            for (TankmateEntity rest : of(h, c, s)) { // the old tank is emptied (its room is needed for the next one)
                c.getTank().inhabitants().remove(rest.getUUID());
                rest.discard();
            }
            c = n;
        }
    }

    // ------------------------------------------------------------------ animals of other mods with a profile

    /**
     * Tropical fish by a test profile (livebearer, inherit Variant) over three generations: two patterns breed young of a
     * parent's pattern; two young of the same pattern by fish bag into a new tank breed only that pattern; a third tank the same.
     * Fintastic guppies (if installed) by their shipped profile: two generations, the young with the fin and tail colours of
     * their parents, moved by bag.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "bettacraft_m42_gen_foreign")
    public static void generationsOfForeignAnimals(GameTestHelper h) {
        ForeignGameTests.withProfile(ForeignGameTests.TROPICAL, () -> {
            ServerPlayer sp = breeder(h);
            ServerLevel level = h.getLevel();
            TankBlockEntity c = ForeignGameTests.tank(h, 1, 2, 3, 2, 2, 25F);
            ForeignGameTests.add(h, c, EntityType.TROPICAL_FISH, t -> t.putInt("Variant", 65536));
            ForeignGameTests.add(h, c, EntityType.TROPICAL_FISH, t -> t.putInt("Variant", 117506305));
            java.util.Set<Integer> parents = java.util.Set.of(65536, 117506305);
            for (int gen = 1; gen <= 3; gen++) {
                List<Entity> before = new ArrayList<>(TankInhabitants.liveInhabitants(level, c.getTank()));
                level.getRandom().setSeed(4320L + gen);
                for (int i = 0; i < 20 && TankInhabitants.liveInhabitants(level, c.getTank()).size() == before.size(); i++) {
                    ForeignCare.feed(livings(before), 2, null);
                    ForeignBreeding.step(level, c, 0.01);
                    ForeignBreeding.step(level, c, 1.1);
                }
                List<Entity> young = TankInhabitants.liveInhabitants(level, c.getTank()).stream().filter(e -> !before.contains(e)).toList();
                h.assertTrue(!young.isEmpty(), "young tropical fish in generation " + gen);
                for (Entity e : young) h.assertTrue(parents.contains(variant(e)), "the pattern of a parent: " + variant(e));
                LOG.info("[gametest] tropical F{}: {}", gen, tally(young, e -> String.valueOf(variant(e))));
                if (gen == 3) break;
                int keep = variant(young.getFirst());
                List<Entity> pair = young.stream().filter(e -> variant(e) == keep).limit(2).toList();
                if (pair.size() < 2) pair = List.of(young.getFirst(), before.stream().filter(e -> variant(e) == keep).findFirst().orElseThrow());
                TankBlockEntity n = ForeignGameTests.tank(h, 1 + gen * 5, 2, 3, 2, 2, 25F);
                for (Entity e : pair) h.assertTrue(variant(move(h, sp, c, e, n, new ItemStack(ModItems.FISH_BAG.get()), true)) == keep, "the bag keeps the pattern");
                parents = java.util.Set.of(keep);
                c = n;
            }
        });
        fintasticGuppies(h);
        h.succeed();
    }

    /**
     * Aquaculture's box turtle (if installed) by its shipped profile over two generations: a warm nest (33 °C) hatches females, a
     * cool one (24 °C) males; a young female and a young male go by terrarium box into a new terrarium, keep their sex (in the
     * persistent data) and breed there in turn.
     */
    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "bettacraft_m42_gen_foreign_turtle")
    public static void generationsOfAquacultureBoxTurtles(GameTestHelper h) {
        if (!org.overengineer.bettacraft.platform.Platform.get().isModLoaded("aquaculture")) {
            LOG.info("aquaculture not installed: its box turtle generations skipped");
            h.succeed();
            return;
        }
        ServerLevel level = h.getLevel();
        ServerPlayer sp = breeder(h);
        EntityType<?> box = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("aquaculture:box_turtle"));
        Climate.State home = new Climate.State(28, 28, 40, 75, 5, 0);
        TankBlockEntity c = ForeignTerrariumGameTests.terrarium(h, 1, 18, 6, 3, 4);
        c.getTank().setClimate(home);
        LivingEntity f = ForeignTerrariumGameTests.put(h, c, ForeignTerrariumGameTests.container(h, box, false, false));
        LivingEntity m = ForeignTerrariumGameTests.put(h, c, ForeignTerrariumGameTests.container(h, box, false, false));
        ForeignBreeding.setFemale(f, true);
        ForeignBreeding.setFemale(m, false);
        for (int gen = 1; gen <= 2; gen++) {
            List<LivingEntity> sexes = new ArrayList<>();
            for (float nest : new float[]{33F, 24F}) {
                level.getRandom().setSeed(4350L + gen * 10 + (long) nest);
                c.getTank().setClimate(home);
                List<LivingEntity> before = ForeignTerrariumGameTests.ofType(h, c, box);
                ForeignTerrariumGameTests.layEggs(h, c, box);
                c.getTank().setClimate(new Climate.State(nest, nest, 40, 75, 5, 0));
                ForeignBreeding.step(level, c, 12.0);
                List<LivingEntity> young = ForeignTerrariumGameTests.ofType(h, c, box).stream().filter(e -> !before.contains(e)).toList();
                h.assertTrue(!young.isEmpty(), "hatchlings at " + nest + " °C in generation " + gen);
                for (LivingEntity y : young) h.assertTrue(y.isBaby() && ForeignBreeding.female(y) == nest > 30, "the nest temperature decides the sex");
                sexes.add(young.getFirst());
            }
            c.getTank().setClimate(home);
            LOG.info("[gametest] box turtles F{}: a warm and a cool clutch hatched", gen);
            if (gen == 2) break;
            TankBlockEntity n = ForeignTerrariumGameTests.terrarium(h, 9, 18, 6, 3, 4);
            n.getTank().setClimate(home);
            f = (LivingEntity) move(h, sp, c, sexes.get(0), n, new ItemStack(ModItems.TERRARIUM_BOX.get()), true);
            m = (LivingEntity) move(h, sp, c, sexes.get(1), n, new ItemStack(ModItems.TERRARIUM_BOX.get()), true);
            h.assertTrue(ForeignBreeding.female(f) && !ForeignBreeding.female(m), "the box keeps the sex");
            ((net.minecraft.world.entity.Mob) f).setBaby(false); // grown up (sped up)
            ((net.minecraft.world.entity.Mob) m).setBaby(false);
            c = n;
        }
        h.succeed();
    }

    public static List<LivingEntity> livings(List<Entity> l) {
        List<LivingEntity> out = new ArrayList<>();
        for (Entity e : l) if (e instanceof LivingEntity le) out.add(le);
        return out;
    }

    /** M42 part c: a clean freshwater tank of the foreign animal tests (for the integration suite). */
    public static TankBlockEntity waterTank(GameTestHelper h, int x, int z, int sx, int sy, int sz, float temp) {
        return ForeignGameTests.tank(h, x, z, sx, sy, sz, temp);
    }

    /** An animal of {@code type} with this data as an inhabitant of the tank (for the integration suite). */
    public static LivingEntity addAnimal(GameTestHelper h, TankBlockEntity c, EntityType<?> type, java.util.function.Consumer<CompoundTag> data) {
        return ForeignGameTests.add(h, c, type, data);
    }

    /** A plant (rotala) in the interior of the tank. */
    public static void plant(GameTestHelper h, TankBlockEntity c, int x, int z) {
        InteriorRegion r = InteriorManager.ensureRegion(h.getLevel(), c);
        InteriorPlants.plantAt(InteriorManager.interiorLevel(h.getLevel().getServer()), r, PlantSpecies.ROTALA, x, 0, z, 4, x * 31L + z);
    }

    public static int variant(Entity e) {
        return NbtCompat.saveEntityWithoutId(e).getIntOr("Variant", 0);
    }

    private static void fintasticGuppies(GameTestHelper h) {
        if (!org.overengineer.bettacraft.platform.Platform.get().isModLoaded("fintastic")) {
            LOG.info("fintastic not installed: its guppy generations skipped");
            return;
        }
        ServerLevel level = h.getLevel();
        ServerPlayer sp = breeder(h);
        EntityType<?> guppy = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("fintastic:guppy"));
        TankBlockEntity c = ForeignGameTests.tank(h, 1, 10, 3, 2, 2, 25F);
        ForeignGameTests.add(h, c, guppy, t -> {
            t.putInt("FinColor", 1);
            t.putInt("TailColor", 2);
        });
        ForeignGameTests.add(h, c, guppy, t -> {
            t.putInt("FinColor", 5);
            t.putInt("TailColor", 6);
        });
        java.util.Set<Integer> fins = java.util.Set.of(1, 5), tails = java.util.Set.of(2, 6);
        for (int gen = 1; gen <= 2; gen++) {
            List<Entity> before = new ArrayList<>(TankInhabitants.liveInhabitants(level, c.getTank()));
            level.getRandom().setSeed(4330L + gen);
            for (int i = 0; i < 2000 && TankInhabitants.liveInhabitants(level, c.getTank()).size() == before.size(); i++) {
                ForeignBreeding.step(level, c, 0.05);
                ForeignCare.feed(livings(before), 2, null);
            }
            List<Entity> young = TankInhabitants.liveInhabitants(level, c.getTank()).stream().filter(e -> !before.contains(e)).toList();
            h.assertTrue(!young.isEmpty(), "young Fintastic guppies in generation " + gen);
            for (Entity e : young) {
                CompoundTag t = NbtCompat.saveEntityWithoutId(e);
                h.assertTrue(fins.contains(t.getIntOr("FinColor", 0)) && tails.contains(t.getIntOr("TailColor", 0)), "the colours of a parent: " + t.getIntOr("FinColor", 0) + "/" + t.getIntOr("TailColor", 0));
                if (e instanceof AgeableMob a) a.setAge(0);
                else if (e instanceof net.minecraft.world.entity.Mob mob) mob.setBaby(false); // Fintastic's own age (BreedableWaterAnimal)
            }
            if (gen == 2) break;
            // two young into a new tank by fish bag (or one young and a parent)
            List<Entity> pair = young.size() >= 2 ? young.subList(0, 2) : List.of(young.getFirst(), before.getFirst());
            TankBlockEntity n = ForeignGameTests.tank(h, 6, 10, 3, 2, 2, 25F);
            fins = new java.util.HashSet<>();
            tails = new java.util.HashSet<>();
            for (Entity e : pair) {
                Entity m = move(h, sp, c, e, n, new ItemStack(ModItems.FISH_BAG.get()), true);
                CompoundTag t = NbtCompat.saveEntityWithoutId(m);
                fins.add(t.getIntOr("FinColor", 0));
                tails.add(t.getIntOr("TailColor", 0));
            }
            c = n;
        }
    }
}
