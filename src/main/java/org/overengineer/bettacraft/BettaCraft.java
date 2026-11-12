package org.overengineer.bettacraft;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.overengineer.bettacraft.config.ClientConfig;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.network.ModNetwork;
import org.overengineer.bettacraft.registry.ModAttachments;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModCodecs;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModMenus;
import org.overengineer.bettacraft.registry.ModCreativeTabs;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModRecipes;

/** Common entry point: registration only. Client code lives in {@link org.overengineer.bettacraft.client.BettaCraftClient}. */
@Mod(BettaCraft.MOD_ID)
public final class BettaCraft {
    public static final String MOD_ID = "bettacraft";

    public BettaCraft(IEventBus modBus, ModContainer container) {
        ModDataComponents.register(modBus);
        ModBlocks.register(modBus);
        ModEntities.register(modBus);
        ModItems.register(modBus);
        ModBlockEntities.register(modBus);
        ModMenus.register(modBus);
        ModRecipes.register(modBus);
        ModCodecs.register(modBus);
        ModAttachments.register(modBus);
        ModCreativeTabs.register(modBus);
        org.overengineer.bettacraft.progression.ModTriggers.register(modBus);
        org.overengineer.bettacraft.registry.ModSounds.register(modBus);
        modBus.addListener(ModNetwork::register);
        modBus.addListener(BettaCraft::onCapabilities);
        // Better Fish Tanks (M7): release our bags and betta buckets into its tanks (reflection, only with the mod)
        if (net.neoforged.fml.ModList.get().isLoaded(org.overengineer.bettacraft.compat.betterfishtanks.BetterFishTanksCompat.MOD_ID)) {
            org.overengineer.bettacraft.compat.betterfishtanks.BetterFishTanksCompat.register();
        }
        // The One Probe (M8): providers through its IMC entry point. Jade and WTHIT find their plugins themselves.
        if (net.neoforged.fml.ModList.get().isLoaded(org.overengineer.bettacraft.compat.top.TheOneProbeCompat.MOD_ID)) {
            org.overengineer.bettacraft.compat.top.TheOneProbeCompat.register(modBus);
        }

        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        container.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }

    private static void onCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        // hoppers, pipes and other mods insert food into the feeder
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, ModBlockEntities.FEEDER.get(),
                (be, side) -> be.items());
        // cabinet storage (not the filter slot): the feeder and hoppers use it
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, ModBlockEntities.CABINET.get(),
                (be, side) -> be.storage());
        // M9 live food breeders: ingredients and bottles in, food out; water from pipes into the larva tub
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, ModBlockEntities.CULTURE_TUB.get(),
                (be, side) -> be.automation());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, ModBlockEntities.CULTURE_TUB.get(),
                (be, side) -> be.fluid());
        // M9 tank water from pipes (fresh water in, used water out) and FE for the equipment (power.* config):
        // any tank block, an attached external filter, a cabinet under the tank or a light on it
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, ModBlockEntities.TANK.get(),
                (be, side) -> new org.overengineer.bettacraft.tank.TankFluid(be.getLevel(), be.getBlockPos()));
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, ModBlockEntities.TANK.get(),
                (be, side) -> new org.overengineer.bettacraft.tank.TankPower.Storage(be.getLevel(), be.getBlockPos()));
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, ModBlockEntities.EXTERNAL_FILTER.get(),
                (be, side) -> be.tankBlockPos() == null ? null : new org.overengineer.bettacraft.tank.TankPower.Storage(be.getLevel(), be.tankBlockPos()));
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, ModBlockEntities.CABINET.get(),
                (be, side) -> new org.overengineer.bettacraft.tank.TankPower.Storage(be.getLevel(), be.getBlockPos().above()));
        event.registerBlock(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
                (level, pos, state, be, side) -> new org.overengineer.bettacraft.tank.TankPower.Storage(level, pos.below()), ModBlocks.TANK_LIGHT.get());
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
