package org.overengineer.bettacraft.platform.event;

import org.jetbrains.annotations.Nullable;

/** M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). */
public final class PlayerInteractEvent {
    private PlayerInteractEvent() {}

    public static class EntityInteract extends GameEvent {
        private final net.minecraft.world.entity.player.Player entity;
        private final net.minecraft.world.InteractionHand hand;
        private final net.minecraft.world.entity.Entity target;
        private net.minecraft.world.InteractionResult cancellationResult = net.minecraft.world.InteractionResult.PASS;

        public EntityInteract(net.minecraft.world.entity.player.Player entity, net.minecraft.world.InteractionHand hand, net.minecraft.world.entity.Entity target) {
            this.entity = entity;
            this.hand = hand;
            this.target = target;
        }

        public net.minecraft.world.entity.player.Player getEntity() {
            return entity;
        }

        public net.minecraft.world.InteractionHand getHand() {
            return hand;
        }

        public net.minecraft.world.entity.Entity getTarget() {
            return target;
        }

        public net.minecraft.world.level.Level getLevel() {
            return getEntity().level();
        }

        public net.minecraft.world.item.ItemStack getItemStack() {
            return getEntity().getItemInHand(getHand());
        }

        @Override
        public boolean isCancelable() {
            return true;
        }

        public net.minecraft.world.InteractionResult getCancellationResult() {
            return cancellationResult;
        }

        public void setCancellationResult(net.minecraft.world.InteractionResult result) {
            this.cancellationResult = result;
        }
    }

    public static class RightClickBlock extends GameEvent {
        private final net.minecraft.world.entity.player.Player entity;
        private final net.minecraft.world.InteractionHand hand;
        private final net.minecraft.core.BlockPos pos;
        private final net.minecraft.world.phys.BlockHitResult hitVec;
        private net.minecraft.world.InteractionResult cancellationResult = net.minecraft.world.InteractionResult.PASS;
        private TriState useBlock = TriState.DEFAULT;
        private TriState useItem = TriState.DEFAULT;

        public RightClickBlock(net.minecraft.world.entity.player.Player entity, net.minecraft.world.InteractionHand hand, net.minecraft.core.BlockPos pos, net.minecraft.world.phys.BlockHitResult hitVec) {
            this.entity = entity;
            this.hand = hand;
            this.pos = pos;
            this.hitVec = hitVec;
        }

        public net.minecraft.world.entity.player.Player getEntity() {
            return entity;
        }

        public net.minecraft.world.InteractionHand getHand() {
            return hand;
        }

        public net.minecraft.core.BlockPos getPos() {
            return pos;
        }

        public net.minecraft.world.phys.BlockHitResult getHitVec() {
            return hitVec;
        }

        public net.minecraft.world.level.Level getLevel() {
            return getEntity().level();
        }

        public net.minecraft.world.item.ItemStack getItemStack() {
            return getEntity().getItemInHand(getHand());
        }

        public @Nullable net.minecraft.core.Direction getFace() {
            return hitVec == null ? null : hitVec.getDirection();
        }

        @Override
        public boolean isCancelable() {
            return true;
        }

        public net.minecraft.world.InteractionResult getCancellationResult() {
            return cancellationResult;
        }

        public void setCancellationResult(net.minecraft.world.InteractionResult result) {
            this.cancellationResult = result;
        }

        public TriState getUseBlock() {
            return useBlock;
        }

        public void setUseBlock(TriState v) {
            this.useBlock = v;
        }

        public TriState getUseItem() {
            return useItem;
        }

        public void setUseItem(TriState v) {
            this.useItem = v;
        }
    }

    public static class RightClickItem extends GameEvent {
        private final net.minecraft.world.entity.player.Player entity;
        private final net.minecraft.world.InteractionHand hand;
        private net.minecraft.world.InteractionResult cancellationResult = net.minecraft.world.InteractionResult.PASS;

        public RightClickItem(net.minecraft.world.entity.player.Player entity, net.minecraft.world.InteractionHand hand) {
            this.entity = entity;
            this.hand = hand;
        }

        public net.minecraft.world.entity.player.Player getEntity() {
            return entity;
        }

        public net.minecraft.world.InteractionHand getHand() {
            return hand;
        }

        public net.minecraft.world.level.Level getLevel() {
            return getEntity().level();
        }

        public net.minecraft.world.item.ItemStack getItemStack() {
            return getEntity().getItemInHand(getHand());
        }

        @Override
        public boolean isCancelable() {
            return true;
        }

        public net.minecraft.world.InteractionResult getCancellationResult() {
            return cancellationResult;
        }

        public void setCancellationResult(net.minecraft.world.InteractionResult result) {
            this.cancellationResult = result;
        }
    }

    public static class LeftClickBlock extends GameEvent {
        private final net.minecraft.world.entity.player.Player entity;
        private final net.minecraft.core.BlockPos pos;

        public LeftClickBlock(net.minecraft.world.entity.player.Player entity, net.minecraft.core.BlockPos pos) {
            this.entity = entity;
            this.pos = pos;
        }

        public net.minecraft.world.entity.player.Player getEntity() {
            return entity;
        }

        public net.minecraft.core.BlockPos getPos() {
            return pos;
        }

        public net.minecraft.world.level.Level getLevel() {
            return getEntity().level();
        }

        @Override
        public boolean isCancelable() {
            return true;
        }
    }
}
