package org.overengineer.bettacraft.command;

import org.overengineer.bettacraft.util.Perms;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.overengineer.bettacraft.aquarist.AquaristArrival;
import org.overengineer.bettacraft.aquarist.AquaristEntity;
import org.overengineer.bettacraft.aquarist.AquaristRecords;
import org.overengineer.bettacraft.config.ServerConfig;

/**
 * M51: {@code /bettacraft aquarist hire|decline} (the buttons of the offer in the chat; every player for his own
 * aquarist), {@code status} (why none comes yet) and for operators {@code arrive [player]} (one comes now, whatever the
 * conditions).
 */
final class AquaristCommand {
    private AquaristCommand() {}

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("aquarist")
                .then(Commands.literal("hire").executes(ctx -> answer(ctx.getSource(), true)))
                .then(Commands.literal("decline").executes(ctx -> answer(ctx.getSource(), false)))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).requires(s -> Perms.has(s, 2))
                                .executes(ctx -> status(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("arrive").requires(s -> Perms.has(s, 2))
                        .executes(ctx -> arrive(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> arrive(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))));
    }

    private static int answer(CommandSourceStack src, boolean hire) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        Component err = hire ? AquaristArrival.hire(p) : AquaristArrival.decline(p);
        if (err != null) {
            src.sendFailure(err);
            return 0;
        }
        return 1;
    }

    private static int status(CommandSourceStack src, ServerPlayer p) {
        String why = AquaristArrival.whyNot(p, false);
        AquaristRecords rec = AquaristRecords.get(p.level().getServer());
        AquaristRecords.PlayerRecord r = rec.player(p.getUUID());
        int tanks = rec.ownTanks(p.level().getServer(), p.getUUID());
        double wait = Math.max(0, (r.notBefore - p.level().getServer().overworld().getGameTime()) / 24000.0);
        Component reason = why == null ? Component.translatable("message.bettacraft.aquarist.why.none")
                : Component.translatable("message.bettacraft.aquarist.why." + why, tanks, ServerConfig.getOr(ServerConfig.AQUARIST_MIN_TANKS, 3),
                String.format(java.util.Locale.ROOT, "%.1f", wait));
        src.sendSuccess(() -> Component.translatable("message.bettacraft.aquarist.status_line", p.getDisplayName(), tanks, r.breedings, r.refused, r.left)
                .withStyle(ChatFormatting.AQUA), false);
        src.sendSuccess(() -> reason.copy().withStyle(why == null ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
        return why == null ? 1 : 0;
    }

    private static int arrive(CommandSourceStack src, ServerPlayer p) {
        AquaristRecords.PlayerRecord r = AquaristRecords.get(p.level().getServer()).player(p.getUUID());
        if (r.npc != null) {
            src.sendFailure(Component.translatable("message.bettacraft.aquarist.why." + (r.hired ? "hired" : "offering"), 0, 0, "0"));
            return 0;
        }
        AquaristEntity npc = AquaristArrival.arrive(p);
        if (npc == null) {
            src.sendFailure(Component.translatable("message.bettacraft.aquarist.no_spot"));
            return 0;
        }
        src.sendSuccess(() -> Component.translatable("message.bettacraft.aquarist.coming", npc.getName()), true);
        return 1;
    }
}
