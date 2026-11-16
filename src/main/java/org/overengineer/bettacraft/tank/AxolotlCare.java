package org.overengineer.bettacraft.tank;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterObject;
import org.overengineer.bettacraft.tankmates.TankmateSpots;

/**
 * M29 part d: our care and breeding for the vanilla axolotls in a tank (no entity of our own: the vanilla model, its
 * bucket and other mods stay as they are).
 * <ul>
 *     <li><b>Cool water:</b> 15–20 °C is right (a cooler in most biomes). From 21 °C the axolotl is stressed and does not
 *     breed, above 23 °C it loses health ({@code invertebrates.axolotlHeatDamage} per degree and day).</li>
 *     <li><b>Breeding:</b> two adults in cool water with plants lay their eggs on the plants (a clutch of
 *     {@link CritterObject.Kind#AXOLOTL_EGGS}); after {@code invertebrates.axolotlEggDays} the larvae hatch as baby
 *     axolotls. Without plants there are no eggs.</li>
 *     <li><b>Colours:</b> the five vanilla colours are recessive alleles at their own loci against the wild colour (like
 *     the shrimp colours, {@link CritterGenes}): leucistic (Lucy), gold, cyan and blue. An axolotl shows a colour only
 *     with two alleles; with several, the rarer wins (blue over cyan over gold over Lucy). Blue stays rare: a wild axolotl
 *     carries it now and then, hidden. The genes live in the entity's persistent data; an axolotl without them (caught
 *     in the world) gets them from its colour.</li>
 * </ul>
 */
public final class AxolotlCare {
    /** Loci of the axolotl colours (bit i of {@link CritterGenes}). */
    public static final int LUCY = 0, GOLD = 1, CYAN = 2, BLUE = 3, LOCI = 4;
    public static final String[] LOCUS_NAMES = {"lucy", "gold", "cyan", "blue"};
    public static final float MIN_TEMP = 15, MAX_TEMP = 20, STRESS_FROM = 21, HARM_FROM = 23;
    /** Chance of a wild axolotl to carry one hidden allele: blue (rare), the other colours. */
    public static final double BLUE_CARRIER = 0.06, COLOR_CARRIER = 0.12;
    /** Chance per day and pair to lay eggs in good conditions. */
    public static final double LAY_RATE = 0.5;
    /** Days an axolotl needs after laying before the next clutch. */
    public static final double REST_DAYS = 2.0;
    public static final String GENES = "bettacraft:axolotl_genes", HEAT = "bettacraft:axolotl_heat", REST = "bettacraft:axolotl_rest",
            GENERATION = "bettacraft:axolotl_gen", ANALYSED = "bettacraft:axolotl_analysed";
    /** Key of the genes in the bucket data of an axolotl taken out of a tank. */
    public static final String BUCKET_KEY = "BettacraftGenes";
    /** M42: the generation and the magnifier's analysis in the bucket data (they were lost: every bucketed axolotl began a new pedigree). */
    public static final String BUCKET_GENERATION = "BettacraftGeneration", BUCKET_ANALYSED = "BettacraftAnalysed";
    private static final long LOG_TICKS = 6000;

    private AxolotlCare() {}

    // ------------------------------------------------------------------ genetics (pure)

    /** The colour an axolotl with these genes shows. */
    public static Axolotl.Variant variant(CritterGenes g) {
        int hom = g.homozygous();
        if ((hom & (1 << BLUE)) != 0) return Axolotl.Variant.BLUE;
        if ((hom & (1 << CYAN)) != 0) return Axolotl.Variant.CYAN;
        if ((hom & (1 << GOLD)) != 0) return Axolotl.Variant.GOLD;
        if ((hom & (1 << LUCY)) != 0) return Axolotl.Variant.LUCY;
        return Axolotl.Variant.WILD;
    }

    /** The locus of a colour, -1 for the wild colour. */
    public static int locus(Axolotl.Variant v) {
        return switch (v) {
            case LUCY -> LUCY;
            case GOLD -> GOLD;
            case CYAN -> CYAN;
            case BLUE -> BLUE;
            default -> -1;
        };
    }

    /** Genes for an axolotl of this colour that has none yet: the colour twice, now and then a hidden allele. */
    public static CritterGenes genesFor(Axolotl.Variant v, RandomGenerator rnd) {
        int l = locus(v);
        int a = l < 0 ? 0 : 1 << l, b = a;
        for (int i = 0; i < LOCI; i++) {
            if (i == l) continue;
            if (rnd.nextDouble() < (i == BLUE ? BLUE_CARRIER : COLOR_CARRIER)) a |= 1 << i;
        }
        return new CritterGenes(a, b, 0.1F);
    }

    /** The young of two axolotls (Mendel, one allele of every locus from each parent). */
    public static CritterGenes cross(CritterGenes mother, CritterGenes father, RandomGenerator rnd) {
        CritterGenes g = CritterGenes.cross(mother, father, LOCI, rnd);
        return new CritterGenes(g.a(), g.b(), 0.1F);
    }

    /** Genotype text like {@code blue/+ gold/gold}. */
    public static String genotypeText(CritterGenes g) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LOCI; i++) {
            boolean x = (g.a() & (1 << i)) != 0, y = (g.b() & (1 << i)) != 0;
            if (!x && !y) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(x ? LOCUS_NAMES[i] : "+").append('/').append(y ? LOCUS_NAMES[i] : "+");
        }
        return sb.isEmpty() ? "+/+" : sb.toString();
    }

    /** Whether the water suits the breeding (15–20 °C). */
    public static boolean breedingTemp(float t) {
        return t >= MIN_TEMP && t < STRESS_FROM;
    }

    /** Health lost per day at this temperature. */
    public static double heatDamagePerDay(float t, double perDegree) {
        return t <= HARM_FROM ? 0 : (t - HARM_FROM) * perDegree;
    }

    // ------------------------------------------------------------------ the entity

    /** The genes of an axolotl (made from its colour and kept when it has none yet). */
    public static CritterGenes genes(Axolotl a) {
        CompoundTag d = a.getPersistentData();
        if (d.contains(GENES, Tag.TAG_COMPOUND)) return CritterGenes.load(d.getCompound(GENES));
        long seed = a.getUUID().getMostSignificantBits() ^ a.getUUID().getLeastSignificantBits();
        CritterGenes g = genesFor(a.getVariant(), new java.util.SplittableRandom(seed));
        setGenes(a, g);
        return g;
    }

    public static void setGenes(Axolotl a, CritterGenes g) {
        a.getPersistentData().put(GENES, g.save());
    }

    /** Keeps the genes in the bucket data of an axolotl taken out of a tank (vanilla and other mods ignore the key). */
    public static void toBucket(Entity e, CompoundTag bucketData) {
        if (!(e instanceof Axolotl a)) return;
        CompoundTag d = a.getPersistentData();
        if (d.contains(GENES, Tag.TAG_COMPOUND)) bucketData.put(BUCKET_KEY, genes(a).save());
        if (d.getInt(GENERATION) > 0) bucketData.putInt(BUCKET_GENERATION, d.getInt(GENERATION)); // M42
        if (d.getBoolean(ANALYSED)) bucketData.putBoolean(BUCKET_ANALYSED, true);
    }

    /**
     * Takes the genes back from the bucket data, if they still fit its colour (M42: the generation always, the magnifier's
     * analysis with the genes).
     */
    public static void fromBucket(Entity e, CompoundTag bucketData) {
        if (!(e instanceof Axolotl a)) return;
        if (bucketData.getInt(BUCKET_GENERATION) > 0) a.getPersistentData().putInt(GENERATION, bucketData.getInt(BUCKET_GENERATION));
        if (!bucketData.contains(BUCKET_KEY, Tag.TAG_COMPOUND)) return;
        CritterGenes g = CritterGenes.load(bucketData.getCompound(BUCKET_KEY));
        if (variant(g) == a.getVariant()) {
            setGenes(a, g);
            if (bucketData.getBoolean(BUCKET_ANALYSED)) a.getPersistentData().putBoolean(ANALYSED, true);
        }
    }

    /**
     * The magnifier on a tank: tells the player the colour genes of every axolotl in it (each costs durability once,
     * nothing with free gene visibility). Returns the axolotls newly analysed.
     */
    public static int analyse(ServerLevel level, TankData t, net.minecraft.world.entity.player.Player player, net.minecraft.world.item.ItemStack magnifier,
                              @Nullable net.minecraft.world.entity.EquipmentSlot slot) {
        if (!Features.enabled(Feature.AXOLOTL_CARE)) return 0;
        boolean free = org.overengineer.bettacraft.genetics.GeneVisibility.free();
        int n = 0;
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) {
            if (!(e instanceof Axolotl a)) continue;
            CompoundTag d = a.getPersistentData();
            if (!free && !d.getBoolean(ANALYSED)) {
                if (magnifier.isEmpty()) break;
                d.putBoolean(ANALYSED, true);
                org.overengineer.bettacraft.genetics.MagnifierItem.wear(player, magnifier, slot);
                n++;
            }
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.bettacraft.magnifier.axolotl", a.getDisplayName(),
                    net.minecraft.network.chat.Component.translatable("variety.bettacraft.axolotl." + a.getVariant().getSerializedName()),
                    genotypeText(genes(a))));
        }
        return n;
    }

    // ------------------------------------------------------------------ care step

    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        step(level, c, days, level.getRandom()::nextLong);
    }

    /** M32 part d: with a random source of its own (GameTests: a fixed seed instead of the level's shared random). */
    public static void step(ServerLevel level, TankBlockEntity c, double days, RandomGenerator rnd) {
        if (days <= 0 || !Features.enabled(Feature.AXOLOTL_CARE)) return;
        TankData t = c.getTank();
        hatch(level, c, rnd);
        List<Axolotl> list = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) if (e instanceof Axolotl a) list.add(a);
        if (list.isEmpty()) return;
        float temp = t.waterTemp();
        long now = level.getGameTime();
        double per = ServerConfig.getOr(ServerConfig.AXOLOTL_HEAT_DAMAGE, 1.0);
        for (Axolotl a : list) {
            genes(a);
            CompoundTag d = a.getPersistentData();
            if (d.getDouble(REST) > 0) d.putDouble(REST, Math.max(0, d.getDouble(REST) - days));
            double dmg = org.overengineer.bettacraft.config.Care.needs() ? heatDamagePerDay(temp, per) * days : 0; // M33 part c: DECOR
            if (dmg > 0) {
                double pending = d.getDouble(HEAT) + dmg;
                if (pending >= 1) {
                    float hit = (float) Math.floor(pending);
                    pending -= hit;
                    a.invulnerableTime = 0;
                    a.hurt(level.damageSources().generic(), hit);
                }
                d.putDouble(HEAT, pending);
            }
        }
        if (temp >= STRESS_FROM && c.logDue("axolotl_warm", now, LOG_TICKS)) {
            TankCare.log(level, c, "log.bettacraft.axolotl_warm", String.valueOf(Math.round(temp)));
        }
        lay(level, c, list, temp, days, rnd);
    }

    /** Whether an axolotl can lay or fertilise eggs now. */
    static boolean ready(Axolotl a) {
        return a.isAlive() && !a.isBaby() && a.getPersistentData().getDouble(REST) <= 0;
    }

    private static void lay(ServerLevel level, TankBlockEntity c, List<Axolotl> list, float temp, double days, RandomGenerator rnd) {
        double speed = ServerConfig.getOr(ServerConfig.AXOLOTL_BREEDING, 1.0);
        if (speed <= 0 || !breedingTemp(temp)) return;
        List<Axolotl> ready = new ArrayList<>();
        for (Axolotl a : list) if (ready(a)) ready.add(a);
        if (ready.size() < 2) return;
        TankData t = c.getTank();
        for (CritterObject o : t.critterObjects()) if (o.kind == CritterObject.Kind.AXOLOTL_EGGS) return; // one clutch at a time
        if (rnd.nextDouble() >= 1 - Math.exp(-LAY_RATE * speed * (ready.size() / 2) * days)) return;
        List<TankmateSpots.Spot> plants = new ArrayList<>(TankmateSpots.plants(level.getServer(), t, false));
        plants.addAll(TankmateSpots.plants(level.getServer(), t, true));
        long now = level.getGameTime();
        if (plants.isEmpty()) {
            if (c.logDue("axolotl_no_plants", now, LOG_TICKS)) TankCare.log(level, c, "log.bettacraft.axolotl_no_plants");
            return;
        }
        Axolotl mother = ready.get(rnd.nextInt(ready.size()));
        Axolotl father = null;
        double best = Double.MAX_VALUE;
        for (Axolotl a : ready) {
            if (a == mother) continue;
            double dd = a.distanceToSqr(mother);
            if (dd < best) {
                best = dd;
                father = a;
            }
        }
        if (father == null) return;
        TankmateSpots.Spot spot = plants.get(rnd.nextInt(plants.size()));
        Vec3 p = spot.pos();
        CritterObject o = new CritterObject(t.nextCritterId(), CritterObject.Kind.AXOLOTL_EGGS, p.x, p.y, p.z);
        o.mother = genes(mother);
        o.father = genes(father);
        o.count = 3 + rnd.nextInt(4);
        o.generation = Math.max(mother.getPersistentData().getInt(GENERATION), father.getPersistentData().getInt(GENERATION)) + 1;
        o.yaw = (float) (rnd.nextDouble() * 360);
        t.critterObjects().add(o);
        mother.getPersistentData().putDouble(REST, REST_DAYS);
        father.getPersistentData().putDouble(REST, REST_DAYS * 0.5);
        TankCare.log(level, c, "log.bettacraft.axolotl_eggs", TankCare.nameArg(mother));
        c.setChanged();
        org.overengineer.bettacraft.critters.TankCritters.sync(level, c);
    }

    /** Eggs whose time is up hatch into baby axolotls (as many as the tank has room for). */
    private static void hatch(ServerLevel level, TankBlockEntity c, RandomGenerator rnd) {
        TankData t = c.getTank();
        double eggDays = ServerConfig.getOr(ServerConfig.AXOLOTL_EGG_DAYS, 1.5);
        boolean changed = false;
        for (Iterator<CritterObject> it = t.critterObjects().iterator(); it.hasNext(); ) {
            CritterObject o = it.next();
            if (o.kind != CritterObject.Kind.AXOLOTL_EGGS || o.age < eggDays) continue;
            it.remove();
            changed = true;
            int live = TankInhabitants.liveInhabitants(level, t).size();
            int room = Math.max(0, ServerConfig.getOr(ServerConfig.TANK_MAX_ENTITIES, 64) - live);
            int n = Math.min(o.count, room);
            int made = 0;
            TankWaterVolume w = new TankWaterVolume(t);
            for (int i = 0; i < n; i++) {
                CritterGenes g = o.mother != null && o.father != null ? cross(o.mother, o.father, rnd) : CritterGenes.WILD;
                Vec3 p = w.clamp(new Vec3(o.x + (rnd.nextDouble() - 0.5) * 0.3, o.y, o.z + (rnd.nextDouble() - 0.5) * 0.3), 0.25, 0.1, 0.3);
                if (spawn(level, c, g, o.generation, p) != null) made++;
            }
            TankCare.log(level, c, "log.bettacraft.axolotl_hatched", String.valueOf(made));
            org.overengineer.bettacraft.api.ApiEvents.born(level, c, net.minecraft.world.entity.EntityType.AXOLOTL, made, true); // M32 part d
        }
        if (changed) {
            c.setChanged();
            org.overengineer.bettacraft.critters.TankCritters.sync(level, c);
        }
    }

    /** A baby axolotl with these genes in the tank (hatching, tests). */
    @Nullable
    public static Axolotl spawn(ServerLevel level, TankBlockEntity c, CritterGenes g, int generation, Vec3 p) {
        Axolotl a = EntityType.AXOLOTL.create(level);
        if (a == null) return null;
        a.setVariant(variant(g));
        a.setAge(-24000);
        setGenes(a, g);
        a.getPersistentData().putInt(GENERATION, generation);
        a.moveTo(p.x, p.y, p.z, level.getRandom().nextFloat() * 360F, 0F);
        TankInhabitants.adopt(a, c);
        if (!level.addFreshEntity(a)) return null;
        c.getTank().inhabitants().add(a.getUUID());
        return a;
    }
}
