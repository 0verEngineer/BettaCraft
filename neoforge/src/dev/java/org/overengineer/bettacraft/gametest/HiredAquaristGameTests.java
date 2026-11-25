package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import net.minecraft.world.InteractionResult;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.platform.item.IItemHandler;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.aquarist.AquaristAccess;
import org.overengineer.bettacraft.aquarist.AquaristArrival;
import org.overengineer.bettacraft.aquarist.AquaristEntity;
import org.overengineer.bettacraft.aquarist.AquaristRecords;
import org.overengineer.bettacraft.aquarist.AquaristRules;
import org.overengineer.bettacraft.aquarist.AquaristTask;
import org.overengineer.bettacraft.aquarist.AquaristWork;
import org.overengineer.bettacraft.compat.FtbTeamsCompat;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.progression.Progress;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.algae.AlgaeSim;
import org.overengineer.bettacraft.tank.algae.TankAlgae;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tank.plants.TankPlants;

/**
 * M51: the hired aquarist. The arrival after the conditions (own tanks, a breeding), the refusal and the next one after the
 * wait and another breeding, the offer running out; each task alone (feeding from the chest, water change from the
 * reservoir, algae, plants with the cuttings into the chest, young into the rearing tank, warnings); which tanks he works
 * at (own, team, released, foreign, locked, none wanted, ownerless); missing wage (slower, he leaves); the chunk of the
 * aquarist unloading while he carries young and food; the switch off; he cannot be hurt or caught; a walk to a real tank.
 * Every test has its own batch: an aquarist works in a radius of 32 blocks and must not meet the tanks of another test.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HiredAquaristGameTests {
    private static final String EMPTY = "empty_32x10x32";
    static final BlockPos BASIN = new BlockPos(4, 1, 4), CHEST = new BlockPos(5, 1, 4), RESERVOIR = new BlockPos(3, 1, 4), BED = new BlockPos(4, 1, 8);

    private HiredAquaristGameTests() {}

    private static void check(boolean ok, String what) {
        if (!ok) throw new GameTestFailure(what);
    }

    // ------------------------------------------------------------------ helpers

    static void floor(GameTestHelper h) {
        for (int x = 0; x < 32; x++) for (int z = 0; z < 32; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    }

    static ServerPlayer player(GameTestHelper h) {
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        sp.snapTo(h.absoluteVec(new Vec3(16, 1, 16)));
        return sp;
    }

    /** A 3x2x2 tank at x,z owned by {@code owner}, full of water, in the index. */
    static TankBlockEntity tank(GameTestHelper h, int x, int z, UUID owner) {
        for (int a = 0; a < 3; a++)
            for (int b = 0; b < 2; b++)
                for (int c = 0; c < 2; c++) {
                    BlockPos p = new BlockPos(x + a, 1 + b, z + c);
                    h.setBlock(p, ModBlocks.TANK.get().defaultBlockState());
                    TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(p), null);
                }
        TankBlockEntity c = TankMultiblock.getController(h.getLevel(), h.absolutePos(new BlockPos(x, 1, z)));
        if (c == null) throw new GameTestFailure("tank did not form");
        TankInteractions.changeWater(h.getLevel(), c, c.getTank().capacityMb(), 0F);
        c.getTank().setWaterTemp(26F);
        c.getTank().setTargetTemp(26F);
        c.getTank().setWaterQuality(1F);
        c.getTank().setOwner(owner);
        AquaristRecords.get(h.getLevel().getServer()).noteTank(h.getLevel(), c);
        return c;
    }

    /** The home: a breeding basin with a chest and a reservoir next to it, and a bed. */
    static void home(GameTestHelper h) {
        h.setBlock(BASIN, ModBlocks.BREEDING_BASIN.get().defaultBlockState());
        h.setBlock(CHEST, Blocks.CHEST.defaultBlockState());
        h.setBlock(RESERVOIR, ModBlocks.RESERVOIR.get().defaultBlockState());
        BlockState foot = Blocks.BED.pick(net.minecraft.world.item.DyeColor.RED).defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.FOOT);
        h.setBlock(BED, foot);
        h.setBlock(BED.south(), foot.setValue(BedBlock.PART, BedPart.HEAD));
    }

    /** A hired aquarist of {@code sp} at the home (tasks at their default levels). */
    static AquaristEntity npc(GameTestHelper h, ServerPlayer sp) {
        AquaristEntity npc = AquaristArrival.spawnHiredHomeless(h.getLevel(), sp.getUUID(), "tester", h.absoluteVec(new Vec3(6.5, 1, 6.5)));
        check(npc.setHome(h.getLevel(), h.absolutePos(BASIN), h.absolutePos(BED.south())), "the home (basin and bed) is free: " + npc.workplace() + " " + npc.bed());
        npc.alwaysDay = true;
        npc.setNoAi(true); // the tests do his jobs themselves (one test lets him walk)
        return npc;
    }

    static IItemHandler chest(GameTestHelper h) {
        IItemHandler c = org.overengineer.bettacraft.platform.Platform.get().itemHandler(h.getLevel(), h.absolutePos(CHEST), null);
        if (c == null) throw new GameTestFailure("no chest");
        return c;
    }

    static int count(IItemHandler h, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < h.getSlots(); i++) if (h.getStackInSlot(i).is(item)) n += h.getStackInSlot(i).getCount();
        return n;
    }

    static void put(IItemHandler h, ItemStack s) {
        ItemStack rest = s;
        for (int i = 0; i < h.getSlots() && !rest.isEmpty(); i++) rest = h.insertItem(i, rest, false);
    }

    static void done(GameTestHelper h, ServerPlayer sp, Runnable more) {
        try {
            more.run();
        } finally {
            AquaristRecords.get(h.getLevel().getServer()).reset(sp.getUUID());
        }
    }

    static void cleanup(GameTestHelper h, UUID... players) {
        var list = h.getLevel().getServer().getPlayerList();
        for (UUID p : players) {
            AquaristRecords.get(h.getLevel().getServer()).reset(p);
            // the mock players leave again: they would stand near the tests of later batches (alarm waypoints count players nearby)
            ServerPlayer sp = list.getPlayer(p);
            if (sp != null && "test-mock-player".equals(sp.getGameProfile().name())) list.remove(sp);
        }
        for (Entity e : h.getLevel().getEntities(ModEntities.HIRED_AQUARIST.get(), new net.minecraft.world.phys.AABB(h.absolutePos(BlockPos.ZERO)).inflate(40), x -> true)) e.discard();
    }

    static BettaEntity betta(GameTestHelper h, TankBlockEntity c, long seed) {
        return LiveFoodGameTests.betta(h, c, seed % 2 == 0, seed);
    }

    static boolean logged(TankData t, String key) {
        return t.log().stream().anyMatch(e -> e.key().equals(key));
    }

    static void run(GameTestHelper h, AquaristEntity npc, AquaristWork.Kind kind, TankBlockEntity c) {
        AquaristWork.perform(h.getLevel(), npc, new AquaristWork.Job(kind, c == null ? null : c.getTank().id()));
    }

    // ------------------------------------------------------------------ arrival, refusal

    /** No aquarist without three own tanks and a breeding; then one comes, offers, is hired (advancement event). */
    @GameTest(template = EMPTY, batch = "bettacraft_m51_arrival", timeoutTicks = 200)
    public static void arrivesAfterTanksAndABreeding(GameTestHelper h) {
        floor(h);
        ServerPlayer sp = player(h);
        try {
            check("tanks".equals(AquaristArrival.whyNot(sp, true)), "no tanks: " + AquaristArrival.whyNot(sp, true));
            for (int i = 0; i < 3; i++) tank(h, 4 + i * 5, 20, sp.getUUID());
            check("breeding".equals(AquaristArrival.whyNot(sp, true)), "three tanks, no breeding: " + AquaristArrival.whyNot(sp, true));
            Progress.award(sp, Progress.SPAWNED);
            check(AquaristArrival.whyNot(sp, true) == null, "after a breeding he may come: " + AquaristArrival.whyNot(sp, true));
            AquaristEntity npc = AquaristArrival.arrive(sp);
            check(npc != null && npc.phase() == AquaristEntity.Phase.OFFERING, "he came and offers his work");
            check(sp.getUUID().equals(npc.employer()) && npc.hasCustomName(), "he offers it to the player, with a first name");
            check("offering".equals(AquaristArrival.whyNot(sp, true)), "one at a time: " + AquaristArrival.whyNot(sp, true));
            check(AquaristArrival.hire(sp) == null, "hired");
            check(npc.phase() == AquaristEntity.Phase.HIRED && AquaristRecords.get(h.getLevel().getServer()).player(sp.getUUID()).hired, "he works now");
            check("hired".equals(AquaristArrival.whyNot(sp, true)), "no second one: " + AquaristArrival.whyNot(sp, true));
            h.succeed();
        } finally {
            cleanup(h, sp.getUUID());
        }
    }

    /** Refused, he leaves; the next one only after the wait and another breeding; an unanswered offer runs out. */
    @GameTest(template = EMPTY, batch = "bettacraft_m51_refusal", timeoutTicks = 400)
    public static void refusedHeLeavesAndAnotherComesLater(GameTestHelper h) {
        floor(h);
        ServerPlayer sp = player(h);
        MinecraftServerRef s = new MinecraftServerRef(h);
        for (int i = 0; i < 3; i++) tank(h, 4 + i * 5, 20, sp.getUUID());
        Progress.award(sp, Progress.SPAWNED);
        AquaristEntity first = AquaristArrival.arrive(sp);
        check(first != null, "one came");
        first.snapTo(h.absoluteVec(new Vec3(8, 1, 8))); // inside the test's forced chunks, so he ticks
        check(AquaristArrival.decline(sp) == null, "refused");
        check(first.phase() == AquaristEntity.Phase.LEAVING, "he leaves");
        AquaristRecords.PlayerRecord r = AquaristRecords.get(s.server()).player(sp.getUUID());
        long now = s.server().overworld().getGameTime();
        check(r.npc == null && r.refused == 1, "the record: none, refused once");
        long days = (r.notBefore - now) / 24000L;
        check(days >= ServerConfig.getOr(ServerConfig.AQUARIST_RETURN_MIN, 5) && days <= ServerConfig.getOr(ServerConfig.AQUARIST_RETURN_MAX, 10),
                "the next one in 5 to 10 days: " + days);
        check("wait".equals(AquaristArrival.whyNot(sp, true)), "waits: " + AquaristArrival.whyNot(sp, true));
        r.notBefore = now - 1; // the days went by
        check("breed_again".equals(AquaristArrival.whyNot(sp, true)), "needs another breeding: " + AquaristArrival.whyNot(sp, true));
        Progress.award(sp, Progress.LITTER);
        check(AquaristArrival.whyNot(sp, true) == null, "after another breeding: " + AquaristArrival.whyNot(sp, true));
        AquaristEntity second = AquaristArrival.arrive(sp);
        check(second != null && second != first, "the next one came");
        second.snapTo(h.absoluteVec(new Vec3(10, 1, 10)));
        // he waits for an answer only so long
        second.setNoAi(false);
        r.offerUntil = now - 1;
        AquaristRecords.get(s.server()).setDirty();
        second.setOfferUntil(now - 1);
        h.succeedWhen(() -> {
            check(second.phase() == AquaristEntity.Phase.LEAVING || second.isRemoved(), "the unanswered one leaves");
            check(first.isRemoved(), "the refused one walked off and is gone");
            AquaristRecords.PlayerRecord rr = AquaristRecords.get(s.server()).player(sp.getUUID());
            check(rr.npc == null && rr.refused == 2, "counted as refused: " + rr.refused);
            cleanup(h, sp.getUUID());
        });
    }

    record MinecraftServerRef(GameTestHelper h) {
        net.minecraft.server.MinecraftServer server() {
            return h.getLevel().getServer();
        }
    }

    // ------------------------------------------------------------------ the tasks

    /** Feeding: food from the chest into his bag, the hungry betta eats, the rest goes back; nothing is lost or made. */
    @GameTest(template = EMPTY, batch = "bettacraft_m51_feed", timeoutTicks = 200)
    public static void feedsFromTheChest(GameTestHelper h) {
        floor(h);
        home(h);
        ServerPlayer sp = player(h);
        try {
            TankBlockEntity c = tank(h, 10, 4, sp.getUUID());
            BettaEntity b = betta(h, c, 5101);
            b.health().feedPoorly(3);
            AquaristEntity npc = npc(h, sp);
            put(chest(h), new ItemStack(ModItems.BETTA_PELLETS.get(), 20));
            check(AquaristWork.feedDue(h.getLevel(), npc, c), "a feeding is due");
            List<AquaristWork.Job> plan = npc.planNow(h.getLevel());
            check(!plan.isEmpty() && plan.getFirst().kind() == AquaristWork.Kind.FETCH_FOOD, "first the food: " + plan);
            run(h, npc, AquaristWork.Kind.FETCH_FOOD, null);
            int inBag = count(npc.bag(), ModItems.BETTA_PELLETS.get());
            check(inBag > 0 && inBag + count(chest(h), ModItems.BETTA_PELLETS.get()) == 20, "taken, not made: bag " + inBag);
            run(h, npc, AquaristWork.Kind.TEND, c);
            check(b.health().daysSinceFed() < 0.01, "the betta ate");
            check(logged(c.getTank(), "log.bettacraft.aquarist.fed"), "in the tank log");
            check(!AquaristWork.feedDue(h.getLevel(), npc, c), "fed for now");
            int eaten = 20 - count(npc.bag(), ModItems.BETTA_PELLETS.get()) - count(chest(h), ModItems.BETTA_PELLETS.get());
            check(eaten >= 1 && eaten <= 2, "one portion eaten: " + eaten);
            run(h, npc, AquaristWork.Kind.DEPOSIT, null);
            check(count(npc.bag(), ModItems.BETTA_PELLETS.get()) == 0 && count(chest(h), ModItems.BETTA_PELLETS.get()) == 20 - eaten, "the rest back in the chest");
            h.succeed();
        } finally {
            cleanup(h, sp.getUUID());
        }
    }

    /** Water change: water from the reservoir at the basin into the bad water of the tank, the level stays. */
    @GameTest(template = EMPTY, batch = "bettacraft_m51_water", timeoutTicks = 200)
    public static void changesWaterFromTheReservoir(GameTestHelper h) {
        floor(h);
        home(h);
        ServerPlayer sp = player(h);
        try {
            TankBlockEntity c = tank(h, 10, 4, sp.getUUID());
            betta(h, c, 5102);
            c.getTank().setWaterQuality(0.3F);
            c.getTank().setNitrate(80F);
            IFluidHandler res = org.overengineer.bettacraft.platform.Platform.get().fluidHandler(h.getLevel(), h.absolutePos(RESERVOIR), null);
            check(res != null, "a reservoir");
            res.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 10000), IFluidHandler.FluidAction.EXECUTE);
            AquaristEntity npc = npc(h, sp);
            check(AquaristWork.waterDue(h.getLevel(), npc, c), "bad water: a change is due");
            long water = c.getTank().waterMb();
            int before = res.getFluidInTank(0).getAmount();
            run(h, npc, AquaristWork.Kind.FETCH_WATER, c);
            check(npc.waterMb() > 0 && res.getFluidInTank(0).getAmount() == before - npc.waterMb(), "he took water: " + npc.waterMb());
            int carried = npc.waterMb();
            run(h, npc, AquaristWork.Kind.CHANGE_WATER, c);
            check(npc.waterMb() == 0, "all poured in");
            check(c.getTank().waterMb() == water, "the level stays: " + c.getTank().waterMb() + " of " + water);
            check(c.getTank().waterQuality() > 0.4F && c.getTank().nitrate() < 70F, "fresher water: " + c.getTank().waterQuality() + " " + c.getTank().nitrate());
            check(carried == Math.round(c.getTank().capacityMb() * 0.25), "level 2: a quarter: " + carried);
            check(!AquaristWork.waterDue(h.getLevel(), npc, c), "not again the same day");
            check(logged(c.getTank(), "log.bettacraft.aquarist.water"), "logged");
            h.succeed();
        } finally {
            cleanup(h, sp.getUUID());
        }
    }

    /** Algae: scraped off every side once there are enough. */
    @GameTest(template = EMPTY, batch = "bettacraft_m51_algae", timeoutTicks = 200)
    public static void scrapesTheAlgae(GameTestHelper h) {
        floor(h);
        home(h);
        ServerPlayer sp = player(h);
        try {
            TankBlockEntity c = tank(h, 10, 4, sp.getUUID());
            AquaristEntity npc = npc(h, sp);
            for (AlgaeSim.Face f : TankAlgae.wetFaces(c.getTank())) AlgaeSim.set(c.getTank().algae(), f.pos(), f.dir(), 0.2F);
            check(!AquaristWork.algaeDue(npc, c), "level 2: not yet at 20 %");
            for (AlgaeSim.Face f : TankAlgae.wetFaces(c.getTank())) AlgaeSim.set(c.getTank().algae(), f.pos(), f.dir(), 0.8F);
            check(AquaristWork.algaeDue(npc, c), "at 80 %");
            run(h, npc, AquaristWork.Kind.TEND, c);
            check(TankAlgae.mean(c.getTank()) < 0.01, "clean glass: " + TankAlgae.mean(c.getTank()));
            check(logged(c.getTank(), "log.bettacraft.aquarist.algae"), "logged");
            npc.setTaskLevel(AquaristTask.ALGAE, 0);
            for (AlgaeSim.Face f : TankAlgae.wetFaces(c.getTank())) AlgaeSim.set(c.getTank().algae(), f.pos(), f.dir(), 0.9F);
            check(!AquaristWork.algaeDue(npc, c), "the task off: not due");
            h.succeed();
        } finally {
            cleanup(h, sp.getUUID());
        }
    }

    /** Plants: a dense cover of floating plants thinned out, the plants go into the chest. */
    @GameTest(template = EMPTY, batch = "bettacraft_m51_plants", timeoutTicks = 200)
    public static void thinsThePlantsIntoTheChest(GameTestHelper h) {
        floor(h);
        home(h);
        ServerPlayer sp = player(h);
        try {
            TankBlockEntity c = tank(h, 10, 4, sp.getUUID());
            AquaristEntity npc = npc(h, sp);
            var rnd = h.getLevel().getRandom();
            var min = c.getTank().min();
            for (int i = 0; i < 200 && TankPlants.coverage(c.getTank()) < 0.9; i++) {
                TankPlants.add(c, FloatingPlantType.SALVINIA, min.getX() + rnd.nextDouble() * 3, min.getZ() + rnd.nextDouble() * 2, 1F, 0F);
            }
            int plants = c.getTank().plants().size();
            check(TankPlants.coverage(c.getTank()) >= 0.9 && AquaristWork.plantsDue(h.getLevel(), npc, c), "dense cover: " + TankPlants.coverage(c.getTank()));
            run(h, npc, AquaristWork.Kind.TEND, c);
            int taken = plants - c.getTank().plants().size();
            check(taken > 0, "thinned out");
            int inBag = 0;
            for (int i = 0; i < npc.bag().getSlots(); i++) inBag += npc.bag().getStackInSlot(i).getCount();
            check(inBag == taken, "every plant in his bag: " + inBag + " of " + taken);
            run(h, npc, AquaristWork.Kind.DEPOSIT, null);
            int inChest = 0;
            for (int i = 0; i < chest(h).getSlots(); i++) inChest += chest(h).getStackInSlot(i).getCount();
            check(inChest == taken && !npc.hasCarriedItems(), "into the chest: " + inChest);
            h.succeed();
        } finally {
            cleanup(h, sp.getUUID());
        }
    }

    /** Young: a juvenile betta goes from its tank into the rearing tank shown to him (the same fish, the water moves along). */
    @GameTest(template = EMPTY, batch = "bettacraft_m51_young", timeoutTicks = 200)
    public static void movesTheYoungIntoTheRearingTank(GameTestHelper h) {
        floor(h);
        home(h);
        ServerPlayer sp = player(h);
        try {
            TankBlockEntity from = tank(h, 10, 4, sp.getUUID()), rear = tank(h, 10, 12, sp.getUUID());
            BettaEntity adult = betta(h, from, 5103);
            BettaEntity young = betta(h, from, 5104);
            young.setBettaData(young.getBettaData().withStage(LifeStage.JUVENILE));
            String fishId = young.getBettaData().fishId().toString();
            AquaristEntity npc = npc(h, sp);
            check(npc.planNow(h.getLevel()).stream().noneMatch(j -> j.kind() == AquaristWork.Kind.TAKE_YOUNG), "no rearing tank: no trip");
            npc.setRearingTank(rear.getTank().id());
            check(npc.planNow(h.getLevel()).stream().anyMatch(j -> j.kind() == AquaristWork.Kind.TAKE_YOUNG && from.getTank().id().equals(j.tank())), "a trip with the young");
            run(h, npc, AquaristWork.Kind.TAKE_YOUNG, from);
            check(npc.carriedAnimals().size() == 1 && !young.isAlive(), "he carries the juvenile");
            check(adult.isAlive() && TankInhabitants.liveInhabitants(h.getLevel(), from.getTank()).size() == 1, "the adult stays");
            run(h, npc, AquaristWork.Kind.RELEASE_YOUNG, rear);
            check(npc.carriedAnimals().isEmpty(), "put in");
            var in = TankInhabitants.liveInhabitants(h.getLevel(), rear.getTank());
            check(in.size() == 1 && in.getFirst() instanceof BettaEntity b && b.getBettaData().fishId().toString().equals(fishId), "the same fish lives in the rearing tank");
            check(logged(rear.getTank(), "log.bettacraft.aquarist.young_in"), "logged");
            h.succeed();
        } finally {
            cleanup(h, sp.getUUID());
        }
    }

    /** Warnings: too little water is reported once and cleared when the water is back. */
    @GameTest(template = EMPTY, batch = "bettacraft_m51_warn", timeoutTicks = 200)
    public static void reportsWarnings(GameTestHelper h) {
        floor(h);
        home(h);
        ServerPlayer sp = player(h);
        try {
            TankBlockEntity c = tank(h, 10, 4, sp.getUUID());
            betta(h, c, 5105);
            AquaristEntity npc = npc(h, sp);
            TankInteractions.changeWater(h.getLevel(), c, -(long) (c.getTank().capacityMb() * 0.7));
            check(AquaristWork.warnings(h.getLevel(), c, 1).contains("water_low"), "too little water: " + AquaristWork.warnings(h.getLevel(), c, 1));
            run(h, npc, AquaristWork.Kind.TEND, c);
            check(npc.reported().getOrDefault(c.getTank().id(), java.util.Set.of()).contains("water_low"), "reported: " + npc.reported());
            TankInteractions.changeWater(h.getLevel(), c, c.getTank().capacityMb());
            run(h, npc, AquaristWork.Kind.TEND, c);
            check(!npc.reported().containsKey(c.getTank().id()), "cleared: " + npc.reported());
            npc.setTaskLevel(AquaristTask.WARN, 0);
            check(!AquaristWork.warnDue(h.getLevel(), npc, c), "off: he does not look");
            h.succeed();
        } finally {
            cleanup(h, sp.getUUID());
        }
    }

    /** With his AI on he walks from his basin to the tank and feeds it (the round of jobs, no shortcut). */
    @GameTest(template = EMPTY, batch = "bettacraft_m51_walk", timeoutTicks = 1600)
    public static void walksToTheTankAndFeeds(GameTestHelper h) {
        floor(h);
        home(h);
        ServerPlayer sp = player(h);
        TankBlockEntity c = tank(h, 18, 18, sp.getUUID());
        BettaEntity b = betta(h, c, 5106);
        b.health().feedPoorly(3);
        put(chest(h), new ItemStack(ModItems.BETTA_PELLETS.get(), 10));
        AquaristEntity npc = npc(h, sp);
        npc.setTaskLevel(AquaristTask.WARN, 0);
        npc.setNoAi(false);
        h.succeedWhen(() -> {
            check(logged(c.getTank(), "log.bettacraft.aquarist.fed"), "fed the tank (now at " + npc.blockPosition() + ", job " + npc.currentJob() + ")");
            check(b.health().daysSinceFed() < 0.5, "the betta ate");
            check(npc.jobsDone >= 2, "fetched the food and went to the tank");
            cleanup(h, sp.getUUID());
        });
    }

    // ------------------------------------------------------------------ whose tanks

    /** Own and released tanks yes; foreign, locked, no-aquarist and ownerless tanks never (also when they need care). */
    @GameTest(template = EMPTY, batch = "bettacraft_m51_owners", timeoutTicks = 200)
    public static void worksOnlyAtOwnAndReleasedTanks(GameTestHelper h) {
        floor(h);
        home(h);
        ServerPlayer sp = player(h);
        UUID other = UUID.randomUUID();
        try {
            TankBlockEntity own = tank(h, 10, 2, sp.getUUID());
            TankBlockEntity foreign = tank(h, 15, 2, other);
            TankBlockEntity released = tank(h, 20, 2, other);
            released.getTank().setAquaristAccess(AquaristAccess.ALL);
            TankBlockEntity locked = tank(h, 10, 8, sp.getUUID());
            locked.getTank().setLocked(true);
            TankBlockEntity none = tank(h, 15, 8, sp.getUUID());
            none.getTank().setAquaristAccess(AquaristAccess.NONE);
            TankBlockEntity lockedReleased = tank(h, 20, 8, other);
            lockedReleased.getTank().setAquaristAccess(AquaristAccess.ALL);
            lockedReleased.getTank().setLocked(true);
            TankBlockEntity ownerless = tank(h, 10, 14, sp.getUUID());
            ownerless.getTank().setOwner(null);
            AquaristEntity npc = npc(h, sp);
            List<TankBlockEntity> all = List.of(own, foreign, released, locked, none, lockedReleased, ownerless);
            for (TankBlockEntity c : all) for (AlgaeSim.Face f : TankAlgae.wetFaces(c.getTank())) AlgaeSim.set(c.getTank().algae(), f.pos(), f.dir(), 0.9F);
            // (tests run side by side: only the tanks of this test count)
            BlockPos o = h.absolutePos(BlockPos.ZERO);
            var mine = AquaristWork.tanks(h.getLevel(), npc).stream().filter(t -> {
                BlockPos r = t.getBlockPos().subtract(o);
                return r.getX() >= 0 && r.getX() < 32 && r.getZ() >= 0 && r.getZ() < 32;
            }).map(t -> t.getTank().id()).toList();
            check(mine.size() == 2 && mine.contains(own.getTank().id()) && mine.contains(released.getTank().id()), "own and released: "
                    + AquaristWork.tanks(h.getLevel(), npc).stream().map(t -> t.getBlockPos().subtract(h.absolutePos(BlockPos.ZERO)) + " "
                    + AquaristRules.verdict(t.getTank(), sp.getUUID())).toList());
            check(AquaristRules.verdict(foreign.getTank(), sp.getUUID()) == AquaristRules.Verdict.FOREIGN, "foreign");
            check(AquaristRules.verdict(locked.getTank(), sp.getUUID()) == AquaristRules.Verdict.LOCKED, "locked");
            check(AquaristRules.verdict(none.getTank(), sp.getUUID()) == AquaristRules.Verdict.EXCLUDED, "none wanted");
            check(AquaristRules.verdict(lockedReleased.getTank(), sp.getUUID()) == AquaristRules.Verdict.LOCKED, "locked beats released");
            check(AquaristRules.verdict(ownerless.getTank(), sp.getUUID()) == AquaristRules.Verdict.NO_OWNER, "ownerless");
            // a full round: every job he plans
            for (AquaristWork.Job j : npc.planNow(h.getLevel())) AquaristWork.perform(h.getLevel(), npc, j);
            check(TankAlgae.mean(own.getTank()) < 0.01 && TankAlgae.mean(released.getTank()) < 0.01, "own and released cleaned");
            for (TankBlockEntity c : List.of(foreign, locked, none, lockedReleased, ownerless)) {
                check(TankAlgae.mean(c.getTank()) > 0.85 && c.getTank().log().stream().noneMatch(e -> e.key().startsWith("log.bettacraft.aquarist")),
                        "untouched: " + c.getBlockPos());
            }
            // a tank locked while he walks to it is left alone (the check right before the job)
            for (AlgaeSim.Face f : TankAlgae.wetFaces(own.getTank())) AlgaeSim.set(own.getTank().algae(), f.pos(), f.dir(), 0.9F);
            own.getTank().setLocked(true);
            run(h, npc, AquaristWork.Kind.TEND, own);
            check(TankAlgae.mean(own.getTank()) > 0.85, "locked on the way: untouched");
            h.succeed();
        } finally {
            cleanup(h, sp.getUUID(), other);
        }
    }

    /** M66: the list of the menu shows what he cares for: without a workplace nothing (and "cared" says 0), with one every tank around it. */
    @GameTest(template = EMPTY, batch = "bettacraft_m66_list", timeoutTicks = 200)
    public static void menuListMatchesTheTanksHeCaresFor(GameTestHelper h) {
        floor(h);
        home(h);
        ServerPlayer sp = player(h);
        try {
            TankBlockEntity own = tank(h, 10, 2, sp.getUUID());
            AquaristEntity npc = AquaristArrival.spawnHiredHomeless(h.getLevel(), sp.getUUID(), "tester", h.absoluteVec(new Vec3(6.5, 1, 6.5)));
            npc.alwaysDay = true;
            npc.setNoAi(true);
            var none = org.overengineer.bettacraft.aquarist.AquaristMenu.view(sp, npc);
            check(none.getIntOr("cared", 0) == 0, "no workplace: cares for none");
            check(none.getListOrEmpty("tanks").isEmpty(), "no workplace: no tank in the list (was: "
                    + none.getListOrEmpty("tanks").size() + ")");
            check(npc.setHome(h.getLevel(), h.absolutePos(BASIN), h.absolutePos(BED.south())), "home");
            var with = org.overengineer.bettacraft.aquarist.AquaristMenu.view(sp, npc);
            long mineRows = with.getListOrEmpty("tanks").stream().filter(t -> {
                BlockPos r = BlockPos.of(((net.minecraft.nbt.CompoundTag) t).getLongOr("pos", 0L)).subtract(h.absolutePos(BlockPos.ZERO));
                return r.getX() >= 0 && r.getX() < 32 && r.getZ() >= 0 && r.getZ() < 32;
            }).count();
            check(mineRows == 1 && own.getTank().id() != null, "the tank around his workplace is listed");
            h.succeed();
        } finally {
            cleanup(h, sp.getUUID());
        }
    }

    /** Team tanks count (FTB Teams through its answer); the config switch turns it off. */
    @GameTest(template = EMPTY, batch = "bettacraft_m51_team", timeoutTicks = 200)
    public static void teamTanksCount(GameTestHelper h) {
        floor(h);
        home(h);
        ServerPlayer sp = player(h);
        UUID mate = UUID.randomUUID();
        boolean teams = ServerConfig.AQUARIST_FTB_TEAMS.get();
        try {
            TankBlockEntity team = tank(h, 10, 4, mate);
            AquaristEntity npc = npc(h, sp);
            check(AquaristRules.verdict(team.getTank(), sp.getUUID()) == AquaristRules.Verdict.FOREIGN, "not in a team: foreign");
            FtbTeamsCompat.testOverride = (a, b) -> (a.equals(mate) && b.equals(sp.getUUID())) || (b.equals(mate) && a.equals(sp.getUUID()));
            check(AquaristRules.verdict(team.getTank(), sp.getUUID()) == AquaristRules.Verdict.TEAM, "the team's tank");
            check(AquaristWork.tanks(h.getLevel(), npc).contains(team), "he works there");
            ServerConfig.AQUARIST_FTB_TEAMS.set(false);
            check(AquaristRules.verdict(team.getTank(), sp.getUUID()) == AquaristRules.Verdict.FOREIGN, "the config off: foreign again");
            h.succeed();
        } finally {
            FtbTeamsCompat.testOverride = null;
            ServerConfig.AQUARIST_FTB_TEAMS.set(teams);
            cleanup(h, sp.getUUID(), mate);
        }
    }

    /**
     * With FTB Teams loaded (-Pquests): two players in a real party, the tank of the one counts for the aquarist of the
     * other; without the mod the test only notes that it was skipped.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m51_ftbteams", timeoutTicks = 200)
    public static void teamTanksWithFtbTeams(GameTestHelper h) {
        if (!FtbTeamsCompat.loaded()) {
            org.slf4j.LoggerFactory.getLogger("bettacraft/gametest").info("FTB Teams not loaded: real team test skipped");
            h.succeed();
            return;
        }
        floor(h);
        home(h);
        ServerPlayer a = MockPlayers.serverPlayer(h, true), b = MockPlayers.serverPlayer(h, true);
        Object party = null;
        try {
            Class<?> impl = Class.forName("dev.ftb.mods.ftbteams.data.TeamManagerImpl");
            Object mgr = impl.getField("INSTANCE").get(null);
            check(mgr != null, "the FTB Teams manager");
            var login = impl.getMethod("playerLoggedIn", ServerPlayer.class, UUID.class, String.class);
            login.invoke(mgr, a, a.getUUID(), "aquarist-a");
            login.invoke(mgr, b, b.getUUID(), "aquarist-b");
            check(!FtbTeamsCompat.sameTeam(a.getUUID(), b.getUUID()), "two players alone are no team");
            party = impl.getMethod("createParty", ServerPlayer.class, String.class).invoke(mgr, a, "bettacraft-m51");
            Class<?> pt = Class.forName("dev.ftb.mods.ftbteams.data.PartyTeam");
            try {
                pt.getMethod("invite", ServerPlayer.class, java.util.Collection.class).invoke(party, a, List.of(b.getGameProfile()));
            } catch (java.lang.reflect.InvocationTargetException ignored) {
                // the invitation message may not reach a mock player; joining works on the invitation list
            }
            pt.getMethod("join", ServerPlayer.class).invoke(party, b);
            check(FtbTeamsCompat.sameTeam(a.getUUID(), b.getUUID()), "a party of two");
            TankBlockEntity bs = tank(h, 10, 4, b.getUUID());
            AquaristEntity npc = npc(h, a);
            check(AquaristRules.verdict(bs.getTank(), a.getUUID()) == AquaristRules.Verdict.TEAM, "the mate's tank is a team tank");
            check(AquaristWork.tanks(h.getLevel(), npc).contains(bs), "his aquarist works there");
            h.succeed();
        } catch (ReflectiveOperationException e) {
            throw new GameTestFailure("FTB Teams: " + (e.getCause() != null ? e.getCause() : e));
        } finally {
            if (party != null) {
                try {
                    party.getClass().getMethod("forceDisband", net.minecraft.commands.CommandSourceStack.class).invoke(party, h.getLevel().getServer().createCommandSourceStack());
                } catch (ReflectiveOperationException ignored) {
                    // the team file of the test world is deleted with it
                }
            }
            cleanup(h, a.getUUID(), b.getUUID());
        }
    }

    // ------------------------------------------------------------------ wage

    /** Paid in emeralds, else in food; without a wage he works slower and leaves after three days. */
    @GameTest(template = EMPTY, batch = "bettacraft_m51_wage", timeoutTicks = 200)
    public static void withoutWageHeLeaves(GameTestHelper h) {
        floor(h);
        home(h);
        ServerPlayer sp = player(h);
        boolean wage = ServerConfig.AQUARIST_WAGE.get();
        try {
            ServerConfig.AQUARIST_WAGE.set(true);
            AquaristEntity npc = npc(h, sp);
            ServerLevel level = h.getLevel();
            put(chest(h), new ItemStack(Items.EMERALD, 2));
            put(chest(h), new ItemStack(Items.BREAD, 3));
            npc.payDay(level, 100); // the first day is free
            check(count(chest(h), Items.EMERALD) == 2, "first day free");
            npc.payDay(level, 101);
            check(count(chest(h), Items.EMERALD) == 1 && npc.unpaidDays() == 0, "an emerald");
            npc.payDay(level, 101);
            check(count(chest(h), Items.EMERALD) == 1, "once a day");
            npc.payDay(level, 102);
            npc.payDay(level, 103);
            check(count(chest(h), Items.EMERALD) == 0 && count(chest(h), Items.BREAD) == 0 && npc.unpaidDays() == 0, "the emerald, then the bread");
            npc.payDay(level, 104);
            check(npc.unpaidDays() == 1 && npc.workSpeed() < 1.0, "unpaid: slower");
            npc.payDay(level, 105);
            check(npc.phase() == AquaristEntity.Phase.HIRED, "still there after two days");
            npc.payDay(level, 106);
            check(npc.phase() == AquaristEntity.Phase.LEAVING, "gone after three days");
            AquaristRecords.PlayerRecord r = AquaristRecords.get(level.getServer()).player(sp.getUUID());
            check(r.npc == null && r.left == 1 && r.notBefore > level.getServer().overworld().getGameTime(), "he left: the next one later");
            check(!level.getPoiManager().existsAtPosition(org.overengineer.bettacraft.villager.ModVillagers.AQUARIST_POI_KEY, h.absolutePos(BASIN))
                    || level.getPoiManager().getInSquare(t -> true, h.absolutePos(BASIN), 0, net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY)
                            .anyMatch(rec -> rec.getPos().equals(h.absolutePos(BASIN)) && rec.getFreeTickets() == 1), "the basin is free again");
            h.succeed();
        } finally {
            ServerConfig.AQUARIST_WAGE.set(wage);
            cleanup(h, sp.getUUID());
        }
    }

    /** Dismissed while he carries food, cuttings and young: everything goes back, nothing is lost or doubled. */
    @GameTest(template = EMPTY, batch = "bettacraft_m51_dismiss", timeoutTicks = 200)
    public static void dismissedHePutsEverythingBack(GameTestHelper h) {
        floor(h);
        home(h);
        ServerPlayer sp = player(h);
        try {
            TankBlockEntity from = tank(h, 10, 4, sp.getUUID()), rear = tank(h, 10, 12, sp.getUUID());
            BettaEntity young = betta(h, from, 5107);
            young.setBettaData(young.getBettaData().withStage(LifeStage.JUVENILE));
            AquaristEntity npc = npc(h, sp);
            npc.setRearingTank(rear.getTank().id());
            put(chest(h), new ItemStack(ModItems.BETTA_PELLETS.get(), 12));
            run(h, npc, AquaristWork.Kind.FETCH_FOOD, null);
            run(h, npc, AquaristWork.Kind.TAKE_YOUNG, from);
            check(npc.carriedAnimals().size() == 1 && npc.hasFood(), "he carries food and a young betta");
            AquaristArrival.dismiss(sp, npc);
            check(npc.phase() == AquaristEntity.Phase.LEAVING, "he leaves");
            check(count(chest(h), ModItems.BETTA_PELLETS.get()) == 12, "all food back: " + count(chest(h), ModItems.BETTA_PELLETS.get()));
            check(!npc.hasCarriedItems() && npc.carriedAnimals().isEmpty(), "carries nothing");
            check(TankInhabitants.liveInhabitants(h.getLevel(), from.getTank()).size() == 1, "the young betta is back in its tank");
            check(h.getLevel().getEntities(ModEntities.BETTA.get(), new net.minecraft.world.phys.AABB(h.absolutePos(BlockPos.ZERO)).inflate(40), e -> true).size() == 1,
                    "exactly one betta");
            check(count(chest(h), ModItems.FISH_BAG.get()) == 0, "no fish bag made");
            h.succeed();
        } finally {
            cleanup(h, sp.getUUID());
        }
    }

    // ------------------------------------------------------------------ chunk unload

    /** His chunk unloads while he carries young and food: he comes back with them; a tank in an unloaded chunk is not his to touch. */
    @GameTest(template = EMPTY, batch = "bettacraft_m51_chunk", timeoutTicks = 3000)
    public static void survivesHisChunkUnloading(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        home(h);
        ServerPlayer sp = player(h);
        TankBlockEntity from = tank(h, 10, 4, sp.getUUID()), rear = tank(h, 10, 12, sp.getUUID());
        BettaEntity young = betta(h, from, 5108);
        young.setBettaData(young.getBettaData().withStage(LifeStage.JUVENILE));
        AquaristEntity npc = npc(h, sp);
        npc.setRearingTank(rear.getTank().id());
        put(chest(h), new ItemStack(ModItems.BETTA_PELLETS.get(), 6));
        run(h, npc, AquaristWork.Kind.FETCH_FOOD, null);
        run(h, npc, AquaristWork.Kind.TAKE_YOUNG, from);
        check(npc.carriedAnimals().size() == 1 && npc.hasFood(), "carries");
        int food = count(npc.bag(), ModItems.BETTA_PELLETS.get());
        UUID id = npc.getUUID();
        // he walks off to a place far away (the chunks of the GameTest structures are forced and never unload)
        BlockPos far = h.absolutePos(BlockPos.ZERO).offset(3200, 0, 0);
        ChunkPos cp = ChunkPos.containing(far);
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) level.setChunkForced(cp.x() + x, cp.z() + z, true);
        Runnable release = () -> {
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) level.setChunkForced(cp.x() + x, cp.z() + z, false);
        };
        await(h, 400, () -> level.isPositionEntityTicking(far), "the far chunks load", () -> {
            level.setBlock(far.below(), Blocks.STONE.defaultBlockState(), 3);
            TankBlockEntity farTank = tankAt(h, far.offset(3, 0, 0), sp.getUUID());
            UUID farId = farTank.getTank().id();
            npc.teleportTo(far.getX() + 0.5, far.getY(), far.getZ() + 0.5);
            await(h, 100, () -> level.getEntity(id) != null && ChunkPos.containing(level.getEntity(id).blockPosition()).equals(cp), "he is there", () -> {
                for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) level.setChunkForced(cp.x() + x, cp.z() + z, false);
                await(h, 1500, () -> level.getEntity(id) == null && !level.hasChunk(cp.x(), cp.z()), "his chunk unloads", () -> {
                    check(AquaristWork.tank(level, npc, farId) == null, "a tank in an unloaded chunk is not his to touch");
                    AquaristRecords.PlayerRecord r0 = AquaristRecords.get(level.getServer()).player(sp.getUUID());
                    check(id.equals(r0.npc) && r0.hired, "unloaded he stays hired");
                    for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) level.setChunkForced(cp.x() + x, cp.z() + z, true);
                    await(h, 600, () -> level.getEntity(id) instanceof AquaristEntity, "he comes back", () -> {
                        AquaristEntity back = (AquaristEntity) level.getEntity(id);
                        check(back.carriedAnimals().size() == 1 && count(back.bag(), ModItems.BETTA_PELLETS.get()) == food, "with the young and the food");
                        check(back.phase() == AquaristEntity.Phase.HIRED && back.workplace() != null && back.bed() != null, "still hired, with his home");
                        AquaristRecords.PlayerRecord r = AquaristRecords.get(level.getServer()).player(sp.getUUID());
                        check(id.equals(r.npc) && r.hired, "the record still has him");
                        AquaristWork.perform(level, back, new AquaristWork.Job(AquaristWork.Kind.RELEASE_YOUNG, rear.getTank().id()));
                        check(back.carriedAnimals().isEmpty() && TankInhabitants.liveInhabitants(level, rear.getTank()).size() == 1, "the young betta in the rearing tank");
                        back.discard();
                        for (int a = 0; a < 3; a++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) level.setBlock(far.offset(3 + a, y, z), Blocks.AIR.defaultBlockState(), 3);
                        release.run();
                        cleanup(h, sp.getUUID());
                        h.succeed();
                    });
                });
            });
        });
    }

    /** A 3x2x2 tank at an absolute position (far from the test), full, owned, in the index. */
    static TankBlockEntity tankAt(GameTestHelper h, BlockPos min, UUID owner) {
        ServerLevel level = h.getLevel();
        List<BlockPos> blocks = new java.util.ArrayList<>();
        for (int a = 0; a < 3; a++) for (int b = 0; b < 2; b++) for (int c = 0; c < 2; c++) blocks.add(min.offset(a, b, c));
        for (BlockPos p : blocks) level.setBlock(p, ModBlocks.TANK.get().defaultBlockState(), 3);
        TankMultiblock.formAll(level, blocks);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        if (c == null) throw new GameTestFailure("no far tank");
        TankInteractions.changeWater(level, c, c.getTank().capacityMb());
        c.getTank().setWaterTemp(26F);
        c.getTank().setOwner(owner);
        AquaristRecords.get(level.getServer()).noteTank(level, c);
        return c;
    }

    /** A betta put into a tank, found by the inhabitant list (far from the test the box lookup may miss it the first tick). */
    static BettaEntity bettaById(GameTestHelper h, TankBlockEntity c, long seed) {
        var d = org.overengineer.bettacraft.entity.betta.BettaData.create(org.overengineer.bettacraft.genetics.BettaGenotype.randomDomestic(
                net.minecraft.util.RandomSource.create(seed)), false, 0);
        java.util.Set<UUID> before = new java.util.HashSet<>(c.getTank().inhabitants());
        var err = TankInhabitants.insert(h.getLevel(), c, org.overengineer.bettacraft.entity.betta.BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d),
                ModEntities.BETTA.get(), null, false);
        if (err != null) throw new GameTestFailure("insert failed: " + err.getString());
        for (UUID u : c.getTank().inhabitants()) {
            if (!before.contains(u) && h.getLevel().getEntity(u) instanceof BettaEntity b) return b;
        }
        throw new GameTestFailure("inserted betta not found (far)");
    }

    private static void await(GameTestHelper h, int max, java.util.function.BooleanSupplier done, String what, Runnable then) {
        if (done.getAsBoolean()) {
            then.run();
            return;
        }
        if (max <= 0) throw new GameTestFailure("timed out: " + what);
        h.runAfterDelay(1, () -> await(h, max - 1, done, what, then));
    }

    // ------------------------------------------------------------------ the switch, protection

    /** {@code features.aquaristNpc.enabled} off: none comes, a hired one does not work, the tank menu refuses the setting. */
    @GameTest(template = EMPTY, batch = "bettacraft_m51_off", timeoutTicks = 200)
    public static void switchedOff(GameTestHelper h) {
        floor(h);
        home(h);
        ServerPlayer sp = player(h);
        boolean on = Features.own(Feature.AQUARIST_NPC);
        try {
            for (int i = 0; i < 3; i++) tank(h, 10, 2 + i * 5, sp.getUUID());
            Progress.award(sp, Progress.SPAWNED);
            AquaristEntity npc = npc(h, sp);
            AquaristRecords.get(h.getLevel().getServer()).reset(sp.getUUID());
            npc.discard();
            check(AquaristArrival.whyNot(sp, true) == null, "on: he may come");
            Features.set(Feature.AQUARIST_NPC, false);
            check("off".equals(AquaristArrival.whyNot(sp, true)), "off: none comes");
            AquaristEntity hired = npc(h, sp);
            check(!hired.canWork(), "a hired one does not work");
            h.succeed();
        } finally {
            Features.set(Feature.AQUARIST_NPC, on);
            cleanup(h, sp.getUUID());
        }
    }

    /** He cannot be hurt, pushed, leashed or caught in a bag; a stranger cannot use him; the tank data keep his marks. */
    @GameTest(template = EMPTY, batch = "bettacraft_m51_protect", timeoutTicks = 200)
    public static void cannotBeHurtOrCaught(GameTestHelper h) {
        floor(h);
        home(h);
        ServerPlayer sp = player(h);
        try {
            AquaristEntity npc = npc(h, sp);
            float hp = npc.getHealth();
            var zombie = h.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, new BlockPos(20, 1, 20));
            npc.hurt(h.getLevel().damageSources().mobAttack(zombie), 10F);
            npc.hurt(h.getLevel().damageSources().lava(), 10F);
            check(npc.getHealth() == hp && npc.isAlive(), "unhurt");
            zombie.discard();
            check(!npc.isPushable() && !npc.canBeLeashed(), "not pushed, not leashed");
            check(!TankInhabitants.bagTakes(npc), "no fish bag takes him");
            check(TankInhabitants.accepts(npc.getType(), npc) != TankInhabitants.Acceptance.OK, "no tank takes him");
            ServerPlayer stranger = MockPlayers.serverPlayer(h);
            stranger.snapTo(npc.position().add(1, 0, 0));
            var r = npc.interact(stranger, net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.phys.Vec3.ZERO);
            check(r == net.minecraft.world.InteractionResult.CONSUME, "a stranger only gets told whose he is: " + r);
            h.getLevel().getServer().getPlayerList().remove(stranger);
            // the marks on the tank survive saving; an older tank has none
            TankBlockEntity c = tank(h, 10, 4, sp.getUUID());
            TankData copy = TankData.load(c.getTank().save()); // a copy: a released tank here would be work for the aquarists of other tests
            copy.setAquaristAccess(AquaristAccess.ALL);
            copy.setAquaristDone("feed", 1234L);
            TankData back = TankData.load(copy.save());
            check(back.aquaristAccess() == AquaristAccess.ALL && back.aquaristDone("feed") == 1234L, "saved and loaded");
            var tag = c.getTank().save();
            tag.remove("aquarist");
            check(TankData.load(tag).aquaristAccess() == AquaristAccess.OWNER, "an old tank: the owner's aquarist");
            npc.kill((net.minecraft.server.level.ServerLevel) npc.level());
            check(AquaristRecords.get(h.getLevel().getServer()).player(sp.getUUID()).npc == null, "/kill: he is gone for the records");
            h.succeed();
        } finally {
            cleanup(h, sp.getUUID());
        }
    }
}
