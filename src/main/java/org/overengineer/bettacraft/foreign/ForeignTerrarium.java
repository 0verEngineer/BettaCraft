package org.overengineer.bettacraft.foreign;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.BettaHealth;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.BigTankRules;
import org.overengineer.bettacraft.tank.InhabitantRules;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.terrarium.LandWalker;
import org.overengineer.bettacraft.terrarium.TankClimate;
import org.overengineer.bettacraft.terrarium.TerrariumAnimals;
import org.overengineer.bettacraft.terrarium.TerrariumBoxItem;
import org.overengineer.bettacraft.terrarium.TerrariumNeeds;

/**
 * M35: the land and marsh animals of other mods in terraria and paludaria ({@code features.foreignAnimals.terrarium}), by the
 * {@code terrarium} part of their profile ({@link ForeignProfile#land}):
 * <ul>
 *     <li><b>Size:</b> one with a {@code length} is a big animal (M30): the size rule over the land, the water or the whole tank
 *     ({@link ForeignProfile.Land#volume}, {@code BigAnimals}); without one its hitbox decides as for every animal.</li>
 *     <li><b>Movement:</b> in its tank the walker of our terrarium animals ({@link LandWalker}) moves it instead of its own AI
 *     (the mixins on {@code Mob#serverAiStep} and {@code LivingEntity#travel}, like the mobs in glass): the land and the water
 *     of a tank are its interior in miniature, which the AI of a mod cannot see (it would walk into the glass or stand still),
 *     and the walker also brings it to its basking spot and its nest. {@code "movement": "own"} keeps its own AI.</li>
 *     <li><b>Hunting:</b> one with {@code prey} eats the small fish (tankmates up to that length, bettas, dwarf frogs, small fish
 *     of other mods) and the shrimp of its tank, like our big predators ({@code tankmates.predation} per day; plants hide them);
 *     a meal feeds it.</li>
 *     <li><b>Catching:</b> the empty terrarium box catches one whose profile allows it ({@code box}, default yes), and the animals
 *     on {@code terrarium.boxAllow} (question C1); every other animal is refused with a message. A big one without the box
 *     goes the way of the big water animals (large net, transport tub).</li>
 * </ul>
 * The care (hunger, the climate needs) is the one of the profiles (M32, {@link ForeignCare}) and of the terrarium animals (M31).
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class ForeignTerrarium {
    /** The needs of an animal of {@code terrarium.boxAllow}: any terrarium or paludarium, no climate wishes. */
    public static final TerrariumNeeds LISTED_NEEDS = TerrariumNeeds.builder(TerrariumNeeds.Habitat.ANY).build();
    /** The gait of an animal of {@code terrarium.boxAllow}. */
    private static final LandWalker.Gait LISTED_GAIT = ForeignProfile.gait(LandWalker.Style.WALK);

    /** The walkers of the animals in their tanks (server thread only; an entity that is gone drops out). */
    private static final Map<Mob, State> STATES = new WeakHashMap<>();

    private static final class State {
        final LandWalker walker = new LandWalker();
        @Nullable
        TankBlockEntity controller;
        int nextResolve;
    }

    private ForeignTerrarium() {}

    /** The switch, the care of the foreign animals it needs and the terrariums. */
    public static boolean enabled() {
        return Features.enabled(Feature.FOREIGN_TERRARIUM) && TankClimate.enabled();
    }

    // ------------------------------------------------------------------ profiles

    /** The land part of the profile of this type (whatever the switches), or null. */
    @Nullable
    public static ForeignProfile.Land land(EntityType<?> type) {
        ForeignProfile p = ForeignProfiles.of(type);
        return p != null ? p.land() : null;
    }

    /** Its length (m) for the size rule, 0 if it has none (whatever the switches: one in a tank is never put out as too big). */
    public static double length(EntityType<?> type) {
        ForeignProfile.Land l = land(type);
        return l != null && l.big() ? l.lengthM() : 0;
    }

    /** The part of the tank its size rule measures, or null (no terrarium profile). */
    @Nullable
    public static BigTankRules.Volume volume(EntityType<?> type) {
        ForeignProfile.Land l = land(type);
        if (l != null) return l.volume();
        return type == EntityType.TURTLE ? BigTankRules.Volume.WATER : null;
    }

    /**
     * Whether the type is on {@code terrarium.boxAllow} (and has no care of ours and no profile): a land animal the box
     * catches all the same; it lives in terraria and paludaria without our care.
     */
    public static boolean listed(EntityType<?> type) {
        List<? extends String> allow = ServerConfig.getOr(ServerConfig.TERRA_BOX_ALLOW, List.of());
        if (allow.isEmpty()) return false;
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (BettaCraft.MOD_ID.equals(id.getNamespace()) || TerrariumAnimals.isRegistered(type) || ForeignProfiles.of(type) != null) return false;
        if (type == EntityType.PLAYER || type.is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES)) return false;
        return InhabitantRules.matches(allow, id.toString());
    }

    /** The gait the walker moves it with in its tank, or null (its own AI; no terrarium animal of another mod). */
    @Nullable
    public static LandWalker.Gait gait(EntityType<?> type) {
        ForeignProfile.Land l = land(type);
        if (l != null) return l.gait();
        return listed(type) ? LISTED_GAIT : null;
    }

    // ------------------------------------------------------------------ the walker (mixin on Mob#serverAiStep)

    /** Instead of its own AI while it is in its tank: one step of the walker. Returns false when its AI should run. */
    public static boolean aiStep(Mob m) {
        if (!(m.level() instanceof ServerLevel level) || ForeignProfiles.all().isEmpty() && ServerConfig.getOr(ServerConfig.TERRA_BOX_ALLOW, List.of()).isEmpty()) {
            return false;
        }
        LandWalker.Gait gait = gait(m.getType());
        if (gait == null || !TankInhabitants.hasTankRef(m) || !enabled()) {
            if (gait != null) STATES.remove(m);
            return false;
        }
        State s = STATES.computeIfAbsent(m, k -> new State());
        if (m.tickCount >= s.nextResolve || m.tickCount < s.nextResolve - 40 || s.controller == null) {
            s.nextResolve = m.tickCount + 20;
            TankBlockEntity c = TankInhabitants.resolveController(level, m);
            if (c == null) {
                s.controller = null;
                return false;
            }
            if (s.controller != c) s.walker.reset();
            s.controller = c;
        }
        TankData t = s.controller.getTank();
        if (m.getTarget() != null) m.setTarget(null);
        TerrariumNeeds needs = TerrariumAnimals.needs(m);
        boolean cold = needs != null && t.hasClimate() && needs.air().below(t.airTemp());
        s.walker.tick(m, level, t, gait, cold);
        m.setYHeadRot(m.getYRot());
        m.yBodyRot = m.getYRot();
        return true;
    }

    /** Server: whether the walker holds this animal now (no vanilla physics, no drowning in its tank). */
    public static boolean held(LivingEntity e) {
        if (!(e instanceof Mob m) || e.level().isClientSide) return false;
        State s = STATES.get(m);
        return s != null && s.controller != null;
    }

    /** The walker of this animal in its tank, or null (its own AI, not in a tank yet). */
    @Nullable
    public static LandWalker walker(Entity e) {
        State s = e instanceof Mob m ? STATES.get(m) : null;
        return s != null && s.controller != null ? s.walker : null;
    }

    // ------------------------------------------------------------------ hunting (the care step)

    /** One care step of the hunters among the foreign animals of a tank. */
    public static void hunt(ServerLevel level, TankBlockEntity c, double days) {
        if (days <= 0 || !enabled() || !org.overengineer.bettacraft.config.Care.canDie()) return; // M33 part c: NO_DEATH / DECOR: nobody is eaten
        TankData t = c.getTank();
        if (t.inhabitants().isEmpty() || ForeignProfiles.all().isEmpty()) return;
        List<Entity> live = TankInhabitants.liveInhabitants(level, t);
        List<LivingEntity> hunters = new ArrayList<>();
        double reach = 0;
        for (Entity e : live) {
            ForeignProfile.Land l = land(e.getType());
            if (l != null && l.preyLength() > 0 && e instanceof LivingEntity le && !le.isBaby() && ForeignProfiles.cared(e) != null) {
                hunters.add(le);
                reach = Math.max(reach, l.preyLength());
            }
        }
        if (hunters.isEmpty()) return;
        double rate = ServerConfig.getOr(ServerConfig.TANKMATES_PREDATION, 0.3);
        if (rate <= 0) return;
        List<Entity> fish = prey(live, reach);
        List<Critter> shrimp = new ArrayList<>();
        for (Critter k : t.critters()) if (k.alive() && k.species.group() == CritterSpecies.Group.SHRIMP) shrimp.add(k);
        if (fish.isEmpty() && shrimp.isEmpty()) return;
        RandomSource r = level.getRandom();
        double hide = org.overengineer.bettacraft.critters.TankCritters.hiding(level, t);
        double expected = rate * hunters.size() * days * (1 - 0.7 * hide);
        int n = (int) Math.floor(expected) + (r.nextDouble() < expected - Math.floor(expected) ? 1 : 0);
        for (int i = 0; i < n && !(fish.isEmpty() && shrimp.isEmpty()); i++) {
            LivingEntity by = hunters.get(r.nextInt(hunters.size()));
            int pick = r.nextInt(fish.size() + shrimp.size());
            if (pick < fish.size()) {
                Entity victim = fish.remove(pick);
                TankCare.log(level, c, "log.bettacraft.eaten", TankCare.nameArg(victim), TankCare.nameArg(by));
                org.overengineer.bettacraft.api.ApiEvents.died(level, c, victim, "eaten", null); // M32 part d
                t.inhabitants().remove(victim.getUUID());
                victim.discard();
            } else {
                Critter k = shrimp.remove(pick - fish.size());
                t.critters().remove(k);
                org.overengineer.bettacraft.api.ApiEvents.diedData(level, c, BettaCraft.id(k.species.id()), 1, "eaten"); // M32 part d
                TankCare.log(level, c, "log.bettacraft.tm.ate_prey", TankCare.nameArg(by), "0", "1");
            }
            level.playSound(null, by.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.6F, 0.8F);
            BettaHealth h = ForeignCare.health(by);
            h.feed(); // a meal
            ForeignCare.setHealth(by, h);
            c.markTankChanged();
        }
    }

    /**
     * The animals of the tank a hunter of this reach (blocks) eats: tankmates up to that length (no hunters themselves, no
     * big ones, no fry), and from half a block on the bettas and dwarf frogs too (an adult betta is half a block long), and
     * the small fish of vanilla and other mods (a hitbox up to the reach).
     */
    public static List<Entity> prey(List<Entity> live, double reach) {
        List<Entity> out = new ArrayList<>();
        for (Entity e : live) {
            if (!e.isAlive()) continue;
            if (e instanceof TankmateEntity m) {
                if (m.species().preyLength() <= 0 && !m.species().isLarge() && m.species().length() <= reach + 1e-6) out.add(m);
            } else if (e instanceof BettaEntity || e instanceof org.overengineer.bettacraft.entity.frog.DwarfFrogEntity) {
                if (reach >= org.overengineer.bettacraft.tankmates.TankmateBreeding.BETTA_PREY_LENGTH - 1e-6) out.add(e);
            } else if (e instanceof net.minecraft.world.entity.animal.AbstractFish && e.getBbWidth() <= reach + 1e-6
                    && org.overengineer.bettacraft.big.BigAnimals.length(e.getType(), e) <= 0) {
                out.add(e);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ the terrarium box

    /** Whether the terrarium box takes this animal now: a profile that allows it (or {@code terrarium.boxAllow}), the switch on. */
    public static boolean boxTakes(Entity e) {
        if (!enabled() || !(e instanceof Mob) || e instanceof Player) return false;
        ForeignProfile p = ForeignProfiles.of(e.getType());
        if (p != null) return p.land() != null && p.land().box() && Features.enabled(Feature.FOREIGN_CARE);
        return listed(e.getType()) && TankInhabitants.accepts(e.getType(), e) != TankInhabitants.Acceptance.TOO_BIG;
    }

    /**
     * The empty terrarium box on an animal of another mod (before the animal's own interaction, so a mod cannot take the
     * click): one with a box profile goes into the box (out of its tank, if it was in one); a tame one only for its owner.
     * Every other animal is refused with a message (our own land animals catch themselves, a water animal goes into a bucket).
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.TERRARIUM_BOX.get()) || TerrariumBoxItem.isFilled(stack)) return;
        Entity target = event.getTarget();
        if (!(target instanceof LivingEntity le) || target instanceof Player || !le.isAlive()) return;
        if (BettaCraft.MOD_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getNamespace())) return; // ours: their own way
        // villagers, golems, monsters ...: the box is nothing for them, their own click stays
        if (!boxTakes(target) && !(target instanceof net.minecraft.world.entity.animal.Animal) && ForeignProfiles.of(target.getType()) == null) return;
        Player player = event.getEntity();
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (event.getLevel().isClientSide) return;
        Component no = boxRefusal(player, target);
        if (no != null) {
            player.displayClientMessage(no, true);
            return;
        }
        if (TankInhabitants.hasTankRef(target) && event.getLevel() instanceof ServerLevel sl) {
            TankBlockEntity c = TankInhabitants.resolveController(sl, target);
            if (c != null) {
                c.getTank().inhabitants().remove(target.getUUID());
                c.markTankChanged();
            }
        }
        catchInto(player, event.getHand(), target);
    }

    /** Why the box does not take this animal for this player, or null. */
    @Nullable
    public static Component boxRefusal(Player player, Entity target) {
        if (!boxTakes(target)) {
            if (!enabled()) return Component.translatable("message.bettacraft.feature_off");
            ForeignProfile p = ForeignProfiles.of(target.getType());
            if (p != null && p.land() != null && !p.land().box()) return Component.translatable("message.bettacraft.box.too_big", target.getDisplayName());
            return Component.translatable("message.bettacraft.box.refused", target.getDisplayName());
        }
        if (target instanceof OwnableEntity o && o.getOwnerUUID() != null && !o.getOwnerUUID().equals(player.getUUID()) && !player.hasPermissions(2)) {
            return Component.translatable("message.bettacraft.box.not_yours", target.getDisplayName());
        }
        if (TankInhabitants.hasTankRef(target) && player.level() instanceof ServerLevel sl) {
            TankBlockEntity c = TankInhabitants.resolveController(sl, target);
            if (c != null && !TankInhabitants.canModify(player, c.getTank())) return Component.translatable("message.bettacraft.tank.locked");
        }
        return null;
    }

    /** Puts the animal into one empty box of the stack in {@code hand} (removes the entity). */
    public static void catchInto(Player player, net.minecraft.world.InteractionHand hand, Entity target) {
        TankInhabitants.release(target);
        ItemStack filled = TerrariumBoxItem.fillWith(target);
        player.level().playSound(null, target.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        ItemStack held = player.getItemInHand(hand);
        if (player.hasInfiniteMaterials()) {
            if (!player.getInventory().add(filled)) player.drop(filled, false);
        } else if (held.getCount() <= 1) {
            player.setItemInHand(hand, filled);
        } else {
            held.shrink(1);
            if (!player.getInventory().add(filled)) player.drop(filled, false);
        }
        target.discard();
    }

    /** GameTests: walk this animal to a spot now (its walker), if the walker holds it. */
    public static boolean goTo(Mob m, ServerLevel level, TankData t, Vec3 at, int ticks) {
        LandWalker w = walker(m);
        if (w == null) return false;
        w.goTo(m, level, t, at, ticks);
        return true;
    }
}
