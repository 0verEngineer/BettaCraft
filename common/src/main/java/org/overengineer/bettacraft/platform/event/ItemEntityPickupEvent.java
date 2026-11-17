package org.overengineer.bettacraft.platform.event;

import org.jetbrains.annotations.Nullable;

/** M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). */
public final class ItemEntityPickupEvent {
    private ItemEntityPickupEvent() {}

    public static class Pre extends GameEvent {
        private final net.minecraft.world.entity.player.Player player;
        private final net.minecraft.world.entity.item.ItemEntity itemEntity;

        public Pre(net.minecraft.world.entity.player.Player player, net.minecraft.world.entity.item.ItemEntity itemEntity) {
            this.player = player;
            this.itemEntity = itemEntity;
        }

        public net.minecraft.world.entity.player.Player getPlayer() {
            return player;
        }

        public net.minecraft.world.entity.item.ItemEntity getItemEntity() {
            return itemEntity;
        }
    }
}
