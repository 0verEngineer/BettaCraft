package org.overengineer.bettacraft.gametest;

import net.minecraft.world.entity.Entity;

/** M52: small loader differences of the GameTests (they run on NeoForge and, most of them, on Fabric). */
final class GameTestCompat {
    private GameTestCompat() {}

    /** A part of a multipart mob (NeoForge: its PartEntity; vanilla: the dragon's parts). */
    static boolean isPart(Entity e) {
        if (e instanceof net.minecraft.world.entity.boss.EnderDragonPart) return true;
        for (Class<?> c = e.getClass(); c != null; c = c.getSuperclass()) {
            if (c.getName().equals("net.neoforged.neoforge.entity.PartEntity")) return true;
        }
        return false;
    }

    /** The owner an item entity is kept for (NeoForge: getTarget; vanilla: its private field). */
    static java.util.UUID target(net.minecraft.world.entity.item.ItemEntity e) {
        try {
            java.lang.reflect.Field f = net.minecraft.world.entity.item.ItemEntity.class.getDeclaredField("target");
            f.setAccessible(true);
            return (java.util.UUID) f.get(e);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /** NeoForge: the block capabilities at a position are looked up again; Fabric has no such cache. */
    static void invalidateCapabilities(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        try {
            level.getClass().getMethod("invalidateCapabilities", net.minecraft.core.BlockPos.class).invoke(level, pos);
        } catch (NoSuchMethodException e) {
            // Fabric
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
