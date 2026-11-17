package org.overengineer.bettacraft.equipment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * M21.7: which blocks a stone cabinet can be made of and the slab that belongs to each.
 * <ul>
 *     <li>A stone is a full block item in {@code #bettacraft:cabinet_stones} (stones, cobblestones, stone bricks, sandstone,
 *     the deepslate, blackstone and tuff families, bricks, nether bricks, quartz, terracotta; modpacks can add to it) or,
 *     for blocks of other mods, one that is mined with a pickaxe, sounds like stone and is no storage block ({@code #c:storage_blocks}).
 *     Never: {@code #bettacraft:cabinet_excluded} (copper, ores, glass, purpur, end stone, prismarine, concrete, mud bricks),
 *     planks, blocks with a block entity and blocks that are not full cubes.</li>
 *     <li>The slab of a stone is found through the game's own recipes, so it works with every mod: the crafting recipe
 *     "three blocks in a row -> slabs" first, else a stonecutter recipe block -> slab whose name fits the block
 *     ({@code stone_bricks -> stone_brick_slab}; the stonecutter also turns stone into stone brick slabs, that one does
 *     not count). Without a slab the stone cannot be made into a cabinet.</li>
 * </ul>
 * The slab table is built once per recipe manager (cleared on every reload of recipes or tags).
 */
public final class CabinetStones {
    /** A stone and its slab. */
    public record Pair(Item stone, Item slab) {
        public Identifier stoneId() {
            return BuiltInRegistries.ITEM.getKey(stone);
        }
    }

    /** The vanilla stones with slabs (recipe JSON, creative tab, handbook, showcase; a GameTest checks them against the recipes). */
    public static final List<Pair> VANILLA = List.of(
            new Pair(Items.STONE, Items.STONE_SLAB), new Pair(Items.COBBLESTONE, Items.COBBLESTONE_SLAB),
            new Pair(Items.MOSSY_COBBLESTONE, Items.MOSSY_COBBLESTONE_SLAB), new Pair(Items.SMOOTH_STONE, Items.SMOOTH_STONE_SLAB),
            new Pair(Items.STONE_BRICKS, Items.STONE_BRICK_SLAB), new Pair(Items.MOSSY_STONE_BRICKS, Items.MOSSY_STONE_BRICK_SLAB),
            new Pair(Items.GRANITE, Items.GRANITE_SLAB), new Pair(Items.POLISHED_GRANITE, Items.POLISHED_GRANITE_SLAB),
            new Pair(Items.DIORITE, Items.DIORITE_SLAB), new Pair(Items.POLISHED_DIORITE, Items.POLISHED_DIORITE_SLAB),
            new Pair(Items.ANDESITE, Items.ANDESITE_SLAB), new Pair(Items.POLISHED_ANDESITE, Items.POLISHED_ANDESITE_SLAB),
            new Pair(Items.COBBLED_DEEPSLATE, Items.COBBLED_DEEPSLATE_SLAB), new Pair(Items.POLISHED_DEEPSLATE, Items.POLISHED_DEEPSLATE_SLAB),
            new Pair(Items.DEEPSLATE_BRICKS, Items.DEEPSLATE_BRICK_SLAB), new Pair(Items.DEEPSLATE_TILES, Items.DEEPSLATE_TILE_SLAB),
            new Pair(Items.TUFF, Items.TUFF_SLAB), new Pair(Items.POLISHED_TUFF, Items.POLISHED_TUFF_SLAB),
            new Pair(Items.TUFF_BRICKS, Items.TUFF_BRICK_SLAB), new Pair(Items.BLACKSTONE, Items.BLACKSTONE_SLAB),
            new Pair(Items.POLISHED_BLACKSTONE, Items.POLISHED_BLACKSTONE_SLAB), new Pair(Items.POLISHED_BLACKSTONE_BRICKS, Items.POLISHED_BLACKSTONE_BRICK_SLAB),
            new Pair(Items.SANDSTONE, Items.SANDSTONE_SLAB), new Pair(Items.CUT_SANDSTONE, Items.CUT_STANDSTONE_SLAB),
            new Pair(Items.SMOOTH_SANDSTONE, Items.SMOOTH_SANDSTONE_SLAB), new Pair(Items.RED_SANDSTONE, Items.RED_SANDSTONE_SLAB),
            new Pair(Items.CUT_RED_SANDSTONE, Items.CUT_RED_SANDSTONE_SLAB), new Pair(Items.SMOOTH_RED_SANDSTONE, Items.SMOOTH_RED_SANDSTONE_SLAB),
            new Pair(Items.BRICKS, Items.BRICK_SLAB), new Pair(Items.NETHER_BRICKS, Items.NETHER_BRICK_SLAB),
            new Pair(Items.RED_NETHER_BRICKS, Items.RED_NETHER_BRICK_SLAB), new Pair(Items.QUARTZ_BLOCK, Items.QUARTZ_SLAB),
            new Pair(Items.SMOOTH_QUARTZ, Items.SMOOTH_QUARTZ_SLAB));

    /** The selection of the creative tab and the handbook. */
    public static final List<Item> SELECTION = List.of(Items.STONE, Items.COBBLESTONE, Items.STONE_BRICKS, Items.MOSSY_STONE_BRICKS, Items.SMOOTH_STONE,
            Items.POLISHED_GRANITE, Items.POLISHED_DIORITE, Items.POLISHED_ANDESITE, Items.DEEPSLATE_BRICKS, Items.DEEPSLATE_TILES,
            Items.POLISHED_BLACKSTONE_BRICKS, Items.TUFF_BRICKS, Items.SANDSTONE, Items.RED_SANDSTONE, Items.BRICKS, Items.NETHER_BRICKS,
            Items.QUARTZ_BLOCK);

    /** Sounds of stone-like blocks (the check for blocks of other mods that are not in the tag). */
    private static final Set<SoundType> STONE_SOUNDS = Set.of(SoundType.STONE, SoundType.DEEPSLATE, SoundType.DEEPSLATE_BRICKS, SoundType.DEEPSLATE_TILES,
            SoundType.POLISHED_DEEPSLATE, SoundType.TUFF, SoundType.TUFF_BRICKS, SoundType.POLISHED_TUFF, SoundType.CALCITE, SoundType.NETHER_BRICKS,
            SoundType.BASALT, SoundType.DRIPSTONE_BLOCK, SoundType.NETHERRACK);
    /** Names of blocks of other mods that are never stone here, even if they sound like it (concrete, metal, ores ...). */
    private static final Pattern MODDED_EXCLUDED = Pattern.compile(
            "concrete|copper|purpur|prismarine|glass|mud_brick|end_stone|obsidian|(^|_)ores?($|_)|iron|gold|steel|metal|brass|bronze");

    private static final Map<RecipeManager, Map<Item, Item>> SLABS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<RecipeManager, List<Pair>> ALL = Collections.synchronizedMap(new WeakHashMap<>());

    private CabinetStones() {}

    /** Forget the slab tables (recipes or tags reloaded). */
    public static void clear() {
        SLABS.clear();
        ALL.clear();
    }

    /** Whether {@code stack} may be the stone of a cabinet (the slab is checked separately, {@link #slabFor}). */
    public static boolean isStone(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem bi)) return false;
        if (stack.is(ModTags.CABINET_EXCLUDED) || stack.is(ItemTags.PLANKS) || stack.is(ItemTags.SLABS)) return false;
        Block b = bi.getBlock();
        if (b instanceof EntityBlock || !fullCube(b.defaultBlockState())) return false;
        if (stack.is(ModTags.CABINET_STONES)) return true;
        // metal and other storage blocks (not in the exclusion tag: mods put the quartz block into #c:storage_blocks)
        if (stack.is(org.overengineer.bettacraft.platform.Tags.Items.STORAGE_BLOCKS)) return false;
        Identifier id = BuiltInRegistries.BLOCK.getKey(b);
        if (id.getNamespace().equals("minecraft")) return false; // vanilla: only what the tag says
        BlockState s = b.defaultBlockState();
        return s.is(BlockTags.MINEABLE_WITH_PICKAXE) && STONE_SOUNDS.contains(s.getSoundType()) && !MODDED_EXCLUDED.matcher(id.getPath()).find();
    }

    public static boolean isStone(Item item) {
        return isStone(new ItemStack(item));
    }

    private static boolean fullCube(BlockState s) {
        try {
            return Block.isShapeFullBlock(s.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO))
                    && Block.isShapeFullBlock(s.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Whether a placed cabinet of this material behaves like stone (pickaxe, hardness, blast resistance and sound of the
     * stone). Planks and anything that is not mined with a pickaxe stay wood, so wooden cabinets are unchanged.
     */
    public static boolean isStoneMaterial(Block material) {
        BlockState s = material.defaultBlockState();
        return !s.is(BlockTags.PLANKS) && s.is(BlockTags.MINEABLE_WITH_PICKAXE);
    }

    /** The stone a cabinet in the world is made of, or null for wood (and for anything that is not a cabinet). */
    @Nullable
    public static BlockState stoneOf(BlockGetter level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof CabinetBlockEntity be)) return null;
        BlockState m = be.woodState();
        return isStoneMaterial(m.getBlock()) ? m : null;
    }

    /** The slab of the same stone, or null if the recipes have none. */
    @Nullable
    public static Item slabFor(RecipeManager recipes, HolderLookup.Provider registries, Item stone) {
        return SLABS.computeIfAbsent(recipes, rm -> build(rm, registries)).get(stone);
    }

    /** Every stone with a slab, vanilla first, then by id (JEI). */
    public static List<Pair> all(RecipeManager recipes, HolderLookup.Provider registries) {
        // M52b: a client has no recipes; it got the pairs from the server (RecipeSync)
        if (recipes == null) return org.overengineer.bettacraft.crafting.RecipeSync.client().stones();
        return ALL.computeIfAbsent(recipes, rm -> {
            List<Pair> out = new ArrayList<>();
            for (var e : SLABS.computeIfAbsent(rm, k -> build(k, registries)).entrySet()) {
                if (isStone(e.getKey())) out.add(new Pair(e.getKey(), e.getValue()));
            }
            out.sort(Comparator.comparing((Pair p) -> !p.stoneId().getNamespace().equals("minecraft")).thenComparing(p -> p.stoneId().toString()));
            return List.copyOf(out);
        });
    }

    private record Candidate(Item slab, boolean crafting, int score) {}

    private static Map<Item, Item> build(RecipeManager rm, HolderLookup.Provider registries) {
        Map<Item, List<Candidate>> cands = new HashMap<>();
        for (RecipeHolder<CraftingRecipe> h : org.overengineer.bettacraft.crafting.RecipeCompat.byType(rm, RecipeType.CRAFTING)) {
            if (!(h.value() instanceof ShapedRecipe r) || r instanceof CabinetRecipe || r instanceof StoneCabinetRecipe) continue;
            if (r.getWidth() != 3 || r.getHeight() != 1) continue;
            // a shaped recipe's assemble is its result (M52b: getResultItem is gone)
            Item slab = slabItem(r.assemble(net.minecraft.world.item.crafting.CraftingInput.EMPTY, registries));
            if (slab == null) continue;
            var ing = r.getIngredients();
            if (ing.size() != 3 || ing.get(0).isEmpty() || ing.get(1).isEmpty() || ing.get(2).isEmpty()) continue;
            for (ItemStack s : org.overengineer.bettacraft.crafting.RecipeCompat.stacks(ing.get(0))) {
                if (ing.get(1).get().test(s) && ing.get(2).get().test(s)) add(cands, s.getItem(), slab, true);
            }
        }
        for (RecipeHolder<StonecutterRecipe> h : org.overengineer.bettacraft.crafting.RecipeCompat.byType(rm, RecipeType.STONECUTTING)) {
            Item slab = slabItem(h.value().assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(ItemStack.EMPTY), registries));
            if (slab == null) continue;
            Ingredient in = h.value().input();
            for (ItemStack s : org.overengineer.bettacraft.crafting.RecipeCompat.stacks(in)) add(cands, s.getItem(), slab, false);
        }
        Map<Item, Item> out = new HashMap<>();
        for (var e : cands.entrySet()) {
            Candidate best = null;
            for (Candidate c : e.getValue()) {
                // the stonecutter makes the slabs of other stones too (stone -> stone brick slab): only one with a fitting name
                if (!c.crafting && c.score == 0) continue;
                if (best == null || c.score > best.score || c.score == best.score && c.crafting && !best.crafting
                        || c.score == best.score && c.crafting == best.crafting
                        && BuiltInRegistries.ITEM.getKey(c.slab).toString().compareTo(BuiltInRegistries.ITEM.getKey(best.slab).toString()) < 0) {
                    best = c;
                }
            }
            if (best != null) out.put(e.getKey(), best.slab);
        }
        return out;
    }

    private static void add(Map<Item, List<Candidate>> cands, Item block, Item slab, boolean crafting) {
        if (!(block instanceof BlockItem) || block == slab) return;
        cands.computeIfAbsent(block, k -> new ArrayList<>()).add(new Candidate(slab, crafting, score(block, slab)));
    }

    @Nullable
    private static Item slabItem(ItemStack result) {
        return result != null && result.getItem() instanceof BlockItem bi && bi.getBlock() instanceof SlabBlock ? result.getItem() : null;
    }

    /** How well the slab's name fits the block's: 3 the same stone, 2 a plural or "_block" form, 1 a part of it, 0 not at all. */
    static int score(Item block, Item slab) {
        return score(BuiltInRegistries.ITEM.getKey(block).getPath(), BuiltInRegistries.ITEM.getKey(slab).getPath());
    }

    static int score(String block, String slab) {
        String base = slab.endsWith("_slab") ? slab.substring(0, slab.length() - "_slab".length()) : slab;
        if (base.equals(block)) return 3;
        if ((base + "s").equals(block) || (base + "_block").equals(block) || (base + "_blocks").equals(block)) return 2;
        if (block.endsWith(base) || block.startsWith(base)) return 1;
        return 0;
    }
}
