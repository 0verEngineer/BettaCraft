package org.overengineer.bettacraft.client;

import net.minecraft.client.Minecraft;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.overengineer.bettacraft.progression.ItemHints;

/** "Hold Shift for details" on Bettacraft items (M6); the details are the item's info text (see {@link ItemHints}). */
final class ItemHintTooltips {
    private ItemHintTooltips() {}

    static void onTooltip(net.minecraft.world.item.ItemStack stack, List<Component> tip) {
        String key = ItemHints.key(stack.getItem());
        if (key == null || !net.minecraft.locale.Language.getInstance().has(key)) return;
        if (Minecraft.getInstance().hasShiftDown()) {
            for (Component line : ItemHints.lines(I18n.get(key))) tip.add(line.copy().withStyle(ChatFormatting.GRAY));
        } else {
            tip.add(Component.translatable("tooltip.bettacraft.hold_shift",
                    Component.translatable("tooltip.bettacraft.shift").withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
