package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.co2.BioCo2Block;
import org.overengineer.bettacraft.co2.BioCo2BlockEntity;
import org.overengineer.bettacraft.co2.Co2CylinderBlock;
import org.overengineer.bettacraft.co2.Co2CylinderBlockEntity;
import org.overengineer.bettacraft.co2.Co2StationBlockEntity;
import org.overengineer.bettacraft.osmosis.RoUnitBlock;
import org.overengineer.bettacraft.osmosis.RoUnitBlockEntity;
import org.overengineer.bettacraft.plumbing.PipeBlock;
import org.overengineer.bettacraft.progression.GuideBook;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M25 part b dev client smoke scene ({@code -Psmoketestm25}): a tank with a CO2 cylinder (solenoid valve) on one side and
 * a fermenting bio CO2 bottle on the other, a CO2 filling station on a burning furnace with a cylinder in it, a reverse
 * osmosis unit next to a water source with a glass pipe to the tank. Screenshots of the scene, the regulator screen of
 * the cylinder (en + de) and the handbook pages (CO2, reverse osmosis unit, water as a fluid; en + de). Checks (errors in
 * the log): the cylinder and the bottle serve the tank, the station fills, the unit runs. Screenshots {@code bettacraft_m25_*}.
 */
final class DevClientSmokeM25 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final BlockPos base;
    private final String lang = Minecraft.getInstance().options.languageCode;
    private int gui;

    DevClientSmokeM25() {
        base = mc.player.blockPosition().offset(-10, 0, 680);
        then(1, () -> server(this::build));
        then(20, () -> {});
        then(1, () -> mc.options.hideGui = true);
        then(1, () -> look(base.getX() + 2.5, base.getY() + 3.2, base.getZ() - 5.0, 0F, 22F));
        then(160, () -> {
            check();
            shot("scene");
        });
        then(1, () -> mc.options.hideGui = false);
        then(1, () -> server(p -> p.teleportTo(p.level(), base.getX() + 5.5, base.getY() + 1.0, base.getZ() - 1.5, java.util.Set.of(), 0F, 20F, false)));
        then(20, () -> org.overengineer.bettacraft.util.ClientHooks.openRegulator(base.offset(5, 0, 0)));
        then(20, () -> {
            if (!(mc.screen instanceof org.overengineer.bettacraft.client.screen.RegulatorScreen)) LOGGER.error("[smoketest] M25 no regulator screen for the cylinder");
        });
        then(10, () -> shot("cylinder_regulator"));
        then(1, () -> quickLanguage("de_de"));
        then(20, () -> shot("cylinder_regulator_de"));
        then(1, () -> quickLanguage(lang));
        then(1, () -> mc.setScreen(null));
        guide("en");
        then(1, () -> quickLanguage("de_de"));
        guide("de");
        then(1, () -> quickLanguage(lang));
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        for (int x = -14; x <= 20; x++)
            for (int z = -12; z <= 16; z++) {
                level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
                for (int y = 0; y < 5; y++) level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
        TankBlockEntity c = tank(level, p, base.offset(2, 0, 0), 3, 2, 2);
        // CO2 cylinder with a solenoid valve on the right, a bio CO2 bottle on the left
        level.setBlockAndUpdate(base.offset(5, 0, 0), ModBlocks.CO2_CYLINDER.get().defaultBlockState()
                .setValue(Co2CylinderBlock.FACING, Direction.NORTH).setValue(Co2CylinderBlock.SOLENOID, true));
        if (level.getBlockEntity(base.offset(5, 0, 0)) instanceof Co2CylinderBlockEntity cyl) {
            cyl.setGas(Co2CylinderBlockEntity.capacity() * 0.8F);
            cyl.setPower(3);
        }
        level.setBlockAndUpdate(base.offset(1, 0, 0), ModBlocks.BIO_CO2.get().defaultBlockState().setValue(BioCo2Block.FACING, Direction.NORTH));
        if (level.getBlockEntity(base.offset(1, 0, 0)) instanceof BioCo2BlockEntity bio) bio.start(level.getGameTime() - 24000L);
        TankEquipment.invalidate(c);
        // the filling station on a burning furnace
        level.setBlockAndUpdate(base.offset(8, 0, 0), Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.LIT, true));
        level.setBlockAndUpdate(base.offset(8, 1, 0), ModBlocks.CO2_STATION.get().defaultBlockState());
        if (level.getBlockEntity(base.offset(8, 1, 0)) instanceof Co2StationBlockEntity st) st.items().setStackInSlot(0, new ItemStack(ModItems.CO2_CYLINDER.get()));
        // the reverse osmosis unit next to a water source, a glass pipe to the tank
        // a walled water source (it must not flood the scene)
        for (BlockPos w : List.of(base.offset(-2, 0, 2), base.offset(-1, 0, 1), base.offset(-1, 0, 3)))
            level.setBlockAndUpdate(w, Blocks.STONE_BRICKS.defaultBlockState());
        level.setBlockAndUpdate(base.offset(-1, 0, 2), Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(base.offset(0, 0, 2), ModBlocks.RO_UNIT.get().defaultBlockState().setValue(RoUnitBlock.FACING, Direction.NORTH));
        if (level.getBlockEntity(base.offset(0, 0, 2)) instanceof RoUnitBlockEntity ro) {
            ro.parts().setStackInSlot(RoUnitBlockEntity.MEMBRANE, new ItemStack(ModItems.RO_MEMBRANE.get()));
            ro.parts().setStackInSlot(RoUnitBlockEntity.PREFILTER, new ItemStack(ModItems.RO_PREFILTER.get()));
        }
        level.setBlockAndUpdate(base.offset(1, 0, 2), ModBlocks.GLASS_PIPE.get().defaultBlockState());
        level.setBlockAndUpdate(base.offset(1, 0, 1), ModBlocks.GLASS_PIPE.get().defaultBlockState());
        for (BlockPos pp : List.of(base.offset(1, 0, 2), base.offset(1, 0, 1))) {
            var s = level.getBlockState(pp);
            if (s.getBlock() instanceof PipeBlock) level.setBlockAndUpdate(pp, Block.updateFromNeighbourShapes(s, level, pp));
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

    private void check() {
        server(p -> {
            ServerLevel level = p.level();
            TankBlockEntity c = TankMultiblock.getController(level, base.offset(2, 0, 0));
            int n = c == null ? 0 : TankEquipment.co2Sources(level, c).size();
            if (n != 2) LOGGER.error("[smoketest] M25 the tank has {} CO2 sources, expected 2", n);
            if (level.getBlockEntity(base.offset(8, 1, 0)) instanceof Co2StationBlockEntity st
                    && org.overengineer.bettacraft.co2.Co2CylinderItem.gas(st.items().getStackInSlot(0)) <= 0)
                LOGGER.error("[smoketest] M25 the filling station did not fill");
            if (level.getBlockEntity(base.offset(0, 0, 2)) instanceof RoUnitBlockEntity ro && ro.pure() <= 0 && ro.lastRate() <= 0)
                LOGGER.error("[smoketest] M25 the reverse osmosis unit did not run: {}", ro.stopReason());
            LOGGER.info("[smoketest] M25 CO2 {} mg/L, {} bubbles/s", c == null ? -1 : c.getTank().co2(), c == null ? -1 : c.co2Bubbles);
        });
    }

    // ------------------------------------------------------------------ handbook

    private void guide(String l) {
        if (!GuideBook.available()) {
            LOGGER.warn("[smoketest] M25 GuideME not loaded, no handbook pictures");
            return;
        }
        then(1, () -> {
            gui = mc.options.guiScale().get();
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
        });
        for (String page : new String[]{"equipment/co2.md", "equipment/reverse_osmosis.md", "aquarium/water_fluids.md"}) {
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

    private void quickLanguage(String code) {
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
        if (mc.screen != null) mc.screen.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
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
        mc.getToasts().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m25_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
