package org.overengineer.bettacraft.tank;

import org.overengineer.bettacraft.util.NbtCompat;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

/**
 * Entity attachment {@code bettacraft:tank_ref}: the tank an inhabitant belongs to (PLAN 4.2). Only set on
 * entities that live in a Bettacraft tank; the lid collision and the confinement of our bettas key off it.
 */
public record TankRef(BlockPos controller, UUID tankId) {
    public static final TankRef NONE = new TankRef(BlockPos.ZERO, new UUID(0, 0));

    public boolean isSet() {
        return !tankId.equals(NONE.tankId);
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.put("controller", NbtCompat.writeBlockPos(controller));
        NbtCompat.putUUID(t, "tank", tankId);
        return t;
    }

    public static TankRef load(CompoundTag t) {
        if (!NbtCompat.hasUUID(t, "tank")) return NONE;
        return new TankRef(NbtCompat.readBlockPos(t, "controller").orElse(BlockPos.ZERO), NbtCompat.getUUID(t, "tank"));
    }
}
