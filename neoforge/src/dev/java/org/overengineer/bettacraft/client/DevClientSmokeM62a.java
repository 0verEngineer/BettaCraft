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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.entity.critter.WildCritterEntity;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.terrarium.HerpEntity;
import org.overengineer.bettacraft.terrarium.HerpSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M62 part 1 client smoke ({@code -Psmoketestm62a}, NeoForge and Fabric, background window; parts by
 * {@code -Pm62aparts=school,step,wall,water}, default all): the animals in the world as frame sequences.
 * <ul>
 *     <li>school: six neon tetras and four harlequin rasboras let go apart in a pond come together in two schools (from above);</li>
 *     <li>step: a leopard gecko, a slider and a corn snake climb a one-block step (no jump; from the side);</li>
 *     <li>wall: a crested gecko climbs a four-block wall and over it, another holds on two blocks up (from the side and the front);</li>
 *     <li>water: a Neocaridina and a zebra nerite climb a stone wall under water (through the glass side of the pond).</li>
 * </ul>
 * Screenshots {@code bettacraft_m62a_*.png}; log lines "[smoketest] M62a ...".
 */
final class DevClientSmokeM62a {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final Set<String> parts;
    private int wait;
    private BlockPos base;
    private final List<UUID> spawned = new ArrayList<>();
    /** Runs on the server every tick while set (drives the animals of a part). */
    @javax.annotation.Nullable
    private volatile Consumer<ServerPlayer> every;

    DevClientSmokeM62a() {
        String p = System.getProperty("bettacraft.smoketest.m62aparts", "school,step,wall,water");
        parts = Set.of(p.toLowerCase(Locale.ROOT).split(","));
        LOGGER.info("[smoketest] M62a parts {}", parts);
        then(1, () -> server(this::build));
        then(60, () -> {});
        then(1, () -> {
            mc.options.hideGui = true;
            mc.options.fov().set(50);
        });
        if (parts.contains("school")) school();
        if (parts.contains("step")) step();
        if (parts.contains("wall")) wall();
        if (parts.contains("water")) water();
        then(1, () -> mc.options.hideGui = false);
        then(5, () -> LOGGER.info("[smoketest] M62a done"));
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        org.overengineer.bettacraft.gametest.framework.DevTime.set(level, 6000);
        level.getServer().setWeatherParameters(6000, 0, false, false);
        base = p.blockPosition().offset(0, 0, 14);
        for (int x = -16; x <= 16; x++) for (int z = -6; z <= 26; z++) {
            level.setBlock(base.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            for (int y = 0; y < 8; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private void clear() {
        server(p -> {
            for (UUID id : spawned) {
                Entity e = p.level().getEntity(id);
                if (e != null) e.discard();
            }
            spawned.clear();
        });
    }

    private <T extends Entity> T add(ServerLevel level, T e, double x, double y, double z, float yaw) {
        e.snapTo(x, y, z, yaw, 0F);
        if (e instanceof net.minecraft.world.entity.Mob m) m.setPersistenceRequired();
        level.addFreshEntity(e);
        spawned.add(e.getUUID());
        return e;
    }

    // ------------------------------------------------------------------ a school in a pond

    private void school() {
        then(1, () -> server(p -> {
            ServerLevel level = p.level();
            // a pond 14 x 14, 3 deep, sand bottom
            for (int x = -14; x <= -1; x++) for (int z = 2; z <= 15; z++) {
                level.setBlock(base.offset(x, -5, z), Blocks.SAND.defaultBlockState(), 2);
                for (int y = -4; y <= -1; y++) level.setBlock(base.offset(x, y, z), Blocks.WATER.defaultBlockState(), 2);
            }
            // a glass side (north) with a dry trench in front of it: the school from the side
            for (int x = -14; x <= -1; x++) for (int y = -4; y <= -1; y++) {
                level.setBlock(base.offset(x, y, 2), Blocks.GLASS.defaultBlockState(), 2);
                level.setBlock(base.offset(x, y, 1), Blocks.AIR.defaultBlockState(), 2);
                level.setBlock(base.offset(x, y, 0), Blocks.AIR.defaultBlockState(), 2);
            }
            double[][] at = {{-12, 4}, {-3, 4}, {-12, 10}, {-3, 10}, {-8, 7}, {-10, 5}};
            for (int i = 0; i < at.length; i++) {
                TankmateEntity f = ModEntities.TANKMATES.get(TankmateSpecies.NEON_TETRA).get().create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
                f.getRandom().setSeed(6260L + i);
                add(level, f, base.getX() + at[i][0] + 0.5, base.getY() - 2.5, base.getZ() + at[i][1] + 0.5, i * 60F);
            }
            for (int i = 0; i < 4; i++) {
                TankmateEntity f = ModEntities.TANKMATES.get(TankmateSpecies.HARLEQUIN_RASBORA).get().create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
                f.getRandom().setSeed(6270L + i);
                add(level, f, base.getX() - 13.5 + i * 3, base.getY() - 3.0, base.getZ() + 7.5 - i, i * 90F);
            }
        }));
        then(5, () -> look(base.getX() - 7.5, base.getY() + 4.0, base.getZ() + 7.0, 180F, 89.9F));
        for (int f = 0; f < 8; f++) {
            int n = f;
            then(f == 0 ? 10 : 25, () -> {
                shot("school_top_f" + two(n));
                if (n % 4 == 0) logSchool();
            });
        }
        then(1, () -> look(base.getX() - 7.5, base.getY() - 1.5, base.getZ() + 0.6, 0F, 22F));
        for (int f = 0; f < 10; f++) {
            int n = f;
            then(f == 0 ? 10 : 15, () -> shot("school_side_f" + two(n)));
        }
        then(1, this::logSchool);
        then(1, this::clear);
    }

    private void logSchool() {
        server(p -> {
            StringBuilder b = new StringBuilder();
            for (UUID id : spawned) {
                Entity e = p.level().getEntity(id);
                if (e instanceof TankmateEntity t) {
                    TankmateEntity l = t.wildSchoolLeader();
                    b.append(t.getId()).append("->").append(l == null ? "-" : l.getId())
                            .append(String.format(Locale.ROOT, " %.1f; ", l == null ? 0 : t.distanceTo(l)));
                }
            }
            LOGGER.info("[smoketest] M62a school: {}", b);
        });
    }

    // ------------------------------------------------------------------ a one-block step

    private void step() {
        then(1, () -> server(p -> {
            ServerLevel level = p.level();
            for (int x = 2; x <= 10; x++) for (int z = 2; z <= 8; z++) level.setBlock(base.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 2);
            HerpSpecies[] who = {HerpSpecies.LEOPARD_GECKO, HerpSpecies.SLIDER, HerpSpecies.CORN_SNAKE};
            for (int i = 0; i < who.length; i++) {
                HerpEntity e = ModEntities.herp(who[i]).create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
                e.getRandom().setSeed(6280L + i);
                add(level, e, base.getX() + 0.5, base.getY(), base.getZ() + 3.5 + i * 2, 270F);
                e.stopGoalsForTest();
            }
        }));
        then(10, () -> server(p -> {
            for (UUID id : spawned) {
                if (p.level().getEntity(id) instanceof HerpEntity e) e.getNavigation().snapTo(base.getX() + 6.5, base.getY() + 1, e.getZ(), 1.0);
            }
        }));
        then(1, () -> look(base.getX() + 1.0, base.getY() + 1.3, base.getZ() + 0.0, 0F, 15F));
        for (int f = 0; f < 28; f++) {
            int n = f;
            then(f == 0 ? 2 : 4, () -> shot("step_side_f" + two(n)));
            then(0, () -> server(p -> {
                for (UUID id : spawned) {
                    if (p.level().getEntity(id) instanceof HerpEntity e && !e.climbingInWorld() && e.getNavigation().isDone() && e.getY() < base.getY() + 0.9) {
                        e.getNavigation().snapTo(base.getX() + 6.5, base.getY() + 1, e.getZ(), 1.0);
                    }
                }
            }));
        }
        then(1, this::clear);
    }

    // ------------------------------------------------------------------ a whole wall

    private void wall() {
        then(1, () -> server(p -> {
            ServerLevel level = p.level();
            for (int y = 0; y <= 3; y++) for (int z = 12; z <= 22; z++) level.setBlock(base.offset(12, y, z), Blocks.STONE.defaultBlockState(), 2);
            HerpEntity over = ModEntities.herp(HerpSpecies.CRESTED_GECKO).create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
            over.getRandom().setSeed(6290L);
            add(level, over, base.getX() + 10.5, base.getY(), base.getZ() + 14.5, 270F);
            over.stopGoalsForTest();
            HerpEntity part = ModEntities.herp(HerpSpecies.CRESTED_GECKO).create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
            part.getRandom().setSeed(6291L);
            add(level, part, base.getX() + 10.5, base.getY(), base.getZ() + 17.5, 270F);
            part.stopGoalsForTest();
            part.climbPart(2, 400);
        }));
        int[] count = {0};
        then(1, () -> every = p -> {
            if (++count[0] > 120) return;
            for (UUID id : spawned) {
                if (p.level().getEntity(id) instanceof HerpEntity e && !e.climbingInWorld() && e.getY() < base.getY() + 0.5) {
                    e.getMoveControl().setWantedPosition(base.getX() + 12.5, e.getY(), e.getZ(), 1.0);
                }
            }
        });
        then(1, () -> look(base.getX() + 10.2, base.getY() + 2.0, base.getZ() + 12.0, -35F, 12F));
        for (int f = 0; f < 24; f++) {
            int n = f;
            then(f == 0 ? 5 : 8, () -> shot("wall_side_f" + two(n)));
        }
        then(1, () -> every = null);
        then(1, () -> look(base.getX() + 7.5, base.getY() + 2.0, base.getZ() + 16.0, 270F, 5F));
        for (int f = 0; f < 4; f++) {
            int n = f;
            then(10, () -> shot("wall_front_f" + two(n)));
        }
        then(1, this::clear);
    }

    // ------------------------------------------------------------------ a wall under water

    private void water() {
        then(1, () -> server(p -> {
            ServerLevel level = p.level();
            // a glass basin on the meadow (water x 3..6, z 15..18, 3 deep) whose east side is a stone wall (x 7)
            for (int x = 2; x <= 7; x++) for (int z = 14; z <= 19; z++) {
                level.setBlock(base.offset(x, -1, z), Blocks.SAND.defaultBlockState(), 2);
                for (int y = 0; y <= 3; y++) {
                    boolean rim = x == 2 || z == 14 || z == 19;
                    var st = x == 7 ? Blocks.STONE.defaultBlockState() : rim ? (y <= 2 ? Blocks.GLASS.defaultBlockState() : Blocks.AIR.defaultBlockState())
                            : y <= 2 ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
                    level.setBlock(base.offset(x, y, z), st, 2);
                }
            }
            CritterSpecies[] who = {CritterSpecies.NEOCARIDINA, CritterSpecies.ZEBRA_NERITE, CritterSpecies.NEOCARIDINA};
            for (int i = 0; i < who.length; i++) {
                Critter k = new Critter(0, who[i], i == 0, CritterGenes.WILD, 0, 0, 0);
                k.age = who[i].juvenileDays() + 2;
                WildCritterEntity e = WildCritterEntity.of(level, k.saveAnimal());
                e.getRandom().setSeed(6300L + i);
                add(level, e, base.getX() + 6.6, base.getY(), base.getZ() + 15.6 + i * 1.0, 270F);
            }
        }));
        int[] wcount = {0};
        then(1, () -> every = p -> {
            if (++wcount[0] > 200) return;
            for (UUID id : spawned) {
                if (p.level().getEntity(id) instanceof WildCritterEntity e && !e.onWall()) e.crawlTo(new Vec3(base.getX() + 7.5, e.getY(), e.getZ()));
            }
        });
        then(1, () -> look(base.getX() + 4.6, base.getY() + 1.1, base.getZ() + 16.6, -90F, 18F));
        for (int f = 0; f < 16; f++) {
            int n = f;
            then(f == 0 ? 5 : 20, () -> shot("water_wall_f" + two(n)));
        }
        then(1, () -> every = null);
        then(1, this::clear);
    }

    // ------------------------------------------------------------------ helpers

    private static String two(int n) {
        return n < 10 ? "0" + n : String.valueOf(n);
    }

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.level().getServer().overworld(), x, eyeY - p.getEyeHeight(), z, java.util.Set.of(), yaw, pitch, false));
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    boolean tick() {
        Consumer<ServerPlayer> e = every;
        if (e != null && mc.getSingleplayerServer() != null) server(e);
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
        mc.getToastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m62a_" + name + ".png", mc.getMainRenderTarget(), 1, c -> {});
    }
}
