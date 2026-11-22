package org.overengineer.bettacraft.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** M57: the switches of the debug log, its file (rotation), the spam protection and that a switched-off line costs nothing. */
class DebugLogTest {
    @TempDir
    Path dir;

    @AfterEach
    void restore() {
        DebugLog.override(null, null);
        DebugLog.configure(DebugLog.Level.INFO, List.of("all"));
        LogThrottle.clock = System::currentTimeMillis;
        LogThrottle.reset();
    }

    @Test
    void configAndCommandSetTheSwitches() {
        DebugLog.useFile(dir.resolve("a.log"));
        DebugLog.configure(DebugLog.Level.INFO, List.of("all"));
        for (LogArea a : LogArea.values()) assertFalse(a.debug || a.trace, a.id);
        assertFalse(DebugLog.anyOn());

        DebugLog.configure(DebugLog.Level.DEBUG, List.of("care", " Breeding ", "nonsense"));
        assertTrue(LogArea.CARE.debug && LogArea.BREEDING.debug);
        assertFalse(LogArea.CARE.trace);
        assertFalse(LogArea.NETWORK.debug);

        DebugLog.override(LogArea.NETWORK, DebugLog.Level.TRACE); // /bettacraft debug network trace
        assertTrue(LogArea.NETWORK.debug && LogArea.NETWORK.trace);
        DebugLog.override(LogArea.CARE, DebugLog.Level.INFO); // /bettacraft debug care off wins over the config
        assertFalse(LogArea.CARE.debug);
        assertTrue(DebugLog.overridden(LogArea.CARE));

        DebugLog.override(null, null); // /bettacraft debug reset
        assertTrue(LogArea.CARE.debug);
        assertFalse(LogArea.NETWORK.debug);

        DebugLog.override(null, DebugLog.Level.DEBUG); // /bettacraft debug all on
        for (LogArea a : LogArea.values()) assertTrue(a.debug, a.id);
        assertTrue(DebugLog.anyOn());
    }

    @Test
    void linesGoToTheFileWithAreaLevelAndStackTrace() throws Exception {
        Path f = dir.resolve("bettacraft-debug.log");
        DebugLog.useFile(f);
        DebugLog.override(LogArea.CARE, DebugLog.Level.DEBUG);
        LogArea.CARE.debug("tank {} log: {}", 7, "died", new IllegalStateException("boom"));
        assertTrue(DebugLog.file().flush(5000));
        String text = Files.readString(f, StandardCharsets.UTF_8);
        assertTrue(text.contains("[debug log] areas on: care"), text);
        assertTrue(text.contains("/DEBUG] [bettacraft.care]: tank 7 log: died"), text);
        assertTrue(text.contains("java.lang.IllegalStateException: boom"), text);
    }

    @Test
    void theFileIsRotatedAndANewSessionStartsANewFile() throws Exception {
        Path f = dir.resolve("r.log");
        Files.writeString(f, "last session\n");
        DebugLogFile file = new DebugLogFile(f, 1000);
        for (int i = 0; i < 60; i++) file.add("line " + i + " " + "x".repeat(40));
        assertTrue(file.flush(5000));
        assertTrue(Files.size(f) <= 1000, "size " + Files.size(f));
        assertTrue(Files.exists(file.backup()));
        String now = Files.readString(f);
        assertTrue(now.contains("line 59"), now);
        assertFalse(now.contains("last session"));
        assertFalse(Files.readString(file.backup()).contains("last session"), "the old session went out with the first rotation");
    }

    @Test
    void repeatedWarningsComeOnceAMinuteWithACount() {
        AtomicLong now = new AtomicLong(1_000_000);
        LogThrottle.clock = now::get;
        assertEquals(0, LogThrottle.admit("k"));
        for (int i = 0; i < 99; i++) {
            now.addAndGet(100);
            assertEquals(-1, LogThrottle.admit("k"));
        }
        assertEquals(0, LogThrottle.admit("other"), "another kind is not held back");
        now.addAndGet(LogThrottle.WINDOW_MS);
        assertEquals(99, LogThrottle.admit("k"));
        assertEquals("x (99 more like this in the last minute)", LogThrottle.withCount("x", 99));
        assertEquals("x", LogThrottle.withCount("x", 0));
    }

    /** The guard before a debug line: with the area off no argument is boxed and nothing is formatted (no allocation). */
    @Test
    void aSwitchedOffLineAllocatesNothing() {
        DebugLog.override(null, DebugLog.Level.INFO);
        var mx = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long id = Thread.currentThread().getId();
        long sink = 0;
        for (int round = 0; round < 3; round++) { // warm up, then measure
            long before = mx.getThreadAllocatedBytes(id);
            for (int i = 0; i < 1_000_000; i++) {
                if (LogArea.CARE.debug) LogArea.CARE.debug("tank {} at {}: {} died: {}", i, i * 2L, "betta", 0.5 * i);
                sink += i;
            }
            long allocated = mx.getThreadAllocatedBytes(id) - before;
            if (round == 2) assertTrue(allocated < 64 * 1024, "allocated " + allocated + " bytes for a million switched-off lines");
        }
        assertTrue(sink != 0);
    }
}
