package org.overengineer.bettacraft.monitor;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.crucible.CrucibleBlockEntity;
import org.overengineer.bettacraft.network.MonitorDataPayload;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;

/**
 * M27: the work of a monitor screen every {@link #INTERVAL} ticks (in its controller, all screens in the same tick):
 * <ol>
 * <li>Power: while a power option is on ({@link #powerRequired}) it takes {@code monitor.fePerBlock} FE per tick and block,
 * else it shows "No power". With {@code features.monitors.enabled} off it is dark.</li>
 * <li>Viewers: the players tracking the chunk (their render distance) within {@link #VIEW_DISTANCE} blocks. Without
 * viewers nothing else happens: no tank lookups, no packets.</li>
 * <li>Tanks: every {@link #DISCOVER_EVERY} cycles it looks through the loaded chunks around it for tank controllers within
 * {@code monitor.range} blocks (nearest point of the tank) and remembers them by id ({@link MonitorTank}); in between
 * it reads the values of the tanks it knows. Unloaded or out of range: "not loaded" with the last values; a tank whose
 * place is loaded but that is gone is forgotten.</li>
 * <li>Sync: the tanks go to the viewers when they changed (or to new viewers, and every {@link #RESEND_EVERY} cycles); the
 * history lines the tiles need when a new hour was recorded. Everything a player gets in one tick goes in one
 * {@link MonitorDataPayload} ({@link #onServerTick}).</li>
 * <li>Alarms (part b): the alarm tiles with redstone are checked every cycle; while one is past its limit the screen gives
 * a redstone signal ({@link MonitorBlockEntity#alarms}). Since M32 part d a screen with any alarm tile reads its tanks
 * also without viewers: {@link MonitorAlarms} posts the alarm events and opens the map waypoints.</li>
 * </ol>
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class MonitorSync {
    public static final int INTERVAL = 20;
    public static final int DISCOVER_EVERY = 5;
    public static final int RESEND_EVERY = 10;
    public static final int VIEW_DISTANCE = 96;
    public static final int MAX_TANKS = MonitorDataPayload.MAX_TANKS;

    private static final Map<ServerPlayer, List<MonitorDataPayload.Entry>> PENDING = new LinkedHashMap<>();
    /** Payloads sent (for the GameTests). */
    public static int sentPayloads;

    private MonitorSync() {}

    public static int range() {
        return ServerConfig.getOr(ServerConfig.MONITOR_RANGE, 32);
    }

    /** Monitors need FE while any power option is on (like the food maker and the crucible). */
    public static boolean powerRequired() {
        return CrucibleBlockEntity.energyOffered();
    }

    public static int fePerBlock() {
        return ServerConfig.getOr(ServerConfig.MONITOR_FE_PER_BLOCK, 1);
    }

    public static void cycle(ServerLevel level, MonitorBlockEntity c) {
        int status;
        if (!Features.enabled(Feature.MONITORS)) status = MonitorBlockEntity.OFF;
        else if (!powerRequired()) status = MonitorBlockEntity.ON;
        else status = c.energy().use(fePerBlock() * c.area() * INTERVAL) ? MonitorBlockEntity.ON : MonitorBlockEntity.NO_POWER;
        c.setStatus(status);
        List<ServerPlayer> viewers = viewers(level, c);
        if (viewers.isEmpty() || status != MonitorBlockEntity.ON) {
            c.sentTo.clear();
            c.historySentTo.clear();
            // part b: alarms with redstone watch their tanks even when nobody looks; M32 part d: every alarm tile does (events, waypoints)
            if (status == MonitorBlockEntity.ON && c.layout().hasAlarm()) work(level, c);
            c.setAlarms(status == MonitorBlockEntity.ON ? c.layout().triggeredAlarms(c::tank) : 0);
            MonitorAlarms.update(level, c, status == MonitorBlockEntity.ON);
            return;
        }
        work(level, c);
        c.setAlarms(c.layout().triggeredAlarms(c::tank));
        MonitorAlarms.update(level, c, true);
        // the tanks
        int hash = 1;
        for (MonitorTank t : c.tanks()) hash = hash * 31 + t.contentHash();
        boolean all = hash != c.lastTankHash || c.cycles % RESEND_EVERY == 0;
        c.lastTankHash = hash;
        List<MonitorTank> tanks = null;
        // the history lines the tiles show
        Map<UUID, Set<MonitorValue>> needs = c.layout().historyNeeds();
        TankHistory history = needs.isEmpty() ? null : TankHistory.get(level);
        List<MonitorDataPayload.HistoryLine> lines = new ArrayList<>();
        int hHash = 7;
        if (history != null) {
            for (Map.Entry<UUID, Set<MonitorValue>> e : needs.entrySet()) {
                TankHistory.Series s = history.series(e.getKey());
                if (s == null) continue;
                for (MonitorValue v : e.getValue()) {
                    lines.add(new MonitorDataPayload.HistoryLine(e.getKey(), v, s.lastHour(), s.values(v, TankHistory.HOURS)));
                    hHash = hHash * 31 + e.getKey().hashCode() * 17 + v.ordinal() * 7 + Long.hashCode(s.lastHour()) + s.size();
                }
                if (lines.size() >= MonitorDataPayload.MAX_LINES) break;
            }
        }
        if (lines.size() > MonitorDataPayload.MAX_LINES) lines = lines.subList(0, MonitorDataPayload.MAX_LINES);
        boolean allHistory = hHash != c.lastHistoryHash;
        c.lastHistoryHash = hHash;
        Set<UUID> now = new HashSet<>();
        for (ServerPlayer p : viewers) {
            now.add(p.getUUID());
            boolean sendTanks = all || !c.sentTo.contains(p.getUUID());
            boolean sendHistory = allHistory || !c.historySentTo.contains(p.getUUID());
            if (!sendTanks && !sendHistory) continue;
            if (sendTanks && tanks == null) tanks = copy(c.tanks());
            PENDING.computeIfAbsent(p, k -> new ArrayList<>()).add(new MonitorDataPayload.Entry(c.getBlockPos(), sendTanks ? tanks : null,
                    sendHistory ? lines : null));
        }
        c.sentTo.clear();
        c.sentTo.addAll(now);
        c.historySentTo.clear();
        c.historySentTo.addAll(now);
    }

    /** Finds the tanks (every {@link #DISCOVER_EVERY} cycles) or reads their values, and makes the automatic layout again. */
    public static void work(ServerLevel level, MonitorBlockEntity c) {
        if (c.cycles++ % DISCOVER_EVERY == 0) discover(level, c);
        else refresh(level, c);
        if (c.layout().auto) {
            DashboardLayout a = DashboardLayout.autoLayout(c.tanks(), c.width(), c.height());
            if (!a.sameAs(c.layout())) c.setLayout(a);
        }
    }

    private static List<MonitorTank> copy(List<MonitorTank> tanks) {
        List<MonitorTank> out = new ArrayList<>(tanks.size());
        for (MonitorTank t : tanks) out.add(t.copy());
        return out;
    }

    /** The players that see the monitor: they track its chunk and are within {@link #VIEW_DISTANCE} blocks. */
    public static List<ServerPlayer> viewers(ServerLevel level, MonitorBlockEntity c) {
        Vec3 center = c.screenCenter();
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : level.getChunkSource().chunkMap.getPlayers(new ChunkPos(c.getBlockPos()), false)) {
            if (p.distanceToSqr(center) <= (double) VIEW_DISTANCE * VIEW_DISTANCE) out.add(p);
        }
        return out;
    }

    /** Distance from {@code p} to the nearest point of the tank's box. */
    static double distance(Vec3 p, TankData t) {
        BlockPos mn = t.min(), mx = t.max();
        double dx = Math.max(0, Math.max(mn.getX() - p.x, p.x - (mx.getX() + 1)));
        double dy = Math.max(0, Math.max(mn.getY() - p.y, p.y - (mx.getY() + 1)));
        double dz = Math.max(0, Math.max(mn.getZ() - p.z, p.z - (mx.getZ() + 1)));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static void update(ServerLevel level, MonitorBlockEntity c, MonitorTank k, TankBlockEntity t, long now) {
        k.pos = t.getBlockPos().immutable();
        k.name = t.getTank().name();
        k.values = MonitorValue.sampleAll(level, t);
        // part b: warnings, animals, devices and the log only for the tiles that show them, and not every cycle
        if (!c.layout().needsDetail()) k.detail = new net.minecraft.nbt.CompoundTag();
        else if (k.detail.isEmpty() || c.cycles % DISCOVER_EVERY == 1) k.detail = MonitorDetail.build(level, t);
        k.seen = now;
        k.live = true;
    }

    /** Looks for the tanks in range in the loaded chunks around the screen. */
    public static void discover(ServerLevel level, MonitorBlockEntity c) {
        int range = range();
        Vec3 center = c.screenCenter();
        int scan = range + 24; // controllers sit in a corner of their tank
        int cx0 = ((int) Math.floor(center.x) - scan) >> 4, cx1 = ((int) Math.floor(center.x) + scan) >> 4;
        int cz0 = ((int) Math.floor(center.z) - scan) >> 4, cz1 = ((int) Math.floor(center.z) + scan) >> 4;
        Map<UUID, TankBlockEntity> found = new HashMap<>();
        Set<Long> scanned = new HashSet<>();
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                LevelChunk ch = level.getChunkSource().getChunkNow(cx, cz);
                if (ch == null) continue;
                scanned.add(ChunkPos.asLong(cx, cz));
                for (BlockEntity be : ch.getBlockEntities().values()) {
                    if (be instanceof TankBlockEntity t && t.isController() && !t.isRemoved()) found.put(t.getTank().id(), t);
                }
            }
        }
        long now = level.getGameTime();
        for (Iterator<MonitorTank> it = c.tanks().iterator(); it.hasNext(); ) {
            MonitorTank k = it.next();
            TankBlockEntity t = found.remove(k.id);
            if (t != null) {
                if (distance(center, t.getTank()) <= range) update(level, c, k, t, now);
                else k.live = false;
            } else {
                k.live = false;
                // its place was looked at and it is not there: broken up or gone
                if (scanned.contains(ChunkPos.asLong(k.pos.getX() >> 4, k.pos.getZ() >> 4))) it.remove();
            }
        }
        List<TankBlockEntity> fresh = new ArrayList<>();
        for (TankBlockEntity t : found.values()) if (distance(center, t.getTank()) <= range) fresh.add(t);
        fresh.sort(Comparator.comparingDouble((TankBlockEntity t) -> distance(center, t.getTank())).thenComparing(t -> t.getTank().id()));
        for (TankBlockEntity t : fresh) {
            if (c.tanks().size() >= MAX_TANKS) break;
            MonitorTank k = new MonitorTank(t.getTank().id(), "", t.getBlockPos(), true, new float[0], now);
            update(level, c, k, t, now);
            c.tanks().add(k);
        }
        c.setChanged();
    }

    /** Reads the values of the tanks it knows. */
    public static void refresh(ServerLevel level, MonitorBlockEntity c) {
        long now = level.getGameTime();
        int range = range();
        Vec3 center = c.screenCenter();
        for (MonitorTank k : c.tanks()) {
            if (!level.isLoaded(k.pos)) {
                k.live = false;
                continue;
            }
            if (level.getBlockEntity(k.pos) instanceof TankBlockEntity t && t.isController() && t.getTank().id().equals(k.id)
                    && distance(center, t.getTank()) <= range) {
                update(level, c, k, t, now);
            } else {
                k.live = false;
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) return;
        for (Map.Entry<ServerPlayer, List<MonitorDataPayload.Entry>> e : PENDING.entrySet()) {
            if (e.getKey().hasDisconnected()) continue;
            List<MonitorDataPayload.Entry> list = e.getValue();
            for (int i = 0; i < list.size(); i += MonitorDataPayload.MAX_ENTRIES) {
                PacketDistributor.sendToPlayer(e.getKey(), new MonitorDataPayload(List.copyOf(list.subList(i, Math.min(list.size(), i + MonitorDataPayload.MAX_ENTRIES)))));
                sentPayloads++;
            }
        }
        PENDING.clear();
    }
}
