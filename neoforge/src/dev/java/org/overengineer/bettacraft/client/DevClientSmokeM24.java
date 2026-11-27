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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.crucible.CrucibleBlockEntity;
import org.overengineer.bettacraft.filter.FilterChamberBlockEntity;
import org.overengineer.bettacraft.filter.FilterPartBlock;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.plumbing.PipeBlock;
import org.overengineer.bettacraft.plumbing.ReservoirBlockEntity;
import org.overengineer.bettacraft.plumbing.WaterPumpBlock;
import org.overengineer.bettacraft.plumbing.interior.InteriorPlumbing;
import org.overengineer.bettacraft.plumbing.interior.PlumbingBlock;
import org.overengineer.bettacraft.plumbing.interior.PlumbingItem;
import org.overengineer.bettacraft.progression.GuideBook;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M24 dev client smoke scene ({@code -Psmoketestm24}): three tanks. Left: a multiblock filter of four chambers (sponge,
 * ceramic, carbon, peat) with pump head, inlet and outlet, a level sensor with a reservoir and a water change controller.
 * Middle: raised on stainless steel pipes, two bulkheads with a water pump between them (strainer out, nozzle, lily pipe
 * and spray bar in), the interior pipes inside. Right: the crucible glowing on a magma block, glass pipes. Screenshots of
 * the scene, the pipes under the tank, the interior with the outlets running, the filter, water changer and crucible
 * screens, the water tab with nitrate (en + de), the items and the handbook pages (en + de). Checks (errors in the log):
 * the outlets run, the filter forms and serves its tank, the client sees the pipe connections and the active pump head.
 * Screenshots {@code bettacraft_m24_*}.
 */
final class DevClientSmokeM24 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static final int SPACING = 8;

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final BlockPos base;
    private volatile InteriorRegion pipeRegion;
    private final String lang = Minecraft.getInstance().options.languageCode;
    private CompletableFuture<Void> reload;
    private int gui;

    DevClientSmokeM24() {
        base = mc.player.blockPosition().offset(-10, 0, 620);
        then(1, () -> server(this::build));
        then(20, () -> {});
        then(1, () -> mc.options.hideGui = true);
        // 1. the whole scene and each part
        then(1, () -> look(base.getX() + SPACING + 2.0, base.getY() + 6.5, base.getZ() - 12.0, 0F, 22F));
        then(160, () -> {
            check();
            shot("scene");
        });
        then(1, () -> look(base.getX() + 6.5, base.getY() + 4.2, base.getZ() - 4.5, -35F, 22F));
        then(60, () -> shot("multiblock_filter"));
        then(1, () -> look(base.getX() + SPACING + 2.0, base.getY() + 1.4, base.getZ() - 3.0, 0F, 8F));
        then(60, () -> shot("pipes_under"));
        then(1, () -> look(base.getX() + SPACING + 2.0, base.getY() + 3.6, base.getZ() - 2.6, 0F, 10F));
        then(60, () -> shot("pipes_miniature"));
        then(1, () -> look(base.getX() + 2 * SPACING + 3.5, base.getY() + 2.6, base.getZ() - 2.5, -35F, 30F));
        then(60, () -> shot("crucible"));
        then(1, () -> look(base.getX() - 3.0, base.getY() + 3.0, base.getZ() - 2.5, -40F, 30F));
        then(60, () -> shot("top_off"));
        // M68 part B: the controller close up from the front left: its port stubs (fresh in north, waste out south into the drain, waste in on top)
        then(1, () -> look(base.getX() - 3.0, base.getY() + 2.9, base.getZ() + 4.4, -128F, 32F));
        then(60, () -> shot("water_changer_ports"));
        // 2. inside the middle tank: the nozzle, the lily pipe and the spray bar run
        then(1, () -> server(p -> {
            InteriorRegion r = pipeRegion;
            if (r == null) return;
            BlockPos at = r.toWorld(r.scale() + r.scale() / 2 + 4, 6, r.scale() + r.scale() / 2 - 10);
            p.teleportTo(InteriorManager.interiorLevel(p.level().getServer()), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, java.util.Set.of(), 22F, -18F, true);
        }));
        then(120, () -> {
            checkInterior();
            shot("interior");
        });
        then(1, () -> server(p -> p.teleportTo(p.level().getServer().overworld(), base.getX() + 6.0, base.getY() + 1.5, base.getZ() - 2.0, java.util.Set.of(), 0F, 0F, true)));
        then(5, () -> server(p -> p.teleportTo(p.level(), base.getX() + 4.5, base.getY() + 1.0, base.getZ() - 1.5, java.util.Set.of(), 0F, 20F, false)));
        // 3. screens
        then(60, () -> mc.options.hideGui = false);
        then(1, () -> server(this::fillInventory));
        then(40, () -> mc.getToastManager().clear());
        then(1, () -> org.overengineer.bettacraft.util.ClientHooks.openFilter(base.offset(5, 3, 0)));
        then(20, () -> {
            if (!(mc.screen instanceof org.overengineer.bettacraft.client.screen.MultiblockFilterScreen)) LOGGER.error("[smoketest] M24 no filter screen");
            mouseTo(-50, -50);
        });
        then(10, () -> shot("filter_screen"));
        then(1, () -> quickLanguage("de_de"));
        then(20, () -> shot("filter_screen_de"));
        then(1, () -> quickLanguage(lang));
        then(1, () -> mc.setScreen(null));
        then(5, () -> org.overengineer.bettacraft.util.ClientHooks.openWaterChanger(base.offset(-1, 1, 2)));
        then(20, () -> {
            if (!(mc.screen instanceof org.overengineer.bettacraft.client.screen.WaterChangerScreen)) LOGGER.error("[smoketest] M24 no water changer screen");
            mouseTo(-50, -50);
        });
        then(10, () -> shot("water_changer"));
        then(1, () -> quickLanguage("de_de"));
        then(20, () -> shot("water_changer_de"));
        then(1, () -> quickLanguage(lang));
        then(1, () -> mc.setScreen(null));
        then(1, () -> server(p -> p.teleportTo(p.level(), base.getX() + 2 * SPACING + 5.5, base.getY() + 1.0, base.getZ() - 2.0, java.util.Set.of(), 0F, 20F, false)));
        then(10, () -> server(p -> {
            BlockPos cp = base.offset(2 * SPACING + 5, 1, 1);
            if (p.level().getBlockEntity(cp) instanceof net.minecraft.world.MenuProvider mp) org.overengineer.bettacraft.platform.Platform.get().openMenu(p, mp, cp);
        }));
        then(30, () -> {
            if (!(mc.screen instanceof org.overengineer.bettacraft.client.screen.CrucibleScreen)) LOGGER.error("[smoketest] M24 no crucible menu");
            mouseTo(-50, -50);
        });
        then(10, () -> shot("crucible_menu"));
        then(1, () -> quickLanguage("de_de"));
        then(20, () -> shot("crucible_menu_de"));
        then(1, () -> quickLanguage(lang));
        then(1, () -> server(ServerPlayer::closeContainer));
        then(5, () -> mc.setScreen(null));
        then(1, () -> server(p -> p.teleportTo(p.level(), base.getX() + 2.0, base.getY() + 1.0, base.getZ() - 2.0, java.util.Set.of(), 0F, 20F, false)));
        tankMenu("menu_water", base.offset(0, 1, 0));
        // 4. the items with their tooltips
        then(5, () -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));
        then(20, () -> hover(s -> s.is(ModItems.FILTER_FOAM.get())));
        then(10, () -> shot("items"));
        then(5, () -> mc.setScreen(null));
        // 5. the handbook
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
        for (int x = -8; x < 3 * SPACING + 6; x++)
            for (int z = -14; z < 9; z++) {
                level.setBlock(base.offset(x, -1, z), Blocks.SMOOTH_QUARTZ.defaultBlockState(), 2);
                level.setBlock(base.offset(x, 0, z), Blocks.SMOOTH_QUARTZ.defaultBlockState(), 2);
                for (int y = 1; y < 14; y++) level.setBlock(base.offset(x, y, z), z == 7 ? Blocks.WHITE_CONCRETE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
            }
        ServerLevel in = InteriorManager.interiorLevel(p.level().getServer());
        // ---- left: the multiblock filter, level sensor, reservoir, water change controller
        TankBlockEntity a = tank(level, p, base.offset(0, 1, 0), 4, 2, 3, 900);
        a.getTank().setNitrate(34F);
        int[][] chambers = {{5, 1, 0}, {5, 2, 0}, {5, 1, 1}, {5, 2, 1}};
        net.minecraft.world.item.Item[] media = {ModItems.FILTER_FOAM.get(), ModItems.CERAMIC_RINGS.get(), ModItems.ACTIVATED_CARBON.get(), ModItems.FILTER_PEAT.get()};
        for (int i = 0; i < chambers.length; i++) {
            BlockPos cp = base.offset(chambers[i][0], chambers[i][1], chambers[i][2]);
            level.setBlockAndUpdate(cp, ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
            if (level.getBlockEntity(cp) instanceof FilterChamberBlockEntity ch) {
                ItemStack m = new ItemStack(media[i]);
                if (i == 0) m.setDamageValue(450); // a sponge half clogged
                ch.setMedium(m);
            }
        }
        level.setBlockAndUpdate(base.offset(5, 3, 0), ModBlocks.FILTER_PUMP_HEAD.get().defaultBlockState());
        level.setBlockAndUpdate(base.offset(4, 1, 0), ModBlocks.FILTER_INLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.WEST));
        level.setBlockAndUpdate(base.offset(4, 2, 1), ModBlocks.FILTER_OUTLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.WEST));
        level.setBlockAndUpdate(base.offset(-1, 1, 0), ModBlocks.LEVEL_SENSOR.get().defaultBlockState().setValue(org.overengineer.bettacraft.plumbing.WaterDeviceBlock.FACING, Direction.EAST));
        level.setBlockAndUpdate(base.offset(-2, 1, 0), ModBlocks.RESERVOIR.get().defaultBlockState());
        if (level.getBlockEntity(base.offset(-2, 1, 0)) instanceof ReservoirBlockEntity r)
            r.water().fill(new org.overengineer.bettacraft.platform.fluid.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 9000), IFluidHandler.FluidAction.EXECUTE);
        level.setBlockAndUpdate(base.offset(-1, 1, 2), ModBlocks.WATER_CHANGE_CONTROLLER.get().defaultBlockState().setValue(org.overengineer.bettacraft.plumbing.WaterDeviceBlock.FACING, Direction.WEST));
        // M68 part B: fresh water through a steel pipe at its inlet (north, kept off the glass) from a reservoir, the old water out of its
        // outlet (south) into a drain
        level.setBlockAndUpdate(base.offset(-1, 1, 1), ModBlocks.STAINLESS_STEEL_PIPE.get().defaultBlockState());
        org.overengineer.bettacraft.plumbing.PipeWrenchItem.setClosed(level, base.offset(-1, 1, 1), -1, Direction.EAST, true);
        level.setBlockAndUpdate(base.offset(-1, 1, 3), ModBlocks.DRAIN.get().defaultBlockState());
        level.setBlockAndUpdate(base.offset(-2, 1, 1), ModBlocks.RESERVOIR.get().defaultBlockState());
        if (level.getBlockEntity(base.offset(-2, 1, 1)) instanceof ReservoirBlockEntity r2)
            r2.water().fill(new org.overengineer.bettacraft.platform.fluid.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 14000), IFluidHandler.FluidAction.EXECUTE);
        // ---- middle: raised on steel pipes, two bulkheads, a pump between them
        BlockPos mb = base.offset(SPACING, 2, 0);
        TankBlockEntity b = tank(level, p, mb, 4, 2, 3, 850);
        InteriorRegion rb = InteriorManager.ensureRegion(level, b);
        pipeRegion = rb;
        PlumbingItem.installFromOutside(level, mb.offset(1, 0, 1), null, ItemStack.EMPTY);
        PlumbingItem.installFromOutside(level, mb.offset(2, 0, 1), null, ItemStack.EMPTY);
        // under the tank: pipes at y 0 and 1, the pump under the nozzle bulkhead pulls from the strainer bulkhead
        level.setBlockAndUpdate(base.offset(SPACING + 1, 0, 1), ModBlocks.STAINLESS_STEEL_PIPE.get().defaultBlockState());
        level.setBlockAndUpdate(base.offset(SPACING + 2, 0, 1), ModBlocks.STAINLESS_STEEL_PIPE.get().defaultBlockState());
        level.setBlockAndUpdate(base.offset(SPACING + 2, 1, 1), ModBlocks.STAINLESS_STEEL_PIPE.get().defaultBlockState());
        level.setBlockAndUpdate(base.offset(SPACING + 1, 1, 1), ModBlocks.WATER_PUMP.get().defaultBlockState().setValue(WaterPumpBlock.FACING, Direction.UP));
        // stands at the corners
        for (int[] c : new int[][]{{0, 0}, {3, 0}, {0, 2}, {3, 2}}) level.setBlockAndUpdate(base.offset(SPACING + c[0], 1, c[1]), ModBlocks.STAINLESS_STEEL_PIPE.get().defaultBlockState());
        // inside: from the nozzle bulkhead (cell 1,1) pipes up to a lily pipe, a nozzle and a spray bar; a strainer at the other one
        int s = rb.scale();
        var plumbing = InteriorManager.equipment(p.level().getServer(), rb).plumbing();
        long port1 = InteriorPlumbing.portInCell(plumbing, 1, 1, s), port2 = InteriorPlumbing.portInCell(plumbing, 2, 1, s);
        if (port1 == Long.MIN_VALUE || port2 == Long.MIN_VALUE) LOGGER.error("[smoketest] M24 the bulkheads were not fitted");
        int px = BlockPos.getX(port1), pz = BlockPos.getZ(port1), qx = BlockPos.getX(port2), qz = BlockPos.getZ(port2);
        for (int y = 1; y <= 22; y++) in.setBlock(rb.toWorld(px, y, pz), ModBlocks.INTERIOR_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
        in.setBlock(rb.toWorld(px, 23, pz), ModBlocks.LILY_PIPE.get().defaultBlockState().setValue(PlumbingBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
        in.setBlock(rb.toWorld(px + 1, 5, pz), ModBlocks.NOZZLE.get().defaultBlockState().setValue(PlumbingBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        in.setBlock(rb.toWorld(px - 1, 12, pz), ModBlocks.SPRAY_BAR.get().defaultBlockState().setValue(PlumbingBlock.FACING, Direction.WEST), Block.UPDATE_ALL);
        in.setBlock(rb.toWorld(qx, 1, qz), ModBlocks.INTAKE_STRAINER.get().defaultBlockState(), Block.UPDATE_ALL);
        // re-set the interior pipes so that they show their joints
        for (int y = 1; y <= 22; y++) {
            BlockPos ip = rb.toWorld(px, y, pz);
            var st = in.getBlockState(ip);
            for (Direction d : Direction.values()) st = st.setValue(PipeBlock.SIDES.get(d), in.getBlockState(ip.relative(d)).getBlock() instanceof PlumbingBlock);
            in.setBlock(ip, st, Block.UPDATE_ALL);
        }
        InteriorPlants.plantAt(in, rb, PlantSpecies.VALLISNERIA, 54, 0, 34, 6, 5L);
        InteriorPlants.plantAt(in, rb, PlantSpecies.AMAZON_SWORD, 10, 0, 36, 6, 6L);
        // ---- right: the crucible on a magma block, glass pipes from a reservoir to the tank
        TankBlockEntity cTank = tank(level, p, base.offset(2 * SPACING, 1, 0), 4, 2, 3, 900);
        level.setBlockAndUpdate(base.offset(2 * SPACING + 5, 0, 1), Blocks.MAGMA_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(base.offset(2 * SPACING + 5, 1, 1), ModBlocks.CRUCIBLE.get().defaultBlockState());
        if (level.getBlockEntity(base.offset(2 * SPACING + 5, 1, 1)) instanceof CrucibleBlockEntity cr) {
            cr.items().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 60));
            cr.items().setStackInSlot(1, new ItemStack(Items.COAL, 20));
            cr.items().setStackInSlot(2, new ItemStack(Items.QUARTZ, 20));
            cr.items().setStackInSlot(3, new ItemStack(Items.IRON_INGOT, 20));
            cr.items().setStackInSlot(CrucibleBlockEntity.OUTPUT, new ItemStack(ModItems.STAINLESS_STEEL_INGOT.get(), 6));
        }
        level.setBlockAndUpdate(base.offset(2 * SPACING + 4, 1, 2), ModBlocks.GLASS_PIPE.get().defaultBlockState());
        level.setBlockAndUpdate(base.offset(2 * SPACING + 5, 1, 2), ModBlocks.WATER_PUMP.get().defaultBlockState().setValue(WaterPumpBlock.FACING, Direction.WEST));
        level.setBlockAndUpdate(base.offset(2 * SPACING + 6, 1, 2), ModBlocks.GLASS_PIPE.get().defaultBlockState());
        level.setBlockAndUpdate(base.offset(2 * SPACING + 6, 1, 3), ModBlocks.GLASS_PIPE.get().defaultBlockState());
        level.setBlockAndUpdate(base.offset(2 * SPACING + 6, 1, 4), ModBlocks.RESERVOIR.get().defaultBlockState());
        if (level.getBlockEntity(base.offset(2 * SPACING + 6, 1, 4)) instanceof ReservoirBlockEntity r3)
            r3.water().fill(new org.overengineer.bettacraft.platform.fluid.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 16000), IFluidHandler.FluidAction.EXECUTE);
        TankInteractions.changeWater(level, cTank, -3000);
        LOGGER.info("[smoketest] M24 scene at {}", base);
    }

    private TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, int sy, int sz, int perBlock) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, (long) sx * sy * sz * perBlock, 0F);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setOwner(p.getUUID());
        c.getTank().setWaterTemp(25F);
        c.getTank().setTargetTemp(25F);
        c.markTankChanged();
        return c;
    }

    /** The filter formed and serves its tank, the pipes join, the pump head runs on the client too. */
    private void check() {
        server(p -> {
            ServerLevel level = p.level();
            if (level.getBlockEntity(base.offset(5, 3, 0)) instanceof org.overengineer.bettacraft.filter.FilterPumpHeadBlockEntity h) {
                org.overengineer.bettacraft.gametest.FilterLinesGameTests.guardAll(level, h.getBlockPos()); // M40: its guards
                h.run(level, true);
                if (!h.problem().equals(org.overengineer.bettacraft.filter.MultiblockFilter.Problem.NONE) || h.servedTank() == null)
                    LOGGER.error("[smoketest] M24 the multiblock filter does not serve its tank: {} {}", h.problem(), h.servedTank());
                else LOGGER.info("[smoketest] M24 multiblock filter: {} chambers, {} mB/t, cleans {}", h.chambers(), h.flow(), h.cleanBlocks());
            }
            TankBlockEntity b = TankMultiblock.getController(level, base.offset(SPACING, 2, 0));
            int active = b == null ? 0 : InteriorPlumbing.activeOutlets(level, b);
            if (active < 4) LOGGER.error("[smoketest] M24 {} running outlets in the middle tank (want 4)", active);
            else LOGGER.info("[smoketest] M24 {} outlets run", active);
        });
        if (mc.level == null) return;
        var under = mc.level.getBlockState(base.offset(SPACING + 2, 1, 1));
        if (!(under.getBlock() instanceof PipeBlock) || !under.getValue(PipeBlock.SIDES.get(Direction.UP)) || !under.getValue(PipeBlock.SIDES.get(Direction.DOWN)))
            LOGGER.error("[smoketest] M24 the pipe under the tank does not join up and down: {}", under);
        var head = mc.level.getBlockState(base.offset(5, 3, 0));
        if (!head.hasProperty(FilterPartBlock.ACTIVE) || !head.getValue(FilterPartBlock.ACTIVE)) LOGGER.error("[smoketest] M24 the client pump head is not active");
        var lit = mc.level.getBlockState(base.offset(2 * SPACING + 5, 1, 1));
        if (!lit.hasProperty(org.overengineer.bettacraft.crucible.CrucibleBlock.LIT) || !lit.getValue(org.overengineer.bettacraft.crucible.CrucibleBlock.LIT))
            LOGGER.error("[smoketest] M24 the crucible does not glow on the client");
    }

    /** Inside: the nozzle shows its stream (ACTIVE on the client). */
    private void checkInterior() {
        InteriorRegion r = pipeRegion;
        if (r == null || mc.level == null) return;
        int s = r.scale();
        var st = mc.level.getBlockState(r.toWorld(s + s / 2 + 1, 5, s + s / 2)); // the bulkhead sits in the middle of its block
        if (!(st.getBlock() instanceof PlumbingBlock) || !st.getValue(PlumbingBlock.ACTIVE)) LOGGER.error("[smoketest] M24 the client nozzle does not run: {}", st);
        else LOGGER.info("[smoketest] M24 the nozzle runs inside the tank");
    }

    private void fillInventory(ServerPlayer p) {
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.getAbilities().mayfly = true;
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.getInventory().clearContent();
        ItemStack foam = new ItemStack(ModItems.FILTER_FOAM.get());
        foam.setDamageValue(620);
        ItemStack[] hotbar = {new ItemStack(ModItems.STAINLESS_STEEL_INGOT.get(), 12), new ItemStack(ModItems.GLASS_PIPE.get(), 16),
                new ItemStack(ModItems.STAINLESS_STEEL_PIPE.get(), 16), new ItemStack(ModItems.WATER_PUMP.get()), new ItemStack(ModItems.BULKHEAD.get()),
                new ItemStack(ModItems.NOZZLE.get(), 4), foam, new ItemStack(ModItems.CERAMIC_RINGS.get()), new ItemStack(ModItems.FILTER_PUMP_HEAD.get())};
        for (int i = 0; i < hotbar.length; i++) p.getInventory().setItem(i, hotbar[i]);
        ItemStack[] more = {new ItemStack(ModItems.CRUCIBLE.get()), new ItemStack(ModItems.FILTER_CHAMBER.get(), 4), new ItemStack(ModItems.FILTER_INLET.get()),
                new ItemStack(ModItems.FILTER_OUTLET.get()), new ItemStack(ModItems.ACTIVATED_CARBON.get()), new ItemStack(ModItems.FILTER_PEAT.get()),
                new ItemStack(ModItems.GLASS_PIPE.get(), 32), new ItemStack(ModItems.INTAKE_STRAINER.get()), new ItemStack(ModItems.LILY_PIPE.get()),
                new ItemStack(ModItems.SPRAY_BAR.get()), new ItemStack(ModItems.RESERVOIR.get()), new ItemStack(ModItems.LEVEL_SENSOR.get()),
                new ItemStack(ModItems.WATER_CHANGE_CONTROLLER.get())};
        for (int i = 0; i < more.length; i++) p.getInventory().setItem(9 + i, more[i]);
        p.inventoryMenu.broadcastChanges();
    }

    private void tankMenu(String name, BlockPos tankPos) {
        then(10, () -> server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.level(), tankPos);
            if (c == null) return;
            org.overengineer.bettacraft.platform.Platform.get().openMenu(p, new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new org.overengineer.bettacraft.tank.menu.TankMenu(id, inv, c),
                    Component.translatable("gui.bettacraft.tank.title")), buf -> buf.writeBlockPos(c.getBlockPos()));
        }));
        then(30, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.selectTabForTest(5);
            else LOGGER.error("[smoketest] M24 no tank menu for {}", name);
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
            LOGGER.warn("[smoketest] M24 GuideME not loaded, no handbook pictures");
            return;
        }
        then(1, () -> {
            gui = mc.options.guiScale().get();
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
        });
        for (String page : new String[]{"equipment/crucible.md", "equipment/pipes.md", "equipment/bulkhead.md", "equipment/multiblock_filter.md",
                "equipment/filter_media.md", "equipment/water_change.md", "equipment/top_off.md", "aquarium/nitrate.md"}) {
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
        LOGGER.warn("[smoketest] M24 item to hover not found");
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
            LOGGER.info("[smoketest] M24 resources reloaded for {}", mc.options.languageCode);
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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m24_" + name + ".png", mc.getMainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
