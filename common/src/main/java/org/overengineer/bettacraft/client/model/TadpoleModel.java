package org.overengineer.bettacraft.client.model;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.overengineer.bettacraft.terrarium.TerrariumAnimalEntity;

/** M31 part c: a dart frog tadpole ({@code dart_frog_tadpole}: body, tail); the tail beats, faster while it swims. */
public class TadpoleModel extends HierarchicalModel<TerrariumAnimalEntity> {
    private final ModelPart root, body, tail;

    public TadpoleModel(ModelPart root) {
        this.root = root;
        ModelPart animal = root.getChild("animal");
        this.body = animal.getChild("body");
        this.tail = animal.getChild("tail");
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(TerrariumAnimalEntity e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        float speed = 0.25F + Math.min(1F, limbSwingAmount * 6F) * 0.6F;
        tail.yRot = Mth.sin(ageInTicks * speed) * 0.55F;
        body.yRot = -tail.yRot * 0.15F;
    }
}
