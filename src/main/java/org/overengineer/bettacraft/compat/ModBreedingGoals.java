package org.overengineer.bettacraft.compat;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.tank.TankInhabitants;

/**
 * M42 (found by the integration suite): mods that give every fish their own breeding by AI goals (Fish Eggs, "Aquatic
 * Breeding": a fed fish falls in love, pairs up and lays an egg item that hatches fish of the type without anything of
 * ours). Our bettas and tankmates are fish too: such young would carry no genes, and the eggs land in the tank glass. Their
 * goals are taken from our own animals always and from the animals of other mods while they live in a tank (with
 * {@code foreignAnimals.suppressOwnBreeding}, like their other breeding). By the goals' package only, no compile dependency.
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class ModBreedingGoals {
    /** Packages of the breeding (and food hunting) goals of other mods. */
    static final List<String> PACKAGES = List.of("dev.cephelo.fisheggs.");

    private ModBreedingGoals() {}

    /** Whether the goal belongs to a mod's own fish breeding. */
    public static boolean breedingGoal(Object goal) {
        String n = goal.getClass().getName();
        for (String p : PACKAGES) if (n.startsWith(p)) return true;
        return false;
    }

    /** Removes the goals of those mods from the mob (both selectors: Fish Eggs uses the target selector); true when there were any. */
    public static boolean strip(Mob m) {
        boolean had = false;
        for (var sel : new net.minecraft.world.entity.ai.goal.GoalSelector[]{m.goalSelector, m.targetSelector}) {
            if (sel.getAvailableGoals().stream().noneMatch(w -> breedingGoal(w.getGoal()))) continue;
            sel.removeAllGoals(ModBreedingGoals::breedingGoal);
            had = true;
        }
        return had;
    }

    static boolean ours(Entity e) {
        return BettaCraft.MOD_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace());
    }

    /** After the mods added their goals (they do it on joining the level, at normal priority). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob m)) return;
        if (ours(m) || Features.enabled(Feature.SUPPRESS_FOREIGN_BREEDING) && TankInhabitants.hasTankRef(m)) strip(m);
    }
}
