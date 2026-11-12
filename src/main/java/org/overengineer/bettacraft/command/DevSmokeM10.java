package org.overengineer.bettacraft.command;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M10 part of the dedicated-server smoke test: an L-shaped tank is built block by block on the real server (fresh each
 * run), filled, gets its interior region with the L's cells, and is split by breaking its corner. The interior regions
 * of earlier runs (persistent dev world, saved before M10) load as full boxes.
 */
final class DevSmokeM10 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private DevSmokeM10() {}

    static BlockPos tankPos(BlockPos base) {
        return base.offset(0, 0, 14);
    }

    /** The L: (0..2, z 0) plus (0, z 1..2). */
    private static List<BlockPos> ell(BlockPos t) {
        List<BlockPos> l = new ArrayList<>();
        for (int x = 0; x < 3; x++) l.add(t.offset(x, 0, 0));
        l.add(t.offset(0, 0, 1));
        l.add(t.offset(0, 0, 2));
        return l;
    }

    static void setup(ServerLevel level, BlockPos base) {
        BlockPos t = tankPos(base);
        // fresh every run: remove what an earlier run left (an empty tank breaks freely)
        for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) {
            BlockPos p = t.offset(x, 0, z);
            if (!level.getBlockState(p).isAir()) level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
        }
        for (BlockPos p : ell(t)) {
            level.setBlockAndUpdate(p, ModBlocks.TANK.get().defaultBlockState());
            TankMultiblock.onTankBlockAdded(level, p, null);
        }
        TankBlockEntity c = TankMultiblock.getController(level, t);
        if (c == null) throw new IllegalStateException("M10: the L did not form");
        TankInteractions.changeWater(level, c, 4000);
    }

    static void check(ServerLevel level, BlockPos base) {
        BlockPos t = tankPos(base);
        TankBlockEntity c = TankMultiblock.getController(level, t);
        if (c == null) throw new IllegalStateException("M10: no controller for the L");
        TankData d = c.getTank();
        for (BlockPos p : ell(t)) if (TankMultiblock.getController(level, p) != c) throw new IllegalStateException("M10: L block " + p + " not joined");
        if (d.memberCount() != 5 || d.isCuboid()) throw new IllegalStateException("M10: expected a 5-block L, got " + d);
        if (d.waterMb() > 4000 || d.waterMb() < 3500) throw new IllegalStateException("M10: water " + d.waterMb());
        InteriorRegion r = InteriorManager.interiorLevel(level.getServer()) == null ? null : InteriorManager.ensureRegion(level, c);
        if (r != null && (r.fullBox() || !r.hasCell(0, 0, 0) || r.hasCell(1, 0, 1))) throw new IllegalStateException("M10: region cells of the L");
        int full = 0, shaped = 0;
        for (InteriorRegion x : InteriorRegions.get(level.getServer()).all()) {
            if (x.fullBox()) full++;
            else shaped++;
        }
        long water = d.waterMb();
        // breaking the corner leaves two bars
        level.setBlockAndUpdate(t, Blocks.AIR.defaultBlockState());
        TankBlockEntity a = TankMultiblock.getController(level, t.offset(1, 0, 0)), b = TankMultiblock.getController(level, t.offset(0, 0, 1));
        if (a == null || b == null || a == b) throw new IllegalStateException("M10: the L did not split into two bars");
        long rest = a.getTank().waterMb() + b.getTank().waterMb();
        if (rest != water * 4 / 5) throw new IllegalStateException("M10: water after the split " + rest + " of " + water);
        LOGGER.info("[smoketest] M10: L of {} blocks ({}x{}x{}), {} mB, region {}; split into {} + {} blocks with {} mB; regions: {} full box, {} shaped",
                d.memberCount(), d.sizeX(), d.sizeY(), d.sizeZ(), water, r == null ? -1 : r.id(), a.getTank().memberCount(), b.getTank().memberCount(),
                rest, full, shaped);
    }
}
