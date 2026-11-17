package org.overengineer.bettacraft.tankmates;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
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
            .eats(0.7).territorial().bettaTarget().dimorphic().labyrinth().egg(0x3A70C0, 0xD04030).native_(Water.SWAMP, 10).trader(Trade.COMMON)
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
            .trader(Trade.RARE).salt(Salinity.Range.HARDY_MARINE)),
    // ---------------------------------------------------------------- M29 part a: cichlids (South America, features.tankmates.cichlids)
    // open brooders: the pair cleans a flat stone (rams) or a broad leaf or the glass (angelfish, discus) and guards eggs and young;
    // cave brooders (Apistogramma): the female spawns in a small cave and guards it, the male defends his territory
    RAM_CICHLID(d("ram_cichlid", "Mikrogeophagus ramirezi", Model.CICHLID, Group.CICHLID, Breeding.OPEN, Zone.LOW)
            .size(0.45).temp(25, 31).breed(27, 30).quality(0.65).shoal(1).stock(0.8).brood(20, 60).growth(1.3)
            .eats(0.35).territorial().territory(3).dimorphic().egg(0xE8A040, 0x3A78C8).native_(Water.RIVER, 6).native_(Water.JUNGLE, 3)
            .trader(Trade.COMMON).varieties("wild", "gold", "electric_blue", "black", "german_blue", "balloon")),
    BOLIVIAN_RAM(d("bolivian_ram", "Mikrogeophagus altispinosus", Model.CICHLID, Group.CICHLID, Breeding.OPEN, Zone.LOW)
            .size(0.62).temp(22, 29).breed(25, 28).quality(0.5).shoal(1).stock(1.0).brood(20, 60).growth(1.5)
            .eats(0.35).territorial().territory(3).egg(0xE8B860, 0xD06030).native_(Water.RIVER, 4).native_(Water.JUNGLE, 2).trader(Trade.RARE)),
    COCKATOO_CICHLID(d("cockatoo_cichlid", "Apistogramma cacatuoides", Model.APISTO, Group.CICHLID, Breeding.CICHLID_CAVE, Zone.LOW)
            .size(0.6).temp(22, 29).breed(24, 28).quality(0.5).shoal(1).stock(0.8).brood(15, 50).growth(1.4)
            .eats(0.35).territorial().territory(4).dimorphic().egg(0xE8C030, 0xE04020).native_(Water.JUNGLE, 5).native_(Water.RIVER, 2)
            .trader(Trade.COMMON).varieties("wild", "double_red", "orange_flash")),
    AGASSIZ_DWARF_CICHLID(d("agassiz_dwarf_cichlid", "Apistogramma agassizii", Model.APISTO, Group.CICHLID, Breeding.CICHLID_CAVE, Zone.LOW)
            .size(0.55).temp(24, 29).breed(26, 29).quality(0.65).shoal(1).stock(0.7).brood(15, 45).growth(1.4)
            .eats(0.35).territorial().territory(4).dimorphic().egg(0x4A90D0, 0xE04030).native_(Water.JUNGLE, 4).trader(Trade.COMMON)
            .varieties("wild", "fire_red")),
    ANGELFISH(d("angelfish", "Pterophyllum scalare", Model.ANGEL, Group.CICHLID, Breeding.OPEN, Zone.MID)
            .size(0.9).hitbox(0.5, 0.6).temp(24, 30).breed(26, 29).quality(0.5).shoal(1).stock(2.0).brood(30, 80).growth(2.0)
            .eats(0.8).prey(0.25).egg(0xC8C4B0, 0x202020).native_(Water.JUNGLE, 4).trader(Trade.COMMON)
            .varieties("wild", "black", "marble", "koi", "platinum")),
    DISCUS(d("discus", "Symphysodon aequifasciatus", Model.DISCUS, Group.CICHLID, Breeding.OPEN, Zone.MID)
            .size(0.95).hitbox(0.55, 0.55).temp(27, 32).breed(28, 31).quality(0.75).shoal(3).stock(2.5).brood(20, 60).growth(2.5)
            .eats(0.4).parentsFeed().egg(0x8A5030, 0x40A0C0).native_(Water.JUNGLE, 2).trader(Trade.RARE)
            .varieties("wild", "red_turquoise", "pigeon_blood", "blue_diamond")),
    // ---------------------------------------------------------------- M29 part b: puffers (features.tankmates.puffers)
    // they crack snails (bladder and ramshorn snails first) and need them or hard food, else their teeth overgrow; they nip the
    // fins of long-finned fish; the dwarf puffers scatter their eggs into moss, the green spotted puffer lives in brackish water
    DWARF_PUFFER(d("dwarf_puffer", "Carinotetraodon travancoricus", Model.PUFFER, Group.PUFFER, Breeding.SCATTER, Zone.MID)
            .size(0.22).temp(22, 29).breed(25, 28).quality(0.55).shoal(1).stock(0.4).brood(5, 15).growth(0.9)
            .eats(0.6).snails().finNipper().dimorphic().egg(0xC8C050, 0x3A4A28).native_(Water.RIVER, 5).native_(Water.SWAMP, 3)
            .trader(Trade.COMMON)),
    RED_EYE_PUFFER(d("red_eye_puffer", "Carinotetraodon irrubesco", Model.PUFFER, Group.PUFFER, Breeding.SCATTER, Zone.MID)
            .size(0.3).temp(23, 29).breed(25, 28).quality(0.6).shoal(1).stock(0.5).brood(5, 12).growth(1.0)
            .eats(0.6).snails().finNipper().dimorphic().egg(0x8A6A48, 0xD03A28).native_(Water.SWAMP, 3).native_(Water.JUNGLE, 2)
            .trader(Trade.RARE)),
    GREEN_SPOTTED_PUFFER(d("green_spotted_puffer", "Tetraodon nigroviridis", Model.PUFFER, Group.PUFFER, Breeding.NONE, Zone.MID)
            .size(0.8).hitbox(0.5, 0.45).temp(24, 29).breed(26, 28).quality(0.55).shoal(1).stock(2.5).brood(0, 0).growth(2.0)
            .eats(0.8).snails().finNipper().prey(0.25).egg(0xB8D030, 0x1A1A14).native_(Water.MANGROVE, 6).trader(Trade.RARE)
            .salt(Salinity.Range.BRACKISH_ONLY)),
    // ---------------------------------------------------------------- M29 part b: barbs (egg scatterers, features.tankmates.barbsAndLoaches)
    TIGER_BARB(d("tiger_barb", "Puntigrus tetrazona", Model.BARB, Group.BARB, Breeding.SCATTER, Zone.MID)
            .size(0.5).temp(20, 28).breed(24, 27).quality(0.45).shoal(6).stock(0.6).brood(30, 80).growth(1.0)
            .eats(0.6).finNipper().egg(0xE8A040, 0x141414).native_(Water.RIVER, 6).native_(Water.SWAMP, 6).trader(Trade.COMMON)
            .varieties("wild", "green", "albino")),
    ODESSA_BARB(d("odessa_barb", "Pethia padamya", Model.BARB, Group.BARB, Breeding.SCATTER, Zone.MID)
            .size(0.48).temp(18, 26).breed(22, 25).quality(0.45).shoal(6).stock(0.55).brood(30, 80).growth(1.0)
            .eats(0.6).dimorphic().egg(0xC0B898, 0xE03020).native_(Water.RIVER, 6).trader(Trade.COMMON)),
    CHERRY_BARB(d("cherry_barb", "Rohanella titteya", Model.BARB, Group.BARB, Breeding.SCATTER, Zone.MID)
            .size(0.4).temp(22, 28).breed(24, 27).quality(0.5).shoal(6).stock(0.4).brood(20, 50).growth(1.0)
            .eats(0.5).dimorphic().egg(0xD84A3A, 0x3A2018).native_(Water.JUNGLE, 6).native_(Water.RIVER, 3).trader(Trade.COMMON)),
    // ---------------------------------------------------------------- M29 part b: loaches and the garra (features.tankmates.barbsAndLoaches)
    DWARF_CHAIN_LOACH(d("dwarf_chain_loach", "Ambastaia sidthimunki", Model.LOACH, Group.LOACH, Breeding.NONE, Zone.LOW)
            .size(0.42).temp(24, 29).breed(26, 28).quality(0.55).shoal(6).stock(0.4).brood(0, 0).growth(1.2)
            .eats(0.2).snails().egg(0xE8E0C0, 0x2A2420).native_(Water.RIVER, 4).trader(Trade.RARE)),
    RETICULATED_LOACH(d("reticulated_loach", "Botia lohachata", Model.LOACH, Group.LOACH, Breeding.NONE, Zone.LOW)
            .size(0.75).hitbox(0.5, 0.3).temp(24, 30).breed(26, 28).quality(0.5).shoal(5).stock(1.5).brood(0, 0).growth(2.0)
            .eats(0.3).snails().egg(0xD8D0B8, 0x2A2428).native_(Water.RIVER, 4).native_(Water.SWAMP, 2).trader(Trade.RARE)),
    GARRA_RUFA(d("garra_rufa", "Garra rufa", Model.GARRA, Group.LOACH, Breeding.SCATTER, Zone.BOTTOM)
            .size(0.7).hitbox(0.45, 0.22).temp(18, 32).breed(24, 28).quality(0.55).shoal(5).stock(1.0).brood(20, 60).growth(1.4)
            .eats(0.3).algae(0.6).egg(0x8A8070, 0x2A2420).native_(Water.RIVER, 6).trader(Trade.COMMON)),
    // ---------------------------------------------------------------- M29 part c: danios (egg scatterers, features.tankmates.communityFish)
    ZEBRA_DANIO(d("zebra_danio", "Danio rerio", Model.RASBORA, Group.RASBORA, Breeding.SCATTER, Zone.UPPER)
            .size(0.4).temp(18, 27).breed(24, 27).quality(0.35).shoal(6).stock(0.4).brood(30, 80).growth(0.8)
            .eats(0.7).egg(0x3A5AC8, 0xE8D8A0).native_(Water.RIVER, 10).native_(Water.SWAMP, 5).trader(Trade.COMMON)
            .varieties("wild", "leopard", "golden")),
    CELESTIAL_PEARL_DANIO(d("celestial_pearl_danio", "Celestichthys margaritatus", Model.RASBORA, Group.RASBORA, Breeding.SCATTER, Zone.LOW)
            .size(0.17).temp(20, 26).breed(22, 25).quality(0.6).shoal(6).stock(0.15).brood(10, 30).growth(0.8)
            .eats(0.4).dimorphic().egg(0x2A3A6A, 0xF08030).native_(Water.SWAMP, 8).trader(Trade.COMMON)),
    // ---------------------------------------------------------------- M29 part c: rainbowfish (eggs into moss, features.tankmates.communityFish)
    FORKTAIL_RAINBOW(d("forktail_rainbow", "Pseudomugil furcatus", Model.RAINBOW, Group.RAINBOW, Breeding.SCATTER, Zone.UPPER)
            .size(0.4).temp(23, 28).breed(25, 27).quality(0.55).shoal(6).stock(0.4).brood(10, 30).growth(1.0)
            .eats(0.5).dimorphic().egg(0x8AC0D0, 0xF0D020).native_(Water.JUNGLE, 6).native_(Water.RIVER, 3).trader(Trade.COMMON)),
    NEON_DWARF_RAINBOW(d("neon_dwarf_rainbow", "Melanotaenia praecox", Model.RAINBOW, Group.RAINBOW, Breeding.SCATTER, Zone.MID)
            .size(0.5).temp(23, 28).breed(25, 27).quality(0.5).shoal(6).stock(0.6).brood(15, 40).growth(1.2)
            .eats(0.5).dimorphic().egg(0x5AA0E8, 0xE03030).native_(Water.JUNGLE, 6).native_(Water.RIVER, 4).trader(Trade.COMMON)),
    // ---------------------------------------------------------------- M29 part c: gouramis (labyrinth fish, features.tankmates.gouramis)
    // the honey and the pearl gourami build foam nests like the dwarf gourami; the chocolate gourami female broods the eggs in her mouth
    HONEY_GOURAMI(d("honey_gourami", "Trichogaster chuna", Model.GOURAMI, Group.GOURAMI, Breeding.FOAM_NEST, Zone.UPPER)
            .size(0.4).temp(22, 28).breed(25, 28).quality(0.5).shoal(1).stock(0.6).brood(20, 60).growth(0.9)
            .eats(0.6).dimorphic().labyrinth().egg(0xE8A040, 0x2A2420).native_(Water.SWAMP, 6).native_(Water.RIVER, 3).trader(Trade.COMMON)
            .varieties("wild", "gold", "red")),
    CHOCOLATE_GOURAMI(d("chocolate_gourami", "Sphaerichthys osphromenoides", Model.GOURAMI, Group.GOURAMI, Breeding.MOUTH, Zone.MID)
            .size(0.45).temp(25, 30).breed(27, 29).quality(0.75).shoal(4).stock(0.5).brood(10, 25).growth(1.2)
            .eats(0.3).labyrinth().egg(0x5A3A28, 0xE8D8A8).native_(Water.SWAMP, 3).native_(Water.JUNGLE, 3).trader(Trade.RARE)),
    PEARL_GOURAMI(d("pearl_gourami", "Trichopodus leerii", Model.GOURAMI, Group.GOURAMI, Breeding.FOAM_NEST, Zone.UPPER)
            .size(0.85).temp(24, 30).breed(27, 29).quality(0.5).shoal(1).stock(1.6).brood(30, 80).growth(1.6)
            .eats(0.6).territorial().bettaTarget().dimorphic().labyrinth().egg(0xC8B8A0, 0xE85030).native_(Water.SWAMP, 5).native_(Water.JUNGLE, 3)
            .trader(Trade.COMMON)),
    // ---------------------------------------------------------------- M29 part c: tetras (egg scatterers, features.tankmates.communityFish)
    EMPEROR_TETRA(d("emperor_tetra", "Nematobrycon palmeri", Model.TETRA, Group.TETRA, Breeding.SCATTER, Zone.MID)
            .size(0.42).temp(23, 27).breed(24, 27).quality(0.55).shoal(6).stock(0.45).brood(20, 50).growth(1.1)
            .eats(0.5).dimorphic().egg(0x6A6AA0, 0x141414).native_(Water.RIVER, 4).native_(Water.JUNGLE, 4).trader(Trade.COMMON)),
    RUMMY_NOSE_TETRA(d("rummy_nose_tetra", "Hemigrammus rhodostomus", Model.TETRA, Group.TETRA, Breeding.SCATTER, Zone.MID)
            .size(0.4).temp(24, 29).breed(26, 28).quality(0.7).shoal(8).stock(0.4).brood(15, 40).growth(1.0)
            .eats(0.4).egg(0xD8D0C0, 0xE02020).native_(Water.JUNGLE, 12).trader(Trade.COMMON)),
    BLACK_PHANTOM_TETRA(d("black_phantom_tetra", "Hyphessobrycon megalopterus", Model.TETRA, Group.TETRA, Breeding.SCATTER, Zone.MID)
            .size(0.37).temp(22, 28).breed(24, 27).quality(0.5).shoal(6).stock(0.4).brood(20, 60).growth(1.0)
            .eats(0.5).dimorphic().egg(0x6A6A68, 0x141414).native_(Water.JUNGLE, 8).native_(Water.RIVER, 3).trader(Trade.COMMON)),
    // ---------------------------------------------------------------- M29 part c: livebearers (features.tankmates.communityFish)
    // simple variety genetics like the platy (not the guppy genome); the molly also lives in brackish water and mangroves
    BLACK_MOLLY(d("black_molly", "Poecilia sphenops", Model.PLATY, Group.LIVEBEARER, Breeding.LIVEBEARER, Zone.MID)
            .size(0.6).temp(22, 30).breed(24, 28).quality(0.45).shoal(3).stock(0.8).brood(10, 40).growth(0.9)
            .eats(0.6).algae(0.2).dimorphic().egg(0x1A1A1E, 0xE8C040).native_(Water.MANGROVE, 5).native_(Water.RIVER, 4).trader(Trade.COMMON)
            .salt(Salinity.Range.BRACKISH).varieties("wild", "dalmatian", "gold")),
    SWORDTAIL(d("swordtail", "Xiphophorus hellerii", Model.SWORDTAIL, Group.LIVEBEARER, Breeding.LIVEBEARER, Zone.MID)
            .size(0.6).temp(22, 28).breed(23, 27).quality(0.4).shoal(3).stock(0.8).brood(20, 60).growth(0.9)
            .eats(0.6).dimorphic().egg(0x7A9A50, 0xE04030).native_(Water.RIVER, 8).trader(Trade.COMMON).salt(Salinity.Range.BRACKISH)
            .varieties("wild", "red", "red_wagtail")),
    // ---------------------------------------------------------------- M30 part a: big animals (features.bigTanks.bigAnimals, LargeSpecies)
    // the iridescent shark catfish of the Mekong: a restless, silvery schooling giant of big rivers; in a home tank about 1 m,
    // eats everything and, once big, small fish; never breeds in a tank
    PANGASIUS(d("pangasius", "Pangasianodon hypophthalmus", Model.PANGASIUS, Group.CATFISH, Breeding.NONE, Zone.MID)
            .large(LargeSpecies.of(1.0).turn(3.0).speed(0.6).eatsSmallFish(0.6)).temp(22, 30).breed(26, 28).quality(0.4).shoal(1)
            .brood(0, 0).growth(4.0).eats(0.5).egg(0xA8B0B8, 0x3A4250).native_(Water.RIVER, 3).native_(Water.JUNGLE, 2)),
    // ---------------------------------------------------------------- M30 part b: the other freshwater giants
    // the redtail catfish of the Amazon and the Orinoco: a broad, flat head with long barbels, a dark back, a white belly and a
    // bright red tail; 1.2 m in big show tanks (wild 1.8 m), swallows every fish that fits its mouth; rests a lot by day;
    // never spawns in a tank
    REDTAIL_CATFISH(d("redtail_catfish", "Phractocephalus hemioliopterus", Model.REDTAIL, Group.CATFISH, Breeding.NONE, Zone.LOW)
            .large(LargeSpecies.of(1.2).turn(2.5).speed(0.45).diet(org.overengineer.bettacraft.food.FoodDiet.Diet.CARNIVORE)
                    .eatsSmallFish(1.0).rests(0.2)).temp(20, 28).breed(24, 27).quality(0.4).shoal(1).brood(0, 0).growth(5.0).eats(0.6)
            .nocturnal().egg(0x34363C, 0xD8402A).native_(Water.JUNGLE, 2)),
    // the wels (European catfish) of the deep, slow rivers and lakes of Europe: a long, scaleless body that tapers into a very
    // long anal fin, a tiny dorsal fin, two long barbels on the upper jaw and four short ones below; marbled olive and brown;
    // up to 2.7 m wild, 1.5 m in the game; cool water (4-26 C), a night hunter that lies on the ground by day; spawns in a
    // nest in shallow, warm water in spring and never in a tank
    WELS_CATFISH(d("wels_catfish", "Silurus glanis", Model.WELS, Group.CATFISH, Breeding.NONE, Zone.LOW)
            .large(LargeSpecies.of(1.5).turn(2.2).speed(0.4).diet(org.overengineer.bettacraft.food.FoodDiet.Diet.CARNIVORE)
                    .eatsSmallFish(1.0).rests(0.25)).temp(4, 26).breed(20, 24).quality(0.35).shoal(1).brood(0, 0).growth(6.0).eats(0.6)
            .nocturnal().egg(0x4A4A36, 0xC8BC98).native_(Water.RIVER, 1)),
    // the arapaima (pirarucu) of the Amazon floodplains: one of the largest freshwater fish (wild up to 3 m), 2 m in the game;
    // a long, round body, the dorsal and anal fins far back next to the tail, scales turning red towards the rear; an
    // obligate air breather that comes up to the surface for air every few minutes (low oxygen does not trouble it); eats fish;
    // breeds only in big ponds (the male guards the young in a nest pit), never in a tank
    ARAPAIMA(d("arapaima", "Arapaima gigas", Model.ARAPAIMA, Group.BONYTONGUE, Breeding.NONE, Zone.MID)
            .large(LargeSpecies.of(2.0).turn(2.0).speed(0.5).diet(org.overengineer.bettacraft.food.FoodDiet.Diet.CARNIVORE)
                    .eatsSmallFish(1.0)).temp(24, 31).breed(27, 30).quality(0.45).shoal(1).brood(0, 0).growth(6.0).eats(0.6)
            .labyrinth().airGulper().egg(0x4E5A48, 0xC8402C).native_(Water.JUNGLE, 1)),
    // the motoro (ocellate river stingray) of the Amazon and Parana basins: a round, flat disc with orange, black-ringed
    // eye spots and a tail with a venomous spine; disc up to 45 cm, 0.6 m with the tail; glides over the sand and lies half
    // buried on it; eats worms, shrimp and small bottom fish; soft, clean, warm water; livebearing: after about three months
    // the female gives birth to 2-5 fully formed pups (they need the room of an adult)
    MOTORO_STINGRAY(d("motoro_stingray", "Potamotrygon motoro", Model.STINGRAY, Group.RAY, Breeding.LIVEBEARER, Zone.BOTTOM)
            .large(LargeSpecies.of(0.6).turn(3.5).speed(0.35).diet(org.overengineer.bettacraft.food.FoodDiet.Diet.CARNIVORE)
                    .eatsSmallFish(0.4).swim(LargeSpecies.Swim.RAY_BOTTOM).rests(0.4)).temp(24, 30).breed(26, 29).quality(0.65).shoal(1)
            .brood(2, 5).growth(4.0).eats(0.4).egg(0x6A5034, 0xE8902C).native_(Water.JUNGLE, 2)),
    // ---------------------------------------------------------------- M30 part c: the marine big animals
    // the brownbanded bamboo shark of the Indo-West Pacific reefs and lagoons: a slender bottom shark (up to 1 m) with barbels
    // and two dorsal fins far back; lies on the ground by day under ledges and hunts crabs, shrimp and small fish at night;
    // lays its eggs in leathery egg cases (two at a time) that hatch after about four months
    BAMBOO_SHARK(d("bamboo_shark", "Chiloscyllium punctatum", Model.BAMBOO_SHARK, Group.SHARK, Breeding.EGG_CASE, Zone.BOTTOM)
            .large(LargeSpecies.of(1.0).turn(3.5).speed(0.4).diet(org.overengineer.bettacraft.food.FoodDiet.Diet.CARNIVORE)
                    .eatsSmallFish(0.35).rests(0.35)).temp(22, 28).breed(24, 27).quality(0.5).shoal(1).brood(1, 2).growth(5.0).eats(0.3)
            .nocturnal().egg(0x7A6448, 0xE6DCC6).native_(Water.WARM_OCEAN, 2).native_(Water.LUKEWARM_OCEAN, 1).salt(Salinity.Range.MARINE)),
    // the blacktip reef shark of the shallow Indo-Pacific reefs: the classic requiem shark with black fin tips, 1.6 m (wild up
    // to 1.8 m); it has to keep swimming to breathe (ram ventilation), never rests, hunts reef fish; livebearing, but practically
    // never breeds in a tank
    BLACKTIP_REEF_SHARK(d("blacktip_reef_shark", "Carcharhinus melanopterus", Model.REEF_SHARK, Group.SHARK, Breeding.NONE, Zone.MID)
            .large(LargeSpecies.of(1.6).turn(3.5).speed(0.65).diet(org.overengineer.bettacraft.food.FoodDiet.Diet.CARNIVORE)
                    .eatsSmallFish(1.0).keepsSwimming()).temp(24, 30).breed(26, 29).quality(0.55).shoal(1).brood(0, 0).growth(6.0).eats(0.6)
            .egg(0x8A8880, 0x16161A).native_(Water.WARM_OCEAN, 1).salt(Salinity.Range.MARINE)),
    // the spotted eagle ray (Indo-Pacific: Aetobatus ocellatus, long called A. narinari): a dark, white-spotted ray with pointed
    // wings, a duck-bill snout and a whip tail that flies through open water with slow wing beats; digs for clams, snails and
    // shrimp; 1.5 m long in the game (disc up to 3 m across in the wild); does not breed in a tank
    SPOTTED_EAGLE_RAY(d("spotted_eagle_ray", "Aetobatus ocellatus", Model.EAGLE_RAY, Group.RAY, Breeding.NONE, Zone.MID)
            .large(LargeSpecies.of(1.5).turn(2.5).speed(0.55).diet(org.overengineer.bettacraft.food.FoodDiet.Diet.CARNIVORE)
                    .eatsSmallFish(0.15).swim(LargeSpecies.Swim.RAY_OPEN)).temp(24, 29).breed(26, 28).quality(0.6).shoal(1).brood(0, 0)
            .growth(6.0).eats(0.2).snails().egg(0x1E2434, 0xE8ECF0).native_(Water.WARM_OCEAN, 1).salt(Salinity.Range.MARINE)),
    // the cownose ray of the western Atlantic: a brown ray with a notched "cow nose" head that migrates in big schools through
    // warm and temperate seas, a favourite of touch pools; 1 m (disc about 0.9 m across); eats clams, snails and shrimp;
    // livebearing: one big pup after almost a year
    COWNOSE_RAY(d("cownose_ray", "Rhinoptera bonasus", Model.COWNOSE_RAY, Group.RAY, Breeding.LIVEBEARER, Zone.MID)
            .large(LargeSpecies.of(1.0).turn(3.0).speed(0.5).diet(org.overengineer.bettacraft.food.FoodDiet.Diet.CARNIVORE)
                    .eatsSmallFish(0.15).swim(LargeSpecies.Swim.RAY_OPEN)).temp(18, 28).breed(23, 27).quality(0.55).shoal(3).brood(1, 1)
            .growth(5.0).eats(0.2).snails().egg(0x6E5236, 0xF2EEE4).native_(Water.LUKEWARM_OCEAN, 2).native_(Water.WARM_OCEAN, 1)
            .salt(Salinity.Range.MARINE)),
    // the honeycomb (laced) moray of the Indo-Pacific reefs: a big moray eel with dark spots in a pale network; it lives in a
    // crevice of the reef, mostly with only its head out, opening and closing its mouth to breathe, and comes out at night or
    // for food; 1.2 m in the game (wild up to 1.8 m); swallows fish, shrimp and crabs; never breeds in a tank
    HONEYCOMB_MORAY(d("honeycomb_moray", "Gymnothorax favagineus", Model.MORAY, Group.MORAY, Breeding.NONE, Zone.LOW)
            .large(LargeSpecies.of(1.2).turn(6.0).speed(0.4).diet(org.overengineer.bettacraft.food.FoodDiet.Diet.CARNIVORE)
                    .eatsSmallFish(0.8).swim(LargeSpecies.Swim.LURK)).temp(24, 28).breed(25, 27).quality(0.45).shoal(1).brood(0, 0)
            .growth(6.0).eats(0.5).nocturnal().egg(0x2A2016, 0xE6D890).native_(Water.WARM_OCEAN, 2).salt(Salinity.Range.MARINE));

    /** Body plan of the renderer (and of {@code tools/assetgen/tankmates.py}). */
    public enum Model { TETRA, RASBORA, CORY, PLECO, OTO, KUHLI, LOACH, LIVEBEARER, PLATY, GOURAMI, CLOWN, TANG, GOBY, DAMSEL, SEAHORSE,
        /** M29: the dwarf cichlids (rams), Apistogramma with its crest, the tall angelfish, the round discus. */
        CICHLID, APISTO, ANGEL, DISCUS,
        /** M29 part b: the round puffers, the deep-bodied barbs, the flat garra with its sucker mouth. */
        PUFFER, BARB, GARRA,
        /** M29 part c: the rainbowfish with two dorsal fins, the swordtail with the male's sword. */
        RAINBOW, SWORDTAIL,
        /** M30 part a: the pangasius (long, torpedo-shaped, a high sickle-shaped dorsal fin, a deeply forked tail). */
        PANGASIUS,
        /**
         * M30 part b: the redtail catfish (broad flat head, long barbels), the wels (long body, endless anal fin), the arapaima
         * (long round body, dorsal and anal fin at the tail), the stingray (flat round disc with wave segments, thin tail).
         */
        REDTAIL, WELS, ARAPAIMA, STINGRAY,
        /**
         * M30 part c: the bamboo shark (slender bottom shark, two dorsals far back, long low tail), the reef shark (torpedo body,
         * tall dorsal, sickle pectorals, heterocercal tail), the eagle and the cownose ray (wing-like discs with wave segments,
         * a long tail), the moray (an eel in nested segments with a jaw that opens).
         */
        BAMBOO_SHARK, REEF_SHARK, EAGLE_RAY, COWNOSE_RAY, MORAY }

    /** Family group (handbook, overlays, menu). */
    public enum Group { TETRA, RASBORA, CORY, PLECO, OTOCINCLUS, LOACH, LIVEBEARER, GOURAMI, CLOWNFISH, TANG, GOBY, DAMSEL, SEAHORSE, CICHLID,
        /** M29 part b. */
        PUFFER, BARB,
        /** M29 part c. */
        RAINBOW,
        /** M30: the giant catfish (pangasius; part b: the redtail catfish and the wels). */
        CATFISH,
        /** M30 part b: the bonytongues (arapaima). */
        BONYTONGUE,
        /** M30 part b: the rays (motoro stingray; part c: eagle ray, cownose ray). */
        RAY,
        /** M30 part c: the sharks (bamboo shark, blacktip reef shark) and the moray eels. */
        SHARK, MORAY }

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
        /**
         * M29: open brooders (rams, angelfish, discus): the pair cleans a flat stone (rams) or a broad leaf or the glass
         * (angelfish, discus), both guard the eggs and the young and drive other fish away; without the parents the brood dies.
         */
        OPEN,
        /**
         * M29: cave brooders (Apistogramma): every female claims a small cave (coconut shell, slate cave, spawning cave),
         * spawns in it with the male and guards eggs and young there; the male defends his territory.
         */
        CICHLID_CAVE,
        /**
         * M29 part c: a mouthbrooder (chocolate gourami): after spawning the female carries the eggs in her mouth and spits
         * out free-swimming young after the gestation days of the livebearers.
         */
        MOUTH,
        /** Practically never breeds in a tank (clown loach, the other reef fish). */
        NONE,
        /**
         * M30 part c: an egg-laying shark (bamboo shark): the female lays leathery egg cases (mermaid's purses) near the
         * decoration; each holds one pup that hatches after many days, only when the tank has room for it by the size rule.
         */
        EGG_CASE
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
    /** M23/M29: a labyrinth fish breathes air at the surface (the dwarf gourami; part c: more gouramis). */
    public boolean labyrinth() { return def.labyrinth; }
    /** M29: adults eat tankmates up to this length in blocks and shrimp (angelfish; 0 = no). M30: big ones by their data. */
    public double preyLength() { return def.large != null ? def.large.preyLength() : def.prey; }
    /** M30: the data of a big species (size rule, movement, catching), null for the small fish. */
    @Nullable
    public LargeSpecies large() { return def.large; }
    /** M30: a big animal (giant catfish, shark, ray). */
    public boolean isLarge() { return def.large != null; }
    /** M29: water blocks a territorial adult male needs; more males than the tank has room for fight (0 = no territory). */
    public double territory() { return def.territory; }
    /** M29: the parents feed the young with their skin secretion (discus): the brood stays with them longer and dies without them. */
    public boolean parentsFeed() { return def.parentsFeed; }
    /** M29 part b: nips the fins of long-finned fish (puffers, tiger barbs; {@code tankmates.behaviour.finNipping}). */
    public boolean finNipper() { return def.finNipper; }
    /**
     * M29 part b: long, flowing fins the fin nippers go for: bettas (in their own code), angelfish, gouramis and the males of
     * the guppies.
     */
    public boolean longFins(boolean female) {
        return this == ANGELFISH || def.group == Group.GOURAMI || this == GUPPY && !female;
    }
    /** M29 part b: a puffer (eats snails, its teeth overgrow without snails or hard food). */
    public boolean puffer() { return def.group == Group.PUFFER; }
    /** M29: a cichlid (open and cave brooders guarding their brood). */
    public boolean cichlid() { return def.group == Group.CICHLID; }
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

    /**
     * M33: the feature switch of the species (its group of species, or the tankmates as a whole); off: neither the trader,
     * the net nor the wild brings new ones.
     */
    public Feature feature() {
        if (isLarge()) return Feature.BIG_ANIMALS; // M30
        if (seahorse()) return Feature.SEAHORSES;
        if (marine()) return Feature.REEF_FISH;
        if (cichlid()) return Feature.CICHLIDS;
        if (puffer()) return Feature.PUFFERS;
        if (barbOrNewLoach()) return Feature.BARBS_AND_LOACHES;
        if (newGourami()) return Feature.GOURAMIS;
        if (communityFish()) return Feature.COMMUNITY_FISH;
        return this == GREEN_NEON_TETRA ? Feature.GREEN_NEON : Feature.TANKMATES;
    }

    /** M21.5: new ones come from the trader and the net ({@link #feature} on, with its group). */
    public boolean available() {
        return Features.enabled(feature());
    }

    /** The feature switch of the wild ones (handbook, sources): the species' own one, the wild spawns for the plain tankmates. */
    public String featureKey() {
        Feature f = feature();
        return (f == Feature.TANKMATES ? Feature.TANKMATES_WILD : f).key();
    }

    /** M29 part b: the barbs and the loaches that came with them (switched with features.tankmates.barbsAndLoaches). */
    private boolean barbOrNewLoach() {
        return def.group == Group.BARB || this == DWARF_CHAIN_LOACH || this == RETICULATED_LOACH || this == GARRA_RUFA;
    }

    /** M29 part c: the honey, chocolate and pearl gourami (switched with features.tankmates.gouramis). */
    private boolean newGourami() {
        return this == HONEY_GOURAMI || this == CHOCOLATE_GOURAMI || this == PEARL_GOURAMI;
    }

    /** M29 part c: danios, rainbowfish, tetras, molly and swordtail (switched with features.tankmates.communityFish). */
    private boolean communityFish() {
        return switch (this) {
            case ZEBRA_DANIO, CELESTIAL_PEARL_DANIO, FORKTAIL_RAINBOW, NEON_DWARF_RAINBOW, EMPEROR_TETRA, RUMMY_NOSE_TETRA, BLACK_PHANTOM_TETRA,
                 BLACK_MOLLY, SWORDTAIL -> true;
            default -> false;
        };
    }

    /** M29 part c: a mouthbrooder (the female carries the eggs, no breeding box). */
    public boolean mouthbrooder() {
        return def.breeding == Breeding.MOUTH;
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

    /** M30: the kind of water of the species for a water of the net (null: none of the waters of the species). */
    @Nullable
    public static Water waterOf(org.overengineer.bettacraft.critters.NetCatching.Water w) {
        return switch (w) {
            case RIVER -> Water.RIVER;
            case SWAMP -> Water.SWAMP;
            case JUNGLE -> Water.JUNGLE;
            case MANGROVE -> Water.MANGROVE;
            case WARM_OCEAN -> Water.WARM_OCEAN;
            case LUKEWARM_OCEAN -> Water.LUKEWARM_OCEAN;
            default -> null;
        };
    }

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
        boolean territorial, bettaTarget, snails, airGulper, nocturnal, broadLeaves, dimorphic, labyrinth, parentsFeed, finNipper;
        double prey, territory;
        List<String> varieties = List.of("wild");
        final List<Water> waters = new java.util.ArrayList<>();
        final List<Double> weights = new java.util.ArrayList<>();
        Trade trade = Trade.NONE;
        Salinity.Range salt = Salinity.Range.FRESH;
        @Nullable
        LargeSpecies large;

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
                    case CICHLID -> 0.5;
                    case APISTO -> 0.38;
                    case ANGEL, DISCUS -> 0.8;
                    case PUFFER -> 0.55; // M29 part b
                    case BARB -> 0.48;
                    case GARRA -> 0.25;
                    case RAINBOW -> 0.42; // M29 part c

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

        /**
         * M30: a big species: the length from its real length ({@link LargeSpecies#BLOCKS_PER_METRE}), a hitbox from it,
         * the stocking weight from its water need, the movement speed; no trader.
         */
        Def large(LargeSpecies l) {
            this.large = l;
            this.length = l.lengthBlocks();
            this.width = (float) Math.min(1.2, Math.max(0.5, length * 0.4));
            this.height = (float) Math.min(0.9, Math.max(0.3, length * (model == Model.PANGASIUS ? 0.3 : 0.25)));
            if (l.ray()) {
                // M30 part b: a flat disc about as wide as half its length with the tail, thin
                this.width = (float) Math.min(1.6, Math.max(0.4, length * 0.55));
                this.height = (float) Math.min(0.4, Math.max(0.12, length * 0.13));
            }
            if (model == Model.BAMBOO_SHARK || model == Model.REEF_SHARK) {
                // M30 part c: a shark is slim for its length
                this.width = (float) Math.max(0.4, length * 0.2);
                this.height = (float) Math.max(0.3, length * 0.18);
            }
            if (model == Model.MORAY) {
                // M30 part c: an eel: long, narrow and not very tall
                this.width = (float) Math.max(0.25, length * 0.11);
                this.height = (float) Math.max(0.25, length * 0.15);
            }
            this.stock = l.stock();
            this.trade = Trade.NONE;
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
        Def labyrinth() { labyrinth = true; return this; }
        Def prey(double maxLength) { prey = maxLength; return this; }
        Def territory(double waterBlocks) { territory = waterBlocks; return this; }
        Def parentsFeed() { parentsFeed = true; return this; }
        Def finNipper() { finNipper = true; return this; }
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
