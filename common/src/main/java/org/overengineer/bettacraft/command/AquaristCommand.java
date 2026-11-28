package org.overengineer.bettacraft.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.overengineer.bettacraft.aquarist.AquaristArrival;
import org.overengineer.bettacraft.aquarist.AquaristEntity;
import org.overengineer.bettacraft.aquarist.AquaristMenu;
import org.overengineer.bettacraft.aquarist.AquaristRecords;
import org.overengineer.bettacraft.aquarist.AquaristTeam;
import org.overengineer.bettacraft.config.ServerConfig;

/**
 * M51: {@code /bettacraft aquarist hire|decline} (the buttons of the offer in the chat; every player for his own
 * aquarist), {@code status} (why none comes yet) and for operators {@code arrive [player]} (one comes now, whatever the
 * conditions). M69: {@code status} lists the whole team and the conditions of the next friend; {@code friend show} (the
 * button of the recommendation: the menu of the one who recommends, at the tab Friend), {@code friend hire|decline}, and
 * for operators {@code friend [player]} (the friend is recommended now, whatever the conditions but the team limit).
 */
final class AquaristCommand {
    private AquaristCommand() {}

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("aquarist")
                .then(Commands.literal("hire").executes(ctx -> answer(ctx.getSource(), true)))
                .then(Commands.literal("decline").executes(ctx -> answer(ctx.getSource(), false)))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).requires(s -> s.hasPermission(2))
                                .executes(ctx -> status(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("arrive").requires(s -> s.hasPermission(2))
                        .executes(ctx -> arrive(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> arrive(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("friend")
                        .executes(ctx -> recommend(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.literal("show").executes(ctx -> show(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                        .then(Commands.literal("hire").executes(ctx -> friendAnswer(ctx.getSource(), true)))
                        .then(Commands.literal("decline").executes(ctx -> friendAnswer(ctx.getSource(), false)))
                        .then(Commands.argument("player", EntityArgument.player()).requires(s -> s.hasPermission(2))
                                .executes(ctx -> recommend(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))));
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
        AquaristRecords rec = AquaristRecords.get(p.getServer());
        AquaristRecords.PlayerRecord r = rec.player(p.getUUID());
        int tanks = rec.ownTanks(p.getServer(), p.getUUID());
        double wait = Math.max(0, (r.notBefore - p.getServer().overworld().getGameTime()) / 24000.0);
        Component reason = why == null ? Component.translatable("message.bettacraft.aquarist.why.none")
                : Component.translatable("message.bettacraft.aquarist.why." + why, tanks, ServerConfig.getOr(ServerConfig.AQUARIST_MIN_TANKS, 3),
                String.format(java.util.Locale.ROOT, "%.1f", wait));
        src.sendSuccess(() -> Component.translatable("message.bettacraft.aquarist.status_line", p.getDisplayName(), tanks, r.breedings, r.refused, r.left)
                .withStyle(ChatFormatting.AQUA), false);
        src.sendSuccess(() -> reason.copy().withStyle(why == null ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
        // M69: every aquarist of the team, then the next friend
        AquaristRecords.Member med = r.mediator();
        for (AquaristRecords.Member m : r.team) {
            if (!m.hired) continue;
            String pos = m.workplace == null ? "-" : m.workplace.getX() + " " + m.workplace.getY() + " " + m.workplace.getZ();
            Component st = Component.translatable("gui.bettacraft.aquarist.status." + (m.status.isEmpty() ? "unloaded" : m.status));
            String name = (med != null && med.id.equals(m.id) ? "★ " : "") + m.name;
            src.sendSuccess(() -> Component.translatable("message.bettacraft.aquarist.team.status_line", name, st, m.cared, pos).withStyle(ChatFormatting.WHITE), false);
        }
        if (med != null) {
            if (r.friendOffered) {
                src.sendSuccess(() -> Component.translatable("message.bettacraft.aquarist.team.friend_offered", r.friendName).withStyle(ChatFormatting.GREEN), false);
            } else {
                for (AquaristTeam.Condition c : AquaristTeam.conditions(p.getServer(), p.getUUID(), r)) {
                    src.sendSuccess(() -> Component.literal(c.ok() ? "✔ " : "✘ ").withStyle(c.ok() ? ChatFormatting.GREEN : ChatFormatting.RED)
                            .append(Component.translatable("gui.bettacraft.aquarist.friend.cond." + c.key(), c.have(), c.need()).withStyle(ChatFormatting.GRAY)), false);
                }
            }
        }
        return why == null ? 1 : 0;
    }

    private static int arrive(CommandSourceStack src, ServerPlayer p) {
        AquaristRecords.PlayerRecord r = AquaristRecords.get(p.getServer()).player(p.getUUID());
        if (r.npc() != null) {
            src.sendFailure(Component.translatable("message.bettacraft.aquarist.why." + (r.hired() ? "hired" : "offering"), 0, 0, "0"));
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

    /** M69, operators: the one who recommends friends recommends his friend now (only the team limit counts). */
    private static int recommend(CommandSourceStack src, ServerPlayer p) {
        if (!src.hasPermission(2)) {
            src.sendFailure(Component.translatable("message.bettacraft.aquarist.team.op_only"));
            return 0;
        }
        AquaristRecords.PlayerRecord r = AquaristRecords.get(p.getServer()).player(p.getUUID());
        if (r.mediator() == null) {
            src.sendFailure(Component.translatable("message.bettacraft.aquarist.team.no_mediator"));
            return 0;
        }
        if (r.hiredCount() >= AquaristTeam.max()) {
            src.sendFailure(Component.translatable("message.bettacraft.aquarist.friend.full", r.hiredCount(), AquaristTeam.max()));
            return 0;
        }
        AquaristTeam.offer(p.getServer(), p, r);
        return 1;
    }

    /** M69: the button of the recommendation: the menu of the one who recommends, at the tab Friend (when he is near). */
    private static int show(CommandSourceStack src, ServerPlayer p) {
        AquaristRecords.PlayerRecord r = AquaristRecords.get(p.getServer()).player(p.getUUID());
        AquaristEntity med = AquaristTeam.mediatorEntity(p.getServer(), r);
        if (med == null || med.level() != p.level() || p.distanceToSqr(med) > 8 * 8) {
            AquaristRecords.Member m = r.mediator();
            if (m == null) {
                src.sendFailure(Component.translatable("message.bettacraft.aquarist.team.no_mediator"));
                return 0;
            }
            String pos = m.workplace == null ? "?" : m.workplace.getX() + " " + m.workplace.getY() + " " + m.workplace.getZ();
            src.sendSuccess(() -> Component.translatable("message.bettacraft.aquarist.team.go_to", m.name, pos).withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }
        AquaristMenu.open(p, med);
        return 1;
    }

    private static int friendAnswer(CommandSourceStack src, boolean hire) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        Component err = hire ? AquaristTeam.hireFriend(p) : AquaristTeam.declineFriend(p);
        if (err != null) {
            src.sendFailure(err);
            return 0;
        }
        return 1;
    }
}
