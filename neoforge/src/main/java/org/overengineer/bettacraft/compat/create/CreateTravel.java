package org.overengineer.bettacraft.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.platform.energy.IEnergyStorage;
import org.overengineer.bettacraft.platform.network.PacketDistributor;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity;
import org.overengineer.bettacraft.equipment.FilterHost;
import org.overengineer.bettacraft.network.TravellingTankPayload;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TravelCare;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M50 (A1, C1): the contraption side of {@link TravelCare}, compiled against Create and only loaded with it. Every tick of a
 * trip the tank's movement behaviour hands its context here (server); every second the data of the controller is advanced
 * with what travels along on the same contraption:
 * <ul>
 *     <li>the external filters next to the tank and the cabinets below it (their block entities are read from the
 *     contraption's block data),</li>
 *     <li>drive adapters at the tank: they turn with the trip (no Create network turns on a contraption), at
 *     {@code createTravel.dynamoFullSpeed} blocks per second they give an input's full power into the tank's buffer,</li>
 *     <li>FE batteries of other mods next to the tank: a copy of their block entity is made from the contraption's data,
 *     the FE is taken through its energy capability and the data written back.</li>
 * </ul>
 * Every two seconds the players who see the contraption get the tank's state ({@link TravellingTankPayload}) for the
 * drawing: the values on the sign, the animals as they are now.
 */
public final class CreateTravel {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/create-travel");
    static final int STEP = 20;
    static final int SYNC = 40;
    private static boolean failedOnce;

    private CreateTravel() {}

    /** Transient state of one travelling controller (Create's temporary data of the context). */
    private static final class State {
        /** Sum over the ticks of the drive adapters' shares of the full dynamo speed. */
        double motion;
        int ticks;
        long synced = Long.MIN_VALUE;
        /** The drive adapters at the tank (contraption-local) and where they were in the world the tick before. */
        List<BlockPos> adapters;
        final Map<BlockPos, Vec3> prev = new java.util.HashMap<>();
    }

    /** Server, every tick of the trip. */
    public static void tick(Object context) {
        if (!(context instanceof MovementContext ctx) || !(ctx.world instanceof ServerLevel level)) return;
        CompoundTag nbt = ctx.blockEntityData;
        if (nbt == null || !nbt.contains("tank", Tag.TAG_COMPOUND) || !TravelCare.enabled()) return;
        try {
            State s = ctx.temporaryData instanceof State st ? st : new State();
            ctx.temporaryData = s;
            if (s.adapters == null) s.adapters = around(level, TankData.load(nbt.getCompound("tank")), offset(ctx, nbt), ctx.contraption.getBlocks()).adapters();
            // each drive adapter turns by how fast it moves through the world (on a bearing: by its distance from the axis)
            double full = Math.max(0.1, ServerConfig.getOr(ServerConfig.TRAVEL_DYNAMO_SPEED, 8.0));
            if (!s.adapters.isEmpty() && ctx.contraption.entity != null) {
                for (BlockPos p : s.adapters) {
                    Vec3 w = ctx.contraption.entity.toGlobalVector(Vec3.atCenterOf(p), 1F);
                    Vec3 last = s.prev.put(p, w);
                    if (last != null) s.motion += Math.min(1.0, w.distanceTo(last) * 20.0 / full);
                }
            }
            s.ticks++;
            long now = level.getGameTime();
            CompoundTag trip = nbt.getCompound(TravelCare.TRIP_KEY);
            if (!trip.contains("at", Tag.TAG_LONG) || now < trip.getLong("at")) {
                trip.putLong("at", now);
                nbt.put(TravelCare.TRIP_KEY, trip);
                return;
            }
            long dt = now - trip.getLong("at");
            if (dt >= STEP) {
                if (!ServerConfig.getOr(ServerConfig.SIMULATE_UNLOADED_TIME, false)) dt = Math.min(dt, STEP * 5L);
                double motion = s.ticks <= 0 ? 0 : s.motion / s.ticks;
                s.motion = 0;
                s.ticks = 0;
                step(level, ctx, nbt, dt, motion);
                nbt.getCompound(TravelCare.TRIP_KEY).putLong("at", now);
            }
            if (s.synced == Long.MIN_VALUE || now - s.synced >= SYNC || now < s.synced) {
                s.synced = now;
                sync(ctx, nbt);
            }
        } catch (RuntimeException | LinkageError e) {
            if (!failedOnce) {
                failedOnce = true;
                LOGGER.error("The care of a tank on a contraption failed", e);
            }
        }
    }

    /** The contraption's blocks around the tank: filters, cabinets, drive adapters, batteries (local positions). */
    private record Around(List<FilterHost> hosts, List<BlockPos> adapters, List<BlockPos> batteries) {}

    /** Old world position -> contraption-local position of the tank's data (by the controller's own place). */
    private static BlockPos offset(MovementContext ctx, CompoundTag nbt) {
        int[] c = nbt.getIntArray("controller");
        BlockPos oldController = c.length == 3 ? new BlockPos(c[0], c[1], c[2]) : TankData.load(nbt.getCompound("tank")).anchor();
        return ctx.localPos.subtract(oldController);
    }

    /**
     * One care step of {@code ticks} ticks ({@code motion}: the drive adapters' shares of the full dynamo speed, summed, on
     * average over the ticks since the last step).
     */
    static TravelCare.Result step(ServerLevel level, MovementContext ctx, CompoundTag nbt, long ticks, double motion) {
        TankData t = TankData.load(nbt.getCompound("tank"));
        BlockPos offset = offset(ctx, nbt);
        Contraption contraption = ctx.contraption;
        Map<BlockPos, StructureBlockInfo> blocks = contraption.getBlocks();
        Around around = around(level, t, offset, blocks);
        if (ctx.temporaryData instanceof State s) s.adapters = around.adapters();
        double dynamo = 0;
        if (!around.adapters().isEmpty() && Features.enabled(Feature.CREATE_DRIVE)) {
            dynamo = Math.min(ServerConfig.getOr(ServerConfig.DRIVE_MAX_FE, 1024), ServerConfig.getOr(ServerConfig.DRIVE_FE, 256) * Math.max(0, motion));
        }
        Vec3 where = ctx.position != null ? ctx.position : contraption.entity != null ? contraption.entity.position() : Vec3.atCenterOf(t.anchor().offset(offset));
        float ambient = TravelCare.ambientAt(level, where);
        List<BlockPos> batteries = around.batteries();
        long perBattery = (long) ServerConfig.getOr(ServerConfig.TRAVEL_BATTERY_FE, 256) * ticks;
        TravelCare.Battery battery = fe -> {
            long got = 0;
            for (BlockPos p : batteries) {
                if (got >= fe) break;
                got += drain(level, blocks, p, offset, Math.min(fe - got, perBattery));
            }
            return got;
        };
        TravelCare.Env env = TravelCare.interior(level, t, ambient, around.hosts(), dynamo, battery);
        return TravelCare.step(level, nbt, env, ticks);
    }

    private static Around around(ServerLevel level, TankData t, BlockPos offset, Map<BlockPos, StructureBlockInfo> blocks) {
        Set<BlockPos> members = new HashSet<>();
        for (long l : t.members()) members.add(BlockPos.of(l).offset(offset));
        List<FilterHost> hosts = new ArrayList<>();
        List<BlockPos> batteries = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        List<BlockPos> adapters = new ArrayList<>();
        for (BlockPos m : members) {
            for (Direction d : Direction.values()) {
                BlockPos n = m.relative(d);
                if (members.contains(n) || !seen.add(n)) continue;
                StructureBlockInfo info = blocks.get(n);
                if (info == null) continue;
                if (info.state().getBlock() instanceof DriveAdapterBlock) {
                    if (info.state().getValue(DriveAdapterBlock.FACING) == d) adapters.add(n);
                    continue;
                }
                if (info.nbt() == null || info.nbt().isEmpty()) continue;
                String ns = BuiltInRegistries.BLOCK.getKey(info.state().getBlock()).getNamespace();
                if (BettaCraft.MOD_ID.equals(ns)) {
                    BlockEntity be = virtual(level, info, n.subtract(offset));
                    if (be instanceof CabinetBlockEntity cab && d == Direction.DOWN) hosts.add(cab);
                    else if (be instanceof ExternalFilterBlockEntity f && f.tankBlockPos() != null && t.contains(f.tankBlockPos())) hosts.add(f);
                } else if (!"create".equals(ns)) {
                    batteries.add(n);
                }
            }
        }
        return new Around(hosts, adapters, batteries);
    }

    /** A copy of a block entity of the contraption from its data (never in the world), at its old world position. */
    @Nullable
    private static BlockEntity virtual(ServerLevel level, StructureBlockInfo info, BlockPos at) {
        try {
            BlockEntity be = BlockEntity.loadStatic(at, info.state(), info.nbt(), level.registryAccess());
            if (be != null) be.setLevel(level);
            return be;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Takes up to {@code fe} FE from the battery at {@code local} (a copy from its data; the data is written back). */
    private static long drain(ServerLevel level, Map<BlockPos, StructureBlockInfo> blocks, BlockPos local, BlockPos offset, long fe) {
        if (fe <= 0) return 0;
        StructureBlockInfo info = blocks.get(local);
        if (info == null || info.nbt() == null) return 0;
        BlockPos at = local.subtract(offset);
        BlockEntity be = virtual(level, info, at);
        if (be == null) return 0;
        try {
            IEnergyStorage es = null;
            for (Direction d : Direction.values()) {
                es = org.overengineer.bettacraft.platform.Platform.get().energyStorage(level, at, info.state(), be, d);
                if (es != null && es.canExtract()) break;
                es = null;
            }
            if (es == null) return 0;
            int got = es.extractEnergy((int) Math.min(Integer.MAX_VALUE, fe), false);
            if (got <= 0) return 0;
            CompoundTag saved = be.saveWithFullMetadata(level.registryAccess());
            CompoundTag nbt = info.nbt();
            for (String k : new ArrayList<>(nbt.getAllKeys())) if (!saved.contains(k)) nbt.remove(k);
            for (String k : saved.getAllKeys()) nbt.put(k, saved.get(k));
            return got;
        } catch (RuntimeException e) {
            LOGGER.debug("A battery on a contraption gave no FE", e);
            return 0;
        }
    }

    /** The state of the tank for the players who see the contraption (the drawing on it). */
    private static void sync(MovementContext ctx, CompoundTag nbt) {
        if (!(ctx.contraption.entity instanceof AbstractContraptionEntity e)) return;
        CompoundTag view = view(nbt);
        if (view == null) return;
        PacketDistributor.sendToPlayersTrackingEntity(e, new TravellingTankPayload(e.getId(), ctx.localPos, view));
    }

    /** What a client's copy of the controller loads (like its update tag): the client view of the data and the packed animals. */
    @Nullable
    public static CompoundTag view(CompoundTag nbt) {
        if (!nbt.contains("tank", Tag.TAG_COMPOUND)) return null;
        CompoundTag v = nbt.copy();
        v.remove("tank");
        v.remove("at");
        v.remove(TravelCare.TRIP_KEY);
        v.put("tank_view", TankData.load(nbt.getCompound("tank")).saveClientView());
        return v;
    }
}
