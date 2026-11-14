package org.overengineer.bettacraft.monitor;

import java.util.Arrays;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * M27: a tank a monitor knows: its stable id ({@code TankData#id}, kept when tanks merge or split), the name of its tank
 * sign, where its controller was, whether it is loaded and in range now ({@link #live}) and the last values it had
 * ({@link MonitorValue} by ordinal). A tank that is unloaded or out of range keeps its last values and shows "not loaded".
 */
public final class MonitorTank {
    public final UUID id;
    public String name;
    public BlockPos pos;
    public boolean live;
    public float[] values;
    /** Game time of the last values. */
    public long seen;
    /** Part b: warnings, animals, devices, log ({@link MonitorDetail}); empty until a tile needs it. */
    public CompoundTag detail = new CompoundTag();

    public MonitorTank(UUID id, String name, BlockPos pos, boolean live, float[] values, long seen) {
        this.id = id;
        this.name = name;
        this.pos = pos.immutable();
        this.live = live;
        this.values = values;
        this.seen = seen;
    }

    public float value(MonitorValue v) {
        return v.ordinal() < values.length ? values[v.ordinal()] : Float.NaN;
    }

    public MonitorTank copy() {
        MonitorTank t = new MonitorTank(id, name, pos, live, values.clone(), seen);
        t.detail = detail;
        return t;
    }

    public int contentHash() {
        int h = id.hashCode();
        h = h * 31 + name.hashCode();
        h = h * 31 + (live ? 1 : 0);
        h = h * 31 + Arrays.hashCode(values);
        h = h * 31 + detail.hashCode();
        return h;
    }

    public CompoundTag save() {
        CompoundTag c = new CompoundTag();
        c.putUUID("id", id);
        c.putString("name", name);
        c.putLong("pos", pos.asLong());
        c.putLong("seen", seen);
        CompoundTag v = new CompoundTag();
        for (MonitorValue m : MonitorValue.ALL) if (Float.isFinite(value(m))) v.putFloat(m.id, value(m));
        c.put("values", v);
        if (!detail.isEmpty()) c.put("detail", detail.copy());
        return c;
    }

    /** Loaded tanks are not live until the monitor finds them again. */
    public static MonitorTank load(CompoundTag c) {
        float[] values = new float[MonitorValue.ALL.length];
        Arrays.fill(values, Float.NaN);
        CompoundTag v = c.getCompound("values");
        for (MonitorValue m : MonitorValue.ALL) if (v.contains(m.id)) values[m.ordinal()] = v.getFloat(m.id);
        MonitorTank t = new MonitorTank(c.getUUID("id"), c.getString("name"), BlockPos.of(c.getLong("pos")), false, values, c.getLong("seen"));
        t.detail = c.getCompound("detail").copy();
        return t;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeUtf(name, 64);
        buf.writeBoolean(live);
        buf.writeLong(seen);
        buf.writeVarInt(values.length);
        for (float f : values) buf.writeFloat(f);
        buf.writeNbt(detail);
    }

    public static MonitorTank read(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        String name = buf.readUtf(64);
        boolean live = buf.readBoolean();
        long seen = buf.readLong();
        int n = Math.min(buf.readVarInt(), 256);
        float[] values = new float[Math.max(n, MonitorValue.ALL.length)];
        Arrays.fill(values, Float.NaN);
        for (int i = 0; i < n; i++) values[i] = buf.readFloat();
        MonitorTank t = new MonitorTank(id, name, BlockPos.ZERO, live, values, seen);
        CompoundTag d = buf.readNbt();
        t.detail = d == null ? new CompoundTag() : d;
        return t;
    }
}
