package org.overengineer.bettacraft.gametest;

import static org.overengineer.bettacraft.gametest.ForeignTerrariumGameTests.container;
import static org.overengineer.bettacraft.gametest.ForeignTerrariumGameTests.insert;
import static org.overengineer.bettacraft.gametest.ForeignTerrariumGameTests.ofType;
import static org.overengineer.bettacraft.gametest.ForeignTerrariumGameTests.paludarium;
import static org.overengineer.bettacraft.gametest.ForeignTerrariumGameTests.put;
import static org.overengineer.bettacraft.gametest.ForeignTerrariumGameTests.says;
import static org.overengineer.bettacraft.gametest.ForeignTerrariumGameTests.terrarium;
import static org.overengineer.bettacraft.gametest.ForeignTerrariumGameTests.wild;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.big.BigAnimals;
import org.overengineer.bettacraft.big.TransportTubItem;
import org.overengineer.bettacraft.foreign.ForeignBreeding;
import org.overengineer.bettacraft.foreign.ForeignCare;
import org.overengineer.bettacraft.foreign.ForeignHabits;
import org.overengineer.bettacraft.foreign.ForeignProfile;
import org.overengineer.bettacraft.foreign.ForeignProfiles;
import org.overengineer.bettacraft.foreign.ForeignTerrarium;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.terrarium.LandWalker;
import org.overengineer.bettacraft.terrarium.TerrariumBoxItem;

/**
 * M39 part b: the insects, mammals and birds of other mods in terraria and paludaria, by their shipped profiles and the real mods:
 * Naturalist and Fintastic on the dev runtime (unless {@code -PnoCompat}), Critters and Companions and Living Things with
 * {@code -Paquatic}; a test whose mod is missing passes with a log line (the clean-up crew has a vanilla stand-in). Fliers need a
 * lid (refused without, they fly out when it is taken off), fireflies glow at night and stay in the daylight, the clean-up crew
 * eats the leftovers and lives from the waste, the big ones go by the size rule and are caught with the large net, the small
 * mammals breed by their sexes, the giants are refused; the multi-part arapaima of Fintastic goes in and out by the tub.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ForeignLandAnimalsGameTests {
    private static final String EMPTY = "empty_32x10x32";
    private static final String LID = "message.bettacraft.terrarium.needs_lid", WRONG = "message.bettacraft.terrarium.wrong_mode",
            TOO_SMALL = "message.bettacraft.big.too_small";

    private ForeignLandAnimalsGameTests() {}

    // ------------------------------------------------------------------ helpers

    private static boolean skip(GameTestHelper h, String... mods) {
        for (String m : mods) {
            if (!ModList.get().isLoaded(m)) {
                org.slf4j.LoggerFactory.getLogger("bettacraft/gametest").info("{} not installed: M39 part b test skipped", m);
                h.succeed();
                return true;
            }
        }
        return false;
    }

    private static EntityType<?> type(String id) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(id)).orElseThrow(() -> new GameTestAssertException("no entity " + id));
    }

    private static ForeignProfile profile(EntityType<?> t) {
        ForeignProfile p = ForeignProfiles.of(t);
        if (p == null) throw new GameTestAssertException("no profile for " + BuiltInRegistries.ENTITY_TYPE.getKey(t));
        return p;
    }

    /** Lays a lid on every open top block of the tank. */
    static void lid(GameTestHelper h, TankBlockEntity c) {
        for (BlockPos top : c.getTank().openTops()) h.getLevel().setBlock(top, ModBlocks.TANK_LID.get().defaultBlockState(), 3);
    }

    /** Takes the lid off again (one block is enough to open it). */
    static void unlid(GameTestHelper h, TankBlockEntity c) {
        h.getLevel().setBlock(c.getTank().openTops().getFirst(), Blocks.AIR.defaultBlockState(), 3);
    }

    /** Inserts a new animal of the type from its box (or tub) and expects the message {@code key} (null: accepted, then taken out again). */
    private static void expect(GameTestHelper h, TankBlockEntity c, EntityType<?> type, String key, String what) {
        boolean tub = !profile(type).land().box();
        Component err = insert(h, c, container(h, type, tub, false));
        if (key == null) {
            if (err != null) throw new GameTestAssertException(what + " refused: " + err.getString());
            for (LivingEntity e : ofType(h, c, type)) {
                c.getTank().inhabitants().remove(e.getUUID());
                TankInhabitants.release(e);
                e.discard();
            }
        } else if (!says(err, key)) {
            throw new GameTestAssertException(what + ": expected " + key + ", got " + (err == null ? "accepted" : err.getString()));
        }
    }

    private static void assertSex(LivingEntity e, boolean female) {
        e.getPersistentData().putBoolean(ForeignBreeding.SEX, female);
    }

    // ------------------------------------------------------------------ Naturalist insects

    /**
     * Naturalist's insects: caterpillar and ant into a dry terrarium (not into a paludarium); the butterfly and the firefly fly and
     * are refused while the top is open, taken with a lid; the firefly lives in a paludarium too. None of them breeds (G1).
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m39b_insects", timeoutTicks = 300)
    public static void naturalistInsectsNeedTheirTankAndFliersALid(GameTestHelper h) {
        if (skip(h, "naturalist")) return;
        EntityType<?> caterpillar = type("naturalist:caterpillar"), ant = type("naturalist:ant"), butterfly = type("naturalist:butterfly"),
                firefly = type("naturalist:firefly");
        TankBlockEntity dry = terrarium(h, 2, 2, 6, 3, 6);
        TankBlockEntity palu = paludarium(h, 10, 2, 8, 3, 6, 0, 0.5, Blocks.SAND.defaultBlockState());
        for (EntityType<?> t : List.of(caterpillar, ant)) {
            expect(h, dry, t, null, t.toShortString() + " into a dry terrarium");
            expect(h, palu, t, WRONG, t.toShortString() + " into a paludarium");
            h.assertFalse(profile(t).breeding().breeds(), "no breeding of ours for " + t.toShortString());
        }
        for (EntityType<?> t : List.of(butterfly, firefly)) expect(h, dry, t, LID, t.toShortString() + " into an open terrarium");
        expect(h, palu, firefly, LID, "firefly into an open paludarium");
        lid(h, dry);
        lid(h, palu);
        for (EntityType<?> t : List.of(butterfly, firefly)) expect(h, dry, t, null, t.toShortString() + " into a closed terrarium");
        expect(h, palu, firefly, null, "firefly into a closed paludarium");
        // the dragonflies of M35 need one now too
        TankBlockEntity open = paludarium(h, 2, 12, 8, 3, 6, 0, 0.5, Blocks.SAND.defaultBlockState());
        expect(h, open, type("naturalist:dragonfly"), LID, "dragonfly into an open paludarium");
        h.succeed();
    }

    /** A butterfly in a closed terrarium flies (our walker) and stays; with the lid taken off it flies out within the care steps, alive. */
    @GameTest(template = EMPTY, batch = "bettacraft_m39b_insects", timeoutTicks = 300)
    public static void fliersFlyOutOfAnOpenTank(GameTestHelper h) {
        if (skip(h, "naturalist")) return;
        EntityType<?> butterfly = type("naturalist:butterfly");
        TankBlockEntity c = terrarium(h, 2, 2, 8, 3, 6);
        lid(h, c);
        LivingEntity b = put(h, c, container(h, butterfly, false, false));
        h.assertTrue(profile(butterfly).land().gait().style() == LandWalker.Style.FLY, "it flies");
        ForeignHabits.step(h.getLevel(), c, 1.0);
        h.assertTrue(TankInhabitants.hasTankRef(b) && c.getTank().inhabitants().contains(b.getUUID()), "with the lid on it stays");
        h.runAfterDelay(40, () -> {
            h.assertTrue(ForeignTerrarium.held(b), "the walker holds it in the air of the tank");
            h.assertTrue(TankInhabitants.tankBox(c.getTank()).contains(b.position()), "in the tank");
            unlid(h, c);
            ForeignHabits.step(h.getLevel(), c, 1.0); // a day with an open top: certain
            h.assertTrue(b.isAlive() && !TankInhabitants.hasTankRef(b), "out of the tank, alive");
            h.assertFalse(c.getTank().inhabitants().contains(b.getUUID()), "no inhabitant any more");
            h.assertTrue(b.getY() >= c.getTank().max().getY() + 1, "above the tank: " + b.getY());
            h.assertTrue(ForeignTerrariumGameTests.logged(c.getTank(), "log.bettacraft.foreign.flew_out"), "logged");
            b.discard();
            h.succeed();
        });
    }

    /**
     * The firefly glows at night in its closed tank (its own glow, flashing every few seconds) and stays in the daylight (Naturalist
     * lets a firefly vanish after half a minute in the sun: in our tank the sun ticks are kept at 0). Own batch: the time of day.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m39b_night", timeoutTicks = 400)
    public static void fireflyGlowsAtNightAndStaysByDay(GameTestHelper h) {
        if (skip(h, "naturalist")) return;
        ServerLevel level = h.getLevel();
        long time = level.getDayTime();
        EntityType<?> firefly = type("naturalist:firefly");
        try {
            TankBlockEntity c = paludarium(h, 2, 2, 8, 3, 6, 0, 0.5, Blocks.SAND.defaultBlockState());
            lid(h, c);
            LivingEntity f = put(h, c, container(h, firefly, false, false));
            h.assertTrue(ForeignHabits.glows(firefly), "a glowing profile");
            level.setDayTime(18000); // midnight
            boolean[] glowed = {false};
            h.onEachTick(() -> {
                if (ForeignHabits.glowing(f)) glowed[0] = true;
            });
            h.runAfterDelay(ForeignHabits.FLASH_EVERY + 20, () -> {
                try {
                    h.assertTrue(glowed[0], "it glowed in the dark tank");
                    level.setDayTime(6000); // noon: the sun would drive it away after 600 ticks
                    ForeignHabits.setSunTicks(f, 590);
                } catch (RuntimeException ex) {
                    level.setDayTime(time);
                    throw ex;
                }
            });
            h.runAfterDelay(ForeignHabits.FLASH_EVERY + 60, () -> {
                try {
                    h.assertTrue(f.isAlive(), "still there in the daylight");
                    h.assertTrue(ForeignHabits.sunTicks(f) < 100, "the sun ticks are kept down: " + ForeignHabits.sunTicks(f));
                    h.assertTrue(TankInhabitants.tankBox(c.getTank()).contains(f.position()), "in its tank");
                    h.succeed();
                } finally {
                    level.setDayTime(time);
                }
            });
        } catch (RuntimeException ex) {
            level.setDayTime(time);
            throw ex;
        }
    }

    // ------------------------------------------------------------------ the clean-up crew

    /**
     * The woodlouse (Critters and Companions, or a stand-in on its profile) is the clean-up crew of a terrarium: it eats the
     * leftovers of the food (they do not rot) and lives from the waste of a fed lizard; alone and unfed it gets hungry.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m39b_cleanup", timeoutTicks = 200)
    public static void cleanUpCrewEatsLeftoversAndWaste(GameTestHelper h) {
        var crew = ForeignTerrariumGameTests.animal("crittersandcompanions_roly_poly", "crittersandcompanions:roly_poly", EntityType.ARMADILLO);
        var lizard = ForeignTerrariumGameTests.animal("naturalist_lizard", "naturalist:lizard", EntityType.RABBIT);
        ForeignTerrariumGameTests.with(List.of(crew, lizard), () -> {
            ServerLevel level = h.getLevel();
            TankBlockEntity c = terrarium(h, 2, 2, 6, 3, 6);
            LivingEntity w = put(h, c, container(h, crew.type(), false, false));
            h.assertTrue(crew.profile().land().cleanup(), "the clean-up crew");
            ForeignProfile cp = ForeignProfiles.of(w.getType());
            if (cp == null) throw new GameTestAssertException("no profile for the woodlouse");
            // alone: hungry after its hunger days
            ForeignCare.advance(level, c, w, cp, cp.hungerDays() + 0.5);
            ForeignHabits.step(level, c, 0.01);
            h.assertTrue(ForeignCare.health(w).daysSinceFed() > cp.hungerDays(), "nobody leaves waste: it stays hungry");
            // leftovers: it eats one portion, the rest rots
            int left = ForeignHabits.eatLeftovers(level, c, 3);
            h.assertTrue(left == 2, "one woodlouse eats one of three portions: " + left);
            h.assertTrue(ForeignCare.health(w).daysSinceFed() < 1e-6, "fed by the leftovers");
            h.assertTrue(ForeignTerrariumGameTests.logged(c.getTank(), "log.bettacraft.foreign.cleanup"), "logged");
            // waste: a fed lizard in the tank feeds it
            ForeignCare.advance(level, c, w, cp, cp.hungerDays() + 0.5);
            LivingEntity l = put(h, c, container(h, lizard.type(), false, false));
            ForeignTerrariumGameTests.feed(l);
            ForeignHabits.step(level, c, 0.01);
            h.assertTrue(ForeignCare.health(w).daysSinceFed() < 1e-6, "it lives from the lizard's waste");
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ Naturalist mammals, birds and the komodo dragon

    /**
     * Naturalist's small mammals (hedgehog, mole, rat) go into a dry terrarium big enough for them; a pair of rats (a female and a
     * male) breeds (livebearers), two males do not. The songbird needs a lid. The elephant and the giraffe are refused as too big.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m39b_mammals", timeoutTicks = 300)
    public static void naturalistSmallMammalsAndBirds(GameTestHelper h) {
        if (skip(h, "naturalist")) return;
        ServerLevel level = h.getLevel();
        TankBlockEntity dry = terrarium(h, 2, 2, 8, 3, 6);
        TankBlockEntity palu = paludarium(h, 12, 2, 8, 3, 6, 0, 0.5, Blocks.SAND.defaultBlockState());
        for (String id : List.of("naturalist:hedgehog", "naturalist:mole", "naturalist:rat")) {
            EntityType<?> t = type(id);
            h.assertTrue(BigAnimals.length(t, null) > 0 && BigAnimals.length(t, null) < 0.3, id + " has its small real length");
            expect(h, dry, t, null, id + " into a dry terrarium");
            expect(h, palu, t, WRONG, id + " into a paludarium");
        }
        EntityType<?> bird = type("naturalist:bird");
        expect(h, dry, bird, LID, "a songbird into an open terrarium");
        // the rats breed by their sexes
        EntityType<?> rat = type("naturalist:rat");
        TankBlockEntity rats = terrarium(h, 2, 12, 8, 3, 6);
        LivingEntity a = put(h, rats, container(h, rat, false, false)), b = put(h, rats, container(h, rat, false, false));
        assertSex(a, false);
        assertSex(b, false);
        for (int i = 0; i < 40; i++) {
            ForeignCare.feed(List.of(a, b), 2, null);
            ForeignBreeding.step(level, rats, 0.5);
        }
        h.assertFalse(a.getPersistentData().contains(ForeignBreeding.BROOD) || b.getPersistentData().contains(ForeignBreeding.BROOD), "two males: no young");
        assertSex(a, true);
        LivingEntity mother = null;
        for (int i = 0; i < 80 && mother == null; i++) {
            ForeignCare.feed(List.of(a, b), 2, null);
            ForeignBreeding.step(level, rats, 0.5);
            if (a.getPersistentData().contains(ForeignBreeding.BROOD)) mother = a;
        }
        h.assertTrue(mother == a, "the female carries young");
        int before = ofType(h, rats, rat).size();
        for (int i = 0; i < 20 && ofType(h, rats, rat).size() == before; i++) {
            ForeignCare.feed(ofType(h, rats, rat), 9, null);
            ForeignBreeding.step(level, rats, 0.5);
        }
        h.assertTrue(ofType(h, rats, rat).size() > before, "young rats are born");
        for (String id : List.of("naturalist:elephant", "naturalist:giraffe", "naturalist:hippo")) {
            h.assertTrue("too_big".equals(ForeignProfiles.refusal(type(id))), id + " refused as too big");
        }
        h.succeed();
    }

    /**
     * The komodo dragon (2.5 m) by the size rule over the land: not into a terrarium of 12 x 3 x 6 (the minimum named), into one of
     * 30 x 8 x 15. Caught in the world with the large net into an empty tub (the real click, before the animal's own), put into the
     * big terrarium from the tub, and let go again on the land by the tub. It eats raw meat, does not breed and needs no lid.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m39b_komodo", timeoutTicks = 300)
    public static void komodoDragonInABigTerrarium(GameTestHelper h) {
        if (skip(h, "naturalist")) return;
        EntityType<?> komodo = type("naturalist:komodo_dragon");
        ForeignProfile p = profile(komodo);
        h.assertTrue(Math.abs(BigAnimals.length(komodo, null) - 2.5) < 1e-6 && !p.land().box() && !p.land().lid(), "2.5 m, tub, no lid");
        h.assertFalse(p.breeding().breeds(), "no breeding in a tank");
        TankBlockEntity small = terrarium(h, 1, 17, 12, 3, 6);
        expect(h, small, komodo, TOO_SMALL, "komodo into 12 x 3 x 6");
        TankBlockEntity big = terrarium(h, 1, 1, 30, 8, 15);
        // caught with the large net
        ServerPlayer player = MockPlayers.serverPlayer(h);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LARGE_NET.get()));
        player.getInventory().add(new ItemStack(ModItems.TRANSPORT_TUB.get()));
        LivingEntity k = wild(h, komodo, new Vec3(16, 2, 26));
        player.moveTo(k.getX() + 1.5, k.getY(), k.getZ());
        player.interactOn(k, InteractionHand.MAIN_HAND);
        h.assertFalse(k.isAlive(), "the net caught it");
        ItemStack tub = ItemStack.EMPTY;
        for (ItemStack s : player.getInventory().items) if (TransportTubItem.isFilled(s)) tub = s;
        h.assertTrue(TransportTubItem.containedType(tub) == komodo, "a tub with the komodo dragon");
        h.assertTrue(says(insert(h, small, tub.copy()), TOO_SMALL), "still too small");
        LivingEntity in = put(h, big, tub.copy());
        h.assertTrue(BigAnimals.isBig(in) && in.isAlive(), "in the big terrarium");
        int fed = 0;
        ForeignCare.advance(h.getLevel(), big, in, p, 3.0);
        List<LivingEntity> eaters = ForeignCare.eaters(h.getLevel(), big.getTank(), new ItemStack(net.minecraft.world.item.Items.BEEF), null);
        fed += ForeignCare.feed(eaters, 1, null);
        h.assertTrue(fed == 1 && ForeignCare.health(in).daysSinceFed() < 1e-6, "it eats raw meat");
        // back onto the land with the tub: a land animal needs no water
        ItemStack held = tub.copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        BlockPos ground = h.absolutePos(new BlockPos(20, 4, 28));
        while (h.getLevel().getBlockState(ground).isAir() && ground.getY() > h.getLevel().getMinBuildHeight()) ground = ground.below();
        player.moveTo(ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5, 0F, 90F); // looking straight down onto the ground
        var r = held.getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(r.getResult().consumesAction(), "let go on the land: " + r.getResult());
        h.assertFalse(TransportTubItem.isFilled(r.getObject()), "the tub is empty again");
        for (Entity e : h.getLevel().getEntities(komodo, TankInhabitants.tankBox(big.getTank()).inflate(40), e -> !TankInhabitants.hasTankRef(e))) e.discard();
        player.discard();
        h.succeed();
    }

    /** Naturalist's big mammals by the size rule: the lion (2 m) not into 12 x 3 x 6, into 24 x 6 x 12; the bear (2.2 m) not even there. */
    @GameTest(template = EMPTY, batch = "bettacraft_m39b_big", timeoutTicks = 300)
    public static void bigMammalsByTheSizeRule(GameTestHelper h) {
        if (skip(h, "naturalist")) return;
        EntityType<?> lion = type("naturalist:lion"), bear = type("naturalist:bear"), deer = type("naturalist:deer");
        TankBlockEntity small = terrarium(h, 1, 18, 12, 3, 6);
        TankBlockEntity big = terrarium(h, 1, 1, 24, 6, 12);
        expect(h, small, lion, TOO_SMALL, "lion into 12 x 3 x 6");
        expect(h, big, bear, TOO_SMALL, "bear (2.2 m) into 24 x 6 x 12");
        expect(h, big, lion, null, "lion into 24 x 6 x 12");
        expect(h, big, deer, null, "deer (1.7 m) into 24 x 6 x 12");
        for (String id : List.of("naturalist:lion", "naturalist:tiger", "naturalist:bear", "naturalist:zebra", "naturalist:ostrich", "naturalist:vulture")) {
            ForeignProfile p = profile(type(id));
            h.assertTrue(!p.land().box() || p.land().lengthM() < 1.2, id + ": large net and tub");
            h.assertFalse(p.breeding().breeds(), id + " does not breed in a tank");
        }
        h.assertTrue(profile(type("naturalist:vulture")).land().lid(), "the vulture needs a lid");
        h.succeed();
    }

    // ------------------------------------------------------------------ Critters and Companions

    /**
     * Critters and Companions: the terrarium crawlers (roly-poly, snail, stag beetle, stick bug, jumping spider, acorn weevil) into a dry
     * terrarium, the ladybug and the shima enaga only with a lid; the ferret and the red panda by their small real length; the box
     * catches a wild ferret (the real click). The insects do not breed, the clean-up crew does.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m39b_cnc", timeoutTicks = 300)
    public static void crittersAndCompanionsLandAnimals(GameTestHelper h) {
        if (skip(h, "crittersandcompanions")) return;
        TankBlockEntity dry = terrarium(h, 2, 2, 8, 3, 6);
        TankBlockEntity palu = paludarium(h, 12, 2, 8, 3, 6, 0, 0.5, Blocks.SAND.defaultBlockState());
        for (String id : List.of("roly_poly", "snail", "stag_beetle", "stick_bug", "jumping_spider", "weevil", "ferret", "red_panda")) {
            EntityType<?> t = type("crittersandcompanions:" + id);
            expect(h, dry, t, null, id + " into a dry terrarium");
            expect(h, palu, t, WRONG, id + " into a paludarium");
        }
        for (String id : List.of("ladybug", "shima_enaga")) expect(h, dry, type("crittersandcompanions:" + id), LID, id + " into an open terrarium");
        h.assertTrue(profile(type("crittersandcompanions:roly_poly")).land().cleanup() && profile(type("crittersandcompanions:snail")).land().cleanup(),
                "the clean-up crew");
        h.assertFalse(profile(type("crittersandcompanions:stag_beetle")).breeding().breeds(), "an insect: no breeding");
        h.assertTrue(profile(type("crittersandcompanions:roly_poly")).breeding().breeds(), "the woodlice breed");
        lid(h, dry);
        for (String id : List.of("ladybug", "shima_enaga")) expect(h, dry, type("crittersandcompanions:" + id), null, id + " into a closed terrarium");
        // the box on a wild ferret
        ServerPlayer player = MockPlayers.serverPlayer(h);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TERRARIUM_BOX.get(), 2));
        LivingEntity f = wild(h, type("crittersandcompanions:ferret"), new Vec3(6, 2, 26));
        player.moveTo(f.getX() + 1, f.getY(), f.getZ());
        player.interactOn(f, InteractionHand.MAIN_HAND);
        h.assertFalse(f.isAlive(), "the ferret went into the box");
        boolean boxed = false;
        for (ItemStack s : player.getInventory().items) if (TerrariumBoxItem.containedType(s) == f.getType()) boxed = true;
        h.assertTrue(boxed, "a box with the ferret");
        player.discard();
        h.succeed();
    }

    // ------------------------------------------------------------------ Living Things

    /**
     * Living Things: the penguin in a cold paludarium (it swims and hunts small fish), the flamingo in a warm one (both by the size
     * rule); koala, monkey, raccoon and snail in a terrarium, the owl and the peacock only with a lid; the giraffe and the elephant
     * are refused as too big, the nether knight as hostile.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m39b_livingthings", timeoutTicks = 300)
    public static void livingThingsLandAnimals(GameTestHelper h) {
        if (skip(h, "livingthings")) return;
        TankBlockEntity dry = terrarium(h, 1, 1, 12, 3, 6);
        for (String id : List.of("koala", "monkey", "raccoon", "snail")) expect(h, dry, type("livingthings:" + id), null, id + " into a terrarium");
        for (String id : List.of("owl", "peacock")) expect(h, dry, type("livingthings:" + id), LID, id + " into an open terrarium");
        lid(h, dry);
        for (String id : List.of("owl", "peacock")) expect(h, dry, type("livingthings:" + id), null, id + " into a closed terrarium");
        EntityType<?> penguin = type("livingthings:penguin"), flamingo = type("livingthings:flamingo");
        TankBlockEntity cold = paludarium(h, 1, 9, 16, 4, 8, 0, 0.5, Blocks.SAND.defaultBlockState());
        cold.getTank().setClimate(new org.overengineer.bettacraft.terrarium.Climate.State(4, 4, 4, 70, 0, 0));
        cold.getTank().setWaterTemp(6F);
        expect(h, cold, penguin, null, "penguin into a cold paludarium");
        h.assertTrue(profile(penguin).land().preyLength() > 0, "it hunts small fish");
        TankBlockEntity small = paludarium(h, 18, 1, 6, 3, 6, 0, 0.5, Blocks.SAND.defaultBlockState());
        expect(h, small, flamingo, TOO_SMALL, "flamingo (1.2 m) into a small paludarium");
        expect(h, cold, flamingo, null, "flamingo into the big paludarium");
        expect(h, dry, flamingo, WRONG, "flamingo into a terrarium");
        h.assertTrue("too_big".equals(ForeignProfiles.refusal(type("livingthings:giraffe"))), "giraffe too big");
        h.assertTrue("too_big".equals(ForeignProfiles.refusal(type("livingthings:elephant"))), "elephant too big");
        h.assertTrue("hostile".equals(ForeignProfiles.refusal(type("livingthings:nether_knight"))), "nether knight hostile");
        h.assertTrue(says(TankInhabitants.insert(h.getLevel(), dry, container(h, type("livingthings:giraffe"), true, false), type("livingthings:giraffe"), null, 0),
                "message.bettacraft.tank.refused.too_big"), "the tub with a giraffe is refused with the reason");
        h.succeed();
    }

    // ------------------------------------------------------------------ Fintastic arapaima (multi-part)

    /**
     * The arapaima of Fintastic is a multi-part animal (head and tail parts): from a transport tub into a big tank of 30 x 8 x 16, it
     * lives there with its parts inside the tank; the large net takes it out into an empty tub again (the parts go with it) and it goes
     * back in from the tub.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m39b_arapaima", timeoutTicks = 400)
    public static void multiPartArapaimaGoesInAndOut(GameTestHelper h) {
        if (skip(h, "fintastic")) return;
        EntityType<?> arapaima = type("fintastic:arapaima");
        TankBlockEntity c = ForeignGameTests.tank(h, 1, 1, 30, 8, 16, 27F);
        Entity e0 = arapaima.create(h.getLevel());
        ItemStack tub = TransportTubItem.fillWith(e0);
        e0.discard();
        LivingEntity a = put(h, c, tub);
        h.assertTrue(a.isMultipartEntity() && a.getParts() != null && a.getParts().length > 0, "a multi-part animal");
        h.runAfterDelay(60, () -> {
            TankData t = c.getTank();
            h.assertTrue(a.isAlive() && t.inhabitants().contains(a.getUUID()), "alive in the tank");
            var box = TankInhabitants.tankBox(t).inflate(0.5);
            h.assertTrue(box.contains(a.position()), "in the tank: " + a.position());
            for (var part : a.getParts()) h.assertTrue(box.contains(part.position()), "its part " + part.position() + " in the tank too");
            h.assertTrue(TankInhabitants.liveInhabitants(h.getLevel(), t).stream().noneMatch(x -> x instanceof net.neoforged.neoforge.entity.PartEntity<?>),
                    "the parts are no inhabitants");
            // out with the large net (on a part too: the click goes to the animal)
            ServerPlayer player = MockPlayers.serverPlayer(h);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LARGE_NET.get()));
            player.getInventory().add(new ItemStack(ModItems.TRANSPORT_TUB.get()));
            player.moveTo(a.getX(), t.max().getY() + 1.5, a.getZ());
            player.interactOn(a, InteractionHand.MAIN_HAND);
            h.assertFalse(a.isAlive(), "caught");
            h.assertFalse(t.inhabitants().contains(a.getUUID()), "no inhabitant any more");
            ItemStack back = ItemStack.EMPTY;
            for (ItemStack s : player.getInventory().items) if (TransportTubItem.isFilled(s)) back = s;
            h.assertTrue(TransportTubItem.containedType(back) == arapaima, "in the tub");
            h.assertTrue(h.getLevel().getEntities((Entity) null, box.inflate(4), x -> x instanceof net.neoforged.neoforge.entity.PartEntity<?>).isEmpty(),
                    "no part left behind");
            LivingEntity again = put(h, c, back.copy());
            h.assertTrue(again.isAlive() && BigAnimals.isBig(again), "back in from the tub");
            player.discard();
            h.succeed();
        });
    }

    /** GameTests elsewhere: the lid helper. */
    static boolean open(GameTestHelper h, TankBlockEntity c) {
        return org.overengineer.bettacraft.mobs.GlassMobs.openTop(h.getLevel(), c.getTank());
    }

    @SuppressWarnings("unused")
    private static Mob mob(Entity e) {
        return (Mob) e;
    }
}
