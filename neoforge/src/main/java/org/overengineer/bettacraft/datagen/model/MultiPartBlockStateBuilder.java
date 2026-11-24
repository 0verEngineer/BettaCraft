package org.overengineer.bettacraft.datagen.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;

/** M52b: a multipart block state file: models that apply when their conditions hold. */
public final class MultiPartBlockStateBuilder {
    private final Block block;
    private final List<PartBuilder> parts = new ArrayList<>();

    MultiPartBlockStateBuilder(Block block) {
        this.block = block;
    }

    public Block getOwner() {
        return block;
    }

    public ConfiguredModel.Builder<PartBuilder> part() {
        return new ConfiguredModel.Builder<>(models -> {
            PartBuilder p = new PartBuilder(models);
            parts.add(p);
            return p;
        }, new ArrayList<>());
    }

    JsonObject toJson() {
        JsonArray a = new JsonArray();
        for (PartBuilder p : parts) a.add(p.toJson());
        JsonObject root = new JsonObject();
        root.add("multipart", a);
        return root;
    }

    public final class PartBuilder {
        private final ConfiguredModel[] models;
        private final Map<Property<?>, List<String>> conditions = new LinkedHashMap<>();
        private boolean useOr;
        /** M65: groups of conditions that all must hold (with {@link #useOr}: one of the groups). */
        private final List<ConditionGroup> groups = new ArrayList<>();

        PartBuilder(ConfiguredModel[] models) {
            this.models = models;
        }

        @SafeVarargs
        public final <T extends Comparable<T>> PartBuilder condition(Property<T> property, T... values) {
            List<String> names = conditions.computeIfAbsent(property, k -> new ArrayList<>());
            for (T v : values) names.add(property.getName(v));
            return this;
        }

        public PartBuilder useOr() {
            this.useOr = true;
            return this;
        }

        public MultiPartBlockStateBuilder end() {
            return MultiPartBlockStateBuilder.this;
        }

        /** M65: a group of conditions (AND), like NeoForge's nested condition groups. */
        public ConditionGroup nestedGroup() {
            ConditionGroup g = new ConditionGroup();
            groups.add(g);
            return g;
        }

        public final class ConditionGroup {
            private final Map<Property<?>, List<String>> conditions = new LinkedHashMap<>();

            @SafeVarargs
            public final <T extends Comparable<T>> ConditionGroup condition(Property<T> property, T... values) {
                List<String> names = conditions.computeIfAbsent(property, k -> new ArrayList<>());
                for (T v : values) names.add(property.getName(v));
                return this;
            }

            public PartBuilder end() {
                return PartBuilder.this;
            }

            JsonObject toJson() {
                JsonObject c = new JsonObject();
                conditions.forEach((p, v) -> c.addProperty(p.getName(), String.join("|", v)));
                return c;
            }
        }

        JsonObject toJson() {
            JsonObject o = new JsonObject();
            o.add("apply", VariantBlockStateBuilder.json(models));
            if (!groups.isEmpty()) {
                JsonArray arr = new JsonArray();
                for (ConditionGroup g : groups) arr.add(g.toJson());
                conditions.forEach((p, v) -> {
                    JsonObject c = new JsonObject();
                    c.addProperty(p.getName(), String.join("|", v));
                    arr.add(c);
                });
                JsonObject w = new JsonObject();
                w.add(useOr ? "OR" : "AND", arr);
                o.add("when", w);
            } else if (!conditions.isEmpty()) {
                JsonObject w = new JsonObject();
                if (useOr) {
                    JsonArray or = new JsonArray();
                    conditions.forEach((p, v) -> {
                        JsonObject c = new JsonObject();
                        c.addProperty(p.getName(), String.join("|", v));
                        or.add(c);
                    });
                    w.add("OR", or);
                } else {
                    conditions.forEach((p, v) -> w.addProperty(p.getName(), String.join("|", v)));
                }
                o.add("when", w);
            }
            return o;
        }
    }
}
