package org.overengineer.bettacraft.terrarium;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M31: the land food of the terrarium animals, by item tag ({@link Diet}; the aquarium food goes the way of the fish,
 * {@code TankCare#feed}, {@link Diet#ANIMAL}):
 * <ul>
 *     <li>part b {@link Diet#GREENS} ({@code #bettacraft:turtle_greens}): Hermann's tortoise eats herbs, dandelion and clover in
 *     the wild; Minecraft has dandelions, the clover-like pink petals and short grass and ferns for the herbs (the tag holds
 *     them and the oxeye daisy; packs can add more). Sliders and Reeves' turtles eat greens too.</li>
 *     <li>part c {@link Diet#INSECTS} ({@code #bettacraft:feeder_insects}: crickets): horned frog, leopard gecko, crested gecko.</li>
 *     <li>part c {@link Diet#SMALL_INSECTS} ({@code #bettacraft:small_feeder_insects}: fruit flies): dart frogs, crested gecko.</li>
 *     <li>part c {@link Diet#FRUIT} ({@code #bettacraft:gecko_fruit}: sweet berries, glow berries, melon): the crested gecko (it
 *     licks soft, ripe fruit in the wild; the hobby feeds a fruit mash).</li>
 *     <li>part c {@link Diet#PREY} ({@code #bettacraft:snake_prey}: raw rabbit and chicken, for the frozen mice of the hobby):
 *     the corn snake, about once a week ({@link HerpSpecies#mealDays}).</li>
 * </ul>
 * One item feeds one animal: by hand on the tank (the hungriest one that eats it), or from a feeder (every animal that
 * ate more than {@link #HUNGRY_DAYS} days ago, or its own {@link HerpSpecies#mealDays}).
 */
public final class TerrariumFood {
    /** From this many days after its last meal an animal eats land food from the feeder. */
    public static final double HUNGRY_DAYS = 0.33;

    /** What a terrarium animal eats. */
    public enum Diet {
        /** Aquarium food: pellets, frozen and live food (the kinds of {@code BettaFood}). */
        ANIMAL(null),
        GREENS(ModTags.TURTLE_GREENS),
        INSECTS(ModTags.FEEDER_INSECTS),
        SMALL_INSECTS(ModTags.SMALL_FEEDER_INSECTS),
        FRUIT(ModTags.GECKO_FRUIT),
        PREY(ModTags.SNAKE_PREY);

        @Nullable
        public final TagKey<Item> tag;

        Diet(@Nullable TagKey<Item> tag) {
            this.tag = tag;
        }
    }

    private TerrariumFood() {}

    /** The land diet of this item (null: none, it may still be aquarium food). */
    @Nullable
    public static Diet diet(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (Diet d : Diet.values()) if (d.tag != null && stack.is(d.tag)) return d;
        return null;
    }

    /** Whether the item is land food of some terrarium animal (the feeder takes it). */
    public static boolean isFood(ItemStack stack) {
        return diet(stack) != null;
    }

    /** The animals of the tank that eat this now, hungriest first. */
    public static List<Entity> eaters(ServerLevel level, TankData t, Diet d) {
        List<Entity> out = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) if (e instanceof TerrariumAnimal a && a.eats(d)) out.add(e);
        out.sort(Comparator.comparingDouble((Entity e) -> ((TerrariumAnimal) e).daysSinceFed()).reversed());
        return out;
    }

    /** Land food on a tank: the hungriest animal that eats it eats one. PASS if it is no land food or nobody eats it now. */
    public static InteractionResult handFeed(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
        Diet d = diet(stack);
        if (d == null || !(level instanceof ServerLevel server)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        TankBlockEntity c = TankMultiblock.getController(level, pos);
        if (c == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        List<Entity> list = eaters(server, c.getTank(), d);
        if (list.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        Entity pick = list.getFirst();
        ((TerrariumAnimal) pick).feed();
        ItemStack one = stack.copyWithCount(1);
        String id = stack.getItem().getDescriptionId();
        if (!player.getAbilities().instabuild) stack.shrink(1);
        server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(one)), pick.getX(), pick.getY() + 0.2, pick.getZ(), 6, 0.1, 0.05, 0.1, 0.02);
        level.playSound(null, pick.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 0.5F, 1.4F);
        TankCare.log(server, c, "log.bettacraft.turtle.greens", TankCare.nameArg(pick), "@" + id);
        player.sendOverlayMessage(d == Diet.GREENS ? Component.translatable("message.bettacraft.turtle.greens", pick.getDisplayName())
                : Component.translatable("message.bettacraft.terrarium.ate", pick.getDisplayName(), one.getHoverName()));
        return InteractionResult.CONSUME;
    }

    /** The feeder: land food for every animal that eats it and is hungry. Returns the items used. */
    public static int feedFromFeeder(ServerLevel level, TankBlockEntity c, org.overengineer.bettacraft.platform.item.IItemHandler items) {
        int used = 0;
        for (Diet d : Diet.values()) {
            if (d.tag == null) continue;
            for (Entity e : eaters(level, c.getTank(), d)) {
                if (((TerrariumAnimal) e).daysSinceFed() < HUNGRY_DAYS) continue;
                for (int i = 0; i < items.getSlots(); i++) {
                    if (!items.getStackInSlot(i).is(d.tag)) continue;
                    if (items.extractItem(i, 1, false).isEmpty()) continue;
                    ((TerrariumAnimal) e).feed();
                    used++;
                    break;
                }
            }
        }
        return used;
    }
}
