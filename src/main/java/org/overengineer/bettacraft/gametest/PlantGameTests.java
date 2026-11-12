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

    /** Empty hand takes out the plants looked at (else the menu opens as before), shears + sneak thin by half, and the menu button. */
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
            // shears while sneaking: half of the 15 growing plants (rounded up), the lily pad stays
            sp.setShiftKeyDown(true);
            use(sp, h, top, new ItemStack(Items.SHEARS));
            h.assertTrue(t.plants().size() == 8, "thinned to 7 + lily pad: " + t.plants().size());
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
                for (int i = 0; i < rolls; i++) {
                    LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(at))
                            .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD)).create(LootContextParamSets.FISHING);
                    for (ItemStack s : table.getRandomItems(params)) {
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
        double saved = ServerConfig.PLANTS_BOTTLE_CHANCE.get();
        BlockPos water = h.absolutePos(new BlockPos(20, 1, 20));
        var src = level.getServer().createCommandSourceStack().withSuppressedOutput().withLevel(level);
        level.getServer().getCommands().performPrefixedCommand(src, "fillbiome " + (water.getX() - 8) + " " + water.getY() + " " + (water.getZ() - 8) + " "
                + (water.getX() + 8) + " " + (water.getY() + 8) + " " + (water.getZ() + 8) + " minecraft:swamp");
        h.setBlock(new BlockPos(20, 1, 20), Blocks.WATER);
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
        h.assertTrue(saved.getInt("schema") == 7, "saved as v7: " + saved.getInt("schema"));
        h.assertTrue(saved.getCompound("tank").getList("plants", 10).size() == 1, "plant saved");
        h.succeed();
    }
}
