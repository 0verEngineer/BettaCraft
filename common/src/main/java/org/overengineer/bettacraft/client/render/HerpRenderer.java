package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.client.model.FrogModel;
import org.overengineer.bettacraft.client.model.GeckoModel;
import org.overengineer.bettacraft.client.model.SnakeModel;
import org.overengineer.bettacraft.client.model.TadpoleModel;
import org.overengineer.bettacraft.terrarium.HerpEntity;
import org.overengineer.bettacraft.terrarium.HerpSpecies;
import org.overengineer.bettacraft.terrarium.LandWalker;
import org.overengineer.bettacraft.terrarium.TerrariumAnimalEntity;

/**
 * M31 part c: renderer of the amphibians and reptiles: the model of the body plan ({@link FrogModel}, {@link GeckoModel},
 * {@link SnakeModel}; a dart frog tadpole the {@link TadpoleModel}), the texture of the variety, the size of the species and
 * of a young one, and the poses of the walker: <b>clinging to a wall</b> (turned head up with the belly on the glass) and
 * <b>buried</b> (sunk into the ground up to the eyes).
 */
public class HerpRenderer extends MobRenderer<TerrariumAnimalEntity, EntityModel<TerrariumAnimalEntity>> {
    private static final Map<String, ResourceLocation> TEXTURES = new ConcurrentHashMap<>();
    private final HerpSpecies species;
    private final EntityModel<TerrariumAnimalEntity> main;
    @Nullable
    private final EntityModel<TerrariumAnimalEntity> tadpole;
    private final float scale, buryDepth;

    public HerpRenderer(EntityRendererProvider.Context ctx, HerpSpecies species, float scale, float shadow, float buryDepth) {
        super(ctx, model(ctx, species), shadow);
        this.species = species;
        this.main = model;
        this.tadpole = species.tadpoles() ? new TadpoleModel(ctx.bakeLayer(TerrariumAnimalRenderer.layer(species.id() + "_tadpole"))) : null;
        this.scale = scale;
        this.buryDepth = buryDepth;
    }

    private static EntityModel<TerrariumAnimalEntity> model(EntityRendererProvider.Context ctx, HerpSpecies s) {
        ModelPart part = ctx.bakeLayer(TerrariumAnimalRenderer.layer(s.id()));
        Function<ModelPart, EntityModel<TerrariumAnimalEntity>> f = switch (s.body()) {
            case FROG -> FrogModel::new;
            case SNAKE -> SnakeModel::new;
            default -> GeckoModel::new;
        };
        return f.apply(part);
    }

    private static boolean tadpole(TerrariumAnimalEntity e) {
        return e instanceof HerpEntity h && h.isTadpole();
    }

    @Override
    public void render(TerrariumAnimalEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        model = tadpole != null && tadpole(e) ? tadpole : main;
        super.render(e, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(TerrariumAnimalEntity e) {
        String name = tadpole != null && tadpole(e) ? species.id() + "_tadpole"
                : species.texture(e instanceof HerpEntity h ? h.variety() : 0);
        return TEXTURES.computeIfAbsent(name, k -> BettaCraft.id("textures/entity/terrarium/" + k + ".png"));
    }

    @Override
    protected void scale(TerrariumAnimalEntity e, PoseStack pose, float partialTick) {
        float s = scale * e.getAgeScale();
        pose.scale(s, s, s);
    }

    @Override
    protected void setupRotations(TerrariumAnimalEntity e, PoseStack pose, float bob, float bodyRot, float partialTick, float scale) {
        LandWalker.Pose p = e.herpPose();
        Direction wall = p.wall();
        if (wall != null) {
            // clinging: head towards the wall, then turned nose up, so the belly lies on the glass
            super.setupRotations(e, pose, bob, wall.toYRot(), partialTick, scale);
            pose.mulPose(Axis.XP.rotationDegrees(90F));
            return;
        }
        super.setupRotations(e, pose, bob, bodyRot, partialTick, scale);
        if (p == LandWalker.Pose.BURIED) pose.translate(0, -buryDepth * e.getAgeScale(), 0);
    }
}
