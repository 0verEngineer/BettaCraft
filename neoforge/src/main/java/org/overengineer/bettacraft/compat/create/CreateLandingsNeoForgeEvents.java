package org.overengineer.bettacraft.compat.create;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.PackedTank;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankMoveMap;
import org.overengineer.bettacraft.tank.TankMoves;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.overengineer.bettacraft.compat.create.CreateLandings.*;

/** M52: the NeoForge event handlers of {@link CreateLandings} (moved here from the common class). */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
final class CreateLandingsNeoForgeEvents {
    private CreateLandingsNeoForgeEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (STOPS.isEmpty()) {
            LANDED.clear();
            MEMBERS.clear();
            return;
        }
        List<Stop> todo = new ArrayList<>(STOPS);
        STOPS.clear();
        try {
            for (Stop s : todo) {
                ServerLevel level = event.getServer().getLevel(s.dim());
                if (level == null || LANDED.contains(s.tank())) continue;
                Entity e = s.contraption();
                if (e == null || !e.isRemoved()) continue;
                Entity.RemovalReason why = e.getRemovalReason();
                Boolean putDown = putDown(e);
                try {
                    if (why == Entity.RemovalReason.DISCARDED && Boolean.TRUE.equals(putDown)) {
                        notPlaced(level, s);
                    } else if (putDown != null && (why == Entity.RemovalReason.KILLED
                            || why == Entity.RemovalReason.DISCARDED && CREATE_OWN.contains(e.getClass().getName()))) {
                        destroyed(level, s);
                    }
                } catch (RuntimeException ex) {
                    LOGGER.error("Could not look after tank {} whose contraption ended", s.tank(), ex);
                }
            }
        } finally {
            LANDED.clear();
            MEMBERS.clear();
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        STOPS.clear();
        LANDED.clear();
        MEMBERS.clear();
        CreateCompat.forgetMessages(); // M33 part d
    }
}
