package org.overengineer.bettacraft.mobs;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModAttachments;
import org.overengineer.bettacraft.tank.InhabitantRules;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankWater;
import org.overengineer.bettacraft.terrarium.TankClimate;
import org.overengineer.bettacraft.terrarium.TerrariumAnimals;
import org.overengineer.bettacraft.terrarium.TerrariumNeeds;

/**
 * M31 part d: Minecraft mobs in glass. A mob of {@link GlassMobSpecies} lives in a tank <b>shrunk</b> by the
 * {@code generic.scale} attribute (an attribute modifier, so the renderer, the hitbox and the shadow follow) and
 * <b>harmless</b>: in its tank its vanilla AI does not run (the mixins on {@code Mob#serverAiStep} and
 * {@code LivingEntity#travel}); the {@link org.overengineer.bettacraft.terrarium.LandWalker} of the terrarium animals moves
 * it instead (spiders up the glass), it targets nothing, slimes do not hurt on touch, zombies call no reinforcements, the
 * undead do not burn in daylight (the glass "protects"; {@code terrarium.mobUndeadBurn}: they do under an open top), it
 * does not drown and does not despawn (also not in peaceful). Every mob that ever lived in glass carries the
 * {@link GlassMobData} mark: it <b>drops nothing and gives no experience</b>, whoever or whatever kills it (players, mobs,
 * fire, a fall), in the tank or after it left it ({@link GlassMobEvents}), so breeding them is no farm.
 * <p>
 * Ways in: the {@link MobJarItem} (a weakened mob), luring into an open tank with bait ({@link #layBait}, {@link #lure}),
 * and the capture items of other mods ({@link org.overengineer.bettacraft.compat.MobCaptures}). In the tank a zombie under
 * water becomes a drowned after 45 s (the vanilla times; a husk a zombie), and pairs fed by hand breed
 * ({@link GlassMobSpecies.Breeding}). The habitats go through the terrarium care hook ({@link TerrariumAnimals}).
 */
public final class GlassMobs {
    public static final ResourceLocation SCALE_ID = BettaCraft.id("in_glass");
    public static final ResourceLocation REINFORCEMENTS_ID = BettaCraft.id("in_glass_reinforcements");
    /** In-game days a young mob bred in the tank needs to grow up. */
    public static final double GROW_DAYS = 2.0;
    /** A fed pair breeds within this many days of the meal; a mob that bred waits {@link #BREED_PAUSE_DAYS}. */
    public static final double LOVE_DAYS = 1.0, BREED_PAUSE_DAYS = 2.0;
    /** Ticks under water before a zombie in a tank becomes a drowned (vanilla: 600 + 300 of shaking). */
    public static final int DROWN_TICKS = 900;
    /** Ticks a bait lasts on an open tank; blocks a lured mob looks for it. */
    public static final long BAIT_TICKS = 24000L;
    public static final double LURE_RANGE = 12.0;

    private static final Map<ResourceLocation, GlassMobSpecies> DEFAULTS = new HashMap<>();
    private static final Map<TankBlockEntity, Bait> BAITS = new WeakHashMap<>();

    static {
        for (GlassMobSpecies s : GlassMobSpecies.DEFAULTS) DEFAULTS.put(s.id(), s);
    }

    private GlassMobs() {}

    /** A bait on an open tank: the item and until when it lures. */
    public record Bait(ItemStack item, long until) {}

    // ------------------------------------------------------------------ which mobs

    /** Whether the feature is on ({@code features.mobsInGlass.enabled}; it lives in terraria, so {@code features.terrariums.enabled} too). */
    public static boolean enabled() {
        return Features.enabled(Feature.MOBS_IN_GLASS) && TankClimate.enabled();
    }

    /**
     * The species of a mob type, or null if it does not go into glass: the defaults (C2) unless {@code inhabitants.deny} names
     * them, and every other mob of the monster category that {@code inhabitants.allow} names (the M16 lists, C2 "extendable").
     */
    @Nullable
    public static GlassMobSpecies species(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        var rules = TankInhabitants.rules();
        if (InhabitantRules.matches(rules.deny(), id.toString())) return null;
        GlassMobSpecies s = DEFAULTS.get(id);
        if (s != null) return s;
        if (type.getCategory() == MobCategory.MONSTER && InhabitantRules.matches(rules.allow(), id.toString())
                && !type.is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES)) {
            return GlassMobSpecies.generic(id, type.getDimensions().width(), type.getDimensions().height());
        }
        return null;
    }

    public static boolean isAllowed(EntityType<?> type) {
        return species(type) != null;
    }

    /** A slime or magma cube goes into glass only small (size 1). */
    public static boolean fits(Entity e) {
        return !(e instanceof Slime s) || s.getSize() <= 1;
    }

    @Nullable
    public static TerrariumNeeds needs(EntityType<?> type) {
        GlassMobSpecies s = species(type);
        return s == null ? null : s.needs();
    }

    /** Whether this mob lives (or lived) in glass: it never drops anything. */
    public static boolean isGlassMob(Entity e) {
        return e.hasData(ModAttachments.GLASS_MOB.get());
    }

    public static GlassMobData data(Entity e) {
        return e.getData(ModAttachments.GLASS_MOB.get());
    }

    /** Server: whether the mob is in its tank right now (its vanilla AI and physics are off). */
    public static boolean held(Entity e) {
        if (!(e instanceof Mob) || e.level().isClientSide || !e.hasData(ModAttachments.GLASS_MOB.get())) return false;
        GlassMobState s = e.getExistingData(ModAttachments.GLASS_STATE.get()).orElse(null);
        return s != null && s.controller != null;
    }

    /** Whether the mob belongs to a tank (saved with it; also before its first tick there). */
    public static boolean inTank(Entity e) {
        return e.hasData(ModAttachments.GLASS_MOB.get()) && TankInhabitants.hasTankRef(e);
    }

    public static double weight(Entity e) {
        GlassMobSpecies s = species(e.getType());
        double w = s == null ? 0.5 : s.stock();
        return grown(e) ? w : w * 0.5;
    }

    public static boolean grown(Entity e) {
        return data(e).growth >= 1 && !(e instanceof LivingEntity le && le.isBaby());
    }

    // ------------------------------------------------------------------ shrinking

    /**
     * Makes a mob a mob in glass (before it joins its tank): the mark, the scale for its growth, no reinforcements, no
     * picking up items, no target; it stays (persistence comes with the tank).
     */
    public static void prepare(Mob m) {
        GlassMobData d = data(m); // creates the mark
        m.setData(ModAttachments.GLASS_MOB.get(), d);
        shrink(m, d.growth);
        m.setCanPickUpLoot(false);
        m.setTarget(null);
        m.setAggressive(false);
        m.clearFire();
        AttributeInstance r = m.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
        if (r != null) r.addOrReplacePermanentModifier(new AttributeModifier(REINFORCEMENTS_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    static void shrink(Mob m, double growth) {
        GlassMobSpecies s = species(m.getType());
        float scale = s != null ? s.scaleAt(growth) : GlassMobSpecies.generic(BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()), m.getType().getDimensions().width(),
                m.getType().getDimensions().height()).scale();
        AttributeInstance a = m.getAttribute(Attributes.SCALE);
        if (a == null) return;
        AttributeModifier old = a.getModifier(SCALE_ID);
        double v = scale - 1.0;
        if (old == null || Math.abs(old.amount() - v) > 1e-4) {
            a.addOrReplacePermanentModifier(new AttributeModifier(SCALE_ID, v, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        m.refreshDimensions(); // the hitbox now (the attribute alone updates it only on the next tick; one loaded from a jar is stale)
        m.getExistingData(ModAttachments.GLASS_STATE.get()).ifPresent(st -> st.shrunk = true);
    }

    /** Back to full size (out of the tank: a broken tank, released from a jar). The mark stays (no drops). */
    public static void unshrink(Mob m) {
        AttributeInstance a = m.getAttribute(Attributes.SCALE);
        if (a != null) a.removeModifier(SCALE_ID);
        AttributeInstance r = m.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
        if (r != null) r.removeModifier(REINFORCEMENTS_ID);
        m.getExistingData(ModAttachments.GLASS_STATE.get()).ifPresent(st -> st.shrunk = false);
        m.refreshDimensions();
    }

    // ------------------------------------------------------------------ the tick (mixin on Mob#serverAiStep)

    /**
     * Instead of the vanilla AI while the mob is in its tank: looks the tank up every second (out of the tank it is a mob
     * again, full size), then one step of the walker. Returns false when the vanilla AI should run.
     */
    public static boolean aiStep(Mob m) {
        if (!(m.level() instanceof ServerLevel level) || !m.hasData(ModAttachments.GLASS_MOB.get())) return false;
        GlassMobState s = m.getData(ModAttachments.GLASS_STATE.get());
        if (m.tickCount >= s.nextResolve || m.tickCount < s.nextResolve - 40) {
            s.nextResolve = m.tickCount + 20;
            TankBlockEntity c = TankInhabitants.resolveController(level, m);
            if (c == null) {
                if (s.controller != null || m.getAttribute(Attributes.SCALE) != null && m.getAttribute(Attributes.SCALE).getModifier(SCALE_ID) != null) unshrink(m);
                s.controller = null;
                return false;
            }
            if (s.controller != c) s.walker.reset();
            s.controller = c;
            s.openTop = openTop(level, c.getTank());
            if (!s.shrunk) shrink(m, data(m).growth);
        }
        TankData t = s.controller.getTank();
        if (m.getTarget() != null) m.setTarget(null);
        GlassMobSpecies sp = species(m.getType());
        var gait = sp != null ? sp.gait() : GlassMobSpecies.generic(BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()), 1, 1).gait();
        s.walker.tick(m, level, t, gait, false);
        m.horizontalCollision = s.walker.onWall(); // a spider shows it climbs (the client lays it onto the glass)
        net.minecraft.core.Direction wall = s.walker.pose().wall();
        if (wall != null) m.setYRot(wall.toYRot());
        m.setYHeadRot(m.getYRot());
        m.yBodyRot = m.getYRot();
        return true;
    }

    /** Whether an undead mob in its tank may burn in daylight now (config, and only under an open top). */
    public static boolean mayBurn(Mob m) {
        GlassMobState s = m.getExistingData(ModAttachments.GLASS_STATE.get()).orElse(null);
        return s != null && s.openTop && ServerConfig.getOr(ServerConfig.TERRA_MOB_UNDEAD_BURN, false);
    }

    // ------------------------------------------------------------------ the care step (every second)

    /** Growth, drowning zombies, breeding and luring of the mobs of one tank. */
    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        TankData t = c.getTank();
        if (days > 0 && !t.inhabitants().isEmpty()) {
            List<Mob> mobs = new ArrayList<>();
            for (Entity e : TankInhabitants.liveInhabitants(level, t)) if (e instanceof Mob m && isGlassMob(m)) mobs.add(m);
            for (Mob m : mobs) {
                GlassMobData d = data(m);
                if (d.growth < 1) {
                    d.growth = Math.min(1, d.growth + days / GROW_DAYS);
                    shrink(m, d.growth);
                    if (d.growth >= 1 && m.isBaby()) m.setBaby(false);
                    if (d.growth >= 1) TankCare.log(level, c, "log.bettacraft.glass.grown", TankCare.nameArg(m));
                }
                drown(level, c, m);
            }
            if (!mobs.isEmpty() && ServerConfig.getOr(ServerConfig.TERRA_MOB_BREEDING, true)) breed(level, c, mobs);
        }
        if (level.getGameTime() % 100 < 20) lure(level, c);
    }

    /** A zombie (or husk) under the water of its tank: after {@link #DROWN_TICKS} a drowned (a husk a zombie), as in vanilla. */
    static void drown(ServerLevel level, TankBlockEntity c, Mob m) {
        if (!(m instanceof Zombie z) || m.getType() != EntityType.ZOMBIE && m.getType() != EntityType.HUSK) return;
        GlassMobState s = m.getData(ModAttachments.GLASS_STATE.get());
        TankData t = c.getTank();
        boolean under = t.waterMb() > 0 && m.getEyeY() < t.waterSurfaceY();
        s.underWater = under ? s.underWater + 20 : 0;
        if (s.underWater < DROWN_TICKS) return;
        convert(level, c, z, m.getType() == EntityType.HUSK ? EntityType.ZOMBIE : EntityType.DROWNED);
    }

    /**
     * M33 part d (hardening): a mob that lived in glass and is converted by vanilla or another mod (lightning, powder snow,
     * a mod's own conversion) keeps the mark on its new body, so it still drops nothing. If it was in a tank, the new mob
     * stays there when it may live in glass (shrunk again), otherwise it leaves the tank's list.
     */
    public static void converted(LivingEntity from, LivingEntity to) {
        if (from == to || !isGlassMob(from) || isGlassMob(to)) return;
        GlassMobData d = data(from), nd = data(to); // data(to) creates the mark
        nd.growth = d.growth;
        nd.fedAt = d.fedAt;
        nd.bredAt = d.bredAt;
        to.setData(ModAttachments.GLASS_MOB.get(), nd);
        if (!(to.level() instanceof ServerLevel level) || !TankInhabitants.hasTankRef(from)) return;
        TankBlockEntity c = TankInhabitants.resolveController(level, from);
        if (c == null) return;
        TankData t = c.getTank();
        t.inhabitants().remove(from.getUUID());
        if (to instanceof Mob m && species(to.getType()) != null && enabled()) {
            prepare(m);
            TankInhabitants.adopt(m, c);
            t.inhabitants().add(m.getUUID());
        } else {
            TankInhabitants.release(to);
        }
        c.markTankChanged();
    }

    /** Converts a zombie in its tank (GameTests call it directly). */
    @Nullable
    public static Mob convert(ServerLevel level, TankBlockEntity c, Zombie z, EntityType<? extends Zombie> to) {
        GlassMobData d = data(z);
        TankData t = c.getTank();
        java.util.UUID old = z.getUUID();
        String from = "@" + z.getType().getDescriptionId();
        Zombie n = z.convertTo(to, true);
        if (n == null) return null;
        GlassMobData nd = data(n);
        nd.growth = d.growth;
        nd.fedAt = d.fedAt;
        nd.bredAt = d.bredAt;
        prepare(n);
        TankInhabitants.adopt(n, c);
        t.inhabitants().remove(old);
        t.inhabitants().add(n.getUUID());
        level.playSound(null, n.blockPosition(), to == EntityType.DROWNED ? SoundEvents.ZOMBIE_CONVERTED_TO_DROWNED : SoundEvents.HUSK_CONVERTED_TO_ZOMBIE,
                SoundSource.HOSTILE, 0.6F, 1.6F);
        TankCare.log(level, c, "log.bettacraft.glass.converted", from, "@" + to.getDescriptionId());
        c.markTankChanged();
        return n;
    }

    /** Two grown, fed mobs of a species that breeds: a young one (a vanilla baby, a spiderling, a small slime). */
    static void breed(ServerLevel level, TankBlockEntity c, List<Mob> mobs) {
        long now = level.getGameTime();
        Map<EntityType<?>, List<Mob>> byType = new HashMap<>();
        for (Mob m : mobs) {
            GlassMobSpecies s = species(m.getType());
            if (s == null || s.breeding() == GlassMobSpecies.Breeding.NONE || !grown(m) || !fits(m)) continue;
            GlassMobData d = data(m);
            if (d.fedAt == GlassMobData.NEVER || now - d.fedAt > LOVE_DAYS * 24000) continue;
            if (d.bredAt != GlassMobData.NEVER && now - d.bredAt < BREED_PAUSE_DAYS * 24000) continue;
            byType.computeIfAbsent(m.getType(), k -> new ArrayList<>()).add(m);
        }
        for (List<Mob> pair : byType.values()) {
            if (pair.size() < 2) continue;
            Mob a = pair.get(0), b = pair.get(1);
            Mob young = young(level, c, a, b);
            if (young == null) continue;
            data(a).bredAt = now;
            data(b).bredAt = now;
            data(a).fedAt = GlassMobData.NEVER;
            data(b).fedAt = GlassMobData.NEVER;
            level.sendParticles(ParticleTypes.HEART, young.getX(), young.getY() + 0.3, young.getZ(), 3, 0.1, 0.05, 0.1, 0.0);
            TankCare.log(level, c, "log.bettacraft.glass.born", TankCare.nameArg(young));
            org.overengineer.bettacraft.api.ApiEvents.born(level, c, young.getType(), 1, false); // M32 part d
        }
    }

    @Nullable
    static Mob young(ServerLevel level, TankBlockEntity c, Mob a, Mob b) {
        TankData t = c.getTank();
        List<Entity> live = TankInhabitants.liveInhabitants(level, t);
        if (live.size() >= ServerConfig.getOr(ServerConfig.TANK_MAX_ENTITIES, 64)) return null;
        double cap = TankInhabitants.hardCap(t.waterBlocks() + TerrariumAnimals.landBlocks(t));
        Entity made = a.getType().create(level);
        if (!(made instanceof Mob m)) {
            if (made != null) made.discard();
            return null;
        }
        GlassMobSpecies s = species(a.getType());
        GlassMobData d = data(m);
        if (s != null && s.breeding() == GlassMobSpecies.Breeding.BABY) m.setBaby(true);
        d.growth = 0;
        if (m instanceof Slime slime) slime.setSize(1, true);
        prepare(m);
        if (TankInhabitants.stocking(live, t) + weight(m) > cap + 1e-6) {
            m.discard();
            return null;
        }
        Vec3 at = a.position().add(b.position()).scale(0.5);
        m.moveTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360F, 0F);
        TankInhabitants.adopt(m, c);
        if (!level.tryAddFreshEntityWithPassengers(m) || m.isRemoved()) {
            if (!m.isRemoved()) m.discard();
            return null;
        }
        t.inhabitants().add(m.getUUID());
        c.markTankChanged();
        return m;
    }

    // ------------------------------------------------------------------ admission (jar, lure, other mods)

    /**
     * Puts a live mob (lured, or one that was never in a container) into a tank: the same rules as an animal from a bucket
     * (the tank's mode, the stocking, the entity limit). Returns an error or null.
     */
    @Nullable
    public static Component admit(ServerLevel level, TankBlockEntity c, Mob m) {
        TankData t = c.getTank();
        if (!enabled() || species(m.getType()) == null || !fits(m)) return Component.translatable("message.bettacraft.glass.not_allowed", m.getType().getDescription());
        Component no = TerrariumAnimals.refusal(level, c, m.getType());
        if (no != null) return no;
        List<Entity> live = TankInhabitants.liveInhabitants(level, t);
        int max = ServerConfig.getOr(ServerConfig.TANK_MAX_ENTITIES, 64);
        if (live.size() >= max) return Component.translatable("message.bettacraft.tank.max_entities", max);
        int cap = TankInhabitants.hardCap(t.waterBlocks() + TerrariumAnimals.landBlocks(t));
        if (TankInhabitants.stocking(live, t) + weight(m) > cap + 1e-6) return Component.translatable("message.bettacraft.tank.crowded_full", cap);
        if (m.isPassenger()) m.stopRiding();
        m.ejectPassengers();
        prepare(m);
        Vec3 at = TerrariumAnimals.spawnPos(level, t, m.getType(), level.getRandom());
        m.moveTo(at.x, at.y, at.z, m.getYRot(), 0F);
        m.setDeltaMovement(Vec3.ZERO);
        m.fallDistance = 0;
        TankInhabitants.adopt(m, c);
        t.inhabitants().add(m.getUUID());
        TankCare.log(level, c, "log.bettacraft.inserted", TankCare.nameArg(m));
        TankInhabitants.reconcile(level, c);
        c.markTankChanged();
        return null;
    }

    /**
     * Food by hand on a tank: the first grown mob that eats it and has not eaten lately is fed (a fed pair breeds). Without
     * one, on a tank with an open top, the item is laid as bait ({@link #layBait}). PASS otherwise.
     */
    public static ItemInteractionResult handFeed(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (stack.isEmpty() || !(level instanceof ServerLevel server) || !enabled()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        TankBlockEntity c = TankMultiblock.getController(level, pos);
        if (c == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        long now = server.getGameTime();
        for (Entity e : TankInhabitants.liveInhabitants(server, c.getTank())) {
            if (!(e instanceof Mob m) || !isGlassMob(m)) continue;
            GlassMobSpecies s = species(m.getType());
            if (s == null || !s.eats(stack) || !grown(m)) continue;
            GlassMobData d = data(m);
            if (d.fedAt != GlassMobData.NEVER && now - d.fedAt < LOVE_DAYS * 24000) continue;
            d.fedAt = now;
            ItemStack one = stack.copyWithCount(1);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            server.sendParticles(ParticleTypes.HEART, m.getX(), m.getY() + m.getBbHeight() + 0.1, m.getZ(), 2, 0.05, 0.05, 0.05, 0.0);
            server.playSound(null, m.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.5F, 1.4F);
            TankCare.log(server, c, "log.bettacraft.glass.fed", TankCare.nameArg(m), "@" + one.getDescriptionId());
            player.displayClientMessage(Component.translatable("message.bettacraft.terrarium.ate", m.getDisplayName(), one.getHoverName()), true);
            return ItemInteractionResult.CONSUME;
        }
        Component bait = layBait(server, c, stack, player);
        if (bait == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        player.displayClientMessage(bait, true);
        return ItemInteractionResult.CONSUME;
    }

    /**
     * Lays one of the item as bait on a tank with an open top, if it lures some mob (a message), else null. A bait lasts a
     * day ({@link #BAIT_TICKS}; it is not saved: after a restart it is gone).
     */
    @Nullable
    public static Component layBait(ServerLevel level, TankBlockEntity c, ItemStack stack, @Nullable Player player) {
        boolean lures = false;
        for (GlassMobSpecies s : GlassMobSpecies.DEFAULTS) if (s.lures(stack) && species(BuiltInRegistries.ENTITY_TYPE.get(s.id())) != null) lures = true;
        if (!lures) return null;
        if (!openTop(level, c.getTank())) return Component.translatable("message.bettacraft.glass.bait_lid");
        BAITS.put(c, new Bait(stack.copyWithCount(1), level.getGameTime() + BAIT_TICKS));
        if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
        TankCare.log(level, c, "log.bettacraft.glass.bait", "@" + stack.getDescriptionId());
        return Component.translatable("message.bettacraft.glass.bait", stack.getHoverName());
    }

    @Nullable
    public static Bait bait(TankBlockEntity c) {
        return BAITS.get(c);
    }

    /**
     * Luring (every 5 s while a bait lies on an open tank): mobs that bait lures within {@link #LURE_RANGE} walk to the tank;
     * one that reaches an open top (within a jump of it, a spider at any height) falls in and shrinks. One bait, one mob.
     */
    public static void lure(ServerLevel level, TankBlockEntity c) {
        Bait b = BAITS.get(c);
        if (b == null) return;
        if (level.getGameTime() > b.until || !enabled()) {
            BAITS.remove(c);
            return;
        }
        TankData t = c.getTank();
        List<BlockPos> open = new ArrayList<>();
        for (BlockPos top : t.openTops()) if (!TankWater.covers(level, top) && level.isLoaded(top)) open.add(top);
        if (open.isEmpty()) return;
        AABB box = TankInhabitants.tankBox(t);
        for (Mob m : level.getEntitiesOfClass(Mob.class, box.inflate(LURE_RANGE), x -> x.isAlive() && !TankInhabitants.hasTankRef(x) && fits(x))) {
            GlassMobSpecies s = species(m.getType());
            if (s == null || !s.lures(b.item())) continue;
            BlockPos top = nearest(open, m.position());
            double topY = top.getY();
            double dx = m.getX() - (top.getX() + 0.5), dz = m.getZ() - (top.getZ() + 0.5);
            boolean reach = dx * dx + dz * dz <= 1.6 * 1.6 && (s.climbs() || m.getY() + 1.25 >= topY);
            if (reach && admit(level, c, m) == null) {
                BAITS.remove(c);
                level.playSound(null, top, SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL, 0.4F, 1.6F);
                TankCare.log(level, c, "log.bettacraft.glass.lured", TankCare.nameArg(m));
                return;
            }
            m.getNavigation().moveTo(top.getX() + 0.5, topY, top.getZ() + 0.5, 1.0);
        }
    }

    /** Whether a top block of the tank has no lid (nor a lamp, feeder or another block over it). */
    public static boolean openTop(ServerLevel level, TankData t) {
        for (BlockPos top : t.openTops()) if (level.isLoaded(top) && !TankWater.covers(level, top)) return true;
        return false;
    }

    private static BlockPos nearest(List<BlockPos> list, Vec3 p) {
        BlockPos best = list.getFirst();
        double bd = Double.MAX_VALUE;
        for (BlockPos q : list) {
            double d = q.distToCenterSqr(p);
            if (d < bd) {
                bd = d;
                best = q;
            }
        }
        return best;
    }

    /** GameTests: forget the baits. */
    public static void clearBaits() {
        BAITS.clear();
    }
}
