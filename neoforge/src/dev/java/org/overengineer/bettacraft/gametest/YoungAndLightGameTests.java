package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.equipment.TankLightItem;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.BubbleNest;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.plants.TankPlants;

/**
 * M18: young animals count in fractions for the stocking (bettas by stage, young of other mods, young small animals,
 * eggs and larvae by config) and the coloured aquarium lights (dyeing the bar, items, drops, recipes, old worlds, the
 * light for the plants stays the same).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class YoungAndLightGameTests {
    private static final String EMPTY = "empty_32x10x32";
    private static final double EPS = 1e-6;

    private YoungAndLightGameTests() {}

    private static TankBlockEntity tank(GameTestHelper h, int x, int y, int z, int sx, int sy, int sz, long water) {
        return CritterGameTests.tank(h, x, y, z, sx, sy, sz, water);
    }

    private static BettaData betta(LifeStage s, long seed) {
        return BettaData.create(BettaGenotype.randomDomestic(RandomSource.create(seed)), seed % 2 == 0, 0).withStage(s);
    }

    private static Component insert(GameTestHelper h, TankBlockEntity c, BettaData d) {
        return TankInhabitants.insert(h.getLevel(), c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d), ModEntities.BETTA.get(), null, 0L);
    }

    private static void mustInsert(GameTestHelper h, TankBlockEntity c, BettaData d) {
        Component err = insert(h, c, d);
        if (err != null) throw new GameTestFailure("insert failed: " + err.getString());
    }

    private static void near(GameTestHelper h, double got, double want, String what) {
        h.assertTrue(Math.abs(got - want) < 1e-4, what + ": expected " + want + ", got " + got);
    }

    private static BubbleNest nestWithEggs(TankBlockEntity c, int n) {
        BlockPos p = c.getBlockPos();
        CompoundTag t = new CompoundTag();
        t.putString("phase", "eggs");
        t.putDouble("x", p.getX() + 0.5);
        t.putDouble("y", p.getY() + 0.9);
        t.putDouble("z", p.getZ() + 0.5);
        ListTag eggs = new ListTag();
        for (int i = 0; i < n; i++) {
            CompoundTag e = new CompoundTag();
            e.put("genome", new CompoundTag());
            e.putBoolean("female", i % 2 == 0);
            eggs.add(e);
        }
        t.put("eggs", eggs);
        return BubbleNest.load(t);
    }

    // ------------------------------------------------------------------ stocking

    /** Defaults: fry 0.02, juvenile 0.1, subadult 0.3, adult 1; eggs and larvae 0. */
    @GameTest(template = EMPTY)
    public static void youngBettasCountInFractions(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 2, 2, 8000);
        mustInsert(h, c, betta(LifeStage.FRY, 1));
        mustInsert(h, c, betta(LifeStage.JUVENILE, 2));
        mustInsert(h, c, betta(LifeStage.SUBADULT, 3));
        mustInsert(h, c, betta(LifeStage.ADULT, 4));
        List<Entity> live = TankInhabitants.liveInhabitants(level, c.getTank());
        h.assertTrue(live.size() == 4, "four bettas, got " + live.size());
        TankInhabitants.Stocking st = TankInhabitants.breakdown(live, c.getTank());
        near(h, st.adults(), 1.0, "one adult");
        near(h, st.young(), 0.42, "fry + juvenile + subadult");
        near(h, TankInhabitants.stocking(level, c.getTank()), 1.42, "stocking");
        h.assertTrue(TankInhabitants.countAdults(live) == 1, "still one adult");
        // eggs in the nest count nothing by default
        c.getTank().setNest(nestWithEggs(c, 20));
        near(h, TankInhabitants.stocking(level, c.getTank()), 1.42, "eggs count 0");
        h.succeed();
    }

    /** Young animals of other mods count like a juvenile betta, young shrimp in their share of a fish; frogs unchanged. */
    @GameTest(template = EMPTY)
    public static void otherYoungCountLikeJuveniles(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Axolotl a = EntityType.AXOLOTL.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        h.assertTrue(a != null, "axolotl");
        a.setBaby(true);
        near(h, TankInhabitants.weight(a), 0.1, "baby axolotl");
        a.setBaby(false);
        near(h, TankInhabitants.weight(a), 1.0, "adult axolotl");
        a.discard();
        var frog = ModEntities.DWARF_FROG.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (frog != null) {
            near(h, TankInhabitants.weight(frog), 1.0, "frog (frogsPerFish 1)");
            frog.discard();
        }
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 1, 2, 4000);
        for (int i = 0; i < 25; i++) TankCritters.add(level, c, CritterSpecies.NEOCARIDINA, CritterGenes.WILD, i % 2 == 0, CritterSpecies.NEOCARIDINA.juvenileDays() + 1);
        near(h, TankCritters.stock(c.getTank()), 1.0, "25 adult shrimp");
        for (int i = 0; i < 25; i++) TankCritters.add(level, c, CritterSpecies.NEOCARIDINA, CritterGenes.WILD, i % 2 == 0, 0);
        near(h, TankCritters.stock(c.getTank()), 1.1, "25 young shrimp add a tenth of a fish");
        h.succeed();
    }

    /** At the hard cap a juvenile does not fit any more (it counted 0 before M18). */
    @GameTest(template = EMPTY)
    public static void youngNeedRoomToo(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 1, 1, 1, 1000);
        int cap = TankInhabitants.hardCap(c.getTank().waterBlocks());
        for (int i = 0; i < cap; i++) mustInsert(h, c, betta(LifeStage.ADULT, 10 + i));
        h.assertTrue(insert(h, c, betta(LifeStage.JUVENILE, 99)) != null, "no juvenile into a full tank");
        h.assertTrue(insert(h, c, betta(LifeStage.FRY, 98)) != null, "no fry either");
        h.succeed();
    }

    /** Every share is a server option (own batch: changes the config and restores it). */
    @GameTest(template = EMPTY, batch = "bettacraft_m18_stocking_config")
    public static void stockingSharesFromTheConfig(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        double egg = ServerConfig.STOCKING_EGG.get(), larva = ServerConfig.STOCKING_LARVA.get(), fry = ServerConfig.STOCKING_FRY.get(),
                juv = ServerConfig.STOCKING_JUVENILE.get(), sub = ServerConfig.STOCKING_SUBADULT.get();
        try {
            ServerConfig.STOCKING_EGG.set(0.01);
            ServerConfig.STOCKING_LARVA.set(0.01);
            ServerConfig.STOCKING_FRY.set(0.5);
            ServerConfig.STOCKING_JUVENILE.set(0.25);
            ServerConfig.STOCKING_SUBADULT.set(0.75);
            TankBlockEntity c = tank(h, 2, 1, 2, 2, 2, 2, 8000);
            mustInsert(h, c, betta(LifeStage.FRY, 1));
            mustInsert(h, c, betta(LifeStage.JUVENILE, 2));
            mustInsert(h, c, betta(LifeStage.SUBADULT, 3));
            near(h, TankInhabitants.stocking(level, c.getTank()), 1.5, "0.5 + 0.25 + 0.75");
            c.getTank().setNest(nestWithEggs(c, 10));
            near(h, TankInhabitants.nestStocking(c.getTank()), 0.1, "10 eggs at 0.01");
            near(h, TankInhabitants.stocking(level, c.getTank()), 1.6, "with the nest");
            TankCritters.add(level, c, CritterSpecies.NEOCARIDINA, CritterGenes.WILD, true, 0);
            near(h, TankCritters.stock(c.getTank()), 0.25 / 25, "a young shrimp at the juvenile share");
        } finally {
            ServerConfig.STOCKING_EGG.set(egg);
            ServerConfig.STOCKING_LARVA.set(larva);
            ServerConfig.STOCKING_FRY.set(fry);
            ServerConfig.STOCKING_JUVENILE.set(juv);
            ServerConfig.STOCKING_SUBADULT.set(sub);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ coloured lights

    private static BlockState light(DyeColor c) {
        return ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.LIT, true).setValue(TankLightBlock.COLOR, c);
    }

    /** A dye on one segment colours the whole bar for one dye; the same colour again keeps the dye. Plants: same light. */
    @GameTest(template = EMPTY)
    public static void dyeColoursTheWholeBar(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 1, 2, 3, 1, 1, 3000);
        for (int x = 2; x <= 4; x++) h.setBlock(new BlockPos(x, 2, 2), light(DyeColor.WHITE));
        int lightBefore = TankPlants.light(level, c);
        int[] before = TankCare.lights(level, c.getTank());
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        try {
            sp.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            ItemStack dye = new ItemStack(Items.RED_DYE, 2);
            sp.setItemInHand(InteractionHand.MAIN_HAND, dye);
            BlockPos abs = h.absolutePos(new BlockPos(3, 2, 2));
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
            sp.gameMode.useItemOn(sp, level, dye, InteractionHand.MAIN_HAND, hit);
            for (int x = 2; x <= 4; x++) {
                DyeColor col = level.getBlockState(h.absolutePos(new BlockPos(x, 2, 2))).getValue(TankLightBlock.COLOR);
                h.assertTrue(col == DyeColor.RED, "segment " + x + " red, is " + col);
            }
            h.assertTrue(sp.getMainHandItem().getCount() == 1, "one dye for the bar, left " + sp.getMainHandItem().getCount());
            sp.gameMode.useItemOn(sp, level, sp.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            h.assertTrue(sp.getMainHandItem().getCount() == 1, "already red: dye kept");
            // optics only: light level, the counted lights and the light the plants get are the same
            BlockState red = level.getBlockState(abs);
            h.assertTrue(red.getLightEmission() == 15, "light level 15");
            int[] after = TankCare.lights(level, c.getTank());
            h.assertTrue(after[0] == before[0] && after[1] == before[1], "same lights counted");
            h.assertTrue(TankPlants.light(level, c) == lightBefore, "plants get the same light: " + lightBefore + " vs " + TankPlants.light(level, c));
            for (DyeColor d : DyeColor.values()) h.assertTrue(light(d).getLightEmission() == 15, "level 15 for " + d);
        } finally {
            sp.level().getServer().getPlayerList().remove(sp);
        }
        h.succeed();
    }

    /** The item places its colour, the block drops it (white as the plain item), middle click gives it. */
    @GameTest(template = EMPTY)
    public static void lightsKeepTheirColourAsItems(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        h.setBlock(new BlockPos(4, 1, 4), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        try {
            ItemStack blue = TankLightItem.of(ModItems.TANK_LIGHT.get(), DyeColor.BLUE, 1);
            h.assertTrue(TankLightItem.colorOf(blue) == DyeColor.BLUE, "blue item");
            h.assertTrue(blue.getHoverName().getString().contains("(") , "coloured name: " + blue.getHoverName().getString());
            sp.setItemInHand(InteractionHand.MAIN_HAND, blue);
            BlockPos stone = h.absolutePos(new BlockPos(4, 1, 4));
            blue.useOn(new UseOnContext(sp, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(stone).add(0, 0.5, 0), Direction.UP, stone, false)));
            BlockState placed = level.getBlockState(stone.above());
            h.assertTrue(placed.is(ModBlocks.TANK_LIGHT.get()) && placed.getValue(TankLightBlock.COLOR) == DyeColor.BLUE, "placed blue: " + placed);
            List<ItemStack> drops = Block.getDrops(placed, level, stone.above(), null);
            h.assertTrue(drops.size() == 1 && TankLightItem.colorOf(drops.getFirst()) == DyeColor.BLUE, "drops blue: " + drops);
            List<ItemStack> white = Block.getDrops(light(DyeColor.WHITE), level, stone.above(), null);
            h.assertTrue(white.size() == 1 && !white.getFirst().has(DataComponents.BLOCK_STATE), "white drops the plain item: " + white);
            h.assertTrue(ItemStack.isSameItemSameComponents(white.getFirst(), new ItemStack(ModItems.TANK_LIGHT.get())), "stacks with old lights");
            ItemStack clone = placed.getCloneItemStack(level, stone.above(), false);
            h.assertTrue(TankLightItem.colorOf(clone) == DyeColor.BLUE, "middle click: blue");
        } finally {
            sp.level().getServer().getPlayerList().remove(sp);
        }
        h.succeed();
    }

    /** Recipes: any light + dye, eight lights around a dye; white dye gives the plain light back. */
    @GameTest(template = EMPTY)
    public static void lightDyeRecipes(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ItemStack blue = TankLightItem.of(ModItems.TANK_LIGHT.get(), DyeColor.BLUE, 1);
        ItemStack out = craft(level, CraftingInput.of(2, 1, List.of(blue, new ItemStack(Items.RED_DYE))));
        h.assertTrue(out.is(ModItems.TANK_LIGHT.get()) && TankLightItem.colorOf(out) == DyeColor.RED && out.getCount() == 1, "blue + red dye = red: " + out);
        ItemStack back = craft(level, CraftingInput.of(2, 1, List.of(out, new ItemStack(Items.WHITE_DYE))));
        h.assertTrue(back.is(ModItems.TANK_LIGHT.get()) && !back.has(DataComponents.BLOCK_STATE), "white dye: plain light " + back);
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) grid.add(i == 4 ? new ItemStack(Items.LIME_DYE) : new ItemStack(ModItems.TANK_LIGHT.get()));
        ItemStack eight = craft(level, CraftingInput.of(3, 3, grid));
        h.assertTrue(TankLightItem.colorOf(eight) == DyeColor.LIME && eight.getCount() == 8, "eight lime lights: " + eight);
        h.succeed();
    }

    private static ItemStack craft(ServerLevel level, CraftingInput in) {
        return level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, in, level)
                .map(r -> r.value().assemble(in, level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    /** Lights of worlds from before M18 have no colour property in their saved state: they load white. */
    @GameTest(template = EMPTY)
    public static void oldLightsLoadWhite(GameTestHelper h) {
        CompoundTag t = new CompoundTag();
        t.putString("Name", BettaCraft.id("tank_light").toString());
        CompoundTag p = new CompoundTag();
        p.putString("axis", "z");
        p.putString("hanging", "false");
        p.putString("lit", "true");
        p.putString("neg", "true");
        p.putString("pos", "false");
        p.putString("powered", "false");
        t.put("Properties", p);
        BlockState s = NbtUtils.readBlockState(h.getLevel().holderLookup(Registries.BLOCK), t);
        h.assertTrue(s.is(ModBlocks.TANK_LIGHT.get()), "a light: " + s);
        h.assertTrue(s.getValue(TankLightBlock.COLOR) == DyeColor.WHITE, "white");
        h.assertTrue(s.getValue(TankLightBlock.AXIS) == Direction.Axis.Z && s.getValue(TankLightBlock.NEG), "other properties kept");
        h.succeed();
    }

    /** Bettas are the only fish whose stage we know; check that every stage has its share. */
    @GameTest(template = EMPTY)
    public static void everyStageHasAShare(GameTestHelper h) {
        near(h, LifeStage.EGG.stocking(), 0, "egg");
        near(h, LifeStage.LARVA.stocking(), 0, "larva");
        near(h, LifeStage.FRY.stocking(), 0.02, "fry");
        near(h, LifeStage.JUVENILE.stocking(), 0.1, "juvenile");
        near(h, LifeStage.SUBADULT.stocking(), 0.3, "subadult");
        near(h, LifeStage.ADULT.stocking(), 1, "adult");
        h.succeed();
    }
}
