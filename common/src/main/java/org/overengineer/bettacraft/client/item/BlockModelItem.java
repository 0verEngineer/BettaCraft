package org.overengineer.bettacraft.client.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.client.model.CabinetBakedModel;
import org.overengineer.bettacraft.client.tank.TankBakedModel;
import org.overengineer.bettacraft.equipment.CabinetBlockItem;
import org.overengineer.bettacraft.platform.client.ClientPlatform;
import org.overengineer.bettacraft.platform.client.DynamicModel;
import org.overengineer.bettacraft.platform.model.ModelValues;
import org.overengineer.bettacraft.registry.ModBlocks;

/**
 * M52b: the item models whose look depends on the stack, which 1.21.1 did with item overrides: the <b>tank</b> (its glass
 * and frame style, quads of {@link TankBakedModel#forItem}) and the <b>cabinet</b> (its planks: the placeholder wood of the
 * JSON model retextured like the placed block, {@link CabinetBakedModel#retexture}). {@code model} is the JSON item model
 * whose display transforms (and, for the cabinet, quads) are used: {@code {"type": "bettacraft:block_model", "kind":
 * "tank", "model": "bettacraft:item/tank"}}.
 */
public final class BlockModelItem implements ItemModel {
    public static final Identifier TYPE = BettaCraft.id("block_model");

    public enum Kind implements StringRepresentable {
        TANK, CABINET;

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private final Kind kind;
    private final List<BakedQuad> baseQuads;
    private final ModelRenderProperties properties;
    /** Per variant (glass + style, planks): its quads; the list is also the GUI cache identity of the variant. */
    private final Map<Object, List<BakedQuad>> variants = new ConcurrentHashMap<>();

    private final org.joml.Matrix4fc transformation;

    private BlockModelItem(Kind kind, List<BakedQuad> baseQuads, ModelRenderProperties properties, org.joml.Matrix4fc transformation) {
        this.transformation = transformation;
        this.kind = kind;
        this.baseQuads = baseQuads;
        this.properties = properties;
    }

    @Override
    public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext context,
                       @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        List<BakedQuad> quads = switch (kind) {
            case TANK -> tankQuads(stack);
            case CABINET -> cabinetQuads(stack);
        };
        state.appendModelIdentityElement(this);
        state.appendModelIdentityElement(quads);
        ItemStackRenderState.LayerRenderState layer = state.newLayer();
        if (stack.hasFoil()) {
            layer.setFoilType(ItemStackRenderState.FoilType.STANDARD);
            state.setAnimated();
        }
        // M52c (26.1): every quad carries its item render type (translucent glass and water, cutout frame)
        layer.setLocalTransform(transformation);
        layer.setExtents(() -> net.minecraft.client.renderer.item.CuboidItemModelWrapper.computeExtents(quads));
        properties.applyToLayer(layer, context);
        layer.prepareQuadList().addAll(quads);
    }

    private List<BakedQuad> tankQuads(ItemStack stack) {
        BlockState block = ModBlocks.TANK.get().defaultBlockState();
        DynamicModel dm = ClientPlatform.get().dynamic(Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(block));
        if (!(dm instanceof TankBakedModel tank)) return baseQuads;
        TankBakedModel item = tank.forItem(stack);
        return variants.computeIfAbsent(item, k -> {
            List<BakedQuad> out = new ArrayList<>();
            RandomSource rand = RandomSource.create(42L);
            for (Direction d : Direction.values()) out.addAll(item.quads(block, d, rand, ModelValues.EMPTY, null));
            out.addAll(item.quads(block, null, rand, ModelValues.EMPTY, null));
            return List.copyOf(out);
        });
    }

    private List<BakedQuad> cabinetQuads(ItemStack stack) {
        Identifier wood = CabinetBlockItem.getWood(stack);
        return variants.computeIfAbsent(wood, k -> CabinetBakedModel.retextureAll(baseQuads, CabinetBlockItem.woodBlock(stack).defaultBlockState()));
    }

    public record Unbaked(Kind kind, Identifier model) implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Kind.CODEC.fieldOf("kind").forGetter(Unbaked::kind),
                Identifier.CODEC.fieldOf("model").forGetter(Unbaked::model)).apply(i, Unbaked::new));

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(model);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, org.joml.Matrix4fc transformation) {
            var baker = context.blockModelBaker();
            var resolved = baker.getModel(model);
            var slots = resolved.getTopTextureSlots();
            List<BakedQuad> quads = kind == Kind.CABINET ? resolved.bakeTopGeometry(slots, baker, BlockModelRotation.IDENTITY).getAll() : List.of();
            return new BlockModelItem(kind, quads, ModelRenderProperties.fromResolvedModel(baker, resolved, slots), transformation);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
