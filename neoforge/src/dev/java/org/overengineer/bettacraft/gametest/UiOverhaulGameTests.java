package org.overengineer.bettacraft.gametest;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.aquarist.AquaristAccess;
import org.overengineer.bettacraft.aquarist.AquaristAssign;
import org.overengineer.bettacraft.aquarist.AquaristEntity;
import org.overengineer.bettacraft.aquarist.AquaristMenu;
import org.overengineer.bettacraft.aquarist.AquaristTask;
import org.overengineer.bettacraft.aquarist.AquaristWork;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.equipment.ClipFeederBlockEntity;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity;
import org.overengineer.bettacraft.equipment.ExternalFilterMenu;
import org.overengineer.bettacraft.equipment.MenuEnergy;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankPower;
import org.overengineer.bettacraft.tank.menu.TankView;
import org.overengineer.bettacraft.terrarium.TankClimate;
import org.overengineer.bettacraft.terrarium.TankModeSetting;

import static org.overengineer.bettacraft.gametest.HiredAquaristGameTests.BASIN;
import static org.overengineer.bettacraft.gametest.HiredAquaristGameTests.BED;
import static org.overengineer.bettacraft.gametest.HiredAquaristGameTests.CHEST;

/**
 * M66 (the UI overhaul): the aquarist's several rearing tanks (in order, by species, the old single one migrated), what he does per
 * tank, the chest and the bed assigned in the menu; the clip-on feeder's two slots (and an old one-slot feeder loading); the energy a
 * filter menu shows; the hint "fill to x mB" that a terrarium does not get.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class UiOverhaulGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private UiOverhaulGameTests() {}

    private static void check(boolean ok, String what) {
        if (!ok) throw new GameTestAssertException(what);
    }

    // ------------------------------------------------------------------ the aquarist: rearing tanks

    /**
     * Several rearing tanks: the young go to the first that takes the animal. A rearing tank limited to other species is passed over; the
     * trip names its target tank; the young end up there. The list survives a save and a load, and an aquarist saved by an older
     * version (one "rearing" tank) keeps that tank as his rearing tank.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m66_rearing", timeoutTicks = 200)
    public static void severalRearingTanksTakeTheYoungInOrder(GameTestHelper h) {
        HiredAquaristGameTests.floor(h);
        HiredAquaristGameTests.home(h);
        ServerPlayer sp = HiredAquaristGameTests.player(h);
        try {
            TankBlockEntity from = HiredAquaristGameTests.tank(h, 10, 4, sp.getUUID()), a = HiredAquaristGameTests.tank(h, 10, 12, sp.getUUID()),
                    b = HiredAquaristGameTests.tank(h, 20, 12, sp.getUUID());
            HiredAquaristGameTests.betta(h, from, 6103);
            BettaEntity young = HiredAquaristGameTests.betta(h, from, 6104);
            young.setBettaData(young.getBettaData().withStage(LifeStage.JUVENILE));
            AquaristEntity npc = HiredAquaristGameTests.npc(h, sp);
            check(npc.addRearingTank(a.getTank().id()) && npc.addRearingTank(b.getTank().id()) && !npc.addRearingTank(b.getTank().id()), "two rearing tanks, each once");
            check(npc.rearingTank().equals(a.getTank().id()) && npc.rearingTanks().size() == 2, "in order, the first is the old rearing tank");
            var trip = npc.planNow(h.getLevel()).stream().filter(j -> j.kind() == AquaristWork.Kind.TAKE_YOUNG).findFirst();
            check(trip.isPresent() && a.getTank().id().equals(trip.get().to()), "the first takes it: " + trip);
            // the first takes only other species: the second gets the betta
            npc.toggleRearingSpecies(a.getTank().id(), "neon_tetra");
            check(!npc.rearingTanks().get(0).takes("betta") && npc.rearingTanks().get(1).takes("betta"), "species filter");
            trip = npc.planNow(h.getLevel()).stream().filter(j -> j.kind() == AquaristWork.Kind.TAKE_YOUNG).findFirst();
            check(trip.isPresent() && b.getTank().id().equals(trip.get().to()), "the second takes it: " + trip);
            AquaristWork.perform(h.getLevel(), npc, trip.get());
            check(npc.carriedAnimals().size() == 1 && b.getTank().id().equals(npc.carriedTo()), "he carries the young to the second");
            AquaristWork.perform(h.getLevel(), npc, new AquaristWork.Job(AquaristWork.Kind.RELEASE_YOUNG, b.getTank().id()));
            check(TankInhabitants.liveInhabitants(h.getLevel(), b.getTank()).size() == 1 && TankInhabitants.liveInhabitants(h.getLevel(), a.getTank()).isEmpty(), "in the second tank");
            // switching the species off again: every species
            npc.toggleRearingSpecies(a.getTank().id(), "neon_tetra");
            check(npc.rearingTanks().get(0).takes("betta"), "nothing ticked: all species");
            // save and load
            CompoundTag tag = new CompoundTag();
            npc.addAdditionalSaveData(tag);
            AquaristEntity back = ModEntities.HIRED_AQUARIST.get().create(h.getLevel());
            back.readAdditionalSaveData(tag);
            check(back.rearingTanks().size() == 2 && back.rearingTanks().get(1).tank().equals(b.getTank().id()), "the list survives a load");
            // an older save: one "rearing" tank
            CompoundTag old = tag.copy();
            CompoundTag inner = old.getCompoundOrEmpty("bettacraft_aquarist");
            inner.remove("rearing_tanks");
            inner.putUUID("rearing", a.getTank().id());
            AquaristEntity migrated = ModEntities.HIRED_AQUARIST.get().create(h.getLevel());
            migrated.readAdditionalSaveData(old);
            check(migrated.rearingTanks().size() == 1 && a.getTank().id().equals(migrated.rearingTank()), "the old rearing tank stays his rearing tank");
            // the menu lists them
            CompoundTag view = AquaristMenu.view(sp, npc);
            check(view.getListOrEmpty("rearing_tanks").size() == 2, "the menu lists both");
            h.succeed();
        } finally {
            HiredAquaristGameTests.cleanup(h, sp.getUUID());
        }
    }

    /** Per tank: a task switched off for a tank is not done there, the others are; the menu view tells which are off; it survives a load. */
    @GameTest(template = EMPTY, batch = "bettacraft_m66_pertank", timeoutTicks = 200)
    public static void tasksCanBeSwitchedOffPerTank(GameTestHelper h) {
        HiredAquaristGameTests.floor(h);
        HiredAquaristGameTests.home(h);
        ServerPlayer sp = HiredAquaristGameTests.player(h);
        try {
            TankBlockEntity a = HiredAquaristGameTests.tank(h, 10, 4, sp.getUUID()), b = HiredAquaristGameTests.tank(h, 10, 12, sp.getUUID());
            HiredAquaristGameTests.betta(h, a, 6201);
            HiredAquaristGameTests.betta(h, b, 6202);
            AquaristEntity npc = HiredAquaristGameTests.npc(h, sp);
            check(AquaristWork.feedDue(h.getLevel(), npc, a) && AquaristWork.feedDue(h.getLevel(), npc, b), "both are due");
            npc.setMayDo(a.getTank().id(), AquaristTask.FEED, false);
            check(!AquaristWork.feedDue(h.getLevel(), npc, a) && AquaristWork.feedDue(h.getLevel(), npc, b), "feeding is off at the first tank only");
            check(npc.mayDo(a.getTank().id(), AquaristTask.WATER) && !npc.mayDo(a.getTank().id(), AquaristTask.FEED), "the other tasks stay");
            npc.setMayDo(a.getTank().id(), AquaristTask.WARN, false);
            check(!AquaristWork.warnDue(h.getLevel(), npc, a), "no warnings from there");
            for (var j : npc.planNow(h.getLevel())) {
                check(!(j.kind() == AquaristWork.Kind.TEND && a.getTank().id().equals(j.tank())) || AquaristWork.algaeDue(npc, a) || AquaristWork.plantsDue(h.getLevel(), npc, a),
                        "no visit to the first tank for nothing");
            }
            boolean viewed = false;
            for (Tag t : AquaristMenu.view(sp, npc).getListOrEmpty("tanks")) {
                CompoundTag c = (CompoundTag) t;
                if (c.getUUID("id").equals(a.getTank().id())) {
                    viewed = true;
                    check(c.getIntOr("off", 0) == ((1 << AquaristTask.FEED.ordinal()) | (1 << AquaristTask.WARN.ordinal())), "the menu knows what is off: " + c.getIntOr("off", 0));
                }
            }
            check(viewed, "the tank is in the menu");
            CompoundTag tag = new CompoundTag();
            npc.addAdditionalSaveData(tag);
            AquaristEntity back = ModEntities.HIRED_AQUARIST.get().create(h.getLevel());
            back.readAdditionalSaveData(tag);
            check(!back.mayDo(a.getTank().id(), AquaristTask.FEED) && back.mayDo(b.getTank().id(), AquaristTask.FEED), "it survives a load");
            npc.setMayDo(a.getTank().id(), AquaristTask.FEED, true);
            check(AquaristWork.feedDue(h.getLevel(), npc, a), "switched on again");
            h.succeed();
        } finally {
            HiredAquaristGameTests.cleanup(h, sp.getUUID());
        }
    }

    /**
     * The chest and the bed assigned in the menu: the click on a chest within the home radius makes it his (his wage comes from it, not from
     * the chest next to the basin); a click on a block that is no container, or too far away, is refused with a reason; a second bed
     * replaces the first (its ticket is free again); "auto" gives the chest back.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m66_assign", timeoutTicks = 200)
    public static void chestAndBedCanBeAssigned(GameTestHelper h) {
        HiredAquaristGameTests.floor(h);
        HiredAquaristGameTests.home(h);
        ServerPlayer sp = HiredAquaristGameTests.player(h);
        ServerLevel level = h.getLevel();
        try {
            AquaristEntity npc = HiredAquaristGameTests.npc(h, sp);
            BlockPos own = new BlockPos(8, 1, 12);
            h.setBlock(own, Blocks.BARREL);
            check(npc.chest() == null && AquaristWork.chests(level, npc).size() == 1, "automatic: the chest next to the basin");
            check(AquaristAssign.assign(level, sp, npc, h.absolutePos(new BlockPos(8, 1, 14)), AquaristAssign.Kind.CHEST) != null, "stone is no chest");
            h.setBlock(new BlockPos(8, 1, 14), Blocks.STONE);
            check(AquaristAssign.assign(level, sp, npc, h.absolutePos(new BlockPos(8, 1, 14)), AquaristAssign.Kind.CHEST) != null, "a block without inventory is refused");
            h.setBlock(new BlockPos(30, 1, 30), Blocks.CHEST);
            check(AquaristAssign.assign(level, sp, npc, h.absolutePos(new BlockPos(30, 1, 30)), AquaristAssign.Kind.CHEST) != null, "too far from the basin is refused");
            check(npc.chest() == null, "nothing changed");
            check(AquaristAssign.assign(level, sp, npc, h.absolutePos(own), AquaristAssign.Kind.CHEST) == null, "the barrel is his chest now");
            check(h.absolutePos(own).equals(npc.chest()) && AquaristWork.chests(level, npc).size() == 1, "only the assigned one counts");
            // the wage comes from his chest (an emerald in the old chest does not count)
            var oldChest = HiredAquaristGameTests.chest(h);
            HiredAquaristGameTests.put(oldChest, new ItemStack(Items.EMERALD, 5));
            npc.payDay(level, 1);
            npc.payDay(level, 2);
            check(npc.unpaidDays() == 1, "no emerald in his own barrel: unpaid (" + npc.unpaidDays() + ")");
            HiredAquaristGameTests.put(AquaristWork.chests(level, npc).get(0), new ItemStack(Items.EMERALD, 5));
            npc.payDay(level, 3);
            check(npc.unpaidDays() == 0 && HiredAquaristGameTests.count(oldChest, Items.EMERALD) == 5, "paid from his barrel, the old chest untouched");
            // a second bed
            BlockPos bed2 = new BlockPos(12, 1, 20);
            var foot = Blocks.RED_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, Direction.SOUTH)
                    .setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT);
            h.setBlock(bed2, foot);
            h.setBlock(bed2.south(), foot.setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD));
            BlockPos oldBed = npc.bed();
            check(AquaristAssign.assign(level, sp, npc, h.absolutePos(bed2), AquaristAssign.Kind.BED) == null, "the second bed is his now");
            check(h.absolutePos(bed2.south()).equals(npc.bed()), "his bed is the head of the second: " + npc.bed());
            check(level.getPoiManager().getFreeTickets(oldBed) > 0, "the first bed is free again");
            // somebody else's bed is refused
            check(AquaristAssign.assign(level, sp, npc, h.absolutePos(new BlockPos(14, 1, 14)), AquaristAssign.Kind.BED) != null, "no bed there");
            npc.setChest(null);
            check(npc.chest() == null && AquaristWork.chests(level, npc).size() == 1, "auto: back to the basin's chest");
            h.succeed();
        } finally {
            HiredAquaristGameTests.cleanup(h, sp.getUUID());
        }
    }

    /**
     * The breeding basin assigned in the menu: a free basin becomes his workplace (the old one is free again); a basin somebody else
     * holds, and a block that is no basin, are refused and nothing changes; "auto" gives the basin back and takes a free one.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m66_assign", timeoutTicks = 200)
    public static void basinCanBeAssigned(GameTestHelper h) {
        HiredAquaristGameTests.floor(h);
        HiredAquaristGameTests.home(h);
        ServerPlayer sp = HiredAquaristGameTests.player(h);
        ServerLevel level = h.getLevel();
        try {
            AquaristEntity npc = HiredAquaristGameTests.npc(h, sp);
            BlockPos oldBasin = npc.workplace();
            BlockPos taken = new BlockPos(20, 1, 6), free = new BlockPos(20, 1, 12);
            h.setBlock(taken, org.overengineer.bettacraft.registry.ModBlocks.BREEDING_BASIN.get().defaultBlockState());
            h.setBlock(free, org.overengineer.bettacraft.registry.ModBlocks.BREEDING_BASIN.get().defaultBlockState());
            // somebody else holds the first one
            check(level.getPoiManager().take(hh -> hh.is(org.overengineer.bettacraft.villager.ModVillagers.AQUARIST_POI_KEY),
                    (hh, p) -> p.equals(h.absolutePos(taken)), h.absolutePos(taken), 1).isPresent(), "the other basin is taken by somebody");
            check(AquaristAssign.assign(level, sp, npc, h.absolutePos(taken), AquaristAssign.Kind.BASIN) != null, "an occupied basin is refused");
            check(oldBasin.equals(npc.workplace()) && level.getPoiManager().getFreeTickets(oldBasin) == 0, "nothing changed, he still holds his basin");
            check(AquaristAssign.assign(level, sp, npc, h.absolutePos(new BlockPos(8, 1, 14)), AquaristAssign.Kind.BASIN) != null, "no basin there");
            check(AquaristAssign.assign(level, sp, npc, h.absolutePos(free), AquaristAssign.Kind.BASIN) == null, "the free basin is his now");
            check(h.absolutePos(free).equals(npc.workplace()), "he works at the new basin: " + npc.workplace());
            check(level.getPoiManager().getFreeTickets(oldBasin) > 0, "the first basin is free again");
            check(level.getPoiManager().getFreeTickets(h.absolutePos(free)) == 0, "the new basin carries his ticket");
            // auto: the new basin back, another free one (the first)
            npc.autoBasin(level);
            check(oldBasin.equals(npc.workplace()), "auto: a free basin of his own search: " + npc.workplace());
            h.succeed();
        } finally {
            HiredAquaristGameTests.cleanup(h, sp.getUUID());
        }
    }

    // ------------------------------------------------------------------ the clip-on feeder

    /** A clip-on feeder saved with one slot (before 1.55) loads into two slots and keeps its food in the first. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void oldClipFeederLoadsIntoTwoSlots(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos at = new BlockPos(3, 1, 3);
        h.setBlock(new BlockPos(3, 1, 4), ModBlocks.TANK.get().defaultBlockState());
        h.setBlock(at, ModBlocks.CLIP_FEEDER.get().defaultBlockState());
        var be = (ClipFeederBlockEntity) level.getBlockEntity(h.absolutePos(at));
        check(be != null && be.items().getSlots() == 2, "two slots");
        be.items().setStackInSlot(0, new ItemStack(ModItems.BETTA_PELLETS.get(), 5));
        CompoundTag tag = be.saveWithoutMetadata(level.registryAccess());
        tag.getCompoundOrEmpty("items").putInt("Size", 1); // what the old version wrote
        tag.getCompoundOrEmpty("items").getListOrEmpty("Items");
        ClipFeederBlockEntity loaded = new ClipFeederBlockEntity(h.absolutePos(at), level.getBlockState(h.absolutePos(at)));
        loaded.loadWithComponents(tag, level.registryAccess());
        check(loaded.items().getSlots() == 2 && loaded.items().getStackInSlot(0).getCount() == 5, "the food stays in the first slot of two: " + loaded.items().getSlots());
        h.succeed();
    }

    // ------------------------------------------------------------------ energy in the filter menu

    /** The menu of a filter shows the energy of its tank's store, over the 16-bit data slots (a value above 65535 comes through whole). */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void filterMenuShowsTheStoreOfItsTank(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity t = PlumbingGameTests.tank(h, 5, 1, 5, 2, 2, 2);
        t.getTank().setPowerBuffer(123_456L);
        h.setBlock(new BlockPos(4, 1, 5), ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, Direction.EAST));
        var f = (ExternalFilterBlockEntity) level.getBlockEntity(h.absolutePos(new BlockPos(4, 1, 5)));
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        var menu = (ExternalFilterMenu) f.createMenu(9, sp.getInventory(), sp);
        MenuEnergy client = new MenuEnergy(null);
        DataSlot[] server = menu.energy().slots(), shown = client.slots();
        for (int i = 0; i < 4; i++) shown[i].set(server[i].get());
        check(client.stored() == 123_456L, "the stored energy: " + client.stored());
        check(client.capacity() == TankPower.capacity(), "the capacity: " + client.capacity());
        // not attached to a tank: nothing to show
        h.setBlock(new BlockPos(20, 1, 20), ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, Direction.EAST));
        var lone = (ExternalFilterMenu) ((ExternalFilterBlockEntity) level.getBlockEntity(h.absolutePos(new BlockPos(20, 1, 20)))).createMenu(10, sp.getInventory(), sp);
        MenuEnergy none = new MenuEnergy(null);
        for (int i = 0; i < 4; i++) none.slots()[i].set(lone.energy().slots()[i].get());
        check(none.capacity() == 0, "no tank: capacity 0");
        h.succeed();
    }

    // ------------------------------------------------------------------ the overview hint

    /** "Fill to x mB to add animals" is for a tank of water animals: a terrarium (and a paludarium) needs no water to take animals. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void terrariumOverviewHasNoFillHint(GameTestHelper h) {
        TankBlockEntity dry = CritterGameTests.tank(h, 2, 2, 2, 3, 3, 3, 0);
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        TankView aquarium = TankView.read(TankView.build(sp, dry));
        check(aquarium.waterMb < aquarium.minInsertMb && TankView.showFillHint(aquarium), "an empty aquarium asks for water");
        dry.getTank().setTankMode(TankModeSetting.TERRARIUM);
        dry.limitMb = -1;
        TankClimate.refreshMode(h.getLevel(), dry);
        TankView terrarium = TankView.read(TankView.build(sp, dry));
        check(terrarium.climate.hasLand() && !TankView.showFillHint(terrarium), "a terrarium does not: " + terrarium.climate.hasLand() + " " + terrarium.waterMb + " < " + terrarium.minInsertMb);
        h.succeed();
    }

    // ------------------------------------------------------------------ M66 part 2: the groups of the breeding tab, the motor filters by kind, the plant effects

    /**
     * The view of a tank has the breeding groups of the small animals and the frogs (conditions with their state), the motor filters by
     * kind with and without an intake sponge, and the effects of the plant groups (the floating plants' share is what the tank takes).
     */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void viewHasBreedingGroupsMotorFiltersAndPlantEffects(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = PlumbingGameTests.tank(h, 5, 1, 5, 3, 2, 2);
        c.getTank().setWaterQuality(0.8F);
        var shrimp = org.overengineer.bettacraft.critters.CritterSpecies.NEOCARIDINA;
        org.overengineer.bettacraft.critters.TankCritters.add(level, c, shrimp, org.overengineer.bettacraft.critters.CritterGenes.WILD, false, 40);
        org.overengineer.bettacraft.critters.TankCritters.add(level, c, shrimp, org.overengineer.bettacraft.critters.CritterGenes.WILD, true, 40);
        c.getTank().tadpoles().add(new org.overengineer.bettacraft.entity.frog.Tadpole(c.getTank().nextFrogBroodId(), 1, 1, 1));
        for (int i = 0; i < 12; i++) {
            org.overengineer.bettacraft.tank.plants.TankPlants.add(c, org.overengineer.bettacraft.tank.plants.FloatingPlantType.DUCKWEED,
                    c.getTank().min().getX() + 0.5 + i * 0.2, c.getTank().min().getZ() + 0.5, 0.8F, 0F);
        }
        h.setBlock(new BlockPos(4, 1, 5), ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, Direction.EAST));
        var f = (ExternalFilterBlockEntity) level.getBlockEntity(h.absolutePos(new BlockPos(4, 1, 5)));
        org.overengineer.bettacraft.tank.TankEquipment.invalidate(c);
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        TankView v = TankView.read(TankView.build(sp, c));
        var small = v.breeding.getListOrEmpty("g_small");
        check(small.size() == 1 && small.getCompound(0).getStringOr("n", "").equals(shrimp.translationKey()), "the shrimp group: " + small);
        var conds = small.getCompound(0).getListOrEmpty("cond");
        boolean pair = false, temp = false;
        for (int i = 0; i < conds.size(); i++) {
            var cd = conds.getCompound(i);
            if (cd.getStringOr("k", "").equals("pair")) pair = cd.getBooleanOr("ok", false);
            if (cd.getStringOr("k", "").equals("temp")) temp = cd.getBooleanOr("ok", false);
        }
        check(pair && temp, "a pair of shrimp at 26 C: " + conds);
        var frogs = v.breeding.getListOrEmpty("g_frogs");
        check(frogs.size() == 1 && !frogs.getCompound(0).getListOrEmpty("st").isEmpty(), "the frog group with its tadpoles: " + frogs);
        check(v.tech.motors[4] == 1 && v.tech.motors[5] == 0 && v.tech.unprotected >= 1, "one external filter without a sponge: " + java.util.Arrays.toString(v.tech.motors));
        f.setIntakeSponge(true);
        TankView v2 = TankView.read(TankView.build(sp, c));
        check(v2.tech.motors[4] == 1 && v2.tech.motors[5] == 1, "and with one: " + java.util.Arrays.toString(v2.tech.motors));
        check(v2.water.waterBlocks > 0, "the tank's water blocks are in the view: " + v2.water.waterBlocks);
        double floating = v2.aquatic.effects[0];
        check(Math.abs(floating - v2.plants.uptakePerDay) < 1e-9, "the floating plants' quality share is the tank's uptake: " + floating + " vs " + v2.plants.uptakePerDay);
        h.succeed();
    }

    /** An old tank without a breeding tag for the groups still reads (the new lists are optional). */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void viewReadsWithoutTheNewLists(GameTestHelper h) {
        TankBlockEntity c = PlumbingGameTests.tank(h, 5, 1, 5, 2, 2, 2);
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        CompoundTag t = TankView.build(sp, c);
        t.getCompoundOrEmpty("breeding").remove("g_small");
        t.getCompoundOrEmpty("tech").remove("motors");
        t.getCompoundOrEmpty("water_care").remove("water_blocks");
        t.getCompoundOrEmpty("aquatic").remove("fx");
        TankView v = TankView.read(t);
        check(v.tech.motors.length == 8 && v.aquatic.effects.length == 16, "defaults kept");
        h.succeed();
    }
}
