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

    /** M11: look of a tank item/block (textured glass or clean clear glass); missing = textured. */
    public static final Supplier<DataComponentType<org.overengineer.bettacraft.tank.TankStyle>> TANK_STYLE = COMPONENTS.registerComponentType("tank_style",
            b -> b.persistent(org.overengineer.bettacraft.tank.TankStyle.CODEC).networkSynchronized(org.overengineer.bettacraft.tank.TankStyle.STREAM_CODEC));

    /** Typed betta data of a bucket / bag (tooltip, item tint). Mirrored into minecraft:bucket_entity_data under key "betta". */
    public static final Supplier<DataComponentType<org.overengineer.bettacraft.entity.betta.BettaData>> FISH_DATA = COMPONENTS.registerComponentType("fish_data",
            b -> b.persistent(org.overengineer.bettacraft.entity.betta.BettaData.CODEC)
                    .networkSynchronized(org.overengineer.bettacraft.entity.betta.BettaData.STREAM_CODEC));

    /** Entity type inside a filled fish bag. */
    public static final Supplier<DataComponentType<ResourceLocation>> BAG_ENTITY = COMPONENTS.registerComponentType("bag_entity",
            b -> b.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));

    /** Game time a food culture was started (infusoria / artemia). */
    public static final Supplier<DataComponentType<Long>> CULTURE_START = COMPONENTS.registerComponentType("culture_start",
            b -> b.persistent(com.mojang.serialization.Codec.LONG).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_LONG));

    /** The planks a cabinet item/block is made of (id, like {@link #GLASS}). */
    public static final Supplier<DataComponentType<ResourceLocation>> WOOD = COMPONENTS.registerComponentType("wood",
            b -> b.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));

    /** M22.4: the outlet side of a cabinet ({@code OutletSide} id; missing = back). */
    public static final Supplier<DataComponentType<String>> OUTLET_SIDE = COMPONENTS.registerComponentType("outlet_side",
            b -> b.persistent(com.mojang.serialization.Codec.STRING).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8));

    /** M24: the values (temperature, salt, quality, nitrate) of water in pipes and other mods' tanks. */
    public static final Supplier<DataComponentType<org.overengineer.bettacraft.tank.WaterValues>> WATER_VALUES = COMPONENTS.registerComponentType("water",
            b -> b.persistent(org.overengineer.bettacraft.tank.WaterValues.CODEC).networkSynchronized(org.overengineer.bettacraft.tank.WaterValues.STREAM_CODEC));

    /** M13: growth stage of an aquarium plant taken out of a tank interior (missing = a cutting). */
    public static final Supplier<DataComponentType<Integer>> PLANT_STAGE = COMPONENTS.registerComponentType("plant_stage",
            b -> b.persistent(com.mojang.serialization.Codec.INT).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    /**
     * M14: shrimp, crayfish or snails in a fish bag (each one a compound like {@code Critter#saveAnimal}); a bag with this
     * component counts as filled.
     */
    public static final Supplier<DataComponentType<java.util.List<net.minecraft.nbt.CompoundTag>>> CRITTERS = COMPONENTS.registerComponentType("critters",
            b -> b.persistent(net.minecraft.nbt.CompoundTag.CODEC.listOf())
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.COMPOUND_TAG.apply(net.minecraft.network.codec.ByteBufCodecs.list(64))));

    /**
     * M19: several tankmates of one species in a fish bag (each one the bucket data of a fish, key "tankmate"); the bag
     * also has {@link #BAG_ENTITY} (the species' entity type).
     */
    public static final Supplier<DataComponentType<java.util.List<net.minecraft.nbt.CompoundTag>>> TANKMATE_GROUP = COMPONENTS.registerComponentType("tankmate_group",
            b -> b.persistent(net.minecraft.nbt.CompoundTag.CODEC.listOf())
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.COMPOUND_TAG.apply(net.minecraft.network.codec.ByteBufCodecs.list(64))));

    /** M19: a clutch of tankmate eggs (or larvae) taken out of a tank with the net, in a fish bag ({@code Clutch#save}). */
    public static final Supplier<DataComponentType<net.minecraft.nbt.CompoundTag>> TANKMATE_CLUTCH = COMPONENTS.registerComponentType("tankmate_clutch",
            b -> b.persistent(net.minecraft.nbt.CompoundTag.CODEC).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.COMPOUND_TAG));

    /** M25: CO2 in a gas cylinder (bubble-days: days at one bubble per second; missing = empty). */
    public static final Supplier<DataComponentType<Float>> CO2_GAS = COMPONENTS.registerComponentType("co2_gas",
            b -> b.persistent(com.mojang.serialization.Codec.FLOAT).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.FLOAT));

    /** M25: a solenoid valve sits on the cylinder (it closes at night). */
    public static final Supplier<DataComponentType<Boolean>> CO2_SOLENOID = COMPONENTS.registerComponentType("co2_solenoid",
            b -> b.persistent(com.mojang.serialization.Codec.BOOL).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL));

    /** M26: tubifex from a foul dish (they stress the fish a little). */
    public static final Supplier<DataComponentType<Boolean>> POOR_CULTURE = COMPONENTS.registerComponentType("poor_culture",
            b -> b.persistent(com.mojang.serialization.Codec.BOOL).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL));

    /** M26: what a live food suspension (fluid {@code bettacraft:live_food}) carries. */
    public static final Supplier<DataComponentType<org.overengineer.bettacraft.food.LiveFood.Content>> LIVE_FOOD = COMPONENTS.registerComponentType("live_food",
            b -> b.persistent(org.overengineer.bettacraft.food.LiveFood.Content.CODEC).networkSynchronized(org.overengineer.bettacraft.food.LiveFood.Content.STREAM_CODEC));

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }

    private ModDataComponents() {}
}
