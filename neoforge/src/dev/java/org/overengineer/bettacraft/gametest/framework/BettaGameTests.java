package org.overengineer.bettacraft.gametest.framework;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M52b: the GameTests of both loaders on the data-driven GameTest system of Minecraft 1.21.5+. The test classes keep the
 * annotations of 1.21.1 (ours now, same attributes: {@link GameTest}, {@link GameTestHolder}, {@link PrefixGameTestTemplate},
 * {@link GameTestGenerator}); this class reads them by reflection and gives each test
 * <ul>
 * <li>a test function {@code bettacraft:<class>.<method>} (lower case, the old test name; generated tests keep their
 * name) in the static registry {@code test_function},</li>
 * <li>an empty test environment per batch ({@code bettacraft:batch/<batch>}): Minecraft batches the tests by environment,
 * so the old batches (config-changing tests in their own) stay as they were,</li>
 * <li>a {@link FunctionGameTestInstance} with structure, timeout, setup ticks, required, rotation and attempts.</li>
 * </ul>
 * The classes of the normal suite are listed by the build ({@code bettacraft_gametests.txt}: every class with
 * {@code @GameTestHolder}); the integration and zoo suites add theirs only with their system property.
 * {@code -Pgametests=<prefix>[,...]} (system property {@code bettacraft.gametest.only}) keeps only the tests whose name, method
 * name or batch starts with a prefix; {@code -Pgametestrepeat=<n>} (M63) runs each of them n times side by side. The loaders register: NeoForge in {@code NeoGameTestRegistration} (RegisterEvent,
 * RegisterGameTestsEvent), Fabric in {@code FabricGameTests}.
 */
public final class BettaGameTests {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/gametest");
    public static final String NAMESPACE = "bettacraft";
    public static final String LIST = "/bettacraft_gametests.txt";
    private static final String ONLY = System.getProperty("bettacraft.gametest.only", "");
    /** The optional suites (a class with a public static boolean ENABLED and a static List of classes CLASSES). */
    private static final List<String> SUITES = List.of("org.overengineer.bettacraft.gametest.integration.IntegrationSuite",
            "org.overengineer.bettacraft.gametest.zoo.ZooSuite");

    private static Map<Identifier, Entry> tests;

    /** One test: its id, the lower-case method name (for the filter) and the record. */
    public record Entry(Identifier id, String method, TestFunction function) {
        public Identifier environment() {
            return environmentId(function.batchName());
        }
    }

    private BettaGameTests() {}

    public static Identifier environmentId(String batch) {
        return Identifier.fromNamespaceAndPath(NAMESPACE, "batch/" + sanitize(batch));
    }

    /** Every test of this run (normal suite, the enabled optional suites, filtered), in a stable order. */
    public static synchronized Collection<Entry> all() {
        if (tests == null) tests = collect();
        return tests.values();
    }

    /** Whether the id is one of our tests (other mods' libraries may bring GameTests of their own). */
    public static boolean ours(Identifier id) {
        all();
        return tests.containsKey(id);
    }

    /** Registers each test function (static registry {@code test_function}). */
    public static void registerFunctions(BiConsumer<ResourceKey<Consumer<GameTestHelper>>, Consumer<GameTestHelper>> sink) {
        for (Entry e : all()) sink.accept(ResourceKey.create(Registries.TEST_FUNCTION, e.id()), e.function().function());
    }

    /**
     * Registers an empty environment per batch and a test instance per test.
     *
     * @param environments registers an environment by id and returns its holder
     * @param instances registers a test instance by id
     */
    public static void registerInstances(BiFunction<Identifier, TestEnvironmentDefinition<?>, Holder<TestEnvironmentDefinition<?>>> environments,
            BiConsumer<Identifier, GameTestInstance> instances) {
        Map<Identifier, Holder<TestEnvironmentDefinition<?>>> envs = new LinkedHashMap<>();
        for (Entry e : all()) {
            Holder<TestEnvironmentDefinition<?>> env = envs.computeIfAbsent(e.environment(),
                    id -> environments.apply(id, new TestEnvironmentDefinition.AllOf(List.of())));
            instances.accept(e.id(), instance(e, env));
        }
        LOGGER.info("[gametest] registered {} GameTests in {} batches", all().size(), envs.size());
    }

    public static GameTestInstance instance(Entry e, Holder<TestEnvironmentDefinition<?>> env) {
        TestFunction f = e.function();
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(env, Identifier.parse(f.structureName()), f.maxTicks(),
                (int) f.setupTicks(), f.required(), f.rotation(), f.manualOnly(), Math.max(1, f.maxAttempts()),
                Math.max(1, f.requiredSuccesses()), f.skyAccess(), 0); // M52c: padding (26.1)
        return new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, e.id()), data);
    }

    private static Map<Identifier, Entry> collect() {
        List<Class<?>> classes = new ArrayList<>();
        try (var in = BettaGameTests.class.getResourceAsStream(LIST)) {
            if (in == null) throw new IllegalStateException(LIST + " is missing (made by the build from the @GameTestHolder classes)");
            for (String n : new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines().map(String::trim).filter(s -> !s.isEmpty()).toList()) {
                classes.add(Class.forName(n));
            }
        } catch (Exception e) {
            throw new IllegalStateException("cannot read the GameTest classes", e);
        }
        for (String suite : SUITES) classes.addAll(suiteClasses(suite));
        List<String> prefixes = Arrays.stream(ONLY.toLowerCase(Locale.ROOT).split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        Map<Identifier, Entry> out = new LinkedHashMap<>();
        int total = 0;
        for (Class<?> c : classes) {
            for (Entry e : entries(c)) {
                total++;
                if (!prefixes.isEmpty() && prefixes.stream().noneMatch(p -> e.function().testName().toLowerCase(Locale.ROOT).startsWith(p)
                        || e.method().startsWith(p) || e.function().batchName().toLowerCase(Locale.ROOT).startsWith(p))) continue;
                if (out.put(e.id(), e) != null) throw new IllegalStateException("two GameTests named " + e.id());
            }
        }
        if (!prefixes.isEmpty()) LOGGER.info("[gametest] -Pgametests={}: {} of {} tests", ONLY, out.size(), total);
        // M63: -Pgametestrepeat=<n>: every test n times side by side in its batch (copies <test>_r<i>), for flakiness hunts
        int repeat = Integer.getInteger("bettacraft.gametest.repeat", 1);
        if (repeat > 1) {
            for (Entry e : List.copyOf(out.values())) {
                TestFunction f = e.function();
                for (int i = 2; i <= repeat; i++) {
                    String name = f.testName() + "_r" + i;
                    TestFunction copy = new TestFunction(f.batchName(), name, f.structureName(), f.rotation(), f.maxTicks(), f.setupTicks(),
                            f.required(), f.manualOnly(), f.maxAttempts(), f.requiredSuccesses(), f.skyAccess(), f.function());
                    Entry c = new Entry(Identifier.fromNamespaceAndPath(NAMESPACE, sanitize(name)), e.method() + "_r" + i, copy);
                    out.put(c.id(), c);
                }
            }
            LOGGER.info("[gametest] -Pgametestrepeat={}: {} tests", repeat, out.size());
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static List<Class<?>> suiteClasses(String name) {
        try {
            Class<?> s = Class.forName(name);
            if (!s.getField("ENABLED").getBoolean(null)) return List.of();
            var f = s.getDeclaredField("CLASSES");
            f.setAccessible(true);
            return (List<Class<?>>) f.get(null);
        } catch (ClassNotFoundException e) {
            return List.of(); // not in this loader's dev source set
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot read the suite " + name, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Entry> entries(Class<?> c) {
        List<Entry> out = new ArrayList<>();
        GameTestHolder holder = c.getAnnotation(GameTestHolder.class);
        String cls = c.getSimpleName().toLowerCase(Locale.ROOT);
        Method[] methods = c.getDeclaredMethods();
        Arrays.sort(methods, Comparator.comparing(Method::getName));
        for (Method m : methods) {
            if (m.isAnnotationPresent(GameTestGenerator.class)) {
                try {
                    m.setAccessible(true);
                    for (TestFunction f : (Collection<TestFunction>) m.invoke(null)) {
                        out.add(new Entry(Identifier.fromNamespaceAndPath(NAMESPACE, sanitize(f.testName())), f.testName().toLowerCase(Locale.ROOT), f));
                    }
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("GameTest generator " + c.getName() + "." + m.getName(), e);
                }
                continue;
            }
            GameTest t = m.getAnnotation(GameTest.class);
            if (t == null) continue;
            if (!Modifier.isStatic(m.getModifiers()) || m.getParameterCount() != 1 || m.getParameterTypes()[0] != GameTestHelper.class) {
                throw new IllegalStateException("GameTest " + c.getName() + "." + m.getName() + " must be static with one GameTestHelper");
            }
            String method = m.getName().toLowerCase(Locale.ROOT);
            String name = cls + "." + method;
            String ns = !t.templateNamespace().isEmpty() ? t.templateNamespace() : holder != null ? holder.value() : "minecraft";
            PrefixGameTestTemplate prefix = m.getAnnotation(PrefixGameTestTemplate.class);
            if (prefix == null) prefix = c.getAnnotation(PrefixGameTestTemplate.class);
            boolean prefixed = prefix == null || prefix.value();
            String template = t.template().isEmpty() ? name : prefixed ? cls + "." + t.template() : t.template();
            TestFunction f = new TestFunction(t.batch(), name, ns + ":" + template, StructureUtils.getRotationForRotationSteps(t.rotationSteps()),
                    t.timeoutTicks(), t.setupTicks(), t.required(), t.manualOnly(), t.attempts(), t.requiredSuccesses(), t.skyAccess(), invoker(m));
            out.add(new Entry(Identifier.fromNamespaceAndPath(NAMESPACE, sanitize(name)), method, f));
        }
        return out;
    }

    private static Consumer<GameTestHelper> invoker(Method m) {
        m.setAccessible(true);
        return h -> {
            try {
                m.invoke(null, h);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException r) throw r;
                if (e.getCause() instanceof Error err) throw err;
                throw new RuntimeException(e.getCause());
            } catch (IllegalAccessException e) {
                throw new RuntimeException("cannot run " + m, e);
            }
        };
    }

    private static String sanitize(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
    }
}
