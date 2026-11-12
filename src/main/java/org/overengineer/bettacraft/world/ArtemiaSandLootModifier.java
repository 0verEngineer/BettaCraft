package org.overengineer.bettacraft.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModCodecs;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * Mining a block of {@code #bettacraft:artemia_sand} (sand) drops artemia cysts with the chance of the server config
 * {@code drops.artemiaFromSandChance} (PLAN 6.3, OFFEN H-16b).
 */
public class ArtemiaSandLootModifier extends LootModifier {
    public static final MapCodec<ArtemiaSandLootModifier> CODEC = RecordCodecBuilder.mapCodec(i -> codecStart(i)
            .and(ItemStack.CODEC.fieldOf("item").forGetter(m -> m.item))
            .apply(i, ArtemiaSandLootModifier::new));

    private final ItemStack item;

    public ArtemiaSandLootModifier(LootItemCondition[] conditions, ItemStack item) {
        super(conditions);
        this.item = item;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        if (state == null || !state.is(ModTags.ARTEMIA_SAND)) return loot;
        double chance = ServerConfig.getOr(ServerConfig.DROPS_ARTEMIA_FROM_SAND_CHANCE, 0.02);
        if (context.getRandom().nextDouble() < chance) loot.add(item.copy());
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return ModCodecs.ARTEMIA_SAND.get();
    }
}
