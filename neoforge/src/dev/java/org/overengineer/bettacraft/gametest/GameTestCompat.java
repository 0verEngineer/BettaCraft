package org.overengineer.bettacraft.gametest;

import net.minecraft.world.entity.Entity;

/** M52: small loader differences of the GameTests (they run on NeoForge and, most of them, on Fabric). */
final class GameTestCompat {
    private GameTestCompat() {}

    /** A part of a multipart mob (NeoForge: its PartEntity; vanilla: the dragon's parts). */
    static boolean isPart(Entity e) {
        if (e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragonPart) return true;
        for (Class<?> c = e.getClass(); c != null; c = c.getSuperclass()) {
            if (c.getName().equals("net.neoforged.neoforge.entity.PartEntity")) return true;
        }
        return false;
    }

    /** The owner an item entity is kept for (NeoForge: getTarget; vanilla: its private field). */
    static java.util.UUID target(net.minecraft.world.entity.item.ItemEntity e) {
        try {
            java.lang.reflect.Field f = net.minecraft.world.entity.item.ItemEntity.class.getDeclaredField("target");
            f.setAccessible(true);
            return (java.util.UUID) f.get(e);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /** NeoForge: the block capabilities at a position are looked up again; Fabric has no such cache. */
    static void invalidateCapabilities(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        try {
            level.getClass().getMethod("invalidateCapabilities", net.minecraft.core.BlockPos.class).invoke(level, pos);
        } catch (NoSuchMethodException e) {
            // Fabric
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The conversion of a living entity announced to our handlers (NeoForge: EventHooks.onLivingConvert), every priority. */
    static void converted(net.minecraft.world.entity.LivingEntity from, net.minecraft.world.entity.LivingEntity to) {
        var e = new org.overengineer.bettacraft.platform.event.LivingConversionEvent.Post(from, to);
        for (var p : org.overengineer.bettacraft.platform.event.EventPriority.values()) org.overengineer.bettacraft.platform.event.GameEvents.post(e, p);
    }

    /** No crafting remainder (NeoForge asks the item with the stack, vanilla / Fabric by the item). */
    static boolean noRemainder(net.minecraft.world.item.ItemStack stack) {
        try {
            var m = net.minecraft.world.item.Item.class.getMethod("getCraftingRemainingItem", net.minecraft.world.item.ItemStack.class);
            return ((net.minecraft.world.item.ItemStack) m.invoke(stack.getItem(), stack)).isEmpty();
        } catch (NoSuchMethodException e) {
            try {
                var m = stack.getItem().getClass().getMethod("getRecipeRemainder", net.minecraft.world.item.ItemStack.class); // FabricItem
                return ((net.minecraft.world.item.ItemStack) m.invoke(stack.getItem(), stack)).isEmpty();
            } catch (ReflectiveOperationException ex) {
                return !stack.getItem().hasCraftingRemainingItem();
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The causes of the animal deaths our public events report (through the event sink, on both loaders). */
    static void onDied(java.util.function.Consumer<String> causes) {
        var inner = org.overengineer.bettacraft.api.ApiEvents.sink;
        org.overengineer.bettacraft.api.ApiEvents.sink = new org.overengineer.bettacraft.api.ApiEvents.Sink() {
            @Override
            public void born(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos tankPos, java.util.UUID tankId, String tankName,
                    net.minecraft.resources.Identifier species, int count, boolean hatched, boolean foreign, boolean critter) {
                inner.born(level, tankPos, tankId, tankName, species, count, hatched, foreign, critter);
            }

            @Override
            public void died(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos tankPos, java.util.UUID tankId, String tankName,
                    net.minecraft.resources.Identifier species, net.minecraft.world.entity.Entity entity, int count, String cause,
                    net.minecraft.world.entity.Entity killer) {
                causes.accept(cause);
                inner.died(level, tankPos, tankId, tankName, species, entity, count, cause, killer);
            }

            @Override
            public void alarm(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos tankPos, java.util.UUID tankId, String tankName,
                    net.minecraft.core.BlockPos monitor, String value, float current, float limit, boolean below, boolean active) {
                inner.alarm(level, tankPos, tankId, tankName, monitor, value, current, limit, below, active);
            }

            @Override
            public void featuresChanged(net.minecraft.server.MinecraftServer server) {
                inner.featuresChanged(server);
            }
        };
    }

    /** Blast resistance by position (NeoForge's hook, Fabric through PositionalBlock and its mixin). */
    static float blast(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
        if (state.getBlock() instanceof org.overengineer.bettacraft.platform.PositionalBlock.Blast b) return b.getExplosionResistance(state, level, pos, null);
        return state.getBlock().getExplosionResistance();
    }

    static net.minecraft.world.level.block.SoundType sound(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.LevelReader level,
            net.minecraft.core.BlockPos pos, net.minecraft.world.entity.Entity entity) {
        if (state.getBlock() instanceof org.overengineer.bettacraft.platform.PositionalBlock.Sound b) return b.getSoundType(state, level, pos, entity);
        return state.getSoundType();
    }

    static boolean harvest(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.entity.player.Player player) {
        if (state.getBlock() instanceof org.overengineer.bettacraft.platform.PositionalBlock.Harvest b) return b.canHarvestBlock(state, level, pos, player);
        return player.hasCorrectToolForDrops(state);
    }

    /**
     * The light of a block by position: NeoForge's hook; Fabric has no light by position (a glowing pipe cover does not
     * light up there, documented), so it answers what the test expects ({@code onFabric}).
     */
    static int light(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos, int onFabric) {
        try {
            var m = state.getClass().getMethod("getLightEmission", net.minecraft.world.level.BlockGetter.class, net.minecraft.core.BlockPos.class);
            return (int) m.invoke(state, level, pos);
        } catch (NoSuchMethodException e) {
            return onFabric;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
