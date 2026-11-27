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
        registrar.head(new Tank(), TankBlock.class, 5000); // M64: the tank's name instead of the block name (after the core name)
        registrar.body(new Filter(), ExternalFilterBlock.class);
        registrar.body(new Filter(), CabinetBlock.class);
        registrar.body(new Pipe(), org.overengineer.bettacraft.plumbing.PipeBlock.class); // M66
        registrar.body(new Pipe(), org.overengineer.bettacraft.plumbing.PipeBundleBlock.class);
        registrar.body(new Culture(), org.overengineer.bettacraft.food.CultureTubBlock.class);
        registrar.body(new Betta(), BettaEntity.class);
        registrar.body(new Aquarist(), org.overengineer.bettacraft.aquarist.AquaristEntity.class); // M51
        registrar.body(new Spring(), org.overengineer.bettacraft.food.FoodMakerBlock.class);
        registrar.body(new Spring(), org.overengineer.bettacraft.fertilizer.FertilizerMakerBlock.class);
        registrar.body(new Spring(), org.overengineer.bettacraft.osmosis.RoUnitBlock.class);
        registrar.body(new Spring(), org.overengineer.bettacraft.plumbing.WaterDeviceBlock.class);
        registrar.body(new Spring(), org.overengineer.bettacraft.plumbing.WaterPumpBlock.class);
        registrar.body(new Spring(), org.overengineer.bettacraft.filter.FilterPartBlock.class);
        // looking through the glass at a fish shows the fish, not the tank block
        registrar.redirect(new Tank(), TankBlock.class);
    }

    private static boolean details(net.minecraft.world.entity.player.Player player) {
        return player != null && player.isShiftKeyDown();
    }

    static final class Tank implements IBlockComponentProvider {
        @Override
        public ITargetRedirector.Result redirect(ITargetRedirector redirect, IBlockAccessor accessor, IPluginConfig config) {
            if (!org.overengineer.bettacraft.compat.info.OverlayProvider.shows(org.overengineer.bettacraft.compat.info.OverlayProvider.Overlay.WTHIT)) return redirect.toSelf();
            EntityHitResult hit = InfoTarget.bettaBehindGlass(accessor.getLevel(), accessor.getPlayer());
            return hit == null ? redirect.toSelf() : redirect.to(hit);
        }

        @Override
        public void appendHead(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
            if (!org.overengineer.bettacraft.compat.info.OverlayProvider.shows(org.overengineer.bettacraft.compat.info.OverlayProvider.Overlay.WTHIT)) return;
            net.minecraft.network.chat.Component title = InfoLines.tankTitle(accessor.getData().raw().getCompound(InfoData.KEY));
            if (title != null) tooltip.setLine(mcp.mobius.waila.api.WailaConstants.OBJECT_NAME_TAG,
                    mcp.mobius.waila.api.IWailaConfig.get().getFormatter().blockName(title.getString()));
        }

        @Override
        public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
            if (!org.overengineer.bettacraft.compat.info.OverlayProvider.shows(org.overengineer.bettacraft.compat.info.OverlayProvider.Overlay.WTHIT)) return;
            InfoLines.tank(accessor.getData().raw().getCompound(InfoData.KEY), details(accessor.getPlayer())).forEach(tooltip::addLine);
        }
    }

    static final class Filter implements IBlockComponentProvider {
        @Override
        public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
            if (!org.overengineer.bettacraft.compat.info.OverlayProvider.shows(org.overengineer.bettacraft.compat.info.OverlayProvider.Overlay.WTHIT)) return;
            InfoLines.filter(accessor.getData().raw().getCompound(InfoData.KEY)).forEach(tooltip::addLine);
        }
    }

    /** M37: the spring of the devices the crank winds. */
    static final class Spring implements IBlockComponentProvider {
        @Override
        public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
            if (!org.overengineer.bettacraft.compat.info.OverlayProvider.shows(org.overengineer.bettacraft.compat.info.OverlayProvider.Overlay.WTHIT)) return;
            InfoLines.spring(accessor.getData().raw().getCompound(InfoData.KEY)).forEach(tooltip::addLine);
        }
    }

    /** M66: a pipe carries water, it stores none: the flow a second. */
    static final class Pipe implements IBlockComponentProvider {
        @Override
        public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
            if (!org.overengineer.bettacraft.compat.info.OverlayProvider.shows(org.overengineer.bettacraft.compat.info.OverlayProvider.Overlay.WTHIT)) return;
            net.minecraft.nbt.CompoundTag d = accessor.getData().raw().getCompound(InfoData.KEY);
            InfoLines.pipe(accessor.getWorld(), accessor.getPosition(), accessor.getBlockState(), d.isEmpty() ? null : d).forEach(tooltip::addLine);
        }
    }

    static final class Culture implements IBlockComponentProvider {
        @Override
        public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
            if (!org.overengineer.bettacraft.compat.info.OverlayProvider.shows(org.overengineer.bettacraft.compat.info.OverlayProvider.Overlay.WTHIT)) return;
            InfoLines.culture(accessor.getData().raw().getCompound(InfoData.KEY)).forEach(tooltip::addLine);
        }
    }

    /** M51: the hired aquarist (synced data only). */
    static final class Aquarist implements IEntityComponentProvider {
        @Override
        public void appendBody(ITooltip tooltip, IEntityAccessor accessor, IPluginConfig config) {
            if (!org.overengineer.bettacraft.compat.info.OverlayProvider.shows(org.overengineer.bettacraft.compat.info.OverlayProvider.Overlay.WTHIT)) return;
            if (accessor.getEntity() instanceof org.overengineer.bettacraft.aquarist.AquaristEntity a) InfoLines.aquarist(a).forEach(tooltip::addLine);
        }
    }

    static final class Betta implements IEntityComponentProvider {
        @Override
        public void appendBody(ITooltip tooltip, IEntityAccessor accessor, IPluginConfig config) {
            if (!org.overengineer.bettacraft.compat.info.OverlayProvider.shows(org.overengineer.bettacraft.compat.info.OverlayProvider.Overlay.WTHIT)) return;
            if (!(accessor.getEntity() instanceof BettaEntity b)) return;
            InfoLines.betta(b, accessor.getData().raw().getCompound(InfoData.KEY), details(accessor.getPlayer())).forEach(tooltip::addLine);
        }
    }
}
