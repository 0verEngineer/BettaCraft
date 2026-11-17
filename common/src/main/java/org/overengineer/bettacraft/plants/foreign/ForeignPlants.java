package org.overengineer.bettacraft.plants.foreign;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.foreign.ForeignProfiles;
import org.overengineer.bettacraft.plants.AquaticPlantBlock;
import org.overengineer.bettacraft.tank.interior.InteriorEquipment;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M38: plants and corals of Minecraft and other mods in the tank interior. Their profiles come from
 * {@code data/<ns>/bettacraft/foreign_plants/*.json} (server data, {@code /reload}); a later file for the same block replaces an
 * earlier one. The interior keeps an index of their blocks ({@link InteriorEquipment#foreignPlants}); the blocks of one profile
 * that touch each other are one plant, which works like a full-grown plant of ours of the same growth form times
 * {@code interiorPlants.foreignPlantFactor} (and the profile's factor): plant mass for the nitrate, the waste, breeding,
 * hiding, the small animals, oxygen and algae. Floating leaves shade the surface, live rock filters salt water a little.
 * <p>
 * They never grow, spread or change, and they have no needs: no light minimum, no fertilizer (M37), no reef elements, no
 * salinity. At most {@code interiorPlants.maxPlantsPerTank} of them count per tank. Switch: {@code features.aquaticPlants.foreignPlants}.
 */
public final class ForeignPlants extends SimplePreparableReloadListener<Map<Identifier, ForeignPlantProfile>> {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/foreign_plants");
    private static final FileToIdConverter FILES = FileToIdConverter.json("bettacraft/foreign_plants");
    /** The files of the last {@link #set} (GameTests add their own and restore these). */
    private static volatile Map<Identifier, ForeignPlantProfile> files = Map.of();
    /** Block -> its profile (resolved lazily: tags are bound after the reload). */
    private static final Map<Block, Entry> BY_BLOCK = new ConcurrentHashMap<>();
    private static final Entry NO_PROFILE = new Entry(null, null);
    /** Bumped whenever profiles or tags change (cached effects are computed again). */
    private static volatile int generation = 1;
    /** Hash of every block that has a profile (an interior index of another one is scanned again), 0 = not computed. */
    private static volatile int signature;
    /** Warnings about a block id missing from a loaded mod so far (GameTests). */
    public static int renamedWarnings;

    /** The profile of a block with the file that maps it. */
    public record Entry(@Nullable ForeignPlantProfile profile, @Nullable Identifier file) {}

    /** One plant of a tank: its first block (the lowest), the profile file, its blocks and the plant mass it counts with. */
    public record Group(Block block, Identifier file, ForeignPlantRole role, int blocks, double mass) {}

    /**
     * What the plants of other mods in a region do: the plants that count (at most maxPlantsPerTank), their plant mass
     * (interior blocks), the floating blocks (shade), the live rock blocks, and the plants over the limit.
     */
    public record Effect(List<Group> groups, double mass, int floating, int liveRock, int overLimit) {
        public static final Effect NONE = new Effect(List.of(), 0, 0, 0, 0);

        public int plants() {
            return groups.size();
        }
    }



    // ------------------------------------------------------------------ the profiles

    /** On with {@code features.aquaticPlants.foreignPlants}. */
    public static boolean enabled() {
        return Features.enabled(Feature.FOREIGN_PLANTS);
    }

    public static double factor() {
        return ServerConfig.getOr(ServerConfig.FOREIGN_PLANT_FACTOR, 0.6);
    }

    /** The profile files as loaded (by id). */
    public static Map<Identifier, ForeignPlantProfile> files() {
        return files;
    }

    /** The profile and file of this block (no profile: both null; our own plants never have one). */
    public static Entry entry(Block b) {
        Entry e = BY_BLOCK.get(b);
        if (e == null) {
            e = resolve(b);
            BY_BLOCK.put(b, e);
        }
        return e;
    }

    /** The profile of this block (null: none, or one with the role {@code none}). */
    @Nullable
    public static ForeignPlantProfile of(BlockState s) {
        return s.isAir() ? null : entry(s.getBlock()).profile();
    }

    /** Whether the interior index keeps this block (it has a profile; the switch is asked when it counts). */
    public static boolean indexed(BlockState s) {
        return of(s) != null;
    }

    private static Entry resolve(Block b) {
        if (b instanceof AquaticPlantBlock) return NO_PROFILE;
        Identifier id = BuiltInRegistries.BLOCK.getKey(b);
        if (BettaCraft.MOD_ID.equals(id.getNamespace())) return NO_PROFILE;
        Entry found = NO_PROFILE;
        BlockState st = b.defaultBlockState();
        for (Map.Entry<Identifier, ForeignPlantProfile> f : files.entrySet()) {
            ForeignPlantProfile p = f.getValue();
            boolean match = p.blocks().contains(id);
            if (!match) {
                for (Identifier t : p.tags()) {
                    if (st.is(TagKey.create(Registries.BLOCK, t))) {
                        match = true;
                        break;
                    }
                }
            }
            if (match) found = p.role() == ForeignPlantRole.NONE ? NO_PROFILE : new Entry(p, f.getKey());
        }
        return found;
    }

    /** Profiles or tags changed: resolve again, compute the effects again, scan the interiors again if other blocks count now. */
    static void changed() {
        BY_BLOCK.clear();
        signature = 0;
        generation++;
    }

    public static int generation() {
        return generation;
    }

    /** Hash of the ids of every block with a profile (never 0). */
    public static int signature() {
        int s = signature;
        if (s != 0) return s;
        List<String> ids = new ArrayList<>();
        for (Block b : BuiltInRegistries.BLOCK) if (entry(b).profile() != null) ids.add(BuiltInRegistries.BLOCK.getKey(b).toString());
        ids.sort(String::compareTo);
        s = ids.hashCode();
        if (s == 0) s = 1;
        signature = s;
        return s;
    }

    @Override
    protected Map<Identifier, ForeignPlantProfile> prepare(ResourceManager rm, ProfilerFiller profiler) {
        Map<Identifier, ForeignPlantProfile> out = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> e : FILES.listMatchingResources(rm).entrySet()) {
            Identifier id = FILES.fileToId(e.getKey());
            try (Reader r = e.getValue().openAsReader()) {
                JsonElement json = JsonParser.parseReader(r);
                out.put(id, ForeignPlantProfile.parse(json.getAsJsonObject(), w -> LOGGER.warn("{}: {}", id, w)));
            } catch (Exception ex) {
                LOGGER.warn("Ignoring foreign plant profile {}: {}", id, ex.getMessage());
            }
        }
        return out;
    }

    @Override
    protected void apply(Map<Identifier, ForeignPlantProfile> files, ResourceManager rm, ProfilerFiller profiler) {
        set(files);
    }

    /** Puts the profiles in place (reload, GameTests) and warns about block ids that are missing. Returns the ids missing. */
    public static synchronized List<Identifier> set(Map<Identifier, ForeignPlantProfile> files) {
        ForeignPlants.files = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(files));
        changed();
        List<Identifier> missing = new ArrayList<>();
        int known = 0;
        for (Map.Entry<Identifier, ForeignPlantProfile> f : files.entrySet()) {
            for (Identifier bid : f.getValue().blocks()) {
                if (BuiltInRegistries.BLOCK.containsKey(bid)) {
                    known++;
                    continue;
                }
                missing.add(bid);
                if (modLoaded(bid.getNamespace())) {
                    // the mod is there but not this id: probably renamed in a newer version of the mod (like M33 part d)
                    renamedWarnings++;
                    List<String> like = ForeignProfiles.similar(bid.getPath(), pathsOf(bid.getNamespace()), 3);
                    LOGGER.warn("Foreign plant profile {}: block {} is not registered although the mod {} is loaded (renamed?); {}",
                            f.getKey(), bid, bid.getNamespace(), like.isEmpty() ? "no block of that mod has a similar id"
                                    : "similar ids of that mod: " + String.join(", ", like.stream().map(x -> bid.getNamespace() + ":" + x).toList()));
                } else if (f.getValue().required()) {
                    LOGGER.warn("{}: block {} is not registered", f.getKey(), bid);
                }
            }
        }
        LOGGER.info("Loaded {} foreign plant profiles for {} blocks ({} of mods not installed)", files.size(), known, missing.size());
        return missing;
    }

    /** Whether the mod of this namespace is loaded (by its mod id, or because it has blocks registered under it). */
    static boolean modLoaded(String namespace) {
        if ("minecraft".equals(namespace)) return true;
        if (org.overengineer.bettacraft.platform.Platform.available() && org.overengineer.bettacraft.platform.Platform.get().isModLoaded(namespace)) return true;
        return !pathsOf(namespace).isEmpty();
    }

    static List<String> pathsOf(String namespace) {
        List<String> out = new ArrayList<>();
        for (Identifier id : BuiltInRegistries.BLOCK.keySet()) if (id.getNamespace().equals(namespace)) out.add(id.getPath());
        return out;
    }

    // ------------------------------------------------------------------ what they do

    /** The effect of the plants of other mods in this region (nothing with the switch off; cached until the index changes). */
    public static Effect effect(MinecraftServer server, @Nullable InteriorRegion r) {
        if (r == null || !r.built() || !enabled()) return Effect.NONE;
        InteriorEquipment eq = InteriorManager.equipment(server, r);
        if (eq.foreignPlants().isEmpty()) return Effect.NONE;
        double factor = factor();
        int cap = ServerConfig.getOr(ServerConfig.AQUATIC_MAX_PER_TANK, 48);
        long key = ((long) eq.foreignVersion() << 32) ^ ((long) generation << 16) ^ r.scale() ^ ((long) cap << 40) ^ Double.hashCode(factor);
        Effect cached = eq.foreignEffect(key);
        if (cached != null) return cached;
        Effect e = compute(eq.foreignPlants(), r.scale(), factor, cap);
        eq.cacheForeignEffect(key, e);
        return e;
    }

    /** Plant mass in interior blocks the plants of other mods in this region count with (0 without, or switched off). */
    public static double mass(MinecraftServer server, @Nullable InteriorRegion r) {
        return effect(server, r).mass();
    }

    /**
     * Groups the indexed blocks into plants (6-connected blocks of the same profile file), the lowest block first, in a
     * fixed order (by position), and sums their mass; the plants after the first {@code cap} do not count.
     */
    public static Effect compute(Long2ObjectMap<Block> blocks, int scale, double factor, int cap) {
        long[] keys = blocks.keySet().toLongArray();
        Arrays.sort(keys);
        LongOpenHashSet seen = new LongOpenHashSet();
        List<Group> groups = new ArrayList<>();
        double mass = 0;
        int floating = 0, rock = 0, over = 0;
        for (long k : keys) {
            if (seen.contains(k)) continue;
            Block b = blocks.get(k);
            Entry en = entry(b);
            ForeignPlantProfile p = en.profile();
            seen.add(k);
            if (p == null) continue;
            int n = 1;
            long lowest = k;
            // flood fill over the blocks of the same profile file
            LongArrayFIFOQueue q = new LongArrayFIFOQueue();
            q.enqueue(k);
            while (!q.isEmpty()) {
                long c = q.dequeueLong();
                for (Direction d : Direction.values()) {
                    long nk = BlockPos.offset(c, d);
                    if (seen.contains(nk)) continue;
                    Block nb = blocks.get(nk);
                    if (nb == null || !en.file().equals(entry(nb).file())) continue;
                    seen.add(nk);
                    q.enqueue(nk);
                    n++;
                    if (BlockPos.getY(nk) < BlockPos.getY(lowest)) lowest = nk;
                }
            }
            if (p.role() == ForeignPlantRole.LIVE_ROCK) {
                rock += n;
                continue;
            }
            if (p.role() == ForeignPlantRole.FLOATING) floating += n;
            if (groups.size() >= cap) {
                over++;
                continue;
            }
            double m = factor * p.factor() * p.role().adultMass(scale);
            groups.add(new Group(blocks.get(lowest), en.file(), p.role(), n, m));
            mass += m;
        }
        return new Effect(List.copyOf(groups), mass, floating, rock, over);
    }

    /** Live rock blocks of other mods in the region that count (0 with the switch off). */
    public static int liveRock(MinecraftServer server, @Nullable InteriorRegion r) {
        return effect(server, r).liveRock();
    }
}
