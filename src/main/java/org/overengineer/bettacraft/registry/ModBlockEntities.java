package org.overengineer.bettacraft.registry;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.tank.TankBlockEntity;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BettaCraft.MOD_ID);

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<TankBlockEntity>> TANK = BLOCK_ENTITIES.register("tank",
            () -> BlockEntityType.Builder.of(TankBlockEntity::new, ModBlocks.TANK.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.equipment.FeederBlockEntity>> FEEDER = BLOCK_ENTITIES.register("feeder",
            () -> BlockEntityType.Builder.of(org.overengineer.bettacraft.equipment.FeederBlockEntity::new, ModBlocks.FEEDER.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.equipment.ClipFeederBlockEntity>> CLIP_FEEDER = BLOCK_ENTITIES.register("clip_feeder",
            () -> BlockEntityType.Builder.of(org.overengineer.bettacraft.equipment.ClipFeederBlockEntity::new, ModBlocks.CLIP_FEEDER.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.tank.interior.InteriorFloorBlockEntity>> INTERIOR_FLOOR = BLOCK_ENTITIES.register("interior_floor",
            () -> BlockEntityType.Builder.of(org.overengineer.bettacraft.tank.interior.InteriorFloorBlockEntity::new, ModBlocks.INTERIOR_FLOOR.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity>> EXTERNAL_FILTER = BLOCK_ENTITIES.register("external_filter",
            () -> BlockEntityType.Builder.of(org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity::new, ModBlocks.EXTERNAL_FILTER.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.equipment.CabinetBlockEntity>> CABINET = BLOCK_ENTITIES.register("cabinet",
            () -> BlockEntityType.Builder.of(org.overengineer.bettacraft.equipment.CabinetBlockEntity::new, ModBlocks.CABINET.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<org.overengineer.bettacraft.food.CultureTubBlockEntity>> CULTURE_TUB = BLOCK_ENTITIES.register("culture_tub",
            () -> BlockEntityType.Builder.of(org.overengineer.bettacraft.food.CultureTubBlockEntity::new, ModBlocks.LARVA_TUB.get(),
                    ModBlocks.ARTEMIA_HATCHERY.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }

    private ModBlockEntities() {}
}
