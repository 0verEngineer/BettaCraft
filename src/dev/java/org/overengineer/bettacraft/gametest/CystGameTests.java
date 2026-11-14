package org.overengineer.bettacraft.gametest;

import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.food.ArtemiaCystsItem;
import org.overengineer.bettacraft.food.CultureTubBlockEntity;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;

/**
 * M21.8 GameTests: jars of artemia cysts stack (16 by default, 32 or 64 by config), keep their 16 uses each, only jars with
 * the same uses left stack; crafting and the hatchery take one use off a stack by splitting one jar off; old jars (the
 * vanilla damage component of the times they did not stack) keep their uses; the network codec keeps count and uses.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CystGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private CystGameTests() {}

    private static ItemStack jars(int n) {
        return new ItemStack(ModItems.ARTEMIA_CYSTS.get(), n);
    }

    /** Full jars stack to 16, a used jar stacks only with jars of the same uses, an old damaged jar keeps its uses. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void jarsStackWithTheSameUsesLeft(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ItemStack full = jars(1);
        h.assertTrue(full.getMaxStackSize() == 16, "16 per stack by default: " + full.getMaxStackSize());
        h.assertTrue(full.isStackable(), "stackable");
        h.assertTrue(ArtemiaCystsItem.usesLeft(full) == 16, "16 uses per jar");
        ItemStack used = ArtemiaCystsItem.usedJar(full);
        h.assertTrue(ArtemiaCystsItem.usesLeft(used) == 15 && used.getCount() == 1, "one use off: 15 left");
        h.assertTrue(ItemStack.isSameItemSameComponents(full, jars(3)), "full jars stack");
        h.assertTrue(!ItemStack.isSameItemSameComponents(full, used), "a used jar does not stack with full ones");
        h.assertTrue(ItemStack.isSameItemSameComponents(used, ArtemiaCystsItem.usedJar(jars(1))), "jars with the same uses stack");
        h.assertTrue(ArtemiaCystsItem.usedJar(ArtemiaCystsItem.withUsesLeft(full, 1)).isEmpty(), "the last use empties the jar");
        // an old jar (M2..M21.7 stored its spent uses as the vanilla damage) keeps its uses and stacks with its kind
        ItemStack old = jars(1);
        old.set(DataComponents.DAMAGE, 5);
        h.assertTrue(ArtemiaCystsItem.usesLeft(old) == 11, "old jar: 11 uses left");
        h.assertTrue(ArtemiaCystsItem.totalUses(jars(16)) == 256, "a full stack holds 256 uses");
        // a player picks up 20 full jars: 16 + 4
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        try {
            sp.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            sp.getInventory().clearContent();
            h.assertTrue(sp.getInventory().add(jars(20)), "picked up");
            int stacks = 0, total = 0, biggest = 0;
            for (ItemStack s : sp.getInventory().items) {
                if (!s.is(ModItems.ARTEMIA_CYSTS.get())) continue;
                stacks++;
                total += s.getCount();
                biggest = Math.max(biggest, s.getCount());
            }
            h.assertTrue(stacks == 2 && total == 20 && biggest == 16, "two stacks (16 + 4): " + stacks + " / " + total);
            // a used jar goes into a slot of its own
            sp.getInventory().add(used.copy());
            int usedSlots = 0;
            for (ItemStack s : sp.getInventory().items) if (s.is(ModItems.ARTEMIA_CYSTS.get()) && ArtemiaCystsItem.used(s) == 1) usedSlots++;
            h.assertTrue(usedSlots == 1, "the used jar on its own");
        } finally {
            sp.getServer().getPlayerList().remove(sp);
        }
        // crafting an artemia culture from a stack of five jars: one jar is split off and comes back with 15 uses
        CraftingInput input = CraftingInput.of(2, 1, List.of(new ItemStack(ModItems.SALT_WATER_BOTTLE.get()), jars(5)));
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        h.assertTrue(recipe.isPresent(), "artemia culture recipe");
        var rest = recipe.get().value().getRemainingItems(input);
        ItemStack back = rest.get(1);
        h.assertTrue(back.is(ModItems.ARTEMIA_CYSTS.get()) && back.getCount() == 1 && ArtemiaCystsItem.usesLeft(back) == 15,
                "one jar with 15 uses comes back: " + back);
        // the network codec (what the clients get) keeps count and uses; so does saving
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        ItemStack sixteen = jars(16);
        ItemStack old5 = old.copyWithCount(3);
        ItemStack.STREAM_CODEC.encode(buf, sixteen);
        ItemStack.STREAM_CODEC.encode(buf, old5);
        ItemStack a = ItemStack.STREAM_CODEC.decode(buf), b = ItemStack.STREAM_CODEC.decode(buf);
        h.assertTrue(a.getCount() == 16 && ArtemiaCystsItem.usesLeft(a) == 16, "sent: 16 full jars");
        h.assertTrue(b.getCount() == 3 && ArtemiaCystsItem.usesLeft(b) == 11, "sent: 3 jars with 11 uses");
        ItemStack loaded = ItemStack.parseOptional(level.registryAccess(), (net.minecraft.nbt.CompoundTag) old5.save(level.registryAccess()));
        h.assertTrue(loaded.getCount() == 3 && ArtemiaCystsItem.usesLeft(loaded) == 11, "saved and loaded");
        h.succeed();
    }

    /** 32 and 64 per stack by config (back to 16 afterwards); a stack above a smaller new limit stays as it is. */
    @GameTest(template = EMPTY, batch = "bettacraft_cyst_stack_config", timeoutTicks = 100)
    public static void stackSizeFollowsTheConfig(GameTestHelper h) {
        int before = ServerConfig.FOOD_CYST_STACK.get();
        try {
            ServerConfig.FOOD_CYST_STACK.set(32);
            h.assertTrue(jars(1).getMaxStackSize() == 32, "32");
            ServerConfig.FOOD_CYST_STACK.set(64);
            h.assertTrue(jars(1).getMaxStackSize() == 64, "64");
            ItemStack big = jars(64);
            ServerConfig.FOOD_CYST_STACK.set(16);
            h.assertTrue(jars(1).getMaxStackSize() == 16 && big.getCount() == 64, "back to 16: the stack of 64 keeps its jars");
            h.assertTrue(ArtemiaCystsItem.totalUses(big) == 64 * 16, "and its uses");
        } finally {
            ServerConfig.FOOD_CYST_STACK.set(before);
        }
        h.succeed();
    }

    /**
     * The artemia hatchery with a stack of jars: a hopper puts three in, a batch opens one jar (15 uses left in it, two full
     * ones stay in the slot), the next batch uses the open jar first; breaking the hatchery drops the open jar with its uses.
     * One old jar alone in the slot is used up in place.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void hatcheryOpensOneJarOfAStack(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos rel = new BlockPos(12, 2, 12);
        h.setBlock(rel, ModBlocks.ARTEMIA_HATCHERY.get().defaultBlockState());
        h.setBlock(rel.above().east(), Blocks.GLOWSTONE.defaultBlockState());
        BlockPos pos = h.absolutePos(rel);
        CultureTubBlockEntity tub = (CultureTubBlockEntity) level.getBlockEntity(pos);
        var items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        h.assertTrue(tub != null && items != null, "hatchery with its item handler");
        for (int i = 0; i < 4; i++) items.insertItem(CultureTubBlockEntity.INPUT, new ItemStack(ModItems.SALT_WATER_BOTTLE.get()), false);
        h.assertTrue(items.insertItem(CultureTubBlockEntity.INPUT, jars(3), false).isEmpty(), "three jars in");
        h.assertTrue(items.insertItem(CultureTubBlockEntity.INPUT, ArtemiaCystsItem.usedJar(jars(1)), false).getCount() == 1,
                "a used jar does not go onto the full ones");
        h.assertTrue(tub.ingredientUses() == 48, "48 uses: " + tub.ingredientUses());
        h.runAfterDelay(2, () -> {
            h.assertTrue(tub.blocker(level) == null, "ready: " + tub.blocker(level));
            h.assertTrue(tub.advance(level, 1.01) == 1, "a batch");
            ItemStack in = tub.items().getStackInSlot(CultureTubBlockEntity.INPUT);
            h.assertTrue(in.getCount() == 2 && ArtemiaCystsItem.usesLeft(in) == 16, "two full jars stay in the slot: " + in);
            h.assertTrue(ArtemiaCystsItem.usesLeft(tub.openedJar()) == 15, "the open jar has 15 left: " + tub.openedJar());
            h.assertTrue(tub.ingredientUses() == 47, "47 uses: " + tub.ingredientUses());
            tub.items().setStackInSlot(CultureTubBlockEntity.OUTPUT, ItemStack.EMPTY);
            tub.items().setStackInSlot(CultureTubBlockEntity.BOTTLES, new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE, 2));
            h.assertTrue(tub.advance(level, 1.01) == 1, "a second batch");
            h.assertTrue(ArtemiaCystsItem.usesLeft(tub.openedJar()) == 14 && tub.items().getStackInSlot(CultureTubBlockEntity.INPUT).getCount() == 2,
                    "the open jar first");
            // breaking it: two full jars and the open one with 14 uses
            tub.dropContents(level, pos);
            int full = 0, open = 0;
            for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2))) {
                ItemStack s = e.getItem();
                if (!s.is(ModItems.ARTEMIA_CYSTS.get())) continue;
                if (ArtemiaCystsItem.usesLeft(s) == 16) full += s.getCount();
                else if (ArtemiaCystsItem.usesLeft(s) == 14) open += s.getCount();
                e.discard();
            }
            h.assertTrue(full == 2 && open == 1, "dropped 2 full and the open jar: " + full + " / " + open);
            // an old jar alone in the slot: used up in place, as before
            ItemStack old = jars(1);
            old.set(DataComponents.DAMAGE, 14);
            tub.items().setStackInSlot(CultureTubBlockEntity.INPUT, old);
            tub.setWater(1000);
            tub.items().setStackInSlot(CultureTubBlockEntity.BOTTLES, new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE, 4));
            h.assertTrue(tub.advance(level, 1.01) == 1, "a batch from the old jar");
            h.assertTrue(ArtemiaCystsItem.usesLeft(tub.items().getStackInSlot(CultureTubBlockEntity.INPUT)) == 1 && tub.openedJar().isEmpty(),
                    "the old jar has its last use left, no jar opened");
            h.succeed();
        });
    }
}
