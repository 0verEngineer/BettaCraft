package org.overengineer.bettacraft.gametest.zoo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import org.overengineer.bettacraft.BettaCraft;

/**
 * M42 part b: the optional zoo suite: how the animals and water plants of mods we do <em>not</em> integrate (no profile of
 * ours) behave in our tanks, terraria and paludaria. Registered only with the system property {@code bettacraft.zoo}, which
 * {@code gradlew zooTests} (or {@code runGameTestServer -Pzoo}) sets together with {@code run/zoo-mods} and the dev groups
 * aquatic, biomes and create (their animals without a profile count too). Never in the normal suite.
 * <p>
 * The tests record the behaviour of every such animal and plant in tables ({@code zoo/*.tsv} in the run's game directory) and
 * fail only where <em>our</em> code goes wrong: an exception in our code, an animal lost or doubled while it is taken out,
 * put back, packed or released, a refused animal that is left in the tank anyway, a tank that can be broken with animals in
 * it, a plant that breaks without its item. What the foreign animals themselves do (escape, die, breed) is only written down.
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class ZooSuite {
    static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("bettacraft/zoo");
    public static final boolean ENABLED = Boolean.getBoolean("bettacraft.zoo");
    public static final String EMPTY = "empty_32x10x32";
    /** Namespaces that are never part of the zoo: ours and Minecraft's (they have their own tests). */
    static final Set<String> OWN = Set.of(BettaCraft.MOD_ID, "minecraft");

    static final List<Class<?>> CLASSES = List.of(ZooAnimalTests.class, ZooPlantTests.class);

    private ZooSuite() {}

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        if (!ENABLED) return;
        for (Class<?> c : CLASSES) event.register(c);
        LOGGER.info("[zoo] registered {} zoo test classes", CLASSES.size());
    }

    static String id(ResourceLocation rl) {
        return rl.toString();
    }

    static String entityId(net.minecraft.world.entity.EntityType<?> t) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(t).toString();
    }

    /** Whether the throwable went through our code (not a GameTest helper), which makes it a bug of ours. */
    static boolean ours(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            for (StackTraceElement s : c.getStackTrace()) {
                String cls = s.getClassName();
                if (cls.startsWith("org.overengineer.bettacraft.") && !cls.startsWith("org.overengineer.bettacraft.gametest.")) return true;
            }
        }
        return false;
    }

    static String brief(Throwable t) {
        String m = t.getClass().getSimpleName() + ": " + t.getMessage();
        return m.replace('\t', ' ').replace('\n', ' ');
    }

    /** Writes a table to {@code <game dir>/zoo/<name>.tsv} (replaced on every run). */
    static synchronized void table(String name, List<String> header, List<List<String>> rows) {
        Path dir = FMLPaths.GAMEDIR.get().resolve("zoo");
        List<String> lines = new ArrayList<>();
        lines.add(String.join("\t", header));
        for (List<String> r : rows) lines.add(String.join("\t", r.stream().map(s -> s == null ? "" : s.replace('\t', ' ').replace('\n', ' ')).toList()));
        try {
            Files.createDirectories(dir);
            Files.write(dir.resolve(name + ".tsv"), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("[zoo] cannot write {}", name, e);
        }
        LOGGER.info("[zoo] {}: {} rows -> {}", name, rows.size(), dir.resolve(name + ".tsv"));
    }

    /** Fails with every violation (after the table is written). */
    static void verdict(String what, List<String> violations) {
        if (violations.isEmpty()) return;
        LOGGER.error("[zoo] {}: {} violation(s): {}", what, violations.size(), violations);
        throw new GameTestAssertException(what + ": " + violations.size() + " violation(s) of our rules: " + String.join("; ", violations.subList(0, Math.min(6, violations.size()))));
    }

    static List<String> row(Object... cells) {
        return new ArrayList<>(Arrays.stream(cells).map(o -> o == null ? "" : String.valueOf(o)).toList());
    }

    static String low(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}
