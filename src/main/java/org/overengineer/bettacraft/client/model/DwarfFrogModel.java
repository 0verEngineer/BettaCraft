package org.overengineer.bettacraft.client.model;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.overengineer.bettacraft.entity.frog.DwarfFrogEntity;

/**
 * M14: the dwarf clawed frog ({@link CritterModelDefinitions#createFrogLayer()}): the hind legs kick while it swims, the
 * front legs paddle, at the surface it points its nose up to breathe.
 */
public class DwarfFrogModel extends HierarchicalModel<DwarfFrogEntity> {
    private final ModelPart root, frog, body, head, armRight, armLeft, legRight, legLeft;

    public DwarfFrogModel(ModelPart root) {
        this.root = root;
        this.frog = root.getChild("frog");
        this.body = frog.getChild("body");
        this.head = frog.getChild("head");
        this.armRight = frog.getChild("arm_right");
        this.armLeft = frog.getChild("arm_left");
        this.legRight = frog.getChild("leg_right");
        this.legLeft = frog.getChild("leg_left");
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(DwarfFrogEntity frog, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
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
    }
}
