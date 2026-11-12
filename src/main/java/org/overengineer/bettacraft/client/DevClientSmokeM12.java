package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.plants.FloatingPlant;
import org.overengineer.bettacraft.tank.plants.FloatingPlantSim;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tank.plants.TankPlants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M12 part of the dev client smoke test: floating plants. A 5x2x3 tank with all four species and a lily pad under a
 * lit tank light (the red root floater turns red), a 3x2x2 tank under a stone roof (weak light: the red root floater
 * stays green), and an L-shaped tank with a raised step full of duckweed and salvinia. Screenshots
 * {@code bettacraft_m12_*}: overview, close-up from above, frogbit roots from the side through the glass, weak light,
 * the free shape, the plants page of the tank menu and the info overlay.
 */
final class DevClientSmokeM12 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private BlockPos base;
    private int wait;

    DevClientSmokeM12() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        // once the light engine has caught up: the red of the red root floaters as their light makes it
        then(40, () -> server(p -> {
            settleRed(p.serverLevel(), TankMultiblock.getController(p.serverLevel(), base));
            settleRed(p.serverLevel(), TankMultiblock.getController(p.serverLevel(), base.offset(0, 0, 14)));
        }));
        // overview of the main tank, slightly from above
        then(80, () -> look(base.getX() + 2.5, base.getY() + 4.4, base.getZ() - 3.2, 0F, 40F));
        then(80, () -> shot("overview"));
        // close-up from above: leaves on the surface
        then(1, () -> look(base.getX() + 2.5, base.getY() + 3.6, base.getZ() + 0.4, 0F, 62F));
        then(50, () -> shot("closeup"));
        // from the side, a little below the surface: the roots hang into the water
        then(1, () -> look(base.getX() + 2.4, base.getY() + 1.55, base.getZ() - 1.6, 0F, -6F));
        then(50, () -> shot("roots"));
        // weak light under the roof: green red root floater
        then(1, () -> look(base.getX() + 1.5, base.getY() + 3.1, base.getZ() + 14 - 2.2, 0F, 38F));
        then(50, () -> shot("weak_light"));
        // the free shape from above
        then(1, () -> look(base.getX() + 11.5, base.getY() + 6.2, base.getZ() - 1.4, 0F, 58F));
        then(50, () -> shot("free_shape"));
        // the info overlay and the plants page of the menu (with GUI)
        then(1, () -> mc.options.hideGui = false);
        then(1, () -> look(base.getX() + 2.5, base.getY() + 1.5, base.getZ() - 2.2, 0F, 0F));
        then(60, () -> shot("overlay"));
        then(1, () -> server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.serverLevel(), base);
            p.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new org.overengineer.bettacraft.tank.menu.TankMenu(id, inv, c),
                    Component.translatable("gui.bettacraft.tank.title")), buf -> buf.writeBlockPos(c.getBlockPos()));
        }));
        then(30, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.showPlantsPageForTest();
        });
        then(30, () -> shot("menu_plants"));
        then(1, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.selectTabForTest(5);
        });
        then(20, () -> shot("menu_water"));
        // the seven tabs (small tab font) at GUI scale 2, 3 and 4, in English and German: screenshots and a check that
        // every label fits its button and the row fits the menu
        String lang = mc.options.languageCode;
        for (String code : new String[]{"en_us", "de_de"}) {
            then(1, () -> language(code));
            for (int scale = 2; scale <= 4; scale++) {
                int sc = scale;
                then(1, () -> {
                    mc.options.guiScale().set(sc);
                    mc.resizeDisplay();
                });
                then(10, () -> {
                    if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) {
                        ts.showPlantsPageForTest();
                        String fit = DevClientSmokeMP.tabsFit(mc, ts.tabButtonsForTest());
                        StringBuilder labels = new StringBuilder();
                        for (var b : ts.tabButtonsForTest()) labels.append(b.getMessage().getString()).append('=').append(b.getWidth()).append(' ');
                        LOGGER.info("[smoketest] M12 menu tabs {} GUI scale {}: {} [{}]", code, sc, fit, labels.toString().trim());
                        if (!fit.startsWith("tabs ok")) LOGGER.error("[smoketest] M12 menu tabs do not fit: {} {}", code, fit);
                    }
                });
                then(20, () -> shot("menu_tabs_" + code.substring(0, 2) + "_gui" + sc));
            }
            then(1, () -> {
                if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.selectTabForTest(5);
            });
            then(20, () -> shot("menu_water_" + code.substring(0, 2) + "_gui4"));
        }
        then(1, () -> {
            language(lang);
            mc.options.guiScale().set(0);
            mc.resizeDisplay();
        });
        then(1, () -> server(ServerPlayer::closeContainer));
        then(5, () -> mc.setScreen(null));
        then(1, () -> server(p -> {
            for (BlockPos at : List.of(base, base.offset(0, 0, 14), base.offset(9, 0, 0))) {
                TankBlockEntity c = TankMultiblock.getController(p.serverLevel(), at);
                if (c == null) continue;
                TankData t = c.getTank();
                LOGGER.info("[smoketest] M12 tank {}: {} plants, cover {} %, light {}", at, t.plants().size(),
                        Math.round(TankPlants.coverage(t) * 100), TankPlants.light(p.serverLevel(), c));
            }
        }));
    }

    private static void place(ServerLevel level, List<BlockPos> blocks) {
        for (BlockPos pos : blocks) level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
    }

    private static List<BlockPos> cuboid(BlockPos min, int sx, int sy, int sz) {
        List<BlockPos> l = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) l.add(min.offset(x, y, z));
        return l;
    }

    private static TankBlockEntity setup(ServerLevel level, ServerPlayer p, BlockPos min, long water) {
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, water);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setOwner(p.getUUID());
        return c;
    }

    /** {@code n} plants of {@code type} around (cx, cz) (absolute), size 0.6-1. */
    private static void patch(TankBlockEntity c, FloatingPlantType type, double cx, double cz, int n, double spread, RandomSource r) {
        for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * spread;
            TankPlants.add(c, type, cx + Math.cos(a) * d, cz + Math.sin(a) * d, 0.6F + r.nextFloat() * 0.4F, r.nextFloat() * 6.28F);
        }
    }

    /** Red of the red root floaters as the light of the tank makes it (instead of waiting a quarter day). */
    private static void settleRed(ServerLevel level, TankBlockEntity c) {
        int light = TankPlants.light(level, c);
        for (FloatingPlant pl : c.getTank().plants()) pl.red = (float) FloatingPlantSim.redTarget(light);
        c.markTankChanged();
        LOGGER.info("[smoketest] M12 light at {}: {} (red {})", c.getBlockPos(), light, FloatingPlantSim.redTarget(light));
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        base = p.blockPosition().offset(0, 2, 190);
        RandomSource r = RandomSource.create(1212);
        // main tank 5x2x3 with a lit light bar along the middle row
        place(level, cuboid(base, 5, 2, 3));
        TankBlockEntity main = setup(level, p, base, 30000);
        for (int x = 0; x < 5; x++) {
            level.setBlockAndUpdate(base.offset(x, 2, 2), ModBlocks.TANK_LIGHT.get().defaultBlockState()
                    .setValue(TankLightBlock.AXIS, Direction.Axis.X).setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 4));
        }
        double bx = base.getX(), bz = base.getZ();
        patch(main, FloatingPlantType.DUCKWEED, bx + 0.9, bz + 0.9, 14, 0.7, r);
        patch(main, FloatingPlantType.FROGBIT, bx + 2.4, bz + 1.2, 7, 0.6, r);
        patch(main, FloatingPlantType.SALVINIA, bx + 4.0, bz + 1.0, 8, 0.7, r);
        patch(main, FloatingPlantType.RED_ROOT_FLOATER, bx + 3.2, bz + 2.3, 8, 0.6, r);
        TankPlants.add(main, FloatingPlantType.LILY_PAD, bx + 1.3, bz + 2.3, 1F, 0.7F);
        settleRed(level, main);
        fish(level, p, main, new double[][]{{1.5, 0.6, 1.2}, {3.4, 0.9, 1.6}});
        // tank under a stone roof: weak light, the red root floater stays green
        BlockPos dark = base.offset(0, 0, 14);
        place(level, cuboid(dark, 3, 2, 2));
        TankBlockEntity weak = setup(level, p, dark, 12000);
        // a stone roof one block above the tank, reaching 5 blocks out: only side light from the open edges comes in
        for (int x = -5; x <= 7; x++) for (int z = -5; z <= 6; z++) level.setBlockAndUpdate(dark.offset(x, 4, z), Blocks.SMOOTH_STONE.defaultBlockState());
        patch(weak, FloatingPlantType.RED_ROOT_FLOATER, dark.getX() + 1.5, dark.getZ() + 1.0, 9, 0.8, r);
        patch(weak, FloatingPlantType.FROGBIT, dark.getX() + 0.6, dark.getZ() + 0.7, 2, 0.2, r);
        // L-shaped tank with a raised back row: plants only float over the water of the surface layer
        BlockPos fs = base.offset(9, 0, 0);
        List<BlockPos> shape = new ArrayList<>();
        for (int x = 0; x < 5; x++) for (int z = 0; z < 4; z++) if (!(x >= 3 && z < 2)) shape.add(fs.offset(x, 0, z));
        for (int x = 0; x < 5; x++) shape.add(fs.offset(x, 1, 3));
        place(level, shape);
        TankBlockEntity free = setup(level, p, fs, 15000);
        patch(free, FloatingPlantType.DUCKWEED, fs.getX() + 1.5, fs.getZ() + 1.5, 26, 1.6, r);
        patch(free, FloatingPlantType.SALVINIA, fs.getX() + 3.5, fs.getZ() + 2.6, 8, 0.8, r);
        fish(level, p, free, new double[][]{{1.2, 0.5, 1.5}});
        LOGGER.info("[smoketest] M12 scene at {}: main {} plants, weak light {} plants, free shape {} blocks / {} plants", base,
                main.getTank().plants().size(), weak.getTank().plants().size(), free.getTank().memberCount(), free.getTank().plants().size());
    }

    private static void fish(ServerLevel level, ServerPlayer p, TankBlockEntity c, double[][] spots) {
        List<Map<String, String>> genes = List.of(
                Map.of("finform", "halfmoon", "fin_length", "long", "body_color", "royal_blue", "fin_color", "red", "pattern", "butterfly",
                        "pattern_color", "white", "iridescence", "high"),
                Map.of("finform", "round", "fin_length", "short", "body_color", "yellow", "fin_color", "yellow", "pattern", "koi",
                        "pattern_color", "orange"));
        for (int i = 0; i < spots.length; i++) {
            BettaData d = BettaData.create(BettaGenome.of(genes.get(i % genes.size()), BettaGenome.DEFAULT), i > 0, level.getGameTime());
            Component err = TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d),
                    ModEntities.BETTA.get(), p, false);
            if (err != null) LOGGER.warn("[smoketest] M12 insert failed: {}", err.getString());
        }
        TankData t = c.getTank();
        int i = 0;
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) {
            if (!(e instanceof BettaEntity b) || i >= spots.length) continue;
            SmokeFreeze.freeze(b, true);
            b.moveTo(t.min().getX() + spots[i][0], t.min().getY() + spots[i][1], t.min().getZ() + spots[i][2], 90F + i * 60F, 0F);
            b.setYBodyRot(b.getYRot());
            b.setYHeadRot(b.getYRot());
            i++;
        }
    }

    /** Puts the eye of the player at (x, eyeY, z). */
    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.serverLevel(), x, eyeY - p.getEyeHeight(), z, yaw, pitch));
    }

    /** Switches the client language without a full resource reload (only the translations), re-inits the open screen. */
    private void language(String code) {
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
        if (mc.screen != null) mc.screen.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    /** Returns true when all steps are done. */
    boolean tick() {
        if (mc.player != null && !steps.isEmpty()) {
            mc.player.getAbilities().flying = true;
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
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

    private void server(java.util.function.Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        mc.getToasts().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m12_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
