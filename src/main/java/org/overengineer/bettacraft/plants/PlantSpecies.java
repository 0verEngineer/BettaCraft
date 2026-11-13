package org.overengineer.bettacraft.plants;

import java.util.Locale;
import javax.annotation.Nullable;

/**
 * Aquarium plants of the tank interior (M13). Pure data: growth form, time to full size, light need, colours (the
 * block textures are grey and tinted per species, part and redness) and the leaf texture. Sizes are in interior
 * blocks at scale 16 (one interior block = one pixel of the tank, about 0.6 mm), see {@link PlantShapes}.
 */
public enum PlantSpecies {
    // epiphytes: on stone and wood (decor too), very slow to slow
    JAVA_FERN("java_fern", Form.EPIPHYTE, 12, 8, Light.LOW, Red.NONE, "frond", 0x3E7A2E, 0x5C9A3A, 0, 0x3A6228, 0x5A4A2E),
    ANUBIAS_NANA("anubias_nana", Form.EPIPHYTE, 25, 6, Light.LOW, Red.NONE, "broad", 0x2C5A28, 0x4A8436, 0, 0x3C6629, 0x4E5A30),
    BUCE_GREEN_WAVY("bucephalandra_green_wavy", Form.EPIPHYTE, 20, 6, Light.LOW, Red.NONE, "wavy", 0x3F6E34, 0x5E8C3E, 0, 0x46603A, 0x4E4A30),
    BUCE_BROWNIE_RED("bucephalandra_brownie_red", Form.EPIPHYTE, 22, 6, Light.LOW, Red.RED, "wavy", 0x5E5232, 0x6E5A36, 0x8E2E2A, 0x5A4630, 0x4E3E2A),
    BUCE_BLUE("bucephalandra_blue", Form.EPIPHYTE, 22, 6, Light.LOW, Red.NONE, "wavy", 0x2C4C5A, 0x3C6C80, 0, 0x34505A, 0x3E4436),
    BUCE_MINI_NEEDLE("bucephalandra_mini_needle", Form.EPIPHYTE, 20, 6, Light.LOW, Red.NONE, "needle", 0x456E36, 0x5E8A40, 0, 0x46603A, 0x4E4A30),
    // moss: on any solid block, medium
    JAVA_MOSS("java_moss", Form.MOSS, 6, 6, Light.LOW, Red.NONE, "moss", 0x4A7A2C, 0x5E9036, 0, 0x4A7A2C, 0x4A7A2C),
    // rosettes: on substrate, slow to medium
    CRYPT_GREEN("cryptocoryne_wendtii_green", Form.ROSETTE, 9, 6, Light.LOW, Red.NONE, "broad", 0x4C7A34, 0x689842, 0, 0x5E7E40, 0x5E7E40),
    CRYPT_BROWN("cryptocoryne_wendtii_brown", Form.ROSETTE, 9, 6, Light.MID, Red.RED, "wavy", 0x5E5A34, 0x6A6036, 0x7E3A2E, 0x6A5636, 0x6A5636),
    CRYPT_FLAMINGO("cryptocoryne_flamingo", Form.ROSETTE, 14, 6, Light.MID, Red.FLAMINGO, "broad", 0xD0B4B0, 0xDCC0BC, 0xF6C6D4, 0xC8A8A0, 0xC8A8A0),
    AMAZON_SWORD("amazon_sword", Form.ROSETTE, 7, 8, Light.MID, Red.NONE, "broad", 0x5A9638, 0x7CB648, 0, 0x6A963E, 0x6A963E),
    // stem plants: on substrate, fast, up to the surface, then side shoots
    ROTALA("rotala_rotundifolia", Form.STEM, 2.5, 8, Light.HIGH, Red.TIPS, "fine", 0x6A9A3C, 0x8AA040, 0xE05A4C, 0x7A8A44, 0x7A8A44),
    LUDWIGIA("ludwigia_repens", Form.STEM, 3, 8, Light.MID, Red.RED, "fine", 0x6A8A3A, 0x7E9040, 0xB0462E, 0x7A5A36, 0x7A5A36),
    ALTERNANTHERA("alternanthera_reineckii", Form.STEM, 3.5, 8, Light.HIGH, Red.RED, "fine", 0x7A4A40, 0x8A5048, 0xB42A40, 0x8A3A3A, 0x8A3A3A),
    // runners: on substrate, fast, spread
    VALLISNERIA("vallisneria", Form.RIBBON, 2.5, 8, Light.MID, Red.NONE, "ribbon", 0x6AA040, 0x84B850, 0, 0x6AA040, 0x6A8A3A),
    MONTE_CARLO("monte_carlo", Form.CARPET, 3, 6, Light.HIGH, Red.NONE, "mat", 0x62A83A, 0x7CC046, 0, 0x62A83A, 0x62A83A),
    // tuber: on substrate, medium, floating leaves at the surface
    TIGER_LOTUS("red_tiger_lotus", Form.BULB, 6, 8, Light.MID, Red.RED, "lotus", 0x6A3A30, 0x7A4234, 0xAE2A30, 0x7A5A3A, 0x6A4A2A),
    // M20 corals and the anemone of the reef tank: on reef rock, salt water only; colours: tissue, tips, glow under actinic light
    // soft corals: no calcium, gentle to medium flow, the polyps spread over the rock
    ZOANTHUS("zoanthus", Form.POLYPS, 5, 6, Light.MID, Red.CORAL, "polyp", 0x3E8A5A, 0xF07830, 0xFFB04A, 0x4A5A3A, 0x4A5A3A),
    GREEN_STAR_POLYPS("green_star_polyps", Form.POLYPS, 4, 6, Light.MID, Red.CORAL, "polyp", 0x6A2E6E, 0x5AE05A, 0x9AFF6A, 0x5A2A5A, 0x5A2A5A),
    MUSHROOM_CORAL("mushroom_coral", Form.DISC, 8, 6, Light.LOW, Red.CORAL, "disc", 0x9A2E52, 0xC85A7A, 0xFF5AA8, 0x7A3A4A, 0x7A3A4A),
    LEATHER_CORAL("leather_coral", Form.TOADSTOOL, 10, 8, Light.MID, Red.CORAL, "polyp", 0xC8B888, 0xE0D8A8, 0xE8FFB0, 0xB0A27A, 0xB0A27A),
    // stony corals (LPS, SPS): they build a skeleton of calcium and need more light and flow
    HAMMER_CORAL("hammer_coral", Form.BRANCH, 12, 8, Light.MID, Red.CORAL, "tentacle", 0x3E8A74, 0xA8F0C0, 0x5AFFD8, 0xA89A80, 0xA89A80),
    LOBOPHYLLIA("lobophyllia", Form.DOME, 15, 8, Light.MID, Red.CORAL, "brain", 0xA8402A, 0x4E9A4A, 0xFF6A3A, 0x8A3A2A, 0x8A3A2A),
    ACROPORA("acropora", Form.BRANCH, 14, 8, Light.HIGH, Red.CORAL, "branch", 0x5A62C0, 0xA8C0FF, 0x7AA8FF, 0x7A7AA8, 0x7A7AA8),
    // the host anemone of the clownfish (Entacmaea quadricolor): tentacles with bubble tips on a sticky foot
    BUBBLE_TIP_ANEMONE("bubble_tip_anemone", Form.ANEMONE, 6, 6, Light.MID, Red.CORAL, "tentacle", 0xC24A56, 0xF4A6B8, 0xFF7AA6, 0xD8A890, 0xD8A890);

    /** Growth form: decides the shape ({@link PlantShapes}), where it may be planted and how it spreads. */
    public enum Form {
        EPIPHYTE, MOSS, ROSETTE, STEM, RIBBON, CARPET, BULB,
        /** M20: coral forms (on reef rock): a mat of polyps, mushroom discs, a leather toadstool, branches, a dome, an anemone. */
        POLYPS, DISC, TOADSTOOL, BRANCH, DOME, ANEMONE;

        /** Needs substrate (the interior floor with a substrate, sand, gravel, dirt ...). */
        public boolean rooted() {
            return this == ROSETTE || this == STEM || this == RIBBON || this == CARPET || this == BULB;
        }

        /** M20: a coral (or the anemone): glued onto reef rock, lives in salt water. */
        public boolean coral() {
            return ordinal() >= POLYPS.ordinal();
        }
    }

    /** M20: how much water movement a coral wants (relative to the strong flow of {@code TankReef#flow}). */
    public enum Flow {
        LOW(0.15, 1.6), MID(0.35, 2.2), HIGH(0.7, 3.0);

        /** Full growth from this flow on; above {@code max} the tissue is torn and the coral stops growing. */
        public final double need, max;

        Flow(double need, double max) {
            this.need = need;
            this.max = max;
        }
    }

    /** Light need: growth starts at {@code from} (surface light 0..15) and is full at {@code full}. */
    public enum Light {
        LOW(4, 8), MID(6, 11), HIGH(9, 14);

        public final int from, full;

        Light(int from, int full) {
            this.from = from;
            this.full = full;
        }
    }

    /** How the colour follows the light ({@link PlantColors}). */
    public enum Red {
        /** Always the same green. */
        NONE,
        /** Leaves and tips get redder with more light. */
        RED,
        /** Only the young tips turn red with more light (Rotala). */
        TIPS,
        /** Stays whitish pink and gets brighter and pinker in strong light. */
        FLAMINGO,
        /** M20 corals: 0 = bleached, 1 = pale, 2 = healthy colours, 3 = glowing under actinic light. */
        CORAL
    }

    private final String id;
    private final Form form;
    private final double daysToFull;
    private final int stages;
    private final Light light;
    private final Red red;
    private final String leafTexture;
    final int leaf, top, redColor, stem, rhizome;

    PlantSpecies(String id, Form form, double daysToFull, int stages, Light light, Red red, String leafTexture,
                 int leaf, int top, int redColor, int stem, int rhizome) {
        this.id = id;
        this.form = form;
        this.daysToFull = daysToFull;
        this.stages = stages;
        this.light = light;
        this.red = red;
        this.leafTexture = leafTexture;
        this.leaf = leaf;
        this.top = top;
        this.redColor = redColor;
        this.stem = stem;
        this.rhizome = rhizome;
    }

    /** Item and block id path. */
    public String id() { return id; }

    public Form form() { return form; }

    /** M21: drawn with real leaf shapes in the tank interior (prototype, config features.plantMeshes). */
    public boolean meshPrototype() {
        return this == ROTALA || this == VALLISNERIA || this == ANUBIAS_NANA;
    }

    /** In-game days from cutting to full size under good conditions (growth speed 1). */
    public double daysToFull() { return daysToFull; }

    /** Number of growth stages after the cutting (stage 0 = cutting, {@link #stages()} = full size). */
    public int stages() { return stages; }

    /** Days per growth stage under good conditions. */
    public double daysPerStage() { return daysToFull / stages; }

    public Light light() { return light; }

    public Red red() { return red; }

    /** Whether the colour depends on the light (red species and the Flamingo crypt). */
    public boolean tinted() { return red != Red.NONE; }

    /** Grey leaf texture name ({@code textures/block/plant_leaf_<name>}). */
    public String leafTexture() { return leafTexture; }

    /** Spreads by itself once fully grown: side shoots (stem plants) or runners; M20: polyps and mushrooms over the rock. */
    public boolean spreads() {
        return form == Form.STEM || form == Form.RIBBON || form == Form.CARPET || this == CRYPT_GREEN || this == CRYPT_BROWN
                || form == Form.POLYPS || form == Form.DISC;
    }

    /** M20: a coral or the anemone (salt water, reef rock). */
    public boolean marine() {
        return form.coral();
    }

    /** M20: stony corals build a calcium skeleton (they need calcium and use it up). */
    public boolean stony() {
        return this == HAMMER_CORAL || this == LOBOPHYLLIA || this == ACROPORA;
    }

    /** M20: the flow a coral wants. */
    public Flow flow() {
        return switch (this) {
            case ZOANTHUS, MUSHROOM_CORAL, LOBOPHYLLIA -> Flow.LOW;
            case ACROPORA -> Flow.HIGH;
            default -> Flow.MID;
        };
    }

    /** M20: the host anemone of the clownfish. */
    public boolean anemone() {
        return form == Form.ANEMONE;
    }

    /** Stages one trim with shears takes off. */
    public int trimStages() {
        return form == Form.STEM || form == Form.RIBBON || form == Form.CARPET || form == Form.MOSS || form == Form.POLYPS ? 2 : 1;
    }

    /** Lowest stage trimming leaves (the plant stays alive). */
    public int trimFloor() {
        return form == Form.STEM ? 2 : 1;
    }

    /** Cuttings one trim gives (stem plants give the cut tops). */
    public int cuttingsPerTrim(int stagesCut) {
        return switch (form) {
            case STEM -> 1 + stagesCut;
            case RIBBON, CARPET, MOSS, POLYPS -> stagesCut;
            case BRANCH -> 1 + stagesCut / 2; // M20: frags of the branch tips
            default -> 1;
        };
    }

    public static PlantSpecies byId(String s) {
        PlantSpecies p = byIdOrNull(s);
        return p == null ? JAVA_FERN : p;
    }

    @Nullable
    public static PlantSpecies byIdOrNull(String s) {
        String k = s == null ? "" : s.toLowerCase(Locale.ROOT);
        for (PlantSpecies t : values()) if (t.id.equals(k)) return t;
        return null;
    }
}
