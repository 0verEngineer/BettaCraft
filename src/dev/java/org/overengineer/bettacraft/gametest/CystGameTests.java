package org.overengineer.bettacraft.gametest;

import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.food.CultureTubBlockEntity;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.sources.Source;
import org.overengineer.bettacraft.sources.Sources;
import org.overengineer.bettacraft.sources.Target;

/**
 * M35.1 GameTests (replacing those of M21.8): one jar of artemia cysts is one culture and always stacks to 64; old jars
 * (their spent uses in the vanilla damage component) keep their count, lose the damage wherever they are loaded (saves,
 * the network, item entities, the hatchery) and stack with new ones; the sand drop is 4x as likely as before.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CystGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private CystGameTests() {}

    private static ItemStack jars(int n) {
        return new ItemStack(ModItems.ARTEMIA_CYSTS.get(), n);
    }

    /** {@code n} jars as a save of M21.8..M35 holds them: the spent uses in {@code minecraft:damage}. */
    private static CompoundTag oldJarsTag(int n, int damage) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "bettacraft:artemia_cysts");
        tag.putInt("count", n);
        CompoundTag comps = new CompoundTag();
        comps.putInt("minecraft:damage", damage);
        tag.put("components", comps);
        return tag;
    }

    private static boolean plain(ItemStack s) {
        return s.is(ModItems.ARTEMIA_CYSTS.get()) && !s.has(DataComponents.DAMAGE) && ItemStack.isSameItemSameComponents(s, jars(1));
    }

    /** Crafting an artemia culture uses one jar up (nothing comes back); one hatchery batch uses one jar, then it waits. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void oneJarStartsOneCulture(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ItemStack jar = jars(1);
        h.assertTrue(!jar.isDamageableItem() && !jar.isBarVisible(), "no wear, no bar");
        h.assertTrue(!jar.getItem().hasCraftingRemainingItem(jar) && jar.getItem().getCraftingRemainingItem(jar).isEmpty(), "no remainder");
        CraftingInput input = CraftingInput.of(2, 1, List.of(new ItemStack(ModItems.SALT_WATER_BOTTLE.get()), jars(5)));
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        h.assertTrue(recipe.isPresent(), "artemia culture recipe");
        ItemStack out = recipe.get().value().assemble(input, level.registryAccess());
        h.assertTrue(out.is(ModItems.ARTEMIA_CULTURE.get()) && out.getCount() == 1, "one culture: " + out);
        ItemStack back = recipe.get().value().getRemainingItems(input).get(1);
        h.assertTrue(back.isEmpty(), "the used jar is gone (nothing comes back): " + back);

        // the hatchery: one jar, one batch
        BlockPos rel = new BlockPos(12, 2, 12);
        h.setBlock(rel, ModBlocks.ARTEMIA_HATCHERY.get().defaultBlockState());
        h.setBlock(rel.above().east(), Blocks.GLOWSTONE.defaultBlockState());
        BlockPos pos = h.absolutePos(rel);
        CultureTubBlockEntity tub = (CultureTubBlockEntity) level.getBlockEntity(pos);
        var items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        h.assertTrue(tub != null && items != null, "hatchery with its item handler");
        for (int i = 0; i < 4; i++) items.insertItem(CultureTubBlockEntity.INPUT, new ItemStack(ModItems.SALT_WATER_BOTTLE.get()), false);
        h.assertTrue(items.insertItem(CultureTubBlockEntity.INPUT, jars(1), false).isEmpty(), "one jar in");
        h.assertTrue(tub.ingredientUses() == 1, "one batch: " + tub.ingredientUses());
        h.runAfterDelay(2, () -> {
            h.assertTrue(tub.blocker(level) == null, "ready: " + tub.blocker(level));
            h.assertTrue(tub.advance(level, 1.01) == 1, "a batch");
            h.assertTrue(tub.items().getStackInSlot(CultureTubBlockEntity.INPUT).isEmpty() && tub.openedJar().isEmpty(), "the jar is gone");
            h.assertTrue(tub.ingredientUses() == 0, "no batch left");
            tub.items().setStackInSlot(CultureTubBlockEntity.OUTPUT, ItemStack.EMPTY);
            tub.items().setStackInSlot(CultureTubBlockEntity.BOTTLES, new ItemStack(Items.GLASS_BOTTLE, 4));
            h.assertTrue("cysts".equals(tub.blocker(level)), "the next batch waits for a jar: " + tub.blocker(level));
            h.assertTrue(tub.advance(level, 1.01) == 0, "no second batch from the same jar");
            h.succeed();
        });
    }

    /** Jars always stack to 64: a player picks up 100 (64 + 36). */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void jarsStackTo64(GameTestHelper h) {
        h.assertTrue(jars(1).getMaxStackSize() == 64 && jars(1).isStackable(), "64 per stack: " + jars(1).getMaxStackSize());
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        try {
            sp.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            sp.getInventory().clearContent();
            h.assertTrue(sp.getInventory().add(jars(100)), "picked up");
            int stacks = 0, total = 0, biggest = 0;
            for (ItemStack s : sp.getInventory().items) {
                if (!s.is(ModItems.ARTEMIA_CYSTS.get())) continue;
                stacks++;
                total += s.getCount();
                biggest = Math.max(biggest, s.getCount());
            }
            h.assertTrue(stacks == 2 && total == 100 && biggest == 64, "two stacks (64 + 36): " + stacks + " / " + total + " / " + biggest);
        } finally {
            sp.getServer().getPlayerList().remove(sp);
        }
        h.succeed();
    }

    /**
     * An old stack of 16 used jars (damage set) loads as 16 plain jars and stacks with new ones: from a save, over the network,
     * as an item entity on the ground, by copying, and in a player's inventory (16 old + 48 new = one stack of 64).
     */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void oldDamagedJarsBecomeWholeJars(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var reg = level.registryAccess();
        ItemStack loaded = ItemStack.parseOptional(reg, oldJarsTag(16, 9));
        h.assertTrue(loaded.getCount() == 16 && plain(loaded), "saved old jars load as 16 plain jars: " + loaded + " " + loaded.getComponentsPatch());
        // a stack that still carries the damage (as if it were made in memory) is cleaned by copy() and by the network codec
        ItemStack raw = jars(16);
        raw.set(DataComponents.DAMAGE, 5);
        h.assertTrue(plain(raw.copy()) && raw.copy().getCount() == 16, "copy() drops the old uses");
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), reg);
        ItemStack.STREAM_CODEC.encode(buf, raw);
        ItemStack sent = ItemStack.STREAM_CODEC.decode(buf);
        h.assertTrue(sent.getCount() == 16 && plain(sent), "sent: 16 plain jars: " + sent);
        // an item entity of an old world
        ItemEntity e = new ItemEntity(level, 0, 0, 0, jars(16));
        CompoundTag et = new CompoundTag();
        e.saveWithoutId(et);
        CompoundTag item = et.getCompound("Item");
        CompoundTag comps = item.getCompound("components");
        comps.putInt("minecraft:damage", 12);
        item.put("components", comps);
        ItemEntity back = new ItemEntity(EntityType.ITEM, level);
        back.load(et);
        h.assertTrue(back.getItem().getCount() == 16 && plain(back.getItem()), "item entity: 16 plain jars: " + back.getItem());
        // they stack with new ones
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        try {
            sp.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            sp.getInventory().clearContent();
            sp.getInventory().setItem(0, loaded);
            h.assertTrue(sp.getInventory().add(jars(48)), "48 new jars picked up");
            ItemStack s0 = sp.getInventory().getItem(0);
            h.assertTrue(s0.getCount() == 64 && plain(s0), "one stack of 64: " + s0);
            for (int i = 1; i < sp.getInventory().items.size(); i++) {
                h.assertTrue(sp.getInventory().items.get(i).isEmpty(), "nothing in slot " + i);
            }
        } finally {
            sp.getServer().getPlayerList().remove(sp);
        }
        h.succeed();
    }

    /**
     * A hatchery of an old world: 16 old used jars in the input slot and an open jar (its rest of uses) load as 17 plain jars
     * in the slot; batches go on, one jar each.
     */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void hatcheryWithOldJarsWorks(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var reg = level.registryAccess();
        BlockPos rel = new BlockPos(12, 2, 12);
        h.setBlock(rel, ModBlocks.ARTEMIA_HATCHERY.get().defaultBlockState());
        h.setBlock(rel.above().east(), Blocks.GLOWSTONE.defaultBlockState());
        BlockPos pos = h.absolutePos(rel);
        CultureTubBlockEntity tub = (CultureTubBlockEntity) level.getBlockEntity(pos);
        h.assertTrue(tub != null, "hatchery");
        tub.items().setStackInSlot(CultureTubBlockEntity.INPUT, jars(16));
        CompoundTag tag = tub.saveWithoutMetadata(reg);
        // write it as M21.8..M35 did: the jars in the slot with spent uses, one open jar beside them
        ListTag list = tag.getCompound("items").getList("Items", Tag.TAG_COMPOUND);
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            CompoundTag slot = list.getCompound(i);
            if (slot.getInt("Slot") != CultureTubBlockEntity.INPUT) continue;
            CompoundTag comps = slot.getCompound("components");
            comps.putInt("minecraft:damage", 4);
            slot.put("components", comps);
            found = true;
        }
        h.assertTrue(found, "the input slot in the save");
        tag.put("opened_jar", oldJarsTag(1, 11));
        tub.items().setStackInSlot(CultureTubBlockEntity.INPUT, ItemStack.EMPTY);
        tub.loadWithComponents(tag, reg);
        ItemStack in = tub.items().getStackInSlot(CultureTubBlockEntity.INPUT);
        h.assertTrue(in.getCount() == 17 && plain(in), "17 plain jars in the slot: " + in + " " + in.getComponentsPatch());
        h.assertTrue(tub.openedJar().isEmpty() && tub.ingredientUses() == 17, "no open jar left, 17 batches: " + tub.ingredientUses());
        // a jar inserted later stacks onto them
        var items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        h.assertTrue(items != null && items.insertItem(CultureTubBlockEntity.INPUT, jars(1), false).isEmpty(), "a new jar goes onto the old ones");
        tub.setWater(1000);
        tub.items().setStackInSlot(CultureTubBlockEntity.BOTTLES, new ItemStack(Items.GLASS_BOTTLE, 4));
        h.runAfterDelay(2, () -> {
            h.assertTrue(tub.blocker(level) == null, "ready: " + tub.blocker(level));
            h.assertTrue(tub.advance(level, 1.01) == 1, "a batch from the old jars");
            h.assertTrue(tub.items().getStackInSlot(CultureTubBlockEntity.INPUT).getCount() == 17, "one jar used: 17 left");
            ItemStack nauplii = tub.items().getStackInSlot(CultureTubBlockEntity.OUTPUT);
            h.assertTrue(nauplii.is(ModItems.ARTEMIA_NAUPLII.get()) && nauplii.getCount() == tub.batchYield(), "nauplii: " + nauplii);
            h.succeed();
        });
    }

    /**
     * The sources of cysts: the only one is the sand drop (2 % -> 8 % in M35.1, halved to 4 % in M46, one jar each; no trader
     * offer, loot, recipe or net catch gives cysts), the handbook source table says so, and 4000 sand blocks broken with a fixed
     * seed drop about 4 % jars.
     */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void sandDropsFourTimesAsManyJars(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        double chance = ServerConfig.DROPS_ARTEMIA_FROM_SAND_CHANCE.getDefault();
        h.assertTrue(Math.abs(chance - ServerConfig.ARTEMIA_SAND_CHANCE_M35 / 2) < 1e-9 && Math.abs(chance - 0.04) < 1e-9, "4 % = 8 % / 2: " + chance);
        List<Source> sources = Sources.of(Target.item("artemia_cysts"));
        h.assertTrue(sources.size() == 1, "one source of cysts: " + sources);
        Source s = sources.get(0);
        h.assertTrue(s.kind() == Source.Kind.BLOCK_DROP && Math.abs(s.chance() - 0.04) < 1e-9 && s.min() <= 1 && s.max() <= 1,
                "the sand drop, 4 %, one jar: " + s);
        h.assertTrue(sources.stream().noneMatch(x -> x.kind() == Source.Kind.TRADER_USUAL || x.kind() == Source.Kind.TRADER_RARE), "no trader offer");
        // break sand 4000 times with one random source of a fixed seed (the first values of neighbouring seeds are alike)
        BlockPos pos = h.absolutePos(new BlockPos(4, 2, 4));
        var table = level.getServer().reloadableRegistries().getLootTable(Blocks.SAND.getLootTable());
        int jars = 0, sand = 0;
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(351L);
        for (int i = 0; i < 4000; i++) {
            LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                    .withParameter(LootContextParams.BLOCK_STATE, Blocks.SAND.defaultBlockState())
                    .withParameter(LootContextParams.TOOL, new ItemStack(Items.IRON_SHOVEL))
                    .create(LootContextParamSets.BLOCK);
            for (ItemStack drop : table.getRandomItems(params, random)) {
                if (drop.is(ModItems.ARTEMIA_CYSTS.get())) jars += drop.getCount();
                if (drop.is(Items.SAND)) sand += drop.getCount();
            }
        }
        h.assertTrue(sand == 4000, "the sand itself: " + sand);
        h.assertTrue(jars >= 110 && jars <= 210, "about 4 % jars (160 of 4000; 320 in 1.36.0): " + jars);
        h.succeed();
    }

    /**
     * M46: a world with its own server config ({@code <world>/serverconfig/bettacraft-server.toml}) still at the 1.36.0 default
     * 0.08 gets 0.04 once after the server started (the file kept as .pre-m46); the world records the migration, so 0.08 set
     * afterwards stays.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m46_world_config", timeoutTicks = 100)
    public static void worldServerConfigGetsTheHalvedSandChanceOnce(GameTestHelper h) throws java.io.IOException {
        var server = h.getLevel().getServer();
        var migrations = org.overengineer.bettacraft.config.WorldConfigMigration.Done.get(server);
        String id = org.overengineer.bettacraft.config.WorldConfigMigration.ARTEMIA_M46;
        double saved = ServerConfig.DROPS_ARTEMIA_FROM_SAND_CHANCE.get();
        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("bettacraft-m46");
        java.nio.file.Path file = dir.resolve("bettacraft-server.toml");
        try {
            java.nio.file.Files.writeString(file, "[drops]\n\tartemiaFromSandChance = 0.08\n");
            h.assertTrue(migrations.has(id), "the test world was migrated when it started");
            migrations.remove(id); // an old world: not migrated yet
            ServerConfig.DROPS_ARTEMIA_FROM_SAND_CHANCE.set(0.08);
            h.assertTrue(org.overengineer.bettacraft.config.WorldConfigMigration.run(server, file), "the old default was not migrated");
            h.assertTrue(Math.abs(ServerConfig.DROPS_ARTEMIA_FROM_SAND_CHANCE.get() - 0.04) < 1e-9, "0.08 -> 0.04: " + ServerConfig.DROPS_ARTEMIA_FROM_SAND_CHANCE.get());
            h.assertTrue(migrations.has(id), "the world records the migration");
            h.assertTrue(java.nio.file.Files.exists(dir.resolve("bettacraft-server.toml.pre-m46")), "the old file is kept");
            // the player sets 0.08 again: it stays
            ServerConfig.DROPS_ARTEMIA_FROM_SAND_CHANCE.set(0.08);
            h.assertTrue(!org.overengineer.bettacraft.config.WorldConfigMigration.run(server, file), "migrated twice");
            h.assertTrue(Math.abs(ServerConfig.DROPS_ARTEMIA_FROM_SAND_CHANCE.get() - 0.08) < 1e-9, "a chance set later stays");
            // a set chance (0.10) in a world not migrated yet stays, the world is still recorded
            migrations.remove(id);
            ServerConfig.DROPS_ARTEMIA_FROM_SAND_CHANCE.set(0.10);
            h.assertTrue(!org.overengineer.bettacraft.config.WorldConfigMigration.run(server, file), "0.10 changed");
            h.assertTrue(Math.abs(ServerConfig.DROPS_ARTEMIA_FROM_SAND_CHANCE.get() - 0.10) < 1e-9 && migrations.has(id), "0.10 stays, recorded");
        } finally {
            migrations.add(id);
            ServerConfig.DROPS_ARTEMIA_FROM_SAND_CHANCE.set(saved);
        }
        h.succeed();
    }
}
