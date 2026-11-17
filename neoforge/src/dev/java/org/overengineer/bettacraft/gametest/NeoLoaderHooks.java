package org.overengineer.bettacraft.gametest;

import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;

/** M52: the NeoForge side of {@link LoaderHooks}. */
public final class NeoLoaderHooks implements LoaderHooks {
    @Override
    public void prepareRecipeLoader(RecipeManager loader, MinecraftServer server) {
        loader.injectContext(ICondition.IContext.EMPTY, server.registryAccess());
    }

    @Override
    public boolean recipeAllowed(JsonElement json, MinecraftServer server) {
        return true;
    }

    @Override
    public ServerPlayer fakePlayer(ServerLevel level, GameProfile profile) {
        return net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, profile);
    }

    @Override
    public ForeignTank foreignTank(int capacityMb) {
        FluidTank tank = new FluidTank(capacityMb);
        return new ForeignTank() {
            @Override
            public IFluidHandler handler() {
                return org.overengineer.bettacraft.neoforge.transfer.NeoTransfer.fromNeo(tank);
            }

            @Override
            public int amount() {
                return tank.getFluidAmount();
            }

            @Override
            public boolean holds(Fluid fluid) {
                return tank.getFluid().is(fluid);
            }

            @Override
            public int simulateFill(Fluid fluid) {
                return tank.fill(new net.neoforged.neoforge.fluids.FluidStack(fluid, 1),
                        net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
            }
        };
    }

    @Override
    public Entity[] parts(Entity e) {
        return e.isMultipartEntity() ? e.getParts() : null;
    }

    @Override
    public void playerTickPost(ServerPlayer player) {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
    }
}
