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
            org.overengineer.bettacraft.util.NbtCompat.putUUID(inner, "rearing", a.getTank().id());
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
                if (org.overengineer.bettacraft.util.NbtCompat.getUUID(c, "id").equals(a.getTank().id())) {
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
            check(AquaristAssign.assign(level, sp, npc, h.absolutePos(new BlockPos(8, 1, 14)), false) != null, "stone is no chest");
            h.setBlock(new BlockPos(8, 1, 14), Blocks.STONE);
            check(AquaristAssign.assign(level, sp, npc, h.absolutePos(new BlockPos(8, 1, 14)), false) != null, "a block without inventory is refused");
            h.setBlock(new BlockPos(30, 1, 30), Blocks.CHEST);
            check(AquaristAssign.assign(level, sp, npc, h.absolutePos(new BlockPos(30, 1, 30)), false) != null, "too far from the basin is refused");
            check(npc.chest() == null, "nothing changed");
            check(AquaristAssign.assign(level, sp, npc, h.absolutePos(own), false) == null, "the barrel is his chest now");
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
            check(AquaristAssign.assign(level, sp, npc, h.absolutePos(bed2), true) == null, "the second bed is his now");
            check(h.absolutePos(bed2.south()).equals(npc.bed()), "his bed is the head of the second: " + npc.bed());
            check(level.getPoiManager().getFreeTickets(oldBed) > 0, "the first bed is free again");
            // somebody else's bed is refused
            check(AquaristAssign.assign(level, sp, npc, h.absolutePos(new BlockPos(14, 1, 14)), true) != null, "no bed there");
            npc.setChest(null);
            check(npc.chest() == null && AquaristWork.chests(level, npc).size() == 1, "auto: back to the basin's chest");
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
}
