package org.overengineer.bettacraft.neoforge;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.overengineer.bettacraft.platform.model.ModelKey;
import org.overengineer.bettacraft.platform.model.ModelValues;

/** M52: the common render data ({@link ModelValues}) as NeoForge's model data: one ModelProperty per {@link ModelKey}. */
public final class NeoModelData {
    private static final Map<ModelKey<?>, ModelProperty<?>> PROPS = new ConcurrentHashMap<>();

    private NeoModelData() {}

    @SuppressWarnings("unchecked")
    public static <T> ModelProperty<T> prop(ModelKey<T> key) {
        return (ModelProperty<T>) PROPS.computeIfAbsent(key, k -> new ModelProperty<>());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ModelData toNeo(ModelValues values) {
        if (values.isEmpty()) return ModelData.EMPTY;
        ModelData.Builder b = ModelData.builder();
        for (Map.Entry<ModelKey<?>, Object> e : values.asMap().entrySet()) b.with((ModelProperty) prop(e.getKey()), e.getValue());
        return b.build();
    }
}
