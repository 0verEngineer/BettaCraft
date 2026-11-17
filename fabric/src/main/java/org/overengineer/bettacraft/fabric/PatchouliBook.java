package org.overengineer.bettacraft.fabric;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.progression.GuideBook;

/**
 * M52: the handbook on Fabric: the Patchouli book {@code bettacraft:handbook}, converted from the GuideME pages at build
 * time (tools/guide). Patchouli is optional; without it the handbook item is not given (like without GuideME on NeoForge).
 */
final class PatchouliBook implements GuideBook.Backend {
    static final Identifier BOOK = BettaCraft.id("handbook");

    @Override
    public String modId() {
        return "patchouli";
    }

    @Override
    public ItemStack createBook(Identifier guideId) {
        return Api.book();
    }

    @Override
    public boolean isBook(ItemStack stack, Identifier guideId) {
        ItemStack book = Api.book();
        return !book.isEmpty() && ItemStack.isSameItemSameComponents(stack, book);
    }

    /** Only loaded with Patchouli. */
    static final class Api {
        static ItemStack book() {
            return vazkii.patchouli.api.PatchouliAPI.get().getBookStack(BOOK);
        }

        private Api() {}
    }
}
