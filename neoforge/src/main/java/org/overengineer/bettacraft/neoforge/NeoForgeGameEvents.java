package org.overengineer.bettacraft.neoforge;

import java.util.Map;
import java.util.Set;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.overengineer.bettacraft.platform.event.*;

/**
 * M52: NeoForge's game events as the common ones ({@link GameEvents}): one listener per event type and priority the
 * common handlers use (so they keep their place among other mods' listeners), cancellation and results copied back.
 * Generated list (scratch gen_neo_adapter.py); a new common event type needs its line here and in FabricGameEvents.
 */
final class NeoForgeGameEvents {
    private NeoForgeGameEvents() {}

    static void register() {
        Map<Class<?>, Set<EventPriority>> types = GameEvents.types();
        for (EventPriority p : types.getOrDefault(ServerTickEvent.Post.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.tick.ServerTickEvent.Post.class, e -> {
                var s = new ServerTickEvent.Post(e.getServer());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(ServerStartedEvent.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.server.ServerStartedEvent.class, e -> {
                var s = new ServerStartedEvent(e.getServer());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(ServerStoppedEvent.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.server.ServerStoppedEvent.class, e -> {
                var s = new ServerStoppedEvent(e.getServer());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(ServerAboutToStartEvent.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.server.ServerAboutToStartEvent.class, e -> {
                var s = new ServerAboutToStartEvent(e.getServer());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(LevelTickEvent.Post.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.tick.LevelTickEvent.Post.class, e -> {
                var s = new LevelTickEvent.Post(e.getLevel());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(PlayerTickEvent.Post.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.tick.PlayerTickEvent.Post.class, e -> {
                var s = new PlayerTickEvent.Post(e.getEntity());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(EntityTickEvent.Post.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.tick.EntityTickEvent.Post.class, e -> {
                var s = new EntityTickEvent.Post(e.getEntity());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(PlayerEvent.PlayerLoggedInEvent.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent.class, e -> {
                var s = new PlayerEvent.PlayerLoggedInEvent(e.getEntity());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(PlayerEvent.PlayerLoggedOutEvent.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent.class, e -> {
                var s = new PlayerEvent.PlayerLoggedOutEvent(e.getEntity());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(PlayerEvent.ItemCraftedEvent.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent.class, e -> {
                var s = new PlayerEvent.ItemCraftedEvent(e.getEntity(), e.getCrafting());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(PlayerEvent.ItemSmeltedEvent.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemSmeltedEvent.class, e -> {
                var s = new PlayerEvent.ItemSmeltedEvent(e.getEntity(), e.getSmelting());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(PlayerInteractEvent.EntityInteract.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract.class, e -> {
                var s = new PlayerInteractEvent.EntityInteract(e.getEntity(), e.getHand(), e.getTarget());
                fire(e, s, p);
                if (s.isCanceled()) e.setCancellationResult(s.getCancellationResult());
            });
        }
        for (EventPriority p : types.getOrDefault(PlayerInteractEvent.RightClickBlock.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock.class, e -> {
                var s = new PlayerInteractEvent.RightClickBlock(e.getEntity(), e.getHand(), e.getPos(), e.getHitVec());
                fire(e, s, p);
                if (s.isCanceled()) e.setCancellationResult(s.getCancellationResult());
            if (!s.getUseBlock().isDefault()) e.setUseBlock(tri(s.getUseBlock()));
            if (!s.getUseItem().isDefault()) e.setUseItem(tri(s.getUseItem()));
            });
        }
        for (EventPriority p : types.getOrDefault(PlayerInteractEvent.RightClickItem.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem.class, e -> {
                var s = new PlayerInteractEvent.RightClickItem(e.getEntity(), e.getHand());
                fire(e, s, p);
                if (s.isCanceled()) e.setCancellationResult(s.getCancellationResult());
            });
        }
        for (EventPriority p : types.getOrDefault(PlayerInteractEvent.LeftClickBlock.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.class, e -> {
                var s = new PlayerInteractEvent.LeftClickBlock(e.getEntity(), e.getPos());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(EntityJoinLevelEvent.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.entity.EntityJoinLevelEvent.class, e -> {
                var s = new EntityJoinLevelEvent(e.getEntity(), e.getLevel(), e.loadedFromDisk());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(EntityLeaveLevelEvent.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent.class, e -> {
                var s = new EntityLeaveLevelEvent(e.getEntity(), e.getLevel());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(BlockEvent.BreakEvent.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.level.BlockEvent.BreakEvent.class, e -> {
                var s = new BlockEvent.BreakEvent(e.getLevel(), e.getPos(), e.getState(), e.getPlayer());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(BlockEvent.EntityPlaceEvent.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent.class, e -> {
                var s = new BlockEvent.EntityPlaceEvent(e.getLevel(), e.getPos(), e.getPlacedBlock(), e.getEntity());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(LivingDeathEvent.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.entity.living.LivingDeathEvent.class, e -> {
                var s = new LivingDeathEvent(e.getEntity(), e.getSource());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(LivingDropsEvent.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.entity.living.LivingDropsEvent.class, e -> {
                var s = new LivingDropsEvent(e.getEntity());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(LivingExperienceDropEvent.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent.class, e -> {
                var s = new LivingExperienceDropEvent(e.getEntity(), e.getDroppedExperience());
                fire(e, s, p);
                e.setDroppedExperience(s.getDroppedExperience());
            });
        }
        for (EventPriority p : types.getOrDefault(LivingConversionEvent.Pre.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.entity.living.LivingConversionEvent.Pre.class, e -> {
                var s = new LivingConversionEvent.Pre(e.getEntity(), e.getOutcome());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(LivingConversionEvent.Post.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.entity.living.LivingConversionEvent.Post.class, e -> {
                var s = new LivingConversionEvent.Post(e.getEntity(), e.getOutcome());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(LivingDamageEvent.Pre.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre.class, e -> {
                var s = new LivingDamageEvent.Pre(e.getEntity(), e.getSource(), e.getNewDamage());
                fire(e, s, p);
                e.setNewDamage(s.getNewDamage());
            });
        }
        for (EventPriority p : types.getOrDefault(LivingChangeTargetEvent.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent.class, e -> {
                var s = new LivingChangeTargetEvent(e.getEntity(), e.getNewAboutToBeSetTarget());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(MobDespawnEvent.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.entity.living.MobDespawnEvent.class, e -> {
                var s = new MobDespawnEvent(e.getEntity());
                fire(e, s, p);
                if (s.getResult() != MobDespawnEvent.DespawnResult.DEFAULT) e.setResult(s.getResult() == MobDespawnEvent.DespawnResult.ALLOW
                    ? net.neoforged.neoforge.event.entity.living.MobDespawnEvent.Result.ALLOW : net.neoforged.neoforge.event.entity.living.MobDespawnEvent.Result.DENY);
            });
        }
        for (EventPriority p : types.getOrDefault(BabyEntitySpawnEvent.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent.class, e -> {
                var s = new BabyEntitySpawnEvent(e.getParentA(), e.getParentB());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(ItemFishedEvent.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.entity.player.ItemFishedEvent.class, e -> {
                var s = new ItemFishedEvent(e.getEntity(), e.getHookEntity());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(ItemEntityPickupEvent.Pre.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre.class, e -> {
                var s = new ItemEntityPickupEvent.Pre(e.getPlayer(), e.getItemEntity());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(TradeWithVillagerEvent.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent.class, e -> {
                var s = new TradeWithVillagerEvent(e.getEntity(), e.getAbstractVillager());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(ExplosionEvent.Detonate.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.level.ExplosionEvent.Detonate.class, e -> {
                var s = new ExplosionEvent.Detonate(e.getLevel(), e.getAffectedBlocks());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(PistonEvent.Pre.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.level.PistonEvent.Pre.class, e -> {
                var s = new PistonEvent.Pre(e.getLevel(), e.getPos(), e.getDirection(), e.getPistonMoveType().isExtend);
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(TagsUpdatedEvent.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.TagsUpdatedEvent.class, e -> {
                var s = new TagsUpdatedEvent(e.getUpdateCause() == net.neoforged.neoforge.event.TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED ? TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED : TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD);
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(OnDatapackSyncEvent.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.OnDatapackSyncEvent.class, e -> {
                var s = new OnDatapackSyncEvent(e.getRelevantPlayers());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(RegisterCommandsEvent.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.RegisterCommandsEvent.class, e -> {
                var s = new RegisterCommandsEvent(e.getDispatcher(), e.getBuildContext());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(AddReloadListenerEvent.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.AddReloadListenerEvent.class, e -> {
                var s = new AddReloadListenerEvent();
                fire(e, s, p);
                s.getListeners().forEach(e::addListener);
            });
        }
        for (EventPriority p : types.getOrDefault(VillagerTradesEvent.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.village.VillagerTradesEvent.class, e -> {
                var s = new VillagerTradesEvent(e.getType(), e.getTrades());
                fire(e, s, p);
            });
        }
        for (EventPriority p : types.getOrDefault(WandererTradesEvent.class, Set.of())) {
            NeoForge.EVENT_BUS.addListener(prio(p), true, net.neoforged.neoforge.event.village.WandererTradesEvent.class, e -> {
                var s = new WandererTradesEvent(e.getGenericTrades(), e.getRareTrades());
                fire(e, s, p);
            });
        }
        // a common event type without its line here would silently never fire
        for (Class<?> c : types.keySet()) {
            if (!KNOWN.contains(c)) throw new IllegalStateException("Bettacraft: no NeoForge event for " + c.getName());
        }
    }

    private static final Set<Class<?>> KNOWN = Set.of(ServerTickEvent.Post.class, ServerStartedEvent.class, ServerStoppedEvent.class, ServerAboutToStartEvent.class, LevelTickEvent.Post.class, PlayerTickEvent.Post.class, EntityTickEvent.Post.class, PlayerEvent.PlayerLoggedInEvent.class, PlayerEvent.PlayerLoggedOutEvent.class, PlayerEvent.ItemCraftedEvent.class, PlayerEvent.ItemSmeltedEvent.class, PlayerInteractEvent.EntityInteract.class, PlayerInteractEvent.RightClickBlock.class, PlayerInteractEvent.RightClickItem.class, PlayerInteractEvent.LeftClickBlock.class, EntityJoinLevelEvent.class, EntityLeaveLevelEvent.class, BlockEvent.BreakEvent.class, BlockEvent.EntityPlaceEvent.class, LivingDeathEvent.class, LivingDropsEvent.class, LivingExperienceDropEvent.class, LivingConversionEvent.Pre.class, LivingConversionEvent.Post.class, LivingDamageEvent.Pre.class, LivingChangeTargetEvent.class, MobDespawnEvent.class, BabyEntitySpawnEvent.class, ItemFishedEvent.class, ItemEntityPickupEvent.Pre.class, TradeWithVillagerEvent.class, ExplosionEvent.Detonate.class, PistonEvent.Pre.class, TagsUpdatedEvent.class, OnDatapackSyncEvent.class, RegisterCommandsEvent.class, AddReloadListenerEvent.class, VillagerTradesEvent.class, WandererTradesEvent.class);

    private static void fire(net.neoforged.bus.api.Event e, GameEvent s, EventPriority p) {
        boolean cancellable = e instanceof ICancellableEvent && s.isCancelable();
        if (cancellable && ((ICancellableEvent) e).isCanceled()) s.setCanceled(true);
        GameEvents.post(s, p);
        if (cancellable) ((ICancellableEvent) e).setCanceled(s.isCanceled());
    }

    private static net.neoforged.bus.api.EventPriority prio(EventPriority p) {
        return net.neoforged.bus.api.EventPriority.valueOf(p.name());
    }

    private static net.neoforged.neoforge.common.util.TriState tri(TriState t) {
        return t.isTrue() ? net.neoforged.neoforge.common.util.TriState.TRUE : t.isFalse() ? net.neoforged.neoforge.common.util.TriState.FALSE
                : net.neoforged.neoforge.common.util.TriState.DEFAULT;
    }
}
