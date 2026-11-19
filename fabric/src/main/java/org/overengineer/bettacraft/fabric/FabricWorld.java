package org.overengineer.bettacraft.fabric;

import org.overengineer.bettacraft.util.NbtCompat;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.biome.v1.BiomeModification;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.api.registry.CompostableRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.world.WildSpawns;

/**
 * M52: what NeoForge reads from data and Fabric does in code: the spawns and features of the biome modifiers
 * ({@code data/bettacraft/neoforge/biome_modifier}), the global loot modifiers ({@code data/bettacraft/loot_modifiers},
 * read from the same JSON files: the same conditions and items), the compost chance of NeoForge's data map.
 */
public final class FabricWorld {
    private FabricWorld() {}

    static void register() {
        biomes();
        loot();
        // M49: the Chaetomorpha goes into the composter (NeoForge: data/neoforge/data_maps/item/compostables.json)
        CompostableRegistry.INSTANCE.add(org.overengineer.bettacraft.registry.ModItems.CHAETOMORPHA.get(), 0.65F);
    }

    // ------------------------------------------------------------------ biomes

    private static TagKey<Biome> tag(String id) {
        return TagKey.create(Registries.BIOME, Identifier.parse(id));
    }

    private static void biomes() {
        BiomeModification m = BiomeModifications.create(BettaCraft.id("spawns"));
        m.add(ModificationPhase.ADDITIONS, BiomeSelectors.tag(tag("bettacraft:betta_spawn_biomes")), (sel, ctx) -> {
            for (WildSpawns.Spawn s : WildSpawns.bettas()) ctx.getMobSpawnSettings().addSpawn(s.category(), s.data(), s.weight());
        });
        m.add(ModificationPhase.ADDITIONS, BiomeSelectors.all(), (sel, ctx) -> {
            for (WildSpawns.Spawn s : WildSpawns.wildlife(sel.getBiomeHolder())) ctx.getMobSpawnSettings().addSpawn(s.category(), s.data(), s.weight());
        });
        // the wild corals: #bettacraft:coral_reef_biomes or (#c:is_ocean and #c:is_hot)
        BiomeModifications.addFeature(BiomeSelectors.tag(tag("bettacraft:coral_reef_biomes"))
                        .or(BiomeSelectors.tag(tag("c:is_ocean")).and(BiomeSelectors.tag(tag("c:is_hot")))),
                GenerationStep.Decoration.VEGETAL_DECORATION, placed("wild_coral"));
        // the wild plants of the waters and the lush cave pools
        var plants = BiomeSelectors.tag(tag("bettacraft:jungle_waters")).or(BiomeSelectors.tag(tag("bettacraft:swamp_waters")))
                .or(BiomeSelectors.tag(tag("bettacraft:river_waters"))).or(BiomeSelectors.tag(tag("bettacraft:mangrove_waters")))
                .or(BiomeSelectors.tag(tag("bettacraft:cold_waters"))).or(BiomeSelectors.tag(tag("bettacraft:lush_cave_waters")))
                .or(BiomeSelectors.tag(tag("c:is_lush")).and(BiomeSelectors.tag(tag("c:is_cave"))))
                .or(BiomeSelectors.tag(tag("c:is_cold/overworld")));
        BiomeModifications.addFeature(plants, GenerationStep.Decoration.VEGETAL_DECORATION, placed("wild_plants"));
        BiomeModifications.addFeature(plants, GenerationStep.Decoration.VEGETAL_DECORATION, placed("wild_cave_plants"));
    }

    private static ResourceKey<PlacedFeature> placed(String path) {
        return ResourceKey.create(Registries.PLACED_FEATURE, BettaCraft.id(path));
    }

    // ------------------------------------------------------------------ loot

    /** One global loot modifier of our data: the loot table it is limited to (or null), its conditions, what it does. */
    record Modifier(String type, Identifier table, List<LootItemCondition> conditions, net.minecraft.world.item.@org.jetbrains.annotations.Nullable ItemStackTemplate item) {}

    static volatile List<Modifier> modifiers = List.of();

    private static void loot() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return BettaCraft.id("loot_modifiers");
            }

            @Override
            public void onResourceManagerReload(ResourceManager rm) {
                modifiers = read(rm);
            }
        });
    }

    /** The loot of a table was rolled (LootTableMixin; the table's id is its random sequence, as vanilla's tables have it). */
    public static void modify(Identifier id, LootContext context, ObjectArrayList<ItemStack> loot) {
        List<Modifier> ms = modifiers;
        if (ms.isEmpty()) return;
        for (Modifier m : ms) {
            if (m.table() != null && !m.table().equals(id)) continue;
            if (!m.conditions().stream().allMatch(c -> c.test(context))) continue;
            apply(m, loot, context);
        }
    }

    private static void apply(Modifier m, ObjectArrayList<ItemStack> loot, LootContext context) {
        switch (m.type()) {
            case "bettacraft:add_item" -> { if (m.item() != null) loot.add(m.item().create()); }
            case "bettacraft:artemia_sand" -> { if (m.item() != null) org.overengineer.bettacraft.world.LootChanges.artemiaSand(loot, context, m.item().create()); }
            case "bettacraft:fishing_floating_plants" -> org.overengineer.bettacraft.world.LootChanges.floatingPlants(loot, context);
            case "bettacraft:fishing_aquatic_plants" -> org.overengineer.bettacraft.world.AquaticPlantFishing.apply(loot, context);
            default -> {
            }
        }
    }

    /**
     * M52c (26.1): every modifier file of ours ({@code data/bettacraft/loot_modifiers/*.json}; NeoForge 26.1 has no list any
     * more), in the order of their priority (higher first, as NeoForge applies them).
     */
    private static List<Modifier> read(ResourceManager rm) {
        List<Modifier> out = new ArrayList<>();
        List<Integer> priorities = new ArrayList<>();
        var ops = RegistryOps.create(JsonOps.INSTANCE, net.minecraft.core.RegistryAccess.EMPTY);
        var files = new java.util.TreeMap<>(rm.listResources("loot_modifiers", id -> id.getNamespace().equals(BettaCraft.MOD_ID) && id.getPath().endsWith(".json")));
        for (var res : files.values()) {
            try (Reader mr = res.openAsReader()) {
                JsonObject o = JsonParser.parseReader(mr).getAsJsonObject();
                Identifier table = null;
                List<LootItemCondition> conds = new ArrayList<>();
                for (JsonElement c : o.getAsJsonArray("conditions")) {
                    JsonObject co = c.getAsJsonObject();
                    if ("neoforge:loot_table_id".equals(co.get("condition").getAsString())) {
                        table = Identifier.parse(co.get("loot_table_id").getAsString());
                        continue;
                    }
                    LootItemCondition.DIRECT_CODEC.parse(ops, co).result().ifPresent(conds::add);
                }
                // a template: the item components are bound after the data is read
                var item = o.has("item") ? net.minecraft.world.item.ItemStackTemplate.CODEC.parse(ops, o.get("item")).result().orElse(null) : null;
                int prio = o.has("priority") ? o.get("priority").getAsInt() : 1000;
                int at = 0;
                while (at < priorities.size() && priorities.get(at) >= prio) at++;
                priorities.add(at, prio);
                out.add(at, new Modifier(o.get("type").getAsString(), table, conds, item));
            } catch (Exception ex) {
                com.mojang.logging.LogUtils.getLogger().error("Bettacraft: reading the loot modifier {} failed", res.sourcePackId(), ex);
            }
        }
        return List.copyOf(out);
    }

    static Optional<Modifier> byType(String type) {
        return modifiers.stream().filter(m -> m.type().equals(type)).findFirst();
    }
}
