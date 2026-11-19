package org.overengineer.bettacraft.fabric.dev.mixin;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;
import org.overengineer.bettacraft.fabric.dev.FabricGameTests;
import org.overengineer.bettacraft.gametest.framework.BettaGameTests;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * M52b (dev): our test environments and instances join the dynamic registries where Fabric's GameTest module adds its own
 * (while the registries of a world with test instances are loaded, before they are frozen).
 */
@Pseudo
@Mixin(targets = "net.fabricmc.fabric.impl.gametest.FabricGameTestModInitializer", remap = false)
public abstract class FabricGameTestIdsMixin {
    @Inject(method = "registerDynamicEntries", at = @At("TAIL"), remap = false)
    @SuppressWarnings("unchecked")
    private static void bettacraft$ours(List<?> loaders, CallbackInfo ci) {
        if (!FabricGameTests.enabled()) return;
        Map<ResourceKey<?>, WritableRegistry<?>> registries = new IdentityHashMap<>();
        for (Object loader : loaders) {
            WritableRegistry<?> r = registry(loader);
            registries.put(r.key(), r);
        }
        WritableRegistry<TestEnvironmentDefinition<?>> envs = (WritableRegistry<TestEnvironmentDefinition<?>>) registries.get(Registries.TEST_ENVIRONMENT);
        WritableRegistry<GameTestInstance> instances = (WritableRegistry<GameTestInstance>) registries.get(Registries.TEST_INSTANCE);
        BettaGameTests.registerInstances(
                (id, env) -> (Holder<TestEnvironmentDefinition<?>>) envs.register(ResourceKey.create(Registries.TEST_ENVIRONMENT, id), env, RegistrationInfo.BUILT_IN),
                (id, test) -> Registry.register(instances, ResourceKey.create(Registries.TEST_INSTANCE, id), test));
    }

    /**
     * The registry of one of the load tasks of {@code RegistryDataLoader} (M52c, 26.1: {@code RegistryLoadTask}, its field
     * {@code registry}; 1.21.x: a record with a registry component).
     */
    private static WritableRegistry<?> registry(Object loader) {
        try {
            for (Class<?> c = loader.getClass(); c != null; c = c.getSuperclass()) {
                for (var f : c.getDeclaredFields()) {
                    if (WritableRegistry.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        return (WritableRegistry<?>) f.get(loader);
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot read the registry of " + loader, e);
        }
        throw new IllegalStateException("no registry in " + loader);
    }
}
