package org.overengineer.bettacraft.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.equipment.FeederBlock;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.interior.InteriorFloorBlock;
import org.overengineer.bettacraft.tank.interior.InteriorWallBlock;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BettaCraft.MOD_ID);

    public static final DeferredBlock<TankBlock> TANK = BLOCKS.register("tank", () -> new TankBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.NONE)
            .strength(0.3F) // like glass (hardness and blast resistance)
            .sound(SoundType.GLASS)
            .noOcclusion()
            .pushReaction(PushReaction.BLOCK)
            .isValidSpawn((s, l, p, e) -> false)
            .isRedstoneConductor((s, l, p) -> false)
            .isSuffocating((s, l, p) -> false)
            .isViewBlocking((s, l, p) -> false)));

    /** Automatic feeder on top of a tank. */
    public static final DeferredBlock<FeederBlock> FEEDER = BLOCKS.register("feeder", () -> new FeederBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(1.5F, 6.0F)
            .sound(SoundType.METAL)
            .noOcclusion()));

    /** Light bar for tanks: light level 15, switched off by redstone. */
    public static final DeferredBlock<TankLightBlock> TANK_LIGHT = BLOCKS.register("tank_light", () -> new TankLightBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(0.8F)
            .sound(SoundType.METAL)
            .noOcclusion()
            .lightLevel(s -> s.getValue(TankLightBlock.LIT) ? 15 : 0)
            .isRedstoneConductor((s, l, p) -> false)));

    /** Heater rod in a tank interior (PLAN 7.3), one block per segment. */
    public static final DeferredBlock<org.overengineer.bettacraft.equipment.SegmentBlock> HEATER_SEGMENT = BLOCKS.register("heater_segment",
            () -> new org.overengineer.bettacraft.equipment.SegmentBlock(org.overengineer.bettacraft.equipment.SegmentBlock.Kind.HEATER, () -> ModItems.HEATER_SMALL.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE).strength(0.3F).sound(SoundType.GLASS).noLootTable().lightLevel(s -> 3)
                    .pushReaction(PushReaction.BLOCK)));

    /** Cooler rod in a tank interior (M9), one block per segment. */
    public static final DeferredBlock<org.overengineer.bettacraft.equipment.SegmentBlock> COOLER_SEGMENT = BLOCKS.register("cooler_segment",
            () -> new org.overengineer.bettacraft.equipment.SegmentBlock(org.overengineer.bettacraft.equipment.SegmentBlock.Kind.COOLER, () -> ModItems.COOLER_SMALL.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.3F).sound(SoundType.GLASS).noLootTable().lightLevel(s -> 2)
                    .pushReaction(PushReaction.BLOCK)));

    /** Internal filter box in a tank interior (PLAN 7.3). */
    public static final DeferredBlock<org.overengineer.bettacraft.equipment.SegmentBlock> FILTER_SEGMENT = BLOCKS.register("filter_segment",
            () -> new org.overengineer.bettacraft.equipment.SegmentBlock(org.overengineer.bettacraft.equipment.SegmentBlock.Kind.FILTER, () -> ModItems.INTERNAL_FILTER.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY).strength(0.5F).sound(SoundType.METAL).noLootTable().pushReaction(PushReaction.BLOCK)));

    /** External filter next to a tank (PLAN 6.2), two heater slots. */
    public static final DeferredBlock<org.overengineer.bettacraft.equipment.ExternalFilterBlock> EXTERNAL_FILTER = BLOCKS.register("external_filter",
            () -> new org.overengineer.bettacraft.equipment.ExternalFilterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY).strength(1.0F, 6.0F).sound(SoundType.METAL).noOcclusion()
                    .isRedstoneConductor((s, l, p) -> false)));

    /** Aquarium cabinet of any planks (PLAN 6.5). */
    public static final DeferredBlock<org.overengineer.bettacraft.equipment.CabinetBlock> CABINET = BLOCKS.register("cabinet",
            () -> new org.overengineer.bettacraft.equipment.CabinetBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).ignitedByLava()));

    /** Glass lid on top of a tank (M9): stops evaporation from the block below. */
    public static final DeferredBlock<org.overengineer.bettacraft.equipment.TankLidBlock> TANK_LID = BLOCKS.register("tank_lid",
            () -> new org.overengineer.bettacraft.equipment.TankLidBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE).strength(0.3F).sound(SoundType.GLASS).noOcclusion()
                    .isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)));

    /** Larva tub (M9): breeds mosquito larvae under the open sky. */
    public static final DeferredBlock<org.overengineer.bettacraft.food.CultureTubBlock> LARVA_TUB = BLOCKS.register("larva_tub",
            () -> new org.overengineer.bettacraft.food.CultureTubBlock(org.overengineer.bettacraft.food.CultureTubBlock.Kind.LARVAE, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));

    /** Artemia hatchery (M9): hatches artemia nauplii in salt water with light. */
    public static final DeferredBlock<org.overengineer.bettacraft.food.CultureTubBlock> ARTEMIA_HATCHERY = BLOCKS.register("artemia_hatchery",
            () -> new org.overengineer.bettacraft.food.CultureTubBlock(org.overengineer.bettacraft.food.CultureTubBlock.Kind.ARTEMIA, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY).strength(1.0F).sound(SoundType.GLASS).noOcclusion()
                    .isRedstoneConductor((s, l, p) -> false)));

    /** Floor of a tank interior; can carry a substrate look (PLAN 7.2). Unbreakable. */
    public static final DeferredBlock<InteriorFloorBlock> INTERIOR_FLOOR = BLOCKS.register("interior_floor", () -> new InteriorFloorBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .isValidSpawn((s, l, p, e) -> false)
                    .pushReaction(PushReaction.BLOCK)));

    /** Invisible, unbreakable wall bounding a tank interior. */
    public static final DeferredBlock<InteriorWallBlock> INTERIOR_WALL = BLOCKS.register("interior_wall", () -> new InteriorWallBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .noOcclusion()
                    .isValidSpawn((s, l, p, e) -> false)
                    .isRedstoneConductor((s, l, p) -> false)
                    .isSuffocating((s, l, p) -> false)
                    .isViewBlocking((s, l, p) -> false)
                    .pushReaction(PushReaction.BLOCK)));

    /** M13: aquarium plants of the tank interior, one block per species (parts and redness as states). */
    public static final java.util.Map<org.overengineer.bettacraft.plants.PlantSpecies,
            DeferredBlock<org.overengineer.bettacraft.plants.AquaticPlantBlock>> AQUATIC_PLANTS = aquaticPlants();

    private static java.util.Map<org.overengineer.bettacraft.plants.PlantSpecies, DeferredBlock<org.overengineer.bettacraft.plants.AquaticPlantBlock>> aquaticPlants() {
        var m = new java.util.EnumMap<org.overengineer.bettacraft.plants.PlantSpecies, DeferredBlock<org.overengineer.bettacraft.plants.AquaticPlantBlock>>(
                org.overengineer.bettacraft.plants.PlantSpecies.class);
        for (var sp : org.overengineer.bettacraft.plants.PlantSpecies.values()) {
            m.put(sp, BLOCKS.register(sp.id(), () -> new org.overengineer.bettacraft.plants.AquaticPlantBlock(sp, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT).strength(0.2F).sound(SoundType.WET_GRASS).noCollission().noOcclusion().noLootTable()
                    .isValidSpawn((s, l, p, e) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)
                    .isRedstoneConductor((s, l, p) -> false).pushReaction(PushReaction.DESTROY))));
        }
        return java.util.Collections.unmodifiableMap(m);
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }

    private ModBlocks() {}
}
