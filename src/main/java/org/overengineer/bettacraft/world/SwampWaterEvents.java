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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * A glass bottle used on water whose block lies in a betta spawn biome gives a bottle of swamp water, in an ocean
 * ({@code #bettacraft:salt_water_biomes}) a bottle of salt water (PLAN 6.3).
 * The biome at the position of the clicked water block decides, found by the same fluid ray trace as vanilla. Since M12
 * swamp water sometimes brings up duckweed as well (server config floatingPlants.bottleDuckweedChance).
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class SwampWaterEvents {
    private SwampWaterEvents() {}

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
        if (biome.is(ModTags.SALT_WATER_BIOMES)) result = new ItemStack(ModItems.SALT_WATER_BOTTLE.get());
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
        if (result.is(ModItems.SWAMP_WATER_BOTTLE.get()) && level.getRandom().nextDouble()
                < org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.PLANTS_BOTTLE_CHANCE, 0.15)) {
            ItemStack duckweed = org.overengineer.bettacraft.tank.plants.TankPlants.item(org.overengineer.bettacraft.tank.plants.FloatingPlantType.DUCKWEED);
            if (!player.getInventory().add(duckweed)) player.drop(duckweed, false);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.bettacraft.plants.duckweed_in_bottle"), true);
        }
    }
}
