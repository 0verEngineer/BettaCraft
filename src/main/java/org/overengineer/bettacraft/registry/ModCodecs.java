package org.overengineer.bettacraft.registry;

import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.world.AddItemLootModifier;
import org.overengineer.bettacraft.world.BettaSpawnsBiomeModifier;
import org.overengineer.bettacraft.world.FishBagRecipeCondition;

/** Data-driven serializers: biome modifier, recipe condition, global loot modifier. */
public final class ModCodecs {
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, BettaCraft.MOD_ID);
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, BettaCraft.MOD_ID);
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, BettaCraft.MOD_ID);

    public static final Supplier<MapCodec<BettaSpawnsBiomeModifier>> BETTA_SPAWNS = BIOME_MODIFIERS.register("betta_spawns", () -> BettaSpawnsBiomeModifier.CODEC);
    public static final Supplier<MapCodec<FishBagRecipeCondition>> CONFIG_CONDITION = CONDITIONS.register("config", () -> FishBagRecipeCondition.CODEC);
    public static final Supplier<MapCodec<AddItemLootModifier>> ADD_ITEM = LOOT_MODIFIERS.register("add_item", () -> AddItemLootModifier.CODEC);

    public static final Supplier<MapCodec<org.overengineer.bettacraft.world.ArtemiaSandLootModifier>> ARTEMIA_SAND =
            LOOT_MODIFIERS.register("artemia_sand", () -> org.overengineer.bettacraft.world.ArtemiaSandLootModifier.CODEC);

    /** M12: floating plants from fishing in swamps. */
    public static final Supplier<MapCodec<org.overengineer.bettacraft.world.FloatingPlantFishingModifier>> FISHING_PLANTS =
            LOOT_MODIFIERS.register("fishing_floating_plants", () -> org.overengineer.bettacraft.world.FloatingPlantFishingModifier.CODEC);

    /** M13: aquarium plants from fishing in jungles, rivers and swamps. */
    public static final Supplier<MapCodec<org.overengineer.bettacraft.world.AquaticPlantFishingModifier>> FISHING_AQUATIC_PLANTS =
            LOOT_MODIFIERS.register("fishing_aquatic_plants", () -> org.overengineer.bettacraft.world.AquaticPlantFishingModifier.CODEC);

    public static void register(IEventBus bus) {
        BIOME_MODIFIERS.register(bus);
        CONDITIONS.register(bus);
        LOOT_MODIFIERS.register(bus);
    }

    private ModCodecs() {}
}
