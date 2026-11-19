package org.overengineer.bettacraft.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.overengineer.bettacraft.registry.NeoForgeCodecs;

/** The global loot modifier of {@link AquaticPlantFishing} (M13; M52: the logic is in the common class). */
public class AquaticPlantFishingModifier extends LootModifier {
    public static final MapCodec<AquaticPlantFishingModifier> CODEC = RecordCodecBuilder.mapCodec(i -> codecStart(i)
            .apply(i, AquaticPlantFishingModifier::new));

    /** M52c (26.1): NeoForge's loot modifiers carry a priority. */
    public AquaticPlantFishingModifier(java.util.Optional<net.minecraft.core.Holder<LootItemCondition>> conditions) {
        this(conditions, net.neoforged.neoforge.common.loot.IGlobalLootModifier.DEFAULT_PRIORITY);
    }

    public AquaticPlantFishingModifier(java.util.Optional<net.minecraft.core.Holder<LootItemCondition>> conditions, int priority) {
        super(conditions, priority);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        return AquaticPlantFishing.apply(loot, context);
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return NeoForgeCodecs.FISHING_AQUATIC_PLANTS.get();
    }
}
