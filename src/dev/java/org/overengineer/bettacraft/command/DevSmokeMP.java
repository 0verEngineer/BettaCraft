package org.overengineer.bettacraft.command;

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
import net.minecraft.world.level.GameRules;
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
        String name = p.getGameProfile().getName();
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
            case "reef" -> checkReef(name, a);
            case "cabinets" -> check(name + ": stone and wooden cabinet " + (a.length > 2 ? a[1] + " " + a[2] : "?") + " (server minecraft:deepslate_bricks minecraft:cherry_planks)",
                    a.length > 3 && "minecraft:deepslate_bricks".equals(a[1]) && "minecraft:cherry_planks".equals(a[2]) && "deepslate_bricks".equals(a[3]));
            case "seahorses" -> checkSeahorses(name, a);
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
            case "outletmenu" -> check(name + ": cabinet menu with the outlet button (" + (a.length > 1 ? a[1] : "?") + ")", a.length > 1 && "ok".equals(a[1]));
            case "error" -> fail(name + ": " + text.substring(6));
            default -> { }
        }
        reports.put(name + ":" + a[0] + (a.length > 1 && java.util.Set.of("state", "aqua", "critters", "seen", "mini", "mine").contains(a[0]) ? a[1] : "")
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
        p.teleportTo(server.overworld(), at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 0F, 90F);
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
                int bettas = p == null ? -1 : p.serverLevel().getEntitiesOfClass(org.overengineer.bettacraft.entity.betta.BettaEntity.class,
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
     * M21.8 {@code seahorses <seahorses> <full pouches> <stack size> <slot 0 count> <slot 0 uses> <slot 1 count> <slot 1 uses>}: the
     * clients see both seahorses and the full brood pouch of the male, follow the live change of the cyst stack size (32) and
     * have the stacked jars with the right uses.
     */
    private static void checkSeahorses(String name, String[] a) {
        if (a.length < 8) {
            fail(name + ": short seahorse report");
            return;
        }
        check(name + ": " + a[1] + " seahorses (server 2)", "2".equals(a[1]));
        check(name + ": " + a[2] + " full brood pouch (server 1, the male)", "1".equals(a[2]));
        check(name + ": cyst jars per stack " + a[3] + " (server 32 after the live change)", "32".equals(a[3]));
        check(name + ": a stack of " + a[4] + " jars with " + a[5] + " uses each (server 20 x 16)", "20".equals(a[4]) && "16".equals(a[5]));
        check(name + ": a used jar " + a[6] + " x " + a[7] + " uses (server 1 x 15)", "1".equals(a[6]) && "15".equals(a[7]));
    }

    private static TankBlockEntity main() {
        TankBlockEntity c = TankMultiblock.getController(server.overworld(), mainMin);
        if (c == null) throw new IllegalStateException("MP: main tank gone");
        return c;
    }

    private static void build(ServerLevel level) {
        base = level.getSharedSpawnPos().offset(0, 60, -48);
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
        level.setDayTime(6000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.setWeatherParameters(6000, 0, false, false);
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
        p.teleportTo(in, px, base, pz, yaw, pitch);
    }

    /** Dev1 in front of the north glass, eye a little below the water surface, looking south at the tank. */
    private static void placeDev1(ServerPlayer p) {
        p.teleportTo(server.overworld(), mainMin.getX() + 2.0, mainMin.getY(), mainMin.getZ() - 2.0, 0F, 0F);
    }

    /** Dev2 on the ledge south of the tank, looking down at the surface. */
    private static void placeDev2(ServerPlayer p) {
        p.teleportTo(server.overworld(), mainMin.getX() + 2.0, mainMin.getY() + 3.0, mainMin.getZ() + 6.6, 180F, 32F);
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
        if (P1.equals(p.getGameProfile().getName())) placeDev1(p);
        else placeDev2(p);
        p.sendSystemMessage(Component.literal(PREFIX + "tank:all:" + mainMin.getX() + "," + mainMin.getY() + "," + mainMin.getZ()));
        LOGGER.info("[smoketest] MP {} joined", p.getGameProfile().getName());
    }

    // ------------------------------------------------------------------ the scenario

    private static ServerPlayer player(String name) {
        return server.getPlayerList().getPlayerByName(name);
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
        if (p != null) p.getInventory().setItem(p.getInventory().selected, stack);
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
                () -> reports.containsKey(P1 + ":motion") && reports.containsKey(P2 + ":motion"), 20 * 40));
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
        steps.add(new Step("plants grow", () -> {
            TankBlockEntity c = main();
            var r = org.overengineer.bettacraft.plants.InteriorPlants.region(server, c.getTank());
            for (int i = 0; i < 8; i++) org.overengineer.bettacraft.plants.InteriorPlants.grow(server.overworld(), c, r, 0.5);
            LOGGER.info("[smoketest] MP aquarium plants grown: {}", r.plants().all());
        }, () -> stepTicks >= 60, 100));
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
                p.teleportTo(server.overworld(), lMin.getX() + (n.equals(P1) ? 1.5 : 0.5), lMin.getY(), lMin.getZ() - 1.5, 0F, 20F);
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
                p.teleportTo(server.overworld(), lMin.getX() + 0.5, lMin.getY(), lMin.getZ() - 1.5, 0F, 36F);
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
                    cab.setWood(net.minecraft.resources.ResourceLocation.withDefaultNamespace(woods[i]));
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
            p.teleportTo(server.overworld(), at.getX() + 0.5, at.getY(), at.getZ() - 1.6, 0F, 35F); // looking at the cabinet door
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
            p.teleportTo(server.overworld(), min.getX() - 1.5, min.getY(), min.getZ() - 1.5, 0F, 30F);
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
                r.water().fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 2000),
                        net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            level.setBlockAndUpdate(min.offset(-2, 0, 0), ModBlocks.STAINLESS_STEEL_PIPE.get().defaultBlockState());
            level.setBlockAndUpdate(min.offset(-1, 0, 0), ModBlocks.WATER_PUMP.get().defaultBlockState()
                    .setValue(org.overengineer.bettacraft.plumbing.WaterPumpBlock.FACING, net.minecraft.core.Direction.EAST));
            level.setBlockAndUpdate(min.offset(-1, 0, 1), ModBlocks.WATER_CHANGE_CONTROLLER.get().defaultBlockState());
            level.setBlockAndUpdate(min.offset(-2, 0, 1), ModBlocks.RESERVOIR.get().defaultBlockState());
            if (level.getBlockEntity(min.offset(-2, 0, 1)) instanceof org.overengineer.bettacraft.plumbing.ReservoirBlockEntity r2)
                r2.water().fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 16000),
                        net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            if (level.getBlockEntity(min.offset(-1, 0, 1)) instanceof org.overengineer.bettacraft.plumbing.WaterChangerBlockEntity wc) wc.set(9, 25, false);
            say("plumbing", "all", min.getX() + "," + min.getY() + "," + min.getZ());
        }, () -> reports.containsKey(P1 + ":plumbing") && reports.containsKey(P2 + ":plumbing"), 20 * 40));
        steps.add(new Step("water change", () -> {
            BlockPos min = base.offset(-26, 0, 12);
            ServerPlayer p = player(P1);
            if (p == null) return;
            server.overworld().setBlockAndUpdate(new BlockPos(min.getX() - 1, min.getY() - 1, min.getZ() - 2), net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState());
            p.resetFallDistance();
            p.teleportTo(server.overworld(), min.getX() - 0.5, min.getY(), min.getZ() - 1.5, 0F, 30F);
            say("waterchange", P1, (min.getX() - 1) + "," + min.getY() + "," + (min.getZ() + 1));
        }, () -> server.overworld().getBlockEntity(base.offset(-27, 0, 13)) instanceof org.overengineer.bettacraft.plumbing.WaterChangerBlockEntity wc
                && wc.intervalDays() == 3 && wc.share() == 30 && wc.enabled() && wc.last() >= 0
                && TankMultiblock.getController(server.overworld(), base.offset(-26, 0, 12)) instanceof TankBlockEntity c && c.getTank().nitrate() < 39.9F, 20 * 30));
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
            ServerConfig.FOOD_CYST_STACK.set(32);
            org.overengineer.bettacraft.network.ServerSettingsPayload.sendAll(server);
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(org.overengineer.bettacraft.registry.ModItems.ARTEMIA_CYSTS.get(), 20));
                p.getInventory().setItem(1, org.overengineer.bettacraft.food.ArtemiaCystsItem.usedJar(
                        new net.minecraft.world.item.ItemStack(org.overengineer.bettacraft.registry.ModItems.ARTEMIA_CYSTS.get())));
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
                e.moveTo(min.getX() + 0.3 + i * 0.7, min.getY() + 0.1, min.getZ() + 1.5, 0F, 0F);
                e.setPersistenceRequired();
                level.addFreshEntity(e);
            }
            ServerConfig.GENES_ALWAYS_VISIBLE.set(true);
            org.overengineer.bettacraft.network.ServerSettingsPayload.sendAll(server);
            say("wildlife", "all", min.getX() + "," + min.getY() + "," + min.getZ());
        }, () -> reports.containsKey(P1 + ":wildlife") && reports.containsKey(P2 + ":wildlife"), 20 * 30));
        steps.add(new Step("screenshots", () -> {
            ServerConfig.GENES_ALWAYS_VISIBLE.set(false);
            org.overengineer.bettacraft.network.ServerSettingsPayload.sendAll(server);
            // M21.8: back to the default cyst stack size
            ServerConfig.FOOD_CYST_STACK.set(16);
            org.overengineer.bettacraft.network.ServerSettingsPayload.sendAll(server);
            placeDev1(player(P1));
            ServerPlayer p2 = player(P2);
            if (p2 != null) p2.teleportTo(server.overworld(), mainMin.getX() + 5.5, mainMin.getY(), mainMin.getZ() - 2.5, 35F, 10F);
            say("shot", "all", tankArg());
        }, () -> reports.containsKey(P1 + ":shot") && reports.containsKey(P2 + ":shot"), 20 * 40));
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

    private static void finish() {
        if (finished) return;
        finished = true;
        ServerConfig.EVAPORATION_ENABLED.set(savedEvaporation);
        if (server != null && base != null) {
            for (int cx = (base.getX() - 8) >> 4; cx <= (base.getX() + 14) >> 4; cx++)
                for (int cz = (base.getZ() - 8) >> 4; cz <= (base.getZ() + 14) >> 4; cz++) server.overworld().setChunkForced(cx, cz, false);
        }
        if (failures.isEmpty() && !passes.isEmpty()) LOGGER.info("[smoketest] MP PASSED ({} checks)", passes.size());
        else LOGGER.error("[smoketest] MP FAILED: {} of {} checks failed: {}", failures.size(), failures.size() + passes.size(), failures);
        haltAt = ticks + 40;
    }
}
