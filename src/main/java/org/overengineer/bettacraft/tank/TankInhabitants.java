package org.overengineer.bettacraft.tank;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * Inhabitant management of a tank. M0 stub: there are no inhabitants yet, so breaking is never blocked.
 * M1 will check living entities with {@code bettacraft:tank_ref} and M2 the nest (eggs/larvae).
 */
public final class TankInhabitants {
    private TankInhabitants() {}

    /** Reason why the tank block at {@code pos} must not be broken, e.g. "3 bettas are still inside". */
    public static Optional<Component> breakBlockedReason(Level level, BlockPos pos) {
        return Optional.empty();
    }
}
