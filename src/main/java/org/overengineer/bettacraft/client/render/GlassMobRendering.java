package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Spider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.joml.Vector3f;
import org.overengineer.bettacraft.BettaCraft;

/**
 * M31 part d: a spider in glass ({@link org.overengineer.bettacraft.mobs.GlassMobs}, shrunk) that climbs the glass of its
 * tank lies with its belly on the glass, head up, like the climbing geckos ({@link HerpRenderer}); a vanilla spider climbs
 * upright. The walker turns it towards the wall and sets the synced climbing flag; the client only sees the small scale.
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID, value = Dist.CLIENT)
public final class GlassMobRendering {
    /** Below this scale a spider is one in glass (wild ones are 1; the cave spider in glass 0.3). */
    private static final float GLASS_SCALE = 0.6F;
    private static final Set<LivingEntity> TURNED = Collections.newSetFromMap(new IdentityHashMap<>());

    private GlassMobRendering() {}

    @SubscribeEvent(priority = EventPriority.LOWEST) // after every listener that could cancel it (then Post never comes)
    public static void pre(RenderLivingEvent.Pre<?, ?> e) {
        if (!(e.getEntity() instanceof Spider s) || !s.isClimbing() || s.getScale() >= GLASS_SCALE) return;
        Direction wall = Direction.fromYRot(Mth.rotLerp(e.getPartialTick(), s.yBodyRotO, s.yBodyRot));
        PoseStack pose = e.getPoseStack();
        pose.pushPose();
        // turn about the horizontal axis along the glass: its front goes up, its belly onto the glass
        pose.mulPose(Axis.of(new Vector3f(-wall.getStepZ(), 0, wall.getStepX())).rotationDegrees(90F));
        TURNED.add(s);
    }

    @SubscribeEvent
    public static void post(RenderLivingEvent.Post<?, ?> e) {
        if (TURNED.remove(e.getEntity())) e.getPoseStack().popPose();
    }
}
