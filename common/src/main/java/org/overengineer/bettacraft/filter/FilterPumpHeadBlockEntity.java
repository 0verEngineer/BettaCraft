package org.overengineer.bettacraft.filter;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import org.overengineer.bettacraft.util.NbtCompat;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.equipment.Regulator;
import org.overengineer.bettacraft.plumbing.DeviceEnergy;
import org.overengineer.bettacraft.plumbing.FluidMoves;
import org.overengineer.bettacraft.plumbing.PipeNetwork;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankNitrate;

/**
 * M24: the pump head of a multiblock external filter ({@link MultiblockFilter}). Every {@link PipeNetwork#WINDOW} ticks
 * it pulls a batch of water through an inlet and pushes it out of an outlet (half a water pump x regulator, slower with
 * clogged sponges), finds the tank both lead to, cleans it by its media and lets the media clog with the tank's
 * stocking. Shows everything in its screen (synced: chambers, media, flow, tank, problem).
 * <p>
 * M40 ({@code features.pipes.filterLines}): it serves every tank its lines reach ({@link MultiblockFilter#links}); the
 * cleaning and the flow are shared by the tanks' water volumes, a tank without guards (or with a pipe through it) gets
 * nothing and a warning. A pump head from before M40 is marked {@code retrofit} by its schema migration: its warnings say
 * that the update asks for the guards now (D2), until every tank of it is guarded.
 */
public class FilterPumpHeadBlockEntity extends BlockEntity implements Regulator.Device, org.overengineer.bettacraft.winding.Windable {
    private int power = 10;
    private final DeviceEnergy energy = new DeviceEnergy(20000, this::setChanged);
    // synced state
    private MultiblockFilter.Problem problem = MultiblockFilter.Problem.NONE;
    private int chambers, inlets, outlets, flow;
    private double cleanBlocks;
    @Nullable
    private BlockPos tank;
    /** Per chamber: medium ordinal and dirt in percent. */
    private int[] media = new int[0];
    private long lastScan = Long.MIN_VALUE;
    private MultiblockFilter.Structure structure;
    /** M40: the tanks of the filter (looked up with the structure, and when a filter pipe changed). */
    private List<MultiblockFilter.Link> links = List.of();
    private int linkVersion = -1;
    private boolean linksStale = true;
    /** M40 part b synced: the fresh and waste water ports and the water in them. */
    private int freshPorts, wastePorts, freshMb, wasteMb;
    /** M66: a water source block touches a fresh water port (tap water, endless). */
    private boolean freshEndless;
    /** M40 synced: per tank of the filter its status, flow and cleaning. */
    private List<TankLink> tanks = List.of();
    /** M40 (D2): built before M40 (set by the schema migration); cleared once every tank of the filter is guarded. */
    private boolean retrofit;
    /** M40: the players already told in chat about the missing guards of this pre-M40 filter (once per server run). */
    private final java.util.Set<java.util.UUID> told = new java.util.HashSet<>();

    /** M40: one tank of the filter as the screen shows it. */
    /** M66: {@code blocks} = the water blocks of the tank (what the cleaning is for). */
    public record TankLink(BlockPos tank, MultiblockFilter.LinkStatus status, int flow, double clean, int percent, int blocks) {}

    public FilterPumpHeadBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FILTER_PUMP_HEAD.get(), pos, state);
    }

    public DeviceEnergy energy() { return energy; }

    /** M37: the spring the crank winds ({@code features.power.winding}): the pump runs on it (six charges) while its FE is short. */
    private final org.overengineer.bettacraft.winding.WindSpring spring =
            org.overengineer.bettacraft.winding.WindSpring.of(org.overengineer.bettacraft.winding.Winding.Device.MULTIBLOCK_FILTER, this::setChanged);

    @Override
    public org.overengineer.bettacraft.winding.WindSpring windSpring() {
        return spring;
    }

    @Override
    public org.overengineer.bettacraft.winding.Winding.Device windDevice() {
        return org.overengineer.bettacraft.winding.Winding.Device.MULTIBLOCK_FILTER;
    }

    public MultiblockFilter.Problem problem() { return problem; }
    public int chambers() { return chambers; }
    public int inlets() { return inlets; }
    public int outlets() { return outlets; }
    /** mB per tick in the last batch. */
    public int flow() { return flow; }
    public double cleanBlocks() { return cleanBlocks; }
    @Nullable public BlockPos servedTank() { return tank; }
    /** M40: every tank of the filter with its status. */
    public List<TankLink> tanks() { return tanks; }
    /** M40 (D2): a filter from before M40 that still lacks guards. */
    public boolean retrofit() { return retrofit; }
    public int[] media() { return media; }
    /** M40 part b: the fresh / waste water ports of the filter and the mB in them (synced). */
    public int freshPorts() { return freshPorts; }
    public int wastePorts() { return wastePorts; }
    public int freshMb() { return freshMb; }
    public boolean freshEndless() { return freshEndless; }
    public int wasteMb() { return wasteMb; }

    /** The parts of the filter (scanned again at most every 40 ticks). */
    public MultiblockFilter.Structure structure(ServerLevel level) {
        long now = level.getGameTime();
        if (structure == null || now - lastScan >= 40 || now < lastScan) {
            structure = MultiblockFilter.scan(level, worldPosition);
            lastScan = now;
            linksStale = true;
        }
        return structure;
    }

    /** M40: the tanks of the filter, looked up again when the parts were scanned or a filter pipe changed. */
    public List<MultiblockFilter.Link> currentLinks(ServerLevel level) {
        MultiblockFilter.Structure s = structure(level);
        if (linksStale || linkVersion != org.overengineer.bettacraft.plumbing.FilterPipeBlock.version()) {
            linkVersion = org.overengineer.bettacraft.plumbing.FilterPipeBlock.version();
            linksStale = false;
            links = s.formed() && Features.enabled(Feature.FILTER_LINES) ? MultiblockFilter.links(level, s) : List.of();
        }
        return links;
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, FilterPumpHeadBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % PipeNetwork.WINDOW != 0) return;
        be.run((ServerLevel) level, false);
    }

    /** Forgets the structure so the next run scans again (a part was placed or broken; tests). */
    public void rescan() {
        structure = null;
        linksStale = true;
    }

    /** One batch (public for the GameTests; {@code force} scans the parts again first). */
    public int run(ServerLevel level, boolean force) {
        long now = level.getGameTime();
        if (force) rescan();
        MultiblockFilter.Structure s = structure(level);
        List<FilterChamberBlockEntity> ch = new ArrayList<>();
        for (BlockPos p : s.chambers()) if (level.getBlockEntity(p) instanceof FilterChamberBlockEntity c) ch.add(c);
        if (!Features.enabled(Feature.FILTER_LINES)) {
            links = List.of();
            tanks = List.of();
            return runSingle(level, s, ch);
        }
        currentLinks(level);
        // every tank: guarded? water in it?
        List<MultiblockFilter.LinkStatus> st = new ArrayList<>();
        List<MultiblockFilter.Link> working = new ArrayList<>();
        for (MultiblockFilter.Link l : links) {
            MultiblockFilter.LinkStatus x = l.tank().isRemoved() ? MultiblockFilter.LinkStatus.NO_WATER : MultiblockFilter.status(level, l);
            st.add(x);
            if (x == MultiblockFilter.LinkStatus.OK) working.add(l);
        }
        long[] water = new long[working.size()];
        for (int i = 0; i < water.length; i++) water[i] = working.get(i).water();
        double[] part = MultiblockFilter.shares(water);
        int[] flows = new int[working.size()];
        double[] cleans = new double[working.size()];
        int moved = 0;
        double clean = 0;
        BlockPos served = null;
        double share = Regulator.share(power);
        if (s.formed() && Features.enabled(Feature.MULTIBLOCK_FILTER) && share > 0 && !working.isEmpty() && !level.hasNeighborSignal(worldPosition)) {
            int fe = (int) Math.ceil(DeviceEnergy.pumpFe() * PipeNetwork.WINDOW * share);
            if (DeviceEnergy.required()) energy.drive(level, worldPosition, fe); // M32 part c: Create's rotation
            if (!DeviceEnergy.required() || org.overengineer.bettacraft.winding.Winding.canPay(energy, this, fe, DeviceEnergy.pumpFe())) {
                int max = maxBatch(ch, share);
                // A1: the flow is shared by the water volumes (the pump moves each tank's share through the filter and back)
                for (int i = 0; i < flows.length; i++) {
                    flows[i] = (int) Math.round(max * part[i]);
                    moved += flows[i];
                }
                if (moved > 0) {
                    if (DeviceEnergy.required()) org.overengineer.bettacraft.winding.Winding.pay(energy, this, fe, DeviceEnergy.pumpFe()); // M37: FE, else the spring
                    double peat = 0;
                    for (FilterChamberBlockEntity c : ch) {
                        double b = c.kind().blocks() * c.kind().efficiency(c.dirt());
                        clean += b;
                        if (c.kind() == FilterMedium.PEAT) peat += b; // M25: peat softens the water
                    }
                    clean *= share;
                    peat *= share;
                    // A1: and so is the cleaning
                    for (int i = 0; i < flows.length; i++) {
                        MultiblockFilter.Link l = working.get(i);
                        cleans[i] = clean * part[i];
                        if (flows[i] <= 0) continue;
                        if (served == null) served = l.tank().getBlockPos();
                        MultiblockFilter.serve(level, l.tank().getBlockPos(), worldPosition, cleans[i], peat * part[i]);
                        for (var d : l.intakes()) org.overengineer.bettacraft.plumbing.interior.TankAnchors.markFlow(level, l.tank(), d.member(), d.face(), true, now);
                        for (var d : l.returns()) org.overengineer.bettacraft.plumbing.interior.TankAnchors.markFlow(level, l.tank(), d.member(), d.face(), false, now);
                    }
                    clog(level, working, part, ch, PipeNetwork.WINDOW / 24000.0);
                    int servedTanks = 0; // M45: one filter for two or more tanks
                    for (int f : flows) if (f > 0) servedTanks++;
                    if (servedTanks >= 2) for (int i = 0; i < flows.length; i++) {
                        if (flows[i] > 0) org.overengineer.bettacraft.progression.Progress.awardNear(level, working.get(i).tank(), org.overengineer.bettacraft.progression.Progress.SHARED_FILTER, servedTanks);
                    }
                }
            }
        }
        if (served == null) MultiblockFilter.stop(level, worldPosition);
        // the reports for the tank menus and what the screen shows
        List<TankLink> out = new ArrayList<>();
        boolean allOk = !links.isEmpty();
        for (int i = 0; i < links.size(); i++) {
            MultiblockFilter.Link l = links.get(i);
            MultiblockFilter.LinkStatus x = st.get(i);
            MultiblockFilter.report(level, l.tank().getBlockPos(), worldPosition, x);
            allOk &= !x.warning();
            int w = working.indexOf(l);
            out.add(new TankLink(l.tank().getBlockPos(), x, w < 0 ? 0 : flows[w] / PipeNetwork.WINDOW, w < 0 ? 0 : cleans[w],
                    w < 0 ? 0 : (int) Math.round(part[w] * 100), (int) Math.round(l.tank().getTank().waterBlocks())));
        }
        if (retrofit && allOk) {
            retrofit = false; // D2: retrofitted
            setChanged();
        } else if (retrofit && !links.isEmpty()) tellRetrofit(level);
        boolean changed = !out.equals(tanks);
        tanks = out;
        update(level, s, ch, moved / PipeNetwork.WINDOW, clean, served, changed);
        return moved;
    }

    /** mB the pump moves per batch at most: half a water pump x regulator, slower with clogged sponges. */
    private static int maxBatch(List<FilterChamberBlockEntity> ch, double share) {
        double spongeDirt = 0;
        int sponges = 0;
        for (FilterChamberBlockEntity c : ch) {
            if (c.kind() == FilterMedium.SPONGE) {
                spongeDirt += c.dirt();
                sponges++;
            }
        }
        double flowFactor = 1 - 0.6 * (sponges == 0 ? 0 : spongeDirt / sponges);
        return (int) Math.round(ServerConfig.getOr(ServerConfig.PIPE_PUMP_RATE, 200) / 2.0 * PipeNetwork.WINDOW * share * flowFactor);
    }

    /** D2: the players near a filter from before M40 hear once (per server run) why it stopped. */
    private void tellRetrofit(ServerLevel level) {
        for (var p : level.players()) {
            if (p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition)) > 32 * 32 || !told.add(p.getUUID())) continue;
            p.sendSystemMessage(Component.translatable("message.bettacraft.mb_filter.retrofit", worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())
                    .withStyle(net.minecraft.ChatFormatting.RED));
        }
    }

    /** The filter as before M40 ({@code features.pipes.filterLines} off): one tank, no guards. */
    private int runSingle(ServerLevel level, MultiblockFilter.Structure s, List<FilterChamberBlockEntity> ch) {
        int moved = 0;
        double clean = 0;
        BlockPos served = null;
        double share = Regulator.share(power);
        if (s.formed() && Features.enabled(Feature.MULTIBLOCK_FILTER) && share > 0 && !level.hasNeighborSignal(worldPosition)) {
            int fe = (int) Math.ceil(DeviceEnergy.pumpFe() * PipeNetwork.WINDOW * share);
            if (DeviceEnergy.required()) energy.drive(level, worldPosition, fe); // M32 part c: Create's rotation
            if (!DeviceEnergy.required() || org.overengineer.bettacraft.winding.Winding.canPay(energy, this, fe, DeviceEnergy.pumpFe())) {
                moved = pump(level, s, maxBatch(ch, share));
                if (moved > 0) {
                    if (DeviceEnergy.required()) org.overengineer.bettacraft.winding.Winding.pay(energy, this, fe, DeviceEnergy.pumpFe()); // M37: FE, else the spring
                    TankBlockEntity t = tankOf(level, s);
                    if (t != null) {
                        served = t.getBlockPos();
                        double peat = 0;
                        for (FilterChamberBlockEntity c : ch) {
                            double b = c.kind().blocks() * c.kind().efficiency(c.dirt());
                            clean += b;
                            if (c.kind() == FilterMedium.PEAT) peat += b; // M25: peat softens the water
                        }
                        clean *= share;
                        MultiblockFilter.serve(level, served, worldPosition, clean, peat * share);
                        clog(level, t, ch, PipeNetwork.WINDOW / 24000.0);
                    }
                }
            }
        }
        if (served == null) MultiblockFilter.stop(level, worldPosition);
        update(level, s, ch, moved / PipeNetwork.WINDOW, clean, served, false);
        return moved;
    }

    /** M40: the media clog with the stocking of all its tanks, weighted by their shares. */
    private void clog(ServerLevel level, List<MultiblockFilter.Link> working, double[] part, List<FilterChamberBlockEntity> ch, double days) {
        double load = 0;
        for (int i = 0; i < working.size(); i++) load += part[i] * Math.max(0.25, Math.min(4.0, TankNitrate.load(level, working.get(i).tank().getTank())));
        double rate = ServerConfig.getOr(ServerConfig.MEDIA_DIRT_PER_DAY, 0.04) * Math.max(0.25, load) * days;
        for (FilterChamberBlockEntity c : ch) c.dirty(rate * c.kind().dirtRate());
    }

    /** Lets the media clog: {@code multiblockFilter.dirtPerDay} x the tank's stocking (at least a quarter) x the medium's rate. */
    public void clog(ServerLevel level, TankBlockEntity t, List<FilterChamberBlockEntity> ch, double days) {
        double load = Math.max(0.25, Math.min(4.0, TankNitrate.load(level, t.getTank())));
        double rate = ServerConfig.getOr(ServerConfig.MEDIA_DIRT_PER_DAY, 0.04) * load * days;
        for (FilterChamberBlockEntity c : ch) c.dirty(rate * c.kind().dirtRate());
    }

    /** Clogs every chamber of this filter by {@code days} at the stocking of its tank (GameTests, catching up). */
    public void clogDays(ServerLevel level, double days) {
        if (structure == null) structure = MultiblockFilter.scan(level, worldPosition);
        if (Features.enabled(Feature.FILTER_LINES)) {
            List<MultiblockFilter.Link> w = new ArrayList<>();
            for (MultiblockFilter.Link l : MultiblockFilter.links(level, structure)) if (MultiblockFilter.status(level, l) == MultiblockFilter.LinkStatus.OK) w.add(l);
            long[] water = new long[w.size()];
            for (int i = 0; i < water.length; i++) water[i] = w.get(i).water();
            List<FilterChamberBlockEntity> ch = new ArrayList<>();
            for (BlockPos p : structure.chambers()) if (level.getBlockEntity(p) instanceof FilterChamberBlockEntity c) ch.add(c);
            if (!w.isEmpty()) clog(level, w, MultiblockFilter.shares(water), ch, days);
            return;
        }
        TankBlockEntity t = tankOf(level, structure);
        if (t == null) return;
        List<FilterChamberBlockEntity> ch = new ArrayList<>();
        for (BlockPos p : structure.chambers()) if (level.getBlockEntity(p) instanceof FilterChamberBlockEntity c) ch.add(c);
        clog(level, t, ch, days);
    }

    private static int pump(Level level, MultiblockFilter.Structure s, int max) {
        for (BlockPos in : s.inlets()) {
            Direction fi = level.getBlockState(in).getValue(FilterPartBlock.FACING);
            IFluidHandler from = FluidMoves.neighbour(level, in, fi);
            if (from == null) continue;
            for (BlockPos out : s.outlets()) {
                Direction fo = level.getBlockState(out).getValue(FilterPartBlock.FACING);
                IFluidHandler to = FluidMoves.neighbour(level, out, fo);
                if (to == null) continue;
                int n = FluidMoves.move(from, to, max, true);
                if (n > 0) return n;
            }
        }
        return 0;
    }

    /** The tank both an inlet and an outlet lead to, or null. */
    @Nullable
    private static TankBlockEntity tankOf(Level level, MultiblockFilter.Structure s) {
        List<TankBlockEntity> in = new ArrayList<>();
        for (BlockPos p : s.inlets()) in.addAll(MultiblockFilter.tanksAt(level, p, level.getBlockState(p).getValue(FilterPartBlock.FACING)));
        if (in.isEmpty()) return null;
        for (BlockPos p : s.outlets()) {
            for (TankBlockEntity t : MultiblockFilter.tanksAt(level, p, level.getBlockState(p).getValue(FilterPartBlock.FACING))) {
                for (TankBlockEntity i : in) if (i.getBlockPos().equals(t.getBlockPos())) return t;
            }
        }
        return null;
    }

    private void update(Level level, MultiblockFilter.Structure s, List<FilterChamberBlockEntity> ch, int flowNow, double clean, @Nullable BlockPos served, boolean linksChanged) {
        // M40 part b: the water ports
        int fm = 0, wm = 0;
        boolean endless = false;
        if (level instanceof ServerLevel sl && !s.fresh().isEmpty()) {
            // M66: what the filter can use, not only what sits in the ports: the ends of their lines and tap water count too
            boolean[] tap = new boolean[1];
            fm = (int) Math.min(Integer.MAX_VALUE, FilterWater.freshAvailable(sl, this, tap));
            endless = tap[0];
        } else {
            for (BlockPos p : s.fresh()) if (level.getBlockEntity(p) instanceof FilterPortBlockEntity port) fm += port.water().amount();
        }
        for (BlockPos p : s.waste()) if (level.getBlockEntity(p) instanceof FilterPortBlockEntity port) wm += port.water().amount();
        if (fm != freshMb || wm != wasteMb || endless != freshEndless || s.fresh().size() != freshPorts || s.waste().size() != wastePorts) {
            freshEndless = endless;
            freshMb = fm;
            wasteMb = wm;
            freshPorts = s.fresh().size();
            wastePorts = s.waste().size();
            linksChanged = true;
        }
        int[] m = new int[ch.size()];
        for (int i = 0; i < ch.size(); i++) m[i] = ch.get(i).kind().ordinal() * 1000 + (int) Math.round(ch.get(i).dirt() * 100);
        boolean dirty = linksChanged || s.problem() != problem || s.chambers().size() != chambers || s.inlets().size() != inlets || s.outlets().size() != outlets
                || flowNow != flow || Math.abs(clean - cleanBlocks) > 0.05 || !java.util.Objects.equals(served, tank) || !java.util.Arrays.equals(m, media);
        problem = s.problem();
        chambers = s.chambers().size();
        inlets = s.inlets().size();
        outlets = s.outlets().size();
        flow = flowNow;
        cleanBlocks = clean;
        tank = served;
        media = m;
        BlockState st = getBlockState();
        boolean on = flowNow > 0;
        if (st.hasProperty(FilterPartBlock.ACTIVE) && st.getValue(FilterPartBlock.ACTIVE) != on) level.setBlock(worldPosition, st.setValue(FilterPartBlock.ACTIVE, on), Block.UPDATE_CLIENTS);
        if (dirty) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ------------------------------------------------------------------ regulator

    @Override
    public BlockPos pos() {
        return worldPosition;
    }

    @Override
    public int power() {
        return power;
    }

    @Override
    public void setPower(int p) {
        p = Math.max(0, Math.min(10, p));
        if (p == power) return;
        power = p;
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public Component name() {
        return Component.translatable("block.bettacraft.filter_pump_head");
    }

    @Override
    public Regulator.Sound sound() {
        return Regulator.Sound.FILTER;
    }

    @Override
    public Component note() {
        return Component.translatable("gui.bettacraft.mb_filter.flow", flow, Math.round(cleanBlocks));
    }

    // ------------------------------------------------------------------ save / sync

    private void write(CompoundTag t) {
        t.putInt("power", power);
        t.putString("problem", problem.name());
        t.putInt("chambers", chambers);
        t.putInt("inlets", inlets);
        t.putInt("outlets", outlets);
        t.putInt("flow", flow);
        t.putDouble("clean", cleanBlocks);
        if (tank != null) t.put("tank", NbtCompat.writeBlockPos(tank));
        t.putIntArray("media", media);
        t.putBoolean("retrofit", retrofit);
        net.minecraft.nbt.ListTag l = new net.minecraft.nbt.ListTag();
        for (TankLink k : tanks) {
            CompoundTag e = new CompoundTag();
            e.put("pos", NbtCompat.writeBlockPos(k.tank()));
            e.putString("status", k.status().name());
            e.putInt("flow", k.flow());
            e.putDouble("clean", k.clean());
            e.putInt("percent", k.percent());
            e.putInt("blocks", k.blocks());
            l.add(e);
        }
        t.put("tanks", l);
        t.putIntArray("ports", new int[]{freshPorts, wastePorts, freshMb, wasteMb, freshEndless ? 1 : 0}); // M40 part b (M66: the fifth: tap water)
    }

    private void read(CompoundTag t) {
        power = t.contains("power") ? Math.max(0, Math.min(10, t.getIntOr("power", 0))) : 10;
        try {
            problem = MultiblockFilter.Problem.valueOf(t.getStringOr("problem", ""));
        } catch (IllegalArgumentException e) {
            problem = MultiblockFilter.Problem.NONE;
        }
        chambers = t.getIntOr("chambers", 0);
        inlets = t.getIntOr("inlets", 0);
        outlets = t.getIntOr("outlets", 0);
        flow = t.getIntOr("flow", 0);
        cleanBlocks = t.getDoubleOr("clean", 0d);
        tank = t.contains("tank") ? NbtCompat.readBlockPos(t, "tank").orElse(null) : null;
        media = t.getIntArray("media").orElse(new int[0]);
        retrofit = t.getBooleanOr("retrofit", false);
        List<TankLink> l = new ArrayList<>();
        var list = t.getListOrEmpty("tanks");
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompoundOrEmpty(i);
            MultiblockFilter.LinkStatus st;
            try {
                st = MultiblockFilter.LinkStatus.valueOf(e.getStringOr("status", ""));
            } catch (IllegalArgumentException x) {
                st = MultiblockFilter.LinkStatus.OK;
            }
            BlockPos p = NbtCompat.readBlockPos(e, "pos").orElse(null);
            if (p != null) l.add(new TankLink(p, st, e.getIntOr("flow", 0), e.getDoubleOr("clean", 0d), e.getIntOr("percent", 0), e.getIntOr("blocks", 0)));
        }
        tanks = l;
        int[] ports = t.getIntArray("ports").orElse(new int[0]);
        freshPorts = ports.length >= 4 ? ports[0] : 0;
        wastePorts = ports.length >= 4 ? ports[1] : 0;
        freshMb = ports.length >= 4 ? ports[2] : 0;
        wasteMb = ports.length >= 4 ? ports[3] : 0;
        freshEndless = ports.length >= 5 && ports[4] != 0;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = NbtCompat.registries(this);
        super.saveAdditional(output);
        org.overengineer.bettacraft.data.schema.Schemas.FILTER_PUMP_HEAD.stamp(tag);
        write(tag);
        energy.save(tag);
        spring.save(tag, "spring");
        NbtCompat.write(output, tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        CompoundTag raw = NbtCompat.read(input);
        HolderLookup.Provider registries = input.lookup();
        super.loadAdditional(input);
        // M40: the schema stamp is only on the saved block entity, not on the sync tag (which carries "retrofit" itself)
        CompoundTag tag = level != null && level.isClientSide() ? raw : org.overengineer.bettacraft.data.schema.Schemas.FILTER_PUMP_HEAD.migrate(raw);
        read(tag);
        energy.load(tag);
        spring.load(tag, "spring");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag t = new CompoundTag();
        write(t);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
