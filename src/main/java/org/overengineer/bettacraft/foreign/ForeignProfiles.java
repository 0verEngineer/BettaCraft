package org.overengineer.bettacraft.foreign;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.HashSet;
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

    /** The profile of this animal while our care for foreign animals is on ({@code features.foreignCare}), or null. */
    @Nullable
    public static ForeignProfile cared(Entity e) {
        return CommonConfig.foreignCare() ? byType.get(e.getType()) : null;
    }

    /** The profile of this type while our care for foreign animals is on, or null. */
    @Nullable
    public static ForeignProfile cared(EntityType<?> type) {
        return CommonConfig.foreignCare() ? byType.get(type) : null;
    }

    @Nullable
    public static ForeignProfile of(Entity e) {
        return of(e.getType());
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
        return TerrariumAnimals.isTerrariumAnimal(type) && !TERRARIUM.contains(type);
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
        int skipped = 0;
        for (Map.Entry<ResourceLocation, ForeignProfile> f : files.entrySet()) {
            ForeignProfile p = f.getValue();
            for (ResourceLocation eid : p.entities()) {
                var type = BuiltInRegistries.ENTITY_TYPE.getOptional(eid);
                if (type.isEmpty()) {
                    if (p.required()) LOGGER.warn("{}: entity {} is not registered", f.getKey(), eid);
                    skipped++;
                    continue;
                }
                if (ownCare(type.get())) {
                    LOGGER.warn("{}: {} has Bettacraft's own care and is not mapped", f.getKey(), eid);
                    continue;
                }
                map.put(type.get(), p);
            }
        }
        for (Map.Entry<EntityType<?>, ForeignProfile> e : map.entrySet()) {
            if (e.getValue().terrarium() == null) continue;
            TerrariumAnimals.register(e.getKey(), e.getValue().terrarium(), CommonConfig::foreignCare);
            TERRARIUM.add(e.getKey());
        }
        byType = Map.copyOf(map);
        LOGGER.info("Loaded {} foreign animal profiles for {} entity types ({} of mods not installed)", files.size(), map.size(), skipped);
        return byType;
    }
}
