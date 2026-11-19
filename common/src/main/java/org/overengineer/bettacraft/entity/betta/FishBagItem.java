package org.overengineer.bettacraft.entity.betta;

import org.overengineer.bettacraft.util.NbtCompat;
import org.overengineer.bettacraft.util.SinkList;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankInhabitants;

/**
 * Reusable fish bag (PLAN 4.3): holds one fish plus 100 mB of water. Empty bags stack to 16, filled bags do not.
 * The fish is stored like in a bucket ({@code minecraft:bucket_entity_data}, plus {@code bettacraft:fish_data} for
 * bettas); the entity type is the {@code bettacraft:bag_entity} component.
 */
public class FishBagItem extends Item {
    public FishBagItem(Properties properties) {
        super(properties);
    }

    public static boolean isFilled(ItemStack stack) {
        return stack.has(ModDataComponents.BAG_ENTITY.get()) || stack.has(ModDataComponents.CRITTERS.get()) || stack.has(ModDataComponents.TANKMATE_CLUTCH.get())
                || stack.has(ModDataComponents.CRITTER_LARVAE.get()); // M49
    }

    @Nullable
    public static EntityType<?> containedType(ItemStack stack) {
        Identifier id = stack.get(ModDataComponents.BAG_ENTITY.get());
        if (id == null) return null;
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
    }

    /** M16: custom data key of the full entity data of a water animal without a bucket (squid, glow squid ...). */
    public static final String STORED_KEY = "BettacraftEntity";

    /** A filled bag holding {@code e} (the entity itself is not removed). */
    public static ItemStack fillWith(Entity e) {
        ItemStack bag = new ItemStack(ModItems.FISH_BAG.get());
        if (e instanceof Bucketable b) {
            b.saveToBucketTag(bag);
        } else {
            // M16: no bucket format: the whole entity, like the Better Fish Tanks bag
            net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
            String id = e.getEncodeId();
            if (id != null) t.putString("id", id);
            t.merge(NbtCompat.saveEntityWithoutId(e));
            net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
            data.put(STORED_KEY, t);
            bag.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        }
        bag.set(ModDataComponents.BAG_ENTITY.get(), BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()));
        bag.set(DataComponents.MAX_STACK_SIZE, 1);
        return bag;
    }

    /** The full entity data (with {@code id}) of a water animal without a bucket in this bag, or null (M16). */
    @Nullable
    public static net.minecraft.nbt.CompoundTag storedEntity(ItemStack stack) {
        if (!(stack.getItem() instanceof FishBagItem) || !stack.has(ModDataComponents.BAG_ENTITY.get())) return null;
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null || !cd.copyTag().contains(STORED_KEY)) return null;
        net.minecraft.nbt.CompoundTag t = cd.copyTag().getCompoundOrEmpty(STORED_KEY);
        return NbtCompat.has(t, "id", net.minecraft.nbt.Tag.TAG_STRING) ? t : null;
    }

    /** Catch a small water animal (same size rules as the tank; M16: also water animals without a bucket, e.g. squids). */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (isFilled(stack) || !target.isAlive() || target instanceof Player) return InteractionResult.PASS;
        if (!(target instanceof Bucketable) && !TankInhabitants.isWaterAnimal(target)) return InteractionResult.PASS;
        if (org.overengineer.bettacraft.mobs.GlassMobs.isAllowed(target.getType())) return InteractionResult.PASS; // M31 part d: a drowned goes into a mob jar
        if (!TankInhabitants.bagTakes(target)) { // M35: a sea turtle hatchling too
            if (!player.level().isClientSide() && TankInhabitants.isWaterAnimal(target)) {
                player.sendOverlayMessage(TankInhabitants.rejection(TankInhabitants.accepts(target.getType(), target), target.getType()));
            }
            return InteractionResult.PASS;
        }
        if (!player.level().isClientSide()) {
            TankInhabitants.release(target);
            ItemStack filled = fillWith(target);
            target.playSound(target instanceof Bucketable b ? b.getPickupSound() : SoundEvents.BUCKET_FILL_FISH, 1.0F, 1.0F);
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, filled, false));
            target.discard();
            if (target instanceof BettaEntity) org.overengineer.bettacraft.progression.Progress.onBettaCaught(player);
        }
        return InteractionResult.SUCCESS;
    }

    /** Release the fish into water. Clicking a tank is handled by the tank block before this. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isFilled(stack)) {
            // M22.1: an empty bag is shaken out anywhere (sneak + right-click): a few drops, no water block
            if (!player.isSecondaryUseActive()) return InteractionResult.PASS;
            org.overengineer.bettacraft.containers.ContainerRules.pour(level, player,
                    org.overengineer.bettacraft.containers.ContainerRules.pourSpot(player, getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY)));
            return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
        }
        if (stack.has(ModDataComponents.CRITTER_LARVAE.get())) { // M49: larvae only go into a tank
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.bettacraft.amano.larvae_need_tank"));
            return InteractionResult.FAIL;
        }
        if (stack.has(ModDataComponents.CRITTERS.get()) || stack.has(ModDataComponents.TANKMATE_CLUTCH.get())) return releaseCritters(level, player, hand, stack);
        EntityType<?> type = containedType(stack);
        if (type == null) return InteractionResult.PASS;
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
        BlockPos pos = hit.getBlockPos();
        if (!level.getFluidState(pos).is(FluidTags.WATER)) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.bettacraft.fish_bag.needs_water"));
            return InteractionResult.FAIL;
        }
        // M22.1: animals only go into the world once the player confirmed it
        if (!org.overengineer.bettacraft.containers.ContainerRules.release(level, player, hand, stack)) {
            return org.overengineer.bettacraft.containers.ContainerRules.waiting(stack);
        }
        if (level instanceof ServerLevel server && stack.has(ModDataComponents.TANKMATE_GROUP.get())) {
            // M19: a group of tankmates swims off
            org.overengineer.bettacraft.tankmates.TankmateItems.release(server, stack, pos, player);
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_FISH, SoundSource.NEUTRAL, 1.0F, 1.0F);
        } else if (level instanceof ServerLevel server) {
            net.minecraft.nbt.CompoundTag stored = storedEntity(stack);
            Entity e;
            if (stored != null) { // M16: a water animal without a bucket, with its full data
                e = org.overengineer.bettacraft.compat.FishContainers.create(server, stored);
                if (e != null) {
                    e.snapTo(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, player.getYRot(), 0);
                    if (!server.addFreshEntity(e)) e = null;
                }
            } else {
                e = type.spawn(server, stack, player, pos, EntitySpawnReason.BUCKET, false, false);
            }
            if (e == null) {
                // M17: the animal could not be created (removed mod, another mod cancelled the spawn): keep the bag filled
                player.sendOverlayMessage(Component.translatable("message.bettacraft.fish_bag.release_failed"));
                return InteractionResult.FAIL;
            }
            if (e instanceof Bucketable b) {
                b.loadFromBucketTag(stack.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).copyTag());
                b.setFromBucket(true);
            }
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_FISH, SoundSource.NEUTRAL, 1.0F, 1.0F);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
        }
        ItemStack empty = new ItemStack(ModItems.FISH_BAG.get());
        return InteractionResult.SUCCESS.heldItemTransformedTo(ItemUtils.createFilledResult(stack, player, empty));
    }

    /** M14: shrimp, crayfish or snails let go into open water (they are gone). */
    private InteractionResult releaseCritters(Level level, Player player, InteractionHand hand, ItemStack stack) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
        if (!level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.bettacraft.fish_bag.needs_water"));
            return InteractionResult.FAIL;
        }
        if (!org.overengineer.bettacraft.containers.ContainerRules.release(level, player, hand, stack)) {
            return org.overengineer.bettacraft.containers.ContainerRules.waiting(stack);
        }
        if (level instanceof ServerLevel server) {
            level.playSound(null, hit.getBlockPos(), SoundEvents.BUCKET_EMPTY_FISH, SoundSource.NEUTRAL, 0.8F, 1.3F);
            var critters = stack.get(ModDataComponents.CRITTERS.get());
            if (critters != null && !critters.isEmpty()) {
                // M22.3: they live in the world now (crawl off as wild animals)
                org.overengineer.bettacraft.entity.critter.WildCritterEntity.release(server, critters, hit.getBlockPos(), player);
            } else {
                player.sendOverlayMessage(Component.translatable("message.bettacraft.critters.released"));
            }
        }
        return InteractionResult.SUCCESS.heldItemTransformedTo(ItemUtils.createFilledResult(stack, player, new ItemStack(ModItems.FISH_BAG.get())));
    }

    @Override
    public Component getName(ItemStack stack) {
        Component tm = org.overengineer.bettacraft.tankmates.TankmateItems.bagName(stack); // M19
        if (tm != null) return tm;
        var critters = stack.get(ModDataComponents.CRITTERS.get());
        if (critters != null && !critters.isEmpty()) {
            return Component.translatable("item.bettacraft.fish_bag.critters", critters.size(),
                    org.overengineer.bettacraft.critters.CritterTooltip.bagName(critters));
        }
        EntityType<?> type = containedType(stack);
        if (type == null) return super.getName(stack);
        Component inner = stack.has(DataComponents.CUSTOM_NAME) ? stack.get(DataComponents.CUSTOM_NAME) : type.getDescription();
        return Component.translatable("item.bettacraft.fish_bag.filled", inner);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        List<Component> tooltip = new SinkList<>(tooltipAdder);
        BettaData data = stack.get(ModDataComponents.FISH_DATA.get());
        var critters = stack.get(ModDataComponents.CRITTERS.get());
        if (critters != null && !critters.isEmpty()) {
            org.overengineer.bettacraft.critters.CritterTooltip.append(critters, tooltip);
        } else if (org.overengineer.bettacraft.tankmates.TankmateItems.appendBag(stack, tooltip)) {
            // M19: tankmates (one or a group) or a clutch
        } else if (data != null) {
            BettaTooltip.append(data, tooltip);
        } else if (!isFilled(stack)) {
            tooltip.add(Component.translatable("tooltip.bettacraft.fish_bag.empty").withStyle(ChatFormatting.GRAY));
        }
    }
}
