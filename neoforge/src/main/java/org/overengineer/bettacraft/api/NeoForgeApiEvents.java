package org.overengineer.bettacraft.api;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.api.event.AnimalBornEvent;
import org.overengineer.bettacraft.api.event.AnimalDiedEvent;
import org.overengineer.bettacraft.api.event.FeaturesChangedEvent;
import org.overengineer.bettacraft.api.event.TankAlarmEvent;

/** M52: posts the public events of {@code api.event} on NeoForge's game event bus (for KubeJS and other mods). */
public final class NeoForgeApiEvents implements ApiEvents.Sink {
    @Override
    public void born(ServerLevel level, BlockPos tankPos, UUID tankId, String tankName, Identifier species, int count, boolean hatched,
            boolean foreign, boolean critter) {
        NeoForge.EVENT_BUS.post(new AnimalBornEvent(level, tankPos, tankId, tankName, species, count, hatched, foreign, critter));
    }

    @Override
    public void died(ServerLevel level, BlockPos tankPos, UUID tankId, String tankName, Identifier species, @Nullable Entity entity, int count,
            String cause, @Nullable Entity killer) {
        NeoForge.EVENT_BUS.post(new AnimalDiedEvent(level, tankPos, tankId, tankName, species, entity, count, cause, killer));
    }

    @Override
    public void alarm(ServerLevel level, BlockPos tankPos, UUID tankId, String tankName, BlockPos monitor, String value, float current, float limit,
            boolean below, boolean active) {
        NeoForge.EVENT_BUS.post(new TankAlarmEvent(level, tankPos, tankId, tankName, monitor, value, current, limit, below, active));
    }

    @Override
    public void featuresChanged(MinecraftServer server) {
        NeoForge.EVENT_BUS.post(new FeaturesChangedEvent(server));
    }
}
