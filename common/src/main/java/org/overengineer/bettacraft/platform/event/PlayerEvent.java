package org.overengineer.bettacraft.platform.event;

import org.jetbrains.annotations.Nullable;

/** M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). */
public final class PlayerEvent {
    private PlayerEvent() {}

    public static class PlayerLoggedInEvent extends GameEvent {
        private final net.minecraft.world.entity.player.Player entity;

        public PlayerLoggedInEvent(net.minecraft.world.entity.player.Player entity) {
            this.entity = entity;
        }

        public net.minecraft.world.entity.player.Player getEntity() {
            return entity;
        }
    }

    public static class PlayerLoggedOutEvent extends GameEvent {
        private final net.minecraft.world.entity.player.Player entity;

        public PlayerLoggedOutEvent(net.minecraft.world.entity.player.Player entity) {
            this.entity = entity;
        }

        public net.minecraft.world.entity.player.Player getEntity() {
            return entity;
        }
    }

    public static class ItemCraftedEvent extends GameEvent {
        private final net.minecraft.world.entity.player.Player entity;
        private final net.minecraft.world.item.ItemStack crafting;

        public ItemCraftedEvent(net.minecraft.world.entity.player.Player entity, net.minecraft.world.item.ItemStack crafting) {
            this.entity = entity;
            this.crafting = crafting;
        }

        public net.minecraft.world.entity.player.Player getEntity() {
            return entity;
        }

        public net.minecraft.world.item.ItemStack getCrafting() {
            return crafting;
        }
    }

    public static class ItemSmeltedEvent extends GameEvent {
        private final net.minecraft.world.entity.player.Player entity;
        private final net.minecraft.world.item.ItemStack smelting;

        public ItemSmeltedEvent(net.minecraft.world.entity.player.Player entity, net.minecraft.world.item.ItemStack smelting) {
            this.entity = entity;
            this.smelting = smelting;
        }

        public net.minecraft.world.entity.player.Player getEntity() {
            return entity;
        }

        public net.minecraft.world.item.ItemStack getSmelting() {
            return smelting;
        }
    }
}
