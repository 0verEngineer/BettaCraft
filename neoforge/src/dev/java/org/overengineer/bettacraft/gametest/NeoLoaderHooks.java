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
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;

/** M52: the NeoForge side of {@link LoaderHooks}. */
public final class NeoLoaderHooks implements LoaderHooks {
    @Override
    public void prepareRecipeLoader(RecipeManager loader, MinecraftServer server) {
    }

    /** M52e: NeoForge decides the load conditions when it reads the registry: here by hand ({@code neoforge:conditions}). */
    @Override
    public boolean recipeAllowed(JsonElement json, MinecraftServer server) {
        var ops = new net.neoforged.neoforge.common.conditions.ConditionalOps<>(
                server.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), ICondition.IContext.TAGS_INVALID);
        return ICondition.conditionsMatched(ops, json);
    }

    @Override
    public ServerPlayer fakePlayer(ServerLevel level, GameProfile profile) {
        return net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, profile);
    }

    @Override
    public ForeignTank foreignTank(int capacityMb) {
        // M52e: NeoForge 26.3 has no legacy FluidTank; a tank of the transfer API
        var tank = new net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler(1, capacityMb);
        return new ForeignTank() {
            @Override
            public IFluidHandler handler() {
                return org.overengineer.bettacraft.neoforge.transfer.FromResources.fluids(tank);
            }

            @Override
            public int amount() {
                return tank.getAmountAsInt(0);
            }

            @Override
            public boolean holds(Fluid fluid) {
                return !tank.getResource(0).isEmpty() && tank.getResource(0).getFluid() == fluid;
            }

            @Override
            public int simulateFill(Fluid fluid) {
                try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                    return tank.insert(net.neoforged.neoforge.transfer.fluid.FluidResource.of(fluid), 1, tx);
                }
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
