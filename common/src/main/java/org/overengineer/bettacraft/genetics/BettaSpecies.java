package org.overengineer.bettacraft.genetics;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

/**
 * M29 part c: the species of the Betta splendens complex. They all live in the same entity and the same genetics: a betta's
 * {@link Ancestry} says how much of each species it carries, its look comes from the ordinary loci (the wild species start
 * with their own wild alleles, see {@link #randomWild}), so crosses between them are ordinary Mendelian crosses and the
 * hybrids are fertile (as in nature: B. imbellis and B. smaragdina cross freely with B. splendens).
 * <p>
 * The ancestry is what the loci cannot show: the size of the fish, how hard males fight and the name (pure species from
 * 7/8 on, a hybrid below). Pure logic (unit-tested without a Minecraft bootstrap).
 */
public enum BettaSpecies {
    /** The domestic and the wild fighting fish of Thailand: 6 cm, males fight to the death. */
    SPLENDENS("betta", "Betta splendens", 1.0F, 1.0),
    /** The "peaceful betta" of the peat swamps of the Malay peninsula: 5 cm, dark with blue-green scales, a red crescent in the tail. */
    IMBELLIS("imbellis", "Betta imbellis", 0.85F, 0.25),
    /** The emerald betta of north-eastern Thailand and Laos: 6.5 cm, green scales all over. */
    SMARAGDINA("smaragdina", "Betta smaragdina", 1.08F, 0.6);

    /** Species id of a hybrid (several species, none from 7/8 on). */
    public static final String HYBRID_ID = "bettacraft:betta_hybrid";
    /** Share of the ancestry (in 64ths) from which a fish counts as a pure species. */
    public static final int PURE = 56;

    private final String key, latin;
    private final float scale;
    private final double aggression;

    BettaSpecies(String key, String latin, float scale, double aggression) {
        this.key = key;
        this.latin = latin;
        this.scale = scale;
        this.aggression = aggression;
    }

    /** Short key (betta, imbellis, smaragdina): lang keys, handbook targets. */
    public String key() { return key; }

    public String latin() { return latin; }

    /** Genotype species id: {@code bettacraft:betta} (the old id of every betta) and {@code bettacraft:betta_<key>}. */
    public String id() {
        return this == SPLENDENS ? BettaGenome.SPECIES_BETTA : "bettacraft:betta_" + key;
    }

    /** Body size relative to Betta splendens (renderer). */
    public float scale() { return scale; }

    /** How hard the males fight (Betta splendens = 1). */
    public double aggression() { return aggression; }

    public String translationKey() {
        return "betta.bettacraft.species." + key;
    }

    @Nullable
    public static BettaSpecies byId(String id) {
        for (BettaSpecies s : values()) if (s.id().equals(id)) return s;
        return null;
    }

    @Nullable
    public static BettaSpecies byKey(String key) {
        for (BettaSpecies s : values()) if (s.key.equals(key)) return s;
        return null;
    }

    /** Render scale of a synced species id (hybrids in between). */
    public static float scaleOf(String speciesId) {
        BettaSpecies s = byId(speciesId);
        if (s != null) return s.scale;
        return HYBRID_ID.equals(speciesId) ? 0.95F : 1.0F;
    }

    // ------------------------------------------------------------------ wild fish

    /**
     * Wild type of the species: short-finned roundtails. Betta imbellis dark (smoke, black, navy) with a blue-green sheen and red
     * fin edges; Betta smaragdina bronze-olive with an emerald sheen all over, sometimes red-edged fins.
     */
    public static BettaGenome randomWildGenome(BettaSpecies s, RandomSource r) {
        return switch (s) {
            case SPLENDENS -> BettaGenome.randomWild(r);
            case IMBELLIS -> new BettaGenome(s.id(), "round", BettaGenome.SHORT, "normal", pick(r, List.of("smoke", "black", "navy")),
                    pick(r, List.of("navy", "steel", "black")), "tipped", "red", "high", pick(r, List.of("turquoise", "teal", "steel")));
            case SMARAGDINA -> new BettaGenome(s.id(), "round", BettaGenome.SHORT, "normal", pick(r, List.of("olive", "bronze", "chocolate")),
                    pick(r, List.of("teal", "emerald", "olive")), r.nextInt(3) == 0 ? "tipped" : "none", "red", "high",
                    pick(r, List.of("emerald", "turquoise", "teal")));
        };
    }

    private static String pick(RandomSource r, List<String> l) {
        return l.get(r.nextInt(l.size()));
    }

    // ------------------------------------------------------------------ ancestry

    /**
     * Shares of Betta imbellis and Betta smaragdina in 64ths (the rest is Betta splendens). A young fish gets the mean of its
     * parents (a back-cross to splendens halves the wild share each generation).
     */
    public record Ancestry(int imbellis, int smaragdina) {
        public static final Ancestry SPLENDENS = new Ancestry(0, 0);

        public Ancestry {
            imbellis = Math.max(0, Math.min(64, imbellis));
            smaragdina = Math.max(0, Math.min(64 - imbellis, smaragdina));
        }

        public static Ancestry pure(BettaSpecies s) {
            return switch (s) {
                case SPLENDENS -> SPLENDENS;
                case IMBELLIS -> new Ancestry(64, 0);
                case SMARAGDINA -> new Ancestry(0, 64);
            };
        }

        /** Ancestry of an old or foreign species id (unknown ids: Betta splendens). */
        public static Ancestry of(String speciesId) {
            BettaSpecies s = byId(speciesId);
            return s == null ? SPLENDENS : pure(s);
        }

        public static Ancestry mix(Ancestry mother, Ancestry father) {
            return new Ancestry((mother.imbellis + father.imbellis + 1) / 2, (mother.smaragdina + father.smaragdina) / 2);
        }

        public int splendens() {
            return 64 - imbellis - smaragdina;
        }

        public int share(BettaSpecies s) {
            return switch (s) {
                case SPLENDENS -> splendens();
                case IMBELLIS -> imbellis;
                case SMARAGDINA -> smaragdina;
            };
        }

        /** The species from 7/8 on, or null for a hybrid. */
        @Nullable
        public BettaSpecies species() {
            for (BettaSpecies s : BettaSpecies.values()) if (share(s) >= PURE) return s;
            return null;
        }

        public String speciesId() {
            BettaSpecies s = species();
            return s == null ? HYBRID_ID : s.id();
        }

        public boolean pureSplendens() {
            return imbellis == 0 && smaragdina == 0;
        }

        /** Fight strength of a male: the ancestry-weighted aggression of the species. */
        public double aggression() {
            double a = 0;
            for (BettaSpecies s : BettaSpecies.values()) a += s.aggression * share(s) / 64.0;
            return a;
        }

        public CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putInt("imbellis", imbellis);
            t.putInt("smaragdina", smaragdina);
            return t;
        }

        public static Ancestry load(CompoundTag t) {
            return new Ancestry(t.getIntOr("imbellis", 0), t.getIntOr("smaragdina", 0));
        }
    }
}
