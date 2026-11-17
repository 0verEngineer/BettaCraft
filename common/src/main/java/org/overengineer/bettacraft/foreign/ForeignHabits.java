package org.overengineer.bettacraft.foreign;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.overengineer.bettacraft.entity.betta.BettaHealth;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankWater;

/**
 * M39 part b: the habits of the small land animals, insects, mammals and birds of other mods in our terraria and paludaria
 * (the {@code terrarium} part of their profile, {@link ForeignProfile.Land}):
 * <ul>
 *     <li><b>Lid</b> (question G1, {@code lid}; every flier by default): a flying insect or a bird needs a closed top. Our tanks
 *     refuse it while a top block of the tank is open (a lid, a light, a feeder or another solid block covers one, as for the
 *     evaporation), and one in a tank whose lid is taken off flies out through the open top within a few hours (it is let
 *     go into the world above the tank, alive, with its own AI again).</li>
 *     <li><b>Clean-up crew</b> ({@code cleanup}: woodlice, snails): they eat the leftovers of the tank's food (the portions
 *     nobody needed do not rot) and live from the waste of the other animals (a clean-up animal is fed as long as another
 *     animal of its tank ate within the last day).</li>
 *     <li><b>Glow</b> ({@code glows}: Naturalist's fireflies): in the dark of the tank (at night) a firefly flashes every few
 *     seconds with its own glow, and the daylight through the glass does not drive it away (Naturalist lets a firefly
 *     vanish after half a minute in the sun; in our tank it stays).</li>
 * </ul>
 * The caterpillar of Naturalist only turns into a butterfly by its own goal (it needs a real log beside it in the world, which
 * the land of a tank, its interior in miniature, is not): we do not suppress it, and it does not happen in our tanks.
 */
public final class ForeignHabits {
    /** Escapes per day of a flier in a tank with an open top (half of them within about an hour). */
    public static final double ESCAPE_PER_DAY = 16.0;
    /** Days after its last meal another animal still leaves waste the clean-up crew lives from. */
    public static final double WASTE_DAYS = 1.0;
    /** Ticks between two flashes of a glowing animal in the dark, and how long one lasts. */
    public static final int FLASH_EVERY = 60, FLASH_TICKS = 40;

    private ForeignHabits() {}

    // ------------------------------------------------------------------ profile

    @Nullable
    private static ForeignProfile.Land land(EntityType<?> type) {
        return ForeignTerrarium.land(type);
    }

    /** Whether this type needs a closed top (a flier). */
    public static boolean needsLid(EntityType<?> type) {
        ForeignProfile.Land l = land(type);
        return l != null && l.lid();
    }

    public static boolean cleanup(EntityType<?> type) {
        ForeignProfile.Land l = land(type);
        return l != null && l.cleanup();
    }

    public static boolean glows(EntityType<?> type) {
        ForeignProfile.Land l = land(type);
        return l != null && l.glows();
    }

    // ------------------------------------------------------------------ lid

    /** Why a flier does not go into this tank now (an open top), or null. */
    @Nullable
    public static Component lidRefusal(ServerLevel level, TankBlockEntity c, EntityType<?> type) {
        if (!needsLid(type) || !ForeignTerrarium.enabled()) return null;
        if (!org.overengineer.bettacraft.mobs.GlassMobs.openTop(level, c.getTank())) return null;
        return Component.translatable("message.bettacraft.terrarium.needs_lid", type.getDescription());
    }

    /** The first open top block of the tank (the spot above it), or null if every one is covered. */
    @Nullable
    static BlockPos openTop(ServerLevel level, TankData t) {
        for (BlockPos top : t.openTops()) if (level.isLoaded(top) && !TankWater.covers(level, top)) return top;
        return null;
    }

    // ------------------------------------------------------------------ the care step

    /** One care step (every second): fliers of a tank with an open top fly out, the clean-up crew lives from the waste. */
    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        if (days <= 0 || !ForeignTerrarium.enabled() || ForeignProfiles.all().isEmpty()) return;
        TankData t = c.getTank();
        if (t.inhabitants().isEmpty()) return;
        List<Entity> live = TankInhabitants.liveInhabitants(level, t);
        BlockPos open = null;
        boolean checked = false;
        List<LivingEntity> crew = new ArrayList<>();
        boolean waste = false;
        for (Entity e : live) {
            ForeignProfile.Land l = land(e.getType());
            if (l != null && l.cleanup() && e instanceof LivingEntity le && ForeignProfiles.cared(e) != null) {
                crew.add(le);
                continue;
            }
            if (!waste) waste = ateLately(e);
            if (l == null || !l.lid() || !(e instanceof Mob m) || m.isNoAi()) continue;
            if (!checked) {
                open = openTop(level, t);
                checked = true;
            }
            if (open == null) continue;
            if (level.getRandom().nextDouble() >= 1 - Math.exp(-ESCAPE_PER_DAY * days)) continue;
            flyOut(level, c, m, open);
        }
        if (waste && !crew.isEmpty()) {
            for (LivingEntity e : crew) {
                BettaHealth h = ForeignCare.health(e);
                if (h.daysSinceFed() < 0.25) continue; // ate a little while ago
                h.feed();
                ForeignCare.setHealth(e, h);
            }
        }
    }

    /** Whether this animal ate within {@link #WASTE_DAYS} (it leaves waste for the clean-up crew). */
    private static boolean ateLately(Entity e) {
        if (e instanceof org.overengineer.bettacraft.terrarium.TerrariumAnimal a) return a.daysSinceFed() < WASTE_DAYS;
        if (ForeignProfiles.of(e.getType()) != null) return ForeignCare.health(e).daysSinceFed() < WASTE_DAYS;
        if (e instanceof org.overengineer.bettacraft.entity.betta.BettaEntity b) return b.health().daysSinceFed() < WASTE_DAYS;
        return false;
    }

    /** The flier leaves its tank through the open top: out of the tank, into the world above the open block, its own AI again. */
    public static void flyOut(ServerLevel level, TankBlockEntity c, Mob m, BlockPos open) {
        TankData t = c.getTank();
        TankCare.log(level, c, "log.bettacraft.foreign.flew_out", TankCare.nameArg(m));
        t.inhabitants().remove(m.getUUID());
        TankInhabitants.release(m);
        m.snapTo(open.getX() + 0.5, open.getY() + 0.6, open.getZ() + 0.5, m.getYRot(), 0F);
        m.setDeltaMovement(0, 0.2, 0);
        c.markTankChanged();
    }

    // ------------------------------------------------------------------ clean-up crew

    /**
     * Food nobody needed (overfed, left over): the clean-up crew of the tank eats it, one portion each, the hungriest first.
     * Returns the portions that are still left (they rot).
     */
    public static int eatLeftovers(ServerLevel level, TankBlockEntity c, int portions) {
        if (portions <= 0 || !ForeignTerrarium.enabled() || ForeignProfiles.all().isEmpty()) return portions;
        TankData t = c.getTank();
        if (t.inhabitants().isEmpty()) return portions;
        List<LivingEntity> crew = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) {
            if (e instanceof LivingEntity le && cleanup(e.getType()) && ForeignProfiles.cared(e) != null) crew.add(le);
        }
        if (crew.isEmpty()) return portions;
        crew.sort(Comparator.comparingDouble((LivingEntity e) -> ForeignCare.health(e).daysSinceFed()).reversed());
        int n = Math.min(portions, crew.size());
        ForeignCare.feed(crew, n, null);
        TankCare.log(level, c, "log.bettacraft.foreign.cleanup", TankCare.nameArg(crew.getFirst()), String.valueOf(n));
        return portions - n;
    }

    // ------------------------------------------------------------------ glow (every tick while the walker holds it)

    private record GlowFields(@Nullable EntityDataAccessor<Integer> glow, @Nullable EntityDataAccessor<Integer> start,
                              @Nullable EntityDataAccessor<Integer> sun) {}

    private static final Map<Class<?>, GlowFields> FIELDS = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    @Nullable
    private static EntityDataAccessor<Integer> accessor(Class<?> c, String name) {
        for (Class<?> k = c; k != null && k != Entity.class; k = k.getSuperclass()) {
            try {
                Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                Object v = f.get(null);
                return v instanceof EntityDataAccessor<?> a ? (EntityDataAccessor<Integer>) a : null;
            } catch (NoSuchFieldException ignored) {
                // the next superclass
            } catch (ReflectiveOperationException | RuntimeException ex) {
                return null;
            }
        }
        return null;
    }

    private static GlowFields fields(Entity e) {
        return FIELDS.computeIfAbsent(e.getClass(), c -> new GlowFields(accessor(c, "GLOW_TICKS_REMAINING"), accessor(c, "GLOW_START_TICK"),
                accessor(c, "SUN_TICKS")));
    }

    /** Whether the tank is dark for a glowing animal now (night). */
    public static boolean dark(ServerLevel level) {
        return !level.isBrightOutside();
    }

    /** One tick of a glowing animal held by the walker in its tank: no sun that drives it away, a flash now and then in the dark. */
    public static void tick(Mob m, ServerLevel level) {
        if (!glows(m.getType())) return;
        GlowFields f = fields(m);
        try {
            if (f.sun() != null) m.getEntityData().set(f.sun(), 0);
            if (f.glow() != null && dark(level)) flash(m, f);
        } catch (RuntimeException ignored) {
            // not the data of this entity after all
        }
    }

    private static void flash(Mob m, GlowFields f) {
        int phase = Math.floorMod(m.tickCount + m.getUUID().hashCode(), FLASH_EVERY);
        if (phase != 0 || m.getEntityData().get(f.glow()) > 0) return;
        m.getEntityData().set(f.glow(), FLASH_TICKS);
        if (f.start() != null) m.getEntityData().set(f.start(), m.tickCount);
    }

    /** GameTests: whether this animal glows now (its own glow ticks), or false if it has none. */
    public static boolean glowing(Entity e) {
        GlowFields f = fields(e);
        try {
            return f.glow() != null && e.getEntityData().get(f.glow()) > 0;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /** GameTests: its own sun ticks (Naturalist's firefly vanishes above 600), or -1. */
    public static int sunTicks(Entity e) {
        GlowFields f = fields(e);
        try {
            return f.sun() != null ? e.getEntityData().get(f.sun()) : -1;
        } catch (RuntimeException ex) {
            return -1;
        }
    }

    /** GameTests: sets its sun ticks. */
    public static void setSunTicks(Entity e, int ticks) {
        GlowFields f = fields(e);
        if (f.sun() != null) e.getEntityData().set(f.sun(), ticks);
    }

    /** GameTests: one flash now (as in the dark). */
    public static boolean flashNow(Mob m) {
        GlowFields f = fields(m);
        if (f.glow() == null) return false;
        m.getEntityData().set(f.glow(), FLASH_TICKS);
        if (f.start() != null) m.getEntityData().set(f.start(), m.tickCount); // the glow animation starts now
        return true;
    }
}
