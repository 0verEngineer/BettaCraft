package org.overengineer.bettacraft.tank;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * M53: the users of a tank besides its owner (the player who built it). Three levels: the owner, the {@link Role#CARETAKER
 * caretakers} (may do everything the owner may: care, settings, going in, the users and the waypoints of everyone) and the
 * {@link Role#VIEWER viewers} (see the menu, change nothing, cannot go in). Everybody else is {@link Role#OTHER}: the lock
 * of the tank decides for them (open: may change, locked: may only look). Players of the owner's FTB Teams party count as
 * caretakers unless they are listed.
 * <p>
 * Also the alarm waypoints of the tank: one switch for all of its users ({@link #waypointsAll}) and one per user (the
 * owner's own switch defaults to on, every other user's to off).
 */
public final class TankUsers {
    /** At most this many listed users per tank. */
    public static final int MAX = 32;

    public enum Role {
        OWNER, CARETAKER, VIEWER, OTHER;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public String translationKey() {
            return "gui.bettacraft.users.role." + id();
        }

        public static Role byId(String id) {
            for (Role r : values()) if (r.id().equals(id)) return r;
            return VIEWER;
        }

        /** A role a listed user can have. */
        public boolean listable() {
            return this == CARETAKER || this == VIEWER;
        }
    }

    /** One listed user: the last known name (for the menu when he is offline), his role and his own waypoint switch. */
    public static final class Entry {
        public final UUID id;
        public String name;
        public Role role;
        public boolean waypoint;

        public Entry(UUID id, String name, Role role, boolean waypoint) {
            this.id = id;
            this.name = name == null ? "" : name;
            this.role = role.listable() ? role : Role.VIEWER;
            this.waypoint = waypoint;
        }

        Entry copy() {
            return new Entry(id, name, role, waypoint);
        }
    }

    private final Map<UUID, Entry> entries = new LinkedHashMap<>();
    /** The owner's own waypoint switch (default on). */
    private boolean ownerWaypoint = true;
    /** The waypoints of this tank for all of its users (owner, caretakers and viewers can only change it for all). */
    private boolean waypointsAll = true;

    public List<Entry> entries() {
        return List.copyOf(entries.values());
    }

    @Nullable
    public Entry get(@Nullable UUID id) {
        return id == null ? null : entries.get(id);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }

    /** Adds or updates a user; false when the list is full. A new user's own waypoint switch is off. */
    public boolean put(UUID id, String name, Role role) {
        if (!role.listable()) return false;
        Entry e = entries.get(id);
        if (e != null) {
            e.role = role;
            if (name != null && !name.isEmpty()) e.name = name;
            return true;
        }
        if (entries.size() >= MAX) return false;
        entries.put(id, new Entry(id, name, role, false));
        return true;
    }

    public boolean remove(UUID id) {
        return entries.remove(id) != null;
    }

    public boolean ownerWaypoint() {
        return ownerWaypoint;
    }

    public void setOwnerWaypoint(boolean on) {
        ownerWaypoint = on;
    }

    public boolean waypointsAll() {
        return waypointsAll;
    }

    public void setWaypointsAll(boolean on) {
        waypointsAll = on;
    }

    /** The own waypoint switch of {@code id} ({@code owner} = the tank's owner); false for a player who is no user. */
    public boolean waypoint(UUID id, @Nullable UUID owner) {
        if (id.equals(owner)) return ownerWaypoint;
        Entry e = entries.get(id);
        return e != null && e.waypoint;
    }

    /** Sets the own waypoint switch of {@code id}; false when he is neither the owner nor listed. */
    public boolean setWaypoint(UUID id, @Nullable UUID owner, boolean on) {
        if (id.equals(owner)) {
            ownerWaypoint = on;
            return true;
        }
        Entry e = entries.get(id);
        if (e == null) return false;
        e.waypoint = on;
        return true;
    }

    public void copyFrom(TankUsers o) {
        entries.clear();
        for (Entry e : o.entries.values()) entries.put(e.id, e.copy());
        ownerWaypoint = o.ownerWaypoint;
        waypointsAll = o.waypointsAll;
    }

    /** Null when there is nothing to save (no users, both switches at their default). */
    @Nullable
    public CompoundTag save() {
        if (entries.isEmpty() && ownerWaypoint && waypointsAll) return null;
        CompoundTag t = new CompoundTag();
        ListTag l = new ListTag();
        for (Entry e : entries.values()) {
            CompoundTag u = new CompoundTag();
            org.overengineer.bettacraft.util.NbtCompat.putUUID(u, "id", e.id);
            u.putString("name", e.name);
            u.putString("role", e.role.id());
            if (e.waypoint) u.putBoolean("waypoint", true);
            l.add(u);
        }
        t.put("list", l);
        t.putBoolean("owner_waypoint", ownerWaypoint);
        t.putBoolean("waypoints", waypointsAll);
        return t;
    }

    public static TankUsers load(CompoundTag t) {
        TankUsers u = new TankUsers();
        ListTag l = t.getListOrEmpty("list");
        for (int i = 0; i < l.size() && u.entries.size() < MAX; i++) {
            CompoundTag e = l.getCompoundOrEmpty(i);
            if (!org.overengineer.bettacraft.util.NbtCompat.hasUUID(e, "id")) continue;
            Role r = Role.byId(e.getStringOr("role", ""));
            u.entries.put(org.overengineer.bettacraft.util.NbtCompat.getUUID(e, "id"), new Entry(org.overengineer.bettacraft.util.NbtCompat.getUUID(e, "id"), e.getStringOr("name", ""), r, e.getBooleanOr("waypoint", false)));
        }
        u.ownerWaypoint = !t.contains("owner_waypoint") || t.getBooleanOr("owner_waypoint", false);
        u.waypointsAll = !t.contains("waypoints") || t.getBooleanOr("waypoints", false);
        return u;
    }

    /** The listed users' ids (for the waypoints). */
    public List<UUID> ids() {
        return new ArrayList<>(entries.keySet());
    }
}
