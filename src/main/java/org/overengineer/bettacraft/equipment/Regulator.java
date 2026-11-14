package org.overengineer.bettacraft.equipment;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * M23: the power regulator (0..100 % in steps of 10) of the devices: internal and corner filter, sponge filter, flow
 * pump, air stone (interior blocks, the value is in the anchor's block state) and air pump, external filter, cabinet
 * filter (block entities). Less power cleans / moves / aerates less, needs less FE and is quieter. Set in the regulator
 * screen (right-click), in the filter menus and with sneak + mouse wheel ({@code RegulatorPayload}).
 */
public final class Regulator {
    private Regulator() {}

    /** Which client volume a device uses. */
    public enum Sound { FILTER, AIR_PUMP, AIR_STONE, FLOW_PUMP }

    /** A device whose power can be set. */
    public interface Device {
        BlockPos pos();

        int power();

        void setPower(int p);

        Component name();

        Sound sound();

        default boolean takesSponge() {
            return false;
        }

        default boolean hasSponge() {
            return false;
        }

        default void setSponge(boolean on) {}

        /** M24: a line about what the device does now (the regulator screen), or null. */
        @Nullable
        default Component note() {
            return null;
        }

        /** M25: the setting as text instead of "x %" (the CO2 cylinder: bubbles per second), or null. */
        @Nullable
        default Component valueLabel() {
            return null;
        }

        /** M25: whether it can be set also with features.powerRegulator off (the pressure regulator of a CO2 cylinder). */
        default boolean alwaysAdjustable() {
            return false;
        }
    }

    /** The device at this position (for a multi-block piece its anchor), or null. */
    @Nullable
    public static Device find(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) return null;
        BlockState s = level.getBlockState(pos);
        if (s.getBlock() instanceof DeviceSegmentBlock seg) {
            BlockPos anchor = anchorOf(level, pos, seg);
            return anchor == null ? null : new SegmentDevice(level, anchor, seg);
        }
        if (s.getBlock() instanceof AirStoneBlock) return new StoneDevice(level, pos.immutable());
        var be = level.getBlockEntity(pos);
        if (be instanceof AirPumpBlockEntity pump) return new PumpDevice(pump);
        if (be instanceof Device d) return d; // M24: water pump, filter pump head
        if (be instanceof FilterHost f && f.hasFilter()) return new FilterDevice(be, f);
        return null;
    }

    /** The anchor of the piece the block at {@code pos} belongs to: the anchor at or below and west/north of it. */
    @Nullable
    static BlockPos anchorOf(Level level, BlockPos pos, DeviceSegmentBlock block) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : block.connected(level, pos)) {
            BlockState s = level.getBlockState(p);
            if (s.getValue(SegmentBlock.PART) != SegmentBlock.Part.ANCHOR) continue;
            if (p.getX() > pos.getX() || p.getY() > pos.getY() || p.getZ() > pos.getZ()) continue;
            double d = p.distSqr(pos);
            if (d < bestD) {
                bestD = d;
                best = p.immutable();
            }
        }
        return best;
    }

    /** Changes the power by {@code delta} steps or sets it ({@code absolute}); server side, checked. */
    public static void apply(ServerPlayer player, BlockPos pos, int value, boolean absolute) {
        Level level = player.level();
        if (player.distanceToSqr(pos.getCenter()) > 64) return;
        // like a lever: no spawn protection check (turning a knob is not building), but spectators and adventure players cannot
        if (!player.mayBuild() || player.isSpectator() || !level.getWorldBorder().isWithinBounds(pos)) return;
        Device d = find(level, pos);
        if (d == null) return;
        if (!org.overengineer.bettacraft.config.CommonConfig.powerRegulator() && !d.alwaysAdjustable()) return;
        if (org.overengineer.bettacraft.tank.interior.InteriorManager.isInterior(level)
                && !org.overengineer.bettacraft.tank.interior.InteriorManager.canBuild(player, pos)) return;
        int next = Math.max(0, Math.min(10, absolute ? value : d.power() + value));
        if (next == d.power()) return;
        d.setPower(next);
        Component label = d.valueLabel();
        if (label != null) player.displayClientMessage(Component.translatable("message.bettacraft.regulator.set_value", d.name(), label), true); // M25
        else player.displayClientMessage(Component.translatable("message.bettacraft.regulator.set", d.name(), next * 10), true);
    }

    // ------------------------------------------------------------------ devices

    record SegmentDevice(Level level, BlockPos pos, DeviceSegmentBlock block) implements Device {
        public int power() {
            return level.getBlockState(pos).getValue(DeviceSegmentBlock.POWER);
        }

        public void setPower(int p) {
            BlockState s = level.getBlockState(pos);
            if (s.is(block)) level.setBlock(pos, s.setValue(DeviceSegmentBlock.POWER, p), Block.UPDATE_ALL);
        }

        public Component name() {
            return block.itemFor(level.getBlockState(pos)).getHoverName();
        }

        public Sound sound() {
            return switch (block.kind()) {
                case PUMP -> Sound.FLOW_PUMP;
                case SPONGE_FILTER -> Sound.AIR_STONE;
                default -> Sound.FILTER;
            };
        }

        public boolean takesSponge() {
            return block.kind().motorFilter();
        }

        public boolean hasSponge() {
            return level.getBlockState(pos).getValue(DeviceSegmentBlock.SPONGE);
        }

        public void setSponge(boolean on) {
            BlockState s = level.getBlockState(pos);
            if (s.is(block)) level.setBlock(pos, s.setValue(DeviceSegmentBlock.SPONGE, on), Block.UPDATE_ALL);
        }
    }

    record StoneDevice(Level level, BlockPos pos) implements Device {
        public int power() {
            return level.getBlockState(pos).getValue(DeviceSegmentBlock.POWER);
        }

        public void setPower(int p) {
            BlockState s = level.getBlockState(pos);
            if (s.getBlock() instanceof AirStoneBlock) level.setBlock(pos, s.setValue(DeviceSegmentBlock.POWER, p), Block.UPDATE_ALL);
        }

        public Component name() {
            return Component.translatable("block.bettacraft.air_stone");
        }

        public Sound sound() {
            return Sound.AIR_STONE;
        }
    }

    record PumpDevice(AirPumpBlockEntity be) implements Device {
        public BlockPos pos() {
            return be.getBlockPos();
        }

        public int power() {
            return be.power();
        }

        public void setPower(int p) {
            be.setPower(p);
        }

        public Component name() {
            return Component.translatable("block.bettacraft.air_pump");
        }

        public Sound sound() {
            return Sound.AIR_PUMP;
        }
    }

    record FilterDevice(net.minecraft.world.level.block.entity.BlockEntity be, FilterHost f) implements Device {
        public BlockPos pos() {
            return be.getBlockPos();
        }

        public int power() {
            return f.regulator();
        }

        public void setPower(int p) {
            f.setRegulator(p);
        }

        public Component name() {
            return Component.translatable("block.bettacraft.external_filter");
        }

        public Sound sound() {
            return Sound.FILTER;
        }

        public boolean takesSponge() {
            return true;
        }

        public boolean hasSponge() {
            return f.intakeSponge();
        }

        public void setSponge(boolean on) {
            f.setIntakeSponge(on);
        }
    }

    /** Share 0..1 of a block state's regulator (1 without the property or with features.powerRegulator off). */
    public static double share(BlockState s) {
        if (!s.hasProperty(DeviceSegmentBlock.POWER) || !org.overengineer.bettacraft.config.CommonConfig.powerRegulator()) return 1.0;
        return s.getValue(DeviceSegmentBlock.POWER) / 10.0;
    }

    /** Share 0..1 of a stored power value (0..10) (1 with features.powerRegulator off). */
    public static double share(int power) {
        return org.overengineer.bettacraft.config.CommonConfig.powerRegulator() ? Math.max(0, Math.min(10, power)) / 10.0 : 1.0;
    }
}
