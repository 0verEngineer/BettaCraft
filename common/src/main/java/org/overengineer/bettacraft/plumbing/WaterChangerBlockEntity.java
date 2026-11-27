package org.overengineer.bettacraft.plumbing;

import it.unimi.dsi.fastutil.longs.Long2LongLinkedOpenHashMap;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.filter.FilterPartBlock;
import org.overengineer.bettacraft.filter.FilterPumpHeadBlockEntity;
import org.overengineer.bettacraft.filter.FilterWater;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankNitrate;
import org.overengineer.bettacraft.tank.WaterValues;

/**
 * M24: the water change controller: it changes {@link #share} % of the water every {@link #intervalDays} days (menu). It only
 * drains what it can refill. Fresh tap water has the temperature of the place (a cool water change, M19).
 * <p>
 * M68 part B: four pipe ports ({@link WaterDeviceBlock#changerPort}): <b>fresh water in</b> (left, seen from the front: a line from
 * a reservoir, an osmosis unit, a water source with a pump, another mod's tank; pumps and other mods' pipes may also push into it),
 * <b>fresh water out</b> (back: the line to the tanks), <b>waste water in</b> (top: the line from the tanks) and <b>waste water
 * out</b> (right: a line to a drain, a reservoir or another mod's tank, or such a block right at the port; other mods' pipes may
 * also pull from it). The pipes get their role (and ring) from the port. Against a tank or a part of a multiblock filter the touch
 * stands for both tank-side ports (then two pipes do). Fresh water never comes from what merely stands next to it.
 * <p>
 * It fills every tank its fresh water line reaches and drains those its waste water line reaches (a water change needs both);
 * through a multiblock filter (touched, or a tank-side line ending at a part of it) every tank of the filter, the water going in over
 * the filter's return. A water change runs in steps of {@link #STEP_TICKS} ticks: each step moves at most what the tank-side lines
 * carry (their weakest pipe; a touch has no limit), what its waste store can take and what the fresh water line gives. Between
 * changes it tops off the tanks it fills that have a level sensor ({@link LevelSensorBlockEntity}: no cable), with fresh water only.
 * Where its waste water line docks (or it touches the tank) is a suction point: without an intake guard a change sucks in young
 * animals ({@link ChangeIntake}).
 */
public class WaterChangerBlockEntity extends BlockEntity implements org.overengineer.bettacraft.winding.Windable {
    public enum Status { WAITING, CHANGING, CHANGED, TOPPED, NO_TANK, NO_WATER, NO_DRAIN, SALTY, SHORT_CIRCUIT, NO_POWER, OFF, DISABLED }

    /** Ticks between two steps (a pipe's window). */
    public static final int STEP_TICKS = PipeNetwork.WINDOW;
    /** mB its own fresh and waste water stores hold (what pumps push in, what waits for the waste water line). */
    public static final int STORE_MB = 4000;

    /** Synced flags of what it is connected to ({@link #flags()}). */
    public static final int FRESH_LINE = 1, WASTE_LINE = 2, YOUNG = 4, SHORT = 8, CONTACT = 16, FILTER = 32, FRESH_OUT_LINE = 64, WASTE_IN_LINE = 128;

    /** Days between changes; 0 = not set yet: what the difficulty asks for ({@link #defaultInterval}). */
    private int intervalDays;
    private int share = 25;
    private boolean enabled = true;
    /** Game time of the last finished change (or of placing it: the first change comes one interval later). */
    private long last = -1;
    private Status status = Status.WAITING;
    /** mB of the last finished change (all its tanks). */
    private int lastMb;
    /** The change that is running: mB still to change per tank (key: the position of the tank's controller block). */
    private final Long2LongLinkedOpenHashMap pending = new Long2LongLinkedOpenHashMap();
    /** mB changed so far per tank of the running change (for its log line), the whole run so far, and when it started. */
    private final Long2LongLinkedOpenHashMap done = new Long2LongLinkedOpenHashMap();
    private long runMb;
    private long started = -1;
    /** mB of the last top-off step and all topped off. */
    private int lastTopOff;
    private long toppedTotal;
    /** Synced: the tanks it serves, those through a filter, the sensors it reads, {@link #FRESH_LINE} ... flags. */
    private int tanks, filterTanks, sensors, flags;
    private final DeviceEnergy energy = new DeviceEnergy(40000, this::setChanged);
    /** Fresh water pushed into its inlet, old water waiting for its outlet. */
    private final WaterStore fresh = new WaterStore(STORE_MB, this::setChanged);
    private final WaterStore waste = new WaterStore(STORE_MB, this::setChanged);
    /** M33 part d (hardening): game time of the last "change now" from the menu (one per second; not saved). */
    private long manualAt = Long.MIN_VALUE;
    /** What the clients saw last (sent again only when it changes). */
    private int syncedHash;
    /** On a client: what the server's stores and running change hold (synced). */
    private long clientPending;
    private int clientFresh, clientWaste;

    public WaterChangerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_CHANGER.get(), pos, state);
    }

    public DeviceEnergy energy() { return energy; }

    /** M37: the spring the crank winds ({@code features.power.winding}): its pump changes the water on it (a charge is a day of pump work) while its FE is short. */
    private final org.overengineer.bettacraft.winding.WindSpring spring =
            org.overengineer.bettacraft.winding.WindSpring.of(org.overengineer.bettacraft.winding.Winding.Device.WATER_CHANGER, this::setChanged);

    @Override
    public org.overengineer.bettacraft.winding.WindSpring windSpring() {
        return spring;
    }

    @Override
    public org.overengineer.bettacraft.winding.Winding.Device windDevice() {
        return org.overengineer.bettacraft.winding.Winding.Device.WATER_CHANGER;
    }

    public int intervalDays() { return intervalDays > 0 ? intervalDays : defaultInterval(); }
    public int share() { return share; }
    public boolean enabled() { return enabled; }
    public long last() { return last; }
    public Status status() { return status; }
    public int lastMb() { return lastMb; }
    /** The tanks it serves (directly or through a filter). */
    public int tanks() { return tanks; }
    /** The tanks it serves through a multiblock filter. */
    public int filterTanks() { return filterTanks; }
    /** The level sensors it reads. */
    public int sensors() { return sensors; }
    public int flags() { return flags; }
    public boolean flag(int f) { return (flags & f) != 0; }
    public int lastTopOff() { return lastTopOff; }
    public long toppedTotal() { return toppedTotal; }
    public WaterStore freshStore() { return fresh; }
    public WaterStore wasteStore() { return waste; }

    /** mB still to change in the running change (0: none runs). */
    public long pendingMb() {
        long n = 0;
        for (long v : pending.values()) n += v;
        return n;
    }

    /** mB still to change, the fresh and the waste water stores: on the server the real values, on a client the synced ones. */
    public long shownPending() { return level != null && level.isClientSide ? clientPending : pendingMb(); }
    public int shownFresh() { return level != null && level.isClientSide ? clientFresh : fresh.amount(); }
    public int shownWaste() { return level != null && level.isClientSide ? clientWaste : waste.amount(); }

    /** M33 part d: whether "change now" may run again (at most once per {@code 20} ticks, a client can send it in a loop). */
    public boolean manualChangeAllowed(long now) {
        if (manualAt != Long.MIN_VALUE && now - manualAt >= 0 && now - manualAt < 20) return false;
        manualAt = now;
        return true;
    }

    /** Menu: interval 1..60 days, share 5..50 %, on/off. */
    public void set(int interval, int sharePct, boolean on) {
        intervalDays = Math.max(1, Math.min(60, interval));
        share = Math.max(5, Math.min(50, sharePct));
        enabled = on;
        changed(true);
    }

    /** The default interval: what the difficulty asks for (7 days when the nitrate is off). */
    public static int defaultInterval() {
        int d = TankNitrate.intervalDays(TankNitrate.difficulty());
        return d <= 0 ? 7 : d;
    }

    // ------------------------------------------------------------------ ports

    /** M68 part B: the role of the port on {@code face} (null: none there). */
    @Nullable
    public PipeRole port(Direction face) {
        BlockState s = getBlockState();
        return s.hasProperty(WaterDeviceBlock.FACING) ? WaterDeviceBlock.changerPort(s.getValue(WaterDeviceBlock.FACING), face) : null;
    }

    /**
     * The fluid handler on {@code side}: the fresh water inlet takes water (into its fresh store), the waste water outlet gives its
     * old water; none elsewhere (other mods' pipes join only those two ports).
     */
    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        if (side == null) return null;
        PipeRole r = port(side);
        if (r == PipeRole.FRESH_IN) return new Inlet();
        if (r == PipeRole.WASTE_OUT) return new Outlet();
        return null;
    }

    /** The fresh water inlet: fill only. */
    private final class Inlet implements IFluidHandler {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { return fresh.getFluidInTank(tank); }
        @Override public int getTankCapacity(int tank) { return fresh.capacity(); }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return fresh.isFluidValid(tank, stack); }
        @Override public int fill(FluidStack resource, FluidAction action) {
            return Features.enabled(Feature.AUTO_WATER_CHANGE) ? fresh.fill(resource, action) : 0;
        }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    }

    /** The waste water outlet: drain only. */
    private final class Outlet implements IFluidHandler {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { return waste.getFluidInTank(tank); }
        @Override public int getTankCapacity(int tank) { return waste.capacity(); }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return false; }
        @Override public int fill(FluidStack resource, FluidAction action) { return 0; }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return waste.drain(resource, action); }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return waste.drain(maxDrain, action); }
    }

    // ------------------------------------------------------------------ what it serves

    /** A tank it serves: whether fresh water reaches it, whether waste water leaves it, and where it drains it itself. */
    private static final class Served {
        final TankBlockEntity tank;
        boolean fresh, waste;
        final List<PipeNetwork.Dock> wasteDocks = new ArrayList<>();

        Served(TankBlockEntity tank) {
            this.tank = tank;
        }
    }

    /**
     * What the controller reaches now: its four lines, the tanks (by touch, by its tank-side lines, through a filter), the caps of a
     * step for the fresh water out and the waste water in (mB; {@link PipePorts#UNLIMITED} for a touch).
     */
    private record Service(Map<PipeRole, PipePorts.Line> lines, Map<BlockPos, Served> tanks, int filterTanks, boolean contact,
                           long freshOutCap, long wasteInCap, boolean shortCircuit) {}

    private Service service(ServerLevel level) {
        Direction facing = getBlockState().hasProperty(WaterDeviceBlock.FACING) ? getBlockState().getValue(WaterDeviceBlock.FACING) : Direction.NORTH;
        Map<PipeRole, PipePorts.Line> lines = new EnumMap<>(PipeRole.class);
        boolean sc = false;
        for (PipeRole r : new PipeRole[]{PipeRole.FRESH_IN, PipeRole.FRESH_OUT, PipeRole.WASTE_IN, PipeRole.WASTE_OUT}) {
            PipePorts.Line l = PipePorts.line(level, worldPosition, WaterDeviceBlock.changerFace(facing, r));
            lines.put(r, l);
            sc |= l.shortCircuit();
        }
        Map<BlockPos, Served> tanks = new LinkedHashMap<>();
        boolean contact = false;
        // the touch: a tank block right next to it (any face)
        for (Direction d : Direction.values()) {
            BlockPos n = worldPosition.relative(d);
            if (!level.isLoaded(n) || !(level.getBlockState(n).getBlock() instanceof TankBlock)) continue;
            TankBlockEntity c = TankMultiblock.getController(level, n);
            if (c == null) continue;
            Served s = tanks.computeIfAbsent(c.getBlockPos(), k -> new Served(c));
            s.fresh = s.waste = true;
            s.wasteDocks.add(new PipeNetwork.Dock(n.immutable(), d.getOpposite()));
            contact = true;
        }
        java.util.Set<BlockPos> viaFilter = new java.util.HashSet<>();
        // ... or a part of a multiblock filter: every tank of the filter
        FilterPumpHeadBlockEntity touched = FilterWater.filterNextTo(level, worldPosition);
        if (touched != null) {
            contact = true;
            for (TankBlockEntity c : FilterWater.tanks(level, touched)) {
                Served s = tanks.computeIfAbsent(c.getBlockPos(), k -> new Served(c));
                s.fresh = s.waste = true;
                viaFilter.add(c.getBlockPos());
            }
        }
        // the tank-side lines: their tank docks, and a part of a filter at an end
        for (PipeRole r : new PipeRole[]{PipeRole.FRESH_OUT, PipeRole.WASTE_IN}) {
            PipePorts.Line l = lines.get(r);
            if (l.network() == null || l.shortCircuit()) continue;
            boolean in = r == PipeRole.WASTE_IN;
            for (PipeNetwork.Dock d : l.network().docks()) {
                if (!level.isLoaded(d.member()) || !(level.getBlockState(d.member()).getBlock() instanceof TankBlock)) continue;
                TankBlockEntity c = TankMultiblock.getController(level, d.member());
                if (c == null) continue;
                Served s = tanks.computeIfAbsent(c.getBlockPos(), k -> new Served(c));
                if (in) {
                    s.waste = true;
                    if (!s.wasteDocks.contains(d)) s.wasteDocks.add(d);
                } else s.fresh = true;
            }
            java.util.Set<BlockPos> heads = new java.util.HashSet<>();
            for (PipeNetwork.End e : l.network().ends()) {
                FilterPumpHeadBlockEntity h = FilterWater.filterAt(level, e.pos());
                if (h == null || !heads.add(h.getBlockPos())) continue;
                for (TankBlockEntity c : FilterWater.tanks(level, h)) {
                    Served s = tanks.computeIfAbsent(c.getBlockPos(), k -> new Served(c));
                    if (in) s.waste = true;
                    else s.fresh = true;
                    viaFilter.add(c.getBlockPos());
                }
            }
        }
        long freshCap = contact ? PipePorts.UNLIMITED : capOf(lines.get(PipeRole.FRESH_OUT));
        long wasteCap = contact ? PipePorts.UNLIMITED : capOf(lines.get(PipeRole.WASTE_IN));
        return new Service(lines, tanks, viaFilter.size(), contact, freshCap, wasteCap, sc);
    }

    /** mB a tank-side line carries in one step (none without a pipe or with a short circuit). */
    private static long capOf(PipePorts.Line l) {
        if (l.network() == null || l.shortCircuit()) return 0;
        return (long) l.rate() * STEP_TICKS;
    }

    // ------------------------------------------------------------------ the step

    static void serverTick(Level level, BlockPos pos, BlockState state, WaterChangerBlockEntity be) {
        if (be.last < 0) {
            be.last = level.getGameTime();
            be.changed(true);
        }
        if ((level.getGameTime() + pos.asLong()) % STEP_TICKS != 0) return;
        be.check((ServerLevel) level, false);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) TankWaterLinks.forgetChanger(level, worldPosition);
    }

    /**
     * One step (public for the GameTests): starts a change when it is due (or now with {@code force}: also restarts a running one),
     * moves one step of a running change, else tops off the tanks with a level sensor below its target. Returns the mB of fresh water
     * it put into tanks in this step.
     */
    public int check(ServerLevel level, boolean force) {
        long now = level.getGameTime();
        Service svc = service(level);
        int f = 0;
        PipePorts.Line fi = svc.lines.get(PipeRole.FRESH_IN), wo = svc.lines.get(PipeRole.WASTE_OUT);
        if (fi.network() != null && !fi.shortCircuit()) f |= FRESH_LINE;
        if ((wo.network() != null && !wo.shortCircuit()) || wasteTarget(level, wo) != null) f |= WASTE_LINE;
        if (svc.lines.get(PipeRole.FRESH_OUT).network() != null) f |= FRESH_OUT_LINE;
        if (svc.lines.get(PipeRole.WASTE_IN).network() != null) f |= WASTE_IN_LINE;
        if (svc.shortCircuit) f |= SHORT;
        if (svc.contact) f |= CONTACT;
        if (svc.filterTanks > 0) f |= FILTER;
        int nSensors = 0;
        for (Served s : svc.tanks.values()) {
            TankWaterLinks.changer(level, s.tank.getBlockPos(), worldPosition, s.waste ? s.wasteDocks : List.of());
            if (s.waste && !s.wasteDocks.isEmpty() && ChangeIntake.active() && ChangeIntake.openShare(level, s.tank, s.wasteDocks) > 0) f |= YOUNG;
            if (s.fresh) nSensors += sensorsOf(level, s.tank).size();
        }
        flags = f;
        tanks = svc.tanks.size();
        filterTanks = svc.filterTanks;
        sensors = nSensors;
        int added = 0;
        Status st;
        if (!Features.enabled(Feature.AUTO_WATER_CHANGE)) st = Status.OFF;
        else {
            flushWaste(level, svc);
            if (last < 0) last = now; // placed now: the first change comes one interval later
            boolean due = now - last >= intervalDays() * 24000L;
            // "change now" starts again; a change stuck for a whole interval too
            if (!pending.isEmpty() && (force || (started >= 0 && now - started >= intervalDays() * 24000L))) clearRun();
            int startCode = 1;
            if (pending.isEmpty() && (force || (enabled && due))) startCode = start(level, svc, now);
            if (!pending.isEmpty()) {
                int r = stepChange(level, svc, now);
                added = Math.max(0, r);
                st = r > 0 ? (pending.isEmpty() ? Status.CHANGED : Status.CHANGING) : reason(r, svc);
            } else if (svc.shortCircuit) st = Status.SHORT_CIRCUIT;
            else if (svc.tanks.isEmpty()) st = Status.NO_TANK;
            else {
                int r = topOff(level, svc);
                added = Math.max(0, r);
                if (r > 0) st = Status.TOPPED;
                else if (r < 0) st = reason(r, svc);
                else if (startCode < 0) st = noBoth(svc);
                else if (force && startCode == 0) st = Status.NO_WATER;
                else if (!enabled) st = Status.DISABLED;
                else st = lastMb > 0 ? Status.CHANGED : Status.WAITING;
            }
            flushWaste(level, svc);
        }
        status = st;
        changed(false);
        return added;
    }

    /** Status for a step that moved nothing ({@code r}: 0 no water, -1 no FE, -2 no room for the waste, -3 salty, -4 short circuit, -5 a tank-side line missing). */
    private Status reason(int r, Service svc) {
        return switch (r) {
            case -1 -> Status.NO_POWER;
            case -2 -> Status.NO_DRAIN;
            case -3 -> Status.SALTY;
            case -4 -> Status.SHORT_CIRCUIT;
            case -5 -> noBoth(svc);
            default -> svc.tanks.isEmpty() ? Status.NO_TANK : Status.NO_WATER;
        };
    }

    /** No tank it both fills and drains: which line is missing. */
    private static Status noBoth(Service svc) {
        if (svc.tanks.isEmpty()) return Status.NO_TANK;
        for (Served s : svc.tanks.values()) if (s.fresh && !s.waste) return Status.NO_DRAIN;
        return Status.NO_WATER;
    }

    /**
     * Starts a change: {@link #share} % of every tank it both fills and drains (never below the minimum fill while animals live in
     * it). Returns 1 started, 0 nothing to drain, -5 no tank with both lines.
     */
    private int start(ServerLevel level, Service svc, long now) {
        clearRun();
        boolean both = false;
        for (Served s : svc.tanks.values()) {
            if (!s.fresh || !s.waste) continue;
            both = true;
            TankData t = s.tank.getTank();
            long want = Math.min(Math.round(t.capacityMb() * share / 100.0), Math.max(0, t.waterMb() - minFill(t)));
            if (want > 0) pending.put(s.tank.getBlockPos().asLong(), want);
        }
        if (!both) return -5;
        if (pending.isEmpty()) return 0;
        started = now;
        return 1;
    }

    private void clearRun() {
        pending.clear();
        done.clear();
        runMb = 0;
        started = -1;
    }

    private static long minFill(TankData t) {
        // like the empty bucket: never below the minimum fill while animals live in the tank
        return t.inhabitants().isEmpty() ? 0 : Math.round(ServerConfig.getOr(ServerConfig.TANK_MIN_FILL_TO_INSERT, 0.5) * t.capacityMb());
    }

    /** One step of the running change: mB changed, or 0 / a negative reason ({@link #reason}). */
    private int stepChange(ServerLevel level, Service svc, long now) {
        if (svc.shortCircuit) return -4;
        List<Served> list = new ArrayList<>();
        List<Long> keys = new ArrayList<>();
        for (long k : pending.keySet().toLongArray()) {
            Served s = svc.tanks.get(BlockPos.of(k));
            if (s == null || !s.fresh || !s.waste || s.tank.isRemoved()) continue; // no longer served: it waits
            list.add(s);
            keys.add(k);
        }
        if (list.isEmpty()) return svc.tanks.isEmpty() ? 0 : -5;
        long[] want = new long[list.size()];
        long sum = 0;
        for (int i = 0; i < want.length; i++) {
            TankData t = list.get(i).tank.getTank();
            want[i] = Math.max(0, Math.min(pending.get((long) keys.get(i)), t.waterMb() - minFill(t)));
            sum += want[i];
        }
        if (sum <= 0) { // nothing left that may be drained: the change ends here
            finish(level, svc, now);
            return 0;
        }
        long room = waste.capacity() - waste.amount();
        if (room <= 0) return -2;
        long budget = Math.min(Math.min(sum, room), Math.min(svc.freshOutCap, svc.wasteInCap));
        if (budget <= 0) return -5;
        int fe = (int) Math.ceil(DeviceEnergy.pumpFe() * budget / 100.0);
        if (DeviceEnergy.required()) energy.drive(level, worldPosition, fe); // M32 part c: Create's rotation
        if (DeviceEnergy.required() && !org.overengineer.bettacraft.winding.Winding.canPay(energy, this, fe, DeviceEnergy.pumpFe())) return -1;
        Portion in = takeFresh(level, svc, (int) Math.min(Integer.MAX_VALUE, budget), false);
        if (in.mb <= 0) return in.salty ? -3 : 0;
        if (DeviceEnergy.required()) org.overengineer.bettacraft.winding.Winding.pay(energy, this, (int) Math.ceil(DeviceEnergy.pumpFe() * in.mb / 100.0), DeviceEnergy.pumpFe()); // M37: FE, else the spring
        long[] give = FilterWater.distribute(want, in.mb);
        int moved = 0;
        for (int i = 0; i < give.length; i++) {
            if (give[i] <= 0) continue;
            Served s = list.get(i);
            TankBlockEntity c = s.tank;
            TankData t = c.getTank();
            long before = t.waterMb();
            FluidStack old = WaterValues.stack((int) give[i], WaterValues.of(t));
            TankInteractions.changeWater(level, c, -give[i]);
            waste.fill(old, IFluidHandler.FluidAction.EXECUTE);
            if (!s.wasteDocks.isEmpty() && ChangeIntake.active()) ChangeIntake.suck(level, c, give[i], before, ChangeIntake.openShare(level, c, s.wasteDocks));
            TankInteractions.addWater(level, c, give[i], in.values);
            moved += (int) give[i];
            long k = keys.get(i);
            long left = pending.get(k) - give[i];
            done.addTo(k, give[i]);
            runMb += give[i];
            if (left <= 0) {
                pending.remove(k);
                logChange(level, c, done.remove(k));
            } else pending.put(k, left);
        }
        reportFlow(level, svc, moved);
        if (pending.isEmpty()) finish(level, svc, now);
        return moved;
    }

    /** The change is over: the tanks still open are logged with what they got, the change counts as done now. */
    private void finish(ServerLevel level, Service svc, long now) {
        for (long k : done.keySet().toLongArray()) {
            Served s = svc.tanks.get(BlockPos.of(k));
            long mb = done.get(k);
            if (s != null && mb > 0) logChange(level, s.tank, mb);
        }
        if (runMb > 0) {
            last = now;
            lastMb = (int) Math.min(Integer.MAX_VALUE, runMb);
        }
        clearRun();
    }

    private static void logChange(ServerLevel level, TankBlockEntity c, long mb) {
        TankData t = c.getTank();
        TankCare.log(level, c, "log.bettacraft.auto_water_change", String.valueOf(Math.round(mb * 100.0 / Math.max(1, t.capacityMb()))), String.valueOf(mb));
    }

    /** Tops off the tanks it fills whose level sensor is below its target: mB added, 0 nothing to do or no water, or a negative reason. */
    private int topOff(ServerLevel level, Service svc) {
        if (!Features.enabled(Feature.TOP_OFF)) return 0;
        List<Served> list = new ArrayList<>();
        List<Long> wants = new ArrayList<>();
        long now = level.getGameTime();
        for (Served s : svc.tanks.values()) {
            if (!s.fresh || s.tank.isRemoved()) continue;
            long missing = 0;
            for (LevelSensorBlockEntity sensor : sensorsOf(level, s.tank)) {
                sensor.read(now);
                missing = Math.max(missing, sensor.missing(level, s.tank));
            }
            if (missing < LevelSensorBlockEntity.HYSTERESIS_MB) continue;
            list.add(s);
            wants.add(Math.min(missing, LevelSensorBlockEntity.MAX_PER_STEP));
        }
        if (list.isEmpty()) return 0;
        long[] want = new long[list.size()];
        long sum = 0;
        for (int i = 0; i < want.length; i++) sum += want[i] = wants.get(i);
        long budget = Math.min(sum, svc.freshOutCap);
        if (budget <= 0) return -5;
        int fe = (int) Math.ceil(DeviceEnergy.pumpFe() * budget / 100.0);
        if (DeviceEnergy.required()) energy.drive(level, worldPosition, fe); // M32 part c: Create's rotation
        if (DeviceEnergy.required() && !org.overengineer.bettacraft.winding.Winding.canPay(energy, this, fe, DeviceEnergy.pumpFe())) return -1;
        Portion in = takeFresh(level, svc, (int) Math.min(Integer.MAX_VALUE, budget), true);
        if (in.mb <= 0) return in.salty ? -3 : 0;
        if (DeviceEnergy.required()) org.overengineer.bettacraft.winding.Winding.pay(energy, this, (int) Math.ceil(DeviceEnergy.pumpFe() * in.mb / 100.0), DeviceEnergy.pumpFe());
        long[] give = FilterWater.distribute(want, in.mb);
        int added = 0;
        for (int i = 0; i < give.length; i++) {
            if (give[i] <= 0) continue;
            TankInteractions.topOff(level, list.get(i).tank, give[i], in.values);
            added += (int) give[i];
        }
        lastTopOff = added;
        toppedTotal += added;
        reportFlow(level, svc, added);
        return added;
    }

    /** The level sensors at a tank (said lately, still there and still watching it). */
    public static List<LevelSensorBlockEntity> sensorsOf(ServerLevel level, TankBlockEntity tank) {
        List<LevelSensorBlockEntity> out = new ArrayList<>();
        for (BlockPos p : TankWaterLinks.sensors(level, tank.getBlockPos())) {
            if (level.isLoaded(p) && level.getBlockEntity(p) instanceof LevelSensorBlockEntity s && tank.getBlockPos().equals(s.tankPos())) out.add(s);
        }
        return out;
    }

    private void reportFlow(ServerLevel level, Service svc, int mb) {
        for (PipeRole r : new PipeRole[]{PipeRole.FRESH_OUT, PipeRole.WASTE_IN}) {
            PipeNetwork n = svc.lines.get(r).network();
            if (n != null) n.reportFlow(level, mb / STEP_TICKS);
        }
    }

    /** Fresh water taken for a step: mB and their values (null = tap water); {@code salty}: there was only salty water (top-off). */
    private record Portion(int mb, @Nullable WaterValues values, boolean salty) {}

    /** Takes up to {@code max} mB of fresh water: from its store first, then from the ends of its fresh water line (fresh only if asked). */
    private Portion takeFresh(ServerLevel level, Service svc, int max, boolean freshOnly) {
        int got = 0;
        WaterValues v = null;
        boolean salty = false;
        if (fresh.amount() > 0) {
            WaterValues fv = fresh.values();
            if (freshOnly && fv != null && !fv.fresh()) salty = true;
            else {
                FluidStack s = fresh.drain(max, IFluidHandler.FluidAction.EXECUTE);
                got += s.getAmount();
                v = WaterValues.from(s);
            }
        }
        PipePorts.Line l = svc.lines.get(PipeRole.FRESH_IN);
        PipeNetwork n = l.network();
        for (int tries = 0; got < max && n != null && !l.shortCircuit() && tries < 8; tries++) {
            FluidStack sim = n.drain(level, max - got, null, IFluidHandler.FluidAction.SIMULATE, worldPosition, true);
            if (sim.isEmpty()) break;
            WaterValues sv = WaterValues.from(sim);
            if (freshOnly && sv != null && !sv.fresh()) {
                salty = true;
                break;
            }
            FluidStack s = n.drain(level, max - got, sim, IFluidHandler.FluidAction.EXECUTE, worldPosition, true);
            if (s.isEmpty()) break;
            v = WaterStore.mix(v, got, WaterValues.from(s), s.getAmount());
            got += s.getAmount();
        }
        return new Portion(got, v, got <= 0 && salty);
    }

    /** The block a waste water outlet without a pipe gives its water to (a drain, a reservoir, another mod's tank; never a tank of ours). */
    @Nullable
    private IFluidHandler wasteTarget(ServerLevel level, PipePorts.Line l) {
        if (l.direct() == null || !level.isLoaded(l.direct())) return null;
        BlockState s = level.getBlockState(l.direct());
        if (s.getBlock() instanceof TankBlock || s.getBlock() instanceof FilterPartBlock
                || s.getBlock() instanceof WaterDeviceBlock w && w.kind() != WaterDeviceBlock.Kind.RESERVOIR) return null;
        return org.overengineer.bettacraft.platform.Platform.get().fluidHandler(level, l.direct(), l.face().getOpposite());
    }

    /** Gives the old water in its waste store to its waste water line (or the block in front of the outlet). */
    private void flushWaste(ServerLevel level, Service svc) {
        if (waste.amount() <= 0) return;
        PipePorts.Line l = svc.lines.get(PipeRole.WASTE_OUT);
        FluidStack s = waste.getFluidInTank(0);
        int took = 0;
        if (l.network() != null && !l.shortCircuit()) took = l.network().fill(level, s, IFluidHandler.FluidAction.EXECUTE, worldPosition);
        else {
            IFluidHandler h = wasteTarget(level, l);
            if (h != null) took = FluidMoves.fill(h, s);
        }
        if (took > 0) waste.drain(took, IFluidHandler.FluidAction.EXECUTE);
    }

    // ------------------------------------------------------------------ save / sync

    private void changed(boolean force) {
        setChanged();
        if (level == null || level.isClientSide) return;
        int h = java.util.Objects.hash(intervalDays, share, enabled, last, status, lastMb, tanks, filterTanks, sensors, flags, lastTopOff,
                fresh.amount() / 250, waste.amount() / 250, pendingMb() / 250);
        if (!force && h == syncedHash) return;
        syncedHash = h;
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private void write(CompoundTag tag) {
        tag.putInt("interval", intervalDays);
        tag.putInt("share", share);
        tag.putBoolean("enabled", enabled);
        tag.putLong("last", last);
        tag.putString("status", status.name());
        tag.putInt("last_mb", lastMb);
        tag.putInt("tanks", tanks);
        tag.putInt("filter_tanks", filterTanks);
        tag.putInt("sensors", sensors);
        tag.putInt("flags", flags);
        tag.putInt("last_top_off", lastTopOff);
        tag.putLong("topped", toppedTotal);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        write(tag);
        energy.save(tag);
        spring.save(tag, "spring");
        ListTag p = new ListTag();
        for (long k : pending.keySet().toLongArray()) {
            CompoundTag e = new CompoundTag();
            e.putLong("tank", k);
            e.putLong("mb", pending.get(k));
            e.putLong("done", done.get(k));
            p.add(e);
        }
        tag.put("pending", p);
        tag.putLong("run_mb", runMb);
        tag.putLong("started", started);
        CompoundTag fs = new CompoundTag();
        fresh.save(fs, registries);
        tag.put("fresh", fs);
        CompoundTag ws = new CompoundTag();
        waste.save(ws, registries);
        tag.put("waste", ws);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        intervalDays = tag.contains("interval") ? Math.max(0, Math.min(60, tag.getInt("interval"))) : 0;
        share = tag.contains("share") ? Math.max(5, Math.min(50, tag.getInt("share"))) : 25;
        enabled = !tag.contains("enabled") || tag.getBoolean("enabled");
        last = tag.contains("last") ? tag.getLong("last") : -1;
        try {
            status = Status.valueOf(tag.getString("status"));
        } catch (IllegalArgumentException e) {
            status = Status.WAITING;
        }
        lastMb = tag.getInt("last_mb");
        tanks = tag.getInt("tanks");
        filterTanks = tag.getInt("filter_tanks");
        sensors = tag.getInt("sensors");
        flags = tag.getInt("flags");
        lastTopOff = tag.getInt("last_top_off");
        toppedTotal = tag.getLong("topped");
        energy.load(tag);
        spring.load(tag, "spring");
        if (tag.contains("pending", Tag.TAG_LIST)) { // the server's save
            pending.clear();
            done.clear();
            ListTag p = tag.getList("pending", Tag.TAG_COMPOUND);
            for (int i = 0; i < p.size(); i++) {
                CompoundTag e = p.getCompound(i);
                if (e.getLong("mb") > 0) pending.put(e.getLong("tank"), e.getLong("mb"));
                if (e.getLong("done") > 0) done.put(e.getLong("tank"), e.getLong("done"));
            }
            runMb = tag.getLong("run_mb");
            started = tag.contains("started") ? tag.getLong("started") : -1;
        }
        if (tag.contains("fresh", Tag.TAG_COMPOUND)) fresh.load(tag.getCompound("fresh"), registries);
        if (tag.contains("waste", Tag.TAG_COMPOUND)) waste.load(tag.getCompound("waste"), registries);
        // a client gets only the sums (getUpdateTag)
        if (tag.contains("pending_mb")) clientPending = tag.getLong("pending_mb");
        if (tag.contains("fresh_mb")) clientFresh = tag.getInt("fresh_mb");
        if (tag.contains("waste_mb")) clientWaste = tag.getInt("waste_mb");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag t = new CompoundTag();
        write(t);
        t.putInt("interval", intervalDays()); // the client sees the effective interval
        t.putLong("pending_mb", pendingMb());
        t.putInt("fresh_mb", fresh.amount());
        t.putInt("waste_mb", waste.amount());
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
