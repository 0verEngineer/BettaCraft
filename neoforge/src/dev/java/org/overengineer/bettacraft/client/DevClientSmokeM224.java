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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.OutletSide;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.progression.GuideBook;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tank.plants.TankPlants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M22.4 dev client smoke scene ({@code -Psmoketestm22_4}): four tanks on cabinets with a filter, one per outlet side (back,
 * left, right, front), with floating plants drifting away from the jet; the cabinet menu with the outlet button (en + de);
 * the tooltip of a cabinet that keeps its side; the handbook pages with the new "Obtaining" (Amazon sword, Flamingo, Where
 * to find what) and the cabinet page (en + de). Checks (errors in the log): every client cabinet got its side over the
 * network. Screenshots {@code bettacraft_m22_4_*}.
 */
final class DevClientSmokeM224 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static final int SPACING = 5;

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private BlockPos base;
    private final String lang = Minecraft.getInstance().options.languageCode;
    private CompletableFuture<Void> reload;
    private int gui;

    DevClientSmokeM224() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        // 1. the four outlet sides in a row, seen from the front (the cabinet doors face the camera)
        then(1, () -> look(base.getX() + 1.5 * SPACING + 1.5, base.getY() + 5.0, base.getZ() - 11.0, 0F, 16F));
        then(140, () -> {
            checkSync();
            shot("outlets");
        });
        then(1, () -> look(base.getX() + 1.5 * SPACING + 1.5, base.getY() + 9.0, base.getZ() - 5.5, 0F, 48F));
        then(60, () -> shot("outlets_above"));
        for (OutletSide side : OutletSide.values()) {
            int i = side.ordinal();
            then(1, () -> look(base.getX() + i * SPACING + 1.5, base.getY() + 3.6, base.getZ() - 3.6, 0F, 22F));
            then(50, () -> shot("outlet_" + side.id()));
        }
        // 2. the cabinet menu with the outlet button, English and German; the tooltip of a cabinet item with its side
        then(1, () -> mc.options.hideGui = false);
        then(1, () -> server(this::fillInventory));
        then(40, () -> mc.getToastManager().clear()); // the recipe and advancement toasts of the new items
        cabinetMenu("cabinet_menu");
        then(5, () -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));
        then(20, () -> hover(s -> s.is(ModItems.CABINET.get()) && s.has(ModDataComponents.OUTLET_SIDE.get())));
        then(10, () -> shot("tooltip_cabinet"));
        then(5, () -> mc.setScreen(null));
        // 3. the handbook: plants with their sources, the cabinet page (English, then German)
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
        ServerLevel level = p.level();
        level.setDayTime(6000);
        level.setWeatherParameters(24000, 0, false, false);
        level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME, false, p.level().getServer());
        base = p.blockPosition().offset(-10, 0, 480);
        // a light floor and a white wall behind the row
        for (int x = -6; x < 4 * SPACING + 6; x++)
            for (int z = -12; z < 8; z++) {
                level.setBlock(base.offset(x, -1, z), Blocks.SMOOTH_QUARTZ.defaultBlockState(), 2);
                for (int y = 0; y < 12; y++) level.setBlock(base.offset(x, y, z), z == 6 ? Blocks.WHITE_CONCRETE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
            }
        for (OutletSide side : OutletSide.values()) {
            BlockPos at = base.offset(side.ordinal() * SPACING, 0, 0);
            // two cabinets facing north (towards the camera), the filter in the right one (seen from the front: west)
            for (int x = 0; x < 3; x++) level.setBlockAndUpdate(at.offset(x, 0, 0), ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH));
            for (int x = 0; x < 3; x++) {
                BlockPos cp = at.offset(x, 0, 0);
                level.setBlockAndUpdate(cp, net.minecraft.world.level.block.Block.updateFromNeighbourShapes(level.getBlockState(cp), level, cp));
            }
            TankBlockEntity c = tank(level, p, at.above(), 3, 2, 2);
            CabinetBlockEntity cab = (CabinetBlockEntity) level.getBlockEntity(at.offset(1, 0, 0));
            cab.items().setStackInSlot(CabinetBlockEntity.FILTER_SLOT, new ItemStack(ModItems.EXTERNAL_FILTER.get()));
            cab.setOutletSide(side);
            // floating plants: they drift away from the outlet
            for (int k = 0; k < 6; k++) {
                FloatingPlantType t = k % 3 == 0 ? FloatingPlantType.SALVINIA : k % 3 == 1 ? FloatingPlantType.FROGBIT : FloatingPlantType.DUCKWEED;
                TankPlants.add(c, t, at.getX() + 0.6 + (k % 3) * 0.9, at.getZ() + 0.6 + (k / 3) * 0.8, 0.8F, k * 40F);
            }
            org.overengineer.bettacraft.tank.TankEquipment.invalidate(c);
        }
        LOGGER.info("[smoketest] M22.4 scene at {}", base);
    }

    private TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, int sy, int sz) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        // three quarters full: the jets fall into the water
        TankInteractions.changeWater(level, c, (long) sx * sy * sz * 750, 0F);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setOwner(p.getUUID());
        c.getTank().setWaterTemp(25F);
        c.getTank().setTargetTemp(25F);
        c.markTankChanged();
        return c;
    }

    /** Every client cabinet with a filter knows the side the server set (network sync). */
    private void checkSync() {
        if (mc.level == null) return;
        int ok = 0;
        for (OutletSide side : OutletSide.values()) {
            BlockPos cp = base.offset(side.ordinal() * SPACING + 1, 0, 0);
            if (mc.level.getBlockEntity(cp) instanceof CabinetBlockEntity cab && cab.hasFilter() && cab.outletSide() == side) ok++;
            else LOGGER.error("[smoketest] M22.4 the client cabinet at {} does not show the outlet {}", cp, side);
        }
        LOGGER.info("[smoketest] M22.4 {} of 4 cabinets synced their outlet side", ok);
    }

    private void fillInventory(ServerPlayer p) {
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.getAbilities().mayfly = true;
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.getInventory().clearContent();
        ItemStack left = new ItemStack(ModItems.CABINET.get());
        left.set(ModDataComponents.OUTLET_SIDE.get(), OutletSide.LEFT.id());
        p.getInventory().setItem(0, left);
        p.getInventory().setItem(1, new ItemStack(ModItems.EXTERNAL_FILTER.get()));
        p.getInventory().setItem(2, new ItemStack(ModItems.AQUARIUM_NET.get()));
        p.getInventory().setItem(3, InteriorPlants.item(PlantSpecies.AMAZON_SWORD, 0));
        p.getInventory().setItem(4, InteriorPlants.item(PlantSpecies.CRYPT_FLAMINGO, 0));
        p.inventoryMenu.broadcastChanges();
    }

    private void cabinetMenu(String name) {
        then(10, () -> server(p -> {
            BlockPos cp = base.offset(OutletSide.RIGHT.ordinal() * SPACING + 1, 0, 0);
            if (p.level().getBlockEntity(cp) instanceof CabinetBlockEntity cab) org.overengineer.bettacraft.platform.Platform.get().openMenu(p, cab, cp);
        }));
        then(30, () -> {
            if (!(mc.screen instanceof org.overengineer.bettacraft.client.screen.CabinetScreen)) LOGGER.error("[smoketest] M22.4 no cabinet menu");
            mouseTo(-50, -50);
        });
        then(10, () -> shot(name));
        // the button: the next side (front), the setting arrives at the cabinet
        then(1, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.CabinetScreen s) {
                double x = s.getGuiLeft() + org.overengineer.bettacraft.client.screen.CabinetScreen.OUTLET_X + 10;
                double y = s.getGuiTop() + org.overengineer.bettacraft.client.screen.CabinetScreen.OUTLET_Y + 5;
                s.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x, y, new net.minecraft.client.input.MouseButtonInfo(0, 0)), false);
                s.mouseReleased(new net.minecraft.client.input.MouseButtonEvent(x, y, new net.minecraft.client.input.MouseButtonInfo(0, 0)));
            }
        });
        then(10, () -> {
            BlockPos cp = base.offset(OutletSide.RIGHT.ordinal() * SPACING + 1, 0, 0);
            server(p -> {
                var be = p.level().getBlockEntity(cp);
                if (!(be instanceof CabinetBlockEntity cab) || cab.outletSide() != OutletSide.FRONT) {
                    LOGGER.error("[smoketest] M22.4 the outlet button did not set the next side: {}", be instanceof CabinetBlockEntity c2 ? c2.outletSide() : be);
                } else {
                    LOGGER.info("[smoketest] M22.4 the outlet button set {}", cab.outletSide());
                }
            });
        });
        then(10, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.CabinetScreen s) {
                mouseTo(s.getGuiLeft() + org.overengineer.bettacraft.client.screen.CabinetScreen.OUTLET_X + 20,
                        s.getGuiTop() + org.overengineer.bettacraft.client.screen.CabinetScreen.OUTLET_Y + 5);
            }
        });
        then(10, () -> shot(name + "_front_tooltip"));
        then(1, () -> mouseTo(-50, -50));
        // the menu in German: only the language (no resource reload, like the M22.3 menus)
        then(1, () -> quickLanguage("de_de"));
        then(20, () -> shot(name + "_de"));
        then(1, () -> quickLanguage(lang));
        // back to the right wall, the menu closed
        then(1, () -> server(p -> {
            BlockPos cp = base.offset(OutletSide.RIGHT.ordinal() * SPACING + 1, 0, 0);
            if (p.level().getBlockEntity(cp) instanceof CabinetBlockEntity cab) cab.setOutletSide(OutletSide.RIGHT);
            p.closeContainer();
        }));
        then(5, () -> mc.setScreen(null));
    }

    // ------------------------------------------------------------------ handbook

    private void guide(String l) {
        if (!GuideBook.available()) {
            LOGGER.warn("[smoketest] M22.4 GuideME not loaded, no handbook pictures");
            return;
        }
        then(1, () -> {
            gui = mc.options.guiScale().get();
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
        });
        for (String page : new String[]{"plants/amazon_sword.md", "plants/cryptocoryne_flamingo.md", "plants/red_tiger_lotus.md", "equipment/cabinet.md",
                "getting_started/sources.md"}) {
            String name = page.substring(page.indexOf('/') + 1, page.length() - 3);
            open(page);
            then(40, () -> shot("guide_" + name + "_" + l));
            then(1, this::scrollPage);
            then(10, () -> shot("guide_" + name + "_" + l + "_2"));
        }
        then(1, () -> {
            mc.setScreen(null);
            mc.options.guiScale().set(gui);
            mc.resizeDisplay();
        });
    }

    private void open(String page) {
        then(5, () -> org.overengineer.bettacraft.compat.guideme.GuideMECompat.open(mc.player, GuideBook.GUIDE_ID, BettaCraft.id(page)));
    }

    private void scrollPage() {
        if (mc.screen == null) return;
        double x = mc.getWindow().getGuiScaledWidth() * 0.65, y = mc.getWindow().getGuiScaledHeight() / 2.0;
        for (int i = 0; i < 8; i++) mc.screen.mouseScrolled(x, y, 0, -3);
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
        LOGGER.warn("[smoketest] M22.4 item to hover not found");
    }

    private void mouseTo(int x, int y) {
        double scale = mc.getWindow().getGuiScale();
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().handle(), x * scale, y * scale);
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
        // like the language screen: reload the resources, so GuideME loads the pages of the new language
        reload = mc.reloadResourcePacks();
    }

    private void quickLanguage(String code) {
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
        if (mc.screen != null) mc.screen.resize(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    private void awaitReload() {
        if (reload != null && !reload.isDone()) {
            steps.addFirst(new Step(10, this::awaitReload));
        } else {
            if (mc.screen != null) mc.screen.resize(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            LOGGER.info("[smoketest] M22.4 resources reloaded for {}", mc.options.languageCode);
        }
    }

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.level(), x, eyeY - p.getEyeHeight(), z, java.util.Set.of(), yaw, pitch, false));
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
        mc.getToastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m22_4_" + name + ".png", mc.getMainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
