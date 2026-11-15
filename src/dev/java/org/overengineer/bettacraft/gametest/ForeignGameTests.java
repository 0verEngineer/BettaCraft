package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.function.Consumer;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.foreign.ForeignBreeding;
import org.overengineer.bettacraft.foreign.ForeignCare;
import org.overengineer.bettacraft.foreign.ForeignProfile;
import org.overengineer.bettacraft.foreign.ForeignProfiles;
import org.overengineer.bettacraft.reef.SaltTolerance;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.terrarium.TerrariumAnimals;

/**
 * M32 part a: animals of other mods with our care and breeding. Vanilla stand-ins (tropical fish, axolotls, frogs with a
 * test profile) check the rules without any foreign mod; Fintastic and Aquaculture (on the dev runtime unless -PnoCompat)
 * and Critters and Companions (-Paquatic) check the shipped profiles with the real animals. Every test that changes the
 * profiles or the config runs in its own batch and restores them; random draws use fixed seeds or certain rates.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ForeignGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private ForeignGameTests() {}

    // ------------------------------------------------------------------ helpers

    private static boolean skip(GameTestHelper h, String modId) {
        if (ModList.get().isLoaded(modId)) return false;
        org.slf4j.LoggerFactory.getLogger("bettacraft/gametest").info("{} not installed: foreign animal test skipped", modId);
        h.succeed();
        return true;
    }

    static TankBlockEntity tank(GameTestHelper h, int x, int z, int sx, int sy, int sz, float temp) {
        TankBlockEntity c = CritterGameTests.tank(h, x, 2, z, sx, sy, sz, sx * sy * sz * 1000L);
        c.getTank().setWaterTemp(temp);
        c.getTank().setTargetTemp(temp);
        c.getTank().setWaterQuality(1F);
        return c;
    }

    private static EntityType<?> type(String id) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(id)).orElseThrow(() -> new GameTestAssertException("no entity " + id));
    }

    /** An animal of {@code type} as an inhabitant of the tank (at its middle). */
    static LivingEntity add(GameTestHelper h, TankBlockEntity c, EntityType<?> type, Consumer<CompoundTag> data) {
        Entity e = type.create(h.getLevel());
        if (!(e instanceof LivingEntity le)) throw new GameTestAssertException("no living " + type);
        CompoundTag t = e.saveWithoutId(new CompoundTag());
        data.accept(t);
        t.remove("UUID");
        e.load(t);
        TankData tank = c.getTank();
        Vec3 mid = new TankWaterVolume(tank).clamp(TankInhabitants.tankBox(tank).getCenter(), 0.3, 0.1, 0.3);
        e.moveTo(mid.x, mid.y, mid.z, 0, 0);
        TankInhabitants.adopt(e, c);
        if (!h.getLevel().addFreshEntity(e)) throw new GameTestAssertException("not added");
        tank.inhabitants().add(e.getUUID());
        return le;
    }

    private static ForeignProfile profile(String json) {
        return ForeignProfile.parse(JsonParser.parseString(json).getAsJsonObject(), w -> {
            throw new GameTestAssertException("profile warning: " + w);
        });
    }

    /** Runs {@code body} with an extra test profile (and the shipped ones), restoring the profiles afterwards. */
    static void withProfile(String json, Runnable body) {
        Map<ResourceLocation, ForeignProfile> saved = ForeignProfiles.files();
        Map<ResourceLocation, ForeignProfile> files = new LinkedHashMap<>(saved);
        files.put(BettaCraft.id("gametest_stand_in"), profile(json));
        ForeignProfiles.set(files);
        try {
            body.run();
        } finally {
            ForeignProfiles.set(saved);
        }
    }

    private static List<Entity> ofType(GameTestHelper h, TankBlockEntity c, EntityType<?> type) {
        List<Entity> out = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(h.getLevel(), c.getTank())) if (e.getType() == type) out.add(e);
        return out;
    }

    private static boolean logged(TankData t, String key) {
        return t.log().stream().anyMatch(e -> e.key().equals(key));
    }

    private static int variant(Entity e) {
        return e.saveWithoutId(new CompoundTag()).getInt("Variant");
    }

    static final String TROPICAL = """
            {"entity": "minecraft:tropical_fish", "group": "test",
             "care": {"temperature": [22, 28], "min_quality": 0.4, "salinity": "fresh", "stock": 0.5},
             "breeding": {"type": "livebearer", "temperature": [24, 27], "young": [3, 3], "days": 1, "rate": 1000, "inherit": ["Variant"]}}""";

    // ------------------------------------------------------------------ vanilla stand-ins

    /**
     * A tropical fish with a test profile: it gets hungry, the tank's food feeds it, its salinity and stocking come from
     * the profile; unfed it falls sick and dies (log with the cause), only in the tank.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m32_foreign_care", timeoutTicks = 200)
    public static void standInGetsOurNeedsAndDies(GameTestHelper h) {
        withProfile(TROPICAL, () -> {
            ServerLevel level = h.getLevel();
            TankBlockEntity c = tank(h, 2, 2, 2, 2, 2, 25F);
            LivingEntity fish = add(h, c, EntityType.TROPICAL_FISH, t -> {});
            ForeignProfile p = ForeignProfiles.cared(fish);
            h.assertTrue(p != null, "the stand-in has a profile");
            h.assertTrue(SaltTolerance.of(fish) == null, "lenient by default: the salt does not matter");
            h.assertTrue(Math.abs(TankInhabitants.weight(fish) - 0.5) < 1e-6, "stocking from the profile");
            ForeignCare.advance(level, c, fish, p, 2.0);
            h.assertTrue(ForeignCare.health(fish).isHungry(ForeignCare.params(p)), "hungry after two days");
            int used = TankCare.feed(level, c, new ItemStack(ModItems.BETTA_PELLETS.get()), 1);
            h.assertTrue(used == 1 && ForeignCare.health(fish).daysSinceFed() < 1e-6, "the pellets fed it (" + used + ")");
            float[] menu = ForeignCare.menuValues(fish);
            h.assertTrue(menu != null && menu[1] > 0.9F, "the menu shows it fed");
            // a fish of the same kind outside any tank gets nothing
            Entity wild = EntityType.TROPICAL_FISH.create(level);
            wild.moveTo(h.absoluteVec(new Vec3(20, 3, 20)));
            level.addFreshEntity(wild);
            ForeignCare.step(level, c, 5.0);
            h.assertTrue(!wild.getPersistentData().contains(ForeignCare.KEY), "no needs outside our tanks");
            for (int i = 0; i < 60 && fish.isAlive(); i++) ForeignCare.advance(level, c, fish, p, 1.0);
            h.assertTrue(!fish.isAlive() && !c.getTank().inhabitants().contains(fish.getUUID()), "it starved");
            h.assertTrue(c.getTank().log().stream().anyMatch(e -> e.key().equals("log.bettacraft.died") && e.args().contains("@cause.bettacraft.hunger")),
                    "died of hunger in the log");
            h.assertTrue(logged(c.getTank(), "log.bettacraft.sick"), "fell sick first");
            wild.discard();
        });
        h.succeed();
    }

    /** Cold water, bad water and the wrong salinity are trouble; off, the animal is left alone. */
    @GameTest(template = EMPTY, batch = "bettacraft_m32_foreign_off", timeoutTicks = 200)
    public static void standInConditionsAndSwitch(GameTestHelper h) {
        boolean saved = CommonConfig.FEATURE_FOREIGN_CARE.get();
        withProfile(TROPICAL, () -> {
            try {
                ServerLevel level = h.getLevel();
                TankBlockEntity c = tank(h, 2, 2, 2, 2, 2, 15F);
                LivingEntity fish = add(h, c, EntityType.TROPICAL_FISH, t -> {});
                ForeignProfile p = ForeignProfiles.of(fish.getType());
                h.assertTrue(ForeignCare.conditions(c, fish, p).tempBad(), "15 °C is too cold for it");
                c.getTank().setWaterTemp(25F);
                c.getTank().setWaterQuality(0.1F);
                h.assertTrue(ForeignCare.conditions(c, fish, p).waterBad(), "bad water");
                c.getTank().setWaterQuality(1F);
                c.getTank().setSalinity(35F);
                h.assertTrue(!ForeignCare.conditions(c, fish, p).saltBad(), "lenient: sea water does not matter");
                var salt = org.overengineer.bettacraft.config.ServerConfig.SALT_FOREIGN.get();
                try {
                    org.overengineer.bettacraft.config.ServerConfig.SALT_FOREIGN.set(org.overengineer.bettacraft.config.ServerConfig.ForeignSalinity.REALISTIC);
                    h.assertTrue(ForeignCare.conditions(c, fish, p).saltBad(), "realistic: a freshwater profile in sea water");
                    h.assertTrue(SaltTolerance.of(fish) == p.salinity(), "realistic: salinity from the profile");
                } finally {
                    org.overengineer.bettacraft.config.ServerConfig.SALT_FOREIGN.set(salt);
                }
                c.getTank().setSalinity(0F);
                CommonConfig.FEATURE_FOREIGN_CARE.set(false);
                fish.getPersistentData().remove(ForeignCare.KEY);
                ForeignCare.step(level, c, 30.0);
                h.assertTrue(fish.isAlive() && !fish.getPersistentData().contains(ForeignCare.KEY), "off: no needs");
                h.assertTrue(ForeignCare.menuValues(fish) == null && SaltTolerance.of(fish) == SaltTolerance.foreign(fish.getType()), "off: as before");
            } finally {
                CommonConfig.FEATURE_FOREIGN_CARE.set(saved);
            }
        });
        h.succeed();
    }

    /**
     * Two fed tropical fish of different patterns breed by the profile (livebearer, certain rate): the young take the
     * pattern of one of their parents; too little water for a big-tank profile stops them (log).
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m32_foreign_breed", timeoutTicks = 200)
    public static void standInBreedsAndInheritsItsVariant(GameTestHelper h) {
        withProfile(TROPICAL, () -> {
            ServerLevel level = h.getLevel();
            TankBlockEntity c = tank(h, 2, 2, 2, 2, 2, 25F);
            LivingEntity a = add(h, c, EntityType.TROPICAL_FISH, t -> t.putInt("Variant", 65536));
            LivingEntity b = add(h, c, EntityType.TROPICAL_FISH, t -> t.putInt("Variant", 117506305));
            int va = variant(a), vb = variant(b);
            h.assertTrue(va != vb, "two patterns " + va + " " + vb);
            ForeignBreeding.step(level, c, 0.01); // mating (rate 1000)
            Entity mother = a.getPersistentData().contains(ForeignBreeding.BROOD) ? a : b;
            h.assertTrue(mother.getPersistentData().contains(ForeignBreeding.BROOD), "a mother carries young");
            h.assertTrue(logged(c.getTank(), "log.bettacraft.foreign.pregnant"), "logged");
            ForeignBreeding.step(level, c, 1.1); // birth
            List<Entity> all = ofType(h, c, EntityType.TROPICAL_FISH);
            h.assertTrue(all.size() == 5, "three young (" + all.size() + " fish)");
            for (Entity e : all) {
                if (e == a || e == b) continue;
                h.assertTrue(variant(e) == va || variant(e) == vb, "pattern of a parent: " + variant(e));
                h.assertTrue(TankInhabitants.hasTankRef(e), "the young live in the tank");
            }
            h.assertTrue(logged(c.getTank(), "log.bettacraft.foreign.young"), "birth logged");
        });
        withProfile(TROPICAL.replace("\"days\": 1,", "\"days\": 1, \"min_water_blocks\": 24,"), () -> {
            TankBlockEntity c = tank(h, 12, 2, 2, 2, 2, 25F);
            add(h, c, EntityType.TROPICAL_FISH, t -> {});
            add(h, c, EntityType.TROPICAL_FISH, t -> {});
            ForeignBreeding.step(h.getLevel(), c, 1.0);
            for (Entity e : ofType(h, c, EntityType.TROPICAL_FISH)) h.assertTrue(!e.getPersistentData().contains(ForeignBreeding.BROOD), "no brood in 8 blocks");
            h.assertTrue("tank_size".equals(ForeignBreeding.blocked(h.getLevel(), c, ForeignProfiles.of(EntityType.TROPICAL_FISH))), "too small");
            h.assertTrue(logged(c.getTank(), "log.bettacraft.foreign.cannot_breed"), "logged why");
        });
        h.succeed();
    }

    /**
     * Vanilla breeding in a tank is suppressed (question A2): love is taken from the axolotls each care step, a baby from
     * two parents in a tank is cancelled; off, the vanilla baby comes. Our axolotl eggs (M29) still hatch.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m32_foreign_suppress", timeoutTicks = 200)
    public static void vanillaBreedingIsSuppressedInTanks(GameTestHelper h) {
        boolean saved = CommonConfig.FEATURE_SUPPRESS_FOREIGN_BREEDING.get();
        try {
            ServerLevel level = h.getLevel();
            TankBlockEntity c = tank(h, 2, 2, 2, 2, 2, 18F);
            Axolotl a = (Axolotl) add(h, c, EntityType.AXOLOTL, t -> {});
            Axolotl b = (Axolotl) add(h, c, EntityType.AXOLOTL, t -> {});
            a.setInLove(null);
            b.setInLove(null);
            ForeignBreeding.step(level, c, 0.001);
            h.assertTrue(!a.isInLove() && !b.isInLove(), "no love in the tank");
            a.spawnChildFromBreeding(level, b);
            h.assertTrue(ofType(h, c, EntityType.AXOLOTL).size() == 2, "no vanilla baby");
            h.assertTrue(level.getEntities(EntityType.AXOLOTL, TankInhabitants.tankBox(c.getTank()).inflate(2), e -> e.isBaby()).isEmpty(), "none at all");
            // our breeding still works: an axolotl baby of M29 joins
            h.assertTrue(org.overengineer.bettacraft.tank.AxolotlCare.spawn(level, c, org.overengineer.bettacraft.critters.CritterGenes.WILD, 1,
                    a.position()) != null, "our hatching is not suppressed");
            CommonConfig.FEATURE_SUPPRESS_FOREIGN_BREEDING.set(false);
            a.setAge(0);
            b.setAge(0);
            a.spawnChildFromBreeding(level, b);
            h.assertTrue(level.getEntities(EntityType.AXOLOTL, TankInhabitants.tankBox(c.getTank()).inflate(2), e -> e.isBaby()).size() >= 2,
                    "off: the vanilla baby comes");
        } finally {
            CommonConfig.FEATURE_SUPPRESS_FOREIGN_BREEDING.set(saved);
        }
        h.succeed();
    }

    /** A profile with terrarium needs makes the type a terrarium animal (M31) while it is loaded; our own types are never mapped. */
    @GameTest(template = EMPTY, batch = "bettacraft_m32_foreign_terrarium", timeoutTicks = 100)
    public static void terrariumProfileAndOwnCare(GameTestHelper h) {
        withProfile("""
                {"entity": ["minecraft:frog", "minecraft:axolotl", "bettacraft:betta"], "group": "test",
                 "terrarium": {"habitat": "paludarium", "air": [15, 30], "water": [12, 28], "land": [0.2, 0.8]}}""", () -> {
            h.assertTrue(TerrariumAnimals.isTerrariumAnimal(EntityType.FROG), "the frog is a terrarium animal by its profile");
            h.assertTrue(ForeignProfiles.of(EntityType.AXOLOTL) == null, "the axolotl keeps its own care (M29)");
            h.assertTrue(ForeignProfiles.of(type("bettacraft:betta")) == null, "our bettas are never mapped");
        });
        h.assertTrue(!TerrariumAnimals.isTerrariumAnimal(EntityType.FROG), "taken back with the profile");
        h.succeed();
    }

    // ------------------------------------------------------------------ the real mods

    /**
     * Fintastic guppies by the shipped profile (our guppy): fed, they breed as livebearers and the young are babies with
     * the fin and tail colours of their parents; their own breeding (love, Fintastic's breeding goal) gives no young in the
     * tank.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m32_fintastic", timeoutTicks = 200)
    public static void fintasticGuppiesBreedOurWay(GameTestHelper h) {
        if (skip(h, "fintastic")) return;
        ServerLevel level = h.getLevel();
        EntityType<?> guppy = type("fintastic:guppy");
        ForeignProfile p = ForeignProfiles.of(guppy);
        h.assertTrue(p != null && "guppy".equals(p.species()), "the shipped profile maps the guppy to ours");
        TankBlockEntity c = tank(h, 2, 2, 2, 2, 2, 25F);
        LivingEntity a = add(h, c, guppy, t -> {
            t.putInt("FinColor", 1);
            t.putInt("TailColor", 2);
        });
        LivingEntity b = add(h, c, guppy, t -> {
            t.putInt("FinColor", 5);
            t.putInt("TailColor", 6);
        });
        // their own breeding: in love and the mod's own birth
        call(a, "setInLoveTime", 600);
        call(b, "setInLoveTime", 600);
        ForeignBreeding.step(level, c, 0.001);
        h.assertTrue((int) call(a, "getInLoveTime") == 0 && (int) call(b, "getInLoveTime") == 0, "love taken in the tank");
        invokeBreed(a, b, level); // posts Fintastic's BabyFishSpawnEvent, which we cancel in tanks
        h.assertTrue(ofType(h, c, guppy).size() == 2 && level.getEntities(guppy, TankInhabitants.tankBox(c.getTank()).inflate(2), e -> ((LivingEntity) e).isBaby()).isEmpty(),
                "Fintastic's own young is suppressed");
        // ours: fed parents, a brood, a birth
        int fed = TankCare.feed(level, c, new ItemStack(ModItems.BETTA_PELLETS.get()), 1);
        h.assertTrue(fed == 1, "the guppies eat the pellets");
        for (int i = 0; i < 1000 && !a.getPersistentData().contains(ForeignBreeding.BROOD) && !b.getPersistentData().contains(ForeignBreeding.BROOD); i++) {
            ForeignBreeding.step(level, c, 0.05);
            ForeignCare.feed(List.of(a, b), 2, null);
        }
        h.assertTrue(a.getPersistentData().contains(ForeignBreeding.BROOD) || b.getPersistentData().contains(ForeignBreeding.BROOD), "a female carries young");
        ForeignBreeding.step(level, c, p.breeding().days() + 0.1);
        List<Entity> young = new ArrayList<>();
        for (Entity e : ofType(h, c, guppy)) if (e != a && e != b) young.add(e);
        h.assertTrue(!young.isEmpty(), "young guppies were born");
        for (Entity e : young) {
            CompoundTag t = e.saveWithoutId(new CompoundTag());
            h.assertTrue(((LivingEntity) e).isBaby(), "a baby guppy");
            h.assertTrue(t.getInt("FinColor") == 1 || t.getInt("FinColor") == 5, "fin colour of a parent: " + t.getInt("FinColor"));
            h.assertTrue(t.getInt("TailColor") == 2 || t.getInt("TailColor") == 6, "tail colour of a parent: " + t.getInt("TailColor"));
        }
        h.succeed();
    }

    /** Fintastic's own birth (BreedableWaterAnimal#spawnChildFromBreeding) by name, whatever its parameter type. */
    private static void invokeBreed(LivingEntity a, LivingEntity b, ServerLevel level) {
        for (var m : a.getClass().getMethods()) {
            if (m.getName().equals("spawnChildFromBreeding") && m.getParameterCount() == 2) {
                try {
                    m.invoke(a, level, b);
                    return;
                } catch (ReflectiveOperationException ex) {
                    throw new GameTestAssertException("spawnChildFromBreeding: " + ex);
                }
            }
        }
        throw new GameTestAssertException("no spawnChildFromBreeding");
    }

    private static Object call(Object o, String name, Object... args) {
        for (var m : o.getClass().getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == args.length) {
                try {
                    return m.invoke(o, args);
                } catch (ReflectiveOperationException ex) {
                    throw new GameTestAssertException(name + ": " + ex);
                }
            }
        }
        throw new GameTestAssertException("no method " + name);
    }

    /**
     * Aquaculture bluegills (food fish, question A: they breed in big tanks only, M30): in a small tank they cannot, in a
     * tank of 27 water blocks they spawn; their temperate range bears 10 °C, too warm water harms them.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m32_aquaculture", timeoutTicks = 200)
    public static void aquacultureFoodFishNeedABigTank(GameTestHelper h) {
        if (skip(h, "aquaculture")) return;
        ServerLevel level = h.getLevel();
        EntityType<?> bluegill = type("aquaculture:bluegill");
        ForeignProfile p = ForeignProfiles.of(bluegill);
        h.assertTrue(p != null && "food_fish".equals(p.group()) && p.breeding().minWaterBlocks() >= 24, "the shipped food fish profile");
        h.assertTrue(p.tolerates(10) && !p.tolerates(31), "a temperate fish");
        TankBlockEntity small = tank(h, 2, 2, 2, 2, 2, 20F);
        add(h, small, bluegill, t -> {});
        add(h, small, bluegill, t -> {});
        h.assertTrue("tank_size".equals(ForeignBreeding.blocked(level, small, p)), "8 blocks are too small");
        TankBlockEntity big = tank(h, 10, 2, 3, 3, 3, 20F);
        LivingEntity x = add(h, big, bluegill, t -> {});
        LivingEntity y = add(h, big, bluegill, t -> {});
        h.assertTrue(ForeignBreeding.blocked(level, big, p) == null, "27 blocks are enough: " + ForeignBreeding.blocked(level, big, p));
        for (int i = 0; i < 2000 && !x.getPersistentData().contains(ForeignBreeding.BROOD) && !y.getPersistentData().contains(ForeignBreeding.BROOD); i++) {
            ForeignBreeding.step(level, big, 0.05);
            ForeignCare.feed(List.of(x, y), 2, null);
        }
        h.assertTrue(x.getPersistentData().contains(ForeignBreeding.BROOD) || y.getPersistentData().contains(ForeignBreeding.BROOD), "they spawned");
        ForeignBreeding.step(level, big, p.breeding().days() + 0.1);
        h.assertTrue(ofType(h, big, bluegill).size() > 2, "young bluegills");
        big.getTank().setWaterTemp(33F);
        h.assertTrue(ForeignCare.conditions(big, x, p).tempBad(), "33 °C is too warm for them");
        h.succeed();
    }

    /** Critters and Companions koi (-Paquatic): a pond fish that needs plants for its eggs and bears cool water. */
    @GameTest(template = EMPTY, batch = "bettacraft_m32_koi", timeoutTicks = 200)
    public static void koiIsAPondFish(GameTestHelper h) {
        if (skip(h, "crittersandcompanions")) return;
        EntityType<?> koi = type("crittersandcompanions:koi_fish");
        ForeignProfile p = ForeignProfiles.of(koi);
        h.assertTrue(p != null && "pond_fish".equals(p.group()), "the shipped koi profile");
        TankBlockEntity c = tank(h, 2, 2, 4, 2, 4, 20F);
        LivingEntity k = add(h, c, koi, t -> {});
        add(h, c, koi, t -> {});
        h.assertTrue("plants".equals(ForeignBreeding.blocked(h.getLevel(), c, p)), "no plants, no eggs: " + ForeignBreeding.blocked(h.getLevel(), c, p));
        c.getTank().setWaterTemp(8F);
        h.assertTrue(!ForeignCare.conditions(c, k, p).tempBad(), "8 °C is fine for a koi");
        int fed = TankCare.feed(h.getLevel(), c, new ItemStack(ModItems.BETTA_PELLETS.get()), 1);
        h.assertTrue(fed == 1 && ForeignCare.health(k).daysSinceFed() < 1e-6, "koi eat pellets");
        h.succeed();
    }
}
