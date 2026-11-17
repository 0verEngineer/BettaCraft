package org.overengineer.bettacraft.co2;

import net.minecraft.core.component.DataComponentGetter;
import org.overengineer.bettacraft.util.NbtCompat;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.equipment.Regulator;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.tank.TankBlockEntity;

/**
 * M25: a CO2 gas cylinder ({@link Co2CylinderBlock}): the gas in bubble-days (days at one bubble per second; a full
 * cylinder holds {@code co2Dosing.cylinderBubbleDays}), the pressure regulator (0..10 bubbles per second) and whether the
 * solenoid valve has it closed now.
 */
public class Co2CylinderBlockEntity extends BlockEntity implements Co2Source, Regulator.Device {
    private float gas;
    private int power = 2;
    /** The solenoid valve has closed the cylinder (night); set by the care step. */
    private boolean closed;
    private double lastBubbles;

    public Co2CylinderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CO2_CYLINDER.get(), pos, state);
    }

    /** Bubble-days of a full cylinder. */
    public static float capacity() {
        return (float) (double) ServerConfig.getOr(ServerConfig.CO2_CYLINDER_DAYS, 60.0);
    }

    public float gas() {
        return gas;
    }

    public void setGas(float g) {
        gas = Math.max(0, Math.min(capacity(), g));
        changed();
    }

    public boolean solenoid() {
        return getBlockState().hasProperty(Co2CylinderBlock.SOLENOID) && getBlockState().getValue(Co2CylinderBlock.SOLENOID);
    }

    public boolean closed() {
        return closed;
    }

    public double lastBubbles() {
        return lastBubbles;
    }

    @Override
    public double bubbles(ServerLevel level, TankBlockEntity tank) {
        boolean was = closed;
        closed = solenoid() && !TankCo2.lightsOn(level, tank.getTank());
        if (was != closed) changed();
        if (closed || gas <= 0) return 0;
        return power;
    }

    @Override
    public void used(ServerLevel level, double bubbles, double days) {
        float before = gas;
        if (bubbles > 0 && days > 0) gas = (float) Math.max(0, gas - bubbles * days);
        boolean on = bubbles > 0;
        float cap = Math.max(0.001F, capacity());
        boolean changed = Math.abs(lastBubbles - bubbles) > 1e-6 || Math.round(100 * before / cap) != Math.round(100 * gas / cap);
        lastBubbles = bubbles;
        if (before != gas) setChanged();
        BlockState s = getBlockState();
        if (s.getValue(Co2CylinderBlock.ACTIVE) != on) level.setBlock(worldPosition, s.setValue(Co2CylinderBlock.ACTIVE, on), Block.UPDATE_CLIENTS);
        if (changed) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    static void clientTick(Level level, BlockPos pos, BlockState state, Co2CylinderBlockEntity be) {
        // the bubble counter: one bubble per bubble per second
        if (!state.getValue(Co2CylinderBlock.ACTIVE) || be.power <= 0) return;
        if (level.random.nextInt(20) >= be.power) return;
        level.addParticle(net.minecraft.core.particles.ParticleTypes.BUBBLE_POP, pos.getX() + 0.5, pos.getY() + 1.02, pos.getZ() + 0.5, 0, 0.03, 0);
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
        changed();
    }

    @Override
    public Component name() {
        return Component.translatable("block.bettacraft.co2_cylinder");
    }

    @Override
    public Regulator.Sound sound() {
        return Regulator.Sound.AIR_STONE;
    }

    @Override
    public Component valueLabel() {
        return Component.translatable("gui.bettacraft.co2.bubbles", power);
    }

    @Override
    public boolean alwaysAdjustable() {
        return true;
    }

    @Override
    public Component quickLabel(int v) {
        return Component.translatable("gui.bettacraft.co2.quick", v); // M25c: "5 bubbles/s"
    }

    @Override
    public Component hint() {
        return Component.translatable("gui.bettacraft.co2.hint");
    }

    @Override
    public Component note() {
        int pct = Math.round(100F * gas / Math.max(0.001F, capacity()));
        if (!TankCo2.enabled()) return Component.translatable("message.bettacraft.co2.off");
        if (gas <= 0) return Component.translatable("gui.bettacraft.co2.empty");
        if (closed) return Component.translatable("gui.bettacraft.co2.closed", pct);
        return Component.translatable(lastBubbles > 0 ? "gui.bettacraft.co2.running" : "gui.bettacraft.co2.idle", pct);
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    // ------------------------------------------------------------------ item, save, sync

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        Float g = input.get(ModDataComponents.CO2_GAS.get());
        gas = g == null ? 0 : Math.max(0, g);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (gas > 0) components.set(ModDataComponents.CO2_GAS.get(), gas);
        if (solenoid()) components.set(ModDataComponents.CO2_SOLENOID.get(), true);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        output.discard("gas");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = NbtCompat.registries(this);
        super.saveAdditional(output);
        tag.putFloat("gas", gas);
        tag.putInt("power", power);
        tag.putBoolean("closed", closed);
        tag.putDouble("bubbles", lastBubbles);
        NbtCompat.write(output, tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        CompoundTag tag = NbtCompat.read(input);
        HolderLookup.Provider registries = input.lookup();
        super.loadAdditional(input);
        gas = Math.max(0, tag.getFloatOr("gas", 0f));
        power = tag.contains("power") ? Math.max(0, Math.min(10, tag.getIntOr("power", 0))) : 2;
        closed = tag.getBooleanOr("closed", false);
        lastBubbles = tag.getDoubleOr("bubbles", 0d);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag t = new CompoundTag();
        t.putFloat("gas", gas);
        t.putInt("power", power);
        t.putBoolean("closed", closed);
        t.putDouble("bubbles", lastBubbles);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
