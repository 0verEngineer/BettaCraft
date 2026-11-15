package org.overengineer.bettacraft.sources;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M28.1: every animal of the mod (bettas, every tankmate and seahorse, every small animal, the dwarf clawed frog), also
 * the ones later milestones add, can be found in the wild: it spawns in the world or the aquarium net or the fishing rod
 * brings it. The wandering trader or breeding alone is not enough. Deliberate exceptions stand in {@link #EXCEPTIONS}
 * with their reason.
 */
class AnimalSourcesTest {
    private static final Set<Source.Kind> WILD = EnumSet.of(Source.Kind.SPAWN, Source.Kind.NET, Source.Kind.FISHING, Source.Kind.BIG_NET,
            Source.Kind.STEEL_LEADER); // M30: the large net and the steel leader catch big animals

    /** Animals without a wild source on purpose (target key, reason). Keep it short. */
    static final Map<String, String> EXCEPTIONS = Map.of();

    /** Every animal target of the mod. */
    static Map<String, String> animals() {
        Map<String, String> out = new LinkedHashMap<>();
        out.put(Target.BETTA, "betta");
        // M29 part c: the wild species of the splendens complex
        for (var s : org.overengineer.bettacraft.genetics.BettaSpecies.values()) out.put(Target.betta(s), s.latin());
        out.put(Target.FROG, "dwarf clawed frog");
        for (TankmateSpecies s : TankmateSpecies.values()) out.put(Target.tankmate(s), s.id());
        for (CritterSpecies s : CritterSpecies.values()) out.put(Target.critter(s), s.id());
        // M31 part b: the turtles (their bucket or box is the target)
        for (var s : org.overengineer.bettacraft.terrarium.HerpSpecies.ALL) out.put(Target.item(s.containerId()), s.id());
        return out;
    }

    @Test
    void everyAnimalHasAWildSource() {
        List<String> problems = new ArrayList<>();
        for (var e : animals().entrySet()) {
            if (EXCEPTIONS.containsKey(e.getKey())) continue;
            boolean wild = Sources.of(e.getKey()).stream().anyMatch(s -> WILD.contains(s.kind()));
            if (!wild) problems.add(e.getValue() + ": no wild spawn, no net, no fishing (only " + kinds(e.getKey()) + ")");
        }
        assertTrue(problems.isEmpty(), "animals without a wild source (add one in world/Wildlife, critters/NetCatching or sources/*, "
                + "or put them on the exception list with a reason):\n" + String.join("\n", problems));
    }

    @Test
    void theExceptionsAreStillNeeded() {
        for (String key : EXCEPTIONS.keySet()) {
            assertTrue(animals().containsKey(key), "exception for an unknown animal: " + key);
            assertTrue(Sources.of(key).stream().noneMatch(s -> WILD.contains(s.kind())), key + " has a wild source now: take it off the exception list");
        }
    }

    private static Set<Source.Kind> kinds(String target) {
        Set<Source.Kind> k = EnumSet.noneOf(Source.Kind.class);
        for (Source s : Sources.of(target)) k.add(s.kind());
        return k;
    }
}
