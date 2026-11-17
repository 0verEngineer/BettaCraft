package org.overengineer.bettacraft.command;

import org.overengineer.bettacraft.util.Perms;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.tank.interior.InteriorBench;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.overengineer.bettacraft.tank.interior.InteriorSync;

/** {@code /bettacraft ...} debug and admin commands. */
public final class BettaCraftCommands {
    private BettaCraftCommands() {}


    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal(BettaCraft.MOD_ID)
                .then(Commands.literal("interior")
                        .then(Commands.literal("leave").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            if (InteriorManager.isInterior(p.level())) InteriorManager.leave(p);
                            return 1;
                        }))
                        .then(Commands.literal("bench").requires(s -> Perms.has(s, 2)).executes(ctx -> {
                            for (InteriorBench.Result r : InteriorBench.runDefault()) {
                                ctx.getSource().sendSuccess(() -> Component.literal(r.toString()), false);
                            }
                            var src = ctx.getSource();
                            boolean started = InteriorBench.startRealRegions(src.getServer(), line -> src.sendSuccess(() -> Component.literal(line), false));
                            src.sendSuccess(() -> Component.literal(started ? "Real 16x16x16 regions are prepared in the background; results follow"
                                    : "Real-region bench not started (already running or no interior dimension)"), false);
                            return 1;
                        }))
                        .then(Commands.literal("resync").requires(s -> Perms.has(s, 2)).executes(ctx -> {
                            int n = 0;
                            for (InteriorRegion r : InteriorRegions.get(ctx.getSource().getServer()).all()) {
                                InteriorSync.markDirty(r.id());
                                n++;
                            }
                            int count = n;
                            ctx.getSource().sendSuccess(() -> Component.literal("Resyncing " + count + " interior region(s)"), true);
                            return n;
                        })))
                // M28.1: where the wild animals are and why (not)
                .then(WildlifeCommand.build())
                // M33 part c: the presets
                .then(PresetCommand.build())
                // M33 part d: the foreign animals in tanks and their profiles
                .then(ForeignCommand.build())
                .then(AquaristCommand.build()) // M51
                // M16: every water animal of the loaded mods and whether tanks take it (table in bettacraft-water-animals.tsv)
                .then(Commands.literal("wateranimals").requires(s -> Perms.has(s, 2)).executes(ctx -> {
                    var src = ctx.getSource();
                    var all = org.overengineer.bettacraft.compat.AquaticSpecies.discover(src.getLevel());
                    var lines = org.overengineer.bettacraft.compat.AquaticSpecies.table(all);
                    java.nio.file.Path file = src.getServer().getServerDirectory().resolve("bettacraft-water-animals.tsv");
                    try {
                        java.nio.file.Files.write(file, lines, java.nio.charset.StandardCharsets.UTF_8);
                    } catch (java.io.IOException e) {
                        src.sendFailure(Component.literal("Cannot write " + file + ": " + e.getMessage()));
                    }
                    long ok = all.stream().filter(org.overengineer.bettacraft.compat.AquaticSpecies.Species::accepted).count();
                    src.sendSuccess(() -> Component.translatable("command.bettacraft.wateranimals", all.size(), ok, all.size() - ok, file.toString()), false);
                    return (int) ok;
                })));
    }
}
