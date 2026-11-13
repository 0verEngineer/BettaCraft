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
            case "error" -> fail(name + ": " + text.substring(6));
            default -> { }
        }
        reports.put(name + ":" + a[0] + (a.length > 1 && java.util.Set.of("state", "aqua", "critters", "seen", "mini", "mine").contains(a[0]) ? a[1] : ""), text);
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
        check(name + ": " + a[1] + " tankmates (server " + mates.size() + ")", Integer.parseInt(a[1]) == mates.size() && mates.size() == 5);
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
        check(name + ": " + a[3] + " clownfish (server " + fish + ")", Integer.parseInt(a[3]) == fish && fish == 2);
        check(name + ": " + a[4] + " coral blocks in the miniature (server " + coral + ")", Integer.parseInt(a[4]) == coral && coral > 1);
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
        p.setGameMode(GameType.SURVIVAL);
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
            for (int i = 0; i < 2; i++) {
                org.overengineer.bettacraft.tankmates.TankmateBreeding.spawn(level, c, sp, org.overengineer.bettacraft.tankmates.TankmateGenes.wild(sp), i == 0,
                        org.overengineer.bettacraft.entity.betta.LifeStage.ADULT, 0, w.box().getCenter().add(i * 0.3 - 0.15, 0, 0));
            }
            c.markTankChanged();
            say("reef", "all", min.getX() + "," + min.getY() + "," + min.getZ());
        }, () -> reports.containsKey(P1 + ":reef") && reports.containsKey(P2 + ":reef"), 20 * 30));
        steps.add(new Step("screenshots", () -> {
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
