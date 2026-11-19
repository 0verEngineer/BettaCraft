package org.overengineer.bettacraft.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import org.overengineer.bettacraft.platform.event.SubscribeEvent;
import org.overengineer.bettacraft.platform.event.ServerStartedEvent;
import org.overengineer.bettacraft.BettaCraft;
import static org.overengineer.bettacraft.config.WorldConfigMigration.*;

/** M52: the game event handlers of {@link WorldConfigMigration} (M52: on the common game events, fired by each loader). */
public final class WorldConfigMigrationHandlers {
    private WorldConfigMigrationHandlers() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        Path file = server.getWorldPath(new LevelResource(org.overengineer.bettacraft.platform.Platform.get().worldConfigFolder()))
                .resolve(BettaCraft.MOD_ID + "-server.toml");
        run(server, Files.isRegularFile(file) ? file : null);
    }
}
