package org.overengineer.bettacraft.registry;

import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.overengineer.bettacraft.BettaCraft;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, BettaCraft.MOD_ID);

    /**
     * The glass block a tank item/block is made of. Stored as id (not as Block reference) so that tanks made of
     * glass from a removed mod keep their data instead of failing to load.
     */
    public static final Supplier<DataComponentType<ResourceLocation>> GLASS = COMPONENTS.registerComponentType("glass",
            b -> b.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));

    /** Typed betta data of a bucket / bag (tooltip, item tint). Mirrored into minecraft:bucket_entity_data under key "betta". */
    public static final Supplier<DataComponentType<org.overengineer.bettacraft.entity.betta.BettaData>> FISH_DATA = COMPONENTS.registerComponentType("fish_data",
            b -> b.persistent(org.overengineer.bettacraft.entity.betta.BettaData.CODEC)
                    .networkSynchronized(org.overengineer.bettacraft.entity.betta.BettaData.STREAM_CODEC));

    /** Entity type inside a filled fish bag. */
    public static final Supplier<DataComponentType<ResourceLocation>> BAG_ENTITY = COMPONENTS.registerComponentType("bag_entity",
            b -> b.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }

    private ModDataComponents() {}
}
