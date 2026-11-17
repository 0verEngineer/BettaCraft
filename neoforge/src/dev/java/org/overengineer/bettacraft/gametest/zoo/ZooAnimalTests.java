package org.overengineer.bettacraft.gametest.zoo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.foreign.ForeignProfiles;
import org.overengineer.bettacraft.foreign.ForeignTerrarium;
import org.overengineer.bettacraft.gametest.ZooAccess;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMoves;
import org.overengineer.bettacraft.terrarium.TerrariumBoxItem;

/**
 * M42 part b (zoo suite, {@link ZooSuite}): the animals of other mods without a profile of ours. The survey lists every mob
 * of a foreign mod with its status (profile, refusal, own care, without profile) and, for those without one, how each way in
 * decides (the mod's own bucket, our fish bag, the terrarium box, a capture item that carries the whole animal like a soul
 * vial); a refused animal must not be left in the tank. The water animals that go in live in small tanks (two each, in love)
 * and one paludarium for 15 seconds; the land animals go into terraria the way a modpack allows them
 * ({@code terrarium.boxAllow}). Then each is taken out with its container and put back, its tank packed and released like a
 * travelling tank, and the tank must not break while it is inhabited. Tables: {@code zoo/animals.tsv}, {@code zoo/water.tsv},
 * {@code zoo/land.tsv}, {@code zoo/paludarium.tsv}.
 */
@PrefixGameTestTemplate(false)
public final class ZooAnimalTests {
    /** Real ticks the animals live in the tanks before the checks (15 care steps). */
    static final int LIVE_TICKS = 300;
    /** Tanks of one test (a 10 x 10 grid of 2 x 2 x 2 tanks in the 32 x 32 template). */
    static final int SLOTS = 100;

    private ZooAnimalTests() {}

    // ------------------------------------------------------------------ the candidates

    /** A mob of another mod without a profile of ours and what is known about it. */
    public record Cand(EntityType<?> type, String id, String clazz, String flags, String category, String size, @Nullable Item bucket,
                TankInhabitants.Acceptance acceptance, boolean bag, boolean water) {
        public String mod() {
            return id.substring(0, id.indexOf(':'));
        }
    }

    /** Every Mob type of a foreign namespace and its status (the integrated ones too, for the overview). */
    public record Status(EntityType<?> type, String id, String status, @Nullable Cand cand, @Nullable String error) {}

    private static Map<EntityType<?>, Item> buckets() {
        Map<EntityType<?>, Item> out = new IdentityHashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof MobBucketItem)) continue;
            EntityType<?> t = TankInhabitants.containedType(new ItemStack(item));
            if (t != null) out.putIfAbsent(t, item);
        }
        return out;
    }

    static String flags(Entity e) {
        StringBuilder sb = new StringBuilder();
        if (e instanceof WaterAnimal) sb.append("WaterAnimal ");
        if (e instanceof AbstractFish) sb.append("Fish ");
        if (e instanceof Animal) sb.append("Animal ");
        if (e instanceof Enemy) sb.append("Enemy ");
        if (e instanceof FlyingAnimal || e instanceof net.minecraft.world.entity.FlyingMob) sb.append("Flying ");
        if (e instanceof net.minecraft.world.entity.TamableAnimal) sb.append("Tamable ");
        if (e instanceof net.minecraft.world.entity.animal.Bucketable) sb.append("Bucketable ");
        if (e instanceof LivingEntity le && le.canBreatheUnderwater()) sb.append("BreathesWater ");
        return sb.toString().trim();
    }

    /** The survey over the entity registry (sorted by id); instances are created and discarded again. */
    static List<Status> survey(ServerLevel level) {
        Map<EntityType<?>, Item> buckets = buckets();
        List<ResourceLocation> ids = new ArrayList<>(BuiltInRegistries.ENTITY_TYPE.keySet());
        ids.sort(java.util.Comparator.comparing(ResourceLocation::toString));
        List<Status> out = new ArrayList<>();
        for (ResourceLocation rl : ids) {
            if (ZooSuite.OWN.contains(rl.getNamespace())) continue;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rl);
            if (type == EntityType.PLAYER) continue;
            Entity e;
            try {
                e = type.create(level);
            } catch (Throwable t) {
                out.add(new Status(type, rl.toString(), "create failed", null, ZooSuite.brief(t)));
                continue;
            }
            if (!(e instanceof Mob mob)) {
                if (e != null) e.discard();
                continue; // projectiles, parts, vehicles, effects
            }
            try {
                String status;
                if (ForeignProfiles.ownCare(type)) status = "own care";
                else if (ForeignProfiles.refusal(type) != null) status = "refused by profile (" + ForeignProfiles.refusal(type) + ")";
                else if (ForeignProfiles.of(type) != null) status = "profile";
                else status = "WITHOUT PROFILE";
                Cand c = null;
                if (status.equals("WITHOUT PROFILE")) {
                    TankInhabitants.Acceptance a = TankInhabitants.accepts(type, mob);
                    c = new Cand(type, rl.toString(), mob.getClass().getName(), flags(mob), type.getCategory().getName(),
                            String.format(java.util.Locale.ROOT, "%.2fx%.2f", mob.getBbWidth(), mob.getBbHeight()), buckets.get(type), a,
                            TankInhabitants.bagTakes(mob), TankInhabitants.isWaterAnimal(mob));
                }
                out.add(new Status(type, rl.toString(), status, c, null));
            } catch (Throwable t) {
                out.add(new Status(type, rl.toString(), "survey failed", null, ZooSuite.brief(t) + (ZooSuite.ours(t) ? " [OURS]" : "")));
            } finally {
                mob.discard();
            }
        }
        return out;
    }

    /** The survey of this server (the registries do not change while it runs; the survey test makes its own). */
    @Nullable
    private static volatile List<Status> cached;

    public static List<Cand> without(ServerLevel level) {
        List<Status> all = cached;
        if (all == null) cached = all = survey(level);
        List<Cand> out = new ArrayList<>();
        for (Status s : all) if (s.cand() != null) out.add(s.cand());
        return out;
    }

    /** The water animals without a profile that a player can put into a tank (their bucket or our bag); the big ones need a tub. */
    public static List<Cand> waterAnimals(ServerLevel level) {
        List<Cand> out = new ArrayList<>();
        for (Cand c : without(level)) if (c.acceptance() == TankInhabitants.Acceptance.OK && (c.bag() || c.bucket() != null)) out.add(c);
        return out;
    }

    /** Land mobs without a profile, no enemies: what a modpack could allow for the terrarium box. */
    public static List<Cand> landAnimals(ServerLevel level) {
        List<Cand> out = new ArrayList<>();
        for (Cand c : without(level)) {
            if (c.water() || c.flags().contains("Enemy") || c.type().getCategory() == MobCategory.MONSTER) continue;
            out.add(c);
        }
        return out;
    }

    static String key(@Nullable Component c) {
        if (c == null) return "accepted";
        return c.getContents() instanceof TranslatableContents tc ? tc.getKey() : c.getString();
    }

    static List<Entity> ofType(ServerLevel level, TankData t, EntityType<?> type) {
        List<Entity> out = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) if (e.getType() == type) out.add(e);
        return out;
    }

    /** Every living entity of the type near the tank (inhabitant or not). */
    static List<Entity> near(ServerLevel level, TankData t, EntityType<?> type) {
        AABB box = TankInhabitants.tankBox(t).inflate(3);
        return level.getEntities((Entity) null, box, e -> e.getType() == type && e.isAlive());
    }

    // ------------------------------------------------------------------ survey and refusals

    /**
     * Every mob of a foreign mod with its status; for those without a profile how each way in decides. A refused one (bag or
     * capture item with the whole animal, its own bucket) must leave nothing in the tank.
     */
    @GameTest(templateNamespace = BettaCraft.MOD_ID, template = ZooSuite.EMPTY, batch = "bettacraft_zoo_survey", timeoutTicks = 600)
    public static void zooSurvey(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer player = ZooAccess.player(h);
        List<String> violations = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        TankBlockEntity tank = ZooAccess.waterTank(h, 2, 2, 4, 2, 4, 25F);
        TankBlockEntity terra = ZooAccess.terrarium(h, 10, 2, 4, 2, 4);
        int without = 0, refusedOk = 0;
        List<Status> all = survey(level);
        cached = all;
        for (Status s : all) {
            if (s.error() != null && s.error().contains("[OURS]")) violations.add(s.id() + ": " + s.error());
            Cand c = s.cand();
            if (c == null) {
                rows.add(ZooSuite.row(s.id().substring(0, s.id().indexOf(':')), s.id(), s.status(), "", "", "", "", "", "", "", "", "", s.error()));
                continue;
            }
            without++;
            String box = "", capture = "", bucket = "";
            try {
                Entity e = c.type().create(level);
                Vec3 at = h.absoluteVec(new Vec3(20, 2, 20));
                e.moveTo(at.x, at.y, at.z, 0, 0);
                box = key(ForeignTerrarium.boxRefusal(player, e));
                // a capture item (soul vial, imprisonment tool) or our bag with the whole animal: into the water tank and a terrarium
                for (TankBlockEntity into : List.of(tank, terra)) {
                    int before = near(level, into.getTank(), c.type()).size(), inh = into.getTank().inhabitants().size();
                    Component msg = TankInhabitants.insert(level, into, FishBagItem.fillWith(e), c.type(), null, 0);
                    capture += (into == tank ? "tank " : "terrarium ") + key(msg) + " ";
                    int after = near(level, into.getTank(), c.type()).size();
                    if (msg != null && (after != before || into.getTank().inhabitants().size() != inh)) {
                        violations.add(c.id() + " refused (" + key(msg) + ") but left in the " + (into == tank ? "tank" : "terrarium"));
                    }
                    if (msg == null) for (Entity x : ofType(level, into.getTank(), c.type())) { // went in: out again for the next one
                        into.getTank().inhabitants().remove(x.getUUID());
                        x.discard();
                    }
                    else refusedOk++;
                }
                if (c.bucket() != null) {
                    int before = near(level, tank.getTank(), c.type()).size();
                    Component msg = TankInhabitants.insert(level, tank, new ItemStack(c.bucket()), c.type(), null, TankInhabitants.BUCKET_MB);
                    bucket = key(msg);
                    if (msg != null && near(level, tank.getTank(), c.type()).size() != before) violations.add(c.id() + " refused from its bucket but left in the tank");
                    if (msg == null) for (Entity x : ofType(level, tank.getTank(), c.type())) {
                        tank.getTank().inhabitants().remove(x.getUUID());
                        x.discard();
                    }
                }
                e.discard();
            } catch (Throwable t) {
                if (ZooSuite.ours(t)) violations.add(c.id() + ": " + ZooSuite.brief(t));
                capture += " EXCEPTION " + ZooSuite.brief(t) + (ZooSuite.ours(t) ? " [OURS]" : "");
            }
            rows.add(ZooSuite.row(c.mod(), c.id(), s.status(), c.clazz(), c.flags(), c.category(), c.size(), c.acceptance(), c.bag() ? "yes" : "no",
                    c.bucket() == null ? "" : BuiltInRegistries.ITEM.getKey(c.bucket()) + " -> " + bucket, box, capture.trim(), ""));
        }
        ZooSuite.table("animals", List.of("mod", "entity", "status", "class", "flags", "category", "size", "acceptance", "fish bag", "own bucket",
                "terrarium box", "capture item / bag (tank, terrarium)", "error"), rows);
        ZooSuite.LOGGER.info("[zoo] survey: {} foreign mobs, {} without a profile, {} refusals checked", rows.size(), without, refusedOk);
        player.discard();
        ZooSuite.verdict("zoo survey", violations);
        h.succeed();
    }

    // ------------------------------------------------------------------ living in the tanks

    /** One tank of the grid with the animals put in and what happened to them. */
    static final class Slot {
        final Cand cand;
        TankBlockEntity tank;
        final List<UUID> put = new ArrayList<>();
        String in = "", out = "";
        float hp;

        Slot(Cand cand) {
            this.cand = cand;
        }
    }

    @GameTest(templateNamespace = BettaCraft.MOD_ID, template = ZooSuite.EMPTY, batch = "bettacraft_zoo_water", timeoutTicks = LIVE_TICKS + 400)
    public static void zooWaterAnimals0(GameTestHelper h) {
        live(h, 0, false);
    }

    @GameTest(templateNamespace = BettaCraft.MOD_ID, template = ZooSuite.EMPTY, batch = "bettacraft_zoo_water", timeoutTicks = LIVE_TICKS + 400)
    public static void zooWaterAnimals1(GameTestHelper h) {
        live(h, 1, false);
    }

    /** The land mobs without a profile, allowed for the box by config (as a modpack would): terraria. Own batch (config). */
    @GameTest(templateNamespace = BettaCraft.MOD_ID, template = ZooSuite.EMPTY, batch = "bettacraft_zoo_land0", timeoutTicks = LIVE_TICKS + 400)
    public static void zooLandAnimals0(GameTestHelper h) {
        live(h, 0, true);
    }

    @GameTest(templateNamespace = BettaCraft.MOD_ID, template = ZooSuite.EMPTY, batch = "bettacraft_zoo_land1", timeoutTicks = LIVE_TICKS + 400)
    public static void zooLandAnimals1(GameTestHelper h) {
        live(h, 1, true);
    }

    @GameTest(templateNamespace = BettaCraft.MOD_ID, template = ZooSuite.EMPTY, batch = "bettacraft_zoo_land2", timeoutTicks = LIVE_TICKS + 400)
    public static void zooLandAnimals2(GameTestHelper h) {
        live(h, 2, true);
    }

    private static void live(GameTestHelper h, int part, boolean land) {
        ServerLevel level = h.getLevel();
        List<Cand> all = land ? landAnimals(level) : waterAnimals(level);
        int from = part * SLOTS, to = Math.min(all.size(), from + SLOTS);
        if (from >= to) {
            ZooSuite.LOGGER.info("[zoo] {} part {}: nothing ({} animals)", land ? "land" : "water", part, all.size());
            h.succeed();
            return;
        }
        List<Cand> mine = all.subList(from, to);
        List<? extends String> allowBefore = ServerConfig.TERRA_BOX_ALLOW.get();
        Runnable restore = () -> {
            if (land) ServerConfig.TERRA_BOX_ALLOW.set(allowBefore);
        };
        try {
            if (land) ServerConfig.TERRA_BOX_ALLOW.set(mine.stream().map(Cand::id).toList());
            List<Slot> slots = new ArrayList<>();
            List<String> violations = new ArrayList<>();
            for (int i = 0; i < mine.size(); i++) {
                Slot s = new Slot(mine.get(i));
                slots.add(s);
                int x = 1 + 3 * (i % 10), z = 1 + 3 * (i / 10);
                try {
                    s.tank = land ? ZooAccess.terrarium(h, x, z, 2, 2, 2) : ZooAccess.waterTank(h, x, z, 2, 2, 2, 25F);
                    var salt = org.overengineer.bettacraft.reef.SaltTolerance.of(s.cand.type());
                    if (!land && salt != null && salt.marine()) s.tank.getTank().setSalinity(35F);
                    for (int n = 0; n < 2; n++) putIn(h, s, land, n == 0);
                    for (UUID u : s.put) if (level.getEntity(u) instanceof Animal a) a.setInLove(null);
                    for (UUID u : s.put) if (level.getEntity(u) instanceof LivingEntity le) s.hp += le.getHealth();
                } catch (Throwable t) {
                    s.in += " EXCEPTION " + ZooSuite.brief(t);
                    if (ZooSuite.ours(t)) violations.add(s.cand.id() + " putting in: " + ZooSuite.brief(t));
                }
            }
            h.runAfterDelay(LIVE_TICKS, () -> {
                try {
                    List<List<String>> rows = new ArrayList<>();
                    ServerPlayer player = ZooAccess.player(h);
                    for (Slot s : slots) rows.add(check(h, s, land, player, violations));
                    player.discard();
                    ZooSuite.table((land ? "land" : "water") + part, List.of("mod", "entity", "flags", "size", "put in", "after " + LIVE_TICKS + " ticks",
                            "take out / put back / pack / break"), rows);
                    ZooSuite.verdict("zoo " + (land ? "land" : "water") + " part " + part, violations);
                    h.succeed();
                } finally {
                    restore.run();
                }
            });
        } catch (RuntimeException ex) {
            restore.run();
            throw ex;
        }
    }

    /** Puts one animal of the slot's type in: the land ones in a terrarium box, the water ones from their bucket (the first) or a bag. */
    private static void putIn(GameTestHelper h, Slot s, boolean land, boolean first) {
        ServerLevel level = h.getLevel();
        Cand c = s.cand;
        ItemStack stack;
        String how;
        if (land) {
            Entity e = c.type().create(level);
            Vec3 at = h.absoluteVec(new Vec3(30, 2, 30));
            e.moveTo(at.x, at.y, at.z, 0, 0);
            if (!ForeignTerrarium.boxTakes(e)) {
                s.in += "box refuses (" + key(ForeignTerrarium.boxRefusal(ZooAccess.player(h), e)) + ") ";
                e.discard();
                return;
            }
            stack = TerrariumBoxItem.fillWith(e);
            e.discard();
            how = "box";
        } else if (first && c.bucket() != null) {
            stack = new ItemStack(c.bucket());
            how = "bucket";
        } else {
            Entity e = c.type().create(level);
            stack = FishBagItem.fillWith(e);
            e.discard();
            how = "bag";
        }
        List<Entity> before = ofType(level, s.tank.getTank(), c.type());
        Component msg = TankInhabitants.insert(level, s.tank, stack, c.type(), null, TankInhabitants.carriedWater(stack));
        s.in += how + ": " + key(msg) + " ";
        if (msg == null) for (Entity e : ofType(level, s.tank.getTank(), c.type())) if (!before.contains(e)) s.put.add(e.getUUID());
    }

    private static List<String> check(GameTestHelper h, Slot s, boolean land, ServerPlayer player, List<String> violations) {
        ServerLevel level = h.getLevel();
        Cand c = s.cand;
        StringBuilder after = new StringBuilder(), act = new StringBuilder();
        if (s.tank == null || s.put.isEmpty()) return ZooSuite.row(c.mod(), c.id(), c.flags(), c.size(), s.in.trim(), "-", "-");
        try {
            TankData t = s.tank.getTank();
            int alive = 0, inside = 0, wet = 0, interior = 0;
            float hp = 0;
            for (UUID u : s.put) {
                Entity e = level.getEntity(u);
                if (e == null || !e.isAlive()) continue;
                alive++;
                if (TankInhabitants.inside(t, e)) inside++;
                if (e.isInWater()) wet++;
                if (e instanceof LivingEntity le) hp += le.getHealth();
            }
            for (UUID u : s.put) {
                Entity e = org.overengineer.bettacraft.tank.interior.InteriorManager.interiorLevel(level.getServer()).getEntity(u);
                if (e != null) interior++;
            }
            Set<UUID> mine = new HashSet<>(s.put);
            long babies = near(level, t, c.type()).stream().filter(e -> !mine.contains(e.getUUID())).count();
            // anything else that appeared in or at the tank (a mod that swaps the animal for another type, eggs, drops)
            List<String> others = level.getEntities((Entity) null, TankInhabitants.tankBox(t), e -> !mine.contains(e.getUUID())
                    && e.getType() != c.type() && !(e instanceof net.minecraft.world.entity.player.Player)).stream()
                    .map(e -> e instanceof net.minecraft.world.entity.item.ItemEntity ie ? "item " + BuiltInRegistries.ITEM.getKey(ie.getItem().getItem())
                            : ZooSuite.entityId(e.getType())).distinct().toList();
            after.append("alive ").append(alive).append('/').append(s.put.size()).append(", inside ").append(inside).append(", in water ").append(wet)
                    .append(String.format(java.util.Locale.ROOT, ", hp %.1f -> %.1f", s.hp, hp)).append(", new ones near ").append(babies);
            if (!others.isEmpty()) after.append(", other entities ").append(others);
            // our care: they only count for the stocking; the tank's log says what the care noticed
            after.append(String.format(java.util.Locale.ROOT, ", stocking %.2f, water quality %.2f", TankInhabitants.stocking(level, t), t.waterQuality()));
            List<String> log = t.log().stream().map(en -> en.key().replace("log.bettacraft.", "")).distinct().toList();
            if (!log.isEmpty()) after.append(", log ").append(log);
            if (interior > 0) after.append(", IN THE INTERIOR ").append(interior);
            if (interior > 0) violations.add(c.id() + ": an animal got into the interior dimension");
            List<Entity> live = ofType(level, t, c.type());
            if (!live.isEmpty() && TankInhabitants.breakBlockedReason(level, s.tank.getBlockPos()).isEmpty()) {
                violations.add(c.id() + ": the tank could be broken with the animal in it");
                act.append("BREAKABLE ");
            }
            // take one out with its container and put it back
            if (!live.isEmpty()) {
                player.getInventory().clearContent();
                player.getInventory().setItem(0, new ItemStack(land ? ModItems.TERRARIUM_BOX.get() : ModItems.FISH_BAG.get(), 2));
                player.getInventory().setItem(1, new ItemStack(Items.BUCKET));
                Entity one = live.getFirst();
                int n = live.size();
                Component msg = TankInhabitants.takeOut(player, s.tank, one.getUUID(), true);
                act.append("take out: ").append(key(msg));
                ItemStack filled = ItemStack.EMPTY;
                for (ItemStack st : player.getInventory().items) if (TankInhabitants.containedType(st) == c.type()) filled = st;
                if (msg == null) {
                    if (filled.isEmpty()) violations.add(c.id() + ": taken out but no container holds it (lost)");
                    if (ofType(level, t, c.type()).size() != n - 1) violations.add(c.id() + ": taking out left " + ofType(level, t, c.type()).size() + " of " + n);
                    if (!filled.isEmpty()) {
                        Component back = TankInhabitants.insert(level, s.tank, filled.copy(), c.type(), player, TankInhabitants.carriedWater(filled));
                        act.append(", back: ").append(key(back));
                        if (back == null && ofType(level, t, c.type()).size() != n) violations.add(c.id() + ": put back, but " + ofType(level, t, c.type()).size() + " of " + n);
                    }
                }
                // pack and release like a travelling tank
                List<Entity> packed = new ArrayList<>();
                int before = ofType(level, t, c.type()).size();
                ListTag list = TankMoves.pack(level, t, packed);
                for (Entity e : packed) {
                    t.inhabitants().remove(e.getUUID());
                    e.discard();
                }
                int back = TankMoves.release(level, list, null);
                List<Entity> again = ofType(level, t, c.type());
                Set<UUID> ids = new HashSet<>();
                for (Entity e : again) ids.add(e.getUUID());
                act.append(", pack ").append(list.size()).append(" -> released ").append(back).append(", now ").append(again.size());
                if (back != list.size()) violations.add(c.id() + ": " + list.size() + " packed, " + back + " released");
                if (again.size() != before || ids.size() != again.size()) violations.add(c.id() + ": " + before + " before packing, " + again.size() + " after");
            }
        } catch (Throwable ex) {
            act.append(" EXCEPTION ").append(ZooSuite.brief(ex));
            if (ZooSuite.ours(ex)) violations.add(c.id() + ": " + ZooSuite.brief(ex));
        }
        return ZooSuite.row(c.mod(), c.id(), c.flags(), c.size(), s.in.trim(), after.toString(), act.toString());
    }

    // ------------------------------------------------------------------ breeding control

    /**
     * The control for "new ones near 0" of the land and water tables: the same pairs, in love, with the suppression of their own
     * breeding switched off (features.foreignAnimals.suppressOwnBreeding): which of them breed in our tanks by their own AI. Own
     * batch (the switch); the table {@code zoo/breeding.tsv} lists both. A baby with the switch on is a violation.
     */
    @GameTest(templateNamespace = BettaCraft.MOD_ID, template = ZooSuite.EMPTY, batch = "bettacraft_zoo_breeding", timeoutTicks = LIVE_TICKS * 2 + 400)
    public static void zooOwnBreedingWithAndWithoutSuppression(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        List<Cand> pairs = new ArrayList<>();
        for (Cand c : waterAnimals(level)) if (c.flags().contains("Animal")) pairs.add(c);
        // the land animals that the box takes (the tables): one of each mod first, then the rest
        List<Cand> walkers = new ArrayList<>();
        Set<String> mods = new HashSet<>();
        for (Cand c : landAnimals(level)) if (c.flags().contains("Animal") && !c.id().contains("familiar") && !c.id().contains("summon") && mods.add(c.mod())) walkers.add(c);
        for (Cand c : landAnimals(level)) if (c.flags().contains("Animal") && !c.id().contains("familiar") && !c.id().contains("summon") && !walkers.contains(c)) walkers.add(c);
        pairs.addAll(walkers);
        if (pairs.size() > 24) pairs = pairs.subList(0, 24);
        List<Cand> mine = pairs;
        List<? extends String> allowBefore = ServerConfig.TERRA_BOX_ALLOW.get();
        boolean suppressBefore = org.overengineer.bettacraft.config.Features.own(org.overengineer.bettacraft.config.Feature.SUPPRESS_FOREIGN_BREEDING);
        Runnable restore = () -> {
            ServerConfig.TERRA_BOX_ALLOW.set(allowBefore);
            org.overengineer.bettacraft.config.Features.set(org.overengineer.bettacraft.config.Feature.SUPPRESS_FOREIGN_BREEDING, suppressBefore);
        };
        try {
            ServerConfig.TERRA_BOX_ALLOW.set(mine.stream().filter(c -> !c.water()).map(Cand::id).toList()); // a listed water animal would count as a terrarium animal
            List<String> violations = new ArrayList<>();
            Map<Cand, String> on = new LinkedHashMap<>(), off = new LinkedHashMap<>();
            breedRound(h, mine, 0, true, on, violations, () -> breedRound(h, mine, 24, false, off, violations, () -> {
                try {
                    List<List<String>> rows = new ArrayList<>();
                    for (Cand c : mine) rows.add(ZooSuite.row(c.mod(), c.id(), c.flags(), on.get(c), off.get(c)));
                    ZooSuite.table("breeding", List.of("mod", "entity", "flags", "suppression on: young", "suppression off: young"), rows);
                    ZooSuite.verdict("zoo breeding", violations);
                    h.succeed();
                } finally {
                    restore.run();
                }
            }));
        } catch (RuntimeException ex) {
            restore.run();
            throw ex;
        }
    }

    /** Pairs in love in a grid of tanks (slots from {@code first}) for LIVE_TICKS, the young counted; then {@code next}. */
    private static void breedRound(GameTestHelper h, List<Cand> pairs, int first, boolean suppress, Map<Cand, String> out, List<String> violations, Runnable next) {
        ServerLevel level = h.getLevel();
        org.overengineer.bettacraft.config.Features.set(org.overengineer.bettacraft.config.Feature.SUPPRESS_FOREIGN_BREEDING, suppress);
        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < pairs.size() && i < 24; i++) {
            Cand c = pairs.get(i);
            Slot s = new Slot(c);
            slots.add(s);
            int k = first + i, x = 1 + 3 * (k % 10), z = 1 + 3 * (k / 10);
            boolean land = !c.water();
            try {
                s.tank = land ? ZooAccess.terrarium(h, x, z, 2, 2, 2) : ZooAccess.waterTank(h, x, z, 2, 2, 2, 25F);
                for (int n = 0; n < 2; n++) putIn(h, s, land, false);
                for (UUID u : s.put) if (level.getEntity(u) instanceof Animal a) {
                    a.setAge(0);
                    a.setInLove(null);
                }
            } catch (Throwable t) {
                s.in += " EXCEPTION " + ZooSuite.brief(t);
            }
        }
        h.runAfterDelay(LIVE_TICKS, () -> {
            for (Slot s : slots) {
                if (s.tank == null || s.put.size() < 2) {
                    out.put(s.cand, "not two in (" + s.in.trim() + ")");
                    continue;
                }
                Set<UUID> mine = new HashSet<>(s.put);
                List<Entity> young = near(level, s.tank.getTank(), s.cand.type()).stream().filter(e -> !mine.contains(e.getUUID())).toList();
                out.put(s.cand, String.valueOf(young.size()));
                if (suppress && !young.isEmpty()) violations.add(s.cand.id() + ": " + young.size() + " young of its own breeding with the suppression on");
                for (Entity e : young) e.discard();
            }
            next.run();
        });
    }

    // ------------------------------------------------------------------ paludarium

    /** The water animals without a profile in one sea water and one freshwater paludarium (12 x 3 x 8, a third land). */
    @GameTest(templateNamespace = BettaCraft.MOD_ID, template = ZooSuite.EMPTY, batch = "bettacraft_zoo_paludarium", timeoutTicks = LIVE_TICKS + 400)
    public static void zooPaludarium(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        List<Cand> all = waterAnimals(level);
        TankBlockEntity fresh = ZooAccess.paludarium(h, 2, 2, 12, 3, 8, 0, 0.3, Blocks.SAND.defaultBlockState());
        TankBlockEntity sea = ZooAccess.paludarium(h, 2, 14, 12, 3, 8, 35, 0.3, Blocks.SAND.defaultBlockState());
        List<String> violations = new ArrayList<>();
        Map<Cand, String> in = new LinkedHashMap<>();
        Map<Cand, UUID> who = new LinkedHashMap<>();
        for (Cand c : all) {
            var salt = org.overengineer.bettacraft.reef.SaltTolerance.of(c.type());
            TankBlockEntity into = salt != null && salt.marine() ? sea : fresh;
            try {
                Entity e = c.type().create(level);
                List<Entity> before = ofType(level, into.getTank(), c.type());
                Component msg = TankInhabitants.insert(level, into, FishBagItem.fillWith(e), c.type(), null, TankInhabitants.BAG_MB);
                e.discard();
                in.put(c, (into == sea ? "sea: " : "fresh: ") + key(msg));
                if (msg == null) for (Entity x : ofType(level, into.getTank(), c.type())) if (!before.contains(x)) who.put(c, x.getUUID());
            } catch (Throwable t) {
                in.put(c, "EXCEPTION " + ZooSuite.brief(t));
                if (ZooSuite.ours(t)) violations.add(c.id() + ": " + ZooSuite.brief(t));
            }
        }
        h.runAfterDelay(LIVE_TICKS, () -> {
            List<List<String>> rows = new ArrayList<>();
            for (Cand c : all) {
                UUID u = who.get(c);
                Entity e = u == null ? null : level.getEntity(u);
                String after = u == null ? "-" : e == null || !e.isAlive() ? "gone" : "alive, " + (TankInhabitants.inside(fresh.getTank(), e)
                        || TankInhabitants.inside(sea.getTank(), e) ? "inside" : "OUTSIDE") + (e.isInWater() ? ", in water" : ", on land/air");
                rows.add(ZooSuite.row(c.mod(), c.id(), c.flags(), c.size(), in.get(c), after));
            }
            ZooSuite.table("paludarium", List.of("mod", "entity", "flags", "size", "put in", "after " + LIVE_TICKS + " ticks"), rows);
            ZooSuite.verdict("zoo paludarium", violations);
            h.succeed();
        });
    }
}
