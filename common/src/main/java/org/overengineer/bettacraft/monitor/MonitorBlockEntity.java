package org.overengineer.bettacraft.monitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.network.MonitorDataPayload;
import org.overengineer.bettacraft.plumbing.DeviceEnergy;
import org.overengineer.bettacraft.registry.ModBlockEntities;

/**
 * M27: a monitor block. Every block knows the top left block of its screen ({@link #origin}, as the viewer sees it) and
 * the screen's size; that block is the controller and holds the screen's state: the dashboard layout, the FE buffer, the
 * tanks it knows and the status. The controller works every 20 ticks ({@link MonitorSync#cycle}).
 * <p>
 * Saved: origin and size in every block; layout, energy and tanks in the controller. Synced with the block entity (chunk
 * data and updates): origin, size, status and layout. The live values come bundled through {@link MonitorDataPayload}
 * and are kept on the client in {@link #clientTanks} / {@link #clientHistory}.
 */
public class MonitorBlockEntity extends BlockEntity {
    public static final int ON = 0, NO_POWER = 1, OFF = 2;
    public static final int ENERGY_CAPACITY = 50000;

    @Nullable
    private BlockPos origin;
    private int width = 1, height = 1;
    /** False until {@link MonitorScreens} put the block into a screen (a block placed just now). */
    private boolean formed;
    // controller state
    private DashboardLayout layout = new DashboardLayout();
    private final DeviceEnergy energy = new DeviceEnergy(ENERGY_CAPACITY, this::setChanged);
    private final List<MonitorTank> tanks = new ArrayList<>();
    private int status = ON;
    /** Part b: triggered alarm tiles with redstone (not saved, checked every cycle); > 0 = redstone signal. */
    private int alarms;
    // server only, not saved
    int cycles;
    int lastTankHash, lastHistoryHash;
    final Set<UUID> sentTo = new HashSet<>(), historySentTo = new HashSet<>();
    /** M32 part d: the alarm tiles that are on now ({@link MonitorAlarms}, not saved: after a restart they go off again). */
    final java.util.Map<Integer, MonitorAlarms.Watch> activeAlarms = new java.util.HashMap<>();
    // client only
    public List<MonitorTank> clientTanks = List.of();
    public final Map<String, MonitorDataPayload.HistoryLine> clientHistory = new HashMap<>();
    public long clientDataTime = Long.MIN_VALUE;

    public MonitorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MONITOR.get(), pos, state);
    }

    // ------------------------------------------------------------------ the screen

    public BlockPos origin() {
        return origin == null ? worldPosition : origin;
    }

    public boolean isController() {
        return origin == null || origin.equals(worldPosition);
    }

    /** Not yet in a screen (placed just now): holds no state worth keeping. */
    public boolean isFresh() {
        return !formed;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int area() {
        return width * height;
    }

    public Direction facing() {
        BlockState s = getBlockState();
        return s.hasProperty(MonitorBlock.FACING) ? s.getValue(MonitorBlock.FACING) : Direction.NORTH;
    }

    /** The controller of this block's screen (this one, or null if it is not loaded or not a monitor). */
    @Nullable
    public MonitorBlockEntity controller() {
        if (isController()) return this;
        if (level == null || !level.isLoaded(origin)) return null;
        return level.getBlockEntity(origin) instanceof MonitorBlockEntity c && c.isController() ? c : null;
    }

    /** The blocks of this screen (from the controller's origin and size). */
    public List<BlockPos> members() {
        List<BlockPos> out = new ArrayList<>(area());
        Direction right = MonitorScreens.right(facing());
        for (int r = 0; r < height; r++) for (int c = 0; c < width; c++) out.add(origin().relative(right, c).below(r));
        return out;
    }

    /** The centre of the screen's blocks. */
    public Vec3 screenCenter() {
        Direction right = MonitorScreens.right(facing());
        Vec3 o = Vec3.atCenterOf(origin());
        return o.add(right.getStepX() * (width - 1) / 2.0, -(height - 1) / 2.0, right.getStepZ() * (width - 1) / 2.0);
    }

    public AABB screenBox() {
        Direction right = MonitorScreens.right(facing());
        BlockPos a = origin(), b = origin().relative(right, width - 1).below(height - 1);
        return new AABB(Math.min(a.getX(), b.getX()), b.getY(), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, a.getY() + 1, Math.max(a.getZ(), b.getZ()) + 1);
    }

    /** From {@link MonitorScreens}: this block belongs to the screen at {@code origin} of {@code w} x {@code h}. */
    void setScreen(BlockPos origin, int w, int h) {
        this.origin = origin.equals(worldPosition) ? null : origin.immutable();
        this.width = w;
        this.height = h;
        this.formed = true;
        if (!isController()) {
            layout = new DashboardLayout();
            tanks.clear();
            energy.set(0);
            status = ON;
        }
        sentTo.clear();
        historySentTo.clear();
        setChanged();
    }

    /** From {@link MonitorScreens}: the controller takes over the state of an old screen. */
    void inherit(DashboardLayout layout, List<MonitorTank> tanks, int energy, int status) {
        this.layout = layout;
        this.layout.clip(width * DashboardLayout.CELLS, height * DashboardLayout.CELLS);
        this.tanks.clear();
        for (MonitorTank t : tanks) this.tanks.add(t.copy());
        this.energy.set(energy);
        this.status = status;
        this.cycles = 0;
        setChanged();
    }

    // ------------------------------------------------------------------ controller state

    public DashboardLayout layout() {
        return layout;
    }

    public List<MonitorTank> tanks() {
        return tanks;
    }

    public DeviceEnergy energy() {
        return energy;
    }

    /** The FE buffer of the screen (the controller's) for the energy capability of any of its blocks. */
    public DeviceEnergy screenEnergy() {
        MonitorBlockEntity c = controller();
        return c == null ? energy : c.energy;
    }

    public int status() {
        return status;
    }

    void setStatus(int status) {
        if (this.status == status) return;
        this.status = status;
        syncToClients();
    }

    /** The triggered alarm tiles with redstone of this block's screen (the controller's). */
    public int alarms() {
        MonitorBlockEntity c = controller();
        return c == null ? 0 : c.alarms;
    }

    /** The tank with this id this screen knows (server). */
    @Nullable
    public MonitorTank tank(UUID id) {
        for (MonitorTank t : tanks) if (t.id.equals(id)) return t;
        return null;
    }

    /** From {@link MonitorSync#cycle}: the redstone of every block of the screen changes with the count of alarms. */
    void setAlarms(int n) {
        if (alarms == n || level == null) return;
        boolean was = alarms > 0;
        alarms = n;
        Block block = getBlockState().getBlock();
        for (BlockPos p : members()) {
            if (!level.isLoaded(p)) continue;
            if (was != n > 0) level.updateNeighborsAt(p, block);
            level.updateNeighbourForOutputSignal(p, block);
        }
    }

    public void setLayout(DashboardLayout layout) {
        this.layout = layout;
        syncToClients();
    }

    /**
     * A layout from the editor (part b, {@link org.overengineer.bettacraft.network.MonitorLayoutPayload}): checked (within
     * 8 blocks, may build), clipped to the screen, saved and synced. The editor switches {@link DashboardLayout#auto} off.
     */
    public static boolean applyLayout(ServerPlayer player, BlockPos pos, CompoundTag tag) {
        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof MonitorBlockEntity be)) return false;
        MonitorBlockEntity c = be.controller();
        if (c == null || player.distanceToSqr(Vec3.atCenterOf(pos)) > 64 || !player.mayBuild() || !level.mayInteract(player, pos)) return false;
        DashboardLayout l = DashboardLayout.load(tag);
        l.sanitize(c.width * DashboardLayout.CELLS, c.height * DashboardLayout.CELLS);
        c.setLayout(l);
        return true;
    }

    void syncToClients() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MonitorBlockEntity be) {
        if (level instanceof ServerLevel server && be.isController() && level.getGameTime() % MonitorSync.INTERVAL == 0) MonitorSync.cycle(server, be);
    }

    // ------------------------------------------------------------------ client data

    /** The live data of a tank on the client (null if the monitor does not know it). */
    @Nullable
    public MonitorTank clientTank(@Nullable UUID id) {
        if (id == null) return null;
        for (MonitorTank t : clientTanks) if (t.id.equals(id)) return t;
        return null;
    }

    @Nullable
    public MonitorDataPayload.HistoryLine clientHistory(@Nullable UUID tank, MonitorValue v) {
        return tank == null ? null : clientHistory.get(tank + "/" + v.id);
    }

    /** From the payload on the client. */
    public void acceptClient(MonitorDataPayload.Entry e, long time) {
        if (e.tanks() != null) clientTanks = List.copyOf(e.tanks());
        if (e.history() != null) {
            clientHistory.clear();
            for (MonitorDataPayload.HistoryLine l : e.history()) clientHistory.put(l.tank() + "/" + l.value().id, l);
        }
        clientDataTime = time;
    }

    // ------------------------------------------------------------------ saving and sync

    private void saveScreen(CompoundTag tag) {
        tag.putInt("version", 1);
        if (origin != null) tag.putLong("origin", origin.asLong());
        tag.putInt("w", width);
        tag.putInt("h", height);
        tag.putBoolean("formed", formed);
        if (isController()) {
            tag.putInt("status", status);
            tag.put("layout", layout.save());
        }
    }

    private void loadScreen(CompoundTag tag) {
        origin = tag.contains("origin") ? BlockPos.of(tag.getLong("origin")) : null;
        if (origin != null && origin.equals(worldPosition)) origin = null;
        width = Math.max(1, Math.min(MonitorScreens.MAX_W, tag.getInt("w")));
        height = Math.max(1, Math.min(MonitorScreens.MAX_H, tag.getInt("h")));
        formed = tag.getBoolean("formed");
        status = tag.getInt("status");
        layout = DashboardLayout.load(tag.getCompound("layout"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveScreen(tag);
        if (isController()) {
            energy.save(tag);
            ListTag list = new ListTag();
            for (MonitorTank t : tanks) list.add(t.save());
            tag.put("tanks", list);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadScreen(tag);
        energy.load(tag);
        tanks.clear();
        ListTag list = tag.getList("tanks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && tanks.size() < MonitorSync.MAX_TANKS; i++) {
            CompoundTag c = list.getCompound(i);
            if (c.hasUUID("id")) tanks.add(MonitorTank.load(c));
        }
    }

    @Override
    public void setRemoved() {
        // M32 part d: broken or unloaded: its alarms clear (event, waypoint)
        if (level instanceof net.minecraft.server.level.ServerLevel sl && !activeAlarms.isEmpty()) MonitorAlarms.clearAll(sl, this);
        super.setRemoved();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveScreen(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        handleUpdateTag(pkt.getTag(), registries);
    }

    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadScreen(tag);
    }
}
