package org.overengineer.bettacraft.tank.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bucketable;
import org.overengineer.bettacraft.entity.betta.BettaCare;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.BettaHealth;
import org.overengineer.bettacraft.equipment.FeederBlockEntity;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.tank.TankBreeding;
import org.overengineer.bettacraft.tank.SignPosition;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;

/**
 * What the tank menu shows (PLAN 8). Built on the server per player, sent as NBT only when it changed while the
 * menu is open ({@link TankMenu#broadcastChanges}).
 */
public final class TankView {
    public static final int LOG_LINES = 60;

    /**
     * @param health   care health (1 healthy, 0.5 sick, 0 dead)
     * @param hp       wounds: current / max hit points
     * @param progress growth within the stage (0..1)
     */
    public record Inhabitant(UUID uuid, String customName, String typeKey, boolean betta, boolean female, String stage,
                             boolean adult, float health, float satiation, boolean sick, boolean bucketable, BettaGenome genome,
                             BettaGenotype genotype, float hp, float progress, int generation, boolean frog, float air, boolean noAir,
                             CompoundTag tankmate) {
        /** M19: without the tankmate details (bettas, frogs, other animals). */
        public Inhabitant(UUID uuid, String customName, String typeKey, boolean betta, boolean female, String stage,
                          boolean adult, float health, float satiation, boolean sick, boolean bucketable, BettaGenome genome,
                          BettaGenotype genotype, float hp, float progress, int generation, boolean frog, float air, boolean noAir) {
            this(uuid, customName, typeKey, betta, female, stage, adult, health, satiation, sick, bucketable, genome, genotype, hp, progress,
                    generation, frog, air, noAir, new CompoundTag());
        }

        /** M19: a tankmate (the details are in {@link #tankmate}). */
        public boolean isTankmate() {
            return tankmate.contains("species");
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", uuid);
            t.putString("name", customName);
            t.putString("type", typeKey);
            t.putBoolean("betta", betta);
            t.putBoolean("female", female);
            t.putString("stage", stage);
            t.putBoolean("adult", adult);
            t.putFloat("health", health);
            t.putFloat("sat", satiation);
            t.putBoolean("sick", sick);
            t.putBoolean("bucketable", bucketable);
            BettaGenome.CODEC.encodeStart(NbtOps.INSTANCE, genome).result().ifPresent(g -> t.put("genome", g));
            t.put("genotype", genotype.save());
            t.putFloat("hp", hp);
            t.putFloat("progress", progress);
            t.putInt("gen", generation);
            if (frog) {
                t.putBoolean("frog", true);
                t.putFloat("air", air);
                t.putBoolean("no_air", noAir);
            }
            if (!tankmate.isEmpty()) t.put("tm", tankmate);
            return t;
        }

        static Inhabitant load(CompoundTag t) {
            BettaGenome g = BettaGenome.CODEC.parse(NbtOps.INSTANCE, t.getCompound("genome")).result().orElse(BettaGenome.DEFAULT);
            return new Inhabitant(t.getUUID("id"), t.getString("name"), t.getString("type"), t.getBoolean("betta"), t.getBoolean("female"),
                    t.getString("stage"), t.getBoolean("adult"), t.getFloat("health"), t.getFloat("sat"), t.getBoolean("sick"),
                    t.getBoolean("bucketable"), g, BettaGenotype.load(t.getCompound("genotype")), t.getFloat("hp"), t.getFloat("progress"),
                    t.getInt("gen"), t.getBoolean("frog"), t.getFloat("air"), t.getBoolean("no_air"), t.getCompound("tm"));
        }
    }

    public record LogLine(long time, String key, List<String> args) {}

    /** M19: what the animals tab shows of a tankmate. */
    static CompoundTag tankmateDetails(TankData t, org.overengineer.bettacraft.entity.tankmate.TankmateEntity m) {
        var s = m.species();
        CompoundTag d = new CompoundTag();
        d.putString("species", s.id());
        d.putString("variety", m.variety());
        d.putString("genes", m.genes().text());
        // M21.6: the guppy and endler genome (with features.livebearerGenetics)
        var lb = m.livebearerGenome();
        if (lb != null) d.put("lb", lb.save());
        d.putInt("shoal", m.shoalSize());
        d.putInt("stress", m.stressBits());
        d.putBoolean("guard", m.isGuarding());
        d.putBoolean("cave", m.cave() != null);
        d.putFloat("gravid", m.isGravid() ? (float) Math.min(1.0, m.gravidDays() / Math.max(1e-6,
                org.overengineer.bettacraft.tankmates.TankmateBreeding.gestationDays())) : -1F);
        d.putFloat("temp", t.waterTemp());
        d.putFloat("stock", (float) org.overengineer.bettacraft.tank.TankInhabitants.weight(m));
        return d;
    }

    public BlockPos pos = BlockPos.ZERO;
    public String name = "";
    public boolean signVisible = true;
    public SignPosition signPos = SignPosition.TOP_CENTER;
    public Direction front = Direction.SOUTH;
    public boolean locked;
    /** M21.5: break protection on, and whether the feature is switched on at all. */
    public boolean breakProtected, protectionFeature;
    public boolean canModify = true;
    public boolean isOwner;
    public String ownerName = "";
    public long waterMb, capacityMb;
    public int sizeX, sizeY, sizeZ;
    /** M11: glass block id and style of the tank. */
    public String glass = "minecraft:glass";
    public org.overengineer.bettacraft.tank.TankStyle style = org.overengineer.bettacraft.tank.TankStyle.TEXTURED;
    public float waterTemp, targetTemp, ambient;
    public double heaterBlocks;
    public int feederFood = -1, feederPerDay;
    public int lights, lightsLit;
    public int adults, hardCap;
    public boolean crowded;
    public boolean hasBucket, hasBag;
    /** Water the bag button takes: 100 mB for fish bags, 0 for a Fintastic fishnet (M7). */
    public long bagMb = 100;
    public long minInsertMb;
    public final List<Inhabitant> inhabitants = new ArrayList<>();
    public final List<LogLine> log = new ArrayList<>();
    public long gameTime;
    /** Breeding tab content ({@link org.overengineer.bettacraft.tank.TankBreeding#describe}). */
    public CompoundTag breeding = new CompoundTag();
    public final List<String> warnings = new ArrayList<>();
    /** Interior tab (M3): scale, size, synced blocks, equipment. */
    public int scale, interiorX, interiorY, interiorZ, interiorBlocks = -1, renderLimit, substrate, heatersInside, heaterCapWet, heaterCapDry,
            filters, floating;
    public boolean interiorBuilt, interiorTruncated;
    /** M4: external filters (blocks next to the tank / filters in cabinets below), their heaters, cabinets, old slots. */
    public int filterBlocks, cabinetFilters, cabinets, filterHeaters, filterHeaterBlocks, legacyHeaters;
    /** M9: coolers, water quality, evaporation and power. */
    public double coolerBlocks;
    public int filterCoolers, coolersInside, coolerCapWet;
    public final Water water = new Water();
    /** M12: floating plants (water tab, plants page). */
    public final Plants plants = new Plants();
    /** M13: aquarium plants of the interior (second page of the plants tab). */
    public final Aquatic aquatic = new Aquatic();
    /** M14: shrimp, crayfish and snails (second page of the animals tab), algae, stocking. */
    public final Critters critters = new Critters();
    /** M14: stocking in fish (adults, frogs and small animals by config). */
    public double stock;
    /** M18: parts of the stocking: adult and young inhabitants, small animals, eggs and larvae in the nest. */
    public double stockAdults, stockYoung, stockCritters, stockNest;
    public int young;

    /** One kind of small animal in the tank: species and variety with its counts and grades. */
    public record CritterGroup(int species, String variety, int rgb, int adults, int young, int females, int males, int berried,
                               float gradeAvg, float gradeMax, float gradeMin) {
        public String key() {
            return org.overengineer.bettacraft.critters.CritterSpecies.values()[species].id() + ":" + variety;
        }
    }

    /** M14: the small animals for the menu. */
    public static final class Critters {
        public final List<CritterGroup> groups = new ArrayList<>();
        public int total, max, dead, clutches, molts;
        public double stock, algae, algaeGrowth, grazing, foodDays, hiding;
        public boolean hunting;

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            ListTag l = new ListTag();
            for (CritterGroup g : groups) {
                CompoundTag c = new CompoundTag();
                c.putIntArray("n", new int[]{g.species(), g.rgb(), g.adults(), g.young(), g.females(), g.males(), g.berried()});
                c.putString("v", g.variety());
                c.putFloat("ga", g.gradeAvg());
                c.putFloat("gx", g.gradeMax());
                c.putFloat("gn", g.gradeMin());
                l.add(c);
            }
            t.put("groups", l);
            t.putIntArray("n", new int[]{total, max, dead, clutches, molts});
            t.putDouble("stock", stock);
            t.putDouble("algae", algae);
            t.putDouble("growth", algaeGrowth);
            t.putDouble("grazing", grazing);
            t.putDouble("food", foodDays);
            t.putDouble("hiding", hiding);
            t.putBoolean("hunting", hunting);
            return t;
        }

        void load(CompoundTag t) {
            ListTag l = t.getList("groups", Tag.TAG_COMPOUND);
            int species = org.overengineer.bettacraft.critters.CritterSpecies.values().length;
            for (int i = 0; i < l.size(); i++) {
                CompoundTag c = l.getCompound(i);
                int[] n = c.getIntArray("n");
                if (n.length != 7 || n[0] < 0 || n[0] >= species) continue;
                groups.add(new CritterGroup(n[0], c.getString("v"), n[1], n[2], n[3], n[4], n[5], n[6], c.getFloat("ga"), c.getFloat("gx"), c.getFloat("gn")));
            }
            int[] n = t.getIntArray("n");
            if (n.length == 5) {
                total = n[0];
                max = n[1];
                dead = n[2];
                clutches = n[3];
                molts = n[4];
            }
            stock = t.getDouble("stock");
            algae = t.getDouble("algae");
            algaeGrowth = t.getDouble("growth");
            grazing = t.getDouble("grazing");
            foodDays = t.getDouble("food");
            hiding = t.getDouble("hiding");
            hunting = t.getBoolean("hunting");
        }
    }

    private static Critters critters(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        Critters out = new Critters();
        java.util.Map<String, List<org.overengineer.bettacraft.critters.Critter>> by = new java.util.LinkedHashMap<>();
        List<org.overengineer.bettacraft.critters.Critter> sorted = new ArrayList<>(t.critters());
        sorted.sort(java.util.Comparator.comparingInt((org.overengineer.bettacraft.critters.Critter k) -> k.species.ordinal())
                .thenComparing(k -> k.genes.variety(k.species)));
        for (var k : sorted) {
            if (!k.alive()) {
                out.dead++;
                continue;
            }
            by.computeIfAbsent(org.overengineer.bettacraft.critters.TankCritters.groupKey(k), x -> new ArrayList<>()).add(k);
        }
        for (var e : by.values()) {
            var first = e.getFirst();
            int adults = 0, young = 0, females = 0, males = 0, berried = 0;
            float sum = 0, mx = 0, mn = 1;
            for (var k : e) {
                if (k.adult()) adults++;
                else young++;
                if (k.species.hasSexes()) {
                    if (k.female) females++;
                    else males++;
                }
                if (k.berried()) berried++;
                sum += k.genes.grade();
                mx = Math.max(mx, k.genes.grade());
                mn = Math.min(mn, k.genes.grade());
            }
            out.groups.add(new CritterGroup(first.species.ordinal(), first.genes.variety(first.species), first.genes.rgb(first.species), adults, young,
                    females, males, berried, sum / e.size(), mx, mn));
        }
        out.total = org.overengineer.bettacraft.critters.TankCritters.alive(t);
        out.max = org.overengineer.bettacraft.critters.TankCritters.maxPerTank();
        for (var o : t.critterObjects()) {
            if (o.kind == org.overengineer.bettacraft.critters.CritterObject.Kind.CLUTCH) out.clutches++;
            else out.molts++;
        }
        out.stock = org.overengineer.bettacraft.critters.TankCritters.stock(t);
        out.algae = org.overengineer.bettacraft.tank.algae.TankAlgae.mean(t);
        out.algaeGrowth = org.overengineer.bettacraft.tank.algae.TankAlgae.growthPerDay(level, c)
                * org.overengineer.bettacraft.tank.algae.TankAlgae.wetFaces(t).size();
        out.grazing = org.overengineer.bettacraft.tank.algae.TankAlgae.grazingPerDay(t);
        double demand = org.overengineer.bettacraft.critters.CritterLife.demand(t.critters());
        double supply = org.overengineer.bettacraft.critters.TankCritters.foodPerDay(level, t) + Math.min(out.algaeGrowth, out.grazing)
                * org.overengineer.bettacraft.critters.TankCritters.FOOD_PER_ALGAE;
        out.foodDays = demand <= 0 ? 99 : supply / demand;
        out.hiding = org.overengineer.bettacraft.critters.TankCritters.hiding(level, t);
        out.hunting = org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.CRITTERS_BETTAS_HUNT, true)
                && !TankCare.bettas(level, t).isEmpty();
        return out;
    }

    /** Aquarium plants of the interior: count per species, mass, light, growth, uptake, hiding spots, shade. */
    public static final class Aquatic {
        public int[] counts = new int[org.overengineer.bettacraft.plants.PlantSpecies.values().length];
        /** Highest tint level per species (red species: the colour they show). */
        public int[] tints = new int[org.overengineer.bettacraft.plants.PlantSpecies.values().length];
        public int total, maxPlants, blocks, light, growing, fullGrown, dark, dry;
        public float share, surfaceCover, growthSpeed, hiding, temp;
        public double uptakePerDay;
        public boolean bonus, overgrown, built;

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putIntArray("counts", counts);
            t.putIntArray("tints", tints);
            t.putIntArray("n", new int[]{total, maxPlants, blocks, light, growing, fullGrown, dark, dry});
            t.putFloat("share", share);
            t.putFloat("surface", surfaceCover);
            t.putFloat("speed", growthSpeed);
            t.putFloat("hiding", hiding);
            t.putFloat("temp", temp);
            t.putDouble("uptake", uptakePerDay);
            t.putBoolean("bonus", bonus);
            t.putBoolean("overgrown", overgrown);
            t.putBoolean("built", built);
            return t;
        }

        void load(CompoundTag t) {
            int[] c = t.getIntArray("counts");
            if (c.length == counts.length) counts = c;
            int[] ti = t.getIntArray("tints");
            if (ti.length == tints.length) tints = ti;
            int[] n = t.getIntArray("n");
            if (n.length == 8) {
                total = n[0];
                maxPlants = n[1];
                blocks = n[2];
                light = n[3];
                growing = n[4];
                fullGrown = n[5];
                dark = n[6];
                dry = n[7];
            }
            share = t.getFloat("share");
            surfaceCover = t.getFloat("surface");
            growthSpeed = t.getFloat("speed");
            hiding = t.getFloat("hiding");
            temp = t.getFloat("temp");
            uptakePerDay = t.getDouble("uptake");
            bonus = t.getBoolean("bonus");
            overgrown = t.getBoolean("overgrown");
            built = t.getBoolean("built");
        }
    }

    private static Aquatic aquatic(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        Aquatic a = new Aquatic();
        a.maxPlants = org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.AQUATIC_MAX_PER_TANK, 48);
        a.growthSpeed = (float) org.overengineer.bettacraft.plants.InteriorPlants.growthSpeed();
        a.temp = t.waterTemp();
        var r = org.overengineer.bettacraft.plants.InteriorPlants.region(level.getServer(), t);
        a.light = org.overengineer.bettacraft.tank.plants.TankPlants.light(level, c);
        if (r == null) return a;
        a.built = true;
        var env = org.overengineer.bettacraft.plants.InteriorPlants.env(level, c, r);
        a.light = env.light();
        for (var p : r.plants().all()) {
            a.counts[p.species.ordinal()]++;
            a.tints[p.species.ordinal()] = Math.max(a.tints[p.species.ordinal()], p.tint);
            a.blocks += p.blocks;
            if (p.y + 0.5 >= env.waterLocal()) a.dry++;
            else if (org.overengineer.bettacraft.plants.InteriorPlants.lightFactor(env.effectiveLight(), p.species.light()) <= 0) a.dark++;
            else if (p.fullGrown()) a.fullGrown++;
            else if (env.rate(p) > 0) a.growing++;
        }
        a.total = r.plants().size();
        a.share = (float) org.overengineer.bettacraft.plants.InteriorPlants.volumeShare(level.getServer(), t);
        a.surfaceCover = (float) org.overengineer.bettacraft.plants.InteriorPlants.surfaceCover(t, r);
        a.hiding = (float) org.overengineer.bettacraft.plants.InteriorPlants.hiding(level.getServer(), t);
        a.bonus = org.overengineer.bettacraft.plants.InteriorPlants.breedingBonus(level.getServer(), t) > 1.0;
        a.overgrown = org.overengineer.bettacraft.plants.InteriorPlants.overgrown(level.getServer(), t);
        a.uptakePerDay = org.overengineer.bettacraft.plants.InteriorPlants.uptakePerDay(level, c, r);
        return a;
    }

    /** Floating plants of the tank: count per species, cover, light, growth and waste uptake. */
    public static final class Plants {
        public int[] counts = new int[org.overengineer.bettacraft.tank.plants.FloatingPlantType.values().length];
        public int total, light, maxPlants;
        public float coverage, maxCoverage, denseFrom, growthSpeed;
        public double uptakePerDay;
        public boolean dense;

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putIntArray("counts", counts);
            t.putInt("total", total);
            t.putInt("light", light);
            t.putInt("max", maxPlants);
            t.putFloat("cov", coverage);
            t.putFloat("max_cov", maxCoverage);
            t.putFloat("dense_from", denseFrom);
            t.putFloat("speed", growthSpeed);
            t.putDouble("uptake", uptakePerDay);
            t.putBoolean("dense", dense);
            return t;
        }

        void load(CompoundTag t) {
            int[] c = t.getIntArray("counts");
            if (c.length == counts.length) counts = c;
            total = t.getInt("total");
            light = t.getInt("light");
            maxPlants = t.getInt("max");
            coverage = t.getFloat("cov");
            maxCoverage = t.getFloat("max_cov");
            denseFrom = t.getFloat("dense_from");
            growthSpeed = t.getFloat("speed");
            uptakePerDay = t.getDouble("uptake");
            dense = t.getBoolean("dense");
        }
    }

    private static Plants plants(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        Plants p = new Plants();
        for (var pl : t.plants()) p.counts[pl.type.ordinal()]++;
        p.total = t.plants().size();
        var params = org.overengineer.bettacraft.tank.plants.TankPlants.params();
        p.maxPlants = params.maxPlants();
        p.coverage = (float) org.overengineer.bettacraft.tank.plants.TankPlants.coverage(t);
        p.maxCoverage = (float) params.maxCoverage();
        p.denseFrom = (float) params.shadeFrom();
        p.growthSpeed = (float) params.growthSpeed();
        p.dense = org.overengineer.bettacraft.tank.plants.TankPlants.dense(t);
        p.light = org.overengineer.bettacraft.tank.plants.TankPlants.light(level, c);
        double area = 0;
        for (var pl : t.plants()) if (pl.type.grows()) area += pl.area() * pl.type.uptake();
        double uptake = org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.PLANTS_WASTE_UPTAKE, 0.04);
        p.uptakePerDay = !org.overengineer.bettacraft.tank.TankWater.qualityEnabled() ? 0
                : uptake * area * org.overengineer.bettacraft.tank.plants.FloatingPlantSim.lightFactor(p.light) / Math.max(1.0, t.waterBlocks());
        return p;
    }

    /** Water tab (M9). */
    public static final class Water {
        public boolean qualityEnabled = true, evaporationEnabled = true;
        public float quality = 1, badBelow = 0.35F;
        public String state = "clean";
        public double declinePerDay, filterShare, animalsPerBlock, filterCapacity, evaporationPerDay;
        public int openTop, totalTop, internalFilters;
        public long lastFreshWater = -1;
        public boolean heaterPower, filterPower, lightPower, heatPowered = true, filterPowered = true, lightPowered = true;
        public long buffer, bufferCap;
        public int demand;
        /** M20: salinity (ppt), calcium (mg/L), skimmers, flow pumps, flow (1 = strong), live rock blocks, light spectrum for corals. */
        public float salinity, calcium;
        public int skimmers, pumps, liveRock;
        public double flow, spectrum;

        public boolean reefLine() {
            return salinity >= org.overengineer.bettacraft.tank.Salinity.BRACKISH_FROM || skimmers > 0 || pumps > 0 || liveRock > 0;
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putFloat("salinity", salinity);
            t.putFloat("calcium", calcium);
            t.putInt("skimmers", skimmers);
            t.putInt("pumps", pumps);
            t.putInt("live_rock", liveRock);
            t.putDouble("flow", flow);
            t.putDouble("spectrum", spectrum);
            t.putBoolean("q_on", qualityEnabled);
            t.putBoolean("e_on", evaporationEnabled);
            t.putFloat("q", quality);
            t.putFloat("bad", badBelow);
            t.putString("state", state);
            t.putDouble("decline", declinePerDay);
            t.putDouble("filter_share", filterShare);
            t.putDouble("animals", animalsPerBlock);
            t.putDouble("filter_cap", filterCapacity);
            t.putDouble("evap", evaporationPerDay);
            t.putInt("open", openTop);
            t.putInt("top", totalTop);
            t.putInt("internal_filters", internalFilters);
            t.putLong("fresh", lastFreshWater);
            t.putBoolean("p_heat", heaterPower);
            t.putBoolean("p_filter", filterPower);
            t.putBoolean("p_light", lightPower);
            t.putBoolean("ok_heat", heatPowered);
            t.putBoolean("ok_filter", filterPowered);
            t.putBoolean("ok_light", lightPowered);
            t.putLong("buffer", buffer);
            t.putLong("buffer_cap", bufferCap);
            t.putInt("demand", demand);
            return t;
        }

        void load(CompoundTag t) {
            salinity = t.getFloat("salinity");
            calcium = t.getFloat("calcium");
            skimmers = t.getInt("skimmers");
            pumps = t.getInt("pumps");
            liveRock = t.getInt("live_rock");
            flow = t.getDouble("flow");
            spectrum = t.getDouble("spectrum");
            qualityEnabled = t.getBoolean("q_on");
            evaporationEnabled = t.getBoolean("e_on");
            quality = t.getFloat("q");
            badBelow = t.getFloat("bad");
            state = t.getString("state");
            declinePerDay = t.getDouble("decline");
            filterShare = t.getDouble("filter_share");
            animalsPerBlock = t.getDouble("animals");
            filterCapacity = t.getDouble("filter_cap");
            evaporationPerDay = t.getDouble("evap");
            openTop = t.getInt("open");
            totalTop = t.getInt("top");
            internalFilters = t.getInt("internal_filters");
            lastFreshWater = t.getLong("fresh");
            heaterPower = t.getBoolean("p_heat");
            filterPower = t.getBoolean("p_filter");
            lightPower = t.getBoolean("p_light");
            heatPowered = t.getBoolean("ok_heat");
            filterPowered = t.getBoolean("ok_filter");
            lightPowered = t.getBoolean("ok_light");
            buffer = t.getLong("buffer");
            bufferCap = t.getLong("buffer_cap");
            demand = t.getInt("demand");
        }

        public boolean anyPower() {
            return heaterPower || filterPower || lightPower;
        }
    }

    public static CompoundTag build(ServerPlayer player, TankBlockEntity controller) {
        ServerLevel level = player.serverLevel();
        TankData t = controller.getTank();
        CompoundTag out = new CompoundTag();
        out.putLong("pos", controller.getBlockPos().asLong());
        out.putString("name", t.name());
        out.putBoolean("sign", t.signVisible());
        out.putString("sign_pos", t.signPos().getSerializedName());
        out.putString("front", t.front().getSerializedName());
        out.putBoolean("locked", t.locked());
        out.putBoolean("break_protected", t.breakProtected());
        out.putBoolean("protection_feature", org.overengineer.bettacraft.config.CommonConfig.breakProtection());
        out.putBoolean("can_modify", TankInhabitants.canModify(player, t));
        out.putBoolean("owner", player.getUUID().equals(t.owner()));
        String ownerName = "";
        if (t.owner() != null) {
            var sp = level.getServer().getPlayerList().getPlayer(t.owner());
            if (sp != null) ownerName = sp.getGameProfile().getName();
            else ownerName = level.getServer().getProfileCache() == null ? "" : level.getServer().getProfileCache().get(t.owner())
                    .map(com.mojang.authlib.GameProfile::getName).orElse("");
        }
        out.putString("owner_name", ownerName);
        out.putLong("water", t.waterMb());
        out.putLong("cap", t.capacityMb());
        out.putIntArray("size", new int[]{t.sizeX(), t.sizeY(), t.sizeZ()});
        out.putString("glass", controller.getGlassId().toString());
        out.putString("style", controller.getStyle().getSerializedName());
        out.putFloat("temp", Math.round(t.waterTemp() * 10) / 10.0F);
        out.putFloat("target", t.targetTemp());
        out.putFloat("ambient", Math.round(TankCare.ambient(level, t) * 10) / 10.0F);
        out.putDouble("heaters", TankCare.heaterBlocks(level, t));
        out.putDouble("coolers", TankCare.coolerBlocks(level, t));
        FeederBlockEntity feeder = TankCare.findFeeder(level, t);
        out.putInt("feeder", feeder == null ? -1 : feeder.foodCount());
        out.putInt("feeder_per_day", feeder == null ? 0 : feeder.feedingsPerDay());
        int[] lights = TankCare.lights(level, t);
        out.putInt("lights", lights[0]);
        out.putInt("lights_lit", lights[1]);
        out.putBoolean("has_bucket", TankInhabitants.findContainer(player, false) >= 0);
        long bagMb = TankInhabitants.bagWater(player);
        out.putBoolean("has_bag", bagMb >= 0);
        out.putLong("bag_mb", Math.max(0, bagMb));
        out.putLong("min_insert", Math.round(org.overengineer.bettacraft.config.ServerConfig.getOr(
                org.overengineer.bettacraft.config.ServerConfig.TANK_MIN_FILL_TO_INSERT, 0.5) * t.capacityMb()));

        BettaHealth.Params p = BettaCare.params();
        ListTag inh = new ListTag();
        List<Entity> live = TankInhabitants.liveInhabitants(level, t);
        live.sort(java.util.Comparator.comparing(e -> e.getUUID()));
        for (Entity e : live) {
            Inhabitant i;
            String custom = e.hasCustomName() ? e.getCustomName().getString() : "";
            if (e instanceof BettaEntity b) {
                BettaHealth h = b.health();
                i = new Inhabitant(e.getUUID(), custom, e.getType().getDescriptionId(), true, b.isFemale(), b.stage(), b.isAdult(),
                        round(h.healthFraction(p)), round(h.satiation(p)), h.isSick(), true, b.genome(), b.genotype(),
                        round(b.getHealth() / b.getMaxHealth()), round((float) b.stageProgress()), b.getBettaData().generation(), false, 0F, false);
            } else if (e instanceof org.overengineer.bettacraft.entity.frog.DwarfFrogEntity f) {
                // M14: frogs have needs (food, air at the surface)
                float hp = round(f.getHealth() / f.getMaxHealth());
                i = new Inhabitant(e.getUUID(), custom, e.getType().getDescriptionId(), false, false, "", true, hp, round(f.satiation()),
                        f.daysSinceFed() > org.overengineer.bettacraft.entity.frog.DwarfFrogEntity.STARVE_DAYS, true, BettaGenome.DEFAULT,
                        BettaGenotype.homozygous(BettaGenome.DEFAULT), hp, 1F, 0, true,
                        round(f.breath() / (float) org.overengineer.bettacraft.entity.frog.DwarfFrogEntity.maxBreath()), !f.airAvailable());
            } else if (e instanceof org.overengineer.bettacraft.entity.tankmate.TankmateEntity m) {
                // M19: tankmates have needs like the bettas, plus their shoal, temperature range and breeding state
                BettaHealth h = m.health();
                i = new Inhabitant(e.getUUID(), custom, e.getType().getDescriptionId(), false, m.isFemale(), m.lifeStage().id(), m.isAdult(),
                        round(h.healthFraction(p)), round(h.satiation(p)), h.isSick(), true, BettaGenome.DEFAULT,
                        BettaGenotype.homozygous(BettaGenome.DEFAULT), round(m.getHealth() / m.getMaxHealth()), round((float) m.stageProgress()),
                        m.generation(), false, 0F, false, tankmateDetails(t, m));
            } else {
                float hp = e instanceof net.minecraft.world.entity.LivingEntity le ? round(le.getHealth() / le.getMaxHealth()) : 1F;
                i = new Inhabitant(e.getUUID(), custom, e.getType().getDescriptionId(), false, false, "", TankInhabitants.isAdult(e),
                        1F, 1F, false, e instanceof Bucketable, BettaGenome.DEFAULT, BettaGenotype.homozygous(BettaGenome.DEFAULT), hp, 1F, 0,
                        false, 0F, false);
            }
            inh.add(i.save());
        }
        out.put("inhabitants", inh);
        out.putInt("adults", TankInhabitants.countAdults(live));
        out.putInt("hard_cap", TankInhabitants.hardCap(t.waterBlocks()));
        TankInhabitants.Stocking st = TankInhabitants.breakdown(live, t);
        out.putDouble("stock", st.total());
        out.putDouble("stock_adults", st.adults());
        out.putDouble("stock_young", st.young());
        out.putDouble("stock_critters", st.critters());
        out.putDouble("stock_nest", st.nest());
        out.putInt("young", live.size() - TankInhabitants.countAdults(live));
        out.put("critters", critters(level, controller).save());
        out.putBoolean("crowded", controller.isCrowded());

        ListTag lg = new ListTag();
        List<TankData.LogEntry> entries = t.log();
        for (int k = Math.max(0, entries.size() - LOG_LINES); k < entries.size(); k++) {
            TankData.LogEntry e = entries.get(k);
            CompoundTag c = new CompoundTag();
            c.putLong("t", e.time());
            c.putString("k", e.key());
            ListTag a = new ListTag();
            for (String s : e.args()) a.add(StringTag.valueOf(s));
            c.put("a", a);
            lg.add(c);
        }
        out.put("log", lg);
        CompoundTag breeding = TankBreeding.describe(level, controller);
        // M19: the tankmates' breeding, the breeding box
        breeding.put("tankmates", org.overengineer.bettacraft.tankmates.TankmateBreeding.describe(level, controller));
        breeding.putBoolean("box", t.breedingBox());
        breeding.putInt("box_fry", org.overengineer.bettacraft.tankmates.TankmateBreeding.boxFry(t));
        breeding.putInt("box_cap", org.overengineer.bettacraft.tankmates.TankmateBreeding.boxCapacity());
        out.put("breeding", breeding);
        out.put("interior", interior(level, t));
        CompoundTag eq = new CompoundTag();
        int blocks = 0, cab = 0, heaters = 0, cap = 0, coolers = 0;
        for (var f : org.overengineer.bettacraft.tank.TankEquipment.filters(level, controller)) {
            if (f instanceof org.overengineer.bettacraft.equipment.CabinetBlockEntity) cab++;
            else blocks++;
            heaters += f.heaterCount();
            cap += f.heaterBlocks();
            coolers += f.coolerCount();
        }
        eq.putInt("filter_coolers", coolers);
        eq.putInt("filter_blocks", blocks);
        eq.putInt("cabinet_filters", cab);
        eq.putInt("cabinets", org.overengineer.bettacraft.tank.TankEquipment.cabinets(level, controller).size());
        eq.putInt("filter_heaters", heaters);
        eq.putInt("filter_heat", cap);
        int legacy = 0;
        if (t.hasLegacyHeaters()) for (int i = 0; i < t.legacyHeaters().getContainerSize(); i++) if (!t.legacyHeaters().getItem(i).isEmpty()) legacy++;
        eq.putInt("legacy", legacy);
        out.put("equipment", eq);
        out.put("water_care", water(level, controller, live).save());
        out.put("plants", plants(level, controller).save());
        out.put("aquatic", aquatic(level, controller).save());
        return out;
    }

    /** M9: the water tab. */
    private static Water water(ServerLevel level, TankBlockEntity c, List<Entity> live) {
        TankData t = c.getTank();
        Water w = new Water();
        var p = org.overengineer.bettacraft.tank.TankWater.params();
        w.qualityEnabled = org.overengineer.bettacraft.tank.TankWater.qualityEnabled();
        w.evaporationEnabled = org.overengineer.bettacraft.tank.TankWater.evaporationEnabled();
        w.quality = t.waterQuality();
        w.badBelow = (float) p.badBelow();
        w.state = org.overengineer.bettacraft.tank.WaterQuality.state(t.waterQuality(), p).name().toLowerCase(java.util.Locale.ROOT);
        double animals = org.overengineer.bettacraft.tank.TankWater.animals(live, t);
        double blocks = t.waterBlocks();
        w.animalsPerBlock = blocks <= 0 ? 0 : animals / blocks;
        w.filterCapacity = org.overengineer.bettacraft.tank.TankWater.filterBlocks(level, c);
        w.filterShare = org.overengineer.bettacraft.tank.WaterQuality.filterShare(w.filterCapacity, blocks, p);
        w.declinePerDay = !w.qualityEnabled || t.waterMb() <= 0 ? 0
                : org.overengineer.bettacraft.tank.WaterQuality.wastePerDay(animals, blocks, p) * (1 - w.filterShare);
        w.internalFilters = org.overengineer.bettacraft.tank.TankWater.internalFilters(level, t);
        int[] top = org.overengineer.bettacraft.tank.TankWater.openTop(level, t);
        w.openTop = top[0];
        w.totalTop = top[1];
        w.evaporationPerDay = org.overengineer.bettacraft.tank.TankWater.evaporationPerDay(level, t);
        w.lastFreshWater = t.lastFreshWater();
        w.heaterPower = org.overengineer.bettacraft.tank.TankPower.heaterRequired();
        w.filterPower = org.overengineer.bettacraft.tank.TankPower.filterRequired();
        w.lightPower = org.overengineer.bettacraft.tank.TankPower.lightRequired();
        w.heatPowered = c.heatPowered();
        w.filterPowered = c.filterPowered();
        w.lightPowered = c.lightPowered();
        w.buffer = t.powerBuffer();
        w.bufferCap = org.overengineer.bettacraft.tank.TankPower.capacity();
        w.demand = c.lastDemand();
        // M20: salt water values and the reef technique
        w.salinity = t.salinity();
        w.calcium = t.calcium();
        w.skimmers = org.overengineer.bettacraft.reef.TankReef.skimmerCount(level, c);
        w.pumps = org.overengineer.bettacraft.reef.TankReef.pumps(level, t);
        w.liveRock = org.overengineer.bettacraft.reef.TankReef.liveRock(level, t);
        w.flow = org.overengineer.bettacraft.reef.TankReef.flow(level, c);
        w.spectrum = org.overengineer.bettacraft.reef.TankReef.spectrum(level, t);
        return w;
    }

    private static CompoundTag interior(ServerLevel level, TankData t) {
        CompoundTag c = new CompoundTag();
        c.putInt("scale", t.scale());
        c.putIntArray("size", new int[]{t.sizeX() * t.scale(), t.sizeY() * t.scale(), t.sizeZ() * t.scale()});
        c.putInt("limit", org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.INTERIOR_MAX_RENDERED_BLOCKS, 262144));
        var r = org.overengineer.bettacraft.tank.interior.InteriorRegions.get(level.getServer()).get(t.regionId());
        if (r == null || !r.built()) return c;
        c.putBoolean("built", true);
        c.putInt("blocks", org.overengineer.bettacraft.tank.interior.InteriorSync.syncedBlocks(r.id()));
        c.putBoolean("truncated", org.overengineer.bettacraft.tank.interior.InteriorSync.truncated(r.id()));
        var eq = org.overengineer.bettacraft.tank.interior.InteriorManager.equipment(level.getServer(), r);
        c.putInt("substrate", eq.substrateCount());
        c.putInt("heaters", eq.heaters().size());
        int total = 0;
        for (int v : eq.heaters().values()) total += v;
        int wet = eq.heaterCapacityBelow(TankCare.waterLocalHeight(t, r.scale()));
        c.putInt("heat_wet", wet);
        c.putInt("heat_dry", total - wet);
        c.putInt("filters", eq.filters().size());
        c.putInt("floating", eq.floatingPlants().size());
        c.putInt("coolers", eq.coolers().size());
        c.putInt("cool_wet", eq.coolerCapacityBelow(TankCare.waterLocalHeight(t, r.scale())));
        return c;
    }

    private static float round(float f) {
        return Math.round(f * 100) / 100.0F;
    }

    public static TankView read(CompoundTag t) {
        TankView v = new TankView();
        v.pos = BlockPos.of(t.getLong("pos"));
        v.name = t.getString("name");
        v.signVisible = t.getBoolean("sign");
        v.signPos = SignPosition.byName(t.getString("sign_pos"));
        Direction f = Direction.byName(t.getString("front"));
        v.front = f == null ? Direction.SOUTH : f;
        v.locked = t.getBoolean("locked");
        v.breakProtected = t.getBoolean("break_protected");
        v.protectionFeature = t.getBoolean("protection_feature");
        v.canModify = t.getBoolean("can_modify");
        v.isOwner = t.getBoolean("owner");
        v.ownerName = t.getString("owner_name");
        v.waterMb = t.getLong("water");
        v.capacityMb = t.getLong("cap");
        v.glass = t.getString("glass");
        v.style = org.overengineer.bettacraft.tank.TankStyle.byId(t.getString("style"));
        int[] s = t.getIntArray("size");
        if (s.length == 3) {
            v.sizeX = s[0];
            v.sizeY = s[1];
            v.sizeZ = s[2];
        }
        v.waterTemp = t.getFloat("temp");
        v.targetTemp = t.getFloat("target");
        v.ambient = t.getFloat("ambient");
        v.heaterBlocks = t.getDouble("heaters");
        v.coolerBlocks = t.getDouble("coolers");
        v.feederFood = t.getInt("feeder");
        v.feederPerDay = t.getInt("feeder_per_day");
        v.lights = t.getInt("lights");
        v.lightsLit = t.getInt("lights_lit");
        v.hasBucket = t.getBoolean("has_bucket");
        v.hasBag = t.getBoolean("has_bag");
        v.bagMb = t.getLong("bag_mb");
        v.minInsertMb = t.getLong("min_insert");
        ListTag inh = t.getList("inhabitants", Tag.TAG_COMPOUND);
        for (int i = 0; i < inh.size(); i++) v.inhabitants.add(Inhabitant.load(inh.getCompound(i)));
        v.adults = t.getInt("adults");
        v.hardCap = t.getInt("hard_cap");
        v.stock = t.getDouble("stock");
        v.stockAdults = t.getDouble("stock_adults");
        v.stockYoung = t.getDouble("stock_young");
        v.stockCritters = t.getDouble("stock_critters");
        v.stockNest = t.getDouble("stock_nest");
        v.young = t.getInt("young");
        v.critters.load(t.getCompound("critters"));
        v.crowded = t.getBoolean("crowded");
        ListTag lg = t.getList("log", Tag.TAG_COMPOUND);
        for (int i = 0; i < lg.size(); i++) {
            CompoundTag c = lg.getCompound(i);
            List<String> args = new ArrayList<>();
            ListTag a = c.getList("a", Tag.TAG_STRING);
            for (int k = 0; k < a.size(); k++) args.add(a.getString(k));
            v.log.add(new LogLine(c.getLong("t"), c.getString("k"), args));
        }
        CompoundTag in = t.getCompound("interior");
        v.scale = in.getInt("scale");
        int[] is = in.getIntArray("size");
        if (is.length == 3) {
            v.interiorX = is[0];
            v.interiorY = is[1];
            v.interiorZ = is[2];
        }
        v.renderLimit = in.getInt("limit");
        v.interiorBuilt = in.getBoolean("built");
        v.interiorBlocks = in.contains("blocks") ? in.getInt("blocks") : -1;
        v.interiorTruncated = in.getBoolean("truncated");
        v.substrate = in.getInt("substrate");
        v.heatersInside = in.getInt("heaters");
        v.heaterCapWet = in.getInt("heat_wet");
        v.heaterCapDry = in.getInt("heat_dry");
        v.filters = in.getInt("filters");
        v.floating = in.getInt("floating");
        v.coolersInside = in.getInt("coolers");
        v.coolerCapWet = in.getInt("cool_wet");
        CompoundTag eq = t.getCompound("equipment");
        v.filterBlocks = eq.getInt("filter_blocks");
        v.cabinetFilters = eq.getInt("cabinet_filters");
        v.cabinets = eq.getInt("cabinets");
        v.filterHeaters = eq.getInt("filter_heaters");
        v.filterHeaterBlocks = eq.getInt("filter_heat");
        v.legacyHeaters = eq.getInt("legacy");
        v.filterCoolers = eq.getInt("filter_coolers");
        v.water.load(t.getCompound("water_care"));
        v.plants.load(t.getCompound("plants"));
        v.aquatic.load(t.getCompound("aquatic"));
        v.breeding = t.getCompound("breeding");
        ListTag w = v.breeding.getList("warnings", Tag.TAG_STRING);
        for (int i = 0; i < w.size(); i++) v.warnings.add(w.getString(i));
        return v;
    }
}
