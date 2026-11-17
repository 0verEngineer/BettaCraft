package org.overengineer.bettacraft.client.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * M52b: the render state of our animals (1.21.2 render states, drawn later through the 1.21.9 submit queue). The models
 * animate from the live entity like in 1.21.1 (fins, gills, wiggle read the genome and the eased swim pose), so the state
 * keeps the entity of this frame; extraction and drawing both run on the render thread in the same frame. The renderer
 * also puts the model, skin and alpha it chose for the animal here.
 */
public class MobState<E extends LivingEntity> extends LivingEntityRenderState {
    public E entity;
    public float partialTick;
    /** The model of this animal (a renderer with one model per body shape or species), or null for the renderer's own. */
    public @Nullable Object model;
    public @Nullable Identifier skin;
    public int alpha = 255;
    public int tint = -1;
}
