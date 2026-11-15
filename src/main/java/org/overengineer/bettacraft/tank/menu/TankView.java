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
                             CompoundTag tankmate, boolean analysed, boolean genesVisible) {
        /** M22.3: without the analysis state (animals without hidden genes). */
        public Inhabitant(UUID uuid, String customName, String typeKey, boolean betta, boolean female, String stage,
                          boolean adult, float health, float satiation, boolean sick, boolean bucketable, BettaGenome genome,
                          BettaGenotype genotype, float hp, float progress, int generation, boolean frog, float air, boolean noAir, CompoundTag tankmate) {
            this(uuid, customName, typeKey, betta, female, stage, adult, health, satiation, sick, bucketable, genome, genotype, hp, progress,
                    generation, frog, air, noAir, tankmate, true, true);
        }

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
            t.putBoolean("analysed", analysed);
            t.putBoolean("genes_visible", genesVisible);
            return t;
        }

        static Inhabitant load(CompoundTag t) {
            BettaGenome g = BettaGenome.CODEC.parse(NbtOps.INSTANCE, t.getCompound("genome")).result().orElse(BettaGenome.DEFAULT);
            return new Inhabitant(t.getUUID("id"), t.getString("name"), t.getString("type"), t.getBoolean("betta"), t.getBoolean("female"),
                    t.getString("stage"), t.getBoolean("adult"), t.getFloat("health"), t.getFloat("sat"), t.getBoolean("sick"),
                    t.getBoolean("bucketable"), g, BettaGenotype.load(t.getCompound("genotype")), t.getFloat("hp"), t.getFloat("progress"),
                    t.getInt("gen"), t.getBoolean("frog"), t.getFloat("air"), t.getBoolean("no_air"), t.getCompound("tm"),
                    !t.contains("analysed") || t.getBoolean("analysed"), !t.contains("genes_visible") || t.getBoolean("genes_visible"));
        }
    }

    public record LogLine(long time, String key, List<String> args) {}

    /** M19: what the animals tab shows of a tankmate. */
    /** M26 part b: the diet of a betta (in the details tag; it has no "species", so it is no tankmate). */
    static CompoundTag bettaDetails(BettaEntity b) {
        CompoundTag d = new CompoundTag();
        if (org.overengineer.bettacraft.config.CommonConfig.foodVariety()) d.put("diet", org.overengineer.bettacraft.food.FoodDiet.info(b.health()));
        return d;
    }

    static CompoundTag tankmateDetails(TankData t, org.overengineer.bettacraft.entity.tankmate.TankmateEntity m) {
        var s = m.species();
        CompoundTag d = new CompoundTag();
        if (org.overengineer.bettacraft.config.CommonConfig.foodVariety()) d.put("diet", org.overengineer.bettacraft.food.FoodDiet.info(m.health())); // M26 part b
        d.putString("species", s.id());
        d.putString("variety", m.variety());
        boolean known = org.overengineer.bettacraft.genetics.GeneVisibility.visible(m.analysed()); // M22.3: the alleles only once analysed
        d.putString("genes", known ? m.genes().text() : "");
        // M21.6: the guppy and endler genome (with features.livebearerGenetics); M22.3: the whole genome only once analysed
        var lb = m.livebearerGenome();
        if (lb != null) {
            d.put("lb", lb.save());
            if (!known) d.putBoolean("lb_hidden", true);
        }
        d.putInt("shoal", m.shoalSize());
        d.putInt("stress", m.stressBits());
        d.putBoolean("guard", m.isGuarding());
        d.putBoolean("cave", m.cave() != null);
        d.putBoolean("hitched", m.isHitched()); // M21.8
        d.putFloat("gravid", m.isGravid() ? (float) Math.min(1.0, m.gravidDays() / Math.max(1e-6,
                org.overengineer.bettacraft.tankmates.TankmateBreeding.gestationDays(s))) : -1F);
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
    /** M22.3: the player carries a magnifier; the hidden genes need it at all. */
    public boolean hasMagnifier, genesFree;
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
    /** M23: oxygen, aeration and the lights (tab Devices). */
    public final Tech tech = new Tech();
    /** M31: terrarium / paludarium climate (the climate tab, only shown for those modes). */
    public final Climate climate = new Climate();
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

    /**
     * M22.3: one small animal of the tank for the single-animal list: its id (to take exactly this one out), kind, sex,
     * age, colour density and genes (empty while they are not analysed).
     */
    public record CritterInfo(int id, int species, String variety, boolean female, boolean adult, float ageDays, float grade, String genes,
                              boolean analysed, boolean berried, String lookKey, int rgb) {
        public String key() {
            return org.overengineer.bettacraft.critters.CritterSpecies.values()[species].id() + ":" + variety;
        }

        CompoundTag save() {
            CompoundTag c = new CompoundTag();
            c.putIntArray("n", new int[]{id, species, rgb});
            c.putString("v", variety);
            c.putByte("f", (byte) ((female ? 1 : 0) | (adult ? 2 : 0) | (analysed ? 4 : 0) | (berried ? 8 : 0)));
            c.putFloat("age", ageDays);
            c.putFloat("g", grade);
            if (!genes.isEmpty()) c.putString("genes", genes);
            if (!lookKey.isEmpty()) c.putString("look", lookKey);
            return c;
        }

        static CritterInfo load(CompoundTag c) {
            int[] n = c.getIntArray("n");
            if (n.length != 3 || n[1] < 0 || n[1] >= org.overengineer.bettacraft.critters.CritterSpecies.values().length) return null;
            byte f = c.getByte("f");
            return new CritterInfo(n[0], n[1], c.getString("v"), (f & 1) != 0, (f & 2) != 0, c.getFloat("age"), c.getFloat("g"), c.getString("genes"),
                    (f & 4) != 0, (f & 8) != 0, c.getString("look"), n[2]);
        }
    }

    /** M14: the small animals for the menu. */
    public static final class Critters {
        public final List<CritterGroup> groups = new ArrayList<>();
        /** M22.3: every living animal (with {@code features.animalList}), sorted by kind and grade. */
        public final List<CritterInfo> animals = new ArrayList<>();
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
            ListTag a = new ListTag();
            for (CritterInfo k : animals) a.add(k.save());
            t.put("animals", a);
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
            ListTag a = t.getList("animals", Tag.TAG_COMPOUND);
            for (int i = 0; i < a.size(); i++) {
                CritterInfo k = CritterInfo.load(a.getCompound(i));
                if (k != null) animals.add(k);
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
            if (org.overengineer.bettacraft.config.CommonConfig.animalList()) {
                // M22.3: every animal of the kind, the best first
                List<org.overengineer.bettacraft.critters.Critter> each = new ArrayList<>(e);
                each.sort(java.util.Comparator.comparing((org.overengineer.bettacraft.critters.Critter k) -> !k.adult())
                        .thenComparing(k -> -k.genes.grade()).thenComparingInt(k -> k.id));
                for (var k : each) {
                    boolean known = org.overengineer.bettacraft.genetics.GeneVisibility.visible(k.analysed);
                    String look = org.overengineer.bettacraft.critters.CritterLooks.nameKey(k.species, k.look);
                    out.animals.add(new CritterInfo(k.id, k.species.ordinal(), k.genes.variety(k.species), k.female && k.species.hasSexes(), k.adult(),
                            (float) k.age, k.genes.grade(), known && k.species.loci().length > 0 ? k.genes.genotypeText(k.species) : "", k.analysed,
                            k.berried(), look == null ? "" : look, k.genes.rgb(k.species)));
                }
            }
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
            else if (org.overengineer.bettacraft.plants.InteriorPlants.lightFactor(env.effectiveLight(), org.overengineer.bettacraft.plants.PlantSpecies.Light.need(p.species.light())) <= 0) a.dark++;
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
    /** M23: oxygen and aeration, the lights with dimmer and timer, the filters without intake sponge (tab Devices). */
    public static final class Tech {
        public boolean oxygenOn, aerationOn, timerFeature, tiersOn, fry;
        public float oxygen = Float.NaN;
        public double saturation, target, exchange, use, co2Strip;
        public int pumps, pumpsRunning, stones, spongeFilters, outlets, driven;
        public boolean airPowered = true, airPower;
        public double light, ledLight, factor = 1;
        public int bars, panels, highPower, lit;
        public int dimmer = 100, lightOn = 480, lightOff = 1200;
        public boolean timer;
        public int unprotected;

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putBoolean("o2_on", oxygenOn);
            t.putBoolean("air_on", aerationOn);
            t.putBoolean("timer_feature", timerFeature);
            t.putBoolean("tiers_on", tiersOn);
            t.putBoolean("fry", fry);
            if (!Float.isNaN(oxygen)) t.putFloat("o2", oxygen);
            t.putDouble("sat", saturation);
            t.putDouble("target", target);
            t.putDouble("exchange", exchange);
            t.putDouble("use", use);
            t.putDouble("co2", co2Strip);
            t.putIntArray("air", new int[]{pumps, pumpsRunning, stones, spongeFilters, outlets, driven});
            t.putBoolean("air_ok", airPowered);
            t.putBoolean("air_power", airPower);
            t.putDouble("light", light);
            t.putDouble("led", ledLight);
            t.putDouble("factor", factor);
            t.putIntArray("lights", new int[]{bars, panels, highPower, lit});
            t.putIntArray("timer_set", new int[]{dimmer, lightOn, lightOff});
            t.putBoolean("timer", timer);
            t.putInt("unprotected", unprotected);
            return t;
        }

        void load(CompoundTag t) {
            oxygenOn = t.getBoolean("o2_on");
            aerationOn = t.getBoolean("air_on");
            timerFeature = t.getBoolean("timer_feature");
            tiersOn = t.getBoolean("tiers_on");
            fry = t.getBoolean("fry");
            oxygen = t.contains("o2") ? t.getFloat("o2") : Float.NaN;
            saturation = t.getDouble("sat");
            target = t.getDouble("target");
            exchange = t.getDouble("exchange");
            use = t.getDouble("use");
            co2Strip = t.getDouble("co2");
            int[] a = t.getIntArray("air");
            if (a.length == 6) {
                pumps = a[0]; pumpsRunning = a[1]; stones = a[2]; spongeFilters = a[3]; outlets = a[4]; driven = a[5];
            }
            airPowered = t.getBoolean("air_ok");
            airPower = t.getBoolean("air_power");
            light = t.getDouble("light");
            ledLight = t.getDouble("led");
            factor = t.getDouble("factor");
            int[] l = t.getIntArray("lights");
            if (l.length == 4) {
                bars = l[0]; panels = l[1]; highPower = l[2]; lit = l[3];
            }
            int[] ts = t.getIntArray("timer_set");
            if (ts.length == 3) {
                dimmer = ts[0]; lightOn = ts[1]; lightOff = ts[2];
            }
            timer = t.getBoolean("timer");
            unprotected = t.getInt("unprotected");
        }
    }

    /** M31: the climate tab: mode, land, the values, the devices and the problems of the terrarium animals. */
    public static final class Climate {
        public int mode;
        public float land = -1, air = Float.NaN, ground = Float.NaN, bask = Float.NaN, humidity = Float.NaN, uv, duty, ambient, roomHumidity, target;
        public int matsUnder, matsWall, lamps, lampsLit, misters, misting, mistWater, mistSetting = -1, openTops, tops;
        public boolean devicesOn = true, heatPowered = true, misterPowered = true;
        /** "type key|problem key" of every animal kind with a problem. */
        public final List<String> problems = new ArrayList<>();
        /** M31 part b: the turtle clutches: "species key|eggs|progress %|nest °C|sex class (open, females, males, mixed)". */
        public final List<String> eggs = new ArrayList<>();
        public int animals;

        public org.overengineer.bettacraft.terrarium.TerrariumMode mode() {
            return org.overengineer.bettacraft.terrarium.TerrariumMode.byOrdinal(mode);
        }

        public boolean hasLand() {
            return mode().hasLand();
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putInt("mode", mode);
            t.putFloat("land", land);
            t.putFloat("air", air);
            t.putFloat("ground", ground);
            t.putFloat("bask", bask);
            t.putFloat("rh", humidity);
            t.putFloat("uv", uv);
            t.putFloat("duty", duty);
            t.putFloat("ambient", ambient);
            t.putFloat("room_rh", roomHumidity);
            t.putFloat("target", target);
            t.putIntArray("dev", new int[]{matsUnder, matsWall, lamps, lampsLit, misters, misting, mistWater, mistSetting, openTops, tops, animals});
            t.putBoolean("dev_on", devicesOn);
            t.putBoolean("heat_ok", heatPowered);
            t.putBoolean("mist_ok", misterPowered);
            ListTag p = new ListTag();
            for (String s : problems) p.add(StringTag.valueOf(s));
            t.put("problems", p);
            ListTag e = new ListTag();
            for (String s : eggs) e.add(StringTag.valueOf(s));
            t.put("eggs", e);
            return t;
        }

        void load(CompoundTag t) {
            mode = t.getInt("mode");
            land = t.contains("land") ? t.getFloat("land") : -1;
            air = t.contains("air") ? t.getFloat("air") : Float.NaN;
            ground = t.contains("ground") ? t.getFloat("ground") : Float.NaN;
            bask = t.contains("bask") ? t.getFloat("bask") : Float.NaN;
            humidity = t.contains("rh") ? t.getFloat("rh") : Float.NaN;
            uv = t.getFloat("uv");
            duty = t.getFloat("duty");
            ambient = t.getFloat("ambient");
            roomHumidity = t.getFloat("room_rh");
            target = t.getFloat("target");
            int[] d = t.getIntArray("dev");
            if (d.length == 11) {
                matsUnder = d[0]; matsWall = d[1]; lamps = d[2]; lampsLit = d[3]; misters = d[4]; misting = d[5]; mistWater = d[6]; mistSetting = d[7];
                openTops = d[8]; tops = d[9]; animals = d[10];
            }
            devicesOn = !t.contains("dev_on") || t.getBoolean("dev_on");
            heatPowered = !t.contains("heat_ok") || t.getBoolean("heat_ok");
            misterPowered = !t.contains("mist_ok") || t.getBoolean("mist_ok");
            problems.clear();
            ListTag p = t.getList("problems", Tag.TAG_STRING);
            for (int i = 0; i < p.size(); i++) problems.add(p.getString(i));
            eggs.clear();
            ListTag e = t.getList("eggs", Tag.TAG_STRING);
            for (int i = 0; i < e.size(); i++) eggs.add(e.getString(i));
        }
    }

    private static Climate climate(ServerLevel level, TankBlockEntity c, List<Entity> live) {
        TankData t = c.getTank();
        Climate x = new Climate();
        x.mode = t.terrariumMode().ordinal();
        x.land = t.landShare();
        x.air = t.airTemp();
        x.ground = t.groundTemp();
        x.bask = t.baskTemp();
        x.humidity = t.humidity();
        x.uv = t.uv();
        x.duty = t.matDuty();
        x.ambient = TankCare.ambient(level, t);
        x.target = t.targetTemp();
        if (!t.terrariumMode().hasLand()) return x;
        x.roomHumidity = org.overengineer.bettacraft.terrarium.TankClimate.roomHumidity(level, t);
        x.devicesOn = org.overengineer.bettacraft.config.CommonConfig.terrariumDevices();
        x.heatPowered = c.heatPowered();
        x.misterPowered = c.misterPowered();
        for (BlockPos m : org.overengineer.bettacraft.tank.TankEquipment.heatMats(level, c)) {
            if (org.overengineer.bettacraft.terrarium.HeatMatBlock.underFloor(level.getBlockState(m))) x.matsUnder++;
            else x.matsWall++;
        }
        for (BlockPos top : t.openTops()) {
            if (!level.isLoaded(top)) continue;
            var s = level.getBlockState(top);
            if (s.getBlock() instanceof org.overengineer.bettacraft.terrarium.BaskingLampBlock) {
                x.lamps++;
                if (s.getValue(org.overengineer.bettacraft.equipment.TankLightBlock.LIT)) x.lampsLit++;
            }
        }
        for (var m : org.overengineer.bettacraft.tank.TankEquipment.misters(level, c)) {
            x.misters++;
            if (m.spraying(level.getGameTime())) x.misting++;
            x.mistWater += m.amount();
            x.mistSetting = x.mistSetting < 0 ? m.setting() : Math.min(x.mistSetting, m.setting());
        }
        int[] top = org.overengineer.bettacraft.tank.TankWater.openTop(level, t);
        x.openTops = top[0];
        x.tops = top[1];
        var cond = org.overengineer.bettacraft.terrarium.TankClimate.conditions(t);
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        for (Entity e : live) {
            var n = org.overengineer.bettacraft.terrarium.TerrariumAnimals.needs(e);
            if (n == null) continue;
            x.animals++;
            var a = n.assess(cond);
            if (e instanceof org.overengineer.bettacraft.terrarium.TerrariumAnimal ta) a = ta.adjust(a); // M31 part b: brumation
            if (e instanceof org.overengineer.bettacraft.terrarium.HerpEntity te && te.isBrumating()) seen.add(e.getType().getDescriptionId() + "|gui.bettacraft.terrarium.brumating");
            for (var p : a.problems()) seen.add(e.getType().getDescriptionId() + "|" + p.translationKey());
        }
        x.problems.addAll(seen);
        // M31 part b: the clutches in the land
        double nest = org.overengineer.bettacraft.terrarium.HerpBreeding.nestTemp(t);
        for (var k : t.herpClutches()) {
            double f = k.femaleShare();
            String sex = k.tspDays <= 1e-9 ? "open" : f > 0.8 ? "females" : f < 0.2 ? "males" : "mixed";
            x.eggs.add(k.species.translationKey() + "|" + k.eggs + "|" + Math.round(k.progress * 100) + "|"
                    + org.overengineer.bettacraft.terrarium.TankClimate.f1((float) nest) + "|" + sex);
        }
        return x;
    }

    private static Tech tech(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        Tech x = new Tech();
        x.oxygenOn = org.overengineer.bettacraft.tank.TankAeration.enabled();
        x.aerationOn = org.overengineer.bettacraft.config.CommonConfig.aeration();
        x.timerFeature = org.overengineer.bettacraft.config.CommonConfig.lightTimer();
        x.tiersOn = org.overengineer.bettacraft.config.CommonConfig.ledTiers();
        x.fry = org.overengineer.bettacraft.config.CommonConfig.fryIntake();
        x.oxygen = t.oxygen();
        x.saturation = org.overengineer.bettacraft.tank.TankAeration.saturation(t.waterTemp(), t.salinity());
        x.target = c.o2Target;
        x.exchange = c.o2Exchange;
        x.use = c.o2Use;
        x.co2Strip = org.overengineer.bettacraft.tank.TankAeration.co2Stripping(c.o2Exchange);
        var pumps = org.overengineer.bettacraft.tank.TankEquipment.airPumps(level, c);
        x.pumps = pumps.size();
        for (var p : pumps) if (p.airRunning()) x.pumpsRunning++;
        var r = org.overengineer.bettacraft.tank.interior.InteriorRegions.get(level.getServer()).get(t.regionId());
        if (r != null && r.built()) {
            var eq = org.overengineer.bettacraft.tank.interior.InteriorManager.equipment(level.getServer(), r);
            x.stones = eq.airStones().size();
            x.spongeFilters = eq.spongeFilters().size();
            double y = org.overengineer.bettacraft.tank.TankCare.waterLocalHeight(t, r.scale());
            for (long l : eq.filters()) if (net.minecraft.core.BlockPos.getY(l) < y && !eq.hasIntakeSponge(l)) x.unprotected++;
            for (long l : eq.corners()) if (net.minecraft.core.BlockPos.getY(l) < y && !eq.hasIntakeSponge(l)) x.unprotected++;
        }
        for (var f : org.overengineer.bettacraft.tank.TankEquipment.filters(level, c)) if (!f.intakeSponge()) x.unprotected++;
        x.outlets = c.airOutlets;
        x.driven = c.airDriven;
        x.airPowered = c.airPowered();
        x.airPower = org.overengineer.bettacraft.tank.TankPower.airRequired();
        x.light = org.overengineer.bettacraft.tank.plants.TankPlants.strongLight(level, c);
        x.ledLight = org.overengineer.bettacraft.tank.TankLighting.ledLight(level, c);
        for (var p : org.overengineer.bettacraft.tank.TankCare.aboveTop(t)) {
            if (!level.isLoaded(p)) continue;
            var s = level.getBlockState(p);
            if (!(s.getBlock() instanceof org.overengineer.bettacraft.equipment.TankLightBlock)) continue;
            if (s.getBlock() instanceof org.overengineer.bettacraft.equipment.LedPanelBlock) x.panels++;
            else if (s.getBlock() instanceof org.overengineer.bettacraft.equipment.HighPowerLedBlock) x.highPower++;
            else x.bars++;
            if (s.getValue(org.overengineer.bettacraft.equipment.TankLightBlock.LIT)) x.lit++;
        }
        x.dimmer = t.lightDimmer();
        x.lightOn = t.lightOn();
        x.lightOff = t.lightOff();
        x.timer = t.lightTimer();
        x.factor = org.overengineer.bettacraft.tank.TankLighting.factor(t, level.getDayTime());
        return x;
    }

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
        /** M25 part c: alkalinity (dKH), magnesium (mg/L), trace elements (0..1); dosingOn = features.reefDosing. */
        public float kh, magnesium, trace;
        public boolean dosingOn;
        public int skimmers, pumps, liveRock;
        public double flow, spectrum;
        /** M24: nitrate (mg/L), its change per day, the levels and the water change interval of the difficulty (0 = off). */
        public float nitrate;
        public double nitrateRate, nitrateStress = 50, nitrateLethal = 120;
        public int nitrateInterval;
        /**
         * M25: water chemistry (feature on), TDS, pH and the pH it drifts to, the hardness, what drives the pH (shares 0..1:
         * soil, limestone, peat, almond leaves, driftwood), the CO2 and the animals the water does not suit
         * ("translation key|bad" or "|stress").
         */
        public boolean chemOn;
        public float tds = 250F, ph = 7.5F, co2 = 3F;
        public double phTarget = 7.5, acid, alkaline, peat, tannin, wood;
        public String hardness = "medium";
        public List<String> chemOff = new ArrayList<>();
        /** M25 part b: CO2 dosing on, bubbles per second of the sources, their number, the CO2 the water moves to, its state. */
        public boolean co2On;
        public double co2Bubbles, co2Target = 3;
        public int co2Sources;
        public String co2State = "low";

        public boolean reefLine() {
            return salinity >= org.overengineer.bettacraft.tank.Salinity.BRACKISH_FROM || skimmers > 0 || pumps > 0 || liveRock > 0;
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putFloat("salinity", salinity);
            t.putFloat("calcium", calcium);
            t.putFloat("kh", kh);
            t.putFloat("magnesium", magnesium);
            t.putFloat("trace", trace);
            t.putBoolean("dosing_on", dosingOn);
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
            t.putFloat("no3", nitrate);
            t.putDouble("no3_rate", nitrateRate);
            t.putDouble("no3_stress", nitrateStress);
            t.putDouble("no3_lethal", nitrateLethal);
            t.putInt("no3_interval", nitrateInterval);
            // M25
            t.putBoolean("chem_on", chemOn);
            t.putFloat("tds", tds);
            t.putFloat("ph", ph);
            t.putFloat("co2", co2);
            t.putDouble("ph_target", phTarget);
            t.putString("hardness", hardness);
            t.putDouble("acid", acid);
            t.putDouble("alkaline", alkaline);
            t.putDouble("peat", peat);
            t.putDouble("tannin", tannin);
            t.putDouble("wood", wood);
            net.minecraft.nbt.ListTag off = new net.minecraft.nbt.ListTag();
            for (String s : chemOff) off.add(net.minecraft.nbt.StringTag.valueOf(s));
            t.put("chem_off", off);
            t.putBoolean("co2_on", co2On);
            t.putDouble("co2_bubbles", co2Bubbles);
            t.putDouble("co2_target", co2Target);
            t.putInt("co2_sources", co2Sources);
            t.putString("co2_state", co2State);
            return t;
        }

        void load(CompoundTag t) {
            nitrate = t.getFloat("no3");
            nitrateRate = t.getDouble("no3_rate");
            nitrateStress = t.getDouble("no3_stress");
            nitrateLethal = t.getDouble("no3_lethal");
            nitrateInterval = t.getInt("no3_interval");
            chemOn = t.getBoolean("chem_on"); // M25
            tds = t.getFloat("tds");
            ph = t.getFloat("ph");
            co2 = t.getFloat("co2");
            phTarget = t.getDouble("ph_target");
            hardness = t.getString("hardness");
            acid = t.getDouble("acid");
            alkaline = t.getDouble("alkaline");
            peat = t.getDouble("peat");
            tannin = t.getDouble("tannin");
            wood = t.getDouble("wood");
            chemOff = new ArrayList<>();
            for (net.minecraft.nbt.Tag x : t.getList("chem_off", net.minecraft.nbt.Tag.TAG_STRING)) chemOff.add(x.getAsString());
            co2On = t.getBoolean("co2_on");
            co2Bubbles = t.getDouble("co2_bubbles");
            co2Target = t.getDouble("co2_target");
            co2Sources = t.getInt("co2_sources");
            co2State = t.getString("co2_state");
            salinity = t.getFloat("salinity");
            calcium = t.getFloat("calcium");
            kh = t.getFloat("kh");
            magnesium = t.getFloat("magnesium");
            trace = t.getFloat("trace");
            dosingOn = t.getBoolean("dosing_on");
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
        // M22.3: also a bucket of water, of the same species or of small animals (the server picks the fitting one)
        boolean anyBucket = TankInhabitants.findContainer(player, false) >= 0;
        for (var st : player.getInventory().items) {
            if (org.overengineer.bettacraft.containers.AnimalContainers.waterBucket(st) || st.getItem() instanceof org.overengineer.bettacraft.tankmates.TankmateBucketItem
                    || st.getItem() instanceof org.overengineer.bettacraft.containers.CritterBucketItem) anyBucket = true;
        }
        out.putBoolean("has_bucket", anyBucket);
        out.putBoolean("has_magnifier", !org.overengineer.bettacraft.genetics.MagnifierItem.find(player).isEmpty());
        out.putBoolean("genes_free", org.overengineer.bettacraft.genetics.GeneVisibility.free());
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
                        round(h.healthFraction(p)), round(h.satiation(p)), h.isSick(), true, b.genome(),
                        org.overengineer.bettacraft.genetics.GeneVisibility.visible(b.getBettaData().analysed()) ? b.genotype() : BettaGenotype.homozygous(b.genome()),
                        round(b.getHealth() / b.getMaxHealth()), round((float) b.stageProgress()), b.getBettaData().generation(), false, 0F, false,
                        bettaDetails(b), b.getBettaData().analysed(), org.overengineer.bettacraft.genetics.GeneVisibility.visible(b.getBettaData().analysed()));
            } else if (e instanceof org.overengineer.bettacraft.entity.frog.DwarfFrogEntity f) {
                // M14: frogs have needs (food, air at the surface)
                float hp = round(f.getHealth() / f.getMaxHealth());
                i = new Inhabitant(e.getUUID(), custom, e.getType().getDescriptionId(), false, false, "", true, hp, round(f.satiation()),
                        f.daysSinceFed() > org.overengineer.bettacraft.entity.frog.DwarfFrogEntity.STARVE_DAYS, true, BettaGenome.DEFAULT,
                        BettaGenotype.homozygous(BettaGenome.DEFAULT), hp, 1F, 0, true,
                        round(f.breath() / (float) org.overengineer.bettacraft.entity.frog.DwarfFrogEntity.maxBreath()), !f.airAvailable());
            } else if (e instanceof org.overengineer.bettacraft.terrarium.TerrariumAnimalEntity ta) {
                // M31: terrarium animals: hunger and the stress from their needs
                float hp = round(ta.getHealth() / ta.getMaxHealth());
                i = new Inhabitant(e.getUUID(), custom, e.getType().getDescriptionId(), false, false, "", !ta.isBaby(), round(1F - ta.stress()), round(ta.satiation()),
                        ta.stress() >= 0.5F, ta instanceof Bucketable, BettaGenome.DEFAULT, BettaGenotype.homozygous(BettaGenome.DEFAULT), hp, 1F, 0, false, 0F, false);
            } else if (e instanceof org.overengineer.bettacraft.entity.tankmate.TankmateEntity m) {
                // M19: tankmates have needs like the bettas, plus their shoal, temperature range and breeding state
                BettaHealth h = m.health();
                i = new Inhabitant(e.getUUID(), custom, e.getType().getDescriptionId(), false, m.isFemale(), m.lifeStage().id(), m.isAdult(),
                        round(h.healthFraction(p)), round(h.satiation(p)), h.isSick(), true, BettaGenome.DEFAULT,
                        BettaGenotype.homozygous(BettaGenome.DEFAULT), round(m.getHealth() / m.getMaxHealth()), round((float) m.stageProgress()),
                        m.generation(), false, 0F, false, tankmateDetails(t, m), m.analysed(), org.overengineer.bettacraft.genetics.GeneVisibility.visible(m.analysed()));
            } else {
                float hp = e instanceof net.minecraft.world.entity.LivingEntity le ? round(le.getHealth() / le.getMaxHealth()) : 1F;
                float[] fv = org.overengineer.bettacraft.foreign.ForeignCare.menuValues(e); // M32 part a: our needs by its profile
                i = new Inhabitant(e.getUUID(), custom, e.getType().getDescriptionId(), false, false, "", TankInhabitants.isAdult(e),
                        fv != null ? round(fv[0]) : 1F, fv != null ? round(fv[1]) : 1F, fv != null && fv[2] > 0, e instanceof Bucketable, BettaGenome.DEFAULT, BettaGenotype.homozygous(BettaGenome.DEFAULT), hp, 1F, 0,
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
        eq.putInt("filter_blocks", blocks + (int) org.overengineer.bettacraft.filter.MultiblockFilter.serving(level, controller)[0]); // M24: multiblock filters count as outside
        eq.putInt("cabinet_filters", cab);
        eq.putInt("cabinets", org.overengineer.bettacraft.tank.TankEquipment.cabinets(level, controller).size());
        eq.putInt("filter_heaters", heaters);
        eq.putInt("filter_heat", cap);
        int legacy = 0;
        if (t.hasLegacyHeaters()) for (int i = 0; i < t.legacyHeaters().getContainerSize(); i++) if (!t.legacyHeaters().getItem(i).isEmpty()) legacy++;
        eq.putInt("legacy", legacy);
        out.put("equipment", eq);
        out.put("water_care", water(level, controller, live).save());
        out.put("tech", tech(level, controller).save());
        out.put("climate", climate(level, controller, live).save()); // M31
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
        w.kh = t.kh(); // M25 part c
        w.magnesium = t.magnesium();
        w.trace = t.trace();
        w.dosingOn = org.overengineer.bettacraft.reef.ReefDosing.enabled();
        w.skimmers = org.overengineer.bettacraft.reef.TankReef.skimmerCount(level, c);
        w.pumps = org.overengineer.bettacraft.reef.TankReef.pumps(level, t);
        w.liveRock = org.overengineer.bettacraft.reef.TankReef.liveRock(level, t);
        w.flow = org.overengineer.bettacraft.reef.TankReef.flow(level, c);
        w.spectrum = org.overengineer.bettacraft.reef.TankReef.spectrum(level, t);
        // M24: nitrate and the water changes
        w.nitrate = t.nitrate();
        w.nitrateRate = org.overengineer.bettacraft.tank.TankNitrate.ratePerDay(level, c);
        w.nitrateStress = org.overengineer.bettacraft.tank.TankNitrate.stressFrom();
        w.nitrateLethal = org.overengineer.bettacraft.tank.TankNitrate.lethalFrom();
        w.nitrateInterval = org.overengineer.bettacraft.tank.TankNitrate.intervalDays(org.overengineer.bettacraft.tank.TankNitrate.difficulty());
        chemistry(level, c, live, w); // M25
        return w;
    }

    /** M25: the water chemistry part of the water tab. */
    private static void chemistry(ServerLevel level, TankBlockEntity c, List<Entity> live, Water w) {
        TankData t = c.getTank();
        w.chemOn = org.overengineer.bettacraft.chem.TankChemistry.enabled();
        w.tds = t.tds();
        w.ph = t.ph();
        w.co2 = t.co2();
        // M25 part b: CO2 dosing
        w.co2On = org.overengineer.bettacraft.co2.TankCo2.enabled();
        w.co2Bubbles = c.co2Bubbles;
        w.co2Target = c.co2Target;
        w.co2Sources = org.overengineer.bettacraft.tank.TankEquipment.co2Sources(level, c).size();
        w.co2State = org.overengineer.bettacraft.co2.TankCo2.state(t.co2()).name().toLowerCase(java.util.Locale.ROOT);
        if (!w.chemOn) return;
        var d = org.overengineer.bettacraft.chem.TankChemistry.drivers(level, c);
        w.phTarget = org.overengineer.bettacraft.chem.TankChemistry.phTarget(t, d);
        w.hardness = org.overengineer.bettacraft.chem.TankChemistry.hardness(t, d).name().toLowerCase(java.util.Locale.ROOT);
        w.acid = d.acid();
        w.alkaline = d.alkaline();
        w.peat = d.peat();
        w.tannin = d.tannin();
        w.wood = d.wood();
        java.util.Map<String, Boolean> off = new java.util.LinkedHashMap<>();
        for (Entity e : live) {
            var r = org.overengineer.bettacraft.chem.ChemTolerance.of(e);
            var fit = org.overengineer.bettacraft.chem.TankChemistry.fit(t, r);
            if (fit == org.overengineer.bettacraft.chem.ChemTolerance.Fit.OK) continue;
            String key = e instanceof org.overengineer.bettacraft.entity.tankmate.TankmateEntity m ? m.species().translationKey() : e.getType().getDescriptionId();
            off.merge(key, fit == org.overengineer.bettacraft.chem.ChemTolerance.Fit.BAD, Boolean::logicalOr);
        }
        for (var k : t.critters()) {
            if (!k.alive()) continue;
            var fit = org.overengineer.bettacraft.chem.TankChemistry.fit(t, org.overengineer.bettacraft.chem.ChemTolerance.critter(k.species));
            if (fit != org.overengineer.bettacraft.chem.ChemTolerance.Fit.OK) off.merge(k.species.translationKey(), fit == org.overengineer.bettacraft.chem.ChemTolerance.Fit.BAD, Boolean::logicalOr);
        }
        for (var e : off.entrySet()) w.chemOff.add(e.getKey() + (e.getValue() ? "|bad" : "|stress"));
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
        v.hasMagnifier = t.getBoolean("has_magnifier");
        v.genesFree = t.getBoolean("genes_free");
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
        v.tech.load(t.getCompound("tech"));
        v.climate.load(t.getCompound("climate")); // M31
        v.plants.load(t.getCompound("plants"));
        v.aquatic.load(t.getCompound("aquatic"));
        v.breeding = t.getCompound("breeding");
        ListTag w = v.breeding.getList("warnings", Tag.TAG_STRING);
        for (int i = 0; i < w.size(); i++) v.warnings.add(w.getString(i));
        return v;
    }
}
