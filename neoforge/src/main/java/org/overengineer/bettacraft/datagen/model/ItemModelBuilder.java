package org.overengineer.bettacraft.datagen.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

/**
 * M52b: an item model being generated (see {@link ModelFile}). Its overrides (1.21.1: a model by an item property) no
 * longer go into the model: {@link ItemModelProvider} turns them into the 1.21.4 item model definition
 * ({@code items/<id>.json}, a condition on {@code bettacraft:property}).
 */
public class ItemModelBuilder extends ModelBuilder<ItemModelBuilder> {
    final List<OverrideBuilder> overrides = new ArrayList<>();

    public ItemModelBuilder(Identifier location, ExistingFileHelper helper) {
        super(location);
    }

    public OverrideBuilder override() {
        OverrideBuilder o = new OverrideBuilder();
        overrides.add(o);
        return o;
    }

    public OverrideBuilder override(int index) {
        return overrides.get(index);
    }

    public class OverrideBuilder {
        final Map<Identifier, Float> predicates = new LinkedHashMap<>();
        ModelFile model;

        public OverrideBuilder predicate(Identifier key, float value) {
            predicates.put(key, value);
            return this;
        }

        public OverrideBuilder model(ModelFile model) {
            this.model = model;
            return this;
        }

        public ItemModelBuilder end() {
            return ItemModelBuilder.this;
        }
    }
}
