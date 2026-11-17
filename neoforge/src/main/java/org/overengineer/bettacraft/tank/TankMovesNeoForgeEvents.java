package org.overengineer.bettacraft.tank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.overengineer.bettacraft.tank.TankMoves.*;

/** M52: the NeoForge event handlers of {@link TankMoves} (moved here from the common class). */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
final class TankMovesNeoForgeEvents {
    private TankMovesNeoForgeEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (LIFTS.isEmpty()) return;
        List<Lift> todo = new ArrayList<>(LIFTS);
        LIFTS.clear();
        Map<String, List<Lift>> byTank = new HashMap<>();
        for (Lift l : todo) byTank.computeIfAbsent(l.dim().location() + "|" + l.tank(), k -> new ArrayList<>()).add(l);
        for (List<Lift> group : byTank.values()) {
            ServerLevel level = event.getServer().getLevel(group.getFirst().dim());
            if (level == null) continue;
            try {
                settle(level, group);
            } catch (RuntimeException ex) {
                LOGGER.error("Could not tidy up after a tank left on a contraption", ex);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        LIFTS.clear();
        TRAVELLING_REGIONS.clear();
    }
}
