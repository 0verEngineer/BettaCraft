package org.overengineer.bettacraft.tank;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.equipment.FeederBlockEntity;
import org.overengineer.bettacraft.equipment.HeaterItem;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.food.BettaFood;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * Server-side care simulation of a tank controller: water temperature, inhabitant reconciliation, feeding and the
 * tank log (PLAN 5.5, 6.1, 6.3).
 */
public final class TankCare {
    private static final int INTERVAL = 20;
    private static final int RECONCILE_INTERVAL = 40;
    /** One food item (pellet or larvae portion) feeds up to this many fish. */
    public static final int FISH_PER_FOOD = BettaFood.FISH_PER_FOOD;
    /** A fish fed again within this many days was not hungry: its portion rots in the water (M9 overfeeding). */
    public static final double OVERFED_DAYS = 0.5;

    private TankCare() {}

    public static void serverTick(Level level, BlockPos pos, BlockState state, TankBlockEntity be) {
        // M32 part b: a tank block put down by a mover (Create contraption, Sable) first takes its tank along
        if (be.movedFrom != null && level instanceof ServerLevel s) {
            TankMoves.tick(s, pos, be);
            return;
        }
        if (be.orphanCheck > 0 && --be.orphanCheck == 0 && level instanceof ServerLevel s) TankMultiblock.repairOrphan(s, pos, be);
        if (!(level instanceof ServerLevel server) || !be.isController()) return;
        if (!be.regionChecked) { // M53c: region ids are reused: one that belongs to another tank now is let go
            be.regionChecked = true;
            org.overengineer.bettacraft.tank.interior.InteriorManager.checkRegionOwner(server, be);
        }
        if (!be.stowed.isEmpty()) TankMoves.unpackLeftovers(server, pos, be);
        // M32 part b: on a moving structure (Sable sub-level) the care is paused and the animals travel packed (M50: unless
        // the care runs on the road, features.compat.create.travelCare)
        if ((level.getGameTime() + pos.asLong()) % INTERVAL == 0 && TankMoves.travelling(level, pos) && !TravelCare.enabled()) return;
        // the air gap below the glass (tank.waterHeadroom) is in the fill states: recompute them once after loading and
        // whenever the config changes, not only on the next change of the water amount
        int headroom = TankData.headroom();
        if (be.appliedHeadroom != headroom && server.isLoaded(be.getTank().min()) && server.isLoaded(be.getTank().max())) {
            be.appliedHeadroom = headroom;
            TankMultiblock.applyStates(server, be.getTank());
        }
        // the energy capabilities only exist while a power option is on: when that changes, tell cables and pipes nearby
        // (they cache capabilities; before, they only noticed after a chunk reload)
        boolean power = TankPower.anyRequired();
        if (be.appliedPower == null || be.appliedPower != power) {
            if (be.appliedPower != null) TankPower.invalidateCapabilities(server, be.getTank());
            be.appliedPower = power;
        }
        long now = level.getGameTime();
        if ((now + pos.asLong()) % INTERVAL != 0) return;
        long dt = be.lastCareTick < 0 ? INTERVAL : Math.max(0, now - be.lastCareTick);
        if (!ServerConfig.getOr(ServerConfig.SIMULATE_UNLOADED_TIME, false)) dt = Math.min(dt, INTERVAL * 5L);
        be.lastCareTick = now;
        care(server, be, dt);
        // M51: the index of owned tanks (the arrival of the hired aquarist counts them, he finds his tanks in it)
        if (Features.enabled(Feature.AQUARIST_NPC)) org.overengineer.bettacraft.aquarist.AquaristRecords.get(server.getServer()).noteTank(server, be);
        // M50: a tank whose devices run on power on an airship
        if (be.lastDemand > 0 && be.filterPowered && be.heatPowered && TankMoves.livesOn(level, pos)) {
            org.overengineer.bettacraft.progression.Progress.awardNear(server, be, org.overengineer.bettacraft.progression.Progress.POWERED_TRIP, 0);
        }
        if ((now / INTERVAL) % (RECONCILE_INTERVAL / INTERVAL) == 0) {
            TankInhabitants.reconcile(server, be);
        }
    }

    /** One care step of {@code dt} game ticks: every system of the tank in its order (also used by GameTests). */
    public static void care(ServerLevel server, TankBlockEntity be, long dt) {
        TankData tank = be.getTank();
        if (org.overengineer.bettacraft.debug.LogArea.CARE.trace) org.overengineer.bettacraft.debug.LogArea.CARE.trace("care of tank {} at {}: {} ticks, {} animals, water {} mB, quality {}", tank.id(), be.getBlockPos(), dt,
                tank.inhabitants().size(), tank.waterMb(), tank.waterQuality()); // M57
        if (tank.hasLegacyHeaters()) TankEquipment.moveLegacyHeaters(server, be);
        TankPower.step(server, be, dt);
        TankDrive.step(server, be, dt); // M41: Create's rotation refills the buffer with what the devices took
        org.overengineer.bettacraft.equipment.Sump.step(server, be, dt / 24000.0); // M49: the water of the sumps, their media and refugia
        updateTemperature(server, be, dt / 24000.0);
        TankWater.step(server, be, dt / 24000.0);
        TankNitrate.step(server, be, dt / 24000.0); // M24: nitrate rises, water changes lower it
        TankLighting.step(server, be); // M23: dimmer and daylight timer
        org.overengineer.bettacraft.terrarium.TankClimate.step(server, be, dt / 24000.0); // M31: terrarium mode and climate
        TankAeration.step(server, be, dt / 24000.0); // M23: air pumps, oxygen, fry intake
        org.overengineer.bettacraft.co2.TankCo2.step(server, be, dt / 24000.0); // M25: CO2 dosing
        org.overengineer.bettacraft.chem.TankChemistry.step(server, be, dt / 24000.0); // M25: TDS and pH
        org.overengineer.bettacraft.plumbing.interior.InteriorPlumbing.syncStates(server, be); // M24: outlets show their stream
        if (server.getGameTime() % 100 < 20) org.overengineer.bettacraft.plumbing.interior.TankAnchors.sweep(server, be); // M40: docks without a pipe go
        org.overengineer.bettacraft.tank.plants.TankPlants.step(server, be, dt / 24000.0);
        org.overengineer.bettacraft.plants.InteriorPlants.step(server, be, dt / 24000.0);
        org.overengineer.bettacraft.fertilizer.Fertilizer.step(server, be, dt / 24000.0); // M37: the fertilizer fades a little by itself
        org.overengineer.bettacraft.tank.algae.TankAlgae.step(server, be, dt / 24000.0); // M14
        org.overengineer.bettacraft.critters.TankCritters.step(server, be, dt / 24000.0); // M14
        TankFrogs.step(server, be); // M14
        org.overengineer.bettacraft.entity.frog.FrogBreeding.step(server, be, dt / 24000.0); // M47 part c: amplexus, spawn, tadpoles
        AxolotlCare.step(server, be, dt / 24000.0); // M29 part d
        org.overengineer.bettacraft.big.DolphinCare.step(server, be, dt / 24000.0); // M30
        org.overengineer.bettacraft.terrarium.TerrariumCare.step(server, be, dt / 24000.0); // M31: the needs of the terrarium animals
        org.overengineer.bettacraft.terrarium.HerpBreeding.step(server, be, dt / 24000.0); // M31 part b: nests, eggs, hatchlings
        org.overengineer.bettacraft.mobs.GlassMobs.step(server, be, dt / 24000.0); // M31 part d: growing, drowning, breeding, luring mobs
        org.overengineer.bettacraft.foreign.ForeignCare.step(server, be, dt / 24000.0); // M32 part a: our needs for animals of other mods
        org.overengineer.bettacraft.foreign.ForeignBreeding.step(server, be, dt / 24000.0); // M32 part a: their breeding ours, their own suppressed
        org.overengineer.bettacraft.foreign.ForeignTerrarium.hunt(server, be, dt / 24000.0); // M35: an alligator eats the small fish
        org.overengineer.bettacraft.foreign.ForeignHabits.step(server, be, dt / 24000.0); // M39 part b: fliers out of an open top, the clean-up crew
        org.overengineer.bettacraft.foreign.MinecraftPets.step(server, be, dt / 24000.0); // M39 part c: the needs of the Minecraft pets, no farm, bait
        TankBreeding.step(server, be, dt / 24000.0);
        org.overengineer.bettacraft.tankmates.TankmateBreeding.step(server, be, dt / 24000.0); // M19
        org.overengineer.bettacraft.tankmates.Archerfish.step(server, be, dt / 24000.0); // M49: insects above the water, the shots
        org.overengineer.bettacraft.plants.Mangroves.step(server, be, dt / 24000.0); // M49: the mangroves of the interior
        org.overengineer.bettacraft.critters.AmanoLarvae.step(server, be, dt / 24000.0); // M49: the Amano larvae die or settle
        org.overengineer.bettacraft.reef.ReefCare.step(server, be, dt / 24000.0); // M20: salt shock of frogs and foreign animals
        org.overengineer.bettacraft.reef.ReefDosing.step(server, be, dt / 24000.0); // M25 part c: magnesium and trace elements
        org.overengineer.bettacraft.reef.Copepods.step(server, be, dt / 24000.0); // M48 part b: the copepods of the live rock, the mandarins
        org.overengineer.bettacraft.reef.ClownLarvae.step(server, be, dt / 24000.0); // M48 part b: phytoplankton, rotifers, nauplii
        org.overengineer.bettacraft.monitor.TankHistory.onCare(server, be); // M27: one point per hour for the monitors' graphs
    }

    // ------------------------------------------------------------------ temperature

    public static float ambient(ServerLevel level, TankData tank) {
        BlockPos c = BlockPos.containing(TankInhabitants.tankBox(tank).getCenter());
        // M50: on an airship (Sable sub-level) the air of the place where the ship is, not of its plot far away
        if (TankMoves.travelling(level, c)) return TravelCare.ambientAt(level, TankMoves.PROJECT.apply(level, TankInhabitants.tankBox(tank).getCenter()));
        float biome = level.getBiome(c).value().getBaseTemperature();
        float t = TankTemperature.ambientFromBiome(biome);
        if (level.dimensionType().hasSkyLight() && !level.dimensionType().hasFixedTime()) {
            t += TankTemperature.dayNightOffset(level.getDayTime());
        }
        return t;
    }

    /** Heating capacity of the heaters left in the old menu slots (M1-M3, see {@link TankData#legacyHeaters}). */
    public static double slotHeaterBlocks(TankData tank) {
        if (!tank.hasLegacyHeaters()) return 0;
        double sum = 0;
        var c = tank.legacyHeaters();
        for (int i = 0; i < c.getContainerSize(); i++) {
            if (c.getItem(i).getItem() instanceof HeaterItem h) sum += h.capacityBlocks();
        }
        return sum;
    }

    /**
     * All heating capacity that works now (PLAN 6.1): heaters in attached external filters (block next to the tank or
     * filter in a cabinet below), heater rods in the interior (only if their lowest segment is below the water surface)
     * and heaters still left in the old menu slots. 0 while the heaters lack power (M9, {@link TankPower}).
     */
    public static double heaterBlocks(ServerLevel level, TankData tank) {
        TankBlockEntity c = controllerOf(level, tank);
        if (c != null && !c.heatPowered) return 0;
        return rawHeaterBlocks(level, tank);
    }

    /** Heating capacity of all installed heaters, powered or not. */
    public static double rawHeaterBlocks(ServerLevel level, TankData tank) {
        return slotHeaterBlocks(tank) + interiorHeaterBlocks(level, tank) + filterHeaterBlocks(level, tank);
    }

    /** Cooling capacity that works now (M9): coolers in attached filters and cooler rods under water in the interior. */
    public static double coolerBlocks(ServerLevel level, TankData tank) {
        TankBlockEntity c = controllerOf(level, tank);
        if (c != null && !c.heatPowered) return 0;
        return rawCoolerBlocks(level, tank);
    }

    /** Cooling capacity of all installed coolers, powered or not. */
    public static double rawCoolerBlocks(ServerLevel level, TankData tank) {
        double sum = interiorCoolerBlocks(level, tank);
        TankBlockEntity c = controllerOf(level, tank);
        if (c != null) for (var f : TankEquipment.filters(level, c)) sum += f.coolerBlocks();
        return sum;
    }

    public static double interiorCoolerBlocks(ServerLevel level, TankData tank) {
        var r = org.overengineer.bettacraft.tank.interior.InteriorRegions.get(level.getServer()).get(tank.regionId());
        if (r == null || !r.built()) return 0;
        var eq = org.overengineer.bettacraft.tank.interior.InteriorManager.equipment(level.getServer(), r);
        if (eq.coolers().isEmpty()) return 0;
        return eq.coolerCapacityBelow(waterLocalHeight(tank, r.scale()));
    }

    /** Heating capacity of the heaters in the attached external filters. */
    public static double filterHeaterBlocks(ServerLevel level, TankData tank) {
        TankBlockEntity c = controllerOf(level, tank);
        if (c == null) return 0;
        double sum = 0;
        for (var f : TankEquipment.filters(level, c)) sum += f.heaterBlocks();
        return sum;
    }

    @Nullable
    private static TankBlockEntity controllerOf(ServerLevel level, TankData tank) {
        for (long l : tank.members()) {
            TankBlockEntity c = TankMultiblock.getController(level, BlockPos.of(l));
            return c != null && c.getTank() == tank ? c : null;
        }
        return null;
    }

    public static double interiorHeaterBlocks(ServerLevel level, TankData tank) {
        var r = org.overengineer.bettacraft.tank.interior.InteriorRegions.get(level.getServer()).get(tank.regionId());
        if (r == null || !r.built()) return 0;
        var eq = org.overengineer.bettacraft.tank.interior.InteriorManager.equipment(level.getServer(), r);
        if (eq.heaters().isEmpty()) return 0;
        return eq.heaterCapacityBelow(waterLocalHeight(tank, r.scale()));
    }

    /** Water surface height in interior blocks above the interior floor. */
    public static double waterLocalHeight(TankData tank, int scale) {
        return Math.max(0, tank.waterSurfaceY() - tank.min().getY()) * scale;
    }

    public static void updateTemperature(ServerLevel level, TankBlockEntity be, double dtDays) {
        TankData tank = be.getTank();
        float ambient = ambient(level, tank);
        // M31: without water no heater heats anything: the "water" temperature of a dry tank follows its air (a terrarium) or the room
        float eq = tank.waterMb() <= 0 ? (tank.hasClimate() ? tank.airTemp() : ambient)
                : TankTemperature.equilibrium(ambient, tank.targetTemp(), heaterBlocks(level, tank), coolerBlocks(level, tank), tank.waterBlocks());
        double tau = ServerConfig.getOr(ServerConfig.TEMPERATURE_TAU_DAYS, 0.1);
        float next = TankTemperature.approach(tank.waterTemp(), eq, dtDays, tau);
        if (Math.abs(next - tank.waterTemp()) > 1e-4) {
            tank.setWaterTemp(next);
            be.setChanged();
        }
    }

    // ------------------------------------------------------------------ geometry

    /**
     * Bounding box of the water (inside the glass, up to the surface). For free shapes (M10) it also covers the notches;
     * use {@link TankWaterVolume} to keep things inside the actual water.
     */
    public static AABB waterBox(TankData tank) {
        BlockPos mn = tank.min(), mx = tank.max();
        double wall = 1.0 / 16.0;
        double h = tank.waterSurfaceY() - mn.getY();
        return new AABB(mn.getX() + wall, mn.getY() + wall, mn.getZ() + wall,
                mx.getX() + 1 - wall, mn.getY() + Math.max(wall * 2, h), mx.getZ() + 1 - wall);
    }

    /**
     * Positions directly above every open top of the tank (M10: also the lower steps of a free shape; for a cuboid the
     * top layer). Lids, the feeder and lights sit there.
     */
    public static List<BlockPos> aboveTop(TankData tank) {
        return tank.openTops();
    }

    @Nullable
    public static FeederBlockEntity findFeeder(Level level, TankData tank) {
        for (BlockPos p : aboveTop(tank)) {
            if (level.isLoaded(p) && level.getBlockEntity(p) instanceof FeederBlockEntity f) return f;
        }
        // M21.5: a clip-on feeder on the side of a top block
        if (Features.enabled(Feature.CLIP_FEEDER)) {
            for (long l : tank.members()) {
                BlockPos p = BlockPos.of(l);
                if (tank.contains(p.above())) continue;
                for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                    BlockPos n = p.relative(d);
                    if (!level.isLoaded(n) || !(level.getBlockEntity(n) instanceof org.overengineer.bettacraft.equipment.ClipFeederBlockEntity f)) continue;
                    if (f.tankPos().equals(p)) return f;
                }
            }
        }
        return null;
    }

    /** Number of lights on the tank and how many of them are lit: {total, lit}. */
    public static int[] lights(Level level, TankData tank) {
        int total = 0, lit = 0;
        for (BlockPos p : aboveTop(tank)) {
            if (!level.isLoaded(p)) continue;
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof TankLightBlock) {
                total++;
                if (s.getValue(TankLightBlock.LIT)) lit++;
            }
        }
        return new int[]{total, lit};
    }

    // ------------------------------------------------------------------ feeding

    public static List<BettaEntity> bettas(ServerLevel level, TankData tank) {
        List<BettaEntity> out = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(level, tank)) if (e instanceof BettaEntity b) out.add(b);
        return out;
    }

    /** Bettas of the tank that eat this kind of food (fry food for fry, pellets/larvae from juvenile on). */
    public static List<BettaEntity> eaters(ServerLevel level, TankData tank, BettaFood.Kind kind) {
        List<BettaEntity> out = new ArrayList<>();
        for (BettaEntity b : bettas(level, tank)) if (BettaFood.canEat(b.lifeStage(), b.stageProgress(), kind)) out.add(b);
        return out;
    }

    /**
     * Feeds the hungriest bettas that eat this food with up to {@code maxItems} items (one item per
     * {@link BettaFood#fishPerItem} fish). Returns the number of items used (0 if nobody eats it).
     */
    public static int feed(ServerLevel level, TankBlockEntity controller, ItemStack food, int maxItems) {
        return feed(level, controller, food, maxItems, null);
    }

    /** As {@link #feed(ServerLevel, TankBlockEntity, ItemStack, int)}; skips and collects the fish fed in this {@code round}. */
    public static int feed(ServerLevel level, TankBlockEntity controller, ItemStack food, int maxItems, @Nullable java.util.Set<java.util.UUID> round) {
        // M49: fruit flies over a tank with an archerfish: let out above the water for it to shoot
        int released = org.overengineer.bettacraft.tankmates.Archerfish.release(level, controller, food, maxItems);
        if (released > 0) return released;
        BettaFood.Kind kind = BettaFood.kind(food);
        if (kind == null || maxItems <= 0) return 0;
        // M26 part b: shrimp food goes to the shrimp, crayfish and snails only (twice the food of a leftover)
        if (food.is(org.overengineer.bettacraft.registry.ModTags.CRITTER_FOOD)) {
            if (!org.overengineer.bettacraft.critters.TankCritters.wantFood(controller.getTank())) return 0;
            org.overengineer.bettacraft.critters.TankCritters.addFood(controller.getTank(),
                    BettaFood.FISH_PER_FOOD * ServerConfig.getOr(ServerConfig.DIET_SHRIMP_FOOD_FACTOR, 2.0));
            log(level, controller, "log.bettacraft.critters_fed", "@" + food.getDescriptionId());
            return 1;
        }
        // M19: algae wafers sink straight to the bottom fish (the bettas do not eat them)
        boolean wafer = org.overengineer.bettacraft.tankmates.TankmateFeeding.bottomFood(food);
        List<BettaEntity> fish = wafer ? new ArrayList<>() : eaters(level, controller.getTank(), kind);
        if (round != null) fish.removeIf(b -> round.contains(b.getUUID()));
        List<org.overengineer.bettacraft.entity.tankmate.TankmateEntity> mates =
                org.overengineer.bettacraft.tankmates.TankmateFeeding.eaters(level, controller.getTank(), kind, wafer);
        if (round != null) mates.removeIf(m -> round.contains(m.getUUID()));
        // M14: the frogs (slow eaters) and the shrimp, crayfish and snails (they eat what sinks to the floor)
        List<org.overengineer.bettacraft.entity.frog.DwarfFrogEntity> frogs = TankFrogs.eats(kind) && !wafer ? TankFrogs.frogs(level, controller.getTank())
                : new ArrayList<>();
        if (round != null) frogs.removeIf(f -> round.contains(f.getUUID()));
        boolean critters = org.overengineer.bettacraft.critters.TankCritters.wantFood(controller.getTank());
        // M47 part c: the tadpoles take fine food (infusoria, artemia nauplii, fry food)
        boolean tadpoles = !wafer && org.overengineer.bettacraft.entity.frog.FrogBreeding.wantFood(controller.getTank(), kind);
        // M48 part b: rotifers and artemia nauplii for the clownfish larvae (the water of a reef tank holds them)
        boolean larvae = org.overengineer.bettacraft.reef.ClownLarvae.wantFood(controller.getTank(), food);
        // M31: the terrarium animals (turtles ...) eat what is meant for them
        List<Entity> terra = wafer ? new ArrayList<>() : org.overengineer.bettacraft.terrarium.TerrariumFeeding.eaters(level, controller.getTank(), kind);
        if (round != null) terra.removeIf(e -> round.contains(e.getUUID()));
        // M32 part a: the animals of other mods with a profile
        List<net.minecraft.world.entity.LivingEntity> foreign = wafer ? new ArrayList<>() : org.overengineer.bettacraft.foreign.ForeignCare.eaters(level, controller.getTank(), food, kind);
        if (round != null) foreign.removeIf(e -> round.contains(e.getUUID()));
        if (fish.isEmpty() && mates.isEmpty() && frogs.isEmpty() && !critters && !tadpoles && !larvae && terra.isEmpty() && foreign.isEmpty()) return 0;
        fish.sort(Comparator.comparingDouble((BettaEntity b) -> b.health().daysSinceFed()).reversed());
        int per = BettaFood.fishPerItem(kind);
        int mouths = fish.size() + mates.size() + frogs.size() + terra.size() + foreign.size() + (critters ? 1 : 0) + (tadpoles ? 1 : 0) + (larvae ? 1 : 0);
        int items = Math.min(maxItems, (mouths + per - 1) / per);
        int fed = Math.min(fish.size(), items * per);
        int overfed = 0;
        for (int i = 0; i < fed; i++) {
            BettaEntity b = fish.get(i);
            if (b.health().daysSinceFed() < OVERFED_DAYS) overfed++;
            b.health().feed();
            org.overengineer.bettacraft.food.LiveFood.eaten(food, b.health(), b.breeding(), b.isAdult(),
                    org.overengineer.bettacraft.food.FoodDiet.Diet.BETTA); // M26: tubifex, poor cultures; part b: the diet
            b.syncDiet();
            if (round != null) round.add(b.getUUID());
        }
        if (fed > 0) log(level, controller, "log.bettacraft.fed", String.valueOf(fed), "@" + food.getDescriptionId());
        int left = items * per - fed;
        left -= org.overengineer.bettacraft.tankmates.TankmateFeeding.feed(level, controller, mates, food, kind, Math.min(left, mates.size()), !fish.isEmpty(), wafer, round);
        int frogsFed = TankFrogs.feed(level, controller, kind, Math.min(left, frogs.size()), !fish.isEmpty(), false);
        if (round != null) for (var f : frogs) if (f.daysSinceFed() <= 1e-6) round.add(f.getUUID());
        left -= frogsFed;
        if (tadpoles) { // M47 part c: one item's share for the tadpoles (at least one portion)
            int took = org.overengineer.bettacraft.entity.frog.FrogBreeding.feed(controller.getTank(), kind, Math.max(1, Math.min(Math.max(0, left), per)));
            if (took > 0) log(level, controller, "log.bettacraft.frog.tadpoles_fed", "@" + food.getDescriptionId());
            left -= took;
        }
        if (larvae) { // M48 part b: one portion of the item goes into the plankton of the water
            int took = org.overengineer.bettacraft.reef.ClownLarvae.feed(controller.getTank(), food, 1);
            if (took > 0) log(level, controller, "log.bettacraft.reef.plankton_fed", "@" + food.getDescriptionId());
            left -= took;
        }
        left -= org.overengineer.bettacraft.terrarium.TerrariumFeeding.feed(terra, Math.max(0, Math.min(left, terra.size())), round); // M31
        left -= org.overengineer.bettacraft.foreign.ForeignCare.feed(foreign, Math.max(0, Math.min(left, foreign.size())), round); // M32 part a
        // M39 part b: the clean-up crew of a terrarium (woodlice, snails) eats the leftovers and the food the bettas did not need
        if (left > 0) left = org.overengineer.bettacraft.foreign.ForeignHabits.eatLeftovers(level, controller, left);
        if (overfed > 0) overfed = org.overengineer.bettacraft.foreign.ForeignHabits.eatLeftovers(level, controller, overfed);
        // what is left sinks to the floor: the small animals eat it (or a little of what the bettas ate as crumbs)
        org.overengineer.bettacraft.critters.TankCritters.addFood(controller.getTank(), left > 0 ? left : fed * org.overengineer.bettacraft.critters.TankCritters.CRUMBS);
        TankWater.overfed(level, controller, overfed);
        return items;
    }

    /** M26: whether anybody in the tank eats this food now (bettas, tankmates, frogs or the small animals). */
    public static boolean anyoneEats(ServerLevel level, TankData tank, ItemStack food) {
        BettaFood.Kind kind = BettaFood.kind(food);
        if (kind == null) return false;
        if (food.is(org.overengineer.bettacraft.registry.ModTags.CRITTER_FOOD)) return org.overengineer.bettacraft.critters.TankCritters.wantFood(tank);
        boolean wafer = org.overengineer.bettacraft.tankmates.TankmateFeeding.bottomFood(food);
        if (!wafer && !eaters(level, tank, kind).isEmpty()) return true;
        if (!org.overengineer.bettacraft.tankmates.TankmateFeeding.eaters(level, tank, kind, wafer).isEmpty()) return true;
        if (TankFrogs.eats(kind) && !wafer && !TankFrogs.frogs(level, tank).isEmpty()) return true;
        if (!wafer && org.overengineer.bettacraft.entity.frog.FrogBreeding.wantFood(tank, kind)) return true; // M47 part c
        if (org.overengineer.bettacraft.reef.ClownLarvae.wantFood(tank, food)) return true; // M48 part b
        if (!wafer && !org.overengineer.bettacraft.terrarium.TerrariumFeeding.eaters(level, tank, kind).isEmpty()) return true; // M31
        if (!wafer && !org.overengineer.bettacraft.foreign.ForeignCare.eaters(level, tank, food, kind).isEmpty()) return true; // M32 part a
        return org.overengineer.bettacraft.critters.TankCritters.wantFood(tank);
    }

    /** Right-click on the tank with betta food: one item, food sinks down, bettas swim to it. */
    public static ItemInteractionResult handFeed(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand, Vec3 hit) {
        if (BettaFood.kind(stack) == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        ServerLevel server = (ServerLevel) level;
        TankBlockEntity controller = TankMultiblock.getController(level, pos);
        if (controller == null) return ItemInteractionResult.CONSUME;
        TankData tank = controller.getTank();
        boolean wafer = org.overengineer.bettacraft.tankmates.TankmateFeeding.bottomFood(stack);
        List<BettaEntity> fish = wafer ? new ArrayList<>() : eaters(server, tank, BettaFood.kind(stack));
        List<org.overengineer.bettacraft.entity.tankmate.TankmateEntity> mates =
                org.overengineer.bettacraft.tankmates.TankmateFeeding.eaters(server, tank, BettaFood.kind(stack), wafer);
        boolean others = (TankFrogs.eats(BettaFood.kind(stack)) && !wafer && !TankFrogs.frogs(server, tank).isEmpty())
                || !mates.isEmpty() || org.overengineer.bettacraft.critters.TankCritters.wantFood(tank)
                || !wafer && org.overengineer.bettacraft.entity.frog.FrogBreeding.wantFood(tank, BettaFood.kind(stack)) // M47 part c
                || org.overengineer.bettacraft.reef.ClownLarvae.wantFood(tank, stack) // M48 part b
                || !wafer && !org.overengineer.bettacraft.terrarium.TerrariumFeeding.eaters(server, tank, BettaFood.kind(stack)).isEmpty() // M31
                || !wafer && !org.overengineer.bettacraft.foreign.ForeignCare.eaters(server, tank, stack, BettaFood.kind(stack)).isEmpty(); // M32 part a
        if (fish.isEmpty() && !others) {
            player.displayClientMessage(Component.translatable(bettas(server, tank).isEmpty() ? "message.bettacraft.tank.no_bettas"
                    : "message.bettacraft.tank.nobody_eats", stack.getHoverName()), true);
            return ItemInteractionResult.CONSUME;
        }
        if (feed(server, controller, stack, 1) <= 0) return ItemInteractionResult.CONSUME;
        ItemStack remainder = org.overengineer.bettacraft.platform.Platform.get().craftingRemainder(stack);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            if (!remainder.isEmpty() && !player.getInventory().add(remainder)) player.drop(remainder, false);
        }
        // M10: the food lands in the water of the block that was clicked (never in the notch of a free shape)
        Vec3 spot = new TankWaterVolume(tank).surfacePoint(hit, 1.0 / 16, 0.05);
        server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack.copyWithCount(1)), spot.x, spot.y, spot.z, 10, 0.15, 0.02, 0.15, 0.02);
        server.sendParticles(ParticleTypes.BUBBLE, spot.x, spot.y - 0.3, spot.z, 6, 0.2, 0.2, 0.2, 0.01);
        level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.3F, 1.6F);
        long until = level.getGameTime() + 120;
        for (BettaEntity b : fish) b.setFoodTarget(spot, until);
        for (var m : mates) m.setFoodTarget(spot, until); // M19
        return ItemInteractionResult.CONSUME;
    }

    // ------------------------------------------------------------------ log

    /** Log argument for an entity: its custom name or "@" + translation key of its type. */
    public static String nameArg(Entity e) {
        return e.hasCustomName() ? e.getCustomName().getString() : "@" + e.getType().getDescriptionId();
    }

    public static void log(ServerLevel level, TankBlockEntity controller, String key, String... args) {
        if (org.overengineer.bettacraft.debug.LogArea.CARE.debug) org.overengineer.bettacraft.debug.LogArea.CARE.debug("tank {} log: {} {}", controller.getTank().id(), key, java.util.Arrays.toString(args)); // M57
        controller.getTank().addLog(level.getGameTime(), key, args);
        controller.setChanged();
        org.overengineer.bettacraft.progression.Progress.onTankLog(level, controller, key);
    }

    /** Turns a log argument back into a component (see {@link #nameArg}). */
    public static Component argComponent(String arg) {
        return arg.startsWith("@") ? Component.translatable(arg.substring(1)) : Component.literal(arg);
    }
}
