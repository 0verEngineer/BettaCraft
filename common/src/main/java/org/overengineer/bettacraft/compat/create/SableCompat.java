package org.overengineer.bettacraft.compat.create;

import java.lang.reflect.Method;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.tank.TankMoves;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M32 part b: Sable, the physics library of Create Aeronautics (airships, cars). An assembled ship is a sub-level: its
 * blocks live in a plot far away in the same level and are drawn at the ship's pose. Our tank goes there as data
 * ({@code sable:silent_assembly_removal}, {@link TankMoves}); while it is in a sub-level it counts as travelling (animals
 * packed, care paused). Only Sable's public helper, by reflection.
 */
public final class SableCompat {
    public static final String MOD_ID = "sable";
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/sable");
    @Nullable
    private static Object helper;
    @Nullable
    private static Method getContaining, projectOut;

    private SableCompat() {}

    public static boolean loaded() {
        return org.overengineer.bettacraft.platform.Platform.get().isModLoaded(MOD_ID);
    }

    public static void register() {
        if (helper != null || !loaded()) return;
        try {
            Class<?> sable = Class.forName("dev.ryanhcode.sable.Sable", true, SableCompat.class.getClassLoader());
            Object h = sable.getField("HELPER").get(null);
            getContaining = h.getClass().getMethod("getContaining", Level.class, Vec3i.class);
            projectOut = h.getClass().getMethod("projectOutOfSubLevel", Level.class, Vec3.class);
            helper = h;
            TankMoves.TRAVELLING = SableCompat::inSubLevel;
            TankMoves.PROJECT = SableCompat::project;
            LOGGER.info("Sable found: tanks on airships travel packed");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.error("Sable found, but its helper could not be reached: tanks in sub-levels are treated as standing", e);
        }
    }

    /** Whether the block lies in a sub-level (on an airship). */
    public static boolean inSubLevel(Level level, BlockPos pos) {
        if (helper == null || getContaining == null) return false;
        try {
            return getContaining.invoke(helper, level, pos) != null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    /** A point of a sub-level's plot where it is in the world now (the ship's pose); the point itself outside sub-levels. */
    public static Vec3 project(Level level, Vec3 p) {
        if (helper == null || projectOut == null) return p;
        try {
            Object v = projectOut.invoke(helper, level, p);
            return v instanceof Vec3 w ? w : p;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return p;
        }
    }
}
