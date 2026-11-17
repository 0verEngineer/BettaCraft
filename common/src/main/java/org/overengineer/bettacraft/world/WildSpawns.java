package org.overengineer.bettacraft.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M52: the natural spawns we add to biomes (M3 bettas, M22.3 the wildlife, M31 the turtles), read once when the biomes are
 * built. NeoForge adds them with its biome modifiers (bettacraft:betta_spawns, bettacraft:wildlife_spawns), Fabric with
 * its biome modification API.
 */
public final class WildSpawns {
    /** M52b: the weight is no longer part of the spawner data (1.21.5 weighted lists). */
    public record Spawn(MobCategory category, MobSpawnSettings.SpawnerData data, int weight) {}

    private WildSpawns() {}

    /** The wild bettas of a biome of {@code #bettacraft:betta_spawn_biomes} (the caller checks the tag). */
    public static List<Spawn> bettas() {
        List<Spawn> out = new ArrayList<>();
        if (!Features.enabled(Feature.BETTAS_WILD)) return out; // M33
        int weight = CommonConfig.getOr(CommonConfig.SPAWN_WEIGHT, 8);
        if (weight <= 0) return out;
        int min = CommonConfig.getOr(CommonConfig.SPAWN_MIN_GROUP, 1);
        int max = Math.max(min, CommonConfig.getOr(CommonConfig.SPAWN_MAX_GROUP, 2));
        out.add(new Spawn(ModEntities.BETTA.get().getCategory(), new MobSpawnSettings.SpawnerData(ModEntities.BETTA.get(), min, max), weight));
        return out;
    }

    /** The wild tankmates, frogs, small animals and turtles of a biome. */
    public static List<Spawn> wildlife(Holder<Biome> biome) {
        List<Spawn> out = new ArrayList<>();
        // M31 part b: the wild turtles (creatures on the shore of swamps and rivers, the tortoise on dry land)
        if (org.overengineer.bettacraft.terrarium.HerpSpawns.enabled()) {
            for (var e : org.overengineer.bettacraft.terrarium.HerpSpawns.entries(org.overengineer.bettacraft.terrarium.HerpSpawns.home(biome))) {
                var type = ModEntities.herp(e.species());
                int[] g = org.overengineer.bettacraft.terrarium.HerpSpawns.group(e.species());
                out.add(new Spawn(type.getCategory(), new MobSpawnSettings.SpawnerData(type, g[0], g[1]), e.weight()));
            }
        }
        NetCatching.Water w = Wildlife.water(biome);
        if (w == NetCatching.Water.NONE) return out;
        if (Features.enabled(Feature.TANKMATES_WILD)) {
            for (TankmateSpecies s : TankmateSpecies.values()) {
                if (!s.available()) continue;
                int weight = Wildlife.fishWeight(s, w);
                if (weight <= 0) continue;
                int[] g = Wildlife.group(s);
                var type = ModEntities.TANKMATES.get(s).get();
                out.add(new Spawn(type.getCategory(), new MobSpawnSettings.SpawnerData(type, g[0], g[1]), weight));
            }
        }
        if (Features.enabled(Feature.FROGS_WILD)) { // M33: the frogs have their own switch
            int frog = Wildlife.frogWeight(w);
            if (frog > 0) out.add(new Spawn(ModEntities.DWARF_FROG.get().getCategory(), new MobSpawnSettings.SpawnerData(ModEntities.DWARF_FROG.get(), 1, 2), frog));
        }
        if (Features.enabled(Feature.INVERTEBRATES_WILD)) {
            int c = Wildlife.critterWeight(w);
            if (c > 0) out.add(new Spawn(ModEntities.WILD_CRITTER.get().getCategory(), new MobSpawnSettings.SpawnerData(ModEntities.WILD_CRITTER.get(), 2, 5), c));
        }
        return out;
    }
}
