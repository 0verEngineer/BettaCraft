package org.overengineer.bettacraft.fabric.dev;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.gametest.framework.TestFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M52: the GameTests on Fabric: the shared GameTest classes of the NeoForge dev source set that need nothing else of
 * NeoForge (list: build task fabricGameTestList) are registered with vanilla's registry. Fabric's GameTest module wants
 * each class as an entry point with a public constructor; the dev mixins let it run ours as they are (static methods,
 * private constructors). {@code -Pgametests=<prefix>} filters like on NeoForge.
 */
public final class FabricGameTests implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/gametest");
    public static final Set<Class<?>> CLASSES = new HashSet<>();

    @Override
    public void onInitialize() {
        if (System.getProperty("fabric-api.gametest") == null) return;
        try (var in = FabricGameTests.class.getResourceAsStream("/bettacraft_fabric_gametests.txt")) {
            if (in == null) throw new IllegalStateException("bettacraft_fabric_gametests.txt is missing");
            List<String> names = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines().map(String::trim)
                    .filter(s -> !s.isEmpty()).toList();
            for (String n : names) {
                Class<?> c = Class.forName(n);
                CLASSES.add(c);
                register(c);
            }
            LOGGER.info("[gametest] Fabric: {} GameTest classes, {} tests", CLASSES.size(), GameTestRegistry.getAllTestFunctions().size());
        } catch (Exception e) {
            throw new IllegalStateException("cannot register the GameTests", e);
        }
        String only = System.getProperty("bettacraft.gametest.only", "");
        if (!only.isBlank()) ServerLifecycleEvents.SERVER_STARTING.register(server -> filter(server, only));
    }

    /**
     * Like vanilla's {@code GameTestRegistry.register(Class)}, but our classes have private constructors and private
     * helpers: every method is made accessible first, the generators are called here.
     */
    @SuppressWarnings("unchecked")
    private static void register(Class<?> c) throws ReflectiveOperationException {
        for (Method m : c.getDeclaredMethods()) {
            m.setAccessible(true);
            if (m.isAnnotationPresent(net.minecraft.gametest.framework.GameTestGenerator.class)) {
                Object target = null;
                if (!Modifier.isStatic(m.getModifiers())) {
                    Constructor<?> ctor = c.getDeclaredConstructor();
                    ctor.setAccessible(true);
                    target = ctor.newInstance();
                }
                GameTestRegistry.getAllTestFunctions().addAll((Collection<TestFunction>) m.invoke(target));
                GameTestRegistry.getAllTestClassNames().add(c.getSimpleName());
            } else if (m.isAnnotationPresent(net.minecraft.gametest.framework.GameTest.class)
                    || m.isAnnotationPresent(net.minecraft.gametest.framework.BeforeBatch.class)
                    || m.isAnnotationPresent(net.minecraft.gametest.framework.AfterBatch.class)) {
                GameTestRegistry.register(m);
            }
        }
    }

    /** The invoker of a test method of ours (Fabric's needs a public constructor). */
    public static Consumer<GameTestHelper> invoker(Method method) {
        return h -> {
            try {
                method.setAccessible(true);
                Object target = null;
                if (!Modifier.isStatic(method.getModifiers())) {
                    Constructor<?> c = method.getDeclaringClass().getDeclaredConstructor();
                    c.setAccessible(true);
                    target = c.newInstance();
                }
                method.invoke(target, h);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException r) throw r;
                if (e.getCause() instanceof Error err) throw err;
                throw new RuntimeException(e.getCause());
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("cannot run " + method, e);
            }
        };
    }

    private static void filter(net.minecraft.server.MinecraftServer s, String only) {
        if (!(s instanceof GameTestServer server)) return;
        List<String> prefixes = Arrays.stream(only.toLowerCase(Locale.ROOT).split(",")).map(String::trim).filter(p -> !p.isEmpty()).toList();
        try {
            java.lang.reflect.Field f = GameTestServer.class.getDeclaredField("testFunctions");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            Collection<TestFunction> tests = (Collection<TestFunction>) f.get(server);
            int before = tests.size();
            tests.removeIf(t -> prefixes.stream().noneMatch(p -> t.testName().toLowerCase(Locale.ROOT).startsWith(p)
                    || t.testName().toLowerCase(Locale.ROOT).contains("." + p) || t.batchName().toLowerCase(Locale.ROOT).startsWith(p)));
            LOGGER.info("[gametest] -Pgametests={}: {} of {} tests", only, tests.size(), before);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot filter the GameTests", e);
        }
    }
}
