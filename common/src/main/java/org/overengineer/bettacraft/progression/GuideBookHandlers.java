package org.overengineer.bettacraft.progression;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.platform.event.SubscribeEvent;
import org.overengineer.bettacraft.platform.event.ItemEntityPickupEvent;
import org.overengineer.bettacraft.platform.event.PlayerEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModAttachments;
import static org.overengineer.bettacraft.progression.GuideBook.*;

/** M52: the game event handlers of {@link GuideBook} (M52: on the common game events, fired by each loader). */
public final class GuideBookHandlers {
    private GuideBookHandlers() {}

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
