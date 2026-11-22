package org.overengineer.bettacraft.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.nio.file.Path;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.overengineer.bettacraft.debug.BugReport;
import org.overengineer.bettacraft.debug.DebugLog;
import org.overengineer.bettacraft.debug.LogArea;

/**
 * M57: {@code /bettacraft debug} shows which areas write debug lines; {@code /bettacraft debug <area|all> <on|off|trace>}
 * switches them at once until the restart, {@code /bettacraft debug reset} goes back to the config ({@code debug.*} of
 * the common config). {@code /bettacraft report} writes the bug report ({@link BugReport}) with the tank the player looks
 * at. Both for operators and the owner of a single player world (also without cheats).
 */
public final class DebugCommand {
    private DebugCommand() {}

    static boolean allowed(CommandSourceStack s) {
        if (s.hasPermission(2)) return true;
        return s.getEntity() instanceof ServerPlayer p && s.getServer().isSingleplayerOwner(p.getGameProfile());
    }

    public static LiteralArgumentBuilder<CommandSourceStack> debug() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("debug").requires(DebugCommand::allowed)
                .executes(ctx -> status(ctx.getSource()))
                .then(Commands.literal("reset").executes(ctx -> {
                    DebugLog.override(null, null);
                    ctx.getSource().sendSuccess(() -> Component.translatable("command.bettacraft.debug.reset"), true);
                    return status(ctx.getSource());
                }));
        root.then(target("all", null));
        for (LogArea a : LogArea.values()) root.then(target(a.id, a));
        return root;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> target(String name, @Nullable LogArea area) {
        return Commands.literal(name)
                .then(Commands.literal("on").executes(ctx -> set(ctx.getSource(), name, area, DebugLog.Level.DEBUG)))
                .then(Commands.literal("trace").executes(ctx -> set(ctx.getSource(), name, area, DebugLog.Level.TRACE)))
                .then(Commands.literal("off").executes(ctx -> set(ctx.getSource(), name, area, DebugLog.Level.INFO)));
    }

    private static int set(CommandSourceStack src, String name, @Nullable LogArea area, DebugLog.Level level) {
        DebugLog.override(area, level);
        src.sendSuccess(() -> Component.translatable("command.bettacraft.debug.set", name,
                Component.translatable("command.bettacraft.debug.level." + level.name().toLowerCase(java.util.Locale.ROOT)), path(DebugLog.file().file())), true);
        return 1;
    }

    static int status(CommandSourceStack src) {
        src.sendSuccess(() -> Component.translatable("command.bettacraft.debug.header", path(DebugLog.file().file()), DebugLog.configLevel().name()), false);
        int on = 0;
        for (LogArea a : LogArea.values()) {
            DebugLog.Level l = DebugLog.level(a);
            if (l != DebugLog.Level.INFO) on++;
            Component lv = Component.translatable("command.bettacraft.debug.level." + l.name().toLowerCase(java.util.Locale.ROOT))
                    .withStyle(l == DebugLog.Level.INFO ? ChatFormatting.GRAY : ChatFormatting.GREEN);
            src.sendSuccess(() -> Component.translatable(DebugLog.overridden(a) ? "command.bettacraft.debug.area_command" : "command.bettacraft.debug.area", a.id, lv), false);
        }
        return on;
    }

    /** A path to click on: copies it (works on every client, also for a file on a server). */
    static Component path(Path p) {
        String s = p.toAbsolutePath().normalize().toString();
        return Component.literal(s).withStyle(st -> st.withColor(ChatFormatting.AQUA).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, s))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("command.bettacraft.report.copy"))));
    }

    static Component link() {
        MutableComponent c = Component.translatable("command.bettacraft.report.link");
        return c.withStyle(st -> st.withColor(ChatFormatting.AQUA).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, BugReport.ISSUE_URL))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(BugReport.ISSUE_URL))));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> report() {
        return Commands.literal("report").requires(DebugCommand::allowed).executes(ctx -> {
            CommandSourceStack src = ctx.getSource();
            ServerPlayer p = src.getEntity() instanceof ServerPlayer sp ? sp : null;
            var tank = p == null ? null : BugReport.lookedAtTank(p);
            var entity = p == null ? null : BugReport.lookedAtEntity(p);
            Path file;
            try {
                List<String> lines = BugReport.lines(src.getServer(), p, tank, entity);
                file = BugReport.write(lines);
            } catch (Exception e) {
                src.sendFailure(Component.translatable("command.bettacraft.report.failed", e.toString()));
                return 0;
            }
            Path written = file;
            src.sendSuccess(() -> Component.translatable(tank != null ? "command.bettacraft.report.written_tank" : "command.bettacraft.report.written",
                    path(written)), false);
            src.sendSuccess(() -> Component.translatable("command.bettacraft.report.issue", link()), false);
            if (!DebugLog.anyOn()) src.sendSuccess(() -> Component.translatable("command.bettacraft.report.debug_hint"), false);
            return 1;
        });
    }
}
