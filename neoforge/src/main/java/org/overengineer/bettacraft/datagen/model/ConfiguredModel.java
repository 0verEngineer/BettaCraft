package org.overengineer.bettacraft.datagen.model;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** M52b: a model of a block state variant or multipart case: the model, its rotation, uv lock and weight. */
public final class ConfiguredModel {
    public static final int DEFAULT_WEIGHT = 1;
    public final ModelFile model;
    public final int rotationX, rotationY;
    public final boolean uvLock;
    public final int weight;

    public ConfiguredModel(ModelFile model, int rotationX, int rotationY, boolean uvLock, int weight) {
        this.model = model;
        this.rotationX = rotationX;
        this.rotationY = rotationY;
        this.uvLock = uvLock;
        this.weight = weight;
    }

    public ConfiguredModel(ModelFile model, int rotationX, int rotationY, boolean uvLock) {
        this(model, rotationX, rotationY, uvLock, DEFAULT_WEIGHT);
    }

    public ConfiguredModel(ModelFile model) {
        this(model, 0, 0, false);
    }

    public static Builder<?> builder() {
        return new Builder<>(null, new ArrayList<>());
    }

    JsonObject toJson(boolean withWeight) {
        JsonObject o = new JsonObject();
        o.addProperty("model", model.getLocation().toString());
        if (rotationX != 0) o.addProperty("x", rotationX);
        if (rotationY != 0) o.addProperty("y", rotationY);
        if (uvLock) o.addProperty("uvlock", true);
        if (withWeight && weight != DEFAULT_WEIGHT) o.addProperty("weight", weight);
        return o;
    }

    /** Builds one or more models; {@code owner} takes them on {@link #addModel} (a partial state or a multipart case). */
    public static class Builder<T> {
        private final Function<ConfiguredModel[], T> owner;
        private final List<ConfiguredModel> done;
        private ModelFile model;
        private int rotationX, rotationY, weight = DEFAULT_WEIGHT;
        private boolean uvLock;

        Builder(Function<ConfiguredModel[], T> owner, List<ConfiguredModel> done) {
            this.owner = owner;
            this.done = done;
        }

        public Builder<T> modelFile(ModelFile model) {
            this.model = model;
            return this;
        }

        public Builder<T> rotationX(int x) {
            this.rotationX = x;
            return this;
        }

        public Builder<T> rotationY(int y) {
            this.rotationY = y;
            return this;
        }

        public Builder<T> uvLock(boolean lock) {
            this.uvLock = lock;
            return this;
        }

        public Builder<T> weight(int weight) {
            this.weight = weight;
            return this;
        }

        public ConfiguredModel buildLast() {
            if (model == null) throw new IllegalStateException("no model");
            return new ConfiguredModel(model, rotationX, rotationY, uvLock, weight);
        }

        public ConfiguredModel[] build() {
            List<ConfiguredModel> all = new ArrayList<>(done);
            all.add(buildLast());
            return all.toArray(new ConfiguredModel[0]);
        }

        public Builder<T> nextModel() {
            List<ConfiguredModel> all = new ArrayList<>(done);
            all.add(buildLast());
            return new Builder<>(owner, all);
        }

        public T addModel() {
            if (owner == null) throw new IllegalStateException("nothing takes these models");
            return owner.apply(build());
        }
    }
}
