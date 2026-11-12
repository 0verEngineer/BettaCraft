package org.overengineer.bettacraft.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.tank.interior.InteriorBench;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.overengineer.bettacraft.tank.interior.InteriorSync;

/** {@code /bettacraft ...} debug and admin commands. */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class BettaCraftCommands {
    private BettaCraftCommands() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal(BettaCraft.MOD_ID)
                .then(Commands.literal("interior")
                        .then(Commands.literal("leave").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            if (InteriorManager.isInterior(p.level())) InteriorManager.leave(p);
                            return 1;
                        }))
                        .then(Commands.literal("bench").requires(s -> s.hasPermission(2)).executes(ctx -> {
                            for (InteriorBench.Result r : InteriorBench.runDefault()) {
                                ctx.getSource().sendSuccess(() -> Component.literal(r.toString()), false);
                            }
                            var src = ctx.getSource();
                            boolean started = InteriorBench.startRealRegions(src.getServer(), line -> src.sendSuccess(() -> Component.literal(line), false));
                            src.sendSuccess(() -> Component.literal(started ? "Real 16x16x16 regions are prepared in the background; results follow"
                                    : "Real-region bench not started (already running or no interior dimension)"), false);
                            return 1;
                        }))
                        .then(Commands.literal("resync").requires(s -> s.hasPermission(2)).executes(ctx -> {
                            int n = 0;
                            for (InteriorRegion r : InteriorRegions.get(ctx.getSource().getServer()).all()) {
                                InteriorSync.markDirty(r.id());
                                n++;
                            }
                            int count = n;
                            ctx.getSource().sendSuccess(() -> Component.literal("Resyncing " + count + " interior region(s)"), true);
                            return n;
                        }))));
    }
}
