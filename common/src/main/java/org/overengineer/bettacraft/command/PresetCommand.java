package org.overengineer.bettacraft.command;

import org.overengineer.bettacraft.util.Perms;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.Preset;
import org.overengineer.bettacraft.config.Presets;
import org.overengineer.bettacraft.network.FeatureSyncPayload;

/**
 * M33 part c: {@code /bettacraft preset} shows the active preset and the values it sets now; {@code /bettacraft preset
 * <custom|simple|realistic|decor>} (operators, level 3) writes the key {@code preset} into the common config, sends the
 * switches to every client and asks for {@code /reload} (recipes and loot).
 */
public final class PresetCommand {
    private PresetCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("preset")
                .executes(ctx -> show(ctx.getSource()))
                .then(Commands.argument("name", StringArgumentType.word()).requires(s -> Perms.has(s, 3))
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(java.util.Arrays.stream(Preset.values()).map(Preset::id), b))
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            Preset p = Preset.byId(name);
                            if (p == null) {
                                ctx.getSource().sendFailure(Component.translatable("command.bettacraft.preset.unknown", name));
                                return 0;
                            }
                            apply(ctx.getSource().getServer(), p, true);
                            report(ctx.getSource(), p);
                            return 1;
                        }));
    }

    /** Makes {@code p} the active preset (saved into the file or, for GameTests, only in memory) and tells the clients. */
    public static void apply(MinecraftServer server, Preset p, boolean save) {
        CommonConfig.PRESET.set(p);
        if (save) CommonConfig.PRESET.save();
        if (server != null) FeatureSyncPayload.sendAll(server);
    }

    private static void report(CommandSourceStack src, Preset p) {
        int n = Presets.inEffect().size();
        src.sendSuccess(() -> Component.translatable("command.bettacraft.preset.set", name(p), n), true);
        Component reload = Component.literal("/reload").withStyle(s -> s.withColor(ChatFormatting.AQUA).withUnderlined(true)
                .withClickEvent(new ClickEvent.SuggestCommand("/reload"))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("/reload"))));
        src.sendSuccess(() -> Component.translatable("command.bettacraft.preset.reload", reload), false);
    }

    private static int show(CommandSourceStack src) {
        Preset p = Presets.active();
        var keep = Presets.keep();
        src.sendSuccess(() -> Component.translatable("command.bettacraft.preset.active", name(p),
                keep.isEmpty() ? Component.translatable("command.bettacraft.preset.none") : Component.literal(String.join(", ", keep))), false);
        var live = Presets.inEffect();
        for (Presets.Entry e : live) {
            src.sendSuccess(() -> Component.literal("  " + e.path() + " = " + e.value()).withStyle(ChatFormatting.GRAY), false);
        }
        int overridden = Presets.overridden().size();
        if (overridden > 0) src.sendSuccess(() -> Component.translatable("command.bettacraft.preset.overridden", overridden), false);
        return live.size();
    }

    private static Component name(Preset p) {
        return Component.translatable("preset.bettacraft." + p.id()).withStyle(ChatFormatting.GOLD);
    }
}
