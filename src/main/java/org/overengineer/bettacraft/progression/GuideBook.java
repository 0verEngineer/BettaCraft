package org.overengineer.bettacraft.progression;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.compat.guideme.GuideMECompat;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModAttachments;

/**
 * The GuideME handbook (PLAN 9, M6). Each player gets it once, depending on {@code guide.giveBook}: on the first join,
 * or with the first caught betta / first crafted (or smelted) Bettacraft item, or never. With a full inventory the book
 * is dropped at the player's feet and only they can pick it up for {@link #OWNER_ONLY_TICKS}. The "given" flag is set
 * after the delivery (inventory or drop). Without GuideME there is no book and nothing happens.
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class GuideBook {
    public static final ResourceLocation GUIDE_ID = BettaCraft.id("guide");
    public static final int OWNER_ONLY_TICKS = 60 * 20;
    /** Item entity data: game time until which only the target player may pick the dropped book up. */
    public static final String OWNER_UNTIL = "bettacraft:owner_until";

    public enum Trigger { JOIN, CATCH, CRAFT }

    private GuideBook() {}

    /** GuideME is loaded (the guide is registered with it). */
    public static boolean installed() {
        return ModList.get() != null && ModList.get().isLoaded("guideme");
    }

    /** GuideME is loaded and the handbook is on ({@code features.compat.guideme}; off: no book in the tab, none given). */
    public static boolean available() {
        return installed() && org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.COMPAT_GUIDEME);
    }

    /** A handbook stack, or empty without GuideME (or with the handbook off). */
    public static ItemStack createBook() {
        return available() ? GuideMECompat.createBook(GUIDE_ID) : ItemStack.EMPTY;
    }

    public static boolean isBook(ItemStack stack) {
        return installed() && !stack.isEmpty() && GuideMECompat.isBook(stack, GUIDE_ID);
    }

    public static ServerConfig.GiveBook mode() {
        return ServerConfig.getOr(ServerConfig.GUIDE_GIVE_BOOK, ServerConfig.GiveBook.ON_FIRST_CATCH_OR_CRAFT);
    }

    public static boolean wasGiven(ServerPlayer player) {
        return player.getData(ModAttachments.GUIDE_BOOK).given();
    }

    public static void handle(ServerPlayer player, Trigger trigger) {
        handle(player, trigger, mode());
    }

    /** Gives the book if {@code trigger} matches {@code mode} and the player has not had it yet. Returns true if given. */
    public static boolean handle(ServerPlayer player, Trigger trigger, ServerConfig.GiveBook mode) {
        boolean matches = switch (mode) {
            case ON_JOIN -> trigger == Trigger.JOIN;
            case ON_FIRST_CATCH_OR_CRAFT -> trigger == Trigger.CATCH || trigger == Trigger.CRAFT;
            case NEVER -> false;
        };
        if (!matches || wasGiven(player)) return false;
        ItemStack book = createBook();
        if (book.isEmpty()) return false;
        deliver(player, book);
        player.setData(ModAttachments.GUIDE_BOOK, GuideBookState.GIVEN);
        return true;
    }

    /** Into the inventory, or at the player's feet (owner-only pickup for 60 s) when it is full. */
    public static void deliver(ServerPlayer player, ItemStack book) {
        // free slot first: in creative Inventory#add "succeeds" with a full inventory and deletes the stack
        if (player.getInventory().getFreeSlot() >= 0 && player.getInventory().add(book)) {
            player.sendSystemMessage(Component.translatable("message.bettacraft.guide.received").withStyle(ChatFormatting.AQUA));
            return;
        }
        ItemEntity e = new ItemEntity(player.level(), player.getX(), player.getY() + 0.2, player.getZ(), book, 0, 0.1, 0);
        e.setTarget(player.getUUID());
        e.setNoPickUpDelay();
        e.getPersistentData().putLong(OWNER_UNTIL, player.level().getGameTime() + OWNER_ONLY_TICKS);
        player.level().addFreshEntity(e);
        player.sendSystemMessage(Component.translatable("message.bettacraft.guide.dropped", OWNER_ONLY_TICKS / 20).withStyle(ChatFormatting.AQUA));
    }

    /** After the owner-only time anyone may pick the book up. */
    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        ItemEntity item = event.getItemEntity();
        CompoundTag data = item.getPersistentData();
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

    private static void crafted(ServerPlayer player, ItemStack result) {
        if (result.isEmpty()) return;
        if (isBook(result)) {
            // crafted the handbook itself (book + betta pellets): do not hand out a second one later
            player.setData(ModAttachments.GUIDE_BOOK, GuideBookState.GIVEN);
            return;
        }
        if (BettaCraft.MOD_ID.equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result.getItem()).getNamespace())) {
            handle(player, Trigger.CRAFT);
        }
    }
}
