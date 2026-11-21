package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.containers.AnimalContainers;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.plants.wild.WildFloatingPlantBlock;
import org.overengineer.bettacraft.plants.wild.WildPlantBlock;
import org.overengineer.bettacraft.plants.wild.WildPlantKind;
import org.overengineer.bettacraft.reef.WildCoralBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M55 client smoke ({@code -Psmoketestm55}, NeoForge and Fabric, background window; parts by
 * {@code -Pm55parts=carpets,plants,icons}, default all; {@code -Pm55tag=<name>} names the carpet shots, e.g. the design):
 * <ul>
 *     <li>carpets: the wild carpets (the four looks, grown, trimmed and a loose patch) in a pool from above, close and
 *     low, each look close up, and a tank on the bank with the same four species planted inside for comparison;</li>
 *     <li>plants: every wild plant of the world (each growth form and look, grown and trimmed, the floating plants and the
 *     wild corals) in a pool, an overview and a close-up per growth form;</li>
 *     <li>icons: every container with an animal of the mod (the tankmate buckets, a critter bucket per species, a filled fish
 *     bag per animal, the herp boxes and turtle buckets, a filled transport tub per big animal) on chest pages.</li>
 * </ul>
 * Screenshots {@code bettacraft_m55_*.png}; log lines "[smoketest] M55 ...".
 */
final class DevClientSmokeM55 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final Set<String> parts;
    private final String tag;
    private int wait;
    private BlockPos home;
    private volatile BlockPos carpetPool, plantPool;
    private final List<ItemStack> icons = new ArrayList<>();
    private int guiScale;

    /** One species per carpet look (mat, clover, fine, needle) and the same species planted in the comparison tank. */
    private static final PlantSpecies[] CARPETS = {PlantSpecies.HC_CUBA, PlantSpecies.MARSILEA, PlantSpecies.STAUROGYNE, PlantSpecies.ELEOCHARIS};

    DevClientSmokeM55() {
        String p = System.getProperty("bettacraft.smoketest.m55parts", "carpets,plants,icons");
        parts = Set.of(p.toLowerCase(Locale.ROOT).split(","));
        String t = System.getProperty("bettacraft.smoketest.m55tag", "");
        tag = t.isEmpty() ? "" : "_" + t;
        LOGGER.info("[smoketest] M55 parts {} tag '{}'", parts, tag);
        then(1, () -> server(this::setup));
        then(20, () -> {
            mc.options.hideGui = true;
            mc.options.fov().set(60);
        });
        if (parts.contains("carpets")) carpets();
        if (parts.contains("plants")) plants();
        if (parts.contains("icons")) icons();
        // back at the comparison tank: the Fabric runner of the ports checks the crack overlay on tank blocks near the player
        if (parts.contains("carpets")) then(20, () -> look(carpetPool, 20.0, 2.4, 0.2, 0F, 38F));
        then(1, () -> mc.options.hideGui = false);
        then(20, () -> LOGGER.info("[smoketest] M55 done"));
    }

    private void setup(ServerPlayer p) {
        ServerLevel level = p.level();
        org.overengineer.bettacraft.gametest.framework.DevTime.set(level, 6000);
        home = p.blockPosition();
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        // clear sight under water (no blue fog over the plants)
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 20 * 60 * 30, 0, false, false));
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.CONDUIT_POWER, 20 * 60 * 30, 0, false, false));
    }

    // ------------------------------------------------------------------ carpets

    private void carpets() {
        then(1, () -> server(this::carpetPool));
        then(200, () -> look(carpetPool, 7.0, 4.2, 0.6, 0F, 52F));
        then(80, () -> shot("carpet_overview" + tag));
        then(1, () -> look(carpetPool, 6.5, 2.1, 1.6, 0F, 58F));
        then(50, () -> shot("carpet_close" + tag));
        then(1, () -> look(carpetPool, 6.5, 0.75, 1.3, 0F, 20F));
        then(50, () -> shot("carpet_low" + tag));
        for (int i = 0; i < CARPETS.length; i++) {
            int k = i;
            then(1, () -> look(carpetPool, 2.5 + k * 3, 0.9, 1.4, 0F, 48F));
            then(40, () -> shot("carpet_look_" + lookName(k) + tag));
        }
        then(1, () -> look(carpetPool, 20.0, 2.4, 0.2, 0F, 38F));
        then(60, () -> shot("carpet_tank" + tag));
    }

    private static String lookName(int k) {
        return WildPlantKind.look(CARPETS[k]);
    }

    private static BlockState carpet(PlantSpecies s, boolean trimmed) {
        return ModBlocks.WILD_PLANTS.get(WildPlantKind.CARPET).get().withSpecies(s).setValue(WildPlantBlock.TRIMMED, trimmed);
    }

    /** A pool 14 x 9 in a river: the four looks grown (3 x 2 each), trimmed behind them, loose patches; a tank on the bank. */
    private void carpetPool(ServerPlayer p) {
        ServerLevel level = p.level();
        BlockPos b = pool(level, home, new Block[]{Blocks.SAND, Blocks.GRAVEL, Blocks.SAND, Blocks.DIRT});
        for (int i = 0; i < CARPETS.length; i++) {
            for (int dx = 0; dx < 3; dx++) for (int dz = 0; dz < 2; dz++) {
                level.setBlock(b.offset(1 + i * 3 + dx, -1, 2 + dz), carpet(CARPETS[i], false), 2);
                level.setBlock(b.offset(1 + i * 3 + dx, -1, 5 + dz), carpet(CARPETS[i], true), 2);
            }
        }
        for (int[] c : new int[][]{{2, 8}, {3, 8}, {4, 8}, {3, 9}, {7, 8}, {8, 8}, {11, 9}, {12, 8}}) {
            level.setBlock(b.offset(c[0], -1, c[1]), carpet(CARPETS[(c[0] / 3) % 4], false), 2);
        }
        carpetPool = b;
        comparisonTank(p, b.offset(19, 1, 2));
        LOGGER.info("[smoketest] M55 carpet pool at {}", b);
    }

    /** A 2 x 1 x 2 tank on the bank with the four carpet species planted in strips. */
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
        long seed = 5500;
        for (int i = 0; i < CARPETS.length; i++) {
            for (int j = 0; j < 3; j++) InteriorPlants.plantAt(in, r, CARPETS[i], 4 + i * 7, 0, 4 + j * 9, CARPETS[i].stages(), seed++);
        }
        c.markTankChanged();
    }

    // ------------------------------------------------------------------ every wild plant

    /** One species per growth form and look of the wild plant blocks (in the order of the species). */
    static Map<String, PlantSpecies> wildLooks() {
        Map<String, PlantSpecies> m = new LinkedHashMap<>();
        for (WildPlantKind k : WildPlantKind.values()) {
            for (PlantSpecies s : PlantSpecies.values()) {
                if (WildPlantKind.of(s) == k) m.putIfAbsent(k.id() + "_" + WildPlantKind.look(s), s);
            }
        }
        return m;
    }

    private void plants() {
        then(1, () -> server(this::plantPool));
        then(200, () -> look(plantPool, 9.0, 5.0, 0.8, 0F, 50F));
        then(80, () -> shot("plants_overview"));
        // a close-up of each row (one growth form per row), then the floating plants and the corals
        for (int row = 0; row < WildPlantKind.values().length + 1; row++) {
            int k = row;
            then(1, () -> look(plantPool, 4.5, 0.75, 1.2 + k * 2, 0F, 26F));
            then(40, () -> shot("plants_row_" + (k < WildPlantKind.values().length ? WildPlantKind.values()[k].id() : "corals")));
            then(1, () -> look(plantPool, 12.5, 0.75, 1.2 + k * 2, 0F, 26F));
            then(40, () -> shot("plants_row_" + (k < WildPlantKind.values().length ? WildPlantKind.values()[k].id() : "corals") + "_b"));
        }
        // close-ups from above: each growth form's first looks (grown and trimmed side by side)
        for (int row = 0; row < WildPlantKind.values().length; row++) {
            int k = row;
            for (int half = 0; half < 2; half++) {
                int h = half;
                then(1, () -> look(plantPool, 2.5 + h * 6, 0.72, 1.6 + k * 2, 0F, 42F));
                then(30, () -> shot("plants_close_" + WildPlantKind.values()[k].id() + "_" + h));
            }
        }
        then(1, () -> look(plantPool, 6.0, 4.0, 12.5, 0F, 50F));
        then(40, () -> shot("plants_floating"));
    }

    /** A pool 18 x 15 : a row per growth form (each look grown and trimmed side by side), a row of wild corals, floating plants. */
    private void plantPool(ServerPlayer p) {
        ServerLevel level = p.level();
        BlockPos b = pool(level, home.offset(40, 0, 0), new Block[]{Blocks.SAND, Blocks.MUD, Blocks.GRAVEL, Blocks.CLAY});
        // rocks for the epiphytes and mosses
        Map<WildPlantKind, Integer> col = new java.util.EnumMap<>(WildPlantKind.class);
        StringBuilder made = new StringBuilder();
        for (var e : wildLooks().entrySet()) {
            PlantSpecies s = e.getValue();
            WildPlantKind k = WildPlantKind.of(s);
            int row = k.ordinal();
            int x = 1 + col.merge(k, 3, Integer::sum) - 3;
            int z = 3 + row * 2;
            WildPlantBlock block = ModBlocks.WILD_PLANTS.get(k).get();
            boolean onStone = k == WildPlantKind.EPIPHYTE || k == WildPlantKind.MOSS && s != PlantSpecies.MARIMO;
            for (int t = 0; t < 2; t++) {
                BlockPos at = b.offset(x + t, -1, z);
                if (onStone) level.setBlock(at.below(), Blocks.COBBLESTONE.defaultBlockState(), 2);
                level.setBlock(at, block.withSpecies(s).setValue(WildPlantBlock.TRIMMED, t == 1), 2);
            }
            made.append(e.getKey()).append(' ');
        }
        int x = 1;
        int z = 3 + WildPlantKind.values().length * 2;
        for (var e : ModBlocks.WILD_CORALS.entrySet()) {
            for (int t = 0; t < 2; t++) {
                BlockState s = e.getValue().get().defaultBlockState();
                if (s.hasProperty(WildCoralBlock.TRIMMED)) s = s.setValue(WildCoralBlock.TRIMMED, t == 1);
                if (s.hasProperty(WildCoralBlock.WATERLOGGED)) s = s.setValue(WildCoralBlock.WATERLOGGED, true);
                level.setBlock(b.offset(x + t, -1, z), s, 2);
            }
            x += 2;
            if (x > 16) break;
        }
        int fx = 1;
        for (FloatingPlantType t : FloatingPlantType.values()) {
            if (!WildFloatingPlantBlock.TYPE.getPossibleValues().contains(t)) continue;
            for (int dx = 0; dx < 2; dx++) {
                level.setBlock(b.offset(fx + dx, 1, 15), ModBlocks.WILD_FLOATING_PLANT.get().defaultBlockState().setValue(WildFloatingPlantBlock.TYPE, t), 2);
            }
            fx += 3;
        }
        plantPool = b;
        LOGGER.info("[smoketest] M55 plant pool at {}: {}", b, made);
    }

    /**
     * A pool (2 blocks of water, 18 x 15) over the start, next to {@code from} (near the start: its chunks are loaded on the client
     * at once; a pool in a far biome showed empty chunks), its bed of the given blocks; returns its corner.
     */
    private static BlockPos pool(ServerLevel level, BlockPos from, Block[] bed) {
        BlockPos b = new BlockPos(from.getX() - 9, from.getY() + 3, from.getZ() + 10); // above the start (a flat world's sea level is below its floor)
        for (int x = -4; x < 24; x++) for (int z = -6; z < 20; z++) {
            for (int y = -4; y < 32; y++) level.setBlock(b.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            boolean water = x >= 0 && x < 18 && z >= 1 && z < 16;
            for (int y = -4; y < -2; y++) level.setBlock(b.offset(x, y, z), Blocks.DIRT.defaultBlockState(), 2);
            if (water) {
                level.setBlock(b.offset(x, -2, z), bed[Math.floorMod(x * 7 + z * 3, bed.length)].defaultBlockState(), 2);
                level.setBlock(b.offset(x, -1, z), Blocks.WATER.defaultBlockState(), 2);
                level.setBlock(b.offset(x, 0, z), Blocks.WATER.defaultBlockState(), 2);
            } else {
                level.setBlock(b.offset(x, -2, z), Blocks.DIRT.defaultBlockState(), 2);
                level.setBlock(b.offset(x, -1, z), Blocks.DIRT.defaultBlockState(), 2);
                level.setBlock(b.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            }
        }
        return b;
    }

    // ------------------------------------------------------------------ icons

    /** Every container with an animal of the mod, in a fixed order (also used by the contact sheet's legend in the log). */
    static List<ItemStack> iconStacks(ServerLevel level) {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(ModItems.BETTA_BUCKET.get()));
        for (var e : ModItems.TANKMATE_BUCKETS.values()) out.add(new ItemStack(e.get()));
        out.add(new ItemStack(ModItems.FROG_BUCKET.get()));
        for (var s : org.overengineer.bettacraft.terrarium.HerpSpecies.ALL) out.add(new ItemStack(ModItems.herpContainer(s)));
        java.util.random.RandomGenerator rnd = new java.util.Random(55);
        for (CritterSpecies s : CritterSpecies.values()) {
            ItemStack bag = TankCritters.bagOf(s, 2, () -> CritterGenes.wild(s.loci().length, 0, rnd), rnd, true);
            out.add(AnimalContainers.critterBucket(bag.get(org.overengineer.bettacraft.registry.ModDataComponents.CRITTERS.get())));
        }
        // the fish bags: a betta, every small tankmate, every small animal
        Entity betta = ModEntities.BETTA.get().create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (betta != null) out.add(FishBagItem.fillWith(betta));
        for (TankmateSpecies s : TankmateSpecies.values()) {
            if (s.isLarge()) continue;
            Entity e = ModEntities.TANKMATES.get(s).get().create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            if (e != null) out.add(FishBagItem.fillWith(e));
        }
        for (CritterSpecies s : CritterSpecies.values()) out.add(TankCritters.bagOf(s, 2, () -> CritterGenes.wild(s.loci().length, 0, rnd), rnd, true));
        // the transport tubs: every big animal
        for (TankmateSpecies s : TankmateSpecies.values()) {
            if (!s.isLarge()) continue;
            Entity e = ModEntities.TANKMATES.get(s).get().create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            if (e != null) out.add(org.overengineer.bettacraft.big.TransportTubItem.fillWith(e));
        }
        return out;
    }

    private void icons() {
        then(1, () -> server(p -> {
            icons.clear();
            icons.addAll(iconStacks(p.level()));
            LOGGER.info("[smoketest] M55 {} icon stacks", icons.size());
        }));
        then(20, () -> {
            mc.options.hideGui = false;
            guiScale = mc.options.guiScale().get();
            mc.options.guiScale().set(4);
            mc.resizeGui();
        });
        for (int page = 0; page < 5; page++) {
            int pg = page;
            then(1, () -> server(p -> openPage(p, pg)));
            then(40, () -> {
                if (pg * 54 < icons.size()) shot("icons_" + pg);
            });
            then(1, () -> server(ServerPlayer::closeContainer));
        }
        then(10, () -> {
            mc.options.guiScale().set(guiScale);
            mc.resizeGui();
        });
    }

    private void openPage(ServerPlayer p, int page) {
        if (page * 54 >= icons.size()) return;
        SimpleContainer c = new SimpleContainer(54);
        StringBuilder legend = new StringBuilder();
        for (int i = 0; i < 54 && page * 54 + i < icons.size(); i++) {
            ItemStack s = icons.get(page * 54 + i);
            c.setItem(i, s.copy());
            legend.append(i).append('=').append(s.getHoverName().getString()).append("; ");
        }
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> ChestMenu.sixRows(id, inv, c), Component.literal("M55 icons " + (page + 1))));
        LOGGER.info("[smoketest] M55 icons page {}: {}", page, legend);
    }

    // ------------------------------------------------------------------ steps

    private void look(BlockPos b, double dx, double eyeDy, double dz, float yaw, float pitch) {
        if (b == null) return;
        server(p -> p.teleportTo(p.level(), b.getX() + dx, b.getY() + eyeDy - p.getEyeHeight(), b.getZ() + dz, java.util.Set.of(), yaw, pitch, true));
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
        Step st = steps.peek();
        if (wait < st.delay()) {
            wait++;
            return false;
        }
        steps.poll();
        wait = 0;
        st.action().run();
        return steps.isEmpty();
    }

    private void server(Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        mc.getToastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m55_" + name + ".png", mc.getMainRenderTarget(), 1, c -> {});
    }
}
