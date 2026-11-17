package org.overengineer.bettacraft.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.random.RandomGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModCodecs;
import org.overengineer.bettacraft.registry.NeoForgeCodecs;
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
        boolean mangrove = BiomeWaters.mangrove(biome); // M25.1: biome tags, also biome mods
        if (!mangrove && !BiomeWaters.swampOrBettaBiome(biome)) return loot;
        double chance = ServerConfig.getOr(ServerConfig.PLANTS_FISHING_CHANCE, 0.06);
        if (context.getRandom().nextDouble() >= chance) return loot;
        var r = context.getRandom();
        ItemStack plant = TankPlants.item(pick(mangrove, r::nextLong));
        if (plant.isEmpty()) return loot;
        loot.clear();
        loot.add(plant);
        return loot;
    }

    /**
     * Which plant comes up: duckweed 45, salvinia 30, frogbit 25; in a mangrove swamp 35 / 25 / 25 and 15 red root floater
     * (M22.2: the tables {@link org.overengineer.bettacraft.sources.WildTables#FISHING_FLOATING_SWAMP} and
     * {@link org.overengineer.bettacraft.sources.WildTables#FISHING_FLOATING_MANGROVE}).
     */
    public static FloatingPlantType pick(boolean mangrove, RandomGenerator rnd) {
        return (mangrove ? org.overengineer.bettacraft.sources.WildTables.FISHING_FLOATING_MANGROVE
                : org.overengineer.bettacraft.sources.WildTables.FISHING_FLOATING_SWAMP).pick(rnd);
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return NeoForgeCodecs.FISHING_PLANTS.get();
    }
}
