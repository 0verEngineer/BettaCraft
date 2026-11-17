package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.client.model.TerrariumAnimalModel;
import org.overengineer.bettacraft.terrarium.TerrariumAnimalEntity;

/**
 * M31: renderer of a terrarium animal: its model ({@code TerrariumModelDefinitions}) and texture
 * ({@code textures/entity/terrarium/<model>.png}) drawn at its size ({@code scale}: the 16 px model is one block long at 1).
 */
public class TerrariumAnimalRenderer extends MobRenderer<TerrariumAnimalEntity, MobState<TerrariumAnimalEntity>, TerrariumAnimalModel> {
    private final Identifier texture;
    private final float scale;

    public TerrariumAnimalRenderer(EntityRendererProvider.Context ctx, String model, float scale, float shadow) {
        super(ctx, new TerrariumAnimalModel(ctx.bakeLayer(layer(model))), shadow);
        this.texture = BettaCraft.id("textures/entity/terrarium/" + model + ".png");
        this.scale = scale;
    }

    public static ModelLayerLocation layer(String model) {
        return new ModelLayerLocation(BettaCraft.id(model), "main");
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
        state.skin = texture(e);
    }

    private Identifier texture(TerrariumAnimalEntity e) {
        // M31 part b: the varieties (the slider: red-eared, yellow-bellied) have their own textures
        if (e instanceof org.overengineer.bettacraft.terrarium.HerpEntity t && t.species().varieties().size() > 1) {
            return VARIETIES.computeIfAbsent(t.species().texture(t.variety()), k -> BettaCraft.id("textures/entity/terrarium/" + k + ".png"));
        }
        return texture;
    }

    @Override
    public Identifier getTextureLocation(MobState<TerrariumAnimalEntity> state) {
        return state.skin != null ? state.skin : texture;
    }

    private static final java.util.Map<String, Identifier> VARIETIES = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    protected void scale(MobState<TerrariumAnimalEntity> state, PoseStack pose) {
        float s = scale * state.entity.getAgeScale(); // M31 part b: a turtle hatchling grows from its hatch size
        pose.scale(s, s, s);
    }
}
