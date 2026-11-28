package org.overengineer.bettacraft.client.render;

import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.client.model.BettaModel;
import org.overengineer.bettacraft.client.model.BettaModelDefinitions;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.genetics.BettaColors;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.genetics.BettaPatterns;

/**
 * Renders a betta from greyscale mask layers tinted at runtime with the genome's colours (see tools/assetgen README):
 * body -> fins -> pattern -> bars -> iridescence -> eyes. Females are slightly paler, sick fish desaturated. The layers
 * are composited into one texture per look ({@link BettaSkins}) and drawn in a single pass, so shader packs neither
 * z-fight coplanar passes nor drop the translucent ones.
 */
public class BettaRenderer extends MobRenderer<BettaEntity, MobState<BettaEntity>, BettaModel> {
    public static final Map<BettaModelDefinitions.Shape, ModelLayerLocation> LAYERS = new EnumMap<>(BettaModelDefinitions.Shape.class);

    static {
        for (BettaModelDefinitions.Shape s : BettaModelDefinitions.Shape.values()) {
            LAYERS.put(s, new ModelLayerLocation(BettaCraft.id("betta_" + s.name().toLowerCase(java.util.Locale.ROOT)), "main"));
        }
    }

    private static final Identifier BODY = tex("region_body");
    private static final Identifier FINS = tex("region_fins");
    private static final Identifier IRIDESCENCE = tex("iridescence");
    private static final Identifier EYE = tex("eye");
    private static final Identifier BARS = tex("pattern_bars");
    private static final Map<String, Identifier> PATTERN_TEX = new java.util.HashMap<>();

    static {
        for (String p : BettaModelDefinitions.PATTERNS) PATTERN_TEX.put(p, tex("pattern_" + p));
    }

    /** Iridescence pulse steps per cycle (each step is its own composite). */
    private static final int SHEEN_STEPS = 6;
    private final Map<BettaModelDefinitions.Shape, BettaModel> models = new EnumMap<>(BettaModelDefinitions.Shape.class);

    public BettaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new BettaModel(ctx.bakeLayer(LAYERS.get(BettaModelDefinitions.Shape.ROUND))), 0.15F);
        for (BettaModelDefinitions.Shape s : BettaModelDefinitions.Shape.values()) {
            models.put(s, new BettaModel(ctx.bakeLayer(LAYERS.get(s))));
        }
    }

    private static Identifier tex(String name) {
        return BettaCraft.id("textures/entity/betta/" + name + ".png");
    }

    public static BettaModelDefinitions.Shape shapeOf(BettaGenome g) {
        for (BettaModelDefinitions.Shape s : BettaModelDefinitions.Shape.values()) {
            if (s.name().equalsIgnoreCase(g.shape())) return s;
        }
        return BettaModelDefinitions.Shape.ROUND; // unknown (datapack) tail shapes
    }

    @Override
    public MobState<BettaEntity> createRenderState() {
        return new MobState<>();
    }

    /** M52b: what 1.21.1 chose in render(): the model of the tail shape, the composited skin, the stage alpha. */
    @Override
    public void extractRenderState(BettaEntity fish, MobState<BettaEntity> state, float partialTick) {
        super.extractRenderState(fish, state, partialTick);
        state.entity = fish;
        state.partialTick = partialTick;
        state.model = models.get(shapeOf(fish.genome()));
        BettaTints.beginFish(fish);
        Identifier skin = skin(fish);
        BettaTints.endFish();
        state.skin = skin != null ? skin : BODY;
        // the stage alpha (see-through fry) is in the composite itself (shader packs ignore vertex alpha)
        state.alpha = Math.round(BettaModelDefinitions.STAGE_ALPHAS[fish.lifeStage().ordinal()] * 255);
    }

    @Override
    protected float getShadowRadius(MobState<BettaEntity> state) {
        return 0.15F * state.entity.sizeScale();
    }

    @Override
    public void submit(MobState<BettaEntity> state, PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector collector,
                       net.minecraft.client.renderer.state.level.CameraRenderState camera) {
        if (state.model instanceof BettaModel m) this.model = m;
        super.submit(state, pose, collector, camera);
    }

    /** The composited texture of this fish right now (tints depend on sex, stage, health, flaring and the tank). */
    @Nullable
    private static Identifier skin(BettaEntity f) {
        BettaGenome g = f.genome();
        java.util.List<BettaSkins.Layer> layers = new java.util.ArrayList<>(6);
        // body and fins carry the stage alpha; the overlays only colour what is there (BettaSkins keeps their coverage)
        layers.add(new BettaSkins.Layer(BODY, BettaTints.forFish(BettaColors.rgb(g.bodyColor()), f)));
        layers.add(new BettaSkins.Layer(FINS, BettaTints.forFish(BettaColors.rgb(g.finColor()), f)));
        boolean fry = f.lifeStage().ordinal() <= 2;
        if (!fry) {
            Identifier pattern = PATTERN_TEX.get(g.pattern());
            if (pattern != null && !pattern.equals(PATTERN_TEX.get(BettaPatterns.NONE))) {
                layers.add(new BettaSkins.Layer(pattern, BettaTints.forFish(BettaColors.rgb(g.patternColor()), f) | 0xFF000000));
            }
            // breeding colours: females in the mood show vertical bars (PLAN 5.2), dark on light fish and pale on dark fish
            if (f.isFemale() && f.isInBreedingMood()) layers.add(new BettaSkins.Layer(BARS, BettaTints.bars(g)));
            if (!"none".equals(g.iridescence()) && (f.isAdult() || f.lifeStage().ordinal() >= 3)) {
                // the sheen pulses with sin(tick * 0.08): sampled in SHEEN_STEPS steps per cycle
                int period = Math.round((float) (2 * Math.PI / 0.08));
                int step = Math.floorMod(f.tickCount, period) * SHEEN_STEPS / period;
                int tick = Math.round((step + 0.5F) * period / SHEEN_STEPS);
                layers.add(new BettaSkins.Layer(IRIDESCENCE, BettaTints.iridescence(g, f.isSick(), tick)));
            }
        }
        layers.add(new BettaSkins.Layer(EYE, 0xFFFFFFFF));
        return BettaSkins.get(layers.toArray(new BettaSkins.Layer[0]));
    }

    @Override
    public Identifier getTextureLocation(MobState<BettaEntity> state) {
        return state.skin != null ? state.skin : BODY;
    }

    @Nullable
    @Override
    protected RenderType getRenderType(MobState<BettaEntity> state, boolean bodyVisible, boolean translucent, boolean glowing) {
        Identifier currentSkin = getTextureLocation(state);
        int currentAlpha = state.alpha;
        if (!bodyVisible && !translucent) return glowing ? RenderTypes.outline(currentSkin) : null;
        // fry are see-through (PLAN 5.3). With back-face culling: the fins are flat boxes whose front and back faces lie
        // in one plane; drawn both, shader packs let them z-fight (flickering fins). Culled, each side shows one face.
        return currentAlpha < 255 ? RenderTypes.entityTranslucentCullItemTarget(currentSkin) : RenderTypes.entityCutoutCull(currentSkin);
    }

    @Override
    protected void scale(MobState<BettaEntity> state, PoseStack pose) {
        BettaEntity fish = state.entity;
        float s = BettaModelDefinitions.ADULT_RENDER_SCALE * fish.sizeScale() // M36: life stage times the size setting
                * org.overengineer.bettacraft.genetics.BettaSpecies.scaleOf(fish.genome().species()); // M29 part c: imbellis smaller
        pose.scale(s, s, s);
    }

    @Override
    protected void setupRotations(MobState<BettaEntity> state, PoseStack pose, float yBodyRot, float scale) {
        super.setupRotations(state, pose, yBodyRot, scale);
        BettaEntity fish = state.entity;
        float bob = state.ageInTicks, partialTick = state.partialTick;
        // body wiggle: strong while swimming, a faint sway while hovering (a full wiggle at rest looked like turning)
        double v = Math.hypot(fish.getX() - fish.xo, fish.getZ() - fish.zo);
        // M21.5: the eased speed (the raw last-tick speed made the wiggle jump)
        float swim = Features.enabled(Feature.SMOOTH_SWIMMING) ? Math.min(1F, fish.swimPose.swim(partialTick) * 0.75F)
                : (float) Math.min(1.0, v / 0.04);
        float f = (fish.isSick() ? 2.0F : 1.2F + 3.1F * swim) * Mth.sin(0.6F * bob);
        pose.mulPose(Axis.YP.rotationDegrees(f));
        if (FishFlop.outOfWater(fish)) {
            pose.translate(0.1F, 0.1F, -0.1F);
            pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
        }
    }
}
