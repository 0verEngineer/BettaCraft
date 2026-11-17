package org.overengineer.bettacraft.plumbing;

import org.overengineer.bettacraft.util.NbtCompat;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.equipment.Regulator;
import org.overengineer.bettacraft.registry.ModBlockEntities;

/**
 * M24: the water pump. Every {@link PipeNetwork#WINDOW} ticks it moves a batch from the block behind it to the block in
 * front of it: {@code pipes.pumpRate} x regulator per tick; less if the pipes carry less or the target is full.
 */
public class WaterPumpBlockEntity extends BlockEntity implements Regulator.Device, org.overengineer.bettacraft.winding.Windable {
    private int power = 10;
    /** mB moved in the last batch (0 = standing still). */
    private int moved;
    private final DeviceEnergy energy = new DeviceEnergy(20000, this::setChanged);

    public WaterPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_PUMP.get(), pos, state);
    }

    public DeviceEnergy energy() {
        return energy;
    }

    /** M37: the spring the crank winds ({@code features.power.winding}): it pumps on it while its FE is short. */
    private final org.overengineer.bettacraft.winding.WindSpring spring =
            org.overengineer.bettacraft.winding.WindSpring.of(org.overengineer.bettacraft.winding.Winding.Device.WATER_PUMP, this::setChanged);

    @Override
    public org.overengineer.bettacraft.winding.WindSpring windSpring() {
        return spring;
    }

    @Override
    public org.overengineer.bettacraft.winding.Winding.Device windDevice() {
        return org.overengineer.bettacraft.winding.Winding.Device.WATER_PUMP;
    }

    /** mB per tick in the last batch. */
    public int flowPerTick() {
        return moved / PipeNetwork.WINDOW;
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, WaterPumpBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % PipeNetwork.WINDOW != 0) return;
        be.pump(level, pos, state);
    }

    /** One batch (public for the GameTests). */
    public int pump(Level level, BlockPos pos, BlockState state) {
        int n = 0;
        double share = Regulator.share(power);
        if (Features.enabled(Feature.PIPES) && share > 0 && !level.hasNeighborSignal(pos)) {
            int fe = (int) Math.ceil(DeviceEnergy.pumpFe() * PipeNetwork.WINDOW * share);
            if (DeviceEnergy.required()) energy.drive(level, pos, fe); // M32 part c: Create's rotation
            if (!DeviceEnergy.required() || org.overengineer.bettacraft.winding.Winding.canPay(energy, this, fe, DeviceEnergy.pumpFe())) {
                Direction out = state.getValue(WaterPumpBlock.FACING);
                IFluidHandler from = FluidMoves.neighbour(level, pos, out.getOpposite()), to = FluidMoves.neighbour(level, pos, out);
                if (from != null && to != null) {
                    int max = (int) Math.round(ServerConfig.getOr(ServerConfig.PIPE_PUMP_RATE, 200) * PipeNetwork.WINDOW * share);
                    n = FluidMoves.move(from, to, max);
                    if (n > 0 && DeviceEnergy.required()) org.overengineer.bettacraft.winding.Winding.pay(energy, this, fe, DeviceEnergy.pumpFe()); // M37: FE, else the spring
                }
            }
        }
        if (n != moved) {
            moved = n;
            setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
        boolean on = n > 0;
        if (state.getValue(WaterPumpBlock.ACTIVE) != on) level.setBlock(pos, state.setValue(WaterPumpBlock.ACTIVE, on), Block.UPDATE_CLIENTS);
        return n;
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
        return Component.translatable("block.bettacraft.water_pump");
    }

    @Override
    public Regulator.Sound sound() {
        return Regulator.Sound.FLOW_PUMP;
    }

    @Override
    public Component note() {
        return Component.translatable(moved > 0 ? "gui.bettacraft.water_pump.running" : "gui.bettacraft.water_pump.idle", moved / PipeNetwork.WINDOW);
    }

    // ------------------------------------------------------------------ save / sync

    @Override
    protected void saveAdditional(ValueOutput output) {
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = NbtCompat.registries(this);
        super.saveAdditional(output);
        tag.putInt("power", power);
        tag.putInt("moved", moved);
        energy.save(tag);
        spring.save(tag, "spring");
        NbtCompat.write(output, tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        CompoundTag tag = NbtCompat.read(input);
        HolderLookup.Provider registries = input.lookup();
        super.loadAdditional(input);
        power = tag.contains("power") ? Math.max(0, Math.min(10, tag.getIntOr("power", 0))) : 10;
        moved = Math.max(0, tag.getIntOr("moved", 0));
        energy.load(tag);
        spring.load(tag, "spring");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag t = new CompoundTag();
        t.putInt("power", power);
        t.putInt("moved", moved);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
