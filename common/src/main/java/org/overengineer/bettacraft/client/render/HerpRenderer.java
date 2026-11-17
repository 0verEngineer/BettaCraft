package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
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
public class HerpRenderer extends MobRenderer<TerrariumAnimalEntity, MobState<TerrariumAnimalEntity>, EntityModel<MobState<TerrariumAnimalEntity>>> {
    private static final Map<String, Identifier> TEXTURES = new ConcurrentHashMap<>();
    private final HerpSpecies species;
    private final EntityModel<MobState<TerrariumAnimalEntity>> main;
    @Nullable
    private final EntityModel<MobState<TerrariumAnimalEntity>> tadpole;
    private final float scale, buryDepth;

    public HerpRenderer(EntityRendererProvider.Context ctx, HerpSpecies species, float scale, float shadow, float buryDepth) {
        super(ctx, model(ctx, species), shadow);
        this.species = species;
        this.main = model;
        this.tadpole = species.tadpoles() ? new TadpoleModel(ctx.bakeLayer(TerrariumAnimalRenderer.layer(species.id() + "_tadpole"))) : null;
        this.scale = scale;
        this.buryDepth = buryDepth;
    }

    private static EntityModel<MobState<TerrariumAnimalEntity>> model(EntityRendererProvider.Context ctx, HerpSpecies s) {
        ModelPart part = ctx.bakeLayer(TerrariumAnimalRenderer.layer(s.id()));
        Function<ModelPart, EntityModel<MobState<TerrariumAnimalEntity>>> f = switch (s.body()) {
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
    public MobState<TerrariumAnimalEntity> createRenderState() {
        return new MobState<>();
    }

    @Override
    public void extractRenderState(TerrariumAnimalEntity e, MobState<TerrariumAnimalEntity> state, float partialTick) {
        super.extractRenderState(e, state, partialTick);
        state.entity = e;
        state.partialTick = partialTick;
        boolean young = tadpole != null && tadpole(e);
        state.model = young ? tadpole : main;
        String name = young ? species.id() + "_tadpole" : species.texture(e instanceof HerpEntity h ? h.variety() : 0);
        state.skin = TEXTURES.computeIfAbsent(name, k -> BettaCraft.id("textures/entity/terrarium/" + k + ".png"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void submit(MobState<TerrariumAnimalEntity> state, PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector collector,
                       net.minecraft.client.renderer.state.CameraRenderState camera) {
        model = state.model instanceof EntityModel<?> m ? (EntityModel<MobState<TerrariumAnimalEntity>>) m : main;
        super.submit(state, pose, collector, camera);
    }

    @Override
    public Identifier getTextureLocation(MobState<TerrariumAnimalEntity> state) {
        return state.skin;
    }

    @Override
    protected void scale(MobState<TerrariumAnimalEntity> state, PoseStack pose) {
        float s = scale * state.entity.getAgeScale();
        pose.scale(s, s, s);
    }

    @Override
    protected void setupRotations(MobState<TerrariumAnimalEntity> state, PoseStack pose, float bodyRot, float scale) {
        TerrariumAnimalEntity e = state.entity;
        LandWalker.Pose p = e.herpPose();
        Direction wall = p.wall();
        if (wall != null) {
            // clinging: head towards the wall, then turned nose up, so the belly lies on the glass
            super.setupRotations(state, pose, wall.toYRot(), scale);
            pose.mulPose(Axis.XP.rotationDegrees(90F));
            return;
        }
        super.setupRotations(state, pose, bodyRot, scale);
        if (p == LandWalker.Pose.BURIED) pose.translate(0, -buryDepth * e.getAgeScale(), 0);
    }
}
