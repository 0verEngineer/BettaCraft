package org.overengineer.bettacraft.tankmates;

import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.world.entity.EntityType;
import org.overengineer.bettacraft.tank.Salinity;

/**
 * M19: the tankmate fish. Real entities like the bettas ({@link org.overengineer.bettacraft.entity.tankmate.TankmateEntity},
 * one entity type per species), each with a body plan for the renderer, a size, where it swims, the temperatures and the
 * water it tolerates, how big its shoal must be, how much it counts for the stocking, how it breeds and how fast the
 * young grow, and its colour varieties (one Mendelian locus, see {@link TankmateGenes}).
 * <p>
 * Sizes follow the bettas (an adult betta, 6 cm, is half a block long, so one block is about 12 cm); the plecos and the
 * clown loach are drawn a bit smaller than in nature so they still fit a tank. Temperatures are the tolerable range;
 * outside of it the fish is stressed (health timers like the bettas'), breeding needs the narrower breeding range.
 */
public enum TankmateSpecies {
    // ---------------------------------------------------------------- tetras (egg scatterers, South America)
    NEON_TETRA(d("neon_tetra", "Paracheirodon innesi", Model.TETRA, Group.TETRA, Breeding.SCATTER, Zone.MID)
            .size(0.25).temp(20, 27).breed(22, 25).quality(0.6).shoal(6).stock(0.35).brood(20, 50).growth(1.0)
            .eats(0.5).egg(0x2E6FD0, 0xD03028).native_(Water.JUNGLE, 30).trader(Trade.COMMON)),
    CARDINAL_TETRA(d("cardinal_tetra", "Paracheirodon axelrodi", Model.TETRA, Group.TETRA, Breeding.SCATTER, Zone.MID)
            .size(0.32).temp(24, 30).breed(26, 28).quality(0.65).shoal(6).stock(0.4).brood(20, 50).growth(1.1)
            .eats(0.5).egg(0x2A78E0, 0xE02A20).native_(Water.JUNGLE, 22).trader(Trade.COMMON)),
    // M21.5: the green neon (Paracheirodon simulans), green or blue stripe over almost the whole body, hardly any red
    GREEN_NEON_TETRA(d("green_neon_tetra", "Paracheirodon simulans", Model.TETRA, Group.TETRA, Breeding.SCATTER, Zone.MID)
            .size(0.22).temp(24, 30).breed(26, 29).quality(0.7).shoal(6).stock(0.25).brood(15, 40).growth(0.9)
            .eats(0.4).egg(0x2AD0B8, 0x2A60E0).native_(Water.JUNGLE, 12).trader(Trade.COMMON).varieties("wild", "blue")),
    EMBER_TETRA(d("ember_tetra", "Hyphessobrycon amandae", Model.TETRA, Group.TETRA, Breeding.SCATTER, Zone.MID)
            .size(0.17).temp(22, 29).breed(25, 28).quality(0.55).shoal(6).stock(0.2).brood(15, 40).growth(0.9)
            .eats(0.3).egg(0xF07028, 0xC04018).native_(Water.JUNGLE, 18).trader(Trade.COMMON)),
    GLOWLIGHT_TETRA(d("glowlight_tetra", "Hemigrammus erythrozonus", Model.TETRA, Group.TETRA, Breeding.SCATTER, Zone.MID)
            .size(0.29).temp(23, 28).breed(25, 28).quality(0.55).shoal(6).stock(0.35).brood(20, 50).growth(1.0)
            .eats(0.5).egg(0xB8B4A8, 0xF06A30).native_(Water.JUNGLE, 15).trader(Trade.RARE)),
    // ---------------------------------------------------------------- rasboras (egg scatterers, South-East Asia)
    HARLEQUIN_RASBORA(d("harlequin_rasbora", "Trigonostigma heteromorpha", Model.RASBORA, Group.RASBORA, Breeding.SCATTER, Zone.MID)
            .size(0.37).temp(22, 28).breed(26, 28).quality(0.55).shoal(6).stock(0.45).brood(15, 40).growth(1.0)
            .eats(0.5).broadLeaves().egg(0xE0884A, 0x1A1A24).native_(Water.SWAMP, 25).trader(Trade.COMMON)),
    MOSQUITO_RASBORA(d("mosquito_rasbora", "Boraras brigittae", Model.RASBORA, Group.RASBORA, Breeding.SCATTER, Zone.UPPER)
            .size(0.15).temp(22, 28).breed(25, 28).quality(0.6).shoal(8).stock(0.12).brood(8, 25).growth(0.8)
            .eats(0.2).egg(0xE03A28, 0x2A1A18).native_(Water.SWAMP, 22).trader(Trade.COMMON)),
    // ---------------------------------------------------------------- corydoras (eggs on the glass after a cool water change)
    PYGMY_CORY(d("pygmy_cory", "Corydoras pygmaeus", Model.CORY, Group.CORY, Breeding.GLASS_EGGS, Zone.LOW)
            .size(0.21).temp(22, 27).breed(22, 26).quality(0.55).shoal(8).stock(0.2).brood(10, 25).growth(1.0)
            .eats(0.15).airGulper().egg(0xB8B0A0, 0x2A2A2A).native_(Water.JUNGLE, 14).trader(Trade.COMMON)),
    PANDA_CORY(d("panda_cory", "Corydoras panda", Model.CORY, Group.CORY, Breeding.GLASS_EGGS, Zone.BOTTOM)
            .size(0.37).temp(20, 26).breed(21, 25).quality(0.55).shoal(6).stock(0.45).brood(12, 30).growth(1.2)
            .eats(0.2).airGulper().egg(0xE8DCC8, 0x202020).native_(Water.JUNGLE, 16).trader(Trade.COMMON)),
    STERBAI_CORY(d("sterbai_cory", "Corydoras sterbai", Model.CORY, Group.CORY, Breeding.GLASS_EGGS, Zone.BOTTOM)
            .size(0.5).temp(24, 30).breed(24, 28).quality(0.5).shoal(6).stock(0.6).brood(12, 30).growth(1.3)
            .eats(0.2).airGulper().egg(0x403A36, 0xF0A040).native_(Water.JUNGLE, 14).trader(Trade.COMMON)),
    GREEN_GOLD_CORY(d("green_gold_cory", "Corydoras melanotaenia", Model.CORY, Group.CORY, Breeding.GLASS_EGGS, Zone.BOTTOM)
            .size(0.46).temp(22, 27).breed(23, 26).quality(0.55).shoal(6).stock(0.55).brood(12, 30).growth(1.3)
            .eats(0.2).airGulper().egg(0x5A7A4A, 0xE8A030).native_(Water.RIVER, 6).native_(Water.JUNGLE, 8).trader(Trade.RARE)),
    // ---------------------------------------------------------------- plecos (cave spawners, the male guards)
    LEOPARD_FROG_PLECO(d("leopard_frog_pleco", "Peckoltia compta (L134)", Model.PLECO, Group.PLECO, Breeding.CAVE, Zone.GLASS)
            .size(0.6).temp(25, 30).breed(27, 30).quality(0.55).shoal(1).stock(1.0).brood(8, 20).growth(1.6)
            .eats(0.1).algae(0.3).territorial().egg(0xE8B040, 0x2A2420).native_(Water.JUNGLE, 5).trader(Trade.RARE)),
    ZEBRA_PLECO(d("zebra_pleco", "Hypancistrus zebra (L046)", Model.PLECO, Group.PLECO, Breeding.CAVE, Zone.BOTTOM)
            .size(0.55).temp(26, 31).breed(28, 31).quality(0.65).shoal(1).stock(0.9).brood(5, 14).growth(1.8)
            .eats(0.15).territorial().egg(0xF0F0F0, 0x141414).native_(Water.JUNGLE, 2).trader(Trade.RARE)),
    BRISTLENOSE_PLECO(d("bristlenose_pleco", "Ancistrus sp.", Model.PLECO, Group.PLECO, Breeding.CAVE, Zone.GLASS)
            .size(0.7).temp(21, 28).breed(23, 27).quality(0.4).shoal(1).stock(1.2).brood(15, 40).growth(1.4)
            .eats(0.1).algae(1.2).territorial().egg(0x4A4038, 0xC8B890).native_(Water.JUNGLE, 10).native_(Water.RIVER, 4)
            .trader(Trade.COMMON).varieties("wild", "albino", "super_red")),
    // ---------------------------------------------------------------- otocinclus, loaches
    OTOCINCLUS(d("otocinclus", "Otocinclus vittatus", Model.OTO, Group.OTOCINCLUS, Breeding.GLASS_EGGS, Zone.GLASS)
            .size(0.33).temp(21, 27).breed(22, 26).quality(0.7).shoal(5).stock(0.3).brood(6, 18).growth(1.2)
            .eats(0.05).algae(0.8).egg(0xC8B890, 0x3A3028).native_(Water.JUNGLE, 10).native_(Water.RIVER, 4).trader(Trade.COMMON)),
    KUHLI_LOACH(d("kuhli_loach", "Pangio kuhlii", Model.KUHLI, Group.LOACH, Breeding.SCATTER, Zone.BOTTOM)
            .size(0.6).temp(24, 30).breed(25, 28).quality(0.55).shoal(5).stock(0.4).brood(15, 50).growth(1.4)
            .eats(0.2).nocturnal().egg(0xE8A040, 0x2A1E18).native_(Water.SWAMP, 12).trader(Trade.COMMON)),
    CLOWN_LOACH(d("clown_loach", "Chromobotia macracanthus", Model.LOACH, Group.LOACH, Breeding.NONE, Zone.LOW)
            .size(0.9).hitbox(0.6, 0.35).temp(25, 30).breed(27, 29).quality(0.6).shoal(5).stock(2.5).brood(0, 0).growth(2.5)
            .eats(0.4).snails().egg(0xF08A20, 0x101010).native_(Water.SWAMP, 3).native_(Water.JUNGLE, 3).trader(Trade.RARE)),
    // ---------------------------------------------------------------- livebearers
    GUPPY(d("guppy", "Poecilia reticulata", Model.LIVEBEARER, Group.LIVEBEARER, Breeding.LIVEBEARER, Zone.UPPER)
            .size(0.33).temp(22, 28).breed(23, 28).quality(0.4).shoal(3).stock(0.35).brood(5, 16).growth(0.6)
            .eats(0.6).bettaTarget().dimorphic().egg(0x8A8E70, 0xE84A30).native_(Water.RIVER, 12).trader(Trade.COMMON).salt(Salinity.Range.BRACKISH)
            .varieties("wild", "red", "blue_grass", "green_cobra", "tuxedo")),
    ENDLER_GUPPY(d("endler_guppy", "Poecilia wingei", Model.LIVEBEARER, Group.LIVEBEARER, Breeding.LIVEBEARER, Zone.UPPER)
            .size(0.22).temp(23, 30).breed(24, 29).quality(0.4).shoal(3).stock(0.22).brood(4, 12).growth(0.6)
            .eats(0.5).dimorphic().egg(0x88907A, 0xF08A20).native_(Water.RIVER, 6).native_(Water.JUNGLE, 4).trader(Trade.COMMON).salt(Salinity.Range.BRACKISH)
            .varieties("wild", "tiger", "black_bar")),
    PLATY(d("platy", "Xiphophorus maculatus", Model.PLATY, Group.LIVEBEARER, Breeding.LIVEBEARER, Zone.MID)
            .size(0.42).temp(20, 27).breed(22, 26).quality(0.4).shoal(3).stock(0.5).brood(8, 25).growth(0.7)
            .eats(0.6).dimorphic().egg(0x8A8A60, 0xE85020).native_(Water.RIVER, 10).trader(Trade.COMMON).salt(Salinity.Range.BRACKISH)
            .varieties("wild", "red", "sunset", "mickey_mouse", "blue")),
    // ---------------------------------------------------------------- gourami (foam nest like the bettas)
    DWARF_GOURAMI(d("dwarf_gourami", "Trichogaster lalius", Model.GOURAMI, Group.GOURAMI, Breeding.FOAM_NEST, Zone.UPPER)
            .size(0.55).temp(24, 29).breed(26, 29).quality(0.5).shoal(1).stock(1.0).brood(20, 50).growth(1.0)
            .eats(0.7).territorial().bettaTarget().dimorphic().egg(0x3A70C0, 0xD04030).native_(Water.SWAMP, 10).trader(Trade.COMMON)
            .varieties("wild", "powder_blue", "flame")),
    // ---------------------------------------------------------------- M20 reef fish (salt water)
    // the clownfish pair lives in its host anemone and lays its eggs at the anemone's foot; the male guards them
    OCELLARIS_CLOWNFISH(d("ocellaris_clownfish", "Amphiprion ocellaris", Model.CLOWN, Group.CLOWNFISH, Breeding.ANEMONE, Zone.MID)
            .size(0.45).temp(24, 28).breed(25, 28).quality(0.5).shoal(1).stock(1.0).brood(15, 40).growth(1.6)
            .eats(0.3).egg(0xF07818, 0xF8F8F8).native_(Water.WARM_OCEAN, 12).trader(Trade.COMMON).salt(Salinity.Range.MARINE)
            .varieties("wild", "black")),
    YELLOW_TANG(d("yellow_tang", "Zebrasoma flavescens", Model.TANG, Group.TANG, Breeding.NONE, Zone.MID)
            .size(0.75).hitbox(0.5, 0.45).temp(24, 28).breed(25, 27).quality(0.6).shoal(1).stock(2.5).brood(0, 0).growth(2.0)
            .eats(0.2).algae(1.0).egg(0xF8E020, 0xE8C810).native_(Water.WARM_OCEAN, 5).trader(Trade.RARE).salt(Salinity.Range.MARINE)),
    YELLOW_WATCHMAN_GOBY(d("yellow_watchman_goby", "Cryptocentrus cinctus", Model.GOBY, Group.GOBY, Breeding.NONE, Zone.BOTTOM)
            .size(0.42).temp(22, 28).breed(25, 27).quality(0.5).shoal(1).stock(0.6).brood(0, 0).growth(1.4)
            .eats(0.3).egg(0xF0D040, 0x6A88C8).native_(Water.WARM_OCEAN, 8).native_(Water.LUKEWARM_OCEAN, 6).trader(Trade.COMMON)
            .salt(Salinity.Range.MARINE)),
    GREEN_CHROMIS(d("green_chromis", "Chromis viridis", Model.DAMSEL, Group.DAMSEL, Breeding.NONE, Zone.MID)
            .size(0.38).temp(24, 28).breed(25, 27).quality(0.5).shoal(5).stock(0.5).brood(0, 0).growth(1.2)
            .eats(0.4).egg(0x7AD8B0, 0x48A8C8).native_(Water.WARM_OCEAN, 14).native_(Water.LUKEWARM_OCEAN, 10).trader(Trade.COMMON)
            .salt(Salinity.Range.MARINE)),
    YELLOWTAIL_DAMSEL(d("yellowtail_damsel", "Chrysiptera parasema", Model.DAMSEL, Group.DAMSEL, Breeding.NONE, Zone.LOW)
            .size(0.33).temp(23, 28).breed(25, 27).quality(0.4).shoal(1).stock(0.45).brood(0, 0).growth(1.1)
            .eats(0.5).egg(0x2A5AE0, 0xF0D020).native_(Water.WARM_OCEAN, 10).native_(Water.LUKEWARM_OCEAN, 12).trader(Trade.COMMON)
            .salt(Salinity.Range.HARDY_MARINE)),
    // ---------------------------------------------------------------- M21.8 seahorses (sea water down to strong brackish)
    // slow, upright swimmers that hold on to plants, corals and decoration with their tail; eat live food only (artemia,
    // larvae), often; the female lays her eggs into the male's brood pouch and he gives birth to fry that eat artemia at once
    LINED_SEAHORSE(d("lined_seahorse", "Hippocampus erectus", Model.SEAHORSE, Group.SEAHORSE, Breeding.POUCH, Zone.LOW)
            .size(0.62).hitbox(0.22, 0.58).temp(18, 27).breed(21, 25).quality(0.6).shoal(1).stock(1.5).brood(8, 20).growth(1.8)
            .eats(0.0).egg(0xD8A030, 0xF2E2A8).native_(Water.LUKEWARM_OCEAN, 4).native_(Water.WARM_OCEAN, 3)
            .trader(Trade.RARE).salt(Salinity.Range.HARDY_MARINE).varieties("wild", "yellow", "orange")),
    DWARF_SEAHORSE(d("dwarf_seahorse", "Hippocampus zosterae", Model.SEAHORSE, Group.SEAHORSE, Breeding.POUCH, Zone.LOW)
            .size(0.22).hitbox(0.1, 0.21).temp(21, 28).breed(24, 27).quality(0.6).shoal(1).stock(0.3).brood(3, 10).growth(1.0)
            .eats(0.0).egg(0xC8B070, 0xEADCA8).native_(Water.WARM_OCEAN, 4)
            .trader(Trade.RARE).salt(Salinity.Range.HARDY_MARINE));

    /** Body plan of the renderer (and of {@code tools/assetgen/tankmates.py}). */
    public enum Model { TETRA, RASBORA, CORY, PLECO, OTO, KUHLI, LOACH, LIVEBEARER, PLATY, GOURAMI, CLOWN, TANG, GOBY, DAMSEL, SEAHORSE }

    /** Family group (handbook, overlays, menu). */
    public enum Group { TETRA, RASBORA, CORY, PLECO, OTOCINCLUS, LOACH, LIVEBEARER, GOURAMI, CLOWNFISH, TANG, GOBY, DAMSEL, SEAHORSE }

    public enum Breeding {
        /** The female glues the eggs onto the glass, decoration and leaves; a cool water change sets them off (corydoras, otocinclus). */
        GLASS_EGGS,
        /** In a spawning cave (decoration); the male guards the clutch and the larvae (plecos). */
        CAVE,
        /** Eggs scattered into fine-leaved plants, moss or a spawning mop; the parents eat them (tetras, rasboras, kuhli loaches). */
        SCATTER,
        /** Live young after a few days (guppies, endlers, platys). */
        LIVEBEARER,
        /** The male builds a foam nest at the surface like a betta and guards it (dwarf gourami). */
        FOAM_NEST,
        /** M20: the clownfish pair spawns at the foot of its host anemone; the male guards the eggs. */
        ANEMONE,
        /**
         * M21.8: seahorses: the female lays her eggs into the brood pouch of the male, who carries them and gives birth
         * to live fry ({@code seahorses.pouchDays}).
         */
        POUCH,
        /** Practically never breeds in a tank (clown loach, the other reef fish). */
        NONE
    }

    /** Where in the water column a species swims (fractions of the water height, 0 = bottom). */
    public enum Zone {
        BOTTOM(0.0, 0.12), LOW(0.03, 0.4), MID(0.25, 0.75), UPPER(0.5, 0.92), GLASS(0.0, 0.85);

        public final double lo, hi;

        Zone(double lo, double hi) {
            this.lo = lo;
            this.hi = hi;
        }

        public boolean bottom() {
            return this == BOTTOM || this == GLASS;
        }
    }

    /** Kinds of open water for the net (M14 {@code NetCatching.Water}): rough analogues of the native regions. */
    public enum Water { RIVER, SWAMP, JUNGLE, MANGROVE, WARM_OCEAN, LUKEWARM_OCEAN }

    /** Wandering trader: among the usual offers, among the rare ones, or not at all. */
    public enum Trade { COMMON, RARE, NONE }

    private final Def def;

    TankmateSpecies(Def def) {
        this.def = def;
    }

    public String id() { return def.id; }
    public String latin() { return def.latin; }
    public Model model() { return def.model; }
    public Group group() { return def.group; }
    public Breeding breeding() { return def.breeding; }
    public Zone zone() { return def.zone; }
    /** Rendered length of an adult in blocks. */
    public double length() { return def.length; }
    public float hitboxWidth() { return def.width; }
    public float hitboxHeight() { return def.height; }
    public double tempMin() { return def.tMin; }
    public double tempMax() { return def.tMax; }
    public double breedMin() { return def.bMin; }
    public double breedMax() { return def.bMax; }
    /** Water quality (0..1) below which the species is stressed. */
    public double minQuality() { return def.quality; }
    /** Smallest group that does not stress the fish (1 = it lives alone fine). */
    public int shoal() { return def.shoal; }
    public boolean shoaling() { return def.shoal > 1; }
    /** How much one adult counts for the stocking, in fish (a betta = 1). */
    public double stock() { return def.stock; }
    public int broodMin() { return def.broodMin; }
    public int broodMax() { return def.broodMax; }
    /** Multiplier of the life-stage durations of the bettas (stages.*Days). */
    public double growth() { return def.growth; }
    /** Glass faces of algae one adult grazes per day (like the M14 snails). */
    public double algae() { return def.algae; }
    public boolean grazer() { return def.algae >= 0.5; }
    /** How strongly adults hunt eggs and fry (0..1). */
    public double eatsFry() { return def.eatsFry; }
    public boolean territorial() { return def.territorial; }
    /** Male bettas go for males with long, colourful fins (guppies) and for labyrinth fish rivals (gourami). */
    public boolean bettaTarget() { return def.bettaTarget; }
    public boolean eatsSnails() { return def.snails; }
    /** Corydoras dart up to the surface for a gulp of air now and then. */
    public boolean airGulper() { return def.airGulper; }
    public boolean nocturnal() { return def.nocturnal; }
    /** Harlequin rasboras spawn under broad leaves (crypts, anubias, sword plants) instead of fine leaves. */
    public boolean broadLeafSpawner() { return def.broadLeaves; }
    /** Males and females look different (own textures). */
    public boolean dimorphic() { return def.dimorphic; }
    public int eggColor1() { return def.egg1; }
    public int eggColor2() { return def.egg2; }
    public List<String> varieties() { return def.varieties; }
    public Trade trade() { return def.trade; }
    /** M20: the salinity the species lives in (fresh water, brackish-tolerant livebearers, sea water). */
    public Salinity.Range salinity() { return def.salt; }
    /** M20: a reef fish (salt water). */
    public boolean marine() { return def.salt.marine(); }

    /** M21.5: new species can be switched off (features.*): then neither the trader nor the net brings them. */
    public boolean available() {
        if (seahorse()) return org.overengineer.bettacraft.config.CommonConfig.seahorses();
        return this != GREEN_NEON_TETRA || org.overengineer.bettacraft.config.CommonConfig.greenNeon();
    }

    /** M21.8: a seahorse (hitches, low flow, live food, brood pouch). */
    public boolean seahorse() {
        return def.model == Model.SEAHORSE;
    }

    /** Net catch weight in this kind of water (0 = not native there). */
    public double nativeWeight(Water w) {
        for (int i = 0; i < def.waters.size(); i++) if (def.waters.get(i) == w) return def.weights.get(i);
        return 0;
    }

    public List<Water> waters() { return def.waters; }

    public String entityKey() {
        return "entity.bettacraft." + id();
    }

    public String bucketId() {
        return id() + "_bucket";
    }

    public String spawnEggId() {
        return id() + "_spawn_egg";
    }

    /** Variety id at {@code index} (0 = wild type). */
    public String variety(int index) {
        return def.varieties.get(Math.max(0, Math.min(def.varieties.size() - 1, index)));
    }

    public int varietyIndex(String id) {
        int i = def.varieties.indexOf(id);
        return Math.max(0, i);
    }

    public String varietyKey(String variety) {
        return "variety.bettacraft." + id() + "." + variety;
    }

    /** Texture name of a fish of this species: species, variety (if more than one) and sex (if they look different). */
    public String texture(String variety, boolean female) {
        StringBuilder b = new StringBuilder(id());
        if (def.varieties.size() > 1) b.append('_').append(def.varieties.contains(variety) ? variety : def.varieties.getFirst());
        if (def.dimorphic) b.append(female ? "_female" : "_male");
        return b.toString();
    }

    /** Every texture name (assetgen and the renderer agree on these). */
    public List<String> textures() {
        List<String> out = new java.util.ArrayList<>();
        for (String v : def.varieties) {
            if (def.dimorphic) {
                out.add(texture(v, false));
                out.add(texture(v, true));
            } else {
                out.add(texture(v, false));
            }
        }
        return out;
    }

    /** Whether a temperature is tolerable. */
    public boolean tolerates(double celsius) {
        return celsius >= tempMin() - 1e-4 && celsius <= tempMax() + 1e-4;
    }

    public boolean breedingTemp(double celsius) {
        return celsius >= breedMin() - 1e-4 && celsius <= breedMax() + 1e-4;
    }

    @Nullable
    public static TankmateSpecies byId(String id) {
        for (TankmateSpecies s : values()) if (s.id().equals(id)) return s;
        return null;
    }

    /** The species of an entity type of ours, or null. */
    @Nullable
    public static TankmateSpecies of(EntityType<?> type) {
        return org.overengineer.bettacraft.registry.ModEntities.tankmateSpecies(type);
    }

    public String translationKey() {
        return entityKey();
    }

    public String groupKey() {
        return "tankmate.bettacraft.group." + def.group.name().toLowerCase(Locale.ROOT);
    }

    public String breedingKey() {
        return "tankmate.bettacraft.breeding." + def.breeding.name().toLowerCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------ definition builder

    private static Def d(String id, String latin, Model model, Group group, Breeding breeding, Zone zone) {
        return new Def(id, latin, model, group, breeding, zone);
    }

    private static final class Def {
        final String id, latin;
        final Model model;
        final Group group;
        final Breeding breeding;
        final Zone zone;
        double length = 0.3;
        float width = -1, height = -1;
        double tMin = 22, tMax = 28, bMin = 24, bMax = 27, quality = 0.5, stock = 0.5, growth = 1.0, algae, eatsFry = 0.3;
        int shoal = 1, broodMin = 10, broodMax = 30, egg1 = 0x808080, egg2 = 0x404040;
        boolean territorial, bettaTarget, snails, airGulper, nocturnal, broadLeaves, dimorphic;
        List<String> varieties = List.of("wild");
        final List<Water> waters = new java.util.ArrayList<>();
        final List<Double> weights = new java.util.ArrayList<>();
        Trade trade = Trade.NONE;
        Salinity.Range salt = Salinity.Range.FRESH;

        Def(String id, String latin, Model model, Group group, Breeding breeding, Zone zone) {
            this.id = id;
            this.latin = latin;
            this.model = model;
            this.group = group;
            this.breeding = breeding;
            this.zone = zone;
        }

        Def size(double length) {
            this.length = length;
            if (width < 0) {
                double h = switch (model) {
                    case TANG -> 0.7;
                    case CLOWN, DAMSEL -> 0.5;
                    case GOBY -> 0.3;
                    case SEAHORSE -> 1.6;

                    case CORY -> 0.42;
                    case PLECO -> 0.3;
                    case KUHLI -> 0.18;
                    case GOURAMI, PLATY -> 0.5;
                    case LOACH -> 0.4;
                    default -> 0.36;
                };
                double w = model == Model.KUHLI ? 0.45 : 0.72;
                this.width = (float) Math.max(0.12, Math.min(0.6, length * w));
                this.height = (float) Math.max(0.08, Math.min(0.5, length * h));
            }
            return this;
        }

        Def hitbox(double w, double h) {
            this.width = (float) w;
            this.height = (float) h;
            return this;
        }

        Def temp(double lo, double hi) { tMin = lo; tMax = hi; return this; }
        Def breed(double lo, double hi) { bMin = lo; bMax = hi; return this; }
        Def quality(double q) { quality = q; return this; }
        Def shoal(int n) { shoal = n; return this; }
        Def stock(double s) { stock = s; return this; }
        Def brood(int lo, int hi) { broodMin = lo; broodMax = hi; return this; }
        Def growth(double g) { growth = g; return this; }
        Def algae(double a) { algae = a; return this; }
        Def eats(double e) { eatsFry = e; return this; }
        Def territorial() { territorial = true; return this; }
        Def bettaTarget() { bettaTarget = true; return this; }
        Def snails() { snails = true; return this; }
        Def airGulper() { airGulper = true; return this; }
        Def nocturnal() { nocturnal = true; return this; }
        Def broadLeaves() { broadLeaves = true; return this; }
        Def dimorphic() { dimorphic = true; return this; }
        Def egg(int a, int b) { egg1 = a; egg2 = b; return this; }
        Def varieties(String... v) { varieties = List.of(v); return this; }
        Def trader(Trade t) { trade = t; return this; }
        Def salt(Salinity.Range r) { salt = r; return this; }

        Def native_(Water w, double weight) {
            waters.add(w);
            weights.add(weight);
            return this;
        }
    }
}
