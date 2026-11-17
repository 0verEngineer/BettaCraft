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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.overengineer.bettacraft.BettaCraft;
import static org.overengineer.bettacraft.config.WorldConfigMigration.*;

/** M52: the NeoForge event handlers of {@link WorldConfigMigration} (moved here from the common class). */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
final class WorldConfigMigrationNeoForgeEvents {
    private WorldConfigMigrationNeoForgeEvents() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        Path file = server.getWorldPath(SERVERCONFIG).resolve(BettaCraft.MOD_ID + "-server.toml");
        run(server, Files.isRegularFile(file) ? file : null);
    }
}
