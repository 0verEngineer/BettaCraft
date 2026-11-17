package org.overengineer.bettacraft.client;

import org.overengineer.bettacraft.util.NbtCompat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * M29.1: every block of the mod shows as a 3D block item (inventory, hotbar, JEI, handbook), like vanilla blocks. Its
 * item model has a block model as parent (or its own elements under {@code minecraft:block/block}) and display
 * transforms somewhere in the chain, so the GUI shows the three-quarter view and not a flat front. Flat icons are only
 * allowed for plants and crossed models (like vanilla saplings and seagrass). The devices that are no block item but
 * place a device in the tank (heaters, coolers, internal filters, the flow pump) are 3D too.
 */
class BlockItemModelTest {
    /** Items that place a device block but are no block item. */
    private static final Set<String> DEVICES = Set.of("heater_small", "heater_medium", "heater_large", "cooler_small", "cooler_medium",
            "cooler_large", "internal_filter", "sponge_filter", "corner_filter", "flow_pump");
    /** Vanilla parents that bring the block display transforms. */
    private static final Set<String> VANILLA_BLOCK_PARENTS = Set.of("minecraft:block/block", "minecraft:block/cube", "minecraft:block/cube_all",
            "minecraft:block/cube_column", "minecraft:block/orientable", "minecraft:block/cube_bottom_top");

    private static Path assets() {
        return Path.of(System.getProperty("bettacraft.projectDir", "."), "src");
    }

    private static JsonObject model(String path) throws IOException {
        for (String set : new String[]{"generated", "main"}) {
            Path p = assets().resolve(set + "/resources/assets/bettacraft/models/" + path + ".json");
            if (Files.exists(p)) return JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        return null;
    }

    private static String parent(JsonObject m) {
        return m != null && m.has("parent") ? m.get("parent").getAsString() : "";
    }

    /** Whether the GUI view of the model has display transforms (its own or inherited from a block parent). */
    private static boolean hasDisplay(JsonObject m) throws IOException {
        for (int i = 0; m != null && i < 8; i++) {
            if (m.has("display") && m.getAsJsonObject("display").has("gui")) return true;
            String p = parent(m);
            if (VANILLA_BLOCK_PARENTS.contains(p)) return true;
            if (!p.startsWith("bettacraft:")) return false;
            m = model(p.substring("bettacraft:".length()));
        }
        return false;
    }

    /** The model of the block's first variant or multipart piece. */
    private static JsonObject blockModel(String block) throws IOException {
        Path bs = assets().resolve("generated/resources/assets/bettacraft/blockstates/" + block + ".json");
        JsonObject s = JsonParser.parseString(Files.readString(bs, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject first = s.has("variants")
                ? s.getAsJsonObject("variants").entrySet().iterator().next().getValue().isJsonArray()
                        ? s.getAsJsonObject("variants").entrySet().iterator().next().getValue().getAsJsonArray().get(0).getAsJsonObject()
                        : s.getAsJsonObject("variants").entrySet().iterator().next().getValue().getAsJsonObject()
                : s.getAsJsonArray("multipart").get(0).getAsJsonObject().getAsJsonObject("apply");
        String m = first.get("model").getAsString();
        return m.startsWith("bettacraft:") ? model(m.substring("bettacraft:".length())) : null;
    }

    @Test
    void everyBlockItemIs3dOrAnAllowedFlatIcon() throws IOException {
        List<String> names = new ArrayList<>();
        try (Stream<Path> s = Files.list(assets().resolve("generated/resources/assets/bettacraft/blockstates"))) {
            s.forEach(p -> names.add(p.getFileName().toString().replace(".json", "")));
        }
        names.addAll(DEVICES);
        List<String> bad = new ArrayList<>();
        int checked = 0, flat = 0;
        for (String n : names) {
            JsonObject item = model("item/" + n);
            if (item == null) continue; // a block without an item (segments, the interior shell, wild plants)
            checked++;
            String p = parent(item);
            boolean block3d = p.startsWith("bettacraft:block/") || p.equals("minecraft:block/block") && item.has("elements");
            if (block3d) {
                if (!hasDisplay(item)) bad.add(n + ": 3D but no display transforms (flat front view in the GUI)");
                continue;
            }
            // allowed flat: the aquarium plants and every crossed model (like vanilla saplings and seagrass)
            String layer0 = item.has("textures") && item.getAsJsonObject("textures").has("layer0")
                    ? item.getAsJsonObject("textures").get("layer0").getAsString() : "";
            boolean plant = layer0.startsWith("bettacraft:item/plant_");
            boolean cross = !DEVICES.contains(n) && parent(blockModel(n)).equals("minecraft:block/cross");
            if (plant || cross) flat++;
            else bad.add(n + ": flat icon (" + p + ") but neither a plant nor a crossed model");
        }
        assertTrue(checked > 100, "found the block items: " + checked);
        assertTrue(flat > 0, "the plants keep their flat icons");
        assertTrue(bad.isEmpty(), "block items that are no 3D block:\n" + String.join("\n", bad));
    }
}
