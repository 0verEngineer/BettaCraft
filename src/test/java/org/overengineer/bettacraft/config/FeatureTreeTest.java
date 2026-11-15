package org.overengineer.bettacraft.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * M33: the shape of the feature tree (unique paths, groups, parents in the same group or a group switch, ids in the
 * config), the parent/child rule ({@link Features#resolve}) and the content mapping ({@link FeatureContent}).
 */
class FeatureTreeTest {
    @Test
    void pathsAndParents() {
        Set<String> paths = new HashSet<>();
        Set<String> old = new HashSet<>();
        for (Feature f : Feature.values()) {
            assertTrue(paths.add(f.path()), "path twice: " + f.path());
            assertTrue(f.path().matches("[a-z][A-Za-z0-9]*(\\.[a-z][A-Za-z0-9]*)+"), "bad path " + f.path());
            for (String o : f.oldKeys()) {
                if (!o.equals("wildFish") && !o.equals("wildPlants")) assertTrue(old.add(o), "old key twice: " + o); // the split ones
            }
            assertNotNull(Feature.byPath(f.path()));
            assertFalse(f.label().isBlank());
            assertFalse(f.tooltip().isBlank());
            String section = f.path().substring(0, f.path().lastIndexOf('.'));
            assertTrue(Feature.SECTIONS.containsKey(section), "section without a label: " + section);
            if (f.parent() != null) {
                assertTrue(f.parent().isGroup() || f.parent().parent() != null, f + ": parent " + f.parent() + " is neither a group nor below one");
                assertTrue(f.parent().ordinal() < f.ordinal(), "parent after child: " + f);
                assertFalse(f.isGroup(), "a group has no parent: " + f);
            }
            // a sub-switch of a group section has the group as (grand)parent
            String groupPath = section + ".enabled";
            Feature group = Feature.byPath(groupPath);
            if (group != null && group != f) assertTrue(FeatureContent.implies(f, group), f + " is not below " + group);
        }
    }

    @Test
    void parentAndChild() {
        Set<Feature> off = EnumSet.noneOf(Feature.class);
        assertTrue(Features.resolve(Feature.CICHLIDS, x -> !off.contains(x)));
        off.add(Feature.TANKMATES);
        assertFalse(Features.resolve(Feature.CICHLIDS, x -> !off.contains(x)), "child off with its group");
        assertFalse(Features.resolve(Feature.SEAHORSES, x -> !off.contains(x)), "seahorses need the tankmates");
        assertTrue(Features.resolve(Feature.SALTWATER, x -> !off.contains(x)), "the group of salt water is still on");
        off.clear();
        off.add(Feature.BREEDING);
        assertFalse(Features.resolve(Feature.HERP_BREEDING, x -> !off.contains(x)), "herp breeding needs breeding");
        assertTrue(Features.resolve(Feature.HERPS, x -> !off.contains(x)));
        off.clear();
        off.add(Feature.TERRARIUMS);
        assertFalse(Features.resolve(Feature.WILD_HERPS, x -> !off.contains(x)), "two levels: terrariums > herps > wild herps");
        assertFalse(Features.resolve(Feature.WILD_TURTLES, x -> !off.contains(x)), "turtles need terrariums");
        off.clear();
        off.add(Feature.WILD_HERPS);
        assertTrue(Features.resolve(Feature.HERPS, x -> !off.contains(x)), "a child does not switch off its parent");
        // before the config is loaded everything is on
        for (Feature f : Feature.values()) assertTrue(Features.enabled(f), f + " default");
    }

    @Test
    void content() throws Exception {
        assertEquals(List.of(), FeatureContent.ofItem("tank"));
        assertEquals(List.of(Feature.CICHLIDS), FeatureContent.ofItem("discus_bucket"));
        assertEquals(List.of(Feature.REEF_FISH), FeatureContent.ofItem("yellow_tang_spawn_egg"));
        assertEquals(List.of(Feature.TANKMATES), FeatureContent.ofItem("neon_tetra_bucket"));
        assertEquals(List.of(Feature.CORALS), FeatureContent.ofItem("acropora"));
        assertEquals(List.of(Feature.CORALS), FeatureContent.ofItem("wild_acropora"));
        assertEquals(List.of(Feature.AQUATIC_PLANTS), FeatureContent.ofItem("java_moss"));
        assertEquals(List.of(Feature.FLOATING_PLANTS), FeatureContent.ofItem("duckweed"));
        assertEquals(List.of(Feature.TURTLES), FeatureContent.ofItem("musk_turtle_bucket"));
        assertEquals(List.of(Feature.HERPS), FeatureContent.ofItem("corn_snake_box"));
        assertEquals(Set.of(Feature.BREEDING, Feature.DECOR), Set.copyOf(FeatureContent.ofItem("decor_spawning_cave")));
        assertEquals(Set.of(Feature.LIVE_FOOD, Feature.FOOD_MACHINE), Set.copyOf(FeatureContent.ofItem("frozen_daphnia")));
        assertEquals(List.of(Feature.INTERIOR_PIPES), FeatureContent.ofItem("bulkhead"), "pipes is implied");
        assertEquals(Set.of(Feature.STAINLESS_STEEL), Set.copyOf(FeatureContent.ofRecipe("stainless_steel_ingot_from_crucible", "stainless_steel_ingot",
                "bettacraft:crucible")));
        assertEquals(List.of(Feature.CORALS), FeatureContent.ofRecipe("frag_from_brain_coral", null, "minecraft:crafting"));
        assertEquals(List.of(Feature.STONE_CABINETS), FeatureContent.ofRecipe("stone_cabinet", "cabinet", "minecraft:crafting"));
        assertEquals(List.of(Feature.WILD_BETTAS), FeatureContent.ofTarget("betta:imbellis"));
        assertEquals(List.of(Feature.FROGS), FeatureContent.ofTarget("frog"));
        assertTrue(FeatureContent.ofTarget("critter:cleaner_shrimp").contains(Feature.SALTWATER));
        assertEquals(List.of(Feature.CARIDINA), FeatureContent.ofTarget("critter:caridina"));
        // every item of the mod maps without an error; animals, plants and devices are never "core" by accident
        Path models = Path.of(System.getProperty("bettacraft.projectDir", "."), "src/generated/resources/assets/bettacraft/models/item");
        Set<String> core = new TreeSet<>();
        try (Stream<Path> s = Files.list(models)) {
            for (Path p : s.toList()) {
                String id = p.getFileName().toString().replace(".json", "");
                if (FeatureContent.ofItem(id).isEmpty()) core.add(id);
            }
        }
        for (String id : core) {
            assertFalse(id.endsWith("_bucket") && !id.equals("betta_bucket") && !id.equals("critter_bucket") && !id.equals("frog_bucket")
                    && !id.equals("sea_water_bucket") && !id.equals("ro_water_bucket"), "animal bucket without a feature: " + id);
            assertFalse(id.endsWith("_spawn_egg"), "spawn egg without a feature: " + id);
        }
        assertEquals(new TreeSet<>(List.of("algae_wafer", "betta_pellets", "cabinet", "cooler_large", "cooler_medium", "cooler_small",
                "external_filter", "feeder", "fish_bag", "fish_bag_filled", "flow_pump", "heater_large", "heater_medium", "heater_small",
                "internal_filter", "mirror_stick", "swamp_water_bottle", "tank", "tank_lid", "tank_light", "tank_net")), core,
                "the core items (always there)");
    }
}
