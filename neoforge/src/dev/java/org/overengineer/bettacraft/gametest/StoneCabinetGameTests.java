package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.CabinetBlockItem;
import org.overengineer.bettacraft.equipment.CabinetRecipe;
import org.overengineer.bettacraft.equipment.CabinetStones;
import org.overengineer.bettacraft.equipment.StoneCabinetRecipe;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M21.7 GameTests: the cabinet of stone. Every vanilla stone of the table finds its slab through the game's recipes and
 * crafts a cabinet of that stone; metals, copper, glass, purpur, prismarine, mud bricks, mixed stones and slabs of
 * another stone do not; a stone of another mod in the dev runtime works the same; the cabinet keeps its stone through
 * breaking and placing, needs a pickaxe and has the hardness, blast resistance and sound of the stone; wooden cabinets
 * stay as they were; with {@code features.equipment.stoneCabinets} off nothing can be crafted.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StoneCabinetGameTests {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/gametest");
    private static final String EMPTY = "empty_32x10x32";

    private StoneCabinetGameTests() {}

    static CraftingInput grid(Item slab, Item stone) {
        return grid(slab, stone, stone);
    }

    /** {@code SSS / P C P2 / PPP}: the right block in the middle row can differ (mixed stones). */
    static CraftingInput grid(Item slab, Item stone, Item other) {
        return CraftingInput.of(3, 3, List.of(new ItemStack(slab), new ItemStack(slab), new ItemStack(slab),
                new ItemStack(stone), new ItemStack(Items.CHEST), new ItemStack(other),
                new ItemStack(stone), new ItemStack(stone), new ItemStack(stone)));
    }

    private static ItemStack craft(ServerLevel level, CraftingInput in) {
        var r = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, in, level);
        return r.map(h -> h.value().assemble(in)).orElse(ItemStack.EMPTY);
    }

    private static Identifier blockId(Item item) {
        return BuiltInRegistries.BLOCK.getKey(((BlockItem) item).getBlock());
    }

    @GameTest(template = EMPTY)
    public static void stoneCabinetRecipeTakesEveryStoneWithItsSlab(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var rm = level.recipeAccess();
        // the slab of every vanilla stone of the table comes from the game's recipes
        for (CabinetStones.Pair p : CabinetStones.VANILLA) {
            Item slab = CabinetStones.slabFor(rm, level.registryAccess(), p.stone());
            h.assertTrue(slab == p.slab(), "slab of " + p.stoneId() + ": " + (slab == null ? "none" : BuiltInRegistries.ITEM.getKey(slab)));
            h.assertTrue(CabinetStones.isStone(p.stone()), "stone: " + p.stoneId());
        }
        // stone, cobblestone, stone bricks, deepslate, sandstone, bricks, nether bricks, quartz
        for (Item stone : List.of(Items.STONE, Items.COBBLESTONE, Items.STONE_BRICKS, Items.COBBLED_DEEPSLATE, Items.DEEPSLATE_TILES, Items.SANDSTONE,
                Items.RED_SANDSTONE, Items.BRICKS, Items.NETHER_BRICKS, Items.QUARTZ_BLOCK, Items.SMOOTH_QUARTZ, Items.POLISHED_BLACKSTONE_BRICKS)) {
            Item slab = CabinetStones.slabFor(rm, level.registryAccess(), stone);
            CraftingInput in = grid(slab, stone);
            RecipeHolder<CraftingRecipe> r = rm.getRecipeFor(RecipeType.CRAFTING, in, level).orElse(null);
            h.assertTrue(r != null && r.value() instanceof StoneCabinetRecipe, "stone cabinet recipe for " + blockId(stone));
            ItemStack out = r.value().assemble(in);
            h.assertTrue(out.is(ModItems.CABINET.get()) && CabinetBlockItem.getWood(out).equals(blockId(stone)), "cabinet of " + blockId(stone) + ": " + out);
        }
        // the stone of the cabinet carries the name of the stone
        ItemStack bricks = CabinetBlockItem.setWood(new ItemStack(ModItems.CABINET.get()), blockId(Items.STONE_BRICKS));
        h.assertTrue(bricks.getHoverName().getString().contains(Items.STONE_BRICKS.getDefaultInstance().getHoverName().getString())
                || bricks.getHoverName().getString().contains("Stone Bricks"), "named after the stone: " + bricks.getHoverName().getString());
        // not: metal (also copper with its own slabs), glass, purpur, prismarine, mud bricks, terracotta without slabs
        for (Item[] bad : new Item[][] {{Items.IRON_BLOCK, Items.STONE_SLAB}, {Items.CUT_COPPER, Items.CUT_COPPER_SLAB},
                {Items.WAXED_CUT_COPPER, Items.WAXED_CUT_COPPER_SLAB}, {Items.GOLD_BLOCK, Items.STONE_SLAB}, {Items.GLASS, Items.STONE_SLAB},
                {Items.PURPUR_BLOCK, Items.PURPUR_SLAB}, {Items.END_STONE_BRICKS, Items.END_STONE_BRICK_SLAB}, {Items.PRISMARINE, Items.PRISMARINE_SLAB},
                {Items.MUD_BRICKS, Items.MUD_BRICK_SLAB}, {Items.TERRACOTTA, Items.STONE_SLAB}, {Items.WHITE_CONCRETE, Items.STONE_SLAB},
                {Items.OAK_PLANKS, Items.STONE_SLAB}, {Items.COAL_ORE, Items.STONE_SLAB}, {Items.FURNACE, Items.STONE_SLAB}}) {
            h.assertTrue(craft(level, grid(bad[1], bad[0])).isEmpty(), "no cabinet of " + BuiltInRegistries.ITEM.getKey(bad[0]));
        }
        // the slabs must be of the same stone: stone brick slabs on stone (the stonecutter makes them from stone), stone slabs on bricks
        h.assertTrue(craft(level, grid(Items.STONE_BRICK_SLAB, Items.STONE)).isEmpty(), "stone with stone brick slabs");
        h.assertTrue(craft(level, grid(Items.STONE_SLAB, Items.STONE_BRICKS)).isEmpty(), "stone bricks with stone slabs");
        h.assertTrue(craft(level, grid(Items.OAK_SLAB, Items.COBBLESTONE)).isEmpty(), "cobblestone with oak slabs");
        // mixed stones and mixed slabs
        h.assertTrue(craft(level, grid(Items.STONE_SLAB, Items.STONE, Items.COBBLESTONE)).isEmpty(), "mixed stones");
        CraftingInput mixedSlabs = CraftingInput.of(3, 3, List.of(new ItemStack(Items.STONE_SLAB), new ItemStack(Items.COBBLESTONE_SLAB), new ItemStack(Items.STONE_SLAB),
                new ItemStack(Items.STONE), new ItemStack(Items.CHEST), new ItemStack(Items.STONE),
                new ItemStack(Items.STONE), new ItemStack(Items.STONE), new ItemStack(Items.STONE)));
        h.assertTrue(craft(level, mixedSlabs).isEmpty(), "mixed slabs");
        // stones are not planks: the wooden recipe never takes them, the stone recipe never takes planks
        h.assertTrue(CabinetRecipe.findWood(grid(Items.OAK_SLAB, Items.STONE)) == null, "the wooden recipe does not take stone");
        ItemStack oak = craft(level, grid(Items.OAK_SLAB, Items.OAK_PLANKS));
        h.assertTrue(CabinetBlockItem.getWood(oak).toString().equals("minecraft:oak_planks"), "wooden cabinets as before: " + oak);
        // a stone of another mod in the dev runtime (if any is loaded): found by its sound and pickaxe, slab from its recipes
        int modded = 0;
        for (CabinetStones.Pair p : CabinetStones.all(rm, level.registryAccess())) {
            if (p.stoneId().getNamespace().equals("minecraft")) {
                if (CabinetStones.VANILLA.stream().noneMatch(v -> v.stone() == p.stone())) {
                    LOGGER.info("[gametest] M21.7 vanilla cabinet stone outside the table: {} with {}", p.stoneId(), BuiltInRegistries.ITEM.getKey(p.slab()));
                }
                continue;
            }
            ItemStack out = craft(level, grid(p.slab(), p.stone()));
            h.assertTrue(CabinetBlockItem.getWood(out).equals(blockId(p.stone())), "cabinet of the modded stone " + p.stoneId() + ": " + out);
            if (modded++ < 8) LOGGER.info("[gametest] M21.7 modded cabinet stone {} with {}", p.stoneId(), BuiltInRegistries.ITEM.getKey(p.slab()));
        }
        LOGGER.info("[gametest] M21.7 cabinet stones: {} in all, {} of other mods", CabinetStones.all(rm, level.registryAccess()).size(), modded);
        h.succeed();
    }

    /** A stone cabinet keeps its stone through placing and breaking, needs a pickaxe and is as hard as its stone. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void stoneCabinetIsMinedLikeItsStone(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        // placed from an item of stone bricks: the block entity takes the stone
        BlockPos a = new BlockPos(4, 1, 4), b = new BlockPos(8, 1, 4), w = new BlockPos(12, 1, 4);
        ItemStack item = CabinetBlockItem.setWood(new ItemStack(ModItems.CABINET.get()), blockId(Items.STONE_BRICKS));
        sp.setItemInHand(InteractionHand.MAIN_HAND, item);
        h.setBlock(a.below(), Blocks.STONE.defaultBlockState());
        BlockPos below = h.absolutePos(a).below();
        var ctx = new BlockPlaceContext(level, sp, InteractionHand.MAIN_HAND, item, new BlockHitResult(Vec3.atCenterOf(below).add(0, 0.5, 0), Direction.UP, below, false));
        h.assertTrue(((BlockItem) ModItems.CABINET.get()).place(ctx).consumesAction(), "placed");
        h.assertTrue(level.getBlockEntity(h.absolutePos(a)) instanceof CabinetBlockEntity be && be.woodState().is(Blocks.STONE_BRICKS), "placed with its stone");
        // a second one by hand and a wooden one
        for (BlockPos p : List.of(b, w)) h.setBlock(p, ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH));
        ((CabinetBlockEntity) level.getBlockEntity(h.absolutePos(b))).setWood(blockId(Items.COBBLED_DEEPSLATE));
        // properties of the stone, wood unchanged
        var stoneState = h.getBlockState(a);
        float resist = GameTestCompat.blast(stoneState, level, h.absolutePos(a));
        h.assertTrue(Math.abs(resist - Blocks.STONE_BRICKS.getExplosionResistance()) < 1e-4, "blast resistance of stone bricks: " + resist);
        h.assertTrue(GameTestCompat.sound(stoneState, level, h.absolutePos(a), sp) == SoundType.STONE, "stone sound");
        h.assertTrue(GameTestCompat.sound(h.getBlockState(b), level, h.absolutePos(b), sp) == SoundType.DEEPSLATE, "deepslate sound");
        float woodResist = GameTestCompat.blast(h.getBlockState(w), level, h.absolutePos(w));
        h.assertTrue(Math.abs(woodResist - 2.5F) < 1e-4 && GameTestCompat.sound(h.getBlockState(w), level, h.absolutePos(w), sp) == SoundType.WOOD, "wooden cabinet unchanged");
        sp.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        sp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        float hand = stoneState.getDestroyProgress(sp, level, h.absolutePos(a));
        h.assertTrue(!GameTestCompat.harvest(stoneState, level, h.absolutePos(a), sp), "no drop by hand");
        float handWood = h.getBlockState(w).getDestroyProgress(sp, level, h.absolutePos(w));
        h.assertTrue(GameTestCompat.harvest(h.getBlockState(w), level, h.absolutePos(w), sp), "wood by hand as before");
        sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
        float pick = stoneState.getDestroyProgress(sp, level, h.absolutePos(a));
        float expect = Blocks.STONE_BRICKS.defaultBlockState().getDestroyProgress(sp, level, h.absolutePos(a));
        h.assertTrue(Math.abs(pick - expect) < 1e-6 && pick > hand * 5, "a pickaxe mines it like stone bricks: " + pick + " / " + expect + ", hand " + hand);
        h.assertTrue(GameTestCompat.harvest(stoneState, level, h.absolutePos(a), sp), "drops with a pickaxe");
        // contents and stone through breaking with the pickaxe
        ((CabinetBlockEntity) level.getBlockEntity(h.absolutePos(a))).storage().insertItem(0, new ItemStack(ModItems.BETTA_PELLETS.get(), 3), false);
        h.assertTrue(sp.gameMode.destroyBlock(h.absolutePos(a)), "broken");
        // by hand the second one is lost (like stone), its contents are not
        sp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        ((CabinetBlockEntity) level.getBlockEntity(h.absolutePos(b))).storage().insertItem(0, new ItemStack(ModItems.ALGAE_WAFER.get(), 2), false);
        h.assertTrue(sp.gameMode.destroyBlock(h.absolutePos(b)), "broken by hand");
        h.runAfterDelay(3, () -> {
            List<ItemEntity> da = level.getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(a)).inflate(1.5));
            h.assertTrue(da.stream().anyMatch(e -> e.getItem().is(ModItems.BETTA_PELLETS.get())), "contents dropped");
            ItemStack drop = da.stream().map(ItemEntity::getItem).filter(s -> s.is(ModItems.CABINET.get())).findFirst().orElse(ItemStack.EMPTY);
            h.assertTrue(CabinetBlockItem.getWood(drop).equals(blockId(Items.STONE_BRICKS)), "the item keeps its stone: " + drop);
            List<ItemEntity> db = level.getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(b)).inflate(1.5));
            h.assertTrue(db.stream().anyMatch(e -> e.getItem().is(ModItems.ALGAE_WAFER.get())), "contents dropped by hand too");
            h.assertTrue(db.stream().noneMatch(e -> e.getItem().is(ModItems.CABINET.get())), "no cabinet without a pickaxe");
            h.assertTrue(handWood > 0, "wood by hand: " + handWood);
            sp.discard();
            h.succeed();
        });
    }

    /** features.equipment.stoneCabinets off: no stone cabinets can be crafted, wooden ones still can. */
    @GameTest(template = EMPTY, batch = "bettacraft_stone_cabinet_config")
    public static void stoneCabinetsOffCraftsNone(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        boolean before = Features.enabled(Feature.STONE_CABINETS);
        try {
            Features.set(Feature.STONE_CABINETS, false);
            h.assertTrue(craft(level, grid(Items.STONE_BRICK_SLAB, Items.STONE_BRICKS)).isEmpty(), "no stone cabinet while off");
            h.assertTrue(!craft(level, grid(Items.SPRUCE_SLAB, Items.SPRUCE_PLANKS)).isEmpty(), "wooden cabinets still");
        } finally {
            Features.set(Feature.STONE_CABINETS, before);
        }
        h.assertTrue(!craft(level, grid(Items.STONE_BRICK_SLAB, Items.STONE_BRICKS)).isEmpty(), "on again");
        h.succeed();
    }
}
