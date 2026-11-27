package org.overengineer.bettacraft.data;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Moves the saved data of a world from before 26.1 to the places 26.1 reads it from, before any of it is loaded.
 * <p>
 * Up to 1.21.11 our saved data lay in the data folder of its dimension as {@code bettacraft_<x>.dat} (the overworld's in
 * {@code data/}, the Nether's in {@code DIM-1/data/}, the End's in {@code DIM1/data/}, every other dimension's in
 * {@code dimensions/<ns>/<path>/data/}). Since 26.1 a saved data is identified by a namespaced id and lies in
 * {@code dimensions/<ns>/<path>/data/bettacraft/<x>.dat} ({@code NbtCompat.savedData}). Minecraft's world upgrade (the
 * {@code FileFixerUpper} of 26.1) moves only vanilla's own files, so ours stayed behind: the world then started with an
 * empty interior region list, every tank got a new empty interior and the old one (substrate, plants, decor) was no longer
 * reached, while the animals (kept in the tank's own data) were still there.
 * <p>
 * Runs when the server is about to start (no level, no saved data loaded yet). A file whose new place is taken is left
 * where it is (a world opened by a version without this move already made a new one there) and reported.
 */
public final class LegacySavedDataFiles {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/upgrade");
    static final String PREFIX = "bettacraft_";

    private LegacySavedDataFiles() {
    }

    /** What happened to the old files: moved ones and the ones left because their new place was taken. */
    public record Result(List<Path> moved, List<Path> kept) {
    }

    public static Result onServerAboutToStart(MinecraftServer server) {
        Path root = server.getWorldPath(LevelResource.ROOT);
        Result r = new Result(new ArrayList<>(), new ArrayList<>());
        try {
            r = moveAll(root);
        } catch (Exception e) {
            LOGGER.error("Moving the saved data of an older Minecraft version to its new place failed", e);
        }
        if (!r.moved().isEmpty()) LOGGER.info("World from an older Minecraft version: moved {} saved data file(s) of Bettacraft to the new places: {}",
                r.moved().size(), r.moved().stream().map(p -> root.relativize(p).toString()).toList());
        for (Path p : r.kept()) LOGGER.warn("Old saved data file {} not moved: its new place is already taken (the world was opened by an older "
                + "Bettacraft on this Minecraft version); left untouched", root.relativize(p));
        return r;
    }

    /** Moves every old saved data file of ours below {@code root} (also for tests on a copy). */
    public static Result moveAll(Path root) throws IOException {
        Result r = new Result(new ArrayList<>(), new ArrayList<>());
        // the folders of DimensionType.getStorageFolder since 26.1 (dimensions/<ns>/<path>; plain paths: no game class is
        // needed, so the unit test runs without a game)
        Path dims = root.resolve("dimensions");
        moveFolder(root.resolve("data"), dims.resolve("minecraft").resolve("overworld").resolve("data"), r);
        moveFolder(root.resolve("DIM-1").resolve("data"), dims.resolve("minecraft").resolve("the_nether").resolve("data"), r);
        moveFolder(root.resolve("DIM1").resolve("data"), dims.resolve("minecraft").resolve("the_end").resolve("data"), r);
        // every other dimension kept its folder (dimensions/<ns>/<path>); only the name of the file changed
        if (Files.isDirectory(dims)) {
            try (DirectoryStream<Path> namespaces = Files.newDirectoryStream(dims, Files::isDirectory)) {
                for (Path ns : namespaces) {
                    try (DirectoryStream<Path> paths = Files.newDirectoryStream(ns, Files::isDirectory)) {
                        for (Path dim : paths) moveFolder(dim.resolve("data"), dim.resolve("data"), r);
                    }
                }
            }
        }
        return r;
    }

    private static void moveFolder(Path from, Path toData, Result r) throws IOException {
        if (!Files.isDirectory(from)) return;
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> s = Files.newDirectoryStream(from, PREFIX + "*.dat")) {
            for (Path p : s) if (Files.isRegularFile(p)) files.add(p);
        }
        for (Path p : files) {
            String name = p.getFileName().toString();
            Path target = toData.resolve("bettacraft").resolve(name.substring(PREFIX.length()));
            if (Files.exists(target)) {
                r.kept().add(p);
                continue;
            }
            Files.createDirectories(target.getParent());
            Files.move(p, target);
            r.moved().add(target);
        }
    }
}
