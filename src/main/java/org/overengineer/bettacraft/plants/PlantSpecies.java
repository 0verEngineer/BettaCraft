package org.overengineer.bettacraft.plants;

import java.util.Locale;
import javax.annotation.Nullable;

/**
 * Aquarium plants of the tank interior (M13). Pure data: growth form, time to full size, light need, colours (the
 * block textures are grey and tinted per species, part and redness) and the leaf texture. Sizes are in interior
 * blocks at scale 16 (one interior block = one pixel of the tank, about 0.6 mm), see {@link PlantShapes}.
 */
public enum PlantSpecies implements net.minecraft.util.StringRepresentable {
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
    ALTERNANTHERA("alternanthera_reineckii", Form.STEM, 3.5, 8, Light.VERY_HIGH, Red.RED, "fine", 0x7A4A40, 0x8A5048, 0xB42A40, 0x8A3A3A, 0x8A3A3A),
    // runners: on substrate, fast, spread
    VALLISNERIA("vallisneria", Form.RIBBON, 2.5, 8, Light.MID, Red.NONE, "ribbon", 0x6AA040, 0x84B850, 0, 0x6AA040, 0x6A8A3A),
    MONTE_CARLO("monte_carlo", Form.CARPET, 3, 6, Light.VERY_HIGH, Red.NONE, "mat", 0x62A83A, 0x7CC046, 0, 0x62A83A, 0x62A83A),
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
    BUBBLE_TIP_ANEMONE("bubble_tip_anemone", Form.ANEMONE, 6, 6, Light.MID, Red.CORAL, "tentacle", 0xC24A56, 0xF4A6B8, 0xFF7AA6, 0xD8A890, 0xD8A890),
    // M28 part a: more Anubias (very slow shade plants), Bucephalandras and stem plants. Always appended: the order seeds
    // the shapes ({@link PlantShapes}), so a species in the middle would change the plants of existing worlds.
    ANUBIAS_PETITE("anubias_nana_petite", Form.EPIPHYTE, 30, 6, Light.LOW, Red.NONE, "broad", 0x2A5626, 0x45803A, 0, 0x3A6228, 0x4E5A30),
    /** White marbled leaves: the mesh draws them with a coloured texture of its own ({@code leaf_pinto}). */
    ANUBIAS_PINTO("anubias_pinto", Form.EPIPHYTE, 32, 6, Light.LOW, Red.NONE, "broad", 0x9CBC8A, 0xC8DCB8, 0, 0x5A7A48, 0x4E5A30),
    ANUBIAS_WHITE("anubias_white", Form.EPIPHYTE, 30, 6, Light.MID, Red.NONE, "broad", 0xC6D4B2, 0xEEF0E4, 0, 0xB4C29C, 0x5A5A3A),
    ANUBIAS_COFFEEFOLIA("anubias_coffeefolia", Form.EPIPHYTE, 25, 6, Light.LOW, Red.NONE, "broad", 0x2C5026, 0x56602E, 0, 0x3A5A28, 0x4E4A30),
    ANUBIAS_ROUND_LEAF("anubias_barteri_round_leaf", Form.EPIPHYTE, 28, 8, Light.LOW, Red.NONE, "broad", 0x2E5C2A, 0x4C8838, 0, 0x3C6629, 0x4E5A30),
    BUCE_KEDAGANG("bucephalandra_kedagang", Form.EPIPHYTE, 22, 6, Light.LOW, Red.NONE, "wavy", 0x2A4E40, 0x3E6E5C, 0, 0x3A4E3A, 0x3E4436),
    BUCE_THEIA("bucephalandra_theia", Form.EPIPHYTE, 20, 6, Light.LOW, Red.NONE, "wavy", 0x4E7A3E, 0x6E9A4A, 0, 0x4A603A, 0x4E4A30),
    BUCE_GODZILLA("bucephalandra_godzilla", Form.EPIPHYTE, 24, 6, Light.LOW, Red.RED, "wavy", 0x4A5A30, 0x5A6236, 0x7A2E2E, 0x5A4630, 0x4E3E2A),
    BUCE_PINK_LADY("bucephalandra_pink_lady", Form.EPIPHYTE, 24, 6, Light.MID, Red.WHITE, "wavy", 0x6E8A5A, 0x86A06A, 0xF2CAD4, 0x6A6A4A, 0x4E4A30),
    BUCE_MINI_PINK("bucephalandra_mini_pink", Form.EPIPHYTE, 22, 6, Light.MID, Red.WHITE, "wavy", 0x708C5C, 0x8AA46E, 0xF2B6C8, 0x6A6A4A, 0x4E4A30),
    BUCE_SNOW_WHITE("bucephalandra_snow_white", Form.EPIPHYTE, 26, 6, Light.MID, Red.WHITE, "wavy", 0x7A9468, 0x94AC80, 0xF6F2EC, 0x7A7A5A, 0x4E4A30),
    LUDWIGIA_SUPER_RED("ludwigia_super_red", Form.STEM, 3.5, 8, Light.HIGH, Red.RED, "fine", 0x8A4E3A, 0xA0583E, 0xCC2238, 0x7A3A30, 0x7A3A30),
    ROTALA_HRA("rotala_hra", Form.STEM, 3, 8, Light.HIGH, Red.RED, "fine", 0x7A9A44, 0x9AA048, 0xEA6E46, 0x8A6A40, 0x8A6A40),
    ROTALA_WALLICHII("rotala_wallichii", Form.STEM, 3.5, 8, Light.VERY_HIGH, Red.TIPS, "fine", 0x7A9A40, 0x9A9A4A, 0xF0507A, 0x8A5A44, 0x8A5A44),
    HYGROPHILA_PINNATIFIDA("hygrophila_pinnatifida", Form.STEM, 5, 8, Light.MID, Red.RED, "fine", 0x4E6A36, 0x6A7A3E, 0x8E4A30, 0x5A4A30, 0x5A4A30),
    POGOSTEMON_DOWNOI("pogostemon_helferi", Form.STEM, 6, 6, Light.HIGH, Red.NONE, "fine", 0x7AB04A, 0x94C45A, 0, 0x6A9A40, 0x6A9A40),
    LIMNOPHILA_SESSILIFLORA("limnophila_sessiliflora", Form.STEM, 2.5, 8, Light.MID, Red.NONE, "fine", 0x78B246, 0x96C85A, 0, 0x6A9A3E, 0x6A9A3E),
    MYRIOPHYLLUM_RED("myriophyllum_red", Form.STEM, 3, 8, Light.HIGH, Red.RED, "fine", 0x8A5A44, 0x9A6048, 0xC83A3A, 0x7A4A3A, 0x7A4A3A),
    BACOPA_CAROLINIANA("bacopa_caroliniana", Form.STEM, 4, 8, Light.MID, Red.TIPS, "fine", 0x6A9A44, 0x84AA4E, 0xB4683A, 0x6A8A3E, 0x6A8A3E),
    // M28 part b: carpets, mosses, rosettes and tubers (appended like part a)
    /** Dwarf baby tears: the finest carpet, strong light and CO2. */
    HC_CUBA("hemianthus_callitrichoides", Form.CARPET, 4, 6, Light.VERY_HIGH, Red.NONE, "mat", 0x5EAE36, 0x7ECC48, 0, 0x5E9A36, 0x5E9A36),
    /** Hairgrass: a lawn of upright needles. */
    ELEOCHARIS("eleocharis_acicularis", Form.CARPET, 3.5, 6, Light.MID, Red.NONE, "needle", 0x5E9A36, 0x80B44A, 0, 0x5A8A34, 0x5A8A34),
    STAUROGYNE("staurogyne_repens", Form.CARPET, 4.5, 6, Light.MID, Red.NONE, "fine", 0x4E8A34, 0x6AA444, 0, 0x4E7A30, 0x4E7A30),
    /** Water clover: four small leaflets on every stalk, undemanding. */
    MARSILEA("marsilea_hirsuta", Form.CARPET, 6, 6, Light.LOW, Red.NONE, "mat", 0x4E9238, 0x68AA46, 0, 0x4E7A30, 0x4E7A30),
    CHRISTMAS_MOSS("christmas_moss", Form.MOSS, 8, 6, Light.LOW, Red.NONE, "moss", 0x3E7028, 0x548A32, 0, 0x3E7028, 0x3E7028),
    FLAME_MOSS("flame_moss", Form.MOSS, 8, 6, Light.LOW, Red.NONE, "moss", 0x3A6A28, 0x5A8C34, 0, 0x3A6A28, 0x3A6A28),
    /** Crystalwort: a bright cushion tied onto stone or wood, or floating ({@code FloatingPlantType#RICCIA}). */
    RICCIA("riccia_fluitans", Form.MOSS, 4, 6, Light.HIGH, Red.NONE, "moss", 0x7CBE3E, 0x9AD650, 0, 0x7CBE3E, 0x7CBE3E),
    /** A ball of algae (Aegagropila linnaei): grows very slowly, never minds the cold and rolls with the current. */
    MARIMO("marimo", Form.MOSS, 60, 6, Light.LOW, Red.NONE, "moss", 0x2E6A2A, 0x3E7E32, 0, 0x2E6A2A, 0x2E6A2A),
    ECHINODORUS_RED_DIAMOND("echinodorus_red_diamond", Form.ROSETTE, 8, 8, Light.MID, Red.RED, "broad", 0x6A5A36, 0x7A5E3A, 0xB0283A, 0x6A4A34, 0x6A4A34),
    /** The smallest crypt: a slow lawn of narrow leaves. */
    CRYPT_PARVA("cryptocoryne_parva", Form.ROSETTE, 16, 6, Light.LOW, Red.NONE, "broad", 0x3E7A2E, 0x58943A, 0, 0x4A6A30, 0x4A6A30),
    /** Dwarf sagittaria: grass-like leaves, spreads by runners. */
    SAGITTARIA("sagittaria_subulata", Form.ROSETTE, 4, 6, Light.LOW, Red.NONE, "ribbon", 0x5A9A3A, 0x76B248, 0, 0x5A8A38, 0x5A8A38),
    /** A tuber with long, undulate leaves and a flower spike at the surface. */
    APONOGETON("aponogeton_ulvaceus", Form.BULB, 6, 8, Light.MID, Red.NONE, "wavy", 0x6AA640, 0x88BC52, 0xF2EEDA, 0x6A8A3E, 0x7A6A42),
    /** The green tiger lotus (Nymphaea lotus): green leaves with brown spots. */
    NYMPHAEA_GREEN("nymphaea_lotus_green", Form.BULB, 6, 8, Light.MID, Red.NONE, "lotus", 0x4E8A34, 0x6AA040, 0, 0x6A7A3A, 0x6A5A32);

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
        LOW(4, 8), MID(6, 11), HIGH(9, 14),
        /** M23: strong-light plants: grow slowly under the light bar (15), fully only under an LED panel or high-power LED. */
        VERY_HIGH(12, 18);

        public final int from, full;

        /** The need that counts now: without features.ledTiers the strong-light plants are content with the bar like before. */
        public static Light need(Light l) {
            return l == VERY_HIGH && !org.overengineer.bettacraft.config.CommonConfig.ledTiers() ? HIGH : l;
        }

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
        CORAL,
        /**
         * M28: greenish in ordinary light, white to whitish pink only in strong light (fully with the LED tiers only under
         * an LED panel or a high-power LED): the pink and white Bucephalandras.
         */
        WHITE
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

    /** M28.1: the species as a block state value of the wild plants ({@code plants/wild}). */
    @Override
    public String getSerializedName() { return id; }

    public Form form() { return form; }

    /**
     * Drawn with real shapes inside and outside (M21 prototype: Rotala, Vallisneria, Anubias; M22.1: every plant and
     * coral). Config {@code features.plantMeshes} (pack) and {@code render.plantMeshes} (each player).
     */
    public boolean meshed() {
        return true;
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
                || form == Form.POLYPS || form == Form.DISC || this == CRYPT_PARVA || this == SAGITTARIA;
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

    /** M28: grows on stone and wood (hardscape) as well as on substrate: Hygrophila pinnatifida. */
    public boolean alsoOnHardscape() {
        return this == HYGROPHILA_PINNATIFIDA;
    }

    /**
     * M28: grows well only with CO2 ({@code features.co2}): without dosed CO2 at {@link #NO_CO2_GROWTH} of its speed (the
     * demanding stem plants of the aquascapes).
     */
    public boolean needsCo2() {
        return switch (this) {
            case LUDWIGIA_SUPER_RED, ROTALA_HRA, ROTALA_WALLICHII, POGOSTEMON_DOWNOI, MYRIOPHYLLUM_RED, HC_CUBA -> true;
            default -> false;
        };
    }

    /** M28: growth of a {@link #needsCo2()} plant without CO2 (it rises to the full speed with a good CO2 level). */
    public static final double NO_CO2_GROWTH = 0.4;

    /** M28: wants soft, slightly acidic water ({@code features.waterChemistry}): slower in hard or alkaline water. */
    public boolean softWater() {
        return this == ROTALA_WALLICHII || this == MYRIOPHYLLUM_RED;
    }

    /** M28 part b: Riccia is tied onto stone or wood only (the other mosses grow on any solid block). */
    public boolean onlyOnHardscape() {
        return this == RICCIA;
    }

    /**
     * M28 part b: the Marimo ball: grows even in cold water and rolls over the floor with the current
     * ({@code InteriorPlants#roll}).
     */
    public boolean rolls() {
        return this == MARIMO;
    }

    /** M28: the species that came after the plant tables became config lists (M22.4); see {@code PlantTables}. */
    public boolean newerThanConfigLists() {
        return ordinal() > BUBBLE_TIP_ANEMONE.ordinal();
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
