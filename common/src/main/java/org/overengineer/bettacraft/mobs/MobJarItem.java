package org.overengineer.bettacraft.mobs;

import org.overengineer.bettacraft.util.SinkList;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import org.overengineer.bettacraft.util.NbtCompat;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankInhabitants;

/**
 * M31 part d (question C1a): the mob jar ("Mobglas"). Right-click a mob of {@link GlassMobSpecies} that has lost three
 * quarters of its health ({@code terrarium.mobCatchHealth}, 25 %) and it goes into the jar alive, with all its data and the
 * glass mark (from now on it drops nothing). A mob that already lives in a tank comes out at any health. After every try
 * the jar needs {@link #COOLDOWN} ticks. Right-click a tank with the full jar to put the mob in (it shrinks there); any
 * other block lets it out at full size (still without drops).
 */
public class MobJarItem extends Item {
    public static final String KEY = "BettacraftMob";
    public static final int COOLDOWN = 60;

    public MobJarItem(Properties properties) {
        super(properties);
    }

    public static boolean isFilled(ItemStack stack) {
        return storedEntity(stack) != null;
    }

    /** The mob in a full jar (its data with {@code id}), or null. */
    @Nullable
    public static CompoundTag storedEntity(ItemStack stack) {
        if (!(stack.getItem() instanceof MobJarItem)) return null;
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null || !cd.copyTag().contains(KEY)) return null;
        CompoundTag t = cd.copyTag().getCompoundOrEmpty(KEY);
        return NbtCompat.has(t, "id", Tag.TAG_STRING) ? t : null;
    }

    @Nullable
    public static EntityType<?> containedType(ItemStack stack) {
        CompoundTag t = storedEntity(stack);
        return t == null ? null : EntityType.byString(t.getStringOr("id", "")).orElse(null);
    }

    /** A full jar holding {@code e} (the entity itself is not removed). */
    public static ItemStack fillWith(Entity e) {
        ItemStack jar = new ItemStack(ModItems.MOB_JAR.get());
        CompoundTag t = new CompoundTag();
        String id = e.getEncodeId();
        if (id != null) t.putString("id", id);
        t.merge(NbtCompat.saveEntityWithoutId(e));
        CompoundTag data = new CompoundTag();
        data.put(KEY, t);
        jar.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        jar.set(DataComponents.MAX_STACK_SIZE, 1);
        return jar;
    }

    /** Whether the jar takes this mob now (its kind, small enough, weak enough or already a mob of a tank). */
    @Nullable
    public static Component refusal(LivingEntity target) {
        if (!GlassMobs.enabled()) return Component.translatable("message.bettacraft.feature_off");
        if (!(target instanceof Mob) || GlassMobs.species(target.getType()) == null || !GlassMobs.fits(target)) {
            return Component.translatable("message.bettacraft.glass.not_allowed", target.getType().getDescription());
        }
        boolean pet = GlassMobs.isGlassMob(target) && TankInhabitants.hasTankRef(target);
        double below = ServerConfig.getOr(ServerConfig.TERRA_MOB_CATCH_HEALTH, 0.25);
        if (!pet && target.getHealth() > target.getMaxHealth() * below + 1e-4) {
            return Component.translatable("message.bettacraft.glass.too_strong", target.getDisplayName(), Math.round(below * 100));
        }
        return null;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (isFilled(stack) || !target.isAlive() || target instanceof Player) return InteractionResult.PASS;
        if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;
        Component no = refusal(target);
        if (no != null) {
            player.displayClientMessage(no, true);
            if (GlassMobs.species(target.getType()) != null) player.getCooldowns().addCooldown(stack, COOLDOWN);
            return InteractionResult.FAIL;
        }
        Mob m = (Mob) target;
        TankInhabitants.release(m);
        GlassMobs.prepare(m); // the mark: it never drops anything again
        ItemStack filled = fillWith(m);
        org.overengineer.bettacraft.containers.CatchGuard.caught(player); // M62 part 7
        m.playSound(SoundEvents.BOTTLE_FILL_DRAGONBREATH, 0.8F, 1.3F);
        player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, filled, false));
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        if (m.level() instanceof ServerLevel server) {
            var c = org.overengineer.bettacraft.tank.TankMultiblock.getController(server, m.blockPosition());
            if (c != null) {
                c.getTank().inhabitants().remove(m.getUUID());
                org.overengineer.bettacraft.tank.TankCare.log(server, c, "log.bettacraft.taken_out", org.overengineer.bettacraft.tank.TankCare.nameArg(m));
                c.markTankChanged();
            }
        }
        m.discard();
        return InteractionResult.SUCCESS;
    }

    /**
     * A full jar on a block that is no tank (the tank takes it first): the mob comes out, at full size. M62 part 7: only after
     * the confirmation, like a bucket ({@link org.overengineer.bettacraft.containers.ContainerRules#release}).
     */
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        ItemStack stack = ctx.getItemInHand();
        CompoundTag stored = storedEntity(stack);
        if (stored == null) return InteractionResult.PASS;
        Player player = ctx.getPlayer();
        if (player != null && !org.overengineer.bettacraft.containers.ContainerRules.release(ctx.getLevel(), player, ctx.getHand(), stack)) return InteractionResult.FAIL;
        return releaseAt(ctx.getLevel(), player, ctx.getHand(), stored, ctx.getClickedPos().relative(ctx.getClickedFace()), ctx.getRotation());
    }

    /** M62 part 7: the confirmed release (the confirmation uses the item again): onto the block the player looks at. */
    @Override
    public InteractionResult use(net.minecraft.world.level.Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag stored = storedEntity(stack);
        if (stored == null) return InteractionResult.PASS;
        net.minecraft.world.phys.BlockHitResult hit = getPlayerPOVHitResult(level, player, net.minecraft.world.level.ClipContext.Fluid.NONE);
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK
                || level.getBlockState(hit.getBlockPos()).getBlock() instanceof org.overengineer.bettacraft.tank.TankBlock) {
            return InteractionResult.PASS;
        }
        if (!org.overengineer.bettacraft.containers.ContainerRules.release(level, player, hand, stack)) return org.overengineer.bettacraft.containers.ContainerRules.waiting(stack);
        InteractionResult r = releaseAt(level, player, hand, stored, hit.getBlockPos().relative(hit.getDirection()), player.getYRot() + 180F);
        return r.consumesAction() ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    private static InteractionResult releaseAt(net.minecraft.world.level.Level lvl, @Nullable Player player, InteractionHand hand, CompoundTag stored, BlockPos at, float yaw) {
        if (!(lvl instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        Entity e = org.overengineer.bettacraft.compat.FishContainers.create(level, stored);
        if (e == null) return InteractionResult.FAIL;
        e.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0F);
        if (e instanceof Mob m) GlassMobs.unshrink(m);
        TankInhabitants.release(e);
        if (!level.tryAddFreshEntityWithPassengers(e)) return InteractionResult.FAIL;
        level.playSound(null, at, SoundEvents.BOTTLE_EMPTY, SoundSource.NEUTRAL, 0.8F, 1.0F);
        if (player != null && !player.getAbilities().instabuild) {
            player.setItemInHand(hand, new ItemStack(ModItems.MOB_JAR.get()));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        List<Component> lines = new SinkList<>(tooltipAdder);
        CompoundTag t = storedEntity(stack);
        if (t == null) {
            lines.add(Component.translatable("tooltip.bettacraft.mob_jar.empty", Math.round(ServerConfig.getOr(ServerConfig.TERRA_MOB_CATCH_HEALTH, 0.25) * 100))
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        EntityType.byString(t.getStringOr("id", "")).ifPresent(type -> lines.add(type.getDescription().copy().withStyle(ChatFormatting.GOLD)));
        if (t.contains("Health")) lines.add(Component.translatable("tooltip.bettacraft.mob_jar.health", Math.round(t.getFloatOr("Health", 0f))).withStyle(ChatFormatting.GRAY));
    }
}
