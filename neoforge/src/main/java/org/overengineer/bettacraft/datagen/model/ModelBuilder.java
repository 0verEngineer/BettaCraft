package org.overengineer.bettacraft.datagen.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;

/**
 * M52b: a block or item model being generated (see {@link ModelFile}): parent, textures, render type, ambient occlusion,
 * display transforms and elements with their faces, written as model JSON by {@link #toJson}.
 */
public class ModelBuilder<T extends ModelBuilder<T>> extends ModelFile {
    protected @Nullable ModelFile parent;
    protected final Map<String, String> textures = new LinkedHashMap<>();
    protected @Nullable Identifier renderType;
    protected @Nullable Boolean ambientOcclusion;
    protected @Nullable String guiLight;
    protected final List<ElementBuilder> elements = new ArrayList<>();
    protected final TransformsBuilder transforms = new TransformsBuilder();

    protected ModelBuilder(Identifier location) {
        super(location);
    }

    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
    }

    public T parent(ModelFile parent) {
        this.parent = parent;
        return self();
    }

    public T texture(String key, String texture) {
        textures.put(key, texture.startsWith("#") ? texture : Identifier.parse(texture).toString());
        return self();
    }

    public T texture(String key, Identifier texture) {
        textures.put(key, texture.toString());
        return self();
    }

    public T renderType(String renderType) {
        return renderType(Identifier.parse(renderType));
    }

    public T renderType(Identifier renderType) {
        this.renderType = renderType;
        return self();
    }

    public T ao(boolean ao) {
        this.ambientOcclusion = ao;
        return self();
    }

    /** "front" or "side" (the light of the model in the inventory). */
    public T guiLight(String light) {
        this.guiLight = light;
        return self();
    }

    public TransformsBuilder transforms() {
        return transforms;
    }

    public ElementBuilder element() {
        ElementBuilder e = new ElementBuilder();
        elements.add(e);
        return e;
    }

    /** The {@code index}-th element made so far. */
    public ElementBuilder element(int index) {
        return elements.get(index);
    }

    /** A number as NeoForge's generators wrote it: whole numbers without a fraction. */
    static JsonPrimitive num(float f) {
        if (f == (int) f) return new JsonPrimitive((int) f);
        return new JsonPrimitive(f);
    }

    static JsonArray vec(float x, float y, float z) {
        JsonArray a = new JsonArray();
        a.add(num(x));
        a.add(num(y));
        a.add(num(z));
        return a;
    }

    /**
     * M52b: the chunk layer names of 1.21.11: {@code cutout_mipped} was merged into {@code cutout} (1.21.11 draws cutout
     * blocks with mipmaps).
     */
    static String renderTypeName(Identifier id) {
        if (id.getNamespace().equals("minecraft") && id.getPath().equals("cutout_mipped")) return "minecraft:cutout";
        return id.toString();
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        if (parent != null) root.addProperty("parent", parent.getLocation().toString());
        if (ambientOcclusion != null && !ambientOcclusion) root.addProperty("ambientocclusion", false);
        if (guiLight != null) root.addProperty("gui_light", guiLight);
        if (renderType != null) root.addProperty("render_type", renderTypeName(renderType));
        JsonObject display = transforms.toJson();
        if (!display.isEmpty()) root.add("display", display);
        if (!textures.isEmpty()) {
            JsonObject t = new JsonObject();
            textures.forEach(t::addProperty);
            root.add("textures", t);
        }
        if (!elements.isEmpty()) {
            JsonArray a = new JsonArray();
            for (ElementBuilder e : elements) a.add(e.toJson());
            root.add("elements", a);
        }
        extraJson(root);
        return root;
    }

    /** Subclasses add their own keys. */
    protected void extraJson(JsonObject root) {}

    // ------------------------------------------------------------------ elements

    public class ElementBuilder {
        private float fx, fy, fz, tx = 16, ty = 16, tz = 16;
        private boolean shade = true;
        private final Map<Direction, FaceBuilder> faces = new EnumMap<>(Direction.class);
        private @Nullable RotationBuilder rotation;

        public ElementBuilder from(float x, float y, float z) {
            fx = x;
            fy = y;
            fz = z;
            return this;
        }

        public ElementBuilder to(float x, float y, float z) {
            tx = x;
            ty = y;
            tz = z;
            return this;
        }

        public ElementBuilder shade(boolean shade) {
            this.shade = shade;
            return this;
        }

        public FaceBuilder face(Direction d) {
            return faces.computeIfAbsent(d, FaceBuilder::new);
        }

        public ElementBuilder allFaces(BiConsumer<Direction, FaceBuilder> action) {
            for (Direction d : Direction.values()) action.accept(d, face(d));
            return this;
        }

        public ElementBuilder allFacesExcept(BiConsumer<Direction, FaceBuilder> action, Set<Direction> except) {
            for (Direction d : Direction.values()) if (!except.contains(d)) action.accept(d, face(d));
            return this;
        }

        /** Every face with its texture (and the uvs of a full cube). */
        public ElementBuilder textureAll(String texture) {
            return allFaces((d, f) -> f.texture(texture));
        }

        public RotationBuilder rotation() {
            if (rotation == null) rotation = new RotationBuilder();
            return rotation;
        }

        public T end() {
            return self();
        }

        JsonObject toJson() {
            JsonObject o = new JsonObject();
            o.add("from", vec(fx, fy, fz));
            o.add("to", vec(tx, ty, tz));
            if (rotation != null) o.add("rotation", rotation.toJson());
            if (!shade) o.addProperty("shade", false);
            JsonObject f = new JsonObject();
            faces.forEach((d, face) -> f.add(d.getSerializedName(), face.toJson()));
            o.add("faces", f);
            return o;
        }

        public class FaceBuilder {
            private final Direction direction;
            private @Nullable String texture;
            private float @Nullable [] uvs;
            private @Nullable Direction cullface;
            private int tintindex = -1;
            private int rotation;
            private int lightEmission;

            FaceBuilder(Direction direction) {
                this.direction = direction;
            }

            public FaceBuilder texture(String texture) {
                this.texture = texture;
                return this;
            }

            public FaceBuilder uvs(float u0, float v0, float u1, float v1) {
                this.uvs = new float[]{u0, v0, u1, v1};
                return this;
            }

            public FaceBuilder cullface(@Nullable Direction side) {
                this.cullface = side;
                return this;
            }

            public FaceBuilder tintindex(int index) {
                this.tintindex = index;
                return this;
            }

            /** The texture rotation in degrees (0, 90, 180, 270). */
            public FaceBuilder rotation(int degrees) {
                this.rotation = degrees;
                return this;
            }

            /**
             * Full-bright faces (1.21.1: block and sky light of NeoForge's face data; M52b: NeoForge 21.5+ has one light
             * emission per face, the brighter of the two).
             */
            public FaceBuilder emissivity(int blockLight, int skyLight) {
                this.lightEmission = Math.max(blockLight, skyLight);
                return this;
            }

            public ElementBuilder end() {
                return ElementBuilder.this;
            }

            JsonObject toJson() {
                JsonObject o = new JsonObject();
                o.addProperty("texture", texture == null ? "#missing" : texture);
                if (uvs != null) {
                    JsonArray a = new JsonArray();
                    for (float u : uvs) a.add(num(u));
                    o.add("uv", a);
                }
                if (cullface != null) o.addProperty("cullface", cullface.getSerializedName());
                if (tintindex != -1) o.addProperty("tintindex", tintindex);
                if (rotation != 0) o.addProperty("rotation", rotation);
                if (lightEmission > 0) {
                    JsonObject n = new JsonObject();
                    n.addProperty("light_emission", lightEmission);
                    o.add("neoforge_data", n);
                }
                return o;
            }
        }

        public class RotationBuilder {
            private float ox = 8, oy = 8, oz = 8;
            private Direction.Axis axis = Direction.Axis.Y;
            private float angle;
            private boolean rescale;

            public RotationBuilder origin(float x, float y, float z) {
                ox = x;
                oy = y;
                oz = z;
                return this;
            }

            public RotationBuilder axis(Direction.Axis axis) {
                this.axis = axis;
                return this;
            }

            public RotationBuilder angle(float angle) {
                this.angle = angle;
                return this;
            }

            public RotationBuilder rescale(boolean rescale) {
                this.rescale = rescale;
                return this;
            }

            public ElementBuilder end() {
                return ElementBuilder.this;
            }

            JsonObject toJson() {
                JsonObject o = new JsonObject();
                o.add("origin", vec(ox, oy, oz));
                o.addProperty("axis", axis.getSerializedName());
                o.addProperty("angle", angle);
                o.addProperty("rescale", rescale);
                return o;
            }
        }
    }

    // ------------------------------------------------------------------ display transforms

    public class TransformsBuilder {
        private final Map<ItemDisplayContext, TransformVecBuilder> transforms = new LinkedHashMap<>();

        public TransformVecBuilder transform(ItemDisplayContext context) {
            return transforms.computeIfAbsent(context, k -> new TransformVecBuilder());
        }

        public T end() {
            return self();
        }

        JsonObject toJson() {
            JsonObject o = new JsonObject();
            transforms.forEach((c, t) -> o.add(c.getSerializedName(), t.toJson()));
            return o;
        }

        public class TransformVecBuilder {
            private final float[] rotation = {0, 0, 0}, translation = {0, 0, 0}, scale = {1, 1, 1};

            public TransformVecBuilder rotation(float x, float y, float z) {
                rotation[0] = x;
                rotation[1] = y;
                rotation[2] = z;
                return this;
            }

            public TransformVecBuilder leftRotation(float x, float y, float z) {
                return rotation(x, y, z);
            }

            public TransformVecBuilder translation(float x, float y, float z) {
                translation[0] = x;
                translation[1] = y;
                translation[2] = z;
                return this;
            }

            public TransformVecBuilder scale(float s) {
                return scale(s, s, s);
            }

            public TransformVecBuilder scale(float x, float y, float z) {
                scale[0] = x;
                scale[1] = y;
                scale[2] = z;
                return this;
            }

            public TransformsBuilder end() {
                return TransformsBuilder.this;
            }

            JsonElement toJson() {
                JsonObject o = new JsonObject();
                if (rotation[0] != 0 || rotation[1] != 0 || rotation[2] != 0) o.add("rotation", vec(rotation[0], rotation[1], rotation[2]));
                if (translation[0] != 0 || translation[1] != 0 || translation[2] != 0) o.add("translation", vec(translation[0], translation[1], translation[2]));
                if (scale[0] != 1 || scale[1] != 1 || scale[2] != 1) o.add("scale", vec(scale[0], scale[1], scale[2]));
                return o;
            }
        }
    }
}
