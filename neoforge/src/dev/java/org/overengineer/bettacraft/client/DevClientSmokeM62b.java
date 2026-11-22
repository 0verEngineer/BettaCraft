package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.client.screen.FilterLinkScreen;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity;
import org.overengineer.bettacraft.equipment.FilterLinks;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M62 parts 2 and 3 client smoke ({@code -Psmoketestm62b}, NeoForge and Fabric, background window): the under-cabinet over
 * the whole area (a 10 x 5 tank on a full area of cabinets, a 4 x 4 of cabinets, a ring of cabinets around stone, a 4 x 1
 * tank with a cabinet at each end and its shared spring in the overlay) and the external filter below the tank (one and two
 * blocks lower with the long hose, an unlinked one in the overlay, the choice of the tank with the outline, en and de, the
 * item tooltip). Screenshots {@code bettacraft_m62b_*.png}; log lines "[smoketest] M62b ...".
 */
final class DevClientSmokeM62b {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private int problems;
    private volatile BlockPos home;
    private volatile BlockPos big, square, ring, pair, below1, below2, unlinked, choice;
    private boolean powerFilter, power, winding;
    private String lang;

    DevClientSmokeM62b() {
        then(1, () -> server(this::setup));
        then(80, () -> look(big.offset(5, 2, 2), -9, 6, -10));
        then(30, () -> shot("cabinet_area_10x5"));
        then(1, () -> look(big.offset(5, 2, 6), 8, 5, 10));
        then(30, () -> shot("cabinet_area_10x5_back"));
        then(1, () -> look(square.offset(2, 1, 2), -6, 5, -7));
        then(30, () -> shot("cabinet_area_4x4"));
        then(1, () -> look(ring.offset(4, 1, 2), 7, 5, -9));
        then(30, () -> shot("cabinet_ring"));
        then(1, () -> look(pair, -1.2, 1.0, -2.4));
        then(40, () -> shot("cabinet_pair_overlay"));
        then(1, () -> look(below1.offset(1, 1, 0), -3, 2.5, -4));
        then(30, () -> shot("filter_one_below"));
        then(1, () -> look(below2.offset(1, 1, 0), -3, 3, -4.5));
        then(30, () -> shot("filter_two_below"));
        then(1, () -> look(unlinked, -1.6, 1.2, -2.2));
        then(40, () -> shot("filter_unlinked_overlay"));
        // the choice of the tank: two tanks side by side, the filter one below between them
        then(1, () -> look(choice.offset(0, 4, 0), 0, -1.5, -6.3));
        then(10, () -> server(p -> FilterLinks.open(p, choice)));
        then(20, () -> {
            if (!(mc.gui.screen() instanceof FilterLinkScreen s)) problem("the choice did not open");
            else {
                if (s.data().options().size() != 2) problem("choice: " + s.data().options().size() + " tanks instead of 2");
                s.hover(1);
            }
        });
        then(10, () -> shot("filter_choice_en"));
        then(1, () -> quickLanguage("de_de"));
        then(10, () -> shot("filter_choice_de"));
        then(1, () -> quickLanguage(lang));
        then(5, () -> {
            if (mc.gui.screen() instanceof FilterLinkScreen s) s.choose(1);
        });
        then(10, () -> shot("filter_chosen"));
        then(5, this::checkChoice);
        // the tooltip of the filter item (drawn by a small screen: the creative inventory of the smoke world has no slots to hover)
        then(5, () -> mc.gui.setScreen(new TooltipScreen(new ItemStack(ModItems.EXTERNAL_FILTER.get()))));
        then(20, () -> shot("filter_tooltip"));
        then(5, () -> mc.gui.setScreen(null));
        then(5, () -> server(p -> restore()));
        then(5, () -> LOGGER.info("[smoketest] M62b done: {} problem(s)", problems));
    }

    int problems() {
        return problems;
    }

    // ------------------------------------------------------------------ the scene

    private void setup(ServerPlayer p) {
        ServerLevel level = p.level();
        lang = mc.options.languageCode;
        mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
        powerFilter = ServerConfig.POWER_FILTER.get();
        power = Features.own(Feature.POWER);
        winding = Features.own(Feature.WINDING);
        ServerConfig.POWER_FILTER.set(true);
        Features.set(Feature.POWER, true);
        Features.set(Feature.WINDING, true);
        org.overengineer.bettacraft.gametest.framework.DevTime.set(level, 6000);
        level.getServer().setWeatherParameters(24000, 0, false, false);
        home = p.blockPosition();
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        BlockPos o = home.offset(-20, 0, 6);
        for (int x = -4; x < 52; x++) for (int z = -6; z < 14; z++) {
            level.setBlock(o.offset(x, -1, z), Blocks.SMOOTH_QUARTZ.defaultBlockState(), 2);
            for (int y = 0; y < 8; y++) level.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
        // 1. a 10 x 5 tank on a full area of cabinets (facing north: the front row at the smallest z)
        big = o;
        area(level, big, 10, 5, false);
        tank(level, big.above(), 10, 2, 5);
        // 2. a 4 x 4 of cabinets with a 4 x 4 tank
        square = o.offset(14, 0, 0);
        area(level, square, 4, 4, false);
        tank(level, square.above(), 4, 2, 4);
        // 3. a ring of cabinets around stone (cheaper), an 8 x 4 tank on it
        ring = o.offset(21, 0, 0);
        area(level, ring, 8, 4, true);
        tank(level, ring.above(), 8, 2, 4);
        // 4. a 4 x 1 tank with a cabinet at each end: one spring store
        pair = o.offset(32, 0, 0);
        level.setBlock(pair, cabinet(), 3);
        level.setBlock(pair.offset(3, 0, 0), cabinet(), 3);
        TankBlockEntity pt = tank(level, pair.above(), 4, 1, 1);
        if (level.getBlockEntity(pair) instanceof CabinetBlockEntity cab) {
            cab.items().setStackInSlot(CabinetBlockEntity.FILTER_SLOT, new ItemStack(ModItems.EXTERNAL_FILTER.get()));
            if (pt != null) TankEquipment.invalidate(pt);
            cab.windStore().set(cab.windStore().capacity() * 2 / 5);
        }
        // 5. filters one and two blocks below a tank (beside the cabinet; the second tank stands on a cabinet and a block)
        below1 = o.offset(38, 0, 0);
        level.setBlock(below1.offset(1, 0, 0), cabinet(), 3);
        level.setBlock(below1.offset(2, 0, 0), cabinet(), 3);
        TankBlockEntity t1 = tank(level, below1.offset(1, 1, 0), 2, 2, 2);
        filter(level, below1, t1, below1.offset(1, 1, 0));
        below2 = o.offset(44, 0, 0);
        for (int x = 1; x <= 2; x++) {
            level.setBlock(below2.offset(x, 0, 0), Blocks.SPRUCE_PLANKS.defaultBlockState(), 3);
            level.setBlock(below2.offset(x, 1, 0), cabinet(), 3);
        }
        TankBlockEntity t2 = tank(level, below2.offset(1, 2, 0), 2, 2, 2);
        filter(level, below2, t2, below2.offset(1, 2, 0));
        // 6. an unlinked filter one below a tank (nobody chose its tank yet)
        unlinked = o.offset(0, 0, 10);
        level.setBlock(unlinked.offset(1, 0, 0), cabinet(), 3);
        tank(level, unlinked.offset(1, 1, 0), 2, 1, 1);
        level.setBlock(unlinked, ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, Direction.NORTH), 3);
        // 7. two tanks side by side, a filter one below between them
        choice = o.offset(10, 0, 10);
        level.setBlock(choice.offset(-1, 0, 0), cabinet(), 3);
        level.setBlock(choice.offset(1, 0, 0), cabinet(), 3);
        tank(level, choice.offset(-2, 1, 0), 2, 2, 1);
        tank(level, choice.offset(1, 1, 0), 2, 2, 1);
        level.setBlock(choice, ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, Direction.NORTH), 3);
        LOGGER.info("[smoketest] M62b scene built at {}", o);
    }

    private static BlockState cabinet() {
        return ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH);
    }

    /** An area of cabinets facing north (the front row at {@code at}'s z); {@code ring}: stone inside. */
    private void area(ServerLevel level, BlockPos at, int sx, int sz, boolean ring) {
        for (int x = 0; x < sx; x++) for (int z = 0; z < sz; z++) {
            boolean edge = x == 0 || z == 0 || x == sx - 1 || z == sz - 1;
            level.setBlock(at.offset(x, 0, z), ring && !edge ? Blocks.STONE.defaultBlockState() : cabinet(), 3);
        }
        for (int x = 0; x < sx; x++) for (int z = 0; z < sz; z++) CabinetBlock.refresh(level, at.offset(x, 0, z));
        int covered = 0;
        for (int x = 0; x < sx; x++) for (int z = 0; z < sz; z++) {
            BlockState s = level.getBlockState(at.offset(x, 0, z));
            if (s.getBlock() instanceof CabinetBlock && s.getValue(CabinetBlock.COVERED)) covered++;
        }
        // full: all but the front row; ring: the side columns behind the front row (the back row faces the stone)
        int expected = ring ? 2 * (sz - 1) : sx * (sz - 1);
        if (covered != expected) problem("area " + sx + "x" + sz + (ring ? " ring" : "") + ": " + covered + " covered instead of " + expected);
        else LOGGER.info("[smoketest] M62b area {}x{}{}: {} carcass blocks PASS", sx, sz, ring ? " ring" : "", covered);
    }

    private TankBlockEntity tank(ServerLevel level, BlockPos min, int sx, int sy, int sz) {
        for (int x = 0; x < sx; x++) for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) {
            BlockPos q = min.offset(x, y, z);
            level.setBlock(q, ModBlocks.TANK.get().defaultBlockState(), 3);
            TankMultiblock.onTankBlockAdded(level, q, null);
        }
        TankBlockEntity c = TankMultiblock.getController(level, min);
        if (c == null) {
            problem("tank at " + min + " did not form");
            return null;
        }
        TankInteractions.changeWater(level, c, c.getTank().capacityMb(), 0F);
        return c;
    }

    private void filter(ServerLevel level, BlockPos at, TankBlockEntity c, BlockPos wall) {
        level.setBlock(at, ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, Direction.NORTH), 3);
        if (!(level.getBlockEntity(at) instanceof ExternalFilterBlockEntity f) || c == null) {
            problem("filter at " + at);
            return;
        }
        FilterLinks.link(level, f, wall);
        TankEquipment.invalidate(c);
        if (!TankEquipment.filters(level, c).contains(f)) problem("filter " + f.below() + " below is not attached");
        else LOGGER.info("[smoketest] M62b filter {} below attached PASS", f.below());
    }

    private void checkChoice() {
        server(p -> {
            if (!(p.level().getBlockEntity(choice) instanceof ExternalFilterBlockEntity f) || FilterLinks.unlinked(p.level(), f)) problem("the choice did not connect the filter");
            else LOGGER.info("[smoketest] M62b the choice connected the filter to {} PASS", f.link());
        });
    }

    private void restore() {
        ServerConfig.POWER_FILTER.set(powerFilter);
        Features.set(Feature.POWER, power);
        Features.set(Feature.WINDING, winding);
    }

    // ------------------------------------------------------------------ helpers

    /** Shows the tooltip of one item in the middle of the window. */
    private static final class TooltipScreen extends net.minecraft.client.gui.screens.Screen {
        private final ItemStack stack;

        TooltipScreen(ItemStack stack) {
            super(net.minecraft.network.chat.Component.empty());
            this.stack = stack;
        }

        @Override
        public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
            super.extractRenderState(g, mouseX, mouseY, partialTick);
            int x = width / 2 - 100, y = height / 2 - 20;
            g.item(stack, x - 20, y - 12);
            g.setTooltipForNextFrame(font, stack, x, y);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    private void hoverFilter() {
        if (!(mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> scr)) return;
        for (var s : scr.getMenu().slots) {
            if (s.getItem().is(ModItems.EXTERNAL_FILTER.get())) {
                mouseTo(scr.getGuiLeft() + s.x + 8, scr.getGuiTop() + s.y + 8);
                return;
            }
        }
        problem("the filter item to hover was not found");
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

    private void quickLanguage(String code) {
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
        if (mc.gui.screen() != null) mc.gui.screen().resize(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    private void problem(String what) {
        problems++;
        LOGGER.error("[smoketest] M62b FAIL: {}", what);
    }

    /** Puts the eye at {@code at} + (dx, dy, dz), looking at the centre of {@code at}. */
    private void look(BlockPos at, double dx, double dy, double dz) {
        Vec3 c = Vec3.atCenterOf(at);
        Vec3 eye = c.add(dx, dy, dz);
        Vec3 d = c.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
        server(p -> p.teleportTo(p.level(), eye.x, eye.y - p.getEyeHeight(), eye.z, java.util.Set.of(), yaw, pitch, true));
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

    private void shot(String name) {
        mc.gui.toastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m62b_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, c -> {});
    }
}
