package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.equipment.AirPumpBlock;
import org.overengineer.bettacraft.equipment.DeviceSegmentBlock;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.equipment.HighPowerLedBlock;
import org.overengineer.bettacraft.equipment.LedPanelBlock;
import org.overengineer.bettacraft.equipment.SegmentBlock;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.equipment.TankLightItem;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.progression.GuideBook;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankAeration;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankLighting;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M23 dev client smoke scene ({@code -Psmoketestm23}): three tanks under the three light tiers (light bar, LED panels,
 * high-power LED) with red and strong-light plants; the left tank with an air pump, three air stones, a sponge filter and a
 * corner filter (bubble columns in the miniature, the air hose), an external filter at 50 %; the same at night with the
 * daylight timer; inside the interior (bubble columns, sponge filter); the tank menu tab Devices (en + de), the
 * regulator screen of the air pump, the external filter menu with its power button; the items; the handbook pages (en +
 * de). Checks (errors in the log): the client sees the air stones active, the panels in pairs, the timer switched the
 * lights. Screenshots {@code bettacraft_m23_*}.
 */
final class DevClientSmokeM23 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static final int SPACING = 7;

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final BlockPos base;
    private volatile InteriorRegion airRegion;
    private final String lang = Minecraft.getInstance().options.languageCode;
    private CompletableFuture<Void> reload;
    private int gui;

    DevClientSmokeM23() {
        base = mc.player.blockPosition().offset(-10, 0, 560); // known before the server built the scene (it runs async)
        then(1, () -> server(this::build));
        then(20, () -> {});
        then(1, () -> mc.options.hideGui = true);
        // 1. the three tanks under the three tiers, front and from above
        then(1, () -> look(base.getX() + SPACING + 2.0, base.getY() + 4.0, base.getZ() - 9.0, 0F, 14F));
        then(160, () -> {
            check();
            shot("tiers");
        });
        then(1, () -> look(base.getX() + SPACING + 2.0, base.getY() + 9.5, base.getZ() - 4.0, 0F, 52F));
        then(60, () -> shot("tiers_above"));
        // 2. the aerated tank close: bubble columns in the miniature, air pump and hose
        then(1, () -> look(base.getX() + 2.0, base.getY() + 2.6, base.getZ() - 3.4, 0F, 12F));
        then(80, () -> shot("bubbles"));
        then(1, () -> look(base.getX() - 2.6, base.getY() + 3.2, base.getZ() - 1.8, -55F, 30F));
        then(60, () -> shot("air_pump"));
        // 3. night: the timer switched the panel tank off, the others shine
        then(1, () -> server(p -> {
            p.serverLevel().setDayTime(18000);
            for (int i = 0; i < 3; i++) {
                TankBlockEntity c = TankMultiblock.getController(p.serverLevel(), base.offset(i * SPACING, 1, 0));
                if (c != null) TankLighting.step(p.serverLevel(), c);
            }
        }));
        then(1, () -> look(base.getX() + SPACING + 2.0, base.getY() + 4.0, base.getZ() - 9.0, 0F, 14F));
        then(80, this::checkNight);
        then(1, () -> shot("night_timer"));
        then(1, () -> server(p -> p.serverLevel().setDayTime(6000)));
        // 4. inside the aerated tank
        then(1, () -> server(p -> {
            InteriorRegion r = airRegion;
            if (r == null) return;
            ServerLevel in = InteriorManager.interiorLevel(p.server);
            BlockPos at = r.toWorld(40, 12, 2);
            p.teleportTo(in, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 20F, 10F);
        }));
        then(160, () -> shot("interior"));
        then(1, () -> server(p -> {
            InteriorRegion r = airRegion;
            if (r == null) return;
            BlockPos at = r.toWorld(14, 6, 6);
            p.teleportTo(p.serverLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, -10F, -12F);
        }));
        then(100, () -> {
            int n = DeviceEffects.spongeFiltersForTest();
            if (n != 1) LOGGER.error("[smoketest] M23 the client found {} running sponge filters in the interior (want 1)", n);
            else LOGGER.info("[smoketest] M23 the client sees the sponge filter bubble in the interior: {}", DeviceEffects.spongeBubblesForTest());
            shot("interior_stones");
        });
        then(1, () -> server(p -> {
            if (InteriorManager.isInterior(p.level())) p.teleportTo(p.server.overworld(), base.getX() + 2.0, base.getY() + 1.5, base.getZ() - 3.5, 0F, 0F);
        }));
        // 5. menus: tab Devices en + de, regulator of the air pump, external filter with its power button
        then(60, () -> mc.options.hideGui = false);
        then(1, () -> server(this::fillInventory));
        then(40, () -> mc.getToasts().clear());
        tankMenu("menu_devices", base.offset(0, 1, 0));
        tankMenu("menu_devices_panel", base.offset(SPACING, 1, 0));
        then(1, () -> org.overengineer.bettacraft.util.ClientHooks.openRegulator(base.offset(-1, 1, 1)));
        then(20, () -> {
            if (!(mc.screen instanceof org.overengineer.bettacraft.client.screen.RegulatorScreen)) LOGGER.error("[smoketest] M23 no regulator screen");
            mouseTo(-50, -50);
        });
        then(10, () -> shot("regulator"));
        then(1, () -> quickLanguage("de_de"));
        then(20, () -> shot("regulator_de"));
        then(1, () -> quickLanguage(lang));
        then(1, () -> mc.setScreen(null));
        then(10, () -> server(p -> {
            BlockPos fp = base.offset(2, 1, 3);
            if (p.serverLevel().getBlockEntity(fp) instanceof net.minecraft.world.MenuProvider mp) org.overengineer.bettacraft.platform.Platform.get().openMenu(p, mp, fp);
        }));
        then(30, () -> {
            if (!(mc.screen instanceof org.overengineer.bettacraft.client.screen.ExternalFilterScreen)) LOGGER.error("[smoketest] M23 no filter menu");
            mouseTo(-50, -50);
        });
        then(10, () -> shot("filter_menu"));
        then(1, () -> server(ServerPlayer::closeContainer));
        then(5, () -> mc.setScreen(null));
        // 6. the items with their tooltips
        then(5, () -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));
        then(20, () -> hover(s -> s.is(ModItems.SPONGE_FILTER.get())));
        then(10, () -> shot("items"));
        then(5, () -> mc.setScreen(null));
        // 7. the handbook
        guide("en");
        then(1, () -> language("de_de"));
        then(1, this::awaitReload);
        guide("de");
        then(1, () -> language(lang));
        then(1, this::awaitReload);
        then(1, () -> mc.setScreen(null));
    }

    // ------------------------------------------------------------------ the scene

    private void build(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        level.setDayTime(6000);
        level.setWeatherParameters(24000, 0, false, false);
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, p.server);
        for (int x = -8; x < 3 * SPACING + 6; x++)
            for (int z = -12; z < 9; z++) {
                level.setBlock(base.offset(x, -1, z), Blocks.SMOOTH_QUARTZ.defaultBlockState(), 2);
                level.setBlock(base.offset(x, 0, z), Blocks.SMOOTH_QUARTZ.defaultBlockState(), 2);
                for (int y = 1; y < 14; y++) level.setBlock(base.offset(x, y, z), z == 7 ? Blocks.WHITE_CONCRETE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
            }
        // three tanks 4x2x3 on the floor at y+1
        TankBlockEntity[] tanks = new TankBlockEntity[3];
        for (int i = 0; i < 3; i++) tanks[i] = tank(level, p, base.offset(i * SPACING, 1, 0), 4, 2, 3);
        BlockPos top0 = base.offset(0, 3, 0), top1 = base.offset(SPACING, 3, 0), top2 = base.offset(2 * SPACING, 3, 0);
        // tier 1: bars along x on the middle row
        for (int x = 0; x < 4; x++) level.setBlockAndUpdate(top0.offset(x, 0, 1), ModBlocks.TANK_LIGHT.get().defaultBlockState()
                .setValue(TankLightBlock.AXIS, Direction.Axis.X).setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 3).setValue(TankLightBlock.LIT, true));
        // tier 2: a long panel two blocks wide (rows z 0 and 1) with the daylight timer
        for (int x = 0; x < 4; x++)
            for (int z = 0; z < 2; z++) {
                level.setBlockAndUpdate(top1.offset(x, 0, z), ModBlocks.LED_PANEL.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                        .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 3).setValue(LedPanelBlock.PARTNER_POS, z == 0)
                        .setValue(TankLightBlock.LIT, true));
            }
        tanks[1].getTank().setLightTimer(true);
        // tier 3: a high-power fixture over the whole top, blue-white like a planted tank
        for (int x = 0; x < 4; x++)
            for (int z = 0; z < 3; z++) {
                level.setBlockAndUpdate(top2.offset(x, 0, z), ModBlocks.HIGH_POWER_LED.get().defaultBlockState().setValue(TankLightBlock.NEG, x > 0)
                        .setValue(TankLightBlock.POS, x < 3).setValue(HighPowerLedBlock.NORTH, z > 0).setValue(HighPowerLedBlock.SOUTH, z < 2)
                        .setValue(TankLightBlock.LIT, true).setValue(TankLightBlock.COLOR, x == 1 || x == 2 ? DyeColor.WHITE : DyeColor.LIGHT_BLUE));
            }
        // plants in every tank: red Alternanthera, Ludwigia and Monte Carlo (strong light)
        ServerLevel in = InteriorManager.interiorLevel(p.server);
        for (int i = 0; i < 3; i++) {
            InteriorRegion r = InteriorManager.ensureRegion(level, tanks[i]);
            InteriorPlants.plantAt(in, r, PlantSpecies.ALTERNANTHERA, 46, 0, 32, 6, 7L + i);
            InteriorPlants.plantAt(in, r, PlantSpecies.LUDWIGIA, 54, 0, 30, 6, 11L + i);
            for (int k = 0; k < 4; k++) InteriorPlants.plantAt(in, r, PlantSpecies.MONTE_CARLO, 8 + k * 9, 0, 8, 5, 21L + k + i);
            InteriorPlants.settleTint(level, tanks[i]);
        }
        // the left tank: air pump on the west side, three air stones, a sponge filter, a corner filter with intake sponge, an external filter at 50 %
        InteriorRegion r0 = InteriorManager.ensureRegion(level, tanks[0]);
        airRegion = r0;
        level.setBlockAndUpdate(base.offset(-1, 1, 1), ModBlocks.AIR_PUMP.get().defaultBlockState().setValue(AirPumpBlock.FACING, Direction.EAST));
        for (int[] s : new int[][]{{12, 0, 20}, {24, 0, 24}, {36, 0, 18}}) {
            in.setBlock(r0.toWorld(s[0], s[1], s[2]), ModBlocks.AIR_STONE.get().defaultBlockState(), Block.UPDATE_ALL);
        }
        place(in, r0, ModBlocks.SPONGE_FILTER_SEGMENT.get(), 4, 0, 38, 2, 5, Direction.NORTH, false);
        place(in, r0, ModBlocks.CORNER_FILTER_SEGMENT.get(), 60, 0, 44, 2, 7, Direction.NORTH, true);
        level.setBlockAndUpdate(base.offset(2, 1, 3), ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, Direction.NORTH));
        if (level.getBlockEntity(base.offset(2, 1, 3)) instanceof org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity f) f.setRegulator(5);
        for (TankBlockEntity c : tanks) {
            TankEquipment.invalidate(c);
            TankAeration.step(level, c, 0.01);
        }
        LOGGER.info("[smoketest] M23 scene at {}", base);
    }

    /** A segment piece (foot x foot x height) like the placement does it. */
    private static void place(ServerLevel in, InteriorRegion r, Block block, int x0, int y0, int z0, int foot, int height, Direction facing, boolean sponge) {
        for (int y = 0; y < height; y++)
            for (int z = 0; z < foot; z++)
                for (int x = 0; x < foot; x++) {
                    SegmentBlock.Part part = y == 0 ? (x == 0 && z == 0 ? SegmentBlock.Part.ANCHOR : SegmentBlock.Part.BASE)
                            : y == height - 1 ? SegmentBlock.Part.TOP : SegmentBlock.Part.MIDDLE;
                    BlockState s = block.defaultBlockState().setValue(SegmentBlock.PART, part).setValue(SegmentBlock.FACING, facing)
                            .setValue(DeviceSegmentBlock.SPONGE, sponge && y == 0);
                    in.setBlock(r.toWorld(x0 + x, y0 + y, z0 + z), s, Block.UPDATE_ALL);
                }
    }

    private TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, int sy, int sz) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, (long) sx * sy * sz * 900, 0F);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setOwner(p.getUUID());
        c.getTank().setWaterTemp(25F);
        c.getTank().setTargetTemp(25F);
        c.markTankChanged();
        return c;
    }

    /** The client sees the stones bubble (miniature) and the panels in pairs. */
    private void check() {
        if (mc.level == null) return;
        TankBlockEntity c = TankMultiblock.getController(mc.level, base.offset(0, 1, 0));
        var region = c == null || c.getTank() == null ? null : org.overengineer.bettacraft.client.interior.ClientInteriorCache.get(c.getTank().regionId());
        int bubblers = region == null ? 0 : region.bubblers().size();
        if (bubblers < 4) LOGGER.error("[smoketest] M23 the miniature shows {} bubbling air stones and sponge filters (want 4)", bubblers);
        else LOGGER.info("[smoketest] M23 the miniature shows {} bubblers", bubblers);
        int panels = 0;
        for (int x = 0; x < 4; x++) for (int z = 0; z < 2; z++) if (mc.level.getBlockState(base.offset(SPACING + x, 3, z)).getBlock() instanceof LedPanelBlock) panels++;
        if (panels != 8) LOGGER.error("[smoketest] M23 only {} of 8 panel blocks on the client", panels);
        var pump = mc.level.getBlockEntity(base.offset(-1, 1, 1));
        if (!(pump instanceof org.overengineer.bettacraft.equipment.AirPumpBlockEntity a) || !a.running()) LOGGER.error("[smoketest] M23 the client air pump does not run");
    }

    private void checkNight() {
        if (mc.level == null) return;
        boolean panelLit = mc.level.getBlockState(base.offset(SPACING, 3, 0)).getValue(TankLightBlock.LIT);
        boolean hpLit = mc.level.getBlockState(base.offset(2 * SPACING, 3, 0)).getValue(TankLightBlock.LIT);
        if (panelLit || !hpLit) LOGGER.error("[smoketest] M23 night: panel (timer) lit {}, high-power (no timer) lit {}", panelLit, hpLit);
        else LOGGER.info("[smoketest] M23 night: the timer switched the panel off");
    }

    private void fillInventory(ServerPlayer p) {
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.getAbilities().mayfly = true;
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.getInventory().clearContent();
        ItemStack[] hotbar = {new ItemStack(ModItems.AIR_PUMP.get()), new ItemStack(ModItems.AIR_HOSE.get()), new ItemStack(ModItems.AIR_STONE.get()),
                new ItemStack(ModItems.SPONGE_FILTER.get()), new ItemStack(ModItems.CORNER_FILTER.get()), new ItemStack(ModItems.INTAKE_SPONGE.get()),
                new ItemStack(ModItems.LED_PANEL.get()), TankLightItem.of(ModItems.HIGH_POWER_LED.get(), DyeColor.LIGHT_BLUE, 1),
                TankLightItem.of(ModItems.LED_PANEL.get(), DyeColor.RED, 1)};
        for (int i = 0; i < hotbar.length; i++) p.getInventory().setItem(i, hotbar[i]);
        p.inventoryMenu.broadcastChanges();
    }

    private void tankMenu(String name, BlockPos tankPos) {
        then(10, () -> server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.serverLevel(), tankPos);
            if (c == null) return;
            org.overengineer.bettacraft.platform.Platform.get().openMenu(p, new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new org.overengineer.bettacraft.tank.menu.TankMenu(id, inv, c),
                    Component.translatable("gui.bettacraft.tank.title")), buf -> buf.writeBlockPos(c.getBlockPos()));
        }));
        then(30, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.selectTabForTest(7);
            else LOGGER.error("[smoketest] M23 no tank menu for {}", name);
            mouseTo(-50, -50);
        });
        then(20, () -> shot(name));
        then(1, () -> quickLanguage("de_de"));
        then(20, () -> shot(name + "_de"));
        then(1, () -> quickLanguage(lang));
        then(1, () -> server(ServerPlayer::closeContainer));
        then(5, () -> mc.setScreen(null));
    }

    // ------------------------------------------------------------------ handbook

    private void guide(String l) {
        if (!GuideBook.available()) {
            LOGGER.warn("[smoketest] M23 GuideME not loaded, no handbook pictures");
            return;
        }
        then(1, () -> {
            gui = mc.options.guiScale().get();
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
        });
        for (String page : new String[]{"equipment/air_pump.md", "equipment/air_stone.md", "equipment/intake_sponge.md", "equipment/led_panel.md",
                "equipment/high_power_led.md", "equipment/light.md", "equipment/regulator.md", "aquarium/oxygen.md"}) {
            String name = page.substring(page.indexOf('/') + 1, page.length() - 3);
            then(5, () -> org.overengineer.bettacraft.compat.guideme.GuideMECompat.open(mc.player, GuideBook.GUIDE_ID, BettaCraft.id(page)));
            then(40, () -> shot("guide_" + name + "_" + l));
        }
        then(1, () -> {
            mc.setScreen(null);
            mc.options.guiScale().set(gui);
            mc.resizeDisplay();
        });
    }

    // ------------------------------------------------------------------ helpers

    private void hover(java.util.function.Predicate<ItemStack> which) {
        if (!(mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> scr)) return;
        for (var s : scr.getMenu().slots) {
            if (which.test(s.getItem())) {
                mouseTo(scr.getGuiLeft() + s.x + 8, scr.getGuiTop() + s.y + 8);
                return;
            }
        }
        LOGGER.warn("[smoketest] M23 item to hover not found");
    }

    private void mouseTo(int x, int y) {
        double scale = mc.getWindow().getGuiScale();
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), x * scale, y * scale);
        try {
            for (String f : new String[]{"xpos", "ypos"}) {
                java.lang.reflect.Field fld = net.minecraft.client.MouseHandler.class.getDeclaredField(f);
                fld.setAccessible(true);
                fld.setDouble(mc.mouseHandler, f.equals("xpos") ? x * scale : y * scale);
            }
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("[smoketest] cannot move the mouse: {}", e.toString());
        }
    }

    private void language(String code) {
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        reload = mc.reloadResourcePacks();
    }

    private void quickLanguage(String code) {
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
        if (mc.screen != null) mc.screen.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    private void awaitReload() {
        if (reload != null && !reload.isDone()) {
            steps.addFirst(new Step(10, this::awaitReload));
        } else {
            if (mc.screen != null) mc.screen.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            LOGGER.info("[smoketest] M23 resources reloaded for {}", mc.options.languageCode);
        }
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

    private int wait;

    private void server(java.util.function.Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        mc.getToasts().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m23_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
