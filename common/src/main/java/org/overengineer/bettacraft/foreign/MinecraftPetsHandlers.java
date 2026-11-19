package org.overengineer.bettacraft.foreign;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.overengineer.bettacraft.platform.event.EventPriority;
import org.overengineer.bettacraft.platform.event.SubscribeEvent;
import org.overengineer.bettacraft.platform.event.LivingDropsEvent;
import org.overengineer.bettacraft.platform.event.LivingExperienceDropEvent;
import org.overengineer.bettacraft.platform.event.PlayerInteractEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.entity.betta.BettaHealth;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankWater;
import org.overengineer.bettacraft.tank.interior.InteriorGround;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.overengineer.bettacraft.terrarium.TerrariumAnimals;
import org.overengineer.bettacraft.terrarium.TerrariumBoxItem;
import static org.overengineer.bettacraft.foreign.MinecraftPets.*;

/** M52: the game event handlers of {@link MinecraftPets} (M52: on the common game events, fired by each loader). */
public final class MinecraftPetsHandlers {
    private MinecraftPetsHandlers() {}

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        if (noDrops(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onExperience(LivingExperienceDropEvent e) {
        if (noDrops(e.getEntity())) {
            e.setDroppedExperience(0);
            e.setCanceled(true);
        }
    }

    /** Shears, a bucket, a bowl or a brush on a pet give nothing (no wool, milk, stew or scute farm). */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        Entity target = event.getTarget();
        if (!(target instanceof Animal) || !noDrops(target)) return;
        ItemStack stack = event.getItemStack();
        if (!farmTool(stack)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!event.getLevel().isClientSide()) {
            event.getEntity().sendOverlayMessage(Component.translatable("message.bettacraft.pet.no_farm", target.getDisplayName()));
        }
    }
}
