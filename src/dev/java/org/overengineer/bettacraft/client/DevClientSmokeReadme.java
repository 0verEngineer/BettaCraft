package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.client.interior.IrisCompat;
import org.overengineer.bettacraft.co2.Co2CylinderBlock;
import org.overengineer.bettacraft.co2.Co2CylinderBlockEntity;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.equipment.AirPumpBlock;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.LedPanelBlock;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.filter.FilterPartBlock;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.genetics.BettaVarietyTable;
import org.overengineer.bettacraft.plants.InteriorPlant;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.progression.GuideBook;
import org.overengineer.bettacraft.reef.DosingPumpBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankStyle;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tank.plants.TankPlants;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * README media ({@code gradlew runClient -Psmoketestreadme}, background window, 1600x900): builds a small aquarium room on
 * a spruce cabinet counter (a planted show tank with LED panels, CO2 cylinder, multiblock filter and air pump, a reef tank
 * with corals, clownfish and seahorses and a dosing pump, a betta nano tank) and a long tank with a line of betta
 * varieties, then takes stills {@code run/screenshots/readme_*.png} and frame sequences
 * {@code readme_anim_<name>_<nnn>.png} (one frame per game tick, taken right after the frame is drawn).
 * {@code tools/assetgen/readme_media.py} turns them into {@code docs/images}. With a shader pack (add {@code -Pshaders})
 * only the stills of the room are taken, with the suffix {@code _shaders}.
 */
final class DevClientSmokeReadme {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static final ResourceLocation WOOD = ResourceLocation.withDefaultNamespace("spruce_planks");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final String lang = mc.options.languageCode;
    private BlockPos base, gallery;
    private int wait;
    private boolean shaders;

    // frame capture: one picture per game tick of the client level, taken after the frame is drawn
    private static DevClientSmokeReadme active;
    private static boolean listening;
    private String seqName;
    private int seqLeft, seqIndex;
    private double seqNext;

    DevClientSmokeReadme() {
        active = this;
        if (!listening) {
            listening = true;
            NeoForge.EVENT_BUS.addListener((RenderFrameEvent.Post e) -> {
                if (active != null) active.frame(e.getPartialTick().getGameTimeDeltaPartialTick(false));
            });
        }
        then(1, () -> {
            shaders = IrisCompat.shaderPackInUse();
            mc.getWindow().setWindowed(1600, 900);
            mc.options.hideGui = true;
            mc.options.enableVsync().set(false);
            mc.getWindow().updateVsync(false);
            mc.options.framerateLimit().set(120);
        });
        then(1, () -> server(this::build));
        then(1, () -> server(p -> {
            p.serverLevel().setDayTime(5000);
            reach(p, false);
        }));
        // the room: let the chunks, miniatures and plant meshes build
        then(1, () -> look(base.getX() + 4.0, base.getY() + 2.5, base.getZ() - 7.6, 0F, 2F));
        then(240, () -> shot("room"));
        then(1, () -> look(base.getX() + 2.0, base.getY() + 3.0, base.getZ() - 6.0, -10F, 10F));
        then(60, () -> shot("room_left"));
        if (!Boolean.getBoolean("bettacraft.readme.roomonly")) {
            // the planted tank close up and as a frame sequence
            then(1, () -> look(base.getX() + 3.0, base.getY() + 2.45, base.getZ() - 3.3, 0F, 4F));
            then(80, () -> shot("planted"));
            then(1, () -> look(base.getX() + 3.0, base.getY() + 2.5, base.getZ() - 2.4, 0F, 3F));
            then(40, () -> { if (!shaders) sequence("planted", 110); });
            then(20, () -> {});
            // the reef tank close up and as a frame sequence
            then(1, () -> look(base.getX() + 11.0, base.getY() + 2.0, base.getZ() - 2.5, 0F, 6F));
            then(80, () -> shot("reef"));
            then(1, () -> look(base.getX() + 11.0, base.getY() + 1.95, base.getZ() - 1.7, 0F, 4F));
            then(40, () -> { if (!shaders) sequence("reef", 110); });
            then(20, () -> {});
            // the nano tank
            then(1, () -> look(base.getX() - 3.0, base.getY() + 2.1, base.getZ() - 2.0, 0F, 10F));
            then(60, () -> shot("nano"));
            // the betta varieties
            then(1, () -> look(gallery.getX() + 2.5, gallery.getY() + 0.55, gallery.getZ() - 1.5, 0F, 0F));
            then(80, () -> shot("varieties"));
            // inside the planted tank (scale 16)
            then(1, () -> server(p -> {
                TankBlockEntity c = TankMultiblock.getController(p.serverLevel(), base.offset(0, 1, 0));
                InteriorRegion r = InteriorManager.ensureRegion(p.serverLevel(), c);
                BlockPos at = r.toWorld(48, 16, 1);
                p.teleportTo(InteriorManager.interiorLevel(p.server), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F, 18F);
            }));
            then(160, () -> shot("inside"));
            then(1, () -> { if (!shaders) sequence("inside", 70); });
            then(20, () -> {});
            // the tank menu: the betta list with the genes of the first, the water tab; the handbook
            then(1, () -> mc.options.hideGui = false);
            then(1, () -> look(gallery.getX() + 2.5, gallery.getY() + 0.55, gallery.getZ() - 2.0, 0F, 0F));
            then(20, () -> openMenu(gallery));
            then(20, () -> {
                if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.selectTabForTest(1);
                else LOGGER.error("[smoketest] README no tank screen");
            });
            then(20, () -> shot("menu_bettas"));
            then(1, () -> server(ServerPlayer::closeContainer));
            then(5, () -> mc.setScreen(null));
            then(1, () -> look(base.getX() + 3.0, base.getY() + 2.0, base.getZ() - 3.0, 0F, 0F));
            then(20, () -> openMenu(base.offset(0, 1, 0)));
            then(20, () -> {
                if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.selectTabForTest(5);
            });
            then(20, () -> shot("menu_water"));
            then(1, () -> server(ServerPlayer::closeContainer));
            then(5, () -> mc.setScreen(null));
            then(1, () -> {
                if (GuideBook.available()) org.overengineer.bettacraft.compat.guideme.GuideMECompat.open(mc.player, GuideBook.GUIDE_ID,
                        BettaCraft.id("bettas/genetics.md"));
            });
            then(60, () -> shot("guide"));
            then(5, () -> mc.setScreen(null));
            then(1, () -> mc.options.hideGui = true);
        }
        then(1, () -> mc.options.languageCode = lang);
    }

    // ------------------------------------------------------------------ building

    private void build(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ServerLevel in = InteriorManager.interiorLevel(p.server);
        RandomSource r = RandomSource.create(1800);
        base = p.blockPosition().offset(-4, 0, 24);
        // the room: plank floor, a back wall with a dark skirting, a long spruce cabinet counter
        for (int x = -10; x <= 18; x++)
            for (int z = -14; z <= 5; z++) {
                level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.DARK_OAK_PLANKS.defaultBlockState());
                for (int y = 0; y < 8; y++) level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
        for (int x = -10; x <= 18; x++)
            for (int y = 0; y < 14; y++) level.setBlockAndUpdate(base.offset(x, y, 4), (y == 0 ? Blocks.STRIPPED_DARK_OAK_WOOD : Blocks.SMOOTH_QUARTZ).defaultBlockState());
        // black background foil behind the planted and the reef tank
        for (int x = 0; x < 6; x++) for (int y = 1; y < 4; y++) level.setBlockAndUpdate(base.offset(x, y, 3), Blocks.BLACK_CONCRETE.defaultBlockState());
        for (int x = 9; x < 15; x++) for (int y = 1; y < 3; y++) level.setBlockAndUpdate(base.offset(x, y, 3), Blocks.BLACK_CONCRETE.defaultBlockState());
        for (int x = -5; x <= 13; x++)
            for (int z = 0; z <= 2; z++) cabinet(level, base.offset(x, 0, z));
        // the planted show tank, 6 x 3 x 3
        BlockPos h = base.offset(0, 1, 0);
        TankBlockEntity hero = tank(level, h, 6, 3, 3, 0F);
        hero.getTank().setWaterTemp(26F);
        hero.getTank().setTargetTemp(26F);
        for (int x = 0; x < 6; x++)
            for (int z = 0; z < 2; z++)
                level.setBlockAndUpdate(h.offset(x, 3, z), ModBlocks.LED_PANEL.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                        .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 5).setValue(LedPanelBlock.PARTNER_POS, z == 0)
                        .setValue(TankLightBlock.LIT, true));
        scape(level, in, hero);
        // CO2 cylinder with a solenoid on the left, multiblock filter and air pump on the right
        level.setBlockAndUpdate(h.offset(-1, 0, 1), ModBlocks.CO2_CYLINDER.get().defaultBlockState()
                .setValue(Co2CylinderBlock.FACING, Direction.NORTH).setValue(Co2CylinderBlock.SOLENOID, true));
        if (level.getBlockEntity(h.offset(-1, 0, 1)) instanceof Co2CylinderBlockEntity cyl) cyl.setGas(Co2CylinderBlockEntity.capacity() * 0.8F);
        net.minecraft.world.item.Item[] media = {ModItems.FILTER_FOAM.get(), ModItems.CERAMIC_RINGS.get()};
        for (int y = 0; y < 2; y++) {
            level.setBlockAndUpdate(h.offset(7, y, 1), ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
            if (level.getBlockEntity(h.offset(7, y, 1)) instanceof org.overengineer.bettacraft.filter.FilterChamberBlockEntity ch) ch.setMedium(new ItemStack(media[y]));
        }
        level.setBlockAndUpdate(h.offset(7, 2, 1), ModBlocks.FILTER_PUMP_HEAD.get().defaultBlockState());
        level.setBlockAndUpdate(h.offset(6, 0, 1), ModBlocks.FILTER_INLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.WEST));
        level.setBlockAndUpdate(h.offset(6, 1, 1), ModBlocks.FILTER_OUTLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.WEST));
        level.setBlockAndUpdate(h.offset(6, 0, 2), ModBlocks.AIR_PUMP.get().defaultBlockState().setValue(AirPumpBlock.FACING, Direction.WEST));
        TankEquipment.invalidate(hero);
        // the reef tank, 4 x 2 x 3, blue and white lights, a dosing pump
        BlockPos rf = base.offset(9, 1, 0);
        TankBlockEntity reef = tank(level, rf, 4, 2, 3, 35F);
        for (int x = 0; x < 4; x++)
            level.setBlockAndUpdate(rf.offset(x, 2, 1), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 3).setValue(TankLightBlock.LIT, true)
                    .setValue(TankLightBlock.COLOR, x % 2 == 0 ? DyeColor.BLUE : DyeColor.WHITE));
        level.setBlockAndUpdate(rf.offset(4, 0, 1), ModBlocks.DOSING_PUMP.get().defaultBlockState().setValue(DosingPumpBlock.FACING, Direction.NORTH));
        reefScape(level, in, reef, r);
        // the betta nano tank, 2 x 2 x 2: a Nemo male, floating plants
        BlockPos nn = base.offset(-4, 1, 0);
        TankBlockEntity nano = tank(level, nn, 2, 2, 2, 0F);
        for (int x = 0; x < 2; x++)
            level.setBlockAndUpdate(nn.offset(x, 2, 1), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 1).setValue(TankLightBlock.LIT, true));
        InteriorRegion nr = region(level, nano, Blocks.SAND.defaultBlockState());
        rock(in, nr, 8, 22, 5, 5, Blocks.OAK_LOG.defaultBlockState());
        plant(in, nr, PlantSpecies.JAVA_FERN, 8, -1, 22, 31);
        plant(in, nr, PlantSpecies.CRYPT_GREEN, 22, 0, 10, 32);
        plant(in, nr, PlantSpecies.VALLISNERIA, 24, 0, 26, 33);
        plant(in, nr, PlantSpecies.ANUBIAS_NANA, 14, 0, 14, 34);
        InteriorPlants.settleTint(level, nano);
        for (int i = 0; i < 6; i++) TankPlants.add(nano, i % 2 == 0 ? FloatingPlantType.FROGBIT : FloatingPlantType.RED_ROOT_FLOATER,
                nn.getX() + 0.3 + r.nextDouble() * 1.4, nn.getZ() + 0.6 + r.nextDouble() * 1.2, 0.7F + r.nextFloat() * 0.3F, r.nextFloat() * 6.28F);
        nano.markTankChanged();
        betta(level, nano, "nemo", false);
        // the varieties: a long low tank further away (not in the room pictures), bettas in a row, still
        gallery = base.offset(0, 1, -40);
        for (int x = -4; x <= 9; x++)
            for (int y = -1; y < 5; y++) level.setBlockAndUpdate(gallery.offset(x, y, 1), Blocks.SMOOTH_QUARTZ.defaultBlockState());
        for (int x = 0; x < 5; x++) cabinet(level, gallery.offset(x, -1, 0));
        TankBlockEntity gal = tank(level, gallery, 5, 1, 1, 0F);
        InteriorRegion gr = region(level, gal, Blocks.SAND.defaultBlockState());
        for (int x = 3; x < 80; x += 5) plant(in, gr, x % 2 == 0 ? PlantSpecies.VALLISNERIA : PlantSpecies.ROTALA, x, 0, 13, 40 + x);
        InteriorPlants.settleTint(level, gal);
        for (int x = 0; x < 5; x++)
            level.setBlockAndUpdate(gallery.offset(x, 1, 0), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 4).setValue(TankLightBlock.LIT, true));
        String[] varieties = {"koi_galaxy", "black_orchid", "mustard_gas", "samurai", "avatar", "hellboy"};
        for (String v : varieties) betta(level, gal, v, false);
        server(pl -> arrangeGallery(pl.serverLevel(), varieties.length));
        LOGGER.info("[smoketest] README scene at {}, varieties at {}", base, gallery);
    }

    /** The bettas of the varieties tank in a row, side on, alternating directions, frozen. */
    private void arrangeGallery(ServerLevel level, int n) {
        List<BettaEntity> fish = level.getEntitiesOfClass(BettaEntity.class, new AABB(gallery).inflate(6, 1, 1));
        fish.sort(java.util.Comparator.comparingInt(BettaEntity::getId));
        for (int i = 0; i < fish.size(); i++) {
            BettaEntity b = fish.get(i);
            float yaw = i % 2 == 0 ? 90F : -90F;
            b.moveTo(gallery.getX() + 0.45 + i * 0.82, gallery.getY() + 0.42 + (i % 3) * 0.08, gallery.getZ() + 0.5, yaw, 0F);
            b.setYBodyRot(yaw);
            b.setYHeadRot(yaw);
            SmokeFreeze.freeze(b, true);
            org.overengineer.bettacraft.genetics.MagnifierItem.analyse(b);
        }
        if (fish.size() != n) LOGGER.error("[smoketest] README {} bettas in the varieties tank, expected {}", fish.size(), n);
    }

    /** The planted tank: a stone hill on the left, a root on the right, a carpet in front, stem plants and Vallisneria behind. */
    private void scape(ServerLevel level, ServerLevel in, TankBlockEntity c) {
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++)
            for (int z = 0; z < r.sizeZ(); z++)
                Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z),
                        (z < 14 ? Blocks.SAND : ModBlocks.ACTIVE_SOIL.get()).defaultBlockState(), false);
        rock(in, r, 20, 26, 9, 11, Blocks.STONE.defaultBlockState());
        rock(in, r, 28, 20, 5, 6, Blocks.ANDESITE.defaultBlockState());
        rock(in, r, 72, 28, 6, 6, Blocks.OAK_LOG.defaultBlockState());
        long seed = 1800;
        for (int x = 4; x < 94; x += 7) plant(in, r, PlantSpecies.VALLISNERIA, x, 0, 43 - (x / 7) % 2 * 2, seed++);
        plant(in, r, PlantSpecies.ROTALA, 38, 0, 38, seed++);
        plant(in, r, PlantSpecies.ROTALA, 44, 0, 40, seed++);
        plant(in, r, PlantSpecies.ROTALA, 41, 0, 34, seed++);
        plant(in, r, PlantSpecies.LUDWIGIA, 84, 0, 37, seed++);
        plant(in, r, PlantSpecies.LUDWIGIA, 89, 0, 32, seed++);
        plant(in, r, PlantSpecies.ALTERNANTHERA, 54, 0, 32, seed++);
        plant(in, r, PlantSpecies.ALTERNANTHERA, 60, 0, 36, seed++);
        plant(in, r, PlantSpecies.AMAZON_SWORD, 8, 0, 34, seed++);
        plant(in, r, PlantSpecies.TIGER_LOTUS, 50, 0, 22, seed++);
        plant(in, r, PlantSpecies.CRYPT_GREEN, 10, 0, 14, seed++);
        plant(in, r, PlantSpecies.CRYPT_BROWN, 86, 0, 18, seed++);
        plant(in, r, PlantSpecies.CRYPT_FLAMINGO, 62, 0, 18, seed++);
        for (int x = 22; x < 80; x += 6) plant(in, r, PlantSpecies.MONTE_CARLO, x, 0, 6 + (x % 4), seed++);
        plant(in, r, PlantSpecies.JAVA_FERN, 20, -1, 26, seed++);
        plant(in, r, PlantSpecies.BUCE_BROWNIE_RED, 28, -1, 20, seed++);
        plant(in, r, PlantSpecies.ANUBIAS_NANA, 72, -1, 28, seed++);
        plant(in, r, PlantSpecies.JAVA_MOSS, 67, -1, 25, seed++);
        plant(in, r, PlantSpecies.BUCE_GREEN_WAVY, 76, -1, 24, seed);
        InteriorPlants.settleTint(level, c);
        for (int k = 0; k < 3; k++) in.setBlock(r.toWorld(90 - k * 3, 0, 44), ModBlocks.AIR_STONE.get().defaultBlockState(), 3);
        BlockPos min = c.getTank().min();
        RandomSource fr = RandomSource.create(7);
        for (int i = 0; i < 9; i++) {
            TankPlants.add(c, i % 3 == 0 ? FloatingPlantType.SALVINIA : FloatingPlantType.FROGBIT, min.getX() + 4.0 + fr.nextDouble() * 1.8,
                    min.getZ() + 0.6 + fr.nextDouble() * 2.0, 0.7F + fr.nextFloat() * 0.3F, fr.nextFloat() * 6.28F);
        }
        c.markTankChanged();
        betta(level, c, "koi_galaxy", false);
        for (int i = 0; i < 10; i++) fish(level, c, TankmateSpecies.NEON_TETRA, TankmateGenes.wild(TankmateSpecies.NEON_TETRA), i % 2 == 0, fr);
        for (int i = 0; i < 6; i++) fish(level, c, TankmateSpecies.HARLEQUIN_RASBORA, TankmateGenes.wild(TankmateSpecies.HARLEQUIN_RASBORA), i % 2 == 0, fr);
        for (int i = 0; i < 4; i++) fish(level, c, TankmateSpecies.PANDA_CORY, TankmateGenes.wild(TankmateSpecies.PANDA_CORY), i % 2 == 0, fr);
        for (int i = 0; i < 2; i++) fish(level, c, TankmateSpecies.OTOCINCLUS, TankmateGenes.wild(TankmateSpecies.OTOCINCLUS), i % 2 == 0, fr);
        for (int i = 0; i < 8; i++) TankCritters.add(level, c, CritterSpecies.NEOCARIDINA, CritterGenes.pure(0, 0.7F), i % 2 == 0, 10);
        for (int i = 0; i < 3; i++) TankCritters.add(level, c, CritterSpecies.ZEBRA_NERITE, CritterGenes.WILD, i % 2 == 0, 10);
    }

    private void reefScape(ServerLevel level, ServerLevel in, TankBlockEntity c, RandomSource r) {
        InteriorRegion ir = region(level, c, Blocks.SAND.defaultBlockState());
        rock(in, ir, 36, 34, 10, 10, ModBlocks.REEF_ROCK.get().defaultBlockState());
        rock(in, ir, 12, 32, 8, 6, ModBlocks.LIVE_ROCK.get().defaultBlockState());
        rock(in, ir, 54, 30, 7, 7, ModBlocks.LIVE_ROCK.get().defaultBlockState());
        rock(in, ir, 20, 14, 5, 4, ModBlocks.LIVE_ROCK.get().defaultBlockState());
        long seed = 5000;
        coral(in, ir, PlantSpecies.ACROPORA, 36, 34, seed++);
        coral(in, ir, PlantSpecies.HAMMER_CORAL, 28, 30, seed++);
        coral(in, ir, PlantSpecies.LOBOPHYLLIA, 44, 30, seed++);
        coral(in, ir, PlantSpecies.LEATHER_CORAL, 12, 32, seed++);
        coral(in, ir, PlantSpecies.GREEN_STAR_POLYPS, 54, 30, seed++);
        coral(in, ir, PlantSpecies.MUSHROOM_CORAL, 50, 16, seed++);
        coral(in, ir, PlantSpecies.ZOANTHUS, 8, 26, seed++);
        coral(in, ir, PlantSpecies.BUBBLE_TIP_ANEMONE, 20, 14, seed);
        for (int x = 30; x < 62; x++)
            for (int z = 4; z < 14; z++) {
                int k = (x * 7 + z * 13) % 9;
                if (k == 0) in.setBlock(ir.toWorld(x, 0, z), Blocks.TALL_SEAGRASS.defaultBlockState(), 2);
                else if (k == 4) in.setBlock(ir.toWorld(x, 0, z), Blocks.SEAGRASS.defaultBlockState(), 2);
            }
        for (int y = 0; y < 24; y++) in.setBlock(ir.toWorld(60, y, 10), (y == 23 ? Blocks.KELP : Blocks.KELP_PLANT).defaultBlockState(), 2);
        TankEquipment.invalidate(c);
        for (int i = 0; i < 2; i++) fish(level, c, TankmateSpecies.OCELLARIS_CLOWNFISH, TankmateGenes.wild(TankmateSpecies.OCELLARIS_CLOWNFISH), i == 0, r);
        for (int i = 0; i < 4; i++) fish(level, c, TankmateSpecies.GREEN_CHROMIS, TankmateGenes.wild(TankmateSpecies.GREEN_CHROMIS), i % 2 == 0, r);
        fish(level, c, TankmateSpecies.YELLOW_TANG, TankmateGenes.wild(TankmateSpecies.YELLOW_TANG), false, r);
        fish(level, c, TankmateSpecies.LINED_SEAHORSE, TankmateGenes.pure(TankmateSpecies.LINED_SEAHORSE, "yellow"), true, r);
        fish(level, c, TankmateSpecies.LINED_SEAHORSE, TankmateGenes.wild(TankmateSpecies.LINED_SEAHORSE), false, r);
        TankCritters.add(level, c, CritterSpecies.CLEANER_SHRIMP, CritterGenes.WILD, true, 10);
        for (int i = 0; i < 2; i++) TankCritters.add(level, c, CritterSpecies.HERMIT_CRAB, CritterGenes.WILD, i == 0, 10);
    }

    // ------------------------------------------------------------------ helpers

    private static void cabinet(ServerLevel level, BlockPos at) {
        level.setBlockAndUpdate(at, ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH));
        if (level.getBlockEntity(at) instanceof CabinetBlockEntity cab) cab.setWood(WOOD);
        level.setBlockAndUpdate(at, Block.updateFromNeighbourShapes(level.getBlockState(at), level, at));
    }

    private static TankBlockEntity tank(ServerLevel level, BlockPos min, int sx, int sy, int sz, float salt) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) {
            level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
            if (level.getBlockEntity(pos) instanceof TankBlockEntity be) be.setStyle(TankStyle.CLEAN);
        }
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        // the cabinets below join into one counter with the tank on top
        for (int x = -1; x <= sx; x++)
            for (int z = -1; z <= sz; z++) {
                BlockPos cp = min.offset(x, -1, z);
                if (level.getBlockState(cp).is(ModBlocks.CABINET.get())) level.setBlockAndUpdate(cp, Block.updateFromNeighbourShapes(level.getBlockState(cp), level, cp));
            }
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, (long) blocks.size() * 1000, salt);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setWaterTemp(salt > 20 ? 26F : 25F);
        c.getTank().setTargetTemp(salt > 20 ? 26F : 25F);
        c.getTank().setWaterQuality(1F);
        c.markTankChanged();
        return c;
    }

    private static InteriorRegion region(ServerLevel level, TankBlockEntity c, BlockState floor) {
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++)
            for (int z = 0; z < r.sizeZ(); z++) Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), floor, false);
        return r;
    }

    private static void rock(ServerLevel in, InteriorRegion r, int cx, int cz, int rad, int h, BlockState s) {
        for (int x = -rad; x <= rad; x++)
            for (int z = -rad; z <= rad; z++) {
                double d = Math.sqrt(x * x + z * z) / rad;
                if (d > 1.0 || cx + x < 0 || cz + z < 0 || cx + x >= r.sizeX() || cz + z >= r.sizeZ()) continue;
                int top = (int) Math.round(h * (1 - d * d * 0.7));
                for (int y = 0; y < top; y++) in.setBlock(r.toWorld(cx + x, y, cz + z), s, 2);
            }
    }

    private static int topAt(ServerLevel in, InteriorRegion r, int x, int z) {
        int y = 0;
        while (y < r.sizeY() && !in.getBlockState(r.toWorld(x, y, z)).isAir()) y++;
        return y;
    }

    /** Plants {@code s} fully grown at (x, z); y < 0: on top of whatever stands there. */
    private static void plant(ServerLevel in, InteriorRegion r, PlantSpecies s, int x, int y, int z, long seed) {
        int py = y < 0 ? topAt(in, r, x, z) : y;
        if (InteriorPlants.plantAt(in, r, s, x, py, z, s.stages(), seed) == null) LOGGER.warn("[smoketest] README {} not planted at {} {} {}", s.id(), x, py, z);
    }

    private static void coral(ServerLevel in, InteriorRegion r, PlantSpecies s, int x, int z, long seed) {
        InteriorPlant p = InteriorPlants.plantAt(in, r, s, x, topAt(in, r, x, z), z, s.stages(), seed);
        if (p == null) {
            LOGGER.warn("[smoketest] README {} not placed", s.id());
            return;
        }
        p.tint = 3;
        p.redness = 1;
        InteriorPlants.apply(in, r, p);
    }

    private static Map<String, String> example(String variety) {
        for (var v : BettaVarietyTable.VARIETIES) if (v.id().equals(variety)) return v.example();
        throw new IllegalArgumentException(variety);
    }

    private static void betta(ServerLevel level, TankBlockEntity c, String variety, boolean female) {
        BettaData d = BettaData.create(BettaGenome.of(example(variety), BettaGenome.DEFAULT), female, level.getGameTime());
        Component err = TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d),
                ModEntities.BETTA.get(), null, 0L);
        if (err != null) LOGGER.error("[smoketest] README betta {}: {}", variety, err.getString());
    }

    private static void fish(ServerLevel level, TankBlockEntity c, TankmateSpecies s, TankmateGenes genes, boolean female, RandomSource r) {
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        Vec3 p = w.clamp(new Vec3(w.box().minX + r.nextDouble() * (w.box().maxX - w.box().minX), w.box().minY + 0.3 + r.nextDouble() * 0.6,
                w.box().minZ + r.nextDouble() * (w.box().maxZ - w.box().minZ)), 0.35, 0.1, 0.2);
        var e = TankmateBreeding.spawn(level, c, s, genes, female, LifeStage.ADULT, 0, p);
        if (e == null) LOGGER.error("[smoketest] README {} not spawned", s.id());
        else e.health().feed();
    }

    /** No info overlay (Jade) in the pictures. */
    private static void reach(ServerPlayer p, boolean normal) {
        var b = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE);
        var e = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE);
        if (b != null) b.setBaseValue(normal ? 4.5 : 0.0);
        if (e != null) e.setBaseValue(normal ? 3.0 : 0.0);
    }

    private void openMenu(BlockPos at) {
        server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.serverLevel(), at);
            if (c == null) return;
            p.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new org.overengineer.bettacraft.tank.menu.TankMenu(id, inv, c),
                    Component.translatable("gui.bettacraft.tank.title")), buf -> buf.writeBlockPos(c.getBlockPos()));
        });
    }

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> {
            if (InteriorManager.isInterior(p.level())) p.teleportTo(p.server.overworld(), x, eyeY - p.getEyeHeight(), z, yaw, pitch);
            else p.teleportTo(p.serverLevel(), x, eyeY - p.getEyeHeight(), z, yaw, pitch);
        });
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
        if (seqLeft > 0) return false;
        if (steps.isEmpty()) {
            active = null;
            return true;
        }
        Step s = steps.peek();
        if (wait < s.delay()) {
            wait++;
            return false;
        }
        steps.poll();
        wait = 0;
        s.action().run();
        return false;
    }

    private void server(java.util.function.Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        mc.getToasts().clear();
        String file = "readme_" + name + (shaders ? "_shaders" : "") + ".png";
        Screenshot.grab(mc.gameDirectory, file, mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }

    /** Starts a frame sequence of {@code frames} pictures, one per game tick. */
    private void sequence(String name, int frames) {
        // quarter speed while recording: four times as many drawn frames per game tick, so every picture is close to its
        // tick (the animations of the mod run on game time); the steps wait until the sequence is complete
        server(p -> p.server.tickRateManager().setTickRate(5F));
        seqName = name;
        seqLeft = frames;
        seqIndex = 0;
        seqNext = -1;
    }

    private void frame(float partial) {
        if (seqLeft <= 0 || mc.level == null) return;
        double t = mc.level.getGameTime() + partial;
        if (seqNext < 0) seqNext = Math.ceil(t) + 1;
        if (t < seqNext) return;
        seqNext += 1;
        if (t > seqNext) seqNext = Math.floor(t) + 1; // dropped behind: keep the pace of the game
        mc.getToasts().clear();
        String file = String.format(java.util.Locale.ROOT, "readme_anim_%s_%03d.png", seqName, seqIndex++);
        Screenshot.grab(mc.gameDirectory, file, mc.getMainRenderTarget(), c -> {});
        if (--seqLeft == 0) server(p -> p.server.tickRateManager().setTickRate(20F));
        if (seqLeft == 0) LOGGER.info("[smoketest] README sequence {} done ({} frames)", seqName, seqIndex);
    }
}
