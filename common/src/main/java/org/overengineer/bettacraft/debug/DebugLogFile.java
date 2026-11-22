package org.overengineer.bettacraft.debug;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * M57: the file {@code logs/bettacraft-debug.log}. Lines are queued by the game threads and written by one background
 * thread, so the server tick never waits for the disk. The file is rotated at {@link #MAX_BYTES} (10 MB): the full file
 * becomes {@code bettacraft-debug.1.log} (the older one is dropped), so the debug log never takes more than 20 MB. The
 * file of the previous session is moved to {@code .1} when the first line of a session is written. A full queue (the
 * disk far slower than the game) drops lines and says how many.
 */
public final class DebugLogFile {
    public static final long MAX_BYTES = 10L * 1024 * 1024;
    static final int QUEUE_MAX = 100_000;

    private final Path file;
    private final long maxBytes;
    private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>(QUEUE_MAX);
    private final AtomicLong queued = new AtomicLong(), written = new AtomicLong(), dropped = new AtomicLong();
    private Thread thread;
    private BufferedWriter out;
    private long size;
    private boolean opened;

    public DebugLogFile(Path file, long maxBytes) {
        this.file = file;
        this.maxBytes = maxBytes;
    }

    public Path file() {
        return file;
    }

    /** The previous file ({@code bettacraft-debug.1.log}). */
    public Path backup() {
        return file.resolveSibling(file.getFileName().toString().replaceFirst("\\.log$", "") + ".1.log");
    }

    /** Queues a line (any thread). */
    public void add(String line) {
        if (!queue.offer(line)) {
            dropped.incrementAndGet();
            return;
        }
        queued.incrementAndGet();
        start();
    }

    private synchronized void start() {
        if (thread != null) return;
        thread = new Thread(this::run, "Bettacraft debug log");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    /** Waits until every line queued so far is on the disk (GameTests, the report command); false on timeout. */
    public boolean flush(long timeoutMs) {
        long until = System.currentTimeMillis() + timeoutMs, target = queued.get();
        while (written.get() < target) {
            if (System.currentTimeMillis() > until) return false;
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }

    public long dropped() {
        return dropped.get();
    }

    private void run() {
        List<String> batch = new ArrayList<>();
        long reportedDrops = 0;
        while (true) {
            int taken = 0;
            try {
                String first = queue.poll(1, TimeUnit.SECONDS);
                if (first == null) continue;
                batch.add(first);
                queue.drainTo(batch, 4096);
                taken = batch.size();
                long d = dropped.get();
                if (d > reportedDrops) {
                    batch.add(0, "[debug log] " + (d - reportedDrops) + " line(s) dropped: the disk could not keep up");
                    reportedDrops = d;
                }
                for (String line : batch) write(line);
                if (queue.isEmpty()) closeQuietly(); // nothing more for now: the file is not held open (Windows can read, move and delete it)
                else if (out != null) out.flush();
            } catch (InterruptedException e) {
                return;
            } catch (IOException | RuntimeException e) {
                org.slf4j.LoggerFactory.getLogger("bettacraft").warn("Could not write {}: {}", file, e.toString());
                closeQuietly();
            } finally {
                written.addAndGet(taken);
                batch.clear();
            }
        }
    }

    private void write(String line) throws IOException {
        if (out == null) open();
        int bytes = line.getBytes(StandardCharsets.UTF_8).length + System.lineSeparator().length();
        if (size > 0 && size + bytes > maxBytes) {
            closeQuietly();
            Files.move(file, backup(), StandardCopyOption.REPLACE_EXISTING);
            open();
        }
        out.write(line);
        out.newLine();
        size += bytes;
    }

    private void open() throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        if (!opened) { // a new session starts a new file, the last one stays as .1
            opened = true;
            if (Files.exists(file) && Files.size(file) > 0) Files.move(file, backup(), StandardCopyOption.REPLACE_EXISTING);
        }
        out = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        size = Files.size(file);
    }

    private void closeQuietly() {
        if (out == null) return;
        try {
            out.close();
        } catch (IOException ignored) {
            // nothing more to do
        }
        out = null;
    }
}
