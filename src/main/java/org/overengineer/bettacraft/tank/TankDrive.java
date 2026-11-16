package org.overengineer.bettacraft.tank;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.DriveSide;

/**
 * M41: Create's rotation charges a tank's FE buffer ({@link TankPower}), so every device of the tank that takes the tank's
 * FE runs on it. The inputs of a tank are the drive adapters on its walls or under it and the shafts on the drive side of
 * the cabinets under it. This class is the part without Create: the planning of one care step (what the tank needs, what
 * every input gives, the limit per tank) and the hooks; the Create side ({@code compat.create.CreateDrive}, compiled against
 * Create and only loaded with it) sets {@link #BRIDGE} and charges the stress.
 * <p>
 * <b>Planning</b> (every care step, after {@link TankPower#step} took the devices' FE): an input at {@code fullSpeedRpm} or
 * faster can give {@code fePerTickPerInput}, slower proportionally (its theoretical speed: an overstressed network keeps its
 * load, so it stays overstressed until it has the capacity). The tank wants what fills its buffer again (with a full buffer
 * that is what its devices took); all inputs together give at most that and at most {@code maxFePerTickPerTank}, shared by
 * what each could give.
 * With {@code power.winding} on a cabinet's own shaft winds the cabinet's spring with the rest. Every input costs stress by
 * its load ({@code stressPerFe} SU per FE/t, at least {@code idleStressPerRpm} per RPM); an overstressed input gives nothing.
 */
public final class TankDrive {
    private TankDrive() {}

    /** One input of a tank: a drive adapter or the shaft at a cabinet. */
    public interface Input {
        BlockPos pos();

        /** Theoretical speed (RPM, absolute) of the network at the input, 0 while it stands or nothing is connected. */
        float speed();

        /** Whether the network at the input is overstressed (it then gives nothing). */
        boolean overstressed();

        /** The load of the next step: the FE per tick it gives (its stress follows) and whether it delivered it. */
        void load(double fePerTick, boolean delivering);

        /** The stress impact (SU per RPM) the input costs now (Create multiplies it by the speed). */
        float stressImpact();
    }

    /** The Create side, set when Create is there ({@code compat.create.CreateDrive}). */
    public interface Bridge {
        /** Whether this is a drive adapter whose back sits on the tank face at {@code outward} (pointing away from the tank). */
        boolean isAdapter(BlockState state, Direction outward);

        @Nullable
        Input adapter(Level level, BlockPos pos);

        /** The shaft at the drive side of a cabinet, or null (no side, nothing kinetic there). */
        @Nullable
        Input cabinet(Level level, CabinetBlockEntity cab);

        /** The cabinet no longer drives: its shaft stops costing stress. */
        void release(Level level, CabinetBlockEntity cab);

        /** FE per tick the shaft of the cabinet gave in the last step, -1 while it drives nothing (for the overlays). */
        double cabinetFe(CabinetBlockEntity cab);

        /** Whether the network of the cabinet's shaft is overstressed. */
        boolean cabinetOverstressed(CabinetBlockEntity cab);
    }

    @Nullable
    public static volatile Bridge BRIDGE;
    /** The drive adapter item (only with Create), for the creative tab. */
    @Nullable
    public static volatile Supplier<Item> ADAPTER_ITEM;

    /** Whether rotation can charge tanks at all: Create there and {@code features.compat.create.drive} on. */
    public static boolean enabled() {
        return BRIDGE != null && Features.enabled(Feature.CREATE_DRIVE);
    }

    /** Whether cabinets take a shaft: {@link #enabled()} and {@code features.compat.create.cabinetDrive} on. */
    public static boolean cabinetsEnabled() {
        return enabled() && Features.enabled(Feature.CREATE_CABINET_DRIVE);
    }

    /** Whether a cabinet may take a shaft on this side ({@code createDrive.cabinetSides}). */
    public static boolean sideAllowed(DriveSide side) {
        if (side == DriveSide.NONE) return true;
        List<? extends String> sides = ServerConfig.getOr(ServerConfig.DRIVE_CABINET_SIDES, List.of("back", "left", "right", "front", "bottom"));
        return sides.contains(side.id());
    }

    public static float fullSpeed() {
        return Math.max(1, ServerConfig.getOr(ServerConfig.DRIVE_FULL_SPEED, 64));
    }

    public static double fePerInput() {
        return ServerConfig.getOr(ServerConfig.DRIVE_FE, 256);
    }

    public static double maxFePerTank() {
        return ServerConfig.getOr(ServerConfig.DRIVE_MAX_FE, 1024);
    }

    public static double stressPerFe() {
        return ServerConfig.getOr(ServerConfig.DRIVE_STRESS_PER_FE, 2.0);
    }

    public static double idleStress() {
        return ServerConfig.getOr(ServerConfig.DRIVE_IDLE_STRESS, 0.5);
    }

    public static double fePerSpringTick() {
        return ServerConfig.getOr(ServerConfig.DRIVE_FE_PER_SPRING_TICK, 10.0);
    }

    // ------------------------------------------------------------------ pure planning (unit tested)

    /** Share of the full power (0 .. 1) at {@code rpm} (absolute; {@code full} RPM and faster = 1). */
    public static double work(float rpm, float full) {
        if (!(rpm > 0F) || !(full > 0F)) return 0;
        return Math.min(1.0, rpm / full);
    }

    /**
     * What every input gives: all together at most {@code want} and {@code cap}, shared in proportion to what each can
     * give ({@code max}).
     */
    public static double[] shares(double[] max, double want, double cap) {
        double[] out = new double[max.length];
        double total = 0;
        for (double m : max) total += Math.max(0, m);
        double give = Math.min(Math.max(0, want), Math.min(Math.max(0, cap), total));
        if (total <= 0 || give <= 0) return out;
        for (int i = 0; i < max.length; i++) out[i] = Math.max(0, max[i]) * give / total;
        return out;
    }

    /** Stress (SU) of an input that gives {@code fePerTick} at {@code rpm}: by the load, at least the idle stress. */
    public static double stress(double fePerTick, float rpm, double perFe, double idlePerRpm) {
        double r = Math.abs(rpm);
        if (!(r > 0)) return 0;
        return Math.max(idlePerRpm * r, Math.max(0, fePerTick) * perFe);
    }

    /** Create's stress impact (SU per RPM) for {@link #stress}: what the network multiplies by the speed. */
    public static float impact(double fePerTick, float rpm, double perFe, double idlePerRpm) {
        double r = Math.abs(rpm);
        if (!(r > 0)) return (float) idlePerRpm;
        return (float) (stress(fePerTick, rpm, perFe, idlePerRpm) / r);
    }

    // ------------------------------------------------------------------ the care step

    /** Most ticks one step gives for (a step after unloaded time: the shafts did not turn meanwhile). */
    static final long MAX_STEP = 40;

    /** One care step of {@code ticks} (after the devices took their FE): plans the inputs of the tank, refills its buffer and winds the cabinets' springs. */
    public static void step(ServerLevel level, TankBlockEntity c, long ticks) {
        c.driveFe = 0;
        c.driveInputs = 0;
        c.driveOverstressed = false;
        Bridge bridge = BRIDGE;
        if (bridge == null) return;
        List<CabinetBlockEntity> cabinets = TankEquipment.cabinets(level, c);
        if (!enabled()) {
            for (CabinetBlockEntity cab : cabinets) bridge.release(level, cab);
            return;
        }
        List<Input> inputs = new ArrayList<>();
        List<CabinetBlockEntity> owners = new ArrayList<>();
        for (BlockPos p : TankEquipment.driveAdapters(level, c)) {
            Input in = bridge.adapter(level, p);
            if (in == null) continue;
            inputs.add(in);
            owners.add(null);
        }
        boolean cabs = cabinetsEnabled();
        for (CabinetBlockEntity cab : cabinets) {
            Input in = cabs && cab.driveSide() != DriveSide.NONE ? bridge.cabinet(level, cab) : null;
            if (in == null) {
                bridge.release(level, cab);
                continue;
            }
            inputs.add(in);
            owners.add(cab);
        }
        if (inputs.isEmpty()) return;
        long n = Math.max(1, Math.min(ticks, MAX_STEP));
        float full = fullSpeed();
        double perInput = fePerInput(), cap = maxFePerTank();
        double[] max = new double[inputs.size()];
        for (int i = 0; i < max.length; i++) max[i] = work(inputs.get(i).speed(), full) * perInput;
        TankData t = c.getTank();
        double want = 0;
        if (TankPower.anyRequired()) want = Math.max(0, TankPower.capacity() - t.powerBuffer()) / (double) n;
        double[] share = shares(max, want, cap);
        double used = 0;
        for (double s : share) used += s;
        // M37 with M41: a cabinet's own shaft winds the cabinet's spring with what the tank does not need
        double[] spring = new double[inputs.size()];
        if (TankPower.anyRequired() && org.overengineer.bettacraft.winding.Winding.Device.CABINET.windable()) {
            double perTick = Math.max(1e-6, fePerSpringTick());
            for (int i = 0; i < spring.length && used < cap; i++) {
                CabinetBlockEntity cab = owners.get(i);
                if (cab == null) continue;
                var s = cab.windSpring();
                double room = (s.capacity() - s.ticks()) * perTick / n;
                spring[i] = Math.max(0, Math.min(Math.min(max[i] - share[i], room), cap - used));
                used += spring[i];
            }
        }
        long buffer = t.powerBuffer();
        double fe = 0;
        for (int i = 0; i < inputs.size(); i++) {
            Input in = inputs.get(i);
            double load = share[i] + spring[i];
            boolean delivering = load > 0 && !in.overstressed();
            in.load(load, delivering);
            c.driveInputs++;
            if (in.overstressed()) c.driveOverstressed = true;
            if (!delivering) continue;
            fe += load;
            buffer += Math.round(share[i] * n);
            if (spring[i] > 0) {
                CabinetBlockEntity cab = owners.get(i);
                var s = cab.windSpring();
                s.set(Math.min(s.capacity(), s.ticks() + (long) Math.floor(spring[i] * n / Math.max(1e-6, fePerSpringTick()))));
            }
        }
        c.driveFe = (int) Math.round(fe);
        buffer = Math.min(buffer, Math.max(TankPower.capacity(), t.powerBuffer())); // rounding
        if (buffer != t.powerBuffer()) {
            t.setPowerBuffer(buffer);
            c.setChanged();
        }
    }
}
