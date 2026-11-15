package org.overengineer.bettacraft.client.model;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.overengineer.bettacraft.terrarium.TerrariumAnimalEntity;

/**
 * M31: the model of a four-legged terrarium animal ({@link TerrariumModelDefinitions}: shell, head, four legs, tail). The legs
 * walk in a diagonal gait on land and paddle in the water; on a basking spot the head stretches up and the hind legs lie
 * stretched out behind (as turtles do under a lamp).
 */
public class TerrariumAnimalModel extends HierarchicalModel<TerrariumAnimalEntity> {
    private final ModelPart root, animal, head, legFr, legFl, legBr, legBl, tail;

    public TerrariumAnimalModel(ModelPart root) {
        this.root = root;
        this.animal = root.getChild("animal");
        this.head = animal.getChild("head");
        this.legFr = animal.getChild("leg_fr");
        this.legFl = animal.getChild("leg_fl");
        this.legBr = animal.getChild("leg_br");
        this.legBl = animal.getChild("leg_bl");
        this.tail = animal.getChild("tail");
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(TerrariumAnimalEntity e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        float amount = Math.min(1F, limbSwingAmount * 4F);
        float swing = Mth.sin(limbSwing * 2.2F) * 0.6F * amount;
        // a diagonal walk: front right with back left
        legFr.yRot = swing;
        legBl.yRot = swing;
        legFl.yRot = -swing;
        legBr.yRot = -swing;
        if (e.isInWater() && amount > 0.05F) { // paddling: the front legs sweep wider
            legFr.zRot = -0.3F * Mth.sin(limbSwing * 2.2F);
            legFl.zRot = 0.3F * Mth.sin(limbSwing * 2.2F);
        }
        head.yRot = Mth.clamp(netHeadYaw, -40F, 40F) * Mth.DEG_TO_RAD * 0.5F + Mth.sin(ageInTicks * 0.03F) * 0.08F;
        head.xRot = 0;
        if (e.isBasking()) {
            head.xRot = -0.35F;
            head.z -= 0.6F;
            legBr.yRot = -0.9F;
            legBl.yRot = 0.9F;
        }
        tail.yRot = Mth.sin(ageInTicks * 0.05F) * 0.1F;
        // M31 part b: brumation: head and legs drawn into the shell, no movement
        if (e instanceof org.overengineer.bettacraft.terrarium.TurtleEntity t && t.isBrumating()) {
            head.yRot = 0;
            head.xRot = 0.15F;
            head.z += 2.2F;
            head.y += 0.4F;
            for (ModelPart leg : new ModelPart[]{legFr, legFl, legBr, legBl}) {
                leg.yRot = 0;
                leg.zRot = 0;
                leg.x *= 0.7F;
                leg.y -= 0.6F;
            }
            legFr.z += 1.0F;
            legFl.z += 1.0F;
            legBr.z -= 1.0F;
            legBl.z -= 1.0F;
            tail.yRot = 0;
        }
    }
}
