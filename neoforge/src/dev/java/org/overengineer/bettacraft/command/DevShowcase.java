package org.overengineer.bettacraft.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.decor.DecorItem;
import org.overengineer.bettacraft.decor.DecorType;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.frog.DwarfFrogEntity;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.EquipmentPlacement;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity;
import org.overengineer.bettacraft.equipment.FilterHeaters;
import org.overengineer.bettacraft.reef.ProteinSkimmerBlock;
import org.overengineer.bettacraft.equipment.SegmentBlock;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.genetics.BettaVarietyTable;
import org.overengineer.bettacraft.plants.InteriorPlant;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.SignPosition;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
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
 * M21: builds the showcase world "Bettacraft Schaukasten" ({@code gradlew buildShowcase}: a dedicated server without a
 * window on a fresh flat world in {@code run/saves}, {@code -Dbettacraft.showcase=true}). Rows of tanks grouped by theme,
 * every tank named on its glass (German names, no vanilla signs) and a small named dry tank at the start of every row: every betta variety, every
 * tankmate, every small animal and the frog, every aquarium plant (grown, under strong light), every floating plant,
 * every decoration, the equipment (filters, heaters, coolers, lid and feeder, skimmer, pump), the 16 light colours, the
 * glass types, styles and free shapes, every coral and reef animal in its own salt water tank, a reef tank and a
 * brackish tank. The animals live normally (not frozen). Then the world is saved and the server stops.
 * <p>
 * M21.5: every tank stands on cabinets with an external filter in the front one (the doors face the path, the wood
 * changes from row to row), so the tanks are one block higher, at eye level; power is off in the world's server config;
 * every tank has the break protection on; a row per colour of the small animals with colour genetics (ramshorn,
 * apple snail, shrimp, crayfish) and the racer nerites with their individual shades, and the colour forms of the
 * tankmates (green and blue neon, guppies, endlers, platies, ...).
 */
public final class DevShowcase {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/showcase");
    private static final boolean ENABLED = Boolean.getBoolean("bettacraft.showcase");
    private static final int GAP = 4, ROW = 9;
    private static int ticks;
    private static int haltAt = -1;
    private static int builtAt = -1;
    private static int tanks, animals, plants, named;
    private static BlockPos origin;
    private static int row;
    /** M21.5: the wood of the cabinets in the current row. */
    private static Identifier rowWood = Identifier.withDefaultNamespace("oak_planks");
    private static final String[] WOODS = {"oak_planks", "spruce_planks", "birch_planks", "jungle_planks", "acacia_planks", "dark_oak_planks",
            "mangrove_planks", "cherry_planks", "bamboo_planks", "crimson_planks", "warped_planks", "pale_oak_planks"};
    private static final List<ChunkPos> forced = new ArrayList<>();

    /** M52: extra rows after the showcase (the Fabric save/load test) and a step after saving (its manifest). */
    public static java.util.function.BiConsumer<ServerLevel, BlockPos> extraRows;
    public static java.util.function.BiConsumer<MinecraftServer, ServerLevel> afterSave;

    private DevShowcase() {}

    /** M52: every server tick (NeoForge: {@link DevShowcaseNeoForge}; Fabric: the Fabric dev smoke). */
    public static void onTick(MinecraftServer tickedServer) {
        if (!ENABLED) return;
        MinecraftServer server = tickedServer;
        ServerLevel level = server.overworld();
        ticks++;
        try {
            if (ticks == 5) {
                prepare(server, level);
            } else if (builtAt < 0 && ticks > 5) {
                // the animals are entities: build only once the entity sections of every chunk are loaded (an entity
                // added to a chunk whose entities are still loading is lost)
                boolean ready = true;
                for (ChunkPos cp : forced) {
                    level.getChunk(cp.x(), cp.z());
                    ready &= level.areEntitiesLoaded(cp.pack());
                }
                if (!ready && ticks < 1200) return;
                builtAt = ticks;
                build(level);
                if (extraRows != null) extraRows.accept(level, origin.offset(0, 0, row * ROW));
                LOGGER.info("[showcase] built {} rows, {} tanks, {} animals, {} plants at {}", row, tanks, animals, plants, origin);
                logChunks(server, "after the build");
            } else if (builtAt > 0 && ticks == builtAt + 40) {
                int live = 0, any = 0;
                for (var e : level.getAllEntities()) {
                    any++;
                    if (e instanceof org.overengineer.bettacraft.entity.betta.BettaEntity || e instanceof org.overengineer.bettacraft.entity.tankmate.TankmateEntity) live++;
                }
                LOGGER.info("[showcase] tick {}: {} fish of {} entities", ticks, live, any);
                logChunks(server, "40 ticks later");
            } else if (builtAt > 0 && ticks == builtAt + 400) {
                // the care loop ran for a while: settle the plant colours under their lights, then save
                int live = 0;
                for (var e : level.getAllEntities()) {
                    if (e instanceof org.overengineer.bettacraft.entity.betta.BettaEntity || e instanceof org.overengineer.bettacraft.entity.tankmate.TankmateEntity
                            || e instanceof DwarfFrogEntity) live++;
                }
                LOGGER.info("[showcase] {} fish and frogs alive in the world", live);
                logChunks(server, "before saving");
                LOGGER.info("[showcase] saving");
                for (ChunkPos cp : forced) level.setChunkForced(cp.x(), cp.z(), false);
                server.saveEverything(false, true, true);
                if (afterSave != null) afterSave.accept(server, level);
                LOGGER.info("[showcase] SHOWCASE BUILT: {} tanks ({} named), {} animals, {} plants", tanks, named, animals, plants);
                haltAt = ticks + 20;
            } else if (ticks == haltAt) {
                server.halt(false);
            }
        } catch (RuntimeException e) {
            LOGGER.error("[showcase] FAILED", e);
            server.halt(false);
        }
    }

    /** M31.1: interior chunks held and the heap in use (the build once ran out of memory with ~750 chunks per tank). */
    private static void logChunks(MinecraftServer server, String when) {
        ServerLevel in = org.overengineer.bettacraft.tank.interior.InteriorManager.interiorLevel(server);
        if (in != null) LOGGER.info("[showcase] interior chunks {}: {}", when, org.overengineer.bettacraft.gametest.InteriorChunkGameTests.stats(in));
    }

    private static void prepare(MinecraftServer server, ServerLevel level) {
        GameRules rules = level.getGameRules();
        rules.set(GameRules.ADVANCE_TIME, false, server);
        rules.set(GameRules.SPAWN_MOBS, false, server);
        rules.set(GameRules.ADVANCE_WEATHER, false, server);
        rules.set(GameRules.SPAWN_PATROLS, false, server);
        rules.set(GameRules.SPAWN_WANDERING_TRADERS, false, server);
        org.overengineer.bettacraft.gametest.framework.DevTime.set(level, 6000);
        level.getServer().setWeatherParameters(120000, 0, false, false);
        // M21.5: power off in this world (filters, heaters and pumps run without FE); M67: power.costs OFF
        org.overengineer.bettacraft.config.ServerConfig.POWER_COSTS.set(org.overengineer.bettacraft.config.ServerConfig.PowerCosts.OFF);
        org.overengineer.bettacraft.config.ServerConfig.POWER_COSTS.save();
        BlockPos spawn = level.getRespawnData().pos();
        // M52c (26.1): the spawn chunks are no longer kept loaded; the heightmap of a chunk that is not there is the world's
        // bottom (the showcase was built 4 blocks under the grass): load the chunk first
        level.getChunk(spawn.getX() >> 4, spawn.getZ() >> 4);
        int ground = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, spawn.getX(), spawn.getZ());
        origin = new BlockPos(spawn.getX() + 3, ground, spawn.getZ() + 6);
        level.setRespawnData(net.minecraft.world.level.storage.LevelData.RespawnData.of(level.dimension(), new BlockPos(spawn.getX(), ground, spawn.getZ()), 180F, 0F));
        // keep the whole showcase loaded while it is built (the animals are entities)
        for (int cx = (origin.getX() >> 4) - 1; cx <= (origin.getX() + 150 >> 4) + 1; cx++)
            for (int cz = (origin.getZ() >> 4) - 1; cz <= (origin.getZ() + 20 * ROW + 16 >> 4) + 1; cz++) { // M30: the big tanks of the last row
                level.setChunkForced(cx, cz, true);
                forced.add(new ChunkPos(cx, cz));
            }
        // the cheats of a singleplayer world (the world was made by a dedicated server)
        try {
            var data = server.getWorldData();
            if (data instanceof net.minecraft.world.level.storage.PrimaryLevelData pd) {
                java.lang.reflect.Field f = net.minecraft.world.level.storage.PrimaryLevelData.class.getDeclaredField("settings");
                f.setAccessible(true);
                var s = (net.minecraft.world.level.LevelSettings) f.get(pd);
                f.set(pd, new net.minecraft.world.level.LevelSettings("Bettacraft Schaukasten", net.minecraft.world.level.GameType.CREATIVE,
                        new net.minecraft.world.level.LevelSettings.DifficultySettings(net.minecraft.world.Difficulty.PEACEFUL, false, false), true,
                        s.dataConfiguration()));
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("[showcase] could not switch on cheats: {}", e.toString());
        }
    }

    // ------------------------------------------------------------------ rows

    private static void build(ServerLevel level) {
        RandomSource r = RandomSource.create(21);
        // 1: bettas, one tank per named variety
        BlockPos at = startRow(level, "Bettas (Zuchtformen)");
        for (var v : BettaVarietyTable.VARIETIES) {
            TankBlockEntity c = tank(level, at, 2, 2, 2, 0F, true);
            betta(level, c, v.example(), false);
            betta(level, c, v.example(), true);
            name(level, at, de("variety.bettacraft." + v.id()));
            at = at.east(2 + GAP);
        }
        // 2: tankmates (fresh water), a small group each
        at = startRow(level, "Beifische (Süßwasser)");
        for (TankmateSpecies s : TankmateSpecies.values()) {
            if (s.marine() || s.isLarge()) continue; // M30: the big animals need big tanks
            TankBlockEntity c = tank(level, at, 2, 2, 2, 0F, true);
            fish(level, c, s, Math.min(4, Math.max(2, s.shoal())), r);
            name(level, at, de(s.translationKey()));
            at = at.east(2 + GAP);
        }
        // 3: small animals (fresh water) and the frog
        at = startRow(level, "Kleintiere und Frosch");
        for (CritterSpecies s : CritterSpecies.values()) {
            if (s.marine()) continue;
            TankBlockEntity c = tank(level, at, 2, 1, 2, 0F, true);
            critters(level, c, s, 6);
            name(level, at, de(s.translationKey()));
            at = at.east(2 + GAP);
        }
        TankBlockEntity frogs = tank(level, at, 2, 2, 2, 0F, true);
        for (int i = 0; i < 2; i++) frog(level, frogs);
        name(level, at, de("entity.bettacraft.dwarf_frog"));
        // M21.5: a tank per colour of every small animal with colour genetics, the racer nerites with their shades
        at = startRow(level, "Kleintiere: Farben");
        for (CritterSpecies s : CritterSpecies.values()) {
            if (s.loci().length == 0 && !org.overengineer.bettacraft.critters.CritterLooks.has(s)) continue;
            for (int l = -1; l < s.loci().length; l++) {
                TankBlockEntity c = tank(level, at, 2, 1, 2, 0F, true);
                region(level, c, Blocks.SAND.defaultBlockState());
                CritterGenes g = l < 0 ? CritterGenes.WILD : CritterGenes.pure(l, s == CritterSpecies.NEOCARIDINA ? 0.5F : 0.6F);
                critters(level, c, s, s.nerite() ? 8 : 5, g);
                String v = de("variety.bettacraft." + s.id() + "." + (l < 0 ? "wild" : g.variety(s)));
                name(level, at, s.nerite() ? de(s.translationKey()) : de(s.translationKey()) + ": " + v);
                at = at.east(2 + GAP);
            }
        }
        // M21.5: the colour forms of the tankmates (pure lines, a few of each)
        at = startRow(level, "Beifische: Farbformen");
        for (TankmateSpecies s : TankmateSpecies.values()) {
            if (s.varieties().size() <= 1) continue;
            for (String v : s.varieties()) {
                TankBlockEntity c = tank(level, at, 2, 2, 2, s.marine() ? 35F : 0F, true);
                InteriorRegion vr = region(level, c, (s.marine() ? Blocks.SAND : Blocks.GRAVEL).defaultBlockState());
                if (s.marine()) reefLight(level, at, 2);
                if (s.seahorse()) seagrass(InteriorManager.interiorLevel(level.getServer()), vr); // M21.8: something to hold on to
                for (int i = 0; i < Math.min(4, Math.max(2, s.shoal())); i++) fish(level, c, s, TankmateGenes.pure(s, v), i % 2 == 0, r);
                name(level, at, de("variety.bettacraft." + s.id() + "." + v));
                at = at.east(2 + GAP);
            }
        }
        // M21.6: the named guppy and endler strains (two males and a female each, pure-bred) and a hybrid tank
        at = startRow(level, "Guppys und Endler: Zuchtformen");
        for (var st : org.overengineer.bettacraft.genetics.livebearer.LivebearerStrain.values()) {
            TankmateSpecies s = st.endler ? TankmateSpecies.ENDLER_GUPPY : TankmateSpecies.GUPPY;
            TankBlockEntity c = tank(level, at, 2, 1, 2, 0F, true);
            region(level, c, Blocks.GRAVEL.defaultBlockState());
            for (int i = 0; i < 3; i++) fish(level, c, s, TankmateGenes.strain(s, st), i == 2, r);
            name(level, at, de(st.translationKey()));
            at = at.east(2 + GAP);
        }
        {
            TankBlockEntity c = tank(level, at, 2, 1, 2, 0F, true);
            region(level, c, Blocks.GRAVEL.defaultBlockState());
            java.util.Random hr = new java.util.Random(216);
            var cobra = org.overengineer.bettacraft.genetics.livebearer.LivebearerStrain.RED_COBRA.genome();
            var tiger = org.overengineer.bettacraft.genetics.livebearer.LivebearerStrain.ENDLER_TIGER.genome();
            for (int i = 0; i < 4; i++) {
                boolean female = i == 3;
                fish(level, c, TankmateSpecies.GUPPY, new TankmateGenes("wild", "wild",
                        org.overengineer.bettacraft.genetics.livebearer.LivebearerGenome.cross(cobra, tiger, female, hr, 0)), female, r);
            }
            name(level, at, "Hybride Red Cobra x Endler Tiger");
            at = at.east(2 + GAP);
        }
        // 4: aquarium plants, grown, strong light
        at = startRow(level, "Pflanzen (starkes Licht)");
        for (PlantSpecies s : PlantSpecies.values()) {
            if (s.marine()) continue;
            TankBlockEntity c = tank(level, at, 2, 2, 2, 0F, true);
            light(level, at, 2, 2, 0, DyeColor.WHITE);
            InteriorRegion ir = region(level, c, Blocks.GRAVEL.defaultBlockState());
            ServerLevel in = InteriorManager.interiorLevel(level.getServer());
            int y = 0;
            if (!s.form().rooted()) {
                rock(in, ir, 16, 16, 6, 5, Blocks.STONE.defaultBlockState());
                y = topAt(in, ir, 16, 16);
            }
            if (InteriorPlants.plantAt(in, ir, s, 16, y, 16, s.stages(), 3000L + s.ordinal()) != null) plants++;
            InteriorPlants.settleTint(level, c);
            name(level, at, de("item.bettacraft." + s.id()));
            at = at.east(2 + GAP);
        }
        // 5: floating plants
        at = startRow(level, "Schwimmpflanzen");
        for (FloatingPlantType t : FloatingPlantType.values()) {
            TankBlockEntity c = tank(level, at, 2, 1, 2, 0F, true);
            light(level, at, 2, 1, 1, DyeColor.WHITE);
            for (int i = 0; i < 14; i++) {
                TankPlants.add(c, t, at.getX() + 0.25 + r.nextDouble() * 1.5, at.getZ() + 0.25 + r.nextDouble() * 1.5, 0.6F + r.nextFloat() * 0.4F,
                        r.nextFloat() * 6.28F);
            }
            c.markTankChanged();
            name(level, at, de(TankPlants.item(t).getItem().getDescriptionId()));
            at = at.east(2 + GAP);
        }
        // 6: decoration
        at = startRow(level, "Deko");
        for (DecorType d : DecorType.values()) {
            TankBlockEntity c = tank(level, at, 2, 2, 2, 0F, true);
            InteriorRegion ir = region(level, c, Blocks.SAND.defaultBlockState());
            ServerLevel in = InteriorManager.interiorLevel(level.getServer());
            Component err = DecorItem.place(in, d, new BlockPos(ir.originX() + 16 - d.sizeX / 2, InteriorRegion.BASE_Y, ir.originZ() + 16 - d.sizeZ / 2),
                    Rotation.NONE, null);
            if (err != null) LOGGER.warn("[showcase] decor {}: {}", d, err.getString());
            name(level, at, de("item.bettacraft.decor_" + d.id()));
            at = at.east(2 + GAP);
        }
        // 7: equipment
        at = startRow(level, "Technik");
        equipment(level, at, r);
        // 8: the 16 light colours (at night they light the water)
        at = startRow(level, "Lichtfarben");
        for (DyeColor color : DyeColor.values()) {
            tank(level, at, 2, 1, 1, 0F, true);
            light(level, at, 2, 1, 0, color);
            name(level, at, "Licht " + COLORS_DE[color.ordinal()]);
            at = at.east(2 + GAP);
        }
        // 9: glass types, styles and free shapes
        at = startRow(level, "Glas und Formen");
        glassAndShapes(level, at);
        // 10: salt water: a reef tank, a brackish tank, every coral and reef animal on its own
        at = startRow(level, "Meer- und Brackwasser");
        reef(level, at, r);
        at = at.east(5 + GAP);
        TankBlockEntity brack = tank(level, at, 3, 2, 2, 12F, true);
        fish(level, brack, TankmateSpecies.GUPPY, 4, r);
        fish(level, brack, TankmateSpecies.PLATY, 3, r);
        critters(level, brack, CritterSpecies.ZEBRA_NERITE, 3);
        name(level, at, "Brackwasser (12 ‰)");
        at = at.east(3 + GAP);
        for (PlantSpecies s : PlantSpecies.values()) {
            if (!s.marine()) continue;
            TankBlockEntity c = tank(level, at, 2, 2, 2, 35F, true);
            reefLight(level, at, 2);
            InteriorRegion ir = region(level, c, Blocks.SAND.defaultBlockState());
            ServerLevel in = InteriorManager.interiorLevel(level.getServer());
            rock(in, ir, 16, 16, 7, 6, ModBlocks.REEF_ROCK.get().defaultBlockState());
            coral(in, ir, s, 16, 16, 4000L + s.ordinal());
            if (s == PlantSpecies.BUBBLE_TIP_ANEMONE) fish(level, c, TankmateSpecies.OCELLARIS_CLOWNFISH, 2, r);
            name(level, at, de("item.bettacraft." + s.id()));
            at = at.east(2 + GAP);
        }
        for (TankmateSpecies s : TankmateSpecies.values()) {
            if (!s.marine() || s.isLarge()) continue;
            TankBlockEntity c = tank(level, at, 2, 2, 2, 35F, true);
            reefLight(level, at, 2);
            InteriorRegion ir = region(level, c, Blocks.SAND.defaultBlockState());
            rock(InteriorManager.interiorLevel(level.getServer()), ir, 16, 20, 6, 5, ModBlocks.LIVE_ROCK.get().defaultBlockState());
            fish(level, c, s, Math.min(4, Math.max(1, s.shoal())), r);
            name(level, at, de(s.translationKey()));
            at = at.east(2 + GAP);
        }
        for (CritterSpecies s : CritterSpecies.values()) {
            if (!s.marine()) continue;
            TankBlockEntity c = tank(level, at, 2, 1, 2, 35F, true);
            reefLight(level, at, 2);
            InteriorRegion ir = region(level, c, Blocks.SAND.defaultBlockState());
            rock(InteriorManager.interiorLevel(level.getServer()), ir, 16, 20, 5, 4, ModBlocks.LIVE_ROCK.get().defaultBlockState());
            critters(level, c, s, 4);
            name(level, at, de(s.translationKey()));
            at = at.east(2 + GAP);
        }
        // M21.8: seahorses: a seahorse tank (seagrass, kelp, corals, calm water, no pump) with lined seahorses of every colour,
        // a small dwarf seahorse tank, a pair with a full brood pouch and fry
        at = startRow(level, "Seepferdchen");
        {
            ServerLevel in = InteriorManager.interiorLevel(level.getServer());
            TankBlockEntity c = tank(level, at, 4, 2, 3, 33F, true);
            InteriorRegion ir = region(level, c, Blocks.SAND.defaultBlockState());
            seagrass(in, ir);
            rock(in, ir, 18, 38, 7, 5, ModBlocks.REEF_ROCK.get().defaultBlockState());
            rock(in, ir, 48, 38, 7, 6, ModBlocks.REEF_ROCK.get().defaultBlockState());
            coral(in, ir, PlantSpecies.LEATHER_CORAL, 18, 38, 2180L);
            coral(in, ir, PlantSpecies.HAMMER_CORAL, 48, 38, 2181L);
            reefLight(level, at, 4);
            c.getTank().setWaterTemp(24F);
            c.getTank().setTargetTemp(24F);
            for (String v : TankmateSpecies.LINED_SEAHORSE.varieties()) {
                fish(level, c, TankmateSpecies.LINED_SEAHORSE, TankmateGenes.pure(TankmateSpecies.LINED_SEAHORSE, v), true, r);
                fish(level, c, TankmateSpecies.LINED_SEAHORSE, TankmateGenes.pure(TankmateSpecies.LINED_SEAHORSE, v), false, r);
            }
            critters(level, c, CritterSpecies.TURBO_SNAIL, 2);
            name(level, at, "Seepferdchenbecken (33 ‰)");
            at = at.east(4 + GAP);
            TankBlockEntity d = tank(level, at, 2, 2, 2, 33F, true);
            seagrass(in, region(level, d, Blocks.SAND.defaultBlockState()));
            reefLight(level, at, 2);
            for (int i = 0; i < 4; i++) fish(level, d, TankmateSpecies.DWARF_SEAHORSE, TankmateGenes.wild(TankmateSpecies.DWARF_SEAHORSE), i % 2 == 0, r);
            name(level, at, "Zwergseepferdchen");
            at = at.east(2 + GAP);
            TankBlockEntity b = tank(level, at, 2, 2, 2, 33F, true);
            seagrass(in, region(level, b, Blocks.SAND.defaultBlockState()));
            reefLight(level, at, 2);
            b.getTank().setWaterTemp(23F);
            b.getTank().setTargetTemp(23F);
            TankmateGenes yellow = TankmateGenes.pure(TankmateSpecies.LINED_SEAHORSE, "yellow");
            fish(level, b, TankmateSpecies.LINED_SEAHORSE, yellow, true, r);
            fish(level, b, TankmateSpecies.LINED_SEAHORSE, yellow, false, r);
            for (var e : TankmateBreeding.tankmates(level, b.getTank())) if (!e.isFemale()) e.setGravid(1.0, yellow);
            name(level, at, "Seepferdchen: Bruttasche");
        }
        // M21.7: cabinets of stone: a small tank on a cabinet of every vanilla stone with slabs, then the first stones of
        // other mods that the recipes know a slab for
        at = startRow(level, "Steinschränke");
        List<Identifier> stones = new ArrayList<>();
        for (var p : org.overengineer.bettacraft.equipment.CabinetStones.VANILLA) stones.add(blockOf(p.stone()));
        int modded = 0;
        for (var p : org.overengineer.bettacraft.equipment.CabinetStones.all(level.recipeAccess(), level.registryAccess())) {
            if (!p.stoneId().getNamespace().equals("minecraft") && modded++ < 6) stones.add(blockOf(p.stone()));
        }
        for (Identifier id : stones) {
            rowWood = id;
            tank(level, at, 1, 1, 1, 0F, true);
            String de = STONES_DE.get(id.getPath());
            name(level, at, de != null && id.getNamespace().equals("minecraft") ? de
                    : net.minecraft.locale.Language.getInstance().getOrDefault(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(id).getDescriptionId(), id.getPath()));
            at = at.east(3);
        }
        LOGGER.info("[showcase] stone cabinets: {} stones ({} of other mods)", stones.size(), Math.min(modded, 6));
        // M22.4: the outlet side of the cabinet filter: back, left, right, front; floating plants drift away from the jet
        at = startRow(level, "Filterauslass am Schrank");
        String[] sidesDe = {"Auslass hinten", "Auslass links", "Auslass rechts", "Auslass vorne"};
        for (var side : org.overengineer.bettacraft.equipment.OutletSide.values()) {
            TankBlockEntity c = tank(level, at, 3, 2, 2, 0F, true);
            TankInteractions.changeWater(level, c, -3000, 0F); // three quarters full: the jet falls into the water
            for (int x = 0; x < 3; x++) {
                if (level.getBlockEntity(at.offset(x, -1, 0)) instanceof CabinetBlockEntity cab && cab.hasFilter()) cab.setOutletSide(side);
            }
            for (int k = 0; k < 5; k++) {
                var t = k % 2 == 0 ? org.overengineer.bettacraft.tank.plants.FloatingPlantType.SALVINIA : org.overengineer.bettacraft.tank.plants.FloatingPlantType.FROGBIT;
                org.overengineer.bettacraft.tank.plants.TankPlants.add(c, t, at.getX() + 0.7 + (k % 3) * 0.8, at.getZ() + 0.6 + (k / 3) * 0.8, 0.8F, k * 50F);
                plants++;
            }
            org.overengineer.bettacraft.tank.TankEquipment.invalidate(c);
            name(level, at, sidesDe[side.ordinal()]);
            at = at.east(3 + GAP);
        }
        // M23: the three light tiers over red and strong-light plants; air pumps with air stones (bubble columns), a sponge filter
        at = startRow(level, "Belüftung und LED-Stufen");
        String[] tiersDe = {"Lichtleiste", "LED-Panel", "Hochleistungs-LED"};
        for (int tier = 0; tier < 3; tier++) {
            TankBlockEntity c = tank(level, at, 4, 2, 2, 0F, true);
            for (int x = 0; x < 4; x++)
                for (int z = 0; z < 2; z++) {
                    BlockPos lp = at.offset(x, 2, z);
                    var st = switch (tier) {
                        case 0 -> z == 1 ? null : ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                                .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 3);
                        case 1 -> ModBlocks.LED_PANEL.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                                .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 3)
                                .setValue(org.overengineer.bettacraft.equipment.LedPanelBlock.PARTNER_POS, z == 0);
                        default -> ModBlocks.HIGH_POWER_LED.get().defaultBlockState().setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 3)
                                .setValue(org.overengineer.bettacraft.equipment.HighPowerLedBlock.NORTH, z > 0)
                                .setValue(org.overengineer.bettacraft.equipment.HighPowerLedBlock.SOUTH, z < 1);
                    };
                    if (st != null) level.setBlockAndUpdate(lp, st.setValue(TankLightBlock.LIT, true));
                }
            InteriorRegion lr = region(level, c, Blocks.GRAVEL.defaultBlockState());
            ServerLevel lin = org.overengineer.bettacraft.tank.interior.InteriorManager.interiorLevel(level.getServer());
            org.overengineer.bettacraft.plants.InteriorPlants.plantAt(lin, lr, org.overengineer.bettacraft.plants.PlantSpecies.ALTERNANTHERA, 44, 0, 20, 6, 7L + tier);
            org.overengineer.bettacraft.plants.InteriorPlants.plantAt(lin, lr, org.overengineer.bettacraft.plants.PlantSpecies.LUDWIGIA, 52, 0, 18, 6, 9L + tier);
            for (int k = 0; k < 3; k++) org.overengineer.bettacraft.plants.InteriorPlants.plantAt(lin, lr, org.overengineer.bettacraft.plants.PlantSpecies.MONTE_CARLO, 6 + k * 10, 0, 8, 5, 21L + k);
            org.overengineer.bettacraft.plants.InteriorPlants.settleTint(level, c);
            plants += 5;
            // an air pump at the west wall with two to four air stones
            level.setBlockAndUpdate(at.offset(-1, 0, 0), ModBlocks.AIR_PUMP.get().defaultBlockState()
                    .setValue(org.overengineer.bettacraft.equipment.AirPumpBlock.FACING, Direction.EAST));
            for (int k = 0; k < 2 + tier; k++) lin.setBlock(lr.toWorld(10 + k * 10, 0, 20 - k * 3), ModBlocks.AIR_STONE.get().defaultBlockState(), 3);
            org.overengineer.bettacraft.tank.TankEquipment.invalidate(c);
            org.overengineer.bettacraft.tank.TankAeration.step(level, c, 0.01);
            name(level, at, tiersDe[tier]);
            at = at.east(4 + GAP);
        }
        // M24: a multiblock filter at a tank, the top-off and the water change controller, the crucible
        at = startRow(level, "Rohre, Filter und Wasserwechsel");
        TankBlockEntity ft = tank(level, at, 3, 2, 2, 0F, true);
        net.minecraft.world.item.Item[] media = {ModItems.FILTER_FOAM.get(), ModItems.CERAMIC_RINGS.get()};
        for (int y = 0; y < 2; y++) {
            level.setBlockAndUpdate(at.offset(4, y, 0), ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
            if (level.getBlockEntity(at.offset(4, y, 0)) instanceof org.overengineer.bettacraft.filter.FilterChamberBlockEntity ch) ch.setMedium(new ItemStack(media[y]));
        }
        level.setBlockAndUpdate(at.offset(4, 2, 0), ModBlocks.FILTER_PUMP_HEAD.get().defaultBlockState());
        level.setBlockAndUpdate(at.offset(3, 0, 0), ModBlocks.FILTER_INLET.get().defaultBlockState().setValue(org.overengineer.bettacraft.filter.FilterPartBlock.FACING, Direction.WEST));
        level.setBlockAndUpdate(at.offset(3, 1, 0), ModBlocks.FILTER_OUTLET.get().defaultBlockState().setValue(org.overengineer.bettacraft.filter.FilterPartBlock.FACING, Direction.WEST));
        org.overengineer.bettacraft.gametest.FilterLinesGameTests.guardAll(level, at.offset(4, 2, 0)); // M40: the guards in the tank
        name(level, at, "Multiblock-Filter");
        at = at.east(5 + GAP);
        TankBlockEntity wt = tank(level, at, 3, 2, 2, 0F, true);
        wt.getTank().setNitrate(45F);
        level.setBlockAndUpdate(at.offset(-1, 0, 0), ModBlocks.LEVEL_SENSOR.get().defaultBlockState());
        level.setBlockAndUpdate(at.offset(-2, 0, 0), ModBlocks.RESERVOIR.get().defaultBlockState());
        if (level.getBlockEntity(at.offset(-2, 0, 0)) instanceof org.overengineer.bettacraft.plumbing.ReservoirBlockEntity res24)
            res24.water().fill(new org.overengineer.bettacraft.platform.fluid.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 16000),
                    org.overengineer.bettacraft.platform.fluid.IFluidHandler.FluidAction.EXECUTE);
        level.setBlockAndUpdate(at.offset(3, 0, 1), ModBlocks.WATER_CHANGE_CONTROLLER.get().defaultBlockState());
        name(level, at, "Nachfüllen + Wasserwechsel");
        at = at.east(5 + GAP);
        level.setBlockAndUpdate(at.offset(1, -1, 0), Blocks.MAGMA_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(at.offset(1, 0, 0), ModBlocks.CRUCIBLE.get().defaultBlockState());
        if (level.getBlockEntity(at.offset(1, 0, 0)) instanceof org.overengineer.bettacraft.crucible.CrucibleBlockEntity cr) {
            cr.items().setStackInSlot(0, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 64));
            cr.items().setStackInSlot(1, new ItemStack(net.minecraft.world.item.Items.COAL, 64));
            cr.items().setStackInSlot(2, new ItemStack(net.minecraft.world.item.Items.QUARTZ, 64));
            cr.items().setStackInSlot(3, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 64));
        }
        for (int x = 2; x < 6; x++) level.setBlockAndUpdate(at.offset(x, 0, 0), (x % 2 == 0 ? ModBlocks.GLASS_PIPE : ModBlocks.STAINLESS_STEEL_PIPE).get().defaultBlockState());
        LOGGER.info("[showcase] M24 row: filter {}, top-off {}", ft.getBlockPos(), wt.getBlockPos());
        filterLines(level, at.east(6 + GAP)); // M40
        terraria(level); // M31
        powerStore(level); // M67
        // M30: big tanks (the last row: they are deeper than a row): the freshwater giants and the sea giants
        at = startRow(level, "Großbecken");
        bigTank(level, at, 24, 6, 12, 0F, new TankmateSpecies[]{TankmateSpecies.PANGASIUS, TankmateSpecies.REDTAIL_CATFISH, TankmateSpecies.WELS_CATFISH,
                TankmateSpecies.ARAPAIMA, TankmateSpecies.MOTORO_STINGRAY, TankmateSpecies.MOTORO_STINGRAY}, "Süßwasser-Riesen");
        at = at.east(24 + GAP);
        bigTank(level, at, 28, 6, 14, 35F, new TankmateSpecies[]{TankmateSpecies.BAMBOO_SHARK, TankmateSpecies.BAMBOO_SHARK, TankmateSpecies.BLACKTIP_REEF_SHARK,
                TankmateSpecies.SPOTTED_EAGLE_RAY, TankmateSpecies.COWNOSE_RAY, TankmateSpecies.COWNOSE_RAY, TankmateSpecies.COWNOSE_RAY,
                TankmateSpecies.HONEYCOMB_MORAY}, "Riesen des Meeres");
    }

    /**
     * M31: a row of terraria: a paludarium with sliders and a musk turtle (land on the right, a basking lamp), a dry terrarium
     * with leopard geckos and a corn snake, one with crested geckos and dart frogs on moss, and the mobs in glass: drowned in an
     * aquarium, a spider, a skeleton and a zombie in a terrarium with a lid.
     */
    private static void terraria(ServerLevel level) {
        BlockPos at = startRow(level, "Terrarien");
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        // a paludarium: water on the left, land rising to the right
        TankBlockEntity pc = tank(level, at, 4, 3, 3, 0F, false);
        InteriorRegion pr = region(level, pc, Blocks.SAND.defaultBlockState());
        for (int x = 30; x < pr.sizeX(); x++) for (int z = 0; z < pr.sizeZ(); z++) {
            int h = Math.min(26, 2 + (x - 30) * 2);
            for (int y = 0; y < h; y++) in.setBlock(pr.toWorld(x, y, z), y == h - 1 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState(), 2);
        }
        org.overengineer.bettacraft.tank.TankInteractions.changeWater(level, pc, 12000);
        level.setBlockAndUpdate(at.offset(3, 3, 1), ModBlocks.BASKING_LAMP.get().defaultBlockState());
        org.overengineer.bettacraft.terrarium.TankClimate.refreshMode(level, pc);
        herp(level, pc, org.overengineer.bettacraft.terrarium.HerpSpecies.SLIDER, true, 0);
        herp(level, pc, org.overengineer.bettacraft.terrarium.HerpSpecies.SLIDER, false, 1);
        herp(level, pc, org.overengineer.bettacraft.terrarium.HerpSpecies.MUSK, false, 0);
        // M66 point 12: bromeliads, ferns, moss and carnivorous plants on the land and the shore
        landPlants(level, pc, pr, new PlantSpecies[]{PlantSpecies.NEOREGELIA, PlantSpecies.GUZMANIA, PlantSpecies.BOSTON_FERN, PlantSpecies.CUSHION_MOSS,
                PlantSpecies.SUNDEW, PlantSpecies.SARRACENIA, PlantSpecies.VENUS_FLYTRAP, PlantSpecies.SOFT_RUSH}, 34, pr.sizeX() - 4, 6, pr.sizeZ() - 6, 5600);
        name(level, at, "Paludarium: Schmuckschildkröten");
        // a dry terrarium: leopard geckos and a corn snake on sand and stone under a basking lamp
        at = at.east(4 + GAP);
        TankBlockEntity dc = tank(level, at, 4, 2, 3, 0F, false);
        InteriorRegion dr = region(level, dc, Blocks.SAND.defaultBlockState());
        rock(in, dr, dr.sizeX() * 3 / 4, dr.sizeZ() * 2 / 3, 9, 10, Blocks.SANDSTONE.defaultBlockState());
        level.setBlockAndUpdate(at.offset(1, 2, 1), ModBlocks.BASKING_LAMP.get().defaultBlockState());
        org.overengineer.bettacraft.terrarium.TankClimate.refreshMode(level, dc);
        herp(level, dc, org.overengineer.bettacraft.terrarium.HerpSpecies.LEOPARD_GECKO, true, 0);
        herp(level, dc, org.overengineer.bettacraft.terrarium.HerpSpecies.LEOPARD_GECKO, false, 1);
        herp(level, dc, org.overengineer.bettacraft.terrarium.HerpSpecies.CORN_SNAKE, true, 0);
        // M66 point 12: cacti and succulents
        landPlants(level, dc, dr, new PlantSpecies[]{PlantSpecies.BARREL_CACTUS, PlantSpecies.COLUMN_CACTUS, PlantSpecies.PRICKLY_PEAR, PlantSpecies.ECHEVERIA,
                PlantSpecies.HAWORTHIA, PlantSpecies.ALOE, PlantSpecies.LITHOPS, PlantSpecies.SEDUM}, 4, dr.sizeX() / 2, 6, dr.sizeZ() - 6, 5610);
        name(level, at, "Terrarium: Leopardgeckos");
        // a moist terrarium on moss: crested geckos and a horned frog
        at = at.east(4 + GAP);
        TankBlockEntity mc = tank(level, at, 3, 3, 3, 0F, false);
        InteriorRegion mr = region(level, mc, Blocks.MOSS_BLOCK.defaultBlockState());
        rock(in, mr, mr.sizeX() / 3, mr.sizeZ() * 2 / 3, 7, 14, Blocks.OAK_LOG.defaultBlockState());
        org.overengineer.bettacraft.terrarium.TankClimate.refreshMode(level, mc);
        herp(level, mc, org.overengineer.bettacraft.terrarium.HerpSpecies.CRESTED_GECKO, true, 0);
        herp(level, mc, org.overengineer.bettacraft.terrarium.HerpSpecies.CRESTED_GECKO, false, 2);
        herp(level, mc, org.overengineer.bettacraft.terrarium.HerpSpecies.HORNED_FROG, false, 0);
        // M66 point 12: moss, ferns, fittonia
        landPlants(level, mc, mr, new PlantSpecies[]{PlantSpecies.FITTONIA, PlantSpecies.BIRDS_NEST_FERN, PlantSpecies.SELAGINELLA, PlantSpecies.BEGONIA,
                PlantSpecies.PEPEROMIA, PlantSpecies.CUSHION_MOSS, PlantSpecies.POTHOS, PlantSpecies.VRIESEA}, mr.sizeX() / 2, mr.sizeX() - 4, 4, mr.sizeZ() - 6, 5620);
        name(level, at, "Terrarium: Kronengeckos");
        // M31 part d: mobs in glass
        at = at.east(3 + GAP);
        TankBlockEntity ac = tank(level, at, 3, 2, 2, 0F, true);
        region(level, ac, Blocks.GRAVEL.defaultBlockState());
        org.overengineer.bettacraft.terrarium.TankClimate.refreshMode(level, ac);
        glassMob(level, ac, net.minecraft.world.entity.EntityTypes.DROWNED, false);
        glassMob(level, ac, net.minecraft.world.entity.EntityTypes.DROWNED, true);
        name(level, at, "Mobs im Glas: Ertrunkene");
        at = at.east(3 + GAP);
        TankBlockEntity sc = tank(level, at, 3, 2, 2, 0F, false);
        InteriorRegion sr = region(level, sc, Blocks.COARSE_DIRT.defaultBlockState());
        rock(in, sr, sr.sizeX() * 2 / 3, sr.sizeZ() / 2, 8, 8, Blocks.COBBLESTONE.defaultBlockState());
        for (int x = 0; x < 3; x++) for (int z = 0; z < 2; z++) level.setBlockAndUpdate(at.offset(x, 2, z), ModBlocks.TANK_LID.get().defaultBlockState());
        org.overengineer.bettacraft.terrarium.TankClimate.refreshMode(level, sc);
        glassMob(level, sc, net.minecraft.world.entity.EntityTypes.SPIDER, false);
        glassMob(level, sc, net.minecraft.world.entity.EntityTypes.SKELETON, false);
        glassMob(level, sc, net.minecraft.world.entity.EntityTypes.ZOMBIE, false);
        glassMob(level, sc, net.minecraft.world.entity.EntityTypes.SLIME, false);
        name(level, at, "Mobs im Glas: Spinne, Skelett");
        // M35: land animals of other mods (Naturalist), when it is installed
        if (org.overengineer.bettacraft.platform.Platform.get().isModLoaded("naturalist")) {
            at = at.east(3 + GAP);
            TankBlockEntity fc = tank(level, at, 4, 2, 3, 0F, false);
            InteriorRegion fr = region(level, fc, Blocks.SAND.defaultBlockState());
            rock(in, fr, fr.sizeX() / 4, fr.sizeZ() / 3, 8, 8, Blocks.SANDSTONE.defaultBlockState());
            level.setBlockAndUpdate(at.offset(2, 2, 1), ModBlocks.BASKING_LAMP.get().defaultBlockState());
            org.overengineer.bettacraft.terrarium.TankClimate.refreshMode(level, fc);
            for (String id : List.of("naturalist:lizard", "naturalist:snake", "naturalist:desert_scorpion")) foreign(level, fc, id);
            name(level, at, "Landtiere anderer Mods");
        }
        // M39 part c: Minecraft animals as terrarium pets (grass, flowers, a mini beehive in the interior, a lid for the fliers)
        at = at.east(4 + GAP);
        TankBlockEntity vc = tank(level, at, 6, 2, 3, 0F, false); // the cat (0.5 m) wants 6 x 3 x 2
        InteriorRegion vr = region(level, vc, Blocks.GRASS_BLOCK.defaultBlockState());
        for (int x = 0; x < vr.sizeX(); x++) for (int z = 0; z < vr.sizeZ(); z++) {
            int k = (x * 7 + z * 13) % 23;
            if (k == 0) in.setBlock(vr.toWorld(x, 0, z), (x % 2 == 0 ? Blocks.DANDELION : Blocks.POPPY).defaultBlockState(), 2);
            else if (k < 4) in.setBlock(vr.toWorld(x, 0, z), Blocks.SHORT_GRASS.defaultBlockState(), 2);
        }
        rock(in, vr, vr.sizeX() * 3 / 4, vr.sizeZ() / 3, 2, 3, Blocks.OAK_LOG.defaultBlockState());
        in.setBlock(vr.toWorld(vr.sizeX() * 3 / 4, 3, vr.sizeZ() / 3), Blocks.BEEHIVE.defaultBlockState(), 2);
        for (int x = 0; x < 6; x++) for (int z = 0; z < 3; z++) level.setBlockAndUpdate(at.offset(x, 2, z), ModBlocks.TANK_LID.get().defaultBlockState());
        org.overengineer.bettacraft.terrarium.TankClimate.refreshMode(level, vc);
        for (String id : List.of("minecraft:chicken", "minecraft:chicken", "minecraft:rabbit", "minecraft:rabbit", "minecraft:bee", "minecraft:cat")) foreign(level, vc, id);
        name(level, at, "Minecraft-Haustiere");
        LOGGER.info("[showcase] M31 row: paludarium {}, terraria, mobs in glass {}", pc.getBlockPos(), sc.getBlockPos());
    }

    /** M35: a land animal of another mod from its terrarium box into a tank. */
    private static void foreign(ServerLevel level, TankBlockEntity c, String id) {
        var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(net.minecraft.resources.Identifier.parse(id)).orElse(null);
        var e = type != null ? type.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED) : null;
        if (e == null) return;
        ItemStack box = org.overengineer.bettacraft.terrarium.TerrariumBoxItem.fillWith(e);
        e.discard();
        Component err = org.overengineer.bettacraft.tank.TankInhabitants.insert(level, c, box, type, null, 0L);
        if (err != null) LOGGER.warn("[showcase] {}: {}", id, err.getString());
        else animals++;
    }

    private static void herp(ServerLevel level, TankBlockEntity c, org.overengineer.bettacraft.terrarium.HerpSpecies s, boolean female, int variety) {
        var e = org.overengineer.bettacraft.registry.ModEntities.herp(s).create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (e == null) return;
        e.setFemale(female);
        e.setVariety(variety);
        ItemStack box = e.getBucketItemStack();
        e.saveToBucketTag(box);
        e.discard();
        Component err = org.overengineer.bettacraft.tank.TankInhabitants.insert(level, c, box, org.overengineer.bettacraft.registry.ModEntities.herp(s), null, 0L);
        if (err != null) LOGGER.warn("[showcase] {}: {}", s.id(), err.getString());
        else animals++;
    }

    private static void glassMob(ServerLevel level, TankBlockEntity c, net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob> type, boolean baby) {
        var m = type.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (m == null) return;
        if (baby) m.setBaby(true);
        if (m instanceof net.minecraft.world.entity.monster.cubemob.Slime s) s.setSize(1, true);
        ItemStack jar = org.overengineer.bettacraft.mobs.MobJarItem.fillWith(m);
        m.discard();
        Component err = org.overengineer.bettacraft.tank.TankInhabitants.insert(level, c, jar, type, null, 0L);
        if (err != null) LOGGER.warn("[showcase] {}: {}", type.toShortString(), err.getString());
        else animals++;
    }

    /** M30: a big tank (interior scale 8) with sand, rocks to hide under and big animals put in by the size rule. */
    private static void bigTank(ServerLevel level, BlockPos at, int sx, int sy, int sz, float salt, TankmateSpecies[] animals, String title) {
        TankBlockEntity c = tank(level, at, sx, sy, sz, salt, true);
        InteriorRegion ir = region(level, c, Blocks.SAND.defaultBlockState());
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        var rockState = salt > 20 ? ModBlocks.LIVE_ROCK.get().defaultBlockState() : Blocks.STONE.defaultBlockState();
        rock(in, ir, ir.sizeX() / 5, ir.sizeZ() * 2 / 3, 12, 10, rockState);
        rock(in, ir, ir.sizeX() * 4 / 5, ir.sizeZ() * 3 / 5, 14, 12, rockState);
        for (TankmateSpecies s : animals) {
            var tub = org.overengineer.bettacraft.big.BigCatching.caught(level, s, org.overengineer.bettacraft.critters.NetCatching.random(level));
            Component err = tub == null ? Component.literal("no tub") : org.overengineer.bettacraft.tank.TankInhabitants.insert(level, c, tub,
                    org.overengineer.bettacraft.tank.TankInhabitants.containedType(tub), null, 0);
            if (err != null) LOGGER.warn("[showcase] big tank {}: {}", s.id(), err.getString());
        }
        name(level, at, title);
    }

    private static Identifier blockOf(net.minecraft.world.item.Item item) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(((net.minecraft.world.item.BlockItem) item).getBlock());
    }

    /** M21.7: German names of the vanilla cabinet stones (the server only knows the English ones). */
    private static final java.util.Map<String, String> STONES_DE = java.util.Map.ofEntries(
            java.util.Map.entry("stone", "Stein"), java.util.Map.entry("cobblestone", "Bruchstein"), java.util.Map.entry("mossy_cobblestone", "Bemooster Bruchstein"),
            java.util.Map.entry("smooth_stone", "Glatter Stein"), java.util.Map.entry("stone_bricks", "Steinziegel"), java.util.Map.entry("mossy_stone_bricks", "Bemooste Steinziegel"),
            java.util.Map.entry("granite", "Granit"), java.util.Map.entry("polished_granite", "Polierter Granit"), java.util.Map.entry("diorite", "Diorit"),
            java.util.Map.entry("polished_diorite", "Polierter Diorit"), java.util.Map.entry("andesite", "Andesit"), java.util.Map.entry("polished_andesite", "Polierter Andesit"),
            java.util.Map.entry("cobbled_deepslate", "Bruchtiefenschiefer"), java.util.Map.entry("polished_deepslate", "Polierter Tiefenschiefer"),
            java.util.Map.entry("deepslate_bricks", "Tiefenschieferziegel"), java.util.Map.entry("deepslate_tiles", "Tiefenschieferfliesen"), java.util.Map.entry("tuff", "Tuffstein"),
            java.util.Map.entry("polished_tuff", "Polierter Tuffstein"), java.util.Map.entry("tuff_bricks", "Tuffsteinziegel"), java.util.Map.entry("blackstone", "Schwarzstein"),
            java.util.Map.entry("polished_blackstone", "Polierter Schwarzstein"), java.util.Map.entry("polished_blackstone_bricks", "Polierte Schwarzsteinziegel"),
            java.util.Map.entry("sandstone", "Sandstein"), java.util.Map.entry("cut_sandstone", "Geschnittener Sandstein"), java.util.Map.entry("smooth_sandstone", "Glatter Sandstein"),
            java.util.Map.entry("red_sandstone", "Roter Sandstein"), java.util.Map.entry("cut_red_sandstone", "Geschn. roter Sandstein"),
            java.util.Map.entry("smooth_red_sandstone", "Glatter roter Sandstein"), java.util.Map.entry("bricks", "Ziegelsteine"), java.util.Map.entry("nether_bricks", "Netherziegel"),
            java.util.Map.entry("red_nether_bricks", "Rote Netherziegel"), java.util.Map.entry("quartz_block", "Quarzblock"), java.util.Map.entry("smooth_quartz", "Glatter Quarzblock"));

    private static BlockPos startRow(ServerLevel level, String title) {
        // M21.5: the tanks stand on cabinets: the row is one block higher, the cabinets on the ground
        BlockPos start = origin.offset(0, 1, row * ROW);
        String wood = WOODS[row % WOODS.length];
        rowWood = net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(Identifier.withDefaultNamespace(wood))
                ? Identifier.withDefaultNamespace(wood) : Identifier.withDefaultNamespace("oak_planks");
        row++;
        // a path in front of the row and, at its start, a small dry tank named after the row (no vanilla signs: the
        // tanks carry their names on the glass)
        for (int x = -7; x < 150; x++) for (int z = -3; z <= -2; z++) level.setBlockAndUpdate(start.offset(x, -2, z), Blocks.SMOOTH_STONE.defaultBlockState());
        BlockPos header = start.offset(-5, 0, 0);
        tank(level, header, 2, 1, 1, 0F, false);
        name(level, header, row + ": " + title);
        return start;
    }

    /** M66 point 12: grown terrarium plants on a grid of the land between x0..x1, z0..z1 (interior coordinates). */
    private static void landPlants(ServerLevel level, TankBlockEntity c, InteriorRegion r, PlantSpecies[] species, int x0, int x1, int z0, int z1, long seed) {
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        int cols = (int) Math.ceil(Math.sqrt(species.length)), rows = (species.length + cols - 1) / cols, ok = 0;
        for (int i = 0; i < species.length; i++) {
            PlantSpecies s = species[i];
            int x = x0 + (int) ((i % cols + 0.5) * (x1 - x0) / cols), z = z0 + (int) ((i / cols + 0.5) * (z1 - z0) / rows);
            if (InteriorPlants.plantAt(in, r, s, x, topAt(in, r, x, z), z, s.stages(), seed + i) != null) ok++;
            else LOGGER.warn("[showcase] terrarium plant {} did not grow at {} {}", s, x, z);
        }
        InteriorPlants.settleTint(level, c);
        plants += ok;
        LOGGER.info("[showcase] M66 terrarium plants: {} of {} in {}", ok, species.length, c.getBlockPos());
    }

    /**
     * M67: the station "Stromspeicher": one energy store per tank. A tank on six cabinets with two more joined beside them (a big
     * store), a tank whose cabinet and external filter both take the crank (one store), a tank fed by FE (a Powah creative cell
     * when Powah is there, else a sign). Signs give the numbers for EASY and HARD. The showcase world keeps power.costs OFF (the
     * other rows run without power): the signs say how to switch it on.
     */
    private static void powerStore(ServerLevel level) {
        BlockPos at = startRow(level, "Stromspeicher");
        var saved = org.overengineer.bettacraft.config.ServerConfig.POWER_COSTS.get();
        // 1: a 3 x 2 x 2 tank on six cabinets and two joined cabinets on its right
        TankBlockEntity big = tank(level, at, 3, 2, 2, 0F, true);
        for (int z = 0; z < 2; z++) {
            BlockPos cp = at.offset(3, -1, z);
            level.setBlockAndUpdate(cp, ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH));
            if (level.getBlockEntity(cp) instanceof CabinetBlockEntity cab) cab.setWood(rowWood);
            level.setBlockAndUpdate(cp, Block.updateFromNeighbourShapes(level.getBlockState(cp), level, cp));
        }
        light(level, at, 3, 2, 0, DyeColor.WHITE);
        name(level, at, "Großer Speicher: 8 Schränke");
        // 2: a 2 x 2 x 2 tank, the crank at its cabinet and at its external filter
        BlockPos at2 = at.east(5 + GAP);
        TankBlockEntity crank = tank(level, at2, 2, 2, 2, 0F, true);
        level.setBlockAndUpdate(at2.offset(2, 0, 1), ModBlocks.EXTERNAL_FILTER.get().defaultBlockState()
                .setValue(org.overengineer.bettacraft.equipment.ExternalFilterBlock.FACING, Direction.WEST));
        light(level, at2, 2, 2, 0, DyeColor.WHITE);
        name(level, at2, "Kurbel: Schrank + Filter");
        // 3: a 2 x 2 x 2 tank fed by FE
        BlockPos at3 = at2.east(3 + GAP);
        TankBlockEntity fed = tank(level, at3, 2, 2, 2, 0F, true);
        light(level, at3, 2, 2, 0, DyeColor.WHITE);
        boolean powah = org.overengineer.bettacraft.platform.Platform.get().isModLoaded("powah");
        if (powah) {
            var reg = net.minecraft.core.registries.BuiltInRegistries.BLOCK;
            level.setBlockAndUpdate(at3.offset(2, 0, 0), reg.getValue(net.minecraft.resources.Identifier.parse("powah:energy_cable_starter")).defaultBlockState());
            level.setBlockAndUpdate(at3.offset(3, 0, 0), reg.getValue(net.minecraft.resources.Identifier.parse("powah:energy_cell_creative")).defaultBlockState());
        }
        name(level, at3, "FE-Einspeisung");
        // the numbers at EASY and HARD (the devices of tank 2: the filter with its heater in the front cabinet, a light)
        for (TankBlockEntity c : List.of(big, crank, fed)) {
            c.getTank().setTargetTemp(28F); // the heater heats
            TankEquipment.invalidate(c);
        }
        int[] easy = new int[3], hard = new int[3];
        long[] cap = new long[3];
        List<TankBlockEntity> all = List.of(big, crank, fed);
        try {
            for (int i = 0; i < 3; i++) {
                TankBlockEntity c = all.get(i);
                c.getTank().setPowerBuffer(Long.MAX_VALUE / 4);
                org.overengineer.bettacraft.config.ServerConfig.POWER_COSTS.set(org.overengineer.bettacraft.config.ServerConfig.PowerCosts.EASY);
                org.overengineer.bettacraft.tank.TankPower.step(level, c, 1);
                easy[i] = c.lastDemand();
                org.overengineer.bettacraft.config.ServerConfig.POWER_COSTS.set(org.overengineer.bettacraft.config.ServerConfig.PowerCosts.HARD);
                org.overengineer.bettacraft.tank.TankPower.step(level, c, 1);
                hard[i] = c.lastDemand();
                cap[i] = org.overengineer.bettacraft.tank.TankPower.capacity(c);
            }
        } finally {
            org.overengineer.bettacraft.config.ServerConfig.POWER_COSTS.set(saved);
        }
        big.getTank().setPowerBuffer(cap[0] * 6 / 10);
        crank.getTank().setPowerBuffer(cap[1] / 4);
        fed.getTank().setPowerBuffer(cap[2]);
        for (TankBlockEntity c : all) c.markTankChanged();
        long click = org.overengineer.bettacraft.tank.TankStore.fePerClick();
        sign(level, at.offset(1, -1, -2), "Speicher " + kfe(cap[0]), "12 Bl. + 8 Schr.", "EASY " + days(cap[0], easy[0]), "HARD " + days(cap[0], hard[0]));
        sign(level, at2.offset(0, -1, -2), "Kurbel: Schrank", "+ Außenfilter", click + " FE/Klick", ((cap[1] + click - 1) / click) + " Klicks voll");
        sign(level, at2.offset(1, -1, -2), "Speicher " + kfe(cap[1]), "8 Bl. + 4 Schr.", "EASY " + days(cap[1], easy[1]), "HARD " + days(cap[1], hard[1]));
        sign(level, at3.offset(0, -1, -2), "FE-Kabel an", "Becken, Filter,", "Schrank, Licht", powah ? "(Powah-Zelle)" : "(Strom-Mod)");
        sign(level, at3.offset(1, -1, -2), "Welt: costs OFF", "Server-Config", "power.costs =", "EASY / HARD");
        LOGGER.info("[showcase] M67 power store: big {} FE ({} / {} FE/t), crank {} FE ({} / {} FE/t), fed {} FE ({} / {} FE/t), Powah {}",
                cap[0], easy[0], hard[0], cap[1], easy[1], hard[1], cap[2], easy[2], hard[2], powah);
    }

    /** "720k FE". */
    private static String kfe(long fe) {
        return (fe >= 10_000 ? fe / 1000 + "k" : String.valueOf(fe)) + " FE";
    }

    /** How long a full store runs the tank at {@code demand} FE/t: "1.1 Tage" / "8.4 h". */
    private static String days(long cap, int demand) {
        if (demand <= 0) return "-";
        double d = cap / (24_000.0 * demand);
        return d >= 1 ? String.format(java.util.Locale.ROOT, "%.1f Tage", d) : String.format(java.util.Locale.ROOT, "%.1f h", d * 24);
    }

    /** A standing oak sign on the path facing it (north), four short lines. */
    private static void sign(ServerLevel level, BlockPos pos, String... lines) {
        level.setBlockAndUpdate(pos.below(), Blocks.SMOOTH_STONE.defaultBlockState());
        level.setBlockAndUpdate(pos, Blocks.OAK_SIGN.defaultBlockState().setValue(net.minecraft.world.level.block.StandingSignBlock.ROTATION, 8));
        if (level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.SignBlockEntity s) {
            net.minecraft.world.level.block.entity.SignText t = new net.minecraft.world.level.block.entity.SignText();
            for (int i = 0; i < Math.min(4, lines.length); i++) t = t.setMessage(i, Component.literal(lines[i]));
            s.setText(t, true);
            s.setWaxed(true);
            s.setChanged();
            level.sendBlockUpdated(pos, s.getBlockState(), s.getBlockState(), 3);
        }
    }

    // ------------------------------------------------------------------ helpers

    private static TankBlockEntity tank(ServerLevel level, BlockPos min, int sx, int sy, int sz, float salt, boolean water) {
        return tank(level, cuboid(min, sx, sy, sz), min, salt, water, null, null);
    }

    private static TankBlockEntity tank(ServerLevel level, List<BlockPos> blocks, BlockPos min, float salt, boolean water, Identifier glass, TankStyle style) {
        cabinets(level, blocks);
        for (BlockPos pos : blocks) {
            level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
            if (level.getBlockEntity(pos) instanceof TankBlockEntity be) {
                if (glass != null) be.setGlassId(glass);
                if (style != null) be.setStyle(style);
            }
        }
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        if (c == null) throw new IllegalStateException("no tank at " + min);
        if (water) TankInteractions.changeWater(level, c, (long) blocks.size() * 1000, salt);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setWaterTemp(salt > 20 ? 26F : 25F);
        c.getTank().setTargetTemp(salt > 20 ? 26F : 25F);
        c.getTank().setWaterQuality(1F);
        c.getTank().setBreakProtected(true); // M21.5: nobody breaks the showcase by accident
        c.markTankChanged();
        tanks++;
        return c;
    }

    /**
     * M21.5: cabinets of the row's wood under every block of the bottom layer that stands on air (doors to the path,
     * north), an external filter with a medium heater in the front left one.
     */
    private static void cabinets(ServerLevel level, List<BlockPos> blocks) {
        int minY = Integer.MAX_VALUE;
        for (BlockPos p : blocks) minY = Math.min(minY, p.getY());
        List<BlockPos> placed = new ArrayList<>();
        for (BlockPos p : blocks) {
            if (p.getY() != minY) continue;
            BlockPos cp = p.below();
            if (!level.getBlockState(cp).isAir()) continue;
            level.setBlockAndUpdate(cp, ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH));
            if (level.getBlockEntity(cp) instanceof CabinetBlockEntity cab) cab.setWood(rowWood);
            placed.add(cp);
        }
        if (placed.isEmpty()) return;
        BlockPos front = placed.getFirst();
        for (BlockPos cp : placed) {
            if (cp.getZ() < front.getZ() || cp.getZ() == front.getZ() && cp.getX() < front.getX()) front = cp;
        }
        if (level.getBlockEntity(front) instanceof CabinetBlockEntity cab) {
            ItemStack f = new ItemStack(ModItems.EXTERNAL_FILTER.get());
            FilterHeaters.write(f, List.of(new ItemStack(ModItems.HEATER_MEDIUM.get()), ItemStack.EMPTY));
            cab.items().setStackInSlot(CabinetBlockEntity.FILTER_SLOT, f);
            cab.setChanged();
        }
        for (BlockPos cp : placed) level.setBlockAndUpdate(cp, Block.updateFromNeighbourShapes(level.getBlockState(cp), level, cp));
    }

    private static List<BlockPos> cuboid(BlockPos min, int sx, int sy, int sz) {
        List<BlockPos> l = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) l.add(min.offset(x, y, z));
        return l;
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
                if (d > 1.0) continue;
                int top = (int) Math.round(h * (1 - d * d * 0.7));
                for (int y = 0; y < top; y++) in.setBlock(r.toWorld(cx + x, y, cz + z), s, 2);
            }
    }

    private static int topAt(ServerLevel in, InteriorRegion r, int x, int z) {
        int y = 0;
        while (y < r.sizeY() && !in.getBlockState(r.toWorld(x, y, z)).isAir()) y++;
        return y;
    }

    private static void coral(ServerLevel in, InteriorRegion r, PlantSpecies s, int x, int z, long seed) {
        InteriorPlant p = InteriorPlants.plantAt(in, r, s, x, topAt(in, r, x, z), z, s.stages(), seed);
        if (p == null) {
            LOGGER.warn("[showcase] {} not placed", s.id());
            return;
        }
        p.tint = 3;
        p.redness = 1;
        InteriorPlants.apply(in, r, p);
        plants++;
    }

    private static void light(ServerLevel level, BlockPos min, int sx, int y, int z, DyeColor color) {
        for (int x = 0; x < sx; x++) {
            level.setBlockAndUpdate(min.offset(x, y, z), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < sx - 1).setValue(TankLightBlock.LIT, true)
                    .setValue(TankLightBlock.COLOR, color));
        }
    }

    private static void reefLight(ServerLevel level, BlockPos min, int sx) {
        for (int x = 0; x < sx; x++) {
            level.setBlockAndUpdate(min.offset(x, 2, 0), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < sx - 1).setValue(TankLightBlock.LIT, true)
                    .setValue(TankLightBlock.COLOR, x % 2 == 0 ? DyeColor.BLUE : DyeColor.WHITE));
        }
    }

    /** German colour names (the server has no vanilla de_de). */
    private static final String[] COLORS_DE = {"Weiß", "Orange", "Magenta", "Hellblau", "Gelb", "Hellgrün", "Rosa", "Grau", "Hellgrau", "Türkis",
            "Violett", "Blau", "Braun", "Grün", "Rot", "Schwarz"};
    private static Map<String, String> german;

    /** The German name of a key from the mod's de_de.json (the showcase is for the German user), else English, else the key. */
    private static String de(String key) {
        if (german == null) {
            german = new java.util.HashMap<>();
            try (var in = DevShowcase.class.getResourceAsStream("/assets/bettacraft/lang/de_de.json")) {
                if (in != null) {
                    var json = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                    for (var e : json.entrySet()) german.put(e.getKey(), e.getValue().getAsString());
                }
            } catch (java.io.IOException | RuntimeException e) {
                LOGGER.warn("[showcase] de_de.json not readable: {}", e.toString());
            }
        }
        String v = german.get(key);
        return v != null ? v : net.minecraft.locale.Language.getInstance().getOrDefault(key, key.substring(key.lastIndexOf('.') + 1));
    }

    private static String glassName(String id) {
        if (id.equals("glass")) return "Glas";
        if (id.equals("tinted_glass")) return "Getöntes Glas";
        return "Glas " + COLORS_DE[DyeColor.byName(id.replace("_stained_glass", ""), DyeColor.WHITE).ordinal()];
    }

    /**
     * Names the tank at {@code tankMin} (M21 follow-up: instead of a vanilla sign in front of it): the name plate on the
     * glass, top centre of the front, at eye height from the path. At most {@link TankData#MAX_NAME_LENGTH} characters.
     */
    private static void name(ServerLevel level, BlockPos tankMin, String name) {
        TankBlockEntity c = TankMultiblock.getController(level, tankMin);
        if (c == null) {
            LOGGER.warn("[showcase] no tank at {} for the name {}", tankMin, name);
            return;
        }
        // too long: without the hint in brackets ("Otocinclus (Ohrgitter-Harnischwels)" -> "Otocinclus")
        if (name.length() > TankData.MAX_NAME_LENGTH && name.indexOf(" (") > 0) name = name.substring(0, name.indexOf(" ("));
        if (name.length() > TankData.MAX_NAME_LENGTH) LOGGER.warn("[showcase] name too long ({}): {}", name.length(), name);
        c.getTank().setName(name);
        c.getTank().setSignVisible(true);
        c.getTank().setSignPos(SignPosition.TOP_CENTER);
        c.markTankChanged();
        named++;
    }

    private static void betta(ServerLevel level, TankBlockEntity c, Map<String, String> example, boolean female) {
        BettaData d = BettaData.create(BettaGenome.of(example, BettaGenome.DEFAULT), female, level.getGameTime());
        Component err = TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d),
                ModEntities.BETTA.get(), null, false);
        if (err != null) LOGGER.warn("[showcase] betta: {}", err.getString());
        else animals++;
    }

    private static void fish(ServerLevel level, TankBlockEntity c, TankmateSpecies s, int n, RandomSource r) {
        for (int i = 0; i < n; i++) fish(level, c, s, TankmateGenes.wild(s), i % 2 == 0, r);
    }

    private static void fish(ServerLevel level, TankBlockEntity c, TankmateSpecies s, TankmateGenes genes, boolean female, RandomSource r) {
        {
            TankWaterVolume w = new TankWaterVolume(c.getTank());
            Vec3 p = w.clamp(new Vec3(w.box().minX + r.nextDouble() * (w.box().maxX - w.box().minX), w.box().minY + 0.3 + r.nextDouble() * 0.5,
                    w.box().minZ + r.nextDouble() * (w.box().maxZ - w.box().minZ)), 0.35, 0.1, 0.2);
            var e = TankmateBreeding.spawn(level, c, s, genes, female, LifeStage.ADULT, 0, p);
            if (e == null) {
                LOGGER.warn("[showcase] {} not spawned", s.id());
                return;
            }
            e.health().feed();
            animals++;
        }
    }

    private static void critters(ServerLevel level, TankBlockEntity c, CritterSpecies s, int n) {
        critters(level, c, s, n, CritterGenes.WILD);
    }

    private static void critters(ServerLevel level, TankBlockEntity c, CritterSpecies s, int n, CritterGenes g) {
        for (int i = 0; i < n; i++) if (TankCritters.add(level, c, s, g, i % 2 == 0, s.juvenileDays() + 1) != null) animals++;
    }

    private static void frog(ServerLevel level, TankBlockEntity c) {
        DwarfFrogEntity f = ModEntities.DWARF_FROG.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (f == null) return;
        ItemStack bag = FishBagItem.fillWith(f);
        f.discard();
        Component err = TankInhabitants.insert(level, c, bag, ModEntities.DWARF_FROG.get(), null, 0L);
        if (err != null) LOGGER.warn("[showcase] frog: {}", err.getString());
        else animals++;
    }

    private static void segment(ServerLevel level, TankBlockEntity c, SegmentBlock.Kind kind, SegmentBlock.Size size, int lx, int lz) {
        InteriorRegion r = region(level, c, Blocks.SAND.defaultBlockState());
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        BlockPos floor = new BlockPos(r.originX() + lx, InteriorRegion.FLOOR_Y, r.originZ() + lz);
        ItemStack stack = new ItemStack(switch (kind) {
            case HEATER -> size == SegmentBlock.Size.SMALL ? ModItems.HEATER_SMALL.get() : size == SegmentBlock.Size.MEDIUM ? ModItems.HEATER_MEDIUM.get() : ModItems.HEATER_LARGE.get();
            case COOLER -> size == SegmentBlock.Size.SMALL ? ModItems.COOLER_SMALL.get() : size == SegmentBlock.Size.MEDIUM ? ModItems.COOLER_MEDIUM.get() : ModItems.COOLER_LARGE.get();
            default -> ModItems.INTERNAL_FILTER.get();
        });
        var ctx = new UseOnContext(in, null, InteractionHand.MAIN_HAND, stack, new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false));
        var res = EquipmentPlacement.place(ctx, kind, size);
        if (!res.consumesAction()) LOGGER.warn("[showcase] {} {} not placed: {}", kind, size, res);
    }

    private static void equipment(ServerLevel level, BlockPos at, RandomSource r) {
        // external filter with a small and a medium heater
        TankBlockEntity a = tank(level, at, 2, 2, 2, 0F, true);
        BlockPos ef = at.west();
        level.setBlockAndUpdate(ef.below(), Blocks.POLISHED_ANDESITE.defaultBlockState());
        level.setBlockAndUpdate(ef, ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, Direction.EAST));
        if (level.getBlockEntity(ef) instanceof ExternalFilterBlockEntity f) {
            f.insertHeater(new ItemStack(ModItems.HEATER_SMALL.get()));
            f.insertHeater(new ItemStack(ModItems.HEATER_MEDIUM.get()));
        }
        TankEquipment.invalidate(a);
        name(level, at, de("block.bettacraft.external_filter") + " + Heizer");
        at = at.east(2 + GAP + 1);
        // external filter with a cooler
        TankBlockEntity b = tank(level, at, 2, 2, 2, 0F, true);
        ef = at.west();
        level.setBlockAndUpdate(ef.below(), Blocks.POLISHED_ANDESITE.defaultBlockState());
        level.setBlockAndUpdate(ef, ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, Direction.EAST));
        if (level.getBlockEntity(ef) instanceof ExternalFilterBlockEntity f) f.insertHeater(new ItemStack(ModItems.COOLER_MEDIUM.get()));
        TankEquipment.invalidate(b);
        name(level, at, de("block.bettacraft.external_filter") + " + Kühler");
        at = at.east(2 + GAP + 1);
        // a cabinet under the tank with a filter, a heater and food in its storage (M21.5: every tank has its cabinet now)
        TankBlockEntity cab = tank(level, at, 2, 2, 2, 0F, true);
        if (level.getBlockEntity(at.below()) instanceof CabinetBlockEntity cb) {
            cb.storage().insertItem(0, new ItemStack(ModItems.BETTA_PELLETS.get(), 24), false);
            // M29.1: an air pump in the cabinet too: its hose goes up with the filter hose and down to two air stones
            cb.items().setStackInSlot(CabinetBlockEntity.AIR_PUMP_SLOT, new ItemStack(ModItems.AIR_PUMP.get()));
            cb.setChanged();
        }
        InteriorRegion cr = region(level, cab, Blocks.GRAVEL.defaultBlockState());
        ServerLevel cin = InteriorManager.interiorLevel(level.getServer());
        cin.setBlock(cr.toWorld(8, 0, 22), ModBlocks.AIR_STONE.get().defaultBlockState(), 3);
        cin.setBlock(cr.toWorld(22, 0, 16), ModBlocks.AIR_STONE.get().defaultBlockState(), 3);
        TankEquipment.invalidate(cab);
        org.overengineer.bettacraft.tank.TankAeration.step(level, cab, 0.01);
        name(level, at, "Schrank + Filter + Luftpumpe");
        at = at.east(2 + GAP);
        // M21.5: a one-block tank with a light on top and a clip-on feeder on its glass
        TankBlockEntity clip = tank(level, at, 1, 1, 1, 0F, true);
        light(level, at, 1, 1, 0, DyeColor.WHITE);
        level.setBlockAndUpdate(at.north(), ModBlocks.CLIP_FEEDER.get().defaultBlockState()
                .setValue(org.overengineer.bettacraft.equipment.ClipFeederBlock.FACING, Direction.NORTH));
        if (level.getBlockEntity(at.north()) instanceof org.overengineer.bettacraft.equipment.ClipFeederBlockEntity cf) {
            cf.items().insertItem(0, new ItemStack(ModItems.BETTA_PELLETS.get(), 32), false);
            cf.setChanged();
        }
        betta(level, clip, BettaVarietyTable.VARIETIES.getFirst().example(), false);
        TankEquipment.invalidate(clip);
        name(level, at, de("block.bettacraft.clip_feeder"));
        at = at.east(1 + GAP + 1);
        // internal filter, heaters and coolers in the interior
        TankBlockEntity d = tank(level, at, 2, 2, 2, 0F, true);
        segment(level, d, SegmentBlock.Kind.FILTER, null, 4, 26);
        name(level, at, de("item.bettacraft.internal_filter"));
        at = at.east(2 + GAP);
        TankBlockEntity e = tank(level, at, 3, 2, 2, 0F, true);
        segment(level, e, SegmentBlock.Kind.HEATER, SegmentBlock.Size.SMALL, 6, 28);
        segment(level, e, SegmentBlock.Kind.HEATER, SegmentBlock.Size.MEDIUM, 20, 28);
        segment(level, e, SegmentBlock.Kind.HEATER, SegmentBlock.Size.LARGE, 34, 28);
        name(level, at, "Heizstäbe: klein, mittel, groß");
        at = at.east(3 + GAP);
        TankBlockEntity f = tank(level, at, 3, 2, 2, 0F, true);
        segment(level, f, SegmentBlock.Kind.COOLER, SegmentBlock.Size.SMALL, 6, 28);
        segment(level, f, SegmentBlock.Kind.COOLER, SegmentBlock.Size.MEDIUM, 20, 28);
        segment(level, f, SegmentBlock.Kind.COOLER, SegmentBlock.Size.LARGE, 34, 28);
        name(level, at, "Kühler: klein, mittel, groß");
        at = at.east(3 + GAP);
        // lid, feeder and light on top
        TankBlockEntity g = tank(level, at, 3, 2, 2, 0F, true);
        for (int x = 0; x < 3; x++) level.setBlockAndUpdate(at.offset(x, 2, 1), ModBlocks.TANK_LID.get().defaultBlockState());
        level.setBlockAndUpdate(at.offset(0, 2, 0), ModBlocks.TANK_LIGHT.get().defaultBlockState());
        level.setBlockAndUpdate(at.offset(1, 2, 0), ModBlocks.FEEDER.get().defaultBlockState());
        TankEquipment.invalidate(g);
        name(level, at, "Abdeckung, " + de("block.bettacraft.feeder") + ", Licht");
        at = at.east(3 + GAP + 1);
        // protein skimmer and flow pump (salt water)
        TankBlockEntity h = tank(level, at, 2, 2, 2, 35F, true);
        level.setBlockAndUpdate(at.offset(-1, 0, 1), Blocks.POLISHED_ANDESITE.defaultBlockState());
        level.setBlockAndUpdate(at.offset(-1, 1, 1), ModBlocks.PROTEIN_SKIMMER.get().defaultBlockState().setValue(ProteinSkimmerBlock.FACING, Direction.EAST));
        reefLight(level, at, 2);
        InteriorRegion ir = region(level, h, Blocks.SAND.defaultBlockState());
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        BlockPos pumpAt = ir.toWorld(26, 14, 24);
        for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
            BlockState s = ModBlocks.PUMP_SEGMENT.get().defaultBlockState().setValue(SegmentBlock.FACING, Direction.WEST)
                    .setValue(SegmentBlock.PART, y == 1 ? SegmentBlock.Part.TOP : x == 0 && z == 0 ? SegmentBlock.Part.ANCHOR : SegmentBlock.Part.BASE);
            in.setBlock(pumpAt.offset(x, y, z), s, 3);
        }
        TankEquipment.invalidate(h);
        name(level, at, "Abschäumer + " + de("item.bettacraft.flow_pump"));
        at = at.east(2 + GAP + 1);
        multiblockFilters(level, at);
    }

    /**
     * M29.1: a multiblock filter connected to its tank by pipes (two separate runs, inlet low at the front, outlet high at the
     * back) and one standing directly under its tank (inlet and outlet on top of the chambers, facing up into the floor).
     */
    private static void multiblockFilters(ServerLevel level, BlockPos at) {
        net.minecraft.world.item.Item[] media = {ModItems.FILTER_FOAM.get(), ModItems.CERAMIC_RINGS.get(), ModItems.FILTER_PEAT.get(), ModItems.FILTER_FOAM.get()};
        // by pipes: tank x 0..2, pipes x 3..4, connectors x 5, chambers x 6 (2 x 2), pump head on top
        TankBlockEntity pt = tank(level, at, 3, 2, 2, 0F, true);
        for (int x = 3; x <= 6; x++) for (int z = 0; z < 2; z++) level.setBlockAndUpdate(at.offset(x, -1, z), Blocks.POLISHED_ANDESITE.defaultBlockState());
        int k = 0;
        for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
            level.setBlockAndUpdate(at.offset(6, y, z), ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
            if (level.getBlockEntity(at.offset(6, y, z)) instanceof org.overengineer.bettacraft.filter.FilterChamberBlockEntity ch) ch.setMedium(new ItemStack(media[k++]));
        }
        level.setBlockAndUpdate(at.offset(6, 2, 0), ModBlocks.FILTER_PUMP_HEAD.get().defaultBlockState());
        level.setBlockAndUpdate(at.offset(5, 0, 0), ModBlocks.FILTER_INLET.get().defaultBlockState().setValue(org.overengineer.bettacraft.filter.FilterPartBlock.FACING, Direction.WEST));
        level.setBlockAndUpdate(at.offset(5, 1, 1), ModBlocks.FILTER_OUTLET.get().defaultBlockState().setValue(org.overengineer.bettacraft.filter.FilterPartBlock.FACING, Direction.WEST));
        for (int x = 3; x <= 4; x++) {
            level.setBlockAndUpdate(at.offset(x, 0, 0), ModBlocks.GLASS_PIPE.get().defaultBlockState());
            level.setBlockAndUpdate(at.offset(x, 1, 1), ModBlocks.GLASS_PIPE.get().defaultBlockState());
        }
        for (int x = 3; x <= 5; x++) for (int[] yz : new int[][]{{0, 0}, {1, 1}}) {
            BlockPos p = at.offset(x, yz[0], yz[1]);
            level.setBlockAndUpdate(p, Block.updateFromNeighbourShapes(level.getBlockState(p), level, p));
        }
        org.overengineer.bettacraft.gametest.FilterLinesGameTests.guardAll(level, at.offset(6, 2, 0)); // M40
        name(level, at, "Multiblock-Filter über Rohre");
        LOGGER.info("[showcase] M29.1 multiblock filter by pipes at {}", pt.getBlockPos());
        at = at.east(7 + GAP);
        // under the tank: chambers 2 x 2 on the cabinet layer, two more chambers and the connectors above them, the pump head beside
        BlockPos fl = at.below();
        k = 0;
        for (int z = 0; z < 2; z++) for (int x = 0; x < 2; x++) {
            level.setBlockAndUpdate(fl.offset(x, 0, z), ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
            if (level.getBlockEntity(fl.offset(x, 0, z)) instanceof org.overengineer.bettacraft.filter.FilterChamberBlockEntity ch) ch.setMedium(new ItemStack(media[k++]));
        }
        for (int x = 0; x < 2; x++) level.setBlockAndUpdate(at.offset(x, 0, 1), ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
        level.setBlockAndUpdate(at.offset(0, 0, 0), ModBlocks.FILTER_INLET.get().defaultBlockState().setValue(org.overengineer.bettacraft.filter.FilterPartBlock.FACING, Direction.UP));
        level.setBlockAndUpdate(at.offset(1, 0, 0), ModBlocks.FILTER_OUTLET.get().defaultBlockState().setValue(org.overengineer.bettacraft.filter.FilterPartBlock.FACING, Direction.UP));
        level.setBlockAndUpdate(fl.offset(2, 0, 0), ModBlocks.FILTER_PUMP_HEAD.get().defaultBlockState());
        TankBlockEntity ut = tank(level, at.above(), 2, 2, 2, 0F, true);
        org.overengineer.bettacraft.gametest.FilterLinesGameTests.guardAll(level, fl.offset(2, 0, 0)); // M40
        name(level, at.above(), "Multiblock-Filter unter dem Becken");
        LOGGER.info("[showcase] M29.1 multiblock filter under the tank at {}", ut.getBlockPos());
    }

    /**
     * M40: one multiblock filter for two tanks. The intake (blue) and return (orange) lines leave the filter in a row of pipe
     * bundles that also carry the fresh (green) and waste (brown) water lines, then branch with T-pieces into both tanks
     * (guards inside). The fresh water line ends at a reservoir with water, the waste water line at an empty reservoir; a
     * level sensor and a water change controller sit on the filter. Two bundles wear covers (stone bricks, oak planks).
     * Layout (x east, z south): tanks x 0..1 at z 0..1 and z 3..4, T-pieces x 2, bundles x 3..6 at z 2, filter x 7..9.
     */
    private static void filterLines(ServerLevel level, BlockPos at) {
        for (int x = 2; x <= 9; x++) for (int z = 0; z <= 4; z++) level.setBlockAndUpdate(at.offset(x, -1, z), Blocks.POLISHED_ANDESITE.defaultBlockState());
        TankBlockEntity a = tank(level, at, 2, 2, 2, 0F, true);
        TankBlockEntity b = tank(level, at.offset(0, 0, 3), 2, 2, 2, 0F, true);
        for (int y = 0; y < 2; y++) {
            level.setBlockAndUpdate(at.offset(8, y, 2), ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
            if (level.getBlockEntity(at.offset(8, y, 2)) instanceof org.overengineer.bettacraft.filter.FilterChamberBlockEntity ch)
                ch.setMedium(new ItemStack(y == 0 ? ModItems.CERAMIC_RINGS.get() : ModItems.FILTER_FOAM.get()));
        }
        BlockPos head = at.offset(8, 2, 2);
        var facing = org.overengineer.bettacraft.filter.FilterPartBlock.FACING;
        level.setBlockAndUpdate(head, ModBlocks.FILTER_PUMP_HEAD.get().defaultBlockState());
        level.setBlockAndUpdate(at.offset(7, 0, 2), ModBlocks.FILTER_INLET.get().defaultBlockState().setValue(facing, Direction.WEST));
        level.setBlockAndUpdate(at.offset(7, 1, 2), ModBlocks.FILTER_OUTLET.get().defaultBlockState().setValue(facing, Direction.WEST));
        level.setBlockAndUpdate(at.offset(8, 0, 1), ModBlocks.FILTER_FRESH_PORT.get().defaultBlockState().setValue(facing, Direction.NORTH));
        level.setBlockAndUpdate(at.offset(8, 0, 3), ModBlocks.FILTER_WASTE_PORT.get().defaultBlockState().setValue(facing, Direction.SOUTH));
        level.setBlockAndUpdate(at.offset(9, 0, 2), ModBlocks.LEVEL_SENSOR.get().defaultBlockState());
        level.setBlockAndUpdate(at.offset(9, 1, 2), ModBlocks.WATER_CHANGE_CONTROLLER.get().defaultBlockState());
        level.setBlockAndUpdate(at.offset(3, 0, 0), ModBlocks.RESERVOIR.get().defaultBlockState());
        level.setBlockAndUpdate(at.offset(3, 0, 4), ModBlocks.RESERVOIR.get().defaultBlockState());
        if (level.getBlockEntity(at.offset(3, 0, 0)) instanceof org.overengineer.bettacraft.plumbing.ReservoirBlockEntity res)
            res.water().fill(new org.overengineer.bettacraft.platform.fluid.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 16000),
                    org.overengineer.bettacraft.platform.fluid.IFluidHandler.FluidAction.EXECUTE);
        Block in = ModBlocks.FILTER_INLET_PIPE.get(), out = ModBlocks.FILTER_OUTLET_PIPE.get(), fresh = ModBlocks.FILTER_FRESH_PIPE.get(), waste = ModBlocks.FILTER_WASTE_PIPE.get();
        // the branches at the tanks first (T-pieces at x 2), then the runs to the filter
        for (int[] q : new int[][]{{2, 0, 1}, {2, 0, 3}, {2, 0, 2}}) level.setBlockAndUpdate(at.offset(q[0], q[1], q[2]), in.defaultBlockState());
        for (int[] q : new int[][]{{2, 1, 1}, {2, 1, 3}, {2, 1, 2}, {3, 1, 2}}) level.setBlockAndUpdate(at.offset(q[0], q[1], q[2]), out.defaultBlockState());
        org.overengineer.bettacraft.plumbing.FilterPipeBlock.makeJunction(level, at.offset(2, 0, 2));
        org.overengineer.bettacraft.plumbing.FilterPipeBlock.makeJunction(level, at.offset(2, 1, 2));
        for (int x = 3; x <= 6; x++) level.setBlockAndUpdate(at.offset(x, 0, 2), in.defaultBlockState());
        level.setBlockAndUpdate(at.offset(6, 1, 2), out.defaultBlockState());
        for (int[] q : new int[][]{{8, 0, 0}, {7, 0, 0}, {6, 0, 0}, {6, 0, 1}, {3, 0, 1}}) level.setBlockAndUpdate(at.offset(q[0], q[1], q[2]), fresh.defaultBlockState());
        for (int[] q : new int[][]{{8, 0, 4}, {7, 0, 4}, {6, 0, 4}, {6, 0, 3}, {3, 0, 3}}) level.setBlockAndUpdate(at.offset(q[0], q[1], q[2]), waste.defaultBlockState());
        // the bundles: from the filter end on, every line joins its pipe at the end
        for (int x = 6; x >= 3; x--)
            for (var l : new org.overengineer.bettacraft.plumbing.PipeLine[]{org.overengineer.bettacraft.plumbing.PipeLine.RETURN,
                    org.overengineer.bettacraft.plumbing.PipeLine.FRESH, org.overengineer.bettacraft.plumbing.PipeLine.WASTE})
                org.overengineer.bettacraft.plumbing.FilterPipeBundleBlock.addLine(level, at.offset(x, 0, 2), l, null, null);
        org.overengineer.bettacraft.gametest.FilterLinesGameTests.guardAll(level, head);
        var st = org.overengineer.bettacraft.filter.MultiblockFilter.scan(level, head);
        int tanksServed = org.overengineer.bettacraft.filter.MultiblockFilter.links(level, st).size();
        if (tanksServed != 2 || st.fresh().size() != 1 || st.waste().size() != 1)
            LOGGER.warn("[showcase] M40 the filter reaches {} tanks, {} fresh and {} waste ports", tanksServed, st.fresh().size(), st.waste().size());
        org.overengineer.bettacraft.plumbing.PipeCovers.apply(level, at.offset(4, 0, 2), Blocks.STONE_BRICKS.defaultBlockState(), new ItemStack(Blocks.STONE_BRICKS), null);
        org.overengineer.bettacraft.plumbing.PipeCovers.apply(level, at.offset(5, 0, 2), Blocks.OAK_PLANKS.defaultBlockState(), new ItemStack(Blocks.OAK_PLANKS), null);
        name(level, at, "Ein Filter, zwei Becken");
        name(level, at.offset(0, 0, 3), "Rohrbündel mit vier Leitungen");
        LOGGER.info("[showcase] M40 filter lines at {}: {} tanks ({} and {})", at, tanksServed, a.getBlockPos(), b.getBlockPos());
    }

    private static void glassAndShapes(ServerLevel level, BlockPos at) {
        List<Identifier> glass = new ArrayList<>();
        glass.add(Identifier.withDefaultNamespace("glass"));
        for (DyeColor c : DyeColor.values()) glass.add(Identifier.withDefaultNamespace(c.getName() + "_stained_glass"));
        glass.add(Identifier.withDefaultNamespace("tinted_glass"));
        for (Identifier g : glass) {
            tank(level, cuboid(at, 2, 1, 2), at, 0F, true, g, TankStyle.TEXTURED);
            name(level, at, glassName(g.getPath()));
            at = at.east(2 + GAP);
        }
        tank(level, cuboid(at, 2, 2, 2), at, 0F, true, Identifier.withDefaultNamespace("glass"), TankStyle.CLEAN);
        name(level, at, "Stil: klar (clean)");
        at = at.east(2 + GAP);
        tank(level, cuboid(at, 2, 2, 2), at, 0F, true, Identifier.withDefaultNamespace("glass"), TankStyle.TEXTURED);
        name(level, at, "Stil: texturiert (textured)");
        at = at.east(2 + GAP);
        // free shapes: an L, a T, a step
        List<BlockPos> l = new ArrayList<>();
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 3; x++) l.add(at.offset(x, y, 0));
            for (int z = 1; z < 3; z++) l.add(at.offset(0, y, z));
        }
        tank(level, l, at, 0F, true, null, null);
        name(level, at, "Form: L");
        at = at.east(3 + GAP);
        List<BlockPos> t = new ArrayList<>();
        for (int x = 0; x < 3; x++) t.add(at.offset(x, 0, 0));
        for (int z = 1; z < 3; z++) t.add(at.offset(1, 0, z));
        tank(level, t, at, 0F, true, null, null);
        name(level, at, "Form: T");
        at = at.east(3 + GAP);
        List<BlockPos> s = new ArrayList<>(cuboid(at, 3, 1, 2));
        s.add(at.offset(0, 1, 0));
        s.add(at.offset(0, 1, 1));
        tank(level, s, at, 0F, true, null, null);
        name(level, at, "Form: Stufe");
    }

    /** M21.8: seagrass, tall seagrass and two kelp stems in an interior (hitching posts for seahorses). */
    private static void seagrass(ServerLevel in, InteriorRegion r) {
        for (int x = 2; x < r.sizeX() - 2; x++) {
            for (int z = 2; z < Math.min(r.sizeZ() - 2, 22); z++) {
                int k = (x * 7 + z * 13) % 11;
                if (k == 0) in.setBlock(r.toWorld(x, 0, z), Blocks.TALL_SEAGRASS.defaultBlockState(), 2);
                else if (k == 3 || k == 6) in.setBlock(r.toWorld(x, 0, z), Blocks.SEAGRASS.defaultBlockState(), 2);
            }
        }
        int top = Math.min(r.sizeY() - 6, 22);
        for (int[] kp : new int[][]{{r.sizeX() / 4, r.sizeZ() * 2 / 3}, {r.sizeX() * 3 / 4, r.sizeZ() * 2 / 3}}) {
            for (int y = 0; y < top; y++) in.setBlock(r.toWorld(kp[0], y, kp[1]), (y == top - 1 ? Blocks.KELP : Blocks.KELP_PLANT).defaultBlockState(), 2);
        }
    }

    private static void reef(ServerLevel level, BlockPos at, RandomSource r) {
        TankBlockEntity c = tank(level, at, 5, 3, 3, 35F, true);
        InteriorRegion ir = region(level, c, Blocks.SAND.defaultBlockState());
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        rock(in, ir, 40, 36, 11, 10, ModBlocks.REEF_ROCK.get().defaultBlockState());
        rock(in, ir, 16, 34, 9, 7, ModBlocks.LIVE_ROCK.get().defaultBlockState());
        rock(in, ir, 66, 32, 9, 8, ModBlocks.LIVE_ROCK.get().defaultBlockState());
        rock(in, ir, 22, 16, 5, 4, ModBlocks.LIVE_ROCK.get().defaultBlockState());
        long seed = 5000;
        coral(in, ir, PlantSpecies.ACROPORA, 40, 34, seed++);
        coral(in, ir, PlantSpecies.HAMMER_CORAL, 30, 32, seed++);
        coral(in, ir, PlantSpecies.LOBOPHYLLIA, 50, 33, seed++);
        coral(in, ir, PlantSpecies.LEATHER_CORAL, 16, 34, seed++);
        coral(in, ir, PlantSpecies.GREEN_STAR_POLYPS, 66, 32, seed++);
        coral(in, ir, PlantSpecies.MUSHROOM_CORAL, 60, 40, seed++);
        coral(in, ir, PlantSpecies.ZOANTHUS, 12, 30, seed++);
        coral(in, ir, PlantSpecies.BUBBLE_TIP_ANEMONE, 22, 16, seed);
        for (int x = 0; x < 5; x++) {
            level.setBlockAndUpdate(at.offset(x, 3, 1), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 4).setValue(TankLightBlock.LIT, true)
                    .setValue(TankLightBlock.COLOR, x % 2 == 0 ? DyeColor.BLUE : DyeColor.WHITE));
        }
        for (int y = 0; y < 2; y++) level.setBlockAndUpdate(at.offset(-1, y, 1), Blocks.POLISHED_ANDESITE.defaultBlockState());
        level.setBlockAndUpdate(at.offset(-1, 2, 1), ModBlocks.PROTEIN_SKIMMER.get().defaultBlockState().setValue(ProteinSkimmerBlock.FACING, Direction.EAST));
        BlockPos pumpAt = ir.toWorld(76, 20, 44);
        for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
            BlockState s = ModBlocks.PUMP_SEGMENT.get().defaultBlockState().setValue(SegmentBlock.FACING, Direction.WEST)
                    .setValue(SegmentBlock.PART, y == 1 ? SegmentBlock.Part.TOP : x == 0 && z == 0 ? SegmentBlock.Part.ANCHOR : SegmentBlock.Part.BASE);
            in.setBlock(pumpAt.offset(x, y, z), s, 3);
        }
        TankEquipment.invalidate(c);
        fish(level, c, TankmateSpecies.OCELLARIS_CLOWNFISH, 2, r);
        fish(level, c, TankmateSpecies.GREEN_CHROMIS, 5, r);
        fish(level, c, TankmateSpecies.YELLOW_TANG, 1, r);
        fish(level, c, TankmateSpecies.YELLOW_WATCHMAN_GOBY, 1, r);
        fish(level, c, TankmateSpecies.YELLOWTAIL_DAMSEL, 1, r);
        critters(level, c, CritterSpecies.CLEANER_SHRIMP, 2);
        critters(level, c, CritterSpecies.HERMIT_CRAB, 3);
        critters(level, c, CritterSpecies.TURBO_SNAIL, 2);
        name(level, at, "Riffbecken (35 ‰)");
    }
}
