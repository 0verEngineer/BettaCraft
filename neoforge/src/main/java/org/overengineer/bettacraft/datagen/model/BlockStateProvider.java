package org.overengineer.bettacraft.datagen.model;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

/**
 * M52b: the block state files of a mod ({@code assets/<ns>/blockstates}) with the block and item models they need
 * ({@link #models()}, {@link #itemModels()}); see {@link ModelFile}.
 */
public abstract class BlockStateProvider implements DataProvider {
    private final PackOutput output;
    private final String modid;
    private final Map<Block, Object> registered = new LinkedHashMap<>();
    private final BlockModelProvider blockModels;
    private final ItemModelProvider itemModels;

    public BlockStateProvider(PackOutput output, String modid, ExistingFileHelper helper) {
        this.output = output;
        this.modid = modid;
        this.blockModels = new BlockModelProvider(output, modid, helper) {
            @Override
            protected void registerModels() {}
        };
        this.itemModels = new ItemModelProvider(output, modid, helper) {
            @Override
            protected void registerModels() {}
        };
    }

    protected abstract void registerStatesAndModels();

    public BlockModelProvider models() {
        return blockModels;
    }

    public ItemModelProvider itemModels() {
        return itemModels;
    }

    public Identifier modLoc(String name) {
        return Identifier.fromNamespaceAndPath(modid, name);
    }

    public Identifier mcLoc(String name) {
        return Identifier.withDefaultNamespace(name);
    }

    private static Identifier key(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }

    public Identifier blockTexture(Block block) {
        Identifier id = key(block);
        return Identifier.fromNamespaceAndPath(id.getNamespace(), ModelProvider.BLOCK_FOLDER + "/" + id.getPath());
    }

    public ModelFile cubeAll(Block block) {
        return models().cubeAll(key(block).getPath(), blockTexture(block));
    }

    public void simpleBlock(Block block) {
        simpleBlock(block, cubeAll(block));
    }

    public void simpleBlock(Block block, ModelFile model) {
        simpleBlock(block, new ConfiguredModel(model));
    }

    public void simpleBlock(Block block, ConfiguredModel... models) {
        getVariantBuilder(block).partialState().setModels(models);
    }

    public void simpleBlockItem(Block block, ModelFile model) {
        itemModels().getBuilder(key(block).getPath()).parent(model);
    }

    public void simpleBlockWithItem(Block block, ModelFile model) {
        simpleBlock(block, model);
        simpleBlockItem(block, model);
    }

    public VariantBlockStateBuilder getVariantBuilder(Block block) {
        Object b = registered.computeIfAbsent(block, VariantBlockStateBuilder::new);
        if (!(b instanceof VariantBlockStateBuilder v)) throw new IllegalStateException(key(block) + " already has a multipart block state");
        return v;
    }

    public MultiPartBlockStateBuilder getMultipartBuilder(Block block) {
        Object b = registered.computeIfAbsent(block, MultiPartBlockStateBuilder::new);
        if (!(b instanceof MultiPartBlockStateBuilder m)) throw new IllegalStateException(key(block) + " already has variants");
        return m;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        blockModels.generatedModels.clear();
        itemModels.generatedModels.clear();
        registered.clear();
        registerStatesAndModels();
        ItemModelProvider.collect(itemModels);
        List<CompletableFuture<?>> out = new ArrayList<>();
        out.add(blockModels.save(cache));
        out.add(itemModels.save(cache));
        PackOutput.PathProvider paths = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
        registered.forEach((block, b) -> {
            JsonObject json = b instanceof VariantBlockStateBuilder v ? v.toJson() : ((MultiPartBlockStateBuilder) b).toJson();
            out.add(DataProvider.saveStable(cache, json, paths.json(key(block))));
        });
        return CompletableFuture.allOf(out.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Block States: " + modid;
    }
}
