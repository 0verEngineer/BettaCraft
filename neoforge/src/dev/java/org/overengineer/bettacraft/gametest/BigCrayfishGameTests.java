package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.critters.BigCrayfish;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterObject;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.genetics.BettaSpecies;
import org.overengineer.bettacraft.plants.InteriorPlant;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.AxolotlCare;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M29 part d: the big crayfish eat plants back and dig up cuttings (epiphytes are safe), eat fry and pinch a betta; the
 * feature switch takes them out of the net; vanilla axolotls lay eggs on plants in cool water only, the larvae hatch with
 * the parents' colour, warm water hurts them, and a bucket from the tank keeps their genes. Random draws use fixed seeds;
 * the axolotl laying is a 30-day step (failure chance below one in a million).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BigCrayfishGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private BigCrayfishGameTests() {}

    private static TankBlockEntity tank(GameTestHelper h, int x, int y, int z, int sx, int sy, int sz, float temp) {
        TankBlockEntity c = CritterGameTests.tank(h, x, y, z, sx, sy, sz, sx * sy * sz * 1000L);
        c.getTank().setWaterTemp(temp);
        c.getTank().setTargetTemp(temp);
        c.getTank().setWaterQuality(1F);
        return c;
    }

    private static ServerLevel interior(GameTestHelper h) {
        ServerLevel l = InteriorManager.interiorLevel(h.getLevel().getServer());
        if (l == null) throw new GameTestFailure("interior dimension not loaded");
        return l;
    }

    private static InteriorRegion region(GameTestHelper h, TankBlockEntity c) {
        ServerLevel in = interior(h);
        InteriorRegion r = InteriorManager.ensureRegion(h.getLevel(), c);
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) {
            Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.GRAVEL.defaultBlockState(), false);
        }
        return r;
    }

    private static boolean logged(TankData t, String key) {
        return t.log().stream().anyMatch(e -> e.key().equals(key));
    }

    /** A Florida crayfish digs up a Rotala cutting and eats a grown Ludwigia back; a Java fern on a stone stays. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void bigCrayfishEatAndDigUpPlants(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 2, 2, 2, 1, 1, 24F);
        InteriorRegion r = region(h, c);
        ServerLevel in = interior(h);
        InteriorPlant cutting = InteriorPlants.plantAt(in, r, PlantSpecies.ROTALA, 8, 0, 8, 0, 7L);
        InteriorPlant grown = InteriorPlants.plantAt(in, r, PlantSpecies.LUDWIGIA, 24, 0, 8, 4, 3L);
        h.assertTrue(cutting != null && grown != null, "planted");
        h.assertTrue(BigCrayfish.edible(PlantSpecies.ROTALA) && !BigCrayfish.edible(PlantSpecies.JAVA_FERN) && !BigCrayfish.edible(PlantSpecies.JAVA_MOSS),
                "epiphytes and moss are safe");
        TankCritters.add(level, c, CritterSpecies.FLORIDA_CRAYFISH, CritterGenes.pure(0, 0.5F), true, 30);
        h.assertTrue(BigCrayfish.adults(c.getTank()) == 1, "one adult big crayfish");
        int n = BigCrayfish.plants(level, c, 1, 20.0, new SplittableRandom(29)); // expected 6 bites
        h.assertTrue(n >= 2, "bites " + n);
        h.assertTrue(!r.plants().all().contains(cutting), "the cutting was dug up");
        h.assertTrue(!r.plants().all().contains(grown) || grown.target < 4, "the Ludwigia was eaten back (target " + grown.target + ")");
        h.assertTrue(logged(c.getTank(), "log.bettacraft.big_crayfish_uprooted"), "logged");
        h.succeed();
    }

    /** A Florida crayfish eats neon fry and pinches a betta; dwarf shrimp are no match for it (unit tests), fish are. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void bigCrayfishEatFryAndPinchABetta(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 2, 2, 3, 2, 3, 26F);
        TankCritters.add(level, c, CritterSpecies.FLORIDA_CRAYFISH, CritterGenes.WILD, false, 30);
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        var b = w.box();
        Vec3 mid = w.clamp(new Vec3((b.minX + b.maxX) / 2, b.minY + 0.3, (b.minZ + b.maxZ) / 2), 0.3, 0.1, 0.2);
        List<TankmateEntity> fry = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            TankmateEntity e = TankmateBreeding.spawn(level, c, TankmateSpecies.NEON_TETRA, TankmateGenes.wild(TankmateSpecies.NEON_TETRA), i % 2 == 0,
                    LifeStage.FRY, 1, mid);
            if (e != null) fry.add(e);
        }
        h.assertTrue(fry.size() == 4, "fry");
        BettaData d = BettaData.create(BettaGenotype.randomWild(BettaSpecies.SPLENDENS, RandomSource.create(3)), false, 0);
        Component err = TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d),
                ModEntities.BETTA.get(), null, false);
        h.assertTrue(err == null, "betta in: " + (err == null ? "" : err.getString()));
        h.assertTrue(BigCrayfish.fishWeight(fry.getFirst()) > 0 && BigCrayfish.eaten(fry.getFirst()), "fry are eaten");
        BettaEntity betta = null;
        for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) if (e instanceof BettaEntity be) betta = be;
        h.assertTrue(betta != null && BigCrayfish.fishWeight(betta) > 0, "the betta is a target");
        h.assertTrue(BigCrayfish.eaten(betta) == betta.lifeStage().ordinal() < LifeStage.SUBADULT.ordinal(), "an adult betta is pinched, young ones eaten");
        int caught = BigCrayfish.fish(level, c, 1, 40.0, new SplittableRandom(30)); // expected 6
        h.assertTrue(caught >= 3, "caught " + caught);
        long left = fry.stream().filter(Entity::isAlive).count();
        h.assertTrue(left < 4, "fry eaten: " + (4 - left));
        h.assertTrue(logged(c.getTank(), "log.bettacraft.big_crayfish_ate"), "logged");
        h.succeed();
    }

    /** With features.invertebrates.bigCrayfish off the net no longer catches them (those in tanks stay). */
    @GameTest(template = EMPTY, batch = "bettacraft_m29d_feature", timeoutTicks = 100)
    public static void bigCrayfishFeatureSwitch(GameTestHelper h) {
        boolean saved = Features.own(Feature.BIG_CRAYFISH);
        try {
            h.assertTrue(NetCatching.table(NetCatching.Water.SWAMP, false, false, false).stream().anyMatch(x -> x.species() == CritterSpecies.FLORIDA_CRAYFISH),
                    "Florida crayfish in swamps");
            h.assertTrue(NetCatching.table(NetCatching.Water.RIVER, false, false, false).stream().anyMatch(x -> x.species() == CritterSpecies.MARBLED_CRAYFISH),
                    "marbled crayfish in rivers");
            Features.set(Feature.BIG_CRAYFISH, false);
            h.assertTrue(NetCatching.table(NetCatching.Water.SWAMP, false, false, false).stream().noneMatch(x -> x.species() == CritterSpecies.FLORIDA_CRAYFISH),
                    "off: not in the net");
        } finally {
            Features.set(Feature.BIG_CRAYFISH, saved);
        }
        h.succeed();
    }

    private static Axolotl axolotl(GameTestHelper h, TankBlockEntity c, Axolotl.Variant v) {
        int l = AxolotlCare.locus(v);
        Vec3 p = new TankWaterVolume(c.getTank()).clamp(Vec3.atCenterOf(c.getTank().min()).add(0.5, 0, 0.5), 0.45, 0.1, 0.45);
        Axolotl a = AxolotlCare.spawn(h.getLevel(), c, l < 0 ? CritterGenes.WILD : CritterGenes.pure(l, 0.1F), 0, p);
        if (a == null) throw new GameTestFailure("no axolotl");
        a.setAge(0);
        return a;
    }

    private static CritterObject eggs(TankData t) {
        for (CritterObject o : t.critterObjects()) if (o.kind == CritterObject.Kind.AXOLOTL_EGGS) return o;
        return null;
    }

    /**
     * Two leucistic axolotls in cool water: no eggs without plants, then eggs on the plant; the larvae hatch leucistic.
     * M32 part d: the care runs with a random source of its own (fixed seed); with the level's shared random a rare
     * spontaneous mutation ({@code otherMutationChance}) sometimes gave a wild-coloured larva.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void axolotlsLayEggsOnPlantsInCoolWater(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        java.util.random.RandomGenerator rnd = new java.util.Random(29L);
        TankBlockEntity c = tank(h, 2, 2, 2, 3, 2, 3, 18F);
        InteriorRegion r = region(h, c);
        axolotl(h, c, Axolotl.Variant.LUCY);
        axolotl(h, c, Axolotl.Variant.LUCY);
        AxolotlCare.step(level, c, 30.0, rnd);
        h.assertTrue(eggs(c.getTank()) == null, "no eggs without plants");
        h.assertTrue(logged(c.getTank(), "log.bettacraft.axolotl_no_plants"), "no plants logged");
        h.assertTrue(InteriorPlants.plantAt(interior(h), r, PlantSpecies.ROTALA, 24, 0, 24, 4, 5L) != null, "planted");
        AxolotlCare.step(level, c, 30.0, rnd);
        CritterObject o = eggs(c.getTank());
        h.assertTrue(o != null, "eggs on the plant");
        h.assertTrue(logged(c.getTank(), "log.bettacraft.axolotl_eggs"), "eggs logged");
        o.age = 10;
        AxolotlCare.step(level, c, 0.01, rnd);
        h.assertTrue(eggs(c.getTank()) == null, "hatched");
        List<Axolotl> young = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) if (e instanceof Axolotl a && a.isBaby()) young.add(a);
        h.assertTrue(young.size() >= 3, "larvae " + young.size());
        h.assertTrue(young.stream().allMatch(a -> a.getVariant() == Axolotl.Variant.LUCY), "leucistic like the parents");
        h.succeed();
    }

    /** At 26 °C axolotls do not breed and lose health; the genes survive a bucket from the tank. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void warmWaterHurtsAxolotlsAndBucketsKeepTheGenes(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 2, 2, 3, 2, 3, 26F);
        InteriorRegion r = region(h, c);
        InteriorPlants.plantAt(interior(h), r, PlantSpecies.ROTALA, 24, 0, 24, 4, 5L);
        Axolotl a = axolotl(h, c, Axolotl.Variant.WILD);
        axolotl(h, c, Axolotl.Variant.WILD);
        AxolotlCare.setGenes(a, new CritterGenes(1 << AxolotlCare.BLUE, 0, 0.1F)); // a hidden blue carrier
        float before = a.getHealth();
        AxolotlCare.step(level, c, 2.0); // 3 hit points a day
        h.assertTrue(a.getHealth() < before, "warm water hurts: " + before + " -> " + a.getHealth());
        h.assertTrue(eggs(c.getTank()) == null, "no eggs in warm water");
        h.assertTrue(logged(c.getTank(), "log.bettacraft.axolotl_warm"), "logged");
        CompoundTagHolder data = new CompoundTagHolder();
        AxolotlCare.toBucket(a, data.tag);
        Axolotl b = net.minecraft.world.entity.EntityTypes.AXOLOTL.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        h.assertTrue(b != null, "second axolotl");
        b.setComponent(net.minecraft.core.component.DataComponents.AXOLOTL_VARIANT, Axolotl.Variant.WILD);
        AxolotlCare.fromBucket(b, data.tag);
        h.assertTrue(AxolotlCare.genes(b).carries(AxolotlCare.BLUE), "the bucket kept the hidden blue");
        b.discard();
        ItemStack bucket = new ItemStack(Items.AXOLOTL_BUCKET);
        bucket.set(DataComponents.BUCKET_ENTITY_DATA, CustomData.of(data.tag));
        h.assertTrue(bucket.has(DataComponents.BUCKET_ENTITY_DATA), "bucket data");
        h.succeed();
    }

    private static final class CompoundTagHolder {
        final net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
    }
}
