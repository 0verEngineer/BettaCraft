package org.overengineer.bettacraft.plants.foreign;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.event.SubscribeEvent;
import org.overengineer.bettacraft.platform.event.AddReloadListenerEvent;
import org.overengineer.bettacraft.platform.event.TagsUpdatedEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.foreign.ForeignProfiles;
import org.overengineer.bettacraft.plants.AquaticPlantBlock;
import org.overengineer.bettacraft.tank.interior.InteriorEquipment;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.overengineer.bettacraft.plants.foreign.ForeignPlants.*;

/** M52: the game event handlers of {@link ForeignPlants} (M52: on the common game events, fired by each loader). */
public final class ForeignPlantsHandlers {
    private ForeignPlantsHandlers() {}

    @SubscribeEvent
    static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ForeignPlants());
    }

    @SubscribeEvent
    static void onTags(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) changed();
    }
}
