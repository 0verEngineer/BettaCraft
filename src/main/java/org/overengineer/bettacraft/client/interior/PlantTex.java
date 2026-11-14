package org.overengineer.bettacraft.client.interior;

import net.minecraft.resources.ResourceLocation;
import org.overengineer.bettacraft.BettaCraft;

/**
 * Textures of the plant meshes ({@link PlantMeshRenderer}), grey and tinted per vertex (species, part, redness), from
 * {@code tools/assetgen/plant_meshes.py}. Inside each is drawn as a texture of its own (stems and ribbons repeat along
 * their length); outside (the miniature) they come from the block atlas, the leaves and polyps as their "mini" variant
 * (larger shape, flatter colour, bled into the transparent texels, so the mipmaps keep them).
 */
enum PlantTex {
    ROUND("leaf_round", true), OVATE("leaf_ovate", true), RIBBON("ribbon", true), STEM("stem", false),
    // M22.1
    LANCE("leaf_lance", true), WAVY("leaf_wavy", true), NEEDLE("leaf_needle", true), SWORD("leaf_sword", true),
    CRYPT("leaf_crypt", true), ELLIPTIC("leaf_elliptic", true), NARROW("leaf_narrow", true), LOTUS_LEAF("lotus_leaf", true),
    LOTUS_PAD("lotus_pad", true), MOSS("moss", true), CORAL_MAT("coral_mat", false), ZOA_POLYP("zoa_polyp", true),
    STAR_POLYP("star_polyp", true), MUSHROOM("mushroom_disc", true), FLESH("coral_flesh", false), ACRO("acro", false),
    TENTACLE("tentacle", false), TUFT("tuft", true), BUBBLE("bubble", true);

    static final PlantTex[] ALL = values();

    /** The texture drawn inside (its own render type). */
    final ResourceLocation texture;
    /** The sprite in the block atlas drawn outside. */
    final ResourceLocation sprite;

    PlantTex(String name, boolean mini) {
        this.texture = BettaCraft.id("textures/block/plant_mesh/" + name + ".png");
        this.sprite = BettaCraft.id("block/plant_mesh/" + name + (mini ? "_mini" : ""));
    }
}
