package org.overengineer.bettacraft.terrarium;

import org.overengineer.bettacraft.util.SinkList;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import org.overengineer.bettacraft.util.NbtCompat;
import net.minecraft.world.InteractionResult;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.overengineer.bettacraft.containers.ContainerRules;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankInhabitants;

/**
 * M31 part b: the terrarium box. Empty, it catches our land animals (they turn it into their own box item,
 * {@link AnimalBoxItem}). M35: it also carries a land animal of another mod with a terrarium profile (or on
 * {@code terrarium.boxAllow}): then this same item holds the whole animal ({@code minecraft:custom_data} key
 * {@link #STORED_KEY}, its type as {@code bettacraft:bag_entity}; one per box). Into a tank the tank takes it like an animal
 * from a bucket and adds no water; into the world it is let go on the block looked at (after the same question as a
 * bucket, {@link ContainerRules#release}), and the empty box comes back. Catching: {@code ForeignTerrarium#onInteract}.
 */
public class TerrariumBoxItem extends Item {
    public static final String STORED_KEY = "BettacraftLandAnimal";

    public TerrariumBoxItem(Properties properties) {
        super(properties);
    }

    public static boolean isFilled(ItemStack stack) {
        return stack.getItem() instanceof TerrariumBoxItem && stack.has(ModDataComponents.BAG_ENTITY.get());
    }

    @Nullable
    public static EntityType<?> containedType(ItemStack stack) {
        if (!isFilled(stack)) return null;
        return BuiltInRegistries.ENTITY_TYPE.getOptional(stack.get(ModDataComponents.BAG_ENTITY.get())).orElse(null);
    }

    /** The full entity data (with {@code id}) of the animal in the box, or null. */
    @Nullable
    public static CompoundTag storedEntity(ItemStack stack) {
        if (!isFilled(stack)) return null;
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null || !cd.copyTag().contains(STORED_KEY)) return null;
        CompoundTag t = cd.copyTag().getCompoundOrEmpty(STORED_KEY);
        return NbtCompat.has(t, "id", Tag.TAG_STRING) ? t : null;
    }

    /** A filled box holding {@code e} (the entity itself is not removed). */
    public static ItemStack fillWith(Entity e) {
        ItemStack box = new ItemStack(ModItems.TERRARIUM_BOX.get());
        CompoundTag t = new CompoundTag();
        String id = e.getEncodeId();
        if (id != null) t.putString("id", id);
        t.merge(NbtCompat.saveEntityWithoutId(e));
        t.remove("UUID"); // a new one when it comes out (no clash with the old entity)
        CompoundTag data = new CompoundTag();
        data.put(STORED_KEY, t);
        box.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        box.set(ModDataComponents.BAG_ENTITY.get(), BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()));
        box.set(DataComponents.MAX_STACK_SIZE, 1);
        return box;
    }

    /** Lets the animal go on the block looked at. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isFilled(stack)) return InteractionResult.PASS;
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
        if (level.getBlockState(hit.getBlockPos()).getBlock() instanceof TankBlock) return InteractionResult.PASS;
        BlockPos pos = hit.getBlockPos().relative(hit.getDirection());
        if (!level.mayInteract(player, hit.getBlockPos()) || !player.mayUseItemAt(pos, hit.getDirection(), stack)) return InteractionResult.FAIL;
        if (!ContainerRules.release(level, player, hand, stack)) return ContainerRules.waiting(stack);
        if (level instanceof ServerLevel server) {
            CompoundTag t = storedEntity(stack);
            Entity e = t == null ? null : EntityType.loadEntityRecursive(t, server, net.minecraft.world.entity.EntitySpawnReason.LOAD, en -> en);
            if (e == null) {
                player.displayClientMessage(Component.translatable("message.bettacraft.fish_bag.release_failed"), true);
                return InteractionResult.FAIL;
            }
            e.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot() + 180F, 0F);
            if (e instanceof Mob m) m.setPersistenceRequired();
            TankInhabitants.release(e);
            if (!server.tryAddFreshEntityWithPassengers(e)) {
                player.displayClientMessage(Component.translatable("message.bettacraft.fish_bag.release_failed"), true);
                return InteractionResult.FAIL;
            }
            level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.NEUTRAL, 1.0F, 1.0F);
            return InteractionResult.CONSUME.heldItemTransformedTo(player.hasInfiniteMaterials() ? stack : new ItemStack(this));
        }
        return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        EntityType<?> t = containedType(stack);
        return t == null ? super.getName(stack) : Component.translatable("item.bettacraft.terrarium_box.filled", t.getDescription());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        List<Component> tooltip = new SinkList<>(tooltipAdder);
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);
        tooltip.add(Component.translatable(isFilled(stack) ? "tooltip.bettacraft.terrarium_box.filled" : "tooltip.bettacraft.terrarium_box")
                .withStyle(ChatFormatting.GRAY));
    }
}
