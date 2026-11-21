package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.critter.WildCritterEntity;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.terrarium.Climate;
import org.overengineer.bettacraft.terrarium.Habits;
import org.overengineer.bettacraft.terrarium.HerpEntity;
import org.overengineer.bettacraft.terrarium.HerpSpecies;
import org.overengineer.bettacraft.terrarium.LandWalker;
import org.overengineer.bettacraft.terrarium.TankClimate;
import org.overengineer.bettacraft.terrarium.TerrariumAnimalEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M54 client smoke ({@code -Psmoketestm54}, NeoForge and Fabric, background window; parts by
 * {@code -Pm54parts=world,actions,tank,critters}, default all): the movement of the animals.
 * <ul>
 *     <li>world: shrimp, snails and crayfish in a pond (head first: the apple snails crawled backwards) and the amphibians and
 *     reptiles on a meadow walking about, as frame sequences ({@code m54_world_*_fNN});</li>
 *     <li>actions: every action of the habits in the world from the side and from above (the snake raised, coiled, flicking its
 *     tongue; geckos looking round, stretching, waving the tail; a frog calling; a turtle stretching its neck);</li>
 *     <li>tank: a terrarium with a rock and a ledge (the snake and the geckos climb and hide), a paludarium with leaves (dart
 *     frogs), as frame sequences, and the actions in the miniature;</li>
 *     <li>critters: apple snails, nerites and shrimp in a tank from above as a frame sequence (head first).</li>
 * </ul>
 * Screenshots {@code bettacraft_m54_*.png}; log lines "[smoketest] M54 ...".
 */
final class DevClientSmokeM54 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final Set<String> parts;
    private int wait;
    private BlockPos base;
    private final List<UUID> herps = new ArrayList<>();
    private final List<UUID> wild = new ArrayList<>();
    private TankBlockEntity terrarium, paludarium, aquarium;

    DevClientSmokeM54() {
        String p = System.getProperty("bettacraft.smoketest.m54parts", "world,actions,tank,critters");
        parts = Set.of(p.toLowerCase(Locale.ROOT).split(","));
        LOGGER.info("[smoketest] M54 parts {}", parts);
        then(1, () -> server(this::build));
        then(60, () -> {});
        then(1, () -> {
            mc.options.hideGui = true;
            mc.options.fov().set(50);
        });
        if (parts.contains("critters")) critters();
        if (parts.contains("tank")) tank();
        if (parts.contains("world")) world();
        if (parts.contains("actions")) actions();
        then(1, () -> mc.options.hideGui = false);
        then(5, () -> LOGGER.info("[smoketest] M54 done"));
    }

    // ------------------------------------------------------------------ set-up

    private void build(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        base = p.blockPosition().offset(0, 0, 14);
        // a meadow 24 x 24 with a pond (sand under 1 block of water) and a roof on posts at its side
        for (int x = -12; x <= 12; x++) for (int z = -4; z <= 20; z++) {
            level.setBlock(base.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            for (int y = 0; y < 6; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
        for (int x = -10; x <= -6; x++) for (int z = 12; z <= 16; z++) {
            level.setBlock(base.offset(x, -2, z), Blocks.SAND.defaultBlockState(), 2);
            level.setBlock(base.offset(x, -1, z), Blocks.WATER.defaultBlockState(), 2);
        }
        for (int x = 7; x <= 10; x++) for (int z = 14; z <= 17; z++) level.setBlock(base.offset(x, 2, z), Blocks.OAK_PLANKS.defaultBlockState(), 2);
        // the tanks: a terrarium 4 x 3 x 3, a paludarium 3 x 3 x 3, an aquarium 3 x 2 x 3
        terrarium = tank(level, base.offset(-6, 0, 0), 4, 3, 3);
        paludarium = tank(level, base.offset(0, 0, 0), 3, 3, 3);
        aquarium = tank(level, base.offset(5, 0, 0), 3, 2, 3);
        LOGGER.info("[smoketest] M54 tanks at {}, {}, {}", terrarium.getBlockPos(), paludarium.getBlockPos(), aquarium.getBlockPos());
    }

    private static TankBlockEntity tank(ServerLevel level, BlockPos at, int sx, int sy, int sz) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int x = 0; x < sx; x++) for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) blocks.add(at.offset(x, y, z));
        for (BlockPos q : blocks) level.setBlock(q, ModBlocks.TANK.get().defaultBlockState(), 2);
        TankData t = TankMultiblock.formAll(level, blocks);
        TankBlockEntity c = TankMultiblock.getController(level, at);
        t.setFront(Direction.NORTH);
        return c;
    }

    private static void fill(ServerLevel in, InteriorRegion r, int x0, int x1, int y0, int y1, int z0, int z1, BlockState s) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) in.setBlock(r.toWorld(x, y, z), s, 2);
    }

    private static void floor(ServerLevel in, InteriorRegion r, BlockState s) {
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) {
            Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), s, false);
        }
    }

    private static HerpEntity put(ServerLevel level, TankBlockEntity c, HerpSpecies s, int variety) {
        HerpEntity e = ModEntities.herp(s).create(level);
        e.setVariety(variety);
        var stack = e.getBucketItemStack();
        e.saveToBucketTag(stack);
        e.discard();
        List<Entity> before = TankInhabitants.liveInhabitants(level, c.getTank());
        var err = TankInhabitants.insert(level, c, stack, ModEntities.herp(s), null, 0L);
        if (err != null) LOGGER.info("[smoketest] M54 {} not in the tank: {}", s.id(), err.getString());
        for (Entity n : TankInhabitants.liveInhabitants(level, c.getTank())) if (n instanceof HerpEntity h && !before.contains(n)) return h;
        return null;
    }

    // ------------------------------------------------------------------ the small animals in a tank (from above)

    private void critters() {
        then(1, () -> look(aquarium.getBlockPos().getX() + 1.5, aquarium.getBlockPos().getY() + 3.2, aquarium.getBlockPos().getZ() + 1.5, 180F, 89.9F));
        then(40, () -> server(p -> {
            ServerLevel level = p.serverLevel();
            TankData t = aquarium.getTank();
            TankInteractions.changeWater(level, aquarium, t.capacityMb());
            t.setWaterTemp(25F);
            t.critters().clear();
            CritterSpecies[] kinds = {CritterSpecies.APPLE_SNAIL, CritterSpecies.APPLE_SNAIL, CritterSpecies.ZEBRA_NERITE, CritterSpecies.NEOCARIDINA,
                    CritterSpecies.DWARF_CRAYFISH, CritterSpecies.TYLOMELANIA, CritterSpecies.APPLE_SNAIL, CritterSpecies.AMANO_SHRIMP};
            for (int i = 0; i < kinds.length; i++) {
                CritterSpecies s = kinds[i];
                Critter k = new Critter(t.nextCritterId(), s, s.hasSexes(), CritterGenes.WILD, t.min().getX() + 0.5 + (i % 4) * 0.6,
                        t.min().getY() + 0.05, t.min().getZ() + 0.6 + (i / 4) * 1.6);
                k.age = s.juvenileDays() + 2;
                t.critters().add(k);
            }
            aquarium.markTankChanged();
            TankCritters.sync(level, aquarium);
        }));
        then(40, () -> server(p -> TankCritters.sync(p.serverLevel(), aquarium)));
        for (int f = 0; f < 10; f++) {
            int n = f;
            then(f == 0 ? 20 : 20, () -> shot("tank_critters_f" + two(n)));
        }
    }

    // ------------------------------------------------------------------ terrarium and paludarium

    private void tank() {
        then(1, () -> server(p -> {
            ServerLevel level = p.serverLevel();
            ServerLevel in = InteriorManager.interiorLevel(level.getServer());
            // the terrarium: sand, a rock 16 x 16 x 10 (a perch), a ledge over the floor (a hide), warm and dry
            InteriorRegion r = InteriorManager.ensureRegion(level, terrarium);
            floor(in, r, Blocks.SAND.defaultBlockState());
            fill(in, r, 36, 51, 0, 9, 14, 29, Blocks.STONE.defaultBlockState());
            fill(in, r, 0, 19, 7, 8, 18, 35, Blocks.OAK_PLANKS.defaultBlockState());
            fill(in, r, 0, 2, 0, 6, 18, 35, Blocks.STONE.defaultBlockState());
            TankClimate.step(level, terrarium, 0.01);
            terrarium.getTank().setClimate(new Climate.State(26, 30, 34, 60, 3, 0));
            // the paludarium: water, land on the far half with moss and a shrub of leaves
            InteriorRegion q = InteriorManager.ensureRegion(level, paludarium);
            floor(in, q, Blocks.SAND.defaultBlockState());
            fill(in, q, 0, q.sizeX() - 1, 0, 22, 24, q.sizeZ() - 1, Blocks.DIRT.defaultBlockState());
            fill(in, q, 0, q.sizeX() - 1, 23, 23, 24, q.sizeZ() - 1, Blocks.MOSS_BLOCK.defaultBlockState());
            fill(in, q, 14, 30, 24, 29, 32, 44, Blocks.OAK_LEAVES.defaultBlockState());
            TankInteractions.changeWater(level, paludarium, paludarium.getTank().capacityMb() / 3);
            TankClimate.step(level, paludarium, 0.01);
            paludarium.getTank().setClimate(new Climate.State(25, 25, 25, 90, 0, 0));
            paludarium.getTank().setWaterTemp(25F);
            for (HerpSpecies s : List.of(HerpSpecies.CORN_SNAKE, HerpSpecies.LEOPARD_GECKO, HerpSpecies.CRESTED_GECKO, HerpSpecies.HORNED_FROG)) {
                HerpEntity e = put(level, terrarium, s, 0);
                if (e != null) herps.add(e.getUUID());
            }
            for (int i = 0; i < 3; i++) {
                HerpEntity e = put(level, paludarium, HerpSpecies.DART_FROG, i);
                if (e != null) herps.add(e.getUUID());
            }
            HerpEntity musk = put(level, paludarium, HerpSpecies.MUSK, 0);
            if (musk != null) herps.add(musk.getUUID());
            LOGGER.info("[smoketest] M54 tank animals: {}", herps.size());
        }));
        // the terrarium from the front, then the paludarium: frame sequences
        then(60, () -> look(terrarium.getBlockPos().getX() + 2.0, terrarium.getBlockPos().getY() + 1.6, terrarium.getBlockPos().getZ() - 3.2, 0F, 18F));
        for (int f = 0; f < 12; f++) {
            int n = f;
            then(f == 0 ? 40 : 15, () -> {
                shot("tank_terrarium_f" + two(n));
                if (n % 4 == 0) logHerps("terrarium");
            });
        }
        then(1, () -> look(paludarium.getBlockPos().getX() + 1.5, paludarium.getBlockPos().getY() + 1.8, paludarium.getBlockPos().getZ() - 2.6, 0F, 22F));
        for (int f = 0; f < 10; f++) {
            int n = f;
            then(f == 0 ? 30 : 15, () -> {
                shot("tank_paludarium_f" + two(n));
                if (n % 5 == 0) logHerps("paludarium");
            });
        }
        // the actions in the miniature: the snake raised, then coiled; the leopard gecko stretching; the crested gecko looking
        then(1, () -> clientHerps(e -> {
            if (e instanceof HerpEntity h && h.species() == HerpSpecies.CORN_SNAKE) h.setActionForTest(Habits.Action.HEAD_UP);
            if (e instanceof HerpEntity h && h.species() == HerpSpecies.LEOPARD_GECKO) h.setActionForTest(Habits.Action.STRETCH);
            if (e instanceof HerpEntity h && h.species() == HerpSpecies.CRESTED_GECKO) h.setActionForTest(Habits.Action.LOOK);
            if (e instanceof HerpEntity h && h.species() == HerpSpecies.DART_FROG) h.setActionForTest(Habits.Action.CALL);
        }));
        then(1, () -> server(p -> freezeHerps(p, true)));
        then(1, () -> look(terrarium.getBlockPos().getX() + 2.0, terrarium.getBlockPos().getY() + 1.3, terrarium.getBlockPos().getZ() - 2.0, 0F, 22F));
        then(30, () -> shot("tank_actions_raised"));
        then(1, () -> clientHerps(e -> {
            if (e instanceof HerpEntity h && h.species() == HerpSpecies.CORN_SNAKE) h.setActionForTest(Habits.Action.COIL);
        }));
        then(30, () -> shot("tank_actions_coiled"));
        then(1, () -> look(paludarium.getBlockPos().getX() + 1.5, paludarium.getBlockPos().getY() + 1.7, paludarium.getBlockPos().getZ() - 1.6, 0F, 25F));
        then(20, () -> shot("tank_actions_frogs"));
        then(1, () -> clientHerps(e -> {
            if (e instanceof TerrariumAnimalEntity t) t.setActionForTest(null);
        }));
        then(1, () -> server(p -> freezeHerps(p, false)));
    }

    /** Logs where the animals of the tanks are and what they do. */
    private void logHerps(String where) {
        server(p -> {
            ServerLevel level = p.serverLevel();
            StringBuilder b = new StringBuilder();
            for (UUID id : herps) {
                if (!(level.getEntity(id) instanceof HerpEntity e)) continue;
                b.append(e.species().id()).append(' ').append(e.walker().activity()).append(e.walker().settled() ? "(settled)" : "")
                        .append(' ').append(e.action()).append(String.format(Locale.ROOT, " y%.2f; ", e.getY() - e.blockPosition().getY()));
            }
            LOGGER.info("[smoketest] M54 {}: {}", where, b);
        });
    }

    private void freezeHerps(ServerPlayer p, boolean on) {
        for (UUID id : herps) if (p.serverLevel().getEntity(id) instanceof HerpEntity e) e.setNoAi(on);
    }

    private void clientHerps(Consumer<Entity> action) {
        if (mc.level == null) return;
        for (Entity e : mc.level.entitiesForRendering()) if (e instanceof TerrariumAnimalEntity) action.accept(e);
    }

    // ------------------------------------------------------------------ the world

    private void world() {
        then(1, () -> server(p -> {
            ServerLevel level = p.serverLevel();
            // the small animals in the pond
            CritterSpecies[] kinds = {CritterSpecies.APPLE_SNAIL, CritterSpecies.APPLE_SNAIL, CritterSpecies.APPLE_SNAIL, CritterSpecies.ZEBRA_NERITE,
                    CritterSpecies.NEOCARIDINA, CritterSpecies.DWARF_CRAYFISH};
            for (int i = 0; i < kinds.length; i++) {
                CritterSpecies s = kinds[i];
                WildCritterEntity w = ModEntities.WILD_CRITTER.get().create(level);
                Critter k = new Critter(0, s, s.hasSexes(), CritterGenes.WILD, 0, 0, 0);
                k.age = s.juvenileDays() + 2;
                w.setAnimal(k);
                w.moveTo(base.getX() - 9.5 + (i % 3) * 1.4, base.getY() - 1.0, base.getZ() + 13.5 + (i / 3) * 1.6, i * 60F, 0F);
                w.setPersistenceRequired();
                level.addFreshEntity(w);
                wild.add(w.getUUID());
            }
            // the amphibians and reptiles on the meadow
            HerpSpecies[] all = {HerpSpecies.CORN_SNAKE, HerpSpecies.LEOPARD_GECKO, HerpSpecies.CRESTED_GECKO, HerpSpecies.DART_FROG, HerpSpecies.HORNED_FROG,
                    HerpSpecies.SLIDER, HerpSpecies.HERMANN, HerpSpecies.MUDSKIPPER};
            for (int i = 0; i < all.length; i++) {
                HerpEntity e = ModEntities.herp(all[i]).create(level);
                e.moveTo(base.getX() - 3.5 + (i % 4) * 2.5, base.getY(), base.getZ() + 8.5 + (i / 4) * 3.0, i * 45F, 0F);
                e.setPersistenceRequired();
                level.addFreshEntity(e);
                wild.add(e.getUUID());
            }
        }));
        // the pond from above: the snails must crawl head first
        then(30, () -> look(base.getX() - 7.5, base.getY() + 3.0, base.getZ() + 14.5, 180F, 89.9F));
        for (int f = 0; f < 10; f++) {
            int n = f;
            then(20, () -> {
                shot("world_pond_f" + two(n));
                if (n % 3 == 0) logWild();
            });
        }
        // the meadow from above and from the side
        then(1, () -> look(base.getX() + 0.5, base.getY() + 7.0, base.getZ() + 10.5, 180F, 89.9F));
        for (int f = 0; f < 10; f++) {
            int n = f;
            then(20, () -> shot("world_meadow_top_f" + two(n)));
        }
        then(1, () -> look(base.getX() + 0.5, base.getY() + 2.5, base.getZ() + 2.0, 0F, 25F));
        for (int f = 0; f < 8; f++) {
            int n = f;
            then(15, () -> shot("world_meadow_side_f" + two(n)));
        }
        then(1, this::logWild);
    }

    private void logWild() {
        server(p -> {
            ServerLevel level = p.serverLevel();
            StringBuilder b = new StringBuilder();
            for (UUID id : wild) {
                Entity e = level.getEntity(id);
                if (e == null) continue;
                Vec3 v = e.getDeltaMovement();
                String name = e instanceof HerpEntity h ? h.species().id() : e instanceof WildCritterEntity w ? w.species().name().toLowerCase(Locale.ROOT) : "?";
                b.append(name).append(String.format(Locale.ROOT, " yaw %.0f move %.0f", e.getYRot(),
                        Math.hypot(v.x, v.z) > 1e-3 ? Math.atan2(v.z, v.x) * 180 / Math.PI - 90 : Double.NaN));
                if (e instanceof TerrariumAnimalEntity t) b.append(' ').append(t.action()).append(t.restingInWorld() ? " resting" : "").append(t.isBasking() ? " basking" : "");
                b.append("; ");
            }
            LOGGER.info("[smoketest] M54 world: {}", b);
        });
    }

    // ------------------------------------------------------------------ every action in the world

    private void actions() {
        record Show(HerpSpecies species, Habits.Action action, String name) {}
        List<Show> shows = List.of(new Show(HerpSpecies.CORN_SNAKE, Habits.Action.HEAD_UP, "snake_head_up"),
                new Show(HerpSpecies.CORN_SNAKE, Habits.Action.COIL, "snake_coil"), new Show(HerpSpecies.CORN_SNAKE, Habits.Action.TONGUE, "snake_tongue"),
                new Show(HerpSpecies.LEOPARD_GECKO, Habits.Action.LOOK, "gecko_look"), new Show(HerpSpecies.LEOPARD_GECKO, Habits.Action.STRETCH, "gecko_stretch"),
                new Show(HerpSpecies.CRESTED_GECKO, Habits.Action.TAIL, "crested_tail"), new Show(HerpSpecies.DART_FROG, Habits.Action.CALL, "frog_call"),
                new Show(HerpSpecies.SLIDER, Habits.Action.STRETCH, "slider_stretch"), new Show(HerpSpecies.HERMANN, Habits.Action.LOOK, "tortoise_look"));
        List<UUID> shown = new ArrayList<>();
        then(1, () -> server(p -> {
            ServerLevel level = p.serverLevel();
            for (UUID id : wild) {
                Entity e = level.getEntity(id);
                if (e != null) e.discard();
            }
            for (int i = 0; i < shows.size(); i++) {
                HerpEntity e = ModEntities.herp(shows.get(i).species()).create(level);
                e.moveTo(base.getX() - 8.0 + i * 2.0, base.getY(), base.getZ() + 4.5, 270F, 0F);
                e.setYHeadRot(270F);
                e.setYBodyRot(270F);
                e.setNoAi(true);
                e.setPersistenceRequired();
                level.addFreshEntity(e);
                shown.add(e.getUUID());
            }
        }));
        then(20, () -> {
            for (int i = 0; i < shown.size(); i++) {
                Entity e = mc.level == null ? null : findClient(shown.get(i));
                if (e instanceof TerrariumAnimalEntity t) t.setActionForTest(shows.get(i).action());
            }
        });
        for (int i = 0; i < shows.size(); i++) {
            int n = i;
            then(1, () -> look(base.getX() - 8.0 + n * 2.0, base.getY() + 0.45, base.getZ() + 4.5 - 1.3, 0F, 12F));
            then(i == 0 ? 25 : 8, () -> shot("actions_" + shows.get(n).name() + "_side"));
            then(1, () -> look(base.getX() - 8.0 + n * 2.0, base.getY() + 1.5, base.getZ() + 4.51, 180F, 89.9F));
            then(8, () -> shot("actions_" + shows.get(n).name() + "_top"));
        }
        then(1, () -> server(p -> {
            for (UUID id : shown) {
                Entity e = p.serverLevel().getEntity(id);
                if (e != null) e.discard();
            }
        }));
    }

    @javax.annotation.Nullable
    private Entity findClient(UUID id) {
        for (Entity e : mc.level.entitiesForRendering()) if (e.getUUID().equals(id)) return e;
        return null;
    }

    // ------------------------------------------------------------------ helpers

    private static String two(int n) {
        return n < 10 ? "0" + n : String.valueOf(n);
    }

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.server.overworld(), x, eyeY - p.getEyeHeight(), z, yaw, pitch));
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    boolean tick() {
        if (mc.player != null && !steps.isEmpty()) {
            mc.player.getAbilities().flying = true;
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
        if (steps.isEmpty()) return true;
        Step st = steps.peek();
        if (wait < st.delay()) {
            wait++;
            return false;
        }
        steps.poll();
        wait = 0;
        st.action().run();
        return steps.isEmpty();
    }

    private void server(Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        mc.getToasts().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m54_" + name + ".png", mc.getMainRenderTarget(), c -> {});
    }
}
