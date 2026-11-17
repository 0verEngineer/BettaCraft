package org.overengineer.bettacraft.mobs;

import net.minecraft.nbt.CompoundTag;

/**
 * M31 part d: what a mob in glass keeps (the entity attachment {@code bettacraft:glass_mob}, saved with the mob, also in
 * a mob jar or another mod's capture item). Its presence is the mark that the mob lived shrunk in a tank: such a mob never
 * drops anything and gives no experience, in the tank and after it left it ({@link GlassMobs}). {@code growth} 1 = grown
 * (a young one bred in the tank grows over {@link GlassMobs#GROW_DAYS}); {@code fedAt}/{@code bredAt} are game times.
 */
public final class GlassMobData {
    public static final long NEVER = Long.MIN_VALUE;

    public double growth = 1.0;
    public long fedAt = NEVER;
    public long bredAt = NEVER;

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        if (growth < 1) t.putDouble("growth", growth);
        if (fedAt != NEVER) t.putLong("fed", fedAt);
        if (bredAt != NEVER) t.putLong("bred", bredAt);
        return t;
    }

    public static GlassMobData load(CompoundTag t) {
        GlassMobData d = new GlassMobData();
        d.growth = t.contains("growth") ? Math.max(0, Math.min(1, t.getDoubleOr("growth", 0d))) : 1.0;
        d.fedAt = t.contains("fed") ? t.getLongOr("fed", 0L) : NEVER;
        d.bredAt = t.contains("bred") ? t.getLongOr("bred", 0L) : NEVER;
        return d;
    }
}
