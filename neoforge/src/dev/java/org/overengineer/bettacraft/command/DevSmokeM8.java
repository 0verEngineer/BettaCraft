package org.overengineer.bettacraft.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.overengineer.bettacraft.compat.info.InfoData;
import org.overengineer.bettacraft.compat.info.InfoLines;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M8 part of the dedicated-server smoke test: the shared info data of the overlay plugins on the server (tank, betta,
 * filter) and, when installed, that The One Probe has our providers and WTHIT our plugin (by reflection; the server
 * must not need either mod).
 */
final class DevSmokeM8 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private DevSmokeM8() {}

    static void check(ServerLevel level, BlockPos tankPos, BlockPos filterPos) {
        TankBlockEntity c = TankMultiblock.getController(level, tankPos);
        CompoundTag tank = InfoData.tank(level, tankPos);
        if (c == null || tank == null) throw new IllegalStateException("M8: no info data for the tank");
        List<Component> lines = InfoLines.tank(tank, true);
        if (lines.size() < 3 || tank.getInt("count") != TankInhabitants.liveInhabitants(level, c.getTank()).size()) {
            throw new IllegalStateException("M8: tank info " + tank);
        }
        CompoundTag filter = InfoData.filter(level, level.getBlockEntity(filterPos));
        if (filter == null || !filter.getBoolean("attached")) throw new IllegalStateException("M8: filter info " + filter);
        BettaEntity b = (BettaEntity) TankInhabitants.liveInhabitants(level, c.getTank()).stream()
                .filter(e -> e instanceof BettaEntity).findFirst().orElseThrow(() -> new IllegalStateException("M8: no betta"));
        List<Component> betta = InfoLines.betta(b, InfoData.betta(b), true);
        // a betta whose genes are still unknown (M22.3 magnifier) shows one line instead of its genes: 4 lines are enough
        if (betta.size() < 4) throw new IllegalStateException("M8: betta info " + betta);
        LOGGER.info("[smoketest] M8 info lines: tank {}, filter {}, betta {} ({})", lines.size(), InfoLines.filter(filter).size(), betta.size(),
                betta.getFirst().getString());
        List<String> mods = new ArrayList<>();
        for (String m : List.of("jade", "theoneprobe", "wthit")) if (org.overengineer.bettacraft.platform.Platform.get().isModLoaded(m)) mods.add(m);
        if (org.overengineer.bettacraft.platform.Platform.get().isModLoaded("theoneprobe")) {
            try {
                Object imp = Class.forName("mcjty.theoneprobe.TheOneProbe").getField("theOneProbeImp").get(null);
                List<?> blocks = (List<?>) imp.getClass().getMethod("getProviders").invoke(imp);
                List<?> entities = (List<?>) imp.getClass().getMethod("getEntityProviders").invoke(imp);
                boolean ours = blocks.stream().anyMatch(p -> p.getClass().getName().startsWith("org.overengineer.bettacraft"))
                        && entities.stream().anyMatch(p -> p.getClass().getName().startsWith("org.overengineer.bettacraft"));
                if (!ours) throw new IllegalStateException("M8: The One Probe has no Bettacraft providers");
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("M8: The One Probe lookup failed", e);
            }
        }
        if (org.overengineer.bettacraft.platform.Platform.get().isModLoaded("wthit")) {
            try {
                Collection<?> plugins = (Collection<?>) Class.forName("mcp.mobius.waila.api.IPluginInfo").getMethod("getAllFromMod", String.class)
                        .invoke(null, "bettacraft");
                if (plugins.isEmpty()) throw new IllegalStateException("M8: WTHIT has no Bettacraft plugin");
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("M8: WTHIT lookup failed", e);
            }
        }
        LOGGER.info("[smoketest] M8 info mods: {} (plugins registered)", mods.isEmpty() ? "none" : mods);
    }
}
