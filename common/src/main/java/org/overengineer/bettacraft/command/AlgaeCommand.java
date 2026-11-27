package org.overengineer.bettacraft.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.algae.AlgaeSim;
import org.overengineer.bettacraft.tank.algae.TankAlgae;

import java.util.ArrayList;
import java.util.List;

/**
 * Operators: {@code /bettacraft algae <0..1> [radiusChunks]} sets the algae on every wet glass face of every tank around the
 * command's position to that level (0 = clean). For trying the algae film and its frame cost in a whole world at once.
 */
public final class AlgaeCommand {
    private AlgaeCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("algae").requires(s -> org.overengineer.bettacraft.util.Perms.has(s, 2))
                .then(Commands.argument("level", FloatArgumentType.floatArg(0F, 1F))
                        .executes(ctx -> run(ctx.getSource(), FloatArgumentType.getFloat(ctx, "level"), 16))
                        .then(Commands.argument("radiusChunks", IntegerArgumentType.integer(1, 64))
                                .executes(ctx -> run(ctx.getSource(), FloatArgumentType.getFloat(ctx, "level"),
                                        IntegerArgumentType.getInteger(ctx, "radiusChunks")))));
    }

    private static int run(CommandSourceStack src, float level, int radius) {
        ServerLevel world = src.getLevel();
        int cx0 = (int) Math.floor(src.getPosition().x) >> 4, cz0 = (int) Math.floor(src.getPosition().z) >> 4;
        int tanks = 0;
        for (int cx = cx0 - radius; cx <= cx0 + radius; cx++) {
            for (int cz = cz0 - radius; cz <= cz0 + radius; cz++) {
                LevelChunk chunk = world.getChunk(cx, cz);
                List<BlockEntity> bes = new ArrayList<>(chunk.getBlockEntities().values());
                for (BlockEntity be : bes) {
                    if (!(be instanceof TankBlockEntity t) || !t.isController() || t.getTank() == null) continue;
                    t.getTank().algae().clear();
                    if (level > 0) for (var f : TankAlgae.wetFaces(t.getTank())) AlgaeSim.set(t.getTank().algae(), f.pos(), f.dir(), level);
                    t.markTankChanged();
                    tanks++;
                }
            }
        }
        final int n = tanks;
        final String pct = Math.round(level * 100) + "%";
        src.sendSuccess(() -> Component.translatable("command.bettacraft.algae.done", pct, n, radius), true);
        return tanks;
    }
}
