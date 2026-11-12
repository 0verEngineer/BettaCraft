package org.overengineer.bettacraft.compat.jade;

import net.minecraft.resources.ResourceLocation;
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
    public static final ResourceLocation TANK = BettaCraft.id("tank");
    public static final ResourceLocation BETTA = BettaCraft.id("betta");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(JadeServerData.Tank.INSTANCE, TankBlockEntity.class);
        registration.registerEntityDataProvider(JadeServerData.Betta.INSTANCE, BettaEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(JadeClientTooltips.Tank.INSTANCE, TankBlock.class);
        registration.registerEntityComponent(JadeClientTooltips.Betta.INSTANCE, BettaEntity.class);
    }
}
