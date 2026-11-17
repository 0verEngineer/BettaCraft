package org.overengineer.bettacraft.platform.event;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * M52: the bus of the common game events. The handler classes (static {@link SubscribeEvent} methods taking one event) are
 * listed by {@link #register}; the loader asks {@link #types()} which events have handlers, fires them from its own events
 * ({@link #post(GameEvent, EventPriority)} once per priority, at the matching priority of the loader) and copies the
 * results back (cancelled, interaction result ...).
 */
public final class GameEvents {
    private record Handler(MethodHandle method, boolean receiveCanceled, String name) {}

    private static final Map<Class<?>, Map<EventPriority, List<Handler>>> HANDLERS = new HashMap<>();

    private GameEvents() {}

    /** Registers every handler of these classes (once, at mod construction). */
    public static synchronized void register(Class<?>... holders) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        for (Class<?> holder : holders) {
            for (Method m : holder.getDeclaredMethods()) {
                SubscribeEvent a = m.getAnnotation(SubscribeEvent.class);
                if (a == null) continue;
                if (!Modifier.isStatic(m.getModifiers()) || m.getParameterCount() != 1 || !GameEvent.class.isAssignableFrom(m.getParameterTypes()[0])) {
                    throw new IllegalArgumentException("not a static handler of one GameEvent: " + m);
                }
                try {
                    m.setAccessible(true);
                    MethodHandle h = lookup.unreflect(m);
                    HANDLERS.computeIfAbsent(m.getParameterTypes()[0], k -> new EnumMap<>(EventPriority.class))
                            .computeIfAbsent(a.priority(), k -> new ArrayList<>())
                            .add(new Handler(h, a.receiveCanceled(), holder.getSimpleName() + "." + m.getName()));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    /**
     * A listener added later (development code): only for an event type that already has handlers of the normal
     * priority (the loaders hook the types and priorities of the registered handlers once).
     */
    public static synchronized <E extends GameEvent> void listen(Class<E> type, java.util.function.Consumer<E> listener) {
        Map<EventPriority, List<Handler>> byPriority = HANDLERS.get(type);
        if (byPriority == null || !byPriority.containsKey(EventPriority.NORMAL)) {
            throw new IllegalStateException("no handlers of " + type.getSimpleName() + " at the normal priority to listen along");
        }
        try {
            MethodHandle accept = MethodHandles.lookup().findVirtual(java.util.function.Consumer.class, "accept",
                    java.lang.invoke.MethodType.methodType(void.class, Object.class)).bindTo(listener)
                    .asType(java.lang.invoke.MethodType.methodType(void.class, type));
            byPriority.get(EventPriority.NORMAL).add(new Handler(accept, false, "listener " + listener));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The event classes that have handlers, with their priorities. */
    public static synchronized Map<Class<?>, Set<EventPriority>> types() {
        Map<Class<?>, Set<EventPriority>> out = new HashMap<>();
        HANDLERS.forEach((k, v) -> out.put(k, Collections.unmodifiableSet(v.keySet())));
        return out;
    }

    public static boolean has(Class<? extends GameEvent> type) {
        return HANDLERS.containsKey(type);
    }

    /** Calls the handlers of one priority (in registration order); returns the event. */
    public static <E extends GameEvent> E post(E event, EventPriority priority) {
        Map<EventPriority, List<Handler>> byPriority = HANDLERS.get(event.getClass());
        if (byPriority == null) return event;
        List<Handler> hs = byPriority.get(priority);
        if (hs == null) return event;
        for (Handler h : hs) {
            if (event.isCanceled() && !h.receiveCanceled()) continue;
            try {
                h.method().invoke(event);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable t) {
                throw new RuntimeException("handler " + h.name() + " failed", t);
            }
        }
        return event;
    }

    /** Calls all handlers, highest priority first (loaders without priorities of their own: Fabric). */
    public static <E extends GameEvent> E post(E event) {
        for (EventPriority p : EventPriority.values()) post(event, p);
        return event;
    }
}
