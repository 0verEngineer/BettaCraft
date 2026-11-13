package org.overengineer.bettacraft.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * M21.5: the smoothed swimming pose of a fish on the client (the renderer used to take the raw movement of the last
 * tick: the pitch jumped in and out above a speed threshold, the tail beat jumped whenever the speed changed because its
 * frequency multiplied the whole age, and the body followed the synced yaw in 1.4° steps). Updated once per client tick,
 * interpolated with the partial tick when drawing:
 * <ul>
 *   <li>{@code swim} 0..1 - how fast the fish swims, eased;</li>
 *   <li>{@code phase} - the tail beat / body wave, advanced by a speed-dependent frequency (no jumps);</li>
 *   <li>{@code pitch} - nose up/down in degrees, eased towards the direction of movement, fading out smoothly at low speed;</li>
 *   <li>{@code glass} 0..1 - how far a pleco or otocinclus is turned upright onto the glass;</li>
 *   <li>{@code yaw} - the body yaw eased towards the entity's (synced) yaw.</li>
 * </ul>
 */
public final class SwimPose {
    private float swimO, swim, phaseO, phase, pitchO, pitch, glassO, glass, yawO, yaw;
    /** Eased movement (the synced positions arrive in uneven steps). */
    private double svy, sh;
    private boolean started;

    /**
     * One client tick.
     *
     * @param freqSlow  phase per tick at rest
     * @param freqFast  extra phase per tick at full speed
     * @param maxPitch  largest pitch in degrees (0 = never tilts)
     * @param onGlass   the fish is (to be) upright on the glass
     */
    public void tick(Entity e, float freqSlow, float freqFast, float maxPitch, boolean onGlass) {
        double vx = e.getX() - e.xo, vy = e.getY() - e.yo, vz = e.getZ() - e.zo;
        double h = Math.sqrt(vx * vx + vz * vz);
        if (!started) {
            started = true;
            yaw = yawO = e.getYRot();
            glass = glassO = onGlass ? 1 : 0;
            phase = phaseO = e.getId() * 1.7F;
        }
        swimO = swim;
        phaseO = phase;
        pitchO = pitch;
        glassO = glass;
        yawO = yaw;
        svy += (vy - svy) * 0.25;
        sh += (h - sh) * 0.25;
        float wantSwim = (float) Mth.clamp(sh / 0.03, 0, 1);
        swim += (wantSwim - swim) * 0.2F;
        phase += freqSlow + freqFast * swim;
        // keep the numbers small (sin is periodic; both old and new phase move together)
        if (phase > 1000F * Mth.TWO_PI) {
            phase -= 1000F * Mth.TWO_PI;
            phaseO -= 1000F * Mth.TWO_PI;
        }
        float wantPitch = 0;
        if (maxPitch > 0 && e.isInWater() && !onGlass) {
            float raw = (float) Math.toDegrees(Math.atan2(svy, Math.max(sh, 0.015)));
            // fades in with the speed instead of a hard threshold
            float weight = (float) Mth.clamp((sh + Math.abs(svy) - 0.002) / 0.012, 0, 1);
            wantPitch = Mth.clamp(raw, -maxPitch, maxPitch) * weight * weight * (3 - 2 * weight);
        }
        // eased, and at most 4 degrees a tick
        pitch += Mth.clamp((wantPitch - pitch) * 0.15F, -4F, 4F);
        glass = Mth.approach(glass, onGlass ? 1F : 0F, 0.08F);
        yaw += Mth.wrapDegrees(e.getYRot() - yaw) * 0.35F;
    }

    public float swim(float pt) {
        return Mth.lerp(pt, swimO, swim);
    }

    public float phase(float pt) {
        return Mth.lerp(pt, phaseO, phase);
    }

    public float pitch(float pt) {
        return Mth.lerp(pt, pitchO, pitch);
    }

    /** 0..1, eased (smoothstep) for the upright turn. */
    public float glass(float pt) {
        float g = Mth.lerp(pt, glassO, glass);
        return g * g * (3 - 2 * g);
    }

    public float glassRaw() {
        return glass;
    }

    /** The smoothed body yaw (set as yBodyRot, the renderer interpolates yBodyRotO..yBodyRot). */
    public float yaw() {
        return yaw;
    }
}
