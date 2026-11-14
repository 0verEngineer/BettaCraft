package org.overengineer.bettacraft;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
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
        org.overengineer.bettacraft.registry.ModFluids.register(modBus); // M25
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
        org.overengineer.bettacraft.registry.ModParticles.register(modBus); // M23
        modBus.addListener(ModNetwork::register);
        // M22.3: spontaneous mutations of the small animals (wildlife.otherMutationChance)
        org.overengineer.bettacraft.critters.CritterGenes.MUTATION = org.overengineer.bettacraft.world.Wildlife::mutationChance;
        modBus.addListener(BettaCraft::onCapabilities);
        // M22.1: the sea water bucket in a cauldron and from a dispenser like a water bucket
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent e) -> e.enqueueWork((Runnable) BettaCraft::containerSetup));
        // M21.7: the slab table of the stone cabinets follows reloads of recipes and tags
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.TagsUpdatedEvent e) ->
                org.overengineer.bettacraft.equipment.CabinetStones.clear());
        // Better Fish Tanks (M7): release our bags and betta buckets into its tanks (reflection, only with the mod)
        if (net.neoforged.fml.ModList.get().isLoaded(org.overengineer.bettacraft.compat.betterfishtanks.BetterFishTanksCompat.MOD_ID)) {
            org.overengineer.bettacraft.compat.betterfishtanks.BetterFishTanksCompat.register();
        }
        // The One Probe (M8): providers through its IMC entry point. Jade and WTHIT find their plugins themselves.
        if (net.neoforged.fml.ModList.get().isLoaded(org.overengineer.bettacraft.compat.top.TheOneProbeCompat.MOD_ID)) {
            org.overengineer.bettacraft.compat.top.TheOneProbeCompat.register(modBus);
        }

        // M21.8: the cyst stack size reaches the clients at login and whenever the server config is reloaded
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) org.overengineer.bettacraft.network.ServerSettingsPayload.send(sp);
        });
        modBus.addListener((net.neoforged.fml.event.config.ModConfigEvent.Reloading e) -> {
            if (e.getConfig().getSpec() != ServerConfig.SPEC) return;
            var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) server.execute(() -> org.overengineer.bettacraft.network.ServerSettingsPayload.sendAll(server));
        });
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        container.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }

    /** M22.1: every container empties everywhere (ContainerRules): the sea water bucket into cauldrons and from dispensers. */
    private static void containerSetup() {
        containerSetup(org.overengineer.bettacraft.registry.ModItems.SEA_WATER_BUCKET.get());
        containerSetup(org.overengineer.bettacraft.registry.ModItems.RO_WATER_BUCKET.get()); // M25
    }

    private static void containerSetup(net.minecraft.world.item.Item sea) {
        for (var map : new net.minecraft.core.cauldron.CauldronInteraction.InteractionMap[] {net.minecraft.core.cauldron.CauldronInteraction.EMPTY,
                net.minecraft.core.cauldron.CauldronInteraction.WATER, net.minecraft.core.cauldron.CauldronInteraction.LAVA,
                net.minecraft.core.cauldron.CauldronInteraction.POWDER_SNOW}) {
            map.map().put(sea, net.minecraft.core.cauldron.CauldronInteraction.FILL_WATER);
        }
        net.minecraft.world.level.block.DispenserBlock.registerBehavior(sea, new net.minecraft.core.dispenser.DefaultDispenseItemBehavior() {
            private final net.minecraft.core.dispenser.DefaultDispenseItemBehavior drop = new net.minecraft.core.dispenser.DefaultDispenseItemBehavior();

            @Override
            public ItemStack execute(net.minecraft.core.dispenser.BlockSource source, ItemStack stack) {
                var bucket = (net.minecraft.world.item.DispensibleContainerItem) stack.getItem();
                var pos = source.pos().relative(source.state().getValue(net.minecraft.world.level.block.DispenserBlock.FACING));
                var level = source.level();
                if (bucket.emptyContents(null, level, pos, null, stack)) {
                    bucket.checkExtraContent(null, level, stack, pos);
                    return consumeWithRemainder(source, stack, new ItemStack(net.minecraft.world.item.Items.BUCKET));
                }
                return drop.dispense(source, stack);
            }
        });
    }

    private static void onCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        // M22.1: a bucket of sea water is a bucket of water for other mods' tanks and pipes (it empties into a bucket)
        // M25: other mods see the fluids of their own (bettacraft:sea_water, bettacraft:ro_water)
        event.registerItem(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM,
                (stack, ctx) -> new org.overengineer.bettacraft.osmosis.WaterBucketWrapper(stack, org.overengineer.bettacraft.registry.ModFluids.SEA_WATER),
                org.overengineer.bettacraft.registry.ModItems.SEA_WATER_BUCKET.get());
        event.registerItem(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM,
                (stack, ctx) -> new org.overengineer.bettacraft.osmosis.WaterBucketWrapper(stack, org.overengineer.bettacraft.registry.ModFluids.RO_WATER),
                org.overengineer.bettacraft.registry.ModItems.RO_WATER_BUCKET.get());
        // M25: the reverse osmosis unit (water in, reverse osmosis water out; parts from hoppers; FE for the pressure pump while
        // a power option is on) and the CO2 filling station (cylinders, FE for the compressor, CO2 gas of other mods)
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, ModBlockEntities.RO_UNIT.get(), (be, side) -> be.fluid());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, ModBlockEntities.RO_UNIT.get(), (be, side) -> be.parts());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, ModBlockEntities.DOSING_PUMP.get(), (be, side) -> be.automation()); // M25c
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, ModBlockEntities.RO_UNIT.get(),
                (be, side) -> org.overengineer.bettacraft.crucible.CrucibleBlockEntity.energyOffered() ? be.energy() : null);
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, ModBlockEntities.CO2_STATION.get(), (be, side) -> be.automation());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, ModBlockEntities.CO2_STATION.get(), (be, side) -> be.gasIn());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, ModBlockEntities.CO2_STATION.get(),
                (be, side) -> org.overengineer.bettacraft.crucible.CrucibleBlockEntity.energyOffered() ? be.energy() : null);
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
        // M26: live food cultures (starters, food and mud in; the harvest and empty bottles out; water from pipes) and the
        // live food pump (its empty bottles out; FE while the pumps need it)
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, ModBlockEntities.LIVE_CULTURE.get(),
                (be, side) -> be.automation());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, ModBlockEntities.LIVE_CULTURE.get(),
                (be, side) -> be.fluid());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, ModBlockEntities.LIVE_FOOD_PUMP.get(),
                (be, side) -> be.bottleHandler());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, ModBlockEntities.LIVE_FOOD_PUMP.get(),
                (be, side) -> org.overengineer.bettacraft.plumbing.DeviceEnergy.required() ? be.energy() : null);
        // M9 tank water from pipes (fresh water in, used water out) and FE for the equipment (power.* config):
        // any tank block, an attached external filter, a cabinet under the tank or a light on it; only offered while the
        // server config needs power (no misleading energy bars in info mods otherwise)
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, ModBlockEntities.TANK.get(),
                (be, side) -> new org.overengineer.bettacraft.tank.TankFluid(be.getLevel(), be.getBlockPos(), side)); // M24: a bulkhead from below
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, ModBlockEntities.TANK.get(),
                (be, side) -> org.overengineer.bettacraft.tank.TankPower.anyRequired() ? new org.overengineer.bettacraft.tank.TankPower.Storage(be.getLevel(), be.getBlockPos()) : null);
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, ModBlockEntities.EXTERNAL_FILTER.get(),
                (be, side) -> org.overengineer.bettacraft.tank.TankPower.anyRequired() ? new org.overengineer.bettacraft.tank.TankPower.Storage(be.getLevel(), be::tankBlockPos) : null);
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, ModBlockEntities.CABINET.get(),
                (be, side) -> org.overengineer.bettacraft.tank.TankPower.anyRequired() ? new org.overengineer.bettacraft.tank.TankPower.Storage(be.getLevel(), be.getBlockPos().above()) : null);
        event.registerBlock(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
                (level, pos, state, be, side) -> org.overengineer.bettacraft.tank.TankPower.anyRequired()
                        ? new org.overengineer.bettacraft.tank.TankPower.Storage(level, pos.below()) : null, ModBlocks.TANK_LIGHT.get(),
                ModBlocks.LED_PANEL.get(), ModBlocks.HIGH_POWER_LED.get()); // M23: the stronger lights too
        // M24: pipes carry water (no block entity: the network does it), the reservoir holds it; FE for the water devices
        event.registerBlock(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                (level, pos, state, be, side) -> new org.overengineer.bettacraft.plumbing.PipeFluid(level, pos, side),
                ModBlocks.GLASS_PIPE.get(), ModBlocks.STAINLESS_STEEL_PIPE.get());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, ModBlockEntities.RESERVOIR.get(),
                (be, side) -> be.water());
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, ModBlockEntities.CRUCIBLE.get(),
                (be, side) -> be.automation(side));
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, ModBlockEntities.CRUCIBLE.get(),
                (be, side) -> org.overengineer.bettacraft.crucible.CrucibleBlockEntity.energyOffered() ? be.energy() : null);
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, ModBlockEntities.WATER_PUMP.get(),
                (be, side) -> org.overengineer.bettacraft.plumbing.DeviceEnergy.required() ? be.energy() : null);
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, ModBlockEntities.FILTER_PUMP_HEAD.get(),
                (be, side) -> org.overengineer.bettacraft.plumbing.DeviceEnergy.required() ? be.energy() : null);
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, ModBlockEntities.WATER_CHANGER.get(),
                (be, side) -> org.overengineer.bettacraft.plumbing.DeviceEnergy.required() ? be.energy() : null);
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, ModBlockEntities.LEVEL_SENSOR.get(),
                (be, side) -> org.overengineer.bettacraft.plumbing.DeviceEnergy.required() ? be.energy() : null);
        // M23: the air pump forwards FE to the tank it serves
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, ModBlockEntities.AIR_PUMP.get(),
                (be, side) -> org.overengineer.bettacraft.tank.TankPower.anyRequired() ? new org.overengineer.bettacraft.tank.TankPower.Storage(be.getLevel(), be::tankBlockPos) : null);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
