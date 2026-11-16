package org.overengineer.bettacraft.equipment;

import java.util.Locale;
import javax.annotation.Nullable;

/**
 * M44: the front chosen in the cabinet menu. {@link #AUTO} (the default) takes the front from the row the cabinet stands in
 * ({@link CabinetFront#layout}); the others fix the front of this one block.
 */
public enum CabinetFrontSetting {
    AUTO(null), PAIR_L(CabinetFront.PAIR_L), PAIR_R(CabinetFront.PAIR_R), SINGLE(CabinetFront.SINGLE), DRAWERS(CabinetFront.DRAWERS),
    TWO(CabinetFront.TWO);

    @Nullable
    private final CabinetFront fixed;

    CabinetFrontSetting(@Nullable CabinetFront fixed) {
        this.fixed = fixed;
    }

    /** The fixed front, null for {@link #AUTO}. */
    @Nullable
    public CabinetFront fixed() {
        return fixed;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "gui.bettacraft.cabinet.front." + id();
    }

    public CabinetFrontSetting next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public CabinetFrontSetting previous() {
        return values()[(ordinal() + values().length - 1) % values().length];
    }

    /** By id, AUTO for null and unknown ids (cabinets of older versions). */
    public static CabinetFrontSetting byId(@Nullable String id) {
        if (id != null) for (CabinetFrontSetting s : values()) if (s.id().equals(id)) return s;
        return AUTO;
    }

    /** By ordinal, AUTO when out of range. */
    public static CabinetFrontSetting byIndex(int i) {
        return i >= 0 && i < values().length ? values()[i] : AUTO;
    }
}
