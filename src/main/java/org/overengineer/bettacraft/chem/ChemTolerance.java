package org.overengineer.bettacraft.chem;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M25: the pH and TDS each animal lives in. Every range holds tap water (pH 7.5, TDS 250), so the tanks of earlier
 * versions stay fine. Bettas and most tankmates are tolerant; the soft-water fish of the Amazon and of South East Asia
 * also take very soft, acidic water, the livebearers and the snails want medium to hard water (minerals for the shells),
 * the Neocaridina are tolerant. Salt water animals only care about the pH (the alkalinity holds it).
 * <p>
 * Out of the range ({@link Range#distance}, pH units plus 100 ppm TDS per unit): below {@link #BAD_FROM} the animals are
 * only stressed (shown in the menu, critters breed slower), from it the water counts as bad for them (sick, in the end
 * dead, like bad water quality).
 * <p>
 * Part c of M25 adds the Caridina ({@link #CARIDINA}) and Sulawesi animals ({@link #SULAWESI}) to {@link #critter}.
 */
public final class ChemTolerance {
    private ChemTolerance() {}

    /** From this distance out of the range the water counts as bad for the animal. */
    public static final double BAD_FROM = 0.5;

    /** A range of pH and TDS (ppm, inclusive). {@code marine}: only the pH counts. */
    public record Range(float phMin, float phMax, float tdsMin, float tdsMax, boolean marine) {
        public static final Range TOLERANT = fresh(6.0, 8.4, 0, 700);
        public static final Range MARINE = new Range(7.8F, 8.6F, 0, 0, true);

        public static Range fresh(double phMin, double phMax, double tdsMin, double tdsMax) {
            return new Range((float) phMin, (float) phMax, (float) tdsMin, (float) tdsMax, false);
        }

        /** pH units outside of the range (0 inside). */
        public double phDistance(double ph) {
            return ph < phMin ? phMin - ph : ph > phMax ? ph - phMax : 0;
        }

        /** ppm outside of the range (0 inside; always 0 for salt water animals). */
        public double tdsDistance(double tds) {
            if (marine) return 0;
            return tds < tdsMin ? tdsMin - tds : tds > tdsMax ? tds - tdsMax : 0;
        }

        /** How far out of the range, in "pH units" (100 ppm TDS count as one). */
        public double distance(double ph, double tds) {
            return phDistance(ph) + tdsDistance(tds) / 100.0;
        }

        public boolean contains(double ph, double tds) {
            return distance(ph, tds) <= 1e-6;
        }

        /** "pH 6.0–8.0 · TDS 0–600 ppm" (salt water: only the pH). */
        public String describe() {
            String ph = String.format(Locale.ROOT, "pH %.1f–%.1f", phMin, phMax);
            return marine ? ph : ph + String.format(Locale.ROOT, " · TDS %d–%d ppm", Math.round(tdsMin), Math.round(tdsMax));
        }
    }

    /** What the water does to an animal. */
    public enum Fit { OK, STRESS, BAD }

    public static Fit fit(double distance) {
        return distance <= 1e-6 ? Fit.OK : distance < BAD_FROM ? Fit.STRESS : Fit.BAD;
    }

    public static final Range BETTA = Range.fresh(6.0, 8.0, 0, 600);
    /** Caridina (bee shrimp, Taiwan bees; part c): soft, acidic water (pH 5.5-6.8, TDS 100-200). */
    public static final Range CARIDINA = Range.fresh(5.5, 6.8, 100, 200);
    /** Sulawesi shrimp and Tylomelania (part c): hard, warm, alkaline water of the ancient lakes. */
    public static final Range SULAWESI = Range.fresh(7.5, 8.6, 150, 350);
    /** Soft-water fish (tetras, rasboras, kuhli loaches): they take very soft, acidic water. */
    public static final Range SOFT = Range.fresh(5.0, 7.8, 0, 450);
    /** Livebearers: medium to hard water. */
    public static final Range LIVEBEARER = Range.fresh(6.8, 8.6, 100, 800);
    /** Dwarf gourami (a labyrinth fish like the bettas). */
    public static final Range GOURAMI = Range.fresh(6.0, 8.0, 0, 500);

    /** M25 part b: how much CO2 an animal with this range bears (the labyrinth fish breathe air: more). */
    public static double co2Tolerance(Range r) {
        return r == BETTA || r == GOURAMI ? org.overengineer.bettacraft.co2.TankCo2.LABYRINTH : 1.0;
    }

    /** M29 part b: brackish fish (green spotted puffer): hard, alkaline water; only the pH counts (like the marine animals). */
    public static final Range BRACKISH = new Range(7.2F, 8.6F, 0, 0, true);

    /** Snails: medium to hard water for the shells. */
    public static final Range SNAIL = Range.fresh(6.8, 8.6, 100, 700);

    public static Range critter(CritterSpecies s) {
        if (s.marine()) return Range.MARINE;
        return switch (s) {
            case NEOCARIDINA -> Range.fresh(6.2, 8.2, 80, 500);
            case CARIDINA -> CARIDINA; // M25 part c: soft water only
            case SULAWESI_SHRIMP, TYLOMELANIA -> SULAWESI;
            case DWARF_CRAYFISH -> Range.fresh(6.5, 8.5, 100, 600);
            case RAMSHORN, BLADDER_SNAIL, TRUMPET_SNAIL -> Range.fresh(6.2, 8.6, 50, 800);
            default -> s.isSnail() ? SNAIL : Range.TOLERANT;
        };
    }

    public static Range tankmate(TankmateSpecies s) {
        if (s.salinity().marine()) return Range.MARINE;
        return switch (s) {
            case NEON_TETRA, CARDINAL_TETRA, GREEN_NEON_TETRA, EMBER_TETRA, GLOWLIGHT_TETRA, HARLEQUIN_RASBORA, MOSQUITO_RASBORA,
                    KUHLI_LOACH -> SOFT;
            case PYGMY_CORY, PANDA_CORY, STERBAI_CORY, GREEN_GOLD_CORY, OTOCINCLUS -> Range.fresh(5.8, 8.0, 0, 500);
            case LEOPARD_FROG_PLECO, ZEBRA_PLECO, BRISTLENOSE_PLECO, CLOWN_LOACH -> Range.fresh(5.8, 8.0, 0, 550);
            case GUPPY, ENDLER_GUPPY, PLATY -> LIVEBEARER;
            case DWARF_GOURAMI -> GOURAMI;
            // M29 part a: the cichlids of soft South American water (the Bolivian ram and the cockatoo cichlid are hardier)
            case RAM_CICHLID -> Range.fresh(5.0, 7.5, 0, 300);
            case BOLIVIAN_RAM -> Range.fresh(6.0, 8.0, 0, 450);
            case COCKATOO_CICHLID -> Range.fresh(5.5, 8.0, 0, 450);
            case AGASSIZ_DWARF_CICHLID -> Range.fresh(5.0, 7.2, 0, 250);
            case ANGELFISH -> Range.fresh(6.0, 8.0, 0, 500);
            case DISCUS -> Range.fresh(5.0, 7.0, 0, 200);
            // M29 part b: puffers, barbs, loaches (the brackish green spotted puffer: only the pH counts, the salt drives the TDS)
            case GREEN_SPOTTED_PUFFER -> BRACKISH;
            case DWARF_PUFFER -> Range.fresh(6.5, 8.2, 50, 600);
            case RED_EYE_PUFFER -> Range.fresh(5.0, 7.6, 0, 400);
            case CHERRY_BARB -> Range.fresh(6.0, 8.0, 0, 500);
            case DWARF_CHAIN_LOACH, RETICULATED_LOACH -> Range.fresh(6.0, 8.0, 0, 550);
            case GARRA_RUFA -> Range.fresh(6.5, 8.6, 50, 800);
            // M29 part c (the chocolate gourami of the peat swamps: very soft, acid water only)
            case RUMMY_NOSE_TETRA, BLACK_PHANTOM_TETRA, EMPEROR_TETRA, CELESTIAL_PEARL_DANIO -> SOFT;
            case FORKTAIL_RAINBOW, NEON_DWARF_RAINBOW -> Range.fresh(6.5, 8.5, 50, 700);
            case HONEY_GOURAMI, PEARL_GOURAMI -> GOURAMI;
            case CHOCOLATE_GOURAMI -> Range.fresh(4.5, 7.0, 0, 200);
            case BLACK_MOLLY, SWORDTAIL -> LIVEBEARER;
            // M30 part b: the Amazon giants want soft to medium water, the stingray soft, clean water; the wels of European
            // rivers is hardy and likes it neutral to alkaline
            case REDTAIL_CATFISH -> Range.fresh(5.5, 7.8, 0, 500);
            case ARAPAIMA -> Range.fresh(5.5, 7.8, 0, 450);
            case MOTORO_STINGRAY -> Range.fresh(5.5, 7.5, 0, 300);
            case WELS_CATFISH -> Range.fresh(6.5, 8.5, 50, 800);
            default -> Range.TOLERANT;
        };
    }

    /** The range of an animal of ours (bettas, tankmates), or null (frogs and animals of other mods: no chemistry). */
    @Nullable
    public static Range of(Entity e) {
        if (e instanceof org.overengineer.bettacraft.entity.betta.BettaEntity) return BETTA;
        if (e instanceof org.overengineer.bettacraft.entity.tankmate.TankmateEntity m) return tankmate(m.species());
        return null;
    }
}
