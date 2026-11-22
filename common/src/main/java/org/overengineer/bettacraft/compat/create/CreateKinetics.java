package org.overengineer.bettacraft.compat.create;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.config.CommonConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M32 part c: Create's rotation drives our devices as an alternative to FE (pumps, filter pump head, water change
 * controller, level sensor, reverse osmosis unit, food maker). A device reads the speed of a Create shaft, cogwheel,
 * gearbox or motor next to it whose shaft points at it (Create's {@code IRotate#hasShaftTowards}, {@code
 * KineticBlockEntity#getSpeed} / {@code isOverStressed}, by reflection: no compile dependency).
 * <p>
 * <b>Speed to work:</b> {@link #FULL_SPEED} RPM or faster (either direction) is the device's full work, slower is
 * proportional (32 RPM = half), an overstressed network gives nothing. The device turns that into the FE its work needs
 * ({@link org.overengineer.bettacraft.plumbing.DeviceEnergy#drive}), so all its limits stay as they are; the food maker
 * without FE has its spring wound instead.
 * <p>
 * <b>Stress:</b> none. Our devices are not kinetic blocks of Create's network (that would need a compile dependency on
 * Create and blocks of our own that extend Create's classes), so Create cannot charge stress units for them; they only
 * look at a shaft. Kept fair by their small needs and by the full-speed threshold.
 */
public final class CreateKinetics {
    /** RPM at which a device does its full work. */
    public static final float FULL_SPEED = 64F;
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/create");
    private static boolean looked, ok;
    private static Class<?> kinetic, rotate;
    private static Method getSpeed, isOverStressed, hasShaftTowards;

    private CreateKinetics() {}

    private static boolean init() {
        if (looked) return ok;
        looked = true;
        if (!CreateCompat.loaded()) return false;
        try {
            ClassLoader cl = CreateKinetics.class.getClassLoader();
            kinetic = Class.forName("com.simibubi.create.content.kinetics.base.KineticBlockEntity", false, cl);
            rotate = Class.forName("com.simibubi.create.content.kinetics.base.IRotate", false, cl);
            getSpeed = kinetic.getMethod("getSpeed");
            isOverStressed = kinetic.getMethod("isOverStressed");
            hasShaftTowards = rotate.getMethod("hasShaftTowards", LevelReader.class, BlockPos.class, BlockState.class, Direction.class);
            ok = true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.error("Create found, but its kinetic classes could not be reached: no rotation for our devices", e);
        }
        return ok;
    }

    /** Whether rotation can drive devices at all (Create there, {@code features.compat.create.rotation} on). */
    public static boolean available() {
        return Features.enabled(Feature.CREATE_ROTATION) && init();
    }

    /** The fastest speed (RPM, absolute) of a Create shaft pointing at {@code pos} that is not overstressed, or 0. */
    public static float speed(Level level, BlockPos pos) {
        if (level == null || level.isClientSide || !available()) return 0F;
        float best = 0F;
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            if (!level.isLoaded(n)) continue;
            BlockEntity be = level.getBlockEntity(n);
            if (be == null || !kinetic.isInstance(be)) continue;
            BlockState st = be.getBlockState();
            if (!rotate.isInstance(st.getBlock())) continue;
            try {
                if (!(Boolean) hasShaftTowards.invoke(st.getBlock(), level, n, st, d.getOpposite())) continue;
                if ((Boolean) isOverStressed.invoke(be)) continue;
                best = Math.max(best, Math.abs((Float) getSpeed.invoke(be)));
            } catch (ReflectiveOperationException | RuntimeException e) {
                if (org.overengineer.bettacraft.debug.LogArea.COMPAT.debug) org.overengineer.bettacraft.debug.LogArea.COMPAT.debug("Create speed at {}", n, e);
            }
        }
        return best;
    }

    /** Share of its full work (0 .. 1) a device at {@code pos} gets from Create's rotation. */
    public static float drive(Level level, BlockPos pos) {
        return work(speed(level, pos));
    }

    /** Pure: the share of full work at {@code rpm} (absolute). */
    public static float work(float rpm) {
        if (!(rpm > 0F)) return 0F;
        return Math.min(1F, rpm / FULL_SPEED);
    }
}
