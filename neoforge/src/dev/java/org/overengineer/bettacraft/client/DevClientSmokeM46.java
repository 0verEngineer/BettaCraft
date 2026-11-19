package org.overengineer.bettacraft.client;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.plants.wild.WildPlantBlock;
import org.overengineer.bettacraft.plants.wild.WildPlantKind;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.terrarium.HerpEntity;
import org.overengineer.bettacraft.terrarium.HerpSpecies;
import org.overengineer.bettacraft.world.WildSpawnPass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M46 ({@code -Psmoketestm46}; background window). Part 1: the heat mat item lying flat (hotbar and first-person hand, third
 * person, an item frame on a wall and one on the floor, a dropped mat, the inventory with JEI's list filtered to it, JEI's
 * recipe). Part 2: the wild carpets (all four looks, grown and trimmed) in a pool in a river, a swamp and a jungle biome, with
 * the interior carpets of a small tank on the river bank for comparison. Part 3: the leopard gecko (the three morphs, adult and
 * young) from eight directions, one adult close up from eight directions, the other herps and the turtles from four.
 * Screenshots {@code run/screenshots/bettacraft_m46_*.png}.
 */
final class DevClientSmokeM46 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private int guiScale;
    private volatile Vec3 critterAt;
    private BlockPos home, wall, herps, others;
    private final List<BlockPos> pools = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static final String[] WATERS = {"river", "swamp", "jungle"};

    /** {@code -Pm46carpets=<tag>}: only the carpet pools, the screenshots named with the tag (to compare carpet variants). */
    private static final String CARPETS_ONLY = System.getProperty("bettacraft.smoketest.m46carpets", "");

    DevClientSmokeM46() {
        then(1, () -> server(this::setup));
        if (!CARPETS_ONLY.isEmpty()) {
            then(1, () -> mc.options.hideGui = true);
            carpets("_" + CARPETS_ONLY, false);
            return;
        }
        then(1, () -> mc.options.hideGui = false);
        // part 1: the heat mat
        then(120, () -> server(this::heatMat));
        then(60, () -> lookAt(wall.getX() + 0.5, wall.getY() + 1.0, wall.getZ() - 2.2, 0F, 12F));
        then(60, () -> shot("icon_hand"));
        then(1, () -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        then(30, () -> shot("icon_third_person"));
        then(1, () -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        then(1, () -> mc.options.hideGui = true);
        then(1, () -> lookAt(wall.getX() + 1.5, wall.getY() + 1.4, wall.getZ() - 1.6, 0F, 28F));
        then(40, () -> shot("icon_frames"));
        then(1, () -> mc.options.hideGui = false);
        then(1, () -> server(p -> p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL)));
        then(10, () -> {
            guiScale = mc.options.guiScale().get();
            mc.options.guiScale().set(4);
            mc.resizeGui();
            if (mc.player != null) mc.setScreen(new InventoryScreen(mc.player));
        });
        then(20, () -> jeiFilter("heat mat"));
        then(30, () -> shot("icon_inventory"));
        then(1, () -> jei(new ItemStack(ModItems.HEAT_MAT.get())));
        then(30, () -> shot("icon_jei"));
        then(1, () -> {
            jeiFilter("");
            mc.setScreen(null);
            mc.options.guiScale().set(guiScale);
            mc.resizeGui();
        });
        then(1, () -> server(p -> {
            p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
        }));
        then(1, () -> mc.options.hideGui = true);
        // part 2: the wild carpets in three waters
        carpets("", true);
        // part 3: the herps
        then(1, () -> server(this::herps));
        then(120, () -> {});
        orbit("gecko_morphs", () -> new Vec3(herps.getX() + 0.5, herps.getY(), herps.getZ() + 0.5), 1.25, 0.6, 8);
        orbit("gecko_close", () -> new Vec3(herps.getX() + 0.5, herps.getY(), herps.getZ() - 3.5), 0.7, 0.32, 8);
        orbit("herps", () -> new Vec3(others.getX() + 0.5, others.getY(), others.getZ() + 0.5), 3.6, 1.8, 4);
        then(1, () -> lookAt(home.getX() + 0.5, home.getY() + 2, home.getZ() + 0.5, 0F, 20F));
        then(10, () -> {});
    }

    /** The pools of part 2 (with {@code extras} also the comparison tank and the small animals at the river bank). */
    private void carpets(String tag, boolean extras) {
        for (int i = 0; i < WATERS.length; i++) {
            int k = i;
            then(1, () -> server(p -> pool(p, k)));
            then(1, () -> {});
            then(200, () -> {
                BlockPos b = pools.get(k);
                lookAt(b.getX() + 7.0, b.getY() + 3.2, b.getZ() - 1.2, 0F, 38F);
            });
            then(100, () -> shot("carpet_" + WATERS[k] + tag));
            then(1, () -> {
                BlockPos b = pools.get(k);
                lookAt(b.getX() + 4.5, b.getY() + 1.9, b.getZ() + 1.6, 0F, 50F);
            });
            then(60, () -> shot("carpet_" + WATERS[k] + "_close" + tag));
            if (i == 0 && extras) {
                then(1, () -> {
                    BlockPos b = pools.get(0);
                    lookAt(b.getX() + 16.0, b.getY() + 2.2, b.getZ() + 0.4, 0F, 40F);
                });
                then(60, () -> shot("carpet_tank"));
                // the small animals of the spawn pass at the bank (part 5)
                then(1, () -> server(this::critters));
                then(1, () -> {
                    BlockPos b = pools.get(0);
                    lookAt(b.getX() + 7.0, b.getY() + 1.6, b.getZ() + 0.2, 0F, 50F);
                });
                then(80, () -> shot("critters_bank"));
                then(1, () -> {
                    Vec3 c = critterAt;
                    if (c != null) lookAt(c.x, c.y + 1.0, c.z - 0.75, 0F, 52F);
                });
                then(40, () -> shot("critters_bank_close"));
            }
        }
    }

    // ------------------------------------------------------------------ setup

    private void setup(ServerPlayer p) {
        ServerLevel level = p.level();
        org.overengineer.bettacraft.gametest.framework.DevTime.set(level, 6000);
        level.getGameRules().set(GameRules.ADVANCE_TIME, false, level.getServer());
        level.getGameRules().set(GameRules.ADVANCE_WEATHER, false, level.getServer());
        level.getServer().setWeatherParameters(12000, 0, false, false);
        level.getGameRules().set(GameRules.SPAWN_MOBS, false, level.getServer());
        home = p.blockPosition().offset(0, 40, 0);
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        LOGGER.info("[m46] home {}", home);
    }

    private static void platform(ServerLevel level, BlockPos min, int sx, int sz, Block block) {
        for (int x = 0; x < sx; x++) for (int z = 0; z < sz; z++) {
            level.setBlock(min.offset(x, -1, z), block.defaultBlockState(), 2);
            for (int y = 0; y < 5; y++) level.setBlock(min.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
    }

    // ------------------------------------------------------------------ part 1: the heat mat

    private void heatMat(ServerPlayer p) {
        ServerLevel level = p.level();
        wall = home.offset(0, 0, 0);
        platform(level, wall.offset(-4, 0, -4), 10, 8, Blocks.SMOOTH_STONE);
        for (int x = -2; x <= 3; x++) for (int y = 0; y < 3; y++) level.setBlock(wall.offset(x, y, 1), Blocks.STONE_BRICKS.defaultBlockState(), 2);
        ItemStack mat = new ItemStack(ModItems.HEAT_MAT.get());
        // a frame on the wall (facing the viewer) and one on the floor
        ItemFrame onWall = new ItemFrame(level, wall.offset(1, 1, 0), Direction.NORTH);
        onWall.setItem(mat.copy(), false);
        level.addFreshEntity(onWall);
        ItemFrame onFloor = new ItemFrame(level, wall.offset(2, 0, -1), Direction.UP);
        onFloor.setItem(mat.copy(), false);
        level.addFreshEntity(onFloor);
        ItemEntity dropped = new ItemEntity(level, wall.getX() + 0.5, wall.getY() + 0.05, wall.getZ() - 0.5, mat.copy(), 0, 0, 0);
        dropped.setPickUpDelay(32767);
        dropped.setNoGravity(true);
        level.addFreshEntity(dropped);
        // the hotbar: two mats, a basking lamp, a vanilla carpet (lies flat too) and the mister; the first mat in the hand
        var inv = p.getInventory();
        inv.clearContent();
        inv.setItem(0, mat.copy());
        inv.setItem(1, new ItemStack(ModItems.HEAT_MAT.get(), 16));
        inv.setItem(2, new ItemStack(Items.MOSS_CARPET));
        inv.setItem(3, new ItemStack(Items.WHITE_CARPET));
        inv.setItem(4, new ItemStack(ModBlocks.BASKING_LAMP.get()));
        inv.setItem(9, new ItemStack(ModItems.HEAT_MAT.get(), 4));
        inv.setSelectedSlot(0);
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket(0));
        p.inventoryMenu.broadcastChanges();
        LOGGER.info("[m46] heat mat scene at {}", wall);
    }

    private void jeiFilter(String text) {
        try {
            mezz.jei.api.runtime.IJeiRuntime rt = jeiRuntime();
            if (rt != null) rt.getIngredientFilter().setFilterText(text);
        } catch (ReflectiveOperationException | LinkageError e) {
            LOGGER.warn("[m46] JEI filter: {}", e.toString());
        }
    }

    private void jei(ItemStack stack) {
        try {
            mezz.jei.api.runtime.IJeiRuntime rt = jeiRuntime();
            if (rt == null) {
                LOGGER.warn("[m46] JEI has no runtime");
                return;
            }
            var focus = rt.getJeiHelpers().getFocusFactory().createFocus(mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT,
                    mezz.jei.api.constants.VanillaTypes.ITEM_STACK, stack);
            rt.getRecipesGui().show(focus);
        } catch (ReflectiveOperationException | LinkageError e) {
            LOGGER.warn("[m46] JEI: {}", e.toString());
        }
    }

    private static mezz.jei.api.runtime.IJeiRuntime jeiRuntime() throws ReflectiveOperationException {
        if (!org.overengineer.bettacraft.platform.Platform.get().isModLoaded("jei")) return null;
        var f = org.overengineer.bettacraft.compat.jei.BettaCraftJeiPlugin.class.getDeclaredField("runtime");
        f.setAccessible(true);
        return (mezz.jei.api.runtime.IJeiRuntime) f.get(null);
    }

    // ------------------------------------------------------------------ part 2: the wild carpets

    private static final PlantSpecies[] LOOKS = {PlantSpecies.HC_CUBA, PlantSpecies.MARSILEA, PlantSpecies.STAUROGYNE, PlantSpecies.ELEOCHARIS};

    private static BlockState carpet(PlantSpecies s, boolean trimmed) {
        WildPlantBlock b = ModBlocks.WILD_PLANTS.get(WildPlantKind.CARPET).get();
        return b.withSpecies(s).setValue(WildPlantBlock.TRIMMED, trimmed);
    }

    /** A pool 14 x 9 in the nearest biome of the kind {@code k}: a bank in the north, two blocks of water, carpet patches on the bed. */
    private void pool(ServerPlayer p, int k) {
        ServerLevel level = p.level();
        ResourceKey<Biome> biome = k == 0 ? Biomes.RIVER : k == 1 ? Biomes.SWAMP : Biomes.JUNGLE;
        BlockPos from = home.offset(k * 600, 0, 0);
        Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(h -> h.is(biome), from.atY(62), 6400, 32, 64);
        BlockPos at = found == null ? from.offset(0, 0, 200) : found.getFirst();
        LOGGER.info("[m46] {} biome at {} (found {})", WATERS[k], at, found != null);
        BlockPos b = new BlockPos(at.getX() - 7, level.getSeaLevel() - 1, at.getZ() - 4);
        Block bank = k == 0 ? Blocks.GRASS_BLOCK : k == 1 ? Blocks.GRASS_BLOCK : Blocks.PODZOL;
        Block[] bed = k == 0 ? new Block[]{Blocks.SAND, Blocks.GRAVEL, Blocks.SAND, Blocks.STONE}
                : k == 1 ? new Block[]{Blocks.MUD, Blocks.CLAY, Blocks.MUD, Blocks.DIRT} : new Block[]{Blocks.SAND, Blocks.COARSE_DIRT, Blocks.GRAVEL, Blocks.SAND};
        for (int x = -4; x < 22; x++) for (int z = -6; z < 14; z++) {
            for (int y = -4; y < 16; y++) level.setBlock(b.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            boolean water = x >= 0 && x < 14 && z >= 1 && z < 10;
            for (int y = -4; y < -2; y++) level.setBlock(b.offset(x, y, z), Blocks.DIRT.defaultBlockState(), 2);
            if (water) {
                level.setBlock(b.offset(x, -2, z), bed[Math.floorMod(x * 7 + z * 3, bed.length)].defaultBlockState(), 2);
                level.setBlock(b.offset(x, -1, z), Blocks.WATER.defaultBlockState(), 2);
                level.setBlock(b.offset(x, 0, z), Blocks.WATER.defaultBlockState(), 2);
            } else {
                level.setBlock(b.offset(x, -2, z), Blocks.DIRT.defaultBlockState(), 2);
                level.setBlock(b.offset(x, -1, z), Blocks.DIRT.defaultBlockState(), 2);
                level.setBlock(b.offset(x, 0, z), bank.defaultBlockState(), 2);
            }
        }
        // patches on the bed: the four looks grown (2 x 3 each), the same trimmed behind them, a lone carpet, a loose edge
        for (int i = 0; i < LOOKS.length; i++) {
            for (int dx = 0; dx < 3; dx++) for (int dz = 0; dz < 2; dz++) {
                level.setBlock(b.offset(1 + i * 3 + dx, -1, 2 + dz), carpet(LOOKS[i], false), 2);
                level.setBlock(b.offset(1 + i * 3 + dx, -1, 5 + dz), carpet(LOOKS[i], true), 2);
            }
        }
        for (int[] c : new int[][]{{2, 8}, {3, 8}, {4, 8}, {3, 9}, {7, 8}, {8, 8}, {11, 9}}) level.setBlock(b.offset(c[0], -1, c[1]), carpet(LOOKS[(c[0] + c[1]) % 4], false), 2);
        if (k == 1) for (int[] c : new int[][]{{1, 1}, {5, 4}, {12, 7}}) level.setBlock(b.offset(c[0], 1, c[1]), Blocks.LILY_PAD.defaultBlockState(), 2);
        pools.add(b);
        if (k == 0) comparisonTank(p, b.offset(15, 1, 2));
        p.teleportTo(level, b.getX() + 7, b.getY() + 3, b.getZ() - 2, java.util.Set.of(), 0F, 38F, false);
    }

    /**
     * Part 5: the small animals' own try of the spawn pass in the river pool: shallow columns by the bank are picked
     * ({@link WildSpawnPass#pickCritterColumn}), groups spawn there ({@link WildSpawnPass#spawnCritters}), frozen for the shot.
     */
    private void critters(ServerPlayer p) {
        ServerLevel level = p.level();
        BlockPos b = pools.get(0);
        net.minecraft.util.RandomSource r = net.minecraft.util.RandomSource.create(4646);
        int made = 0, room = WildSpawnPass.critterRoom(level, b);
        for (int i = 0; i < 12 && made < 10; i++) {
            List<BlockPos> cols = new ArrayList<>();
            for (int k = 0; k < 4; k++) {
                BlockPos s = WildSpawnPass.surface(level, b.getX() + 1 + r.nextInt(12), b.getZ() + 1 + r.nextInt(2));
                if (s != null) cols.add(s);
            }
            BlockPos col = WildSpawnPass.pickCritterColumn(level, cols, r);
            if (col != null) made += WildSpawnPass.spawnCritters(level, col, r, Math.min(room - made, 10 - made));
        }
        StringBuilder who = new StringBuilder();
        for (var e : level.getEntitiesOfClass(org.overengineer.bettacraft.entity.critter.WildCritterEntity.class, new net.minecraft.world.phys.AABB(b).inflate(16))) {
            SmokeFreeze.freeze(e, false);
            if (critterAt == null) critterAt = e.position();
            who.append(e.species().id()).append(' ');
        }
        LOGGER.info("[m46] critters at the river bank: {} spawned (room {}): {}", made, room, who);
        if (made == 0) LOGGER.error("[smoketest] M46 FAILED: no small animals at the river bank");
    }

    /** A 2 x 1 x 2 tank on the river bank with the interior carpets (Hemianthus, Marsilea, Staurogyne) for comparison. */
    private static void comparisonTank(ServerPlayer p, BlockPos min) {
        ServerLevel level = p.level();
        List<BlockPos> blocks = new ArrayList<>();
        for (int x = 0; x < 2; x++) for (int z = 0; z < 2; z++) blocks.add(min.offset(x, 0, z));
        for (BlockPos pos : blocks) level.setBlock(pos, ModBlocks.TANK.get().defaultBlockState(), 2);
        TankData t = TankMultiblock.formAll(level, blocks);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        if (c == null) return;
        t.setFront(Direction.NORTH);
        t.setSignVisible(false);
        t.setOwner(p.getUUID());
        org.overengineer.bettacraft.tank.TankInteractions.changeWater(level, c, t.capacityMb());
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) {
            Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.SAND.defaultBlockState(), false);
        }
        long seed = 4600;
        PlantSpecies[] sp = {PlantSpecies.HC_CUBA, PlantSpecies.MARSILEA, PlantSpecies.STAUROGYNE, PlantSpecies.MONTE_CARLO};
        for (int i = 0; i < sp.length; i++) {
            for (int j = 0; j < 3; j++) InteriorPlants.plantAt(in, r, sp[i], 4 + i * 7, 0, 4 + j * 9, sp[i].stages(), seed++);
        }
        c.markTankChanged();
    }

    // ------------------------------------------------------------------ part 3: the herps

    private void herps(ServerPlayer p) {
        ServerLevel level = p.level();
        herps = home.offset(40, 0, 0);
        platform(level, herps.offset(-5, 0, -5), 11, 11, Blocks.SMOOTH_STONE);
        // the leopard gecko: normal, tangerine, albino; adults in front, the young behind them
        var type = ModEntities.HERPS.get(HerpSpecies.LEOPARD_GECKO).get();
        for (int v = 0; v < 3; v++) {
            herp(level, type, v, false, herps.getX() + 0.5 + (v - 1) * 0.5, herps.getY(), herps.getZ() + 0.65, 0F);
            herp(level, type, v, true, herps.getX() + 0.5 + (v - 1) * 0.5, herps.getY(), herps.getZ() + 0.0, 0F);
        }
        herp(level, type, 0, false, herps.getX() + 0.5, herps.getY(), herps.getZ() - 3.5, 0F); // alone, for the close-up
        others = home.offset(60, 0, 0);
        platform(level, others.offset(-6, 0, -6), 13, 13, Blocks.SMOOTH_STONE);
        List<Object[]> line = new ArrayList<>();
        for (int v = 0; v < 3; v++) line.add(new Object[]{ModEntities.HERPS.get(HerpSpecies.DART_FROG).get(), v});
        for (int v = 0; v < 2; v++) line.add(new Object[]{ModEntities.HERPS.get(HerpSpecies.HORNED_FROG).get(), v});
        for (int v = 0; v < 3; v++) line.add(new Object[]{ModEntities.HERPS.get(HerpSpecies.CRESTED_GECKO).get(), v});
        for (int v = 0; v < 3; v++) line.add(new Object[]{ModEntities.HERPS.get(HerpSpecies.CORN_SNAKE).get(), v});
        line.add(new Object[]{ModEntities.SLIDER_TURTLE.get(), 0});
        line.add(new Object[]{ModEntities.SLIDER_TURTLE.get(), 1});
        line.add(new Object[]{ModEntities.HERMANN_TORTOISE.get(), 0});
        line.add(new Object[]{ModEntities.REEVES_TURTLE.get(), 0});
        line.add(new Object[]{ModEntities.MUSK_TURTLE.get(), 0});
        for (int i = 0; i < line.size(); i++) {
            int col = i % 5, row = i / 5;
            herp(level, (EntityType<?>) line.get(i)[0], (Integer) line.get(i)[1], false,
                    others.getX() + 0.5 + (col - 2) * 0.9, others.getY(), others.getZ() + 0.5 + (row - 1.5) * 0.9, 0F);
        }
        LOGGER.info("[m46] herps at {}, others at {}", herps, others);
    }

    private static void herp(ServerLevel level, EntityType<?> type, int variety, boolean young, double x, double y, double z, float yaw) {
        if (!(type.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED) instanceof net.minecraft.world.entity.Mob m)) return;
        m.snapTo(x, y, z, yaw, 0F);
        m.setYHeadRot(yaw);
        m.setYBodyRot(yaw);
        if (m instanceof HerpEntity h) h.setVariety(variety);
        if (young && m instanceof HerpEntity h) h.makeHatchling();
        m.setPersistenceRequired();
        SmokeFreeze.freeze(m, false);
        level.addFreshEntity(m);
    }

    /** {@code n} screenshots around the centre at {@code radius}, the eye {@code height} over the floor, looking at the middle. */
    private void orbit(String name, java.util.function.Supplier<Vec3> centre, double radius, double height, int n) {
        for (int i = 0; i < n; i++) {
            float yaw = i * 360F / n;
            double rad = Math.toRadians(yaw);
            float pitch = (float) Math.toDegrees(Math.atan2(height - 0.06, radius));
            int k = i;
            then(1, () -> {
                Vec3 c = centre.get();
                lookAt(c.x + radius * Math.sin(rad), c.y + height, c.z - radius * Math.cos(rad), yaw, pitch);
            });
            then(25, () -> shot(name + "_" + k));
        }
    }

    // ------------------------------------------------------------------ steps

    private void lookAt(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.level().getServer().overworld(), x, eyeY - p.getEyeHeight(), z, java.util.Set.of(), yaw, pitch, true));
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    boolean tick() {
        if (mc.player != null && !steps.isEmpty()) {
            mc.player.getAbilities().flying = true;
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (steps.isEmpty()) return true;
        Step s = steps.peek();
        if (wait < s.delay()) {
            wait++;
            return false;
        }
        steps.poll();
        wait = 0;
        s.action().run();
        return steps.isEmpty();
    }

    private void server(Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        mc.getToastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m46_" + name + ".png", mc.getMainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
