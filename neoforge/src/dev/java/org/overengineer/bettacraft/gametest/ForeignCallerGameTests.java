package org.overengineer.bettacraft.gametest;

import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.platform.Platform;
import org.overengineer.bettacraft.registry.ModBlocks;

/**
 * M61: other mods call methods with null arguments where vanilla never reads them (Modern Dynamics:
 * {@code sendBlockUpdated(pos, null, null, 0)}, see {@code PlantMeshRendererNullStateTest} and the client smoke M61). The
 * blocks whose behaviour depends on their block entity (stone cabinets, covered filter pipes) look it up by level and
 * position in exactly such methods: the map colour, the destroy progress, the sound, the blast resistance, the right tool,
 * the light. Without a level or position they act as a plain block (the wood cabinet, an uncovered pipe) and never throw.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ForeignCallerGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private ForeignCallerGameTests() {}

    @GameTest(template = EMPTY)
    public static void positionalBlocksWithoutLevelOrPosition(GameTestHelper h) {
        ServerPlayer player = MockPlayers.serverPlayer(h);
        List<BlockState> states = List.of(ModBlocks.CABINET.get().defaultBlockState(), ModBlocks.FILTER_INLET_PIPE.get().defaultBlockState(),
                ModBlocks.FILTER_PIPE_BUNDLE.get().defaultBlockState());
        for (BlockState s : states) {
            String name = s.getBlock().getDescriptionId();
            try {
                h.assertTrue(s.getMapColor(null, null) != null, "map colour of " + name);
                h.assertTrue(s.getDestroyProgress(player, null, null) >= 0, "destroy progress of " + name);
                h.assertTrue(Platform.get().soundType(s, null, null, null) != null, "sound of " + name);
                h.assertTrue(Platform.get().explosionResistance(s, null, null, null) >= 0, "blast resistance of " + name);
                Platform.get().canHarvestBlock(s, null, null, player);
                h.assertTrue(Platform.get().lightEmission(s, null, null) >= 0, "light of " + name);
            } catch (RuntimeException e) {
                throw new IllegalStateException(name + " without a level or position: " + e, e);
            }
        }
        h.succeed();
    }
}
