package org.overengineer.bettacraft.debug;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * M57: which areas write debug lines and where they go. The level comes from the common config ({@code debug.logLevel}:
 * INFO, DEBUG or TRACE; {@code debug.areas}: the areas it applies to, {@code all} for every one); the command
 * {@code /bettacraft debug <area|all> <on|off|trace>} overrides it per area until the game restarts (or
 * {@code /bettacraft debug reset}). The result ends up in the fields {@link LogArea#debug} and {@link LogArea#trace},
 * which the call sites read before they build a line.
 */
public final class DebugLog {
    public enum Level { INFO, DEBUG, TRACE }

    public static final String FILE_NAME = "bettacraft-debug.log";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private static Level configLevel = Level.INFO;
    private static final Set<LogArea> CONFIG_AREAS = EnumSet.allOf(LogArea.class);
    private static final Map<LogArea, Level> OVERRIDES = new EnumMap<>(LogArea.class);
    private static volatile boolean anyOn;
    @Nullable
    private static DebugLogFile file;
    @Nullable
    private static Path fileOverride;

    private DebugLog() {}

    /** Some area writes debug lines (the file is in use). */
    public static boolean anyOn() {
        return anyOn;
    }

    /** The common config was loaded or changed: its level and areas ({@code all} or ids; unknown ids are ignored). */
    public static synchronized void configure(@Nullable Level level, Collection<? extends String> areas) {
        configLevel = level == null ? Level.INFO : level;
        CONFIG_AREAS.clear();
        for (String s : areas) {
            if (s.trim().equalsIgnoreCase("all")) CONFIG_AREAS.addAll(EnumSet.allOf(LogArea.class));
            LogArea a = LogArea.byId(s);
            if (a != null) CONFIG_AREAS.add(a);
        }
        apply();
    }

    /** The command: {@code level} for one area (null = every area) until the restart; {@code level} null removes the override. */
    public static synchronized void override(@Nullable LogArea area, @Nullable Level level) {
        for (LogArea a : area == null ? EnumSet.allOf(LogArea.class) : EnumSet.of(area)) {
            if (level == null) OVERRIDES.remove(a);
            else OVERRIDES.put(a, level);
        }
        apply();
    }

    /** The level an area writes with now. */
    public static synchronized Level level(LogArea a) {
        Level o = OVERRIDES.get(a);
        if (o != null) return o;
        return CONFIG_AREAS.contains(a) ? configLevel : Level.INFO;
    }

    public static synchronized boolean overridden(LogArea a) {
        return OVERRIDES.containsKey(a);
    }

    public static synchronized Level configLevel() {
        return configLevel;
    }

    public static synchronized Set<LogArea> configAreas() {
        return EnumSet.copyOf(CONFIG_AREAS.isEmpty() ? EnumSet.noneOf(LogArea.class) : CONFIG_AREAS);
    }

    private static void apply() {
        boolean any = false;
        List<String> on = new ArrayList<>();
        for (LogArea a : LogArea.values()) {
            Level l = level(a);
            a.trace = l == Level.TRACE;
            a.debug = l != Level.INFO;
            if (a.debug) {
                any = true;
                on.add(a.id + (a.trace ? " (trace)" : ""));
            }
        }
        boolean was = anyOn;
        anyOn = any;
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        if (any) file().add("[" + now + "] [debug log] areas on: " + String.join(", ", on) + " (Bettacraft " + version() + ")");
        else if (was) file().add("[" + now + "] [debug log] every area off");
    }

    private static String version() {
        try {
            return org.overengineer.bettacraft.platform.Platform.available()
                    ? org.overengineer.bettacraft.platform.Platform.get().modVersion(org.overengineer.bettacraft.BettaCraft.MOD_ID) : "?";
        } catch (RuntimeException e) {
            return "?";
        }
    }

    /** The debug file ({@code <game dir>/logs/bettacraft-debug.log}). */
    public static synchronized DebugLogFile file() {
        if (file == null) {
            Path p = fileOverride;
            if (p == null) {
                Path dir = org.overengineer.bettacraft.platform.Platform.available()
                        ? org.overengineer.bettacraft.platform.Platform.get().gameDir() : Path.of(".");
                p = dir.resolve("logs").resolve(FILE_NAME);
            }
            file = new DebugLogFile(p, DebugLogFile.MAX_BYTES);
        }
        return file;
    }

    /** Unit tests: write to another file. */
    static synchronized void useFile(Path p) {
        fileOverride = p;
        file = null;
    }

    /** One line: {@code [time] [thread/LEVEL] [bettacraft.area]: message}, with the stack trace of a Throwable argument. */
    static void write(LogArea area, String level, String message, Object[] args) {
        FormattingTuple t = MessageFormatter.arrayFormat(message, args);
        String text = t.getMessage();
        Throwable th = t.getThrowable();
        StringBuilder b = new StringBuilder(text.length() + 64);
        b.append('[').append(LocalTime.now().format(TIME)).append("] [").append(Thread.currentThread().getName()).append('/').append(level)
                .append("] [bettacraft.").append(area.id).append("]: ").append(text);
        if (th != null) {
            StringWriter sw = new StringWriter();
            th.printStackTrace(new PrintWriter(sw));
            b.append(System.lineSeparator()).append(sw.toString().stripTrailing());
        }
        file().add(b.toString());
        // the launcher's own debug log (NeoForge's debug.log) gets them too; latest.log stays at INFO
        if (level.equals("DEBUG") && area.logger.isDebugEnabled()) area.logger.debug(text, th);
        else if (level.equals("TRACE") && area.logger.isTraceEnabled()) area.logger.trace(text, th);
    }
}
