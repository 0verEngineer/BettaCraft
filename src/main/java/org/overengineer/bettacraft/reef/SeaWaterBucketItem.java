package org.overengineer.bettacraft.reef;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.material.Fluids;

/**
 * M20: a bucket of sea water. In the world it pours plain water (Minecraft has one water); poured into a tank it brings
 * 35 ppt of salt and the calcium of sea water. Draining a salt water tank fills an empty bucket with it.
 */
public class SeaWaterBucketItem extends BucketItem {
    public SeaWaterBucketItem(Properties properties) {
        super(Fluids.WATER, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.bettacraft.sea_water_bucket").withStyle(ChatFormatting.GRAY));
    }
}
