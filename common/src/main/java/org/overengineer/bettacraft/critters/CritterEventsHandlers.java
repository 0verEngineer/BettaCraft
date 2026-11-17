package org.overengineer.bettacraft.critters;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.overengineer.bettacraft.platform.event.EventPriority;
import org.overengineer.bettacraft.platform.event.SubscribeEvent;
import org.overengineer.bettacraft.platform.event.PlayerInteractEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankFrogs;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.algae.TankAlgae;
import static org.overengineer.bettacraft.critters.CritterEvents.*;

/** M52: the game event handlers of {@link CritterEvents} (M52: on the common game events, fired by each loader). */
public final class CritterEventsHandlers {
    private CritterEventsHandlers() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        Level level = e.getLevel();
        BlockPos pos = e.getPos();
        Player player = e.getEntity();
        ItemStack stack = e.getItemStack();
        if (level.getBlockState(pos).getBlock() instanceof TankBlock) {
            TankBlockEntity c = TankMultiblock.getController(level, pos);
            if (c == null || c.getTank() == null) return;
            if (stack.is(ModItems.ALGAE_SCRAPER.get())) {
                handled(e);
                if (!(level instanceof ServerLevel server)) return;
                if (!TankInhabitants.canModify(player, c.getTank())) {
                    player.displayClientMessage(Component.translatable("message.bettacraft.tank.locked"), true);
                    return;
                }
                boolean side = player.isSecondaryUseActive();
                int n = TankAlgae.scrape(server, c, pos, e.getFace() == null ? player.getDirection().getOpposite() : e.getFace(), side);
                if (n <= 0) {
                    player.displayClientMessage(Component.translatable("message.bettacraft.algae.clean"), true);
                    return;
                }
                if (!player.getAbilities().instabuild) stack.hurtAndBreak(n, player, e.getHand().asEquipmentSlot());
                level.playSound(null, pos, SoundEvents.BRUSH_GENERIC, SoundSource.BLOCKS, 0.8F, 1.2F);
                player.displayClientMessage(Component.translatable("message.bettacraft.algae.scraped", n).withStyle(ChatFormatting.GREEN), true);
                org.overengineer.bettacraft.tank.TankCare.log(server, c, "log.bettacraft.algae_scraped", org.overengineer.bettacraft.tank.TankCare.nameArg(player),
                        String.valueOf(n));
                return;
            }
            // sneak + food: straight to the frogs (the bettas would snatch it away)
            if (player.isSecondaryUseActive() && org.overengineer.bettacraft.food.BettaFood.kind(stack) != null) {
                if (level instanceof ServerLevel server) {
                    if (TankFrogs.frogs(server, c.getTank()).isEmpty() || !TankFrogs.eats(org.overengineer.bettacraft.food.BettaFood.kind(stack))) return;
                    handled(e);
                    if (TankFrogs.feedTargeted(server, c, player, stack)) {
                        player.displayClientMessage(Component.translatable("message.bettacraft.frogs.fed").withStyle(ChatFormatting.GREEN), true);
                        level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.3F, 1.6F);
                    }
                } else if (c.getTank().inhabitantCount() > 0) {
                    handled(e);
                }
            }
            return;
        }
        // shovel on mud in a swamp: a trumpet snail now and then
        if (stack.getItem() instanceof ShovelItem && level.getBlockState(pos).is(Blocks.MUD) && level instanceof ServerLevel server) {
            var biome = level.getBiome(pos);
            if (!org.overengineer.bettacraft.world.BiomeWaters.swamp(biome) && !org.overengineer.bettacraft.world.BiomeWaters.mangrove(biome)) return;
            if (player.getCooldowns().isOnCooldown(stack)) return;
            player.getCooldowns().addCooldown(stack, 20);
            if (!CritterSpecies.TRUMPET_SNAIL.available()) return; // M33: features.invertebrates
            if (server.getRandom().nextDouble() >= ServerConfig.getOr(ServerConfig.CRITTERS_MUD_CHANCE, 0.08)) return;
            var rnd = NetCatching.random(server);
            ItemStack bag = TankCritters.bagOf(CritterSpecies.TRUMPET_SNAIL, 1, () -> CritterGenes.WILD, rnd, false);
            NetCatching.give(player, bag, Component.translatable(CritterSpecies.TRUMPET_SNAIL.translationKey()));
        }
    }

    static void handled(PlayerInteractEvent.RightClickBlock e) {
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
    }
}
