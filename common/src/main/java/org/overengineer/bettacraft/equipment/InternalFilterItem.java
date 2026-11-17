package org.overengineer.bettacraft.equipment;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Internal filter (PLAN 6.2 / 7.3): placed in the tank interior as a small box of {@link SegmentBlock}s with the outlet
 * facing the player. The miniature shows a water jet and bubbles at the outlet (M4); since M9 it cleans the water.
 * M23: the same item class places the corner filter and the air-driven sponge filter ({@code kind}).
 */
public class InternalFilterItem extends Item {
    private final SegmentBlock.Kind kind;

    public InternalFilterItem(Properties properties) {
        this(SegmentBlock.Kind.FILTER, properties);
    }

    public InternalFilterItem(SegmentBlock.Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return EquipmentPlacement.place(context, kind, SegmentBlock.Size.SMALL);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String key = switch (kind) {
            case SPONGE_FILTER -> "tooltip.bettacraft.sponge_filter.usage";
            case CORNER_FILTER -> "tooltip.bettacraft.corner_filter.usage";
            default -> "tooltip.bettacraft.internal_filter.usage";
        };
        tooltip.add(Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY));
    }
}
