package org.overengineer.bettacraft.gametest.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;

/**
 * M42 integration suite: the group itself and the ids we name of other mods. With every integrated mod loaded, each id of
 * another mod in our data (foreign animal and plant profiles, our tags, the compat recipes) must exist in the version of
 * the pack: an id that a mod renamed or removed would otherwise just be skipped silently ({@code "required": false}).
 */
@PrefixGameTestTemplate(false)
public final class IntegrationGroupTests {
    private IntegrationGroupTests() {}

    /** Every mod of the integration group (run/*-mods of the groups plus run/integration-mods, and the aquarium mods). */
    static final List<String> GROUP = List.of(
            // -Ppower
            "mekanism", "powah", "fluxnetworks", "enderio", "pipez", "mekanismcovers",
            // -Paquatic
            "alexscaves", "cataclysm", "crittersandcompanions", "deeperdarker", "livingthings", "shiny", "sushigocrafting",
            "undergarden", "iceandfire", "fisheggs",
            // -Pbiomes
            "regions_unexplored", "biomeswevegone", "twilightforest", "terrablender", "lithostitched",
            // -Pstorage, -Pcreate, -Pquests, -Pkubejs, -Pfacades
            "ae2", "refinedstorage", "create", "sable", "ftbquests", "kubejs", "cable_facades",
            // run/integration-mods
            "integrateddynamics", "integratedtunnels", "xnet", "laserio", "modularrouters", "industrialforegoing",
            // the aquarium mods and GuideME of the normal dev runtime
            "betterfishtanks", "fintastic", "fishofthieves", "aquaculture", "naturalist", "guideme");

    @GameTest(template = IntegrationSuite.EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_group")
    public static void everyIntegratedModIsLoaded(GameTestHelper h) {
        List<String> missing = GROUP.stream().filter(m -> !org.overengineer.bettacraft.platform.Platform.get().isModLoaded(m)).toList();
        if (!missing.isEmpty() && IntegrationSuite.PARTIAL) IntegrationSuite.LOGGER.warn("[integration] partial run without {}", missing);
        else if (!missing.isEmpty()) throw new GameTestAssertException("the integration group lacks " + missing);
        h.succeed();
    }

    /** Ids of loaded mods that our data names but the registries do not know. */
    @GameTest(template = IntegrationSuite.EMPTY, templateNamespace = BettaCraft.MOD_ID, batch = "bettacraft_integration_group")
    public static void everyForeignIdOfOurDataExists(GameTestHelper h) throws Exception {
        MinecraftServer server = h.getLevel().getServer();
        ResourceManager rm = server.getResourceManager();
        TreeSet<String> bad = new TreeSet<>();
        int[] checked = {0};
        // the profiles of animals and plants of other mods
        for (Map.Entry<ResourceLocation, Resource> e : rm.listResources("bettacraft/foreign_animals", p -> p.getPath().endsWith(".json")).entrySet()) {
            JsonObject o = read(e.getValue());
            String where = e.getKey().toString();
            for (String id : strings(o.get("entity"))) check(BuiltInRegistries.ENTITY_TYPE, id, where, bad, checked);
            for (String id : strings(o.get("food"))) {
                if (id.startsWith("#") || !id.contains(":")) continue; // tags and food groups (live, normal ...)
                check(BuiltInRegistries.ITEM, id, where, bad, checked);
            }
        }
        for (Map.Entry<ResourceLocation, Resource> e : rm.listResources("bettacraft/foreign_plants", p -> p.getPath().endsWith(".json")).entrySet()) {
            JsonObject o = read(e.getValue());
            for (String id : strings(o.get("blocks"))) {
                if (!id.startsWith("#")) check(BuiltInRegistries.BLOCK, id, e.getKey().toString(), bad, checked);
            }
        }
        // our tag files (of every namespace) that name ids of other mods
        Registry<?> biomes = server.registryAccess().registryOrThrow(Registries.BIOME);
        for (Map.Entry<ResourceLocation, List<Resource>> e : rm.listResourceStacks("tags", p -> p.getPath().endsWith(".json")).entrySet()) {
            String path = e.getKey().getPath();
            Registry<?> reg = path.startsWith("tags/item/") ? BuiltInRegistries.ITEM
                    : path.startsWith("tags/block/") ? BuiltInRegistries.BLOCK
                    : path.startsWith("tags/entity_type/") ? BuiltInRegistries.ENTITY_TYPE
                    : path.startsWith("tags/fluid/") ? BuiltInRegistries.FLUID
                    : path.startsWith("tags/worldgen/biome/") ? biomes : null;
            if (reg == null) continue;
            for (Resource r : e.getValue()) {
                if (!r.sourcePackId().contains(BettaCraft.MOD_ID)) continue; // only our own files
                JsonObject o = read(r);
                if (!o.has("values")) continue;
                for (JsonElement v : o.getAsJsonArray("values")) {
                    String id = v.isJsonObject() ? v.getAsJsonObject().get("id").getAsString() : v.getAsString();
                    if (id.startsWith("#")) continue;
                    check(reg, id, e.getKey().toString(), bad, checked);
                }
            }
        }
        // the compat recipes load when their mod is there
        List<String> recipes = new ArrayList<>();
        for (String[] r : new String[][]{{"create", "compat/create_drive_adapter"}, {"create", "compat/create_stainless_steel_ingot"},
                {"enderio", "compat/enderio_stainless_steel_ingot"}, {"mekanism", "compat/mekanism_stainless_steel_ingot"}}) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(BettaCraft.MOD_ID, r[1]);
            if (org.overengineer.bettacraft.platform.Platform.get().isModLoaded(r[0]) && server.getRecipeManager().byKey(id).isEmpty()) recipes.add(id.toString());
        }
        if (!recipes.isEmpty()) bad.add("recipes not loaded: " + recipes);
        if (!bad.isEmpty()) throw new GameTestAssertException(bad.size() + " ids of loaded mods unknown: " + String.join("; ", bad));
        h.assertTrue(checked[0] > 300, "checked only " + checked[0] + " foreign ids");
        IntegrationSuite.LOGGER.info("[integration] {} ids of other mods checked", checked[0]);
        h.succeed();
    }

    static JsonObject read(Resource r) throws Exception {
        try (Reader in = r.openAsReader()) {
            return JsonParser.parseReader(in).getAsJsonObject();
        }
    }

    static List<String> strings(JsonElement e) {
        List<String> out = new ArrayList<>();
        if (e == null) return out;
        if (e.isJsonArray()) {
            for (JsonElement x : (JsonArray) e) if (x.isJsonPrimitive()) out.add(x.getAsString());
        } else if (e.isJsonPrimitive()) out.add(e.getAsString());
        return out;
    }

    /** Checks one id: only ids of loaded mods (the namespace is the mod id) and vanilla count, not ours. */
    private static void check(Registry<?> reg, String id, String where, TreeSet<String> bad, int[] checked) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) {
            bad.add(where + ": malformed " + id);
            return;
        }
        String ns = rl.getNamespace();
        if (ns.equals(BettaCraft.MOD_ID) || !(ns.equals("minecraft") || org.overengineer.bettacraft.platform.Platform.get().isModLoaded(ns))) return;
        checked[0]++;
        if (!reg.containsKey(rl)) bad.add(where + ": " + id);
    }
}
