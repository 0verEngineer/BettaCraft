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
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
