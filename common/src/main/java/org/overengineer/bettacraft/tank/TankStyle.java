package org.overengineer.bettacraft.tank;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * Look of a tank (M11), stored next to the glass in the block entity and in the {@code bettacraft:tank_style} item
 * component. Tanks only connect with tanks of the same glass <b>and</b> the same style.
 * <ul>
 *     <li>{@link #TEXTURED}: the texture of the glass it was made from, connected across the blocks of one tank (the
 *     glass frame only along the outer edges). Every tank made before M11 is textured.</li>
 *     <li>{@link #CLEAN}: perfectly clear (vanilla) glass without texture, only the thin edge lines.</li>
 * </ul>
 */
public enum TankStyle implements StringRepresentable {
    TEXTURED("textured"),
    CLEAN("clean");

    public static final Codec<TankStyle> CODEC = StringRepresentable.fromEnum(TankStyle::values);
    public static final StreamCodec<ByteBuf, TankStyle> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[Math.floorMod(i, values().length)],
            TankStyle::ordinal);

    private final String id;

    TankStyle(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    /** Unknown or missing ids are the textured default (the look of every tank before M11). */
    public static TankStyle byId(String id) {
        if (id == null) return TEXTURED;
        String s = id.toLowerCase(Locale.ROOT);
        for (TankStyle t : values()) if (t.id.equals(s)) return t;
        return TEXTURED;
    }

    /** Translation key of the style name ({@code gui.bettacraft.tank.style.<id>}). */
    public String key() {
        return "gui.bettacraft.tank.style." + id;
    }
}
