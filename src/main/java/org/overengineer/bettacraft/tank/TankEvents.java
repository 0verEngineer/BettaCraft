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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.overengineer.bettacraft.BettaCraft;

/** Server tick work of tanks: removes water sources left behind by removed tank blocks (end of the same tick). */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class TankEvents {
    private record Pending(ResourceKey<Level> dim, BlockPos pos) {}

    private static final List<Pending> DRY = new ArrayList<>();

    private TankEvents() {}

    static void dryLater(ServerLevel level, BlockPos pos) {
        DRY.add(new Pending(level.dimension(), pos.immutable()));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (DRY.isEmpty()) return;
        List<Pending> todo = new ArrayList<>(DRY);
        DRY.clear();
        for (Pending p : todo) {
            ServerLevel level = event.getServer().getLevel(p.dim());
            if (level != null && level.getBlockState(p.pos()).getBlock() instanceof LiquidBlock) {
                level.setBlock(p.pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }
}
