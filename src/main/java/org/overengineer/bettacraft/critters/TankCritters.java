package org.overengineer.bettacraft.critters;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.network.TankCrittersPayload;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankWater;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;

/**
 * Shrimp, crayfish and snails of a tank on the server (M14): the care step (movement every second, life, food, clutches,
 * moults, bettas hunting, the log, the sync to the clients), putting them in from a fish bag, taking them out into one
 * (menu, aquarium net), and what they mean for the tank (stocking, bioload, leftovers, the break rule). The animals live
 * in {@link TankData#critters()}; the rules are in {@link CritterMovement} and {@link CritterLife}.
 */
public final class TankCritters {
    /** Animals one fish bag holds (all of one species). */
    public static final int MAX_PER_BAG = 16;
    /** Reach of the aquarium net around the view ray (blocks plus the animal's size). */
    public static final double NET_REACH = 0.08;
    /** Food portions per fully grown algae face eaten. */
    public static final double FOOD_PER_ALGAE = 0.5;
    /** Share of every portion the bettas eat that sinks to the floor for the small animals. */
    public static final double CRUMBS = 0.1;

    private TankCritters() {}

    // ------------------------------------------------------------------ config

    public static int maxPerTank() {
        return ServerConfig.getOr(ServerConfig.CRITTERS_MAX_PER_TANK, 200);
    }

    public static Map<CritterSpecies.Group, Double> breeding() {
        Map<CritterSpecies.Group, Double> m = new EnumMap<>(CritterSpecies.Group.class);
        m.put(CritterSpecies.Group.SHRIMP, ServerConfig.getOr(ServerConfig.CRITTERS_SHRIMP_BREEDING, 1.0));
        m.put(CritterSpecies.Group.CRAYFISH, ServerConfig.getOr(ServerConfig.CRITTERS_CRAYFISH_BREEDING, 1.0));
        m.put(CritterSpecies.Group.SNAIL, ServerConfig.getOr(ServerConfig.CRITTERS_SNAIL_BREEDING, 1.0));
        return m;
    }

    /** How many of the group count as one fish (config stocking.*PerFish). */
    public static double perFish(CritterSpecies.Group g) {
        return switch (g) {
            case SHRIMP -> ServerConfig.getOr(ServerConfig.STOCKING_SHRIMP_PER_FISH, 25);
            case CRAYFISH -> ServerConfig.getOr(ServerConfig.STOCKING_CRAYFISH_PER_FISH, 12);
            case SNAIL -> ServerConfig.getOr(ServerConfig.STOCKING_SNAILS_PER_FISH, 50);
        };
    }

    /**
     * Stocking of the small animals in fish: adults 1 / stocking.*PerFish, M18: the young at the share of a juvenile betta
     * (stocking.juvenileStocking, 0.1 by default) of that.
     */
    public static double stock(TankData t) {
        double s = 0;
        for (Critter c : t.critters()) if (c.alive()) s += weight(c);
        return s;
    }

    /** M18: how much of a fish one small animal counts. */
    public static double weight(Critter c) {
        double w = 1.0 / Math.max(1e-6, perFish(c.species.group()));
        return c.adult() ? w : w * org.overengineer.bettacraft.entity.betta.LifeStage.JUVENILE.stocking();
    }

    /** Bioload of the small animals (in fish): all living ones, young at half. */
    public static double bioload(TankData t) {
        double s = 0;
        for (Critter c : t.critters()) if (c.alive()) s += (c.adult() ? 1.0 : 0.5) / Math.max(1e-6, perFish(c.species.group()));
        return s;
    }

    public static int alive(TankData t) {
        return CritterLife.alive(t.critters());
    }

    // ------------------------------------------------------------------ terrain

    @Nullable
    public static InteriorRegion region(ServerLevel level, TankData t) {
        InteriorRegion r = InteriorRegions.get(level.getServer()).get(t.regionId());
        return r != null && r.built() ? r : null;
    }

    public static TankTerrain terrain(ServerLevel level, TankData t) {
        long dt = level.getDayTime() % 24000L;
        boolean day = !level.dimensionType().hasFixedTime() ? dt < 12500L || dt > 23500L : true;
        return new TankTerrain(t, level.getServer(), region(level, t), day);
    }

    private static RandomGenerator random(Level level) {
        var r = level.getRandom();
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return r.nextLong();
            }

            @Override
            public double nextGaussian() {
                return r.nextGaussian();
            }
        };
    }

    // ------------------------------------------------------------------ hiding, food

    /** Share of the hunts that fail because the young hide in plants, Java moss and floating plant roots (0..0.9). */
    public static double hiding(ServerLevel level, TankData t) {
        double share = org.overengineer.bettacraft.plants.InteriorPlants.volumeShare(level.getServer(), t);
        double h = 0.75 * Math.min(1.0, share / org.overengineer.bettacraft.plants.InteriorPlants.HIDING_FULL_AT);
        InteriorRegion r = region(level, t);
        if (r != null) {
            for (var p : r.plants().all()) {
                if (p.species == org.overengineer.bettacraft.plants.PlantSpecies.JAVA_MOSS && p.stage >= 2) {
                    h += 0.15;
                    break;
                }
            }
        }
        h += 0.1 * Math.min(1.0, org.overengineer.bettacraft.tank.plants.TankPlants.coverage(t));
        return Math.min(0.9, h);
    }

    /** Food the tank grows per day for the small animals: biofilm per water block, plants (interior and floating). */
    public static double foodPerDay(ServerLevel level, TankData t) {
        if (t.waterMb() <= 0) return 0;
        double f = ServerConfig.getOr(ServerConfig.CRITTERS_BIOFILM, 0.03) * t.waterBlocks();
        f += org.overengineer.bettacraft.plants.InteriorPlants.volumeShare(level.getServer(), t) * t.waterBlocks();
        f += 0.02 * t.plants().size();
        return f;
    }

    /** Adds food to the pool (up to what it keeps); returns the portions taken. */
    public static double addFood(TankData t, double portions) {
        if (portions <= 0) return 0;
        double cap = CritterLife.poolCap(t.critters());
        double room = Math.max(0, cap - t.critterFood());
        double take = Math.min(room, portions);
        t.setCritterFood(t.critterFood() + take);
        return take;
    }

    /** Whether the small animals would eat food put into the tank (alive and the pool below a day of their needs). */
    public static boolean wantFood(TankData t) {
        if (alive(t) == 0) return false;
        return t.critterFood() < CritterLife.demand(t.critters());
    }

    /**
     * Food the fish did not need (overfeeding, food left on the plants): the shrimp, crayfish and snails eat what they
     * can, the rest rots. Returns the portions that rot.
     */
    public static int eatLeftovers(TankData t, int portions) {
        if (portions <= 0 || alive(t) == 0) return portions;
        double eaten = addFood(t, portions);
        return Math.max(0, portions - (int) Math.floor(eaten + 1e-6));
    }

    // ------------------------------------------------------------------ care step

    /** One care step (every 20 ticks, {@code days} since the last one). */
    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        TankData t = c.getTank();
        if (t.critters().isEmpty() && t.critterObjects().isEmpty()) return;
        RandomGenerator rnd = random(level);
        TankTerrain terrain = terrain(level, t);
        long now = level.getGameTime();
        // one second of movement
        for (Critter k : t.critters()) CritterMovement.step(k, terrain, rnd);
        // apple snails above the water line lay their clutch there
        for (Critter k : t.critters()) {
            if (k.alive() && CritterMovement.laying(k, terrain) && k.onGlass() && Double.isFinite(terrain.surface()) && k.y > terrain.surface() + 0.005) {
                CritterObject o = CritterLife.layClutch(k, t.nextCritterId(), rnd);
                t.critterObjects().add(o);
                TankCare.log(level, c, "log.bettacraft.clutch_laid", "@" + k.species.translationKey());
                k.idle = 5;
                k.ty = terrain.ground(k.x, k.z);
            }
        }
        // the betta nips at the feelers of an apple snail now and then (only a look)
        List<BettaEntity> bettas = TankCare.bettas(level, t);
        if (!bettas.isEmpty()) {
            for (Critter k : t.critters()) {
                if (!k.alive() || k.species != CritterSpecies.APPLE_SNAIL || now < k.retractUntil) continue;
                if (rnd.nextDouble() < days * 3 * bettas.size()) {
                    k.retractUntil = now + 80;
                    BettaEntity b = bettas.get(rnd.nextInt(bettas.size()));
                    b.setFoodTarget(new Vec3(k.x, k.y + 0.08, k.z), now + 60);
                }
            }
        }
        // food: biofilm and plants
        addFood(t, foodPerDay(level, t) * days);
        int adults = 0;
        for (BettaEntity b : bettas) if (b.isAdult()) adults++;
        boolean dry = t.waterMb() <= 0;
        // breeding stops at the limit of the tank, and when the stocking (fish + small animals) is at the hard cap
        int cap = maxPerTank();
        if (TankInhabitants.stocking(level, t) >= TankInhabitants.hardCap(t.waterBlocks()) - 1e-6) cap = Math.min(cap, alive(t));
        CritterLife.Env env = new CritterLife.Env(dry ? 0F : t.waterQuality(), TankWater.qualityEnabled() || dry, t.waterTemp(), adults,
                hiding(level, t), cap, breeding(), Double.isFinite(terrain.airTop()),
                ServerConfig.getOr(ServerConfig.CRITTERS_BETTAS_HUNT, true), t.salinity(), org.overengineer.bettacraft.reef.SaltTolerance.speed());
        double[] pool = {t.critterFood()};
        CritterLife.Result r = CritterLife.step(t.critters(), t.critterObjects(), env, days, pool, rnd, t::nextCritterId);
        t.setCritterFood(pool[0]);
        afterLife(level, c, terrain, r, bettas, rnd);
        c.setChanged();
        sync(level, c);
    }

    private static void afterLife(ServerLevel level, TankBlockEntity c, TankTerrain terrain, CritterLife.Result r, List<BettaEntity> bettas,
                                  RandomGenerator rnd) {
        TankData t = c.getTank();
        long now = level.getGameTime();
        // the young start on the floor below their mother (or the clutch)
        for (Critter k : r.born) {
            k.face = Critter.FLOOR;
            double[] spot = null;
            double g = CritterMovement.groundFor(terrain, k.species, k.x, k.z);
            if (!Double.isFinite(g)) {
                double m = CritterMovement.margin(k.species) + 0.01;
                k.x = Math.max(t.min().getX() + m, Math.min(t.max().getX() + 1 - m, k.x));
                k.z = Math.max(t.min().getZ() + m, Math.min(t.max().getZ() + 1 - m, k.z));
                g = CritterMovement.groundFor(terrain, k.species, k.x, k.z);
            }
            if (!Double.isFinite(g)) spot = CritterMovement.randomSpot(terrain, k.species, rnd);
            if (spot != null) {
                k.x = spot[0];
                k.z = spot[2];
                g = spot[1];
            }
            k.y = Double.isFinite(g) ? g : t.min().getY() + 0.05;
            k.tx = k.x;
            k.ty = k.y;
            k.tz = k.z;
            k.idle = 2 + rnd.nextInt(5);
        }
        for (var e : r.bornBySpecies.entrySet()) {
            TankCare.log(level, c, "log.bettacraft.critters_born", String.valueOf(e.getValue()[0]), "@" + e.getKey().translationKey());
        }
        for (CritterObject o : r.hatchedClutches) {
            TankCare.log(level, c, "log.bettacraft.clutch_hatched", "@" + o.species.translationKey());
        }
        for (Critter k : r.molted) {
            t.critterObjects().add(CritterLife.molt(k, t.nextCritterId()));
        }
        // deaths: one line per species and half a minute
        Map<CritterSpecies, int[]> died = new EnumMap<>(CritterSpecies.class);
        Map<CritterSpecies, CritterLife.Cause> cause = new EnumMap<>(CritterSpecies.class);
        for (Critter k : r.died) {
            died.computeIfAbsent(k.species, s -> new int[1])[0]++;
            cause.put(k.species, r.causes.get(k));
        }
        for (var e : died.entrySet()) {
            Long last = c.critterDeathLog.get(e.getKey());
            if (last != null && now - last < 600) continue;
            c.critterDeathLog.put(e.getKey(), now);
            TankCare.log(level, c, "log.bettacraft.critters_died", String.valueOf(e.getValue()[0]), "@" + e.getKey().translationKey(),
                    "@cause.bettacraft.critter." + cause.get(e.getKey()).name().toLowerCase(java.util.Locale.ROOT));
        }
        for (Critter k : r.eaten) {
            CritterLife.Cause why = r.causes.get(k);
            String young = k.adult() ? "adult" : "young";
            if (why == CritterLife.Cause.BETTA && !bettas.isEmpty()) {
                BettaEntity b = bettas.get(rnd.nextInt(bettas.size()));
                b.setFoodTarget(new Vec3(k.x, k.y + 0.05, k.z), now + 40);
                TankCare.log(level, c, "log.bettacraft.critter_eaten_" + young, TankCare.nameArg(b), "@" + k.species.translationKey());
            } else if (why == CritterLife.Cause.ASSASSIN) {
                TankCare.log(level, c, "log.bettacraft.critter_assassin", "@" + k.species.translationKey());
            } else if (why == CritterLife.Cause.CRAYFISH) {
                TankCare.log(level, c, "log.bettacraft.critter_crayfish", "@" + k.species.translationKey());
            }
        }
        if (r.rotted > 0) TankWater.overfed(level, c, r.rotted);
    }

    /** Sends the animals to every player near the tank (they glide on the client over one care step). */
    public static void sync(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        byte[] data = CritterSync.encode(t.critters(), t.critterObjects(), t.min().getX(), t.min().getY(), t.min().getZ(), level.getGameTime());
        // M17: nothing moved or changed (resting snails, buried crayfish): no packet; at least every 10 s anyway
        if (!c.critterSync.changed(data, level.getGameTime())) return;
        PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(c.getBlockPos()), new TankCrittersPayload(c.getBlockPos(), data));
    }

    // ------------------------------------------------------------------ adding

    /** A new animal on a free spot of the floor (GameTests, smoke tests, bags). Null when the tank is full. */
    @Nullable
    public static Critter add(ServerLevel level, TankBlockEntity c, CritterSpecies s, CritterGenes genes, boolean female, double age) {
        TankData t = c.getTank();
        if (alive(t) >= maxPerTank()) return null;
        TankTerrain terrain = terrain(level, t);
        double[] spot = CritterMovement.randomSpot(terrain, s, random(level));
        if (spot == null) spot = new double[]{t.min().getX() + 0.5, t.min().getY() + TankTerrain.MARGIN, t.min().getZ() + 0.5};
        Critter k = new Critter(t.nextCritterId(), s, s.hasSexes() && female, genes, spot[0], spot[1], spot[2]);
        k.age = age;
        k.yaw = level.getRandom().nextFloat() * 360F;
        k.look = CritterLooks.random(s, random(level)); // M21.5
        CritterMovement.pick(k, terrain, random(level)); // off it goes, exploring the new tank
        t.critters().add(k);
        c.markTankChanged();
        return k;
    }

    /** Animals of a filled fish bag (or null when it holds none). */
    @Nullable
    public static List<CompoundTag> bagged(ItemStack stack) {
        if (!(stack.getItem() instanceof FishBagItem)) return null;
        List<CompoundTag> l = stack.get(ModDataComponents.CRITTERS.get());
        return l == null || l.isEmpty() ? null : l;
    }

    /** A filled fish bag with these animals. */
    public static ItemStack bag(List<CompoundTag> animals) {
        ItemStack bag = new ItemStack(ModItems.FISH_BAG.get());
        bag.set(ModDataComponents.CRITTERS.get(), List.copyOf(animals));
        bag.set(DataComponents.MAX_STACK_SIZE, 1);
        return bag;
    }

    /** A bag with {@code n} new animals of one kind (net catches, the trader, creative). */
    public static ItemStack bagOf(CritterSpecies s, int n, java.util.function.Supplier<CritterGenes> genes, RandomGenerator rnd, boolean pairs) {
        List<CompoundTag> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            boolean female = pairs ? i % 2 == 0 : rnd.nextBoolean();
            Critter k = new Critter(0, s, s.hasSexes() && female, genes.get(), 0, 0, 0);
            k.age = s.juvenileDays() + rnd.nextDouble();
            k.look = CritterLooks.random(s, rnd); // M21.5: every racer nerite / apple snail its own shade
            out.add(k.saveAnimal());
        }
        return bag(out);
    }

    /** Right-click on a tank with a bag of small animals: they go in (with the 100 mB of the bag). PASS for other stacks. */
    public static ItemInteractionResult tryInsert(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
        List<CompoundTag> animals = bagged(stack);
        if (animals == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        ServerLevel server = (ServerLevel) level;
        TankBlockEntity c = TankMultiblock.getController(level, pos);
        if (c == null) {
            player.displayClientMessage(Component.translatable("message.bettacraft.tank.not_formed"), true);
            return ItemInteractionResult.CONSUME;
        }
        Component error = insert(server, c, animals, player, TankInhabitants.BAG_MB);
        if (error != null) {
            player.displayClientMessage(error, true);
            return ItemInteractionResult.CONSUME;
        }
        player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(ModItems.FISH_BAG.get())));
        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_FISH, SoundSource.BLOCKS, 0.8F, 1.3F);
        return ItemInteractionResult.CONSUME;
    }

    /** Puts the animals into the tank; returns an error message or null on success. */
    @Nullable
    public static Component insert(ServerLevel level, TankBlockEntity c, List<CompoundTag> animals, @Nullable Player player, long addMb) {
        TankData t = c.getTank();
        if (player != null && !TankInhabitants.canModify(player, t)) return Component.translatable("message.bettacraft.tank.locked");
        double minFill = ServerConfig.getOr(ServerConfig.TANK_MIN_FILL_TO_INSERT, 0.5);
        if (t.capacityMb() <= 0 || t.waterMb() < minFill * t.capacityMb()) {
            return Component.translatable("message.bettacraft.tank.too_little_water", Math.round(minFill * 100));
        }
        if (alive(t) + animals.size() > maxPerTank()) return Component.translatable("message.bettacraft.critters.tank_full", maxPerTank());
        double add = 0;
        List<Critter> fresh = new ArrayList<>();
        for (CompoundTag a : animals) {
            Critter k = Critter.loadAnimal(a, 0, 0, 0, 0);
            if (k == null) continue;
            fresh.add(k);
            if (k.alive()) add += weight(k);
        }
        if (fresh.isEmpty()) return Component.translatable("message.bettacraft.critters.unknown");
        // M20: no freshwater shrimp into the reef tank (and no reef animals into fresh water)
        for (Critter k : fresh) {
            var range = org.overengineer.bettacraft.reef.SaltTolerance.critter(k.species);
            if (org.overengineer.bettacraft.reef.SaltTolerance.refuse(range, t)) {
                return Component.translatable(org.overengineer.bettacraft.reef.SaltTolerance.refuseKey(range),
                        Component.translatable(k.species.translationKey()), org.overengineer.bettacraft.reef.ReefItems.fmt(t.salinity()));
            }
        }
        long waterAfter = Math.min(t.capacityMb(), t.waterMb() + addMb);
        int cap = TankInhabitants.hardCap(waterAfter / (double) TankData.MB_PER_BLOCK);
        double stock = TankInhabitants.stocking(level, t);
        if (stock + add > cap + 1e-6) return Component.translatable("message.bettacraft.tank.crowded_full", cap);
        TankTerrain terrain = terrain(level, t);
        RandomGenerator rnd = random(level);
        for (Critter k : fresh) {
            double[] spot = CritterMovement.randomSpot(terrain, k.species, rnd);
            if (spot == null) spot = new double[]{t.min().getX() + 0.5, t.min().getY() + TankTerrain.MARGIN, t.min().getZ() + 0.5};
            Critter placed = Critter.loadAnimal(k.saveAnimal(), t.nextCritterId(), spot[0], spot[1], spot[2]);
            placed.yaw = rnd.nextFloat() * 360F;
            CritterMovement.pick(placed, terrain, rnd);
            t.critters().add(placed);
        }
        if (addMb > 0) TankInteractions.changeWater(level, c, addMb, c.getTank().salinity()); // M20
        Critter first = fresh.getFirst();
        TankCare.log(level, c, "log.bettacraft.critters_inserted", String.valueOf(fresh.size()), "@" + first.species.translationKey());
        TankInhabitants.reconcile(level, c);
        c.markTankChanged();
        sync(level, c);
        return null;
    }

    // ------------------------------------------------------------------ taking out

    /** The kind of an animal for the menu: species and variety. */
    public static String groupKey(Critter k) {
        return k.species.id() + ":" + k.genes.variety(k.species);
    }

    /**
     * Takes one adult of a kind out into a fish bag (the best grade, or the worst for {@code worst}: picking out the
     * weak ones of a breeding line). It goes into a bag of the same species the player already carries (up to
     * {@link #MAX_PER_BAG}), otherwise into an empty fish bag (-100 mB).
     */
    @Nullable
    public static Component takeOut(ServerLevel level, TankBlockEntity c, Player player, String group, boolean worst) {
        return takeOut(level, c, player, group, worst, 1);
    }

    /** As {@link #takeOut(ServerLevel, TankBlockEntity, Player, String, boolean)} up to {@code count} animals (a full bag). */
    @Nullable
    public static Component takeOut(ServerLevel level, TankBlockEntity c, Player player, String group, boolean worst, int count) {
        for (int i = 0; i < Math.max(1, count); i++) {
            Component e = takeOne(level, c, player, group, worst);
            if (e != null) return i == 0 ? e : null; // the first one failed: say why; later ones: the bag is full or none left
        }
        return null;
    }

    @Nullable
    private static Component takeOne(ServerLevel level, TankBlockEntity c, Player player, String group, boolean worst) {
        TankData t = c.getTank();
        if (!TankInhabitants.canModify(player, t)) return Component.translatable("message.bettacraft.tank.locked");
        Critter best = null;
        for (Critter k : t.critters()) {
            if (!k.alive() || !groupKey(k).equals(group)) continue;
            if (best == null || (best.adult() != k.adult() ? k.adult()
                    : worst ? k.genes.grade() < best.genes.grade() : k.genes.grade() > best.genes.grade())) best = k;
        }
        if (best == null) return Component.translatable("message.bettacraft.critters.none_of_kind");
        return catchOne(level, c, player, best);
    }

    /** Puts {@code k} into a bag of the player (see {@link #takeOut}); null on success, else the reason. */
    @Nullable
    public static Component catchOne(ServerLevel level, TankBlockEntity c, Player player, Critter k) {
        TankData t = c.getTank();
        var inv = player.getInventory();
        int same = -1;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            List<CompoundTag> in = bagged(inv.getItem(i));
            if (in == null || in.size() >= MAX_PER_BAG) continue;
            Critter first = Critter.loadAnimal(in.getFirst(), 0, 0, 0, 0);
            if (first != null && first.species == k.species) {
                same = i;
                break;
            }
        }
        if (same >= 0) {
            ItemStack bag = inv.getItem(same);
            List<CompoundTag> l = new ArrayList<>(bagged(bag));
            l.add(k.saveAnimal());
            bag.set(ModDataComponents.CRITTERS.get(), List.copyOf(l));
        } else {
            int slot = -1;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (s.getItem() instanceof FishBagItem && !FishBagItem.isFilled(s)) {
                    slot = i;
                    break;
                }
            }
            if (slot < 0) return Component.translatable("message.bettacraft.tank.need_bag");
            if (t.waterMb() < TankInhabitants.BAG_MB) return Component.translatable("message.bettacraft.tank.not_enough_water");
            if (!player.getAbilities().instabuild) inv.getItem(slot).shrink(1);
            ItemStack filled = bag(List.of(k.saveAnimal()));
            if (!inv.add(filled)) player.drop(filled, false);
            TankInteractions.changeWater(level, c, -TankInhabitants.BAG_MB);
        }
        t.critters().remove(k);
        TankCare.log(level, c, "log.bettacraft.critter_taken", TankCare.nameArg(player), "@" + k.species.translationKey());
        level.playSound(null, player.blockPosition(), SoundEvents.BUCKET_FILL_FISH, SoundSource.NEUTRAL, 0.7F, 1.4F);
        c.markTankChanged();
        sync(level, c);
        return null;
    }

    /** Takes all clutches off the glass (menu button); returns how many. */
    public static int removeClutches(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        int n = 0;
        for (var it = t.critterObjects().iterator(); it.hasNext(); ) {
            if (it.next().kind == CritterObject.Kind.CLUTCH) {
                it.remove();
                n++;
            }
        }
        if (n > 0) {
            c.markTankChanged();
            sync(level, c);
        }
        return n;
    }

    // ------------------------------------------------------------------ aiming (aquarium net)

    /** What the aquarium net is aimed at: a small animal, a body, a clutch or a moult. */
    public record Aimed(@Nullable Critter critter, @Nullable CritterObject object, double distance) {}

    /** The animal or object closest to the view ray (within reach and 12 blocks), or null. */
    @Nullable
    public static Aimed aimed(TankData t, Vec3 eye, Vec3 look) {
        Aimed best = null;
        for (Critter k : t.critters()) {
            if (k.buried) continue;
            double d = along(eye, look, k.x, k.y + k.species.length() * 0.2, k.z, NET_REACH + k.species.length() * 0.5 * k.size());
            if (d >= 0 && (best == null || d < best.distance())) best = new Aimed(k, null, d);
        }
        for (CritterObject o : t.critterObjects()) {
            double d = along(eye, look, o.x, o.y, o.z, NET_REACH + 0.08);
            if (d >= 0 && (best == null || d < best.distance())) best = new Aimed(null, o, d);
        }
        return best;
    }

    /** Distance along the ray to the point if it passes within {@code reach}, else -1. */
    static double along(Vec3 eye, Vec3 look, double x, double y, double z, double reach) {
        double px = x - eye.x, py = y - eye.y, pz = z - eye.z;
        double s = px * look.x + py * look.y + pz * look.z;
        if (s < 0 || s > 12) return -1;
        double cx = px - look.x * s, cy = py - look.y * s, cz = pz - look.z * s;
        return cx * cx + cy * cy + cz * cz <= reach * reach ? s : -1;
    }

    /** The aquarium net on an animal / object in the tank. Returns the message for the player (null = nothing aimed at). */
    @Nullable
    public static Component net(ServerLevel level, TankBlockEntity c, Player player, Aimed a) {
        TankData t = c.getTank();
        if (a.object() != null) {
            t.critterObjects().remove(a.object());
            c.markTankChanged();
            sync(level, c);
            level.playSound(null, player.blockPosition(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.NEUTRAL, 0.6F, 1.2F);
            if (a.object().kind == CritterObject.Kind.CLUTCH) {
                TankCare.log(level, c, "log.bettacraft.clutch_removed", TankCare.nameArg(player));
                return Component.translatable("message.bettacraft.critters.clutch_removed").withStyle(ChatFormatting.GREEN);
            }
            return Component.translatable("message.bettacraft.critters.molt_removed").withStyle(ChatFormatting.GREEN);
        }
        Critter k = a.critter();
        if (k == null) return null;
        if (!k.alive()) {
            t.critters().remove(k);
            c.markTankChanged();
            sync(level, c);
            return Component.translatable("message.bettacraft.critters.body_removed", Component.translatable(k.species.translationKey()));
        }
        Component err = catchOne(level, c, player, k);
        if (err != null) return err.copy().withStyle(ChatFormatting.RED);
        return Component.translatable("message.bettacraft.critters.caught", name(k)).withStyle(ChatFormatting.GREEN);
    }

    // ------------------------------------------------------------------ names, break rule

    /** "Sakura (Neocaridina shrimp)" or just the species for one-coloured ones. */
    public static MutableComponent name(Critter k) {
        return name(k.species, k.genes.variety(k.species));
    }

    public static MutableComponent name(CritterSpecies s, String variety) {
        if (s.loci().length == 0) return Component.translatable(s.translationKey());
        return Component.translatable("critter.bettacraft.variety_of", Component.translatable("variety.bettacraft." + s.id() + "." + variety),
                Component.translatable(s.translationKey()));
    }

    /** Living animals or clutches keep the tank from being broken (like the bettas). */
    @Nullable
    public static Component breakBlocked(TankData t) {
        int n = alive(t);
        if (n > 0) return Component.translatable("message.bettacraft.tank.break_blocked_critters", n);
        return null;
    }

    /** Counts per group for the info overlays and the menu: {shrimp, crayfish, snails, young}. */
    public static int[] counts(TankData t) {
        int[] n = new int[4];
        for (Critter k : t.critters()) {
            if (!k.alive()) continue;
            n[k.species.group().ordinal()]++;
            if (!k.adult()) n[3]++;
        }
        return n;
    }
}
