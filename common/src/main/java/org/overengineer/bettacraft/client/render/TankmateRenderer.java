package org.overengineer.bettacraft.client.render;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.client.model.TankmateModel;
import org.overengineer.bettacraft.client.model.TankmateModelDefinitions;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.entity.tankmate.TankmateExtents;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M19: the tankmates: one model per body plan, one texture per species (variety, sex), scaled to the species' length
 * and the life stage (guppy and platy females a bit bigger than the males). The body sways with the tail; ascending or
 * descending fish tilt; a pleco or otocinclus on the glass stands upright with its belly to the glass.
 */
public class TankmateRenderer extends MobRenderer<TankmateEntity, TankmateModel> {
    public static final Map<TankmateSpecies.Model, ModelLayerLocation> LAYERS = new EnumMap<>(TankmateSpecies.Model.class);

    static {
        for (TankmateSpecies.Model m : TankmateSpecies.Model.values()) {
            LAYERS.put(m, new ModelLayerLocation(BettaCraft.id("tankmate_" + name(m)), "main"));
        }
    }

    /** M21.6: the guppy and endler model of the livebearer genetics (one part per tail shape). */
    public static final ModelLayerLocation GUPPY_LAYER = new ModelLayerLocation(BettaCraft.id("tankmate_guppy"), "main");

    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();
    private final Map<TankmateSpecies.Model, TankmateModel> models = new EnumMap<>(TankmateSpecies.Model.class);
    private final org.overengineer.bettacraft.client.model.GuppyModel guppyModel;

    public TankmateRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new TankmateModel(ctx.bakeLayer(LAYERS.get(TankmateSpecies.Model.TETRA))), 0.08F);
        for (TankmateSpecies.Model m : TankmateSpecies.Model.values()) models.put(m, new TankmateModel(ctx.bakeLayer(LAYERS.get(m))));
        guppyModel = new org.overengineer.bettacraft.client.model.GuppyModel(ctx.bakeLayer(GUPPY_LAYER));
    }

    /** M21.6: a male old enough to show his colours and his big tail (subadult and adult). */
    public static boolean adultLook(TankmateEntity fish) {
        var st = fish.lifeStage();
        return !fish.isFemale() && (st == org.overengineer.bettacraft.entity.betta.LifeStage.ADULT || st == org.overengineer.bettacraft.entity.betta.LifeStage.SUBADULT);
    }

    public static String name(TankmateSpecies.Model m) {
        return m.name().toLowerCase(java.util.Locale.ROOT);
    }

    public static ResourceLocation texture(TankmateSpecies s, String variety, boolean female) {
        return TEXTURES.computeIfAbsent(s.texture(variety, female), n -> BettaCraft.id("textures/entity/tankmates/" + n + ".png"));
    }

    /** Length factor of the sexes (shared with the server: {@link TankmateExtents#sexScale}). */
    public static float sexScale(TankmateSpecies s, boolean female) {
        return TankmateExtents.sexScale(s, female);
    }

    /** The model is 32 px = 2 blocks long at scale 1: scale to the species' length, the stage and the sex. */
    public static float scaleOf(TankmateEntity fish) {
        return TankmateExtents.renderScale(fish);
    }

    /** M21.6: hybrids are between the guppy and the endler in size (by their endler share). */
    public static float hybridScale(TankmateEntity fish) {
        return TankmateExtents.hybridScale(fish);
    }

    @Override
    public void render(TankmateEntity fish, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        var lb = fish.livebearerPhenotype();
        if (lb != null) {
            // M21.6: the genetics model with the expressed tail shape
            boolean adult = adultLook(fish);
            guppyModel.prepare(lb.tail(), GuppyLook.tailScale(lb, adult), !adult);
            this.model = guppyModel;
        } else {
            this.model = models.get(fish.species().model());
        }
        this.shadowRadius = 0.1F * (float) fish.species().length() * fish.lifeStage().scale();
        super.render(fish, yaw, partialTicks, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(TankmateEntity fish) {
        var lb = fish.livebearerPhenotype();
        if (lb != null) {
            ResourceLocation skin = BettaSkins.get(GuppyLook.layers(lb, adultLook(fish), fish.isGravid()));
            if (skin != null) return skin;
        }
        return texture(fish.species(), fish.variety(), fish.isFemale());
    }

    @Override
    protected RenderType getRenderType(TankmateEntity fish, boolean bodyVisible, boolean translucent, boolean glowing) {
        ResourceLocation tex = getTextureLocation(fish);
        if (!bodyVisible && !translucent) return glowing ? RenderType.outline(tex) : null;
        return RenderType.entityCutout(tex);
    }

    @Override
    protected void scale(TankmateEntity fish, PoseStack pose, float partialTick) {
        float s = scaleOf(fish);
        // M29: the balloon ram is short and round (a bred body shape on the colour locus)
        if (fish.species() == TankmateSpecies.RAM_CICHLID && "balloon".equals(fish.variety())) pose.scale(s * 1.08F, s * TankmateExtents.heightScale(fish), s * 0.78F);
        else pose.scale(s, s, s);
    }

    @Override
    protected void setupRotations(TankmateEntity fish, PoseStack pose, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(fish, pose, bob, yBodyRot, partialTick, scale);
        if (!Features.enabled(Feature.SMOOTH_SWIMMING)) {
            legacyRotations(fish, pose, bob);
            return;
        }
        // M30 part b: a ray lies flat out of the water and swims with its disc (no body sway)
        boolean ray = fish.species().large() != null && fish.species().large().ray();
        if (FishFlop.outOfWater(fish)) {
            if (ray) return;
            pose.translate(0.05F, 0.05F, -0.05F);
            pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
            return;
        }
        var sp = fish.swimPose;
        float g = sp.glass(partialTick);
        float swim = sp.swim(partialTick);
        boolean eel = fish.species().model() == TankmateSpecies.Model.KUHLI || fish.species().model() == TankmateSpecies.Model.MORAY; // M30 part c
        boolean rest = fish.flag(TankmateEntity.FLAG_REST);
        // M21.5: the whole body sways with the beat (an eel hardly: its body wave does that), less when upright on the glass
        float sway = (eel ? 0.4F : rest ? 0.6F : 1.0F + 2.8F * swim) * Mth.sin(sp.phase(partialTick) * (eel ? 1.0F : 1.2F)) * (1 - g);
        if (ray) sway = 0;
        pose.mulPose(Axis.YP.rotationDegrees(sway));
        if (g > 0) {
            // M21.5: turned upright onto the glass smoothly (nose up, belly against the wall in front of it)
            float len = (float) fish.species().length() * fish.lifeStage().scale();
            float toGlass = (float) Math.max(0, fish.sideMargin() - 1.0 / 16 - 0.012);
            pose.translate(0, len * 0.5F * g, -toGlass * g);
            pose.mulPose(Axis.XP.rotationDegrees(90F * g));
        }
        float pitch = sp.pitch(partialTick) * (1 - g);
        if (pitch != 0) pose.mulPose(Axis.XP.rotationDegrees(pitch));
    }

    /** Up to M21: raw last-tick movement, thresholds (features.visuals.smoothSwimming off). */
    private static void legacyRotations(TankmateEntity fish, PoseStack pose, float bob) {
        if (fish.flag(TankmateEntity.FLAG_GLASS) && fish.isInWater()) {
            float len = (float) fish.species().length() * fish.lifeStage().scale();
            float toGlass = (float) Math.max(0, fish.sideMargin() - 1.0 / 16 - 0.012);
            pose.translate(0, len * 0.5F, -toGlass);
            pose.mulPose(Axis.XP.rotationDegrees(90F));
            return;
        }
        double vx = fish.getX() - fish.xo, vz = fish.getZ() - fish.zo, vy = fish.getY() - fish.yo;
        double h = Math.sqrt(vx * vx + vz * vz);
        float swim = (float) Math.min(1.0, h / 0.03);
        boolean rest = fish.flag(TankmateEntity.FLAG_REST);
        float sway = (rest ? 0.6F : 1.0F + 2.8F * swim) * Mth.sin(0.55F * bob + fish.getId());
        pose.mulPose(Axis.YP.rotationDegrees(sway));
        if (fish.isInWater() && Math.abs(vy) > 0.004 && h + Math.abs(vy) > 0.006) {
            float pitch = (float) Mth.clamp(Math.toDegrees(Math.atan2(vy, Math.max(h, 0.01))), -35, 35);
            pose.mulPose(Axis.XP.rotationDegrees(pitch));
        }
        if (FishFlop.outOfWater(fish)) {
            pose.translate(0.05F, 0.05F, -0.05F);
            pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
        }
    }
}
