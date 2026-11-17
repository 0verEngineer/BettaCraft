package org.overengineer.bettacraft.world;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.overengineer.bettacraft.platform.event.SubscribeEvent;
import org.overengineer.bettacraft.platform.event.LevelTickEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.entity.critter.WildCritterEntity;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import static org.overengineer.bettacraft.world.WildSpawnPass.*;

/** M52: the game event handlers of {@link WildSpawnPass} (M52: on the common game events, fired by each loader). */
public final class WildSpawnPassHandlers {
    private WildSpawnPassHandlers() {}

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !Features.enabled(Feature.WILD_SPAWN_PASS)) return;
        if (InteriorManager.isInterior(level) || !level.getGameRules().get(GameRules.SPAWN_MOBS)) return;
        int every = interval();
        long t = level.getGameTime();
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || (t + p.getId()) % every != 0) continue;
            tryFor(level, p.blockPosition(), level.random);
            tryCrittersFor(level, p.blockPosition(), level.random); // M46: the small animals' own try
            // M31 part b: now and then a turtle on the shore or a tortoise on dry grassland
            org.overengineer.bettacraft.terrarium.HerpSpawns.tryFor(level, p.blockPosition(), level.random, room(level, p.blockPosition()));
        }
    }
}
