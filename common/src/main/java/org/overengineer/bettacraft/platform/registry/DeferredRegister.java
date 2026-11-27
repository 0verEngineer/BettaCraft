package org.overengineer.bettacraft.platform.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * M52: collects the registry objects of one registry and namespace; the loader registers them (NeoForge: in its
 * RegisterEvent, in the order the registers were {@link #register() handed over}; Fabric: at mod initialisation, registry
 * by registry in the vanilla order, like NeoForge). The loader-independent counterpart of NeoForge's class of the same
 * name, with the methods the mod uses.
 */
public class DeferredRegister<T> {
    /** Every register handed over by {@link #register()}, in that order (read by the loader). */
    private static final List<DeferredRegister<?>> REGISTERED = new ArrayList<>();

    private final ResourceKey<? extends Registry<T>> registryKey;
    private final String namespace;
    private final Map<DeferredHolder<T, ? extends T>, Supplier<? extends T>> entries = new LinkedHashMap<>();
    /** M68: old ids that resolve to an entry of this register (saved worlds, item stacks); from -> to. */
    private final Map<Identifier, Identifier> aliases = new LinkedHashMap<>();
    private boolean handedOver;
    private boolean frozen;

    protected DeferredRegister(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
        this.registryKey = Objects.requireNonNull(registryKey);
        this.namespace = Objects.requireNonNull(namespace);
    }

    public static <T> DeferredRegister<T> create(ResourceKey<? extends Registry<T>> key, String namespace) {
        return new DeferredRegister<>(key, namespace);
    }

    public static <T> DeferredRegister<T> create(Registry<T> registry, String namespace) {
        return new DeferredRegister<>(registry.key(), namespace);
    }

    public static Items createItems(String modid) {
        return new Items(modid);
    }

    public static Blocks createBlocks(String modid) {
        return new Blocks(modid);
    }

    public static DataComponents createDataComponents(String modid) {
        return new DataComponents(Registries.DATA_COMPONENT_TYPE, modid);
    }

    public static DataComponents createDataComponents(ResourceKey<Registry<DataComponentType<?>>> registryKey, String modid) {
        return new DataComponents(registryKey, modid);
    }

    public <I extends T> DeferredHolder<T, I> register(String name, Supplier<? extends I> sup) {
        return register(name, id -> sup.get());
    }

    public <I extends T> DeferredHolder<T, I> register(String name, Function<Identifier, ? extends I> func) {
        if (frozen) throw new IllegalStateException("Cannot register new entries to DeferredRegister after the registry event: " + name);
        Objects.requireNonNull(name);
        Objects.requireNonNull(func);
        Identifier id = Identifier.fromNamespaceAndPath(namespace, name);
        DeferredHolder<T, I> holder = createHolder(registryKey, id);
        // M52b: the factory runs with its id at hand (item / block properties need it, see RegistrationIds)
        if (entries.putIfAbsent(holder, () -> RegistrationIds.with(registryKey, id, () -> func.apply(id))) != null) throw new IllegalArgumentException("Duplicate registration " + name);
        return holder;
    }

    protected <I extends T> DeferredHolder<T, I> createHolder(ResourceKey<? extends Registry<T>> registryKey, Identifier id) {
        return DeferredHolder.create(registryKey, id);
    }

    /**
     * M68: an old id ({@code from}, no longer registered) that resolves to {@code to} wherever the game looks an id up: blocks
     * in saved chunks, item stacks in inventories and containers, block entity types (NeoForge: its registry alias; Fabric: a
     * mixin on the id lookups of the registries, {@code RegistryAliasMixin}). Both in this register's namespace.
     */
    public void addAlias(String from, String to) {
        if (frozen) throw new IllegalStateException("Cannot add aliases after the registry event: " + from);
        aliases.put(Identifier.fromNamespaceAndPath(namespace, from), Identifier.fromNamespaceAndPath(namespace, to));
    }

    /** M68: the aliases of this register (from -> to). */
    public Map<Identifier, Identifier> aliases() {
        return Collections.unmodifiableMap(aliases);
    }

    /** Hands this register over to the loader (instead of NeoForge's register(IEventBus)); repeated calls do nothing. */
    public void register() {
        if (handedOver) return;
        handedOver = true;
        synchronized (REGISTERED) {
            REGISTERED.add(this);
        }
    }

    public Collection<DeferredHolder<T, ? extends T>> getEntries() {
        return Collections.unmodifiableSet(entries.keySet());
    }

    public ResourceKey<? extends Registry<T>> getRegistryKey() {
        return registryKey;
    }

    public Identifier getRegistryName() {
        return registryKey.identifier();
    }

    public String getNamespace() {
        return namespace;
    }

    // ---- for the loader's platform layer

    /** Every register handed over so far, in that order. */
    public static List<DeferredRegister<?>> registered() {
        synchronized (REGISTERED) {
            return List.copyOf(REGISTERED);
        }
    }

    /** The entries in registration order: id and the factory that makes the object. Freezes the register. */
    public Map<Identifier, Supplier<? extends T>> takeEntries() {
        frozen = true;
        Map<Identifier, Supplier<? extends T>> out = new LinkedHashMap<>();
        entries.forEach((h, s) -> out.put(h.getId(), s));
        return out;
    }

    /** Items: register and the shortcuts for simple items and block items. */
    public static class Items extends DeferredRegister<Item> {
        protected Items(String namespace) {
            super(Registries.ITEM, namespace);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <I extends Item> DeferredItem<I> register(String name, Function<Identifier, ? extends I> func) {
            return (DeferredItem<I>) super.register(name, func);
        }

        @Override
        public <I extends Item> DeferredItem<I> register(String name, Supplier<? extends I> sup) {
            return register(name, id -> sup.get());
        }

        @Override
        protected <I extends Item> DeferredHolder<Item, I> createHolder(ResourceKey<? extends Registry<Item>> registryKey, Identifier id) {
            return DeferredItem.createItem(id);
        }

        public DeferredItem<BlockItem> registerSimpleBlockItem(String name, Supplier<? extends Block> block, Item.Properties properties) {
            return register(name, id -> new BlockItem(block.get(), properties));
        }

        public DeferredItem<BlockItem> registerSimpleBlockItem(String name, Supplier<? extends Block> block) {
            return registerSimpleBlockItem(name, block, new Item.Properties());
        }

        public DeferredItem<BlockItem> registerSimpleBlockItem(Holder<Block> block, Item.Properties properties) {
            return registerSimpleBlockItem(block.unwrapKey().orElseThrow().identifier().getPath(), block::value, properties);
        }

        public DeferredItem<BlockItem> registerSimpleBlockItem(Holder<Block> block) {
            return registerSimpleBlockItem(block, new Item.Properties());
        }

        public <I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, ? extends I> func, Item.Properties props) {
            return register(name, id -> func.apply(props));
        }

        public <I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, ? extends I> func) {
            return registerItem(name, func, new Item.Properties());
        }

        public DeferredItem<Item> registerSimpleItem(String name, Item.Properties props) {
            return registerItem(name, Item::new, props);
        }

        public DeferredItem<Item> registerSimpleItem(String name) {
            return registerItem(name, Item::new, new Item.Properties());
        }
    }

    /** Blocks: register and the shortcuts for simple blocks. */
    public static class Blocks extends DeferredRegister<Block> {
        protected Blocks(String namespace) {
            super(Registries.BLOCK, namespace);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <B extends Block> DeferredBlock<B> register(String name, Function<Identifier, ? extends B> func) {
            return (DeferredBlock<B>) super.register(name, func);
        }

        @Override
        public <B extends Block> DeferredBlock<B> register(String name, Supplier<? extends B> sup) {
            return register(name, id -> sup.get());
        }

        @Override
        protected <I extends Block> DeferredHolder<Block, I> createHolder(ResourceKey<? extends Registry<Block>> registryKey, Identifier id) {
            return DeferredBlock.createBlock(id);
        }

        public <B extends Block> DeferredBlock<B> registerBlock(String name, Function<BlockBehaviour.Properties, ? extends B> func, BlockBehaviour.Properties props) {
            return register(name, id -> func.apply(props));
        }

        public <B extends Block> DeferredBlock<B> registerBlock(String name, Function<BlockBehaviour.Properties, ? extends B> func) {
            return registerBlock(name, func, BlockBehaviour.Properties.of());
        }

        public DeferredBlock<Block> registerSimpleBlock(String name, BlockBehaviour.Properties props) {
            return registerBlock(name, Block::new, props);
        }

        public DeferredBlock<Block> registerSimpleBlock(String name) {
            return registerSimpleBlock(name, BlockBehaviour.Properties.of());
        }
    }

    /** Data component types. */
    public static class DataComponents extends DeferredRegister<DataComponentType<?>> {
        protected DataComponents(ResourceKey<Registry<DataComponentType<?>>> registryKey, String namespace) {
            super(registryKey, namespace);
        }

        public <D> DeferredHolder<DataComponentType<?>, DataComponentType<D>> registerComponentType(String name,
                UnaryOperator<DataComponentType.Builder<D>> builder) {
            return register(name, () -> builder.apply(DataComponentType.builder()).build());
        }
    }
}
