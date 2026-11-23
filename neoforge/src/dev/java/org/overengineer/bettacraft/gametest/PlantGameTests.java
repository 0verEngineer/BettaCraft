package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.network.TankActionPayload;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankBreeding;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.menu.TankActions;
import org.overengineer.bettacraft.tank.menu.TankMenu;
import org.overengineer.bettacraft.tank.plants.FloatingPlant;
import org.overengineer.bettacraft.tank.plants.FloatingPlantSim;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tank.plants.PlantSurface;
import org.overengineer.bettacraft.tank.plants.TankPlants;

/**
 * M12 GameTests: floating plants on the tank surface (placing like a player, lily pads, growth with light and waste,
 * the coverage cap, darkness, taking out / thinning / the menu button, free shapes and filter jets, the nest, fishing
 * loot and trades, migration of an M11 controller).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlantGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private PlantGameTests() {}

    private static TankBlockEntity tank(GameTestHelper h, List<BlockPos> blocks, long water) {
        for (BlockPos p : blocks) {
            h.setBlock(p, ModBlocks.TANK.get().defaultBlockState());
            TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(p), null);
        }
        TankBlockEntity c = TankMultiblock.getController(h.getLevel(), h.absolutePos(blocks.getFirst()));
        if (c == null) throw new GameTestAssertException("tank did not form");
        if (water > 0) TankInteractions.changeWater(h.getLevel(), c, water);
        return c;
    }

    private static TankBlockEntity box(GameTestHelper h, int x, int y, int z, int sx, int sy, int sz, long water) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int a = 0; a < sx; a++) for (int b = 0; b < sy; b++) for (int c = 0; c < sz; c++) blocks.add(new BlockPos(x + a, y + b, z + c));
        return tank(h, blocks, water);
    }

    /** A player standing above the tank and looking straight down at (x, z) (absolute). */
    private static ServerPlayer lookingDown(GameTestHelper h, double x, double y, double z) {
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        sp.teleportTo(h.getLevel(), x, y - sp.getEyeHeight(), z, 0F, 90F);
        sp.setXRot(90F);
        sp.setYRot(0F);
        sp.getInventory().selected = 8; // the hand slot stays apart from the items the player gets
        return sp;
    }

    private static void use(ServerPlayer sp, GameTestHelper h, BlockPos absTop, ItemStack stack) {
        sp.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absTop).add(0, 0.5, 0), Direction.UP, absTop, false);
        sp.gameMode.useItemOn(sp, h.getLevel(), stack, InteractionHand.MAIN_HAND, hit);
    }

    private static int count(ServerPlayer sp, Item item) {
        int n = 0;
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            ItemStack s = sp.getInventory().getItem(i);
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }

    private static Item item(FloatingPlantType t) {
        return TankPlants.item(t).getItem();
    }

    private static void litLight(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel, ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.LIT, true));
    }

    // ------------------------------------------------------------------ placing

    /** A plant item used on the tank floats where the player looks; it is used up like a player would expect. */
    @GameTest(template = EMPTY)
    public static void plantItemFloatsWhereThePlayerLooks(GameTestHelper h) {
        TankBlockEntity c = box(h, 4, 1, 4, 3, 2, 3, 18000);
        TankData t = c.getTank();
        BlockPos top = h.absolutePos(new BlockPos(5, 2, 5));
        Vec3 look = Vec3.atLowerCornerOf(h.absolutePos(new BlockPos(6, 4, 4))).add(0.3, 0, 0.6);
        ServerPlayer sp = lookingDown(h, look.x, look.y, look.z);
        try {
            sp.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            use(sp, h, top, new ItemStack(item(FloatingPlantType.FROGBIT), 3));
            h.assertTrue(t.plants().size() == 1, "one plant on the surface, got " + t.plants().size());
            FloatingPlant p = t.plants().getFirst();
            h.assertTrue(p.type == FloatingPlantType.FROGBIT, "frogbit");
            h.assertTrue(Math.hypot(p.x - look.x, p.z - look.z) < 0.35, "where the player looks: " + p.x + "/" + p.z + " vs " + look);
            h.assertTrue(sp.getMainHandItem().getCount() == 2, "one item used, left " + sp.getMainHandItem().getCount());
            h.assertTrue(!(sp.containerMenu instanceof TankMenu), "no menu opened");
            h.assertTrue(TankData.loadClientView(t.saveClientView()).plants().size() == 1, "clients get the plant");
            // sneaking works as well (no block of a lily pad on top of the tank either)
            sp.setShiftKeyDown(true);
            use(sp, h, top, new ItemStack(Items.LILY_PAD));
            h.assertTrue(t.plants().size() == 2 && t.plants().get(1).type == FloatingPlantType.LILY_PAD, "lily pad on the surface");
            h.assertTrue(h.getLevel().getBlockState(top.above()).isAir(), "no lily pad block placed");
        } finally {
            sp.getServer().getPlayerList().remove(sp);
        }
        h.succeed();
    }

    /** A lily pad stays where it was put and never multiplies, also with light and nutrients. */
    @GameTest(template = EMPTY)
    public static void lilyPadStaysFixed(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = box(h, 4, 1, 4, 2, 1, 2, 4000);
        TankData t = c.getTank();
        litLight(h, new BlockPos(4, 2, 4));
        t.setWaterQuality(0.6F);
        FloatingPlant lily = TankPlants.add(c, FloatingPlantType.LILY_PAD, h.absolutePos(new BlockPos(5, 1, 5)).getX() + 0.2,
                h.absolutePos(new BlockPos(5, 1, 5)).getZ() + 0.4, 1F, 0F);
        h.assertTrue(lily != null, "placed");
        double x = lily.x, z = lily.z;
        for (int i = 0; i < 200; i++) TankPlants.step(level, c, 0.05);
        h.assertTrue(t.plants().size() == 1, "never multiplies: " + t.plants().size());
        h.assertTrue(lily.x == x && lily.z == z, "does not move");
        h.succeed();
    }

    // ------------------------------------------------------------------ growth

    /** Under a lit tank light in water with waste the plants grow, form offshoots and take up waste; the cover is capped. */
    @GameTest(template = EMPTY)
    public static void growsWithLightAndWasteUpToTheCap(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = box(h, 4, 1, 4, 3, 1, 3, 9000);
        TankData t = c.getTank();
        for (int x = 4; x < 7; x++) for (int z = 4; z < 7; z++) litLight(h, new BlockPos(x, 2, z));
        BlockPos o = h.absolutePos(new BlockPos(4, 1, 4));
        TankPlants.add(c, FloatingPlantType.DUCKWEED, o.getX() + 1.5, o.getZ() + 1.5, 0.5F, 0F);
        TankPlants.add(c, FloatingPlantType.SALVINIA, o.getX() + 0.6, o.getZ() + 2.4, 0.5F, 0F);
        h.assertTrue(TankPlants.light(level, c) == 15, "strong light under the tank light: " + TankPlants.light(level, c));
        t.setWaterQuality(0.6F);
        TankPlants.grow(level, c, 0.5);
        h.assertTrue(t.plants().size() > 2, "offshoots after half a day: " + t.plants().size());
        h.assertTrue(t.waterQuality() > 0.6F, "waste taken up: " + t.waterQuality());
        for (int i = 0; i < 60; i++) {
            t.setWaterQuality(0.6F); // the animals keep adding waste
            TankPlants.grow(level, c, 0.5);
        }
        int n = t.plants().size();
        double cov = TankPlants.coverage(t);
        double cap = TankPlants.params().maxCoverage();
        h.assertTrue(cov > cap - 0.15 && cov < cap + 0.12, "cover near the cap " + cap + ": " + cov);
        for (int i = 0; i < 20; i++) {
            t.setWaterQuality(0.6F);
            TankPlants.grow(level, c, 0.5);
        }
        h.assertTrue(t.plants().size() == n, "no offshoots above the cap: " + n + " -> " + t.plants().size());
        h.assertTrue(TankPlants.dense(t), "dense cover warns");
        h.assertTrue(TankBreeding.warnings(level, c).contains("dense_plants"), "warning line");
        PlantSurface s = TankPlants.surface(t);
        for (FloatingPlant p : t.plants()) h.assertTrue(s.contains(p.x, p.z), "offshoot over the water at " + p.x + "/" + p.z);
        h.succeed();
    }

    /**
     * Several in-game days in the care steps of a tank with a lit tank light, two bettas and a feeder (water quality,
     * plants and the feeder as the care loop runs them, a quarter day per step): the plants spread up to the cap, the
     * dense cover warns (menu warning, info overlay line) and food stays on the leaves (log line), the red root floater
     * turns red.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_plants_days", timeoutTicks = 200)
    public static void severalDaysWithLightAnimalsAndFeeder(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        double savedCap = ServerConfig.PLANTS_MAX_COVERAGE.get(), savedLeaves = ServerConfig.PLANTS_PELLETS_ON_LEAVES.get();
        try {
            ServerConfig.PLANTS_MAX_COVERAGE.set(0.8);
            ServerConfig.PLANTS_PELLETS_ON_LEAVES.set(1.0);
            TankBlockEntity c = box(h, 4, 1, 4, 4, 2, 3, 20000);
            TankData t = c.getTank();
            for (int x = 4; x < 8; x++) for (int z = 4; z < 7; z++) if (!(x == 4 && z == 4)) litLight(h, new BlockPos(x, 3, z));
            BlockPos feederPos = new BlockPos(4, 3, 4);
            h.setBlock(feederPos, ModBlocks.FEEDER.get().defaultBlockState());
            org.overengineer.bettacraft.equipment.FeederBlockEntity feeder = (org.overengineer.bettacraft.equipment.FeederBlockEntity) h.getBlockEntity(feederPos);
            feeder.items().insertItem(0, new ItemStack(ModItems.BETTA_PELLETS.get(), 64), false);
            for (int i = 0; i < 2; i++) {
                var g = org.overengineer.bettacraft.genetics.BettaGenome.randomDomestic(net.minecraft.util.RandomSource.create(40 + i));
                ItemStack bucket = org.overengineer.bettacraft.entity.betta.BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()),
                        org.overengineer.bettacraft.entity.betta.BettaData.create(g, i == 1, 0));
                var err = TankInhabitants.insert(level, c, bucket, org.overengineer.bettacraft.registry.ModEntities.BETTA.get(), null, false);
                h.assertTrue(err == null, "insert: " + (err == null ? "" : err.getString()));
            }
            BlockPos o = h.absolutePos(new BlockPos(4, 1, 4));
            TankPlants.add(c, FloatingPlantType.DUCKWEED, o.getX() + 1.2, o.getZ() + 1.5, 0.5F, 0F);
            TankPlants.add(c, FloatingPlantType.SALVINIA, o.getX() + 2.8, o.getZ() + 1.5, 0.5F, 0F);
            FloatingPlant rrf = TankPlants.add(c, FloatingPlantType.RED_ROOT_FLOATER, o.getX() + 2.0, o.getZ() + 0.8, 0.5F, 0F);
            h.assertTrue(TankPlants.light(level, c) == 15, "strong light under the tank lights");
            double days = 0;
            int steps = 0;
            while (days < 30 && !(TankPlants.dense(t) && t.log().stream().anyMatch(e -> e.key().equals("log.bettacraft.pellets_on_leaves")))) {
                org.overengineer.bettacraft.tank.TankWater.step(level, c, 0.25);
                TankPlants.step(level, c, 0.25);
                if (steps % 2 == 0) feeder.feedNow(level, c);
                days += 0.25;
                steps++;
            }
            h.assertTrue(t.plants().size() > 10, "the plants spread over " + days + " days: " + t.plants().size());
            h.assertTrue(TankPlants.dense(t), "dense cover after " + days + " days: " + TankPlants.coverage(t));
            h.assertTrue(TankPlants.coverage(t) < 0.8 + 0.12, "cover stays near the cap: " + TankPlants.coverage(t));
            h.assertTrue(t.log().stream().anyMatch(e -> e.key().equals("log.bettacraft.plants_full")), "log: surface full");
            h.assertTrue(t.log().stream().anyMatch(e -> e.key().equals("log.bettacraft.pellets_on_leaves")), "log: food on the leaves");
            h.assertTrue(TankBreeding.warnings(level, c).contains("dense_plants"), "menu warning for the dense cover");
            h.assertTrue(rrf.red > 0.9F, "red root floater red under the tank light: " + rrf.red);
            // the info overlay line (Jade / The One Probe / WTHIT share it)
            var d = org.overengineer.bettacraft.compat.info.InfoData.tank(level, c.getBlockPos());
            h.assertTrue(d.getInt("plants") == t.plants().size(), "overlay data: plant count");
            boolean line = false;
            for (var l : org.overengineer.bettacraft.compat.info.InfoLines.tank(d, true)) { // M64: with sneaking
                if (l.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc && tc.getKey().equals("jade.bettacraft.tank.plants")) {
                    line = String.valueOf(tc.getArgs()[0]).equals(String.valueOf(t.plants().size()))
                            && String.valueOf(tc.getArgs()[1]).equals(String.valueOf(Math.min(100, Math.round(TankPlants.coverage(t) * 100))));
                }
            }
            h.assertTrue(line, "overlay line \"Floating plants: N (x % cover)\"");
            // all plants over the water of the tank
            PlantSurface surf = TankPlants.surface(t);
            for (FloatingPlant p : t.plants()) h.assertTrue(surf.contains(p.x, p.z), "over the water: " + p.x + "/" + p.z);
        } finally {
            ServerConfig.PLANTS_MAX_COVERAGE.set(savedCap);
            ServerConfig.PLANTS_PELLETS_ON_LEAVES.set(savedLeaves);
        }
        h.succeed();
    }

    /** Enclosed in stone (no sky, no lamp) plants neither grow nor take up waste. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void noGrowthInTheDark(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int x = 3; x <= 6; x++) for (int y = 0; y <= 3; y++) for (int z = 3; z <= 6; z++) h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
        for (int x = 4; x <= 5; x++) for (int y = 1; y <= 2; y++) for (int z = 4; z <= 5; z++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        TankBlockEntity c = box(h, 4, 1, 4, 2, 1, 2, 4000);
        TankData t = c.getTank();
        BlockPos o = h.absolutePos(new BlockPos(4, 1, 4));
        TankPlants.add(c, FloatingPlantType.DUCKWEED, o.getX() + 1, o.getZ() + 1, 0.5F, 0F);
        t.setWaterQuality(0.6F);
        h.runAfterDelay(20, () -> {
            int light = TankPlants.light(level, c);
            h.assertTrue(light < 9, "dark inside the stone: " + light);
            float size = t.plants().getFirst().size;
            float q = t.waterQuality();
            TankPlants.grow(level, c, 3.0);
            h.assertTrue(t.plants().size() == 1 && t.plants().getFirst().size == size, "no growth in the dark");
            h.assertTrue(t.waterQuality() == q, "no uptake in the dark: " + q + " -> " + t.waterQuality());
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ taking out

    /** Empty hand takes out the plants looked at (else the menu opens as before), net + sneak thins by half, shears do nothing, and the menu button. */
    @GameTest(template = EMPTY)
    public static void takeOutThinAndMenuButton(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = box(h, 4, 1, 4, 4, 1, 4, 16000);
        TankData t = c.getTank();
        BlockPos o = h.absolutePos(new BlockPos(4, 1, 4));
        // 16 salvinia in a grid, one per block
        for (int x = 0; x < 4; x++) for (int z = 0; z < 4; z++) TankPlants.add(c, FloatingPlantType.SALVINIA, o.getX() + x + 0.5, o.getZ() + z + 0.5, 0.5F, 0F);
        TankPlants.add(c, FloatingPlantType.LILY_PAD, o.getX() + 3.5, o.getZ() + 3.2, 1F, 0F);
        h.assertTrue(t.plants().size() == 17, "17 plants: " + t.plants().size());
        BlockPos top = h.absolutePos(new BlockPos(5, 1, 5));
        ServerPlayer sp = lookingDown(h, o.getX() + 1.5, o.getY() + 3, o.getZ() + 1.5);
        try {
            use(sp, h, top, ItemStack.EMPTY);
            h.assertTrue(t.plants().size() == 16, "one plant taken out: " + t.plants().size());
            h.assertTrue(count(sp, item(FloatingPlantType.SALVINIA)) == 1, "into the inventory");
            h.assertTrue(!(sp.containerMenu instanceof TankMenu), "no menu while taking plants out");
            // the spot is empty now: the menu opens as before
            use(sp, h, top, ItemStack.EMPTY);
            h.assertTrue(sp.containerMenu instanceof TankMenu, "menu opens where no plant floats");
            TankMenu menu = (TankMenu) sp.containerMenu;
            // shears (also sneaking) no longer touch the plants
            sp.setShiftKeyDown(true);
            use(sp, h, top, new ItemStack(Items.SHEARS));
            h.assertTrue(t.plants().size() == 16, "shears do not thin out: " + t.plants().size());
            sp.setShiftKeyDown(false);
            use(sp, h, top, new ItemStack(Items.SHEARS));
            h.assertTrue(t.plants().size() == 16, "shears do not take out: " + t.plants().size());
            sp.containerMenu = menu;
            // the aquarium net while sneaking: half of the 15 growing plants (rounded up), the lily pad stays
            sp.setShiftKeyDown(true);
            use(sp, h, top, new ItemStack(ModItems.AQUARIUM_NET.get()));
            h.assertTrue(t.plants().size() == 8, "net + sneak thins to 7 + lily pad: " + t.plants().size());
            h.assertTrue(t.plants().stream().anyMatch(p -> p.type == FloatingPlantType.LILY_PAD), "lily pad stays");
            h.assertTrue(count(sp, item(FloatingPlantType.SALVINIA)) == 9, "8 more items: " + count(sp, item(FloatingPlantType.SALVINIA)));
            sp.setShiftKeyDown(false);
            // the menu button goes the same way (needs the open menu)
            sp.containerMenu = menu;
            TankActions.handle(sp, new TankActionPayload(menu.containerId, TankActions.THIN_PLANTS, 0, "", Optional.empty()));
            h.assertTrue(t.plants().size() == 4, "menu button thins out: " + t.plants().size());
            h.assertTrue(count(sp, item(FloatingPlantType.SALVINIA)) == 13, "items from the button: " + count(sp, item(FloatingPlantType.SALVINIA)));
            // a water bucket still drains / fills as before (plants do not steal other right-clicks)
            long water = t.waterMb();
            sp.setShiftKeyDown(false);
            use(sp, h, top, new ItemStack(Items.BUCKET));
            h.assertTrue(t.waterMb() == water - 1000, "bucket still drains: " + t.waterMb());
        } finally {
            sp.getServer().getPlayerList().remove(sp);
        }
        // breaking the whole tank drops the plants
        int before = t.plants().size();
        for (int x = 4; x < 8; x++) for (int z = 4; z < 8; z++) level.destroyBlock(h.absolutePos(new BlockPos(x, 1, z)), false);
        int drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(o).inflate(6),
                e -> TankPlants.typeOf(e.getItem()) != null).stream().mapToInt(e -> e.getItem().getCount()).sum();
        h.assertTrue(drops == before, "plants drop with the tank: " + drops + " of " + before);
        h.succeed();
    }

    /** A player standing beside the tank at (x, eyeY, z) (absolute), looking in {@code yaw} with {@code pitch}. */
    private static ServerPlayer looking(GameTestHelper h, double x, double eyeY, double z, float yaw, float pitch) {
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        sp.teleportTo(h.getLevel(), x, eyeY - sp.getEyeHeight(), z, yaw, pitch);
        sp.setYRot(yaw);
        sp.setXRot(pitch);
        sp.getInventory().selected = 8;
        return sp;
    }

    private static void useOnFace(ServerPlayer sp, GameTestHelper h, BlockPos abs, Direction face, ItemStack stack) {
        sp.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).relative(face, 0.5), face, abs, false);
        sp.gameMode.useItemOn(sp, h.getLevel(), stack, InteractionHand.MAIN_HAND, hit);
    }

    /**
     * The aquarium net takes plants out from any angle: from the side through the glass (eye below the surface, where
     * the empty hand opens the menu), from above, and nothing where no plant floats (no menu either); sneaking with it
     * thins out by half.
     */
    @GameTest(template = EMPTY)
    public static void netTakesPlantsOutFromAnyAngle(GameTestHelper h) {
        TankBlockEntity c = box(h, 4, 1, 4, 4, 2, 2, 14000);
        TankData t = c.getTank();
        BlockPos o = h.absolutePos(new BlockPos(4, 1, 4));
        // two frogbit near the north glass, one salvinia at the back
        FloatingPlant a = TankPlants.add(c, FloatingPlantType.FROGBIT, o.getX() + 1.5, o.getZ() + 0.5, 1F, 0F);
        FloatingPlant b = TankPlants.add(c, FloatingPlantType.FROGBIT, o.getX() + 3.4, o.getZ() + 0.5, 1F, 0F);
        TankPlants.add(c, FloatingPlantType.SALVINIA, o.getX() + 1.5, o.getZ() + 1.6, 0.6F, 0F);
        h.assertTrue(a != null && b != null && t.plants().size() == 3, "three plants");
        double surface = t.waterSurfaceY();
        h.assertTrue(surface > o.getY() + 1.5, "water up to the second layer: " + surface);
        // north of the tank, eye a bit below the surface, looking south (yaw 0) at the first frogbit through the glass
        ServerPlayer sp = looking(h, o.getX() + 1.5, surface - 0.3, o.getZ() - 1.5, 0F, 0F);
        BlockPos glass = o.above();
        try {
            sp.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            useOnFace(sp, h, glass, Direction.NORTH, ItemStack.EMPTY);
            h.assertTrue(sp.containerMenu instanceof TankMenu && t.plants().size() == 3, "empty hand from the side: the menu as before");
            sp.closeContainer();
            useOnFace(sp, h, glass, Direction.NORTH, new ItemStack(ModItems.AQUARIUM_NET.get()));
            h.assertTrue(!t.plants().contains(a), "the net took out the frogbit seen from the side");
            h.assertTrue(t.plants().size() == 2, "only that one: " + t.plants().size());
            h.assertTrue(count(sp, item(FloatingPlantType.FROGBIT)) == 1, "frogbit into the inventory");
            h.assertTrue(!(sp.containerMenu instanceof TankMenu), "no menu with the net");
            h.assertTrue(sp.getMainHandItem().is(ModItems.AQUARIUM_NET.get()), "the net stays in the hand");
            // the same spot again: at most the salvinia behind comes out, no menu
            useOnFace(sp, h, glass, Direction.NORTH, new ItemStack(ModItems.AQUARIUM_NET.get()));
            h.assertTrue(t.plants().size() >= 1, "the other frogbit stays: " + t.plants().size());
            h.assertTrue(!(sp.containerMenu instanceof TankMenu), "no menu with the net");
        } finally {
            sp.getServer().getPlayerList().remove(sp);
        }
        // from above, looking down at the second frogbit
        ServerPlayer top = lookingDown(h, b.x, o.getY() + 4.5, b.z);
        try {
            use(top, h, h.absolutePos(new BlockPos(7, 2, 4)), new ItemStack(ModItems.AQUARIUM_NET.get()));
            h.assertTrue(!t.plants().contains(b), "the net from above");
            // refill and thin out with net + sneak (lily pad stays)
            for (int i = 0; i < 6; i++) TankPlants.add(c, FloatingPlantType.DUCKWEED, o.getX() + 0.5 + i * 0.5, o.getZ() + 1.0, 0.5F, 0F);
            TankPlants.add(c, FloatingPlantType.LILY_PAD, o.getX() + 3.5, o.getZ() + 1.5, 1F, 0F);
            int growing = (int) t.plants().stream().filter(p -> p.type.grows()).count();
            top.setShiftKeyDown(true);
            use(top, h, h.absolutePos(new BlockPos(7, 2, 4)), new ItemStack(ModItems.AQUARIUM_NET.get()));
            int left = (int) t.plants().stream().filter(p -> p.type.grows()).count();
            h.assertTrue(left == growing / 2, "net + sneak thins by half: " + growing + " -> " + left);
            h.assertTrue(t.plants().stream().anyMatch(p -> p.type == FloatingPlantType.LILY_PAD), "lily pad stays");
            h.assertTrue(!(top.containerMenu instanceof TankMenu), "no menu");
            h.assertTrue(top.level() == h.getLevel(), "sneaking with the net does not enter the interior");
        } finally {
            top.getServer().getPlayerList().remove(top);
        }
        h.succeed();
    }

    /** A locked tank of another player: placing, taking out (hand, net), thinning (net, menu button) do nothing. */
    @GameTest(template = EMPTY)
    public static void lockedTankKeepsItsPlants(GameTestHelper h) {
        TankBlockEntity c = box(h, 4, 1, 4, 3, 1, 3, 9000);
        TankData t = c.getTank();
        BlockPos o = h.absolutePos(new BlockPos(4, 1, 4));
        for (int i = 0; i < 6; i++) TankPlants.add(c, FloatingPlantType.SALVINIA, o.getX() + 0.5 + (i % 3), o.getZ() + 0.5 + (i / 3), 0.6F, 0F);
        t.setOwner(java.util.UUID.randomUUID());
        t.setLocked(true);
        BlockPos top = h.absolutePos(new BlockPos(5, 1, 5));
        ServerPlayer sp = lookingDown(h, o.getX() + 0.5, o.getY() + 3, o.getZ() + 0.5);
        try {
            sp.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            h.assertTrue(!TankInhabitants.canModify(sp, t), "a stranger may not change the tank (no operator)");
            use(sp, h, top, new ItemStack(item(FloatingPlantType.DUCKWEED), 2));
            h.assertTrue(t.plants().size() == 6 && sp.getMainHandItem().getCount() == 2, "no plant placed, item kept");
            use(sp, h, top, ItemStack.EMPTY);
            h.assertTrue(t.plants().size() == 6, "empty hand takes nothing out");
            use(sp, h, top, new ItemStack(ModItems.AQUARIUM_NET.get()));
            h.assertTrue(t.plants().size() == 6, "the net takes nothing out");
            sp.setShiftKeyDown(true);
            use(sp, h, top, new ItemStack(ModItems.AQUARIUM_NET.get()));
            h.assertTrue(t.plants().size() == 6, "the net does not thin out");
            sp.setShiftKeyDown(false);
            h.assertTrue(count(sp, item(FloatingPlantType.SALVINIA)) == 0, "no items for the stranger");
            // the menu button (the menu itself opens for everybody: looking is allowed)
            TankMenu menu = new TankMenu(1234, sp.getInventory(), c);
            sp.containerMenu = menu;
            TankActions.handle(sp, new TankActionPayload(menu.containerId, TankActions.THIN_PLANTS, 0, "", Optional.empty()));
            h.assertTrue(t.plants().size() == 6, "the menu button does not thin out");
            // the owner may
            t.setOwner(sp.getUUID());
            TankActions.handle(sp, new TankActionPayload(menu.containerId, TankActions.THIN_PLANTS, 0, "", Optional.empty()));
            h.assertTrue(t.plants().size() == 3, "the owner thins out: " + t.plants().size());
        } finally {
            sp.getServer().getPlayerList().remove(sp);
        }
        h.succeed();
    }

    /**
     * With floating plants on the tank every other right-click works as before: water buckets fill and drain, a betta
     * bucket and a fish bag put in their fish, pellets feed by hand, an empty hand at a free spot opens the menu; the
     * plants stay as they are.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void otherInteractionsUnchangedWithPlants(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = box(h, 4, 1, 4, 3, 2, 3, 12000);
        TankData t = c.getTank();
        BlockPos o = h.absolutePos(new BlockPos(4, 1, 4));
        for (int i = 0; i < 5; i++) TankPlants.add(c, FloatingPlantType.values()[i % 4], o.getX() + 0.6 + i * 0.45, o.getZ() + 0.6, 0.7F, 0F);
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        for (FloatingPlant p : t.plants()) ids.add(p.id);
        BlockPos top = h.absolutePos(new BlockPos(5, 2, 6));
        // looking down at the back row, where no plant floats
        ServerPlayer sp = lookingDown(h, o.getX() + 1.5, o.getY() + 4.5, o.getZ() + 2.5);
        try {
            sp.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            long w = t.waterMb();
            use(sp, h, top, new ItemStack(Items.WATER_BUCKET));
            h.assertTrue(t.waterMb() == w + 1000 && sp.getMainHandItem().is(Items.BUCKET), "water bucket fills: " + t.waterMb());
            use(sp, h, top, new ItemStack(Items.BUCKET));
            h.assertTrue(t.waterMb() == w && sp.getMainHandItem().is(Items.WATER_BUCKET), "bucket drains: " + t.waterMb());
            var g = org.overengineer.bettacraft.genetics.BettaGenome.randomDomestic(net.minecraft.util.RandomSource.create(5));
            use(sp, h, top, org.overengineer.bettacraft.entity.betta.BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()),
                    org.overengineer.bettacraft.entity.betta.BettaData.create(g, false, 0)));
            h.assertTrue(TankInhabitants.liveInhabitants(level, t).size() == 1, "betta bucket puts the fish in");
            var tropical = net.minecraft.world.entity.EntityType.TROPICAL_FISH.create(level);
            ItemStack bag = org.overengineer.bettacraft.entity.betta.FishBagItem.fillWith(tropical);
            tropical.discard();
            use(sp, h, top, bag);
            h.assertTrue(TankInhabitants.liveInhabitants(level, t).size() == 2, "fish bag puts its fish in");
            use(sp, h, top, new ItemStack(ModItems.BETTA_PELLETS.get(), 3));
            h.assertTrue(sp.getMainHandItem().getCount() == 2, "pellets fed by hand: " + sp.getMainHandItem().getCount());
            use(sp, h, top, ItemStack.EMPTY);
            h.assertTrue(sp.containerMenu instanceof TankMenu, "empty hand at a free spot opens the menu");
            java.util.Set<Integer> after = new java.util.HashSet<>();
            for (FloatingPlant p : t.plants()) after.add(p.id);
            h.assertTrue(after.equals(ids), "plants unchanged: " + ids + " -> " + after);
        } finally {
            sp.getServer().getPlayerList().remove(sp);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ movement

    /** In an L-shaped tank the plants stay over the water, and they drift away from where a filter jet hits. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void plantsStayOverTheWaterAndAvoidTheJet(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        // L: 3x3 without the corner (6, 6), 1 high
        List<BlockPos> l = new ArrayList<>();
        for (int x = 4; x < 7; x++) for (int z = 4; z < 7; z++) if (!(x == 6 && z == 6)) l.add(new BlockPos(x, 1, z));
        TankBlockEntity c = tank(h, l, 8000);
        TankData t = c.getTank();
        h.assertTrue(t.memberCount() == 8, "L tank");
        BlockPos o = h.absolutePos(new BlockPos(4, 1, 4));
        for (int i = 0; i < 10; i++) TankPlants.add(c, FloatingPlantType.values()[i % 4], o.getX() + 2.2 + (i % 3) * 0.2, o.getZ() + 1.8 + i * 0.03, 0.6F, 0F);
        PlantSurface s = TankPlants.surface(t);
        for (int k = 0; k < 400; k++) {
            TankPlants.step(level, c, 0.0);
            for (FloatingPlant p : t.plants()) {
                if (!s.contains(p.x, p.z)) throw new GameTestAssertException("plant over the notch / outside at " + p.x + "/" + p.z);
            }
        }
        // a second tank with a filter on the west wall: plants near the jet move away
        TankBlockEntity f = box(h, 12, 1, 4, 6, 2, 2, 12000);
        TankData ft = f.getTank();
        h.setBlock(new BlockPos(11, 1, 4), ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, Direction.EAST));
        TankEquipment.invalidate(f);
        List<FloatingPlantSim.Jet> jets = TankPlants.jets(level, f);
        h.assertTrue(jets.size() == 1, "one jet: " + jets.size());
        FloatingPlantSim.Jet j = jets.getFirst();
        for (int i = 0; i < 5; i++) TankPlants.add(f, FloatingPlantType.SALVINIA, j.x() + 0.1 + i * 0.05, j.z() + (i - 2) * 0.1, 0.5F, 0F);
        double before = 0, after = 0;
        for (FloatingPlant p : ft.plants()) before += Math.hypot(p.x - j.x(), p.z - j.z());
        for (int k = 0; k < 150; k++) TankPlants.step(level, f, 0.0);
        for (FloatingPlant p : ft.plants()) after += Math.hypot(p.x - j.x(), p.z - j.z());
        before /= ft.plants().size();
        after /= ft.plants().size();
        h.assertTrue(after > before + 0.6 && after > FloatingPlantSim.JET_RANGE * j.strength() * 0.8, "away from the jet: " + before + " -> " + after);
        h.succeed();
    }

    // ------------------------------------------------------------------ nest

    /** The male's nest goes under the plant cluster, not where he swims. */
    @GameTest(template = EMPTY)
    public static void nestUnderThePlantCluster(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = box(h, 4, 1, 4, 4, 2, 4, 32000);
        TankData t = c.getTank();
        BlockPos o = h.absolutePos(new BlockPos(4, 1, 4));
        for (int i = 0; i < 4; i++) TankPlants.add(c, FloatingPlantType.FROGBIT, o.getX() + 3.2 + (i % 2) * 0.3, o.getZ() + 3.2 + (i / 2) * 0.3, 1F, 0F);
        Vec3 male = new Vec3(o.getX() + 0.5, o.getY() + 1.0, o.getZ() + 0.5);
        Vec3 nest = TankBreeding.nestSpot(level, t, male);
        h.assertTrue(Math.hypot(nest.x - (o.getX() + 3.35), nest.z - (o.getZ() + 3.35)) < 0.4, "nest under the frogbit: " + nest);
        h.assertTrue(Math.abs(nest.y - t.waterSurfaceY()) < 0.2, "at the surface: " + nest.y + " vs " + t.waterSurfaceY());
        h.assertTrue(TankPlants.breedingBonus(t) == 1.0 || TankPlants.coverage(t) >= TankPlants.BONUS_FROM, "bonus only from 10 % cover");
        h.succeed();
    }

    // ------------------------------------------------------------------ obtaining

    /** Fishing in a swamp (chance 1 for the test) brings up floating plants; mangrove swamps also the red root floater. */
    @GameTest(template = EMPTY, batch = "bettacraft_plants_fishing")
    public static void fishingInSwampsBringsUpPlants(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        double saved = ServerConfig.PLANTS_FISHING_CHANCE.get();
        try {
            ServerConfig.PLANTS_FISHING_CHANCE.set(1.0);
            var table = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
            java.util.Set<FloatingPlantType> seen = java.util.EnumSet.noneOf(FloatingPlantType.class);
            for (String biome : new String[]{"minecraft:swamp", "minecraft:mangrove_swamp", "minecraft:plains"}) {
                BlockPos at = h.absolutePos(new BlockPos(16, 2, 16));
                var src = level.getServer().createCommandSourceStack().withSuppressedOutput().withLevel(level);
                level.getServer().getCommands().performPrefixedCommand(src, "fillbiome " + (at.getX() - 8) + " " + at.getY() + " " + (at.getZ() - 8) + " "
                        + (at.getX() + 8) + " " + (at.getY() + 8) + " " + (at.getZ() + 8) + " " + biome);
                h.assertTrue(level.getBiome(at).is(net.minecraft.resources.ResourceLocation.parse(biome)), "biome set: " + level.getBiome(at));
                int plants = 0, rolls = 200;
                for (int i = 0, n = rolls; i < n; i++) {
                    LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(at))
                            .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD)).create(LootContextParamSets.FISHING);
                    var loot = table.getRandomItems(params);
                    // M16: other mods replace some catches after us (Critters and Companions: 15 % koi or clams)
                    if (MockPlayers.replacedByOtherMod(loot)) {
                        rolls--;
                        continue;
                    }
                    for (ItemStack s : loot) {
                        FloatingPlantType t = TankPlants.typeOf(s);
                        if (t != null && t != FloatingPlantType.LILY_PAD) { // vanilla junk has lily pads anyway
                            plants++;
                            if (biome.equals("minecraft:swamp") && t == FloatingPlantType.RED_ROOT_FLOATER) {
                                throw new GameTestAssertException("red root floater only in mangrove swamps");
                            }
                            if (!biome.equals("minecraft:plains")) seen.add(t);
                        }
                    }
                }
                if (biome.equals("minecraft:plains")) h.assertTrue(plants == 0, "no plants outside swamps: " + plants);
                else h.assertTrue(plants == rolls, biome + ": every catch a plant at chance 1, got " + plants);
            }
            h.assertTrue(seen.containsAll(List.of(FloatingPlantType.DUCKWEED, FloatingPlantType.SALVINIA, FloatingPlantType.FROGBIT,
                    FloatingPlantType.RED_ROOT_FLOATER)), "all four species: " + seen);
        } finally {
            ServerConfig.PLANTS_FISHING_CHANCE.set(saved);
        }
        h.succeed();
    }

    /** The wandering trader sells frogbit and salvinia, the red root floater among his rare offers. */
    @GameTest(template = EMPTY)
    public static void wanderingTraderSellsPlants(GameTestHelper h) {
        WanderingTrader trader = h.spawn(EntityType.WANDERING_TRADER, new BlockPos(2, 1, 2));
        var rnd = h.getLevel().getRandom();
        java.util.Set<Item> generic = new java.util.HashSet<>(), rare = new java.util.HashSet<>();
        for (int level = 1; level <= 2; level++) {
            VillagerTrades.ItemListing[] listings = VillagerTrades.WANDERING_TRADER_TRADES.get(level);
            h.assertTrue(listings != null, "trade level " + level);
            for (VillagerTrades.ItemListing l : listings) {
                var offer = l.getOffer(trader, rnd);
                if (offer != null) (level == 1 ? generic : rare).add(offer.getResult().getItem());
            }
        }
        h.assertTrue(generic.contains(item(FloatingPlantType.FROGBIT)), "frogbit for sale");
        h.assertTrue(generic.contains(item(FloatingPlantType.SALVINIA)), "salvinia for sale");
        h.assertTrue(rare.contains(item(FloatingPlantType.RED_ROOT_FLOATER)), "red root floater among the rare offers");
        trader.discard();
        h.succeed();
    }

    /** Filling a glass bottle from swamp water still gives swamp water; with chance 1 duckweed comes with it. */
    @GameTest(template = EMPTY, batch = "bettacraft_plants_bottle")
    public static void swampBottleSometimesBringsDuckweed(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos water = h.absolutePos(new BlockPos(20, 1, 20));
        h.setBlock(new BlockPos(20, 1, 20), Blocks.WATER);
        fillSwamp(level, water);
        // the biome change can need a moment (and a second try) right after the template was placed (M13: was flaky)
        h.runAfterDelay(10, () -> {
            if (!level.getBiome(water).is(net.minecraft.world.level.biome.Biomes.SWAMP)) fillSwamp(level, water);
            bottleCheck(h, level, water);
        });
    }

    private static void fillSwamp(ServerLevel level, BlockPos water) {
        // fillbiome does nothing when a chunk of the box is not loaded: load them first
        for (int x = water.getX() - 4; x <= water.getX() + 4; x += 4) for (int z = water.getZ() - 4; z <= water.getZ() + 4; z += 4) level.getChunk(x >> 4, z >> 4);
        var src = level.getServer().createCommandSourceStack().withSuppressedOutput().withLevel(level);
        // a few blocks around the water (the biome lookup is fuzzy across neighbouring 4x4x4 cells, also the cell below), inside the loaded template
        level.getServer().getCommands().performPrefixedCommand(src, "fillbiome " + (water.getX() - 4) + " " + Math.max(level.getMinBuildHeight(), water.getY() - 5) + " " + (water.getZ() - 4) + " "
                + (water.getX() + 4) + " " + (water.getY() + 4) + " " + (water.getZ() + 4) + " minecraft:swamp");
    }

    private static void bottleCheck(GameTestHelper h, ServerLevel level, BlockPos water) {
        double saved = ServerConfig.PLANTS_BOTTLE_CHANCE.get();
        ServerPlayer sp = lookingDown(h, water.getX() + 0.5, water.getY() + 2.6, water.getZ() + 0.5);
        try {
            ServerConfig.PLANTS_BOTTLE_CHANCE.set(1.0);
            sp.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
            h.assertTrue(level.getBiome(water).is(net.minecraft.world.level.biome.Biomes.SWAMP), "swamp: " + level.getBiome(water));
            sp.gameMode.useItem(sp, level, sp.getMainHandItem(), InteractionHand.MAIN_HAND);
            h.assertTrue(count(sp, ModItems.SWAMP_WATER_BOTTLE.get()) == 1, "swamp water as before");
            h.assertTrue(count(sp, item(FloatingPlantType.DUCKWEED)) == 1, "duckweed with it");
        } finally {
            ServerConfig.PLANTS_BOTTLE_CHANCE.set(saved);
            sp.getServer().getPlayerList().remove(sp);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ migration

    /** A controller saved by M11 (schema v6) loads with no floating plants and gets plants like any tank. */
    @GameTest(template = EMPTY)
    public static void m11TankLoadsWithoutPlants(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = box(h, 4, 1, 4, 2, 1, 2, 4000);
        CompoundTag tag = c.saveWithFullMetadata(level.registryAccess());
        tag.putInt("schema", 6);
        CompoundTag tank = tag.getCompound("tank");
        tank.remove("plants");
        tank.remove("next_plant_id");
        c.loadWithComponents(tag, level.registryAccess());
        TankData t = c.getTank();
        h.assertTrue(t.plants().isEmpty(), "no plants after the migration");
        BlockPos o = h.absolutePos(new BlockPos(4, 1, 4));
        FloatingPlant p = TankPlants.add(c, FloatingPlantType.DUCKWEED, o.getX() + 1, o.getZ() + 1, 0.5F, 0F);
        h.assertTrue(p != null && p.id >= 1, "new plants get ids");
        CompoundTag saved = c.saveWithFullMetadata(level.registryAccess());
        h.assertTrue(saved.getInt("schema") == org.overengineer.bettacraft.data.schema.Schemas.TANK_BLOCK_ENTITY.currentVersion(), "saved in the current schema: " + saved.getInt("schema"));
        h.assertTrue(saved.getCompound("tank").getList("plants", 10).size() == 1, "plant saved");
        h.succeed();
    }
}
