package org.overengineer.bettacraft.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.registry.ModCodecs;
import org.overengineer.bettacraft.registry.NeoForgeCodecs;
import org.overengineer.bettacraft.registry.ModEntities;

/**
 * Adds wild bettas to {@code #bettacraft:betta_spawn_biomes} (PLAN 5.1). Like {@code neoforge:add_spawns}, but weight
 * and group size come from the COMMON config ({@code spawn.*}), so packs can tune them without a datapack.
 */
public record BettaSpawnsBiomeModifier(HolderSet<Biome> biomes) implements BiomeModifier {
    public static final MapCodec<BettaSpawnsBiomeModifier> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(BettaSpawnsBiomeModifier::biomes)
    ).apply(i, BettaSpawnsBiomeModifier::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD || !biomes.contains(biome)) return;
        for (var s : org.overengineer.bettacraft.world.WildSpawns.bettas()) builder.getMobSpawnSettings().addSpawn(s.category(), s.data());
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return NeoForgeCodecs.BETTA_SPAWNS.get();
    }
}
