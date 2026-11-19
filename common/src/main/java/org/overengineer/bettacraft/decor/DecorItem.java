package org.overengineer.bettacraft.decor;

import org.overengineer.bettacraft.util.SinkList;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;

/**
 * A decoration structure item (PLAN 7.3): right-click on a block places the structure (vanilla blocks) with its
 * footprint centred on the target and its front facing the player; the item is used up and the blocks are mined
 * individually afterwards. Works in the tank interior (where 1 block = 1 tank pixel at scale 16) and in the normal
 * world. A hologram shows the placement while holding the item (client).
 */
public class DecorItem extends Item {
    private static final Map<Identifier, DecorTemplate> SERVER_CACHE = new HashMap<>();

    private final DecorType type;

    public DecorItem(DecorType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public DecorType type() {
        return type;
    }

    public static void clearCache() {
        SERVER_CACHE.clear();
    }

    public static Optional<DecorTemplate> template(ServerLevel level, Identifier id) {
        DecorTemplate t = SERVER_CACHE.get(id);
        if (t == null) {
            t = DecorTemplate.load(level, id).orElse(null);
            if (t != null) SERVER_CACHE.put(id, t);
        }
        return Optional.ofNullable(t);
    }

    /** Target block for placement: the clicked block if replaceable, else the one in front of the clicked face. */
    public static BlockPos target(BlockPlaceContext ctx) {
        return ctx.getClickedPos();
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ServerLevel server = (ServerLevel) level;
        Player player = ctx.getPlayer();
        BlockPlaceContext place = new BlockPlaceContext(ctx);
        BlockPos base = target(place);
        Rotation rot = DecorTemplate.rotationFor(ctx.getHorizontalDirection());
        Component error = place(server, type, base, rot, player);
        if (error != null) {
            if (player != null) player.sendOverlayMessage(error.copy().withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (player == null || !player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        server.playSound(null, base, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F);
        return InteractionResult.CONSUME;
    }

    /** Places the structure. Returns an error message, or null on success. */
    public static Component place(ServerLevel level, DecorType type, BlockPos base, Rotation rot, Player player) {
        Optional<DecorTemplate> t = template(level, type.structure());
        if (t.isEmpty()) return Component.translatable("message.bettacraft.decor.missing", type.structure().toString());
        List<DecorTemplate.Placed> blocks = t.get().layout(base, rot);
        boolean interior = InteriorManager.isInterior(level);
        InteriorRegion region = interior ? InteriorManager.regionAt(level.getServer(), base) : null;
        if (interior && (region == null || (player != null && !InteriorManager.canBuild(player, base)))) {
            return Component.translatable("message.bettacraft.tank.locked");
        }
        for (DecorTemplate.Placed b : blocks) {
            if (interior ? !region.containsInterior(b.pos()) : !level.isInWorldBounds(b.pos())) {
                return Component.translatable("message.bettacraft.decor.no_room");
            }
            if (!level.getBlockState(b.pos()).canBeReplaced()) return Component.translatable("message.bettacraft.decor.blocked");
            if (player != null && !level.mayInteract(player, b.pos())) return Component.translatable("message.bettacraft.decor.blocked");
        }
        for (DecorTemplate.Placed b : blocks) level.setBlock(b.pos(), b.state(), Block.UPDATE_ALL);
        org.overengineer.bettacraft.progression.Progress.award(player, org.overengineer.bettacraft.progression.Progress.DECOR, interior ? 1 : 0);
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        List<Component> tooltip = new SinkList<>(tooltipAdder);
        tooltip.add(Component.translatable("tooltip.bettacraft.decor.size", type.sizeX, type.sizeZ, type.sizeY).withStyle(ChatFormatting.GRAY));
        if (type.hasHint()) tooltip.add(Component.translatable("tooltip.bettacraft." + type.itemId()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.bettacraft.decor.usage").withStyle(ChatFormatting.DARK_GRAY));
    }
}
