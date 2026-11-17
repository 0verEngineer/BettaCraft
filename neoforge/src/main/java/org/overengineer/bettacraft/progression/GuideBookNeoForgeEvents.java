package org.overengineer.bettacraft.progression;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.compat.guideme.GuideMECompat;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModAttachments;
import static org.overengineer.bettacraft.progression.GuideBook.*;

/** M52: the NeoForge event handlers of {@link GuideBook} (moved here from the common class). */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
final class GuideBookNeoForgeEvents {
    private GuideBookNeoForgeEvents() {}

    /** After the owner-only time anyone may pick the book up. */
    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        ItemEntity item = event.getItemEntity();
        CompoundTag data = org.overengineer.bettacraft.platform.Platform.get().persistentData(item);
        if (!data.contains(OWNER_UNTIL)) return;
        if (item.level().getGameTime() >= data.getLong(OWNER_UNTIL)) {
            item.setTarget(null);
            data.remove(OWNER_UNTIL);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) handle(sp, Trigger.JOIN);
    }

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) crafted(sp, event.getCrafting());
    }

    @SubscribeEvent
    public static void onSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) crafted(sp, event.getSmelting());
    }
}
