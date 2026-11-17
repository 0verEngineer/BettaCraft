package org.overengineer.bettacraft.tank;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import org.overengineer.bettacraft.BettaCraft;

/**
 * Server tick work of tanks: removes water sources left behind by removed tank blocks (end of the same tick); sneaking
 * clicks with filled containers.
 */
public final class TankEvents {
    record Pending(ResourceKey<Level> dim, BlockPos pos) {}

    static final List<Pending> DRY = new ArrayList<>();
    /** Tank inhabitants a player interacted with this tick (bucket, fish bag, Better Fish Tanks bag, Fintastic net). */
    static final java.util.Map<java.util.UUID, String> CATCHING = new java.util.HashMap<>();

    private TankEvents() {}

    /** M17: nothing pending may run in the next world of the same game (integrated server). */
    public static void onServerStopped() {
        DRY.clear();
        NOT_HERE.clear();
        CATCHING.clear();
        ForeignAnimals.clear();
    }

    static void dryLater(ServerLevel level, BlockPos pos) {
        DRY.add(new Pending(level.dimension(), pos.immutable()));
    }

    /** M16: tank blocks set into the interior dimension without the item; broken (and dropped) at the end of the tick. */
    static final List<Pending> NOT_HERE = new ArrayList<>();

    static void removeLater(ServerLevel level, BlockPos pos) {
        NOT_HERE.add(new Pending(level.dimension(), pos.immutable()));
    }




}
