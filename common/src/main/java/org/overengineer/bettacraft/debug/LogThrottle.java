package org.overengineer.bettacraft.debug;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import org.slf4j.Logger;

/**
 * M57: spam protection for warnings that can repeat every tick (a broken foreign mod, a script that throws, a frame that
 * cannot be drawn): the first one of a kind is written, the next ones of the same kind within a minute only counted,
 * and the first one after the minute says how many were left out. The kinds are fixed strings, so the table stays small.
 */
public final class LogThrottle {
    public static final long WINDOW_MS = 60_000L;
    /** The clock (unit tests replace it). */
    static LongSupplier clock = System::currentTimeMillis;
    private static final ConcurrentHashMap<String, State> STATES = new ConcurrentHashMap<>();

    private LogThrottle() {}

    private static final class State {
        long last;
        boolean seen;
        int suppressed;
    }

    /**
     * Whether a line of this kind may be written now: -1 = no (counted), otherwise the number of lines of this kind left
     * out since the last one written.
     */
    public static int admit(String kind) {
        State s = STATES.computeIfAbsent(kind, k -> new State());
        synchronized (s) {
            long now = clock.getAsLong();
            if (s.seen && now - s.last < WINDOW_MS) {
                s.suppressed++;
                return -1;
            }
            int n = s.suppressed;
            s.seen = true;
            s.last = now;
            s.suppressed = 0;
            return n;
        }
    }

    /** The message with the count of the lines left out ({@code n} from {@link #admit}). */
    public static String withCount(String message, int n) {
        return n <= 0 ? message : message + " (" + n + " more like this in the last minute)";
    }

    public static void warn(Logger log, String kind, String message, Object... args) {
        int n = admit(kind);
        if (n >= 0) log.warn(withCount(message, n), args);
    }

    public static void error(Logger log, String kind, String message, Object... args) {
        int n = admit(kind);
        if (n >= 0) log.error(withCount(message, n), args);
    }

    /** Forgets every kind (unit tests). */
    static void reset() {
        STATES.clear();
    }
}
