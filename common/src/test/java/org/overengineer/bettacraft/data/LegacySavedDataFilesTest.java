package org.overengineer.bettacraft.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The saved data of a world from before 26.1 (after Minecraft's own world upgrade) moves to the places 26.1 reads. */
class LegacySavedDataFilesTest {
    @TempDir
    Path root;

    private Path file(String rel, String content) throws Exception {
        Path p = root.resolve(rel);
        Files.createDirectories(p.getParent());
        Files.writeString(p, content);
        return p;
    }

    @Test
    void movesEveryDimensionsFilesToTheNamespacedPlace() throws Exception {
        file("data/bettacraft_interior_regions.dat", "regions");
        file("data/bettacraft_tank_history.dat", "history");
        file("data/raids.dat", "vanilla");
        file("data/mezz_config/server-id.txt", "other mod");
        file("DIM-1/data/bettacraft_tank_history.dat", "nether");
        file("DIM1/data/bettacraft_tank_history.dat", "end");
        file("dimensions/bettacraft/tank_interior/data/bettacraft_tank_history.dat", "interior");
        file("dimensions/othermod/deep/data/bettacraft_tank_history.dat", "other dimension");

        LegacySavedDataFiles.Result r = LegacySavedDataFiles.moveAll(root);

        assertEquals(6, r.moved().size());
        assertTrue(r.kept().isEmpty());
        Path ow = root.resolve("dimensions/minecraft/overworld/data/bettacraft");
        assertEquals("regions", Files.readString(ow.resolve("interior_regions.dat")));
        assertEquals("history", Files.readString(ow.resolve("tank_history.dat")));
        assertEquals("nether", Files.readString(root.resolve("dimensions/minecraft/the_nether/data/bettacraft/tank_history.dat")));
        assertEquals("end", Files.readString(root.resolve("dimensions/minecraft/the_end/data/bettacraft/tank_history.dat")));
        assertEquals("interior", Files.readString(root.resolve("dimensions/bettacraft/tank_interior/data/bettacraft/tank_history.dat")));
        assertEquals("other dimension", Files.readString(root.resolve("dimensions/othermod/deep/data/bettacraft/tank_history.dat")));
        assertFalse(Files.exists(root.resolve("data/bettacraft_interior_regions.dat")));
        // not ours: untouched
        assertTrue(Files.exists(root.resolve("data/raids.dat")));
        assertTrue(Files.exists(root.resolve("data/mezz_config/server-id.txt")));
        // a second start finds nothing to do
        assertTrue(LegacySavedDataFiles.moveAll(root).moved().isEmpty());
    }

    @Test
    void aTakenPlaceIsNeverOverwritten() throws Exception {
        Path old = file("data/bettacraft_interior_regions.dat", "old");
        file("dimensions/minecraft/overworld/data/bettacraft/interior_regions.dat", "new");

        LegacySavedDataFiles.Result r = LegacySavedDataFiles.moveAll(root);

        assertTrue(r.moved().isEmpty());
        assertEquals(1, r.kept().size());
        assertEquals("old", Files.readString(old));
        assertEquals("new", Files.readString(root.resolve("dimensions/minecraft/overworld/data/bettacraft/interior_regions.dat")));
    }
}
