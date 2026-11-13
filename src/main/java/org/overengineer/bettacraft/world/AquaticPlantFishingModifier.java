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
 * "Blue" and "Brownie Red" rarely), in swamps and mangrove swamps Cryptocoryne, Ludwigia and Rotala. Applied to the
 * fishing loot table only (condition in the JSON); a catch that already became a floating plant (M12) stays one.
 */
public class AquaticPlantFishingModifier extends LootModifier {
    public static final MapCodec<AquaticPlantFishingModifier> CODEC = RecordCodecBuilder.mapCodec(i -> codecStart(i)
            .apply(i, AquaticPlantFishingModifier::new));

    public AquaticPlantFishingModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    /** Where the plants of a biome come from: jungle / river, or swamp. */
    public enum Water { TROPICAL, SWAMP }

    @Nullable
    public static Water water(Holder<Biome> biome) {
        if (biome.is(Biomes.MANGROVE_SWAMP) || biome.is(ModTags.BETTA_SPAWN_BIOMES)) return Water.SWAMP;
        if (biome.is(BiomeTags.IS_JUNGLE) || biome.is(BiomeTags.IS_RIVER)) return Water.TROPICAL;
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
        ItemStack plant = InteriorPlants.item(pick(w, r::nextLong), 0);
        if (plant.isEmpty()) return loot;
        loot.clear();
        loot.add(plant);
        return loot;
    }

    /**
     * Which plant comes up. Jungle / river: Java fern 30, Anubias 30, Bucephalandra "Green Wavy" 15, "Mini Needle" 13,
     * "Blue" 6, "Brownie Red" 6. Swamp: Cryptocoryne green 35, brown 25, Ludwigia 20, Rotala 20.
     */
    public static PlantSpecies pick(Water w, RandomGenerator rnd) {
        int roll = rnd.nextInt(100);
        if (w == Water.SWAMP) {
            if (roll < 35) return PlantSpecies.CRYPT_GREEN;
            if (roll < 60) return PlantSpecies.CRYPT_BROWN;
            if (roll < 80) return PlantSpecies.LUDWIGIA;
            return PlantSpecies.ROTALA;
        }
        if (roll < 30) return PlantSpecies.JAVA_FERN;
        if (roll < 60) return PlantSpecies.ANUBIAS_NANA;
        if (roll < 75) return PlantSpecies.BUCE_GREEN_WAVY;
        if (roll < 88) return PlantSpecies.BUCE_MINI_NEEDLE;
        if (roll < 94) return PlantSpecies.BUCE_BLUE;
        return PlantSpecies.BUCE_BROWNIE_RED;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return ModCodecs.FISHING_AQUATIC_PLANTS.get();
    }
}
