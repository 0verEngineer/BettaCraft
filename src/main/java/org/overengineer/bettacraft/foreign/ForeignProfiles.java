package org.overengineer.bettacraft.foreign;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.terrarium.TerrariumAnimals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M32 part a: the profiles of the foreign animals by entity type, from {@code data/<ns>/bettacraft/foreign_animals/*.json}
 * (server data; {@code /reload} reads them again). A later file for the same type replaces an earlier one (datapacks
 * after the mod), so a modpack can change or remove ({@code "breeding": {"type": "none"}}) a default.
 * <p>
 * Types that have their own care in Bettacraft are never mapped: our own entities, the axolotl (M29), the dolphin (M30),
 * the sea turtle, the mobs in glass and the terrarium animals of our own (M31). A profile with {@code "terrarium"} needs
 * registers the type as a terrarium animal (M31) as well, so it goes into terraria and paludaria by its habitat.
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class ForeignProfiles extends SimplePreparableReloadListener<Map<ResourceLocation, ForeignProfile>> {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/foreign");
    private static final FileToIdConverter FILES = FileToIdConverter.json("bettacraft/foreign_animals");
    private static volatile Map<EntityType<?>, ForeignProfile> byType = Map.of();
    /** M33 part d: warnings about an entity id missing from a loaded mod (renamed?) so far (GameTests). */
    public static int renamedWarnings;
    /** M33 part d: the profile file that maps each type (for {@code /bettacraft foreign list}). */
    private static volatile Map<EntityType<?>, ResourceLocation> fileByType = Map.of();
    /** M39 (question E1): the types our tanks refuse, with the reason of their profile. */
    private static volatile Map<EntityType<?>, String> refused = Map.of();
    /** Types registered as terrarium animals by a profile (taken back on the next reload). */
    private static final Set<EntityType<?>> TERRARIUM = new HashSet<>();
    /** The files of the last {@link #set} (GameTests add their own and restore these). */
    private static volatile Map<ResourceLocation, ForeignProfile> files = Map.of();

    @SubscribeEvent
    static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ForeignProfiles());
    }

    /** The profile of this type, or null (the mapping itself; every user checks its own feature switch). */
    @Nullable
    public static ForeignProfile of(EntityType<?> type) {
        return byType.get(type);
    }

    /** The profile of this animal while our care for foreign animals is on ({@code features.foreignAnimals.care}), or null. */
    @Nullable
    public static ForeignProfile cared(Entity e) {
        return Features.enabled(Feature.FOREIGN_CARE) ? byType.get(e.getType()) : null;
    }

    /**
     * The salinity range that counts for an animal with this profile: the profile's with
     * {@code saltwater.foreignAnimals = REALISTIC}, null (the salt does not matter) with the default LENIENT, as for every
     * animal of vanilla and other mods before.
     */
    @Nullable
    public static org.overengineer.bettacraft.tank.Salinity.Range salinity(ForeignProfile p) {
        return org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.SALT_FOREIGN,
                org.overengineer.bettacraft.config.ServerConfig.ForeignSalinity.LENIENT) == org.overengineer.bettacraft.config.ServerConfig.ForeignSalinity.REALISTIC
                ? p.salinity() : null;
    }

    /** The profile of this type while our care for foreign animals is on, or null. */
    @Nullable
    public static ForeignProfile cared(EntityType<?> type) {
        return Features.enabled(Feature.FOREIGN_CARE) ? byType.get(type) : null;
    }

    @Nullable
    public static ForeignProfile of(Entity e) {
        return of(e.getType());
    }

    /** M33 part d: the id of the profile file that maps this type, or null. */
    @Nullable
    public static ResourceLocation fileOf(EntityType<?> type) {
        return fileByType.get(type);
    }

    /**
     * M39 (question E1): the reason our tanks refuse this type ({@code deep_sea}, {@code too_big}, {@code hostile}, {@code fungus},
     * {@code cave} ...), or null. Whatever the switches: an unsuitable animal stays out.
     */
    @Nullable
    public static String refusal(EntityType<?> type) {
        return refused.get(type);
    }

    /** M39: every refused type with its reason. */
    public static Map<EntityType<?>, String> refused() {
        return refused;
    }

    /** Every mapped type with its profile (handbook, dev command, tests). */
    public static Map<EntityType<?>, ForeignProfile> all() {
        return byType;
    }

    /** The profile files as loaded (by id). */
    public static Map<ResourceLocation, ForeignProfile> files() {
        return files;
    }

    /** Whether Bettacraft has its own care for this type, so a profile must not take it over. */
    public static boolean ownCare(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (BettaCraft.MOD_ID.equals(id.getNamespace())) return true;
        if (type == EntityType.AXOLOTL || type == EntityType.DOLPHIN || type == EntityType.TURTLE || type == EntityType.TADPOLE) return true;
        if (org.overengineer.bettacraft.mobs.GlassMobs.isAllowed(type)) return true;
        return TerrariumAnimals.isRegistered(type) && !TERRARIUM.contains(type);
    }

    /** Whether the mod of this namespace is loaded (by its mod id, or because it has entities registered under it). */
    static boolean modLoaded(String namespace) {
        if ("minecraft".equals(namespace)) return true;
        var mods = net.neoforged.fml.ModList.get();
        if (mods != null && mods.isLoaded(namespace)) return true;
        return !pathsOf(namespace).isEmpty();
    }

    /** The paths of the entity types registered under a namespace. */
    static List<String> pathsOf(String namespace) {
        List<String> out = new ArrayList<>();
        for (ResourceLocation id : BuiltInRegistries.ENTITY_TYPE.keySet()) if (id.getNamespace().equals(namespace)) out.add(id.getPath());
        return out;
    }

    /**
     * M33 part d (pure, unit-tested): up to {@code max} of {@code candidates} that look like {@code missing}, the most
     * similar first: by edit distance, where a candidate that contains the other (or is contained) or shares its last word
     * ({@code _}-separated) counts as close; nothing further away than half of the longer id.
     */
    public static List<String> similar(String missing, java.util.Collection<String> candidates, int max) {
        record Scored(String id, int score) {}
        List<Scored> scored = new ArrayList<>();
        String last = missing.substring(missing.lastIndexOf('_') + 1);
        for (String c : candidates) {
            int d = distance(missing, c);
            boolean close = c.contains(missing) || missing.contains(c) || (last.length() >= 3 && c.substring(c.lastIndexOf('_') + 1).equals(last));
            if (close) d = Math.min(d, Math.abs(c.length() - missing.length()) / 2);
            if (close || d <= Math.max(missing.length(), c.length()) / 2) scored.add(new Scored(c, d));
        }
        scored.sort(java.util.Comparator.comparingInt(Scored::score).thenComparing(Scored::id));
        List<String> out = new ArrayList<>();
        for (int i = 0; i < Math.min(max, scored.size()); i++) out.add(scored.get(i).id());
        return out;
    }

    /** Levenshtein distance. */
    static int distance(String a, String b) {
        int[] prev = new int[b.length() + 1], cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[b.length()];
    }

    @Override
    protected Map<ResourceLocation, ForeignProfile> prepare(ResourceManager rm, ProfilerFiller profiler) {
        Map<ResourceLocation, ForeignProfile> out = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Resource> e : FILES.listMatchingResources(rm).entrySet()) {
            ResourceLocation id = FILES.fileToId(e.getKey());
            try (Reader r = e.getValue().openAsReader()) {
                JsonElement json = JsonParser.parseReader(r);
                out.put(id, ForeignProfile.parse(json.getAsJsonObject(), w -> LOGGER.warn("{}: {}", id, w)));
            } catch (Exception ex) {
                LOGGER.warn("Ignoring foreign animal profile {}: {}", id, ex.getMessage());
            }
        }
        return out;
    }

    @Override
    protected void apply(Map<ResourceLocation, ForeignProfile> files, ResourceManager rm, ProfilerFiller profiler) {
        set(files);
    }

    /** Puts the profiles in place (reload, GameTests). Returns the types now mapped. */
    public static synchronized Map<EntityType<?>, ForeignProfile> set(Map<ResourceLocation, ForeignProfile> files) {
        ForeignProfiles.files = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(files));
        for (EntityType<?> t : TERRARIUM) TerrariumAnimals.unregister(t);
        TERRARIUM.clear();
        Map<EntityType<?>, ForeignProfile> map = new LinkedHashMap<>();
        Map<EntityType<?>, ResourceLocation> fileMap = new LinkedHashMap<>();
        Map<EntityType<?>, String> refusedMap = new LinkedHashMap<>();
        int skipped = 0;
        for (Map.Entry<ResourceLocation, ForeignProfile> f : files.entrySet()) {
            ForeignProfile p = f.getValue();
            for (ResourceLocation eid : p.entities()) {
                var type = BuiltInRegistries.ENTITY_TYPE.getOptional(eid);
                if (type.isEmpty()) {
                    if (modLoaded(eid.getNamespace())) {
                        // M33 part d: the mod is there but not this id: probably renamed in a newer version of the mod
                        renamedWarnings++;
                        List<String> like = similar(eid.getPath(), pathsOf(eid.getNamespace()), 3);
                        LOGGER.warn("Foreign animal profile {}: entity {} is not registered although the mod {} is loaded (renamed?); {}",
                                f.getKey(), eid, eid.getNamespace(), like.isEmpty() ? "no entity of that mod has a similar id"
                                        : "similar ids of that mod: " + String.join(", ", like.stream().map(x -> eid.getNamespace() + ":" + x).toList()));
                    } else if (p.required()) {
                        LOGGER.warn("{}: entity {} is not registered", f.getKey(), eid);
                    }
                    skipped++;
                    continue;
                }
                if (ownCare(type.get())) {
                    LOGGER.warn("{}: {} has Bettacraft's own care and is not mapped", f.getKey(), eid);
                    continue;
                }
                if (p.refuse() != null) { // M39 (E1): refused, nothing else (a later file can map it after all)
                    map.remove(type.get());
                    refusedMap.put(type.get(), p.refuse());
                    fileMap.put(type.get(), f.getKey());
                    continue;
                }
                refusedMap.remove(type.get());
                map.put(type.get(), p);
                fileMap.put(type.get(), f.getKey());
            }
        }
        for (Map.Entry<EntityType<?>, ForeignProfile> e : map.entrySet()) {
            if (e.getValue().terrarium() == null) continue;
            TerrariumAnimals.register(e.getKey(), e.getValue().terrarium(), ForeignTerrarium::enabled); // M35: features.foreignAnimals.terrarium
            TERRARIUM.add(e.getKey());
        }
        fileByType = Map.copyOf(fileMap);
        refused = Map.copyOf(refusedMap);
        byType = Map.copyOf(map);
        LOGGER.info("Loaded {} foreign animal profiles for {} entity types, {} refused ({} of mods not installed)", files.size(), map.size(), refusedMap.size(), skipped);
        return byType;
    }
}
