package org.overengineer.bettacraft.chem;

import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.tank.Salinity;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M25: the water chemistry items used on a tank: the TDS meter (measures, never wears out), the pH drop test (measures,
 * used up after its tests), remineralising salt (raises the TDS; sneaking: up to {@link TankChemistry#REMINERALISE_TARGET})
 * and almond leaves (tannins: they lower the pH a little and fade). M28: alder cones (the same with a third of the
 * tannins); both are also laid on the floor as leaf litter ({@link org.overengineer.bettacraft.decor.LitterBlock}).
 */
public class ChemItem extends Item {
    public enum Kind { TDS_METER, PH_TEST, REMINERALISER, ALMOND_LEAF, ALDER_CONE }

    /** M28: tannins of an alder cone against an almond leaf. */
    public static final double CONE_TANNIN = WaterChemistry.CONE_TANNIN;

    private final Kind kind;

    public ChemItem(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    /** Sneaking skips the tank's own interaction: the tank is handled here then. */
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getPlayer() == null || !(ctx.getLevel().getBlockState(ctx.getClickedPos()).getBlock() instanceof TankBlock)) {
            return kind == Kind.ALMOND_LEAF || kind == Kind.ALDER_CONE ? placeLitter(ctx) : InteractionResult.PASS;
        }
        return useOnTank(ctx.getItemInHand(), ctx.getLevel(), ctx.getClickedPos(), ctx.getPlayer(), ctx.getHand()).result();
    }

    /** M28: lays the leaf or cone on the floor as leaf litter (in a tank interior: with the right to build there). */
    private InteractionResult placeLitter(UseOnContext ctx) {
        Level level = ctx.getLevel();
        net.minecraft.world.item.context.BlockPlaceContext place = new net.minecraft.world.item.context.BlockPlaceContext(ctx);
        BlockPos pos = place.getClickedPos();
        net.minecraft.world.level.block.state.BlockState state = (kind == Kind.ALMOND_LEAF
                ? org.overengineer.bettacraft.registry.ModBlocks.ALMOND_LEAF_LITTER : org.overengineer.bettacraft.registry.ModBlocks.ALDER_CONES).get().defaultBlockState();
        if (!place.canPlace() || !state.canSurvive(level, pos)) return InteractionResult.PASS;
        Player player = ctx.getPlayer();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (org.overengineer.bettacraft.tank.interior.InteriorManager.isInterior(level) && player != null
                && !org.overengineer.bettacraft.tank.interior.InteriorManager.canBuild(player, pos)) {
            player.displayClientMessage(Component.translatable("message.bettacraft.tank.locked"), true);
            return InteractionResult.FAIL;
        }
        if (player != null && !level.mayInteract(player, pos)) return InteractionResult.FAIL;
        level.setBlock(pos, state, net.minecraft.world.level.block.Block.UPDATE_ALL);
        level.playSound(null, pos, kind == Kind.ALMOND_LEAF ? SoundEvents.AZALEA_LEAVES_PLACE : SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.7F, 1.1F);
        if (player == null || !player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.bettacraft." + kind.name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.GRAY));
    }

    /** Right-click on a tank with one of the items (from {@link org.overengineer.bettacraft.tank.TankInteractions}). */
    public static ItemInteractionResult useOnTank(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!(stack.getItem() instanceof ChemItem item)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        TankBlockEntity c = TankMultiblock.getController(level, pos);
        if (c == null) return ItemInteractionResult.CONSUME;
        TankData t = c.getTank();
        if (!TankChemistry.enabled()) {
            player.displayClientMessage(Component.translatable("message.bettacraft.chem.off").withStyle(ChatFormatting.GRAY), true);
            return ItemInteractionResult.CONSUME;
        }
        if (t.waterMb() <= 0) {
            player.displayClientMessage(Component.translatable("message.bettacraft.reef.no_water").withStyle(ChatFormatting.GOLD), true);
            return ItemInteractionResult.CONSUME;
        }
        ServerLevel server = (ServerLevel) level;
        TankChemistry.Drivers d = TankChemistry.drivers(server, c);
        switch (item.kind) {
            case TDS_METER -> {
                player.displayClientMessage(tdsLine(t, d), true);
                level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.4F, 1.8F);
            }
            case PH_TEST -> {
                player.displayClientMessage(phLine(t, TankChemistry.phTarget(t, d)), true);
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.5F, 1.4F);
                if (!player.getAbilities().instabuild) stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
            case REMINERALISER, ALMOND_LEAF, ALDER_CONE -> {
                if (!TankInhabitants.canModify(player, t)) {
                    player.displayClientMessage(Component.translatable("message.bettacraft.tank.locked"), true);
                    return ItemInteractionResult.CONSUME;
                }
                if (item.kind == Kind.REMINERALISER) remineralise(stack, server, pos, player, c);
                else addLeaf(stack, server, pos, player, c);
            }
        }
        return ItemInteractionResult.CONSUME;
    }

    private static void remineralise(ItemStack stack, ServerLevel level, BlockPos pos, Player player, TankBlockEntity c) {
        TankData t = c.getTank();
        if (t.salinity() >= Salinity.BRACKISH_FROM) {
            player.displayClientMessage(Component.translatable("message.bettacraft.chem.salty").withStyle(ChatFormatting.GOLD), true);
            return;
        }
        double per = ServerConfig.getOr(ServerConfig.CHEM_REMINERAL_PPM, 400.0);
        int n = 1;
        if (player.isShiftKeyDown()) {
            n = WaterChemistry.portionsTo(t.tds(), t.waterMb(), TankChemistry.REMINERALISE_TARGET, per);
            if (n <= 0) {
                player.displayClientMessage(Component.translatable("message.bettacraft.chem.hard_enough", Math.round(t.tds())).withStyle(ChatFormatting.GOLD), true);
                return;
            }
        }
        if (!player.getAbilities().instabuild) n = Math.min(n, stack.getCount());
        t.setTds(WaterChemistry.remineralise(t.tds(), t.waterMb(), n, per));
        if (!player.getAbilities().instabuild) stack.shrink(n);
        level.playSound(null, pos, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 0.7F, 1.6F);
        player.displayClientMessage(Component.translatable("message.bettacraft.chem.remineralised", n, Math.round(t.tds())), true);
        c.markTankChanged();
    }

    private static void addLeaf(ItemStack stack, ServerLevel level, BlockPos pos, Player player, TankBlockEntity c) {
        TankData t = c.getTank();
        double per = ServerConfig.getOr(ServerConfig.CHEM_LEAF_TANNIN, 1.0)
                * (stack.getItem() instanceof ChemItem ci && ci.kind == Kind.ALDER_CONE ? CONE_TANNIN : 1.0);
        t.setTannin(WaterChemistry.addLeaves(t.tannin(), t.waterMb(), 1, per));
        if (!player.getAbilities().instabuild) stack.shrink(1);
        level.playSound(null, pos, SoundEvents.AZALEA_LEAVES_PLACE, SoundSource.BLOCKS, 0.7F, 1.0F);
        player.displayClientMessage(Component.translatable("message.bettacraft.chem.leaf", Math.round(t.tannin() * 100)), true);
        c.markTankChanged();
    }

    // ------------------------------------------------------------------ texts (also for the menu)

    /** "TDS 250 ppm · medium hard water". */
    public static MutableComponent tdsLine(TankData t, TankChemistry.Drivers d) {
        WaterChemistry.Hardness h = TankChemistry.hardness(t, d);
        return Component.translatable("message.bettacraft.chem.tds", Math.round(t.tds()), hardness(h));
    }

    public static MutableComponent hardness(WaterChemistry.Hardness h) {
        ChatFormatting col = switch (h) {
            case SOFT -> ChatFormatting.AQUA;
            case MEDIUM -> ChatFormatting.GREEN;
            case HARD -> ChatFormatting.GOLD;
        };
        return Component.translatable("gui.bettacraft.chem.hardness." + h.name().toLowerCase(Locale.ROOT)).withStyle(col);
    }

    /** "pH 7.5 (neutral, falling)". */
    public static MutableComponent phLine(TankData t, double target) {
        return Component.translatable("message.bettacraft.chem.ph", ph(t.ph()), phClass(t.ph()), trend(t.ph(), target));
    }

    public static String ph(double ph) {
        return String.format(Locale.ROOT, "%.1f", ph);
    }

    public static MutableComponent phClass(double ph) {
        String k = ph < 6.5 ? "acidic" : ph <= 7.5 ? "neutral" : "alkaline";
        ChatFormatting col = ph < 6.5 ? ChatFormatting.GOLD : ph <= 7.5 ? ChatFormatting.GREEN : ChatFormatting.AQUA;
        return Component.translatable("gui.bettacraft.chem.ph." + k).withStyle(col);
    }

    public static MutableComponent trend(double ph, double target) {
        String k = target < ph - 0.05 ? "falling" : target > ph + 0.05 ? "rising" : "steady";
        return Component.translatable("gui.bettacraft.chem.trend." + k, ph(target));
    }
}
