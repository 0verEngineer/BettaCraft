package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaCare;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.equipment.FeederBlockEntity;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.genetics.BettaLoci;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.BubbleNest;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankBreeding;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;

/** M2: migration, breeding flow, life stages and fry food, fights, predators (run with {@code gradlew runGameTestServer}). */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BreedingGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private BreedingGameTests() {}

    private static TankBlockEntity tank(GameTestHelper h, int sx, int sy, int sz, long water) {
        for (int a = 0; a < sx; a++)
            for (int b = 0; b < sy; b++)
                for (int c = 0; c < sz; c++) {
                    BlockPos p = new BlockPos(2 + a, 1 + b, 2 + c);
                    h.setBlock(p, ModBlocks.TANK.get().defaultBlockState());
                    TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(p), null);
                }
        TankBlockEntity c = TankMultiblock.getController(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 2)));
        if (c == null) throw new GameTestAssertException("tank did not form");
        TankInteractions.changeWater(h.getLevel(), c, water);
        // warm water for breeding: a large heater keeps the target temperature
        c.getTank().legacyHeaters().setItem(0, new ItemStack(ModItems.HEATER_LARGE.get()));
        c.getTank().setTargetTemp(27.0F);
        c.getTank().setWaterTemp(27.0F);
        return c;
    }

    private static BettaEntity insert(GameTestHelper h, TankBlockEntity c, BettaData d) {
        ItemStack bucket = BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d);
        Component err = TankInhabitants.insert(h.getLevel(), c, bucket, ModEntities.BETTA.get(), null, false);
        if (err != null) throw new GameTestAssertException("insert failed: " + err.getString());
        for (BettaEntity b : bettas(h, c)) if (b.getBettaData().fishId().equals(d.fishId())) return b;
        throw new GameTestAssertException("inserted betta not found");
    }

    private static BettaEntity adult(GameTestHelper h, TankBlockEntity c, boolean female, long seed) {
        return insert(h, c, BettaData.create(BettaGenotype.randomDomestic(RandomSource.create(seed)), female, 0));
    }

    private static List<BettaEntity> bettas(GameTestHelper h, TankBlockEntity c) {
        List<BettaEntity> out = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(h.getLevel(), c.getTank())) if (e instanceof BettaEntity b && b.isAlive()) out.add(b);
        return out;
    }

    private static long count(List<BettaEntity> l, LifeStage s) {
        return l.stream().filter(b -> b.lifeStage() == s).count();
    }

    private static boolean logged(TankData t, String key) {
        return t.log().stream().anyMatch(e -> e.key().equals(key));
    }

    // ------------------------------------------------------------------ migration

    /** A betta bucket written by M1 (schema 1, one value per locus) loads as a homozygous genotype. */
    @GameTest(template = EMPTY)
    public static void m1BucketMigratesToHomozygousGenotype(GameTestHelper h) throws Exception {
        TankBlockEntity c = tank(h, 2, 2, 2, 6000);
        CompoundTag v1 = TagParser.parseTag("{betta:{schema:1,genome:{species:\"bettacraft:betta\",shape:\"veil\",pectoral:\"normal\","
                + "body_color:\"red\",fin_color:\"black\",pattern:\"koi\",pattern_color:\"white\",iridescence:\"low\",iridescence_color:\"teal\"},"
                + "sex:\"female\",stage:\"adult\",generation:2,born:5L,health:{}},Health:6.0f}");
        ItemStack bucket = new ItemStack(ModItems.BETTA_BUCKET.get());
        bucket.set(DataComponents.BUCKET_ENTITY_DATA, CustomData.of(v1));
        Component err = TankInhabitants.insert(h.getLevel(), c, bucket, ModEntities.BETTA.get(), null, false);
        h.assertTrue(err == null, "insert of an M1 bucket");
        BettaEntity b = bettas(h, c).getFirst();
        BettaGenotype g = b.genotype();
        h.assertTrue(g.pair(BettaLoci.FINFORM).homozygous() && "veil".equals(g.pair(BettaLoci.FINFORM).a()), "finform veil/veil: " + g);
        h.assertTrue("koi".equals(b.genome().pattern()) && "red".equals(b.genome().bodyColor()), "phenotype unchanged");
        h.assertTrue(b.isFemale() && b.isAdult() && b.getBettaData().generation() == 2, "other data kept");
        CompoundTag saved = b.getBettaData().save();
        h.assertTrue(saved.getInt("schema") == 3 && saved.getCompound("genome").getList("finform", 8).size() == 2, "saved as v3");
        h.succeed();
    }

    // ------------------------------------------------------------------ breeding

    /**
     * The whole realistic flow (PLAN 5.4): nest building -> spawning -> the male chases the female (she loses health)
     * -> female taken out -> larvae -> fry entities with pedigree -> the male eats fry.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void breedingFlow(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 3, 2, 2, 12000);
        BettaEntity male = adult(h, c, false, 11);
        BettaEntity female = adult(h, c, true, 12);
        TankData t = c.getTank();
        // conditions: fed, 27 C -> readiness (0.25 d), nest building (0.25 d), spawning
        for (int i = 0; i < 40 && (t.nest() == null || !t.nest().hasEggs()); i++) TankBreeding.step(level, c, 0.05);
        h.assertTrue(logged(t, "log.bettacraft.nest_started") && logged(t, "log.bettacraft.nest_built"), "nest started and built");
        BubbleNest n = t.nest();
        h.assertTrue(n != null && n.hasEggs(), "eggs laid");
        h.assertTrue(n.eggCount() >= 8 && n.eggCount() <= 22, "8-20 eggs, got " + n.eggCount());
        h.assertTrue(logged(t, "log.bettacraft.spawned"), "spawning logged");
        h.assertTrue(female.breeding().cooldown() > 0, "female rests after spawning");
        h.assertTrue(TankInhabitants.breakBlockedReason(level, c.getBlockPos()).isPresent(), "nest with eggs blocks breaking");
        h.assertTrue(TankBreeding.warnings(level, c).contains("remove_female"), "menu warns: take the female out");
        // the male defends the nest: the female loses health
        float hp = female.getHealth();
        for (int i = 0; i < 10 && female.getHealth() >= hp; i++) TankBreeding.step(level, c, 0.1);
        h.assertTrue(female.getHealth() < hp, "chased female lost health (" + hp + " -> " + female.getHealth() + ")");
        h.assertTrue(logged(t, "log.bettacraft.female_chased"), "chase logged");
        // take her out (as the player would)
        t.inhabitants().remove(female.getUUID());
        female.discard();
        // eggs -> larvae -> fry (0.5 + 0.5 days)
        for (int i = 0; i < 30 && t.nest() != null; i++) TankBreeding.step(level, c, 0.05);
        h.assertTrue(logged(t, "log.bettacraft.larvae"), "larvae hatched");
        h.assertTrue(t.nest() == null && logged(t, "log.bettacraft.fry_free"), "fry swim free, nest gone");
        List<BettaEntity> all = bettas(h, c);
        long fry = count(all, LifeStage.FRY);
        h.assertTrue(fry >= 4, "at least 60 % survived, fry: " + fry);
        BettaEntity one = all.stream().filter(b -> b.lifeStage() == LifeStage.FRY).findFirst().orElseThrow();
        h.assertTrue(one.getBettaData().father().isPresent() && one.getBettaData().mother().isPresent()
                && one.getBettaData().generation() >= 1, "pedigree");
        h.assertTrue(one.getBbWidth() < male.getBbWidth() * 0.5F, "fry are small");
        h.assertTrue(TankInhabitants.countAdults(new ArrayList<>(all)) == 1, "only adults count for stocking");
        h.assertTrue(TankBreeding.warnings(level, c).contains("remove_male"), "menu warns: take the male out");
        // the male eats free-swimming fry
        for (int i = 0; i < 20; i++) TankBreeding.step(level, c, 0.25);
        long left = count(bettas(h, c), LifeStage.FRY);
        h.assertTrue(left < fry && logged(t, "log.bettacraft.fry_eaten"), "male ate fry: " + fry + " -> " + left);
        h.succeed();
    }

    /** No nest without breeding conditions (cold water). */
    @GameTest(template = EMPTY)
    public static void noNestInColdWater(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 2, 2, 8000);
        c.getTank().legacyHeaters().setItem(0, ItemStack.EMPTY);
        adult(h, c, false, 21);
        adult(h, c, true, 22);
        c.getTank().setWaterTemp(22.0F);
        for (int i = 0; i < 20; i++) TankBreeding.step(h.getLevel(), c, 0.05);
        h.assertTrue(c.getTank().nest() == null, "22 C is below the breeding range");
        h.succeed();
    }

    // ------------------------------------------------------------------ stages and fry food

    @GameTest(template = EMPTY)
    public static void fryGrowOnlyWithFryFood(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 2, 2, 8000);
        BettaEntity fry = insert(h, c, BettaData.offspring(BettaGenotype.homozygous(BettaGenome.DEFAULT), false, null, null, 1, 0));
        BettaEntity adult = adult(h, c, true, 31);
        h.assertTrue(fry.lifeStage() == LifeStage.FRY && !fry.isAdult(), "inserted as fry");
        h.assertTrue(TankInhabitants.countAdults(new ArrayList<>(bettas(h, c))) == 1, "fry do not count");
        // what fry eat
        h.assertTrue(TankCare.feed(level, c, new ItemStack(ModItems.ARTEMIA_NAUPLII.get()), 1) == 0, "no nauplii in the first half");
        int pellets = TankCare.feed(level, c, new ItemStack(ModItems.BETTA_PELLETS.get()), 5);
        h.assertTrue(pellets == 1 && adult.health().daysSinceFed() == 0, "pellets feed the adult only");
        // hungry fry stop growing
        for (int i = 0; i < 8; i++) BettaCare.advance(level, c, fry, 0.25); // 2 days without fry food
        double grown = fry.stageProgress();
        h.assertTrue(fry.lifeStage() == LifeStage.FRY && grown > 0.3 && grown <= 0.5, "grew only while fed, progress " + grown);
        // infusoria
        h.assertTrue(TankCare.feed(level, c, new ItemStack(ModItems.INFUSORIA.get()), 1) == 1, "infusoria feed fry");
        BettaCare.advance(level, c, fry, 0.25);
        BettaCare.advance(level, c, fry, 0.25);
        h.assertTrue(fry.stageProgress() >= 0.5, "second half, progress " + fry.stageProgress());
        h.assertTrue(TankCare.feed(level, c, new ItemStack(ModItems.ARTEMIA_NAUPLII.get()), 1) == 1, "nauplii from the second half");
        for (int i = 0; i < 4; i++) {
            TankCare.feed(level, c, new ItemStack(ModItems.ARTEMIA_NAUPLII.get()), 1);
            BettaCare.advance(level, c, fry, 0.25);
        }
        h.assertTrue(fry.lifeStage() == LifeStage.JUVENILE, "fry -> juvenile, is " + fry.lifeStage());
        h.assertTrue(TankCare.feed(level, c, new ItemStack(ModItems.INFUSORIA.get()), 1) == 0, "juveniles eat normal food");
        // juvenile -> subadult -> adult with pellets (2.5 + 3.5 days)
        for (int i = 0; i < 26 && !fry.isAdult(); i++) {
            TankCare.feed(level, c, new ItemStack(ModItems.BETTA_PELLETS.get()), 1);
            BettaCare.advance(level, c, fry, 0.25);
        }
        h.assertTrue(fry.isAdult() && logged(c.getTank(), "log.bettacraft.grown_up"), "grown up");
        h.assertTrue(Math.abs(fry.getBbWidth() - adult.getBbWidth()) < 1e-3, "adult size");
        h.assertTrue(TankInhabitants.countAdults(new ArrayList<>(bettas(h, c))) == 2, "now counts for stocking");
        h.succeed();
    }

    /** The feeder gives fry food to fry and pellets to adults in the same feeding. */
    @GameTest(template = EMPTY)
    public static void feederChoosesFryFood(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 2, 2, 8000);
        BettaEntity fry = insert(h, c, BettaData.offspring(BettaGenotype.homozygous(BettaGenome.DEFAULT), true, null, null, 1, 0));
        BettaEntity adult = adult(h, c, false, 41);
        BettaCare.advance(level, c, fry, 0.9);
        BettaCare.advance(level, c, adult, 0.9);
        BlockPos fp = new BlockPos(2, 3, 2);
        h.setBlock(fp, ModBlocks.FEEDER.get().defaultBlockState());
        FeederBlockEntity f = (FeederBlockEntity) h.getBlockEntity(fp);
        f.items().insertItem(0, new ItemStack(ModItems.BETTA_PELLETS.get(), 4), false);
        f.items().insertItem(1, new ItemStack(ModItems.INFUSORIA.get(), 2), false);
        int used = f.feedNow(level, c);
        h.assertTrue(used == 2, "one pellet + one infusoria, used " + used);
        h.assertTrue(fry.health().daysSinceFed() == 0 && adult.health().daysSinceFed() == 0, "both fed");
        h.succeed();
    }

    // ------------------------------------------------------------------ fights and predators

    @GameTest(template = EMPTY)
    public static void malesFightUntilOneDies(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 3, 2, 2, 12000);
        adult(h, c, false, 51);
        adult(h, c, false, 52);
        h.assertTrue(TankBreeding.warnings(level, c).contains("fight"), "menu warns at once");
        for (int i = 0; i < 40; i++) {
            long males = bettas(h, c).stream().filter(b -> b.isAdult() && !b.isFemale()).count();
            if (males < 2) break;
            TankBreeding.step(level, c, 0.05);
        }
        long males = bettas(h, c).stream().filter(b -> b.isAdult() && !b.isFemale()).count();
        h.assertTrue(males == 1, "one male survives the fight, alive: " + males);
        h.assertTrue(logged(c.getTank(), "log.bettacraft.fight") && logged(c.getTank(), "log.bettacraft.killed_fight"), "fight + death logged");
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void femalesDoNotFight(GameTestHelper h) {
        TankBlockEntity c = tank(h, 3, 2, 2, 12000);
        adult(h, c, true, 61);
        adult(h, c, true, 62);
        for (int i = 0; i < 30; i++) TankBreeding.step(h.getLevel(), c, 0.05);
        h.assertTrue(bettas(h, c).size() == 2 && bettas(h, c).stream().allMatch(b -> b.getHealth() == b.getMaxHealth()), "no damage");
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void pufferfishEatsFry(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 3, 2, 2, 12000);
        for (int i = 0; i < 5; i++) insert(h, c, BettaData.offspring(BettaGenotype.homozygous(BettaGenome.DEFAULT), i % 2 == 0, null, null, 1, 0));
        Component err = TankInhabitants.insert(level, c, new ItemStack(Items.PUFFERFISH_BUCKET), EntityType.PUFFERFISH, null, false);
        h.assertTrue(err == null, "pufferfish accepted");
        h.assertTrue(TankBreeding.warnings(level, c).contains("predator"), "predator warning");
        for (int i = 0; i < 16; i++) TankBreeding.step(level, c, 0.25);
        long fry = count(bettas(h, c), LifeStage.FRY);
        h.assertTrue(fry < 5 && logged(c.getTank(), "log.bettacraft.eaten"), "pufferfish ate fry, left " + fry);
        h.succeed();
    }
}
