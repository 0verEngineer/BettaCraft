package org.overengineer.bettacraft.neoforge;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;
import org.overengineer.bettacraft.platform.model.ModelKey;
import org.overengineer.bettacraft.platform.model.ModelValues;

/** M52: the common render data ({@link ModelValues}) as NeoForge's model data: one ModelProperty per {@link ModelKey}. */
public final class NeoModelData {
    private static final Map<ModelKey<?>, ModelProperty<?>> PROPS = new ConcurrentHashMap<>();
    private static final Map<ModelProperty<?>, ModelKey<?>> KEYS = new ConcurrentHashMap<>();
    /** The last conversion per thread (a model asks for every side and layer with the same data). */
    private static final ThreadLocal<Object[]> LAST = ThreadLocal.withInitial(() -> new Object[2]);

    private NeoModelData() {}

    @SuppressWarnings("unchecked")
    public static <T> ModelProperty<T> prop(ModelKey<T> key) {
        return (ModelProperty<T>) PROPS.computeIfAbsent(key, k -> {
            ModelProperty<?> p = new ModelProperty<>();
            KEYS.put(p, k);
            return p;
        });
    }

    /** NeoForge's model data as our render values (the properties of our keys). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ModelValues fromNeo(ModelData data) {
        if (data == null || data == ModelData.EMPTY) return ModelValues.EMPTY;
        Object[] last = LAST.get();
        if (last[0] == data) return (ModelValues) last[1];
        ModelValues.Builder b = ModelValues.builder();
        for (ModelProperty<?> p : data.getProperties()) {
            ModelKey key = KEYS.get(p);
            if (key != null) b.with(key, data.get(p));
        }
        ModelValues v = b.build();
        last[0] = data;
        last[1] = v;
        return v;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ModelData toNeo(ModelValues values) {
        if (values.isEmpty()) return ModelData.EMPTY;
        ModelData.Builder b = ModelData.builder();
        for (Map.Entry<ModelKey<?>, Object> e : values.asMap().entrySet()) b.with((ModelProperty) prop(e.getKey()), e.getValue());
        return b.build();
    }
}
