package org.overengineer.bettacraft.plants;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankWater;
import org.overengineer.bettacraft.tank.interior.InteriorFloorBlock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorPreparation;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tank.plants.TankPlants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aquarium plants on the server (M13): planting cuttings in the interior, growth in the care loop of the tank (on the
 * plant index only; the blocks of a new stage are written when it is due, the interior chunks are loaded briefly for
 * that, and the miniature gets the change as an {@code InteriorSync} delta like any other block change), trimming
 * (shears inside on a plant block, or from outside on the tank: the view ray into the miniature), dividing and taking
 * plants out, side shoots and runners, and what the plants do for the tank (waste uptake, hiding spots after spawning,
 * a small breeding bonus, shade at the surface).
 */
public final class InteriorPlants {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/plants");
    /** Plant volume (tank blocks of plant mass per water block) from which a tank counts as well planted. */
    public static final double BONUS_FROM = 0.03;
    /** Plant volume at which the hiding spots are as good as they get (server config interiorPlants.hiding). */
    public static final double HIDING_FULL_AT = 0.08;
    /** How far (interior blocks) a new plant must stay from the root of another one. */
    public static final int MIN_ROOT_DISTANCE = 2;
    /** Keeps the miniature 0.5 px inside the glass, like {@code MiniatureRenderer}. */
    public static final double MARGIN = 0.5 / 16;
    /** Reach of shears aimed from outside into the miniature (world blocks). */
    public static final double OUTSIDE_REACH = 8;

    private InteriorPlants() {}

    // ------------------------------------------------------------------ blocks and items

    public static AquaticPlantBlock block(PlantSpecies s) {
        return ModBlocks.AQUATIC_PLANTS.get(s).get();
    }

    public static BlockState state(PlantSpecies s, PlantPart part, int tint) {
        return block(s).defaultBlockState().setValue(AquaticPlantBlock.PART, part)
                .setValue(AquaticPlantBlock.TINT, s.tinted() ? Math.max(0, Math.min(PlantColors.MAX_TINT, tint)) : 0);
    }

    @Nullable
    public static PlantSpecies speciesOf(BlockState s) {
        return s.getBlock() instanceof AquaticPlantBlock b ? b.species() : null;
    }

    /** The item of a species: a cutting (stage 0) or a plant taken out at its stage. */
    public static ItemStack item(PlantSpecies s, int stage) {
        var item = ModItems.AQUATIC_PLANTS.get(s);
        if (item == null) return ItemStack.EMPTY;
        ItemStack st = new ItemStack(item.get());
        if (stage > 0) st.set(ModDataComponents.PLANT_STAGE.get(), Math.min(stage, s.stages()));
        return st;
    }

    // ------------------------------------------------------------------ config

    public static double growthSpeed() {
        return ServerConfig.getOr(ServerConfig.AQUATIC_GROWTH_SPEED, 1.0);
    }

    static int maxPerTank() {
        return ServerConfig.getOr(ServerConfig.AQUATIC_MAX_PER_TANK, 48);
    }

    // ------------------------------------------------------------------ regions

    @Nullable
    public static InteriorRegion region(MinecraftServer server, TankData t) {
        InteriorRegion r = InteriorRegions.get(server).get(t.regionId());
        return r != null && r.built() ? r : null;
    }

    /** The tank of a region, if its controller is loaded. */
    @Nullable
    public static TankBlockEntity controller(MinecraftServer server, InteriorRegion r) {
        ServerLevel tankLevel = server.getLevel(r.tankDimension());
        if (tankLevel == null || !tankLevel.isLoaded(r.tankMin())) return null;
        TankBlockEntity c = TankMultiblock.getController(tankLevel, r.tankMin());
        return c != null && c.getTank().regionId() == r.id() ? c : null;
    }

    /** Water surface in interior blocks above the interior floor (the whole height without a tank). */
    static double waterLocal(MinecraftServer server, InteriorRegion r) {
        TankBlockEntity c = controller(server, r);
        return c == null ? r.sizeY() : TankCare.waterLocalHeight(c.getTank(), r.scale());
    }

    /** Room up to the water surface for a plant (fixed once it begins to grow). */
    static int room(double waterLocal, InteriorRegion r, InteriorPlant p) {
        int top = (int) Math.floor(Math.min(r.sizeY(), waterLocal <= 0 ? r.sizeY() : waterLocal));
        return Math.max(2, top - p.y);
    }

    public static PlantShapes.Shape shape(InteriorRegion r, InteriorPlant p) {
        return PlantShapes.of(p.species, p.seed, r.scale(), Math.max(2, p.room == 0 ? r.sizeY() - p.y : p.room), p.shapeVersion);
    }

    // ------------------------------------------------------------------ planting

    /** Why a species cannot grow on {@code support} (clicked on {@code face}), or null if it can. */
    @Nullable
    public static String supportProblem(PlantSpecies s, ServerLevel level, BlockPos supportPos, BlockState support, Direction face) {
        if (s.marine()) {
            // M20: corals are glued onto reef rock (on top or at the side; polyp mats also hang from overhangs)
            if (!support.is(ModTags.REEF_SURFACES) || face == Direction.DOWN && s.form() != PlantSpecies.Form.POLYPS) return "needs_reef_rock";
            return null;
        }
        switch (s.form()) {
            case EPIPHYTE -> {
                if (face == Direction.DOWN || !support.is(ModTags.EPIPHYTE_SURFACES)) return "needs_hardscape";
            }
            case MOSS -> {
                // M28: Riccia is tied onto stone or wood
                if (s.onlyOnHardscape() && (face == Direction.DOWN || !support.is(ModTags.EPIPHYTE_SURFACES))) return "needs_hardscape";
                if (support.isAir() || support.getBlock() instanceof AquaticPlantBlock || !support.isFaceSturdy(level, supportPos, face)) return "needs_solid";
            }
            default -> {
                boolean floor = Substrate.isFloor(support) && support.getValue(InteriorFloorBlock.SUBSTRATE);
                // M28: Hygrophila pinnatifida also roots on stone and wood
                boolean hardscape = s.alsoOnHardscape() && support.is(ModTags.EPIPHYTE_SURFACES);
                if (face != Direction.UP || !(floor || hardscape || support.is(ModTags.PLANT_SUBSTRATE))) return "needs_substrate";
            }
        }
        return null;
    }

    /** A player uses a plant item in a tank interior. Returns true when it was planted (the item is used up). */
    public static boolean plant(ServerLevel level, UseOnContext ctx, PlantSpecies s, int stage) {
        Player player = ctx.getPlayer();
        if (!InteriorManager.isInterior(level)) {
            message(player, "only_interior", ChatFormatting.GOLD);
            return false;
        }
        BlockPos clicked = ctx.getClickedPos();
        Direction face = ctx.getClickedFace();
        BlockState cs = level.getBlockState(clicked);
        BlockPos pos = clicked.relative(face);
        if (cs.canBeReplaced() && !(cs.getBlock() instanceof AquaticPlantBlock)) {
            pos = clicked;
            face = Direction.UP;
        }
        BlockPos supportPos = pos.relative(face.getOpposite());
        InteriorRegion r = InteriorManager.regionAt(level.getServer(), pos);
        if (r == null || !r.built() || !r.containsShapeInterior(pos)) {
            message(player, "only_interior", ChatFormatting.GOLD);
            return false;
        }
        if (player != null && !InteriorManager.canBuild(player, pos)) {
            if (player != null) player.displayClientMessage(Component.translatable("message.bettacraft.tank.locked").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!level.getBlockState(pos).canBeReplaced()) {
            message(player, "occupied", ChatFormatting.GOLD);
            return false;
        }
        String problem = supportProblem(s, level, supportPos, level.getBlockState(supportPos), face);
        if (problem != null) {
            message(player, problem, ChatFormatting.GOLD);
            return false;
        }
        if (r.plants().size() >= maxPerTank()) {
            message(player, "full", ChatFormatting.GOLD);
            return false;
        }
        int lx = r.localX(pos.getX()), ly = r.localY(pos.getY()), lz = r.localZ(pos.getZ());
        if (tooClose(r, lx, ly, lz, null)) {
            message(player, "too_close", ChatFormatting.GOLD);
            return false;
        }
        InteriorPlant p = r.plants().add(s, lx, ly, lz, level.getRandom().nextLong(), 0);
        level.setBlock(pos, state(s, PlantPart.CUTTING, 0), Block.UPDATE_ALL);
        if (stage > 0) {
            p.room = room(waterLocal(level.getServer(), r), r, p);
            p.target = Math.min(stage, s.stages());
            TankBlockEntity c = controller(level.getServer(), r);
            if (c != null && s.tinted()) {
                p.redness = PlantColors.tintFor((int) Math.round(TankPlants.strongLight(c.getLevel() instanceof ServerLevel sl ? sl : level, c))) / (double) PlantColors.MAX_TINT;
                p.tint = (int) Math.round(p.redness * PlantColors.MAX_TINT);
            }
            apply(level, r, p);
        }
        InteriorRegions.get(level.getServer()).setDirty();
        level.playSound(null, pos, SoundEvents.BIG_DRIPLEAF_PLACE, SoundSource.BLOCKS, 0.8F, 1.3F);
        if (player != null && !player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        TankBlockEntity c = controller(level.getServer(), r);
        if (c != null && c.getLevel() instanceof ServerLevel tl && player != null) {
            TankCare.log(tl, c, "log.bettacraft.aquatic_planted", TankCare.nameArg(player), "@" + item(s, 0).getDescriptionId());
        }
        return true;
    }

    /** Whether another plant has its root within {@link #MIN_ROOT_DISTANCE} (Chebyshev, same layer +- 1). */
    static boolean tooClose(InteriorRegion r, int x, int y, int z, @Nullable InteriorPlant except) {
        for (InteriorPlant o : r.plants().all()) {
            if (o == except) continue;
            if (Math.abs(o.y - y) <= 1 && Math.max(Math.abs(o.x - x), Math.abs(o.z - z)) < MIN_ROOT_DISTANCE) return true;
        }
        return false;
    }

    private static void message(@Nullable Player player, String key, ChatFormatting color) {
        if (player != null) player.displayClientMessage(Component.translatable("message.bettacraft.aquatic." + key).withStyle(color), true);
    }

    // ------------------------------------------------------------------ growth (care loop)

    /** Conditions in a tank for its aquarium plants. */
    /**
     * @param salinity M20: ppt of the water; {@code flow}, {@code calcium}, {@code spectrum} (share of the light the
     *                 corals use), {@code quality} (water quality) and {@code glow} (actinic light) matter for the corals
     */
    public record Env(int light, int effectiveLight, double nutrients, double temperature, double waterLocal, double shade,
                      double salinity, double flow, double calcium, double spectrum, double quality, boolean glow, double coralTemp,
                      @Nullable java.util.function.ToDoubleFunction<InteriorPlant> localFlow, double reefChem, boolean trace) {
        /** M25 part c: {@code reefChem} = alkalinity x magnesium x pH factor of the stony corals, {@code trace}: trace elements give stronger colours. */
        public Env(int light, int effectiveLight, double nutrients, double temperature, double waterLocal, double shade,
                   double salinity, double flow, double calcium, double spectrum, double quality, boolean glow, double coralTemp,
                   @Nullable java.util.function.ToDoubleFunction<InteriorPlant> localFlow) {
            this(light, effectiveLight, nutrients, temperature, waterLocal, shade, salinity, flow, calcium, spectrum, quality, glow, coralTemp, localFlow, 1, false);
        }

        public Env(int light, int effectiveLight, double nutrients, double temperature, double waterLocal, double shade) {
            this(light, effectiveLight, nutrients, temperature, waterLocal, shade, 0, 0, 0, 0.6, 0.9, false, 0);
        }

        public Env(int light, int effectiveLight, double nutrients, double temperature, double waterLocal, double shade,
                   double salinity, double flow, double calcium, double spectrum, double quality, boolean glow, double coralTemp) {
            this(light, effectiveLight, nutrients, temperature, waterLocal, shade, salinity, flow, calcium, spectrum, quality, glow, coralTemp, null);
        }

        /** M22.4: the conditions where this plant grows: the flow is stronger in front of a filter outlet ({@code TankCurrents}). */
        public Env at(InteriorPlant p) {
            if (localFlow == null || flow <= 0) return this;
            double f = localFlow.applyAsDouble(p);
            if (Math.abs(f - 1) < 1e-9) return this;
            return new Env(light, effectiveLight, nutrients, temperature, waterLocal, shade, salinity, flow * f, calcium, spectrum, quality, glow, coralTemp, null,
                    reefChem, trace);
        }

        /** Growth factor of a plant (0..1): light for its need, nutrients, temperature, under water. */
        public double rate(InteriorPlant p) {
            if (p.y + 0.5 >= waterLocal) return 0; // dry: no growth
            if (p.species.marine()) return org.overengineer.bettacraft.reef.Corals.rate(at(p), p.species, effectiveLight);
            // M20: freshwater plants stop growing in brackish and salt water
            // M28: the Marimo does not mind cool water
            double temp = p.species.rolls() ? Math.max(0.8, temperature) : temperature;
            return lightFactor(effectiveLight, PlantSpecies.Light.need(p.species.light())) * nutrients * temp * org.overengineer.bettacraft.reef.Corals.freshFactor(salinity);
        }
    }

    /**
     * M28: growth factor of the special needs of a species (1 for most): a {@link PlantSpecies#needsCo2()} plant grows at
     * {@link PlantSpecies#NO_CO2_GROWTH} without CO2 and at full speed from a good CO2 level (with {@code features.co2.enabled}); a
     * {@link PlantSpecies#softWater()} plant slows down in hard or alkaline water (with {@code features.waterChemistry.enabled}):
     * full up to pH 7.0 and 300 TDS, half from pH 8.0 or 700 TDS.
     */
    public static double needsFactor(TankData t, PlantSpecies s) {
        double f = 1;
        if (s.needsCo2() && org.overengineer.bettacraft.co2.TankCo2.enabled()) {
            f *= PlantSpecies.NO_CO2_GROWTH + (1 - PlantSpecies.NO_CO2_GROWTH) * org.overengineer.bettacraft.chem.WaterChemistry.co2Boost(t.co2());
        }
        if (s.softWater() && Features.enabled(Feature.WATER_CHEMISTRY)) f *= softWaterFactor(t.ph(), t.tds());
        return f;
    }

    /** M28: growth of a soft water plant at this pH and TDS (1 .. 0.5 each, multiplied, at least 0.25). */
    public static double softWaterFactor(double ph, double tds) {
        double a = 1 - 0.5 * Math.max(0, Math.min(1, ph - 7.0));
        double b = 1 - 0.5 * Math.max(0, Math.min(1, (tds - 300) / 400));
        return Math.max(0.25, a * b);
    }

    /** Light factor for a light need: 0 below {@code from}, 1 from {@code full}. */
    public static double lightFactor(int light, PlantSpecies.Light need) {
        if (light < need.from) return 0;
        return Math.min(1.0, (light - need.from + 1) / (double) (need.full - need.from + 1));
    }

    /** Nutrients from the waste in the water: half speed in clean water, full from 25 % waste. */
    public static double nutrientFactor(float quality) {
        double waste = 1 - quality;
        return 0.5 + 0.5 * Math.min(1.0, Math.max(0, waste) / 0.25);
    }

    /** Temperature factor: 20-30 °C full, 18-20 and 30-32 half, else none. */
    public static double temperatureFactor(float celsius) {
        if (celsius >= 20 && celsius <= 30) return 1.0;
        if (celsius >= 18 && celsius <= 32) return 0.5;
        return 0;
    }

    public static Env env(ServerLevel level, TankBlockEntity c, InteriorRegion r) {
        TankData t = c.getTank();
        int light = (int) Math.round(TankPlants.strongLight(level, c)); // M23: up to 20 under strong LEDs
        double shade = TankPlants.coverage(t) + surfaceCover(t, r);
        boolean dense = shade >= ServerConfig.getOr(ServerConfig.PLANTS_DENSE_FROM, 0.5);
        float q = TankWater.qualityEnabled() ? t.waterQuality() : 0.9F;
        // M20: the reef values only matter in salt water (no lookups for a freshwater tank)
        boolean salt = t.salinity() > 0 || !r.plants().isEmpty() && r.plants().all().stream().anyMatch(p -> p.species.marine());
        double flow = salt ? org.overengineer.bettacraft.reef.TankReef.flow(level, c) : 0;
        double spectrum = salt ? org.overengineer.bettacraft.reef.TankReef.spectrum(level, t) : 0.6;
        boolean glow = salt && org.overengineer.bettacraft.reef.TankReef.glows(level, t);
        // M22.4: the corals in front of a filter outlet get more flow
        java.util.function.ToDoubleFunction<InteriorPlant> local = null;
        if (flow > 0) {
            var currents = org.overengineer.bettacraft.tank.TankCurrents.cached(level, c);
            if (!currents.isEmpty()) {
                local = p -> {
                    var w = org.overengineer.bettacraft.tankmates.TankmateSpots.toWorld(t, r, p.x + 0.5, p.y + 0.5, p.z + 0.5);
                    return org.overengineer.bettacraft.tank.TankCurrents.factor(currents, w.x, w.z);
                };
            }
        }
        return new Env(light, Math.max(0, light - (dense ? 3 : 0)), nutrientFactor(q), temperatureFactor(t.waterTemp()),
                t.waterMb() <= 0 ? 0 : TankCare.waterLocalHeight(t, r.scale()), shade, t.salinity(), flow, t.calcium(), spectrum, q, glow,
                org.overengineer.bettacraft.reef.Corals.temperatureFactor(t.waterTemp()), local,
                salt ? org.overengineer.bettacraft.reef.ReefDosing.stonyFactor(t) : 1, salt && org.overengineer.bettacraft.reef.ReefDosing.traceBonus(t)); // M25 part c
    }

    /** Share of the water surface (interior blocks) covered by plant blocks at the surface (stem tops, lotus pads). */
    public static double surfaceCover(TankData t, InteriorRegion r) {
        int s = r.plants().surfaceBlocks();
        if (s <= 0) return 0;
        int area = TankPlants.surface(t).area() * r.scale() * r.scale();
        return area <= 0 ? 0 : s / (double) area;
    }

    /** One care step of the tank ({@code days} since the last one): growth on the index, blocks of due stages. */
    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        TankData t = c.getTank();
        if (!org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.INTERIOR)) return; // M33 part b: the interior rests
        InteriorRegion r = region(level.getServer(), t);
        if (r == null || r.plants().isEmpty() || days <= 0) return;
        grow(level, c, r, days);
    }

    /** M25 part d: growth factor of the freshwater plants from the active soil on the floor (1 .. {@link #ACTIVE_SOIL_BOOST}). */
    public static final double ACTIVE_SOIL_BOOST = 1.25;

    public static double activeSoilBoost(ServerLevel level, InteriorRegion r) {
        if (!r.built()) return 1.0;
        var eq = org.overengineer.bettacraft.tank.interior.InteriorManager.equipment(level.getServer(), r);
        if (eq == null || eq.floorBlocks() <= 0) return 1.0;
        return 1.0 + (ACTIVE_SOIL_BOOST - 1.0) * Math.min(1.0, eq.activeSoil() / (double) eq.floorBlocks());
    }

    /** Growth for {@code days} (also used by GameTests with long steps). Returns the number of plants whose blocks changed. */
    public static int grow(ServerLevel level, TankBlockEntity c, InteriorRegion r, double days) {
        TankData t = c.getTank();
        Env env = env(level, c, r);
        double speed = growthSpeed();
        boolean spread = ServerConfig.getOr(ServerConfig.AQUATIC_SPREAD, true);
        List<InteriorPlant> due = new ArrayList<>();
        List<InteriorPlant> shoots = new ArrayList<>();
        double activeMass = 0, fedMass = 0;
        int redTarget = PlantColors.tintFor(env.light());
        double soilBoost = activeSoilBoost(level, r);
        double fertilizer = org.overengineer.bettacraft.fertilizer.Fertilizer.growth(t); // M37: liquid fertilizer speeds up our freshwater plants
        for (InteriorPlant p : r.plants().all()) {
            // M33 part b: aquarium plants / corals off: they rest (no growth, spreading, uptake or colour change)
            if (!org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.AQUATIC_PLANTS) && !p.species.marine() || !org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.CORALS) && p.species.marine()) continue;
            double rate = speed * env.rate(p);
            // M25: CO2 makes the freshwater plants grow faster, the strong-light plants most
            if (rate > 0 && !p.species.marine()) rate *= org.overengineer.bettacraft.co2.TankCo2.growth(t, PlantSpecies.Light.need(p.species.light()));
            // M25 part d: active soil feeds the freshwater plants (up to +25 % on a fully covered floor)
            if (rate > 0 && !p.species.marine()) rate *= soilBoost;
            // M37: liquid fertilizer (up to +50 % at 100 %; the plants use it up)
            if (rate > 0 && !p.species.marine()) {
                rate *= fertilizer;
                fedMass += p.blocks;
            }
            // M28: the demanding species: CO2 and soft water
            if (rate > 0) rate *= needsFactor(t, p.species);
            if (rate > 0 && p.room == 0) p.room = room(env.waterLocal(), r, p);
            if (rate > 0) {
                activeMass += p.blocks;
                double dps = p.species.daysPerStage();
                if (p.target < p.species.stages()) {
                    p.progress += days * rate / dps;
                    while (p.progress >= 1 && p.target < p.species.stages()) {
                        p.target++;
                        p.progress -= 1;
                        // M20: a stony coral builds its skeleton from the calcium of the water
                        if (p.species.stony()) org.overengineer.bettacraft.reef.Corals.useCalcium(t); // M25 part c: and alkalinity and magnesium
                        if (p.target == p.species.stages()) {
                            p.progress = 0;
                            TankCare.log(level, c, "log.bettacraft.aquatic_grown", "@" + item(p.species, 0).getDescriptionId());
                        }
                    }
                } else if (spread && p.species.spreads()) {
                    double every = p.species.form() == PlantSpecies.Form.ROSETTE ? 3.0 : p.species.form() == PlantSpecies.Form.STEM ? 1.5 : 1.0;
                    p.progress += days * rate / (dps * every);
                    if (p.progress >= 1) {
                        p.progress -= 1;
                        shoots.add(p);
                    }
                }
            }
            if (p.species.tinted()) {
                // smoothed over about two days, with hysteresis: the tint level only moves when clearly off
                // M20: corals: bleached / pale / healthy / glowing from their conditions
                double want = (p.species.marine() ? org.overengineer.bettacraft.reef.Corals.tint(env.at(p), p.species) : redTarget) / (double) PlantColors.MAX_TINT;
                p.redness += (want - p.redness) * Math.min(1.0, days / 2.0);
                int tint = p.tint;
                if (Math.abs(p.redness * PlantColors.MAX_TINT - tint) > 0.6) tint = (int) Math.round(p.redness * PlantColors.MAX_TINT);
                if (tint != p.tint) {
                    p.tint = tint;
                    if (!due.contains(p)) due.add(p);
                }
            }
            if (p.target != p.stage && !due.contains(p)) due.add(p);
        }
        uptake(level, c, r, env, activeMass, days);
        org.overengineer.bettacraft.fertilizer.Fertilizer.used(t, fedMass / Math.pow(r.scale(), 3), days); // M37
        int rolled = rollAll(level, c, r, days);
        if (due.isEmpty() && shoots.isEmpty()) return rolled;
        ServerLevel interior = InteriorManager.interiorLevel(level.getServer());
        if (interior == null) return rolled;
        long t0 = System.nanoTime();
        int changed = rolled;
        for (InteriorPlant p : due) {
            if (!r.plants().all().contains(p)) continue;
            if (apply(interior, r, p)) changed++;
        }
        for (InteriorPlant p : shoots) {
            InteriorPlant n = offshoot(interior, r, p);
            if (n != null) {
                changed++;
                TankCare.log(level, c, n.species.form() == PlantSpecies.Form.STEM ? "log.bettacraft.aquatic_side_shoot" : "log.bettacraft.aquatic_runner",
                        "@" + item(n.species, 0).getDescriptionId());
            }
        }
        InteriorRegions.get(level.getServer()).setDirty();
        LOGGER.debug("Aquarium plants of region {}: {} plant(s) changed in {} ms", r.id(), changed, (System.nanoTime() - t0) / 1e6);
        return changed;
    }

    // ------------------------------------------------------------------ M28: rolling Marimo balls

    /** M28: rolls per in-game day of a Marimo in the current of an outlet (more where it is stronger). */
    public static final double MARIMO_ROLLS_IN_CURRENT = 4.0;
    /** M28: rolls per in-game day of a Marimo elsewhere in a tank with a running filter (the water moves a little). */
    public static final double MARIMO_ROLLS_FILTERED = 0.3;

    /**
     * M28: the Marimo balls of a tank roll over the floor ({@code features.aquaticPlants.marimoRolling}): in the current of a filter or
     * pipe outlet ({@code TankCurrents}) along it, now and then; elsewhere, with a running filter, rarely and to a random
     * side; in still water never. Returns the number of balls that moved.
     */
    static int rollAll(ServerLevel level, TankBlockEntity c, InteriorRegion r, double days) {
        if (!Features.enabled(Feature.MARIMO_ROLLING) || days <= 0 || c.getTank().waterMb() <= 0) return 0;
        List<InteriorPlant> balls = null;
        for (InteriorPlant p : r.plants().all()) {
            if (p.species.rolls() && p.stage > 0) {
                if (balls == null) balls = new ArrayList<>();
                balls.add(p);
            }
        }
        if (balls == null) return 0;
        ServerLevel interior = InteriorManager.interiorLevel(level.getServer());
        if (interior == null) return 0;
        var currents = org.overengineer.bettacraft.tank.TankCurrents.cached(level, c);
        // moving water: an outlet current, or a running filter on the tank
        boolean filtered = !currents.isEmpty() || c.filterPowered() && !org.overengineer.bettacraft.tank.TankEquipment.filters(level, c).isEmpty();
        var rnd = interior.getRandom();
        int moved = 0;
        for (InteriorPlant p : balls) {
            var w = org.overengineer.bettacraft.tankmates.TankmateSpots.toWorld(c.getTank(), r, p.x + 0.5, p.y + 0.5, p.z + 0.5);
            double[] along = currentAt(currents, w.x, w.z);
            double perDay = along != null ? MARIMO_ROLLS_IN_CURRENT * along[2] : filtered ? MARIMO_ROLLS_FILTERED : 0;
            if (perDay <= 0 || rnd.nextDouble() >= Math.min(1.0, perDay * days)) continue;
            int dx, dz;
            if (along != null && rnd.nextInt(4) != 0) {
                // mostly along the current (the larger of its two directions), sometimes a little to the side
                if (Math.abs(along[0]) >= Math.abs(along[1])) {
                    dx = (int) Math.signum(along[0]);
                    dz = 0;
                } else {
                    dx = 0;
                    dz = (int) Math.signum(along[1]);
                }
            } else {
                int[] d = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}[rnd.nextInt(4)];
                dx = d[0];
                dz = d[1];
            }
            if (roll(interior, r, p, dx, dz)) moved++;
        }
        return moved;
    }

    /** The direction (x, z) and strength (0..1) of the strongest outlet current at a world position, or null outside every current. */
    @Nullable
    static double[] currentAt(List<org.overengineer.bettacraft.tank.TankCurrents.Current> currents, double x, double z) {
        double[] best = null;
        for (var cur : currents) {
            double f = org.overengineer.bettacraft.tank.TankCurrents.factor(List.of(cur), x, z) - 1;
            if (f <= 1e-6) continue;
            double s = Math.min(1.0, f / org.overengineer.bettacraft.tank.TankCurrents.JET_BOOST);
            if (best == null || s > best[2]) best = new double[]{cur.dx(), cur.dz(), s};
        }
        return best;
    }

    /**
     * M28: rolls a Marimo one block to the side when there is room: the root block next to it free with something solid
     * below (and no other plant's root too close); the ball keeps its stage and shape. Returns whether it moved.
     */
    public static boolean roll(ServerLevel interior, InteriorRegion r, InteriorPlant p, int dx, int dz) {
        int nx = p.x + dx, nz = p.z + dz;
        BlockPos to = r.toWorld(nx, p.y, nz);
        if (!r.containsShapeInterior(to) || tooClose(r, nx, p.y, nz, p)) return false;
        interior.getChunk(to.getX() >> 4, to.getZ() >> 4);
        BlockState here = interior.getBlockState(to);
        // the ball's own blocks may stand there (it rolls into itself)
        if (!here.isAir() && !(here.is(block(p.species)) && ownerOf(r, to, p.species) == p)) return false;
        BlockPos below = to.below();
        if (supportProblem(p.species, interior, below, interior.getBlockState(below), Direction.UP) != null) return false;
        int stage = p.stage, target = p.target;
        remove(interior, r, p, false);
        p.x = nx;
        p.z = nz;
        r.plants().all().add(p);
        p.stage = 0;
        p.target = target;
        interior.setBlock(to, state(p.species, PlantPart.CUTTING, p.tint), Block.UPDATE_CLIENTS);
        if (!apply(interior, r, p)) return false;
        LOGGER.debug("Marimo {} rolled from stage {} by {},{}", p, stage, dx, dz);
        return true;
    }

    /** Waste uptake by the plant mass while there is light (server config interiorPlants.wasteUptake). */

    static void uptake(ServerLevel level, TankBlockEntity c, InteriorRegion r, Env env, double activeBlocks, double days) {
        TankData t = c.getTank();
        double k = ServerConfig.getOr(ServerConfig.AQUATIC_WASTE_UPTAKE, 0.5);
        if (!TankWater.qualityEnabled() || k <= 0 || activeBlocks <= 0 || t.waterQuality() >= 1.0F || t.waterMb() <= 0) return;
        double mass = activeBlocks / Math.pow(r.scale(), 3);
        double dq = k * mass * Math.min(1.0, env.effectiveLight() / 12.0) * days / Math.max(1.0, t.waterBlocks());
        t.setWaterQuality((float) Math.min(1.0, t.waterQuality() + dq));
        c.setChanged();
    }

    /** Water quality per day the plants of a tank take up now (menu). */
    public static double uptakePerDay(ServerLevel level, TankBlockEntity c, InteriorRegion r) {
        Env env = env(level, c, r);
        double active = 0;
        for (InteriorPlant p : r.plants().all()) if (env.rate(p) > 0) active += p.blocks;
        double k = ServerConfig.getOr(ServerConfig.AQUATIC_WASTE_UPTAKE, 0.5);
        if (!TankWater.qualityEnabled() || k <= 0) return 0;
        return k * active / Math.pow(r.scale(), 3) * Math.min(1.0, env.effectiveLight() / 12.0) / Math.max(1.0, c.getTank().waterBlocks());
    }

    // ------------------------------------------------------------------ writing the blocks

    /** Loads the interior chunks a plant reaches (briefly, with the interior ticket) so its blocks can be written. */
    static void loadChunks(ServerLevel interior, InteriorRegion r, InteriorPlant p, PlantShapes.Shape shape) {
        int minX = 0, maxX = 0, minZ = 0, maxZ = 0;
        for (PlantShapes.Voxel v : shape.all()) {
            minX = Math.min(minX, v.x());
            maxX = Math.max(maxX, v.x());
            minZ = Math.min(minZ, v.z());
            maxZ = Math.max(maxZ, v.z());
        }
        int wx = r.originX() + p.x, wz = r.originZ() + p.z;
        for (int cx = (wx + minX) >> 4; cx <= (wx + maxX) >> 4; cx++)
            for (int cz = (wz + minZ) >> 4; cz <= (wz + maxZ) >> 4; cz++) {
                ChunkPos cp = new ChunkPos(cx, cz);
                if (interior.getChunkSource().getChunkNow(cx, cz) == null) {
                    interior.getChunkSource().addRegionTicket(InteriorPreparation.TICKET, cp, 0, cp);
                    interior.getChunk(cx, cz); // loads it now (generated with the shell already)
                }
            }
    }

    /**
     * Brings the blocks of a plant to its {@link InteriorPlant#target} stage and {@link InteriorPlant#tint}: blocks of
     * higher stages go, the blocks of the stage grow out from the root through free space (around decoration and other
     * plants; parts that cannot be reached stay away). Returns false (and forgets the plant) when its root is gone.
     */
    public static boolean apply(ServerLevel interior, InteriorRegion r, InteriorPlant p) {
        PlantShapes.Shape shape = shape(r, p);
        loadChunks(interior, r, p, shape);
        BlockPos root = r.toWorld(p.x, p.y, p.z);
        AquaticPlantBlock block = block(p.species);
        if (!interior.getBlockState(root).is(block)) {
            LOGGER.debug("Plant {} lost its root in region {}", p, r.id());
            r.plants().remove(p);
            return false;
        }
        int to = p.target;
        Long2ObjectOpenHashMap<PlantPart> want = new Long2ObjectOpenHashMap<>();
        if (to > 0) for (PlantShapes.Voxel v : shape.at(to)) want.put(root.offset(v.x(), v.y(), v.z()).asLong(), v.part());
        // what goes: blocks of the stages above the target
        if (p.stage > to) {
            for (PlantShapes.Voxel v : shape.at(p.stage)) {
                BlockPos at = root.offset(v.x(), v.y(), v.z());
                if (want.containsKey(at.asLong()) || at.equals(root)) continue;
                if (interior.getBlockState(at).is(block) && !claimedByOther(r, p, at)) interior.setBlock(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        // the root, then everything reachable from it
        BlockState rootState = state(p.species, to == 0 ? PlantPart.CUTTING : PlantPart.ROOT, p.tint);
        if (interior.getBlockState(root) != rootState) interior.setBlock(root, rootState, Block.UPDATE_CLIENTS);
        int count = 1, surface = 0;
        if (to > 0) {
            LongOpenHashSet seen = new LongOpenHashSet();
            LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
            seen.add(root.asLong());
            queue.enqueue(root.asLong());
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            int topY = root.getY() + p.room - 1;
            while (!queue.isEmpty()) {
                long cur = queue.dequeueLong();
                int x = BlockPos.getX(cur), y = BlockPos.getY(cur), z = BlockPos.getZ(cur);
                for (int dx = -1; dx <= 1; dx++)
                    for (int dy = -1; dy <= 1; dy++)
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) continue;
                            m.set(x + dx, y + dy, z + dz);
                            long k = m.asLong();
                            PlantPart part = want.get(k);
                            if (part == null || !seen.add(k)) continue;
                            if (!r.containsShapeInterior(m)) continue;
                            BlockState now = interior.getBlockState(m);
                            boolean ours = now.is(block) && now.getValue(AquaticPlantBlock.PART) != PlantPart.ROOT
                                    && now.getValue(AquaticPlantBlock.PART) != PlantPart.CUTTING;
                            if (!now.isAir() && !ours) continue; // decoration, another species, another root
                            BlockState st = state(p.species, part, p.tint);
                            if (now != st) interior.setBlock(m, st, Block.UPDATE_CLIENTS);
                            count++;
                            if (part.surface() || m.getY() >= topY && p.species.form() == PlantSpecies.Form.STEM) surface++;
                            queue.enqueue(k);
                        }
            }
        }
        p.stage = to;
        p.blocks = count;
        p.surface = surface;
        return true;
    }

    /** Whether another plant of the same species has a block at {@code at} at its stage. */
    static boolean claimedByOther(InteriorRegion r, InteriorPlant p, BlockPos at) {
        int lx = r.localX(at.getX()), ly = r.localY(at.getY()), lz = r.localZ(at.getZ());
        for (InteriorPlant o : r.plants().all()) {
            if (o == p || o.species != p.species || o.stage <= 0) continue;
            if (Math.abs(o.x - lx) > 48 || Math.abs(o.z - lz) > 48 || Math.abs(o.y - ly) > 64) continue;
            if (o.x == lx && o.y == ly && o.z == lz) return true;
            for (PlantShapes.Voxel v : shape(r, o).at(o.stage)) {
                if (o.x + v.x() == lx && o.y + v.y() == ly && o.z + v.z() == lz) return true;
            }
        }
        return false;
    }

    /** The plant a block of the interior belongs to (its root or a block of its shape), or null. */
    @Nullable
    public static InteriorPlant ownerOf(InteriorRegion r, BlockPos worldPos, PlantSpecies s) {
        int lx = r.localX(worldPos.getX()), ly = r.localY(worldPos.getY()), lz = r.localZ(worldPos.getZ());
        InteriorPlant best = null;
        int bestD = Integer.MAX_VALUE;
        for (InteriorPlant o : r.plants().all()) {
            if (o.species != s) continue;
            if (o.x == lx && o.y == ly && o.z == lz) return o;
            if (Math.abs(o.x - lx) > 48 || Math.abs(o.z - lz) > 48 || Math.abs(o.y - ly) > 64 || o.stage <= 0) continue;
            for (PlantShapes.Voxel v : shape(r, o).at(o.stage)) {
                if (o.x + v.x() == lx && o.y + v.y() == ly && o.z + v.z() == lz) {
                    int d = Math.abs(o.x - lx) + Math.abs(o.y - ly) + Math.abs(o.z - lz);
                    if (d < bestD) {
                        bestD = d;
                        best = o;
                    }
                    break;
                }
            }
        }
        return best;
    }

    /** Takes all blocks of a plant out of the interior (the root too) and forgets it. */
    public static void remove(ServerLevel interior, InteriorRegion r, InteriorPlant p, boolean keepRoot) {
        PlantShapes.Shape shape = shape(r, p);
        loadChunks(interior, r, p, shape);
        r.plants().remove(p);
        BlockPos root = r.toWorld(p.x, p.y, p.z);
        AquaticPlantBlock block = block(p.species);
        for (PlantShapes.Voxel v : shape.at(Math.max(p.stage, 1))) {
            BlockPos at = root.offset(v.x(), v.y(), v.z());
            if (at.equals(root) && keepRoot) continue;
            if (interior.getBlockState(at).is(block) && !claimedByOther(r, p, at)) {
                interior.setBlock(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        if (!keepRoot && interior.getBlockState(root).is(block)) interior.setBlock(root, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        InteriorRegions.get(interior.getServer()).setDirty();
    }

    /** A side shoot (stem plants) or runner next to a full-grown plant: a new plant at stage 1 on substrate nearby. */
    @Nullable
    static InteriorPlant offshoot(ServerLevel interior, InteriorRegion r, InteriorPlant p) {
        if (r.plants().size() >= maxPerTank()) return null;
        var rnd = interior.getRandom();
        int[] dist = switch (p.species.form()) {
            case STEM -> new int[]{2, 4};
            case RIBBON -> new int[]{3, 7};
            case CARPET -> new int[]{3, 5};
            default -> new int[]{3, 6};
        };
        double u = r.scale() / 16.0;
        for (int attempt = 0; attempt < 8; attempt++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            double d = Math.max(MIN_ROOT_DISTANCE, (dist[0] + rnd.nextDouble() * (dist[1] - dist[0])) * Math.max(0.5, u));
            int x = p.x + (int) Math.round(Math.cos(a) * d), z = p.z + (int) Math.round(Math.sin(a) * d), y = p.y;
            BlockPos at = r.toWorld(x, y, z);
            if (!r.containsShapeInterior(at) || tooClose(r, x, y, z, null)) continue;
            int cx = at.getX() >> 4, cz = at.getZ() >> 4;
            if (interior.getChunkSource().getChunkNow(cx, cz) == null) {
                ChunkPos cp = new ChunkPos(cx, cz);
                interior.getChunkSource().addRegionTicket(InteriorPreparation.TICKET, cp, 0, cp);
                interior.getChunk(cx, cz);
            }
            if (!interior.getBlockState(at).isAir()) continue;
            BlockPos below = at.below();
            if (supportProblem(p.species, interior, below, interior.getBlockState(below), Direction.UP) != null) continue;
            InteriorPlant n = r.plants().add(p.species, x, y, z, rnd.nextLong(), 0);
            n.room = room(waterLocal(interior.getServer(), r), r, n);
            n.target = 1;
            n.tint = p.tint;
            n.redness = p.redness;
            interior.setBlock(at, state(p.species, PlantPart.CUTTING, p.tint), Block.UPDATE_CLIENTS);
            apply(interior, r, n);
            return n;
        }
        return null;
    }

    // ------------------------------------------------------------------ tests and smoke scenes

    /**
     * Plants a species at an interior-local position without the checks of a player (GameTests, smoke scenes) and
     * grows it to {@code stage} at once. Null if the place is taken.
     */
    @Nullable
    public static InteriorPlant plantAt(ServerLevel interior, InteriorRegion r, PlantSpecies s, int lx, int ly, int lz, int stage, long seed) {
        BlockPos at = r.toWorld(lx, ly, lz);
        int cx = at.getX() >> 4, cz = at.getZ() >> 4;
        interior.getChunk(cx, cz);
        if (!interior.getBlockState(at).canBeReplaced()) return null;
        InteriorPlant p = r.plants().add(s, lx, ly, lz, seed, 0);
        interior.setBlock(at, state(s, PlantPart.CUTTING, 0), Block.UPDATE_CLIENTS);
        p.room = room(waterLocal(interior.getServer(), r), r, p);
        p.target = Math.max(0, Math.min(stage, s.stages()));
        apply(interior, r, p);
        InteriorRegions.get(interior.getServer()).setDirty();
        return p;
    }

    /** Sets the redness of every plant of a tank to what its light makes it now (instead of waiting about two days). */
    public static void settleTint(ServerLevel level, TankBlockEntity c) {
        InteriorRegion r = region(level.getServer(), c.getTank());
        ServerLevel interior = InteriorManager.interiorLevel(level.getServer());
        if (r == null || interior == null) return;
        int tint = PlantColors.tintFor((int) Math.round(TankPlants.strongLight(level, c)));
        for (InteriorPlant p : new ArrayList<>(r.plants().all())) {
            if (!p.species.tinted()) continue;
            p.redness = tint / (double) PlantColors.MAX_TINT;
            if (p.tint != tint) {
                p.tint = tint;
                apply(interior, r, p);
            }
        }
    }

    // ------------------------------------------------------------------ trimming, dividing, taking out

    /** Shears on a plant block inside the interior. */
    public static void trimBlock(ServerLevel interior, BlockPos pos, Player player, ItemStack shears, InteractionHand hand) {
        InteriorRegion r = InteriorManager.regionAt(interior.getServer(), pos);
        PlantSpecies s = speciesOf(interior.getBlockState(pos));
        if (r == null || s == null) return;
        if (!InteriorManager.canBuild(player, pos)) {
            player.displayClientMessage(Component.translatable("message.bettacraft.tank.locked").withStyle(ChatFormatting.RED), true);
            return;
        }
        InteriorPlant p = ownerOf(r, pos, s);
        if (p == null) {
            message(player, "none_here", ChatFormatting.GOLD);
            return;
        }
        trim(interior, r, p, player, shears, hand);
    }

    /** Cuts a plant back by {@link PlantSpecies#trimStages()} and gives the cuttings. Returns the cuttings given. */
    public static int trim(ServerLevel interior, InteriorRegion r, InteriorPlant p, @Nullable Player player, @Nullable ItemStack shears,
                           @Nullable InteractionHand hand) {
        int cut = Math.min(p.species.trimStages(), p.stage - p.species.trimFloor());
        if (cut <= 0) {
            message(player, "too_small", ChatFormatting.GOLD);
            return 0;
        }
        p.target = p.stage - cut;
        p.progress = 0;
        apply(interior, r, p);
        int n = p.species.cuttingsPerTrim(cut);
        if (player != null) {
            ItemStack items = item(p.species, 0).copyWithCount(n);
            if (!player.getInventory().add(items) && !items.isEmpty()) player.drop(items, false);
            if (shears != null && hand != null && !player.getAbilities().instabuild) {
                shears.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 0.7F, 1.4F);
            player.displayClientMessage(Component.translatable("message.bettacraft.aquatic.trimmed", n,
                    Component.translatable(item(p.species, 0).getDescriptionId())).withStyle(ChatFormatting.GREEN), true);
        }
        InteriorRegions.get(interior.getServer()).setDirty();
        TankBlockEntity c = controller(interior.getServer(), r);
        if (c != null && c.getLevel() instanceof ServerLevel tl) {
            TankCare.log(tl, c, "log.bettacraft.aquatic_trimmed", player == null ? "?" : TankCare.nameArg(player), "@" + item(p.species, 0).getDescriptionId(),
                    String.valueOf(n));
        }
        return n;
    }

    /**
     * A player breaks a plant block in the interior (before it is removed): the root takes the whole plant out (as an
     * item that keeps its stage), a rhizome piece divides an epiphyte (half of it as an item). Other blocks are just
     * gone (the plant grows them again with its next stage).
     */
    public static void onBreak(ServerLevel interior, BlockPos pos, BlockState state, Player player) {
        InteriorRegion r = InteriorManager.regionAt(interior.getServer(), pos);
        PlantSpecies s = speciesOf(state);
        if (r == null || s == null) return;
        InteriorPlant p = ownerOf(r, pos, s);
        if (p == null) return;
        PlantPart part = state.getValue(AquaticPlantBlock.PART);
        boolean drops = !player.getAbilities().instabuild;
        if (p.x == r.localX(pos.getX()) && p.y == r.localY(pos.getY()) && p.z == r.localZ(pos.getZ())) {
            int stage = p.stage;
            remove(interior, r, p, true);
            if (drops) Block.popResource(interior, pos, item(s, stage));
            message(player, "taken_out", ChatFormatting.GREEN);
            logAt(interior, r, "log.bettacraft.aquatic_taken_out", TankCare.nameArg(player), "@" + item(s, 0).getDescriptionId());
        } else if (part == PlantPart.RHIZOME && s.form() == PlantSpecies.Form.EPIPHYTE && p.stage >= 2) {
            int half = p.stage / 2;
            p.target = p.stage - half;
            p.progress = 0;
            apply(interior, r, p);
            if (drops) Block.popResource(interior, pos, item(s, half));
            message(player, "divided", ChatFormatting.GREEN);
            logAt(interior, r, "log.bettacraft.aquatic_divided", TankCare.nameArg(player), "@" + item(s, 0).getDescriptionId());
            InteriorRegions.get(interior.getServer()).setDirty();
        }
    }

    private static void logAt(ServerLevel interior, InteriorRegion r, String key, String... args) {
        TankBlockEntity c = controller(interior.getServer(), r);
        if (c != null && c.getLevel() instanceof ServerLevel tl) TankCare.log(tl, c, key, args);
    }

    /** A root block vanished without a player breaking it (commands, other mods): the plant is forgotten. */
    public static void onRootGone(InteriorRegion r, int lx, int ly, int lz) {
        InteriorPlant p = r.plants().at(lx, ly, lz);
        if (p != null) {
            r.plants().remove(p);
        }
    }

    /**
     * Shears used on the tank from outside: the first plant block the view ray meets in the miniature is trimmed (the
     * ray stops at decoration). Returns false when it met no plant.
     */
    public static boolean trimFromOutside(ServerLevel tankLevel, TankBlockEntity c, Player player, ItemStack shears, InteractionHand hand) {
        InteriorRegion r = region(tankLevel.getServer(), c.getTank());
        ServerLevel interior = InteriorManager.interiorLevel(tankLevel.getServer());
        if (r == null || interior == null || r.plants().isEmpty()) {
            message(player, "none", ChatFormatting.GOLD);
            return false;
        }
        InteriorPlant p = aimedAt(interior, r, c.getTank(), player.getEyePosition(), player.getViewVector(1.0F));
        if (p == null) {
            message(player, "none_here", ChatFormatting.GOLD);
            return false;
        }
        trim(interior, r, p, player, shears, hand);
        return true;
    }

    /** The plant the ray from {@code eye} along {@code look} (world, in front of the tank) meets in the miniature. */
    @Nullable
    public static InteriorPlant aimedAt(ServerLevel interior, InteriorRegion r, TankData t, Vec3 eye, Vec3 look) {
        BlockPos min = t.min();
        double fx = (t.sizeX() - 2 * MARGIN) / r.sizeX(), fy = (t.sizeY() - 2 * MARGIN) / r.sizeY(), fz = (t.sizeZ() - 2 * MARGIN) / r.sizeZ();
        double ox = (eye.x - min.getX() - MARGIN) / fx, oy = (eye.y - min.getY() - MARGIN) / fy, oz = (eye.z - min.getZ() - MARGIN) / fz;
        double dx = look.x / fx, dy = look.y / fy, dz = look.z / fz;
        InteriorPlant[] found = new InteriorPlant[1];
        PlantRay.walk(ox, oy, oz, dx, dy, dz, r.sizeX(), r.sizeY(), r.sizeZ(), OUTSIDE_REACH, (x, y, z) -> {
            BlockPos at = r.toWorld(x, y, z);
            BlockState s = interior.getBlockState(at);
            if (s.isAir() || InteriorManager.isShellBlock(s)) return false;
            PlantSpecies sp = speciesOf(s);
            if (sp == null) return !s.getCollisionShape(interior, at).isEmpty(); // decoration hides what is behind it
            found[0] = ownerOf(r, at, sp);
            return found[0] != null;
        });
        return found[0];
    }

    // ------------------------------------------------------------------ effects on the tank

    /** Plant mass in tank blocks per water block (0 without plants). */
    public static double volumeShare(MinecraftServer server, TankData t) {
        InteriorRegion r = region(server, t);
        if (r == null || r.plants().isEmpty()) return 0;
        return r.plants().blocks() / Math.pow(r.scale(), 3) / Math.max(1.0, t.waterBlocks());
    }

    /** Breeding readiness bonus of a well planted tank (server config interiorPlants.breedingBonus). */
    public static double breedingBonus(MinecraftServer server, TankData t) {
        return volumeShare(server, t) >= BONUS_FROM ? Math.max(1.0, ServerConfig.getOr(ServerConfig.AQUATIC_BREEDING_BONUS, 1.1)) : 1.0;
    }

    /** Share of the chases after spawning a female escapes by hiding in the plants (0..interiorPlants.hiding). */
    public static double hiding(MinecraftServer server, TankData t) {
        double max = ServerConfig.getOr(ServerConfig.AQUATIC_HIDING, 0.6);
        if (max <= 0) return 0;
        return max * Math.min(1.0, volumeShare(server, t) / HIDING_FULL_AT);
    }

    /** Plants at the surface plus floating plants cover the surface densely (warning). */
    public static boolean overgrown(MinecraftServer server, TankData t) {
        InteriorRegion r = region(server, t);
        if (r == null || r.plants().isEmpty()) return false;
        double own = surfaceCover(t, r);
        return own >= 0.05 && own + TankPlants.coverage(t) >= ServerConfig.getOr(ServerConfig.PLANTS_DENSE_FROM, 0.5);
    }
}
