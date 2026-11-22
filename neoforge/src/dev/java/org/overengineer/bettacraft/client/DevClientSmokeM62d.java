package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterMovement;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.critter.WildCritterEntity;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankBlockItem;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankStyle;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M62 part 5 and 6 client smoke ({@code -Psmoketestm62d}, NeoForge and Fabric, background window; parts by
 * {@code -Pm62dparts=snail,icons,glass}, default all; {@code -Pm62dtag=<name>} names the snail shots, e.g. {@code before}):
 * <ul>
 *     <li>snail: Tylomelania (and the trumpet and assassin snails, whose shells were lifted) frozen in a tank on the sand and on
 *     the front glass, from the front, from the side along the glass and from above; a wild Tylomelania on the bed of a pool;</li>
 *     <li>icons: every plant item on chest pages (GUI scale 4);</li>
 *     <li>glass: the Glass Tank and the Clear Glass Tank side by side in a chest at GUI scale 4 and 6.</li>
 * </ul>
 * Screenshots {@code bettacraft_m62d_*.png}; log lines "[smoketest] M62d ...".
 */
final class DevClientSmokeM62d {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final Set<String> parts;
    private final String tag;
    private int wait;
    private BlockPos home;
    private volatile BlockPos tankMin, pool;
    private final List<ItemStack> stacks = new ArrayList<>();
    private int guiScale;

    DevClientSmokeM62d() {
        String p = System.getProperty("bettacraft.smoketest.m62dparts", "snail,icons,glass");
        parts = Set.of(p.toLowerCase(Locale.ROOT).split(","));
        String t = System.getProperty("bettacraft.smoketest.m62dtag", "");
        tag = t.isEmpty() ? "" : "_" + t;
        LOGGER.info("[smoketest] M62d parts {} tag '{}'", parts, tag);
        then(1, () -> server(this::setup));
        then(20, () -> {
            org.overengineer.bettacraft.client.DevHud.setHidden(mc, true);
            mc.options.fov().set(50);
        });
        if (parts.contains("snail")) snail();
        if (parts.contains("icons")) icons();
        if (parts.contains("glass")) glass();
        then(1, () -> org.overengineer.bettacraft.client.DevHud.setHidden(mc, false));
        then(20, () -> LOGGER.info("[smoketest] M62d done"));
    }

    private void setup(ServerPlayer p) {
        ServerLevel level = p.level();
        org.overengineer.bettacraft.gametest.framework.DevTime.set(level, 6000);
        level.getServer().setWeatherParameters(12000, 0, false, false);
        home = p.blockPosition();
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 20 * 60 * 30, 0, false, false));
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.CONDUIT_POWER, 20 * 60 * 30, 0, false, false));
    }

    // ------------------------------------------------------------------ the snails

    private void snail() {
        then(1, () -> server(this::buildSnails));
        then(120, () -> server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.level(), tankMin);
            if (c != null) TankCritters.sync(p.level(), c);
        }));
        // the tank is 3 x 1 x 1 (x along the front); the snails sit in its middle block
        then(40, () -> look(tankMin, 1.5, 0.42, -0.62, 0F, 8F));
        then(50, () -> shot("tylo_front" + tag));
        then(1, () -> look(tankMin, 1.5, 0.30, -0.42, 0F, 2F));
        then(40, () -> shot("tylo_front_close" + tag));
        // along the front glass from the east side: the snail on the glass must not reach through it
        then(1, () -> look(tankMin, 3.55, 0.45, 0.06, 90F, 10F));
        then(40, () -> shot("tylo_glass_side" + tag));
        // from the side (east) at the snails on the sand
        then(1, () -> look(tankMin, 3.6, 0.32, 0.62, 90F, 6F));
        then(40, () -> shot("tylo_side" + tag));
        then(1, () -> look(tankMin, 1.5, 1.35, 0.5, 0F, 89.9F));
        then(40, () -> shot("tylo_top" + tag));
        // the wild snail in the pool
        then(1, () -> look(pool, 2.5, 1.7, 1.2, 0F, 55F));
        then(60, () -> shot("tylo_wild" + tag));
        then(1, () -> look(pool, 4.3, 1.5, 2.5, 90F, 50F));
        then(40, () -> shot("tylo_wild_side" + tag));
    }

    private void buildSnails(ServerPlayer p) {
        ServerLevel level = p.level();
        BlockPos min = home.offset(-1, 3, 6);
        for (int x = -2; x < 8; x++) for (int y = -1; y < 4; y++) for (int z = -3; z < 9; z++) level.setBlock(min.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        for (int x = -2; x < 8; x++) for (int z = -3; z < 9; z++) level.setBlock(min.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
        List<BlockPos> blocks = new ArrayList<>();
        for (int x = 0; x < 3; x++) blocks.add(min.offset(x, 0, 0));
        for (BlockPos q : blocks) level.setBlockAndUpdate(q, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos q : blocks) TankMultiblock.onTankBlockAdded(level, q, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        if (c == null) {
            LOGGER.info("[smoketest] M62d no tank");
            return;
        }
        TankData t = c.getTank();
        t.setFront(Direction.NORTH);
        t.setSignVisible(false);
        t.setOwner(p.getUUID());
        TankInteractions.changeWater(level, c, t.capacityMb(), 0F);
        t.setWaterTemp(26F);
        t.setTargetTemp(26F);
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) {
            Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.SAND.defaultBlockState(), false);
        }
        t.critters().clear();
        var terrain = TankCritters.terrain(level, t);
        double bx = min.getX(), bz = min.getZ();
        put(t, terrain, CritterSpecies.TYLOMELANIA, bx + 1.3, bz + 0.35, 90F);
        put(t, terrain, CritterSpecies.TYLOMELANIA, bx + 1.75, bz + 0.7, -120F);
        put(t, terrain, CritterSpecies.TRUMPET_SNAIL, bx + 0.5, bz + 0.4, 90F);
        put(t, terrain, CritterSpecies.ASSASSIN_SNAIL, bx + 2.5, bz + 0.4, 90F);
        Critter g = new Critter(t.nextCritterId(), CritterSpecies.TYLOMELANIA, true, CritterGenes.WILD, bx + 1.5, min.getY() + 0.55, bz + CritterMovement.GLASS_INSET);
        g.age = CritterSpecies.TYLOMELANIA.juvenileDays() + 2;
        g.face = Critter.GLASS_NORTH;
        g.yaw = 90F;
        g.idle = 1_000_000;
        t.critters().add(g);
        c.markTankChanged();
        TankCritters.sync(level, c);
        tankMin = min;
        // a pool beside it with a wild Tylomelania on its bed
        BlockPos b = min.offset(0, 0, 4);
        for (int x = 0; x < 6; x++) for (int z = 0; z < 4; z++) {
            level.setBlock(b.offset(x, -1, z), Blocks.SAND.defaultBlockState(), 2);
            level.setBlock(b.offset(x, 0, z), Blocks.WATER.defaultBlockState(), 2);
            level.setBlock(b.offset(x, 1, z), Blocks.WATER.defaultBlockState(), 2);
        }
        for (int x = -1; x < 7; x++) for (int z = -1; z < 5; z++) {
            if (x >= 0 && x < 6 && z >= 0 && z < 4) continue;
            for (int y = 0; y < 2; y++) level.setBlock(b.offset(x, y, z), Blocks.GLASS.defaultBlockState(), 2);
        }
        WildCritterEntity w = ModEntities.WILD_CRITTER.get().create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (w != null) {
            Critter k = new Critter(0, CritterSpecies.TYLOMELANIA, true, CritterGenes.WILD, 0, 0, 0);
            k.age = CritterSpecies.TYLOMELANIA.juvenileDays() + 2;
            w.setAnimal(k);
            w.snapTo(b.getX() + 2.5, b.getY(), b.getZ() + 2.5, 90F, 0F);
            w.setYBodyRot(90F);
            w.setYHeadRot(90F);
            w.setPersistenceRequired();
            level.addFreshEntity(w);
            SmokeFreeze.freeze(w, false);
        }
        pool = b;
        LOGGER.info("[smoketest] M62d snails in the tank at {} ({} animals), the pool at {}", min, t.critters().size(), b);
    }

    private static void put(TankData t, CritterMovement.Terrain terrain, CritterSpecies s, double x, double z, float yaw) {
        double y = terrain.ground(x, z);
        Critter k = new Critter(t.nextCritterId(), s, true, CritterGenes.WILD, x, Double.isFinite(y) ? y : t.min().getY() + 0.05, z);
        k.age = s.juvenileDays() + 2;
        k.yaw = yaw;
        k.idle = 1_000_000;
        t.critters().add(k);
    }

    // ------------------------------------------------------------------ the plant icons

    private void icons() {
        then(1, () -> {
            stacks.clear();
            for (PlantSpecies s : PlantSpecies.values()) stacks.add(new ItemStack(ModItems.AQUATIC_PLANTS.get(s).get()));
            LOGGER.info("[smoketest] M62d {} plant items", stacks.size());
        });
        then(20, () -> scale(4));
        for (int page = 0; page < 2; page++) {
            int pg = page;
            then(1, () -> server(p -> openPage(p, "plants", pg)));
            then(20, this::mouseAway);
            then(20, () -> shot("icons_plants_" + pg));
            then(1, () -> server(ServerPlayer::closeContainer));
        }
        then(10, this::restoreScale);
    }

    // ------------------------------------------------------------------ the glass tank icons

    private void glass() {
        then(1, () -> {
            stacks.clear();
            ItemStack tank = new ItemStack(ModItems.TANK.get());
            ItemStack clear = TankBlockItem.of(ModItems.TANK.get(), TankBlockEntity.DEFAULT_GLASS, TankStyle.CLEAN, 1);
            for (int i = 0; i < 9; i++) stacks.add(ItemStack.EMPTY);
            stacks.add(ItemStack.EMPTY);
            stacks.add(tank);
            stacks.add(ItemStack.EMPTY);
            stacks.add(clear);
            stacks.add(ItemStack.EMPTY);
            stacks.add(tank.copy());
            stacks.add(clear.copy());
        });
        for (int s : new int[]{4, 6}) {
            then(20, () -> scale(s));
            then(1, () -> server(p -> openPage(p, "glass", 0)));
            then(20, this::mouseAway);
            then(20, () -> shot("clear_glass_gui" + s));
            then(1, () -> server(ServerPlayer::closeContainer));
        }
        then(10, this::restoreScale);
    }

    private void scale(int s) {
        org.overengineer.bettacraft.client.DevHud.setHidden(mc, false);
        if (guiScale == 0) guiScale = mc.options.guiScale().get() == 0 ? -1 : mc.options.guiScale().get();
        mc.options.guiScale().set(s);
        mc.resizeGui();
    }

    private void restoreScale() {
        mc.options.guiScale().set(Math.max(0, guiScale));
        guiScale = 0;
        mc.resizeGui();
    }

    private void openPage(ServerPlayer p, String what, int page) {
        if (page * 54 >= stacks.size()) return;
        SimpleContainer c = new SimpleContainer(54);
        StringBuilder legend = new StringBuilder();
        for (int i = 0; i < 54 && page * 54 + i < stacks.size(); i++) {
            ItemStack s = stacks.get(page * 54 + i);
            c.setItem(i, s.copy());
            if (!s.isEmpty()) legend.append(i).append('=').append(s.getHoverName().getString()).append("; ");
        }
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> ChestMenu.sixRows(id, inv, c), Component.literal("M62 " + what + " " + (page + 1))));
        LOGGER.info("[smoketest] M62d {} page {}: {}", what, page, legend);
    }

    // ------------------------------------------------------------------ steps

    private void look(BlockPos b, double dx, double eyeDy, double dz, float yaw, float pitch) {
        if (b == null) return;
        server(p -> p.teleportTo(p.level(), b.getX() + dx, b.getY() + eyeDy - p.getEyeHeight(), b.getZ() + dz, java.util.Set.of(), yaw, pitch, true));
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    boolean tick() {
        if (mc.player != null && !steps.isEmpty()) {
            mc.player.getAbilities().flying = true;
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
        if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.PauseScreen) mc.gui.setScreen(null);
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

    /** Moves the mouse into the top left corner (no tooltip over the slots). */
    private void mouseAway() {
        mc.mouseHandler.onMove(mc.getWindow().handle(), 1, 1, 0, 0);
        try {
            for (String f : new String[]{"xpos", "ypos"}) {
                java.lang.reflect.Field fld = net.minecraft.client.MouseHandler.class.getDeclaredField(f);
                fld.setAccessible(true);
                fld.setDouble(mc.mouseHandler, 1);
            }
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("[smoketest] M62d cannot move the mouse: {}", e.toString());
        }
    }

    private void shot(String name) {
        mc.gui.toastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m62d_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, c -> {});
    }
}
