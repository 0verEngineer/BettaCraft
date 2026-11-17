package org.overengineer.bettacraft.world;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.random.RandomGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tank.plants.TankPlants;

/**
 * M52: the loot changes of our global loot modifiers (the conditions of their JSON decide when): NeoForge's loot modifier
 * classes and Fabric's loot event both call these.
 */
public final class LootChanges {
    private LootChanges() {}

    /** M9: artemia cysts from sand of the right kind, with the server config's chance. */
    public static ObjectArrayList<ItemStack> artemiaSand(ObjectArrayList<ItemStack> loot, LootContext context, ItemStack item) {
        BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        if (state == null || !state.is(ModTags.ARTEMIA_SAND)) return loot;
        double chance = ServerConfig.getOr(ServerConfig.DROPS_ARTEMIA_FROM_SAND_CHANCE, ServerConfig.ARTEMIA_SAND_CHANCE);
        if (context.getRandom().nextDouble() < chance) loot.add(item.copy());
        return loot;
    }

    /** M12: floating plants from fishing in swamps and mangrove swamps. */
    public static ObjectArrayList<ItemStack> floatingPlants(ObjectArrayList<ItemStack> loot, LootContext context) {
        Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        if (origin == null) return loot;
        Holder<Biome> biome = context.getLevel().getBiome(BlockPos.containing(origin));
        boolean mangrove = BiomeWaters.mangrove(biome); // M25.1: biome tags, also biome mods
        if (!mangrove && !BiomeWaters.swampOrBettaBiome(biome)) return loot;
        double chance = ServerConfig.getOr(ServerConfig.PLANTS_FISHING_CHANCE, 0.06);
        if (context.getRandom().nextDouble() >= chance) return loot;
        var r = context.getRandom();
        ItemStack plant = TankPlants.item(pickFloating(mangrove, r::nextLong));
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
    public static FloatingPlantType pickFloating(boolean mangrove, RandomGenerator rnd) {
        return (mangrove ? org.overengineer.bettacraft.sources.WildTables.FISHING_FLOATING_MANGROVE
                : org.overengineer.bettacraft.sources.WildTables.FISHING_FLOATING_SWAMP).pick(rnd);
    }
}
