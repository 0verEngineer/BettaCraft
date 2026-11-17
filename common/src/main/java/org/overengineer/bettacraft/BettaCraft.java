package org.overengineer.bettacraft;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.registry.ModAttachments;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModCodecs;
import org.overengineer.bettacraft.registry.ModCreativeTabs;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModMenus;
import org.overengineer.bettacraft.registry.ModRecipes;

/**
 * The mod: registration and the loader-independent start-up steps. M52: each loader's entry point
 * ({@code neoforge.BettaCraftNeoForge}, {@code fabric.BettaCraftFabric}) calls {@link #init()} first and the other hooks
 * from its events. Client code starts in {@code client.BettaCraftClient}.
 */
public final class BettaCraft {
    public static final String MOD_ID = "bettacraft";

    private BettaCraft() {}

    /** Creates every registry object and hands the registers to the loader, in this order. */
    public static void init() {
        ModDataComponents.register();
        ModBlocks.register();
        org.overengineer.bettacraft.registry.ModFluids.register(); // M25
        ModEntities.register();
        ModItems.register();
        ModBlockEntities.register();
        ModMenus.register();
        ModRecipes.register();
        ModCodecs.register();
        ModAttachments.register();
        ModCreativeTabs.register();
        org.overengineer.bettacraft.progression.ModTriggers.register();
        org.overengineer.bettacraft.registry.ModSounds.register();
        org.overengineer.bettacraft.registry.ModParticles.register(); // M23
        org.overengineer.bettacraft.villager.ModVillagers.register(); // M47
        // M22.3: spontaneous mutations of the small animals (wildlife.otherMutationChance)
        org.overengineer.bettacraft.critters.CritterGenes.MUTATION = org.overengineer.bettacraft.world.Wildlife::mutationChance;
        org.overengineer.bettacraft.critters.CritterLife.BIG_PREY = () -> org.overengineer.bettacraft.config.ServerConfig.getOr(
                org.overengineer.bettacraft.config.ServerConfig.BIG_CRAYFISH_PREY, 0.4); // M29 part d
    }

    /** The common set-up work, on the game thread once everything is registered. */
    public static void commonSetup() {
        // M22.1: the sea water bucket in a cauldron and from a dispenser like a water bucket
        containerSetup();
        // M31: the terrarium animals and their needs (the care hook)
        org.overengineer.bettacraft.terrarium.TerrariumAnimals.registerOurs();
        // M32 part b: tanks on Create contraptions (reflection, only with Create) and Sable sub-levels (Create Aeronautics)
        org.overengineer.bettacraft.compat.create.CreateCompat.register();
        org.overengineer.bettacraft.compat.create.SableCompat.register();
    }

    /** M21.7: the slab table of the stone cabinets follows reloads of recipes and tags. */
    public static void onTagsUpdated() {
        org.overengineer.bettacraft.equipment.CabinetStones.clear();
    }

    /** M22.3: the server settings (free genes) and M33 part b the feature switches reach a player at login. */
    public static void onPlayerLoggedIn(ServerPlayer sp) {
        org.overengineer.bettacraft.network.ServerSettingsPayload.send(sp);
        org.overengineer.bettacraft.network.FeatureSyncPayload.send(sp); // M33 part b: the server's feature switches
    }

    /** M33 part b: the feature switches changed in the common config file (on the server thread). */
    public static void onCommonConfigReloaded(MinecraftServer server) {
        org.overengineer.bettacraft.network.FeatureSyncPayload.sendAll(server);
        org.overengineer.bettacraft.equipment.CabinetBlockEntity.refreshAllFronts(); // M44.1: fronts switch at once
        org.overengineer.bettacraft.api.ApiEvents.featuresChanged(server); // M45: the quest script shows and hides its quests
    }

    /** The server config file changed (on the server thread). */
    public static void onServerConfigReloaded(MinecraftServer server) {
        org.overengineer.bettacraft.network.ServerSettingsPayload.sendAll(server);
        org.overengineer.bettacraft.api.ApiEvents.featuresChanged(server); // M45: the power options decide some quests
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
                if (org.overengineer.bettacraft.platform.Platform.get().emptyContents(bucket, null, level, pos, null, stack)) {
                    bucket.checkExtraContent(null, level, stack, pos);
                    return consumeWithRemainder(source, stack, new ItemStack(net.minecraft.world.item.Items.BUCKET));
                }
                return drop.dispense(source, stack);
            }
        });
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
