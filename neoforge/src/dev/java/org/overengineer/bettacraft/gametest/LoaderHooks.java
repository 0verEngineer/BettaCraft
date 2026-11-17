package org.overengineer.bettacraft.gametest;

import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.material.Fluid;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;

/**
 * M52: what the shared GameTests need from the loader (NeoForge: NeoLoaderHooks in this source set; Fabric:
 * FabricLoaderHooks in fabric/src/dev), found by name so each loader compiles only its own.
 */
public interface LoaderHooks {
    /** A recipe manager made by hand reads the recipe conditions like a reload does. */
    void prepareRecipeLoader(RecipeManager loader, MinecraftServer server);

    /** Whether a recipe file passes its load conditions (NeoForge decides inside the recipe manager: always true). */
    boolean recipeAllowed(JsonElement json, MinecraftServer server);

    ServerPlayer fakePlayer(ServerLevel level, GameProfile profile);

    /** A fluid tank of the loader's own kind ("another mod's tank"). */
    ForeignTank foreignTank(int capacityMb);

    /** The parts of a multipart entity, or null. */
    Entity[] parts(Entity e);

    /** The end of a player's tick as the loader announces it to other mods (FTB Quests checks its tasks there). */
    void playerTickPost(ServerPlayer player);

    interface ForeignTank {
        IFluidHandler handler();

        int amount();

        boolean holds(Fluid fluid);

        /** Simulated fill of 1 mB of the fluid. */
        int simulateFill(Fluid fluid);
    }

    static LoaderHooks get() {
        return Holder.HOOKS;
    }

    final class Holder {
        private static final LoaderHooks HOOKS = find();

        private Holder() {}

        private static LoaderHooks find() {
            for (String n : new String[]{"org.overengineer.bettacraft.gametest.NeoLoaderHooks", "org.overengineer.bettacraft.gametest.FabricLoaderHooks"}) {
                try {
                    return (LoaderHooks) Class.forName(n).getDeclaredConstructor().newInstance();
                } catch (ClassNotFoundException e) {
                    // the other loader
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            }
            throw new IllegalStateException("no LoaderHooks");
        }
    }
}
