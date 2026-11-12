package org.overengineer.bettacraft.client;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.overengineer.bettacraft.progression.ItemHints;

/** "Hold Shift for details" on Bettacraft items (M6); the details are the item's info text (see {@link ItemHints}). */
final class ItemHintTooltips {
    private ItemHintTooltips() {}

    static void onTooltip(ItemTooltipEvent event) {
        String key = ItemHints.key(event.getItemStack().getItem());
        if (key == null || !I18n.exists(key)) return;
        List<Component> tip = event.getToolTip();
        if (Screen.hasShiftDown()) {
            for (Component line : ItemHints.lines(I18n.get(key))) tip.add(line.copy().withStyle(ChatFormatting.GRAY));
        } else {
            tip.add(Component.translatable("tooltip.bettacraft.hold_shift",
                    Component.translatable("tooltip.bettacraft.shift").withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
