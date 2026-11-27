package org.overengineer.bettacraft.fabric;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;

/**
 * M68: the registry aliases of the common registers on Fabric (NeoForge has its own): an old id of a removed block, item or
 * block entity type resolves to its successor wherever a registry looks an id up ({@code mixin.RegistryAliasMixin}): blocks
 * in saved chunks, item stacks in inventories, chests and other mods' storage, block entities. Only ids of our namespace
 * that are no longer registered are aliased, so the lookup of every other id costs one string comparison.
 */
public final class FabricRegistryAliases {
    private static final Map<Identifier, Map<Identifier, Identifier>> BY_REGISTRY = new HashMap<>();
    private static volatile String namespace = "";

    private FabricRegistryAliases() {}

    static synchronized void add(Identifier registry, Identifier from, Identifier to) {
        BY_REGISTRY.computeIfAbsent(registry, k -> new HashMap<>()).put(from, to);
        namespace = from.getNamespace();
    }

    /** The id {@code id} of registry {@code registry} resolves to (itself when it is no alias). */
    public static Identifier resolve(Identifier registry, Identifier id) {
        if (id == null || !id.getNamespace().equals(namespace)) return id;
        Map<Identifier, Identifier> m = BY_REGISTRY.get(registry);
        if (m == null) return id;
        Identifier to = m.get(id);
        return to == null ? id : to;
    }

    /** The key {@code key} resolves to (itself when it is no alias). */
    public static <T> ResourceKey<T> resolve(ResourceKey<T> key) {
        if (key == null || !key.identifier().getNamespace().equals(namespace)) return key;
        Identifier to = resolve(key.registry(), key.identifier());
        return to == key.identifier() ? key : ResourceKey.create(ResourceKey.createRegistryKey(key.registry()), to);
    }
}
