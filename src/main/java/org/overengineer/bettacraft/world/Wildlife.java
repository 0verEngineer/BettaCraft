package org.overengineer.bettacraft.world;

import java.util.List;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M22.3: the animals of the wild. Where the tankmates, the dwarf clawed frog and the small animals live in the world
 * (the same waters as the aquarium net, {@link NetCatching}), the spawn rules that keep them rare and in small shoals
 * (a cap of wild ones nearby, never inside a tank), the rare beautiful wild colours and the spontaneous mutations of the
 * animals without their own genetics.
 */
public final class Wildlife {
    /** Radius around a spawn in which the wild animals of the mod are counted. */
    public static final double NEARBY = 32;

    private Wildlife() {}

    // ------------------------------------------------------------------ colours and mutations

    /** Chance that a wild animal shows a bred colour openly ({@code wildlife.beautifulWildChance}, {@code features.wildColors}). */
    public static double beautifulChance() {
        return CommonConfig.wildColors() ? ServerConfig.getOr(ServerConfig.WILD_BEAUTIFUL_CHANCE, 0.03) : 0;
    }

    /** Chance per gene and birth for the animals without their own genetics ({@code wildlife.otherMutationChance}). */
    public static double mutationChance() {
        return CommonConfig.mutations() ? ServerConfig.getOr(ServerConfig.WILD_MUTATION_CHANCE, 0.003) : 0;
    }

    /** Genes of a wild betta: wild colours with hidden carriers, rarely a domestic-looking fish. */
    public static BettaGenotype betta(RandomSource r) {
        return r.nextDouble() < beautifulChance() ? BettaGenotype.randomDomestic(r) : BettaGenotype.randomWild(r);
    }

    /**
     * Genes of a wild tankmate: the wild type, now and then carrying a bred variety hidden (8 %), rarely pure-bred
     * (visible): a named strain for guppies and endlers, a bred variety for the others.
     */
    public static TankmateGenes tankmate(TankmateSpecies s, RandomGenerator r) {
        if (r.nextDouble() < beautifulChance()) {
            if (TankmateGenes.livebearer(s) && CommonConfig.livebearerGenetics()) {
                var strains = org.overengineer.bettacraft.genetics.livebearer.LivebearerStrain.of(s == TankmateSpecies.ENDLER_GUPPY);
                if (!strains.isEmpty()) return TankmateGenes.strain(s, strains.get(r.nextInt(strains.size())));
            } else if (s.varieties().size() > 1) {
                return TankmateGenes.pure(s, s.variety(1 + r.nextInt(s.varieties().size() - 1)));
            }
        }
        return TankmateGenes.wild(s, 0.08, r);
    }

    /** Genes of a wild shrimp, crayfish or snail ({@link NetCatching#wildGenes}), rarely showing a colour line. */
    public static CritterGenes critter(CritterSpecies s, RandomGenerator r) {
        int loci = s.loci().length;
        if (!CommonConfig.snailColors() && s == CritterSpecies.RAMSHORN) loci = 2;
        if (loci > 0 && r.nextDouble() < beautifulChance()) {
            return CritterGenes.pure(r.nextInt(loci), 0.25F + r.nextFloat() * 0.3F);
        }
        return NetCatching.wildGenes(s, r);
    }

    /** A young tankmate without its own genetics mutates now and then: an allele becomes another variety. */
    public static TankmateGenes mutate(TankmateSpecies s, TankmateGenes g, RandomGenerator r) {
        double p = mutationChance();
        if (p <= 0 || TankmateGenes.livebearer(s) || s.varieties().size() < 2) return g;
        String a = g.a(), b = g.b();
        if (r.nextDouble() < p) a = other(s, a, r);
        if (r.nextDouble() < p) b = other(s, b, r);
        return a.equals(g.a()) && b.equals(g.b()) ? g : new TankmateGenes(a, b, g.lb());
    }

    private static String other(TankmateSpecies s, String v, RandomGenerator r) {
        List<String> all = s.varieties();
        int i = all.indexOf(v);
        int j = r.nextInt(all.size() - 1);
        if (j >= i && i >= 0) j++;
        return all.get(Math.min(j, all.size() - 1));
    }

    // ------------------------------------------------------------------ where they live

    /** The kind of water of a biome for the spawns (the biome alone; caves are left out). */
    public static NetCatching.Water water(Holder<Biome> b) {
        return BiomeWaters.water(b, false); // M25.1: biome tags, also biome mods
    }

    /** Spawn weight of a tankmate in this water ({@code spawn.wildFishWeight} tenths of its net weight), 0 = not there. */
    public static int fishWeight(TankmateSpecies s, NetCatching.Water w) {
        Double net = NetCatching.fishTable(w).get(s);
        if (net == null) return 0;
        int f = CommonConfig.getOr(CommonConfig.SPAWN_FISH_WEIGHT, 5);
        return f <= 0 ? 0 : Math.max(1, (int) Math.round(net * f / 10.0));
    }

    /** Smallest and largest shoal of a species that spawns together. */
    public static int[] group(TankmateSpecies s) {
        if (!s.shoaling()) return new int[]{1, 1};
        return new int[]{3, Math.max(3, Math.min(6, s.shoal()))};
    }

    /** Spawn weight of the dwarf clawed frog (swamps and jungle water). */
    public static int frogWeight(NetCatching.Water w) {
        int f = CommonConfig.getOr(CommonConfig.SPAWN_FISH_WEIGHT, 5);
        if (f <= 0) return 0;
        return switch (w) {
            case SWAMP -> Math.max(1, Math.round(5 * f / 10F));
            case JUNGLE -> Math.max(1, Math.round(8 * f / 10F));
            default -> 0;
        };
    }

    /** Spawn weight of the small animals in this water (one entity type, the species is picked per group). */
    public static int critterWeight(NetCatching.Water w) {
        if (w == NetCatching.Water.NONE || w == NetCatching.Water.CAVE) return 0;
        boolean any = false;
        for (var c : NetCatching.table(w, false, false, false)) if (c.species() != null) any = true;
        return any ? CommonConfig.getOr(CommonConfig.SPAWN_CRITTER_WEIGHT, 6) : 0;
    }

    /** The small animals of a water with their weights (the net's table without the frog). */
    public static List<NetCatching.Catch> critterTable(NetCatching.Water w, boolean plants, boolean hard) {
        return NetCatching.table(w, plants, hard, false).stream().filter(c -> c.species() != null).toList();
    }

    @Nullable
    public static CritterSpecies pickCritter(NetCatching.Water w, boolean plants, boolean hard, RandomGenerator r) {
        var c = NetCatching.roll(critterTable(w, plants, hard), r);
        return c == null ? null : c.species();
    }

    /** How many animals of one critter species spawn together. */
    public static int[] critterGroup(CritterSpecies s) {
        return switch (s.group()) {
            case SHRIMP -> s == CritterSpecies.CLEANER_SHRIMP ? new int[]{1, 2} : s == CritterSpecies.SULAWESI_SHRIMP ? new int[]{2, 4} : new int[]{3, 6};
            case CRAYFISH -> new int[]{1, 2};
            case SNAIL -> s == CritterSpecies.APPLE_SNAIL || s == CritterSpecies.ASSASSIN_SNAIL || s == CritterSpecies.TYLOMELANIA ? new int[]{1, 2} : new int[]{2, 4};
        };
    }

    // ------------------------------------------------------------------ spawn rules

    /** Natural spawns (and new chunks) are limited; spawn eggs, buckets and commands are not. */
    static boolean natural(MobSpawnType type) {
        return type == MobSpawnType.NATURAL || type == MobSpawnType.CHUNK_GENERATION;
    }

    /** Never inside a tank (tank blocks hold water). */
    public static boolean inTank(ServerLevelAccessor level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof TankBlock || level.getBlockState(pos.above()).getBlock() instanceof TankBlock
                || level.getBlockState(pos.below()).getBlock() instanceof TankBlock;
    }

    /** Wild animals of the mod of this kind within {@link #NEARBY} blocks. */
    public static int nearby(ServerLevelAccessor level, BlockPos pos, Class<? extends Entity> type, java.util.function.Predicate<Entity> wild) {
        AABB box = new AABB(pos).inflate(NEARBY);
        return level.getEntitiesOfClass(type, box, wild::test).size();
    }

    /** Spawn rule of the wild tankmates and frogs: open surface water, no tank, few of them nearby. */
    public static boolean checkFish(EntityType<?> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (!natural(spawnType)) return true;
        if (!CommonConfig.wildFish()) return false;
        if (!level.getFluidState(pos).is(FluidTags.WATER) || inTank(level, pos)) return false;
        int sea = level.getSeaLevel();
        if (pos.getY() > sea || pos.getY() < sea - 13) return false;
        if (!level.getFluidState(pos.below()).is(FluidTags.WATER) && !level.getFluidState(pos.above()).is(FluidTags.WATER)) return false;
        int cap = ServerConfig.getOr(ServerConfig.WILD_FISH_NEARBY, 6);
        return nearby(level, pos, net.minecraft.world.entity.animal.AbstractFish.class, Wildlife::isWildFish) < cap;
    }

    /** Spawn rule of the wild small animals: water right above a solid bottom, near the surface, no tank, few nearby. */
    public static boolean checkCritter(EntityType<?> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (!natural(spawnType)) return true;
        if (!CommonConfig.wildCritters()) return false;
        if (!level.getFluidState(pos).is(FluidTags.WATER) || inTank(level, pos)) return false;
        if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) return false;
        int sea = level.getSeaLevel();
        if (pos.getY() > sea || pos.getY() < sea - 24) return false;
        int cap = ServerConfig.getOr(ServerConfig.WILD_CRITTERS_NEARBY, 8);
        return nearby(level, pos, org.overengineer.bettacraft.entity.critter.WildCritterEntity.class, e -> true) < cap;
    }

    /** A wild tankmate or frog that spawned by itself (not from a bucket, not in a tank). */
    public static boolean isWildFish(Entity e) {
        if (e instanceof org.overengineer.bettacraft.entity.tankmate.TankmateEntity m) return m.isWild();
        if (e instanceof org.overengineer.bettacraft.entity.frog.DwarfFrogEntity f) return f.isWild();
        return false;
    }
}
