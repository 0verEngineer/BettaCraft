package org.overengineer.bettacraft.api.event;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

/**
 * M32 part d: the base of Bettacraft's public events on {@code NeoForge.EVENT_BUS} (server side, main thread). They are
 * notifications (not cancellable) for modpack scripts (KubeJS: {@code NativeEvents.onEvent}) and other mods; the
 * switch {@code features.compat.modpackEvents} turns them off. Every event names the tank: the level, the position of its
 * controller block, its stable id (kept when tanks merge, split or travel) and the name of its sign ("" without one).
 */
public abstract class TankEvent extends Event {
    private final ServerLevel level;
    private final BlockPos tankPos;
    private final UUID tankId;
    private final String tankName;

    protected TankEvent(ServerLevel level, BlockPos tankPos, UUID tankId, String tankName) {
        this.level = level;
        this.tankPos = tankPos.immutable();
        this.tankId = tankId;
        this.tankName = tankName == null ? "" : tankName;
    }

    public ServerLevel getLevel() {
        return level;
    }

    /** The controller block of the tank. */
    public BlockPos getTankPos() {
        return tankPos;
    }

    public UUID getTankId() {
        return tankId;
    }

    /** The name of the tank's sign, "" without one. */
    public String getTankName() {
        return tankName;
    }
}
