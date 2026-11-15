package org.overengineer.bettacraft.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.registry.ModCodecs;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M22.3: the wild tankmates, seahorses, dwarf clawed frogs and small animals in the biomes of their waters
 * ({@link Wildlife}: the net's waters by biome: jungle, swamp, river, mangrove, beach, warm and lukewarm ocean). Weights
 * from the COMMON config ({@code spawn.wildFishWeight}, {@code spawn.wildCritterWeight}); the spawn rules
 * ({@link Wildlife#checkFish}, {@link Wildlife#checkCritter}) keep them rare and never in a tank. Species switched off
 * (green neon, seahorses) are left out.
 */
public record WildlifeSpawnsBiomeModifier() implements BiomeModifier {
    public static final MapCodec<WildlifeSpawnsBiomeModifier> CODEC = MapCodec.unit(WildlifeSpawnsBiomeModifier::new);

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD) return;
        // M31 part b: the wild turtles (creatures on the shore of swamps and rivers, the tortoise on dry land)
        if (org.overengineer.bettacraft.terrarium.TurtleSpawns.enabled()) {
            for (var e : org.overengineer.bettacraft.terrarium.TurtleSpawns.entries(org.overengineer.bettacraft.terrarium.TurtleSpawns.home(biome))) {
                var type = ModEntities.turtle(e.species());
                int[] g = org.overengineer.bettacraft.terrarium.TurtleSpawns.group(e.species());
                builder.getMobSpawnSettings().addSpawn(type.getCategory(), new MobSpawnSettings.SpawnerData(type, e.weight(), g[0], g[1]));
            }
        }
        NetCatching.Water w = Wildlife.water(biome);
        if (w == NetCatching.Water.NONE) return;
        var spawns = builder.getMobSpawnSettings();
        if (org.overengineer.bettacraft.config.CommonConfig.wildFish()) {
            for (TankmateSpecies s : TankmateSpecies.values()) {
                if (!s.available()) continue;
                int weight = Wildlife.fishWeight(s, w);
                if (weight <= 0) continue;
                int[] g = Wildlife.group(s);
                var type = ModEntities.TANKMATES.get(s).get();
                spawns.addSpawn(type.getCategory(), new MobSpawnSettings.SpawnerData(type, weight, g[0], g[1]));
            }
            int frog = Wildlife.frogWeight(w);
            if (frog > 0) spawns.addSpawn(ModEntities.DWARF_FROG.get().getCategory(), new MobSpawnSettings.SpawnerData(ModEntities.DWARF_FROG.get(), frog, 1, 2));
        }
        if (org.overengineer.bettacraft.config.CommonConfig.wildCritters()) {
            int c = Wildlife.critterWeight(w);
            if (c > 0) spawns.addSpawn(ModEntities.WILD_CRITTER.get().getCategory(), new MobSpawnSettings.SpawnerData(ModEntities.WILD_CRITTER.get(), c, 2, 5));
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return ModCodecs.WILDLIFE_SPAWNS.get();
    }
}
