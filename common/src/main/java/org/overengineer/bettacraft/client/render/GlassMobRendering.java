package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.spider.Spider;
import org.joml.Vector3f;
import org.overengineer.bettacraft.BettaCraft;

/**
 * M31 part d: a spider in glass ({@link org.overengineer.bettacraft.mobs.GlassMobs}, shrunk) that climbs the glass of its
 * tank lies with its belly on the glass, head up, like the climbing geckos ({@link HerpRenderer}); a vanilla spider climbs
 * upright. The walker turns it towards the wall and sets the synced climbing flag; the client only sees the small scale.
 */
public final class GlassMobRendering {
    /** Below this scale a spider is one in glass (wild ones are 1; the cave spider in glass 0.3). */
    static final float GLASS_SCALE = 0.6F;
    static final Set<LivingEntity> TURNED = Collections.newSetFromMap(new IdentityHashMap<>());

    private GlassMobRendering() {}

    /** Before a living entity is drawn: a climbing spider in glass lies flat on the glass. */
    public static void pre(LivingEntity e, float partialTick, com.mojang.blaze3d.vertex.PoseStack pose) {
        if (!(e instanceof net.minecraft.world.entity.monster.spider.Spider s) || !s.isClimbing() || s.getScale() >= GLASS_SCALE) return;
        net.minecraft.core.Direction wall = net.minecraft.core.Direction.fromYRot(net.minecraft.util.Mth.rotLerp(partialTick, s.yBodyRotO, s.yBodyRot));
        pose.pushPose();
        // turn about the horizontal axis along the glass: its front goes up, its belly onto the glass
        pose.mulPose(com.mojang.math.Axis.of(new org.joml.Vector3f(-wall.getStepZ(), 0, wall.getStepX())).rotationDegrees(90F));
        TURNED.add(s);
    }

    /** After it was drawn. */
    public static void post(LivingEntity e, com.mojang.blaze3d.vertex.PoseStack pose) {
        if (TURNED.remove(e)) pose.popPose();
    }

}
