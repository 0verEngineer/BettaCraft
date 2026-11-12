package org.overengineer.bettacraft.command;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.compat.info.InfoData;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M11 part of the dedicated-server smoke test: the tanks of earlier runs (persistent dev world, saved before M11)
 * load as textured tanks with their glass; a fresh textured and a fresh clean block next to each other stay two tanks;
 * the new recipes and the mirror advancement are loaded; the info data names the glass.
 */
final class DevSmokeM11 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private DevSmokeM11() {}

    private static BlockPos pairPos(BlockPos base) {
        return base.offset(0, 0, 18);
    }

    static void setup(ServerLevel level, BlockPos base) {
        BlockPos p = pairPos(base);
        // fresh every run
        for (int x = 0; x < 2; x++) if (!level.getBlockState(p.offset(x, 0, 0)).isAir()) level.setBlockAndUpdate(p.offset(x, 0, 0), Blocks.AIR.defaultBlockState());
        TankStyle[] styles = {TankStyle.TEXTURED, TankStyle.CLEAN};
        for (int x = 0; x < 2; x++) {
            BlockPos q = p.offset(x, 0, 0);
            level.setBlockAndUpdate(q, ModBlocks.TANK.get().defaultBlockState());
            if (level.getBlockEntity(q) instanceof TankBlockEntity be) be.setStyle(styles[x]);
            TankMultiblock.onTankBlockAdded(level, q, null);
        }
    }

    static void check(ServerLevel level, BlockPos base) {
        // the old main tank of the dev world: saved by M10 or earlier, now textured
        TankBlockEntity old = TankMultiblock.getController(level, base);
        if (old == null) throw new IllegalStateException("M11: no dev world tank");
        if (old.getStyle() != TankStyle.TEXTURED) throw new IllegalStateException("M11: old tank is " + old.getStyle());
        CompoundTag saved = old.saveWithFullMetadata(level.registryAccess());
        if (saved.getInt("schema") != 6 || !"textured".equals(saved.getString("style"))) throw new IllegalStateException("M11: saved as " + saved.getInt("schema"));
        // styles never merge
        BlockPos p = pairPos(base);
        TankBlockEntity a = TankMultiblock.getController(level, p), b = TankMultiblock.getController(level, p.offset(1, 0, 0));
        if (a == null || b == null || a == b) throw new IllegalStateException("M11: textured and clean tank merged (or did not form)");
        if (a.getStyle() != TankStyle.TEXTURED || b.getStyle() != TankStyle.CLEAN) throw new IllegalStateException("M11: styles " + a.getStyle() + "/" + b.getStyle());
        // recipes and advancement
        var rm = level.getServer().getRecipeManager();
        for (String id : new String[]{"tank", "tank_clean", "tank_textured_to_clean", "tank_clean_to_textured", "mirror_stick"}) {
            if (rm.byKey(BettaCraft.id(id)).isEmpty()) throw new IllegalStateException("M11: recipe " + id + " missing");
        }
        if (level.getServer().getAdvancements().get(BettaCraft.id("mirror")) == null) throw new IllegalStateException("M11: advancement mirror missing");
        CompoundTag info = InfoData.tank(level, base);
        if (info == null || !old.getGlassId().toString().equals(info.getString("glass")) || !"textured".equals(info.getString("style"))) {
            throw new IllegalStateException("M11: info data " + info);
        }
        LOGGER.info("[smoketest] M11: dev world tank {} ({} blocks) is textured {}, saved as schema 6; textured + clean pair stays two tanks; "
                + "recipes and advancement loaded; info glass {}", base, old.getTank().memberCount(), old.getGlassId(), info.getString("glass"));
    }
}
