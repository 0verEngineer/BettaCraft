package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.critter.WildCritterEntity;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.world.WildSpawnPass;

/**
 * M46 GameTests: the small animals' own limit and try of the spawn pass. With the fish at their limit small animals still
 * spawn and the other way round; the small animals' limit is kept; in water with a shallow and a deep part most groups go to
 * the shallow part (deep ground stays possible); with {@code features.invertebrates.wildSpawns} off none spawn.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CritterSpawnPassGameTests {
    private static final String EMPTY = "empty_32x10x32";
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("bettacraft-m46");

    private CritterSpawnPassGameTests() {}

    private static void check(boolean ok, String what) {
        if (!ok) throw new GameTestAssertException(what);
    }

    /**
     * A pool {@code sx} x {@code sz} with its surface at sea level (absolute, at the test's x/z): the half with x below
     * {@code sx / 2} {@code shallow} blocks deep over sand, the other half {@code deep} blocks deep over stone.
     */
    private static BlockPos pool(GameTestHelper h, int sx, int sz, int shallow, int deep) {
        ServerLevel level = h.getLevel();
        BlockPos rel = h.absolutePos(new BlockPos(4, 0, 4));
        int sea = level.getSeaLevel();
        BlockPos top = new BlockPos(rel.getX(), sea - 1, rel.getZ()); // the surface water blocks
        for (int x = -1; x <= sx; x++)
            for (int z = -1; z <= sz; z++) {
                boolean wall = x < 0 || z < 0 || x == sx || z == sz;
                int d = x < sx / 2 ? shallow : deep;
                for (int y = -deep - 1; y < 6; y++) {
                    BlockPos p = top.offset(x, y, z);
                    if (y > 0) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    else if (wall) level.setBlock(p, Blocks.GLASS.defaultBlockState(), 2);
                    else if (y > -d) level.setBlock(p, Blocks.WATER.defaultBlockState(), 2);
                    else level.setBlock(p, (d == shallow ? Blocks.SAND : Blocks.STONE).defaultBlockState(), 2);
                }
            }
        return top;
    }

    private static void clear(GameTestHelper h, BlockPos top, int sx, int sz, int deep) {
        ServerLevel level = h.getLevel();
        for (Entity e : level.getEntities((Entity) null, new AABB(top).inflate(sx + sz + 8), e -> e.getType().is(ModTags.ALL_ANIMALS))) e.discard();
        for (int x = -1; x <= sx; x++)
            for (int z = -1; z <= sz; z++)
                for (int y = -deep - 1; y < 6; y++) level.setBlock(top.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
    }

    private static int critters(ServerLevel level, BlockPos top) {
        return level.getEntitiesOfClass(WildCritterEntity.class, new AABB(top).inflate(40), WildCritterEntity::isWild).size();
    }

    /** Fills the fish limit with wild guppies spread over the pool. */
    private static void fillFish(ServerLevel level, BlockPos top, int sx, int sz, int n) {
        var type = ModEntities.TANKMATES.get(TankmateSpecies.GUPPY).get();
        for (int i = 0; i < n; i++) type.spawn(level, top.offset(i % sx, 0, (i / sx) % sz), MobSpawnType.NATURAL);
    }

    /** With as many fish as the pass allows small animals still spawn; with the small animals at their limit fish still do. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void crittersAndFishKeepTheirOwnRoom(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        int sx = 10, sz = 6;
        BlockPos top = pool(h, sx, sz, 3, 3);
        try {
            int fishLimit = WildSpawnPass.perPlayer(), critterLimit = WildSpawnPass.crittersPerPlayer();
            check(fishLimit == 10 && critterLimit == 12, "the limits " + fishLimit + " / " + critterLimit);
            fillFish(level, top, sx, sz, fishLimit);
            check(WildSpawnPass.room(level, top) == 0, "room for fish with " + fishLimit + " fish: " + WildSpawnPass.room(level, top));
            check(WildSpawnPass.critterRoom(level, top) == critterLimit, "the fish take the small animals' room: " + WildSpawnPass.critterRoom(level, top));
            RandomSource r = RandomSource.create(46);
            int made = 0;
            for (int i = 0; i < 8 && made == 0; i++) {
                made = WildSpawnPass.spawnCritters(level, WildSpawnPass.surface(level, top.getX() + 1 + i, top.getZ() + 2), r, WildSpawnPass.critterRoom(level, top));
            }
            check(made > 0 && critters(level, top) == made, "no small animals with the fish at their limit: " + made);
            check(WildSpawnPass.room(level, top) == 0, "the small animals count for the fish");
            // the other way round: the small animals at their limit, no fish
            for (Entity e : WildSpawnPass.wildAround(level, top, 40)) e.discard();
            var critter = ModEntities.WILD_CRITTER.get();
            for (int i = 0; i < critterLimit; i++) critter.spawn(level, top.offset(i % sx, -2, (i / sx) % sz), MobSpawnType.NATURAL);
            check(WildSpawnPass.critterRoom(level, top) == 0, "room for small animals at their limit: " + WildSpawnPass.critterRoom(level, top));
            check(WildSpawnPass.room(level, top) == fishLimit, "the small animals take the fish's room: " + WildSpawnPass.room(level, top));
            check(WildSpawnPass.tryCrittersFor(level, top, r) == 0, "the small animals' try went over its limit");
            var river = WildSpawnPass.fish(WildSpawnPass.ours(level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.RIVER)));
            check(!river.isEmpty() && river.stream().noneMatch(d -> d.type == critter), "river fish entries " + river);
            int fish = 0;
            for (int i = 0; i < 16 && fish == 0; i++) {
                fish = WildSpawnPass.spawnInColumn(level, WildSpawnPass.surface(level, top.getX() + 1 + i % 8, top.getZ() + 1 + i / 8), river, r, WildSpawnPass.room(level, top));
            }
            check(fish > 0, "no fish with the small animals at their limit");
            LOG.info("M46 own room: {} small animals beside {} fish; {} fish beside {} small animals", made, fishLimit, fish, critterLimit);
        } finally {
            clear(h, top, sx, sz, 3);
        }
        h.succeed();
    }

    /** The small animals' limit is kept over many tries (here set to 3; 0 spawns none). */
    @GameTest(template = EMPTY, batch = "bettacraft_m46_critter_limit", timeoutTicks = 100)
    public static void crittersKeepToTheirLimit(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        int sx = 10, sz = 6;
        BlockPos top = pool(h, sx, sz, 3, 3);
        int saved = ServerConfig.WILD_PASS_CRITTERS_PER_PLAYER.get();
        try {
            ServerConfig.WILD_PASS_CRITTERS_PER_PLAYER.set(3);
            RandomSource r = RandomSource.create(4603);
            int most = 0;
            for (int i = 0; i < 40; i++) {
                WildSpawnPass.spawnCritters(level, WildSpawnPass.surface(level, top.getX() + i % sx, top.getZ() + (i / sx) % sz), r, WildSpawnPass.critterRoom(level, top));
                most = Math.max(most, critters(level, top));
                check(critters(level, top) <= 3, "over the limit: " + critters(level, top));
            }
            check(most == 3, "the limit of 3 was not reached: " + most);
            for (Entity e : WildSpawnPass.wildAround(level, top, 40)) e.discard();
            ServerConfig.WILD_PASS_CRITTERS_PER_PLAYER.set(0);
            check(WildSpawnPass.critterRoom(level, top) == 0, "limit 0 leaves room");
            check(WildSpawnPass.tryCrittersFor(level, top, r) == 0, "limit 0 spawned");
        } finally {
            ServerConfig.WILD_PASS_CRITTERS_PER_PLAYER.set(saved);
            clear(h, top, sx, sz, 3);
        }
        h.succeed();
    }

    /** Water 3 deep beside water 11 deep: most tries pick the shallow part, a few the deep one; the groups land there. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void crittersPreferShallowWater(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        int sx = 12, sz = 6, shallow = 3, deep = 11;
        BlockPos top = pool(h, sx, sz, shallow, deep);
        try {
            List<BlockPos> shallowCols = new ArrayList<>(), deepCols = new ArrayList<>();
            for (int x = 0; x < sx; x++) {
                BlockPos s = WildSpawnPass.surface(level, top.getX() + x, top.getZ() + 2);
                check(s != null && s.getY() == top.getY(), "surface at " + x + ": " + s);
                int d = WildSpawnPass.depth(level, s);
                check(d == (x < sx / 2 ? shallow : deep), "depth at " + x + ": " + d);
                (x < sx / 2 ? shallowCols : deepCols).add(s);
            }
            RandomSource r = RandomSource.create(4604);
            int inShallow = 0, inDeep = 0, n = 400;
            for (int i = 0; i < n; i++) {
                List<BlockPos> cols = new ArrayList<>();
                // like the pass: a handful of random columns, some shallow, some deep
                for (int k = 0; k < 4; k++) cols.add(r.nextBoolean() ? shallowCols.get(r.nextInt(shallowCols.size())) : deepCols.get(r.nextInt(deepCols.size())));
                BlockPos c = WildSpawnPass.pickCritterColumn(level, cols, r);
                if (c == null) continue;
                if (WildSpawnPass.depth(level, c) <= WildSpawnPass.SHALLOW_MAX) inShallow++;
                else inDeep++;
            }
            LOG.info("M46 column picks: {} shallow, {} deep of {}", inShallow, inDeep, n);
            check(inShallow > n * 0.7, "most should go shallow: " + inShallow + " of " + n);
            check(inDeep > 0, "deep ground stays possible: " + inDeep);
            // only deep columns: still a column (deep ground is possible)
            check(WildSpawnPass.pickCritterColumn(level, deepCols, r) != null, "no column in deep water");
            // the groups really land on the shallow bottom
            int made = 0;
            for (int i = 0; i < 6; i++) {
                List<BlockPos> cols = List.of(shallowCols.get(i % shallowCols.size()), deepCols.get(i % deepCols.size()));
                BlockPos c = WildSpawnPass.pickCritterColumn(level, cols, RandomSource.create(i));
                made += WildSpawnPass.spawnCritters(level, c, r, WildSpawnPass.critterRoom(level, top));
            }
            int low = 0, all = 0;
            for (WildCritterEntity e : level.getEntitiesOfClass(WildCritterEntity.class, new AABB(top).inflate(40))) {
                all++;
                if (e.getY() >= top.getY() - shallow + 0.5) low++;
            }
            LOG.info("M46 spawned {} small animals, {} of {} on the shallow bottom", made, low, all);
            check(made > 0 && low * 2 > all, "most groups should be in the shallow part: " + low + " of " + all);
        } finally {
            clear(h, top, sx, sz, deep);
        }
        h.succeed();
    }

    /** features.invertebrates.wildSpawns off: the small animals' try spawns nothing. */
    @GameTest(template = EMPTY, batch = "bettacraft_m46_critters_off", timeoutTicks = 100)
    public static void noCrittersWithTheirSwitchOff(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        int sx = 10, sz = 6;
        BlockPos top = pool(h, sx, sz, 3, 3);
        boolean saved = Features.own(Feature.INVERTEBRATES_WILD);
        try {
            Features.set(Feature.INVERTEBRATES_WILD, false);
            RandomSource r = RandomSource.create(4605);
            int made = 0;
            for (int i = 0; i < 10; i++) {
                made += WildSpawnPass.spawnCritters(level, WildSpawnPass.surface(level, top.getX() + i, top.getZ() + 2), r, 12);
                made += WildSpawnPass.tryCrittersFor(level, top, r);
            }
            check(made == 0 && critters(level, top) == 0, "small animals spawned with the switch off: " + made);
            Features.set(Feature.INVERTEBRATES_WILD, true);
            for (int i = 0; i < 10 && made == 0; i++) made += WildSpawnPass.spawnCritters(level, WildSpawnPass.surface(level, top.getX() + i, top.getZ() + 2), r, 12);
            check(made > 0, "with the switch on they spawn again");
        } finally {
            Features.set(Feature.INVERTEBRATES_WILD, saved);
            clear(h, top, sx, sz, 3);
        }
        h.succeed();
    }
}
