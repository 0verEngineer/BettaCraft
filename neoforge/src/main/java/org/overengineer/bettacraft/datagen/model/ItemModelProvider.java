package org.overengineer.bettacraft.datagen.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * M52b: the item models of a mod ({@code assets/<ns>/models/item}). Every item model made by any of these providers is
 * also noted for {@link ItemDefinitionProvider}, which writes the 1.21.4 item model definitions after them.
 */
public abstract class ItemModelProvider extends ModelProvider<ItemModelBuilder> {
    /** Every item model generated in this run, by id ({@code ns:item/path}). */
    static final Map<Identifier, ItemModelBuilder> ALL = new LinkedHashMap<>();

    public ItemModelProvider(PackOutput output, String modid, ExistingFileHelper helper) {
        super(output, modid, ITEM_FOLDER, id -> new ItemModelBuilder(id, helper), helper);
    }

    @SuppressWarnings("unchecked")
    static void collect(ModelProvider<?> provider) {
        if (provider instanceof ItemModelProvider p) ALL.putAll(p.generatedModels);
    }

    public ItemModelBuilder basicItem(Item item) {
        return basicItem(BuiltInRegistries.ITEM.getKey(item));
    }

    public ItemModelBuilder basicItem(Identifier item) {
        return getBuilder(item.toString()).parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", Identifier.fromNamespaceAndPath(item.getNamespace(), "item/" + item.getPath()));
    }

    public ItemModelBuilder handheldItem(Item item) {
        return handheldItem(BuiltInRegistries.ITEM.getKey(item));
    }

    public ItemModelBuilder handheldItem(Identifier item) {
        return getBuilder(item.toString()).parent(new ModelFile.UncheckedModelFile("item/handheld"))
                .texture("layer0", Identifier.fromNamespaceAndPath(item.getNamespace(), "item/" + item.getPath()));
    }

    @Override
    public String getName() {
        return "Item Models: " + modid;
    }
}
