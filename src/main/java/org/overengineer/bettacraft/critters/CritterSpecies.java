package org.overengineer.bettacraft.critters;

import java.util.Locale;
import javax.annotation.Nullable;

/**
 * The small tank animals of M14 that are data, not entities: dwarf shrimp, dwarf crayfish and snails. Every species
 * has a body plan for the renderer, a size, a walking speed, how it breeds, how long the young need to grow up, the
 * temperatures and the water it tolerates, how much it eats and how many algae it grazes, and its colour loci.
 * <p>
 * Sizes follow the bettas (an adult betta, 6 cm, is half a block long, so one block is about 12 cm).
 */
public enum CritterSpecies {
    // id, group, model, length, speed, glass, breeding, rate/day, egg days, brood, juvenile days, temp, bad water, food, algae, loci
    NEOCARIDINA("neocaridina", Group.SHRIMP, Model.SHRIMP, 0.2, 0.08, true, Breeding.PAIR_BERRIED, 0.35, 2.0, 3, 8, 3.0, 18, 28, 0.45, 0.03, 0.08,
            new String[]{"red", "blue", "yellow", "orange", "green", "black"}, 0x7A6A50),
    DWARF_CRAYFISH("dwarf_crayfish", Group.CRAYFISH, Model.CRAYFISH, 0.34, 0.06, false, Breeding.PAIR_BERRIED, 0.2, 3.0, 3, 6, 4.0, 15, 26, 0.4, 0.07, 0.03,
            new String[]{"orange"}, 0x6E6252),
    ZEBRA_NERITE("zebra_nerite", Group.SNAIL, Model.NERITE, 0.17, 0.025, true, Breeding.NONE, 0, 0, 0, 0, 3.0, 20, 28, 0.35, 0.02, 2.0,
            new String[0], 0xE2C23A),
    HORNED_NERITE("horned_nerite", Group.SNAIL, Model.NERITE, 0.13, 0.022, true, Breeding.NONE, 0, 0, 0, 0, 3.0, 20, 28, 0.35, 0.015, 1.6,
            new String[0], 0xD8B248),
    OLIVE_NERITE("olive_nerite", Group.SNAIL, Model.NERITE, 0.18, 0.024, true, Breeding.NONE, 0, 0, 0, 0, 3.0, 20, 28, 0.35, 0.02, 2.0,
            new String[0], 0x5E6A2E),
    RED_NERITE("red_nerite", Group.SNAIL, Model.NERITE, 0.16, 0.024, true, Breeding.NONE, 0, 0, 0, 0, 3.0, 20, 28, 0.35, 0.02, 2.0,
            new String[0], 0xC4421E),
    // M21.5: the yellow form of the racer nerite in the trade, its own entry (no breeding in fresh water, every one its own shade)
    YELLOW_NERITE("yellow_nerite", Group.SNAIL, Model.NERITE, 0.16, 0.024, true, Breeding.NONE, 0, 0, 0, 0, 3.0, 20, 28, 0.35, 0.02, 2.0,
            new String[0], 0xE4BE2C),
    APPLE_SNAIL("apple_snail", Group.SNAIL, Model.APPLE, 0.4, 0.03, true, Breeding.PAIR_CLUTCH, 0.15, 3.0, 5, 10, 4.0, 20, 30, 0.3, 0.08, 1.5,
            new String[]{"gold", "blue", "purple", "ivory"}, 0x6B5030),
    RAMSHORN("ramshorn", Group.SNAIL, Model.RAMSHORN, 0.12, 0.018, true, Breeding.SELF, 0.1, 0, 1, 2, 3.0, 15, 30, 0.3, 0.015, 0.6,
            new String[]{"pink", "blue", "red"}, 0x8A3A22), // M21.5: red (Red Ramshorn), stronger than the wild red-brown
    BLADDER_SNAIL("bladder_snail", Group.SNAIL, Model.BLADDER, 0.07, 0.02, true, Breeding.SELF, 0.1, 0, 1, 2, 2.0, 10, 30, 0.25, 0.008, 0.3,
            new String[0], 0xA08058),
    TRUMPET_SNAIL("trumpet_snail", Group.SNAIL, Model.TRUMPET, 0.17, 0.012, false, Breeding.SELF, 0.06, 0, 1, 2, 3.0, 18, 32, 0.3, 0.01, 0.1,
            new String[0], 0x6E5A40),
    ASSASSIN_SNAIL("assassin_snail", Group.SNAIL, Model.ASSASSIN, 0.17, 0.02, false, Breeding.PAIR, 0.05, 0, 1, 1, 6.0, 20, 30, 0.3, 0.01, 0.0,
            new String[0], 0xD0A030),
    // M20 reef animals (salt water; they do not breed in a tank): the cleaner shrimp picks parasites off the fish,
    // blue-leg hermit crabs and turbo snails are the clean-up crew of a reef tank (algae, leftovers)
    CLEANER_SHRIMP("cleaner_shrimp", Group.SHRIMP, Model.SHRIMP, 0.45, 0.07, true, Breeding.NONE, 0, 0, 0, 0, 3.0, 23, 28, 0.55, 0.05, 0.05,
            new String[0], 0xD8302A),
    HERMIT_CRAB("hermit_crab", Group.SNAIL, Model.HERMIT, 0.2, 0.045, true, Breeding.NONE, 0, 0, 0, 0, 3.0, 22, 28, 0.35, 0.03, 0.8,
            new String[0], 0x3A6AD0),
    TURBO_SNAIL("turbo_snail", Group.SNAIL, Model.TURBO, 0.3, 0.025, true, Breeding.NONE, 0, 0, 0, 0, 3.0, 22, 28, 0.4, 0.03, 2.5,
            new String[0], 0x6A6A3E);

    /** Stocking group: how many of them count as one fish ({@code stocking.*PerFish}). */
    public enum Group { SHRIMP, CRAYFISH, SNAIL }

    /** Body plan of the renderer (and of tools/assetgen/critters.py). */
    public enum Model { SHRIMP, CRAYFISH, NERITE, APPLE, RAMSHORN, BLADDER, TRUMPET, ASSASSIN, HERMIT, TURBO }

    public enum Breeding {
        /** No eggs, no young in fresh water (nerites). */
        NONE,
        /** Male and female; the female carries the eggs under her tail, the young hatch fully formed (shrimp, crayfish). */
        PAIR_BERRIED,
        /** Male and female; the female lays a pink clutch on the glass above the water line (apple snails). */
        PAIR_CLUTCH,
        /** Hermaphrodites: one adult is enough, few eggs at a time (ramshorn, bladder and trumpet snails). */
        SELF,
        /** Male and female, rarely one young at a time (assassin snail). */
        PAIR
    }

    private final String id;
    private final Group group;
    private final Model model;
    private final double length, speed;
    private final boolean climbsGlass;
    private final Breeding breeding;
    private final double breedRate, eggDays;
    private final int broodMin, broodMax;
    private final double juvenileDays;
    private final float minTemp, maxTemp;
    private final float badWater;
    private final double foodPerDay, algaePerDay;
    private final String[] loci;
    private final int wildColor;

    CritterSpecies(String id, Group group, Model model, double length, double speed, boolean climbsGlass, Breeding breeding, double breedRate,
                   double eggDays, int broodMin, int broodMax, double juvenileDays, float minTemp, float maxTemp, double badWater, double foodPerDay,
                   double algaePerDay, String[] loci, int wildColor) {
        this.id = id;
        this.group = group;
        this.model = model;
        this.length = length;
        this.speed = speed;
        this.climbsGlass = climbsGlass;
        this.breeding = breeding;
        this.breedRate = breedRate;
        this.eggDays = eggDays;
        this.broodMin = broodMin;
        this.broodMax = broodMax;
        this.juvenileDays = juvenileDays;
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.badWater = (float) badWater;
        this.foodPerDay = foodPerDay;
        this.algaePerDay = algaePerDay;
        this.loci = loci;
        this.wildColor = wildColor;
    }

    public String id() { return id; }
    public Group group() { return group; }
    public Model model() { return model; }
    /** Body length of an adult in blocks. */
    public double length() { return length; }
    /** Walking (crawling) speed in blocks per second. */
    public double speed() { return speed; }
    public boolean climbsGlass() { return climbsGlass; }
    public Breeding breeding() { return breeding; }
    /** Chance per day that a ready adult (female) starts a brood. */
    public double breedRate() { return breedRate; }
    /** Days the female carries her eggs, or a clutch needs to hatch. */
    public double eggDays() { return eggDays; }
    public int broodMin() { return broodMin; }
    public int broodMax() { return broodMax; }
    public double juvenileDays() { return juvenileDays; }
    public float minTemp() { return minTemp; }
    public float maxTemp() { return maxTemp; }
    /** Below this water quality the animal suffers and may die. */
    public float badWater() { return badWater; }
    /** Food portions an adult needs per day (biofilm, algae, leftovers). */
    public double foodPerDay() { return foodPerDay; }
    /** Glass faces of algae an adult grazes clean per day (1 = one fully grown face). */
    public double algaePerDay() { return algaePerDay; }
    /** Colour loci (recessive colour alleles against the wild colour), empty for species with one colour. */
    public String[] loci() { return loci; }
    public int wildColor() { return wildColor; }

    public boolean isShrimp() { return group == Group.SHRIMP; }
    public boolean isCrayfish() { return group == Group.CRAYFISH; }
    public boolean isSnail() { return group == Group.SNAIL; }
    public boolean nerite() { return model == Model.NERITE; }
    public boolean hermaphrodite() { return breeding == Breeding.SELF; }
    public boolean hasSexes() { return breeding != Breeding.SELF; }
    public boolean breeds() { return breeding != Breeding.NONE; }
    /** Trumpet snails dig into the substrate by day. */
    public boolean burrows() { return this == TRUMPET_SNAIL; }
    /** Animals with a colour line also have a grade (colour density) that is bred up over generations. */
    public boolean hasGrade() { return loci.length > 0; }
    /** M20: a reef animal (salt water). */
    public boolean marine() { return org.overengineer.bettacraft.reef.SaltTolerance.critter(this).marine(); }

    public String translationKey() {
        return "critter.bettacraft." + id;
    }

    public static CritterSpecies byId(String id) {
        CritterSpecies s = byIdOrNull(id);
        return s == null ? NEOCARIDINA : s;
    }

    @Nullable
    public static CritterSpecies byIdOrNull(String id) {
        String k = id.toLowerCase(Locale.ROOT);
        for (CritterSpecies s : values()) if (s.id.equals(k)) return s;
        return null;
    }
}
