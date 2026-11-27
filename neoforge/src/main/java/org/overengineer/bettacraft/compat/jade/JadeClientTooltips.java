package org.overengineer.bettacraft.compat.jade;

import net.minecraft.resources.ResourceLocation;
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

    /**
     * M66: a pipe carries water, it does not store any. Jade's own fluid view shows "Empty 500 mB" for it (the pipe offers its
     * throughput as a tank size); this removes that line and says how much the pipe carries a second instead.
     */
    static final class Pipe implements IBlockComponentProvider {
        static final Pipe INSTANCE = new Pipe();

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!org.overengineer.bettacraft.compat.info.OverlayProvider.shows(org.overengineer.bettacraft.compat.info.OverlayProvider.Overlay.JADE)) return;
            tooltip.remove(snownee.jade.api.JadeIds.UNIVERSAL_FLUID_STORAGE);
            // M68: its role (ring), throughput and what it moves now (server data)
            net.minecraft.nbt.CompoundTag d = accessor.getServerData().getCompound(InfoData.KEY);
            InfoLines.pipe(accessor.getLevel(), accessor.getPosition(), accessor.getBlockState(), d.isEmpty() ? null : d).forEach(tooltip::add);
        }

        @Override
        public ResourceLocation getUid() {
            return BettaCraftJadePlugin.PIPE;
        }

        /** After Jade's own fluid view (it is added late), so that the line can be taken out again. */
        @Override
        public int getDefaultPriority() {
            return 10000;
        }
    }

    static final class Tank implements IBlockComponentProvider {
        static final Tank INSTANCE = new Tank();

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!org.overengineer.bettacraft.compat.info.OverlayProvider.shows(org.overengineer.bettacraft.compat.info.OverlayProvider.Overlay.JADE)) return;
            net.minecraft.nbt.CompoundTag d = accessor.getServerData().getCompound(InfoData.KEY);
            // M64: the tank's name instead of the block name "Tank"
            net.minecraft.network.chat.Component title = InfoLines.tankTitle(d);
            if (title != null) tooltip.replace(snownee.jade.api.JadeIds.CORE_OBJECT_NAME, snownee.jade.api.theme.IThemeHelper.get().title(title));
            InfoLines.tank(d, accessor.showDetails()).forEach(tooltip::add);
        }

        @Override
        public ResourceLocation getUid() {
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
        public ResourceLocation getUid() {
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
        public ResourceLocation getUid() {
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
        public ResourceLocation getUid() {
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
        public ResourceLocation getUid() {
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
        public ResourceLocation getUid() {
            return BettaCraftJadePlugin.BETTA;
        }
    }
}
