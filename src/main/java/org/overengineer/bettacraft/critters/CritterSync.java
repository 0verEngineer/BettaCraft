package org.overengineer.bettacraft.critters;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compact client format of the small animals of a tank (M14): per animal 20 bytes (id, species, flags, surface, colour,
 * grade, size, look (M21.5), position relative to the tank in 1/512 block, heading), per clutch or moult 10 bytes. Sent with the tank's
 * client view and every care step while the tank has animals; clients glide them to the new spot.
 */
public final class CritterSync {
    public static final int F_FEMALE = 1, F_BERRIED = 2, F_DEAD = 4, F_BURIED = 8, F_RETRACT = 16, F_SOFT = 32, F_ADULT = 64;
    private static final double UNIT = 512.0;

    private CritterSync() {}

    public record Decoded(List<Critter> critters, List<CritterObject> objects) {}

    public static byte[] encode(List<Critter> critters, List<CritterObject> objects, int minX, int minY, int minZ, long now) {
        int n = Math.min(critters.size(), 0xFFFF), m = Math.min(objects.size(), 0xFFFF);
        ByteBuffer b = ByteBuffer.allocate(4 + n * 20 + m * 10);
        b.putShort((short) n);
        for (int i = 0; i < n; i++) {
            Critter c = critters.get(i);
            b.putInt(c.id);
            b.put((byte) c.species.ordinal());
            int f = (c.female ? F_FEMALE : 0) | (c.berried() ? F_BERRIED : 0) | (c.alive() ? 0 : F_DEAD) | (c.buried ? F_BURIED : 0)
                    | (now < c.retractUntil ? F_RETRACT : 0) | (c.soft() ? F_SOFT : 0) | (c.adult() ? F_ADULT : 0);
            b.put((byte) f);
            b.put(c.face);
            int color = c.genes.color(c.species);
            b.put((byte) (color == CritterGenes.CHOCOLATE ? 120 : color));
            b.put((byte) Math.round(c.genes.grade() * 255));
            b.put((byte) Math.round(c.size() * 255));
            b.putShort((short) c.look);
            b.putShort(rel(c.x, minX));
            b.putShort(rel(c.y, minY));
            b.putShort(rel(c.z, minZ));
            b.put((byte) Math.round(c.yaw / 360F * 256F));
        }
        b.putShort((short) m);
        for (int i = 0; i < m; i++) {
            CritterObject o = objects.get(i);
            b.put((byte) (o.kind.ordinal() | (o.species.ordinal() << 2)));
            b.put(o.face);
            b.putShort(rel(o.x, minX));
            b.putShort(rel(o.y, minY));
            b.putShort(rel(o.z, minZ));
            b.put((byte) Math.round(o.yaw / 360F * 256F));
            b.put((byte) Math.round(Math.min(1, o.size) * 255));
        }
        return b.array();
    }

    private static short rel(double v, int min) {
        return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round((v - min) * UNIT)));
    }

    public static Decoded decode(byte[] data, int minX, int minY, int minZ) {
        List<Critter> out = new ArrayList<>();
        List<CritterObject> objs = new ArrayList<>();
        ByteBuffer b = ByteBuffer.wrap(data);
        try {
            int n = b.getShort() & 0xFFFF;
            CritterSpecies[] all = CritterSpecies.values();
            for (int i = 0; i < n; i++) {
                int id = b.getInt();
                int sp = b.get() & 0xFF;
                int f = b.get() & 0xFF;
                byte face = b.get();
                int color = b.get();
                float grade = (b.get() & 0xFF) / 255F;
                float size = (b.get() & 0xFF) / 255F;
                int look = b.getShort() & 0xFFFF;
                double x = minX + b.getShort() / UNIT, y = minY + b.getShort() / UNIT, z = minZ + b.getShort() / UNIT;
                float yaw = (b.get() & 0xFF) / 256F * 360F;
                if (sp >= all.length) continue;
                Critter c = new Critter(id, all[sp], (f & F_FEMALE) != 0, CritterGenes.WILD, x, y, z);
                c.yaw = c.pyaw = yaw;
                c.face = face;
                c.clientColor = color == 120 ? CritterGenes.CHOCOLATE : color;
                c.clientGrade = grade;
                c.clientSize = size;
                c.look = look;
                c.clientFlags = (byte) f;
                c.buried = (f & F_BURIED) != 0;
                c.dead = (f & F_DEAD) != 0 ? 0 : -1;
                out.add(c);
            }
            int m = b.getShort() & 0xFFFF;
            for (int i = 0; i < m; i++) {
                int k = b.get() & 0xFF;
                byte face = b.get();
                double x = minX + b.getShort() / UNIT, y = minY + b.getShort() / UNIT, z = minZ + b.getShort() / UNIT;
                float yaw = (b.get() & 0xFF) / 256F * 360F;
                float size = (b.get() & 0xFF) / 255F;
                CritterObject.Kind kind = (k & 3) == 0 ? CritterObject.Kind.CLUTCH : CritterObject.Kind.MOLT;
                CritterObject o = new CritterObject(i, kind, x, y, z);
                o.face = face;
                o.yaw = yaw;
                o.size = size;
                int sp = k >> 2;
                if (sp < all.length) o.species = all[sp];
                objs.add(o);
            }
        } catch (java.nio.BufferUnderflowException e) {
            // truncated: keep what was read
        }
        return new Decoded(out, objs);
    }

    /** Client: the new animals start where the old ones were drawn at {@code now} and move over {@code duration} ticks. */
    public static void adopt(List<Critter> old, List<Critter> fresh, long now, double duration) {
        Map<Integer, Critter> byId = new HashMap<>();
        for (Critter c : old) byId.put(c.id, c);
        for (Critter c : fresh) {
            Critter o = byId.get(c.id);
            if (o == null) {
                c.px = c.x;
                c.py = c.y;
                c.pz = c.z;
                c.pyaw = c.yaw;
                c.lerpStart = Long.MIN_VALUE;
                continue;
            }
            c.px = o.renderX(now, duration);
            c.py = o.renderY(now, duration);
            c.pz = o.renderZ(now, duration);
            c.pyaw = o.renderYaw(now, duration);
            // a jump onto/off the glass or a big step: no gliding through the glass corner
            double d = Math.abs(c.x - c.px) + Math.abs(c.y - c.py) + Math.abs(c.z - c.pz);
            c.moving = d > 0.002;
            c.lerpStart = d > 2 ? Long.MIN_VALUE : now;
            if (c.lerpStart == Long.MIN_VALUE) {
                c.px = c.x;
                c.py = c.y;
                c.pz = c.z;
            }
        }
    }
}
