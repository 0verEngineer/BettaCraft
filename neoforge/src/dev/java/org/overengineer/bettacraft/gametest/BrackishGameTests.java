package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.critters.AmanoLarvae;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterObject;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.plants.Mangroves;
import org.overengineer.bettacraft.registry.ModBlocks;
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
import org.overengineer.bettacraft.tankmates.Archerfish;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.terrarium.HerpEntity;
import org.overengineer.bettacraft.terrarium.HerpSpawns;
import org.overengineer.bettacraft.terrarium.HerpSpecies;
import org.overengineer.bettacraft.terrarium.Mudskippers;
import org.overengineer.bettacraft.terrarium.TankClimate;
import org.overengineer.bettacraft.terrarium.TerrariumMode;
import org.overengineer.bettacraft.terrarium.TerrariumNeeds;

/**
 * M49 part b: the archerfish shoots the fruit flies let out over its open tank (and splashes a player leaning over it), the
 * mudskipper lives in a brackish paludarium and is stressed in fresh water, a mangrove grows from its propagule to a crown over
 * the water, the Amano larvae die in fresh water and settle in brackish water (the net carries them), and the switches.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BrackishGameTests {
    private static final String EMPTY = "empty_32x10x32";
    private static final double SECOND = 1.0 / 1200;

    private BrackishGameTests() {}

    private static void check(boolean ok, String what) {
        if (!ok) throw new GameTestFailure(what);
    }

    // ------------------------------------------------------------------ the archerfish

    /** Fruit flies over an open tank: the archerfish shoots them one by one and is fed; a lid keeps them out; a player is splashed. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void archerfishShootsTheFlies(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity open = SeahorseGameTests.tank(h, 2, 2, 2, 2, 2, 2, 7000, 8F, 27F);
        TankBlockEntity lidded = SeahorseGameTests.tank(h, 8, 2, 2, 2, 2, 2, 7000, 8F, 27F);
        for (int x = 8; x <= 9; x++) for (int z = 2; z <= 3; z++) h.setBlock(new BlockPos(x, 4, z), ModBlocks.TANK_LID.get().defaultBlockState());
        TankmateEntity a = SeahorseGameTests.add(h, open, TankmateSpecies.ARCHERFISH, true, LifeStage.ADULT);
        SeahorseGameTests.add(h, lidded, TankmateSpecies.ARCHERFISH, false, LifeStage.ADULT);
        check(TankCare.feed(level, lidded, new ItemStack(ModItems.FRUIT_FLIES.get()), 1) == 0 || lidded.getTank().flies() == 0,
                "under a lid the flies do not go over the water");
        check(lidded.getTank().flies() == 0, "no flies in the closed tank");
        check(TankCare.feed(level, open, new ItemStack(ModItems.FRUIT_FLIES.get()), 1) == 1, "the open tank takes the fruit flies");
        check(open.getTank().flies() == Archerfish.FLIES_PER_ITEM, "flies over the water: " + open.getTank().flies());
        a.health().feedPoorly(1.0); // a day since it ate
        double hungry = a.health().daysSinceFed();
        for (int i = 0; i < 200 && open.getTank().flies() >= 1; i++) Archerfish.step(level, open, SECOND);
        check(open.getTank().flies() < 1, "the archerfish shot them all: " + open.getTank().flies());
        check(a.health().daysSinceFed() < hungry, "the shots fed it");
        // a player leaning over the open tank
        ServerPlayer p = MockPlayers.serverPlayer(h);
        BlockPos top = h.absolutePos(new BlockPos(2, 4, 2));
        p.snapTo(top.getX() + 0.5, top.getY() + 0.2, top.getZ() + 0.5);
        check(Archerfish.leansOver(p, Archerfish.openTops(level, open.getTank())), "the player's head is over the tank");
        p.igniteForSeconds(5);
        Archerfish.splash(level, open, a, p);
        check(!p.isOnFire(), "the splash put the fire out");
        p.snapTo(top.getX() + 6.5, top.getY() + 0.2, top.getZ() + 0.5);
        check(!Archerfish.leansOver(p, Archerfish.openTops(level, open.getTank())), "beside the tank: not over it");
        h.succeed();
    }

    // ------------------------------------------------------------------ the mudskipper

    private static InteriorRegion floor(GameTestHelper h, TankBlockEntity c, BlockState floor) {
        InteriorRegion r = InteriorManager.ensureRegion(h.getLevel(), c);
        ServerLevel in = InteriorManager.interiorLevel(h.getLevel().getServer());
        if (in == null) throw new GameTestFailure("no interior dimension");
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) {
            Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), floor, false);
        }
        return r;
    }

    /** A 3x3x3 paludarium with mud land over a third of it and water of {@code salt} ppt. */
    private static TankBlockEntity paludarium(GameTestHelper h, int x, float salt) {
        TankBlockEntity c = CritterGameTests.tank(h, x, 2, 2, 3, 3, 3, 0);
        InteriorRegion r = floor(h, c, Blocks.MUD.defaultBlockState());
        ServerLevel in = InteriorManager.interiorLevel(h.getLevel().getServer());
        for (int lx = 16; lx < r.sizeX(); lx++) for (int z = 0; z < r.sizeZ(); z++) for (int y = 0; y < 24; y++) {
            in.setBlock(r.toWorld(lx, y, z), y == 23 ? Blocks.MUD.defaultBlockState() : Blocks.DIRT.defaultBlockState(), 3);
        }
        TankInteractions.changeWater(h.getLevel(), c, 9000, salt);
        TankClimate.step(h.getLevel(), c, 0.01);
        TankData t = c.getTank();
        check(t.terrariumMode() == TerrariumMode.PALUDARIUM, "a paludarium: " + t.terrariumMode());
        t.setClimate(new org.overengineer.bettacraft.terrarium.Climate.State(28, 28, 28, 90, 0, 0));
        t.setWaterTemp(27F);
        return c;
    }

    private static HerpEntity putMudskipper(GameTestHelper h, TankBlockEntity c) {
        HerpEntity e = ModEntities.herp(HerpSpecies.MUDSKIPPER).create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (e == null) throw new GameTestFailure("no mudskipper");
        ItemStack box = e.getBucketItemStack();
        e.saveToBucketTag(box);
        e.discard();
        check(box.is(ModItems.herpContainer(HerpSpecies.MUDSKIPPER)), "it travels in a terrarium box");
        List<Entity> before = TankInhabitants.liveInhabitants(h.getLevel(), c.getTank());
        var err = TankInhabitants.insert(h.getLevel(), c, box, ModEntities.herp(HerpSpecies.MUDSKIPPER), null, 0L);
        if (err != null) throw new GameTestFailure("mudskipper in: " + err.getString());
        for (Entity x : TankInhabitants.liveInhabitants(h.getLevel(), c.getTank())) if (x instanceof HerpEntity m && !before.contains(x)) return m;
        throw new GameTestFailure("no mudskipper in the tank");
    }

    /** The mudskipper goes into a paludarium (not an aquarium); brackish water suits it, fresh water stresses it; it spawns in mangroves. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void mudskipperInABrackishPaludarium(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity brackish = paludarium(h, 2, 10F), fresh = paludarium(h, 12, 0F);
        TankBlockEntity aquarium = SeahorseGameTests.tank(h, 22, 2, 2, 2, 2, 2, 8000, 10F, 27F);
        HerpEntity m = putMudskipper(h, brackish);
        HerpEntity f = putMudskipper(h, fresh);
        ItemStack box = m.getBucketItemStack();
        check(TankInhabitants.insert(level, aquarium, box, ModEntities.herp(HerpSpecies.MUDSKIPPER), null, 0L) != null, "an aquarium without land refuses it");
        check(!Mudskippers.fresh(brackish.getTank()) && Mudskippers.fresh(fresh.getTank()), "brackish and fresh");
        m.onTerrariumCare(level, brackish, TerrariumNeeds.Assessment.FINE, 0.01);
        f.onTerrariumCare(level, fresh, TerrariumNeeds.Assessment.FINE, 0.01);
        check(!m.adjust(TerrariumNeeds.Assessment.FINE).problems().contains(TerrariumNeeds.Problem.FRESH_WATER), "brackish water suits it");
        check(f.adjust(TerrariumNeeds.Assessment.FINE).problems().contains(TerrariumNeeds.Problem.FRESH_WATER), "fresh water stresses it");
        check(Mudskippers.happy(level, brackish.getTank()) && !Mudskippers.happy(level, fresh.getTank()), "happy in brackish water only");
        check(HerpSpecies.MUDSKIPPER.gait().style() == org.overengineer.bettacraft.terrarium.LandWalker.Style.CLIMB, "it climbs");
        var mangrove = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME)
                .getOrThrow(net.minecraft.world.level.biome.Biomes.MANGROVE_SWAMP);
        check(HerpSpawns.home(mangrove) == HerpSpawns.Home.MANGROVE, "the mangrove swamp is its home");
        check(HerpSpawns.entries(HerpSpawns.Home.MANGROVE).stream().anyMatch(e -> e.species() == HerpSpecies.MUDSKIPPER)
                && HerpSpawns.entries(HerpSpawns.Home.MANGROVE).stream().anyMatch(e -> e.species() == HerpSpecies.SLIDER), "mudskippers and the swamp animals in mangroves");
        h.succeed();
    }

    // ------------------------------------------------------------------ the mangrove

    /** A propagule on the floor grows stage by stage: stilt roots in the water, a trunk, a crown of leaves over the water. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void mangroveGrowsOverTheWater(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = SeahorseGameTests.tank(h, 2, 2, 2, 2, 2, 2, 3500, 10F, 26F);
        InteriorRegion r = floor(h, c, Blocks.MUD.defaultBlockState());
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        String err = Mangroves.plant(level, c, r.sizeX() / 2, r.sizeZ() / 2);
        check(err == null, "planted: " + err);
        Mangroves.Tree m = c.getTank().mangroves().getFirst();
        check(in.getBlockState(r.toWorld(m.x, m.y, m.z)).is(Blocks.MANGROVE_PROPAGULE), "the propagule on the floor");
        check(Mangroves.plant(level, c, r.sizeX() / 2 + 1, r.sizeZ() / 2) != null, "not two in one spot");
        for (int i = 0; i < 400 && m.stage < Mangroves.STAGES; i++) Mangroves.step(level, c, 0.1);
        check(m.stage == Mangroves.STAGES, "full grown: stage " + m.stage);
        double surface = TankCare.waterLocalHeight(c.getTank(), r.scale());
        int leavesAbove = 0, roots = 0, logs = 0;
        for (int x = Math.max(0, m.x - 8); x <= Math.min(r.sizeX() - 1, m.x + 8); x++)
            for (int z = Math.max(0, m.z - 8); z <= Math.min(r.sizeZ() - 1, m.z + 8); z++)
                for (int y = 0; y < r.sizeY(); y++) {
                    BlockState s = in.getBlockState(r.toWorld(x, y, z));
                    if (s.is(Blocks.MANGROVE_LEAVES) && y > surface) leavesAbove++;
                    if (s.is(Blocks.MANGROVE_ROOTS) || s.is(Blocks.MUDDY_MANGROVE_ROOTS)) roots++;
                    if (s.is(Blocks.MANGROVE_LOG)) logs++;
                }
        check(leavesAbove > 10 && roots > 10 && logs > 3, "crown over the water " + leavesAbove + ", roots " + roots + ", trunk " + logs);
        check(Mangroves.crowns(c.getTank()) == 1 && c.getTank().log().stream().anyMatch(e -> e.key().equals("log.bettacraft.mangrove.crown")), "the crown logged");
        h.succeed();
    }

    // ------------------------------------------------------------------ the Amano shrimp

    private static Critter amano(TankData t, boolean female, double x, double y, double z) {
        Critter k = new Critter(t.nextCritterId(), CritterSpecies.AMANO_SHRIMP, female, CritterGenes.WILD, x, y, z);
        k.age = 30;
        t.critters().add(k);
        return k;
    }

    /**
     * A berried female in fresh water lets out larvae; they die there after a day. A second cloud is netted into a fish bag and
     * let out into a brackish tank with phytoplankton: after the larva days young shrimp settle there.
     */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void amanoLarvaeNeedBrackishWater(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity fresh = SeahorseGameTests.tank(h, 2, 2, 2, 2, 2, 2, 8000, 0F, 23F);
        TankBlockEntity brackish = SeahorseGameTests.tank(h, 10, 2, 2, 2, 2, 2, 8000, 12F, 23F);
        TankData f = fresh.getTank(), b = brackish.getTank();
        double cx = f.min().getX() + 1, cy = f.min().getY() + 0.1, cz = f.min().getZ() + 1;
        Critter mother = amano(f, true, cx, cy, cz);
        amano(f, false, cx + 0.3, cy, cz);
        check(AmanoLarvae.matesIn(f.salinity()) && !AmanoLarvae.matesIn(b.salinity()), "they mate in fresh water only");
        // the female is berried and her eggs are ripe: the next small animal step lets the larvae out
        mother.eggs = CritterSpecies.AMANO_SHRIMP.eggDays() + 0.01;
        mother.father = CritterGenes.WILD;
        TankCritters.step(level, fresh, 0.01);
        check(AmanoLarvae.count(f)[0] == 1, "a cloud of larvae, no young: " + AmanoLarvae.count(f)[0]);
        int youngBefore = 0;
        for (Critter k : f.critters()) if (k.alive() && !k.adult()) youngBefore++;
        check(youngBefore == 0, "no young shrimp hatch in fresh water");
        // the first cloud dies in fresh water after a day
        for (CritterObject o : f.critterObjects()) if (o.kind == CritterObject.Kind.LARVAE) o.age = AmanoLarvae.freshDays() + 0.01;
        AmanoLarvae.step(level, fresh, 0.01);
        check(AmanoLarvae.count(f)[0] == 0 && f.log().stream().anyMatch(e -> e.key().equals("log.bettacraft.amano.larvae_died")), "the larvae died in fresh water");
        // a second cloud: into a fish bag with the net, out into the brackish tank
        CritterObject cloud = AmanoLarvae.hatch(mother, f.nextCritterId(), org.overengineer.bettacraft.critters.NetCatching.random(level));
        f.critterObjects().add(cloud);
        int larvae = cloud.count;
        ServerPlayer p = MockPlayers.serverPlayer(h);
        p.getInventory().add(new ItemStack(ModItems.FISH_BAG.get()));
        AmanoLarvae.net(level, fresh, p, cloud);
        ItemStack bag = ItemStack.EMPTY;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).has(ModDataComponents.CRITTER_LARVAE.get())) bag = p.getInventory().getItem(i);
        check(!bag.isEmpty() && AmanoLarvae.count(f)[0] == 0, "the larvae in the bag");
        CritterObject carried = CritterObject.load(bag.get(ModDataComponents.CRITTER_LARVAE.get()));
        check(carried != null, "the bag holds the cloud");
        b.setPhytoplankton(20);
        AmanoLarvae.insert(level, brackish, carried, p);
        check(AmanoLarvae.count(b)[1] == larvae, "the larvae drift in the brackish water");
        for (CritterObject o : b.critterObjects()) if (o.kind == CritterObject.Kind.LARVAE) o.age = AmanoLarvae.freshDays() + 0.5;
        AmanoLarvae.step(level, brackish, 0.01);
        check(AmanoLarvae.count(b)[0] == 1, "in brackish water they live past the fresh water day");
        for (CritterObject o : b.critterObjects()) if (o.kind == CritterObject.Kind.LARVAE) o.age = AmanoLarvae.larvaDays() + 0.01;
        AmanoLarvae.step(level, brackish, 0.01);
        int young = 0;
        for (Critter k : b.critters()) if (k.alive() && k.species == CritterSpecies.AMANO_SHRIMP && !k.adult()) young++;
        check(AmanoLarvae.count(b)[0] == 0 && young > 0, "young shrimp settled: " + young + " of " + larvae);
        check(b.log().stream().anyMatch(e -> e.key().equals("log.bettacraft.amano.young")), "logged");
        h.succeed();
    }

    // ------------------------------------------------------------------ the switches

    /** Archerfish off: the flies are fed as before and nobody shoots; mangroves off: no planting; Amano off: the larvae die. */
    @GameTest(template = EMPTY, batch = "bettacraft_m49_brackish_off", timeoutTicks = 300)
    public static void brackishSwitchesOff(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = SeahorseGameTests.tank(h, 2, 2, 2, 2, 2, 2, 7000, 12F, 27F);
        SeahorseGameTests.add(h, c, TankmateSpecies.ARCHERFISH, true, LifeStage.ADULT);
        TankBlockEntity m = SeahorseGameTests.tank(h, 10, 2, 2, 2, 2, 2, 3500, 12F, 27F);
        InteriorRegion r = floor(h, m, Blocks.MUD.defaultBlockState());
        try {
            Features.set(Feature.ARCHERFISH, false);
            check(Archerfish.release(level, c, new ItemStack(ModItems.FRUIT_FLIES.get()), 1) == 0, "archerfish off: no flies over the water");
            c.getTank().setFlies(5);
            Archerfish.step(level, c, 0.1);
            check(c.getTank().flies() == 5, "archerfish off: it does not shoot");
            Features.set(Feature.ARCHERFISH, true);
            Features.set(Feature.MANGROVES, false);
            check("message.bettacraft.mangrove.off".equals(Mangroves.plant(level, m, r.sizeX() / 2, r.sizeZ() / 2)), "mangroves off: no planting");
            Features.set(Feature.MANGROVES, true);
            Features.set(Feature.AMANO_SHRIMP, false);
            CritterObject o = AmanoLarvae.hatch(new Critter(1, CritterSpecies.AMANO_SHRIMP, true, CritterGenes.WILD, 3, 3, 3), c.getTank().nextCritterId(),
                    org.overengineer.bettacraft.critters.NetCatching.random(level));
            c.getTank().critterObjects().add(o);
            AmanoLarvae.step(level, c, 0.01);
            check(AmanoLarvae.count(c.getTank())[0] == 0, "Amano off: the larvae die");
            check(!CritterSpecies.AMANO_SHRIMP.available(), "Amano off: none from the net or the trader");
            Features.set(Feature.AMANO_SHRIMP, true);
            Features.set(Feature.BRACKISH, false);
            check(!Features.enabled(Feature.ARCHERFISH) && !Features.enabled(Feature.MUDSKIPPER) && !Features.enabled(Feature.MANGROVES)
                    && !Features.enabled(Feature.AMANO_SHRIMP), "the group switch turns them all off");
            check(!TankmateSpecies.ARCHERFISH.available(), "no new archerfish");
        } finally {
            Features.set(Feature.BRACKISH, true);
            Features.set(Feature.ARCHERFISH, true);
            Features.set(Feature.MANGROVES, true);
            Features.set(Feature.AMANO_SHRIMP, true);
        }
        h.succeed();
    }
}
