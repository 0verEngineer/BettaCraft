package org.overengineer.bettacraft.client.tank;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import org.overengineer.bettacraft.BettaCraft;

/** Geometry loader {@code bettacraft:tank}: model JSON needs textures {@code edge} and {@code particle}. */
public final class TankModelLoader implements IGeometryLoader<TankModelLoader.Geometry> {
    public static final net.minecraft.resources.ResourceLocation ID = BettaCraft.id("tank");
    public static final TankModelLoader INSTANCE = new TankModelLoader();

    @Override
    public Geometry read(JsonObject json, JsonDeserializationContext ctx) {
        return new Geometry();
    }

    public static final class Geometry implements IUnbakedGeometry<Geometry> {
        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter,
                               ModelState modelState, ItemOverrides overrides) {
            TextureAtlasSprite edge = spriteGetter.apply(context.getMaterial("edge"));
            TextureAtlasSprite particle = spriteGetter.apply(context.getMaterial("particle"));
            return new TankBakedModel(edge, particle, context.getTransforms(), null, null);
        }
    }
}
