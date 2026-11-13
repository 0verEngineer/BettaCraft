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
                if (!problems.isEmpty()) {
                    for (String p : problems) LOGGER.error("[smoketest] upgrade: {}", p);
                    throw new IllegalStateException(problems.size() + " difference(s)");
                }
                if (STAGE == 1) {
                    server.saveEverything(false, true, true);
                    write(server, level, SNAPSHOT);
                }
                for (ChunkPos cp : forced) level.setChunkForced(cp.x, cp.z, false);
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
            if (!want.get("critters").equals(have.get("critters"))) problems.add(where + ": small animals " + want.get("critters") + " -> " + have.get("critters"));
            critters += want.getAsJsonArray("critters").size();
            if (want.has("interior") && !want.get("interior").equals(have.get("interior"))) {
                problems.add(where + ": interior " + diff(want.getAsJsonObject("interior"), have.getAsJsonObject("interior")));
            }
            if (!want.get("equipment").equals(have.get("equipment"))) problems.add(where + ": equipment " + want.get("equipment") + " -> " + have.get("equipment"));
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
            java.util.Collections.sort(comps);
            it.addProperty("components", String.join(",", comps));
            a.add(it);
        }
        o.add("items", a);
        return o;
    }
}
