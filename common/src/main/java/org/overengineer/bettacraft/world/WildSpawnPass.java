package org.overengineer.bettacraft.world;

import net.minecraft.util.random.Weighted;
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
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.entity.critter.WildCritterEntity;
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
 * Switch: {@code features.wildSpawnPass.enabled}.
 * <p>
 * M46: the small animals (shrimp, snails, crayfish, hermit crabs: {@code wild_critter}) have their own limit
 * ({@code wildlife.spawnPassCrittersPerPlayer}, 12 within 48 blocks) and their own try per pass ({@link #tryCrittersFor}), so
 * they never take the room of the fish and the fish never take theirs (frogs and turtles stay with the fish). Their try
 * prefers shallow water (1 to 6 blocks deep, {@link #pickCritterColumn}) over sand, gravel, stone or by water plants, where
 * one sees them from the bank; deep ground stays possible. The rules of {@link Wildlife#checkCritter} stay.
 */
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

    /** M46: the small animals' own limit (wild ones within {@link #PLAYER_RADIUS} blocks). */
    public static int crittersPerPlayer() {
        return ServerConfig.getOr(ServerConfig.WILD_PASS_CRITTERS_PER_PLAYER, 12);
    }

    /** M46: columns the small animals' try looks at (it picks a shallow one of them). */
    public static final int CRITTER_TRIES = 16;
    /** M46: water this deep (blocks, the bottom water block included) counts as shallow for the small animals. */
    public static final int SHALLOW_MIN = 1, SHALLOW_MAX = 6;
    /** M46: the share of the small animals' tries that takes a deep column although a shallow one was found. */
    public static final double DEEP_SHARE = 0.15;


    /**
     * Wild animals of the mod within {@link #PLAYER_RADIUS} blocks of a position that count for {@link #perPlayer}: fish, frogs,
     * bettas, turtles (M46: not the small animals, they have {@link #crittersNearby}).
     */
    public static int nearby(ServerLevel level, BlockPos pos) {
        int n = 0;
        for (Entity e : wildAround(level, pos, PLAYER_RADIUS)) if (!(e instanceof WildCritterEntity)) n++;
        return n;
    }

    /** M46: wild small animals within {@link #PLAYER_RADIUS} blocks of a position (for {@link #crittersPerPlayer}). */
    public static int crittersNearby(ServerLevel level, BlockPos pos) {
        int n = 0;
        for (Entity e : wildAround(level, pos, PLAYER_RADIUS)) if (e instanceof WildCritterEntity) n++;
        return n;
    }

    /** The wild animals of the mod around a position (not in a tank, not caught). */
    public static List<Entity> wildAround(ServerLevel level, BlockPos pos, double radius) {
        return level.getEntities((Entity) null, new AABB(pos).inflate(radius), Wildlife::isWildAnimal);
    }

    /** How many more fish, frogs or turtles the pass may spawn around this position. */
    public static int room(ServerLevel level, BlockPos pos) {
        return Math.max(0, perPlayer() - nearby(level, pos));
    }

    /** M46: how many more small animals the pass may spawn around this position. */
    public static int critterRoom(ServerLevel level, BlockPos pos) {
        return Math.max(0, crittersPerPlayer() - crittersNearby(level, pos));
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
            List<Weighted<MobSpawnSettings.SpawnerData>> ours = fish(ours(level.getBiome(surface)));
            if (ours.isEmpty()) continue;
            int n = spawnInColumn(level, surface, ours, r, room);
            if (n > 0) return n;
        }
        return 0;
    }

    /** M46: the entries without the small animals (they have their own try). */
    public static List<Weighted<MobSpawnSettings.SpawnerData>> fish(List<Weighted<MobSpawnSettings.SpawnerData>> entries) {
        return entries.stream().filter(d -> d.value().type() != ModEntities.WILD_CRITTER.get()).toList();
    }

    /** M46: whether small animals spawn in this biome. */
    public static boolean hasCritters(Holder<Biome> biome) {
        return ours(biome).stream().anyMatch(d -> d.value().type() == ModEntities.WILD_CRITTER.get());
    }

    // ------------------------------------------------------------------ M46: the small animals' own try

    /**
     * One try of the small animals around a player position: up to {@link #CRITTER_TRIES} water columns 24 to 48 blocks away
     * whose biome has small animals, one of them picked by {@link #pickCritterColumn}, a group on its bottom. Returns how many
     * spawned (none while {@link #critterRoom} is 0 or {@code features.invertebrates.wildSpawns} is off).
     */
    public static int tryCrittersFor(ServerLevel level, BlockPos center, RandomSource r) {
        if (!Features.enabled(Feature.INVERTEBRATES_WILD)) return 0;
        int room = critterRoom(level, center);
        if (room <= 0) return 0;
        List<BlockPos> columns = new ArrayList<>();
        for (int i = 0; i < CRITTER_TRIES; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = MIN_DISTANCE + r.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
            int x = center.getX() + (int) Math.round(Math.cos(a) * d), z = center.getZ() + (int) Math.round(Math.sin(a) * d);
            BlockPos surface = surface(level, x, z);
            if (surface != null && hasCritters(level.getBiome(surface))) columns.add(surface);
        }
        BlockPos pick = pickCritterColumn(level, columns, r);
        return pick == null ? 0 : spawnCritters(level, pick, r, room);
    }

    /** M46: how deep the water of a column is (blocks from its surface block down to the bottom water block, both counted). */
    public static int depth(ServerLevel level, BlockPos surface) {
        return surface.getY() - level.getHeight(Heightmap.Types.OCEAN_FLOOR, surface.getX(), surface.getZ()) + 1;
    }

    /**
     * M46: the column the small animals spawn in: shallow water ({@link #SHALLOW_MIN} to {@link #SHALLOW_MAX} blocks) over sand,
     * gravel or stone or by water plants first, then any shallow column, then a deep one; in {@link #DEEP_SHARE} of the tries a
     * deep one although there is shallow water (they live on deep ground too). Null without a column.
     */
    @Nullable
    public static BlockPos pickCritterColumn(ServerLevel level, List<BlockPos> columns, RandomSource r) {
        BlockPos good = null, shallow = null, deep = null;
        for (BlockPos c : columns) {
            int d = depth(level, c);
            if (d < SHALLOW_MIN) continue;
            if (d > SHALLOW_MAX) {
                if (deep == null) deep = c;
            } else if (good == null && goodGround(level, c.below(d - 1))) {
                good = c;
            } else if (shallow == null) {
                shallow = c;
            }
        }
        if (deep != null && (good != null || shallow != null) && r.nextDouble() < DEEP_SHARE) return deep;
        return good != null ? good : shallow != null ? shallow : deep;
    }

    /** M46: the bottom water block lies on sand, gravel or stone, or water plants (also the wild ones) grow near it. */
    static boolean goodGround(ServerLevel level, BlockPos bottomWater) {
        if (NetCatching.hardBottom(level, bottomWater) || NetCatching.plantsNear(level, bottomWater)) return true;
        for (BlockPos p : BlockPos.betweenClosed(bottomWater.offset(-2, -1, -2), bottomWater.offset(2, 1, 2))) {
            if (level.getBlockState(p).getBlock() instanceof org.overengineer.bettacraft.plants.wild.WildPlantBlock) return true;
        }
        return false;
    }

    /**
     * M46: a small group of small animals on the bottom of the column under {@code surface}: the species picked for the water
     * there ({@link Wildlife#pickCritter}, the apple snail damped), as many as {@link Wildlife#critterGroup} says, at most
     * {@code room}, with the normal spawn rules ({@link Wildlife#checkCritter}).
     */
    public static int spawnCritters(ServerLevel level, BlockPos surface, RandomSource r, int room) {
        if (room <= 0 || !Features.enabled(Feature.INVERTEBRATES_WILD)) return 0;
        var type = ModEntities.WILD_CRITTER.get();
        BlockPos start = new BlockPos(surface.getX(), level.getHeight(Heightmap.Types.OCEAN_FLOOR, surface.getX(), surface.getZ()), surface.getZ());
        var w = Wildlife.water(level.getBiome(start));
        var s = Wildlife.pickCritter(w, NetCatching.plantsNear(level, start), NetCatching.hardBottom(level, start), NetCatching.random(level));
        int[] g = s == null ? new int[]{2, 4} : Wildlife.critterGroup(s);
        int count = Math.min(room, g[0] + r.nextInt(Math.max(1, g[1] - g[0] + 1)));
        return spawnGroup(level, type, start, true, count, r, s == null ? null : new WildCritterEntity.Group(s));
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
    public static List<Weighted<MobSpawnSettings.SpawnerData>> ours(Holder<Biome> biome) {
        List<Weighted<MobSpawnSettings.SpawnerData>> out = new ArrayList<>();
        for (var d : biome.value().getMobSettings().getMobs(MobCategory.WATER_AMBIENT).unwrap()) {
            if (d.value().type().builtInRegistryHolder().is(ModTags.ALL_ANIMALS)) out.add(d);
        }
        return out;
    }

    /**
     * Picks one of {@code entries} by weight and spawns a group of it in the water column below {@code surface} (fish near
     * the surface, small animals on the bottom), at most {@code room} animals, with the normal spawn rules and events.
     */
    public static int spawnInColumn(ServerLevel level, BlockPos surface, List<Weighted<MobSpawnSettings.SpawnerData>> entries, RandomSource r, int room) {
        var pick = WeightedRandom.getRandomItem(r, entries, Weighted::weight).map(Weighted::value);
        if (pick.isEmpty() || room <= 0) return 0;
        MobSpawnSettings.SpawnerData d = pick.get();
        boolean bottom = d.type() == ModEntities.WILD_CRITTER.get();
        int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR, surface.getX(), surface.getZ());
        int y = bottom ? floor : Math.max(floor, surface.getY() - r.nextInt(3));
        BlockPos start = new BlockPos(surface.getX(), y, surface.getZ());
        int count = Math.min(room, d.minCount() + r.nextInt(Math.max(1, d.maxCount() - d.minCount() + 1)));
        return spawnGroup(level, d.type(), start, bottom, count, r, null);
    }

    private static int spawnGroup(ServerLevel level, EntityType<?> type, BlockPos start, boolean bottom, int count, RandomSource r,
                                  @Nullable SpawnGroupData first) {
        SpawnGroupData group = first;
        int made = 0;
        for (int i = 0; i < count * 3 && made < count; i++) {
            BlockPos at = i == 0 ? start : start.offset(r.nextInt(5) - 2, bottom ? 0 : r.nextInt(3) - 1, r.nextInt(5) - 2);
            if (bottom) at = new BlockPos(at.getX(), level.getHeight(Heightmap.Types.OCEAN_FLOOR, at.getX(), at.getZ()), at.getZ());
            if (!level.getFluidState(at).is(FluidTags.WATER) || !level.isPositionEntityTicking(at)) continue;
            if (!SpawnPlacements.isSpawnPositionOk(type, level, at)) continue;
            if (!SpawnPlacements.checkSpawnRules(type, level, EntitySpawnReason.NATURAL, at, r)) continue;
            double x = at.getX() + 0.5, z = at.getZ() + 0.5;
            if (!level.noCollision(type.getSpawnAABB(x, at.getY(), z))) continue;
            if (!(type.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED) instanceof Mob mob)) return made;
            mob.snapTo(x, at.getY(), z, r.nextFloat() * 360F, 0F);
            if (!org.overengineer.bettacraft.platform.Platform.get().checkSpawnPosition(mob, level, EntitySpawnReason.NATURAL)) {
                mob.discard();
                continue;
            }
            group = org.overengineer.bettacraft.platform.Platform.get().finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(at), EntitySpawnReason.NATURAL, group);
            if (level.tryAddFreshEntityWithPassengers(mob)) made++;
        }
        return made;
    }
}
