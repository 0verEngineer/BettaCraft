package org.overengineer.bettacraft.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.random.RandomGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
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
import org.overengineer.bettacraft.registry.ModCodecs;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tank.plants.TankPlants;

/**
 * Fishing in a swamp ({@code #bettacraft:betta_spawn_biomes}) or a mangrove swamp sometimes brings up a floating plant
 * instead of the catch, like junk (M12, chance in the server config {@code floatingPlants.fishingChance}). The red root
 * floater only comes up in mangrove swamps, and rarely. Applied to the fishing loot table only (condition in the JSON).
 */
public class FloatingPlantFishingModifier extends LootModifier {
    public static final MapCodec<FloatingPlantFishingModifier> CODEC = RecordCodecBuilder.mapCodec(i -> codecStart(i)
            .apply(i, FloatingPlantFishingModifier::new));

    public FloatingPlantFishingModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        if (origin == null) return loot;
        Holder<Biome> biome = context.getLevel().getBiome(BlockPos.containing(origin));
        boolean mangrove = biome.is(Biomes.MANGROVE_SWAMP);
        if (!mangrove && !biome.is(ModTags.BETTA_SPAWN_BIOMES)) return loot;
        double chance = ServerConfig.getOr(ServerConfig.PLANTS_FISHING_CHANCE, 0.06);
        if (context.getRandom().nextDouble() >= chance) return loot;
        var r = context.getRandom();
        ItemStack plant = TankPlants.item(pick(mangrove, r::nextLong));
        if (plant.isEmpty()) return loot;
        loot.clear();
        loot.add(plant);
        return loot;
    }

    /** Which plant comes up: duckweed 45, salvinia 30, frogbit 25; in a mangrove swamp 35 / 25 / 25 and 15 red root floater. */
    public static FloatingPlantType pick(boolean mangrove, RandomGenerator rnd) {
        int roll = rnd.nextInt(100);
        if (mangrove) {
            if (roll < 35) return FloatingPlantType.DUCKWEED;
            if (roll < 60) return FloatingPlantType.SALVINIA;
            if (roll < 85) return FloatingPlantType.FROGBIT;
            return FloatingPlantType.RED_ROOT_FLOATER;
        }
        if (roll < 45) return FloatingPlantType.DUCKWEED;
        if (roll < 75) return FloatingPlantType.SALVINIA;
        return FloatingPlantType.FROGBIT;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return ModCodecs.FISHING_PLANTS.get();
    }
}
