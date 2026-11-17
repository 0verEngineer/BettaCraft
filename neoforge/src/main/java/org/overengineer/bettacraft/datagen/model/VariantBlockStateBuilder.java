package org.overengineer.bettacraft.datagen.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/** M52b: a block state file of variants: a model (or several, weighted) per combination of property values. */
public final class VariantBlockStateBuilder {
    private final Block block;
    private final Map<PartialBlockstate, ConfiguredModel[]> models = new LinkedHashMap<>();

    VariantBlockStateBuilder(Block block) {
        this.block = block;
    }

    public Block getOwner() {
        return block;
    }

    public PartialBlockstate partialState() {
        return new PartialBlockstate(new TreeMap<>(Comparator.comparing(Property::getName)));
    }

    public VariantBlockStateBuilder setModels(PartialBlockstate state, ConfiguredModel... m) {
        models.put(state, m);
        return this;
    }

    public VariantBlockStateBuilder addModels(PartialBlockstate state, ConfiguredModel... m) {
        ConfiguredModel[] had = models.get(state);
        if (had == null) return setModels(state, m);
        ConfiguredModel[] all = Arrays.copyOf(had, had.length + m.length);
        System.arraycopy(m, 0, all, had.length, m.length);
        models.put(state, all);
        return this;
    }

    public VariantBlockStateBuilder forAllStates(Function<BlockState, ConfiguredModel[]> mapper) {
        return forAllStatesExcept(mapper);
    }

    public VariantBlockStateBuilder forAllStatesExcept(Function<BlockState, ConfiguredModel[]> mapper, Property<?>... ignored) {
        Set<Property<?>> skip = Set.of(ignored);
        for (BlockState s : block.getStateDefinition().getPossibleStates()) {
            TreeMap<Property<?>, Comparable<?>> values = new TreeMap<>(Comparator.comparing(Property::getName));
            for (Property<?> p : s.getProperties()) if (!skip.contains(p)) values.put(p, s.getValue(p));
            PartialBlockstate partial = new PartialBlockstate(values);
            if (models.containsKey(partial)) continue;
            models.put(partial, mapper.apply(s));
        }
        return this;
    }

    JsonObject toJson() {
        JsonObject variants = new JsonObject();
        models.forEach((state, m) -> variants.add(state.key(), json(m)));
        JsonObject root = new JsonObject();
        root.add("variants", variants);
        return root;
    }

    static JsonElement json(ConfiguredModel[] m) {
        if (m.length == 1) return m[0].toJson(false);
        JsonArray a = new JsonArray();
        for (ConfiguredModel c : m) a.add(c.toJson(true));
        return a;
    }

    /** A combination of some properties' values (the key of a variant). */
    public final class PartialBlockstate {
        private final TreeMap<Property<?>, Comparable<?>> values;

        PartialBlockstate(TreeMap<Property<?>, Comparable<?>> values) {
            this.values = values;
        }

        public <T extends Comparable<T>> PartialBlockstate with(Property<T> property, T value) {
            TreeMap<Property<?>, Comparable<?>> m = new TreeMap<>(values);
            m.put(property, value);
            return new PartialBlockstate(m);
        }

        public VariantBlockStateBuilder setModels(ConfiguredModel... m) {
            return VariantBlockStateBuilder.this.setModels(this, m);
        }

        public VariantBlockStateBuilder addModels(ConfiguredModel... m) {
            return VariantBlockStateBuilder.this.addModels(this, m);
        }

        public ConfiguredModel.Builder<VariantBlockStateBuilder> modelForState() {
            return new ConfiguredModel.Builder<>(this::setModels, new ArrayList<>());
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        String key() {
            StringBuilder b = new StringBuilder();
            for (Map.Entry<Property<?>, Comparable<?>> e : values.entrySet()) {
                if (!b.isEmpty()) b.append(',');
                b.append(e.getKey().getName()).append('=').append(((Property) e.getKey()).getName(e.getValue()));
            }
            return b.toString();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof PartialBlockstate p && p.key().equals(key());
        }

        @Override
        public int hashCode() {
            return key().hashCode();
        }
    }
}
