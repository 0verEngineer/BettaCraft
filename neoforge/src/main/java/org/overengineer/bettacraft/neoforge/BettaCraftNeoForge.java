package org.overengineer.bettacraft.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ClientConfig;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.NeoForgeCodecs;

/**
 * M52: the NeoForge entry point (was the constructor of the mod class): common registration through the platform layer,
 * NeoForge's own registers, payloads, capabilities, configs and the mod and game events. Client code starts in
 * {@link org.overengineer.bettacraft.client.BettaCraftClient}.
 */
@Mod(BettaCraft.MOD_ID)
public final class BettaCraftNeoForge {
    public BettaCraftNeoForge(IEventBus modBus, ModContainer container) {
        BettaCraft.init();
        org.overengineer.bettacraft.api.ApiEvents.sink = new org.overengineer.bettacraft.api.NeoForgeApiEvents();
        // the handbook is a GuideME guide on NeoForge
        org.overengineer.bettacraft.progression.GuideBook.backend = new org.overengineer.bettacraft.progression.GuideBook.Backend() {
            @Override
            public String modId() {
                return "guideme";
            }

            @Override
            public net.minecraft.world.item.ItemStack createBook(net.minecraft.resources.ResourceLocation guideId) {
                return org.overengineer.bettacraft.compat.guideme.GuideMECompat.createBook(guideId);
            }

            @Override
            public boolean isBook(net.minecraft.world.item.ItemStack stack, net.minecraft.resources.ResourceLocation guideId) {
                return org.overengineer.bettacraft.compat.guideme.GuideMECompat.isBook(stack, guideId);
            }
        };
        NeoForgeRegistration.register(modBus);
        NeoForgeGameEvents.register();
        org.overengineer.bettacraft.foreign.ForeignBreeding.modEventHook = org.overengineer.bettacraft.foreign.ForeignBreedingNeoForge::hookModEvents;
        org.overengineer.bettacraft.registry.NeoForgeFluids.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> ModEntities.attributes(e::put));
        modBus.addListener((RegisterSpawnPlacementsEvent e) -> ModEntities.spawnPlacements(new org.overengineer.bettacraft.platform.SpawnPlacementRegistrar() {
            @Override
            public <T extends net.minecraft.world.entity.Mob> void register(net.minecraft.world.entity.EntityType<T> type,
                    net.minecraft.world.entity.SpawnPlacementType placement, net.minecraft.world.level.levelgen.Heightmap.Types heightmap,
                    net.minecraft.world.entity.SpawnPlacements.SpawnPredicate<T> predicate) {
                e.register(type, placement, heightmap, predicate, RegisterSpawnPlacementsEvent.Operation.REPLACE);
            }
        }));
        NeoForgeCodecs.register(modBus);
        modBus.addListener(NeoForgeNetwork::register);
        modBus.addListener(NeoForgeCapabilities::register);
        modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(BettaCraft::commonSetup));
        // M32 part c: tanks and monitors as sources of Create's Display Link (the only class compiled against Create's API)
        if (ModList.get().isLoaded(org.overengineer.bettacraft.compat.create.CreateCompat.MOD_ID)) {
            org.overengineer.bettacraft.compat.create.CreateDisplays.register(modBus);
            // M41: the drive adapter and the shaft on the cabinet with real stress (compiled against Create too)
            org.overengineer.bettacraft.compat.create.CreateDrive.register(modBus);
            // M50: the care on the road (compiled against Create too)
            org.overengineer.bettacraft.compat.create.CreateCompat.travelTick = org.overengineer.bettacraft.compat.create.CreateTravel::tick;
        }
        // M21.7: the slab table of the stone cabinets follows reloads of recipes and tags
        NeoForge.EVENT_BUS.addListener((TagsUpdatedEvent e) -> BettaCraft.onTagsUpdated());
        // Better Fish Tanks (M7): release our bags and betta buckets into its tanks (reflection, only with the mod)
        if (ModList.get().isLoaded(org.overengineer.bettacraft.compat.betterfishtanks.BetterFishTanksCompat.MOD_ID)) {
            org.overengineer.bettacraft.compat.betterfishtanks.BetterFishTanksCompat.register();
            NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e) -> {
                var r = org.overengineer.bettacraft.compat.betterfishtanks.BetterFishTanksCompat.onRightClickBlock(e.getEntity(), e.getLevel(), e.getHand(), e.getPos(), e.getItemStack());
                if (r != null) {
                    e.setCanceled(true);
                    e.setCancellationResult(r);
                }
            });
            // M52: their bag does not catch a fish out of our tank past the menu
            NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem e) -> {
                var r = org.overengineer.bettacraft.compat.betterfishtanks.BetterFishTanksCompat.onRightClickItem(e.getEntity(), e.getLevel(), e.getItemStack());
                if (r != null) {
                    e.setCanceled(true);
                    e.setCancellationResult(r);
                }
            });
        }
        // M40 part c: Cable Facades (its facades on our filter pipes: the block tag in our data; here the pipes hidden under them)
        if (ModList.get().isLoaded(org.overengineer.bettacraft.compat.facades.CableFacadesCompat.MOD_ID)) {
            org.overengineer.bettacraft.compat.facades.CableFacadesCompat.register();
        }
        // The One Probe (M8): providers through its IMC entry point. Jade and WTHIT find their plugins themselves.
        if (ModList.get().isLoaded(org.overengineer.bettacraft.compat.top.TheOneProbeCompat.MOD_ID)) {
            modBus.addListener((net.neoforged.fml.event.lifecycle.InterModEnqueueEvent e) -> net.neoforged.fml.InterModComms.sendTo(
                    "theoneprobe", "getTheOneProbe", org.overengineer.bettacraft.compat.top.TheOneProbeCompat::new)); // TOP's IMC entry point
        }

        // M22.3: the server settings (free genes) reach the clients at login and whenever the server config is reloaded
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer sp) BettaCraft.onPlayerLoggedIn(sp);
        });
        // M57: the debug log follows the common config (loaded and changed in the file or the config screen)
        modBus.addListener((ModConfigEvent.Loading e) -> {
            if (e.getConfig().getSpec() == CommonConfig.SPEC) CommonConfig.applyDebug();
        });
        modBus.addListener((ModConfigEvent.Reloading e) -> {
            if (e.getConfig().getSpec() == CommonConfig.SPEC) { // M33 part b: the feature switches changed in the file
                CommonConfig.applyDebug(); // M57
                var server = ServerLifecycleHooks.getCurrentServer();
                if (server != null) server.execute(() -> BettaCraft.onCommonConfigReloaded(server));
                return;
            }
            if (e.getConfig().getSpec() != ServerConfig.SPEC) return;
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) server.execute(() -> BettaCraft.onServerConfigReloaded(server));
        });
        // M35.1: a server config from before M35.1 loses food.artemiaCystStackSize (and gets the 4x sand chance if it had the old default)
        org.overengineer.bettacraft.config.FeatureMigration.migrateServerFile(FMLPaths.CONFIGDIR.get().resolve(BettaCraft.MOD_ID + "-server.toml"));
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        // M33: a common config from before M33 gets its flat feature switches moved into the feature tree first
        org.overengineer.bettacraft.config.FeatureMigration.migrateFile(FMLPaths.CONFIGDIR.get().resolve(BettaCraft.MOD_ID + "-common.toml"));
        container.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }
}
