package org.overengineer.bettacraft.client;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.overengineer.bettacraft.client.screen.TankScreen;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.equipment.FeederBlockEntity;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.SignPosition;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorSync;
import org.overengineer.bettacraft.tank.interior.InteriorWorldOps;
import org.overengineer.bettacraft.tank.menu.TankMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development-only visual smoke test ({@code -Dbettacraft.smoketest.world=true}, {@code gradlew runClient -Psmoketestworld}):
 * creates a flat creative world, builds a decorated tank with bettas, light bar, feeder and name sign, takes
 * screenshots of the tank and of the tank menu (run/screenshots) and quits. Never active in normal play.
 */
final class DevClientSmokeTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static int ticks;
    private static boolean created;
    private static BlockPos tankMin;
    private static BlockPos showMin;
    private static BlockPos breedMin;
    private static BlockPos bigMin;
    private static volatile InteriorRegion bigRegion;
    private static boolean bigDecorated;
    private static DevClientSmokeM6 m6;
    private static DevClientSmokeM7 m7;
    private static DevClientSmokeM8 m8;
    private static DevClientSmokeM9 m9;
    private static DevClientSmokeM10 m10;
    private static DevClientSmokeM11 m11;
    private static DevClientSmokeM12 m12;
    private static DevClientSmokeM13 m13;
    private static DevClientSmokeM14 m14;
    private static DevClientSmokeM15 m15;
    private static DevClientSmokeM16 m16;
    private static DevClientSmokeM18 m18;
    private static DevClientSmokeM19 m19;
    private static DevClientSmokeM20 m20;
    private static DevClientSmokeM21 m21;
    private static DevClientSmokeM211 m211;
    private static DevClientSmokeM215 m215;
    private static DevClientSmokeM216 m216;
    private static DevClientSmokeM217 m217;
    private static DevClientSmokeM218 m218;
    private static DevClientSmokeM221 m221;
    private static DevClientSmokeM222 m222;
    private static DevClientSmokeM223 m223;
    private static DevClientSmokeM224 m224;
    /** -Psmoketestm23: only the M23 scene (aeration, internal filters, light tiers, timer, menus, handbook). */
    private static final boolean M23_ONLY = Boolean.getBoolean("bettacraft.smoketest.m23only");
    private static DevClientSmokeM23 m23;
    /** -Psmoketestm24: only the M24 scene (pipes, multiblock filter, interior pipes, crucible, water devices, menus, handbook). */
    private static final boolean M24_ONLY = Boolean.getBoolean("bettacraft.smoketest.m24only");
    /** -Psmoketestm25: only the M25 part b scene (CO2 cylinder, bio CO2, filling station, reverse osmosis unit, regulator, handbook). */
    private static final boolean M25_ONLY = Boolean.getBoolean("bettacraft.smoketest.m25only");
    private static final boolean M25C_ONLY = Boolean.getBoolean("bettacraft.smoketest.m25conly");
    /** -Psmoketestreadme: only the README media scene (room with three tanks, frame sequences, menus). */
    private static final boolean README_ONLY = Boolean.getBoolean("bettacraft.smoketest.readmeonly");
    private static DevClientSmokeReadme readme;
    private static DevClientSmokeM25c m25c;
    private static final boolean M26_ONLY = Boolean.getBoolean("bettacraft.smoketest.m26only");
    /** -Psmoketestm26_1: only the M26.1 scene (the mod list with the wide logo). */
    private static final boolean M26_1_ONLY = Boolean.getBoolean("bettacraft.smoketest.m26_1only");
    private static DevClientSmokeM261 m26_1;
    private static DevClientSmokeM26 m26;
    /** -Psmoketestm28: only the M28 part a scene (the new plants inside and outside, white Bucephalandras by light). */
    private static final boolean M28_ONLY = Boolean.getBoolean("bettacraft.smoketest.m28only");
    private static DevClientSmokeM28 m28;
    /** -Psmoketestm28b: only the M28 part b scene (carpets, mosses, tubers, the Marimo, the corals v1/v3, floating plants). */
    private static final boolean M28B_ONLY = Boolean.getBoolean("bettacraft.smoketest.m28bonly");
    private static DevClientSmokeM28b m28b;
    /** -Psmoketestm28c: only the M28 part c scene (the new decorations, the leaf litter). */
    private static final boolean M28C_ONLY = Boolean.getBoolean("bettacraft.smoketest.m28conly");
    private static final boolean M281_ONLY = Boolean.getBoolean("bettacraft.smoketest.m281only");
    /** -Psmoketestm29: only the M29 part a scene (the cichlids). */
    private static final boolean M29_ONLY = Boolean.getBoolean("bettacraft.smoketest.m29only");
    private static DevClientSmokeM29 m29;
    /** -Psmoketestm291: only the M29.1 scene (every block item of the mod in a grid). */
    private static final boolean M291_ONLY = Boolean.getBoolean("bettacraft.smoketest.m291only");
    private static DevClientSmokeM291 m291;
    /** -Psmoketestm291b: plants, sound test, air pump in the cabinet, multiblock filter under a tank. */
    private static DevClientSmokeM291b m291b;
    /** -Psmoketestm30a: big tanks, dolphins, the pangasius, the transport tub. */
    private static final boolean M30A_ONLY = Boolean.getBoolean("bettacraft.smoketest.m30aonly");
    private static DevClientSmokeM30a m30a;
    /** -Psmoketestm30b: the freshwater giants (arapaima, redtail catfish, wels, motoro stingrays). */
    private static final boolean M30B_ONLY = Boolean.getBoolean("bettacraft.smoketest.m30bonly");
    private static DevClientSmokeM30b m30b;
    /** -Psmoketestm31c: dart frogs in a paludarium, geckos (one on the glass), corn snakes, horned frogs (one buried). */
    private static final boolean M31C_ONLY = Boolean.getBoolean("bettacraft.smoketest.m31conly");
    private static DevClientSmokeM31c m31c;
    /** -Psmoketestm31d: mobs in glass (drowned in an aquarium, a spider on the glass, slimes), the mob jar. */
    private static final boolean M31D_ONLY = Boolean.getBoolean("bettacraft.smoketest.m31donly");
    /** -Pcreate -Psmoketestm32b: a tank travelling on a Create bearing and on a Sable airship (M32 part b). */
    private static final boolean M32B_ONLY = Boolean.getBoolean("bettacraft.smoketest.m32bonly");
    private static DevClientSmokeM32b m32b;
    /** -Pcreate -Psmoketestm32c: a Display Link writes tank values onto a sign (M32 part c). */
    private static final boolean M32C_ONLY = Boolean.getBoolean("bettacraft.smoketest.m32conly");
    /** -Pmaps -Psmoketestm32d: a monitor alarm puts a JourneyMap waypoint at its tank (M32 part d). */
    private static final boolean M32D_ONLY = Boolean.getBoolean("bettacraft.smoketest.m32donly");
    private static DevClientSmokeM32d m32d;
    /** -Psmoketestm33b: the creative tab and the handbook with the tankmates and salt water off (M33 part b). */
    private static final boolean M33B_ONLY = Boolean.getBoolean("bettacraft.smoketest.m33bonly");
    /** -Psmoketestm331: the tank type switch in the climate tab and the overview (M33.1). */
    private static final boolean M331_ONLY = Boolean.getBoolean("bettacraft.smoketest.m331only");
    private static DevClientSmokeM331 m331;
    /** -Psmoketestm35a: alligators, tortoises and sea turtles in their big terraria and paludaria (M35 part a). */
    private static final boolean M35A_ONLY = Boolean.getBoolean("bettacraft.smoketest.m35aonly");
    private static DevClientSmokeM35a m35a;
    /** -Psmoketestm36: the size setting of the bettas (Normal / Large), with -Pcompare next to one of DragN's bettas (M36). */
    private static final boolean M36_ONLY = Boolean.getBoolean("bettacraft.smoketest.m36only");
    private static DevClientSmokeM36 m36;
    /** -Psmoketestm38: plants and corals of Minecraft and other mods in two tanks, swaying, the plants tab (M38). */
    private static final boolean M38_ONLY = Boolean.getBoolean("bettacraft.smoketest.m38only");
    private static DevClientSmokeM38 m38;
    /** -Psmoketestm37: the fertilizer maker, the crank and the cabinet's spring, the water tab (M37). */
    private static final boolean M37_ONLY = Boolean.getBoolean("bettacraft.smoketest.m37only");
    /** -Pintegration -Psmoketestm42: JEI recipe screens and the handbook with every integrated mod loaded (M42). */
    private static final boolean M42_ONLY = Boolean.getBoolean("bettacraft.smoketest.m42only");
    private static DevClientSmokeM42 m42;
    /** -Pzoo -Psmoketestm42b: the miniature with animals and plants of mods we do not integrate (M42 part b). */
    private static final boolean M42B_ONLY = Boolean.getBoolean("bettacraft.smoketest.m42bonly");
    private static DevClientSmokeM42b m42b;
    /** -Psmoketestm46: the heat mat icon, the wild carpets, the herps from all sides, the critter spawn pass (M46). */
    private static final boolean M46_ONLY = Boolean.getBoolean("bettacraft.smoketest.m46only");
    private static DevClientSmokeM46 m46;
    /** -Psmoketestm47: the aquarist villager in a village and his trade menu, the ram forms, the frog breeding (M47). */
    private static final boolean M47_ONLY = Boolean.getBoolean("bettacraft.smoketest.m47only");
    private static final boolean M48_ONLY = Boolean.getBoolean("bettacraft.smoketest.m48only");
    private static final boolean M49_ONLY = Boolean.getBoolean("bettacraft.smoketest.m49only");
    private static DevClientSmokeM49 m49;
    /** -Pcreate -Psmoketestm50: tanks on a turning bearing, a minecart contraption in a curve and an airship (M50). */
    private static final boolean M50_ONLY = Boolean.getBoolean("bettacraft.smoketest.m50only");
    /** -Psmoketestm54: the movement of the animals (M54). */
    private static final boolean M54_ONLY = Boolean.getBoolean("bettacraft.smoketest.m54only");
    private static DevClientSmokeM54 m54;
    /** -Psmoketestm53: users tab, gear, map mods, interior brightness, every animal from six sides (M53). */
    private static final boolean M53_ONLY = Boolean.getBoolean("bettacraft.smoketest.m53only");
    private static DevClientSmokeM53 m53;
    /** -Psmoketestm51: the hired aquarist comes, is hired, works; his menu, the tank menu button, his looks, asleep (M51). */
    private static final boolean M51_ONLY = Boolean.getBoolean("bettacraft.smoketest.m51only");
    private static DevClientSmokeM51 m51;
    private static DevClientSmokeM50 m50;
    private static DevClientSmokeM47 m47;
    private static DevClientSmokeM48 m48;
    /** -Pquests -Pkubejs -Psmoketestm45: the quest chapter in the quest book, English and German (M45). */
    private static final boolean M45_ONLY = Boolean.getBoolean("bettacraft.smoketest.m45only");
    private static DevClientSmokeM45 m45;
    /** -Psmoketestm44: the walk animation in tanks and the cabinet fronts (M44). */
    private static final boolean M44_ONLY = Boolean.getBoolean("bettacraft.smoketest.m44only");
    private static DevClientSmokeM44 m44;
    /** -Pcreate -Psmoketestm41: a tank powered by Create's rotation through a drive adapter and a cabinet shaft (M41). */
    private static final boolean M41_ONLY = Boolean.getBoolean("bettacraft.smoketest.m41only");
    private static DevClientSmokeM41 m41;
    private static DevClientSmokeM37 m37;
    /** -Psmoketestm39a: big fish of other mods in a big tank, sea animals of other mods, the Aquaculture turtles (M39 part a). */
    private static final boolean M39A_ONLY = Boolean.getBoolean("bettacraft.smoketest.m39aonly");
    private static DevClientSmokeM39a m39a;
    /** -Psmoketestm39b: insects, mammals and birds of other mods in terraria, the komodo dragon, fireflies at night (M39 part b). */
    private static final boolean M39B_ONLY = Boolean.getBoolean("bettacraft.smoketest.m39bonly");
    private static DevClientSmokeM39b m39b;
    /** -Psmoketestm40a: one multiblock filter for three tanks, the pipe docks and guards inside (M40 part a). */
    private static final boolean M40A_ONLY = Boolean.getBoolean("bettacraft.smoketest.m40aonly");
    private static DevClientSmokeM40a m40a;
    /** -Psmoketestm40b: a row of pipe bundles with all four lines, the filter with its fresh and waste water ports (M40 part b). */
    private static final boolean M40B_ONLY = Boolean.getBoolean("bettacraft.smoketest.m40bonly");
    private static DevClientSmokeM40b m40b;
    /** -Psmoketestm40c: the bundles of part b under our covers and the facades of other mods (M40 part c). */
    private static final boolean M40C_ONLY = Boolean.getBoolean("bettacraft.smoketest.m40conly");
    private static DevClientSmokeM40c m40c;
    /** -Psmoketestm39c: the peaceful animals of Minecraft as terrarium pets (M39 part c, optional feature #25). */
    private static final boolean M39C_ONLY = Boolean.getBoolean("bettacraft.smoketest.m39conly");
    private static DevClientSmokeM39c m39c;
    /** -Psmoketestm35b: lizard, snake and scorpions in a terrarium, fish in a paludarium's water part with a crab, dragonflies (M35 part b). */
    private static final boolean M35B_ONLY = Boolean.getBoolean("bettacraft.smoketest.m35bonly");
    private static DevClientSmokeM35b m35b;
    private static DevClientSmokeM33b m33b;
    private static DevClientSmokeM31d m31d;
    /** -Psmoketestm31b: the turtles, a clutch of eggs and hatchlings, Hermann's tortoises, the climate tab with the eggs. */
    private static final boolean M31B_ONLY = Boolean.getBoolean("bettacraft.smoketest.m31bonly");
    private static DevClientSmokeM31b m31b;
    /** -Psmoketestm31a: terrarium and paludarium with the devices and two musk turtles, the climate tab. */
    private static final boolean M31A_ONLY = Boolean.getBoolean("bettacraft.smoketest.m31aonly");
    private static DevClientSmokeM31a m31a;
    /** -Psmoketestm30c: the sea giants (sharks, rays, moray) and the bamboo shark's egg cases. */
    private static final boolean M30C_ONLY = Boolean.getBoolean("bettacraft.smoketest.m30conly");
    private static DevClientSmokeM30c m30c;
    /** -Psmoketestm29d: the big crayfish and the axolotls. */
    private static DevClientSmokeM29d m29d;
    private static DevClientSmokeM281 m281;
    private static DevClientSmokeM28c m28c;
    /** -Psmoketestm27: only the M27 scene (monitor screens). */
    private static final boolean M27_ONLY = Boolean.getBoolean("bettacraft.smoketest.m27only");
    private static DevClientSmokeM27 m27;
    private static DevClientSmokeM25 m25;
    private static DevClientSmokeM24 m24;
    /** -Psmoketestm22_4: only the M22.4 scene (the four outlet sides of the cabinet filter, the cabinet menu, the handbook plant sources). */
    private static final boolean M22_4_ONLY = Boolean.getBoolean("bettacraft.smoketest.m22_4only");
    /** -Psmoketestm22_3: only the M22.3 scene (wild fish and small animals, wild corals, magnifier, single animals, bucket tooltips). */
    private static final boolean M22_3_ONLY = Boolean.getBoolean("bettacraft.smoketest.m22_3only");
    /** -Psmoketestm22_2: only the M22.2 scene (species gallery for the handbook, the handbook with its navigation en + de). */
    private static final boolean M22_2_ONLY = Boolean.getBoolean("bettacraft.smoketest.m22_2only");
    /** -Psmoketestm22_1: only the M22.1 scene (every plant and coral with its shape, swaying water plants, emptying containers). */
    private static final boolean M22_1_ONLY = Boolean.getBoolean("bettacraft.smoketest.m22_1only");
    /** -Psmoketestm21_8: only the M21.8 scene (seahorses, cyst stacks, clear glass icon). */
    private static final boolean M21_8_ONLY = Boolean.getBoolean("bettacraft.smoketest.m21_8only");
    /** -Psmoketestm21_7: only the M21.7 scene (stone cabinets). */
    private static final boolean M21_7_ONLY = Boolean.getBoolean("bettacraft.smoketest.m21_7only");
    /** -Psmoketestm21_6: only the M21.6 scene (guppy and endler genetics). */
    private static final boolean M21_6_ONLY = Boolean.getBoolean("bettacraft.smoketest.m21_6only");
    /** -Psmoketestm21_5: only the M21.5 scene (smooth swimming, colours, cabinets, decor, clip-on feeder, protection, handbook). */
    private static final boolean M21_5_ONLY = Boolean.getBoolean("bettacraft.smoketest.m21_5only");
    /** -Psmoketestm21_1: only the M21.1 scene (plant meshes outside, Rotala stems, Vallisneria ribbons, lily pads). */
    private static final boolean M21_1_ONLY = Boolean.getBoolean("bettacraft.smoketest.m21_1only");
    /** -Psmoketestm21: only the M21 scene (plant prototype old/new, new fish buckets). */
    private static final boolean M21_ONLY = Boolean.getBoolean("bettacraft.smoketest.m21only");
    /** -Psmoketestm20: only the M20 scene (reef tank, corals, reef animals, water colours, menus, items). */
    private static final boolean M20_ONLY = Boolean.getBoolean("bettacraft.smoketest.m20only");
    /** -Psmoketestm19: only the M19 scene (tankmates, their breeding, menus, items). */
    private static final boolean M19_ONLY = Boolean.getBoolean("bettacraft.smoketest.m19only");
    /** -Psmoketestm18: only the M18 scene (coloured lights, young bettas in the stocking). */
    private static final boolean M18_ONLY = Boolean.getBoolean("bettacraft.smoketest.m18only");
    /** -Psmoketestm16: only the M16 scene (water animals of other mods, refusals, no tank inside a tank). */
    private static final boolean M16_ONLY = Boolean.getBoolean("bettacraft.smoketest.m16only");
    /** -Psmoketestm15: only the M15 scene (name sign, domed nest, interior leave hint). */
    private static final boolean M15_ONLY = Boolean.getBoolean("bettacraft.smoketest.m15only");
    /** -Psmoketestm14: only the M14 scene (shrimp, crayfish, snails, frogs, algae). */
    private static final boolean M14_ONLY = Boolean.getBoolean("bettacraft.smoketest.m14only");
    /** -Psmoketestm13: only the M13 scene (aquarium plants). */
    private static final boolean M13_ONLY = Boolean.getBoolean("bettacraft.smoketest.m13only");
    /** -Psmoketestm12: only the M12 scene (floating plants). */
    private static final boolean M12_ONLY = Boolean.getBoolean("bettacraft.smoketest.m12only");
    /** -Psmoketestm11: only the M11 scene (tank styles, connected glass, edges, water seam, mirror stick). */
    private static final boolean M11_ONLY = Boolean.getBoolean("bettacraft.smoketest.m11only");
    /** -Psmoketestm10: only the M10 scene (free tank shapes). */
    private static final boolean M10_ONLY = Boolean.getBoolean("bettacraft.smoketest.m10only");
    /** -Psmoketestm9: only the M9 scene (water care, coolers, breeders). */
    private static final boolean M9_ONLY = Boolean.getBoolean("bettacraft.smoketest.m9only");
    /** -Psmoketestm7: only the M7 compat scene (faster when iterating on it). */
    private static final boolean M7_ONLY = Boolean.getBoolean("bettacraft.smoketest.m7only");
    /** -Psmoketestm8: only the M8 info overlay scene (Jade, The One Probe, WTHIT). */
    private static final boolean M8_ONLY = Boolean.getBoolean("bettacraft.smoketest.m8only");

    private DevClientSmokeTest() {}

    static void register() {
        org.overengineer.bettacraft.platform.client.ClientEvents.SCREEN_OPENING.add(screen -> {
            if (!created && screen instanceof TitleScreen) {
                created = true;
                Minecraft mc = Minecraft.getInstance();
                // M52c (26.1): the game rules are saved data of the world; set in tick() once the server is up
                LevelSettings settings = new LevelSettings("bettacraft_smoketest_" + System.currentTimeMillis(), GameType.CREATIVE,
                        new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
                // M46: -Psmoketestm46 needs real biomes (rivers, swamps, jungles): a normal world of the same seed
                var preset = Boolean.getBoolean("bettacraft.smoketest.normalworld") ? WorldPresets.NORMAL : WorldPresets.FLAT;
                mc.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings, new WorldOptions(42L, Boolean.getBoolean("bettacraft.smoketest.structures"), false),
                        ra -> ra.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(preset).value().createWorldDimensions(),
                        screen);
            }
        });
        org.overengineer.bettacraft.platform.client.ClientEvents.TICK_POST.add(DevClientSmokeTest::tick);
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getSingleplayerServer() == null) return;
        ticks++;
        if (ticks == 1) {
            var server = mc.getSingleplayerServer();
            server.execute(() -> {
                server.getGameRules().set(GameRules.ADVANCE_TIME, false, server);
                server.getGameRules().set(GameRules.SPAWN_MOBS, false, server);
            });
        }
        mc.options.pauseOnLostFocus = false;
        if (ticks == 2) mc.getWindow().setWindowed(1280, 960); // larger screenshots (handbook images, menus at GUI scale 4)
        if (ticks > 60 && m8 == null) mc.player.getAbilities().flying = true; // hover at the camera positions
        if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.PauseScreen) mc.gui.setScreen(null);
        try {
            if (README_ONLY) {
                if (ticks == 60) readme = new DevClientSmokeReadme();
                if (readme != null && readme.tick()) {
                    readme = null;
                    LOGGER.info("[smoketest] client world test finished (README media only)");
                    mc.stop();
                }
                return;
            }
            if (M38_ONLY) {
                if (ticks == 60) m38 = new DevClientSmokeM38();
                if (m38 != null && m38.tick()) {
                    m38 = null;
                    LOGGER.info("[smoketest] client world test finished (M38 only)");
                    mc.stop();
                }
                return;
            }
            if (M37_ONLY) {
                if (ticks == 60) m37 = new DevClientSmokeM37();
                if (m37 != null && m37.tick()) {
                    m37 = null;
                    LOGGER.info("[smoketest] client world test finished (M37 only)");
                    mc.stop();
                }
                return;
            }
            if (M36_ONLY) {
                if (ticks == 60) m36 = new DevClientSmokeM36();
                if (m36 != null && m36.tick()) {
                    m36 = null;
                    LOGGER.info("[smoketest] client world test finished (M36 only)");
                    mc.stop();
                }
                return;
            }
            if (M40C_ONLY) {
                if (ticks == 60) m40c = new DevClientSmokeM40c();
                if (m40c != null && m40c.tick()) {
                    m40c = null;
                    LOGGER.info("[smoketest] client world test finished (M40 part c only)");
                    mc.stop();
                }
                return;
            }
            if (M40B_ONLY) {
                if (ticks == 60) m40b = new DevClientSmokeM40b();
                if (m40b != null && m40b.tick()) {
                    m40b = null;
                    LOGGER.info("[smoketest] client world test finished (M40 part b only)");
                    mc.stop();
                }
                return;
            }
            if (M40A_ONLY) {
                if (ticks == 60) m40a = new DevClientSmokeM40a();
                if (m40a != null && m40a.tick()) {
                    m40a = null;
                    LOGGER.info("[smoketest] client world test finished (M40 part a only)");
                    mc.stop();
                }
                return;
            }
            if (M39C_ONLY) {
                if (ticks == 60) m39c = new DevClientSmokeM39c();
                if (m39c != null && m39c.tick()) {
                    m39c = null;
                    LOGGER.info("[smoketest] client world test finished (M39 part c only)");
                    mc.stop();
                }
                return;
            }
            if (M39B_ONLY) {
                if (ticks == 60) m39b = new DevClientSmokeM39b();
                if (m39b != null && m39b.tick()) {
                    m39b = null;
                    LOGGER.info("[smoketest] client world test finished (M39 part b only)");
                    mc.stop();
                }
                return;
            }
            if (M39A_ONLY) {
                if (ticks == 60) m39a = new DevClientSmokeM39a();
                if (m39a != null && m39a.tick()) {
                    m39a = null;
                    LOGGER.info("[smoketest] client world test finished (M39 part a only)");
                    mc.stop();
                }
                return;
            }
            if (M35B_ONLY) {
                if (ticks == 60) m35b = new DevClientSmokeM35b();
                if (m35b != null && m35b.tick()) {
                    m35b = null;
                    LOGGER.info("[smoketest] client world test finished (M35 part b only)");
                    mc.stop();
                }
                return;
            }
            if (M35A_ONLY) {
                if (ticks == 60) m35a = new DevClientSmokeM35a();
                if (m35a != null && m35a.tick()) {
                    m35a = null;
                    LOGGER.info("[smoketest] client world test finished (M35 part a only)");
                    mc.stop();
                }
                return;
            }
            if (M331_ONLY) {
                if (ticks == 60) m331 = new DevClientSmokeM331();
                if (m331 != null && m331.tick()) {
                    m331 = null;
                    LOGGER.info("[smoketest] client world test finished (M33.1 only)");
                    mc.stop();
                }
                return;
            }
            if (M33B_ONLY) {
                if (ticks == 60) m33b = new DevClientSmokeM33b();
                if (m33b != null && m33b.tick()) {
                    m33b = null;
                    LOGGER.info("[smoketest] client world test finished (M33 part b only)");
                    mc.stop();
                }
                return;
            }
            if (M32D_ONLY) {
                if (ticks == 60) m32d = new DevClientSmokeM32d();
                if (m32d != null && m32d.tick()) {
                    m32d = null;
                    LOGGER.info("[smoketest] client world test finished (M32 part d only)");
                    mc.stop();
                }
                return;
            }
            if (M54_ONLY) {
                if (ticks == 60) m54 = new DevClientSmokeM54();
                if (m54 != null && m54.tick()) {
                    m54 = null;
                    LOGGER.info("[smoketest] client world test finished (M54 only)");
                    mc.stop();
                }
                return;
            }
            if (M53_ONLY) {
                if (ticks == 60) m53 = new DevClientSmokeM53();
                if (m53 != null && m53.tick()) {
                    m53 = null;
                    LOGGER.info("[smoketest] client world test finished (M53 only)");
                    mc.stop();
                }
                return;
            }
            if (M51_ONLY) {
                if (ticks == 60) m51 = new DevClientSmokeM51();
                if (m51 != null && m51.tick()) {
                    m51 = null;
                    LOGGER.info("[smoketest] client world test finished (M51 only)");
                    mc.stop();
                }
                return;
            }
            if (M50_ONLY) {
                if (ticks == 60) m50 = new DevClientSmokeM50();
                if (m50 != null && m50.tick()) {
                    m50 = null;
                    LOGGER.info("[smoketest] client world test finished (M50 only)");
                    mc.stop();
                }
                return;
            }
            if (M49_ONLY) {
                if (ticks == 60) m49 = new DevClientSmokeM49();
                if (m49 != null && m49.tick()) {
                    m49 = null;
                    LOGGER.info("[smoketest] client world test finished (M49 only)");
                    mc.stop();
                }
                return;
            }
            if (M48_ONLY) {
                if (ticks == 60) m48 = new DevClientSmokeM48();
                if (m48 != null && m48.tick()) {
                    m48 = null;
                    LOGGER.info("[smoketest] client world test finished (M48 only)");
                    mc.stop();
                }
                return;
            }
            if (M47_ONLY) {
                if (ticks == 60) m47 = new DevClientSmokeM47();
                if (m47 != null && m47.tick()) {
                    m47 = null;
                    LOGGER.info("[smoketest] client world test finished (M47 only)");
                    mc.stop();
                }
                return;
            }
            if (M46_ONLY) {
                if (ticks == 60) m46 = new DevClientSmokeM46();
                if (m46 != null && m46.tick()) {
                    m46 = null;
                    LOGGER.info("[smoketest] client world test finished (M46 only)");
                    mc.stop();
                }
                return;
            }
            if (M45_ONLY) {
                if (ticks == 60) m45 = new DevClientSmokeM45();
                if (m45 != null && m45.tick()) {
                    m45 = null;
                    LOGGER.info("[smoketest] client world test finished (M45 only)");
                    mc.stop();
                }
                return;
            }
            if (M44_ONLY) {
                if (ticks == 60) m44 = new DevClientSmokeM44();
                if (m44 != null && m44.tick()) {
                    m44 = null;
                    LOGGER.info("[smoketest] client world test finished (M44 only)");
                    mc.stop();
                }
                return;
            }
            if (M42B_ONLY) {
                if (ticks == 60) m42b = new DevClientSmokeM42b();
                if (m42b != null && m42b.tick()) {
                    m42b = null;
                    LOGGER.info("[smoketest] client world test finished (M42 part b only)");
                    mc.stop();
                }
                return;
            }
            if (M42_ONLY) {
                if (ticks == 60) m42 = new DevClientSmokeM42();
                if (m42 != null && m42.tick()) {
                    m42 = null;
                    LOGGER.info("[smoketest] client world test finished (M42 only)");
                    mc.stop();
                }
                return;
            }
            if (M41_ONLY) {
                if (ticks == 60) m41 = new DevClientSmokeM41();
                if (m41 != null && m41.tick()) {
                    m41 = null;
                    LOGGER.info("[smoketest] client world test finished (M41 only)");
                    mc.stop();
                }
                return;
            }
            if (M32C_ONLY) {
                // M52b: Create (Display Link) has no 1.21.11 build; the scene is not part of this branch
                LOGGER.info("[smoketest] client world test finished (M32 part c only: no Create on this version)");
                mc.stop();
                return;
            }
            if (M32B_ONLY) {
                if (ticks == 60) m32b = new DevClientSmokeM32b();
                if (m32b != null && m32b.tick()) {
                    m32b = null;
                    LOGGER.info("[smoketest] client world test finished (M32 part b only)");
                    mc.stop();
                }
                return;
            }
            if (M31D_ONLY) {
                if (ticks == 60) m31d = new DevClientSmokeM31d();
                if (m31d != null && m31d.tick()) {
                    m31d = null;
                    LOGGER.info("[smoketest] client world test finished (M31 part d only)");
                    mc.stop();
                }
                return;
            }
            if (M31C_ONLY) {
                if (ticks == 60) m31c = new DevClientSmokeM31c();
                if (m31c != null && m31c.tick()) {
                    m31c = null;
                    LOGGER.info("[smoketest] client world test finished (M31 part c only)");
                    mc.stop();
                }
                return;
            }
            if (M31B_ONLY) {
                if (ticks == 60) m31b = new DevClientSmokeM31b();
                if (m31b != null && m31b.tick()) {
                    m31b = null;
                    LOGGER.info("[smoketest] client world test finished (M31 part b only)");
                    mc.stop();
                }
                return;
            }
            if (M31A_ONLY) {
                if (ticks == 60) m31a = new DevClientSmokeM31a();
                if (m31a != null && m31a.tick()) {
                    m31a = null;
                    LOGGER.info("[smoketest] client world test finished (M31 part a only)");
                    mc.stop();
                }
                return;
            }
            if (M30C_ONLY) {
                if (ticks == 60) m30c = new DevClientSmokeM30c();
                if (m30c != null && m30c.tick()) {
                    m30c = null;
                    LOGGER.info("[smoketest] client world test finished (M30 part c only)");
                    mc.stop();
                }
                return;
            }
            if (M30B_ONLY) {
                if (ticks == 60) m30b = new DevClientSmokeM30b();
                if (m30b != null && m30b.tick()) {
                    m30b = null;
                    LOGGER.info("[smoketest] client world test finished (M30 part b only)");
                    mc.stop();
                }
                return;
            }
            if (M30A_ONLY) {
                if (ticks == 60) m30a = new DevClientSmokeM30a();
                if (m30a != null && m30a.tick()) {
                    m30a = null;
                    LOGGER.info("[smoketest] client world test finished (M30 part a only)");
                    mc.stop();
                }
                return;
            }
            if (M291_ONLY && Boolean.getBoolean("bettacraft.smoketest.m291b")) {
                if (ticks == 60) m291b = new DevClientSmokeM291b();
                if (m291b != null && m291b.tick()) {
                    m291b = null;
                    LOGGER.info("[smoketest] client world test finished (M29.1 part b only)");
                    mc.stop();
                }
                return;
            }
            if (M291_ONLY) {
                if (ticks == 60) m291 = new DevClientSmokeM291();
                if (m291 != null && m291.tick()) {
                    m291 = null;
                    LOGGER.info("[smoketest] client world test finished (M29.1 only)");
                    mc.stop();
                }
                return;
            }
            if (M29_ONLY && Boolean.getBoolean("bettacraft.smoketest.m29d")) {
                if (ticks == 60) m29d = new DevClientSmokeM29d();
                if (m29d != null && m29d.tick()) {
                    m29d = null;
                    LOGGER.info("[smoketest] client world test finished (M29 part d only)");
                    mc.stop();
                }
                return;
            }
            if (M29_ONLY) {
                if (ticks == 60) m29 = new DevClientSmokeM29(Boolean.getBoolean("bettacraft.smoketest.m29b"), Boolean.getBoolean("bettacraft.smoketest.m29c"));
                if (m29 != null && m29.tick()) {
                    m29 = null;
                    LOGGER.info("[smoketest] client world test finished (M29 only)");
                    mc.stop();
                }
                return;
            }
            if (M281_ONLY) {
                if (ticks == 60) m281 = new DevClientSmokeM281();
                if (m281 != null && m281.tick()) {
                    m281 = null;
                    LOGGER.info("[smoketest] client world test finished (M28.1 only)");
                    mc.stop();
                }
                return;
            }
            if (M28C_ONLY) {
                if (ticks == 60) m28c = new DevClientSmokeM28c();
                if (m28c != null && m28c.tick()) {
                    m28c = null;
                    LOGGER.info("[smoketest] client world test finished (M28 part c only)");
                    mc.stop();
                }
                return;
            }
            if (M28B_ONLY) {
                if (ticks == 60) m28b = new DevClientSmokeM28b();
                if (m28b != null && m28b.tick()) {
                    m28b = null;
                    LOGGER.info("[smoketest] client world test finished (M28 part b only)");
                    mc.stop();
                }
                return;
            }
            if (M28_ONLY) {
                if (ticks == 60) m28 = new DevClientSmokeM28();
                if (m28 != null && m28.tick()) {
                    m28 = null;
                    LOGGER.info("[smoketest] client world test finished (M28 only)");
                    mc.stop();
                }
                return;
            }
            if (M27_ONLY) {
                if (ticks == 60) m27 = new DevClientSmokeM27();
                if (m27 != null && m27.tick()) {
                    m27 = null;
                    LOGGER.info("[smoketest] client world test finished (M27 only)");
                    mc.stop();
                }
                return;
            }
            if (M26_1_ONLY) {
                if (ticks == 60) m26_1 = new DevClientSmokeM261();
                if (m26_1 != null && m26_1.tick()) {
                    m26_1 = null;
                    LOGGER.info("[smoketest] client world test finished (M26.1 only)");
                    mc.stop();
                }
                return;
            }
            if (M26_ONLY) {
                if (ticks == 60) m26 = new DevClientSmokeM26();
                if (m26 != null && m26.tick()) {
                    m26 = null;
                    LOGGER.info("[smoketest] client world test finished (M26 only)");
                    mc.stop();
                }
                return;
            }
            if (M25C_ONLY) {
                if (ticks == 60) m25c = new DevClientSmokeM25c();
                if (m25c != null && m25c.tick()) {
                    m25c = null;
                    LOGGER.info("[smoketest] client world test finished (M25 part c only)");
                    mc.stop();
                }
                return;
            }
            if (M25_ONLY) {
                if (ticks == 60) m25 = new DevClientSmokeM25();
                if (m25 != null && m25.tick()) {
                    m25 = null;
                    LOGGER.info("[smoketest] client world test finished (M25 only)");
                    mc.stop();
                }
                return;
            }
            if (M24_ONLY) {
                if (ticks == 60) m24 = new DevClientSmokeM24();
                if (m24 != null && m24.tick()) {
                    m24 = null;
                    LOGGER.info("[smoketest] client world test finished (M24 only)");
                    mc.stop();
                }
                return;
            }
            if (M23_ONLY) {
                if (ticks == 60) m23 = new DevClientSmokeM23();
                if (m23 != null && m23.tick()) {
                    m23 = null;
                    LOGGER.info("[smoketest] client world test finished (M23 only)");
                    mc.stop();
                }
                return;
            }
            if (M22_4_ONLY) {
                if (ticks == 60) m224 = new DevClientSmokeM224();
                if (m224 != null && m224.tick()) {
                    m224 = null;
                    LOGGER.info("[smoketest] client world test finished (M22.4 only)");
                    mc.stop();
                }
                return;
            }
            if (M22_3_ONLY) {
                if (ticks == 60) m223 = new DevClientSmokeM223();
                if (m223 != null && m223.tick()) {
                    m223 = null;
                    LOGGER.info("[smoketest] client world test finished (M22.3 only)");
                    mc.stop();
                }
                return;
            }
            if (M22_2_ONLY) {
                if (ticks == 60) m222 = new DevClientSmokeM222();
                if (m222 != null && m222.tick()) {
                    m222 = null;
                    LOGGER.info("[smoketest] client world test finished (M22.2 only)");
                    mc.stop();
                }
                return;
            }
            if (M22_1_ONLY) {
                if (ticks == 60) m221 = new DevClientSmokeM221();
                if (m221 != null && m221.tick()) {
                    m221 = null;
                    LOGGER.info("[smoketest] client world test finished (M22.1 only)");
                    mc.stop();
                }
                return;
            }
            if (M21_8_ONLY) {
                if (ticks == 60) m218 = new DevClientSmokeM218();
                if (m218 != null && m218.tick()) {
                    m218 = null;
                    LOGGER.info("[smoketest] client world test finished (M21.8 only)");
                    mc.stop();
                }
                return;
            }
            if (M21_7_ONLY) {
                if (ticks == 60) m217 = new DevClientSmokeM217();
                if (m217 != null && m217.tick()) {
                    m217 = null;
                    LOGGER.info("[smoketest] client world test finished (M21.7 only)");
                    mc.stop();
                }
                return;
            }
            if (M21_6_ONLY) {
                if (ticks == 60) m216 = new DevClientSmokeM216();
                if (m216 != null && m216.tick()) {
                    m216 = null;
                    LOGGER.info("[smoketest] client world test finished (M21.6 only)");
                    mc.stop();
                }
                return;
            }
            if (M21_5_ONLY) {
                if (ticks == 60) m215 = new DevClientSmokeM215();
                if (m215 != null && m215.tick()) {
                    m215 = null;
                    LOGGER.info("[smoketest] client world test finished (M21.5 only)");
                    mc.stop();
                }
                return;
            }
            if (M21_1_ONLY) {
                if (ticks == 60) m211 = new DevClientSmokeM211();
                if (m211 != null && m211.tick()) {
                    m211 = null;
                    LOGGER.info("[smoketest] client world test finished (M21.1 only)");
                    mc.stop();
                }
                return;
            }
            if (M21_ONLY) {
                if (ticks == 60) m21 = new DevClientSmokeM21();
                if (m21 != null && m21.tick()) {
                    m21 = null;
                    LOGGER.info("[smoketest] client world test finished (M21 only)");
                    mc.stop();
                }
                return;
            }
            if (M20_ONLY) {
                if (ticks == 60) m20 = new DevClientSmokeM20();
                if (m20 != null && m20.tick()) {
                    m20 = null;
                    LOGGER.info("[smoketest] client world test finished (M20 only)");
                    mc.stop();
                }
                return;
            }
            if (M19_ONLY) {
                if (ticks == 60) m19 = new DevClientSmokeM19();
                if (m19 != null && m19.tick()) {
                    m19 = null;
                    LOGGER.info("[smoketest] client world test finished (M19 only)");
                    mc.stop();
                }
                return;
            }
            if (M18_ONLY) {
                if (ticks == 60) m18 = new DevClientSmokeM18();
                if (m18 != null && m18.tick()) {
                    m18 = null;
                    LOGGER.info("[smoketest] client world test finished (M18 only)");
                    mc.stop();
                }
                return;
            }
            if (M16_ONLY) {
                if (ticks == 60) m16 = new DevClientSmokeM16();
                if (m16 != null && m16.tick()) {
                    m16 = null;
                    LOGGER.info("[smoketest] client world test finished (M16 only)");
                    mc.stop();
                }
                return;
            }
            if (M15_ONLY) {
                if (ticks == 60) m15 = new DevClientSmokeM15();
                if (m15 != null && m15.tick()) {
                    m15 = null;
                    LOGGER.info("[smoketest] client world test finished (M15 only)");
                    mc.stop();
                }
                return;
            }
            if (M14_ONLY) {
                if (ticks == 60) m14 = new DevClientSmokeM14();
                if (m14 != null && m14.tick()) {
                    m14 = null;
                    LOGGER.info("[smoketest] client world test finished (M14 only)");
                    mc.stop();
                }
                return;
            }
            if (M13_ONLY) {
                if (ticks == 60) m13 = new DevClientSmokeM13();
                if (m13 != null && m13.tick()) {
                    m13 = null;
                    LOGGER.info("[smoketest] client world test finished (M13 only)");
                    mc.stop();
                }
                return;
            }
            if (M12_ONLY) {
                if (ticks == 60) m12 = new DevClientSmokeM12();
                if (m12 != null && m12.tick()) {
                    m12 = null;
                    LOGGER.info("[smoketest] client world test finished (M12 only)");
                    mc.stop();
                }
                return;
            }
            if (M11_ONLY) {
                if (ticks == 60) m11 = new DevClientSmokeM11();
                if (m11 != null && m11.tick()) {
                    m11 = null;
                    LOGGER.info("[smoketest] client world test finished (M11 only)");
                    mc.stop();
                }
                return;
            }
            if (M10_ONLY) {
                if (ticks == 60) m10 = new DevClientSmokeM10();
                if (m10 != null && m10.tick()) {
                    m10 = null;
                    LOGGER.info("[smoketest] client world test finished (M10 only)");
                    mc.stop();
                }
                return;
            }
            if (M9_ONLY) {
                if (ticks == 60) m9 = new DevClientSmokeM9();
                if (m9 != null && m9.tick()) {
                    m9 = null;
                    LOGGER.info("[smoketest] client world test finished (M9 only)");
                    mc.stop();
                }
                return;
            }
            if (M8_ONLY) {
                if (ticks == 60) m8 = new DevClientSmokeM8();
                if (m8 != null && m8.tick()) {
                    m8 = null;
                    LOGGER.info("[smoketest] client world test finished (M8 only)");
                    mc.stop();
                }
                return;
            }
            if (M7_ONLY) {
                if (ticks == 60) m7 = new DevClientSmokeM7();
                if (m7 != null && m7.tick()) {
                    m7 = null;
                    LOGGER.info("[smoketest] client world test finished (M7 only)");
                    mc.stop();
                }
                return;
            }
            if (bigRegion != null && !bigDecorated && bigRegion.built()) {
                bigDecorated = true;
                mc.getSingleplayerServer().execute(() -> {
                    ServerLevel in = InteriorManager.interiorLevel(mc.getSingleplayerServer());
                    long t0 = System.nanoTime();
                    decorate(in, bigRegion, 16);
                    InteriorSync.markAllDirty(bigRegion.id());
                    LOGGER.info("[smoketest] big tank 16x16x16 decorated in {} ms", (System.nanoTime() - t0) / 1_000_000);
                });
            }
            switch (ticks) {
                case 60 -> mc.getSingleplayerServer().execute(DevClientSmokeTest::buildScene);
                case 290 -> mc.getSingleplayerServer().execute(() -> {
                    // close to the front glass: the miniature decoration
                    ServerPlayer p = player(mc);
                    p.teleportTo(p.level(), tankMin.getX() + 2.0, tankMin.getY() + 2.3, tankMin.getZ() - 2.0, java.util.Set.of(), 0F, 32F, false);
                });
                case 330 -> shot(mc, "bettacraft_m5_tank");
                case 340 -> mc.getSingleplayerServer().execute(() -> {
                    // M5: all named varieties side by side
                    ServerPlayer p = player(mc);
                    p.teleportTo(p.level(), typesMin.getX() + 4.0, typesMin.getY() + 1.0 - 1.62, typesMin.getZ() - 3.4, java.util.Set.of(), 0F, 0F, false);
                });
                case 400 -> shot(mc, "bettacraft_m5_types");
                case 410 -> mc.getSingleplayerServer().execute(() -> {
                    ServerPlayer p = player(mc);
                    p.teleportTo(p.level(), typesMin.getX() + 1.6, typesMin.getY() + 0.95 - 1.62, typesMin.getZ() - 1.25, java.util.Set.of(), 0F, 0F, false);
                });
                case 450 -> shot(mc, "bettacraft_m5_types_closeup");
                case 455 -> mc.getSingleplayerServer().execute(() -> {
                    ServerPlayer p = player(mc);
                    p.teleportTo(p.level(), typesMin.getX() + 6.4, typesMin.getY() + 0.95 - 1.62, typesMin.getZ() - 1.25, java.util.Set.of(), 0F, 0F, false);
                });
                case 495 -> shot(mc, "bettacraft_m5_types_closeup2");
                case 500 -> mc.getSingleplayerServer().execute(() -> {
                    // fin forms: 8 long-finned tails and 4 plakat forms in the same colours
                    ServerPlayer p = player(mc);
                    p.teleportTo(p.level(), formsMin.getX() + 3.0, formsMin.getY() + 1.0 - 1.62, formsMin.getZ() - 2.9, java.util.Set.of(), 0F, 0F, false);
                });
                case 550 -> shot(mc, "bettacraft_m5_forms");
                case 555 -> mc.getSingleplayerServer().execute(() -> {
                    // stages, flaring male and the female in breeding colours (bars)
                    ServerPlayer p = player(mc);
                    p.teleportTo(p.level(), showMin.getX() + 2.5, showMin.getY() - 1.15, showMin.getZ() - 2.1, java.util.Set.of(), 0F, 2F, false);
                });
                case 600 -> shot(mc, "bettacraft_m5_stages_bars");
                case 601 -> mc.getSingleplayerServer().execute(() -> {
                    // close-up of the female in breeding colours
                    ServerPlayer p = player(mc);
                    p.teleportTo(p.level(), showMin.getX() + 4.4, showMin.getY() + 0.42 - 1.62, showMin.getZ() - 0.55, java.util.Set.of(), 0F, 0F, false);
                });
                case 630 -> shot(mc, "bettacraft_m5_female_bars");
                case 635 -> mc.getSingleplayerServer().execute(() -> {
                    // straight at one variety (Jade shows the type)
                    ServerPlayer p = player(mc);
                    p.teleportTo(p.level(), typesMin.getX() + 1.5, typesMin.getY() + 0.42 - 1.62, typesMin.getZ() - 0.7, java.util.Set.of(), 0F, 0F, false);
                });
                case 660 -> shot(mc, "bettacraft_m5_jade_betta");
                case 665 -> mc.getSingleplayerServer().execute(() -> {
                    ServerPlayer p = player(mc);
                    p.teleportTo(p.level(), typesMin.getX() + 4.0, typesMin.getY() + 1.2, typesMin.getZ() - 2.5, java.util.Set.of(), 0F, 20F, false);
                    TankBlockEntity c = TankMultiblock.getController(p.level(), typesMin);
                    org.overengineer.bettacraft.platform.Platform.get().openMenu(p, new SimpleMenuProvider((id, inv, pl) -> new TankMenu(id, inv, c), Component.translatable("gui.bettacraft.tank.title")),
                            buf -> buf.writeBlockPos(c.getBlockPos()));
                });
                case 700 -> {
                    if (mc.gui.screen() instanceof TankScreen ts) ts.selectTabForTest(1);
                }
                case 730 -> shot(mc, "bettacraft_m5_menu_inhabitants");
                case 735 -> mc.getSingleplayerServer().execute(() -> {
                    // inventory with the new icons: decor, internal filter, buckets of every variety, cabinets
                    ServerPlayer p = player(mc);
                    p.closeContainer();
                    p.setGameMode(GameType.SURVIVAL);
                    p.getAbilities().flying = true;
                    p.getAbilities().mayfly = true;
                    p.onUpdateAbilities();
                    fillInventory(p);
                });
                case 760 -> mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
                case 770 -> hoverSlot(mc, 0, 1);
                case 800 -> shot(mc, "bettacraft_m5_inventory");
                case 805 -> hoverSlot(mc, 3, 3);
                case 830 -> shot(mc, "bettacraft_m5_inventory_decor");
                case 835 -> hoverSlot(mc, 7, 2);
                case 860 -> shot(mc, "bettacraft_m5_inventory_cabinet");
                case 865 -> {
                    mc.gui.setScreen(null);
                    mc.getSingleplayerServer().execute(() -> {
                        ServerPlayer p = player(mc);
                        p.getInventory().clearContent();
                        p.setGameMode(GameType.CREATIVE);
                        p.teleportTo(p.level(), tankMin.getX() + 2.0, tankMin.getY() + 3.5, tankMin.getZ() - 7.0, java.util.Set.of(), 0F, 20F, false);
                        InteriorManager.enter(p, tankMin);
                    });
                }
                case 940 -> mc.getSingleplayerServer().execute(() -> {
                    // holding a dino skull: placement hologram on the floor in front
                    ServerPlayer p = player(mc);
                    InteriorRegion r = InteriorManager.regionAt(p.level().getServer(), p.blockPosition());
                    if (r != null) {
                        BlockPos eye = r.toWorld(59, 1, 6);
                        p.teleportTo(p.level(), eye.getX() + 0.5, eye.getY(), eye.getZ() + 0.5, java.util.Set.of(), 0F, 45F, false);
                    }
                    p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                            new ItemStack(ModItems.DECOR.get(org.overengineer.bettacraft.decor.DecorType.DINO_SKULL).get()));
                });
                case 970 -> shot(mc, "bettacraft_m5_interior_hologram");
                case 975 -> mc.getSingleplayerServer().execute(() -> {
                    ServerPlayer p = player(mc);
                    p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    InteriorManager.leave(p);
                });
                case 1040 -> mc.getSingleplayerServer().execute(() -> {
                    ServerPlayer p = player(mc);
                    p.teleportTo(p.level(), cabMin.getX() + 5.5, cabMin.getY() + 2.4, cabMin.getZ() - 4.5, java.util.Set.of(), 0F, 16F, false);
                });
                case 1090 -> shot(mc, "bettacraft_m5_cabinets");
                case 1100 -> {
                    LOGGER.info("[smoketest] miniature render-thread time last frame: {} ms",
                            org.overengineer.bettacraft.client.interior.MiniatureRenderer.lastFrameNanos / 1e6);
                    // M6: handbook images, handbook pages and advancements (en, de)
                    mc.getSingleplayerServer().execute(() -> DevClientSmokeM6.grantAll(player(mc)));
                    m6 = new DevClientSmokeM6(java.util.List.of(
                            new DevClientSmokeM6.Spot("tank", tankMin.getX() + 2.0, tankMin.getY() + 2.0, tankMin.getZ() - 2.7, 0F, 24F),
                            new DevClientSmokeM6.Spot("nest", breedMin.getX() + 1.5, breedMin.getY() + 2.4, breedMin.getZ() + 0.2, 0F, 78F),
                            new DevClientSmokeM6.Spot("stages", showMin.getX() + 2.5, showMin.getY() - 1.12, showMin.getZ() - 1.5, 0F, 2F),
                            new DevClientSmokeM6.Spot("forms", formsMin.getX() + 3.0, formsMin.getY() + 1.0 - 1.62, formsMin.getZ() - 3.1, 0F, 0F),
                            new DevClientSmokeM6.Spot("varieties", typesMin.getX() + 4.0, typesMin.getY() + 1.0 - 1.62, typesMin.getZ() - 4.0, 0F, 0F),
                            new DevClientSmokeM6.Spot("cabinets", cabMin.getX() + 5.5, cabMin.getY() + 2.2, cabMin.getZ() - 5.0, 0F, 16F),
                            new DevClientSmokeM6.Spot("external_filter", filterMin.getX() + 1.9, filterMin.getY() + 0.9, filterMin.getZ() - 1.6, 0F, 38F)),
                            tankMin, typesMin, breedMin);
                }
                default -> {
                    if (m6 != null && m6.tick()) {
                        m6 = null;
                        m7 = new DevClientSmokeM7(); // M7: aquarium mods
                    }
                    if (m7 != null && m7.tick()) {
                        m7 = null;
                        m8 = new DevClientSmokeM8(); // M8: info overlays
                    }
                    if (m8 != null && m8.tick()) {
                        m8 = null;
                        m9 = new DevClientSmokeM9(); // M9: water care, coolers, breeders
                    }
                    if (m9 != null && m9.tick()) {
                        m9 = null;
                        m10 = new DevClientSmokeM10(); // M10: free tank shapes
                    }
                    if (m10 != null && m10.tick()) {
                        m10 = null;
                        m11 = new DevClientSmokeM11(); // M11: tank styles, edges, water seam, mirror stick
                    }
                    if (m11 != null && m11.tick()) {
                        m11 = null;
                        m12 = new DevClientSmokeM12(); // M12: floating plants
                    }
                    if (m12 != null && m12.tick()) {
                        m12 = null;
                        m13 = new DevClientSmokeM13(); // M13: aquarium plants
                    }
                    if (m13 != null && m13.tick()) {
                        m13 = null;
                        m14 = new DevClientSmokeM14(); // M14: shrimp, crayfish, snails, frogs, algae
                    }
                    if (m14 != null && m14.tick()) {
                        m14 = null;
                        m15 = new DevClientSmokeM15(); // M15: name sign, domed nest, interior leave hint
                    }
                    if (m15 != null && m15.tick()) {
                        m15 = null;
                        m16 = new DevClientSmokeM16(); // M16: water animals of other mods, no tank inside a tank
                    }
                    if (m16 != null && m16.tick()) {
                        m16 = null;
                        m18 = new DevClientSmokeM18(); // M18: coloured lights, young bettas in the stocking
                    }
                    if (m18 != null && m18.tick()) {
                        m18 = null;
                        m19 = new DevClientSmokeM19(); // M19: tankmates and their breeding
                    }
                    if (m19 != null && m19.tick()) {
                        m19 = null;
                        m20 = new DevClientSmokeM20(); // M20: reef tanks
                    }
                    if (m20 != null && m20.tick()) {
                        m20 = null;
                        m21 = new DevClientSmokeM21(); // M21: plant prototype, fish buckets
                    }
                    if (m21 != null && m21.tick()) {
                        m21 = null;
                        m211 = new DevClientSmokeM211(); // M21.1: plant meshes outside, stems, ribbons, lily pads
                    }
                    if (m211 != null && m211.tick()) {
                        m211 = null;
                        m215 = new DevClientSmokeM215(); // M21.5: polish
                    }
                    if (m215 != null && m215.tick()) {
                        m215 = null;
                        m216 = new DevClientSmokeM216(); // M21.6: guppy and endler genetics
                    }
                    if (m216 != null && m216.tick()) {
                        m216 = null;
                        m217 = new DevClientSmokeM217(); // M21.7: stone cabinets
                    }
                    if (m217 != null && m217.tick()) {
                        m217 = null;
                        m218 = new DevClientSmokeM218(); // M21.8: seahorses, cyst stacks, clear glass icon
                    }
                    if (m218 != null && m218.tick()) {
                        m218 = null;
                        m221 = new DevClientSmokeM221(); // M22.1: plant look for every species, swaying water plants, containers
                    }
                    if (m221 != null && m221.tick()) {
                        m221 = null;
                        m222 = new DevClientSmokeM222(); // M22.2: species gallery and the handbook en + de
                    }
                    if (m222 != null && m222.tick()) {
                        m222 = null;
                        m223 = new DevClientSmokeM223(); // M22.3: wild animals and corals, the magnifier, single animals, buckets
                    }
                    if (m223 != null && m223.tick()) {
                        m223 = null;
                        m224 = new DevClientSmokeM224(); // M22.4: the outlet sides of the cabinet filter, plant sources in the handbook
                    }
                    if (m224 != null && m224.tick()) {
                        m224 = null;
                        m23 = new DevClientSmokeM23(); // M23: aeration, internal filters, light tiers and timer
                    }
                    if (m23 != null && m23.tick()) {
                        m23 = null;
                        m24 = new DevClientSmokeM24(); // M24: pipes, multiblock filter, interior pipes, crucible, water devices
                    }
                    if (m24 != null && m24.tick()) {
                        m24 = null;
                        LOGGER.info("[smoketest] client world test finished");
                        mc.stop();
                    }
                }
            }
        } catch (RuntimeException ex) {
            LOGGER.error("[smoketest] FAILED", ex);
            mc.stop();
        }
    }

    /** Moves the mouse over a slot of the open inventory screen (col 0-8, row 0-2 main inventory, row 3 hotbar). */
    private static void hoverSlot(Minecraft mc, int col, int row) {
        if (!(mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> scr)) return;
        double scale = mc.getWindow().getGuiScale();
        int left = (scr.width - 176) / 2, top = (scr.height - 166) / 2;
        int x = left + 8 + col * 18 + 8, y = top + (row < 3 ? 84 + row * 18 : 142) + 8;
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().handle(), x * scale, y * scale);
        try {
            for (String f : new String[]{"xpos", "ypos"}) {
                java.lang.reflect.Field fld = net.minecraft.client.MouseHandler.class.getDeclaredField(f);
                fld.setAccessible(true);
                fld.setDouble(mc.mouseHandler, f.equals("xpos") ? x * scale : y * scale);
            }
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("[smoketest] cannot move the mouse: {}", e.toString());
        }
    }

    private static void fillInventory(ServerPlayer p) {
        var inv = p.getInventory();
        inv.clearContent();
        int slot = 0;
        for (var d : org.overengineer.bettacraft.decor.DecorType.values()) inv.setItem(slot++, new ItemStack(ModItems.DECOR.get(d).get()));
        inv.setItem(slot++, new ItemStack(ModItems.INTERNAL_FILTER.get()));
        inv.setItem(slot++, new ItemStack(ModItems.EXTERNAL_FILTER.get()));
        inv.setItem(slot++, new ItemStack(ModItems.HEATER_SMALL.get()));
        inv.setItem(slot++, new ItemStack(ModItems.BETTA_PELLETS.get(), 16));
        inv.setItem(slot++, new ItemStack(ModItems.MOSQUITO_LARVAE.get(), 8));
        inv.setItem(slot++, new ItemStack(ModItems.INFUSORIA.get()));
        inv.setItem(slot++, new ItemStack(ModItems.ARTEMIA_NAUPLII.get()));
        // main inventory: one bucket per variety, then cabinets
        slot = 17;
        for (var v : org.overengineer.bettacraft.genetics.GeneticsData.get().varieties()) {
            if (slot >= 33) break;
            BettaGenome g = BettaGenome.of(v.example(), BettaGenome.DEFAULT);
            inv.setItem(slot++, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), BettaData.create(g, false, 0)));
        }
        String[] woods = {"cherry_planks", "dark_oak_planks", "warped_planks"};
        for (String w : woods) {
            if (slot >= 36) break;
            inv.setItem(slot++, org.overengineer.bettacraft.equipment.CabinetBlockItem.setWood(new ItemStack(ModItems.CABINET.get()),
                    net.minecraft.resources.Identifier.withDefaultNamespace(w)));
        }
    }

    private static BlockPos typesMin, formsMin;

    /** M5: a showcase tank with every named variety and one with the fin forms (motionless fish, side view). */
    private static void m5Scene(ServerLevel level, BlockPos feet, ServerPlayer p) {
        typesMin = feet.offset(14, 0, 4);
        place(level, typesMin, 8, 2, 1);
        TankBlockEntity types = TankMultiblock.getController(level, typesMin);
        TankInteractions.changeWater(level, types, 16000);
        types.getTank().setName("Varieties");
        types.getTank().setSignVisible(false);
        types.getTank().setFront(Direction.NORTH);
        java.util.List<BettaData> list = new java.util.ArrayList<>();
        for (var v : org.overengineer.bettacraft.genetics.GeneticsData.get().varieties()) {
            list.add(BettaData.create(BettaGenome.of(v.example(), BettaGenome.DEFAULT), false, level.getGameTime()));
        }
        showcase(level, types, typesMin, list, 8, p);
        formsMin = feet.offset(14, 0, 10);
        place(level, formsMin, 6, 2, 1);
        TankBlockEntity forms = TankMultiblock.getController(level, formsMin);
        TankInteractions.changeWater(level, forms, 12000);
        forms.getTank().setSignVisible(false);
        java.util.List<BettaData> fl = new java.util.ArrayList<>();
        java.util.Map<String, String> colours = java.util.Map.of("body_color", "royal_blue", "fin_color", "red", "pattern", "butterfly",
                "pattern_color", "white", "iridescence", "low", "iridescence_color", "steel");
        for (String len : new String[]{"long", "short"}) {
            for (String shape : BettaGenome.SHAPES) {
                if (len.equals("short") && !java.util.List.of("round", "halfmoon", "crowntail", "doubletail").contains(shape)) continue;
                java.util.Map<String, String> m = new java.util.HashMap<>(colours);
                m.put("finform", shape);
                m.put("fin_length", len);
                fl.add(BettaData.create(BettaGenome.of(m, BettaGenome.DEFAULT), false, level.getGameTime()));
            }
        }
        showcase(level, forms, formsMin, fl, 6, p);
    }

    private static void showcase(ServerLevel level, TankBlockEntity c, BlockPos min, java.util.List<BettaData> fish, int perRow, ServerPlayer p) {
        java.util.List<java.util.UUID> order = new java.util.ArrayList<>();
        for (BettaData d : fish) {
            ItemStack bucket = BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d);
            Component err = TankInhabitants.insert(level, c, bucket, ModEntities.BETTA.get(), p, false);
            if (err != null) LOGGER.warn("[smoketest] showcase insert failed: {}", err.getString());
            order.add(d.fishId());
        }
        for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) {
            if (e instanceof org.overengineer.bettacraft.entity.betta.BettaEntity m) {
                int k = order.indexOf(m.getBettaData().fishId());
                SmokeFreeze.freeze(m, true);
                int col = k % perRow, row = k / perRow;
                m.snapTo(min.getX() + col + 0.5, min.getY() + (1 - row) + 0.38, min.getZ() + 0.5, 90F, 0F);
                m.setYBodyRot(90F);
                m.setYHeadRot(90F);
            }
        }
        c.markTankChanged();
    }

    private static ServerPlayer player(Minecraft mc) {
        return mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.gameRenderer.mainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }

    private static void buildScene() {
        Minecraft mc = Minecraft.getInstance();
        ServerPlayer p = player(mc);
        ServerLevel level = p.level();
        org.overengineer.bettacraft.gametest.framework.DevTime.set(level, 6000);
        p.getAbilities().flying = true; // keep camera positions (creative, no falling)
        p.onUpdateAbilities();
        BlockPos feet = p.blockPosition();
        // tank 4x3x3 south of the player, front facing the player
        tankMin = feet.offset(-2, 0, 4);
        place(level, tankMin, 4, 3, 3);
        TankBlockEntity c = TankMultiblock.getController(level, tankMin);
        TankInteractions.changeWater(level, c, 30000);
        c.getTank().setName("Betta Paradise");
        c.getTank().setSignPos(SignPosition.TOP_CENTER);
        c.getTank().setFront(Direction.NORTH);
        // M4: external filter on the west side with a large heater
        BlockPos efp = tankMin.offset(-1, 0, 1);
        level.setBlockAndUpdate(efp, ModBlocks.EXTERNAL_FILTER.get().defaultBlockState()
                .setValue(org.overengineer.bettacraft.equipment.ExternalFilterBlock.FACING, Direction.EAST));
        if (level.getBlockEntity(efp) instanceof org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity ef) {
            ef.insertHeater(new ItemStack(ModItems.HEATER_LARGE.get()));
        }
        c.getTank().setOwner(p.getUUID());
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        decorate(in, r, 3);
        decorItems(in, r);
        InteriorSync.markDirty(r.id());

        RandomSource rnd = RandomSource.create(1234);
        for (int i = 0; i < 6; i++) {
            BettaGenome g = BettaGenome.randomDomestic(rnd).withShape(BettaGenome.SHAPES.get(i % BettaGenome.SHAPES.size()));
            ItemStack bucket = BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), BettaData.create(g, i >= 4, level.getGameTime()));
            Component err = TankInhabitants.insert(level, c, bucket, ModEntities.BETTA.get(), p, false);
            if (err != null) LOGGER.warn("[smoketest] insert failed: {}", err.getString());
        }
        int n = 0;
        for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) {
            if (n++ == 0) e.setCustomName(Component.literal("Bubbles"));
        }
        // light bar along the front and a feeder on top
        for (int x = 0; x < 4; x++) {
            BlockPos lp = tankMin.offset(x, 3, 1);
            level.setBlockAndUpdate(lp, ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X));
        }
        for (int x = 0; x < 4; x++) {
            BlockPos lp = tankMin.offset(x, 3, 1);
            BlockState s = level.getBlockState(lp);
            BlockState e = s.updateShape(level, level, lp, Direction.EAST, lp.east(), level.getBlockState(lp.east()), level.getRandom());
            level.setBlockAndUpdate(lp, e.updateShape(level, level, lp, Direction.WEST, lp.west(), level.getBlockState(lp.west()), level.getRandom()));
        }
        BlockPos fp = tankMin.offset(3, 3, 2);
        level.setBlockAndUpdate(fp, ModBlocks.FEEDER.get().defaultBlockState());
        if (level.getBlockEntity(fp) instanceof FeederBlockEntity f) {
            f.items().insertItem(0, new ItemStack(ModItems.BETTA_PELLETS.get(), 16), false);
            f.items().insertItem(1, new ItemStack(ModItems.MOSQUITO_LARVAE.get(), 8), false);
            f.feedNow(level, c);
        }
        c.markTankChanged();
        // a pool with a wild betta next to it (ordinary water stays vanilla)
        for (int x = 0; x < 2; x++)
            for (int z = 0; z < 2; z++) level.setBlockAndUpdate(feet.offset(4 + x, -1, 4 + z), Blocks.WATER.defaultBlockState());
        var wild = ModEntities.BETTA.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (wild != null) {
            wild.snapTo(feet.getX() + 5, feet.getY() - 0.8, feet.getZ() + 5, 0, 0);
            wild.finalizeSpawn(level, level.getCurrentDifficultyAt(wild.blockPosition()), net.minecraft.world.entity.EntitySpawnReason.NATURAL, null);
            level.addFreshEntity(wild);
        }
        // showcase: one motionless betta per body shape in a 5x1x1 tank (for a close-up screenshot)
        showMin = feet.offset(-12, 0, 4);
        place(level, showMin, 5, 1, 1);
        TankBlockEntity show = TankMultiblock.getController(level, showMin);
        TankInteractions.changeWater(level, show, 5000);
        show.getTank().setSignVisible(false);
        // showcase: fry, juvenile, subadult, flaring adult male, female in breeding colours (motionless, for a close-up)
        RandomSource sr = RandomSource.create(99);
        org.overengineer.bettacraft.entity.betta.LifeStage[] stages = {org.overengineer.bettacraft.entity.betta.LifeStage.FRY,
                org.overengineer.bettacraft.entity.betta.LifeStage.JUVENILE, org.overengineer.bettacraft.entity.betta.LifeStage.SUBADULT,
                org.overengineer.bettacraft.entity.betta.LifeStage.ADULT, org.overengineer.bettacraft.entity.betta.LifeStage.ADULT};
        java.util.List<java.util.UUID> order = new java.util.ArrayList<>();
        BettaGenotype showGenes = BettaGenotype.randomDomestic(sr).with(org.overengineer.bettacraft.genetics.BettaLoci.FINFORM,
                new BettaGenotype.AllelePair("halfmoon", "round"));
        for (int i = 0; i < 5; i++) {
            BettaData d = BettaData.create(showGenes, i == 4, level.getGameTime()).withStage(stages[i]);
            ItemStack bucket = BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d);
            TankInhabitants.insert(level, show, bucket, ModEntities.BETTA.get(), p, false);
            order.add(d.fishId());
        }
        for (Entity e : TankInhabitants.liveInhabitants(level, show.getTank())) {
            if (e instanceof org.overengineer.bettacraft.entity.betta.BettaEntity m) {
                int k = order.indexOf(m.getBettaData().fishId());
                SmokeFreeze.freeze(m, true);
                m.snapTo(showMin.getX() + k + 0.5, showMin.getY() + 0.4, showMin.getZ() + 0.5, 90F, 0F);
                m.setYBodyRot(90F);
                m.setYHeadRot(90F);
                if (k == 3) m.flare(Long.MAX_VALUE / 2);
                if (k == 4) m.showBreedingMood(Long.MAX_VALUE / 2);
                if (k == 3) m.setCustomName(Component.literal("Sultan"));
            }
        }
        // breeding tank: a pair in warm water, run until the female has spawned into the bubble nest
        breedMin = feet.offset(5, 0, 9);
        place(level, breedMin, 3, 2, 2);
        TankBlockEntity breed = TankMultiblock.getController(level, breedMin);
        TankInteractions.changeWater(level, breed, 12000);
        breed.getTank().setName("Breeding");
        breed.getTank().setFront(Direction.NORTH);
        breed.getTank().setOwner(p.getUUID());
        BlockPos bfp = breedMin.offset(3, 0, 0);
        level.setBlockAndUpdate(bfp, ModBlocks.EXTERNAL_FILTER.get().defaultBlockState()
                .setValue(org.overengineer.bettacraft.equipment.ExternalFilterBlock.FACING, Direction.WEST));
        if (level.getBlockEntity(bfp) instanceof org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity bf) {
            bf.insertHeater(new ItemStack(ModItems.HEATER_LARGE.get()));
        }
        org.overengineer.bettacraft.tank.TankEquipment.invalidate(breed);
        breed.getTank().setTargetTemp(27.0F);
        breed.getTank().setWaterTemp(27.0F);
        RandomSource br = RandomSource.create(4321);
        for (int i = 0; i < 2; i++) {
            BettaGenotype g = BettaGenotype.randomDomestic(br).with(org.overengineer.bettacraft.genetics.BettaLoci.FINFORM,
                    new BettaGenotype.AllelePair(i == 0 ? "round" : "veil", "halfmoon"));
            ItemStack bucket = BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), BettaData.create(g, i == 1, level.getGameTime()));
            TankInhabitants.insert(level, breed, bucket, ModEntities.BETTA.get(), p, false);
        }
        for (Entity e : TankInhabitants.liveInhabitants(level, breed.getTank())) {
            if (e instanceof org.overengineer.bettacraft.entity.betta.BettaEntity b) e.setCustomName(Component.literal(b.isFemale() ? "Luna" : "Rex"));
        }
        TankCare.feed(level, breed, new ItemStack(ModItems.MOSQUITO_LARVAE.get()), 1);
        for (int i = 0; i < 60 && (breed.getTank().nest() == null || !breed.getTank().nest().hasEggs()); i++) {
            org.overengineer.bettacraft.tank.TankBreeding.step(level, breed, 0.05);
        }
        org.overengineer.bettacraft.tank.TankBreeding.step(level, breed, 0.3); // larvae
        breed.markTankChanged();
        // 16x16x16 tank at scale 16, prepared asynchronously like for a player, decorated when ready (see tick)
        bigMin = feet.offset(-40, 0, 30);
        place(level, bigMin, 16, 16, 16);
        TankBlockEntity big = TankMultiblock.getController(level, bigMin);
        big.getTank().setSignVisible(false);
        TankInteractions.changeWater(level, big, 16L * 16 * 16 * 1000);
        InteriorRegion bigR = InteriorManager.regionFor(level, big);
        org.overengineer.bettacraft.tank.interior.InteriorPreparation.prepare(level.getServer(), bigR, null);
        bigRegion = bigR;
        m4Scene(level, feet, p);
        m5Scene(level, feet, p);
        p.teleportTo(level, tankMin.getX() + 2.0, tankMin.getY() + 3.5, tankMin.getZ() - 7.0, java.util.Set.of(), 0F, 20F, false);
        LOGGER.info("[smoketest] scene built: {} inhabitants, {}", TankInhabitants.liveInhabitants(level, c.getTank()).size(), c.getTank());
    }

    private static BlockPos cabMin, filterMin;

    /**
     * M4: four pairs of cabinets (oak, cherry, crimson, bamboo) each carrying a 2x2x1 tank, a filter with heater in
     * one cabinet per pair; and a small 2x1x1 tank with low water, an external filter (east) and an internal filter.
     */
    private static void m4Scene(ServerLevel level, BlockPos feet, ServerPlayer p) {
        cabMin = feet.offset(-16, 0, 14);
        String[] woods = {"oak_planks", "cherry_planks", "crimson_planks", "bamboo_planks"};
        for (int g = 0; g < woods.length; g++) {
            BlockPos gx = cabMin.offset(g * 3, 0, 0);
            for (int i = 0; i < 2; i++) {
                BlockPos cp = gx.offset(i, 0, 0);
                level.setBlockAndUpdate(cp, ModBlocks.CABINET.get().defaultBlockState()
                        .setValue(org.overengineer.bettacraft.equipment.CabinetBlock.FACING, Direction.NORTH));
                if (level.getBlockEntity(cp) instanceof org.overengineer.bettacraft.equipment.CabinetBlockEntity cab) {
                    cab.setWood(net.minecraft.resources.Identifier.withDefaultNamespace(woods[g]));
                    if (i == 1) {
                        ItemStack f = new ItemStack(ModItems.EXTERNAL_FILTER.get());
                        org.overengineer.bettacraft.equipment.FilterHeaters.write(f, java.util.List.of(new ItemStack(ModItems.HEATER_MEDIUM.get()), ItemStack.EMPTY));
                        cab.items().setStackInSlot(org.overengineer.bettacraft.equipment.CabinetBlockEntity.FILTER_SLOT, f);
                        cab.storage().insertItem(0, new ItemStack(ModItems.BETTA_PELLETS.get(), 32), false);
                    }
                    cab.setChanged();
                    level.sendBlockUpdated(cp, level.getBlockState(cp), level.getBlockState(cp), 3);
                }
            }
            for (int i = 0; i < 2; i++) {
                BlockPos cp = gx.offset(i, 0, 0);
                level.setBlockAndUpdate(cp, net.minecraft.world.level.block.Block.updateFromNeighbourShapes(level.getBlockState(cp), level, cp));
            }
            place(level, gx.above(), 2, 2, 1);
            TankBlockEntity t = TankMultiblock.getController(level, gx.above());
            TankInteractions.changeWater(level, t, 3400);
            t.getTank().setSignVisible(false);
            t.markTankChanged();
        }
        // filter demo tank
        filterMin = feet.offset(8, 0, -6);
        place(level, filterMin, 2, 1, 1);
        TankBlockEntity ft = TankMultiblock.getController(level, filterMin);
        TankInteractions.changeWater(level, ft, 900);
        ft.getTank().setSignVisible(false);
        ft.getTank().setFront(Direction.NORTH);
        BlockPos fp = filterMin.offset(2, 0, 0);
        level.setBlockAndUpdate(fp, ModBlocks.EXTERNAL_FILTER.get().defaultBlockState()
                .setValue(org.overengineer.bettacraft.equipment.ExternalFilterBlock.FACING, Direction.WEST));
        if (level.getBlockEntity(fp) instanceof org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity f) {
            f.insertHeater(new ItemStack(ModItems.HEATER_SMALL.get()));
            f.insertHeater(new ItemStack(ModItems.HEATER_SMALL.get()));
        }
        InteriorRegion r = InteriorManager.ensureRegion(level, ft);
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        LongOpenHashSet touched = new LongOpenHashSet();
        BlockPos o = r.toWorld(0, 0, 0);
        InteriorWorldOps.fill(in, o.getX(), o.getY(), o.getZ(), o.getX() + r.sizeX() - 1, o.getY(), o.getZ() + r.sizeZ() - 1,
                Blocks.SAND.defaultBlockState(), touched);
        InteriorWorldOps.resendChunks(in, touched);
        int[] shape = org.overengineer.bettacraft.equipment.SegmentBlock.filterShape(r.scale());
        for (int y = 0; y < shape[1]; y++)
            for (int z = 0; z < shape[0]; z++)
                for (int x = 0; x < shape[0]; x++) {
                    var part = y == 0 ? (x == 0 && z == 0 ? org.overengineer.bettacraft.equipment.SegmentBlock.Part.ANCHOR
                            : org.overengineer.bettacraft.equipment.SegmentBlock.Part.BASE)
                            : y == shape[1] - 1 ? org.overengineer.bettacraft.equipment.SegmentBlock.Part.TOP
                            : org.overengineer.bettacraft.equipment.SegmentBlock.Part.MIDDLE;
                    in.setBlockAndUpdate(r.toWorld(8 + x, 1 + y, 8 + z), ModBlocks.FILTER_SEGMENT.get().defaultBlockState()
                            .setValue(org.overengineer.bettacraft.equipment.SegmentBlock.PART, part)
                            .setValue(org.overengineer.bettacraft.equipment.SegmentBlock.FACING, Direction.NORTH));
                }
        for (int i = 0; i < 6; i++) in.setBlockAndUpdate(r.toWorld(20 + (i % 3) * 3, 1, 6 + (i / 3) * 4), Blocks.SEAGRASS.defaultBlockState());
        InteriorSync.markDirty(r.id());
        ft.markTankChanged();
    }

    private static void place(ServerLevel level, BlockPos min, int sx, int sy, int sz) {
        for (int y = 0; y < sy; y++)
            for (int z = 0; z < sz; z++)
                for (int x = 0; x < sx; x++) {
                    BlockPos pos = min.offset(x, y, z);
                    level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
                    TankMultiblock.onTankBlockAdded(level, pos, null);
                }
    }

    /** M3: decoration structures, a substrate patch, a heater rod and lily pads in the main tank. */
    private static void decorItems(ServerLevel in, InteriorRegion r) {
        // clear a stage in the front half and put the decoration structures on it
        LongOpenHashSet touched = new LongOpenHashSet();
        InteriorWorldOps.clear(in, r.originX() + 1, InteriorRegion.BASE_Y, r.originZ() + 1, r.originX() + r.sizeX() - 2,
                InteriorRegion.BASE_Y + r.sizeY() - 1, r.originZ() + 30, touched);
        InteriorWorldOps.resendChunks(in, touched);
        org.overengineer.bettacraft.decor.DecorType[] types = {org.overengineer.bettacraft.decor.DecorType.CASTLE,
                org.overengineer.bettacraft.decor.DecorType.SHIPWRECK, org.overengineer.bettacraft.decor.DecorType.VOLCANO,
                org.overengineer.bettacraft.decor.DecorType.DINO_SKULL, org.overengineer.bettacraft.decor.DecorType.CAVE,
                org.overengineer.bettacraft.decor.DecorType.DRIFTWOOD, org.overengineer.bettacraft.decor.DecorType.STONES,
                org.overengineer.bettacraft.decor.DecorType.HAMMOCK, org.overengineer.bettacraft.decor.DecorType.ALMOND_LEAF,
                org.overengineer.bettacraft.decor.DecorType.MOSS_BALL};
        int[][] spots = {{32, 14}, {12, 10}, {52, 12}, {20, 22}, {44, 24}, {8, 24}, {26, 6}, {38, 6}, {16, 4}, {28, 26}};
        for (int i = 0; i < types.length; i++) {
            var err = org.overengineer.bettacraft.decor.DecorItem.place(in, types[i], r.toWorld(spots[i][0], 0, spots[i][1]),
                    net.minecraft.world.level.block.Rotation.NONE, null);
            if (err != null) LOGGER.warn("[smoketest] decor {}: {}", types[i], err.getString());
        }
        for (int y = 0; y < 6; y++) {
            in.setBlockAndUpdate(r.toWorld(58, y, 4), ModBlocks.HEATER_SEGMENT.get().defaultBlockState()
                    .setValue(org.overengineer.bettacraft.equipment.SegmentBlock.PART, y == 0 ? org.overengineer.bettacraft.equipment.SegmentBlock.Part.ANCHOR
                            : y == 5 ? org.overengineer.bettacraft.equipment.SegmentBlock.Part.TOP : org.overengineer.bettacraft.equipment.SegmentBlock.Part.MIDDLE)
                    .setValue(org.overengineer.bettacraft.equipment.SegmentBlock.SIZE, org.overengineer.bettacraft.equipment.SegmentBlock.Size.LARGE));
        }
        for (int[] lp : new int[][]{{20, 14}, {21, 15}, {44, 8}}) {
            in.setBlockAndUpdate(r.toWorld(lp[0], r.sizeY() - 6, lp[1]), Blocks.LILY_PAD.defaultBlockState());
        }
        // substrate pattern on the floor of the front stage (its sand layer was cleared above the floor only)
        for (int x = 0; x < r.sizeX(); x++)
            for (int z = 0; z < r.sizeZ(); z++)
                org.overengineer.bettacraft.tank.interior.Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z),
                        ((x / 4 + z / 4) % 2 == 0 ? Blocks.GRAVEL : Blocks.RED_SAND).defaultBlockState(), false);
    }

    /** Substrate (1/16 of the height), rock boxes and plant columns, written directly into the sections. */
    private static void decorate(ServerLevel in, InteriorRegion r, int tankHeight) {
        LongOpenHashSet touched = new LongOpenHashSet();
        int s = r.scale();
        int sub = Math.max(2, r.sizeY() / 16);
        BlockPos o = r.toWorld(0, 0, 0);
        InteriorWorldOps.fill(in, o.getX(), o.getY(), o.getZ(), o.getX() + r.sizeX() - 1, o.getY() + sub - 1, o.getZ() + r.sizeZ() - 1,
                Blocks.SAND.defaultBlockState(), touched);
        java.util.Random rnd = new java.util.Random(7);
        BlockState[] rocks = {Blocks.STONE.defaultBlockState(), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), Blocks.TUFF.defaultBlockState()};
        int rockCount = Math.max(3, r.sizeX() * r.sizeZ() / 1600);
        for (int i = 0; i < rockCount; i++) {
            int w = s / 2 + rnd.nextInt(s), h = s / 2 + rnd.nextInt(s);
            int x = rnd.nextInt(r.sizeX() - w), z = rnd.nextInt(r.sizeZ() - w);
            InteriorWorldOps.fill(in, o.getX() + x, o.getY() + sub, o.getZ() + z, o.getX() + x + w - 1, o.getY() + sub + h - 1,
                    o.getZ() + z + w - 1, rocks[i % rocks.length], touched);
        }
        BlockState[] plants = {Blocks.SEAGRASS.defaultBlockState(), Blocks.KELP_PLANT.defaultBlockState(), Blocks.TUBE_CORAL_BLOCK.defaultBlockState()};
        int plantCount = Math.max(20, r.sizeX() * r.sizeZ() / 220);
        for (int i = 0; i < plantCount; i++) {
            int x = rnd.nextInt(r.sizeX()), z = rnd.nextInt(r.sizeZ());
            int h = s * (1 + rnd.nextInt(Math.max(1, tankHeight / 2)));
            InteriorWorldOps.fill(in, o.getX() + x, o.getY() + sub, o.getZ() + z, o.getX() + x, Math.min(o.getY() + sub + h, o.getY() + r.sizeY() - 1),
                    o.getZ() + z, plants[i % plants.length], touched);
        }
        InteriorWorldOps.resendChunks(in, touched);
    }
}
