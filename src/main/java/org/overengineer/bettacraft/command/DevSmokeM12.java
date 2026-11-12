package org.overengineer.bettacraft.command;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.level.block.Blocks;
import org.overengineer.bettacraft.compat.info.InfoData;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.plants.FloatingPlant;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tank.plants.PlantSurface;
import org.overengineer.bettacraft.tank.plants.TankPlants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M12 part of the dedicated-server smoke test: the dev world tank (saved before M12) loads with schema 7 and no or its
 * old plants; a fresh 3x1x3 tank gets one plant of every species, which drift for the rest of the run in the real care
 * loop and must still float over its water; the four items, the trades and the info line are there.
 */
final class DevSmokeM12 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static double[] start;

    private DevSmokeM12() {}

    private static BlockPos tankPos(BlockPos base) {
        return base.offset(0, 0, 22);
    }

    static void setup(ServerLevel level, BlockPos base) {
        BlockPos p = tankPos(base);
        for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) {
            BlockPos q = p.offset(x, 0, z);
            if (!level.getBlockState(q).isAir()) level.setBlockAndUpdate(q, Blocks.AIR.defaultBlockState());
        }
        for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) {
            BlockPos q = p.offset(x, 0, z);
            level.setBlockAndUpdate(q, ModBlocks.TANK.get().defaultBlockState());
            TankMultiblock.onTankBlockAdded(level, q, null);
        }
        TankBlockEntity c = TankMultiblock.getController(level, p);
        if (c == null) throw new IllegalStateException("M12: plant tank did not form");
        TankInteractions.changeWater(level, c, 9000);
        FloatingPlantType[] types = FloatingPlantType.values();
        start = new double[types.length * 2];
        for (int i = 0; i < types.length; i++) {
            FloatingPlant pl = TankPlants.add(c, types[i], p.getX() + 0.6 + (i % 3) * 0.9, p.getZ() + 0.6 + (i / 3) * 1.5, 0.6F, i);
            if (pl == null) throw new IllegalStateException("M12: could not add " + types[i]);
            start[i * 2] = pl.x;
            start[i * 2 + 1] = pl.z;
        }
    }

    static void check(ServerLevel level, BlockPos base) {
        TankBlockEntity old = TankMultiblock.getController(level, base);
        if (old == null) throw new IllegalStateException("M12: no dev world tank");
        CompoundTag saved = old.saveWithFullMetadata(level.registryAccess());
        if (saved.getInt("schema") != 7 || !saved.getCompound("tank").contains("plants")) throw new IllegalStateException("M12: saved as " + saved.getInt("schema"));
        TankBlockEntity c = TankMultiblock.getController(level, tankPos(base));
        if (c == null || c.getTank().plants().size() != FloatingPlantType.values().length) {
            throw new IllegalStateException("M12: plant tank lost its plants: " + (c == null ? "no tank" : c.getTank().plants().size()));
        }
        PlantSurface s = TankPlants.surface(c.getTank());
        double moved = 0;
        for (FloatingPlant p : c.getTank().plants()) {
            if (!s.contains(p.x, p.z)) throw new IllegalStateException("M12: plant " + p.type + " left the water at " + p.x + "/" + p.z);
            int i = p.type.ordinal();
            double d = Math.hypot(p.x - start[i * 2], p.z - start[i * 2 + 1]);
            if (p.type == FloatingPlantType.LILY_PAD && d > 1e-9) throw new IllegalStateException("M12: the lily pad moved");
            moved = Math.max(moved, d);
        }
        if (moved <= 0.0) throw new IllegalStateException("M12: no plant drifted");
        for (var e : ModItems.FLOATING_PLANTS.entrySet()) {
            if (e.getValue().get() == null) throw new IllegalStateException("M12: item " + e.getKey() + " missing");
        }
        boolean frogbit = false, red = false;
        for (int lv = 1; lv <= 2; lv++) {
            for (VillagerTrades.ItemListing l : VillagerTrades.WANDERING_TRADER_TRADES.get(lv)) {
                if (l instanceof net.neoforged.neoforge.common.BasicItemListing b) {
                    var offer = b.getOffer(null, level.getRandom());
                    if (offer == null) continue;
                    frogbit |= offer.getResult().is(ModItems.FLOATING_PLANTS.get(FloatingPlantType.FROGBIT).get());
                    red |= offer.getResult().is(ModItems.FLOATING_PLANTS.get(FloatingPlantType.RED_ROOT_FLOATER).get());
                }
            }
        }
        if (!frogbit || !red) throw new IllegalStateException("M12: trades frogbit " + frogbit + ", red root floater " + red);
        CompoundTag info = InfoData.tank(level, tankPos(base));
        if (info == null || info.getInt("plants") != FloatingPlantType.values().length) throw new IllegalStateException("M12: info data " + info);
        LOGGER.info("[smoketest] M12: dev world tank saved as schema 7 ({} plants); plant tank keeps its {} plants over the water, "
                + "drifted up to {} blocks, lily pad fixed; items, trades and info line ok", old.getTank().plants().size(),
                c.getTank().plants().size(), String.format(java.util.Locale.ROOT, "%.2f", moved));
    }
}
