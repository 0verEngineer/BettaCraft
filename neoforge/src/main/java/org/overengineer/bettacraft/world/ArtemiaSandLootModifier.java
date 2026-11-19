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
import org.overengineer.bettacraft.registry.NeoForgeCodecs;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * Mining a block of {@code #bettacraft:artemia_sand} (sand) drops artemia cysts with the chance of the server config
 * {@code drops.artemiaFromSandChance} (PLAN 6.3, OFFEN H-16b; M35.1: 8 % instead of 2 %, one jar is one culture).
 */
public class ArtemiaSandLootModifier extends LootModifier {
    public static final MapCodec<ArtemiaSandLootModifier> CODEC = RecordCodecBuilder.mapCodec(i -> codecStart(i)
            .and(net.minecraft.world.item.ItemStackTemplate.CODEC.fieldOf("item").forGetter(m -> m.item))
            .apply(i, ArtemiaSandLootModifier::new));

    // M52c (26.1): a template (the item components are bound after the data is read)
    private final net.minecraft.world.item.ItemStackTemplate item;

    /** M52c (26.1): NeoForge's loot modifiers carry a priority. */
    public ArtemiaSandLootModifier(LootItemCondition[] conditions, net.minecraft.world.item.ItemStackTemplate item) {
        this(conditions, net.neoforged.neoforge.common.loot.IGlobalLootModifier.DEFAULT_PRIORITY, item);
    }

    public ArtemiaSandLootModifier(LootItemCondition[] conditions, int priority, net.minecraft.world.item.ItemStackTemplate item) {
        super(conditions, priority);
        this.item = item;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        return LootChanges.artemiaSand(loot, context, item.create());
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return NeoForgeCodecs.ARTEMIA_SAND.get();
    }
}
