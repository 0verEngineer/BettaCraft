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

/**
 * Server tick work of tanks: removes water sources left behind by removed tank blocks (end of the same tick); sneaking
 * clicks with filled containers.
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class TankEvents {
    private record Pending(ResourceKey<Level> dim, BlockPos pos) {}

    private static final List<Pending> DRY = new ArrayList<>();
    /** Tank inhabitants a player interacted with this tick (bucket, fish bag, Better Fish Tanks bag, Fintastic net). */
    private static final java.util.Map<java.util.UUID, String> CATCHING = new java.util.HashMap<>();

    private TankEvents() {}

    static void dryLater(ServerLevel level, BlockPos pos) {
        DRY.add(new Pending(level.dimension(), pos.immutable()));
    }

    /** M16: tank blocks set into the interior dimension without the item; broken (and dropped) at the end of the tick. */
    private static final List<Pending> NOT_HERE = new ArrayList<>();

    static void removeLater(ServerLevel level, BlockPos pos) {
        NOT_HERE.add(new Pending(level.dimension(), pos.immutable()));
    }

    /**
     * Shift + right-click on a tank with a filled container (betta bucket, fish bag, any mob bucket, Better Fish Tanks
     * bag, Fintastic net) inserts the animal like a normal click. Without this, sneaking skips the block and the item
     * itself would release the animal into the world in front of the tank (M7 issue).
     */
    @SubscribeEvent
    public static void onRightClickBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().isSecondaryUseActive()) return;
        if (!(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof TankBlock)) return;
        if (TankInhabitants.containedType(event.getItemStack()) == null) return;
        event.setUseBlock(net.neoforged.neoforge.common.util.TriState.TRUE);
        event.setUseItem(net.neoforged.neoforge.common.util.TriState.FALSE);
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST, receiveCanceled = true)
    public static void onEntityInteract(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        if (!event.getLevel().isClientSide && TankInhabitants.hasTankRef(event.getTarget())) {
            CATCHING.put(event.getTarget().getUUID(), event.getEntity().getScoreboardName());
        }
    }

    /**
     * An inhabitant caught in the world (bucket or bag of any mod, after an interaction this tick) leaves the tank at
     * once: log entry and inhabitant list are updated right away instead of at the next reconciliation (M7 issue).
     * Menu take-outs release the tank reference first and are logged there.
     */
    @SubscribeEvent
    public static void onEntityLeave(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent event) {
        if (CATCHING.isEmpty() || !(event.getLevel() instanceof ServerLevel level)) return;
        var e = event.getEntity();
        if (e.getRemovalReason() != net.minecraft.world.entity.Entity.RemovalReason.DISCARDED || CATCHING.remove(e.getUUID()) == null) return;
        if (!TankInhabitants.hasTankRef(e)) return;
        TankRef ref = e.getData(org.overengineer.bettacraft.registry.ModAttachments.TANK_REF.get());
        if (!level.isLoaded(ref.controller())) return;
        TankBlockEntity c = TankMultiblock.getBE(level, ref.controller());
        if (c == null || !c.isController() || !c.getTank().id().equals(ref.tankId())) return;
        TankCare.log(level, c, "log.bettacraft.taken_out", TankCare.nameArg(e));
        c.getTank().inhabitants().remove(e.getUUID());
        c.markTankChanged();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        CATCHING.clear();
        if (!NOT_HERE.isEmpty()) {
            List<Pending> todo = new ArrayList<>(NOT_HERE);
            NOT_HERE.clear();
            for (Pending p : todo) {
                ServerLevel level = event.getServer().getLevel(p.dim());
                if (level != null && level.getBlockState(p.pos()).getBlock() instanceof TankBlock) level.destroyBlock(p.pos(), true);
            }
        }
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
