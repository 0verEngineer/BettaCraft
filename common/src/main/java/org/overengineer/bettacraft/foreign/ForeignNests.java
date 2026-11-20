package org.overengineer.bettacraft.foreign;

import java.util.random.RandomGenerator;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.terrarium.EggSex;
import org.overengineer.bettacraft.terrarium.HerpBreeding;
import org.overengineer.bettacraft.terrarium.LandWalker;

/**
 * M35: the eggs of a foreign egg layer with a {@code nest} in its profile (the Naturalist tortoise and alligator, the vanilla sea
 * turtle), by the patterns of our turtles (M31, {@link HerpBreeding}): after the mating the mother carries the eggs, finds a
 * nest on the land of the tank whose top is of the nest ground (sand or soil), walks there (our walker; one with its own AI
 * lays at once) and lays them: our egg block in the interior ({@code turtle_eggs}, the miniature shows it). The nest
 * temperature ({@link HerpBreeding#nestTemp}: the heat mats, else the air) incubates them like turtle eggs ({@link EggSex#rate}
 * with the profile's {@code nest_temperature}): below 18 °C they wait, above 36 °C they die, water over the nest drowns them,
 * an egg block broken in the interior destroys them. Then the young hatch at the nest. The animals of the mods have no sexes,
 * so there is no TSD: the temperature only sets the speed.
 * <p>
 * The clutch stays in the brood of the mother's persistent data ({@code ForeignBreeding#BROOD}: {@code nest} with the egg block,
 * the spot, the eggs, the progress, the heat): it needs no tank data, an old tank stays as it was. If the mother leaves the
 * tank (box, tub, the menu, let go), she takes the clutch along (part b): the egg block goes from the interior (nothing drops),
 * the eggs go back into her brood with their progress, and in her next tank she looks for a nest and lays them again (the
 * clutch hatches there, as far as it was). A mother that dies in her tank loses her eggs (the egg block goes).
 */
public final class ForeignNests {
    /** Days a carrying mother looks for a nest before she gives the eggs up. */
    public static final double GIVE_UP_DAYS = 2.0;
    /** Days she walks to her nest at most before she lays where she is (if that is a nest) or picks another. */
    public static final double WALK_DAYS = 0.1;

    private ForeignNests() {}

    public static TagKey<Block> ground(ForeignProfile p) {
        return TagKey.create(Registries.BLOCK, p.breeding().nest());
    }

    /** One care step of a brood with a nest (from {@link ForeignBreeding#step}). */
    static void step(ServerLevel level, TankBlockEntity c, LivingEntity mother, ForeignProfile p, CompoundTag brood, double days, RandomGenerator rnd) {
        if (!brood.contains("nest")) carry(level, c, mother, p, brood, days, rnd);
        else incubate(level, c, mother, p, brood, days, rnd);
    }

    private static void carry(ServerLevel level, TankBlockEntity c, LivingEntity mother, ForeignProfile p, CompoundTag brood, double days, RandomGenerator rnd) {
        TankData t = c.getTank();
        double age = brood.getDoubleOr("age", 0d) + days;
        brood.putDouble("age", age);
        TagKey<Block> ground = ground(p);
        Vec3 target = brood.contains("tx") ? new Vec3(brood.getDoubleOr("tx", 0d), brood.getDoubleOr("ty", 0d), brood.getDoubleOr("tz", 0d)) : null;
        if (target == null) {
            HerpBreeding.Nest n = HerpBreeding.findNest(level, t, mother.getRandom(), ground);
            if (n == null) {
                if (c.logDue("foreign_no_nest:" + mother.getType().getDescriptionId(), level.getGameTime(), 24000)) {
                    TankCare.log(level, c, "log.bettacraft.turtle.no_nest", TankCare.nameArg(mother));
                }
                if (age > GIVE_UP_DAYS) giveUp(mother, p);
                return;
            }
            target = n.world();
            brood.putDouble("tx", target.x);
            brood.putDouble("ty", target.y);
            brood.putDouble("tz", target.z);
            brood.putDouble("walk", 0);
            if (mother instanceof Mob m) ForeignTerrarium.goTo(m, level, t, target, (int) (WALK_DAYS * 24000));
        }
        double walk = brood.getDoubleOr("walk", 0d) + days;
        brood.putDouble("walk", walk);
        boolean walker = ForeignTerrarium.walker(mother) != null;
        if (walker && mother instanceof Mob mm && !LandWalker.arrivedAt(mm, target, HerpBreeding.NEST_REACH + mother.getBbWidth() / 2) && walk < WALK_DAYS) return;
        HerpBreeding.Nest n = HerpBreeding.nestAt(level, t, target, ground);
        if (n == null) {
            brood.remove("tx"); // the spot is no nest any more: another one
            return;
        }
        lay(level, c, mother, p, brood, n, rnd);
    }

    private static void lay(ServerLevel level, TankBlockEntity c, LivingEntity mother, ForeignProfile p, CompoundTag brood, HerpBreeding.Nest n, RandomGenerator rnd) {
        ServerLevel interior = InteriorManager.interiorLevel(level.getServer());
        if (interior == null) return;
        ForeignProfile.Breeding b = p.breeding();
        int eggs = b.youngMin() + (b.youngMax() > b.youngMin() ? rnd.nextInt(b.youngMax() - b.youngMin() + 1) : 0);
        if (brood.contains("carried_eggs")) eggs = brood.getIntOr("carried_eggs", 0); // part b: a clutch she took along
        if (eggs <= 0) {
            giveUp(mother, p);
            return;
        }
        interior.getChunk(n.interior());
        interior.setBlock(n.interior(), ModBlocks.TURTLE_EGGS.get().withEggs(eggs), 3);
        CompoundTag nest = new CompoundTag();
        nest.putLong("block", n.interior().asLong());
        nest.putDouble("x", n.world().x);
        nest.putDouble("y", n.world().y);
        nest.putDouble("z", n.world().z);
        nest.putInt("eggs", eggs);
        nest.putDouble("progress", brood.getDoubleOr("carried_progress", 0d));
        nest.putDouble("hot", brood.getDoubleOr("carried_hot", 0d));
        brood.remove("carried_eggs");
        brood.remove("carried_progress");
        brood.remove("carried_hot");
        brood.put("nest", nest);
        brood.remove("tx");
        TankCare.log(level, c, "log.bettacraft.turtle.laid", TankCare.nameArg(mother), String.valueOf(eggs));
        c.setChanged();
    }

    private static void incubate(ServerLevel level, TankBlockEntity c, LivingEntity mother, ForeignProfile p, CompoundTag brood, double days, RandomGenerator rnd) {
        TankData t = c.getTank();
        CompoundTag nest = brood.getCompoundOrEmpty("nest");
        BlockPos block = BlockPos.of(nest.getLongOr("block", 0L));
        ServerLevel interior = InteriorManager.interiorLevel(level.getServer());
        String what = "@" + mother.getType().getDescriptionId();
        if (level.getGameTime() % 200 < 20 && interior != null && interior.isLoaded(block) && !interior.getBlockState(block).is(ModBlocks.TURTLE_EGGS.get())) {
            TankCare.log(level, c, "log.bettacraft.turtle.eggs_gone", what);
            end(mother, p, null, null);
            return;
        }
        double surface = t.waterMb() <= 0 ? Double.NEGATIVE_INFINITY : t.waterSurfaceY();
        if (nest.getDoubleOr("y", 0d) < surface - 0.02) {
            TankCare.log(level, c, "log.bettacraft.turtle.eggs_drowned", what);
            end(mother, p, interior, block);
            return;
        }
        double temp = HerpBreeding.nestTemp(t);
        if (Double.isFinite(temp) && temp > EggSex.LETHAL) nest.putDouble("hot", nest.getDoubleOr("hot", 0d) + days);
        if (nest.getDoubleOr("hot", 0d) >= EggSex.LETHAL_DAYS) {
            TankCare.log(level, c, "log.bettacraft.turtle.eggs_cooked", what);
            end(mother, p, interior, block);
            return;
        }
        double before = nest.getDoubleOr("progress", 0d);
        double progress = Math.min(1, before + EggSex.rate(p.breeding().nestPivot(), p.breeding().days(), temp, HerpBreeding.speed()) * days);
        nest.putDouble("progress", progress);
        // M39 (question I1): the temperature in the thermosensitive period (the middle third) decides the sex of the young
        if (p.breeding().sex() == ForeignProfile.Sex.TEMPERATURE && Double.isFinite(temp) && before < EggSex.TSP_TO && progress > EggSex.TSP_FROM) {
            nest.putDouble("tsp_sum", nest.getDoubleOr("tsp_sum", 0d) + temp * days);
            nest.putDouble("tsp_days", nest.getDoubleOr("tsp_days", 0d) + days);
        }
        if (progress < 1 - 1e-9) return;
        int eggs = nest.getIntOr("eggs", 0);
        Vec3 at = new Vec3(nest.getDoubleOr("x", 0d), nest.getDoubleOr("y", 0d) + 0.01, nest.getDoubleOr("z", 0d));
        double tsp = nest.getDoubleOr("tsp_days", 0d) > 0 ? nest.getDoubleOr("tsp_sum", 0d) / nest.getDoubleOr("tsp_days", 0d) : temp;
        end(mother, p, interior, block);
        hatch(level, c, mother, p, eggs, at, rnd, tsp);
    }

    /** The young of a clutch at its nest, as many as the tank has room for (the entity limit, the hard stocking limit). */
    static int hatch(ServerLevel level, TankBlockEntity c, LivingEntity mother, ForeignProfile p, int eggs, Vec3 at, RandomGenerator rnd) {
        return hatch(level, c, mother, p, eggs, at, rnd, HerpBreeding.nestTemp(c.getTank()));
    }

    /**
     * The young of a clutch at its nest; M39: with a sexed profile each one is female by the chance of the nest temperature
     * {@code tsp} in the thermosensitive period ({@link ForeignProfile.Sex#TEMPERATURE}) or half and half.
     */
    static int hatch(ServerLevel level, TankBlockEntity c, LivingEntity mother, ForeignProfile p, int eggs, Vec3 at, RandomGenerator rnd, double tsp) {
        TankData t = c.getTank();
        var live = TankInhabitants.liveInhabitants(level, t);
        int room = Math.max(0, ServerConfig.getOr(ServerConfig.TANK_MAX_ENTITIES, 64) - live.size());
        CompoundTag genes = ForeignBreeding.inherited(mother, p);
        int made = 0;
        int females = 0;
        double chance = p.breeding().sex() == ForeignProfile.Sex.TEMPERATURE ? EggSex.femaleChance(p.breeding().nestPivot(), tsp) : 0.5;
        for (int i = 0; i < Math.min(eggs, room); i++) {
            Vec3 spot = inTank(t, at.add((rnd.nextDouble() - 0.5) * 0.2, 0, (rnd.nextDouble() - 0.5) * 0.2), at);
            Entity y = ForeignBreeding.spawnYoung(level, c, mother.getType(), p, genes, genes, spot, rnd);
            if (y == null) continue;
            made++;
            if (p.breeding().sexed()) {
                boolean f = rnd.nextDouble() < chance;
                ForeignBreeding.setFemale(y, f);
                if (f) females++;
            }
        }
        if (p.breeding().sexed() && made > 0) { // M39: how many of each
            TankCare.log(level, c, "log.bettacraft.foreign.hatched_sexes", "@" + mother.getType().getDescriptionId(), String.valueOf(made),
                    String.valueOf(females), String.valueOf(made - females));
        } else {
            TankCare.log(level, c, "log.bettacraft.foreign.hatched", "@" + mother.getType().getDescriptionId(), String.valueOf(made));
        }
        if (made < eggs) TankCare.log(level, c, "log.bettacraft.turtle.hatch_crowded", String.valueOf(eggs - made));
        org.overengineer.bettacraft.api.ApiEvents.born(level, c, org.overengineer.bettacraft.api.ApiEvents.id(mother.getType()), made, true, true, false); // M32 part d
        c.markTankChanged();
        return made;
    }

    /**
     * M52: where a young comes into the tank: {@code spot} (a little off its nest or mother), but always inside the glass and
     * in a block of the tank, else at {@code base}. A hatchling of a nest by the wall came out just past the glass before
     * (the nest of the edge column lies 0.06 inside, the young up to 0.1 off it): outside the tank, the next reconcile let it go.
     */
    static Vec3 inTank(TankData t, Vec3 spot, Vec3 base) {
        double m = org.overengineer.bettacraft.critters.TankTerrain.MARGIN + 0.01;
        Vec3 p = new Vec3(net.minecraft.util.Mth.clamp(spot.x, t.min().getX() + m, t.max().getX() + 1 - m), spot.y,
                net.minecraft.util.Mth.clamp(spot.z, t.min().getZ() + m, t.max().getZ() + 1 - m));
        return t.contains(BlockPos.containing(p)) ? p : base;
    }

    /** Ends the brood (the egg block goes when given) and lets the mother rest. */
    private static void end(LivingEntity mother, ForeignProfile p, @Nullable ServerLevel interior, @Nullable BlockPos block) {
        if (interior != null && block != null) {
            interior.getChunk(block);
            if (interior.getBlockState(block).is(ModBlocks.TURTLE_EGGS.get())) interior.removeBlock(block, false);
        }
        org.overengineer.bettacraft.platform.Platform.get().persistentData(mother).remove(ForeignBreeding.BROOD);
        org.overengineer.bettacraft.platform.Platform.get().persistentData(mother).putDouble(ForeignBreeding.REST, p.breeding().pauseDays());
    }

    /**
     * Part b: the mother leaves her tank: a laid clutch goes along with her (the egg block goes from the interior, nothing
     * drops; the eggs and their progress go back into her brood, she lays them again in her next tank). A mother that
     * still carries her eggs keeps them as they are; she forgets the nest she was walking to.
     */
    public static void leave(LivingEntity mother) {
        CompoundTag d = org.overengineer.bettacraft.platform.Platform.get().persistentData(mother);
        if (!d.contains(ForeignBreeding.BROOD)) return;
        CompoundTag brood = d.getCompoundOrEmpty(ForeignBreeding.BROOD);
        brood.remove("tx");
        if (brood.contains("nest")) {
            CompoundTag nest = brood.getCompoundOrEmpty("nest");
            removeEggs(mother, BlockPos.of(nest.getLongOr("block", 0L)));
            brood.putInt("carried_eggs", nest.getIntOr("eggs", 0));
            brood.putDouble("carried_progress", nest.getDoubleOr("progress", 0d));
            brood.putDouble("carried_hot", nest.getDoubleOr("hot", 0d));
            brood.remove("nest");
            brood.putDouble("age", 0);
        }
        d.put(ForeignBreeding.BROOD, brood);
    }

    /** Part b: a mother died in her tank: her laid eggs go with her (the egg block goes). */
    public static void died(LivingEntity mother) {
        CompoundTag d = org.overengineer.bettacraft.platform.Platform.get().persistentData(mother);
        if (!d.contains(ForeignBreeding.BROOD) || !d.getCompoundOrEmpty(ForeignBreeding.BROOD).contains("nest")) return;
        removeEggs(mother, BlockPos.of(d.getCompoundOrEmpty(ForeignBreeding.BROOD).getCompoundOrEmpty("nest").getLongOr("block", 0L)));
        d.remove(ForeignBreeding.BROOD);
    }

    private static void removeEggs(LivingEntity mother, BlockPos block) {
        if (mother.level().getServer() == null) return;
        ServerLevel interior = InteriorManager.interiorLevel(mother.level().getServer());
        if (interior == null) return;
        interior.getChunk(block);
        if (interior.getBlockState(block).is(ModBlocks.TURTLE_EGGS.get())) interior.removeBlock(block, false);
    }

    private static void giveUp(LivingEntity mother, ForeignProfile p) {
        end(mother, p, null, null);
    }

    /** The nest of a brood (GameTests, menu): the spot, or null while she carries the eggs (or without a brood). */
    @Nullable
    public static Vec3 nestOf(LivingEntity mother) {
        CompoundTag d = org.overengineer.bettacraft.platform.Platform.get().persistentData(mother);
        if (!d.contains(ForeignBreeding.BROOD)) return null;
        CompoundTag b = d.getCompoundOrEmpty(ForeignBreeding.BROOD);
        if (!b.contains("nest")) return null;
        CompoundTag n = b.getCompoundOrEmpty("nest");
        return new Vec3(n.getDoubleOr("x", 0d), n.getDoubleOr("y", 0d), n.getDoubleOr("z", 0d));
    }

    /** GameTests: the interior block of the eggs of a brood, or null. */
    @Nullable
    public static BlockPos eggBlockOf(LivingEntity mother) {
        CompoundTag d = org.overengineer.bettacraft.platform.Platform.get().persistentData(mother);
        if (!d.contains(ForeignBreeding.BROOD) || !d.getCompoundOrEmpty(ForeignBreeding.BROOD).contains("nest")) return null;
        return BlockPos.of(d.getCompoundOrEmpty(ForeignBreeding.BROOD).getCompoundOrEmpty("nest").getLongOr("block", 0L));
    }
}
