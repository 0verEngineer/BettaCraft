package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.client.screen.TankScreen;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.Sump;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankUsers;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tank.menu.TankMenu;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tank.plants.TankPlants;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.terrarium.Climate;
import org.overengineer.bettacraft.terrarium.HerpEntity;
import org.overengineer.bettacraft.terrarium.HerpSpecies;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M62 part 4 client smoke ({@code -Psmoketestm62c}, NeoForge and Fabric, background window): the tank menu of a well-stocked
 * aquarium on cabinets with a sump (bettas, a shoal of neon tetras that is too small, cherry barbs, small animals, floating and
 * interior plants, two users, a log) and of a terrarium (the climate tab): every tab and sub-view in English and German at the GUI
 * scales 1, 2 and 4 of a 1920 x 1080 window. The buttons and fields whose text does not fit are logged ("[smoketest] M62c fit
 * ..."). Screenshots {@code bettacraft_m62c_<lang>_s<scale>_<view>.png}.
 */
final class DevClientSmokeM62c {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private BooleanSupplier until;
    private BlockPos home;
    private volatile BlockPos aquarium, terrarium;
    private int problems;

    DevClientSmokeM62c() {
        then(1, () -> GLFW.glfwSetWindowSize(mc.getWindow().handle(), 1920, 1080));
        then(1, () -> server(this::setup));
        then(20, () -> server(this::build));
        then(200, () -> server(this::settle));
        then(40, () -> server(this::settle));
        for (String lang : new String[]{"en_us", "de_de"}) {
            for (int scale : new int[]{1, 2, 4}) menu(lang, scale);
        }
        then(1, () -> language("en_us"));
        then(1, () -> until = () -> mc.gui.overlay() == null);
        then(20, () -> LOGGER.info("[smoketest] M62c done: {} problem(s), window {}x{}", problems,
                mc.getWindow().getWidth(), mc.getWindow().getHeight()));
    }

    private void setup(ServerPlayer p) {
        ServerLevel level = p.level();
        org.overengineer.bettacraft.gametest.framework.DevTime.set(level, 6000);
        level.getServer().setWeatherParameters(12000, 0, false, false);
        home = p.blockPosition();
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        BlockPos base = home.offset(-4, 0, 8);
        for (int x = -3; x < 16; x++) for (int z = -4; z < 6; z++) {
            level.setBlock(base.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 2);
            for (int y = 0; y < 5; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
        // the aquarium: 4 x 2 x 2 on cabinets, the sump in one of them
        aquarium = base.offset(0, 1, 0);
        for (int x = 0; x < 4; x++) {
            level.setBlockAndUpdate(aquarium.offset(x, -1, 0), ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH));
            level.setBlockAndUpdate(aquarium.offset(x, -1, 1), Blocks.SMOOTH_STONE.defaultBlockState());
        }
        TankBlockEntity a = tank(level, p, aquarium, 4, 2, 2, 26F, true);
        if (a == null) return;
        InteriorRegion r = region(level, a, Blocks.SAND);
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        long seed = 6200;
        PlantSpecies[] plants = {PlantSpecies.AMAZON_SWORD, PlantSpecies.CRYPT_GREEN, PlantSpecies.JAVA_FERN, PlantSpecies.ANUBIAS_NANA, PlantSpecies.VALLISNERIA,
                PlantSpecies.LUDWIGIA, PlantSpecies.ROTALA, PlantSpecies.JAVA_MOSS};
        for (int k = 0; k < plants.length; k++) {
            if (InteriorPlants.plantAt(in, r, plants[k], 4 + (k % 4) * 14, 0, k < 4 ? 8 : 22, plants[k].stages(), seed++) == null) {
                LOGGER.warn("[smoketest] M62c {} not planted", plants[k].id());
            }
        }
        for (int i = 0; i < 6; i++) TankPlants.add(a, i % 2 == 0 ? FloatingPlantType.DUCKWEED : FloatingPlantType.FROGBIT,
                a.getTank().min().getX() + 0.5 + i * 0.6, a.getTank().min().getZ() + 0.6 + (i % 2) * 0.6, 1F, i * 40F);
        light(level, aquarium, 4, 2, 1);
        level.setBlockAndUpdate(aquarium.offset(4, 0, 0), ModBlocks.EXTERNAL_FILTER.get().defaultBlockState());
        if (level.getBlockEntity(aquarium.offset(1, -1, 0)) instanceof CabinetBlockEntity cab) {
            ItemStack sump = new ItemStack(ModItems.SUMP.get());
            NonNullList<ItemStack> ch = NonNullList.withSize(Sump.CHAMBERS, ItemStack.EMPTY);
            ch.set(Sump.HEATER0, new ItemStack(ModItems.HEATER_MEDIUM.get()));
            ch.set(Sump.MEDIA0, new ItemStack(ModItems.FILTER_FOAM.get()));
            Sump.write(sump, ch);
            cab.setSumpItem(sump);
        }
        // a long-named male, a female, a juvenile; a too small shoal of neons (3 of 6), six cherry barbs, panda corys
        betta(level, a, false, 62001L, "Samurai Halfmoon Plakat Prince");
        betta(level, a, true, 62002L, null);
        betta(level, a, false, 62003L, null);
        for (int i = 0; i < 3; i++) fish(level, a, TankmateSpecies.NEON_TETRA, "wild", i == 0);
        for (int i = 0; i < 6; i++) fish(level, a, TankmateSpecies.CHERRY_BARB, "wild", i % 2 == 0);
        for (int i = 0; i < 2; i++) fish(level, a, TankmateSpecies.HARLEQUIN_RASBORA, "wild", i == 0);
        // small animals
        TankData t = a.getTank();
        CritterSpecies[] cs = {CritterSpecies.NEOCARIDINA, CritterSpecies.NEOCARIDINA, CritterSpecies.NEOCARIDINA, CritterSpecies.ZEBRA_NERITE,
                CritterSpecies.APPLE_SNAIL, CritterSpecies.DWARF_CRAYFISH};
        for (int i = 0; i < cs.length; i++) {
            Critter k = new Critter(t.nextCritterId(), cs[i], i % 2 == 0, CritterGenes.WILD, t.min().getX() + 0.4 + i * 0.5, t.min().getY() + 0.05, t.min().getZ() + 0.5);
            k.age = cs[i].juvenileDays() + 1;
            t.critters().add(k);
        }
        TankCritters.sync(level, a);
        // two users and a log
        UUID alice = UUID.nameUUIDFromBytes("m62c-alice".getBytes()), bob = UUID.nameUUIDFromBytes("m62c-bob".getBytes());
        t.users().put(alice, "Alice", TankUsers.Role.CARETAKER);
        t.users().put(bob, "Bartholomew_Longname", TankUsers.Role.VIEWER);
        TankCare.log(level, a, "log.bettacraft.tm.shoal_small", "@" + TankmateSpecies.NEON_TETRA.translationKey(), "3", "6");
        a.markTankChanged();
        TankEquipment.invalidate(a);
        Sump.step(level, a, 0.001);

        // the terrarium (climate tab)
        terrarium = base.offset(7, 0, 0);
        TankBlockEntity d = tank(level, p, terrarium, 3, 2, 2, 27F, false);
        if (d != null) {
            region(level, d, Blocks.SAND);
            level.setBlock(terrarium.offset(1, 2, 0), ModBlocks.BASKING_LAMP.get().defaultBlockState().setValue(TankLightBlock.LIT, true), 2);
            d.getTank().setClimate(new Climate.State(31, 31, 40, 25, 4, 0));
            herp(level, d, HerpSpecies.HERMANN, true);
            herp(level, d, HerpSpecies.LEOPARD_GECKO, false);
            d.markTankChanged();
        }
        LOGGER.info("[smoketest] M62c aquarium at {}, terrarium at {}", aquarium, terrarium);
    }

    /** A few care steps so that the shoals, the stocking, the log and the sump are filled in. */
    private void settle(ServerPlayer p) {
        ServerLevel level = p.level();
        for (BlockPos b : new BlockPos[]{aquarium, terrarium}) {
            if (b == null) continue;
            TankBlockEntity c = TankMultiblock.getController(level, b);
            if (c == null) continue;
            TankmateBreeding.step(level, c, 0.01);
            for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) {
                if (e instanceof BettaEntity be) be.getBettaData();
            }
            c.markTankChanged();
        }
    }

    private TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, int sy, int sz, float temp, boolean water) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        if (c == null) {
            LOGGER.error("[smoketest] M62c tank at {} did not form", min);
            problems++;
            return null;
        }
        if (water) TankInteractions.changeWater(level, c, (long) sx * sy * sz * 1000, 0F);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setOwner(p.getUUID());
        c.getTank().setName(water ? "Living room" : "");
        c.getTank().setWaterTemp(temp);
        c.getTank().setTargetTemp(temp);
        c.getTank().setWaterQuality(0.92F);
        c.markTankChanged();
        return c;
    }

    private static InteriorRegion region(ServerLevel level, TankBlockEntity c, net.minecraft.world.level.block.Block floor) {
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++)
            for (int z = 0; z < r.sizeZ(); z++) Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), floor.defaultBlockState(), false);
        return r;
    }

    private static void light(ServerLevel level, BlockPos min, int sx, int y, int z) {
        for (int x = 0; x < sx; x++) {
            level.setBlockAndUpdate(min.offset(x, y, z), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < sx - 1).setValue(TankLightBlock.LIT, true)
                    .setValue(TankLightBlock.COLOR, x % 2 == 0 ? DyeColor.BLUE : DyeColor.WHITE));
        }
    }

    private static void fish(ServerLevel level, TankBlockEntity c, TankmateSpecies s, String variety, boolean female) {
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        Vec3 p = w.clamp(new Vec3(w.box().minX + 0.5, w.box().minY + 0.5, w.box().minZ + 0.5), 0.3, 0.1, 0.3);
        var e = TankmateBreeding.spawn(level, c, s, TankmateGenes.pure(s, variety), female, LifeStage.ADULT, 0, p);
        if (e == null) LOGGER.error("[smoketest] M62c {} not spawned", s.id());
        else e.health().feed();
    }

    private static void betta(ServerLevel level, TankBlockEntity c, boolean female, long seed, String name) {
        BettaData d = BettaData.create(BettaGenotype.randomDomestic(RandomSource.create(seed)), female, 0);
        ItemStack bucket = BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d);
        Component err = TankInhabitants.insert(level, c, bucket, ModEntities.BETTA.get(), null, false);
        if (err != null) {
            LOGGER.error("[smoketest] M62c betta not inserted: {}", err.getString());
            return;
        }
        if (name == null) return;
        for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) {
            if (e instanceof BettaEntity b && b.getBettaData().fishId().equals(d.fishId())) b.setCustomName(Component.literal(name));
        }
    }

    private static void herp(ServerLevel level, TankBlockEntity c, HerpSpecies s, boolean female) {
        HerpEntity e = ModEntities.herp(s).create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
        if (e == null) return;
        e.setFemale(female);
        TankData t = c.getTank();
        e.snapTo(t.min().getX() + 1.5, t.min().getY() + 0.4, t.min().getZ() + 0.8, 0F, 0F);
        TankInhabitants.adopt(e, c);
        if (!level.addFreshEntity(e)) return;
        t.inhabitants().add(e.getUUID());
        c.markTankChanged();
    }

    // ------------------------------------------------------------------ the menu

    private void menu(String lang, int scale) {
        String pre = lang + "_s" + scale + "_";
        then(1, () -> language(lang));
        then(1, () -> until = () -> mc.gui.overlay() == null);
        then(1, () -> {
            org.overengineer.bettacraft.client.DevHud.setHidden(mc, false);
            mc.options.guiScale().set(scale);
            mc.resizeGui();
        });
        then(10, () -> look(aquarium, 2.0, 1.1, -2.6));
        then(20, () -> openMenu(aquarium));
        then(40, () -> {
            if (!(mc.gui.screen() instanceof TankScreen)) openMenu(aquarium);
        });
        view(pre + "01_overview", ts -> ts.selectTabForTest(0));
        view(pre + "02_animals_betta", TankScreen::showBettaForTest);
        view(pre + "03_animals_neon", ts -> ts.showTankmateForTest("neon_tetra"));
        view(pre + "04_animals_small", TankScreen::showCrittersPageForTest);
        view(pre + "05_animals_small_one", TankScreen::selectFirstCritterForTest);
        view(pre + "06_breeding", ts -> ts.selectTabForTest(2));
        view(pre + "07_water", ts -> ts.selectTabForTest(5));
        view(pre + "08_devices", ts -> ts.selectTabForTest(7));
        view(pre + "09_sump", ts -> ts.selectTabForTest(9));
        view(pre + "10_plants_floating", TankScreen::showPlantsPageForTest);
        view(pre + "11_plants_interior", TankScreen::showAquaticPageForTest);
        view(pre + "12_users", ts -> ts.selectTabForTest(4));
        view(pre + "13_log", ts -> ts.selectTabForTest(3));
        then(1, () -> server(ServerPlayer::closeContainer));
        then(5, () -> mc.gui.setScreen(null));
        then(10, () -> look(terrarium, 1.5, 1.1, -2.6));
        then(20, () -> openMenu(terrarium));
        then(40, () -> {
            if (!(mc.gui.screen() instanceof TankScreen)) openMenu(terrarium);
        });
        view(pre + "14_terrarium_overview", ts -> ts.selectTabForTest(0));
        view(pre + "15_terrarium_climate", ts -> ts.selectTabForTest(8));
        then(1, () -> server(ServerPlayer::closeContainer));
        then(5, () -> mc.gui.setScreen(null));
    }

    private void view(String name, Consumer<TankScreen> select) {
        then(20, () -> {
            if (mc.gui.screen() instanceof TankScreen ts) select.accept(ts);
        });
        then(15, () -> {
            checkFit(name);
            shot(name);
        });
    }

    private void checkFit(String name) {
        if (!(mc.gui.screen() instanceof TankScreen ts)) {
            LOGGER.error("[smoketest] M62c {}: the tank menu is not open", name);
            problems++;
            return;
        }
        List<String> bad = new ArrayList<>(ts.overflowForTest());
        bad.addAll(ts.textFitForTest());
        int[] size = ts.contentSizeForTest();
        if (bad.isEmpty()) LOGGER.info("[smoketest] M62c fit {} PASS (content {}x{})", name, size[0], size[1]);
        else {
            problems += bad.size();
            LOGGER.warn("[smoketest] M62c fit {} FAIL (content {}x{}): {}", name, size[0], size[1], bad);
        }
    }

    private void openMenu(BlockPos at) {
        server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.level(), at);
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

    private void look(BlockPos b, double dx, double eyeDy, double dz) {
        if (b == null) return;
        server(p -> p.teleportTo(p.level().getServer().overworld(), b.getX() + dx, b.getY() + eyeDy - p.getEyeHeight(), b.getZ() + dz, java.util.Set.of(), 0F, 6F, true));
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    boolean tick() {
        if (mc.player != null && !steps.isEmpty()) {
            mc.player.getAbilities().flying = true;
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
        if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.PauseScreen) mc.gui.setScreen(null);
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
        mc.gui.toastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m62c_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, c -> {});
    }
}
