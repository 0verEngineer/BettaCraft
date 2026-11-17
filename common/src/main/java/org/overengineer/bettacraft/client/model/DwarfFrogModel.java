package org.overengineer.bettacraft.client.model;

import org.overengineer.bettacraft.client.render.MobState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.overengineer.bettacraft.entity.frog.DwarfFrogEntity;

/**
 * M14: the dwarf clawed frog ({@link CritterModelDefinitions#createFrogLayer()}): the hind legs kick while it swims, the
 * front legs paddle, at the surface it points its nose up to breathe. M47: in amplexus the male clasps the female.
 */
public class DwarfFrogModel extends EntityModel<MobState<DwarfFrogEntity>> {
    private final ModelPart frog, body, head, armRight, armLeft, legRight, legLeft;

    public DwarfFrogModel(ModelPart root) {
        super(root);
        this.frog = root.getChild("frog");
        this.body = frog.getChild("body");
        this.head = frog.getChild("head");
        this.armRight = frog.getChild("arm_right");
        this.armLeft = frog.getChild("arm_left");
        this.legRight = frog.getChild("leg_right");
        this.legLeft = frog.getChild("leg_left");
    }

    @Override
    public void setupAnim(MobState<DwarfFrogEntity> state) {
        DwarfFrogEntity frog = state.entity;
        float limbSwing = state.walkAnimationPos, limbSwingAmount = state.walkAnimationSpeed, ageInTicks = state.ageInTicks, netHeadYaw = state.yRot, headPitch = state.xRot;
        float kick = Mth.sin(limbSwing * 1.6F) * Math.min(1F, limbSwingAmount * 3F);
        float idle = Mth.sin(ageInTicks * 0.08F) * 0.06F;
        legRight.yRot = -0.5F * kick - 0.1F + idle;
        legLeft.yRot = 0.5F * kick + 0.1F - idle;
        legRight.xRot = legLeft.xRot = 0.1F * Math.abs(kick);
        armRight.yRot = 0.35F * kick + idle;
        armLeft.yRot = -0.35F * kick - idle;
        boolean breathing = frog.isBreathing();
        this.frog.xRot = breathing ? -0.6F : (frog.isInWater() && frog.getDeltaMovement().y > 0.01 ? -0.3F : 0F);
        head.xRot = breathing ? -0.15F : Mth.sin(ageInTicks * 0.05F) * 0.03F;
        body.xRot = 0;
        if (frog.claspedForRender() != null) {
            // M47: amplexus: the male holds the female around the waist with his front legs, the hind legs pressed to her
            armRight.yRot = 0.9F;
            armLeft.yRot = -0.9F;
            armRight.zRot = -0.5F;
            armLeft.zRot = 0.5F;
            legRight.yRot = -0.25F + Mth.sin(ageInTicks * 0.3F) * 0.05F;
            legLeft.yRot = 0.25F - Mth.sin(ageInTicks * 0.3F) * 0.05F;
            this.frog.xRot = -0.12F;
            head.xRot = 0.05F;
        } else {
            armRight.zRot = 0;
            armLeft.zRot = 0;
        }
    }
}
