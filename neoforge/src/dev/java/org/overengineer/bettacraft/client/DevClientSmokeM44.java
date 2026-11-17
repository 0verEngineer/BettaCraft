package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.big.TransportTubItem;
import org.overengineer.bettacraft.config.ClientConfig;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.CabinetFrontSetting;
import org.overengineer.bettacraft.foreign.ForeignProfiles;
import org.overengineer.bettacraft.foreign.ForeignTerrarium;
import org.overengineer.bettacraft.mobs.MobJarItem;
import org.overengineer.bettacraft.registry.ModAttachments;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.terrarium.HerpSpecies;
import org.overengineer.bettacraft.terrarium.LandWalker;
import org.overengineer.bettacraft.terrarium.TerrariumAnimalEntity;
import org.overengineer.bettacraft.terrarium.TerrariumBoxItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M44 ({@code -Psmoketestm44}; background window). Part 1: mobs in glass, Minecraft pets and our own terrarium animals walking in
 * terraria next to the same mobs walking outside, once with {@code walkAnimationBoost = 0} (vanilla) and once with "auto"; every
 * tick the client's walk animation is logged ({@code [m44]} lines), the means are checked (auto: the tank mobs at least 0.2 on
 * average, our geckos above 0, the chicken on the ground) and frame sequences are taken. Part 2: cabinet rows of 1 to 12 blocks
 * with their automatic fronts, the menu settings, a row without joints, the rows with the feature off, and the cabinet menu.
 */
final class DevClientSmokeM44 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private record Tracked(String label, int id) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final List<Tracked> tracked = new CopyOnWriteArrayList<>();
    private final List<Mob> walkersOutside = new CopyOnWriteArrayList<>();
    private int wait;
    private BlockPos glass, pets, herps, big, pen;
    private int logTicks = -1;
    private String condition = "?";
    private int tick;
    /** condition|label -> {sum of the swing, ticks, ticks on the ground}. */
    private final Map<String, double[]> stats = new HashMap<>();
    private boolean failed;

    DevClientSmokeM44() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(200, () -> server(pl -> place(pl.serverLevel())));
        then(40, () -> server(pl -> force(pl.serverLevel())));
        for (String cond : new String[]{"0", "auto"}) {
            then(1, () -> {
                ClientConfig.WALK_ANIMATION_BOOST.set(cond);
                condition = cond.equals("0") ? "vanilla" : cond;
            });
            then(1, () -> look(glass.getX() + 4, glass.getY() + 0.75, glass.getZ() - 1.1, 0F, 22F));
            then(60, () -> logTicks = 400);
            frames("glass", 6, 3);
            then(1, () -> look(pets.getX() + 5, pets.getY() + 1.6, pets.getZ() - 2.4, 0F, 22F));
            then(40, () -> {});
            frames("pets", 6, 3);
            then(1, () -> look(herps.getX() + 3, herps.getY() + 0.9, herps.getZ() - 1.2, 0F, 22F));
            then(40, () -> {});
            frames("herps", 6, 3);
            then(1, () -> look(big.getX() + 12, big.getY() + 3.5, big.getZ() - 4.0, 0F, 22F));
            then(40, () -> {});
            frames("big", 6, 3);
            then(1, () -> look(pen.getX() + 4, pen.getY() + 2.6, pen.getZ() - 6.0, 0F, 18F));
            then(40, () -> {});
            frames("outside", 6, 3);
            then(1, () -> look(glass.getX() + 4, glass.getY() + 0.75, glass.getZ() - 1.1, 0F, 22F));
            then(Math.max(1, 400 - 320), () -> {}); // the rest of the 400 measured ticks
            then(1, () -> logTicks = 0);
        }
        then(1, this::evaluate);
        // part 2: the cabinets
        then(1, () -> server(this::cabinets));
        then(160, () -> {});
        then(1, () -> look(cab.getX() + 8, cab.getY() + 1.9, cab.getZ() - 9.0, 0F, 9F));
        then(60, () -> shot("cab_rows_1to4"));
        then(1, () -> look(cab.getX() + 24.5, cab.getY() + 1.9, cab.getZ() - 8.0, 0F, 10F));
        then(40, () -> shot("cab_rows_5_6"));
        then(1, () -> look(cab.getX() + 48.5, cab.getY() + 1.9, cab.getZ() - 6.5, 0F, 12F));
        then(40, () -> shot("cab_rows_7_8"));
        then(1, () -> look(cab.getX() + 66, cab.getY() + 1.9, cab.getZ() - 7.0, 0F, 12F));
        then(40, () -> shot("cab_row_12"));
        then(1, () -> look(cab.getX() + 63, cab.getY() + 2.4, cab.getZ() - 3.4, 30F, 20F));
        then(40, () -> shot("cab_angle"));
        then(1, () -> look(cab.getX() + 78.5, cab.getY() + 1.6, cab.getZ() - 4.0, 0F, 12F));
        then(40, () -> shot("cab_settings"));
        then(1, () -> look(cab.getX() + 88, cab.getY() + 1.7, cab.getZ() - 5.2, 0F, 12F));
        then(40, () -> shot("cab_nojoints"));
        then(1, () -> look(cab.getX() + 88, cab.getY() + 1.2, cab.getZ() - 1.6, 0F, 8F));
        then(40, () -> shot("cab_joints_close"));
        // the switch off: every cabinet with its old two doors
        then(1, () -> server(p -> fronts(p.serverLevel(), false)));
        then(1, () -> look(cab.getX() + 24.5, cab.getY() + 1.9, cab.getZ() - 8.0, 0F, 10F));
        then(60, () -> shot("cab_off"));
        then(1, () -> server(p -> fronts(p.serverLevel(), true)));
        then(40, () -> {});
        // the menu of a cabinet in a row (auto) and of the one with the single door
        then(1, () -> mc.options.hideGui = false);
        then(1, () -> look(cab.getX() + 66, cab.getY() + 1.9, cab.getZ() - 3.0, 0F, 12F));
        then(10, () -> server(p -> open(p, cab.offset(60 + 5, 0, 0))));
        then(40, () -> shot("menu_auto"));
        then(1, () -> server(p -> open(p, settingsAt(3))));
        then(40, () -> shot("menu_single"));
        then(1, () -> {
            if (mc.player != null) mc.player.closeContainer();
        });
        then(10, () -> ClientConfig.WALK_ANIMATION_BOOST.set("auto"));
    }

    // ------------------------------------------------------------------ the cabinets

    /** The rows of group 1 (1 to 6 blocks), group 2 (7, 8), group 3 (12); x offsets from {@link #cab}. */
    private static final int[][] ROWS = {{1, 0}, {2, 3}, {3, 7}, {4, 12}, {5, 18}, {6, 25}, {7, 40}, {8, 49}, {12, 60}};
    /** The settings row (one block each, from the viewer's left) and the row of 8 without joints. */
    private static final CabinetFrontSetting[] SETTINGS = {CabinetFrontSetting.PAIR_L, CabinetFrontSetting.PAIR_R, CabinetFrontSetting.DRAWERS,
            CabinetFrontSetting.SINGLE, CabinetFrontSetting.TWO};
    private static final int SETTINGS_X = 76, NOJOINTS_X = 84;
    private BlockPos cab;
    private final List<BlockPos> cabinets = new ArrayList<>();

    /** Block {@code i} (from the viewer's left) of the settings row. */
    private BlockPos settingsAt(int i) {
        return cab.offset(SETTINGS_X + SETTINGS.length - 1 - i, 0, 0);
    }

    private void row(ServerLevel level, int x0, int n, int depth) {
        BlockPos start = cab.offset(x0, 0, 0);
        for (int d = 0; d < depth; d++) for (int i = 0; i < n; i++) {
            BlockPos pos = start.offset(i, 0, d);
            level.setBlock(pos, ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH), 3);
            cabinets.add(pos);
        }
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 1; y <= 2; y++) for (int z = 0; z < depth; z++) for (int x = 0; x < n; x++) blocks.add(start.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlock(pos, ModBlocks.TANK.get().defaultBlockState(), 2);
        TankMultiblock.formAll(level, blocks);
        TankBlockEntity c = TankMultiblock.getController(level, start.above());
        if (c != null) {
            c.getTank().setFront(Direction.NORTH);
            c.getTank().setSignVisible(false);
            org.overengineer.bettacraft.tank.TankInteractions.changeWater(level, c, c.getTank().capacityMb());
            c.markTankChanged();
        }
    }

    private void cabinets(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        cab = glass.offset(0, 0, 40);
        for (int x = -6; x < 100; x++) for (int z = -16; z < 8; z++) {
            level.setBlock(cab.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 2);
            for (int y = 0; y < 6; y++) if (!level.getBlockState(cab.offset(x, y, z)).isAir()) level.setBlock(cab.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
        for (int[] r : ROWS) row(level, r[1], r[0], 2);
        row(level, SETTINGS_X, SETTINGS.length, 1);
        for (int i = 0; i < SETTINGS.length; i++) {
            if (level.getBlockEntity(settingsAt(i)) instanceof CabinetBlockEntity be) be.setFrontSetting(SETTINGS[i]);
        }
        row(level, NOJOINTS_X, 8, 1);
        for (int i = 0; i < 8; i++) if (level.getBlockEntity(cab.offset(NOJOINTS_X + i, 0, 0)) instanceof CabinetBlockEntity be) be.setJoints(false);
        // the log: every row's fronts from the viewer's left
        for (int[] r : ROWS) LOGGER.info("[m44] row {}: {}", r[0], fronts(level, r[1], r[0]));
        LOGGER.info("[m44] settings row: {} | no joints: {}", fronts(level, SETTINGS_X, SETTINGS.length), fronts(level, NOJOINTS_X, 8));
        String twelve = fronts(level, 60, 12);
        if (!"DLRLRLRLRLRD".equals(twelve)) fail("row of 12: " + twelve);
        LOGGER.info("[m44] cabinets at {}", cab);
    }

    private String fronts(ServerLevel level, int x0, int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) {
            BlockState s = level.getBlockState(cab.offset(x0 + n - 1 - i, 0, 0));
            if (!s.hasProperty(CabinetBlock.FRONT)) return "?";
            b.append(switch (s.getValue(CabinetBlock.FRONT)) {
                case TWO -> 'T';
                case PAIR_L -> 'L';
                case PAIR_R -> 'R';
                case SINGLE -> 'S';
                case DRAWERS -> 'D';
            });
            if (!s.getValue(CabinetBlock.JOINTS)) b.append('-');
        }
        return b.toString();
    }

    private void fronts(ServerLevel level, boolean on) {
        Features.set(Feature.CABINET_FRONTS, on);
        for (BlockPos pos : cabinets) CabinetBlock.refresh(level, pos);
        LOGGER.info("[m44] fronts {}: row 6 {}", on ? "on" : "off", fronts(level, 25, 6));
    }

    private static void open(ServerPlayer p, BlockPos pos) {
        if (p.serverLevel().getBlockEntity(pos) instanceof CabinetBlockEntity be) p.openMenu(be, be.getBlockPos());
    }

    // ------------------------------------------------------------------ the walk scene

    private void frames(String name, int n, int gap) {
        for (int i = 0; i < n; i++) {
            int k = i;
            then(gap, () -> shot("walk_" + name + "_" + condition + "_" + k));
        }
    }

    private TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, int sy, int sz) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlock(pos, ModBlocks.TANK.get().defaultBlockState(), 2);
        TankData t = TankMultiblock.formAll(level, blocks);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        t.setFront(Direction.NORTH);
        t.setSignVisible(false);
        t.setOwner(p.getUUID());
        t.setWaterTemp(22F);
        t.setTargetTemp(22F);
        c.markTankChanged();
        return c;
    }

    private static void floor(ServerLevel level, TankBlockEntity c, BlockState ground) {
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) {
            Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), ground, false);
        }
    }

    private static void lid(ServerLevel level, TankBlockEntity c) {
        for (BlockPos top : c.getTank().openTops()) level.setBlock(top, ModBlocks.TANK_LID.get().defaultBlockState(), 3);
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        BlockPos at = p.blockPosition().offset(-12, 0, 120);
        int gy = level.getChunk(at).getHeight(Heightmap.Types.MOTION_BLOCKING, at.getX(), at.getZ()) + 1;
        BlockPos base = new BlockPos(at.getX(), Math.max(gy, p.blockPosition().getY() - 2), at.getZ());
        for (int x = -4; x < 60; x++) for (int z = -16; z < 12; z++) {
            level.setBlock(base.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 2);
            for (int y = 0; y < 8; y++) if (!level.getBlockState(base.offset(x, y, z)).isAir()) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
        glass = base;
        TankBlockEntity gc = tank(level, p, glass, 8, 3, 4);
        floor(level, gc, Blocks.GRASS_BLOCK.defaultBlockState());
        lid(level, gc);
        pets = base.offset(10, 0, 0);
        TankBlockEntity pc = tank(level, p, pets, 10, 3, 5);
        floor(level, pc, Blocks.GRASS_BLOCK.defaultBlockState());
        lid(level, pc);
        herps = base.offset(22, 0, 0);
        TankBlockEntity hc = tank(level, p, herps, 6, 3, 4);
        floor(level, hc, Blocks.SAND.defaultBlockState());
        lid(level, hc);
        big = base.offset(30, 0, 0);
        TankBlockEntity bc = tank(level, p, big, 24, 6, 12);
        floor(level, bc, Blocks.GRASS_BLOCK.defaultBlockState());
        pen = base.offset(0, 0, -14);
        p.teleportTo(level, base.getX() + 4, base.getY() + 3, base.getZ() - 6, 0F, 20F);
        LOGGER.info("[m44] glass {} pets {} herps {} big {} pen {}", glass, pets, herps, big, pen);
    }

    private static TankBlockEntity ctl(ServerLevel level, BlockPos at) {
        TankBlockEntity c = TankMultiblock.getController(level, at);
        org.overengineer.bettacraft.terrarium.TankClimate.step(level, c, 0.01);
        c.getTank().setClimate(new org.overengineer.bettacraft.terrarium.Climate.State(26, 26, 30, 50, 2, 0));
        return c;
    }

    private void track(String label, Entity e) {
        if (e != null) tracked.add(new Tracked(label, e.getId()));
        else fail("not in its tank: " + label);
    }

    private static LivingEntity insertBox(ServerLevel level, TankBlockEntity c, EntityType<?> type) {
        Entity e = type.create(level);
        if (e == null) return null;
        var p = ForeignProfiles.of(type);
        boolean tub = p != null && p.land() != null && !p.land().box();
        ItemStack stack = tub ? TransportTubItem.fillWith(e) : TerrariumBoxItem.fillWith(e);
        e.discard();
        return insert(level, c, stack, type);
    }

    private static LivingEntity insertJar(ServerLevel level, TankBlockEntity c, EntityType<? extends Mob> type) {
        Mob m = type.create(level);
        if (m == null) return null;
        m.setHealth(1F);
        ItemStack jar = MobJarItem.fillWith(m);
        m.discard();
        LivingEntity e = insert(level, c, jar, type);
        if (e != null) e.setHealth(e.getMaxHealth());
        return e;
    }

    private static LivingEntity insert(ServerLevel level, TankBlockEntity c, ItemStack stack, EntityType<?> type) {
        List<Entity> before = TankInhabitants.liveInhabitants(level, c.getTank());
        var err = TankInhabitants.insert(level, c, stack, type, null, TankInhabitants.carriedWater(stack));
        if (err != null) {
            LOGGER.error("[m44] {} refused: {}", BuiltInRegistries.ENTITY_TYPE.getKey(type), err.getString());
            return null;
        }
        for (Entity x : TankInhabitants.liveInhabitants(level, c.getTank())) {
            if (!before.contains(x) && x instanceof LivingEntity le) return le;
        }
        return null;
    }

    private Mob outside(ServerLevel level, EntityType<? extends Mob> type, double dx, double dz) {
        Mob m = type.create(level);
        if (m == null) return null;
        m.moveTo(pen.getX() + dx, pen.getY(), pen.getZ() + dz, 0F, 0F);
        m.setPersistenceRequired();
        m.goalSelector.removeAllGoals(g -> true);
        m.targetSelector.removeAllGoals(g -> true);
        if (type == EntityType.ZOMBIE || type == EntityType.SKELETON) m.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        level.addFreshEntity(m);
        walkersOutside.add(m);
        return m;
    }

    private void place(ServerLevel level) {
        level.getServer().setDifficulty(net.minecraft.world.Difficulty.EASY, true);
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        TankBlockEntity gc = ctl(level, glass), pc = ctl(level, pets), hc = ctl(level, herps), bc = ctl(level, big);
        for (TankBlockEntity c : List.of(gc, pc, hc, bc)) {
            InteriorRegion r = InteriorRegions.get(level.getServer()).get(c.getTank().regionId());
            for (int cx = r.originX() >> 4; cx <= (r.originX() + r.sizeX()) >> 4; cx++)
                for (int cz = r.originZ() >> 4; cz <= (r.originZ() + r.sizeZ()) >> 4; cz++) in.getChunk(cx, cz);
        }
        track("glass_zombie", insertJar(level, gc, EntityType.ZOMBIE));
        track("glass_skeleton", insertJar(level, gc, EntityType.SKELETON));
        track("glass_spider", insertJar(level, gc, EntityType.SPIDER));
        track("glass_husk", insertJar(level, gc, EntityType.HUSK));
        track("pet_chicken", insertBox(level, pc, EntityType.CHICKEN));
        track("pet_cat", insertBox(level, pc, EntityType.CAT));
        track("pet_rabbit", insertBox(level, pc, EntityType.RABBIT));
        track("pet_fox", insertBox(level, pc, EntityType.FOX));
        track("herp_leopard_gecko", insertBox(level, hc, ModEntities.HERPS.get(HerpSpecies.LEOPARD_GECKO).get()));
        track("herp_hermann", insertBox(level, hc, ModEntities.HERMANN_TORTOISE.get()));
        track("climb_crested_gecko", insertBox(level, hc, ModEntities.HERPS.get(HerpSpecies.CRESTED_GECKO).get()));
        track("big_wolf", insertBox(level, bc, EntityType.WOLF));
        track("big_pig", insertBox(level, bc, EntityType.PIG));
        track("big_cow", insertBox(level, bc, EntityType.COW));
        track("big_sheep", insertBox(level, bc, EntityType.SHEEP));
        track("out_chicken", outside(level, EntityType.CHICKEN, 1, 2));
        track("out_zombie", outside(level, EntityType.ZOMBIE, 3, 3));
        track("out_pig", outside(level, EntityType.PIG, 5, 2));
        track("out_wolf", outside(level, EntityType.WOLF, 7, 3));
        track("out_spider", outside(level, EntityType.SPIDER, 9, 2));
        for (TankBlockEntity c : List.of(gc, pc, hc, bc)) c.markTankChanged();
        // spread them out (they all start at the same spot)
        int k = 0;
        for (Tracked t : tracked) {
            Entity e = level.getEntity(t.id());
            if (e == null || !TankInhabitants.hasTankRef(e)) continue;
            TankBlockEntity c = TankInhabitants.resolveController(level, e);
            if (c == null) continue;
            TankData td = c.getTank();
            double fx = 0.2 + 0.6 * ((k * 0.37) % 1.0), fz = 0.3 + 0.4 * ((k * 0.61) % 1.0);
            e.moveTo(td.min().getX() + fx * td.sizeX(), e.getY(), td.min().getZ() + fz * td.sizeZ(), e.getYRot(), 0F);
            k++;
        }
        LOGGER.info("[m44] tracked {}", tracked);
    }

    /** Keeps every walker walking (a new land goal every 2 s; the crested gecko climbs) and the outside mobs going back and forth. */
    private void force(ServerLevel level) {
        for (Tracked t : tracked) {
            Entity e = level.getEntity(t.id());
            LandWalker w = null;
            if (e instanceof TerrariumAnimalEntity ta) w = ta.walker();
            else if (e instanceof Mob m && ModAttachments.GLASS_STATE.has(e)) w = ModAttachments.GLASS_STATE.get(m).walker();
            else if (e != null) w = ForeignTerrarium.walker(e);
            if (w != null) w.forceActivity(t.label().startsWith("climb") ? LandWalker.Activity.CLIMB : LandWalker.Activity.LAND);
        }
        boolean even = (level.getGameTime() / 40) % 2 == 0;
        for (Mob m : walkersOutside) {
            double x = m.getX() + (even ? 4 : -4);
            m.getNavigation().moveTo(Math.max(pen.getX() + 1, Math.min(pen.getX() + 12, x)), pen.getY(), m.getZ(), 1.0);
        }
    }

    // ------------------------------------------------------------------ measuring (client tick)

    private void measure() {
        tick++;
        var server = mc.getSingleplayerServer();
        if (mc.level == null || server == null) return;
        if (tick % 40 == 0) server.execute(() -> force(server.overworld()));
        if (logTicks <= 0) return;
        logTicks--;
        for (Tracked t : tracked) {
            Entity ce = mc.level.getEntity(t.id());
            if (!(ce instanceof LivingEntity c)) continue;
            float speed = c.walkAnimation.speed();
            double[] s = stats.computeIfAbsent(condition + "|" + t.label(), k -> new double[3]);
            s[0] += speed;
            s[1]++;
            if (c.onGround()) s[2]++;
            if (tick % 10 == 0) {
                LOGGER.info(String.format(Locale.ROOT, "[m44] %s %s t=%d cspd=%.4f cpos=%.3f scale=%.3f cog=%b wall=%b",
                        condition, t.label(), tick, speed, c.walkAnimation.position(), c.getScale(), c.onGround(),
                        c instanceof TerrariumAnimalEntity ta && ta.herpPose().wall() != null));
            }
        }
    }

    private double mean(String cond, String label) {
        double[] s = stats.get(cond + "|" + label);
        return s == null || s[1] == 0 ? Double.NaN : s[0] / s[1];
    }

    /** The conditions of the milestone: with auto the tank mobs swing at least 0.2 on average, our geckos above 0, the chicken stands. */
    private void evaluate() {
        StringBuilder table = new StringBuilder();
        double sum = 0;
        int n = 0;
        for (Tracked t : tracked) {
            double off = mean("vanilla", t.label()), auto = mean("auto", t.label());
            table.append(String.format(Locale.ROOT, "%n  %-22s vanilla %.3f  auto %.3f", t.label(), off, auto));
            if (t.label().startsWith("glass_") || t.label().startsWith("pet_") || t.label().startsWith("big_")) {
                if (Double.isFinite(auto)) {
                    sum += auto;
                    n++;
                }
            }
        }
        LOGGER.info("[m44] walk animation means (client walkAnimation.speed):{}", table);
        double tankMean = n == 0 ? 0 : sum / n;
        LOGGER.info(String.format(Locale.ROOT, "[m44] tank mobs with auto: mean %.3f over %d mobs", tankMean, n));
        if (n < 10 || tankMean < 0.2) fail("tank mean " + tankMean + " over " + n);
        for (String g : new String[]{"herp_leopard_gecko", "climb_crested_gecko"}) {
            double v = mean("auto", g);
            if (!(v > 0)) fail(g + " does not walk: " + v);
        }
        double[] ch = stats.get("auto|pet_chicken");
        double ground = ch == null || ch[1] == 0 ? 0 : ch[2] / ch[1];
        LOGGER.info(String.format(Locale.ROOT, "[m44] chicken on the ground (client) %.0f %% of the ticks", ground * 100));
        if (ground < 0.5) fail("the chicken is in the air " + ground);
        LOGGER.info("[m44] walk check {}", failed ? "FAILED" : "OK");
    }

    private void fail(String what) {
        failed = true;
        LOGGER.error("[smoketest] M44 FAILED: {}", what);
    }

    // ------------------------------------------------------------------ steps

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
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (glass != null) measure();
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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m44_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
