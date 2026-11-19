package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.CabinetBlockItem;
import org.overengineer.bettacraft.equipment.CabinetRecipe;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity;
import org.overengineer.bettacraft.equipment.FeederBlockEntity;
import org.overengineer.bettacraft.equipment.FilterHeaters;
import org.overengineer.bettacraft.equipment.FilterHost;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;

/** M4 GameTests: external filter, heaters in filters, migration of the old heater slots, cabinet recipe and placement. */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private EquipmentGameTests() {}

    private static TankBlockEntity tank(GameTestHelper h, int x, int y, int z, int sx, int sy, int sz, long water) {
        for (int a = 0; a < sx; a++)
            for (int b = 0; b < sy; b++)
                for (int c = 0; c < sz; c++) {
                    BlockPos p = new BlockPos(x + a, y + b, z + c);
                    h.setBlock(p, ModBlocks.TANK.get().defaultBlockState());
                    TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(p), null);
                }
        TankBlockEntity c = TankMultiblock.getController(h.getLevel(), h.absolutePos(new BlockPos(x, y, z)));
        if (c == null) throw new GameTestFailure("tank did not form");
        if (water > 0) TankInteractions.changeWater(h.getLevel(), c, water);
        return c;
    }

    /** Places an external filter like a player clicking the tank side (state from {@link ExternalFilterBlock#getStateForPlacement} rules). */
    private static ExternalFilterBlockEntity filter(GameTestHelper h, BlockPos rel, Direction towardsTank) {
        h.setBlock(rel, ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, towardsTank));
        if (!(h.getLevel().getBlockEntity(h.absolutePos(rel)) instanceof ExternalFilterBlockEntity f)) throw new GameTestFailure("no filter BE");
        return f;
    }

    private static int countHeaters(TankData t) {
        int n = 0;
        if (t.hasLegacyHeaters()) for (int i = 0; i < t.legacyHeaters().getContainerSize(); i++) n += t.legacyHeaters().getItem(i).getCount();
        return n;
    }

    @GameTest(template = EMPTY)
    public static void externalFilterAttachDetach(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 4, 1, 4, 2, 2, 2, 8000);
        // west of the tank, facing east (towards it)
        ExternalFilterBlockEntity f = filter(h, new BlockPos(3, 1, 4), Direction.EAST);
        // a filter without a tank next to it is not attached
        filter(h, new BlockPos(3, 1, 7), Direction.NORTH);
        TankEquipment.invalidate(c);
        List<FilterHost> attached = TankEquipment.filters(level, c);
        h.assertTrue(attached.size() == 1 && attached.getFirst() == f, "exactly the facing filter is attached, got " + attached.size());
        h.assertTrue(f.insertHeater(new ItemStack(ModItems.HEATER_MEDIUM.get())), "heater goes in");
        h.assertTrue(TankCare.filterHeaterBlocks(level, c.getTank()) == 32, "32 blocks of heating through the filter");
        // a filter placed against the side of the tank turns towards it on its own
        BlockState placed = ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, Direction.SOUTH);
        h.setBlock(new BlockPos(6, 2, 5), Block.updateFromNeighbourShapes(placed, level, h.absolutePos(new BlockPos(6, 2, 5))));
        h.assertTrue(h.getBlockState(new BlockPos(6, 2, 5)).getValue(ExternalFilterBlock.FACING) == Direction.WEST, "filter turns to the tank");
        TankEquipment.invalidate(c);
        h.assertTrue(TankEquipment.filters(level, c).size() == 2, "second filter attached");
        // detach: breaking the filter drops it with its heater inside
        level.destroyBlock(h.absolutePos(new BlockPos(3, 1, 4)), true);
        TankEquipment.invalidate(c);
        h.assertTrue(TankEquipment.filters(level, c).size() == 1 && TankCare.filterHeaterBlocks(level, c.getTank()) == 0, "filter detached");
        h.runAfterDelay(2, () -> {
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(new BlockPos(3, 1, 4))).inflate(1.5));
            ItemEntity drop = drops.stream().filter(e -> e.getItem().is(ModItems.EXTERNAL_FILTER.get())).findFirst()
                    .orElseThrow(() -> new GameTestFailure("filter item dropped"));
            h.assertTrue(FilterHeaters.capacityOfItem(drop.getItem()) == 32, "the dropped filter keeps its heater");
            // removing the tank block next to the remaining filter detaches it as well
            TankBlockEntity c2 = TankMultiblock.getController(level, h.absolutePos(new BlockPos(4, 1, 4)));
            h.assertTrue(c2 != null, "tank still there");
            h.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void heaterInFilterHeats(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 4, 1, 4, 2, 2, 2, 8000);
        TankData t = c.getTank();
        float ambient = TankCare.ambient(level, t);
        t.setTargetTemp(30.0F);
        t.setWaterTemp(5.0F);
        ExternalFilterBlockEntity f = filter(h, new BlockPos(6, 1, 4), Direction.WEST);
        TankEquipment.invalidate(c);
        TankCare.updateTemperature(level, c, 10.0);
        h.assertTrue(Math.abs(t.waterTemp() - ambient) < 0.05, "empty filter does not heat: " + t.waterTemp());
        f.insertHeater(new ItemStack(ModItems.HEATER_SMALL.get())); // 16 blocks >= 8 water blocks
        TankCare.updateTemperature(level, c, 10.0);
        float expected = Math.max(ambient, 30.0F);
        h.assertTrue(Math.abs(t.waterTemp() - expected) < 0.05, "heater in the filter reaches the target, got " + t.waterTemp());
        h.assertTrue(TankCare.heaterBlocks(level, t) == 16, "heating capacity counted once: " + TankCare.heaterBlocks(level, t));
        h.succeed();
    }

    /** A controller saved by M3 (schema v2) with heaters in the old menu slots: nothing is lost, they move into a filter. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void oldHeaterSlotsMoveIntoFilter(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 4, 1, 4, 2, 2, 2, 8000);
        CompoundTag tag = c.saveWithFullMetadata(level.registryAccess());
        // turn it into what M3 wrote: schema 2, "heaters" instead of "legacy_heaters"
        tag.putInt("schema", 2);
        CompoundTag tank = tag.getCompoundOrEmpty("tank");
        tank.remove("legacy_heaters");
        ListTag old = new ListTag();
        for (String id : new String[]{"bettacraft:heater_large", "", "bettacraft:heater_small", "bettacraft:heater_medium"}) old.add(StringTag.valueOf(id));
        tank.put("heaters", old);
        c.loadWithComponents(org.overengineer.bettacraft.util.NbtCompat.input(level.registryAccess(), tag));
        TankData t = c.getTank();
        h.assertTrue(countHeaters(t) == 3, "three old heaters survive the migration, got " + countHeaters(t));
        h.assertTrue(TankCare.slotHeaterBlocks(t) == 112, "old heaters keep heating until moved: " + TankCare.slotHeaterBlocks(t));
        double before = TankCare.heaterBlocks(level, t);
        // a filter with two free slots: two heaters move, one stays
        ExternalFilterBlockEntity f1 = filter(h, new BlockPos(3, 1, 4), Direction.EAST);
        TankEquipment.invalidate(c);
        h.runAfterDelay(45, () -> {
            h.assertTrue(f1.heaterCount() == 2 && countHeaters(t) == 1, "two moved into the filter, one left: " + f1.heaterCount() + "/" + countHeaters(t));
            h.assertTrue(TankCare.heaterBlocks(level, t) == before, "total heating unchanged");
            // second filter takes the rest
            ExternalFilterBlockEntity f2 = filter(h, new BlockPos(6, 1, 5), Direction.WEST);
            TankEquipment.invalidate(c);
            h.runAfterDelay(45, () -> {
                h.assertTrue(countHeaters(t) == 0 && f1.heaterCount() + f2.heaterCount() == 3, "all three heaters in filters");
                h.assertTrue(TankCare.heaterBlocks(level, t) == before, "total heating unchanged after the move");
                h.assertTrue(t.log().stream().anyMatch(e -> e.key().equals("log.bettacraft.heaters_moved")), "logged");
                // saved again: schema 3, no old key
                CompoundTag saved = c.saveWithFullMetadata(level.registryAccess());
                h.assertTrue(saved.getIntOr("schema", 0) == org.overengineer.bettacraft.data.schema.Schemas.TANK_BLOCK_ENTITY.currentVersion() && !saved.getCompoundOrEmpty("tank").contains("heaters"), "saved as the current version");
                h.succeed();
            });
        });
    }

    /** Rebinds #minecraft:planks with one extra item (simulates planks of another mod), runs the check, restores the tag. */
    private static void withExtraPlanks(Item extra, Runnable check) {
        var reg = BuiltInRegistries.ITEM;
        Map<TagKey<Item>, List<Holder<Item>>> original = new HashMap<>();
        reg.getTags().forEach(p -> original.put(p.key(), p.stream().toList()));
        Map<TagKey<Item>, List<Holder<Item>>> changed = new HashMap<>(original);
        List<Holder<Item>> planks = new ArrayList<>(original.getOrDefault(ItemTags.PLANKS, List.of()));
        planks.add(reg.wrapAsHolder(extra));
        changed.put(ItemTags.PLANKS, planks);
        reg.prepareTagReload(new net.minecraft.tags.TagLoader.LoadResult<>(net.minecraft.core.registries.Registries.ITEM, changed)).apply();
        try {
            check.run();
        } finally {
            reg.prepareTagReload(new net.minecraft.tags.TagLoader.LoadResult<>(net.minecraft.core.registries.Registries.ITEM, original)).apply();
        }
    }

    private static CraftingInput grid(Item slab, Item planks, Item chest) {
        return CraftingInput.of(3, 3, List.of(new ItemStack(slab), new ItemStack(slab), new ItemStack(slab),
                new ItemStack(planks), new ItemStack(chest), new ItemStack(planks),
                new ItemStack(planks), new ItemStack(planks), new ItemStack(planks)));
    }

    @GameTest(template = EMPTY)
    public static void cabinetRecipeAcceptsAnyPlanksFromTag(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var rm = level.recipeAccess();
        // vanilla but non-tree wood
        var crimson = rm.getRecipeFor(RecipeType.CRAFTING, grid(Items.CRIMSON_SLAB, Items.CRIMSON_PLANKS, Items.CHEST), level)
                .orElseThrow(() -> new GameTestFailure("crimson cabinet recipe"));
        h.assertTrue(crimson.value() instanceof CabinetRecipe, "our recipe");
        ItemStack r = crimson.value().assemble(grid(Items.CRIMSON_SLAB, Items.CRIMSON_PLANKS, Items.CHEST));
        h.assertTrue(r.is(ModItems.CABINET.get()) && CabinetBlockItem.getWood(r).toString().equals("minecraft:crimson_planks"), "crimson wood: " + r);
        // slabs of another wood are fine, mixed planks are not
        h.assertTrue(rm.getRecipeFor(RecipeType.CRAFTING, grid(Items.OAK_SLAB, Items.BAMBOO_PLANKS, Items.CHEST), level).isPresent(), "any slabs");
        CraftingInput mixed = CraftingInput.of(3, 3, List.of(new ItemStack(Items.OAK_SLAB), new ItemStack(Items.OAK_SLAB), new ItemStack(Items.OAK_SLAB),
                new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.CHEST), new ItemStack(Items.BIRCH_PLANKS),
                new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.OAK_PLANKS)));
        h.assertTrue(CabinetRecipe.findWood(mixed) == null, "mixed planks do not match");
        // planks that only the tag knows about (like those of another mod): mushroom stem stands in for them
        h.assertTrue(CabinetRecipe.findWood(grid(Items.OAK_SLAB, Items.MUSHROOM_STEM, Items.CHEST)) == null, "not planks without the tag");
        withExtraPlanks(Items.MUSHROOM_STEM, () -> {
            CraftingInput in = grid(Items.SPRUCE_SLAB, Items.MUSHROOM_STEM, Items.CHEST);
            var modded = rm.getRecipeFor(RecipeType.CRAFTING, in, level).orElseThrow(() -> new GameTestFailure("tag planks recipe"));
            ItemStack out = modded.value().assemble(in);
            h.assertTrue(CabinetBlockItem.getWood(out).toString().equals("minecraft:mushroom_stem"), "wood from the tag entry: " + out);
        });
        h.assertTrue(CabinetRecipe.findWood(grid(Items.OAK_SLAB, Items.MUSHROOM_STEM, Items.CHEST)) == null, "tag restored");
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 120)
    public static void cabinetsJoinCarryTankFilterAndFood(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos a = new BlockPos(4, 1, 4), b = new BlockPos(5, 1, 4);
        BlockState s = ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH);
        for (BlockPos p : List.of(a, b)) h.setBlock(p, s);
        for (BlockPos p : List.of(a, b)) h.setBlock(p, Block.updateFromNeighbourShapes(h.getBlockState(p), level, h.absolutePos(p)));
        // facing north: left (for the viewer in front) is east
        h.assertTrue(h.getBlockState(a).getValue(CabinetBlock.LEFT) && !h.getBlockState(a).getValue(CabinetBlock.RIGHT), "west cabinet joins east");
        h.assertTrue(h.getBlockState(b).getValue(CabinetBlock.RIGHT) && !h.getBlockState(b).getValue(CabinetBlock.LEFT), "east cabinet joins west");
        CabinetBlockEntity ca = (CabinetBlockEntity) level.getBlockEntity(h.absolutePos(a));
        CabinetBlockEntity cb = (CabinetBlockEntity) level.getBlockEntity(h.absolutePos(b));
        ca.setWood(BuiltInRegistries.BLOCK.getKey(net.minecraft.world.level.block.Blocks.CHERRY_PLANKS));
        h.assertTrue(ca.woodState().is(net.minecraft.world.level.block.Blocks.CHERRY_PLANKS), "cherry cabinet");
        // a cabinet turned the other way does not join
        BlockPos d = new BlockPos(6, 1, 4);
        h.setBlock(d, ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.SOUTH));
        h.setBlock(d, Block.updateFromNeighbourShapes(h.getBlockState(d), level, h.absolutePos(d)));
        h.assertTrue(!h.getBlockState(d).getValue(CabinetBlock.LEFT) && !h.getBlockState(d).getValue(CabinetBlock.RIGHT), "other facing stays apart");

        // a 2x1x1 tank on top of the two cabinets
        TankBlockEntity c = tank(h, 4, 2, 4, 2, 1, 1, 2000);
        TankEquipment.invalidate(c);
        h.assertTrue(TankEquipment.cabinets(level, c).size() == 2, "two cabinets under the tank");
        h.assertTrue(TankEquipment.filters(level, c).isEmpty(), "no filter yet");
        // a filter with a large heater in the cabinet slot
        ItemStack filter = new ItemStack(ModItems.EXTERNAL_FILTER.get());
        FilterHeaters.write(filter, List.of(new ItemStack(ModItems.HEATER_LARGE.get()), ItemStack.EMPTY));
        cb.items().setStackInSlot(CabinetBlockEntity.FILTER_SLOT, filter);
        h.assertTrue(TankEquipment.filters(level, c).size() == 1 && TankCare.filterHeaterBlocks(level, c.getTank()) == 64, "cabinet filter heats");
        h.assertTrue(cb.insertHeater(new ItemStack(ModItems.HEATER_SMALL.get())) && !cb.insertHeater(new ItemStack(ModItems.HEATER_SMALL.get())),
                "second heater slot, then full");
        h.assertTrue(TankCare.filterHeaterBlocks(level, c.getTank()) == 80, "both heaters count");
        // the feeder on the tank takes food from the cabinet storage
        ca.storage().insertItem(0, new ItemStack(ModItems.BETTA_PELLETS.get(), 5), false);
        h.setBlock(new BlockPos(4, 3, 4), ModBlocks.FEEDER.get().defaultBlockState());
        FeederBlockEntity feeder = (FeederBlockEntity) level.getBlockEntity(h.absolutePos(new BlockPos(4, 3, 4)));
        var data = org.overengineer.bettacraft.entity.betta.BettaData.create(org.overengineer.bettacraft.genetics.BettaGenotype.randomDomestic(
                net.minecraft.util.RandomSource.create(3)), false, level.getGameTime());
        ItemStack bucket = org.overengineer.bettacraft.entity.betta.BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), data);
        var err = TankInhabitants.insert(level, c, bucket, ModEntities.BETTA.get(), null, false);
        h.assertTrue(err == null, "betta inserted: " + (err == null ? "" : err.getString()));
        int used = feeder.feedNow(level, c);
        h.assertTrue(used == 1 && ca.storage().getStackInSlot(0).getCount() == 4, "one pellet from the cabinet, used " + used);
        // breaking a cabinet drops its contents and keeps the wood on the item
        level.destroyBlock(h.absolutePos(a), true);
        h.runAfterDelay(2, () -> {
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(a)).inflate(1.5));
            h.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(ModItems.BETTA_PELLETS.get())), "storage dropped");
            h.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(ModItems.CABINET.get())
                    && CabinetBlockItem.getWood(e.getItem()).toString().equals("minecraft:cherry_planks")), "cherry cabinet item");
            h.assertTrue(!h.getBlockState(b).getValue(CabinetBlock.RIGHT), "remaining cabinet no longer joined");
            for (var e : TankInhabitants.liveInhabitants(level, c.getTank())) e.discard();
            c.getTank().inhabitants().clear();
            h.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void internalFilterOutletFacesPlayer(GameTestHelper h) {
        // the segment block keeps the outlet direction and rotates with structures
        BlockState s = ModBlocks.FILTER_SEGMENT.get().defaultBlockState()
                .setValue(org.overengineer.bettacraft.equipment.SegmentBlock.FACING, Direction.EAST);
        h.assertTrue(s.rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_90)
                .getValue(org.overengineer.bettacraft.equipment.SegmentBlock.FACING) == Direction.SOUTH, "rotates");
        h.succeed();
    }
}
