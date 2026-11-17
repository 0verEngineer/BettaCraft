package org.overengineer.bettacraft.datagen.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

/**
 * M52b: generates the models of one folder ({@code block} or {@code item}) of a mod: builders by path, the usual vanilla
 * parents (cube, cube_all, cube_column, cross, orientable) and the JSON files ({@code assets/<ns>/models/...}).
 */
public abstract class ModelProvider<T extends ModelBuilder<T>> implements DataProvider {
    public static final String BLOCK_FOLDER = "block";
    public static final String ITEM_FOLDER = "item";

    protected final PackOutput output;
    protected final String modid;
    protected final String folder;
    protected final Function<Identifier, T> factory;
    protected final ExistingFileHelper existingFileHelper;
    public final Map<Identifier, T> generatedModels = new LinkedHashMap<>();

    protected ModelProvider(PackOutput output, String modid, String folder, Function<Identifier, T> factory, ExistingFileHelper helper) {
        this.output = output;
        this.modid = modid;
        this.folder = folder;
        this.factory = factory;
        this.existingFileHelper = helper;
    }

    protected abstract void registerModels();

    private Identifier extendWithFolder(Identifier id) {
        return id.getPath().contains("/") ? id : id.withPrefix(folder + "/");
    }

    public T getBuilder(String path) {
        Identifier id = extendWithFolder(path.contains(":") ? Identifier.parse(path) : Identifier.fromNamespaceAndPath(modid, path));
        return generatedModels.computeIfAbsent(id, factory);
    }

    public Identifier modLoc(String name) {
        return Identifier.fromNamespaceAndPath(modid, name);
    }

    public Identifier mcLoc(String name) {
        return Identifier.withDefaultNamespace(name);
    }

    public T withExistingParent(String name, String parent) {
        return withExistingParent(name, mcLoc(parent));
    }

    public T withExistingParent(String name, Identifier parent) {
        return getBuilder(name).parent(getExistingFile(parent));
    }

    public ModelFile.ExistingModelFile getExistingFile(Identifier path) {
        return new ModelFile.ExistingModelFile(extendWithFolder(path), existingFileHelper);
    }

    public T cube(String name, Identifier down, Identifier up, Identifier north, Identifier south, Identifier east, Identifier west) {
        return withExistingParent(name, BLOCK_FOLDER + "/cube").texture("down", down).texture("up", up).texture("north", north)
                .texture("south", south).texture("east", east).texture("west", west);
    }

    public T singleTexture(String name, Identifier parent, Identifier texture) {
        return singleTexture(name, parent, "texture", texture);
    }

    public T singleTexture(String name, Identifier parent, String key, Identifier texture) {
        return withExistingParent(name, parent).texture(key, texture);
    }

    public T cubeAll(String name, Identifier texture) {
        return singleTexture(name, mcLoc(BLOCK_FOLDER + "/cube_all"), "all", texture);
    }

    public T cubeColumn(String name, Identifier side, Identifier end) {
        return withExistingParent(name, BLOCK_FOLDER + "/cube_column").texture("side", side).texture("end", end);
    }

    public T cross(String name, Identifier cross) {
        return singleTexture(name, mcLoc(BLOCK_FOLDER + "/cross"), "cross", cross);
    }

    public T orientable(String name, Identifier side, Identifier front, Identifier top) {
        return withExistingParent(name, BLOCK_FOLDER + "/orientable").texture("side", side).texture("front", front).texture("top", top);
    }

    public T carpet(String name, Identifier wool) {
        return singleTexture(name, mcLoc(BLOCK_FOLDER + "/carpet"), "wool", wool);
    }

    /** Writes the generated models (also used by {@link BlockStateProvider} for its own two model sets). */
    CompletableFuture<?> save(CachedOutput cache) {
        List<CompletableFuture<?>> out = new ArrayList<>();
        PackOutput.PathProvider paths = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
        for (T m : generatedModels.values()) out.add(DataProvider.saveStable(cache, m.toJson(), paths.json(m.getLocation())));
        return CompletableFuture.allOf(out.toArray(CompletableFuture[]::new));
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        generatedModels.clear();
        registerModels();
        ItemModelProvider.collect(this);
        return save(cache);
    }
}
