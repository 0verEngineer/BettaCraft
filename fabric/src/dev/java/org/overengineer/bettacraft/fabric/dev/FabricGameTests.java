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
    /** The names of our tests (libraries of other mods bring GameTests of their own, which do not run here). */
    private static final Set<String> OURS = new HashSet<>();

    @Override
    public void onInitialize() {
        if (System.getProperty("fabric-api.gametest") == null) return;
        try (var in = FabricGameTests.class.getResourceAsStream("/bettacraft_fabric_gametests.txt")) {
            if (in == null) throw new IllegalStateException("bettacraft_fabric_gametests.txt is missing");
            List<String> names = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines().map(String::trim)
                    .filter(s -> !s.isEmpty()).toList();
            Set<String> before = new HashSet<>();
            for (TestFunction f : GameTestRegistry.getAllTestFunctions()) before.add(f.testName());
            for (String n : names) {
                Class<?> c = Class.forName(n);
                CLASSES.add(c);
                register(c);
            }
            for (TestFunction f : GameTestRegistry.getAllTestFunctions()) if (!before.contains(f.testName())) OURS.add(f.testName());
            LOGGER.info("[gametest] Fabric: {} GameTest classes, {} tests", CLASSES.size(), OURS.size());
        } catch (Exception e) {
            throw new IllegalStateException("cannot register the GameTests", e);
        }
        String only = System.getProperty("bettacraft.gametest.only", "");
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            filter(server, only);
            logFailures();
        });
    }

    /**
     * M52: Fabric's GameTest module puts its JUnit reporter (report.xml) in the place of vanilla's log reporter, so the log
     * named a failed test without its reason, and the next run overwrote the file. The log reporter now runs beside it.
     */
    private static void logFailures() {
        try {
            java.lang.reflect.Field f = net.minecraft.gametest.framework.GlobalTestReporter.class.getDeclaredField("DELEGATE");
            f.setAccessible(true);
            Object inner = f.get(null);
            if (!(inner instanceof net.minecraft.gametest.framework.TestReporter r) || inner instanceof net.minecraft.gametest.framework.LogTestReporter
                    || inner instanceof AlsoLog) return;
            net.minecraft.gametest.framework.GlobalTestReporter.replaceWith(new AlsoLog(r));
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("[gametest] cannot add the log reporter: {}", e.toString());
        }
    }

    /** Vanilla's log line of a failed test ("... failed at ...! reason"), then the reporter that was there. */
    private record AlsoLog(net.minecraft.gametest.framework.TestReporter inner) implements net.minecraft.gametest.framework.TestReporter {
        private static final net.minecraft.gametest.framework.LogTestReporter LOG = new net.minecraft.gametest.framework.LogTestReporter();

        @Override
        public void onTestFailed(net.minecraft.gametest.framework.GameTestInfo info) {
            LOG.onTestFailed(info);
            inner.onTestFailed(info);
        }

        @Override
        public void onTestSuccess(net.minecraft.gametest.framework.GameTestInfo info) {
            inner.onTestSuccess(info);
        }

        @Override
        public void finish() {
            inner.finish();
        }
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
            int foreign = tests.size();
            tests.removeIf(t -> !OURS.contains(t.testName()));
            if (foreign != tests.size()) LOGGER.info("[gametest] {} GameTests of other mods left out", foreign - tests.size());
            if (!prefixes.isEmpty()) {
                int before = tests.size();
                tests.removeIf(t -> prefixes.stream().noneMatch(p -> t.testName().toLowerCase(Locale.ROOT).startsWith(p)
                        || t.testName().toLowerCase(Locale.ROOT).contains("." + p) || t.batchName().toLowerCase(Locale.ROOT).startsWith(p)));
                LOGGER.info("[gametest] -Pgametests={}: {} of {} tests", only, tests.size(), before);
            }
            // M63: -Pgametestrepeat=<n>
            org.overengineer.bettacraft.gametest.GameTestRepeat.repeat(tests, Integer.getInteger("bettacraft.gametest.repeat", 1));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot filter the GameTests", e);
        }
    }
}
