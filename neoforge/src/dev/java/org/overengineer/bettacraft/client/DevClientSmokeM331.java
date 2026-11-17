package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankFluid;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.terrarium.TankClimate;
import org.overengineer.bettacraft.terrarium.TankModeSetting;
import org.overengineer.bettacraft.terrarium.WaterLimit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M33.1 client smoke ({@code -Psmoketestm331}): a 4 x 3 x 3 paludarium (a ramp up to land on the right) set to the fixed
 * type paludarium and filled by a "pipe" (the fluid capability) with far more water than it takes, and a full 3 x 2 x 2
 * aquarium set to terrarium (the water stays). Screenshots {@code bettacraft_m331_*}: the climate tab of the paludarium
 * with the type button and the water limit, the overview of the former aquarium with the button and the hint.
 */
final class DevClientSmokeM331 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private BlockPos palu, wet;

    DevClientSmokeM331() {
        then(1, () -> server(this::build));
        then(150, () -> server(this::fill));
        then(40, () -> menu(palu));
        then(30, () -> tab(8));
        then(40, () -> shot("climate_tab"));
        then(1, () -> mc.setScreen(null));
        then(10, () -> menu(wet));
        then(30, () -> tab(0));
        then(40, () -> shot("overview_tab"));
        then(1, () -> mc.setScreen(null));
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
        t.setWaterTemp(25F);
        t.setTargetTemp(26F);
        c.markTankChanged();
        return c;
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        ServerLevel in = InteriorManager.interiorLevel(p.server);
        palu = p.blockPosition().offset(-8, 2, 220);
        TankBlockEntity pc = tank(level, p, palu, 4, 3, 3);
        InteriorRegion r = InteriorManager.ensureRegion(level, pc);
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) {
            Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.SAND.defaultBlockState(), false);
            int h = x < 26 ? 1 : Math.min(24, 1 + (x - 26));
            for (int y = 0; y < h && h > 1; y++) in.setBlock(r.toWorld(x, y, z), y == h - 1 && h >= 20 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState(), 2);
        }
        pc.getTank().setTankMode(TankModeSetting.PALUDARIUM);
        pc.markTankChanged();
        wet = palu.offset(8, 0, 0);
        TankBlockEntity wc = tank(level, p, wet, 3, 2, 2);
        TankInteractions.changeWater(level, wc, wc.getTank().capacityMb());
        wc.getTank().setTankMode(TankModeSetting.TERRARIUM);
        wc.markTankChanged();
        p.teleportTo(level, palu.getX() + 6.0, palu.getY(), palu.getZ() - 3.0, java.util.Set.of(), 0F, 10F, false);
    }

    private void fill(ServerPlayer p) {
        ServerLevel level = p.level();
        TankBlockEntity pc = TankMultiblock.getController(level, palu);
        TankBlockEntity wc = TankMultiblock.getController(level, wet);
        if (pc == null || wc == null) {
            LOGGER.error("[smoketest] M33.1 tanks missing");
            return;
        }
        TankClimate.refreshMode(level, pc);
        int in = new TankFluid(level, palu, Direction.NORTH).fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 40000), IFluidHandler.FluidAction.EXECUTE);
        long max = WaterLimit.maxWaterMb(level, pc);
        LOGGER.info("[smoketest] M33.1 paludarium took {} of 40000 mB (limit {}), mode {}, land {}", in, max, pc.getTank().terrariumMode(), pc.getTank().landShare());
        if (in != max) LOGGER.error("[smoketest] M33.1 the pipe filled {} instead of the limit {}", in, max);
        TankClimate.step(level, pc, 0.05);
        TankClimate.refreshMode(level, wc);
        TankClimate.step(level, wc, 0.05);
        LOGGER.info("[smoketest] M33.1 the former aquarium: {} with {} mB, room {}", wc.getTank().terrariumMode(), wc.getTank().waterMb(), WaterLimit.room(level, wc));
    }

    private void menu(BlockPos pos) {
        server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.level(), pos);
            if (c == null) return;
            org.overengineer.bettacraft.platform.Platform.get().openMenu(p, new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new org.overengineer.bettacraft.tank.menu.TankMenu(id, inv, c),
                    Component.translatable("gui.bettacraft.tank.title")), buf -> buf.writeBlockPos(c.getBlockPos()));
        });
    }

    private void tab(int tab) {
        if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.selectTabForTest(tab);
        else LOGGER.error("[smoketest] M33.1 no tank menu");
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

    private void server(Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        mc.getToasts().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m331_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
