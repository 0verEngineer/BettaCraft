package org.overengineer.bettacraft.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Type names, the variety rules and the data-driven genetics (no Minecraft bootstrap). */
class BettaTypesTest {
    @AfterEach
    void reset() {
        GeneticsData.reset();
    }

    private static String key(Component c) {
        return ((TranslatableContents) c.getContents()).getKey();
    }

    private static Object arg(Component c, int i) {
        return ((TranslatableContents) c.getContents()).getArgs()[i];
    }

    private static BettaGenome g(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return BettaGenome.of(m, new BettaGenome(BettaGenome.SPECIES_BETTA, "halfmoon", "long", "normal", "red", "red", "none", "black",
                "none", "steel"));
    }

    @Test
    void everyVarietyNamesItsOwnExample() {
        for (GeneticsData.VarietyDef v : GeneticsData.get().varieties()) {
            BettaGenome ex = BettaGenome.of(v.example(), BettaGenome.DEFAULT);
            assertEquals(v.id(), BettaTypes.variety(ex).id(), "example of " + v.id());
        }
        assertEquals(BettaVarietyTable.VARIETIES.size(), GeneticsData.get().varieties().size());
    }

    @Test
    void wildTypeIsNamedAlone() {
        GeneticsData.VarietyDef v = BettaTypes.variety(BettaGenome.DEFAULT);
        assertNotNull(v);
        assertEquals("wild", v.id());
        assertEquals("variety.bettacraft.wild", key(BettaTypes.name(BettaGenome.DEFAULT)), "no form for the wild type");
    }

    @Test
    void formNames() {
        assertEquals("shape.bettacraft.halfmoon", key(BettaTypes.form(g())));
        assertEquals("form.bettacraft.plakat", key(BettaTypes.form(g("finform", "round", "fin_length", "short"))));
        Component hmpk = BettaTypes.form(g("fin_length", "short"));
        assertEquals("form.bettacraft.short", key(hmpk));
        assertEquals("shape.bettacraft.halfmoon", key((Component) arg(hmpk, 0)));
        assertEquals("gui.bettacraft.dumbo", key(BettaTypes.form(g("pectoral", "dumbo"))));
    }

    @Test
    void namedAndGenericColours() {
        Component mg = BettaTypes.name(g("body_color", "navy", "fin_color", "mustard"));
        assertEquals("type.bettacraft.full", key(mg));
        assertEquals("variety.bettacraft.mustard_gas", key((Component) arg(mg, 1)));
        assertEquals("super_red", BettaTypes.variety(g()).id());
        // generic: solid, bicolour, pattern, metallic
        assertNull(BettaTypes.variety(g("body_color", "lavender", "fin_color", "lavender")));
        assertEquals("type.bettacraft.solid", key(BettaTypes.genericColors(g("body_color", "lavender", "fin_color", "lavender"))));
        assertEquals("type.bettacraft.bicolor", key(BettaTypes.genericColors(g("body_color", "lavender", "fin_color", "mint"))));
        assertEquals("type.bettacraft.patterned", key(BettaTypes.genericColors(g("pattern", "butterfly"))));
        assertEquals("type.bettacraft.metallic", key(BettaTypes.genericColors(g("iridescence", "high"))));
        // priority: koi + high iridescence is a Koi Galaxy, not a Nemo
        assertEquals("koi_galaxy", BettaTypes.variety(g("body_color", "orange", "pattern", "koi", "iridescence", "high")).id());
        assertEquals("nemo", BettaTypes.variety(g("body_color", "orange", "pattern", "koi")).id());
    }

    @Test
    void typeIsInheritedThroughTheGenotype() {
        // two Mustard Gas carriers of short fins: a quarter of the offspring are long-finned Mustard Gas
        BettaGenotype p = BettaGenotype.homozygous(g("body_color", "navy", "fin_color", "mustard"))
                .with(BettaLoci.FIN_LENGTH, new BettaGenotype.AllelePair("short", "long"));
        assertEquals("short", p.express().finLength(), "short fins are dominant");
        var square = BettaGenotype.predict(p, p).get(BettaLoci.FIN_LENGTH);
        assertEquals("short", square.get(0).allele());
        assertEquals(0.75, square.get(0).probability(), 1e-9);
        net.minecraft.util.RandomSource r = net.minecraft.util.RandomSource.create(5);
        int longFins = 0;
        for (int i = 0; i < 4000; i++) {
            BettaGenome child = BettaGenotype.cross(p, p, r, 0.0).express();
            assertEquals("mustard_gas", BettaTypes.variety(child).id());
            if (!child.isShortFinned()) longFins++;
        }
        assertEquals(0.25, longFins / 4000.0, 0.03);
    }

    @Test
    void datapackJsonMatchesTheDefaultsAndCanOverride() {
        GeneticsData def = GeneticsData.defaults();
        Map<String, JsonElement> colors = new LinkedHashMap<>(), genes = new LinkedHashMap<>(), varieties = new LinkedHashMap<>();
        def.colors().values().forEach(c -> colors.put(c.id(), GeneticsData.colorJson(c)));
        GeneticsData.DEFAULT_GENES.forEach((k, l) -> genes.put(k, GeneticsData.genesJson(l)));
        def.varieties().forEach(v -> varieties.put(v.id(), GeneticsData.varietyJson(v)));
        GeneticsData fromJson = GeneticsData.fromJson(colors, genes, varieties, w -> { throw new AssertionError(w); });
        assertEquals(def.toNbt(), fromJson.toNbt(), "datagen JSON reproduces the built-in data");
        assertEquals(def.toNbt(), GeneticsData.fromNbt(def.toNbt()).toNbt(), "sync round trip");

        // a datapack adds a colour, makes doubletail dominant and adds a variety for it
        colors.put("mypack:glow", JsonParser.parseString("{\"color\":\"#00ff88\",\"iridescent\":true,\"name\":\"Glow\"}"));
        genes.put("mypack:finform", JsonParser.parseString("{\"alleles\":{\"doubletail\":{\"rank\":99,\"weight\":1}}}"));
        varieties.put("mypack:glowtail", JsonParser.parseString("{\"priority\":200,\"when\":{\"body_color\":[\"mypack:glow\"]}}"));
        GeneticsData.set(GeneticsData.fromJson(colors, genes, varieties, w -> { throw new AssertionError(w); }));
        assertEquals(0x00FF88, BettaColors.rgb("mypack:glow"));
        assertTrue(BettaColors.iridescentIds().contains("mypack:glow"));
        assertEquals("color.mypack.glow", BettaColors.translationKey("mypack:glow"));
        assertEquals("doubletail", BettaLoci.FINFORM.express("round", "doubletail"));
        BettaGenome glow = g("body_color", "mypack:glow");
        assertEquals("mypack:glowtail", BettaTypes.variety(glow).id());
        assertEquals("variety.mypack.glowtail", key(BettaTypes.varietyName(BettaTypes.variety(glow))));
        // broken files are reported and skipped, the rest still loads
        Map<String, JsonElement> bad = new LinkedHashMap<>(colors);
        bad.put("broken", JsonParser.parseString("{\"color\":\"not a colour\"}"));
        int[] warnings = {0};
        GeneticsData d = GeneticsData.fromJson(bad, genes, varieties, w -> warnings[0]++);
        assertEquals(1, warnings[0]);
        assertTrue(d.isColor("red"));
    }

    @Test
    void shapesMatchTheModel() {
        assertEquals(BettaGenome.SHAPES, GeneticsData.DEFAULT_GENES.get("finform").stream().map(GeneticsData.AlleleDef::id).toList(),
                "every built-in tail shape has a model and vice versa");
        assertEquals(BettaGenome.SHAPES.size(), org.overengineer.bettacraft.client.model.BettaModelDefinitions.Shape.values().length);
        for (String p : org.overengineer.bettacraft.client.model.BettaModelDefinitions.PATTERNS) {
            assertTrue(BettaPatterns.isKnown(p), "pattern mask without gene: " + p);
        }
        for (String p : BettaPatterns.ids()) {
            assertTrue(java.util.Arrays.asList(org.overengineer.bettacraft.client.model.BettaModelDefinitions.PATTERNS).contains(p),
                    "pattern gene without mask: " + p);
        }
    }
}
