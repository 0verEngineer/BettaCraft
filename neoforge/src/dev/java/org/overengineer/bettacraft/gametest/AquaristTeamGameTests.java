package org.overengineer.bettacraft.gametest;
import org.overengineer.bettacraft.util.NbtCompat;

import static org.overengineer.bettacraft.gametest.HiredAquaristGameTests.BASIN;
import static org.overengineer.bettacraft.gametest.HiredAquaristGameTests.BED;
import static org.overengineer.bettacraft.gametest.HiredAquaristGameTests.cleanup;
import static org.overengineer.bettacraft.gametest.HiredAquaristGameTests.floor;
import static org.overengineer.bettacraft.gametest.HiredAquaristGameTests.home;
import static org.overengineer.bettacraft.gametest.HiredAquaristGameTests.npc;
import static org.overengineer.bettacraft.gametest.HiredAquaristGameTests.player;
import static org.overengineer.bettacraft.gametest.HiredAquaristGameTests.tank;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.aquarist.AquaristArrival;
import org.overengineer.bettacraft.aquarist.AquaristEntity;
import org.overengineer.bettacraft.aquarist.AquaristMenu;
import org.overengineer.bettacraft.aquarist.AquaristRecords;
import org.overengineer.bettacraft.aquarist.AquaristTeam;
import org.overengineer.bettacraft.aquarist.AquaristWork;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;

/**
 * M69: several aquarists per player. The conditions of the friend one by one (team limit, a week without a break, enough
 * tanks, another breeding, no trouble, the wait after a refusal), the recommendation in the chat and the menu, hiring him
 * (he takes the second home; without one he waits next to the first and no homeless day counts), refusing him, the limit
 * of the config (1 = as before M69), the switch, every tank with exactly one aquarist (the nearest workplace, chosen in the
 * menu), the one who recommends after the first left, and the records of an old world with one aquarist.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AquaristTeamGameTests {
    private static final String EMPTY = "empty_32x10x32";
    static final BlockPos BASIN2 = new BlockPos(20, 1, 4), CHEST2 = new BlockPos(21, 1, 4), BED2 = new BlockPos(20, 1, 8);

    private AquaristTeamGameTests() {}

    private static void check(boolean ok, String what) {
        if (!ok) throw new GameTestFailure(what);
    }

    /** The second home: a breeding basin with a chest, and a bed. */
    static void home2(GameTestHelper h) {
        h.setBlock(BASIN2, ModBlocks.BREEDING_BASIN.get().defaultBlockState());
        h.setBlock(CHEST2, Blocks.CHEST.defaultBlockState());
        BlockState foot = Blocks.BLUE_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.FOOT);
        h.setBlock(BED2, foot);
        h.setBlock(BED2.south(), foot.setValue(BedBlock.PART, BedPart.HEAD));
    }

    /** Six own tanks (enough for the second aquarist with the default three per aquarist). */
    static List<TankBlockEntity> tanks(GameTestHelper h, UUID owner, int n) {
        List<TankBlockEntity> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add(tankAt(h, i, owner));
        return out;
    }

    private static final int[][] AT = {{6, 14}, {10, 14}, {16, 14}, {22, 14}, {6, 20}, {10, 20}, {16, 20}, {22, 20}, {6, 26}, {10, 26}};

    /** The tank number {@code i} of the test area (3x2x2, apart from the others). */
    static TankBlockEntity tankAt(GameTestHelper h, int i, UUID owner) {
        return tank(h, AT[i][0], AT[i][1], owner);
    }

    /** Makes every condition of the next friend true: the first worked {@code days} days without a break, another breeding. */
    static AquaristRecords.PlayerRecord ripe(MinecraftServer server, UUID player, int days) {
        AquaristRecords.PlayerRecord r = AquaristRecords.get(server).player(player);
        long t = AquaristTeam.now(server) - days * 24000L - 100;
        for (AquaristRecords.Member m : r.team) {
            m.since = t;
            m.paidSince = t;
            m.trouble = Long.MIN_VALUE;
        }
        r.lastHire = t;
        r.breedings = r.breedingsAtHire + 1;
        r.friendNotBefore = Long.MIN_VALUE;
        return r;
    }

    static boolean ok(MinecraftServer server, UUID player, AquaristRecords.PlayerRecord r, String key) {
        for (AquaristTeam.Condition c : AquaristTeam.conditions(server, player, r)) if (c.key().equals(key)) return c.ok();
        throw new GameTestFailure("no condition " + key);
    }

    // ------------------------------------------------------------------ the conditions

    /** Each condition alone holds the friend back; all together he is recommended (chat) and the tab Friend shows him. */
    @GameTest(template = EMPTY, batch = "bettacraft_m69_conditions", timeoutTicks = 200)
    public static void friendConditions(GameTestHelper h) {
        floor(h);
        home(h);
        ServerPlayer sp = player(h);
        MinecraftServer server = h.getLevel().getServer();
        try {
            AquaristEntity first = npc(h, sp);
            List<TankBlockEntity> ts = tanks(h, sp.getUUID(), 5);
            AquaristRecords.PlayerRecord r = ripe(server, sp.getUUID(), 8);
            check(ok(server, sp.getUUID(), r, "days") && ok(server, sp.getUUID(), r, "breeding") && ok(server, sp.getUUID(), r, "calm")
                    && ok(server, sp.getUUID(), r, "limit"), "everything but the tanks");
            check(!ok(server, sp.getUUID(), r, "tanks") && !AquaristTeam.ready(server, sp.getUUID(), r), "5 tanks: not enough work for two (6)");
            ts.add(tankAt(h, 5, sp.getUUID()));
            check(AquaristTeam.ready(server, sp.getUUID(), r), "6 tanks: ready " + AquaristTeam.conditions(server, sp.getUUID(), r));
            // a week without a break: six days are not enough, and an unpaid day starts the streak again
            ripe(server, sp.getUUID(), 6);
            check(!ok(server, sp.getUUID(), r, "days"), "six days are not a week");
            ripe(server, sp.getUUID(), 8);
            r.team.get(0).paidSince = AquaristTeam.now(server) - 24000L;
            check(!ok(server, sp.getUUID(), r, "days"), "an unpaid day a day ago: the streak starts again");
            ripe(server, sp.getUUID(), 8);
            r.lastHire = AquaristTeam.now(server) - 2 * 24000L;
            check(!ok(server, sp.getUUID(), r, "days"), "the last hiring two days ago");
            // another breeding
            ripe(server, sp.getUUID(), 8);
            r.breedings = r.breedingsAtHire;
            check(!ok(server, sp.getUUID(), r, "breeding"), "no breeding since the last hiring");
            // no trouble: a tank with bad water in his care
            ripe(server, sp.getUUID(), 8);
            ts.get(0).getTank().setWaterQuality(0.05F);
            first.setNoAi(false);
            invokeNoteTeam(h, first);
            check(!ok(server, sp.getUUID(), r, "calm") && r.team.get(0).trouble != Long.MIN_VALUE, "bad water in his care is trouble");
            ts.get(0).getTank().setWaterQuality(1F);
            ripe(server, sp.getUUID(), 8);
            check(ok(server, sp.getUUID(), r, "calm"), "trouble days ago is forgotten");
            // ready: the server tick recommends him, once
            check(!r.friendOffered, "not yet offered");
            AquaristTeam.offer(server, sp, r);
            check(r.friendOffered && !r.friendName.isEmpty() && !r.friendName.equals(first.getName().getString()), "offered, with a name of his own: " + r.friendName);
            CompoundTag v = AquaristMenu.view(sp, first);
            check(v.getBooleanOr("mediator", false) && v.getCompoundOrEmpty("friend").getBooleanOr("offered", false), "the tab Friend of the first shows the offer");
            check(v.getListOrEmpty("team").size() == 1 && v.getIntOr("team_max", 0) == 3, "the team tab: 1 of 3");
            h.succeed();
        } finally {
            cleanup(h, sp.getUUID());
        }
    }

    /** The aquarist's every-5-seconds look at his tanks (bad water = trouble), as the GameTest cannot wait for it. */
    static void invokeNoteTeam(GameTestHelper h, AquaristEntity npc) {
        try {
            var m = AquaristEntity.class.getDeclaredMethod("noteTeam", ServerLevel.class);
            m.setAccessible(true);
            m.invoke(npc, h.getLevel());
        } catch (ReflectiveOperationException e) {
            throw new GameTestFailure("noteTeam: " + e);
        }
    }

    // ------------------------------------------------------------------ hiring and refusing

    /** Hired, the friend comes to the first and takes the second home; the team has two, the conditions start again. */
    @GameTest(template = EMPTY, batch = "bettacraft_m69_hire", timeoutTicks = 200)
    public static void friendHiredTakesTheSecondHome(GameTestHelper h) {
        floor(h);
        home(h);
        home2(h);
        ServerPlayer sp = player(h);
        MinecraftServer server = h.getLevel().getServer();
        try {
            AquaristEntity first = npc(h, sp);
            tanks(h, sp.getUUID(), 6);
            AquaristRecords.PlayerRecord r = ripe(server, sp.getUUID(), 8);
            AquaristTeam.offer(server, sp, r);
            String name = r.friendName;
            check(AquaristTeam.hireFriend(sp) == null, "hired");
            AquaristEntity friend = AquaristTeam.lastHired;
            check(friend != null && friend.isAlive() && friend.phase() == AquaristEntity.Phase.HIRED, "the friend is there and hired");
            check(friend.getName().getString().equals(name), "he keeps the name of the offer: " + friend.getName().getString());
            check(friend.distanceToSqr(first) <= 12 * 12, "he comes next to the first");
            check(friend.housed() && h.absolutePos(BASIN2).equals(friend.workplace()) && h.absolutePos(BED2.south()).equals(friend.bed()),
                    "he takes the second home: " + friend.workplace() + " " + friend.bed());
            check(h.absolutePos(BASIN).equals(first.workplace()), "the first keeps his");
            check(r.hiredCount() == 2 && !r.friendOffered && r.mediator().id.equals(first.getUUID()), "two in the team, the first recommends");
            check(!ok(server, sp.getUUID(), r, "days") && !ok(server, sp.getUUID(), r, "breeding") && !ok(server, sp.getUUID(), r, "tanks"),
                    "the next friend: again a week, a breeding and 9 tanks");
            check(AquaristTeam.hireFriend(sp) != null, "no second hiring of the same offer");
            CompoundTag v = AquaristMenu.view(sp, friend);
            check(!v.getBooleanOr("mediator", false) && !v.contains("friend") && v.getListOrEmpty("team").size() == 2, "the friend's menu: team of 2, no tab Friend");
            h.succeed();
        } finally {
            cleanup(h, sp.getUUID());
        }
    }

    /** Without a free home the friend waits next to the first: no homeless day counts until his grace runs out. */
    @GameTest(template = EMPTY, batch = "bettacraft_m69_wait", timeoutTicks = 200)
    public static void friendWaitsForAHome(GameTestHelper h) {
        floor(h);
        home(h);
        ServerPlayer sp = player(h);
        MinecraftServer server = h.getLevel().getServer();
        ServerLevel level = h.getLevel();
        int radius = ServerConfig.AQUARIST_HOME_RADIUS.get();
        try {
            // M69: the free basins of other tests' structures nearby must not give him a home: he looks only 2 blocks around him
            ServerConfig.AQUARIST_HOME_RADIUS.set(2);
            npc(h, sp);
            tanks(h, sp.getUUID(), 6);
            AquaristRecords.PlayerRecord r = ripe(server, sp.getUUID(), 8);
            AquaristTeam.offer(server, sp, r);
            check(AquaristTeam.hireFriend(sp) == null, "hired");
            AquaristEntity friend = AquaristTeam.lastHired;
            check(!friend.housed() && friend.waitingForHome(), "no free home: he waits");
            check("waiting_home".equals(AquaristMenu.view(sp, friend).getStringOr("status", "")), "his menu says so");
            for (int d = 200; d < 206; d++) friend.payDay(level, d);
            check(friend.phase() == AquaristEntity.Phase.HIRED && friend.homelessDays() == 0, "six days of waiting: still there, no homeless day");
            friend.setGraceUntil(AquaristTeam.now(server) - 1);
            friend.payDay(level, 206);
            check(friend.homelessDays() == 1, "after the grace the days without a home count");
            ServerConfig.AQUARIST_HOME_RADIUS.set(radius);
            home2(h);
            friend.findHome(level);
            check(friend.housed(), "a home built: he moves in");
            h.succeed();
        } finally {
            ServerConfig.AQUARIST_HOME_RADIUS.set(radius);
            cleanup(h, sp.getUUID());
        }
    }

    /** Refused: no friend before the wait of a refusal, and another breeding is needed. */
    @GameTest(template = EMPTY, batch = "bettacraft_m69_refuse", timeoutTicks = 200)
    public static void friendRefused(GameTestHelper h) {
        floor(h);
        home(h);
        ServerPlayer sp = player(h);
        MinecraftServer server = h.getLevel().getServer();
        try {
            npc(h, sp);
            tanks(h, sp.getUUID(), 6);
            AquaristRecords.PlayerRecord r = ripe(server, sp.getUUID(), 8);
            AquaristTeam.offer(server, sp, r);
            check(AquaristTeam.declineFriend(sp) == null, "refused");
            check(!r.friendOffered && r.friendsRefused == 1 && r.hiredCount() == 1, "nobody came");
            check(r.friendNotBefore >= AquaristTeam.now(server) + 5 * 24000L - 1, "the next after 5 to 10 days");
            check(!ok(server, sp.getUUID(), r, "wait") && !ok(server, sp.getUUID(), r, "breeding"), "the wait and another breeding");
            check(AquaristTeam.declineFriend(sp) != null && AquaristTeam.hireFriend(sp) != null, "nothing to answer any more");
            h.succeed();
        } finally {
            cleanup(h, sp.getUUID());
        }
    }

    // ------------------------------------------------------------------ the limit, the switch

    /** maxAquarists = 2: with two the team is full ("Team voll (2/2)"); 1 = as before M69: never a friend. */
    @GameTest(template = EMPTY, batch = "bettacraft_m69_limit", timeoutTicks = 200)
    public static void teamLimitFromTheConfig(GameTestHelper h) {
        floor(h);
        home(h);
        home2(h);
        ServerPlayer sp = player(h);
        MinecraftServer server = h.getLevel().getServer();
        int max = ServerConfig.AQUARIST_MAX_COUNT.get();
        try {
            ServerConfig.AQUARIST_MAX_COUNT.set(2);
            AquaristEntity first = npc(h, sp);
            tanks(h, sp.getUUID(), 9);
            AquaristRecords.PlayerRecord r = ripe(server, sp.getUUID(), 8);
            AquaristTeam.offer(server, sp, r);
            check(AquaristTeam.hireFriend(sp) == null, "the second is hired");
            ripe(server, sp.getUUID(), 8);
            check(!ok(server, sp.getUUID(), r, "limit") && !AquaristTeam.ready(server, sp.getUUID(), r), "2 of 2: full");
            r.friendOffered = true; // an offer from before the limit was lowered
            check(AquaristTeam.hireFriend(sp) != null && r.hiredCount() == 2, "no third");
            CompoundTag v = AquaristMenu.view(sp, first);
            check(v.getIntOr("team_hired", 0) == 2 && v.getIntOr("team_max", 0) == 2, "the tab Friend: team full (2/2)");
            // 1: the behaviour before M69
            ServerConfig.AQUARIST_MAX_COUNT.set(1);
            check(AquaristTeam.max() == 1 && !AquaristTeam.ready(server, sp.getUUID(), ripe(server, sp.getUUID(), 30)), "1: never a friend");
            h.succeed();
        } finally {
            ServerConfig.AQUARIST_MAX_COUNT.set(max);
            cleanup(h, sp.getUUID());
        }
    }

    /** The switch features.aquaristNpc.team off: no friend, an offer is withdrawn; the hired ones stay. */
    @GameTest(template = EMPTY, batch = "bettacraft_m69_switch", timeoutTicks = 200)
    public static void teamSwitchOff(GameTestHelper h) {
        floor(h);
        home(h);
        home2(h);
        ServerPlayer sp = player(h);
        MinecraftServer server = h.getLevel().getServer();
        boolean on = Features.own(Feature.AQUARIST_TEAM);
        try {
            AquaristEntity first = npc(h, sp);
            AquaristEntity second = AquaristTeam.spawnMember(h.getLevel(), sp.getUUID(), "tester", h.absoluteVec(new Vec3(18.5, 1, 6.5)));
            second.setHome(h.getLevel(), h.absolutePos(BASIN2), h.absolutePos(BED2.south()));
            tanks(h, sp.getUUID(), 9);
            AquaristRecords.PlayerRecord r = ripe(server, sp.getUUID(), 8);
            check(AquaristTeam.ready(server, sp.getUUID(), r), "on: ready for the third");
            r.friendOffered = true;
            Features.set(Feature.AQUARIST_TEAM, false);
            check(AquaristTeam.max() == 1 && !AquaristTeam.ready(server, sp.getUUID(), r), "off: no friend");
            var tick = AquaristTeam.class.getDeclaredMethod("tick", MinecraftServer.class);
            tick.setAccessible(true);
            tick.invoke(null, server);
            check(!r.friendOffered, "off: the offer is withdrawn");
            check(first.isAlive() && second.isAlive() && r.hiredCount() == 2, "the hired ones stay");
            check(!AquaristMenu.view(sp, first).getBooleanOr("team_feature", false), "the menu knows (no tab Friend)");
            h.succeed();
        } catch (ReflectiveOperationException e) {
            throw new GameTestFailure("tick: " + e);
        } finally {
            Features.set(Feature.AQUARIST_TEAM, on);
            cleanup(h, sp.getUUID());
        }
    }

    // ------------------------------------------------------------------ no double work

    /** Every tank has exactly one aquarist: the nearest workplace; given to the other in the menu it moves; back to auto. */
    @GameTest(template = EMPTY, batch = "bettacraft_m69_share", timeoutTicks = 200)
    public static void everyTankHasOneAquarist(GameTestHelper h) {
        floor(h);
        home(h);
        home2(h);
        ServerPlayer sp = player(h);
        ServerLevel level = h.getLevel();
        MinecraftServer server = level.getServer();
        try {
            AquaristEntity a = npc(h, sp);
            AquaristEntity b = AquaristTeam.spawnMember(level, sp.getUUID(), "tester", h.absoluteVec(new Vec3(18.5, 1, 6.5)));
            check(b.setHome(level, h.absolutePos(BASIN2), h.absolutePos(BED2.south())), "the second home");
            b.alwaysDay = true;
            b.setNoAi(true);
            List<TankBlockEntity> ts = tanks(h, sp.getUUID(), 4); // x 6, 10 near A (x 4), x 16, 22 near B (x 20)
            List<TankBlockEntity> ofA = AquaristWork.tanks(level, a), ofB = AquaristWork.tanks(level, b);
            Set<UUID> ia = ids(ofA), ib = ids(ofB);
            check(ia.size() == 2 && ib.size() == 2, "two each: " + ia.size() + " / " + ib.size());
            check(ia.contains(ts.get(0).getTank().id()) && ia.contains(ts.get(1).getTank().id()), "A: the two near his basin");
            check(ib.contains(ts.get(2).getTank().id()) && ib.contains(ts.get(3).getTank().id()), "B: the two near his");
            Set<UUID> both = new HashSet<>(ia);
            both.retainAll(ib);
            check(both.isEmpty(), "no tank twice");
            // the menu gives a tank of A to B
            UUID t0 = ts.get(0).getTank().id();
            check(AquaristTeam.assign(server, sp.getUUID(), t0, b.getUUID()) == null, "given to B");
            check(!ids(AquaristWork.tanks(level, a)).contains(t0) && ids(AquaristWork.tanks(level, b)).contains(t0), "now B's alone");
            CompoundTag v = AquaristMenu.view(sp, a);
            boolean shown = false;
            for (Tag x : v.getListOrEmpty("tanks")) {
                CompoundTag c = (CompoundTag) x;
                if (NbtCompat.getUUID(c, "id").equals(t0)) shown = NbtCompat.getUUID(c, "who").equals(b.getUUID()) && c.getBooleanOr("given", false);
            }
            check(shown, "A's tank list says B looks after it (chosen)");
            check(AquaristTeam.assign(server, sp.getUUID(), t0, null) == null && ids(AquaristWork.tanks(level, a)).contains(t0), "auto: back to A");
            // the team tab of either shows both
            check(AquaristMenu.view(sp, b).getListOrEmpty("team").size() == 2, "the team tab of B: both");
            h.succeed();
        } finally {
            cleanup(h, sp.getUUID());
        }
    }

    static Set<UUID> ids(List<TankBlockEntity> l) {
        Set<UUID> out = new HashSet<>();
        for (TankBlockEntity c : l) out.add(c.getTank().id());
        return out;
    }

    // ------------------------------------------------------------------ the first leaves

    /** The first leaves: the next one (with the player longest) recommends; the team goes on, no "next aquarist" wait. */
    @GameTest(template = EMPTY, batch = "bettacraft_m69_succession", timeoutTicks = 200)
    public static void theNextRecommendsWhenTheFirstLeaves(GameTestHelper h) {
        floor(h);
        home(h);
        home2(h);
        ServerPlayer sp = player(h);
        ServerLevel level = h.getLevel();
        try {
            AquaristEntity a = npc(h, sp);
            AquaristEntity b = AquaristTeam.spawnMember(level, sp.getUUID(), "tester", h.absoluteVec(new Vec3(18.5, 1, 6.5)));
            b.setHome(level, h.absolutePos(BASIN2), h.absolutePos(BED2.south()));
            AquaristRecords.PlayerRecord r = AquaristRecords.get(level.getServer()).player(sp.getUUID());
            r.team.get(0).since = AquaristTeam.now(level.getServer()) - 50 * 24000L; // A is the older one
            check(AquaristTeam.isMediator(level.getServer(), a) && !AquaristTeam.isMediator(level.getServer(), b), "A recommends");
            long before = r.notBefore;
            AquaristArrival.leave(level, a, "dismissed");
            check(r.hiredCount() == 1 && AquaristTeam.isMediator(level.getServer(), b), "B recommends now");
            check(r.notBefore == before && r.left == 1, "the team goes on: no wait for a new first aquarist");
            check(AquaristArrival.whyNot(sp, true) != null, "no new first aquarist comes while B works");
            h.succeed();
        } finally {
            cleanup(h, sp.getUUID());
        }
    }

    // ------------------------------------------------------------------ an old world

    /** The records of a world before M69 (one aquarist, npc + hired) load as a team of one; his times start now. */
    @GameTest(template = EMPTY, batch = "bettacraft_m69_migration", timeoutTicks = 100)
    public static void oldRecordsKeepTheirAquarist(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        UUID player = UUID.randomUUID(), npc = UUID.randomUUID();
        CompoundTag old = new CompoundTag();
        ListTag players = new ListTag();
        CompoundTag p = new CompoundTag();
        NbtCompat.putUUID(p, "player", player);
        NbtCompat.putUUID(p, "npc", npc);
        p.putBoolean("hired", true);
        p.putInt("breedings", 3);
        p.putString("name", "old");
        players.add(p);
        old.put("players", players);
        old.put("tanks", new ListTag());
        AquaristRecords rec = AquaristRecords.load(old, server.registryAccess());
        AquaristRecords.PlayerRecord r = rec.peek(player);
        check(r != null && npc.equals(r.npc()) && r.hired() && r.hiredCount() == 1, "his aquarist is still hired");
        check(r.breedingsAtHire == 3 && r.mediator() != null && r.mediator().id.equals(npc), "he recommends; the next friend needs another breeding");
        check(rec.isDirty(), "migrated: saved again");
        AquaristTeam.conditions(server, player, r);
        long now = AquaristTeam.now(server);
        check(r.team.get(0).since == now && r.lastHire == now, "his days count from now");
        CompoundTag saved = rec.save(new CompoundTag(), server.registryAccess());
        AquaristRecords again = AquaristRecords.load(saved, server.registryAccess());
        check(npc.equals(again.peek(player).npc()) && again.peek(player).hired(), "saved and loaded again");
        h.succeed();
    }
}
