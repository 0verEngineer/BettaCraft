package org.overengineer.bettacraft.terrarium;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.critters.TankTerrain;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.interior.InteriorGround;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.overengineer.bettacraft.tank.interior.Substrate;

/**
 * M31: breeding of the egg layers in a tank, every care step (after the needs): part b the turtles, part c the dart frogs,
 * the geckos and the corn snake (the horned frog does not breed in a tank, {@link HerpSpecies#breeds}). The same way for all:
 * the dart frogs lay onto moss, leaves or soil ({@code #bettacraft:frog_nest_ground}), a mate of the same variety only
 * ({@link HerpSpecies#pureVarieties}: the three species do not cross), and their young hatch as tadpoles into the water;
 * the geckos lay two eggs; the sex is genetic (half and half) unless the species has {@link HerpSpecies#tsd} (the leopard
 * gecko: pattern FMF); a hatchling of a species with morphs shows one of its own now and then ({@link HerpSpecies#morphChance}).
 * <ol>
 *     <li><b>Laying:</b> a grown female with a grown male of her species in the tank, fed within two days, not brumating,
 *     without a harmful problem and at most a little stress, walks to a nest: a spot on the land whose top is
 *     <b>sand or soil</b> ({@code #bettacraft:turtle_nest_ground}; also the floor substrate where it is land) with room
 *     above. There she lays {@link HerpSpecies#eggsMin}-{@link HerpSpecies#eggsMax} eggs: a {@link TurtleEggsBlock} in
 *     the interior and a {@link HerpClutch} in the tank. Then she waits {@code terrarium.clutchIntervalDays}. No nest
 *     (stone, gravel, no land): no eggs (a log line once a day). She does not lay while the tank would be overcrowded
 *     with the hatchlings (stocking.overcrowdedPerWaterBlock over water and land).</li>
 *     <li><b>Incubation:</b> the nest has the temperature of the ground (the heat mats warm it, else the air); see
 *     {@link EggSex}. Water over the nest drowns the eggs; above 36 °C they die.</li>
 *     <li><b>Hatching:</b> each egg becomes a hatchling of the mother's or the father's variety; its sex comes from the mean
 *     nest temperature of the thermosensitive period (a fixed seed per clutch). Only as many hatch as the tank's hard
 *     limit (water and land) and the entity limit allow; the others stay in their eggs and die.</li>
 * </ol>
 * The egg block is checked every ten seconds: a player who broke it in the interior destroyed the clutch.
 * Switches: {@code features.turtleBreeding}, part c {@code features.herpBreeding} (off: no new clutches; laid ones still hatch).
 */
public final class HerpBreeding {
    /** Nest reach of a female (tank blocks, flat). */
    public static final double NEST_REACH = 0.3;
    /** How much stress a female may have to lay. */
    public static final double MAX_STRESS = 0.45;

    private HerpBreeding() {}

    public static double speed() {
        return ServerConfig.getOr(ServerConfig.TERRA_INCUBATION_SPEED, 1.0);
    }

    public static double intervalDays() {
        return ServerConfig.getOr(ServerConfig.TERRA_CLUTCH_DAYS, 4.0);
    }

    /** The temperature in the land of the tank (the nest): the ground with heat mats, else the air, else the water. */
    public static double nestTemp(TankData t) {
        if (t.hasClimate()) return Float.isFinite(t.groundTemp()) ? t.groundTemp() : t.airTemp();
        return t.waterTemp();
    }

    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        if (!TankClimate.enabled() || days <= 0) return;
        TankData t = c.getTank();
        if (!t.herpClutches().isEmpty()) incubate(level, c, days);
        if (t.inhabitants().isEmpty()) return;
        List<Entity> live = TankInhabitants.liveInhabitants(level, t);
        List<HerpEntity> turtles = new ArrayList<>();
        for (Entity e : live) if (e instanceof HerpEntity te && te.isAlive()) turtles.add(te);
        if (turtles.size() < 2) return;
        long now = level.getGameTime();
        TerrariumNeeds.Conditions cond = TankClimate.conditions(t);
        for (HerpEntity f : turtles) {
            if (!breedingOn(f.species())) continue;
            if (!f.layReady(now)) {
                if (f.nestTarget() != null && f.isBrumating()) f.forgetNest();
                continue;
            }
            if (!hasMale(turtles, f)) continue;
            TerrariumNeeds.Assessment a = f.adjust(f.terrariumNeeds().assess(cond));
            if (a.harmful() || a.stress() > MAX_STRESS || !f.fedWithin(2.0)) {
                f.forgetNest();
                continue;
            }
            if (f.nestTarget() == null) {
                if (!room(live, t, f.species(), f.species().eggsMax())) {
                    if (c.logDue("turtle_crowded:" + f.species().id(), now, 24000)) TankCare.log(level, c, "log.bettacraft.turtle.too_crowded", TankCare.nameArg(f));
                    continue;
                }
                Nest n = findNest(level, t, f.getRandom(), f.species());
                if (n == null) {
                    if (c.logDue("turtle_no_nest:" + f.species().id(), now, 24000)) {
                        TankCare.log(level, c, f.species().body() == HerpSpecies.Body.FROG ? "log.bettacraft.herp.no_nest_frog" : "log.bettacraft.turtle.no_nest",
                                TankCare.nameArg(f));
                    }
                    continue;
                }
                f.goNest(level, t, n.world());
                continue;
            }
            if (LandWalker.arrivedAt(f, f.nestTarget(), NEST_REACH)) {
                Nest n = nestAt(level, t, f.nestTarget(), f.species());
                if (n == null) n = findNest(level, t, f.getRandom(), f.species());
                if (n != null && lay(level, c, f, mateOf(turtles, f), n)) continue;
                f.forgetNest();
            } else if (f.nestTimeout()) {
                f.forgetNest();
            }
        }
    }

    /** Whether this species breeds now (its switches and whether it breeds in a tank at all). */
    public static boolean breedingOn(HerpSpecies s) {
        if (!s.breeds()) return false;
        return s.turtle() ? CommonConfig.turtles() && CommonConfig.turtleBreeding() : CommonConfig.herps() && CommonConfig.herpBreeding();
    }

    /** The egg block of a species: the jelly clutch of the frogs, the leathery eggs of the reptiles. */
    public static TurtleEggsBlock eggBlock(HerpSpecies s) {
        return s.body() == HerpSpecies.Body.FROG ? ModBlocks.FROG_EGGS.get() : ModBlocks.TURTLE_EGGS.get();
    }

    /** The blocks the eggs are laid onto: moss, leaves, soil for the frogs; sand and soil for the reptiles. */
    public static net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> nestGround(HerpSpecies s) {
        return s.body() == HerpSpecies.Body.FROG ? ModTags.FROG_NEST_GROUND : ModTags.TURTLE_NEST_GROUND;
    }

    private static boolean hasMale(List<HerpEntity> all, HerpEntity f) {
        return mateOf(all, f) != null;
    }

    @Nullable
    private static HerpEntity mateOf(List<HerpEntity> all, HerpEntity f) {
        for (HerpEntity m : all) {
            if (m == f || m.species() != f.species() || m.isFemale() || m.isBaby() || m.isBrumating()) continue;
            if (f.species().pureVarieties() && m.variety() != f.variety()) continue; // part c: the dart frog species do not cross
            return m;
        }
        return null;
    }

    /** Whether the tank stays below the overcrowding with {@code eggs} more hatchlings of this species. */
    public static boolean room(List<Entity> live, TankData t, HerpSpecies s, int eggs) {
        double blocks = t.waterBlocks() + TerrariumAnimals.landBlocks(t);
        double stock = TankInhabitants.stocking(live, t) + eggs * s.stock() * 0.1 + clutchStock(t);
        int per = ServerConfig.getOr(ServerConfig.TANK_OVERCROWDED_PER_WATER_BLOCK, 2);
        return stock <= per * blocks + 1e-6;
    }

    /** The hatchlings still in their eggs, as stocking. */
    public static double clutchStock(TankData t) {
        double s = 0;
        for (HerpClutch k : t.herpClutches()) s += k.eggs * k.species.stock() * 0.1;
        return s;
    }

    private static boolean lay(ServerLevel level, TankBlockEntity c, HerpEntity f, @Nullable HerpEntity father, Nest n) {
        ServerLevel interior = InteriorManager.interiorLevel(level.getServer());
        if (interior == null || father == null) return false;
        TankData t = c.getTank();
        RandomSource r = f.getRandom();
        HerpSpecies s = f.species();
        int eggs = s.eggsMin() + r.nextInt(s.eggsMax() - s.eggsMin() + 1);
        interior.getChunk(n.interior()); // loaded (the nest was just checked; a chunk can unload in between)
        interior.setBlock(n.interior(), eggBlock(s).withEggs(eggs), 3);
        HerpClutch k = new HerpClutch(t.nextHerpClutchId(), s, eggs, n.world(), n.interior(), r.nextLong());
        k.motherVariety = f.variety();
        k.fatherVariety = father.variety();
        k.generation = Math.max(f.generation(), father.generation()) + 1;
        t.herpClutches().add(k);
        f.laid(level.getGameTime(), intervalDays());
        TankCare.log(level, c, "log.bettacraft.turtle.laid", TankCare.nameArg(f), String.valueOf(eggs));
        c.setChanged();
        return true;
    }

    // ------------------------------------------------------------------ incubation and hatching

    private static void incubate(ServerLevel level, TankBlockEntity c, double days) {
        TankData t = c.getTank();
        ServerLevel interior = InteriorManager.interiorLevel(level.getServer());
        long now = level.getGameTime();
        boolean check = now % 200 < 20;
        double temp = nestTemp(t), surface = t.waterMb() <= 0 ? Double.NEGATIVE_INFINITY : t.waterSurfaceY();
        for (var it = t.herpClutches().iterator(); it.hasNext(); ) {
            HerpClutch k = it.next();
            if (check && interior != null && interior.isLoaded(k.interiorPos()) && !interior.getBlockState(k.interiorPos()).is(eggBlock(k.species))) {
                it.remove();
                TankCare.log(level, c, "log.bettacraft.turtle.eggs_gone", "@" + k.species.translationKey());
                c.setChanged();
                continue;
            }
            if (k.y < surface - 0.02) {
                remove(interior, k);
                it.remove();
                TankCare.log(level, c, "log.bettacraft.turtle.eggs_drowned", "@" + k.species.translationKey());
                c.setChanged();
                continue;
            }
            k.incubate(temp, days, speed());
            if (k.cooked()) {
                remove(interior, k);
                it.remove();
                TankCare.log(level, c, "log.bettacraft.turtle.eggs_cooked", "@" + k.species.translationKey());
                c.setChanged();
                continue;
            }
            if (k.ready()) {
                hatch(level, c, k);
                remove(interior, k);
                it.remove();
                c.setChanged();
            }
        }
    }

    private static void remove(@Nullable ServerLevel interior, HerpClutch k) {
        if (interior == null) return;
        interior.getChunk(k.interiorPos()); // the egg block must go even while no player looks into the interior
        if (interior.getBlockState(k.interiorPos()).is(eggBlock(k.species))) {
            interior.removeBlock(k.interiorPos(), false);
        }
    }

    /** Hatches a clutch: the young by the nest temperature (as many as fit). Returns how many hatched. */
    public static int hatch(ServerLevel level, TankBlockEntity c, HerpClutch k) {
        TankData t = c.getTank();
        List<Entity> live = TankInhabitants.liveInhabitants(level, t);
        int maxEntities = ServerConfig.getOr(ServerConfig.TANK_MAX_ENTITIES, 64);
        int cap = TankInhabitants.hardCap(t.waterBlocks() + TerrariumAnimals.landBlocks(t));
        double stock = TankInhabitants.stocking(live, t);
        RandomSource r = RandomSource.create(k.seed);
        HerpSpecies s = k.species;
        double female = k.femaleShare();
        int females = 0, males = 0;
        // part c: dart frog tadpoles go into the water (no water: they die)
        TankTerrain terrain = s.tadpoles() ? new TankTerrain(t, level.getServer(), InteriorRegions.get(level.getServer()).get(t.regionId()), level.isDay()) : null;
        if (terrain != null && !Double.isFinite(terrain.surface())) {
            TankCare.log(level, c, "log.bettacraft.herp.no_tadpole_water", "@" + s.translationKey());
            return 0;
        }
        for (int i = 0; i < k.eggs; i++) {
            boolean isFemale = r.nextDouble() < female;
            int variety = s.pureVarieties() ? k.motherVariety : r.nextBoolean() ? k.motherVariety : k.fatherVariety;
            boolean morph = s.morphChance() > 0 && s.varieties().size() > s.wildVarieties() && r.nextDouble() < s.morphChance();
            if (morph) variety = s.wildVarieties() + r.nextInt(s.varieties().size() - s.wildVarieties()); // part c: a new morph
            double weight = s.stock() * 0.1;
            if (live.size() + females + males >= maxEntities || stock + weight > cap + 1e-6) continue;
            HerpEntity y = ModEntities.herp(s).create(level);
            if (y == null) continue;
            y.makeHatchling();
            y.setFemale(isFemale);
            y.setVariety(variety);
            y.setGeneration(k.generation);
            Vec3 at = terrain != null ? waterSpot(t, terrain, r) : null;
            if (at != null) y.moveTo(at.x, at.y, at.z, r.nextFloat() * 360F, 0F);
            else y.moveTo(k.x + (r.nextDouble() - 0.5) * 0.12, k.y + 0.01, k.z + (r.nextDouble() - 0.5) * 0.12, r.nextFloat() * 360F, 0F);
            TankInhabitants.adopt(y, c);
            if (!level.addFreshEntity(y)) continue;
            y.setFromBucket(false);
            t.inhabitants().add(y.getUUID());
            if (morph) TankCare.log(level, c, "log.bettacraft.herp.morph", "@" + s.translationKey(), "@" + s.varietyKey(variety));
            stock += weight;
            if (isFemale) females++;
            else males++;
        }
        int lost = k.eggs - females - males;
        if (s.tadpoles()) TankCare.log(level, c, "log.bettacraft.herp.tadpoles", "@" + s.translationKey(), String.valueOf(females + males));
        else TankCare.log(level, c, "log.bettacraft.turtle.hatched", "@" + s.translationKey(), String.valueOf(females + males), String.valueOf(females),
                String.valueOf(males));
        if (lost > 0) TankCare.log(level, c, "log.bettacraft.turtle.hatch_crowded", String.valueOf(lost));
        org.overengineer.bettacraft.api.ApiEvents.born(level, c, org.overengineer.bettacraft.registry.ModEntities.herp(s), females + males, true); // M32 part d
        c.markTankChanged();
        return females + males;
    }

    /** Part c: a spot in the open water of the tank for a tadpole (just under the surface), or null. */
    @Nullable
    private static Vec3 waterSpot(TankData t, TankTerrain ter, RandomSource r) {
        double surface = ter.surface();
        for (int i = 0; i < 32; i++) {
            double x = t.min().getX() + 0.2 + r.nextDouble() * Math.max(0.01, t.sizeX() - 0.4);
            double z = t.min().getZ() + 0.2 + r.nextDouble() * Math.max(0.01, t.sizeZ() - 0.4);
            double g = ter.ground(x, z);
            if (!Double.isFinite(g) || g > surface - 0.12) continue;
            return new Vec3(x, Math.max(g, surface - 0.1), z);
        }
        return null;
    }

    // ------------------------------------------------------------------ nests

    /** A nest spot: the egg block's place in the interior and the same spot in the tank. */
    public record Nest(BlockPos interior, Vec3 world) {}

    /** A random land spot of the tank with the species' nest ground on top and room above (null if none after 48 tries). */
    @Nullable
    public static Nest findNest(ServerLevel level, TankData t, RandomSource r, HerpSpecies s) {
        InteriorRegion region = InteriorRegions.get(level.getServer()).get(t.regionId());
        ServerLevel interior = InteriorManager.interiorLevel(level.getServer());
        if (region == null || !region.built() || interior == null) return null;
        for (int i = 0; i < 48; i++) {
            Nest n = nestIn(level, t, region, interior, r.nextInt(region.sizeX()), r.nextInt(region.sizeZ()), nestGround(s));
            if (n != null) return n;
        }
        return null;
    }

    /** The nest at this tank spot (the column under it), or null if it is no nest any more. */
    @Nullable
    public static Nest nestAt(ServerLevel level, TankData t, Vec3 at, HerpSpecies s) {
        InteriorRegion region = InteriorRegions.get(level.getServer()).get(t.regionId());
        ServerLevel interior = InteriorManager.interiorLevel(level.getServer());
        if (region == null || !region.built() || interior == null) return null;
        double fx = (t.sizeX() - 2 * TankTerrain.MARGIN) / region.sizeX(), fz = (t.sizeZ() - 2 * TankTerrain.MARGIN) / region.sizeZ();
        int lx = (int) Math.floor((at.x - t.min().getX() - TankTerrain.MARGIN) / fx), lz = (int) Math.floor((at.z - t.min().getZ() - TankTerrain.MARGIN) / fz);
        return nestIn(level, t, region, interior, lx, lz, nestGround(s));
    }

    @Nullable
    private static Nest nestIn(ServerLevel level, TankData t, InteriorRegion region, ServerLevel interior, int lx, int lz,
                               net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> nestGround) {
        if (lx < 0 || lz < 0 || lx >= region.sizeX() || lz >= region.sizeZ() || !region.inShape(lx, 0, lz)) return null;
        int wx = region.originX() + lx, wz = region.originZ() + lz;
        if (interior.getChunkSource().getChunkNow(wx >> 4, wz >> 4) == null) {
            InteriorGround.top16(level.getServer(), region, lx, lz); // asks for the chunk (a short ticket): the next try finds it
            return null;
        }
        int top16 = InteriorGround.top16(level.getServer(), region, lx, lz);
        if (top16 % 16 != 0) return null; // a slab, a plant mat: no full block of ground
        int ly = top16 / 16;
        if (ly >= region.sizeY() || !region.inShape(lx, ly, lz)) return null;
        double water = t.waterMb() <= 0 ? -1 : TankCare.waterLocalHeight(t, region.scale());
        if (water >= 0 && ly <= water + 0.05) return null; // under water: no land
        BlockPos at = region.toWorld(lx, ly, lz);
        if (!interior.getBlockState(at).isAir()) return null;
        if (ly + 1 < region.sizeY() && !interior.getBlockState(at.above()).isAir()) return null;
        BlockState ground;
        if (ly == 0) {
            BlockPos floor = new BlockPos(wx, InteriorRegion.FLOOR_Y, wz);
            var sub = Substrate.active(interior, floor);
            if (sub == null) return null;
            ground = sub.substrate();
        } else {
            ground = interior.getBlockState(at.below());
        }
        if (!ground.is(nestGround)) return null;
        double fx = (t.sizeX() - 2 * TankTerrain.MARGIN) / region.sizeX(), fy = (t.sizeY() - 2 * TankTerrain.MARGIN) / region.sizeY(),
                fz = (t.sizeZ() - 2 * TankTerrain.MARGIN) / region.sizeZ();
        Vec3 w = new Vec3(t.min().getX() + TankTerrain.MARGIN + (lx + 0.5) * fx, t.min().getY() + TankTerrain.MARGIN + top16 / 16.0 * fy,
                t.min().getZ() + TankTerrain.MARGIN + (lz + 0.5) * fz);
        if (!t.contains(BlockPos.containing(w.x, w.y + 0.01, w.z))) return null;
        return new Nest(at, w);
    }
}
