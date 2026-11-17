package org.overengineer.bettacraft.foreign;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.overengineer.bettacraft.platform.event.SubscribeEvent;
import org.overengineer.bettacraft.platform.event.AddReloadListenerEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.terrarium.TerrariumAnimals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.overengineer.bettacraft.foreign.ForeignProfiles.*;

/** M52: the game event handlers of {@link ForeignProfiles} (M52: on the common game events, fired by each loader). */
public final class ForeignProfilesHandlers {
    private ForeignProfilesHandlers() {}

    @SubscribeEvent
    static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ForeignProfiles());
    }
}
