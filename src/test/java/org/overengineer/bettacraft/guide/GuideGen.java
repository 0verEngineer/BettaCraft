package org.overengineer.bettacraft.guide;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.reef.SaltTolerance;
import org.overengineer.bettacraft.sources.Source;
import org.overengineer.bettacraft.sources.Sources;
import org.overengineer.bettacraft.sources.Target;
import org.overengineer.bettacraft.sources.Where;
import org.overengineer.bettacraft.tank.Salinity;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M22.2: writes the parts of the handbook that come from the game data, in English and German: the "Profile" table
 * and the "Obtaining" section of every species page, the species tables of the overview pages, the page "Where to find
 * what" and the advancements. A generated section starts at its heading ({@code ## Obtaining} / {@code ## Bezug}, ...) and
 * runs to the next {@code ## } heading or the end of the page; everything else of a page is written by hand.
 * <p>
 * {@code gradlew updateGuide} writes the sections ({@link GuideGenTest} with {@code -Dbettacraft.updateGuide=true});
 * {@code gradlew test} fails when the book and the data differ. The catalogue of pages ({@link #page}) is the place to
 * add the page of a new species.
 */
public final class GuideGen {
    public static Path project() {
        return Path.of(System.getProperty("bettacraft.projectDir", "."));
    }

    public static Path guide() {
        return project().resolve("src/main/resources/assets/bettacraft/guides/bettacraft/guide");
    }

    private final boolean de;
    private final Map<String, String> lang;
    private final Map<String, String> itemPages;
    private final Map<String, List<String>> recipesByResult;
    private final List<Source> sources = Sources.all();

    public GuideGen(boolean german) {
        this.de = german;
        this.lang = readLang(german);
        this.itemPages = itemPages(german);
        this.recipesByResult = recipes();
        this.crucible = machineRecipes("bettacraft:crucible");
        this.foodMaker = machineRecipes("bettacraft:food_maker");
    }

    /** M26 part b: result item id -> batches of the food maker. */
    private final Map<String, List<String>> foodMaker;

    /** M24: result item id -> melts of the crucible ("3 × ... + ... → 3"), read from the recipe files. */
    private final Map<String, List<String>> crucible;

    private static final Map<String, String> TAG_ITEMS = Map.of("c:ingots/iron", "minecraft:iron_ingot", "minecraft:coals", "minecraft:coal",
            "c:gems/quartz", "minecraft:quartz", "bettacraft:pellet_fish", "minecraft:cod", "bettacraft:food_vegetables", "minecraft:carrot",
            "bettacraft:freezing_ice", "minecraft:ice");

    /** Recipes of one of the mod's machines (crucible, food maker) by result, read from the recipe files. */
    private static Map<String, List<String>> machineRecipes(String type) {
        Map<String, List<String>> out = new TreeMap<>();
        Path d = project().resolve("src/generated/resources/data/bettacraft/recipe");
        if (!Files.isDirectory(d)) return out;
        try (Stream<Path> s = Files.walk(d)) {
            for (Path p : s.filter(f -> f.toString().endsWith(".json")).sorted().toList()) {
                JsonObject o = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
                if (!o.has("type") || !type.equals(o.get("type").getAsString())) continue;
                StringBuilder b = new StringBuilder();
                for (JsonElement e : o.getAsJsonArray("ingredients")) {
                    JsonObject i = e.getAsJsonObject();
                    String item = i.has("item") ? i.get("item").getAsString() : TAG_ITEMS.getOrDefault(i.get("tag").getAsString(), "minecraft:barrier");
                    if (!b.isEmpty()) b.append(" + ");
                    b.append(i.has("count") ? i.get("count").getAsInt() : 1).append(" × <ItemLink id=\"").append(item).append("\" />");
                }
                JsonObject r = o.getAsJsonObject("result");
                b.append(" → ").append(r.has("count") ? r.get("count").getAsInt() : 1);
                out.computeIfAbsent(r.get("id").getAsString(), k -> new ArrayList<>()).add(b.toString());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out;
    }

    // ================================================================== catalogue

    /** The handbook page of a {@link Target} key (path relative to the guide root), or null. */
    public String page(String target) {
        int c = target.indexOf(':');
        String kind = c < 0 ? target : target.substring(0, c), id = c < 0 ? "" : target.substring(c + 1);
        return switch (kind) {
            case "betta" -> id.isEmpty() ? "bettas.md" : "bettas/betta_" + id + ".md"; // M29 part c: the wild species
            case "frog" -> "invertebrates/dwarf_clawed_frog.md";
            case "tankmate" -> {
                TankmateSpecies s = TankmateSpecies.byId(id);
                // M30: the big animals have their own chapter
                yield (s.isLarge() ? "big_tanks/" : s.marine() ? "saltwater/" : "tankmates/") + s.id() + ".md";
            }
            case "critter" -> {
                CritterSpecies s = CritterSpecies.byId(id);
                yield (s.marine() ? "saltwater/" : "invertebrates/") + s.id() + ".md";
            }
            case "plant" -> {
                PlantSpecies p = PlantSpecies.byId(id);
                yield (p.marine() ? "saltwater/" : "plants/") + p.id() + ".md";
            }
            case "floating" -> "plants/" + id + ".md";
            case "item" -> itemPages.get("bettacraft:" + id);
            default -> null;
        };
    }

    /** Every animal and plant of the mod with the page it must have. */
    public static Map<String, String> speciesTargets() {
        Map<String, String> out = new LinkedHashMap<>();
        out.put(Target.BETTA, "Betta");
        for (var s : org.overengineer.bettacraft.genetics.BettaSpecies.values()) out.put(Target.betta(s), s.latin()); // M29 part c
        out.put(Target.FROG, "dwarf clawed frog");
        for (TankmateSpecies s : TankmateSpecies.values()) out.put(Target.tankmate(s), s.id());
        for (CritterSpecies s : CritterSpecies.values()) out.put(Target.critter(s), s.id());
        for (PlantSpecies p : PlantSpecies.values()) out.put(Target.plant(p), p.id());
        // M28: floating Riccia is the aquarium plant (its page)
        for (FloatingPlantType t : FloatingPlantType.values()) if (t.grows() && !t.plantItem()) out.put(Target.floating(t), t.id());
        return out;
    }

    /** Tankmate group pages (several species), null = the species hangs directly under its chapter. */
    public static String groupPage(TankmateSpecies s) {
        return switch (s.group()) {
            case TETRA -> "tankmates/tetras.md";
            case RASBORA -> "tankmates/rasboras.md";
            case CORY -> "tankmates/corydoras.md";
            case PLECO, OTOCINCLUS -> "tankmates/plecos.md";
            case LOACH -> "tankmates/loaches.md";
            case LIVEBEARER -> "tankmates/livebearers.md";
            case GOURAMI -> "tankmates/gouramis.md"; // M29 part c: four gouramis
            case CICHLID -> "tankmates/cichlids.md"; // M29
            case PUFFER -> "tankmates/puffers.md"; // M29 part b
            case BARB -> "tankmates/barbs.md";
            case RAINBOW -> "tankmates/rainbowfish.md"; // M29 part c
            case CLOWNFISH, TANG, GOBY, DAMSEL -> "saltwater/reef_animals.md";
            case SEAHORSE -> "saltwater/seahorses.md";
            case CATFISH, BONYTONGUE, RAY, SHARK, MORAY -> "big_tanks.md"; // M30: the big animals hang directly under their chapter
        };
    }

    public static String groupPage(CritterSpecies s) {
        if (s.marine()) return "saltwater/reef_invertebrates.md";
        return s.isSnail() ? "invertebrates/snails.md" : "invertebrates.md";
    }

    public static String groupPage(PlantSpecies p) {
        if (p.marine()) return "saltwater/corals.md";
        return switch (p.form()) {
            case EPIPHYTE, MOSS -> "plants/epiphytes.md";
            case ROSETTE, BULB -> "plants/rosettes.md";
            case STEM -> "plants/stem_plants.md";
            default -> "plants/runners.md";
        };
    }

    // ================================================================== all generated sections

    /** page path -> (heading line -> body) */
    public Map<String, Map<String, String>> sections() {
        Map<String, Map<String, String>> out = new TreeMap<>();
        for (String target : speciesTargets().keySet()) {
            String page = page(target);
            Map<String, String> m = out.computeIfAbsent(page, k -> new LinkedHashMap<>());
            String profile = profile(target, page);
            if (profile != null) m.put(t("## Profile", "## Steckbrief"), profile);
            m.put(t("## Obtaining", "## Bezug"), obtaining(target, page));
        }
        // devices, food and the other items: the Obtaining section of their page (species pages have their own)
        Map<String, List<String>> pageItems = new TreeMap<>();
        for (var e : itemPages.entrySet()) pageItems.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        for (var e : pageItems.entrySet()) {
            if (out.containsKey(e.getKey())) continue;
            StringBuilder b = new StringBuilder();
            StringBuilder recipes = new StringBuilder();
            boolean several = e.getValue().size() > 1;
            for (String id : e.getValue()) {
                if (!id.startsWith("bettacraft:")) continue;
                String path = id.substring(id.indexOf(':') + 1);
                for (String l : sourceLines(Target.item(path), e.getKey())) {
                    b.append("* ").append(several ? "<ItemLink id=\"" + id + "\" />: " : "").append(l).append(".\n");
                }
                // M24: melted in the crucible (and in the machines of other mods)
                for (String l : crucible.getOrDefault(id, List.of())) {
                    b.append("* ").append(several ? "<ItemLink id=\"" + id + "\" />: " : "")
                            .append(link(t("Crucible", "Schmelztiegel"), "equipment/crucible.md", e.getKey())).append(": ").append(l).append(".\n");
                }
                // M26 part b: made in the food maker
                for (String l : foodMaker.getOrDefault(id, List.of())) {
                    b.append("* ").append(several ? "<ItemLink id=\"" + id + "\" />: " : "")
                            .append(link(t("Food maker", "Futtermaschine"), "food/food_maker.md", e.getKey())).append(": ").append(l).append(".\n");
                }
                if (crucible.containsKey(id)) b.append("* ").append(t("Also in the metallurgic infuser (Mekanism), the alloy smelter (EnderIO), heated "
                        + "mixing (Create) and the induction smelter (Thermal), when installed", "Auch im metallurgischen Infusor (Mekanism), im Legierungsofen "
                        + "(EnderIO), beim beheizten Mischen (Create) und im Induktionsschmelzer (Thermal), wenn installiert")).append(".\n");
                else if (recipesByResult.containsKey(id)) recipes.append("<RecipesFor id=\"").append(id).append("\" />\n");
            }
            if (!recipes.isEmpty()) {
                if (!b.isEmpty()) b.append('\n');
                b.append(t("Crafting:", "Herstellung:")).append("\n\n").append(recipes);
            }
            if (!b.isEmpty()) out.computeIfAbsent(e.getKey(), k -> new LinkedHashMap<>()).put(t("## Obtaining", "## Bezug"), b.toString());
        }
        // overview tables
        tankmateTable(out, "tankmates.md", s -> !s.marine() && !s.isLarge(), t("## All species", "## Alle Arten"));
        tankmateTable(out, "big_tanks.md", TankmateSpecies::isLarge, t("## Big animals", "## Große Tiere")); // M30
        for (String g : List.of("tankmates/tetras.md", "tankmates/rasboras.md", "tankmates/corydoras.md", "tankmates/plecos.md", "tankmates/loaches.md",
                "tankmates/livebearers.md", "tankmates/cichlids.md", "tankmates/puffers.md", "tankmates/barbs.md", "tankmates/rainbowfish.md", "tankmates/gouramis.md", "saltwater/reef_animals.md", "saltwater/seahorses.md")) {
            tankmateTable(out, g, s -> groupPage(s).equals(g), t("## Species", "## Arten"));
        }
        critterTable(out, "invertebrates.md", s -> !s.marine(), true, t("## All species", "## Alle Arten"));
        critterTable(out, "invertebrates/snails.md", s -> !s.marine() && s.isSnail(), false, t("## Species", "## Arten"));
        critterTable(out, "saltwater/reef_invertebrates.md", CritterSpecies::marine, false, t("## Species", "## Arten"));
        plantTable(out, "plants.md", p -> !p.marine(), t("## All plants", "## Alle Pflanzen"));
        for (String g : List.of("plants/epiphytes.md", "plants/rosettes.md", "plants/stem_plants.md", "plants/runners.md", "saltwater/corals.md")) {
            plantTable(out, g, p -> groupPage(p).equals(g), t("## Species", "## Arten"));
        }
        floatingTable(out, "plants/floating_plants.md");
        foreignPlantTable(out, "plants/foreign_plants.md"); // M38
        foreignAnimalTables(out, "settings/foreign_animal_list.md"); // M39
        sourcesPage(out, "getting_started/sources.md");
        advancements(out, "progress.md");
        return out;
    }

    // ================================================================== profiles

    private String profile(String target, String page) {
        int c = target.indexOf(':');
        String kind = c < 0 ? target : target.substring(0, c), id = c < 0 ? "" : target.substring(c + 1);
        return switch (kind) {
            case "tankmate" -> tankmateProfile(TankmateSpecies.byId(id));
            case "critter" -> critterProfile(CritterSpecies.byId(id));
            case "plant" -> plantProfile(PlantSpecies.byId(id));
            case "floating" -> floatingProfile(FloatingPlantType.byId(id));
            default -> null;
        };
    }

    private String tankmateProfile(TankmateSpecies s) {
        Table tb = new Table();
        tb.row(t("Group", "Gruppe"), lang(s.groupKey()));
        tb.row(t("Scientific name", "Wissenschaftlicher Name"), "*" + s.latin() + "*");
        if (s.isLarge()) {
            // M30: a big animal by its real length, and the tank it needs (default size rule)
            var l = s.large();
            var need = org.overengineer.bettacraft.tank.BigTankRules.need(l.lengthM(), org.overengineer.bettacraft.tank.BigTankRules.Factors.DEFAULT);
            tb.row(t("Size", "Größe"), t("about " + num(l.lengthM()) + " m (" + num(s.length()) + " blocks in the game)",
                    "etwa " + num(l.lengthM()) + " m (" + num(s.length()) + " Blöcke im Spiel)"));
            tb.row(t("Tank", "Becken"), t("at least " + need.longSide() + " × " + need.shortSide() + " blocks, " + need.height() + " high, "
                    + need.water() + " water blocks each", "mindestens " + need.longSide() + " × " + need.shortSide() + " Blöcke, " + need.height()
                    + " hoch, je " + need.water() + " Wasserblöcke"));
        } else {
            tb.row(t("Size in the game", "Größe im Spiel"), size(s.length()));
        }
        tb.row(t("Temperature", "Temperatur"), range(s.tempMin(), s.tempMax()) + " °C");
        if (s.breeding() != TankmateSpecies.Breeding.NONE) tb.row(t("Breeding temperature", "Zuchttemperatur"), range(s.breedMin(), s.breedMax()) + " °C");
        tb.row(t("Water", "Wasser"), salinity(s.salinity()) + ", " + sensitivity(s.minQuality(), 0.45, 0.55));
        tb.row(t("Water chemistry", "Wasserchemie"), chem(org.overengineer.bettacraft.chem.ChemTolerance.tankmate(s))); // M25
        tb.row(t("Shoal", "Schwarm"), s.shoaling() ? t("from " + s.shoal() + " fish", "ab " + s.shoal() + " Tieren") : t("alone or as a pair", "einzeln oder als Paar"));
        tb.row(t("Swims", "Schwimmt"), zone(s.zone()));
        tb.row(t("Stocking", "Besatz"), t("counts as " + num(s.stock()) + " fish", "zählt als " + num(s.stock()) + " Fisch"));
        String breeding = lang("tankmate.bettacraft.breeding." + s.breeding().name().toLowerCase(Locale.ROOT));
        if (s.broodMax() > 0 && s.broodMin() == s.broodMax()) breeding += t(", " + s.broodMax() + " young", ", " + s.broodMax() + (s.broodMax() == 1 ? " Junges" : " Junge")); // M30 part c
        else if (s.broodMax() > 0) breeding += t(", " + s.broodMin() + "–" + s.broodMax() + " young", ", " + s.broodMin() + "–" + s.broodMax() + " Junge");
        tb.row(t("Breeding", "Zucht"), breeding);
        if (!s.marine()) tb.row(t("Betta tank", "Betta-Becken"), bettaTank(s));
        List<String> traits = new ArrayList<>();
        if (s.grazer()) traits.add(t("eats algae", "frisst Algen"));
        if (s.eatsSnails()) traits.add(t("eats snails", "frisst Schnecken"));
        if (s.isLarge() && s.preyLength() >= org.overengineer.bettacraft.tankmates.TankmateBreeding.BETTA_PREY_LENGTH)
            traits.add(t("eats small fish, bettas and shrimp", "frisst kleine Fische, Bettas und Garnelen")); // M30
        else if (s.isLarge() && s.preyLength() > 0 && s.preyLength() < 0.2)
            traits.add(t("eats shrimp and only the tiniest fish", "frisst Garnelen und nur die winzigsten Fische")); // M30 part c: the rays
        else if (s.preyLength() > 0) traits.add(t("eats small fish and shrimp", "frisst kleine Fische und Garnelen")); // M29
        if (s.isLarge() && s.large().swim() == org.overengineer.bettacraft.tankmates.LargeSpecies.Swim.RAY_BOTTOM)
            traits.add(t("glides over the ground and lies on the substrate", "gleitet über den Boden und liegt auf dem Bodengrund")); // M30 part b
        else if (s.isLarge() && s.large().swim() == org.overengineer.bettacraft.tankmates.LargeSpecies.Swim.RAY_OPEN)
            traits.add(t("flies through open water with slow wing beats", "fliegt mit langsamen Flügelschlägen durchs freie Wasser")); // M30 part c
        else if (s.isLarge() && s.large().swim() == org.overengineer.bettacraft.tankmates.LargeSpecies.Swim.LURK)
            traits.add(t("lives in a lair under the decoration and comes out at night and for food", "lebt in einer Höhle unter der Deko und kommt nachts und zum Fressen heraus"));
        else if (s.isLarge()) traits.add(t("swims wide curves", "schwimmt weite Bögen"));
        if (s.isLarge() && s.large().swimsConstantly()) traits.add(t("must keep swimming to breathe", "muss zum Atmen ständig schwimmen")); // M30 part c
        if (s.puffer()) traits.add(t("needs snails or hard food for its teeth", "braucht Schnecken oder hartes Futter für die Zähne")); // M29 part b
        if (s.finNipper()) traits.add(t("nips long fins", "zupft lange Flossen"));
        if (s.cichlid()) traits.add(t("guards its brood", "bewacht seine Brut"));
        if (s.parentsFeed()) traits.add(t("feeds its young with skin secretion", "füttert die Jungen mit Hautschleim"));
        if (s.nocturnal()) traits.add(t("active at night", "nachtaktiv"));
        if (s.isLarge() && s.labyrinth()) traits.add(t("air breather: comes up to breathe at the surface", "Luftatmer: holt Luft an der Oberfläche")); // M30 part b
        else if (s.airGulper()) traits.add(t("gulps air at the surface", "holt Luft an der Oberfläche"));
        if (s.labyrinth() && !s.isLarge()) traits.add(t("breathes air (labyrinth fish)", "atmet Luft (Labyrinthfisch)")); // M29 part c
        if (s.territorial()) traits.add(t("territorial", "revierbildend"));
        if (s.bettaTarget()) traits.add(t("chased by male bettas", "wird von Betta-Männchen gejagt"));
        if (s.varieties().size() > 1) traits.add(t(s.varieties().size() + " colour varieties", s.varieties().size() + " Farbformen"));
        if (!traits.isEmpty()) tb.row(t("Special", "Besonderheiten"), String.join(", ", traits));
        return tb.render();
    }

    private String bettaTank(TankmateSpecies s) {
        if (s.isLarge()) return s.preyLength() >= org.overengineer.bettacraft.tankmates.TankmateBreeding.BETTA_PREY_LENGTH
                ? t("no (eats them)", "nein (frisst sie)") : t("no (too big)", "nein (zu groß)"); // M30
        if (s.tempMax() < 28) return t("too cool", "zu kühl");
        if (s.tempMin() > 26) return t("too warm", "zu warm");
        if (s.stock() >= 2.5) return t("too big", "zu groß");
        if (s.preyLength() > 0) return t("no (big, nips fins)", "nein (groß, zupft Flossen)"); // M29 angelfish
        if (s.finNipper()) return t("no (nips fins)", "nein (zupft Flossen)"); // M29 part b
        if (s.cichlid() && s.territorial()) return t("with care (big tank)", "mit Vorsicht (großes Becken)"); // M29 dwarf cichlids
        if (s.bettaTarget() && s.territorial()) return t("no", "nein");
        if (s.bettaTarget()) return t("females only", "nur Weibchen");
        return t("yes", "ja");
    }

    private String critterProfile(CritterSpecies s) {
        Table tb = new Table();
        tb.row(t("Group", "Gruppe"), switch (s.model()) {
            case SHRIMP -> t("Shrimp", "Garnelen");
            case CRAYFISH -> t("Crayfish", "Krebse");
            case HERMIT -> t("Hermit crabs", "Einsiedlerkrebse");
            default -> t("Snails", "Schnecken");
        });
        tb.row(t("Size in the game", "Größe im Spiel"), size(s.length()));
        tb.row(t("Temperature", "Temperatur"), range(s.minTemp(), s.maxTemp()) + " °C");
        tb.row(t("Water", "Wasser"), salinity(SaltTolerance.critter(s)) + ", " + sensitivity(s.badWater(), 0.3, 0.4));
        tb.row(t("Water chemistry", "Wasserchemie"), chem(org.overengineer.bettacraft.chem.ChemTolerance.critter(s))); // M25
        tb.row(t("Breeding", "Vermehrung"), switch (s.breeding()) {
            case NONE -> s.marine() ? t("does not breed in a tank", "vermehrt sich nicht im Aquarium")
                    : t("no young in fresh water", "keine Jungen im Süßwasser");
            case PAIR_BERRIED -> t("male and female; the female carries the eggs, the young hatch fully formed",
                    "Männchen und Weibchen; das Weibchen trägt die Eier, die Jungen schlüpfen fertig entwickelt");
            case PAIR_CLUTCH -> t("male and female; a pink clutch on the glass above the water line",
                    "Männchen und Weibchen; ein rosa Gelege an der Scheibe über der Wasserlinie");
            case SELF -> t("hermaphrodite: one animal is enough", "Zwitter: ein Tier genügt");
            case PAIR -> t("male and female, rarely one young at a time", "Männchen und Weibchen, selten ein Junges");
            case PARTHENO -> t("females only: every adult carries eggs without a male, the young are her clones",
                    "nur Weibchen: jedes erwachsene Tier trägt Eier ohne Männchen, die Jungen sind ihre Klone"); // M29 part d
        } + (s.breeds() && s.broodMax() > 0 ? t(", " + s.broodMin() + "–" + s.broodMax() + " young", ", " + s.broodMin() + "–" + s.broodMax() + " Junge") : ""));
        if (s.breeds()) tb.row(t("Growing up", "Heranwachsen"), t("about " + num(s.juvenileDays()) + " days", "etwa " + num(s.juvenileDays()) + " Tage"));
        if (s.algaePerDay() >= 0.05) {
            tb.row(t("Algae", "Algen"), t("grazes " + num(s.algaePerDay()) + " glass faces a day", "weidet " + num(s.algaePerDay()) + " Scheiben am Tag ab"));
        }
        tb.row(t("Climbs the glass", "Klettert an der Scheibe"), s.climbsGlass() ? t("yes", "ja") : t("no", "nein"));
        int per = switch (s.group()) {
            case SHRIMP -> ServerConfig.STOCKING_SHRIMP_PER_FISH.getDefault();
            case CRAYFISH -> ServerConfig.STOCKING_CRAYFISH_PER_FISH.getDefault();
            case SNAIL -> ServerConfig.STOCKING_SNAILS_PER_FISH.getDefault();
        };
        if (s.stockScale() != 1.0) { // M29 part d: a big crayfish counts as most of a fish
            String f = num(s.stockScale() / per);
            tb.row(t("Stocking", "Besatz"), t("counts as " + f + " fish", "zählt als " + f + " Fisch"));
        } else {
            tb.row(t("Stocking", "Besatz"), t(per + " count as one fish", per + " zählen als ein Fisch"));
        }
        if (s.loci().length > 0) tb.row(t("Colour lines", "Farblinien"), t(s.loci().length + " (bred from wild animals)", s.loci().length + " (aus Wildtieren herausgezüchtet)"));
        return tb.render();
    }

    private String plantProfile(PlantSpecies p) {
        Table tb = new Table();
        tb.row(t("Growth form", "Wuchsform"), p.rolls() ? t("ball of algae (lies loose)", "Algenkugel (liegt lose)") : form(p.form()));
        tb.row(t("Grows on", "Wächst auf"), p.marine() ? t("reef rock, live rock", "Riffgestein, lebendem Gestein") : switch (p.form()) {
            case EPIPHYTE -> t("stone and wood (also decoration)", "Stein und Holz (auch Deko)");
            case MOSS -> p.onlyOnHardscape() ? t("stone and wood (also decoration); or floating on the surface", "Stein und Holz (auch Deko); oder schwimmend an der Oberfläche")
                    : p.rolls() ? t("the floor or any solid block (it lies there and rolls with the current)", "dem Boden oder jedem festen Block (liegt dort und rollt mit der Strömung)")
                    : t("any solid block", "jedem festen Block");
            default -> p.alsoOnHardscape() ? t("substrate (sand, gravel, dirt, the interior floor), also stone and wood",
                    "Bodengrund (Sand, Kies, Erde, Innenboden), auch Stein und Holz")
                    : t("substrate (sand, gravel, dirt, the interior floor)", "Bodengrund (Sand, Kies, Erde, Innenboden)");
        });
        tb.row(t("Light", "Licht"), switch (p.light()) {
            case LOW -> t("low", "wenig");
            case MID -> t("medium", "mittel");
            case HIGH -> t("strong", "viel");
            case VERY_HIGH -> t("very strong (an LED panel or a high-power LED)", "sehr viel (LED-Panel oder Hochleistungs-LED)");
        });
        if (p.marine()) {
            tb.row(t("Flow", "Strömung"), switch (p.flow()) {
                case LOW -> t("gentle", "sanft");
                case MID -> t("medium", "mittel");
                case HIGH -> t("strong", "stark");
            });
            tb.row(t("Calcium", "Kalzium"), p.stony() ? t("builds a skeleton, uses calcium", "baut ein Skelett, verbraucht Kalzium")
                    : t("not needed (soft coral)", "nicht nötig (Weichkoralle)"));
            tb.row(t("Water", "Wasser"), t("sea water", "Meerwasser"));
        }
        tb.row(t("Full size", "Volle Größe"), t("after about " + num(p.daysToFull()) + " days", "nach etwa " + num(p.daysToFull()) + " Tagen"));
        tb.row(t("Spreads", "Breitet sich aus"), spreads(p));
        tb.row(t("Cuttings per trim", "Ableger pro Schnitt"), String.valueOf(p.cuttingsPerTrim(p.trimStages())));
        if (p.tinted() && !p.marine()) tb.row(t("Colour", "Farbe"), switch (p.red()) {
            case TIPS -> t("red tips in strong light", "rote Spitzen bei viel Licht");
            case FLAMINGO -> t("whitish pink, brighter in strong light", "weißlich rosa, kräftiger bei viel Licht");
            case WHITE -> t("greenish; white to pink only in strong light (fully under an LED panel or a high-power LED)",
                    "grünlich; weiß bis rosa nur bei viel Licht (ganz unter einem LED-Panel oder einer Hochleistungs-LED)");
            default -> t("redder in strong light", "röter bei viel Licht");
        });
        // M28: the demanding species
        if (p.needsCo2()) tb.row("CO2", t("needed: without CO2 it grows at " + Math.round(PlantSpecies.NO_CO2_GROWTH * 100) + " % speed",
                "nötig: ohne CO2 wächst sie mit " + Math.round(PlantSpecies.NO_CO2_GROWTH * 100) + " % Geschwindigkeit"));
        if (p.softWater()) tb.row(t("Water chemistry", "Wasserchemie"), t("soft water: best up to pH 7 and 300 TDS, slower in hard water",
                "weiches Wasser: am besten bis pH 7 und 300 TDS, langsamer in hartem Wasser"));
        // M28 part b: Riccia floats too, the Marimo rolls and minds no cool water
        if (p.onlyOnHardscape()) tb.row(t("Floating", "Schwimmend"), t("yes: use it on a tank from outside", "ja: von außen auf ein Aquarium anwenden"));
        if (p.rolls()) {
            tb.row(t("Temperature", "Temperatur"), t("also cool water (from 5 °C)", "auch kühles Wasser (ab 5 °C)"));
            tb.row(t("Rolls", "Rollt"), t("with the current of filters and outlets (config `features.aquaticPlants.marimoRolling`)",
                    "mit der Strömung von Filtern und Auslässen (Einstellung `features.aquaticPlants.marimoRolling`)"));
        }
        return tb.render();
    }

    private String spreads(PlantSpecies p) {
        if (!p.spreads()) return t("no", "nein");
        return switch (p.form()) {
            case STEM -> t("side shoots", "Seitentriebe");
            case POLYPS, DISC -> t("over the rock", "über das Gestein");
            default -> t("runners", "Ausläufer");
        };
    }

    private String floatingProfile(FloatingPlantType f) {
        Table tb = new Table();
        tb.row(t("Growth form", "Wuchsform"), t("floating plant", "Schwimmpflanze"));
        tb.row(t("Leaf size", "Blattgröße"), t("up to " + num(Math.round(f.maxRadius() * 2 * 12 * 10) / 10.0) + " cm", "bis " + num(Math.round(f.maxRadius() * 2 * 12 * 10) / 10.0) + " cm"));
        tb.row(t("Growth", "Wachstum"), f.growthPerDay() >= 1.2 ? t("very fast", "sehr schnell") : f.growthPerDay() >= 0.9 ? t("fast", "schnell")
                : t("medium", "mittel"));
        tb.row(t("Waste uptake", "Nimmt Abfall auf"), "×" + num(f.uptake()));
        tb.row(t("Nest site", "Nestplatz"), f.nestFavour() >= 1.0 ? t("the favourite", "der liebste") : f.nestFavour() >= 0.8 ? t("good", "gut")
                : t("so-so", "mäßig"));
        tb.row(t("Roots", "Wurzeln"), f == FloatingPlantType.HORNWORT ? t("none: its shoots hang deep into the water", "keine: die Triebe hängen tief ins Wasser")
                : f.rootLength() >= 0.25 ? t("long", "lang") : f.rootLength() >= 0.12 ? t("medium", "mittel") : t("short", "kurz"));
        return tb.render();
    }

    // ================================================================== obtaining

    private String obtaining(String target, String page) {
        List<String> lines = sourceLines(target, page);
        // breeding and propagation from the species data
        String breed = breeding(target, page);
        if (breed != null) lines.add(breed);
        String prop = propagation(target);
        if (prop != null) lines.add(prop);
        StringBuilder b = new StringBuilder();
        for (String l : lines) b.append("* ").append(l).append(".\n");
        // M25.1: the waters are biome tags, the biomes of biome mods count too
        if (sources.stream().anyMatch(s -> s.target().equals(target) && biome(s.where()))) {
            b.append('\n').append(t("The biomes of biome mods count too (", "Die Biome von Biome-Mods zählen mit (")).append(link(t("biome tags", "Biom-Tags"),
                    "settings.md", page, false)).append(").\n");
        }
        String item = Target.itemPath(target);
        boolean recipe = item != null && recipesByResult.containsKey("bettacraft:" + item);
        if (recipe) {
            if (!b.isEmpty()) b.append('\n');
            b.append(t("Crafting:", "Herstellung:")).append("\n\n<RecipesFor id=\"bettacraft:").append(item).append("\" />\n");
        }
        if (b.isEmpty()) b.append(t("Not found in the world; only by breeding, or in creative mode.\n", "Nicht in der Welt zu finden; nur durch Zucht oder im Kreativmodus.\n"));
        return b.toString();
    }

    /** One line per wild source and trader offer of the target. */
    private List<String> sourceLines(String target, String page) {
        List<String> lines = new ArrayList<>();
        for (Source s : sources) {
            if (!s.target().equals(target)) continue;
            String cond = s.condition() == null ? "" : t(" (config `" + s.condition() + "`)", " (Config `" + s.condition() + "`)");
            switch (s.kind()) {
                case NET -> lines.add(link(t("Aquarium net", "Kescher"), "equipment/tank_net.md", page) + " " + where(s.where()) + ": "
                        + chance(s.chance(), s.chanceMax()) + t(" per swing", " pro Schwung") + oneIn(s, t("swings", "Schwüngen")) + count(s) + cond);
                case FISHING -> lines.add(bold(t("Fishing", "Angeln")) + " " + where(s.where()) + ": " + chance(s.chance(), s.chanceMax())
                        + t(" per catch", " pro Fang") + oneIn(s, t("catches", "Fängen")) + t(", instead of the fish", ", statt des Fischs") + cond);
                // M30: the big animals
                case BIG_NET -> lines.add(link(t("Large net", "Großer Kescher"), "big_tanks/large_net.md", page) + " " + where(s.where()) + ": "
                        + chance(s.chance(), s.chanceMax()) + t(" per swing", " pro Schwung") + oneIn(s, t("swings", "Schwüngen"))
                        + t(", into an empty transport tub", ", in eine leere Transportwanne") + cond);
                case STEEL_LEADER -> lines.add(link(t("Steel leader", "Stahlvorfach"), "big_tanks/steel_leader.md", page) + t(" (fishing) ", " (Angeln) ")
                        + where(s.where()) + ": " + chance(s.chance(), s.chanceMax()) + t(" per catch", " pro Fang") + oneIn(s, t("catches", "Fängen"))
                        + t(", into an empty transport tub", ", in eine leere Transportwanne") + cond);
                case BOTTLE -> lines.add(s.chance() >= 1 ? bold(t("Glass bottle", "Glasflasche")) + t(" filled with water ", " mit Wasser gefüllt ") + where(s.where())
                        : bold(t("Glass bottle", "Glasflasche")) + t(" filled with water ", " mit Wasser gefüllt ") + where(s.where()) + ": "
                        + chance(s.chance(), s.chanceMax()) + t(" per bottle, along with the water", " pro Flasche, zusätzlich zum Wasser"));
                case SHEARS -> lines.add(bold(t("Shears", "Schere")) + t(" on a " + block(s.block()) + " ", " an einem " + block(s.block()) + " ") + where(s.where()) + ": "
                        + chance(s.chance(), s.chanceMax()) + t(" per cut", " pro Schnitt") + cond);
                case SHOVEL -> lines.add(bold(t("Shovel", "Schaufel")) + t(" on " + block(s.block()) + " ", " auf " + block(s.block()) + " ") + where(s.where()) + ": "
                        + chance(s.chance(), s.chanceMax()) + t(" per try, into an empty fish bag", " pro Versuch, in einen leeren Fischbeutel"));
                case BLOCK_DROP -> lines.add(bold(t("Breaking " + block(s.block()), block(s.block()) + " abbauen"))
                        + (s.where() == Where.SAND ? "" : " " + where(s.where())) + ": " + chance(s.chance(), s.chanceMax()) + t(" per block", " pro Block") + cond);
                case SPAWN -> lines.add(s.target().startsWith("betta:")
                        // M29 part c: a share of the wild bettas
                        ? bold(t("Wild", "Wild")) + t(" " + where(s.where()) + ": about " + Math.round(s.chanceMax() * 100) + " % of the wild bettas",
                        " " + where(s.where()) + ": etwa " + Math.round(s.chanceMax() * 100) + " % der wilden Bettas") + cond
                        : s.max() <= 1 // M31 part c: a lone animal
                        ? bold(t("Wild", "Wild")) + t(" " + where(s.where()) + ", alone (spawn weight " + (int) s.chance() + ")",
                        " " + where(s.where()) + ", einzeln (Spawn-Gewicht " + (int) s.chance() + ")") + cond
                        : bold(t("Wild", "Wild")) + t(" " + where(s.where()) + ", in groups of " + s.min() + "–" + s.max() + " (spawn weight "
                        + (int) s.chance() + ")", " " + where(s.where()) + ", in Gruppen von " + s.min() + "–" + s.max() + " (Spawn-Gewicht " + (int) s.chance() + ")") + cond);
                case TRADER_USUAL, TRADER_RARE -> {
                    String slot = s.kind() == Source.Kind.TRADER_USUAL ? t("usual offers", "übliche Angebote") : t("rare offers", "seltene Angebote");
                    String price = s.min() + t(" for ", " für ") + s.emeralds() + (s.moreSometimes() ? "+" : "") + " " + t(s.emeralds() == 1 ? "emerald" : "emeralds",
                            s.emeralds() == 1 ? "Smaragd" : "Smaragde");
                    String share = s.chance() < 0.999 ? t("; one of " + Math.round(1 / s.chance()) + " in this offer", "; eins von " + Math.round(1 / s.chance())
                            + " in diesem Angebot") : "";
                    lines.add(bold(t("Wandering trader", "Fahrender Händler")) + " (" + slot + "): " + price + share + cond);
                }
                case AQUARIST -> { // M47: chance = the level, chanceMax = the share in the offer
                    String price = s.min() + t(" for ", " für ") + s.emeralds() + " " + t(s.emeralds() == 1 ? "emerald" : "emeralds", s.emeralds() == 1 ? "Smaragd" : "Smaragde");
                    String share = s.chanceMax() < 0.999 ? t("; one of about " + Math.round(1 / s.chanceMax()) + " in this offer", "; eins von etwa "
                            + Math.round(1 / s.chanceMax()) + " in diesem Angebot") : "";
                    lines.add(link(t("Aquarist villager", "Aquarianer"), "getting_started/aquarist.md", page) + " (" + level((int) s.chance()) + "): "
                            + price + share + cond);
                }
            }
        }
        return lines;
    }

    private String breeding(String target, String page) {
        int c = target.indexOf(':');
        String kind = c < 0 ? target : target.substring(0, c), id = c < 0 ? "" : target.substring(c + 1);
        switch (kind) {
            case "betta":
                return link(t("Breeding", "Zucht"), "bettas/breeding.md", page) + t(": bubble nest, eggs and fry in the aquarium",
                        ": Schaumnest, Eier und Jungfische im Aquarium");
            case "tankmate": {
                TankmateSpecies s = TankmateSpecies.byId(id);
                if (s.breeding() == TankmateSpecies.Breeding.NONE) return null;
                String where = s.seahorse() ? "saltwater/seahorses.md" : s.isLarge() && s.marine() ? "big_tanks.md" // M30 part c: the size rule
                : s.marine() ? "saltwater/reef_animals.md" : "tankmates/breeding.md";
                return link(t("Breeding", "Zucht"), where, page) + ": " + lang("tankmate.bettacraft.breeding." + s.breeding().name().toLowerCase(Locale.ROOT));
            }
            case "critter": {
                CritterSpecies s = CritterSpecies.byId(id);
                if (!s.breeds()) return null;
                return bold(t("Breeding", "Vermehrung")) + t(" in the aquarium", " im Aquarium")
                        + (s.hermaphrodite() || s.allFemale() ? t(" (one animal is enough)", " (ein Tier genügt)") : t(" (male and female)", " (Männchen und Weibchen)"));
            }
            default:
                return null;
        }
    }

    private String propagation(String target) {
        int c = target.indexOf(':');
        String kind = c < 0 ? target : target.substring(0, c), id = c < 0 ? "" : target.substring(c + 1);
        if (kind.equals("plant")) {
            PlantSpecies p = PlantSpecies.byId(id);
            int n = p.cuttingsPerTrim(p.trimStages());
            String s = bold(p.marine() ? t("Frags", "Ableger") : t("Cuttings", "Ableger")) + t(": shears on a grown " + (p.marine() ? "coral" : "plant") + " give "
                    + n + (n == 1 ? " cutting" : " cuttings") + " per trim", ": eine Schere an " + (p.marine() ? "einer gewachsenen Koralle" : "einer gewachsenen Pflanze")
                    + " gibt " + n + " Ableger pro Schnitt");
            if (p.form() == PlantSpecies.Form.EPIPHYTE) s += t("; breaking a rhizome piece divides it", "; ein abgebautes Rhizomstück teilt sie");
            if (p.spreads()) s += t("; it also spreads by itself (" + spreads(p) + ")", "; sie breitet sich auch selbst aus (" + spreads(p) + ")");
            return s;
        }
        if (kind.equals("floating")) {
            return bold(t("Offshoots", "Ableger")) + t(": grows on a lit aquarium surface and forms offshoots; take some out with the aquarium net",
                    ": wächst auf einer beleuchteten Wasseroberfläche und bildet Ableger; mit dem Kescher herausnehmen");
        }
        return null;
    }

    // ================================================================== overview tables

    private void tankmateTable(Map<String, Map<String, String>> out, String page, Predicate<TankmateSpecies> which, String heading) {
        Table tb = new Table(t("Species", "Art"), "°C", t("Shoal", "Schwarm"), page.startsWith("saltwater") ? t("Water", "Wasser") : t("Betta tank", "Betta-Becken"),
                t("Breeding", "Zucht"));
        for (TankmateSpecies s : TankmateSpecies.values()) {
            if (!which.test(s)) continue;
            tb.row("<ItemImage id=\"bettacraft:" + (s.isLarge() ? s.spawnEggId() : s.bucketId()) + "\" /> " + link(lang(s.translationKey()), page(Target.tankmate(s)), page, false),
                    range(s.tempMin(), s.tempMax()), s.shoaling() ? String.valueOf(s.shoal()) : "–",
                    page.startsWith("saltwater") ? salinity(s.salinity()) : bettaTank(s),
                    lang("tankmate.bettacraft.breeding." + s.breeding().name().toLowerCase(Locale.ROOT)));
        }
        out.computeIfAbsent(page, k -> new LinkedHashMap<>()).put(heading, tb.render());
    }

    private void critterTable(Map<String, Map<String, String>> out, String page, Predicate<CritterSpecies> which, boolean frog, String heading) {
        Table tb = new Table(t("Animal", "Tier"), "°C", t("Breeding", "Vermehrung"), t("Algae", "Algen"));
        for (CritterSpecies s : CritterSpecies.values()) {
            if (!which.test(s)) continue;
            tb.row(link(lang(s.translationKey()), page(Target.critter(s)), page, false), range(s.minTemp(), s.maxTemp()),
                    !s.breeds() ? t("no", "nein") : s.hermaphrodite() || s.allFemale() ? t("one is enough", "ein Tier genügt") : t("pair", "Paar"),
                    s.algaePerDay() >= 1.0 ? t("a lot", "viel") : s.algaePerDay() >= 0.25 ? t("some", "etwas") : t("hardly", "kaum"));
        }
        if (frog) {
            tb.row(link(lang("entity.bettacraft.dwarf_clawed_frog"), page(Target.FROG), page, false), "20–28", t("no", "nein"), t("no", "nein"));
        }
        out.computeIfAbsent(page, k -> new LinkedHashMap<>()).put(heading, tb.render());
    }

    private void plantTable(Map<String, Map<String, String>> out, String page, Predicate<PlantSpecies> which, String heading) {
        boolean coral = page.startsWith("saltwater");
        Table tb = coral ? new Table("", t("Coral", "Koralle"), t("Light", "Licht"), t("Flow", "Strömung"), t("Calcium", "Kalzium"), t("Full size", "Volle Größe"))
                : new Table("", t("Plant", "Pflanze"), t("Growth form", "Wuchsform"), t("Light", "Licht"), t("Full size", "Volle Größe"));
        for (PlantSpecies p : PlantSpecies.values()) {
            if (!which.test(p)) continue;
            String light = switch (p.light()) {
                case LOW -> t("low", "wenig");
                case MID -> t("medium", "mittel");
                case HIGH -> t("strong", "viel");
                case VERY_HIGH -> t("very strong", "sehr viel");
            };
            String days = t("about " + num(p.daysToFull()) + " days", "etwa " + num(p.daysToFull()) + " Tage");
            String name = link(lang("item.bettacraft." + p.id()), page(Target.plant(p)), page, false);
            if (coral) {
                tb.row("<ItemImage id=\"bettacraft:" + p.id() + "\" />", name, light, switch (p.flow()) {
                    case LOW -> t("gentle", "sanft");
                    case MID -> t("medium", "mittel");
                    case HIGH -> t("strong", "stark");
                }, p.stony() ? t("yes", "ja") : t("no", "nein"), days);
            } else {
                tb.row("<ItemImage id=\"bettacraft:" + p.id() + "\" />", name, form(p.form()), light, days);
            }
        }
        out.computeIfAbsent(page, k -> new LinkedHashMap<>()).put(heading, tb.render());
    }

    private void floatingTable(Map<String, Map<String, String>> out, String page) {
        Table tb = new Table("", t("Plant", "Pflanze"), t("Growth", "Wachstum"), t("Nest site", "Nestplatz"));
        for (FloatingPlantType f : FloatingPlantType.values()) {
            if (!f.grows()) continue;
            // M28: floating Riccia links to its plant page
            String to = f.plantItem() ? page(Target.plant(PlantSpecies.byId(f.id()))) : page(Target.floating(f));
            tb.row("<ItemImage id=\"bettacraft:" + f.id() + "\" />", link(lang("item.bettacraft." + f.id()), to, page, false),
                    f.growthPerDay() >= 1.2 ? t("very fast", "sehr schnell") : f.growthPerDay() >= 0.9 ? t("fast", "schnell") : t("medium", "mittel"),
                    f.nestFavour() >= 1.0 ? t("the favourite", "der liebste") : f.nestFavour() >= 0.8 ? t("good", "gut") : t("so-so", "mäßig"));
        }
        out.computeIfAbsent(page, k -> new LinkedHashMap<>()).put(t("## Species", "## Arten"), tb.render());
    }

    // ================================================================== M38: plants of Minecraft and other mods

    /** The default factor of interiorPlants.foreignPlantFactor (the table shows the plant mass with it). */
    static final double FOREIGN_FACTOR = 0.6;

    private static final Map<String, String> MOD_NAMES = Map.of("minecraft", "Minecraft", "fintastic", "Fintastic",
            "regions_unexplored", "Regions Unexplored", "alexscaves", "Alex's Caves", "twilightforest", "Twilight Forest");

    /** The profiles of {@code data/bettacraft/bettacraft/foreign_plants}: mod, blocks, role and what they count with (Minecraft first). */
    private void foreignPlantTable(Map<String, Map<String, String>> out, String page) {
        Path dir = project().resolve("src/main/resources/data/bettacraft/bettacraft/foreign_plants");
        List<Path> files = new ArrayList<>();
        try (var st = Files.list(dir)) {
            st.filter(f -> f.toString().endsWith(".json")).sorted().forEach(files::add);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        files.sort(java.util.Comparator.comparing((Path f) -> !f.getFileName().toString().startsWith("minecraft_")).thenComparing(f -> f.getFileName().toString()));
        Table tb = new Table(t("Mod", "Mod"), t("Blocks", "Blöcke"), t("Counts as", "Zählt als"), t("Effect", "Wirkung"));
        for (Path f : files) {
            org.overengineer.bettacraft.plants.foreign.ForeignPlantProfile p;
            try {
                p = org.overengineer.bettacraft.plants.foreign.ForeignPlantProfile.parse(
                        com.google.gson.JsonParser.parseString(Files.readString(f)).getAsJsonObject(), w -> {});
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            List<String> ids = new ArrayList<>();
            for (var b : p.blocks()) ids.add("`" + b + "`");
            for (var tag : p.tags()) ids.add("`#" + tag + "`");
            String ns = !p.blocks().isEmpty() ? p.blocks().get(0).getNamespace() : p.tags().get(0).getNamespace();
            var role = p.role();
            String as = switch (role) {
                case STEM -> t("stem plant", "Stängelpflanze");
                case ROSETTE -> t("rosette", "Rosette");
                case EPIPHYTE -> t("epiphyte", "Aufsitzerpflanze");
                case MOSS -> t("moss", "Moos");
                case CARPET -> t("carpet", "Bodendecker");
                case RIBBON -> t("ribbon plant", "Bandpflanze");
                case BULB -> t("bulb", "Knollenpflanze");
                case FLOATING -> t("floating leaves", "Schwimmblätter");
                case CORAL -> t("coral", "Koralle");
                case LIVE_ROCK -> t("live rock", "Lebendgestein");
                case NONE -> t("decoration", "Deko");
            };
            String effect;
            if (role == org.overengineer.bettacraft.plants.foreign.ForeignPlantRole.LIVE_ROCK) {
                effect = t("biological filter in salt water, " + Math.round(FOREIGN_FACTOR * p.factor() * 100) + " % of our live rock",
                        "biologischer Filter im Meerwasser, " + Math.round(FOREIGN_FACTOR * p.factor() * 100) + " % unseres Lebendgesteins");
            } else {
                long mass = Math.round(FOREIGN_FACTOR * p.factor() * role.adultMass(16));
                effect = t(mass + " blocks of plant mass per plant", mass + " Blöcke Pflanzenmasse pro Pflanze")
                        + (role == org.overengineer.bettacraft.plants.foreign.ForeignPlantRole.FLOATING ? t(", shade", ", Schatten") : "");
            }
            tb.row(MOD_NAMES.getOrDefault(ns, ns), String.join(", ", ids), as, effect);
        }
        out.computeIfAbsent(page, k -> new LinkedHashMap<>()).put(t("## The plants", "## Die Pflanzen"), tb.render());
    }

    // ================================================================== M39: the animals of other mods

    private static final Map<String, String> ANIMAL_MODS = Map.of("minecraft", "Minecraft", "fintastic", "Fintastic", "aquaculture", "Aquaculture 2",
            "fishofthieves", "Fish of Thieves", "naturalist", "Naturalist", "crittersandcompanions", "Critters and Companions",
            "livingthings", "Living Things", "alexscaves", "Alex's Caves", "twilightforest", "Twilight Forest");

    /**
     * The shipped profiles of {@code data/bettacraft/bettacraft/foreign_animals}: the water animals, the land and marsh animals
     * (where they live, the size rule, food, breeding) and the refused ones with their reason; sorted by mod and file.
     */
    private void foreignAnimalTables(Map<String, Map<String, String>> out, String page) {
        Path dir = project().resolve("src/main/resources/data/bettacraft/bettacraft/foreign_animals");
        List<Path> files = new ArrayList<>();
        try (var st = Files.list(dir)) {
            st.filter(f -> f.toString().endsWith(".json")).sorted().forEach(files::add);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Table water = new Table(t("Mod", "Mod"), t("Animals", "Tiere"), t("Water", "Wasser"), t("Size rule", "Größenregel"), t("Food", "Futter"),
                t("Breeding", "Zucht"));
        Table land = new Table(t("Mod", "Mod"), t("Animals", "Tiere"), t("Tank", "Becken"), t("Size rule", "Größenregel"), t("Food", "Futter"),
                t("Breeding", "Zucht"));
        Table refused = new Table(t("Mod", "Mod"), t("Animals", "Tiere"), t("Why", "Warum"));
        var f = new org.overengineer.bettacraft.tank.BigTankRules.Factors(12, 6, 3, 100);
        for (Path file : files) {
            org.overengineer.bettacraft.foreign.ForeignProfile p;
            try {
                p = org.overengineer.bettacraft.foreign.ForeignProfile.parse(com.google.gson.JsonParser.parseString(Files.readString(file)).getAsJsonObject(), w -> {});
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            List<String> ids = new ArrayList<>();
            for (var id : p.entities()) ids.add("`" + id + "`");
            String ns = p.entities().get(0).getNamespace();
            String mod = ANIMAL_MODS.getOrDefault(ns, ns);
            if (p.refuse() != null) {
                String why = switch (p.refuse()) {
                    case "deep_sea" -> t("deep-sea animals (cold, dark, high pressure)", "Tiefseetiere (kalt, dunkel, hoher Druck)");
                    case "too_big" -> t("far too big for any tank", "viel zu groß für jedes Becken");
                    case "hostile" -> t("hostile creatures", "feindliche Wesen");
                    case "fungus" -> t("mushroom creatures, no water animals", "Pilzwesen, keine Wassertiere");
                    case "cave" -> t("bound to their cave world", "an ihre Höhlenwelt gebunden");
                    default -> t("not for a tank", "nichts fürs Becken");
                };
                refused.row(mod, String.join(", ", ids), why);
                continue;
            }
            double len = p.bigLength();
            String size = "–";
            if (len > 0) {
                var n = org.overengineer.bettacraft.tank.BigTankRules.need(len, f);
                size = t(fmt1(len) + " m: " + n.longSide() + " × " + n.shortSide() + " × " + n.height() + " blocks, " + n.water() + " blocks of room",
                        fmt1(len).replace('.', ',') + " m: " + n.longSide() + " × " + n.shortSide() + " × " + n.height() + " Blöcke, " + n.water() + " Blöcke Raum");
            }
            String food = foodWords(p);
            String breed = breedWords(p);
            if (p.terrarium() == null) {
                String salt = saltWord(p.salinity()) + ", " + (int) p.tempMin() + "–" + (int) p.tempMax() + " °C";
                water.row(mod, String.join(", ", ids), salt, size, food, breed);
            } else {
                String tank = switch (p.terrarium().habitat()) {
                    case DRY -> t("terrarium", "Terrarium");
                    case PALUDARIUM, WET -> t("paludarium", "Paludarium");
                    default -> t("terrarium or paludarium", "Terrarium oder Paludarium");
                };
                if (Double.isFinite(p.terrarium().air().min())) tank += ", " + (int) p.terrarium().air().min() + "–" + (int) p.terrarium().air().max() + " °C";
                if (p.land() != null && !p.land().needs().isEmpty()) { // M39 part c: flowers, grass, a beehive in the interior
                    List<String> needs = new ArrayList<>();
                    for (var n : p.land().needs()) {
                        needs.add(switch (n) {
                            case FLOWERS -> t("flowers", "Blumen");
                            case GRASS -> t("grass", "Gras");
                            case HIVE -> t("a beehive", "ein Bienenstock");
                        });
                    }
                    tank += "; " + t("needs ", "braucht ") + String.join(t(" and ", " und "), needs);
                }
                land.row(mod, String.join(", ", ids), tank, size, food, breed);
            }
        }
        Map<String, String> m = out.computeIfAbsent(page, k -> new LinkedHashMap<>());
        m.put(t("## Water animals", "## Wassertiere"), water.render());
        m.put(t("## Land and marsh animals", "## Land- und Sumpftiere"), land.render());
        m.put(t("## Refused animals", "## Abgelehnte Tiere"), refused.render());
    }

    private String saltWord(org.overengineer.bettacraft.tank.Salinity.Range r) {
        if (r.equals(org.overengineer.bettacraft.tank.Salinity.Range.FRESH)) return t("fresh", "Süßwasser");
        if (r.equals(org.overengineer.bettacraft.tank.Salinity.Range.BRACKISH)) return t("fresh to brackish", "Süß- bis Brackwasser");
        if (r.equals(org.overengineer.bettacraft.tank.Salinity.Range.BRACKISH_ONLY)) return t("brackish", "Brackwasser");
        if (r.equals(org.overengineer.bettacraft.tank.Salinity.Range.MARINE) || r.equals(org.overengineer.bettacraft.tank.Salinity.Range.HARDY_MARINE)) {
            return t("sea", "Meerwasser");
        }
        if (r.equals(org.overengineer.bettacraft.tank.Salinity.Range.ANY)) return t("fresh or sea", "Süß- oder Meerwasser");
        return (int) r.min() + "–" + (int) r.max() + " ‰";
    }

    private String foodWords(org.overengineer.bettacraft.foreign.ForeignProfile p) {
        List<String> w = new ArrayList<>();
        for (String x : p.food()) {
            w.add(switch (x) {
                case "normal" -> t("pellets and flakes", "Pellets und Flocken");
                case "live" -> t("live and frozen food", "Lebend- und Frostfutter");
                case "fry" -> t("fry food", "Aufzuchtfutter");
                default -> "`" + x + "`";
            });
        }
        return w.isEmpty() ? "–" : String.join(", ", w);
    }

    private String breedWords(org.overengineer.bettacraft.foreign.ForeignProfile p) {
        var b = p.breeding();
        if (!b.breeds()) return t("no", "nein");
        StringBuilder s = new StringBuilder(b.type() == org.overengineer.bettacraft.foreign.ForeignProfile.Type.LIVEBEARER ? t("live young", "lebendgebärend")
                : b.nests() ? t("eggs into a nest on the land", "Eier in ein Nest an Land") : t("eggs", "Eier"));
        if (b.needsPlants()) s.append(t(" on plants", " an Pflanzen"));
        if (b.minWaterBlocks() > 0) s.append(t(", from " + (int) b.minWaterBlocks() + " water blocks", ", ab " + (int) b.minWaterBlocks() + " Wasserblöcken"));
        if (b.sex() == org.overengineer.bettacraft.foreign.ForeignProfile.Sex.TEMPERATURE) {
            s.append(t(", the nest temperature decides the sex (half and half at " + fmt1(b.nestPivot()) + " °C)",
                    ", die Nesttemperatur bestimmt das Geschlecht (halb und halb bei " + fmt1(b.nestPivot()).replace('.', ',') + " °C)"));
        } else if (b.sex() == org.overengineer.bettacraft.foreign.ForeignProfile.Sex.RANDOM) {
            s.append(t(", a female and a male", ", ein Weibchen und ein Männchen"));
        }
        if (!b.inherit().isEmpty()) s.append(t(", variants inherited", ", Varianten vererbt"));
        if (!b.sterile().isEmpty()) s.append(t(", trophies do not breed", ", Trophäen vermehren sich nicht"));
        return s.toString();
    }

    private static String fmt1(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(Math.round(v * 10) / 10.0);
    }

    // ================================================================== the page "where to find what"

    private void sourcesPage(Map<String, Map<String, String>> out, String page) {
        Map<String, String> m = out.computeIfAbsent(page, k -> new LinkedHashMap<>());
        for (Source.Kind kind : new Source.Kind[]{Source.Kind.NET, Source.Kind.FISHING}) {
            Map<Where, List<String>> byPlace = new LinkedHashMap<>();
            for (Source s : sources) {
                if (s.kind() != kind) continue;
                byPlace.computeIfAbsent(s.where(), k -> new ArrayList<>()).add(link(name(s.target()), page(s.target()), page, false) + " "
                        + chance(s.chance(), s.chanceMax()));
            }
            Table tb = new Table(t("Where", "Wo"), kind == Source.Kind.NET ? t("Catch (chance per swing)", "Fang (Chance pro Schwung)")
                    : t("Catch (chance per catch)", "Fang (Chance pro Fang)"));
            for (var e : byPlace.entrySet()) tb.row(cap(where(e.getKey())), String.join(", ", e.getValue()));
            m.put(kind == Source.Kind.NET ? t("## Aquarium net", "## Kescher") : t("## Fishing", "## Angeln"), tb.render());
        }
        StringBuilder other = new StringBuilder();
        for (Source s : sources) {
            if (s.kind() == Source.Kind.NET || s.kind() == Source.Kind.FISHING || s.kind() == Source.Kind.TRADER_USUAL || s.kind() == Source.Kind.TRADER_RARE
                    || s.kind() == Source.Kind.AQUARIST) continue;
            String what = link(name(s.target()), page(s.target()), page, false);
            String how = switch (s.kind()) {
                case BOTTLE -> t("glass bottle filled with water ", "Glasflasche mit Wasser ") + where(s.where());
                case SHEARS -> t("shears on a " + block(s.block()) + " ", "Schere an einem " + block(s.block()) + " ") + where(s.where());
                case SHOVEL -> t("shovel on " + block(s.block()) + " ", "Schaufel auf " + block(s.block()) + " ") + where(s.where());
                case BLOCK_DROP -> t("breaking " + block(s.block()), block(s.block()) + " abbauen") + (s.where() == Where.SAND ? "" : " " + where(s.where()));
                case SPAWN -> t("spawns ", "spawnt ") + where(s.where());
                case BIG_NET -> t("large net ", "großer Kescher ") + where(s.where()); // M30
                case STEEL_LEADER -> t("fishing with a steel leader ", "Angeln mit Stahlvorfach ") + where(s.where());
                default -> "";
            };
            String ch = s.kind() == Source.Kind.SPAWN ? t("spawn weight " + (int) s.chance(), "Spawn-Gewicht " + (int) s.chance())
                    : s.chance() >= 1 ? t("always", "immer") : chance(s.chance(), s.chanceMax());
            other.append("* ").append(what).append(": ").append(how).append(" (").append(ch).append(").\n");
        }
        m.put(t("## Other finds", "## Weitere Funde"), other.toString());
        for (Source.Kind kind : new Source.Kind[]{Source.Kind.TRADER_USUAL, Source.Kind.TRADER_RARE}) {
            Table tb = new Table(t("Thing", "Ware"), t("Count", "Anzahl"), t("Emeralds", "Smaragde"));
            for (Source s : sources) {
                if (s.kind() != kind) continue;
                tb.row(link(name(s.target()), page(s.target()), page, false), String.valueOf(s.min()), s.emeralds() + (s.moreSometimes() ? "+" : ""));
            }
            m.put(kind == Source.Kind.TRADER_USUAL ? t("## Wandering trader: usual offers", "## Fahrender Händler: übliche Angebote")
                    : t("## Wandering trader: rare offers", "## Fahrender Händler: seltene Angebote"), tb.render());
        }
        // M47: the aquarist villager
        Table aq = new Table(t("Level", "Stufe"), t("Thing", "Ware"), t("Count", "Anzahl"), t("Emeralds", "Smaragde"));
        for (Source s : sources) {
            if (s.kind() != Source.Kind.AQUARIST) continue;
            aq.row(level((int) s.chance()), link(name(s.target()), page(s.target()), page, false), String.valueOf(s.min()), String.valueOf(s.emeralds()));
        }
        m.put(t("## Aquarist villager", "## Aquarianer"), aq.render());
    }

    // ================================================================== advancements

    private void advancements(Map<String, Map<String, String>> out, String page) {
        Path dir = project().resolve("src/generated/resources/data/bettacraft/advancement");
        Map<String, JsonObject> all = new LinkedHashMap<>();
        try (Stream<Path> s = Files.list(dir)) {
            for (Path p : s.filter(f -> f.toString().endsWith(".json")).sorted().toList()) {
                all.put("bettacraft:" + p.getFileName().toString().replace(".json", ""), JsonParser.parseString(Files.readString(p)).getAsJsonObject());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Table tb = new Table("", t("Advancement", "Fortschritt"), t("How", "Wie"));
        walk(all, "bettacraft:root", tb, 0);
        out.computeIfAbsent(page, k -> new LinkedHashMap<>()).put(t("## Advancements", "## Fortschritte"), tb.render());
    }

    private void walk(Map<String, JsonObject> all, String id, Table tb, int depth) {
        JsonObject a = all.get(id);
        if (a == null) return;
        JsonObject d = a.getAsJsonObject("display");
        if (d != null) {
            String icon = d.getAsJsonObject("icon").get("id").getAsString();
            String title = lang(d.getAsJsonObject("title").get("translate").getAsString());
            String desc = lang(d.getAsJsonObject("description").get("translate").getAsString());
            tb.row("<ItemImage id=\"" + icon + "\" />", (depth > 1 ? "↳ ".repeat(1) : "") + "**" + title + "**", desc);
        }
        for (var e : all.entrySet()) {
            JsonElement parent = e.getValue().get("parent");
            if (parent != null && parent.getAsString().equals(id)) walk(all, e.getKey(), tb, depth + 1);
        }
    }

    // ================================================================== words

    /** M47: a villager level by its vanilla name. */
    private String level(int n) {
        String[] en = {"", "Novice", "Apprentice", "Journeyman", "Expert", "Master"}, de = {"", "Neuling", "Lehrling", "Geselle", "Experte", "Meister"};
        int k = Math.max(1, Math.min(5, n));
        return t(en[k], this.de ? de[k] : en[k]);
    }

    private String t(String en, String de) {
        return this.de ? de : en;
    }

    private String lang(String key) {
        String v = lang.get(key);
        if (v == null) throw new IllegalStateException("missing lang key " + key + (de ? " (de)" : " (en)"));
        return v;
    }

    /** The display name of a target. */
    public String name(String target) {
        int c = target.indexOf(':');
        String kind = c < 0 ? target : target.substring(0, c), id = c < 0 ? "" : target.substring(c + 1);
        return switch (kind) {
            case "betta" -> id.isEmpty() ? lang("entity.bettacraft.betta") : lang("betta.bettacraft.species." + id);
            case "frog" -> lang("entity.bettacraft.dwarf_clawed_frog");
            case "tankmate" -> lang(TankmateSpecies.byId(id).translationKey());
            case "critter" -> lang(CritterSpecies.byId(id).translationKey());
            default -> lang.containsKey("item.bettacraft." + id) ? lang("item.bettacraft." + id) : lang("block.bettacraft." + id);
        };
    }

    /** M25.1: a place given by biome tags (also the biomes of biome mods). */
    private static boolean biome(Where w) {
        return w != null && w != Where.CAVE && w != Where.NEAR_WATER && w != Where.SAND;
    }

    private String where(Where w) {
        return switch (w) {
            case RIVER -> t("in rivers", "in Flüssen");
            case SWAMP -> t("in swamps", "in Sümpfen");
            case MANGROVE -> t("in mangrove swamps", "in Mangrovensümpfen");
            case BEACH -> t("at beaches", "an Stränden");
            case JUNGLE -> t("in jungle water", "in Dschungelgewässern");
            case CAVE -> t("in deep cave water", "in tiefem Höhlenwasser");
            case LUSH_CAVES -> t("in lush caves", "in üppigen Höhlen");
            case WARM_OCEAN -> t("in warm oceans (coral reefs)", "in warmen Ozeanen (Korallenriffe)");
            case LUKEWARM_OCEAN -> t("in lukewarm oceans", "in lauwarmen Ozeanen");
            case JUNGLE_OR_RIVER -> t("in jungles and rivers", "in Dschungeln und Flüssen");
            case SWAMP_OR_MANGROVE -> t("in swamps and mangrove swamps", "in Sümpfen und Mangrovensümpfen");
            case SALT_WATER -> t("in oceans and at beaches", "in Ozeanen und an Stränden");
            case NEAR_WATER -> t("next to water", "neben Wasser");
            case SAND -> t("anywhere", "überall");
            case COLD_WATER -> t("in cold lakes and frozen rivers", "in kalten Seen und gefrorenen Flüssen");
            case DRY_LAND -> t("in savannas and dry shrublands", "in Savannen und trockenem Buschland"); // M31 part b
            case JUNGLE_FLOOR -> t("on the floor of jungles", "auf dem Boden von Dschungeln"); // M31 part c
            case ARID -> t("in deserts and badlands", "in Wüsten und Tafelbergen");
            case WOODLAND -> t("in forests and plains", "in Wäldern und Ebenen");
        };
    }

    private String block(String id) {
        return switch (id) {
            case "minecraft:mud" -> t("mud", "Schlamm");
            case "minecraft:moss_block" -> t("moss block", "Moosblock");
            case "#c:sands" -> t("sand", "Sand");
            // M28.1: the wild plants (one block per growth form) and the wild floating plants
            case "bettacraft:wild_epiphyte", "bettacraft:wild_moss", "bettacraft:wild_rosette", "bettacraft:wild_stem", "bettacraft:wild_carpet" ->
                    t("wild plant", "wilden Pflanzenbestand");
            case "bettacraft:wild_floating_plant" -> t("wild floating plants", "Wilde Schwimmpflanzen");
            default -> id.startsWith("bettacraft:wild_") ? t("wild coral colony", "wilden Korallenstock") : id; // M22.3
        };
    }

    private String form(PlantSpecies.Form f) {
        return switch (f) {
            case EPIPHYTE -> t("epiphyte (rhizome)", "Aufsitzerpflanze (Rhizom)");
            case MOSS -> t("moss", "Moos");
            case ROSETTE -> t("rosette", "Rosettenpflanze");
            case STEM -> t("stem plant", "Stängelpflanze");
            case RIBBON -> t("ribbons with runners", "Bänder mit Ausläufern");
            case CARPET -> t("carpet", "Bodendecker");
            case BULB -> t("bulb with floating leaves", "Knolle mit Schwimmblättern");
            case POLYPS -> t("polyp mat (soft coral)", "Polypenrasen (Weichkoralle)");
            case DISC -> t("discs (soft coral)", "Scheiben (Weichkoralle)");
            case TOADSTOOL -> t("toadstool (soft coral)", "Pilzlederkoralle (Weichkoralle)");
            case BRANCH -> t("branches (stony coral)", "Äste (Steinkoralle)");
            case DOME -> t("dome (stony coral)", "Kuppel (Steinkoralle)");
            case ANEMONE -> t("anemone", "Anemone");
        };
    }

    private String zone(TankmateSpecies.Zone z) {
        return switch (z) {
            case BOTTOM -> t("on the bottom", "am Boden");
            case LOW -> t("in the lower water", "im unteren Wasser");
            case MID -> t("in the middle", "in der Mitte");
            case UPPER -> t("near the surface", "nahe der Oberfläche");
            case GLASS -> t("on the glass and the bottom", "an der Scheibe und am Boden");
        };
    }

    private String salinity(Salinity.Range r) {
        if (r.min() >= Salinity.MARINE_FROM + 5) return t("sea water", "Meerwasser");
        if (r.min() >= Salinity.BRACKISH_FROM && r.min() < 10) return t("brackish water, also sea water", "Brackwasser, auch Meerwasser"); // M29 part b
        if (r.min() >= Salinity.BRACKISH_FROM) return t("sea water, also strong brackish water", "Meerwasser, auch starkes Brackwasser");
        if (r.max() >= Salinity.MARINE_FROM) return t("fresh, brackish or sea water", "Süß-, Brack- oder Meerwasser");
        if (r.max() > Salinity.BRACKISH_FROM + 2) return t("fresh or brackish water", "Süß- oder Brackwasser");
        return t("fresh water", "Süßwasser");
    }

    private String sensitivity(double q, double hardy, double normal) {
        return q <= hardy ? t("hardy", "robust") : q <= normal ? t("normal", "normal") : t("sensitive", "empfindlich");
    }

    private String size(double blocks) {
        double cm = Math.round(blocks * 12 * 2) / 2.0;
        return t("about " + num(cm) + " cm (" + num(Math.round(blocks * 100) / 100.0) + " blocks)", "etwa " + num(cm) + " cm (" + num(Math.round(blocks * 100) / 100.0)
                + " Blöcke)");
    }

    /** M25: "pH 6.0–8.0, TDS 0–600 ppm" (salt water: only the pH). */
    private String chem(org.overengineer.bettacraft.chem.ChemTolerance.Range r) {
        String ph = "pH " + ph(r.phMin()) + "–" + ph(r.phMax());
        return r.marine() ? ph : ph + ", TDS " + range(r.tdsMin(), r.tdsMax()) + " ppm";
    }

    private String ph(double v) {
        String s = String.format(Locale.ROOT, "%.1f", v);
        return de ? s.replace('.', ',') : s;
    }

    private String range(double a, double b) {
        return num(a) + "–" + num(b);
    }

    private String num(double v) {
        String s;
        if (Math.abs(v - Math.round(v)) < 1e-9) s = String.valueOf(Math.round(v));
        else s = String.format(Locale.ROOT, "%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
        return de ? s.replace('.', ',') : s;
    }

    private String percent(double p) {
        double v = p * 100;
        String s = v >= 10 ? String.format(Locale.ROOT, "%.0f", v) : v >= 1 ? String.format(Locale.ROOT, "%.1f", v) : String.format(Locale.ROOT, "%.2f", v);
        if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return (de ? s.replace('.', ',') : s) + " %";
    }

    private String chance(double lo, double hi) {
        String a = percent(lo), b = percent(hi);
        return a.equals(b) ? a : a.replace(" %", "") + "–" + b;
    }

    /** M22.4: " (about 1 in 40 swings)" for the net and fishing (a range when plants nearby or a hard bottom raise it). */
    private String oneIn(Source s, String what) {
        if (s.chanceMax() <= 0 || s.chanceMax() >= 0.5) return "";
        long a = Math.round(1 / s.chanceMax()), b = s.chance() > 0 ? Math.round(1 / s.chance()) : a;
        String n = a == b ? String.valueOf(a) : a + "–" + b;
        return t(" (about 1 in " + n + " " + what + ")", " (etwa 1 von " + n + " " + what + ")");
    }

    private String count(Source s) {
        if (s.max() <= 1) return "";
        return t(", " + s.min() + "–" + s.max() + " at a time", ", " + s.min() + "–" + s.max() + " auf einmal");
    }

    private static String bold(String s) {
        return "**" + s + "**";
    }

    private static String cap(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String link(String text, String to, String from) {
        return link(text, to, from, true);
    }

    private String link(String text, String to, String from, boolean bold) {
        if (to == null) return bold ? bold(text) : text;
        String l = "[" + text + "](" + relative(from, to) + ")";
        return bold ? "**" + l + "**" : l;
    }

    /** Relative link from page {@code from} to page {@code to} (both guide-root relative). */
    public static String relative(String from, String to) {
        if (from.equals(to)) return to.substring(to.lastIndexOf('/') + 1);
        String[] f = from.split("/"), tt = to.split("/");
        int common = 0;
        while (common < f.length - 1 && common < tt.length - 1 && f[common].equals(tt[common])) common++;
        StringBuilder b = new StringBuilder();
        for (int i = common; i < f.length - 1; i++) b.append("../");
        for (int i = common; i < tt.length; i++) {
            b.append(tt[i]);
            if (i < tt.length - 1) b.append('/');
        }
        return b.toString();
    }

    /** A markdown table; without a header the first column is the label (two-column profile). */
    private static final class Table {
        private final String[] header;
        private final List<String[]> rows = new ArrayList<>();

        Table(String... header) {
            this.header = header;
        }

        void row(String... cells) {
            rows.add(cells);
        }

        String render() {
            StringBuilder b = new StringBuilder();
            int n = header.length > 0 ? header.length : 2;
            String[] h = header.length > 0 ? header : new String[]{"", ""};
            b.append('|');
            for (String c : h) b.append(' ').append(c).append(" |");
            b.append("\n|");
            for (int i = 0; i < n; i++) b.append("---|");
            b.append('\n');
            for (String[] r : rows) {
                b.append('|');
                for (String c : r) b.append(' ').append(c.replace("|", "\\|")).append(" |");
                b.append('\n');
            }
            String out = b.toString();
            while (out.contains("|  |")) out = out.replace("|  |", "| |");
            return out;
        }
    }

    // ================================================================== inputs

    private static Map<String, String> readLang(boolean german) {
        Path p = project().resolve(german ? "src/main/resources/assets/bettacraft/lang/de_de.json" : "src/generated/resources/assets/bettacraft/lang/en_us.json");
        try {
            JsonObject o = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, String> out = new LinkedHashMap<>();
            for (var e : o.entrySet()) out.put(e.getKey(), e.getValue().getAsString());
            return out;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** item id -> page that lists it in item_ids. */
    private static Map<String, String> itemPages(boolean german) {
        Map<String, String> out = new LinkedHashMap<>();
        Path root = german ? guide().resolve("_de_de") : guide();
        for (String page : pages()) {
            Path f = root.resolve(page);
            if (!Files.exists(f)) continue;
            try {
                String text = Files.readString(f, StandardCharsets.UTF_8).replace("\r\n", "\n");
                if (!text.startsWith("---")) continue;
                String fm = text.substring(3, Math.max(3, text.indexOf("\n---", 3)));
                var m = java.util.regex.Pattern.compile("(?m)^\\s+-\\s*([a-z0-9_]+:[a-z0-9_/]+)\\s*$").matcher(fm);
                while (m.find()) out.put(m.group(1), page);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return out;
    }

    /** All English page paths (guide-root relative). */
    public static List<String> pages() {
        List<String> out = new ArrayList<>();
        try (Stream<Path> s = Files.walk(guide())) {
            s.filter(p -> p.toString().endsWith(".md")).forEach(p -> {
                String rel = guide().relativize(p).toString().replace('\\', '/');
                if (!rel.startsWith("_")) out.add(rel);
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        out.sort(null);
        return out;
    }

    /** result item id -> recipe ids. */
    private static Map<String, List<String>> recipes() {
        Map<String, List<String>> out = new TreeMap<>();
        for (String dir : List.of("src/generated/resources/data/bettacraft/recipe", "src/main/resources/data/bettacraft/recipe")) {
            Path d = project().resolve(dir);
            if (!Files.isDirectory(d)) continue;
            try (Stream<Path> s = Files.walk(d)) {
                for (Path p : s.filter(f -> f.toString().endsWith(".json")).toList()) {
                    JsonObject o = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
                    // M26 part b: the food maker's batches are listed as text (GuideME shows no recipes of that type)
                    if (o.has("type") && "bettacraft:food_maker".equals(o.get("type").getAsString())) continue;
                    JsonElement r = o.get("result");
                    String id = r == null ? null : r.isJsonObject() ? (r.getAsJsonObject().has("id") ? r.getAsJsonObject().get("id").getAsString() : null)
                            : r.isJsonPrimitive() ? r.getAsString() : null; // M24: the Thermal recipe has a list of results
                    if (id != null) out.computeIfAbsent(id, k -> new ArrayList<>()).add(p.getFileName().toString());
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return out;
    }

    // ================================================================== reading and writing sections

    /** The generated sections that differ from the page (heading -> problem), or writes them when {@code write}. */
    public static List<String> apply(boolean german, boolean write) throws IOException {
        GuideGen g = new GuideGen(german);
        List<String> problems = new ArrayList<>();
        Path root = german ? guide().resolve("_de_de") : guide();
        for (var e : g.sections().entrySet()) {
            String page = e.getKey();
            if (page == null) continue;
            Path f = root.resolve(page);
            if (!Files.exists(f)) {
                problems.add((german ? "de: " : "en: ") + page + ": page missing (needed for " + e.getValue().keySet() + ")");
                continue;
            }
            String text = Files.readString(f, StandardCharsets.UTF_8).replace("\r\n", "\n");
            String updated = text;
            for (var s : e.getValue().entrySet()) updated = replaceSection(updated, s.getKey(), s.getValue());
            if (!updated.equals(text)) {
                if (write) Files.writeString(f, updated, StandardCharsets.UTF_8);
                else problems.add((german ? "de: " : "en: ") + page + ": generated sections out of date " + changed(text, updated, e.getValue().keySet()));
            }
        }
        return problems;
    }

    private static List<String> changed(String a, String b, Collection<String> headings) {
        List<String> out = new ArrayList<>();
        for (String h : headings) if (!section(a, h).equals(section(b, h))) out.add(h);
        return out;
    }

    /** The body of a section (null-safe, "" when missing). */
    public static String section(String text, String heading) {
        int i = find(text, heading);
        if (i < 0) return "";
        int start = text.indexOf('\n', i);
        if (start < 0) return "";
        int end = text.indexOf("\n## ", start);
        return text.substring(start + 1, end < 0 ? text.length() : end + 1);
    }

    private static int find(String text, String heading) {
        int i = text.startsWith(heading + "\n") ? 0 : text.indexOf("\n" + heading + "\n");
        return i <= 0 ? i : i + 1;
    }

    /** Replaces the body of the section, or appends the section at the end of the page. */
    public static String replaceSection(String text, String heading, String body) {
        String b = "\n" + body.strip() + "\n\n";
        int i = find(text, heading);
        if (i < 0) {
            String base = text.endsWith("\n") ? text : text + "\n";
            return base + "\n" + heading + "\n" + b.substring(0, b.length() - 1);
        }
        int start = text.indexOf('\n', i);
        int end = text.indexOf("\n## ", start);
        String rest = end < 0 ? "" : text.substring(end + 1);
        String out = text.substring(0, start + 1) + b + rest;
        if (end < 0) out = out.substring(0, out.length() - 1);
        return out;
    }
}
