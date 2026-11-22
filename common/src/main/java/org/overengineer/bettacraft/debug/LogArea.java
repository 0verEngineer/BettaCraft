package org.overengineer.bettacraft.debug;

import java.util.Locale;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M57: the areas of the debug log, one logger each ({@code bettacraft.<id>}). A debug line is written as
 * <pre>if (LogArea.CARE.debug) LogArea.CARE.debug("...", a, b);</pre>
 * so with the level switched off it costs one read of a field: no arguments are boxed, no varargs array is made and
 * nothing is formatted ({@code debug} and {@code trace} are set by {@link DebugLog} from the common config
 * {@code debug.*} and the command {@code /bettacraft debug}). Debug and trace lines go to
 * {@code logs/bettacraft-debug.log} only, info and warnings stay in the normal log.
 */
public enum LogArea {
    /** Needs, sickness, deaths with their reason, the tank log. */
    CARE("care"),
    /** Nests, eggs, litters, young: what came out and why. */
    BREEDING("breeding"),
    /** Putting animals in (refusals with the reason), taking them out, the multiblock, travelling tanks. */
    TANK("tank"),
    /** The interior dimension: regions, chunks, plants, clean-up. */
    INTERIOR("interior"),
    /** Our packets: interior syncs with their size, every payload sent (trace). */
    NETWORK("network"),
    /** Integrations with other mods (Create, Better Fish Tanks, map mods, overlays ...): what they do and how they fail. */
    COMPAT("compat"),
    /** The client: miniature meshes, skins, renderers. */
    RENDER("render"),
    /** The hired aquarist: his plan and jobs. */
    AQUARIST("aquarist"),
    /** Wild spawns and the world. */
    WORLD("world");

    public final String id;
    public final Logger logger;
    /** Debug lines of this area are written (read it before building the line). */
    public volatile boolean debug;
    /** Trace lines (every packet, every care step) are written; implies {@link #debug}. */
    public volatile boolean trace;

    LogArea(String id) {
        this.id = id;
        this.logger = LoggerFactory.getLogger("bettacraft." + id);
    }

    @Nullable
    public static LogArea byId(String id) {
        String k = id.trim().toLowerCase(Locale.ROOT);
        for (LogArea a : values()) if (a.id.equals(k)) return a;
        return null;
    }

    /** A debug line (call it only after checking {@link #debug}); a Throwable as the last argument adds its stack trace. */
    public void debug(String message, Object... args) {
        DebugLog.write(this, "DEBUG", message, args);
    }

    /** A trace line (call it only after checking {@link #trace}). */
    public void trace(String message, Object... args) {
        DebugLog.write(this, "TRACE", message, args);
    }

    /** Info for the normal log; also in the debug file while it is open. */
    public void info(String message, Object... args) {
        logger.info(message, args);
        if (DebugLog.anyOn()) DebugLog.write(this, "INFO", message, args);
    }

    /** A warning for the normal log; also in the debug file while it is open. */
    public void warn(String message, Object... args) {
        logger.warn(message, args);
        if (DebugLog.anyOn()) DebugLog.write(this, "WARN", message, args);
    }

    /** A warning of a kind that can repeat often (every tick): at most once a minute per {@code kind}, with a count ({@link LogThrottle}). */
    public void warnThrottled(String kind, String message, Object... args) {
        LogThrottle.warn(logger, id + ":" + kind, message, args);
        if (DebugLog.anyOn()) DebugLog.write(this, "WARN", message, args);
    }

    /** An error for the normal log, throttled like {@link #warnThrottled}. */
    public void errorThrottled(String kind, String message, Object... args) {
        LogThrottle.error(logger, id + ":" + kind, message, args);
        if (DebugLog.anyOn()) DebugLog.write(this, "ERROR", message, args);
    }
}
