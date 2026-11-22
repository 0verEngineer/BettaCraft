package org.overengineer.bettacraft.big;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankInhabitants;

/**
 * M30: the transport tub carries one big animal (giant catfish, shark, ray, dolphin). It is bulky: while a filled tub is in
 * the inventory the player walks slower ({@code bigAnimalRules.tubSlowness}).
 * <ul>
 *     <li>Right-click a big animal <b>in a tank</b> (or one that was caught before, not a wild one) with an empty tub: it goes
 *     into the tub. A wild big animal needs the large net first ({@link LargeNetItem}).</li>
 *     <li>Right-click a tank with a filled tub: the animal goes in if the tank is big enough (the size rule,
 *     {@link BigAnimals#refusal}); the tub brings no water.</li>
 *     <li>Right-click open water with a filled tub: the animal is released.</li>
 * </ul>
 * The animal is kept with all its data (the {@code minecraft:custom_data} key {@link #STORED_KEY}, its type as
 * {@code bettacraft:bag_entity}).
 */
public class TransportTubItem extends Item {
    public static final String STORED_KEY = "BettacraftBigAnimal";
    public static final ResourceLocation SLOW_ID = BettaCraft.id("transport_tub");

    public TransportTubItem(Properties properties) {
        super(properties);
    }

    public static boolean isFilled(ItemStack stack) {
        return stack.getItem() instanceof TransportTubItem && stack.has(ModDataComponents.BAG_ENTITY.get());
    }

    @Nullable
    public static EntityType<?> containedType(ItemStack stack) {
        if (!isFilled(stack)) return null;
        return BuiltInRegistries.ENTITY_TYPE.getOptional(stack.get(ModDataComponents.BAG_ENTITY.get())).orElse(null);
    }

    /** The full entity data (with {@code id}) of the animal in the tub, or null. */
    @Nullable
    public static CompoundTag storedEntity(ItemStack stack) {
        if (!isFilled(stack)) return null;
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null || !cd.contains(STORED_KEY)) return null;
        CompoundTag t = cd.copyTag().getCompound(STORED_KEY);
        return t.contains("id", Tag.TAG_STRING) ? t : null;
    }

    /** A filled tub holding {@code e} (the entity itself is not removed). */
    public static ItemStack fillWith(Entity e) {
        ItemStack tub = new ItemStack(ModItems.TRANSPORT_TUB.get());
        CompoundTag t = new CompoundTag();
        String id = e.getEncodeId();
        if (id != null) t.putString("id", id);
        e.saveWithoutId(t);
        t.remove("UUID"); // a new one when it comes out (no clash with the old entity)
        CompoundTag data = new CompoundTag();
        data.put(STORED_KEY, t);
        tub.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        tub.set(ModDataComponents.BAG_ENTITY.get(), BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()));
        return tub;
    }

    /** Puts {@code target} into the empty tub in {@code hand} (removes the entity). */
    public static void scoop(Player player, InteractionHand hand, Entity target) {
        org.overengineer.bettacraft.containers.CatchGuard.caught(player); // M62 part 7
        TankInhabitants.release(target);
        ItemStack filled = fillWith(target);
        player.level().playSound(null, target.blockPosition(), SoundEvents.BUCKET_FILL_FISH, SoundSource.PLAYERS, 1.0F, 0.7F);
        ItemStack held = player.getItemInHand(hand);
        if (held.getCount() <= 1) {
            player.setItemInHand(hand, filled);
        } else {
            held.shrink(1);
            if (!player.getInventory().add(filled)) player.drop(filled, false);
        }
        if (target instanceof org.overengineer.bettacraft.entity.tankmate.TankmateEntity m) {
            org.overengineer.bettacraft.tank.TankBlockEntity c = player.level() instanceof ServerLevel sl ? TankInhabitants.resolveController(sl, m) : null;
            if (c != null) c.getTank().inhabitants().remove(target.getUUID());
        }
        target.discard();
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (isFilled(stack) || !target.isAlive() || target instanceof Player) return InteractionResult.PASS;
        if (!BigAnimals.isBig(target)) {
            if (!player.level().isClientSide && TankInhabitants.isWaterAnimal(target)) {
                player.displayClientMessage(Component.translatable("message.bettacraft.big.tub_small", target.getDisplayName()), true);
            }
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        // a wild one does not let itself be scooped up: the large net first
        if (!TankInhabitants.hasTankRef(target) && wild(target)) {
            player.displayClientMessage(Component.translatable("message.bettacraft.big.needs_net", target.getDisplayName()), true);
            return InteractionResult.CONSUME;
        }
        if (TankInhabitants.hasTankRef(target) && player.level() instanceof ServerLevel sl) {
            var c = TankInhabitants.resolveController(sl, target);
            if (c != null && !TankInhabitants.canModify(player, c.getTank())) {
                player.displayClientMessage(Component.translatable("message.bettacraft.tank.locked"), true);
                return InteractionResult.CONSUME;
            }
        }
        scoop(player, hand, target);
        return InteractionResult.CONSUME;
    }

    /** A wild big animal (it spawned in the world and nobody caught it yet). */
    static boolean wild(Entity e) {
        if (e instanceof org.overengineer.bettacraft.entity.tankmate.TankmateEntity m) return m.isWild() && !m.fromBucket();
        if (e instanceof Bucketable b && b.fromBucket()) return false;
        return !(e instanceof Mob m && m.isPersistenceRequired());
    }

    /** Releases the animal into open water. Clicking a tank is handled by the tank block before this. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isFilled(stack)) return InteractionResultHolder.pass(stack);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);
        BlockPos pos = hit.getBlockPos();
        if (level.getBlockState(pos).getBlock() instanceof TankBlock) return InteractionResultHolder.pass(stack);
        // M39 part b: a big land animal of another mod (a lion, a komodo dragon ...) goes back onto the land, a water animal into water
        boolean land = !level.getFluidState(pos).is(FluidTags.WATER) && landAnimal(containedType(stack));
        if (land) {
            if (!level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) return InteractionResultHolder.fail(stack);
            pos = pos.above();
        } else if (!level.getFluidState(pos).is(FluidTags.WATER)) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.bettacraft.fish_bag.needs_water"), true);
            return InteractionResultHolder.fail(stack);
        }
        // M62 part 7: like a bucket, only after the confirmation (and not right after the catch)
        if (!org.overengineer.bettacraft.containers.ContainerRules.release(level, player, hand, stack)) return org.overengineer.bettacraft.containers.ContainerRules.waiting(stack);
        if (level instanceof ServerLevel server) {
            CompoundTag t = storedEntity(stack);
            Entity e = t == null ? null : EntityType.loadEntityRecursive(t, server, en -> en);
            if (e == null) {
                player.displayClientMessage(Component.translatable("message.bettacraft.fish_bag.release_failed"), true);
                return InteractionResultHolder.fail(stack);
            }
            e.moveTo(pos.getX() + 0.5, pos.getY() + (land ? 0.0 : 0.1), pos.getZ() + 0.5, player.getYRot() + 180F, 0F);
            if (e instanceof Bucketable b) b.setFromBucket(true);
            if (e instanceof Mob m) m.setPersistenceRequired();
            TankInhabitants.release(e);
            if (!server.tryAddFreshEntityWithPassengers(e)) {
                player.displayClientMessage(Component.translatable("message.bettacraft.fish_bag.release_failed"), true);
                return InteractionResultHolder.fail(stack);
            }
            level.playSound(null, pos, land ? SoundEvents.ARMOR_EQUIP_LEATHER.value() : SoundEvents.BUCKET_EMPTY_FISH, SoundSource.PLAYERS, 1.0F, 0.7F);
            return InteractionResultHolder.consume(new ItemStack(this));
        }
        return InteractionResultHolder.success(stack);
    }

    /** M39 part b: whether the tub holds a land animal of another mod (a terrarium profile that does not live in the water part). */
    static boolean landAnimal(@javax.annotation.Nullable EntityType<?> type) {
        if (type == null) return false;
        var l = org.overengineer.bettacraft.foreign.ForeignTerrarium.land(type);
        return l != null && l.volume() != org.overengineer.bettacraft.tank.BigTankRules.Volume.WATER;
    }

    @Override
    public Component getName(ItemStack stack) {
        EntityType<?> t = containedType(stack);
        return t == null ? super.getName(stack) : Component.translatable("item.bettacraft.transport_tub.filled", t.getDescription());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(isFilled(stack) ? "tooltip.bettacraft.transport_tub.filled" : "tooltip.bettacraft.transport_tub")
                .withStyle(ChatFormatting.GRAY));
    }

    // ------------------------------------------------------------------ bulky: slower while carried

    /** Whether the player carries a filled tub anywhere in the inventory. */
    public static boolean carries(Player p) {
        for (ItemStack s : p.getInventory().items) if (isFilled(s)) return true;
        for (ItemStack s : p.getInventory().offhand) if (isFilled(s)) return true;
        return isFilled(p.containerMenu.getCarried());
    }


    /** Adds or removes the slowness of a carried tub (server; the attribute is synced to the client). */
    public static void updateSlowness(Player p) {
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        boolean carry = carries(p);
        double amount = -ServerConfig.getOr(ServerConfig.BIG_TUB_SLOWNESS, 0.35);
        AttributeModifier have = speed.getModifier(SLOW_ID);
        if (carry && amount < 0) {
            if (have == null || have.amount() != amount) {
                speed.removeModifier(SLOW_ID);
                speed.addTransientModifier(new AttributeModifier(SLOW_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        } else if (have != null) {
            speed.removeModifier(SLOW_ID);
        }
    }
}
