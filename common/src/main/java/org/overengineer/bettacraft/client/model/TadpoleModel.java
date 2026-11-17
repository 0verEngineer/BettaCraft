package org.overengineer.bettacraft.client.model;

import org.overengineer.bettacraft.client.render.MobState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.overengineer.bettacraft.terrarium.TerrariumAnimalEntity;

/** M31 part c: a dart frog tadpole ({@code dart_frog_tadpole}: body, tail); the tail beats, faster while it swims. */
public class TadpoleModel extends EntityModel<MobState<TerrariumAnimalEntity>> {
    private final ModelPart body, tail;

    public TadpoleModel(ModelPart root) {
        super(root);
        ModelPart animal = root.getChild("animal");
        this.body = animal.getChild("body");
        this.tail = animal.getChild("tail");
    }

    @Override
    public void setupAnim(MobState<TerrariumAnimalEntity> state) {
        TerrariumAnimalEntity e = state.entity;
        float limbSwing = state.walkAnimationPos, limbSwingAmount = state.walkAnimationSpeed, ageInTicks = state.ageInTicks, netHeadYaw = state.yRot, headPitch = state.xRot;
        root().getAllParts().forEach(ModelPart::resetPose);
        float speed = 0.25F + Math.min(1F, limbSwingAmount * 6F) * 0.6F;
        tail.yRot = Mth.sin(ageInTicks * speed) * 0.55F;
        body.yRot = -tail.yRot * 0.15F;
    }
}
