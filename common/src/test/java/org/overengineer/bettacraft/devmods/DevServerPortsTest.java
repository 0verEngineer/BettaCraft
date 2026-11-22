package org.overengineer.bettacraft.devmods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * M62: no dev server run can end up on the default port 25565 (the user's own server holds it). Every server the build starts
 * (runServer, buildShowcase, smoketestUpgrade, the multiplayer tests, smoketestWorldCleanup, runFabricServer, prodFabricServer,
 * fabricSaveLoadTest and the old version's server of the upgrade reference builder) takes this branch's dev_server_port /
 * dev_fabric_server_port: no build script or dev source names a port number of its own, every server.properties the build
 * writes is checked right before the server starts (checkServerPort: not 25565, free), and the reference builder pins the
 * old commit's server to the port Gradle chose and checks the port the server bound.
 */
class DevServerPortsTest {
    private static final List<String> BUILD_FILES = List.of("neoforge/build.gradle", "fabric/build.gradle");
    private static final Pattern LITERAL_PORT = Pattern.compile("server-port=\\d|localhost:\\d|\\b25565\\b");

    private static Path root() {
        return Path.of(System.getProperty("bettacraft.rootDir", ".."));
    }

    private static String read(String rel) throws IOException {
        return Files.readString(root().resolve(rel), StandardCharsets.UTF_8);
    }

    private static int port(String props, String key) {
        Matcher m = Pattern.compile("(?m)^" + key + "=(\\d+)\\s*$").matcher(props);
        assertTrue(m.find(), "gradle.properties has no " + key);
        return Integer.parseInt(m.group(1));
    }

    @Test
    void theBranchHasItsOwnDevPorts() throws IOException {
        String props = read("gradle.properties");
        int neo = port(props, "dev_server_port"), fabric = port(props, "dev_fabric_server_port");
        assertNotEquals(25565, neo, "dev_server_port");
        assertNotEquals(25565, fabric, "dev_fabric_server_port");
        assertNotEquals(neo, fabric, "NeoForge and Fabric dev servers need ports of their own");
    }

    /** Lines that may name 25565: the guards that refuse it (and comments). */
    private static boolean allowed(String line) {
        String t = line.trim();
        return t.startsWith("//") || t.startsWith("#") || t.startsWith("*") || t.contains("'server-port', '25565'")
                || t.contains("== '25565'") || t.startsWith("25565)");
    }

    @Test
    void noBuildScriptNamesAPortOfItsOwn() throws IOException {
        List<String> bad = new ArrayList<>();
        List<String> files = new ArrayList<>(BUILD_FILES);
        files.addAll(List.of("tools/upgrade/build_reference.sh", "tools/upgrade/chain.sh", "tools/upgrade/server_port.sh"));
        for (String f : files) {
            String[] lines = read(f).split("\n");
            for (int i = 0; i < lines.length; i++) {
                if (LITERAL_PORT.matcher(lines[i]).find() && !allowed(lines[i])) bad.add(f + ":" + (i + 1) + ": " + lines[i].trim());
            }
        }
        assertEquals(List.of(), bad, "a server port number in a build script (use dev_server_port / dev_fabric_server_port)");
    }

    @Test
    void noDevSourceNamesAServerPort() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String dir : List.of("neoforge/src/dev", "fabric/src/dev", "common/src/main", "neoforge/src/main", "fabric/src/main")) {
            Path d = root().resolve(dir);
            if (!Files.isDirectory(d)) continue;
            try (Stream<Path> s = Files.walk(d)) {
                for (Path p : s.filter(x -> x.toString().endsWith(".java")).toList()) {
                    String[] lines = Files.readString(p, StandardCharsets.UTF_8).split("\n");
                    for (int i = 0; i < lines.length; i++) {
                        if (Pattern.compile("localhost:\\d|\\b25565\\b").matcher(lines[i]).find()) bad.add(root().relativize(p) + ":" + (i + 1));
                    }
                }
            }
        }
        assertEquals(List.of(), bad, "a server address with a port in the code (the build passes bettacraft.smoketest.mp.address)");
    }

    @Test
    void everyServerStartIsCheckedFirst() throws IOException {
        Pattern writes = Pattern.compile("'server\\.properties'\\)\\.(write|text)|seedServerFiles\\(|fabricServerFiles\\(");
        for (String f : BUILD_FILES) {
            String check = f.startsWith("fabric") ? "fabricCheckServerPort(" : "checkServerPort(";
            String[] lines = read(f).split("\n");
            int n = 0;
            for (int i = 0; i < lines.length; i++) {
                String t = lines[i].trim();
                if (t.startsWith("def ") || t.startsWith("//") || !writes.matcher(t).find()) continue;
                boolean checked = false;
                for (int j = i; j < Math.min(lines.length, i + 12) && !checked; j++) checked = lines[j].contains(check);
                assertTrue(checked, f + ":" + (i + 1) + ": the server.properties is not checked with " + check + " before the server starts");
                n++;
            }
            assertTrue(n >= 3, f + ": expected the server starts of the build, found " + n);
        }
        String nf = read("neoforge/build.gradle");
        assertTrue(nf.contains("put('UPGRADE_SERVER_PORT'"), "the reference builder hands the old version's server a port");
        for (String sh : List.of("tools/upgrade/build_reference.sh", "tools/upgrade/chain.sh")) {
            String s = read(sh);
            assertTrue(s.contains("server_port.sh") && s.contains("pin_server_port") && s.contains("check_server_log"),
                    sh + ": the old version's server must be pinned to this branch's port and its bound port checked");
        }
    }
}
