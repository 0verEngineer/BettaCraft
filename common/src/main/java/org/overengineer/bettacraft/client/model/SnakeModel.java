package org.overengineer.bettacraft.client.model;

import org.overengineer.bettacraft.client.render.MobState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.overengineer.bettacraft.terrarium.TerrariumAnimalEntity;

/**
 * M31 part c: the model of a snake ({@code corn_snake}: head and ten body segments from the neck to the tail tip). The body
 * lies in S-curves: a wave runs from the head to the tail while it slithers (lateral undulation, the curve travels back
 * along the body as fast as the snake moves) and lies nearly still while it rests. Each segment moves sideways on the
 * wave and turns along it, so the chain stays closed; the head follows the neck and points along the curve.
 */
public class SnakeModel extends EntityModel<MobState<TerrariumAnimalEntity>> {
    public static final int SEGMENTS = 10;
    /** Wave length in segments and the sideways reach of the curve (px). */
    private static final float WAVE = 6.5F, REACH = 2.6F;
    private final ModelPart head;
    private final ModelPart[] seg = new ModelPart[SEGMENTS];

    public SnakeModel(ModelPart root) {
        super(root);
        ModelPart animal = root.getChild("animal");
        this.head = animal.getChild("head");
        for (int i = 0; i < SEGMENTS; i++) seg[i] = animal.getChild("seg" + i);
    }

    /** Sideways offset of the body at segment position {@code s} (0 = neck). */
    private static float side(float s, float phase, float reach) {
        float grow = Math.min(1F, 0.35F + s / 3F); // the neck swings less than the body
        return Mth.sin(phase - s * Mth.TWO_PI / WAVE) * reach * grow;
    }

    @Override
    public void setupAnim(MobState<TerrariumAnimalEntity> state) {
        TerrariumAnimalEntity e = state.entity;
        float limbSwing = state.walkAnimationPos, limbSwingAmount = state.walkAnimationSpeed, ageInTicks = state.ageInTicks, netHeadYaw = state.yRot, headPitch = state.xRot;
        root().getAllParts().forEach(ModelPart::resetPose);
        float moving = Math.min(1F, limbSwingAmount * 5F);
        // moving: the wave travels with the distance covered; resting: a slow drift of the resting curves
        float phase = limbSwing * 1.3F + ageInTicks * 0.01F;
        float reach = REACH * (0.75F + 0.25F * moving);
        float segLen = 3.6F;
        for (int i = 0; i < SEGMENTS; i++) {
            float x = side(i, phase, reach);
            float slope = (side(i + 0.5F, phase, reach) - side(i - 0.5F, phase, reach)) / segLen;
            seg[i].x = x;
            seg[i].yRot = (float) Math.atan(slope);
        }
        float hx = side(-0.6F, phase, reach);
        float hs = (side(-0.1F, phase, reach) - side(-1.1F, phase, reach)) / segLen;
        head.x = hx;
        head.yRot = (float) Math.atan(hs) + Mth.clamp(netHeadYaw, -25F, 25F) * Mth.DEG_TO_RAD * 0.3F;
        head.xRot = -0.05F - Mth.sin(ageInTicks * 0.05F) * 0.04F;
    }
}
