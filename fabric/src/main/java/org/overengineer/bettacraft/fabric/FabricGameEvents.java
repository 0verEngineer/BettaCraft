package org.overengineer.bettacraft.fabric;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.platform.event.*;

/**
 * M52: the common game events ({@link GameEvents}) from Fabric's callbacks; the events Fabric API does not have come from
 * the mixins of {@code fabric/mixin} (they call {@link #post}). Fabric has no priorities of its own: the handlers run
 * highest priority first.
 */
public final class FabricGameEvents {
    private FabricGameEvents() {}

    /** Posts an event if any handler listens (the mixins use this). */
    public static <E extends GameEvent> E post(E e) {
        return GameEvents.post(e);
    }

    public static boolean listens(Class<? extends GameEvent> type) {
        return GameEvents.has(type);
    }

    static void register() {
        ServerTickEvents.END_SERVER_TICK.register(s -> post(new ServerTickEvent.Post(s)));
        ServerTickEvents.END_WORLD_TICK.register(l -> post(new LevelTickEvent.Post(l)));
        ServerLifecycleEvents.SERVER_STARTING.register(s -> post(new ServerAboutToStartEvent(s)));
        ServerLifecycleEvents.SERVER_STARTED.register(s -> post(new ServerStartedEvent(s)));
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> post(new ServerStoppedEvent(s)));
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((p, joined) -> post(new OnDatapackSyncEvent(Stream.of(p))));
        ServerPlayConnectionEvents.JOIN.register((h, sender, s) -> post(new PlayerEvent.PlayerLoggedInEvent(h.player)));
        ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> post(new PlayerEvent.PlayerLoggedOutEvent(h.player)));
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> post(new TagsUpdatedEvent(
                client ? TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED : TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD)));
        CommandRegistrationCallback.EVENT.register((dispatcher, context, env) -> post(new RegisterCommandsEvent(dispatcher, context)));

        // EntityInteract: in Player#interactOn like on NeoForge (fabric/mixin/PlayerInteractMixin), not in Fabric's
        // UseEntityCallback (only the packet handler calls that)
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            var e = post(new PlayerInteractEvent.RightClickBlock(player, hand, hit.getBlockPos(), hit));
            if (e.isCanceled()) return orFail(e.getCancellationResult());
            // no block use (beds in the interior): nothing happens, like NeoForge without the block's own action
            if (e.getUseBlock().isFalse()) return InteractionResult.FAIL;
            // NeoForge's useBlock TRUE: the block is used even while sneaking (a filled bucket into a tank)
            if (e.getUseBlock().isTrue() && player.isSecondaryUseActive()) {
                var state = level.getBlockState(hit.getBlockPos());
                var stack = player.getItemInHand(hand);
                var r = state.useItemOn(stack, level, player, hand, hit);
                if (r.consumesAction()) return r.result();
                if (r == net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION && hand == net.minecraft.world.InteractionHand.MAIN_HAND) {
                    var r2 = state.useWithoutItem(level, player, hit);
                    if (r2.consumesAction()) return r2;
                }
                if (e.getUseItem().isFalse()) return InteractionResult.PASS;
            }
            // NeoForge's onItemUseFirst: the item before the block (the crank on a spring)
            var stack = player.getItemInHand(hand);
            if (e.getUseItem().isTrue() || !e.getUseItem().isFalse()) {
                if (stack.getItem() instanceof org.overengineer.bettacraft.platform.UseFirstItem first) {
                    InteractionResult r = first.onItemUseFirst(stack, new net.minecraft.world.item.context.UseOnContext(player, hand, hit));
                    if (r != InteractionResult.PASS) return r;
                }
            }
            return InteractionResult.PASS;
        });
        UseItemCallback.EVENT.register((player, level, hand) -> {
            var e = post(new PlayerInteractEvent.RightClickItem(player, hand));
            if (!e.isCanceled()) return InteractionResultHolder.pass(player.getItemInHand(hand));
            InteractionResult r = orFail(e.getCancellationResult());
            return new InteractionResultHolder<>(r, player.getItemInHand(hand));
        });
        AttackBlockCallback.EVENT.register((player, level, hand, pos, dir) -> {
            var e = post(new PlayerInteractEvent.LeftClickBlock(player, pos));
            return e.isCanceled() ? InteractionResult.FAIL : InteractionResult.PASS;
        });
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, be) -> !post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled());
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> post(new EntityLeaveLevelEvent(entity, level)));
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> !post(new LivingDeathEvent(entity, source)).isCanceled());
        ServerLivingEntityEvents.MOB_CONVERSION.register((from, to, keep) -> post(new LivingConversionEvent.Post(from, to)));
        reloadListeners();
        trades();
    }

    /** A cancelled interaction answers with the handler's result; PASS would let vanilla go on, so it fails instead. */
    private static InteractionResult orFail(InteractionResult r) {
        return r == InteractionResult.PASS ? InteractionResult.FAIL : r;
    }

    /** The data reload listeners of the handlers (asked once; NeoForge asks at every reload, ours are stateless factories). */
    private static void reloadListeners() {
        var e = post(new AddReloadListenerEvent());
        int i = 0;
        for (PreparableReloadListener l : e.getListeners()) {
            ResourceLocation id = BettaCraft.id("reload_" + i++);
            ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new IdentifiableResourceReloadListener() {
                @Override
                public ResourceLocation getFabricId() {
                    return id;
                }

                @Override
                public java.util.concurrent.CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager rm, ProfilerFiller p1,
                        ProfilerFiller p2, java.util.concurrent.Executor bg, java.util.concurrent.Executor game) {
                    return l.reload(barrier, rm, p1, p2, bg, game);
                }
            });
        }
    }

    /** The trades the handlers add, per profession and level (Fabric adds, it cannot remove). */
    private static void trades() {
        if (GameEvents.has(VillagerTradesEvent.class)) {
            for (VillagerProfession prof : BuiltInRegistries.VILLAGER_PROFESSION) {
                var map = new Int2ObjectOpenHashMap<List<VillagerTrades.ItemListing>>();
                for (int lvl = 1; lvl <= 5; lvl++) map.put(lvl, new ArrayList<>());
                post(new VillagerTradesEvent(prof, map));
                for (int lvl = 1; lvl <= 5; lvl++) {
                    List<VillagerTrades.ItemListing> added = map.get(lvl);
                    if (!added.isEmpty()) TradeOfferHelper.registerVillagerOffers(prof, lvl, list -> list.addAll(added));
                }
            }
        }
        if (GameEvents.has(WandererTradesEvent.class)) {
            List<VillagerTrades.ItemListing> generic = new ArrayList<>(), rare = new ArrayList<>();
            post(new WandererTradesEvent(generic, rare));
            if (!generic.isEmpty()) TradeOfferHelper.registerWanderingTraderOffers(1, list -> list.addAll(generic));
            if (!rare.isEmpty()) TradeOfferHelper.registerWanderingTraderOffers(2, list -> list.addAll(rare));
        }
    }
}
