package org.overengineer.bettacraft.client.model;

import org.overengineer.bettacraft.client.render.MobState;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;

/**
 * M19: a tankmate ({@link TankmateModelDefinitions}): the tail beats faster while swimming, the pectorals flutter, the
 * kuhli loach bends in a wave along its segments, the gourami's feelers swing; parts of only one sex (the guppy male's
 * big tail and the gonopodium, the bristlenose male's bristles) show by sex.
 */
public class TankmateModel extends EntityModel<MobState<TankmateEntity>> {
    @Nullable
    private final ModelPart tail, dorsal, pecR, pecL, male, female, filaments, jaw;
    private final List<ModelPart> segments = new ArrayList<>();
    /** M21.8: the seahorse's tail chain (curled round the hitching post). */
    private final List<ModelPart> curls = new ArrayList<>();
    /** M30 part b: the margin strips of a ray's disc, front to back (the wave runs along them). */
    private final List<ModelPart> waveR = new ArrayList<>(), waveL = new ArrayList<>();

    public TankmateModel(ModelPart root) {
        super(root);
        ModelPart fish = root.getChild("fish");
        this.tail = find(fish, "tail");
        this.dorsal = find(fish, "dorsal");
        this.pecR = find(fish, "pec_r");
        this.pecL = find(fish, "pec_l");
        this.male = find(fish, "male");
        this.female = find(fish, "female");
        this.filaments = find(fish, "filaments");
        this.jaw = find(fish, "jaw"); // M30 part c: the moray's lower jaw
        for (int i = 1; i <= 8; i++) {
            ModelPart s = find(fish, "seg" + i);
            if (s == null) break;
            segments.add(s);
        }
        for (int i = 1; i <= 8; i++) {
            ModelPart s = find(fish, "curl" + i);
            if (s == null) break;
            curls.add(s);
        }
        for (int i = 0; i < 12; i++) {
            ModelPart r = find(fish, "wave_r" + i), l = find(fish, "wave_l" + i);
            if (r == null || l == null) break;
            waveR.add(r);
            waveL.add(l);
        }
    }

    /**
     * M30 part b: a ray swims with its disc: a wave runs from the front to the back along the margins (both sides together).
     * A bottom ray (stingray) ripples with short, quick waves, hardly at all while it lies on the ground; an open-water ray
     * (part c) beats its wing-like disc slowly and deeply (a long wave, a big amplitude). The tail trails behind.
     */
    private void ray(TankmateEntity fish, float ageInTicks, float swim, boolean rest) {
        var l = fish.species().large();
        boolean open = l != null && l.swim() == org.overengineer.bettacraft.tankmates.LargeSpecies.Swim.RAY_OPEN;
        float t = ageInTicks + fish.getId() * 7.3F;
        float freq = open ? 0.11F + 0.05F * swim : rest ? 0.08F : 0.22F + 0.14F * swim;
        float lag = open ? 0.3F : 1.05F;
        float amp = open ? (rest ? 0.08F : 0.25F + 0.15F * swim) : rest ? 0.025F : 0.12F + 0.12F * swim;
        int n = waveR.size();
        for (int i = 0; i < n; i++) {
            // the middle of the disc is widest and moves most
            float k = 0.65F + 0.35F * Mth.sin((i + 0.5F) / n * Mth.PI);
            float a = Mth.sin(t * freq - i * lag) * amp * k;
            waveR.get(i).zRot = -a;
            waveL.get(i).zRot = a;
        }
        if (tail != null) tail.yRot = Mth.sin(t * 0.07F) * (rest ? 0.04F : 0.12F);
        float flutter = Mth.sin(t * 0.5F) * 0.15F;
        if (pecR != null) pecR.zRot = flutter;
        if (pecL != null) pecL.zRot = -flutter;
    }

    /**
     * M21.8 seahorse: the tail curls forward segment by segment, tightly round the post while it holds on (FLAG_REST),
     * loosely and slowly waving while it swims; the small dorsal fin on the back flutters fast (that is how a seahorse
     * swims), the pectorals behind the head as well; a male's brood pouch swells while he carries eggs.
     */
    private void seahorse(TankmateEntity fish, float ageInTicks) {
        boolean hitched = fish.flag(TankmateEntity.FLAG_REST);
        float t = ageInTicks + fish.getId() * 3.1F;
        for (int i = 0; i < curls.size(); i++) {
            float k = (i + 1) / (float) curls.size();
            float curl = hitched ? 0.2F + 0.55F * k : 0.06F + 0.16F * k + 0.06F * Mth.sin(t * 0.06F - i * 0.7F);
            curls.get(i).xRot = -curl;
        }
        if (dorsal != null) dorsal.yRot = Mth.sin(t * 1.9F) * (hitched ? 0.25F : 0.55F);
        float flutter = Mth.sin(t * 1.6F) * 0.45F;
        if (pecR != null) pecR.yRot = flutter;
        if (pecL != null) pecL.yRot = -flutter;
        if (male != null) {
            male.visible = !fish.isFemale();
            boolean full = fish.isGravid();
            male.xScale = full ? 1.7F : 1.0F;
            male.yScale = full ? 1.35F : 1.0F;
            male.zScale = full ? 2.3F : 1.0F;
        }
    }

    /** A part anywhere below {@code p} (the kuhli loach's segments are nested). */
    @Nullable
    private static ModelPart find(ModelPart p, String name) {
        if (p.hasChild(name)) return p.getChild(name);
        for (ModelPart c : p.getAllParts().toList()) {
            if (c != p && c.hasChild(name)) return c.getChild(name);
        }
        return null;
    }

    @Override
    public void setupAnim(MobState<TankmateEntity> state) {
        TankmateEntity fish = state.entity;
        float limbSwing = state.walkAnimationPos, limbSwingAmount = state.walkAnimationSpeed, ageInTicks = state.ageInTicks, netHeadYaw = state.yRot, headPitch = state.xRot;
        root.getAllParts().forEach(ModelPart::resetPose);
        if (!curls.isEmpty()) {
            seahorse(fish, ageInTicks);
            return;
        }

        boolean smooth = Features.enabled(Feature.SMOOTH_SWIMMING);
        float pt = ageInTicks - (float) Math.floor(ageInTicks); // the partial tick (age = tickCount + partial)
        float swim;
        float phase;
        if (smooth) {
            // M21.5: eased speed and an accumulated phase (the old phase = age * speed-dependent frequency jumped)
            swim = fish.swimPose.swim(pt);
            phase = fish.swimPose.phase(pt);
        } else {
            double v = Math.sqrt(fish.getDeltaMovement().horizontalDistanceSqr());
            swim = (float) Math.min(1.0, v / 0.03);
            phase = ageInTicks * (0.45F + 1.1F * swim) + fish.getId() * 1.7F;
        }
        boolean rest = fish.flag(TankmateEntity.FLAG_REST) || fish.flag(TankmateEntity.FLAG_GLASS);
        if (!waveR.isEmpty()) {
            ray(fish, ageInTicks, swim, rest);
            return;
        }
        float amp = rest ? 0.06F : 0.12F + 0.35F * swim;
        if (tail != null) tail.yRot = Mth.sin(phase) * amp;
        if (dorsal != null) dorsal.xRot = rest ? -0.1F : 0.05F * Mth.sin(phase * 0.5F);
        float flutter = Mth.sin(ageInTicks * 0.9F + fish.getId()) * (rest ? 0.12F : 0.3F);
        if (pecR != null) pecR.yRot = flutter;
        if (pecL != null) pecL.yRot = -flutter;
        if (smooth && !segments.isEmpty()) {
            // M21.5: an eel's body wave: slow, long, travelling from the head to the tail; small bends per segment
            // (the segments are nested, the bends add up along the body)
            float segAmp = rest ? 0.035F : 0.07F + 0.06F * swim;
            for (int i = 0; i < segments.size(); i++) {
                segments.get(i).yRot = Mth.sin(phase - (i + 1) * 0.75F) * segAmp * (0.6F + 0.4F * i / Math.max(1, segments.size() - 1));
            }
            if (tail != null) tail.yRot = Mth.sin(phase - (segments.size() + 1) * 0.75F) * (rest ? 0.05F : 0.12F + 0.1F * swim);
        } else {
            for (int i = 0; i < segments.size(); i++) {
                segments.get(i).yRot = Mth.sin(phase * 0.8F - (i + 1) * 0.9F) * (rest ? 0.08F : 0.18F + 0.15F * swim);
            }
        }
        if (filaments != null) filaments.xRot = 0.15F * Mth.sin(ageInTicks * 0.12F + fish.getId());
        // M30 part c: a moray breathes by opening and closing its mouth (wider while it lies in its lair)
        if (jaw != null) jaw.xRot = (rest ? 0.2F : 0.1F) + (rest ? 0.16F : 0.08F) * Mth.sin(ageInTicks * 0.13F + fish.getId() * 2.3F);
        // M48 part a: the longfin panda cory: long, flowing tail and dorsal fin (a bred fin form on the colour locus)
        float fin = org.overengineer.bettacraft.entity.tankmate.TankmateExtents.finScale(fish);
        if (tail != null) {
            tail.zScale = fin;
            tail.yScale = 1F + (fin - 1F) * 0.5F;
            if (fin > 1F) tail.yRot *= 0.75F;
        }
        if (dorsal != null) {
            dorsal.yScale = fin;
            dorsal.zScale = 1F + (fin - 1F) * 0.6F;
        }
        boolean f = fish.isFemale();
        if (male != null) male.visible = !f;
        if (female != null) female.visible = f;
    }
}
