package org.overengineer.bettacraft.genetics.livebearer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;

/**
 * M21.6: the named guppy and endler strains (like the named betta varieties). A fish belongs to the first strain whose
 * conditions its phenotype meets (most specific first). Strains with a Y pattern can only be males (a female has no
 * Y); the others name females too. Each strain has an example genome (pure-bred, homozygous: what the trader sells and
 * the showcase shows), the old fixed variety it is closest to ({@link #legacy}, for {@code features.tankmates.livebearerGenetics}
 * off) and whether the trader or the net bring it.
 */
public enum LivebearerStrain {
    // ---------------------------------------------------------------- guppies (most specific first)
    TUXEDO_KOI(false, "Tuxedo Koi", "Tuxedo Koi", "tuxedo",
            c().is("x", "tuxedo").is("y", "red_head").is("tail_color", "red"),
            g -> g.pure(LbLocus.TAIL, "delta").pure(LbLocus.TAIL_COLOR, "red").pure(LbLocus.TAIL_PATTERN, "none").pure(LbLocus.BLOND, "blond")
                    .pure(LbLocus.BLUE, "blue").x("tuxedo").y("red_head"), Source.TRADER),
    ALBINO_FULL_RED(false, "Albino Full Red", "Albino Full Red", "red",
            c().is("body", "albino").is("y", "red_tail").is("tail_color", "red"),
            g -> g.pure(LbLocus.TAIL, "delta").pure(LbLocus.TAIL_COLOR, "red").pure(LbLocus.TAIL_PATTERN, "none").pure(LbLocus.ALBINO, "albino")
                    .x("none").y("red_tail"), Source.TRADER),
    RED_COBRA(false, "Red Cobra", "Red Cobra", "green_cobra",
            c().pattern("cobra").is("tail_color", "red"),
            g -> g.pure(LbLocus.TAIL, "delta").pure(LbLocus.TAIL_COLOR, "red").pure(LbLocus.TAIL_PATTERN, "leopard").x("none").y("cobra"), Source.TRADER),
    GREEN_COBRA(false, "Green Cobra", "Green Cobra", "green_cobra",
            c().pattern("cobra").is("tail_color", "green"),
            g -> g.pure(LbLocus.TAIL, "delta").pure(LbLocus.TAIL_COLOR, "green").pure(LbLocus.TAIL_PATTERN, "leopard").x("none").y("cobra"), Source.TRADER),
    YELLOW_COBRA(false, "Yellow Cobra", "Yellow Cobra", "green_cobra",
            c().pattern("cobra").is("tail_color", "yellow"),
            g -> g.pure(LbLocus.TAIL, "triangle").pure(LbLocus.TAIL_COLOR, "yellow").pure(LbLocus.TAIL_PATTERN, "leopard").x("none").y("cobra"), Source.NONE),
    BLUE_MOSCOW(false, "Blue Moscow", "Blue Moskau", "wild",
            c().pattern("moscow").is("tail_color", "blue"),
            g -> g.pure(LbLocus.TAIL, "delta").pure(LbLocus.TAIL_COLOR, "blue").pure(LbLocus.TAIL_PATTERN, "none").pure(LbLocus.BLUE, "blue")
                    .x("moscow").y("moscow"), Source.TRADER),
    GREEN_MOSCOW(false, "Green Moscow", "Grün Moskau", "wild",
            c().pattern("moscow").is("tail_color", "green"),
            g -> g.pure(LbLocus.TAIL, "delta").pure(LbLocus.TAIL_COLOR, "green").pure(LbLocus.TAIL_PATTERN, "none").x("moscow").y("moscow"), Source.NONE),
    BLACK_MOSCOW(false, "Black Moscow", "Schwarz Moskau", "wild",
            c().pattern("moscow").is("tail_color", "black"),
            g -> g.pure(LbLocus.TAIL, "delta").pure(LbLocus.TAIL_COLOR, "black").pure(LbLocus.TAIL_PATTERN, "none").pure(LbLocus.DARK, "dark")
                    .x("moscow").y("moscow"), Source.TRADER),
    TUXEDO(false, "Half Black Red (Tuxedo)", "Halbschwarz Rot (Tuxedo)", "tuxedo",
            c().pattern("tuxedo").is("tail_color", "red"),
            g -> g.pure(LbLocus.TAIL, "delta").pure(LbLocus.TAIL_COLOR, "red").pure(LbLocus.TAIL_PATTERN, "none").x("tuxedo").y("tuxedo"), Source.TRADER),
    TUXEDO_BLUE(false, "Half Black Blue", "Halbschwarz Blau", "tuxedo",
            c().pattern("tuxedo").is("tail_color", "blue"),
            g -> g.pure(LbLocus.TAIL, "triangle").pure(LbLocus.TAIL_COLOR, "blue").pure(LbLocus.TAIL_PATTERN, "none").x("tuxedo").y("tuxedo"), Source.NONE),
    JAPAN_BLUE(false, "Japan Blue", "Japan Blue", "wild",
            c().is("y", "japan_blue"),
            g -> g.pure(LbLocus.TAIL, "double_sword").pure(LbLocus.TAIL_COLOR, "blue").pure(LbLocus.TAIL_PATTERN, "spots").x("none").y("japan_blue"),
            Source.TRADER),
    FULL_RED(false, "Full Red", "Full Red", "red",
            c().is("y", "red_tail").is("tail_color", "red"),
            g -> g.pure(LbLocus.TAIL, "delta").pure(LbLocus.TAIL_COLOR, "red").pure(LbLocus.TAIL_PATTERN, "none").x("none").y("red_tail"), Source.TRADER),
    RED_MOSAIC(false, "Red Mosaic", "Rot Mosaik", "red",
            c().is("tail_pattern", "mosaic").is("tail_color", "red"),
            g -> g.pure(LbLocus.TAIL, "delta").pure(LbLocus.TAIL_COLOR, "red").pure(LbLocus.TAIL_PATTERN, "mosaic").x("none").y("none"), Source.TRADER),
    BLUE_MOSAIC(false, "Blue Mosaic", "Blau Mosaik", "blue_grass",
            c().is("tail_pattern", "mosaic").is("tail_color", "blue"),
            g -> g.pure(LbLocus.TAIL, "delta").pure(LbLocus.TAIL_COLOR, "blue").pure(LbLocus.TAIL_PATTERN, "mosaic").pure(LbLocus.BLUE, "blue")
                    .x("none").y("none"), Source.NONE),
    BLUE_GRASS(false, "Blue Grass", "Blue Grass", "blue_grass",
            c().is("tail_pattern", "grass").is("tail_color", "blue"),
            g -> g.pure(LbLocus.TAIL, "delta").pure(LbLocus.TAIL_COLOR, "blue").pure(LbLocus.TAIL_PATTERN, "grass").pure(LbLocus.BLUE, "blue")
                    .x("none").y("none"), Source.TRADER),
    YELLOW_GRASS(false, "Yellow Grass", "Gelb Grass", "wild",
            c().is("tail_pattern", "grass").is("tail_color", "yellow"),
            g -> g.pure(LbLocus.TAIL, "delta").pure(LbLocus.TAIL_COLOR, "yellow").pure(LbLocus.TAIL_PATTERN, "grass").x("none").y("none"), Source.NONE),
    LEOPARD(false, "Leopard", "Leopard", "wild",
            c().is("tail_pattern", "leopard").not("tail_color", "red", "green", "yellow"),
            g -> g.pure(LbLocus.TAIL, "delta").pure(LbLocus.TAIL_COLOR, "clear").pure(LbLocus.TAIL_PATTERN, "leopard").x("none").y("none"), Source.NONE),
    FULL_GOLD(false, "Full Gold", "Full Gold", "wild",
            c().is("body", "gold").is("tail_color", "yellow", "orange").is("tail_pattern", "none"),
            g -> g.pure(LbLocus.TAIL, "delta").pure(LbLocus.TAIL_COLOR, "yellow").pure(LbLocus.TAIL_PATTERN, "none").pure(LbLocus.GOLD, "gold")
                    .x("none").y("none"), Source.TRADER),
    PLATINUM_WHITE(false, "Platinum White", "Platin Weiß", "wild",
            c().is("body", "white", "albino").is("tail_color", "white"),
            g -> g.pure(LbLocus.TAIL, "delta").pure(LbLocus.TAIL_COLOR, "white").pure(LbLocus.TAIL_PATTERN, "none").pure(LbLocus.BLOND, "blond")
                    .pure(LbLocus.BLUE, "blue").x("none").y("none"), Source.NONE),
    FULL_BLACK(false, "Full Black", "Full Black", "wild",
            c().is("body", "dark").is("tail_color", "black"),
            g -> g.pure(LbLocus.TAIL, "delta").pure(LbLocus.TAIL_COLOR, "black").pure(LbLocus.TAIL_PATTERN, "none").pure(LbLocus.DARK, "dark")
                    .x("none").y("none"), Source.NONE),
    RED_DOUBLE_SWORD(false, "Red Double Sword", "Rot Doppelschwert", "red",
            c().is("tail", "double_sword").is("tail_color", "red"),
            g -> g.pure(LbLocus.TAIL, "double_sword").pure(LbLocus.TAIL_COLOR, "red").pure(LbLocus.TAIL_PATTERN, "none").x("none").y("none"), Source.NONE),
    LYRETAIL(false, "Lyretail", "Lyratail", "wild",
            c().is("tail", "lyre").not("tail_color", "clear"),
            g -> g.pure(LbLocus.TAIL, "lyre").pure(LbLocus.TAIL_COLOR, "yellow").pure(LbLocus.TAIL_PATTERN, "none").x("none").y("none"), Source.NONE),
    WILD_GUPPY(false, "Wild Guppy", "Wildguppy", "wild",
            c().is("body", "wild").is("y", "wild", "none").is("tail_color", "clear"),
            g -> g.pure(LbLocus.TAIL, "round").pure(LbLocus.TAIL_COLOR, "clear").pure(LbLocus.TAIL_PATTERN, "spots").x("none").y("wild"), Source.NET),
    // ---------------------------------------------------------------- endlers (their Y patterns name them)
    ENDLER_TIGER(true, "Endler Tiger", "Endler Tiger", "tiger",
            c().is("y", "tiger"),
            g -> g.pure(LbLocus.TAIL, "double_sword").y("tiger"), Source.TRADER),
    ENDLER_BLACK_BAR(true, "Endler Black Bar", "Endler Black Bar", "black_bar",
            c().is("y", "black_bar"),
            g -> g.pure(LbLocus.TAIL, "double_sword").y("black_bar"), Source.TRADER),
    ENDLER_JAPAN_BLUE(true, "Endler Japan Blue", "Endler Japan Blue", "wild",
            c().is("y", "japan_blue"),
            g -> g.pure(LbLocus.TAIL, "top_sword").pure(LbLocus.TAIL_COLOR, "yellow").y("japan_blue"), Source.TRADER),
    ENDLER_YELLOW_TAIL(true, "Endler Yellow Tail", "Endler Gelbschwanz", "wild",
            c().is("y", "peacock").is("tail_color", "yellow"),
            g -> g.pure(LbLocus.TAIL, "double_sword").pure(LbLocus.TAIL_COLOR, "yellow").y("peacock"), Source.NET),
    ENDLER_WILD(true, "Endler Wild Type", "Endler Wildform", "wild",
            c().is("y", "peacock"),
            g -> g.pure(LbLocus.TAIL, "double_sword").pure(LbLocus.TAIL_COLOR, "orange").y("peacock"), Source.NET);

    /** Where a strain can be had besides breeding. */
    public enum Source { TRADER, NET, NONE }

    public final boolean endler;
    public final String en, de, legacy;
    private final Cond cond;
    private final UnaryOperator<LivebearerGenome.Builder> example;
    public final Source source;

    LivebearerStrain(boolean endler, String en, String de, String legacy, Cond cond, UnaryOperator<LivebearerGenome.Builder> example, Source source) {
        this.endler = endler;
        this.en = en;
        this.de = de;
        this.legacy = legacy;
        this.cond = cond;
        this.example = example;
        this.source = source;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "strain.bettacraft." + id();
    }

    /** Needs a Y pattern (only males can be of it). */
    public boolean yLinked() {
        return cond.yLinked();
    }

    /** The pure-bred example (homozygous; the same genome works for males and females). */
    public LivebearerGenome genome() {
        return example.apply(LivebearerGenome.builder(endler)).build();
    }

    public boolean matches(LbPhenotype p) {
        if (endler ? !p.endlerType() : !p.guppyType()) return false;
        return cond.test(p);
    }

    /** The strain of a fish, or null (unnamed, or a hybrid). */
    @Nullable
    public static LivebearerStrain of(LbPhenotype p) {
        if (p.hybrid()) return null;
        for (LivebearerStrain s : values()) if (s.matches(p)) return s;
        return null;
    }

    @Nullable
    public static LivebearerStrain byId(String id) {
        for (LivebearerStrain s : values()) if (s.id().equals(id)) return s;
        return null;
    }

    public static List<LivebearerStrain> of(boolean endler) {
        return java.util.Arrays.stream(values()).filter(s -> s.endler == endler).toList();
    }

    // ------------------------------------------------------------------ conditions

    private static Cond c() {
        return new Cond();
    }

    /** Allowed values per phenotype key ("pattern" = the X or the Y pattern), and forbidden ones. */
    private static final class Cond {
        private final Map<String, Set<String>> is = new LinkedHashMap<>(), not = new LinkedHashMap<>();

        Cond is(String key, String... v) {
            is.put(key, Set.of(v));
            return this;
        }

        Cond not(String key, String... v) {
            not.put(key, Set.of(v));
            return this;
        }

        Cond pattern(String p) {
            return is("pattern", p);
        }

        /** A Y pattern, or an X or Y pattern that females do not show (all but tuxedo). */
        boolean yLinked() {
            Set<String> y = is.get("y"), p = is.get("pattern");
            return y != null && !y.contains("none") || p != null && !p.contains("tuxedo");
        }

        boolean test(LbPhenotype p) {
            for (var e : is.entrySet()) {
                if (e.getKey().equals("pattern")) {
                    boolean any = false;
                    for (String v : e.getValue()) any |= p.hasPattern(v) && (!p.female() || v.equals("tuxedo"));
                    if (!any) return false;
                } else if (!e.getValue().contains(p.value(e.getKey()))) {
                    return false;
                }
            }
            for (var e : not.entrySet()) if (e.getValue().contains(p.value(e.getKey()))) return false;
            return true;
        }
    }
}
