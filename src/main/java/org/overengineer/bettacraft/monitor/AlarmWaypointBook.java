package org.overengineer.bettacraft.monitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * M32 part d (pure, unit-tested): which players have the alarm waypoint of which tank. One waypoint per tank, however many
 * alarm tiles (of one or several monitors, {@code key}) are triggered for it: it opens with the first and closes with the
 * last. The players who got it keep it until it closes (they get the removal even when they walked away); the owner
 * also gets it when they come online later.
 */
public final class AlarmWaypointBook {
    public static final class Entry {
        public final UUID tank;
        @Nullable
        public final UUID owner;
        final Set<String> keys = new HashSet<>();
        final Set<UUID> players = new LinkedHashSet<>();
        /** What the players get (the waypoint payload). */
        @Nullable
        public final Object data;

        Entry(UUID tank, @Nullable UUID owner, @Nullable Object data) {
            this.tank = tank;
            this.owner = owner;
            this.data = data;
        }

        public Set<UUID> players() {
            return players;
        }
    }

    private final Map<UUID, Entry> open = new HashMap<>();

    /**
     * Alarm {@code key} of {@code tank} went off. Returns the players to send the waypoint to: {@code recipients} the
     * first time, nobody when the tank has a waypoint already.
     */
    public Set<UUID> open(UUID tank, String key, @Nullable UUID owner, @Nullable Object data, Supplier<Set<UUID>> recipients) {
        Entry e = open.get(tank);
        boolean fresh = e == null;
        if (fresh) {
            e = new Entry(tank, owner, data);
            open.put(tank, e);
        }
        e.keys.add(key);
        if (!fresh) return Set.of();
        e.players.addAll(recipients.get());
        return Set.copyOf(e.players);
    }

    /** Alarm {@code key} of {@code tank} cleared. Returns the players whose waypoint goes away (none while others remain). */
    public Set<UUID> close(UUID tank, String key) {
        Entry e = open.get(tank);
        if (e == null || !e.keys.remove(key) || !e.keys.isEmpty()) return Set.of();
        open.remove(tank);
        return Set.copyOf(e.players);
    }

    /** A player came online: the tanks they own that have a waypoint they do not have yet (they get it now). */
    public Set<UUID> joined(UUID player) {
        Set<UUID> out = new LinkedHashSet<>();
        for (Entry e : open.values()) if (player.equals(e.owner) && e.players.add(player)) out.add(e.tank);
        return out;
    }

    public boolean isOpen(UUID tank) {
        return open.containsKey(tank);
    }

    @Nullable
    public Entry entry(UUID tank) {
        return open.get(tank);
    }

    public int size() {
        return open.size();
    }

    public void clear() {
        open.clear();
    }
}
