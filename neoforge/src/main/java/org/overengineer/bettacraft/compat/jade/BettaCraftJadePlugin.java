package org.overengineer.bettacraft.compat.jade;

import net.minecraft.resources.Identifier;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade integration (optional, PLAN 10): only loaded by Jade itself, so the mod runs without it. Server providers
 * collect the data (tank: temperature, water, animals, nest; betta: health, stage, genotype), client providers draw it.
 */
@WailaPlugin(BettaCraft.MOD_ID)
public class BettaCraftJadePlugin implements IWailaPlugin {
    public static final Identifier TANK = BettaCraft.id("tank");
    public static final Identifier BETTA = BettaCraft.id("betta");
    public static final Identifier FILTER = BettaCraft.id("filter");
    public static final Identifier CULTURE = BettaCraft.id("culture");
    /** M37: the spring of the devices the crank winds. */
    public static final Identifier SPRING = BettaCraft.id("spring");
    /** M66: the water pipes (no stored water, the flow a second). */
    public static final Identifier PIPE = BettaCraft.id("pipe");
    /** M51: the hired aquarist. */
    public static final Identifier AQUARIST = BettaCraft.id("hired_aquarist");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(JadeServerData.Tank.INSTANCE, TankBlockEntity.class);
        registration.registerEntityDataProvider(JadeServerData.Betta.INSTANCE, BettaEntity.class);
        registration.registerBlockDataProvider(JadeServerData.Filter.INSTANCE, org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity.class);
        registration.registerBlockDataProvider(JadeServerData.Filter.INSTANCE, org.overengineer.bettacraft.equipment.CabinetBlockEntity.class);
        registration.registerBlockDataProvider(JadeServerData.Culture.INSTANCE, org.overengineer.bettacraft.food.CultureTubBlockEntity.class);
        registration.registerBlockDataProvider(JadeServerData.Spring.INSTANCE, org.overengineer.bettacraft.food.FoodMakerBlockEntity.class);
        registration.registerBlockDataProvider(JadeServerData.Spring.INSTANCE, org.overengineer.bettacraft.fertilizer.FertilizerMakerBlockEntity.class);
        registration.registerBlockDataProvider(JadeServerData.Spring.INSTANCE, org.overengineer.bettacraft.osmosis.RoUnitBlockEntity.class);
        registration.registerBlockDataProvider(JadeServerData.Spring.INSTANCE, org.overengineer.bettacraft.plumbing.WaterChangerBlockEntity.class);
        registration.registerBlockDataProvider(JadeServerData.Spring.INSTANCE, org.overengineer.bettacraft.plumbing.LevelSensorBlockEntity.class); // M68 part B: the sensor's lines
        registration.registerBlockDataProvider(JadeServerData.Spring.INSTANCE, org.overengineer.bettacraft.plumbing.WaterPumpBlockEntity.class);
        registration.registerBlockDataProvider(JadeServerData.Spring.INSTANCE, org.overengineer.bettacraft.filter.FilterPumpHeadBlockEntity.class);
        registration.registerBlockDataProvider(JadeServerData.Pipe.INSTANCE, org.overengineer.bettacraft.plumbing.PipeBlockEntity.class); // M68: the flow now
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(JadeClientTooltips.Tank.INSTANCE, TankBlock.class);
        registration.registerEntityComponent(JadeClientTooltips.Betta.INSTANCE, BettaEntity.class);
        registration.registerEntityComponent(JadeClientTooltips.Aquarist.INSTANCE, org.overengineer.bettacraft.aquarist.AquaristEntity.class); // M51
        registration.registerBlockComponent(JadeClientTooltips.Pipe.INSTANCE, org.overengineer.bettacraft.plumbing.PipeBlock.class); // M66
        registration.registerBlockComponent(JadeClientTooltips.Pipe.INSTANCE, org.overengineer.bettacraft.plumbing.PipeBundleBlock.class);
        registration.registerBlockComponent(JadeClientTooltips.Filter.INSTANCE, org.overengineer.bettacraft.equipment.ExternalFilterBlock.class);
        registration.registerBlockComponent(JadeClientTooltips.Filter.INSTANCE, org.overengineer.bettacraft.equipment.CabinetBlock.class);
        registration.registerBlockComponent(JadeClientTooltips.Culture.INSTANCE, org.overengineer.bettacraft.food.CultureTubBlock.class);
        registration.registerBlockComponent(JadeClientTooltips.Spring.INSTANCE, org.overengineer.bettacraft.food.FoodMakerBlock.class);
        registration.registerBlockComponent(JadeClientTooltips.Spring.INSTANCE, org.overengineer.bettacraft.fertilizer.FertilizerMakerBlock.class);
        registration.registerBlockComponent(JadeClientTooltips.Spring.INSTANCE, org.overengineer.bettacraft.osmosis.RoUnitBlock.class);
        registration.registerBlockComponent(JadeClientTooltips.Spring.INSTANCE, org.overengineer.bettacraft.plumbing.WaterDeviceBlock.class);
        registration.registerBlockComponent(JadeClientTooltips.Spring.INSTANCE, org.overengineer.bettacraft.plumbing.DrainBlock.class); // M68 part B
        registration.registerBlockComponent(JadeClientTooltips.Spring.INSTANCE, org.overengineer.bettacraft.plumbing.WaterPumpBlock.class);
        registration.registerBlockComponent(JadeClientTooltips.Spring.INSTANCE, org.overengineer.bettacraft.filter.FilterPartBlock.class);
        // M66: Jade's own fluid view (the tank's and the pipes' fluid connections as 'Water 12 mB' / 'Empty 500 mB') comes after
        // every provider whatever their priority, so it is taken out once the whole tooltip is collected; also when another overlay
        // carries our lines (overlay.provider): the line is wrong in every overlay (a pass-through connection, not stored water)
        registration.addTooltipCollectedCallback((box, accessor) -> {
            if (accessor instanceof snownee.jade.api.BlockAccessor b && (b.getBlock() instanceof TankBlock
                    || b.getBlock() instanceof org.overengineer.bettacraft.plumbing.PipeBlock
                    || b.getBlock() instanceof org.overengineer.bettacraft.plumbing.PipeBundleBlock)) {
                box.getTooltip().remove(snownee.jade.api.JadeIds.UNIVERSAL_FLUID_STORAGE);
            }
        });
        // looking through the glass at a fish shows the fish, not the tank block
        registration.addRayTraceCallback((hit, accessor, original) -> JadeClientTooltips.retarget(registration, accessor));
    }
}
