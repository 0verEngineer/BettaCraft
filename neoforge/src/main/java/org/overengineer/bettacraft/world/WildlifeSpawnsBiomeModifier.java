package org.overengineer.bettacraft.world;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.registry.ModCodecs;
import org.overengineer.bettacraft.registry.NeoForgeCodecs;
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
        for (var s : org.overengineer.bettacraft.world.WildSpawns.wildlife(biome)) builder.getMobSpawnSettings().addSpawn(s.category(), s.data());
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return NeoForgeCodecs.WILDLIFE_SPAWNS.get();
    }
}
