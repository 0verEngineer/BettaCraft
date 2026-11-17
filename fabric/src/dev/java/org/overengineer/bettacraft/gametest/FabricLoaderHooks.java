package org.overengineer.bettacraft.gametest;

import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.material.Fluid;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;

/** M52: the Fabric side of {@link LoaderHooks}. */
public final class FabricLoaderHooks implements LoaderHooks {
    @Override
    public void prepareRecipeLoader(RecipeManager loader, MinecraftServer server) {
    }

    /** Fabric decides the load conditions when it reads the files: here by hand ({@code fabric:load_conditions}). */
    @Override
    public boolean recipeAllowed(JsonElement json, MinecraftServer server) {
        if (!json.isJsonObject() || !json.getAsJsonObject().has(ResourceConditions_KEY)) return true;
        var parsed = ResourceCondition.LIST_CODEC.parse(JsonOps.INSTANCE, json.getAsJsonObject().get(ResourceConditions_KEY));
        var list = parsed.getOrThrow(msg -> new IllegalStateException("bad load conditions: " + msg));
        for (ResourceCondition c : list) if (!c.test(server.registryAccess())) return false;
        return true;
    }

    private static final String ResourceConditions_KEY = "fabric:load_conditions";

    @Override
    public ServerPlayer fakePlayer(ServerLevel level, GameProfile profile) {
        return net.fabricmc.fabric.api.entity.FakePlayer.get(level, profile);
    }

    @Override
    public ForeignTank foreignTank(int capacityMb) {
        SingleVariantStorage<FluidVariant> tank = new SingleVariantStorage<>() {
            @Override
            protected FluidVariant getBlankVariant() {
                return FluidVariant.blank();
            }

            @Override
            protected long getCapacity(FluidVariant variant) {
                return (long) capacityMb * FluidConstants.BUCKET / 1000;
            }
        };
        return new ForeignTank() {
            @Override
            public IFluidHandler handler() {
                return org.overengineer.bettacraft.fabric.transfer.FabricTransfer.fromFabric(tank);
            }

            @Override
            public int amount() {
                return (int) (tank.amount * 1000 / FluidConstants.BUCKET);
            }

            @Override
            public boolean holds(Fluid fluid) {
                return !tank.isResourceBlank() && tank.variant.isOf(fluid);
            }

            @Override
            public int simulateFill(Fluid fluid) {
                try (Transaction t = Transaction.openOuter()) {
                    return (int) (tank.insert(FluidVariant.of(fluid), FluidConstants.BUCKET / 1000, t) * 1000 / FluidConstants.BUCKET);
                }
            }
        };
    }

    @Override
    public Entity[] parts(Entity e) {
        return e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon d ? d.getSubEntities() : null;
    }

    /** FTB Quests (Architectury) hears the end of a player's tick through Architectury's event. */
    @Override
    public void playerTickPost(ServerPlayer player) {
        try {
            Class<?> tick = Class.forName("dev.architectury.event.events.common.TickEvent");
            Object event = tick.getField("PLAYER_POST").get(null);
            // through the public interfaces (the implementations are package private)
            Object invoker = Class.forName("dev.architectury.event.Event").getMethod("invoker").invoke(event);
            tick.getMethod("tick", Object.class)
                    .invoke(invoker, player);
        } catch (ClassNotFoundException e) {
            // no Architectury: nobody listens
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
