package org.overengineer.bettacraft.command;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.world.BiomeWaters;
import org.overengineer.bettacraft.world.Wildlife;

/**
 * M25.1: checks for the dev group {@code -Pbiomes} (Regions Unexplored, Oh The Biomes We've Gone from run/biome-mods):
 * a handful of their biomes have the right water (net and spawns), the wild corals are in their warm ocean, and their
 * swamps get the betta spawns. Used by the GameTest {@code biomeModBiomesMapToTheirWaters} and the server smoke.
 */
public final class DevBiomeMods {
    /** Set by build.gradle with {@code -Pbiomes}: then the biome mods must be there. */
    public static final boolean REQUIRED = Boolean.getBoolean("bettacraft.biomemods");

    private DevBiomeMods() {}

    public static boolean present() {
        // M52c: a mod without a build for this Minecraft version is not needed (Oh The Biomes We've Gone on 26.1.2)
        return loadedOrGap("regions_unexplored") && loadedOrGap("biomeswevegone")
                && (org.overengineer.bettacraft.platform.Platform.get().isModLoaded("regions_unexplored") || org.overengineer.bettacraft.platform.Platform.get().isModLoaded("biomeswevegone"));
    }

    private static boolean loadedOrGap(String mod) {
        return org.overengineer.bettacraft.platform.Platform.get().isModLoaded(mod) || org.overengineer.bettacraft.gametest.framework.VersionGaps.NO_BUILD.contains(mod);
    }

    private record Want(String id, NetCatching.Water water, boolean corals) {}

    private static final List<Want> WANTS = List.of(
            new Want("regions_unexplored:rocky_reef", NetCatching.Water.WARM_OCEAN, true), // warm ocean (own tag)
            new Want("regions_unexplored:bayou", NetCatching.Water.SWAMP, false),          // swamp (#c:is_swamp)
            new Want("regions_unexplored:marsh", NetCatching.Water.SWAMP, false),
            new Want("regions_unexplored:tropical_river", NetCatching.Water.RIVER, false), // river (#minecraft:is_river)
            new Want("regions_unexplored:gravel_beach", NetCatching.Water.BEACH, false),
            new Want("regions_unexplored:rainforest", NetCatching.Water.JUNGLE, false),
            new Want("regions_unexplored:tropics", NetCatching.Water.JUNGLE, false),       // jungle (own tag)
            new Want("regions_unexplored:hyacinth_deeps", NetCatching.Water.NONE, false),  // a cold deep ocean
            new Want("biomeswevegone:white_mangrove_marshes", NetCatching.Water.MANGROVE, false), // mangrove (own tag)
            new Want("biomeswevegone:cypress_swamplands", NetCatching.Water.SWAMP, false),
            new Want("biomeswevegone:tropical_rainforest", NetCatching.Water.JUNGLE, false));

    /** The problems found (empty = all right). */
    public static List<String> problems(RegistryAccess access) {
        List<String> out = new ArrayList<>();
        var reg = access.lookupOrThrow(Registries.BIOME);
        for (Want w : WANTS) {
            String mod = w.id().substring(0, w.id().indexOf(':'));
            if (!org.overengineer.bettacraft.platform.Platform.get().isModLoaded(mod) && org.overengineer.bettacraft.gametest.framework.VersionGaps.NO_BUILD.contains(mod)) continue;
            var key = ResourceKey.create(Registries.BIOME, Identifier.parse(w.id()));
            var holder = reg.get(key);
            if (holder.isEmpty()) {
                out.add(w.id() + " missing");
                continue;
            }
            Holder<Biome> b = holder.get();
            if (NetCatching.water(b, false) != w.water()) out.add(w.id() + ": net water " + NetCatching.water(b, false) + ", want " + w.water());
            if (Wildlife.water(b) != w.water()) out.add(w.id() + ": spawn water " + Wildlife.water(b) + ", want " + w.water());
            boolean corals = hasCorals(b);
            if (corals != w.corals() || BiomeWaters.coralReef(b) != w.corals()) out.add(w.id() + ": wild corals " + corals + ", want " + w.corals());
            // M28.1: the wild plants of the waters (the oceans and beaches have none)
            var plants = switch (w.water()) {
                case RIVER -> org.overengineer.bettacraft.sources.WildTables.PlantWater.RIVER;
                case SWAMP -> org.overengineer.bettacraft.sources.WildTables.PlantWater.SWAMP;
                case MANGROVE -> org.overengineer.bettacraft.sources.WildTables.PlantWater.MANGROVE;
                case JUNGLE -> org.overengineer.bettacraft.sources.WildTables.PlantWater.JUNGLE;
                default -> null;
            };
            if (BiomeWaters.plantWater(b) != plants) out.add(w.id() + ": wild plant water " + BiomeWaters.plantWater(b) + ", want " + plants);
            if (plants != null && !hasFeature(b, "wild_plants")) out.add(w.id() + ": no wild plants feature");
        }
        // the lush caves of Regions Unexplored by the common tags (#c:is_lush and #c:is_cave)
        reg.get(ResourceKey.create(Registries.BIOME, Identifier.parse("regions_unexplored:ancient_delta"))).ifPresent(b -> {
            if (!BiomeWaters.lushCave(b) || NetCatching.water(b, false) != NetCatching.Water.CAVE) out.add("regions_unexplored:ancient_delta is no lush cave");
        });
        return out;
    }

    public static boolean hasFeature(Holder<Biome> b, String id) {
        return b.value().getGenerationSettings().features().stream().anyMatch(set -> set.stream()
                .anyMatch(f -> f.unwrapKey().map(k -> k.identifier().equals(BettaCraft.id(id))).orElse(false)));
    }

    public static boolean hasCorals(Holder<Biome> b) {
        return b.value().getGenerationSettings().features().stream().anyMatch(set -> set.stream()
                .anyMatch(f -> f.unwrapKey().map(k -> k.identifier().equals(BettaCraft.id("wild_coral"))).orElse(false)));
    }
}
