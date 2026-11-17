package org.overengineer.bettacraft.monitor;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.overengineer.bettacraft.platform.event.SubscribeEvent;
import org.overengineer.bettacraft.platform.event.PlayerEvent;
import org.overengineer.bettacraft.platform.event.ServerStoppedEvent;
import org.overengineer.bettacraft.platform.network.PacketDistributor;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.api.ApiEvents;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.network.AlarmWaypointPayload;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import static org.overengineer.bettacraft.monitor.MonitorAlarms.*;

/** M52: the game event handlers of {@link MonitorAlarms} (M52: on the common game events, fired by each loader). */
public final class MonitorAlarmsHandlers {
    private MonitorAlarmsHandlers() {}

    /** The owner of a tank with an open waypoint comes online: they get it too. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || !Features.enabled(Feature.ALARM_WAYPOINTS)) return;
        for (UUID tank : BOOK.joined(sp.getUUID())) {
            AlarmWaypointBook.Entry e = BOOK.entry(tank);
            if (e == null || !(e.data instanceof AlarmWaypointPayload add)) continue;
            sentWaypoints++;
            PacketDistributor.sendToPlayer(sp, add);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        BOOK.clear();
    }
}
