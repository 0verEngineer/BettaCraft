package org.overengineer.bettacraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.platform.event.SubscribeEvent;
import org.overengineer.bettacraft.platform.event.PlayerInteractEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;
import static org.overengineer.bettacraft.world.SwampWaterEvents.*;

/** M52: the game event handlers of {@link SwampWaterEvents} (M52: on the common game events, fired by each loader). */
public final class SwampWaterEventsHandlers {
    private SwampWaterEventsHandlers() {}

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.GLASS_BOTTLE)) return;
        Player player = event.getEntity();
        Level level = event.getLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
        if (hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = hit.getBlockPos();
        if (!level.getFluidState(pos).is(FluidTags.WATER)) return;
        var biome = level.getBiome(pos);
        ItemStack result;
        if (biome.is(ModTags.SALT_WATER_BIOMES) && org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.SALTWATER)) result = new ItemStack(ModItems.SALT_WATER_BOTTLE.get()); // M33
        else if (biome.is(ModTags.BETTA_SPAWN_BIOMES)) result = new ItemStack(ModItems.SWAMP_WATER_BOTTLE.get());
        else return;
        if (!level.mayInteract(player, pos)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (level.isClientSide) return;
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
        player.setItemInHand(event.getHand(), ItemUtils.createFilledResult(stack, player, result));
        // M12: swamp water sometimes brings up a bit of duckweed
        // M33: each find only while its feature is on
        if (result.is(ModItems.SWAMP_WATER_BOTTLE.get()) && org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.FLOATING_PLANTS) && level.getRandom().nextDouble()
                < org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.PLANTS_BOTTLE_CHANCE, 0.15)) {
            ItemStack duckweed = org.overengineer.bettacraft.tank.plants.TankPlants.item(org.overengineer.bettacraft.tank.plants.FloatingPlantType.DUCKWEED);
            if (!player.getInventory().add(duckweed)) player.drop(duckweed, false);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.bettacraft.plants.duckweed_in_bottle"), true);
        } else if (result.is(ModItems.SWAMP_WATER_BOTTLE.get()) && org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.AQUATIC_PLANTS) && level.getRandom().nextDouble()
                < org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.AQUATIC_BOTTLE_CHANCE, 0.05)) {
            // M13: or a cutting of a swamp plant (Cryptocoryne, Ludwigia, Rotala)
            var r = level.getRandom();
            var species = org.overengineer.bettacraft.sources.WildTables.BOTTLE_CUTTINGS.pick(r::nextLong); // M22.4: its own table
            ItemStack cutting = org.overengineer.bettacraft.plants.InteriorPlants.item(species, 0);
            if (!player.getInventory().add(cutting)) player.drop(cutting, false);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.bettacraft.aquatic.cutting_in_bottle",
                    cutting.getHoverName()), true);
        }
        // M26: and now and then a few daphnia (the start of a daphnia barrel)
        if (result.is(ModItems.SWAMP_WATER_BOTTLE.get()) && org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.LIVE_FOOD_CULTURES) && level.getRandom().nextDouble()
                < org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.LIVE_FOOD_BOTTLE_DAPHNIA_CHANCE, 0.08)) {
            ItemStack daphnia = new ItemStack(ModItems.DAPHNIA.get());
            if (!player.getInventory().add(daphnia)) player.drop(daphnia, false);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.bettacraft.live_food.daphnia_in_bottle"), true);
        }
    }
}
