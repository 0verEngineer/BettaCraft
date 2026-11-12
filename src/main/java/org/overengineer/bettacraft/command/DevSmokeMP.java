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
    /** Whole test: 7 minutes (the clients need about a minute to start). */
    private static final int TIMEOUT_TICKS = 20 * 60 * 7;
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
            case "menu" -> check(name + ": tank menu (" + text.substring(5) + ")", "ok".equals(a[1]));
            case "error" -> fail(name + ": " + text.substring(6));
            default -> { }
        }
        reports.put(name + ":" + a[0] + (a.length > 1 && "state".equals(a[0]) ? a[1] : ""), text);
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
