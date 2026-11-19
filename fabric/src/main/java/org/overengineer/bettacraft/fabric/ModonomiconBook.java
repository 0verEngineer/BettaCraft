package org.overengineer.bettacraft.fabric;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.progression.GuideBook;

/**
 * M52b: the handbook on Fabric 1.21.11: the Modonomicon book {@code bettacraft:handbook}, converted from the GuideME pages
 * at build time (tools/guide). Patchouli has no 1.21.11 build. Modonomicon is optional; without it the handbook item is not
 * given (like without GuideME on NeoForge). The book is Modonomicon's book item with the component {@code modonomicon:book_id}
 * (its own way of giving a book), found by ids only: no compile dependency.
 */
final class ModonomiconBook implements GuideBook.Backend {
    static final Identifier BOOK = BettaCraft.id("handbook");
    private static final Identifier ITEM = Identifier.fromNamespaceAndPath("modonomicon", "modonomicon");
    private static final Identifier BOOK_ID = Identifier.fromNamespaceAndPath("modonomicon", "book_id");

    @Override
    public String modId() {
        return "modonomicon";
    }

    @Override
    @SuppressWarnings("unchecked")
    public ItemStack createBook(Identifier guideId) {
        var item = BuiltInRegistries.ITEM.getValue(ITEM);
        var type = (DataComponentType<Identifier>) BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(BOOK_ID);
        if (item == Items.AIR || type == null) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item);
        stack.set(type, BOOK);
        return stack;
    }

    @Override
    public boolean isBook(ItemStack stack, Identifier guideId) {
        var type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(BOOK_ID);
        return type != null && BOOK.equals(stack.get(type));
    }
}
