package org.overengineer.bettacraft.command;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.fml.ModList;
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
        return ModList.get().isLoaded("regions_unexplored") && ModList.get().isLoaded("biomeswevegone");
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
        var reg = access.registryOrThrow(Registries.BIOME);
        for (Want w : WANTS) {
            var key = ResourceKey.create(Registries.BIOME, ResourceLocation.parse(w.id()));
            var holder = reg.getHolder(key);
            if (holder.isEmpty()) {
                out.add(w.id() + " missing");
                continue;
            }
            Holder<Biome> b = holder.get();
            if (NetCatching.water(b, false) != w.water()) out.add(w.id() + ": net water " + NetCatching.water(b, false) + ", want " + w.water());
            if (Wildlife.water(b) != w.water()) out.add(w.id() + ": spawn water " + Wildlife.water(b) + ", want " + w.water());
            boolean corals = hasCorals(b);
            if (corals != w.corals() || BiomeWaters.coralReef(b) != w.corals()) out.add(w.id() + ": wild corals " + corals + ", want " + w.corals());
        }
        // the lush caves of Regions Unexplored by the common tags (#c:is_lush and #c:is_cave)
        reg.getHolder(ResourceKey.create(Registries.BIOME, ResourceLocation.parse("regions_unexplored:ancient_delta"))).ifPresent(b -> {
            if (!BiomeWaters.lushCave(b) || NetCatching.water(b, false) != NetCatching.Water.CAVE) out.add("regions_unexplored:ancient_delta is no lush cave");
        });
        return out;
    }

    public static boolean hasCorals(Holder<Biome> b) {
        return b.value().getGenerationSettings().features().stream().anyMatch(set -> set.stream()
                .anyMatch(f -> f.unwrapKey().map(k -> k.location().equals(BettaCraft.id("wild_coral"))).orElse(false)));
    }
}
