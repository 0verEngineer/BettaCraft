package org.overengineer.bettacraft.compat.wthit;

import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IClientRegistrar;
import mcp.mobius.waila.api.IEntityAccessor;
import mcp.mobius.waila.api.IEntityComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITargetRedirector;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.IWailaClientPlugin;
import net.minecraft.world.phys.EntityHitResult;
import org.overengineer.bettacraft.compat.info.InfoData;
import org.overengineer.bettacraft.compat.info.InfoLines;
import org.overengineer.bettacraft.compat.info.InfoTarget;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.tank.TankBlock;

/** WTHIT integration, client side: the lines of {@link InfoLines}; sneaking shows the details like Jade. */
public final class WthitClientPlugin implements IWailaClientPlugin {
    @Override
    public void register(IClientRegistrar registrar) {
        registrar.body(new Tank(), TankBlock.class);
        registrar.body(new Filter(), ExternalFilterBlock.class);
        registrar.body(new Filter(), CabinetBlock.class);
        registrar.body(new Culture(), org.overengineer.bettacraft.food.CultureTubBlock.class);
        registrar.body(new Betta(), BettaEntity.class);
        // looking through the glass at a fish shows the fish, not the tank block
        registrar.redirect(new Tank(), TankBlock.class);
    }

    private static boolean details(net.minecraft.world.entity.player.Player player) {
        return player != null && player.isShiftKeyDown();
    }

    static final class Tank implements IBlockComponentProvider {
        @Override
        public ITargetRedirector.Result redirect(ITargetRedirector redirect, IBlockAccessor accessor, IPluginConfig config) {
            EntityHitResult hit = InfoTarget.bettaBehindGlass(accessor.getLevel(), accessor.getPlayer());
            return hit == null ? redirect.toSelf() : redirect.to(hit);
        }

        @Override
        public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
            InfoLines.tank(accessor.getData().raw().getCompound(InfoData.KEY), details(accessor.getPlayer())).forEach(tooltip::addLine);
        }
    }

    static final class Filter implements IBlockComponentProvider {
        @Override
        public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
            InfoLines.filter(accessor.getData().raw().getCompound(InfoData.KEY)).forEach(tooltip::addLine);
        }
    }

    static final class Culture implements IBlockComponentProvider {
        @Override
        public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
            InfoLines.culture(accessor.getData().raw().getCompound(InfoData.KEY)).forEach(tooltip::addLine);
        }
    }

    static final class Betta implements IEntityComponentProvider {
        @Override
        public void appendBody(ITooltip tooltip, IEntityAccessor accessor, IPluginConfig config) {
            if (!(accessor.getEntity() instanceof BettaEntity b)) return;
            InfoLines.betta(b, accessor.getData().raw().getCompound(InfoData.KEY), details(accessor.getPlayer())).forEach(tooltip::addLine);
        }
    }
}
