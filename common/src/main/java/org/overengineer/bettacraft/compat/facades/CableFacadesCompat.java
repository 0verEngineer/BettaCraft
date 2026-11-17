package org.overengineer.bettacraft.compat.facades;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M40 part c: the Cable Facades mod (ARR, not a dependency; only its public API class is called, by reflection). Its facades
 * go on the blocks of its block tag {@code cable_facades:supports_facade}, which our data fills with the filter pipes and
 * the pipe bundle (datagen, {@code DataGenM40c}); nothing in its config is changed. Here our pipes are also registered as
 * "hidden when facaded" through {@code CableFacadesAPI}: under a facade the pipe itself is not drawn (its arms reach the
 * faces of the block and would flicker through the facade).
 */
public final class CableFacadesCompat {
    public static final String MOD_ID = "cable_facades";
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/compat");
    private static final String API = "com.portingdeadmods.cable_facades.api.CableFacadesAPI";
    /** Our blocks it may cover (also the block tag of the datagen). */
    public static final List<String> BLOCKS = List.of("bettacraft:filter_inlet_pipe", "bettacraft:filter_outlet_pipe",
            "bettacraft:filter_fresh_pipe", "bettacraft:filter_waste_pipe", "bettacraft:filter_pipe_bundle");

    private CableFacadesCompat() {}

    /** From the mod constructor, only with Cable Facades: its API runs the callback before it reads its block lists. */
    public static void register() {
        try {
            Class<?> api = Class.forName(API);
            Method enqueue = api.getMethod("enqueueAPICallback", Consumer.class);
            Method hidden = api.getMethod("registerHiddenBlocks", String[].class);
            Consumer<Object> callback = instance -> {
                try {
                    hidden.invoke(instance, (Object) BLOCKS.toArray(new String[0]));
                } catch (ReflectiveOperationException e) {
                    LOGGER.warn("Cable Facades: could not register the filter pipes as hidden under facades: {}", e.toString());
                }
            };
            enqueue.invoke(null, callback);
            LOGGER.info("Cable Facades: the filter pipes take its facades (hidden under them)");
        } catch (ReflectiveOperationException | LinkageError e) {
            LOGGER.warn("Cable Facades found, but not its API ({}): its facades still go on the filter pipes by the block tag", e.toString());
        }
    }
}
