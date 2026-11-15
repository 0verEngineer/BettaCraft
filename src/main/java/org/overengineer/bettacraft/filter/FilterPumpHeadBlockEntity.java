package org.overengineer.bettacraft.filter;

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
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
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
    public int[] media() { return media; }

    static void serverTick(Level level, BlockPos pos, BlockState state, FilterPumpHeadBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % PipeNetwork.WINDOW != 0) return;
        be.run((ServerLevel) level, false);
    }

    /** Forgets the structure so the next run scans again (a part was placed or broken; tests). */
    public void rescan() {
        lastScan = Long.MIN_VALUE;
    }

    /** One batch (public for the GameTests; {@code force} scans the parts again first). */
    public int run(ServerLevel level, boolean force) {
        long now = level.getGameTime();
        if (structure == null || force || now - lastScan >= 40 || now < lastScan) {
            structure = MultiblockFilter.scan(level, worldPosition);
            lastScan = now;
        }
        MultiblockFilter.Structure s = structure;
        List<FilterChamberBlockEntity> ch = new ArrayList<>();
        for (BlockPos p : s.chambers()) if (level.getBlockEntity(p) instanceof FilterChamberBlockEntity c) ch.add(c);
        int moved = 0;
        double clean = 0;
        BlockPos served = null;
        double share = Regulator.share(power);
        if (s.formed() && Features.enabled(Feature.MULTIBLOCK_FILTER) && share > 0 && !level.hasNeighborSignal(worldPosition)) {
            int fe = (int) Math.ceil(DeviceEnergy.pumpFe() * PipeNetwork.WINDOW * share);
            if (DeviceEnergy.required()) energy.drive(level, worldPosition, fe); // M32 part c: Create's rotation
            if (!DeviceEnergy.required() || org.overengineer.bettacraft.winding.Winding.canPay(energy, this, fe, DeviceEnergy.pumpFe())) {
                double spongeDirt = 0;
                int sponges = 0;
                for (FilterChamberBlockEntity c : ch) {
                    if (c.kind() == FilterMedium.SPONGE) {
                        spongeDirt += c.dirt();
                        sponges++;
                    }
                }
                double flowFactor = 1 - 0.6 * (sponges == 0 ? 0 : spongeDirt / sponges);
                int max = (int) Math.round(ServerConfig.getOr(ServerConfig.PIPE_PUMP_RATE, 200) / 2.0 * PipeNetwork.WINDOW * share * flowFactor);
                moved = pump(level, s, max);
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
        update(level, s, ch, moved / PipeNetwork.WINDOW, clean, served);
        return moved;
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

    private void update(Level level, MultiblockFilter.Structure s, List<FilterChamberBlockEntity> ch, int flowNow, double clean, @Nullable BlockPos served) {
        int[] m = new int[ch.size()];
        for (int i = 0; i < ch.size(); i++) m[i] = ch.get(i).kind().ordinal() * 1000 + (int) Math.round(ch.get(i).dirt() * 100);
        boolean dirty = s.problem() != problem || s.chambers().size() != chambers || s.inlets().size() != inlets || s.outlets().size() != outlets
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
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
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
        if (tank != null) t.put("tank", NbtUtils.writeBlockPos(tank));
        t.putIntArray("media", media);
    }

    private void read(CompoundTag t) {
        power = t.contains("power") ? Math.max(0, Math.min(10, t.getInt("power"))) : 10;
        try {
            problem = MultiblockFilter.Problem.valueOf(t.getString("problem"));
        } catch (IllegalArgumentException e) {
            problem = MultiblockFilter.Problem.NONE;
        }
        chambers = t.getInt("chambers");
        inlets = t.getInt("inlets");
        outlets = t.getInt("outlets");
        flow = t.getInt("flow");
        cleanBlocks = t.getDouble("clean");
        tank = t.contains("tank") ? NbtUtils.readBlockPos(t, "tank").orElse(null) : null;
        media = t.getIntArray("media");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        write(tag);
        energy.save(tag);
        spring.save(tag, "spring");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
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
