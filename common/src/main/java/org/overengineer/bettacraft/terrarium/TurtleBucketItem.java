package org.overengineer.bettacraft.terrarium;

import org.overengineer.bettacraft.util.SinkList;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluids;
import org.overengineer.bettacraft.containers.AnimalBucketItem;

/**
 * M31 part b: a water turtle in a bucket of water (musk turtle, slider, Reeves' turtle), like the fish buckets; the tooltip
 * names the variety and, for a grown one, the sex ({@link #describe}).
 */
public class TurtleBucketItem extends AnimalBucketItem {
    public TurtleBucketItem(EntityType<?> type, Properties properties) {
        super(type, Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        List<Component> lines = new SinkList<>(tooltipAdder);
        super.appendHoverText(stack, ctx, tooltipDisplay, tooltipAdder, flag);
        describe(stack, lines);
    }

    /** Variety, sex (grown) or "young", brumating: from the bucket data of a turtle. */
    public static void describe(ItemStack stack, List<Component> lines) {
        CompoundTag tag = stack.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains("terrarium")) return;
        CompoundTag t = tag.getCompoundOrEmpty("terrarium");
        HerpSpecies s = speciesOf(stack);
        if (s != null && s.varieties().size() > 1) lines.add(Component.translatable(s.varietyKey(t.getIntOr("variety", 0))).withStyle(ChatFormatting.GRAY));
        int age = t.getIntOr("age", 0);
        if (s != null && s.tadpoles() && age < 0 && 1.0 + age / (s.growDays() * 24000.0) < HerpEntity.METAMORPHOSIS) { // part c
            lines.add(Component.translatable("tooltip.bettacraft.herp.tadpole").withStyle(ChatFormatting.AQUA));
        } else if (age < 0) lines.add(Component.translatable("tooltip.bettacraft.turtle.young").withStyle(ChatFormatting.GRAY));
        else if (t.contains("female")) lines.add(Component.translatable(t.getBooleanOr("female", false) ? "tooltip.bettacraft.turtle.female"
                : "tooltip.bettacraft.turtle.male").withStyle(ChatFormatting.GRAY));
        if (t.getBooleanOr("brumating", false)) lines.add(Component.translatable("tooltip.bettacraft.turtle.brumating").withStyle(ChatFormatting.AQUA));
    }

    private static HerpSpecies speciesOf(ItemStack stack) {
        EntityType<?> type = org.overengineer.bettacraft.tank.TankInhabitants.containedType(stack);
        for (HerpSpecies s : HerpSpecies.ALL) if (org.overengineer.bettacraft.registry.ModEntities.herp(s) == type) return s;
        return null;
    }
}
