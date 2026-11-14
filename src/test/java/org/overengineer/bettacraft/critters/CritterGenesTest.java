package org.overengineer.bettacraft.critters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** M14: Mendel colours of the small animals, the grade, the fall back to the wild form, pink ramshorns bred pure. */
class CritterGenesTest {
    private static final int RED = 0, BLUE = 1, YELLOW = 2, BLACK = 5;

    @Test
    void colourIsRecessiveAndShowsOnlyTwice() {
        CritterSpecies n = CritterSpecies.NEOCARIDINA;
        assertEquals("wild", CritterGenes.WILD.variety(n));
        assertEquals("wild", new CritterGenes(1, 0, 0.2F).variety(n), "a carrier looks wild");
        assertEquals("red_cherry", CritterGenes.pure(RED, 0.1F).variety(n));
        assertEquals("sakura", CritterGenes.pure(RED, 0.4F).variety(n));
        assertEquals("fire_red", CritterGenes.pure(RED, 0.7F).variety(n));
        assertEquals("painted_fire_red", CritterGenes.pure(RED, 0.9F).variety(n));
        assertEquals("blue_dream", CritterGenes.pure(BLUE, 0.5F).variety(n));
        assertEquals("black_rose", CritterGenes.pure(BLACK, 0.5F).variety(n));
        int choc = (1 << YELLOW) | (1 << BLACK);
        assertEquals("chocolate", new CritterGenes(choc, choc, 0.5F).variety(n), "black + yellow = chocolate");
        int rb = (1 << RED) | (1 << BLUE);
        assertEquals("wild", new CritterGenes(rb, rb, 0.5F).variety(n), "two other colours at once look wild");
        assertEquals("orange", CritterGenes.pure(0, 0.5F).variety(CritterSpecies.DWARF_CRAYFISH));
        assertEquals("pink", CritterGenes.pure(0, 0.5F).variety(CritterSpecies.RAMSHORN));
        assertEquals("wild", CritterGenes.WILD.variety(CritterSpecies.ZEBRA_NERITE));
        assertEquals("variety.bettacraft.neocaridina.sakura", CritterGenes.pure(RED, 0.4F).varietyKey(n));
    }

    /** M25 part c: the Caridina lines, the Taiwan bee factor and the banding grades. */
    @Test
    void caridinaLinesAndBandingGrades() {
        CritterSpecies c = CritterSpecies.CARIDINA;
        assertEquals("wild", CritterGenes.WILD.variety(c));
        assertEquals("crystal_red", CritterGenes.pure(CritterGenes.CARIDINA_RED, 0.2F).variety(c));
        assertEquals("crystal_red_s", CritterGenes.pure(CritterGenes.CARIDINA_RED, 0.5F).variety(c));
        assertEquals("crystal_red_ss", CritterGenes.pure(CritterGenes.CARIDINA_RED, 0.7F).variety(c));
        assertEquals("crystal_red_sss", CritterGenes.pure(CritterGenes.CARIDINA_RED, 0.9F).variety(c));
        assertEquals("crystal_black_ss", CritterGenes.pure(CritterGenes.CARIDINA_BLACK, 0.65F).variety(c));
        assertEquals("blue_bolt", CritterGenes.pure(CritterGenes.CARIDINA_TAIWAN, 0.5F).variety(c));
        assertEquals("orange_eye_blue_tiger", CritterGenes.pure(CritterGenes.CARIDINA_TIGER, 0.5F).variety(c));
        int wine = (1 << CritterGenes.CARIDINA_TAIWAN) | (1 << CritterGenes.CARIDINA_RED);
        assertEquals("wine_red", new CritterGenes(wine, wine, 0.5F).variety(c), "Taiwan bee on the red line");
        assertEquals(CritterGenes.WINE_RED, new CritterGenes(wine, wine, 0.5F).color(c));
        int bolt = (1 << CritterGenes.CARIDINA_TAIWAN) | (1 << CritterGenes.CARIDINA_BLACK);
        assertEquals("blue_bolt", new CritterGenes(bolt, bolt, 0.5F).variety(c), "Taiwan bee on the black line");
        int tibee = (1 << CritterGenes.CARIDINA_TIGER) | (1 << CritterGenes.CARIDINA_RED);
        assertEquals("wild", new CritterGenes(tibee, tibee, 0.5F).variety(c), "tiger x bee falls back");
        // the client only knows colour and grade
        assertEquals("wine_red", CritterGenes.varietyOf(c, CritterGenes.WINE_RED, 0.3F));
        assertEquals("crystal_black_sss", CritterGenes.varietyOf(c, CritterGenes.CARIDINA_BLACK, 0.95F));
        assertEquals(3, CritterGenes.banding(0.9F));
        // the Neocaridina keep their names
        assertEquals("chocolate", CritterGenes.varietyOf(CritterSpecies.NEOCARIDINA, CritterGenes.CHOCOLATE, 0.5F));
        assertEquals("wild", CritterGenes.varietyOf(CritterSpecies.NEOCARIDINA, CritterGenes.WINE_RED, 0.5F));
    }

    @Test
    void mixedColoursFallBackToTheWildFormAndComeBackInF2() {
        Random rnd = new Random(1);
        CritterSpecies n = CritterSpecies.NEOCARIDINA;
        CritterGenes red = CritterGenes.pure(RED, 0.5F), blue = CritterGenes.pure(BLUE, 0.5F);
        List<CritterGenes> f1 = new ArrayList<>();
        for (int i = 0; i < 200; i++) f1.add(CritterGenes.cross(red, blue, 6, rnd));
        for (CritterGenes g : f1) {
            assertEquals("wild", g.variety(n), "F1 of red x blue is wild-coloured");
            assertTrue(g.carries(RED) && g.carries(BLUE), "and carries both colours");
        }
        int redF2 = 0, total = 4000;
        for (int i = 0; i < total; i++) {
            CritterGenes g = CritterGenes.cross(f1.get(rnd.nextInt(f1.size())), f1.get(rnd.nextInt(f1.size())), 6, rnd);
            if (g.shows(RED) && !g.shows(BLUE)) redF2++;
        }
        // red/red and not blue/blue: 1/4 * 3/4 = 3/16
        assertEquals(3.0 / 16, redF2 / (double) total, 0.025);
    }

    @Test
    void gradeIsBredUpBySelection() {
        Random rnd = new Random(7);
        List<CritterGenes> line = new ArrayList<>();
        for (int i = 0; i < 20; i++) line.add(CritterGenes.pure(RED, 0.2F + rnd.nextFloat() * 0.1F));
        float start = mean(line);
        for (int gen = 0; gen < 25; gen++) {
            line.sort((a, b) -> Float.compare(b.grade(), a.grade()));
            List<CritterGenes> parents = line.subList(0, 5);
            List<CritterGenes> next = new ArrayList<>();
            for (int i = 0; i < 20; i++) next.add(CritterGenes.cross(parents.get(rnd.nextInt(5)), parents.get(rnd.nextInt(5)), 6, rnd));
            line = next;
        }
        float end = mean(line);
        assertTrue(end > start + 0.35F, "grade bred up from " + start + " to " + end);
        assertEquals("painted_fire_red", best(line).variety(CritterSpecies.NEOCARIDINA));
        // without selection it drifts back to the average
        List<CritterGenes> random = new ArrayList<>();
        for (int i = 0; i < 40; i++) random.add(CritterGenes.pure(RED, 0.9F));
        for (int gen = 0; gen < 40; gen++) {
            List<CritterGenes> next = new ArrayList<>();
            for (int i = 0; i < 40; i++) next.add(CritterGenes.cross(random.get(rnd.nextInt(40)), random.get(rnd.nextInt(40)), 6, rnd));
            random = next;
        }
        assertTrue(mean(random) < 0.6F, "unselected line drifts down: " + mean(random));
    }

    @Test
    void pinkRamshornsFromWildCarriers() {
        Random rnd = new Random(3);
        CritterSpecies r = CritterSpecies.RAMSHORN;
        // wild red-brown snails, two of them carry pink
        CritterGenes a = new CritterGenes(1, 0, 0.1F), b = new CritterGenes(0, 1, 0.1F);
        assertEquals("wild", a.variety(r));
        int pink = 0;
        List<CritterGenes> pinks = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            CritterGenes g = CritterGenes.cross(a, b, 2, rnd);
            if ("pink".equals(g.variety(r))) {
                pink++;
                pinks.add(g);
            }
        }
        assertEquals(0.25, pink / 400.0, 0.06, "a quarter of the young is pink");
        // pink x pink breeds true
        for (int i = 0; i < 100; i++) {
            assertEquals("pink", CritterGenes.cross(pinks.get(rnd.nextInt(pinks.size())), pinks.get(rnd.nextInt(pinks.size())), 2, rnd).variety(r));
        }
    }

    @Test
    void colourDensityAndSaveLoad() {
        CritterSpecies n = CritterSpecies.NEOCARIDINA;
        int low = CritterGenes.pure(RED, 0.0F).rgb(n), high = CritterGenes.pure(RED, 1.0F).rgb(n);
        assertTrue(((high >> 16) & 0xFF) - ((high >> 8) & 0xFF) > ((low >> 16) & 0xFF) - ((low >> 8) & 0xFF), "higher grade = redder");
        assertEquals(CritterGenes.fullColor(n, RED), high);
        CritterGenes g = new CritterGenes(5, 3, 0.42F);
        assertEquals(g, CritterGenes.load(g.save()));
        assertEquals("red/red +/blue yellow/+", g.genotypeText(n));
        // out-of-range colour (client data) is the wild colour
        assertEquals(n.wildColor(), CritterGenes.fullColor(n, 77));
    }

    private static float mean(List<CritterGenes> l) {
        float s = 0;
        for (CritterGenes g : l) s += g.grade();
        return s / l.size();
    }

    private static CritterGenes best(List<CritterGenes> l) {
        CritterGenes b = l.getFirst();
        for (CritterGenes g : l) if (g.grade() > b.grade()) b = g;
        return b;
    }
}
