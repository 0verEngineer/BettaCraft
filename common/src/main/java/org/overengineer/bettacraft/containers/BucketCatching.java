package org.overengineer.bettacraft.containers;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.reef.SaltTolerance;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tankmates.TankmateBucketItem;

/**
 * M22.3: catching fish in the world with buckets and bags.
 * <ul>
 *     <li>A bucket or a fish bag that already holds fish of the same species takes one more while it has room
 *     ({@link AnimalContainers#bucket}, {@link AnimalContainers#bag}).</li>
 *     <li>A bucket of sea water works like a bucket of water for every animal that goes into a bucket (vanilla and other
 *     mods' fish too): the item is handed to the animal as a water bucket and comes back unchanged if nothing happened.</li>
 *     <li>The water must suit our animals: a plain water bucket takes no reef animal, a bucket of sea water no freshwater
 *     animal.</li>
 * </ul>
 */
public final class BucketCatching {
    private BucketCatching() {}


    /** The config switch (common). */
    static final class CommonConfigs {
        static boolean big() {
            return Features.enabled(Feature.BIG_CONTAINERS);
        }
    }
}
