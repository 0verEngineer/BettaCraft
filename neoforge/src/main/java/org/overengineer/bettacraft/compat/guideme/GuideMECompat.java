package org.overengineer.bettacraft.compat.guideme;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The only class that touches the GuideME API. Callers must check {@code org.overengineer.bettacraft.platform.Platform.get().isModLoaded("guideme")} first
 * (see {@link org.overengineer.bettacraft.progression.GuideBook#available()}); without GuideME this class is never loaded.
 */
public final class GuideMECompat {
    private GuideMECompat() {}

    /** The {@code guideme:guide} item bound to our guide. */
    public static ItemStack createBook(Identifier guideId) {
        return guideme.Guides.createGuideItem(guideId);
    }

    public static boolean isBook(ItemStack stack, Identifier guideId) {
        // M17: GuideItem is GuideME internal; if a GuideME update moves it, fall back to "not our book" instead of crashing
        try {
            return stack.getItem() instanceof guideme.internal.item.GuideItem && guideId.equals(guideme.internal.item.GuideItem.getGuideId(stack));
        } catch (LinkageError e) {
            return false;
        }
    }

    /** Opens the guide (on the client directly, on the server via GuideME's packet). */
    public static void open(Player player, Identifier guideId, Identifier page) {
        guideme.GuidesCommon.openGuide(player, guideId, guideme.PageAnchor.page(page));
    }
}
