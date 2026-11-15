package org.overengineer.bettacraft.terrarium;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.world.BiomeWaters;
import org.overengineer.bettacraft.world.Wildlife;

/**
 * M31 part b: where the turtles live in the world (decided by the user's plan, biome tags of M25.1 so biome mods count):
 * <ul>
 *     <li>swamps ({@code #bettacraft:swamp_waters}): sliders (mostly the yellow-bellied one, at home in the swamps of the
 *     south-east of North America) and musk turtles;</li>
 *     <li>rivers ({@code #bettacraft:river_waters}): sliders (mostly red-eared, from the Mississippi), musk turtles, Reeves'
 *     turtles;</li>
 *     <li>savannas and dry shrubland ({@code #bettacraft:tortoise_lands}): Hermann's tortoise (Mediterranean scrub).</li>
 * </ul>
 * The water turtles spawn on the shore (at most three blocks from water) or in shallow water, the tortoise on grass, dirt
 * or sand under the open sky; never in a tank, and at most {@link #NEARBY_CAP} wild turtles within 32 blocks. They come
 * at the generation of new chunks like vanilla animals (and stay) and through the frugal spawn pass of the mod
 * ({@link #tryFor}, M28.1): those despawn far from players like the wild fish. Catching: a bucket of water (water turtles),
 * the terrarium box (the tortoise). Switch: {@code features.wildTurtles}.
 */
public final class TurtleSpawns {
    /** Wild turtles allowed within 32 blocks of a new one. */
    public static final int NEARBY_CAP = 2;
    /** Share of the spawn passes that also try a turtle. */
    public static final double PASS_CHANCE = 0.25;

    /** The places of the turtles (for the handbook {@code Sources}: swamp, river, dry land). */
    public enum Home { SWAMP, RIVER, DRY_LAND }

    /** One spawn entry: species, home, CREATURE spawn weight. */
    public record Entry(TurtleSpecies species, Home home, int weight) {}

    public static final List<Entry> TABLE = List.of(
            new Entry(TurtleSpecies.SLIDER, Home.SWAMP, 4), new Entry(TurtleSpecies.MUSK, Home.SWAMP, 4),
            new Entry(TurtleSpecies.SLIDER, Home.RIVER, 3), new Entry(TurtleSpecies.MUSK, Home.RIVER, 2), new Entry(TurtleSpecies.REEVES, Home.RIVER, 3),
            new Entry(TurtleSpecies.HERMANN, Home.DRY_LAND, 4));

    private TurtleSpawns() {}

    /** The home of a biome (null = no turtles). Swamps before rivers; the tortoise lands. */
    @Nullable
    public static Home home(Holder<Biome> b) {
        if (BiomeWaters.swamp(b)) return Home.SWAMP;
        if (BiomeWaters.river(b)) return Home.RIVER;
        if (b.is(ModTags.TORTOISE_LANDS)) return Home.DRY_LAND;
        return null;
    }

    public static List<Entry> entries(@Nullable Home home) {
        List<Entry> out = new ArrayList<>();
        if (home == null) return out;
        for (Entry e : TABLE) if (e.home == home) out.add(e);
        return out;
    }

    /** Smallest and largest group (the tortoise alone, the water turtles one or two). */
    public static int[] group(TurtleSpecies s) {
        return s.box() ? new int[]{1, 1} : new int[]{1, 2};
    }

    /** The variety of a wild slider: yellow-bellied mostly in swamps, red-eared mostly in rivers. */
    public static int variety(TurtleSpecies s, Holder<Biome> biome, RandomSource r) {
        if (s.varieties().size() < 2) return 0;
        double scripta = BiomeWaters.swamp(biome) ? 0.75 : BiomeWaters.river(biome) ? 0.15 : 0.4;
        return r.nextDouble() < scripta ? s.variety("scripta") : s.variety("elegans");
    }

    @Nullable
    public static TurtleSpecies of(EntityType<?> type) {
        for (TurtleSpecies s : TurtleSpecies.ALL) if (ModEntities.turtle(s) == type) return s;
        return null;
    }

    /** Whether wild turtles spawn (both switches). */
    public static boolean enabled() {
        return CommonConfig.turtles() && CommonConfig.wildTurtles();
    }

    // ------------------------------------------------------------------ spawn rule

    /** Spawn rule of the wild turtles (natural spawns and new chunks; spawn eggs and buckets always). */
    public static boolean check(EntityType<?> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (spawnType != MobSpawnType.NATURAL && spawnType != MobSpawnType.CHUNK_GENERATION) return true;
        if (!enabled()) return false;
        TurtleSpecies s = of(type);
        if (s == null || Wildlife.inTank(level, pos)) return false;
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) return false;
        if (s.box()) {
            if (!level.getFluidState(pos).isEmpty() || !level.canSeeSky(pos)) return false;
            var ground = level.getBlockState(below);
            if (!ground.is(BlockTags.DIRT) && !ground.is(BlockTags.SAND)) return false;
        } else {
            boolean wet = level.getFluidState(pos).is(FluidTags.WATER);
            if (wet && level.getFluidState(pos.above(2)).is(FluidTags.WATER)) return false; // shallow water only
            if (!wet && !nearWater(level, pos, 3)) return false;
        }
        AABB box = new AABB(pos).inflate(32);
        return level.getEntitiesOfClass(TurtleEntity.class, box, e -> e.isWild() || !e.isPersistenceRequired() && !e.inTank()).size() < NEARBY_CAP;
    }

    private static boolean nearWater(ServerLevelAccessor level, BlockPos pos, int r) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) for (int dy = -1; dy <= 0; dy++) {
            if (level.getFluidState(p.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz)).is(FluidTags.WATER)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ the spawn pass

    /**
     * M28.1 pass, for the turtles (called by {@link org.overengineer.bettacraft.world.WildSpawnPass}): in a quarter of the
     * passes up to four columns 24-48 blocks from the player; the first whose biome has turtles gets one (or two) at its
     * top block, by the spawn rule. Returns how many spawned.
     */
    public static int tryFor(ServerLevel level, BlockPos center, RandomSource r, int room) {
        if (!enabled() || room <= 0 || r.nextDouble() >= PASS_CHANCE) return 0;
        for (int i = 0; i < 4; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = 24 + r.nextDouble() * 24;
            int x = center.getX() + (int) Math.round(Math.cos(a) * d), z = center.getZ() + (int) Math.round(Math.sin(a) * d);
            BlockPos col = new BlockPos(x, level.getSeaLevel(), z);
            if (!level.isLoaded(col) || !level.isPositionEntityTicking(col)) continue;
            BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            List<Entry> here = entries(home(level.getBiome(top)));
            if (here.isEmpty()) continue;
            int total = 0;
            for (Entry e : here) total += e.weight;
            int pick = r.nextInt(Math.max(1, total));
            Entry chosen = here.getFirst();
            for (Entry e : here) {
                if (pick < e.weight) {
                    chosen = e;
                    break;
                }
                pick -= e.weight;
            }
            return spawn(level, chosen.species, level.getFluidState(top.below()).is(FluidTags.WATER) ? top.below() : top, r, room);
        }
        return 0;
    }

    private static int spawn(ServerLevel level, TurtleSpecies s, BlockPos at, RandomSource r, int room) {
        EntityType<? extends TurtleEntity> type = ModEntities.turtle(s);
        int[] g = group(s);
        int count = Math.min(room, g[0] + r.nextInt(g[1] - g[0] + 1)), made = 0;
        for (int i = 0; i < count * 3 && made < count; i++) {
            BlockPos p = i == 0 ? at : at.offset(r.nextInt(5) - 2, 0, r.nextInt(5) - 2);
            if (i > 0) p = new BlockPos(p.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getX(), p.getZ()), p.getZ());
            if (!SpawnPlacements.checkSpawnRules(type, level, MobSpawnType.NATURAL, p, r)) continue;
            if (!level.noCollision(type.getSpawnAABB(p.getX() + 0.5, p.getY(), p.getZ() + 0.5))) continue;
            TurtleEntity t = type.create(level);
            if (t == null) return made;
            t.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, r.nextFloat() * 360F, 0F);
            if (!EventHooks.checkSpawnPosition(t, level, MobSpawnType.NATURAL)) {
                t.discard();
                continue;
            }
            EventHooks.finalizeMobSpawn(t, level, level.getCurrentDifficultyAt(p), MobSpawnType.NATURAL, null);
            if (level.tryAddFreshEntityWithPassengers(t)) made++;
        }
        return made;
    }
}
