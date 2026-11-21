package org.overengineer.bettacraft.plants;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
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
    NYMPHAEA_GREEN("nymphaea_lotus_green", Form.BULB, 6, 8, Light.MID, Red.NONE, "lotus", 0x4E8A34, 0x6AA040, 0, 0x6A7A3A, 0x6A5A32),
    // M56: the plants of terraria and paludaria (on the land of the interior; appended like M28). Colours: leaf, young leaf /
    // bract / flower ("top"), red, stem, rhizome
    // tropical and humid: the rainforest terrarium and the land of a paludarium
    NEOREGELIA(Habitat.TROPICAL, "neoregelia", Form.ROSETTE, 12, 6, Light.MID, Red.RED, "strap", 0x4E7E36, 0xC2364E, 0xD8203E, 0x56682E, 0x4A5A2A),
    GUZMANIA(Habitat.TROPICAL, "guzmania", Form.ROSETTE, 14, 6, Light.MID, Red.NONE, "strap", 0x3E7A3A, 0xF2782A, 0, 0x4E6A30, 0x4A5A2A),
    VRIESEA(Habitat.TROPICAL, "vriesea", Form.ROSETTE, 14, 6, Light.MID, Red.NONE, "banded", 0x4A6E3C, 0xE84630, 0, 0x4E6430, 0x4A5A2A),
    TILLANDSIA(Habitat.TROPICAL, "tillandsia", Form.EPIPHYTE, 10, 6, Light.HIGH, Red.RED, "spiky", 0x9EB4A0, 0xB8C8B4, 0xC84A78, 0x8A9A84, 0x7A7A60),
    FITTONIA(Habitat.TROPICAL, "fittonia", Form.STEM, 5, 6, Light.LOW, Red.NONE, "veined", 0x3A6A30, 0x56883E, 0, 0x6A7040, 0x6A7040),
    POTHOS(Habitat.TROPICAL, "pothos", Form.STEM, 6, 8, Light.LOW, Red.NONE, "heart", 0x4C8A2E, 0x9CBC4A, 0, 0x6A8038, 0x6A8038),
    MARCGRAVIA(Habitat.TROPICAL, "marcgravia", Form.EPIPHYTE, 18, 6, Light.LOW, Red.NONE, "shingle", 0x3A6A2A, 0x548A36, 0, 0x5A6A30, 0x4E5A2E),
    BOSTON_FERN(Habitat.TROPICAL, "boston_fern", Form.ROSETTE, 9, 6, Light.LOW, Red.NONE, "fern", 0x4E8E30, 0x6EAE42, 0, 0x5A7A30, 0x4A5A2A),
    BIRDS_NEST_FERN(Habitat.TROPICAL, "birds_nest_fern", Form.ROSETTE, 16, 6, Light.LOW, Red.NONE, "wavy", 0x5A9A36, 0x7AB84A, 0, 0x3A3A2A, 0x3A3A2A),
    CUSHION_MOSS(Habitat.TROPICAL, "cushion_moss", Form.MOSS, 12, 6, Light.LOW, Red.NONE, "moss", 0x6E9E4A, 0x8AB45A, 0, 0x6E9E4A, 0x6E9E4A),
    REINDEER_LICHEN(Habitat.TROPICAL, "reindeer_lichen", Form.MOSS, 20, 6, Light.MID, Red.NONE, "lichen", 0xC2CCB2, 0xDADFCC, 0, 0xB0B8A0, 0xB0B8A0),
    SELAGINELLA(Habitat.TROPICAL, "selaginella", Form.CARPET, 6, 6, Light.LOW, Red.NONE, "fern", 0x4E9638, 0x6CB04A, 0, 0x4E7A30, 0x4E7A30),
    MINI_ORCHID(Habitat.TROPICAL, "mini_orchid", Form.EPIPHYTE, 24, 6, Light.MID, Red.NONE, "broad", 0x3E6E30, 0xE27AB8, 0, 0x5A6A3A, 0x6A6A48),
    BEGONIA(Habitat.TROPICAL, "begonia_rex", Form.ROSETTE, 10, 6, Light.LOW, Red.NONE, "velvet", 0xA0A2AE, 0x8A3E62, 0, 0x7A3E3E, 0x5A3A30),
    PEPEROMIA(Habitat.TROPICAL, "peperomia", Form.STEM, 8, 6, Light.LOW, Red.NONE, "round", 0x3A6A2C, 0x568A38, 0, 0x8A5A44, 0x8A5A44),
    // swamp and the shore of a paludarium: carnivorous plants, sedges, rushes and papyrus
    SUNDEW(Habitat.SWAMP, "sundew", Form.ROSETTE, 8, 6, Light.HIGH, Red.RED, "sundew", 0x8AA040, 0xD03A3A, 0xE0222E, 0x7A8A3A, 0x6A6A3A),
    SARRACENIA(Habitat.SWAMP, "sarracenia", Form.ROSETTE, 12, 6, Light.HIGH, Red.RED, "pitcher", 0x8AA844, 0xB43630, 0xC8282C, 0x7A8A3A, 0x6A6A3A),
    NEPENTHES(Habitat.SWAMP, "nepenthes", Form.ROSETTE, 16, 8, Light.MID, Red.RED, "pitcher", 0x5E9A36, 0x9A4A3A, 0xB82E30, 0x6A7A34, 0x5A6030),
    VENUS_FLYTRAP(Habitat.SWAMP, "venus_flytrap", Form.ROSETTE, 10, 6, Light.HIGH, Red.RED, "trap", 0x6AA840, 0xC84030, 0xE0262C, 0x6A8A34, 0x5A6A30),
    BUTTERWORT(Habitat.SWAMP, "butterwort", Form.ROSETTE, 8, 6, Light.MID, Red.NONE, "broad", 0x9CC45E, 0xB688D8, 0, 0x8AA850, 0x7A8A44),
    SWAMP_SEDGE(Habitat.SWAMP, "swamp_sedge", Form.ROSETTE, 5, 6, Light.MID, Red.NONE, "narrow", 0x5E9440, 0x7AAA50, 0, 0x6A7A3A, 0x6A7A3A),
    SOFT_RUSH(Habitat.SWAMP, "soft_rush", Form.ROSETTE, 6, 6, Light.MID, Red.NONE, "needle", 0x4E8A36, 0x9A8A50, 0, 0x5A7A30, 0x5A7A30),
    PAPYRUS(Habitat.SWAMP, "papyrus", Form.STEM, 8, 8, Light.HIGH, Red.NONE, "needle", 0x6AA448, 0x8CC05C, 0, 0x5E8E3E, 0x5A6A30),
    UMBRELLA_SEDGE(Habitat.SWAMP, "umbrella_sedge", Form.STEM, 7, 8, Light.MID, Red.NONE, "narrow", 0x4E8E36, 0x6AAA46, 0, 0x5A8A3A, 0x5A6A30),
    // dry: the desert terrarium (cacti and succulents grow offsets)
    BARREL_CACTUS(Habitat.DRY, "golden_barrel_cactus", Form.CACTUS, 40, 6, Light.HIGH, Red.NONE, "cactus", 0x4E8A3E, 0xE8C850, 0, 0x4E8A3E, 0x6A6A40),
    COLUMN_CACTUS(Habitat.DRY, "column_cactus", Form.CACTUS, 30, 8, Light.HIGH, Red.NONE, "cactus", 0x5A8C70, 0xF0EED8, 0, 0x5A8C70, 0x6A6A40),
    PRICKLY_PEAR(Habitat.DRY, "prickly_pear", Form.CACTUS, 20, 8, Light.HIGH, Red.NONE, "pad", 0x6A9C42, 0xE8D04A, 0, 0x6A9C42, 0x6A6A40),
    MAMMILLARIA(Habitat.DRY, "mammillaria", Form.CACTUS, 30, 6, Light.HIGH, Red.NONE, "cactus", 0x5C8E4E, 0xE866A6, 0, 0x5C8E4E, 0x6A6A40),
    ECHEVERIA(Habitat.DRY, "echeveria", Form.ROSETTE, 14, 6, Light.HIGH, Red.RED, "succulent", 0x8EACB0, 0xA6C0BC, 0xC8607E, 0x7A9A90, 0x6A7A6A),
    HAWORTHIA(Habitat.DRY, "haworthia", Form.ROSETTE, 18, 6, Light.MID, Red.NONE, "zebra", 0x3E6A3A, 0x548A46, 0, 0x4A6A3A, 0x4A5A3A),
    ALOE(Habitat.DRY, "aloe_vera", Form.ROSETTE, 14, 6, Light.HIGH, Red.NONE, "spiky", 0x7E9E6C, 0x96B07E, 0, 0x6A8A5A, 0x6A7A5A),
    AGAVE(Habitat.DRY, "agave", Form.ROSETTE, 30, 8, Light.VERY_HIGH, Red.NONE, "spiky", 0x6E8E8A, 0x86A49E, 0, 0x5E7E78, 0x5E6E60),
    LITHOPS(Habitat.DRY, "lithops", Form.ROSETTE, 40, 6, Light.HIGH, Red.NONE, "succulent", 0xB2A286, 0xF0D24A, 0, 0x9A8A70, 0x8A7A60),
    SEDUM(Habitat.DRY, "sedum", Form.CARPET, 6, 6, Light.HIGH, Red.RED, "round", 0x7EA44A, 0x96B85A, 0xC4462E, 0x7A8A44, 0x7A8A44),
    BLUE_GRAMA(Habitat.DRY, "blue_grama", Form.ROSETTE, 5, 6, Light.HIGH, Red.NONE, "narrow", 0x8C9E6A, 0x7A6A9A, 0, 0x8A9060, 0x8A9060),
    NEEDLEGRASS(Habitat.DRY, "needlegrass", Form.ROSETTE, 6, 6, Light.HIGH, Red.NONE, "needle", 0xA0A46E, 0xD8CC96, 0, 0x9A9A68, 0x9A9A68);

    /** Growth form: decides the shape ({@link PlantShapes}), where it may be planted and how it spreads. */
    public enum Form {
        EPIPHYTE, MOSS, ROSETTE, STEM, RIBBON, CARPET, BULB,
        /** M20: coral forms (on reef rock): a mat of polyps, mushroom discs, a leather toadstool, branches, a dome, an anemone. */
        POLYPS, DISC, TOADSTOOL, BRANCH, DOME, ANEMONE,
        /** M56: cacti: a barrel, a column with arms, a chain of pads or a cluster of small balls (terrarium plants only). */
        CACTUS;

        /** Needs substrate (the interior floor with a substrate, sand, gravel, dirt ...). */
        public boolean rooted() {
            return this == ROSETTE || this == STEM || this == RIBBON || this == CARPET || this == BULB || this == CACTUS;
        }

        /** M20: a coral (or the anemone): glued onto reef rock, lives in salt water. */
        public boolean coral() {
            return this == POLYPS || this == DISC || this == TOADSTOOL || this == BRANCH || this == DOME || this == ANEMONE;
        }
    }

    /**
     * M56: where a plant lives: under water (the aquarium plants and corals) or on the land of a terrarium or paludarium,
     * with the climate it wants there ({@code TankClimate}: relative humidity in %, air temperature in degrees C).
     */
    public enum Habitat {
        AQUATIC(0, 100, 18, 32),
        /** The rainforest terrarium and the land of a paludarium: humid and warm. */
        TROPICAL(65, 100, 20, 30),
        /** The swamp and the shore of a paludarium: wet, a little cooler; the carnivorous plants want strong light. */
        SWAMP(55, 100, 15, 30),
        /** The desert terrarium: dry and hot; too much humidity rots them. */
        DRY(10, 55, 20, 38);

        public final double humidityMin, humidityMax, tempMin, tempMax;

        Habitat(double humidityMin, double humidityMax, double tempMin, double tempMax) {
            this.humidityMin = humidityMin;
            this.humidityMax = humidityMax;
            this.tempMin = tempMin;
            this.tempMax = tempMax;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** M56: how a terrarium plant is propagated with the shears: a cutting, an offset (pup) or a divided clump. */
    public enum Propagation { CUTTING, OFFSET, DIVISION }

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

        /** The need that counts now: without features.equipment.ledTiers the strong-light plants are content with the bar like before. */
        public static Light need(Light l) {
            return l == VERY_HIGH && !Features.enabled(Feature.LED_TIERS) ? HIGH : l;
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

    private final Habitat habitat;
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
        this(Habitat.AQUATIC, id, form, daysToFull, stages, light, red, leafTexture, leaf, top, redColor, stem, rhizome);
    }

    PlantSpecies(Habitat habitat, String id, Form form, double daysToFull, int stages, Light light, Red red, String leafTexture,
                 int leaf, int top, int redColor, int stem, int rhizome) {
        this.habitat = habitat;
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
     * coral). Config {@code features.visuals.plantMeshes} (pack) and {@code render.plantMeshes} (each player).
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
        // M56: the terrarium plants: offsets around the cacti, succulents and bromeliads, runners of the sedges and the grama,
        // the mosses creep on (the stem plants and carpets of the land like those under water)
        if (land()) return form == Form.STEM || form == Form.CARPET || form == Form.MOSS || propagation() == Propagation.OFFSET
                || this == SWAMP_SEDGE || this == SOFT_RUSH || this == BLUE_GRAMA;
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
     * M28: grows well only with CO2 ({@code features.co2.enabled}): without dosed CO2 at {@link #NO_CO2_GROWTH} of its speed (the
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

    /** M28: wants soft, slightly acidic water ({@code features.waterChemistry.enabled}): slower in hard or alkaline water. */
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
        return ordinal() > BUBBLE_TIP_ANEMONE.ordinal() && !land(); // M56: the terrarium plants are in no config list
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

    // ------------------------------------------------------------------ M56: terrarium plants

    /** M56: where it lives (aquatic for the aquarium plants and corals). */
    public Habitat habitat() { return habitat; }

    /** M56: a terrarium plant: grows on the land of a terrarium or paludarium (not under water). */
    public boolean land() {
        return habitat != Habitat.AQUATIC;
    }

    /** M56: the terrarium plants in their order. */
    public static java.util.List<PlantSpecies> landSpecies() {
        java.util.List<PlantSpecies> out = new java.util.ArrayList<>();
        for (PlantSpecies p : values()) if (p.land()) out.add(p);
        return out;
    }

    /** M56: the bromeliads: their leaves form a cup that holds water (dart frogs raise their tadpoles there). */
    public boolean bromeliad() {
        return this == NEOREGELIA || this == GUZMANIA || this == VRIESEA;
    }

    /** M56: how the shears propagate it: offsets of the cacti, succulents and bromeliads, divided mosses, else cuttings. */
    public Propagation propagation() {
        if (!land()) return Propagation.CUTTING;
        if (form == Form.CACTUS || bromeliad()) return Propagation.OFFSET;
        return switch (this) {
            case TILLANDSIA, ECHEVERIA, HAWORTHIA, ALOE, AGAVE, LITHOPS -> Propagation.OFFSET;
            case CUSHION_MOSS, REINDEER_LICHEN, SELAGINELLA -> Propagation.DIVISION;
            default -> Propagation.CUTTING;
        };
    }

    /** M56: lowest relative humidity (%) at which it grows fully (it slows down below and stops 20 % lower). */
    public double humidityMin() {
        return switch (this) {
            case REINDEER_LICHEN -> 40; // a lichen: dries out and waits
            case BUTTERWORT, SWAMP_SEDGE, SOFT_RUSH -> 50;
            case TILLANDSIA -> 45; // air plants take their water from the air and the mist
            default -> habitat.humidityMin;
        };
    }

    /** M56: highest relative humidity (%) it grows fully at (above, a dry plant rots; it stops 20 % higher). */
    public double humidityMax() {
        return switch (this) {
            case LITHOPS -> 45;
            case BLUE_GRAMA, NEEDLEGRASS -> 70; // grasses of the savanna
            default -> habitat.humidityMax;
        };
    }

    /** M56: lowest air temperature (degrees C) it grows fully at (half down to 3 K lower, none below). */
    public double tempMin() {
        return switch (this) {
            case SARRACENIA, VENUS_FLYTRAP, REINDEER_LICHEN -> 10; // temperate bogs and heaths
            case NEPENTHES -> 16; // highland pitcher plants like it cool
            default -> habitat.tempMin;
        };
    }

    /** M56: highest air temperature (degrees C) it grows fully at (half up to 3 K higher, none above). */
    public double tempMax() {
        return switch (this) {
            case NEPENTHES -> 28;
            case REINDEER_LICHEN -> 26;
            default -> habitat.tempMax;
        };
    }

    /** M56: tortoises eat it (the opuntia pads, stonecrop and the grasses). */
    public boolean edible() {
        return this == PRICKLY_PEAR || this == SEDUM || this == BLUE_GRAMA || this == NEEDLEGRASS;
    }

    /** M56: snakes and geckos hide in it once it is grown (the dense and leafy terrarium plants). */
    public boolean shelter() {
        if (!land()) return false;
        return switch (this) {
            case NEOREGELIA, GUZMANIA, VRIESEA, BOSTON_FERN, BIRDS_NEST_FERN, BEGONIA, POTHOS, PEPEROMIA, FITTONIA, SARRACENIA, NEPENTHES,
                 SWAMP_SEDGE, SOFT_RUSH, PAPYRUS, UMBRELLA_SEDGE, ALOE, AGAVE, BLUE_GRAMA, NEEDLEGRASS, PRICKLY_PEAR -> true;
            default -> false;
        };
    }

    /** M56: how much it adds to the humidity of the land it covers (the cacti and succulents keep their water). */
    public double transpiration() {
        return habitat == Habitat.DRY ? 0.3 : 1.0;
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
