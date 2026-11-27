package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankPower;
import org.overengineer.bettacraft.winding.Winding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M67 client smoke ({@code -Psmoketestm67}, NeoForge and Fabric, background window): one energy store per tank on EASY. A 2 x 2 x 2
 * tank on four cabinets with one more joined beside them, an external filter with a heater, a light. The crank at the cabinet (the
 * action bar), the tank overview with the power tooltip, the cabinet and the external filter menus with the store's bar and its
 * tooltip, in English and German. Checks: the crank charges the store, both menus show the same store with the tank's capacity.
 * Screenshots {@code bettacraft_m67_<lang>_<shot>.png}.
 */
final class DevClientSmokeM67 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private int problems;
    private volatile BlockPos tank, cabinet, joined, filter;
    private volatile long capacity, afterCrank;
    private String lang;
    private int guiScale;
    private ServerConfig.PowerCosts costs;
    private boolean power, winding, crank;

    DevClientSmokeM67() {
        then(1, () -> {
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            lang = mc.options.languageCode;
            guiScale = mc.options.guiScale().get();
            server(this::setup);
        });
        then(40, () -> {});
        for (String code : java.util.List.of("en_us", "de_de")) {
            then(1, () -> {
                language(code);
                scale(2);
                mc.setScreen(null);
            });
            // the crank at the joined cabinet: the action bar
            then(1, () -> look(cabinet, 0.5, 1.2, -2.2));
            then(10, () -> server(p -> {
                p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CRANK.get()));
                TankBlockEntity c = TankMultiblock.getController(p.level(), tank);
                long before = c.getTank().powerBuffer();
                for (int i = 0; i < 5; i++) {
                    var r = Winding.wind(p.level(), joined, p);
                    if (r != Winding.Result.WOUND) problem("the crank at the joined cabinet: " + r);
                }
                afterCrank = c.getTank().powerBuffer() - before;
            }));
            then(10, () -> {
                if (afterCrank != 5 * org.overengineer.bettacraft.tank.TankStore.fePerClick()) problem("5 clicks put " + afterCrank + " FE into the store");
                shot(code + "_crank");
            });
            // the tank overview and its power tooltip
            near(() -> tank);
            then(1, this::openTank);
            then(25, () -> {
                if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.selectTabForTest(0);
                else problem("no tank screen");
            });
            then(10, () -> shot(code + "_tank_overview"));
            then(1, () -> {
                if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.showTipForTest(org.overengineer.bettacraft.client.screen.TankScreen.POWER_TIP_FOR_TEST);
            });
            then(6, () -> shot(code + "_tank_power_tip"));
            closeAll();
            // the cabinet and the external filter: the same store
            near(() -> cabinet);
            then(1, () -> openBlock(cabinet));
            then(25, () -> {
                if (!(mc.screen instanceof org.overengineer.bettacraft.client.screen.CabinetScreen s)) problem("no cabinet screen: " + mc.screen);
                else if (s.getMenu().energy().capacity() != capacity || s.getMenu().energy().stored() <= 0)
                    problem("the cabinet shows " + s.getMenu().energy().stored() + " / " + s.getMenu().energy().capacity() + ", the store holds " + capacity);
                shot(code + "_cabinet");
            });
            then(1, () -> {
                if (mc.screen instanceof org.overengineer.bettacraft.client.screen.CabinetScreen s) s.showEnergyTipForTest();
            });
            then(6, () -> shot(code + "_cabinet_energy_tip"));
            closeAll();
            near(() -> filter);
            then(1, () -> openBlock(filter));
            then(25, () -> {
                if (!(mc.screen instanceof org.overengineer.bettacraft.client.screen.ExternalFilterScreen s)) problem("no filter screen: " + mc.screen);
                else if (s.getMenu().energy().capacity() != capacity) problem("the filter shows the capacity " + s.getMenu().energy().capacity() + ", the store " + capacity);
                shot(code + "_external_filter");
            });
            then(1, () -> {
                if (mc.screen instanceof org.overengineer.bettacraft.client.screen.ExternalFilterScreen s) s.showEnergyTipForTest();
            });
            then(6, () -> shot(code + "_external_filter_energy_tip"));
            closeAll();
        }
        then(1, () -> {
            language(lang);
            scale(guiScale);
            server(p -> restore());
        });
        then(5, () -> LOGGER.info("[smoketest] M67 done: {} problem(s)", problems));
    }

    int problems() {
        return problems;
    }

    // ------------------------------------------------------------------ the scene

    private void setup(ServerPlayer p) {
        ServerLevel level = p.level();
        costs = ServerConfig.POWER_COSTS.get();
        power = Features.own(Feature.POWER);
        winding = Features.own(Feature.WINDING);
        crank = Features.own(Feature.TANK_CRANK);
        ServerConfig.POWER_COSTS.set(ServerConfig.PowerCosts.EASY);
        Features.set(Feature.POWER, true);
        Features.set(Feature.WINDING, true);
        Features.set(Feature.TANK_CRANK, true);
        org.overengineer.bettacraft.gametest.framework.DevTime.set(level, 6000);
        DevClientWorldTour.clearWeather(level);
        p.getAbilities().flying = true;
        BlockPos base = p.blockPosition().offset(6, 0, 10);
        for (int x = -3; x <= 8; x++) for (int z = -4; z <= 5; z++) {
            level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
            for (int y = 0; y < 6; y++) level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        // four cabinets under the tank, one joined on the right
        for (int x = 0; x < 3; x++) for (int z = 0; z < 2; z++) {
            if (x == 2 && z == 1) continue;
            level.setBlockAndUpdate(base.offset(x, 0, z), ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH));
        }
        for (int x = 0; x < 2; x++) for (int y = 1; y < 3; y++) for (int z = 0; z < 2; z++) {
            BlockPos q = base.offset(x, y, z);
            level.setBlockAndUpdate(q, ModBlocks.TANK.get().defaultBlockState());
        }
        for (int x = 0; x < 2; x++) for (int y = 1; y < 3; y++) for (int z = 0; z < 2; z++) TankMultiblock.onTankBlockAdded(level, base.offset(x, y, z), null);
        TankBlockEntity c = TankMultiblock.getController(level, base.offset(0, 1, 0));
        if (c == null) {
            problem("the tank did not form");
            return;
        }
        TankInteractions.changeWater(level, c, c.getTank().capacityMb(), 0F);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setTargetTemp(28F);
        BlockPos fp = base.offset(-1, 1, 1);
        level.setBlockAndUpdate(fp, ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, Direction.EAST));
        if (level.getBlockEntity(fp) instanceof ExternalFilterBlockEntity f) f.insertHeater(new ItemStack(ModItems.HEATER_SMALL.get()));
        level.setBlockAndUpdate(base.offset(0, 3, 0), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.LIT, true));
        org.overengineer.bettacraft.tank.TankEquipment.invalidate(c);
        TankCare.care(level, c, 20);
        capacity = TankPower.capacity(c);
        c.getTank().setPowerBuffer(capacity * 4 / 10);
        TankCare.care(level, c, 20);
        tank = c.getBlockPos();
        cabinet = base.offset(0, 0, 0);
        joined = base.offset(2, 0, 0);
        filter = fp;
        LOGGER.info("[smoketest] M67 tank {}: store {} FE ({} cabinets), demand {} FE/t on EASY", tank, capacity,
                org.overengineer.bettacraft.tank.TankStore.cabinets(level, c).size(), c.lastDemand());
        if (org.overengineer.bettacraft.tank.TankStore.cabinets(level, c).size() != 5) problem("the store has " + org.overengineer.bettacraft.tank.TankStore.cabinets(level, c).size() + " cabinets, not 4 + 1 joined");
    }

    private void restore() {
        ServerConfig.POWER_COSTS.set(costs);
        Features.set(Feature.POWER, power);
        Features.set(Feature.WINDING, winding);
        Features.set(Feature.TANK_CRANK, crank);
    }

    // ------------------------------------------------------------------ helpers

    private void near(java.util.function.Supplier<BlockPos> at) {
        then(1, () -> look(at.get(), 0.5, 2.5, -3.5));
        then(6, () -> {});
    }

    private void closeAll() {
        then(1, () -> server(ServerPlayer::closeContainer));
        then(3, () -> mc.setScreen(null));
    }

    private void openTank() {
        server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.level(), tank);
            org.overengineer.bettacraft.platform.Platform.get().openMenu(p, new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new org.overengineer.bettacraft.tank.menu.TankMenu(id, inv, c),
                    Component.translatable("gui.bettacraft.tank.title")), buf -> buf.writeBlockPos(c.getBlockPos()));
        });
    }

    private void openBlock(BlockPos at) {
        server(p -> {
            if (p.level().getBlockEntity(at) instanceof net.minecraft.world.MenuProvider mp) {
                org.overengineer.bettacraft.platform.Platform.get().openMenu(p, mp, buf -> buf.writeBlockPos(at));
            } else {
                LOGGER.error("[smoketest] M67 no menu at {}", at);
            }
        });
    }

    private void language(String code) {
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
        if (mc.screen != null) mc.screen.resize(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    private void scale(int s) {
        mc.options.guiScale().set(s);
        mc.resizeGui();
    }

    private void problem(String what) {
        problems++;
        LOGGER.error("[smoketest] M67 FAIL: {}", what);
    }

    private void look(BlockPos at, double dx, double dy, double dz) {
        Vec3 c = Vec3.atCenterOf(at);
        Vec3 eye = c.add(dx, dy, dz);
        Vec3 d = c.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
        server(p -> p.teleportTo(p.level(), eye.x, eye.y - p.getEyeHeight(), eye.z, java.util.Set.of(), yaw, pitch, false));
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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m67_" + name + ".png", mc.getMainRenderTarget(), 1, c -> {});
    }
}
