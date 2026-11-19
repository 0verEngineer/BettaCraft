package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.client.interior.IrisCompat;
import org.overengineer.bettacraft.client.interior.IrisLightColors;
import org.overengineer.bettacraft.client.tank.TankLightTint;
import org.overengineer.bettacraft.config.ClientConfig;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.equipment.TankLightItem;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M18 part of the dev client smoke test: six tanks under lights of different colours (white, red, blue, lime, magenta,
 * black = violet) by day and by night, close-ups, the panel from below, the water without tint (lightTint 0), dyeing
 * a bar on the server (the client sees the new colour), the 16 light items in the inventory, and the stocking tooltip
 * of a tank with young bettas (English, German). Screenshots {@code bettacraft_m18_*}. Checks (errors in the log):
 * coloured tanks have a tint on the client, the white one none; with a shader pack the lights got shader ids.
 */
final class DevClientSmokeM18 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static final DyeColor[] ROW = {DyeColor.WHITE, DyeColor.RED, DyeColor.BLUE, DyeColor.LIME, DyeColor.MAGENTA, DyeColor.BLACK};
    private static final int SPACING = 4;

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private BlockPos base, young;
    private final List<BlockPos> controllers = new ArrayList<>();
    private int wait;

    DevClientSmokeM18() {
        then(1, () -> server(this::build));
        then(1, () -> org.overengineer.bettacraft.client.DevHud.setHidden(mc, true));
        // 1. the row by day and by night
        then(40, () -> look(base.getX() + 11.0, base.getY() + 3.6, base.getZ() - 13.0, 0F, 12F));
        then(80, () -> shot("row_day"));
        then(1, () -> server(p -> org.overengineer.bettacraft.gametest.framework.DevTime.set(p.level(), 18000)));
        then(60, () -> {
            shot("row_night");
            check();
        });
        // 2. close-ups at night through the front glass
        then(1, () -> look(base.getX() + SPACING + 1.5, base.getY() + 1.3, base.getZ() - 2.2, 0F, 6F));
        then(40, () -> shot("red_night"));
        then(1, () -> look(base.getX() + 5 * SPACING + 1.5, base.getY() + 1.3, base.getZ() - 2.2, 0F, 6F));
        then(40, () -> shot("black_night"));
        then(1, () -> look(base.getX() + 2 * SPACING + 1.5, base.getY() + 1.3, base.getZ() - 2.2, 0F, 6F));
        then(40, () -> shot("blue_night"));
        // the panel seen from below the bar, through the front glass and the water surface
        then(1, () -> look(base.getX() + 3 * SPACING + 1.5, base.getY() + 1.55, base.getZ() - 2.4, 0F, -9F));
        then(40, () -> shot("panel_below"));
        // 3. no water tint: the colour only at the lamp
        then(1, () -> look(base.getX() + 11.0, base.getY() + 3.6, base.getZ() - 13.0, 0F, 12F));
        then(1, () -> ClientConfig.LIGHT_TINT.set(0.0));
        then(60, () -> shot("row_night_notint"));
        then(1, () -> ClientConfig.LIGHT_TINT.set(0.5));
        // 4. dyeing the white bar orange on the server: the client follows
        then(1, () -> server(p -> {
            BlockPos l = controllers.getFirst().offset(0, 2, 1);
            int n = TankLightBlock.dyeBar(p.level(), l, p.level().getBlockState(l), DyeColor.ORANGE, false);
            LOGGER.info("[smoketest] M18 dyed {} segments orange", n);
        }));
        then(60, () -> {
            int c = TankLightTint.colorOf(controllers.getFirst());
            LOGGER.info("[smoketest] M18 tint of the dyed tank: {}", Integer.toHexString(c));
            if (c == 0) LOGGER.error("[smoketest] M18 the dyed bar does not tint its tank on the client");
            shot("row_night_orange");
        });
        then(1, () -> server(p -> org.overengineer.bettacraft.gametest.framework.DevTime.set(p.level(), 6000)));
        // 5. the light items (GUI)
        then(1, () -> org.overengineer.bettacraft.client.DevHud.setHidden(mc, false));
        then(1, () -> server(p -> {
            reach(p, true); // the menu needs it
            p.teleportTo(p.level(), base.getX() + 11.5, base.getY() + 1.0, base.getZ() - 8.5, java.util.Set.of(), 0F, 12F, false);
        }));
        then(20, this::openCreativeTab);
        then(10, () -> hoverLight(DyeColor.RED));
        then(20, () -> shot("items"));
        String lang = mc.options.languageCode;
        then(1, () -> language("de_de"));
        then(5, () -> hoverLight(DyeColor.LIGHT_BLUE));
        then(20, () -> shot("items_de"));
        then(1, () -> language(lang));
        then(1, () -> mc.gui.setScreen(null));
        // 6. the stocking of a tank with young bettas (tooltip of the animals line)
        then(5, () -> server(p -> {
            p.teleportTo(p.level(), young.getX() + 1.5, young.getY() + 1.0, young.getZ() - 2.5, java.util.Set.of(), 0F, 0F, false);
            TankBlockEntity c = TankMultiblock.getController(p.level(), young);
            org.overengineer.bettacraft.platform.Platform.get().openMenu(p, new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new org.overengineer.bettacraft.tank.menu.TankMenu(id, inv, c),
                    Component.translatable("gui.bettacraft.tank.title")), buf -> buf.writeBlockPos(c.getBlockPos()));
        }));
        then(30, () -> {
            if (mc.gui.screen() instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.selectTabForTest(0);
        });
        then(5, this::hoverStocking);
        then(20, () -> shot("menu_stocking"));
        then(1, () -> language("de_de"));
        then(5, this::hoverStocking);
        then(20, () -> shot("menu_stocking_de"));
        then(1, () -> language(lang));
        then(1, () -> server(ServerPlayer::closeContainer));
        then(5, () -> mc.gui.setScreen(null));
        then(1, () -> org.overengineer.bettacraft.client.DevHud.setHidden(mc, true));
    }

    /** No info overlay (Jade) in the pictures: no reach while the scene runs. */
    private static void reach(ServerPlayer p, boolean normal) {
        var b = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE);
        var e = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE);
        if (b != null) b.setBaseValue(normal ? 4.5 : 0.0);
        if (e != null) e.setBaseValue(normal ? 3.0 : 0.0);
    }

    /** The creative inventory on the Bettacraft tab (all 16 lights). */
    private void openCreativeTab() {
        var scr = new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true);
        mc.gui.setScreen(scr);
        try {
            var m = net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", net.minecraft.world.item.CreativeModeTab.class);
            m.setAccessible(true);
            m.invoke(scr, org.overengineer.bettacraft.registry.ModCreativeTabs.MAIN.get());
            // scroll so the row of the first light is on top
            var menu = (net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.ItemPickerMenu) scr.getMenu();
            int idx = 0;
            while (idx < menu.items.size() && !menu.items.get(idx).is(ModItems.TANK_LIGHT.get())) idx++;
            int rows = Math.max(1, (menu.items.size() + 8) / 9 - 5);
            float pos = Math.min(1F, (idx / 9) / (float) rows);
            menu.scrollTo(pos);
            var f = net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.class.getDeclaredField("scrollOffs");
            f.setAccessible(true);
            f.setFloat(scr, pos);
        } catch (ReflectiveOperationException ex) {
            LOGGER.warn("[smoketest] M18 cannot select the creative tab: {}", ex.toString());
        }
    }

    /** Over the light of this colour in the open creative tab. */
    private void hoverLight(DyeColor c) {
        if (!(mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> scr)) return;
        for (var s : scr.getMenu().slots) {
            ItemStack st = s.getItem();
            if (st.is(ModItems.TANK_LIGHT.get()) && TankLightItem.colorOf(st) == c) {
                mouseTo(scr.getGuiLeft() + s.x + 8, scr.getGuiTop() + s.y + 8);
                return;
            }
        }
        LOGGER.error("[smoketest] M18 no {} light in the creative tab", c);
    }

    private void check() {
        for (int i = 0; i < ROW.length; i++) {
            int c = TankLightTint.colorOf(controllers.get(i));
            LOGGER.info("[smoketest] M18 {} tank: client tint {}", ROW[i], Integer.toHexString(c));
            if ((ROW[i] == DyeColor.WHITE) != (c == 0)) LOGGER.error("[smoketest] M18 wrong tint for the {} tank: {}", ROW[i], Integer.toHexString(c));
            BlockState l = mc.level.getBlockState(controllers.get(i).offset(0, 2, 1));
            if (!l.is(ModBlocks.TANK_LIGHT.get()) || l.getValue(TankLightBlock.COLOR) != ROW[i]) LOGGER.error("[smoketest] M18 client has the light as {}", l);
        }
        if (IrisCompat.shaderPackInUse()) {
            LOGGER.info("[smoketest] M18 shader pack: {} light states use the ids of coloured vanilla lights", IrisLightColors.aliased);
            if (IrisLightColors.aliased == 0) LOGGER.warn("[smoketest] M18 shader pack in use but no light colours given (pack without candle ids?)");
        }
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
        c.getTank().setWaterTemp(27F);
        c.getTank().setTargetTemp(27F);
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++)
            for (int z = 0; z < r.sizeZ(); z++) Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.SAND.defaultBlockState(), false);
        return c;
    }

    private static void lights(ServerLevel level, BlockPos min, int sx, int y, int z, DyeColor color) {
        for (int x = 0; x < sx; x++) {
            level.setBlockAndUpdate(min.offset(x, y, z), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < sx - 1).setValue(TankLightBlock.LIT, true)
                    .setValue(TankLightBlock.COLOR, color));
        }
    }

    private static void insert(ServerLevel level, TankBlockEntity c, ServerPlayer p, LifeStage s, long seed) {
        BettaData d = BettaData.create(BettaGenotype.randomDomestic(RandomSource.create(seed)), seed % 2 == 0, 0).withStage(s);
        Component err = TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d), ModEntities.BETTA.get(), p, 0L);
        if (err != null) LOGGER.error("[smoketest] M18 betta not inserted: {}", err.getString());
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        reach(p, false);
        base = p.blockPosition().offset(0, 2, 370);
        for (int i = 0; i < ROW.length; i++) {
            BlockPos min = base.offset(i * SPACING, 0, 0);
            place(level, cuboid(min, 3, 2, 2));
            TankBlockEntity c = setup(level, p, min, 12000);
            lights(level, min, 3, 2, 1, ROW[i]);
            insert(level, c, p, LifeStage.ADULT, 100 + i);
            insert(level, c, p, LifeStage.ADULT, 201 + i);
            controllers.add(c.getBlockPos());
        }
        // the tank with young bettas for the menu (and young shrimp)
        young = base.offset(ROW.length * SPACING + 2, 0, 0);
        place(level, cuboid(young, 3, 2, 2));
        TankBlockEntity y = setup(level, p, young, 12000);
        lights(level, young, 3, 2, 1, DyeColor.CYAN);
        insert(level, y, p, LifeStage.ADULT, 300);
        insert(level, y, p, LifeStage.ADULT, 301);
        for (int i = 0; i < 6; i++) insert(level, y, p, LifeStage.FRY, 310 + i);
        for (int i = 0; i < 4; i++) insert(level, y, p, LifeStage.JUVENILE, 320 + i);
        for (int i = 0; i < 2; i++) insert(level, y, p, LifeStage.SUBADULT, 330 + i);
        for (int i = 0; i < 10; i++) TankCritters.add(level, y, CritterSpecies.NEOCARIDINA, CritterGenes.WILD, i % 2 == 0, i < 5 ? 0 : CritterSpecies.NEOCARIDINA.juvenileDays() + 1);
        TankCritters.sync(level, y);
        // still fish for the pictures
        for (BlockPos cp : controllers) freeze(level, cp);
        LOGGER.info("[smoketest] M18 scene at {}: {} coloured tanks, young tank stocking {}", base, controllers.size(),
                TankInhabitants.stocking(level, y.getTank()));
    }

    private static void freeze(ServerLevel level, BlockPos controller) {
        TankBlockEntity c = TankMultiblock.getController(level, controller);
        if (c == null) return;
        for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) {
            if (e instanceof Mob m) {
                SmokeFreeze.freeze(m, true);
                m.setDeltaMovement(Vec3.ZERO);
            }
        }
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
        if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.PauseScreen) mc.gui.setScreen(null);
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

    /** Puts the eye of the player at (x, eyeY, z). */
    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.level(), x, eyeY - p.getEyeHeight(), z, java.util.Set.of(), yaw, pitch, false));
    }

    private void language(String code) {
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
        if (mc.gui.screen() != null) mc.gui.screen().resize(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    private void hoverStocking() {
        if (mc.gui.screen() instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) {
            int[] at = ts.stockingLineForTest();
            mouseTo(at[0], at[1]);
        } else {
            LOGGER.error("[smoketest] M18 the tank menu is not open");
        }
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

    private void shot(String name) {
        mc.gui.toastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m18_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, c -> LOGGER.info("[smoketest] " + c.getString()));
    }
}
