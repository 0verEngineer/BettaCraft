package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.critters.TankTerrain;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.foreign.ForeignBreeding;
import org.overengineer.bettacraft.foreign.ForeignNests;
import org.overengineer.bettacraft.foreign.ForeignProfile;
import org.overengineer.bettacraft.foreign.ForeignProfiles;
import org.overengineer.bettacraft.foreign.ForeignTerrarium;
import org.overengineer.bettacraft.foreign.ForeignWaterPart;
import org.overengineer.bettacraft.gametest.ForeignTerrariumGameTests.Animal;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.overengineer.bettacraft.terrarium.Climate;
import org.overengineer.bettacraft.terrarium.LandWalker;
import org.overengineer.bettacraft.terrarium.TankClimate;
import org.overengineer.bettacraft.terrarium.TerrariumBoxItem;
import org.overengineer.bettacraft.terrarium.TerrariumMode;

import static org.overengineer.bettacraft.gametest.ForeignTerrariumGameTests.*;

/**
 * M35 part b: the other land animals of Naturalist (lizard, snake, the two scorpions, snail, crab, duck, capybara, dragonfly) and
 * of Critters and Companions (dragonfly, leaf insect, otter, sea bunny) by their shipped profiles; without the mod a vanilla
 * stand-in with the same profile (the rabbit on the land, the bee for the fliers; the sea bunny is skipped). The fish in the water
 * part of a paludarium: ours, vanilla and other mods' fish go in by the water part (not the fill of the whole tank) and stay in it;
 * a mother that leaves her tank takes her laid clutch along and lays it again in her next tank.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ForeignTerrariumBGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private ForeignTerrariumBGameTests() {}

    private static Animal land(String file, String id) {
        return animal(file, id, EntityType.RABBIT);
    }

    /** Inserts the animal (a new one from its box or tub), asserts the outcome and takes it out again. */
    private static void tryInsert(GameTestHelper h, TankBlockEntity c, Animal a, boolean tub, String expectKey, String what) {
        Component err = insert(h, c, container(h, a.type(), tub, false));
        if (expectKey == null) {
            if (err != null) throw new GameTestAssertException(what + " refused: " + err.getString());
            for (LivingEntity e : ofType(h, c, a.type())) {
                c.getTank().inhabitants().remove(e.getUUID());
                TankInhabitants.release(e);
                e.discard();
            }
        } else if (!says(err, expectKey)) {
            throw new GameTestAssertException(what + ": expected " + expectKey + ", got " + (err == null ? "accepted" : err.getString()));
        }
    }

    // ------------------------------------------------------------------ the animals and their tanks

    /**
     * Every land animal of part b only into its kind of tank: the dry terrarium ones (lizard, snake, scorpions, snail, leaf
     * insect) not into a paludarium, the paludarium ones (crab, dragonflies) not into a terrarium, the big ones (duck 0.6 m,
     * capybara 1.2 m, otter 1 m) only into a paludarium big enough (the size rule names the minimum).
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m35b_land", timeoutTicks = 300)
    public static void partBAnimalsOnlyIntoTheirTanks(GameTestHelper h) {
        TankBlockEntity dry = terrarium(h, 2, 2, 12, 3, 6);
        TankBlockEntity palu = paludarium(h, 2, 10, 12, 3, 6, 0, 0.5, Blocks.SAND.defaultBlockState());
        ForeignLandAnimalsGameTests.lid(h, palu); // M39 part b: the dragonflies need a closed top
        TankBlockEntity small = paludarium(h, 16, 2, 6, 3, 6, 0, 0.5, Blocks.SAND.defaultBlockState());
        TankBlockEntity big = paludarium(h, 2, 18, 16, 4, 8, 0, 0.5, Blocks.SAND.defaultBlockState());
        String wrong = "message.bettacraft.terrarium.wrong_mode", tooSmall = "message.bettacraft.big.too_small";
        for (String[] d : new String[][]{{"naturalist_lizard", "naturalist:lizard"}, {"naturalist_snake", "naturalist:snake"},
                {"naturalist_desert_scorpion", "naturalist:desert_scorpion"}, {"naturalist_jungle_scorpion", "naturalist:jungle_scorpion"},
                {"naturalist_snail", "naturalist:snail"}, {"crittersandcompanions_leaf_insect", "crittersandcompanions:leaf_insect"}}) {
            Animal a = land(d[0], d[1]);
            with(List.of(a), () -> {
                tryInsert(h, dry, a, false, null, d[1] + " into a dry terrarium");
                tryInsert(h, palu, a, false, wrong, d[1] + " into a paludarium");
            });
        }
        for (String[] d : new String[][]{{"naturalist_crab", "naturalist:crab"}, {"naturalist_dragonfly", "naturalist:dragonfly"},
                {"crittersandcompanions_dragonfly", "crittersandcompanions:dragonfly"}}) {
            Animal a = land(d[0], d[1]);
            with(List.of(a), () -> {
                tryInsert(h, palu, a, false, null, d[1] + " into a paludarium");
                tryInsert(h, dry, a, false, wrong, d[1] + " into a terrarium");
            });
        }
        for (String[] d : new String[][]{{"naturalist_duck", "naturalist:duck"}, {"naturalist_capybara", "naturalist:capybara"},
                {"crittersandcompanions_otter", "crittersandcompanions:otter"}}) {
            Animal a = land(d[0], d[1]);
            boolean tub = !a.profile().land().box();
            with(List.of(a), () -> {
                tryInsert(h, small, a, tub, tooSmall, d[1] + " into a small paludarium");
                tryInsert(h, big, a, tub, null, d[1] + " into a big paludarium");
                tryInsert(h, dry, a, tub, wrong, d[1] + " into a terrarium");
            });
        }
        h.succeed();
    }

    /** The sea bunny (Critters and Companions) by its aquarium profile: into a sea water tank (fresh water refused with realistic foreign salinity). */
    @GameTest(template = EMPTY, batch = "bettacraft_m35b_land", timeoutTicks = 100)
    public static void seaBunnyIntoSeaWater(GameTestHelper h) {
        if (!org.overengineer.bettacraft.platform.Platform.get().isModLoaded("crittersandcompanions")) {
            h.succeed();
            return;
        }
        EntityType<?> bunny = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse("crittersandcompanions:sea_bunny")).orElseThrow();
        ForeignProfile p = ForeignProfiles.of(bunny);
        h.assertTrue(p != null && p.land() == null && p.salinity().min() >= 20, "a sea water aquarium profile");
        TankBlockEntity sea = ForeignGameTests.tank(h, 2, 2, 3, 3, 3, 25F);
        TankInteractions.changeWater(h.getLevel(), sea, 0, 35F);
        sea.getTank().setSalinity(35F);
        TankBlockEntity fresh = ForeignGameTests.tank(h, 8, 2, 3, 3, 3, 25F);
        Entity e = bunny.create(h.getLevel());
        ItemStack bag = FishBagItem.fillWith(e);
        e.discard();
        // the salt of a foreign profile counts with salt.foreignSalinity = REALISTIC (default LENIENT: no refusal)
        boolean realistic = org.overengineer.bettacraft.foreign.ForeignProfiles.salinity(p) != null;
        Component no = TankInhabitants.insert(h.getLevel(), fresh, bag.copy(), bunny, null, 100);
        h.assertTrue(realistic == (no != null), "fresh water refuses it only when realistic: " + (no == null ? "accepted" : no.getString()));
        Component ok = TankInhabitants.insert(h.getLevel(), sea, bag.copy(), bunny, null, 100);
        h.assertTrue(ok == null, "into sea water: " + (ok == null ? "" : ok.getString()));
        h.succeed();
    }

    /** The dragonfly flies: our walker holds it in the air of the paludarium, above the water and the land, and in the tank. */
    @GameTest(template = EMPTY, batch = "bettacraft_m35b_fly", timeoutTicks = 300)
    public static void dragonflyFliesInThePaludarium(GameTestHelper h) {
        Animal a = animal("naturalist_dragonfly", "naturalist:dragonfly", EntityType.BEE);
        Runnable restore = install(List.of(a));
        try {
            h.assertTrue(a.profile().land().gait().style() == LandWalker.Style.FLY, "the flying style");
            TankBlockEntity c = paludarium(h, 2, 2, 12, 3, 6, 0, 0.5, Blocks.SAND.defaultBlockState());
            ForeignLandAnimalsGameTests.lid(h, c); // M39 part b: a flier needs a closed top
            LivingEntity d = put(h, c, container(h, a.type(), false, false));
            double surface = c.getTank().waterSurfaceY();
            h.runAfterDelay(5, () -> {
                LandWalker w0 = ForeignTerrarium.walker(d);
                if (w0 != null) w0.forceActivity(LandWalker.Activity.LAND); // in the air now (a rest on the land would take long)
            });
            h.runAfterDelay(120, () -> {
                try {
                    LandWalker w = ForeignTerrarium.walker(d);
                    h.assertTrue(w != null && ForeignTerrarium.held(d), "the walker holds it");
                    h.assertTrue(TankInhabitants.tankBox(c.getTank()).contains(d.position()), "it stays in the tank");
                    double top = c.getTank().min().getY() + w.highestClimb() / 16.0;
                    h.assertTrue(top > surface + 0.3, "it flew above the water and the land: up to " + top + ", surface " + surface);
                    h.assertTrue(d.getY() >= surface, "never under the water: " + d.getY());
                    h.succeed();
                } finally {
                    restore.run();
                }
            });
        } catch (RuntimeException ex) {
            restore.run();
            throw ex;
        }
    }

    // ------------------------------------------------------------------ fish in the water part

    private static Entity firstOf(GameTestHelper h, String file) {
        ForeignProfile p = ForeignProfiles.files().get(BettaCraft.id(file));
        if (p == null) throw new GameTestAssertException("no shipped profile " + file);
        for (ResourceLocation id : p.entities()) {
            var t = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
            if (t.isPresent()) return t.get().create(h.getLevel());
        }
        return null;
    }

    /**
     * Fish in a paludarium (a third of water, half land): an aquarium with a third of water refuses a fish by the fill rule, the
     * paludarium takes ours (a betta), vanilla (a tropical fish, M16) and the fish of the other mods with a profile (Fintastic,
     * Aquaculture, Koi, Naturalist, as far as installed) by its water part; water too shallow for a fish is refused. In the tank
     * they stay in the water part: a fish of another mod pushed into the land is put back into the open water.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m35b_fish", timeoutTicks = 300)
    public static void fishLiveInThePaludariumWaterPart(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity aq = CritterGameTests.tank(h, 20, 2, 2, 4, 3, 4, 4 * 3 * 4 * 1000L / 3);
        Component no = TankInhabitants.insert(level, aq, new ItemStack(Items.TROPICAL_FISH_BUCKET), EntityType.TROPICAL_FISH, null, false);
        h.assertTrue(says(no, "message.bettacraft.tank.too_little_water"), "an aquarium with a third of water refuses: " + (no == null ? "-" : no.getString()));
        TankBlockEntity c = paludarium(h, 2, 2, 12, 3, 6, 0, 0.5, Blocks.SAND.defaultBlockState());
        TankData t = c.getTank();
        { // a solid bank under the land layer (the helper's land is a floating layer: open water under it)
            var r = InteriorRegions.get(level.getServer()).get(t.regionId());
            int top = r.sizeY() / 3 + 2;
            for (int lx = 0; lx < r.sizeX() / 2; lx++) for (int lz = 0; lz < r.sizeZ(); lz++) for (int ly = 0; ly < top; ly++) {
                interior(h).setBlock(r.toWorld(lx, ly, lz), Blocks.DIRT.defaultBlockState(), 2);
            }
        }
        List<Entity> fish = new ArrayList<>();
        BettaGenome g = BettaGenome.randomDomestic(RandomSource.create(35));
        h.assertTrue(TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), BettaData.create(g, true, 0)),
                org.overengineer.bettacraft.registry.ModEntities.BETTA.get(), null, false) == null, "our betta into the water part");
        Component trop = TankInhabitants.insert(level, c, new ItemStack(Items.TROPICAL_FISH_BUCKET), EntityType.TROPICAL_FISH, null, false);
        h.assertTrue(trop == null, "a tropical fish (M16) into the water part: " + (trop == null ? "" : trop.getString()));
        List<EntityType<?>> foreign = new ArrayList<>();
        for (String file : List.of("fintastic_guppy", "aquaculture_tropical", "crittersandcompanions_koi", "naturalist_food_fish")) {
            Entity e = firstOf(h, file);
            if (e == null) continue;
            ItemStack bag = FishBagItem.fillWith(e);
            e.discard();
            Component err = TankInhabitants.insert(level, c, bag, e.getType(), null, 100);
            h.assertTrue(err == null, file + " into the water part: " + (err == null ? "" : err.getString()));
            foreign.add(e.getType());
        }
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) fish.add(e);
        h.assertTrue(fish.size() == 2 + foreign.size(), "all in: " + fish.size());
        // M39 part c (flaky before): the fish came in at spots of the level's random (shared by every running test) and swam
        // by unseeded randoms, so now and then our betta ended up at the face of the bank; now fixed spots and seeds
        fish.sort(java.util.Comparator.comparing(e -> BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString()));
        for (int i = 0; i < fish.size(); i++) {
            Entity e = fish.get(i);
            e.moveTo(t.min().getX() + 8.0 + (i % 3) * 1.2, Math.max(t.min().getY() + 0.3, t.waterSurfaceY() - 0.45), t.min().getZ() + 1.5 + (i / 3) * 1.5, 90F * i, 0F);
            e.setDeltaMovement(Vec3.ZERO);
            if (e instanceof net.minecraft.world.entity.LivingEntity le) le.getRandom().setSeed(35_200L + i);
        }
        h.assertTrue(t.terrariumMode() == TerrariumMode.PALUDARIUM, "still a paludarium");
        // too shallow: a second paludarium with a sixth of a block of water
        TankBlockEntity shallow = paludarium(h, 2, 12, 12, 3, 6, 0, 0.5, Blocks.SAND.defaultBlockState());
        TankInteractions.changeWater(level, shallow, -(shallow.getTank().waterMb() - shallow.getTank().capacityMb() / 18));
        TankClimate.step(level, shallow, 0.01);
        Component flat = TankInhabitants.insert(level, shallow, new ItemStack(Items.TROPICAL_FISH_BUCKET), EntityType.TROPICAL_FISH, null, 0);
        h.assertTrue(says(flat, "message.bettacraft.paludarium.too_shallow"), "too shallow: " + (flat == null ? "accepted" : flat.getString()));
        // a fish of another mod (or vanilla) pushed into the land goes back into the water
        Entity pushed = null;
        for (Entity e : fish) if (!(e instanceof BettaEntity)) pushed = e;
        Entity victim = pushed;
        TankTerrain ter = new TankTerrain(t, level.getServer(), InteriorRegions.get(level.getServer()).get(t.regionId()), level.isDay());
        Vec3 inLand = new Vec3(t.min().getX() + 1.5, t.min().getY() + 0.5, t.min().getZ() + 3.0); // x < half: the land part
        h.assertFalse(ForeignWaterPart.inWaterPart(ter, inLand), "the spot is in the land");
        victim.setPos(inLand.x, inLand.y, inLand.z);
        h.runAfterDelay(100, () -> {
            TankTerrain now = new TankTerrain(t, level.getServer(), InteriorRegions.get(level.getServer()).get(t.regionId()), level.isDay());
            double surface = t.waterSurfaceY();
            for (Entity e : fish) {
                h.assertTrue(e.isAlive(), e.getType().toShortString() + " alive");
                // our betta keeps out of the land by the obstacles of the interior, at their resolution (an interior block, 1/16):
                // it may touch the face of the bank (its centre a hair inside the bank's column), never go in
                boolean in = ForeignWaterPart.inWaterPart(now, e.position());
                if (!in && e instanceof BettaEntity) {
                    for (double[] d : new double[][]{{0.07, 0}, {-0.07, 0}, {0, 0.07}, {0, -0.07}}) {
                        if (ForeignWaterPart.inWaterPart(now, e.position().add(d[0], 0, d[1]))) in = true;
                    }
                }
                h.assertTrue(in, e.getType().toShortString() + " in the water part: " + e.position());
                // M54: "not on the land" by the land under it, not by its height: a vanilla fish that leaves the water at the
                // surface for a moment (its own physics) is over the water part, not on the land (seen once: a tropical fish
                // 0.5 above the surface; not reproduced in 160 seeded runs at four times of day and every care phase)
                double under = now.ground(e.getX(), e.getZ());
                boolean overLand = Double.isFinite(under) && under > surface + 0.01;
                h.assertTrue(!overLand || e.getY() < surface + 0.05, e.getType().toShortString() + " in the water, not on the land: " + e.position()
                        + " (ground " + under + ", surface " + surface + ")");
            }
            h.assertTrue(ForeignWaterPart.putBacks(victim) >= 1, "the pushed fish was put back");
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ the clutch goes with its mother

    /**
     * A lizard mother with laid eggs leaves her tank in the terrarium box: the egg block goes (no empty shell), the clutch
     * stays in her; in her next terrarium she lays it again (the same eggs, the progress kept) and the young hatch there.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m35b_nest", timeoutTicks = 200)
    public static void motherTakesHerClutchAlong(GameTestHelper h) {
        Animal a = land("naturalist_lizard", "naturalist:lizard");
        with(List.of(a), () -> {
            ServerLevel level = h.getLevel();
            TankBlockEntity c = terrarium(h, 2, 2, 12, 3, 6);
            put(h, c, container(h, a.type(), false, false));
            put(h, c, container(h, a.type(), false, false));
            LivingEntity mother = layEggs(h, c, a.type());
            BlockPos egg = ForeignNests.eggBlockOf(mother);
            h.assertTrue(egg != null && interior(h).getBlockState(egg).is(ModBlocks.TURTLE_EGGS.get()), "the eggs in the interior");
            int eggs = org.overengineer.bettacraft.platform.Platform.get().persistentData(mother).getCompound(ForeignBreeding.BROOD).getCompound("nest").getInt("eggs");
            c.getTank().setClimate(new Climate.State(30, 31, 42, 50, 5, 0));
            ForeignBreeding.step(level, c, 1.0);
            double progress = org.overengineer.bettacraft.platform.Platform.get().persistentData(mother).getCompound(ForeignBreeding.BROOD).getCompound("nest").getDouble("progress");
            h.assertTrue(progress > 0.05 && progress < 1, "half way: " + progress);
            ServerPlayer player = MockPlayers.serverPlayer(h);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TERRARIUM_BOX.get()));
            c.getTank().inhabitants().remove(mother.getUUID());
            ForeignTerrarium.catchInto(player, InteractionHand.MAIN_HAND, mother);
            ItemStack box = ItemStack.EMPTY;
            for (ItemStack s : player.getInventory().items) if (TerrariumBoxItem.isFilled(s)) box = s;
            if (TerrariumBoxItem.isFilled(player.getItemInHand(InteractionHand.MAIN_HAND))) box = player.getItemInHand(InteractionHand.MAIN_HAND);
            h.assertTrue(TerrariumBoxItem.isFilled(box), "the mother in the box");
            h.assertFalse(interior(h).getBlockState(egg).is(ModBlocks.TURTLE_EGGS.get()), "no empty egg shell stays");
            CompoundTag stored = TerrariumBoxItem.storedEntity(box);
            CompoundTag brood = stored.getCompound("NeoForgeData").getCompound(ForeignBreeding.BROOD);
            h.assertTrue(brood.getInt("carried_eggs") == eggs && !brood.contains("nest"), "the clutch goes with her: " + brood);
            player.discard();
            TankBlockEntity next = terrarium(h, 2, 14, 12, 3, 6);
            LivingEntity back = put(h, next, box.copy());
            ForeignBreeding.step(level, next, ForeignNests.WALK_DAYS + 0.01);
            BlockPos again = ForeignNests.eggBlockOf(back);
            h.assertTrue(again != null && interior(h).getBlockState(again).is(ModBlocks.TURTLE_EGGS.get()), "laid again in the new terrarium");
            CompoundTag nest = org.overengineer.bettacraft.platform.Platform.get().persistentData(back).getCompound(ForeignBreeding.BROOD).getCompound("nest");
            h.assertTrue(nest.getInt("eggs") == eggs && Math.abs(nest.getDouble("progress") - progress) < 1e-6, "the same eggs, the progress kept: " + nest);
            next.getTank().setClimate(new Climate.State(30, 31, 42, 50, 5, 0));
            ForeignBreeding.step(level, next, 4.0);
            long babies = ofType(h, next, a.type()).stream().filter(LivingEntity::isBaby).count();
            h.assertTrue(babies == eggs, eggs + " young in the new terrarium, got " + babies);
            h.succeed();
        });
    }
}
