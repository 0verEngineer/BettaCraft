package org.overengineer.bettacraft.fabric;

import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeModConfigEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.SpawnPlacements;
import net.neoforged.fml.config.ModConfig;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ClientConfig;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModEntities;

/**
 * M52: the Fabric entry point: the configs (Forge Config API Port: the same TOML files and keys as on NeoForge), common
 * registration in the vanilla registry order, attributes and spawn rules, payloads, the transfer API, the game events of
 * the common code from Fabric's callbacks ({@link FabricGameEvents}) and the world content NeoForge gets from data
 * (biome modifiers, loot modifiers, trades: {@link FabricWorld}).
 */
public final class BettaCraftFabric implements ModInitializer {
    /** Our block entities loaded since the last tick start, waiting for their onLoad (see onInitialize). */
    private static final java.util.List<net.minecraft.world.level.block.entity.BlockEntity> PENDING_LOAD = new java.util.ArrayList<>();

    @Override
    public void onInitialize() {
        // the configs first: the feature switches decide registration details and the conditions
        var dir = FabricLoader.getInstance().getConfigDir();
        org.overengineer.bettacraft.config.FeatureMigration.migrateServerFile(dir.resolve(BettaCraft.MOD_ID + "-server.toml"));
        NeoForgeConfigRegistry.INSTANCE.register(BettaCraft.MOD_ID, ModConfig.Type.SERVER, ServerConfig.SPEC);
        org.overengineer.bettacraft.config.FeatureMigration.migrateFile(dir.resolve(BettaCraft.MOD_ID + "-common.toml"));
        NeoForgeConfigRegistry.INSTANCE.register(BettaCraft.MOD_ID, ModConfig.Type.COMMON, CommonConfig.SPEC);
        NeoForgeConfigRegistry.INSTANCE.register(BettaCraft.MOD_ID, ModConfig.Type.CLIENT, ClientConfig.SPEC);

        BettaCraft.init();
        FabricConditions.register();
        org.overengineer.bettacraft.progression.GuideBook.backend = new PatchouliBook();
        FabricRegistration.registerAll();
        ModEntities.attributes(FabricDefaultAttributeRegistry::register);
        ModEntities.spawnPlacements(SpawnPlacements::register);
        FabricNetwork.register();
        FabricCapabilities.register();
        FabricCapabilities.buckets();
        // Better Fish Tanks (M7): our filled bags and betta buckets release their animal into its tanks. M52-final: on Fabric
        // too (NeoForge: BettaCraftNeoForge, RightClickBlock); before our own block events, like a cancelled RightClickBlock
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(org.overengineer.bettacraft.compat.betterfishtanks.BetterFishTanksCompat.MOD_ID)) {
            org.overengineer.bettacraft.compat.betterfishtanks.BetterFishTanksCompat.register();
            net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
                if (player.isSpectator()) return net.minecraft.world.InteractionResult.PASS;
                var r = org.overengineer.bettacraft.compat.betterfishtanks.BetterFishTanksCompat.onRightClickBlock(
                        player, level, hand, hit.getBlockPos(), player.getItemInHand(hand));
                return r != null ? r : net.minecraft.world.InteractionResult.PASS;
            });
            // M52: their bag does not catch a fish out of our tank past the menu (NeoForge: RightClickItem)
            net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register((player, level, hand) -> {
                var r = org.overengineer.bettacraft.compat.betterfishtanks.BetterFishTanksCompat.onRightClickItem(player, level, player.getItemInHand(hand));
                return r != null ? net.minecraft.world.InteractionResultHolder.fail(player.getItemInHand(hand))
                        : net.minecraft.world.InteractionResultHolder.pass(player.getItemInHand(hand));
            });
        }
        FabricGameEvents.register();
        FabricWorld.register();

        ServerLifecycleEvents.SERVER_STARTING.register(s -> FabricPlatform.server = s);
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> FabricPlatform.server = null);
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> BettaCraft.onTagsUpdated());
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> BettaCraft.onPlayerLoggedIn(handler.player));
        // M57: the debug log follows the common config (loaded and changed in the file or the config screen)
        NeoForgeModConfigEvents.loading(BettaCraft.MOD_ID).register(config -> {
            if (config.getSpec() == CommonConfig.SPEC) CommonConfig.applyDebug();
        });
        CommonConfig.applyDebug(); // the common config may be loaded already when it was registered
        NeoForgeModConfigEvents.reloading(BettaCraft.MOD_ID).register(config -> {
            if (config.getSpec() == CommonConfig.SPEC) CommonConfig.applyDebug(); // M57
            var server = FabricPlatform.server;
            if (server == null) return;
            if (config.getSpec() == CommonConfig.SPEC) server.execute(() -> BettaCraft.onCommonConfigReloaded(server));
            else if (config.getSpec() == ServerConfig.SPEC) server.execute(() -> BettaCraft.onServerConfigReloaded(server));
        });
        // NeoForge's onLoad / onChunkUnloaded of our block entities. Fabric's load event fires while the chunk is still being
        // loaded (before it ticks: a tick scheduled there is dropped with an error); NeoForge calls onLoad when the level next
        // ticks its block entities, so the calls wait for the start of the next server tick.
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((be, level) -> {
            if (be instanceof org.overengineer.bettacraft.platform.BlockEntityLifecycle) {
                synchronized (PENDING_LOAD) {
                    PENDING_LOAD.add(be);
                }
            }
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.START_SERVER_TICK.register(s -> {
            java.util.List<net.minecraft.world.level.block.entity.BlockEntity> due;
            synchronized (PENDING_LOAD) {
                if (PENDING_LOAD.isEmpty()) return;
                due = java.util.List.copyOf(PENDING_LOAD);
                PENDING_LOAD.clear();
            }
            for (var be : due) {
                if (!be.isRemoved() && be instanceof org.overengineer.bettacraft.platform.BlockEntityLifecycle l) l.onLoad();
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
            synchronized (PENDING_LOAD) {
                PENDING_LOAD.clear();
            }
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((be, level) -> {
            if (be instanceof org.overengineer.bettacraft.platform.BlockEntityLifecycle l) l.onChunkUnloaded();
        });
        BettaCraft.commonSetup();
    }
}
