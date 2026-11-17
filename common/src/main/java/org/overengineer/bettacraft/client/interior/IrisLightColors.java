package org.overengineer.bettacraft.client.interior;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M18: coloured block light of the aquarium lights under shader packs. Packs such as Complementary colour the block
 * light by the block id their {@code block.properties} gives a block (Complementary: with its "Colored Lighting"
 * option); our light is in no pack. When Iris takes the ids of a pack ({@code WorldRenderingSettings#setBlockStateIds},
 * see {@code IrisBlockIdsMixin}), every lit state of a coloured light gets the id the pack gave a vanilla light source
 * of the same colour (see {@link #reference}). White lights keep the look they had before M18, and a pack that maps our light itself keeps its own ids.
 * Without Iris nothing happens; the water tint and the panel colour work with any pack.
 */
public final class IrisLightColors {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/iris");
    /** Number of light states that got an id the last time (for the smoke test). */
    public static volatile int aliased;

    private IrisLightColors() {}

    /** The ids with the lit coloured lights added (a copy; the input is returned unchanged if nothing is added). */
    public static Object2IntMap<BlockState> withLights(@Nullable Object2IntMap<BlockState> ids) {
        if (ids == null) return null;
        try {
            Object2IntOpenHashMap<BlockState> copy = new Object2IntOpenHashMap<>(ids);
            copy.defaultReturnValue(ids.defaultReturnValue());
            int n = alias(copy);
            aliased = n;
            if (n == 0) return ids;
            LOGGER.info("Shader pack light colours: {} aquarium light states use the ids of coloured vanilla lights", n);
            return copy;
        } catch (RuntimeException e) {
            LOGGER.warn("Could not give the aquarium lights coloured shader light", e);
            return ids;
        }
    }

    /** Adds the lit light states with the id of their reference block; returns how many. */
    static int alias(Object2IntMap<BlockState> ids) {
        int n = 0;
        java.util.List<BlockState> all = new java.util.ArrayList<>(ModBlocks.TANK_LIGHT.get().getStateDefinition().getPossibleStates());
        all.addAll(ModBlocks.LED_PANEL.get().getStateDefinition().getPossibleStates()); // M23
        all.addAll(ModBlocks.HIGH_POWER_LED.get().getStateDefinition().getPossibleStates());
        for (BlockState s : all) {
            if (!s.getValue(TankLightBlock.LIT) || ids.containsKey(s)) continue;
            BlockState ref = reference(s.getValue(TankLightBlock.COLOR));
            if (ref == null || !ids.containsKey(ref)) continue;
            ids.put(s, ids.getInt(ref));
            n++;
        }
        return n;
    }

    /**
     * The vanilla light source whose shader light colour stands in for a light colour; null for white. Strong, always
     * coloured lights of Complementary's table (its coloured candles are off by default): redstone ore (red),
     * shroomlight (orange), the froglights (yellow, green, pink), sea lantern (light blue), soul lantern (cyan), lapis
     * block (blue), crying obsidian (purple, violet), lantern (brown: warm), end rod (grey).
     */
    @Nullable
    public static BlockState reference(DyeColor c) {
        return switch (c) {
            case WHITE -> null;
            case GRAY, LIGHT_GRAY -> Blocks.END_ROD.defaultBlockState();
            case BROWN -> Blocks.LANTERN.defaultBlockState();
            case BLACK, PURPLE -> Blocks.CRYING_OBSIDIAN.defaultBlockState();
            case ORANGE -> Blocks.SHROOMLIGHT.defaultBlockState();
            case MAGENTA, PINK -> Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState();
            case LIGHT_BLUE -> Blocks.SEA_LANTERN.defaultBlockState();
            case YELLOW -> Blocks.OCHRE_FROGLIGHT.defaultBlockState();
            case LIME, GREEN -> Blocks.VERDANT_FROGLIGHT.defaultBlockState();
            case CYAN -> Blocks.SOUL_LANTERN.defaultBlockState();
            case BLUE -> Blocks.LAPIS_BLOCK.defaultBlockState();
            case RED -> Blocks.REDSTONE_ORE.defaultBlockState().setValue(BlockStateProperties.LIT, true);
        };
    }
}
