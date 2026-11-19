package org.overengineer.bettacraft.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.overengineer.bettacraft.registry.ModCodecs;
import org.overengineer.bettacraft.registry.NeoForgeCodecs;

/** Global loot modifier adding an item stack when its conditions match (mud in swamps -> mosquito larvae). */
public class AddItemLootModifier extends LootModifier {
    public static final MapCodec<AddItemLootModifier> CODEC = RecordCodecBuilder.mapCodec(i -> codecStart(i)
            .and(net.minecraft.world.item.ItemStackTemplate.CODEC.fieldOf("item").forGetter(m -> m.item))
            .apply(i, AddItemLootModifier::new));

    // M52c (26.1): a template (the item components are bound after the data is read)
    private final net.minecraft.world.item.ItemStackTemplate item;

    /** M52c (26.1): NeoForge's loot modifiers carry a priority. */
    public AddItemLootModifier(LootItemCondition[] conditions, net.minecraft.world.item.ItemStackTemplate item) {
        this(conditions, net.neoforged.neoforge.common.loot.IGlobalLootModifier.DEFAULT_PRIORITY, item);
    }

    public AddItemLootModifier(LootItemCondition[] conditions, int priority, net.minecraft.world.item.ItemStackTemplate item) {
        super(conditions, priority);
        this.item = item;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        loot.add(item.create());
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return NeoForgeCodecs.ADD_ITEM.get();
    }
}
