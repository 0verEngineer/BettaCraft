package org.overengineer.bettacraft.debug;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ClientConfig;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.platform.Platform;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M57: {@code /bettacraft report}: a text file to attach to a GitHub issue ({@link #ISSUE_URL}): the versions of the mod,
 * loader, Minecraft and Java, the integrations and every loaded mod, the config values that differ from the default,
 * the debug switches, the data of the tank the player looks at (with its animals) and of the animal looked at, and the
 * end of the debug log.
 */
public final class BugReport {
    public static final String ISSUE_URL = "https://github.com/0verEngineer/BettaCraft/issues/new?template=bug_report.yml";
    /** How far the player's look reaches for the tank and the animal. */
    static final double REACH = 8.0;
    static final int MAX_ANIMALS = 64, DEBUG_LOG_LINES = 200;

    /** The mods Bettacraft has an integration with (ids on both loaders and every version). */
    static final List<String> INTEGRATIONS = List.of("guideme", "patchouli", "modonomicon", "jei", "roughlyenoughitems", "emi", "jade",
            "theoneprobe", "wthit", "betterfishtanks", "fintastic", "fishofthieves", "aquaculture", "naturalist", "crittersandcompanions",
            "livingthings", "create", "aeronautics", "sable", "ae2", "refinedstorage", "ftbquests", "ftbteams", "ftbchunks", "kubejs",
            "journeymap", "xaerominimap", "xaeroworldmap", "mekanism", "enderio", "thermal", "cable_facades", "integrateddynamics",
            "xnet", "laserio", "modularrouters", "industrialforegoing", "moderndynamics", "biomesoplenty", "regions_unexplored",
            "biomeswevegone", "twilightforest", "sodium", "iris", "oculus", "embeddium");

    private BugReport() {}

    /** The tank whose block the player looks at (its controller), or null. */
    @Nullable
    public static TankBlockEntity lookedAtTank(ServerPlayer p) {
        HitResult hit = p.pick(REACH, 1.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult bh)) return null;
        return TankMultiblock.getController(p.level(), bh.getBlockPos());
    }

    /** The entity the player looks at (also behind the glass of a tank), or null. */
    @Nullable
    public static Entity lookedAtEntity(ServerPlayer p) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1.0F), end = eye.add(look.scale(REACH));
        AABB box = p.getBoundingBox().expandTowards(look.scale(REACH)).inflate(1.0);
        EntityHitResult r = ProjectileUtil.getEntityHitResult(p, eye, end, box, e -> e != p && !e.isSpectator(), REACH * REACH);
        return r == null ? null : r.getEntity();
    }

    /** The report's lines. */
    public static List<String> lines(MinecraftServer server, @Nullable ServerPlayer viewer, @Nullable TankBlockEntity tank, @Nullable Entity entity) {
        List<String> out = new ArrayList<>();
        Platform pf = Platform.get();
        out.add("# Bettacraft report");
        out.add("Created: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        out.add("Attach this file to a bug report: " + ISSUE_URL);
        out.add("");
        out.add("## Versions");
        out.add("Bettacraft: " + pf.modVersion(BettaCraft.MOD_ID));
        out.add("Minecraft: " + pf.modVersion("minecraft"));
        if (pf.loader().equals("fabric")) {
            out.add("Loader: Fabric (loader " + pf.modVersion("fabricloader") + ", Fabric API " + pf.modVersion("fabric-api") + ")");
        } else {
            out.add("Loader: NeoForge " + pf.modVersion("neoforge"));
        }
        out.add("Java: " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + "), "
                + System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"));
        out.add("Memory: " + Runtime.getRuntime().maxMemory() / 1048576 + " MB max, " + Runtime.getRuntime().availableProcessors() + " cores");
        out.add("Server: " + (server.isDedicatedServer() ? "dedicated" : "integrated (single player or LAN)") + ", "
                + server.getPlayerCount() + " player(s) online");
        out.add("");
        out.add("## Debug log");
        for (LogArea a : LogArea.values()) {
            out.add(a.id + ": " + DebugLog.level(a) + (DebugLog.overridden(a) ? " (by /bettacraft debug)" : ""));
        }
        out.add("File: " + DebugLog.file().file().toAbsolutePath().normalize());
        out.add("");
        out.add("## Integrations loaded");
        int n = 0;
        for (String id : INTEGRATIONS) {
            if (!pf.isModLoaded(id)) continue;
            out.add(id + " " + pf.modVersion(id));
            n++;
        }
        if (n == 0) out.add("(none)");
        out.add("");
        out.add("## Config values that differ from the default");
        out.add("Preset: " + org.overengineer.bettacraft.config.Presets.active().id());
        changed("common", CommonConfig.SPEC, out);
        changed("server", ServerConfig.SPEC, out);
        if (!server.isDedicatedServer()) changed("client", ClientConfig.SPEC, out);
        out.add("");
        tank(server, viewer, tank, out);
        entity(entity, out);
        out.add("## Loaded mods");
        List<String> mods = pf.loadedMods();
        out.add(mods.size() + " mod(s)");
        out.addAll(mods);
        out.add("");
        debugLogTail(out);
        return out;
    }

    /** The values of a config that are not at their default ({@code name.path = value (default x)}). */
    static void changed(String name, ModConfigSpec spec, List<String> out) {
        if (!spec.isLoaded()) {
            out.add(name + ": not loaded");
            return;
        }
        List<String> lines = new ArrayList<>();
        walk(spec.getValues(), lines);
        if (lines.isEmpty()) out.add(name + ": all at their defaults");
        for (String l : lines) out.add(name + " " + l);
    }

    private static void walk(UnmodifiableConfig level, List<String> out) {
        for (Object v : level.valueMap().values()) {
            if (v instanceof UnmodifiableConfig sub) {
                walk(sub, out);
            } else if (v instanceof ModConfigSpec.ConfigValue<?> cv) {
                Object cur, def;
                try {
                    cur = cv.get();
                    def = cv.getDefault();
                } catch (RuntimeException e) {
                    continue;
                }
                if (!Objects.equals(cur, def)) out.add(String.join(".", cv.getPath()) + " = " + cur + " (default " + def + ")");
            }
        }
    }

    private static void tank(MinecraftServer server, @Nullable ServerPlayer viewer, @Nullable TankBlockEntity c, List<String> out) {
        out.add("## Tank looked at");
        if (c == null) {
            out.add(viewer == null ? "(run by the console: no tank)" : "(none: look at a tank when you run /bettacraft report to add its data)");
            out.add("");
            return;
        }
        TankData t = c.getTank();
        out.add("Controller: " + c.getBlockPos() + " in " + (c.getLevel() == null ? "?" : c.getLevel().dimension().location()));
        out.add("Id: " + t.id() + ", name: " + (t.name() == null || t.name().isEmpty() ? "-" : t.name()));
        out.add("Animals (inhabitants): " + t.inhabitants().size() + ", water quality " + t.waterQuality());
        out.add("Data (SNBT):");
        out.add(t.save().toString());
        if (c.getLevel() instanceof ServerLevel level) {
            List<Entity> live = TankInhabitants.liveInhabitants(level, t);
            out.add("Live animals: " + live.size());
            int i = 0;
            for (Entity e : live) {
                if (i++ >= MAX_ANIMALS) {
                    out.add("... " + (live.size() - MAX_ANIMALS) + " more");
                    break;
                }
                out.add(describe(e));
            }
        }
        out.add("");
    }

    private static void entity(@Nullable Entity e, List<String> out) {
        out.add("## Animal looked at");
        out.add(e == null ? "(none)" : describe(e));
        out.add("");
    }

    static String describe(Entity e) {
        CompoundTag tag = new CompoundTag();
        try {
            e.saveWithoutId(tag);
        } catch (RuntimeException ex) {
            tag.putString("error", ex.toString());
        }
        return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()) + " " + e.getUUID() + " at " + e.blockPosition() + ": " + tag;
    }

    private static void debugLogTail(List<String> out) {
        out.add("## End of the debug log");
        DebugLogFile f = DebugLog.file();
        f.flush(2000);
        Path p = f.file();
        if (!Files.exists(p)) {
            out.add("(no debug log in this session: /bettacraft debug all on, reproduce the problem, then /bettacraft report)");
            return;
        }
        try {
            List<String> all = Files.readAllLines(p, StandardCharsets.UTF_8);
            out.addAll(all.subList(Math.max(0, all.size() - DEBUG_LOG_LINES), all.size()));
        } catch (IOException | RuntimeException e) {
            out.add("(cannot read " + p + ": " + e + ")");
        }
    }

    /** Writes the report to {@code <game dir>/logs/bettacraft-report-<time>.txt}. */
    public static Path write(List<String> lines) throws IOException {
        Path dir = Platform.get().gameDir().resolve("logs");
        Files.createDirectories(dir);
        Path file = dir.resolve("bettacraft-report-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".txt");
        Files.write(file, lines, StandardCharsets.UTF_8);
        return file;
    }
}
