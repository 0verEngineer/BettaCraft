package org.overengineer.bettacraft.datagen.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * M52b: the 1.21.4 item model definitions ({@code assets/<ns>/items/<id>.json}) of every item of the mod, from the item
 * models generated before (run after the model providers). A tinted item (1.21.1: an item colour) names our tint source
 * {@code bettacraft:tint} for each tint index; an item model with overrides (1.21.1: by an item property) becomes a
 * condition on {@code bettacraft:property}; the items listed in {@code special} use our item model type
 * {@code bettacraft:block_model} (tank, cabinet).
 */
public final class ItemDefinitionProvider implements DataProvider {
    /** Tint indices our tinted items use (the fish container overlays go up to 3). */
    private static final int TINTS = 4;
    private final PackOutput output;
    private final String modid;
    private final Set<Item> tinted;
    private final Map<String, String> special;

    public ItemDefinitionProvider(PackOutput output, String modid, Set<Item> tinted, Map<String, String> special) {
        this.output = output;
        this.modid = modid;
        this.tinted = tinted;
        this.special = special;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        PackOutput.PathProvider paths = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "items");
        List<CompletableFuture<?>> out = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals(modid)) continue;
            Identifier modelId = id.withPrefix("item/");
            JsonObject model;
            String kind = special.get(id.getPath());
            if (kind != null) {
                model = new JsonObject();
                model.addProperty("type", "bettacraft:block_model");
                model.addProperty("kind", kind);
                model.addProperty("model", modelId.toString());
            } else {
                ItemModelBuilder b = ItemModelProvider.ALL.get(modelId);
                if (b == null) {
                    missing.add(id.toString());
                    continue;
                }
                model = plain(modelId, tinted.contains(item));
                for (int i = 0; i < b.overrides.size(); i++) {
                    // 1.21.1 used the last override that matched: the last one becomes the outermost condition (checked first),
                    // the earlier ones its fallback (M55: several overrides by rising thresholds, the animal in a container)
                    ItemModelBuilder.OverrideBuilder o = b.overrides.get(i);
                    for (Map.Entry<Identifier, Float> p : o.predicates.entrySet()) {
                        JsonObject c = new JsonObject();
                        c.addProperty("type", "minecraft:condition");
                        c.addProperty("property", "bettacraft:property");
                        c.addProperty("name", p.getKey().toString());
                        c.addProperty("threshold", p.getValue());
                        c.add("on_true", plain(o.model.getLocation(), tinted.contains(item)));
                        c.add("on_false", model);
                        model = c;
                    }
                }
            }
            JsonObject root = new JsonObject();
            root.add("model", model);
            out.add(DataProvider.saveStable(cache, root, paths.json(id)));
        }
        if (!missing.isEmpty()) throw new IllegalStateException("Items without an item model: " + missing);
        return CompletableFuture.allOf(out.toArray(CompletableFuture[]::new));
    }

    private static JsonObject plain(Identifier model, boolean tinted) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "minecraft:model");
        o.addProperty("model", model.toString());
        if (tinted) {
            JsonArray t = new JsonArray();
            for (int i = 0; i < TINTS; i++) {
                JsonObject s = new JsonObject();
                s.addProperty("type", "bettacraft:tint");
                s.addProperty("index", i);
                t.add(s);
            }
            o.add("tints", t);
        }
        return o;
    }

    @Override
    public String getName() {
        return "Item Definitions: " + modid;
    }
}
