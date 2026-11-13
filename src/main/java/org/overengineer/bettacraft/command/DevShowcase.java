package org.overengineer.bettacraft.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
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
 * M21: builds the showcase world "Bettacraft Schaukasten" ({@code gradlew buildShowcase}: a dedicated server without a
 * window on a fresh flat world in {@code run/saves}, {@code -Dbettacraft.showcase=true}). Rows of tanks grouped by theme,
 * a sign in front of every tank (translated names) and a sign at the start of every row: every betta variety, every
 * tankmate, every small animal and the frog, every aquarium plant (grown, under strong light), every floating plant,
 * every decoration, the equipment (filters, heaters, coolers, lid and feeder, skimmer, pump), the 16 light colours, the
 * glass types, styles and free shapes, every coral and reef animal in its own salt water tank, a reef tank and a
 * brackish tank. The animals live normally (not frozen). Then the world is saved and the server stops.
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class DevShowcase {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/showcase");
    private static final boolean ENABLED = Boolean.getBoolean("bettacraft.showcase");
    private static final int GAP = 4, ROW = 9;
    private static int ticks;
    private static int haltAt = -1;
    private static int builtAt = -1;
    private static int tanks, animals, plants;
    private static BlockPos origin;
    private static int row;
    private static final List<ChunkPos> forced = new ArrayList<>();

    private DevShowcase() {}

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (!ENABLED) return;
        MinecraftServer server = event.getServer();
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
                    level.getChunk(cp.x, cp.z);
                    ready &= level.areEntitiesLoaded(cp.toLong());
                }
                if (!ready && ticks < 1200) return;
                builtAt = ticks;
                build(level);
                LOGGER.info("[showcase] built {} rows, {} tanks, {} animals, {} plants at {}", row, tanks, animals, plants, origin);
            } else if (builtAt > 0 && ticks == builtAt + 40) {
                int live = 0, any = 0;
                for (var e : level.getAllEntities()) {
                    any++;
                    if (e instanceof org.overengineer.bettacraft.entity.betta.BettaEntity || e instanceof org.overengineer.bettacraft.entity.tankmate.TankmateEntity) live++;
                }
                LOGGER.info("[showcase] tick {}: {} fish of {} entities", ticks, live, any);
            } else if (builtAt > 0 && ticks == builtAt + 400) {
                // the care loop ran for a while: settle the plant colours under their lights, then save
                int live = 0;
                for (var e : level.getAllEntities()) {
                    if (e instanceof org.overengineer.bettacraft.entity.betta.BettaEntity || e instanceof org.overengineer.bettacraft.entity.tankmate.TankmateEntity
                            || e instanceof DwarfFrogEntity) live++;
                }
                LOGGER.info("[showcase] {} fish and frogs alive in the world", live);
                LOGGER.info("[showcase] saving");
                for (ChunkPos cp : forced) level.setChunkForced(cp.x, cp.z, false);
                server.saveEverything(false, true, true);
                LOGGER.info("[showcase] SHOWCASE BUILT: {} tanks, {} animals, {} plants", tanks, animals, plants);
                haltAt = ticks + 20;
            } else if (ticks == haltAt) {
                server.halt(false);
            }
        } catch (RuntimeException e) {
            LOGGER.error("[showcase] FAILED", e);
            server.halt(false);
        }
    }

    private static void prepare(MinecraftServer server, ServerLevel level) {
        GameRules rules = level.getGameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        rules.getRule(GameRules.RULE_DO_PATROL_SPAWNING).set(false, server);
        rules.getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(false, server);
        level.setDayTime(6000);
        level.setWeatherParameters(120000, 0, false, false);
        BlockPos spawn = level.getSharedSpawnPos();
        int ground = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, spawn.getX(), spawn.getZ());
        origin = new BlockPos(spawn.getX() + 3, ground, spawn.getZ() + 6);
        level.setDefaultSpawnPos(new BlockPos(spawn.getX(), ground, spawn.getZ()), 180F);
        // keep the whole showcase loaded while it is built (the animals are entities)
        for (int cx = (origin.getX() >> 4) - 1; cx <= (origin.getX() + 110 >> 4) + 1; cx++)
            for (int cz = (origin.getZ() >> 4) - 1; cz <= (origin.getZ() + 12 * ROW + 10 >> 4) + 1; cz++) {
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
                f.set(pd, new net.minecraft.world.level.LevelSettings("Bettacraft Schaukasten", net.minecraft.world.level.GameType.CREATIVE, false,
                        net.minecraft.world.Difficulty.PEACEFUL, true, s.gameRules(), s.getDataConfiguration()));
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("[showcase] could not switch on cheats: {}", e.toString());
        }
    }

    // ------------------------------------------------------------------ rows

    private static void build(ServerLevel level) {
        RandomSource r = RandomSource.create(21);
        // 1: bettas, one tank per named variety
        BlockPos at = startRow(level, "Bettas", "Zuchtformen");
        for (var v : BettaVarietyTable.VARIETIES) {
            TankBlockEntity c = tank(level, at, 2, 2, 2, 0F, true);
            betta(level, c, v.example(), false);
            betta(level, c, v.example(), true);
            sign(level, at, Component.translatable("variety.bettacraft." + v.id()));
            at = at.east(2 + GAP);
        }
        // 2: tankmates (fresh water), a small group each
        at = startRow(level, "Beifische", "Süßwasser");
        for (TankmateSpecies s : TankmateSpecies.values()) {
            if (s.marine()) continue;
            TankBlockEntity c = tank(level, at, 2, 2, 2, 0F, true);
            fish(level, c, s, Math.min(4, Math.max(2, s.shoal())), r);
            sign(level, at, Component.translatable(s.translationKey()));
            at = at.east(2 + GAP);
        }
        // 3: small animals (fresh water) and the frog
        at = startRow(level, "Kleintiere", "Garnelen, Krebse,", "Schnecken, Frosch");
        for (CritterSpecies s : CritterSpecies.values()) {
            if (s.marine()) continue;
            TankBlockEntity c = tank(level, at, 2, 1, 2, 0F, true);
            critters(level, c, s, 6);
            sign(level, at, Component.translatable(s.translationKey()));
            at = at.east(2 + GAP);
        }
        TankBlockEntity frogs = tank(level, at, 2, 2, 2, 0F, true);
        for (int i = 0; i < 2; i++) frog(level, frogs);
        sign(level, at, Component.translatable("entity.bettacraft.dwarf_frog"));
        // 4: aquarium plants, grown, strong light
        at = startRow(level, "Aquarienpflanzen", "starkes Licht:", "rote Arten rot");
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
            sign(level, at, Component.translatable("item.bettacraft." + s.id()));
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
            sign(level, at, Component.translatable("item.bettacraft." + t.id()));
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
            sign(level, at, Component.translatable("item.bettacraft.decor_" + d.id()));
            at = at.east(2 + GAP);
        }
        // 7: equipment
        at = startRow(level, "Technik", "Filter, Heizer,", "Kühler, Pumpe");
        equipment(level, at, r);
        // 8: the 16 light colours (at night they light the water)
        at = startRow(level, "Lichtfarben");
        for (DyeColor color : DyeColor.values()) {
            tank(level, at, 2, 1, 1, 0F, true);
            light(level, at, 2, 1, 0, color);
            sign(level, at, Component.translatable("color.minecraft." + color.getName()));
            at = at.east(2 + GAP);
        }
        // 9: glass types, styles and free shapes
        at = startRow(level, "Glas und Formen");
        glassAndShapes(level, at);
        // 10: salt water: a reef tank, a brackish tank, every coral and reef animal on its own
        at = startRow(level, "Meerwasser", "Riff, Brackwasser,", "Korallen, Rifftiere");
        reef(level, at, r);
        at = at.east(5 + GAP);
        TankBlockEntity brack = tank(level, at, 3, 2, 2, 12F, true);
        fish(level, brack, TankmateSpecies.GUPPY, 4, r);
        fish(level, brack, TankmateSpecies.PLATY, 3, r);
        critters(level, brack, CritterSpecies.ZEBRA_NERITE, 3);
        sign(level, at, Component.literal("Brackwasser"), Component.literal("12 ‰"));
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
            sign(level, at, Component.translatable("item.bettacraft." + s.id()));
            at = at.east(2 + GAP);
        }
        for (TankmateSpecies s : TankmateSpecies.values()) {
            if (!s.marine()) continue;
            TankBlockEntity c = tank(level, at, 2, 2, 2, 35F, true);
            reefLight(level, at, 2);
            InteriorRegion ir = region(level, c, Blocks.SAND.defaultBlockState());
            rock(InteriorManager.interiorLevel(level.getServer()), ir, 16, 20, 6, 5, ModBlocks.LIVE_ROCK.get().defaultBlockState());
            fish(level, c, s, Math.min(4, Math.max(1, s.shoal())), r);
            sign(level, at, Component.translatable(s.translationKey()));
            at = at.east(2 + GAP);
        }
        for (CritterSpecies s : CritterSpecies.values()) {
            if (!s.marine()) continue;
            TankBlockEntity c = tank(level, at, 2, 1, 2, 35F, true);
            reefLight(level, at, 2);
            InteriorRegion ir = region(level, c, Blocks.SAND.defaultBlockState());
            rock(InteriorManager.interiorLevel(level.getServer()), ir, 16, 20, 5, 4, ModBlocks.LIVE_ROCK.get().defaultBlockState());
            critters(level, c, s, 4);
            sign(level, at, Component.translatable(s.translationKey()));
            at = at.east(2 + GAP);
        }
    }

    private static BlockPos startRow(ServerLevel level, String... title) {
        BlockPos start = origin.offset(0, 0, row * ROW);
        row++;
        // a path in front of the row and the title sign at its start
        for (int x = -3; x < 130; x++) for (int z = -3; z <= -2; z++) level.setBlockAndUpdate(start.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
        Component[] lines = new Component[title.length];
        for (int i = 0; i < title.length; i++) lines[i] = Component.literal(title[i]).withStyle(i == 0 ? net.minecraft.ChatFormatting.BOLD : net.minecraft.ChatFormatting.RESET);
        sign(level, start.west(2), lines);
        return start;
    }

    // ------------------------------------------------------------------ helpers

    private static TankBlockEntity tank(ServerLevel level, BlockPos min, int sx, int sy, int sz, float salt, boolean water) {
        return tank(level, cuboid(min, sx, sy, sz), min, salt, water, null, null);
    }

    private static TankBlockEntity tank(ServerLevel level, List<BlockPos> blocks, BlockPos min, float salt, boolean water, ResourceLocation glass, TankStyle style) {
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
        c.markTankChanged();
        tanks++;
        return c;
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

    private static void sign(ServerLevel level, BlockPos tankMin, Component... lines) {
        BlockPos at = tankMin.north(2);
        level.setBlockAndUpdate(at.below(), Blocks.SMOOTH_STONE.defaultBlockState());
        level.setBlockAndUpdate(at, Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 8));
        if (level.getBlockEntity(at) instanceof SignBlockEntity s) {
            SignText t = new SignText();
            for (int i = 0; i < Math.min(4, lines.length); i++) t = t.setMessage(i, lines[i]);
            s.setText(t, true);
            s.setWaxed(true);
            s.setChanged();
            level.sendBlockUpdated(at, s.getBlockState(), s.getBlockState(), Block.UPDATE_ALL);
        }
    }

    private static void betta(ServerLevel level, TankBlockEntity c, Map<String, String> example, boolean female) {
        BettaData d = BettaData.create(BettaGenome.of(example, BettaGenome.DEFAULT), female, level.getGameTime());
        Component err = TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d),
                ModEntities.BETTA.get(), null, false);
        if (err != null) LOGGER.warn("[showcase] betta: {}", err.getString());
        else animals++;
    }

    private static void fish(ServerLevel level, TankBlockEntity c, TankmateSpecies s, int n, RandomSource r) {
        for (int i = 0; i < n; i++) {
            TankWaterVolume w = new TankWaterVolume(c.getTank());
            Vec3 p = w.clamp(new Vec3(w.box().minX + r.nextDouble() * (w.box().maxX - w.box().minX), w.box().minY + 0.3 + r.nextDouble() * 0.5,
                    w.box().minZ + r.nextDouble() * (w.box().maxZ - w.box().minZ)), 0.35, 0.1, 0.2);
            var e = TankmateBreeding.spawn(level, c, s, TankmateGenes.wild(s), i % 2 == 0, LifeStage.ADULT, 0, p);
            if (e == null) {
                LOGGER.warn("[showcase] {} not spawned", s.id());
                continue;
            }
            e.health().feed();
            animals++;
        }
    }

    private static void critters(ServerLevel level, TankBlockEntity c, CritterSpecies s, int n) {
        for (int i = 0; i < n; i++) if (TankCritters.add(level, c, s, CritterGenes.WILD, i % 2 == 0, s.juvenileDays() + 1) != null) animals++;
    }

    private static void frog(ServerLevel level, TankBlockEntity c) {
        DwarfFrogEntity f = ModEntities.DWARF_FROG.get().create(level);
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
        level.setBlockAndUpdate(ef, ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, Direction.EAST));
        if (level.getBlockEntity(ef) instanceof ExternalFilterBlockEntity f) {
            f.insertHeater(new ItemStack(ModItems.HEATER_SMALL.get()));
            f.insertHeater(new ItemStack(ModItems.HEATER_MEDIUM.get()));
        }
        TankEquipment.invalidate(a);
        sign(level, at, Component.translatable("block.bettacraft.external_filter"), Component.literal("+ Heizer"));
        at = at.east(2 + GAP + 1);
        // external filter with a cooler
        TankBlockEntity b = tank(level, at, 2, 2, 2, 0F, true);
        ef = at.west();
        level.setBlockAndUpdate(ef, ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, Direction.EAST));
        if (level.getBlockEntity(ef) instanceof ExternalFilterBlockEntity f) f.insertHeater(new ItemStack(ModItems.COOLER_MEDIUM.get()));
        TankEquipment.invalidate(b);
        sign(level, at, Component.translatable("block.bettacraft.external_filter"), Component.literal("+ Kühler"));
        at = at.east(2 + GAP + 1);
        // a cabinet under the tank with a filter and a heater in it
        for (int x = 0; x < 2; x++) {
            BlockPos cp = at.offset(x, 0, 0);
            level.setBlockAndUpdate(cp, ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH));
            if (x == 0 && level.getBlockEntity(cp) instanceof CabinetBlockEntity cab) {
                ItemStack f = new ItemStack(ModItems.EXTERNAL_FILTER.get());
                FilterHeaters.write(f, List.of(new ItemStack(ModItems.HEATER_MEDIUM.get()), ItemStack.EMPTY));
                cab.items().setStackInSlot(CabinetBlockEntity.FILTER_SLOT, f);
                cab.storage().insertItem(0, new ItemStack(ModItems.BETTA_PELLETS.get(), 24), false);
                cab.setChanged();
            }
        }
        for (int x = 0; x < 2; x++) {
            BlockPos cp = at.offset(x, 0, 0);
            level.setBlockAndUpdate(cp, Block.updateFromNeighbourShapes(level.getBlockState(cp), level, cp));
        }
        TankBlockEntity cab = tank(level, at.above(), 2, 2, 2, 0F, true);
        TankEquipment.invalidate(cab);
        sign(level, at, Component.translatable("block.bettacraft.cabinet"));
        at = at.east(2 + GAP);
        // internal filter, heaters and coolers in the interior
        TankBlockEntity d = tank(level, at, 2, 2, 2, 0F, true);
        segment(level, d, SegmentBlock.Kind.FILTER, null, 4, 26);
        sign(level, at, Component.translatable("item.bettacraft.internal_filter"));
        at = at.east(2 + GAP);
        TankBlockEntity e = tank(level, at, 3, 2, 2, 0F, true);
        segment(level, e, SegmentBlock.Kind.HEATER, SegmentBlock.Size.SMALL, 6, 28);
        segment(level, e, SegmentBlock.Kind.HEATER, SegmentBlock.Size.MEDIUM, 20, 28);
        segment(level, e, SegmentBlock.Kind.HEATER, SegmentBlock.Size.LARGE, 34, 28);
        sign(level, at, Component.literal("Heizstäbe"), Component.literal("klein, mittel, groß"));
        at = at.east(3 + GAP);
        TankBlockEntity f = tank(level, at, 3, 2, 2, 0F, true);
        segment(level, f, SegmentBlock.Kind.COOLER, SegmentBlock.Size.SMALL, 6, 28);
        segment(level, f, SegmentBlock.Kind.COOLER, SegmentBlock.Size.MEDIUM, 20, 28);
        segment(level, f, SegmentBlock.Kind.COOLER, SegmentBlock.Size.LARGE, 34, 28);
        sign(level, at, Component.literal("Kühler"), Component.literal("klein, mittel, groß"));
        at = at.east(3 + GAP);
        // lid, feeder and light on top
        TankBlockEntity g = tank(level, at, 3, 2, 2, 0F, true);
        for (int x = 0; x < 3; x++) level.setBlockAndUpdate(at.offset(x, 2, 1), ModBlocks.TANK_LID.get().defaultBlockState());
        level.setBlockAndUpdate(at.offset(0, 2, 0), ModBlocks.TANK_LIGHT.get().defaultBlockState());
        level.setBlockAndUpdate(at.offset(1, 2, 0), ModBlocks.FEEDER.get().defaultBlockState());
        TankEquipment.invalidate(g);
        sign(level, at, Component.translatable("block.bettacraft.tank_lid"), Component.translatable("block.bettacraft.feeder"));
        at = at.east(3 + GAP + 1);
        // protein skimmer and flow pump (salt water)
        TankBlockEntity h = tank(level, at, 2, 2, 2, 35F, true);
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
        sign(level, at, Component.translatable("block.bettacraft.protein_skimmer"), Component.translatable("item.bettacraft.flow_pump"));
    }

    private static void glassAndShapes(ServerLevel level, BlockPos at) {
        List<ResourceLocation> glass = new ArrayList<>();
        glass.add(ResourceLocation.withDefaultNamespace("glass"));
        for (DyeColor c : DyeColor.values()) glass.add(ResourceLocation.withDefaultNamespace(c.getName() + "_stained_glass"));
        glass.add(ResourceLocation.withDefaultNamespace("tinted_glass"));
        for (ResourceLocation g : glass) {
            tank(level, cuboid(at, 2, 1, 2), at, 0F, true, g, TankStyle.TEXTURED);
            sign(level, at, Component.translatable("block.minecraft." + g.getPath()));
            at = at.east(2 + GAP);
        }
        tank(level, cuboid(at, 2, 2, 2), at, 0F, true, ResourceLocation.withDefaultNamespace("glass"), TankStyle.CLEAN);
        sign(level, at, Component.literal("Stil: klar"), Component.literal("(clean)"));
        at = at.east(2 + GAP);
        tank(level, cuboid(at, 2, 2, 2), at, 0F, true, ResourceLocation.withDefaultNamespace("glass"), TankStyle.TEXTURED);
        sign(level, at, Component.literal("Stil: texturiert"), Component.literal("(textured)"));
        at = at.east(2 + GAP);
        // free shapes: an L, a T, a step
        List<BlockPos> l = new ArrayList<>();
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 3; x++) l.add(at.offset(x, y, 0));
            for (int z = 1; z < 3; z++) l.add(at.offset(0, y, z));
        }
        tank(level, l, at, 0F, true, null, null);
        sign(level, at, Component.literal("Form: L"));
        at = at.east(3 + GAP);
        List<BlockPos> t = new ArrayList<>();
        for (int x = 0; x < 3; x++) t.add(at.offset(x, 0, 0));
        for (int z = 1; z < 3; z++) t.add(at.offset(1, 0, z));
        tank(level, t, at, 0F, true, null, null);
        sign(level, at, Component.literal("Form: T"));
        at = at.east(3 + GAP);
        List<BlockPos> s = new ArrayList<>(cuboid(at, 3, 1, 2));
        s.add(at.offset(0, 1, 0));
        s.add(at.offset(0, 1, 1));
        tank(level, s, at, 0F, true, null, null);
        sign(level, at, Component.literal("Form: Stufe"));
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
        sign(level, at, Component.literal("Riffbecken"), Component.literal("35 ‰"));
    }
}
