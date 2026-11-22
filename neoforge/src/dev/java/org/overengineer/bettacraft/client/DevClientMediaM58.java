package org.overengineer.bettacraft.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.aquarist.AquaristEntity;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.critters.TankTerrain;
import org.overengineer.bettacraft.decor.DecorItem;
import org.overengineer.bettacraft.decor.DecorType;
import org.overengineer.bettacraft.entity.frog.DwarfFrogEntity;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.monitor.DashboardLayout;
import org.overengineer.bettacraft.monitor.DashboardTile;
import org.overengineer.bettacraft.monitor.MonitorBlock;
import org.overengineer.bettacraft.monitor.MonitorBlockEntity;
import org.overengineer.bettacraft.monitor.MonitorValue;
import org.overengineer.bettacraft.monitor.TankHistory;
import org.overengineer.bettacraft.plants.InteriorPlant;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.Mangroves;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.plants.wild.WildFloatingPlantBlock;
import org.overengineer.bettacraft.plants.wild.WildLandPlantBlock;
import org.overengineer.bettacraft.plants.wild.WildPlantKind;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankUsers;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tank.plants.TankPlants;
import org.overengineer.bettacraft.tankmates.Archerfish;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.terrarium.Climate;
import org.overengineer.bettacraft.terrarium.HerpEntity;
import org.overengineer.bettacraft.terrarium.HerpSpecies;
import org.overengineer.bettacraft.terrarium.TankClimate;
import org.overengineer.bettacraft.terrarium.TerrariumBoxItem;
import org.overengineer.bettacraft.villager.ModVillagers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M58: the scenes of the media run that the README, FEATURES.md and the mod page were missing ({@code -Psmoketestm43},
 * part {@code m58}; with {@code -Pcreate} also part {@code create}). Built beside the README room of
 * {@link DevClientSmokeReadme} in the copy of the showcase world, each tank in front of its own wall:
 * <ul>
 *     <li>a rainforest paludarium with dart frogs, bromeliads, ferns and Endler guppies (still and frame sequence);</li>
 *     <li>a brackish mangrove paludarium with archerfish that shoot at the crown and mudskippers (still and sequence);</li>
 *     <li>a desert terrarium with cacti, succulents, leopard geckos and a corn snake walking (still and sequence);</li>
 *     <li>a seahorse tank with lined and dwarf seahorses on seagrass and corals (still and sequence);</li>
 *     <li>a swamp paludarium with Minecraft frogs, dwarf clawed frogs, a betta and carnivorous plants;</li>
 *     <li>a rainforest terrarium full of the terrarium plants with crested geckos, from outside and inside;</li>
 *     <li>a big sea water paludarium with a sea turtle on its beach, a dolphin tank;</li>
 *     <li>the shark, the eagle ray and the wels catfish of the showcase's big tanks one by one;</li>
 *     <li>the aquarist villager at his breeding basin, the hired aquarist at work at his employer's tanks;</li>
 *     <li>a monitor wall with a dashboard between two tanks;</li>
 *     <li>a pond in the world with the wild water plants and the wild terrarium plants on its bank;</li>
 *     <li>the tank menu's users tab, a chest of animal containers with their icons;</li>
 *     <li>part create: a planted tank on a Create minecart contraption in a curve (the water sloshes).</li>
 * </ul>
 * Stills {@code readme_m58_*.png} (with a shader pack {@code _shaders}), frame sequences {@code readme_anim_m58_*}.
 */
final class DevClientMediaM58 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private final DevClientSmokeReadme r;
    private BlockPos line, palu, mangrove, desert, seahorses, swamp, terra, villager, hired, monitors, pond, turtle, dolphin;
    private BlockPos railStart, curve;
    private Entity cart;
    private boolean pushing;
    private volatile boolean cartInCurve;
    private final List<net.minecraft.world.item.ItemStack> icons = new ArrayList<>();

    /** Animals held at their pose for a picture (the big ones are moved by the tank itself, also without AI). */
    private record Hold(Entity e, double x, double y, double z, float yaw) {}
    private final List<Hold> holds = java.util.Collections.synchronizedList(new ArrayList<>());
    private Villager trader;

    /** Every client tick: the held animals back to their poses (on the server). */
    void tickHolds() {
        if (holds.isEmpty()) return;
        List<Hold> now;
        synchronized (holds) {
            now = List.copyOf(holds);
        }
        r.server(p -> {
            for (Hold h : now) {
                place(h.e(), h.x(), h.y(), h.z(), h.yaw());
                h.e().setDeltaMovement(Vec3.ZERO);
            }
        });
        // the client's copy too: a villager without AI turned its body on the client only (the server had the pose)
        if (r.mc.level != null) for (Hold h : now) {
            Entity c = r.mc.level.getEntity(h.e().getId());
            if (c instanceof LivingEntity le) {
                le.setYRot(h.yaw());
                le.yRotO = h.yaw();
                le.setYHeadRot(h.yaw());
                le.yHeadRotO = h.yaw();
                le.yBodyRot = h.yaw();
                le.yBodyRotO = h.yaw();
            }
        }
    }

    private void hold(Mob m, double x, double y, double z, float yaw) {
        pose(m, x, y, z, yaw);
        holds.add(new Hold(m, x, y, z, yaw));
    }

    DevClientMediaM58(DevClientSmokeReadme readme) {
        this.r = readme;
    }

    // ------------------------------------------------------------------ building (server thread)

    void build(ServerPlayer p) {
        ServerLevel level = level(p);
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        if (r.parts.contains("m58")) {
            line = r.base.offset(0, 1, -95);
            ground(level, line.offset(-8, -1, -6), 96, 12);
            palu = line;
            buildPaludarium(level, in);
            mangrove = line.offset(8, 0, 0);
            buildMangrove(level, in);
            desert = line.offset(17, 0, 0);
            buildDesert(level, in);
            seahorses = line.offset(25, 0, 0);
            buildSeahorses(level, in);
            swamp = line.offset(32, 0, 0);
            buildSwamp(level, in);
            terra = line.offset(40, 0, 0);
            buildTerrarium(level, in);
            villager = line.offset(49, 0, 0);
            buildVillager(level);
            hired = line.offset(59, 0, 0);
            buildHired(level, p);
            monitors = line.offset(72, 0, 0);
            buildMonitors(level, p);
            pond = r.base.offset(-30, 0, -95);
            buildPond(level);
            BlockPos second = r.base.offset(0, 1, -130);
            ground(level, second.offset(-8, -1, -24), 70, 30);
            turtle = second;
            buildTurtle(level, in);
            dolphin = second.offset(20, 0, -16);
            buildDolphin(level, in);
            users(level, p);
        }
        if (r.parts.contains("create")) buildCreate(level, p);
        LOGGER.info("[smoketest] media M58 built at {}", line);
    }

    /** A flat plank floor (the flat world's grass is the default) cleared above. */
    private static void ground(ServerLevel level, BlockPos min, int sx, int sz) {
        for (int x = 0; x < sx; x++) for (int z = 0; z < sz; z++) {
            for (int y = 1; y < 12; y++) level.setBlock(min.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(min.offset(x, 0, z), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 2);
        }
    }

    /** A wall behind a tank (one block behind its back), {@code w} wide, {@code h} high from the floor. */
    private static void wall(ServerLevel level, BlockPos min, int sx, int sz, int h, BlockState s) {
        for (int x = -2; x < sx + 2; x++) for (int y = -1; y < h; y++) level.setBlock(min.offset(x, y, sz + 1), s, 2);
    }

    private static void counter(ServerLevel level, BlockPos min, int sx, int sz) {
        for (int x = 0; x < sx; x++) for (int z = 0; z < sz; z++) DevClientSmokeReadme.cabinet(level, min.offset(x, -1, z));
    }

    private static void lights(ServerLevel level, BlockPos min, int sx, int y, int z, DyeColor color, boolean actinic) {
        for (int x = 0; x < sx; x++) {
            level.setBlockAndUpdate(min.offset(x, y, z), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < sx - 1).setValue(TankLightBlock.LIT, true)
                    .setValue(TankLightBlock.COLOR, actinic ? (x % 2 == 0 ? DyeColor.BLUE : DyeColor.WHITE) : color));
        }
    }

    private static void box(ServerLevel in, InteriorRegion ir, int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (int x = Math.max(0, x0); x <= Math.min(ir.sizeX() - 1, x1); x++)
            for (int y = y0; y <= y1; y++)
                for (int z = Math.max(0, z0); z <= Math.min(ir.sizeZ() - 1, z1); z++) in.setBlock(ir.toWorld(x, y, z), s, 2);
    }

    /** A plant on the first free block of its column (fully grown). */
    private static InteriorPlant grow(ServerLevel in, InteriorRegion ir, PlantSpecies s, int x, int z, long seed) {
        int y = DevClientSmokeReadme.topAt(in, ir, x, z);
        InteriorPlant p = InteriorPlants.plantAt(in, ir, s, x, y, z, s.stages(), seed);
        if (p == null) LOGGER.warn("[smoketest] media M58 {} not planted at {} {} {}", s.id(), x, y, z);
        return p;
    }

    /** A rising bank: from interior z {@code from} (height {@code low}) to the back (height {@code high}), soil with a top. */
    private static void bank(ServerLevel in, InteriorRegion ir, int from, int low, int high, BlockState top, BlockState fill) {
        for (int x = 0; x < ir.sizeX(); x++)
            for (int z = from; z < ir.sizeZ(); z++) {
                double t = (z - from) / (double) Math.max(1, ir.sizeZ() - 1 - from);
                int h = (int) Math.round(low + (high - low) * Math.sqrt(t) + 1.5 * Math.sin(x * 0.21) * t);
                for (int y = 0; y < h; y++) in.setBlock(ir.toWorld(x, y, z), y == h - 1 ? top : fill, 2);
            }
    }

    private void buildPaludarium(ServerLevel level, ServerLevel in) {
        // 4 x 3 x 2: water in front, a mossy bank at the back with bromeliads, ferns and orchids on a branch
        counter(level, palu, 4, 2);
        wall(level, palu, 4, 2, 5, Blocks.DEEPSLATE_TILES.defaultBlockState());
        TankBlockEntity c = DevClientSmokeReadme.tank(level, palu, 4, 3, 2, 0F);
        TankData t = c.getTank();
        InteriorRegion ir = DevClientSmokeReadme.region(level, c, Blocks.SAND.defaultBlockState());
        bank(in, ir, 11, 8, 24, Blocks.MOSS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState());
        DevClientSmokeReadme.rock(in, ir, 46, 10, 4, 6, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
        box(in, ir, 8, 22, 26, 30, 22, 27, Blocks.OAK_LOG.defaultBlockState());
        t.setWaterMb(t.capacityMb() * 22 / 100);
        long seed = 5800;
        PlantSpecies[] bank = {PlantSpecies.NEOREGELIA, PlantSpecies.FITTONIA, PlantSpecies.GUZMANIA, PlantSpecies.SELAGINELLA, PlantSpecies.VRIESEA,
                PlantSpecies.BEGONIA, PlantSpecies.PEPEROMIA, PlantSpecies.NEOREGELIA, PlantSpecies.CUSHION_MOSS};
        for (int k = 0; k < bank.length; k++) grow(in, ir, bank[k], 4 + k * 7, 17 + (k % 2) * 3, seed++);
        PlantSpecies[] back = {PlantSpecies.BOSTON_FERN, PlantSpecies.POTHOS, PlantSpecies.BIRDS_NEST_FERN, PlantSpecies.BOSTON_FERN, PlantSpecies.POTHOS};
        for (int k = 0; k < back.length; k++) grow(in, ir, back[k], 6 + k * 13, 29, seed++);
        grow(in, ir, PlantSpecies.MINI_ORCHID, 12, 26, seed++);
        grow(in, ir, PlantSpecies.TILLANDSIA, 20, 27, seed++);
        grow(in, ir, PlantSpecies.MARCGRAVIA, 27, 26, seed++);
        grow(in, ir, PlantSpecies.CRYPT_GREEN, 10, 5, seed++);
        grow(in, ir, PlantSpecies.CRYPT_BROWN, 30, 4, seed++);
        grow(in, ir, PlantSpecies.JAVA_FERN, 46, 10, seed++);
        grow(in, ir, PlantSpecies.ANUBIAS_NANA, 50, 7, seed++);
        InteriorPlants.settleTint(level, c);
        RandomSource fr = RandomSource.create(5801);
        for (int i = 0; i < 6; i++) TankPlants.add(c, i % 2 == 0 ? FloatingPlantType.FROGBIT : FloatingPlantType.SALVINIA,
                palu.getX() + 0.4 + fr.nextDouble() * 3.2, palu.getZ() + 0.2 + fr.nextDouble() * 0.5, 0.7F + fr.nextFloat() * 0.3F, fr.nextFloat() * 6.28F);
        lights(level, palu, 4, 3, 1, DyeColor.WHITE, false);
        level.setBlockAndUpdate(palu.offset(0, 3, 0), ModBlocks.MISTER.get().defaultBlockState());
        TankClimate.step(level, c, 0.01);
        t.setClimate(new Climate.State(25, 25, 25, 92, 0, 0));
        c.markTankChanged();
        for (int i = 0; i < 5; i++) herp(level, c, HerpSpecies.DART_FROG, i % 2 == 0, i % 3);
        for (int i = 0; i < 6; i++) DevClientSmokeReadme.fish(level, c, TankmateSpecies.ENDLER_GUPPY, TankmateGenes.wild(TankmateSpecies.ENDLER_GUPPY), i % 2 == 0, fr);
        LOGGER.info("[smoketest] media M58 paludarium mode {} land {}", t.terrariumMode(), t.landShare());
    }

    private void buildMangrove(ServerLevel level, ServerLevel in) {
        // 5 x 3 x 3 brackish: mud beach on the right, a grown mangrove, stilt roots, archerfish, mudskippers, Amano shrimp
        counter(level, mangrove, 5, 3);
        wall(level, mangrove, 5, 3, 5, Blocks.DEEPSLATE_TILES.defaultBlockState());
        TankBlockEntity c = DevClientSmokeReadme.tank(level, mangrove, 5, 3, 3, 12F);
        c.getTank().setWaterTemp(27F);
        c.getTank().setTargetTemp(27F);
        c.getTank().setWaterMb(5 * 3 * 3 * 1000L * 45 / 100);
        InteriorRegion ir = DevClientSmokeReadme.region(level, c, Blocks.MUD.defaultBlockState());
        for (int x = 46; x < ir.sizeX(); x++) for (int z = 0; z < ir.sizeZ(); z++) {
            int top = 14 + (x - 46) / 2 + (int) Math.round(1.5 * Math.sin(z * 0.35));
            for (int y = 0; y < top; y++) in.setBlock(ir.toWorld(x, y, z), y >= top - 2 ? Blocks.MUD.defaultBlockState() : Blocks.PACKED_MUD.defaultBlockState(), 2);
        }
        DecorItem.place(in, DecorType.MANGROVE_STILT_ROOTS, ir.toWorld(40, 0, 30), net.minecraft.world.level.block.Rotation.NONE, null);
        lights(level, mangrove, 5, 3, 2, DyeColor.WHITE, false);
        TankClimate.step(level, c, 0.01);
        c.getTank().setClimate(new Climate.State(28, 28, 28, 90, 0, 0));
        String err = Mangroves.plant(level, c, 24, 22);
        if (err != null) LOGGER.error("[smoketest] media M58 mangrove not planted: {}", err);
        else {
            Mangroves.Tree m = c.getTank().mangroves().getFirst();
            Mangroves.grow(level, c, ir, in, m, Mangroves.STAGES);
        }
        TankClimate.step(level, c, 0.01);
        RandomSource fr = RandomSource.create(5810);
        for (int i = 0; i < 3; i++) DevClientSmokeReadme.fish(level, c, TankmateSpecies.ARCHERFISH, TankmateGenes.wild(TankmateSpecies.ARCHERFISH), i == 1, fr);
        for (int i = 0; i < 3; i++) herpByBox(level, c, HerpSpecies.MUDSKIPPER);
        for (int i = 0; i < 4; i++) TankCritters.add(level, c, CritterSpecies.AMANO_SHRIMP, CritterGenes.WILD, i % 2 == 0, 20);
        c.markTankChanged();
    }

    private void buildDesert(ServerLevel level, ServerLevel in) {
        // 4 x 2 x 3 dry: sand, sandstone rocks, cacti and succulents, a basking lamp and a heat mat, geckos and a corn snake
        counter(level, desert, 4, 3);
        wall(level, desert, 4, 3, 4, Blocks.SMOOTH_SANDSTONE.defaultBlockState());
        TankBlockEntity c = DevClientSmokeReadme.tank(level, desert, 4, 2, 3, 0F);
        TankInteractions.changeWater(level, c, -c.getTank().waterMb());
        InteriorRegion ir = DevClientSmokeReadme.region(level, c, Blocks.SAND.defaultBlockState());
        DevClientSmokeReadme.rock(in, ir, 44, 36, 10, 9, Blocks.SANDSTONE.defaultBlockState());
        DevClientSmokeReadme.rock(in, ir, 14, 30, 6, 5, Blocks.SMOOTH_SANDSTONE.defaultBlockState());
        DevClientSmokeReadme.rock(in, ir, 54, 12, 4, 3, Blocks.TERRACOTTA.defaultBlockState());
        long seed = 5820;
        PlantSpecies[] dry = {PlantSpecies.COLUMN_CACTUS, PlantSpecies.BARREL_CACTUS, PlantSpecies.AGAVE, PlantSpecies.PRICKLY_PEAR, PlantSpecies.ALOE,
                PlantSpecies.MAMMILLARIA, PlantSpecies.ECHEVERIA, PlantSpecies.HAWORTHIA, PlantSpecies.LITHOPS, PlantSpecies.SEDUM,
                PlantSpecies.BLUE_GRAMA, PlantSpecies.NEEDLEGRASS};
        int[][] at = {{6, 40}, {24, 42}, {60, 40}, {34, 30}, {4, 22}, {22, 20}, {40, 16}, {12, 10}, {30, 8}, {48, 6}, {58, 22}, {28, 40}};
        for (int k = 0; k < dry.length; k++) grow(in, ir, dry[k], at[k][0], at[k][1], seed++);
        level.setBlockAndUpdate(desert.offset(2, 2, 1), ModBlocks.BASKING_LAMP.get().defaultBlockState().setValue(TankLightBlock.LIT, true));
        lights(level, desert.offset(0, 0, 0), 2, 2, 2, DyeColor.WHITE, false);
        TankClimate.step(level, c, 0.01);
        c.getTank().setClimate(new Climate.State(30, 30, 38, 25, 4, 0));
        c.markTankChanged();
        herp(level, c, HerpSpecies.LEOPARD_GECKO, true, 0);
        herp(level, c, HerpSpecies.LEOPARD_GECKO, false, 1);
        herp(level, c, HerpSpecies.LEOPARD_GECKO, true, 2);
        herp(level, c, HerpSpecies.CORN_SNAKE, false, 0);
    }

    private void buildSeahorses(ServerLevel level, ServerLevel in) {
        // 3 x 2 x 2 sea water (33 ppt): sand, live rock, seagrass and kelp to hold on to, soft corals; lined and dwarf seahorses
        counter(level, seahorses, 3, 2);
        wall(level, seahorses, 3, 2, 4, Blocks.BLUE_TERRACOTTA.defaultBlockState());
        TankBlockEntity c = DevClientSmokeReadme.tank(level, seahorses, 3, 2, 2, 33F);
        InteriorRegion ir = DevClientSmokeReadme.region(level, c, Blocks.SAND.defaultBlockState());
        DevClientSmokeReadme.rock(in, ir, 36, 24, 9, 9, ModBlocks.LIVE_ROCK.get().defaultBlockState());
        DevClientSmokeReadme.rock(in, ir, 8, 26, 6, 6, ModBlocks.LIVE_ROCK.get().defaultBlockState());
        long seed = 5830;
        DevClientSmokeReadme.coral(in, ir, PlantSpecies.LEATHER_CORAL, 36, 24, seed++);
        DevClientSmokeReadme.coral(in, ir, PlantSpecies.GREEN_STAR_POLYPS, 8, 26, seed++);
        DevClientSmokeReadme.coral(in, ir, PlantSpecies.ZOANTHUS, 30, 18, seed++);
        DevClientSmokeReadme.coral(in, ir, PlantSpecies.MUSHROOM_CORAL, 42, 16, seed);
        for (int x = 2; x < ir.sizeX() - 2; x++)
            for (int z = 2; z < 16; z++) {
                int k = (x * 7 + z * 13) % 7;
                if (k == 0) in.setBlock(ir.toWorld(x, 0, z), Blocks.TALL_SEAGRASS.defaultBlockState(), 2);
                else if (k == 3) in.setBlock(ir.toWorld(x, 0, z), Blocks.SEAGRASS.defaultBlockState(), 2);
            }
        for (int[] k : new int[][]{{18, 28, 26}, {24, 29, 20}, {44, 29, 24}}) {
            for (int y = 0; y < k[2]; y++) in.setBlock(ir.toWorld(k[0], y, k[1]), (y == k[2] - 1 ? Blocks.KELP : Blocks.KELP_PLANT).defaultBlockState(), 2);
        }
        lights(level, seahorses, 3, 2, 1, DyeColor.WHITE, true);
        org.overengineer.bettacraft.tank.TankEquipment.invalidate(c);
        RandomSource fr = RandomSource.create(5831);
        String[] colours = {"yellow", "orange", "wild", "yellow"};
        for (int i = 0; i < colours.length; i++)
            DevClientSmokeReadme.fish(level, c, TankmateSpecies.LINED_SEAHORSE, TankmateGenes.pure(TankmateSpecies.LINED_SEAHORSE, colours[i]), i % 2 == 0, fr);
        for (int i = 0; i < 3; i++) DevClientSmokeReadme.fish(level, c, TankmateSpecies.DWARF_SEAHORSE, TankmateGenes.wild(TankmateSpecies.DWARF_SEAHORSE), i % 2 == 0, fr);
        TankCritters.add(level, c, CritterSpecies.CLEANER_SHRIMP, CritterGenes.WILD, true, 10);
        c.markTankChanged();
    }

    private void buildSwamp(ServerLevel level, ServerLevel in) {
        // 4 x 2 x 3 swamp paludarium: muddy water in front with lily pads, a bank with carnivorous plants and sedges;
        // Minecraft frogs on the bank, dwarf clawed frogs and a wild-type betta in the water
        counter(level, swamp, 4, 3);
        wall(level, swamp, 4, 3, 4, Blocks.MUD_BRICKS.defaultBlockState());
        TankBlockEntity c = DevClientSmokeReadme.tank(level, swamp, 4, 2, 3, 0F);
        TankData t = c.getTank();
        t.setWaterTemp(26F);
        t.setTargetTemp(26F);
        InteriorRegion ir = DevClientSmokeReadme.region(level, c, Blocks.MUD.defaultBlockState());
        bank(in, ir, 20, 6, 20, Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.MUD.defaultBlockState());
        t.setWaterMb(t.capacityMb() * 30 / 100);
        long seed = 5840;
        PlantSpecies[] bank = {PlantSpecies.SARRACENIA, PlantSpecies.SUNDEW, PlantSpecies.VENUS_FLYTRAP, PlantSpecies.BUTTERWORT, PlantSpecies.NEPENTHES,
                PlantSpecies.SARRACENIA, PlantSpecies.SUNDEW};
        for (int k = 0; k < bank.length; k++) grow(in, ir, bank[k], 4 + k * 9, 30 + (k % 2) * 3, seed++);
        PlantSpecies[] back = {PlantSpecies.PAPYRUS, PlantSpecies.SWAMP_SEDGE, PlantSpecies.SOFT_RUSH, PlantSpecies.UMBRELLA_SEDGE, PlantSpecies.PAPYRUS,
                PlantSpecies.SWAMP_SEDGE};
        for (int k = 0; k < back.length; k++) grow(in, ir, back[k], 6 + k * 10, 42, seed++);
        grow(in, ir, PlantSpecies.AMAZON_SWORD, 56, 25, seed++); // emersed at the edge
        grow(in, ir, PlantSpecies.CRYPT_GREEN, 12, 8, seed++);
        grow(in, ir, PlantSpecies.VALLISNERIA, 30, 12, seed++);
        grow(in, ir, PlantSpecies.VALLISNERIA, 44, 10, seed++);
        InteriorPlants.settleTint(level, c);
        RandomSource fr = RandomSource.create(5841);
        for (int i = 0; i < 5; i++) TankPlants.add(c, i < 3 ? FloatingPlantType.LILY_PAD : FloatingPlantType.DUCKWEED,
                swamp.getX() + 0.4 + fr.nextDouble() * 3.2, swamp.getZ() + 0.2 + fr.nextDouble() * 0.9, 0.8F + fr.nextFloat() * 0.2F, fr.nextFloat() * 6.28F);
        lights(level, swamp, 4, 2, 2, DyeColor.WHITE, false);
        TankClimate.step(level, c, 0.01);
        t.setClimate(new Climate.State(25, 25, 25, 85, 0, 0));
        c.markTankChanged();
        for (int v = 0; v < 3; v++) vanillaFrog(level, c, v);
        for (int i = 0; i < 2; i++) dwarfFrog(level, c, i == 0, new Vec3(swamp.getX() + 1.2 + i * 1.3, swamp.getY() + 0.25, swamp.getZ() + 0.5));
        DevClientSmokeReadme.betta(level, c, "koi_galaxy", false);
        LOGGER.info("[smoketest] media M58 swamp mode {} land {}", t.terrariumMode(), t.landShare());
    }

    private void buildTerrarium(ServerLevel level, ServerLevel in) {
        // 4 x 3 x 2 rainforest terrarium: soil, a mossy back, a branch with epiphytes, every tropical terrarium plant
        counter(level, terra, 4, 2);
        wall(level, terra, 4, 2, 5, Blocks.DEEPSLATE_TILES.defaultBlockState());
        TankBlockEntity c = DevClientSmokeReadme.tank(level, terra, 4, 3, 2, 0F);
        TankInteractions.changeWater(level, c, -c.getTank().waterMb());
        InteriorRegion ir = DevClientSmokeReadme.region(level, c, Blocks.DIRT.defaultBlockState());
        bank(in, ir, 0, 2, 12, Blocks.MOSS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState());
        box(in, ir, 34, 10, 24, 58, 12, 26, Blocks.OAK_LOG.defaultBlockState());
        box(in, ir, 10, 6, 28, 12, 30, 30, Blocks.OAK_LOG.defaultBlockState());
        long seed = 5850;
        PlantSpecies[] front = {PlantSpecies.FITTONIA, PlantSpecies.NEOREGELIA, PlantSpecies.SELAGINELLA, PlantSpecies.GUZMANIA, PlantSpecies.PEPEROMIA,
                PlantSpecies.VRIESEA, PlantSpecies.CUSHION_MOSS, PlantSpecies.BEGONIA, PlantSpecies.REINDEER_LICHEN};
        for (int k = 0; k < front.length; k++) grow(in, ir, front[k], 4 + k * 7, 6 + (k % 2) * 6, seed++);
        PlantSpecies[] back = {PlantSpecies.BIRDS_NEST_FERN, PlantSpecies.POTHOS, PlantSpecies.BOSTON_FERN, PlantSpecies.BIRDS_NEST_FERN, PlantSpecies.POTHOS};
        for (int k = 0; k < back.length; k++) grow(in, ir, back[k], 4 + k * 13, 21, seed++);
        grow(in, ir, PlantSpecies.MINI_ORCHID, 38, 25, seed++);
        grow(in, ir, PlantSpecies.TILLANDSIA, 46, 25, seed++);
        grow(in, ir, PlantSpecies.MARCGRAVIA, 54, 25, seed++);
        grow(in, ir, PlantSpecies.TILLANDSIA, 11, 29, seed++);
        lights(level, terra, 4, 3, 1, DyeColor.WHITE, false);
        level.setBlockAndUpdate(terra.offset(3, 3, 0), ModBlocks.MISTER.get().defaultBlockState());
        TankClimate.step(level, c, 0.01);
        c.getTank().setClimate(new Climate.State(25, 25, 25, 88, 0, 0));
        InteriorPlants.settleTint(level, c);
        c.markTankChanged();
        herp(level, c, HerpSpecies.CRESTED_GECKO, true, 0);
        herp(level, c, HerpSpecies.CRESTED_GECKO, false, 1);
    }

    private void buildVillager(ServerLevel level) {
        // a market corner: a breeding basin, barrels, a lantern, two small tanks with bettas, the aquarist (master) behind
        BlockPos b = villager;
        for (int x = -1; x < 9; x++) for (int z = -2; z < 4; z++) level.setBlock(b.offset(x, -1, z), Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
        for (int x = -1; x < 9; x++) for (int y = 0; y < 4; y++) level.setBlock(b.offset(x, y, 3), (y == 3 ? Blocks.SPRUCE_SLAB : Blocks.STRIPPED_SPRUCE_LOG).defaultBlockState(), 2);
        level.setBlock(b.offset(2, 0, 1), ModBlocks.BREEDING_BASIN.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.NORTH), 3);
        level.setBlock(b.offset(-1, 0, 2), Blocks.BARREL.defaultBlockState(), 2);
        level.setBlock(b.offset(-1, 1, 2), Blocks.BARREL.defaultBlockState(), 2);
        level.setBlock(b.offset(0, 0, 2), Blocks.LANTERN.defaultBlockState(), 2);
        // two tanks a block apart (touching tanks would join into one)
        for (int k = 0; k < 2; k++) {
            BlockPos tk = b.offset(4 + k * 3, 0, 1);
            DevClientSmokeReadme.cabinet(level, tk.offset(0, -1, 0));
            DevClientSmokeReadme.cabinet(level, tk.offset(1, -1, 0));
        }
        level.setBlock(b.offset(6, 0, 1), Blocks.POTTED_FERN.defaultBlockState(), 2);
        BlockPos tk = b.offset(4, 0, 1);
        TankBlockEntity a = DevClientSmokeReadme.tank(level, tk, 2, 1, 1, 0F);
        TankBlockEntity c = DevClientSmokeReadme.tank(level, tk.offset(3, 0, 0), 2, 1, 1, 0F);
        for (TankBlockEntity t : List.of(a, c)) {
            InteriorRegion ir = DevClientSmokeReadme.region(level, t, Blocks.SAND.defaultBlockState());
            ServerLevel in = InteriorManager.interiorLevel(level.getServer());
            DevClientSmokeReadme.plant(in, ir, PlantSpecies.JAVA_FERN, 6, 0, 10, 5860);
            DevClientSmokeReadme.plant(in, ir, PlantSpecies.CRYPT_GREEN, 24, 0, 10, 5861);
            InteriorPlants.settleTint(level, t);
        }
        DevClientSmokeReadme.betta(level, a, "black_orchid", false);
        DevClientSmokeReadme.betta(level, c, "nemo", false);
        Villager v = create(EntityType.VILLAGER, level);
        if (v == null) return;
        place(v, b.getX() + 2.5, b.getY(), b.getZ() + 2.0, 180F);
        aquarist(v);
        v.setPersistenceRequired();
        level.addFreshEntity(v);
        SmokeFreeze.freeze(v, false);
        trader = v;
    }

    private void buildHired(ServerLevel level, ServerPlayer p) {
        // his workplace beside two tanks of his employer: the basin, a chest, a reservoir; he feeds the left tank
        BlockPos b = hired;
        for (int x = -1; x < 12; x++) for (int z = -3; z < 4; z++) level.setBlock(b.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        TankBlockEntity a = DevClientSmokeReadme.tank(level, b.offset(1, 0, 1), 3, 2, 2, 0F);
        TankBlockEntity c = DevClientSmokeReadme.tank(level, b.offset(6, 0, 1), 3, 2, 2, 0F);
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        long seed = 5870;
        for (TankBlockEntity t : List.of(a, c)) {
            InteriorRegion ir = DevClientSmokeReadme.region(level, t, Blocks.SAND.defaultBlockState());
            for (int x = 4; x < 46; x += 8) DevClientSmokeReadme.plant(in, ir, PlantSpecies.VALLISNERIA, x, 0, 26, seed++);
            DevClientSmokeReadme.plant(in, ir, PlantSpecies.AMAZON_SWORD, 12, 0, 14, seed++);
            DevClientSmokeReadme.plant(in, ir, PlantSpecies.CRYPT_GREEN, 34, 0, 10, seed++);
            InteriorPlants.settleTint(level, t);
            t.getTank().setOwner(p.getUUID());
        }
        RandomSource fr = RandomSource.create(5871);
        DevClientSmokeReadme.betta(level, a, "mustard_gas", false);
        for (int i = 0; i < 8; i++) DevClientSmokeReadme.fish(level, c, TankmateSpecies.NEON_TETRA, TankmateGenes.wild(TankmateSpecies.NEON_TETRA), i % 2 == 0, fr);
        for (int i = 0; i < 3; i++) DevClientSmokeReadme.fish(level, c, TankmateSpecies.PANDA_CORY, TankmateGenes.wild(TankmateSpecies.PANDA_CORY), i % 2 == 0, fr);
        level.setBlock(b.offset(0, 0, 3), ModBlocks.BREEDING_BASIN.get().defaultBlockState(), 3);
        level.setBlock(b.offset(4, 0, 2), Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(b.offset(10, 0, 2), ModBlocks.RESERVOIR.get().defaultBlockState(), 3);
        AquaristEntity e = create(ModEntities.HIRED_AQUARIST.get(), level);
        if (e == null) return;
        e.setVariant(1);
        e.setCustomName(null);
        e.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(ModItems.BETTA_PELLETS.get()));
        place(e, b.getX() + 2.2, b.getY(), b.getZ() - 0.1, 200F);
        e.setNoAi(true);
        level.addFreshEntity(e);
        AquaristEntity e2 = create(ModEntities.HIRED_AQUARIST.get(), level);
        if (e2 == null) return;
        e2.setVariant(3);
        e2.setCustomName(null);
        e2.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(ModItems.FISH_BAG.get()));
        place(e2, b.getX() + 7.6, b.getY(), b.getZ() - 0.2, 160F);
        e2.setNoAi(true);
        level.addFreshEntity(e2);
    }

    private void buildMonitors(ServerLevel level, ServerPlayer p) {
        // a 4 x 2 monitor wall between a betta tank and a reef tank, a day of history, a dashboard with every widget
        BlockPos b = monitors;
        wall(level, b, 13, 2, 5, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        counter(level, b, 3, 2);
        counter(level, b.offset(10, 0, 0), 3, 2);
        TankBlockEntity a = DevClientSmokeReadme.tank(level, b, 3, 2, 2, 0F);
        TankBlockEntity c = DevClientSmokeReadme.tank(level, b.offset(10, 0, 0), 3, 2, 2, 35F);
        a.getTank().setName("Betta Paradise");
        c.getTank().setName("Reef");
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion ar = DevClientSmokeReadme.region(level, a, Blocks.SAND.defaultBlockState());
        for (int x = 4; x < 46; x += 7) DevClientSmokeReadme.plant(in, ar, PlantSpecies.VALLISNERIA, x, 0, 26, 5880 + x);
        DevClientSmokeReadme.plant(in, ar, PlantSpecies.CRYPT_FLAMINGO, 20, 0, 12, 5899);
        InteriorPlants.settleTint(level, a);
        InteriorRegion cr = DevClientSmokeReadme.region(level, c, Blocks.SAND.defaultBlockState());
        DevClientSmokeReadme.rock(in, cr, 24, 22, 9, 8, ModBlocks.LIVE_ROCK.get().defaultBlockState());
        DevClientSmokeReadme.coral(in, cr, PlantSpecies.ACROPORA, 24, 22, 5900);
        DevClientSmokeReadme.coral(in, cr, PlantSpecies.BUBBLE_TIP_ANEMONE, 10, 10, 5901);
        lights(level, b, 3, 2, 1, DyeColor.WHITE, false);
        lights(level, b.offset(10, 0, 0), 3, 2, 1, DyeColor.WHITE, true);
        RandomSource fr = RandomSource.create(5881);
        DevClientSmokeReadme.betta(level, a, "koi_galaxy", false);
        for (int i = 0; i < 6; i++) DevClientSmokeReadme.fish(level, a, TankmateSpecies.HARLEQUIN_RASBORA, TankmateGenes.wild(TankmateSpecies.HARLEQUIN_RASBORA), i % 2 == 0, fr);
        for (int i = 0; i < 2; i++) DevClientSmokeReadme.fish(level, c, TankmateSpecies.OCELLARIS_CLOWNFISH, TankmateGenes.wild(TankmateSpecies.OCELLARIS_CLOWNFISH), i == 0, fr);
        DevClientSmokeReadme.fish(level, c, TankmateSpecies.YELLOW_TANG, TankmateGenes.wild(TankmateSpecies.YELLOW_TANG), false, fr);
        for (TankBlockEntity t : List.of(a, c)) {
            t.getTank().setOwner(p.getUUID());
            t.markTankChanged();
        }
        long hour = TankHistory.hour(level.getGameTime());
        TankHistory h = TankHistory.get(level);
        for (TankBlockEntity t : List.of(a, c)) {
            float[] v = MonitorValue.sampleAll(level, t);
            for (int i = 0; i < 48; i++) {
                float[] pt = v.clone();
                pt[MonitorValue.TEMPERATURE.ordinal()] = v[MonitorValue.TEMPERATURE.ordinal()] + (float) Math.sin(i / 24.0 * Math.PI * 2) * 1.2F;
                h.record(t.getTank().id(), hour - 48 + i, pt);
            }
        }
        var s = ModBlocks.MONITOR.get().defaultBlockState().setValue(MonitorBlock.FACING, Direction.NORTH);
        for (int y = 2; y >= 1; y--) for (int x = 8; x >= 4; x--) level.setBlockAndUpdate(b.offset(x, y, 1), s);
        for (int x = 4; x <= 8; x++) level.setBlockAndUpdate(b.offset(x, 0, 1), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
    }

    /** The dashboard of the monitor wall (after the wall found its tanks). */
    private void dashboard(ServerLevel level) {
        if (!(level.getBlockEntity(monitors.offset(8, 2, 1)) instanceof MonitorBlockEntity m)) {
            LOGGER.error("[smoketest] media M58 no monitor at the top left");
            return;
        }
        UUID betta = null, reef = null;
        for (var k : m.tanks()) {
            if ("Betta Paradise".equals(k.name)) betta = k.id;
            if ("Reef".equals(k.name)) reef = k.id;
        }
        DashboardLayout l = new DashboardLayout();
        l.auto = false;
        l.tiles.add(new DashboardTile(l.nextId++, 0, 0, 5, 4, DashboardTile.STATUS, betta));
        l.tiles.add(DashboardTile.graph(l.nextId++, 5, 0, 5, 4, betta, 2, MonitorValue.TEMPERATURE, MonitorValue.WATER_QUALITY));
        l.tiles.add(DashboardTile.alarm(l.nextId++, 10, 0, 5, 2, reef, MonitorValue.TEMPERATURE, false, 24F, true));
        DashboardTile v = DashboardTile.value(l.nextId++, 10, 2, 5, 2, reef, MonitorValue.SALINITY, false);
        v.background = 0xFF0E2A5A;
        l.tiles.add(v);
        l.tiles.add(new DashboardTile(l.nextId++, 0, 4, 5, 4, DashboardTile.ANIMALS, betta));
        l.tiles.add(new DashboardTile(l.nextId++, 5, 4, 5, 4, DashboardTile.DEVICES, reef));
        l.tiles.add(new DashboardTile(l.nextId++, 10, 4, 5, 4, DashboardTile.LOG, null));
        m.setLayout(l);
    }

    private void buildPond(ServerLevel level) {
        // a pond in the world (20 x 12, two blocks deep): the wild water plants on its bed, floating plants on top, the wild
        // terrarium plants of the swamp and the shore on its bank
        BlockPos b = pond;
        for (int x = -6; x < 26; x++) for (int z = -6; z < 20; z++) {
            for (int y = 0; y < 10; y++) level.setBlock(b.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            double dx = (x - 10) / 10.5, dz = (z - 6) / 6.5;
            boolean water = dx * dx + dz * dz < 1.0;
            boolean deep = dx * dx + dz * dz < 0.55;
            level.setBlock(b.offset(x, -3, z), Blocks.DIRT.defaultBlockState(), 2);
            if (water) {
                level.setBlock(b.offset(x, -3, z), (Math.floorMod(x * 7 + z * 3, 3) == 0 ? Blocks.GRAVEL : Blocks.MUD).defaultBlockState(), 2);
                level.setBlock(b.offset(x, -2, z), deep ? Blocks.WATER.defaultBlockState() : Blocks.MUD.defaultBlockState(), 2);
                level.setBlock(b.offset(x, -1, z), Blocks.WATER.defaultBlockState(), 2);
            } else {
                level.setBlock(b.offset(x, -2, z), Blocks.DIRT.defaultBlockState(), 2);
                level.setBlock(b.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            }
        }
        // the water plants: stem plants, rosettes, carpets and Vallisneria where it is deep, mosses on stones
        List<PlantSpecies> wild = new ArrayList<>(DevClientSmokeM55.wildLooks().values());
        int i = 0;
        for (int x = 2; x < 19; x += 2)
            for (int z = 2; z < 11; z += 2) {
                BlockPos at = b.offset(x, -2, z);
                if (!level.getBlockState(at).is(Blocks.WATER) && !level.getBlockState(at.above()).is(Blocks.WATER)) continue;
                if (Math.floorMod(x * 5 + z * 11, 3) == 0) continue;
                PlantSpecies s = wild.get(i++ % wild.size());
                WildPlantKind k = WildPlantKind.of(s);
                BlockPos spot = level.getBlockState(at).is(Blocks.WATER) ? at : at.above();
                if (k == WildPlantKind.EPIPHYTE || k == WildPlantKind.MOSS && s != PlantSpecies.MARIMO)
                    level.setBlock(spot.below(), Blocks.COBBLESTONE.defaultBlockState(), 2);
                level.setBlock(spot, ModBlocks.WILD_PLANTS.get(k).get().withSpecies(s), 2);
            }
        int fx = 0;
        for (FloatingPlantType t : FloatingPlantType.values()) {
            if (!WildFloatingPlantBlock.TYPE.getPossibleValues().contains(t)) continue;
            for (int dx = 0; dx < 3; dx++) {
                BlockPos at = b.offset(4 + fx * 2 + dx % 2, 0, 9 + dx / 2 + fx % 2);
                if (level.getBlockState(at.below()).is(Blocks.WATER))
                    level.setBlock(at, ModBlocks.WILD_FLOATING_PLANT.get().defaultBlockState().setValue(WildFloatingPlantBlock.TYPE, t), 2);
            }
            fx++;
        }
        // the bank: the plants of the swamp and the shore
        int k = 0;
        for (PlantSpecies s : PlantSpecies.landSpecies()) {
            if (s.habitat() != PlantSpecies.Habitat.SWAMP) continue;
            double ang = Math.PI * (0.05 + 0.9 * (k++ % 14) / 13.0);
            int x = 10 + (int) Math.round(Math.cos(ang) * 11.5), z = 6 + (int) Math.round(Math.sin(ang) * 7.5);
            BlockPos at = b.offset(x, 0, z);
            if (!level.getBlockState(at.below()).is(Blocks.GRASS_BLOCK)) continue;
            BlockState st = ModBlocks.WILD_LAND_PLANTS.get(s).get().defaultBlockState();
            if (s.form() == PlantSpecies.Form.EPIPHYTE) level.setBlock(at.below(), Blocks.OAK_LOG.defaultBlockState(), 2);
            if (st.canSurvive(level, at)) level.setBlock(at, st.setValue(WildLandPlantBlock.TRIMMED, false), 2);
        }
        LOGGER.info("[smoketest] media M58 pond at {}", b);
    }

    private void buildTurtle(ServerLevel level, ServerLevel in) {
        // 12 x 3 x 8 sea water paludarium with a sand beach on the right third, a basking lamp, a sea turtle and a hatchling
        BlockPos b = turtle;
        wall(level, b, 12, 8, 5, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < 3; y++) for (int z = 0; z < 8; z++) for (int x = 0; x < 12; x++) blocks.add(b.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlock(pos, ModBlocks.TANK.get().defaultBlockState(), 2);
        TankData t = TankMultiblock.formAll(level, blocks);
        TankBlockEntity c = TankMultiblock.getController(level, b);
        t.setFront(Direction.NORTH);
        t.setSignVisible(false);
        t.setWaterTemp(26F);
        t.setTargetTemp(26F);
        InteriorRegion ir = DevClientSmokeReadme.region(level, c, Blocks.SAND.defaultBlockState());
        for (int x = 120; x < ir.sizeX(); x++) for (int z = 0; z < ir.sizeZ(); z++) {
            int h = x >= 150 ? 20 : 1 + (int) Math.round(19 * (x - 120) / 30.0);
            for (int y = 0; y < h; y++) in.setBlock(ir.toWorld(x, y, z), Blocks.SAND.defaultBlockState(), 2);
        }
        DevClientSmokeReadme.rock(in, ir, 40, 80, 14, 10, ModBlocks.LIVE_ROCK.get().defaultBlockState());
        for (int x = 4; x < 110; x++) for (int z = 4; z < 120; z++) if ((x * 7 + z * 13) % 11 == 0) in.setBlock(ir.toWorld(x, 0, z), Blocks.SEAGRASS.defaultBlockState(), 2);
        TankInteractions.changeWater(level, c, t.capacityMb() / 3, 35F);
        level.setBlock(b.offset(10, 3, 3), ModBlocks.BASKING_LAMP.get().defaultBlockState(), 3);
        c.markTankChanged();
        TankClimate.step(level, c, 0.01);
    }

    private void buildDolphin(ServerLevel level, ServerLevel in) {
        // 32 x 8 x 16 sea water (interior scale 8): sand, rocks, two dolphins
        BlockPos b = dolphin;
        wall(level, b, 32, 16, 10, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < 8; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 32; x++) blocks.add(b.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlock(pos, ModBlocks.TANK.get().defaultBlockState(), 2);
        TankData t = TankMultiblock.formAll(level, blocks);
        TankBlockEntity c = TankMultiblock.getController(level, b);
        TankInteractions.changeWater(level, c, t.capacityMb(), 35F);
        t.setFront(Direction.NORTH);
        t.setSignVisible(false);
        t.setWaterTemp(25F);
        t.setTargetTemp(25F);
        InteriorRegion ir = DevClientSmokeReadme.region(level, c, Blocks.SAND.defaultBlockState());
        for (int[] s : new int[][]{{40, 90, 10}, {200, 100, 14}, {120, 110, 8}})
            for (int x = -s[2]; x <= s[2]; x++)
                for (int z = -s[2]; z <= s[2]; z++) {
                    int h = (int) Math.round(s[2] * 1.2 * Math.max(0, 1 - (x * x + z * z) / (double) (s[2] * s[2])));
                    for (int y = 0; y < h; y++) in.setBlock(ir.toWorld(s[0] + x, y, s[1] + z), ModBlocks.LIVE_ROCK.get().defaultBlockState(), 2);
                }
        for (int i = 0; i < 2; i++) {
            Entity d = create(EntityType.DOLPHIN, level);
            if (d == null) continue;
            ItemStack tub = org.overengineer.bettacraft.big.TransportTubItem.fillWith(d);
            d.discard();
            Component err = TankInhabitants.insert(level, c, tub, EntityType.DOLPHIN, null, 0);
            if (err != null) LOGGER.error("[smoketest] media M58 dolphin refused: {}", err.getString());
        }
        c.markTankChanged();
    }

    /** The users of the paludarium (for the users tab picture). */
    private void users(ServerLevel level, ServerPlayer p) {
        TankBlockEntity c = TankMultiblock.getController(level, palu);
        if (c == null) return;
        TankData t = c.getTank();
        t.setOwner(p.getUUID());
        t.setName("Rainforest");
        UUID alice = UUID.nameUUIDFromBytes("media-alice".getBytes()), bob = UUID.nameUUIDFromBytes("media-bob".getBytes());
        t.users().put(alice, "Alice", TankUsers.Role.CARETAKER);
        t.users().put(bob, "Bob", TankUsers.Role.VIEWER);
        t.users().setWaypoint(alice, t.owner(), true);
        c.markTankChanged();
    }

    // ------------------------------------------------------------------ the Create minecart contraption

    private void buildCreate(ServerLevel level, ServerPlayer p) {
        Block assembler = BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse("create:cart_assembler"));
        if (assembler == Blocks.AIR) {
            LOGGER.error("[smoketest] media M58 part create needs Create (-Pcreate)");
            return;
        }
        railStart = r.base.offset(-60, 0, -60);
        curve = railStart.offset(14, 0, 0);
        for (int x = -6; x <= 20; x++) for (int z = -8; z <= 14; z++) {
            level.setBlock(railStart.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            for (int y = 0; y < 6; y++) level.setBlock(railStart.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
        BlockState ew = Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST);
        BlockState ns = Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH);
        for (int x = 0; x < 14; x++) level.setBlock(railStart.offset(x, 0, 0), ew, 2);
        level.setBlock(curve, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.SOUTH_WEST), 2);
        for (int z = 1; z <= 12; z++) level.setBlock(curve.offset(0, 0, z), ns, 2);
        BlockPos at = railStart.offset(3, 0, 0);
        level.setBlock(at, with(with(assembler.defaultBlockState(), "shape", RailShape.EAST_WEST), "powered", true), 3);
        level.setBlock(at.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        List<BlockPos> blocks = new ArrayList<>();
        for (int z = 0; z < 2; z++) for (int x = 0; x < 2; x++) blocks.add(at.offset(x, 1, z));
        for (BlockPos pos : blocks) level.setBlock(pos, ModBlocks.TANK.get().defaultBlockState(), 2);
        TankData t = TankMultiblock.formAll(level, blocks);
        TankBlockEntity c = TankMultiblock.getController(level, at.above());
        t.setFront(Direction.NORTH);
        t.setSignVisible(false);
        t.setOwner(p.getUUID());
        TankInteractions.changeWater(level, c, Math.round(t.capacityMb() * 0.8));
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion ir = DevClientSmokeReadme.region(level, c, Blocks.SAND.defaultBlockState());
        DevClientSmokeReadme.rock(in, ir, 20, 20, 5, 4, Blocks.STONE.defaultBlockState());
        for (int x = 3; x < 30; x += 6) DevClientSmokeReadme.plant(in, ir, PlantSpecies.VALLISNERIA, x, 0, 27, 5890 + x);
        DevClientSmokeReadme.plant(in, ir, PlantSpecies.CRYPT_GREEN, 8, 0, 10, 5895);
        InteriorPlants.settleTint(level, c);
        DevClientSmokeReadme.betta(level, c, "koi_galaxy", false);
        DevClientSmokeReadme.betta(level, c, "avatar", true);
        c.markTankChanged();
        LOGGER.info("[smoketest] media M58 Create rail at {}", railStart);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState with(BlockState s, String property, Comparable value) {
        for (Property<?> p : s.getProperties()) if (p.getName().equals(property)) return s.setValue((Property) p, value);
        return s;
    }

    private void startCart(ServerPlayer p) {
        ServerLevel level = level(p);
        cart = minecart(level, railStart.getX() + 0.5, railStart.getY() + 0.1, railStart.getZ() + 0.5);
        if (cart != null) level.addFreshEntity(cart);
        pushing = true;
    }

    private void pushCart() {
        if (!pushing) return;
        r.server(p -> {
            if (cart == null || !cart.isAlive()) return;
            Vec3 v = cart.getDeltaMovement();
            double len = v.horizontalDistance();
            Vec3 dir = len < 1e-3 ? new Vec3(1, 0, 0) : new Vec3(v.x / len, 0, v.z / len);
            cart.setDeltaMovement(dir.x * 0.3, v.y, dir.z * 0.3);
            Vec3 c = cart.position();
            if (c.x > curve.getX() - 0.2 && c.z > curve.getZ() + 0.3) cartInCurve = true;
        });
    }

    // ------------------------------------------------------------------ poses

    /** The giant {@code s} of a showcase big tank alone at the front glass (the others frozen at the back); the camera on it. */
    private void giant(String tank, TankmateSpecies s, String name, double dist, float yaw, double up, float pitch) {
        DevClientSmokeReadme.Box box = r.named.get(tank);
        if (box == null) {
            LOGGER.error("[smoketest] media M58: no showcase tank {}", tank);
            return;
        }
        double cx = (box.min().getX() + box.max().getX() + 1) / 2.0, cy = box.min().getY() + (box.max().getY() - box.min().getY() + 1) * 0.45;
        double front = box.min().getZ() + 1.6;
        r.then(1, () -> r.look(cx, cy + 0.3 + up, box.min().getZ() - dist, 0F, pitch));
        r.then(60, () -> r.server(p -> {
            ServerLevel level = level(p);
            AABB all = new AABB(box.min().getX(), box.min().getY(), box.min().getZ(), box.max().getX() + 1, box.max().getY() + 1, box.max().getZ() + 1);
            int k = 0;
            boolean found = false;
            for (TankmateEntity m : level.getEntitiesOfClass(TankmateEntity.class, all)) {
                if (!found && m.species() == s) {
                    hold(m, cx, cy, front, yaw);
                    found = true;
                } else {
                    hold(m, box.min().getX() + 2.0 + (k % 6) * (box.max().getX() - box.min().getX() - 3) / 5.0, box.min().getY() + 1.0 + (k / 6) * 1.5,
                            box.max().getZ() - 1.5, 90F);
                    k++;
                }
            }
            if (!found) LOGGER.error("[smoketest] media M58: no {} in {}", s.id(), tank);
        }));
        r.then(1, () -> r.mc.options.fov().set(50));
        r.then(80, () -> r.shot("m58_" + name));
        r.then(1, () -> r.mc.options.fov().set(70));
        r.then(1, () -> r.server(p -> {
            holds.clear();
            AABB all = new AABB(box.min().getX(), box.min().getY(), box.min().getZ(), box.max().getX() + 1, box.max().getY() + 1, box.max().getZ() + 1);
            for (TankmateEntity m : level(p).getEntitiesOfClass(TankmateEntity.class, all)) release(m);
        }));
    }

    private static void pose(Mob m, double x, double y, double z, float yaw) {
        place(m, x, y, z, yaw);
        m.setDeltaMovement(Vec3.ZERO);
        SmokeFreeze.freeze(m, true);
    }

    private static void release(Mob m) {
        m.setNoAi(false);
        m.setNoGravity(false);
        m.removeTag(SmokeFreeze.TAG);
    }

    /** The turtles: the grown one swimming at the front, the hatchling on the beach. */
    private void turtles(ServerLevel level) {
        TankBlockEntity c = TankMultiblock.getController(level, turtle);
        if (c == null) return;
        LivingEntity grown = insert(level, c, EntityType.TURTLE, false), baby = insert(level, c, EntityType.TURTLE, true);
        TankData t = c.getTank();
        TankTerrain ter = new TankTerrain(t, level.getServer(), InteriorRegions.get(level.getServer()).get(t.regionId()), true);
        if (grown instanceof Mob g) hold(g, turtle.getX() + 5.5, t.waterSurfaceY() - 0.45, turtle.getZ() + 1.2, 115F);
        if (baby instanceof Mob h) {
            double x = turtle.getX() + 9.6, z = turtle.getZ() + 1.4;
            hold(h, x, ter.ground(x, z) + 0.01, z, 200F);
        }
        LOGGER.info("[smoketest] media M58 sea turtles: mode {} land {} salt {}", t.terrariumMode(), t.landShare(), t.salinity());
    }

    private static LivingEntity insert(ServerLevel level, TankBlockEntity c, EntityType<?> type, boolean baby) {
        Entity e = create(type, level);
        if (e == null) return null;
        if (baby && e instanceof Mob m) m.setBaby(true);
        place(e, c.getBlockPos().getX(), c.getBlockPos().getY(), c.getBlockPos().getZ(), 0F);
        ItemStack stack = baby ? org.overengineer.bettacraft.entity.betta.FishBagItem.fillWith(e) : org.overengineer.bettacraft.big.TransportTubItem.fillWith(e);
        e.discard();
        List<Entity> before = TankInhabitants.liveInhabitants(level, c.getTank());
        Component err = TankInhabitants.insert(level, c, stack, type, null, TankInhabitants.carriedWater(stack));
        if (err != null) {
            LOGGER.error("[smoketest] media M58 {} refused: {}", BuiltInRegistries.ENTITY_TYPE.getKey(type), err.getString());
            return null;
        }
        for (Entity x : TankInhabitants.liveInhabitants(level, c.getTank())) if (!before.contains(x) && x instanceof LivingEntity le) return le;
        return null;
    }

    /** The dolphins: one at the front glass, the other behind it. */
    private void dolphins(ServerLevel level) {
        TankBlockEntity c = TankMultiblock.getController(level, dolphin);
        if (c == null) return;
        int k = 0;
        for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) {
            if (!(e instanceof Mob m) || e.getType() != EntityType.DOLPHIN) continue;
            if (k == 0) hold(m, dolphin.getX() + 15.5, dolphin.getY() + 3.4, dolphin.getZ() + 2.2, 100F);
            else hold(m, dolphin.getX() + 19.5, dolphin.getY() + 4.4, dolphin.getZ() + 5.0, 75F);
            k++;
        }
        if (k == 0) LOGGER.error("[smoketest] media M58 no dolphin in the tank");
    }

    /** The Minecraft frogs of the swamp on its bank, side by side, facing the glass. */
    private void swampFrogs(ServerLevel level) {
        TankBlockEntity c = TankMultiblock.getController(level, swamp);
        if (c == null) return;
        TankData t = c.getTank();
        TankTerrain ter = new TankTerrain(t, level.getServer(), InteriorRegions.get(level.getServer()).get(t.regionId()), true);
        int k = 0;
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) {
            if (!(e instanceof Mob m) || e.getType() != EntityType.FROG) continue;
            double x = swamp.getX() + 0.9 + k * 1.1, z = swamp.getZ() + 1.75;
            hold(m, x, ter.ground(x, z) + 0.01, z, 160F + k * 20F);
            k++;
        }
    }

    /** The archerfish closest to the mangrove's crown spits at it (the shot of the sequence). */
    private void archerShoots(ServerLevel level) {
        TankBlockEntity c = TankMultiblock.getController(level, mangrove);
        if (c == null || c.getTank().mangroves().isEmpty()) return;
        var t = c.getTank().mangroves().getFirst();
        Vec3 crown = new Vec3(mangrove.getX() + (t.x + 0.5) / 16.0, c.getTank().waterSurfaceY() + 0.3, mangrove.getZ() + (t.z + 0.5) / 16.0);
        TankmateEntity best = null;
        for (TankmateEntity m : TankmateBreeding.tankmates(level, c.getTank())) {
            if (m.species() != TankmateSpecies.ARCHERFISH) continue;
            if (best == null || m.position().distanceToSqr(crown) < best.position().distanceToSqr(crown)) best = m;
        }
        if (best == null) return;
        c.getTank().setFlies(c.getTank().flies() + 1);
        Archerfish.shoot(level, c, best, crown.add(level.getRandom().nextDouble() * 0.3 - 0.15, 0, -0.2));
    }

    // ------------------------------------------------------------------ the scenes (client steps)

    void scenes() {
        if (r.parts.contains("m58")) {
            r.then(1, () -> r.server(p -> {
                level(p).getServer().tickRateManager().setTickRate(20F);
            }));
            tankScene("paludarium", palu, 4, 3, 2, 0F, 10F, 0.66, 90);
            tankScene("mangrove", mangrove, 5, 3, 3, 0F, 10F, 0.7, 100);
            tankScene("desert", desert, 4, 2, 3, 0F, 18F, 0.66, 100);
            tankScene("seahorses", seahorses, 3, 2, 2, 0F, 6F, 0.62, 100);
            tankScene("swamp", swamp, 4, 2, 3, 0F, 14F, 0.68, 0);
            tankScene("terrarium_plants", terra, 4, 3, 2, 0F, 10F, 0.66, 0);
            // inside the terrarium: the plants at their own size
            r.then(1, () -> r.server(p -> {
                TankBlockEntity c = TankMultiblock.getController(level(p), terra);
                if (c == null) return;
                InteriorRegion ir = InteriorManager.ensureRegion(level(p), c);
                ServerLevel in = InteriorManager.interiorLevel(level(p).getServer());
                teleport(p, in, ir.originX() + 30, InteriorRegion.BASE_Y + 20 - p.getEyeHeight(), ir.originZ() - 3, 0F, 22F);
            }));
            r.then(120, () -> r.shot("m58_terrarium_inside"));
            r.then(1, () -> r.look(swamp.getX(), swamp.getY() + 3, swamp.getZ() - 6, 0F, 0F));
            r.then(40, () -> {});
            // the sea turtle, the dolphins
            r.then(1, () -> r.look(turtle.getX() + 6.8, turtle.getY() + 3.0, turtle.getZ() - 1.8, 0F, 36F));
            r.then(80, () -> r.server(p -> turtles(level(p))));
            r.then(100, () -> r.shot("m58_sea_turtle"));
            r.then(1, () -> holds.clear());
            r.then(1, () -> r.look(dolphin.getX() + 17.0, dolphin.getY() + 3.8, dolphin.getZ() - 2.6, 0F, 2F));
            r.then(80, () -> r.server(p -> dolphins(level(p))));
            r.then(1, () -> r.mc.options.fov().set(55));
            r.then(100, () -> r.shot("m58_dolphin"));
            r.then(1, () -> r.mc.options.fov().set(70));
            r.then(1, () -> holds.clear());
            // the giants of the showcase one by one
            giant("Riesen des Meeres", TankmateSpecies.BLACKTIP_REEF_SHARK, "shark", 2.4, 90F, 0, 3F);
            giant("Riesen des Meeres", TankmateSpecies.SPOTTED_EAGLE_RAY, "eagle_ray", 1.6, 90F, 2.2, 38F);
            giant("Süßwasser-Riesen", TankmateSpecies.WELS_CATFISH, "wels", 2.2, 90F, 0, 3F);
            // the aquarist villager, the hired aquarist, the monitors
            r.then(1, () -> r.look(villager.getX() + 4.0, villager.getY() + 1.9, villager.getZ() - 3.4, 0F, 14F));
            r.then(60, () -> r.server(p -> {
                if (trader != null) hold(trader, villager.getX() + 2.5, villager.getY(), villager.getZ() + 2.45, 180F);
            }));
            r.then(60, () -> r.shot("m58_aquarist_villager"));
            r.then(1, () -> holds.clear());
            r.then(1, () -> r.look(hired.getX() + 5.0, hired.getY() + 2.4, hired.getZ() - 5.2, 0F, 16F));
            r.then(120, () -> r.shot("m58_hired_aquarist"));
            r.then(1, () -> r.lookAt(new DevClientSmokeReadme.Box("monitors", monitors.below(), monitors.offset(12, 2, 1)), 0F, 6F, 0.72));
            r.then(80, () -> r.server(p -> dashboard(level(p))));
            r.then(120, () -> r.shot("m58_monitors"));
            // the pond with the wild plants
            r.then(1, () -> r.look(pond.getX() + 1.0, pond.getY() + 2.0, pond.getZ() + 6.5, -90F, 32F));
            r.then(160, () -> r.shot("m58_wild_plants"));
            // the users tab of the tank menu, the animal icons (not with shaders: the GUI is the same)
            if (!r.shaders) {
                r.then(1, () -> hud(false));
                r.then(1, () -> r.look(palu.getX() + 2.0, palu.getY() + 1.5, palu.getZ() - 2.5, 0F, 10F));
                r.then(30, () -> r.openMenu(palu));
                r.then(30, () -> {
                    if (screen() instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) {
                        ts.selectTabForTest(4);
                        ts.selectUserForTest(UUID.nameUUIDFromBytes("media-alice".getBytes()));
                    } else LOGGER.error("[smoketest] media M58 no tank screen for the users tab");
                });
                r.then(20, () -> r.shot("m58_menu_users"));
                r.then(1, () -> r.server(ServerPlayer::closeContainer));
                r.then(5, () -> closeScreen());
                r.then(1, () -> r.server(p -> {
                    icons.clear();
                    icons.addAll(DevClientSmokeM55.iconStacks(level(p)));
                    SimpleContainer box = new SimpleContainer(54);
                    for (int i = 0; i < 54 && i < icons.size(); i++) box.setItem(i, icons.get(i).copy());
                    p.openMenu(new SimpleMenuProvider((id, inv, pl) -> ChestMenu.sixRows(id, inv, box), Component.literal("Bettacraft")));
                }));
                r.then(40, () -> r.shot("m58_icons"));
                r.then(1, () -> r.server(ServerPlayer::closeContainer));
                r.then(5, () -> closeScreen());
                r.then(1, () -> hud(true));
            }
        }
        if (r.parts.contains("create") && railStart != null) {
            r.then(1, () -> r.look(curve.getX() - 4.5, curve.getY() + 3.6, curve.getZ() - 4.6, -32F, 34F));
            r.then(200, () -> r.server(this::startCart));
            r.then(1, this::waitForCurve);
        }
        r.then(1, () -> LOGGER.info("[smoketest] media M58 scenes done"));
        r.then(1, r::languageBack);
    }

    /** A tank scene: the still from the front, then (without shaders) a frame sequence of {@code frames} pictures. */
    private void tankScene(String name, BlockPos min, int sx, int sy, int sz, float yaw, float pitch, double dist, int frames) {
        r.then(1, () -> r.lookAt(new DevClientSmokeReadme.Box(name, min.below(), min.offset(sx - 1, sy - 1, sz - 1)), yaw, pitch, dist));
        r.then(60, () -> {
            if (name.equals("swamp")) r.server(p -> swampFrogs(level(p)));
        });
        r.then(100, () -> r.shot("m58_" + name));
        r.then(1, () -> holds.clear());
        if (frames <= 0) return;
        r.then(1, () -> {
            if (r.shaders) return;
            if (name.equals("mangrove")) r.seqHook = (p, i) -> {
                if (i % 20 == 5) archerShoots(level(p));
            };
            r.sequence("m58_" + name, frames);
        });
        r.then(20, () -> r.seqHook = null);
    }

    private void waitForCurve() {
        int[] n = {0};
        r.steps.addFirst(new DevClientSmokeReadme.Step(1, new Runnable() {
            @Override
            public void run() {
                pushCart();
                if (cartInCurve || ++n[0] > 200) {
                    r.shot("m58_create_cart");
                    r.steps.addFirst(new DevClientSmokeReadme.Step(10, () -> {
                        r.shot("m58_create_cart_b");
                        pushing = false;
                    }));
                    return;
                }
                r.steps.addFirst(new DevClientSmokeReadme.Step(1, this));
            }
        }));
    }

    // ------------------------------------------------------------------ animals

    private static HerpEntity herp(ServerLevel level, TankBlockEntity c, HerpSpecies s, boolean female, int variety) {
        HerpEntity e = create(ModEntities.herp(s), level);
        if (e == null) return null;
        e.setFemale(female);
        e.setVariety(variety);
        ItemStack box = e.getBucketItemStack();
        e.saveToBucketTag(box);
        e.discard();
        Component err = TankInhabitants.insert(level, c, box, ModEntities.herp(s), null, 0L);
        if (err != null) LOGGER.error("[smoketest] media M58 {} not in: {}", s.id(), err.getString());
        return e;
    }

    private static void herpByBox(ServerLevel level, TankBlockEntity c, HerpSpecies s) {
        herp(level, c, s, level.getRandom().nextBoolean(), 0);
    }

    private static void vanillaFrog(ServerLevel level, TankBlockEntity c, int variant) {
        net.minecraft.world.entity.animal.frog.Frog f = create(EntityType.FROG, level);
        if (f == null) return;
        frogVariant(f, variant);
        ItemStack box = TerrariumBoxItem.fillWith(f);
        f.discard();
        Component err = TankInhabitants.insert(level, c, box, EntityType.FROG, null, 0L);
        if (err != null) LOGGER.error("[smoketest] media M58 frog not in: {}", err.getString());
    }

    private static void dwarfFrog(ServerLevel level, TankBlockEntity c, boolean female, Vec3 at) {
        DwarfFrogEntity f = create(ModEntities.DWARF_FROG.get(), level);
        if (f == null) return;
        f.setFemale(female);
        place(f, at.x, at.y, at.z, 0F);
        TankInhabitants.adopt(f, c);
        level.addFreshEntity(f);
        c.getTank().inhabitants().add(f.getUUID());
    }

    // ------------------------------------------------------------------ version-dependent calls (ported by hand per branch)

    private static ServerLevel level(ServerPlayer p) {
        return p.level();
    }

    private static <T extends Entity> T create(EntityType<T> type, ServerLevel level) {
        return type.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
    }

    private static void place(Entity e, double x, double y, double z, float yaw) {
        e.snapTo(x, y, z, yaw, 0F);
        e.setYRot(yaw);
        e.yRotO = yaw;
        e.setYHeadRot(yaw);
        if (e instanceof LivingEntity le) {
            le.yBodyRot = yaw;
            le.yBodyRotO = yaw;
            le.yHeadRotO = yaw;
        }
    }

    private static void teleport(ServerPlayer p, ServerLevel to, double x, double y, double z, float yaw, float pitch) {
        p.teleportTo(to, x, y, z, java.util.Set.of(), yaw, pitch, true);
    }

    private static void aquarist(Villager v) {
        v.setVillagerData(v.getVillagerData().withProfession(BuiltInRegistries.VILLAGER_PROFESSION.wrapAsHolder(ModVillagers.AQUARIST.get())).withLevel(5));
    }

    private static void frogVariant(net.minecraft.world.entity.animal.frog.Frog f, int variant) {
        // the frog variants are a data registry since 1.21.5: the frogs keep the variant they were made with
    }

    private static Entity minecart(ServerLevel level, double x, double y, double z) {
        Entity m = create(EntityType.MINECART, level);
        if (m != null) m.setPos(x, y, z);
        return m;
    }

    private void hud(boolean hidden) {
        r.mc.options.hideGui = hidden;
    }

    private net.minecraft.client.gui.screens.Screen screen() {
        return r.mc.screen;
    }

    private void closeScreen() {
        r.mc.setScreen(null);
    }
}
