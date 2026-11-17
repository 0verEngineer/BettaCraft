package org.overengineer.bettacraft.client.model;

import org.overengineer.bettacraft.client.render.MobState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.overengineer.bettacraft.terrarium.TerrariumAnimalEntity;

/**
 * M31 part c: the model of a gecko ({@code leopard_gecko}, {@code crested_gecko}: body, head, four splayed legs, tail and tail
 * tip). It walks like a lizard: the legs in a diagonal gait while the body and the tail swing from side to side; at rest the
 * tail tip curls slowly. On the glass (the renderer turns it, belly to the wall) the legs reach out wide.
 */
public class GeckoModel extends EntityModel<MobState<TerrariumAnimalEntity>> {
    private final ModelPart body, head, tail, tip, legFr, legFl, legBr, legBl;

    public GeckoModel(ModelPart root) {
        super(root);
        ModelPart animal = root.getChild("animal");
        this.body = animal.getChild("body");
        this.head = animal.getChild("head");
        this.tail = animal.getChild("tail");
        this.tip = animal.getChild("tail_tip");
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
        float amount = Math.min(1F, limbSwingAmount * 4F);
        float phase = limbSwing * 2.4F;
        float swing = Mth.sin(phase) * 0.7F * amount;
        legFr.yRot = swing;
        legBl.yRot = swing;
        legFl.yRot = -swing;
        legBr.yRot = -swing;
        legFr.y -= Math.max(0, Mth.sin(phase)) * 0.5F * amount;
        legBl.y -= Math.max(0, Mth.sin(phase)) * 0.5F * amount;
        legFl.y -= Math.max(0, -Mth.sin(phase)) * 0.5F * amount;
        legBr.y -= Math.max(0, -Mth.sin(phase)) * 0.5F * amount;
        // the lateral wave of a lizard: body one way, head and tail the other
        float wave = Mth.cos(phase) * 0.12F * amount;
        body.yRot = wave;
        head.yRot = -wave * 1.2F + Mth.clamp(netHeadYaw, -30F, 30F) * Mth.DEG_TO_RAD * 0.4F;
        float idle = Mth.sin(ageInTicks * 0.04F) * 0.12F;
        tail.yRot = -wave * 1.5F + idle * 0.5F;
        tip.yRot = tail.yRot * 1.6F + idle;
        tip.x += Mth.sin(tail.yRot) * 4.5F;
        if (e.herpPose().wall() != null) {
            // clinging to the glass: legs out wide
            legFr.yRot -= 0.5F;
            legFl.yRot += 0.5F;
            legBr.yRot += 0.5F;
            legBl.yRot -= 0.5F;
            head.xRot = -0.25F;
        }
    }
}
