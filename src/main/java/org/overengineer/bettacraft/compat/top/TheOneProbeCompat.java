package org.overengineer.bettacraft.compat.top;

import java.util.List;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.compat.info.InfoData;
import org.overengineer.bettacraft.compat.info.InfoLines;
import org.overengineer.bettacraft.compat.info.InfoTarget;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.tank.TankBlock;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeHitEntityData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoEntityProvider;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;

/**
 * The One Probe integration (optional, PLAN 10, M8): the same information as the Jade plugin, built by
 * {@link InfoData}/{@link InfoLines}. TOP asks on the server, so the lines are built there and sent as components.
 * Only touched after {@code ModList.isLoaded("theoneprobe")} ({@link #register}).
 */
public final class TheOneProbeCompat implements Function<ITheOneProbe, Void> {
    public static final String MOD_ID = "theoneprobe";

    /** Hands our providers to TOP through its IMC entry point. */
    public static void register(net.neoforged.bus.api.IEventBus modBus) {
        modBus.addListener((InterModEnqueueEvent e) -> InterModComms.sendTo(MOD_ID, "getTheOneProbe", TheOneProbeCompat::new));
    }

    @Override
    public Void apply(ITheOneProbe probe) {
        probe.registerProvider(new Blocks());
        probe.registerEntityProvider(new Bettas());
        // looking through the glass at a fish shows the fish, not the tank block
        probe.registerBlockDisplayOverride((mode, info, player, level, state, data) -> {
            if (!(state.getBlock() instanceof TankBlock)) return false;
            if (!org.overengineer.bettacraft.compat.info.OverlayProvider.shows(org.overengineer.bettacraft.compat.info.OverlayProvider.Overlay.TOP)) return false;
            EntityHitResult hit = InfoTarget.bettaBehindGlass(level, player);
            if (hit == null || !(hit.getEntity() instanceof BettaEntity b)) return false;
            IProbeInfo head = info.horizontal();
            head.entity(b);
            IProbeInfo names = head.vertical();
            names.mcText(b.getDisplayName().copy().withStyle(ChatFormatting.WHITE));
            names.mcText(Component.literal("Bettacraft").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
            betta(info, mode, b);
            return true;
        });
        return null;
    }

    private static void add(IProbeInfo info, List<Component> lines) {
        for (Component c : lines) info.mcText(c);
    }

    private static void betta(IProbeInfo info, ProbeMode mode, BettaEntity b) {
        add(info, InfoLines.betta(b, InfoData.betta(b), mode != ProbeMode.NORMAL));
    }

    /** Tank, external filter and cabinet. */
    static final class Blocks implements IProbeInfoProvider {
        @Override
        public ResourceLocation getID() {
            return BettaCraft.id("blocks");
        }

        @Override
        public void addProbeInfo(ProbeMode mode, IProbeInfo info, Player player, Level level, BlockState state, IProbeHitData data) {
            if (!(level instanceof ServerLevel server)) return;
            if (!org.overengineer.bettacraft.compat.info.OverlayProvider.shows(org.overengineer.bettacraft.compat.info.OverlayProvider.Overlay.TOP)) return;
            if (state.getBlock() instanceof TankBlock) {
                if (InfoTarget.bettaBehindGlass(level, player) != null) return; // the display override shows the fish
                add(info, InfoLines.tank(InfoData.tank(server, data.getPos()), mode != ProbeMode.NORMAL));
            } else if (state.getBlock() instanceof ExternalFilterBlock || state.getBlock() instanceof CabinetBlock) {
                CompoundTag d = InfoData.filter(server, level.getBlockEntity(data.getPos()));
                add(info, InfoLines.filter(d));
            } else if (state.getBlock() instanceof org.overengineer.bettacraft.food.CultureTubBlock) {
                add(info, InfoLines.culture(InfoData.culture(server, level.getBlockEntity(data.getPos()))));
            } else if (level.getBlockEntity(data.getPos()) instanceof org.overengineer.bettacraft.winding.Windable w) { // M37: the spring
                add(info, InfoLines.spring(InfoData.spring(server, (net.minecraft.world.level.block.entity.BlockEntity) w)));
            }
        }
    }

    static final class Bettas implements IProbeInfoEntityProvider {
        @Override
        public String getID() {
            return BettaCraft.id("betta").toString();
        }

        @Override
        public void addProbeEntityInfo(ProbeMode mode, IProbeInfo info, Player player, Level level, Entity entity, IProbeHitEntityData data) {
            if (entity instanceof BettaEntity b && org.overengineer.bettacraft.compat.info.OverlayProvider.shows(org.overengineer.bettacraft.compat.info.OverlayProvider.Overlay.TOP)) betta(info, mode, b);
        }
    }
}
