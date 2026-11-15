package org.overengineer.bettacraft.command;

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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.FeederBlockEntity;
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
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
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
    /** M23: external filters and lights of the old world checked (full power, no sponge, not dimmed). */
    private static int m23Checked;
    /** M24: tanks of the old world checked (nitrate starts at 0, no water change noted, no bulkhead). */
    private static int m24Checked;
    /** M25 part c: old tanks whose alkalinity and magnesium follow their salt. */
    private static int m25cChecked;
    /** M29 part c/d: old bettas that are pure Betta splendens (no ancestry, the species id unchanged). */
    private static int m29Checked;
    /** M25 part b: old sea water buckets found in the chests (they must give other mods the sea water fluid). */
    private static int seaBuckets;

    private DevSmokeUpgrade() {}

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (STAGE == 0) return;
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        ticks++;
        try {
            if (ticks == 5) {
                Path root = server.getWorldPath(LevelResource.ROOT);
                Path file = root.resolve(STAGE == 1 ? REFERENCE : SNAPSHOT);
                if (!Files.exists(file)) throw new IllegalStateException("missing " + file);
                expected = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                Set<Long> chunks = new HashSet<>();
                for (JsonElement e : expected.getAsJsonArray("tanks")) {
                    BlockPos p = BlockPos.of(e.getAsJsonObject().get("pos").getAsLong());
                    for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) chunks.add(ChunkPos.asLong((p.getX() >> 4) + dx, (p.getZ() >> 4) + dz));
                }
                for (JsonElement e : expected.getAsJsonArray("chests")) chunks.add(new ChunkPos(BlockPos.of(e.getAsJsonObject().get("pos").getAsLong())).toLong());
                for (long c : chunks) {
                    ChunkPos cp = new ChunkPos(c);
                    level.setChunkForced(cp.x, cp.z, true);
                    forced.add(cp);
                }
                LOGGER.info("[smoketest] upgrade stage {}: {} from {}, {} tanks, {} chests, {} chunks", STAGE, file.getFileName(),
                        expected.has("version") ? expected.get("version").getAsString() : "?", expected.getAsJsonArray("tanks").size(),
                        expected.getAsJsonArray("chests").size(), forced.size());
            } else if (ticks > 5 && readyAt < 0) {
                boolean ready = true;
                for (ChunkPos cp : forced) {
                    level.getChunk(cp.x, cp.z);
                    ready &= level.areEntitiesLoaded(cp.toLong());
                }
                if (!ready && ticks < 1200) return;
                readyAt = ticks;
                // a few ticks for the animals to join their tanks
            } else if (readyAt > 0 && ticks == readyAt + 40) {
                compare(server, level);
                if (STAGE == 1) { // M29: every betta of the old world is a pure Betta splendens
                    for (var e : level.getAllEntities()) {
                        if (!(e instanceof org.overengineer.bettacraft.entity.betta.BettaEntity b)) continue;
                        var g = b.getBettaData().genotype();
                        if (g.ancestry().equals(org.overengineer.bettacraft.genetics.BettaSpecies.Ancestry.SPLENDENS) && "bettacraft:betta".equals(g.species())) m29Checked++;
                        else problems.add("old betta " + b.getUUID() + " is " + g.species() + " " + g.ancestry());
                    }
                }
                if (!problems.isEmpty()) {
                    for (String p : problems) LOGGER.error("[smoketest] upgrade: {}", p);
                    throw new IllegalStateException(problems.size() + " difference(s)");
                }
                if (STAGE == 1) {
                    server.saveEverything(false, true, true);
                    write(server, level, SNAPSHOT);
                }
                for (ChunkPos cp : forced) level.setChunkForced(cp.x, cp.z, false);
                LOGGER.info("[smoketest] upgrade: {} cabinets keep the back wall (M22.4)", cabinetsChecked);
                LOGGER.info("[smoketest] upgrade: {} filters and lights run at full power, undimmed (M23)", m23Checked);
                LOGGER.info("[smoketest] upgrade: {} tanks start with nitrate 0 and no water change (M24)", m24Checked);
                LOGGER.info("[smoketest] upgrade: {} sea water buckets give the sea water fluid (M25)", seaBuckets);
                LOGGER.info("[smoketest] upgrade: {} tanks with alkalinity and magnesium of their salt (M25c)", m25cChecked);
                LOGGER.info("[smoketest] upgrade: {} old bettas are pure Betta splendens (M29)", m29Checked);
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
                if (!want.get(k).equals(have.get(k))) problems.add(where + ": " + k + " " + want.get(k) + " -> " + have.get(k));
            }
            // floating plants spread while the world runs: none may be lost, a few more are fine
            int fw = want.get("floating").getAsInt(), fh = have.get("floating").getAsInt();
            if (fh < fw || fh > fw + Math.max(3, fw / 4)) problems.add(where + ": floating plants " + fw + " -> " + fh);
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
                if (ha.has("lb_expected") && !wa.has("lb")) {
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
            if (want.has("interior") && !want.get("interior").equals(have.get("interior"))) {
                problems.add(where + ": interior " + diff(want.getAsJsonObject("interior"), have.getAsJsonObject("interior")));
            }
            if (!want.get("equipment").equals(have.get("equipment"))) problems.add(where + ": equipment " + want.get("equipment") + " -> " + have.get("equipment"));
            // M22.3: every animal of an older world counts as analysed (magnifier)
            if (have.has("unanalysed") && have.get("unanalysed").getAsInt() > 0) problems.add(where + ": " + have.get("unanalysed") + " animals not analysed");
            equipment += want.getAsJsonArray("equipment").size();
        }
        Map<Long, JsonElement> chests = new TreeMap<>();
        for (JsonElement e : now.getAsJsonArray("chests")) chests.put(e.getAsJsonObject().get("pos").getAsLong(), e.getAsJsonObject().get("items"));
        int items = 0;
        for (JsonElement e : expected.getAsJsonArray("chests")) {
            long pos = e.getAsJsonObject().get("pos").getAsLong();
            JsonElement want = e.getAsJsonObject().get("items");
            if (!want.equals(chests.get(pos))) problems.add("chest " + BlockPos.of(pos).toShortString() + ": " + want + " -> " + chests.get(pos));
            items += want.getAsJsonArray().size();
        }
        LOGGER.info("[smoketest] upgrade stage {}: compared {} tanks, {} animals, {} small animals, {} equipment blocks, {} items in chests: {} difference(s)",
                STAGE, expected.getAsJsonArray("tanks").size(), animals, critters, equipment, items, problems.size());
        LOGGER.info("[smoketest] upgrade stage {}: {} guppies and endlers converted from their old variety", STAGE, converted);
        if (STAGE == 1 && converted < 16) problems.add("only " + converted + " old guppies and endlers converted (the reference has every old variety)");
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
        s.addProperty("version", net.neoforged.fml.ModList.get().getModContainerById(BettaCraft.MOD_ID).map(m -> m.getModInfo().getVersion().toString()).orElse("?"));
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
        if (STAGE == 1) {
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
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(s.getBlock());
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
                if (cab.outletSetting() != org.overengineer.bettacraft.equipment.OutletSide.BACK) {
                    problems.add("cabinet " + p.toShortString() + ": outlet side " + cab.outletSetting() + " instead of the back wall");
                }
                cabinetsChecked++;
                // M23: full power, no intake sponge
                if (cab.regulator() != 10 || cab.intakeSponge()) problems.add("cabinet " + p.toShortString() + ": regulator " + cab.regulator() + ", sponge " + cab.intakeSponge());
                // M29.1: the old cabinet got the air pump slot (empty) and kept its items in their slots
                if (cab.items().getSlots() != CabinetBlockEntity.SLOTS || cab.hasAirPump() || cab.airDriven() != 0)
                    problems.add("cabinet " + p.toShortString() + ": " + cab.items().getSlots() + " slots, air pump " + cab.hasAirPump());
            } else if (be instanceof org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity xf) {
                if (xf.regulator() != 10 || xf.intakeSponge()) problems.add("filter " + p.toShortString() + ": regulator " + xf.regulator() + ", sponge " + xf.intakeSponge());
                m23Checked++;
            } else if (s.getBlock() instanceof org.overengineer.bettacraft.equipment.TankLightBlock) {
                // M23: an old light is not dimmed (a lit one shines at 15)
                if (s.getValue(org.overengineer.bettacraft.equipment.TankLightBlock.DIM) != 0) problems.add("light " + p.toShortString() + " dimmed");
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

    private static String items(net.neoforged.neoforge.items.IItemHandler h) {
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
        if (group != null) for (var t : group) if (!org.overengineer.bettacraft.genetics.GeneVisibility.analysed(t.getCompound(TankmateEntity.KEY))) return false;
        var bucket = s.get(net.minecraft.core.component.DataComponents.BUCKET_ENTITY_DATA);
        if (bucket != null) {
            var t = bucket.copyTag();
            if (t.contains(TankmateEntity.KEY) && !org.overengineer.bettacraft.genetics.GeneVisibility.analysed(t.getCompound(TankmateEntity.KEY))) return false;
            if (t.contains(org.overengineer.bettacraft.entity.betta.BettaData.KEY)
                    && !org.overengineer.bettacraft.entity.betta.BettaData.load(t.getCompound(org.overengineer.bettacraft.entity.betta.BettaData.KEY)).analysed()) return false;
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
            for (var c : s.getComponentsPatch().entrySet()) comps.add(String.valueOf(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(c.getKey())));
            // M22.3: old bags and buckets hold animals that count as analysed
            if (STAGE == 1 && !analysedContents(s)) problems.add("chest " + cb.getBlockPos().toShortString() + " slot " + i + ": animals not analysed");
            // M25 part b: an old sea water bucket stays valid and is sea water for other mods
            if (STAGE == 1 && s.is(org.overengineer.bettacraft.registry.ModItems.SEA_WATER_BUCKET.get())) {
                var fh = net.neoforged.neoforge.fluids.FluidUtil.getFluidHandler(s.copy()).orElse(null);
                var in = fh == null ? net.neoforged.neoforge.fluids.FluidStack.EMPTY : fh.drain(1000, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
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
