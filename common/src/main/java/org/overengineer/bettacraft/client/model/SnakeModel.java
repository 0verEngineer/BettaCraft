package org.overengineer.bettacraft.client.model;

import javax.annotation.Nullable;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.overengineer.bettacraft.client.render.MobState;
import org.overengineer.bettacraft.terrarium.Habits;
import org.overengineer.bettacraft.terrarium.TerrariumAnimalEntity;

/**
 * M31 part c: the model of a snake ({@code corn_snake}: head and ten body segments from the neck to the tail tip). The body
 * lies in S-curves: a wave runs from the head to the tail while it slithers (lateral undulation, the curve travels back
 * along the body as fast as the snake moves) and lies nearly still while it rests. Each segment moves sideways on the
 * wave and turns along it, so the chain stays closed; the head follows the neck and points along the curve.
 * <p>
 * M54: the {@link Habits.Action}s of a snake, eased in and out: {@link Habits.Action#HEAD_UP} raises the head and the neck
 * (the first two segments tilt up) and flicks the tongue, {@link Habits.Action#TONGUE} flicks the tongue,
 * {@link Habits.Action#COIL} lays the chain in a flat spiral with the head on the outside, {@link Habits.Action#LOOK} turns
 * the head from side to side. A crawling snake flicks its tongue now and then by itself.
 */
public class SnakeModel extends EntityModel<MobState<TerrariumAnimalEntity>> {
    public static final int SEGMENTS = 10;
    /** Wave length in segments and the sideways reach of the curve (px). */
    private static final float WAVE = 6.5F, REACH = 2.6F;
    private final ModelPart head;
    @Nullable
    private final ModelPart tongue;
    private final ModelPart[] seg = new ModelPart[SEGMENTS];
    /** M54: the coiled pose: x, z and yaw of the head (index 0) and of every segment. */
    private final float[] coilX = new float[SEGMENTS + 1], coilZ = new float[SEGMENTS + 1], coilYaw = new float[SEGMENTS + 1];

    public SnakeModel(ModelPart root) {
        super(root);
        ModelPart animal = root.getChild("animal");
        this.head = animal.getChild("head");
        this.tongue = head.hasChild("tongue") ? head.getChild("tongue") : null;
        for (int i = 0; i < SEGMENTS; i++) seg[i] = animal.getChild("seg" + i);
        coil();
    }

    /**
     * M54: the coil: the chain along a flat spiral around a point beside the middle of the body, from the head (outside,
     * radius 6.6 px) to the tail tip (inside, 1.9 px), each part as far along it as along the straight body.
     */
    private void coil() {
        float[] s = new float[SEGMENTS + 1];
        for (int i = 0; i < SEGMENTS; i++) s[i + 1] = Math.abs(seg[i].z - head.z);
        float len = s[SEGMENTS], r0 = 6.6F, r1 = 1.9F, cx = 0F, cz = 1.5F;
        // integrate the angle along the spiral (d angle = d s / r) in small steps
        float angle = (float) Math.PI, at = 0, ds = len / 4000F;
        int k = 0;
        for (int step = 0; step <= 4100 && k <= SEGMENTS; step++) {
            float r = r0 + (r1 - r0) * Math.min(1F, at / len);
            while (k <= SEGMENTS && at >= s[k] - 1e-4F) {
                coilX[k] = cx + r * Mth.cos(angle);
                coilZ[k] = cz + r * Mth.sin(angle);
                // the direction towards the tail (the parts' +z) is the tangent of the growing angle
                coilYaw[k] = (float) Math.atan2(-Mth.sin(angle), Mth.cos(angle));
                k++;
            }
            angle += ds / r;
            at += ds;
        }
    }

    /** Sideways offset of the body at segment position {@code s} (0 = neck). */
    private static float side(float s, float phase, float reach) {
        float grow = Math.min(1F, 0.35F + s / 3F); // the neck swings less than the body
        return Mth.sin(phase - s * Mth.TWO_PI / WAVE) * reach * grow;
    }

    @Override
    public void setupAnim(MobState<TerrariumAnimalEntity> state) {
        TerrariumAnimalEntity e = state.entity;
        float limbSwing = state.walkAnimationPos, limbSwingAmount = state.walkAnimationSpeed, ageInTicks = state.ageInTicks, netHeadYaw = state.yRot;
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
        // M54: the actions
        float coil = e.actionWeight(Habits.Action.COIL, ageInTicks);
        float up = e.actionWeight(Habits.Action.HEAD_UP, ageInTicks);
        float flick = e.actionWeight(Habits.Action.TONGUE, ageInTicks);
        float look = e.actionWeight(Habits.Action.LOOK, ageInTicks);
        if (coil > 0) {
            for (int i = 0; i < SEGMENTS; i++) {
                seg[i].x = Mth.lerp(coil, seg[i].x, coilX[i + 1]);
                seg[i].z = Mth.lerp(coil, seg[i].z, coilZ[i + 1]);
                seg[i].yRot = Mth.rotLerp(coil, seg[i].yRot * Mth.RAD_TO_DEG, coilYaw[i + 1] * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
            }
            head.x = Mth.lerp(coil, head.x, coilX[0]);
            head.z = Mth.lerp(coil, head.z, coilZ[0]);
            head.yRot = Mth.rotLerp(coil, head.yRot * Mth.RAD_TO_DEG, coilYaw[0] * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
            head.y -= 0.6F * coil; // the head rests on the coil, a little raised
        }
        if (up > 0) {
            // the front of the body rises in a curve, the head held level and pulled back a little over the neck
            head.y -= 4.6F * up;
            head.z += 1.4F * up;
            head.xRot += 0.1F * up;
            seg[0].y -= 2.7F * up;
            seg[0].z += 0.5F * up;
            seg[0].xRot -= 0.75F * up;
            seg[1].y -= 0.9F * up;
            seg[1].xRot -= 0.35F * up;
            head.yRot += Mth.sin(ageInTicks * 0.09F) * 0.35F * up; // it looks around up there
        }
        if (look > 0) head.yRot += Mth.sin(ageInTicks * 0.13F) * 0.55F * look;
        // the tongue: out in short flicks while it raises its head, flicks its tongue or crawls (now and then)
        if (tongue != null) {
            float out = 0;
            if (up > 0.5F) out = Math.max(out, flick(ageInTicks, 17F, 6F));
            if (flick > 0.3F) out = Math.max(out, flick(ageInTicks, 9F, 5F));
            if (moving > 0.3F && coil < 0.1F) out = Math.max(out, flick(ageInTicks + e.getId() * 7, 46F, 6F));
            tongue.visible = out > 0.05F;
            tongue.zScale = Math.max(0.05F, out);
            tongue.z += (1F - tongue.zScale) * 1.0F; // scaled about its middle: its root stays at the mouth (the tongue is 2 px long)
        }
    }

    /** A tongue flick every {@code period} ticks for {@code length} ticks: 0 in, 1 fully out (out and back in a sine). */
    private static float flick(float t, float period, float length) {
        float u = t % period;
        if (u > length) return 0;
        return Mth.sin(u / length * Mth.PI);
    }
}
