package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.AnimalSize;
import org.overengineer.bettacraft.config.AnimalSizes;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M36 client smoke ({@code -Psmoketestm36}): the size setting of the bettas. A 6 x 2 x 2 tank in front of a wall of black and
 * white one-block stripes (the ruler) holds, side by side and frozen side-on, a male and a female halfmoon, a male plakat, the
 * tankmates neon, guppy, dwarf gourami and panda cory and, only with {@code -Pcompare} (DragN's Just Bettas in the dev run, never
 * in the repo), one of DragN's bettas. Screenshots {@code bettacraft_m36_*}: the row at Normal and at Large (switched live, the
 * fish refresh on their own), with the hitboxes, and a plain community tank at both sizes for the handbook. The hitboxes are
 * logged. The pictures with DragN's betta stay local.
 */
final class DevClientSmokeM36 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static final ResourceLocation DRAGN = ResourceLocation.fromNamespaceAndPath("dragnbettas", "betta");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private BlockPos row, community;
    private final List<Entity> dragnFish = new ArrayList<>();
    private int savedFov = 70;
    private final List<Mob> rowFish = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final Map<Mob, Vec3> pinned = new java.util.concurrent.ConcurrentHashMap<>();
    private final boolean dragn = BuiltInRegistries.ENTITY_TYPE.containsKey(DRAGN);

    DevClientSmokeM36() {
        then(1, () -> server(p -> ServerConfig.SIZE_BETTA.set(AnimalSize.NORMAL)));
        then(1, () -> server(this::build));
        then(120, () -> mc.options.hideGui = true);
        // a long lens from far away: the fish are drawn nearly without perspective in front of the ruler
        then(1, () -> {
            savedFov = mc.options.fov().get();
            mc.options.fov().set(30);
        });
        then(1, () -> look(row.getX() + 3.0, row.getY() + 0.85, row.getZ() - 7.0, 0F, 0F));
        then(40, () -> server(this::place));
        then(10, () -> shot("row_normal"));
        then(1, () -> mc.getEntityRenderDispatcher().setRenderHitBoxes(true));
        then(5, () -> shot("row_normal_hitbox"));
        then(1, () -> mc.getEntityRenderDispatcher().setRenderHitBoxes(false));
        solo("normal");
        then(1, () -> server(p -> ServerConfig.SIZE_BETTA.set(AnimalSize.LARGE)));
        then(5, () -> server(this::place)); // the same spots (the fish refreshed their hitbox on their own)
        then(10, () -> shot("row_large"));
        then(1, () -> mc.getEntityRenderDispatcher().setRenderHitBoxes(true));
        then(5, () -> shot("row_large_hitbox"));
        then(1, () -> mc.getEntityRenderDispatcher().setRenderHitBoxes(false));
        then(1, () -> server(p -> log(p, "large")));
        solo("large");
        // the background without fish (to measure the drawn fish in the pictures)
        then(1, () -> server(p -> visible(p, false)));
        then(10, () -> shot("row_empty"));
        then(1, () -> server(p -> visible(p, true)));
        then(1, () -> mc.options.fov().set(savedFov));
        // the community tank for the handbook (no foreign animals in it)
        then(1, () -> look(community.getX() + 2.0, community.getY() + 1.0, community.getZ() - 2.0, 0F, 8F));
        then(40, () -> shot("tank_large"));
        then(1, () -> server(p -> ServerConfig.SIZE_BETTA.set(AnimalSize.NORMAL)));
        then(30, () -> shot("tank_normal"));
        then(1, () -> server(p -> log(p, "normal")));
        then(1, () -> mc.options.hideGui = false);
        then(1, () -> server(this::cleanup));
        then(5, () -> LOGGER.info("[smoketest] M36 done (DragN's Just Bettas loaded: {})", dragn));
    }

    private TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, int sy, int sz) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlock(pos, ModBlocks.TANK.get().defaultBlockState(), 2);
        TankMultiblock.formAll(level, blocks);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankData t = c.getTank();
        t.setFront(Direction.NORTH);
        t.setSignVisible(false);
        t.setOwner(p.getUUID());
        t.setWaterTemp(26F);
        t.setTargetTemp(26F);
        TankInteractions.changeWater(level, c, t.capacityMb());
        c.markTankChanged();
        return c;
    }

    private static final Map<String, String> HALFMOON = Map.of("finform", "halfmoon", "fin_length", "long", "body_color", "royal_blue",
            "fin_color", "red", "pattern", "butterfly", "pattern_color", "white", "iridescence", "high");
    private static final Map<String, String> PLAKAT = Map.of("finform", "round", "fin_length", "short", "body_color", "yellow",
            "fin_color", "yellow", "pattern", "koi", "pattern_color", "orange");

    private void build(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        row = p.blockPosition().offset(-3, 1, 40);
        // the ruler: black and white stripes one block wide right behind the tank (above the water), and a floor under it
        for (int x = -1; x <= 6; x++) {
            for (int y = -1; y <= 2; y++) {
                // a plain light wall behind the fish (dark fish stand out), the stripes above the water
                level.setBlock(row.offset(x, y, 2), y < 2 || (x & 1) == 0 ? Blocks.WHITE_CONCRETE.defaultBlockState() : Blocks.BLACK_CONCRETE.defaultBlockState(), 2);
            }
            for (int z = -3; z <= 1; z++) level.setBlock(row.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 2);
        }
        TankBlockEntity c = tank(level, p, row, 6, 2, 2);
        insert(level, p, c, HALFMOON, false);
        insert(level, p, c, HALFMOON, true);
        insert(level, p, c, PLAKAT, false);
        tankmate(level, c, TankmateSpecies.NEON_TETRA, false);
        tankmate(level, c, TankmateSpecies.GUPPY, false);
        tankmate(level, c, TankmateSpecies.DWARF_GOURAMI, false);
        tankmate(level, c, TankmateSpecies.PANDA_CORY, false);
        if (dragn) {
            // DragN's halfmoon and plakat (their body types 2 and 3), adults; only through reflection, never compiled against
            for (int body : new int[] {2, 3}) {
                Entity e = BuiltInRegistries.ENTITY_TYPE.get(DRAGN).spawn(level, row.offset(3, 0, 1), MobSpawnType.SPAWN_EGG);
                if (e instanceof Mob m) m.setPersistenceRequired();
                if (e instanceof net.minecraft.world.entity.AgeableMob a) a.setAge(0);
                try {
                    e.getClass().getMethod("setBodyType", int.class).invoke(e, body);
                    e.getClass().getMethod("setGender", int.class).invoke(e, 0);
                } catch (ReflectiveOperationException | RuntimeException ex) {
                    LOGGER.warn("[smoketest] M36 DragN's body type not set: {}", ex.toString());
                }
                dragnFish.add(e);
                LOGGER.info("[smoketest] M36 DragN's betta spawned: {} body {} baby {}", e, body, e instanceof Mob m && m.isBaby());
            }
        } else {
            LOGGER.info("[smoketest] M36 without DragN's Just Bettas (-Pcompare): only our fish");
        }
        // the community tank for the handbook: two bettas, neons, a guppy, a cory
        community = row.offset(12, 0, 0);
        TankBlockEntity cc = tank(level, p, community, 4, 2, 2);
        insert(level, p, cc, HALFMOON, false);
        insert(level, p, cc, PLAKAT, true);
        for (int i = 0; i < 4; i++) tankmate(level, cc, TankmateSpecies.NEON_TETRA, i % 2 == 0);
        tankmate(level, cc, TankmateSpecies.PANDA_CORY, false);
        tankmate(level, cc, TankmateSpecies.GUPPY, false);
        p.teleportTo(level, row.getX() + 3.0, row.getY(), row.getZ() - 3.0, 0F, 0F);
    }

    private static void insert(ServerLevel level, ServerPlayer p, TankBlockEntity c, Map<String, String> genes, boolean female) {
        BettaData d = BettaData.create(BettaGenome.of(genes, BettaGenome.DEFAULT), female, level.getGameTime());
        Component err = TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d),
                ModEntities.BETTA.get(), p, false);
        if (err != null) LOGGER.error("[smoketest] M36 insert failed: {}", err.getString());
    }

    private static void tankmate(ServerLevel level, TankBlockEntity c, TankmateSpecies s, boolean female) {
        Vec3 at = Vec3.atCenterOf(c.getTank().min()).add(0.5, 0, 0.5);
        TankmateEntity e = TankmateBreeding.spawn(level, c, s, TankmateGenes.pure(s, s.varieties().getFirst()), female, LifeStage.ADULT, 0, at);
        if (e == null) LOGGER.error("[smoketest] M36 {} not spawned", s.id());
        else e.health().feed();
    }

    /** Every fish of the row at its spot, side-on (facing west), frozen; the community tank's fish spread out. */
    private void place(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        TankBlockEntity c = TankMultiblock.getController(level, row);
        if (c == null) {
            LOGGER.error("[smoketest] M36 row tank missing");
            return;
        }
        List<Mob> fish = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) if (e instanceof Mob m) fish.add(m);
        fish.removeAll(dragnFish); // our tank takes them in as foreign animals
        fish.sort(java.util.Comparator.comparingInt(m -> order(m)));
        // DragN's halfmoon next to our male halfmoon, their plakat next to ours
        if (dragnFish.size() == 2 && dragnFish.get(0) instanceof Mob hm && dragnFish.get(1) instanceof Mob pk) {
            fish.add(1, hm);
            fish.add(4, pk);
        }
        double x = row.getX() + 0.45;
        pinned.clear();
        rowFish.clear();
        rowFish.addAll(fish);
        for (Mob m : fish) {
            Vec3 at = new Vec3(x, row.getY() + 0.85, row.getZ() + 1.72);
            freeze(m, at, 90F);
            // DragN's fish do not hold still on their own in our tank: put back every tick
            if (dragnFish.contains(m)) pinned.put(m, at);
            x += 0.64;
        }
        TankBlockEntity cc = TankMultiblock.getController(level, community);
        if (cc != null) {
            int i = 0;
            double[][] spots = {{1.0, 1.1, 1.0}, {2.9, 0.7, 1.1}, {0.6, 0.5, 0.8}, {1.6, 0.4, 0.7}, {2.3, 1.3, 0.9}, {3.4, 1.0, 1.2}, {2.0, 0.15, 1.2}, {3.2, 0.5, 0.8}};
            for (Entity e : TankInhabitants.liveInhabitants(level, cc.getTank())) {
                if (!(e instanceof Mob m) || i >= spots.length) continue;
                freeze(m, new Vec3(community.getX() + spots[i][0], community.getY() + spots[i][1], community.getZ() + spots[i][2]), i % 2 == 0 ? 90F : -90F);
                i++;
            }
        }
    }

    /** Ours first (male halfmoon, female, plakat), then the tankmates in the order they were put in. */
    private static int order(Mob m) {
        if (m instanceof BettaEntity b) return b.isFemale() ? 1 : "round".equals(b.genome().shape()) ? 2 : 0;
        if (m instanceof TankmateEntity t) return 10 + t.species().ordinal();
        return 99;
    }

    private static void freeze(Mob m, Vec3 at, float yaw) {
        SmokeFreeze.freeze(m, true);
        m.moveTo(at.x, at.y, at.z, yaw, 0F);
        m.setDeltaMovement(Vec3.ZERO);
        m.setYRot(yaw);
        m.yBodyRot = yaw;
        m.yHeadRot = yaw;
        m.setYBodyRot(yaw);
        m.setYHeadRot(yaw);
    }

    private void log(ServerPlayer p, String label) {
        ServerLevel level = p.serverLevel();
        LOGGER.info("[smoketest] M36 size {} (factor {}):", AnimalSizes.betta(), AnimalSizes.bettaFactor());
        for (BlockPos at : new BlockPos[] {row, community}) {
            for (Entity e : level.getEntities((Entity) null, new net.minecraft.world.phys.AABB(at).expandTowards(6, 2, 2).inflate(0.5), e -> e instanceof Mob)) {
                String what = e instanceof BettaEntity b ? "betta " + b.genome().shape() + (b.isFemale() ? " female" : " male") + " draw " + b.sizeScale()
                        : BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
                LOGGER.info("[smoketest] M36 {} {}: hitbox {} x {}", label, what, String.format(java.util.Locale.ROOT, "%.3f", e.getBbWidth()),
                        String.format(java.util.Locale.ROOT, "%.3f", e.getBbHeight()));
            }
        }
        // the client side follows the server's setting
        mc.execute(() -> {
            if (mc.level == null) return;
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e instanceof BettaEntity b && Math.abs(b.getBbWidth() - 0.5F * b.lifeStage().scale() * AnimalSizes.bettaFactor()) > 1e-4) {
                    LOGGER.error("[smoketest] M36 client hitbox of a betta does not follow the setting: {}", b.getBbWidth());
                }
            }
        });
    }

    /** One picture per fish of the row with only that one visible (to measure it without its neighbours). */
    private void solo(String size) {
        for (int i = 0; i < 9; i++) {
            int n = i;
            then(1, () -> server(p -> {
                for (int k = 0; k < rowFish.size(); k++) rowFish.get(k).setInvisible(k != n);
            }));
            then(6, () -> {
                if (n < rowFish.size()) shot("solo_" + size + "_" + n);
            });
        }
        then(1, () -> server(p -> rowFish.forEach(m -> m.setInvisible(false))));
    }

    private void visible(ServerPlayer p, boolean on) {
        for (Entity e : p.serverLevel().getEntities((Entity) null, new net.minecraft.world.phys.AABB(row).expandTowards(6, 2, 2).inflate(0.5), e -> e instanceof Mob)) e.setInvisible(!on);
    }

    private void cleanup(ServerPlayer p) {
        pinned.clear();
        ServerConfig.SIZE_BETTA.set(AnimalSize.NORMAL);
        if (!dragn) return;
        // no foreign animals stay in the dev world
        for (Entity e : p.serverLevel().getEntities((Entity) null, new net.minecraft.world.phys.AABB(row).inflate(8),
                e -> DRAGN.equals(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType())))) e.discard();
    }

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.serverLevel(), x, eyeY - p.getEyeHeight(), z, yaw, pitch));
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    boolean tick() {
        if (mc.player != null && !steps.isEmpty()) {
            mc.player.getAbilities().flying = true;
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (!pinned.isEmpty()) server(p -> pinned.forEach((m, at) -> freeze(m, at, 90F)));
        // the client turns the bodies of frozen fish on its own (head turn, eased swimming yaw): exactly side-on for measuring
        if (mc.level != null && row != null) {
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e instanceof Mob m && e.blockPosition().closerThan(row, 24)) {
                    m.yBodyRot = m.yBodyRotO = m.yHeadRot = m.yHeadRotO = m.getYRot();
                    m.yRotO = m.getYRot();
                }
            }
        }
        if (steps.isEmpty()) return true;
        Step s = steps.peek();
        if (wait < s.delay()) {
            wait++;
            return false;
        }
        steps.poll();
        wait = 0;
        s.action().run();
        return steps.isEmpty();
    }

    private void server(Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        mc.getToasts().clear();
        if (mc.level != null && name.startsWith("solo")) {
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e instanceof Mob m && !m.isInvisible() && e.blockPosition().closerThan(row, 10)) {
                    LOGGER.info("[smoketest] M36 {}: {} yaw {} body {} at {}", name, BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()), m.getYRot(), m.yBodyRot, m.position());
                }
            }
        }
        Screenshot.grab(mc.gameDirectory, "bettacraft_m36_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
