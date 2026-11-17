package org.overengineer.bettacraft.client.model;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.overengineer.bettacraft.terrarium.LandWalker;
import org.overengineer.bettacraft.terrarium.TerrariumAnimalEntity;

/**
 * M31 part c: the model of a frog ({@link TerrariumModelDefinitions} {@code dart_frog}, {@code horned_frog}: body, head, arms,
 * the long folded hind legs). Sitting, the hind legs lie folded beside the body; in a hop (the walker lifts the frog in an
 * arc) they stretch out behind and the arms reach forward; in the water the hind legs kick. The throat pumps while it
 * breathes; a buried horned frog sits with its legs pulled in.
 */
public class FrogModel extends HierarchicalModel<TerrariumAnimalEntity> {
    private final ModelPart root, body, head, legFr, legFl, legBr, legBl;

    public FrogModel(ModelPart root) {
        this.root = root;
        ModelPart animal = root.getChild("animal");
        this.body = animal.getChild("body");
        this.head = animal.getChild("head");
        this.legFr = animal.getChild("leg_fr");
        this.legFl = animal.getChild("leg_fl");
        this.legBr = animal.getChild("leg_br");
        this.legBl = animal.getChild("leg_bl");
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(TerrariumAnimalEntity e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        float rise = (float) (e.getY() - e.yo);
        boolean airborne = Math.abs(rise) > 0.002 && !e.isInWater() && limbSwingAmount > 0.01;
        if (airborne) {
            // the hop: hind legs stretched out behind, arms forward
            float stretch = rise > 0 ? 1F : 0.6F;
            legBr.xRot = 0.9F * stretch;
            legBl.xRot = 0.9F * stretch;
            legBr.z += 1.2F * stretch;
            legBl.z += 1.2F * stretch;
            legFr.xRot = -0.6F * stretch;
            legFl.xRot = -0.6F * stretch;
            body.xRot = -0.15F * stretch;
            head.xRot = -0.1F * stretch;
        } else if (e.isInWater() && limbSwingAmount > 0.02) {
            float kick = Mth.sin(limbSwing * 1.6F) * 0.7F;
            legBr.xRot = 0.5F + kick;
            legBl.xRot = 0.5F + kick;
            legFr.xRot = -0.4F;
            legFl.xRot = -0.4F;
        }
        // breathing: the throat and flanks pump
        float breath = Mth.sin(ageInTicks * 0.35F) * 0.04F;
        head.y += breath * 4F;
        head.yRot = Mth.clamp(netHeadYaw, -20F, 20F) * Mth.DEG_TO_RAD * 0.3F;
        if (e.herpPose() == LandWalker.Pose.BURIED) {
            // sitting buried: legs drawn in, only the head and the eyes above the ground
            for (ModelPart leg : new ModelPart[]{legFr, legFl, legBr, legBl}) {
                leg.xRot = 0;
                leg.x *= 0.8F;
            }
            head.yRot = 0;
        }
    }
}
