package org.overengineer.bettacraft.guide;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * M22.2: the handbook matches the game data. The generated sections of every page (profile tables, "Obtaining" with
 * every source and chance, the species tables, "Where to find what", the advancements) must be exactly what
 * {@link GuideGen} makes from the game's tables, in English and German. {@code gradlew updateGuide} rewrites them.
 */
class GuideGenTest {
    @Test
    void bookMatchesGameData() throws IOException {
        boolean write = Boolean.getBoolean("bettacraft.updateGuide");
        List<String> problems = new ArrayList<>();
        problems.addAll(GuideGen.apply(false, write));
        problems.addAll(GuideGen.apply(true, write));
        assertTrue(problems.isEmpty(), "handbook and game data differ (run gradlew updateGuide):\n" + String.join("\n", problems));
    }
}
