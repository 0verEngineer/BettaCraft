package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.aquarist.AquaristArrival;
import org.overengineer.bettacraft.aquarist.AquaristEntity;
import org.overengineer.bettacraft.aquarist.AquaristMenu;
import org.overengineer.bettacraft.aquarist.AquaristRecords;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.filter.FilterChamberBlockEntity;
import org.overengineer.bettacraft.filter.FilterPartBlock;
import org.overengineer.bettacraft.filter.FilterPumpHeadBlockEntity;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.plumbing.FilterPipeBlock;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.platform.item.IItemHandler;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tank.plants.TankPlants;
import org.overengineer.bettacraft.terrarium.TankClimate;
import org.overengineer.bettacraft.terrarium.TankModeSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M66 client smoke ({@code -Psmoketestm66 [-Pm66tag=before|after] [-Pm66parts=tank,devices,aquarist,pipes]}, NeoForge and Fabric,
 * background window): every menu the UI overhaul changed, in English and German, at a roomy and at a cramped GUI scale: the tank menu
 * (overview, water, devices, plants, breeding, users, a terrarium's climate), the cabinet, the external filter, the feeder and the
 * clip-on feeder, the multiblock filter (two tanks, a reservoir behind a steel pipe) and the hired aquarist (three tabs, the species
 * list), and a water pipe under the crosshair (Jade). Screenshots {@code bettacraft_m66_<tag>_<lang>_<scale>_<shot>.png}.
 * <p>
 * The scene is written against the API of both versions (the version before the overhaul has no tabs, no clip-on feeder menu ...): what
 * only the new one has is reached by reflection and skipped where it is missing, so the "before" pictures come from the same scene.
 */
final class DevClientSmokeM66 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private int problems;
    private final String tag = System.getProperty("bettacraft.smoketest.m66tag", "after");
    private final String parts = System.getProperty("bettacraft.smoketest.m66parts", "tank,devices,aquarist,pipes");
    private volatile BlockPos base, mainTank, terrarium, cabinet, filter, feeder, clip, head, pipe, homeBasin, rearing1, rearing2, firstTank;
    private volatile AquaristEntity npc;
    private String lang;
    private int guiScale;
    private boolean powerFilter, powerHeater, power;

    DevClientSmokeM66() {
        then(1, () -> {
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            lang = mc.options.languageCode;
            guiScale = mc.options.guiScale().get();
            server(this::setup);
        });
        then(100, () -> server(this::setupAquarist)); // later: the point of interest tickets of the basin and the bed exist only after a few ticks
        then(60, () -> LOGGER.info("[smoketest] M66 window {}x{}", mc.getWindow().getWidth(), mc.getWindow().getHeight()));
        // the scales: GUI pixels as many as there are (a roomy menu) and as few as keep the smallest menu whole (a cramped one)
        int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
        int cramped = Math.max(1, Math.min(h / 250, w / 300));
        List<Integer> scales = cramped > 1 ? List.of(1, cramped) : List.of(1);
        for (String code : List.of("en_us", "de_de")) {
            for (int s : scales) {
                String sc = s == scales.get(0) ? "roomy" : "cramped";
                then(1, () -> {
                    language(code);
                    scale(s);
                });
                then(15, () -> {});
                if (parts.contains("tank")) tankShots(code, sc);
                if (parts.contains("devices")) deviceShots(code, sc);
                if (parts.contains("aquarist")) aquaristShots(code, sc);
            }
        }
        if (parts.contains("pipes")) {
            then(1, () -> {
                language("en_us");
                scale(2);
                mc.gui.setScreen(null);
            });
            then(1, () -> look(pipe, 1.2, 0.8, -1.6));
            then(60, () -> shot("en_us_pipe_tooltip"));
        }
        then(1, () -> {
            language(lang);
            scale(guiScale);
            server(p -> restore());
        });
        then(5, () -> LOGGER.info("[smoketest] M66 done: {} problem(s)", problems));
    }

    int problems() {
        return problems;
    }

    // ------------------------------------------------------------------ the shots

    private void tankShots(String code, String sc) {
        String pre = code + "_" + sc + "_";
        int[][] tabs = {{0, 0}, {5, 0}, {7, 0}, {6, 0}, {2, 0}, {4, 0}};
        String[] names = {"tank_overview", "tank_water", "tank_devices", "tank_plants", "tank_breeding", "tank_users"};
        for (int i = 0; i < tabs.length; i++) {
            int tab = tabs[i][0];
            String name = names[i];
            near(() -> mainTank);
            then(1, () -> openTank(mainTank));
            then(25, () -> selectTab(tab));
            then(10, () -> shot(pre + name));
            closeAll();
        }
        // the plants tab, second page (the plants of the interior)
        near(() -> mainTank);
        then(1, () -> openTank(mainTank));
        then(25, () -> {
            setField(mc.gui.screen(), "aquaticPage", true);
            selectTab(6);
        });
        then(10, () -> shot(pre + "tank_plants_aquatic"));
        closeAll();
        // a terrarium: its overview and its climate tab
        near(() -> terrarium);
        then(1, () -> openTank(terrarium));
        then(25, () -> selectTab(0));
        then(10, () -> shot(pre + "terrarium_overview"));
        then(1, () -> selectTab(8));
        then(10, () -> shot(pre + "terrarium_climate"));
        closeAll();
    }

    private void deviceShots(String code, String sc) {
        String pre = code + "_" + sc + "_";
        near(() -> cabinet);
        then(1, () -> openBlock(cabinet));
        then(25, () -> shot(pre + "cabinet"));
        closeAll();
        near(() -> filter);
        then(1, () -> openBlock(filter));
        then(25, () -> shot(pre + "external_filter"));
        closeAll();
        near(() -> feeder);
        then(1, () -> openBlock(feeder));
        then(25, () -> shot(pre + "feeder"));
        closeAll();
        near(() -> clip);
        then(1, () -> openBlock(clip));
        then(25, () -> shot(pre + "clip_feeder"));
        closeAll();
        near(() -> head);
        then(1, () -> mc.gui.setScreen(new org.overengineer.bettacraft.client.screen.MultiblockFilterScreen(head)));
        then(25, () -> shot(pre + "multiblock_filter"));
        closeAll();
    }

    private void aquaristShots(String code, String sc) {
        String pre = code + "_" + sc + "_";
        then(1, () -> server(p -> p.teleportTo(p.level(), npc.getX() - 2, npc.getY() + 0.5, npc.getZ() - 3, java.util.Set.of(), 0F, 10F, false)));
        then(10, () -> {});
        then(1, () -> server(p -> AquaristMenu.open(p, npc)));
        then(25, () -> shot(pre + "aquarist_work"));
        for (int i = 1; i <= 2; i++) {
            int tab = i;
            String name = i == 1 ? "aquarist_rearing" : "aquarist_tanks";
            then(1, () -> {
                call(mc.gui.screen(), "showTabForTest", new Class<?>[]{int.class}, tab);
                if (tab == 2) call(mc.gui.screen(), "selectFirstTankForTest", new Class<?>[0]);
            });
            then(10, () -> shot(pre + name));
        }
        then(1, () -> {
            call(mc.gui.screen(), "showTabForTest", new Class<?>[]{int.class}, 1);
            call(mc.gui.screen(), "openSpeciesForTest", new Class<?>[0]);
        });
        then(10, () -> shot(pre + "aquarist_species"));
        closeAll();
    }

    /** Stands the player a few blocks from {@code at} (a menu closes when the block is too far away). */
    private void near(java.util.function.Supplier<BlockPos> at) {
        then(1, () -> look(at.get(), 0.5, 2.5, -3.5));
        then(6, () -> {});
    }

    private void closeAll() {
        then(1, () -> server(ServerPlayer::closeContainer));
        then(3, () -> mc.gui.setScreen(null));
    }

    // ------------------------------------------------------------------ the scene

    private void setup(ServerPlayer p) {
        ServerLevel level = p.level();
        powerFilter = ServerConfig.POWER_FILTER.get();
        powerHeater = ServerConfig.POWER_HEATER.get();
        power = Features.own(Feature.POWER);
        ServerConfig.POWER_FILTER.set(true);
        ServerConfig.POWER_HEATER.set(true);
        Features.set(Feature.POWER, true);
        org.overengineer.bettacraft.gametest.framework.DevTime.set(level, 6000);
        level.getServer().setWeatherParameters(24000, 0, false, false);
        level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME, false, level.getServer());
        level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS, false, level.getServer());
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        base = p.blockPosition().offset(-8, -1, 6);
        for (int x = -4; x <= 46; x++) for (int z = -8; z <= 40; z++) {
            level.setBlock(base.offset(x, -1, z), Blocks.SMOOTH_QUARTZ.defaultBlockState(), 2);
            for (int y = 0; y < 9; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
        UUID me = p.getUUID();
        // 1. the main aquarium on a cabinet with a filter, a light bar and a feeder on top, floating plants, a pair of bettas, power
        for (int x = 0; x < 4; x++) level.setBlock(base.offset(x, 0, 0), cab(), 3);
        mainTank = base.offset(0, 1, 0);
        TankBlockEntity main = tank(level, mainTank, 4, 2, 3, me, true);
        main.getTank().setName("Betta Paradise");
        if (level.getBlockEntity(base.offset(1, 0, 0)) instanceof CabinetBlockEntity cab) {
            cab.items().setStackInSlot(CabinetBlockEntity.FILTER_SLOT, new ItemStack(ModItems.EXTERNAL_FILTER.get()));
            org.overengineer.bettacraft.tank.TankEquipment.invalidate(main);
        }
        cabinet = base.offset(1, 0, 0);
        level.setBlock(base.offset(0, 3, 1), ModBlocks.TANK_LIGHT.get().defaultBlockState(), 3);
        level.setBlock(base.offset(1, 3, 1), ModBlocks.TANK_LIGHT.get().defaultBlockState(), 3);
        feeder = base.offset(3, 3, 1);
        level.setBlock(feeder, ModBlocks.FEEDER.get().defaultBlockState(), 3);
        if (level.getBlockEntity(feeder) instanceof org.overengineer.bettacraft.equipment.FeederBlockEntity f) {
            f.items().setStackInSlot(0, new ItemStack(ModItems.BETTA_PELLETS.get(), 24));
            f.items().setStackInSlot(1, new ItemStack(ModItems.FISH_FLAKES.get(), 12));
        }
        main.getTank().setPowerBuffer(41_000L);
        RandomSource r = RandomSource.create(6600);
        var min = main.getTank().min();
        for (int i = 0; i < 26; i++) {
            TankPlants.add(main, i % 2 == 0 ? FloatingPlantType.SALVINIA : FloatingPlantType.DUCKWEED, min.getX() + 0.5 + r.nextDouble() * 3, min.getZ() + 0.5 + r.nextDouble() * 2, 0.8F,
                    r.nextFloat() * 360F);
        }
        betta(level, main, false, 6601);
        betta(level, main, true, 6602);
        main.markTankChanged();
        // 2. a terrarium (a dry tank of the type terrarium)
        terrarium = base.offset(8, 0, 0);
        TankBlockEntity ter = tank(level, terrarium, 3, 2, 3, me, false);
        ter.getTank().setName("Gecko Terrarium");
        ter.getTank().setTankMode(TankModeSetting.TERRARIUM);
        ter.limitMb = -1;
        TankClimate.refreshMode(level, ter);
        // 3. an external filter block beside a small aquarium, and a clip-on feeder on another
        TankBlockEntity small = tank(level, base.offset(13, 0, 0), 2, 2, 2, me, true);
        small.getTank().setName("Shrimp");
        filter = base.offset(12, 0, 0);
        level.setBlock(filter, ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, Direction.EAST), 3);
        tank(level, base.offset(18, 0, 0), 1, 1, 1, me, true).getTank().setName("Nano");
        clip = base.offset(17, 0, 0);
        level.setBlock(clip, ModBlocks.CLIP_FEEDER.get().defaultBlockState().setValue(org.overengineer.bettacraft.equipment.ClipFeederBlock.FACING, Direction.WEST), 3);
        if (level.getBlockEntity(clip) instanceof org.overengineer.bettacraft.equipment.ClipFeederBlockEntity c) c.items().setStackInSlot(0, new ItemStack(ModItems.BETTA_PELLETS.get(), 16));
        // 4. a multiblock filter on two tanks with a reservoir behind a steel pipe at the end of its fresh water line
        BlockPos o = base.offset(0, 0, 14);
        buildFilterRig(level, o, me);
        // 5. the home of an aquarist: basin, chest, reservoir, bed, three tanks around
        BlockPos h = base.offset(26, 0, 0);
        homeBasin = h;
        level.setBlock(h, ModBlocks.BREEDING_BASIN.get().defaultBlockState(), 3);
        level.setBlock(h.east(), Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(h.west(), ModBlocks.RESERVOIR.get().defaultBlockState(), 3);
        IItemHandler chest = org.overengineer.bettacraft.platform.Platform.get().itemHandler(level, h.east(), null);
        for (ItemStack s : List.of(new ItemStack(ModItems.BETTA_PELLETS.get(), 32), new ItemStack(Items.EMERALD, 8))) {
            ItemStack rest = s;
            for (int i = 0; i < chest.getSlots() && !rest.isEmpty(); i++) rest = chest.insertItem(i, rest, false);
        }
        BlockState foot = Blocks.BED.pick(net.minecraft.world.item.DyeColor.CYAN).defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.FOOT);
        level.setBlock(h.offset(0, 0, 3), foot, 3);
        level.setBlock(h.offset(0, 0, 4), foot.setValue(BedBlock.PART, BedPart.HEAD), 3);
        TankBlockEntity a = tank(level, h.offset(6, 0, -4), 3, 2, 2, me, true), b = tank(level, h.offset(6, 0, 2), 3, 2, 2, me, true),
                rear = tank(level, h.offset(11, 0, -1), 2, 2, 2, me, true), rear2 = tank(level, h.offset(11, 0, 4), 2, 2, 2, me, true);
        a.getTank().setName("Betta Paradise");
        b.getTank().setName("Community");
        rear.getTank().setName("Rearing 1");
        rear2.getTank().setName("Rearing 2");
        for (TankBlockEntity c : List.of(a, b, rear, rear2)) AquaristRecords.get(level.getServer()).noteTank(level, c);
        rearing1 = rear.getBlockPos();
        rearing2 = rear2.getBlockPos();
        firstTank = a.getBlockPos();
        // 6. a glass pipe and a steel pipe with the reservoir
        pipe = base.offset(36, 0, 12);
        level.setBlock(pipe, ModBlocks.GLASS_PIPE.get().defaultBlockState(), 3);
        level.setBlock(pipe.east(), ModBlocks.GLASS_PIPE.get().defaultBlockState(), 3);
        level.setBlock(pipe.west(), ModBlocks.RESERVOIR.get().defaultBlockState(), 3);
        p.teleportTo(level, base.getX() + 2.5, base.getY() + 3, base.getZ() - 6.5, java.util.Set.of(), 0F, 20F, false);
        LOGGER.info("[smoketest] M66 scene built at {}", base);
    }

    /** The aquarist himself, after the basin and the bed have their tickets. */
    private void setupAquarist(ServerPlayer p) {
        ServerLevel level = p.level();
        BlockPos h = homeBasin;
        TankBlockEntity rear = TankMultiblock.getController(level, rearing1), rear2 = TankMultiblock.getController(level, rearing2), a = TankMultiblock.getController(level, firstTank);
        npc = AquaristArrival.spawnHiredHomeless(level, p.getUUID(), p.getGameProfile().name(), Vec3.atCenterOf(h.offset(2, 0, 1)));
        npc.alwaysDay = true;
        npc.setNoAi(true);
        if (!npc.setHome(level, h, h.offset(0, 0, 4))) problem("the aquarist found no home: " + npc.workplace() + " " + npc.bed());
        npc.setRearingTank(rear.getTank().id());
        // what only the new version has: a second rearing tank (limited to bettas), a task off at the first tank, an assigned chest
        call(npc, "addRearingTank", new Class<?>[]{UUID.class}, rear2.getTank().id());
        call(npc, "toggleRearingSpecies", new Class<?>[]{UUID.class, String.class}, rear2.getTank().id(), "betta");
        call(npc, "setMayDo", new Class<?>[]{UUID.class, org.overengineer.bettacraft.aquarist.AquaristTask.class, boolean.class}, a.getTank().id(),
                org.overengineer.bettacraft.aquarist.AquaristTask.WARN, false);
        call(npc, "setChest", new Class<?>[]{BlockPos.class}, h.east());
    }

    private void buildFilterRig(ServerLevel level, BlockPos o, UUID owner) {
        tank(level, o.offset(0, 1, 0), 2, 2, 2, owner, true).getTank().setName("Filter A");
        tank(level, o.offset(4, 1, 0), 4, 2, 2, owner, true).getTank().setName("Filter B");
        BlockPos chamber = o.offset(10, 1, 4);
        level.setBlock(chamber, ModBlocks.FILTER_CHAMBER.get().defaultBlockState(), 3);
        level.setBlock(chamber.above(), ModBlocks.FILTER_CHAMBER.get().defaultBlockState(), 3);
        level.setBlock(chamber.above(2), ModBlocks.FILTER_PUMP_HEAD.get().defaultBlockState(), 3);
        level.setBlock(chamber.west(), ModBlocks.FILTER_INLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.WEST), 3);
        level.setBlock(chamber.above().west(), ModBlocks.FILTER_OUTLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.WEST), 3);
        ((FilterChamberBlockEntity) level.getBlockEntity(chamber)).setMedium(new ItemStack(ModItems.CERAMIC_RINGS.get()));
        ((FilterChamberBlockEntity) level.getBlockEntity(chamber.above())).setMedium(new ItemStack(ModItems.FILTER_FOAM.get()));
        head = chamber.above(2);
        BlockState in = ModBlocks.FILTER_INLET_PIPE.get().defaultBlockState(), out = ModBlocks.FILTER_OUTLET_PIPE.get().defaultBlockState();
        for (int[] p : new int[][]{{0, 1, 2}, {0, 1, 3}, {4, 1, 2}, {4, 1, 3}}) level.setBlock(o.offset(p[0], p[1] + 0, p[2]).above(0), in, 3);
        for (int[] p : new int[][]{{1, 2, 2}, {1, 2, 3}, {5, 2, 2}, {5, 2, 3}}) level.setBlock(o.offset(p[0], p[1], p[2]), out, 3);
        for (int x = 0; x <= 8; x++) level.setBlock(o.offset(x, 1, 4), in, 3);
        for (int x = 1; x <= 8; x++) level.setBlock(o.offset(x, 2, 4), out, 3);
        FilterPipeBlock.makeJunction(level, o.offset(4, 1, 4));
        FilterPipeBlock.makeJunction(level, o.offset(5, 2, 4));
        level.setBlock(o.offset(11, 1, 4), ModBlocks.FILTER_FRESH_PORT.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.EAST), 3);
        level.setBlock(o.offset(12, 1, 4), ModBlocks.FILTER_FRESH_PIPE.get().defaultBlockState(), 3);
        level.setBlock(o.offset(13, 1, 4), ModBlocks.FILTER_FRESH_PIPE.get().defaultBlockState(), 3);
        level.setBlock(o.offset(14, 1, 4), ModBlocks.STAINLESS_STEEL_PIPE.get().defaultBlockState(), 3);
        level.setBlock(o.offset(15, 1, 4), ModBlocks.RESERVOIR.get().defaultBlockState(), 3);
        IFluidHandler res = org.overengineer.bettacraft.platform.Platform.get().fluidHandler(level, o.offset(15, 1, 4), null);
        if (res != null) res.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 6000), IFluidHandler.FluidAction.EXECUTE);
        if (level.getBlockEntity(head) instanceof FilterPumpHeadBlockEntity hd) {
            hd.rescan();
            org.overengineer.bettacraft.gametest.FilterLinesGameTests.guardAll(level, head);
            hd.run(level, true);
        }
    }

    private void restore() {
        ServerConfig.POWER_FILTER.set(powerFilter);
        ServerConfig.POWER_HEATER.set(powerHeater);
        Features.set(Feature.POWER, power);
    }

    private static BlockState cab() {
        return ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH);
    }

    private static TankBlockEntity tank(ServerLevel level, BlockPos min, int sx, int sy, int sz, UUID owner, boolean water) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlock(pos, ModBlocks.TANK.get().defaultBlockState(), 3);
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        if (water) TankInteractions.changeWater(level, c, (long) sx * sy * sz * 1000, 0F);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setOwner(owner);
        c.getTank().setWaterTemp(26F);
        c.getTank().setTargetTemp(26F);
        c.getTank().setWaterQuality(0.86F);
        return c;
    }

    private static void betta(ServerLevel level, TankBlockEntity c, boolean female, long seed) {
        BettaData d = BettaData.create(BettaGenotype.randomDomestic(RandomSource.create(seed)), female, 0);
        Component err = TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d), ModEntities.BETTA.get(), null, false);
        if (err != null) LOGGER.error("[smoketest] M66 betta not inserted: {}", err.getString());
    }

    // ------------------------------------------------------------------ helpers

    private void openTank(BlockPos at) {
        server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.level(), at);
            org.overengineer.bettacraft.platform.Platform.get().openMenu(p, new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new org.overengineer.bettacraft.tank.menu.TankMenu(id, inv, c),
                    Component.translatable("gui.bettacraft.tank.title")), buf -> buf.writeBlockPos(c.getBlockPos()));
        });
    }

    /** Opens the menu of a block entity that is a menu provider (cabinet, filter, feeders). */
    private void openBlock(BlockPos at) {
        server(p -> {
            if (p.level().getBlockEntity(at) instanceof net.minecraft.world.MenuProvider mp) {
                org.overengineer.bettacraft.platform.Platform.get().openMenu(p, mp, buf -> buf.writeBlockPos(at));
            } else {
                LOGGER.error("[smoketest] M66 no menu at {}", at);
            }
        });
    }

    private void selectTab(int index) {
        if (mc.gui.screen() instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.selectTabForTest(index);
        else problem("no tank screen for tab " + index + ": " + mc.gui.screen());
    }

    private void language(String code) {
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
        if (mc.gui.screen() != null) mc.gui.screen().resize(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    private void scale(int s) {
        mc.options.guiScale().set(s);
        mc.resizeGui();
    }

    /** Calls a method that only the new version has (nothing happens in the old one). */
    private static void call(Object target, String name, Class<?>[] types, Object... args) {
        if (target == null) return;
        try {
            var m = target.getClass().getMethod(name, types);
            m.invoke(target, args);
        } catch (NoSuchMethodException e) {
            // the version before the overhaul
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("[smoketest] M66 {} failed: {}", name, e.toString());
        }
    }

    private static void setField(Object target, String name, Object value) {
        if (target == null) return;
        try {
            var f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("[smoketest] M66 field {} failed: {}", name, e.toString());
        }
    }

    private void problem(String what) {
        problems++;
        LOGGER.error("[smoketest] M66 FAIL: {}", what);
    }

    private void look(BlockPos at, double dx, double dy, double dz) {
        Vec3 c = Vec3.atCenterOf(at);
        Vec3 eye = c.add(dx, dy, dz);
        Vec3 d = c.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
        server(p -> p.teleportTo(p.level(), eye.x, eye.y - p.getEyeHeight(), eye.z, java.util.Set.of(), yaw, pitch, false));
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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m66_" + tag + "_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, c -> {});
    }
}
