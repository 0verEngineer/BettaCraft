package org.overengineer.bettacraft.tank.interior;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * Substrate per floor block (PLAN 7.2): shift + right-click with a block on an interior floor block gives the floor
 * the look of that block (no block is placed on top). It costs one block unless {@code interior.substrateCostsBlock}
 * is off; replacing or removing (shift + right-click with an empty hand) gives a paid block back.
 */
public final class Substrate {
    private Substrate() {}

    /** Whether the block can be a substrate: a full, opaque-ish model block without block entity, not blacklisted. */
    public static boolean isValid(Block block) {
        BlockState s = block.defaultBlockState();
        if (s.is(ModTags.ACTIVE_SOIL) && !(block instanceof EntityBlock)) return true; // M25 part d: the mod's own active soil
        if (block instanceof EntityBlock || BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(BettaCraft.MOD_ID)) return false;
        if (s.is(ModTags.SUBSTRATE_BLACKLIST) || s.getRenderShape() != RenderShape.MODEL) return false;
        return s.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    public static boolean isFloor(BlockState s) {
        return s.is(ModBlocks.INTERIOR_FLOOR.get());
    }

    /** M17: the substrate block entity at a floor position, only if the floor really shows a substrate (older worlds can hold stale ones). */
    @org.jetbrains.annotations.Nullable
    public static InteriorFloorBlockEntity active(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return isFloor(s) && s.getValue(InteriorFloorBlock.SUBSTRATE) && level.getBlockEntity(pos) instanceof InteriorFloorBlockEntity be ? be : null;
    }

    /** Handles a shift + right-click on a floor block. Returns true if it was consumed. */
    public static boolean use(ServerPlayer player, InteractionHand hand, BlockPos pos) {
        ServerLevel level = player.level();
        BlockState floor = level.getBlockState(pos);
        if (!isFloor(floor) || InteriorManager.regionAt(level.getServer(), pos) == null) return false;
        if (!InteriorManager.canBuild(player, pos)) {
            player.displayClientMessage(Component.translatable("message.bettacraft.tank.locked").withStyle(ChatFormatting.RED), true);
            return true;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            if (!floor.getValue(InteriorFloorBlock.SUBSTRATE)) return false;
            ItemStack back = clear(level, pos);
            if (!back.isEmpty() && !player.getInventory().add(back)) player.drop(back, false);
            return true;
        }
        if (!(stack.getItem() instanceof BlockItem bi)) return false;
        Block block = bi.getBlock();
        if (!isValid(block)) {
            player.displayClientMessage(Component.translatable("message.bettacraft.substrate.invalid", stack.getHoverName())
                    .withStyle(ChatFormatting.RED), true);
            return true;
        }
        BlockState substrate = block.defaultBlockState();
        if (floor.getValue(InteriorFloorBlock.SUBSTRATE) && level.getBlockEntity(pos) instanceof InteriorFloorBlockEntity be
                && be.substrate() == substrate) {
            return true; // already this substrate
        }
        boolean paid = ServerConfig.getOr(ServerConfig.INTERIOR_SUBSTRATE_COSTS_BLOCK, true) && !player.getAbilities().instabuild;
        ItemStack back = set(level, pos, substrate, paid);
        if (paid) stack.shrink(1);
        org.overengineer.bettacraft.progression.Progress.award(player, org.overengineer.bettacraft.progression.Progress.SUBSTRATE);
        if (!back.isEmpty() && !player.getInventory().add(back)) player.drop(back, false);
        SoundType sound = org.overengineer.bettacraft.platform.Platform.get().soundType(substrate, level, pos, player);
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2, sound.getPitch() * 0.8F);
        return true;
    }

    /** Sets the substrate of a floor block. Returns the paid block of the previous substrate (or empty). */
    public static ItemStack set(ServerLevel level, BlockPos pos, BlockState substrate, boolean paid) {
        ItemStack back = ItemStack.EMPTY;
        BlockState floor = level.getBlockState(pos);
        if (floor.getValue(InteriorFloorBlock.SUBSTRATE) && level.getBlockEntity(pos) instanceof InteriorFloorBlockEntity old && old.paid()) {
            back = new ItemStack(old.substrate().getBlock());
        }
        if (!floor.getValue(InteriorFloorBlock.SUBSTRATE)) {
            level.setBlock(pos, floor.setValue(InteriorFloorBlock.SUBSTRATE, true), Block.UPDATE_CLIENTS);
        }
        BlockState before = floor.getValue(InteriorFloorBlock.SUBSTRATE) && level.getBlockEntity(pos) instanceof InteriorFloorBlockEntity o ? o.substrate() : null;
        if (level.getBlockEntity(pos) instanceof InteriorFloorBlockEntity be) be.set(substrate, paid);
        chemistry(level, pos, before, substrate); // M25
        return back;
    }

    /** M25: the region's soil / limestone counts follow a substrate change. */
    private static void chemistry(ServerLevel level, BlockPos pos, @org.jetbrains.annotations.Nullable BlockState old, @org.jetbrains.annotations.Nullable BlockState neu) {
        InteriorRegion r = InteriorManager.regionAt(level.getServer(), pos);
        if (r != null) {
            r.equipment().onSubstrate(old, neu);
            InteriorRegions.get(level.getServer()).setDirty();
        }
    }

    /** Removes the substrate. Returns the paid block (or empty). */
    public static ItemStack clear(ServerLevel level, BlockPos pos) {
        BlockState floor = level.getBlockState(pos);
        if (!isFloor(floor) || !floor.getValue(InteriorFloorBlock.SUBSTRATE)) return ItemStack.EMPTY;
        ItemStack back = level.getBlockEntity(pos) instanceof InteriorFloorBlockEntity be && be.paid()
                ? new ItemStack(be.substrate().getBlock()) : ItemStack.EMPTY;
        if (level.getBlockEntity(pos) instanceof InteriorFloorBlockEntity be) chemistry(level, pos, be.substrate(), null); // M25
        level.setBlock(pos, floor.setValue(InteriorFloorBlock.SUBSTRATE, false), Block.UPDATE_CLIENTS);
        level.removeBlockEntity(pos); // M17: no stale (paid) substrate left behind
        return back;
    }
}
