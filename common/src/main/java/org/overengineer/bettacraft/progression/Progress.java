package org.overengineer.bettacraft.progression;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.genetics.BettaTypes;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankTemperature;

/**
 * Progression events (M6): fires the {@code bettacraft:event} advancement criterion and hands catches/crafts to the
 * handbook. Events that happen in a tank without a player (nest, spawning, growing up) go to the tank owner (if online)
 * and every player within {@link #NEAR} blocks of the tank.
 */
public final class Progress {
    public static final double NEAR = 32.0;

    public static final String CATCH_BETTA = "catch_betta";
    public static final String TANK_BUILT = "tank_built";
    public static final String TANK_FILLED = "tank_filled";
    public static final String INSERTED = "inserted";
    public static final String IDEAL_TEMPERATURE = "ideal_temperature";
    public static final String FILTER = "filter_attached";
    public static final String FEEDER = "feeder";
    public static final String RECOVERED = "recovered";
    public static final String FIGHT = "fight";
    public static final String NEST_BUILT = "nest_built";
    public static final String SPAWNED = "spawned";
    public static final String FRY_FREE = "fry_free";
    public static final String GROWN_UP = "grown_up";
    public static final String VARIETY = "variety_bred";
    public static final String ENTER_INTERIOR = "enter_interior";
    public static final String DECOR = "decor_placed";
    public static final String SUBSTRATE = "substrate";
    /** M9: fresh water poured into a tank with used water (value = quality afterwards in %). */
    public static final String WATER_CHANGE = "water_change";
    /** M9: live food taken out of a larva tub or an artemia hatchery. */
    public static final String FOOD_BRED = "food_bred";
    /** M11: a male betta flared at the player's mirror stick. */
    public static final String MIRROR = "mirror_flare";
    /** M21.6: a bred guppy or endler of a named strain grew up ({@code lb_strain}, and {@code lb_strain_<id>} per strain). */
    public static final String LB_STRAIN = "lb_strain";
    /** M21.6: a bred endler x guppy hybrid grew up. */
    public static final String LB_HYBRID = "lb_hybrid";
    // M45: the events of the new advancements (the advancement ids are the same words)
    /** A tankmate fish was put into a tank. */
    public static final String TANKMATE = "tankmate_inserted";
    /** Livebearers gave birth in a tank (log tm.born / tm.born_box). */
    public static final String LITTER = "livebearer_litter";
    /** A male seahorse released fry from his pouch (log tm.pouch_born). */
    public static final String SEAHORSE_FRY = "seahorse_fry";
    /** Young shrimp, snails or crayfish were born (log critters_born). */
    public static final String CRITTERS_BRED = "critters_bred";
    /** Algae scraped off the glass (log algae_scraped). */
    public static final String ALGAE_SCRAPED = "algae_scraped";
    /** A clutch of a terrarium animal (geckos, snakes, frogs) hatched. */
    public static final String HERP_CLUTCH = "herp_clutch";
    /** A turtle clutch hatched (the nest temperature decides the sexes). */
    public static final String TURTLE_CLUTCH = "turtle_clutch";
    /** The tank menu of a paludarium was opened. */
    public static final String PALUDARIUM = "paludarium";
    /** A sea turtle was put into a tank. */
    public static final String SEA_TURTLE = "sea_turtle";
    /** A peaceful Minecraft animal moved into a terrarium or paludarium. */
    public static final String PET = "pet_terrarium";
    /** An axolotl or an animal of another mod with a profile was put into a tank. */
    public static final String FOREIGN = "foreign_animal";
    /** A mob went into a tank (mob jar, bait). */
    public static final String MOB_IN_GLASS = "mob_in_glass";
    /** A big animal (by the size rule) was put into a tank. */
    public static final String BIG_ANIMAL = "big_animal";
    /** A multiblock filter cleans the water of two or more tanks (value = tanks). */
    public static final String SHARED_FILTER = "shared_filter";
    /** The water change controller changed water (log auto_water_change). */
    public static final String AUTO_WATER_CHANGE = "auto_water_change";
    /** The tank menu of a tank with a fish on a varied diet was opened. */
    public static final String VARIED_DIET = "varied_diet";
    /** A device was turned below 100 % with its power regulator. */
    public static final String REGULATOR = "regulator";
    /** M53: a player was added to the users of a tank (users tab). */
    public static final String SHARED_TANK = "shared_tank";
    /** The daylight timer of a tank's lights was switched on. */
    public static final String LIGHT_TIMER = "light_timer";
    /** The tank menu of a tank with FE in its buffer while a power option is on was opened. */
    public static final String POWERED = "powered";
    /** A device was wound with the crank. */
    public static final String WOUND = "wound_up";
    /** A turning Create shaft drives a tank (drive adapter or cabinet). */
    public static final String CREATE_DRIVE = "create_drive";
    /** A tank arrived somewhere else in one piece (Create contraption, Sable sub-level). */
    public static final String TRAVELLED = "travelling_tank";
    /** M50: the devices of a travelling tank ran on the power of its contraption or airship. */
    public static final String POWERED_TRIP = "powered_trip";
    // M47
    /** The first tadpoles of the dwarf clawed frogs hatched in a tank (log frog.tadpoles_hatched). */
    public static final String FROG_TADPOLE = "frog_tadpole";
    /** A trade with an aquarist villager. */
    public static final String AQUARIST_TRADE = "aquarist_trade";
    // M48
    /** A bred tetra or cory of an M48 bred form grew up. */
    public static final String TETRA_CORY_FORM = "tetra_cory_form";
    /** A mandarin lived on the copepods of the live rock (its first meal of them in a tank). */
    public static final String MANDARIN_COPEPODS = "mandarin_copepods";
    /** Clownfish larvae raised on rotifers settled as young fish (log tm.clown_larvae_raised). */
    public static final String CLOWN_LARVAE = "clown_larvae";
    /** A Banggai male released his young from his mouth (log tm.banggai_born). */
    public static final String BANGGAI_YOUNG = "banggai_young";
    // M49
    /** A sump sits in a cabinet under a tank (menu opened). */
    public static final String SUMP = "sump_installed";
    /** A calcium reactor dissolves coral rubble into a tank (players near it). */
    public static final String CALCIUM_REACTOR = "calcium_reactor";
    /** An archerfish shot an insect down. */
    public static final String ARCHER_SHOT = "archerfish_shot";
    /** An archerfish splashed the player. */
    public static final String ARCHER_SPLASH = "archerfish_splash";
    /** A mudskipper lives in a brackish paludarium (menu opened). */
    public static final String MUDSKIPPER = "mudskipper_paludarium";
    /** A mangrove in a tank grew its crown over the water (log mangrove.crown). */
    public static final String MANGROVE = "mangrove_grown";
    /** M56: a terrarium plant grew to full size in a tank (log terrarium_plant.grown). */
    public static final String TERRARIUM_PLANT = "terrarium_plant_grown";
    /** M56: dart frog tadpoles went into the cups of bromeliads (log terrarium_plant.tadpoles_in_bromeliads). */
    public static final String BROMELIAD_TADPOLE = "bromeliad_tadpole";
    /** Amano larvae settled as young shrimp in brackish water (log amano.young). */
    public static final String AMANO_YOUNG = "amano_young";
    // M51
    /** A player hired the aquarist who came to him. */
    public static final String AQUARIST_HIRED = "aquarist_hired";
    /** M51: the breeding events (the hired aquarist comes after one, and after a refusal only after another one). */
    static final java.util.Set<String> BREEDING_EVENTS = java.util.Set.of(SPAWNED, FRY_FREE, LITTER, SEAHORSE_FRY, CRITTERS_BRED, HERP_CLUTCH, TURTLE_CLUTCH,
            FROG_TADPOLE, CLOWN_LARVAE, BANGGAI_YOUNG, AMANO_YOUNG);

    private Progress() {}

    public static void award(@Nullable Player player, String event, int value) {
        if (player instanceof ServerPlayer sp && !(sp.level().isClientSide())) {
            if (BREEDING_EVENTS.contains(event)) org.overengineer.bettacraft.aquarist.AquaristArrival.onBreeding(sp); // M51
            try {
                ModTriggers.EVENT.get().trigger(sp, event, value);
            } catch (RuntimeException e) {
                // advancements must never break game logic (e.g. mock players without advancement data)
                org.slf4j.LoggerFactory.getLogger("bettacraft/progress").debug("advancement event {} failed: {}", event, e.toString());
            }
        }
    }

    public static void award(@Nullable Player player, String event) {
        award(player, event, 0);
    }

    /** Owner (if online) plus players near the tank. */
    public static Set<ServerPlayer> nearTank(ServerLevel level, TankData tank) {
        Set<ServerPlayer> out = new LinkedHashSet<>();
        UUID owner = tank.owner();
        if (owner != null) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(owner);
            if (p != null) out.add(p);
        }
        Vec3 c = tank.min() == null ? null : new Vec3((tank.min().getX() + tank.max().getX() + 1) / 2.0,
                (tank.min().getY() + tank.max().getY() + 1) / 2.0, (tank.min().getZ() + tank.max().getZ() + 1) / 2.0);
        if (c != null) {
            for (ServerPlayer p : level.players()) if (p.distanceToSqr(c) <= NEAR * NEAR) out.add(p);
        }
        return out;
    }

    public static void awardNear(ServerLevel level, TankBlockEntity controller, String event, int value) {
        for (ServerPlayer p : nearTank(level, controller.getTank())) award(p, event, value);
    }

    /** Maps tank log entries (TankCare.log) to events. */
    public static void onTankLog(ServerLevel level, TankBlockEntity controller, String key) {
        String event = switch (key) {
            case "log.bettacraft.nest_built" -> NEST_BUILT;
            case "log.bettacraft.spawned" -> SPAWNED;
            case "log.bettacraft.fry_free" -> FRY_FREE;
            case "log.bettacraft.fight" -> FIGHT;
            case "log.bettacraft.recovered" -> RECOVERED;
            case "log.bettacraft.tm.born", "log.bettacraft.tm.born_box" -> LITTER; // M45
            case "log.bettacraft.tm.pouch_born" -> SEAHORSE_FRY;
            case "log.bettacraft.critters_born" -> CRITTERS_BRED;
            case "log.bettacraft.algae_scraped" -> ALGAE_SCRAPED;
            case "log.bettacraft.auto_water_change" -> AUTO_WATER_CHANGE;
            case "log.bettacraft.frog.tadpoles_hatched" -> FROG_TADPOLE; // M47
            case "log.bettacraft.tm.clown_larvae_raised" -> CLOWN_LARVAE; // M48
            case "log.bettacraft.tm.banggai_born" -> BANGGAI_YOUNG;
            case "log.bettacraft.mangrove.crown" -> MANGROVE; // M49
            case "log.bettacraft.amano.young" -> AMANO_YOUNG;
            case "log.bettacraft.terrarium_plant.grown" -> TERRARIUM_PLANT; // M56
            case "log.bettacraft.terrarium_plant.tadpoles_in_bromeliads" -> BROMELIAD_TADPOLE;
            default -> null;
        };
        if (event != null) awardNear(level, controller, event, 0);
    }

    /** A betta in a tank became adult: generation and (for bred fish) a named variety. */
    public static void onGrownUp(ServerLevel level, TankBlockEntity controller, BettaEntity fish) {
        int generation = fish.getBettaData().generation();
        awardNear(level, controller, GROWN_UP, generation);
        if (generation > 0) {
            var v = BettaTypes.variety(fish.getBettaData().genome());
            if (v != null && v.includeForm()) awardNear(level, controller, VARIETY, generation);
        }
    }

    /** M21.6: a guppy or endler in a tank became adult: a bred one of a named strain or a hybrid counts for the breeding progress. */
    public static void onTankmateGrownUp(ServerLevel level, TankBlockEntity controller, org.overengineer.bettacraft.entity.tankmate.TankmateEntity fish) {
        if (fish.generation() <= 0) return;
        // M48 part a: a bred form of a tetra or a cory
        if (fish.species().m48Forms() && !fish.variety().equals(fish.species().variety(0))) awardNear(level, controller, TETRA_CORY_FORM, fish.generation());
        var p = fish.livebearerPhenotype();
        if (p == null) return;
        if (p.hybrid()) {
            awardNear(level, controller, LB_HYBRID, fish.generation());
            return;
        }
        var strain = org.overengineer.bettacraft.genetics.livebearer.LivebearerStrain.of(p);
        if (strain == null) return;
        awardNear(level, controller, LB_STRAIN, fish.generation());
        awardNear(level, controller, LB_STRAIN + "_" + strain.id(), fish.generation());
    }

    /** A player caught a betta with a bucket or a fish bag. */
    public static void onBettaCaught(Player player) {
        award(player, CATCH_BETTA);
        if (player instanceof ServerPlayer sp) GuideBook.handle(sp, GuideBook.Trigger.CATCH);
    }

    /** A player opened the tank menu: checks the conditions that are states rather than events. */
    public static void onMenuOpened(ServerPlayer player, TankBlockEntity controller) {
        TankData t = controller.getTank();
        award(player, TANK_BUILT, t.memberCount());
        if (!org.overengineer.bettacraft.tank.TankCare.bettas(player.level(), t).isEmpty()
                && TankTemperature.isOptimal(t.waterTemp())) {
            award(player, IDEAL_TEMPERATURE);
        }
        if (!TankEquipment.filters(player.level(), controller).isEmpty()) award(player, FILTER);
        if (!org.overengineer.bettacraft.equipment.Sump.hosts(player.level(), controller).isEmpty()) award(player, SUMP); // M49
        if (org.overengineer.bettacraft.terrarium.Mudskippers.happy(player.level(), t)) award(player, MUDSKIPPER); // M49
        // M45: states of the tank
        if (t.terrariumMode() == org.overengineer.bettacraft.terrarium.TerrariumMode.PALUDARIUM) award(player, PALUDARIUM);
        if (org.overengineer.bettacraft.tank.TankPower.anyRequired() && t.powerBuffer() > 0) award(player, POWERED);
        if (org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.FOOD_VARIETY)) {
            var diet = org.overengineer.bettacraft.food.FoodDiet.params();
            for (var e : org.overengineer.bettacraft.tank.TankInhabitants.liveInhabitants(player.level(), t)) {
                org.overengineer.bettacraft.entity.betta.BettaHealth h = e instanceof BettaEntity b ? b.health()
                        : e instanceof org.overengineer.bettacraft.entity.tankmate.TankmateEntity m ? m.health() : null;
                if (h != null && h.varied(diet)) {
                    award(player, VARIED_DIET);
                    break;
                }
            }
        }
    }

    /**
     * M45: an animal went into a tank ({@code big}: by the size rule of the big animals): the advancements of the tankmates,
     * the big animals, the mobs in glass, the Minecraft pets, the sea turtles and the foreign animals. Without a player (a
     * lured pet) the players near the tank.
     */
    public static void onInserted(@Nullable Player player, ServerLevel level, TankBlockEntity controller, net.minecraft.world.entity.Entity e, boolean big) {
        String ev = insertEvent(e, big);
        if (ev == null) return;
        if (player != null) award(player, ev);
        else awardNear(level, controller, ev, 0);
    }

    /** M45: the advancement event of an animal that went into a tank, or null. */
    @Nullable
    public static String insertEvent(net.minecraft.world.entity.Entity e, boolean big) {
        var type = e.getType();
        if (org.overengineer.bettacraft.mobs.GlassMobs.isGlassMob(e) || org.overengineer.bettacraft.mobs.GlassMobs.isAllowed(type)) return MOB_IN_GLASS;
        if (org.overengineer.bettacraft.foreign.ForeignProfiles.pet(type)) return PET;
        if (type == net.minecraft.world.entity.EntityType.TURTLE) return SEA_TURTLE;
        if (big) return BIG_ANIMAL;
        if (e instanceof org.overengineer.bettacraft.entity.tankmate.TankmateEntity) return TANKMATE;
        if (type == net.minecraft.world.entity.EntityType.AXOLOTL) return FOREIGN;
        String ns = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace();
        if (!org.overengineer.bettacraft.BettaCraft.MOD_ID.equals(ns) && org.overengineer.bettacraft.foreign.ForeignProfiles.of(type) != null) return FOREIGN;
        return null;
    }
}
