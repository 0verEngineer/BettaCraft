package org.overengineer.bettacraft.aquarist;

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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M51: what the server remembers about the hired aquarists ({@code data/bettacraft_aquarists.dat} of the overworld): per
 * player his aquarist (offering or hired), when the next one may come after a refusal or a departure and how often the
 * player bred something; and an index of the tanks with an owner (from the care step of every loaded tank), so the
 * arrival can count a player's tanks and an aquarist can find the tanks around his workplace without scanning chunks.
 * Worlds of earlier versions have no file: it starts empty and fills as the tanks tick.
 */
public final class AquaristRecords extends SavedData {
    public static final String NAME = "bettacraft_aquarists";

    /** One player. */
    public static final class PlayerRecord {
        /** The player's current aquarist (offering or hired); null = none. */
        @Nullable
        public UUID npc;
        public boolean hired;
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

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            if (npc != null) t.putUUID("npc", npc);
            t.putBoolean("hired", hired);
            if (offerUntil != Long.MAX_VALUE) t.putLong("offer_until", offerUntil);
            if (notBefore != Long.MIN_VALUE) t.putLong("not_before", notBefore);
            t.putInt("breedings", breedings);
            t.putInt("breedings_at_leave", breedingsAtLeave);
            t.putInt("refused", refused);
            t.putInt("left", left);
            t.putString("name", name);
            return t;
        }

        static PlayerRecord load(CompoundTag t) {
            PlayerRecord r = new PlayerRecord();
            if (t.hasUUID("npc")) r.npc = t.getUUID("npc");
            r.hired = t.getBoolean("hired");
            r.offerUntil = t.contains("offer_until") ? t.getLong("offer_until") : Long.MAX_VALUE;
            r.notBefore = t.contains("not_before") ? t.getLong("not_before") : Long.MIN_VALUE;
            r.breedings = t.getInt("breedings");
            r.breedingsAtLeave = t.contains("breedings_at_leave") ? t.getInt("breedings_at_leave") : -1;
            r.refused = t.getInt("refused");
            r.left = t.getInt("left");
            r.name = t.getString("name");
            return r;
        }
    }

    /** One tank of the index: its owner, level and controller position. */
    public record TankEntry(UUID tank, UUID owner, ResourceKey<Level> dimension, BlockPos pos) {}

    private final Map<UUID, PlayerRecord> players = new HashMap<>();
    private final Map<UUID, TankEntry> tanks = new HashMap<>();

    public static AquaristRecords get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(AquaristRecords::new, AquaristRecords::load, null), NAME);
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

    /** The player whose aquarist this is (offering or hired), or null. */
    @Nullable
    public UUID employerOf(UUID npc) {
        for (var e : players.entrySet()) if (npc.equals(e.getValue().npc)) return e.getKey();
        return null;
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

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag pl = new ListTag();
        for (var e : players.entrySet()) {
            CompoundTag t = e.getValue().save();
            t.putUUID("player", e.getKey());
            pl.add(t);
        }
        tag.put("players", pl);
        ListTag tl = new ListTag();
        for (TankEntry e : tanks.values()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("tank", e.tank());
            t.putUUID("owner", e.owner());
            t.putString("dim", e.dimension().location().toString());
            t.putLong("pos", e.pos().asLong());
            tl.add(t);
        }
        tag.put("tanks", tl);
        return tag;
    }

    public static AquaristRecords load(CompoundTag tag, HolderLookup.Provider registries) {
        AquaristRecords r = new AquaristRecords();
        for (Tag x : tag.getList("players", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) x;
            if (t.hasUUID("player")) r.players.put(t.getUUID("player"), PlayerRecord.load(t));
        }
        for (Tag x : tag.getList("tanks", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) x;
            if (!t.hasUUID("tank") || !t.hasUUID("owner")) continue;
            ResourceLocation dim = ResourceLocation.tryParse(t.getString("dim"));
            if (dim == null) continue;
            UUID id = t.getUUID("tank");
            r.tanks.put(id, new TankEntry(id, t.getUUID("owner"), ResourceKey.create(Registries.DIMENSION, dim), BlockPos.of(t.getLong("pos"))));
        }
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
