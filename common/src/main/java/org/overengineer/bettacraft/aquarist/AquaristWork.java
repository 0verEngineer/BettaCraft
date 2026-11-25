package org.overengineer.bettacraft.aquarist;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.platform.item.IItemHandler;
import org.overengineer.bettacraft.platform.item.ItemHandlerHelper;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.plants.InteriorPlant;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plumbing.FluidMoves;
import org.overengineer.bettacraft.plumbing.WaterChangerBlockEntity;
import org.overengineer.bettacraft.plumbing.WaterSources;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.Salinity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.WaterValues;
import org.overengineer.bettacraft.tank.algae.TankAlgae;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.plants.TankPlants;

/**
 * M51 (question C1): what a hired aquarist does, as jobs he walks to ({@link AquaristEntity} runs them one by one). A
 * round is planned when he has nothing to do: fetch food at the workplace if a tank is due a feeding, then visit every
 * tank with something due (feeding, algae, plants, a look for warnings), then one water change (fetch the water at the
 * workplace, carry it to the tank), then one trip with young into the rearing tank, then back to the workplace to put the
 * cuttings and the food he did not use into the chests. Every job checks right before it touches a tank that it is still
 * loaded, still the same tank and still one he may work at ({@link AquaristRules}); nothing he carries is lost: animals go
 * back into their tank (or as a filled fish bag into a chest), food and cuttings into the chests.
 */
public final class AquaristWork {
    public enum Kind { FETCH_FOOD, TEND, FETCH_WATER, CHANGE_WATER, TAKE_YOUNG, RELEASE_YOUNG, RETURN_YOUNG, DEPOSIT }

    /** One job: what, at which tank (null: at the workplace); M66: {@code to} = the rearing tank the young of a TAKE_YOUNG go to. */
    public record Job(Kind kind, @Nullable UUID tank, @Nullable UUID to) {
        public Job(Kind kind, @Nullable UUID tank) {
            this(kind, tank, null);
        }

        public boolean atWorkplace() {
            return kind == Kind.FETCH_FOOD || kind == Kind.FETCH_WATER || kind == Kind.DEPOSIT;
        }
    }

    /** Feeding times (time of day) for the levels 1, 2, 3: all in the day, when he works. */
    static final long[][] FEED_TIMES = {{2000}, {2000, 8000}, {1500, 5500, 9500}};
    /** Algae share from which he scrapes, for the levels 1, 2, 3. */
    static final double[] ALGAE_FROM = {0.5, 0.3, 0.15};
    /** Floating plant cover from which he thins them out, for the levels 1, 2, 3. */
    static final double[] FLOATING_FROM = {0.9, 0.75, 0.6};
    /** Stages below full size from which he trims an aquarium plant, for the levels 1, 2, 3. */
    static final int[] TRIM_BELOW_FULL = {0, 1, 2};
    /** Share of the water he changes, for the levels 1, 2, 3. */
    static final double[] WATER_SHARE = {0.15, 0.25, 0.35};
    /** Young he carries per trip, for the levels 1, 2, 3. */
    static final int[] YOUNG_PER_TRIP = {2, 4, 8};
    /** Most plants he trims at one visit. */
    static final int TRIMS_PER_VISIT = 4;
    /** Ticks between two looks at a tank for warnings. */
    static final long WARN_EVERY = 2400;
    /** Food items of one kind he takes per tank to feed. */
    static final int FOOD_PER_TANK = 3;

    private AquaristWork() {}

    // ------------------------------------------------------------------ the tanks

    /** The tanks he looks after now: loaded, his by the rules, around the workplace, at most {@code hiredAquarist.maxTanksCared}. */
    public static List<TankBlockEntity> tanks(ServerLevel level, AquaristEntity npc) {
        List<TankBlockEntity> out = new ArrayList<>();
        BlockPos wp = npc.workplace();
        if (wp == null || npc.employer() == null) return out;
        int max = ServerConfig.getOr(ServerConfig.AQUARIST_MAX_TANKS, 16);
        int radius = ServerConfig.getOr(ServerConfig.AQUARIST_WORK_RADIUS, 32);
        AquaristRecords rec = AquaristRecords.get(level.getServer());
        for (AquaristRecords.TankEntry e : rec.near(level.dimension(), wp, radius)) {
            if (out.size() >= max) break;
            TankBlockEntity c = rec.resolve(level.getServer(), e);
            if (c != null && AquaristRules.mayWork(c.getTank(), npc.employer())) out.add(c);
        }
        return out;
    }

    /** Every tank of the index around his workplace with what he may do there (for the menu). */
    public static List<AquaristRecords.TankEntry> tanksAround(ServerLevel level, AquaristEntity npc) {
        BlockPos wp = npc.workplace();
        if (wp == null) return List.of(); // M66: without a workplace he cares for no tank, so the menu lists none
        return AquaristRecords.get(level.getServer()).near(level.dimension(), wp, ServerConfig.getOr(ServerConfig.AQUARIST_WORK_RADIUS, 32));
    }

    /** The tank {@code id} if it is loaded, still there and one he may work at; else null. */
    @Nullable
    public static TankBlockEntity tank(ServerLevel level, AquaristEntity npc, @Nullable UUID id) {
        if (id == null) return null;
        AquaristRecords rec = AquaristRecords.get(level.getServer());
        AquaristRecords.TankEntry e = rec.tank(id);
        if (e == null || !e.dimension().equals(level.dimension())) return null;
        TankBlockEntity c = rec.resolve(level.getServer(), e);
        return c != null && AquaristRules.mayWork(c.getTank(), npc.employer()) ? c : null;
    }

    // ------------------------------------------------------------------ what is due

    static long feedKey(long dayTime, int lvl) {
        long[] times = FEED_TIMES[Math.max(1, Math.min(3, lvl)) - 1];
        long day = Math.floorDiv(dayTime, 24000L), t = Math.floorMod(dayTime, 24000L);
        long best = -1;
        for (long x : times) if (x <= t) best = x;
        if (best < 0) {
            day--;
            best = times[times.length - 1];
        }
        return day * 24000L + best;
    }

    /** A feeding is due (and no feeder on the tank does it). */
    public static boolean feedDue(ServerLevel level, AquaristEntity npc, TankBlockEntity c) {
        int lvl = npc.taskLevel(AquaristTask.FEED);
        if (lvl <= 0 || !npc.mayDo(c.getTank().id(), AquaristTask.FEED) || TankCare.findFeeder(level, c.getTank()) != null) return false;
        if (TankInhabitants.liveInhabitants(level, c.getTank()).isEmpty() && c.getTank().critters().isEmpty()) return false;
        return c.getTank().aquaristDone(AquaristTask.FEED.id()) < feedKey(level.getDayTime(), lvl);
    }

    public static boolean algaeDue(AquaristEntity npc, TankBlockEntity c) {
        int lvl = npc.taskLevel(AquaristTask.ALGAE);
        return lvl > 0 && npc.mayDo(c.getTank().id(), AquaristTask.ALGAE) && Features.enabled(Feature.ALGAE) && TankAlgae.mean(c.getTank()) >= ALGAE_FROM[lvl - 1];
    }

    public static boolean plantsDue(ServerLevel level, AquaristEntity npc, TankBlockEntity c) {
        int lvl = npc.taskLevel(AquaristTask.PLANTS);
        if (lvl <= 0 || !npc.mayDo(c.getTank().id(), AquaristTask.PLANTS)) return false;
        if (!c.getTank().plants().isEmpty() && TankPlants.coverage(c.getTank()) >= FLOATING_FROM[lvl - 1]) return true;
        return !trimmable(level, c, lvl).isEmpty();
    }

    /** The aquarium plants of the interior that are big enough to trim at this level. */
    static List<InteriorPlant> trimmable(ServerLevel level, TankBlockEntity c, int lvl) {
        List<InteriorPlant> out = new ArrayList<>();
        if (!Features.enabled(Feature.INTERIOR)) return out;
        InteriorRegion r = InteriorPlants.region(level.getServer(), c.getTank());
        if (r == null) return out;
        for (InteriorPlant p : r.plants().all()) {
            if (p.stage < p.species.stages() - TRIM_BELOW_FULL[lvl - 1] || p.stage != p.target) continue;
            if (Math.min(p.species.trimStages(), p.stage - p.species.trimFloor()) <= 0) continue;
            out.add(p);
        }
        return out;
    }

    public static boolean warnDue(ServerLevel level, AquaristEntity npc, TankBlockEntity c) {
        return npc.taskLevel(AquaristTask.WARN) > 0 && npc.mayDo(c.getTank().id(), AquaristTask.WARN) && level.getGameTime() - npc.lastWarnLook(c.getTank().id()) >= WARN_EVERY;
    }

    /** A water change is due: the interval of the water change controller has passed, or the water is bad (once a day). */
    public static boolean waterDue(ServerLevel level, AquaristEntity npc, TankBlockEntity c) {
        if (npc.taskLevel(AquaristTask.WATER) <= 0 || !npc.mayDo(c.getTank().id(), AquaristTask.WATER)) return false;
        TankData t = c.getTank();
        if (t.waterMb() <= 0 || waterAmount(t, npc.taskLevel(AquaristTask.WATER)) <= 0) return false;
        long now = level.getGameTime();
        long mine = t.aquaristDone(AquaristTask.WATER.id());
        if (mine != Long.MIN_VALUE && now - mine < 24000L) return false;
        long last = t.lastWaterChange() >= 0 ? t.lastWaterChange() : t.lastFreshWater();
        long interval = WaterChangerBlockEntity.defaultInterval() * 24000L;
        return last < 0 || now - last >= interval || t.waterQuality() < 0.5F;
    }

    /** mB he changes at this level (never below the minimum fill while animals live in the tank). */
    static long waterAmount(TankData t, int lvl) {
        long amount = Math.round(t.capacityMb() * WATER_SHARE[Math.max(1, Math.min(3, lvl)) - 1]);
        long min = t.inhabitants().isEmpty() ? 0 : Math.round(ServerConfig.getOr(ServerConfig.TANK_MIN_FILL_TO_INSERT, 0.5) * t.capacityMb());
        return Math.max(0, Math.min(amount, t.waterMb() - min));
    }

    /** The young of a tank he would move (juvenile and half-grown bettas, young tankmates, froglets), at most {@code max}. */
    public static List<Entity> young(ServerLevel level, TankBlockEntity c, int max) {
        List<Entity> out = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) {
            if (out.size() >= max) break;
            boolean young = e instanceof BettaEntity b ? b.lifeStage() == LifeStage.JUVENILE || b.lifeStage() == LifeStage.SUBADULT
                    : e instanceof org.overengineer.bettacraft.entity.tankmate.TankmateEntity m ? !m.isAdult()
                    : e instanceof org.overengineer.bettacraft.entity.frog.DwarfFrogEntity f && !f.isAdult();
            if (young && TankInhabitants.bagTakes(e)) out.add(e);
        }
        return out;
    }

    /** Whether the rearing tank takes this animal (the salt suits it, the tank is not full). */
    static boolean rearingTakes(ServerLevel level, TankBlockEntity rearing, Entity e) {
        var salt = org.overengineer.bettacraft.reef.SaltTolerance.of(e.getType());
        if (org.overengineer.bettacraft.reef.SaltTolerance.refuse(salt, rearing.getTank())) return false;
        TankData t = rearing.getTank();
        List<Entity> live = TankInhabitants.liveInhabitants(level, t);
        int cap = TankInhabitants.hardCap(t.waterMb() / (double) TankData.MB_PER_BLOCK);
        return TankInhabitants.stocking(live, t) + TankInhabitants.weight(e) <= cap + 1e-6;
    }

    /** M66: the key of an animal's species for the rearing tanks' filters: "betta", the tankmate species id, "dwarf_frog", else the type id. */
    public static String speciesKey(Entity e) {
        if (e instanceof BettaEntity) return "betta";
        if (e instanceof org.overengineer.bettacraft.entity.tankmate.TankmateEntity m) return m.species().id();
        if (e instanceof org.overengineer.bettacraft.entity.frog.DwarfFrogEntity) return "dwarf_frog";
        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
    }

    /** M66: the species a rearing tank can be limited to (the animals that grow up in a tank): key and translation key. */
    public static List<String[]> speciesOptions() {
        List<String[]> out = new ArrayList<>();
        out.add(new String[]{"betta", org.overengineer.bettacraft.registry.ModEntities.BETTA.get().getDescriptionId()});
        for (var sp : org.overengineer.bettacraft.tankmates.TankmateSpecies.values()) out.add(new String[]{sp.id(), sp.translationKey()});
        out.add(new String[]{"dwarf_frog", org.overengineer.bettacraft.registry.ModEntities.DWARF_FROG.get().getDescriptionId()});
        return out;
    }

    /** M66: the first of his rearing tanks (in order) that takes this animal (species switched on, salt suits, room), or null. */
    @Nullable
    public static TankBlockEntity rearingFor(ServerLevel level, AquaristEntity npc, Entity e) {
        String key = speciesKey(e);
        for (AquaristEntity.RearingTank r : npc.rearingTanks()) {
            if (!r.takes(key)) continue;
            TankBlockEntity c = tank(level, npc, r.tank());
            if (c != null && rearingTakes(level, c, e)) return c;
        }
        return null;
    }

    static boolean isRearing(AquaristEntity npc, TankBlockEntity c) {
        for (AquaristEntity.RearingTank r : npc.rearingTanks()) if (r.tank().equals(c.getTank().id())) return true;
        return false;
    }

    // ------------------------------------------------------------------ planning

    /** The next round of jobs (empty: nothing to do now). */
    public static List<Job> plan(ServerLevel level, AquaristEntity npc) {
        List<Job> jobs = planJobs(level, npc);
        if (org.overengineer.bettacraft.debug.LogArea.AQUARIST.trace) org.overengineer.bettacraft.debug.LogArea.AQUARIST.trace("aquarist {} plans {}", npc.getUUID(), jobs); // M57
        return jobs;
    }

    private static List<Job> planJobs(ServerLevel level, AquaristEntity npc) {
        List<Job> jobs = new ArrayList<>();
        // things he still carries come first
        if (!npc.carriedAnimals().isEmpty()) {
            jobs.add(new Job(Kind.RELEASE_YOUNG, npc.carriedTo() != null ? npc.carriedTo() : npc.rearingTank()));
            return jobs;
        }
        if (npc.waterMb() > 0 && npc.waterFor() != null) {
            jobs.add(new Job(Kind.CHANGE_WATER, npc.waterFor()));
            jobs.add(new Job(Kind.DEPOSIT, null));
            return jobs;
        }
        List<TankBlockEntity> tanks = tanks(level, npc);
        if (tanks.isEmpty()) {
            if (npc.hasCarriedItems()) jobs.add(new Job(Kind.DEPOSIT, null));
            return jobs;
        }
        boolean anyFeed = false;
        List<TankBlockEntity> tend = new ArrayList<>();
        for (TankBlockEntity c : tanks) {
            boolean feed = feedDue(level, npc, c);
            anyFeed |= feed;
            if (feed || algaeDue(npc, c) || plantsDue(level, npc, c) || warnDue(level, npc, c)) tend.add(c);
        }
        if (anyFeed && !npc.hasFood()) jobs.add(new Job(Kind.FETCH_FOOD, null));
        // the tanks in a chain of nearest neighbours from the workplace
        BlockPos at = npc.workplace();
        while (!tend.isEmpty()) {
            TankBlockEntity best = null;
            double bd = Double.MAX_VALUE;
            for (TankBlockEntity c : tend) {
                double d = c.getBlockPos().distSqr(at);
                if (d < bd) {
                    bd = d;
                    best = c;
                }
            }
            tend.remove(best);
            jobs.add(new Job(Kind.TEND, best.getTank().id()));
            at = best.getBlockPos();
        }
        // one water change per round
        for (TankBlockEntity c : tanks) {
            if (!waterDue(level, npc, c)) continue;
            if (sourceWater(level, npc, c.getTank(), 1, false) <= 0) {
                npc.complain(level, "no_water", null);
                break;
            }
            jobs.add(new Job(Kind.FETCH_WATER, c.getTank().id()));
            jobs.add(new Job(Kind.CHANGE_WATER, c.getTank().id()));
            break;
        }
        // one trip with young: from the first tank that has some a rearing tank takes (M66: the first of several that takes the animal)
        int perTrip = npc.taskLevel(AquaristTask.YOUNG);
        if (perTrip > 0 && !npc.rearingTanks().isEmpty()) {
            for (TankBlockEntity c : tanks) {
                if (isRearing(npc, c) || !npc.mayDo(c.getTank().id(), AquaristTask.YOUNG)) continue;
                TankBlockEntity target = null;
                for (Entity e : young(level, c, 64)) {
                    target = rearingFor(level, npc, e);
                    if (target != null) break;
                }
                if (target == null) continue;
                jobs.add(new Job(Kind.TAKE_YOUNG, c.getTank().id(), target.getTank().id()));
                jobs.add(new Job(Kind.RELEASE_YOUNG, target.getTank().id()));
                break;
            }
        }
        if (!jobs.isEmpty() || npc.hasCarriedItems()) jobs.add(new Job(Kind.DEPOSIT, null));
        return jobs;
    }

    // ------------------------------------------------------------------ doing

    /** Does the job (he is there). Returns follow-up jobs to put in front of the queue (usually none). */
    public static List<Job> perform(ServerLevel level, AquaristEntity npc, Job job) {
        if (org.overengineer.bettacraft.debug.LogArea.AQUARIST.debug) org.overengineer.bettacraft.debug.LogArea.AQUARIST.debug("aquarist {} at {}: {} (tank {})", npc.getUUID(), npc.blockPosition(), job.kind(), job.tank()); // M57
        List<Job> next = new ArrayList<>();
        switch (job.kind()) {
            case FETCH_FOOD -> fetchFood(level, npc);
            case TEND -> {
                TankBlockEntity c = tank(level, npc, job.tank());
                if (c != null) tend(level, npc, c);
            }
            case FETCH_WATER -> {
                TankBlockEntity c = tank(level, npc, job.tank());
                if (c != null) fetchWater(level, npc, c);
            }
            case CHANGE_WATER -> {
                TankBlockEntity c = tank(level, npc, job.tank());
                if (c != null) changeWater(level, npc, c);
                else npc.pourBackWater(level);
            }
            case TAKE_YOUNG -> {
                TankBlockEntity c = tank(level, npc, job.tank());
                if (c != null) takeYoung(level, npc, c, job.to());
            }
            case RELEASE_YOUNG, RETURN_YOUNG -> {
                TankBlockEntity c = tank(level, npc, job.tank());
                int left = c == null ? npc.carriedAnimals().size() : releaseYoung(level, npc, c, job.kind() == Kind.RETURN_YOUNG);
                if (left > 0) {
                    // the rearing tank did not take them (full, gone, locked): back into the tank they came from
                    if (job.kind() == Kind.RELEASE_YOUNG && npc.carriedFrom() != null && !npc.carriedFrom().equals(job.tank())) {
                        next.add(new Job(Kind.RETURN_YOUNG, npc.carriedFrom()));
                    } else {
                        npc.storeCarriedAnimals(level); // nowhere to go: as filled fish bags into a chest at the workplace
                    }
                }
            }
            case DEPOSIT -> deposit(level, npc);
        }
        return next;
    }

    // ---- food

    /** Whether he would take this stack as food for the tanks. */
    static boolean isTankFood(ItemStack s) {
        return !s.isEmpty() && (s.is(ModTags.BETTA_FOOD) || org.overengineer.bettacraft.terrarium.TerrariumFood.isFood(s));
    }

    /** Takes food from the chests at the workplace: a few items of each kind (live food first, up to four kinds). */
    static void fetchFood(ServerLevel level, AquaristEntity npc) {
        int tanks = Math.max(1, tanks(level, npc).size());
        int kinds = 0;
        Set<net.minecraft.world.item.Item> seen = new HashSet<>();
        for (boolean live : new boolean[]{true, false}) {
            for (IItemHandler h : chests(level, npc)) {
                for (int i = 0; i < h.getSlots() && kinds < 4; i++) {
                    ItemStack s = h.getStackInSlot(i);
                    if (!isTankFood(s) || s.is(ModTags.LIVE_FOOD) != live || seen.contains(s.getItem())) continue;
                    int want = Math.min(s.getCount(), FOOD_PER_TANK * tanks);
                    ItemStack sim = h.extractItem(i, want, true);
                    if (sim.isEmpty()) continue;
                    ItemStack rest = ItemHandlerHelper.insertItemStacked(npc.bag(), sim, true);
                    int fits = sim.getCount() - rest.getCount();
                    if (fits <= 0) continue;
                    ItemStack got = h.extractItem(i, fits, false);
                    ItemStack left = ItemHandlerHelper.insertItemStacked(npc.bag(), got, false);
                    if (!left.isEmpty()) ItemHandlerHelper.insertItemStacked(h, left, false); // never lost
                    seen.add(s.getItem());
                    kinds++;
                }
            }
        }
        if (kinds == 0) npc.complain(level, "no_food", null);
        else npc.swingWork(level, npc.workplace());
    }

    /** At a tank: the feeding, the algae, the plants and the look for warnings that are due. */
    static void tend(ServerLevel level, AquaristEntity npc, TankBlockEntity c) {
        TankData t = c.getTank();
        if (feedDue(level, npc, c)) feed(level, npc, c);
        if (algaeDue(npc, c)) {
            int n = 0;
            for (Direction d : Direction.Plane.HORIZONTAL) n += TankAlgae.scrape(level, c, t.anchor(), d, true);
            if (n > 0) {
                t.setAquaristDone(AquaristTask.ALGAE.id(), level.getGameTime());
                TankCare.log(level, c, "log.bettacraft.aquarist.algae", npc.logName(), String.valueOf(n));
                level.playSound(null, c.getBlockPos(), SoundEvents.BRUSH_GENERIC, SoundSource.NEUTRAL, 0.6F, 1.2F);
            }
        }
        if (plantsDue(level, npc, c)) trimPlants(level, npc, c);
        if (npc.taskLevel(AquaristTask.WARN) > 0 && npc.mayDo(t.id(), AquaristTask.WARN)) {
            npc.markWarnLook(t.id(), level.getGameTime());
            npc.reportWarnings(level, c, warnings(level, c, npc.taskLevel(AquaristTask.WARN)));
        }
        c.setChanged();
    }

    /** Feeds from his bag like a feeder does (live food first; every fish once). */
    static void feed(ServerLevel level, AquaristEntity npc, TankBlockEntity c) {
        IItemHandler items = npc.bag();
        Set<UUID> fed = new HashSet<>();
        int used = 0;
        for (boolean live : new boolean[]{true, false}) {
            for (int i = 0; i < items.getSlots(); i++) {
                ItemStack s = items.getStackInSlot(i);
                if (s.isEmpty() || !s.is(ModTags.BETTA_FOOD) || s.is(ModTags.LIVE_FOOD) != live) continue;
                int n = TankCare.feed(level, c, s, s.getCount(), fed);
                if (n > 0) {
                    ItemStack shown = s.copyWithCount(1);
                    items.extractItem(i, n, false);
                    used += n;
                    level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, shown), c.getTank().anchor().getX() + 0.5,
                            c.getTank().max().getY() + 0.9, c.getTank().anchor().getZ() + 0.5, 6, 0.3, 0.05, 0.3, 0.02);
                }
            }
        }
        used += org.overengineer.bettacraft.terrarium.TerrariumFood.feedFromFeeder(level, c, items);
        used += org.overengineer.bettacraft.foreign.ForeignFood.feedFromFeeder(level, c, items);
        if (used > 0) {
            c.getTank().setAquaristDone(AquaristTask.FEED.id(), feedKey(level.getDayTime(), npc.taskLevel(AquaristTask.FEED)));
            TankCare.log(level, c, "log.bettacraft.aquarist.fed", npc.logName(), String.valueOf(used));
            level.playSound(null, c.getBlockPos(), SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL, 0.3F, 1.6F);
        } else if (!npc.hasFood()) {
            npc.complain(level, "no_food", c);
        } else {
            // nobody eats what he has: counts as done for this feeding (he does not walk back and forth)
            c.getTank().setAquaristDone(AquaristTask.FEED.id(), feedKey(level.getDayTime(), npc.taskLevel(AquaristTask.FEED)));
        }
    }

    /** Thins the floating plants and trims the grown aquarium plants; the plants and cuttings go into his bag. */
    static void trimPlants(ServerLevel level, AquaristEntity npc, TankBlockEntity c) {
        int lvl = npc.taskLevel(AquaristTask.PLANTS);
        int got = 0;
        TankData t = c.getTank();
        if (!t.plants().isEmpty() && TankPlants.coverage(t) >= FLOATING_FROM[lvl - 1] && npc.bagFreeSlots() >= 3) {
            List<ItemStack> items = TankPlants.thin(level, c);
            for (ItemStack s : items) {
                got += s.getCount();
                npc.putInBag(level, s);
            }
            if (!items.isEmpty()) TankCare.log(level, c, "log.bettacraft.plants_thinned", npc.logName(), String.valueOf(got));
        }
        ServerLevel interior = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion r = InteriorPlants.region(level.getServer(), t);
        if (interior != null && r != null) {
            int trims = 0;
            for (InteriorPlant p : trimmable(level, c, lvl)) {
                if (trims >= TRIMS_PER_VISIT || npc.bagFreeSlots() < 1) break;
                int n = InteriorPlants.trimBy(interior, r, p, npc.logName());
                if (n <= 0) continue;
                npc.putInBag(level, InteriorPlants.item(p.species, 0).copyWithCount(n));
                got += n;
                trims++;
            }
        }
        if (got > 0) {
            t.setAquaristDone(AquaristTask.PLANTS.id(), level.getGameTime());
            level.playSound(null, c.getBlockPos(), SoundEvents.SHEEP_SHEAR, SoundSource.NEUTRAL, 0.6F, 1.4F);
        }
    }

    /** Puts what he carries (cuttings, the food he did not use) into the chests at the workplace. */
    static void deposit(ServerLevel level, AquaristEntity npc) {
        boolean full = false;
        for (int i = 0; i < npc.bag().getSlots(); i++) {
            ItemStack s = npc.bag().getStackInSlot(i);
            if (s.isEmpty()) continue;
            ItemStack rest = s.copy();
            for (IItemHandler h : chests(level, npc)) {
                rest = ItemHandlerHelper.insertItemStacked(h, rest, false);
                if (rest.isEmpty()) break;
            }
            npc.bag().setStackInSlot(i, rest);
            if (!rest.isEmpty()) full = true;
        }
        if (full) npc.complain(level, "chest_full", null);
        npc.pourBackWater(level);
    }

    // ---- water

    /** Takes (or with {@code simulate} only counts) up to {@code mb} of water that suits the tank from the workplace. */
    static long sourceWater(ServerLevel level, AquaristEntity npc, TankData t, long mb, boolean execute) {
        BlockPos wp = npc.workplace();
        if (wp == null || mb <= 0) return 0;
        boolean fresh = Salinity.kind(t.salinity()) == Salinity.Kind.FRESH || t.salinity() < Salinity.BRACKISH_FROM;
        long got = 0;
        for (IFluidHandler h : WaterSources.handlers(level, wp)) {
            if (got >= mb) break;
            FluidStack peek = FluidMoves.peekWater(h, (int) Math.min(Integer.MAX_VALUE, mb - got));
            if (peek.isEmpty()) continue;
            WaterValues v = WaterValues.from(peek);
            float salt = v == null ? 0F : v.salinity();
            if (fresh ? salt >= Salinity.BRACKISH_FROM : Math.abs(salt - t.salinity()) > 5F) continue;
            if (!execute) {
                got += peek.getAmount();
                continue;
            }
            FluidStack drained = h.drain(peek, IFluidHandler.FluidAction.EXECUTE);
            if (drained.isEmpty()) continue;
            npc.addWater(drained.getAmount(), WaterValues.from(drained));
            got += drained.getAmount();
        }
        // a water source block next to the workplace: endless tap water (fresh tanks only)
        if (got < mb && fresh && WaterSources.endless(level, wp)) {
            if (execute) npc.addWater((int) (mb - got), null);
            got = mb;
        }
        return got;
    }

    static void fetchWater(ServerLevel level, AquaristEntity npc, TankBlockEntity c) {
        long want = waterAmount(c.getTank(), npc.taskLevel(AquaristTask.WATER));
        if (want <= 0) return;
        npc.pourBackWater(level);
        long got = sourceWater(level, npc, c.getTank(), want, true);
        if (got <= 0) {
            npc.complain(level, "no_water", c);
            return;
        }
        npc.setWaterFor(c.getTank().id());
        npc.swingWork(level, npc.workplace());
        level.playSound(null, npc.workplace(), SoundEvents.BUCKET_FILL, SoundSource.NEUTRAL, 0.6F, 1.0F);
    }

    static void changeWater(ServerLevel level, AquaristEntity npc, TankBlockEntity c) {
        TankData t = c.getTank();
        long amount = Math.min(npc.waterMb(), waterAmount(t, npc.taskLevel(AquaristTask.WATER)));
        if (amount <= 0) {
            npc.pourBackWater(level);
            return;
        }
        WaterValues v = npc.waterValues();
        TankInteractions.changeWater(level, c, -amount);
        if (v == null) TankInteractions.changeWater(level, c, amount);
        else TankInteractions.addWater(level, c, amount, v);
        if (t.lastWaterChange() != level.getGameTime()) t.setLastWaterChange(level.getGameTime()); // water with values: still a change
        npc.useWater((int) amount);
        t.setAquaristDone(AquaristTask.WATER.id(), level.getGameTime());
        TankCare.log(level, c, "log.bettacraft.aquarist.water", npc.logName(), String.valueOf(Math.round(amount * 100.0 / Math.max(1, t.capacityMb()))),
                String.valueOf(amount));
        level.playSound(null, c.getBlockPos(), SoundEvents.BUCKET_EMPTY, SoundSource.NEUTRAL, 0.7F, 1.0F);
        if (npc.waterMb() > 0) npc.pourBackWater(level);
    }

    // ---- young

    static void takeYoung(ServerLevel level, AquaristEntity npc, TankBlockEntity c, @Nullable UUID to) {
        int lvl = npc.taskLevel(AquaristTask.YOUNG);
        TankBlockEntity rearing = tank(level, npc, to != null ? to : npc.rearingTank());
        if (lvl <= 0 || rearing == null) return;
        TankData t = c.getTank();
        int n = 0;
        for (Entity e : young(level, c, 64)) {
            if (n >= YOUNG_PER_TRIP[lvl - 1]) break;
            AquaristEntity.RearingTank r = null;
            for (AquaristEntity.RearingTank x : npc.rearingTanks()) if (x.tank().equals(rearing.getTank().id())) r = x;
            if ((r != null && !r.takes(speciesKey(e))) || !rearingTakes(level, rearing, e) || t.waterMb() < TankInhabitants.BAG_MB) continue;
            ItemStack bag = FishBagItem.fillWith(e);
            if (TankInhabitants.containedType(bag) != e.getType()) continue;
            TankInhabitants.release(e);
            TankCare.log(level, c, "log.bettacraft.aquarist.young_out", npc.logName(), TankCare.nameArg(e));
            t.inhabitants().remove(e.getUUID());
            e.discard();
            TankInteractions.changeWater(level, c, -TankInhabitants.BAG_MB);
            npc.carry(bag, t.id());
            n++;
        }
        if (n > 0) {
            npc.setCarriedTo(rearing.getTank().id());
            TankInhabitants.reconcile(level, c);
            level.playSound(null, c.getBlockPos(), SoundEvents.BUCKET_FILL_FISH, SoundSource.NEUTRAL, 0.8F, 1.1F);
        }
    }

    /** Puts the carried young into {@code c}; returns how many he still carries (refused). */
    static int releaseYoung(ServerLevel level, AquaristEntity npc, TankBlockEntity c, boolean back) {
        List<ItemStack> left = new ArrayList<>();
        int in = 0;
        for (ItemStack bag : npc.carriedAnimals()) {
            EntityType<?> type = TankInhabitants.containedType(bag);
            net.minecraft.network.chat.Component err = type == null ? net.minecraft.network.chat.Component.literal("?")
                    : TankInhabitants.insert(level, c, bag, type, null, TankInhabitants.BAG_MB);
            if (err == null) in++;
            else left.add(bag);
        }
        npc.setCarried(left);
        if (in > 0) {
            if (!back) c.getTank().setAquaristDone(AquaristTask.YOUNG.id(), level.getGameTime());
            TankCare.log(level, c, back ? "log.bettacraft.aquarist.young_back" : "log.bettacraft.aquarist.young_in", npc.logName(), String.valueOf(in));
            level.playSound(null, c.getBlockPos(), SoundEvents.BUCKET_EMPTY_FISH, SoundSource.NEUTRAL, 0.8F, 1.0F);
        }
        return left.size();
    }

    // ---- warnings

    /** The warnings of a tank at this level (keys below {@code aquarist.bettacraft.warning.}). */
    public static Set<String> warnings(ServerLevel level, TankBlockEntity c, int lvl) {
        Set<String> out = new LinkedHashSet<>();
        TankData t = c.getTank();
        List<Entity> live = TankInhabitants.liveInhabitants(level, t);
        boolean sick = false, hungry = false, temp = false, water = false, crowded = false;
        var p = org.overengineer.bettacraft.entity.betta.BettaCare.params();
        for (Entity e : live) {
            org.overengineer.bettacraft.entity.betta.BettaHealth h = e instanceof BettaEntity b ? b.health()
                    : e instanceof org.overengineer.bettacraft.entity.tankmate.TankmateEntity m ? m.health() : null;
            if (h == null) continue;
            sick |= h.isSick();
            hungry |= h.isHungry(p) && h.foodStress() > 0.5;
            temp |= h.tempStress() > 0.25;
            water |= h.waterStress() > 0.25 || h.saltShock() > 0.1;
            crowded |= h.crowdStress() > 0.25;
        }
        if (sick) out.add("sick");
        if (!live.isEmpty() && t.capacityMb() > 0 && t.waterMb() < 0.5 * t.capacityMb() && !t.terrariumMode().hasLand()) out.add("water_low");
        if (lvl >= 2) {
            if (temp) out.add("temperature");
            if (water || !live.isEmpty() && t.waterQuality() < 0.4F) out.add("water_quality");
            if (hungry) out.add("hungry");
            if (crowded) out.add("crowded");
        }
        if (lvl >= 3) for (String w : org.overengineer.bettacraft.tank.TankBreeding.warnings(level, c)) out.add("breeding." + w);
        return out;
    }

    // ------------------------------------------------------------------ the workplace

    /** The item handlers of the blocks around the workplace (chests, barrels, other mods' storage). */
    public static List<IItemHandler> chests(ServerLevel level, @Nullable BlockPos wp) {
        List<IItemHandler> out = new ArrayList<>();
        if (wp == null) return out;
        for (BlockPos p : BlockPos.betweenClosed(wp.offset(-1, -1, -1), wp.offset(1, 1, 1))) {
            if (p.equals(wp) || !level.isLoaded(p)) continue;
            BlockEntity be = level.getBlockEntity(p);
            if (be == null || be instanceof TankBlockEntity) continue;
            IItemHandler h = org.overengineer.bettacraft.platform.Platform.get().itemHandler(level, p.immutable(), null);
            if (h != null) out.add(h);
        }
        return out;
    }

    /** M66: his chest: the assigned one when there is one (and it is still a container), else the blocks around the workplace. */
    public static List<IItemHandler> chests(ServerLevel level, AquaristEntity npc) {
        BlockPos c = npc.chest();
        if (c != null && level.isLoaded(c)) {
            IItemHandler h = org.overengineer.bettacraft.platform.Platform.get().itemHandler(level, c, null);
            if (h != null) return new ArrayList<>(List.of(h));
        }
        return chests(level, npc.workplace());
    }

    /** Whether his chests hold food for the tanks. */
    public static boolean chestsHaveFood(ServerLevel level, AquaristEntity npc) {
        for (IItemHandler h : chests(level, npc)) for (int i = 0; i < h.getSlots(); i++) if (isTankFood(h.getStackInSlot(i))) return true;
        return false;
    }

    /** The box around a tank he works from (one block out). */
    public static AABB reach(TankData t) {
        return TankInhabitants.tankBox(t).inflate(2.2, 1.5, 2.2);
    }
}
