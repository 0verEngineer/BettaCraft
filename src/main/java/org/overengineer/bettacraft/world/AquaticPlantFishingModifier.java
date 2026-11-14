package org.overengineer.bettacraft.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModCodecs;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * Fishing sometimes brings up an aquarium plant instead of the catch (M13, chance in the server config
 * {@code interiorPlants.fishingChance}): in jungles and rivers the epiphytes (Java fern, Anubias, Bucephalandra, the
 * "Blue" and "Brownie Red" rarely), in swamps and mangrove swamps Cryptocoryne, Ludwigia and Rotala. M22.4: rarely the
 * Amazon sword and the tiger lotus in jungles, Vallisneria and Monte Carlo in rivers, Alternanthera, the tiger lotus and
 * the Cryptocoryne "Flamingo" in swamps. Applied to the fishing loot table only (condition in the JSON); a catch that
 * already became a floating plant (M12) stays one.
 */
public class AquaticPlantFishingModifier extends LootModifier {
    public static final MapCodec<AquaticPlantFishingModifier> CODEC = RecordCodecBuilder.mapCodec(i -> codecStart(i)
            .apply(i, AquaticPlantFishingModifier::new));

    public AquaticPlantFishingModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    /** Where the plants of a biome come from (M22.4: jungle and river apart, the mangrove swamp apart from the swamp). */
    public enum Water { JUNGLE, RIVER, SWAMP, MANGROVE }

    @Nullable
    public static Water water(Holder<Biome> biome) {
        if (biome.is(Biomes.MANGROVE_SWAMP)) return Water.MANGROVE;
        if (biome.is(ModTags.BETTA_SPAWN_BIOMES)) return Water.SWAMP;
        if (biome.is(BiomeTags.IS_JUNGLE)) return Water.JUNGLE;
        if (biome.is(BiomeTags.IS_RIVER)) return Water.RIVER;
        return null;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        if (origin == null) return loot;
        Water w = water(context.getLevel().getBiome(BlockPos.containing(origin)));
        if (w == null) return loot;
        for (ItemStack s : loot) if (s.getItem() instanceof org.overengineer.bettacraft.tank.plants.FloatingPlantItem) return loot;
        double chance = ServerConfig.getOr(ServerConfig.AQUATIC_FISHING_CHANCE, 0.05);
        if (context.getRandom().nextDouble() >= chance) return loot;
        var r = context.getRandom();
        PlantSpecies p = pick(w, r::nextLong);
        if (p == null) return loot;
        ItemStack plant = InteriorPlants.item(p, 0);
        if (plant.isEmpty()) return loot;
        loot.clear();
        loot.add(plant);
        return loot;
    }

    /**
     * Which plant comes up (M22.2: from the tables of {@link org.overengineer.bettacraft.sources.WildTables}; M22.4: per water,
     * from the server config lists {@code interiorPlants.fishingPlants*}). Null when the list is empty.
     */
    @Nullable
    public static PlantSpecies pick(Water w, RandomGenerator rnd) {
        var t = table(w);
        return t.isEmpty() ? null : t.pick(rnd);
    }

    public static org.overengineer.bettacraft.sources.WeightedTable<PlantSpecies> table(Water w) {
        return org.overengineer.bettacraft.sources.PlantTables.fishing(w);
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return ModCodecs.FISHING_AQUATIC_PLANTS.get();
    }
}
