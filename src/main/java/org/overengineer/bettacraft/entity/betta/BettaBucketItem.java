package org.overengineer.bettacraft.entity.betta;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluids;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModEntities;

/** Bucket of Betta: vanilla mob bucket with a tooltip of the fish (sex, shape, colours, pattern). */
public class BettaBucketItem extends org.overengineer.bettacraft.containers.AnimalBucketItem {
    public BettaBucketItem(Properties properties) {
        super(ModEntities.BETTA.get(), Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, properties);
    }

    /** Typed data, falling back to the bucket_entity_data mirror (e.g. buckets edited by commands). */
    public static BettaData data(ItemStack stack) {
        BettaData d = stack.get(ModDataComponents.FISH_DATA.get());
        if (d != null) return d;
        CustomData cd = stack.get(DataComponents.BUCKET_ENTITY_DATA);
        return cd == null ? null : BettaData.fromBucketTag(cd.copyTag());
    }

    /** A bucket holding a betta with the given data (creative tab, tests). */
    public static ItemStack withFish(ItemStack bucket, BettaData d) {
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        t.put(BettaData.KEY, d.save());
        t.putFloat("Health", 6.0F);
        bucket.set(DataComponents.BUCKET_ENTITY_DATA, CustomData.of(t));
        bucket.set(ModDataComponents.FISH_DATA.get(), d);
        return bucket;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BettaData d = data(stack);
        if (d != null) BettaTooltip.append(d, tooltip);
    }
}
