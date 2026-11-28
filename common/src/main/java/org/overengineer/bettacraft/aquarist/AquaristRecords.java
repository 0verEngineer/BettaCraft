package org.overengineer.bettacraft.aquarist;

import org.overengineer.bettacraft.util.NbtCompat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.overengineer.bettacraft.data.schema.Schemas;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M51: what the server remembers about the hired aquarists ({@code data/bettacraft_aquarists.dat} of the overworld): per
 * player his aquarist (offering or hired), when the next one may come after a refusal or a departure and how often the
 * player bred something; and an index of the tanks with an owner (from the care step of every loaded tank), so the
 * arrival can count a player's tanks and an aquarist can find the tanks around his workplace without scanning chunks.
 * Worlds of earlier versions have no file: it starts empty and fills as the tanks tick. M69: a team of aquarists per player
 * ({@link Member}), the state of the friend offer and the tanks given to one member in the menu.
 */
public final class AquaristRecords extends SavedData {
    public static final String NAME = "bettacraft_aquarists";

    /**
     * M69: one aquarist of a player's team (offering or hired), as the records know him also while he is not loaded: for the
     * conditions of the next friend, the team tab of every member's menu and the tanks each one looks after.
     */
    public static final class Member {
        public final UUID id;
        public boolean hired;
        /** Game time he was hired (-1: before M69, set on first use). */
        public long since;
        /** Game time since which his wage was paid every morning without a gap (the friend's "days without a break"). */
        public long paidSince;
        /** Game time of his last trouble: a day without wage, or a tank in his care with bad water (Long.MIN_VALUE: none). */
        public long trouble = Long.MIN_VALUE;
        /** His name, his status ({@link AquaristEntity#computeStatus}), the tanks he looks after and his workplace, as last seen. */
        public String name = "", status = "";
        public int cared;
        @Nullable
        public ResourceKey<Level> dimension;
        @Nullable
        public BlockPos workplace;

        public Member(UUID id, boolean hired, long since) {
            this.id = id;
            this.hired = hired;
            this.since = since;
            this.paidSince = since;
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            NbtCompat.putUUID(t, "id", id);
            t.putBoolean("hired", hired);
            t.putLong("since", since);
            t.putLong("paid_since", paidSince);
            if (trouble != Long.MIN_VALUE) t.putLong("trouble", trouble);
            t.putString("name", name);
            t.putString("status", status);
            t.putInt("cared", cared);
            if (dimension != null && workplace != null) {
                t.putString("dim", dimension.identifier().toString());
                t.putLong("workplace", workplace.asLong());
            }
            return t;
        }

        static Member load(CompoundTag t) {
            Member m = new Member(NbtCompat.getUUID(t, "id"), t.getBooleanOr("hired", false), t.contains("since") ? t.getLongOr("since", 0L) : -1L);
            m.paidSince = t.contains("paid_since") ? t.getLongOr("paid_since", 0L) : m.since;
            m.trouble = t.contains("trouble") ? t.getLongOr("trouble", 0L) : Long.MIN_VALUE;
            m.name = t.getStringOr("name", "");
            m.status = t.getStringOr("status", "");
            m.cared = t.getIntOr("cared", 0);
            Identifier dim = t.contains("dim") ? Identifier.tryParse(t.getStringOr("dim", "")) : null;
            if (dim != null && t.contains("workplace")) {
                m.dimension = ResourceKey.create(Registries.DIMENSION, dim);
                m.workplace = BlockPos.of(t.getLongOr("workplace", 0L));
            }
            return m;
        }
    }

    /** One player. */
    public static final class PlayerRecord {
        /**
         * M69: his aquarists in the order they came (offering or hired); the hired one who is with him longest recommends a
         * friend (the "mediator"). Before M69 a player had exactly one ({@code npc}, {@code hired}; migrated by
         * {@link Schemas#AQUARIST_RECORDS}).
         */
        public final List<Member> team = new ArrayList<>();
        /** Game time until which an offering aquarist waits for the answer. */
        public long offerUntil = Long.MAX_VALUE;
        /** Game time before which no aquarist comes (after a refusal or a departure). */
        public long notBefore = Long.MIN_VALUE;
        /** Breedings counted since M51 (the breeding advancement events). */
        public int breedings;
        /** {@link #breedings} when the last one was refused or left (-1: never); the next needs another breeding. */
        public int breedingsAtLeave = -1;
        /** How often the player refused one, and how often one left him. */
        public int refused, left;
        /** The player's name (for the menus and messages of his aquarist while he is offline). */
        public String name = "";
        /** M69: {@link #breedings} at the last hiring (or the last friend refused): the next friend needs another breeding. */
        public int breedingsAtHire;
        /** M69: game time of the last hiring (the first aquarist or a friend; -1: before M69, set on first use). */
        public long lastHire = -1L;
        /** M69: game time before which no friend is offered (after one was refused). */
        public long friendNotBefore = Long.MIN_VALUE;
        /** M69: a friend is offered now (the chat message was sent); his first name. */
        public boolean friendOffered;
        public String friendName = "";
        /** M69: friends refused. */
        public int friendsRefused;

        /** The first aquarist of the team (offering or hired), or null: none. */
        @Nullable
        public UUID npc() {
            return team.isEmpty() ? null : team.get(0).id;
        }

        /** Whether the first aquarist of the team is hired. */
        public boolean hired() {
            return !team.isEmpty() && team.get(0).hired;
        }

        @Nullable
        public Member member(UUID id) {
            for (Member m : team) if (m.id.equals(id)) return m;
            return null;
        }

        public int hiredCount() {
            int n = 0;
            for (Member m : team) if (m.hired) n++;
            return n;
        }

        /** The hired aquarist who has been with the player longest: he recommends the next friend. Null: none hired. */
        @Nullable
        public Member mediator() {
            Member best = null;
            for (Member m : team) if (m.hired && (best == null || m.since < best.since)) best = m;
            return best;
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            ListTag tl = new ListTag();
            for (Member m : team) tl.add(m.save());
            t.put("team", tl);
            if (offerUntil != Long.MAX_VALUE) t.putLong("offer_until", offerUntil);
            if (notBefore != Long.MIN_VALUE) t.putLong("not_before", notBefore);
            t.putInt("breedings", breedings);
            t.putInt("breedings_at_leave", breedingsAtLeave);
            t.putInt("refused", refused);
            t.putInt("left", left);
            t.putString("name", name);
            t.putInt("breedings_at_hire", breedingsAtHire);
            t.putLong("last_hire", lastHire);
            if (friendNotBefore != Long.MIN_VALUE) t.putLong("friend_not_before", friendNotBefore);
            t.putBoolean("friend_offered", friendOffered);
            t.putString("friend_name", friendName);
            t.putInt("friends_refused", friendsRefused);
            return t;
        }

        static PlayerRecord load(CompoundTag t) {
            PlayerRecord r = new PlayerRecord();
            for (Tag x : t.getListOrEmpty("team")) {
                if (x instanceof CompoundTag m && NbtCompat.hasUUID(m, "id")) r.team.add(Member.load(m));
            }
            r.offerUntil = t.contains("offer_until") ? t.getLongOr("offer_until", 0L) : Long.MAX_VALUE;
            r.notBefore = t.contains("not_before") ? t.getLongOr("not_before", 0L) : Long.MIN_VALUE;
            r.breedings = t.getIntOr("breedings", 0);
            r.breedingsAtLeave = t.contains("breedings_at_leave") ? t.getIntOr("breedings_at_leave", 0) : -1;
            r.refused = t.getIntOr("refused", 0);
            r.left = t.getIntOr("left", 0);
            r.name = t.getStringOr("name", "");
            r.breedingsAtHire = t.getIntOr("breedings_at_hire", 0);
            r.lastHire = t.contains("last_hire") ? t.getLongOr("last_hire", 0L) : -1L;
            r.friendNotBefore = t.contains("friend_not_before") ? t.getLongOr("friend_not_before", 0L) : Long.MIN_VALUE;
            r.friendOffered = t.getBooleanOr("friend_offered", false);
            r.friendName = t.getStringOr("friend_name", "");
            r.friendsRefused = t.getIntOr("friends_refused", 0);
            return r;
        }
    }

    /** One tank of the index: its owner, level and controller position. */
    public record TankEntry(UUID tank, UUID owner, ResourceKey<Level> dimension, BlockPos pos) {}

    private final Map<UUID, PlayerRecord> players = new HashMap<>();
    private final Map<UUID, TankEntry> tanks = new HashMap<>();
    /** M69: the tanks given to one aquarist of the team in his menu (tank id -> aquarist); the others go to the nearest workplace. */
    private final Map<UUID, UUID> assigned = new HashMap<>();

    public static AquaristRecords get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(org.overengineer.bettacraft.util.NbtCompat.savedData(NAME, AquaristRecords::new, AquaristRecords::load, AquaristRecords::save));
    }

    public PlayerRecord player(UUID id) {
        PlayerRecord r = players.get(id);
        if (r == null) {
            r = new PlayerRecord();
            players.put(id, r);
            setDirty();
        }
        return r;
    }

    @Nullable
    public PlayerRecord peek(UUID id) {
        return players.get(id);
    }

    public Map<UUID, PlayerRecord> players() {
        return players;
    }

    /** The player whose aquarist this is (offering or hired, any member of his team), or null. */
    @Nullable
    public UUID employerOf(UUID npc) {
        for (var e : players.entrySet()) if (e.getValue().member(npc) != null) return e.getKey();
        return null;
    }

    /** M69: the aquarist a tank was given to in the menu (null: the nearest workplace decides). */
    @Nullable
    public UUID assignedTo(UUID tank) {
        return assigned.get(tank);
    }

    /** M69: the tanks given to {@code npc} go back to the nearest workplace (he left). */
    public void unassignAll(UUID npc) {
        if (assigned.values().removeIf(npc::equals)) setDirty();
    }

    /** M69: gives the tank to one aquarist (null: back to the nearest workplace). */
    public void assign(UUID tank, @Nullable UUID npc) {
        if (npc == null ? assigned.remove(tank) != null : !npc.equals(assigned.put(tank, npc))) setDirty();
    }

    // ------------------------------------------------------------------ tank index

    /** From the care step of a tank: notes (or updates) its owner and place; a tank without an owner is left out. */
    public void noteTank(ServerLevel level, TankBlockEntity c) {
        var t = c.getTank();
        if (t == null) return;
        UUID owner = t.owner();
        TankEntry old = tanks.get(t.id());
        if (owner == null) {
            if (old != null) {
                tanks.remove(t.id());
                setDirty();
            }
            return;
        }
        if (old != null && old.owner().equals(owner) && old.dimension().equals(level.dimension()) && old.pos().equals(c.getBlockPos())) return;
        tanks.put(t.id(), new TankEntry(t.id(), owner, level.dimension(), c.getBlockPos().immutable()));
        setDirty();
    }

    public void forgetTank(UUID tank) {
        if (tanks.remove(tank) != null) setDirty();
    }

    @Nullable
    public TankEntry tank(UUID id) {
        return tanks.get(id);
    }

    /**
     * The tank of an entry if its controller is loaded and still that tank; a loaded entry whose tank is gone (broken,
     * split into new tanks) is dropped. Null when it is not loaded or gone.
     */
    @Nullable
    public TankBlockEntity resolve(MinecraftServer server, TankEntry e) {
        ServerLevel level = server.getLevel(e.dimension());
        if (level == null || !level.isLoaded(e.pos())) return null;
        TankBlockEntity c = TankMultiblock.getController(level, e.pos());
        if (c == null || c.getTank() == null || !c.getTank().id().equals(e.tank())) {
            forgetTank(e.tank()); // the chunk is loaded but the tank is not there any more
            return null;
        }
        return c;
    }

    /** Tanks of the index owned by {@code owner} (loaded ones checked; unloaded ones counted as they were last seen). */
    public int ownTanks(MinecraftServer server, UUID owner) {
        int n = 0;
        for (TankEntry e : new ArrayList<>(tanks.values())) {
            if (!owner.equals(e.owner())) continue;
            ServerLevel level = server.getLevel(e.dimension());
            if (level != null && level.isLoaded(e.pos()) && resolve(server, e) == null) continue;
            n++;
        }
        return n;
    }

    /** Index entries in {@code dimension} within {@code radius} blocks of {@code center}, nearest first. */
    public List<TankEntry> near(ResourceKey<Level> dimension, BlockPos center, int radius) {
        List<TankEntry> out = new ArrayList<>();
        long r2 = (long) radius * radius;
        for (TankEntry e : tanks.values()) {
            if (!e.dimension().equals(dimension)) continue;
            long dx = e.pos().getX() - center.getX(), dz = e.pos().getZ() - center.getZ();
            if (dx * dx + dz * dz <= r2 && Math.abs(e.pos().getY() - center.getY()) <= radius) out.add(e);
        }
        out.sort(java.util.Comparator.comparingDouble(e -> e.pos().distSqr(center)));
        return out;
    }

    public int tankCount() {
        return tanks.size();
    }

    // ------------------------------------------------------------------ save

    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        Schemas.AQUARIST_RECORDS.stamp(tag);
        ListTag pl = new ListTag();
        for (var e : players.entrySet()) {
            CompoundTag t = e.getValue().save();
            NbtCompat.putUUID(t, "player", e.getKey());
            pl.add(t);
        }
        tag.put("players", pl);
        ListTag tl = new ListTag();
        for (TankEntry e : tanks.values()) {
            CompoundTag t = new CompoundTag();
            NbtCompat.putUUID(t, "tank", e.tank());
            NbtCompat.putUUID(t, "owner", e.owner());
            t.putString("dim", e.dimension().identifier().toString());
            t.putLong("pos", e.pos().asLong());
            tl.add(t);
        }
        tag.put("tanks", tl);
        ListTag al = new ListTag();
        for (var e : assigned.entrySet()) {
            CompoundTag t = new CompoundTag();
            NbtCompat.putUUID(t, "tank", e.getKey());
            NbtCompat.putUUID(t, "npc", e.getValue());
            al.add(t);
        }
        tag.put("assigned", al);
        return tag;
    }

    public static AquaristRecords load(CompoundTag raw, HolderLookup.Provider registries) {
        CompoundTag tag = Schemas.AQUARIST_RECORDS.migrate(raw);
        AquaristRecords r = new AquaristRecords();
        for (Tag x : tag.getListOrEmpty("players")) {
            CompoundTag t = (CompoundTag) x;
            if (NbtCompat.hasUUID(t, "player")) r.players.put(NbtCompat.getUUID(t, "player"), PlayerRecord.load(t));
        }
        for (Tag x : tag.getListOrEmpty("tanks")) {
            CompoundTag t = (CompoundTag) x;
            if (!NbtCompat.hasUUID(t, "tank") || !NbtCompat.hasUUID(t, "owner")) continue;
            Identifier dim = Identifier.tryParse(t.getStringOr("dim", ""));
            if (dim == null) continue;
            UUID id = NbtCompat.getUUID(t, "tank");
            r.tanks.put(id, new TankEntry(id, NbtCompat.getUUID(t, "owner"), ResourceKey.create(Registries.DIMENSION, dim), BlockPos.of(t.getLongOr("pos", 0L))));
        }
        for (Tag x : tag.getListOrEmpty("assigned")) {
            CompoundTag t = (CompoundTag) x;
            if (NbtCompat.hasUUID(t, "tank") && NbtCompat.hasUUID(t, "npc")) r.assigned.put(NbtCompat.getUUID(t, "tank"), NbtCompat.getUUID(t, "npc"));
        }
        if (Schemas.AQUARIST_RECORDS.needsMigration(raw)) r.setDirty();
        return r;
    }

    /** GameTests: forgets everything about this player (his tanks stay in the index). */
    public void reset(UUID player) {
        players.remove(player);
        setDirty();
    }

    /** GameTests: drops the index entries of the tanks in {@code dimension} inside the box. */
    public void forgetBox(ResourceKey<Level> dimension, BlockPos min, BlockPos max) {
        for (Iterator<TankEntry> it = tanks.values().iterator(); it.hasNext(); ) {
            TankEntry e = it.next();
            BlockPos p = e.pos();
            if (e.dimension().equals(dimension) && p.getX() >= min.getX() && p.getX() <= max.getX() && p.getY() >= min.getY() && p.getY() <= max.getY()
                    && p.getZ() >= min.getZ() && p.getZ() <= max.getZ()) it.remove();
        }
        setDirty();
    }
}
