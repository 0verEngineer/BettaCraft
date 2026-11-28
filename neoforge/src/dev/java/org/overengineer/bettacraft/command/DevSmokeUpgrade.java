package org.overengineer.bettacraft.command;

import org.overengineer.bettacraft.util.NbtCompat;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.FeederBlockEntity;
import org.overengineer.bettacraft.filter.FilterPumpHeadBlockEntity;
import org.overengineer.bettacraft.filter.MultiblockFilter;
import org.overengineer.bettacraft.plumbing.PipeBlock;
import org.overengineer.bettacraft.plumbing.PipeBlockEntity;
import org.overengineer.bettacraft.plumbing.PipeBundleBlock;
import org.overengineer.bettacraft.plumbing.PipeNetwork;
import org.overengineer.bettacraft.plumbing.PipeRole;
import org.overengineer.bettacraft.plumbing.PipeSlots;
import org.overengineer.bettacraft.plumbing.interior.TankAnchors;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M21.5: the upgrade test ({@code gradlew smoketestUpgrade}, active with {@code -Dbettacraft.smoketest.upgrade=1|2}). The
 * server runs on a copy of the reference world built by the version the user plays (1.13.1-beta.1, see
 * {@code tools/upgrade/build_reference.sh}); that version wrote {@code bettacraft_reference.json} into the world: every
 * tank (water, salinity, blocks, name, lock and owner, name plate, floating plants, its animals by UUID with species,
 * variety, sex, stage and betta genome, the small animals with their genes, the block counts of its interior, the
 * equipment around it: cabinets with wood, filter, heater and contents, feeders with food) and every chest with the
 * mod's items.
 * <p>
 * Stage 1 compares the loaded world with that file, saves, writes the same snapshot as seen by this version
 * ({@code bettacraft_upgrade_stage1.json}) and stops; stage 2 loads the saved world again and compares it with the
 * snapshot (save and reload keep everything). Logs "[smoketest] UPGRADE STAGE n PASSED". Errors in the log are checked by
 * the Gradle task.
 */
public final class DevSmokeUpgrade {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static final int STAGE = Integer.getInteger("bettacraft.smoketest.upgrade", 0);
    public static final String REFERENCE = "bettacraft_reference.json", SNAPSHOT = "bettacraft_upgrade_stage1.json";
    private static int ticks, haltAt = -1, readyAt = -1;
    private static JsonObject expected;
    private static final List<ChunkPos> forced = new ArrayList<>();
    private static final List<String> problems = new ArrayList<>();
    /** M22.4: cabinets whose outlet side was checked (old cabinets keep the back wall). */
    private static int cabinetsChecked;
    /** M44: the fronts the old cabinets show after loading. */
    private static final java.util.List<String> cabinetFronts = new java.util.ArrayList<>();
    /** M23: external filters and lights of the old world checked (full power, no sponge, not dimmed). */
    private static int m23Checked;
    /** M24: tanks of the old world checked (nitrate starts at 0, no water change noted, no bulkhead). */
    private static int m24Checked;
    /** M25 part c: old tanks whose alkalinity and magnesium follow their salt. */
    private static int m25cChecked;
    /** M29 part c/d: old bettas that are pure Betta splendens (no ancestry, the species id unchanged). */
    private static int m29Checked;
    private static int m33Checked;
    private static int m331Checked;
    /** M25 part b: old sea water buckets found in the chests (they must give other mods the sea water fluid). */
    private static int seaBuckets;
    /** M35.1: chest, hatchery and item entity with old cyst jars that came through loading (and in stage 2 a reload). */
    private static int m351Checked;
    private static final String M351_FILE = "bettacraft_m35_1_cysts.txt";
    /** M40 (D2): multiblock filters of the old world: their media kept, marked retrofit, a warning and no filtering until guarded. */
    private static int m40Checked;
    /** M67: a cabinet of the old world with a wound spring (stage 1), its energy in the tank's store after a save and reload (stage 2). */
    private static int m67Checked;
    static final String M67_FILE = "bettacraft_upgrade_m67.json";
    /**
     * M68: the {@code pipes} section of a reference world of the M67 line ({@code Bettacraft Rohre 1.56.0}, written by
     * {@code tools/upgrade/DevUpgradeReferenceM67}): every old pipe block, filter, shared dock and container. Read from the reference in
     * both stages (stage 2: the same expectations after a save and reload). Null for the other reference worlds.
     */
    private static JsonObject pipesRef;
    private static int m68Pipes, m68Roles, m68Filters, m68Docks, m68Containers;
    /** M68: water (mB) open tanks evaporated between the save and the comparison (allowed, logged). */
    private static long evaporated;
    /** M68: the ids the registry aliases turn old blocks (in the world) and old items (in containers) into. */
    static final Map<String, String> M68_BLOCKS = new TreeMap<>(), M68_ITEMS = new TreeMap<>();
    static {
        for (String old : List.of("filter_inlet_pipe", "filter_outlet_pipe", "filter_fresh_pipe", "filter_waste_pipe", "filter_fresh_port", "filter_waste_port")) {
            M68_BLOCKS.put("bettacraft:" + old, "bettacraft:stainless_steel_pipe");
            M68_ITEMS.put("bettacraft:" + old, "bettacraft:stainless_steel_pipe");
        }
        M68_ITEMS.put("bettacraft:pipe_tee", "bettacraft:stainless_steel_pipe");
        M68_ITEMS.put("bettacraft:interior_pipe", "bettacraft:glass_pipe");
    }
    /**
     * M40: the reference world is one of the old 1.13 line (the checks for what came after it apply), not a newer one such
     * as {@code Bettacraft Referenz 1.31.0} (built for the old multiblock filters of M40: only the comparison and the M40 check).
     */
    private static boolean legacy = true;
    /** M52: the world was saved by this very version (a save/load test, no migration to check). */
    private static boolean sameVersion;

    private DevSmokeUpgrade() {}

    /** M52: every server tick (NeoForge: {@link DevSmokeUpgradeNeoForge}; Fabric: the Fabric dev smoke). */
    public static void onTick(MinecraftServer tickedServer) {
        if (STAGE == 0) return;
        MinecraftServer server = tickedServer;
        ServerLevel level = server.overworld();
        ticks++;
        try {
            if (ticks == 5) {
                Path root = server.getWorldPath(LevelResource.ROOT);
                Path file = root.resolve(STAGE == 1 ? REFERENCE : SNAPSHOT);
                if (!Files.exists(file)) throw new IllegalStateException("missing " + file);
                expected = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                refVersion = expected.has("version") ? expected.get("version").getAsString() : "";
                legacy = legacyVersion(refVersion);
                sameVersion = expected.has("version") && expected.get("version").getAsString().equals(org.overengineer.bettacraft.platform.Platform.get().modVersion(BettaCraft.MOD_ID));
                Set<Long> chunks = new HashSet<>();
                for (JsonElement e : expected.getAsJsonArray("tanks")) {
                    BlockPos p = BlockPos.of(e.getAsJsonObject().get("pos").getAsLong());
                    for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) chunks.add(ChunkPos.pack((p.getX() >> 4) + dx, (p.getZ() >> 4) + dz));
                }
                for (JsonElement e : expected.getAsJsonArray("chests")) chunks.add(ChunkPos.containing(BlockPos.of(e.getAsJsonObject().get("pos").getAsLong())).pack());
                // M68: the old pipe system of an M67 world (the reference has it in both stages)
                JsonObject ref = STAGE == 1 ? expected : JsonParser.parseString(Files.readString(root.resolve(REFERENCE), StandardCharsets.UTF_8)).getAsJsonObject();
                pipesRef = ref.has("pipes") ? ref.getAsJsonObject("pipes") : null;
                if (pipesRef != null) for (String k : new String[]{"blocks", "filters", "containers"})
                    for (JsonElement e : pipesRef.getAsJsonArray(k)) chunks.add(ChunkPos.containing(BlockPos.of(e.getAsJsonObject().get("pos").getAsLong())).pack());
                for (long c : chunks) {
                    ChunkPos cp = ChunkPos.unpack(c);
                    level.setChunkForced(cp.x(), cp.z(), true);
                    forced.add(cp);
                }
                LOGGER.info("[smoketest] upgrade stage {}: {} from {}, {} tanks, {} chests, {} chunks", STAGE, file.getFileName(),
                        expected.has("version") ? expected.get("version").getAsString() : "?", expected.getAsJsonArray("tanks").size(),
                        expected.getAsJsonArray("chests").size(), forced.size());
            } else if (ticks > 5 && readyAt < 0) {
                boolean ready = true;
                for (ChunkPos cp : forced) {
                    level.getChunk(cp.x(), cp.z());
                    ready &= level.areEntitiesLoaded(cp.pack());
                }
                if (!ready && ticks < 1200) return;
                readyAt = ticks;
                // a few ticks for the animals to join their tanks
            } else if (readyAt > 0 && ticks == readyAt + 40) {
                compare(server, level);
                if (STAGE == 1 && legacy) { // M29: every betta of the old world is a pure Betta splendens
                    for (var e : level.getAllEntities()) {
                        if (!(e instanceof org.overengineer.bettacraft.entity.betta.BettaEntity b)) continue;
                        var g = b.getBettaData().genotype();
                        if (g.ancestry().equals(org.overengineer.bettacraft.genetics.BettaSpecies.Ancestry.SPLENDENS) && "bettacraft:betta".equals(g.species())) m29Checked++;
                        else problems.add("old betta " + b.getUUID() + " is " + g.species() + " " + g.ancestry());
                        // M33 part d: an old betta has no counted age, or (after its first care step) the age of its stage
                        double age = b.health().ageDays();
                        if (age < 0 || age <= 9.0 + 30) m33Checked++;
                        else problems.add("old betta " + b.getUUID() + " is " + age + " days old");
                    }
                }
                if (STAGE == 2 && legacy) m351Check(server, level); // M35.1: the old cyst jars of stage 1 after a save and reload
                if (STAGE == 2) m67Check(server, level); // M67: the wound spring of stage 1 is energy in the tank's store
                if (pipesRef != null) m68Check(level); // M68: the old pipe system converted, nothing lost (both stages)
                // M40: old multiblock filters need their guards now (M52: not in a world this version saved itself, the
                // Fabric save/load test; M53c: nor in a world of 1.32 or later, whose filters have them: the ports' references)
                if (!sameVersion && olderThan(refVersion, 1, 32)) m40Check(level);
                if (!problems.isEmpty()) {
                    for (String p : problems) LOGGER.error("[smoketest] upgrade: {}", p);
                    throw new IllegalStateException(problems.size() + " difference(s)");
                }
                if (STAGE == 1) m67Place(server, level); // M67: a wound spring in the old save format (before the snapshot: it may add a filter)
                if (STAGE == 1) {
                    server.saveEverything(false, true, true);
                    write(server, level, SNAPSHOT);
                    // M35.1: the reference world (1.13.1) has no cyst jars: old ones as fixtures, loaded from the old save format
                    if (legacy) m351Place(server, level);
                    if (!problems.isEmpty()) {
                        for (String p : problems) LOGGER.error("[smoketest] upgrade: {}", p);
                        throw new IllegalStateException(problems.size() + " problem(s) with the old cyst jars");
                    }
                    server.saveEverything(false, true, true);
                }
                for (ChunkPos cp : forced) level.setChunkForced(cp.x(), cp.z(), false);
                LOGGER.info("[smoketest] upgrade: {} cabinets keep the back wall (M22.4)", cabinetsChecked);
                LOGGER.info("[smoketest] upgrade: cabinets are auto with joints and show the fronts of their rows (M44): {}", cabinetFronts);
                LOGGER.info("[smoketest] upgrade: {} filters and lights run at full power, undimmed (M23)", m23Checked);
                LOGGER.info("[smoketest] upgrade: {} tanks start with nitrate 0 and no water change (M24)", m24Checked);
                LOGGER.info("[smoketest] upgrade: {} sea water buckets give the sea water fluid (M25)", seaBuckets);
                LOGGER.info("[smoketest] upgrade: {} tanks with alkalinity and magnesium of their salt (M25c)", m25cChecked);
                LOGGER.info("[smoketest] upgrade: {} old bettas are pure Betta splendens (M29)", m29Checked);
                LOGGER.info("[smoketest] upgrade: {} old bettas start with no age or the age of their stage (M33 old age)", m33Checked);
                LOGGER.info("[smoketest] upgrade: {} tanks with their tank type (old ones automatic, M33.1)", m331Checked);
                LOGGER.info("[smoketest] upgrade: {} places with old artemia cyst jars keep their count without the old uses (M35.1)", m351Checked);
                LOGGER.info("[smoketest] upgrade: {} old multiblock filters keep their media and warn until guarded (M40, D2)", m40Checked);
                LOGGER.info("[smoketest] upgrade: {} wound springs (cabinet or external filter) became energy in their tank's store, nothing lost (M67)", m67Checked);
                if (pipesRef != null) LOGGER.info("[smoketest] upgrade: M68 {} old pipe blocks are new pipes with their connections ({} of them in their old role), "
                        + "{} filters filter their tanks, {} shared docks split into a dock per line with its guard, {} containers hold the new items 1:1",
                        m68Pipes, m68Roles, m68Filters, m68Docks, m68Containers);
                LOGGER.info("[smoketest] UPGRADE STAGE {} PASSED", STAGE);
                haltAt = ticks + 10;
            } else if (ticks == haltAt) {
                server.halt(false);
            }
        } catch (Exception e) {
            LOGGER.error("[smoketest] UPGRADE STAGE " + STAGE + " FAILED", e);
            server.halt(false);
        }
    }

    // ------------------------------------------------------------------ M40 old multiblock filters

    /** The version that saved the reference world. */
    private static String refVersion = "";

    /** Whether a version is older than {@code major.minor} (unknown: older). */
    static boolean olderThan(String v, int major, int minor) {
        String[] p = v.split("[.-]");
        try {
            int a = Integer.parseInt(p[0]), b = p.length > 1 ? Integer.parseInt(p[1]) : 0;
            return a < major || a == major && b < minor;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    /** Whether the reference version is of the 1.13 line or older (before M22: the checks for later changes apply). */
    static boolean legacyVersion(String v) {
        String[] p = v.split("[.-]");
        try {
            return p.length < 2 || (Integer.parseInt(p[0]) == 1 && Integer.parseInt(p[1]) < 14);
        } catch (NumberFormatException e) {
            return true;
        }
    }

    /**
     * D2: a multiblock filter of the old world (no guards in its tanks) is marked retrofit (stage 2: still, after the save),
     * keeps its media and its regulator, and after a batch it warns for its tank and cleans nothing.
     */
    private static void m40Check(ServerLevel level) {
        for (ChunkPos cp : forced) {
            for (var be : new ArrayList<>(level.getChunk(cp.x(), cp.z()).getBlockEntities().values())) {
                if (!(be instanceof org.overengineer.bettacraft.filter.FilterPumpHeadBlockEntity h)) continue;
                h.run(level, true);
                int media = 0;
                for (int m : h.media()) if (m / 1000 != org.overengineer.bettacraft.filter.FilterMedium.NONE.ordinal()) media++;
                boolean warns = h.tanks().stream().anyMatch(t -> t.status().warning());
                boolean tankWarns = h.tanks().stream().anyMatch(t -> {
                    var c = org.overengineer.bettacraft.tank.TankMultiblock.getController(level, t.tank());
                    return c != null && !org.overengineer.bettacraft.filter.MultiblockFilter.warnings(level, c).isEmpty();
                });
                if (!h.retrofit() || !warns || !tankWarns || h.cleanBlocks() > 0 || media == 0 || h.power() != 10) {
                    problems.add("M40 old multiblock filter at " + h.getBlockPos() + ": retrofit " + h.retrofit() + ", tanks " + h.tanks() + ", media " + media
                            + ", clean " + h.cleanBlocks() + ", power " + h.power());
                } else {
                    m40Checked++;
                }
            }
        }
    }

    // ------------------------------------------------------------------ M68 the old pipe system (an M67 world)

    /** The sides as a mask (bit 1 << 3D data value, as the old side wrote them). */
    private static int mask(java.util.function.Predicate<Direction> joined) {
        int m = 0;
        for (Direction d : Direction.values()) if (joined.test(d)) m |= 1 << d.get3DDataValue();
        return m;
    }

    private static String sides(int mask) {
        List<String> out = new ArrayList<>();
        for (Direction d : Direction.values()) if ((mask & 1 << d.get3DDataValue()) != 0) out.add(d.getName());
        return out.toString();
    }

    /** The role a migrated line may have now: its own (it reaches the filter) or none (a line without a filter port); never the other one. */
    private static void m68Role(String where, BlockPos pos, int slot, int oldLine) {
        PipeNetwork n = PipeNetwork.of(level(), pos, slot);
        if (n == null) {
            problems.add(where + ": no pipe network");
            return;
        }
        if (n.shortCircuit()) {
            problems.add(where + ": short circuit (old line " + oldLine + ")");
            return;
        }
        PipeRole want = oldLine == 0 ? PipeRole.INTAKE : oldLine == 1 ? PipeRole.RETURN : null;
        PipeRole have = n.role();
        if (have == want && want != null) m68Roles++;
        else if (have == PipeRole.INTAKE || have == PipeRole.RETURN) problems.add(where + ": role " + have + ", the old line was " + oldLine);
    }

    private static ServerLevel m68Level;

    private static ServerLevel level() {
        return m68Level;
    }

    /**
     * M68: every old pipe position holds a new pipe joined on exactly the old sides (filter pipes and T-pieces stainless steel, old
     * bundles slot bundles with slot = the old line, glass and steel pipes unchanged, the filter's water ports plain steel pipes), no
     * line in the other role or short-circuited, covers kept; every multiblock filter of the old world reaches the same tanks, filters
     * (a run of its care) and every tank that was OK is OK; every shared dock is now a dock per line, the intake guard behind the intake
     * line and the outlet guard (or the nozzle) behind the return line; every container holds the new items, counts 1:1.
     */
    private static void m68Check(ServerLevel level) {
        m68Level = level;
        m68Pipes = m68Roles = m68Filters = m68Docks = m68Containers = 0;
        org.overengineer.bettacraft.plumbing.PipeUpdates.flush(level);
        for (JsonElement e : pipesRef.getAsJsonArray("blocks")) {
            JsonObject o = e.getAsJsonObject();
            BlockPos pos = BlockPos.of(o.get("pos").getAsLong());
            String kind = o.get("kind").getAsString(), id = o.get("id").getAsString();
            String where = "M68 old " + kind + " " + id + " at " + pos.toShortString();
            BlockState s = level.getBlockState(pos);
            String now = BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString();
            int before = problems.size();
            switch (kind) {
                case "filter", "port", "pipe" -> {
                    String want = M68_BLOCKS.getOrDefault(id, id);
                    if (!now.equals(want) || !(s.getBlock() instanceof PipeBlock)) {
                        problems.add(where + ": now " + now + " instead of " + want);
                        continue;
                    }
                    if (!kind.equals("port")) { // a port had no sides: it joins what is next to it now
                        int want2 = o.get("sides").getAsInt(), have = mask(d -> s.getValue(PipeBlock.SIDES.get(d)));
                        if (want2 != have) problems.add(where + ": joined " + sides(want2) + " -> " + sides(have));
                    }
                    if (kind.equals("filter")) m68Role(where, pos, 0, o.get("line").getAsInt());
                    else if (PipeNetwork.of(level, pos, 0) != null && PipeNetwork.of(level, pos, 0).shortCircuit()) problems.add(where + ": short circuit");
                }
                case "bundle" -> {
                    if (!(s.getBlock() instanceof PipeBundleBlock) || !(level.getBlockEntity(pos) instanceof PipeBlockEntity be)) {
                        problems.add(where + ": now " + now + " instead of a pipe bundle");
                        continue;
                    }
                    int lines = 0;
                    for (JsonElement le : o.getAsJsonArray("lines")) {
                        JsonObject l = le.getAsJsonObject();
                        int line = l.get("line").getAsInt(), slot = be.slot(line), want = l.get("sides").getAsInt();
                        lines++;
                        if (!PipeSlots.present(slot) || !PipeSlots.steel(slot)) {
                            problems.add(where + ": slot " + line + " (old line " + l.get("name").getAsString() + ") missing or not steel: " + Integer.toHexString(slot));
                            continue;
                        }
                        int have = mask(d -> PipeSlots.joined(slot, d));
                        if (want != have) problems.add(where + ": line " + l.get("name").getAsString() + " joined " + sides(want) + " -> " + sides(have));
                        m68Role(where + " line " + l.get("name").getAsString(), pos, line, line);
                    }
                    if (PipeSlots.count(be.slots()) != lines) problems.add(where + ": " + PipeSlots.count(be.slots()) + " pipes instead of " + lines);
                }
                default -> problems.add(where + ": unknown kind");
            }
            if (o.has("cover")) {
                var cover = level.getBlockEntity(pos) instanceof PipeBlockEntity be ? be.cover() : null;
                String c = cover == null ? "none" : BuiltInRegistries.BLOCK.getKey(cover.getBlock()).toString();
                if (!c.equals(o.get("cover").getAsString())) problems.add(where + ": cover " + o.get("cover").getAsString() + " -> " + c);
            }
            if (problems.size() == before) m68Pipes++;
        }
        // the filters: same tanks, they filter (a run of the filter's care), no short circuit, OK where they were OK
        for (JsonElement e : pipesRef.getAsJsonArray("filters")) {
            JsonObject o = e.getAsJsonObject();
            BlockPos pos = BlockPos.of(o.get("pos").getAsLong());
            String where = "M68 old multiblock filter at " + pos.toShortString();
            if (!(level.getBlockEntity(pos) instanceof FilterPumpHeadBlockEntity h)) {
                problems.add(where + ": its pump head is gone");
                continue;
            }
            int before = problems.size();
            int moved = h.run(level, true);
            Map<Long, String> want = new TreeMap<>(), have = new TreeMap<>();
            for (JsonElement l : o.getAsJsonArray("links")) want.put(l.getAsJsonObject().get("tank").getAsLong(), l.getAsJsonObject().get("status").getAsString());
            for (var l : h.tanks()) have.put(l.tank().asLong(), l.status().name());
            if (h.problem() == MultiblockFilter.Problem.SHORT_CIRCUIT) problems.add(where + ": short circuit");
            else if (!h.problem().name().equals(o.get("problem").getAsString())) problems.add(where + ": problem " + o.get("problem").getAsString() + " -> " + h.problem());
            if (!want.keySet().equals(have.keySet())) problems.add(where + ": tanks " + want.keySet() + " -> " + have.keySet());
            for (var w : want.entrySet())
                if (w.getValue().equals("OK") && !"OK".equals(have.get(w.getKey()))) problems.add(where + ": tank " + BlockPos.of(w.getKey()).toShortString() + " OK -> " + have.get(w.getKey()));
            if (o.get("moved").getAsInt() > 0 && moved <= 0) problems.add(where + ": filtered " + o.get("moved").getAsInt() + ", now " + moved + " (" + h.tanks() + ")");
            if (problems.size() == before) m68Filters++;
        }
        // the shared docks: a dock per line, each line's guard behind its own dock, nothing shared any more
        for (JsonElement e : pipesRef.getAsJsonArray("sharedDocks")) {
            JsonObject o = e.getAsJsonObject();
            BlockPos member = BlockPos.of(o.get("member").getAsLong());
            Direction face = Direction.byName(o.get("face").getAsString());
            String where = "M68 old shared dock " + member.toShortString() + " " + face.getName();
            TankBlockEntity c = org.overengineer.bettacraft.tank.TankMultiblock.getController(level, BlockPos.of(o.get("tank").getAsLong()));
            if (c == null || !(level.getBlockEntity(BlockPos.of(o.get("filter").getAsLong())) instanceof FilterPumpHeadBlockEntity h)) {
                problems.add(where + ": tank or filter gone");
                continue;
            }
            int before = problems.size();
            if (TankAnchors.lines(level, member, face).length < 2) problems.add(where + ": lines " + java.util.Arrays.toString(TankAnchors.lines(level, member, face)));
            int intakes = 0, returns = 0;
            for (MultiblockFilter.Link l : h.currentLinks(level)) {
                if (!l.tank().getBlockPos().equals(c.getBlockPos())) continue;
                for (boolean intake : new boolean[]{true, false})
                    for (org.overengineer.bettacraft.plumbing.FilterLines.Dock d : intake ? l.intakes() : l.returns()) {
                        if (!d.member().equals(member) || d.face() != face) continue;
                        TankAnchors.Anchor a = TankAnchors.anchor(level, c, member, face, d.slot(), false);
                        if (a == null || a.shared() || a.line() == 0) {
                            problems.add(where + ": the " + (intake ? "intake" : "return") + " line (slot " + d.slot() + ") has no dock of its own: " + a);
                            continue;
                        }
                        var n = org.overengineer.bettacraft.plumbing.interior.InteriorPlumbing.net(a.region(), a.key());
                        boolean ok = intake ? !n.intakes().isEmpty() && n.outflows().isEmpty() : !n.outflows().isEmpty() && n.intakes().isEmpty();
                        if (!ok) problems.add(where + ": behind the " + (intake ? "intake" : "return") + " line's dock (line " + a.line() + "): " + n);
                        if (intake) intakes++;
                        else returns++;
                    }
            }
            if (intakes != 1 || returns != 1) problems.add(where + ": " + intakes + " intake and " + returns + " return docks of the filter there");
            if (problems.size() == before) m68Docks++;
        }
        // the containers: the new items, counted 1:1 (shulker boxes in them too)
        for (JsonElement e : pipesRef.getAsJsonArray("containers")) {
            JsonObject o = e.getAsJsonObject();
            BlockPos pos = BlockPos.of(o.get("pos").getAsLong());
            Map<String, Integer> want = new TreeMap<>(), have = new TreeMap<>();
            for (var c : o.getAsJsonObject("counts").entrySet()) want.merge(M68_ITEMS.getOrDefault(c.getKey(), c.getKey()), c.getValue().getAsInt(), Integer::sum);
            if (level.getBlockEntity(pos) instanceof net.minecraft.world.Container box) for (int i = 0; i < box.getContainerSize(); i++) m68Count(box.getItem(i), have);
            if (!want.equals(have)) problems.add("M68 " + o.get("block").getAsString() + " at " + pos.toShortString() + ": " + want + " -> " + have);
            else m68Containers++;
        }
        if (m68Pipes == 0 || m68Filters == 0 || m68Docks == 0 || m68Containers == 0) problems.add("M68: nothing of the old pipe system checked");
    }

    private static void m68Count(ItemStack s, Map<String, Integer> out) {
        if (s.isEmpty()) return;
        out.merge(BuiltInRegistries.ITEM.getKey(s.getItem()).toString(), s.getCount(), Integer::sum);
        var in = s.get(net.minecraft.core.component.DataComponents.CONTAINER);
        if (in == null) return;
        // 26.1: the contents hold item templates; copied out as stacks
        net.minecraft.core.NonNullList<ItemStack> list = net.minecraft.core.NonNullList.withSize(64, ItemStack.EMPTY);
        in.copyInto(list);
        list.forEach(i -> m68Count(i, out));
    }

    /** M68 stage 1: the entries of {@code a} (an array of objects) with {@code field} (and the ids in an {@code items} text) mapped by {@code ids}. */
    private static JsonElement m68Ids(JsonElement a, String field, Map<String, String> ids) {
        JsonArray out = new JsonArray();
        for (JsonElement e : a.getAsJsonArray()) {
            JsonObject o = e.getAsJsonObject().deepCopy();
            if (o.has(field)) o.addProperty(field, ids.getOrDefault(o.get(field).getAsString(), o.get(field).getAsString()));
            if (o.has("items") && o.get("items").isJsonPrimitive()) {
                String t = o.get("items").getAsString();
                for (var m : M68_ITEMS.entrySet()) t = t.replace(":" + m.getKey() + "x", ":" + m.getValue() + "x");
                o.addProperty("items", t);
            }
            out.add(o);
        }
        return out;
    }

    /** M68 stage 1: the interior of the tank at {@code pos} with one anchor more per shared dock of it (split into a dock per line). */
    private static JsonObject m68Interior(long pos, JsonObject interior) {
        int more = 0;
        for (JsonElement e : pipesRef.getAsJsonArray("sharedDocks")) if (e.getAsJsonObject().get("tank").getAsLong() == pos) more++;
        if (more == 0) return interior;
        JsonObject o = interior.deepCopy();
        String k = "bettacraft:pipe_anchor";
        o.addProperty(k, (o.has(k) ? o.get(k).getAsInt() : 0) + more);
        return o;
    }

    // ------------------------------------------------------------------ M35.1 old cyst jars

    /** {@code n} jars as M21.8..M35 saved them: the spent uses in {@code minecraft:damage}. */
    private static net.minecraft.nbt.CompoundTag oldJars(int n, int damage, int slot) {
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        t.putString("id", "bettacraft:artemia_cysts");
        t.putInt("count", n);
        net.minecraft.nbt.CompoundTag c = new net.minecraft.nbt.CompoundTag();
        c.putInt("minecraft:damage", damage);
        t.put("components", c);
        if (slot >= 0) t.putByte("Slot", (byte) slot);
        return t;
    }

    private static void plainJars(String where, ItemStack s, int count) {
        if (!s.is(org.overengineer.bettacraft.registry.ModItems.ARTEMIA_CYSTS.get()) || s.getCount() != count
                || s.has(net.minecraft.core.component.DataComponents.DAMAGE)
                || !ItemStack.isSameItemSameComponents(s, new ItemStack(org.overengineer.bettacraft.registry.ModItems.ARTEMIA_CYSTS.get()))) {
            problems.add("M35.1 " + where + ": expected " + count + " plain cyst jars, got " + s + " " + s.getComponentsPatch());
        } else {
            m351Checked++;
        }
    }

    /**
     * M67 stage 1: the first tank of the reference world with a cabinet under it gets a wound cabinet spring in the save format of
     * the versions before M67 (100000 device-ticks, more than a spring holds now); a reference world without one (the ports' worlds
     * stand on the ground) gets an external filter at its first tank with the wound spring instead. The world is saved like that;
     * the tank, the device and the FE its store must hold afterwards go to {@link #M67_FILE}.
     */
    private static void m67Place(MinecraftServer server, ServerLevel level) throws java.io.IOException {
        for (int pass = 0; pass < 2; pass++) for (JsonElement e : expected.getAsJsonArray("tanks")) {
            BlockPos p = BlockPos.of(e.getAsJsonObject().get("pos").getAsLong());
            TankBlockEntity c = level.isLoaded(p) ? org.overengineer.bettacraft.tank.TankMultiblock.getController(level, p) : null;
            if (c == null) continue;
            net.minecraft.world.level.block.entity.BlockEntity be = null;
            var cabs = org.overengineer.bettacraft.tank.TankStore.cabinets(level, c);
            if (!cabs.isEmpty()) be = cabs.getFirst();
            else if (pass == 1) { // no tank of the reference world stands on a cabinet: an external filter at this one
                BlockPos at = c.getTank().min().west();
                if (level.getBlockEntity(at) != null) continue; // (the ports' reference tanks stand buried in dirt: that may go)
                level.setBlockAndUpdate(at, org.overengineer.bettacraft.registry.ModBlocks.EXTERNAL_FILTER.get().defaultBlockState()
                        .setValue(org.overengineer.bettacraft.equipment.ExternalFilterBlock.FACING, net.minecraft.core.Direction.EAST));
                org.overengineer.bettacraft.tank.TankEquipment.invalidate(c);
                be = level.getBlockEntity(at);
                if (org.overengineer.bettacraft.tank.TankStore.tankOf(level, (org.overengineer.bettacraft.winding.Windable) be) != c) continue;
            }
            if (!(be instanceof org.overengineer.bettacraft.winding.Windable w)) continue;
            net.minecraft.nbt.CompoundTag tag = be.saveWithFullMetadata(level.registryAccess());
            long ticks = 100_000;
            tag.putLong("spring", tag.getLongOr("spring", 0L) + ticks);
            be.loadWithComponents(org.overengineer.bettacraft.util.NbtCompat.input(level.registryAccess(), tag));
            long want = c.getTank().powerBuffer() + org.overengineer.bettacraft.tank.TankStore.springFe(w.windSpring().ticks());
            be.setChanged();
            JsonObject o = new JsonObject();
            o.addProperty("tank", c.getBlockPos().asLong());
            o.addProperty("device", be.getBlockPos().asLong());
            o.addProperty("fe", want);
            Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve(M67_FILE), o.toString(), StandardCharsets.UTF_8);
            LOGGER.info("[smoketest] upgrade: M67 {} at {} of tank {} wound {} device-ticks, its store must hold {} FE", w.windDevice(), be.getBlockPos(),
                    c.getBlockPos(), ticks, want);
            return;
        }
        problems.add("M67: no tank for a wound spring in the reference world");
    }

    /** M67 stage 2: the wound spring of stage 1 is energy in the tank's store, the spring is empty, nothing is lost. */
    private static void m67Check(MinecraftServer server, ServerLevel level) throws java.io.IOException {
        Path f = server.getWorldPath(LevelResource.ROOT).resolve(M67_FILE);
        if (!Files.exists(f)) {
            problems.add("M67: " + M67_FILE + " missing");
            return;
        }
        JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
        BlockPos tp = BlockPos.of(o.get("tank").getAsLong()), dp = BlockPos.of(o.get("device").getAsLong());
        long want = o.get("fe").getAsLong();
        TankBlockEntity c = org.overengineer.bettacraft.tank.TankMultiblock.getController(level, tp);
        if (c == null || !(level.getBlockEntity(dp) instanceof org.overengineer.bettacraft.winding.Windable w)) {
            problems.add("M67: tank " + tp + " or device " + dp + " gone");
            return;
        }
        org.overengineer.bettacraft.tank.TankPower.step(level, c, 1); // the migration runs in the care step
        long have = c.getTank().powerBuffer();
        boolean costs = org.overengineer.bettacraft.tank.TankPower.anyRequired();
        if (!w.windSpring().empty()) problems.add("M67: the spring still holds " + w.windSpring().ticks());
        else if (costs ? have < want / 2 : have != want) problems.add("M67: the store holds " + have + " FE, the spring was worth " + want);
        else m67Checked++;
    }

    /** Stage 1: a chest (16 old jars), a hatchery (16 old jars and an open one) and an item entity (16 old jars) above a chest of the reference world. */
    private static void m351Place(MinecraftServer server, ServerLevel level) throws java.io.IOException {
        var reg = server.registryAccess();
        BlockPos base = BlockPos.of(expected.getAsJsonArray("chests").get(0).getAsJsonObject().get("pos").getAsLong()).above(24);
        level.setBlock(base, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(), 3);
        net.minecraft.nbt.CompoundTag chest = new net.minecraft.nbt.CompoundTag();
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        list.add(oldJars(16, 7, 0));
        chest.put("Items", list);
        level.getBlockEntity(base).loadWithComponents(org.overengineer.bettacraft.util.NbtCompat.input(reg, chest));
        level.getBlockEntity(base).setChanged();
        plainJars("chest", ((ChestBlockEntity) level.getBlockEntity(base)).getItem(0), 16);
        BlockPos hp = base.above(2);
        level.setBlock(hp, ModBlocks.ARTEMIA_HATCHERY.get().defaultBlockState(), 3);
        var tub = (org.overengineer.bettacraft.food.CultureTubBlockEntity) level.getBlockEntity(hp);
        net.minecraft.nbt.CompoundTag tag = tub.saveWithoutMetadata(reg);
        net.minecraft.nbt.CompoundTag items = new net.minecraft.nbt.CompoundTag();
        net.minecraft.nbt.ListTag il = new net.minecraft.nbt.ListTag();
        il.add(oldJars(16, 3, 0));
        items.put("Items", il);
        items.putInt("Size", 3);
        tag.put("items", items);
        tag.put("opened_jar", oldJars(1, 12, -1));
        tub.loadWithComponents(org.overengineer.bettacraft.util.NbtCompat.input(reg, tag));
        tub.setChanged();
        plainJars("hatchery", tub.items().getStackInSlot(org.overengineer.bettacraft.food.CultureTubBlockEntity.INPUT), 17);
        net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(level, base.getX() + 0.5, base.getY() + 4, base.getZ() + 0.5,
                new ItemStack(org.overengineer.bettacraft.registry.ModItems.ARTEMIA_CYSTS.get()));
        net.minecraft.nbt.CompoundTag et = new net.minecraft.nbt.CompoundTag();
        et.merge(NbtCompat.saveEntityWithoutId(drop));
        et.put("Item", oldJars(16, 9, -1));
        net.minecraft.world.entity.item.ItemEntity loaded = new net.minecraft.world.entity.item.ItemEntity(net.minecraft.world.entity.EntityTypes.ITEM, level);
        org.overengineer.bettacraft.util.NbtCompat.loadEntity(loaded, et);
        loaded.setUUID(UUID.randomUUID());
        loaded.setNoGravity(true);
        loaded.setUnlimitedLifetime();
        loaded.setNeverPickUp();
        level.addFreshEntity(loaded);
        plainJars("item entity", loaded.getItem(), 16);
        Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve(M351_FILE), base.asLong() + " " + loaded.getUUID(), StandardCharsets.UTF_8);
    }

    /** Stage 2: the jars of stage 1 after saving and loading the world. */
    private static void m351Check(MinecraftServer server, ServerLevel level) throws java.io.IOException {
        Path file = server.getWorldPath(LevelResource.ROOT).resolve(M351_FILE);
        if (!Files.exists(file)) {
            problems.add("M35.1: missing " + file);
            return;
        }
        String[] a = Files.readString(file, StandardCharsets.UTF_8).trim().split(" ");
        BlockPos base = BlockPos.of(Long.parseLong(a[0]));
        level.getChunk(base);
        if (level.getBlockEntity(base) instanceof ChestBlockEntity chest) plainJars("chest after reload", chest.getItem(0), 16);
        else problems.add("M35.1: the chest is gone");
        if (level.getBlockEntity(base.above(2)) instanceof org.overengineer.bettacraft.food.CultureTubBlockEntity tub) {
            plainJars("hatchery after reload", tub.items().getStackInSlot(org.overengineer.bettacraft.food.CultureTubBlockEntity.INPUT), 17);
            if (!tub.openedJar().isEmpty()) problems.add("M35.1: the hatchery still has an open jar");
        } else {
            problems.add("M35.1: the hatchery is gone");
        }
        Entity e = level.getEntity(UUID.fromString(a[1]));
        if (e instanceof net.minecraft.world.entity.item.ItemEntity ie) plainJars("item entity after reload", ie.getItem(), 16);
        else problems.add("M35.1: the item entity is gone: " + e);
    }

    // ------------------------------------------------------------------ compare

    private static void compare(MinecraftServer server, ServerLevel level) {
        JsonObject now = snapshot(server, level, expected);
        Map<Long, JsonObject> got = new TreeMap<>();
        for (JsonElement e : now.getAsJsonArray("tanks")) got.put(e.getAsJsonObject().get("pos").getAsLong(), e.getAsJsonObject());
        int animals = 0, critters = 0, equipment = 0, converted = 0;
        for (JsonElement e : expected.getAsJsonArray("tanks")) {
            JsonObject want = e.getAsJsonObject();
            long pos = want.get("pos").getAsLong();
            String where = "tank " + BlockPos.of(pos).toShortString() + " '" + want.get("name").getAsString() + "'";
            JsonObject have = got.get(pos);
            if (have == null) {
                problems.add(where + ": no tank controller any more");
                continue;
            }
            for (String k : new String[]{"name", "water", "members", "front", "locked", "owner", "signVisible", "regionId"}) {
                // M40: a tank without an interior may get one (a multiblock filter puts the docks of its pipes in): nothing lost
                if (k.equals("regionId") && want.get(k).getAsInt() == -1 && have.get(k).getAsInt() >= 0) continue;
                // M68: an open tank evaporates in the ticks between the save and this comparison (the M67 world's giant tanks have
                // no lid): at most what it can lose in that game time (twice its rate per day, plus 1 mB of rounding), never more
                if (k.equals("water") && expected.has("gameTime") && have.get(k).getAsLong() <= want.get(k).getAsLong()) {
                    TankBlockEntity tc = level.getBlockEntity(BlockPos.of(pos)) instanceof TankBlockEntity tb ? tb : null;
                    long elapsed = Math.max(0, level.getGameTime() - expected.get("gameTime").getAsLong());
                    double evap = tc == null ? 0 : org.overengineer.bettacraft.tank.TankWater.evaporationPerDay(level, tc.getTank());
                    long lost = want.get(k).getAsLong() - have.get(k).getAsLong();
                    if (lost <= (long) Math.ceil(2 * evap * (elapsed + 100) / 24000.0) + 1) {
                        if (lost > 0) evaporated += lost;
                        continue;
                    }
                }
                if (!want.get(k).equals(have.get(k))) problems.add(where + ": " + k + " " + want.get(k) + " -> " + have.get(k));
            }
            // floating plants spread while the world runs: none may be lost, a few more are fine
            int fw = want.get("floating").getAsInt(), fh = have.get("floating").getAsInt();
            if (fh < fw || fh > fw + Math.max(3, fw / 4)) problems.add(where + ": floating plants " + fw + " -> " + fh);
            // M31: a fish tank of an older world stays an aquarium (a nearly full tank has no land part)
            if (want.get("water").getAsLong() > 0 && have.has("terra_mode") && !"aquarium".equals(have.get("terra_mode").getAsString())) {
                LOGGER.info("[upgrade] {} is now a {} (land {})", where, have.get("terra_mode").getAsString(), have.get("land"));
                if (have.has("cap") && want.get("water").getAsLong() >= 0.9 * have.get("cap").getAsLong()) {
                    problems.add(where + ": a full fish tank became a " + have.get("terra_mode").getAsString());
                }
            }
            // M33.1: a tank of an older world starts on the automatic tank type (and keeps a type it had)
            String wantType = want.has("tank_mode") ? want.get("tank_mode").getAsString() : "auto";
            if (!legacy) {
                // a newer world: its tank types were not written into the reference
            } else if (have.has("tank_mode") && !wantType.equals(have.get("tank_mode").getAsString())) {
                problems.add(where + ": tank type " + wantType + " -> " + have.get("tank_mode").getAsString());
            } else if (have.has("tank_mode")) m331Checked++;
            if (Math.abs(want.get("salinity").getAsDouble() - have.get("salinity").getAsDouble()) > 1e-3) {
                problems.add(where + ": salinity " + want.get("salinity") + " -> " + have.get("salinity"));
            }
            // the animals: the same UUIDs; the ones the old version had loaded come back with the same data
            Map<String, JsonObject> haveA = new TreeMap<>();
            for (JsonElement a : have.getAsJsonArray("inhabitants")) haveA.put(a.getAsJsonObject().get("uuid").getAsString(), a.getAsJsonObject());
            for (JsonElement a : want.getAsJsonArray("inhabitants")) {
                JsonObject wa = a.getAsJsonObject();
                JsonObject ha = haveA.remove(wa.get("uuid").getAsString());
                if (ha == null) {
                    problems.add(where + ": animal " + wa + " no longer listed");
                    continue;
                }
                if (!wa.has("type")) continue; // the old version had not loaded it either
                animals++;
                for (String k : new String[]{"type", "variety", "female", "stage", "genome", "lb", "strain"}) {
                    if (wa.has(k) && !wa.get(k).equals(ha.get(k))) problems.add(where + ": animal " + wa.get("uuid").getAsString() + " " + k + " " + wa.get(k) + " -> " + ha.get(k));
                }
                // M21.6: guppies and endlers of an older version (no genome yet) get the genome of their variety
                if (legacy && ha.has("lb_expected") && !wa.has("lb")) {
                    converted++;
                    if (!ha.get("lb_expected").equals(ha.get("lb"))) {
                        problems.add(where + ": animal " + wa.get("uuid").getAsString() + " genome " + ha.get("lb") + ", expected " + ha.get("lb_expected"));
                    }
                    if (ha.has("strain_expected") && !ha.get("strain_expected").equals(ha.get("strain"))) {
                        problems.add(where + ": animal " + wa.get("uuid").getAsString() + " strain " + ha.get("strain") + ", expected " + ha.get("strain_expected"));
                    }
                }
            }
            if (!haveA.isEmpty()) problems.add(where + ": new animals " + haveA.keySet());
            String lostOrNew = critterDiff(want.getAsJsonArray("critters"), have.getAsJsonArray("critters"));
            if (lostOrNew != null) problems.add(where + ": small animals " + lostOrNew);
            critters += want.getAsJsonArray("critters").size();
            // M68 (an M67 world, stage 1): a shared dock became a dock per line (one anchor more each, the guards only moved), the old
            // pipes around the tank are stainless steel pipes now (registry aliases)
            boolean m68 = STAGE == 1 && pipesRef != null;
            JsonObject wantInterior = want.has("interior") ? want.getAsJsonObject("interior") : null;
            if (m68 && wantInterior != null) wantInterior = m68Interior(pos, wantInterior);
            if (wantInterior != null && !wantInterior.equals(have.get("interior"))) {
                problems.add(where + ": interior " + diff(wantInterior, have.getAsJsonObject("interior")));
            }
            JsonElement wantEquipment = m68 ? m68Ids(want.get("equipment"), "block", M68_BLOCKS) : want.get("equipment");
            if (!wantEquipment.equals(have.get("equipment"))) problems.add(where + ": equipment " + wantEquipment + " -> " + have.get("equipment"));
            // M22.3: every animal of an older world counts as analysed (magnifier)
            // M59: small animals born while loading (allowed above) are new and not analysed yet
            int born = Math.max(0, (have.has("critters") ? have.getAsJsonArray("critters").size() : 0) - want.getAsJsonArray("critters").size());
            if (legacy && have.has("unanalysed") && have.get("unanalysed").getAsInt() > born) problems.add(where + ": " + have.get("unanalysed") + " animals not analysed");
            equipment += want.getAsJsonArray("equipment").size();
        }
        Map<Long, JsonElement> chests = new TreeMap<>();
        for (JsonElement e : now.getAsJsonArray("chests")) chests.put(e.getAsJsonObject().get("pos").getAsLong(), e.getAsJsonObject().get("items"));
        int items = 0;
        for (JsonElement e : expected.getAsJsonArray("chests")) {
            long pos = e.getAsJsonObject().get("pos").getAsLong();
            JsonElement want = e.getAsJsonObject().get("items");
            if (STAGE == 1 && pipesRef != null) want = m68Ids(want, "id", M68_ITEMS); // M68: old pipe items are the new pipes, slot for slot
            if (!want.equals(chests.get(pos))) problems.add("chest " + BlockPos.of(pos).toShortString() + ": " + want + " -> " + chests.get(pos));
            items += want.getAsJsonArray().size();
        }
        LOGGER.info("[smoketest] upgrade stage {}: compared {} tanks, {} animals, {} small animals, {} equipment blocks, {} items in chests: {} difference(s)",
                STAGE, expected.getAsJsonArray("tanks").size(), animals, critters, equipment, items, problems.size());
        LOGGER.info("[smoketest] upgrade stage {}: {} guppies and endlers converted from their old variety", STAGE, converted);
        if (evaporated > 0) LOGGER.info("[smoketest] upgrade stage {}: {} mB evaporated from open tanks since the save (allowed)", STAGE, evaporated);
        if (STAGE == 1 && legacy && converted < 16) problems.add("only " + converted + " old guppies and endlers converted (the reference has every old variety)");
    }

    /**
     * M21.7: every small animal of the reference must still be there (same species, variety, sex and genes); new ones
     * are fine if they are of a species that already lived in the tank (the snails keep breeding while the world loads,
     * a predatory snail can hatch in the 40 ticks). Null if that holds, otherwise what is missing or new.
     */
    private static String critterDiff(JsonArray want, JsonArray have) {
        List<String> left = new ArrayList<>();
        if (have != null) for (JsonElement e : have) left.add(e.getAsString());
        List<String> missing = new ArrayList<>();
        Set<String> species = new java.util.HashSet<>();
        for (JsonElement e : want) {
            String k = e.getAsString();
            species.add(k.substring(0, k.indexOf('|') < 0 ? k.length() : k.indexOf('|')));
            if (!left.remove(k)) missing.add(k);
        }
        List<String> foreign = new ArrayList<>();
        int born = 0;
        for (String k : left) {
            if (species.contains(k.substring(0, k.indexOf('|') < 0 ? k.length() : k.indexOf('|')))) born++;
            else foreign.add(k);
        }
        if (born > 0) LOGGER.info("[smoketest] upgrade: {} small animal(s) born while loading (allowed)", born);
        if (missing.isEmpty() && foreign.isEmpty()) return null;
        return "missing " + missing + ", new of another species " + foreign;
    }

    private static String diff(JsonObject a, JsonObject b) {
        StringBuilder sb = new StringBuilder();
        Set<String> keys = new java.util.TreeSet<>(a.keySet());
        if (b != null) keys.addAll(b.keySet());
        for (String k : keys) {
            int x = a.has(k) ? a.get(k).getAsInt() : 0, y = b != null && b.has(k) ? b.get(k).getAsInt() : 0;
            if (x != y) sb.append(k).append(' ').append(x).append("->").append(y).append("; ");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ snapshot (same format as the old version's)

    private static void write(MinecraftServer server, ServerLevel level, String name) throws java.io.IOException {
        JsonObject s = snapshot(server, level, expected);
        s.addProperty("version", org.overengineer.bettacraft.platform.Platform.get().modVersion(BettaCraft.MOD_ID));
        s.addProperty("gameTime", level.getGameTime()); // M68: for the evaporation allowed in stage 2
        Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve(name), new GsonBuilder().setPrettyPrinting().create().toJson(s));
    }

    /** The tanks and chests at the positions of {@code like}. */
    private static JsonObject snapshot(MinecraftServer server, ServerLevel level, JsonObject like) {
        JsonObject root = new JsonObject();
        JsonArray tanks = new JsonArray(), chests = new JsonArray();
        for (JsonElement e : like.getAsJsonArray("tanks")) {
            BlockPos p = BlockPos.of(e.getAsJsonObject().get("pos").getAsLong());
            if (level.getBlockEntity(p) instanceof TankBlockEntity t && t.isController() && t.getTank() != null) tanks.add(tank(server, level, t));
        }
        for (JsonElement e : like.getAsJsonArray("chests")) {
            BlockPos p = BlockPos.of(e.getAsJsonObject().get("pos").getAsLong());
            if (level.getBlockEntity(p) instanceof ChestBlockEntity cb) chests.add(chest(cb));
        }
        root.add("tanks", tanks);
        root.add("chests", chests);
        return root;
    }

    private static JsonObject tank(MinecraftServer server, ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        // M24: a tank of an older version starts without nitrate (40 ticks of care add a trace at most) and without bulkheads
        if (STAGE == 1 && legacy) {
            if (t.nitrate() > 0.5F) problems.add("tank " + c.getBlockPos().toShortString() + ": nitrate " + t.nitrate() + " instead of 0");
            if (t.lastWaterChange() >= 0) problems.add("tank " + c.getBlockPos().toShortString() + ": a water change noted");
            m24Checked++;
            // M25 part b: no CO2 source in an old world: the CO2 stays at the air's
            if (Math.abs(t.co2() - org.overengineer.bettacraft.chem.WaterChemistry.CO2_AIR) > 0.5)
                problems.add("tank " + c.getBlockPos().toShortString() + ": CO2 " + t.co2() + " instead of the air's");
            // M25: tanks of earlier versions hold tap water and no animal of ours minds it
            if (Math.abs(t.tds() - org.overengineer.bettacraft.chem.WaterChemistry.TAP_TDS) > 5 || Math.abs(t.ph() - org.overengineer.bettacraft.chem.WaterChemistry.startPh(t.salinity())) > 0.1)
                problems.add("tank " + c.getBlockPos().toShortString() + ": TDS " + t.tds() + " / pH " + t.ph() + " instead of tap water");
            // M25 part c: alkalinity and magnesium of an old tank follow its salt (sea water: 8 dKH, 1300 mg/L; fresh: tap water)
            if (Math.abs(t.kh() - org.overengineer.bettacraft.chem.WaterChemistry.khOf(t.salinity())) > 0.3
                    || Math.abs(t.magnesium() - org.overengineer.bettacraft.chem.WaterChemistry.magnesiumOf(t.salinity())) > 20)
                problems.add("tank " + c.getBlockPos().toShortString() + ": KH " + t.kh() + " / Mg " + t.magnesium() + " instead of the values of its salt");
            else m25cChecked++;
            for (var e : org.overengineer.bettacraft.tank.TankInhabitants.liveInhabitants(level, t)) {
                if (org.overengineer.bettacraft.chem.TankChemistry.off(t, org.overengineer.bettacraft.chem.ChemTolerance.of(e)))
                    problems.add("tank " + c.getBlockPos().toShortString() + ": the water does not suit " + e.getName().getString());
            }
            for (var k : t.critters()) {
                if (k.alive() && org.overengineer.bettacraft.chem.TankChemistry.off(t, org.overengineer.bettacraft.chem.ChemTolerance.critter(k.species)))
                    problems.add("tank " + c.getBlockPos().toShortString() + ": the water does not suit " + k.species.id());
            }
        }
        JsonObject o = new JsonObject();
        o.addProperty("pos", c.getBlockPos().asLong());
        o.addProperty("name", t.name());
        o.addProperty("water", t.waterMb());
        o.addProperty("salinity", t.salinity());
        o.addProperty("terra_mode", t.terrariumMode().id()); // M31 (absent in the snapshots of older versions)
        o.addProperty("tank_mode", t.tankMode().id()); // M33.1 (absent in the snapshots of older versions)
        o.addProperty("cap", t.capacityMb());
        o.addProperty("land", t.landShare());
        o.addProperty("members", t.memberCount());
        o.addProperty("front", t.front().getName());
        o.addProperty("locked", t.locked());
        o.addProperty("owner", t.owner() == null ? "" : t.owner().toString());
        o.addProperty("signVisible", t.signVisible());
        o.addProperty("floating", t.plants().size());
        o.addProperty("regionId", t.regionId());
        JsonArray inh = new JsonArray();
        for (UUID u : t.inhabitants()) {
            JsonObject a = new JsonObject();
            a.addProperty("uuid", u.toString());
            Entity e = level.getEntity(u);
            if (e != null) {
                a.addProperty("type", BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString());
                if (e instanceof TankmateEntity m) {
                    a.addProperty("variety", m.variety());
                    a.addProperty("female", m.isFemale());
                    a.addProperty("stage", m.lifeStage().name());
                    // M21.6: the guppy and endler genome, its strain and what the old variety should have become
                    var lb = m.genes().lb();
                    if (lb != null) {
                        a.addProperty("lb", lb.toString());
                        var strain = org.overengineer.bettacraft.genetics.livebearer.LivebearerStrain.of(lb.express(m.isFemale()));
                        a.addProperty("strain", strain == null ? "" : strain.id());
                        boolean endler = m.species() == org.overengineer.bettacraft.tankmates.TankmateSpecies.ENDLER_GUPPY;
                        a.addProperty("lb_expected", org.overengineer.bettacraft.genetics.livebearer.LivebearerLegacy.convert(endler, m.genes().a(),
                                m.genes().b(), m.variety()).toString());
                        if (!m.isFemale() && m.genes().pure()) {
                            var want = (endler ? org.overengineer.bettacraft.genetics.livebearer.LivebearerLegacy.ENDLER_STRAINS
                                    : org.overengineer.bettacraft.genetics.livebearer.LivebearerLegacy.GUPPY_STRAINS).get(m.variety());
                            if (want != null) a.addProperty("strain_expected", want.id());
                        }
                    }
                } else if (e instanceof BettaEntity b) {
                    a.addProperty("genome", b.genome().toString());
                    a.addProperty("female", b.isFemale());
                    a.addProperty("stage", b.lifeStage().name());
                }
            }
            inh.add(a);
        }
        o.add("inhabitants", inh);
        JsonArray crit = new JsonArray();
        List<String> cs = new ArrayList<>();
        for (Critter k : t.critters()) cs.add(k.species.id() + "|" + k.genes.variety(k.species) + "|" + k.female + "|" + k.genes.a() + "/" + k.genes.b());
        java.util.Collections.sort(cs);
        cs.forEach(crit::add);
        o.add("critters", crit);
        // M22.3: animals of the old world that would hide their genes (must be none)
        int unanalysed = 0;
        for (Critter k : t.critters()) if (!k.analysed) unanalysed++;
        for (UUID u : t.inhabitants()) {
            Entity e = level.getEntity(u);
            if (e instanceof BettaEntity b && !b.getBettaData().analysed() || e instanceof TankmateEntity m && !m.analysed()) unanalysed++;
        }
        if (STAGE == 1) o.addProperty("unanalysed", unanalysed);
        if (t.regionId() >= 0) {
            InteriorRegion r = InteriorRegions.get(server).get(t.regionId());
            ServerLevel in = InteriorManager.interiorLevel(server);
            if (r != null && in != null) {
                Map<String, Integer> counts = new TreeMap<>();
                for (int y = 0; y < r.sizeY(); y++)
                    for (int z = 0; z < r.sizeZ(); z++)
                        for (int x = 0; x < r.sizeX(); x++) {
                            BlockState s = in.getBlockState(r.toWorld(x, y, z));
                            if (s.isAir()) continue;
                            counts.merge(BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString(), 1, Integer::sum);
                        }
                JsonObject co = new JsonObject();
                counts.forEach(co::addProperty);
                o.add("interior", co);
            }
        }
        JsonArray eq = new JsonArray();
        BlockPos lo = t.min().offset(-1, -1, -1), hi = t.max().offset(1, 1, 1);
        for (BlockPos p : BlockPos.betweenClosed(lo, hi)) {
            BlockState s = level.getBlockState(p);
            Identifier id = BuiltInRegistries.BLOCK.getKey(s.getBlock());
            if (!id.getNamespace().equals(BettaCraft.MOD_ID) || s.is(ModBlocks.TANK.get())) continue;
            JsonObject e = new JsonObject();
            e.addProperty("pos", p.asLong());
            e.addProperty("block", id.toString());
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof CabinetBlockEntity cab) {
                e.addProperty("wood", cab.woodId().toString());
                e.addProperty("filter", BuiltInRegistries.ITEM.getKey(cab.filterItem().getItem()).toString());
                e.addProperty("heater0", BuiltInRegistries.ITEM.getKey(cab.heater(0).getItem()).toString());
                e.addProperty("items", items(cab.items()));
                // M22.4: a cabinet of an older world keeps its hose on the back wall
                if (legacy && cab.outletSetting() != org.overengineer.bettacraft.equipment.OutletSide.BACK) {
                    problems.add("cabinet " + p.toShortString() + ": outlet side " + cab.outletSetting() + " instead of the back wall");
                }
                cabinetsChecked++;
                // M44: an old cabinet becomes Auto with joints, and a tick after loading it shows the front of its row
                if (legacy && (cab.frontSetting() != org.overengineer.bettacraft.equipment.CabinetFrontSetting.AUTO || !cab.joints())) {
                    problems.add("cabinet " + p.toShortString() + ": front " + cab.frontSetting() + ", joints " + cab.joints() + " instead of auto with joints");
                }
                BlockState want = org.overengineer.bettacraft.equipment.CabinetBlock.withFront(s, level, p);
                if (s.getValue(org.overengineer.bettacraft.equipment.CabinetBlock.FRONT) != want.getValue(org.overengineer.bettacraft.equipment.CabinetBlock.FRONT)
                        || s.getValue(org.overengineer.bettacraft.equipment.CabinetBlock.JOINTS) != want.getValue(org.overengineer.bettacraft.equipment.CabinetBlock.JOINTS)) {
                    problems.add("cabinet " + p.toShortString() + ": shows " + s.getValue(org.overengineer.bettacraft.equipment.CabinetBlock.FRONT) + " instead of "
                            + want.getValue(org.overengineer.bettacraft.equipment.CabinetBlock.FRONT));
                }
                cabinetFronts.add(s.getValue(org.overengineer.bettacraft.equipment.CabinetBlock.FRONT).getSerializedName());
                // M41: a cabinet of a world from before M41 takes no Create shaft (schema v3: drive none)
                if (cab.driveSetting() != org.overengineer.bettacraft.equipment.DriveSide.NONE) {
                    problems.add("cabinet " + p.toShortString() + ": drive side " + cab.driveSetting() + " instead of none");
                }
                // M23: full power, no intake sponge
                if (legacy && (cab.regulator() != 10 || cab.intakeSponge())) problems.add("cabinet " + p.toShortString() + ": regulator " + cab.regulator() + ", sponge " + cab.intakeSponge());
                // M29.1: the old cabinet got the air pump slot (empty) and kept its items in their slots
                if (legacy && (cab.items().getSlots() != CabinetBlockEntity.SLOTS || cab.hasAirPump() || cab.airDriven() != 0))
                    problems.add("cabinet " + p.toShortString() + ": " + cab.items().getSlots() + " slots, air pump " + cab.hasAirPump());
            } else if (be instanceof org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity xf) {
                if (legacy && (xf.regulator() != 10 || xf.intakeSponge())) problems.add("filter " + p.toShortString() + ": regulator " + xf.regulator() + ", sponge " + xf.intakeSponge());
                m23Checked++;
            } else if (s.getBlock() instanceof org.overengineer.bettacraft.equipment.TankLightBlock) {
                // M23: an old light is not dimmed (a lit one shines at 15)
                if (legacy && s.getValue(org.overengineer.bettacraft.equipment.TankLightBlock.DIM) != 0) problems.add("light " + p.toShortString() + " dimmed");
                m23Checked++;
            } else if (be instanceof FeederBlockEntity f) {
                e.addProperty("items", items(f.items()));
                e.addProperty("feedings", f.feedingsPerDay());
            }
            eq.add(e);
        }
        o.add("equipment", eq);
        return o;
    }

    private static String items(org.overengineer.bettacraft.platform.item.IItemHandler h) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (s.isEmpty()) continue;
            sb.append(i).append(':').append(BuiltInRegistries.ITEM.getKey(s.getItem())).append('x').append(s.getCount()).append(' ');
        }
        return sb.toString().trim();
    }

    /** M22.3: every animal in a bag or bucket of an older version counts as analysed. */
    private static boolean analysedContents(ItemStack s) {
        var critters = s.get(org.overengineer.bettacraft.registry.ModDataComponents.CRITTERS.get());
        if (critters != null) for (var t : critters) if (!org.overengineer.bettacraft.genetics.GeneVisibility.analysed(t)) return false;
        var group = s.get(org.overengineer.bettacraft.registry.ModDataComponents.TANKMATE_GROUP.get());
        if (group != null) for (var t : group) if (!org.overengineer.bettacraft.genetics.GeneVisibility.analysed(t.getCompoundOrEmpty(TankmateEntity.KEY))) return false;
        var bucket = s.get(net.minecraft.core.component.DataComponents.BUCKET_ENTITY_DATA);
        if (bucket != null) {
            var t = bucket.copyTag();
            if (t.contains(TankmateEntity.KEY) && !org.overengineer.bettacraft.genetics.GeneVisibility.analysed(t.getCompoundOrEmpty(TankmateEntity.KEY))) return false;
            if (t.contains(org.overengineer.bettacraft.entity.betta.BettaData.KEY)
                    && !org.overengineer.bettacraft.entity.betta.BettaData.load(t.getCompoundOrEmpty(org.overengineer.bettacraft.entity.betta.BettaData.KEY)).analysed()) return false;
        }
        var fish = s.get(org.overengineer.bettacraft.registry.ModDataComponents.FISH_DATA.get());
        return fish == null || fish.analysed();
    }

    private static JsonObject chest(ChestBlockEntity cb) {
        JsonObject o = new JsonObject();
        o.addProperty("pos", cb.getBlockPos().asLong());
        JsonArray a = new JsonArray();
        for (int i = 0; i < cb.getContainerSize(); i++) {
            ItemStack s = cb.getItem(i);
            if (s.isEmpty()) continue;
            JsonObject it = new JsonObject();
            it.addProperty("slot", i);
            it.addProperty("id", BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
            it.addProperty("count", s.getCount());
            List<String> comps = new ArrayList<>();
            // M52e (26.3): no entry view of a patch any more; its added components and removed types, sorted
            var split = s.getComponentsPatch().split();
            for (var c : split.added()) comps.add(String.valueOf(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(c.type())));
            for (var t : split.removed()) comps.add(String.valueOf(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(t)));
            java.util.Collections.sort(comps);
            // M22.3: old bags and buckets hold animals that count as analysed
            if (STAGE == 1 && legacy && !analysedContents(s)) problems.add("chest " + cb.getBlockPos().toShortString() + " slot " + i + ": animals not analysed");
            // M25 part b: an old sea water bucket stays valid and is sea water for other mods
            if (STAGE == 1 && s.is(org.overengineer.bettacraft.registry.ModItems.SEA_WATER_BUCKET.get())) {
                var fh = org.overengineer.bettacraft.platform.fluid.FluidUtil.getFluidHandler(s.copy()).orElse(null);
                var in = fh == null ? org.overengineer.bettacraft.platform.fluid.FluidStack.EMPTY : fh.drain(1000, org.overengineer.bettacraft.platform.fluid.IFluidHandler.FluidAction.SIMULATE);
                if (!in.getFluid().isSame(org.overengineer.bettacraft.registry.ModFluids.SEA_WATER.get()))
                    problems.add("chest " + cb.getBlockPos().toShortString() + " slot " + i + ": sea water bucket gives " + in.getFluid());
                else seaBuckets++;
            }
            java.util.Collections.sort(comps);
            it.addProperty("components", String.join(",", comps));
            a.add(it);
        }
        o.add("items", a);
        return o;
    }
}
