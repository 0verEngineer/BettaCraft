package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.client.screen.TankScreen;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.plants.InteriorPlant;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.plants.TerrariumPlantLife;
import org.overengineer.bettacraft.plants.wild.WildLandPlantBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tank.menu.TankMenu;
import org.overengineer.bettacraft.terrarium.Climate;
import org.overengineer.bettacraft.terrarium.HerpEntity;
import org.overengineer.bettacraft.terrarium.HerpSpecies;
import org.overengineer.bettacraft.terrarium.TankClimate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M56 client smoke ({@code -Psmoketestm56}, NeoForge and Fabric, background window; parts by {@code -Pm56parts=tanks,world,menu},
 * default all):
 * <ul>
 *     <li>tanks: a rainforest terrarium (bromeliads with a dart frog tadpole in one cup, ferns, mosses, orchids on a branch), a
 *     swamp paludarium (carnivorous plants, sedges, papyrus on the bank, an emersed sword plant) and a desert terrarium (cacti,
 *     succulents, grasses, a tortoise and a corn snake), each grown full, from the front and the side, and from inside;</li>
 *     <li>world: every terrarium plant planted as decoration in the world (the trimmed look behind it), in rows by habitat;</li>
 *     <li>menu: every tab of the tank menu in English and German at the GUI scale of the window and at the smallest one (1); the
 *     buttons and fields whose text does not fit are logged ("[smoketest] M56 menu fit ...").</li>
 * </ul>
 * Screenshots {@code bettacraft_m56_*.png}; log lines "[smoketest] M56 ...".
 */
final class DevClientSmokeM56 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final Set<String> parts;
    private int wait;
    private BooleanSupplier until;
    private BlockPos home;
    private final BlockPos[] tanks = new BlockPos[3];
    private volatile BlockPos garden;
    private int guiScale;
    private int menuProblems;

    private static final String[] NAMES = {"tropical", "swamp", "dry"};

    DevClientSmokeM56() {
        String p = System.getProperty("bettacraft.smoketest.m56parts", "tanks,world,menu");
        parts = Set.of(p.toLowerCase(Locale.ROOT).split(","));
        LOGGER.info("[smoketest] M56 parts {}", parts);
        then(1, () -> server(this::setup));
        then(20, () -> {
            mc.options.hideGui = true;
            mc.options.fov().set(60);
        });
        if (parts.contains("tanks") || parts.contains("menu")) then(1, () -> server(this::buildTanks));
        if (parts.contains("tanks")) tanks();
        if (parts.contains("world")) world();
        if (parts.contains("menu")) menu();
        then(1, () -> mc.options.hideGui = false);
        then(20, () -> LOGGER.info("[smoketest] M56 done: {} menu problem(s)", menuProblems));
    }

    private void setup(ServerPlayer p) {
        ServerLevel level = p.level();
        org.overengineer.bettacraft.gametest.framework.DevTime.set(level, 6000);
        home = p.blockPosition();
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
    }

    // ------------------------------------------------------------------ the three tanks

    /** A 3 x 2 x 2 tank (interior 48 x 32 x 32) at {@code min} facing north, a lit light bar on its back top row. */
    private static TankBlockEntity tank(ServerPlayer p, BlockPos min) {
        ServerLevel level = p.level();
        List<BlockPos> blocks = new ArrayList<>();
        for (int x = 0; x < 3; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) blocks.add(min.offset(x, y, z));
        for (int x = -2; x < 5; x++) for (int z = -4; z < 4; z++) {
            level.setBlock(min.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 2);
            for (int y = 0; y < 5; y++) level.setBlock(min.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
        for (BlockPos pos : blocks) level.setBlock(pos, ModBlocks.TANK.get().defaultBlockState(), 2);
        TankData t = TankMultiblock.formAll(level, blocks);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        if (c == null) {
            LOGGER.error("[smoketest] M56 tank at {} did not form", min);
            return null;
        }
        t.setFront(Direction.NORTH);
        t.setSignVisible(false);
        t.setOwner(p.getUUID());
        for (int x = 0; x < 3; x++) {
            level.setBlock(min.offset(x, 2, 1), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 2).setValue(TankLightBlock.LIT, true), 2);
        }
        return c;
    }

    private static void floor(ServerLevel in, InteriorRegion r, Block block) {
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) {
            Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), block.defaultBlockState(), false);
        }
    }

    /** The ground of a plant at (x, z): on the first free block of its column. */
    private static InteriorPlant plant(ServerLevel in, InteriorRegion r, PlantSpecies s, int x, int z, long seed) {
        int y = 0;
        while (y < r.sizeY() - 1 && !in.getBlockState(r.toWorld(x, y, z)).isAir()) y++;
        InteriorPlant p = InteriorPlants.plantAt(in, r, s, x, y, z, s.stages(), seed);
        if (p == null) LOGGER.error("[smoketest] M56 {} not planted", s.id());
        return p;
    }

    private static void box(ServerLevel in, InteriorRegion r, int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) in.setBlock(r.toWorld(x, y, z), s, 2);
    }

    private void buildTanks(ServerPlayer p) {
        ServerLevel level = p.level();
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        BlockPos base = home.offset(-6, 0, 8);
        for (int i = 0; i < 3; i++) tanks[i] = base.offset(i * 7, 0, 0);
        long seed = 5600;
        // the rainforest terrarium: soil, a mossy rise at the back, a branch for the epiphytes
        TankBlockEntity a = tank(p, tanks[0]);
        if (a != null) {
            InteriorRegion r = InteriorManager.ensureRegion(level, a);
            floor(in, r, Blocks.DIRT);
            box(in, r, 0, 0, 22, 47, 2, 31, Blocks.DIRT.defaultBlockState());
            box(in, r, 0, 3, 22, 47, 3, 31, Blocks.MOSS_BLOCK.defaultBlockState());
            box(in, r, 30, 0, 12, 44, 2, 14, Blocks.OAK_LOG.defaultBlockState());
            PlantSpecies[] front = {PlantSpecies.NEOREGELIA, PlantSpecies.GUZMANIA, PlantSpecies.VRIESEA, PlantSpecies.FITTONIA, PlantSpecies.SELAGINELLA,
                    PlantSpecies.PEPEROMIA, PlantSpecies.CUSHION_MOSS, PlantSpecies.BEGONIA};
            for (int k = 0; k < front.length; k++) plant(in, r, front[k], 4 + (k % 4) * 7, k < 4 ? 6 : 15, seed++);
            PlantSpecies[] back = {PlantSpecies.BOSTON_FERN, PlantSpecies.BIRDS_NEST_FERN, PlantSpecies.POTHOS, PlantSpecies.REINDEER_LICHEN};
            for (int k = 0; k < back.length; k++) plant(in, r, back[k], 6 + k * 11, 26, seed++);
            plant(in, r, PlantSpecies.MINI_ORCHID, 32, 13, seed++);
            plant(in, r, PlantSpecies.TILLANDSIA, 38, 13, seed++);
            plant(in, r, PlantSpecies.MARCGRAVIA, 43, 13, seed++);
            a.getTank().setClimate(new Climate.State(25, 25, 25, 90, 0, 0));
        }
        // the swamp paludarium: water at the front, a bank at the back
        TankBlockEntity b = tank(p, tanks[1]);
        if (b != null) {
            InteriorRegion r = InteriorManager.ensureRegion(level, b);
            floor(in, r, Blocks.SAND);
            box(in, r, 0, 0, 14, 47, 9, 31, Blocks.DIRT.defaultBlockState());
            box(in, r, 0, 10, 14, 47, 10, 31, Blocks.MUD.defaultBlockState());
            TankInteractions.changeWater(level, b, 1600);
            PlantSpecies[] bank = {PlantSpecies.SUNDEW, PlantSpecies.VENUS_FLYTRAP, PlantSpecies.BUTTERWORT, PlantSpecies.SARRACENIA, PlantSpecies.NEPENTHES,
                    PlantSpecies.SWAMP_SEDGE, PlantSpecies.SOFT_RUSH, PlantSpecies.PAPYRUS, PlantSpecies.UMBRELLA_SEDGE};
            for (int k = 0; k < bank.length; k++) plant(in, r, bank[k], 3 + (k % 5) * 10, k < 5 ? 17 : 27, seed++);
            plant(in, r, PlantSpecies.AMAZON_SWORD, 44, 22, seed++); // emersed on the bank
            plant(in, r, PlantSpecies.CRYPT_GREEN, 10, 6, seed++); // under water in front
            TankClimate.step(level, b, 0.01);
            b.getTank().setClimate(new Climate.State(24, 24, 24, 85, 0, 0));
        }
        // the desert terrarium: sand, a few sandstone rocks, a basking lamp
        TankBlockEntity d = tank(p, tanks[2]);
        if (d != null) {
            InteriorRegion r = InteriorManager.ensureRegion(level, d);
            floor(in, r, Blocks.SAND);
            box(in, r, 20, 0, 24, 30, 3, 31, Blocks.SANDSTONE.defaultBlockState());
            box(in, r, 2, 0, 2, 6, 1, 6, Blocks.SMOOTH_SANDSTONE.defaultBlockState());
            PlantSpecies[] dry = {PlantSpecies.BARREL_CACTUS, PlantSpecies.COLUMN_CACTUS, PlantSpecies.PRICKLY_PEAR, PlantSpecies.MAMMILLARIA,
                    PlantSpecies.ECHEVERIA, PlantSpecies.HAWORTHIA, PlantSpecies.ALOE, PlantSpecies.AGAVE, PlantSpecies.LITHOPS, PlantSpecies.SEDUM,
                    PlantSpecies.BLUE_GRAMA, PlantSpecies.NEEDLEGRASS};
            for (int k = 0; k < dry.length; k++) plant(in, r, dry[k], 4 + (k % 6) * 8, k < 6 ? 8 : 18, seed++);
            level.setBlock(tanks[2].offset(1, 2, 0), ModBlocks.BASKING_LAMP.get().defaultBlockState().setValue(TankLightBlock.LIT, true), 2);
            d.getTank().setClimate(new Climate.State(31, 31, 40, 25, 4, 0));
        }
        for (TankBlockEntity c : new TankBlockEntity[]{a, b, d}) {
            if (c == null) continue;
            InteriorPlants.settleTint(level, c);
            c.markTankChanged();
        }
        animals(p, a, d);
        LOGGER.info("[smoketest] M56 tanks at {} {} {}", tanks[0], tanks[1], tanks[2]);
    }

    /** A dart frog pair and a tadpole in a bromeliad's cup; a tortoise and a corn snake at the agave. */
    private static void animals(ServerPlayer p, TankBlockEntity rain, TankBlockEntity desert) {
        ServerLevel level = p.level();
        if (rain != null) {
            for (int i = 0; i < 2; i++) put(level, rain, HerpSpecies.DART_FROG, i == 0, false);
            var cups = TerrariumPlantLife.cups(level.getServer(), rain.getTank());
            HerpEntity tadpole = put(level, rain, HerpSpecies.DART_FROG, true, true);
            if (tadpole != null && !cups.isEmpty()) tadpole.putInNursery(cups.getFirst());
            LOGGER.info("[smoketest] M56 bromeliad cups {}, tadpole in a cup {}", cups.size(), tadpole != null && tadpole.inNursery() ? "PASS" : "FAIL");
        }
        if (desert != null) {
            put(level, desert, HerpSpecies.HERMANN, true, false);
            put(level, desert, HerpSpecies.CORN_SNAKE, false, false);
        }
    }

    private static HerpEntity put(ServerLevel level, TankBlockEntity c, HerpSpecies s, boolean female, boolean baby) {
        HerpEntity e = ModEntities.herp(s).create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (e == null) return null;
        if (baby) e.makeHatchling();
        e.setFemale(female);
        TankData t = c.getTank();
        e.snapTo(t.min().getX() + 1.5, t.min().getY() + 0.4, t.min().getZ() + 0.8, 0F, 0F);
        TankInhabitants.adopt(e, c);
        if (!level.addFreshEntity(e)) return null;
        t.inhabitants().add(e.getUUID());
        c.markTankChanged();
        return e;
    }

    private void tanks() {
        then(200, () -> {});
        for (int i = 0; i < 3; i++) {
            int k = i;
            then(1, () -> look(tanks[k], 1.5, 1.1, -2.6, 0F, 6F));
            then(80, () -> shot("tank_" + NAMES[k]));
            then(1, () -> look(tanks[k], 3.3, 1.5, -1.3, 40F, 14F));
            then(40, () -> shot("tank_" + NAMES[k] + "_side"));
            then(1, () -> look(tanks[k], 1.5, 3.4, -0.4, 0F, 70F));
            then(40, () -> shot("tank_" + NAMES[k] + "_top"));
        }
        // inside each tank: the plants at their own size
        for (int i = 0; i < 3; i++) {
            int k = i;
            then(1, () -> server(p -> InteriorManager.enter(p, tanks[k])));
            int lift = k == 1 ? 9 : 0; // the bank of the paludarium is high: look from above it
            then(60, () -> inside(k, 24, 18 + lift, -2, 0F, 20F + lift * 2));
            then(60, () -> shot("inside_" + NAMES[k]));
            then(1, () -> inside(k, 40, 14 + lift, 4, 30F, 22F + lift * 2));
            then(40, () -> shot("inside_" + NAMES[k] + "_b"));
            then(1, () -> inside(k, 8, 10 + lift, 2, -25F, 18F + lift * 2));
            then(40, () -> shot("inside_" + NAMES[k] + "_c"));
            then(1, () -> server(InteriorManager::leave));
            then(40, () -> {});
        }
    }

    /** The camera inside a tank's interior at local (x, eyeY, z). */
    private void inside(int k, double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> {
            ServerLevel in = InteriorManager.interiorLevel(p.level().getServer());
            TankBlockEntity c = TankMultiblock.getController(p.level().getServer().overworld(), tanks[k]);
            if (in == null || c == null) return;
            InteriorRegion r = InteriorManager.ensureRegion(p.level().getServer().overworld(), c);
            p.teleportTo(in, r.originX() + x, InteriorRegion.BASE_Y + eyeY - p.getEyeHeight(), r.originZ() + z, java.util.Set.of(), yaw, pitch, true);
        });
    }

    // ------------------------------------------------------------------ the world

    private void world() {
        then(1, () -> server(this::garden));
        then(120, () -> look(garden, 12.5, 6.0, -3.5, 0F, 38F));
        then(60, () -> shot("world_overview"));
        for (int row = 0; row < 3; row++) {
            int k = row;
            for (int half = 0; half < 2; half++) {
                int h = half;
                then(1, () -> look(garden, 5.5 + h * 12, 2.4, k * 5 - 2.6, 0F, 28F));
                then(40, () -> shot("world_" + NAMES[k] + "_" + h));
            }
        }
    }

    /** Three rows (by habitat) of every terrarium plant in the world on its ground, the trimmed look one block behind. */
    private void garden(ServerPlayer p) {
        ServerLevel level = p.level();
        BlockPos g = home.offset(-12, 0, 40);
        for (int x = -2; x < 28; x++) for (int z = -6; z < 16; z++) {
            for (int y = 0; y < 8; y++) level.setBlock(g.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(g.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        }
        int[] col = new int[3];
        for (PlantSpecies s : PlantSpecies.landSpecies()) {
            int row = s.habitat().ordinal() - 1;
            int x = col[row]++ * 2;
            for (int t = 0; t < 2; t++) {
                BlockPos at = g.offset(x, 0, row * 5 + t);
                Block ground = s.form() == PlantSpecies.Form.EPIPHYTE ? Blocks.OAK_LOG : s.habitat() == PlantSpecies.Habitat.DRY ? Blocks.SAND
                        : s.form() == PlantSpecies.Form.MOSS ? Blocks.MOSS_BLOCK : Blocks.DIRT;
                level.setBlock(at.below(), ground.defaultBlockState(), 2);
                BlockState st = ModBlocks.WILD_LAND_PLANTS.get(s).get().defaultBlockState().setValue(WildLandPlantBlock.TRIMMED, t == 1);
                if (!st.canSurvive(level, at)) LOGGER.error("[smoketest] M56 {} cannot stand on {}", s.id(), ground);
                level.setBlock(at, st, 2);
            }
        }
        garden = g;
        LOGGER.info("[smoketest] M56 garden at {}", g);
    }

    // ------------------------------------------------------------------ the tank menu

    private void menu() {
        for (String lang : new String[]{"en_us", "de_de"}) {
            for (int scale : new int[]{0, 1}) {
                then(1, () -> language(lang));
                then(1, () -> until = () -> mc.getOverlay() == null);
                then(1, () -> {
                    mc.options.hideGui = false;
                    guiScale = mc.options.guiScale().get();
                    mc.options.guiScale().set(scale);
                    mc.resizeGui();
                });
                then(20, () -> look(tanks[0], 1.5, 1.1, -2.6, 0F, 6F));
                then(20, this::openMenu);
                then(40, () -> {
                    if (!(mc.screen instanceof TankScreen)) openMenu();
                });
                int tabs = 9;
                for (int i = 0; i < tabs; i++) {
                    int k = i;
                    then(30, () -> {
                        if (mc.screen instanceof TankScreen ts) ts.selectTabForTest(k);
                    });
                    then(15, () -> {
                        String name = "menu_" + lang + "_s" + scale + "_" + k;
                        checkFit(name);
                        shot(name);
                    });
                }
                // the second page of the plants tab: the plants of the interior (the terrarium plants among them)
                then(10, () -> {
                    if (mc.screen instanceof TankScreen ts) ts.showAquaticPageForTest();
                });
                then(20, () -> {
                    String name = "menu_" + lang + "_s" + scale + "_plants_interior";
                    checkFit(name);
                    shot(name);
                });
                // the tooltip of the name field
                then(5, () -> {
                    if (mc.screen instanceof TankScreen ts) ts.selectTabForTest(0);
                });
                then(10, () -> {
                    if (mc.screen instanceof TankScreen ts) ts.hoverNameForTest();
                });
                then(20, () -> shot("menu_" + lang + "_s" + scale + "_name_tooltip"));
                then(1, () -> server(ServerPlayer::closeContainer));
                then(5, () -> mc.setScreen(null));
                then(1, () -> {
                    mc.options.guiScale().set(guiScale);
                    mc.resizeGui();
                    mc.options.hideGui = true;
                });
            }
        }
        then(1, () -> language("en_us"));
        then(1, () -> until = () -> mc.getOverlay() == null);
    }

    private void checkFit(String name) {
        if (!(mc.screen instanceof TankScreen ts)) {
            LOGGER.error("[smoketest] M56 menu {}: the tank menu is not open", name);
            menuProblems++;
            return;
        }
        List<String> bad = ts.overflowForTest();
        if (bad.isEmpty()) LOGGER.info("[smoketest] M56 menu fit {} PASS", name);
        else {
            menuProblems += bad.size();
            LOGGER.warn("[smoketest] M56 menu fit {} FAIL: {}", name, bad);
        }
    }

    private void openMenu() {
        server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.level(), tanks[0]);
            if (c == null) return;
            org.overengineer.bettacraft.platform.Platform.get().openMenu(p,
                    new SimpleMenuProvider((id, inv, pl) -> new TankMenu(id, inv, c), Component.translatable("gui.bettacraft.tank.title")),
                    buf -> buf.writeBlockPos(c.getBlockPos()));
        });
    }

    private void language(String code) {
        if (code.equals(mc.options.languageCode)) return;
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        mc.reloadResourcePacks();
    }

    // ------------------------------------------------------------------ steps

    private void look(BlockPos b, double dx, double eyeDy, double dz, float yaw, float pitch) {
        if (b == null) return;
        server(p -> p.teleportTo(p.level().getServer().overworld(), b.getX() + dx, b.getY() + eyeDy - p.getEyeHeight(), b.getZ() + dz, java.util.Set.of(), yaw, pitch, true));
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
        if (until != null) {
            if (!until.getAsBoolean()) return false;
            until = null;
        }
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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m56_" + name + ".png", mc.getMainRenderTarget(), 1, c -> {});
    }
}
