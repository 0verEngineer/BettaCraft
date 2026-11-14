package org.overengineer.bettacraft.world;

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
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.interior.InteriorManager;

/**
 * M28.1: the own, frugal spawn pass of the wild animals. Vanilla spawns all water-ambient mobs against one cap of 20 per
 * player; cod, salmon, tropical fish and the fish of other mods (Fish of Thieves, Naturalist, ...) fill it, mostly from the
 * big oceans, so the rare animals of the mod in a river or swamp next to them hardly ever get a turn. Every
 * {@code wildlife.spawnPassInterval} ticks each player gets one try here: a random water column 24 to 48 blocks away; if
 * its biome lists animals of the mod (the same biome spawn lists as vanilla, {@link WildlifeSpawnsBiomeModifier},
 * {@link BettaSpawnsBiomeModifier}), one group of them spawns with the normal spawn rules ({@link Wildlife#checkFish},
 * {@link Wildlife#checkCritter}: a few per 32 blocks, never in a tank) while fewer than
 * {@code wildlife.spawnPassPerPlayer} wild animals of the mod live within 48 blocks. They despawn like vanilla fish.
 * Switch: {@code features.wildSpawnPass}.
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class WildSpawnPass {
    /** Radius around a player in which the wild animals of the mod are counted for the limit. */
    public static final int PLAYER_RADIUS = 48;
    /** The tried columns lie this far from the player (vanilla spawns from 24 blocks on; fish beyond 32 may despawn). */
    public static final int MIN_DISTANCE = 24, MAX_DISTANCE = 48;
    /** Columns tried per pass until one fits. */
    public static final int TRIES = 8;

    private WildSpawnPass() {}

    public static int interval() {
        return Math.max(1, ServerConfig.getOr(ServerConfig.WILD_PASS_INTERVAL, 200));
    }

    public static int perPlayer() {
        return ServerConfig.getOr(ServerConfig.WILD_PASS_PER_PLAYER, 10);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !CommonConfig.wildSpawnPass()) return;
        if (InteriorManager.isInterior(level) || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return;
        int every = interval();
        long t = level.getGameTime();
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || (t + p.getId()) % every != 0) continue;
            tryFor(level, p.blockPosition(), level.random);
        }
    }

    /** Wild animals of the mod within {@link #PLAYER_RADIUS} blocks of a position. */
    public static int nearby(ServerLevel level, BlockPos pos) {
        return wildAround(level, pos, PLAYER_RADIUS).size();
    }

    /** The wild animals of the mod around a position (not in a tank, not caught). */
    public static List<Entity> wildAround(ServerLevel level, BlockPos pos, double radius) {
        return level.getEntities((Entity) null, new AABB(pos).inflate(radius), Wildlife::isWildAnimal);
    }

    /** How many more animals the pass may spawn around this position. */
    public static int room(ServerLevel level, BlockPos pos) {
        return Math.max(0, perPlayer() - nearby(level, pos));
    }

    /** One pass around a player position; returns how many animals spawned. */
    public static int tryFor(ServerLevel level, BlockPos center, RandomSource r) {
        int room = room(level, center);
        if (room <= 0) return 0;
        for (int i = 0; i < TRIES; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = MIN_DISTANCE + r.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
            int x = center.getX() + (int) Math.round(Math.cos(a) * d), z = center.getZ() + (int) Math.round(Math.sin(a) * d);
            BlockPos surface = surface(level, x, z);
            if (surface == null) continue;
            List<MobSpawnSettings.SpawnerData> ours = ours(level.getBiome(surface));
            if (ours.isEmpty()) continue;
            int n = spawnInColumn(level, surface, ours, r, room);
            if (n > 0) return n;
        }
        return 0;
    }

    /** The top water block of a loaded, ticking column, or null (no water on top). */
    @Nullable
    public static BlockPos surface(ServerLevel level, int x, int z) {
        BlockPos col = new BlockPos(x, level.getSeaLevel(), z);
        if (!level.isLoaded(col) || !level.isPositionEntityTicking(col)) return null;
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
        for (int y = top; y >= top - 1; y--) { // a lily pad on the water blocks motion too
            BlockPos p = new BlockPos(x, y, z);
            if (level.getFluidState(p).is(FluidTags.WATER)) return p;
        }
        return null;
    }

    /** The spawn entries of the mod in a biome (water ambient group, with their biome weights). */
    public static List<MobSpawnSettings.SpawnerData> ours(Holder<Biome> biome) {
        List<MobSpawnSettings.SpawnerData> out = new ArrayList<>();
        for (var d : biome.value().getMobSettings().getMobs(MobCategory.WATER_AMBIENT).unwrap()) {
            if (d.type.is(ModTags.ALL_ANIMALS)) out.add(d);
        }
        return out;
    }

    /**
     * Picks one of {@code entries} by weight and spawns a group of it in the water column below {@code surface} (fish near
     * the surface, small animals on the bottom), at most {@code room} animals, with the normal spawn rules and events.
     */
    public static int spawnInColumn(ServerLevel level, BlockPos surface, List<MobSpawnSettings.SpawnerData> entries, RandomSource r, int room) {
        var pick = WeightedRandom.getRandomItem(r, entries);
        if (pick.isEmpty() || room <= 0) return 0;
        MobSpawnSettings.SpawnerData d = pick.get();
        boolean bottom = d.type == ModEntities.WILD_CRITTER.get();
        int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR, surface.getX(), surface.getZ());
        int y = bottom ? floor : Math.max(floor, surface.getY() - r.nextInt(3));
        BlockPos start = new BlockPos(surface.getX(), y, surface.getZ());
        int count = Math.min(room, d.minCount + r.nextInt(Math.max(1, d.maxCount - d.minCount + 1)));
        return spawnGroup(level, d.type, start, bottom, count, r);
    }

    private static int spawnGroup(ServerLevel level, EntityType<?> type, BlockPos start, boolean bottom, int count, RandomSource r) {
        SpawnGroupData group = null;
        int made = 0;
        for (int i = 0; i < count * 3 && made < count; i++) {
            BlockPos at = i == 0 ? start : start.offset(r.nextInt(5) - 2, bottom ? 0 : r.nextInt(3) - 1, r.nextInt(5) - 2);
            if (bottom) at = new BlockPos(at.getX(), level.getHeight(Heightmap.Types.OCEAN_FLOOR, at.getX(), at.getZ()), at.getZ());
            if (!level.getFluidState(at).is(FluidTags.WATER) || !level.isPositionEntityTicking(at)) continue;
            if (!SpawnPlacements.isSpawnPositionOk(type, level, at)) continue;
            if (!SpawnPlacements.checkSpawnRules(type, level, MobSpawnType.NATURAL, at, r)) continue;
            double x = at.getX() + 0.5, z = at.getZ() + 0.5;
            if (!level.noCollision(type.getSpawnAABB(x, at.getY(), z))) continue;
            if (!(type.create(level) instanceof Mob mob)) return made;
            mob.moveTo(x, at.getY(), z, r.nextFloat() * 360F, 0F);
            if (!EventHooks.checkSpawnPosition(mob, level, MobSpawnType.NATURAL)) {
                mob.discard();
                continue;
            }
            group = EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(at), MobSpawnType.NATURAL, group);
            if (level.tryAddFreshEntityWithPassengers(mob)) made++;
        }
        return made;
    }
}
