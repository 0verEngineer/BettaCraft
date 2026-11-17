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
import org.overengineer.bettacraft.platform.event.EventPriority;
import org.overengineer.bettacraft.platform.event.SubscribeEvent;
import org.overengineer.bettacraft.platform.event.PlayerInteractEvent;
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
import static org.overengineer.bettacraft.foreign.ForeignTerrarium.*;

/** M52: the game event handlers of {@link ForeignTerrarium} (M52: on the common game events, fired by each loader). */
public final class ForeignTerrariumHandlers {
    private ForeignTerrariumHandlers() {}

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
        if (!boxTakes(target) && !(target instanceof net.minecraft.world.entity.animal.Animal) && ForeignProfiles.of(target.getType()) == null
                && ForeignProfiles.refusal(target.getType()) == null) return; // M39: a refused one hears why
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

    /**
     * M39 part b: the large net (and the transport tub) on a big land animal of another mod, before the animal's own interaction
     * (a tame ferret or a lion of a mod would take the click for itself): the net catches it into a tub as it does a big fish.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBigInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        boolean net = stack.getItem() instanceof org.overengineer.bettacraft.big.LargeNetItem;
        boolean tub = stack.getItem() instanceof org.overengineer.bettacraft.big.TransportTubItem && !org.overengineer.bettacraft.big.TransportTubItem.isFilled(stack);
        if (!net && !tub || !(event.getTarget() instanceof LivingEntity le) || !le.isAlive()) return;
        ForeignProfile.Land l = land(le.getType());
        if (l == null || !l.big() || l.box()) return; // the box catches the small ones
        event.setCanceled(true);
        InteractionResult r = stack.getItem().interactLivingEntity(stack, event.getEntity(), le, event.getHand());
        event.setCancellationResult(r == InteractionResult.PASS ? InteractionResult.sidedSuccess(event.getLevel().isClientSide) : r);
    }

    /** M35 part b: a mother that dies in her tank loses her laid eggs (the egg block goes, no empty shell stays). */
    @SubscribeEvent
    public static void onDeath(org.overengineer.bettacraft.platform.event.LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (!e.level().isClientSide && TankInhabitants.hasTankRef(e)) ForeignNests.died(e);
    }
}
