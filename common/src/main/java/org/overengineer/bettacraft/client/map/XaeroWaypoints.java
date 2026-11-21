package org.overengineer.bettacraft.client.map;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.platform.Platform;

/**
 * M53: alarm waypoints in Xaero's Minimap (and through it in Xaero's World Map) by reflection: its third-party waypoint
 * API (the same route Xaero's own Waystones support takes): {@code BuiltInHudModules.MINIMAP.getCurrentSession()
 * .getWorldManager().getAutoRootContainer()}, the sub container of the dimension, its {@code ThirdPartyWaypointManager}
 * and our origin there; waypoints of it live only in memory (not saved). Colour red from Xaero's {@code WaypointColor}.
 */
final class XaeroWaypoints implements MapWaypoints {
    static final XaeroWaypoints INSTANCE = new XaeroWaypoints();
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(XaeroWaypoints.class);

    private boolean tried, ok;
    private Object minimap, red, normal;
    private java.lang.reflect.Constructor<?> waypoint;
    /** The third-party waypoint list each tank's waypoint went into, and its id there. */
    private final Map<UUID, Object> lists = new HashMap<>();

    private XaeroWaypoints() {}

    @Override
    public String id() {
        return "xaero";
    }

    @Override
    public boolean available() {
        if (tried) return ok;
        tried = true;
        if (!Platform.get().isModLoaded("xaerominimap") && !Platform.get().isModLoaded("xaerobetterpvp")) return false;
        try {
            ClassLoader cl = XaeroWaypoints.class.getClassLoader();
            minimap = Class.forName("xaero.hud.minimap.BuiltInHudModules", true, cl).getField("MINIMAP").get(null);
            Class<?> color = Class.forName("xaero.hud.minimap.waypoint.WaypointColor", true, cl);
            Class<?> purpose = Class.forName("xaero.hud.minimap.waypoint.WaypointPurpose", true, cl);
            red = enumValue(color, "RED");
            normal = enumValue(purpose, "NORMAL");
            waypoint = Class.forName("xaero.common.minimap.waypoints.Waypoint", true, cl)
                    .getConstructor(int.class, int.class, int.class, String.class, String.class, color, purpose);
            ok = true;
            LOGGER.info("Xaero's Minimap found: alarm waypoints on");
        } catch (Throwable t) {
            LOGGER.warn("Xaero's Minimap is loaded, but its waypoint API does not fit; no alarm waypoints there ({})", t.toString());
        }
        return ok;
    }

    private static Object enumValue(Class<?> e, String name) {
        for (Object o : e.getEnumConstants()) if (((Enum<?>) o).name().equals(name)) return o;
        return e.getEnumConstants()[0];
    }

    @Nullable
    private Object list(Identifier dimension) throws ReflectiveOperationException {
        Object session = Reflect.call(minimap, "getCurrentSession");
        if (session == null) return null;
        Object root = Reflect.call(Reflect.call(session, "getWorldManager"), "getAutoRootContainer");
        if (root == null) return null;
        Object helper = Reflect.call(Reflect.call(root, "getSession"), "getDimensionHelper");
        String dir = (String) Reflect.call(helper, "getDimensionDirectoryName", ResourceKey.create(Registries.DIMENSION, dimension));
        Object path = Reflect.call(Reflect.call(root, "getPath"), "resolve", dir);
        Object container = Reflect.call(root, "addSubContainer", path);
        Object manager = Reflect.call(container, "getThirdPartyWaypointManager");
        return Reflect.call(manager, "get", BettaCraft.id("alarms"));
    }

    @Override
    public boolean add(UUID tank, Identifier dimension, BlockPos pos, String label) {
        if (!available()) return false;
        remove(tank);
        try {
            Object l = list(dimension);
            if (l == null) return false;
            BlockPos at = pos.above();
            Object w = waypoint.newInstance(at.getX(), at.getY(), at.getZ(), label, "!", red, normal);
            Reflect.call(l, "add", tank.toString(), w);
            lists.put(tank, l);
            return true;
        } catch (Throwable t) {
            LOGGER.warn("Xaero's Minimap: could not add the alarm waypoint ({})", t.toString());
            return false;
        }
    }

    @Override
    public void remove(UUID tank) {
        Object l = lists.remove(tank);
        if (l == null) return;
        try {
            Reflect.call(l, "remove", tank.toString());
        } catch (Throwable t) {
            LOGGER.warn("Xaero's Minimap: could not remove the alarm waypoint ({})", t.toString());
        }
    }
}
