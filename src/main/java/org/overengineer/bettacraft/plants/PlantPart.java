package org.overengineer.bettacraft.plants;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/**
 * Block parts of an aquarium plant in the tank interior (M13). Every species block has all parts as states; the
 * models of a species only differ where the species uses the part.
 */
public enum PlantPart implements StringRepresentable {
    /** The planted cutting: smaller than a block, like a sapling. */
    CUTTING,
    /** The root block of a grown plant (crown, rhizome start, bulb, stem foot); breaking it takes the whole plant out. */
    ROOT,
    /** Thin stem or leaf stalk. */
    STEM,
    /** Leaf (a full leafy cube, cutout). */
    LEAF,
    /** Young leaves at the tips (red species show their red here first). */
    TOP,
    /** Creeping rhizome of the epiphytes (breaking a piece divides the plant). */
    RHIZOME,
    /** Long ribbon leaf (Vallisneria). */
    RIBBON,
    /** Floating leaf at the water surface (tiger lotus). */
    PAD,
    /** Low carpet (Monte Carlo). */
    MAT,
    /** Fuzzy moss. */
    MOSS;

    private final String name = name().toLowerCase(Locale.ROOT);

    @Override
    public String getSerializedName() {
        return name;
    }

    /** Parts that bettas swim around (the tiny cutting and the flat carpet are no obstacle). */
    public boolean obstacle() {
        return this != CUTTING && this != MAT;
    }

    /** Parts at the water surface that shade the tank. */
    public boolean surface() {
        return this == PAD;
    }
}
