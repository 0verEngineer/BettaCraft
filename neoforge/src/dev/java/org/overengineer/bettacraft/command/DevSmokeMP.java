package org.overengineer.bettacraft.command;

import org.overengineer.bettacraft.util.NbtCompat;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.menu.TankMenu;
import org.overengineer.bettacraft.tank.plants.FloatingPlant;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tank.plants.TankPlants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development-only multiplayer test, server side ({@code -Dbettacraft.smoketest.mp=true}; {@code gradlew runMultiplayerTest}
 * starts this dedicated server and two clients, see build.gradle). Builds a test stage in the dev world (a 4x3x3 tank
 * with water, two bettas, floating plants, an external filter with its jet and a tank light, and an L-shaped tank),
 * waits for the players Dev1 and Dev2 (their clients run {@code DevClientSmokeMP}) and conducts the scenario over system
 * chat lines {@code bcmp:<command>:<target>:<args>} that the clients act on; the clients answer with the command
 * {@code /bettacraftmp <report>}:
 * <ol>
 *     <li>both clients report what they see (water, plants with ids and positions, fish) = the server state;</li>
 *     <li>Dev1 right-clicks like a player: water bucket (+1000 mB), aquarium net from the side (takes the plant in front),
 *     net + sneak (thins by half), empty hand (opens the menu: plants tab, "Thin out" button, water tab);</li>
 *     <li>the server adds plants; both clients report again: Dev2 sees every change made by Dev1;</li>
 *     <li>both clients measure the plant positions frame by frame (they move, no jumps), then take screenshots
 *     (the tank, the info overlay) and quit.</li>
 * </ol>
 * Logs "[smoketest] MP PASSED" or "[smoketest] MP FAILED" with every failed check and stops the server; it never waits
 * longer than {@link #TIMEOUT_TICKS}.
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class DevSmokeMP {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static final boolean ENABLED = Boolean.getBoolean("bettacraft.smoketest.mp");
    /** Whole test: 10 minutes (the clients need about a minute to start). */
    private static final int TIMEOUT_TICKS = 20 * 60 * 10;
    static final String P1 = "Dev1", P2 = "Dev2";
    static final String PREFIX = "bcmp:";

    private record Step(String name, Runnable start, BooleanSupplier done, int timeout) {}

    private static final Deque<Step> steps = new ArrayDeque<>();
    private static final Map<String, String> reports = new HashMap<>();
    private static final List<String> failures = new ArrayList<>();
    private static final List<String> passes = new ArrayList<>();
    private static MinecraftServer server;
    private static int ticks, stepTicks, haltAt = -1;
    private static Step current;
    private static boolean finished;
    private static BlockPos base, mainMin, lMin;
    private static long waterBefore;
    private static int plantsBefore, growingBefore;
    private static boolean savedEvaporation;
    // M15: where Dev1 / Dev2 stood when they entered the L tank (they must come back there)
    private static final Map<String, net.minecraft.world.phys.Vec3> entered = new HashMap<>();
    private static int cobbleBefore = -1;

    private DevSmokeMP() {}

    // ------------------------------------------------------------------ reports from the clients

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        if (!ENABLED) return;
        e.getDispatcher().register(Commands.literal("bettacraftmp")
                .then(Commands.argument("report", StringArgumentType.greedyString()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    report(p, StringArgumentType.getString(ctx, "report"));
                    return 1;
                })));
    }

    private static void report(ServerPlayer p, String text) {
        String name = p.getGameProfile().name();
        LOGGER.info("[smoketest] MP report from {}: {}", name, text);
        String[] a = text.split(" ");
        switch (a[0]) {
            case "state" -> compareState(name, a);
            case "motion" -> checkMotion(name, a);
            case "aqua" -> checkAquatic(name, a);
            case "critters" -> checkCritters(name, a);
            case "seen" -> checkSeen(name, a);
            case "mini" -> checkMini(name, a);
            case "mine" -> checkMine(name, a);
            case "menu" -> check(name + ": tank menu (" + text.substring(5) + ")", "ok".equals(a[1]));
            case "light" -> {
                // M18: the bar was dyed red on the server: the client has the colour and tints the water with it
                check(name + ": the dyed light is " + a[1] + " on the client (server red)", "red".equals(a[1]));
                check(name + ": the water of the tank is tinted (" + a[2] + ")", !"0".equals(a[2]));
            }
            case "tankmates" -> checkTankmates(name, a);
            case "m29" -> { // M29 part d: the big crayfish, the axolotls (vanilla, colours) and a wild betta hybrid on every client
                check(name + ": big crayfish " + (a.length > 1 ? a[1] : "?") + " (server florida_crayfish:0,marbled_crayfish:-1)",
                        a.length > 1 && "florida_crayfish:0,marbled_crayfish:-1".equals(a[1]));
                check(name + ": axolotls " + (a.length > 2 ? a[2] : "?") + " (server blue,gold)", a.length > 2 && "blue,gold".equals(a[2]));
                String hy = org.overengineer.bettacraft.genetics.BettaSpecies.HYBRID_ID;
                check(name + ": betta species " + (a.length > 3 ? a[3] : "?") + " (server " + hy + ", half Betta imbellis)", a.length > 3 && hy.equals(a[3]));
            }
            case "m30" -> { // M30 part c: two bamboo sharks and a moray (big entities) and an egg case on every client
                check(name + ": big animals " + (a.length > 1 ? a[1] : "?") + " (server bamboo_shark,bamboo_shark,honeycomb_moray)",
                        a.length > 1 && "bamboo_shark,bamboo_shark,honeycomb_moray".equals(a[1]));
                check(name + ": egg cases " + (a.length > 2 ? a[2] : "?") + " (server 1)", a.length > 2 && "1".equals(a[2]));
            }
            case "m31" -> { // M31 part a: a dry terrarium with a lit basking lamp: its mode and climate reach every client
                check(name + ": tank mode " + (a.length > 1 ? a[1] : "?") + " (server terrarium)", a.length > 1 && "terrarium".equals(a[1]));
                check(name + ": climate synced " + (a.length > 2 ? a[2] : "?") + " (server true)", a.length > 2 && "true".equals(a[2]));
                check(name + ": UV at the basking spot " + (a.length > 3 ? a[3] : "?") + " (server 5)", a.length > 3 && "5".equals(a[3]));
                // M31 part b: the synced turtle state: the variety of a yellow-bellied slider and the size of a hatchling
                check(name + ": slider variety " + (a.length > 4 ? a[4] : "?") + " (server 1 = yellow-bellied)", a.length > 4 && "1".equals(a[4]));
                check(name + ": hatchling growth " + (a.length > 5 ? a[5] : "?") + " (server 0)", a.length > 5 && "0".equals(a[5]));
                // M31 part c: the pose of a crested gecko on a wall and its morph
                check(name + ": gecko pose " + (a.length > 6 ? a[6] : "?") + " (server WALL_E)", a.length > 6 && "WALL_E".equals(a[6]));
                check(name + ": gecko morph " + (a.length > 7 ? a[7] : "?") + " (server 2 = harlequin)", a.length > 7 && "2".equals(a[7]));
                // M31 part d: the zombie in glass, shrunk to 0.18
                check(name + ": zombie scale x100 " + (a.length > 8 ? a[8] : "?") + " (server 18)", a.length > 8 && "18".equals(a[8]));
                // M33.1: the tank type
                check(name + ": tank type " + (a.length > 9 ? a[9] : "?") + " (server terrarium)", a.length > 9 && "terrarium".equals(a[9]));
            }
            case "reef" -> checkReef(name, a);
            case "m44" -> { // M44: the fronts of the cabinet row and the legs of the zombie in glass on every client
                check(name + ": cabinet fronts " + (a.length > 1 ? a[1] : "?") + " (server DSRD-)", a.length > 1 && "DSRD-".equals(a[1]));
                int walk = a.length > 2 ? Integer.parseInt(a[2]) : -1, ground = a.length > 3 ? Integer.parseInt(a[3]) : -1;
                check(name + ": the zombie in glass swings its legs " + walk + " / 100 on average (at least 15)", walk >= 15);
                check(name + ": the zombie in glass stands on the ground " + ground + " % of the ticks (at least 50)", ground >= 50);
            }
            case "cabinets" -> check(name + ": stone and wooden cabinet " + (a.length > 2 ? a[1] + " " + a[2] : "?") + " (server minecraft:deepslate_bricks minecraft:cherry_planks)",
                    a.length > 3 && "minecraft:deepslate_bricks".equals(a[1]) && "minecraft:cherry_planks".equals(a[2]) && "deepslate_bricks".equals(a[3]));
            case "seahorses" -> checkSeahorses(name, a);
            case "sizes" -> {
                // M36: {@code sizes <setting> <bettas> <hitboxes follow>} the server's betta size on this client
                String want = ServerConfig.SIZE_BETTA.get().name();
                check(name + ": betta size " + (a.length > 1 ? a[1] : "?") + " (server " + want + ")", a.length > 1 && want.equals(a[1]));
                check(name + ": " + (a.length > 2 ? a[2] : "?") + " bettas seen, their hitboxes follow the size: " + (a.length > 3 ? a[3] : "?"),
                        a.length > 3 && !"0".equals(a[2]) && "true".equals(a[3]));
            }
            case "containers" -> checkContainers(name, a);
            case "wildlife" -> {
                // M22.3: the wild small animals with their species and colour, and the genes switch, on every client
                check(name + ": " + (a.length > 1 ? a[1] : "?") + " wild small animals (server 3)", a.length > 3 && "3".equals(a[1]));
                check(name + ": species and colours " + (a.length > 2 ? a[2] : "?") + " (server hermit_crab:-1,neocaridina:0,ramshorn:-1)",
                        a.length > 2 && "hermit_crab:-1,neocaridina:0,ramshorn:-1".equals(a[2]));
                check(name + ": genes always visible " + (a.length > 3 ? a[3] : "?") + " (server true after the live change)", a.length > 3 && "true".equals(a[3]));
            }
            case "outlets" -> {
                // M22.4: the outlet side of each cabinet reaches every client (set on the server, then by Dev1's menu button)
                String want = OUTLETS_EXPECTED;
                check(name + ": cabinet outlets " + (a.length > 1 ? a[1] : "?") + " (server " + want + ")", a.length > 1 && want.equals(a[1]));
            }
            case "aeration" -> check(name + ": air pump runs, bubbling stones and LED panel halves " + (a.length > 1 ? a[1] : "?") + " (server true,2,2)",
                    a.length > 1 && "true,2,2".equals(a[1]));
            case "plumbing" -> check(name + ": pipe joins pump and reservoir, controller interval and share " + (a.length > 1 ? a[1] : "?") + " (server true,9,25)",
                    a.length > 1 && "true,9,25".equals(a[1])); // M24
            case "pipecover" -> check(name + ": covers, bundle lines, light, full shape " + (a.length > 1 ? a[1] : "?")
                    + " (server minecraft:stone_bricks,minecraft:glowstone,2,15,true)", a.length > 1 && "minecraft:stone_bricks,minecraft:glowstone,2,15,true".equals(a[1])); // M40 part c
            case "pipecoveroff" -> check(name + ": the pipe's cover is off " + (a.length > 1 ? a[1] : "?") + " (server none,minecraft:glowstone,2,15,false)",
                    a.length > 1 && "none,minecraft:glowstone,2,15,false".equals(a[1]));
            case "cabpump" -> check(name + ": air pump in the cabinet and pump sounds off by default " + (a.length > 1 ? a[1] : "?") + " (expected true,false,false)",
                    a.length > 1 && "true,false,false".equals(a[1])); // M29.1
            case "outletmenu" -> check(name + ": cabinet menu with the outlet button (" + (a.length > 1 ? a[1] : "?") + ")", a.length > 1 && "ok".equals(a[1]));
            case "monitor" -> { // M27
                float temp = TankMultiblock.getController(server.overworld(), monitorMin()) instanceof TankBlockEntity mc ? mc.getTank().waterTemp() : Float.NaN;
                check(name + ": monitor screen, layout, tank name and live " + (a.length > 1 ? a[1] : "?") + " (server 2x1,auto,MPTank,true)",
                        a.length > 1 && "2x1,auto,MPTank,true".equals(a[1]));
                check(name + ": monitor temperature " + (a.length > 2 ? a[2] : "?") + " (server " + f(temp) + ")",
                        a.length > 2 && Math.abs(Float.parseFloat(a[2]) - temp) < 0.3F);
            }
            case "monitorseen" -> { // M27
                int alarms = server.overworld().getBlockEntity(monitorMin().offset(1, 0, -3)) instanceof org.overengineer.bettacraft.monitor.MonitorBlockEntity m
                        ? m.alarms() : -1;
                check(name + ": the layout from Dev1, alarm and detail " + (a.length > 1 ? a[1] : "?") + " (server 3,false,ph,alarm,detail)",
                        a.length > 1 && "3,false,ph,alarm,detail".equals(a[1]));
                check(name + ": the screen gives a redstone signal for the alarm (server alarms " + alarms + ")", alarms == 1);
            }
            case "featoff" -> check(name + ": server's tankmates off: remote, tankmates, neons in the tab, bettas in the tab " + text.substring(8)
                    + " (expected true false 0 true)", text.endsWith(" true false 0 true"));
            case "feaon" -> check(name + ": tankmates on again: remote, tankmates, neons > 0, bettas " + text.substring(6),
                    text.startsWith("feaon true true ") && !text.contains(" true true 0 ") && text.endsWith(" true"));
            // M33 part c: the server's preset reaches the clients (feature switches and the server values they read)
            case "predecor" -> check(name + ": server preset decor: remote, preset, breeding, care mode " + text.substring(9)
                    + " (expected true decor false DECOR)", text.equals("predecor true decor false DECOR"));
            case "precustom" -> check(name + ": server preset custom again: " + text.substring(10)
                    + " (expected true custom true NORMAL)", text.equals("precustom true custom true NORMAL"));
            case "error" -> fail(name + ": " + text.substring(6));
            case "travel" -> checkTravel(name, a); // M50
            case "aquarists" -> check(name + ": sees both aquarists with their employers " + (a.length > 1 ? a[1] : "?") + " (expected true,Dev1,Dev2)",
                    a.length > 1 && "true,Dev1,Dev2".equals(a[1])); // M51
            case "aquaristmenu" -> check(name + ": the menu of his own aquarist " + (a.length > 1 ? a[1] : "?"), a.length > 1 && a[1].startsWith("ok"));
            default -> { }
        }
        reports.put(name + ":" + a[0] + (a.length > 1 && java.util.Set.of("state", "aqua", "critters", "seen", "mini", "mine", "sizes").contains(a[0]) ? a[1] : "")
                + ("outlets".equals(a[0]) ? OUTLETS_EXPECTED : ""), text);
    }

    /** {@code state <phase> <water> <plants> <idsum> <cx> <cz> <fish>}: what the client sees = the server state now. */
    private static void compareState(String name, String[] a) {
        TankBlockEntity c = main();
        TankData t = c.getTank();
        long water = Long.parseLong(a[2]);
        int n = Integer.parseInt(a[3]);
        long ids = Long.parseLong(a[4]);
        double cx = Double.parseDouble(a[5]), cz = Double.parseDouble(a[6]);
        int fish = Integer.parseInt(a[7]);
        long sids = 0;
        double sx = 0, sz = 0;
        for (FloatingPlant p : t.plants()) {
            sids += p.id;
            sx += p.x;
            sz += p.z;
        }
        int sn = t.plants().size();
        if (sn > 0) {
            sx /= sn;
            sz /= sn;
        }
        int sfish = TankInhabitants.liveInhabitants((ServerLevel) c.getLevel(), t).size();
        String phase = "state " + a[1] + " " + name;
        check(phase + ": water " + water + " (server " + t.waterMb() + ")", Math.abs(water - t.waterMb()) <= TankData.EVAPORATION_BATCH_MB);
        check(phase + ": plants " + n + " (server " + sn + "), ids " + ids + " (server " + sids + ")", n == sn && ids == sids);
        check(phase + ": plant centre " + f(cx) + "/" + f(cz) + " (server " + f(sx) + "/" + f(sz) + ")", n == 0 || Math.hypot(cx - sx, cz - sz) < 0.35);
        check(phase + ": fish " + fish + " (server " + sfish + ")", fish == sfish);
    }

    /** M13 {@code aqua <phase> <plant blocks>}: the miniature on the client shows the aquarium plants of the server. */
    private static void checkAquatic(String name, String[] a) {
        int n = Integer.parseInt(a[2]);
        var r = org.overengineer.bettacraft.plants.InteriorPlants.region(server, main().getTank());
        int sn = r == null ? -1 : r.plants().blocks();
        check("aqua " + a[1] + " " + name + ": miniature shows " + n + " aquarium plant blocks (server " + sn + ")", n == sn);
        if ("2".equals(a[1])) {
            int before = Integer.parseInt(reports.getOrDefault(name + ":aqua1", "aqua 1 0").split(" ")[2]);
            check("aqua " + name + ": the plants grew in the miniature: " + before + " -> " + n + " blocks", n > before + 20);
        }
    }

    /** M14: server positions of the small animals when the clients first looked (id -> x, y, z). */
    private static final java.util.Map<Integer, double[]> critterStart = new java.util.HashMap<>();
    private static final java.util.Map<String, double[]> clientCentre = new java.util.HashMap<>();

    /**
     * M14 {@code critters <phase> <count> <idsum> <cx> <cy> <cz> <algae faces>}: the client shows the same shrimp and
     * snails as the server (count, ids, where they are) and the same algae; in phase 2 they have walked, and both
     * clients agree with each other.
     */
    private static void checkCritters(String name, String[] a) {
        TankData t = main().getTank();
        int n = Integer.parseInt(a[2]);
        long ids = Long.parseLong(a[3]);
        double cx = Double.parseDouble(a[4]), cy = Double.parseDouble(a[5]), cz = Double.parseDouble(a[6]);
        int algae = Integer.parseInt(a[7]);
        long sids = 0;
        double sx = 0, sy = 0, sz = 0;
        for (var k : t.critters()) {
            sids += k.id;
            sx += k.x;
            sy += k.y;
            sz += k.z;
        }
        int sn = t.critters().size();
        if (sn > 0) {
            sx /= sn;
            sy /= sn;
            sz /= sn;
        }
        int salgae = 0;
        for (var e : t.algae().values()) for (float v : e) if (org.overengineer.bettacraft.tank.algae.AlgaeSim.stage(v) > 0) salgae++;
        String phase = "critters " + a[1] + " " + name;
        check(phase + ": " + n + " small animals (server " + sn + "), ids " + ids + " (server " + sids + ")", n == sn && ids == sids && n > 0);
        check(phase + ": where they are " + f(cx) + "/" + f(cy) + "/" + f(cz) + " (server " + f(sx) + "/" + f(sy) + "/" + f(sz) + ")",
                Math.abs(cx - sx) + Math.abs(cy - sy) + Math.abs(cz - sz) < 0.25);
        check(phase + ": algae on " + algae + " glass faces (server " + salgae + ")", algae == salgae && algae > 0);
        clientCentre.put(a[1] + name, new double[]{cx, cy, cz});
        double[] other = clientCentre.get(a[1] + (name.equals(P1) ? P2 : P1));
        if (other != null) {
            check(phase + ": both clients see them in the same place", Math.abs(other[0] - cx) + Math.abs(other[1] - cy) + Math.abs(other[2] - cz) < 0.2);
        }
        if ("1".equals(a[1])) {
            if (critterStart.isEmpty()) for (var k : t.critters()) critterStart.put(k.id, new double[]{k.x, k.y, k.z});
        } else {
            int moved = 0;
            for (var k : t.critters()) {
                double[] s0 = critterStart.get(k.id);
                if (s0 != null && Math.abs(s0[0] - k.x) + Math.abs(s0[1] - k.y) + Math.abs(s0[2] - k.z) > 0.02) moved++;
            }
            check(phase + ": " + moved + " of " + sn + " animals walked since the first look", moved >= Math.max(1, sn / 3));
        }
    }

    // ------------------------------------------------------------------ M15: two players in one interior

    /** Interior blocks of the given kind in the L tank's region (server). */
    private static int countInRegion(net.minecraft.world.level.block.Block block) {
        var r = lRegion();
        ServerLevel in = InteriorManager.interiorLevel(server);
        if (r == null || in == null) return -1;
        int n = 0;
        for (BlockPos q : BlockPos.betweenClosed(r.toWorld(0, 0, 0), r.toWorld(r.sizeX() - 1, r.sizeY() - 1, r.sizeZ() - 1))) {
            if (in.getBlockState(q).is(block)) n++;
        }
        return n;
    }

    /** {@code seen <phase> <cobblestone> <planks>}: blocks this client sees in the interior it stands in = the server's. */
    private static void checkSeen(String name, String[] a) {
        int cob = Integer.parseInt(a[2]), pl = Integer.parseInt(a[3]);
        int sc = countInRegion(Blocks.COBBLESTONE), sp = countInRegion(Blocks.OAK_PLANKS);
        check("seen " + a[1] + " " + name + ": inside the interior " + cob + " cobblestone (from Dev1, server " + sc + ") and " + pl
                + " planks (from Dev2, server " + sp + ")", cob == sc && pl == sp && sc >= 3 && sp >= 3);
    }

    /** {@code mini <phase> <cobblestone> <planks>}: the miniature of the L tank outside shows the blocks built inside. */
    private static void checkMini(String name, String[] a) {
        int cob = Integer.parseInt(a[2]), pl = Integer.parseInt(a[3]);
        int sc = countInRegion(Blocks.COBBLESTONE), sp = countInRegion(Blocks.OAK_PLANKS);
        check("mini " + a[1] + " " + name + ": the miniature outside shows " + cob + " cobblestone (server " + sc + ") and " + pl + " planks (server "
                + sp + ")", cob == sc && pl == sp && sc > 0 && sp > 0);
    }

    /** {@code mine <phase> <tank block still there> <message names Dev1>}: breaking the L tank from outside. */
    private static void checkMine(String name, String[] a) {
        boolean still = "1".equals(a[2]), named = "1".equals(a[3]);
        TankBlockEntity lc = TankMultiblock.getController(server.overworld(), lMin.offset(0, 0, 1));
        int members = lc == null ? 0 : lc.getTank().memberCount();
        boolean there = server.overworld().getBlockState(lMin).is(ModBlocks.TANK.get());
        if ("1".equals(a[1])) {
            check("mine 1 " + name + ": breaking the L tank from outside while Dev1 is inside is refused (block there: server " + there + ", client "
                    + still + ", " + members + " tank blocks)", there && still && members == 8);
            check("mine 1 " + name + ": the refusal names the player inside", named);
        } else {
            check("mine 2 " + name + ": with nobody inside the block breaks (still there: server " + there + ", client " + still + ")", !there && !still);
        }
    }

    /** {@code motion <frames> <moved plants> <plants> <biggest step per frame> <mean distance> <backward jumps>}. */
    private static void checkMotion(String name, String[] a) {
        int frames = Integer.parseInt(a[1]), moved = Integer.parseInt(a[2]), total = Integer.parseInt(a[3]);
        double maxStep = Double.parseDouble(a[4]), mean = Double.parseDouble(a[5]);
        int jumps = Integer.parseInt(a[6]);
        check(name + ": motion over " + frames + " frames: " + moved + " of " + total + " plants moved, mean " + f(mean) + " blocks",
                frames >= 60 && total > 0 && moved >= Math.max(1, total / 3) && mean > 0.003);
        check(name + ": gliding without jumps (biggest step " + String.format(Locale.ROOT, "%.4f", maxStep) + " per frame, " + jumps + " jumps)",
                maxStep < 0.05 && jumps == 0);
    }

    /** M22.4: the outlet sides of the two cabinets the clients must report ("left,front", then "right,front"). */
    private static String OUTLETS_EXPECTED = "left,front";

    private static void check(String what, boolean ok) {
        if (ok) {
            passes.add(what);
            LOGGER.info("[smoketest] MP ok: {}", what);
        } else {
            fail(what);
        }
    }

    private static void fail(String what) {
        failures.add(what);
        LOGGER.error("[smoketest] MP check failed: {}", what);
    }

    private static String f(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }

    // ------------------------------------------------------------------ the stage

    /** M19: the small tank with the tankmates (west of the main tank, in the cleared area). */
    private static BlockPos tankmatesMin() {
        return base.offset(-5, 0, 6);
    }

    /** {@code tankmates <count> <albinos> <clutches> <eggs>}: the tankmates and clutches of the small tank on the client = server. */
    private static void checkTankmates(String name, String[] a) {
        TankBlockEntity c = TankMultiblock.getController(server.overworld(), tankmatesMin());
        if (c == null) {
            fail(name + ": tankmate tank gone");
            return;
        }
        var mates = org.overengineer.bettacraft.tankmates.TankmateBreeding.tankmates(server.overworld(), c.getTank());
        long albino = mates.stream().filter(m -> "albino".equals(m.variety())).count();
        int eggs = c.getTank().clutches().stream().mapToInt(k -> k.count).sum();
        check(name + ": " + a[1] + " tankmates (server " + mates.size() + ")", Integer.parseInt(a[1]) == mates.size() && mates.size() == 7);
        // M21.6: the guppy strains the client sees (from the synced genome) = the server's
        java.util.List<String> strains = new java.util.ArrayList<>();
        for (var m : mates) {
            var ph = m.livebearerPhenotype();
            if (ph == null) continue;
            var st = org.overengineer.bettacraft.genetics.livebearer.LivebearerStrain.of(ph);
            strains.add(st == null ? "none" : st.id());
        }
        java.util.Collections.sort(strains);
        String want = String.join(",", strains);
        check(name + ": guppy strains " + (a.length > 5 ? a[5] : "?") + " (server " + want + ")", a.length > 5 && a[5].equals(want)
                && want.equals("full_gold,tuxedo_koi"));
        check(name + ": " + a[2] + " albino bristlenose (server " + albino + ")", Integer.parseInt(a[2]) == albino && albino == 1);
        check(name + ": " + a[3] + " clutches with " + a[4] + " eggs (server " + c.getTank().clutches().size() + ", " + eggs + ")",
                Integer.parseInt(a[3]) == c.getTank().clutches().size() && Integer.parseInt(a[4]) == eggs && eggs > 0);
    }

    /** M20: the small reef tank (west of the tankmate tank). */
    private static BlockPos reefMin() {
        return base.offset(-9, 0, 6);
    }

    /** {@code reef <salinity x10> <salt drawn> <reef fish> <coral blocks>}: salinity, water colour, clownfish and coral blocks = server. */
    private static void checkReef(String name, String[] a) {
        TankBlockEntity c = TankMultiblock.getController(server.overworld(), reefMin());
        if (c == null) {
            fail(name + ": reef tank gone");
            return;
        }
        int salt10 = Math.round(c.getTank().salinity() * 10);
        long fish = org.overengineer.bettacraft.tankmates.TankmateBreeding.tankmates(server.overworld(), c.getTank()).size();
        var r = org.overengineer.bettacraft.tank.interior.InteriorRegions.get(server).get(c.getTank().regionId());
        int coral = 0;
        if (r != null) for (var p : r.plants().all()) if (p.species.marine()) coral += p.blocks;
        check(name + ": salinity " + a[1] + "/10 ppt (server " + salt10 + ")", Integer.parseInt(a[1]) == salt10 && salt10 >= 340);
        check(name + ": water drawn with " + a[2] + " ppt", Integer.parseInt(a[2]) == Math.round(c.getTank().salinity()));
        check(name + ": " + a[3] + " clownfish (server " + fish + ")", Integer.parseInt(a[3]) == fish && fish >= 2);
        check(name + ": " + a[4] + " coral blocks in the miniature (server " + coral + ")", Integer.parseInt(a[4]) == coral && coral > 1);
    }

    /** M22.1: the pool (8 deep, glass walls) and the stone pad of the container steps, west of the seahorse tank. */
    private static BlockPos poolMin() {
        return base.offset(-24, 0, 6);
    }

    /** M22.1: Dev1 stands over {@code at} (the block below its feet + 1), looking straight down. */
    private static void overBlock(ServerPlayer p, BlockPos at) {
        p.resetFallDistance();
        p.teleportTo(server.overworld(), at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, java.util.Set.of(), 0F, 90F, true);
    }

    /**
     * M22.1 {@code containers <what> <hand item> <block at the spot> <bettas near> <screen>} from Dev1 after each use: the
     * client sees what the server did (no ghost water, no ghost item, no ghost animal).
     */
    private static void checkContainers(String name, String[] a) {
        if (a.length < 6) {
            fail(name + ": short container report");
            return;
        }
        ServerPlayer p = player(name);
        String hand = p == null ? "?" : net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).toString();
        check(name + ": " + a[1] + ": the item in the hand " + a[2] + " (server " + hand + ")", hand.equals(a[2]));
        switch (a[1]) {
            case "sea" -> {
                check(name + ": the sea water bucket emptied into the deep water (" + a[2] + ")", "minecraft:bucket".equals(a[2]));
                check(name + ": the pool is still water on the client (" + a[3] + ")", "water".equals(a[3]));
            }
            case "ask" -> {
                check(name + ": the confirm screen opened (" + a[5] + ")", "screen".equals(a[5]));
                check(name + ": the betta bucket still full while asking (" + a[2] + ")", "bettacraft:betta_bucket".equals(a[2]));
                check(name + ": no water on the pad before the confirmation (" + a[3] + ")", "air".equals(a[3]));
                check(name + ": no betta before the confirmation (" + a[4] + ")", "0".equals(a[4]));
            }
            case "release" -> {
                int bettas = p == null ? -1 : p.level().getEntitiesOfClass(org.overengineer.bettacraft.entity.betta.BettaEntity.class,
                        new net.minecraft.world.phys.AABB(poolMin().offset(8, 1, 1)).inflate(3)).size();
                check(name + ": the confirmed betta bucket is empty (" + a[2] + ")", "minecraft:bucket".equals(a[2]));
                check(name + ": the water of the bucket on the pad on the client (" + a[3] + ")", "water".equals(a[3]));
                check(name + ": " + a[4] + " betta let go on the client (server " + bettas + ")", a[4].equals(String.valueOf(bettas)) && bettas == 1);
            }
            case "pour" -> {
                check(name + ": the bottle poured out (" + a[2] + ")", "minecraft:glass_bottle".equals(a[2]));
                check(name + ": no water block from the bottle on the client (" + a[3] + ")", "air".equals(a[3]));
            }
            default -> fail(name + ": unknown container report " + a[1]);
        }
    }

    /** M21.8: the small seahorse tank (west of the cabinets). */
    private static BlockPos seahorseMin() {
        return base.offset(-16, 0, 6);
    }

    /**
     * M21.8 {@code seahorses <seahorses> <full pouches> <stack size> <slot 0 count> <slot 0 damaged> <slot 1 count> <slot 1 damaged>}:
     * the clients see both seahorses and the full brood pouch of the male; M35.1: cyst jars stack to 64 on the clients too (no
     * config sync any more), and a stack of 16 old jars (loaded with the old damage) arrives as 16 plain jars.
     */
    private static void checkSeahorses(String name, String[] a) {
        if (a.length < 8) {
            fail(name + ": short seahorse report");
            return;
        }
        check(name + ": " + a[1] + " seahorses (server 2)", "2".equals(a[1]));
        check(name + ": " + a[2] + " full brood pouch (server 1, the male)", "1".equals(a[2]));
        check(name + ": cyst jars per stack " + a[3] + " (64)", "64".equals(a[3]));
        check(name + ": a stack of " + a[4] + " jars, damaged " + a[5] + " (server 64, none)", "64".equals(a[4]) && "0".equals(a[5]));
        check(name + ": " + a[6] + " old jars, damaged " + a[7] + " (server 16 loaded from an old save, none)", "16".equals(a[6]) && "0".equals(a[7]));
    }

    private static TankBlockEntity main() {
        TankBlockEntity c = TankMultiblock.getController(server.overworld(), mainMin);
        if (c == null) throw new IllegalStateException("MP: main tank gone");
        return c;
    }

    private static void build(ServerLevel level) {
        base = level.getRespawnData().pos().offset(0, 60, -48);
        mainMin = base;
        lMin = base.offset(7, 0, 0);
        for (int cx = (base.getX() - 8) >> 4; cx <= (base.getX() + 14) >> 4; cx++)
            for (int cz = (base.getZ() - 8) >> 4; cz <= (base.getZ() + 14) >> 4; cz++) level.setChunkForced(cx, cz, true);
        AABB box = new AABB(base.offset(-6, -1, -6).getCenter(), base.offset(12, 8, 12).getCenter()).inflate(1);
        // leftovers of an earlier run (the dev world persists): animals, dropped plants, blocks
        for (Entity e : level.getEntitiesOfClass(Entity.class, box, e -> !(e instanceof ServerPlayer))) e.discard();
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-6, 0, -6), base.offset(12, 8, 12))) {
            if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        }
        for (Entity e : level.getEntitiesOfClass(ItemEntity.class, box)) e.discard();
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-6, -1, -6), base.offset(12, -1, 12))) level.setBlockAndUpdate(p, Blocks.SMOOTH_STONE.defaultBlockState());
        // a ledge for Dev2, looking down at the surface from the south
        for (int x = 1; x <= 3; x++) for (int z = 6; z <= 7; z++) level.setBlockAndUpdate(base.offset(x, 2, z), Blocks.SMOOTH_STONE.defaultBlockState());
        // main tank 4x3x3, 3/4 full
        List<BlockPos> blocks = new ArrayList<>();
        for (int x = 0; x < 4; x++) for (int y = 0; y < 3; y++) for (int z = 0; z < 3; z++) blocks.add(mainMin.offset(x, y, z));
        for (BlockPos p : blocks) level.setBlockAndUpdate(p, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos p : blocks) TankMultiblock.onTankBlockAdded(level, p, null);
        TankBlockEntity c = TankMultiblock.getController(level, mainMin);
        if (c == null) throw new IllegalStateException("MP: main tank did not form");
        TankData t = c.getTank();
        TankInteractions.changeWater(level, c, 27000 - t.waterMb());
        t.setFront(Direction.NORTH);
        t.setName("MP test");
        t.setSignVisible(false);
        // lit tank light along the middle row, external filter on the west wall (its jet pushes the plants)
        for (int x = 0; x < 4; x++) {
            level.setBlockAndUpdate(mainMin.offset(x, 3, 1), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 3).setValue(TankLightBlock.LIT, true));
        }
        level.setBlockAndUpdate(mainMin.offset(-1, 1, 1), ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, Direction.EAST));
        TankEquipment.invalidate(c);
        // two bettas
        for (int i = 0; i < 2; i++) {
            BettaData d = BettaData.create(BettaGenome.randomDomestic(net.minecraft.util.RandomSource.create(77 + i)), i == 1, level.getGameTime());
            Component err = TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d),
                    ModEntities.BETTA.get(), null, false);
            if (err != null) throw new IllegalStateException("MP: insert failed: " + err.getString());
        }
        // plants: a frogbit right behind the front glass where Dev1 looks, the rest spread out
        double x0 = mainMin.getX(), z0 = mainMin.getZ();
        TankPlants.add(c, FloatingPlantType.FROGBIT, x0 + 2.0, z0 + 0.45, 1F, 0F);
        var r = net.minecraft.util.RandomSource.create(4242);
        FloatingPlantType[] types = {FloatingPlantType.DUCKWEED, FloatingPlantType.SALVINIA, FloatingPlantType.FROGBIT, FloatingPlantType.RED_ROOT_FLOATER};
        for (int i = 0; i < 22; i++) {
            TankPlants.add(c, types[i % 4], x0 + 0.4 + r.nextDouble() * 3.2, z0 + 1.0 + r.nextDouble() * 1.7, 0.5F + r.nextFloat() * 0.5F, r.nextFloat() * 6.28F);
        }
        TankPlants.add(c, FloatingPlantType.LILY_PAD, x0 + 3.4, z0 + 2.4, 1F, 0.5F);
        c.markTankChanged();
        // an L-shaped tank beside it (free shape), 3x1x3 without the north-east corner
        List<BlockPos> l = new ArrayList<>();
        for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) if (!(x == 2 && z == 0)) l.add(lMin.offset(x, 0, z));
        for (BlockPos p : l) level.setBlockAndUpdate(p, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos p : l) TankMultiblock.onTankBlockAdded(level, p, null);
        TankBlockEntity lc = TankMultiblock.getController(level, lMin);
        if (lc == null || lc.getTank().memberCount() != 8) throw new IllegalStateException("MP: L tank did not form");
        TankInteractions.changeWater(level, lc, 7000 - lc.getTank().waterMb());
        for (int i = 0; i < 6; i++) TankPlants.add(lc, FloatingPlantType.DUCKWEED, lMin.getX() + 0.5 + (i % 3) * 0.6, lMin.getZ() + 1.5 + (i / 3) * 0.8, 0.6F, i);
        // a bright, quiet stage
        org.overengineer.bettacraft.gametest.framework.DevTime.set(level, 6000);
        level.getGameRules().set(GameRules.ADVANCE_TIME, false, server);
        level.getGameRules().set(GameRules.ADVANCE_WEATHER, false, server);
        level.getGameRules().set(GameRules.SPAWN_MOBS, false, server);
        level.getServer().setWeatherParameters(6000, 0, false, false);
        server.setDifficulty(Difficulty.PEACEFUL, true);
        savedEvaporation = ServerConfig.getOr(ServerConfig.EVAPORATION_ENABLED, true);
        LOGGER.info("[smoketest] MP stage at {}: main tank {} blocks, {} mB, {} plants, {} animals; L tank {} blocks, {} plants", base,
                t.memberCount(), t.waterMb(), t.plants().size(), TankInhabitants.liveInhabitants(level, t).size(),
                lc.getTank().memberCount(), lc.getTank().plants().size());
    }

    @javax.annotation.Nullable
    private static org.overengineer.bettacraft.tank.interior.InteriorRegion lRegion() {
        TankBlockEntity lc = TankMultiblock.getController(server.overworld(), lMin.offset(0, 0, 1));
        return lc == null ? null : org.overengineer.bettacraft.tank.interior.InteriorRegions.get(server).get(lc.getTank().regionId());
    }

    private static String lArg() {
        return lMin.getX() + "," + lMin.getY() + "," + (lMin.getZ() + 1);
    }

    private static boolean inInterior(String name) {
        ServerPlayer p = player(name);
        return p != null && InteriorManager.isInterior(p.level());
    }

    /** Puts a player in the interior at (px, pz) on the floor, turned so the crosshair hits (tx, topY, tz). */
    private static void aim(String name, double px, double pz, double tx, double topY, double tz) {
        ServerPlayer p = player(name);
        ServerLevel in = InteriorManager.interiorLevel(server);
        if (p == null || in == null) return;
        int base = org.overengineer.bettacraft.tank.interior.InteriorRegion.BASE_Y;
        double eye = base + 1.62;
        double dx = tx - px, dz = tz - pz, d = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(Math.atan2(eye - topY, d));
        p.teleportTo(in, px, base, pz, java.util.Set.of(), yaw, pitch, true);
    }

    /** Dev1 in front of the north glass, eye a little below the water surface, looking south at the tank. */
    private static void placeDev1(ServerPlayer p) {
        p.teleportTo(server.overworld(), mainMin.getX() + 2.0, mainMin.getY(), mainMin.getZ() - 2.0, java.util.Set.of(), 0F, 0F, true);
    }

    /** Dev2 on the ledge south of the tank, looking down at the surface. */
    private static void placeDev2(ServerPlayer p) {
        p.teleportTo(server.overworld(), mainMin.getX() + 2.0, mainMin.getY() + 3.0, mainMin.getZ() + 6.6, java.util.Set.of(), 180F, 32F, true);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!ENABLED || base == null || !(e.getEntity() instanceof ServerPlayer p)) return;
        // M22.3: a player who died in an earlier run comes back alive; the test players take no damage (a fall after a
        // teleport under lag killed Dev1 once and left it on the death screen for the next run)
        if (p.isDeadOrDying()) p = server.getPlayerList().respawn(p, false, net.minecraft.world.entity.Entity.RemovalReason.KILLED);
        p.setGameMode(GameType.SURVIVAL);
        p.getAbilities().invulnerable = true;
        p.onUpdateAbilities();
        p.setInvulnerable(true);
        p.setHealth(p.getMaxHealth());
        p.getInventory().clearContent();
        if (P1.equals(p.getGameProfile().name())) placeDev1(p);
        else placeDev2(p);
        p.sendSystemMessage(Component.literal(PREFIX + "tank:all:" + mainMin.getX() + "," + mainMin.getY() + "," + mainMin.getZ()));
        LOGGER.info("[smoketest] MP {} joined", p.getGameProfile().name());
    }

    // ------------------------------------------------------------------ the scenario

    private static BlockPos monitorMin() {
        return base.offset(-34, 0, 12);
    }

    private static ServerPlayer player(String name) {
        return server.getPlayerList().getPlayerByName(name);
    }

    /** M44: a new land goal for the zombie in glass of the M31 terrarium (it walks while the clients watch its legs). */
    private static void walkGlassZombie(ServerLevel level, BlockPos m31) {
        for (var z : level.getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Zombie.class, new net.minecraft.world.phys.AABB(m31).inflate(8))) {
            if (org.overengineer.bettacraft.registry.ModAttachments.GLASS_STATE.has(z)) {
                var w = org.overengineer.bettacraft.registry.ModAttachments.GLASS_STATE.get(z).walker();
                if (w != null) w.forceActivity(org.overengineer.bettacraft.terrarium.LandWalker.Activity.LAND);
            }
        }
    }

    private static void say(String command, String target, String args) {
        server.getPlayerList().broadcastSystemMessage(Component.literal(PREFIX + command + ":" + target + ":" + args), false);
    }

    private static int growing(TankData t) {
        return (int) t.plants().stream().filter(p -> p.type.grows()).count();
    }

    private static String tankArg() {
        return mainMin.getX() + "," + mainMin.getY() + "," + mainMin.getZ();
    }

    private static void hand(String name, ItemStack stack) {
        ServerPlayer p = player(name);
        if (p != null) p.getInventory().setItem(p.getInventory().getSelectedSlot(), stack);
    }

    private static int count(ServerPlayer p, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).is(item)) n += p.getInventory().getItem(i).getCount();
        return n;
    }

    private static void plan() {
        steps.add(new Step("players", () -> LOGGER.info("[smoketest] MP waiting for {} and {}", P1, P2),
                () -> reports.containsKey(P1 + ":ready") && reports.containsKey(P2 + ":ready"), 20 * 60 * 4));
        steps.add(new Step("first look", () -> say("state", "all", "1," + tankArg()),
                () -> reports.containsKey(P1 + ":state1") && reports.containsKey(P2 + ":state1"), 20 * 30));
        steps.add(new Step("water bucket", () -> {
            placeDev1(player(P1));
            waterBefore = main().getTank().waterMb();
            hand(P1, new ItemStack(Items.WATER_BUCKET));
            say("use", P1, "normal");
        }, () -> main().getTank().waterMb() >= waterBefore + 990, 20 * 15));
        steps.add(new Step("net from the side", () -> {
            check("water bucket: " + waterBefore + " -> " + main().getTank().waterMb() + " mB", main().getTank().waterMb() >= waterBefore + 990);
            ServerPlayer p = player(P1);
            check("Dev1 got the empty bucket back", p != null && count(p, Items.BUCKET) == 1);
            plantsBefore = main().getTank().plants().size();
            hand(P1, new ItemStack(ModItems.AQUARIUM_NET.get()));
            say("use", P1, "normal");
        }, () -> main().getTank().plants().size() < plantsBefore, 20 * 15));
        steps.add(new Step("net + sneak", () -> {
            int n = main().getTank().plants().size();
            ServerPlayer p = player(P1);
            int items = 0;
            if (p != null) for (var e : ModItems.FLOATING_PLANTS.values()) items += count(p, e.get());
            check("net from the side took the plant(s) behind the glass: " + plantsBefore + " -> " + n + " plants, Dev1 got " + items + " plant item(s)",
                    n < plantsBefore && items == plantsBefore - n);
            growingBefore = growing(main().getTank());
            say("use", P1, "sneak");
        }, () -> growing(main().getTank()) < growingBefore, 20 * 15));
        steps.add(new Step("menu", () -> {
            int g = growing(main().getTank());
            check("net + sneak thinned out by half: " + growingBefore + " -> " + g, g == growingBefore / 2);
            growingBefore = g;
            hand(P1, ItemStack.EMPTY);
            say("menu", P1, tankArg());
        }, () -> reports.containsKey(P1 + ":menu"), 20 * 40));
        steps.add(new Step("more plants", () -> {
            int g = growing(main().getTank());
            check("menu button \"Thin out\" thinned out by half: " + growingBefore + " -> " + g, g == growingBefore / 2);
            ServerPlayer p = player(P1);
            check("the menu was open on the server", p != null && reports.getOrDefault(P1 + ":menu", "").contains("ok"));
            if (p != null && p.containerMenu instanceof TankMenu) p.closeContainer();
            // new plants near the filter jet: both clients must get them, and they drift away from the jet
            TankBlockEntity c = main();
            for (int i = 0; i < 8; i++) {
                TankPlants.add(c, i % 2 == 0 ? FloatingPlantType.SALVINIA : FloatingPlantType.DUCKWEED, mainMin.getX() + 0.5 + (i % 4) * 0.25,
                        mainMin.getZ() + 1.2 + (i / 4) * 0.5, 0.7F, i);
            }
            c.markTankChanged();
            TankPlants.sync(server.overworld(), c);
        }, () -> stepTicks >= 60, 100));
        steps.add(new Step("second look", () -> say("state", "all", "2," + tankArg()),
                () -> reports.containsKey(P1 + ":state2") && reports.containsKey(P2 + ":state2"), 20 * 30));
        steps.add(new Step("motion", () -> say("motion", "all", "160," + tankArg()),
                () -> reports.containsKey(P1 + ":motion") && reports.containsKey(P2 + ":motion"), 20 * 90)); // 480 frames: slow background windows need ~50 s
        // M13: aquarium plants in the interior grow while nobody is inside; both miniatures follow
        steps.add(new Step("aquarium plants", () -> {
            TankBlockEntity c = main();
            var r = InteriorManager.ensureRegion(server.overworld(), c);
            ServerLevel in = InteriorManager.interiorLevel(server);
            for (var pl : new ArrayList<>(r.plants().all())) org.overengineer.bettacraft.plants.InteriorPlants.remove(in, r, pl, false);
            org.overengineer.bettacraft.plants.InteriorPlants.plantAt(in, r, org.overengineer.bettacraft.plants.PlantSpecies.ROTALA, 34, 0, 20, 0, 21L);
            org.overengineer.bettacraft.plants.InteriorPlants.plantAt(in, r, org.overengineer.bettacraft.plants.PlantSpecies.AMAZON_SWORD, 20, 0, 24, 0, 22L);
            org.overengineer.bettacraft.plants.InteriorPlants.plantAt(in, r, org.overengineer.bettacraft.plants.PlantSpecies.VALLISNERIA, 50, 0, 30, 0, 23L);
        }, () -> stepTicks >= 80, 100));
        steps.add(new Step("cuttings seen", () -> say("aqua", "all", "1," + tankArg()),
                () -> reports.containsKey(P1 + ":aqua1") && reports.containsKey(P2 + ":aqua1"), 20 * 30));
        // M62: the plants grow only once the light at the surface is there (under load the sky light of the fresh stage
        // lagged behind on 26.2 / 26.3: light 0, no plant grew, "3 -> 3 blocks")
        final int[] grownAt = {-1};
        steps.add(new Step("plants grow", () -> grownAt[0] = -1, () -> {
            if (grownAt[0] < 0) {
                TankBlockEntity c = main();
                double light = org.overengineer.bettacraft.tank.plants.TankPlants.strongLight(server.overworld(), c);
                if (light < 12 && stepTicks < 200) return false;
                var r = org.overengineer.bettacraft.plants.InteriorPlants.region(server, c.getTank());
                for (int i = 0; i < 8; i++) org.overengineer.bettacraft.plants.InteriorPlants.grow(server.overworld(), c, r, 0.5);
                LOGGER.info("[smoketest] MP aquarium plants grown at light {} after {} ticks: {}", light, stepTicks, r.plants().all());
                grownAt[0] = stepTicks;
            }
            return stepTicks >= grownAt[0] + 60;
        }, 300));
        steps.add(new Step("grown plants seen", () -> say("aqua", "all", "2," + tankArg()),
                () -> reports.containsKey(P1 + ":aqua2") && reports.containsKey(P2 + ":aqua2"), 20 * 30));
        // M14: shrimp, snails and algae in the stage tank: both clients see the same animals in the same places, walking
        steps.add(new Step("small animals", () -> {
            TankBlockEntity c = main();
            TankData t = c.getTank();
            t.critters().clear();
            t.critterObjects().clear();
            ServerLevel lv = server.overworld();
            for (int i = 0; i < 6; i++) {
                org.overengineer.bettacraft.critters.TankCritters.add(lv, c, org.overengineer.bettacraft.critters.CritterSpecies.NEOCARIDINA,
                        org.overengineer.bettacraft.critters.CritterGenes.pure(i % 3, 0.5F), i % 2 == 0, 5);
            }
            org.overengineer.bettacraft.critters.TankCritters.add(lv, c, org.overengineer.bettacraft.critters.CritterSpecies.ZEBRA_NERITE,
                    org.overengineer.bettacraft.critters.CritterGenes.WILD, true, 5);
            org.overengineer.bettacraft.critters.TankCritters.add(lv, c, org.overengineer.bettacraft.critters.CritterSpecies.APPLE_SNAIL,
                    org.overengineer.bettacraft.critters.CritterGenes.pure(0, 0.4F), true, 6);
            org.overengineer.bettacraft.critters.TankCritters.add(lv, c, org.overengineer.bettacraft.critters.CritterSpecies.DWARF_CRAYFISH,
                    org.overengineer.bettacraft.critters.CritterGenes.pure(0, 0.5F), false, 6);
            int i = 0;
            for (var face : org.overengineer.bettacraft.tank.algae.TankAlgae.wetFaces(t)) {
                if (i++ % 2 == 0) org.overengineer.bettacraft.tank.algae.AlgaeSim.set(t.algae(), face.pos(), face.dir(), 0.7F);
            }
            org.overengineer.bettacraft.tank.algae.TankAlgae.syncIfChanged(c);
            c.markTankChanged();
            org.overengineer.bettacraft.critters.TankCritters.sync(lv, c);
            critterStart.clear();
        }, () -> stepTicks >= 60, 100));
        steps.add(new Step("animals seen", () -> say("critters", "all", "1," + tankArg()),
                () -> reports.containsKey(P1 + ":critters1") && reports.containsKey(P2 + ":critters1"), 20 * 30));
        steps.add(new Step("animals walk", () -> { }, () -> stepTicks >= 160, 200));
        steps.add(new Step("animals seen again", () -> say("critters", "all", "2," + tankArg()),
                () -> reports.containsKey(P1 + ":critters2") && reports.containsKey(P2 + ":critters2"), 20 * 30));
        // M15: Dev1 and Dev2 in the interior of the L tank at the same time: they build, each sees the other's blocks, the
        // miniature outside follows, nobody can break the tank meanwhile, and both come out in front of it
        steps.add(new Step("enter the L tank", () -> {
            entered.clear();
            for (String n : List.of(P1, P2)) {
                ServerPlayer p = player(n);
                if (p == null) continue;
                p.teleportTo(server.overworld(), lMin.getX() + (n.equals(P1) ? 1.5 : 0.5), lMin.getY(), lMin.getZ() - 1.5, java.util.Set.of(), 0F, 20F, true);
                entered.put(n, p.position());
                InteriorManager.enter(p, lMin.offset(0, 0, 1));
            }
        }, () -> inInterior(P1) && inInterior(P2), 20 * 30));
        for (int round = 0; round < 3; round++) {
            final int k = round;
            steps.add(new Step("build together " + (round + 1), () -> {
                var r = lRegion();
                if (k == 0) {
                    check("both players are in the L tank's interior", r != null && inInterior(P1) && inInterior(P2));
                    hand(P1, new ItemStack(Items.COBBLESTONE, 8));
                    hand(P2, new ItemStack(Items.OAK_PLANKS, 8));
                    cobbleBefore = countInRegion(Blocks.COBBLESTONE);
                }
                if (r == null) return;
                int base = org.overengineer.bettacraft.tank.interior.InteriorRegion.BASE_Y;
                // farthest first, so the crosshair passes over the blocks already there
                double tx = r.originX() + 11.5 - k;
                aim(P1, r.originX() + 8.5, r.originZ() + 20.5, tx, base, r.originZ() + 20.5);
                aim(P2, r.originX() + 8.5, r.originZ() + 40.5, tx, base, r.originZ() + 40.5);
                say("place", "all", "");
            }, () -> stepTicks >= 50, 80));
        }
        steps.add(new Step("both see both", () -> {
            int c = countInRegion(Blocks.COBBLESTONE), pl = countInRegion(Blocks.OAK_PLANKS);
            check("built at the same time: Dev1 placed " + (c - Math.max(0, cobbleBefore)) + " cobblestone, Dev2 " + pl + " planks",
                    c - Math.max(0, cobbleBefore) == 3 && pl == 3);
            say("seen", "all", "1");
        }, () -> reports.containsKey(P1 + ":seen1") && reports.containsKey(P2 + ":seen1"), 20 * 30));
        steps.add(new Step("Dev2 sneaks looking down", () -> say("sneak", P2, "60,100"),
                () -> reports.containsKey(P2 + ":sneak"), 20 * 20));
        steps.add(new Step("Dev2 leaves", () -> {
            check("sneaking while looking at the floor does not take Dev2 out", inInterior(P2));
            say("sneak", P2, "-80,110");
        }, () -> !inInterior(P2), 20 * 20));
        steps.add(new Step("Dev2 sees the miniature", () -> {
            ServerPlayer p = player(P2);
            var back = entered.get(P2);
            check("Dev2 is back in front of the tank (" + (p == null ? "?" : p.position()) + ", entered at " + back + ")",
                    p != null && back != null && p.level() == server.overworld() && p.position().distanceTo(back) < 0.6);
            say("mini", P2, "1," + lArg());
        }, () -> reports.containsKey(P2 + ":mini1"), 20 * 30));
        steps.add(new Step("Dev1 builds on", () -> {
            var r = lRegion();
            if (r == null) return;
            int base = org.overengineer.bettacraft.tank.interior.InteriorRegion.BASE_Y;
            // on top of the farthest cobblestone
            aim(P1, r.originX() + 8.5, r.originZ() + 20.5, r.originX() + 11.5, base + 1, r.originZ() + 20.5);
            say("place", P1, "");
        }, () -> stepTicks >= 50, 80));
        steps.add(new Step("miniature follows", () -> say("mini", P2, "2," + lArg()), () -> reports.containsKey(P2 + ":mini2"), 20 * 30));
        steps.add(new Step("break from outside", () -> {
            int c = countInRegion(Blocks.COBBLESTONE) - Math.max(0, cobbleBefore);
            check("Dev1 built on alone: " + c + " cobblestone", c == 4);
            ServerPlayer p = player(P2);
            if (p != null) {
                hand(P2, ItemStack.EMPTY);
                // in front of the north-west block of the L, looking at its north face
                p.teleportTo(server.overworld(), lMin.getX() + 0.5, lMin.getY(), lMin.getZ() - 1.5, java.util.Set.of(), 0F, 36F, true);
            }
            say("mine", P2, "1," + lMin.getX() + "," + lMin.getY() + "," + lMin.getZ());
        }, () -> reports.containsKey(P2 + ":mine1"), 20 * 30));
        steps.add(new Step("Dev1 leaves", () -> say("sneak", P1, "-80,110"), () -> !inInterior(P1), 20 * 20));
        steps.add(new Step("both out", () -> {
            ServerPlayer p = player(P1);
            var back = entered.get(P1);
            check("Dev1 is back in front of the tank (" + (p == null ? "?" : p.position()) + ", entered at " + back + ")",
                    p != null && back != null && p.level() == server.overworld() && p.position().distanceTo(back) < 0.6);
            TankBlockEntity lc = TankMultiblock.getController(server.overworld(), lMin.offset(0, 0, 1));
            check("nobody is in the L tank any more", lc != null && InteriorManager.playersInside(server, lc.getTank()).isEmpty());
            hand(P1, ItemStack.EMPTY);
        }, () -> stepTicks >= 30, 60));
        steps.add(new Step("break when empty", () -> say("mine", P2, "2," + lMin.getX() + "," + lMin.getY() + "," + lMin.getZ()),
                () -> reports.containsKey(P2 + ":mine2"), 20 * 30));
        steps.add(new Step("dye the light", () -> {
            // M18: one dye on the bar on the server; both clients see the colour (block state) and tint the water
            BlockPos l = mainMin.offset(1, 3, 1);
            int n = TankLightBlock.dyeBar(server.overworld(), l, server.overworld().getBlockState(l), net.minecraft.world.item.DyeColor.RED, false);
            check("the whole bar of 4 lights was dyed red (" + n + ")", n == 4);
            BlockPos c = main().getBlockPos();
            say("light", "all", l.getX() + "," + l.getY() + "," + l.getZ() + ";" + c.getX() + "," + c.getY() + "," + c.getZ());
        }, () -> reports.containsKey(P1 + ":light") && reports.containsKey(P2 + ":light"), 20 * 30));
        steps.add(new Step("tankmates", () -> {
            // M19: a small tank with four neons, an albino bristlenose and a clutch of eggs on the glass: both clients see them
            BlockPos min = tankmatesMin();
            ServerLevel level = server.overworld();
            List<BlockPos> blocks = new ArrayList<>();
            for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) blocks.add(min.offset(x, y, z));
            for (BlockPos q : blocks) level.setBlockAndUpdate(q, ModBlocks.TANK.get().defaultBlockState());
            for (BlockPos q : blocks) TankMultiblock.onTankBlockAdded(level, q, null);
            TankBlockEntity c = TankMultiblock.getController(level, min);
            check("the tankmate tank formed", c != null);
            if (c == null) return;
            TankInteractions.changeWater(level, c, 8000 - c.getTank().waterMb());
            c.getTank().setWaterTemp(26F);
            c.getTank().setTargetTemp(26F);
            var w = new org.overengineer.bettacraft.tank.TankWaterVolume(c.getTank());
            var sp = org.overengineer.bettacraft.tankmates.TankmateSpecies.NEON_TETRA;
            for (int i = 0; i < 4; i++) {
                org.overengineer.bettacraft.tankmates.TankmateBreeding.spawn(level, c, sp, org.overengineer.bettacraft.tankmates.TankmateGenes.wild(sp), i % 2 == 0,
                        org.overengineer.bettacraft.entity.betta.LifeStage.ADULT, 0, w.box().getCenter().add(i * 0.2 - 0.3, -0.3, 0));
            }
            var pl = org.overengineer.bettacraft.tankmates.TankmateSpecies.BRISTLENOSE_PLECO;
            org.overengineer.bettacraft.tankmates.TankmateBreeding.spawn(level, c, pl, org.overengineer.bettacraft.tankmates.TankmateGenes.pure(pl, "albino"), false,
                    org.overengineer.bettacraft.entity.betta.LifeStage.ADULT, 0, w.box().getCenter().add(0, -0.5, 0.2));
            // M21.6: two guppies with their genes (the clients must see the strains)
            var gp = org.overengineer.bettacraft.tankmates.TankmateSpecies.GUPPY;
            org.overengineer.bettacraft.tankmates.TankmateBreeding.spawn(level, c, gp, org.overengineer.bettacraft.tankmates.TankmateGenes.strain(gp,
                    org.overengineer.bettacraft.genetics.livebearer.LivebearerStrain.TUXEDO_KOI), false, org.overengineer.bettacraft.entity.betta.LifeStage.ADULT, 0,
                    w.box().getCenter().add(0.2, 0.1, 0));
            org.overengineer.bettacraft.tankmates.TankmateBreeding.spawn(level, c, gp, org.overengineer.bettacraft.tankmates.TankmateGenes.strain(gp,
                    org.overengineer.bettacraft.genetics.livebearer.LivebearerStrain.FULL_GOLD), true, org.overengineer.bettacraft.entity.betta.LifeStage.ADULT, 0,
                    w.box().getCenter().add(-0.2, 0.1, 0));
            var k = new org.overengineer.bettacraft.tankmates.Clutch(c.getTank().nextClutchId(), org.overengineer.bettacraft.tankmates.TankmateSpecies.PANDA_CORY,
                    org.overengineer.bettacraft.tankmates.Clutch.Kind.GLASS, min.getX() + 1.0, min.getY() + 0.5, min.getZ() + 1.0 / 16 + 0.004, 10,
                    org.overengineer.bettacraft.tankmates.TankmateGenes.wild(org.overengineer.bettacraft.tankmates.TankmateSpecies.PANDA_CORY),
                    org.overengineer.bettacraft.tankmates.TankmateGenes.wild(org.overengineer.bettacraft.tankmates.TankmateSpecies.PANDA_CORY));
            k.face = 0;
            k.cover = 1.0;
            c.getTank().clutches().add(k);
            c.markTankChanged();
            say("tankmates", "all", min.getX() + "," + min.getY() + "," + min.getZ());
        }, () -> reports.containsKey(P1 + ":tankmates") && reports.containsKey(P2 + ":tankmates"), 20 * 30));
        steps.add(new Step("m29", () -> {
            // M29 part d: a cool tank with a blue Florida crayfish, a marbled crayfish, a blue and a gold axolotl and a betta
            // that is half Betta imbellis: every client sees the species, colours and the ancestry
            BlockPos min = base.offset(-5, 0, 9);
            ServerLevel level = server.overworld();
            List<BlockPos> blocks = new ArrayList<>();
            for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) blocks.add(min.offset(x, y, z));
            for (BlockPos q : blocks) level.setBlockAndUpdate(q, ModBlocks.TANK.get().defaultBlockState());
            for (BlockPos q : blocks) TankMultiblock.onTankBlockAdded(level, q, null);
            TankBlockEntity c = TankMultiblock.getController(level, min);
            check("the M29 tank formed", c != null);
            if (c == null) return;
            TankInteractions.changeWater(level, c, 8000 - c.getTank().waterMb());
            c.getTank().setWaterTemp(18F);
            c.getTank().setTargetTemp(18F);
            org.overengineer.bettacraft.critters.TankCritters.add(level, c, org.overengineer.bettacraft.critters.CritterSpecies.FLORIDA_CRAYFISH,
                    org.overengineer.bettacraft.critters.CritterGenes.pure(0, 0.5F), true, 30);
            org.overengineer.bettacraft.critters.TankCritters.add(level, c, org.overengineer.bettacraft.critters.CritterSpecies.MARBLED_CRAYFISH,
                    org.overengineer.bettacraft.critters.CritterGenes.WILD, true, 30);
            var w = new org.overengineer.bettacraft.tank.TankWaterVolume(c.getTank());
            for (var v : List.of(net.minecraft.world.entity.animal.axolotl.Axolotl.Variant.BLUE, net.minecraft.world.entity.animal.axolotl.Axolotl.Variant.GOLD)) {
                var a = org.overengineer.bettacraft.tank.AxolotlCare.spawn(level, c, org.overengineer.bettacraft.critters.CritterGenes.pure(
                        org.overengineer.bettacraft.tank.AxolotlCare.locus(v), 0.1F), 0, w.box().getCenter().add(v == net.minecraft.world.entity.animal.axolotl.Axolotl.Variant.BLUE ? -0.3 : 0.3, -0.4, 0));
                if (a != null) a.setAge(0);
            }
            var g = org.overengineer.bettacraft.genetics.BettaGenotype.randomWild(net.minecraft.util.RandomSource.create(29))
                    .withAncestry(new org.overengineer.bettacraft.genetics.BettaSpecies.Ancestry(32, 0));
            var bucket = org.overengineer.bettacraft.entity.betta.BettaBucketItem.withFish(new net.minecraft.world.item.ItemStack(ModItems.BETTA_BUCKET.get()),
                    org.overengineer.bettacraft.entity.betta.BettaData.create(g, false, 0));
            var err = org.overengineer.bettacraft.tank.TankInhabitants.insert(level, c, bucket, org.overengineer.bettacraft.registry.ModEntities.BETTA.get(), null, false);
            check("the hybrid betta went in" + (err == null ? "" : ": " + err.getString()), err == null);
            c.markTankChanged();
            org.overengineer.bettacraft.critters.TankCritters.sync(level, c);
            say("m29", "all", min.getX() + "," + min.getY() + "," + min.getZ());
        }, () -> reports.containsKey(P1 + ":m29") && reports.containsKey(P2 + ":m29"), 20 * 30));
        steps.add(new Step("m30", () -> {
            // M30 part c: a 16 x 4 x 8 sea water tank with two bamboo sharks, a moray and an egg case
            BlockPos min = base.offset(-22, 0, 18);
            ServerLevel level = server.overworld();
            List<BlockPos> blocks = new ArrayList<>();
            for (int x = 0; x < 16; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 8; z++) blocks.add(min.offset(x, y, z));
            for (BlockPos q : blocks) level.setBlockAndUpdate(q, ModBlocks.TANK.get().defaultBlockState());
            for (BlockPos q : blocks) TankMultiblock.onTankBlockAdded(level, q, null);
            TankBlockEntity c = TankMultiblock.getController(level, min);
            check("the M30 tank formed", c != null);
            if (c == null) return;
            TankInteractions.changeWater(level, c, c.getTank().capacityMb() - c.getTank().waterMb(), org.overengineer.bettacraft.tank.Salinity.SEA);
            c.getTank().setWaterTemp(26F);
            c.getTank().setTargetTemp(26F);
            // the world persists between runs: start from an empty tank
            for (var e : org.overengineer.bettacraft.tank.TankInhabitants.liveInhabitants(level, c.getTank())) e.discard();
            c.getTank().inhabitants().clear();
            c.getTank().clutches().clear();
            for (var s : List.of(org.overengineer.bettacraft.tankmates.TankmateSpecies.BAMBOO_SHARK, org.overengineer.bettacraft.tankmates.TankmateSpecies.BAMBOO_SHARK,
                    org.overengineer.bettacraft.tankmates.TankmateSpecies.HONEYCOMB_MORAY)) {
                var tub = org.overengineer.bettacraft.big.BigCatching.caught(level, s, org.overengineer.bettacraft.critters.NetCatching.random(level));
                var err = tub == null ? null : org.overengineer.bettacraft.tank.TankInhabitants.insert(level, c, tub,
                        org.overengineer.bettacraft.tank.TankInhabitants.containedType(tub), null, 0);
                check(s.id() + " went in" + (err == null ? "" : ": " + err.getString()), tub != null && err == null);
            }
            var g = org.overengineer.bettacraft.tankmates.TankmateGenes.wild(org.overengineer.bettacraft.tankmates.TankmateSpecies.BAMBOO_SHARK);
            var w = new org.overengineer.bettacraft.tank.TankWaterVolume(c.getTank());
            c.getTank().clutches().add(new org.overengineer.bettacraft.tankmates.Clutch(c.getTank().nextClutchId(), org.overengineer.bettacraft.tankmates.TankmateSpecies.BAMBOO_SHARK,
                    org.overengineer.bettacraft.tankmates.Clutch.Kind.EGG_CASE, min.getX() + 3, w.box().minY + 0.02, min.getZ() + 2, 1, g, g));
            c.markTankChanged();
            say("m30", "all", min.getX() + "," + min.getY() + "," + min.getZ());
        }, () -> reports.containsKey(P1 + ":m30") && reports.containsKey(P2 + ":m30"), 20 * 30));
        steps.add(new Step("m31", () -> {
            // M31 part a: a dry 3 x 2 x 2 tank with a basking lamp on it: a terrarium with its climate
            BlockPos min = base.offset(-22, 0, 28);
            ServerLevel level = server.overworld();
            List<BlockPos> blocks = new ArrayList<>();
            for (int x = 0; x < 3; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) blocks.add(min.offset(x, y, z));
            for (BlockPos q : blocks) level.setBlockAndUpdate(q, ModBlocks.TANK.get().defaultBlockState());
            for (BlockPos q : blocks) TankMultiblock.onTankBlockAdded(level, q, null);
            TankBlockEntity c = TankMultiblock.getController(level, min);
            check("the M31 tank formed", c != null);
            if (c == null) return;
            if (c.getTank().waterMb() > 0) TankInteractions.changeWater(level, c, -c.getTank().waterMb());
            level.setBlockAndUpdate(min.offset(1, 2, 0), ModBlocks.BASKING_LAMP.get().defaultBlockState());
            var mode = org.overengineer.bettacraft.terrarium.TankClimate.refreshMode(level, c);
            check("the dry tank is a terrarium (" + mode + ")", mode == org.overengineer.bettacraft.terrarium.TerrariumMode.TERRARIUM);
            // M33.1: the tank type is fixed to terrarium (saved and synced to every client)
            c.getTank().setTankMode(org.overengineer.bettacraft.terrarium.TankModeSetting.TERRARIUM);
            check("a fixed terrarium refuses water", org.overengineer.bettacraft.terrarium.WaterLimit.room(level, c) == 0);
            org.overengineer.bettacraft.terrarium.TankClimate.step(level, c, 0.01);
            check("the terrarium has a climate", c.getTank().hasClimate());
            // M31 part b: a yellow-bellied slider and a slider hatchling next to the tank (no AI; the dev world keeps the last run's)
            for (var old : level.getEntitiesOfClass(org.overengineer.bettacraft.terrarium.HerpEntity.class, new net.minecraft.world.phys.AABB(min).inflate(8))) old.discard();
            for (int i = 0; i < 2; i++) {
                var s = org.overengineer.bettacraft.registry.ModEntities.SLIDER_TURTLE.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
                if (s == null) continue;
                s.setVariety(1);
                if (i == 1) s.makeHatchling();
                s.setNoAi(true);
                s.setPersistenceRequired();
                s.snapTo(min.getX() + 0.5 + i, min.getY(), min.getZ() - 2.5, 0F, 0F);
                level.addFreshEntity(s);
            }
            // M31 part c: a harlequin crested gecko shown clinging to a wall (the synced pose)
            var g = org.overengineer.bettacraft.registry.ModEntities.herp(org.overengineer.bettacraft.terrarium.HerpSpecies.CRESTED_GECKO).create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
            if (g != null) {
                g.setVariety(2);
                g.setNoAi(true);
                g.setPersistenceRequired();
                g.setPoseForTest(org.overengineer.bettacraft.terrarium.LandWalker.Pose.WALL_E);
                g.snapTo(min.getX() + 2.5, min.getY(), min.getZ() - 2.5, 0F, 0F);
                level.addFreshEntity(g);
            }
            // M31 part d: a zombie from a mob jar into the terrarium: shrunk on every client (the synced scale attribute)
            for (var old : level.getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Zombie.class, new net.minecraft.world.phys.AABB(min).inflate(8))) old.discard();
            var zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
            if (zombie != null) {
                zombie.setHealth(1F);
                var jar = org.overengineer.bettacraft.mobs.MobJarItem.fillWith(zombie);
                zombie.discard();
                var err = org.overengineer.bettacraft.tank.TankInhabitants.insert(level, c, jar, net.minecraft.world.entity.EntityType.ZOMBIE, null, 0L);
                check("a zombie goes into the terrarium" + (err == null ? "" : ": " + err.getString()), err == null);
            }
            c.markTankChanged();
            say("m31", "all", min.getX() + "," + min.getY() + "," + min.getZ());
        }, () -> reports.containsKey(P1 + ":m31") && reports.containsKey(P2 + ":m31"), 20 * 30));
        steps.add(new Step("reef", () -> {
            // M20: a small reef tank: sea water, a coral on reef rock in the interior, a clownfish pair; both clients see the salt
            BlockPos min = reefMin();
            ServerLevel level = server.overworld();
            List<BlockPos> blocks = new ArrayList<>();
            for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) blocks.add(min.offset(x, y, z));
            for (BlockPos q : blocks) level.setBlockAndUpdate(q, ModBlocks.TANK.get().defaultBlockState());
            for (BlockPos q : blocks) TankMultiblock.onTankBlockAdded(level, q, null);
            TankBlockEntity c = TankMultiblock.getController(level, min);
            check("the reef tank formed", c != null);
            if (c == null) return;
            TankInteractions.changeWater(level, c, 8000 - c.getTank().waterMb(), org.overengineer.bettacraft.tank.Salinity.SEA);
            c.getTank().setWaterTemp(26F);
            c.getTank().setTargetTemp(26F);
            var r = org.overengineer.bettacraft.tank.interior.InteriorManager.ensureRegion(level, c);
            ServerLevel in = org.overengineer.bettacraft.tank.interior.InteriorManager.interiorLevel(server);
            for (int x = 10; x < 18; x++) for (int z = 10; z < 18; z++) in.setBlock(r.toWorld(x, 0, z), ModBlocks.REEF_ROCK.get().defaultBlockState(), 3);
            org.overengineer.bettacraft.plants.InteriorPlants.plantAt(in, r, org.overengineer.bettacraft.plants.PlantSpecies.ZOANTHUS, 14, 1, 14, 4, 2020L);
            var w = new org.overengineer.bettacraft.tank.TankWaterVolume(c.getTank());
            var sp = org.overengineer.bettacraft.tankmates.TankmateSpecies.OCELLARIS_CLOWNFISH;
            // the dev world keeps the fish of the last run: start with an empty tank (M21)
            for (var old : org.overengineer.bettacraft.tankmates.TankmateBreeding.tankmates(level, c.getTank())) {
                c.getTank().inhabitants().remove(old.getUUID());
                old.discard();
            }
            for (int i = 0; i < 2; i++) {
                org.overengineer.bettacraft.tankmates.TankmateBreeding.spawn(level, c, sp, org.overengineer.bettacraft.tankmates.TankmateGenes.wild(sp), i == 0,
                        org.overengineer.bettacraft.entity.betta.LifeStage.ADULT, 0, w.box().getCenter().add(i * 0.3 - 0.15, 0, 0));
            }
            c.markTankChanged();
            say("reef", "all", min.getX() + "," + min.getY() + "," + min.getZ());
        }, () -> reports.containsKey(P1 + ":reef") && reports.containsKey(P2 + ":reef"), 20 * 30));
        steps.add(new Step("cabinets", () -> {
            // M21.7: a cabinet of deepslate bricks next to a cherry one (west of the reef tank): both clients see the stone
            BlockPos at = base.offset(-12, 0, 6);
            ServerLevel level = server.overworld();
            String[] woods = {"deepslate_bricks", "cherry_planks"};
            for (int i = 0; i < 2; i++) {
                BlockPos q = at.offset(0, 0, i);
                level.setBlockAndUpdate(q, ModBlocks.CABINET.get().defaultBlockState()
                        .setValue(org.overengineer.bettacraft.equipment.CabinetBlock.FACING, net.minecraft.core.Direction.NORTH));
                if (level.getBlockEntity(q) instanceof org.overengineer.bettacraft.equipment.CabinetBlockEntity cab) {
                    cab.setWood(net.minecraft.resources.Identifier.withDefaultNamespace(woods[i]));
                    level.sendBlockUpdated(q, level.getBlockState(q), level.getBlockState(q), 3);
                }
            }
            say("cabinets", "all", at.getX() + "," + at.getY() + "," + at.getZ());
        }, () -> reports.containsKey(P1 + ":cabinets") && reports.containsKey(P2 + ":cabinets"), 20 * 30));
        // M22.4: the outlet side of the cabinet filter: set on the server (the stone cabinet left, the cherry one front), both
        // clients see it; then Dev1 turns the stone one to the right with the button of the cabinet menu, both see that too
        steps.add(new Step("outlets", () -> {
            BlockPos at = base.offset(-12, 0, 6);
            ServerLevel level = server.overworld();
            org.overengineer.bettacraft.equipment.OutletSide[] sides = {org.overengineer.bettacraft.equipment.OutletSide.LEFT,
                    org.overengineer.bettacraft.equipment.OutletSide.FRONT};
            for (int i = 0; i < 2; i++) {
                if (level.getBlockEntity(at.offset(0, 0, i)) instanceof org.overengineer.bettacraft.equipment.CabinetBlockEntity cab) {
                    cab.items().setStackInSlot(org.overengineer.bettacraft.equipment.CabinetBlockEntity.FILTER_SLOT,
                            new ItemStack(org.overengineer.bettacraft.registry.ModItems.EXTERNAL_FILTER.get()));
                    cab.setOutletSide(sides[i]);
                }
            }
            OUTLETS_EXPECTED = "left,front";
            say("outlets", "all", at.getX() + "," + at.getY() + "," + at.getZ());
        }, () -> reports.containsKey(P1 + ":outletsleft,front") && reports.containsKey(P2 + ":outletsleft,front"), 20 * 30));
        steps.add(new Step("outlet button", () -> {
            BlockPos at = base.offset(-12, 0, 6);
            ServerPlayer p = player(P1);
            if (p == null) return;
            // next to the cabinet (the menu closes at more than 8 blocks)
            p.resetFallDistance();
            // the stage hangs in the air: a block to stand on in front of the cabinet
            server.overworld().setBlockAndUpdate(at.offset(0, -1, -2), net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState());
            p.teleportTo(server.overworld(), at.getX() + 0.5, at.getY(), at.getZ() - 1.6, java.util.Set.of(), 0F, 35F, true); // looking at the cabinet door
            hand(P1, ItemStack.EMPTY); // the client opens the menu itself (right-click on the cabinet)
            say("outletmenu", P1, at.getX() + "," + at.getY() + "," + at.getZ());
        }, () -> server.overworld().getBlockEntity(base.offset(-12, 0, 6)) instanceof org.overengineer.bettacraft.equipment.CabinetBlockEntity cab
                && cab.outletSide() == org.overengineer.bettacraft.equipment.OutletSide.RIGHT && reports.containsKey(P1 + ":outletmenu"), 20 * 30));
        steps.add(new Step("outlets after the button", () -> {
            ServerPlayer p = player(P1);
            if (p != null) p.closeContainer();
            OUTLETS_EXPECTED = "right,front";
            BlockPos at = base.offset(-12, 0, 6);
            say("outlets", "all", at.getX() + "," + at.getY() + "," + at.getZ());
        }, () -> reports.containsKey(P1 + ":outletsright,front") && reports.containsKey(P2 + ":outletsright,front"), 20 * 30));
        // M44: a row of four cabinets (the second from the viewer's left fixed to a single door, the last one without joints) and
        // the zombie in glass of the M31 terrarium kept walking: every client sees the fronts and moves the zombie's legs
        steps.add(new Step("m44", () -> {
            BlockPos at = base.offset(-30, 0, 6), m31 = base.offset(-22, 0, 28);
            ServerLevel level = server.overworld();
            for (int i = 0; i < 4; i++) {
                level.setBlockAndUpdate(at.offset(i, 0, 0), ModBlocks.CABINET.get().defaultBlockState()
                        .setValue(org.overengineer.bettacraft.equipment.CabinetBlock.FACING, net.minecraft.core.Direction.NORTH));
            }
            // facing north the viewer's left is the east: block i from the left stands at x + 3 - i
            if (level.getBlockEntity(at.offset(2, 0, 0)) instanceof org.overengineer.bettacraft.equipment.CabinetBlockEntity cab) {
                cab.setFrontSetting(org.overengineer.bettacraft.equipment.CabinetFrontSetting.SINGLE);
            }
            if (level.getBlockEntity(at) instanceof org.overengineer.bettacraft.equipment.CabinetBlockEntity cab) cab.setJoints(false);
            walkGlassZombie(level, m31);
            say("m44", "all", at.getX() + "," + at.getY() + "," + at.getZ() + ";" + m31.getX() + "," + m31.getY() + "," + m31.getZ());
        }, () -> {
            if (server.getTickCount() % 40 == 0) walkGlassZombie(server.overworld(), base.offset(-22, 0, 28));
            return reports.containsKey(P1 + ":m44") && reports.containsKey(P2 + ":m44");
        }, 20 * 40));
        // M29.1: an air pump goes into the stone cabinet: both clients learn it (hose); their pump sounds are off by default
        steps.add(new Step("cabinet air pump", () -> {
            BlockPos at = base.offset(-12, 0, 6);
            if (server.overworld().getBlockEntity(at) instanceof org.overengineer.bettacraft.equipment.CabinetBlockEntity cab) {
                cab.items().setStackInSlot(org.overengineer.bettacraft.equipment.CabinetBlockEntity.AIR_PUMP_SLOT,
                        new ItemStack(org.overengineer.bettacraft.registry.ModItems.AIR_PUMP.get()));
            }
            say("cabpump", "all", at.getX() + "," + at.getY() + "," + at.getZ());
        }, () -> reports.containsKey(P1 + ":cabpump") && reports.containsKey(P2 + ":cabpump"), 20 * 30));
        // M23: a small tank with an air pump, two air stones in the interior and an LED panel on top: both clients see the pump
        // run, the stones bubble in the miniature and both halves of the panel; then Dev1 turns the pump to 50 % (regulator)
        steps.add(new Step("aeration", () -> {
            BlockPos min = base.offset(-18, 0, 12);
            ServerLevel level = server.overworld();
            List<BlockPos> blocks = new ArrayList<>();
            for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) blocks.add(min.offset(x, y, z));
            for (BlockPos q : blocks) level.setBlockAndUpdate(q, ModBlocks.TANK.get().defaultBlockState());
            for (BlockPos q : blocks) TankMultiblock.onTankBlockAdded(level, q, null);
            TankBlockEntity c = TankMultiblock.getController(level, min);
            check("the aeration tank formed", c != null);
            if (c == null) return;
            org.overengineer.bettacraft.tank.TankInteractions.changeWater(level, c, c.getTank().capacityMb(), 0F);
            level.setBlockAndUpdate(min.offset(-1, 0, 0), ModBlocks.AIR_PUMP.get().defaultBlockState());
            for (int z = 0; z < 2; z++) level.setBlockAndUpdate(min.offset(0, 2, z), ModBlocks.LED_PANEL.get().defaultBlockState()
                    .setValue(org.overengineer.bettacraft.equipment.LedPanelBlock.PARTNER_POS, z == 0).setValue(org.overengineer.bettacraft.equipment.TankLightBlock.LIT, true));
            var r = org.overengineer.bettacraft.tank.interior.InteriorManager.ensureRegion(level, c);
            ServerLevel in = org.overengineer.bettacraft.tank.interior.InteriorManager.interiorLevel(server);
            in.setBlock(r.toWorld(6, 0, 6), ModBlocks.AIR_STONE.get().defaultBlockState(), 3);
            in.setBlock(r.toWorld(20, 0, 20), ModBlocks.AIR_STONE.get().defaultBlockState(), 3);
            org.overengineer.bettacraft.tank.TankEquipment.invalidate(c);
            org.overengineer.bettacraft.tank.TankAeration.step(level, c, 0.01);
            say("aeration", "all", min.getX() + "," + min.getY() + "," + min.getZ());
        }, () -> reports.containsKey(P1 + ":aeration") && reports.containsKey(P2 + ":aeration"), 20 * 40));
        steps.add(new Step("regulator", () -> {
            BlockPos min = base.offset(-18, 0, 12);
            ServerPlayer p = player(P1);
            if (p == null) return;
            // the stage hangs in the air: a block to stand on beside the pump
            server.overworld().setBlockAndUpdate(new BlockPos(min.getX() - 2, min.getY() - 1, min.getZ() - 2), net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState());
            p.resetFallDistance();
            p.teleportTo(server.overworld(), min.getX() - 1.5, min.getY(), min.getZ() - 1.5, java.util.Set.of(), 0F, 30F, true);
            say("regulate", P1, (min.getX() - 1) + "," + min.getY() + "," + min.getZ());
        }, () -> server.overworld().getBlockEntity(base.offset(-19, 0, 12)) instanceof org.overengineer.bettacraft.equipment.AirPumpBlockEntity a && a.power() == 5, 20 * 30));
        // M24: a tank fed by a water pump through a steel pipe from a reservoir, with a water change controller: both clients
        // see the pipe join, the controller's settings; then Dev1 changes the settings and starts a water change (payload)
        steps.add(new Step("plumbing", () -> {
            BlockPos min = base.offset(-26, 0, 12);
            ServerLevel level = server.overworld();
            List<BlockPos> blocks = new ArrayList<>();
            for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) blocks.add(min.offset(x, y, z));
            for (BlockPos q : blocks) level.setBlockAndUpdate(q, ModBlocks.TANK.get().defaultBlockState());
            for (BlockPos q : blocks) TankMultiblock.onTankBlockAdded(level, q, null);
            TankBlockEntity c = TankMultiblock.getController(level, min);
            check("the pipe tank formed", c != null);
            if (c == null) return;
            org.overengineer.bettacraft.tank.TankInteractions.changeWater(level, c, 6000, 0F);
            c.getTank().setNitrate(40F);
            level.setBlockAndUpdate(min.offset(-3, 0, 0), ModBlocks.RESERVOIR.get().defaultBlockState());
            if (level.getBlockEntity(min.offset(-3, 0, 0)) instanceof org.overengineer.bettacraft.plumbing.ReservoirBlockEntity r)
                r.water().fill(new org.overengineer.bettacraft.platform.fluid.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 2000),
                        org.overengineer.bettacraft.platform.fluid.IFluidHandler.FluidAction.EXECUTE);
            level.setBlockAndUpdate(min.offset(-2, 0, 0), ModBlocks.STAINLESS_STEEL_PIPE.get().defaultBlockState());
            level.setBlockAndUpdate(min.offset(-1, 0, 0), ModBlocks.WATER_PUMP.get().defaultBlockState()
                    .setValue(org.overengineer.bettacraft.plumbing.WaterPumpBlock.FACING, net.minecraft.core.Direction.EAST));
            level.setBlockAndUpdate(min.offset(-1, 0, 1), ModBlocks.WATER_CHANGE_CONTROLLER.get().defaultBlockState());
            level.setBlockAndUpdate(min.offset(-2, 0, 1), ModBlocks.RESERVOIR.get().defaultBlockState());
            if (level.getBlockEntity(min.offset(-2, 0, 1)) instanceof org.overengineer.bettacraft.plumbing.ReservoirBlockEntity r2)
                r2.water().fill(new org.overengineer.bettacraft.platform.fluid.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 16000),
                        org.overengineer.bettacraft.platform.fluid.IFluidHandler.FluidAction.EXECUTE);
            if (level.getBlockEntity(min.offset(-1, 0, 1)) instanceof org.overengineer.bettacraft.plumbing.WaterChangerBlockEntity wc) wc.set(9, 25, false);
            say("plumbing", "all", min.getX() + "," + min.getY() + "," + min.getZ());
        }, () -> reports.containsKey(P1 + ":plumbing") && reports.containsKey(P2 + ":plumbing"), 20 * 40));
        steps.add(new Step("water change", () -> {
            BlockPos min = base.offset(-26, 0, 12);
            ServerPlayer p = player(P1);
            if (p == null) return;
            server.overworld().setBlockAndUpdate(new BlockPos(min.getX() - 1, min.getY() - 1, min.getZ() - 2), net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState());
            p.resetFallDistance();
            p.teleportTo(server.overworld(), min.getX() - 0.5, min.getY(), min.getZ() - 1.5, java.util.Set.of(), 0F, 30F, true);
            say("waterchange", P1, (min.getX() - 1) + "," + min.getY() + "," + (min.getZ() + 1));
        }, () -> server.overworld().getBlockEntity(base.offset(-27, 0, 13)) instanceof org.overengineer.bettacraft.plumbing.WaterChangerBlockEntity wc
                && wc.intervalDays() == 3 && wc.share() == 30 && wc.enabled() && wc.last() >= 0
                && TankMultiblock.getController(server.overworld(), base.offset(-26, 0, 12)) instanceof TankBlockEntity c && c.getTank().nitrate() < 39.9F, 20 * 30));
        // M40 part c: covers on a filter pipe (stone bricks) and a pipe bundle (glowstone): both clients see the covers (block
        // entity update), the bundle keeps its two lines and shines; then the pipe's cover comes off and both see the pipe again
        steps.add(new Step("pipe covers", () -> {
            ServerLevel level = server.overworld();
            BlockPos a = base.offset(-29, 0, 16), b = base.offset(-28, 0, 16);
            level.setBlockAndUpdate(a, ModBlocks.FILTER_INLET_PIPE.get().defaultBlockState());
            level.setBlockAndUpdate(b, ModBlocks.FILTER_INLET_PIPE.get().defaultBlockState());
            org.overengineer.bettacraft.plumbing.FilterPipeBundleBlock.addLine(level, b, org.overengineer.bettacraft.plumbing.PipeLine.RETURN, null, null);
            check("cover on the pipe", org.overengineer.bettacraft.plumbing.PipeCovers.apply(level, a, net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState(),
                    new ItemStack(net.minecraft.world.item.Items.STONE_BRICKS), null));
            check("cover on the bundle", org.overengineer.bettacraft.plumbing.PipeCovers.apply(level, b, net.minecraft.world.level.block.Blocks.GLOWSTONE.defaultBlockState(),
                    new ItemStack(net.minecraft.world.item.Items.GLOWSTONE), null));
            say("pipecover", "all", a.getX() + "," + a.getY() + "," + a.getZ());
        }, () -> reports.containsKey(P1 + ":pipecover") && reports.containsKey(P2 + ":pipecover"), 20 * 30));
        steps.add(new Step("pipe cover off", () -> {
            BlockPos a = base.offset(-29, 0, 16);
            check("the cover comes off", org.overengineer.bettacraft.plumbing.PipeCovers.remove(server.overworld(), a).is(net.minecraft.world.item.Items.STONE_BRICKS));
            say("pipecover", "all", a.getX() + "," + a.getY() + "," + a.getZ());
        }, () -> reports.containsKey(P1 + ":pipecoveroff") && reports.containsKey(P2 + ":pipecoveroff"), 20 * 30));
        // M27: a 2x1 monitor screen next to a named tank: both clients get the screen, its automatic layout and the tank's
        // values (only viewers get data, bundled); then Dev1 sends a layout like the editor (part b) and both see it
        steps.add(new Step("monitor", () -> {
            BlockPos min = monitorMin();
            ServerLevel level = server.overworld();
            List<BlockPos> blocks = new ArrayList<>();
            for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) blocks.add(min.offset(x, y, z));
            for (BlockPos q : blocks) level.setBlockAndUpdate(q, ModBlocks.TANK.get().defaultBlockState());
            for (BlockPos q : blocks) TankMultiblock.onTankBlockAdded(level, q, null);
            TankBlockEntity c = TankMultiblock.getController(level, min);
            check("the monitor tank formed", c != null);
            if (c == null) return;
            TankInteractions.changeWater(level, c, 8000 - c.getTank().waterMb(), 0F);
            c.getTank().setName("MPTank");
            c.getTank().setWaterTemp(24.5F);
            c.getTank().setTargetTemp(24.5F);
            c.markTankChanged();
            var s = ModBlocks.MONITOR.get().defaultBlockState().setValue(org.overengineer.bettacraft.monitor.MonitorBlock.FACING, net.minecraft.core.Direction.NORTH);
            // a fresh screen (the world of an earlier run may still have the screen with Dev1's layout)
            level.setBlockAndUpdate(min.offset(1, 0, -3), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(min.offset(0, 0, -3), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(min.offset(1, 0, -3), s);
            level.setBlockAndUpdate(min.offset(0, 0, -3), s);
            say("monitor", "all", (min.getX() + 1) + "," + min.getY() + "," + (min.getZ() - 3));
        }, () -> reports.containsKey(P1 + ":monitor") && reports.containsKey(P2 + ":monitor"), 20 * 40));
        steps.add(new Step("monitor layout", () -> {
            BlockPos at = monitorMin().offset(1, 0, -3);
            ServerPlayer p = player(P1);
            if (p == null) return;
            server.overworld().setBlockAndUpdate(at.offset(0, -1, -3), net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState());
            p.resetFallDistance();
            p.teleportTo(server.overworld(), at.getX() + 0.5, at.getY(), at.getZ() - 2.5, java.util.Set.of(), 0F, 10F, true);
            say("monitorlayout", P1, at.getX() + "," + at.getY() + "," + at.getZ());
        }, () -> server.overworld().getBlockEntity(monitorMin().offset(1, 0, -3)) instanceof org.overengineer.bettacraft.monitor.MonitorBlockEntity m
                && !m.layout().auto && m.layout().tiles.size() == 3, 20 * 30));
        steps.add(new Step("monitor layout seen", () -> {
            BlockPos at = monitorMin().offset(1, 0, -3);
            say("monitorseen", "all", at.getX() + "," + at.getY() + "," + at.getZ());
        }, () -> reports.containsKey(P1 + ":monitorseen") && reports.containsKey(P2 + ":monitorseen"), 20 * 30));
        steps.add(new Step("seahorses", () -> {
            // M21.8: a small seahorse tank with a pair (the male with a full brood pouch); the cyst stack size is changed live
            // to 32 (both clients must follow), each player gets a stack of 20 jars and a used jar
            BlockPos min = seahorseMin();
            ServerLevel level = server.overworld();
            List<BlockPos> blocks = new ArrayList<>();
            for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) blocks.add(min.offset(x, y, z));
            for (BlockPos q : blocks) level.setBlockAndUpdate(q, ModBlocks.TANK.get().defaultBlockState());
            for (BlockPos q : blocks) TankMultiblock.onTankBlockAdded(level, q, null);
            TankBlockEntity c = TankMultiblock.getController(level, min);
            check("the seahorse tank formed", c != null);
            if (c == null) return;
            TankInteractions.changeWater(level, c, 8000 - c.getTank().waterMb(), 33F);
            c.getTank().setWaterTemp(23F);
            c.getTank().setTargetTemp(23F);
            for (var old : org.overengineer.bettacraft.tankmates.TankmateBreeding.tankmates(level, c.getTank())) {
                c.getTank().inhabitants().remove(old.getUUID());
                old.discard();
            }
            var sp = org.overengineer.bettacraft.tankmates.TankmateSpecies.LINED_SEAHORSE;
            var w = new org.overengineer.bettacraft.tank.TankWaterVolume(c.getTank());
            for (int i = 0; i < 2; i++) {
                var e = org.overengineer.bettacraft.tankmates.TankmateBreeding.spawn(level, c, sp, org.overengineer.bettacraft.tankmates.TankmateGenes.wild(sp), i == 0,
                        org.overengineer.bettacraft.entity.betta.LifeStage.ADULT, 0, w.box().getCenter().add(i * 0.4 - 0.2, -0.4, 0));
                if (e != null && i == 1) {
                    e.setGravid(0.5, org.overengineer.bettacraft.tankmates.TankmateGenes.wild(sp));
                    org.overengineer.bettacraft.entity.tankmate.TankmateCare.advance(level, c, e, 0.01);
                }
            }
            c.markTankChanged();
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(org.overengineer.bettacraft.registry.ModItems.ARTEMIA_CYSTS.get(), 64));
                // M35.1: 16 old jars as an old save holds them (5 uses spent): they load as 16 plain jars
                net.minecraft.nbt.CompoundTag old = new net.minecraft.nbt.CompoundTag();
                old.putString("id", "bettacraft:artemia_cysts");
                old.putInt("count", 16);
                net.minecraft.nbt.CompoundTag comps = new net.minecraft.nbt.CompoundTag();
                comps.putInt("minecraft:damage", 5);
                old.put("components", comps);
                p.getInventory().setItem(1, org.overengineer.bettacraft.util.NbtCompat.parseOptionalStack(server.registryAccess(), old));
            }
            say("seahorses", "all", min.getX() + "," + min.getY() + "," + min.getZ());
        }, () -> reports.containsKey(P1 + ":seahorses") && reports.containsKey(P2 + ":seahorses"), 20 * 30));
        // M22.1: containers in multiplayer. Dev1 empties a sea water bucket into deep water (out of reach of the bottom), a
        // betta bucket onto a stone pad (the confirm screen opens, nothing happens until "Empty"), pours a bottle out
        // sneaking; after each the client reports what it sees (no ghost blocks, items or animals)
        steps.add(new Step("containers: sea water bucket", () -> {
            BlockPos min = poolMin();
            ServerLevel level = server.overworld();
            for (int x = -1; x <= 3; x++)
                for (int z = -1; z <= 3; z++)
                    for (int y = 0; y < 8; y++) {
                        boolean wall = x == -1 || x == 3 || z == -1 || z == 3;
                        level.setBlockAndUpdate(min.offset(x, y, z), wall ? net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState()
                                : net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());
                    }
            // the stone pad: bare, nothing on it (the dev world keeps the last run)
            BlockPos pad = min.offset(8, 0, 1);
            level.setBlockAndUpdate(pad, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            for (int y = 1; y < 4; y++) level.setBlockAndUpdate(pad.above(y), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            for (var b : level.getEntitiesOfClass(org.overengineer.bettacraft.entity.betta.BettaEntity.class, new net.minecraft.world.phys.AABB(pad).inflate(4))) b.discard();
            ServerPlayer p = player(P1);
            if (p == null) return;
            overBlock(p, min.offset(1, 7, 1));
            hand(P1, new ItemStack(org.overengineer.bettacraft.registry.ModItems.SEA_WATER_BUCKET.get()));
            say("container", P1, "sea," + min.getX() + "," + (min.getY() + 7) + "," + min.getZ());
        }, () -> reports.containsKey(P1 + ":containers"), 20 * 30));
        steps.add(new Step("containers: animals ask first", () -> {
            reports.remove(P1 + ":containers");
            BlockPos pad = poolMin().offset(8, 0, 1);
            ServerPlayer p = player(P1);
            if (p == null) return;
            overBlock(p, pad.above(2));
            org.overengineer.bettacraft.genetics.BettaGenome g = org.overengineer.bettacraft.genetics.BettaGenome.randomDomestic(net.minecraft.util.RandomSource.create(221));
            hand(P1, org.overengineer.bettacraft.entity.betta.BettaBucketItem.withFish(new ItemStack(org.overengineer.bettacraft.registry.ModItems.BETTA_BUCKET.get()),
                    org.overengineer.bettacraft.entity.betta.BettaData.create(g, false, 0)));
            say("container", P1, "ask," + pad.getX() + "," + (pad.getY() + 1) + "," + pad.getZ());
        }, () -> reports.containsKey(P1 + ":containers"), 20 * 30));
        steps.add(new Step("containers: animals released", () -> {
            reports.remove(P1 + ":containers");
            BlockPos pad = poolMin().offset(8, 0, 1);
            say("container", P1, "release," + pad.getX() + "," + (pad.getY() + 1) + "," + pad.getZ());
        }, () -> reports.containsKey(P1 + ":containers"), 20 * 30));
        steps.add(new Step("containers: bottle", () -> {
            reports.remove(P1 + ":containers");
            BlockPos pad = poolMin().offset(10, 0, 1);
            ServerLevel level = server.overworld();
            level.setBlockAndUpdate(pad, net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState());
            for (int y = 1; y < 4; y++) level.setBlockAndUpdate(pad.above(y), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            ServerPlayer p = player(P1);
            if (p == null) return;
            overBlock(p, pad.above(2));
            hand(P1, new ItemStack(org.overengineer.bettacraft.registry.ModItems.SALT_WATER_BOTTLE.get()));
            say("container", P1, "pour," + pad.getX() + "," + (pad.getY() + 1) + "," + pad.getZ());
        }, () -> reports.containsKey(P1 + ":containers"), 20 * 30));
        // M22.3: wild small animals in the pool (entities with the full animal data; the clients see species and colour),
        // and the server switch "genes always visible" reaches the clients live
        steps.add(new Step("wildlife", () -> {
            BlockPos min = poolMin();
            ServerLevel level = server.overworld();
            for (var e : level.getEntitiesOfClass(org.overengineer.bettacraft.entity.critter.WildCritterEntity.class, new net.minecraft.world.phys.AABB(min).inflate(8))) e.discard();
            var kinds = new org.overengineer.bettacraft.critters.CritterSpecies[]{org.overengineer.bettacraft.critters.CritterSpecies.NEOCARIDINA,
                    org.overengineer.bettacraft.critters.CritterSpecies.RAMSHORN, org.overengineer.bettacraft.critters.CritterSpecies.HERMIT_CRAB};
            for (int i = 0; i < 3; i++) {
                var k = new org.overengineer.bettacraft.critters.Critter(0, kinds[i], i == 0, i == 0 ? org.overengineer.bettacraft.critters.CritterGenes.pure(0, 0.6F)
                        : org.overengineer.bettacraft.critters.CritterGenes.WILD, 0, 0, 0);
                k.age = 20;
                var e = org.overengineer.bettacraft.entity.critter.WildCritterEntity.of(level, k.saveAnimal());
                if (e == null) continue;
                e.snapTo(min.getX() + 0.3 + i * 0.7, min.getY() + 0.1, min.getZ() + 1.5, 0F, 0F);
                e.setPersistenceRequired();
                level.addFreshEntity(e);
            }
            ServerConfig.GENES_ALWAYS_VISIBLE.set(true);
            org.overengineer.bettacraft.network.ServerSettingsPayload.sendAll(server);
            say("wildlife", "all", min.getX() + "," + min.getY() + "," + min.getZ());
        }, () -> reports.containsKey(P1 + ":wildlife") && reports.containsKey(P2 + ":wildlife"), 20 * 30));
        // M36: the server switches the betta size live (as a reload of the server config does): both clients draw and size
        // the bettas of the main tank as the server says, Large and then Normal again
        steps.add(new Step("size large", () -> {
            ServerConfig.SIZE_BETTA.set(org.overengineer.bettacraft.config.AnimalSize.LARGE);
            org.overengineer.bettacraft.network.ServerSettingsPayload.sendAll(server);
            say("sizes", "all", tankArg());
        }, () -> reports.containsKey(P1 + ":sizesLARGE") && reports.containsKey(P2 + ":sizesLARGE"), 20 * 30));
        steps.add(new Step("size normal", () -> {
            ServerConfig.SIZE_BETTA.set(org.overengineer.bettacraft.config.AnimalSize.NORMAL);
            org.overengineer.bettacraft.network.ServerSettingsPayload.sendAll(server);
            say("sizes", "all", tankArg());
        }, () -> reports.containsKey(P1 + ":sizesNORMAL") && reports.containsKey(P2 + ":sizesNORMAL"), 20 * 30));
        steps.add(new Step("screenshots", () -> {
            ServerConfig.GENES_ALWAYS_VISIBLE.set(false);
            org.overengineer.bettacraft.network.ServerSettingsPayload.sendAll(server);
            placeDev1(player(P1));
            ServerPlayer p2 = player(P2);
            if (p2 != null) p2.teleportTo(server.overworld(), mainMin.getX() + 5.5, mainMin.getY(), mainMin.getZ() - 2.5, java.util.Set.of(), 35F, 10F, true);
            say("shot", "all", tankArg());
        }, () -> reports.containsKey(P1 + ":shot") && reports.containsKey(P2 + ":shot"), 20 * 40));
        // M33 part b: the server switches the tankmates off: both clients get its switches and their creative tab drops them
        steps.add(new Step("features off", () -> {
            org.overengineer.bettacraft.config.Features.set(org.overengineer.bettacraft.config.Feature.TANKMATES, false);
            org.overengineer.bettacraft.network.FeatureSyncPayload.sendAll(server);
            say("features", "all", "featoff");
        }, () -> reports.containsKey(P1 + ":featoff") && reports.containsKey(P2 + ":featoff"), 20 * 30));
        steps.add(new Step("features on", () -> {
            org.overengineer.bettacraft.config.Features.set(org.overengineer.bettacraft.config.Feature.TANKMATES, true);
            org.overengineer.bettacraft.network.FeatureSyncPayload.sendAll(server);
            say("features", "all", "feaon");
        }, () -> reports.containsKey(P1 + ":feaon") && reports.containsKey(P2 + ":feaon"), 20 * 30));
        // M33 part c: the server switches to the preset decor (in memory) and back
        steps.add(new Step("preset decor", () -> {
            org.overengineer.bettacraft.command.PresetCommand.apply(server, org.overengineer.bettacraft.config.Preset.DECOR, false);
            say("preset", "all", "predecor");
        }, () -> reports.containsKey(P1 + ":predecor") && reports.containsKey(P2 + ":predecor"), 20 * 30));
        steps.add(new Step("preset custom", () -> {
            org.overengineer.bettacraft.command.PresetCommand.apply(server, org.overengineer.bettacraft.config.Preset.CUSTOM, false);
            say("preset", "all", "precustom");
        }, () -> reports.containsKey(P1 + ":precustom") && reports.containsKey(P2 + ":precustom"), 20 * 30));
        if (org.overengineer.bettacraft.platform.Platform.get().isModLoaded("create")) travel(); // M50: with -Pcreate a tank turns on a bearing
        aquarists(); // M51: a hired aquarist for each player
        steps.add(new Step("third look", () -> say("state", "all", "3," + tankArg()),
                () -> reports.containsKey(P1 + ":state3") && reports.containsKey(P2 + ":state3"), 20 * 30));
        steps.add(new Step("quit", () -> say("quit", "all", ""), () -> server.getPlayerList().getPlayerCount() == 0, 20 * 60));
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (!ENABLED) return;
        server = event.getServer();
        ticks++;
        try {
            if (ticks == 20) {
                build(server.overworld());
                ServerConfig.EVAPORATION_ENABLED.set(false); // exact water levels to compare (restored at the end)
                plan();
            }
            if (ticks > 20 && !finished) {
                if (current == null && !steps.isEmpty()) {
                    current = steps.poll();
                    stepTicks = 0;
                    LOGGER.info("[smoketest] MP step: {}", current.name());
                    current.start().run();
                } else if (current != null) {
                    stepTicks++;
                    if (current.done().getAsBoolean()) {
                        current = null;
                    } else if (stepTicks > current.timeout()) {
                        fail("step '" + current.name() + "' timed out after " + current.timeout() / 20 + " s");
                        if (current.name().equals("players")) steps.removeIf(s -> !s.name().equals("quit"));
                        current = null;
                    }
                }
                if (current == null && steps.isEmpty()) finish();
                if (ticks > TIMEOUT_TICKS && !finished) {
                    fail("the whole test timed out");
                    finish();
                }
            }
            if (finished && ticks == haltAt) server.halt(false);
        } catch (RuntimeException e) {
            LOGGER.error("[smoketest] MP FAILED", e);
            failures.add("exception " + e);
            finish();
        }
    }

    // ------------------------------------------------------------------ M50: a tank on a turning bearing (only with Create)

    private static BlockPos travelBearing;
    private static net.minecraft.world.level.block.entity.BlockEntity travelBearingBe;
    private static net.minecraft.world.entity.Entity travelContraption;
    private static java.util.UUID travelTank;
    private static final List<java.util.UUID> travelBettas = new ArrayList<>();
    private static float travelAngle = Float.NaN;

    private static java.lang.reflect.Field field(Class<?> c, String name) throws NoSuchFieldException {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try {
                java.lang.reflect.Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    /** M50: before the block entities tick, the bearing is turned on (it would put its contraption down without a motor). */
    @SubscribeEvent
    public static void onServerTickPre(net.neoforged.neoforge.event.tick.ServerTickEvent.Pre event) {
        if (ENABLED && !finished) turnBearing();
    }

    /** Holds the bearing (no motor) at the next angle: it turns by hand, 3 degrees a tick. */
    private static void turnBearing() {
        if (travelBearingBe == null || Float.isNaN(travelAngle)) return;
        try {
            travelAngle += 3F;
            field(travelBearingBe.getClass(), "assembleNextTick").setBoolean(travelBearingBe, false);
            field(travelBearingBe.getClass(), "angle").setFloat(travelBearingBe, travelAngle);
        } catch (ReflectiveOperationException e) {
            fail("bearing: " + e);
        }
    }

    /**
     * M50: a stocked tank above the main tank goes onto a mechanical bearing that turns: both clients see the contraption,
     * get the state of the travelling tank (its two packed bettas) and report it; put down again, both bettas are back in
     * the world exactly once.
     */
    private static void travel() {
        steps.add(new Step("travel: assemble", () -> {
            ServerLevel level = server.overworld();
            travelBearing = mainMin.offset(1, 9, 1);
            clearTravel(level); // the dev world keeps what an earlier run left there
            var bearing = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse("create:mechanical_bearing"));
            net.minecraft.world.level.block.state.BlockState s = bearing.defaultBlockState();
            for (var p : s.getProperties()) if (p.getName().equals("facing")) s = s.setValue((net.minecraft.world.level.block.state.properties.EnumProperty<Direction>) p, net.minecraft.core.Direction.UP);
            level.setBlock(travelBearing, s, 3);
            travelBearingBe = level.getBlockEntity(travelBearing);
            List<BlockPos> blocks = new ArrayList<>();
            for (int x = 0; x < 2; x++) for (int z = 0; z < 2; z++) blocks.add(travelBearing.offset(x, 1, z));
            for (BlockPos p : blocks) level.setBlock(p, ModBlocks.TANK.get().defaultBlockState(), 2);
            TankMultiblock.formAll(level, blocks);
            TankBlockEntity c = TankMultiblock.getController(level, travelBearing.above());
            TankInteractions.changeWater(level, c, 2000);
            travelTank = c.getTank().id();
            travelBettas.clear();
            for (boolean female : new boolean[]{false, true}) {
                var d = org.overengineer.bettacraft.entity.betta.BettaData.create(org.overengineer.bettacraft.genetics.BettaGenotype.randomDomestic(
                        net.minecraft.util.RandomSource.create(female ? 4 : 3)), female, 0);
                TankInhabitants.insert(level, c, org.overengineer.bettacraft.entity.betta.BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d),
                        org.overengineer.bettacraft.registry.ModEntities.BETTA.get(), null, false);
            }
            for (var e : TankInhabitants.liveInhabitants(level, c.getTank())) travelBettas.add(e.getUUID());
            try {
                travelBearingBe.getClass().getMethod("assemble").invoke(travelBearingBe);
                travelContraption = (net.minecraft.world.entity.Entity) field(travelBearingBe.getClass(), "movedContraption").get(travelBearingBe);
            } catch (ReflectiveOperationException e) {
                fail("travel: bearing " + e);
            }
            check("travel: the bearing took the tank with " + travelBettas.size() + " bettas", travelContraption != null && travelBettas.size() == 2);
            travelAngle = 0F;
            turnBearing();
            say("travel", "all", travelBearing.getX() + "," + travelBearing.getY() + "," + travelBearing.getZ());
        }, () -> reports.containsKey(P1 + ":travel") && reports.containsKey(P2 + ":travel"), 20 * 40));
        steps.add(new Step("travel: put down", () -> {
            travelAngle = Float.NaN;
            try {
                field(travelBearingBe.getClass(), "angle").setFloat(travelBearingBe, 0F);
                if (travelContraption != null) travelContraption.getClass().getMethod("setAngle", float.class).invoke(travelContraption, 0F);
                travelBearingBe.getClass().getMethod("disassemble").invoke(travelBearingBe);
            } catch (ReflectiveOperationException e) {
                fail("travel: disassembly " + e);
            }
        }, () -> {
            TankBlockEntity c = TankMultiblock.getController(server.overworld(), travelBearing.above());
            return c != null && c.getTank().id().equals(travelTank) && TankInhabitants.liveInhabitants(server.overworld(), c.getTank()).size() == 2;
        }, 20 * 15));
        steps.add(new Step("travel: check", () -> {
            int once = 0;
            for (var u : travelBettas) if (server.overworld().getEntity(u) != null) once++;
            TankBlockEntity c = TankMultiblock.getController(server.overworld(), travelBearing.above());
            check("travel: both bettas are back exactly once (" + once + ")", once == 2);
            check("travel: the trip is in the tank's log", c != null && c.getTank().log().stream().anyMatch(l -> l.key().equals("log.bettacraft.travel.arrived")));
            clearTravel(server.overworld());
        }, () -> true, 20));
    }

    // ------------------------------------------------------------------ M51: a hired aquarist for each of the two players

    private static BlockPos aqBase;
    private static org.overengineer.bettacraft.aquarist.AquaristEntity aq1, aq2;
    private static final Map<String, BlockPos> aqTanks = new java.util.LinkedHashMap<>();

    /**
     * M51: two players, two aquarists (Dev1's and Dev2's, each with his own basin, chest and bed) and four tanks with algae:
     * Dev1's own, Dev2's own, Dev2's released for every aquarist and Dev2's locked (released too). Each aquarist cleans his
     * employer's tank and the released one, never the other player's own tank or the locked one; both clients see both
     * aquarists with their employers' names; only the employer changes his aquarist's tasks; Dev1 opens his aquarist's menu.
     */
    private static void aquarists() {
        steps.add(new Step("aquarists: hire", () -> {
            ServerLevel level = server.overworld();
            aqBase = base.offset(-26, 0, -10);
            for (int cx = (aqBase.getX() - 4) >> 4; cx <= (aqBase.getX() + 22) >> 4; cx++)
                for (int cz = (aqBase.getZ() - 4) >> 4; cz <= (aqBase.getZ() + 14) >> 4; cz++) level.setChunkForced(cx, cz, true);
            AABB box = new AABB(aqBase.offset(-3, -1, -3).getCenter(), aqBase.offset(22, 6, 14).getCenter()).inflate(1);
            for (Entity e : level.getEntitiesOfClass(Entity.class, box, e -> !(e instanceof ServerPlayer))) e.discard();
            for (BlockPos p : BlockPos.betweenClosed(aqBase.offset(-3, 0, -3), aqBase.offset(22, 5, 14))) {
                if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            }
            for (BlockPos p : BlockPos.betweenClosed(aqBase.offset(-3, -1, -3), aqBase.offset(22, -1, 14))) level.setBlockAndUpdate(p, Blocks.SMOOTH_STONE.defaultBlockState());
            ServerPlayer p1 = player(P1), p2 = player(P2);
            if (p1 == null || p2 == null) {
                fail("aquarists: players missing");
                return;
            }
            for (java.util.UUID u : List.of(p1.getUUID(), p2.getUUID())) org.overengineer.bettacraft.aquarist.AquaristRecords.get(server).reset(u);
            aq1 = aquarist(level, p1, aqBase);
            aq2 = aquarist(level, p2, aqBase.offset(0, 0, 8));
            // M59: names of their own (two random names can be the same, then the log cannot tell whose aquarist worked)
            aq1.setCustomName(net.minecraft.network.chat.Component.literal("Ada"));
            aq2.setCustomName(net.minecraft.network.chat.Component.literal("Bea"));
            aqTanks.clear();
            aqTanks.put("dev1", aqTank(level, aqBase.offset(8, 0, 0), p1.getUUID(), org.overengineer.bettacraft.aquarist.AquaristAccess.OWNER, false));
            aqTanks.put("dev2", aqTank(level, aqBase.offset(8, 0, 6), p2.getUUID(), org.overengineer.bettacraft.aquarist.AquaristAccess.OWNER, false));
            aqTanks.put("released", aqTank(level, aqBase.offset(14, 0, 0), p2.getUUID(), org.overengineer.bettacraft.aquarist.AquaristAccess.ALL, false));
            aqTanks.put("locked", aqTank(level, aqBase.offset(14, 0, 6), p2.getUUID(), org.overengineer.bettacraft.aquarist.AquaristAccess.ALL, true));
            check("aquarists: both have a home", aq1.housed() && aq2.housed());
            // (Dev1 also owns the main tank of the stage: he opened its menu, and a tank without an owner goes to who opens it)
            var t1 = org.overengineer.bettacraft.aquarist.AquaristWork.tanks(level, aq1).stream().map(TankBlockEntity::getBlockPos).toList();
            var t2 = org.overengineer.bettacraft.aquarist.AquaristWork.tanks(level, aq2).stream().map(TankBlockEntity::getBlockPos).toList();
            check("aquarists: Dev1's looks after his own and the released tank, not Dev2's or the locked one " + t1,
                    t1.contains(aqTanks.get("dev1")) && t1.contains(aqTanks.get("released")) && !t1.contains(aqTanks.get("dev2")) && !t1.contains(aqTanks.get("locked")));
            check("aquarists: Dev2's looks after his own and the released tank, not Dev1's or the locked one " + t2,
                    t2.contains(aqTanks.get("dev2")) && t2.contains(aqTanks.get("released")) && !t2.contains(aqTanks.get("dev1")) && !t2.contains(aqTanks.get("locked")));
            p1.teleportTo(level, aqBase.getX() + 4.5, aqBase.getY(), aqBase.getZ() - 4.5, java.util.Set.of(), 0F, 20F, false);
            p2.teleportTo(level, aqBase.getX() + 4.5, aqBase.getY(), aqBase.getZ() + 13.5, java.util.Set.of(), 180F, 20F, false);
        }, () -> true, 20));
        steps.add(new Step("aquarists: work", () -> {}, () -> {
            ServerLevel level = server.overworld();
            return algae(level, "dev1") < 0.01 && algae(level, "dev2") < 0.01 && algae(level, "released") < 0.01;
        }, 20 * 120));
        steps.add(new Step("aquarists: check", () -> {
            ServerLevel level = server.overworld();
            check("aquarists: the locked tank is untouched (" + algae(level, "locked") + ")", algae(level, "locked") > 0.5);
            check("aquarists: Dev1's tank only by Dev1's aquarist " + workers(level, "dev1"), workers(level, "dev1").equals(java.util.Set.of(aq1.logName())));
            check("aquarists: Dev2's tank only by Dev2's aquarist " + workers(level, "dev2"), workers(level, "dev2").equals(java.util.Set.of(aq2.logName())));
            check("aquarists: the released tank by one of them " + workers(level, "released"),
                    !workers(level, "released").isEmpty() && java.util.Set.of(aq1.logName(), aq2.logName()).containsAll(workers(level, "released")));
            // only the employer changes the tasks
            ServerPlayer p1 = player(P1), p2 = player(P2);
            var feed = org.overengineer.bettacraft.aquarist.AquaristTask.FEED;
            int before = aq2.taskLevel(feed);
            if (p1 != null) {
                p1.teleportTo(level, aq2.getX() - 1.5, aq2.getY(), aq2.getZ(), java.util.Set.of(), -90F, 10F, false);
                org.overengineer.bettacraft.aquarist.AquaristMenu.handle(p1, new org.overengineer.bettacraft.network.AquaristActionPayload(aq2.getId(),
                        org.overengineer.bettacraft.aquarist.AquaristMenu.SET_LEVEL, feed.ordinal() * 10 + 3, ""));
            }
            check("aquarists: Dev1 cannot change Dev2's aquarist", aq2.taskLevel(feed) == before);
            if (p2 != null) {
                p2.teleportTo(level, aq2.getX() + 1.5, aq2.getY(), aq2.getZ(), java.util.Set.of(), 90F, 10F, false);
                org.overengineer.bettacraft.aquarist.AquaristMenu.handle(p2, new org.overengineer.bettacraft.network.AquaristActionPayload(aq2.getId(),
                        org.overengineer.bettacraft.aquarist.AquaristMenu.SET_LEVEL, feed.ordinal() * 10 + 3, ""));
            }
            check("aquarists: Dev2 changes his own aquarist", aq2.taskLevel(feed) == 3);
            say("aquarists", "all", aq1.getId() + "," + aq2.getId());
        }, () -> reports.containsKey(P1 + ":aquarists") && reports.containsKey(P2 + ":aquarists"), 20 * 30));
        steps.add(new Step("aquarists: menu", () -> {
            ServerPlayer p1 = player(P1);
            if (p1 != null) {
                p1.teleportTo(server.overworld(), aq1.getX(), aq1.getY(), aq1.getZ() - 2.5, java.util.Set.of(), 0F, 15F, true);
                org.overengineer.bettacraft.aquarist.AquaristMenu.open(p1, aq1);
            }
            say("aquaristmenu", P1, String.valueOf(aq1.getId()));
        }, () -> reports.containsKey(P1 + ":aquaristmenu"), 20 * 30));
        steps.add(new Step("aquarists: clean up", () -> {
            ServerLevel level = server.overworld();
            for (var a : new org.overengineer.bettacraft.aquarist.AquaristEntity[]{aq1, aq2}) if (a != null) a.discard();
            for (String n : List.of(P1, P2)) {
                ServerPlayer p = player(n);
                if (p != null) org.overengineer.bettacraft.aquarist.AquaristRecords.get(server).reset(p.getUUID());
            }
            for (BlockPos p : BlockPos.betweenClosed(aqBase.offset(-3, 0, -3), aqBase.offset(22, 5, 14))) {
                if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
            for (int cx = (aqBase.getX() - 4) >> 4; cx <= (aqBase.getX() + 22) >> 4; cx++)
                for (int cz = (aqBase.getZ() - 4) >> 4; cz <= (aqBase.getZ() + 14) >> 4; cz++) level.setChunkForced(cx, cz, false);
            ServerPlayer p1 = player(P1), p2 = player(P2);
            if (p1 != null) placeDev1(p1);
            if (p2 != null) placeDev2(p2);
        }, () -> true, 20));
    }

    private static org.overengineer.bettacraft.aquarist.AquaristEntity aquarist(ServerLevel level, ServerPlayer p, BlockPos at) {
        level.setBlockAndUpdate(at, org.overengineer.bettacraft.registry.ModBlocks.BREEDING_BASIN.get().defaultBlockState());
        level.setBlockAndUpdate(at.east(), Blocks.CHEST.defaultBlockState());
        var foot = Blocks.WHITE_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, net.minecraft.core.Direction.SOUTH)
                .setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT);
        level.setBlockAndUpdate(at.offset(-2, 0, 0), foot);
        level.setBlockAndUpdate(at.offset(-2, 0, 1), foot.setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD));
        var npc = org.overengineer.bettacraft.aquarist.AquaristArrival.spawnHiredHomeless(level, p.getUUID(), p.getGameProfile().name(),
                net.minecraft.world.phys.Vec3.atBottomCenterOf(at.offset(1, 0, 2)));
        npc.setHome(level, at, at.offset(-2, 0, 1));
        npc.alwaysDay = true;
        for (var t : org.overengineer.bettacraft.aquarist.AquaristTask.ALL) npc.setTaskLevel(t, t == org.overengineer.bettacraft.aquarist.AquaristTask.ALGAE ? 3 : 0);
        return npc;
    }

    private static BlockPos aqTank(ServerLevel level, BlockPos min, java.util.UUID owner, org.overengineer.bettacraft.aquarist.AquaristAccess access, boolean locked) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int x = 0; x < 3; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) blocks.add(min.offset(x, y, z));
        for (BlockPos p : blocks) level.setBlockAndUpdate(p, org.overengineer.bettacraft.registry.ModBlocks.TANK.get().defaultBlockState());
        TankMultiblock.formAll(level, blocks);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        if (c == null) {
            fail("aquarists: no tank at " + min);
            return min;
        }
        org.overengineer.bettacraft.tank.TankInteractions.changeWater(level, c, c.getTank().capacityMb());
        c.getTank().setOwner(owner);
        c.getTank().setAquaristAccess(access);
        c.getTank().setLocked(locked);
        for (var f : org.overengineer.bettacraft.tank.algae.TankAlgae.wetFaces(c.getTank()))
            org.overengineer.bettacraft.tank.algae.AlgaeSim.set(c.getTank().algae(), f.pos(), f.dir(), 0.8F);
        c.markTankChanged();
        org.overengineer.bettacraft.aquarist.AquaristRecords.get(server).noteTank(level, c);
        return c.getBlockPos();
    }

    private static double algae(ServerLevel level, String tank) {
        TankBlockEntity c = TankMultiblock.getController(level, aqTanks.get(tank));
        return c == null ? -1 : org.overengineer.bettacraft.tank.algae.TankAlgae.mean(c.getTank());
    }

    /** The names in the tank's log entries of the aquarists' work. */
    private static java.util.Set<String> workers(ServerLevel level, String tank) {
        TankBlockEntity c = TankMultiblock.getController(level, aqTanks.get(tank));
        java.util.Set<String> out = new java.util.HashSet<>();
        if (c == null) return out;
        for (var e : c.getTank().log()) if (e.key().startsWith("log.bettacraft.aquarist.") && !e.args().isEmpty()) out.add(e.args().get(0));
        return out;
    }

    /** Removes the travel stage (tank blocks, bearing, bettas) around the bearing's place. */
    private static void clearTravel(ServerLevel level) {
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(travelBearing).inflate(4);
        for (var e : level.getEntitiesOfClass(org.overengineer.bettacraft.entity.betta.BettaEntity.class, box)) e.discard();
        for (BlockPos p : BlockPos.betweenClosed(travelBearing.offset(-2, 0, -2), travelBearing.offset(3, 3, 3))) {
            level.removeBlockEntity(p);
            level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2 | 16);
        }
    }

    private static void checkTravel(String name, String[] a) {
        // travel <states received> <contraption seen> <packed animals on the client's copy>
        int received = a.length > 1 ? Integer.parseInt(a[1]) : -1, packed = a.length > 3 ? Integer.parseInt(a[3]) : -1;
        check(name + ": travelling tank states received " + received + ", contraption seen " + (a.length > 2 ? a[2] : "?") + ", packed " + packed + " (server 2)",
                received > 0 && a.length > 2 && "true".equals(a[2]) && packed == 2);
    }

    private static void finish() {
        if (finished) return;
        finished = true;
        ServerConfig.EVAPORATION_ENABLED.set(savedEvaporation);
        org.overengineer.bettacraft.config.Features.set(org.overengineer.bettacraft.config.Feature.TANKMATES, true); // M33 part b
        if (server != null && base != null) {
            for (int cx = (base.getX() - 8) >> 4; cx <= (base.getX() + 14) >> 4; cx++)
                for (int cz = (base.getZ() - 8) >> 4; cz <= (base.getZ() + 14) >> 4; cz++) server.overworld().setChunkForced(cx, cz, false);
        }
        if (failures.isEmpty() && !passes.isEmpty()) LOGGER.info("[smoketest] MP PASSED ({} checks)", passes.size());
        else LOGGER.error("[smoketest] MP FAILED: {} of {} checks failed: {}", failures.size(), failures.size() + passes.size(), failures);
        haltAt = ticks + 40;
    }
}
