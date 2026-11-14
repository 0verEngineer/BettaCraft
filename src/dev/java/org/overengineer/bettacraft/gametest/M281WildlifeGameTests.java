package org.overengineer.bettacraft.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.world.WildSpawnPass;
import org.overengineer.bettacraft.world.Wildlife;

/**
 * M28.1 GameTests: the entity tags of all and of the wild animals, the share of the animals of the mod in the
 * water-ambient spawn lists of their biomes (logged as a measurement, also with the fish mods of {@code -Paquatic}), the own
 * spawn pass (spawns a group in fitting water, keeps to the limit per player, never in a tank) and the command
 * {@code /bettacraft wildlife}.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class M281WildlifeGameTests {
    private static final String EMPTY = "empty_32x10x32";
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("bettacraft-m281");

    private M281WildlifeGameTests() {}

    private static void check(boolean ok, String what) {
        if (!ok) throw new GameTestAssertException(what);
    }

    /** A pool {@code size} wide, 4 deep, its surface at sea level, at the test's x/z (absolute; the test area lies lower). */
    private static BlockPos pool(GameTestHelper h, int size) {
        ServerLevel level = h.getLevel();
        BlockPos rel = h.absolutePos(new BlockPos(4, 0, 4));
        int sea = level.getSeaLevel();
        BlockPos min = new BlockPos(rel.getX(), sea - 3, rel.getZ());
        for (int x = -1; x <= size; x++)
            for (int z = -1; z <= size; z++) {
                level.setBlock(min.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
                for (int y = 0; y < 4; y++) {
                    boolean wall = x < 0 || z < 0 || x == size || z == size;
                    level.setBlock(min.offset(x, y, z), wall ? Blocks.GLASS.defaultBlockState() : Blocks.WATER.defaultBlockState(), 2);
                }
                for (int y = 4; y < 8; y++) level.setBlock(min.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            }
        return min;
    }

    private static void clear(GameTestHelper h, BlockPos min, int size) {
        ServerLevel level = h.getLevel();
        for (Entity e : level.getEntities((Entity) null, new AABB(min).inflate(size + 8), e -> e.getType().is(ModTags.ALL_ANIMALS))) e.discard();
        for (int x = -1; x <= size; x++)
            for (int z = -1; z <= size; z++)
                for (int y = -1; y < 8; y++) level.setBlock(min.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
    }

    private static List<MobSpawnSettings.SpawnerData> ours(GameTestHelper h, ResourceKey<Biome> biome) {
        return WildSpawnPass.ours(h.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(biome));
    }

    // ------------------------------------------------------------------ tags

    /** #bettacraft:all_animals holds every animal of the mod, #bettacraft:wild_animals the ones that spawn. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void animalTagsHoldEveryAnimal(GameTestHelper h) {
        check(ModEntities.BETTA.get().is(ModTags.ALL_ANIMALS) && ModEntities.DWARF_FROG.get().is(ModTags.ALL_ANIMALS)
                && ModEntities.WILD_CRITTER.get().is(ModTags.ALL_ANIMALS), "betta, frog or small animals missing in all_animals");
        for (var e : ModEntities.TANKMATES.entrySet()) {
            check(e.getValue().get().is(ModTags.ALL_ANIMALS), e.getKey().id() + " missing in all_animals");
            check(e.getValue().get().is(ModTags.WILD_ANIMALS) == Wildlife.spawnsWild(e.getKey()), e.getKey().id() + ": wild_animals wrong");
        }
        check(ModEntities.TANKMATES.get(TankmateSpecies.LINED_SEAHORSE).get().is(ModTags.WILD_ANIMALS), "seahorses spawn wild");
        check(!net.minecraft.world.entity.EntityType.COD.is(ModTags.ALL_ANIMALS), "cod is no animal of the mod");
        h.succeed();
    }

    // ------------------------------------------------------------------ measurement

    /**
     * The share of the animals of the mod in the water-ambient spawn lists of their biomes, logged (with {@code -Paquatic}
     * the fish mods add their own entries). Every fitting biome has some; without other mods the share stays modest.
     */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void spawnSharesOfTheWaters(GameTestHelper h) {
        var biomes = h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        List<ResourceKey<Biome>> keys = List.of(Biomes.RIVER, Biomes.SWAMP, Biomes.MANGROVE_SWAMP, Biomes.JUNGLE, Biomes.SPARSE_JUNGLE,
                Biomes.BAMBOO_JUNGLE, Biomes.BEACH, Biomes.WARM_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.OCEAN, Biomes.DEEP_LUKEWARM_OCEAN);
        boolean foreign = false;
        List<String> lines = new ArrayList<>();
        for (var key : keys) {
            var list = biomes.getHolderOrThrow(key).value().getMobSettings().getMobs(MobCategory.WATER_AMBIENT).unwrap();
            int total = 0, ours = 0;
            Map<String, Integer> byMod = new LinkedHashMap<>();
            for (var d : list) {
                int w = d.getWeight().asInt();
                total += w;
                String ns = BuiltInRegistries.ENTITY_TYPE.getKey(d.type).getNamespace();
                byMod.merge(ns, w, Integer::sum);
                if (d.type.is(ModTags.ALL_ANIMALS)) ours += w;
                if (!ns.equals("minecraft") && !ns.equals(BettaCraft.MOD_ID)) foreign = true;
            }
            lines.add(String.format(java.util.Locale.ROOT, "%-28s ours %4d of %4d = %5.1f %%  %s", key.location(), ours, total,
                    total == 0 ? 0.0 : 100.0 * ours / total, byMod));
            if (key != Biomes.OCEAN) check(ours > 0, "no animals of the mod spawn in " + key.location());
        }
        LOG.info("M28.1 water-ambient spawn weights per biome (other mods: {}):\n{}", foreign, String.join("\n", lines));
        h.succeed();
    }

    // ------------------------------------------------------------------ the own spawn pass

    /** One pass in river water spawns a group of river animals, wild, inside the pool, and never more than the room. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void spawnPassSpawnsAGroupInFittingWater(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        int size = 8;
        BlockPos min = pool(h, size);
        try {
            var river = ours(h, Biomes.RIVER);
            check(!river.isEmpty(), "no river animals");
            RandomSource r = RandomSource.create(281);
            int made = 0;
            for (int i = 0; i < 12 && made == 0; i++) {
                BlockPos surface = WildSpawnPass.surface(level, min.getX() + 2 + i % 4, min.getZ() + 2 + i / 4);
                check(surface != null && surface.getY() == min.getY() + 3, "the pool surface was not found: " + surface);
                made = WildSpawnPass.spawnInColumn(level, surface, river, r, 4);
            }
            check(made > 0 && made <= 4, "the pass spawned " + made + " animals (room 4)");
            var spawned = WildSpawnPass.wildAround(level, min, 16);
            check(spawned.size() == made, "wild animals around: " + spawned.size() + " of " + made);
            for (Entity e : spawned) {
                check(Wildlife.isWildAnimal(e), e + " is not wild");
                check(river.stream().anyMatch(d -> d.type == e.getType()), e.getType() + " is no river animal");
                BlockPos p = e.blockPosition();
                check(p.getX() >= min.getX() && p.getX() < min.getX() + size && p.getZ() >= min.getZ() && p.getZ() < min.getZ() + size
                        && level.getFluidState(p).isSource(), e + " spawned outside the water");
            }
            check(WildSpawnPass.nearby(level, min) == made, "nearby count");
            check(WildSpawnPass.spawnInColumn(level, WildSpawnPass.surface(level, min.getX() + 3, min.getZ() + 3), river, r, 0) == 0, "spawned without room");
        } finally {
            clear(h, min, size);
        }
        h.succeed();
    }

    /** The limit per player: with as many wild animals nearby as allowed the pass waits; in a tank it never spawns. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void spawnPassKeepsToItsLimitAndOutOfTanks(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        int size = 8;
        BlockPos min = pool(h, size);
        try {
            int limit = WildSpawnPass.perPlayer();
            check(limit > 0 && WildSpawnPass.room(level, min) == limit, "room before: " + WildSpawnPass.room(level, min));
            var type = ModEntities.WILD_CRITTER.get();
            for (int i = 0; i < limit; i++) type.spawn(level, min.offset(i % size, 0, i / size), net.minecraft.world.entity.MobSpawnType.NATURAL);
            check(WildSpawnPass.room(level, min) == 0, "room with " + limit + " wild animals: " + WildSpawnPass.room(level, min));
            check(WildSpawnPass.tryFor(level, min, RandomSource.create(3)) == 0, "the pass spawned over its limit");
            for (Entity e : WildSpawnPass.wildAround(level, min, 16)) e.discard();
            // a tank at sea level beside the pool: its water (tank blocks hold vanilla water) never gets wild animals
            BlockPos t = min.offset(size + 3, 0, 0);
            for (int a = 0; a < 3; a++)
                for (int b = 0; b < 2; b++)
                    for (int c = 0; c < 3; c++) {
                        level.setBlock(t.offset(a, b, c), ModBlocks.TANK.get().defaultBlockState(), 3);
                        TankMultiblock.onTankBlockAdded(level, t.offset(a, b, c), null);
                    }
            var controller = TankMultiblock.getController(level, t);
            check(controller != null, "tank did not form");
            org.overengineer.bettacraft.tank.TankInteractions.changeWater(level, controller, 18000, 0F);
            var all = new ArrayList<MobSpawnSettings.SpawnerData>();
            all.addAll(ours(h, Biomes.RIVER));
            all.addAll(ours(h, Biomes.SWAMP));
            RandomSource r = RandomSource.create(7);
            int tried = 0;
            for (int i = 0; i < 30; i++) {
                BlockPos surface = WildSpawnPass.surface(level, t.getX() + i % 3, t.getZ() + (i / 3) % 3);
                if (surface == null) continue;
                tried++;
                WildSpawnPass.spawnInColumn(level, surface, all, r, 6);
            }
            check(tried > 0, "the pass did not even look at the tank water");
            check(WildSpawnPass.wildAround(level, t.offset(1, 1, 1), 3).isEmpty(), "wild animals spawned in a tank");
            for (int a = 0; a < 3; a++)
                for (int b = 0; b < 2; b++)
                    for (int c = 0; c < 3; c++) level.setBlock(t.offset(a, b, c), Blocks.AIR.defaultBlockState(), 3);
        } finally {
            clear(h, min, size);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ the command

    /** /bettacraft wildlife (info, list, glow) runs and finds the animals outside tanks. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void wildlifeCommandListsAndGlows(GameTestHelper h) throws CommandSyntaxException {
        ServerLevel level = h.getLevel();
        int size = 6;
        BlockPos min = pool(h, size);
        try {
            for (int i = 0; i < 3; i++) ModEntities.WILD_CRITTER.get().spawn(level, min.offset(i, 0, 1), net.minecraft.world.entity.MobSpawnType.NATURAL);
            CommandSourceStack src = level.getServer().createCommandSourceStack().withLevel(level).withPosition(Vec3.atCenterOf(min.offset(2, 3, 2)))
                    .withPermission(4).withSuppressedOutput();
            var d = level.getServer().getCommands().getDispatcher();
            d.execute("bettacraft wildlife", src);
            check(d.execute("bettacraft wildlife list 12", src) >= 3, "the list misses the small animals");
            check(d.execute("bettacraft wildlife glow 12", src) >= 3, "nothing glows");
            for (Entity e : WildSpawnPass.wildAround(level, min, 12)) check(((LivingEntity) e).hasEffect(MobEffects.GLOWING), e + " does not glow");
            var noOp = level.getServer().createCommandSourceStack().withLevel(level).withPermission(0).withSuppressedOutput();
            boolean refused;
            try {
                d.execute("bettacraft wildlife", noOp);
                refused = false;
            } catch (CommandSyntaxException e) {
                refused = true;
            }
            check(refused, "the command runs without OP");
        } finally {
            clear(h, min, size);
        }
        h.succeed();
    }
}
