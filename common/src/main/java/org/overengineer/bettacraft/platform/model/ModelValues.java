package org.overengineer.bettacraft.platform.model;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/** M52: the render data of one block entity: values by {@link ModelKey}; {@link #EMPTY} when there is none. */
public final class ModelValues {
    public static final ModelValues EMPTY = new ModelValues(Map.of());

    private final Map<ModelKey<?>, Object> values;

    private ModelValues(Map<ModelKey<?>, Object> values) {
        this.values = values;
    }

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable T get(ModelKey<T> key) {
        return (T) values.get(key);
    }

    public Map<ModelKey<?>, Object> asMap() {
        return Collections.unmodifiableMap(values);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public static final class Builder {
        private final Map<ModelKey<?>, Object> values = new IdentityHashMap<>();

        public <T> Builder with(ModelKey<T> key, @Nullable T value) {
            if (value != null) values.put(key, value);
            return this;
        }

        public ModelValues build() {
            return values.isEmpty() ? EMPTY : new ModelValues(values);
        }
    }
}
