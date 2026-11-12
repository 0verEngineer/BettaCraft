package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.client.model.BettaModel;
import org.overengineer.bettacraft.client.model.BettaModelDefinitions;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.genetics.BettaPatterns;

/**
 * Renders a betta as greyscale mask layers tinted at runtime with the genome's colours (see tools/assetgen README):
 * body (base pass) -> fins -> pattern -> iridescence -> eyes. Females are slightly paler, sick fish desaturated.
 */
public class BettaRenderer extends MobRenderer<BettaEntity, BettaModel> {
    public static final Map<BettaModelDefinitions.Shape, ModelLayerLocation> LAYERS = new EnumMap<>(BettaModelDefinitions.Shape.class);

    static {
        for (BettaModelDefinitions.Shape s : BettaModelDefinitions.Shape.values()) {
            LAYERS.put(s, new ModelLayerLocation(BettaCraft.id("betta_" + s.name().toLowerCase(java.util.Locale.ROOT)), "main"));
        }
    }

    private static final ResourceLocation BODY = tex("region_body");
    private static final ResourceLocation FINS = tex("region_fins");
    private static final ResourceLocation IRIDESCENCE = tex("iridescence");
    private static final ResourceLocation EYE = tex("eye");
    private static final Map<String, ResourceLocation> PATTERN_TEX = new java.util.HashMap<>();

    static {
        for (String p : BettaModelDefinitions.PATTERNS) PATTERN_TEX.put(p, tex("pattern_" + p));
    }

    private final Map<BettaModelDefinitions.Shape, BettaModel> models = new EnumMap<>(BettaModelDefinitions.Shape.class);

    public BettaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new BettaModel(ctx.bakeLayer(LAYERS.get(BettaModelDefinitions.Shape.PLAKAT))), 0.15F);
        for (BettaModelDefinitions.Shape s : BettaModelDefinitions.Shape.values()) {
            models.put(s, new BettaModel(ctx.bakeLayer(LAYERS.get(s))));
        }
        addLayer(new TintLayer(this, f -> FINS, f -> BettaTints.fins(f.genome(), f.isFemale(), f.isSick())));
        addLayer(new TintLayer(this, f -> PATTERN_TEX.get(f.genome().pattern()), f -> BettaTints.pattern(f.genome(), f.isFemale(), f.isSick())));
        addLayer(new TintLayer(this, f -> "none".equals(f.genome().iridescence()) ? null : IRIDESCENCE,
                f -> BettaTints.iridescence(f.genome(), f.isSick(), f.tickCount)));
        addLayer(new TintLayer(this, f -> EYE, f -> 0xFFFFFFFF));
    }

    private static ResourceLocation tex(String name) {
        return BettaCraft.id("textures/entity/betta/" + name + ".png");
    }

    public static BettaModelDefinitions.Shape shapeOf(BettaGenome g) {
        for (BettaModelDefinitions.Shape s : BettaModelDefinitions.Shape.values()) {
            if (s.name().equalsIgnoreCase(g.shape())) return s;
        }
        return BettaModelDefinitions.Shape.PLAKAT;
    }

    @Override
    public void render(BettaEntity fish, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        BettaModel m = models.get(shapeOf(fish.genome()));
        this.model = m;
        m.setPendingBaseColor(BettaTints.body(fish.genome(), fish.isFemale(), fish.isSick()));
        super.render(fish, yaw, partialTicks, pose, buffers, light);
        m.setPendingBaseColor(0);
    }

    @Override
    public ResourceLocation getTextureLocation(BettaEntity entity) {
        return BODY;
    }

    @Nullable
    @Override
    protected RenderType getRenderType(BettaEntity entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        if (!bodyVisible && !translucent) return glowing ? RenderType.outline(BODY) : null;
        return RenderType.entityCutoutNoCull(BODY);
    }

    @Override
    protected void scale(BettaEntity fish, PoseStack pose, float partialTick) {
        float s = BettaModelDefinitions.ADULT_RENDER_SCALE;
        pose.scale(s, s, s);
    }

    @Override
    protected void setupRotations(BettaEntity fish, PoseStack pose, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(fish, pose, bob, yBodyRot, partialTick, scale);
        float f = (fish.isSick() ? 2.0F : 4.3F) * Mth.sin(0.6F * bob);
        pose.mulPose(Axis.YP.rotationDegrees(f));
        if (!fish.isInWater()) {
            pose.translate(0.1F, 0.1F, -0.1F);
            pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
        }
    }

    /** One tinted mask pass on the same model. */
    private static final class TintLayer extends RenderLayer<BettaEntity, BettaModel> {
        private final Function<BettaEntity, ResourceLocation> texture;
        private final ToIntFunction<BettaEntity> color;

        TintLayer(RenderLayerParent<BettaEntity, BettaModel> parent, Function<BettaEntity, ResourceLocation> texture,
                  ToIntFunction<BettaEntity> color) {
            super(parent);
            this.texture = texture;
            this.color = color;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, BettaEntity fish, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (fish.isInvisible()) return;
            ResourceLocation tex = texture.apply(fish);
            if (tex == null || (tex.equals(PATTERN_TEX.get(BettaPatterns.NONE)))) return;
            VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(tex));
            getParentModel().renderToBuffer(pose, vc, light, LivingEntityRenderer.getOverlayCoords(fish, 0.0F), color.applyAsInt(fish));
        }
    }
}
