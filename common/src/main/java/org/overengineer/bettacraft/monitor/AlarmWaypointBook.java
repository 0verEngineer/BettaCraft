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
 * last. The players who got it keep it until it closes; its {@code members} (M53: the owner and the users of the tank whose
 * waypoint switches are on) also get it when they come online later. {@link #update} follows changed switches.
 */
public final class AlarmWaypointBook {
    public static final class Entry {
        public final UUID tank;
        /** M53: who should have it (also when they come online later). */
        final Set<UUID> members = new LinkedHashSet<>();
        final Set<String> keys = new HashSet<>();
        final Set<UUID> players = new LinkedHashSet<>();
        /** What the players get (the waypoint payload). */
        @Nullable
        public final Object data;

        Entry(UUID tank, java.util.Collection<UUID> members, @Nullable Object data) {
            this.tank = tank;
            this.members.addAll(members);
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
    public Set<UUID> open(UUID tank, String key, java.util.Collection<UUID> members, @Nullable Object data, Supplier<Set<UUID>> recipients) {
        Entry e = open.get(tank);
        boolean fresh = e == null;
        if (fresh) {
            e = new Entry(tank, members, data);
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

    /** A player came online: the tanks whose waypoint they should have and do not have yet (they get it now). */
    public Set<UUID> joined(UUID player) {
        Set<UUID> out = new LinkedHashSet<>();
        for (Entry e : open.values()) if (e.members.contains(player) && e.players.add(player)) out.add(e.tank);
        return out;
    }

    /** M53: a player went offline: he no longer has any waypoint (his map mod forgot it); {@link #joined} gives it back. */
    public void left(UUID player) {
        for (Entry e : open.values()) e.players.remove(player);
    }

    /** M53: what {@link #update} changed: the players who get the waypoint now and those who lose it. */
    public record Change(Set<UUID> added, Set<UUID> removed) {
        public static final Change NONE = new Change(Set.of(), Set.of());
    }

    /**
     * M53: the waypoint switches of the tank changed: {@code members} should have its waypoint ({@code online} of them get it
     * now); everyone else who has it loses it. Nothing when the tank has no open waypoint.
     */
    public Change update(UUID tank, java.util.Collection<UUID> members, Set<UUID> online) {
        Entry e = open.get(tank);
        if (e == null) return Change.NONE;
        e.members.clear();
        e.members.addAll(members);
        Set<UUID> removed = new LinkedHashSet<>();
        for (java.util.Iterator<UUID> it = e.players.iterator(); it.hasNext(); ) {
            UUID p = it.next();
            if (!e.members.contains(p)) {
                removed.add(p);
                it.remove();
            }
        }
        Set<UUID> added = new LinkedHashSet<>();
        for (UUID p : online) if (e.members.contains(p) && e.players.add(p)) added.add(p);
        return new Change(added, removed);
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
