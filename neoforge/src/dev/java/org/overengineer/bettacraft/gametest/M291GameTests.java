package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.util.NbtCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.filter.FilterChamberBlockEntity;
import org.overengineer.bettacraft.filter.FilterPartBlock;
import org.overengineer.bettacraft.filter.FilterPumpHeadBlockEntity;
import org.overengineer.bettacraft.filter.MultiblockFilter;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankAeration;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankWater;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M29.1 part b: the air pump in the aquarium cabinet drives the air stones of the tank above (and old cabinets keep their
 * items when they get the new slot); a multiblock external filter directly under a tank forms, circulates the water and
 * cleans the tank above it.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class M291GameTests {
    private static final String EMPTY = "empty_32x10x32";

    private M291GameTests() {}

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void airPumpInTheCabinetDrivesTheAirStones(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos cp = new BlockPos(3, 1, 3);
        h.setBlock(cp, ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH));
        TankBlockEntity c = AerationGameTests.tank(h, 2, 2, 3, 3, 2, 2);
        CabinetBlockEntity cab = (CabinetBlockEntity) level.getBlockEntity(h.absolutePos(cp));
        InteriorRegion r = AerationGameTests.region(h, c);
        BlockPos s1 = AerationGameTests.put(h, r, 4, 0, 4, ModBlocks.AIR_STONE.get().defaultBlockState());
        BlockPos s2 = AerationGameTests.put(h, r, 20, 0, 10, ModBlocks.AIR_STONE.get().defaultBlockState());
        TankEquipment.invalidate(c);
        TankAeration.step(level, c, 0.05);
        h.assertTrue(!AerationGameTests.active(h, s1) && TankEquipment.airPumps(level, c).isEmpty(), "an empty cabinet drives nothing");
        // only an air pump goes into the slot
        h.assertTrue(!cab.items().isItemValid(CabinetBlockEntity.AIR_PUMP_SLOT, new ItemStack(ModItems.EXTERNAL_FILTER.get())), "no filter in the pump slot");
        h.assertTrue(cab.storage().getSlots() == CabinetBlockEntity.STORAGE, "hoppers still reach only the nine storage slots");
        cab.items().setStackInSlot(CabinetBlockEntity.AIR_PUMP_SLOT, new ItemStack(ModItems.AIR_PUMP.get()));
        TankAeration.step(level, c, 0.05);
        h.assertTrue(TankEquipment.airPumps(level, c).size() == 1, "the cabinet pump serves the tank");
        h.assertTrue(AerationGameTests.active(h, s1) && AerationGameTests.active(h, s2), "the air pump in the cabinet drives both stones");
        h.assertTrue(cab.airRunning() && cab.airDriven() == 2, "it runs and drives 2: " + cab.airDriven());
        h.assertTrue(c.airDriven == 2, "the tank counts 2 driven outlets: " + c.airDriven);
        h.assertTrue(Math.abs(TankAeration.airDemand(level, c) - 2) < 1e-6, "its FE demand counts the stones: " + TankAeration.airDemand(level, c));
        // the client learns about the pump (hose and hum)
        CompoundTag sync = cab.getUpdateTag(level.registryAccess());
        h.assertTrue(sync.getBooleanOr("has_air_pump", false) && sync.getIntOr("air_driven", 0) == 2, "synced: " + sync);
        // saved and loaded with it; a cabinet of an older version (ten slots) keeps its filter and storage
        CompoundTag saved = cab.saveWithoutMetadata(level.registryAccess());
        CompoundTag items = saved.getCompoundOrEmpty("items");
        h.assertTrue(items.getIntOr("Size", 0) == CabinetBlockEntity.SLOTS, "eleven slots now: " + items.getIntOr("Size", 0));
        CompoundTag old = saved.copy();
        CompoundTag oldItems = new CompoundTag();
        oldItems.putInt("Size", 10);
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        CompoundTag filter = (CompoundTag) NbtCompat.saveStack(level.registryAccess(), new ItemStack(ModItems.EXTERNAL_FILTER.get()));
        filter.putInt("Slot", 0);
        list.add(filter);
        CompoundTag gravel = (CompoundTag) NbtCompat.saveStack(level.registryAccess(), new ItemStack(net.minecraft.world.item.Items.GRAVEL, 12));
        gravel.putInt("Slot", 9);
        list.add(gravel);
        oldItems.put("Items", list);
        old.put("items", oldItems);
        old.remove("air_driven");
        BlockPos cp2 = new BlockPos(12, 1, 12);
        h.setBlock(cp2, ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH));
        CabinetBlockEntity cab2 = (CabinetBlockEntity) level.getBlockEntity(h.absolutePos(cp2));
        cab2.loadWithComponents(org.overengineer.bettacraft.util.NbtCompat.input(level.registryAccess(), old));
        h.assertTrue(cab2.items().getSlots() == CabinetBlockEntity.SLOTS, "the old cabinet gets the pump slot");
        h.assertTrue(cab2.filterItem().is(ModItems.EXTERNAL_FILTER.get()) && cab2.items().getStackInSlot(9).getCount() == 12,
                "and keeps its filter and storage");
        h.assertTrue(!cab2.hasAirPump(), "without a pump");
        // the pump out again: the stones stop
        cab.items().setStackInSlot(CabinetBlockEntity.AIR_PUMP_SLOT, ItemStack.EMPTY);
        TankAeration.step(level, c, 0.05);
        h.assertTrue(!AerationGameTests.active(h, s1) && !cab.airRunning() && TankEquipment.airPumps(level, c).isEmpty(), "pump taken out: still");
        h.succeed();
    }

    /**
     * A multiblock filter directly under a tank: two chambers on the floor, the pump head beside them, the inlet and the
     * outlet on top of the chambers facing up into the tank's floor. It forms, circulates the water without losing any and
     * cleans the tank above it; the tank above stays a tank (nothing of the filter is taken for a cabinet or the tank).
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void multiblockFilterUnderTheTank(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        MultiblockFilter.forget();
        // filter: chambers at y 2, connectors at y 3 (facing up), two more chambers at y 3 right under the tank floor
        h.setBlock(new BlockPos(2, 2, 2), ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
        h.setBlock(new BlockPos(3, 2, 2), ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
        h.setBlock(new BlockPos(4, 2, 2), ModBlocks.FILTER_PUMP_HEAD.get().defaultBlockState());
        h.setBlock(new BlockPos(2, 3, 2), ModBlocks.FILTER_INLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.UP));
        h.setBlock(new BlockPos(3, 3, 2), ModBlocks.FILTER_OUTLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.UP));
        h.setBlock(new BlockPos(2, 3, 3), ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
        h.setBlock(new BlockPos(2, 2, 3), ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
        // a player builds the tank on top: a tank block placed against the outlet's top lands there (before M29.1 the click
        // opened the filter screen and nothing was placed)
        net.minecraft.server.level.ServerPlayer sp = MockPlayers.serverPlayer(h);
        ItemStack tankItem = new ItemStack(ModItems.TANK.get());
        sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, tankItem);
        BlockPos outlet = h.absolutePos(new BlockPos(3, 3, 2));
        sp.gameMode.useItemOn(sp, level, tankItem, net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(outlet).add(0, 0.5, 0), Direction.UP, outlet, false));
        h.assertTrue(level.getBlockState(outlet.above()).is(ModBlocks.TANK.get()), "a tank block placed on the filter's outlet: " + level.getBlockState(outlet.above()));
        level.setBlockAndUpdate(outlet.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        TankBlockEntity c = AerationGameTests.tank(h, 1, 4, 1, 4, 2, 3);
        for (int i = 0; i < 4; i++) AerationGameTests.add(h, c, TankmateSpecies.PLATY, LifeStage.ADULT);
        ((FilterChamberBlockEntity) level.getBlockEntity(h.absolutePos(new BlockPos(2, 2, 2)))).setMedium(new ItemStack(ModItems.FILTER_FOAM.get()));
        ((FilterChamberBlockEntity) level.getBlockEntity(h.absolutePos(new BlockPos(3, 2, 2)))).setMedium(new ItemStack(ModItems.CERAMIC_RINGS.get()));
        FilterPumpHeadBlockEntity head = (FilterPumpHeadBlockEntity) level.getBlockEntity(h.absolutePos(new BlockPos(4, 2, 2)));
        FilterLinesGameTests.guardAll(level, head.getBlockPos()); // M40: the guards on the docks in the floor
        double without = TankWater.filterBlocks(level, c);
        long w = c.getTank().waterMb();
        int moved = head.run(level, true);
        h.assertTrue(head.problem() == MultiblockFilter.Problem.NONE && head.chambers() == 4 && head.inlets() == 1 && head.outlets() == 1,
                "formed under the tank: " + head.problem());
        h.assertTrue(moved > 0 && c.getTank().waterMb() == w, "it circulates the tank's water (none lost): " + moved);
        h.assertTrue(c.getBlockPos().equals(head.servedTank()), "it serves the tank above: " + head.servedTank());
        double with = TankWater.filterBlocks(level, c);
        h.assertTrue(Math.abs(with - without - 36) < 0.01, "sponge 12 + ceramic 24 water blocks: " + (with - without));
        h.assertTrue(c.getTank().contains(h.absolutePos(new BlockPos(2, 4, 2))) && !c.getTank().contains(h.absolutePos(new BlockPos(2, 3, 2))),
                "the tank stays where it is");
        h.succeed();
    }
}
