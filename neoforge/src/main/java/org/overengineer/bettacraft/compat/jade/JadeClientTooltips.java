package org.overengineer.bettacraft.compat.jade;

import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.EntityHitResult;
import org.overengineer.bettacraft.compat.info.InfoData;
import org.overengineer.bettacraft.compat.info.InfoLines;
import org.overengineer.bettacraft.compat.info.InfoTarget;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.tank.TankBlock;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.config.IPluginConfig;

/** Client side of the Jade integration (PLAN 10, OFFEN 33); the lines come from {@link InfoLines}. */
final class JadeClientTooltips {
    private JadeClientTooltips() {}

    /** If the view ray hits a tank block but a betta inside the tank is behind the glass, show that betta instead. */
    static Accessor<?> retarget(IWailaClientRegistration reg, Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor block) || !(block.getBlock() instanceof TankBlock)) return accessor;
        if (!org.overengineer.bettacraft.compat.info.OverlayProvider.shows(org.overengineer.bettacraft.compat.info.OverlayProvider.Overlay.JADE)) return accessor;
        EntityHitResult hit = InfoTarget.bettaBehindGlass(accessor.getLevel(), accessor.getPlayer());
        if (hit == null) return accessor;
        return reg.entityAccessor().level(accessor.getLevel()).player(accessor.getPlayer()).hit(hit).entity(hit.getEntity())
                .serverConnected(accessor.isServerConnected()).showDetails(accessor.showDetails()).requireVerification().build();
    }

    static final class Tank implements IBlockComponentProvider {
        static final Tank INSTANCE = new Tank();

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!org.overengineer.bettacraft.compat.info.OverlayProvider.shows(org.overengineer.bettacraft.compat.info.OverlayProvider.Overlay.JADE)) return;
            InfoLines.tank(accessor.getServerData().getCompound(InfoData.KEY), accessor.showDetails()).forEach(tooltip::add);
        }

        @Override
        public Identifier getUid() {
            return BettaCraftJadePlugin.TANK;
        }
    }

    /** External filter block and cabinet (M4). */
    static final class Filter implements IBlockComponentProvider {
        static final Filter INSTANCE = new Filter();

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!org.overengineer.bettacraft.compat.info.OverlayProvider.shows(org.overengineer.bettacraft.compat.info.OverlayProvider.Overlay.JADE)) return;
            InfoLines.filter(accessor.getServerData().getCompound(InfoData.KEY)).forEach(tooltip::add);
        }

        @Override
        public Identifier getUid() {
            return BettaCraftJadePlugin.FILTER;
        }
    }

    /** M37: the spring of the devices the crank winds. */
    static final class Spring implements IBlockComponentProvider {
        static final Spring INSTANCE = new Spring();

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!org.overengineer.bettacraft.compat.info.OverlayProvider.shows(org.overengineer.bettacraft.compat.info.OverlayProvider.Overlay.JADE)) return;
            InfoLines.spring(accessor.getServerData().getCompound(InfoData.KEY)).forEach(tooltip::add);
        }

        @Override
        public Identifier getUid() {
            return BettaCraftJadePlugin.SPRING;
        }
    }

    /** Larva tub and artemia hatchery (M9). */
    static final class Culture implements IBlockComponentProvider {
        static final Culture INSTANCE = new Culture();

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!org.overengineer.bettacraft.compat.info.OverlayProvider.shows(org.overengineer.bettacraft.compat.info.OverlayProvider.Overlay.JADE)) return;
            InfoLines.culture(accessor.getServerData().getCompound(InfoData.KEY)).forEach(tooltip::add);
        }

        @Override
        public Identifier getUid() {
            return BettaCraftJadePlugin.CULTURE;
        }
    }

    /** M51: the hired aquarist (synced data only). */
    static final class Aquarist implements IEntityComponentProvider {
        static final Aquarist INSTANCE = new Aquarist();

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (!org.overengineer.bettacraft.compat.info.OverlayProvider.shows(org.overengineer.bettacraft.compat.info.OverlayProvider.Overlay.JADE)) return;
            if (accessor.getEntity() instanceof org.overengineer.bettacraft.aquarist.AquaristEntity a) InfoLines.aquarist(a).forEach(tooltip::add);
        }

        @Override
        public Identifier getUid() {
            return BettaCraftJadePlugin.AQUARIST;
        }
    }

    static final class Betta implements IEntityComponentProvider {
        static final Betta INSTANCE = new Betta();

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (!org.overengineer.bettacraft.compat.info.OverlayProvider.shows(org.overengineer.bettacraft.compat.info.OverlayProvider.Overlay.JADE)) return;
            if (!(accessor.getEntity() instanceof BettaEntity b)) return;
            InfoLines.betta(b, accessor.getServerData().getCompound(InfoData.KEY), accessor.showDetails()).forEach(tooltip::add);
        }

        @Override
        public Identifier getUid() {
            return BettaCraftJadePlugin.BETTA;
        }
    }
}
