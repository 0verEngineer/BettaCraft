package org.overengineer.bettacraft.tank.interior;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.registry.ModAttachments;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModDimensions;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server logic of the tank interior dimension (PLAN 7): region allocation (lazy, on first entry), shell building,
 * entering/leaving, and keeping interiors consistent when tanks merge or split.
 */
public final class InteriorManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/interior");

    private InteriorManager() {}

    @Nullable
    public static ServerLevel interiorLevel(MinecraftServer server) {
        return server.getLevel(ModDimensions.TANK_INTERIOR);
    }

    public static boolean isInterior(net.minecraft.world.level.Level level) {
        return level.dimension() == ModDimensions.TANK_INTERIOR;
    }

    public static boolean isShellBlock(BlockState s) {
        return s.is(ModBlocks.INTERIOR_WALL.get()) || s.is(ModBlocks.INTERIOR_FLOOR.get());
    }

    // ------------------------------------------------------------------ enter / leave

    public static void enter(ServerPlayer player, BlockPos clicked) {
        if (isInterior(player.level())) {
            player.displayClientMessage(Component.translatable("message.bettacraft.interior.nested"), true);
            return;
        }
        TankBlockEntity controller = TankMultiblock.getController(player.level(), clicked);
        ServerLevel interior = interiorLevel(player.server);
        if (controller == null || interior == null) {
            player.displayClientMessage(Component.translatable(interior == null
                    ? "message.bettacraft.interior.missing_dimension" : "message.bettacraft.tank.not_formed"), true);
            return;
        }
        InteriorRegion region = ensureRegion((ServerLevel) player.level(), controller);
        player.setData(ModAttachments.INTERIOR_RETURN.get(), new InteriorReturn(player.level().dimension(), player.position(),
                player.getYRot(), player.getXRot(), region.id()));
        BlockPos target = region.toWorld(region.sizeX() / 2, 0, region.sizeZ() / 2);
        player.teleportTo(interior, target.getX() + 0.5, target.getY(), target.getZ() + 0.5, player.getYRot(), player.getXRot());
        player.displayClientMessage(Component.translatable("message.bettacraft.interior.entered",
                region.sizeX(), region.sizeY(), region.sizeZ()), true);
    }

    public static void leave(ServerPlayer player) {
        InteriorReturn ret = player.getData(ModAttachments.INTERIOR_RETURN.get());
        player.removeData(ModAttachments.INTERIOR_RETURN.get());
        ServerLevel target = ret.isSet() ? player.server.getLevel(ret.dimension()) : null;
        if (target == null || isInterior(target)) {
            // No valid return point: world spawn of the overworld.
            ServerLevel overworld = player.server.overworld();
            BlockPos spawn = overworld.getSharedSpawnPos();
            player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, player.getYRot(), player.getXRot());
            return;
        }
        Vec3 p = ret.position();
        player.teleportTo(target, p.x, p.y, p.z, ret.yRot(), ret.xRot());
    }

    // ------------------------------------------------------------------ regions

    /** Returns the tank's region, allocating and building it on first use. */
    public static InteriorRegion ensureRegion(ServerLevel tankLevel, TankBlockEntity controller) {
        TankData t = controller.getTank();
        InteriorRegions regions = InteriorRegions.get(tankLevel.getServer());
        InteriorRegion r = regions.get(t.regionId());
        if (r == null) {
            r = allocate(tankLevel, t);
            controller.markTankChanged();
        } else {
            r.retarget(tankLevel.dimension(), t.min(), t.id());
            regions.setDirty();
        }
        if (!r.built()) buildShell(tankLevel.getServer(), r);
        return r;
    }

    private static InteriorRegion allocate(ServerLevel tankLevel, TankData t) {
        int s = t.scale();
        InteriorRegion r = InteriorRegions.get(tankLevel.getServer()).allocate(t.sizeX() * s, t.sizeY() * s, t.sizeZ() * s, s,
                tankLevel.dimension(), t.min(), t.id(), t.owner());
        t.setRegionId(r.id());
        return r;
    }

    /** Places floor, invisible walls and ceiling of a region. */
    public static void buildShell(MinecraftServer server, InteriorRegion r) {
        ServerLevel level = interiorLevel(server);
        if (level == null) return;
        long start = System.nanoTime();
        LongSet touched = new LongOpenHashSet();
        int x0 = r.originX(), z0 = r.originZ();
        int x1 = x0 + r.sizeX() - 1, z1 = z0 + r.sizeZ() - 1;
        int fy = InteriorRegion.FLOOR_Y, top = InteriorRegion.BASE_Y + r.sizeY();
        BlockState wall = ModBlocks.INTERIOR_WALL.get().defaultBlockState();
        InteriorWorldOps.fill(level, x0, fy, z0, x1, fy, z1, ModBlocks.INTERIOR_FLOOR.get().defaultBlockState(), touched);
        InteriorWorldOps.fill(level, x0 - 1, fy, z0 - 1, x0 - 1, top, z1 + 1, wall, touched);
        InteriorWorldOps.fill(level, x1 + 1, fy, z0 - 1, x1 + 1, top, z1 + 1, wall, touched);
        InteriorWorldOps.fill(level, x0, fy, z0 - 1, x1, top, z0 - 1, wall, touched);
        InteriorWorldOps.fill(level, x0, fy, z1 + 1, x1, top, z1 + 1, wall, touched);
        InteriorWorldOps.fill(level, x0, top, z0, x1, top, z1, wall, touched);
        InteriorWorldOps.resendChunks(level, touched);
        r.setBuilt(true);
        InteriorRegions.get(server).setDirty();
        LOGGER.info("Built interior region {} ({}x{}x{}, scale {}) in {} ms, {} chunks", r.id(), r.sizeX(), r.sizeY(), r.sizeZ(),
                r.scale(), (System.nanoTime() - start) / 1_000_000, touched.size());
    }

    /** Removes everything of a region (shell included) and forgets it. */
    private static void destroyRegion(MinecraftServer server, InteriorRegion r) {
        ServerLevel level = interiorLevel(server);
        if (level != null && r.built()) {
            LongSet touched = new LongOpenHashSet();
            InteriorWorldOps.clear(level, r.originX() - 1, InteriorRegion.FLOOR_Y, r.originZ() - 1,
                    r.originX() + r.sizeX(), InteriorRegion.BASE_Y + r.sizeY(), r.originZ() + r.sizeZ(), touched);
            InteriorWorldOps.resendChunks(level, touched);
        }
        InteriorRegions.get(server).remove(r.id());
        InteriorSync.forget(r.id());
    }

    /** Benchmark helper: removes a region that belongs to no tank. */
    static void destroyRegionForBench(MinecraftServer server, InteriorRegion r) {
        destroyRegion(server, r);
    }

    // ------------------------------------------------------------------ structural changes

    public static void onTanksMerged(ServerLevel tankLevel, List<TankData> olds, TankData merged) {
        MinecraftServer server = tankLevel.getServer();
        InteriorRegions regions = InteriorRegions.get(server);
        List<TankData> withRegion = olds.stream().filter(t -> regions.get(t.regionId()) != null).toList();
        merged.setRegionId(-1);
        if (withRegion.isEmpty()) return;
        ServerLevel interior = interiorLevel(server);
        if (interior == null) return;

        InteriorRegion target = allocate(tankLevel, merged);
        buildShell(server, target);
        int s = merged.scale();
        LongSet touched = new LongOpenHashSet();
        for (TankData old : withRegion) {
            InteriorRegion src = regions.get(old.regionId());
            BlockPos off = old.min().subtract(merged.min());
            BlockPos dst = target.toWorld(off.getX() * s, off.getY() * s, off.getZ() * s);
            copyInterior(interior, src, 0, 0, 0, src.sizeX(), src.sizeY(), src.sizeZ(), dst, touched);
            movePlayers(interior, src, target, off.getX() * s, off.getY() * s, off.getZ() * s);
            destroyRegion(server, src);
        }
        InteriorWorldOps.resendChunks(interior, touched);
        InteriorSync.markDirty(target.id());
    }

    public static void onTankSplit(ServerLevel tankLevel, TankData old, List<TankData> pieces) {
        MinecraftServer server = tankLevel.getServer();
        InteriorRegions regions = InteriorRegions.get(server);
        InteriorRegion src = regions.get(old.regionId());
        for (TankData p : pieces) p.setRegionId(-1);
        if (src == null) return;
        ServerLevel interior = interiorLevel(server);
        if (interior == null) return;
        int s = old.scale();
        LongSet touched = new LongOpenHashSet();
        for (TankData piece : pieces) {
            InteriorRegion target = allocate(tankLevel, piece);
            buildShell(server, target);
            BlockPos off = piece.min().subtract(old.min());
            copyInterior(interior, src, off.getX() * s, off.getY() * s, off.getZ() * s,
                    piece.sizeX() * s, piece.sizeY() * s, piece.sizeZ() * s, target.toWorld(0, 0, 0), touched);
            movePlayers(interior, src, target, -off.getX() * s, -off.getY() * s, -off.getZ() * s);
            InteriorSync.markDirty(target.id());
        }
        InteriorWorldOps.resendChunks(interior, touched);
        ejectPlayers(interior, src);
        destroyRegion(server, src);
    }

    /** Drops the interior part belonging to one tank block as items at that block and clears it. */
    public static void dropInteriorOfBlock(ServerLevel tankLevel, TankData t, BlockPos pos) {
        MinecraftServer server = tankLevel.getServer();
        InteriorRegion r = InteriorRegions.get(server).get(t.regionId());
        ServerLevel interior = interiorLevel(server);
        if (r == null || interior == null || !r.built()) return;
        int s = t.scale();
        BlockPos off = pos.subtract(t.min());
        BlockPos a = r.toWorld(off.getX() * s, off.getY() * s, off.getZ() * s);
        BlockPos b = a.offset(s - 1, s - 1, s - 1);
        Object2IntLinkedOpenHashMap<Item> counts = new Object2IntLinkedOpenHashMap<>();
        InteriorWorldOps.forEachBlock(interior, a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ(), (x, y, z, st) -> {
            if (isShellBlock(st)) return;
            Item item = st.getBlock().asItem();
            if (item != Items.AIR) counts.addTo(item, 1);
        });
        LongSet touched = new LongOpenHashSet();
        InteriorWorldOps.clear(interior, a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ(), touched);
        InteriorWorldOps.resendChunks(interior, touched);
        for (var e : counts.object2IntEntrySet()) {
            int left = e.getIntValue();
            int max = e.getKey().getDefaultMaxStackSize();
            while (left > 0) {
                int n = Math.min(left, max);
                Block.popResource(tankLevel, pos, new ItemStack(e.getKey(), n));
                left -= n;
            }
        }
        InteriorSync.markDirty(r.id());
    }

    public static void freeRegion(ServerLevel tankLevel, TankData t) {
        MinecraftServer server = tankLevel.getServer();
        InteriorRegion r = InteriorRegions.get(server).get(t.regionId());
        if (r == null) return;
        ServerLevel interior = interiorLevel(server);
        if (interior != null) ejectPlayers(interior, r);
        destroyRegion(server, r);
        t.setRegionId(-1);
    }

    private static void copyInterior(ServerLevel level, InteriorRegion src, int lx, int ly, int lz, int sx, int sy, int sz,
                                     BlockPos dst, LongSet touched) {
        BlockPos a = src.toWorld(lx, ly, lz);
        List<InteriorWorldOps.Captured> blocks = InteriorWorldOps.capture(level, a.getX(), a.getY(), a.getZ(),
                a.getX() + sx - 1, a.getY() + sy - 1, a.getZ() + sz - 1);
        InteriorWorldOps.paste(level, dst, blocks, touched);
    }

    private static void movePlayers(ServerLevel level, InteriorRegion from, InteriorRegion to, int dx, int dy, int dz) {
        for (ServerPlayer p : new ArrayList<>(level.players())) {
            if (!from.containsShell(p.blockPosition())) continue;
            double lx = p.getX() - from.originX() + dx, ly = p.getY() - InteriorRegion.BASE_Y + dy, lz = p.getZ() - from.originZ() + dz;
            if (lx < 0 || lz < 0 || lx >= to.sizeX() || lz >= to.sizeZ() || ly < 0 || ly >= to.sizeY()) continue;
            p.teleportTo(level, to.originX() + lx, InteriorRegion.BASE_Y + ly, to.originZ() + lz, p.getYRot(), p.getXRot());
            InteriorReturn ret = p.getData(ModAttachments.INTERIOR_RETURN.get());
            if (ret.isSet()) {
                p.setData(ModAttachments.INTERIOR_RETURN.get(),
                        new InteriorReturn(ret.dimension(), ret.position(), ret.yRot(), ret.xRot(), to.id()));
            }
        }
    }

    private static void ejectPlayers(ServerLevel level, InteriorRegion r) {
        for (ServerPlayer p : new ArrayList<>(level.players())) {
            if (r.containsShell(p.blockPosition())) {
                leave(p);
                p.displayClientMessage(Component.translatable("message.bettacraft.interior.invalid"), true);
            }
        }
    }

    /** Region a player inside the interior dimension is standing in, or null. */
    @Nullable
    public static InteriorRegion regionAt(MinecraftServer server, BlockPos pos) {
        InteriorRegion r = InteriorRegions.get(server).get(InteriorRegion.idAt(pos.getX(), pos.getZ()));
        return r != null && r.containsShell(pos) ? r : null;
    }
}
