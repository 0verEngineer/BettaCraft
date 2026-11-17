package org.overengineer.bettacraft.foreign;

import org.overengineer.bettacraft.util.NbtCompat;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaCare;
import org.overengineer.bettacraft.entity.betta.BettaHealth;
import org.overengineer.bettacraft.food.BettaFood;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankWater;

/**
 * M32 part a (question A1): our needs for the animals of other mods with a {@link ForeignProfile}, in our tanks only
 * ({@code features.foreignAnimals.care}). Every second with the tank's care loop each of them gets the bettas' health timers
 * ({@link BettaHealth}: hunger, temperature out of its range, bad water, wrong salinity, pH/TDS far out, too little oxygen,
 * overcrowding; sick after a week of trouble, dead after two more) with the values of its profile. A fed animal that is
 * not sick heals. The timers live in the entity's persistent data ({@link #KEY}), so they travel with it (bucket data
 * of other mods aside: an animal that comes back into a tank starts fed). An animal that is put in a tank for the first
 * time (or was there before this version) starts fed.
 * <p>
 * Feeding goes through the tank's food ({@code TankCare#feed}: by hand, feeders, the live food pump): every animal whose
 * profile eats the food gets a portion, the hungriest first.
 */
public final class ForeignCare {
    public static final String KEY = "bettacraft:foreign_health";

    private ForeignCare() {}

    /** The health parameters for a profile (the bettas' timers, {@code foreign.foreignHealthSpeed}, the profile's hunger). */
    public static BettaHealth.Params params(ForeignProfile p) {
        BettaHealth.Params b = BettaCare.params();
        double speed = ServerConfig.getOr(ServerConfig.FOREIGN_HEALTH_SPEED, 1.0);
        return new BettaHealth.Params(b.daysUntilSick() / speed, b.daysSickUntilDeath() / speed, b.bothBadMultiplier(), b.recoveryPerDay(),
                p.hungerDays() / speed, BettaHealth.Diet.OFF, b.care());
    }

    /** The health timers of an animal (new ones, fed now, if it has none yet). */
    public static BettaHealth health(Entity e) {
        CompoundTag d = org.overengineer.bettacraft.platform.Platform.get().persistentData(e);
        return NbtCompat.has(d, KEY, Tag.TAG_COMPOUND) ? BettaHealth.load(d.getCompoundOrEmpty(KEY)) : new BettaHealth();
    }

    public static void setHealth(Entity e, BettaHealth h) {
        org.overengineer.bettacraft.platform.Platform.get().persistentData(e).put(KEY, h.save());
    }

    /** What is wrong for this animal in this tank now. */
    public static BettaHealth.Conditions conditions(TankBlockEntity c, Entity e, ForeignProfile p) {
        TankData t = c.getTank();
        boolean wet = t.waterMb() > 0;
        // a terrarium animal (M31 profile) has its climate in TerrariumCare; here only its hunger and the crowding count
        boolean terra = p.terrarium() != null;
        boolean tempBad = !terra && wet && !p.tolerates(t.waterTemp());
        double salt = !terra && wet ? org.overengineer.bettacraft.reef.SaltTolerance.shock(ForeignProfiles.salinity(p), t) : 0; // REALISTIC only
        boolean waterBad = false;
        if (!terra && wet) {
            waterBad = TankWater.qualityEnabled() && t.waterQuality() < p.minQuality();
            if (p.chemistry() != null && org.overengineer.bettacraft.chem.TankChemistry.bad(t, p.chemistry())) waterBad = true;
            if (org.overengineer.bettacraft.tank.TankAeration.lowFor(t, p.labyrinth())) waterBad = true;
        }
        boolean crowded = c.isCrowded() && TankInhabitants.isAdult(e);
        return new BettaHealth.Conditions(tempBad, crowded, waterBad, salt);
    }

    /** The care step of a tank (every second): the timers of every foreign animal with a profile. */
    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        if (days <= 0 || !Features.enabled(Feature.FOREIGN_CARE)) return;
        TankData t = c.getTank();
        if (t.inhabitants().isEmpty()) return;
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) {
            ForeignProfile p = ForeignProfiles.of(e.getType());
            if (p == null || !(e instanceof LivingEntity le)) continue;
            advance(level, c, le, p, days);
        }
    }

    /** Advances the timers of one animal by {@code days} (GameTests too). Returns false when it died. */
    public static boolean advance(ServerLevel level, TankBlockEntity c, LivingEntity e, ForeignProfile p, double days) {
        BettaHealth.Params params = params(p);
        BettaHealth h = health(e);
        BettaHealth.Conditions cond = conditions(c, e, p);
        BettaHealth.Event ev = h.step(days, cond, params);
        switch (ev) {
            case GOT_SICK -> TankCare.log(level, c, "log.bettacraft.sick", TankCare.nameArg(e));
            case RECOVERED -> TankCare.log(level, c, "log.bettacraft.recovered", TankCare.nameArg(e));
            case DIED -> {
                String cause = cond.saltBad() ? "salinity" : h.isHungry(params) ? "hunger" : cond.tempBad() ? "temperature"
                        : cond.waterBad() ? "water_quality" : cond.crowdBad() ? "overcrowding" : "illness";
                TankCare.log(level, c, "log.bettacraft.died", TankCare.nameArg(e), "@cause.bettacraft." + cause);
                org.overengineer.bettacraft.api.ApiEvents.died(level, c, e, cause, null); // M32 part d
                c.getTank().inhabitants().remove(e.getUUID());
                c.markTankChanged();
                e.discard(); // like a betta that dies of illness: it simply vanishes
                return false;
            }
            default -> {}
        }
        double heal = ServerConfig.getOr(ServerConfig.HEALTH_HEAL_PER_DAY, 2.0);
        if (!h.isHungry(params) && !h.isSick() && e.getHealth() < e.getMaxHealth() && heal > 0) e.heal((float) (heal * days));
        setHealth(e, h);
        return true;
    }

    // ------------------------------------------------------------------ food

    /** Whether an animal with this profile eats this food now (a baby also eats fry food). */
    public static boolean eats(ForeignProfile p, Entity e, ItemStack food, @Nullable BettaFood.Kind kind) {
        if (food.isEmpty() && kind == null) return false;
        if (kind != null) {
            if ((kind == BettaFood.Kind.FRY || kind == BettaFood.Kind.FRY_START) && (p.eatsKind("fry") || e instanceof LivingEntity le && le.isBaby())) return true;
            if (kind == BettaFood.Kind.LIVE && p.eatsKind("live")) return true;
            if (kind == BettaFood.Kind.NORMAL && p.eatsKind("normal")) return true;
        }
        if (food.isEmpty()) return false;
        for (String f : p.food()) {
            if (f.startsWith("#")) {
                Identifier id = Identifier.tryParse(f.substring(1));
                if (id != null && food.is(TagKey.create(Registries.ITEM, id))) return true;
            } else if (f.contains(":")) {
                if (f.equals(BuiltInRegistries.ITEM.getKey(food.getItem()).toString())) return true;
            }
        }
        return false;
    }

    /** The foreign animals of the tank that eat this food, hungriest first (empty while the care is off). */
    public static List<LivingEntity> eaters(ServerLevel level, TankData t, ItemStack food, @Nullable BettaFood.Kind kind) {
        List<LivingEntity> out = new ArrayList<>();
        if (!Features.enabled(Feature.FOREIGN_CARE) || ForeignProfiles.all().isEmpty()) return out;
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) {
            ForeignProfile p = ForeignProfiles.of(e.getType());
            if (p != null && e instanceof LivingEntity le && eats(p, e, food, kind)) out.add(le);
        }
        out.sort(Comparator.comparingDouble((LivingEntity e) -> health(e).daysSinceFed()).reversed());
        return out;
    }

    /** Feeds up to {@code portions} of them (one portion each); returns how many ate. */
    public static int feed(List<LivingEntity> eaters, int portions, @Nullable Set<UUID> round) {
        int n = 0;
        for (LivingEntity e : eaters) {
            if (n >= portions) break;
            if (round != null && round.contains(e.getUUID())) continue;
            BettaHealth h = health(e);
            h.feed();
            setHealth(e, h);
            if (round != null) round.add(e.getUUID());
            n++;
        }
        return n;
    }

    // ------------------------------------------------------------------ menu

    /** Health and satiation (0..1) and sickness for the menu, or null if the animal has no profile or the care is off. */
    @Nullable
    public static float[] menuValues(Entity e) {
        ForeignProfile p = ForeignProfiles.cared(e);
        if (p == null) return null;
        BettaHealth h = health(e);
        BettaHealth.Params params = params(p);
        return new float[]{h.healthFraction(params), h.satiation(params), h.isSick() ? 1F : 0F};
    }
}
