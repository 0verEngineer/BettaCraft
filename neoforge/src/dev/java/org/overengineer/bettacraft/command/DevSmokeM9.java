package org.overengineer.bettacraft.command;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.compat.info.InfoData;
import org.overengineer.bettacraft.compat.info.InfoLines;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity;
import org.overengineer.bettacraft.food.CultureTubBlockEntity;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankPower;
import org.overengineer.bettacraft.tank.TankWater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M9 part of the dedicated-server smoke test: a 2x1x2 tank with a cooler filter and lids, water quality and evaporation
 * on the real server, the FE and fluid capabilities (power stays off by default, so FE is refused), a larva tub and an
 * artemia hatchery with their info lines. The main tank of earlier runs (persistent dev world) loaded as schema v5.
 */
final class DevSmokeM9 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private DevSmokeM9() {}

    static BlockPos tankPos(BlockPos base) {
        return base.offset(0, 0, 8);
    }

    static void setup(ServerLevel level, BlockPos base) {
        BlockPos t = tankPos(base);
        for (int x = 0; x < 2; x++) for (int z = 0; z < 2; z++) {
            if (!level.getBlockState(t.offset(x, 0, z)).is(ModBlocks.TANK.get())) level.setBlockAndUpdate(t.offset(x, 0, z), ModBlocks.TANK.get().defaultBlockState());
        }
        BlockPos f = t.offset(-1, 0, 0);
        level.setBlockAndUpdate(f, ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, Direction.EAST));
        BlockPos tub = t.offset(4, 0, 0), hatchery = t.offset(6, 0, 0);
        level.setBlockAndUpdate(tub, ModBlocks.LARVA_TUB.get().defaultBlockState());
        level.setBlockAndUpdate(hatchery, ModBlocks.ARTEMIA_HATCHERY.get().defaultBlockState());
        level.setBlockAndUpdate(hatchery.above().east(), Blocks.GLOWSTONE.defaultBlockState());
    }

    static void check(ServerLevel level, BlockPos base) {
        BlockPos t = tankPos(base);
        TankBlockEntity c = TankMultiblock.getController(level, t);
        if (c == null) throw new IllegalStateException("M9: tank did not form");
        TankData d = c.getTank();
        if (d.waterMb() < 4000) TankInteractions.changeWater(level, c, 4000 - d.waterMb());
        if (!(level.getBlockEntity(t.offset(-1, 0, 0)) instanceof ExternalFilterBlockEntity f)) throw new IllegalStateException("M9: no filter");
        if (f.coolerCount() == 0) f.insertHeater(new ItemStack(ModItems.COOLER_SMALL.get()));
        TankEquipment.invalidate(c);
        double cool = TankCare.coolerBlocks(level, d);
        // lids of an earlier run (persistent dev world) come off first
        for (int x = 0; x < 2; x++) for (int z = 0; z < 2; z++) level.setBlockAndUpdate(t.offset(x, 1, z), Blocks.AIR.defaultBlockState());
        // quality on the real server, open top, then lids
        d.setWaterQuality(1.0F);
        d.setWaterTemp(25.0F);
        double evapOpen = TankWater.evaporationPerDay(level, d);
        TankWater.step(level, c, 2.0);
        float q = d.waterQuality();
        for (int x = 0; x < 2; x++) for (int z = 0; z < 2; z++) level.setBlockAndUpdate(t.offset(x, 1, z), ModBlocks.TANK_LID.get().defaultBlockState());
        double evapLid = TankWater.evaporationPerDay(level, d);
        long before = d.waterMb();
        TankInteractions.changeWater(level, c, -1000);
        TankInteractions.changeWater(level, c, 1000);
        LOGGER.info("[smoketest] M9 water: quality {} after 2 days (filter {} blocks), evaporation {} mB/day open, {} with lids, water change -> {}",
                q, TankWater.filterBlocks(level, c), evapOpen, evapLid, d.waterQuality());
        if (cool < 16 || q >= 1.0F || evapOpen <= 0 || evapLid != 0 || d.waterQuality() <= q || d.waterMb() != before) {
            throw new IllegalStateException("M9: water care (cooler " + cool + ")");
        }
        // capabilities: FE is refused while power is off (default), water pipes work
        var energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, t, null);
        IFluidHandler fluid = org.overengineer.bettacraft.platform.Platform.get().fluidHandler(level, t, null);
        if (fluid == null || (energy == null) == TankPower.anyRequired()) throw new IllegalStateException("M9: tank capabilities (energy " + energy + ")");
        FluidStack drained = fluid.drain(500, IFluidHandler.FluidAction.EXECUTE);
        int filled = fluid.fill(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() != 500 || filled != 500) throw new IllegalStateException("M9: tank fluid handler " + drained.getAmount() + "/" + filled);
        CompoundTag info = InfoData.tank(level, t);
        if (info == null || (TankWater.qualityEnabled() && !info.contains("quality"))) throw new IllegalStateException("M9: info without quality");
        // larva tub and hatchery
        BlockPos tubPos = t.offset(4, 0, 0), hatchPos = t.offset(6, 0, 0);
        if (!(level.getBlockEntity(tubPos) instanceof CultureTubBlockEntity tub) || !(level.getBlockEntity(hatchPos) instanceof CultureTubBlockEntity hatch)) {
            throw new IllegalStateException("M9: culture tubs missing");
        }
        tub.setWater(1000);
        tub.automation().insertItem(CultureTubBlockEntity.INPUT, new ItemStack(Items.WHEAT, 4), false);
        int larvae = tub.advance(level, 3.0);
        for (int i = 0; i < 4; i++) hatch.automation().insertItem(CultureTubBlockEntity.INPUT, new ItemStack(ModItems.SALT_WATER_BOTTLE.get()), false);
        if (hatch.items().getStackInSlot(CultureTubBlockEntity.INPUT).isEmpty()) {
            hatch.automation().insertItem(CultureTubBlockEntity.INPUT, new ItemStack(ModItems.ARTEMIA_CYSTS.get()), false);
        }
        int nauplii = hatch.advance(level, 1.0);
        LOGGER.info("[smoketest] M9 food breeding: larva tub {} batch(es) ({}), hatchery {} batch(es) ({}), blocked {} / {}", larvae,
                tub.items().getStackInSlot(CultureTubBlockEntity.OUTPUT), nauplii, hatch.items().getStackInSlot(CultureTubBlockEntity.OUTPUT),
                tub.blocker(level), hatch.blocker(level));
        if (InfoLines.culture(InfoData.culture(level, tub)).size() < 3) throw new IllegalStateException("M9: culture info");
        if (tub.items().getStackInSlot(CultureTubBlockEntity.OUTPUT).isEmpty() && tub.blocker(level) == null) throw new IllegalStateException("M9: no larvae");
        // the main tank of earlier runs: migrated to schema v5 with clean or used water
        TankBlockEntity main = TankMultiblock.getController(level, base);
        LOGGER.info("[smoketest] M9 main tank: water quality {}, power {}, last fresh water {}", main.getTank().waterQuality(),
                main.getTank().powerBuffer(), main.getTank().lastFreshWater());
    }
}
