package org.overengineer.bettacraft.client.model;

import org.overengineer.bettacraft.client.render.MobState;
import net.minecraft.client.model.EntityModel;
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
public class FrogModel extends EntityModel<MobState<TerrariumAnimalEntity>> {
    private final ModelPart body, head, legFr, legFl, legBr, legBl;

    public FrogModel(ModelPart root) {
        super(root);
        ModelPart animal = root.getChild("animal");
        this.body = animal.getChild("body");
        this.head = animal.getChild("head");
        this.legFr = animal.getChild("leg_fr");
        this.legFl = animal.getChild("leg_fl");
        this.legBr = animal.getChild("leg_br");
        this.legBl = animal.getChild("leg_bl");
    }

    @Override
    public void setupAnim(MobState<TerrariumAnimalEntity> state) {
        TerrariumAnimalEntity e = state.entity;
        float limbSwing = state.walkAnimationPos, limbSwingAmount = state.walkAnimationSpeed, ageInTicks = state.ageInTicks, netHeadYaw = state.yRot, headPitch = state.xRot;
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
        // M54: the actions of a frog while it sits still (eased in and out): it looks around, it calls (the throat pumps fast)
        float look = e.actionWeight(org.overengineer.bettacraft.terrarium.Habits.Action.LOOK, ageInTicks);
        float call = e.actionWeight(org.overengineer.bettacraft.terrarium.Habits.Action.CALL, ageInTicks);
        if (look > 0) head.yRot += Mth.clamp(Mth.sin(ageInTicks * 0.06F) * 1.6F, -1F, 1F) * 0.35F * look;
        if (call > 0) {
            float pump = Math.max(0F, Mth.sin(ageInTicks * 1.3F));
            head.y += pump * 0.5F * call;
            body.xScale = 1F + 0.1F * pump * call;
            body.yScale = 1F + 0.06F * pump * call;
            head.xRot -= 0.12F * call;
        }
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
